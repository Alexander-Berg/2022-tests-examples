#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/roquefort/lib/nginx_profile.h>
#include <maps/libs/common/include/file_utils.h>

#include <fstream>

using namespace maps::roquefort;

static const std::string PARENT_NAME = "test";

Y_UNIT_TEST_SUITE(NginxAccessTskvMetricsTestSuite)
{
    Y_UNIT_TEST(HttpRangeTest)
    {
        {
            NginxAccessTskvProfile::HttpRange range("test");
            UNIT_ASSERT(range.matches({}));
        }

        {
            NginxAccessTskvProfile::HttpRange range("test", 400);
            UNIT_ASSERT(range.matches(Fields({{"status", "400"}})));
            UNIT_ASSERT(!range.matches(Fields({{"status", "500"}})));
        }

        {
            NginxAccessTskvProfile::HttpRange range("test", {{400, 499}});
            UNIT_ASSERT(range.matches(Fields({{"status", "400"}})));
            UNIT_ASSERT(!range.matches(Fields({{"status", "500"}})));
        }

        {
            NginxAccessTskvProfile::HttpRange range("test", {{400, 403}, {405, 428}, {430, 498}});
            UNIT_ASSERT(range.matches(Fields({{"status", "401"}})));
            UNIT_ASSERT(!range.matches(Fields({{"status", "429"}})));
        }

        {
            NginxAccessTskvProfile::HttpRange range("test", 400);
            UNIT_ASSERT(!range.matches({}));
        }
    }

    Y_UNIT_TEST(HttpRangeMetricTest)
    {
        NginxAccessTskvProfile::HttpRange range("test", {{400, 403}, {405, 428}, {430, 498}});
        NginxAccessTskvProfile::HttpRangeMetric metric(PARENT_NAME, nullptr, 2, range);
        metric.add(Fields({{"status", "400"}}));
        metric.add(Fields({{"status", "500"}}));
        metric.add(Fields({{"status", "400"}}));
        metric.add(Fields({{"status", "401"}}));
        metric.add(Fields({{"status", "429"}}));
        metric.add(Fields({{"status", "-"}}));
        metric.add({});

        std::stringstream ss;
        ss << metric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_test_ammv\",1.5]]", ss.str());
    }

    Y_UNIT_TEST(HttpRangeMetricFilterTest)
    {
        std::shared_ptr<Operation> filter = std::make_shared<OperationEquals>("status", "401");
        NginxAccessTskvProfile::HttpRange range("test", {{400, 403}, {405, 428}, {430, 498}});
        NginxAccessTskvProfile::HttpRangeMetric metric(PARENT_NAME, filter, 2, range);
        metric.add(Fields({{"status", "400"}}));
        metric.add(Fields({{"status", "500"}}));
        metric.add(Fields({{"status", "400"}}));
        metric.add(Fields({{"status", "401"}}));
        metric.add(Fields({{"status", "429"}}));
        metric.add(Fields({{"status", "-"}}));
        metric.add({});

        std::stringstream ss;
        ss << metric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_test_ammv\",0.5]]", ss.str());
    }

    Y_UNIT_TEST(TimingMetricTest)
    {
        NginxAccessTskvProfile::RequestTimingMetric metric(PARENT_NAME, nullptr, 1);
        metric.add(Fields({{"request_time", "0.001"}}));
        metric.add(Fields({{"request_time", "0.1"}}));
        metric.add(Fields({{"request_time", "0.12"}}));
        metric.add(Fields({{"request_time", "1"}}));
        metric.add(Fields({{"request_time", "500"}}));
        metric.add(Fields({{"request_time", "-"}}));
        metric.add({});

        std::stringstream ss;
        ss << metric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL(
            "[[\"test_request_timings_ahhh\",[[2,1],[3,0],[5,0],[7,0],[11,0],[17,0],[26,0],[39,0],[58,0],[87,2],[131,0],[197,0],[296,0],[444,0],[666,0],[1000,1],[1500,0],[2250,0],[3375,0],[5062,0],[7593,0],[11390,0],[17085,0],[25628,0],[38443,0],[57665,0],[86497,0],[129746,0],[194619,0],[291929,0],[437893,1]]]]",
            ss.str());
    }

    Y_UNIT_TEST(ByteMetricTest)
    {
        NginxAccessTskvProfile::ByteMetric metric(PARENT_NAME, nullptr, 3);
        metric.add(Fields({{"bytes_sent", "123"}}));
        metric.add(Fields({{"bytes_sent", "456"}}));
        metric.add(Fields({{"bytes_sent", "321"}}));
        metric.add(Fields({{"bytes_sent", "500"}}));
        metric.add(Fields({{"bytes_sent", "-"}}));
        metric.add({});

        std::stringstream ss;
        ss << metric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_bps_ammv\",466.6666666666667]]", ss.str());
    }

    Y_UNIT_TEST(CacheMetricTest)
    {
        NginxAccessTskvProfile::CacheMetric metric(
            PARENT_NAME, nullptr, 3, NginxAccessTskvProfile::CacheMetric::CacheType::Hit);
        metric.add(Fields({{"upstream_cache_status", "HIT"}}));
        metric.add(Fields({{"upstream_cache_status", "HIT"}}));
        metric.add(Fields({{"upstream_cache_status", "BYPASS"}}));
        metric.add(Fields({{"upstream_cache_status", "REVALIDATED"}}));
        metric.add(Fields({{"upstream_cache_status", "-"}}));
        metric.add({});

        std::stringstream ss;
        ss << metric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_cache_hit_ammv\",0.6666666666666666]]", ss.str());
    }

    Y_UNIT_TEST(MetricGroupTest)
    {
        auto profile = createProfile("nginx-access-tskv");

        std::vector<ServiceConfig> services;
        services.emplace_back("files/test.conf");

        auto allMetrics = profile->createMetrics(services);
        UNIT_ASSERT_VALUES_EQUAL(allMetrics.size(), 4);
        auto& metrics = allMetrics[0];

        auto counter = metrics->counter(2);

        std::ifstream log("files/test.log");
        UNIT_ASSERT_VALUES_EQUAL(log.is_open(), true);
        std::string buf;
        while (std::getline(log, buf)) {
            auto parsed = Fields::parseTskv(buf);
            counter->add(parsed);
        }

        std::stringstream ss;
        ss << counter->yasmMetrics();

        auto expected = maps::common::readFileToString("files/test_answer.txt");

        UNIT_ASSERT_VALUES_EQUAL(expected, ss.str());
    }

    Y_UNIT_TEST(MetricGroupWithCacheSignalsTest)
    {
        auto profile = createProfile("nginx-access-tskv");

        std::vector<ServiceConfig> services;
        services.emplace_back("files/test_cache.conf");

        auto allMetrics = profile->createMetrics(services);
        UNIT_ASSERT_VALUES_EQUAL(allMetrics.size(), 5);
        auto& metrics = allMetrics[0];

        auto counter = metrics->counter(2);

        std::ifstream log("files/test.log");
        UNIT_ASSERT_VALUES_EQUAL(log.is_open(), true);
        std::string buf;
        while (std::getline(log, buf)) {
            auto parsed = Fields::parseTskv(buf);
            counter->add(parsed);
        }

        std::stringstream ss;
        ss << counter->yasmMetrics();

        auto expected = maps::common::readFileToString("files/test_answer_cache.txt");

        UNIT_ASSERT_VALUES_EQUAL(expected, ss.str());
    }

    Y_UNIT_TEST(HttpRangeMetricAggregateTest)
    {
        NginxAccessTskvProfile::HttpRange range("test");
        NginxAccessTskvProfile::HttpRangeMetric firstMetric(PARENT_NAME, nullptr, 3, range);
        NginxAccessTskvProfile::HttpRangeMetric secondMetric(PARENT_NAME, nullptr, 2, range);
        firstMetric.add(Fields({{"status", "200"}}));
        firstMetric.add(Fields({{"status", "403"}}));
        secondMetric.add(Fields({{"status", "200"}}));
        secondMetric.add(Fields({{"status", "200"}}));
        secondMetric.add(Fields({{"status", "404"}}));
        firstMetric.aggregate(&secondMetric);

        std::stringstream ss;
        ss << firstMetric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_test_ammv\",1]]", ss.str());
    }

    Y_UNIT_TEST(TimingMetricAggregateTest)
    {
        NginxAccessTskvProfile::RequestTimingMetric firstMetric(PARENT_NAME, nullptr, 0.5);
        NginxAccessTskvProfile::RequestTimingMetric secondMetric(PARENT_NAME, nullptr, 1);
        firstMetric.add(Fields({{"request_time", "0.001"}}));
        firstMetric.add(Fields({{"request_time", "0.1"}}));
        secondMetric.add(Fields({{"request_time", "0.001"}}));
        secondMetric.add(Fields({{"request_time", "0.12"}}));
        firstMetric.aggregate(&secondMetric);

        std::stringstream ss;
        ss << firstMetric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL(
            "[[\"test_request_timings_ahhh\",[[2,2],[3,0],[5,0],[7,0],[11,0],[17,0],[26,0],[39,0],[58,0],[87,2],[131,0],[197,0],[296,0],[444,0],[666,0],[1000,0],[1500,0],[2250,0],[3375,0],[5062,0],[7593,0],[11390,0],[17085,0],[25628,0],[38443,0],[57665,0],[86497,0],[129746,0],[194619,0],[291929,0],[437893,0]]]]",
            ss.str());
    }

    Y_UNIT_TEST(ByteMetricAggregateTest)
    {
        NginxAccessTskvProfile::ByteMetric firstMetric(PARENT_NAME, nullptr, 1);
        NginxAccessTskvProfile::ByteMetric secondMetric(PARENT_NAME, nullptr, 0.5);
        firstMetric.add(Fields({{"bytes_sent", "123"}}));
        secondMetric.add(Fields({{"bytes_sent", "456"}}));
        firstMetric.aggregate(&secondMetric);

        std::stringstream ss;
        ss << firstMetric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_bps_ammv\",386]]", ss.str());
    }

    Y_UNIT_TEST(CacheMetricAggregateTest)
    {
        NginxAccessTskvProfile::CacheMetric firstMetric(
            PARENT_NAME, nullptr, 1.5, NginxAccessTskvProfile::CacheMetric::CacheType::Hit);
        NginxAccessTskvProfile::CacheMetric secondMetric(
            PARENT_NAME, nullptr, 1.5, NginxAccessTskvProfile::CacheMetric::CacheType::Hit);
        firstMetric.add(Fields({{"upstream_cache_status", "HIT"}}));
        firstMetric.add(Fields({{"upstream_cache_status", "HIT"}}));
        firstMetric.add(Fields({{"upstream_cache_status", "BYPASS"}}));
        secondMetric.add(Fields({{"upstream_cache_status", "HIT"}}));
        secondMetric.add(Fields({{"upstream_cache_status", "REVALIDATED"}}));
        firstMetric.aggregate(&secondMetric);

        std::stringstream ss;
        ss << firstMetric.yasmMetrics();
        UNIT_ASSERT_VALUES_EQUAL("[[\"test_cache_hit_ammv\",1]]", ss.str());
    }

    Y_UNIT_TEST(MetricGroupAggregateTest)
    {
        auto profile = createProfile("nginx-access-tskv");

        std::vector<ServiceConfig> services;
        services.emplace_back("files/test.conf");

        auto allMetrics = profile->createMetrics(services);
        UNIT_ASSERT_VALUES_EQUAL(allMetrics.size(), 4);
        auto& metrics = allMetrics[0];

        auto firstCounter = metrics->counter(1);
        auto secondCounter = metrics->counter(1);

        std::ifstream log("files/test.log");
        UNIT_ASSERT_VALUES_EQUAL(log.is_open(), true);
        std::string buf;
        while (std::getline(log, buf)) {
            auto parsed = Fields::parseTskv(buf);
            firstCounter->add(parsed);
            secondCounter->add(parsed);
        }
        firstCounter->aggregate(secondCounter.get());

        std::stringstream ss;
        ss << firstCounter->yasmMetrics();

        auto expected = maps::common::readFileToString("files/test_answer_aggregate.txt");

        UNIT_ASSERT_VALUES_EQUAL(expected, ss.str());
    }

    Y_UNIT_TEST(MollyFilterTest)
    {
        auto profile = createProfile("nginx-access-tskv");
        auto filter = profile->createFilter();
        // Expect filter block lines with request url parameters
        // everybodybecoolthisis=crasher or everybodybecoolthisis=molly
        auto samples = std::vector<std::pair<bool, std::string_view>>{
            {false, "request=/handle/?everybodybecoolthisis=crasher" },
            {false, "request=/?everybodybecoolthisis=molly&some=param"},
            {true, "request=/crasher?everybodybecoolthisis=whatever"},
            {true, "request=/handle/?holly=molly"},
            {true, "request=/handle?param=taram"}
        };
        for (const auto& [matched, line] : samples) {
            auto parsed = Fields::parseTskv(line);
            UNIT_ASSERT_VALUES_EQUAL_C(filter->matches(parsed), matched, line);
        }
    }

    Y_UNIT_TEST(ConfigureBuiltins)
    {
        auto profile = createProfile("nginx-access-tskv");
        std::vector<ServiceConfig> services;
        services.emplace_back("files/total.conf");
        auto allMetrics = profile->createMetrics(services);
        UNIT_ASSERT_VALUES_EQUAL(allMetrics.size(), 3);

        std::vector<std::unique_ptr<MetricGroup::Counter>> counters;
        for (auto& metric: allMetrics)
            counters.push_back(metric->counter(2));

        std::ifstream log("files/teapot_test.log");
        UNIT_ASSERT_VALUES_EQUAL(log.is_open(), true);
        std::string buf;
        while (std::getline(log, buf)) {
            auto parsed = Fields::parseTskv(buf);
            for (auto& counter: counters)
                counter->add(parsed);
        }

        maps::yasm_metrics::YasmMetrics yasmMetrics;
        for (const auto& counter: counters)
            yasmMetrics.addMetrics(counter->yasmMetrics());
        std::stringstream ss;
        ss << yasmMetrics;

        auto expected = maps::common::readFileToString("files/builtins_test_answer.txt");
        UNIT_ASSERT_VALUES_EQUAL(expected, ss.str());
    }

    Y_UNIT_TEST(SelectBuiltinMetrics)
    {
        auto profile = createProfile("nginx-access-tskv");
        std::vector<ServiceConfig> services;
        services.emplace_back("files/test_select_builtin_metrics.conf");
        auto allMetrics = profile->createMetrics(services);
        UNIT_ASSERT_VALUES_EQUAL(allMetrics.size(), 4);

        std::vector<std::unique_ptr<MetricGroup::Counter>> counters;
        for (auto& metric: allMetrics)
            counters.push_back(metric->counter(2));

        std::ifstream log("files/teapot_test.log");
        UNIT_ASSERT_VALUES_EQUAL(log.is_open(), true);
        std::string buf;
        while (std::getline(log, buf)) {
            auto parsed = Fields::parseTskv(buf);
            for (auto& counter: counters)
                counter->add(parsed);
        }

        maps::yasm_metrics::YasmMetrics yasmMetrics;
        for (const auto& counter: counters)
            yasmMetrics.addMetrics(counter->yasmMetrics());
        std::stringstream ss;
        ss << yasmMetrics;

        auto expected = maps::common::readFileToString("files/test_select_builtin_metrics.txt");
        UNIT_ASSERT_VALUES_EQUAL(expected, ss.str());
    }
}
