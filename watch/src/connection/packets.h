#pragma once

#include "pebble.h"

void send_watch_welcome();
void send_trigger_action(uint16_t id, const char* name);
void packets_init();