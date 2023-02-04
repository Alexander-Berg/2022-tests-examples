#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <maps/analyzer/tools/mapbox_quality/lib/graph.h>

namespace mq = maps::analyzer::tools::mapbox_quality;

const std::string OSM_GRAPH_FILE = BinaryPath("maps/analyzer/tools/mapbox_quality/tests/data/park_kultury.osm");

struct TestMapObject {
    osmium::object_id_type id;
    osmium::Location location;
};

TestMapObject nearMetro {
    577370071,
    {37.5936252, 55.7349402}
};

TestMapObject redRoseSpar {
    912959817,
    {37.5890022, 55.7340273}
};

maps::geolib3::BoundingBox redRoseBoundingBox {
    maps::geolib3::Point2{37.587035, 55.732829},
    maps::geolib3::Point2{37.589727, 55.734792}
};

Y_UNIT_TEST_SUITE(test_graph)
{
    Y_UNIT_TEST(test_location_cache)
    {
        osmium::io::Reader reader{OSM_GRAPH_FILE, osmium::osm_entity_bits::node};
        mq::LocationCache cache;
        cache.fill(reader);
        reader.close();

        EXPECT_TRUE(cache.has(nearMetro.id));
        EXPECT_EQ(cache.get(nearMetro.id), nearMetro.location);
        EXPECT_TRUE(cache.has(redRoseSpar.id));
        EXPECT_EQ(cache.get(redRoseSpar.id), redRoseSpar.location);
        EXPECT_FALSE(cache.has(-1));
    }

    Y_UNIT_TEST(test_map_borders)
    {
        osmium::io::Reader reader{OSM_GRAPH_FILE, osmium::osm_entity_bits::node};
        mq::LocationCache cache{redRoseBoundingBox};
        cache.fill(reader);
        reader.close();

        EXPECT_TRUE(cache.has(redRoseSpar.id));
        EXPECT_EQ(cache.get(redRoseSpar.id), redRoseSpar.location);
        EXPECT_FALSE(cache.has(nearMetro.id));
    }
}
