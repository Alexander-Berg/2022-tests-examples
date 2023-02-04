#include <ads/bigkv/torch_v2/entities/banner.h>

#include <library/cpp/testing/unittest/registar.h>


ui64 TEST_BANNER_ID = 14;
ui64 TEST_ORDER_ID = 52;
ui64 TEST_TARGET_DOMAIN = 123;
TString TEST_BANNER_TEXT = "Цена от 200р/кг. 17 вкусов. Доставка от 50кг 0р! Образцы бесплатно! Звоните сейчас!";
TString TEST_BANNER_TITLE = "Пергамент и подпергамент";
TString TEST_PAGE_TITLE = "Crazy for cows";
TString TEST_LANDING_URL = "https://www.crazyforcows.com/gift/cow-t-shirts/asd?cowID=10&cowColor=blue&zcx=sdf";
TString TEST_URL = "https://www.crazyforcows.com/gift/cow-t-shirts?cowID=10&cowColor=blue";
TString TEST_CATEGORIES = "4 3 2 1";


void assertEqualVectors(TVector<ui64> a, TVector<ui64> b) {
    UNIT_ASSERT_EQUAL(a.size(), b.size());
    for (ui32 i = 0; i < a.size(); ++i) {
        UNIT_ASSERT_EQUAL(a[i], b[i]);
    }
}


class TBannerEntityTest : public TTestBase {
    public:
        void TestAllFeaturesExists() {
            for (auto featureName: {"BannerID", "BannerBMCategoryID", "OrderID", "TargetDomainID", "BannerTitleTokenLemma", "BannerTextTokenLemma", "LandingPageTitleTokenLemma", "BannerURLParsed", "LandingURLParsed"}) {
                UNIT_ASSERT(Features.contains(featureName));
            }
        }

        void TestBanneID() {
            assertEqualVectors(Features["BannerID"], {TEST_BANNER_ID});
        }

        void TestBannerBMCategoryID() {
            assertEqualVectors(Features["BannerBMCategoryID"], {1, 2, 3});
        }

        void TestOrderID() {
            assertEqualVectors(Features["OrderID"], {TEST_ORDER_ID});
        }

        void TestTargetDomainID() {
            assertEqualVectors(Features["TargetDomainID"], {TEST_TARGET_DOMAIN});
        }

        void TestBannerTitleTokenLemma() {
            assertEqualVectors(
                Features["BannerTitleTokenLemma"],
                {6411042379361363472ULL, 13713924249569698064ULL, 7203773044700119076ULL}
                // "пергамент", "и", "подпергамент",
            );
        }

        void TestBannerTextTokenLemma() {
            assertEqualVectors(
                Features["BannerTextTokenLemma"],
                {6365795080346080926ULL, 9150841784215469806ULL, 15993612075890497895ULL, 3932373671883736807ULL, 7545806668849854028ULL, 9576196842960452701ULL, 8385095066901342162ULL, 9006137760987575179ULL, 9150841784215469806ULL, 4322088304598153290ULL, 2750728449553910726ULL, 16079790501152756062ULL, 2697548030495225985ULL, 8432406357438071256ULL}
                // "цена", "от", "/", "кг", ".", "17", "вкус", "доставка", "от", "образец", "бесплатно", "звонить", "сейчас", "!"
            );
        }

        void TestLandingPageTitleTokenLemma() {
            assertEqualVectors(
                Features["LandingPageTitleTokenLemma"],
                {12559273869791589744ULL, 16610355636440591213ULL, 6182712327821009870ULL}
                // "crazy", "for", "cow"
            );
        }

        void TestBannerURLParsed() {
            assertEqualVectors(
                Features["BannerURLParsed"],
                {5961595332883608507ULL, 12361601375719815188ULL, 10723482059362140582ULL, 7884767166006269784ULL, 17587098399798077100ULL, 11531626468063151736ULL, 2996417674471781724ULL, 5901862007094604837ULL, 16744013792808808633ULL}
                // "www", "crazyforcows", "com", "gift", "cow-t-shirts", "cowID", "10", "cowColor", "blue",
            );
        }

        void TestLandingURLParsed() {
            assertEqualVectors(
                Features["LandingURLParsed"],
                {5961595332883608507ULL, 12361601375719815188ULL, 10723482059362140582ULL, 7884767166006269784ULL, 17587098399798077100ULL, 6409734634419312114ULL, 11531626468063151736ULL, 2996417674471781724ULL, 5901862007094604837ULL, 16744013792808808633ULL, 5640190733429915122ULL, 406358039206559643ULL}
                // "www", "crazyforcows", "com", "gift", "cow-t-shirts", "asd", "cowID", "10", "cowColor", "blue", "zcx", "sdf"
            );
        }

        void SetUp() override {
            auto banner = NTorchV2::TBanner(
                TEST_BANNER_ID, TEST_ORDER_ID, TEST_TARGET_DOMAIN,
                TEST_BANNER_TEXT, TEST_BANNER_TITLE, TEST_PAGE_TITLE,
                TEST_LANDING_URL, TEST_URL, TEST_CATEGORIES
            );
            Features = banner.GetNamedFeatures();
        }

    private:
        THashMap<TString, TVector<ui64>> Features;

        UNIT_TEST_SUITE(TBannerEntityTest);
        UNIT_TEST(TestAllFeaturesExists);
        UNIT_TEST(TestBanneID);
        UNIT_TEST(TestBannerBMCategoryID);
        UNIT_TEST(TestOrderID);
        UNIT_TEST(TestTargetDomainID);
        UNIT_TEST(TestBannerTitleTokenLemma);
        UNIT_TEST(TestBannerTextTokenLemma);
        UNIT_TEST(TestLandingPageTitleTokenLemma);
        UNIT_TEST(TestBannerURLParsed);
        UNIT_TEST(TestLandingURLParsed);
        UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBannerEntityTest);

Y_UNIT_TEST_SUITE(TBannerV1PrepreprocessorTests) {
    Y_UNIT_TEST(ParseEmptyBannerTest) {
        NTorchV2::TBannerV1Preprocessor preprocessor;

        const auto schema = preprocessor.Schema();
        const auto features = preprocessor.ParseProfile(NCSR::TBannerProfileProto(), {}, {});

        UNIT_ASSERT_EQUAL(schema.size(), features.size());
        for (const auto& [name, type] : schema) {
            UNIT_ASSERT(features.contains(name));
        }
    }
}
