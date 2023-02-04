#include <yandex_io/services/notificationd/notification_endpoint.h>

#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>
#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/stereo_pair/mock/stereo_pair_provider.h>
#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/json.h>

#include <exception>
#include <future>
#include <memory>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::proto;

class NotificationFixture: public QuasarUnitTestFixture {
public:
    std::shared_ptr<ipc::IServer> mockDoNotDisturb_;
    std::shared_ptr<ipc::IServer> mockIoHub_;
    std::shared_ptr<mock::AuthProvider> mockAuthProvider;
    std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider;
    std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider;
    std::shared_ptr<YandexIO::MockIFilePlayerCapability> mockFilePlayerCapability;

    std::unique_ptr<NotificationEndpoint> endpoint;

    using Base = QuasarUnitTestFixture;

    void SetUp(NUnitTest::TTestContext& context) override {
        Base::SetUp(context);

        mockDoNotDisturb_ = createIpcServerForTests("do_not_disturb");
        mockDoNotDisturb_->listenService();

        mockIoHub_ = createIpcServerForTests("iohub_services");
        mockIoHub_->listenService();

        mockAuthProvider = std::make_shared<mock::AuthProvider>();
        mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
        mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
        mockFilePlayerCapability = std::make_shared<YandexIO::MockIFilePlayerCapability>();

        endpoint = std::make_unique<NotificationEndpoint>(getDeviceForTests(),
                                                          ipcFactoryForTests(),
                                                          mockAuthProvider,
                                                          mockStereoPairProvider,
                                                          mockUserConfigProvider,
                                                          mockFilePlayerCapability);
    }

    void TearDown(NUnitTest::TTestContext& context) override {
        Base::TearDown(context);
    }
};

Y_UNIT_TEST_SUITE(notificationd) {
    Y_UNIT_TEST_F(testResetAfterAuthMessage, NotificationFixture) {
        std::atomic_int notificationCount{3};
        SteadyConditionVariable condVar;

        auto connector = createIpcConnectorForTests("notificationd");
        connector->setMessageHandler([&](const auto& msg) {
            YIO_LOG_INFO("Got quasar message: " << msg->DebugString());
            if (msg->has_notification_update_event()) {
                notificationCount = msg->notification_update_event().count();

                if (notificationCount == 0) {
                    condVar.notify_one();
                }
            }
        });

        connector->connectToService();
        connector->waitUntilConnected();

        mockAuthProvider->setOwner(
            AuthInfo2{
                .source = AuthInfo2::Source::AUTHD,
                .authToken = "authToken2",
                .passportUid = "Uid2",
                .tag = 1,
            });
        waitUntil(condVar, [&]() { return notificationCount == 0; });
    }
}
