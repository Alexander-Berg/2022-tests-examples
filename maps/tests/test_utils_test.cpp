#include <maps/infra/yacare/include/response.h>
#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace yacare::tests {

using maps::http::URL;
using maps::http::GET;
using maps::http::POST;
using maps::http::MockRequest;

/*
 * WARN: all handlers being requested are described in
 *       yacare/testapp/lib
 */
Y_UNIT_TEST_SUITE(test_mock_frontend) {

Y_UNIT_TEST(test_404) {
    MockRequest mockRequest(GET, URL("http://localhost/nonexistent"));
    auto response = performTestRequest(mockRequest);
    EXPECT_EQ(response.status, 404);
}

Y_UNIT_TEST(test_custom_exception) {
    MockRequest mockRequest(GET, URL("http://localhost/custom_exception"));
    auto response = performTestRequest(mockRequest);
    EXPECT_EQ(response.status, 422);
}

Y_UNIT_TEST(test_custom_error_reporter) {
    yacare::setErrorReporter([](const Request&, Response& response) {
        response.setStatus(418);
    });
    MockRequest mockRequest(GET, URL("http://localhost/exception"));
    auto response = performTestRequest(mockRequest);
    EXPECT_EQ(response.status, 418);
    yacare::setDefaultErrorReporter();
}

Y_UNIT_TEST(test_mtroute_ping) {
    MockRequest mockRequest(GET, URL("http://localhost/mtroute/ping"));
    auto response = performTestRequest(mockRequest);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.body, "pong\n");
}

Y_UNIT_TEST(test_bool) {
    MockRequest mockRequest(GET, URL("http://localhost/bool?flag=true"));
    auto response = performTestRequest(mockRequest);
    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.body, "bool, flag = true");
}

Y_UNIT_TEST(test_post_body) {
    MockRequest mockRequest(POST, URL("http://localhost/post_body"));
    mockRequest.body = "";
    auto emptyBodyResponse = performTestRequest(mockRequest);
    EXPECT_EQ(emptyBodyResponse.status, 200);
    EXPECT_EQ(emptyBodyResponse.body, "body = `'");

    mockRequest.body = "everybody is somebody";
    auto bodyFulResponse = performTestRequest(mockRequest);
    EXPECT_EQ(bodyFulResponse.status, 200);
    EXPECT_EQ(bodyFulResponse.body, "body = `everybody is somebody'");
}

Y_UNIT_TEST(test_zeroes) {
    MockRequest mockRequest(GET, URL("http://localhost/zeroes/20"));
    auto resp = performTestRequest(mockRequest);
    EXPECT_EQ(resp.body.size(), 20u);
    EXPECT_EQ(resp.body, std::string(20, '\0'));
}

Y_UNIT_TEST(test_http_headers) {
    MockRequest mockRequest(GET, URL("http://localhost/http_headers"));
    mockRequest.headers["Content-Type"] = "text/empty";
    mockRequest.headers["X-Best-Musical-Album-Ever"] = "Sgt. Pepper's Lonely Hearts Club Band";

    auto resp = performTestRequest(mockRequest);
    EXPECT_EQ(resp.body,
R"(HTTP_HOST = localhost
HTTP_X_BEST_MUSICAL_ALBUM_EVER = Sgt. Pepper's Lonely Hearts Club Band
)");
}

} //Y_UNIT_TEST_SUITE

} //namespace yacare::tests
