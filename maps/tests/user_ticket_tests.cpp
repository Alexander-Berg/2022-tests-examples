#include "tvmtool_fixture.h"

#include <maps/libs/auth/include/blackbox.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::auth::tests {

Y_UNIT_TEST_SUITE(user_ticket) {

Y_UNIT_TEST(good_response)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_ticket.good");
        });

    auto info = blackboxApi.userTicketQuery()
            .setUserTicket("3:user:ValidTicket")
            .execute();
    EXPECT_EQ(info.uid(), "3000062912");
    EXPECT_EQ(info.login(), "test");
    EXPECT_TRUE(info.yandexTeamLogin());
    EXPECT_FALSE(info.hasBugIcon());

    // No scopes in user_ticket method response
    EXPECT_THROW(info.scopes(), maps::LogicError);
    EXPECT_THROW(info.hasScope("bb::sessionid"), maps::LogicError);
}

Y_UNIT_TEST(expired)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_ticket.expired");
        });
    // Expired tickets results 'UNKNOWN' blackbox error, we treat it as internal err
    EXPECT_THROW(
        blackboxApi.userTicketQuery()
            .setUserTicket("3:user:ExpiredTicket")
            .execute(),
        errors::InternalError
    );
}

Y_UNIT_TEST(malformed)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                    "maps/libs/auth/tests/responses/user_ticket.malformed");
        });
    // Malformed tickets results 'UNKNOWN' blackbox error, we treat it as internal err
    EXPECT_THROW(
        blackboxApi.userTicketQuery()
            .setUserTicket("3:user:Malformed")
            .execute(),
        errors::InternalError
    );
}


Y_UNIT_TEST(no_blackbox_grants)
{
    // Don't forget to acquire grants for blackbox api
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_ticket.nogrants");
        });

    EXPECT_THROW(
        blackboxApi.userTicketQuery()
            .setUserTicket("3:user:ValidTicket")
            .execute(),
        errors::InternalError
    );
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::auth::tests
