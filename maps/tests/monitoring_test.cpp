#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/introspection/include/stream_output.h>
#include <maps/infra/ratelimiter2/core/include/monitoring.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::rate_limiter2 {

template <typename Info>
inline auto introspect(const LimitsSelectorRegistry<Info>& data)
{
    return std::make_pair(std::ref(data.storage()), data.version());
}

using introspection::operator<<;
using introspection::operator==;

} // namespace maps::rate_limiter2

namespace maps::yasm_metrics {

inline auto introspect(const YasmMetrics& data)
{
    std::stringstream ss;
    ss << data;
    return ss.str();
}
using introspection::operator<<;
using introspection::operator==;

} // namespace maps::yasm_metrics

namespace maps::rate_limiter2::tests {

using yasm_metrics::YasmMetrics;
using yasm_metrics::NumericalYasmMetric;

Y_UNIT_TEST_SUITE(monitoring_test) {

Y_UNIT_TEST(names_registry)
{
    proto::rate_limiter2::Limits protoLimits;
    protoLimits.set_version(153);
    {   // client1 -> resource1
        auto res = protoLimits.add_resources();
        res->set_client_id(TString("client1"));
        res->set_resource(TString("resource1"));
        res->set_rps(10);
        res->set_burst(10);
    }
    {   // all clients -> resource1
        auto res = protoLimits.add_resources();
        res->set_resource(TString("resource1"));
        res->set_rps(1);
        res->set_burst(1);
        res->set_unit(1);
    }
    {   // client2 -> resource2
        auto res = protoLimits.add_resources();
        res->set_client_id(TString("client2"));
        res->set_resource(TString("resource2"));
        res->set_rps(10);
        res->set_burst(10);
        res->set_unit(1);
    }
    {   // client3 -> all resources
        auto res = protoLimits.add_resources();
        res->set_client_id(TString("client3"));
        res->set_rps(10);
        res->set_burst(10);
        res->set_unit(1);
    }

    auto registry = parse<NamesRegistry>(protoLimits);

    auto client1 = makeClientHash("client1");
    auto client2 = makeClientHash("client2");
    auto client3 = makeClientHash("client3");

    {  // check content
        NamesRegistry expected({
                { client1, {{ "resource1", "client1@resource1" }} },
                { makeClientHash(""), {{"resource1", "default@resource1"}} },
                { client2, {{"resource2", "client2@resource2"}}  },
                { client3, {{"", "client3@default"}} },
        }, 153);
        EXPECT_EQ(registry, expected);
    }

    // check select
    {
        auto selected = registry.trySelect(client1, "resource1");
        ASSERT_TRUE(selected);
        EXPECT_EQ("client1@resource1", static_cast<const std::string&>(*selected));
    }
    EXPECT_FALSE(registry.trySelect(client1, "resource2"));
    {
        auto selected = registry.trySelect(client2, "resource1");
        ASSERT_TRUE(selected);
        EXPECT_EQ("default@resource1", static_cast<const std::string&>(*selected));
    }
    {
        auto selected = registry.trySelect(client2, "resource2");
        ASSERT_TRUE(selected);
        EXPECT_EQ("client2@resource2", static_cast<const std::string&>(*selected));
    }
    {
        auto selected = registry.trySelect(makeClientHash("some.client"), "resource1");
        ASSERT_TRUE(selected);
        EXPECT_EQ("default@resource1", static_cast<const std::string&>(*selected));
    }
}

Y_UNIT_TEST(rps_metrics_generator_values)
{
    auto client = makeClientHash("cli");

    RPSMetricsGenerator rps(NamesRegistry::Storage{{ client, {{"res", "cli@res"}} }}, "_dxxm");

    const std::string metricName = "cli@res_dxxm";

    {
        auto metrics = rps.generate(SortedCounters({{"res", {{client, 1}}}}));
        auto expected = YasmMetrics().addMetric(NumericalYasmMetric(metricName, 1.0));
        EXPECT_EQ(metrics, expected);
    }
    {
        auto metrics = rps.generate(SortedCounters({{"res", {{client, 2}}}}));
        auto expected = YasmMetrics().addMetric(NumericalYasmMetric(metricName, 2.0));
        EXPECT_EQ(metrics, expected);
    }
    {   // same value
        auto metrics = rps.generate(SortedCounters({{"res", {{client, 2}}}}));
        auto expected = YasmMetrics().addMetric(NumericalYasmMetric(metricName, 2.0));
        EXPECT_EQ(metrics, expected);
    }
    {   // zero
        auto metrics = rps.generate(SortedCounters({{"res", {{client, 0}}}}));
        auto expected = YasmMetrics().addMetric(NumericalYasmMetric(metricName, 0.0));
        EXPECT_EQ(metrics, expected);
    }
    {   // empty
        auto metrics = rps.generate(SortedCounters());
        EXPECT_EQ(metrics, YasmMetrics());  // expect empty
    }
}

Y_UNIT_TEST(rps_metrics_generator_names)
{
    auto client1 = makeClientHash("client1");
    auto client2 = makeClientHash("client2");
    auto client3 = makeClientHash("client3");
    auto client4 = makeClientHash("client4");

    RPSMetricsGenerator rps(NamesRegistry{{
        { client1, {{ "resource1", "client1@resource1" }} },
        { makeClientHash(""), {{"resource1", "default@resource1"}} },
        { client2, {{"resource2", "client2@resource2"}}  },
    }}, "");

    {
        auto metrics = rps.generate(SortedCounters({
            {"resource1", {{client1, 1}}},
            {"resource2", {{client2, 100}}},
        }));
        YasmMetrics expected;
        expected.addMetric(NumericalYasmMetric("client1@resource1", 1.0));
        expected.addMetric(NumericalYasmMetric("client2@resource2", 100.0));
        EXPECT_EQ(metrics, expected);
    }
    {
        auto metrics = rps.generate(SortedCounters({
            {"resource1", {{client1, 6}}},
            {"resource2", {{client2, 200}}},
        }));
        YasmMetrics expected;
        expected.addMetric(NumericalYasmMetric("client1@resource1", 6.0));
        expected.addMetric(NumericalYasmMetric("client2@resource2", 200.0));
        EXPECT_EQ(metrics, expected);
    }
    {
        auto metrics = rps.generate(SortedCounters({
            {"resource1", {{client3, 10}}},
            {"resource2", {{client2, 300}}},
        }));
        YasmMetrics expected;   // client1 dropped, client3->default
        expected.addMetric(NumericalYasmMetric("default@resource1", 10.0));
        expected.addMetric(NumericalYasmMetric("client2@resource2", 300.0));
        EXPECT_EQ(metrics, expected);
    }
    {
        auto metrics = rps.generate(SortedCounters({
            {"resource1", {{client3, 20}, {client4, 33}}},
            {"resource2", {{client2, 400}}},
        }));

        YasmMetrics expected;
        expected.addMetric(NumericalYasmMetric("default@resource1", 20.0 + 33.0));  // (client3+client4) -> default
        expected.addMetric(NumericalYasmMetric("client2@resource2", 400.0));
        EXPECT_EQ(metrics, expected);
    }

    // reload registry
    rps = RPSMetricsGenerator(NamesRegistry{{
        { client3, {{ "resource1", "client3@resource1" }} }  // single limit left in registry
    }}, "");

    {
        auto metrics = rps.generate(SortedCounters({
            {"resource1", {{client3, 20}}},
            {"resource2", {{client1, 200}}},
        }));

        YasmMetrics expected;   // all except client3->resource1 are 'undefined' now
        expected.addMetric(NumericalYasmMetric("client3@resource1", 20.0));
        expected.addMetric(NumericalYasmMetric("undefined", 200.0));
        EXPECT_EQ(metrics, expected);
    }
}

Y_UNIT_TEST(rps_metrics_generator_resource_totals)
{
    RPSMetricsGenerator gen({}, "_dxxm");  // NB: Names registry not needed, so pass empty

    EXPECT_EQ(gen.generateResourceTotals({}), YasmMetrics());

    auto client1 = makeClientHash("cli1");
    auto client2 = makeClientHash("cli2");
    auto client3 = makeClientHash("cl3");
    auto metrics = gen.generateResourceTotals({
        {"res0", {}},
        {"res1", {{client1, 1}}},
        {"res3", {{client1, 1}, {client2, 1}, {client3, 1}}}
    });
    auto expected = YasmMetrics()
        .addMetric(NumericalYasmMetric("res3_total_dxxm", 3.0))
        .addMetric(NumericalYasmMetric("res1_total_dxxm", 1.0))
        .addMetric(NumericalYasmMetric("res0_total_dxxm", 0.0));
    EXPECT_EQ(metrics, expected);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
