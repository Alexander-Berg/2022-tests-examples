#include <boost/test/unit_test.hpp>
#include "../../../src/common/tile.h"
#include "../../../src/tile_decoder/primitive_extractors/extract_polylines.h"
#include "../../../src/tile_decoder/buffer_writers/polyline_buffer_writer.h"
#include "../../../src/tile_decoder/proto/type_aliases.h"
#include "../../../utils/file.h"
#include "buffer_writer.h"
#include "polyline_buffer_writer.test.h"

namespace yandex::maps::jsapi::vector::tileDecoder::bufferWriters::test {

namespace testUtils = yandex::maps::jsapi::vector::test::utils;
namespace proto = yandex::maps::jsapi::vector::tileDecoder::proto;
namespace primitiveExtractors = yandex::maps::jsapi::vector::tileDecoder::primitiveExtractors;

const auto TILE_FILE = "./tile_decoder/vmap2_tiles/tile_2475_1284_12.pb.bin";
const auto TILE_FILE2 = "./tile_decoder/vmap2_tiles/tile_79317_41144_17.pb.bin";
const auto TILE_FILE3 = "./tile_decoder/vmap2_tiles/tile_19807_10273_15.pb.bin";
const auto TILE_VERTEX_BUFFER_0 = "./tile_decoder/buffer_writers/written_buffers/tile_2475_1284_12.polylineVertexBuffer.bin";
const auto TILE_INDEX_BUFFER_0 = "./tile_decoder/buffer_writers/written_buffers/tile_2475_1284_12.polylineIndexBuffer.bin";

void polylineBufferWriterTest() {
    {
        const auto encodedTileContent = testUtils::readFile(TILE_FILE);
        const auto vertexBufferContent = testUtils::readFile(TILE_VERTEX_BUFFER_0);
        const auto indexBufferContent = testUtils::readFile(TILE_INDEX_BUFFER_0);
        const auto tile = Tile{2475, 1284, 12};
        const auto protoTile = proto::unpackTile(
            NULL,
            encodedTileContent.size(),
            reinterpret_cast<const unsigned char *>(encodedTileContent.data())
        );
        const auto protoPresentation = protoTile->presentation[0];

        auto polylines = primitiveExtractors::extractPolylines(tile, *protoTile, *protoPresentation);

        PolylineBufferWriter writer;

        auto primitives = writer.writePolylines(polylines);

        BOOST_CHECK_EQUAL(writer.pages().size(), 1);

        const auto expectedVertexBufferSize = vertexBufferContent.size() / 4;
        const auto expectedVertexBuffer = reinterpret_cast<const uint32_t*>(vertexBufferContent.data());
        uint32_t* vertexBuffer = const_cast<uint32_t*>(
            reinterpret_cast<const uint32_t*>(writer.pages()[0].vertices.data())
        );

        BOOST_CHECK_EQUAL_COLLECTIONS(
            vertexBuffer,
            vertexBuffer + expectedVertexBufferSize,
            expectedVertexBuffer,
            expectedVertexBuffer + expectedVertexBufferSize
        );

        const auto expectedIndexBufferSize = indexBufferContent.size() / 2;
        const auto indexBuffer = reinterpret_cast<const uint16_t*>(writer.pages()[0].indices.data());
        const auto expectedIndexBuffer = reinterpret_cast<const uint16_t*>(indexBufferContent.data());

        BOOST_CHECK_EQUAL_COLLECTIONS(
            indexBuffer,
            indexBuffer + expectedIndexBufferSize,
            expectedIndexBuffer,
            expectedIndexBuffer + expectedIndexBufferSize
        );

        BOOST_CHECK_EQUAL(primitives.size(), 3794);
        BOOST_CHECK(primitives[0] == (WrittenPrimitive{0, 10912, 0, 744, 0}));
        BOOST_CHECK(primitives[1499] == (WrittenPrimitive{414260, 176, 28416, 12, 0}));
        BOOST_CHECK(primitives[3793] == (WrittenPrimitive{978252, 176, 67068, 12, 0}));
    }

    {
        const auto encodedTileContent = testUtils::readFile(TILE_FILE2);
        const auto tile = Tile{79317,41144,17};
        const auto protoTile = proto::unpackTile(
            NULL,
            encodedTileContent.size(),
            reinterpret_cast<const unsigned char *>(encodedTileContent.data())
        );
        const auto protoPresentation = protoTile -> presentation[0];

        auto polylines = primitiveExtractors::extractPolylines(tile, *protoTile, *protoPresentation);

        PolylineBufferWriter writer;

        // should not fail
        writer.writePolylines(polylines);
    }

    {
        const auto encodedTileContent = testUtils::readFile(TILE_FILE3);
        const auto tile = Tile{19807, 10273, 15};
        const auto protoTile = proto::unpackTile(
            NULL,
            encodedTileContent.size(),
            reinterpret_cast<const unsigned char *>(encodedTileContent.data())
        );
        const auto protoPresentation = protoTile -> presentation[0];

        auto polylines = primitiveExtractors::extractPolylines(tile, *protoTile, *protoPresentation);

        PolylineBufferWriter writer;

        // should not fail
        writer.writePolylines(polylines);
    }
}

}
