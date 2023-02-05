#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yandex/maps/wiki/unittest/json_schema.h>

#include <string>

namespace {
const std::string SCHEMAS_PATH_BASE = SRC_(".");
}

Y_UNIT_TEST_SUITE(renderer_overlay_schemas)
{

Y_UNIT_TEST(test_all)
{
    maps::wiki::unittest::validateJsonDir(SCHEMAS_PATH_BASE + "/examples", SCHEMAS_PATH_BASE);
}

} // Y_UNIT_TEST_SUITE(acl_schemas)
