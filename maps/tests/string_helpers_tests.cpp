#include <maps/wikimap/mapspro/libs/flat_range/impl/string_helpers.h>

#include <maps/wikimap/mapspro/libs/flat_range/include/range.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::flat_range::tests {

Y_UNIT_TEST_SUITE(string_helpers_tests)
{
Y_UNIT_TEST(commonNotNumberLen)
{
    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("", ""), 0);
    UNIT_ASSERT_EQUAL(commonNotNumberPrefixLen("", ""), 0);
    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("11", "12"), 0);

    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("a", ""), 0);
    UNIT_ASSERT_EQUAL(commonNotNumberPrefixLen("a", ""), 0);

    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("a", "a"), 1);
    UNIT_ASSERT_EQUAL(commonNotNumberPrefixLen("a", "a"), 1);

    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("1a", "2a"), 1);
    UNIT_ASSERT_EQUAL(commonNotNumberPrefixLen("1a", "2a"), 0);
    UNIT_ASSERT_EQUAL(commonNotNumberPrefixLen("a1", "a2"), 1);

    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("1a1", "2a1"), 2);
    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("12a1", "2a1"), 2);
    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("1212a1a1", "a1a1"), 4);
    UNIT_ASSERT_EQUAL(commonNotNumberSuffixLen("1.1", "1.1"), 2);
}

Y_UNIT_TEST(split)
{
    UNIT_ASSERT_EXCEPTION(splitQuoted("1\"2\"3"), ParseException);
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(" 1 "), std::vector<std::string>{"1"});
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(" 1 2   5, 6"), (std::vector<std::string>{"1", "2", "5", "6"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted("\"1, 1\""), std::vector<std::string>{"\"1, 1\""});
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted("\"1, 1\" 7"), (std::vector<std::string>{"\"1, 1\"", "7"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(" 1, 1"), (std::vector<std::string>{"1", "1"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(" 1,    1   - 1   "), (std::vector<std::string>{"1", "1-1"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(" -1,    1 - - 1   "), (std::vector<std::string>{"-1", "1--1"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted("\"1 2\", 100, \"5,   8\""),  (std::vector<std::string>{"\"1 2\"", "100", "\"5,   8\""}));

    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(R"("1, 2")"), (std::vector<std::string>{R"("1, 2")"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(R"(1,"2",3)"), (std::vector<std::string>{"1", R"("2")", "3"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(R"(,1)"), (std::vector<std::string>{"1"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(R"(1,)"), (std::vector<std::string>{"1"}));
    UNIT_ASSERT_EXCEPTION(splitQuoted(R"("1,2,3)"), ParseException);
    UNIT_ASSERT_EXCEPTION(splitQuoted(R"(1,"2,3)"), ParseException);
    UNIT_ASSERT_EXCEPTION(splitQuoted(R"(1,2,3")"), ParseException);
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(R"("")"), (std::vector<std::string>{R"("")"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(R"(" ")"), (std::vector<std::string>{R"(" ")"}));
    UNIT_ASSERT_VALUES_EQUAL(splitQuoted(R"()"), (std::vector<std::string>{}));
}
} // Y_UNIT_TEST_SUITE(string_helpers_tests)

} // namespace maps::wiki::flat_range::tests
