#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/road_graph/impl/left_side_traffic_countries.h>

namespace rg = maps::road_graph;

Y_UNIT_TEST_SUITE(TrafficSide) {

Y_UNIT_TEST(LeftSideTrafficCountries) {
    UNIT_ASSERT_EQUAL(true, rg::isLeftSideTraffic(rg::CountryIsocode("ZW")));
    UNIT_ASSERT_EQUAL(false, rg::isLeftSideTraffic(rg::CountryIsocode("RU")));
    UNIT_ASSERT_EQUAL(false, rg::isLeftSideTraffic(rg::CountryIsocode("001")));
    UNIT_ASSERT_EQUAL(false, rg::isLeftSideTraffic(rg::CountryIsocode()));
}

};
