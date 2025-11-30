# Packets Phone -> Watch

TODO

# Packets Watch -> Phone

TODO

# Buckets

Watch can store up to 15 of them, up to 256 bytes each

Every bucket corresponds to one Tasker directory

Data in every bucket:
* Number of items (uint8)
* Items (Max 13 items - 19 bytes per item)
  * ID (uint16)
  * Target directory (uint8) - If item should open another directory, 0 otherwise
  * Flags (uint8) - For future use
  * Title (cstring) - up to 14 bytes + null terminator

Every bucket is stored in the `2001` - `2015` storage keys

# Non-bucket storage on the watch

160 bytes left over from buckets

`1000` - List of all buckets on the watch (up to 60 bytes)    
* array of tuples (up to 15 items)
* array size determined through `persist_get_size()`
* Tuple: (2 bytes each)
  * Bucket id (uint8)
  * Flags (uint8) - For future use
