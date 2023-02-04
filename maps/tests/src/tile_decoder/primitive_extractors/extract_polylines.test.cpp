#include <vector>
#include <boost/test/unit_test.hpp>
#include <boost/format.hpp>
#include "../../../src/tile_decoder/primitive_extractors/extract_polylines.h"
#include "../../../utils/file.h"
#include "../primitives/polyline.h"

namespace yandex::maps::jsapi::vector::tileDecoder::primitiveExtractors::test {

namespace utils = yandex::maps::jsapi::vector::test::utils;
namespace primitives = yandex::maps::jsapi::vector::tileDecoder::primitives;

void extractPolylinesTest() {
    const auto file = utils::readFile("./tile_decoder/vmap2_tiles/tile_2475_1284_12.pb.bin");
    const auto tile = Tile{2475, 1284, 12};
    const auto protoTile = proto::unpackTile(NULL, file.size(), reinterpret_cast<const unsigned char *>(file.data()));
    const auto protoPresentation = protoTile -> presentation[0];

    const auto polylines = extractPolylines(tile, *protoTile, *protoPresentation);

    BOOST_CHECK_EQUAL(polylines.size(), 2520);

    BOOST_CHECK(polylines[0].vertices.size() == 63);
    BOOST_CHECK(polylines[0].vertices[0] == (WorldCoordinates{0.20898436754919203, 0.37295610180640965}));
    BOOST_CHECK(polylines[0].vertices[40] == (WorldCoordinates{0.20856593017317346, 0.37271986648867383}));
    BOOST_CHECK(polylines[0].vertices[62] == (WorldCoordinates{0.20868737834320733, 0.3730087492155789}));
    BOOST_CHECK(polylines[0].zIndex == 660);
    BOOST_CHECK(polylines[0].style.hasInline);
    BOOST_CHECK(!polylines[0].style.hasOutline);
    BOOST_CHECK(polylines[0].style.inlineStyle.width == 1.2000000476837158);
    BOOST_CHECK(polylines[0].style.inlineStyle.joins == primitives::PolylineStyle::LineJoin::MITER);
    BOOST_CHECK(polylines[0].style.inlineStyle.startCap == primitives::PolylineStyle::LineCap::BUTT);
    BOOST_CHECK(polylines[0].style.inlineStyle.endCap == primitives::PolylineStyle::LineCap::BUTT);

    BOOST_CHECK(polylines[1100].vertices.size() == 3);
    BOOST_CHECK(polylines[1100].vertices[0] == (WorldCoordinates{0.20851246317512664, 0.3730014027189131}));
    BOOST_CHECK(polylines[1100].vertices[1] == (WorldCoordinates{0.2085144003852008, 0.3730015666366886}));
    BOOST_CHECK(polylines[1100].vertices[2] == (WorldCoordinates{0.20853751279154714, 0.372988825755047}));
    BOOST_CHECK(polylines[1100].zIndex == 1200);
    BOOST_CHECK(polylines[1100].style.hasInline);
    BOOST_CHECK(polylines[1100].style.hasOutline);
    BOOST_CHECK(polylines[1100].style.inlineStyle.width == 2.200000047683716);
    BOOST_CHECK(polylines[1100].style.inlineStyle.joins == primitives::PolylineStyle::LineJoin::MITER);
    BOOST_CHECK(polylines[1100].style.inlineStyle.startCap == primitives::PolylineStyle::LineCap::BUTT);
    BOOST_CHECK(polylines[1100].style.inlineStyle.endCap == primitives::PolylineStyle::LineCap::BUTT);
    BOOST_CHECK(polylines[1100].style.outlineStyle.width == 3.200000047683716);
    BOOST_CHECK(polylines[1100].style.outlineStyle.joins == primitives::PolylineStyle::LineJoin::MITER);
    BOOST_CHECK(polylines[1100].style.outlineStyle.startCap == primitives::PolylineStyle::LineCap::BUTT);
    BOOST_CHECK(polylines[1100].style.outlineStyle.endCap == primitives::PolylineStyle::LineCap::BUTT);

    BOOST_CHECK(polylines[2519].vertices.size() == 2);
    BOOST_CHECK(polylines[2519].vertices[0] == (WorldCoordinates{0.20851372981248284, 0.37303640661479154}));
    BOOST_CHECK(polylines[2519].vertices[1] == (WorldCoordinates{0.20851663562759407, 0.37304688245080797}));
    BOOST_CHECK(polylines[2519].zIndex == 1000);
    BOOST_CHECK(polylines[2519].style.hasInline);
    BOOST_CHECK(!polylines[2519].style.hasOutline);
    BOOST_CHECK(polylines[2519].style.inlineStyle.width == 3.0);
    BOOST_CHECK(polylines[2519].style.inlineStyle.joins == primitives::PolylineStyle::LineJoin::MITER);
    BOOST_CHECK(polylines[2519].style.inlineStyle.startCap == primitives::PolylineStyle::LineCap::BUTT);
    BOOST_CHECK(polylines[2519].style.inlineStyle.endCap == primitives::PolylineStyle::LineCap::BUTT);

    proto::freeUnpackedTile(protoTile, NULL);
}

}
