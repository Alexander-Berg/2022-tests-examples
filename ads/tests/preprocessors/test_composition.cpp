#include <ads/bigkv/preprocessor_primitives/base_preprocessor/common_preprocessors.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yabs/server/proto/keywords/keywords_data.pb.h>

#include "some_preprocessor.h"


namespace NProfilePreprocessing {

    class TUserPreprocessorCompositionTests : public TTestBase {
    public:
        void Compose1PreprocessorTest() {
            TComposePreprocessors<TSomePreprocessor1> preprocessor({
                {"", TSomePreprocessor1()}
            });
            auto profile = *ProfileBuilder.GetProfile();
            auto features = preprocessor.Parse(profile, TArgs());
            UNIT_ASSERT_VALUES_EQUAL(features.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(features["Feature1"].AsUint64(), 123);
        }

        void Compose2PreprocessorsExtractRequestTest() {
            TComposePreprocessors<TSomePreprocessor1, TSomePreprocessor2> preprocessor({
                {"Pref1", TSomePreprocessor1()},
                {"Pref2", TSomePreprocessor2()},
            });
            auto profile = *ProfileBuilder.GetProfile();

            auto extractRequest = preprocessor.ExtractRequest();
            UNIT_ASSERT(extractRequest.UserExtractRequest.Items.Contains(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Items.Contains(NBSData::NKeywords::KW_USER_REGION));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(1));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(2));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(3));
            UNIT_ASSERT(extractRequest.UserExtractRequest.Counters.Contains(4));
        }

        void Compose2PreprocessorsSchemaTest() {
            TComposePreprocessors<TSomePreprocessor1, TSomePreprocessor2> preprocessor({
                {"Pref1", TSomePreprocessor1()},
                {"Pref2", TSomePreprocessor2()},
            });
            auto profile = *ProfileBuilder.GetProfile();

            auto schema = preprocessor.Schema();
            for (auto col: {"Pref1Feature1", "Pref2FirstQuery", "Pref2Counter4FirstVal", "Pref2Region", "Pref2Timestamp"}) {
                UNIT_ASSERT(schema.contains(col));
            }
        }

        void Compose2PreprocessorsParseTest() {
            TComposePreprocessors<TSomePreprocessor1, TSomePreprocessor2> preprocessor({
                {"Pref1", TSomePreprocessor1()},
                {"Pref2", TSomePreprocessor2("my param")},
            });
            auto profile = *ProfileBuilder.GetProfile();

            auto features = preprocessor.Parse(profile, 456);

            UNIT_ASSERT_VALUES_EQUAL(features.size(), 6);
            /* First preprocessor */
            UNIT_ASSERT_VALUES_EQUAL(features["Pref1Feature1"].AsUint64(), 123);
            /* Second preprocessor */
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2FirstQuery"].AsString(), "My first query");
            UNIT_ASSERT_DOUBLES_EQUAL(features["Pref2Counter4FirstVal"].AsDouble(), 17, 0.000001);
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2Region"].AsUint64(), 123);
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2Timestamp"].AsUint64(), 456);
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2Param"].AsString(), "my param");
        } 

        void Compose2PreprocessorsSaveLoadTest() {
            TStringStream s;
            {
                TComposePreprocessors<TSomePreprocessor1, TSomePreprocessor2> preprocessor({
                    {"Pref1", TSomePreprocessor1()},
                    {"Pref2", TSomePreprocessor2("my param")},
                });
                preprocessor.Save(s);
            }
            TComposePreprocessors<TSomePreprocessor1, TSomePreprocessor2> preprocessor;
            preprocessor.Load(s);

            auto schema = preprocessor.Schema();
            for (auto col: {"Pref1Feature1", "Pref2FirstQuery", "Pref2Counter4FirstVal", "Pref2Region", "Pref2Timestamp"}) {
                UNIT_ASSERT(schema.contains(col));
            }

            auto profile = *ProfileBuilder.GetProfile();
            auto features = preprocessor.Parse(profile, 456);
            UNIT_ASSERT_VALUES_EQUAL(features.size(), 6);
            /* First preprocessor */
            UNIT_ASSERT_VALUES_EQUAL(features["Pref1Feature1"].AsUint64(), 123);
            /* Second preprocessor */
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2FirstQuery"].AsString(), "My first query");
            UNIT_ASSERT_DOUBLES_EQUAL(features["Pref2Counter4FirstVal"].AsDouble(), 17, 0.000001);
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2Region"].AsUint64(), 123);
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2Timestamp"].AsUint64(), 456);
            UNIT_ASSERT_VALUES_EQUAL(features["Pref2Param"].AsString(), "my param");
        } 

        void SetUp() override {
            ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS, {3, 6, 10});
            ProfileBuilder.AddItem(NBSData::NKeywords::KW_USER_REGION, {123});

            ProfileBuilder.AddCounter(1, {4, 5, 6}, {7, 8, 9});
            ProfileBuilder.AddCounter(4, {14, 15, 16, 17}, {17, 18, 19, 20});

            ProfileBuilder.AddQuery("My first query");
        }
    
    private:
        TUserProtoBuilder ProfileBuilder;

        UNIT_TEST_SUITE(TUserPreprocessorCompositionTests);
        UNIT_TEST(Compose1PreprocessorTest);
        UNIT_TEST(Compose2PreprocessorsExtractRequestTest);
        UNIT_TEST(Compose2PreprocessorsSchemaTest);
        UNIT_TEST(Compose2PreprocessorsParseTest);
        UNIT_TEST(Compose2PreprocessorsSaveLoadTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TUserPreprocessorCompositionTests);
}
