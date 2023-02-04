#include <maps/factory/services/sputnica_back/tests/common/common.h>
#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>

#include <maps/libs/http/include/test_utils.h>
#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sputnica::tests {

namespace {

const std::string FACTORY_RENDERER_HOST = "core-factory-renderer.maps.yandex.net";

} // namespace

Y_UNIT_TEST_SUITE_F(proxy_render_api_tests, Fixture) {

Y_UNIT_TEST(test_proxy_successful_request)
{
    const std::string TEST_RESPONSE = "hello world";
    const int TEST_STATUS = 200;

    // maps input urls to expected proxy urls
    std::vector<std::pair<http::URL, http::URL>> testRequests{
        {
            {"http://localhost/render/mosaic-sources/interior-tiles?x=1&y=2&z=3&filter=t"},
            {"http://localhost/v1/rendering/render_satellite_internal?x=1&y=2&z=3&filter=t"}
        },
        {
            {"http://localhost/render/aois/tiles?x=1&y=2&z=3&filter=t&scale=1.0"},
            {"http://localhost/v1/rendering/render_boundary_layers_internal?l=aois&x=1&y=2&z=3&filter=t&scale=1.0"}
        },
        {
            {"http://localhost/render/mosaic-sources/boundary-tiles?x=1&y=2&z=3&filter=t&scale=1.0"},
            {"http://localhost/v1/rendering/render_boundary_layers_internal?l=mosaic_sources&x=1&y=2&z=3&filter=t&scale=1.0"}
        },
        {
            {"http://localhost/render/mosaic-sources/hotspots?x=1&y=2&z=3&filter=t&callback=e"},
            {"http://localhost/v1/rendering/hotspot_boundary_layers_internal?l=mosaic_sources&x=1&y=2&z=3&filter=t&callback=e"}
        }
    };

    for (const auto& requestsPair: testRequests) {
        const auto& testRequest = requestsPair.first;
        const auto& expectedRequest = requestsPair.second;
        auto mockHandle = http::addMock(
            http::URL(expectedRequest).setHost(FACTORY_RENDERER_HOST),
            [&](const http::MockRequest& request) {
                EXPECT_EQ(expectedRequest.params(), request.url.params());
                http::MockResponse response(TEST_RESPONSE);
                response.status = TEST_STATUS;
                return response;
            });
        http::MockRequest rq(
            http::GET,
            http::URL(testRequest)
                .addParam("extra_param", "extra")
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        EXPECT_EQ(resp.status, TEST_STATUS);
        EXPECT_EQ(resp.body, TEST_RESPONSE);
    }
}

Y_UNIT_TEST(test_proxy_response_status)
{
    const std::string testUrl = "http://localhost/render/mosaic-sources/interior-tiles?x=1&y=2&z=3&filter=t";
    const std::string expectedProxyUrl =
        "http://localhost/v1/rendering/render_satellite_internal?x=1&y=2&z=3&filter=t";
    const int TEST_STATUS = 201;

    auto mockHandle = http::addMock(
        http::URL(expectedProxyUrl).setHost(FACTORY_RENDERER_HOST),
        [&](const http::MockRequest&) {
            http::MockResponse response;
            response.status = TEST_STATUS;
            return response;
        });

    http::MockRequest rq(http::GET, http::URL(testUrl));
    setAuthHeaderFor(db::Role::Viewer, rq);
    auto resp = yacare::performTestRequest(rq);
    EXPECT_EQ(resp.status, TEST_STATUS);
}

Y_UNIT_TEST(test_proxy_failed_request)
{
    const std::string testUrl = "http://localhost/render/mosaic-sources/interior-tiles?x=1&y=2&z=3&filter=t";
    const std::string expectedProxyUrl =
        "http://localhost/v1/rendering/render_satellite_internal?x=1&y=2&z=3&filter=t";
    const std::string testResponseBody = "sensitive_response";

    auto mockHandle = http::addMock(
        http::URL(expectedProxyUrl).setHost(FACTORY_RENDERER_HOST),
        [&](const http::MockRequest&) {
            http::MockResponse response;
            response.status = 504;
            response.body = testResponseBody;
            return response;
        });

    http::MockRequest rq(http::GET, http::URL(testUrl));
    setAuthHeaderFor(db::Role::Viewer, rq);
    auto resp = yacare::performTestRequest(rq);
    EXPECT_EQ(resp.status, 500);
    EXPECT_TRUE(resp.body.find(testResponseBody) == std::string::npos);
}

Y_UNIT_TEST(test_proxy_transfer_headers)
{
    std::vector<std::string> FORWARD_HEADERS{
        "If-None-Match", "If-Modified-Since", "Cache-Control"};
    std::vector<std::string> BACKWARD_HEADERS{
        "Cache-Control",
        "ETag",
        "Last-Modified",
        "Content-Type",
        "Expires"};

    const std::string testUrl = "http://localhost/render/mosaic-sources/interior-tiles?x=1&y=2&z=3&filter=t";
    const std::string expectedProxyUrl =
        "http://localhost/v1/rendering/render_satellite_internal?x=1&y=2&z=3&filter=t";

    auto mockHandle = http::addMock(
        http::URL(expectedProxyUrl).setHost(FACTORY_RENDERER_HOST),
        [&](const http::MockRequest& rq) {
            for (const auto& header: FORWARD_HEADERS) {
                SCOPED_TRACE(header);
                EXPECT_TRUE(rq.headers.count(header));
                EXPECT_EQ(rq.headers.at(header), header);
            }

            http::MockResponse response("hello world");
            for (const auto& header: BACKWARD_HEADERS) {
                response.headers.emplace(header, header);
            }
            return response;
        });

    http::MockRequest rq(http::GET, http::URL(testUrl));

    for (const auto& header: FORWARD_HEADERS) {
        rq.headers[header] = header;
    }

    setAuthHeaderFor(db::Role::Viewer, rq);
    auto resp = yacare::performTestRequest(rq);
    EXPECT_EQ(resp.status, 200);
    for (const auto& header: BACKWARD_HEADERS) {
        EXPECT_EQ(resp.headers.at(header), header);
    }
}

Y_UNIT_TEST(test_proxy_anauthorized_request)
{
    const std::string testUrl = "http://localhost/render/mosaic-sources/interior-tiles?x=1&y=2&z=3&filter=t";
    const std::string expectedProxyUrl =
        "http://localhost/v1/rendering/render_satellite_internal?x=1&y=2&z=3&filter=t";

    auto mockHandle = http::addMock(
        http::URL(expectedProxyUrl).setHost(FACTORY_RENDERER_HOST),
        [&](const http::MockRequest&) {
            return http::MockResponse("hello world");
        });

    http::MockRequest rq(http::GET, http::URL(testUrl));
    auto resp = yacare::performTestRequest(rq);
    EXPECT_EQ(resp.status, 401);
}

Y_UNIT_TEST(test_proxy_check_permissions)
{
    const std::string testUrl = "http://localhost/render/mosaic-sources/interior-tiles?x=1&y=2&z=3&filter=t";
    const std::string expectedProxyUrl =
        "http://localhost/v1/rendering/render_satellite_internal?x=1&y=2&z=3&filter=t";

    auto mockHandle = http::addMock(
        http::URL(expectedProxyUrl).setHost(FACTORY_RENDERER_HOST),
        [&](const http::MockRequest&) {
            return http::MockResponse("hello world");
        });

    http::MockRequest rq(http::GET, http::URL(testUrl));
    rq.headers[yacare::tests::USER_ID_HEADER] = "1";
    auto resp = yacare::performTestRequest(rq);
    EXPECT_EQ(resp.status, 403);
}

}

} // namespace maps::factory::sputnica::tests
