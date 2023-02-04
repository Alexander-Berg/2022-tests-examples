#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/bko/entities/user.h>


Y_UNIT_TEST_SUITE(UserEntityTests) {
    Y_UNIT_TEST(ParseEmptyTest) {
        // TBaseEntity constructor will fail if some of
        // CategoricalFeatures, UnravelledCategoricalFeatures, RealvalueFeatures
        // do not belong to schema or there is no proper converter for feature type.
        NBkoTsar::TUserEntity().GetParser()->Parse(
            NProfilePreprocessing::TProfilesPack(yabs::proto::Profile()),
            NProfilePreprocessing::TArgs(static_cast<ui64>(12345678))
        );
    }
}
