#include <maps/infopoint/lib/moderation/verdict_source.h>
#include <maps/infopoint/tests/common/fixture.h>

#include <library/cpp/testing/gtest/gtest.h>

#include "../lib/metrics/metrics.h"

#include <map>


using namespace infopoint;
using namespace infopoint::moderation;
using namespace infopoint::metrics;

TEST(VerdictSource, RevealMetric)
{
    std::map<std::string, Metric> tests {
        {"cache-simple:common-tur-toloka", Metric::CommonCacheTurSubsource},
        {"cache-simple:common_toloka_v2", Metric::CommonCacheRuSubsource},
        {"cache-simple:custom-toloka", Metric::CustomCacheRuSubsource},
        {"cache-simple:custom-tur-toloka", Metric::CustomCacheTurSubsource},
        {"cache-simple:custom-tur-toloka-new", Metric::CustomCacheTurSubsource},
        {"cache-simple:toxic_toloka_v3", Metric::CommonCacheRuSubsource},
        {"cache-smart:common-tur-toloka", Metric::CommonCacheTurSubsource},
        {"cache-smart:common_toloka_v2", Metric::CommonCacheRuSubsource },
        {"cache-smart:custom-toloka", Metric::CustomCacheRuSubsource},
        {"cache-smart:custom-tur-toloka", Metric::CustomCacheTurSubsource},
        {"cache-smart:custom-tur-toloka-new", Metric::CustomCacheTurSubsource},
        {"cache-smart:toxic_toloka_v3", Metric::CommonCacheRuSubsource},
        {"common_toloka_v2", Metric::CommonTolokaRuSubsource},
        {"common-tur-toloka", Metric::CommonTolokaTurSubsource},
        {"custom-toloka", Metric::CustomTolokaRuSubsource},
        {"custom-tur-toloka", Metric::CustomTolokaTurSubsource},
        {"custom-tur-toloka-new", Metric::CustomTolokaTurSubsource},
        {"toxic_toloka_v3", Metric::CommonTolokaRuSubsource},
        {"final_auto", Metric::AutomaticSubsource},
        {"tmu", Metric::AutomaticSubsource},
        {"ml", Metric::AutomaticSubsource},
    };

    for (const auto& [subsource, metric] : tests) {
        EXPECT_EQ(revealMetric(subsource), metric);
    }
}
