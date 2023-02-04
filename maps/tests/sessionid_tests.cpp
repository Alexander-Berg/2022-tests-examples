#include "tvmtool_fixture.h"

#include <maps/libs/auth/include/blackbox.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::auth::tests {

Y_UNIT_TEST_SUITE(sessionid) {

Y_UNIT_TEST(good)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/sessionid.good");
        });

    auto info = blackboxApi.sessionIdQuery()
        .setSessionId("3:valid:sessionid")
        .setHost("yandex.ru")
        .setRemoteAddress("127.0.0.1")
        .execute();

    EXPECT_EQ(info.uid(), "4001517835");
    EXPECT_EQ(info.login(), "test.user");
    EXPECT_TRUE(info.yandexTeamLogin());
    // Expect bb:sessionid scope for user authorized by sessionid cookie
    EXPECT_EQ(info.scopes(), UserInfo::Scopes{SESSIONID_SCOPE});
    EXPECT_TRUE(info.phones().empty());
    EXPECT_TRUE(info.emails().empty());

    // No user ticket if not requested explicitly
    EXPECT_THROW(
        info.userTicket(),
        LogicError
    );
}

Y_UNIT_TEST(good_user_ticket)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/sessionid.good_user_ticket");
        });

    auto info = blackboxApi.sessionIdQuery()
        .setSessionId("3:valid:sessionid")
        .setHost("yandex.ru")
        .setRemoteAddress("127.0.0.1")
        .setQueryParams(BlackboxQueryParams().requestUserTicket())
        .execute();

    EXPECT_EQ(info.uid(), "4001517153");
    EXPECT_EQ(info.login(), "john.doe");
    // Check user ticket present
    EXPECT_EQ(info.userTicket(), "3:user:ValidTicket");
}

Y_UNIT_TEST(good_getphones_getemails)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/sessionid_getphones_getemails.good");
        });

    auto info = blackboxApi.sessionIdQuery()
        .setSessionId("3:valid:sessionid")
        .setHost("yandex.ru")
        .setRemoteAddress("127.0.0.1")
        .setQueryParams(BlackboxQueryParams().requestBoundPhones())
        .execute();

    EXPECT_EQ(info.uid(), "4001517835");
    EXPECT_EQ(info.login(), "test.user");
    EXPECT_TRUE(info.yandexTeamLogin());
    EXPECT_EQ(info.scopes(), UserInfo::Scopes{SESSIONID_SCOPE});

    EXPECT_EQ(info.phones().size(), 2u);

    const auto& phone1 = info.phones().at(0);
    EXPECT_EQ(phone1.id(), "2");
    EXPECT_EQ(phone1.attributes().size(), 0u);

    const auto& phone2 = info.phones().at(1);
    EXPECT_EQ(phone2.id(), "3");
    EXPECT_EQ(phone2.attributes().size(), 0u);

    EXPECT_EQ(info.emails().size(), 1u);

    const auto& email = info.emails().at(0);
    EXPECT_EQ(email.id(), "1");
    EXPECT_EQ(email.attributes().size(), 0u);
}

Y_UNIT_TEST(good_getphones_getemails_attributes)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/sessionid_getphones_getemails_attributes.good");
        });

    auto info = blackboxApi.sessionIdQuery()
        .setSessionId("3:valid:sessionid")
        .setHost("yandex.ru")
        .setRemoteAddress("127.0.0.1")
        .setQueryParams(
            BlackboxQueryParams()
                .requestBoundPhones(
                    {
                        PhoneAttributeType::Number,
                        PhoneAttributeType::Created,
                    }
                )
        )
        .execute();

    EXPECT_EQ(info.uid(), "4001517835");
    EXPECT_EQ(info.login(), "test.user");
    EXPECT_TRUE(info.yandexTeamLogin());
    EXPECT_EQ(info.scopes(), UserInfo::Scopes{SESSIONID_SCOPE});

    EXPECT_EQ(info.phones().size(), 2u);

    const auto& phone1 = info.phones().at(0);
    EXPECT_EQ(phone1.id(), "2");
    EXPECT_EQ(phone1.attributes().size(), 4u);
    EXPECT_EQ(phone1.attributes().at(0).type, PhoneAttributeType::Number);
    EXPECT_EQ(phone1.attributes().at(0).value, "79161112233");

    const auto& phone2 = info.phones().at(1);
    EXPECT_EQ(phone2.id(), "3");
    EXPECT_EQ(phone2.attributes().size(), 2u);

    EXPECT_EQ(info.emails().size(), 1u);

    const auto& email = info.emails().at(0);
    EXPECT_EQ(email.id(), "1");
    // TODO: Finish writing tests, put on review.
    EXPECT_EQ(email.attributes().size(), 4u);
    EXPECT_EQ(email.attributes().at(0).type, EmailAttributeType::Address);
    EXPECT_EQ(email.attributes().at(0).value, "sample@yandex.ru");
    EXPECT_EQ(email.attributes().at(1).type, EmailAttributeType::Created);
    EXPECT_EQ(email.attributes().at(1).value, "1431572100");
    EXPECT_EQ(email.attributes().at(2).type, EmailAttributeType::Confirmed);
    EXPECT_EQ(email.attributes().at(2).value, "1431572505");
    EXPECT_EQ(email.attributes().at(3).type, EmailAttributeType::Bound);
    EXPECT_EQ(email.attributes().at(3).value, "1431574535");
}

Y_UNIT_TEST(need_reset)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                    "maps/libs/auth/tests/responses/sessionid.need_reset");
        });

    auto info = blackboxApi.sessionIdQuery()
            .setSessionId("3:needreset:sessionid:")
            .setHost("yandex.ru")
            .setRemoteAddress("127.0.0.1")
            .execute();

    EXPECT_EQ(info.uid(), "777777777");
    EXPECT_EQ(info.login(), "john.doe");
    EXPECT_FALSE(info.yandexTeamLogin());
}

Y_UNIT_TEST(invalid)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/sessionid.invalid");
        });

    EXPECT_THROW(
        blackboxApi.sessionIdQuery()
            .setSessionId("153")
            .setHost("yandex.ru")
            .setRemoteAddress("127.0.0.1")
            .execute(),
        errors::Unauthorized
    );
}

Y_UNIT_TEST(expired)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/sessionid.expired");
        });

    EXPECT_THROW(
        blackboxApi.sessionIdQuery()
            .setSessionId("3:valid:sessionid")
            .setHost("yandex.ru")
            .setRemoteAddress("127.0.0.1")
            .execute(),
        errors::Unauthorized
    );
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::auth::tests
