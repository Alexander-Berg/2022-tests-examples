#include "../menu/annotation_warn_presenter.h"
#include "../menu/audio_settings_presenter.h"
#include "../menu/bug_reports_presenter.h"
#include "../menu/cursor_model_select_screen_presenter_creator.h"
#include "../menu/developer_settings_presenter.h"
#include "../menu/json_config_presenter.h"
#include "../menu/map_settings_presenter.h"
#include "../menu/menu_screen_presenter_old.h"
#include "../menu/misc_settings_presenter.h"
#include "../menu/navigation_settings_presenter.h"
#include "../menu/push_topics_presenter.h"
#include "../menu/road_events_presenter.h"
#include "../menu/settings_presenter.h"
#include "../menu/update_maps_presenter.h"
#include "init_navikit.h"

#include <yandex/maps/navi/extended_app_component.h>
#include <yandex/maps/navi/mocks/mock_audio_schemes_manager.h>
#include <yandex/maps/navi/mocks/mock_menu_screen.h>
#include <yandex/maps/navi/mocks/mock_message_box_factory.h>
#include <yandex/maps/navi/mocks/mock_ui_controllers.h>
#include <yandex/maps/navi/naviauto_sync/car.h>
#include <yandex/maps/navi/naviauto_sync/naviauto_sync_manager.h>
#include <yandex/maps/navi/test_environment.h>

#include <boost/test/unit_test.hpp>
#include <gmock/gmock.h>

namespace yandex::maps::navi::ui::menu {

using namespace testing;

namespace {

const auto EMPTY_FUNCTION = [](){};

class UITestFixture {
public:
    UITestFixture() :
            audioSchemesManager(std::make_shared<NiceMock<audio::MockAudioSchemesManager>>())
    {
        initAppComponentForTesting();

        EXPECT_CALL(*audioSchemesManager, schemes())
            .WillRepeatedly(Return(std::vector<audio::AudioScheme*>{&audioScheme}));
    }

    std::shared_ptr<NiceMock<audio::MockAudioSchemesManager>> audioSchemesManager;
    NiceMock<audio::MockAudioScheme> audioScheme;
};

void check(const std::shared_ptr<runtime::bindings::Vector<MenuSection*>>& sections) {
    for (auto section : *sections)
        BOOST_CHECK(!section->items()->empty());
}

void setScreen(const std::shared_ptr<MenuScreenPresenter>& presenter) {
    auto menuScreen = std::make_shared<NiceMock<menu::MockMenuScreen>>();
    EXPECT_CALL(*menuScreen, setMenuItems(_)).
        WillRepeatedly(Invoke(check));

    presenter->setView(menuScreen);
    presenter->dismiss();
}

template<typename Presenter, typename... Args>
void testPresenter(Args&&... args) {
    setScreen(std::make_shared<Presenter>(std::forward<Args>(args)...));
}

} // anonymous namespace

BOOST_FIXTURE_TEST_SUITE(UiMenuPresenterTests, UITestFixture)

BOOST_AUTO_TEST_CASE(testMenuScreenPresenterImpl) {
    UI(
        testPresenter<MenuScreenPresenterImpl>(
            getAppComponent()->appData(),
            getAppComponent()->authModel(),
            getAppComponent()->plusManager(),
            getAppComponent()->billingRestoreManager(),
            getAppComponent()->passportManager(),
            getAppComponent()->settingsManager(),
            getAppComponent()->experimentsManager(),
            getAppComponent()->searchHistoryManager(),
            getAppComponent()->pointsHistoryManager(),
            getAppComponent()->rideHistoryManager(),
            getAppComponent()->audioSchemesManager(),
            getAppComponent()->speaker(),
            getAppComponent()->cursorModelManager(),
            getAppComponent()->naviAutoSyncManager()->carsAvailable()
                ? getAppComponent()->naviAutoSyncManager()->carManager()
                : nullptr,
            /* carCardOpener */ [] (const naviauto_sync::Car&) {},
            std::make_shared<NiceMock<MockUiControllers>>(),
            nullptr,
            /* scaleFactor */ 1,
            getAppComponent()->backStack(),
            /* closeAction = */ EMPTY_FUNCTION,
            /* parkingAction = */ EMPTY_FUNCTION,
            /* gasStationsAction */ EMPTY_FUNCTION,
            getAppComponent()->carInfoManager(),
            std::make_unique<MenuMainScreenRibbonAdsPresenter>(
                getAppComponent()->carInfoManager(), 
                getAppComponent()->ribbonAdsManager(),
                getAppComponent()->experimentsManger())
        )
    );
}

BOOST_AUTO_TEST_CASE(testDeveloperSettingsPresenter) {
    UI(
        testPresenter<DeveloperSettingsPresenter>(
            std::make_shared<NiceMock<MockMessageBoxFactory>>(),
            /* provisioningUiController */ nullptr,
            /* parkingKit */ nullptr,
            /* balloonsGalleryManager */ nullptr,
            /* mapkitMap */ nullptr,
            "Developer Settings"
        )
    );
}

BOOST_AUTO_TEST_CASE(testNavigationSettingsPresenter) {
    UI(
        testPresenter<NavigationSettingsPresenter>(
            std::make_shared<NiceMock<MockUiControllers>>()
        )
    );
}

BOOST_AUTO_TEST_CASE(testMapSettingsPresenter) {
    UI(
        testPresenter<MapSettingsPresenter>(
            getAppComponent()->settingsManager(),
            getAppComponent()->nightModeManager(),
            std::make_shared<NiceMock<MockUiControllers>>(),
            getAppComponent()->backStack(),
            getAppComponent()->authModel(),
            getAppComponent()->experimentsManager()
        )
    );
}

BOOST_AUTO_TEST_CASE(testAudioSettingsPresenter) {
    UI(
        testPresenter<AudioSettingsPresenter>(
            audioSchemesManager.get(),
            getAppComponent()->speaker(),
            std::make_shared<NiceMock<MockUiControllers>>(),
            getAppComponent()->backStack()
        )
    );
}

BOOST_AUTO_TEST_CASE(testBugReportsPresenter) {
    UI(
        testPresenter<BugReportsPresenter>(
            getAppComponent()->settingsManager(),
            std::make_shared<NiceMock<MockMessageBoxFactory>>(),
            getAppComponent()->authModel()
        )
    );
}

BOOST_AUTO_TEST_CASE(testUpdateMapsPresenter) {
    UI(
        testPresenter<UpdateMapsPresenter>(
            getAppComponent()->settingsManager()
        );
    );
}

BOOST_AUTO_TEST_CASE(testCreateCursorModelSelectScreenPresenter) {
    UI(
        setScreen(
            createCursorModelSelectScreenPresenter(
                getAppComponent()->cursorModelManager(),
                std::make_shared<NiceMock<MockMessageBoxFactory>>(),
                getAppComponent()->backStack(),
                getAppComponent()->experimentsManager()
            )
        )
    );
}

BOOST_AUTO_TEST_CASE(testSettingsPresenter) {
    UI(
        testPresenter<SettingsPresenter>(
            getAppComponent()->searchHistoryManager(),
            getAppComponent()->pointsHistoryManager(),
            getAppComponent()->rideHistoryManager(),
            audioSchemesManager.get(),
            getAppComponent()->speaker(),
            getAppComponent()->cursorModelManager(),
            getAppComponent()->plusManager(),
            getAppComponent()->billingRestoreManager(),
            std::make_shared<NiceMock<MockUiControllers>>(),
            nullptr,
            false,
            getAppComponent()->backStack(),
            /* closeAction = */ EMPTY_FUNCTION,
            getAppComponent()->authModel()
        )
    );
}

BOOST_AUTO_TEST_CASE(testPushTopicsPresenter) {
    UI(
        testPresenter<PushTopicsPresenter>()
    );
}

BOOST_AUTO_TEST_CASE(testJsonConfigPresenter) {
    UI(
        testPresenter<JsonConfigPresenter>(
            std::make_shared<NiceMock<MockMessageBoxFactory>>(),
            std::string("Test title"),
            [] { return std::string(); },
            [] { return std::string(); },
            [](const std::string& /* str */) { return true; },
            [](const std::string& /* str */) {}
        )
    );
}

BOOST_AUTO_TEST_CASE(testAnnotationWarnPresenter) {
    UI(
        testPresenter<AnnotationWarnPresenter>(
            getAppComponent()->settingsManager(),
            std::make_shared<NiceMock<MockMessageBoxFactory>>(),
            getAppComponent()->backStack()
        )
    );
}

BOOST_AUTO_TEST_CASE(testRoadEventsPresenter) {
    UI(
        testPresenter<RoadEventsPresenter>(
            getAppComponent()->settingsManager(),
            getAppComponent()->backStack()
        )
    );
}

BOOST_AUTO_TEST_CASE(testMiscSettingsPresenter) {
    UI(
        auto presenter = std::make_shared<MiscSettingsPresenter>(
            std::make_shared<MiscMenuSettingsFactory>(
                getAppComponent()->settingsManager(),
                getAppComponent()->experimentsManager(),
                nullptr)
        );
        auto menuScreen = std::make_shared<NiceMock<menu::MockMenuScreen>>();

        EXPECT_CALL(*menuScreen, setMenuItems(_));

        presenter->setView(menuScreen);
        presenter->dismiss();
    );
}

BOOST_AUTO_TEST_SUITE_END()

}
