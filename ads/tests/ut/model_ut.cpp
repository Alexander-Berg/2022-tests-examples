#include <ads/tensor_transport/lib_2/difacto_model.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/vector.h>

using namespace NTsarTransport;

Y_UNIT_TEST_SUITE(TDifactoModelTest) {
    Y_UNIT_TEST(SingleHashModelTest) {
        ui64 testHash = 0;
        THashMap<ui64, TVector<float>> table;
        TVector<float> testVector;
        testVector.push_back(2.4);
        table[testHash] = testVector;
        TModel model(std::move(table));
        auto result = model.Lookup(testHash);
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    }

    Y_UNIT_TEST(ModelWithoutVectorSizeTest) {
        THashMap<ui64, TVector<float>> modelMap;
        TVector<float> testVector = {4.0, 5.0, 6.0};
        ui64 testHash = 45;
        ui64 anotherHash = testHash + 1;
        modelMap[testHash] = testVector;
        UNIT_ASSERT_VALUES_EQUAL(modelMap[anotherHash].size(), 0);
    }

    Y_UNIT_TEST(GetModelSizeTest) {
        THashMap<ui64 , TVector<float>> modelMap;
        ui64 vectorSize = 4;
        TVector<float> testVector(vectorSize, 0.0);
        modelMap[45] = testVector;
        TModel model(std::move(modelMap));
        UNIT_ASSERT_VALUES_EQUAL(model.GetVectorSize(), vectorSize);
    }
};
