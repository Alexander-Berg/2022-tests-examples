#include <yandex_io/services/alarmd/alarm_player.h>

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <util/folder/path.h>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {

    class AlarmPlayerFixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            auto basePath = tryGetRamDrivePath();
            quasarDirPath = JoinFsPaths(basePath, "quasarDir-" + makeUUID());
            queuedPlayerPath = JoinFsPaths(quasarDirPath, "queuedPlayer.dat");

            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

            // set high timeouts by default
            config["alarmd"]["needAlarmApproval"] = false;
            config["alarmd"]["approvalTimeoutMs"] = 100;
            config["alarmd"]["alarmTimerTimeoutSec"] = 10000;
            config["alarmd"]["alarmPlayerFile"] = queuedPlayerPath.GetPath();

            removeStorage();
            quasarDirPath.MkDirs();

            mockAlarmd = createIpcServerForTests("alarmd");
            mockAlarmd->listenService();

            mockIOHub = createIpcServerForTests("iohub_services");
            mockIOHub->setMessageHandler([this](const auto& msg, auto& /*connection*/) {
                if (!msg->has_io_event()) {
                    return;
                }
                const auto& ioEvent = msg->io_event();
                if (ioEvent.has_on_alarm_enqueued()) {
                    contextAlarmEnqueuedPromise.set_value(ioEvent.on_alarm_enqueued().type());
                }
                if (ioEvent.has_on_alarm_started()) {
                    contextAlarmStartedPromise.set_value(ioEvent.on_alarm_started());
                }
                if (ioEvent.has_on_alarm_stopped()) {
                    contextAlarmStoppedPromise.set_value(ioEvent.on_alarm_stopped().type());
                }
                if (ioEvent.has_on_alarm_stop_remaining_media()) {
                    contextAlarmStoppedRemainigMediaPromise.set_value();
                }
            });
            mockIOHub->listenService();

            mockAliced = createIpcServerForTests("aliced");
            mockAliced->setMessageHandler([&](const auto& request, auto& /*connection*/) {
                if (request->has_directive()) {
                    YIO_LOG_INFO("Aliced received command timer commands");
                    commandTimerPromise.set_value();
                }
            });
            mockAliced->listenService();

            startMockIpcServers({"metricad"});

            deviceContext = std::make_shared<YandexIO::DeviceContext>(ipcFactoryForTests());

            toAlarmd = createIpcConnectorForTests("alarmd");
            toAlarmd->setMessageHandler([this](const auto& msg) {
                if (msg->has_alarm_event() && msg->alarm_event().has_alarm_fired()) {
                    YIO_LOG_INFO("Alarm fired event received");
                    alarmFiredPromise.set_value(msg->alarm_event().alarm_fired());
                } else if (msg->has_alarm_event() && msg->alarm_event().has_alarm_approved()) {
                    YIO_LOG_INFO("Alarm approved event received");
                    alarmApprovedPromise.set_value(msg->alarm_event().alarm_approved());
                } else if (msg->has_alarm_event() && msg->alarm_event().has_alarm_stopped()) {
                    YIO_LOG_INFO("Alarm stopped event received");
                    alarmStoppedPromise.set_value(msg->alarm_event().alarm_stopped());
                }
            });

            toAlarmd->connectToService();
            toAlarmd->waitUntilConnected();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            toAlarmd.reset();
            alarmPlayer.reset();
            removeStorage();

            Base::TearDown(context);
        }

        void createAlarmPlayer() {
            alarmPlayer = std::make_shared<AlarmPlayer>(getDeviceForTests(), ipcFactoryForTests(), deviceContext, mockAlarmd);

            alarmPlayer->waitUntilConnected();
        }

        bool playerIsIdle() {
            return alarmPlayer->getPlayingAlarm().id().empty();
        }

        void removeStorage() {
            quasarDirPath.ForceDelete();
        }

        void sendApproveAlarm(const std::string& id) {
            proto::QuasarMessage message;
            message.mutable_io_control()->set_approve_alarm(TString(id));
            mockIOHub->sendToAll(std::move(message));
        }

        void confirmAlarmStarted(const proto::Alarm& alarm) {
            proto::AlarmEvent event;
            event.mutable_alarm_confirmed()->CopyFrom(alarm);

            alarmPlayer->onAlarmEvent(event);
        }

        static proto::Alarm prepareAlarm(proto::Alarm::AlarmType type) {
            proto::Alarm alarm;
            alarm.set_id(makeUUID());
            alarm.set_alarm_type(type);
            alarm.set_start_timestamp_ms(1536590330000);
            alarm.set_duration_seconds(120);
            if (type = proto::Alarm::COMMAND_TIMER) {
                alarm.mutable_command_list()->Add();
            }
            return alarm;
        }

        static bool confirmAlarmWithValues(const proto::Alarm& alarm, const std::string& expectedId, proto::Alarm::AlarmType expectedType) {
            if (alarm.id() != expectedId) {
                return false;
            }
            if (alarm.alarm_type() != expectedType) {
                return false;
            }
            return true;
        }

    protected:
        YandexIO::Configuration::TestGuard testGuard;

        TFsPath quasarDirPath;
        TFsPath queuedPlayerPath;

        std::shared_ptr<ipc::IServer> mockAlarmd;
        std::shared_ptr<ipc::IServer> mockAliced;
        std::shared_ptr<ipc::IServer> mockIOHub;

        std::shared_ptr<ipc::IConnector> toAlarmd;

        std::shared_ptr<YandexIO::DeviceContext> deviceContext;

        std::promise<proto::Alarm> alarmFiredPromise;
        std::promise<proto::Alarm> alarmApprovedPromise;
        std::promise<proto::AlarmEvent::StopEvent> alarmStoppedPromise;

        std::promise<proto::IOEvent_AlarmType> contextAlarmEnqueuedPromise;
        std::promise<proto::IOEvent_AlarmType> contextAlarmStartedPromise;
        std::promise<proto::IOEvent_AlarmType> contextAlarmStoppedPromise;
        std::promise<void> contextAlarmStoppedRemainigMediaPromise;

        std::promise<void> commandTimerPromise;

        std::shared_ptr<AlarmPlayer> alarmPlayer;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(AlarmPlayerTest, AlarmPlayerFixture) {
    Y_UNIT_TEST(testEnqueueAlarm_withoutApproval) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["needAlarmApproval"] = false;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::ALARM);

        UNIT_ASSERT(playerIsIdle());
        alarmPlayer->enqueueAlarm(alarm);

        auto alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));

        alarmFromEvent = alarmApprovedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));
    }

    Y_UNIT_TEST(testEnqueueAlarm_withApproval) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["needAlarmApproval"] = true;
        config["alarmd"]["approvalTimeoutMs"] = 100000;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::ALARM);

        alarmPlayer->enqueueAlarm(alarm);

        auto alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        auto startedType = contextAlarmEnqueuedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::CLASSIC_ALARM);

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));

        sendApproveAlarm(alarm.id());
        alarmFromEvent = alarmApprovedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));
    }

    Y_UNIT_TEST(testEnqueueAlarm_withApprovalTimeout) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["needAlarmApproval"] = true;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::ALARM);

        alarmPlayer->enqueueAlarm(alarm);

        auto alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        auto enqueuedType = contextAlarmEnqueuedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(enqueuedType, proto::IOEvent::CLASSIC_ALARM);

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));

        alarmFromEvent = alarmApprovedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::CLASSIC_ALARM);
    }

    Y_UNIT_TEST(testEnqueueAlarm_timer) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["needAlarmApproval"] = true;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::TIMER);

        alarmPlayer->enqueueAlarm(alarm);

        auto alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        auto enqueuedType = contextAlarmEnqueuedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(enqueuedType, proto::IOEvent::TIMER);

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));

        alarmFromEvent = alarmApprovedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::TIMER);
    }

    Y_UNIT_TEST(testEnqueueAlarm_mediaAlarm) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["needAlarmApproval"] = true;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::MEDIA_ALARM);

        alarmPlayer->enqueueAlarm(alarm);

        auto alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        auto enqueuedType = contextAlarmEnqueuedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(enqueuedType, proto::IOEvent::MEDIA_ALARM);

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));

        alarmFromEvent = alarmApprovedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::MEDIA_ALARM);
    }

    Y_UNIT_TEST(testEnqueueAlarm_mediaAlarm_mediaWasntConfirmed) {
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::MEDIA_ALARM);

        alarmPlayer->enqueueAlarm(alarm);

        auto alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));

        alarmFromEvent = alarmApprovedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        alarm.set_alarm_type(proto::Alarm::ALARM);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::CLASSIC_ALARM);

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));
    }

    Y_UNIT_TEST(testCancelAlarm_classicAlarm) {
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::ALARM);

        alarmPlayer->enqueueAlarm(alarm);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::CLASSIC_ALARM);

        alarmPlayer->cancelAlarm(false); // false here to test 'stop_media == true' anyway

        auto stoppedType = contextAlarmStoppedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(stoppedType, proto::IOEvent::CLASSIC_ALARM);

        auto stoppedEvent = alarmStoppedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(stoppedEvent.alarm(), alarm.id(), alarm.alarm_type()));
        UNIT_ASSERT_EQUAL(stoppedEvent.stop_media(), true);

        UNIT_ASSERT(playerIsIdle());
    }

    Y_UNIT_TEST(testCancelAlarm_timer) {
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::TIMER);

        alarmPlayer->enqueueAlarm(alarm);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::TIMER);

        alarmPlayer->cancelAlarm(false); // false here to test 'stop_media == true' anyway

        auto stoppedType = contextAlarmStoppedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(stoppedType, proto::IOEvent::TIMER);

        auto stoppedEvent = alarmStoppedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(stoppedEvent.alarm(), alarm.id(), alarm.alarm_type()));
        UNIT_ASSERT_EQUAL(stoppedEvent.stop_media(), true);

        UNIT_ASSERT(playerIsIdle());
    }

    Y_UNIT_TEST(testCancelAlarm_mediaAlarm_dontStopMedia) {
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::MEDIA_ALARM);

        alarmPlayer->enqueueAlarm(alarm);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::MEDIA_ALARM);

        alarmPlayer->cancelAlarm(true);

        auto stoppedType = contextAlarmStoppedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(stoppedType, proto::IOEvent::MEDIA_ALARM);

        auto stoppedEvent = alarmStoppedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(stoppedEvent.alarm(), alarm.id(), alarm.alarm_type()));
        UNIT_ASSERT_EQUAL(stoppedEvent.stop_media(), true);

        UNIT_ASSERT(playerIsIdle());

        alarmPlayer->stopAnyRemainingMedia();
        auto status = contextAlarmStoppedRemainigMediaPromise.get_future().wait_for(std::chrono::seconds(3));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testCancelAlarm_mediaAlarm_stopMediaLater) {
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::MEDIA_ALARM);

        alarmPlayer->enqueueAlarm(alarm);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::MEDIA_ALARM);

        alarmPlayer->cancelAlarm(false);

        auto stoppedType = contextAlarmStoppedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(stoppedType, proto::IOEvent::MEDIA_ALARM);

        auto stoppedEvent = alarmStoppedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(stoppedEvent.alarm(), alarm.id(), alarm.alarm_type()));
        UNIT_ASSERT_EQUAL(stoppedEvent.stop_media(), false);

        UNIT_ASSERT(playerIsIdle());

        alarmPlayer->stopAnyRemainingMedia();
        contextAlarmStoppedRemainigMediaPromise.get_future().wait();
    }

    Y_UNIT_TEST(testPeriodicCancel_classicAlarm) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["alarmTimerTimeoutSec"] = 3;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::ALARM);

        alarmPlayer->enqueueAlarm(alarm);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::CLASSIC_ALARM);

        YIO_LOG_INFO("Don't cancel alarm manually");

        auto stoppedType = contextAlarmStoppedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(stoppedType, proto::IOEvent::CLASSIC_ALARM);

        auto stoppedEvent = alarmStoppedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(stoppedEvent.alarm(), alarm.id(), alarm.alarm_type()));
        UNIT_ASSERT_EQUAL(stoppedEvent.stop_media(), true); // timeout should always stop media

        UNIT_ASSERT(playerIsIdle());
    }

    Y_UNIT_TEST(testPeriodicCancel_timer) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["alarmTimerTimeoutSec"] = 3;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::TIMER);

        alarmPlayer->enqueueAlarm(alarm);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::TIMER);

        YIO_LOG_INFO("Don't cancel alarm manually");

        auto stoppedType = contextAlarmStoppedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(stoppedType, proto::IOEvent::TIMER);

        auto stoppedEvent = alarmStoppedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(stoppedEvent.alarm(), alarm.id(), alarm.alarm_type()));
        UNIT_ASSERT_EQUAL(stoppedEvent.stop_media(), true); // timeout should always stop media

        UNIT_ASSERT(playerIsIdle());
    }

    Y_UNIT_TEST(testPeriodicCancel_mediaAlarm) {
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["alarmd"]["alarmTimerTimeoutSec"] = 3;
        createAlarmPlayer();

        auto alarm = prepareAlarm(proto::Alarm::MEDIA_ALARM);

        alarmPlayer->enqueueAlarm(alarm);
        confirmAlarmStarted(alarm);

        auto startedType = contextAlarmStartedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(startedType, proto::IOEvent::MEDIA_ALARM);

        YIO_LOG_INFO("Don't cancel alarm manually");

        auto stoppedType = contextAlarmStoppedPromise.get_future().get();
        UNIT_ASSERT_EQUAL(stoppedType, proto::IOEvent::MEDIA_ALARM);

        auto stoppedEvent = alarmStoppedPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(stoppedEvent.alarm(), alarm.id(), alarm.alarm_type()));
        UNIT_ASSERT_EQUAL(stoppedEvent.stop_media(), true); // timeout should always stop media

        UNIT_ASSERT(playerIsIdle());
    }

    Y_UNIT_TEST(testCommandTimer) {
        createAlarmPlayer();
        auto alarm = prepareAlarm(proto::Alarm::COMMAND_TIMER);

        alarmPlayer->enqueueAlarm(alarm);
        commandTimerPromise.get_future().get();
        UNIT_ASSERT(playerIsIdle());

        // command timer should not send any kind of 'alarm started' message
        auto status = contextAlarmEnqueuedPromise.get_future().wait_for(std::chrono::seconds(3));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
        status = alarmFiredPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
        UNIT_ASSERT(playerIsIdle());
    }

    Y_UNIT_TEST(testCommandTimer_whilePlayingAlarm) {
        createAlarmPlayer();
        auto alarm = prepareAlarm(proto::Alarm::ALARM);
        alarmPlayer->enqueueAlarm(alarm);

        alarmFiredPromise.get_future().wait();
        UNIT_ASSERT(!playerIsIdle());

        YIO_LOG_INFO("Enqueue command timer while regular alarm is playing");

        alarm = prepareAlarm(proto::Alarm::COMMAND_TIMER);
        alarmPlayer->enqueueAlarm(alarm);
        commandTimerPromise.get_future().get();
    }

    Y_UNIT_TEST(testPersistence) {
        UNIT_ASSERT(!queuedPlayerPath.Exists());

        createAlarmPlayer();
        auto alarm = prepareAlarm(proto::Alarm::ALARM);
        alarmPlayer->enqueueAlarm(alarm);

        auto alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        alarmPlayer.reset();

        toAlarmd.reset();
        toAlarmd = createIpcConnectorForTests("alarmd");

        alarmFiredPromise = std::promise<proto::Alarm>();
        toAlarmd->setMessageHandler([&](const auto& msg) {
            if (msg->has_alarm_event() && msg->alarm_event().has_alarm_fired()) {
                YIO_LOG_INFO("Alarm fired event received");
                alarmFiredPromise.set_value(msg->alarm_event().alarm_fired());
            }
        });

        toAlarmd->connectToService();
        toAlarmd->waitUntilConnected();

        createAlarmPlayer();

        alarmFromEvent = alarmFiredPromise.get_future().get();
        UNIT_ASSERT(confirmAlarmWithValues(alarmFromEvent, alarm.id(), alarm.alarm_type()));

        UNIT_ASSERT(confirmAlarmWithValues(alarmPlayer->getPlayingAlarm(), alarm.id(), alarm.alarm_type()));
    }
}
