#include <ads/bigkv/torch_v2/entities/user.h>
#include <ads/pytorch/deploy/model_builder_lib/partitioned_model.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/string/builder.h>


DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    {
        NTorchV2::TUser user(profile, 12345);

        auto catFeatures = user.GetNamedFeatures();
        auto rvFeatures = user.GetRealvalueFeatures();

        UNIT_ASSERT_EQUAL(rvFeatures["ShowTime"].size(), 1);
        UNIT_ASSERT_DOUBLES_EQUAL(rvFeatures["ShowTime"][0], 12345, 1e-2);

        for (auto catFeatureName: {"KryptaTopDomain", "UserRegionID", "UserBestInterests", "UserLast10CatalogiaCategories", "UserCryptaAgeSegment", "UserCryptaGender", "UserCryptaIncome", "UserCryptaYandexLoyalty", "UserCryptaAffinitiveSitesIDs", "UserCryptaLongInterests", "UserCryptaShortInterestsAll", "UserCryptaCommonSegments", "UserTop3BmCategoriesByInterestsFixed", "UserTop1BmCategoriesByInterestsFixed", "UserTop3BmCategoriesByUpdateTimeFixed", "UserTop1BmCategoriesByUpdateTimeFixed", "UserTop3BmCategoriesByEventTimeFixed", "UserTop1BmCategoriesByEventTimeFixed", "QueryTextsTokenLemma0", "QueryTextsTokenLemma1", "QueryTextsTokenLemma2", "QueryTextsTokenLemma3", "QueryTextsTokenLemma4", "QueryTextsTokenLemma5", "QueryTextsTokenLemma6", "QueryTextsTokenLemma7", "QueryTextsTokenLemma8", "QueryTextsTokenLemma9", "QueryTextsTokenLemma10", "QueryTextsTokenLemma11", "QueryTextsTokenLemma12", "QueryTextsTokenLemma13", "QueryTextsTokenLemma14", "QueryTextsTokenLemma15", "QueryTextsTokenLemma16", "QueryTextsTokenLemma17", "QueryTextsTokenLemma18", "QueryTextsTokenLemma19", "QuerySelectTypes0", "QuerySelectTypes1", "QuerySelectTypes2", "QuerySelectTypes3", "QuerySelectTypes4", "QuerySelectTypes5", "QuerySelectTypes6", "QuerySelectTypes7", "QuerySelectTypes8", "QuerySelectTypes9", "QuerySelectTypes10", "QuerySelectTypes11", "QuerySelectTypes12", "QuerySelectTypes13", "QuerySelectTypes14", "QuerySelectTypes15", "QuerySelectTypes16", "QuerySelectTypes17", "QuerySelectTypes18", "QuerySelectTypes19"}) {
            UNIT_ASSERT_C(catFeatures.contains(catFeatureName), (TStringBuilder{} << catFeatureName << " should contains in GetNamedFeatures"));
        }

        for (auto rvFeatureName: {"CountersAggregatedValues", "QueryFactors", "VisitStatesFeatures", "ShowTime"}) {
            UNIT_ASSERT_C(rvFeatures.contains(rvFeatureName), (TStringBuilder{} << rvFeatureName << " should contains in GetRealvalueFeatures"));
        }
    }

    {
        NPytorchTransport::TPartitionedModel Model("./torch_v2_model/UserNamespaces_3", true);
        NTorchV2::TUser userEntity(profile, 12345);
        Model.CalculateModel(userEntity.GetNamedFeatures(), userEntity.GetRealvalueFeatures());
    }

    {
        NTorchV2::TUserWithSearchHistory user(profile, 12345);

        auto catFeatures = user.GetNamedFeatures();
        auto cat2DFeatures = user.GetNamedFeatures2D();
        auto rvFeatures = user.GetRealvalueFeatures();

        UNIT_ASSERT_EQUAL(rvFeatures["ShowTime"].size(), 1);
        UNIT_ASSERT_DOUBLES_EQUAL(rvFeatures["ShowTime"][0], 12345, 1e-2);

        for (auto catFeatureName: {"KryptaTopDomain", "UserRegionID", "UserBestInterests", "UserLast10CatalogiaCategories", "UserCryptaAgeSegment", "UserCryptaGender", "UserCryptaIncome", "UserCryptaYandexLoyalty", "UserCryptaAffinitiveSitesIDs", "UserCryptaLongInterests", "UserCryptaShortInterestsAll", "UserCryptaCommonSegments", "UserTop3BmCategoriesByInterestsFixed", "UserTop1BmCategoriesByInterestsFixed", "UserTop3BmCategoriesByUpdateTimeFixed", "UserTop1BmCategoriesByUpdateTimeFixed", "UserTop3BmCategoriesByEventTimeFixed", "UserTop1BmCategoriesByEventTimeFixed", "QueryTextsTokenLemma0", "QueryTextsTokenLemma1", "QueryTextsTokenLemma2", "QueryTextsTokenLemma3", "QueryTextsTokenLemma4", "QueryTextsTokenLemma5", "QueryTextsTokenLemma6", "QueryTextsTokenLemma7", "QueryTextsTokenLemma8", "QueryTextsTokenLemma9", "QueryTextsTokenLemma10", "QueryTextsTokenLemma11", "QueryTextsTokenLemma12", "QueryTextsTokenLemma13", "QueryTextsTokenLemma14", "QueryTextsTokenLemma15", "QueryTextsTokenLemma16", "QueryTextsTokenLemma17", "QueryTextsTokenLemma18", "QueryTextsTokenLemma19", "QuerySelectTypes0", "QuerySelectTypes1", "QuerySelectTypes2", "QuerySelectTypes3", "QuerySelectTypes4", "QuerySelectTypes5", "QuerySelectTypes6", "QuerySelectTypes7", "QuerySelectTypes8", "QuerySelectTypes9", "QuerySelectTypes10", "QuerySelectTypes11", "QuerySelectTypes12", "QuerySelectTypes13", "QuerySelectTypes14", "QuerySelectTypes15", "QuerySelectTypes16", "QuerySelectTypes17", "QuerySelectTypes18", "QuerySelectTypes19", "QueryHistoryRegions"}) {
            UNIT_ASSERT_C(catFeatures.contains(catFeatureName), (TStringBuilder{} << catFeatureName << " should contains in GetNamedFeatures"));
        }

        for (auto rvFeatureName: {"CountersAggregatedValues", "QueryFactors", "VisitStatesFeatures", "ShowTime", "QueryHistoryFactors", "DocumentHistoryFactors"}) {
            UNIT_ASSERT_C(rvFeatures.contains(rvFeatureName), (TStringBuilder{} << rvFeatureName << " should contains in GetRealvalueFeatures"));
        }

        for (auto catFeatureName: {
            "QueryHistoryTexts", "DocumentHistoryTitles", "DocumentHistoryUrlsHost", "DocumentHistoryUrlsPath", "DocumentHistoryUrlsQuery"
        }) {
            UNIT_ASSERT_C(cat2DFeatures.contains(catFeatureName), (TStringBuilder{} << catFeatureName << " should GetNamedFeatures2D in GetNamedFeatures"));
        }
    }
}
