#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/ltp_vector_preprocessor.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TLTPVectorTests : public TTestBase {
public:
    void LTPVectorEmptyProfileTest() {
        TUserProtoBuilder profileBuilder;

        TLtpVectorComputer preproc(123, 51);
        auto actualResult = preproc.Parse(*profileBuilder.GetProfile(), {});

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 1);
        UNIT_ASSERT(actualResult.contains("LTP_user_vector"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("LTP_user_vector").Size(), 51);
    }

    // void IncrementalVectorTest() {
    //     TUserProtoBuilder profileBuilder;
    //     NNeural::NInference::TLayerInputs<float> internalStates{
    //         MakeInput({1, 2, 3, 4}, 1, 4), MakeInput({4, 5, 6, 7, 9, 1, 2, 3}, 1, 8)
    //     };
    //     TString compressedValue;
    //     NNeural::NInference::PackInternalStateI8(internalStates, compressedValue);
    //     profileBuilder.AddTsarVector(21, compressedValue, 123);

    //     TIncrementalVectorComputer preproc(21, 4, 8);
    //     auto actualResult = preproc.Parse(*profileBuilder.GetProfile(), {});

    //     UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), 2);
    //     UNIT_ASSERT(actualResult.contains("IncrementalVector1"));
    //     UNIT_ASSERT(actualResult.contains("IncrementalVector2"));

    //     UNIT_ASSERT_VALUES_EQUAL(actualResult.at("IncrementalVector1").Size(), 4);
    //     UNIT_ASSERT_VALUES_EQUAL(actualResult.at("IncrementalVector2").Size(), 8);
    // }

    void SetUp() override {
        
    }

private:

    UNIT_TEST_SUITE(TLTPVectorTests);
    UNIT_TEST(LTPVectorEmptyProfileTest);
    // UNIT_TEST(IncrementalVectorTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TLTPVectorTests);
}
