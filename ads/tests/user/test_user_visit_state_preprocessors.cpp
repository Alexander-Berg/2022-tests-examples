#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/visit_state_preprocessor.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TUserVisitStatePreprocessorsTests : public TTestBase {
public:
    void VisitStateTest() {
        TVisitStateFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);
        UNIT_ASSERT(actualResult.contains("VisitStatesFeatures"));

        TVector<float> expectedValues = {2, 23227.5, 35344, 234, 345, 0.01041565743, 0.5};
        auto actualValues = actualResult["VisitStatesFeatures"].AsList();

        for (const auto& [expectedValue, actualValue]: Zip(expectedValues, actualValues)) {
            UNIT_ASSERT_DOUBLES_EQUAL(expectedValue, actualValue.AsDouble(), 1e-5);
        }
    }

    void SetUp() override {
        ProfileBuilder.AddVisitState(12345, 23456, 123, true);
        ProfileBuilder.AddVisitState(43566, 78910, 345, false);
    }

private:
    TUserProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TUserVisitStatePreprocessorsTests);
    UNIT_TEST(VisitStateTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TUserVisitStatePreprocessorsTests);
}
