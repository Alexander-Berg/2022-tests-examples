#define BOOST_TEST_MODULE integration_tests
#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MAIN

#include <../add_closure.h>

#include <yandex/maps/jams/router/jams.h>

#include <yandex/maps/mms/holder2.h>

#include <boost/optional.hpp>
#include <boost/test/unit_test.hpp>

#include <string>


using namespace maps::jams::common2;
using namespace maps::jams::static_graph2;

typedef maps::jams::static_graph::Id EdgeId;

const double CLOSURE_SPEED = 0.0;
const double FREE_SPEED = 20.0;
const double HARD_SPEED = 7.0;
const double JAM_SPEED = 2.0;

const uint32_t M_REGION = 213;

void checkJam(const mjr::Jams& jams,
              EdgeId edgeId,
              double correctSpeed)
{
    boost::optional<mjr::EdgeJam> jam = jams.jam(edgeId);
    BOOST_REQUIRE_EQUAL(static_cast<bool>(jam), true);
    BOOST_CHECK_EQUAL(jam->speed(), correctSpeed);
}

BOOST_AUTO_TEST_CASE( impose_closures )
{
    mjr::Jams jams;
    jams.addJam(0, HARD_SPEED, M_REGION);
    jams.addJam(1, FREE_SPEED, M_REGION);
    jams.addJam(2, JAM_SPEED, M_REGION);
    jams.addJam(3, FREE_SPEED, M_REGION);
    jams.addJam(4, FREE_SPEED, M_REGION);

    std::vector<jsg::SegmentId> closed;
    closed.push_back(SegmentId(0, 2));
    closed.push_back(SegmentId(4, 0));
    closed.push_back(SegmentId(0, 3));
    closed.push_back(SegmentId(7, 0));

    imposeClosures(closed, &jams);

    checkJam(jams, 0, CLOSURE_SPEED);
    checkJam(jams, 1, FREE_SPEED);
    checkJam(jams, 2, JAM_SPEED);
    checkJam(jams, 3, FREE_SPEED);
    checkJam(jams, 4, CLOSURE_SPEED);
    checkJam(jams, 7, CLOSURE_SPEED);
}

std::auto_ptr<JamData> generateJam(const jsg::Graph& graph,
                                   EdgeId edgeId,
                                   double speed,
                                   JamData::Severity severity)
{
    std::auto_ptr<JamData> ptr(new JamData());
    ptr->set_speed(speed);
    ptr->set_severity(severity);

    maps::geolib3::Polyline2 geometry = jsg::edgeGeometry(graph, edgeId);
    for (uint32_t i = 0; i < geometry.pointsNumber(); ++i) {
        maps::geolib3::Point2 point = geometry.pointAt(i);
        auto jamGeometry = ptr->add_geometry();
        jamGeometry->set_lon(point.x());
        jamGeometry->set_lat(point.y());
    }

    return ptr;
}

BOOST_AUTO_TEST_CASE( tie_closures )
{
    std::string testGraphPath = "/usr/share/yandex/maps/test-graph3/";
    std::string topologyFile = testGraphPath + "topology.mms.2";
    std::string dataFile = testGraphPath + "data.mms.2";
    std::string segmentsRtreeFile = testGraphPath + "segments_rtree.mms.2";

    jsg::GraphHolder graphHolder(topologyFile, dataFile);

    // real closures
    SourceJams closures;
    closures.push_back(generateJam(
        *graphHolder.get(), 8, CLOSURE_SPEED, JamData::CLOSED));
    closures.push_back(generateJam(
        *graphHolder.get(), 0, CLOSURE_SPEED, JamData::CLOSED));
    closures.push_back(generateJam(
        *graphHolder.get(), 1, CLOSURE_SPEED, JamData::CLOSED));

    mms::Holder2<jsp::GraphSegmentsRTree> segmentsRtree(segmentsRtreeFile);

    const double angleTolerance = M_PI / 6;
    const double distanceTolerance = 200.0;

    BOOST_CHECK_NO_THROW(
        std::vector<jsg::SegmentId> tied = tieClosures(
            closures, *graphHolder.get(), *segmentsRtree.get(),
            distanceTolerance, angleTolerance);

        std::set<jsg::SegmentId> tiedSet(tied.begin(), tied.end());
        BOOST_CHECK_EQUAL(tiedSet.count(jsg::SegmentId(0, 2)), 1);
        BOOST_CHECK_EQUAL(tiedSet.count(jsg::SegmentId(1, 0)), 1);
        BOOST_CHECK_EQUAL(tiedSet.count(jsg::SegmentId(8, 0)), 1);
    );

    // not only closures
    closures.clear();
    closures.push_back(generateJam(
        *graphHolder.get(), 8, JAM_SPEED, JamData::HARD));
    BOOST_CHECK_THROW(
        tieClosures(
            closures, *graphHolder.get(), *segmentsRtree.get(),
            distanceTolerance, angleTolerance),
        maps::RuntimeError
    );
}
