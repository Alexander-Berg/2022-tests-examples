#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/tskv_parser/include/cast.h>

#include <string>
#include <vector>
#include <initializer_list>

namespace maps {
namespace tskv_parser {
namespace test {

namespace {

std::string valueToStr(const std::string& s) { return s; }

std::string valueToStr(std::string_view s)
{
    return std::string(s);
}

template <class ValueT>
std::string valueToStr(const ValueT& v)
{
    return std::to_string(v);
}

const std::string& KEY_STR = "key";
const std::string& MISSING_KEY_STR = "no_key";

std::unordered_map<std::string_view, std::string_view>
buildDict(
    const std::vector<std::string>& keys,
    const std::vector<std::string>& valueStrs)
{
    std::unordered_map<std::string_view, std::string_view> dict;
    for (size_t i = 0; i < keys.size(); ++i) {
        dict.emplace(toStringView(keys[i]), toStringView(valueStrs[i]));
    }
    return dict;
}

template <class ValueT>
void runTest(
    const std::vector<ValueT>& values,
    const ValueT& defaultValue)
{
    std::vector<std::string> keys;
    std::vector<std::string> valueStrs;
    for (size_t i = 0; i < values.size(); ++i) {
        keys.push_back(KEY_STR + std::to_string(i));
        valueStrs.push_back(valueToStr(values[i]));
    }
    auto dict = buildDict(keys, valueStrs);

    for (size_t i = 0; i < keys.size(); ++i) {
        UNIT_ASSERT_NO_EXCEPTION(get<ValueT>(dict, keys[i]));
        UNIT_ASSERT(get<ValueT>(dict, keys[i]) == values[i]);
    }

    // missing key without default value

    UNIT_ASSERT_EXCEPTION(
        get<ValueT>(dict, MISSING_KEY_STR), maps::RuntimeError);

    // missing key with default value

    auto defaulValueStr = valueToStr(defaultValue);

    UNIT_ASSERT_NO_EXCEPTION(
        get<ValueT>(dict, MISSING_KEY_STR, defaultValue));
    UNIT_ASSERT_EQUAL(
        get<ValueT>(dict, MISSING_KEY_STR, defaultValue),
        defaultValue);
}

} // namespace

Y_UNIT_TEST_SUITE(get_cast_tests)
{

Y_UNIT_TEST(integral_cast_test)
{
    runTest<short>({1, -2}, 0);
    runTest<int>({-34, 78, 145}, 0);
    runTest<long>({-34l, 78l, 0l}, -145l);
    runTest<long long>({-34l, 78l, 145l}, 0l);
    runTest<int8_t>({-34, 78, 127}, -128);
    runTest<int16_t>({-34, 78, 145}, 0);
    runTest<int32_t>({-34, 78, 145}, 0);
    runTest<int64_t>({-34, 78, 145}, 0);

    runTest<unsigned short>({1u, 2u}, 0u);
    runTest<unsigned>({34u, 78u, 145u}, 0u);
    runTest<unsigned long>({34ul, 78ul, 0ul}, 145ul);
    runTest<unsigned long long>({34ul, 78ul, 145ul}, 0ul);
    runTest<uint8_t>({34u, 78u, 127u}, 255u);
    runTest<uint16_t>({34u, 78u, 145u}, 0u);
    runTest<uint32_t>({34u, 78u, 145u}, 0u);
    runTest<uint64_t>({34u, 78u, 145u}, 0u);

}

Y_UNIT_TEST(floating_point_cast_test)
{
    runTest<float>({0.1f, 1e-2f, 34, -11, -1.2e+1f}, -3.0f);
    runTest<double>({0.1, 1e-2, 34, -1, -1.2e+1}, -3.0);
    runTest<long double>({0.1l, 1e-2l, 34, -11, -1.2e+1l}, -3.0l);
}

Y_UNIT_TEST(string_cast_test)
{
    runTest<std::string>(
        {"aa", "abc\n", "", "ghj kl", "ty\tu"}, "aaa");

    const char* s = "aaa bbdddd";
    runTest<std::string_view>(
        {
            toStringView(s, 3),
            toStringView(s + 3, 1),
            toStringView(s + 4, 2),
            toStringView(s + 6, 4)
        },
        std::string_view());
}

Y_UNIT_TEST(timestamp_cast_test)
{
    runTest<time_t>({1517427453, 1517427480}, 1517427300);
}

} // test
} // namespace test
} // namespace tskv_parser
} // namespace maps
