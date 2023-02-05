#include "init_navikit.h"

#include <yandex/maps/navikit/mocks/mock_night_mode_delegate.h>
#include <yandex/maps/navikit/mocks/mock_platform_night_mode_provider.h>
#include <yandex/maps/navi/extended_app_component.h>
#include <yandex/maps/navi/mocks/mock_alice_kit.h>
#include <yandex/maps/navi/mocks/mock_app_component_init_provider.h>
#include <yandex/maps/navi/mocks/mock_permission_delegate.h>
#include <yandex/maps/navi/mocks/mock_remote_auth_delegate.h>
#include <yandex/maps/navi/mocks/mock_renderer_player.h>
#include <yandex/maps/navi/mocks/mock_statusbar_delegate.h>
#include <yandex/maps/navi/mocks/mock_taximeter_action_forwarder.h>

#include <yandex/maps/mapkit/mapkit_factory.h>

#include <yandex/maps/runtime/async/dispatcher.h>

#include <yandex/maps/push/push_support_factory.h>
#include <yandex/maps/recording/recording_factory.h>

#include <gmock/gmock.h>

namespace yandex::maps::navi::ui {

using namespace testing;

namespace {

const auto EMPTY_FUNCTION = [](){};

}  // anonymous namespace

void initAppComponentForTesting() {
    static bool isInit = false;
    if (isInit)
        return;
    isInit = true;

    runtime::async::ui()->spawn(
        [&] {
            maps::push::getPushSupport();
            recording::getRecording();

            auto initProvider = std::make_shared<NiceMock<MockAppComponentInitProvider>>();
            EXPECT_CALL(*initProvider, useNaviprovider())
                .WillRepeatedly(Return(false));
            EXPECT_CALL(*initProvider, authAccount())
                .WillRepeatedly(Return(nullptr));
            EXPECT_CALL(*initProvider, aliceKit())
                .WillRepeatedly(Return(std::make_shared<NiceMock<alice::MockAliceKit>>()));
            EXPECT_CALL(*initProvider, permissionDelegate())
                .WillRepeatedly(Return(std::make_shared<NiceMock<permissions::MockPermissionDelegate>>()));
            EXPECT_CALL(*initProvider, statusBarDelegate())
                .WillRepeatedly(Return(std::make_shared<NiceMock<statusbar::MockStatusBarDelegate>>()));
            EXPECT_CALL(*initProvider, nightModeDelegate())
                .WillRepeatedly(Return(std::make_shared<NiceMock<navikit::night_mode::MockNightModeDelegate>>()));
            EXPECT_CALL(*initProvider, platformNightModeProvider())
                .WillRepeatedly(Return(std::make_shared<NiceMock<navikit::night_mode::MockPlatformNightModeProvider>>()));
            EXPECT_CALL(*initProvider, remoteAuthDelegate())
                .WillRepeatedly(Return(std::make_shared<NiceMock<auth::remote::MockRemoteAuthDelegate>>()));
            EXPECT_CALL(*initProvider, taximeterActionForwarder())
                .WillRepeatedly(Return(std::make_shared<NiceMock<taximeter_action_forwarder::MockTaximeterActionForwarder>>()));
            EXPECT_CALL(*initProvider, rendererPlayerFactory())
                .WillRepeatedly(Return(std::make_shared<NiceMock<audio::MockRendererPlayerFactory>>()));

            initAppComponent(
                initProvider,
                /* pauseHook = */ EMPTY_FUNCTION,
                /* resumeHook = */ EMPTY_FUNCTION,
                /* mapAreaProvider = */ nullptr
            );
        }).wait();
}

}
