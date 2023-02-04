#include <ads/bigkv/preprocessors/user_preprocessors/category_profile_preprocessors.h>
#include <ads/bigkv/preprocessors/user_preprocessors/counters_preprocessors.h>
#include <ads/bigkv/preprocessors/user_preprocessors/item_preprocessors.h>
#include <ads/bigkv/preprocessors/user_preprocessors/query_preprocessor.h>
#include <ads/bigkv/preprocessors/user_preprocessors/visit_state_preprocessor.h>
#include <ads/bigkv/preprocessors/user_preprocessors/search_history_preprocessors.h>
#include <ads/bigkv/preprocessors/user_preprocessors/veniamins_features_preprocessor.h>
#include <ads/bigkv/preprocessors/user_preprocessors/incremental_vector.h>
#include <ads/bigkv/preprocessors/user_preprocessors/rmp.h>
#include <ads/bigkv/preprocessors/user_preprocessors/ltp_vector_preprocessor.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NProfilePreprocessing;


DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    CheckPreprocessor(TBmCategoryFactorsComputer(), profile, {});
    CheckPreprocessor(TCounterKeysComputer({{1, 2}, {3, 0}, {4, 5}}), profile, {});
    CheckPreprocessor(TCounterAggValuesComputer({1, 2, 3}, {4, 5, 6}), profile, 123);
    CheckPreprocessor(TCryptaFactorsComputer(), profile, {});
    CheckPreprocessor(TQueryFactorsComputer(-123.0, 3600 * 24 * 60), profile, 123);
    CheckPreprocessor(TMaxCreationTimeQueryFactorsComputer(), profile, 123);
    CheckPreprocessor(TVisitStateFactorsComputer(), profile, 123);
    CheckPreprocessor(TSearchQueryComputer(), profile, 123);
    CheckPreprocessor(TSearchQueryHistoryComputer(-123.0, 3600 * 24 * 61), profile, 123);
    CheckPreprocessor(TSearchDocumentHistoryComputer(-123.0, 3600 * 24 * 62), profile, 123);
    CheckPreprocessor(TVeniaminItemsFactorsComputer(), profile, 1e9 + 123);
    CheckPreprocessor(TVeniaminCounterFactorsComputer(), profile, 1e9 + 123);
    CheckPreprocessor(TIncrementalVectorComputer(21, 512, 512), profile, 1e9 + 123);
    CheckPreprocessor(TRmpCountersPreprocessor(), profile, 1e9 + 123);
    CheckPreprocessor(TLtpVectorComputer(123, 51), profile, 1e9 + 123);
}
