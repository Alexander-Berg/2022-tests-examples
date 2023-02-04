#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/tskv_parser/include/parser.h>
#include <maps/libs/tskv_parser/include/cast.h>

#include <unordered_map>

namespace maps {
namespace tskv_parser {
namespace test {

typedef std::unordered_map<std::string_view, std::string_view> Map;

Y_UNIT_TEST_SUITE(tskv_parser_tests)
{

Y_UNIT_TEST(empty_string_test)
{
    Map m = parseLine<Map>("");
    UNIT_ASSERT(m.empty());
}

Y_UNIT_TEST(simple_test)
{
    Map m = parseLine<Map>("key1=value1\tkey2=value2\tkey3=value3");
    UNIT_ASSERT(m.size() == 3);
    UNIT_ASSERT(get<std::string>(m, "key1") == "value1");
    UNIT_ASSERT(get<std::string>(m, "key2") == "value2");
    UNIT_ASSERT(get<std::string>(m, "key3") == "value3");
}

Y_UNIT_TEST(empty_field_test)
{
    Map m = parseLine<Map>("key1=value1\t\tkey3=value3");
    UNIT_ASSERT(m.size() == 2);
    UNIT_ASSERT(get<std::string>(m, "key1") == "value1");
    UNIT_ASSERT(get<std::string>(m, "key3") == "value3");
}

Y_UNIT_TEST(empty_value_test)
{
    Map m = parseLine<Map>("key1=value1\tkey2=\tkey3=value3");
    UNIT_ASSERT(m.size() == 3);
    UNIT_ASSERT(get<std::string>(m, "key1") == "value1");
    UNIT_ASSERT(get<std::string>(m, "key2").empty());
    UNIT_ASSERT(get<std::string>(m, "key3") == "value3");
}

Y_UNIT_TEST(empty_key_error_test)
{
    UNIT_ASSERT_EXCEPTION(
        parseLine<Map>("key1=value1\t=value2\tkey3=value3"),
        maps::RuntimeError);
}

Y_UNIT_TEST(duplicate_key_error_test)
{
    UNIT_ASSERT_EXCEPTION(
        parseLine<Map>("key1=value1\tkey1=value2\tkey3=value3"),
        maps::RuntimeError);
}

Y_UNIT_TEST(no_value_test)
{
    Map m = parseLine<Map>("key1=value1\tkey2\tkey3=value3");
    UNIT_ASSERT(m.size() == 3);
    UNIT_ASSERT(get<std::string>(m, "key1") == "value1");
    UNIT_ASSERT(get<std::string>(m, "key2").empty());
    UNIT_ASSERT(get<std::string>(m, "key3") == "value3");
}

} // test suite end

} // namespace test
} // namespace tskv_parser
} // namespace maps
