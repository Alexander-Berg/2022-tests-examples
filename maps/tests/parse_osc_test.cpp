#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/gpsrealtime_hypgen/libs/osm_lib/osc_parser.h>

#include <iostream>

namespace maps::wiki::osm_hypgen {

Y_UNIT_TEST_SUITE(parse_osc) {
    Y_UNIT_TEST(test1) {
        ParsedOsc result = parseOsc("test.osc");

        UNIT_ASSERT_EQUAL(result.created.nodesWithAddr.size(), 2);
        UNIT_ASSERT_EQUAL(result.created.waysWithAddr.size(), 1);
        UNIT_ASSERT_EQUAL(result.modified.nodesWithAddr.size(), 1);
        UNIT_ASSERT_EQUAL(result.modified.waysWithAddr.size(), 1);
        UNIT_ASSERT_EQUAL(result.allNodes.size(), 9);

        UNIT_ASSERT_EQUAL(result.created.waysWithAddr[0].street(), "Гродненская улица");
        UNIT_ASSERT_EQUAL(result.created.waysWithAddr[0].housenumber(), "9");
        UNIT_ASSERT_EQUAL(result.modified.waysWithAddr[0].street(), "Аэродромная улица");
        UNIT_ASSERT_EQUAL(result.modified.waysWithAddr[0].housenumber(), "11");
        UNIT_ASSERT_EQUAL(result.modified.nodesWithAddr[0].street(), "улица Победы");
        UNIT_ASSERT_EQUAL(result.modified.nodesWithAddr[0].housenumber(), "83");

        UNIT_ASSERT_DOUBLES_EQUAL(result.allNodes[946001396].geometry().y(), 55.1662743, 1e-7);
        UNIT_ASSERT_DOUBLES_EQUAL(result.allNodes[946001396].geometry().x(), 61.4588317, 1e-7);
        UNIT_ASSERT_EQUAL(result.allNodes[946001396].street(), "улица Марченко");
    }

} // test suite end

} // namespace maps::wiki::osm_hypgen
