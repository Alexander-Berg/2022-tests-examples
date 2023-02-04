#include <yandex_io/services/alarmd/alarm_endpoint.h>
#include <yandex_io/services/alarmd/event_storage.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <boost/algorithm/string.hpp>

#include <util/folder/path.h>

#include <cstdio>
#include <fstream>
#include <iomanip>
#include <iostream>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            YandexIO::Configuration::TestGuard testGuard;

            auto basePath = tryGetRamDrivePath();
            quasarDirPath = JoinFsPaths(basePath, "quasarDir-" + makeUUID());
            storagePath = JoinFsPaths(quasarDirPath, "queuedPlayer.dat");

            // Check configuration
            const auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            serviceConfig = config["alarmd"];
            serviceConfig["dbFileName"] = storagePath.GetPath();
            filename = serviceConfig["dbFileName"].asString();

            // Create target directories
            quasarDirPath.MkDirs();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            // Delete storage file if exists
            quasarDirPath.ForceDelete();
            Base::TearDown(context);
        }

        static proto::MediaRequest prepareMediaRequest(const std::string& alarmId, const std::string& requestId)
        {
            proto::MediaRequest mediaRequest;
            mediaRequest.mutable_play_audio();
            mediaRequest.set_uid(TString(requestId));
            mediaRequest.set_alarm_id(TString(alarmId));
            return mediaRequest;
        }

    protected:
        TFsPath quasarDirPath;
        TFsPath storagePath;

        EventStorage storage;
        Json::Value serviceConfig;
        std::string filename;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(TestEventStorage, Fixture) {
    Y_UNIT_TEST(testEventStorageWrite)
    {
        EventStorage storage;
        storage.saveEvents(serviceConfig["dbFileName"].asString());

        // Ensure storage file exists

        {
            UNIT_ASSERT(fileExists(filename));
        }
    }

    Y_UNIT_TEST(testEventStorageSaveAndLoadAlarm)
    {
        // Get random UUIDs

        std::string uuid = makeUUID();
        std::string uuid2 = makeUUID();

        // Create Alarm

        proto::Alarm alarm;
        alarm.set_alarm_type(proto::Alarm::TIMER);
        alarm.set_id(TString(uuid));
        alarm.set_start_timestamp_ms(1536590330000);
        alarm.set_duration_seconds(120);

        // Create command timer
        proto::Alarm alarm2;
        alarm2.set_alarm_type(proto::Alarm::COMMAND_TIMER);
        alarm2.set_id(TString(uuid2));
        alarm2.set_start_timestamp_ms(1536590331000);
        alarm2.set_duration_seconds(200);
        proto::ExternalCommandMessage* command = alarm2.mutable_command_list()->Add();
        command->set_name("Test directive");
        command->set_payload("Some nice payload");

        {
            // Add Alarms to Storage

            EventStorage storage;
            storage.addEvent(uuid, alarm);
            storage.addEvent(uuid2, alarm2);

            // Ensure Storage has 2 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 2);

            // Save Storage

            storage.saveEvents(serviceConfig["dbFileName"].asString());
        }

        // Ensure Storage file exists

        UNIT_ASSERT(fileExists(filename));

        // Load Storage

        EventStorage storage;
        storage.loadEvents(serviceConfig["dbFileName"].asString());
        storage.reParseICalendar();
        // Ensure Storage has 2 records

        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 2);

        // Read Alarm from Storage

        proto::Alarm alarmFromStorage;
        storage.tryGetEventById(uuid, alarmFromStorage);
        proto::Alarm alarm2FromStorage;
        storage.tryGetEventById(uuid2, alarm2FromStorage);

        UNIT_ASSERT_EQUAL(alarm.alarm_type(), alarmFromStorage.alarm_type());
        UNIT_ASSERT_VALUES_EQUAL(alarm.id(), alarmFromStorage.id());
        UNIT_ASSERT_VALUES_EQUAL(alarm.start_timestamp_ms(), alarmFromStorage.start_timestamp_ms());
        UNIT_ASSERT_VALUES_EQUAL(alarm.duration_seconds(), alarmFromStorage.duration_seconds());

        UNIT_ASSERT_EQUAL(alarm2.alarm_type(), alarm2FromStorage.alarm_type());
        UNIT_ASSERT_VALUES_EQUAL(alarm2.id(), alarm2FromStorage.id());
        UNIT_ASSERT_VALUES_EQUAL(alarm2.start_timestamp_ms(), alarm2FromStorage.start_timestamp_ms());
        UNIT_ASSERT_VALUES_EQUAL(alarm2.duration_seconds(), alarm2FromStorage.duration_seconds());
        const auto command2 = alarm2.command_list(0);
        const auto command2FromStorage = alarm2FromStorage.command_list(0);
        UNIT_ASSERT_VALUES_EQUAL(command2.name(), command2FromStorage.name());
        UNIT_ASSERT_VALUES_EQUAL(command2.payload(), command2FromStorage.payload());
    }

    Y_UNIT_TEST(testEventStorageDeleteAlarm)
    {
        // Get random UUIDs

        std::string uuid = makeUUID();
        std::string uuid2 = makeUUID();

        // Create Alarm

        proto::Alarm alarm;
        alarm.set_alarm_type(proto::Alarm::TIMER);
        alarm.set_id(TString(uuid));
        alarm.set_start_timestamp_ms(1536590330000);
        alarm.set_remind_message("It's a testing time");
        alarm.set_duration_seconds(120);

        // Create another Alarm

        proto::Alarm alarm2;
        alarm2.set_alarm_type(proto::Alarm::TIMER);
        alarm2.set_id(TString(uuid2));
        alarm2.set_start_timestamp_ms(1536590331000);
        alarm2.set_remind_message("It's a testing time again");
        alarm2.set_duration_seconds(200);

        {
            // Add Alarms to Storage

            EventStorage storage;
            storage.addEvent(uuid, alarm);
            storage.addEvent(uuid2, alarm2);

            // Ensure Storage has 2 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 2);

            // Save Storage

            storage.saveEvents(serviceConfig["dbFileName"].asString());

            // Delete one Alarm

            storage.deleteEvent(uuid2);

            // Ensure Storage has 1 record

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);

            // Save Storage

            storage.saveEvents(serviceConfig["dbFileName"].asString());
        }

        // Ensure Storage file exists

        UNIT_ASSERT(fileExists(filename));

        // Load Storage

        EventStorage storage;
        storage.loadEvents(serviceConfig["dbFileName"].asString());
        storage.reParseICalendar();
        // Ensure Storage has 1 record

        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);

        // Ensure Alarm 1 is in Storage

        proto::Alarm alarmFromStorage;
        UNIT_ASSERT(storage.tryGetEventById(uuid, alarmFromStorage));

        // Ensure Alarm 2 is not in Storage

        proto::Alarm alarm2FromStorage;
        UNIT_ASSERT(!(storage.tryGetEventById(uuid2, alarm2FromStorage)));

        // Ensure Alarm 1 is good

        UNIT_ASSERT_EQUAL(alarm.alarm_type(), alarmFromStorage.alarm_type());
        UNIT_ASSERT_VALUES_EQUAL(alarm.id(), alarmFromStorage.id());
        UNIT_ASSERT_VALUES_EQUAL(alarm.start_timestamp_ms(), alarmFromStorage.start_timestamp_ms());
        UNIT_ASSERT_VALUES_EQUAL(alarm.remind_message(), alarmFromStorage.remind_message());
        UNIT_ASSERT_VALUES_EQUAL(alarm.duration_seconds(), alarmFromStorage.duration_seconds());
    }

    Y_UNIT_TEST(testEventStorageClear)
    {
        // Get random UUIDs

        std::string uuid = makeUUID();
        std::string uuid2 = makeUUID();
        long curTimePlus2MinutesMs = std::time(nullptr) * 1000ll + (2ll * 60ll * 1000ll);
        long curTimePlus5MinutesMs = std::time(nullptr) * 1000ll + (5ll * 60ll * 1000ll);

        // Create Alarm

        proto::Alarm alarm;
        alarm.set_alarm_type(proto::Alarm::ALARM);
        alarm.set_id(TString(uuid));
        alarm.set_start_timestamp_ms(curTimePlus2MinutesMs);
        alarm.set_remind_message("It's a testing time");
        alarm.set_duration_seconds(120);

        // Create another Alarm

        proto::Alarm alarm2;
        alarm2.set_alarm_type(proto::Alarm::TIMER);
        alarm2.set_id(TString(uuid2));
        alarm2.set_start_timestamp_ms(curTimePlus5MinutesMs);
        alarm2.set_remind_message("It's a testing time again");
        alarm2.set_duration_seconds(200);
        {
            // Add Alarms to Storage

            EventStorage storage;
            storage.addEvent(uuid, alarm);
            storage.addEvent(uuid2, alarm2);

            // Ensure Storage has 2 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 2);

            // Save Storage

            storage.saveEvents(serviceConfig["dbFileName"].asString());

            // Clear Storage

            storage.clear();

            // Ensure Storage has 0 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 0);

            // Save Storage

            storage.saveEvents(serviceConfig["dbFileName"].asString());
        }

        // Ensure Storage file exists

        UNIT_ASSERT(fileExists(filename));

        // Load Storage

        EventStorage storage;
        storage.loadEvents(serviceConfig["dbFileName"].asString());
        storage.reParseICalendar();
        // Ensure Storage has 0 records

        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 0);

        // Ensure we can't read Alarms from Storage

        proto::Alarm alarmFromStorage;
        UNIT_ASSERT(!(storage.tryGetEventById(uuid, alarmFromStorage)));
        proto::Alarm alarm2FromStorage;
        UNIT_ASSERT(!(storage.tryGetEventById(uuid2, alarm2FromStorage)));
    }

    Y_UNIT_TEST(testEventStorageGetCurrentAlarms)
    {
        // Get random UUIDs

        std::string uuid = makeUUID();
        std::string uuid2 = makeUUID();
        std::string uuid3 = makeUUID();
        std::string uuid4 = makeUUID();
        std::string uuid5 = makeUUID();

        // Create Alarm 1

        {
            proto::Alarm alarm;
            alarm.set_alarm_type(proto::Alarm::ALARM);
            alarm.set_id(TString(uuid));
            alarm.set_start_timestamp_ms(1536590330000);
            alarm.set_remind_message("It's a testing time");
            alarm.set_duration_seconds(120);

            // Add Alarm to Storage

            storage.addEvent(uuid, alarm);

            // Ensure Storage has 1 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);
        }

        // Create Alarm 2

        {
            proto::Alarm alarm;
            alarm.set_alarm_type(proto::Alarm::ALARM);
            alarm.set_id(TString(uuid));
            alarm.set_start_timestamp_ms(1536849231000);
            alarm.set_remind_message("It's a testing time");
            alarm.set_duration_seconds(120);

            // Add Alarm to Storage

            storage.addEvent(uuid2, alarm);

            // Ensure Storage has 2 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 2);
        }

        // Create Alarm 3

        {
            proto::Alarm alarm;
            alarm.set_alarm_type(proto::Alarm::ALARM);
            alarm.set_id(TString(uuid));
            alarm.set_start_timestamp_ms(1536849232000);
            alarm.set_remind_message("This one will be started.");
            alarm.set_duration_seconds(120);

            // Add Alarm to Storage

            storage.addEvent(uuid3, alarm);

            // Ensure Storage has 3 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 3);
        }

        // Create Alarm 4
        // This one will not be fired, but will be deleted

        {
            proto::Alarm alarm;
            alarm.set_alarm_type(proto::Alarm::ALARM);
            alarm.set_id(TString(uuid));
            alarm.set_start_timestamp_ms(1536849233000);
            alarm.set_remind_message("Another Alarm to be started.");
            alarm.set_duration_seconds(120);

            // Add Alarm to Storage

            storage.addEvent(uuid4, alarm);

            // Ensure Storage has 4 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 4);
        }

        // Create Alarm 5

        {
            proto::Alarm alarm;
            alarm.set_alarm_type(proto::Alarm::ALARM);
            alarm.set_id(TString(uuid));
            alarm.set_start_timestamp_ms(1536849350000);
            alarm.set_remind_message("Yet another alarm to be started");
            alarm.set_duration_seconds(120);

            // Add Alarm to Storage

            storage.addEvent(uuid5, alarm);

            // Ensure Storage has 5 records

            UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 5);
        }

        auto currentAlarms = storage.fireEvents(std::chrono::seconds(1536849353),
                                                std::chrono::milliseconds(10000));

        UNIT_ASSERT_VALUES_EQUAL(currentAlarms.actualEvents.size(), (size_t)3);
        UNIT_ASSERT_VALUES_EQUAL(currentAlarms.expiredEvents.size(), (size_t)1);
        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);
    }

    Y_UNIT_TEST(testEventStorageReparseIcalendarState)
    {
        std::string state =
            "BEGIN:VCALENDAR\n"
            "VERSION:2.0\n"
            "PRODID:-//Yandex LTD//NONSGML Quasar//EN\n"
            "BEGIN:VEVENT\n"
            "DTSTART:20181117T230000Z\n"
            "DTEND:20181117T230000Z\n"
            "BEGIN:VALARM\n"
            "TRIGGER;VALUE=DATE-TIME:20181117T230000Z\n"
            "ACTION:AUDIO\n"
            "END:VALARM\n"
            "END:VEVENT\n"
            "END:VCALENDAR";

        auto alarmDate = std::chrono::system_clock::now() + std::chrono::hours(1);
        time_t alarmTimestamp = std::chrono::duration_cast<std::chrono::seconds>(alarmDate.time_since_epoch()).count();
        std::tm alarmDateTm;
        gmtime_r(&alarmTimestamp, &alarmDateTm);
        std::stringstream stream;
        stream << std::put_time(&alarmDateTm, "%Y%m%dT%H%M%SZ");
        boost::replace_all(state, "20181117T230000Z", stream.str());

        storage.setICalendarState(state);
        storage.reParseICalendar();
        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);

        auto firedEvents = storage.fireEvents(std::chrono::seconds(alarmTimestamp), std::chrono::milliseconds(1));
        UNIT_ASSERT_VALUES_EQUAL(firedEvents.actualEvents.size(), 1U);

        UNIT_ASSERT_VALUES_EQUAL(firedEvents.actualEvents[0].start_timestamp_ms() + firedEvents.actualEvents[0].duration_seconds() * 1000, alarmTimestamp * 1000);
    }

    Y_UNIT_TEST(testEventStorageRepeatingAlarms)
    {
        std::string state = "BEGIN:VCALENDAR\n"
                            "VERSION:2.0\n"
                            "PRODID:-//Yandex LTD//NONSGML Quasar//EN\n"
                            "BEGIN:VEVENT\n"
                            "DTSTART:20160806T080000Z\n"
                            "DTEND:20160806T080000Z\n"
                            "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA,SU\n"
                            "BEGIN:VALARM\n"
                            "TRIGGER:P0D\n"
                            "ACTION:AUDIO\n"
                            "END:VALARM\n"
                            "END:VEVENT\n"
                            "END:VCALENDAR\n"; // Every day at 08:00 am

        const auto now = std::chrono::system_clock::now();
        const auto alarmDate = getStartOfDayUTC(now) - std::chrono::weeks(100) + std::chrono::hours(8); // 08:00 100 weeks ago
        time_t alarmTimestamp = std::chrono::duration_cast<std::chrono::seconds>(alarmDate.time_since_epoch()).count();
        std::tm alarmDateTm;
        gmtime_r(&alarmTimestamp, &alarmDateTm);
        std::stringstream stream;
        stream << std::put_time(&alarmDateTm, "%Y%m%dT%H%M%SZ");
        boost::replace_all(state, "20160806T080000Z", stream.str());

        storage.setICalendarState(state);
        storage.reParseICalendar();
        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);

        auto fireDate = getStartOfDayUTC(now) + std::chrono::hours(8); // 8 a.m. today
        if (now > fireDate) {
            fireDate += std::chrono::days(1); // If today 8 a.m. is over use tomorrow's 8 a.m.
        }
        const int64_t fireTimestamp = std::chrono::duration_cast<std::chrono::seconds>(fireDate.time_since_epoch()).count();
        auto firedEvents = storage.fireEvents(std::chrono::seconds(fireTimestamp), std::chrono::milliseconds(500));

        UNIT_ASSERT_VALUES_EQUAL(firedEvents.actualEvents.size(), 1U);
        UNIT_ASSERT_VALUES_EQUAL(firedEvents.actualEvents[0].start_timestamp_ms() + firedEvents.actualEvents[0].duration_seconds() * 1000, fireTimestamp * 1000);
    }

    Y_UNIT_TEST(testEventStoragePauseResumeAlarm)
    {
        std::string uuid = makeUUID();

        proto::Alarm alarm;
        alarm.set_alarm_type(proto::Alarm::ALARM);
        alarm.set_id(TString(uuid));
        alarm.set_start_timestamp_ms(1536590330000);
        alarm.set_remind_message("It's a testing time");
        alarm.set_duration_seconds(120);
        alarm.set_delay_seconds(0);

        storage.addEvent(uuid, alarm);

        UNIT_ASSERT(!storage.resumeTimerEvent(uuid));

        {
            proto::Alarm resumedAlarm;
            UNIT_ASSERT(storage.tryGetEventById(uuid, resumedAlarm));
            // The alarm isn't paused now, so it should be intact after resumeTimerEvent.
            UNIT_ASSERT_VALUES_EQUAL(alarm.DebugString(), resumedAlarm.DebugString());
            UNIT_ASSERT(!resumedAlarm.has_pause_timestamp_sec());
            UNIT_ASSERT(!resumedAlarm.has_paused_seconds());
        }

        UNIT_ASSERT(storage.pauseTimerEvent(uuid));

        {
            proto::Alarm pausedAlarm;
            UNIT_ASSERT(storage.tryGetEventById(uuid, pausedAlarm));
            UNIT_ASSERT(pausedAlarm.has_pause_timestamp_sec());
        }

        UNIT_ASSERT(storage.resumeTimerEvent(uuid));

        {
            proto::Alarm resumedAlarm;
            UNIT_ASSERT(storage.tryGetEventById(uuid, resumedAlarm));
            // The alarm was paused at the moment of the last resumeTimerEvent, so we should have paused_seconds field now.
            UNIT_ASSERT(!resumedAlarm.has_pause_timestamp_sec());
            UNIT_ASSERT(resumedAlarm.has_paused_seconds());
        }
    }

    Y_UNIT_TEST(testDontPauseAlreadyPausedAlarm)
    {
        std::string uuid = makeUUID();

        proto::Alarm alarm;
        alarm.set_alarm_type(proto::Alarm::ALARM);
        alarm.set_id(TString(uuid));
        alarm.set_start_timestamp_ms(1536590330000);
        alarm.set_remind_message("It's a testing time");
        alarm.set_duration_seconds(120);

        storage.addEvent(uuid, alarm);
        UNIT_ASSERT(storage.pauseTimerEvent(uuid));

        std::uint32_t pause_timestamp{};

        {
            proto::Alarm pausedAlarm;
            UNIT_ASSERT(storage.tryGetEventById(uuid, pausedAlarm));
            UNIT_ASSERT(pausedAlarm.has_pause_timestamp_sec());

            pause_timestamp = pausedAlarm.pause_timestamp_sec();
        }

        std::this_thread::sleep_for(std::chrono::seconds{1});

        UNIT_ASSERT(!storage.pauseTimerEvent(uuid));

        {
            proto::Alarm pausedAlarm;
            UNIT_ASSERT(storage.tryGetEventById(uuid, pausedAlarm));
            UNIT_ASSERT(pausedAlarm.has_pause_timestamp_sec());

            UNIT_ASSERT_VALUES_EQUAL(pause_timestamp, pausedAlarm.pause_timestamp_sec());
        }
    }
    Y_UNIT_TEST(testAlarmsDelay_forAlarmsFromIcalendar)
    {
        std::string state =
            "BEGIN:VCALENDAR\n"
            "VERSION:2.0\n"
            "PRODID:-//Yandex LTD//NONSGML Quasar//EN\n"
            "BEGIN:VEVENT\n"
            "DTSTART:20181117T230000Z\n"
            "DTEND:20181117T230000Z\n"
            "BEGIN:VALARM\n"
            "TRIGGER;VALUE=DATE-TIME:20181117T230000Z\n"
            "ACTION:AUDIO\n"
            "END:VALARM\n"
            "END:VEVENT\n"
            "END:VCALENDAR";

        auto alarmDate = std::chrono::system_clock::now() + std::chrono::hours(1);
        time_t alarmTimestamp = std::chrono::duration_cast<std::chrono::seconds>(alarmDate.time_since_epoch()).count();
        std::tm alarmDateTm;
        gmtime_r(&alarmTimestamp, &alarmDateTm);
        std::stringstream stream;
        stream << std::put_time(&alarmDateTm, "%Y%m%dT%H%M%SZ");
        boost::replace_all(state, "20181117T230000Z", stream.str());

        storage.setICalendarState(state);
        storage.reParseICalendar();
        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);

        auto firedEvents = storage.fireEvents(std::chrono::seconds(alarmTimestamp), std::chrono::milliseconds(1));
        UNIT_ASSERT_VALUES_EQUAL(firedEvents.actualEvents.size(), 1U);
        for (const auto& alarm : firedEvents.actualEvents) {
            UNIT_ASSERT_VALUES_EQUAL(alarm.delay_seconds(), 0);
        }

        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 0);
        storage.setAlarmDelay(5);

        storage.setICalendarState(state);
        storage.reParseICalendar();
        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 1);

        // now alarm should be fired 5 seconds later
        firedEvents = storage.fireEvents(std::chrono::seconds(alarmTimestamp + 5), std::chrono::milliseconds(1));
        UNIT_ASSERT_VALUES_EQUAL(firedEvents.actualEvents.size(), 1U);
        for (const auto& alarm : firedEvents.actualEvents) {
            UNIT_ASSERT_VALUES_EQUAL(alarm.delay_seconds(), 5);
        }
    }

    Y_UNIT_TEST(testAlarmsDelay_forAlarmsInStorage)
    {
        std::string alarmUuid = makeUUID();
        proto::Alarm alarm;
        alarm.set_alarm_type(proto::Alarm::ALARM);
        alarm.set_id(TString(alarmUuid));
        alarm.set_start_timestamp_ms(1536590110000);
        alarm.set_duration_seconds(110);

        std::string mediaAlarmUuid = makeUUID();
        proto::Alarm mediaAlarm;
        mediaAlarm.set_alarm_type(proto::Alarm::MEDIA_ALARM);
        mediaAlarm.set_id(TString(mediaAlarmUuid));
        mediaAlarm.set_start_timestamp_ms(1536590220000);
        mediaAlarm.set_duration_seconds(120);

        std::string timerUuid = makeUUID();
        proto::Alarm timer;
        timer.set_alarm_type(proto::Alarm::TIMER);
        timer.set_id(TString(timerUuid));
        timer.set_start_timestamp_ms(1536590330000);
        timer.set_duration_seconds(130);

        storage.addEvent(alarmUuid, alarm);
        storage.addEvent(mediaAlarmUuid, mediaAlarm);
        storage.addEvent(timerUuid, timer);

        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 3);

        proto::Alarm alarmFromStorage;
        proto::Alarm mediaAlarmFromStorage;
        proto::Alarm timerFromStorage;

        storage.tryGetEventById(alarmUuid, alarmFromStorage);
        storage.tryGetEventById(mediaAlarmUuid, mediaAlarmFromStorage);
        storage.tryGetEventById(timerUuid, timerFromStorage);

        UNIT_ASSERT_VALUES_EQUAL(alarmFromStorage.delay_seconds(), 0);
        UNIT_ASSERT_VALUES_EQUAL(mediaAlarmFromStorage.delay_seconds(), 0);
        UNIT_ASSERT_VALUES_EQUAL(timerFromStorage.delay_seconds(), 0);

        storage.setAlarmDelay(5);

        storage.tryGetEventById(alarmUuid, alarmFromStorage);
        storage.tryGetEventById(mediaAlarmUuid, mediaAlarmFromStorage);
        storage.tryGetEventById(timerUuid, timerFromStorage);

        UNIT_ASSERT_VALUES_EQUAL(alarmFromStorage.delay_seconds(), 5);
        UNIT_ASSERT_VALUES_EQUAL(mediaAlarmFromStorage.delay_seconds(), 5);
        // TIMERs should not be delayed
        UNIT_ASSERT_VALUES_EQUAL(timerFromStorage.delay_seconds(), 0);

        std::string anotherAlarmUuid = makeUUID();
        proto::Alarm anotherAlarm;
        anotherAlarm.set_alarm_type(proto::Alarm::ALARM);
        anotherAlarm.set_id(TString(anotherAlarmUuid));
        anotherAlarm.set_start_timestamp_ms(1536590000000);
        anotherAlarm.set_duration_seconds(100);

        storage.addEvent(anotherAlarmUuid, anotherAlarm);
        UNIT_ASSERT_VALUES_EQUAL(storage.getEventCount(), 4);
        proto::Alarm anotherAlarmFromStorage;
        storage.tryGetEventById(anotherAlarmUuid, anotherAlarmFromStorage);
        UNIT_ASSERT_VALUES_EQUAL(anotherAlarmFromStorage.delay_seconds(), 5);
    }

    Y_UNIT_TEST(testReparseCalendarBetweenFireAndDelay) {
        storage.setAlarmDelay(1000);

        proto::Alarm alarm;
        std::string alarmId = makeUUID();
        alarm.set_alarm_type(proto::Alarm::ALARM);
        alarm.set_id(makeUUID());
        alarm.set_start_timestamp_ms(0);

        constexpr long int curTime = 3;

        storage.addEvent(alarmId, alarm);
        auto fireEvents = storage.fireEvents(std::chrono::seconds(curTime), std::chrono::milliseconds(0));
        UNIT_ASSERT(!storage.isEmpty());
        UNIT_ASSERT(fireEvents.actualEvents.empty());
        UNIT_ASSERT(fireEvents.expiredEvents.empty());

        std::string state =
            "BEGIN:VCALENDAR\n"
            "VERSION:2.0\n"
            "PRODID:-//Yandex LTD//NONSGML Quasar//EN\n"
            "BEGIN:VEVENT\n"
            "DTSTART:19700101T000000Z\n"
            "DTEND:20181117T230000Z\n"
            "BEGIN:VALARM\n"
            "TRIGGER;VALUE=DATE-TIME:19700101T000000Z\n"
            "ACTION:AUDIO\n"
            "END:VALARM\n"
            "END:VEVENT\n"
            "END:VCALENDAR";

        storage.setICalendarState(state);

        storage.reParseICalendar(curTime);

        UNIT_ASSERT(!storage.isEmpty());
    }

    Y_UNIT_TEST(testReparseCalendarBetweenFireAndDelayRegularAlarms) {
        storage.setAlarmDelay(1000);

        proto::Alarm alarm;
        std::string alarmId = makeUUID();
        alarm.set_alarm_type(proto::Alarm::ALARM);
        alarm.set_id(makeUUID());
        alarm.set_start_timestamp_ms(0);

        constexpr long int curTime = 3;

        storage.addEvent(alarmId, alarm);
        auto fireEvents = storage.fireEvents(std::chrono::seconds(curTime), std::chrono::milliseconds(0));
        UNIT_ASSERT(!storage.isEmpty());
        UNIT_ASSERT(fireEvents.actualEvents.empty());
        UNIT_ASSERT(fireEvents.expiredEvents.empty());

        std::string state = "BEGIN:VCALENDAR\n"
                            "VERSION:2.0\n"
                            "PRODID:-//Yandex LTD//NONSGML Quasar//EN\n"
                            "BEGIN:VEVENT\n"
                            "DTSTART:19700101T000000Z\n"
                            "DTEND: 19700101T000000Z\n"
                            "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA,SU\n"
                            "BEGIN:VALARM\n"
                            "TRIGGER:P0D\n"
                            "ACTION:AUDIO\n"
                            "END:VALARM\n"
                            "END:VEVENT\n"
                            "END:VCALENDAR\n"; // Every day at 08:00 am

        storage.setICalendarState(state);

        storage.reParseICalendar(curTime);

        UNIT_ASSERT(!storage.isEmpty());
    }
}
