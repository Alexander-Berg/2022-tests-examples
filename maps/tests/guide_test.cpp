#ifndef MOBILE_BUILD

#include <maps/analyzer/libs/guidance/include/config.h>
#include <maps/analyzer/libs/guidance/include/types.h>
#include <maps/analyzer/libs/guidance/include/guide.h>
#include <maps/analyzer/libs/guidance/include/graph.h>
#include <maps/analyzer/libs/guidance/include/graph/compact_graph.h>
#include <maps/analyzer/libs/guidance/include/route.h>
#include <maps/analyzer/libs/guidance/include/route/segments_route.h>
#include <maps/analyzer/libs/external/geo/include/geo.h>
#include <maps/libs/road_graph/include/graph.h>
#include <maps/libs/succinct_rtree/include/rtree.h>
#include <yandex/maps/mms/holder2.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/yson/node/node_io.h>
#include <util/stream/file.h>

#include <vector>
#include <optional>


namespace external = maps::analyzer::external;
namespace guidance = maps::analyzer::guidance;

const std::string TEST_DATA_ROOT =
    ArcadiaSourceRoot() + "/maps/analyzer/libs/guidance/tests/data";

const std::string ROAD_GRAPH_PATH = BinaryPath("maps/data/test/graph3/road_graph.fb");
const std::string RTREE_PATH = BinaryPath("maps/data/test/graph3/rtree.fb");

const maps::road_graph::Graph graph{ROAD_GRAPH_PATH};
const maps::succinct_rtree::Rtree rtree{RTREE_PATH, graph};

const guidance::Config cfg = guidance::defaultConfig;

template <typename T>
std::optional<T> optionalField(const NYT::TNode& node, const char* name) {
    if (node.HasKey(name)) {
        return node[name].ConvertTo<T>();
    }
    return std::nullopt;
}

using RouteGeometry = std::vector<external::geo::Position>;

std::optional<RouteGeometry> maybeSetRoute(const NYT::TNode& node) {
    RouteGeometry geom;
    if (node.HasKey("route_geometry")) {
        for (const auto& pt: node["route_geometry"].AsList()) {
            const auto& coords = pt.AsList();
            geom.push_back({
                coords[0].ConvertTo<double>(),
                coords[1].ConvertTo<double>()
            });
        }
        return geom;
    }
    return std::nullopt;
}

auto makeSignal(const NYT::TNode& event) {
    return guidance::signal(
        external::geo::position(event["lon"].ConvertTo<double>(), event["lat"].ConvertTo<double>()),
        optionalField<double>(event, "accuracy"),
        optionalField<double>(event, "direction"),
        optionalField<double>(event, "speed").value_or(0.0), // FIXME: no speed, ignore?
        guidance::Timestamp(guidance::Duration(
            event["timestamp"].ConvertTo<ui64>() * 1000
        ))
    );
}

Y_UNIT_TEST_SUITE(GuideTests) {
    Y_UNIT_TEST(GuideTest) {
        guidance::Guide<guidance::RoadGraph, guidance::SegmentsRoute> guide{
            &cfg,
            guidance::fromGraph(graph, rtree)
        };

        TFileInput f{TString(TEST_DATA_ROOT + "/events.yson")};
        const auto events = NYT::NodeFromYsonStream(&f, ::NYson::EYsonType::ListFragment);

        for (const auto& event: events.AsList()) {
            if (const auto routeGeom = maybeSetRoute(event)) {
                guide.setRoute(guidance::fromGeometry(*routeGeom));
                continue;
            }

            guide(makeSignal(event));
        }
    }

    Y_UNIT_TEST(GuideTestNoInstantFinish) {
        guidance::Guide<guidance::RoadGraph, guidance::SegmentsRoute> guide{
            &cfg,
            guidance::fromGraph(graph, rtree)
        };

        TFileInput f{TString(TEST_DATA_ROOT + "/events_instant_finish.yson")};
        const auto events = NYT::NodeFromYsonStream(&f, ::NYson::EYsonType::ListFragment);

        bool isLastEventSetRoute = false;
        for (const auto& event: events.AsList()) {
            if (const auto routeGeom = maybeSetRoute(event)) {
                guide.setRoute(guidance::fromGeometry(*routeGeom));
                isLastEventSetRoute = true;
                continue;
            }

            const auto trackingState = guide(makeSignal(event));

            EXPECT_TRUE(
                !isLastEventSetRoute ||
                !trackingState.event ||
                *trackingState.event != guidance::RouteEvent::Finished
            );

            isLastEventSetRoute = false;
        }
    }

    Y_UNIT_TEST(GuideTestNoPredictedRouteStuck) {
        auto predictedRoutesCfg = cfg;
        predictedRoutesCfg.USE_PREDICTED_ROUTES = true;

        guidance::Guide<guidance::RoadGraph, guidance::SegmentsRoute> guide{
            &predictedRoutesCfg,
            guidance::fromGraph(graph, rtree)
        };

        TFileInput f{TString(TEST_DATA_ROOT + "/events_predicted_route.yson")};
        const auto events = NYT::NodeFromYsonStream(&f, ::NYson::EYsonType::ListFragment);

        bool isFirstSignal = true;
        for (const auto& event: events.AsList()) {
            if (const auto routeGeom = maybeSetRoute(event)) {
                guide.setRoute(guidance::fromGeometry(*routeGeom, /* predicted */ true));
                continue;
            }

            const auto trackingState = guide(makeSignal(event), false, false);

            // check movement smoothness
            // given test we expect each transition distance to be close to signals distance
            // we don't calculate signals distance here since we know the distance is a bit greater than 30m
            EXPECT_TRUE(trackingState.sequence.has_value());
            EXPECT_TRUE(isFirstSignal || trackingState.sequence->previousLocation);

            if (!isFirstSignal) {
                EXPECT_GT(trackingState.sequence->transitionDistance, 30);
            }
            isFirstSignal = false;
        }
    }
}

#endif
