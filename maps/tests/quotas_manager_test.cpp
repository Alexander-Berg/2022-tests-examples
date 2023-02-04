#include <maps/infra/quotateka/datamodel/include/quotas_manager.h>
#include <maps/infra/quotateka/datamodel/include/client_orm.h>
#include <maps/infra/quotateka/datamodel/include/account_orm.h>
#include <maps/infra/quotateka/datamodel/include/provider_orm.h>
#include <maps/infra/quotateka/datamodel/tests/fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

using namespace datamodel;

namespace {

ProviderRecords presetProviders()
{
    return {
        ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
        ProviderRecord{.id = "ProviderB", .abcSlug = "provider-b"},
    };
}
ResourceRecords presetResources()
{
    return {
        ResourceRecord{.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
        ResourceRecord{.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerHourLimit},
        ResourceRecord{.id = "R1", .providerId = "ProviderB", .type = ResourceType::PerDayLimit},
    };
}
ClientRecords presetClients()
{
    return {
        ClientRecord{.id = "ClientX", .abcSlug = "client-x", .abcId = 1},
        ClientRecord{.id = "ClientY", .abcSlug = "client-y", .abcId = 2},
    };
}

}  // anonymous namespace

Y_UNIT_TEST_SUITE(quotas_manager)
{

Y_UNIT_TEST(test_client_quotas)
{
    DatabaseFixture fixture;
    fixture.insert<ProvidersTable>(presetProviders())
        .insert<ResourcesTable>(presetResources())
        .insert<ClientsTable>(presetClients());

    auto& dbPool = fixture.pgPool();

    {
        auto txn = dbPool.slaveTransaction();
        EXPECT_TRUE(QuotasManager(txn).loadClientQuotas("ClientA").empty());
    }

    {
        auto txn = dbPool.masterWriteableTransaction();
        QuotasManager(txn).updateClientQuotas(
            "ClientX", std::make_pair("ProviderA", Quotas{{"R1", 1}})
        );
        QuotasManager(txn).updateClientQuotas(
            "ClientX", std::make_pair("ProviderB", Quotas{{"R1", 5}})
        );
        QuotasManager(txn).updateClientQuotas(
            "ClientY", std::make_pair("ProviderA", Quotas{{"R2", 153}})
        );
        txn->commit();
    }

    {
        auto txn = dbPool.slaveTransaction();
        EXPECT_EQ(
            QuotasManager(txn).loadClientQuotas("ClientX"),
            (QuotasByProvider{{"ProviderA", {{"R1", 1}}}, {"ProviderB", {{"R1", 5}}}})
        );
        EXPECT_EQ(
            QuotasManager(txn).loadClientQuotas("ClientY"),
            (QuotasByProvider{{"ProviderA", {{"R2", 153}}}})
        );
    }
}

Y_UNIT_TEST(account_quotas)
{
    AccountRecord accountX1 = {
        .id = "AccountX1", .clientId = "ClientX", .providerId="ProviderA",
        .slug="account-x1", .isClosed = false
    };
    AccountRecord accountX2 = {
        .id = "AccountX2", .clientId = "ClientX", .providerId="ProviderB",
        .slug="account-x2", .isClosed = false
    };
    AccountRecord accountY1 = {
        .id = "AccountY1", .clientId = "ClientY", .providerId="ProviderA",
        .slug="account-y1", .isClosed = false
    };
    AccountRecord accountY2 = {
        .id = "AccountY2", .clientId = "ClientY", .providerId="ProviderA",
        .slug="account-y2", .isClosed = true
    };

    DatabaseFixture fixture;
    fixture.insert<ProvidersTable>(presetProviders())
        .insert<ResourcesTable>(presetResources())
        .insert<ClientsTable>(presetClients())
        .insert<AccountsTable>({accountX1, accountX2, accountY1, accountY2});
    auto& dbPool = fixture.pgPool();

    {   // No accounts - no quotas
        auto txn = dbPool.slaveTransaction();
        EXPECT_TRUE(QuotasManager(txn).loadAccountsQuotasByIds(std::vector<AccountId>()).empty());
    }
    {   // Failed dispense - invalid resource
        auto txn = dbPool.masterWriteableTransaction();
        EXPECT_THROW(
            QuotasManager(txn).dispenseQuota(accountX1, {{"no-such-resource", 10}}),
            pqxx::foreign_key_violation
        );
    }
    {   // Failed dispense - resource from another provider
        auto txn = dbPool.masterWriteableTransaction();
        EXPECT_THROW(
            ClientQuotasGateway{*txn}.insert(ClientQuotaRecord
                {.clientId = "abc:client-x", .providerId = "provider-a", .resourceId = "resource1", .quota = 100}),
            pqxx::foreign_key_violation
        );
    }
    {    // Failed allocate - no dispensed (account_quotas table entry doesn't exist)
        auto txn = dbPool.masterWriteableTransaction();
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            QuotasManager(txn).allocateQuota(accountX1, {{"R1", 153}}),
            InvalidQuotaError, "No dispensed quota to allocate"
        );
    }
    {   // Failed dispense - closed account
        auto txn = dbPool.masterWriteableTransaction();
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            QuotasManager(txn).dispenseQuota(accountY2, {{"R1", 10}}),
            InvalidQuotaError, "Unable to dispense quota into closed account"
        );
    }
    {  // Dispense zero - changes nothing if table entry doesn't exist
        auto txn = dbPool.masterWriteableTransaction();
        auto quotasManager = QuotasManager(txn);
        quotasManager.dispenseQuota(accountX1, {{"R1", 0}});
        // Expect no table entry created
        EXPECT_EQ(
            quotasManager.loadAccountsQuotasByIds({"AccountX1"}),
            QuotaBundlesByAccount()
        );
        txn->commit();
    }

    {   // Successful dispense
        auto txn = dbPool.masterWriteableTransaction();
        auto quotasManager = QuotasManager(txn);
        auto dispensedDiff = quotasManager.dispenseQuota(accountX1, {{"R1", 10}});
        EXPECT_EQ(
            quotasManager.loadAccountsQuotasByIds({"AccountX1"}),
            QuotaBundlesByAccount({
                {"AccountX1", {
                    .providerId = "ProviderA",
                    .dispensed = {{"R1", 10}},
                    .allocated = {{"R1", 0}}
                }}
            })
        );
        // acc1 diff 0 -> 10 = 10
        EXPECT_EQ(dispensedDiff, QuotasDiff({{"R1", 10}}));

        dispensedDiff = quotasManager.dispenseQuota(accountX2, {{"R1", 2}});
        EXPECT_EQ(
            quotasManager.loadAccountsQuotasByIds({"AccountX2"}),
            QuotaBundlesByAccount({
                {"AccountX2", {
                    .providerId = "ProviderB",
                    .dispensed = {{"R1", 2}},
                    .allocated = {{"R1", 0}},
                }}
            })
        );
        // acc2 diff 0 -> 2 = 2
        EXPECT_EQ(dispensedDiff, QuotasDiff({{"R1", 2}}));

        dispensedDiff = quotasManager.dispenseQuota(accountY1, {{"R2", 25}});
        EXPECT_EQ(
            quotasManager.loadAccountsQuotasByIds({"AccountY1"}),
            QuotaBundlesByAccount({
                {"AccountY1", {
                    .providerId = "ProviderA",
                    .dispensed = {{"R2", 25}},
                    .allocated = {{"R2", 0}},
                }}
            })
        );
        // accY1 diff 0 -> 25 = 25
        EXPECT_EQ(dispensedDiff, QuotasDiff({{"R2", 25}}));
        txn->commit();
    }

    {    // Failed allocate - not allowed exceed dispensed
        auto txn = dbPool.masterWriteableTransaction();
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            QuotasManager(txn).allocateQuota(accountX1, {{"R1", 100}}),
            InvalidQuotaError, "Not allowed to set allocated > dispensed for R1"
        );
    }
    {    // Failed allocate - account is closed
        auto txn = dbPool.masterWriteableTransaction();
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            QuotasManager(txn).allocateQuota(accountY2, {{"R2", 10}}),
            InvalidQuotaError, "Unable to allocate quota for closed account"
        );
    }
    {    // Successful allocate
        auto txn = dbPool.masterWriteableTransaction();
        auto allocatedDiff = QuotasManager(txn).allocateQuota(accountX1, {{"R1", 10}});
        EXPECT_EQ(
            QuotasManager(txn).loadAccountsQuotasByIds({"AccountX1"}),
            QuotaBundlesByAccount({
                {"AccountX1", {
                    .providerId = "ProviderA",
                    .dispensed = {{"R1", 10}},
                    .allocated = {{"R1", 10}}
                }}
            })
        );
        // acc1 diff 0 -> 10 = 10
        EXPECT_EQ(allocatedDiff, QuotasDiff({{"R1", 10}}));
        txn->commit();
    }
    {    // Failed dispense - cant' go lower than allocated
        auto txn = dbPool.masterWriteableTransaction();
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            QuotasManager(txn).dispenseQuota(accountX1, {{"R1", 1}}),
            InvalidQuotaError, "Not allowed to set dispensed < allocated for R1"
        );
    }
    {    // Successful de-allocate
        auto txn = dbPool.masterWriteableTransaction();
        auto allocatedDiff = QuotasManager(txn).allocateQuota(accountX1, {{"R1", 0}});
        EXPECT_EQ(
            QuotasManager(txn).loadAccountsQuotasByIds({"AccountX1"}),
            QuotaBundlesByAccount({
                {"AccountX1", {
                    .providerId = "ProviderA",
                    .dispensed = {{"R1", 10}},
                    .allocated = {{"R1", 0}},
                }}
            })
        );
        // acc1 diff 10 -> 0 = -10
        EXPECT_EQ(allocatedDiff, QuotasDiff({{"R1", -10}}));
        txn->commit();
    }
    {   // Successful un-dispense (set to zero)
        auto txn = dbPool.masterWriteableTransaction();
        auto dispensedDiff = QuotasManager(txn).dispenseQuota(accountX1, {{"R1", 0}});
        // Expect table entry removed
        EXPECT_EQ(
            QuotasManager(txn).loadAccountsQuotasByIds({"AccountX1"}),
            QuotaBundlesByAccount()
        );
        // acc1 diff 10 -> 0 = -10
        EXPECT_EQ(dispensedDiff, QuotasDiff({{"R1", -10}}));
        txn->commit();
    }
}

Y_UNIT_TEST(account_quotas_totals_validate)
{
    AccountRecord accountXA1 = {
        .id = "AccountXA1",
        .clientId = "ClientX",
        .providerId="ProviderA",
        .slug="account-xa1",
        .isClosed = false
    };
    AccountRecord accountXA2 = {
        .id = "AccountXA2",
        .clientId = "ClientX",
        .providerId="ProviderA",
        .slug="account-xa2",
        .isClosed = false
    };
    AccountRecord accountXB = {
        .id = "AccountXB",
        .clientId = "ClientX",
        .providerId="ProviderB",
        .slug="account-xb",
        .isClosed = false
    };
    AccountRecord accountYA = {
        .id = "AccountYA",
        .clientId = "ClientY",
        .providerId="ProviderA",
        .slug="account-ya",
        .isClosed = false
    };

    DatabaseFixture fixture;
    auto& dbPool = fixture.pgPool();
    fixture.insert<ProvidersTable>(presetProviders())
        .insert<ResourcesTable>(presetResources())
        .insert<ClientsTable>(presetClients())
        .insert<ClientQuotasTable>({
            {.clientId = "ClientX", .providerId = "ProviderA", .resourceId = "R1", .quota = 10},
            {.clientId = "ClientX", .providerId = "ProviderB", .resourceId = "R1", .quota = 5},
            {.clientId = "ClientY", .providerId = "ProviderA", .resourceId = "R1", .quota = 5}})
        .insert<AccountsTable>({accountXA1, accountXA2, accountXB, accountYA});
    // Clients totals are:
    // ClientX -> (ProviderA-R1: 10) and (ProviderB-R1: 5)
    // ClientY -> (ProviderA-R1: 5)

    auto txn = dbPool.masterWriteableTransaction();
    auto quotasManager = QuotasManager(txn);
    // Dispense ClientX->ProviderA quota > available
    quotasManager.dispenseQuota(accountXA1, {{"R1", 10}});
    quotasManager.dispenseQuota(accountXA2, {{"R1", 5}});
    // Dispense ClientX -> ProviderB quota < available
    quotasManager.dispenseQuota(accountXB, {{"R1", 1}});
    // Dispense ClientY -> ProviderA quota == available
    quotasManager.dispenseQuota(accountYA, {{"R1", 5}});

    // Validation fails for ClientX -> ProviderA
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        quotasManager.validateDispencedTotals("ClientX", "ProviderA"),
        InvalidQuotaError, "Not allowed to exceed total quota for R1"
    );
    // Ok for ClientX -> ProviderB
    quotasManager.validateDispencedTotals("ClientX", "ProviderB");
    // Ok for ClientY -> ProviderA
    quotasManager.validateDispencedTotals("ClientY", "ProviderA");

    // Un-dispense excess quota
    quotasManager.dispenseQuota(accountXA1, {{"R1", 5}});
    // Now validation success
    QuotasManager(txn).validateDispencedTotals("ClientX", "ProviderA");
    txn->commit();

    // Check result accounts quotas
    txn = dbPool.slaveTransaction();
    EXPECT_EQ(
        QuotasManager(txn).loadAccountsQuotasByIds({accountXA1.id}),
        QuotaBundlesByAccount({
            {"AccountXA1", {.providerId = "ProviderA", .dispensed = {{"R1", 5}}, .allocated = {{"R1", 0}}}}
        })
    );
    EXPECT_EQ(
        QuotasManager(txn).loadAccountsQuotasByIds({accountXA2.id}),
        QuotaBundlesByAccount({
            {"AccountXA2", {.providerId = "ProviderA", .dispensed = {{"R1", 5}}, .allocated = {{"R1", 0}}}}
        })
    );
    EXPECT_EQ(
        QuotasManager(txn).loadAccountsQuotasByIds({accountXB.id}),
        QuotaBundlesByAccount({
            {"AccountXB", {.providerId = "ProviderB", .dispensed = {{"R1", 1}}, .allocated = {{"R1", 0}}}}
        })
    );
    EXPECT_EQ(
        QuotasManager(txn).loadAccountsQuotasByIds({accountYA.id}),
        QuotaBundlesByAccount({
            {"AccountYA", {.providerId = "ProviderA", .dispensed = {{"R1", 5}}, .allocated = {{"R1", 0}}}}
        })
    );
}

Y_UNIT_TEST(account_quotas_bundle)
{
    DatabaseFixture fixture;
    fixture.insert<ProvidersTable>(presetProviders())
        .insert<ResourcesTable>(presetResources())
        .insert<ClientsTable>(presetClients())
        .insert<AccountsTable>({
            {.id = "AccountX1", .clientId = "ClientX", .providerId="ProviderA", .slug="account-x1", .name = "ClientX account"},
            {.id = "AccountX2", .clientId = "ClientX", .providerId="ProviderB", .slug="account-x2", .name = "ClientX 2nd account"},
            {.id = "AccountY1", .clientId = "ClientY", .providerId="ProviderA", .slug="account-y1", .name = "ClientY account"},
        })
        .insert<AccountQuotasTable>({
            {.accountId = "AccountX1", .providerId = "ProviderA", .resourceId = "R2", .quota = 2, .allocated = 1},
            {.accountId = "AccountX2", .providerId = "ProviderB", .resourceId = "R1", .quota = 100, .allocated = 50},
            {.accountId = "AccountX1", .providerId = "ProviderA", .resourceId = "R1", .quota = 222, .allocated = 111},
            {.accountId = "AccountY1", .providerId = "ProviderA", .resourceId = "R1", .quota = 1, .allocated = 1},
        });

    auto txn = fixture.pgPool().slaveTransaction();
    auto quotasManager = QuotasManager{txn};
    EXPECT_EQ(
        quotasManager.lookupAccountsQuotaBundle({.clientId = "NoSuchClient"}),
        QuotaBundlesByAccount{}
    );
    EXPECT_EQ(
        quotasManager.lookupAccountsQuotaBundle({.clientId = "ClientX"}),
        (QuotaBundlesByAccount{
            {"AccountX1", AccountQuotasBundle {
                .providerId = "ProviderA",
                .dispensed = {{"R1", 222}, {"R2", 2}},
                .allocated = {{"R1", 111}, {"R2", 1}}
            }},
            {"AccountX2", AccountQuotasBundle {
                .providerId = "ProviderB",
                .dispensed = {{"R1", 100}},
                .allocated = {{"R1", 50}}
            }}
        })
    );
    EXPECT_EQ(
        quotasManager.lookupAccountsQuotaBundle({.clientId = "ClientY"}),
        (QuotaBundlesByAccount{{ "AccountY1", AccountQuotasBundle {
            .providerId = "ProviderA",
            .dispensed = {{"R1", 1}},
            .allocated = {{"R1", 1}}
        }}})
    );
}

}  // Y_UNIT_TEST_SUITE
} // namespace maps::quotateka::tests
