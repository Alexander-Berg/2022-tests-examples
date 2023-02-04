#include <boost/test/included/unit_test.hpp>
#include <boost/bind.hpp>
#include <iostream>
#include <string>
#include "tile_decoder/tile_decoder.test.h"
#include "tile_decoder/primitive_extractors/extract_polylines.test.h"
#include "tile_decoder/primitive_extractors/extract_polygons.test.h"
#include "tile_decoder/buffer_writers/buffer_writer.test.h"
#include "tile_decoder/buffer_writers/polyline_buffer_writer.test.h"
#include "tile_decoder/buffer_writers/polygon_buffer_writer.test.h"

namespace tile_decoder = yandex::maps::jsapi::vector::tileDecoder;
namespace primitive_extractors = yandex::maps::jsapi::vector::tileDecoder::primitiveExtractors;
namespace buffer_writer = yandex::maps::jsapi::vector::tileDecoder::bufferWriters;

boost::unit_test::test_suite* init_unit_test_suite(int argc, char* argv[]) {
    boost::unit_test::test_suite* all = BOOST_TEST_SUITE("tile_decoder_tests");

    all->add(BOOST_TEST_CASE(tile_decoder::test::decodeTileTest));
    all->add(BOOST_TEST_CASE(primitive_extractors::test::extractPolylinesTest));
    all->add(BOOST_TEST_CASE(primitive_extractors::test::extractPolygonsTest));
    all->add(BOOST_TEST_CASE(buffer_writer::test::bufferWriterTest));
    all->add(BOOST_TEST_CASE(buffer_writer::test::polylineBufferWriterTest));
    all->add(BOOST_TEST_CASE(buffer_writer::test::polygonBufferWriterTest));
    boost::unit_test::framework::master_test_suite().add(all);

    return 0;
}
