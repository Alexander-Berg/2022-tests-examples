#include <ads/bigkv/preprocessor_primitives/base_preprocessor/post_processors.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yabs/server/util/bobhash.h>

#include "some_preprocessor.h"


namespace NProfilePreprocessing {

    class TTextToIDTests : public TTestBase {
    public:
        void TextToIDSchemaTest() {
            TParser<TSomePreprocessor3, TTextToIDPostprocessor> preproc(
                TSomePreprocessor3(), TTextToIDPostprocessor(yabs_bobhash, {"SingleWord", "Sentence", "SentencesList"})
            );
            auto actualSchema = preproc.Schema();
            auto expectedSchema = NYT::TNode::TMapType{
                {"SingleWord", NYT::TNode()("type_name", "uint64")},
                {"Sentence", NYT::TNode()("type_name", "list")("item", "uint64")},
                {"SentencesList", NYT::TNode()("type_name", "list")("item", NYT::TNode()("type_name", "list")("item", "uint64"))}
            };
            UNIT_ASSERT_EQUAL(expectedSchema, actualSchema);
        }

        void TextToIDRequestsTest() {
            TParser<TSomePreprocessor3, TTextToIDPostprocessor> preproc(
                TSomePreprocessor3(), TTextToIDPostprocessor(yabs_bobhash, {"SingleWord", "Sentence", "SentencesList"})
            );
            auto extractRequest = preproc.ExtractRequest();
            UNIT_ASSERT(extractRequest.UserExtractRequest.Items.Contains(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Items.Contains(NBSData::NKeywords::KW_USER_REGION));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(123));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(4));            
        }

        void TextToIDParseTest() {
            TParser<TSomePreprocessor3, TTextToIDPostprocessor> preproc(
                TSomePreprocessor3("my string"), TTextToIDPostprocessor(yabs_bobhash, {"SingleWord", "Sentence", "SentencesList"})
            );

            auto profile = *ProfileBuilder.GetProfile();
            auto features = preproc.Parse(profile, {});

            UNIT_ASSERT_VALUES_EQUAL(features["SingleWord"].AsUint64(), yabs_bobhash("my string"));
            UNIT_ASSERT_EQUAL(features["Sentence"], NYT::TNode::CreateList().Add(yabs_bobhash("asdf")).Add(yabs_bobhash("qwer")));
            UNIT_ASSERT_EQUAL(features["SentencesList"], NYT::TNode::CreateList()
                .Add(NYT::TNode::CreateList().Add(yabs_bobhash("asdf")).Add(yabs_bobhash("qwer")))
                .Add(NYT::TNode::CreateList().Add(yabs_bobhash("zxcv")).Add(yabs_bobhash("tyui")))
            );
        }

        void TextToIDSaveLoadTest() {
            TStringStream s;
            {
                TParser<TSomePreprocessor3, TTextToIDPostprocessor> preproc(
                    TSomePreprocessor3("my string"), TTextToIDPostprocessor(yabs_bobhash, {"SingleWord", "Sentence", "SentencesList"})
                );
                preproc.Save(s);
            }
            TParser<TSomePreprocessor3, TTextToIDPostprocessor> preproc;
            preproc.Load(s);

            auto profile = *ProfileBuilder.GetProfile();
            auto features = preproc.Parse(profile, {});

            UNIT_ASSERT_VALUES_EQUAL(features["SingleWord"].AsUint64(), yabs_bobhash("my string"));
            UNIT_ASSERT_EQUAL(features["Sentence"], NYT::TNode::CreateList().Add(yabs_bobhash("asdf")).Add(yabs_bobhash("qwer")));
            UNIT_ASSERT_EQUAL(features["SentencesList"], NYT::TNode::CreateList()
                .Add(NYT::TNode::CreateList().Add(yabs_bobhash("asdf")).Add(yabs_bobhash("qwer")))
                .Add(NYT::TNode::CreateList().Add(yabs_bobhash("zxcv")).Add(yabs_bobhash("tyui")))
            );
        }

        void SetUp() override {}
    
    private:
        TUserProtoBuilder ProfileBuilder;

        UNIT_TEST_SUITE(TTextToIDTests);
        UNIT_TEST(TextToIDSchemaTest);
        UNIT_TEST(TextToIDRequestsTest);
        UNIT_TEST(TextToIDParseTest);
        UNIT_TEST(TextToIDSaveLoadTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TTextToIDTests);
}
