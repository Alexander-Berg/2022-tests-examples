#include <maps/carparks/renderer/yacare/lib/config.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

namespace maps::carparks::renderer::tests {

namespace {

const std::string SERVICE_CONFIG =
    BuildRoot() + "/maps/carparks/renderer/yacare/lib/tests/data/service.conf";

} // namespace

Y_UNIT_TEST_SUITE(config_tests) {

Y_UNIT_TEST(usePedestrianGraph)
{
    Config config(SERVICE_CONFIG);
    EXPECT_TRUE(config.usePedestrianGraph());
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::carparks::renderer::tests
