#include <pebble.h>
#include "connection/bluetooth.h"
#include "connection/bucket_sync.h"
#include "ui/window_action_list.h"
#include "connection/packets.h"
#include "ui/window_status.h"


int main(void)
{
    bluetooth_init();
    bucket_sync_init();

    send_watch_welcome();

    uint8_t tmp[PERSIST_DATA_MAX_LENGTH];
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
