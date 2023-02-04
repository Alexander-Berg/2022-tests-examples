#include <maps/libs/tskv_parser/include/parser.h>
#include <maps/libs/tskv_parser/include/cast.h>

#include <yandex/maps/program_options/option_description.h>

#include <unordered_map>

#include <chrono>
#include <random>
#include <iostream>

namespace {

struct Range {
    size_t min;
    size_t max;
};

// random unsigned in range [r.min, r.max]
size_t genNumber(const Range& r)
{
    static std::default_random_engine gen;
    std::uniform_int_distribution<size_t> dst(r.min, r.max);
    return dst(gen);
}

std::string genSignStr()
{
    size_t rv = genNumber({0, 6});
    if (rv < 3) {
        return "";
    }
    if (rv < 6) {
        return "-";
    }
    return "+";
}

std::string genNatStr(const Range& numDigits)
{
    std::string res;
    size_t nd = genNumber(numDigits);
    for (size_t d = 0; d < nd; ++d) {
        res += (char)(genNumber(Range{'0', '9'}));
    }
    return res;
}

std::string genIntStr(const Range& numDigits)
{
    return genSignStr() + genNatStr(numDigits);
}

std::string genDoubleStr(
    const Range& numInt, const Range& numFrac, const Range& absDecimalExp)
{
    std::string expStr;
    if (genNumber({0, 1}) == 1) {
        expStr += "e" + genSignStr() + genNatStr(absDecimalExp);
    }
    return genSignStr()
        + genNatStr(numInt) + "." + genNatStr(numFrac)
        + expStr;
}

std::string genStr(const Range& length)
{
    size_t size = genNumber(length);
    std::string s(size, 'a');
    Range letters = {'a', 'z'};
    for (size_t i = 0; i < size; ++i) {
        s[i] = (char)(genNumber(letters));
    }
    return s;
}

const Range g_numDigits = {1, 8};

const Range g_numIntDigits = {1, 4};
const Range g_numFracDidits = {1, 4};
const Range g_absDecimalExp = {1, 2};

const Range g_strLength = {10, 20};

typedef std::function<std::string()> ValueGenerator;

std::map<std::string, ValueGenerator> g_valueGenerators = {
    {"int", std::bind(genIntStr, g_numDigits)},
    {"double", std::bind(genDoubleStr, g_numIntDigits, g_numFracDidits, g_absDecimalExp)},
    {"string", std::bind(genStr, g_strLength)}
};

typedef std::unordered_map<std::string_view, std::string_view> Map;

typedef std::function<void(const Map&, const std::string&)> ValueGetter;

namespace tskv = maps::tskv_parser;

std::map<std::string, ValueGetter> g_valueGetters = {
    {"int", [] (const Map& m, const std::string& k) { tskv::get<int>(m, k); }},
    {"double", [] (const Map& m, const std::string& k) { tskv::get<double>(m, k); }},
    {"string", [] (const Map& m, const std::string& k) { tskv::get<std::string>(m, k); }}
};


std::vector<std::string> genKeys(size_t numFields)
{
    std::vector<std::string> keys(numFields);
    for (size_t i = 0; i < numFields; ++i) {
        keys[i] = genStr(g_strLength) + std::to_string(i);
    }
    return keys;
}

std::vector<std::string> genTskvLines(
    size_t numLines,
    const std::vector<std::string>& keys,
    const ValueGenerator& gen)
{
    std::vector<std::string> lines(numLines);
    for (size_t i = 0; i < numLines; ++i) {
        for (size_t j = 0; j < keys.size(); ++j) {
            lines[i] += (j == 0 ? "" : "\t") + keys[j] + "=" + gen();
        }
    }
    return lines;
}

} // namespace

namespace mpo = maps::program_options;

int main(int argc, char** argv)
{
    mpo::options_description optionsDescr("Options");
    optionsDescr.add_options()("help,h", "Help message");

    mpo::OptionDescription<size_t> numLines(
            &optionsDescr, "lines,l",
            "number of tskv lines to generate",
            100000);
    mpo::OptionDescription<size_t> numFields(
            &optionsDescr, "fields,f",
            "number of fields in line",
            10);
    mpo::OptionDescription<size_t> numSelectedFields(
            &optionsDescr, "selected,s",
            "number of fields to select from dict",
            5);
    mpo::OptionDescription<size_t> numIterations(
            &optionsDescr, "iterations,i",
            "number of iterations over lines",
            100);
    mpo::OptionDescription<std::string> valueType(
            &optionsDescr, "value-type,t",
            "type of values to generate: int, double, string(default)",
            "string");

    mpo::variables_map vm =
        mpo::parseCommandLine(argc, argv, optionsDescr);

    if (vm.count("help")) {
        std::cout << optionsDescr << std::endl;
        return 1;
    }

    namespace crn = std::chrono;
    typedef crn::steady_clock Clock;
    typedef Clock::time_point Time;

    auto keys = genKeys(*numFields);
    auto tskvLines = genTskvLines(*numLines, keys, g_valueGenerators.at(*valueType));

    std::vector<std::string> selectedkeys(
        keys.begin(), keys.begin() + *numSelectedFields);

    Map m;
    m.reserve(*numFields);
    const auto& getter = g_valueGetters.at(*valueType);
    Time start = Clock::now();
    for (size_t it = 0; it < *numIterations; ++it) {
        for (size_t i = 0; i < *numLines; ++i) {
            m.clear();
            tskv::parseLine(tskvLines[i], m);
            for (const auto& k : selectedkeys) {
                getter(m, k);
            }
        }
    }
    Time end = Clock::now();

    size_t totalMSec = crn::duration_cast<crn::milliseconds>(end - start).count();

    const size_t totalLines = *numLines * *numIterations;

    std::cout << "Parsing of "
        << totalLines << " tskv lines of "
        << *numFields << " fields "
        << "of " << *valueType << " values took " << totalMSec << " milliseconds"
        << ", " << (double)totalLines / totalMSec << " Klines/sec in average\n";

    return 0;
}
