#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include "../../config_parsing.h"
#include "../../params.h"
#include "../../router.h"
#include "../../request_point_utils.h"

#include <maps/routing/route-descriptor/include/route_descriptor.h>
#include <maps/routing/router/yacare/response_writers/pb/collection.h>
#include <maps/routing/router/yacare/response_writers/response_params.h>
#include <maps/libs/json-config/include/config.h>
#include <maps/libs/leptidea/include/types.h>
#include <yandex/maps/i18n/i18n.h>
#include <yandex/maps/proto/common2/response.pb.h>
#include <yandex/maps/proto/driving/route.pb.h>
#include <yandex/maps/proto/driving/section.pb.h>
#include <yandex/maps/geolib3/proto.h>

#include <vector>

namespace proto = yandex::maps::proto;
namespace mgp = maps::geolib3::proto;
namespace geolib3 = maps::geolib3;

const double DISTANCE_THRESHOLD = 100;  // meters

namespace {

maps::geolib3::Point2 geoPointByPosition(
    const maps::geolib3::Polyline2& line, double position)
{
    static const double EPS = 1e-5;
    REQUIRE(position > -EPS && position < 1 + EPS,
            "Position must be between 0 and 1");
    double remainingLength = position * maps::geolib3::geoLength(line);

    for (size_t i = 0; i < line.segmentsNumber(); i++) {
        const auto& currentSegment = line.segmentAt(i);
        double currentLength = maps::geolib3::geoLength(currentSegment);
        if (remainingLength <= currentLength) {
            if (currentLength < EPS)
                return currentSegment.start();
            // For a straight line for small coordinate difference
            // geoLength is proportional to cartesian length.
            // This is why we do not need (nonimplemented) geoPointByPosition here,
            // and pointByPosition is enough
            return currentSegment.pointByPosition(
                remainingLength / currentLength);
        }
        remainingLength -= currentLength;
    }
    // due to numerical errors, at this moment
    // we can have remainingLength small but positive
    return line.points().back();
}

struct Fixture : public ::NUnitTest::TBaseFixture {
    Fixture()
    {
        maps::log8::setLevel(maps::log8::Level::DEBUG);
        reset();
    }

    void reset();

    proto::common2::response::Response getResponse(
        const std::vector<RequestPoint>& requestPoints);
    maps::route_descriptor::RouteWithGaps getDescriptor(
        const proto::common2::response::Response& response);
    std::vector<maps::geolib3::Polyline2> getLegs(
        const proto::common2::response::Response& response);

    void checkPoint(
        size_t index,
        const geolib3::Point2& point,
        maps::routing::EdgeId edgeId,
        maps::routing::EdgePosition position);

    void checkResult(
        const std::vector<RequestPoint>& requestPoints,
        leptidea7::PathTopology result);

    std::unique_ptr<maps::routing::Config> config;
    const RouterMmappedData* routerMmappedData = nullptr;
    std::unique_ptr<Router> router;
};


void Fixture::reset()
{
    auto configPath = GetOutputPath() / "router.json";
    if (!configPath.Exists()) {
        WARN() << "Config not found; will not initialize router";
        return;
    }

    maps::json::config::Config<> jsonConfig;
    jsonConfig.addFilename(static_cast<TString>(configPath));

    config = std::make_unique<maps::routing::Config>(
        maps::routing::parseConfig(jsonConfig.value()));

    std::unique_ptr<RouterMmappedData> routerMmappedData =
        std::make_unique<RouterMmappedData>(*config);
    this->routerMmappedData = routerMmappedData.get();

    router.reset(new Router(
        std::move(routerMmappedData), 1 /* threadsNumber */, *config));
    router->reloadJamsData("");
}


proto::common2::response::Response Fixture::getResponse(
    const std::vector<RequestPoint>& requestPoints)
{
    REQUIRE(router, "Router not initialized");

    std::vector<geolib3::Point2> points;
    for (const auto& requestPoint: requestPoints) {
        points.emplace_back(requestPoint.point);
    }
    auto routes = router->route(
        RouterRequest {
            .vehicleParameters = VehicleParameters(VehicleType::CAR),
            .routingMode = RoutingMode::FAST
        },
        RequestPoints(points, {}, {}, {}, {}, std::nullopt),
        1,  // results
        {},  // route
        UseJams::NO,
        maps::routing::SortByTime::Time,
        Avoid(),
        false  // debugLeptideaAlternatives
    );

    REQUIRE(routes.size() == 1, "No routes found");

    const ResponseParams PARAMS = {
        maps::i18n::bestLocale("ru_RU"), // locale
        &routerMmappedData->roadGraph,
        maps::locale::Locale(), // voiceLocale
        router->persistentIndex(),
        VehicleParameters(VehicleType::CAR),
        ResponseFeature::ALL,
        {}, /* snippet metadata ids */
        nullptr /* snippets fb */
    };

    return makeRouterResultsProtobuf(PARAMS, routes);
}

maps::route_descriptor::RouteWithGaps Fixture::getDescriptor(
    const proto::common2::response::Response& protoResponse)
{
    const std::string descriptor = protoResponse
        .reply()
        .geo_object(0)
        .metadata(0)
        .GetExtension(proto::driving::route::ROUTE_METADATA)
        .route_descriptor();
    return maps::route_descriptor::decodeRoute(
        routerMmappedData->roadGraph,
        descriptor,
        *router->persistentIndex()
    ).segmentParts;
}

std::vector<maps::geolib3::Polyline2> Fixture::getLegs(
    const proto::common2::response::Response& response)
{
    std::vector<maps::geolib3::Polyline2> result;
    for (const auto& subObject: response.reply().geo_object(0).geo_object()) {
        auto polylinePart = mgp::decode(subObject.geometry(0).polyline());
        auto index = subObject
            .metadata(0)
            .GetExtension(proto::driving::section::SECTION_METADATA)
            .leg_index();
        if (index >= result.size()) {
            result.resize(index + 1);
        }
        result[index].extend(polylinePart);
    }
    return result;
}


void Fixture::checkPoint(
    size_t index,
    const geolib3::Point2& point,
    maps::routing::EdgeId edgeId,
    maps::routing::EdgePosition position)
{
    const auto& polyline =
        routerMmappedData->roadGraph.edgeData(edgeId).polyline();
    auto edgePoint = geoPointByPosition(polyline, position.value());

    auto distance = geolib3::fastGeoDistance(point, edgePoint);

    INFO() << "Point " << index << " distance to snapped point " << distance;

    REQUIRE(distance < DISTANCE_THRESHOLD,
            "Edge point is too far from request point");
}

void Fixture::checkResult(
    const std::vector<RequestPoint>& requestPoints,
    leptidea7::PathTopology result)
{
    REQUIRE(result.subpaths.size() == requestPoints.size() - 1,
            "Wrong size of result");

    for (size_t i = 0; i < requestPoints.size(); i++) {
        if (i > 0) {
            checkPoint(i,
                       requestPoints[i].point,
                       result.subpaths[i - 1].back().edgeId,
                       result.subpaths[i - 1].back().to);
        }
        if (i + 1 < requestPoints.size()) {
            checkPoint(i,
                       requestPoints[i].point,
                       result.subpaths[i].front().edgeId,
                       result.subpaths[i].front().from);
        }
    }
}

} // namespace


Y_UNIT_TEST_SUITE_F(trim_before_first_request_point, Fixture) {

Y_UNIT_TEST(simple_test) {
    std::vector<RequestPoint> requestPoints{
        RequestPoint{{37.5, 55.8}},
        RequestPoint{{37.6, 55.7}}
    };

    auto response = getResponse(requestPoints);
    auto descriptor = getDescriptor(response);
    auto leg = getLegs(response)[0];

    auto result = trimBeforeFirstRequestPoint(
        descriptor,
        requestPoints,
        *routerMmappedData,
        *config,
        VehicleType::CAR);

    checkResult(requestPoints, result);

    auto altRequestPoints = requestPoints;
    altRequestPoints[0] = RequestPoint(leg.pointAt(leg.pointsNumber() / 3));

    auto altResult = trimBeforeFirstRequestPoint(
        descriptor,
        altRequestPoints,
        *routerMmappedData,
        *config,
        VehicleType::CAR);

    checkResult(altRequestPoints, altResult);
}

Y_UNIT_TEST(several_request_points_test) {
    std::vector<RequestPoint> initialRequestPoints{
        RequestPoint{{37.5, 55.75}},
        RequestPoint{{37.55, 55.75}},
        RequestPoint{{37.55, 55.65}},
        RequestPoint{{37.52, 55.65}}
    };

    auto response = getResponse(initialRequestPoints);
    auto descriptor = getDescriptor(response);
    auto legs = getLegs(response);

    // first check from the start of the route
    auto initialCorrectedRequestPoints = initialRequestPoints;
    initialCorrectedRequestPoints[0] = RequestPoint(legs[0].points().front());

    auto initialResult = trimBeforeFirstRequestPoint(
        descriptor,
        initialCorrectedRequestPoints,
        *routerMmappedData,
        *config,
        VehicleType::CAR);
    checkResult(initialCorrectedRequestPoints, initialResult);

    // move along the first leg of the route
    auto requestPointsFromLeg0 = initialRequestPoints;
    requestPointsFromLeg0[0] = RequestPoint(
        legs[0].pointAt(legs[0].pointsNumber() / 3));

    auto resultFromLeg0 = trimBeforeFirstRequestPoint(
        descriptor,
        requestPointsFromLeg0,
        *routerMmappedData,
        *config,
        VehicleType::CAR);
    checkResult(requestPointsFromLeg0, resultFromLeg0);

    // move onto the next leg
    auto requestPointsFromLeg1 = initialRequestPoints;
    requestPointsFromLeg1.erase(requestPointsFromLeg1.begin());

    requestPointsFromLeg1[0] = RequestPoint(
        legs[1].pointAt(legs[1].pointsNumber() / 3));

    auto resultFromLeg1 = trimBeforeFirstRequestPoint(
        descriptor,
        requestPointsFromLeg1,
        *routerMmappedData,
        *config,
        VehicleType::CAR);
    checkResult(requestPointsFromLeg1, resultFromLeg1);
}

}
