#include "window_action_list.h"
#include "pebble.h"
#include "window_status.h"
#include "../connection/bucket_sync.h"
#include "../utils/bytes.h"
#include "layers/status_bar.h"

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

typedef struct
{
    MenuLayer* menu;
    ActionListBucket current_menu_data;
    CustomStatusBarLayer* status_bar;
} WindowActionList;


static void load_menu(WindowActionList* window, uint8_t directory_id);
static void configure_buttons(void* context);

static uint16_t menu_get_num_rows_callback(MenuLayer* me, uint16_t section_index, void* data)
{
    return ((WindowActionList*)data)->current_menu_data.count;
}

// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void menu_draw_row_callback(GContext* ctx, const Layer* cell_layer, MenuIndex* cell_index, void* data)
{
    const int16_t row = cell_index->row;

    menu_cell_basic_draw(ctx, cell_layer, ((WindowActionList*)data)->current_menu_data.data[row].title, NULL, NULL);
}

static void bucket_update_callback(BucketMetadata bucket_metadata, void* context)
{
    load_menu(context, 1);
}


// ReSharper disable once CppParameterMayBeConstPtrOrRef
static void window_load(Window* window)
{
    WindowActionList* window_action_list = malloc(sizeof(WindowActionList));
    window_action_list->current_menu_data.count = 0;


    Layer* window_layer = window_get_root_layer(window);
    const GRect screen_bounds = layer_get_bounds(window_layer);
    window_action_list->status_bar = custom_status_bar_layer_create(screen_bounds);
    const GRect status_bar_bounds = layer_get_bounds(window_action_list->status_bar->layer);

    window_action_list->menu = menu_layer_create(
        GRect(
            0,
            status_bar_bounds.size.h,
            screen_bounds.size.w,
            screen_bounds.size.h - status_bar_bounds.size.h
        )
    );

    menu_layer_set_callbacks(window_action_list->menu,
                             window_action_list,
                             (MenuLayerCallbacks)
                             {
                                 .get_num_rows = menu_get_num_rows_callback,
                                 .draw_row = menu_draw_row_callback,
                             }
    );

    layer_add_child(window_layer, menu_layer_get_layer(window_action_list->menu));
    layer_add_child(window_layer, window_action_list->status_bar->layer);

    bucket_sync_set_bucket_data_change_callback(bucket_update_callback, window_action_list);
    window_set_click_config_provider_with_context(window, configure_buttons, window_action_list);

    load_menu(window_action_list, 1);
    window_set_user_data(window, window_action_list);
}

static void window_unload(Window* window)
{
    const WindowActionList* window_action_list = window_get_user_data(window);
    bucket_sync_clear_bucket_data_change_callback(bucket_update_callback);
    menu_layer_destroy(window_action_list->menu);
    window_destroy(window);
}

static void window_show(Window* window)
{
    const WindowActionList* window_action_list = window_get_user_data(window);
    custom_status_bar_set_active(window_action_list->status_bar, true);
}

static void window_hide(Window* window)
{
    const WindowActionList* window_action_list = window_get_user_data(window);
    custom_status_bar_set_active(window_action_list->status_bar, false);
}

static void on_button_up_pressed(ClickRecognizerRef recognizer, void* context)
{
    const WindowActionList* window_action_list = context;
    MenuLayer* menu_layer = window_action_list->menu;
    const MenuIndex index = menu_layer_get_selected_index(menu_layer);

    if (index.row == 0)
    {
        const uint8_t last_index = window_action_list->current_menu_data.count - 1;
        menu_layer_set_selected_index(
            menu_layer,
            (MenuIndex){.row = last_index, .section = 0},
            MenuRowAlignCenter,
            true
        );
    }
    else
    {
        menu_layer_set_selected_index(
            menu_layer,
            (MenuIndex){.row = index.row - 1, .section = 0},
            MenuRowAlignCenter,
            true
        );
    }
}

static void on_button_down_pressed(ClickRecognizerRef recognizer, void* context)
{
    const WindowActionList* window_action_list = context;
    MenuLayer* menu_layer = window_action_list->menu;
    const MenuIndex index = menu_layer_get_selected_index(menu_layer);
    const uint8_t last_index = window_action_list->current_menu_data.count - 1;

    if (index.row == last_index)
    {
        menu_layer_set_selected_index(
            menu_layer,
            (MenuIndex){.row = 0, .section = 0},
            MenuRowAlignCenter,
            true
        );
    }
    else
    {
        menu_layer_set_selected_index(
            menu_layer,
            (MenuIndex){.row = index.row + 1, .section = 0},
            MenuRowAlignCenter,
            true
        );
    }
}

static void configure_buttons(void* context)
{
    window_single_repeating_click_subscribe(BUTTON_ID_UP, 100, on_button_up_pressed);
    window_single_repeating_click_subscribe(BUTTON_ID_DOWN, 100, on_button_down_pressed);
}

void window_action_list_show()
{
    Window* window = window_create();
    window_set_window_handlers(window, (WindowHandlers)
                               {
                                   .load = window_load,
                                   .unload = window_unload,
                                   .appear = window_show,
                                   .disappear = window_hide
                               }
    );
    const bool animated = true;
    window_stack_push(window, animated);
}

static void load_menu(WindowActionList* window, uint8_t directory_id)
{
    ActionListBucket* current_menu_data = &window->current_menu_data;
    uint8_t tmp[PERSIST_DATA_MAX_LENGTH];

    if (bucket_sync_load_bucket(directory_id, tmp))
    {
        const uint8_t count = tmp[0];
        if (count == 0 && directory_id == 1)
        {
            window_status_show_empty();
            return;
        }
        current_menu_data->count = count;

        int position = 1;
        for (uint8_t i = 0; i < count; i++)
        {
            current_menu_data->data[i].id = read_uint16_from_byte_array(tmp, position);
            position += 2;

            current_menu_data->data[i].target_directory = tmp[position++];
            current_menu_data->data[i].flags = tmp[position++];

            const char* title = strcpy(current_menu_data->data[i].title, (char*)&tmp[position]);
            position += strlen(title) + 1;
        }
        menu_layer_reload_data(window->menu);
    }
    else if (directory_id == 1)
    {
        window_status_show_empty();
    }
}
