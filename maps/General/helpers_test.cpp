#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/poi_feed/include/helpers.h>
#include <maps/wikimap/mapspro/libs/poi_feed/include/feed_settings_config.h>
#include <maps/wikimap/mapspro/libs/poi_feed/include/helpers.h>
#include <yandex/maps/wiki/unittest/json_schema.h>
#include <library/cpp/resource/resource.h>

using namespace maps::wiki::poi_feed;

Y_UNIT_TEST_SUITE(helpers)
{
Y_UNIT_TEST(loadProtectedFtTypes)
{
    auto ftTypes = loadProtectedFtTypes();
    UNIT_ASSERT(ftTypes.size());
    UNIT_ASSERT(!ftTypes.count(0));
    UNIT_ASSERT(ftTypes.count(2010));
}

Y_UNIT_TEST(FeedSettingsConfig)
{
    FeedSettingsConfig feedCfg;
    const auto* feedSettings = feedCfg.feedSettings(1);
    UNIT_ASSERT(feedSettings);
    UNIT_ASSERT(feedSettings->moderateAllChanges == ModerateAllChanges::Yes);
    for (const auto& ftTypeId : feedCfg.configuredFtTypes()) {
        UNIT_ASSERT(maps::wiki::poi_feed::isFullMergeImplemented(ftTypeId));
    }
}

Y_UNIT_TEST(settings_schema)
{
    maps::wiki::unittest::validateJson(
        NResource::Find("FEED_SETTINGS_RESOURCE"),
        SRC_("../cfg/feed_settings.schema.json"));
}

} // Y_UNIT_TEST_SUITE(helpers)
