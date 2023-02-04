#include <yandex_io/services/aliced/capabilities/external_command_capability/external_command_capability.h>

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/services/aliced/device_state/alice_device_state.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    MATCHER(VerifyBugReportIdMessage, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_bug_report_id()) {
            *result_listener << "There is no bug_report_id in the message";
            return false;
        }
        if (message.bug_report_id() != "1") {
            *result_listener << "bug_report_id != 1";
            return false;
        }

        return true;
    }

    class ExternalCommandCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

    private:
        void init() {
            toInterface_ = std::make_shared<NiceMock<ipc::mock::MockIConnector>>();
            toCall_ = createIpcConnectorForTests("calld");
            toNotification_ = createIpcConnectorForTests("notificationd");
            toBugReport_ = std::make_shared<NiceMock<ipc::mock::MockIConnector>>();

            capability_ = std::make_shared<ExternalCommandCapability>(
                getDeviceForTests(),
                toBugReport_,
                toInterface_,
                toCall_,
                toNotification_);
        }

    public:
        std::shared_ptr<IMultiroomProvider> multiroomProvider_;
        std::shared_ptr<NiceMock<ipc::mock::MockIConnector>> toBugReport_;
        std::shared_ptr<NiceMock<ipc::mock::MockIConnector>> toInterface_;
        std::shared_ptr<ipc::IConnector> toCall_;
        std::shared_ptr<ipc::IConnector> toNotification_;
        std::shared_ptr<ExternalCommandCapability> capability_;
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE(ExternalCommandCapabilityTest) {
    Y_UNIT_TEST_F(testBugReportDirective, ExternalCommandCapabilityFixture) {
        Json::Value payload;
        payload["id"] = "1";
        auto directive = std::make_shared<Directive>(
            Directive::Data(Directives::SEND_BUG_REPORT, "local_action", std::move(payload)));

        EXPECT_CALL(*toBugReport_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyBugReportIdMessage())));
        EXPECT_CALL(*toInterface_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyBugReportIdMessage())));
        capability_->handleDirective(directive);
    }
}
