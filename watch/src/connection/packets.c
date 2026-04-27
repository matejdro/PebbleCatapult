#include "packets.h"
#include "commons/connection/bluetooth.h"
#include "commons/connection/bucket_sync.h"
#include <pebble.h>

#include "../ui/window_status.h"

static void receive_phone_welcome(const DictionaryIterator* iterator);
static void receive_sync_restart(const DictionaryIterator* iterator);
static void receive_sync_next_packet(const DictionaryIterator* iterator);
static void receive_watch_packet(const DictionaryIterator* received);

static uint8_t active_buckets_holder[MAX_BUCKETS];

void packets_init()
{
    bluetooth_register_reconnect_callback(send_watch_welcome);
    bluetooth_register_receive_watch_packet(receive_watch_packet);
}

void send_watch_welcome()
{
    const BucketList* active_buckets = bucket_sync_get_bucket_list();
    for (int i = 0; i < active_buckets->count; i++)
    {
        active_buckets_holder[i] = active_buckets->data[i].id;
    }

    DictionaryIterator* iterator;
    app_message_outbox_begin(&iterator);
    dict_write_uint8(iterator, 0, 0);
    dict_write_uint16(iterator, 1, PROTOCOL_VERSION);
    dict_write_uint16(iterator, 2, bucket_sync_current_version);
    dict_write_uint16(iterator, 3, appmessage_max_size);
    dict_write_data(iterator, 7, active_buckets_holder, active_buckets->count);
    bluetooth_app_message_outbox_send();
}

void send_trigger_action(const uint16_t id, const char* name)
{
    DictionaryIterator* iterator;
    app_message_outbox_begin(&iterator);
    dict_write_uint8(iterator, 0, 4);
    dict_write_uint16(iterator, 1, id);
    dict_write_cstring(iterator, 2, name);
    bluetooth_app_message_outbox_send();
}

static void receive_watch_packet(const DictionaryIterator* received)
{
    const uint8_t packet_id = dict_find(received, 0)->value->uint8;

    switch (packet_id)
    {
    case 1:
        receive_phone_welcome(received);
        break;
    case 2:
        receive_sync_restart(received);
        break;
    case 3:
        receive_sync_next_packet(received);
        break;
    default:
        break;
    }
}

void receive_phone_welcome(const DictionaryIterator* iterator)
{
    if (launch_reason() == APP_LAUNCH_PHONE && dict_find(iterator, 3) != NULL)
    {
        bucket_sync_set_auto_close_after_sync();
    }

    const uint16_t phone_protocol_version = dict_find(iterator, 1)->value->uint16;
    if (phone_protocol_version != PROTOCOL_VERSION)
    {
        if (phone_protocol_version > PROTOCOL_VERSION)
        {
            window_status_show_error("Version mismatch\n\nPlease update watch app");
        }
        else
        {
            window_status_show_error("Version mismatch\n\nPlease update phone app");
        }
        return;
    }

    // ReSharper disable once CppLocalVariableMayBeConst
    Tuple* dict_entry = dict_find(iterator, 2);

    bucket_sync_on_start_received(dict_entry->value->data, dict_entry->length);
}

void receive_sync_restart(const DictionaryIterator* iterator)
{
    // ReSharper disable once CppLocalVariableMayBeConst
    Tuple* dict_entry = dict_find(iterator, 1);

    bucket_sync_on_start_received(dict_entry->value->data, dict_entry->length);
}

void receive_sync_next_packet(const DictionaryIterator* iterator)
{
    // ReSharper disable once CppLocalVariableMayBeConst
    Tuple* dict_entry = dict_find(iterator, 1);

    bucket_sync_on_next_packet_received(dict_entry->value->data, dict_entry->length);
}