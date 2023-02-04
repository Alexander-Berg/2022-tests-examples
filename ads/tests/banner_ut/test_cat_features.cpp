#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/adv_machine/entities/banner.h>


ui64 TEST_BANNER_ID = 14;
ui64 TEST_ORDER_ID = 52;
ui64 TEST_TARGET_DOMAIN = 123;
ui64 TEST_MAIN_REGION = 453;
TVector<ui64> TEST_CATEGORIES = {1, 23, 4, 5};

TString TEST_BANNER_TITLE = "кастрюля нержавеющая сталь 20 л";
TString TEST_BANNER_TEXT = "для кафе бара столовой ресторана большой ассортимент доставка по рб";
TString TEST_LANDING_PAGE_TITLE = "котел 20 0л professional тм appetite купить";
TString TEST_UTA_URL = "fideliatorg posuda posuda iz nerzhaveyushchej stali kotel 0l professional tm appetite";
TString TEST_TOP_COST_PHRASE = "20 кастрюля л нерж";
TString TEST_TOP_COST_PHRASE_2 = "20 кастрюля л нержавейка";
TString TEST_TOP_COST_PHRASE_3 = "20 кастрюля л нержавеющая сталь";
TString TEST_TOP_CLICKS_PHRASE = "20 кастрюля л нерж";
TString TEST_TOP_CLICKS_PHRASE_2 = "20 кастрюля л нержавейка";
TString TEST_TOP_CLICKS_PHRASE_3 = "20 кастрюля л";

TVector<ui64> TEST_SEARCH_PHRASE_IDS = {43, 67, 23};
TVector<ui64> TEST_RSYA_PHRASE_IDS = {654, 34, 23, 65, 76, 12, 32, 54, 67};

TVector<TString> TEST_TOP_REQUEST_TEXTS = {"кастрюля нержавейка 20 л", "кастрюля нержавеющая сталь 20 л"};

// TString TEST_REGION_PATTERN = "225 and ~1 and ~10174";
TVector<ui64> TEST_ALLOWED_REGIONS = {225};
TVector<ui64> TEST_BANNED_REGIONS = {1, 10174};


void assertEqualVectors(TVector<ui64> a, TVector<ui64> b) {
    UNIT_ASSERT_EQUAL_C(a.size(), b.size(), TStringBuilder{} << a.size() << " != " << b.size());
    for (ui32 i = 0; i < a.size(); ++i) {
        UNIT_ASSERT_EQUAL_C(a[i], b[i], TStringBuilder{} << "a[" << i << "] = " << a[i] << " != " << b[i] << " = b[" << i << "]");
    }
}


class TBannerEntityCategoricalFeaturesTest : public TTestBase {
    public:
        void TestBannerID() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::BannerID, "BannerID");
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::BannerID], {TEST_BANNER_ID});
        }
        void TestOrderID() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::OrderID, "OrderID");
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::OrderID], {TEST_ORDER_ID});
        }
        void TestTargetDomainID() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::TargetDomainID, "TargetDomainID");
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::TargetDomainID], {TEST_TARGET_DOMAIN});
        }
        void TestBannerCategories() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::BannerCategories, "BannerCategories");
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::BannerCategories], TEST_CATEGORIES);
        }
        void TestMainRegion() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::MainRegion, "MainRegion");
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::MainRegion], {TEST_MAIN_REGION});
        }

        void TestSearchPhrasesIDs() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::SearchPhrasesIDs, "SearchPhrasesIDs");
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhrasesCount, 5);
            TEST_SEARCH_PHRASE_IDS.resize(NAdvMachineTsar::TBanner::PhrasesCount);
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::SearchPhrasesIDs], TEST_SEARCH_PHRASE_IDS);
        }
        void TestRsyaPhrasesIDs() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::RsyaPhrasesIDs, "RsyaPhrasesIDs");
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhrasesCount, 5);
            TEST_RSYA_PHRASE_IDS.resize(NAdvMachineTsar::TBanner::PhrasesCount);
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::RsyaPhrasesIDs], TEST_RSYA_PHRASE_IDS);
        }

        void TestTopRequests() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::TopRequests, "TopRequests");
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::TopRequestsCount, 10);
            for (ui32 i = 0; i < TEST_TOP_REQUEST_TEXTS.size(); ++i) {
                UNIT_ASSERT(Features[TStringBuilder{} << NAdvMachineTsar::TBanner::TopRequests << i << "TokenLemma"].size() > 0);
            }
            for (ui32 i = TEST_TOP_REQUEST_TEXTS.size(); i < NAdvMachineTsar::TBanner::TopRequestsCount; ++i) {
                UNIT_ASSERT(Features[TStringBuilder{} << NAdvMachineTsar::TBanner::TopRequests << i << "TokenLemma"].size() == 0);
            }
        }

        void TestRegionPattern() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::AllowedRegions, "AllowedRegions");
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::BannedRegions, "BannedRegions");

            assertEqualVectors(Features[NAdvMachineTsar::TBanner::AllowedRegions], TEST_ALLOWED_REGIONS);
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::BannedRegions], TEST_BANNED_REGIONS);
        }

        void SetUp() override {
            NAdvMachine::TBannerIndexProfile profile;

            profile.SetBannerID(ToString(TEST_BANNER_ID));
            profile.SetOrderID(ToString(TEST_ORDER_ID));
            profile.SetTargetDomainID(ToString(TEST_TARGET_DOMAIN));
            for (auto cat: TEST_CATEGORIES) {profile.AddBannerCategories(cat);}
            profile.SetMainRegion(TEST_MAIN_REGION);

            profile.SetBannerTitle(TEST_BANNER_TITLE);
            profile.SetBannerText(TEST_BANNER_TEXT);
            profile.SetLandingPageTitle(TEST_LANDING_PAGE_TITLE);
            profile.SetUtaURL(TEST_UTA_URL);
            profile.SetTopCostPhrase(TEST_TOP_COST_PHRASE);
            profile.SetTopCostPhrase2(TEST_TOP_COST_PHRASE_2);
            profile.SetTopCostPhrase3(TEST_TOP_COST_PHRASE_3);
            profile.SetTopClicksPhrase(TEST_TOP_CLICKS_PHRASE);
            profile.SetTopClicksPhrase2(TEST_TOP_CLICKS_PHRASE_2);
            profile.SetTopClicksPhrase3(TEST_TOP_CLICKS_PHRASE_3);

            for (auto id: TEST_SEARCH_PHRASE_IDS) {
                auto phraseStats = profile.AddSearchPhrases();
                phraseStats->SetPhraseID(id);
            }
            for (auto id: TEST_RSYA_PHRASE_IDS) {
                auto phraseStats = profile.AddRsyaPhrases();
                phraseStats->SetPhraseID(id);
            }

            for (auto text: TEST_TOP_REQUEST_TEXTS) {
                auto topRequest = profile.AddTopRequests();
                topRequest->SetText(text);
            }

            TString regionPattern = ToString(TEST_ALLOWED_REGIONS[0]);
            for (ui32 i = 1; i < TEST_ALLOWED_REGIONS.size(); ++i) {
                regionPattern += TStringBuilder{} << " and " << TEST_ALLOWED_REGIONS[i];
            }
            for (ui32 i = 0; i < TEST_BANNED_REGIONS.size(); ++i) {
                regionPattern += TStringBuilder{} << " and  ~" << TEST_BANNED_REGIONS[i];
            }
            profile.SetRegionPattern(regionPattern);

            Features = NAdvMachineTsar::TBanner(profile).GetNamedFeatures();
        }

    private:
        THashMap<TString, TVector<ui64>> Features;

        UNIT_TEST_SUITE(TBannerEntityCategoricalFeaturesTest);

        UNIT_TEST(TestBannerID);
        UNIT_TEST(TestOrderID);
        UNIT_TEST(TestTargetDomainID);
        UNIT_TEST(TestBannerCategories);
        UNIT_TEST(TestMainRegion);

        UNIT_TEST(TestSearchPhrasesIDs);
        UNIT_TEST(TestRsyaPhrasesIDs);
        
        UNIT_TEST(TestTopRequests);

        UNIT_TEST(TestRegionPattern);

        UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBannerEntityCategoricalFeaturesTest);
