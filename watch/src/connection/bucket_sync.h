#pragma once

#include <pebble.h>

typedef struct __attribute__ ((packed))
{
    uint8_t id;
    uint8_t flags;
}

BucketMetadata;

typedef struct __attribute__ ((packed))
{
    uint8_t count;
    BucketMetadata data[15];
}

BucketList;

extern uint16_t bucket_sync_current_version;
extern bool bucket_sync_is_currently_syncing;

void bucket_sync_init();

/**
 * Load contents of a bucket into a target array.
 *
 * Target must be an array of a size PERSIST_DATA_MAX_LENGTH (256 bytes for now).
 *
 * @return true if load was successful, false if bucket does not exist
 */
bool bucket_sync_load_bucket(uint8_t bucket_id, uint8_t* target);

/**
 * Get the list of currently active buckets. Provided structure must be kept read-only, do not write anything into it.
 */
BucketList* bucket_sync_get_bucket_list();

/**
 * Register the callback that will get triggered whenever a list of currently active buckets change
 */
void bucket_sync_set_bucket_list_change_callback(void (*callback)());

/**
 * Register the callback that will get triggered whenever a data of a bucket changes.
 *
 * Parameter in the callback returns the metadata of the bucket that changed.
 */
void bucket_sync_set_bucket_data_change_callback(void(*callback)(BucketMetadata, void*), void*context);

void bucket_sync_set_auto_close_after_sync();

/**
 * Clear the callback if currently registered callback is the passed one.
 */
void bucket_sync_clear_bucket_data_change_callback(void(*callback)(BucketMetadata, void*), void*context);

void bucket_sync_register_syncing_status_changed_callback(void (*callback)());

void bucket_sync_on_start_received(const uint8_t* data, size_t data_size);
void bucket_sync_on_next_packet_received(const uint8_t* data, size_t data_size);