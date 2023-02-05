#include <maps/wikimap/jams_arm2/libs/config/include/config.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <string>

namespace arm = maps::wiki::jams_arm2;


Y_UNIT_TEST_SUITE(Tests)
{
    Y_UNIT_TEST(ParseClosures)
    {
        for (const std::string env: {"default", "unstable", "testing", "production"}) {
            arm::config::fromFS(ArcadiaSourceRoot() + "/maps/wikimap/jams_arm2/libs/config/configs/tconfig." + env + ".conf");
        }
    }
};
