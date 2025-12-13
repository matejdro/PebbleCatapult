#pragma once

#include "bluetooth.h"

void send_watch_welcome();
void send_trigger_action(uint16_t id);

void receive_watch_packet(const DictionaryIterator* received);