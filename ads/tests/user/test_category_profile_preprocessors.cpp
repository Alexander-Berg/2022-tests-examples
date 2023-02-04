#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/category_profile_preprocessors.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TBmCategoryProfilePreprocessorsTests : public TTestBase {
public:
    void BmCategoriesTest() {
        TBmCategoryFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 6);
        UNIT_ASSERT(actualResult.contains("UserTop1BmCategoriesByInterests"));
        UNIT_ASSERT(actualResult.contains("UserTop3BmCategoriesByInterests"));
        UNIT_ASSERT(actualResult.contains("UserTop1BmCategoriesByUpdateTime"));
        UNIT_ASSERT(actualResult.contains("UserTop3BmCategoriesByUpdateTime"));
        UNIT_ASSERT(actualResult.contains("UserTop1BmCategoriesByEventTime"));
        UNIT_ASSERT(actualResult.contains("UserTop3BmCategoriesByEventTime"));

        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop1BmCategoriesByInterests"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByInterests"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop1BmCategoriesByInterests"].AsList()[0].AsUint64(), 200000005);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByInterests"].AsList()[0].AsUint64(), 200000005);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByInterests"].AsList()[1].AsUint64(), 200000004);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByInterests"].AsList()[2].AsUint64(), 200000003);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop1BmCategoriesByUpdateTime"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByUpdateTime"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop1BmCategoriesByUpdateTime"].AsList()[0].AsUint64(), 200000001);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByUpdateTime"].AsList()[0].AsUint64(), 200000001);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByUpdateTime"].AsList()[1].AsUint64(), 200000002);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByUpdateTime"].AsList()[2].AsUint64(), 200000003);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop1BmCategoriesByEventTime"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByEventTime"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop1BmCategoriesByEventTime"].AsList()[0].AsUint64(), 200000002);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByEventTime"].AsList()[0].AsUint64(), 200000002);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByEventTime"].AsList()[1].AsUint64(), 200000004);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserTop3BmCategoriesByEventTime"].AsList()[2].AsUint64(), 200000005);
    }

    void SetUp() override {
        ProfileBuilder.AddCategoryProfile(1, 1, 50, 11);
        ProfileBuilder.AddCategoryProfile(2, 2, 40, 15);
        ProfileBuilder.AddCategoryProfile(3, 3, 30, 12);
        ProfileBuilder.AddCategoryProfile(4, 4, 20, 14);
        ProfileBuilder.AddCategoryProfile(5, 5, 10, 13);
    }

private:
    TUserProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TBmCategoryProfilePreprocessorsTests);
    UNIT_TEST(BmCategoriesTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBmCategoryProfilePreprocessorsTests);
}
