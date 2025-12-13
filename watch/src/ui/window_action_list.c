#include "window_action_list.h"
#include "pebble.h"
#include "../connection/bucket_sync.h"
#include "../utils/bytes.h"

typedef struct
{
    uint16_t id;
    uint8_t target_directory;
    uint8_t flags;
    char title[15];
} ActionItem;

typedef struct
{
    uint8_t count;
    ActionItem data[13];
} ActionListBucket;

static MenuLayer* menu;

static ActionListBucket current_menu_data;

static void load_menu(uint8_t directory_id);

static uint16_t menu_get_num_rows_callback(MenuLayer* me, uint16_t section_index, void* data)
{
    return current_menu_data.count;
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void menu_draw_row_callback(GContext* ctx, const Layer* cell_layer, MenuIndex* cell_index, void* data)
{
    const int16_t row = cell_index->row;

    menu_cell_basic_draw(ctx, cell_layer, current_menu_data.data[row].title, NULL, NULL);
}

static void bucket_update_callback(BucketMetadata bucket_metadata)
{
    load_menu(1);
}


// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void window_load(Window* window)
{
    current_menu_data.count = 0;

    Layer* window_layer = window_get_root_layer(window);
    const GRect screen_bounds = layer_get_bounds(window_layer);

    menu = menu_layer_create(screen_bounds);

    menu_layer_set_callbacks(menu,
                             NULL,
                             (MenuLayerCallbacks)
    {
        .
        get_num_rows = menu_get_num_rows_callback,
        .
        draw_row = menu_draw_row_callback,
    }
    )
    ;

    layer_add_child(window_layer, menu_layer_get_layer(menu));

    bucket_sync_set_bucket_list_change_callback(bucket_update_callback);

    load_menu(1);
}

static void window_unload(Window* window)
{
    bucket_sync_clear_bucket_data_change_callback(bucket_update_callback);
    menu_layer_destroy(menu);
    window_destroy(window);
}

void window_action_list_show()
{
    Window* window = window_create();
    window_set_window_handlers(window, (WindowHandlers)
    {
        .
        load = window_load,
        .
        unload = window_unload,
    }
    )
    ;
    const bool animated = true;
    window_stack_push(window, animated);
}

void load_menu(uint8_t directory_id)
{
    uint8_t tmp[PERSIST_DATA_MAX_LENGTH];

    if (bucket_sync_load_bucket(directory_id, tmp))
    {
        const uint8_t count = tmp[0];
        current_menu_data.count = count;

        int position = 1;
        for (uint8_t i = 0; i < count; i++)
        {
            current_menu_data.data[i].id = read_uint16_from_byte_array(tmp, position);
            position += 2;

            current_menu_data.data[i].target_directory = tmp[position++];
            current_menu_data.data[i].flags = tmp[position++];

            const char* title = strcpy(current_menu_data.data[i].title, (char*)&tmp[position]);
            position += strlen(title) + 1;
        }
        menu_layer_reload_data(menu);
    }
}