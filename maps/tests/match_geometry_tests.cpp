#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/jams_arm2/libs_common/graph.h>

namespace maps::wiki::jams_arm2 {

class MatchGeometryTest {
public:
    void addEdgeToTestData(uint32_t shortId,
                           uint32_t sourceVertexId,
                           geolib3::Point2 sourcePoint,
                           uint32_t targetVertexId,
                           geolib3::Point2 targetPoint,
                           bool isFerry)
    {
        edgesInternal_.emplace_back(
            Graph::EdgeInternal{road_graph::EdgeId(shortId),
                {road_graph::VertexId(sourceVertexId), sourcePoint},
                {road_graph::VertexId(targetVertexId), targetPoint},
                isFerry});
    }

    void addEdgeNoSegmentToTestData()
    {
        edgesInternal_.emplace_back(Graph::EdgeInternal());
        edgesInternal_.back().noSegment = true;
    }

    void checkMatchSuccess(const geolib3::Polyline2& geoClosureGeom)
    {
        checkMatch(geoClosureGeom, true);
    }
    void checkMatchFail(const geolib3::Polyline2& geoClosureGeom)
    {
        checkMatch(geoClosureGeom, false);
    }

    void checkCurrentBranchIsBetterCommonSource(const geolib3::Polyline2& geoClosureGeom)
    {
        checkCurrentBranchIsBetter(geoClosureGeom, Graph::EdgeEndType::Source);
    }
    void checkCurrentBranchIsBetterCommonTarget(const geolib3::Polyline2& geoClosureGeom)
    {
        checkCurrentBranchIsBetter(geoClosureGeom, Graph::EdgeEndType::Target);
    }

private:
    void checkMatch(const geolib3::Polyline2& geoClosureGeom, bool isMatched)
    {
        UNIT_ASSERT(Graph::isStrictlyMatched(geoClosureGeom, edgesInternal_) == isMatched);
    }
    void checkCurrentBranchIsBetter(const geolib3::Polyline2& geoClosureGeom,
                                    Graph::EdgeEndType commonEnd)
    {
        ASSERT(edgesInternal_.size() == 2);
        UNIT_ASSERT(Graph::isCurrentBranchBetter(geoClosureGeom,
                                                 edgesInternal_[0],
                                                 edgesInternal_[1],
                                                 commonEnd));
    }

    Graph::EdgesInternal edgesInternal_;
};

Y_UNIT_TEST_SUITE(tie_closure)
{
    Y_UNIT_TEST(empty_snap)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {29.278984,40.660674},
                {29.279159,40.661369},
                {29.279261,40.661807}});
        MatchGeometryTest().checkMatchFail(geoClosureGeom);
    }
    Y_UNIT_TEST(dangling_head)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {29.278984,40.660674},
                {29.279159,40.661369},
                {29.279261,40.661807}});
        MatchGeometryTest test;
        test.addEdgeNoSegmentToTestData();
        test.addEdgeToTestData(111,
                               1, {29.2791589,40.6613694}, // source vertex
                               2, {29.2792608,40.6618065}, // target vertex
                               false);
        test.checkMatchFail(geoClosureGeom);
    }
    Y_UNIT_TEST(dangling_head_ferry)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {29.278984,40.660674},
                {29.279159,40.661369},
                {29.279261,40.661807}});
        MatchGeometryTest test;
        test.addEdgeToTestData(111,
                               1, {29.2791589,40.6613694}, // source vertex
                               2, {29.2792608,40.6618065}, // target vertex
                               true);
        test.checkMatchSuccess(geoClosureGeom);
    }
    Y_UNIT_TEST(successful_match)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {44.022721,56.330804},
                {44.022812,56.330937},
                {44.022866,56.331019},
                {44.022907,56.331086},
                {44.023086,56.331473},
                {44.023222,56.331899}});
        MatchGeometryTest test;
        test.addEdgeToTestData(111,
                               1, {44.0226369,56.3306818}, // source vertex
                               2, {44.0228122,56.330937}, // target vertex
                               false);
        test.addEdgeToTestData(112,
                               2, {44.0228122,56.330937},
                               3, {44.0228663,56.3310186},
                               false);
        test.addEdgeToTestData(113,
                               3, {44.0228663,56.3310186},
                               4, {44.0229387,56.3311483},
                               false);
        test.addEdgeToTestData(114,
                               4, {44.0229387,56.3311483},
                               5, {44.0232218,56.3318995},
                               false);
        test.checkMatchSuccess(geoClosureGeom);
    }
    Y_UNIT_TEST(dangling_tail)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {44.022721,56.330804},
                {44.022812,56.330937},
                {44.022866,56.331019},
                {44.022907,56.331086},
                {44.023086,56.331473},
                {44.023222,56.331899}});
        MatchGeometryTest test;
        test.addEdgeToTestData(111,
                               1, {44.0226369,56.3306818}, // source vertex
                               2, {44.0228122,56.330937}, // target vertex
                               false);
        test.addEdgeToTestData(112,
                               2, {44.0228122,56.330937},
                               3, {44.0228663,56.3310186},
                               false);
        test.addEdgeToTestData(113,
                               3, {44.0228663,56.3310186},
                               4, {44.0229387,56.3311483},
                               false);
        test.addEdgeNoSegmentToTestData();
        test.checkMatchFail(geoClosureGeom);
    }
    Y_UNIT_TEST(dangling_tail_ferry)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {44.022721,56.330804},
                {44.022812,56.330937},
                {44.022866,56.331019},
                {44.022907,56.331086},
                {44.023086,56.331473},
                {44.023222,56.331899}});
        MatchGeometryTest test;
        test.addEdgeToTestData(111,
                               1, {44.0226369,56.3306818}, // source vertex
                               2, {44.0228122,56.330937}, // target vertex
                               false);
        test.addEdgeToTestData(112,
                               2, {44.0228122,56.330937},
                               3, {44.0228663,56.3310186},
                               false);
        test.addEdgeToTestData(113,
                               3, {44.0228663,56.3310186},
                               4, {44.0229387,56.3311483},
                               true);
        test.checkMatchSuccess(geoClosureGeom);
    }
    Y_UNIT_TEST(skipped_edge)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {44.022721,56.330804},
                {44.022812,56.330937},
                {44.022866,56.331019},
                {44.022907,56.331086},
                {44.023086,56.331473},
                {44.023222,56.331899}});
        MatchGeometryTest test;
        test.addEdgeToTestData(111,
                               1, {44.0226369,56.3306818}, // source vertex
                               2, {44.0228122,56.330937}, // target vertex
                               false);
        test.addEdgeToTestData(112,
                               2, {44.0228122,56.330937},
                               3, {44.0228663,56.3310186},
                               false);
        test.addEdgeToTestData(113,
                               4, {44.0229387,56.3311483},
                               5, {44.0232218,56.3318995},
                               false);
        test.checkMatchSuccess(geoClosureGeom);
    }

    Y_UNIT_TEST(better_branch_common_source)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {29.2212071,41.0205228},
                {29.2210477,41.0203124},
                {29.2204895,41.0196343}});
        MatchGeometryTest test;
        test.addEdgeToTestData(111, // current edge
                               1, {29.2212071,41.0205228}, // source vertex
                               2, {29.2210477,41.0203124}, // target vertex
                               false);
        test.addEdgeToTestData(112, // prev edge
                               1, {29.2212071,41.0205228},
                               3, {29.2208477,41.0203124},
                               false);

        test.checkCurrentBranchIsBetterCommonSource(geoClosureGeom);
    }

    Y_UNIT_TEST(better_branch_common_target)
    {
        const geolib3::Polyline2 geoClosureGeom(
            geolib3::PointsVector{
                {29.2212071,41.0205228},
                {29.2210477,41.0203124},
                {29.2204895,41.0196343}});
        MatchGeometryTest test;
        test.addEdgeToTestData(111,   // current edge
                               1, {29.2210477,41.0203124}, // source vertex
                               2, {29.2204895,41.0196343}, // target vertex
                               false);
        test.addEdgeToTestData(112,   // prev edge
                               3, {29.2208477,41.0203124},
                               2, {29.2204895,41.0196343},
                               false);

        test.checkCurrentBranchIsBetterCommonTarget(geoClosureGeom);
    }

} // test suite end

} // namespace maps::wiki::jams_arm2
