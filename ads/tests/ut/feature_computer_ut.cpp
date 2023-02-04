#include <library/cpp/testing/unittest/registar.h>
#include <ads/tensor_transport/lib_2/page.h>
#include <ads/tensor_transport/lib_2/model.h>
#include <ads/tensor_transport/lib_2/feature_computer.h>

using namespace NTsarTransport;

class TMockModel: public IModel {
    const ui32 ModelSize;
    TVector<float> StubValue = TVector(ModelSize, 5.0f);

    const TArrayRef<const float> EmptyVector;

public:
    TMockModel() : ModelSize(50) {}

    const TArrayRef<const float> Lookup(ui64 feature) const override {

        const TArrayRef<const float> resultArrayRef(StubValue.begin(), StubValue.end());
        if (feature == 7826035404431730673) {
            return resultArrayRef;
        }
        return EmptyVector;
    }
    ui64 GetVectorSize() const override {
        return ModelSize;
    }

    void Save(IOutputStream *stream) const override {
        Y_UNUSED(stream);
    }

    void Load(IInputStream *stream) override {
        Y_UNUSED(stream);
    }

};


Y_UNIT_TEST_SUITE(TMockModelTest) {
    Y_UNIT_TEST(SingleFeatureTest) {
        ui64 featureHash = 7826035404431730673;
        TMockModel model;
        const TArrayRef<const float> result = model.Lookup(featureHash);
        UNIT_ASSERT_VALUES_EQUAL(result[1], 5.0);
    }

};

Y_UNIT_TEST_SUITE(FeatureComputerTest) {
    Y_UNIT_TEST(PageIDEntityTest) {
        ui64 pageId = 186465;
        TPage entity(pageId);

        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeVector(entity);
        UNIT_ASSERT_VALUES_EQUAL(outputVector[1], 5.0);

    }

    Y_UNIT_TEST(ZeroDimensionTest) {
        ui64 pageId = 186465;
        TPage entity(pageId);
        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeVector(entity);
        UNIT_ASSERT_VALUES_EQUAL(outputVector[0], 1.0);

    }

    Y_UNIT_TEST(NoFeatureCaseTestFirstValue) {
        ui64 pageId = 186465 + 1;
        TPage entity(pageId);
        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeVector(entity);
        UNIT_ASSERT_VALUES_EQUAL(outputVector[1], 0.0);

    }
    Y_UNIT_TEST(NoFeatureCaseTestSizeOfOutputVectorIsNormal) {
        ui64 pageId = 186465 + 1;
        ui64 impID = 2;
        TPage entity(pageId, impID);
        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeVector(entity);
        UNIT_ASSERT_VALUES_EQUAL(outputVector.size(), 51);

    }
}


Y_UNIT_TEST_SUITE(CompressedDecompressedFeatureComputerTest) {
    Y_UNIT_TEST(PageIDEntityTest) {
        ui64 pageId = 186465;
        TPage entity(pageId);

        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeCompressedDecompressedVector(entity);
        UNIT_ASSERT_DOUBLES_EQUAL(outputVector[1], 1.0, 1e-2);

    }

    Y_UNIT_TEST(ZeroDimensionTest) {
        ui64 pageId = 186465;
        TPage entity(pageId);
        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeCompressedDecompressedVector(entity);
        UNIT_ASSERT_DOUBLES_EQUAL(outputVector[0], 1.0, 1e-2);

    }

    Y_UNIT_TEST(NoFeatureCaseTestFirstValue) {
        ui64 pageId = 186465 + 1;
        TPage entity(pageId);
        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeCompressedDecompressedVector(entity);
        UNIT_ASSERT_DOUBLES_EQUAL(outputVector[1], 0.0, 1e-2);

    }
    Y_UNIT_TEST(NoFeatureCaseTestSizeOfOutputVectorIsNormal) {
        ui64 pageId = 186465 + 1;
        ui64 impID = 2;
        TPage entity(pageId, impID);
        TComputer<TMockModel> featureComputer(MakeHolder<TMockModel>());
        auto outputVector = featureComputer.ComputeCompressedDecompressedVector(entity);
        UNIT_ASSERT_VALUES_EQUAL(outputVector.size(), 51);
    }
}
