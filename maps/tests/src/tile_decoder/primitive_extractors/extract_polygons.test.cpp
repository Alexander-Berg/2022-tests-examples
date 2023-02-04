#include <vector>
#include <boost/test/unit_test.hpp>
#include <boost/format.hpp>
#include "../../../src/tile_decoder/primitive_extractors/extract_polygons.h"
#include "../../../utils/file.h"
#include "../primitives/polygon.h"

namespace yandex::maps::jsapi::vector::tileDecoder::primitiveExtractors::test {

namespace utils = yandex::maps::jsapi::vector::test::utils;
namespace primitives = yandex::maps::jsapi::vector::tileDecoder::primitives;

void extractPolygonsTest() {
    const auto file = utils::readFile("./tile_decoder/vmap2_tiles/tile_2475_1284_12_new.pb.bin");
    const auto tile = Tile{2475, 1284, 12};
    const auto protoTile = proto::unpackTile(NULL, file.size(), reinterpret_cast<const unsigned char *>(file.data()));
    const auto protoPresentation = protoTile -> presentation[0];

    const auto polygons = extractPolygons(tile, *protoTile, *protoPresentation);
    BOOST_CHECK_EQUAL(polygons.size(), 322);

    BOOST_CHECK(polygons[0].vertexRings.size() == 1);
    BOOST_CHECK(polygons[0].vertexRings[0].size() == 4);
    BOOST_CHECK(polygons[0].vertexRings[0][0] == (WorldCoordinates{0.20849608629919203, 0.37304688245080797}));
    BOOST_CHECK(polygons[0].vertexRings[0][3] == (WorldCoordinates{0.20849608629919203, 0.37255860120080797}));
    BOOST_CHECK(polygons[0].zIndex == 42);
    BOOST_CHECK(!polygons[0].style.hasPattern);

    BOOST_CHECK(polygons[19].vertexRings.size() == 2);
    BOOST_CHECK(polygons[19].vertexRings[0].size() == 25);
    BOOST_CHECK(polygons[19].vertexRings[1].size() == 5);
    BOOST_CHECK(polygons[19].vertexRings[0][0] == (WorldCoordinates{0.2086420774307039, 0.372705873871292}));
    BOOST_CHECK(polygons[19].vertexRings[0][12] == (WorldCoordinates{0.208740234375, 0.37280274927661594}));
    BOOST_CHECK(polygons[19].vertexRings[0][24] == (WorldCoordinates{0.2086586629292619, 0.37269736504858164}));
    BOOST_CHECK(polygons[19].vertexRings[1][0] == (WorldCoordinates{0.20866650117925428, 0.3727133097776536}));
    BOOST_CHECK(polygons[19].vertexRings[1][4] == (WorldCoordinates{0.20868600739453946, 0.3727271384772599}));
    BOOST_CHECK(polygons[19].zIndex == 63);
    BOOST_CHECK(!polygons[19].style.hasPattern);

    BOOST_CHECK(polygons[321].vertexRings.size() == 2);
    BOOST_CHECK(polygons[321].vertexRings[0].size() == 15);
    BOOST_CHECK(polygons[321].vertexRings[1].size() == 7);
    BOOST_CHECK(polygons[321].vertexRings[0][0] == (WorldCoordinates{0.20861246791980118, 0.3730169600059702}));
    BOOST_CHECK(polygons[321].vertexRings[0][14] == (WorldCoordinates{0.20862255631380275, 0.37299772201977216}));
    BOOST_CHECK(polygons[321].vertexRings[1][0] == (WorldCoordinates{0.20862245200249108, 0.3730168258914266}));
    BOOST_CHECK(polygons[321].vertexRings[1][6] == (WorldCoordinates{0.20862596878385647, 0.37301110367089985}));
    BOOST_CHECK(polygons[321].zIndex == 378);
    BOOST_CHECK(!polygons[321].style.hasPattern);

    proto::freeUnpackedTile(protoTile, NULL);
}

}
