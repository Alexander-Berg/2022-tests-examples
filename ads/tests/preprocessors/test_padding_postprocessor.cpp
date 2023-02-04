#include <ads/bigkv/preprocessor_primitives/base_preprocessor/post_processors.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yabs/server/util/bobhash.h>

#include "some_preprocessor.h"


namespace NProfilePreprocessing {

    class TPaddingTests : public TTestBase {
    public:
        void PaddingSchemaTest() {
            TParser<TSomePreprocessor3, TPaddingPostprocessor> preproc(
                TSomePreprocessor3(), TPaddingPostprocessor({{"Sentence", 4, "a1a1a1"}})
            );
            auto actualSchema = preproc.Schema();
            auto expectedSchema = TSomePreprocessor3().Schema();
            UNIT_ASSERT_EQUAL(expectedSchema, actualSchema);
        }

        void PaddingParseTest() {
            TParser<TSomePreprocessor3, TPaddingPostprocessor> preproc(
                TSomePreprocessor3("my string"), TPaddingPostprocessor({{"Sentence", 4, "a1a1a1"}, {"SentencesList", 5, NYT::TNode::CreateList()}})
            );

            auto profile = *ProfileBuilder.GetProfile();
            auto features = preproc.Parse(profile, {});

            UNIT_ASSERT_VALUES_EQUAL(features["SingleWord"].AsString(), "my string");
            UNIT_ASSERT_EQUAL(features["Sentence"], NYT::TNode::CreateList().Add("asdf").Add("qwer").Add("a1a1a1").Add("a1a1a1"));
            UNIT_ASSERT_EQUAL(features["SentencesList"], NYT::TNode::CreateList()
                .Add(NYT::TNode::CreateList().Add("asdf").Add("qwer"))
                .Add(NYT::TNode::CreateList().Add("zxcv").Add("tyui"))
                .Add(NYT::TNode::CreateList())
                .Add(NYT::TNode::CreateList())
                .Add(NYT::TNode::CreateList())
            );
        }

        void SetUp() override {}
    
    private:
        TUserProtoBuilder ProfileBuilder;

        UNIT_TEST_SUITE(TPaddingTests);
        UNIT_TEST(PaddingSchemaTest);
        UNIT_TEST(PaddingParseTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TPaddingTests);
}
