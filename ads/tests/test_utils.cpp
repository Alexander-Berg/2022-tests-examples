#include <ads/bigkv/bko/entities/utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <iostream>
#include <util/stream/output.h>

Y_UNIT_TEST_SUITE(AddIsBkoTrueFactorTests) {
    Y_UNIT_TEST(Test) {
        THashMap<TString, NPytorchTransport2::TRealvalueInputs> realvalueFeatures;

        NBkoTsar::AddIsBkoTrueFactor(10, realvalueFeatures);

        UNIT_ASSERT(realvalueFeatures.contains("IsBko"));
        const auto& vector = realvalueFeatures["IsBko"];
        UNIT_ASSERT_EQUAL(vector.Numel(), 10);

        UNIT_ASSERT_EQUAL(vector.Sizes().size(), 2);
        UNIT_ASSERT_EQUAL(vector.Sizes()[0], 10);
        UNIT_ASSERT_EQUAL(vector.Sizes()[1], 1);

        for (int i = 0; i < 10; i++) {
            UNIT_ASSERT_EQUAL(vector.DataPtr()[i], 1.0);
        }
    }
}
