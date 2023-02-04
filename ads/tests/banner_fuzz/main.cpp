#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/builder.h>

#include <ads/bigkv/adv_machine/entities/banner.h>


DEFINE_PROTO_FUZZER(const NAdvMachine::TBannerIndexProfile& profile) {
    auto banner = NAdvMachineTsar::TBanner(profile);

    // categorical features
    {
        auto expectedFeaturesList = banner.CatArrFeatureNames();
        THashSet<TString> expectedFeaturesSet(expectedFeaturesList.begin(), expectedFeaturesList.end());

        auto realFeatures = banner.GetNamedFeatures();
        THashSet<TString> realFeaturesSet;
        for (auto [feature, _]: realFeatures) {
            realFeaturesSet.insert(feature);
        }

        for (auto feature: realFeaturesSet) {
            UNIT_ASSERT_C(expectedFeaturesSet.contains(feature), TStringBuilder{} << "CatArrFeatureNames doesn't contain " << feature);
        }

        for (auto feature: expectedFeaturesSet) {
            UNIT_ASSERT_C(realFeaturesSet.contains(feature), TStringBuilder{} << "GetNamedFeatures doesn't contain " << feature);
        }
    }
    
    // real value features
    {
        auto expectedFeaturesList = banner.RealValueFeatureNames();
        THashSet<TString> expectedFeaturesSet(expectedFeaturesList.begin(), expectedFeaturesList.end());

        auto realFeatures = banner.GetRealvalueFeatures();
        THashSet<TString> realFeaturesSet;
        for (auto [feature, _]: realFeatures) {
            realFeaturesSet.insert(feature);
        }

        for (auto feature: realFeaturesSet) {
            UNIT_ASSERT_C(expectedFeaturesSet.contains(feature), TStringBuilder{} << "RealValueFeatureNames doesn't contain " << feature);
        }

        for (auto feature: expectedFeaturesSet) {
            UNIT_ASSERT_C(realFeaturesSet.contains(feature), TStringBuilder{} << "GetRealvalueFeatures doesn't contain " << feature);
        }
    }
}
