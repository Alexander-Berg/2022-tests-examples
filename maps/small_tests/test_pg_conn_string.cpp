#include <yandex/maps/wiki/common/pg_connection_string.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(pg_conn_string) {

Y_UNIT_TEST(test_pasre_empty_string)
{
    UNIT_ASSERT_NO_EXCEPTION(PGConnectionString(""));
}

Y_UNIT_TEST(test_pasre_garbage_string)
{
    UNIT_ASSERT_NO_EXCEPTION(PGConnectionString("asdasd"));
}

Y_UNIT_TEST(test_pasre_valid_string)
{
    const std::string& mapspro =
        "host=um-pgs01h.tst.maps.yandex.ru port=5432"
        " user=mapspro password=mapspro dbname=mapspro";
    PGConnectionString connStr(mapspro);
    UNIT_ASSERT_STRINGS_EQUAL(connStr.optionValue("port"), "5432");
    UNIT_ASSERT(connStr.isSet("user"));
    UNIT_ASSERT(!connStr.isSet("schema"));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
