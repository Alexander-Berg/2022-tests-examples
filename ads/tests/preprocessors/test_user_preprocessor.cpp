#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include "some_preprocessor.h"


namespace NProfilePreprocessing {

    class TUserPreprocessorTests : public TTestBase {
    public:
        void DummyPreprocessorTest() {
            TSomePreprocessor1 preprocessor;
            auto profile = *ProfileBuilder.GetProfile();

            auto features = preprocessor.Parse(profile, TArgs());

            UNIT_ASSERT_VALUES_EQUAL(features.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(features["Feature1"].AsUint64(), 123);
        }

        void SomePreprocessorTest() {
            TSomePreprocessor2 preprocessor("my param");
            auto profile = *ProfileBuilder.GetProfile();

            auto features = preprocessor.Parse(profile, 456);

            UNIT_ASSERT_VALUES_EQUAL(features.size(), 5);
            /* feature obtained directly from profile */
            UNIT_ASSERT_VALUES_EQUAL(features["FirstQuery"].AsString(), "My first query");
            /* feature obtained from extracted features */
            UNIT_ASSERT_DOUBLES_EQUAL(features["Counter4FirstVal"].AsDouble(), 17, 0.000001);
            UNIT_ASSERT_VALUES_EQUAL(features["Region"].AsUint64(), 123);
            /* feature obtained from Args */
            UNIT_ASSERT_VALUES_EQUAL(features["Timestamp"].AsUint64(), 456);
            /* feature obtained from class attributes */
            UNIT_ASSERT_VALUES_EQUAL(features["Param"].AsString(), "my param");
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

        UNIT_TEST_SUITE(TUserPreprocessorTests);
        UNIT_TEST(DummyPreprocessorTest);
        UNIT_TEST(SomePreprocessorTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TUserPreprocessorTests);
}
