#include <boost/test/unit_test.hpp>
#include "tile_decoder.test.h"
#include "../../../src/tile_decoder/tile_decoder.h"
#include "../../../src/common/tile.h"
#include "../../utils/file.h"

namespace yandex::maps::jsapi::vector::tileDecoder::test {

namespace tile_decoder = yandex::maps::jsapi::vector::tileDecoder;
namespace testUtils = yandex::maps::jsapi::vector::test::utils;

void decodeTileTest() {
    Tile tile{2475, 1284, 12};
    auto file = testUtils::readFile("./tile_decoder/vmap2_tiles/tile_2475_1284_12.pb.bin");

    //BOOST_CHECK_EQUAL(decodeTile(tile, file.data, file.size), -1);
}

}
