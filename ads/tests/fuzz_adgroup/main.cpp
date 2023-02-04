#include <ads/bigkv/preprocessors/ad_group_preprocessors/adgroup_counters_preprocessor.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NProfilePreprocessing;


DEFINE_PROTO_FUZZER(const NCSR::TAdGroupProfileProto& profile) {
    CheckPreprocessor(TAdGroupCountersPreprocessor(), profile, {});
}
