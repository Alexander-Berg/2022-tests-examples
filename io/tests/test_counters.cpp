#include <yandex_io/libs/logging/logging.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/common/env.h>
#include <yandex_io/libs/logging/setup/setup.h>
#include <util/system/env.h>

#include <yandex_io/libs/counters/counters_over_periods.h>
#include <yandex_io/libs/counters/enumerated_counter.h>
#include <yandex_io/libs/counters/json_utils.h>
#include <yandex_io/libs/counters/default_daily_counter.h>

using namespace quasar;

namespace {
    class SharedTestClock {
        const std::uint64_t& now;

    public:
        SharedTestClock(std::uint64_t& n)
            : now(n)
        {
        }

        std::uint64_t getNow() const {
            return now;
        }
    };

    enum Field {
        FLD1,
        FLD2,
        FLD3
    };

    using MultiCounter = EnumeratedCounter<Field, 3>;

    struct CountersFixture: public NUnitTest::TBaseFixture {
        using Base = NUnitTest::TBaseFixture;
        using HourTestCounters = CountersOverPeriodsImpl<std::uint64_t, SharedTestClock, 60, 300, 900, 3600>;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
            quasar::Logging::initLoggingToStdout(GetTestParam("log_level", GetEnv("YIO_LOG_LEVEL", "debug")));
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            quasar::Logging::deinitLogging();
            Base::TearDown(context);
        }

        void dump(const HourTestCounters::Result& cnts) const {
            YIO_LOG_DEBUG("time = " << nowStorage << ", counters = " << cnts[0] << ' ' << cnts[1] << ' ' << cnts[2] << ' ' << cnts[3]);
        }

        void checkCounters(HourTestCounters::Result expect) const {
            auto cnts = hourCnt.getCounters();
            dump(cnts);
            for (unsigned i = 0; i < expect.size(); ++i) {
                UNIT_ASSERT_EQUAL_C(cnts[i], expect[i], " at index " + std::to_string(i));
            }
        }

        std::uint64_t nowStorage{0};
        HourTestCounters hourCnt{SharedTestClock(nowStorage)};
    };
} // namespace

Y_UNIT_TEST_SUITE(Counters) {
    Y_UNIT_TEST_F(oncePerPeriod, CountersFixture) {
        hourCnt.increment();
        checkCounters({1, 1, 1, 1}); // we have one 'event' in first period of 60 seconds

        nowStorage += 60;
        hourCnt.increment();
        checkCounters({1, 2, 2, 2}); // now we have one in first period and one in second of 300

        nowStorage = 300;
        hourCnt.increment();
        checkCounters({1, 2, 3, 3}); // now, 240 seconds ago, and 300 seconds ago

        nowStorage = 900;
        hourCnt.increment();
        checkCounters({1, 1, 3, 4}); // tricky part. now, 600 seconds ago, 840 seconds ago. 600 seconds ago not included into 10minutes counters but included into an hour

        nowStorage = 3600;
        hourCnt.increment();
        checkCounters({1, 1, 1, 4}); // same stuff, only one 'event' included into 60/300/900 secs all other gone to hour
    }

    Y_UNIT_TEST_F(everySecond, CountersFixture) {
        for (int i = 0; i < 3601; ++i) {
            hourCnt.increment();
            ++nowStorage;
        }
        checkCounters({60, 300, 900, 3600});
        nowStorage += 3601;

        hourCnt.increment();
        checkCounters({1, 1, 1, 1});
    }

    Y_UNIT_TEST_F(zeroing, CountersFixture) {
        for (int i = 0; i < 3601; ++i) {
            hourCnt.increment();
            ++nowStorage;
        }
        nowStorage += 3601;
        hourCnt.getUpdatedCounters();
        checkCounters({0, 0, 0, 0});
        for (int i = 0; i < 3601; ++i) {
            hourCnt.increment();
            ++nowStorage;
        }
    }

    Y_UNIT_TEST(multiCounters) {
        using TestCounter = CountersOverPeriodsImpl<MultiCounter, SharedTestClock, 60, 300, 900>;
        std::uint64_t nowStorage = 0;
        SharedTestClock testClock(nowStorage);
        TestCounter mCnts(testClock);

        mCnts.increment({Field::FLD1, Field::FLD3});
        nowStorage = 60;
        mCnts.increment(Field::FLD2);
        const auto cnts = mCnts.getCounters();
        UNIT_ASSERT_EQUAL(cnts[0].get()[0], 0);
        UNIT_ASSERT_EQUAL(cnts[0].get()[1], 1);
        UNIT_ASSERT_EQUAL(cnts[0].get()[2], 0);
        UNIT_ASSERT_EQUAL(cnts[1].get()[0], 1);
        UNIT_ASSERT_EQUAL(cnts[1].get()[1], 1);
        UNIT_ASSERT_EQUAL(cnts[1].get()[2], 1);
        UNIT_ASSERT_EQUAL(cnts[1].get()[0], 1);
        UNIT_ASSERT_EQUAL(cnts[1].get()[1], 1);
        UNIT_ASSERT_EQUAL(cnts[1].get()[2], 1);
    }

    Y_UNIT_TEST(toJson) {
        using TestCounter = CountersOverPeriodsImpl<MultiCounter, SharedTestClock, 60, 300, 900>;
        std::uint64_t nowStorage = 0;
        SharedTestClock testClock(nowStorage);
        TestCounter mCnts(testClock);

        Field cur = Field::FLD1;
        for (int i = 0; i < 901; ++i) {
            mCnts.increment(cur);
            if (cur == Field::FLD3) {
                cur = Field::FLD1;
            } else {
                cur = Field(int(cur) + 1);
            };
            ++nowStorage;
        }
        auto cnts = mCnts.getCounters();
        auto json = EnumCountersToJson(cnts, {"fld1", "fld2", "fld3"});
        Json::Value expect = Json::arrayValue;
        expect.append(20u);
        expect.append(100u);
        expect.append(300u);
        UNIT_ASSERT(json["fld1"] == expect);
        UNIT_ASSERT(json["fld2"] == expect);
        UNIT_ASSERT(json["fld3"] == expect);
        auto json2 = EnumCountersToJson(cnts, {"fld1", "fld2"});
        UNIT_ASSERT_EQUAL(json.size(), 3);
        UNIT_ASSERT_EQUAL(json2.size(), 2);
        UNIT_ASSERT(json["fld1"] == json2["fld1"]);
        UNIT_ASSERT(json["fld2"] == json2["fld2"]);
    }

    Y_UNIT_TEST(defaultDailyCounter) {
        enum Counters {
            FLD1,
            FLD2,
            FLD3,
            FLD4,
            FLD5,
            FLD6,
            MAX_
        };

        DefaultDailyCounter<Counters> dailyStats_;
        const auto cnts = dailyStats_.getUpdatedCounters();
        const auto json = EnumCountersToJson(cnts, {"fld1", "fld2", "fld3", "fld4", "fld5", "fld6"});
        UNIT_ASSERT_EQUAL(json.size(), Counters::MAX_);
    }
}
