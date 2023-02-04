#include "data_set_builder.h"

#include <maps/carparks/renderer/yacare/lib/common.h>
#include <maps/carparks/renderer/yacare/lib/data_sets.h>
#include <maps/carparks/renderer/yacare/lib/pedestrian_graph.h>

#include <maps/renderer/libs/data_sets/data_set/include/data_set.h>
#include <maps/renderer/libs/data_sets/data_set/include/view_queriable.h>
#include <maps/masstransit/libs/pedestrian_graph_fb/include/pedestrian_graph.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

namespace maps::carparks::renderer::tests {

namespace {

namespace mr = maps::renderer;

std::unordered_map<uint64_t, std::unique_ptr<mr::feature::Feature>>
getFeatures(
    DataSetBuilder& builder,
    const mr::data_set::ViewQueryParams& params,
    bool withGraph = false)
{
    std::unique_ptr<masstransit::pedestrian_graph_fb::PedestrianGraph> graph;
    if (withGraph)
        graph = createPedestrianGraph(DATA_DIR + "graph.conf");

    builder.finalize();
    auto dataSet = createDataSet(builder.path(), graph.get());

    std::unordered_map<uint64_t, std::unique_ptr<mr::feature::Feature>> res;

    mr::data_set::ViewQueryContext ctx(&params);
    auto views = dataSet.begin()->second->asViewQueriable().queryView(ctx);

    for (const auto& view : views) {
        auto it = view.iterator();
        while (it->hasNext()) {
            const auto& feature = it->next();
            res[feature.sourceId()] = feature.clone();
        }
    }

    return res;
}

} // namespace


Y_UNIT_TEST_SUITE(nearby_feature_filter_tests) {

Y_UNIT_TEST(leaveOnePointByDistance)
{
    DataSetBuilder builder;
    builder.addPoint({37.587065, 55.733709}, buildInfo(1)); // 150m
    builder.addPoint({37.585224, 55.735011}, buildInfo(2)); // 300m
    builder.addPoint({37.584153, 55.735765}, buildInfo(3)); // 400m

    mr::data_set::ViewQueryParams params(
        toQueryBbox({30.0, 50.0}, {40.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{37.589400, 55.733709}, 250};

    auto features = getFeatures(builder, params);

    EXPECT_EQ(features.size(), 1);
    EXPECT_TRUE(features.count(1));
}

Y_UNIT_TEST(leaveTwoPointsByDistance)
{
    DataSetBuilder builder;
    builder.addPoint({37.587065, 55.733709}, buildInfo(1)); // 200m
    builder.addPoint({37.585224, 55.735011}, buildInfo(2)); // 300m
    builder.addPoint({37.584153, 55.735765}, buildInfo(3)); // 400m

    mr::data_set::ViewQueryParams params(
        toQueryBbox({30.0, 50.0}, {40.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{37.589400, 55.733709}, 350};

    auto features = getFeatures(builder, params);

    EXPECT_EQ(features.size(), 2);
    EXPECT_TRUE(features.count(1));
    EXPECT_TRUE(features.count(2));
}

Y_UNIT_TEST(leaveTwoPolylinesByDistance)
{
    DataSetBuilder builder;

    builder.addPolyline({{37.587065, 55.733709},
                         {37.587065, 55.733710}}, buildInfo(1)); // 150 m
    builder.addPolyline({{37.585224, 55.735011},
                         {37.585224, 55.735012}}, buildInfo(2)); // 300 m
    builder.addPolyline({{37.584153, 55.735765},
                         {37.584153, 55.735766}}, buildInfo(3)); // 400 m

    mr::data_set::ViewQueryParams params(
        toQueryBbox({30.0, 50.0}, {40.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{37.589400, 55.733709}, 350};

    auto features = getFeatures(builder, params);

    EXPECT_EQ(features.size(), 2);
    EXPECT_TRUE(features.count(1));
    EXPECT_TRUE(features.count(2));
}

Y_UNIT_TEST(leaveTwoPolygonsByDistance)
{
    DataSetBuilder builder;

    builder.addPolygon({{{37.587065, 55.733709},
                        {37.587065, 55.733710},
                        {37.587000, 55.733000}}}, buildInfo(1)); // 150m
    builder.addPolygon({{{37.585224, 55.735011},
                        {37.587070, 55.733100},
                        {37.585224, 55.735012}}}, buildInfo(2)); // 200m
    builder.addPolygon({{{37.587150, 55.733140},
                        {37.584153, 55.735765},
                        {37.584153, 55.735766}}}, buildInfo(3)); // 250m

    mr::data_set::ViewQueryParams params(
        toQueryBbox({30.0, 50.0}, {40.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{37.589400, 55.733709}, 225};

    auto features = getFeatures(builder, params);

    EXPECT_EQ(features.size(), 2);
    EXPECT_TRUE(features.count(1));
    EXPECT_TRUE(features.count(2));
}

Y_UNIT_TEST(leaveNearestPointOnGraph)
{

    DataSetBuilder builder;
    builder.addPoint({27.893594, 53.881565}, buildInfo(1)); // 240 m directly, 240 m on graph
    builder.addPoint({27.895032, 53.881476}, buildInfo(2)); // 250 m directly, 330 m on graph

    mr::data_set::ViewQueryParams params(
           toQueryBbox({20.0, 50.0}, {30.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{27.893616, 53.879422}, 300};

    auto features = getFeatures(builder, params, true);

    EXPECT_EQ(features.size(), 1);
    EXPECT_TRUE(features.count(1));
}

Y_UNIT_TEST(leaveNearestPolylineOnGraph)
{
    DataSetBuilder builder;

    builder.addPolyline({{27.891298, 53.881312},
                         {27.891770, 53.881844}}, buildInfo(1)); // <280 m directly, >360 on graph
    builder.addPolyline({{27.891899, 53.880614},
                         {27.891942, 53.879993}}, buildInfo(2)); // 190 m on graph

    mr::data_set::ViewQueryParams params(
        toQueryBbox({20.0, 50.0}, {30.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{27.893616, 53.879422}, 300};

    auto features = getFeatures(builder, params, true);

    EXPECT_EQ(features.size(), 1);
    EXPECT_TRUE(features.count(2));
}

Y_UNIT_TEST(testDegeneratedPolylineOnGraph)
{
    DataSetBuilder builder;

    builder.addPolyline({{27.891942, 53.879993},
                         {27.891942, 53.879993}}, buildInfo(1)); // 170 m on graph, 2 points at the same place
    builder.addPolyline({{27.892927, 53.883924},
                         {27.892927, 53.883924}}, buildInfo(2)); // >500 m on graph, 2 points at the same place

    mr::data_set::ViewQueryParams params(
        toQueryBbox({20.0, 50.0}, {30.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{27.893616, 53.879422}, 300};

    auto features = getFeatures(builder, params, true);

    EXPECT_EQ(features.size(), 1);
    EXPECT_TRUE(features.count(1));
}

Y_UNIT_TEST(leaveNearestPolygonOnGraph)
{
    DataSetBuilder builder;

    builder.addPolygon({{{27.891298, 53.881312},
                         {27.891770, 53.881844},
                         {27.891169, 53.882896}}}, buildInfo(1)); // <330 m directly, >360 on graph
    builder.addPolygon({{{27.891899, 53.880614},
                         {27.891942, 53.879993},
                         {27.894989, 53.880880}}}, buildInfo(2)); // <190 m on graph

    mr::data_set::ViewQueryParams params(
        toQueryBbox({20.0, 50.0}, {30.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{27.893616, 53.879422}, 350};

    auto features = getFeatures(builder, params, true);

    EXPECT_EQ(features.size(), 1);
    EXPECT_TRUE(features.count(2));
}

Y_UNIT_TEST(leavePolylineIfCenterIsReachableOnGraph)
{
    geolib3::Point2 farPointA(27.891298, 53.881312); // 270 m directly, 360 m on graph
    geolib3::Point2 farPointB(27.895656, 53.881257); // 250 m directly, 380 m on graph
    geolib3::Point2 center(27.893959, 53.881413); // < 300m on graph
    geolib3::Point2 farCenter(27.892373, 53.883032); // 500 m on graph

    DataSetBuilder builder;

    builder.addPolyline({farPointA, center, farPointB}, buildInfo(1));
    builder.addPolyline({farPointA, farPointB}, buildInfo(2));
    builder.addPolyline({farPointA, farPointA, farPointA, farPointA, farPointB}, buildInfo(3)); // center is reachable on graph
    builder.addPolyline({farPointA, farCenter, farPointB}, buildInfo(4)); // center is unreachable on graph

    mr::data_set::ViewQueryParams params(
        toQueryBbox({20.0, 50.0}, {30.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{27.893616, 53.879422}, 305};

    auto features = getFeatures(builder, params, true);

    EXPECT_EQ(features.size(), 3);
    EXPECT_TRUE(features.count(1));
    EXPECT_TRUE(features.count(2));
    EXPECT_TRUE(features.count(3));
}

Y_UNIT_TEST(leavePolygonIfCenterPointIsReachableOnGraph)
{
    geolib3::Point2 pointA(27.893420, 53.880364);
    geolib3::Point2 farPointB(27.891757, 53.882506);
    geolib3::Point2 farPointC(27.895030, 53.882411);
    geolib3::Point2 veryFarPointB(27.891295, 53.885301);
    geolib3::Point2 veryFarPointC(27.896960, 53.884857);

    DataSetBuilder builder;

    builder.addPolygon({{pointA, veryFarPointB, veryFarPointC}}, buildInfo(1)); // center is unreachable on graph
    builder.addPolygon({{pointA, farPointB, farPointC}}, buildInfo(2)); // center is reachable on graph

    mr::data_set::ViewQueryParams params(
        toQueryBbox({20.0, 50.0}, {30.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{27.893616, 53.879422}, 390};

    auto features = getFeatures(builder, params, true);

    EXPECT_EQ(features.size(), 1);
    EXPECT_TRUE(features.count(2));
}

Y_UNIT_TEST(filterPolylineByDistanceToCenter)
{
    DataSetBuilder builder;

    builder.addPolyline({{37.587565, 55.733710},
                        {37.587885, 55.733710}}, buildInfo(1)); // 35m nearest point, 45m center

    mr::data_set::ViewQueryParams params(
        toQueryBbox({30.0, 50.0}, {40.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{37.587000, 55.733710}, 40};

    auto features = getFeatures(builder, params);

    EXPECT_TRUE(features.empty());
}

Y_UNIT_TEST(filterPolygonByDistanceToCenter)
{
    DataSetBuilder builder;

    builder.addPolygon({{{37.587065, 55.733709},
                        {37.587065, 55.733710},
                        {37.0, 56.0}}}, buildInfo(1)); // 200m nearest point, >2000m center

    mr::data_set::ViewQueryParams params(
        toQueryBbox({30.0, 50.0}, {40.0, 60.0}), {0, 23});
    params.auxData = SllAndDistance{{37.589400, 55.733709}, 350};

    auto features = getFeatures(builder, params);

    EXPECT_TRUE(features.empty());
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::carparks::renderer::tests
