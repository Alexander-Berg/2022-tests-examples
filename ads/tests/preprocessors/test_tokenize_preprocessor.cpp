#include <ads/bigkv/preprocessor_primitives/base_preprocessor/post_processors.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yabs/server/util/bobhash.h>

#include "some_preprocessor.h"


namespace NProfilePreprocessing {

    class TTokenizerTests : public TTestBase {
    public:
        void TokenizeSchemaTest() {
            TParser<TSomePreprocessor3, TTokenizePostprocessor<TMyTokenizer>> preproc(
                TSomePreprocessor3(),
                TTokenizePostprocessor<TMyTokenizer>(TMyTokenizer(), {"SingleWord", "Sentence", "SentencesList"})
            );

            auto actualSchema = preproc.Schema();
            auto expectedSchema = NYT::TNode::TMapType{
                {"SingleWord", NYT::TNode()("type_name", "list")("item", "string")},
                {"Sentence", NYT::TNode()("type_name", "list")("item", NYT::TNode()("type_name", "list")("item", "string"))},
                {"SentencesList", NYT::TNode()("type_name", "list")("item", NYT::TNode()("type_name", "list")("item", NYT::TNode()("type_name", "list")("item", "string")))}
            };
            UNIT_ASSERT_EQUAL(expectedSchema, actualSchema);
        }

        void TokenizeRequestsTest() {
            TParser<TSomePreprocessor3, TTokenizePostprocessor<TMyTokenizer>> preproc(
                TSomePreprocessor3(),
                TTokenizePostprocessor<TMyTokenizer>(TMyTokenizer(), {"SingleWord", "Sentence", "SentencesList"})
            );
            auto extractRequest = preproc.ExtractRequest();
            UNIT_ASSERT(extractRequest.UserExtractRequest.Items.Contains(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Items.Contains(NBSData::NKeywords::KW_USER_REGION));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(123));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(4));            
        }

        void TokenizeParseTest() {
            TParser<TSomePreprocessor3, TTokenizePostprocessor<TMyTokenizer>> preproc(
                TSomePreprocessor3("my string"),
                TTokenizePostprocessor<TMyTokenizer>(TMyTokenizer(), {"SingleWord", "Sentence", "SentencesList"})
            );

            auto profile = *ProfileBuilder.GetProfile();
            auto features = preproc.Parse(profile, {});

            UNIT_ASSERT_EQUAL(features["SingleWord"], NYT::TNode::CreateList().Add("my").Add("string"));
            UNIT_ASSERT_EQUAL(features["Sentence"], NYT::TNode::CreateList()
                .Add(NYT::TNode::CreateList().Add("asdf"))
                .Add(NYT::TNode::CreateList().Add("qwer"))
            );
        }

        void TokenizeSaveLoadTest() {
            TStringStream s;
            {
                TParser<TSomePreprocessor3, TTokenizePostprocessor<TMyTokenizer>> preproc(
                    TSomePreprocessor3("my string"),
                    TTokenizePostprocessor<TMyTokenizer>(TMyTokenizer(), {"SingleWord", "Sentence", "SentencesList"})
                );
                preproc.Save(s);
            }
            TParser<TSomePreprocessor3, TTokenizePostprocessor<TMyTokenizer>> preproc;
            preproc.Load(s);

            auto profile = *ProfileBuilder.GetProfile();
            auto features = preproc.Parse(profile, {});

            UNIT_ASSERT_EQUAL(features["SingleWord"], NYT::TNode::CreateList().Add("my").Add("string"));
            UNIT_ASSERT_EQUAL(features["Sentence"], NYT::TNode::CreateList()
                .Add(NYT::TNode::CreateList().Add("asdf"))
                .Add(NYT::TNode::CreateList().Add("qwer"))
            );
        }

        void TokenizeBadUtfTest() {
            TTokenizePostprocessor<TMyTokenizer> postprocessor(TMyTokenizer(), {"SomeField",});

            const TString badString = "xe2\x28\xa1";
            auto row = NYT::TNode::TMapType{
                {"SomeField", badString},
            };
            UNIT_ASSERT_EXCEPTION_CONTAINS(
                postprocessor.Process(row),
                yexception,
                "Field 'SomeField' contains invalid UTF characters, can't tokenize it"
            );
        }

        void SetUp() override {}
    
    private:
        TUserProtoBuilder ProfileBuilder;

        UNIT_TEST_SUITE(TTokenizerTests);
        UNIT_TEST(TokenizeSchemaTest);
        UNIT_TEST(TokenizeRequestsTest);
        UNIT_TEST(TokenizeParseTest);
        UNIT_TEST(TokenizeSaveLoadTest);
        UNIT_TEST(TokenizeBadUtfTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TTokenizerTests);
}
