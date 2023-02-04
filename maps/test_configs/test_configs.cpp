#include <maps/wikimap/feedback/api/src/yacare/lib/globals.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <string>

namespace maps::wiki::feedback::api::test_configs {

const std::string CONFIGS_DIR =
    "../docker/install/etc/template_generator/templates/"
    "etc/yandex/maps/feedback_api";

Y_UNIT_TEST_SUITE(test_configs)
{

Y_UNIT_TEST(globals)
{
    {
        UNIT_ASSERT_NO_EXCEPTION(Globals::create(
            SRC_(CONFIGS_DIR + "/feedback_api.stable.conf"),
            true /* dryRun */));
    }
    {
        UNIT_ASSERT_NO_EXCEPTION(Globals::create(
            SRC_(CONFIGS_DIR + "/feedback_api.testing.conf"),
            true /* dryRun */));
    }
    {
        UNIT_ASSERT_NO_EXCEPTION(Globals::create(
            SRC_(CONFIGS_DIR + "/feedback_api.unstable.conf"),
            true /* dryRun */));
    }
}

} // Y_UNIT_TEST_SUITE(test_configs)

} // namespace maps::wiki::feedback::api::test_configs
