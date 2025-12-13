#include <pebble.h>
#include "connection/bluetooth.h"
#include "connection/bucket_sync.h"
#include "ui/window_action_list.h"
#include "connection/packets.h"


int main(void)
{
  bluetooth_init();
  bucket_sync_init();

  send_watch_welcome();

  window_action_list_show();

  app_event_loop();
}
