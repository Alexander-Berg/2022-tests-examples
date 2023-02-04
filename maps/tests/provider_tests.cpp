#include <maps/infra/quotateka/server/tests/fixture.h>
#include <maps/infra/quotateka/server/tests/proto_utils.h>
#include <maps/infra/quotateka/proto/configuration.pb.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

namespace proto = yandex::maps::proto::quotateka;

Y_UNIT_TEST_SUITE(providers_api)
{

Y_UNIT_TEST(update_and_get)
{
    ServantFixture fixture;

    uint64_t adminUser = 153, casualUser = 351;
    // Set adminUser role in quotateka ABC
    fixture.abc.addMember(QUOTATEKA_ABC_SLUG, 34, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(
        yacare::tests::USER_ID_HEADER, std::to_string(adminUser));
    auto casualUserHeader = std::make_pair(
        yacare::tests::USER_ID_HEADER, std::to_string(casualUser));

    auto providerId = "driving-router";

    auto providerUpdateProto = jsonToProto<proto::UpdateProviderRequest>(R"({
        "abc_slug": "maps-core-driving-router",
        "abcd_id": "11111111-1111-1111-1111-111111111111",
        "tvm_ids": [12345],
        "scope_support": "AccountSlugAsScope",
        "resources": [{
            "id": "general",
            "endpoints": [
                {"path": "/v1/route", "cost": 100},
                {"path": "/v1/summary", "cost": 500}
            ],
        }]
    })");

    auto expectedProviderProto = jsonToProto<proto::ProviderProfile>(R"({
        "id": "driving-router",
        "abc_slug": "maps-core-driving-router",
        "abcd_id": "11111111-1111-1111-1111-111111111111",
        "tvm_ids": [12345],
        "scope_support": "AccountSlugAsScope",
        "resources": [{
            "id": "general",
            "endpoints": [
                {"path": "/v1/route", "cost": 100},
                {"path": "/v1/summary", "cost": 500}
            ],
        }]
    })");

    {  // Failed update - no user
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/provider_update").addParam("provider_id", providerId));
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
    }
    {  // Failed update - no provider_id
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/provider_update"));
        request.headers.emplace(casualUserHeader);

        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {  // Failed update - no permissions
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/provider_update").addParam("provider_id", providerId));
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    { // Failed update - abc not exists
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/provider_update")
                .addParam("provider_id", providerId));
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(providerUpdateProto);
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    fixture.abc.addService("maps-core-driving-router", 1);
    {  // Success update
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/provider_update").addParam("provider_id", providerId));
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(providerUpdateProto);

        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            expectedProviderProto,
            stringToProto<proto::ProviderProfile>(response.body)
        );
    }

    {  // Failed Get - no provider_id or abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/provider/get"));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {  // Failed Get - invalid provider_id
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/provider/get").addParam("provider_id", "NoSuchProvider"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
        EXPECT_EQ(response.body, "No such API provider: NoSuchProvider");
    }
    {  // Failed Get - invalid abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/provider/get").addParam("provider_abc", "no-such-provider"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
        EXPECT_EQ(response.body, "No API provider associated with ABC: no-such-provider");
    }
    {  // Success Get with provider_id
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/provider/get").addParam("provider_id", providerId));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            expectedProviderProto,
            stringToProto<proto::ProviderProfile>(response.body)
        );
    }
    {  // Success Get with abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/provider/get").addParam("provider_abc", "maps-core-driving-router"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            expectedProviderProto,
            stringToProto<proto::ProviderProfile>(response.body)
        );
    }
}

} // Y_UNIT_TEST_SUITE

}  // namespace maps::quotateka::tests
