#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/page_embedder/page_embedder.h>
#include <ads/pytorch/deploy/lib/tsar_computer.h>

#include <util/generic/array_ref.h>
#include <util/stream/file.h>
#include <util/system/tempfile.h>
#include <util/ysaveload.h>

using namespace NPytorchTransport;

namespace {
    class EmptyModel: public IModel {
    public:
        EmptyModel() = default;

        EmptyModel(const ui64 modelSize)
            : ModelSize(modelSize)
        {
        }

        TVector<float> CalculateModel(const THashMap<TString, TVector<ui64>>& features, const THashMap<TString, TVector<float>>& realvalueFeatures) const override {
            Y_UNUSED(features);
            Y_UNUSED(realvalueFeatures);
            return TVector<float>(ModelSize);
        }

        TVector<TVector<float>> CalculateModel(const TVector<THashMap<TString, TVector<ui64>>>& features, const TVector<THashMap<TString, TVector<float>>>& realvalueFeatures) const override {
            Y_UNUSED(realvalueFeatures);
            TVector<TVector<float>> result(features.size(), TVector<float>(ModelSize));
            return result;
        }

        ui64 GetVectorSize() const override {
            return ModelSize;
        }

        TVector<TString> GetRealValueFeatureNames() const override {
            return {};
        }

        TVector<TString> GetCategoricalFeatureNames() const override {
            return {};
        }

        void WarmUp(const ui64 warmUpCount) const override {
            Y_UNUSED(warmUpCount);
        }

        void Save(IOutputStream& stream) const override {
            ::Save(&stream, ModelSize);
        }

        void Load(IInputStream& stream) override {
            ::Load(&stream, ModelSize);
        }

    private:
        ui64 ModelSize;
    };

    class ConstModel: public IModel {
    public:
        ConstModel() = default;

        ConstModel(const ui64 modelSize)
            : ModelSize(modelSize)
        {
        }

        TVector<float> CalculateModel(const THashMap<TString, TVector<ui64>>& features, const THashMap<TString, TVector<float>>& realvalueFeatures) const override {
            Y_UNUSED(realvalueFeatures);
            float value = features.at("PageID")[0] * 7.0 + features.at("ImpID")[0] * 101 + features.at("ImpID,PageID")[0];
            return TVector<float>(ModelSize, value);
        }

        TVector<TVector<float>> CalculateModel(const TVector<THashMap<TString, TVector<ui64>>>& features, const TVector<THashMap<TString, TVector<float>>>& realvalueFeatures) const override {
            Y_UNUSED(realvalueFeatures);
            TVector<TVector<float>> result(features.size(), TVector<float>(ModelSize));
            return result;
        }

        ui64 GetVectorSize() const override {
            return ModelSize;
        }

        TVector<TString> GetRealValueFeatureNames() const override {
            return {};
        }

        TVector<TString> GetCategoricalFeatureNames() const override {
            return {"key"};
        }

        void WarmUp(const ui64 warmUpCount) const override {
            Y_UNUSED(warmUpCount);
        }

        void Save(IOutputStream& stream) const override {
            ::Save(&stream, ModelSize);
        }

        void Load(IInputStream& stream) override {
            ::Load(&stream, ModelSize);
        }

    private:
        ui64 ModelSize;
    };
}

Y_UNIT_TEST_SUITE(TPageEmbedderTest) {
    Y_UNIT_TEST(SaveLoadTest) {

        TPageEmbedder<TTsarComputer, EmptyModel> embedderOne(new TTsarComputer(new EmptyModel(10)));
        TensorTransport::TPageRecord record;
        record.SetPageID(12345);
        record.SetImpID(10);
        auto out = embedderOne.ComputeVector(record);
        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        embedderOne.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(tmpFile.Name());
        TPageEmbedder<TTsarComputer, EmptyModel> embedderTwo;
        embedderTwo.Load(inputStream);
        auto secondOut = embedderTwo.ComputeVector(record);
        UNIT_ASSERT_VALUES_EQUAL(out.size(), secondOut.size());
        UNIT_ASSERT_VALUES_EQUAL(out.size(), 11);
        for (ui64 i = 0; i < 11; ++i) {
            UNIT_ASSERT_DOUBLES_EQUAL(out[i], secondOut[i], 0.0000001);
        }
    }

    Y_UNIT_TEST(EqualMethodsTest) {
        TPageEmbedder<TTsarComputer, ConstModel> embedder(new TTsarComputer(new ConstModel(10)));
        TensorTransport::TPageRecord record;
        record.SetPageID(12345);
        record.SetImpID(10);
        auto out = embedder.ComputeVector(record);
        auto out2 = embedder.ComputeVector(NTsarTransport::TPage(record));
        UNIT_ASSERT_VALUES_EQUAL(out.size(), out2.size());
        UNIT_ASSERT_VALUES_EQUAL(out.size(), 11);
        for (ui64 i = 0; i < 11; ++i) {
            UNIT_ASSERT_DOUBLES_EQUAL(out[i], out2[i], 0.0000001);
        }
    }
};
