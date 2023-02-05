#include <maps/libs/stringutils/include/join.h>
#include <maps/libs/stringutils/include/split.h>
#include <maps/libs/stringutils/include/string_to.h>
#include <maps/libs/stringutils/include/to_string.h>

#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>
#include <util/random/random.h>
#include <benchmark/benchmark.h>
#include <iostream>


namespace maps::stringutils {

#define BENCH(name, code)                       \
static void name(benchmark::State& state) {     \
    for (auto _ : state) {                      \
        benchmark::DoNotOptimize(code);         \
    }                                           \
}                                               \
BENCHMARK(name)

BENCH (small_int__toString, toString(1));
BENCH (small_int__std_to_string, std::to_string(1));

const int INT = 100000 + RandomNumber<uint32_t>(100000);
BENCH (large_int__toString, toString(INT));
BENCH (large_int__std_to_string, std::to_string(INT));

const std::string STRING = std::to_string(INT);
const char* CHAR_ARRAY = STRING.c_str();
BENCH (char_array_to_int__stringTo, stringTo<int>(CHAR_ARRAY));
BENCH (char_array_to_int__atoi, std::atoi(CHAR_ARRAY));
BENCH (char_array_to_int__boost, boost::lexical_cast<int>(CHAR_ARRAY));

BENCH (string_to_int__stringTo, stringTo<int>(STRING));
BENCH (string_to_int__stoi, std::stoi(STRING));
BENCH (string_to_int__boost, boost::lexical_cast<int>(STRING));

namespace ba = boost::algorithm;
const std::vector<std::string> STRINGS = {"first string", "second string", "and so on", "and so forth", "final string"};
BENCH (string_join__stringutils, join(STRINGS, ", "));
BENCH (string_join__boost, ba::join(STRINGS, ", "));

const std::vector<std::wstring> WSTRINGS = {L"first string", L"second string", L"and so on", L"and so forth", L"final string"};
BENCH (wstring_join__stringutils, join(WSTRINGS, L", "));
BENCH (wstring_join__boost, ba::join(WSTRINGS, L", "));

const std::string TO_SPLIT = "1,2 3,4 5";
std::vector<std::string> RESULT;
BENCH (split__stringutils, split(TO_SPLIT, isAnyOf(", ")));
BENCH (split__boost, ba::split(RESULT, TO_SPLIT, ba::is_any_of(", "), ba::token_compress_off));

} // namespace maps::stringutils
