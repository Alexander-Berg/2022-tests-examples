#pragma once

#include <../../../src/tile_decoder/buffer_writers/buffer_writer.h>

namespace yandex::maps::jsapi::vector::tileDecoder::bufferWriters {

bool operator==(const WrittenPrimitive& lhs, const WrittenPrimitive& rhs);

}
