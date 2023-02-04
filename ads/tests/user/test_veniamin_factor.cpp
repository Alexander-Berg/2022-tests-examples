#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/veniamins_features_preprocessor.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

ui64 TIMESTAMP = TVeniaminCounterFactorsComputer::TIMESTAMP_MIN + 1234567;

class TVeniaminFactorsTests : public TTestBase {
public:
    void ItemsSchemaTest() {
        TVeniaminItemsFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), TIMESTAMP);
        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), preproc.Schema().size());
        for (const auto& [name, _]: preproc.Schema()) {
            UNIT_ASSERT(actualResult.contains(name));
        }
    }

    void ItemsTest() {
        TVeniaminItemsFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), TIMESTAMP);

        UNIT_ASSERT(actualResult.contains("user_region"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_region"].AsUint64(), 123);
        UNIT_ASSERT(actualResult.contains("detailed_device_type_bt"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["detailed_device_type_bt"].AsUint64(), 987);
        UNIT_ASSERT(actualResult.contains("user_loyalty"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_loyalty"].AsUint64(), 456);
        UNIT_ASSERT(actualResult.contains("user_age"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_age"].AsUint64(), 1);
        UNIT_ASSERT(actualResult.contains("user_gender"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_gender"].AsUint64(), 2);
        UNIT_ASSERT(actualResult.contains("user_income"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_income"].AsUint64(), 3);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_short_interests"].Size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_short_interests"].AsList()[0].AsUint64(), 34);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_long_interests"].Size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_long_interests"].AsList()[0].AsUint64(), 21);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_long_interests"].AsList()[1].AsUint64(), 43);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_long_interests"].AsList()[2].AsUint64(), 65);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_long_interests"].AsList()[3].AsUint64(), 87);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_common_segments"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_common_segments"].AsList()[0].AsUint64(), 98);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_common_segments"].AsList()[1].AsUint64(), 87);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_common_segments"].AsList()[2].AsUint64(), 76);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_best_interests"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_best_interests"].AsList()[0].AsUint64(), 214);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_best_interests"].AsList()[1].AsUint64(), 436);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_best_interests"].AsList()[2].AsUint64(), 658);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_top_domains"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_top_domains"].AsList()[0].AsUint64(), 987);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_top_domains"].AsList()[1].AsUint64(), 654);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_top_domains"].AsList()[2].AsUint64(), 432);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_last_goals"].Size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_last_goals"].AsList()[0].AsUint64(), 234);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_last_goals"].AsList()[1].AsUint64(), 567);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["user_last_goals"].AsList()[2].AsUint64(), 89);
    }


    void CountersSchemaTest() {
        TVeniaminCounterFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), TIMESTAMP);
        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), preproc.Schema().size());
    }

    void CounterBiasTest() {
        TVeniaminCounterFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), TIMESTAMP);

        UNIT_ASSERT(actualResult.contains("bias_vect"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bias_vect"].Size(), 2 * TVeniaminCounterFactorsComputer::ActiveCountersBias.at("bias").size());
        UNIT_ASSERT(actualResult.contains("rmp_bias_vect"));
    }

    void CounterActiveTest() {
        TVeniaminCounterFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), TIMESTAMP);

        UNIT_ASSERT(actualResult.contains("order_keys_bow_21"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["order_keys_bow_21"].Size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["order_keys_bow_21"].AsList()[0].AsUint64(), TVeniaminCounterFactorsComputer::DUMMY_KEY_OFFSET + 21);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["order_keys_bow_21"].AsList()[1].AsUint64(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["order_keys_bow_21"].AsList()[2].AsUint64(), 2);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["order_keys_bow_21"].AsList()[3].AsUint64(), 4);

        UNIT_ASSERT(actualResult.contains("bmcat_keys_bow_45"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_keys_bow_45"].Size(), 5);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_keys_bow_45"].AsList()[0].AsUint64(), TVeniaminCounterFactorsComputer::DUMMY_KEY_OFFSET + 45);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_keys_bow_45"].AsList()[1].AsUint64(), 7);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_keys_bow_45"].AsList()[2].AsUint64(), 6);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_keys_bow_45"].AsList()[3].AsUint64(), 5);
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_keys_bow_45"].AsList()[4].AsUint64(), 4);
    }


    void CounterAttentionTest() {
        TVeniaminCounterFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), TIMESTAMP);

        UNIT_ASSERT(actualResult.contains("bmcat_val_k_att"));
        UNIT_ASSERT(actualResult.contains("bmcat_val_v_att"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_val_k_att"].Size(), actualResult["bmcat_val_v_att"].Size());
        for (const auto& vals: actualResult["bmcat_val_v_att"].AsList()) {
            UNIT_ASSERT_VALUES_EQUAL(vals.Size(), 2 * TVeniaminCounterFactorsComputer::ActiveCountersDict.at("bmcat").size() + TVeniaminCounterFactorsComputer::ADDITIONAL_BMCAT_FEATURES);
        }

        for (auto k: TVector<TString>{"order", "domain", "domcat", "page", "pageapp", "pageimp", "ssptoken"}) {
            UNIT_ASSERT(actualResult.contains(k + "_val_k_att"));
            UNIT_ASSERT(actualResult.contains(k + "_val_v_att"));
            UNIT_ASSERT_VALUES_EQUAL(actualResult[k + "_val_k_att"].Size(), actualResult[k + "_val_v_att"].Size());
            for (const auto& vals: actualResult[k + "_val_v_att"].AsList()) {
                UNIT_ASSERT_VALUES_EQUAL(vals.Size(), 2 * TVeniaminCounterFactorsComputer::ActiveCountersDict.at(k).size());
            }
        }

        UNIT_ASSERT_VALUES_EQUAL(actualResult["order_val_k_att"].Size(), 4);
        THashMap<ui64, NYT::TNode> orderKV;
        for (const auto& [key, val]: Zip(
            actualResult["order_val_k_att"].AsList(),
            actualResult["order_val_v_att"].AsList()
        )) {
            orderKV[key.AsList()[0].AsUint64()] = val;
        }
        UNIT_ASSERT(orderKV.contains(1));
        UNIT_ASSERT_DOUBLES_EQUAL(orderKV[1].AsList()[0].AsDouble(), Laf(0.1), 0.00001); // 21
        UNIT_ASSERT_DOUBLES_EQUAL(orderKV[1].AsList()[1].AsDouble(), Laf(0.2), 0.00001); // 23
        UNIT_ASSERT_DOUBLES_EQUAL(orderKV[1].AsList()[2].AsDouble(), 0.0, 0.00001); // 87
        UNIT_ASSERT(orderKV.contains(4));
        UNIT_ASSERT_DOUBLES_EQUAL(orderKV[4].AsList()[0].AsDouble(), Laf(0.3), 0.00001); // 21
        UNIT_ASSERT_DOUBLES_EQUAL(orderKV[4].AsList()[1].AsDouble(), Laf(3.3), 0.00001); // 23
        UNIT_ASSERT_DOUBLES_EQUAL(orderKV[4].AsList()[2].AsDouble(), 0.0, 0.00001); // 87
        UNIT_ASSERT(orderKV.contains(2));
        UNIT_ASSERT(orderKV.contains(TVeniaminCounterFactorsComputer::DUMMY_KEY_OFFSET));

        UNIT_ASSERT_VALUES_EQUAL(actualResult["bmcat_val_k_att"].Size(), 5);
        THashMap<ui64, NYT::TNode> bmcatKV;
        for (const auto& [key, val]: Zip(
            actualResult["bmcat_val_k_att"].AsList(),
            actualResult["bmcat_val_v_att"].AsList()
        )) {
            bmcatKV[key.AsList()[0].AsUint64()] = val;
        }
        UNIT_ASSERT(bmcatKV.contains(6));
        UNIT_ASSERT(bmcatKV.contains(7));
        UNIT_ASSERT_DOUBLES_EQUAL(bmcatKV[6].AsList()[0].AsDouble(), Laf(2), 0.00001); // clicks
        UNIT_ASSERT_DOUBLES_EQUAL(bmcatKV[6].AsList()[1].AsDouble(), Laf(40), 0.00001); // shows
        UNIT_ASSERT_DOUBLES_EQUAL(bmcatKV[6].AsList()[2].AsDouble(), Laf(TIMESTAMP - TVeniaminCounterFactorsComputer::TIMESTAMP_MIN - 15), 0.00001); // update time
        UNIT_ASSERT_DOUBLES_EQUAL(bmcatKV[7].AsList()[0].AsDouble(), Laf(1), 0.00001); // clicks
        UNIT_ASSERT_DOUBLES_EQUAL(bmcatKV[7].AsList()[1].AsDouble(), Laf(50), 0.00001); // shows
        UNIT_ASSERT_DOUBLES_EQUAL(bmcatKV[7].AsList()[2].AsDouble(), Laf(TIMESTAMP - TVeniaminCounterFactorsComputer::TIMESTAMP_MIN - 11), 0.00001); // update time
    }


    void SetUp() override {
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_MAX_SOCDEM, {
            {NBSData::NKeywords::KW_CRYPTA_USER_AGE_6S, 1},
            {NBSData::NKeywords::KW_KRYPTA_USER_GENDER, 2},
            {NBSData::NKeywords::KW_CRYPTA_INCOME_5_SEGMENTS, 3}
        });
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_USER_REGION, {123});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_DETAILED_DEVICE_TYPE_BT, {987});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_USER_LOYALTY, {456});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_KRYPTA_TOP_DOMAINS, {987, 654, 432});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_BEST_PROFILE_CATEGORIES, {214, 436, 658});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_HEURISTIC_COMMON, {98, 87, 76});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS, {21, 43, 65, 87});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_SHORTTERM_INTERESTS, {34});
        ProfileBuilder.AddItem(NBSData::NKeywords::KW_LAST_REACHED_VISIT_GOALS, {234, 567, 89});

        ProfileBuilder.AddCounter(651, {0, 2, 3}, {5, 6, 7});
        ProfileBuilder.AddCounter(653, {0, 9, 10, 15}, {0.1, 2.4, 5.0, -10.3});

        ProfileBuilder.AddCounter(21, {1, 2, 4}, {0.1, 0.2, 0.3});
        ProfileBuilder.AddCounter(23, {1, 2, 4}, {0.2, 1.2, 3.3});
        ProfileBuilder.AddCounter(45, {7, 6, 5, 4}, {0.5, 1.3, 2.4, 2.3});

        ProfileBuilder.AddCategoryProfile(6, 0, TVeniaminCounterFactorsComputer::TIMESTAMP_MIN + 15, 0, 2, 40);
        ProfileBuilder.AddCategoryProfile(7, 0, TVeniaminCounterFactorsComputer::TIMESTAMP_MIN + 11, 0, 1, 50);
    }

private:
    TUserProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TVeniaminFactorsTests);
    UNIT_TEST(ItemsSchemaTest);
    UNIT_TEST(ItemsTest);
    UNIT_TEST(CountersSchemaTest);
    UNIT_TEST(CounterBiasTest);
    UNIT_TEST(CounterActiveTest);
    UNIT_TEST(CounterAttentionTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TVeniaminFactorsTests);
}
