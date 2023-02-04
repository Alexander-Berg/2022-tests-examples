#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/common/include/exception.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/toloka/include/utils.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(utils_tests)
{

Y_UNIT_TEST(extract_bld_id_npro)
{
    http::URL url("https://npro.maps.yandex.ru/#!/objects/101746877?z=20&ll=39.773814%2C47.250930&l=nk%23sat");

    EXPECT_EQ(extractBldId(url), 101746877);
}

Y_UNIT_TEST(extract_bld_id_mpro)
{
    http::URL url("https://mpro.maps.yandex.ru/?ll=46.103494%2C43.347648&z=19&id=1544972247");

    EXPECT_EQ(extractBldId(url), 1544972247);
}

Y_UNIT_TEST(extract_bld_id_nmaps)
{
    http::URL url("https://n.maps.yandex.ru/#!/objects/774138315?z=20&ll=73.289085%2C55.029070&l=nk%23sat");

    EXPECT_EQ(extractBldId(url), 774138315);
}

Y_UNIT_TEST(extract_bld_id_unknown)
{
    http::URL url("https://unknown.maps.yandex.ru/?ll=46.103494%2C43.347648&z=19&id=1544972247");

    EXPECT_THROW(extractBldId(url), maps::RuntimeError);
}


} // Y_UNIT_TEST_SUITE(utils_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
