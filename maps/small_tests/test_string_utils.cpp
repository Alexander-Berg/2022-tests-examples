#include <yandex/maps/wiki/common/string_utils.h>
#include <yandex/maps/wiki/common/pg_utils.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(string_utils) {

Y_UNIT_TEST(test_joining_strings)
{
    const std::vector<std::string> EMPTY;
    UNIT_ASSERT_STRINGS_EQUAL(join(EMPTY, ","), "");

    const std::vector<std::string> SINGLE_ELEMENT{
        "single element"
    };
    UNIT_ASSERT_STRINGS_EQUAL(join(SINGLE_ELEMENT, "habrahabr"), SINGLE_ELEMENT.front());

    const std::vector<std::string> TWO_ELEMENTS{
        "first element",
        "second element"
    };
    const std::string SEP = "separator";
    UNIT_ASSERT_STRINGS_EQUAL(
        join(TWO_ELEMENTS, SEP),
        TWO_ELEMENTS.front() + SEP + TWO_ELEMENTS.back()
    );
}

Y_UNIT_TEST(test_joining_integrals)
{
    const std::vector<size_t> DATA{4, 8, 15, 16, 23, 42};
    UNIT_ASSERT_STRINGS_EQUAL(
        join(DATA, " "),
        "4 8 15 16 23 42"
    );
}

Y_UNIT_TEST(test_parse_sql_array)
{
    auto result = parseSqlArray("{}");
    UNIT_ASSERT(result.empty());

    result = parseSqlArray("{123}");
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_STRINGS_EQUAL(result[0], "123");

    result = parseSqlArray("{123,456}");
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);
    UNIT_ASSERT_STRINGS_EQUAL(result[0], "123");
    UNIT_ASSERT_STRINGS_EQUAL(result[1], "456");
}

Y_UNIT_TEST(test_parse_sql_array_to_int)
{
    auto result = parseSqlArray<uint64_t>("{}");
    UNIT_ASSERT(result.empty());

    result = parseSqlArray<uint64_t>("{123}");
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(result[0], 123);

    result = parseSqlArray<uint64_t>("{123,456}");
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(result[0], 123);
    UNIT_ASSERT_VALUES_EQUAL(result[1], 456);
}

Y_UNIT_TEST(test_split)
{
    {
        const std::vector<std::string> parts{""};
        UNIT_ASSERT_VALUES_EQUAL(split("", " "), parts);
    }
    {
        const std::vector<std::string> parts{"Hello", "world"};
        UNIT_ASSERT_VALUES_EQUAL(split("Hello world", " "), parts);
    }
    {
        const std::vector<std::string> parts{"Hello"};
        UNIT_ASSERT_VALUES_EQUAL(split("Hello", " "), parts);
    }
    {
        const std::vector<std::string> parts{"He", "o"};
        const std::string source("Hello");

        UNIT_ASSERT_VALUES_EQUAL(split(source, "ll"), parts);
    }
    {
        const std::vector<std::string> parts{"Hello", "world"};
        UNIT_ASSERT_VALUES_EQUAL(split(std::string("Hello world"), " "), parts);
    }
    {
        const std::vector<std::string_view> parts{"Hello", "world"};
        UNIT_ASSERT(split<std::string_view>(std::string("Hello world"), " ") == parts);
    }
    {
        const std::vector<std::string> parts{"one", "two", "three"};
        UNIT_ASSERT_VALUES_EQUAL(split("one,two,three", ","), parts);
    }
}

Y_UNIT_TEST(test_split_key_value)
{
    {
        auto [key, value] = splitKeyValue("name=deadpool");

        UNIT_ASSERT_STRINGS_EQUAL(key, "name");
        UNIT_ASSERT_STRINGS_EQUAL(value, "deadpool");
    }

    {
        auto [key, value] = splitKeyValue("name deadpool", ' ');

        UNIT_ASSERT_STRINGS_EQUAL(key, "name");
        UNIT_ASSERT_STRINGS_EQUAL(value, "deadpool");
    }

    {
        auto [key, value] = splitKeyValue("name=");

        UNIT_ASSERT_STRINGS_EQUAL(key, "name");
        UNIT_ASSERT_STRINGS_EQUAL(value, "");
    }

    UNIT_CHECK_GENERATED_EXCEPTION(splitKeyValue("=deadpool"), maps::RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(splitKeyValue("somestring"), maps::RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(splitKeyValue(""), maps::RuntimeError);
}

Y_UNIT_TEST(test_strip_quotas)
{
    UNIT_ASSERT_STRINGS_EQUAL(std::string(stripQuotas("\"word\"")), "word");
    UNIT_ASSERT_STRINGS_EQUAL(std::string(stripQuotas("'word'", '\'')), "word");

    UNIT_CHECK_GENERATED_EXCEPTION(stripQuotas("something"), maps::RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(stripQuotas("\"something"), maps::RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(stripQuotas(""), maps::RuntimeError);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
