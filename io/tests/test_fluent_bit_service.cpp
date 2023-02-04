#include <yandex_io/services/fluent-bitd/fluent_bit_service.h>
#include <yandex_io/services/fluent-bitd/i_fluent_bit.h>

#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <fstream>

using quasar::proto::QuasarMessage;

using namespace quasar;
using namespace TestUtils;

namespace {

    class MockClient: public IFluentBit {
    public:
        bool initCalled;
        std::promise<void> configCalled;

        void init() override {
            initCalled = true;
        }

        void teardown() override {
        }

        void processNewConfig(const Json::Value& /*config*/) override {
            configCalled.set_value();
        }
    };

    class MockFluentBitService: public FluentBitService {
        using FluentBitService::FluentBitService;

    public:
        MockClient* getClient() {
            return dynamic_cast<MockClient*>(client_.get());
        }
    };

    class Fixture: public QuasarUnitTestFixture {
    public:
        YandexIO::Configuration::TestGuard testGuard_;

        std::unique_ptr<MockFluentBitService> testFluentBitService_;
        std::shared_ptr<ipc::IServer> mockSyncd;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            mockSyncd = createIpcServerForTests("syncd");
            mockSyncd->listenService();

            testFluentBitService_ = std::make_unique<MockFluentBitService>(
                getDeviceForTests(),
                ipcFactoryForTests(),
                std::make_unique<MockClient>());
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };

} /* anonymous namespace */

Y_UNIT_TEST_SUITE_F(FluentBitServiceTests, Fixture) {
    Y_UNIT_TEST(testFluentBitServiceStart) {
        testFluentBitService_->start();
        UNIT_ASSERT(testFluentBitService_->getClient()->initCalled);
    }

    Y_UNIT_TEST(testFluentBitServiceConfig) {
        testFluentBitService_->start();
        mockSyncd->waitConnectionsAtLeast(1);

        QuasarMessage message;
        Json::Value userConfig;
        userConfig["system_config"]["fluent-bit"]["enabled"] = true;
        message.mutable_user_config_update()->set_config(jsonToString(userConfig));
        mockSyncd->sendToAll(std::move(message));

        testFluentBitService_->getClient()->configCalled.get_future().wait();
    }
}
