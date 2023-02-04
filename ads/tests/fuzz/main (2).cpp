#include <ads/bigkv/preprocessing/new_user_factors/lib/FeatureComputer.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/hash_set.h>


DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    NewUserFactors::TNewUserFactorsComputer computer;
    auto computedFactors = computer.ComputeFactors(profile, 12345);
    auto allFactorNamesList = computer.Factors();
    THashSet<TString> allFactorNames(allFactorNamesList.begin(), allFactorNamesList.end());
    for (const auto &[name, value]: computedFactors) {
        UNIT_ASSERT_C(allFactorNames.contains(name), "All factors should be presented in .Factors() method");
    }
    for (const auto &name: allFactorNames) {
        UNIT_ASSERT_C(computedFactors.contains(name), "All factors presented in .Factors() method should be presented in the result record");
    }
}
