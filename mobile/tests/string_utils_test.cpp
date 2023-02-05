#include <yandex/maps/navikit/string_utils.h>
#include <yandex/maps/navikit/wstring.h>

#include <boost/algorithm/string/join.hpp>
#include <boost/test/unit_test.hpp>

namespace yandex::maps::navikit::tests {

namespace {

const struct {
    const char* utf8;
    const wchar_t* wstr;
}
utf8Tests[] = {
    {"", L""},
    {"Hello world.", L"Hello world."},
    {"Здравствуй, Мир!", L"Здравствуй, Мир!"},
    {"שלום עולם", L"שלום עולם"},
    {"😀😺👻", L"😀😺👻"},
};

}

void performTest(
    const std::vector<std::string>& tokens,
    const std::string& separator)
{
    const auto string = boost::algorithm::join(tokens, separator);
    const auto splitted = split(string, separator);

    BOOST_REQUIRE(tokens.size() == splitted->size());
    for (size_t i = 0; i < tokens.size(); ++i) {
        BOOST_REQUIRE(tokens.at(i) == splitted->at(i));
    }
}

void testSimplifyUrl(const std::string& url,
    const std::string& result)
{
    std::string simplified = simplifyURL(url);
    BOOST_CHECK_EQUAL(simplified, result);
}

BOOST_AUTO_TEST_CASE(SimpleTests)
{
    performTest({"hello", "world"}, " ");
    performTest({"one", "two", "three"}, ", ");
    performTest({"one", "two", "three"}, "::::");
    performTest({"di9its", "c4n", "b3", "h3r3"}, " ");
}

BOOST_AUTO_TEST_CASE(NonBreakingSpace)
{
    performTest({"120", "km/h"}, "\u00a0");
    performTest({"120", "км/ч"}, "\u00a0");
}

BOOST_AUTO_TEST_CASE(SimplifyUrl)
{
    testSimplifyUrl("t.t","t.t");
    testSimplifyUrl("ya.ru","ya.ru");
    testSimplifyUrl("www.ya.ru","ya.ru");
    testSimplifyUrl("http://ya.ru","ya.ru");
    testSimplifyUrl("https://ya.ru","ya.ru");
    testSimplifyUrl("http://www.ya.ru","ya.ru");
    testSimplifyUrl("https://www.ya.ru","ya.ru");
    testSimplifyUrl("https://www.ya.ru/","ya.ru");
    testSimplifyUrl("https://www.ya.ru/yetantotherpage.html","ya.ru");
}

BOOST_AUTO_TEST_CASE(Utf8ToWstring)
{
    for (const auto& test : utf8Tests) {
        const std::wstring wstr = utf8ToWstring(test.utf8);
        BOOST_TEST(wstr.compare(test.wstr) == 0, "Failed for '" << test.utf8 << "'");
    }
}

BOOST_AUTO_TEST_CASE(WstringToUtf8)
{
    for (const auto& test : utf8Tests) {
        const std::string str = wstringToUtf8(test.wstr);
        BOOST_TEST(str.compare(test.utf8) == 0, "Failed for '" << test.utf8 << "'");
    }
}

BOOST_AUTO_TEST_CASE(ReadUtf8)
{
    for (const auto& test : utf8Tests) {
        std::wstring wstr;

        const char* ptr = test.utf8;
        while (const wchar_t ch = getUnicodeAndIterate(ptr))
            wstr.push_back(ch);

        BOOST_TEST(wstr.compare(test.wstr) == 0, "Failed for '" << test.utf8 << "'");
    }
}

} // namespace yandex
