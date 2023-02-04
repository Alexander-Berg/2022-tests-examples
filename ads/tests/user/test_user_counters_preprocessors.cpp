#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/counters_preprocessors.h>

#include <yabs/server/proto/keywords/keywords_data.pb.h>


namespace NProfilePreprocessing {

class TUserCounterPreprocessorsTests : public TTestBase {
public:
    void CounterKeysComputerTest() {
        TCounterKeysComputer preproc({{19, 0}, {427, 0}, {182, 5}, {123, 0}});

        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        THashMap<TString, THashSet<ui64>> expectedResult = {
            {"CounterKeys427", {1, 2, 3}},
            {"CounterKeys19", {8, 9, 10, 15}},
            {"CounterKeys182Top5", {7, 9, 10, 11, 13}},
            {"CounterKeys123", {}}
        };

        UNIT_ASSERT_VALUES_EQUAL(expectedResult.size(), actualResult.size());
        for (const auto &[key, expectedValue]: expectedResult) {
            UNIT_ASSERT(actualResult.contains(key));

            auto actualValue = actualResult[key].AsList();
            UNIT_ASSERT_VALUES_EQUAL(expectedValue.size(), actualValue.size());
            for (const auto &x: actualValue) {
                UNIT_ASSERT(expectedValue.contains(x.AsUint64()));
            }
        }
    }

    void CounterAggValuesComputerTest() {
        TCounterAggValuesComputer preproc({19, 427}, {123, 234});
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), 1300);

        THashMap<TString, TVector<float>> expectedResult = {
            {"CountersAggregatedValues", {
                4, 5.0, -10.3, -0.7,
                3, 7, 5, 6,
                0, 0, 0, 0,
                4, 1300 - 1234, 1300 - 1257, 1300 - 1248
            }}
        };

        UNIT_ASSERT(actualResult.contains("CountersAggregatedValues"));
        UNIT_ASSERT_VALUES_EQUAL(expectedResult["CountersAggregatedValues"].size(), actualResult["CountersAggregatedValues"].Size());

        for (ui32 i = 0; i < expectedResult["CountersAggregatedValues"].size(); ++i) {
            UNIT_ASSERT_DOUBLES_EQUAL(expectedResult["CountersAggregatedValues"][i], actualResult["CountersAggregatedValues"].AsList()[i].AsDouble(), 0.0001);
        }
    }

    void SetUp() override {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS, {3, 6, 10});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_USER_REGION, {123});

        ProfileBuilder.AddCounter(427, {1, 2, 3}, {5, 6, 7});
        ProfileBuilder.AddCounter(19, {8, 9, 10, 15}, {0.1, 2.4, 5.0, -10.3});
        ProfileBuilder.AddCounter(10, {4, 5, 6}, {0, 1, 2});
        ProfileBuilder.AddCounter(182, {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14}, {0, 1, 2, 5, 3, 6, 7, 9, 1, 4, 3});
        ProfileBuilder.AddCounter(234, {7, 8, 9, 10}, {1234, 1245, 1257, 1256});
    }

private:
    TUserProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TUserCounterPreprocessorsTests);
    UNIT_TEST(CounterKeysComputerTest);
    UNIT_TEST(CounterAggValuesComputerTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TUserCounterPreprocessorsTests);
}
