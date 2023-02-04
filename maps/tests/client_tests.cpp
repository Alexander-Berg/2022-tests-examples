#include <maps/infra/quotateka/server/tests/fixture.h>
#include <maps/infra/quotateka/server/tests/proto_utils.h>
#include <yandex/maps/proto/quotateka/quotas.pb.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

namespace proto = yandex::maps::proto::quotateka;

namespace {

const std::string DB_CONTENT_SCRIPT
    = (ArcadiaSourceRoot() + "/maps/infra/quotateka/migrations/tests/sample_data.sql");

template<typename ProtoMessage>
inline ProtoMessage resetTimestampsProto(ProtoMessage protoMessage)
{
    protoMessage.clear_created();
    protoMessage.clear_updated();
    return protoMessage;
}

}  // anonymous namespace

Y_UNIT_TEST_SUITE(clients_api)
{

Y_UNIT_TEST(create_update)
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

    auto clientAbcSlug = "some-abc-service";
    auto clientAbcId = 1;

    auto quotaUpdateProto = proto::UpdateProviderQuotasRequest();
    {  // single quota entry
        auto quotaProto = quotaUpdateProto.add_quotas();
        quotaProto->set_resource_id("general");
        quotaProto->set_limit(153);
    }

    {  // Failed update - no client_abc
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/client_update").addParam("provider_id", "driving-router"));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {   // Failed update - no provider_id
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/client_update").addParam("client_abc", clientAbcSlug));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {   // Failed update - authorization
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/client_update")
                .addParam("client_abc", clientAbcSlug).addParam("provider_id", "driving-router")
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // No permissions
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    { // Failed update - no nonexistent abc services
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/client_update")
                .addParam("client_abc", clientAbcSlug)
                .addParam("provider_id", "driving-router"));
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(quotaUpdateProto);

        auto resp = yacare::performTestRequest(request);
        EXPECT_EQ(400, resp.status);
        EXPECT_EQ(resp.body, "Not found ABC service with slug: some-abc-service");
    }

    fixture.abc.addService(clientAbcSlug, clientAbcId);

    {  // Failed update - invalid resource set in quotas
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/client_update")
                .addParam("client_abc", clientAbcSlug).addParam("provider_id", "driving-router")
        );
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(quotaUpdateProto);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, response.status);
        EXPECT_EQ("Invalid request, violates data constraints", response.body);
    }

    // Add resource entry to DB
    fixture.database().executeSql(R"(
        INSERT INTO quotateka.providers (id, name, abc_slug)
        VALUES ('driving-router', '{"en": "Driving Router"}', 'maps-core-driving-router');
        INSERT INTO quotateka.resources (id, type, provider_id, name, default_limit, anonym_limit)
        VALUES ('general', 'PerSecondLimit', 'driving-router', '{"en": "Driving Router API"}', 10, 1);
     )");

    {  // Successful update
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/configuration/client_update")
                .addParam("client_abc", clientAbcSlug).addParam("provider_id", "driving-router")
        );
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(quotaUpdateProto);

        EXPECT_EQ(200, yacare::performTestRequest(request).status);
    }

    {  // Successful get client by abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/client/get").addParam("client_abc", clientAbcSlug)
        );

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedClientProto = jsonToProto<proto::ClientProfile>(R"({
            "id": "abc:some-abc-service",
            "abc_slug": "some-abc-service",
            quotas: [
                { provider: {id: "driving-router", abc_slug: "maps-core-driving-router", name: 'Driving Router'},
                    total: [{
                        resource: { id: "general", type: "PerSecondLimit", name: 'Driving Router API'},
                        limit: 153
                    }]
                }
            ],
            accounts: []
        })");
        EXPECT_PROTO_EQ(
            expectedClientProto,
            resetTimestampsProto(stringToProto<proto::ClientProfile>(response.body))
        );
    }
}

Y_UNIT_TEST(get)
{
    ServantFixture fixture;
    // Preset DB content
    fixture.database().executeSql(maps::common::readFileToString(DB_CONTENT_SCRIPT));

    auto expectedClientProto = jsonToProto<proto::ClientProfile>(R"({
        "id": "example-client-id",
        "abc_slug": "example-client-abc-slug",
        accounts: [{
            id: "d4d31379-ccf1-4bd9-bfd6-e622611adc9b",
            name: "example account",
            slug: "test-account",
            provider: {id: "driving-router", abc_slug: "maps-core-driving-router", name: "Driving Router"}
        }],
        quotas: [ {
            provider: {id: "driving-router", abc_slug: "maps-core-driving-router", name: 'Driving Router'},
            total: [
                {resource: {id: "general", type: "PerSecondLimit", name: 'Driving Router API'}, limit: 10}
            ],
            dispensed: [
                {resource: {id: "general", type: "PerSecondLimit", name: 'Driving Router API'}, limit: 10}
            ],
            allocated: [
                {resource: {id: "general", type: "PerSecondLimit", name: 'Driving Router API'}, limit: 5}
            ]
        } ],
    })");

    {  // Successfull get client by abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/client/get").addParam("client_abc", "example-client-abc-slug")
        );

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            expectedClientProto,
            resetTimestampsProto(stringToProto<proto::ClientProfile>(response.body))
        );
    }
    {  // Failed get, invalid ABC
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/client/get").addParam("client_abc", "no-such-abc")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("Not found client with ABC slug: no-such-abc", response.body);
    }
    {  // Failed get, invalid id
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/client/get").addParam("client_id", "no-such-client")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("Not found client with id: no-such-client", response.body);
    }

    {  // Failed get by account
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/client/get").addParam("account_id", "no-such-account")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("Not found client for account: no-such-account", response.body);
    }
    {  // Successful get by account
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/client/get").addParam("account_id", "d4d31379-ccf1-4bd9-bfd6-e622611adc9b")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            expectedClientProto,
            resetTimestampsProto(stringToProto<proto::ClientProfile>(response.body))
        );
    }
}

}  // Y_UNIT_TEST_SUITE

}  // namespace maps::quotateka::tests
