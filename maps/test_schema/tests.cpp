#include <yandex/maps/wiki/unittest/unittest.h>
#include <yandex/maps/wiki/unittest/json_schema.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <set>
#include <string>

namespace maps::wiki::unittest::tests {

namespace {

const std::string SCHEMAS_PATH_BASE = SRC_("data");

} // namespace

Y_UNIT_TEST_SUITE(test_schema)
{

Y_UNIT_TEST(test_good)
{
    UNIT_ASSERT_NO_EXCEPTION(
        validateJsonDir(SCHEMAS_PATH_BASE + "/example_good", SCHEMAS_PATH_BASE));
}

Y_UNIT_TEST(test_bad)
{
    UNIT_ASSERT_EXCEPTION(
        validateJsonDir(SCHEMAS_PATH_BASE + "/example_bad", SCHEMAS_PATH_BASE),
        maps::Exception);
}

} // Y_UNIT_TEST_SUITE

} //namespace maps::wiki::unittest::tests
