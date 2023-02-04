#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/adv_machine/entities/banner.h>


float TEST_MAX_BID = 3.1;
float TEST_MEAN_BID = 43.2;
float TEST_MEAN_ADJUSTED_BID = 5.2;
float TEST_RSYA_MAX_BID = 5.1;
float TEST_RSYA_MEAN_BID = 6.5;
float TEST_RSYA_MEAN_ADJUSTED_BID = 7.2;
float TEST_MAX_DYNAMIC_BID = 3.4;
float TEST_MAX_SMART_BID = 7.6;
float TEST_RELEVANCE_MATCH_BID = 2.3;
float TEST_RELEVANCE_MATCHRSYA_BID = 7.6;
float TEST_GOOD_SEARCH_BID = 5.6;
float TEST_GOOD_RSYA_BID = 8.9;
float TEST_OFFER_MAX_BID = 1.2;

bool TEST_IS_DYNAMIC = true;
bool TEST_IS_AUTO_BUDGET_ENABLED = false;
bool TEST_IS_URL_TURBO = false;

float TEST_TOP_CLICKS_FRACTION = 0.3;
float TEST_TOP_COST_FRACTION_2 = 0.6;

bool TEST_HAS_PICTRUE = true;
ui32 TEST_IMAGE_WIDTH = 123;
ui32 TEST_IMAGE_HEIGHT_FIXED = 456;

TVector<float> TEST_TOP_REQUESTS_FRACTIONS = {0.235, 0.123, 0.4};

ui32 TEST_DEVICE_TYPE_COEF = 536;
ui32 TEST_SOCDEM_MALE_0_17 = 436;
ui32 TEST_SOCDEM_FEMALE_25_34 = 435;
ui32 TEST_SOCDEM_UNK_45 = 987;

float TEST_SUM_COST = 123.45;
TVector<std::tuple<ui64, ui64, float, float, float>> TEST_BANNER_STATS = {
    std::make_tuple(1, 2, 3.4, 5.6, 7.8), std::make_tuple(2, 3, 4.4, 3.6, 73.8),
    std::make_tuple(3, 4, 5.4, 5.5, 71.8), std::make_tuple(4, 5, 9.4, 5.3, 75.8),
    std::make_tuple(5, 6, 8.4, 5.1, 7.88), std::make_tuple(6, 34, 7.4, 6.6, 7.98),
    std::make_tuple(7, 8, 0.4, 8.6, 75.8), std::make_tuple(8, 23, 1.4, 0.6, 57.8),
};

TVector<std::tuple<float, float, float, float, float>> TEST_SEARCH_PHRASE_STATS = {
    std::make_tuple(11.3, 25.6, 4.4, 1.6, 2.8), std::make_tuple(12.1, 3.3, 54.4, 3.76, 3.8),
};
TVector<std::tuple<float, float, float, float, float>> TEST_RSYA_PHRASE_STATS = {
    std::make_tuple(11.3, 25.6, 4.4, 1.6, 2.8), std::make_tuple(12.1, 3.3, 54.4, 3.76, 3.8),
    std::make_tuple(12.1, 3.3, 54.4, 3.76, 3.8), std::make_tuple(12.1, 3.3, 54.4, 3.76, 3.8),
    std::make_tuple(12.1, 3.3, 54.4, 3.76, 3.8), std::make_tuple(12.1, 3.3, 54.4, 3.76, 3.8),
};


void assertEqualVectors(TVector<float> a, TVector<float> b) {
    UNIT_ASSERT_EQUAL_C(a.size(), b.size(), TStringBuilder{} << a.size() << " != " << b.size());
    for (ui32 i = 0; i < a.size(); ++i) {
        UNIT_ASSERT_DOUBLES_EQUAL_C(a[i], b[i], 0.001, TStringBuilder{} << "a[" << i << "] = " << a[i] << " != " << b[i] << " = b[" << i << "]");
    }
}

class TBannerEntityRVFeaturesTest : public TTestBase {
    public:
        void TestBidFactors() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::BidFactors, "BidFactors");
            assertEqualVectors(
                Features[NAdvMachineTsar::TBanner::BidFactors],
                {TEST_MAX_BID, TEST_MEAN_BID, TEST_MEAN_ADJUSTED_BID, TEST_RSYA_MAX_BID, TEST_RSYA_MEAN_BID,
                TEST_RSYA_MEAN_ADJUSTED_BID, TEST_MAX_DYNAMIC_BID, TEST_MAX_SMART_BID, TEST_RELEVANCE_MATCH_BID,
                TEST_RELEVANCE_MATCHRSYA_BID, TEST_GOOD_SEARCH_BID, TEST_GOOD_RSYA_BID, TEST_OFFER_MAX_BID}
            );
        }

        void TestFlagFactors() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::FlagFactors, "FlagFactors");

            UNIT_ASSERT_EQUAL(Features[NAdvMachineTsar::TBanner::FlagFactors].size(), 23);

            UNIT_ASSERT_EQUAL(Features[NAdvMachineTsar::TBanner::FlagFactors][0], static_cast<float>(TEST_IS_DYNAMIC));
            UNIT_ASSERT_EQUAL(Features[NAdvMachineTsar::TBanner::FlagFactors][4], static_cast<float>(TEST_IS_AUTO_BUDGET_ENABLED));
            UNIT_ASSERT_EQUAL(Features[NAdvMachineTsar::TBanner::FlagFactors][22], static_cast<float>(TEST_IS_URL_TURBO));

            for (ui32 i = 0; i < Features[NAdvMachineTsar::TBanner::FlagFactors].size(); ++i) {
                if (i != 0 && i != 4 && i != 22) {
                    UNIT_ASSERT_EQUAL(Features[NAdvMachineTsar::TBanner::FlagFactors][i], -1);
                }
            }
        }

        void TestTopFractionsFactors() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::TopFractionsFactors, "TopFractionsFactors");
            assertEqualVectors(
                Features[NAdvMachineTsar::TBanner::TopFractionsFactors],
                {TEST_TOP_CLICKS_FRACTION, -1, -1,
                -1, TEST_TOP_COST_FRACTION_2, -1}
            );
        }

        void TestImageFactors() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::ImageFactors, "ImageFactors");
            assertEqualVectors(
                Features[NAdvMachineTsar::TBanner::ImageFactors],
                {static_cast<float>(TEST_HAS_PICTRUE), static_cast<float>(TEST_IMAGE_WIDTH), static_cast<float>(TEST_IMAGE_HEIGHT_FIXED)}
            );
        }

        void TestTopRequestsFractions() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::TopRequestsFractions, "TopRequestsFractions");
            for (ui32 i = 0; i < TEST_TOP_REQUESTS_FRACTIONS.size(); ++i) {
                UNIT_ASSERT_EQUAL(Features[NAdvMachineTsar::TBanner::TopRequestsFractions][i], TEST_TOP_REQUESTS_FRACTIONS[i]);
            }
            for (ui32 i = TEST_TOP_REQUESTS_FRACTIONS.size(); i < NAdvMachineTsar::TBanner::TopRequestsCount; ++i) {
                UNIT_ASSERT_EQUAL(Features[NAdvMachineTsar::TBanner::TopRequestsFractions][i], 0);
            }
        }

        void TestCoefs() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::Coefs, "Coefs");
            assertEqualVectors(
                Features[NAdvMachineTsar::TBanner::Coefs],
                {
                    static_cast<float>(TEST_DEVICE_TYPE_COEF),
                    -1, -1, static_cast<float>(TEST_SOCDEM_FEMALE_25_34), -1, -1, -1,
                    static_cast<float>(TEST_SOCDEM_MALE_0_17), -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, static_cast<float>(TEST_SOCDEM_UNK_45), -1,
                }
            );
        }

        void TestStatistics() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::Statistics, "Statistics");
            TVector<float> expectedStatistics({TEST_SUM_COST});
            for (auto [shows, clicks, meanPClick, meanBid, sumCost]: TEST_BANNER_STATS) {
                expectedStatistics.push_back(static_cast<float>(clicks));
                expectedStatistics.push_back(meanBid);
                expectedStatistics.push_back(meanPClick);
                expectedStatistics.push_back(static_cast<float>(shows));
                expectedStatistics.push_back(sumCost);
            }
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::Statistics], expectedStatistics);
        }

        void TestSearchPhrasesStats() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::SearchPhrasesStats, "SearchPhrasesStats");
            TVector<float> expectedStatistics;
            for (auto [adjustedBid, bid, cpc, clicks, shows]: TEST_SEARCH_PHRASE_STATS) {
                expectedStatistics.push_back(adjustedBid);
                expectedStatistics.push_back(bid);
                expectedStatistics.push_back(cpc);
                expectedStatistics.push_back(clicks);
                expectedStatistics.push_back(shows);
            }
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhrasesCount, 5);
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhraseStatsCount, 5); // len([adjustedBid, bid, cpc, clicks, shows])
            expectedStatistics.resize(NAdvMachineTsar::TBanner::PhrasesCount * NAdvMachineTsar::TBanner::PhraseStatsCount);
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::SearchPhrasesStats], expectedStatistics);
        }
        void TestSearchPhrasesAggregatedStats() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::SearchPhrasesAggregatedStats, "SearchPhrasesAggregatedStats");
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhraseStatsCount, 5); // len([adjustedBid, bid, cpc, clicks, shows])
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhraseAggregatedStatsCount, 4); // len([max, min, mean, std])
            UNIT_ASSERT_EQUAL(
                Features[NAdvMachineTsar::TBanner::SearchPhrasesAggregatedStats].size(),
                NAdvMachineTsar::TBanner::PhraseStatsCount * NAdvMachineTsar::TBanner::PhraseAggregatedStatsCount + 1
            );

            TVector<float> clicksVec, showsVec, cpcVec, bidVec, adjustedBidVec;
            for (auto [adjustedBid, bid, cpc, clicks, shows]: TEST_SEARCH_PHRASE_STATS) {
                clicksVec.push_back(clicks);
                showsVec.push_back(shows);
                cpcVec.push_back(cpc);
                bidVec.push_back(bid);
                adjustedBidVec.push_back(adjustedBid);
            }

            TVector<float> expectedAggregatedValues({static_cast<float>(TEST_SEARCH_PHRASE_STATS.size())});
            for (auto vec: {adjustedBidVec, bidVec, cpcVec, clicksVec, showsVec}) {
                float maxElem = 0, minElem = 1000, mean = 0, std = 0;
                for (float x: vec) {
                    if (x > maxElem) { maxElem = x; }
                    if (x < minElem) { minElem = x; }
                    mean += x;
                }
                mean /= vec.size();
                for (float x: vec) {
                    std += (x - mean) * (x - mean);
                }
                std /= vec.size();
                expectedAggregatedValues.push_back(maxElem);
                expectedAggregatedValues.push_back(minElem);
                expectedAggregatedValues.push_back(mean);
                expectedAggregatedValues.push_back(std);
            }
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::SearchPhrasesAggregatedStats], expectedAggregatedValues);
        }
        void TestRsyaPhrasesStats() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::RsyaPhrasesStats, "RsyaPhrasesStats");
            TVector<float> expectedStatistics;
            for (auto [adjustedBid, bid, cpc, clicks, shows]: TEST_RSYA_PHRASE_STATS) {
                expectedStatistics.push_back(adjustedBid);
                expectedStatistics.push_back(bid);
                expectedStatistics.push_back(cpc);
                expectedStatistics.push_back(clicks);
                expectedStatistics.push_back(shows);
            }
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhrasesCount, 5);
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhraseStatsCount, 5); // len([adjustedBid, bid, cpc, clicks, shows])
            expectedStatistics.resize(NAdvMachineTsar::TBanner::PhrasesCount * NAdvMachineTsar::TBanner::PhraseStatsCount);
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::RsyaPhrasesStats], expectedStatistics);
        }
        void TestRsyaPhrasesAggregatedStats() {
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::RsyaPhrasesAggregatedStats, "RsyaPhrasesAggregatedStats");
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhraseStatsCount, 5); // len([adjustedBid, bid, cpc, clicks, shows])
            UNIT_ASSERT_EQUAL(NAdvMachineTsar::TBanner::PhraseAggregatedStatsCount, 4); // len([max, min, mean, std])
            UNIT_ASSERT_EQUAL(
                Features[NAdvMachineTsar::TBanner::RsyaPhrasesAggregatedStats].size(),
                NAdvMachineTsar::TBanner::PhraseStatsCount * NAdvMachineTsar::TBanner::PhraseAggregatedStatsCount + 1
            );

            TVector<float> clicksVec, showsVec, cpcVec, bidVec, adjustedBidVec;
            for (auto [adjustedBid, bid, cpc, clicks, shows]: TEST_RSYA_PHRASE_STATS) {
                clicksVec.push_back(clicks);
                showsVec.push_back(shows);
                cpcVec.push_back(cpc);
                bidVec.push_back(bid);
                adjustedBidVec.push_back(adjustedBid);
            }

            TVector<float> expectedAggregatedValues({static_cast<float>(TEST_RSYA_PHRASE_STATS.size())});
            for (auto vec: {adjustedBidVec, bidVec, cpcVec, clicksVec, showsVec}) {
                float maxElem = 0, minElem = 1000, mean = 0, std = 0;
                for (float x: vec) {
                    if (x > maxElem) { maxElem = x; }
                    if (x < minElem) { minElem = x; }
                    mean += x;
                }
                mean /= vec.size();
                for (float x: vec) {
                    std += (x - mean) * (x - mean);
                }
                std /= vec.size();
                expectedAggregatedValues.push_back(maxElem);
                expectedAggregatedValues.push_back(minElem);
                expectedAggregatedValues.push_back(mean);
                expectedAggregatedValues.push_back(std);
            }
            assertEqualVectors(Features[NAdvMachineTsar::TBanner::RsyaPhrasesAggregatedStats], expectedAggregatedValues);
        }

        void SetUp() override {
            NAdvMachine::TBannerIndexProfile profile;

            // BidFactors
            profile.SetMaxBid(TEST_MAX_BID);
            profile.SetMeanBid(TEST_MEAN_BID);
            profile.SetMeanAdjustedBid(TEST_MEAN_ADJUSTED_BID);
            profile.SetRsyaMaxBid(TEST_RSYA_MAX_BID);
            profile.SetRsyaMeanBid(TEST_RSYA_MEAN_BID);
            profile.SetRsyaMeanAdjustedBid(TEST_RSYA_MEAN_ADJUSTED_BID);
            profile.SetMaxDynamicBid(TEST_MAX_DYNAMIC_BID);
            profile.SetMaxSmartBid(TEST_MAX_SMART_BID);
            profile.SetRelevanceMatchBid(TEST_RELEVANCE_MATCH_BID);
            profile.SetRelevanceMatchRsyaBid(TEST_RELEVANCE_MATCHRSYA_BID);
            profile.SetGoodSearchBid(TEST_GOOD_SEARCH_BID);
            profile.SetGoodRsyaBid(TEST_GOOD_RSYA_BID);
            profile.SetOfferMaxBid(TEST_OFFER_MAX_BID);

            // FlagFactors
            profile.SetIsDynamic(TEST_IS_DYNAMIC);
            profile.SetIsAutoBudgetEnabled(TEST_IS_AUTO_BUDGET_ENABLED);

            // TopFractionsFactors
            profile.SetTopClicksFraction(TEST_TOP_CLICKS_FRACTION);
            profile.SetTopCostFraction2(TEST_TOP_COST_FRACTION_2);

            // ImageFactors
            profile.SetHasPicture(TEST_HAS_PICTRUE);
            profile.SetImageWidth(TEST_IMAGE_WIDTH);
            profile.SetImageHeightFixed(TEST_IMAGE_HEIGHT_FIXED);

            // Statistics
            profile.SetSumCost(TEST_SUM_COST);
            TVector<NAdvMachine::TBannerStats*> bannerStats = {
                profile.MutableBannerRsyaStats(), profile.MutableOrderRsyaStats(),
                profile.MutableDomainRsyaStats(), profile.MutableBannerGuaranteeStats(),
                profile.MutableOrderPremiumStats(), profile.MutableBannerPremiumStats(),
                profile.MutableDomainPremiumStats(), profile.MutableDomainGuaranteeStats(),
            };
            UNIT_ASSERT_EQUAL(bannerStats.size(), TEST_BANNER_STATS.size());
            for (ui32 i = 0; i < bannerStats.size(); ++i) {
                auto [shows, clicks, meanPClick, meanBid, sumCost] = TEST_BANNER_STATS[i];
                bannerStats[i]->SetShows(shows);
                bannerStats[i]->SetClicks(clicks);
                bannerStats[i]->SetMeanPClick(meanPClick);
                bannerStats[i]->SetMeanBid(meanBid);
                bannerStats[i]->SetSumCost(sumCost);
            }

            // SearchPhrasesStats SearchPhrasesAggregatedStats
            for (auto [adjustedBid, bid, cpc, clicks, shows]: TEST_SEARCH_PHRASE_STATS) {
                auto phrase = profile.AddSearchPhrases();
                phrase->SetShows(shows);
                phrase->SetClicks(clicks);
                phrase->SetBid(bid);
                phrase->SetAdjustedBid(adjustedBid);
                phrase->SetCPC(cpc);
            }
            for (auto [adjustedBid, bid, cpc, clicks, shows]: TEST_RSYA_PHRASE_STATS) {
                auto phrase = profile.AddRsyaPhrases();
                phrase->SetShows(shows);
                phrase->SetClicks(clicks);
                phrase->SetBid(bid);
                phrase->SetAdjustedBid(adjustedBid);
                phrase->SetCPC(cpc);
            }

            // TopRequestsFractions
            for (auto fraction: TEST_TOP_REQUESTS_FRACTIONS) {
                auto topRequest = profile.AddTopRequests();
                topRequest->SetFraction(fraction);
            }

            // Coefs
            profile.MutableDeviceTypeCoef()->SetMobileCoef(TEST_DEVICE_TYPE_COEF);
            auto coef = profile.MutableSocdemCoef();
            coef->MutableGenderFemale()->SetAgeFrom25To34(TEST_SOCDEM_FEMALE_25_34);
            coef->MutableGenderMale()->SetAgeFrom0To17(TEST_SOCDEM_MALE_0_17);
            coef->MutableGenderUnknown()->SetAgeFrom45(TEST_SOCDEM_UNK_45);

            Features = NAdvMachineTsar::TBanner(profile).GetRealvalueFeatures();
        }

    private:
        THashMap<TString, TVector<float>> Features;

        UNIT_TEST_SUITE(TBannerEntityRVFeaturesTest);

        UNIT_TEST(TestBidFactors);
        UNIT_TEST(TestFlagFactors);
        UNIT_TEST(TestTopFractionsFactors);
        UNIT_TEST(TestImageFactors);
        UNIT_TEST(TestTopRequestsFractions);
        UNIT_TEST(TestCoefs);
        UNIT_TEST(TestStatistics);
        UNIT_TEST(TestSearchPhrasesStats);
        UNIT_TEST(TestSearchPhrasesAggregatedStats);
        UNIT_TEST(TestRsyaPhrasesStats);
        UNIT_TEST(TestRsyaPhrasesAggregatedStats)

        UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBannerEntityRVFeaturesTest);
