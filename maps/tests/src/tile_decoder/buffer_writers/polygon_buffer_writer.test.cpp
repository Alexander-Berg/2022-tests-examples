#include <boost/test/unit_test.hpp>
#include "../../../src/common/tile.h"
#include "../../../src/tile_decoder/primitive_extractors/extract_polygons.h"
#include "../../../src/tile_decoder/buffer_writers/polygon_buffer_writer.h"
#include "../../../src/tile_decoder/buffer_writers/extruded_polygon_buffer_writer.h"
#include "../../../src/tile_decoder/proto/type_aliases.h"
#include "../../../utils/file.h"
#include "buffer_writer.h"
#include "polygon_buffer_writer.test.h"

namespace yandex::maps::jsapi::vector::tileDecoder::bufferWriters::test {

namespace testUtils = yandex::maps::jsapi::vector::test::utils;
namespace proto = yandex::maps::jsapi::vector::tileDecoder::proto;
namespace primitiveExtractors = yandex::maps::jsapi::vector::tileDecoder::primitiveExtractors;

const auto TILE_FILE = "./tile_decoder/vmap2_tiles/tile_2475_1284_12_new.pb.bin";
const auto TILE_FILE2 = "./tile_decoder/vmap2_tiles/tile_79317_41144_17.pb.bin";
const auto TILE_FILE3 = "./tile_decoder/vmap2_tiles/tile_19807_10273_15.pb.bin";
const auto TILE_FILE4 = "./tile_decoder/vmap2_tiles/tile_39617_20540_16.pb.bin";
const auto TILE_VERTEX_BUFFER_0 = "./tile_decoder/buffer_writers/written_buffers/tile_2475_1284_12_new.polygonVertexBuffer.bin";
const auto TILE_INDEX_BUFFER_0 = "./tile_decoder/buffer_writers/written_buffers/tile_2475_1284_12_new.polygonIndexBuffer.bin";
const auto TILE_TRANSPARENT_VERTEX_BUFFER_0 = "./tile_decoder/buffer_writers/written_buffers/tile_2475_1284_12_new.transparentPolygonVertexBuffer.bin";
const auto TILE_TRANSPARENT_INDEX_BUFFER_0 = "./tile_decoder/buffer_writers/written_buffers/tile_2475_1284_12_new.transparentPolygonIndexBuffer.bin";
const auto TILE_EXTRUDED_VERTEX_BUFFER_4 = "./tile_decoder/buffer_writers/written_buffers/tile_39617_20540_16.extrudedPolygonVertexBuffer.bin";
const auto TILE_EXTRUDED_INDEX_BUFFER_4 = "./tile_decoder/buffer_writers/written_buffers/tile_39617_20540_16.extrudedPolygonIndexBuffer.bin";

bool filterTransparentPolygons(const primitives::Polygon& polygon)
{
    return (((polygon.style.color) & 0x000000FF) != 0x000000FF);
}

void polygonBufferWriterTest() {
    {
        const auto encodedTileContent = testUtils::readFile(TILE_FILE);
        const auto polygonVertexBufferContent = testUtils::readFile(TILE_VERTEX_BUFFER_0);
        const auto polygonIndexBufferContent = testUtils::readFile(TILE_INDEX_BUFFER_0);
        const auto transparentPolygonVertexBufferContent = testUtils::readFile(TILE_TRANSPARENT_VERTEX_BUFFER_0);
        const auto transparentPolygonIndexBufferContent = testUtils::readFile(TILE_TRANSPARENT_INDEX_BUFFER_0);
        const auto tile = Tile{2475, 1284, 12};
        const auto protoTile = proto::unpackTile(
            NULL,
            encodedTileContent.size(),
            reinterpret_cast<const unsigned char *>(encodedTileContent.data())
        );
        const auto protoPresentation = protoTile->presentation[0];

        auto polygons = primitiveExtractors::extractPolygons(tile, *protoTile, *protoPresentation);

        std::vector<primitives::Polygon> transparentPolygons;
        auto position = std::stable_partition(
            polygons.begin(),
            polygons.end(),
            [&](const auto& x) { return !filterTransparentPolygons(x); });
        transparentPolygons.insert(transparentPolygons.end(), std::make_move_iterator(position),
                                   std::make_move_iterator(polygons.end()));
        polygons.erase(position, polygons.end());

        PolygonBufferWriter polygonWriter;
        PolygonBufferWriter transparentPolygonWriter;

        auto polygonPrimitives = polygonWriter.writePolygons(polygons);
        auto transparentPolygonPrimitives = transparentPolygonWriter.writePolygons(transparentPolygons);

        BOOST_CHECK_EQUAL(polygonWriter.pages().size(), 1);
        BOOST_CHECK_EQUAL(transparentPolygonWriter.pages().size(), 1);

        const auto polygonExpectedVertexBufferSize = polygonVertexBufferContent.size() / 4;
        const auto polygonExpectedVertexBuffer = reinterpret_cast<const uint32_t*>(polygonVertexBufferContent.data());
        uint32_t* polygonVertexBuffer = const_cast<uint32_t*>(
            reinterpret_cast<const uint32_t*>(polygonWriter.pages()[0].vertices.data())
        );
        BOOST_CHECK_EQUAL_COLLECTIONS(
            polygonVertexBuffer,
            polygonVertexBuffer + polygonExpectedVertexBufferSize,
            polygonExpectedVertexBuffer,
            polygonExpectedVertexBuffer + polygonExpectedVertexBufferSize
        );

        const auto polygonExpectedIndexBufferSize = polygonIndexBufferContent.size() / 2;
        const auto polygonExpectedIndexBuffer = reinterpret_cast<const uint16_t*>(polygonIndexBufferContent.data());
        const auto polygonIndexBuffer = reinterpret_cast<const uint16_t*>(polygonWriter.pages()[0].indices.data());
        BOOST_CHECK_EQUAL_COLLECTIONS(
            polygonIndexBuffer,
            polygonIndexBuffer + polygonExpectedIndexBufferSize,
            polygonExpectedIndexBuffer,
            polygonExpectedIndexBuffer + polygonExpectedIndexBufferSize
        );

        const auto transparentPolygonExpectedVertexBufferSize = transparentPolygonVertexBufferContent.size() / 4;
        const auto transparentPolygonExpectedVertexBuffer = reinterpret_cast<const uint32_t*>(transparentPolygonVertexBufferContent.data());
        uint32_t* transparentPolygonVertexBuffer = const_cast<uint32_t*>(
            reinterpret_cast<const uint32_t*>(transparentPolygonWriter.pages()[0].vertices.data())
        );
        BOOST_CHECK_EQUAL_COLLECTIONS(
            transparentPolygonVertexBuffer,
            transparentPolygonVertexBuffer + transparentPolygonExpectedVertexBufferSize,
            transparentPolygonExpectedVertexBuffer,
            transparentPolygonExpectedVertexBuffer + transparentPolygonExpectedVertexBufferSize
        );

        const auto transparentPolygonExpectedIndexBufferSize = transparentPolygonIndexBufferContent.size() / 2;
        const auto transparentPolygonExpectedIndexBuffer = reinterpret_cast<const uint16_t*>(transparentPolygonIndexBufferContent.data());
        const auto transparentPolygonIndexBuffer = reinterpret_cast<const uint16_t*>(transparentPolygonWriter.pages()[0].indices.data());
        BOOST_CHECK_EQUAL_COLLECTIONS(
            transparentPolygonIndexBuffer,
            transparentPolygonIndexBuffer + transparentPolygonExpectedIndexBufferSize,
            transparentPolygonExpectedIndexBuffer,
            transparentPolygonExpectedIndexBuffer + transparentPolygonExpectedIndexBufferSize
        );
        BOOST_CHECK_EQUAL(polygonPrimitives.size(), 307);
        BOOST_CHECK(polygonPrimitives[0] == (WrittenPrimitive{0, 80, 0, 12, 0}));
        BOOST_CHECK(polygonPrimitives[149] == (WrittenPrimitive{32100, 120, 7950, 24, 0}));
        BOOST_CHECK(polygonPrimitives[306] == (WrittenPrimitive{69720, 440, 17448, 132, 0}));

        BOOST_CHECK_EQUAL(transparentPolygonPrimitives.size(), 15);
        BOOST_CHECK(transparentPolygonPrimitives[0] == (WrittenPrimitive{0, 80, 0, 12, 0}));
        BOOST_CHECK(transparentPolygonPrimitives[7] == (WrittenPrimitive{560, 80, 84, 12, 0}));
        BOOST_CHECK(transparentPolygonPrimitives[14] == (WrittenPrimitive{1200, 120, 192, 24, 0}));
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

        auto polygons = primitiveExtractors::extractPolygons(tile, *protoTile, *protoPresentation);

        std::vector<primitives::Polygon> transparentPolygons;
        auto position = std::stable_partition(polygons.begin(), polygons.end(),
                                              [&](const auto& x) { return !filterTransparentPolygons(x); });
        transparentPolygons.insert(transparentPolygons.end(), std::make_move_iterator(position),
                                   std::make_move_iterator(polygons.end()));
        polygons.erase(position, polygons.end());

        PolygonBufferWriter polygonWriter;
        PolygonBufferWriter transparentPolygonWriter;

        // should not fail
        polygonWriter.writePolygons(polygons);
        transparentPolygonWriter.writePolygons(transparentPolygons);
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

        auto polygons = primitiveExtractors::extractPolygons(tile, *protoTile, *protoPresentation);

        std::vector<primitives::Polygon> transparentPolygons;
        auto position = std::stable_partition(polygons.begin(), polygons.end(),
                                              [&](const auto& x) { return !filterTransparentPolygons(x); });
        transparentPolygons.insert(transparentPolygons.end(), std::make_move_iterator(position),
                                   std::make_move_iterator(polygons.end()));
        polygons.erase(position, polygons.end());

        PolygonBufferWriter polygonWriter;
        PolygonBufferWriter transparentPolygonWriter;

        // should not fail
        polygonWriter.writePolygons(polygons);
        transparentPolygonWriter.writePolygons(transparentPolygons);
    }
    {
        const auto encodedTileContent = testUtils::readFile(TILE_FILE4);
        const auto extrudedPolygonVertexBufferContent = testUtils::readFile(TILE_EXTRUDED_VERTEX_BUFFER_4);
        const auto extrudedPolygonIndexBufferContent = testUtils::readFile(TILE_EXTRUDED_INDEX_BUFFER_4);
        const auto tile = Tile{39617, 20540, 16};
        const auto protoTile = proto::unpackTile(
            NULL,
            encodedTileContent.size(),
            reinterpret_cast<const unsigned char *>(encodedTileContent.data())
        );
        const auto protoPresentation = protoTile->presentation[0];
        auto polygons = primitiveExtractors::extractPolygons(tile, *protoTile, *protoPresentation);

        ExtrudedPolygonBufferWriter extrudedPolygonWriter;
        std::vector<WrittenPrimitive> extrudedPolygonPrimitives;
        std::vector<primitives::ExternalMeshDescription> externalMeshes;
        for (const auto& polygon : polygons) {
            if (polygon.height != 0) {
                if (!polygon.hasExternalMesh) {
                    extrudedPolygonPrimitives.push_back(extrudedPolygonWriter.writePolygon(polygon));
                } else {
                    externalMeshes.push_back(polygon.externalMesh);
                }
            }
        }

        BOOST_CHECK_EQUAL(extrudedPolygonWriter.pages().size(), 1);

        const auto extrudedPolygonExpectedVertexBufferSize = extrudedPolygonVertexBufferContent.size() / 4;
        const auto extrudedPolygonExpectedVertexBuffer = reinterpret_cast<const uint32_t*>(extrudedPolygonVertexBufferContent.data());
        uint32_t* extrudedPolygonVertexBuffer = const_cast<uint32_t*>(
            reinterpret_cast<const uint32_t*>(extrudedPolygonWriter.pages()[0].vertices.data())
        );
        BOOST_CHECK_EQUAL_COLLECTIONS(
            extrudedPolygonVertexBuffer,
            extrudedPolygonVertexBuffer + extrudedPolygonExpectedVertexBufferSize,
            extrudedPolygonExpectedVertexBuffer,
            extrudedPolygonExpectedVertexBuffer + extrudedPolygonExpectedVertexBufferSize
        );

        const auto extrudedPolygonExpectedIndexBufferSize = extrudedPolygonIndexBufferContent.size() / 2;
        const auto extrudedPolygonExpectedIndexBuffer = reinterpret_cast<const uint16_t*>(extrudedPolygonIndexBufferContent.data());
        const auto extrudedPolygonIndexBuffer = reinterpret_cast<const uint16_t*>(extrudedPolygonWriter.pages()[0].indices.data());
        BOOST_CHECK_EQUAL_COLLECTIONS(
            extrudedPolygonIndexBuffer,
            extrudedPolygonIndexBuffer + extrudedPolygonExpectedIndexBufferSize,
            extrudedPolygonExpectedIndexBuffer,
            extrudedPolygonExpectedIndexBuffer + extrudedPolygonExpectedIndexBufferSize
        );

        BOOST_CHECK_EQUAL(extrudedPolygonPrimitives.size(), 95);
        BOOST_CHECK(extrudedPolygonPrimitives[0] == (WrittenPrimitive{0, 160, 0, 60, 0}));
        BOOST_CHECK(extrudedPolygonPrimitives[47] == (WrittenPrimitive{10920, 160, 4350, 60, 0}));
        BOOST_CHECK(extrudedPolygonPrimitives[94] == (WrittenPrimitive{28960, 1680, 11904, 744, 0}));

        BOOST_CHECK_EQUAL(externalMeshes.size(), 1);
        BOOST_CHECK(externalMeshes[0].meshId == "DWHJ3KDO5HW7AVL7ZGVART4MACNOXEYL");
        BOOST_CHECK(externalMeshes[0].objectId == "23017994");
        BOOST_CHECK(externalMeshes[0].bbox.minX == 0.20901549376519368);
        BOOST_CHECK(externalMeshes[0].bbox.maxX == 0.20902402680302995);
        BOOST_CHECK(externalMeshes[0].bbox.minY == 0.3731423491877487);
        BOOST_CHECK(externalMeshes[0].bbox.maxY == 0.37315996103510557);
        BOOST_CHECK(externalMeshes[0].bbox.minZ == 0.000003193602651663241);
        BOOST_CHECK(externalMeshes[0].bbox.maxZ == 0);
    }
}

}
