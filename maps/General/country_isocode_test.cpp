#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/road_graph/include/country_isocode.h>

#include <unordered_map>
#include <map>

namespace rg = maps::road_graph;

Y_UNIT_TEST_SUITE(CountryIsocode) {

Y_UNIT_TEST(Unknown) {
    UNIT_ASSERT_EQUAL(rg::UNKNOWN_COUNTRY_ISOCODE.name(), "");
    UNIT_ASSERT_EQUAL(rg::CountryIsocode(), rg::UNKNOWN_COUNTRY_ISOCODE);
    UNIT_ASSERT_EQUAL(rg::CountryIsocode(""), rg::UNKNOWN_COUNTRY_ISOCODE);
    UNIT_ASSERT_EQUAL(rg::CountryIsocode().name(), "");
    UNIT_ASSERT_EQUAL(rg::CountryIsocode("").name(), "");
}

Y_UNIT_TEST(Neither) {
    UNIT_ASSERT_EQUAL(rg::NEITHER_COUNTRY_ISOCODE.name(), "001");
    UNIT_ASSERT_EQUAL(rg::CountryIsocode("001"), rg::NEITHER_COUNTRY_ISOCODE);
    UNIT_ASSERT_EQUAL(rg::CountryIsocode("001").name(), "001");
}

Y_UNIT_TEST(RU) {
    UNIT_ASSERT_UNEQUAL(rg::CountryIsocode("RU"), rg::UNKNOWN_COUNTRY_ISOCODE);
    UNIT_ASSERT_UNEQUAL(rg::CountryIsocode("RU"), rg::NEITHER_COUNTRY_ISOCODE);
    UNIT_ASSERT_EQUAL(rg::CountryIsocode("RU").name(), "RU");
    UNIT_ASSERT_EQUAL(rg::CountryIsocode("RU"), rg::CountryIsocode("RU"));
    UNIT_ASSERT_EQUAL(rg::CountryIsocode(std::string("RU")).name(), std::string("RU"));
    const char ru1[3] = "RU";
    UNIT_ASSERT_EQUAL(rg::CountryIsocode(ru1).name(), std::string("RU"));
    const char* ru2 = "RU";
    UNIT_ASSERT_EQUAL(rg::CountryIsocode(ru2).name(), std::string("RU"));
    const std::string_view ru3(ru1, 2); // does not include zero terminator
    UNIT_ASSERT_EQUAL(rg::CountryIsocode(ru3).name(), std::string("RU"));
    const std::string_view ru4(ru1, 3); // includes zero terminator
    UNIT_ASSERT_EQUAL(rg::CountryIsocode(ru4).name(), std::string("RU"));
}

Y_UNIT_TEST(RU_UK) {
    UNIT_ASSERT_UNEQUAL(rg::CountryIsocode("RU"), rg::CountryIsocode("UK"));
    UNIT_ASSERT_UNEQUAL(rg::CountryIsocode("RU").name(), rg::CountryIsocode("UK").name());
}

Y_UNIT_TEST(Map) {
    std::map<rg::CountryIsocode, int> m;
    m[rg::CountryIsocode("RU")] = -1;
    m[rg::CountryIsocode("UA")] = -2;
    m[rg::CountryIsocode("BY")] = -3;
    m[rg::CountryIsocode("KZ")] = -4;
    UNIT_ASSERT(!m.count(rg::CountryIsocode("VI")));
    UNIT_ASSERT(m.count(rg::CountryIsocode("RU")));
    UNIT_ASSERT_EQUAL(m[rg::CountryIsocode("RU")], -1);
    UNIT_ASSERT_EQUAL(m[rg::CountryIsocode("KZ")], -4);
}

Y_UNIT_TEST(UnorderedMap) {
    std::unordered_map<rg::CountryIsocode, int> m;
    m[rg::CountryIsocode("RU")] = -1;
    m[rg::CountryIsocode("UA")] = -2;
    m[rg::CountryIsocode("BY")] = -3;
    m[rg::CountryIsocode("KZ")] = -4;
    UNIT_ASSERT(!m.count(rg::CountryIsocode("VI")));
    UNIT_ASSERT(m.count(rg::CountryIsocode("RU")));
    UNIT_ASSERT_EQUAL(m[rg::CountryIsocode("RU")], -1);
    UNIT_ASSERT_EQUAL(m[rg::CountryIsocode("KZ")], -4);
}

Y_UNIT_TEST(CopyAssignable) {
    rg::CountryIsocode code1("RU");
    rg::CountryIsocode code2("AU");
    UNIT_ASSERT_UNEQUAL(code1, code2);
    code2 = code1;
    UNIT_ASSERT_EQUAL(code1, code2);
}

Y_UNIT_TEST(CopyConstructible) {
    rg::CountryIsocode code1("RU");
    rg::CountryIsocode code2 = code1;
    UNIT_ASSERT_EQUAL(code1, code2);
}

};
