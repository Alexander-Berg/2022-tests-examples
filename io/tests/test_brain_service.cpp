#include <yandex_io/services/braind/brain_service.h>

#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/json.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <future>
#include <memory>

using namespace quasar;

namespace {

    class Fixture: public QuasarUnitTestFixture {
    public:
        Fixture()
            : device_(getDeviceForTests())
                  {};

        void createBrainService() {
            brainService_ = std::make_unique<BrainService>(device_, ipcFactoryForTests());
            brainService_->start();
        }

        /* Data */
    protected:
        std::shared_ptr<YandexIO::IDevice> device_;
        std::unique_ptr<BrainService> brainService_;
        YandexIO::Configuration::TestGuard testGuard_;
    };

    std::vector<std::pair<std::chrono::system_clock::time_point, std::chrono::system_clock::time_point>> convertTimersState(const ::google::protobuf::RepeatedPtrField<proto::IOEvent::TimersTimingsState>& timers) {
        std::vector<std::pair<std::chrono::system_clock::time_point, std::chrono::system_clock::time_point>> result;
        result.reserve(timers.size());
        for (const auto& item : timers) {
            const auto startTs = std::chrono::time_point<std::chrono::system_clock>() + std::chrono::milliseconds(item.start_timer_ms());
            const auto endTs = std::chrono::time_point<std::chrono::system_clock>() + std::chrono::milliseconds(item.end_timer_ms());
            result.emplace_back(std::make_pair(startTs, endTs));
        }
        return result;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(BrainServiceTest, Fixture) {
    Y_UNIT_TEST(TestAlarmd) {
        std::map<std::string, std::shared_ptr<ipc::IServer>> mocks;
        auto startMockServer = [&](const std::string& name) {
            auto server = createIpcServerForTests(name);
            mocks[name] = server;
            server->listenService();
        };

        for (auto name : {"aliced", "calld", "do_not_disturb", "firstrund", "ntpd", "updatesd", "wifid", "notificationd", "iot"}) {
            startMockServer(name);
        }

        auto alarmdMock = createIpcServerForTests("alarmd");
        alarmdMock->listenService();

        const std::chrono::time_point<std::chrono::system_clock> timePoint = std::chrono::system_clock::now();
        auto testPromise = std::make_shared<std::promise<void>>();

        auto ioHubMock = createIpcServerForTests("iohub_services");
        ioHubMock->setMessageHandler([testPromise, timePoint](const auto& msg, const auto& /*connection*/) mutable {
            const auto timers = convertTimersState(msg->io_event().sdk_state().timers_timings());
            if (!timers.empty() && abs((timers[0].second - timePoint).count() - 30000000) < 5000000) {
                if (testPromise) {
                    testPromise->set_value();
                    testPromise.reset();
                }
            }
        });
        ioHubMock->listenService();

        createBrainService();
        YIO_LOG_INFO("Wait alarm connection");
        alarmdMock->waitConnectionsAtLeast(1);
        YIO_LOG_INFO("Wait hub connection");
        ioHubMock->waitConnectionsAtLeast(1);
        YIO_LOG_INFO("All connected");

        {
            proto::QuasarMessage msg;
            auto timersState = msg.mutable_timers_state();

            auto alarm = timersState->add_timers();
            alarm->set_alarm_type(proto::Alarm::TIMER);
            alarm->set_id("1");
            auto duration = timePoint.time_since_epoch();
            auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
            alarm->set_start_timestamp_ms(millis - 30000);
            alarm->set_duration_seconds(60);

            alarmdMock->sendToAll(std::move(msg));
        }
        testPromise->get_future().get();
        UNIT_ASSERT(true);
        YIO_LOG_INFO("Done...");
    }
}
