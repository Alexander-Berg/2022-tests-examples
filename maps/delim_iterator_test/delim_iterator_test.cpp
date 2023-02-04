#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/tskv_parser/include/delim_iterator.h>
#include <maps/libs/tskv_parser/include/cast.h>

namespace maps {
namespace tskv_parser {
namespace test {

Y_UNIT_TEST_SUITE(delimiter_iterator_tests)
{

Y_UNIT_TEST(test_1)
{
    std::string s = "ahj\tv34\ti \t jkl";
    DelimiterIterator i(toStringView(s), '\t');
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "ahj");
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "v34");
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "i ");
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == " jkl");
    UNIT_ASSERT(i == DelimiterIterator());
}

// empty string

Y_UNIT_TEST(empty_string_test)
{
    UNIT_ASSERT(DelimiterIterator(std::string_view(), ' ') == DelimiterIterator());
    std::string s;
    UNIT_ASSERT(DelimiterIterator(toStringView(s), ' ') == DelimiterIterator());
}

// empty fields at begin

Y_UNIT_TEST(empty_fields_begin_test)
{
    std::string s = "..a.b";
    DelimiterIterator i(toStringView(s), '.');
    UNIT_ASSERT(i != DelimiterIterator() && i++->empty());
    UNIT_ASSERT(i != DelimiterIterator() && i++->empty());
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "a");
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "b");
    UNIT_ASSERT(i == DelimiterIterator());
}

// empty fields in the middle

Y_UNIT_TEST(empty_fields_middle_test)
{
    std::string s = "as.t...y.a";
    DelimiterIterator i(toStringView(s), '.');
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "as");
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "t");
    UNIT_ASSERT(i != DelimiterIterator() && i++->empty());
    UNIT_ASSERT(i != DelimiterIterator() && i++->empty());
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "y");
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "a");
    UNIT_ASSERT(i == DelimiterIterator());
}

// empty fields at the end

Y_UNIT_TEST(empty_fields_end_test)
{
    std::string s = "aaa\tbb\t\t\t";
    DelimiterIterator i(toStringView(s), '\t');
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "aaa");
    UNIT_ASSERT(i != DelimiterIterator() && *i++ == "bb");
    UNIT_ASSERT(i != DelimiterIterator() && i++->empty());
    UNIT_ASSERT(i != DelimiterIterator() && i++->empty());
    UNIT_ASSERT(i != DelimiterIterator() && i++->empty());
    UNIT_ASSERT(i == DelimiterIterator());
}

// nested delimiters

Y_UNIT_TEST(nested_delimiters_test)
{
    std::string s = "aa\tb\nc\t\tdef\n\n\t\tuuu\t\n\t\t\t\n";
    DelimiterIterator i(toStringView(s), '\n');
    UNIT_ASSERT(i != DelimiterIterator());
    {
        DelimiterIterator j(*i, '\t');
        UNIT_ASSERT(j != DelimiterIterator() && *j++ == "aa");
        UNIT_ASSERT(j != DelimiterIterator() && *j++ == "b");
        UNIT_ASSERT(j == DelimiterIterator());
    }
    ++i;
    UNIT_ASSERT(i != DelimiterIterator());
    {
        DelimiterIterator j(*i, '\t');
        UNIT_ASSERT(j != DelimiterIterator() && *j++ == "c");
        UNIT_ASSERT(j != DelimiterIterator() && j++->empty());
        UNIT_ASSERT(j != DelimiterIterator() && *j++ == "def");
        UNIT_ASSERT(j == DelimiterIterator());
    }
    ++i;
    UNIT_ASSERT(i != DelimiterIterator());
    UNIT_ASSERT(DelimiterIterator(*i, '\t') == DelimiterIterator());
    ++i;
    UNIT_ASSERT(i != DelimiterIterator());
    {
        DelimiterIterator j(*i, '\t');
        UNIT_ASSERT(j != DelimiterIterator() && j++->empty());
        UNIT_ASSERT(j != DelimiterIterator() && j++->empty());
        UNIT_ASSERT(j != DelimiterIterator() && *j++ == "uuu");
        UNIT_ASSERT(j != DelimiterIterator() && j++->empty());
        UNIT_ASSERT(j == DelimiterIterator());
    }
    ++i;
    UNIT_ASSERT(i != DelimiterIterator());
    {
        DelimiterIterator j(*i, '\t');
        for (size_t k = 0; k < 4; ++k) {
            UNIT_ASSERT(j != DelimiterIterator() && j++->empty());
        }
        UNIT_ASSERT(j == DelimiterIterator());
    }
    ++i;
    UNIT_ASSERT(i != DelimiterIterator());
    UNIT_ASSERT(DelimiterIterator(*i, '\t') == DelimiterIterator());
    ++i;
    UNIT_ASSERT(i == DelimiterIterator());
}

} // test suite end

} // namespace test
} // namespace tskv_parser
} // namespace maps
