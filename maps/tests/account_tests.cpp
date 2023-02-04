#include <maps/infra/quotateka/server/tests/fixture.h>
#include <maps/infra/quotateka/server/tests/proto_utils.h>

#include <maps/infra/quotateka/datamodel/include/quotas_manager.h>
#include <maps/infra/quotateka/datamodel/include/account_manager.h>
#include <maps/infra/quotateka/datamodel/include/client_orm.h>
#include <maps/infra/quotateka/datamodel/include/provider_orm.h>
#include <maps/infra/quotateka/datamodel/include/account_orm.h>
#include <yandex/maps/proto/quotateka/quotas.pb.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/locale/include/convert.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

namespace proto = yandex::maps::proto::quotateka;

namespace {

const auto EN_LOCALE = maps::locale::to<maps::locale::Locale>("en");

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


Y_UNIT_TEST_SUITE(accounts_api)
{

Y_UNIT_TEST(quotas)
{
    ServantFixture fixture;
    auto accountId = "d300377-89e8cd6b-4b36fe18-b2f7a1d1";
    {   // Init DB with preset data
        auto txn = fixture.pgPool().masterWriteableTransaction();
        datamodel::ProvidersGateway{*txn}.insert(datamodel::ProviderRecords{
            {.id = "provider-a", .abcSlug = "provider-a-service"},
            {.id = "provider-b", .abcSlug = "provider-b-service"}});
        datamodel::ResourcesGateway{*txn}.insert(datamodel::ResourceRecords{
            {.id = "resource1", .providerId = "provider-a"},
            {.id = "resource2", .providerId = "provider-b"},
        });
        datamodel::ClientsGateway{*txn}.insert(
            datamodel::ClientRecord{.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1});
        datamodel::ClientQuotasGateway{*txn}.insert(
            datamodel::ClientQuotaRecord{.clientId = "abc:client-x", .providerId = "provider-a", .resourceId = "resource1", .quota = 100});
        datamodel::AccountsGateway{*txn}.insert(
            datamodel::AccountRecord{.id = accountId, .clientId = "abc:client-x", .providerId="provider-a", .name = "MyAccount"});
        txn->commit();
    }

    uint64_t adminUser = 153, casualUser = 351;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(adminUser));
    auto casualUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(casualUser));

    proto::UpdateProviderQuotasRequest updateProto;
    {
        auto quotaEntry = updateProto.add_quotas();
        quotaEntry->set_resource_id("resource1");
        quotaEntry->set_limit(100);
    }

    {  // Dispense: no account
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/dispense_quota"));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {  // Dispense: permission fails
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/dispense_quota")
                .addParam("account_id", accountId).addParam("provider_id", "provider-a")
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // User without permissions
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Dispense: Invalid account
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/dispense_quota")
                .addParam("account_id", "no-such-account").addParam("provider_id", "provider-a")
        );
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
        EXPECT_EQ(response.body, "No such account: no-such-account");
    }
    {  // Dispense: success
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/dispense_quota")
                .addParam("account_id", accountId).addParam("provider_id", "provider-a")
        );
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(updateProto);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedAccountProto = jsonToProto<proto::Account>(R"({
            id: "d300377-89e8cd6b-4b36fe18-b2f7a1d1", name: "MyAccount",
            quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" },
                dispensed: [{ resource: { id: "resource1" }, limit: 100 }] },
            quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" },
                allocated: [{ resource: { id: "resource1" }, limit: 0 }] }
        })");
        EXPECT_PROTO_EQ(
            expectedAccountProto,
            resetTimestampsProto(stringToProto<proto::Account>(response.body))
        );
    }
    {  // Dispense: not enough quota
        proto::UpdateProviderQuotasRequest badUpdateProto;
        {
            auto quotaEntry = badUpdateProto.add_quotas();
            quotaEntry->set_resource_id("resource1");
            quotaEntry->set_limit(153);  // Only 100 is allowed
        }
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/dispense_quota")
                .addParam("account_id", accountId).addParam("provider_id", "provider-a")
        );
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(badUpdateProto);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
        EXPECT_EQ(response.body, "Invalid quota specified: Not allowed to exceed total quota for resource1");
    }

    {  // Allocate: permission fails
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/allocate_quota")
                .addParam("account_id", accountId).addParam("provider_id", "provider-a")
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // User without permissions
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Allocate: Invalid account
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/allocate_quota")
                .addParam("account_id", "no-such-account").addParam("provider_id", "provider-a")
        );
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
        EXPECT_EQ(response.body, "No such account: no-such-account");
    }
    {  // Allocate: success
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/allocate_quota")
                .addParam("account_id", accountId).addParam("provider_id", "provider-a")
        );
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(updateProto);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedAccountProto = jsonToProto<proto::Account>(R"({
            id: "d300377-89e8cd6b-4b36fe18-b2f7a1d1",
            name: "MyAccount",
            quota: {
                provider: { id: "provider-a", abc_slug: "provider-a-service" },
                dispensed: [{ resource: { id: "resource1" }, limit: 100 }],
                allocated: [{ resource: { id: "resource1" }, limit: 100 }]
            }
        })");
        EXPECT_PROTO_EQ(
            expectedAccountProto,
            resetTimestampsProto(stringToProto<proto::Account>(response.body))
        );
    }
    {  // Allocate: not enough quota
        proto::UpdateProviderQuotasRequest badUpdateProto;
        {
            auto quotaEntry = badUpdateProto.add_quotas();
            quotaEntry->set_resource_id("resource1");
            quotaEntry->set_limit(153);  // Only 100 is allowed
        }
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/allocate_quota")
                .addParam("account_id", accountId).addParam("provider_id", "provider-a")
        );
        request.headers.emplace(adminUserHeader);
        request.body = protoToString(badUpdateProto);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
        EXPECT_EQ(response.body, "Invalid quota specified: Not allowed to set allocated > dispensed for resource1");
    }
}

Y_UNIT_TEST(get)
{
    ServantFixture fixture;
    // Preset DB content
    fixture.database().executeSql(maps::common::readFileToString(DB_CONTENT_SCRIPT));
    auto accountId = "d4d31379-ccf1-4bd9-bfd6-e622611adc9b";

    {  // Get invalid account
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/account/get").addParam("account_id", "NoSuchAccount")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("No such account: NoSuchAccount", response.body);
    }
    {  // Get success
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/account/get").addParam("account_id", accountId)
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedAccountProto = jsonToProto<proto::Account>(R"({
            id: "d4d31379-ccf1-4bd9-bfd6-e622611adc9b",
            slug: "test-account",
            name: "example account",
            identities: [ { tvm_id: 12345, name: "default-tvm" }],
            quota: {
                provider: { id: "driving-router", abc_slug: "maps-core-driving-router", name: "Driving Router" },
                dispensed: [
                    { resource: { id: "general", name: "Driving Router API" }, limit: 10 }
                ],
                allocated: [
                    { resource: { id: "general", name: "Driving Router API" }, limit: 5 }
                ]
           }
        })");
        EXPECT_PROTO_EQ(
            expectedAccountProto,
            resetTimestampsProto(stringToProto<proto::Account>(response.body))
        );
    }
}

Y_UNIT_TEST(create)
{
    ServantFixture fixture;
    {   // Preset DB content with single client record
        auto txn = fixture.pgPool().masterWriteableTransaction();
        datamodel::ClientsGateway{*txn}.insert(
            datamodel::ClientRecord{.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1});
        datamodel::ProvidersGateway{*txn}.insert(
            datamodel::ProviderRecord{.id = "provider-a", .abcSlug = "provider-a-service"});
        txn->commit();
    }

    std::string defaultFolderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.abcd.addServiceFolder(1, {.id = defaultFolderId});

    uint64_t adminUser = 153, casualUser = 351;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(adminUser));
    auto casualUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(casualUser));

    {  // Failed create: no permissions
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/create")
                .addParam("client_id", "abc:client-x")
                .addParam("provider_id", "provider-a")
                .addParam("account_slug", "my-account")
                .addParam("name", "MyAccount")
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // User without permissions
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Failed create: invalid client
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/create")
                .addParam("client_id", "NoSuchClient")
                .addParam("provider_id", "provider-a")
                .addParam("account_slug", "my-account")
                .addParam("name", "MyAccount")
        );
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("Not found client with id: NoSuchClient", response.body);
    }
    {  // Failed create: invalid provider
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/create")
                .addParam("client_id", "abc:client-x")
                .addParam("provider_id", "NoSuchProvider")
                .addParam("account_slug", "my-account")
                .addParam("name", "MyAccount")
        );
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("No such API provider: NoSuchProvider", response.body);
    }

    // Successful create
    http::MockRequest request{
        http::POST,
        http::URL("http://localhost/account/create")
            .addParam("client_id", "abc:client-x")
            .addParam("provider_id", "provider-a")
            .addParam("account_slug", "my-account")
            .addParam("name", "MyAccount")
            .addParam("description", "Some description")
    };
    request.headers.emplace(adminUserHeader);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(200, response.status);

    auto accountProto = stringToProto<proto::Account>(response.body);
    EXPECT_NE(accountProto.id(), "");
    accountProto.set_id(""); // drop generated id from comparison

    auto expectedAccountProto = jsonToProto<proto::Account>(R"({
        slug: "my-account",
        name: "MyAccount",
        description: "Some description",
        quota: {
            provider: { id: "provider-a", abc_slug: "provider-a-service" },
            dispensed: [], allocated: []
        }
    })");
    // TODO: GEOINFRA-2420 and timestamps tests
    EXPECT_PROTO_EQ(
        expectedAccountProto,
        resetTimestampsProto(accountProto)
    );

    // Check forlderId is set in database
    auto txn = fixture.pgPool().masterReadOnlyTransaction();
    auto accounts = datamodel::AccountManager{txn}.lookupAccounts(
        {.clientId = "abc:client-x"});
    EXPECT_EQ(accounts.size(), 1u);
    EXPECT_EQ(accounts[0].folderId, defaultFolderId);
}

Y_UNIT_TEST(rename)
{
    ServantFixture fixture;
    {   // Preset DB content with single client record
        auto txn = fixture.pgPool().masterWriteableTransaction();
        datamodel::ClientsGateway{*txn}.insert(
            datamodel::ClientRecord{.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1});
        datamodel::ProvidersGateway{*txn}.insert(
            datamodel::ProviderRecord{.id = "provider-a", .abcSlug = "provider-a-service"});
        txn->commit();
    }

    std::string defaultFolderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.abcd.addServiceFolder(1, {.id = defaultFolderId});

    uint64_t adminUser = 153;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(adminUser));

    // Create
    http::MockRequest request{
        http::POST,
        http::URL("http://localhost/account/create")
            .addParam("client_id", "abc:client-x")
            .addParam("provider_id", "provider-a")
            .addParam("account_slug", "my-account")
            .addParam("name", "MyAccount")
            .addParam("description", "Some description")
    };
    request.headers.emplace(adminUserHeader);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(200, response.status);

    auto accountProto = stringToProto<proto::Account>(response.body);
    auto accountId = accountProto.id();

    {   // Rename
        auto newName = "MyAccount Renamed";
        auto newDescription = "Description Updated";

        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/rename").addParam("account_id", accountProto.id())
                .addParam("name", newName).addParam("description", newDescription)
        };
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto receivedProto = stringToProto<proto::Account>(response.body);

        accountProto.set_name(newName);
        accountProto.set_description(newDescription);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(accountProto),
            resetTimestampsProto(receivedProto)
        );
    }
}

Y_UNIT_TEST(tvm_assign)
{
    ServantFixture fixture;
    auto accX = "111", accY = "222";
    // Init DB with preset data
    fixture.insert<datamodel::ClientsTable>({
        {.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1},
        {.id = "abc:client-y", .abcSlug = "client-y", .abcId = 2},
    })
    .insert<datamodel::ProvidersTable>({
        {.id = "provider-a", .abcSlug = "provider-a-service"},
    })
    .insert<datamodel::AccountsTable>({
        {.id = accX, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-x"},
        {.id = accY, .clientId = "abc:client-y", .providerId = "provider-a", .slug = "acc-y"},
    });

    uint64_t adminUser = 153, casualUser = 351;
    // Set adminUser role for client-x and client-y
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);
    fixture.abc.addMember("client-y", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(adminUser));
    auto casualUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(casualUser));

    {  // Assign fail: invalid params
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/account/assign_tvm"));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
        request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("tvm_id", "invalid_tvm"));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {  // Assign fail: No permission
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("account_id", accX)
                .addParam("tvm_id", 12345).addParam("name", "tvmA")
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // User without permissions
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Assign success  (153 -> accAX)
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("account_id", accX)
                .addParam("tvm_id", 153).addParam("name", "tvm-153")
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "111", slug: "acc-x",
                identities: [ { tvm_id: 153, name: "tvm-153" }],
                quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
            })")
        );
    }
    {  // Assign fail:  can't assign to another acc of the same provider
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("account_id", accY)
                .addParam("tvm_id", 153).addParam("name", "tvm-153")
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(409, response.status);
        EXPECT_EQ("Unique constraint violation", response.body);
    }
    {  // Assign success: re-assign to same acc
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("account_id", accX)
                .addParam("tvm_id", 153).addParam("name", "tvm-153-renamed")
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "111", slug: "acc-x",
                identities: [ { tvm_id: 153, name: "tvm-153-renamed" }],
                quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
            })")
        );
    }
}

Y_UNIT_TEST(tvm_assign_scoped)
{
    ServantFixture fixture;
    auto accX = "111", accY = "222";
    // Init DB with preset data
    fixture.insert<datamodel::ClientsTable>({
        {.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1},
        {.id = "abc:client-y", .abcSlug = "client-y", .abcId = 2},
    })
    .insert<datamodel::ProvidersTable>({
        {.id = "provider-a", .abcSlug = "provider-a-service", .scopeSupport = ScopeSupportType::AccountSlugAsScope}
    })
    .insert<datamodel::AccountsTable>({
        {.id = accX, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-x"},
        {.id = accY, .clientId = "abc:client-y", .providerId = "provider-a", .slug = "acc-y"},
    });

    uint64_t adminUser = 153;
    // Set adminUser role for client-x and client-y
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);
    fixture.abc.addMember("client-y", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(adminUser));

    {  // Assign success  (153 -> accX)
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("account_id", accX)
                .addParam("tvm_id", 153).addParam("name", "tvm-153")
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "111", slug: "acc-x",
                identities: [ { tvm_id: 153, name: "tvm-153" }],
                quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
            })")
        );
    }
    {  // Assign success:  can assign to another acc of the same provider
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("account_id", accY)
                .addParam("tvm_id", 153).addParam("name", "tvm-153")
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "222", slug: "acc-y",
                identities: [ { tvm_id: 153, name: "tvm-153" }],
                quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
            })")
        );
    }
    {  // Assign success: re-assign to same acc
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/assign_tvm").addParam("account_id", accX)
                .addParam("tvm_id", 153).addParam("name", "tvm-153-renamed")
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "111", slug: "acc-x",
                identities: [ { tvm_id: 153, name: "tvm-153-renamed" }],
                quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
            })")
        );
    }
}

Y_UNIT_TEST(tvm_revoke)
{
    ServantFixture fixture;
    auto accX = "111", accY = "222", accX2 = "333", accY2 = "444";
    // Init DB with preset data
    fixture.insert<datamodel::ClientsTable>({
        {.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1},
        {.id = "abc:client-y", .abcSlug = "client-y", .abcId = 2},
    })
    .insert<datamodel::ProvidersTable>({
        {.id = "provider-a", .abcSlug = "provider-a-service"},
        {.id = "provider-b", .abcSlug = "provider-b-service", .scopeSupport = ScopeSupportType::AccountSlugAsScope},
    })
    .insert<datamodel::AccountsTable>({
        {.id = accX, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-x"},
        {.id = accY, .clientId = "abc:client-y", .providerId = "provider-a", .slug = "acc-y"},
        {.id = accX2, .clientId = "abc:client-x", .providerId = "provider-b", .slug = "acc-x2"},
        {.id = accY2, .clientId = "abc:client-x", .providerId = "provider-b", .slug = "acc-y2"},
    })
    .insert<datamodel::AccountTvmTable>({
        {.tvmId = 42, .accountId = accX, .providerId = "provider-a"},
        {.tvmId = 153, .accountId = accY, .providerId = "provider-a"},
        {.tvmId = 153, .accountId = accX2, .providerId = "provider-b", .scope = "acc-x2"},
        {.tvmId = 153, .accountId = accY2, .providerId = "provider-b", .scope = "acc-y2"},
    });

    uint64_t userId = 12345;
    // Set adminUser role for client-y only
    fixture.abc.addMember("client-x", 1, userId, abc::MANAGERS_ROLE_SCOPE);
    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto userHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(userId));

    {  // Revoke fail: No permission
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/revoke_tvm").addParam("account_id", accY).addParam("tvm_id", 153)
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // User without permissions
        request.headers.emplace(userHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Revoke fail: no such tvm in the acc
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/revoke_tvm")
                .addParam("account_id", accX).addParam("tvm_id", 153)
        );
        request.headers.emplace(userHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("Not found tvm:153 assigned to account:111", response.body);
    }
    {  // Revoke: success
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/revoke_tvm")
                .addParam("account_id", accX).addParam("tvm_id", 42)
        );
        request.headers.emplace(userHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "111", slug: "acc-x",
                identities: [ ],
                quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
            })")
        );
    }
    {  // Revoke: success
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/revoke_tvm")
                .addParam("account_id", accX2).addParam("tvm_id", 153)
        );
        request.headers.emplace(userHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "333", slug: "acc-x2",
                identities: [ ],
                quota: { provider: { id: "provider-b", abc_slug: "provider-b-service" } }
            })")
        );
    }
}

Y_UNIT_TEST(tvm_move)
{
    ServantFixture fixture;
    auto accAX = "111", accAX2 = "222", accBX = "333", accAY = "444";
    // Init DB with preset data
    fixture.insert<datamodel::ClientsTable>({
        {.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1},
        {.id = "abc:client-y", .abcSlug = "client-y", .abcId = 2},
    })
    .insert<datamodel::ProvidersTable>({
        {.id = "provider-a", .abcSlug = "provider-a-service"},
        {.id = "provider-b", .abcSlug = "provider-a-service"},
    })
    .insert<datamodel::AccountsTable>({
        {.id = accAX, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-ax"},
        {.id = accAX2, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-ax2"},
        {.id = accAY, .clientId = "abc:client-y", .providerId = "provider-a", .slug = "acc-ay"},
        {.id = accBX, .clientId = "abc:client-x", .providerId = "provider-b", .slug = "acc-bx"},
    })
    .insert<datamodel::AccountTvmTable>({
        {.tvmId = 42, .accountId = accAX, .providerId = "provider-a"},
    });

    uint64_t adminUser = 153, casualUser = 351;
    // Set adminUser role for client-x only
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(adminUser));
    auto casualUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(casualUser));

    {  // Move fail: invalid account
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 42)
                .addParam("from_account_id", accAX).addParam("to_account_id", "no-such-account")
        );
        request.headers.emplace(casualUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, yacare::performTestRequest(request).status);
        EXPECT_EQ("Invalid account specified", response.body);
    }
    {  // Move fail: different providers not allowed
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 42)
                .addParam("from_account_id", accAX).addParam("to_account_id", accBX)
        );
        request.headers.emplace(casualUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
        EXPECT_EQ("Can't move tvm between accounts of different providers", response.body);
    }
    {  // Move fail: different clients not allowed
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 42)
                .addParam("from_account_id", accAX).addParam("to_account_id", accAY)
        );
        request.headers.emplace(casualUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
        EXPECT_EQ("Can't move tvm between accounts of different clients", response.body);
    }
    {  // Move fail: no permissions
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 42)
                .addParam("from_account_id", accAX).addParam("to_account_id", accAX2)
        );
        request.headers.emplace(casualUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Move fail: tvm not assigned
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 666)
                .addParam("from_account_id", accAX).addParam("to_account_id", accAX2)
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, yacare::performTestRequest(request).status);
        EXPECT_EQ("Requested object not found", response.body);
    }
    {  // Move success
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 42)
                .addParam("from_account_id", accAX).addParam("to_account_id", accAX2)
        );
        request.headers.emplace(adminUserHeader);

        auto response = yacare::performTestRequest(request);

        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            resetTimestampsProto(stringToProto<proto::Account>(response.body)),
            jsonToProto<proto::Account>(R"({
                id: "222", slug: "acc-ax2",
                identities: [ {tvm_id: 42} ],
                quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
            })")
        );
    }
}

Y_UNIT_TEST(tvm_move_scoped)
{
    ServantFixture fixture;
    auto accAX = "111", accAX2 = "222", accAX3 = "333";
    // Init DB with preset data
    fixture.insert<datamodel::ClientsTable>({
        {.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1},
    })
    .insert<datamodel::ProvidersTable>({
        {.id = "provider-a", .abcSlug = "provider-a-service", .scopeSupport = ScopeSupportType::AccountSlugAsScope},
    })
    .insert<datamodel::AccountsTable>({
        {.id = accAX, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-ax"},
        {.id = accAX2, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-ax2"},
        {.id = accAX3, .clientId = "abc:client-x", .providerId = "provider-a", .slug = "acc-ax3"},
    })
    .insert<datamodel::AccountTvmTable>({
        {.tvmId = 42, .accountId = accAX, .providerId = "provider-a", .scope = "acc-ax"},
        {.tvmId = 42, .accountId = accAX2, .providerId = "provider-a", .scope = "acc-ax2"},
    });

    uint64_t adminUser = 153;
    // Set adminUser role for client-x only
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(yacare::tests::USER_ID_HEADER, std::to_string(adminUser));

    {  // Move fail
        {
            http::MockRequest request(
                http::POST,
                http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 42)
                    .addParam("from_account_id", accAX).addParam("to_account_id", accAX2)
            );
            request.headers.emplace(adminUserHeader);

            auto response = yacare::performTestRequest(request);
            EXPECT_EQ(409, yacare::performTestRequest(request).status);
            EXPECT_EQ("Unique constraint violation", response.body);
        }
        {
            http::MockRequest request(
                http::GET,
                http::URL("http://localhost/account/get").addParam("account_id", accAX)
            );
            request.headers.emplace(adminUserHeader);

            auto response = yacare::performTestRequest(request);
            EXPECT_EQ(200, response.status);
            EXPECT_PROTO_EQ(
                resetTimestampsProto(stringToProto<proto::Account>(response.body)),
                jsonToProto<proto::Account>(R"({
                    id: "111", slug: "acc-ax",
                    identities: [ {tvm_id: 42} ],
                    quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
                })")
            );
        }
    }
    {  // Move success
        {
            http::MockRequest request(
                http::POST,
                http::URL("http://localhost/account/move_tvm").addParam("tvm_id", 42)
                    .addParam("from_account_id", accAX).addParam("to_account_id", accAX3)
            );
            request.headers.emplace(adminUserHeader);

            auto response = yacare::performTestRequest(request);
            EXPECT_EQ(200, response.status);
            EXPECT_PROTO_EQ(
                resetTimestampsProto(stringToProto<proto::Account>(response.body)),
                jsonToProto<proto::Account>(R"({
                    id: "333", slug: "acc-ax3",
                    identities: [ {tvm_id: 42} ],
                    quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
                })")
            );
        }
        {
            http::MockRequest request(
                http::GET,
                http::URL("http://localhost/account/get").addParam("account_id", accAX)
            );
            request.headers.emplace(adminUserHeader);

            auto response = yacare::performTestRequest(request);
            EXPECT_EQ(200, response.status);
            EXPECT_PROTO_EQ(
                resetTimestampsProto(stringToProto<proto::Account>(response.body)),
                jsonToProto<proto::Account>(R"({
                    id: "111", slug: "acc-ax",
                    identities: [ ],
                    quota: { provider: { id: "provider-a", abc_slug: "provider-a-service" } }
                })")
            );
        }
    }
}

Y_UNIT_TEST(lookup)
{
    ServantFixture fixture;
    // Fill db
    fixture.database().executeSql(maps::common::readFileToString(DB_CONTENT_SCRIPT));

    {   // Lookup by client & account slug
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/account/lookup")
                .addParam("client_id", "example-client-id")
                .addParam("provider_slug", "test-account"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedProto = jsonToProto<proto::AccountsList>(R"({
            accounts: [{
                id: "d4d31379-ccf1-4bd9-bfd6-e622611adc9b",
                name: "example account",
                slug: "test-account",
                provider: {id: "driving-router", abc_slug: "maps-core-driving-router", name: "Driving Router"}
            }]
        })");
        EXPECT_PROTO_EQ(
            expectedProto,
            stringToProto<proto::AccountsList>(response.body)
        );
    }
    {   // Lookup by provider
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/account/lookup").addParam("provider_id", "driving-router"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedProto = jsonToProto<proto::AccountsList>(R"({
            accounts: [{
               id: "d4d31379-ccf1-4bd9-bfd6-e622611adc9b",
               name: "example account",
               slug: "test-account",
               provider: {id: "driving-router", abc_slug: "maps-core-driving-router", name: "Driving Router"}
            }]
        })");
        EXPECT_PROTO_EQ(
            expectedProto,
            stringToProto<proto::AccountsList>(response.body)
        );
    }
    {   // Lookup by nonexistent provider
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/account/lookup").addParam("provider_id", "NoSuchProvider"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_PROTO_EQ(
            proto::AccountsList{},
            stringToProto<proto::AccountsList>(response.body)
        );
    }
}

Y_UNIT_TEST(close_and_reopen)
{
    ServantFixture fixture;
    auto accountXRecord = datamodel::AccountRecord{
        .id = "accountX", .clientId = "abc:client-x", .providerId = "provider-a", .slug = "account-x", .name = "AccountX"};
    auto accountYRecord = datamodel::AccountRecord{
        .id = "accountY", .clientId = "abc:client-x", .providerId = "provider-a", .slug = "account-y", .name = "AccountY"};
    {   // Init DB with preset data
        auto txn = fixture.pgPool().masterWriteableTransaction();
        // Add provider & resource
        datamodel::ProvidersGateway{*txn}.insert(
            datamodel::ProviderRecord{.id = "provider-a", .abcSlug = "provider-a-service"});
        datamodel::ResourcesGateway{*txn}.insert(datamodel::ResourceRecords{
            datamodel::ResourceRecord{.id = "resource1", .providerId = "provider-a"}
        });

        // Add client & client quota
        datamodel::ClientsGateway{*txn}.insert(
            datamodel::ClientRecord{.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1});
        datamodel::ClientQuotasGateway{*txn}.insert(
            datamodel::ClientQuotaRecord{
                .clientId = "abc:client-x", .providerId = "provider-a",
                .resourceId = "resource1", .quota = 100});

        // Add pair of accounts
        datamodel::AccountsGateway{*txn}.insert(accountXRecord);
        datamodel::AccountsGateway{*txn}.insert(accountYRecord);

        // Dispense & allocate quota for the first one
        datamodel::QuotasManager{txn}.dispenseQuota(accountXRecord, {{"resource1", 10}});
        datamodel::QuotasManager{txn}.allocateQuota(accountXRecord, {{"resource1", 10}});
        txn->commit();
    }

    uint64_t adminUser = 153, casualUser = 351;
    // Set adminUser role in abc:client-x ABC
    fixture.abc.addMember("client-x", 1, adminUser, abc::MANAGERS_ROLE_SCOPE);

    yacare::tests::UserIdHeaderFixture userFixture;  // Enable userId through header
    auto adminUserHeader = std::make_pair(
        yacare::tests::USER_ID_HEADER, std::to_string(adminUser));
    auto casualUserHeader = std::make_pair(
        yacare::tests::USER_ID_HEADER, std::to_string(casualUser));

    {  // Close: invalid params
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/account/close"));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {  // Close: no permission
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/close").addParam("account_id", accountXRecord.id)
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // User without permissions
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Close: no such account
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/close").addParam("account_id", "noSuchAccount")
        );
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("No such account: noSuchAccount", response.body);
    }
    {  // Close: account has provided quotas
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/close").addParam("account_id", accountXRecord.id)
        );
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, response.status);
        EXPECT_EQ("Account 'AccountX' has provided quota", response.body);
    }
    {   // Successful close
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/close").addParam("account_id", accountYRecord.id)
        };
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto accountProto = stringToProto<proto::Account>(response.body);
        EXPECT_EQ(accountProto.is_closed(), true);
    }

    {  // Reopen: invalid params
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/account/reopen"));
        EXPECT_EQ(400, yacare::performTestRequest(request).status);
    }
    {  // Reopen: no permission
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/reopen").addParam("account_id", accountXRecord.id)
        );
        // No user
        EXPECT_EQ(401, yacare::performTestRequest(request).status);
        // User without permissions
        request.headers.emplace(casualUserHeader);
        EXPECT_EQ(403, yacare::performTestRequest(request).status);
    }
    {  // Reopen: no such account
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/reopen").addParam("account_id", "noSuchAccount")
        );
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("No such account: noSuchAccount", response.body);
    }
    {   // Successful reopen
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/reopen").addParam("account_id", accountXRecord.id)
        };
        request.headers.emplace(adminUserHeader);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto accountProto = stringToProto<proto::Account>(response.body);
        EXPECT_EQ(accountProto.is_closed(), false);

        request = http::MockRequest{
            http::POST,
            http::URL("http://localhost/account/reopen").addParam("account_id", accountYRecord.id)
        };
        request.headers.emplace(adminUserHeader);
        response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        accountProto = stringToProto<proto::Account>(response.body);
        EXPECT_EQ(accountProto.is_closed(), false);
    }
}

}  // Y_UNIT_TEST_SUITE

}  // namespace maps::quotateka::tests
