#include "tvmtool_fixture.h"

#include <maps/libs/auth/include/blackbox.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::auth::tests {

Y_UNIT_TEST_SUITE(oauth) {

Y_UNIT_TEST(good_response)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());

    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.good");
        });

    auto info = blackboxApi.oauthQuery()
            .setToken("valid:token:value")
            .setRemoteAddress("127.0.0.1")
            .execute();

    EXPECT_EQ(info.uid(), "111111111");
    EXPECT_EQ(info.login(), "test_user.login");
    EXPECT_FALSE(info.yandexTeamLogin());
    EXPECT_EQ(
        info.scopes(),
        UserInfo::Scopes({"fotki:read", "fotki:update", "login:birthday", "login:email", "login:info", "mobile:all"})
    );
    // No user ticket if not requested explicitly
    EXPECT_THROW(
        info.userTicket(),
        LogicError
    );
}

Y_UNIT_TEST(good_employee_response)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.good_employee");
        });

    auto info = blackboxApi.oauthQuery()
        .setToken("b1070fac00ce46388a2f2645dfff09c6")
        .setRemoteAddress("127.0.0.1")
        .execute();

    EXPECT_EQ(info.uid(), "111111111");
    EXPECT_EQ(info.login(), "test_user.login");
    ASSERT_TRUE(info.yandexTeamLogin());
    EXPECT_EQ(*info.yandexTeamLogin(), "test_yandex_employee.login");
    EXPECT_EQ(info.scopes(), (UserInfo::Scopes{"mobile:all", "yaparking:all"}));
}

Y_UNIT_TEST(beta_tester_response)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.ext_tester");
        });

    auto info = blackboxApi.oauthQuery()
        .setToken("b1070fac00ce46388a2f2645dfff09c6")
        .setRemoteAddress("127.0.0.1")
        .execute();

    EXPECT_FALSE(info.yandexTeamLogin());
    EXPECT_TRUE(info.hasBugIcon());
}

Y_UNIT_TEST(missing_argument)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.missing_userip");
        });

    EXPECT_THROW(
        blackboxApi.oauthQuery()
            .setToken("b1070fac00ce46388a2f2645dfff09c6")
            .execute(),
        errors::InternalError
    );
}

Y_UNIT_TEST(expired_response)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.expired");
        });

    EXPECT_THROW(
        blackboxApi.oauthQuery()
            .setToken("b1070fac00ce46388a2f2645dfff09c6")
            .setRemoteAddress("127.0.0.1")
            .execute(),
        errors::Unauthorized
    );
}

Y_UNIT_TEST(disabled_account)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.disabled");
        });

    EXPECT_THROW(
        blackboxApi.oauthQuery()
            .setToken("valid-token")
            .setRemoteAddress("127.0.0.1")
            .execute(),
        errors::Unauthorized
    );
}

Y_UNIT_TEST(good_user_ticket)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.good_user_ticket");
        });

    auto info = blackboxApi.oauthQuery()
        .setToken("b1070fac00ce46388a2f2645dfff09c6")
        .setRemoteAddress("::1")
        .setQueryParams(BlackboxQueryParams().requestUserTicket())
        .execute();

    EXPECT_EQ(info.userTicket(), "3:user:ValidTicket");
    EXPECT_EQ(info.uid(), "220992719");
    EXPECT_EQ(info.scopes(), (UserInfo::Scopes{"mobile:all"}));
}

Y_UNIT_TEST(service_ticket_expired)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/oauth.expired_service_ticket");
        });

    EXPECT_THROW(
        blackboxApi.oauthQuery()
            .setToken("b1070fac00ce46388a2f2645dfff09c6")
            .setRemoteAddress("::1")
            .setQueryParams(BlackboxQueryParams().requestUserTicket())
            .execute(),
        errors::InternalError
    );
}

Y_UNIT_TEST(service_ticket_malformed)
{
    constexpr auto serviceTicket = "3:serv:MalformedServiceTicket";
    auto blackboxEnv = BLACKBOX_UNITTEST;
    auto blackboxMock = http::addMock(
        blackboxEnv.url,
        [&](const http::MockRequest& request) {
            EXPECT_EQ(request.headers.at("X-Ya-Service-Ticket"), serviceTicket);
            return http::MockResponse::fromArcadia(
                 "maps/libs/auth/tests/responses/oauth.malformed_service_ticket");
        });

    // Manually construct query
    EXPECT_THROW(
        BlackboxApi::OAuthQuery(blackboxEnv, serviceTicket)
            .setToken("b1070fac00ce46388a2f2645dfff09c6")
            .setRemoteAddress("::1")
            .setQueryParams(BlackboxQueryParams().requestUserTicket())
            .execute(),
         errors::InternalError
    );
}

Y_UNIT_TEST(service_ticket_missing)
{
    auto blackboxEnv = BLACKBOX_UNITTEST;
    auto blackboxMock = http::addMock(
        blackboxEnv.url,
        [](const http::MockRequest& request) {
            // No service ticket in request
            EXPECT_FALSE(request.headers.count("X-Ya-Service-Ticket"));
            return http::MockResponse::fromArcadia(
                 "maps/libs/auth/tests/responses/oauth.missing_service_ticket");
    });

    // Manually construct query
    EXPECT_THROW(
        BlackboxApi::OAuthQuery(blackboxEnv, {})
            .setToken("b1070fac00ce46388a2f2645dfff09c6")
            .setRemoteAddress("::1")
            .setQueryParams(BlackboxQueryParams().requestUserTicket())
            .execute(),
        errors::InternalError
    );
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::auth::tests
