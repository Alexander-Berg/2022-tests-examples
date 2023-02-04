#include <ads/pytorch/deploy/model_builder_lib/partitioned_model.h>
#include <ads/pytorch/deploy/tests/lib/test_resource_util.h>

#include <thread>

#include <library/cpp/testing/unittest/registar.h>

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

    template <class T>
    void TestPartitionedModel(T& calcModel) {
        TVector<std::thread> threads;

        for (size_t i = 0; i < 20; ++i) {
            threads.emplace_back(std::thread(calcModel));
        }

        for (auto& thr : threads) {
            thr.join();
        }
    }
}

Y_UNIT_TEST_SUITE(TPartitionedModelMultithreadingTest){

    Y_UNIT_TEST(EigenMultithreadTest) {
        auto catFeatures = CreateCategoricalInputs("user_densenet");
        TPartitionedModel model("UserNamespaces_2", false);
        auto calculateModel = [&model, &catFeatures]() {
            auto out = model.CalculateModel(catFeatures, {});
            UNIT_ASSERT_EQUAL(out.size(), 50);
        };
        TestPartitionedModel(calculateModel);
    }
};
