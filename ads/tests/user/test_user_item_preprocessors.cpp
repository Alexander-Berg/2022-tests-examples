#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/item_preprocessors.h>
#include <library/cpp/iterator/zip.h>

#include <yabs/server/proto/keywords/keywords_data.pb.h>


namespace NProfilePreprocessing {

class TUserItemPreprocessorsTests : public TTestBase {
public:
    void SocdemTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_MAX_SOCDEM, {
            {NBSData::NKeywords::KW_CRYPTA_USER_AGE_6S, 1},
            {NBSData::NKeywords::KW_KRYPTA_USER_GENDER, 2},
            {NBSData::NKeywords::KW_CRYPTA_INCOME_5_SEGMENTS, 3}
        });

        TSocdemFactorsComputer preproc;

        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 3);

        UNIT_ASSERT(actualResult.contains("UserCryptaAgeSegment"));
        UNIT_ASSERT(actualResult.contains("UserCryptaGender"));
        UNIT_ASSERT(actualResult.contains("UserCryptaIncome"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaAgeSegment"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaGender"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaIncome"].Size(), 1);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaAgeSegment"].AsList()[0].AsUint64(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaGender"].AsList()[0].AsUint64(), 2);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaIncome"].AsList()[0].AsUint64(), 3);
    }

    void RegionTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_USER_REGION, {123});

        TRegionFactorsComputer preproc;

        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserRegionID"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserRegionID"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserRegionID"].AsList()[0].AsUint64(), 123);
    }

    void LoyaltyTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_USER_LOYALTY, {456});

        TLoyaltyFactorsComputer preproc;

        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserCryptaYandexLoyalty"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaYandexLoyalty"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaYandexLoyalty"].AsList()[0].AsUint64(), 456);
    }

    void AffinitiveSitesTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_TOP_AFFINITIVE_SITES, {{1, 0}, {23, 0}, {456, 0}});

        TAffinitiveSitesFactorsComputer preproc;

        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserCryptaAffinitiveSitesIDs"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaAffinitiveSitesIDs"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaAffinitiveSitesIDs"].AsList()[0].AsUint64(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaAffinitiveSitesIDs"].AsList()[1].AsUint64(), 23);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaAffinitiveSitesIDs"].AsList()[2].AsUint64(), 456);
    }

    void ShortInterestsTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_SHORTTERM_INTERESTS, {34});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_SHORTTERM_INTERESTS, {56});

        TShortInterestsFactorsComputer preproc;

        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserCryptaShortInterestsAll"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaShortInterestsAll"].Size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaShortInterestsAll"].AsList()[0].AsUint64(), 34);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaShortInterestsAll"].AsList()[1].AsUint64(), 56);
    }

    void LongInterestsTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS, {21, 43, 65, 87});

        TLongInterestsFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserCryptaLongInterests"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaLongInterests"].Size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaLongInterests"].AsList()[0].AsUint64(), 21);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaLongInterests"].AsList()[1].AsUint64(), 43);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaLongInterests"].AsList()[2].AsUint64(), 65);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaLongInterests"].AsList()[3].AsUint64(), 87);
    }

    void CommonSegmentsTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_HEURISTIC_COMMON, {98, 87, 76});

        TCommonSegmentsFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserCryptaCommonSegments"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaCommonSegments"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaCommonSegments"].AsList()[0].AsUint64(), 98);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaCommonSegments"].AsList()[1].AsUint64(), 87);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserCryptaCommonSegments"].AsList()[2].AsUint64(), 76);
    }

    void BestInterestsTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_BEST_PROFILE_CATEGORIES, {214, 436, 658});

        TBestInterestsFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserBestInterests"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserBestInterests"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserBestInterests"].AsList()[0].AsUint64(), 214);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserBestInterests"].AsList()[1].AsUint64(), 436);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserBestInterests"].AsList()[2].AsUint64(), 658);
    }

    void Last10CatalogiaItemsTest() {
        for (ui32 i = 0; i < 100; ++i) {
            ProfileBuilder.AddItem(NBSData::NKeywords::KW_AWAPS_BMCATEGORIES, {1000 - i}, i + 5);
        }

        TLast10CatalogiaCategoriesFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("UserLast10CatalogiaCategories"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["UserLast10CatalogiaCategories"].Size(), 10);
        THashSet<ui32> vals;
        for (const auto& x: actualResult["UserLast10CatalogiaCategories"].AsList()) {
            vals.insert(x.AsUint64());
        }
        UNIT_ASSERT_VALUES_EQUAL(vals.size(), 10);
        for (ui32 i = 90; i < 100; ++i) {
            UNIT_ASSERT(vals.contains(1000 - i));
        }
    }

    void KryptaTopDomainTest() {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_KRYPTA_TOP_DOMAINS, {987, 654, 432});

        TKryptaTopDomainFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);

        UNIT_ASSERT(actualResult.contains("KryptaTopDomain"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["KryptaTopDomain"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["KryptaTopDomain"].AsList()[0].AsUint64(), 987);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["KryptaTopDomain"].AsList()[1].AsUint64(), 654);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["KryptaTopDomain"].AsList()[2].AsUint64(), 432);
    }

    void AllCryptaFactorsTest() {
        TCryptaFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});
        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 12);
    }

    void SetUp() override {
        /*
        SetUp may be executed multiple time during the test,
        ProfileBuilder initialization moved to the concrete tests to be executed exactly once
        */
    }

private:
    TUserProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TUserItemPreprocessorsTests);
    UNIT_TEST(SocdemTest);
    UNIT_TEST(RegionTest);
    UNIT_TEST(LoyaltyTest);
    UNIT_TEST(AffinitiveSitesTest);
    UNIT_TEST(ShortInterestsTest);
    UNIT_TEST(LongInterestsTest);
    UNIT_TEST(CommonSegmentsTest);
    UNIT_TEST(BestInterestsTest);
    UNIT_TEST(Last10CatalogiaItemsTest);
    UNIT_TEST(KryptaTopDomainTest);
    UNIT_TEST(AllCryptaFactorsTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TUserItemPreprocessorsTests);
}
