#include <maps/infra/quotateka/proto/configuration.pb.h>
#include <maps/infra/quotateka/server/lib/audit.h>
#include <maps/infra/quotateka/server/lib/abcd/provisions.h>
#include <maps/infra/quotateka/server/lib/abcd/accounts.h>
#include <maps/infra/quotateka/server/tests/fixture.h>
#include <maps/infra/quotateka/server/tests/proto_utils.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <fmt/format.h>

namespace maps::quotateka::tests {

namespace proto = yandex::maps::proto::quotateka;

namespace {

const std::string DB_CONTENT_SCRIPT =
    (ArcadiaSourceRoot() +
     "/maps/infra/quotateka/migrations/tests/sample_data.sql");

const std::string EXISTING_ACCOUNT_ID = "d4d31379-ccf1-4bd9-bfd6-e622611adc9b";
const std::string EXISTING_FOLDER_ID = "86f18e61-5dc5-42c8-ac5c-56c01f676533";

} // anonymous namespace

Y_UNIT_TEST_SUITE(abcd_api) {

Y_UNIT_TEST(create_account)
{
    constexpr auto providerAbdcId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    constexpr auto folderId = "f02c2b0e-ffae-4e8a-9177-e2db69f22c70";

    ServantFixture fixture;
    fixture.abc.addService("example-client-abc-slug", 42);
    fixture.abc.addService("abcd-test-service-slug", 142);
    // Preset DB content with single client record
    fixture.insert<datamodel::ClientsTable>({
        {.id = "abc:example-client-abc-slug", .abcSlug = "example-client-abc-slug", .abcId = 42},
        {.id = "abc:abcd-test-service-slug", .abcSlug = "abcd-test-service-slug", .abcId = 142},
    });
    fixture.insert<datamodel::ProvidersTable>({
        {.id = "driving-router", .abcdId = providerAbdcId, .abcSlug = "maps-core-driving-router"}
    });

    const auto createAccountUrl = fmt::format(
        "http://localhost/quotaManagement/v1/providers/{}/accounts",
        providerAbdcId
    );
    const auto requestAuthor = R"({"passportUid": "1120000000000001", "staffLogin": "login"})";
    { // Successful creation of a new account with existing client
        http::MockRequest request(http::POST, http::URL(createAccountUrl));
        // DB is initialized with abcServiceId=42 for example client
        request.body = fmt::format(
            R"({{
                "key": "maps-core-teaspoon",
                "displayName": "Maps Core Teaspoon",
                "folderId": "{}",
                "abcServiceId": 42,
                "freeTier": false,
                "author": {}
            }})",
            folderId,
            requestAuthor);
        auto response = yacare::performTestRequest(request);
        auto responseValue = json::Value::fromString(response.body);
        auto accountId = responseValue["accountId"].as<std::string>();
        EXPECT_EQ(200, response.status);
        EXPECT_STREQ(response.headers["Content-Type"].c_str(), "application/json");
        EXPECT_EQ(
            responseValue,
            json::Value::fromString(fmt::format(
                R"({{
                    "accountId": "{}",
                    "key": "maps-core-teaspoon",
                    "displayName": "Maps Core Teaspoon",
                    "folderId": "{}",
                    "deleted": false,
                    "freeTier": false
                }})",
                accountId,
                folderId)));

        auto txn = fixture.pgPool().masterReadOnlyTransaction();
        auto accounts = datamodel::AccountManager{txn}.lookupAccounts(
            {.folderId = folderId});
        EXPECT_EQ(accounts.size(), 1u);
        EXPECT_EQ(accounts[0].slug, "maps-core-teaspoon");
        EXPECT_EQ(accounts[0].name, "Maps Core Teaspoon");
    }
    { // Successful creation for a new account along with a new client
        http::MockRequest request(http::POST, http::URL(createAccountUrl));
        // DB is initialized with abcServiceId=42 for example client
        request.body = fmt::format(
            R"({{
                "key": "maps-core-teaspoon-2",
                "displayName": "Maps Core Teaspoon 2",
                "folderId": "{}",
                "abcServiceId": 142,
                "freeTier": false,
                "author": {}
            }})",
            folderId,
            requestAuthor);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);

        auto txn = fixture.pgPool().masterReadOnlyTransaction();
        auto accounts = datamodel::AccountManager{txn}.lookupAccounts(
            {.accountSlug = "maps-core-teaspoon-2", .folderId = folderId});
        EXPECT_EQ(accounts.size(), 1u);
        EXPECT_EQ(accounts[0].slug, "maps-core-teaspoon-2");
        EXPECT_EQ(accounts[0].name, "Maps Core Teaspoon 2");
    }
    { // Fail of creation for already existing account
        http::MockRequest request(http::POST, http::URL(createAccountUrl));
        // DB is initialized with abcServiceId=42 for example client
        request.body = fmt::format(
            R"({{
                "key": "maps-core-teaspoon",
                "displayName": "Maps Core Teaspoon",
                "folderId": "{}",
                "abcServiceId": 42,
                "freeTier": false,
                "author": {}
            }})",
            folderId,
            requestAuthor);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(409, response.status);
        EXPECT_EQ(
            R"({"message": "Account Maps Core Teaspoon already exists"})",
            response.body
        );
    }
    { // Fail of creation an account with invalid slug format
        http::MockRequest request(http::POST, http::URL(createAccountUrl));
        // DB is initialized with abcServiceId=42 for example client
        request.body = fmt::format(
            R"({{
                "key": "WRONG SLUG FORMAT",
                "displayName": "Maps Core Teaspoon",
                "folderId": "{}",
                "abcServiceId": 42,
                "freeTier": false,
                "author": {}
            }})",
            folderId,
            requestAuthor);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, response.status);
        EXPECT_EQ(R"({"message": "Invalid slug format: WRONG SLUG FORMAT"})", response.body);
    }
}

Y_UNIT_TEST(get_account)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    // Account GET
    { // Successful get
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_getOne",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_STREQ(response.headers["Content-Type"].c_str(), "application/json");

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(fmt::format(
                R"({{
                    "accountId": "{}",
                    "key": "test-account",
                    "displayName": "example account",
                    "folderId": "{}",
                    "deleted": false,
                    "freeTier": false
                }})",
                EXISTING_ACCOUNT_ID,
                EXISTING_FOLDER_ID)));
    }
    { // Successful get with provisions
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_getOne",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "withProvisions": true,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto responseValue = json::Value::fromString(response.body);
        EXPECT_EQ(responseValue["provisions"], json::Value::fromString(R"([
                {"resourceKey": {"resourceTypeKey": "general"},
                 "providedAmount": 10,
                 "allocatedAmount": 5,
                 "providedAmountUnitKey": "qp",
                 "allocatedAmountUnitKey": "qp"
                }
            ])"));
    }
    { // Failure for nonexistent account
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_getOne",
            abcdProviderId,
            "nonexistent");
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ(R"({"message": "No such account: nonexistent"})", response.body);
    }
    { // Failure for nonexistent abc id
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_getOne",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 1
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ(R"({"message": "Not found client with ABC id: 1"})", response.body);
    }
}

Y_UNIT_TEST(json_parse_error)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    auto getUrl = fmt::format(
        "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_getOne",
        abcdProviderId,
        EXISTING_ACCOUNT_ID);
    http::MockRequest request(http::POST, http::URL(getUrl));
    request.body = "";
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(400, response.status);
    EXPECT_EQ(response.headers["Content-Type"], "application/json");
    EXPECT_EQ(R"({"message": "Failed to parse json request body"})", response.body);
}

Y_UNIT_TEST(list_accounts)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    // Account GET
    { // Successful get without provisions
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/_getPage",
            abcdProviderId);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "limit": 100,
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_STREQ(response.headers["Content-Type"].c_str(), "application/json");

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(fmt::format(
                R"({{
                    "accounts": [
                        {{
                            "accountId": "{}",
                            "key": "test-account",
                            "displayName": "example account",
                            "folderId": "{}",
                            "deleted": false,
                            "freeTier": false
                        }}
                    ],
                    "nextPageToken": "{}"
                }})",
                EXISTING_ACCOUNT_ID,
                EXISTING_FOLDER_ID,
                EXISTING_ACCOUNT_ID)));
    }
    { // Successful get with provisions
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/_getPage",
            abcdProviderId);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "limit": 100,
                "withProvisions": true,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto responseValue = json::Value::fromString(response.body);
        EXPECT_EQ(
            responseValue["accounts"][0]["provisions"],
            json::Value::fromString(R"([
                {"resourceKey": {"resourceTypeKey": "general"},
                 "providedAmount": 10,
                 "allocatedAmount": 5,
                 "providedAmountUnitKey": "qp",
                 "allocatedAmountUnitKey": "qp"
                }
            ])"));
    }
    { // test the withProvisions: true works fine with no quotas for the account
        fixture.insert<datamodel::AccountsTable>({datamodel::AccountRecord{
            .id = "57bdce25-2ca7bdfe-ad36cbb-bceb81c6",
            .clientId = "example-client-id",
            .providerId = "driving-router",
            .slug = "acbd-account",
            .name = "abcd account with no quotas",
            .folderId = "86f18e61-5dc5-42c8-ac5c-56c01f676533",
            .isClosed = false,
        }});
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/_getPage",
            abcdProviderId);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "limit": 100,
                "withProvisions": true,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto responseValue = json::Value::fromString(response.body);
        EXPECT_EQ(
            responseValue["accounts"][0]["provisions"],
            json::Value::fromString(R"([])"));
    }
    { // Empty response for nonexistent folder
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/_getPage",
            abcdProviderId);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "limit": 100,
                "withProvisions": true,
                "includeDeleted": false,
                "folderId": "nonexistent-folder",
                "abcServiceId": 1
            }})");
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(
                R"({"accounts": [], "nextPageToken": ""})"));
    }
    { // Failure for bad request
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/_getPage",
            abcdProviderId);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "withProvisions": true,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 1
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, response.status);
        EXPECT_EQ(R"({"message": "Value at `/limit' does not exist"})", response.body);
    }
    { // Pagination
        // Add second account that will always be the last one
        auto lastAccountId = "ffffffff-fff-ffff-ffff-ffffffffffff";
        fixture.insert<datamodel::AccountsTable>({
            datamodel::AccountRecord{
                .id = lastAccountId,
                .clientId = "example-client-id",
                .providerId = "driving-router",
                .slug = "test-account-2",
                .name = "example account 2",
                .folderId = "86f18e61-5dc5-42c8-ac5c-56c01f676533",
            }
        });

        // Use page token of first existing account
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/_getPage",
            abcdProviderId);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "limit": 100,
                "pageToken": "{}",
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_ACCOUNT_ID,
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(fmt::format(
                R"({{
                    "accounts": [
                        {{
                            "accountId": "{}",
                            "key": "test-account-2",
                            "displayName": "example account 2",
                            "folderId": "{}",
                            "deleted": false,
                            "freeTier": false
                        }}
                    ],
                    "nextPageToken": "{}"
                }})",
                lastAccountId,
                EXISTING_FOLDER_ID,
                lastAccountId)));
    }
    { // IncludeClosed
        // Add second account that will always be the last one
        auto lastClosedAccountId = "ffffffff-fff-ffff-ffff-fffffffffffe";
        fixture.insert<datamodel::AccountsTable>({
            datamodel::AccountRecord{
                .id = lastClosedAccountId,
                .clientId = "example-client-id",
                .providerId = "driving-router",
                .slug = "closed-account",
                .name = "closed account",
                .folderId = "86f18e61-5dc5-42c8-ac5c-56c01f676533",
                .isClosed = true,
            }
        });

        // Use page token of first existing account
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/_getPage",
            abcdProviderId);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.body = fmt::format(
            R"({{
                "limit": 1,
                "pageToken": "{}",
                "withProvisions": false,
                "includeDeleted": true,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_ACCOUNT_ID,
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(fmt::format(
                R"({{
                    "accounts": [
                        {{
                            "accountId": "{}",
                            "key": "closed-account",
                            "displayName": "closed account",
                            "folderId": "{}",
                            "deleted": true,
                            "freeTier": false
                        }}
                    ],
                    "nextPageToken": "{}"
                }})",
                lastClosedAccountId,
                EXISTING_FOLDER_ID,
                lastClosedAccountId)));
    }
}

Y_UNIT_TEST(update_provision_increase)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Successful update dispensed from 10 to 20
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 20,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_STREQ(response.headers["Content-Type"].c_str(), "application/json");

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(
                R"({"provisions": [{
                    "resourceKey": {
                        "resourceTypeKey": "general"
                    },
                    "providedAmount": 20,
                    "providedAmountUnitKey": "qp",
                    "allocatedAmount": 5,
                    "allocatedAmountUnitKey": "qp"
                }]})"));
    }
}

Y_UNIT_TEST(update_provision_decrease)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Successful update dispensed from 10 to 5
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 5,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(
                R"({"provisions": [{
                    "resourceKey": {
                        "resourceTypeKey": "general"
                    },
                    "providedAmount": 5,
                    "providedAmountUnitKey": "qp",
                    "allocatedAmount": 5,
                    "allocatedAmountUnitKey": "qp"
                }]})"));
    }
}

Y_UNIT_TEST(update_provision_move_within_folder)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string providerId = "driving-router";
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);
    std::string newAccountId = "new-account-id";
    { // Add second account to move quota to
        fixture.insert<datamodel::AccountsTable>({
            datamodel::AccountRecord{
                .id = newAccountId,
                .clientId = "example-client-id",
                .providerId = providerId,
                .name = "New account",
                .folderId = EXISTING_FOLDER_ID},
        });
        // Deallocate from existing account
        fixture.update<datamodel::AccountQuotasTable>({
            datamodel::AccountQuotaRecord{
                .accountId = EXISTING_ACCOUNT_ID,
                .providerId = providerId,
                .resourceId = "general",
                .quota = 10,
                .allocated = 0},
        });
    }

    std::string updateProvisionRequest = R"({{
        "folderId": "{}",
        "abcServiceId": 42,
        "updatedProvisions": {},
        "knownProvisions": [
            {{
                "accountId": "{}",
                "knownProvisions": {}
            }},
            {{
                "accountId": "{}",
                "knownProvisions": {}
            }}
        ],
        "author": {{
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 0 allocated general resource on driving-router

    // Move quota from old account to new account by decrease & increase
    { // Decrease quota from old account
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 0,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions old account
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])",
            newAccountId,
            // knownProvisions old account
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 0,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
    }
    { // Increase quota to new account
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions old account
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 0,
                "providedAmountUnitKey": "qp"
            }])",
            newAccountId,
            // knownProvisions old account
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 0,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(
                R"({"provisions": [{
                    "resourceKey": {
                        "resourceTypeKey": "general"
                    },
                    "providedAmount": 10,
                    "providedAmountUnitKey": "qp",
                    "allocatedAmount": 0,
                    "allocatedAmountUnitKey": "qp"
                }]})"));
    }
}

Y_UNIT_TEST(update_provision_failure_less_than_allocated)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Failure to update dispensed from 10 to 4 that is less than allocated
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 4,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(422, response.status);
        EXPECT_EQ(
            R"({"message": "Not allowed to set dispensed < allocated for general"})",
            response.body
        );
    }
}

Y_UNIT_TEST(update_provision_increase_ignore_accounts_from_different_provider)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);
    {  // add provider with account in the same folder
        fixture.insert<datamodel::ProvidersTable>({
            datamodel::ProviderRecord{
                .id = "abc:different-provider",
                .abcdId = "different-provider-abcd-id",
                .abcSlug = "different-provider"},
        });
        fixture.insert<datamodel::AccountsTable>({
            datamodel::AccountRecord{
                .id = "test-account-123",
                .clientId = "example-client-id",
                .providerId = "abc:different-provider",
                .name = "New account 123",
                .folderId = EXISTING_FOLDER_ID},
        });
    }

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Successful update dispensed from 10 to 20
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 20,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(
                R"({"provisions": [{
                    "resourceKey": {
                        "resourceTypeKey": "general"
                    },
                    "providedAmount": 20,
                    "providedAmountUnitKey": "qp",
                    "allocatedAmount": 5,
                    "allocatedAmountUnitKey": "qp"
                }]})"));
    }
}

Y_UNIT_TEST(update_provision_failure_folder_id_mismatch)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    std::string mismatchedFolderId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Failure to update quota due to mismatchedFolderId
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            mismatchedFolderId,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 5,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(412, response.status);
        EXPECT_EQ(
            fmt::format(R"({{"message": "Folder id mismatch. Expected: {}, got {}."}})",
                        EXISTING_FOLDER_ID, mismatchedFolderId),
            response.body
        );
    }
}

Y_UNIT_TEST(update_provision_ignore_zero_provisions)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string providerId = "driving-router";
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    std::string newAccountId = "new-account-id";
    std::string updateProvisionRequest = R"({{
        "folderId": "{}",
        "abcServiceId": 42,
        "updatedProvisions": {},
        "knownProvisions": [
            {}
        ],
        "author": {{
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";
    { // Add second account to move quota to
        fixture.insert<datamodel::AccountsTable>({
            datamodel::AccountRecord{
                .id = newAccountId,
                .clientId = "example-client-id",
                .providerId = providerId,
                .name = "New account",
                .folderId = EXISTING_FOLDER_ID},
        });
        fixture.insert<datamodel::ResourcesTable>({
            datamodel::ResourceRecord{
                .id = "heavy",
                .providerId = providerId,
                .type = ResourceType::PerSecondLimit,
                .name = json::Value::fromString(R"({"en": "Driving Router heavy API"})"),
                .defaultLimit = 0,
                .anonymLimit = 1},
        });
        // Deallocate from existing account
        fixture.insert<datamodel::AccountQuotasTable>({
            datamodel::AccountQuotaRecord{
                .accountId = EXISTING_ACCOUNT_ID,
                .providerId = providerId,
                .resourceId = "heavy",
                .quota = 0,
                .allocated = 0},
        });
        fixture.insert<datamodel::AccountQuotasTable>({
            datamodel::AccountQuotaRecord{
                .accountId = newAccountId,
                .providerId = providerId,
                .resourceId = "general",
                .quota = 0,
                .allocated = 0},
        });
        fixture.insert<datamodel::AccountQuotasTable>({
            datamodel::AccountQuotaRecord{
                .accountId = newAccountId,
                .providerId = providerId,
                .resourceId = "heavy",
                .quota = 0,
                .allocated = 0},
        });
    }

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Successful update dispensed from 10 to 15 and ignore zero-dispensed quotas from both sides
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 5,
                "providedAmountUnitKey": "qp"
            }])",
            R"(
            {
                "accountId": "d4d31379-ccf1-4bd9-bfd6-e622611adc9b",
                "knownProvisions":
                    [{
                        "resourceKey": {
                            "resourceTypeKey": "general"
                        },
                        "providedAmount": 10,
                        "providedAmountUnitKey": "qp"
                    },
                    {
                        "resourceKey": {
                            "resourceTypeKey": "nonexistent"
                        },
                        "providedAmount": 0,
                        "providedAmountUnitKey": "qp"
                    }]
            },
            {
                "accountId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "knownProvisions":
                    [{
                        "resourceKey": {
                            "resourceTypeKey": "general"
                        },
                        "providedAmount": 0,
                        "providedAmountUnitKey": "qp"
                    },
                    {
                        "resourceKey": {
                            "resourceTypeKey": "heavy"
                        },
                        "providedAmount": 0,
                        "providedAmountUnitKey": "qp"
                    }]
            }
            )"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);

        EXPECT_EQ(
            json::Value::fromString(response.body),
            json::Value::fromString(
                R"({"provisions": [{
                        "resourceKey": {
                            "resourceTypeKey": "general"
                        },
                        "providedAmount": 5,
                        "providedAmountUnitKey": "qp",
                        "allocatedAmount": 5,
                        "allocatedAmountUnitKey": "qp"
                    },
                    {
                        "resourceKey": {
                            "resourceTypeKey": "heavy"
                        },
                        "providedAmount": 0,
                        "providedAmountUnitKey": "qp",
                        "allocatedAmount": 0,
                        "allocatedAmountUnitKey": "qp"
                    }
                ]})"));
    }
}

Y_UNIT_TEST(update_provision_known_quotas_mismatch)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Fail to update dispensed from 10 to 5 due to known provisions mismatch
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 5,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 100,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(422, response.status);
        EXPECT_EQ(R"({"message": "knownProvisions conflict"})", response.body);
    }
}

Y_UNIT_TEST(update_provision_invalid_provided_unit_key)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Fail to update dispensed from 10 to 5 due to known invalid unit key
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 5,
                "providedAmountUnitKey": "INVALID UNIT KEY"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, response.status);
        EXPECT_EQ(R"({"message": "incorrect providedAmountUnitKey"})", response.body);
    }
}

Y_UNIT_TEST(update_provision_unchanged_resources_skipped)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);
    { // Add another resource
        fixture.insert<datamodel::ResourcesTable>({
            datamodel::ResourceRecord{
                .id = "heavy",
                .providerId = "driving-router"},
        });
        // Add existing account some quota
        fixture.insert<datamodel::AccountQuotasTable>({
            datamodel::AccountQuotaRecord{
                .accountId = EXISTING_ACCOUNT_ID,
                .providerId = "driving-router",
                .resourceId = "heavy",
                .quota = 10,
                .allocated = 0},
        });
    }

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Successful update dispensed from 10 to 5
        // Skipping existing resource from knownProvisions as it wasn't changed
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 5,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
    }
}

Y_UNIT_TEST(update_provision_unchanged_resources_skipped_from_other_account)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);
    {   // Add another resource
        fixture.insert<datamodel::ResourcesTable>({
            datamodel::ResourceRecord{
                .id = "heavy",
                .providerId = "driving-router"},
        });
        // Add another account
        fixture.insert<datamodel::AccountsTable>({datamodel::AccountRecord{
            .id = "57bdce25-2ca7bdfe-ad36cbb-bceb81c6",
            .clientId = "example-client-id",
            .providerId = "driving-router",
            .slug = "acbd-account",
            .name = "abcd account with no quotas",
            .folderId = "86f18e61-5dc5-42c8-ac5c-56c01f676533",
            .isClosed = false,
        }});
        // Add new account some quota
        fixture.insert<datamodel::AccountQuotasTable>({
            datamodel::AccountQuotaRecord{
                .accountId = "57bdce25-2ca7bdfe-ad36cbb-bceb81c6",
                .providerId = "driving-router",
                .resourceId = "heavy",
                .quota = 10,
                .allocated = 0},
        });
    }

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
          "passportUid": "1120000000000001",
          "staffLogin": "login"
        }}
    }})";

    uint64_t adminUser = 1120000000000001;
    // Set adminUser role in client-x ABC
    fixture.abc.addMember("example-client-abc-slug", 42, adminUser, abc::MANAGERS_ROLE_SCOPE);

    // Existing data:
    // existing client has 10 dispensed and 5 allocated general resource on driving-router

    { // Successful update dispensed from 10 to 5
        // Skipping existing resource from knownProvisions as it wasn't changed
        auto updateUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_provide",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(updateUrl));

        request.body = fmt::format(
            updateProvisionRequest,
            EXISTING_FOLDER_ID,
            // updatedProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 5,
                "providedAmountUnitKey": "qp"
            }])",
            EXISTING_ACCOUNT_ID,
            // knownProvisions
            R"([{
                "resourceKey": {
                    "resourceTypeKey": "general"
                },
                "providedAmount": 10,
                "providedAmountUnitKey": "qp"
            }])"
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
    }
}

Y_UNIT_TEST(get_account_authorized)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    { // Request with forbidden tvm id
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_getOne",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.headers["X-Ya-Src-Tvm-Id"] = "123";
        request.body = fmt::format(
            R"({{
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(403, response.status);
        EXPECT_EQ(
            R"({"message": "TVM ID 123 is not allowed to access quotateka ABCD API"})",
            response.body
        );
    }
    { // Request with abcd tvm id
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_getOne",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.headers["X-Ya-Src-Tvm-Id"] = "2023015";  // abcd testing
        request.body = fmt::format(
            R"({{
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42
            }})",
            EXISTING_FOLDER_ID);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
    }
}

Y_UNIT_TEST(rename_account)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    const auto requestAuthor = R"({"passportUid": "1120000000000001", "staffLogin": "login"})";
    { // Request with forbidden tvm id
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_rename",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.headers["X-Ya-Src-Tvm-Id"] = "123";
        request.body = fmt::format(
            R"({{
                "folderId": "f02c2b0e-ffae-4e8a-9177-e2db69f22c70",
                "abcServiceId": 1,
                "displayName": "Renamed",
                "author": {}
            }})",
            requestAuthor);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(403, response.status);
        EXPECT_EQ(
            R"({"message": "TVM ID 123 is not allowed to access quotateka ABCD API"})",
            response.body
        );
    }
    { // Request with abcd tvm id
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}/_rename",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::POST, http::URL(getUrl));
        request.headers["X-Ya-Src-Tvm-Id"] = "2023015";  // abcd testing
        request.body = fmt::format(
            R"({{
                "folderId": "f02c2b0e-ffae-4e8a-9177-e2db69f22c70",
                "abcServiceId": 1,
                "displayName": "Renamed",
                "author": {}
            }})",
            requestAuthor);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        EXPECT_STREQ(
            json::Value::fromString(response.body)["displayName"].as<std::string>().c_str(),
            "Renamed"
        );
    }
}

Y_UNIT_TEST(close_account)
{
    ServantFixture fixture;

    // ABCD ID for providerId="driving-router"
    std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.database().executeSqlFile(DB_CONTENT_SCRIPT);

    const auto requestAuthor = R"({"passportUid": "1120000000000001", "staffLogin": "login"})";
    { // Request  with abcd tvm id
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}",
            abcdProviderId,
            EXISTING_ACCOUNT_ID);
        http::MockRequest request(http::DELETE, http::URL(getUrl));
        request.headers["X-Ya-Src-Tvm-Id"] = "123";
        request.body = fmt::format(
            R"({{
                "withProvisions": false,
                "includeDeleted": false,
                "folderId": "{}",
                "abcServiceId": 42,
                "author": {}
            }})",
            EXISTING_FOLDER_ID,
            requestAuthor);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(403, response.status);
        EXPECT_STREQ(
            json::Value::fromString(response.body)["message"].as<std::string>().c_str(),
            "TVM ID 123 is not allowed to access quotateka ABCD API");
    }
    { // Request with abcd tvm id
        // ABCD ID for providerId="driving-router"
        std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
        {
            // Add another account
            fixture.insert<datamodel::AccountsTable>({datamodel::AccountRecord{
                .id = "57bdce25-2ca7bdfe-ad36cbb-bceb81c6",
                .clientId = "example-client-id",
                .providerId = "driving-router",
                .slug = "acbd-account",
                .name = "abcd account with provided quotas",
                .folderId = "86f18e61-5dc5-42c8-ac5c-56c01f676533",
                .isClosed = false,
            }});
            // Add new account some quota
            fixture.insert<datamodel::AccountQuotasTable>({
                datamodel::AccountQuotaRecord{
                    .accountId = "57bdce25-2ca7bdfe-ad36cbb-bceb81c6",
                    .providerId = "driving-router",
                    .resourceId = "general",
                    .quota = 10,
                    .allocated = 0},
            });
        }
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}",
            abcdProviderId,
            "57bdce25-2ca7bdfe-ad36cbb-bceb81c6");
        http::MockRequest request(http::DELETE, http::URL(getUrl));
        request.body = fmt::format(R"({{"author": {}}})", requestAuthor);
        request.headers["X-Ya-Src-Tvm-Id"] = "2023015";  // abcd testing
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, response.status);
        EXPECT_STREQ(
            json::Value::fromString(response.body)["message"].as<std::string>().c_str(),
            "Account 'abcd account with provided quotas' has provided quota"
        );
    }
    { // Request with abcd tvm id
        // ABCD ID for providerId="driving-router"
        std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
        {
            // Add another account
            fixture.insert<datamodel::AccountsTable>({datamodel::AccountRecord{
                .id = "57bdce25-2ca7bdfe-ad36cbb-bceb81c7",
                .clientId = "example-client-id",
                .providerId = "driving-router",
                .slug = "acbd-account-2",
                .name = "abcd account with allocated quotas",
                .folderId = "86f18e61-5dc5-42c8-ac5c-56c01f676533",
                .isClosed = false,
            }});
            // Add new account some quota
            fixture.insert<datamodel::AccountQuotasTable>({
                datamodel::AccountQuotaRecord{
                    .accountId = "57bdce25-2ca7bdfe-ad36cbb-bceb81c7",
                    .providerId = "driving-router",
                    .resourceId = "general",
                    .quota = 0,
                    .allocated = 10}, // over-commit by allocated
            });
        }
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}",
            abcdProviderId,
            "57bdce25-2ca7bdfe-ad36cbb-bceb81c7");
        http::MockRequest request(http::DELETE, http::URL(getUrl));
        request.body = fmt::format(R"({{"author": {}}})", requestAuthor);
        request.headers["X-Ya-Src-Tvm-Id"] = "2023015";  // abcd testing
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(400, response.status);
        EXPECT_STREQ(
            json::Value::fromString(response.body)["message"].as<std::string>().c_str(),
            "Account 'abcd account with allocated quotas' has allocated quota"
        );
    }
    { // Request with abcd tvm id
        // ABCD ID for providerId="driving-router"
        std::string abcdProviderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
        {
            // Add another account
            fixture.insert<datamodel::AccountsTable>({datamodel::AccountRecord{
                .id = "57bdce25-2ca7bdfe-ad36cbb-bceb81c8",
                .clientId = "example-client-id",
                .providerId = "driving-router",
                .slug = "acbd-account-3",
                .name = "abcd account with no provided quotas",
                .folderId = "86f18e61-5dc5-42c8-ac5c-56c01f676533",
                .isClosed = false,
            }});
            // Add new account some quota
            fixture.insert<datamodel::AccountQuotasTable>({
              datamodel::AccountQuotaRecord{
                  .accountId = "57bdce25-2ca7bdfe-ad36cbb-bceb81c8",
                  .providerId = "driving-router",
                  .resourceId = "general",
                  .quota = 0,
                  .allocated = 0},
            });
        }
        auto getUrl = fmt::format(
            "http://localhost/quotaManagement/v1/providers/{}/accounts/{}",
            abcdProviderId,
            "57bdce25-2ca7bdfe-ad36cbb-bceb81c8");
        http::MockRequest request(http::DELETE, http::URL(getUrl));
        request.body = fmt::format(R"({{"author": {}}})", requestAuthor);
        request.headers["X-Ya-Src-Tvm-Id"] = "2023015";  // abcd testing
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(204, response.status);
        EXPECT_EQ(response.body, "");
    }
}
} // Y_UNIT_TEST_SUITE(abcd_api)

} // namespace maps::quotateka::tests
