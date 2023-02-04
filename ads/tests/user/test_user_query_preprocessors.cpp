#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/query_preprocessor.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TUserQueryPreprocessorsTests : public TTestBase {
public:
    void QueryTextTest() {
        TQueryFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), Timestamp);

        UNIT_ASSERT(actualResult.contains("BigBQueryTexts"));
        UNIT_ASSERT(actualResult.contains("BigBQueryFactors"));
        UNIT_ASSERT(actualResult.contains("BigBQuerySelectTypes"));

        auto actualQueryTexts = actualResult["BigBQueryTexts"].AsList();
        auto actualQueryFactors = actualResult["BigBQueryFactors"].AsList();
        auto actualQuerySelectTypes = actualResult["BigBQuerySelectTypes"].AsList();

        UNIT_ASSERT_VALUES_EQUAL(actualQueryTexts.size(), actualQueryFactors.size());
        UNIT_ASSERT_VALUES_EQUAL(actualQueryTexts.size(), actualQuerySelectTypes.size());

        TVector<
            std::tuple<TString, TVector<float>, ui64>
        > expectedQueriesFactors = {
            {
                "asd qwe",
                {1, 2, 3, 1200, 1199},
                4
            },
            {
                "zxc dsa",
                {4, 5, 6, 1100, 1050},
                5
            },
            {
                "slight future",
                {2, 4, 6, -5., -5.},
                7
            }
        };
        UNIT_ASSERT_VALUES_EQUAL(expectedQueriesFactors.size(), actualQueryTexts.size());

        for (ui32 i = 0; i < actualQueryTexts.size(); ++i) {
            const auto &[expectedText, expectedFactors, expectedSelectType] = expectedQueriesFactors[i];

            UNIT_ASSERT_VALUES_EQUAL(expectedText, actualQueryTexts[i].AsString());

            UNIT_ASSERT_VALUES_EQUAL(expectedSelectType, actualQuerySelectTypes[i].AsUint64());

            UNIT_ASSERT_VALUES_EQUAL(expectedFactors.size(), actualQueryFactors[i].AsList().size());
            for (const auto& [expectedValue, actualValue]: Zip(expectedFactors, actualQueryFactors[i].AsList())) {
                UNIT_ASSERT_DOUBLES_EQUAL(expectedValue, actualValue.AsDouble(), 0.0001);
            }
        }
    }

    void MaxCreationTimeQueryTest() {
        TMaxCreationTimeQueryFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), Timestamp);

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);
        UNIT_ASSERT(actualResult.contains("MaxCreationTimeQueryWords"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["MaxCreationTimeQueryWords"].AsString(), "Far future");
    }

    void SetUp() override {
        Timestamp = 1601988640;
        const ui64 far_timedelta = 1e7;
        UNIT_ASSERT(Timestamp > far_timedelta);

        ProfileBuilder.AddQuery("asd qwe", 1, 2, 3, 4, Timestamp - 1200, Timestamp - 1199, 1, 2, 3);
        ProfileBuilder.AddQuery("zxc dsa", 4, 5, 6, 5, Timestamp - 1100, Timestamp - 1050, 7, 8, 9);
        ProfileBuilder.AddQuery("target domain md5", 4, 5, 6, 5, Timestamp - 1101, Timestamp - 1052, 7, 8, 9, true);
        ProfileBuilder.AddQuery("slight future", 2, 4, 6, 7, Timestamp + 5, Timestamp + 5, 10, 11, 12);
        ProfileBuilder.AddQuery("Far future", 2, 4, 6, 7, Timestamp + far_timedelta, Timestamp + far_timedelta, 10, 11, 12);
        ProfileBuilder.AddQuery("Far past", 2, 4, 6, 7, Timestamp - far_timedelta, Timestamp - far_timedelta, 10, 11, 12);
    }

private:
    TUserProtoBuilder ProfileBuilder;
    ui64 Timestamp;

    UNIT_TEST_SUITE(TUserQueryPreprocessorsTests);
    UNIT_TEST(QueryTextTest);
    UNIT_TEST(MaxCreationTimeQueryTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TUserQueryPreprocessorsTests);
}
