#include <maps/infopoint/lib/misc/string_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <unicode/unistr.h>

#include <ostream>
#include <string>

using namespace infopoint::string_utils;

// FIXME
namespace U_ICU_NAMESPACE {

std::ostream& operator<<(std::ostream& os, const icu::UnicodeString& ustr)
{
    os << unicodeToUtf8(ustr);
    return os;
}

} // namespace U_ICU_NAMESPACE


TEST(string_utils_test_test, testUtf8ToUnicode)
{
    EXPECT_EQ(utf8ToUnicode("foo bar"), icu::UnicodeString::fromUTF8("foo bar"));
}

TEST(string_utils_tests, testUtf8ToUnicode_badUtf8)
{
    // illegal input is replaced with U+FFFD
    EXPECT_EQ(utf8ToUnicode("foo" "\xFF" "bar"),
                      utf8ToUnicode("foo\\uFFFDbar").unescape());
}

TEST(string_utils_tests, testUnicodeToUtf8)
{
    EXPECT_EQ(unicodeToUtf8(utf8ToUnicode("foo bar")), "foo bar");
}

inline icu::UnicodeString utf8(const std::string& s) { return utf8ToUnicode(s); }

TEST(string_utils_tests, testFoldCase)
{
    EXPECT_EQ(utf8("").foldCase(), utf8(""));
    EXPECT_EQ(utf8("Foo BAR").foldCase(), utf8("foo bar"));
    EXPECT_EQ(utf8("grüßen").foldCase(), utf8("grüssen"));
    EXPECT_EQ(utf8("В мурелки шлепают ПЕЛЬСИСКИ").foldCase(),
                      utf8("в мурелки шлепают пельсиски"));
}

TEST(string_utils_tests, testFoldSpace)
{
    EXPECT_EQ(foldSpace(utf8("")), utf8(""));
    EXPECT_EQ(foldSpace(utf8("foobar")), utf8("foobar"));
    EXPECT_EQ(foldSpace(utf8("foo bar")), utf8("foo bar"));
    EXPECT_EQ(foldSpace(utf8("  foo\tbar \t baz\n  \r\n")), utf8("foo bar baz"));
    EXPECT_EQ(foldSpace(utf8("\tв   мурелки\nшлепают  пельсиски\t")),
                      utf8("в мурелки шлепают пельсиски"));
}

TEST(string_utils_tests, testFoldSpace_surrogatePair)
{
    icu::UnicodeString ustr = utf8("\\uD834\\uDD1E").unescape();
    EXPECT_EQ(foldSpace(ustr), ustr);
}

TEST(string_utils_tests, testNormalizeUnicode_alreadyNormalized)
{
    EXPECT_EQ(normalizeUnicode(utf8("")), utf8(""));
    EXPECT_EQ(normalizeUnicode(utf8("foobar")), utf8("foobar"));
    EXPECT_EQ(normalizeUnicode(utf8("в стакелках светится мычай")),
                      utf8("в стакелках светится мычай"));
}

TEST(string_utils_tests, testNormalizeUnicode_equivalent)
{
    // LATIN CAPITAL LETTER A + COMBINING ACUTE ACCENT -> LATIN CAPITAL LETTER A WITH ACUTE
    EXPECT_EQ(normalizeUnicode(utf8("foo\\u0041\\u0301bar").unescape()),
                      utf8("foo\\u00C1bar").unescape());
}

TEST(string_utils_tests, testNormalizedUnicode_compatible)
{
    // LATIN SMALL LIGATURE FFI -> "ffi"
    EXPECT_EQ(normalizeUnicode(utf8("foo\\uFB03bar").unescape()),
                      utf8("fooffibar"));
}
