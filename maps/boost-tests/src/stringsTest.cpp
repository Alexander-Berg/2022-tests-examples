#include "../include/stringsTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

#include <maps/renderer/libs/base/include/string_convert.h>
#include <yandex/maps/renderer5/core/strings/Converter.h>
#include <yandex/maps/renderer5/core/strings/normalize_sql_query.h>

#include <thread>

using namespace boost::unit_test;
using namespace maps::renderer5;
using namespace maps::renderer5::test;

void normalizeSqlQueryTest()
{
    using namespace core::strings;

    std::wstring sqlOriginal1 = L"SELECT *  FROM t WHERE  id<  10";
    std::wstring sqlOriginal2 = L"SELECT   *FROM t WHERE  id  <10";
    std::wstring sqlCompactExpected = L"SELECT*FROM t WHERE id<10";
    std::wstring sqlHumanExpected1 = L"SELECT * FROM t WHERE id< 10";
    std::wstring sqlHumanExpected2 = L"SELECT *FROM t WHERE id <10";

    BOOST_REQUIRE_NO_THROW(
        normalizeSqlQuery(
            L"f1='N' AND f2=''", NormalizeCompact));

    BOOST_REQUIRE_NO_THROW(
        normalizeSqlQuery(
            L"f1='N' AND f2=''", NormalizeHumanReadable));

    BOOST_REQUIRE_NO_THROW(
        normalizeSqlQuery(
            L"f1='N AND f2=''", NormalizeCompact));

    BOOST_REQUIRE_NO_THROW(
        normalizeSqlQuery(
            L"f1='N AND f2=''", NormalizeHumanReadable));

    std::wstring result;
    BOOST_REQUIRE_NO_THROW(
        result = normalizeSqlQuery(sqlOriginal1, NormalizeCompact));
    BOOST_CHECK(result == sqlCompactExpected);

    BOOST_REQUIRE_NO_THROW(
        result = normalizeSqlQuery(sqlOriginal2, NormalizeCompact));
    BOOST_CHECK(result == sqlCompactExpected);

    BOOST_REQUIRE_NO_THROW(
        result = normalizeSqlQuery(sqlOriginal1, NormalizeHumanReadable));
    BOOST_CHECK(result == sqlHumanExpected1);

    BOOST_REQUIRE_NO_THROW(
        result = normalizeSqlQuery(sqlOriginal2, NormalizeHumanReadable));
    BOOST_CHECK(result == sqlHumanExpected2);
}

test_suite* test::strings::init_suite()
{
    test_suite* suite = BOOST_TEST_SUITE("Strings test suite");

    suite->add(
        BOOST_TEST_CASE(&normalizeSqlQueryTest));

    return suite;
}
