#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/incremental_vector.h>
#include <library/cpp/iterator/zip.h>

#include <ads/yacontext/lib/dssm/inference/inference.h>


namespace NProfilePreprocessing {

NNeural::NInference::TLayerInput<float> MakeInput(const TVector<float>& vec, size_t nRows, size_t nCols) {
    Y_ENSURE(vec.size() == nRows * nCols);
    return NNeural::NInference::MakeInput(NNeural::NInference::AsDynamicMatrix(vec.data(), nRows, nCols));
}

class TUserIncrementalVectorTests : public TTestBase {
public:
    void IncrementalVectorEmptyProfileTest() {
        TUserProtoBuilder profileBuilder;

        TIncrementalVectorComputer preproc(21, 4, 5);
        auto actualResult = preproc.Parse(*profileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 2);
        UNIT_ASSERT(actualResult.contains("IncrementalVector1"));
        UNIT_ASSERT(actualResult.contains("IncrementalVector2"));

        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("IncrementalVector1").Size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("IncrementalVector2").Size(), 5);
    }

    void IncrementalVectorTest() {
        TUserProtoBuilder profileBuilder;
        NNeural::NInference::TLayerInputs<float> internalStates{
            MakeInput({1, 2, 3, 4}, 1, 4), MakeInput({4, 5, 6, 7, 9, 1, 2, 3}, 1, 8)
        };
        TString compressedValue;
        NNeural::NInference::PackInternalStateI8(internalStates, compressedValue);
        profileBuilder.AddTsarVector(21, compressedValue, 123);

        TIncrementalVectorComputer preproc(21, 4, 8);
        auto actualResult = preproc.Parse(*profileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 2);
        UNIT_ASSERT(actualResult.contains("IncrementalVector1"));
        UNIT_ASSERT(actualResult.contains("IncrementalVector2"));

        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("IncrementalVector1").Size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("IncrementalVector2").Size(), 8);
    }

    void SetUp() override {
        
    }

private:
    

    UNIT_TEST_SUITE(TUserIncrementalVectorTests);
    UNIT_TEST(IncrementalVectorEmptyProfileTest);
    UNIT_TEST(IncrementalVectorTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TUserIncrementalVectorTests);
}
