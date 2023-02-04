#include <ads/bigkv/preprocessor_primitives/base_preprocessor/post_processors.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yabs/server/util/bobhash.h>

#include "some_preprocessor.h"


namespace NProfilePreprocessing {

    class TSeqLenTests : public TTestBase {
    public:
        void SeqLenSchemaTest() {
            TParser<TSomePreprocessor3, TSeqLenPostprocessor> preproc(
                TSomePreprocessor3(), TSeqLenPostprocessor(TVector<std::pair<TString, TString>>{{"Sentence", "Sentence_SeqLen"}})
            );
            auto actualSchema = preproc.Schema();
            auto expectedSchema = TSomePreprocessor3().Schema();
            expectedSchema["Sentence_SeqLen"] = ListType("uint64");
            UNIT_ASSERT_EQUAL(expectedSchema, actualSchema);
        }

        void SeqLenParseTest() {
            TParser<TSomePreprocessor3, TSeqLenPostprocessor> preproc(
                TSomePreprocessor3("my string"), TSeqLenPostprocessor({{"Sentence", "Sentence_SeqLen"}, {"SentencesList", "SentencesList_SeqLen"}})
            );

            auto profile = *ProfileBuilder.GetProfile();
            auto features = preproc.Parse(profile, {});

            UNIT_ASSERT(features["Sentence_SeqLen"].IsList());
            UNIT_ASSERT_EQUAL(features["Sentence_SeqLen"].AsList().size(), 1);
            UNIT_ASSERT_EQUAL(features["Sentence_SeqLen"].AsList()[0].AsUint64(), 2);

            UNIT_ASSERT(features["SentencesList_SeqLen"].IsList());
            UNIT_ASSERT_EQUAL(features["SentencesList_SeqLen"].AsList().size(), 1);
            UNIT_ASSERT_EQUAL(features["SentencesList_SeqLen"].AsList()[0].AsUint64(), 2);
        }

        void SetUp() override {}
    
    private:
        TUserProtoBuilder ProfileBuilder;

        UNIT_TEST_SUITE(TSeqLenTests);
        UNIT_TEST(SeqLenSchemaTest);
        UNIT_TEST(SeqLenParseTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TSeqLenTests);
}
