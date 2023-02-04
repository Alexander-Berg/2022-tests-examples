#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/banner_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/banner_preprocessors/banner_rmp_preprocessor.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TBannerRmpPreprocessorsTests : public TTestBase {
public:
    void RmpFeaturesTest() {
        TBannerAppDataPreprocessor preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), 1e9+1234567);

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 13);
        UNIT_ASSERT(actualResult.contains("AppDataRegionName"));
        UNIT_ASSERT(actualResult.contains("AppDataLocaleName"));
        UNIT_ASSERT(actualResult.contains("AppDataBundleId"));
        UNIT_ASSERT(actualResult.contains("AppDataSourceID"));
        UNIT_ASSERT(actualResult.contains("AppDataBMCategories"));
        UNIT_ASSERT(actualResult.contains("AppDataContentAdvisoryRating"));
        UNIT_ASSERT(actualResult.contains("AppDataDescription"));
        UNIT_ASSERT(actualResult.contains("AppDataMinOsVersion"));
        UNIT_ASSERT(actualResult.contains("AppDataMobileInterests"));
        UNIT_ASSERT(actualResult.contains("AppDataTitle"));
        UNIT_ASSERT(actualResult.contains("AppDataVendorNameRaw"));
        UNIT_ASSERT(actualResult.contains("AppDataVendorWebsite"));
        UNIT_ASSERT(actualResult.contains("AppDataRv"));

        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataRegionName"].AsString(), "ru_reg");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataLocaleName"].AsString(), "ru");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataBundleId"].AsString(), "com.smth");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataSourceID"].AsUint64(), 123);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataBMCategories"].AsList()[0].AsUint64(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataBMCategories"].AsList()[1].AsUint64(), 2);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataContentAdvisoryRating"].AsUint64(), 5);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataDescription"].AsString(), "Some desc");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataMinOsVersion"].AsString(), "0.0.5");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataMobileInterests"].AsList()[0].AsUint64(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataMobileInterests"].AsList()[1].AsUint64(), 4);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataTitle"].AsString(), "Some title");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataVendorNameRaw"].AsString(), "MyVendor");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AppDataVendorWebsite"].AsString(), "my-vendor.ru");

        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[0].AsDouble(), 1, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[1].AsDouble(), 0, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[2].AsDouble(), 567, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[3].AsDouble(), 5, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[4].AsDouble(), 890123, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[5].AsDouble(), 45, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[6].AsDouble(), 67, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(actualResult["AppDataRv"].AsList()[7].AsDouble(), 1234567 - 12345, 0.0001);
    }

    void SetUp() override {
        ProfileBuilder.AddAppData(
            "ru_reg", "ru", "com.smth", 123,
            5, {1, 2}, {3, 4},
            "Some desc", "Some title", "0.0.5", "MyVendor", "my-vendor.ru",
            true, false, 567, 5, 890123, 45, 67, 1e9+12345
        );
    }

private:
    TBannerProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TBannerRmpPreprocessorsTests);
    UNIT_TEST(RmpFeaturesTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBannerRmpPreprocessorsTests);
}
