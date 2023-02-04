#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/banner_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/banner_preprocessors/base_fields_preprocessors.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TBannerBaseFieldsPreprocessorsTests : public TTestBase {
public:
    void BaseFieldsTest() {
        TBannerBaseFieldsPreprocessor preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 9);

        UNIT_ASSERT(actualResult.contains("BannerID"));
        UNIT_ASSERT(actualResult.contains("BannerBMCategoryID"));
        UNIT_ASSERT(actualResult.contains("OrderID"));
        UNIT_ASSERT(actualResult.contains("TargetDomainID"));
        UNIT_ASSERT(actualResult.contains("BannerTitle"));
        UNIT_ASSERT(actualResult.contains("BannerText"));
        UNIT_ASSERT(actualResult.contains("LandingPageTitle"));
        UNIT_ASSERT(actualResult.contains("BannerURL"));
        UNIT_ASSERT(actualResult.contains("LandingURL"));

        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerID"].AsUint64(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["OrderID"].AsUint64(), 2);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["TargetDomainID"].AsUint64(), 3);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerBMCategoryID"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerBMCategoryID"].AsList()[0].AsUint64(), 4);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerBMCategoryID"].AsList()[1].AsUint64(), 5);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerBMCategoryID"].AsList()[2].AsUint64(), 6);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerTitle"].AsString(), "banner title");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerText"].AsString(), "banner text");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["LandingPageTitle"].AsString(), "landing page title");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["LandingURL"].AsString(), "landing url");
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BannerURL"].AsString(), "banner url");
    }

    void SetUp() override {
        ProfileBuilder.AddBaseFields(
            1, 2, 3, {4, 7, 5, 6},
            "banner title", "banner text", "landing page title",
            "landing url", "banner url"
        );
    }

private:
    TBannerProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TBannerBaseFieldsPreprocessorsTests);
    UNIT_TEST(BaseFieldsTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBannerBaseFieldsPreprocessorsTests);
}
