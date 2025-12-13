#pragma once

#include "pebble.h"

extern uint32_t appmessage_max_size;
extern bool is_currently_sending_data;
extern bool is_phone_connected;
extern bool got_sending_error;

extern const uint16_t PROTOCOL_VERSION;

void bluetooth_init();

void register_bluetooth_sending_finish(void (*callback)(bool success));

void register_phone_connected_change_callback(void (*callback)());

void register_sending_now_change_callback(void (*callback)());

void register_sending_error_status_callback(void (*callback)());

void custom_app_mesage_outbox_send();