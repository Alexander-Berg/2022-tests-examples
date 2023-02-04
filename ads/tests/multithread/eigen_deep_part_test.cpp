#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/eigen_lib/eigen_deep_part.h>
#include <ads/pytorch/deploy/tests/lib/test_resource_util.h>

#include <util/system/tempfile.h>
#include <util/generic/xrange.h>

#include <thread>

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
        TVector<float> trueOutput{0.461124, -0.586891, 0.100931, -0.037472, 0.2419, 0.308833, 0.385573, -0.111759, -0.32881, -0.0903663};

        auto calculateModel = [&model, &inputs, &trueOutput, &outputSize] () {
            auto output = model.CalculateModel(inputs);
            UNIT_ASSERT_VALUES_EQUAL(output.size(), outputSize);
            for (size_t i : xrange(outputSize)) {
                UNIT_ASSERT_DOUBLES_EQUAL(output[i], trueOutput[i], 0.000001);
            }
        };

        TVector<std::thread> threads;
        for (size_t i : xrange(20)) {
            Y_UNUSED(i);
            threads.emplace_back(std::thread(calculateModel));
        }

        for (auto& thr : threads) {
            thr.join();
        }
    }
};
