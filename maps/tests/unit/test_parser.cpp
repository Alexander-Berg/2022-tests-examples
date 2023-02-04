#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <maps/analyzer/tools/mapbox_quality/lib/parser.h>

namespace mq = maps::analyzer::tools::mapbox_quality;

const std::string TEST_DATA_ROOT = "maps/analyzer/tools/mapbox_quality/tests/data";
const std::string OSM_GRAPH_FILE = BinaryPath(TEST_DATA_ROOT + "/park_kultury.osm");
const std::string SPEED_DATA_FILE = BinaryPath(TEST_DATA_ROOT + "/live_data/2020/4/11/12/1/120310101011321.csv.gz");

Y_UNIT_TEST_SUITE(test_parser)
{
    Y_UNIT_TEST(test_file_reading)
    {
        osmium::io::Reader reader{OSM_GRAPH_FILE, osmium::osm_entity_bits::node};
        mq::LocationCache cache;
        cache.fill(reader);
        reader.close();

        mq::CompressedFileReader typicalReader{std::string{SPEED_DATA_FILE}};
        auto lines = mq::readLines<mq::LiveSpeedData>(typicalReader, cache);
        EXPECT_EQ(lines.size(), 3u);
        EXPECT_EQ(lines[0].speedData, 10.);
        EXPECT_EQ(lines[1].speedData, 15.);
        EXPECT_EQ(lines[2].speedData, 20.);
    }
}
