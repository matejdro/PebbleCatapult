#include "bucket_sync.h"
#include "bluetooth.h"
#include "../ui/window_status.h"
#include "../utils/bytes.h"

static const uint16_t FILE_BUCKET_LIST = 1000;
static const uint16_t FILE_BUCKET_SYNC_VERSION = 1001;
static const uint16_t FILE_PROTOCOL_VERSION = 1002;

typedef struct
{
    void (*data_change_callback)(BucketMetadata, void*);
    void* context;
} DataChangeCallback;

uint16_t bucket_sync_current_version = 0;
static BucketList buckets;
static void (*list_change_callback)() = NULL;
static DataChangeCallback data_change_callback = {
    .data_change_callback = NULL,
    .context = NULL,
};
static void (*syncing_status_callback)() = NULL;

static uint16_t bucket_sync_pending_next_version = 0;
bool bucket_sync_is_currently_syncing = false;

static uint32_t get_bucket_persist_key(uint8_t bucket_id);
static void delete_inactive_buckets(const uint8_t* data, const uint8_t new_active_buckets);
static void save_bucket_data(const uint8_t* data, size_t data_size, uint8_t position);
static void complete_sync(void);

void bucket_sync_init()
{
    if (!persist_exists(FILE_BUCKET_SYNC_VERSION))
    {
        buckets.count = 0;
        return;
    }

    persist_read_data(
        FILE_BUCKET_SYNC_VERSION,
        &bucket_sync_current_version,
        sizeof(bucket_sync_current_version)
    );

    const int read_data = persist_read_data(FILE_BUCKET_LIST, buckets.data, sizeof(buckets.data));
    if (read_data >= 0)
    {
        buckets.count = read_data / sizeof(BucketMetadata);
    }
    else
    {
        buckets.count = 0;
    }

    uint16_t written_protocol_version;
    persist_read_data(
        FILE_PROTOCOL_VERSION,
        &written_protocol_version,
        sizeof(written_protocol_version)
    );

    if (written_protocol_version != PROTOCOL_VERSION)
    {
        // If protocol version has been updated, it means that the old data on the watch might not be compatible anymore
        // To save limited storage space, bucket sync is not backwards compatible, so this data must now be deleted and
        // re-synced from the phone.
        // Since this happens rarely and re-sync is fairly quick, it should not be a big issue

        for (int i = 0; i < buckets.count; i++)
        {
            persist_delete(get_bucket_persist_key(buckets.data[i].id));
        }
        buckets.count = 0;
        persist_delete(FILE_BUCKET_SYNC_VERSION);
        persist_delete(FILE_BUCKET_LIST);
    }
}

bool bucket_sync_load_bucket(const uint8_t bucket_id, uint8_t* target)
{
    const uint32_t persist_key = get_bucket_persist_key(bucket_id);
    const int status = persist_read_data(persist_key, target, PERSIST_DATA_MAX_LENGTH);
    return status != E_DOES_NOT_EXIST;
}

BucketList* bucket_sync_get_bucket_list()
{
    return &buckets;
}

void bucket_sync_set_bucket_list_change_callback(void (*callback)())
{
    list_change_callback = callback;
}

void bucket_sync_set_bucket_data_change_callback(void (*callback)(BucketMetadata, void*), void* context)
{
    data_change_callback = (DataChangeCallback){
        .data_change_callback = callback,
        .context = context
    };
}

void bucket_sync_clear_bucket_data_change_callback(void (*callback)(BucketMetadata, void*))
{
    if (data_change_callback.data_change_callback == callback)
    {
        data_change_callback = (DataChangeCallback){
            .data_change_callback = NULL,
            .context = NULL
        };
    }
}

void bucket_sync_on_start_received(const uint8_t* data, size_t data_size)
{
    const uint8_t sync_status = data[0];
    if (sync_status == 2)
    {
        bucket_sync_is_currently_syncing = false;
        void (*local_syncing_callback)() = syncing_status_callback;
        if (local_syncing_callback != NULL)
        {
            local_syncing_callback();
        }

        return;
    }

    bucket_sync_is_currently_syncing = true;
    void (*local_syncing_callback)() = syncing_status_callback;
    if (local_syncing_callback != NULL)
    {
        local_syncing_callback();
    }

    bucket_sync_pending_next_version = read_uint16_from_byte_array(data, 1);
    const uint8_t new_active_buckets = data[3];
    delete_inactive_buckets(data, new_active_buckets);

    buckets.count = new_active_buckets;
    for (int i = 0; i < new_active_buckets; i++)
    {
        BucketMetadata* bucket_metadata = &buckets.data[i];
        bucket_metadata->id = data[4 + i * sizeof(BucketMetadata)];
        bucket_metadata->flags = data[4 + i * sizeof(BucketMetadata) + 1];
    }

    persist_write_data(FILE_BUCKET_LIST, buckets.data, buckets.count * sizeof(BucketMetadata));

    void (*local_list_change_callback)() = list_change_callback;
    if (local_list_change_callback != NULL)
    {
        local_list_change_callback();
    }

    save_bucket_data(data, data_size, 4 + new_active_buckets * sizeof(BucketMetadata));
    if (sync_status == 1)
    {
        complete_sync();
    }
}

void bucket_sync_on_next_packet_received(const uint8_t* data, size_t data_size)
{
    const uint8_t sync_status = data[0];

    save_bucket_data(data, data_size, 1);

    if (sync_status == 1)
    {
        complete_sync();
    }
}

static void save_bucket_data(const uint8_t* data, size_t data_size, uint8_t position)
{
    while (position < data_size)
    {
        const uint8_t id = data[position++];
        const uint8_t size = data[position++];

        const int status = persist_write_data(get_bucket_persist_key(id), &data[position], size);
        if (status < 0)
        {
            window_status_show_error("Failed writing data\n\nIs watch's storage full?");
            return;
        };

        const DataChangeCallback local_bucket_data_change_callback = data_change_callback;
        if (local_bucket_data_change_callback.data_change_callback != NULL)
        {
            for (int j = 0; j < buckets.count; j++)
            {
                if (buckets.data[j].id == id)
                {
                    local_bucket_data_change_callback.data_change_callback(buckets.data[j],
                                                                           local_bucket_data_change_callback.context);
                    break;
                }
            }
        }

        position += size;
    }
}

static void complete_sync(void)
{
    bucket_sync_current_version = bucket_sync_pending_next_version;
    persist_write_data(
        FILE_BUCKET_SYNC_VERSION,
        &bucket_sync_current_version,
        sizeof(bucket_sync_current_version)
    );
    persist_write_data(
        FILE_PROTOCOL_VERSION,
        &PROTOCOL_VERSION,
        sizeof(PROTOCOL_VERSION)
    );

    bucket_sync_is_currently_syncing = false;
    void (*local_syncing_callback)() = syncing_status_callback;
    if (local_syncing_callback != NULL)
    {
        local_syncing_callback();
    }
}

static void delete_inactive_buckets(const uint8_t* data, const uint8_t new_active_buckets)
{
    for (int i = 0; i < buckets.count; i++)
    {
        bool bucket_exists = false;
        const uint8_t old_bucket_id = buckets.data[i].id;
        for (int j = 0; j < new_active_buckets; j++)
        {
            const uint8_t new_bucket_id = data[3 + i * sizeof(BucketMetadata)];
            if (new_bucket_id == old_bucket_id)
            {
                bucket_exists = true;
                break;
            }
        }

        if (!bucket_exists)
        {
            persist_delete(get_bucket_persist_key(old_bucket_id));
        }
    }
}

static uint32_t get_bucket_persist_key(const uint8_t bucket_id)
{
    return bucket_id + 2000;
}

void bucket_sync_register_syncing_status_changed_callback(void (*callback)())
{
    syncing_status_callback = callback;
}
