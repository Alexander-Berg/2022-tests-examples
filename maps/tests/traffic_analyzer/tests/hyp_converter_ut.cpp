#include <maps/wikimap/mapspro/tools/traffic_analyzer/lib/hyp_converter.h>
#include <maps/wikimap/mapspro/tools/traffic_analyzer/lib/revision_roads_miner.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <yandex/maps/wiki/revision/revisionid.h>

#include <maps/libs/ymapsdf/include/rd.h>

#include <algorithm>

namespace bg = boost::gregorian;
namespace mwr = maps::wiki::revision;
namespace mwt = maps::wiki::traffic_analyzer;
namespace mg = maps::geolib3;

const std::string ANY_VERSION = "18.01.31";

const bg::date_period ONE_DAY_PERIOD(
    bg::date(2018, bg::Jan, 30), bg::date(2018, bg::Jan, 31));

const uint32_t ANY_FUNCLASS = 7;

using roadIdToGeomMap = std::unordered_map<uint32_t, mg::Polyline2>;

/////////////
/// Stubs ///
/////////////

class GraphGeomStub : public mwt::IGraph
{
public:
    GraphGeomStub(roadIdToGeomMap roadIdToGeom) :
        roadIdToGeom_(std::move(roadIdToGeom)) {}

    const std::string& version() const override { return ANY_VERSION; }

    mwt::GraphRoadInfo roadInfo(uint32_t roadId) const override
    {
        return {ANY_FUNCLASS, roadIdToGeom_.at(roadId)};
    }

    std::vector<mwt::TwoWayRoadIds>
    getTwoWayRoads(const maps::geolib3::BoundingBox&) const override
    {
        return {};
    }

private:
    roadIdToGeomMap roadIdToGeom_;
};


class RevisionRoadsMinerStub : public mwt::IRevisionRoadsMiner
{
public:
    RevisionRoadsMinerStub(std::vector<mwt::RevisionRoad> roads) :
        roads_(std::move(roads))
    {}

    std::vector<mwt::RevisionRoad>
    getRoadsFromBox(const mg::BoundingBox& boxMerc) const override
    {
        std::vector<mwt::RevisionRoad> inBox;
        std::copy_if(roads_.begin(), roads_.end(), std::back_inserter(inBox),
            [&](const mwt::RevisionRoad& road) {
                return mg::spatialRelation(
                    boxMerc, road.geomMerc(), mg::SpatialRelation::Intersects);
            }
        );
        return inBox;
    }

private:
    std::vector<mwt::RevisionRoad> roads_;
};

/////////////////////////////////
/// Concrete graph hypothesis ///
/////////////////////////////////

const uint32_t ROAD_GRAPH_ID = 1;
const double MATCH_RATIO = 0.9;
const uint32_t BOTH_EDGES_TRACKS = 10;

// Geo polyline below corresponds to {100, 100}, {200, 200} polyline in Mercator
//
const mg::Polyline2 ROAD_GRAPH_POLYLINE_GEO(
    std::vector<mg::Point2>{
        {0.000898315, 0.000904369},
        {0.00179663, 0.00180874}
    }
);

const mwt::OnewayHypothesisGraph HYP_GRAPH {
    ROAD_GRAPH_ID,
    mwt::RoadTraffic{
        BOTH_EDGES_TRACKS,
        MATCH_RATIO,
        ANY_VERSION,
        ONE_DAY_PERIOD
    }
};

const GraphGeomStub GRAPH(
    roadIdToGeomMap{{ROAD_GRAPH_ID, ROAD_GRAPH_POLYLINE_GEO}});

/////////////
/// Tests ///
/////////////

TEST(hyp_converter, no_roads_in_box)
{
    auto revMiner = std::make_unique<RevisionRoadsMinerStub>(
        std::vector<mwt::RevisionRoad>()
    );
    mwt::OnewayHypothesisConverter conv(GRAPH, std::move(revMiner));

    ASSERT_TRUE(conv.convert(HYP_GRAPH) == boost::none);
}

TEST(hyp_converter, hyp_and_graph_versions_diff)
{
    auto revMiner = std::make_unique<RevisionRoadsMinerStub>(
        std::vector<mwt::RevisionRoad>()
    );
    mwt::OnewayHypothesisConverter conv(GRAPH, std::move(revMiner));

    mwt::OnewayHypothesisGraph hypWithBadVersion{
        ROAD_GRAPH_ID, mwt::RoadTraffic{
            BOTH_EDGES_TRACKS, MATCH_RATIO, "18.02.05-0", ONE_DAY_PERIOD
        }
    };

    ASSERT_THROW(conv.convert(hypWithBadVersion), maps::Exception);
}

TEST(hyp_converter, single_rev_road_in_box)
{
    mwr::RevisionID revId(100500, 10050010);
    mg::Point2 p1(100, 180), p2(110, 190), p3(120, 200);
    mwt::RevisionRoad revRoad(revId, mg::Polyline2({p1, p2, p3}));

    auto revMiner = std::make_unique<RevisionRoadsMinerStub>(
        std::vector<mwt::RevisionRoad>{revRoad}
    );
    mwt::OnewayHypothesisConverter conv(GRAPH, std::move(revMiner));

    auto hypRev = conv.convert(HYP_GRAPH);
    ASSERT_TRUE(hypRev != boost::none);
    ASSERT(hypRev->revId == revId);
    ASSERT(hypRev->positionMerc == p2);
    ASSERT(hypRev->direction == maps::ymapsdf::rd::Direction::Forward);
}

TEST(hyp_converter, not_choosing_road_from_crossroads)
{
    mwr::RevisionID revIdShifted(111, 888);
    mwr::RevisionID revIdCrossroads(222, 999);
    mwt::RevisionRoad revRoadShifted(
        revIdShifted,
        mg::Polyline2(std::vector<mg::Point2>{{90, 100}, {190, 200}})
    );
    mwt::RevisionRoad revRoadCrossroads(
        revIdCrossroads,
        mg::Polyline2(std::vector<mg::Point2>{{190, 100}, {200, 200}})
    );

    auto revMiner = std::make_unique<RevisionRoadsMinerStub>(
        std::vector<mwt::RevisionRoad>{revRoadShifted, revRoadCrossroads}
    );
    mwt::OnewayHypothesisConverter conv(GRAPH, std::move(revMiner));

    auto hypRev = conv.convert(HYP_GRAPH);
    ASSERT_TRUE(hypRev != boost::none);
    ASSERT(hypRev->revId == revIdShifted);
}

TEST(hyp_converter, reverse_direction)
{
    mwr::RevisionID revIdReversed(111, 888);
    mwt::RevisionRoad revRoadReversed(
        revIdReversed,
        mg::Polyline2(std::vector<mg::Point2>{{200, 200}, {100, 100}})
    );

    auto revMiner = std::make_unique<RevisionRoadsMinerStub>(
        std::vector<mwt::RevisionRoad>{revRoadReversed}
    );
    mwt::OnewayHypothesisConverter conv(GRAPH, std::move(revMiner));

    auto hypRev = conv.convert(HYP_GRAPH);
    ASSERT_TRUE(hypRev != boost::none);
    ASSERT(hypRev->direction == maps::ymapsdf::rd::Direction::Backward);
}

TEST(hyp_converter, failing_convertion_case_when_midpoint_is_near_end)
{
    mwr::RevisionID revIdShifted(111, 888);
    mwr::RevisionID revIdCoinciding(222, 999);
    mwt::RevisionRoad revRoadShifted(
        revIdShifted,
        mg::Polyline2(std::vector<mg::Point2>{{80, 100}, {180, 200}})
    );
    mwt::RevisionRoad revRoadCoinciding(
        revIdCoinciding,
        mg::Polyline2({{100, 100}, {195, 195}, {200, 200}})
    );

    auto revMiner = std::make_unique<RevisionRoadsMinerStub>(
        std::vector<mwt::RevisionRoad>{revRoadShifted, revRoadCoinciding}
    );
    mwt::OnewayHypothesisConverter conv(GRAPH, std::move(revMiner));

    auto hypRev = conv.convert(HYP_GRAPH);
    ASSERT_TRUE(hypRev != boost::none);
    ASSERT(hypRev->revId == revIdShifted);
}
