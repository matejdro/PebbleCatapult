#include <pebble.h>
#include "commons/connection/bluetooth.h"
#include "commons/connection/bucket_sync.h"
#include "ui/window_action_list.h"
#include "connection/packets.h"
#include "ui/window_status.h"


const uint16_t PROTOCOL_VERSION = 3;

// We must initialize this here instead of in the main, otherwise the entire Basalt watch crashes when creating a voice session
// Maybe we run out of stack?
static uint8_t tmp[PERSIST_DATA_MAX_LENGTH];

int main(void)
{
    packets_init();
    bluetooth_init();
    bucket_sync_init();

    send_watch_welcome();

    const bool loaded = bucket_sync_load_bucket(1, tmp);

    if (!loaded || tmp[0] == 0)
    {
        window_status_show_empty();
    }
    else
    {
        window_action_list_show(1);
    }

    app_event_loop();
}