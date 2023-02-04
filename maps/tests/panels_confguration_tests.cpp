#include <maps/infra/quotateka/server/tests/fixture.h>

#include <maps/infra/quotateka/datamodel/include/quotas_manager.h>
#include <maps/infra/quotateka/datamodel/include/client_orm.h>
#include <maps/infra/quotateka/datamodel/include/provider_orm.h>
#include <maps/infra/quotateka/datamodel/include/account_orm.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/json/include/value.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

namespace {

void presetDbContent(pgpool3::Pool& pgPool, bool scoped = false) {
    auto txn = pgPool.masterWriteableTransaction();
    datamodel::ProvidersGateway{*txn}.insert(
        datamodel::ProviderRecord{
            .id = "core-bicycle-router", .abcSlug = "maps-core-bicycle-router",
            .scopeSupport = scoped ? ScopeSupportType::AccountSlugAsScope : ScopeSupportType::Disable
        }
    );
    datamodel::ResourcesGateway{*txn}.insert(datamodel::ResourceRecords{
        {
            .id = "general",
            .providerId = "core-bicycle-router",
            .endpoints = json::Value::fromString(R"([
                {"path": "/one", "cost": 1},
                {"path": "/two", "cost": 2}
            ])")
        },
        {.id = "heavy", .providerId = "core-bicycle-router"},
    });

    datamodel::ClientsGateway{*txn}.insert(datamodel::ClientRecords{
        {.id = "abc:client-x", .abcSlug = "client-x", .abcId = 1},
        {.id = "abc:client-y", .abcSlug = "client-y", .abcId = 2}
    });

    auto accountRecords = datamodel::AccountRecords{
        {.id = "111", .clientId = "abc:client-x", .providerId = "core-bicycle-router", .slug = "account-a"},
        {.id = "222", .clientId = "abc:client-x", .providerId = "core-bicycle-router", .slug = "account-b"},
        {.id = "333", .clientId = "abc:client-y", .providerId = "core-bicycle-router", .slug = "account-c"}
    };

    auto accountTvmRecords = datamodel::AccountTvmRecords{
        {.tvmId = 12345, .accountId = "111", .providerId = "core-bicycle-router", .name = "stable-tvm", .scope = scoped ? "account-a" : EMPTY_SCOPE},
        {.tvmId = 67890, .accountId = "111", .providerId = "core-bicycle-router", .name = "testing-tvm", .scope = scoped ? "account-a" : EMPTY_SCOPE},
    };

    if (scoped) {
        accountRecords.push_back(
            {.id = "444", .clientId = "abc:client-x", .providerId = "core-bicycle-router", .slug = "another-account-a"});

        accountTvmRecords.push_back(
            {.tvmId = 12345, .accountId = "444", .providerId = "core-bicycle-router", .name = "another-stable-tvm", .scope = "another-account-a"});
    }

    datamodel::AccountsGateway{*txn}.insert(accountRecords);

    datamodel::AccountTvmGateway{*txn}.insert(accountTvmRecords);

    datamodel::AccountQuotasGateway{*txn}.insert(datamodel::AccountQuotaRecords{
        {.accountId = "111", .providerId = "core-bicycle-router", .resourceId = "general", .quota = 1},
        {.accountId = "222", .providerId = "core-bicycle-router", .resourceId = "heavy", .quota = 1},
    });

    txn->commit();
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(panels_configuration)
{

Y_UNIT_TEST(provider)
{
    ServantFixture fixture;
    presetDbContent(fixture.pgPool());

    {  // Invalid provider_abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/provider?provider_abc=no-such-provider"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("No API provider associated with ABC: no-such-provider", response.body);
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/provider?provider_abc=maps-core-bicycle-router"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        // Expect both client-x accounts, but no client-y
        auto expectedJson = R"({
            "provider": {
                "id" : "core-bicycle-router",
                "abc" : "maps-core-bicycle-router",
                "resources": [
                    {
                        "id": "general",
                        "endpoints": [{"cost":1, "path":"/one"}, {"cost":2, "path":"/two"}]
                    },
                    {
                        "id": "heavy",
                        "endpoints": []
                    }
                ]
            },
            "clients": [
                {
                    "abc": "client-y",
                    "accounts": [
                        {"slug": "account-c",
                         "id": "333",
                         "uuid5": "5394bbd2-b158-528a-b9b0-bda6b4d4c9c1",
                         "name":""}
                    ],
                    "identities": []
                },
                {
                    "abc": "client-x",
                    "accounts": [
                        {"slug":"account-a",
                         "id": "111",
                         "uuid5": "02b79189-8de2-553a-b70e-dd4f584184af",
                         "name": ""},
                        {"slug": "account-b",
                         "id": "222",
                         "uuid5": "923f207f-cc6a-5cd3-8439-0005ac3468d3",
                         "name": ""}
                    ],
                    "identities": [{"tvm": 12345},{"tvm": 67890}]
                }
            ]
        })";
        EXPECT_EQ(json::Value::fromString(expectedJson), json::Value::fromString(response.body));
    }
}

Y_UNIT_TEST(scoped_provider)
{
    ServantFixture fixture;
    presetDbContent(fixture.pgPool(), true);

    {  // Invalid provider_abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/provider?provider_abc=no-such-provider"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("No API provider associated with ABC: no-such-provider", response.body);
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/provider?provider_abc=maps-core-bicycle-router"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        // Expect both client-x accounts, but no client-y
        auto expectedJson = R"({
            "provider": {
                "id" : "core-bicycle-router",
                "abc" : "maps-core-bicycle-router",
                "resources": [
                    {
                        "id": "general",
                        "endpoints": [{"cost":1, "path":"/one"}, {"cost":2, "path":"/two"}]
                    },
                    {
                        "id": "heavy",
                        "endpoints": []
                    }
                ]
            },
            "clients": [
                {
                    "abc": "client-y",
                    "accounts": [
                        {"slug": "account-c",
                         "id": "333",
                         "uuid5": "5394bbd2-b158-528a-b9b0-bda6b4d4c9c1",
                         "name":""}
                    ],
                    "identities": []
                },
                {
                    "abc": "client-x",
                    "accounts": [
                        {"slug":"account-a",
                         "id": "111",
                         "uuid5": "02b79189-8de2-553a-b70e-dd4f584184af",
                         "name": ""},
                        {"slug": "account-b",
                         "id": "222",
                         "uuid5": "923f207f-cc6a-5cd3-8439-0005ac3468d3",
                         "name": ""},
                        {"slug":"another-account-a",
                         "id": "444",
                         "uuid5": "dbbe722b-65f1-5270-b149-aa192aa3e4c5",
                         "name": ""}
                    ],
                    "identities": [
                        {
                            "tvm": 12345,
                            "scope": "account-a"
                        },
                        {
                            "tvm": 67890,
                            "scope": "account-a"
                        },
                        {
                            "tvm": 12345,
                            "scope": "another-account-a"
                        }
                    ]
                }
            ]
        })";
        EXPECT_EQ(json::Value::fromString(expectedJson), json::Value::fromString(response.body));
    }
}

Y_UNIT_TEST(account)
{
    ServantFixture fixture;
    presetDbContent(fixture.pgPool());

    {  // Invalid client_abc
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/account?client_abc=123&account_slug=wtf"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
    }
    {  // Invalid slug
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/account?client_abc=client-x&account_slug=wtf"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(404, response.status);
        EXPECT_EQ("No account with slug: wtf", response.body);
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/account?client_abc=client-x&account_slug=account-a"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedJson = R"({
            "identities": [
                {"id": 12345, "name": "stable-tvm"},
                {"id": 67890, "name": "testing-tvm"}
            ],
            "uuid5": "02b79189-8de2-553a-b70e-dd4f584184af",
            "provider":{
                "abc": "maps-core-bicycle-router",
                "id": "core-bicycle-router",
                "resources": [
                    {
                        "id": "general",
                        "endpoints": [{"path": "/one", "cost": 1}, {"path": "/two", "cost": 2}]
                    },
                    {
                        "id":"heavy",
                        "endpoints":[]
                    }
                ]
            }
        })";
        EXPECT_EQ(json::Value::fromString(expectedJson), json::Value::fromString(response.body));
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/account?client_abc=client-y&account_slug=account-c"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        // 'account-c' of 'client-y' has no quotas and no tvm2
        auto expectedJson = R"({
            "identities": [],
            "uuid5": "5394bbd2-b158-528a-b9b0-bda6b4d4c9c1",
            "provider":{
                "abc": "maps-core-bicycle-router",
                "id": "core-bicycle-router",
                "resources": [
                    {
                        "id": "general",
                        "endpoints": [{"path": "/one", "cost": 1}, {"path": "/two", "cost": 2}]
                    },
                    {
                        "id":"heavy",
                        "endpoints":[]
                    }
                ]
            }
        })";
        EXPECT_EQ(json::Value::fromString(expectedJson), json::Value::fromString(response.body));
    }
}

Y_UNIT_TEST(account_of_scoped_provider)
{
    ServantFixture fixture;
    presetDbContent(fixture.pgPool(), true);

    {
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/account?client_abc=client-x&account_slug=account-a"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedJson = R"({
            "identities": [
                {"id": 12345, "name": "stable-tvm", "scope": "account-a"},
                {"id": 67890, "name": "testing-tvm", "scope": "account-a"}
            ],
            "uuid5": "02b79189-8de2-553a-b70e-dd4f584184af",
            "provider":{
                "abc": "maps-core-bicycle-router",
                "id": "core-bicycle-router",
                "resources": [
                    {
                        "id": "general",
                        "endpoints": [{"path": "/one", "cost": 1}, {"path": "/two", "cost": 2}]
                    },
                    {
                        "id":"heavy",
                        "endpoints":[]
                    }
                ]
            }
        })";
        EXPECT_EQ(json::Value::fromString(expectedJson), json::Value::fromString(response.body));
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL("http://localhost/panel/account?client_abc=client-x&account_slug=another-account-a"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(200, response.status);
        auto expectedJson = R"({
            "identities": [
                {"id": 12345, "name": "another-stable-tvm", "scope": "another-account-a"}
            ],
            "uuid5": "dbbe722b-65f1-5270-b149-aa192aa3e4c5",
            "provider":{
                "abc": "maps-core-bicycle-router",
                "id": "core-bicycle-router",
                "resources": [
                    {
                        "id": "general",
                        "endpoints": [{"path": "/one", "cost": 1}, {"path": "/two", "cost": 2}]
                    },
                    {
                        "id":"heavy",
                        "endpoints":[]
                    }
                ]
            }
        })";
        EXPECT_EQ(json::Value::fromString(expectedJson), json::Value::fromString(response.body));
    }
}

}  // Y_UNIT_TEST_SUITE

}  // namespace maps::quotateka::tests
