#include "bluetooth.h"
#include "packets.h"

uint32_t appmessage_max_size = 0;
bool is_currently_sending_data = false;
bool is_phone_connected = true;
bool got_sending_error = false;

const uint16_t PROTOCOL_VERSION = 1;

static void (*sending_finish_callback)(bool success) = NULL;

static void (*phone_connected_change_callback)() = NULL;

static void (*sending_now_callback)() = NULL;

static void (*sending_error_callback)() = NULL;

static void on_sent_data(DictionaryIterator* iterator, void* context);

static void on_connection_changed(bool status);

static void on_sending_failed(DictionaryIterator* iterator, AppMessageResult reason, void* context);

static void on_received_data(DictionaryIterator* iterator, void* context);

void bluetooth_init()
{
    appmessage_max_size = app_message_inbox_size_maximum();
    if (appmessage_max_size > 4096)
        appmessage_max_size = 4096; //Limit inbox size to conserve RAM.

    connection_service_peek_pebble_app_connection();
    const ConnectionHandlers connection_handlers = {
        .pebble_app_connection_handler = on_connection_changed

    };
    connection_service_subscribe(connection_handlers);
    app_message_register_inbox_received(on_received_data);
    app_message_register_outbox_sent(on_sent_data);
    app_message_register_outbox_failed(on_sending_failed);
    app_message_open(appmessage_max_size, 200);
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void on_received_data(DictionaryIterator* iterator, void* context)
{
    if (!is_phone_connected)
    {
        is_phone_connected = true;
        void (*local_phone_connected)() = phone_connected_change_callback;
        if (local_phone_connected != NULL)
        {
            local_phone_connected();
        }
    }

    receive_watch_packet(iterator);
}

static void on_sent_data(DictionaryIterator* iterator, void* context)
{
    void (*local_sending_now_callback)() = sending_now_callback;
    is_currently_sending_data = false;
    if (local_sending_now_callback != NULL)
    {
        local_sending_now_callback();
    }

    void (*localCallback)(bool success) = sending_finish_callback;
    if (localCallback != NULL)
    {
        localCallback(true);
    }
}

void bluetooth_app_message_outbox_send()
{
    is_currently_sending_data = true;
    void (*local_sending_now_callback)() = sending_now_callback;
    if (local_sending_now_callback != NULL)
    {
        local_sending_now_callback();
    }

    const AppMessageResult res = app_message_outbox_send();
    if (res != APP_MSG_OK)
    {
        on_sending_failed(NULL, res, NULL);
    }
}

static void on_sending_failed(DictionaryIterator* iterator, const AppMessageResult reason, void* context)
{
    void (*local_sending_finish_callback)(bool success) = sending_finish_callback;
    void (*local_sending_error_callback)() = sending_error_callback;

    void (*local_sending_now_callback)() = sending_now_callback;
    is_currently_sending_data = false;
    if (local_sending_now_callback != NULL)
    {
        local_sending_now_callback();
    }

    switch (reason)
    {
    case APP_MSG_OK:
        if (local_sending_finish_callback != NULL)
        {
            local_sending_finish_callback(true);
        }
        break;
    case APP_MSG_BUSY:
    case APP_MSG_SEND_REJECTED:
    case APP_MSG_APP_NOT_RUNNING:
    case APP_MSG_INVALID_ARGS:
    case APP_MSG_BUFFER_OVERFLOW:
    case APP_MSG_ALREADY_RELEASED:
    case APP_MSG_CALLBACK_ALREADY_REGISTERED:
    case APP_MSG_CALLBACK_NOT_REGISTERED:
    case APP_MSG_OUT_OF_MEMORY:
    case APP_MSG_CLOSED:
    case APP_MSG_INTERNAL_ERROR:
    case APP_MSG_INVALID_STATE:
        if (local_sending_finish_callback != NULL)
        {
            local_sending_finish_callback(false);
        }
        got_sending_error = true;
        if (local_sending_error_callback != NULL)
        {
            local_sending_error_callback();
        }
        break;
    case APP_MSG_NOT_CONNECTED:
    case APP_MSG_SEND_TIMEOUT:
        if (local_sending_finish_callback != NULL)
        {
            local_sending_finish_callback(false);
        }

        is_phone_connected = false;
        void (*local_phone_connected)() = phone_connected_change_callback;
        if (local_phone_connected != NULL)
        {
            local_phone_connected();
        }
        break;
    }
}

static void on_connection_changed(bool status)
{
    is_phone_connected = status;
    void (*local_phone_connected)() = phone_connected_change_callback;
    if (local_phone_connected != NULL)
    {
        local_phone_connected();
    }
}

void bluetooth_register_sending_finish(void (*callback)(bool success))
{
    sending_finish_callback = callback;
}

void bluetooth_register_phone_connected_change_callback(void (*callback)())
{
    phone_connected_change_callback = callback;
}

void bluetooth_register_sending_now_change_callback(void (*callback)())
{
    sending_now_callback = callback;
}

void bluetooth_register_sending_error_status_callback(void (*callback)())
{
    sending_error_callback = callback;
}