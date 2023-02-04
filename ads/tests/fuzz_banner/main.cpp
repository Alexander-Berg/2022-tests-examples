#include <ads/bigkv/preprocessors/banner_preprocessors/banner_counters_preprocessor.h>
#include <ads/bigkv/preprocessors/banner_preprocessors/base_fields_preprocessors.h>
#include <ads/bigkv/preprocessors/banner_preprocessors/banner_rmp_preprocessor.h>
#include <ads/bigkv/preprocessors/banner_preprocessors/banner_robot_preprocessor.h>
#include <ads/bigkv/preprocessors/banner_preprocessors/banner_bko_preprocessors.h>
#include <ads/bigkv/preprocessors/banner_preprocessors/banner_market_preprocessor.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NProfilePreprocessing;


DEFINE_PROTO_FUZZER(const NCSR::TBannerProfileProto& profile) {
    CheckPreprocessor(TBannerBaseFieldsPreprocessor(), profile, {});
    CheckPreprocessor(TBannerCountersPreprocessor(), profile, {});
    CheckPreprocessor(TBannerAppDataPreprocessor(), profile, 123456789);
    CheckPreprocessor(TBannerRobotUrlPreprocessor(), profile, 123456789);
    CheckPreprocessor(TBannerRobotErfPreprocessor(), profile, 123456789);

    CheckPreprocessor(TProductPricePreprocessor(), profile, 123456789);
    CheckPreprocessor(TProductProbabilityPreprocessor(), profile, 123456789);
    CheckPreprocessor(TImageAvatarsMetaDataPreprocessor(), profile, 123456789);
    CheckPreprocessor(TDirectImageMdsMetaPreprocessor(), profile, 123456789);
    CheckPreprocessor(TBannerLandProductInfoPreprocessor(), profile, 123456789);
    CheckPreprocessor(TBannerMarketPreprocessor(), profile, 123456789);
}
