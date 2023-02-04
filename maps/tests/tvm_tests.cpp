#include "tvmtool_fixture.h"

#include <maps/libs/auth/include/tvm.h>
#include <maps/libs/auth/include/blackbox_environment.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::auth::tests {

const auto RECIPE_TVM_CONFIG_CONTENTS = json::Value::fromFile(
        common::joinPath(ArcadiaSourceRoot(), RECIPE_TVM_CONFIG_FILE));

Y_UNIT_TEST_SUITE(tvm)
{

Y_UNIT_TEST(make_client_from_json)
{
    TvmtoolSettings tvmSettings(RECIPE_TVM_CONFIG_CONTENTS);

    EXPECT_NO_THROW(tvmSettings.setAuthToken(maps::common::readFileToString(Fixture::RECIPE_AUTHTOKEN_FILE))
        .setPort(std::stoi(maps::common::readFileToString(Fixture::RECIPE_PORT_FILE))).makeTvmClient());
}

Y_UNIT_TEST(make_client)
{
    Fixture fix;

    // autoselect client alias
    EXPECT_NO_THROW(fix.tvmtoolSettings().makeTvmClient());

    // explicit client alias
    EXPECT_NO_THROW(fix.tvmtoolSettings().selectClientAlias("myself").makeTvmClient());

    // incorrect client alias
    EXPECT_THROW(
        fix.tvmtoolSettings().selectClientAlias("incorrect-self-alias").makeTvmClient(),
        std::exception);
}

Y_UNIT_TEST(recipe_config)
{
    Fixture fix;

    // autoselected client
    EXPECT_EQ(
        fix.tvmtoolSettings().clientConfigContents(),
        fix.tvmtoolSettings().selectClientAlias("myself").clientConfigContents()
    );

    // incorrect client alias
    EXPECT_THROW(
        fix.tvmtoolSettings()
            .selectClientAlias("incorrect-client-alias").clientConfigContents(),
        std::exception);

    // Blackbox default alias
    EXPECT_EQ(fix.tvmtoolSettings().blackboxTvmId(), BLACKBOX_UNITTEST.tvmId);
    // Blackbox explicit alias
    EXPECT_EQ(
        fix.tvmtoolSettings().setBlackboxAlias("blackbox").blackboxTvmId(),
        BLACKBOX_UNITTEST.tvmId);
    // Blackbox bad alias
    EXPECT_THROW(
        fix.tvmtoolSettings().setBlackboxAlias("no-such-blackbox").blackboxTvmId(),
        std::exception);
}

Y_UNIT_TEST(multiclient_config)
{
    // NB: tvmtool recipe not running with this config
    TvmtoolSettings tvmtoolSettings(
        TvmtoolSettings::Paths().setConfigPath(
            common::joinPath(ArcadiaSourceRoot(), MULTICLIENT_TVM_CONFIG_FILE))
    );

    // client autoselection fails
    EXPECT_THROW(tvmtoolSettings.clientConfigContents(), std::exception);

    // explicit client selection works
    const auto& client1Section =
            tvmtoolSettings.selectClientAlias("me").clientConfigContents();
    EXPECT_EQ(client1Section["self_tvm_id"].as<TvmId>(), static_cast<TvmId>(100500));

    const auto& client2Section =
            tvmtoolSettings.selectClientAlias("me2").clientConfigContents();
    EXPECT_EQ(client2Section["self_tvm_id"].as<TvmId>(), static_cast<TvmId>(100501));

    // Port read from config
    EXPECT_EQ(tvmtoolSettings.port(), 153);

    // No blackbox config destinations
    EXPECT_THROW(
        tvmtoolSettings.selectClientAlias("me").blackboxTvmId(),
        std::exception);
}

Y_UNIT_TEST(nonexistent_config)
{
    Fixture fix;
    auto paths = fix.tvmtoolPaths();
    EXPECT_THROW(
        TvmtoolSettings(paths.setConfigPath("no-such-tvmtool.conf")),
        std::exception);
}

Y_UNIT_TEST(nonexistent_token_file)
{
    Fixture fix;
    auto paths = fix.tvmtoolPaths();

    // No token file is allowed
    auto settings = TvmtoolSettings(paths.setTokenPath("no-such-authtoken"));
    // But can't makeTvmClient, since token no set through environment either
    EXPECT_THROW(
        settings.makeTvmClient(),
        std::exception);
}

} // Y_UNIT_TEST_SUITE(make_tvm_client)
} // namespace maps::auth::tests
