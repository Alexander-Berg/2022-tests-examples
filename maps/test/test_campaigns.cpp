#include <maps/wikimap/mapspro/libs/sender/include/campaigns.h>
#include <maps/wikimap/mapspro/libs/sender/include/campaign_achieve_edits.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(campaigns_suite) {

Y_UNIT_TEST(sub_campaign_achieve_edits_parsing)
{
    const auto jsonVal = json::Value::fromString(R"({
        "nick": "5",
        "edits": [5],
        "slugs": [
            {"lang": "ru", "value": "8UDTCFK3-GYC"},
            {"lang": "en", "value": "OKUVCFK3-LYS1"}
        ]
    })");

    auto subCampaign = sender::SubCampaignAchieveEdits::fromJson(jsonVal);
    UNIT_ASSERT_EQUAL(subCampaign.nick, "5");
    UNIT_ASSERT_EQUAL(subCampaign.edits, std::vector<int>({5}));
    UNIT_ASSERT_EQUAL(subCampaign.slugs.getForLang("ru"), "8UDTCFK3-GYC");
    UNIT_ASSERT_EQUAL(subCampaign.slugs.getForLang("en"), "OKUVCFK3-LYS1");
}

Y_UNIT_TEST(campaign_achieve_edits_parsing)
{
    const auto jsonVal = json::Value::fromString(R"({
        "name": "achieve-edits-count",
        "sub-campaigns": [
            {
                "nick": "5",
                "edits": [5],
                "slugs": [
                    {"lang": "ru", "value": "5_ru_slug"},
                    {"lang": "en", "value": "5_en_slug"}
                ]
            },
            {
                "nick": "10-500",
                "edits": [10, 50, 100, 500],
                "slugs": [
                    {"lang": "ru", "value": "10_500_ru_slug"},
                    {"lang": "en", "value": "10_500_en_slug"}
                ]
            }
        ]
    })");

    auto campaign = sender::CampaignAchieveEdits::fromJson(jsonVal);
    UNIT_ASSERT_EQUAL(campaign.getName(), "achieve-edits-count");

    UNIT_ASSERT_EQUAL(campaign.getCampaignSlug(5, "ru"), "5_ru_slug");
    UNIT_ASSERT_EXCEPTION(campaign.getCampaignSlug(6, "ru"), sender::NotExistingCampaignAchieveEditsError);

    UNIT_ASSERT_EQUAL(campaign.getCampaignSlug(10, "en"), "10_500_en_slug");
    UNIT_ASSERT_EQUAL(campaign.getCampaignSlug(100, "ru"), "10_500_ru_slug");
    UNIT_ASSERT_EXCEPTION(campaign.getCampaignSlug(10, "hy"), sender::NotExistingCampaignAchieveEditsError);
}

} // campaigns_suite

} //namespace maps::wiki::tests
