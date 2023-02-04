#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/builder.h>

#include <ads/bigkv/adv_machine/entities/user.h>


DEFINE_PROTO_FUZZER(const NBSYeti::TBigbPublicProto& profile) {
    auto user = NAdvMachineTsar::TUser(profile, 12345);

    // categorical features
    {
        auto expectedFeaturesList = user.CatArrFeatureNames();
        THashSet<TString> expectedFeaturesSet(expectedFeaturesList.begin(), expectedFeaturesList.end());

        auto realFeatures = user.GetNamedFeatures();
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
        auto expectedFeaturesList = user.RealValueFeatureNames();
        THashSet<TString> expectedFeaturesSet(expectedFeaturesList.begin(), expectedFeaturesList.end());

        auto realFeatures = user.GetRealvalueFeatures();
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
