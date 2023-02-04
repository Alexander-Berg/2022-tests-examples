#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/lib/tsar_computer.h>

#include <util/generic/array_ref.h>
#include <util/system/tempfile.h>
#include <util/ysaveload.h>

class EmptyEntity: public NTsarTransport::IEntity {
public:
    TVector<ui64> GetFeatures() const override {
        return {};
    }

    THashMap<TString, TVector<ui64>> GetNamedFeatures() const override {
        return {};
    }

    THashMap<TString, TVector<float>> GetRealvalueFeatures() const override {
        return {};
    }
};

class EmptyModel: public NPytorchTransport::IModel {
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

class ConstEntity: public NTsarTransport::IEntity {
public:
    ConstEntity(const ui64 size, const ui64 value)
        : Size(size)
        , Value(value)
    {
    }

    TVector<ui64> GetFeatures() const override {
        return TVector<ui64>(Size, Value);
    }

    THashMap<TString, TVector<ui64>> GetNamedFeatures() const override {
        return {std::make_pair("key", TVector<ui64>(Size, Value))};
    }

    THashMap<TString, TVector<float>> GetRealvalueFeatures() const override {
        return {};
    }

private:
    ui64 Size;
    ui64 Value;
};

class IdModel: public NPytorchTransport::IModel {
public:
    IdModel() = default;

    IdModel(const ui64 modelSize)
        : ModelSize(modelSize)
    {
    }

    TVector<float> CalculateModel(const THashMap<TString, TVector<ui64>>& features, const THashMap<TString, TVector<float>>& realvalueFeatures) const override {
        Y_UNUSED(realvalueFeatures);
        TVector<float> result(ModelSize);
        UNIT_ASSERT_EQUAL(ModelSize, features.at("key").size());
        for (ui64 i = 0; i < result.size(); ++i) {
            result[i] = features.at("key")[i];
        }
        return result;
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

Y_UNIT_TEST_SUITE(TTsarComputerTest){
    Y_UNIT_TEST(VectorSizeTest){
        NPytorchTransport::TTsarComputer computer(MakeHolder<EmptyModel>(10));
        EmptyEntity entity;
        auto vec = computer.ComputeVector(entity);
        UNIT_ASSERT_EQUAL(vec.size(), 11ULL);
        NPytorchTransport::TTsarComputer computer2(new EmptyModel(42));
        auto vec2 = computer2.ComputeVector(entity);
        UNIT_ASSERT_EQUAL(vec2.size(), 43ULL);
    }

    Y_UNIT_TEST(AdditionalOneTest) {
        NPytorchTransport::TTsarComputer computer(MakeHolder<EmptyModel>(10));
        EmptyEntity entity;
        auto vec = computer.ComputeVector(entity);
        UNIT_ASSERT_EQUAL(vec[0], 1.f);
        NPytorchTransport::TTsarComputer computer2(new EmptyModel(42));
        auto vec2 = computer2.ComputeVector(entity);
        UNIT_ASSERT_EQUAL(vec2[0], 1.f);
    }

    Y_UNIT_TEST(CorrectVectorTest) {
        NPytorchTransport::TTsarComputer computer(MakeHolder<IdModel>(10));
        ConstEntity entity(10, 7);
        auto vec = computer.ComputeVector(entity);
        UNIT_ASSERT_EQUAL(vec[0], 1.f);
        UNIT_ASSERT_EQUAL(vec.size(), 11ULL);
        for (ui64 i = 1; i < 11; ++i) {
            UNIT_ASSERT_EQUAL(vec[i], 7.f);
        }
        NPytorchTransport::TTsarComputer computer2(new IdModel(42));
        ConstEntity entity2(42, 11);
        auto vec2 = computer2.ComputeVector(entity2);
        UNIT_ASSERT_EQUAL(vec2[0], 1.f);
        UNIT_ASSERT_EQUAL(vec2.size(), 43ULL);
        for (ui64 i = 1; i < 43; ++i) {
            UNIT_ASSERT_EQUAL(vec2[i], 11.f);
        }
    }

    Y_UNIT_TEST(SaveLoadEmptyModelTest) {
        NPytorchTransport::TTsarComputer firstComputer(new EmptyModel(10));
        EmptyEntity emptyEntity;
        auto firstVec = firstComputer.ComputeVector(emptyEntity);
        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        firstComputer.Save(outputStream);
        outputStream.Finish();

        NPytorchTransport::TTsarComputer secondComputer;
        TFileInput inputStream(tmpFile.Name());
        secondComputer.Load<EmptyModel>(inputStream);
        auto secondVec = secondComputer.ComputeVector(emptyEntity);
        UNIT_ASSERT_EQUAL(firstVec.size(), 11ULL);
        UNIT_ASSERT_EQUAL(firstVec.size(), secondVec.size());
        for (ui64 i = 0; i < firstVec.size(); ++i) {
            UNIT_ASSERT_EQUAL(firstVec[i], secondVec[i]);
        }
    }

    Y_UNIT_TEST(SaveLoadConstModelTest) {
        NPytorchTransport::TTsarComputer firstComputer(new IdModel(10));
        ConstEntity constEntity(10, 7);
        auto firstVec = firstComputer.ComputeVector(constEntity);
        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        firstComputer.Save(outputStream);
        outputStream.Finish();

        NPytorchTransport::TTsarComputer secondComputer;
        TFileInput inputStream(tmpFile.Name());
        secondComputer.Load<IdModel>(inputStream);
        auto secondVec = secondComputer.ComputeVector(constEntity);
        UNIT_ASSERT_EQUAL(firstVec.size(), 11ULL);
        UNIT_ASSERT_EQUAL(firstVec.size(), secondVec.size());
        for (ui64 i = 0; i < firstVec.size(); ++i) {
            UNIT_ASSERT_EQUAL(firstVec[i], secondVec[i]);
        }
    }
};
