#include <maps/infra/quotateka/datamodel/include/provider_manager.h>
#include <maps/infra/quotateka/datamodel/tests/fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

using namespace datamodel;

Y_UNIT_TEST_SUITE(provider_manager)
{

Y_UNIT_TEST(test_provider_update)
{
    DatabaseFixture fixture;
    auto& dbPool = fixture.pgPool();

    {  // Lookup non-existing provider
        auto txn = dbPool.slaveTransaction();
        EXPECT_THROW(
            ProviderManager{txn}.lookupProvider("NoSuchProvider"),
            ProviderNotFoundError
        );
        EXPECT_THROW(
            ProviderManager{txn}.lookupProviderByAbc("abc:no-such-provider"),
            ProviderNotFoundError
        );
    }

    auto providerA = ProviderRecord{
        .id = "driving-router",
        .name = json::Value(json::repr::ObjectRepr({
            {"en", json::Value("Yandex.Maps Router")},
        })),
        .abcSlug = "maps-core-driving-router",
    };
    auto providerB = ProviderRecord{
        .id = "bicycle-router",
        .name = json::Value(json::repr::ObjectRepr({
            {"en", json::Value("Yandex.Maps Bicycle Router")}
        })),
        .abcSlug = "maps-core-bicycle-router",
    };
    {   // New providers
        auto txn = dbPool.masterWriteableTransaction();
        ProviderManager{txn}.updateProvider(providerA);
        ProviderManager{txn}.updateProvider(providerB);
        txn->commit();
    }
    {  // Check both providers were inserted
        auto txn = dbPool.slaveTransaction();
        EXPECT_EQ(
            providerA,
            ProviderManager{txn}.lookupProvider(providerA.id)
        );
        EXPECT_EQ(
            providerA,
            ProviderManager{txn}.lookupProviderByAbc(providerA.abcSlug)
        );
        EXPECT_EQ(
            providerB,
            ProviderManager{txn}.lookupProviderByAbc(providerB.abcSlug)
        );
    }

    providerB.name = json::Value(json::repr::ObjectRepr({
        {"en", json::Value("Yandex.Maps Bicycle Router")},
        {"ru", json::Value("Веломаршрутизатор Яндекс.Карт")}
    }));
    {   // Update providerB
        auto txn = dbPool.masterWriteableTransaction();
        ProviderManager{txn}.updateProvider(providerB);
        txn->commit();
    }
    {  // Check providerB was updated
        auto txn = dbPool.slaveTransaction();
        EXPECT_EQ(
            ProviderManager{txn}.lookupProvider(providerB.id),
            providerB
        );
    }
} // Y_UNIT_TEST(test_provider_update)

Y_UNIT_TEST(test_resources_update)
{
    DatabaseFixture fixture;
    // Init db with 2 provider records
    fixture.insert<ProvidersTable>({
       ProviderRecord{
           .id = "driving-router",
           .abcSlug = "maps-core-driving-router"
       },
       ProviderRecord{
           .id = "bicycle-router",
           .abcSlug = "maps-core-bicycle-router"
       }
    });

    auto& dbPool = fixture.pgPool();

    auto resource1 = ResourceRecord {
        .id = "general",
        .providerId = "driving-router",
        .type = ResourceType::PerSecondLimit,
        .name = json::Value(json::repr::ObjectRepr({
            {"en", json::Value("Router access")}
        })),
        .endpoints = json::Value(std::vector<json::Value>{
            json::Value(json::repr::ObjectRepr({
                { "path", json::Value("/v1/route") },
                { "cost", json::Value(42) }
            })),
        }),
        .defaultLimit = 1,
        .anonymLimit = 2
    };
    auto resource2 = ResourceRecord {
        .id = "specific",
        .providerId = "driving-router",
        .type = ResourceType::PerDayLimit,
        .endpoints = json::Value(std::vector<json::Value>{
            json::Value(json::repr::ObjectRepr({
                { "path", json::Value("/v1/matrix") },
                { "cost", json::Value(123) }
            })),
        }),
    };
    {   // Add single resource
        auto txn = dbPool.masterWriteableTransaction();
        auto resources = ResourceRecords{resource1};
        ProviderManager{txn}.updateProviderResources("driving-router", resources);
        txn->commit();
    }
    {   // Add resource with invalid providerId
        auto txn = dbPool.masterWriteableTransaction();
        auto invalidResource = resource1;
        invalidResource.providerId = "NoSuchProvider";
        auto resources = ResourceRecords{invalidResource};
        EXPECT_THROW(
            ProviderManager{txn}.updateProviderResources("driving-router", resources),
            maps::LogicError
        );
        EXPECT_THROW(
            ProviderManager{txn}.updateProviderResources("NoSuchProvider", resources),
            pqxx::foreign_key_violation
        );
    }
    {   // Check single resource was added
        auto txn = dbPool.slaveTransaction();
        EXPECT_EQ(
            ProviderManager{txn}.lookupProviderResources("driving-router"),
            ResourceRecords{resource1}
        );
    }
    {   // Update resource1 and add resource2
        auto txn = dbPool.masterWriteableTransaction();
        resource1.anonymLimit = 153;
        auto endpoints = std::vector<json::Value>(
            resource1.endpoints.begin(),
            resource1.endpoints.end()
        );
        endpoints.emplace_back(
            json::Value(json::repr::ObjectRepr({
                { "path", json::Value("/v1/alternatives") },
                { "cost", json::Value(666) }
            }))
        );
        resource1.endpoints = json::Value(endpoints);

        auto resources = ResourceRecords{resource1, resource2};
        ProviderManager{txn}.updateProviderResources("driving-router", resources);
        txn->commit();
    }
    {   // Check both resources are there
        auto txn = dbPool.slaveTransaction();
        EXPECT_EQ(
            ProviderManager{txn}.lookupProviderResources("driving-router"),
            ResourceRecords({resource1, resource2})
        );
    }
    {   // Delete resource1, leave only resource2
        auto txn = dbPool.masterWriteableTransaction();
        auto resources = ResourceRecords{resource2};
        ProviderManager{txn}.updateProviderResources("driving-router", resources);
        txn->commit();
    }
    {   // Check just resource2 is left
        auto txn = dbPool.slaveTransaction();
        EXPECT_EQ(
            ProviderManager{txn}.lookupProviderResources("driving-router"),
            ResourceRecords{resource2}
        );
    }

    {   // Check no resources for second provider
        auto txn = dbPool.slaveTransaction();
        EXPECT_EQ(
            ProviderManager{txn}.lookupProviderResources("bicycle-router"),
            ResourceRecords{}
        );
    }
}

Y_UNIT_TEST(resource_providers)
{
    DatabaseFixture fixture;
    fixture.insert<ProvidersTable>({
       ProviderRecord{.id = "driving-router", .abcSlug = "maps-core-driving-router"},
       ProviderRecord{.id = "bicycle-router", .abcSlug = "maps-core-bicycle-router"}
    })
    .insert<ResourcesTable>({ // quotas_version increment on every row
        ResourceRecord{
            .id = "general",
            .providerId = "driving-router",
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
        },
        ResourceRecord{
            .id = "specific",
            .providerId = "driving-router",
            .type = ResourceType::PerHourLimit,
            .endpoints = json::Value(std::vector<json::Value>{
                json::Value(json::repr::ObjectRepr({
                    { "path", json::Value("/v1/matrix") },
                    { "cost", json::Value(123) }
                })),
            }),
        },
        ResourceRecord{
            .id = "RPD",
            .providerId = "bicycle-router",
            .type = ResourceType::PerDayLimit,
            .endpoints = json::Value(std::vector<json::Value>{}),
        },
    });

    // quotas_version increment on every row
    fixture.insert<ResourcesTable>({
        ResourceRecord{
            .id = "short-live",
            .providerId = "driving-router",
            .type = ResourceType::PerSecondLimit,
            .endpoints = json::Value(std::vector<json::Value>{}),
        }
    });
    fixture.removeById<ResourcesTable>("short-live");

    auto txn = fixture.pgPool().slaveTransaction();
    EXPECT_EQ(
        ProviderManager{txn}.loadProvidersBundles({"NoSuchProvider"}),
        BundlesByProvider{}
    );

    auto expectedBundles = BundlesByProvider{
        {"driving-router", ProviderBundle{
            .provider = ProviderRecord{.id = "driving-router", .abcSlug = "maps-core-driving-router", .quotasVersion = 5},
            .resources = {
                ResourceRecord{
                    .id = "general",
                    .providerId = "driving-router",
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
                },
                ResourceRecord{
                    .id = "specific",
                    .providerId = "driving-router",
                    .type = ResourceType::PerHourLimit,
                    .endpoints = json::Value(std::vector<json::Value>{
                        json::Value(json::repr::ObjectRepr({
                            { "path", json::Value("/v1/matrix") },
                            { "cost", json::Value(123) }
                        })),
                    }),
                },
            }
        }},
        { "bicycle-router", ProviderBundle{
            .provider = ProviderRecord{.id = "bicycle-router", .abcSlug = "maps-core-bicycle-router", .quotasVersion = 2},
            .resources = {
                ResourceRecord{
                    .id = "RPD",
                    .providerId = "bicycle-router",
                    .type = ResourceType::PerDayLimit,
                    .endpoints = json::Value(std::vector<json::Value>{}),
                }
            }
        }},
    };
    EXPECT_EQ(
        expectedBundles,
        ProviderManager{txn}.loadProvidersBundles({"driving-router", "bicycle-router"})
    );
    // Test load without filtering by id
    EXPECT_EQ(
        expectedBundles,
        ProviderManager{txn}.loadProvidersBundles()
    );
}

Y_UNIT_TEST(test_quota_versions_on_provider_update)
{
    DatabaseFixture fixture;
    fixture.insert<ProvidersTable>({
        ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
    }).insert<ResourcesTable>({ // quotas_version increment on every row
        ResourceRecord{.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
        ResourceRecord{.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerDayLimit},
    });

    auto txn = fixture.pgPool().slaveTransaction();

    auto expectedBundles = BundlesByProvider{
        { "ProviderA", ProviderBundle{
            .provider = ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a", .quotasVersion = 3},
            .resources = {
                ResourceRecord{.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
                ResourceRecord{.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerDayLimit}
            }
        }},
    };
    EXPECT_EQ(
        expectedBundles,
        ProviderManager{txn}.loadProvidersBundles({"ProviderA"})
    );

    fixture.update<ProvidersTable>({
        ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"}
    });

    EXPECT_EQ(
        expectedBundles,
        ProviderManager{txn}.loadProvidersBundles({"ProviderA"})
    );
}

Y_UNIT_TEST(provider_bundle_without_resources)
{
    DatabaseFixture fixture;
    fixture
        .insert<ProvidersTable>({
            ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
        });

    auto txn = fixture.pgPool().slaveTransaction();

    auto expectedBundles = BundlesByProvider{
        {"ProviderA",
         ProviderBundle{
             .provider = ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a", .quotasVersion = 1},
         .resources = {}}},
    };

    EXPECT_EQ(expectedBundles, ProviderManager{txn}.loadProvidersBundles());
}

} // Y_UNIT_TEST_SUITE(provider_manager)

} // namespace maps::quotateka::tests
