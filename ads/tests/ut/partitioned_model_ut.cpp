#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/model_builder_lib/partitioned_model.h>

#include <util/system/tempfile.h>
#include <util/generic/xrange.h>

#include <ads/pytorch/deploy/tests/lib/test_resource_util.h>

using namespace NPytorchTransport;

namespace {
    THashMap<TString, TVector<ui64>> CreateCategoricalInputs(const TString& archName) {
        THashMap<TString, TVector<ui64>> result;
        auto featureNames = NPyTorchTransportTests::GetModelFeatureNames(archName);
        for (const auto& name : featureNames) {
            result.emplace(name, TVector<ui64>{0, 1, 2, 3, 4, 1957371084, 5030928245});
        }
        return result;
    }
}

Y_UNIT_TEST_SUITE(TPartitionedModelTest) {
    Y_UNIT_TEST(EigenlibUserModelTest) {
        auto catFeatures = CreateCategoricalInputs("user_densenet");
        TPartitionedModel model("UserNamespaces_2", false);
        auto output = model.CalculateModel(catFeatures, {});
        UNIT_ASSERT_VALUES_EQUAL(model.GetVectorSize(), 50ULL);
        UNIT_ASSERT_VALUES_EQUAL(output.size(), model.GetVectorSize());
    }

    Y_UNIT_TEST(EigenlibBannerModelSaveLoadTest) {
        auto catFeatures = CreateCategoricalInputs("banner_densenet");
        TPartitionedModel model("BannerNamespaces", true);
        auto output = model.CalculateModel(catFeatures, {});

        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        model.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(tmpFile.Name());
        TPartitionedModel loadedModel;
        loadedModel.Load(inputStream);
        auto loadedOutput = loadedModel.CalculateModel(catFeatures, {});
        UNIT_ASSERT_VALUES_EQUAL(loadedOutput.size(), output.size());
        for (ui64 i : xrange(output.size())) {
            UNIT_ASSERT_DOUBLES_EQUAL(output[i], loadedOutput[i], 0.0000001);
        }
    }

    Y_UNIT_TEST(EigenlibBannerModelSharedEmbeddingTest) {
        auto catFeatures = CreateCategoricalInputs("page_densenet");
        TPartitionedModel model("PageNamespaces_shared", true);
        auto output = model.CalculateModel(catFeatures, {});

        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        model.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(tmpFile.Name());
        TPartitionedModel loadedModel;
        loadedModel.Load(inputStream);
        auto loadedOutput = loadedModel.CalculateModel(catFeatures, {});
        UNIT_ASSERT_VALUES_EQUAL(loadedOutput.size(), output.size());
        for (ui64 i : xrange(output.size())) {
            UNIT_ASSERT_DOUBLES_EQUAL(output[i], loadedOutput[i], 0.0000001);
        }
    }
};
