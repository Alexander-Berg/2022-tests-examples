#include "tvmtool_fixture.h"
#include <maps/libs/auth/include/blackbox.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::auth::tests {

Y_UNIT_TEST_SUITE(blackbox_api) {

Y_UNIT_TEST(blackbox_auto_selection)
{
    // No blackbox in tvmtool.conf
    EXPECT_THROW(
        BlackboxApi(
            TvmtoolSettings(
                TvmtoolSettings::Paths()
                    .setConfigPath(common::joinPath(ArcadiaSourceRoot(), MULTICLIENT_TVM_CONFIG_FILE))
            ).selectClientAlias("me")),
        RuntimeError
    );

    // Incorrect client selected
    EXPECT_THROW(
        BlackboxApi(Fixture().tvmtoolSettings().selectClientAlias("NoSuchClient")),
        RuntimeError
    );

    // Incorrect blackbox selected
    EXPECT_THROW(
        BlackboxApi(Fixture().tvmtoolSettings()
            .selectClientAlias("myself")
            .setBlackboxAlias("NoSuchBlackbox")),
        RuntimeError
    );

    // Client selected successfully
    auto tvmsettings = Fixture().tvmtoolSettings()
            .selectClientAlias("myself")
            .setBlackboxAlias("blackbox");
    auto blackbox = BlackboxApi(tvmsettings);
    EXPECT_EQ(blackbox.environment(), BLACKBOX_UNITTEST);
    EXPECT_TRUE(blackbox.maybeTvmtoolSettings());
    EXPECT_TRUE(!blackbox.fetchTvmServiceTicket().empty());
}

Y_UNIT_TEST(blackbox_manual_selection)
{
    // work with tvmtool recipe
    auto tvmSettings = Fixture().tvmtoolSettings().selectClientAlias("myself");

    // TvmClient can't issue ticket for required blackbox instance
    EXPECT_THROW(
        BlackboxApi(BLACKBOX_PROD, tvmSettings.makeTvmClient()),
        NTvmAuth::TBrokenTvmClientSettings
    );

    // Successful initialization
    auto blackbox = BlackboxApi(BLACKBOX_UNITTEST, tvmSettings.makeTvmClient());
    EXPECT_EQ(blackbox.environment(), BLACKBOX_UNITTEST);
    EXPECT_FALSE(blackbox.maybeTvmtoolSettings());
    EXPECT_TRUE(!blackbox.fetchTvmServiceTicket().empty());
}

Y_UNIT_TEST(query_has_service_ticket)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());

    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [&](const http::MockRequest& request) {
            EXPECT_EQ(
                request.headers.at("X-Ya-Service-Ticket"),
                blackboxApi.fetchTvmServiceTicket()
            );
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.good");
        });

    EXPECT_NO_THROW(
        blackboxApi.oauthQuery()
            .setToken("b1070fac00ce46388a2f2645dfff09c6")
            .setRemoteAddress("127.0.0.1")
            .execute()
    );
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::auth::tests
