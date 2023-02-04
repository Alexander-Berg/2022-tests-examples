#include "buffer_writer.h"

namespace yandex::maps::jsapi::vector::tileDecoder::bufferWriters {

bool operator==(const WrittenPrimitive& lhs, const WrittenPrimitive& rhs) {
    return
        lhs.vertexByteOffset == rhs.vertexByteOffset &&
        lhs.vertexByteLength == rhs.vertexByteLength &&
        lhs.indexByteOffset == rhs.indexByteOffset &&
        lhs.indexByteLength == rhs.indexByteLength &&
        lhs.bufferIndex == rhs.bufferIndex;
}

}
