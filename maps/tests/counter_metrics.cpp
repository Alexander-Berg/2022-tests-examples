#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/roquefort/lib/counter_profile.h>

#include <fstream>

using namespace maps::roquefort;

const std::string PARENT_NAME = "test";

Y_UNIT_TEST_SUITE(CounterMetricsTestSuite)
{
    Y_UNIT_TEST(CounterMetricTest)
    {
        std::string suffix{"ammv"};
        CounterProfile::CounterMetric metric(PARENT_NAME, suffix, 3);
        for (int i = 0; i < 6; ++i) {
            metric.add({});
        }

        std::stringstream ss;
        ss << metric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_ps_ammv\",2]]", ss.str());
    }

    Y_UNIT_TEST(MetricGroupTest)
    {
        {
            CounterProfile::MetricGroup metrics(
                "test-counter_example", maps::json::Value::fromFile("files/test-counter.conf")["example"]);
            auto counter = metrics.counter(2);

            std::ifstream log("files/test.log");
            std::string buf;
            while (std::getline(log, buf)) {
                auto parsed = Fields::parseTskv(buf);
                counter->add(parsed);
            }

            std::stringstream ss;
            ss << counter->yasmMetrics();

            UNIT_ASSERT_VALUES_EQUAL("[[\"test-counter_example_ps_axxv\",6]]", ss.str());
        }

        {
            CounterProfile::MetricGroup metrics(
                "test-counter_example", maps::json::Value::fromFile("files/test-counter.conf")["example"]);
            auto counter = metrics.counter(6);

            std::ifstream log("files/test.log");
            std::string buf;
            while (std::getline(log, buf)) {
                auto parsed = Fields::parseTskv(buf);
                counter->add(parsed);
            }

            std::stringstream ss;
            ss << counter->yasmMetrics();

            UNIT_ASSERT_VALUES_EQUAL("[[\"test-counter_example_ps_axxv\",2]]", ss.str());
        }

        {
            CounterProfile::MetricGroup metrics(
                "test-counter_example-200", maps::json::Value::fromFile("files/test-counter.conf")["example-200"]);
            auto counter = metrics.counter(2);

            std::ifstream log("files/test.log");
            std::string buf;
            while (std::getline(log, buf)) {
                auto parsed = Fields::parseTskv(buf);
                counter->add(parsed);
            }

            std::stringstream ss;
            ss << counter->yasmMetrics();

            UNIT_ASSERT_VALUES_EQUAL("[[\"test-counter_example-200_ps_axxv\",1.5]]", ss.str());
        }
    }

    Y_UNIT_TEST(MetricGroupAggregateTest)
    {
        CounterProfile::MetricGroup metrics(
                "test-counter_example", maps::json::Value::fromFile("files/test-counter.conf")["example"]);
        auto firstCounter = metrics.counter(1);
        auto secondCounter = metrics.counter(1);

        std::ifstream log("files/test.log");
        std::string buf;
        while (std::getline(log, buf)) {
            auto parsed = Fields::parseTskv(buf);
            firstCounter->add(parsed);
            secondCounter->add(parsed);
        }

        firstCounter->aggregate(secondCounter.get());
        std::stringstream ss;
        ss << firstCounter->yasmMetrics();

        UNIT_ASSERT_VALUES_EQUAL("[[\"test-counter_example_ps_axxv\",12]]", ss.str());
    }
}
