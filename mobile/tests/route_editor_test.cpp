#include "../route_editor_impl.h"
#include "../../guidance/legacy_route_builder_creator.h"

#include <yandex/maps/navikit/check_context.h>
#include <yandex/maps/navikit/format.h>
#include <yandex/maps/navikit/mocks/mock_app_lifecycle_manager.h>
#include <yandex/maps/navikit/mocks/mock_bug_report_manager.h>
#include <yandex/maps/navikit/mocks/mock_driving_router.h>
#include <yandex/maps/navikit/mocks/mock_guide.h>
#include <yandex/maps/navikit/mocks/mock_guidance.h>
#include <yandex/maps/navikit/mocks/mock_ride_history_manager.h>
#include <yandex/maps/navikit/mocks/mock_route.h>
#include <yandex/maps/navikit/mocks/mock_route_manager.h>

#include <yandex/maps/mapkit/geometry/math.h>
#include <yandex/maps/navikit/create_geo_object.h>
#include <yandex/maps/navikit/empty_guidance_listener.h>
#include <yandex/maps/navikit/enable_subscription_from_this.h>
#include <yandex/maps/navikit/location/location_provider_creator.h>
#include <yandex/maps/navikit/route_editor/empty_route_editor_listener.h>
#include <yandex/maps/navikit/routing/parking/parking_route_builder.h>
#include <yandex/maps/navikit/routing/router_options_manager_creator.h>
#include <yandex/maps/navikit/routing/routing_available_handler.h>
#include <yandex/maps/navikit/routing/variants_manager.h>
#include <yandex/maps/navikit/vehicle/vehicle_type_manager.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/async/utils/retryable_session.h>
#include <yandex/maps/runtime/network/exceptions.h>

#include <boost/test/unit_test.hpp>

using namespace std::chrono_literals;

namespace yandex::maps::navikit::route_editor {

using namespace runtime;
using namespace mapkit;
using namespace mapkit;

namespace {

using namespace testing;
using StringVector = std::vector<std::string>;
using RequestPointVector = bindings::SharedVector<RequestPoint>;

const char* toString(UserAction action)
{
    switch (action) {
        case UserAction::SetPoint: return "SetPoint";
        case UserAction::MovePoint: return "MovePoint";
        case UserAction::Voice: return "Voice";
        case UserAction::Parking: return "Parking";
        case UserAction::OnlineRoute: return "OnlineRoute";
        case UserAction::VoiceSearch: return "VoiceSearch";
        case UserAction::ImmediateRoute: return "ImmediateRoute";
        case UserAction::Entrance: return "Entrance";
    }
    ASSERT(false);
}

class MyGuide;
class MyGuidance;

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// RouteEditorTestFixture
//

class RouteEditorTestFixture :
    public EnableSubscriptionFromThis<EmptyRouteEditorListener>,
    public routing::RoutingAvailableHandler {
    // Constructor
protected:
    RouteEditorTestFixture();

    // Methods
public:
    RouteEditorImpl* routeEditor() { return routeEditor_.get(); }

    void setLocation(const std::string& position, double heading);

    void setNetwork(bool enabled) { networkEnabled_ = enabled; }

    void setFromPoint(const std::string& pos)
    {
        async::runOnUiThread([=] { routeEditor_->setFromPoint(geoObjectFor(pos)); });
    }

    void setToPoint(const std::string& pos)
    {
        async::runOnUiThread([=] { routeEditor_->setToPoint(geoObjectFor(pos)); });
    }

    void setPoints(
        const std::string& from,
        const std::string& to,
        const StringVector& via,
        UserAction action = UserAction::SetPoint);

    void addViaPoint(RequestPointType requestPointType, const std::string& pos)
    {
        async::runOnUiThread(
            [=] { routeEditor_->addViaPoint(requestPointType, geoObjectFor(pos)); });
    }

    void insertViaPoint(
        int index,
        RequestPointType requestPointType,
        const std::string& pos,
        UserAction action)
    {
        async::runOnUiThread([=] {
            routeEditor_->insertViaPoint(index, requestPointType, geoObjectFor(pos), action);
        });
    }

    void reset(ResetMode resetMode)
    {
        async::runOnUiThread([=] { routeEditor_->reset(resetMode); });
    }

    void restorePoints(const std::shared_ptr<driving::Route>& route, ResetMode resetMode)
    {
        async::runOnUiThread([=] { routeEditor_->restorePoints(route, resetMode); });
    }

    void createParkingRoute()
    {
        async::runOnUiThread([=] { routeEditor_->createParkingRoute(); });
    }

    void moveFromPin(const std::string& pos)
    {
        async::runOnUiThread([=] {
            routeEditor_->setFromPoint(
                createUnnamedGeoObject(points_.at(pos)),
                UserAction::MovePoint);
        });
    }

    void moveToPin(const std::string& pos)
    {
        async::runOnUiThread([=] {
            routeEditor_->setToPoint(
                createUnnamedGeoObject(points_.at(pos)),
                UserAction::MovePoint);
        });
    }

    bool hasViaPoints() const
    {
        return bool(getViaPointTypeAt(0));
    }

    boost::optional<RequestPointType> getViaPointTypeAt(size_t index) const {
        return async::runOnUiThread([=]() -> boost::optional<RequestPointType> {
            const auto viaRequestPointTypes = routeEditor_->viaRequestPointTypes();
            if (index >= viaRequestPointTypes.size())
                return boost::none;
            return viaRequestPointTypes.at(index);
        });
    }

    bool hasFromPoint() const
    {
        return async::runOnUiThread([=] { return routeEditor_->hasFromPoint(); });
    }

    bool hasToPoint() const
    {
        return async::runOnUiThread([=] { return routeEditor_->hasToPoint(); });
    }

    void cancelRouting()
    {
        async::runOnUiThread([=] { routeEditor_->cancelRouting(); });
    }

    void waitForRoute() const
    {
        assertNotUi();

        while (async::runOnUiThread([=] { return routeEditor_->isRouting(); })) {
            async::sleepFor(10ms);
        }
    }

    void startDriving();
    void driveTo(const geometry::PolylinePosition& routePos);
    void finishDriving();

    void log(const std::string& line) { logLines_.push_back(line); }
    const StringVector& getLog() const { return logLines_; }

    std::string nameOf(const geometry::Point& point) const;
    geometry::Point pointFor(const std::string& name) const;
    std::shared_ptr<GeoObject> geoObjectFor(const std::string& name) const;

    void checkRoute(const std::string& points, UserAction action) const;
    void checkRoute(
        const std::string& routeType, const std::string& points, UserAction action) const;
    void checkNoRoute() const;

    void checkNoPanels() const;
    void checkLocatingPanel(bool value) const;
    void checkRoutingPanel(bool value) const;

    void sendNetworkRequest();

    std::shared_ptr<driving::Route> createRoute(
        const std::string& from,
        const std::string& to,
        RequestPointType pointType,
        const std::string& via);

    virtual bool handleBeforeSetFromPoint() const override
    {
        navikit::assertUi();

        return !isRoutingAvailable_;
    }

    virtual bool handleBeforeBuildRoute(bool /* hasToPoint */) const override
    {
        navikit::assertUi();

        return !isRoutingAvailable_;
    }

    void setRoutingAvailable(bool isAvailable)
    {
        isRoutingAvailable_ = isAvailable;
    }

    void doTestPassedViaPoint(RequestPointType viaPointType);

    // RouteEditorListener
private:
    virtual void onLocating() override
    {
        BOOST_REQUIRE(!hasLocatingPanel_);
        hasLocatingPanel_ = true;
    }
    virtual void onConnecting() override
    {
        BOOST_REQUIRE(!hasLocatingPanel_);
        hasRoutingPanel_ = true;
    }
    virtual void onRouting() override
    {
        hasLocatingPanel_ = false;
        hasRoutingPanel_ = true;
    }
    virtual void onLocatingCancelled() override
    {
        BOOST_REQUIRE(hasLocatingPanel_);
        hasLocatingPanel_ = false;
    }
    virtual void onConnectingCancelled() override
    {
        BOOST_REQUIRE(hasRoutingPanel_);
        hasRoutingPanel_ = false;
    }
    virtual void onRoutingCancelled() override
    {
        BOOST_REQUIRE(hasRoutingPanel_);
        hasRoutingPanel_ = false;
    }
    virtual void onRouteError(Error* /* error */) override
    {
        hasRoutingPanel_ = false;
    }

    virtual void onRouteCreated(
        const std::shared_ptr<bindings::SharedVector<driving::Route>>& routes,
        UserAction action) override
    {
        hasRoutingPanel_ = false;
        route_ = routes->sharedAt(0);
        lastAction_ = action;
        routeEditor_->selectRoute(route_);
    }

    // Helper methods
private:
    void setUp();

    // Fields
private:
    std::map<std::string, std::shared_ptr<GeoObject>> geoObjects_;
    std::map<std::string, geometry::Point> points_;
    StringVector logLines_;
    bool networkEnabled_ = true;
    std::shared_ptr<MyGuide> guide_;
    std::shared_ptr<navikit::location::LocationProvider> defaultLocationProvider_;
    std::unique_ptr<navikit::routing::parking::ParkingRouteBuilder> parkingRouteBuilder_;
    std::unique_ptr<driving::DrivingRouter> router_;
    std::shared_ptr<navikit::vehicle::VehicleTypeManager> vehicleTypeManager_;
    std::shared_ptr<navikit::routing::RouterOptionsManager> routerOptionsManager_;
    std::shared_ptr<MyGuidance> guidance_;
    std::shared_ptr<RouteEditorImpl> routeEditor_;
    std::shared_ptr<driving::Route> route_;
    boost::optional<UserAction> lastAction_;
    NiceMock<ride_history::MockRideHistoryManager> historyManager_;
    NiceMock<routing::MockRouteManager> routeManager_;
    NiceMock<bug_report::MockBugReportManager> bugReportManager_;
    bool hasRoutingPanel_ = false;
    bool hasLocatingPanel_ = false;
    bool isRoutingAvailable_ = true;
};

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// MyGuide
//

class MyGuide : public directions::guidance::MockGuide {
public:
    // Methods
    //
    void setLocation(const geometry::Point& pos, double heading)
    {
        location_ = mapkit::location::Location();
        location_->position = pos;
        location_->heading = heading;
        listeners_.notify(&directions::guidance::GuidanceListener::onLocationUpdated);
    }

    void startDriving() { driveTo({0, 0.0}); }

    void driveTo(const geometry::PolylinePosition& routePos)
    {
        assertUi();
        ASSERT(route_, "MyGuide.startDriving: route hasn't been set");
        routePos_ = routePos;
        listeners_.notify(&directions::guidance::GuidanceListener::onRoutePositionUpdated);
    }

    void finishDriving() {
        assertUi();
        ASSERT(route_, "MyGuide.startDriving: route hasn't been set");
        driveTo({ static_cast<unsigned>(route_->geometry()->points->size() - 2), 1.0 });
        listeners_.notify(&directions::guidance::GuidanceListener::onFinishedRoute);
    }

    // Guide
    //
    void subscribe(const std::shared_ptr<directions::guidance::GuidanceListener>& listener) override
    {
        listeners_.subscribe(listener);
    }

    void unsubscribe(const std::shared_ptr<directions::guidance::GuidanceListener>& listener) override
    {
        listeners_.unsubscribe(listener);
    }

    boost::optional<directions::guidance::ClassifiedLocation> location() const override
    {
        if (!location_)
            return boost::none;

        directions::guidance::ClassifiedLocation classified;
        classified.location = *location_;
        return classified;
    }

    void setRoute(const std::shared_ptr<driving::Route>& route) override { route_ = route; }

    const std::shared_ptr<driving::Route> route() const override { return route_; }
    boost::optional<geometry::PolylinePosition> routePosition() const override { return routePos_; }

private:
    // Fields
    //
    subscription::Subscription<directions::guidance::GuidanceListener> listeners_;
    std::shared_ptr<driving::Route> route_;
    boost::optional<geometry::PolylinePosition> routePos_;
    boost::optional<mapkit::location::Location> location_;
};

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// MyRoute
//

class MyRoute : public driving::MockRoute {
public:
    explicit MyRoute(const std::shared_ptr<RequestPointVector>& points, bool forParking = false)
        : requestPoints_(forParking ? nullptr : points)
        , metadata_(std::make_shared<driving::RouteMetadata>())
        , sections_(std::make_shared<bindings::SharedVector<driving::Section>>())
        , geometry_(std::make_shared<geometry::Polyline>())
    {
        ASSERT(points->size() >= 2);
        ASSERT(points->front().type == RequestPointType::Waypoint);
        ASSERT(points->back().type == RequestPointType::Waypoint);

        metadata_->routePoints = std::make_shared<bindings::SharedVector<driving::RoutePoint>>();
        metadata_->flags.forParking = forParking;

        unsigned legIndex = 0;
        for (size_t i = 0; i < points->size(); ++i) {
            const RequestPoint& point = points->at(i);
            metadata_->routePoints->emplace_back(point.point, boost::none, boost::none);
            geometry_->points->push_back(point.point);

            if (i == 0)
                continue;

            const unsigned segment = (i - 1);
            driving::Section section;
            section.geometry.begin = {segment, 0.0};
            section.geometry.end = {segment, 1.0};

            auto metadata = section.metadata = std::make_shared<driving::SectionMetadata>();
            metadata->legIndex = legIndex;

            switch (point.type) {
                case RequestPointType::Waypoint:
                    ++legIndex;
                    break;
                case RequestPointType::Viapoint:
                    metadata->viaPointPositions = std::make_shared<bindings::Vector<unsigned>>();
                    metadata->viaPointPositions->push_back(1);
                    break;
                default:
                    ASSERT(false);
            }

            sections_->push_back(section);
        }

        maxLegIndex_ = legIndex;
    }

    void resetRequestPoints() {
        requestPoints_.reset();
    }

    const std::shared_ptr<driving::RouteMetadata>& metadata() const override { return metadata_; }

    const std::shared_ptr<bindings::SharedVector<driving::Section>>& sections() const override
    {
        return sections_;
    }

    const std::shared_ptr<geometry::Polyline>& geometry() const override { return geometry_; }

    const geometry::PolylinePosition& position() const override { return position_; }

    void setPosition(const geometry::PolylinePosition& position) override { position_ = position; }

    unsigned legIndex() const override { return legIndex_; }

    void setLegIndex(unsigned int legIndex) override {
        ASSERT(legIndex <= maxLegIndex_);
        legIndex_ = legIndex;
    }

    virtual const std::shared_ptr<RequestPointVector>& requestPoints() const override {
        return requestPoints_;
    }

private:
    std::shared_ptr<RequestPointVector> requestPoints_;
    const std::shared_ptr<driving::RouteMetadata> metadata_;
    const std::shared_ptr<bindings::SharedVector<driving::Section>> sections_;
    const std::shared_ptr<geometry::Polyline> geometry_;
    geometry::PolylinePosition position_;
    unsigned maxLegIndex_ = 0;
    unsigned legIndex_ = 0;
};

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// MyDrivingRouter
//

class MyDrivingRouter : public driving::MockDrivingRouter {
public:
    explicit MyDrivingRouter(RouteEditorTestFixture* env) : env_(env) {}

    std::unique_ptr<driving::Session> requestRoutes(
        const std::shared_ptr<bindings::SharedVector<RequestPoint>>& points,
        const driving::DrivingOptions& /* drivingOptions */,
        const driving::VehicleOptions& /* vehicleOptions */,
        const driving::Session::OnDrivingRoutes& onSuccess,
        const driving::Session::OnDrivingRoutesError& onError) override
    {
        return doRequestRoutes("", points, onSuccess, onError);
    }

    std::unique_ptr<driving::Session> requestParkingRoutes(
        const geometry::Point& location,
        const boost::optional<geometry::Point>& finish,
        const driving::DrivingOptions& /* drivingOptions */,
        const driving::VehicleOptions& /* vehicleOptions */,
        const driving::Session::OnDrivingRoutes& onSuccess,
        const driving::Session::OnDrivingRoutesError& onError) override
    {
        auto points = std::make_shared<bindings::SharedVector<RequestPoint>>();
        points->emplace_back(location, RequestPointType::Waypoint, boost::none);
        points->emplace_back(location, RequestPointType::Viapoint, boost::none);
        points->emplace_back(
            finish.value_or(location), RequestPointType::Waypoint, boost::none);

        return doRequestRoutes("Parking", points, onSuccess, onError);
    }

private:
    std::unique_ptr<driving::Session> doRequestRoutes(
        const std::string& routeType,
        const std::shared_ptr<bindings::SharedVector<RequestPoint>>& points,
        const driving::Session::OnDrivingRoutes& onSuccess,
        const driving::Session::OnDrivingRoutesError& onError)
    {
        return async::utils::makeRetryableSession<driving::Session>(
            [=] {
                env_->sendNetworkRequest();

                return async::runOnUiThread([=] {
                    env_->log(
                        "MyDrivingRouter.request" + routeType +
                        "Routes(points=" + toString(points) + ")");
                    auto route = std::make_shared<MyRoute>(points, routeType == "Parking");
                    auto routes = std::make_shared<bindings::SharedVector<driving::Route>>();
                    routes->push_back_shared(route);
                    return routes;
                });
            },
            std::move(onSuccess),
            std::move(onError));
    }

    std::string toString(
        const std::shared_ptr<bindings::SharedVector<RequestPoint>>& points) const
    {
        std::stringstream str;
        const char* sep = "";
        for (auto it : *points) {
            const char* const pointType = (it.type == RequestPointType::Waypoint ? "" : "Via:");
            str << sep << pointType << env_->nameOf(it.point);
            sep = ", ";
        }
        return "[" + str.str() + "]";
    }

    RouteEditorTestFixture* env_;
};

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// MyGuidance
//

class MyGuidance : public navikit::guidance::MockGuidance,
    public navikit::EmptyGuidanceListener,
    public std::enable_shared_from_this<MyGuidance> {
public:

    MyGuidance(MyGuide* guide,
        driving::DrivingRouter* router,
        navikit::routing::RouterOptionsManager* routerOptionsManager)
        : guide_(guide)
        , variantsManager_(navikit::routing::createVariantsManager(
            &appLifecycleManager_,
            guide_,
            router,
            routerOptionsManager))
    {
        async::runOnUiThread([=] {
            routeBuilder_ = navikit::guidance::createRouteBuilder(
                router,
                routerOptionsManager,
                variantsManager_.get(),
                [](const guidance::RoutePtr&){ });
        });
    }

    std::shared_ptr<MyGuidance> init()
    {
        guide_->subscribe(shared_from_this());
        return shared_from_this();
    }

    // navikit::guidance::Guidance
    navikit::guidance::RouteBuilder* routeBuilder() const override
    {
        return routeBuilder_.get();
    }

    void addGuidanceListener(
        const std::shared_ptr<navikit::guidance::GuidanceListener>& guidanceListener) override
    {
        listeners_.subscribe(guidanceListener);
    }

    void removeGuidanceListener(
        const std::shared_ptr<navikit::guidance::GuidanceListener>& guidanceListener) override
    {
        listeners_.unsubscribe(guidanceListener);
    }

    std::shared_ptr<directions::driving::Route> route() const override
    {
        return guide_->route();
    }

    boost::optional<directions::guidance::ClassifiedLocation> location() const override
    {
        return guide_->location();
    }

    boost::optional<geometry::PolylinePosition> routePosition() const override
    {
        return guide_->routePosition();
    }

    // directions::guidance::GuidanceListener
    void onFinishedRoute() override
    {
        listeners_.notify(&navikit::guidance::GuidanceListener::onFinishedRoute);
    }

    void onLocationUpdated() override
    {
        listeners_.notify(&navikit::guidance::GuidanceListener::onLocationUpdated);
    }

    void onRoutePositionUpdated() override
    {
        listeners_.notify(&navikit::guidance::GuidanceListener::onRoutePositionUpdated);
    }

private:
    MyGuide* const guide_;
    NiceMock<navikit::MockAppLifecycleManager> appLifecycleManager_;
    std::shared_ptr<navikit::routing::VariantsManager> variantsManager_;
    std::shared_ptr<navikit::guidance::RouteBuilder> routeBuilder_;
    subscription::Subscription<navikit::guidance::GuidanceListener> listeners_;
};

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// RouteEditorTestFixture implementation
//

RouteEditorTestFixture::RouteEditorTestFixture()
    : guide_(new MyGuide())
    , router_(new MyDrivingRouter(this))
    , vehicleTypeManager_(navikit::vehicle::createVehicleTypeManager(/* projectedSystemManager */ nullptr))
    , routerOptionsManager_(async::runOnUiThread([=](MyGuide* guide) {
            return navikit::routing::createRouterOptionsManager(
                guide,
                vehicleTypeManager_.get());
        }, guide_.get()))
    , guidance_(std::make_shared<MyGuidance>(
        guide_.get(),
        router_.get(),
        routerOptionsManager_.get())->init())
{
    static const struct {
        const char* key;
        double latitude;
        double longitude;
    } data[] = {
        {"MOSCOW_CENTER", 55.755786, 37.617633},
        {"ZELENOGRAD", 55.974650, 37.169906},
        {"YANDEX", 55.733670, 37.587874},
        {"PETROVSKY_PALACE", 55.793024, 37.552184},
    };

    for (const auto& pt : data) {
        points_[pt.key] = geometry::Point(pt.latitude, pt.longitude);
        geoObjects_[pt.key] = createGeoObject(boost::none, boost::none, points_.at(pt.key));
    }
    geoObjects_["-"] = nullptr;

    async::ui()->spawn([&] { setUp(); }).wait();
}

void RouteEditorTestFixture::setUp()
{
    ASSERT(!defaultLocationProvider_);
    defaultLocationProvider_ = navikit::location::createDefaultLocationProvider(guide_.get());

    parkingRouteBuilder_ = navikit::routing::parking::createParkingRouteBuilder(
        router_.get(),
        routerOptionsManager_.get(),
        [](const guidance::RoutePtr&, routing::ParkingRouteType){ });

    routeEditor_.reset(new RouteEditorImpl(
        guidance_.get(),
        &historyManager_,
        &bugReportManager_,
        this,
        defaultLocationProvider_.get(),
        parkingRouteBuilder_.get()));
    routeEditor_->initialize();
    routeEditor_->addListener(subscriptionFromThis());
}

void RouteEditorTestFixture::setLocation(const std::string& position, double heading)
{
    async::runOnUiThread([=] { guide_->setLocation(points_.at(position), heading); });
}

void RouteEditorTestFixture::startDriving()
{
    assertNotUi();
    async::runOnUiThread([=] {
        ASSERT(route_, "RouteEditorTestFixture.startDriving: no route to drive");
        guide_->setRoute(route_);
        guide_->startDriving();
    });
}

void RouteEditorTestFixture::driveTo(const geometry::PolylinePosition& routePos)
{
    assertNotUi();
    async::runOnUiThread([=] { guide_->driveTo(routePos); });
}

void RouteEditorTestFixture::finishDriving()
{
    assertNotUi();
    async::runOnUiThread([=] { guide_->finishDriving(); });
}

std::string RouteEditorTestFixture::nameOf(const geometry::Point& point) const
{
    using geometry::sign;

    for (const auto& it : points_) {
        const auto& pt = it.second;
        if (sign(pt.latitude - point.latitude) == 0 && sign(pt.longitude - point.longitude) == 0)
            return it.first;
    }
    std::stringstream str;
    str << '{' << point.latitude << ", " << point.longitude << '}';
    return str.str();
}

geometry::Point RouteEditorTestFixture::pointFor(const std::string& name) const
{
    return *getPosition(*geoObjectFor(name));
}

std::shared_ptr<GeoObject> RouteEditorTestFixture::geoObjectFor(const std::string& name) const
{
    return geoObjects_.at(name);
}

std::shared_ptr<driving::Route> RouteEditorTestFixture::createRoute(
    const std::string& from,
    const std::string& to,
    RequestPointType pointType,
    const std::string& via)
{
    auto points = std::make_shared<bindings::SharedVector<RequestPoint>>();
    points->emplace_back(pointFor(from), RequestPointType::Waypoint, boost::none);
    points->emplace_back(pointFor(via), pointType, boost::none);
    points->emplace_back(pointFor(to), RequestPointType::Waypoint, boost::none);

    const auto route = std::make_shared<MyRoute>(points);
    route->resetRequestPoints();  // deserialized route has no "requestPoints"
    return route;
}

void RouteEditorTestFixture::checkRoute(const std::string& route, UserAction action) const
{
    checkRoute("", route, action);
}

void RouteEditorTestFixture::checkRoute(
    const std::string& routeType, const std::string& route, UserAction action) const
{
    async::ui()->spawn([=] {

        const StringVector& log = getLog();
        const std::string result = log.empty() ? "-" : log.back();
        const std::string ok =
            "MyDrivingRouter.request" + routeType + "Routes(points=" + route + ")";
        if (result != ok) {
            BOOST_CHECK_MESSAGE(false,
                "RouteEditorTest.checkRoute: FAILED\n"
                << "Result:   " << result << "\n"
                << "Expected: " << ok << "\n\n");
            return;
        }

        if (lastAction_ != action) {
            BOOST_CHECK_MESSAGE(false,
                "RouteEditorTest.checkRoute (action): FAILED\n"
                << "Result: " << (lastAction_ ? toString(*lastAction_) : "none") << "\n"
                << "Expected: " << toString(action) << "\n\n");
            return;
        }

        BOOST_CHECK(true);
    })
    .wait();
}

void RouteEditorTestFixture::checkNoRoute() const
{
    async::ui()
        ->spawn([=] {
            const StringVector& log = getLog();
            const std::string lastRecord = log.empty() ? "" : log.back();
            BOOST_REQUIRE(!boost::starts_with(lastRecord, "MyDrivingRouter.request"));
        })
        .wait();
}

void RouteEditorTestFixture::checkNoPanels() const
{
    async::ui()->spawn([&] {

        BOOST_CHECK(!hasLocatingPanel_);
        BOOST_CHECK(!hasRoutingPanel_);

    })
    .wait();
}

void RouteEditorTestFixture::checkLocatingPanel(bool value) const
{
    async::ui()->spawn([&] {

        BOOST_CHECK(hasLocatingPanel_ == value);

    })
    .wait();
}

void RouteEditorTestFixture::checkRoutingPanel(bool value) const
{
    async::ui()->spawn([&] {

        BOOST_CHECK(hasRoutingPanel_ == value);

    })
    .wait();
}

void RouteEditorTestFixture::setPoints(
    const std::string& from, const std::string& to, const StringVector& via, UserAction action)
{
    async::runOnUiThread([=] {
        const std::vector<RequestPointType> viaTypes(via.size(), RequestPointType::Waypoint);
        std::vector<std::shared_ptr<GeoObject>> viaGeo;
        for (const auto& it : via)
            viaGeo.push_back(geoObjectFor(it));

        routeEditor_->setPoints(geoObjectFor(from), geoObjectFor(to), viaTypes, viaGeo, action);
    });
}

void RouteEditorTestFixture::sendNetworkRequest()
{
    if (networkEnabled_)
        return;

    throw network::NetworkException() << "sendNetworkRequest: Network error";
}

void RouteEditorTestFixture::doTestPassedViaPoint(RequestPointType viaPointType)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");

    addViaPoint(viaPointType, "PETROVSKY_PALACE");

    waitForRoute();
    const auto expectedRoute = format(
        "[YANDEX, %sPETROVSKY_PALACE, ZELENOGRAD]",
        viaPointType == RequestPointType::Viapoint ? "Via:" : "");
    checkRoute(expectedRoute, UserAction::SetPoint);
    checkNoPanels();

    startDriving();
    BOOST_CHECK(getViaPointTypeAt(0) == viaPointType);

    driveTo({0, 0.99});
    BOOST_CHECK(getViaPointTypeAt(0) == viaPointType);

    driveTo({1, 0.0});
    BOOST_CHECK(!hasViaPoints());
}

}  // anonymous namespace

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Test suite
//

BOOST_FIXTURE_TEST_SUITE(RouteEditorTests, RouteEditorTestFixture)

BOOST_AUTO_TEST_CASE(testRouteFromHere)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");

    waitForRoute();
    checkRoute("[YANDEX, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testSetFromPointBeforeTo)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setFromPoint("YANDEX");
    setToPoint("ZELENOGRAD");

    waitForRoute();
    checkRoute("[YANDEX, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testRouteFromHereWithoutLocation)
{
    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");

    waitForRoute();
    checkRoute("[YANDEX, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();

    reset(ResetMode::ClearAll);
    setToPoint("ZELENOGRAD");

    checkLocatingPanel(true);
}

BOOST_AUTO_TEST_CASE(testRouteFromHereWithVia)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");
    addViaPoint(RequestPointType::Waypoint, "PETROVSKY_PALACE");

    waitForRoute();
    checkRoute("[YANDEX, PETROVSKY_PALACE, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testRouteFromHereTo)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");
    setToPoint("PETROVSKY_PALACE");

    waitForRoute();
    checkRoute("[MOSCOW_CENTER, PETROVSKY_PALACE]", UserAction::SetPoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testRouteFromHereWithViaAndTo)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");
    addViaPoint(RequestPointType::Waypoint, "PETROVSKY_PALACE");
    setToPoint("ZELENOGRAD");

    waitForRoute();
    checkRoute("[MOSCOW_CENTER, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testMoveAuxPoint)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    insertViaPoint(0, RequestPointType::Viapoint, "PETROVSKY_PALACE", UserAction::MovePoint);

    waitForRoute();
    checkRoute("[MOSCOW_CENTER, Via:PETROVSKY_PALACE, ZELENOGRAD]", UserAction::MovePoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testOffline)
{
    setNetwork(false);
    setLocation("YANDEX", 0.0);
    setToPoint("ZELENOGRAD");

    checkNoRoute();
    checkRoutingPanel(true);

    cancelRouting();
    checkNoPanels();

    async::sleepFor(6s);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testMoveFromPin)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");

    waitForRoute();
    checkRoute("[MOSCOW_CENTER, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();

    moveFromPin("YANDEX");

    waitForRoute();
    checkRoute("[YANDEX, ZELENOGRAD]", UserAction::MovePoint);
    checkNoPanels();

    addViaPoint(RequestPointType::Waypoint, "PETROVSKY_PALACE");

    waitForRoute();
    checkRoute("[YANDEX, PETROVSKY_PALACE, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testMoveToPin)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");
    moveToPin("PETROVSKY_PALACE");

    waitForRoute();
    checkRoute("[YANDEX, PETROVSKY_PALACE]", UserAction::MovePoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testSetNullPoints)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setPoints("-", "-", {});

    BOOST_CHECK(true);
}

BOOST_AUTO_TEST_CASE(testSetPoints)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setPoints(/* from= */ "YANDEX", /* to= */ "-", /* via= */ {"ZELENOGRAD"});

    waitForRoute();
    checkRoute("[YANDEX, ZELENOGRAD]", UserAction::SetPoint);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testStartDriving)
{
    setLocation("MOSCOW_CENTER", 0.0);
    setToPoint("ZELENOGRAD");
    waitForRoute();

    BOOST_CHECK(hasFromPoint());

    startDriving();

    BOOST_CHECK(!hasFromPoint());
}

BOOST_AUTO_TEST_CASE(testPassedViaPoint)
{
    doTestPassedViaPoint(RequestPointType::Viapoint);
}

BOOST_AUTO_TEST_CASE(testPassedWayPoint)
{
    doTestPassedViaPoint(RequestPointType::Waypoint);
}

BOOST_AUTO_TEST_CASE(testVoiceCommandWithoutLocation)
{
    setPoints("-", "ZELENOGRAD", {}, UserAction::Voice);
    setLocation("YANDEX", 0.0);

    waitForRoute();
    checkRoute("[YANDEX, ZELENOGRAD]", UserAction::Voice);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testParkingRoute)
{
    setLocation("ZELENOGRAD", 0.0);
    createParkingRoute();

    waitForRoute();
    checkRoute("Parking", "[ZELENOGRAD, Via:ZELENOGRAD, ZELENOGRAD]", UserAction::Parking);
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testParkingRouteRightBeforeFinishing)
{
    setLocation("MOSCOW_CENTER", 0.0);

    setToPoint("ZELENOGRAD");
    setFromPoint("YANDEX");

    waitForRoute();
    startDriving();

    async::runOnUiThread([=] {
        routeEditor()->createParkingRoute();
    });

    finishDriving();
    waitForRoute();
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testRoutingDisabledBothPoints)
{
    setRoutingAvailable(false);

    setLocation("MOSCOW_CENTER", 0.0);

    setFromPoint("YANDEX");
    setToPoint("ZELENOGRAD");

    BOOST_CHECK(!hasFromPoint());
    BOOST_CHECK(!hasToPoint());
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testRoutingDisabledToPoint)
{
    setRoutingAvailable(false);

    setLocation("MOSCOW_CENTER", 0.0);
    setToPoint("ZELENOGRAD");

    BOOST_CHECK(!hasFromPoint());
    BOOST_CHECK(!hasToPoint());
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testRoutingDisabledFromPointNotSet)
{
    setRoutingAvailable(false);

    setLocation("MOSCOW_CENTER", 0.0);
    setFromPoint("ZELENOGRAD");

    BOOST_CHECK(!hasFromPoint());
    checkNoPanels();
}

BOOST_AUTO_TEST_CASE(testRestorePoints)
{
    restorePoints(
        createRoute("YANDEX", "ZELENOGRAD", RequestPointType::Viapoint, "PETROVSKY_PALACE"),
        ResetMode::ClearAll);

    BOOST_CHECK(hasFromPoint());
    BOOST_CHECK(hasToPoint());
    BOOST_CHECK(getViaPointTypeAt(0) == RequestPointType::Viapoint);
}

BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex
