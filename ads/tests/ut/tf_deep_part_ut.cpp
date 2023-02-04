#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/tf_lib/tf_deep_part.h>
#include <ads/pytorch/deploy/tests/lib/test_resource_util.h>

#include <util/system/tempfile.h>

NPytorchTransport::TTFDeepPart InitOneInputModel() {
    TTempFileHandle modelInputs;
    TTempFileHandle modelOutputs;
    TString inputMapping = "featureInput input";
    modelInputs.Write(inputMapping.c_str(), inputMapping.size());
    modelInputs.Close();
    TString outputName = "network_output";
    modelOutputs.Write(outputName.c_str(), outputName.size());
    modelOutputs.Close();
    return NPytorchTransport::TTFDeepPart("sum.pb", modelInputs.Name(), modelOutputs.Name(), 1);
}

NPytorchTransport::TTFDeepPart InitTwoInputModel() {
    TTempFileHandle modelInputs;
    TTempFileHandle modelOutputs;
    TString inputMapping = "featureOne input_1\nfeatureTwo input_2";
    modelInputs.Write(inputMapping.c_str(), inputMapping.size());
    modelInputs.Close();
    TString outputName = "network_output";
    modelOutputs.Write(outputName.c_str(), outputName.size());
    modelOutputs.Close();
    return NPytorchTransport::TTFDeepPart("sum_2_inputs.pb", modelInputs.Name(), modelOutputs.Name(), 1);
}

Y_UNIT_TEST_SUITE(TTFDeepPartTest){
    Y_UNIT_TEST(SimpleOneInputModelTest){
        auto model = InitOneInputModel();
UNIT_ASSERT_VALUES_EQUAL(model.GetOutputDim(), 1ULL);
THashMap<TString, TVector<float>> features({std::make_pair("featureInput", TVector<float>{1.f, 2.f})});
auto output = model.CalculateModel(features);
UNIT_ASSERT(output.size() == 1);
UNIT_ASSERT_VALUES_EQUAL(output[0], 3.f);
}

Y_UNIT_TEST(SimpleOneInputModelSaveLoadTest) {
    auto model = InitOneInputModel();
    TTempFile tmpFile(MakeTempName());
    TFileOutput outputStream(tmpFile.Name());
    model.Save(outputStream);
    outputStream.Finish();

    TFileInput inputStream(tmpFile.Name());
    NPytorchTransport::TTFDeepPart secondModel;
    secondModel.Load(inputStream);
    UNIT_ASSERT_VALUES_EQUAL(model.GetOutputDim(), secondModel.GetOutputDim());

    THashMap<TString, TVector<float>> features({std::make_pair("featureInput", TVector<float>{1.f, 2.f})});
    auto firstOutput = model.CalculateModel(features);
    auto secondOutput = secondModel.CalculateModel(features);
    UNIT_ASSERT_VALUES_EQUAL(firstOutput.size(), secondOutput.size());
    UNIT_ASSERT_VALUES_EQUAL(firstOutput[0], secondOutput[0]);
}

Y_UNIT_TEST(SimpleTwoInputModelTest) {
    auto model = InitTwoInputModel();
    UNIT_ASSERT_VALUES_EQUAL(model.GetOutputDim(), 1ULL);
    THashMap<TString, TVector<float>> features({std::make_pair("featureOne", TVector<float>{1.f}),
                                                std::make_pair("featureTwo", TVector<float>{2.f})});
    auto output = model.CalculateModel(features);
    UNIT_ASSERT(output.size() == 1);
    UNIT_ASSERT_VALUES_EQUAL(output[0], 3.f);
}

Y_UNIT_TEST(SimpleTwoInputSaveLoadModelTest) {
    auto model = InitTwoInputModel();
    TTempFile tmpFile(MakeTempName());
    TFileOutput outputStream(tmpFile.Name());
    model.Save(outputStream);
    outputStream.Finish();

    TFileInput inputStream(tmpFile.Name());
    NPytorchTransport::TTFDeepPart secondModel;
    secondModel.Load(inputStream);
    UNIT_ASSERT_VALUES_EQUAL(model.GetOutputDim(), secondModel.GetOutputDim());

    THashMap<TString, TVector<float>> features({std::make_pair("featureOne", TVector<float>{1.f}),
                                                std::make_pair("featureTwo", TVector<float>{2.f})});
    auto firstOutput = model.CalculateModel(features);
    auto secondOutput = secondModel.CalculateModel(features);
    UNIT_ASSERT_VALUES_EQUAL(firstOutput.size(), secondOutput.size());
    UNIT_ASSERT_VALUES_EQUAL(firstOutput[0], secondOutput[0]);
}

Y_UNIT_TEST(SimpleZeroInputModelTest) {
    TTempFileHandle modelInputs;
    TTempFileHandle modelOutputs;
    modelInputs.Close();
    TString outputName = "network_output";
    modelOutputs.Write(outputName.c_str(), outputName.size());
    modelOutputs.Close();
    THolder<NPytorchTransport::IDeepPart> model(new NPytorchTransport::TTFDeepPart("const_square.pb", modelInputs.Name(), modelOutputs.Name(), 1));
    UNIT_ASSERT_VALUES_EQUAL(model->GetOutputDim(), 1ULL);
    THashMap<TString, TVector<float>> features;
    auto output = model->CalculateModel(features);
    UNIT_ASSERT(output.size() == 1);
    UNIT_ASSERT_VALUES_EQUAL(output[0], 100.f);
}

Y_UNIT_TEST(TsarModelDeepPartTest) {
    THolder<NPytorchTransport::IDeepPart> model(new NPytorchTransport::TTFDeepPart("deep_part", 1));
    UNIT_ASSERT_VALUES_EQUAL(model->GetOutputDim(), 1ULL);
    TVector<TString> featureNames = NPyTorchTransportTests::GetModelFeatureNames("online_densenet");
    THashMap<TString, TVector<float>> features;
    TVector<float> onesVector(90, 1.f);
    for (const auto& featureName : featureNames) {
        features.emplace(std::make_pair(featureName, onesVector));
    }
    auto output = model->CalculateModel(features);
    UNIT_ASSERT(output.size() == 1);
    UNIT_ASSERT_DOUBLES_EQUAL(output[0], -0.7826162, 0.00001);

    TTempFile tmpFile(MakeTempName());
    TFileOutput outputStream(tmpFile.Name());
    model->Save(outputStream);
    outputStream.Finish();

    TFileInput inputStream(tmpFile.Name());
    NPytorchTransport::TTFDeepPart secondModel;
    secondModel.Load(inputStream);
    UNIT_ASSERT_VALUES_EQUAL(model->GetOutputDim(), secondModel.GetOutputDim());

    auto secondOutput = secondModel.CalculateModel(features);
    UNIT_ASSERT_DOUBLES_EQUAL(output[0], secondOutput[0], 0.00001);
}
}
;
