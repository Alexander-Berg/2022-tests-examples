
#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/eigen_lib/eigen_deep_part.h>
#include <ads/pytorch/deploy/tests/lib/test_resource_util.h>

#include <util/system/tempfile.h>
#include <util/generic/xrange.h>

using namespace NPytorchTransport;

namespace {
    TVector<float> ConstantVector(const float& c, size_t size) {
        return TVector<float>(size, c);
    }
}


Y_UNIT_TEST_SUITE(TEigenDeepPartTest) {
    Y_UNIT_TEST(FakeDumpedModel) {
        size_t outputSize = 10;
        TEigenDeepPart model("processed_deep_model", NamespaceApplicableModel, outputSize);
        THashMap<TString, TVector<float>> inputs{
            {"cat_one_FirstNamespace",  ConstantVector(2, 10)},
            {"cat_two_FirstNamespace",  ConstantVector(5, 10)},
            {"real_one_FirstNamespace", ConstantVector(3, 2)},
            {"real_two_FirstNamespace", ConstantVector(9, 5)}
        };
        auto output = model.CalculateModel(inputs);
        UNIT_ASSERT_VALUES_EQUAL(output.size(), outputSize);
        TVector<float> trueOutput{0.461124, -0.586891, 0.100931, -0.037472, 0.2419, 0.308833, 0.385573, -0.111759, -0.32881, -0.0903663};
        for (size_t i : xrange(outputSize)) {
            UNIT_ASSERT_DOUBLES_EQUAL(output[i], trueOutput[i], 0.000001);
        }
    }

    Y_UNIT_TEST(FeatureNamesTestFakeDumpModel) {
        size_t outputSize = 10;
        TEigenDeepPart model("processed_deep_model", NamespaceApplicableModel, outputSize);
        UNIT_ASSERT_VALUES_EQUAL(model.GetFeatureNames().size(), 4);
        auto features = model.GetFeatureNames();
        UNIT_ASSERT_VALUES_EQUAL(features[0], "cat_one_FirstNamespace");
        UNIT_ASSERT_VALUES_EQUAL(features[1], "cat_two_FirstNamespace");
        UNIT_ASSERT_VALUES_EQUAL(features[2], "real_one_FirstNamespace");
        UNIT_ASSERT_VALUES_EQUAL(features[3], "real_two_FirstNamespace");
    }

    Y_UNIT_TEST(FakeDumpedModelBatchCalculationConsistency) {
        size_t outputSize = 10;
        TEigenDeepPart model("processed_deep_model", NamespaceApplicableModel, outputSize);
        TVector<THashMap<TString, TVector<float>>> inputs{
            {
                {"cat_one_FirstNamespace",  ConstantVector(2, 10)},
                {"cat_two_FirstNamespace",  ConstantVector(5, 10)},
                {"real_one_FirstNamespace", ConstantVector(3, 2)},
                {"real_two_FirstNamespace", ConstantVector(9, 5)}
            },
            {
                {"cat_one_FirstNamespace",  ConstantVector(1, 10)},
                {"cat_two_FirstNamespace",  ConstantVector(2, 10)},
                {"real_one_FirstNamespace", ConstantVector(3, 2)},
                {"real_two_FirstNamespace", ConstantVector(6, 5)}
            },
            {
                {"cat_one_FirstNamespace",  ConstantVector(4, 10)},
                {"cat_two_FirstNamespace",  ConstantVector(12, 10)},
                {"real_one_FirstNamespace", ConstantVector(18, 2)},
                {"real_two_FirstNamespace", ConstantVector(1, 5)}
            },
        };
        auto batchOutputs = model.CalculateModel(inputs);

        TVector<TVector<float>> separateInstanceCalculationOutputs;

        for (const auto& input : inputs) {
            separateInstanceCalculationOutputs.emplace_back(model.CalculateModel(input));
        }

        UNIT_ASSERT_VALUES_EQUAL(batchOutputs.size(), inputs.size());

        for (size_t i = 0; i < inputs.size(); ++i) {
            UNIT_ASSERT_VALUES_EQUAL(batchOutputs[i].size(), separateInstanceCalculationOutputs[i].size());
            for (size_t j = 0; j < batchOutputs[i].size(); ++j) {
                UNIT_ASSERT_DOUBLES_EQUAL(batchOutputs[i][j], separateInstanceCalculationOutputs[i][j], 0.000001);
            }
        }
    }

    Y_UNIT_TEST(SaveLoadTest) {
        size_t outputSize = 10;
        TEigenDeepPart model("processed_deep_model", NamespaceApplicableModel, outputSize, true);
        THashMap<TString, TVector<float>> inputs{
            {"cat_one_FirstNamespace",  ConstantVector(2, 10)},
            {"cat_two_FirstNamespace",  ConstantVector(5, 10)},
            {"real_one_FirstNamespace", ConstantVector(3, 2)},
            {"real_two_FirstNamespace", ConstantVector(9, 5)}
        };
        auto output = model.CalculateModel(inputs);

        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        model.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(tmpFile.Name());
        TEigenDeepPart loadedModel;
        loadedModel.Load(inputStream);
        auto loadedOutput = loadedModel.CalculateModel(inputs);
        UNIT_ASSERT_VALUES_EQUAL(output.size(), loadedOutput.size());
        for (ui64 i : xrange(output.size())) {
            UNIT_ASSERT_DOUBLES_EQUAL(output[i], loadedOutput[i], 0.000001);
        }
    }

    Y_UNIT_TEST(PytorchAutoDeployTest) {
        size_t outputSize = 5;
        TEigenDeepPart model("fake_deployed_deep_part", TorchYamlDeployedModel, outputSize, true);
        THashMap<TString, TVector<float>> inputs{{"fake_input", ConstantVector(1, 10)}};
        auto output = model.CalculateModel(inputs);
        TVector<float> ref{0.0281,  0.1997, -0.0748, -0.0845, -0.0646};
        UNIT_ASSERT_VALUES_EQUAL(output.size(), ref.size());
        for (ui64 i : xrange(output.size())) {
            UNIT_ASSERT_DOUBLES_EQUAL(output[i], ref[i], 0.0001);
        }

        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        model.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(tmpFile.Name());
        TEigenDeepPart loadedModel;
        loadedModel.Load(inputStream);
        auto loadedOutput = loadedModel.CalculateModel(inputs);
        UNIT_ASSERT_VALUES_EQUAL(output.size(), loadedOutput.size());
        for (ui64 i : xrange(output.size())) {
            UNIT_ASSERT_DOUBLES_EQUAL(output[i], loadedOutput[i], 0.000001);
        }
    }
};
