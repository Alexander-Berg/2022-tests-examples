#include <maps/infra/yacare/include/test_utils.h>

#include <maps/libs/http/include/http.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace yacare::tests {

using maps::http::URL;
using maps::http::GET;
using maps::http::MockRequest;
using maps::http::MockResponse;

// Fixtures for userId and userInfo allow testing user auth
// without settings up Blackbox and TVM mocks

Y_UNIT_TEST_SUITE(user_auth_params_fixtures) {

// UserIdHeaderFixture allows passing plain uid through request header
Y_UNIT_TEST(test_user_id_in_headers)
{
    UserIdHeaderFixture userIdHeaderFixture;
    MockRequest request(GET, URL("http://localhost/user"));
    std::string userId = "111111111";
    request.headers[USER_ID_HEADER] = userId;

    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.body, "uid=" + userId);
}

// UserInfoFixture sets global mock userInfo value
Y_UNIT_TEST(test_user_info_in_fixture)
{
    const std::string uid = "111";
    const std::string login = "test";
    maps::auth::UserInfo userInfo;
    userInfo.setUid(uid);
    userInfo.setLogin(login);
    UserInfoFixture fixture(userInfo);

    MockRequest request(GET, URL("http://localhost/user_info"));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.body, "userinfo: {uid:" + uid + ", login:" + login + "}");
}

Y_UNIT_TEST() {
    UserIdHeaderFixture userIdHeaderFixture;
    MockRequest request(GET, URL("http://localhost/log_user_id"));
    std::string userId = "111111111";
    request.headers[USER_ID_HEADER] = userId;

    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.body, "uid = " + userId);
}

} // Y_UNIT_TEST_SUITE

} //namespace yacare::tests
