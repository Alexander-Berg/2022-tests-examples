#include <yandex_io/libs/triggers/time_trigger.h>

#include <library/cpp/testing/unittest/registar.h>
#include <yandex_io/libs/logging/logging.h>
#include <util/datetime/systime.h>

Y_UNIT_TEST_SUITE(triggers) {
    Y_UNIT_TEST(TooLow) {
        quasar::TriggerConfig cfg{
            .limit = 10,
            .interval = std::chrono::seconds(5),
        };
        quasar::TooLowTrigger tooLow;
        auto timePoint = std::chrono::steady_clock::now();
        tooLow.update(0, timePoint, cfg);
        UNIT_ASSERT(!tooLow.update(100, timePoint + std::chrono::seconds(10), cfg));
        UNIT_ASSERT(!tooLow.update(101, timePoint + std::chrono::seconds(11), cfg));
        UNIT_ASSERT(!tooLow.update(104, timePoint + std::chrono::seconds(12), cfg));
        UNIT_ASSERT(!tooLow.update(116, timePoint + std::chrono::seconds(15), cfg));
        UNIT_ASSERT(tooLow.update(121, timePoint + std::chrono::seconds(20), cfg));

    }

    Y_UNIT_TEST(TooHigh) {
        quasar::TriggerConfig cfg{
            .limit = 10,
            .interval = std::chrono::seconds(5),
        };
        quasar::TooHighTrigger tooHigh;
        auto timePoint = std::chrono::steady_clock::now();
        tooHigh.update(0, timePoint, cfg);
        UNIT_ASSERT(!tooHigh.update(1, timePoint + std::chrono::seconds(1), cfg));
        UNIT_ASSERT(!tooHigh.update(1, timePoint + std::chrono::seconds(6), cfg));
        UNIT_ASSERT(tooHigh.update(100, timePoint + std::chrono::seconds(11), cfg));
    }

    Y_UNIT_TEST(FalseTooLong) {
        quasar::FalseTooLong tooLong;
        auto timePoint = std::chrono::steady_clock::now();
        const std::chrono::seconds limit{5};
        tooLong.update(true, timePoint);
        UNIT_ASSERT(!tooLong.check(timePoint + std::chrono::seconds(10), limit));
        tooLong.update(false, timePoint + std::chrono::seconds(11));
        UNIT_ASSERT(!tooLong.check(timePoint + std::chrono::seconds(15), limit));
        UNIT_ASSERT(tooLong.check(timePoint + std::chrono::seconds(17), limit));
        tooLong.update(true, timePoint + std::chrono::seconds(18));
        UNIT_ASSERT(!tooLong.check(timePoint + std::chrono::seconds(20), limit));
    }

    Y_UNIT_TEST(DailyTrigger) {
        auto now = std::chrono::system_clock::now();
        time_t ep = std::chrono::system_clock::to_time_t(now);
        struct tm loct;
        localtime_r(&ep, &loct);
        YIO_LOG_INFO(loct.tm_hour << ':' << loct.tm_min);
        quasar::OnceADayTrigger trigger;
        trigger.changeTime(std::chrono::hours(loct.tm_hour) + std::chrono::minutes(loct.tm_min));
        UNIT_ASSERT(!trigger.check(now - std::chrono::hours(1)));
        UNIT_ASSERT(trigger.check(now + std::chrono::seconds(10)));
        UNIT_ASSERT(!trigger.check(now + std::chrono::hours(1)));
        UNIT_ASSERT(trigger.check(now + std::chrono::seconds(10) + std::chrono::hours(24)));
    }
}
