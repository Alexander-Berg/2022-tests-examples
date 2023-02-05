#include "../data_manager.h"
#include "../estimates_manager.h"
#include "../internal/statistical_destination_predictor.h"
#include "destination_prediction_manager_stub.h"
#include "destination_prediction_manager_stubs.h"
#include "location_provider_stub.h"
#include "route_editor_stub.h"

#include <yandex/maps/navikit/destination_suggest/destination.h>
#include <yandex/maps/navikit/mocks/mock_app_data.h>
#include <yandex/maps/navikit/mocks/mock_config_manager.h>
#include <yandex/maps/navikit/mocks/mock_ride_history_manager.h>
#include <yandex/maps/navikit/mocks/mock_route.h>
#include <yandex/maps/navikit/mocks/mock_route_manager.h>
#include <yandex/maps/navi/test_environment.h>

#include <boost/test/unit_test.hpp>

using namespace testing;

namespace yandex::maps::navikit::destination_suggest::test {

namespace {

struct GeneralFixture {
    std::vector<Destination> destinations = {
        Destination(
            DestinationType::Home,
            mapkit::geometry::Point(1, 1),
            std::string("home title"),
            std::string("home subtitle"),
            std::string("home context"),
            DestinationLogInfo(),
            {}),
        Destination(
            DestinationType::Work,
            mapkit::geometry::Point(1, 1.1),
            std::string("work title"),
            std::string("work subtitle"),
            std::string("work context"),
            DestinationLogInfo(),
            {}),
        Destination(
            DestinationType::History,
            mapkit::geometry::Point(1, 1.2),
            std::string("history1 title"),
            std::string("history1 subtitle"),
            std::string("history1 context"),
            DestinationLogInfo(),
            {}),
        Destination(
            DestinationType::History,
            mapkit::geometry::Point(1, 1.3),
            std::string("history2 title"),
            std::string("history2 subtitle"),
            std::string("history2 context"),
            DestinationLogInfo(),
            {}),
    };
    DestinationPredictionManagerStub prediction;

    GeneralFixture() { prediction.setDestinations(destinations); }
    static bool isDestinationTheSame(const Destination& l, const Destination& r)
    {
        return l.type == r.type && l.title == r.title && l.point.latitude == r.point.latitude &&
               l.point.longitude == r.point.longitude;
    }
};

class EstimatesProviderTest : public impl::EstimatesProvider {
public:
    void requestEstimates(
        const mapkit::location::Location& /*location*/,
        const std::vector<Destination>& /*destinations*/) override
    {
        inProgress_ = true;
    }

    [[nodiscard]] bool isRequestInProgress() const override { return inProgress_; }

    void cancelRequest() override { inProgress_ = false; }

    void setCallbacks(
        std::function<void(std::vector<impl::Estimate>)> onSuccess,
        std::function<void(runtime::Error*)> onError) override
    {
        onSuccess_ = onSuccess;
        onError_ = onError;
    }

    void completeWithSuccess(std::vector<impl::Estimate> result)
    {
        onSuccess_(std::move(result));
        inProgress_ = false;
    }

    void completeWithError(runtime::Error* error)
    {
        onError_(error);
        inProgress_ = false;
    }
private:
    bool inProgress_ = false;
    std::function<void(std::vector<impl::Estimate>)> onSuccess_;
    std::function<void(runtime::Error*)> onError_;
};

struct DestinationsFixture : GeneralFixture {
    std::shared_ptr<impl::DataManager> manager;
    int notificationCount = 0;
    DestinationsFixture()
    {
        manager = impl::DestinationsManager::create(
            &prediction,
            nullptr /* externalDestinationProvider */);
        manager->setListener([this] { ++notificationCount; });
    }
};

struct DestinationPredictionFixture {
    std::shared_ptr<config::MockConfigManager> configManager;
    std::shared_ptr<MockAppData> appData = std::make_shared<MockAppData>();

    std::shared_ptr<providers::bookmarks::BookmarksProviderStub> bookmarksProvider =
        std::make_shared<providers::bookmarks::BookmarksProviderStub>();
    std::shared_ptr<providers::places::PlacesProviderStub> placesProvider =
        std::make_shared<providers::places::PlacesProviderStub>();

    std::shared_ptr<ride_history::MockRideHistoryManager> rideHistoryManager =
        std::make_shared<ride_history::MockRideHistoryManager>();
    std::shared_ptr<routing::MockRouteManager> routeManager =
        std::make_shared<routing::MockRouteManager>();

    std::shared_ptr<mapkit::search::SearchManagerStub> searchManager;

    std::shared_ptr<location::test::LocationProviderStub> locationProvider =
        std::make_shared<location::test::LocationProviderStub>();
    std::shared_ptr<ride_history::RideTypeProviderStub> rideTypeProvider =
        std::make_shared<ride_history::RideTypeProviderStub>();

    std::shared_ptr<DestinationPredictionManager> manager;

    class DestinationListenerImpl : public DestinationListener {
    public:
        DestinationListenerImpl(runtime::async::MultiPromise<int>&& promise) :
            promise_(std::move(promise))
        {
        }

        virtual void onDestinationsChanged() override
        {
            promise_.yield(1);
        }

    private:
        runtime::async::MultiPromise<int> promise_;
    };

    runtime::async::MultiFuture<int> listenerFuture;
    std::shared_ptr<DestinationListenerImpl> listener;

    DestinationPredictionFixture()
    {
        // One-time initialization goes here, as opposed to anything that happens in createManager(...).

        // AppData must not return boost::none for device ID:
        static const boost::optional<std::string> deviceId = std::string();
        EXPECT_CALL(*appData, deviceId()).WillRepeatedly(ReturnRef(deviceId));

        // Coordinates here must not be too close to those inside RideHistoryManager stub! If they
        // are too close, SearchManager will not be invoked, and we won't be able to simulate long
        // work with createManager(...) method's workDuration argument.
        locationProvider->setLocation(mapkit::directions::guidance::ClassifiedLocation(
            mapkit::location::Location(
                mapkit::geometry::Point(55.5, 37.5),
                { }, { }, { }, { }, { }, { }, { }, { }),
            mapkit::directions::guidance::LocationClass::Fine,
            mapkit::directions::guidance::NeedCameraJump::No));

        // Ride history data must be ready:
        EXPECT_CALL(*rideHistoryManager, isDataReady()).WillRepeatedly(Return(true));

        // There must be at least one item in ride history:
        std::shared_ptr<runtime::bindings::SharedVector<ride_history::RideInfo>> history =
            std::make_shared<runtime::bindings::SharedVector<ride_history::RideInfo>>(
                std::vector<ride_history::RideInfo>{ {
                    ride_history::RideInfoPoint{ mapkit::geometry::Point(55.0, 37.0), { }, "from_title", "from_address", std::string("ru") },
                    ride_history::RideInfoPoint{ mapkit::geometry::Point(56.0, 38.0), { }, "to_title", "to_address", std::string("ru") },
                    ride_history::RideType::Car,
                    runtime::now<runtime::AbsoluteTimestamp>()
                } }
            );
        EXPECT_CALL(*rideHistoryManager, history()).WillRepeatedly(Return(history));

        // We need to appear to be driving on route:
        EXPECT_CALL(*routeManager, routeState()).WillRepeatedly(Return(routing::RouteState::DriveOnRoute));

        // Need to ignore not interesting mock function calls, because we use mocks (GMocks) to
        // implement our stubs:
        EXPECT_CALL(*routeManager, addListener(_));
        EXPECT_CALL(*rideHistoryManager, addRideHistoryListener(_));
    }

    /**
     * @param workDuration - makes prediction longer by this amount, so that it's not basically
     *                       instantaneous. And that is important so that tests take longer than
     *                       short timeouts and shorter than long timeouts, and do not flap.
     */
    void createManager(runtime::TimeInterval timeoutDuration, runtime::TimeInterval workDuration)
    {
        configManager = std::make_shared<config::MockConfigManager>();

        static config::DestinationSuggest destinationSuggest;
        destinationSuggest.predictorTimeout = timeoutDuration;
        EXPECT_CALL(*configManager, destinationSuggest())
            .WillRepeatedly(ReturnRef(destinationSuggest));

        searchManager = std::make_shared<mapkit::search::SearchManagerStub>(workDuration);

        manager = createDestinationPredictionManager(
            configManager.get(), appData.get(),
            bookmarksProvider, placesProvider,
            rideHistoryManager.get(), routeManager.get(),
            searchManager.get(),
            locationProvider.get(),
            nullptr /* experimentsManager */,
            rideTypeProvider,
            StatisticalModel::Navi);
        manager->resume();

        runtime::async::MultiPromise<int> listenerPromise;
        listenerFuture = listenerPromise.future();
        listener = std::make_shared<DestinationListenerImpl>(std::move(listenerPromise));

        manager->addListener(listener);
    }
};

struct EstimatesFixture : GeneralFixture {
    location::test::LocationProviderStub locationProviderStub;
    std::shared_ptr<impl::DataManager> manager;
    EstimatesProviderTest* estimatesProviderTest = nullptr;
    int notificationCount = 0;

    EstimatesFixture()
    {
        estimatesProviderTest = new EstimatesProviderTest();

        manager = impl::EstimatesManager::create(
            &prediction,
            nullptr,  // DestinationProvider
            &locationProviderStub,
            std::unique_ptr<impl::EstimatesProvider>(estimatesProviderTest));
        manager->setListener([this] { ++notificationCount; });
    }

    std::vector<impl::Estimate> generateEstimates(
        mapkit::location::Location location, runtime::AbsoluteTimestamp timestamp)
    {
        using namespace mapkit::directions::driving;
        Weight weight(mapkit::LocalizedValue(10, ""),
                      mapkit::LocalizedValue(10, ""),
                      mapkit::LocalizedValue(10, ""));
        Summary summary(weight, Flags());
        mapkit::geometry::Point badPoint(1, 0);
        return {
            impl::Estimate{destinations[0].point, summary, location, timestamp},
            impl::Estimate{destinations[2].point, summary, location, timestamp},
            impl::Estimate{badPoint, summary, location, timestamp}};
    }

    void finishRequestSuccessfully()
    {
        estimatesProviderTest->completeWithSuccess(generateEstimates(
            mapkit::location::Location(), runtime::now<runtime::AbsoluteTimestamp>()));
    }

    void setFineLocation()
    {
        locationProviderStub.setLocation(mapkit::directions::guidance::ClassifiedLocation(
            mapkit::location::Location(),
            mapkit::directions::guidance::LocationClass::Fine,
            mapkit::directions::guidance::NeedCameraJump::No));
        locationProviderStub.notify();  // here we have to make request
    }

    void setCoarseLocation()
    {
        locationProviderStub.setLocation(mapkit::directions::guidance::ClassifiedLocation(
            mapkit::location::Location(),
            mapkit::directions::guidance::LocationClass::Coarse,
            mapkit::directions::guidance::NeedCameraJump::No));
        locationProviderStub.notify();  // here we have to make request
    }
};

void ensureTimeIntervalInRange(
    runtime::TimeInterval interval,
    runtime::TimeInterval minValue,
    runtime::TimeInterval maxValue)
{
    // Use heuristic time-measurement safety margins at around 10%:
    BOOST_TEST(interval.count() >= minValue.count() * 9 / 10);
    BOOST_TEST(interval.count() <= maxValue.count() * 10 / 9);
}

} // namespace

BOOST_AUTO_TEST_SUITE(DestinationManager)

BOOST_AUTO_TEST_CASE(creationOfDestinationManager)
{
    DestinationsFixture fixture;

    BOOST_TEST(fixture.manager != nullptr);
}

BOOST_FIXTURE_TEST_CASE(stateAfterCreation, DestinationsFixture)
{
    UI(
        auto actual = manager->data();
        BOOST_CHECK(actual);
        BOOST_CHECK(actual->empty());
        BOOST_CHECK_EQUAL(notificationCount, 0);
    );
}

BOOST_FIXTURE_TEST_CASE(stateAfterFirstResume, DestinationsFixture)
{
    UI(
        manager->resume();

        BOOST_CHECK_GE(notificationCount, 1);

        auto actual = manager->data();
        BOOST_CHECK(actual);

        BOOST_CHECK(std::equal(
           actual->begin(),
           actual->end(),
           destinations.begin(),
           destinations.end(),
           &isDestinationTheSame));
    );
}

BOOST_FIXTURE_TEST_CASE(notifycationPausedWhileSuspended, DestinationsFixture)
{
    UI(
        prediction.notify();
        BOOST_CHECK_EQUAL(notificationCount, 0);
    );
}

BOOST_FIXTURE_TEST_CASE(notificationIsCalledOnDestinationChanges, DestinationsFixture)
{
    UI(
        manager->resume();
        auto beforeDestinationChangesNotificationsCount = notificationCount;
        prediction.notify();
        BOOST_CHECK_GT(notificationCount, beforeDestinationChangesNotificationsCount);
    );

}

BOOST_FIXTURE_TEST_CASE(notificationIsCalledIfDestinationsChangedWhileSuspended, DestinationsFixture)
{
    UI(
        manager->resume();
        manager->suspend();
        notificationCount = 0;  // skip the first notification
        prediction.notify();
        BOOST_CHECK_EQUAL(0, notificationCount);
        manager->resume();
        BOOST_CHECK_EQUAL(1, notificationCount);
    );
}

BOOST_AUTO_TEST_SUITE_END()

// -------------- DestinationPredictionManager ------------------

BOOST_AUTO_TEST_SUITE(DestinationPredictionManager)

BOOST_AUTO_TEST_CASE(creationOfDestinationPredictionManagerWorks)
{
    BOOST_TEST(internal::PREDICTOR_MIN_HISTORY_SIZE == 1); // Required for RideHistoryManager stub

    DestinationPredictionFixture fixture;
    UI(
        fixture.createManager(std::chrono::milliseconds(4000L), std::chrono::milliseconds(100L));
    );
    BOOST_TEST(fixture.manager != nullptr);

    fixture.listenerFuture.get(); // Makes sure the test is complete and nothing is hanging
}

BOOST_AUTO_TEST_CASE(shortPredictionTimeoutWorks)
{
    const auto shortTimeout = std::chrono::milliseconds(1000L);
    const auto workDuration = std::chrono::milliseconds(2000L);

    const auto start = runtime::now<runtime::AbsoluteTimestamp>();

    DestinationPredictionFixture fixture;
    UI(
        fixture.createManager(shortTimeout, workDuration);
    );
    fixture.listenerFuture.get();

    const auto end = runtime::now<runtime::AbsoluteTimestamp>();
    ensureTimeIntervalInRange(end - start, shortTimeout, workDuration);
}

BOOST_AUTO_TEST_CASE(longPredictionTimeoutWorks)
{
    const auto longTimeout = std::chrono::milliseconds(10000L);
    const auto workDuration = std::chrono::milliseconds(2000L);

    const auto start = runtime::now<runtime::AbsoluteTimestamp>();

    DestinationPredictionFixture fixture;
    UI(
        fixture.createManager(longTimeout, workDuration);
    );
    fixture.listenerFuture.get();

    const auto end = runtime::now<runtime::AbsoluteTimestamp>();
    ensureTimeIntervalInRange(end - start, workDuration, longTimeout);
}

BOOST_AUTO_TEST_SUITE_END()

// -------------- EstimatesManager ------------------

BOOST_AUTO_TEST_SUITE(EstimatesManager)

BOOST_AUTO_TEST_CASE(creationOfEstimatesManager)
{
    EstimatesFixture fixture;
    BOOST_TEST(fixture.manager != nullptr);
}

BOOST_FIXTURE_TEST_CASE(stateRightAfterCreation, EstimatesFixture)
{
    UI(
        BOOST_CHECK(!manager->data());
        BOOST_TEST(!estimatesProviderTest->isRequestInProgress());
    );
}

BOOST_FIXTURE_TEST_CASE(notifycationPausedBeforeResumed, EstimatesFixture)
{
    UI(
        prediction.notify();
        locationProviderStub.notify();
        BOOST_CHECK_EQUAL(0, notificationCount);
    );
}

BOOST_FIXTURE_TEST_CASE(stateAfterFirstResume, EstimatesFixture)
{
    UI(
        setCoarseLocation();
        manager->resume();
    );

    BOOST_CHECK_GE(notificationCount, 1);
    BOOST_CHECK(!estimatesProviderTest->isRequestInProgress());
    BOOST_TEST(!manager->data()); // there is no data yet
}

BOOST_FIXTURE_TEST_CASE(gotRequestIfFineLocation, EstimatesFixture)
{
    UI(
        manager->resume();
        setFineLocation();
    );

    BOOST_TEST(estimatesProviderTest->isRequestInProgress());
}

BOOST_FIXTURE_TEST_CASE(gotNotEmptyDataAfterRequestCompleted, EstimatesFixture)
{
    UI(
        manager->resume();
        setFineLocation();
        notificationCount = 0;
        finishRequestSuccessfully();

        BOOST_CHECK_GE(notificationCount, 1);
        BOOST_CHECK((bool)manager->data() );
        BOOST_CHECK(!manager->data()->empty());
    );
}

BOOST_FIXTURE_TEST_CASE(gotNotificationWhenRouteCreated, EstimatesFixture)
{
    using namespace testing;
    ::yandex::maps::mapkit::RequestPoint requestPoint(
        destinations.front().point, {}, {}
        );

    auto requestPoints = std::make_shared<
        ::yandex::maps::runtime::bindings::SharedVector<
            ::yandex::maps::mapkit::RequestPoint
        >
    >();
    requestPoints->push_back(requestPoint);
    requestPoints->push_back(requestPoint);

    auto metadata = std::make_shared<::yandex::maps::mapkit::directions::driving::RouteMetadata>();
    metadata->weight.timeWithTraffic.value=1;
    metadata->weight.time.value = 1;

    auto route = std::make_shared<NiceMock<mapkit::directions::driving::MockRoute>>();
    EXPECT_CALL(*route, requestPoints()).WillRepeatedly(ReturnRef(requestPoints));
    EXPECT_CALL(*route, metadata()).WillRepeatedly(ReturnRef(metadata));

    auto routes = std::make_shared<runtime::bindings::SharedVector<mapkit::directions::driving::Route>>();
    routes->push_back_shared(route);

    auto routeEditorStub = std::make_shared<route_editor::RouteEditorStub>();

    UI(
        manager->resume();
        manager->setRouteEditor(routeEditorStub);
        notificationCount = 0;

        routeEditorStub->notifyRouteCreated(
                routes, {}
            );
        BOOST_CHECK_GE(notificationCount, 1);
    );
}


BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex::maps::navikit::destination_suggest::test
