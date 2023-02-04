#include <yandex_io/libs/metrics_collector/numeric_stat.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE(TestNumericStat) {
    Y_UNIT_TEST(Double) {
        NumericStat<double> stat;

        stat.process(-5);
        stat.process(-5.5);
        stat.process(3);

        UNIT_ASSERT_DOUBLES_EQUAL(stat.getMin(), -5.5, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat.getMax(), 3, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat.getMean(), -2.5, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat.getLast(), 3, 0.0001);

        stat.reset();
        stat.process(1);
        UNIT_ASSERT_DOUBLES_EQUAL(stat.getMin(), 1, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat.getMax(), 1, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat.getMean(), 1, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat.getLast(), 1, 0.0001);
    }

    Y_UNIT_TEST(DoubleAssociativity) {
        NumericStat<double> stat1;
        stat1.process(1.0);
        stat1.process(2.0);

        NumericStat<double> stat2;
        stat2.process(4.0);
        stat2.process(8.0);

        NumericStat<double> stat12;
        stat12.process(1.0);
        stat12.process(2.0);
        stat12.process(4.0);
        stat12.process(8.0);

        auto merged = stat1;
        merged.processStat(stat2);

        UNIT_ASSERT_DOUBLES_EQUAL(stat12.getMin(), merged.getMin(), 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat12.getMax(), merged.getMax(), 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat12.getMean(), merged.getMean(), 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(stat12.getLast(), merged.getLast(), 0.0001);
    }
}
