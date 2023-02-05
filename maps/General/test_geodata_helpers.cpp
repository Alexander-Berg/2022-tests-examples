#include <maps/goods/lib/geodata_helpers/geodata_helpers.h>

#include <maps/automotive/libs/interfaces/factory_singleton.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace maps::goods;


Y_UNIT_TEST_SUITE(test_geodata_helpers) {

Y_UNIT_TEST(test_tycoon_specific_behavior)
{
    auto factory = maps::interfaces::getFactory();
    factory->addSingleton<geodata_helpers::GeobaseManager>(std::nullopt);

    auto geobaseManager = factory->getInterface<geodata_helpers::GeobaseManager>();
    UNIT_ASSERT(geobaseManager != nullptr);

    auto geobase = geobaseManager->getGeobase();
    UNIT_ASSERT(geobase != nullptr);

    UNIT_ASSERT(!geobase->isRegionInRegion(213, 1));
    UNIT_ASSERT_EQUAL("Москва", geobase->getRegionName(213));
    UNIT_ASSERT_EQUAL("Московская область", geobase->getRegionName(1));
}

}
