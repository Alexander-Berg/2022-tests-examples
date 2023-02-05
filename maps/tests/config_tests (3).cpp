#include <maps/wikimap/infopoints_hypgen/libs/config/include/config.h>

#include <maps/libs/vault_boy/include/secrets.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <string>

const std::string CONFIGS_PATH = "/maps/wikimap/infopoints_hypgen/libs/config/configs/";

namespace hypgen = maps::wiki::infopoints_hypgen;


Y_UNIT_TEST_SUITE(Tests)
{
    Y_UNIT_TEST(ParseConfigs)
    {
        maps::vault_boy::RandomContext cxt;
        for (const std::string env: {"development", "testing", "production"}) {
            hypgen::config::fromFS(
                cxt,
                ArcadiaSourceRoot() + CONFIGS_PATH + "tconfig." + env + ".conf"
            );
        }
    }
};

