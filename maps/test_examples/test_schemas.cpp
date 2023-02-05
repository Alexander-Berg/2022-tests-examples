#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yandex/maps/wiki/unittest/json_schema.h>

#include <string>

namespace {
const std::string SCHEMAS_PATH_BASE = SRC_("..");
}

Y_UNIT_TEST_SUITE(social_schemas)
{
Y_UNIT_TEST(test_main)
{
    maps::wiki::unittest::validateJsonDir(SCHEMAS_PATH_BASE + "/examples", SCHEMAS_PATH_BASE);
}

Y_UNIT_TEST(test_source_context)
{
    maps::wiki::unittest::validateJsonDir(SCHEMAS_PATH_BASE + "/source_contexts/examples", SCHEMAS_PATH_BASE + "/source_contexts");
}

} // Y_UNIT_TEST_SUITE(editor_schemas)

