#include "packets.h"
#include "bluetooth.h"
#include "bucket_sync.h"
#include <pebble.h>

static void receive_phone_welcome(const DictionaryIterator* iterator);
static void receive_sync_restart(const DictionaryIterator* iterator);

void send_watch_welcome()
{
    DictionaryIterator* iterator;
    app_message_outbox_begin(&iterator);
    dict_write_uint8(iterator, 0, 0);
    dict_write_uint16(iterator, 1, PROTOCOL_VERSION);
    dict_write_uint16(iterator, 2, bucket_sync_current_version);
    dict_write_uint16(iterator, 3, appmessage_max_size);
    custom_app_mesage_outbox_send();
}

void receive_watch_packet(DictionaryIterator* received)
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
    default:
        break;
    }
}

void receive_phone_welcome(const DictionaryIterator* iterator)
{
    const uint16_t phone_protocol_version = dict_find(iterator, 1)->value->uint16;
    if (phone_protocol_version != PROTOCOL_VERSION)
    {
        // TODO show version mismatch error
        return;
    }

    Tuple* dict_entry = dict_find(iterator, 2);

    on_bucket_sync_start_received(dict_entry->value->data, dict_entry->length);
}

void receive_sync_restart(const DictionaryIterator* iterator)
{
    Tuple* dict_entry = dict_find(iterator, 1);
    on_bucket_sync_start_received(dict_entry->value->data, dict_entry->length);
}
