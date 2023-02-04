#include <library/cpp/testing/unittest/registar.h>
#include <yandex/maps/wiki/social/common.h>
#include <maps/wikimap/mapspro/libs/social/helpers.h>

namespace maps::wiki::social::tests {

Y_UNIT_TEST_SUITE(fbapi_entrance_tests) {

Y_UNIT_TEST(test_array_operations)
{
    std::set<std::string> strings;

    const std::string EMPTY = "{}";
    strings = parseStringSetFromSqlArray(EMPTY);
    UNIT_ASSERT(strings.empty());

    const std::string SINGLE = "{cat:rd_jc}";
    strings = parseStringSetFromSqlArray(SINGLE);
    CategoryIdsSet EXPECTED_SINGLE{"cat:rd_jc"};
    UNIT_ASSERT_EQUAL(strings, EXPECTED_SINGLE);

    const std::string DOUBLE = "{cat:rd_jc,cat:jc_rd}";
    strings = parseStringSetFromSqlArray(DOUBLE);
    CategoryIdsSet EXPECTED_DOUBLE{"cat:rd_jc", "cat:jc_rd"};
    UNIT_ASSERT_EQUAL(strings, EXPECTED_DOUBLE);

    UNIT_ASSERT_EQUAL(
        dumpToSqlArray({}),
        "{}"
    );
    UNIT_ASSERT_EQUAL(
        dumpToSqlArray({"cat:rd_jc"}),
        "{cat:rd_jc}"
    );
    UNIT_ASSERT_EQUAL(
        dumpToSqlArray({"cat:rd_jc", "cat:jc_rd"}),
        "{cat:jc_rd,cat:rd_jc}"
    );
}

}

} // namespace maps::wiki::social::tests
