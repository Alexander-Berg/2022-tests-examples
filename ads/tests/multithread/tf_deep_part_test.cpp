#include <ads/pytorch/deploy/tf_lib/tf_deep_part.h>
#include <ads/pytorch/deploy/tests/lib/test_resource_util.h>

#include <thread>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TTsarDeepPartMultithreadingTest){
    Y_UNIT_TEST(MultithreadTest){
        NPytorchTransport::TTFDeepPart model("deep_part", 1);

        TVector<TString> featureNames = NPyTorchTransportTests::GetModelFeatureNames("online_densenet");
        THashMap<TString, TVector<float>> features;
        TVector<float> onesVector(90, 1.f);
        for (const auto& featureName : featureNames) {
            features.emplace(std::make_pair(featureName, onesVector));
        }

        TVector<std::thread> threads;
        auto calculateModel = [&model, &features]() {
            auto out1 = model.CalculateModel(features);
            auto out2 = model.CalculateModel(features);
            auto out3 = model.CalculateModel(features);
            UNIT_ASSERT_EQUAL(out1.size(), 1ULL);
            UNIT_ASSERT_EQUAL(out2.size(), 1ULL);
            UNIT_ASSERT_EQUAL(out3.size(), 1ULL);
            UNIT_ASSERT_EQUAL(out1[0], out2[0]);
            UNIT_ASSERT_EQUAL(out2[0], out3[0]);
        };

        for (size_t i = 0; i < 20; ++i) {
            threads.emplace_back(std::thread(calculateModel));
        }

        for (auto& thr : threads) {
            thr.join();
        }
    }
};
