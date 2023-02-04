#include <ads/bigkv/market/entities/banner.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/yson/parser.h>


Y_UNIT_TEST_SUITE(BannerEntityTests) {
    Y_UNIT_TEST(ParseEmptyTest) {
        // TBaseEntity constructor will fail if some of
        // CategoricalFeatures, UnravelledCategoricalFeatures, RealvalueFeatures
        // do not belong to schema or there is no proper converter for feature type.
        NMarket::TBannerEntity().GetParser()->Parse(
                NProfilePreprocessing::TProfilesPack(NCSR::TBannerProfileProto()),
                NProfilePreprocessing::TArgs(static_cast<ui64>(12345678))
        );
    }
}
