#include <maps/infra/quotateka/datamodel/include/client_manager.h>
#include <maps/infra/quotateka/datamodel/tests/fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

using namespace datamodel;

namespace {

template <typename Record>
Record resetTimestamps(
    Record record, const chrono::TimePoint& timestamp = chrono::TimePoint())
{
    record.created = timestamp;
    record.updated = timestamp;
    return record;
}

template <typename Record>
std::vector<Record> resetAllTimestamps(
    std::vector<Record> records, const chrono::TimePoint& timestamp = chrono::TimePoint())
{
    for (auto& record: records) {
        record = resetTimestamps(record, timestamp);
    }
    return records;
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(client_manager)
{

Y_UNIT_TEST(test_client_upsert_and_load)
{
    DatabaseFixture fixture;

    {  // Loading of not existing client should throw exception
        auto txn = fixture.pgPool().slaveTransaction();
        EXPECT_THROW(
            ClientManager{txn}.loadClient("NoSuchClient"),
            ClientNotFoundError
        );
    }
    {  // Register client
        auto txn = fixture.pgPool().masterWriteableTransaction();
//        auto timeBefore = chrono::TimePoint::clock::now();
        auto record = ClientManager{txn}.upsertClientByAbc("maps-mobile-proxy", 42);
//        auto timeAfter = chrono::TimePoint::clock::now();
        txn->commit();
        // Check timestamps
// TODO: fix timezones (expect DB entries in UTC)
//        EXPECT_LE(timeBefore, record.created);
//        EXPECT_LE(record.created, timeAfter);
        EXPECT_TRUE(record.created == record.updated);
        // Check record content
        EXPECT_EQ(
            (ClientRecord{.id = "abc:maps-mobile-proxy", .abcSlug = "maps-mobile-proxy", .abcId = 42}),
            resetTimestamps(record)
        );
    }

    {  // Load client
        auto txn = fixture.pgPool().slaveTransaction();
        auto record = ClientManager{txn}.loadClient("abc:maps-mobile-proxy");
        EXPECT_EQ(
            (ClientRecord{.id = "abc:maps-mobile-proxy", .abcSlug = "maps-mobile-proxy", .abcId = 42}),
            resetTimestamps(record)
        );
    }
}

Y_UNIT_TEST(test_client_lookup_and_bundle)
{
    DatabaseFixture fixture;
    fixture.insert<ProvidersTable>({
        ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
        ProviderRecord{.id = "ProviderB", .abcSlug = "provider-b"},
    })
    .insert<ResourcesTable>(ResourceRecords{ // quotas_version increment on every row
        {.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
        {.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerHourLimit},
        {.id = "R1", .providerId = "ProviderB", .type = ResourceType::PerDayLimit},
    })
    .insert<ClientsTable>({
        ClientRecord{.id = "ClientX", .abcSlug = "client-x", .abcId = 1},
        ClientRecord{.id = "ClientY", .abcSlug = "client-y", .abcId = 2},
    })
    //NB: No quotas for ClientY
    .insert<ClientQuotasTable>(ClientQuotaRecords{
        {.clientId = "ClientX", .providerId = "ProviderA", .resourceId = "R1", .quota = 10},
        {.clientId = "ClientX", .providerId = "ProviderA", .resourceId = "R2", .quota = 11},
        {.clientId = "ClientX", .providerId = "ProviderB", .resourceId = "R1", .quota = 153}}
    )
    .insert<AccountsTable>(AccountRecords{
        {.id = "AccountX1", .clientId = "ClientX", .providerId="ProviderA", .slug="account-x1", .name = "ClientX account"},
        {.id = "AccountX2", .clientId = "ClientX", .providerId="ProviderB", .slug="account-x2", .name = "ClientX 2nd account"},
        {.id = "AccountY1", .clientId = "ClientY", .providerId="ProviderB", .slug="account-y1", .name = "ClientY account"},
    });

    // zero-diff update, quotas_versions should not be changed
    fixture.update<ResourcesTable>({
        ResourceRecord{.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
        ResourceRecord{.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerHourLimit},
        ResourceRecord{.id = "R1", .providerId = "ProviderB", .type = ResourceType::PerDayLimit},
    });

    auto txn = fixture.pgPool().slaveTransaction();
    auto clientManager = ClientManager{txn};

    {  // client by account
        auto record = clientManager.lookupClientByAccount("AccountX2");
        EXPECT_EQ(
            (ClientRecord{.id = "ClientX", .abcSlug = "client-x", .abcId = 1}),
            resetTimestamps(record)
        );
        record = clientManager.lookupClientByAccount("AccountY1");
        EXPECT_EQ(
            (ClientRecord{.id = "ClientY", .abcSlug = "client-y", .abcId = 2}),
            resetTimestamps(record)
        );
        EXPECT_THROW(
            clientManager.lookupClientByAccount("NoSuchAccount"),
            ClientNotFoundError
        );
    }

    {  // clients lookup by provider
        auto records = clientManager.lookupClientsByProvider("ProviderA");
        auto expectedRecords = ClientRecords{
            ClientRecord{.id = "ClientX", .abcSlug = "client-x", .abcId = 1}
        };
        EXPECT_EQ(expectedRecords, resetAllTimestamps(records));

        records = clientManager.lookupClientsByProvider("ProviderB");
        std::sort(records.begin(), records.end(), [](auto a, auto b) { return a.id < b.id; });
        expectedRecords = ClientRecords{
            ClientRecord{.id = "ClientX", .abcSlug = "client-x", .abcId = 1},
            ClientRecord{.id = "ClientY", .abcSlug = "client-y", .abcId = 2}
        };
        EXPECT_EQ(expectedRecords, resetAllTimestamps(records));

        EXPECT_EQ(
            ClientRecords{},
            clientManager.lookupClientsByProvider("NoSuchProviders")
        );
    }

    // Client bundles will contain all registered providers info
    BundlesByProvider allProvidersBundle{
        {"ProviderA", ProviderBundle{
            .provider = ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a", .quotasVersion = 3},
            .resources = {
                ResourceRecord{.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
                ResourceRecord{.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerHourLimit},
            }
        }},
        {"ProviderB", ProviderBundle{
            .provider = ProviderRecord{.id = "ProviderB", .abcSlug = "provider-b", .quotasVersion = 2},
            .resources = {
                ResourceRecord{.id = "R1", .providerId = "ProviderB", .type = ResourceType::PerDayLimit},
            }
        }}
    };

    {  // ClientX bundle
        auto expectedBundle = ClientBundle{
            .accounts = {
                {.id = "AccountX1", .clientId = "ClientX", .providerId="ProviderA", .slug="account-x1", .name = "ClientX account"},
                {.id = "AccountX2", .clientId = "ClientX", .providerId="ProviderB", .slug="account-x2", .name = "ClientX 2nd account"},
            },
            .totalQuotas = {
                {"ProviderA", {{"R1", 10}, {"R2", 11}}},
                {"ProviderB", {{"R1", 153}}}
            },
            .providers = allProvidersBundle
        };
        auto bundle = clientManager.loadClientBundle("ClientX");
        bundle.accounts = resetAllTimestamps(bundle.accounts);
        EXPECT_EQ(expectedBundle, bundle);
    }
    {  // ClientY bundle
        auto expectedBundle = ClientBundle{
            .accounts = {
                {.id = "AccountY1", .clientId = "ClientY", .providerId="ProviderB", .slug="account-y1", .name = "ClientY account"},
            },
            .providers = allProvidersBundle
        };
        auto bundle = clientManager.loadClientBundle("ClientY");
        bundle.accounts = resetAllTimestamps(bundle.accounts);
        EXPECT_EQ(expectedBundle, bundle);
    }
}

} // Y_UNIT_TEST_SUITE(client_manager)

} // namespace maps::quotateka::tests
