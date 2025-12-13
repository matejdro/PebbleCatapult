#include "bytes.h"

inline uint16_t read_uint16_from_byte_array(const uint8_t* bytes, const size_t offset)
{
    return (bytes[offset] << 8) | (bytes[offset + 1]);
}