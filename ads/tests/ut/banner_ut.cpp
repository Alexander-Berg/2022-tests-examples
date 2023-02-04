#include <ads/tensor_transport/lib_2/banner.h>
#include <ads/tensor_transport/lib_2/banner_features.h>
#include <ads/tensor_transport/proto/banner.pb.h>
#include <library/cpp/testing/unittest/registar.h>

#include <algorithm>

using namespace NTsarTransport;

ui64 TEST_BANNER_ID = 14;
ui64 TEST_GROUP_BANNER_ID = 23;
ui64 TEST_PRODUCT_TYPE = 1;
ui64 TEST_ORDER_ID = 52;
const char* TEST_URL = "https://www.crazyforcows.com/gift/cow-t-shirts?cowID=10&cowColor=blue";
const char* TEST_PAGE_TITLE = "Crazy for cows";
const char* TEST_BANNER_TITLE = "Пергамент и подпергамент";
const char* TEST_BANNER_TEXT = "Цена от 200р/кг. 17 вкусов. Доставка от 50кг 0р! Образцы бесплатно! Звоните сейчас!";
const ui64 TEST_TARGET_DOMAIN_ID = 200;
const char* TEST_CATEGORIES = "1 2 3 4";
int TEST_MAX_PATH_DEPTH = 10;
int TEST_MAX_QUERY_PARAMS = 12;
int TEST_MAX_TITLE_WORDS = 50;
THashMap<TString, THashSet<TString>> LEMMER_FIXLIST;


class TTestBanner : public TBanner {
    friend class TBannerEntityTest;

    TTestBanner(
        ui64 bannerID,
        ui64 groupBannerID,
        ui64 productType,
        ui64 orderID,
        TStringBuf landingURL,
        TStringBuf landingPageTitle,
        TStringBuf bannerText,
        TStringBuf bannerTitle,
        ui64 targetDomainID,
        TStringBuf categories,
        int maxPathDepth,
        int maxQueryParams,
        int maxTitleWords,
        const THashMap<TString, THashSet<TString>>& lemmerFixlist
    ) : TBanner(
        bannerID,
        groupBannerID,
        productType,
        orderID,
        landingURL,
        landingURL,
        landingPageTitle,
        bannerText,
        bannerTitle,
        targetDomainID,
        categories,
        maxPathDepth,
        maxQueryParams,
        maxTitleWords,
        lemmerFixlist
    )
    {}
};


class TBannerEntityTest : public TTestBase {
public:
    UNIT_TEST_SUITE(TBannerEntityTest);
    UNIT_TEST(TestBannerID);
    UNIT_TEST(TestGroupBannerID);
    UNIT_TEST(TestOrderID);
    UNIT_TEST(TestProductType);
    UNIT_TEST(TestTargetDomainID);
    UNIT_TEST(TestGetBigrams);
    UNIT_TEST(TestBannerBMCategoryID);
    UNIT_TEST(TestLandingURLPathH);
    UNIT_TEST(TestLandingURLNetLocPathH);
    UNIT_TEST(TestLandingPageTitleLemmaH);
    UNIT_TEST(TestBannerTitleLemmaH);
    UNIT_TEST(TestBannerTextLemmaH);
    UNIT_TEST(TestLandingURLQueryH);
    UNIT_TEST(TestLandingPageTitleLemmaHBigrams);
    UNIT_TEST(TestBannerTitleLemmaHBigrams);
    UNIT_TEST(TestBannerTextLemmaHBigrams);
    UNIT_TEST_SUITE_END();

    void TestGetBigrams() {
        UNIT_ASSERT_EQUAL(TVector<TString>(), GetBigrams({}));
        UNIT_ASSERT_EQUAL(TVector<TString>(), GetBigrams({"a"}));
        UNIT_ASSERT_EQUAL(TVector<TString>({"2048923011483675113"}), GetBigrams({"a", "b"}));
        UNIT_ASSERT_EQUAL(TVector<TString>({"13298049566834512065", "4379590064192378333"}), GetBigrams({"a", "bwjvdfm", "g2k4m5"}));
    }

    void TestBannerID() {
        TVector<TString> expectedValues = {"14"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::BannerID);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestGroupBannerID() {
        TVector<TString> expectedValues = {"23"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::GroupBannerID);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestOrderID() {
        TVector<TString> expectedValues = {"52"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::OrderID);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestProductType() {
        TVector<TString> expectedValues = {"1"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::ProductType);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestTargetDomainID() {
        TVector<TString> expectedValues = {"200"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::TargetDomainID);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }


    void TestBannerBMCategoryID() {
        TVector<TString> expectedValues = {"1", "2", "3"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::BannerBMCategoryID);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestLandingURLPathH() {
        TVector<TString> expectedValues = {"7884767166006269784", "17587098399798077100"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::LandingURLPathH);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestLandingURLNetLocPathH() {
        TVector<TString> expectedValues = {"16079705689044127687", "11755656148298533504"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::LandingURLNetLocPathH);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestLandingPageTitleLemmaH() {
        TVector<TString> expectedValues = {"12559273869791589744", "16610355636440591213", "6182712327821009870"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::LandingPageTitleLemmaH);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestBannerTitleLemmaH() {
        TVector<TString> expectedValues = {"6411042379361363472", "7203773044700119076"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::BannerTitleLemmaH);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestBannerTextLemmaH() {
        TVector<TString> expectedValues = {"6365795080346080926", "1083047737773150478", "3932373671883736807", "9576196842960452701", "8385095066901342162", "9006137760987575179", "5493227220034031740", "18241671912579855054", "4322088304598153290", "2750728449553910726", "16079790501152756062", "2697548030495225985"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::BannerTextLemmaH);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestLandingURLQueryH() {
        TVector<TString> expectedValues = {"10018195066857326801", "15057704467372224054"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::LandingURLQueryH);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestLandingPageTitleLemmaHBigrams() {
        TVector<TString> expectedValues = {"12637642859963005825", "12952615323683616885"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::LandingPageTitleLemmaHBigrams);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestBannerTitleLemmaHBigrams() {
        TVector<TString> expectedValues = {"12031686543353651912"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::BannerTitleLemmaHBigrams);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }

    void TestBannerTextLemmaHBigrams() {
        TVector<TString> expectedValues = {"9302921436745669735", "11591518501704244998", "7632402975663103284", "12151859085132643032", "2021074423380529191", "8011467767408949719", "17892207032796367320", "18192700126539957470", "1421147342102922339", "17717376568714146775", "10737794937227348675"};
        auto currentValues = Banner.GetFeatureByIndex(EBannerFeatureIndex::BannerTextLemmaHBigrams);
        UNIT_ASSERT_EQUAL(expectedValues, currentValues);
    }


    TBannerEntityTest()
    : Banner(
        TEST_BANNER_ID,
        TEST_GROUP_BANNER_ID,
        TEST_PRODUCT_TYPE,
        TEST_ORDER_ID,
        TEST_URL,
        TEST_PAGE_TITLE,
        TEST_BANNER_TEXT,
        TEST_BANNER_TITLE,
        TEST_TARGET_DOMAIN_ID,
        TEST_CATEGORIES,
        TEST_MAX_PATH_DEPTH,
        TEST_MAX_QUERY_PARAMS,
        TEST_MAX_TITLE_WORDS,
        LEMMER_FIXLIST
        )
    {}

private:
    TTestBanner Banner;
};

UNIT_TEST_SUITE_REGISTRATION(TBannerEntityTest);
