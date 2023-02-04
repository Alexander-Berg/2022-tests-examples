#include <maps/infra/yacare/include/test_utils.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace yacare::tests {

using maps::http::GET;
using maps::http::MockRequest;
using maps::http::MockResponse;

constexpr auto RECIPE_TVM_CONFIG_FILE = "maps/infra/auth_agent/tests/yacare_ut/tvmtool.recipe.conf";
UserAuthBlackboxFixture makeBlackboxMock(const std::string& responsePath)
{
    return UserAuthBlackboxFixture(
        RECIPE_TVM_CONFIG_FILE,
        tvm::DEFAULT_AUTH_METHODS,
        [=](const MockRequest&) {
            return MockResponse::fromArcadia(responsePath);
        }
    );
}

TEST (user_info, by_oauth_token) {
    auto mockAuth = makeBlackboxMock("maps/libs/auth/tests/responses/oauth.good_employee");
    maps::http::MockRequest request(maps::http::GET, "http://localhost/user_info");
    request.headers = {
        { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
    };

    const auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(
        response.body,
        "{"
            "\"uid\":\"111111111\","
            "\"login\":\"test_user.login\","
            "\"yandex_team_login\":\"test_yandex_employee.login\","
            "\"has_bug_icon\":false,"
            "\"scopes\":["
                "\"mobile:all\","
                "\"yaparking:all\""
            "]"
        "}"
    );
}

TEST (user_info, by_oauth_token_empty_phones) {
    auto mockAuth = makeBlackboxMock("maps/libs/auth/tests/responses/oauth.good_employee");
    maps::http::MockRequest request(maps::http::GET, "http://localhost/user_info?phones=true");
    request.headers = {
        { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
    };

    const auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(
        response.body,
        "{"
            "\"uid\":\"111111111\","
            "\"login\":\"test_user.login\","
            "\"yandex_team_login\":\"test_yandex_employee.login\","
            "\"has_bug_icon\":false,"
            "\"phones\":["
            "],"
            "\"scopes\":["
                "\"mobile:all\","
                "\"yaparking:all\""
            "]"
        "}"
    );
}

TEST (user_info, by_sessionid) {
    auto mockAuth = makeBlackboxMock("maps/libs/auth/tests/responses/sessionid_getphones_getemails_attributes.good");
    maps::http::MockRequest request(maps::http::GET, "http://localhost/user_info");
    request.headers = {
        { "Cookie", "someCookie=value; Session_id=3:valid:sessioncookie; otherCookie=value" },
        { "Host", "yandex.ru" }
    };

    const auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(
        response.body,
        "{"
            "\"uid\":\"4001517835\","
            "\"login\":\"test.user\","
            "\"yandex_team_login\":\"test_yandex_employee.login\","
            "\"has_bug_icon\":false,"
            "\"scopes\":["
                "\"bb:sessionid\""
            "]"
        "}"
    );
}

TEST (user_info, by_sessionid_with_phones) {
    auto mockAuth = makeBlackboxMock("maps/libs/auth/tests/responses/sessionid_getphones_getemails_attributes.good");
    maps::http::MockRequest request(maps::http::GET, "http://localhost/user_info?phones=true");
    request.headers = {
        { "Cookie", "someCookie=value; Session_id=3:valid:sessioncookie; otherCookie=value" },
        { "Host", "yandex.ru" }
    };

    const auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(
        response.body,
        "{"
            "\"uid\":\"4001517835\","
            "\"login\":\"test.user\","
            "\"yandex_team_login\":\"test_yandex_employee.login\","
            "\"has_bug_icon\":false,"
            "\"phones\":["
                "{"
                    "\"attributes\":{"
                        "\"1\":\"79161112233\","
                        "\"2\":\"1411571146\","
                        "\"4\":\"1411916746\","
                        "\"6\":\"1412183145\""
                    "},"
                    "\"id\":\"2\""
                "},"
                "{"
                    "\"attributes\":{"
                        "\"1\":\"79161111111\","
                        "\"2\":\"1412442345\""
                    "},"
                    "\"id\":\"3\""
                "}"
            "],"
            "\"scopes\":["
                "\"bb:sessionid\""
            "]"
        "}"
    );
}

} // namespace yacare::tests
