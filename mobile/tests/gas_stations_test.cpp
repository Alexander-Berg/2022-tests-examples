#include "../dummy/gas_stations_kit_stub_inl.h"
#include "../gas_stations_manager_impl.h"

#include <yandex/maps/navikit/default_request_factory.h>
#include <yandex/maps/navikit/mocks/mock_app_data.h>
#include <yandex/maps/navikit/mocks/mock_app_lifecycle_manager.h>
#include <yandex/maps/navikit/mocks/mock_auth_model.h>
#include <yandex/maps/navikit/mocks/mock_config_manager.h>
#include <yandex/maps/navikit/mocks/mock_experiments_manager.h>
#include <yandex/maps/navikit/mocks/mock_location_provider.h>
#include <yandex/maps/navikit/mocks/mock_projected_system_manager.h>
#include <yandex/maps/navikit/mocks/mock_route_manager.h>
#include <yandex/maps/navikit/mocks/mock_ui_experiments_manager.h>
#include <yandex/maps/navi/mocks/mock_interaction_feedback_manager.h>
#include <yandex/maps/navi/mocks/mock_network_manager.h>
#include <yandex/maps/navi/mocks/mock_search_manager.h>
#include <yandex/maps/navi/mocks/mock_settings_manager.h>
#include <yandex/maps/navi/test_environment.h>

#include <yandex/maps/mapkit/geometry/ostream_helpers.h>
#include <yandex/maps/mapkit/search/offline/common/ostream_helpers/bounding_box.h>
#include <yandex/maps/mapkit/search/offline/common/ostream_helpers/circle.h>
#include <yandex/maps/mapkit/search/suggest_session.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::gas_stations {

using namespace testing;
using namespace runtime;
using namespace std::literals::chrono_literals;
using Device = navikit::EnvironmentConfig::Device;

namespace {

const std::string TEST_STATIONS_DATA = R"([
    {
        "id":"e3070cddb76dd16af9198ea309984dc0",
        "name":"АЗС Astra №26",
        "location":{"lon":37.522795944757,"lat":55.3722903653378},
        "polygon":[
            {"lon":37.523049547188862,"lat":55.372450699998133},
            {"lon":37.522534295733408,"lat":55.372523858507215},
            {"lon":37.522325083633177,"lat":55.372183375263823},
            {"lon":37.523333861141509,"lat":55.372143747413851},
            {"lon":37.523626222127064,"lat":55.372394306888381}
        ],
        "prestable": false,
        "alien": false
    },
    {
        "id":"46e2059185f44d61b6aa578b1415ec49",
        "name":"Тестировочная АЗС",
        "location":{"lon":37.6225039999999,"lat":55.753215},
        "polygon":[
            {"lon":37.622385327734605,"lat":55.753645130275551},
            {"lon":37.621351305192206,"lat":55.753089659982159},
            {"lon":37.622338358942784,"lat":55.752570522313867},
            {"lon":37.623377744237018,"lat":55.753174302033663}
        ],
        "prestable": false,
        "alien": true
    }
])";

} // anonymous namespace

class GasStationsTestFixture {
public:
    GasStationsTestFixture()
        : uiExperimentsManager_(
            std::make_shared<NiceMock<mapkit::experiments::MockUiExperimentsManager>>())
    {
        static const navikit::config::GasStationsBanners gasStationsBanners;
        static const boost::optional<settings::CachedConfig> gasStationPolygons;
        static const boost::optional<std::string> strNone;

        services_.insert({navikit::config::Service::LegacyGasStationsPolygons,
                          navikit::config::ServiceUrls(
                              "https://yandexnavi.s3.yandex.net/gas_stations/stations-v2.json",
                              boost::none,
                              boost::none,
                              [this] { return settingsManager_.datatestingEnvironmentForced(); })});

        services_.insert({navikit::config::Service::GasStationsPolygons,
                          navikit::config::ServiceUrls(
                              "https://yandexnavi.s3.yandex.net/gas_stations/stations-v4.json",
                              boost::none,
                              boost::none,
                              [this] { return settingsManager_.datatestingEnvironmentForced(); })});

        EXPECT_CALL(networkManager_, get(_))
            .WillRepeatedly(Return(
                network::HttpResponse(
                    network::HttpStatus::OK,
                    /* headers = */ {{}},
                    TEST_STATIONS_DATA
                )
            )
        );

        EXPECT_CALL(authModel_, isTesterAccount())
            .WillRepeatedly(Return(true));

        EXPECT_CALL(configManager_, addListener(_)).Times(AnyNumber());
        EXPECT_CALL(configManager_, services())
            .WillRepeatedly(ReturnRef(services_));
        EXPECT_CALL(configManager_, gasStationsBanners())
            .WillRepeatedly(ReturnRef(gasStationsBanners));

        EXPECT_CALL(settingsManager_, jsonConfig(_))
            .WillRepeatedly(ReturnRef(gasStationPolygons));
        EXPECT_CALL(settingsManager_, fuelTypes())
            .WillRepeatedly(Return(std::make_shared<runtime::bindings::Vector<settings::FuelType>>()));
        EXPECT_CALL(experimentsManager_, experimentSnapshotValue(_))
            .WillRepeatedly(Return(boost::none));
        EXPECT_CALL(experimentsManager_, getFeatureValue(_))
            .WillRepeatedly(Return(boost::none));

        EXPECT_CALL(appData_, addListener(_)).Times(AnyNumber());
        EXPECT_CALL(appData_, deviceId()).WillRepeatedly(ReturnRef(strNone));
        EXPECT_CALL(appData_, uuid()).WillRepeatedly(ReturnRef(strNone));
    }

    bool isEnabled() {
        auto gasStationsManager = std::make_unique<GasStationsManagerImpl>(
            std::make_shared<dummy::GasStationsKitStub>(),
            &settingsManager_,
            &experimentsManager_,
            &authModel_,
            &configManager_,
            &projectedSystemManager_,
            &locationProvider_,
            &routeManager_,
            &appData_,
            locationManager_,
            &networkManager_,
            nullptr,
            &searchManager_,
            &interactionFeedbackManager_,
            &appLifecycleManager_,
            [] (const OffersVector&) { }
        );

        return gasStationsManager->isEnabled();
    }

    NiceMock<navikit::location::MockLocationProvider> locationProvider_;
    NiceMock<navikit::routing::MockRouteManager> routeManager_;
    NiceMock<navikit::projected_system::MockProjectedSystemManager> projectedSystemManager_;
    settings::MockSettingsManager settingsManager_;
    navikit::experiments::MockExperimentsManager experimentsManager_;
    std::map<navikit::config::Service, navikit::config::ServiceUrls> services_;
    navikit::config::MockConfigManager configManager_;
    navikit::MockAppData appData_;
    location::LocationManagerWrapper* locationManager_ = nullptr;
    NiceMock<navikit::MockAppLifecycleManager> appLifecycleManager_;

    std::shared_ptr<mapkit::experiments::MockUiExperimentsManager> uiExperimentsManager_;

protected:
    NiceMock<navikit::auth::MockAuthModel> authModel_;

private:
    NiceMock<MockNetworkManager> networkManager_;
    NiceMock<mapkit::search::MockSearchManager> searchManager_;
    NiceMock<interaction::MockInteractionFeedbackManager> interactionFeedbackManager_;
};

BOOST_FIXTURE_TEST_SUITE(GasStationsTests, GasStationsTestFixture)

BOOST_AUTO_TEST_CASE(testIsEnabled)
{
    EXPECT_CALL(projectedSystemManager_, isConnected())
        .WillRepeatedly(Return(false));

    const struct {
        Device device;
        bool isEnabled;
    }
    tests[] = {
        { Device::ANDROID,       true  },
        { Device::YA_AUTO,       true  },
        { Device::YA_CARSHARING, false },
        { Device::IPHONE,        true  }
    };

    UI(
        for (const auto& test : tests) {
            auto config = std::make_unique<navikit::TestEnvironmentConfig>(test.device);
            config->setUiExperimentsManager(uiExperimentsManager_);
            auto handle = navikit::getTestEnvironment()->setConfig(std::move(config));

            BOOST_TEST(isEnabled() == test.isEnabled, "Failed for device: " << test.device.model);
        }
    );
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace yandex
