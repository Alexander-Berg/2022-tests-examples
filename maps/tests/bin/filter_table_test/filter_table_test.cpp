#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <mapreduce/yt/interface/operation.h>

#include <maps/analyzer/toolkit/bin/filter_table/predicate.h>
#include <maps/analyzer/toolkit/bin/filter_table/by_bounding_box.h>
#include <maps/analyzer/toolkit/bin/filter_table/by_category.h>
#include <maps/analyzer/toolkit/bin/filter_table/by_region.h>
#include <maps/analyzer/toolkit/bin/filter_table/by_time_of_day.h>
#include <maps/analyzer/toolkit/bin/filter_table/by_time_period.h>
#include <maps/analyzer/toolkit/bin/filter_table/by_weekday.h>
#include <maps/analyzer/toolkit/bin/filter_table/by_custom_file.h>

#include <maps/analyzer/libs/common/include/io_helpers.h>
#include <yandex/maps/coverage5/coverage.h>
#include <maps/libs/geolib/include/test_tools/io_operations.h>

#include <map>
#include <vector>

namespace ma = maps::analyzer;
namespace geo = maps::geolib3;
namespace rg = maps::road_graph;

const std::string ROAD_GRAPH_FILE = BinaryPath("maps/data/test/graph3/road_graph.fb");
const std::string GRAPH_EDGES_PERSISTENT_INDEX_FILE = BinaryPath("maps/data/test/graph3/edges_persistent_index.fb");
const std::string GEOID_FILE = BinaryPath("maps/data/test/geoid/geoid.mms.1");
const std::string GEOBASE_FILE = BinaryPath("maps/data/test/geobase/geodata5.bin");

const rg::Graph graph(ROAD_GRAPH_FILE);
const rg::PersistentIndex persistentIndex(GRAPH_EDGES_PERSISTENT_INDEX_FILE);

namespace maps {
    namespace geolib3 {
        using io::operator >>;
    } // namespace maps
} // namespace geolib3

inline NYT::TNode node()
{
    return NYT::TNode::CreateMap();
}

inline NYT::TNode nodeNull()
{
    return NYT::TNode::CreateEntity();
}

using Nodes = std::vector<NYT::TNode>;
using Examples = std::vector<std::pair<NYT::TNode, bool>>;

template <class T>
inline void check(const T& predicate, const Examples& examples)
{
    for (const auto& ex : examples) {
        UNIT_ASSERT_EQUAL(predicate(ex.first), ex.second);
    }
}

inline ui64 toPersistentId(ui64 edgeId) {
    auto shortId = persistentIndex.findLongId(rg::EdgeId(edgeId));
    REQUIRE(shortId, "Can't find long id for: " << edgeId);
    return shortId->value();
}

Y_UNIT_TEST_SUITE(TestFilterTableSuite) {
    Y_UNIT_TEST(ByBboxUsePersistentId)
    {
        geo::BoundingBox bbox;
        std::istringstream iss_bbox("37.646608 55.778845 37.661285 55.771903");
        iss_bbox >> bbox;
        check(ByBoundingBox(bbox, &graph, &persistentIndex), {
            { node()("persistent_id", 10710410755214454439u)("segment_index", 0u), true },
            { node()("persistent_id", 7206252668709167927u)("segment_index", 0u), false },
        });
    }

    Y_UNIT_TEST(ByBboxUseLatLon)
    {
        geo::BoundingBox bbox;
        std::istringstream iss_bbox("37.646608 55.778845 37.661285 55.771903");
        iss_bbox >> bbox;
        check(ByBoundingBox(bbox, nullptr, nullptr), {
            { node()("lat", 55.7746)("lon", 37.6513), true },
            { node()("lat", 55.591)("lon", 37.7501), false },
        });
    }

    Y_UNIT_TEST(ByRegionUsePersistentId)
    {
        check(ByRegion({213}, GEOID_FILE, GEOBASE_FILE, &graph, &persistentIndex), {
            { node()("persistent_id", 10710410755214454439u), true },
            { node()("persistent_id", 7206252668709167927u), false },
        });
    }

    Y_UNIT_TEST(ByRegionUseRegion)
    {
        check(ByRegion({213,2}, GEOID_FILE, GEOBASE_FILE, nullptr, nullptr), {
            { node()("region_id", 0u), false },
            { node()("region_id", 1u), false },
            { node()("region_id", 2u), true },
            { node()("region_id", 1u), false },
            { node()("region_id", 2u), true },
            { node()("region_id", 213u), true },
            { node()("region_id", nodeNull()), false },
        });
    }

    Y_UNIT_TEST(ByCategory)
    {
        check(ByCategory({7,8}, &graph, &persistentIndex), {
            { node()("persistent_id", toPersistentId(204282u)), false },
            { node()("persistent_id", toPersistentId(143779u)), false },
            { node()("persistent_id", toPersistentId(143779u)), false },
            { node()("persistent_id", toPersistentId(199026u)), true },
            { node()("persistent_id", toPersistentId(100575u)), true },
            { node()("persistent_id", toPersistentId(197686u)), true },
            { node()("persistent_id", toPersistentId(156202u)), true },
            { node()("persistent_id", toPersistentId(8u)), true },
        });
    }

    Y_UNIT_TEST(ByCategoryMix)
    {
        check(ByCategory({7,8}, &graph, &persistentIndex), {
            { node()("persistent_id", toPersistentId(204282u)), false },
            { node()("category", 7u), true },
            { node()("category", 9u), false },
            { node()("persistent_id", toPersistentId(8u)), true },
            { node()("persistent_id", 10710410755214454439u), false },
            { node()("persistent_id", 6743281101179894802u), true },
        });
    }

    Y_UNIT_TEST(ByCustomFileByEdgeId)
    {
        std::string edge_ids = ArcadiaSourceRoot() + "/maps/analyzer/toolkit/tests/bin/data/edge_ids.txt";
        check(ByCustomFile(edge_ids), {
            { node()("edge_id", 204282u), false },
            { node()("edge_id", 143779u), true },
            { node()("edge_id", 199026u), false },
            { node()("edge_id", 100575u), true },
        });
    }

    Y_UNIT_TEST(ByCustomFileByPersistentId)
    {
        std::string persistent_ids = ArcadiaSourceRoot() + "/maps/analyzer/toolkit/tests/bin/data/persistent_ids.txt";
        check(ByCustomFile(persistent_ids), {
            { node()("persistent_id", 10710410755214454439u), false },
            { node()("persistent_id", 4962698367499090997u), true },
            { node()("persistent_id", 13403253638237282832u), false },
            { node()("persistent_id", 4863320839283909785u), true },
        });
    }

    Y_UNIT_TEST(ByTimeOfDay)
    {
        auto intervals = std::vector{
            boost::lexical_cast<TimeOfDayInterval>("-00:01"),
            boost::lexical_cast<TimeOfDayInterval>("00:03-00:04"),
            boost::lexical_cast<TimeOfDayInterval>("00:05-"),
        };
        check(ByTimeOfDay(intervals, "time"), {
            { node()("time", "20150824T000000"), true },
            { node()("time", "20150825T000100"), false },
            { node()("time", "20150826T000200"), false },
            { node()("time", "20150826T000300"), true },
            { node()("time", "20150826T000400"), false },
            { node()("time", "20150826T000500"), true },
        });
    }

    Y_UNIT_TEST(ByTimePeriod)
    {
        check(ByTimePeriod(ma::parseTimePeriod("20150102T013000 20150102T023000"), "isotime"), {
            { node()("isotime", "20150102T010000"), false },
            { node()("isotime", "20150102T020000"), true },
        });
    }

    Y_UNIT_TEST(ByCustomFileByVehicleId)
    {
        std::string vehicle_ids = ArcadiaSourceRoot() + "/maps/analyzer/toolkit/tests/bin/data/vehicle_ids.txt";
        check(ByCustomFile(vehicle_ids), {
            { node()("clid", "0")("uuid", "0"), false },
            { node()("clid", "1")("uuid", "1"), false },
            { node()("clid", "1")("uuid", "1"), false },
            { node()("clid", "2")("uuid", "4"), true },
            { node()("clid", "3")("uuid", "9"), true },
            { node()("clid", "4")("uuid", "16"), false },
            { node()("clid", "5")("uuid", "25"), true },
            { node()("clid", "8")("uuid", "64"), false },
            { node()("clid", "9")("uuid", "81"), false },
            { node()("clid", "10")("uuid", "100"), false },
            { node()("clid", "11")("uuid", "121"), true },
            { node()("clid", "12")("uuid", "144"), false },
        });
    }

    Y_UNIT_TEST(ByWeekday)
    {
        auto wdays = WeekdaySet{
            boost::lexical_cast<Weekday>("mon"),
            boost::lexical_cast<Weekday>("wed"),
            boost::lexical_cast<Weekday>("thu"),
        };
        check(ByWeekday(wdays, "time"), {
            { node()("time", "20150824T000000"), true },
            { node()("time", "20150825T000100"), false },
            { node()("time", "20150826T000200"), true },
        });
    }

    Y_UNIT_TEST(ByCustomFile)
    {
        std::string filename = ArcadiaSourceRoot() + "/maps/analyzer/toolkit/tests/bin/data/custom_file.txt";
        check(ByCustomFile(filename), {
            { node()("clid", "clid1")("uuid", "uuid1")("region", 213u), true },
            { node()("clid", "clid1")("uuid", "uuid2")("region", 2u), false },
            { node()("clid", "clid1")("uuid", "uuid3")("region", 1u), false },
            { node()("clid", "clid2")("uuid", "uuid4")("region", 213u), false },
            { node()("clid", "clid2")("uuid", "uuid5")("region", 2u), true },
            { node()("clid", "clid2")("uuid", "uuid6")("region", 1u), false },
            { node()("clid", "clid3")("uuid", "uuid7")("region", 213u), false },
            { node()("clid", "clid3")("uuid", "uuid8")("region", 2u), false },
            { node()("clid", "clid3")("uuid", "uuid9")("region", 1u), true },
        });
    }

    Y_UNIT_TEST(ByCustomFileByNonexistingColumn)
    {
        std::string filename = ArcadiaSourceRoot() + "/maps/analyzer/toolkit/tests/bin/data/custom_file.txt";
        check(ByCustomFile(filename), {
            { node()("clid", "clid1")("region", 213u), true },
            { node()("A", "clid1")("region", 2123u), false },
        });
    }
}
