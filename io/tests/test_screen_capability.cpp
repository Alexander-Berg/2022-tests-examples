#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/services/aliced/capabilities/screen_capability/screen_capability.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>

#include <yandex_io/services/aliced/device_state/alice_device_state.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/null_device_state_capability/null_device_state_capability.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    class ScreenCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

    private:
        void init() {
            const auto factory = ipcFactoryForTests();
            directiveProcessor_ = std::make_shared<MockIDirectiveProcessor>();
            interfacedMock_ = factory->allocateGMockIpcConnector("interfaced");
            capability_ = std::make_shared<ScreenCapability>(std::make_shared<NullDeviceStateCapability>(), std::make_shared<TestCallbackQueue>(), directiveProcessor_, deviceState_, interfacedMock_);
        }

    public:
        AliceDeviceState deviceState_{"", nullptr, nullptr, EnvironmentStateHolder("", nullptr)};
        std::shared_ptr<MockIDirectiveProcessor> directiveProcessor_;
        std::shared_ptr<ipc::mock::MockIConnector> interfacedMock_;
        std::shared_ptr<ScreenCapability> capability_;
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE(ScreenCapabilityTest) {
    Y_UNIT_TEST_F(testHandleDirectiveScreenOn, ScreenCapabilityFixture) {
        auto directive = std::make_shared<Directive>(Directive::Data(Directives::SCREEN_ON, "local_action"));

        EXPECT_CALL(*interfacedMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_)))
            .WillOnce(Invoke([](const quasar::ipc::SharedMessage& sharedMessage) {
                auto message = *sharedMessage;
                UNIT_ASSERT(message.has_media_message());
                UNIT_ASSERT(message.media_message().has_hdmi_on());

                return true;
            }));
        capability_->handleDirective(directive);
    }

    Y_UNIT_TEST_F(testHandleDirectiveScreenOff, ScreenCapabilityFixture) {
        auto directive = std::make_shared<Directive>(Directive::Data(Directives::SCREEN_OFF, "local_action"));

        EXPECT_CALL(*interfacedMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_))).WillOnce(Invoke([](const quasar::ipc::SharedMessage& sharedMessage) {
            auto message = *sharedMessage;
            UNIT_ASSERT(message.has_media_message());
            UNIT_ASSERT(message.media_message().has_hdmi_off());

            return true;
        }));
        capability_->handleDirective(directive);
    }
}
