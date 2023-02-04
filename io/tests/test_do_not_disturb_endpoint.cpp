#include <yandex_io/services/do_not_disturb/do_not_disturb_endpoint.h>

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

TimeInfo createTimeInfo(const std::time_t time) {
    const std::tm calendar_time = *std::gmtime(std::addressof(time));
    return TimeInfo(calendar_time.tm_hour, calendar_time.tm_min, calendar_time.tm_sec);
}

class DoNotDisturbFixture: public QuasarUnitTestFixture {
public:
    YandexIO::Configuration::TestGuard testGuard_;

    std::shared_ptr<ipc::IServer> mockNtp_;
    std::shared_ptr<ipc::IServer> mockSync_;

    std::unique_ptr<DoNotDisturbEndpoint> endpoint;

    using Base = QuasarUnitTestFixture;

    void SetUp(NUnitTest::TTestContext& context) override {
        Base::SetUp(context);

        mockSync_ = createIpcServerForTests("syncd");
        mockSync_->listenService();

        mockNtp_ = createIpcServerForTests("ntpd");
        mockNtp_->listenService();

        endpoint = std::make_unique<DoNotDisturbEndpoint>(getDeviceForTests(),
                                                          ipcFactoryForTests());
    }

    void TearDown(NUnitTest::TTestContext& context) override {
        Base::TearDown(context);
    }

    void setDndTime(const std::time_t startDnd, const std::time_t endDnd) const {
        Json::Value config;
        Json::Value systemConfig;
        Json::Value dndConfig;

        dndConfig["start_time"] = createTimeInfo(startDnd).to_string();
        dndConfig["end_time"] = createTimeInfo(endDnd).to_string();

        systemConfig["dnd_prefs"] = dndConfig;

        config["system_config"] = systemConfig;

        YIO_LOG_INFO("Send config: " << jsonToString(config));

        QuasarMessage msg;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSync_->waitConnectionsAtLeast(1); // make sure dnd connected to syncd
        mockSync_->sendToAll(std::move(msg));
    }
};

Y_UNIT_TEST_SUITE(do_not_disturb) {
    Y_UNIT_TEST_F(testDoNotDisturbEndpoint, DoNotDisturbFixture) {
        const int expectedDndEventsCount = 3;
        std::atomic_int dndEventsCounter{0};

        SteadyConditionVariable condVar;

        auto connector = createIpcConnectorForTests("do_not_disturb");
        connector->setMessageHandler([&](const auto& msg) {
            YIO_LOG_INFO("Got quasar message: " << msg->DebugString());
            switch (dndEventsCounter.load()) {
                case 0:
                    // we should recv first dnd message when connected to do_not_disturb
                    UNIT_ASSERT(!msg->do_not_disturb_event().is_dnd_enabled());
                    break;
                case 1:
                    // we should recv second dnd message when system_config will be updated
                    UNIT_ASSERT(!msg->do_not_disturb_event().is_dnd_enabled());
                    break;
                case 2:
                    // after one second dnd should be enabled according to config
                    UNIT_ASSERT(msg->do_not_disturb_event().is_dnd_enabled());
                    break;
                case 3:
                    // after one more second dnd should be disabled according to config
                    UNIT_ASSERT(!msg->do_not_disturb_event().is_dnd_enabled());
                    break;
            }
            dndEventsCounter++;
            condVar.notify_one();
        });

        connector->connectToService();
        connector->waitUntilConnected();

        /* wait "onConnected" message before updating system config */
        waitUntil(condVar, [&]() { return dndEventsCounter == 1; });

        const std::time_t currentTime = std::time(nullptr);
        setDndTime(currentTime + 10, currentTime + 15);

        waitUntil(condVar, [&]() { return dndEventsCounter == expectedDndEventsCount; });
    }
}
