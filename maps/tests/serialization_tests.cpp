#include <maps/infra/quotateka/datamodel/include/serialization.h>
#include <maps/infra/quotateka/datamodel/tests/proto_utils.h>
#include <maps/infra/quotateka/datamodel/tests/fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

using namespace datamodel;

Y_UNIT_TEST_SUITE(serialization)
{

Y_UNIT_TEST(quotas_update_from_proto)
{
    // empty
    EXPECT_EQ(
        deserializeFromProto(proto::UpdateProviderQuotasRequest()),
        Quotas()
    );

    proto::UpdateProviderQuotasRequest updateProto;
    auto entryProto = updateProto.add_quotas();
    entryProto->set_resource_id("Resource1");
    entryProto->set_limit(0);
    entryProto = updateProto.add_quotas();
    entryProto->set_resource_id("Resource2");
    entryProto->set_limit(153);

    EXPECT_EQ(
        deserializeFromProto(updateProto),
        Quotas({{"Resource1", 0}, {"Resource2", 153}})
    );
}

Y_UNIT_TEST(quotas_to_proto_list)
{
    auto quotas = QuotasByProvider{
        {"Provider1", {{"Resource1", 0}, {"Resource2", 153}}},
        {"Provider2", {{"Resource3", 321}}}
    };
    auto providersIndex = BundlesByProvider{
        {"Provider1", {
            .provider = ProviderRecord{.id = "Provider1", .abcSlug = "provider-1"},
            .resources = {
                ResourceRecord{.id = "Resource1", .providerId = "Provider1", .type = ResourceType::PerSecondLimit},
                ResourceRecord{.id = "Resource2", .providerId = "Provider1", .type = ResourceType::PerHourLimit},
            }
        }},
        {"Provider2", {
            .provider = ProviderRecord{.id = "Provider2", .abcSlug = "provider-2"},
            .resources = {
                ResourceRecord{.id = "Resource3", .providerId = "Provider2", .type = ResourceType::PerDayLimit},
            }
        }}
    };

    auto protoQuotasList = serializeQuotasToProto(quotas["Provider1"], providersIndex["Provider1"], EN_LOCALE);
    auto providerProto = serializeProviderToProto(providersIndex["Provider1"], EN_LOCALE);

    ASSERT_EQ(protoQuotasList.size(), 2);
    {   // Provider1 quotas
        proto::ProviderInfo expectedProviderProto;
        expectedProviderProto.set_id("Provider1");
        expectedProviderProto.set_abc_slug("provider-1");
        EXPECT_THAT(providerProto, NGTest::EqualsProto(expectedProviderProto));
        {   // Resource1 quota
            auto protoResourceEntry = findRepeatedEntry(
                protoQuotasList, [](const auto& entry) { return entry.resource().id() == "Resource1";});
            ASSERT_TRUE(protoResourceEntry);
            proto::ResourceQuota expectedQuotaProto;
            expectedQuotaProto.set_limit(0);
            auto resourceProto = expectedQuotaProto.mutable_resource();
            resourceProto->set_id("Resource1");
            resourceProto->set_type(proto::ResourceType::PerSecondLimit);
            EXPECT_THAT(*protoResourceEntry, NGTest::EqualsProto(expectedQuotaProto));
        }
        {   // Resource2 quota
            auto protoResourceEntry = findRepeatedEntry(
                protoQuotasList, [](const auto& entry) { return entry.resource().id() == "Resource2";});
            ASSERT_TRUE(protoResourceEntry);
            proto::ResourceQuota expectedQuotaProto;
            expectedQuotaProto.set_limit(153);
            auto resourceProto = expectedQuotaProto.mutable_resource();
            resourceProto->set_id("Resource2");
            resourceProto->set_type(proto::ResourceType::PerHourLimit);

            EXPECT_THAT(*protoResourceEntry, NGTest::EqualsProto(expectedQuotaProto));
        }
    }

    protoQuotasList = serializeQuotasToProto(quotas["Provider2"], providersIndex["Provider2"], EN_LOCALE);
    providerProto = serializeProviderToProto(providersIndex["Provider2"], EN_LOCALE);

    {   // Provider2 quotas
        proto::ProviderInfo expectedProviderProto;
        expectedProviderProto.set_id("Provider2");
        expectedProviderProto.set_abc_slug("provider-2");
        EXPECT_THAT(providerProto, NGTest::EqualsProto(expectedProviderProto));
        ASSERT_EQ(protoQuotasList.size(), 1);

        auto protoResourceEntry = protoQuotasList[0];
        proto::ResourceQuota expectedQuotaProto;
        expectedQuotaProto.set_limit(321);
        auto resourceProto = expectedQuotaProto.mutable_resource();
        resourceProto->set_id("Resource3");
        resourceProto->set_type(proto::ResourceType::PerDayLimit);
        EXPECT_THAT(protoResourceEntry, NGTest::EqualsProto(expectedQuotaProto));
    }
}

Y_UNIT_TEST(translations)
{
    EXPECT_EQ(
        deserializeTranslationsFromProto(ProtoTranslations()),
        json::Value(json::repr::ObjectRepr{})
    );

    auto translationsProto = toLocalizedProto(
        {{"en", "Yandex.Maps Router"}, {"ru", "Автомобильный маршрутизатор Яндекс.Карт"}}
    );

    EXPECT_EQ(
        deserializeTranslationsFromProto(translationsProto),
        json::Value(json::repr::ObjectRepr({
            {"en", json::Value("Yandex.Maps Router")},
            {"ru", json::Value("Автомобильный маршрутизатор Яндекс.Карт")}
        }))
    );
}

Y_UNIT_TEST(provider_and_resources_to_proto)
{
    auto provider = ProviderRecord{
        .id = "driving-router",
        .name = json::Value(json::repr::ObjectRepr({
            {"en", json::Value("Yandex.Maps Router")},
        })),
        .abcSlug = "maps-core-driving-router"
    };
    auto resources = ResourceRecords{
        ResourceRecord {
            .id = "driving-router-general",
            .providerId = "driving-router",
            .type = ResourceType::PerSecondLimit,
            .name = json::Value(json::repr::ObjectRepr({
                {"en", json::Value("Router access")},
                {"ru", json::Value("Автомаршрутизация")}
            })),
            .endpoints = json::Value(std::vector<json::Value>{
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/route") },
                    { "cost", json::Value(100) }
                })),
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/summary") },
                    { "cost", json::Value(500) }
                })),
            }),
            .defaultLimit = 100,
            .anonymLimit = 53
        },
        ResourceRecord {
            .id = "driving-router-specific",
            .providerId = "driving-router",
            .type = ResourceType::PerDayLimit,
            .name = json::Value(json::repr::ObjectRepr({
                {"en", json::Value("Router specific access")}
            }))
        }
    };

    // EN locale test
    auto expectedProtoEN = jsonToProto<proto::ProviderProfile>(R"({
        "id": "driving-router",
        "abc_slug": "maps-core-driving-router",
        "name": "Yandex.Maps Router",
        "resources": [{
            "id": "driving-router-general",
            "name": "Router access",
            "endpoints": [
                {"path": "/v1/route", "cost": 100},
                {"path": "/v1/summary", "cost": 500}
            ],
            "default_limit": "100",
            "anonym_limit": "53"
        },
        {
            "id": "driving-router-specific",
            "type": "PerDayLimit",
            "name": "Router specific access"
        }]
    })");
    EXPECT_THAT(
        serializeProviderToProto(provider, resources, EN_LOCALE),
        NGTest::EqualsProto(expectedProtoEN)
    );

    // RU locale test
    auto expectedProtoRU = jsonToProto<proto::ProviderProfile>(R"({
        "id": "driving-router",
        "abc_slug": "maps-core-driving-router",
        "name": "Yandex.Maps Router",
        "resources": [{
            "id": "driving-router-general",
            "name": "Автомаршрутизация",
            "endpoints": [
                {"path": "/v1/route", "cost": 100},
                {"path": "/v1/summary", "cost": 500}
            ],
            "default_limit": "100",
            "anonym_limit": "53"
        },
        {
            "id": "driving-router-specific",
            "type": "PerDayLimit",
            "name": "Router specific access"
        }]
    })");
    EXPECT_THAT(
        serializeProviderToProto(provider, resources, RU_LOCALE),
        NGTest::EqualsProto(expectedProtoRU)
    );
}

Y_UNIT_TEST(provider_from_proto)
{
    auto updateProto = jsonToProto<proto::UpdateProviderRequest>(R"({
        "abc_slug": "maps-core-driving-router",
        "tvm_ids": [12345],
        "localized_name": [
            {"lang": "en", "value": "Yandex.Maps Router"},
            {"lang": "ru", "value": "Автомобильный маршрутизатор"},
        ]
    })");

    auto expected = ProviderRecord {
        .id = "router",
        .tvmIds = std::vector<int32_t>{12345},
        .name = json::Value(json::repr::ObjectRepr({
            {"en", json::Value("Yandex.Maps Router")},
            {"ru", json::Value("Автомобильный маршрутизатор")},
        })),
        .abcSlug = "maps-core-driving-router"
    };

    EXPECT_EQ(
        expected,
        deserializeProviderFromProto(expected.id, updateProto)
    );
}

Y_UNIT_TEST(resources_from_proto)
{
    auto updateProto = jsonToProto<proto::UpdateProviderRequest>(R"({
        "resources": [{
            "id": "driving-router-general",
            "type": "PerSecondLimit",
            "localized_name": [
                {"lang": "ru", "value": "Автомобильная маршрутизация"},
            ],
            "endpoints": [{"path": "/v1/route", "cost": 42}],
            "default_limit": "100",
            "anonym_limit": "53"
        },
        {
            "id": "driving-router-specific",
            "type": "PerDayLimit",
            "localized_name": [
                {"lang": "en", "value": "Specific routing"},
            ],
            "endpoints": [{"path": "/v1/matrix", "cost": 123}]
        }]
    })");

    auto providerId = "some_provider";
    ResourceRecords expected = {
        ResourceRecord{
            .id = "driving-router-general",
            .providerId = providerId,
            .type = ResourceType::PerSecondLimit,
            .name = json::Value(json::repr::ObjectRepr({
                {"ru", json::Value("Автомобильная маршрутизация")},
            })),
            .endpoints = json::Value(std::vector<json::Value>{
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/route") },
                    { "cost", json::Value(42) }
                })),
            }),
            .defaultLimit = 100,
            .anonymLimit = 53,
        },
        ResourceRecord{
            .id = "driving-router-specific",
            .providerId = providerId,
            .type = ResourceType::PerDayLimit,
            .name = json::Value(json::repr::ObjectRepr({
                {"en", json::Value("Specific routing")},
            })),
            .endpoints = json::Value(std::vector<json::Value>{
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/matrix") },
                    { "cost", json::Value(123) }
                })),
            }),
        }
    };

    EXPECT_EQ(
        expected,
        deserializeResourcesFromProto(providerId, updateProto)
    );
}

Y_UNIT_TEST(inventory_to_proto)
{
    auto provider = ProviderRecord{
        .id = "driving-router",
        .name = json::Value(json::repr::ObjectRepr({
            {"en", json::Value("Yandex.Maps Router")},
        })),
        .abcSlug = "maps-core-driving-router"
    };
    ResourceRecords resources = {
        ResourceRecord{
            .id = "driving-router-general",
            .type = ResourceType::PerSecondLimit,
            .endpoints = json::Value(std::vector<json::Value>{
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/route") },
                    { "cost", json::Value(42) }
                })),
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/alternatives") },
                    { "cost", json::Value(666) }
                })),
            }),
            .defaultLimit = 100,
            .anonymLimit = 53,
        },
        ResourceRecord{
            .id = "driving-router-specific",
            .type = ResourceType::PerSecondLimit,
            .endpoints = json::Value(std::vector<json::Value>{
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/matrix") },
                    { "cost", json::Value(123) }
                })),
            }),
            .defaultLimit = 10,
            .anonymLimit = 5,
        }
    };
    TvmRecordsByAccount identities{
        {"AccountX", { AccountTvmRecord{.tvmId = 11}, AccountTvmRecord{.tvmId = 22}}},
        {"AccountY", {
            AccountTvmRecord{.tvmId = 111},
            AccountTvmRecord{.tvmId = 22, .scope="YY"}  // scoped tvm case
        }},
    };
    QuotaRecordsByAccount quotas{
        {"AccountX", {
            AccountQuotaRecord{.resourceId = "driving-router-general", .quota = 10, .allocated = 5},
        }},
        {"AccountY", {  // NB: Quota records with allocated=0 still included into Inventory
            AccountQuotaRecord{.resourceId = "driving-router-general", .quota = 100},
            AccountQuotaRecord{.resourceId = "driving-router-specific", .quota = 110, .allocated = 55}
        }},
    };

    auto expectedInventoryProto = jsonToProto<proto::ProviderInventory>(R"({
        "accounts": [
            { account_id: "AccountY", quota_id: "AccountY", identities: [{tvm_id: 111}, {tvm_id: 22, scope: "YY"}] },
            { account_id: "AccountX", quota_id: "AccountX", identities: [{tvm_id: 11}, {tvm_id: 22}] }
        ],
        "resources": [
            {
                resource_id: "driving-router-general",
                endpoints: [{"path": "/v1/route", "cost": 42}, {"path": "/v1/alternatives", "cost": 666}],
                default_limit: 100,
                anonym_limit: 53
            },
            {
                resource_id: "driving-router-specific",
                endpoints: [{"path": "/v1/matrix", "cost": 123}],
                default_limit: 10,
                anonym_limit: 5
            }
        ],
        "quotas": [
            {
                quota_id: "AccountY",
                quotas: [
                    { resource_id: "driving-router-general"},
                    { resource_id: "driving-router-specific", limit: 55 }
                ]
            },
            { quota_id: "AccountX", quotas: [{ resource_id: "driving-router-general", limit: 5 }] },
        ],
        "provider_id": "driving-router",
        "quotas_version": 1
    })");
    EXPECT_THAT(
        serializeInventoryToProto(provider, resources, identities, quotas),
        NGTest::EqualsProto(expectedInventoryProto)
    );
}

Y_UNIT_TEST(client_profile_to_proto)
{
    auto clientRecord = ClientRecord{
        .id = "some-client", .abcSlug = "some-client-abc",
        .created = chrono::parseIsoDateTime("2020-10-01T00:00:00Z"),
        .updated = chrono::parseIsoDateTime("2020-10-11T11:11:11Z")
    };

    {  // Empty bundle
        auto expectedProto = jsonToProto<proto::ClientProfile>(R"({
            id: "some-client",
            abc_slug: "some-client-abc",
            created: "2020-10-01T00:00:00Z",
            updated: "2020-10-11T11:11:11Z",
        })");
        EXPECT_THAT(
            serializeClientProfileToProto(clientRecord, ClientBundle{}, EN_LOCALE),
            NGTest::EqualsProto(expectedProto)
        );
    }

    ClientBundle clientBundle{
        .accounts = {
            AccountRecord{.id = "AccountX1", .clientId = "ClientX", .providerId="ProviderA", .name = "ClientX account"},
            AccountRecord{.id = "AccountX2", .clientId = "ClientX", .providerId="ProviderB", .name = "ClientX 2nd account"},
        },
        .totalQuotas = {
            {"ProviderA", {{"R1", 10}, {"R2", 100}}},
            {"ProviderB", {{"R1", 153}}}
        },
        .dispensedQuotas = {{"ProviderA", {{"R1", 7}}}, {"ProviderB", {{"R1", 93}}}},
        .allocatedQuotas = {{"ProviderA", {{"R1", 5}}}},
        .providers = {
            {"ProviderA", ProviderBundle{
                .provider = ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
                .resources = {
                    ResourceRecord{.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
                    ResourceRecord{.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerHourLimit},
                }
            }},
            {"ProviderB", ProviderBundle{
                .provider = ProviderRecord{.id = "ProviderB", .abcSlug = "provider-b"},
                .resources = {
                    ResourceRecord{.id = "R1", .providerId = "ProviderB", .type = ResourceType::PerDayLimit},
                }
            }}
        }
    };
    {
        auto expectedProto = jsonToProto<proto::ClientProfile>(R"({
            id: "some-client",
            abc_slug: "some-client-abc",
            created: "2020-10-01T00:00:00Z",
            updated: "2020-10-11T11:11:11Z",
            accounts: [
                {id: "AccountX1", name: "ClientX account", provider: {id: "ProviderA", abc_slug: "provider-a"}},
                {id: "AccountX2", name: "ClientX 2nd account", provider: {id: "ProviderB", abc_slug: "provider-b"}}
            ],
            quotas: [
                {
                    provider: {id: "ProviderB", abc_slug: "provider-b"},
                    total: [
                        {resource: { id: "R1", type: "PerDayLimit" }, limit: 153}
                    ],
                    dispensed: [
                        {resource: { id: "R1", type: "PerDayLimit" }, limit: 93}
                    ],
                    allocated: [],
                },
                {
                    provider: {id: "ProviderA", abc_slug: "provider-a"},
                    total: [
                        {resource: { id: "R2", type: "PerHourLimit" }, limit: 100},
                        {resource: { id: "R1", type: "PerSecondLimit" }, limit: 10},
                    ],
                    dispensed: [
                        {resource: { id: "R1", type: "PerSecondLimit" }, limit: 7}
                    ],
                    allocated: [
                        {resource: { id: "R1", type: "PerSecondLimit" }, limit: 5}
                    ],
                },
            ],
        })");
        EXPECT_THAT(
            serializeClientProfileToProto(clientRecord, clientBundle, EN_LOCALE),
            NGTest::EqualsProto(expectedProto)
        );
    }
}

Y_UNIT_TEST(clients_list_to_proto)
{
    auto clientsProto = serializeClientsToProto({
        ClientRecord {.id = "some-client", .abcSlug = "some-client-abc"},
        ClientRecord {.id = "another-client", .abcSlug = "another-client-abc"},
    });
    auto expectedProto = jsonToProto<proto::ClientsList>(R"({
        clients: [
            { id: "some-client", abc_slug: "some-client-abc" },
            { id: "another-client", abc_slug: "another-client-abc" }
        ]
    })");
    EXPECT_THAT(clientsProto, NGTest::EqualsProto(expectedProto));
}

Y_UNIT_TEST(account_to_proto)
{
    auto accountRecord = AccountRecord{
        .id = "d300377-89e8cd6b-4b36fe18-b2f7a1d1", .clientId = "client-id",
        .name = "Account Title", .description = "Account Details",
        .created = chrono::parseIsoDateTime("2020-10-01T00:00:00Z"),
        .updated = chrono::parseIsoDateTime("2020-10-11T11:11:11Z")
    };

    auto accountBundle = AccountBundle{
        .tvmRecords = {
            AccountTvmRecord{.tvmId = 123, .name = "tvm 123"},
            AccountTvmRecord{.tvmId = 456, .name = "tvm 456"}
        },
        .quotasBundle = {
            .providerId = "Provider1",
            .dispensed = {{"Resource1", 11}, {"Resource2", 222}},
            .allocated = {{"Resource1", 10}, {"Resource2", 200}}
        },
        .provider = ProviderBundle{
                .provider = {.id = "Provider1", .abcSlug = "provider-1"},
                .resources = {
                    {.id = "Resource1", .providerId = "Provider1", .type = ResourceType::PerSecondLimit},
                    {.id = "Resource2", .providerId = "Provider2", .type = ResourceType::PerDayLimit},
                }
        }
    };

    auto expectedProto = jsonToProto<proto::Account>(R"({
        id: "d300377-89e8cd6b-4b36fe18-b2f7a1d1",
        name: "Account Title",
        description: "Account Details",
        identities: [
            { tvm_id: 123, name: "tvm 123" }, { tvm_id: 456, name: "tvm 456" }
        ],
        quota: {
            provider: { id: "Provider1", abc_slug: "provider-1" },
            dispensed: [
                { resource: { id: "Resource2", type: "PerDayLimit" }, limit: 222 },
                { resource: { id: "Resource1" }, limit: 11 }
            ],
            allocated: [
                { resource: { id: "Resource2", type: "PerDayLimit" }, limit: 200 },
                { resource: { id: "Resource1" }, limit: 10 }
            ],
        },
        created: "2020-10-01T00:00:00Z",
        updated: "2020-10-11T11:11:11Z"
    })");
    EXPECT_THAT(
        serializeAccountToProto(accountRecord, accountBundle, EN_LOCALE),
        NGTest::EqualsProto(expectedProto)
    );
}

Y_UNIT_TEST(accounts_list_to_proto)
{
    auto accountsProto = serializeAccountsToProto(
        AccountRecords{
            {.id="ccf9e0cd-328c3bd9-508f824f-85fe9b7f", .providerId="ProviderA", .name="Some Account"},
            {.id="394104a3-6f0ff7dd-279bfd27-e48e2053", .providerId="ProviderB", .name="Another Account", .isClosed=true},
        },
        BundlesByProvider{
            {"ProviderA", {.provider = {.id="ProviderA", .abcSlug="maps-core-a"}}},
            {"ProviderB", {.provider = {.id="ProviderB", .abcSlug="maps-core-b"}}},
        },
        EN_LOCALE
    );
    auto expectedProto = jsonToProto<proto::AccountsList>(R"({
        accounts: [
            {
                id: "ccf9e0cd-328c3bd9-508f824f-85fe9b7f",
                name: "Some Account",
                provider: {id: "ProviderA", abc_slug: "maps-core-a"}
            },
            {
                id: "394104a3-6f0ff7dd-279bfd27-e48e2053",
                name: "Another Account",
                provider: {id: "ProviderB", abc_slug: "maps-core-b"},
                is_closed: true
            }
        ]
    })");
    EXPECT_THAT(accountsProto, NGTest::EqualsProto(expectedProto));

    // Missing provider will raise std::out_of_range
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        serializeAccountsToProto(
            {{.id="ccf9e0cd-328c3bd9-508f824f-85fe9b7f", .providerId="ProviderA"}},
            BundlesByProvider{}, EN_LOCALE
        ),
        std::out_of_range, "key not found"
    );
}

}  // Y_UNIT_TEST_SUITE

}  // namespace maps::quotateka::tests
