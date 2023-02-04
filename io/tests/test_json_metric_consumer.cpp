#include <yandex_io/libs/metrics_collector/consumers/json/json_metric_consumer.h>

#include <yandex_io/libs/json_utils/json_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <functional>

using namespace quasar;

namespace {
    Json::Value buildMetricJson(std::function<void(IMetricConsumer&)> f) {
        auto result = Json::Value(Json::objectValue);
        JsonMetricConsumer consumer(result);
        f(consumer);
        return result;
    }
} // namespace

Y_UNIT_TEST_SUITE(TestJsonMetricConsumer) {
    Y_UNIT_TEST(Empty) {
        auto result = buildMetricJson([](auto& /*metrics*/) {});
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{}\n");
    }

    Y_UNIT_TEST(SingleDGaugeValue) {
        auto result = buildMetricJson([](auto& metrics) {
            metrics.addDGauge("testDGauge", 42.5);
        });
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{\"testDGauge\":42.5}\n");
    }

    Y_UNIT_TEST(SingleIGaugeValue) {
        auto result = buildMetricJson([](auto& metrics) {
            metrics.addIGauge("testIGauge", 1'000'000'000'000);
        });
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{\"testIGauge\":1000000000000}\n");
    }

    Y_UNIT_TEST(SingleJsonValue) {
        auto result = buildMetricJson([](auto& metrics) {
            auto value = Json::Value();
            value["foo"] = "bar";
            value["bar"] = 42;
            metrics.addJson("testJson", std::move(value));
        });
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{\"testJson\":{\"bar\":42,\"foo\":\"bar\"}}\n");
    }

    Y_UNIT_TEST(MultipleValues) {
        auto result = buildMetricJson([](auto& metrics) {
            auto value = Json::Value();
            value["foo"] = "bar";
            value["bar"] = 42;
            metrics.addJson("testJson", std::move(value));
            metrics.addDGauge("testDGauge", 42.5);
            metrics.addIGauge("testIGauge", 1'000'000'000'000);
        });
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{\"testDGauge\":42.5,\"testIGauge\":1000000000000,\"testJson\":{\"bar\":42,\"foo\":\"bar\"}}\n");
    }

    Y_UNIT_TEST(MultipleValuesWithOverlappingKeys) {
        auto result = buildMetricJson([](auto& metrics) {
            metrics.addDGauge("testGauge", 42.5);
            metrics.addIGauge("testGauge", 1'000'000'000'000);
        });
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{\"testGauge\":1000000000000}\n");
    }

    Y_UNIT_TEST(SingleDGaugeStatValue) {
        auto result = buildMetricJson([](auto& metrics) {
            NumericStat<double> stat;
            stat.process(1.0);
            stat.process(2.0);
            stat.process(4.0);
            stat.process(8.0);
            metrics.addDGaugeStat("testDGaugeStat", stat);
        });
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{\"testDGaugeStat\":{\"last\":8.0,\"max\":8.0,\"mean\":3.75,\"min\":1.0}}\n");
    }

    Y_UNIT_TEST(SingleIGaugeStatValue) {
        auto result = buildMetricJson([](auto& metrics) {
            NumericStat<int64_t> stat;
            stat.process(1);
            stat.process(2);
            stat.process(4);
            stat.process(8);
            metrics.addIGaugeStat("testIGaugeStat", stat);
        });
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(result), "{\"testIGaugeStat\":{\"last\":8,\"max\":8,\"mean\":3.75,\"min\":1}}\n");
    }
}
