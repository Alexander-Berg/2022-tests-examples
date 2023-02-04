#include <ads/bigkv/preprocessors/page_preprocessors/veniamin_features_preprocessors.h>
#include <ads/bigkv/preprocessors/page_preprocessors/grab_preprocessor.h>
#include <ads/bigkv/preprocessors/page_preprocessors/bko_preprocessors.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NProfilePreprocessing;


DEFINE_PROTO_FUZZER(const yabs::proto::TPageContext& profile_) {
    auto profile = profile_;
    // Grab can't be fuzzed
    profile.SetGrab("dNCS0LvQsNGB0YLQuCDQnNC-0YHQutCy0Ysg0YDQsNC30YrRj9GB0L3QuNC70Lgg0L_RgNCw0LLQsCDQvtGC0LrQsNC30LDQstGI0LjRhdGB0Y8g0L7RgsKg0LLQsNC60YbQuNC90LDRhtC40Lg6INCv0L3QtNC10LrRgS7QndC-0LLQvtGB0YLQuAox0JLQu9Cw0YHRgtC4INCc0L7RgdC60LLRiyDRgNCw0LfRitGP0YHQvdC40LvQuCDQv9GA0LDQstCwINC-0YLQutCw0LfQsNCy0YjQuNGF0YHRjyDQvtGCINCy0LDQutGG0LjQvdCw0YbQuNC4IAoy0J_QvtC00YDQvtCx0L3QtdC1INC-INGB0L7QsdGL0YLQuNC4IAoy0JLQu9Cw0YHRgtC4INCc0L7RgdC60LLRiyDQvdCw0LfQstCw0LvQuCDRg9GB0LvQvtCy0LjRjyDQv9C-0LvRg9GH0LXQvdC40Y8gUVIt0LrQvtC00L7QsiAKMtCS0J7QlyDQvtC00L7QsdGA0LjQu9CwINC_0YDQuNC80LXQvdC10L3QuNC1INC_0YDQuNCy0LjQstC60Lgg0L_RgNC-0YLQuNCyIENPVklELTE5INGDINC_0L7QtNGA0L7RgdGC0LrQvtCyIAoy0JIg0JzQvtGB0LrQstC1INC30LAg0YHRg9GC0LrQuCDQstGL0Y_QstC70LXQvdC-IDg1OTgg0YHQu9GD0YfQsNC10LIg0LrQvtGA0L7QvdCw0LLQuNGA");

    CheckPreprocessor(TVeniaminBasePageFactorsComputer(), profile, {});
    CheckPreprocessor(TVeniaminUserAgentPageFactorsComputer(), profile, {});
    CheckPreprocessor(TVeniaminLayoutPageFactorsComputer(), profile, {});
    CheckPreprocessor(TVeniaminDateTimeFactorsComputer(), profile, 1622715867);
    CheckPreprocessor(TVeniaminAllPageFactorsComputer(), profile, 1622715867);
    CheckPreprocessor(TGrabParser(), profile, 1622715867);
    CheckPreprocessor(TPageIDFactorComputer(), profile, 1622715867);
}
