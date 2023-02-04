#include <yandex_io/services/alarmd/alarm_endpoint.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/protobuf_utils/debug.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <util/folder/path.h>

#include <atomic>
#include <future>
#include <iostream>
#include <memory>
#include <thread>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace std::literals;

namespace {

    class AlarmdFixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            quasarDir = JoinFsPaths(tryGetRamDrivePath(), "quasar-" + quasar::makeUUID());
            alarmStoragePath = JoinFsPaths(quasarDir, "alarms.dat");
            alarmsQueuedPlayerPath = JoinFsPaths(quasarDir, "queued_player.dat");
            callbackAlarmPlayerPath = JoinFsPaths(quasarDir, "cb_player.dat");
            alarmsSettingsPath = JoinFsPaths(quasarDir, "alarms_settings_file.dat");
            mediaAlarmsSettingsPath = JoinFsPaths(quasarDir, "sound_alarm_setting.dat");

            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            removeStorage();
            quasarDir.MkDirs();

            toAlarmd = createIpcConnectorForTests("alarmd");

            config["alarmd"]["dbFileName"] = alarmStoragePath;
            config["alarmd"]["alarmPlayerFile"] = callbackAlarmPlayerPath;
            config["alarmd"]["alarmsSettingsFile"] = alarmsSettingsPath;
            config["alarmd"]["mediaAlarmSettingJsonFile"] = mediaAlarmsSettingsPath;
            config["alarmd"]["needAlarmApproval"] = true;
            /* prevent reparsing by seting up long timeout */
            config["alarmd"]["startIcalendarParseIntervalMs"] = 100000 /* 100 sec */;
            config["alarmd"]["finishAlarmUserVolume"] = 7;

            mockMediad = createIpcServerForTests("mediad");
            mockAliced = createIpcServerForTests("aliced");
            mockIOHub = createIpcServerForTests("iohub_services");

            startMockIpcServers({"metricad", "firstrund", "syncd", "updatesd"});

            mockAuthProvider = std::make_shared<mock::AuthProvider>();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            destroyEndpoint();
            removeStorage();

            Base::TearDown(context);
        }

        void mockServersStartListen() {
            mockMediad->listenService();
            mockAliced->listenService();
            mockIOHub->listenService();
        }

        void createEndpoint() {
            alarmEndpoint = std::make_unique<AlarmEndpoint>(getDeviceForTests(),
                                                            ipcFactoryForTests(), mockAuthProvider, nullptr);

            /* Connect to endpoint */
            if (!toAlarmd) {
                toAlarmd = createIpcConnectorForTests("alarmd");
            }
            toAlarmd->connectToService();
            toAlarmd->waitUntilConnected();
        }

        void destroyEndpoint() {
            toAlarmd.reset();
            alarmEndpoint.reset();
        }

        void removeStorage() {
            quasarDir.ForceDelete();
        }

        static Json::Value getMusicMediaAlarmPayload() {
            Json::Value payload;
            payload["server_action"]["data"]["from_alarm"] = true;
            payload["server_action"]["name"] = "bass_action";
            payload["server_action"]["type"] = "server_action";

            payload["sound_alarm_setting"]["type"] = "music";
            payload["sound_alarm_setting"]["info"]["artists"] = "JD";
            payload["sound_alarm_setting"]["info"]["album"] = "Scrubs";
            return payload;
        }

        static Json::Value getRadioMediaAlarmPayload() {
            Json::Value payload;
            payload["server_action"]["data"]["from_alarm"] = true;
            payload["server_action"]["name"] = "bass_action";
            payload["server_action"]["type"] = "server_action";

            payload["sound_alarm_setting"]["type"] = "radio";
            payload["sound_alarm_setting"]["info"]["radioId"] = "HIMYM";
            payload["sound_alarm_setting"]["info"]["radioTitle"] = "Hey Beautiful";
            return payload;
        }

        void setMusicMediaAlarm() {
            proto::QuasarMessage msg;
            msg.mutable_alarm_set_sound()->set_payload(jsonToString(getMusicMediaAlarmPayload()));
            toAlarmd->sendMessage(std::move(msg));
        }

        void setRadioMediaAlarm() {
            proto::QuasarMessage msg;
            msg.mutable_alarm_set_sound()->set_payload(jsonToString(getRadioMediaAlarmPayload()));
            toAlarmd->sendMessage(std::move(msg));
        }

        void resetMediaAlarm() {
            proto::QuasarMessage msg;
            msg.mutable_alarm_reset_sound();
            toAlarmd->sendMessage(std::move(msg));
        }

        static proto::Alarm prepareAlarmIn(std::chrono::milliseconds ms, proto::Alarm_AlarmType type = proto::Alarm::ALARM) {
            /* Set up Alarm*/
            proto::Alarm alarm;
            alarm.set_alarm_type(type);
            alarm.set_id(makeUUID());
            const auto alarmStartTpSec = std::chrono::system_clock::now().time_since_epoch() + ms;
            const auto alarmStartTpMs = std::chrono::duration_cast<std::chrono::milliseconds>(alarmStartTpSec).count();
            alarm.set_start_timestamp_ms(alarmStartTpMs);

            return alarm;
        }

        std::string sendAlarmToAlarmd(proto::Alarm alarm) {
            proto::QuasarMessage request;
            proto::AlarmMessage* alarm_message = request.mutable_alarm_message();
            *(alarm_message->mutable_add_alarm()) = alarm;

            UNIT_ASSERT(toAlarmd->sendMessage(std::move(request)));
            return alarm.id();
        }

        proto::Alarm addTimerForSeconds(size_t timeSec) {
            proto::QuasarMessage request;
            proto::AlarmMessage* alarm_message = request.mutable_alarm_message();
            proto::Alarm* alarm = alarm_message->mutable_add_alarm();
            alarm->set_alarm_type(proto::Alarm::TIMER);
            alarm->set_id(makeUUID());
            auto timeSinceEpoch = std::chrono::system_clock::now().time_since_epoch();
            auto startTS = std::chrono::duration_cast<std::chrono::milliseconds>(timeSinceEpoch).count();
            alarm->set_start_timestamp_ms(startTS);
            alarm->set_duration_seconds(timeSec);

            UNIT_ASSERT(toAlarmd->sendMessage(std::move(request)));
            return *alarm;
        }

        void deleteTimer(const std::string& uuid) {
            proto::QuasarMessage deleteRequest;
            proto::AlarmMessage* deleteAlarmMessage = deleteRequest.mutable_alarm_message();
            deleteAlarmMessage->set_remove_alarm_id(TString(uuid));

            UNIT_ASSERT(toAlarmd->sendMessage(std::move(deleteRequest)));
        }

        void sendStopAlarm() {
            proto::QuasarMessage msg;
            msg.mutable_alarm_message()->mutable_stop_alarm();
            UNIT_ASSERT(toAlarmd->sendMessage(std::move(msg)));
        }

    protected:
        YandexIO::Configuration::TestGuard testGuard;

        TFsPath quasarDir;
        std::string alarmStoragePath;
        std::string alarmsQueuedPlayerPath;
        std::string callbackAlarmPlayerPath;
        std::string alarmsSettingsPath;
        std::string mediaAlarmsSettingsPath;

        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        std::shared_ptr<ipc::IServer> mockAliced;
        std::shared_ptr<ipc::IServer> mockMediad;
        std::shared_ptr<ipc::IServer> mockIOHub;

        std::shared_ptr<ipc::IConnector> toAlarmd;

        std::unique_ptr<AlarmEndpoint> alarmEndpoint;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(AlarmEndpointTest, AlarmdFixture) {
    Y_UNIT_TEST(testAddAlarm) {
        mockServersStartListen();
        createEndpoint();
        UNIT_ASSERT(alarmEndpoint->getEventStorage().getEventCount() == 0);

        auto checkAlarmsEqual = [](const proto::Alarm& first, const proto::Alarm& second) {
            UNIT_ASSERT(first.id() == second.id());
            UNIT_ASSERT(first.alarm_type() == second.alarm_type());
            UNIT_ASSERT(first.start_timestamp_ms() == second.start_timestamp_ms());
            UNIT_ASSERT(first.duration_seconds() == second.duration_seconds());
        };

        auto addAlarmReceived = std::make_shared<std::promise<void>>();
        alarmEndpoint->onQuasarMessageReceivedCallback = [&addAlarmReceived](const proto::QuasarMessage& message) {
            if (message.has_alarm_message() && message.alarm_message().has_add_alarm()) {
                YIO_LOG_INFO("Alarm was added");
                addAlarmReceived->set_value();
            }
        };

        { // add timer to storage
            auto timer = addTimerForSeconds(120);
            addAlarmReceived->get_future().get();

            // Ensure event is added
            proto::Alarm event;
            alarmEndpoint->getEventStorage().tryGetEventById(timer.id(), event);
            checkAlarmsEqual(event, timer);
            UNIT_ASSERT_VALUES_EQUAL(alarmEndpoint->getEventStorage().getEventCount(), 1);
        }
        addAlarmReceived = std::make_shared<std::promise<void>>();

        { // add second one
            auto timer = addTimerForSeconds(200);
            addAlarmReceived->get_future().get();

            // Ensure event is added
            proto::Alarm event;
            alarmEndpoint->getEventStorage().tryGetEventById(timer.id(), event);
            checkAlarmsEqual(event, timer);
            UNIT_ASSERT_VALUES_EQUAL(alarmEndpoint->getEventStorage().getEventCount(), 2);
        }
    }

    Y_UNIT_TEST(testAlarmDelete) {
        createEndpoint();
        UNIT_ASSERT(alarmEndpoint->getEventStorage().getEventCount() == 0);

        std::promise<void> addAlarmReceived;
        std::promise<void> deleteAlarmReceived;
        alarmEndpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
            if (message.has_alarm_message() && message.alarm_message().has_add_alarm()) {
                YIO_LOG_INFO("Alarm was added");
                addAlarmReceived.set_value();
            }
            if (message.has_alarm_message() && message.alarm_message().has_remove_alarm_id()) {
                YIO_LOG_INFO("Alarm was deleted");
                deleteAlarmReceived.set_value();
            }
        };

        // add timer to storage
        auto timer = addTimerForSeconds(120);
        addAlarmReceived.get_future().get();
        proto::Alarm event;
        UNIT_ASSERT(alarmEndpoint->getEventStorage().tryGetEventById(timer.id(), event));
        UNIT_ASSERT_VALUES_EQUAL(alarmEndpoint->getEventStorage().getEventCount(), 1);

        // delete timer
        deleteTimer(timer.id());
        deleteAlarmReceived.get_future().get();

        UNIT_ASSERT(!alarmEndpoint->getEventStorage().tryGetEventById(timer.id(), event));
        UNIT_ASSERT_VALUES_EQUAL(alarmEndpoint->getEventStorage().getEventCount(), 0);
    }

    Y_UNIT_TEST(testAlarmSettings) {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["alarmd"]["finishAlarmUserVolume"] = 7;

        createEndpoint();
        std::promise<void> alarmSettingsReceived;
        alarmEndpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
            if (message.has_alarms_settings() && message.alarms_settings().has_max_volume_level()) {
                YIO_LOG_INFO("Alarm settings were set");
                alarmSettingsReceived.set_value();
            }
        };

        // Check volume is equal to the config value
        UNIT_ASSERT_EQUAL(alarmEndpoint->getAlarmsSettings().max_volume_level(), 7);

        proto::QuasarMessage request;
        request.mutable_alarms_settings()->set_max_volume_level(5);

        UNIT_ASSERT(toAlarmd->sendMessage(std::move(request)));

        alarmSettingsReceived.get_future().get();

        // Ensure setting are applied
        UNIT_ASSERT_EQUAL(alarmEndpoint->getAlarmsSettings().max_volume_level(), 5);

        // check settings were saved
        UNIT_ASSERT(fileExists(alarmsSettingsPath));

        proto::AlarmsSettings settingsFromFile;
        Y_PROTOBUF_SUPPRESS_NODISCARD settingsFromFile.ParseFromString(getFileContent(alarmsSettingsPath));

        UNIT_ASSERT_EQUAL(alarmEndpoint->getAlarmsSettings().max_volume_level(), settingsFromFile.max_volume_level());

        // destroy current endpoint and create new one
        destroyEndpoint();
        createEndpoint();

        // Ensure setting were loaded from file
        UNIT_ASSERT_EQUAL(alarmEndpoint->getAlarmsSettings().max_volume_level(), 5);
    }

    Y_UNIT_TEST(testAlarmClearAllWhenAccountIsSwitched) {
        std::promise<void> alarmsClearedReceived;
        std::atomic_bool timerWasSetup{false};
        toAlarmd->setMessageHandler([&](const auto& msg) mutable {
            YIO_LOG_INFO("Alarmd message: " << shortUtf8DebugString(*msg))
            if (msg->has_alarms_state()) {
                if (msg->timers_state().timers_size() > 0) {
                    // after that we can wait for timers to be cleared
                    YIO_LOG_INFO("Timers are setup");
                    timerWasSetup = true;
                }
                if (msg->timers_state().timers_size() == 0 && msg->alarms_state().icalendar_state().empty()) {
                    if (timerWasSetup) {
                        // alarms are cleared by account changed
                        YIO_LOG_INFO("Alarm state is empty");
                        alarmsClearedReceived.set_value();
                    } else {
                        YIO_LOG_INFO("Alarm state but timer wasn't set up");
                    }
                } else {
                    YIO_LOG_INFO("Not empty state: timers: " << msg->timers_state().timers_size() << ", alarms: " << msg->alarms_state().icalendar_state().size());
                }
            }
        });

        createEndpoint();
        UNIT_ASSERT(alarmEndpoint->getEventStorage().getEventCount() == 0);

        YIO_LOG_INFO("Sent startup info ");
        mockAuthProvider->setOwner(
            AuthInfo2{
                .source = AuthInfo2::Source::AUTHD,
                .authToken = "token1",
                .passportUid = "uid1",
                .tag = 1600000000,
            });

        std::promise<void> addAlarmReceived;
        alarmEndpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
            if (message.has_alarm_message() && message.alarm_message().has_add_alarm()) {
                YIO_LOG_INFO("Alarm was added");
                addAlarmReceived.set_value();
            }
        };

        // add timer to storage
        auto timer = addTimerForSeconds(12000);
        YIO_LOG_DEBUG("Wait alarm added");
        addAlarmReceived.get_future().get();
        YIO_LOG_DEBUG("Alarm added");
        TestUtils::waitUntil([this]() {
            return alarmEndpoint->getEventStorage().getEventCount() == 1;
        });

        YIO_LOG_INFO("Change account");
        mockAuthProvider->setOwner(
            AuthInfo2{
                .source = AuthInfo2::Source::AUTHD,
                .authToken = "token",
                .passportUid = "uid2",
                .tag = 1600000001,
            });

        alarmsClearedReceived.get_future().get();
        YIO_LOG_INFO("Alarms cleared");
        TestUtils::waitUntil([this]() {
            YIO_LOG_INFO("Storage size: " << alarmEndpoint->getEventStorage().getEventCount());
            return alarmEndpoint->getEventStorage().getEventCount() == 0;
        });
    }

    Y_UNIT_TEST(testAlarmCommandTimerDontSendFire) {
        std::promise<void> commandTimerFired;
        toAlarmd->setMessageHandler([&](const auto& msg) {
            if (msg->has_alarm_event() && (msg->alarm_event().has_alarm_fired())) {
                YIO_LOG_INFO("Command timer sent alarm message for some reason");
                commandTimerFired.set_value();
            }
        });

        createEndpoint();

        std::promise<void> addAlarmReceived;
        alarmEndpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
            if (message.has_alarm_message() && message.alarm_message().has_add_alarm()) {
                YIO_LOG_INFO("Alarm was added");
                addAlarmReceived.set_value();
            }
        };

        {
            proto::QuasarMessage commandTimerMessage;
            commandTimerMessage.mutable_alarm_message()->mutable_add_alarm()->set_id(makeUUID());
            commandTimerMessage.mutable_alarm_message()->mutable_add_alarm()->set_alarm_type(proto::Alarm::COMMAND_TIMER);
            commandTimerMessage.mutable_alarm_message()->mutable_add_alarm()->mutable_command_list()->Add();
            const auto duration = std::chrono::system_clock::now().time_since_epoch();
            const auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
            commandTimerMessage.mutable_alarm_message()->mutable_add_alarm()->set_start_timestamp_ms(millis);
            commandTimerMessage.mutable_alarm_message()->mutable_add_alarm()->set_duration_seconds(1);
            UNIT_ASSERT(toAlarmd->sendMessage(std::move(commandTimerMessage)));
        }

        addAlarmReceived.get_future().get();
        TestUtils::waitUntil([this]() {
            return alarmEndpoint->getEventStorage().getEventCount() == 1;
        });

        // command timer is not expected to send any alarm message
        const auto status = commandTimerFired.get_future().wait_for(std::chrono::seconds(3));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);

        // ensure command timer was actually fired
        TestUtils::waitUntil([this]() {
            return alarmEndpoint->getEventStorage().getEventCount() == 0;
        });
    }

    Y_UNIT_TEST(testAlarmPlayerPersistence) {
        std::string alarmId;
        {
            std::promise<std::string> alarmFiredMessagePromise;
            toAlarmd->setMessageHandler([&](const auto& msg) {
                if (msg->has_alarm_event() && msg->alarm_event().has_alarm_fired()) {
                    YIO_LOG_INFO("Alarm fired");
                    alarmFiredMessagePromise.set_value(msg->alarm_event().alarm_fired().id());
                }
            });
            createEndpoint();
            alarmId = sendAlarmToAlarmd(prepareAlarmIn(300ms));

            std::string firedId = alarmFiredMessagePromise.get_future().get();
            UNIT_ASSERT_VALUES_EQUAL(firedId, alarmId);
        }
        YIO_LOG_INFO("Destroy endpoint and create new one");
        destroyEndpoint();

        YIO_LOG_INFO("Destroy endpoint and create new one");

        std::promise<std::string> alarmFiredMessagePromise;
        toAlarmd = createIpcConnectorForTests("alarmd");
        toAlarmd->setMessageHandler([&](const auto& msg) {
            if (msg->has_alarm_event() && msg->alarm_event().has_alarm_fired()) {
                YIO_LOG_INFO("Alarm fired after restart");
                alarmFiredMessagePromise.set_value(msg->alarm_event().alarm_fired().id());
            }
        });

        YIO_LOG_INFO("Recreate endpoint, alarm player should now fire alarm from persistent file");
        createEndpoint();
        std::string firedId = alarmFiredMessagePromise.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(firedId, alarmId);
    }
}

namespace {

    class AlarmdSoundSettingsTestFixture: public AlarmdFixture {
    public:
        void setMessageHandlers() {
            soundSettingsSetPromise = std::promise<std::string>();
            soundSettingsRemovedPromise = std::promise<void>();
            alarmFiredPromise = std::promise<proto::Alarm>();
            wereSettingsSet = false;

            toAlarmd->setMessageHandler([&](const auto& msg) {
                if (msg->has_alarm_event() && msg->alarm_event().has_alarm_fired()) {
                    const auto alarm = msg->alarm_event().alarm_fired();
                    YIO_LOG_INFO("Alarm fired. id " << alarm.id() << " type " << (int)alarm.alarm_type());
                    alarmFiredPromise.set_value(alarm);
                }
                if (msg->has_alarms_state()) {
                    if (msg->alarms_state().has_media_alarm_setting()) {
                        if (!wereSettingsSet) {
                            YIO_LOG_INFO("Media alarm Setting was set up");
                            soundSettingsSetPromise.set_value(msg->alarms_state().media_alarm_setting());
                            wereSettingsSet = true;
                        }
                    } else {
                        if (wereSettingsSet) {
                            YIO_LOG_INFO("Media alarm Setting was removed!");
                            soundSettingsRemovedPromise.set_value();
                        }
                    }
                }
            });
        }

    protected:
        std::promise<std::string> soundSettingsSetPromise;
        std::promise<void> soundSettingsRemovedPromise;
        std::promise<proto::Alarm> alarmFiredPromise;
        std::atomic_bool wereSettingsSet;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(MediaAlarmSettingsTest, AlarmdSoundSettingsTestFixture) {
    Y_UNIT_TEST(testSoundSettingsSetResetMusic) {
        mockServersStartListen();
        setMessageHandlers();
        createEndpoint();
        setMusicMediaAlarm();

        const auto settings = parseJson(soundSettingsSetPromise.get_future().get());
        UNIT_ASSERT_EQUAL(settings, getMusicMediaAlarmPayload());
        UNIT_ASSERT(fileExists(mediaAlarmsSettingsPath));

        resetMediaAlarm();

        soundSettingsRemovedPromise.get_future().wait();
        UNIT_ASSERT(!fileExists(mediaAlarmsSettingsPath));
    }

    Y_UNIT_TEST(testSoundSettingsSetResetRadio) {
        mockServersStartListen();
        setMessageHandlers();
        createEndpoint();
        setRadioMediaAlarm();

        const auto settings = parseJson(soundSettingsSetPromise.get_future().get());
        UNIT_ASSERT_EQUAL(settings, getRadioMediaAlarmPayload());
        UNIT_ASSERT(fileExists(mediaAlarmsSettingsPath));

        resetMediaAlarm();

        soundSettingsRemovedPromise.get_future().wait();
        UNIT_ASSERT(!fileExists(mediaAlarmsSettingsPath));
    }

    Y_UNIT_TEST(testSoundSettingsSetFromFS) {
        mockServersStartListen();
        setMessageHandlers();
        createEndpoint();
        setMusicMediaAlarm();

        auto settings = parseJson(soundSettingsSetPromise.get_future().get());
        UNIT_ASSERT_EQUAL(settings, getMusicMediaAlarmPayload());
        UNIT_ASSERT(fileExists(mediaAlarmsSettingsPath));

        // destroy current endpoint and create new one
        destroyEndpoint();

        toAlarmd = createIpcConnectorForTests("alarmd");

        setMessageHandlers();
        createEndpoint();

        // sound settings should be loaded from file
        settings = parseJson(soundSettingsSetPromise.get_future().get());
        UNIT_ASSERT_EQUAL(settings, getMusicMediaAlarmPayload());
        UNIT_ASSERT(fileExists(mediaAlarmsSettingsPath));
    }

    Y_UNIT_TEST(testEndpoint_enqueueMediaAlarm) {
        mockServersStartListen();
        setMessageHandlers();
        createEndpoint();
        setMusicMediaAlarm();

        auto settings = parseJson(soundSettingsSetPromise.get_future().get());
        UNIT_ASSERT_EQUAL(settings, getMusicMediaAlarmPayload());

        const std::string mediaAlarmId = sendAlarmToAlarmd(prepareAlarmIn(300ms));
        const auto firedMediaAlarm = alarmFiredPromise.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(firedMediaAlarm.id(), mediaAlarmId);
        UNIT_ASSERT(firedMediaAlarm.alarm_type() == proto::Alarm::MEDIA_ALARM);

        sendStopAlarm();

        resetMediaAlarm();
        soundSettingsRemovedPromise.get_future().wait();

        toAlarmd.reset();
        toAlarmd = createIpcConnectorForTests("alarmd");

        alarmFiredPromise = std::promise<proto::Alarm>();
        toAlarmd->setMessageHandler([&](const auto& msg) {
            if (msg->has_alarm_event() && msg->alarm_event().has_alarm_fired()) {
                const auto alarm = msg->alarm_event().alarm_fired();
                YIO_LOG_INFO("Alarm fired. id " << alarm.id() << " type " << (int)alarm.alarm_type());
                alarmFiredPromise.set_value(alarm);
            }
        });

        toAlarmd->connectToService();
        toAlarmd->waitUntilConnected();

        const std::string alarmId = sendAlarmToAlarmd(prepareAlarmIn(300ms));
        const auto firedAlarm = alarmFiredPromise.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(firedAlarm.id(), alarmId);
        UNIT_ASSERT(firedAlarm.alarm_type() == proto::Alarm::ALARM);
    }
}
