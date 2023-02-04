#include <maps/infra/quotateka/proto/configuration.pb.h>
#include <maps/infra/quotateka/server/lib/abcd/accounts.h>
#include <maps/infra/quotateka/server/lib/abcd/provisions.h>
#include <maps/infra/quotateka/server/lib/audit.h>
#include <maps/infra/quotateka/server/tests/fixture.h>
#include <maps/infra/quotateka/server/tests/proto_utils.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <fmt/format.h>

#include <regex>

namespace maps::quotateka::tests {

namespace proto = yandex::maps::proto::quotateka;

namespace {

const std::string DB_CONTENT_SCRIPT =
    (ArcadiaSourceRoot() +
     "/maps/infra/quotateka/migrations/tests/sample_data.sql");

constexpr uint64_t ADMIN_USER = 153;
constexpr auto ABC_ID = 42;
constexpr auto PROVIDER_ABCD_ID = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
constexpr auto ACCOUNT_ID = "d300377-89e8cd6b-4b36fe18-b2f7a1d1";
constexpr auto EMPTY_ACCOUNT_ID = "e8dd9f8-73bbcd6b-4b14fe18-bdfd3352";
constexpr auto FOLDER_ID = "f02c2b0e-ffae-4e8a-9177-e2db69f22c70";
constexpr auto REQUEST_AUTHOR = R"({"passportUid": "153", "staffLogin": "login"})";

class AuditLogFixture {
public:
    AuditLogFixture()
    {
        savedState_ = auditLoggers();
        auditLoggers() = std::vector<Logger>{
            [this](std::string s) { logger(std::move(s)); },
        };
    }
    ~AuditLogFixture() { auditLoggers() = savedState_; }

    void logger(std::string logLine)
    {
        logLine = resetTimestamp(logLine);
        capturedLogs_.emplace_back(json::Value::fromString(logLine));
    }
    const std::vector<json::Value>& capturedLogsJson() const
    {
        return capturedLogs_;
    }

    std::string resetTimestamp(const std::string& logLine)
    {
        return std::regex_replace(logLine, timestampPattern_, "$010.0,");
    }

private:
    const std::regex timestampPattern_{R"(("timestamp":)([\d\.]+),)"};

    std::vector<json::Value> capturedLogs_{};
    std::vector<Logger> savedState_;
};

auth::UserInfo makeUserInfo(const std::string& login, uint64_t userId)
{
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    userInfo.setUid(std::to_string(userId));
    return userInfo;
}

class QuotatekaDefaultFixture {
public:
    QuotatekaDefaultFixture() {
        userInfoFixture = std::make_unique<yacare::tests::UserInfoFixture>(makeUserInfo("login", ADMIN_USER));

        {   // Init DB with preset data
            auto txn = servantFixture.pgPool().masterWriteableTransaction();
            datamodel::ProvidersGateway{*txn}.insert(datamodel::ProviderRecords{
                    {.id = "provider-a", .abcdId = PROVIDER_ABCD_ID, .abcSlug = "provider-a-service"}});
            datamodel::ResourcesGateway{*txn}.insert(datamodel::ResourceRecords{
                    {.id = "resource1", .providerId = "provider-a"},
                    {.id = "resource2", .providerId = "provider-a"},
                    {.id = "resource3", .providerId = "provider-a"},
            });
            datamodel::ClientsGateway{*txn}.insert(
                    datamodel::ClientRecord{.id = "abc:client-x", .abcSlug = "client-x", .abcId = ABC_ID});
            datamodel::ClientQuotasGateway{*txn}.insert(datamodel::ClientQuotaRecords{
                    datamodel::ClientQuotaRecord{.clientId = "abc:client-x", .providerId = "provider-a", .resourceId = "resource1", .quota = 100},
                    datamodel::ClientQuotaRecord{.clientId = "abc:client-x", .providerId = "provider-a", .resourceId = "resource2", .quota = 100},
                    datamodel::ClientQuotaRecord{.clientId = "abc:client-x", .providerId = "provider-a", .resourceId = "resource3", .quota = 100},
            });
            datamodel::AccountsGateway{*txn}.insert(datamodel::AccountRecords{
                    datamodel::AccountRecord{.id = ACCOUNT_ID, .clientId = "abc:client-x", .providerId="provider-a", .slug = "account-slug", .name = "MyAccount", .description = "Description", .folderId = FOLDER_ID},
                    datamodel::AccountRecord{.id = EMPTY_ACCOUNT_ID, .clientId = "abc:client-x", .providerId="provider-a", .slug = "empty-account", .name = "Empty Account"},
            });
            datamodel::AccountQuotasGateway{*txn}.insert(datamodel::AccountQuotaRecords{
                    datamodel::AccountQuotaRecord{.accountId = ACCOUNT_ID, .providerId = "provider-a", .resourceId = "resource2", .quota = 50},
                    datamodel::AccountQuotaRecord{.accountId = ACCOUNT_ID, .providerId = "provider-a", .resourceId = "resource3", .quota = 100, .allocated = 50},
            });
            txn->commit();
        }

        // Set ADMIN_USER role in client-x ABC
        servantFixture.abc.addMember("client-x", ABC_ID, ADMIN_USER, abc::MANAGERS_ROLE_SCOPE);
        servantFixture.abcd.addServiceFolder(ABC_ID, {.id = FOLDER_ID});
    }

    ServantFixture servantFixture;
    std::unique_ptr<yacare::tests::UserInfoFixture> userInfoFixture;
};

template<typename ProtoMessage>
inline ProtoMessage resetTimestampsProto(ProtoMessage protoMessage)
{
    protoMessage.clear_created();
    protoMessage.clear_updated();
    return protoMessage;
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(audit_log) {

Y_UNIT_TEST(abcd_account)
{
    QuotatekaDefaultFixture fixture;

    { // Create
        const auto createAccountUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts",
            PROVIDER_ABCD_ID);
        AuditLogFixture logFixture;
        http::MockRequest request(http::POST, http::URL(createAccountUrl));
        request.body = fmt::format(
            R"({{
                "key": "maps-core-teaspoon",
                "displayName": "Maps Core Teaspoon",
                "folderId": "{}",
                "abcServiceId": 42,
                "freeTier": false,
                "author": {}
            }})",
            FOLDER_ID,
            REQUEST_AUTHOR);
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        std::string teaspoonAccountId;
        {
            auto txn = fixture.servantFixture.pgPool().masterWriteableTransaction();
            teaspoonAccountId =
                datamodel::AccountManager{txn}
                    .lookupAccounts({.accountSlug = "maps-core-teaspoon"})[0].id;
        }
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(fmt::format(R"(
            {{"abc_slug":"client-x",
              "account_id":"{}",
              "account_slug":"maps-core-teaspoon",
              "change":{{}},
              "operation":"/abcd/account/create",
              "staff_login":"login",
              "timestamp":0}}
            )", teaspoonAccountId))});
    }
    { // Rename
        const auto renameAccountUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_rename",
            PROVIDER_ABCD_ID,
            ACCOUNT_ID);
        AuditLogFixture logFixture;
        http::MockRequest request(http::POST, http::URL(renameAccountUrl));
        request.body = fmt::format(
            R"({{
                "displayName": "Maps Core Teaspoon",
                "folderId": "{}",
                "abcServiceId": 42,
                "author": {}
            }})",
            FOLDER_ID,
            REQUEST_AUTHOR);
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"d300377-89e8cd6b-4b36fe18-b2f7a1d1",
             "account_slug":"account-slug",
             "change":{"old_name":"MyAccount","new_name":"Maps Core Teaspoon"},
             "operation":"/abcd/account/rename",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }
    { // Delete
        const auto deleteAccountUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}",
            PROVIDER_ABCD_ID,
            EMPTY_ACCOUNT_ID);
        AuditLogFixture logFixture;
        http::MockRequest request(http::DELETE, http::URL(deleteAccountUrl));
        request.body = fmt::format(R"({{"author": {}}})", REQUEST_AUTHOR);
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(204, response.status);
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"e8dd9f8-73bbcd6b-4b14fe18-bdfd3352",
             "account_slug":"empty-account",
             "change":{},
             "operation":"/abcd/account/close",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }
} // Y_UNIT_TEST(abcd_account)

Y_UNIT_TEST(qttk_account)
{
    QuotatekaDefaultFixture fixture;

    { // Create
        AuditLogFixture logFixture;
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/create")
                .addParam("client_id", "abc:client-x")
                .addParam("provider_id", "provider-a")
                .addParam("account_slug", "maps-core-teaspoon")
                .addParam("name", "MyAccount")
                .addParam("description", "Some description")};
        auto response = yacare::performTestRequest(request);
        std::string teaspoonAccountId;
        {
            auto txn = fixture.servantFixture.pgPool().masterWriteableTransaction();
            teaspoonAccountId =
                datamodel::AccountManager{txn}
                    .lookupAccounts({.accountSlug = "maps-core-teaspoon"})[0].id;
        }
        ASSERT_EQ(200, response.status);
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(fmt::format(R"(
            {{"abc_slug":"client-x",
              "account_id":"{}",
              "account_slug":"maps-core-teaspoon",
              "change":{{}},
              "operation":"/account/create",
              "staff_login":"login",
              "timestamp":0}}
            )", teaspoonAccountId))});
    }
    { // Rename
        AuditLogFixture logFixture;
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/rename")
                .addParam("account_id", ACCOUNT_ID)
                .addParam("name", "MyAccount Renamed")
                .addParam("description", "Description Updated")};
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"d300377-89e8cd6b-4b36fe18-b2f7a1d1",
             "account_slug":"account-slug",
             "change":{"old_name":"MyAccount","new_name":"MyAccount Renamed","old_description":"Description","new_description":"Description Updated"},
             "operation":"/account/rename",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }
    { // Close
        AuditLogFixture logFixture;
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/close")
                .addParam("account_id", EMPTY_ACCOUNT_ID)};
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"e8dd9f8-73bbcd6b-4b14fe18-bdfd3352",
             "account_slug":"empty-account",
             "change":{},
             "operation":"/account/close",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }
    { // Reopen
        AuditLogFixture logFixture;
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/reopen")
                .addParam("account_id", EMPTY_ACCOUNT_ID)};
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"e8dd9f8-73bbcd6b-4b14fe18-bdfd3352",
             "account_slug":"empty-account",
             "change":{},
             "operation":"/account/reopen",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }

} // Y_UNIT_TEST(qttk_account)

Y_UNIT_TEST(qttk_tvm)
{
    QuotatekaDefaultFixture fixture;

    { // Assign
        AuditLogFixture logFixture;
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/assign_tvm")
                .addParam("account_id", ACCOUNT_ID)
                .addParam("tvm_id", 123)};
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        EXPECT_EQ(
            logFixture.capturedLogsJson(),
            std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"d300377-89e8cd6b-4b36fe18-b2f7a1d1",
             "account_slug":"account-slug",
             "change":{"tvm_id":123},
             "operation":"/account/assign_tvm",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }
    { // Move
        AuditLogFixture logFixture;
        http::MockRequest request{
            http::POST,
            http::URL("http://localhost/account/move_tvm")
                .addParam("from_account_id", ACCOUNT_ID)
                .addParam("to_account_id", EMPTY_ACCOUNT_ID)
                .addParam("tvm_id", 123)};
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        auto expectedLogs = std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"d300377-89e8cd6b-4b36fe18-b2f7a1d1",
             "account_slug":"account-slug",
             "change":{"tvm_id":123},
             "operation":"/account/revoke_tvm",
             "staff_login":"login",
             "timestamp":0}
            )"), json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"e8dd9f8-73bbcd6b-4b14fe18-bdfd3352",
             "account_slug":"empty-account",
             "change":{"tvm_id":123},
             "operation":"/account/assign_tvm",
             "staff_login":"login",
             "timestamp":0}
            )")};
        EXPECT_EQ(logFixture.capturedLogsJson(), expectedLogs);
    }
    { // Revoke
        AuditLogFixture logFixture;
        http::MockRequest request{
                http::POST,
                http::URL("http://localhost/account/revoke_tvm")
                        .addParam("account_id", EMPTY_ACCOUNT_ID)
                        .addParam("tvm_id", 123)};
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(200, response.status);
        EXPECT_EQ(
                logFixture.capturedLogsJson(),
                std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"e8dd9f8-73bbcd6b-4b14fe18-bdfd3352",
             "account_slug":"empty-account",
             "change":{"tvm_id":123},
             "operation":"/account/revoke_tvm",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }
}

Y_UNIT_TEST(abcd_quotas)
{
    QuotatekaDefaultFixture fixture;
    AuditLogFixture logFixture;

    std::string updateProvisionRequest = R"({{
        "folderId": "{}",
        "abcServiceId": 42,
        "updatedProvisions": {},
        "knownProvisions": [
            {{
                "accountId": "{}",
                "knownProvisions": {}
            }}
        ],
        "author": {{
          "passportUid": "153",
          "staffLogin": "login"
        }}
    }})";
    auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            PROVIDER_ABCD_ID,
            ACCOUNT_ID);
    http::MockRequest request(http::POST, http::URL(updateUrl));

    request.body = fmt::format(
            updateProvisionRequest,
            FOLDER_ID,
            // updatedProvisions
            R"([{
                    "resourceKey": {
                        "resourceTypeKey": "resource1"
                    },
                    "providedAmount": 100,
                    "providedAmountUnitKey": "qp"
                },
                {
                    "resourceKey": {
                        "resourceTypeKey": "resource2"
                    },
                    "providedAmount": 0,
                    "providedAmountUnitKey": "qp"
                }])",
            ACCOUNT_ID,
            // knownProvisions
            R"([{
                    "resourceKey": {
                        "resourceTypeKey": "resource1"
                    },
                    "providedAmount": 0,
                    "providedAmountUnitKey": "qp"
                },
                {
                    "resourceKey": {
                        "resourceTypeKey": "resource2"
                    },
                    "providedAmount": 50,
                    "providedAmountUnitKey": "qp"
                }])"
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(200, response.status);
    EXPECT_EQ(
        logFixture.capturedLogsJson(),
        std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"d300377-89e8cd6b-4b36fe18-b2f7a1d1",
             "account_slug":"account-slug",
             "change":{"provider_id":"provider-a","dispensed_diff":{"resource2":-50,"resource1":100}},
             "operation":"/abcd/account/dispense_quota",
             "staff_login":"login",
             "timestamp":0}
        )")});
} // Y_UNIT_TEST(abcd_quotas)

Y_UNIT_TEST(qttk_quotas)
{
    QuotatekaDefaultFixture fixture;

    {  // Dispense
        proto::UpdateProviderQuotasRequest updateProto;
        {
            auto resource1 = updateProto.add_quotas();
            resource1->set_resource_id("resource1");
            resource1->set_limit(100);
            auto resource2 = updateProto.add_quotas();
            resource2->set_resource_id("resource2");
            resource2->set_limit(0);
        }

        AuditLogFixture logFixture;
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/dispense_quota")
                .addParam("account_id", ACCOUNT_ID).addParam("provider_id", "provider-a")
        );
        request.body = protoToString(updateProto);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(
                logFixture.capturedLogsJson(),
                std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"d300377-89e8cd6b-4b36fe18-b2f7a1d1",
             "account_slug":"account-slug",
             "change":{"provider_id":"provider-a","dispensed_diff":{"resource2":-50,"resource1":100}},
             "operation":"/account/dispense_quota",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }

    {  // Allocate
        proto::UpdateProviderQuotasRequest updateProto;
        {
            auto resource1 = updateProto.add_quotas();
            resource1->set_resource_id("resource1");
            resource1->set_limit(100);
            auto resource2 = updateProto.add_quotas();
            resource2->set_resource_id("resource3");
            resource2->set_limit(0);
        }

        AuditLogFixture logFixture;
        http::MockRequest request(
            http::POST,
            http::URL("http://localhost/account/allocate_quota")
                .addParam("account_id", ACCOUNT_ID).addParam("provider_id", "provider-a")
        );
        request.body = protoToString(updateProto);

        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(
                logFixture.capturedLogsJson(),
                std::vector<json::Value>{json::Value::fromString(R"(
            {"abc_slug":"client-x",
             "account_id":"d300377-89e8cd6b-4b36fe18-b2f7a1d1",
             "account_slug":"account-slug",
             "change":{"provider_id":"provider-a","allocated_diff":{"resource1":100,"resource3":-50}},
             "operation":"/account/allocate_quota",
             "staff_login":"login",
             "timestamp":0}
            )")});
    }
} // Y_UNIT_TEST(qttk_quotas)

} // Y_UNIT_TEST_SUITE(audit_log)

} // namespace maps::quotateka::tests
