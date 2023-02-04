#include <maps/infra/quotateka/datamodel/include/client_orm.h>
#include <maps/infra/quotateka/datamodel/include/account_manager.h>
#include <maps/infra/quotateka/datamodel/tests/fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

using namespace datamodel;

namespace {

template<typename Record>
Record resetTimestamps(
    Record record, const chrono::TimePoint& timestamp = chrono::TimePoint())
{
    record.created = timestamp;
    record.updated = timestamp;
    return record;
}

template<typename Record>
std::vector<Record> resetAllTimestamps(
    std::vector<Record> records,
    const chrono::TimePoint& timestamp = chrono::TimePoint())
{
    for (auto& record: records) {
        record = resetTimestamps(record, timestamp);
    }
    return records;
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(account_manager)
{
Y_UNIT_TEST(create_and_read)
{
    DatabaseFixture fixture;
    fixture.insert<ClientsTable>(
        {ClientRecord{.id = "ClientX", .abcSlug = "client-x", .abcId = 1}})
    .insert<ProvidersTable>({
        ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
    });

    { // Create account with invalid client
        auto txn = fixture.pgPool().masterWriteableTransaction();
        EXPECT_THROW(
            AccountManager{txn}.createAccount({
                .clientId = "NoSuchClient",
                .providerId = "ProviderA",
                .slug = "my-shiny-account",
                .name = "MyShinyAccount",
                .description = "Some description",
                .folderId = "folder-id"}),
            pqxx::foreign_key_violation);
    }
    { // Create account with invalid provider
        auto txn = fixture.pgPool().masterWriteableTransaction();
        EXPECT_THROW(
            AccountManager{txn}.createAccount({
                .clientId = "ClientX",
                .providerId = "NoSuchProvider",
                .slug = "my-shiny-account",
                .name = "MyShinyAccount",
                .description = "Some description",
                .folderId = "folder-id"}),
            pqxx::foreign_key_violation);
    }
    { // Create account with invalid account slug
        auto txn = fixture.pgPool().masterWriteableTransaction();
        EXPECT_THROW(
            AccountManager{txn}.createAccount({
                .clientId = "NoSuchClient",
                .providerId = "ProviderA",
                .slug = "!invalid-slug!",
                .name = "MyShinyAccount",
                .description = "Some description",
                .folderId = "folder-id"}),
            InvalidAccountSlugError
        );
    }
    AccountRecord expectedAccount;
    { // Successful create account
        auto txn = fixture.pgPool().masterWriteableTransaction();
        auto record = AccountManager{txn}.createAccount({
            .clientId = "ClientX",
            .providerId = "ProviderA",
            .slug = "my-shiny-account",
            .name = "MyShinyAccount",
            .description = "Some description",
            .folderId = "folder-id"});
        txn->commit();
        EXPECT_EQ(record.clientId, "ClientX");
        EXPECT_EQ(record.providerId, "ProviderA");
        EXPECT_EQ(record.slug, "my-shiny-account");
        EXPECT_EQ(record.name, "MyShinyAccount");
        EXPECT_EQ(record.description, "Some description");
        EXPECT_EQ(record.folderId, "folder-id");
        // TODO: GEOINFRA-2420 and timestamps tests
        expectedAccount = resetTimestamps(record);
    }

    { // Load account
        auto txn = fixture.pgPool().slaveTransaction();
        auto record = AccountManager{txn}.loadAccount(expectedAccount.id);
        EXPECT_EQ(expectedAccount, resetTimestamps(record));
    }
    { // Load invalid account
        auto txn = fixture.pgPool().slaveTransaction();
        EXPECT_THROW(
            AccountManager{txn}.loadAccount("NoSuchAccount"),
            AccountNotFoundError
        );
    }

    { // Client ABC by account
        auto txn = fixture.pgPool().slaveTransaction();
        auto clientAbc =
            AccountManager{txn}.abcSlugByAccount(expectedAccount.id);
        EXPECT_EQ(clientAbc, "client-x");

        EXPECT_THROW(
            AccountManager{txn}.abcSlugByAccount("NoSuchAccount"),
            AccountNotFoundError
        );
    }
}

Y_UNIT_TEST(lookup)
{
    DatabaseFixture fixture;
    fixture
        .insert<ProvidersTable>({
            ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
            ProviderRecord{.id = "ProviderB", .abcSlug = "provider-b"},
        })
        .insert<ResourcesTable>({
            {.id = "R1", .providerId = "ProviderA", .type = ResourceType::PerSecondLimit},
            {.id = "R2", .providerId = "ProviderA", .type = ResourceType::PerHourLimit},
            {.id = "R1", .providerId = "ProviderB", .type = ResourceType::PerDayLimit},
        })
        .insert<ClientsTable>({
            ClientRecord{.id = "ClientX", .abcSlug = "client-x", .abcId = 1},
            ClientRecord{.id = "ClientY", .abcSlug = "client-y", .abcId = 2},
        })
        .insert<AccountsTable>({
            {.id = "AccountX1", .clientId = "ClientX", .providerId = "ProviderA", .slug = "account-x1", .name = "ClientX account"},
            {.id = "AccountY1", .clientId = "ClientY", .providerId = "ProviderB", .slug = "account-y1", .name = "ClientY account"},
        })
        .insert<AccountQuotasTable>({
            {.accountId = "AccountX1", .providerId = "ProviderA", .resourceId = "R1", .quota = 10, .allocated = 5},
            {.accountId = "AccountX1", .providerId = "ProviderA", .resourceId = "R2", .quota = 2, .allocated = 1},
            {.accountId = "AccountY1", .providerId = "ProviderB", .resourceId = "R1", .quota = 100, .allocated = 75},
        });

    auto txn = fixture.pgPool().slaveTransaction();
    auto accountManager = AccountManager{txn};
    EXPECT_EQ(
        resetAllTimestamps(
            accountManager.lookupAccounts({.clientId = "ClientX", .accountSlug = "account-x1"})
        ),
        (AccountRecords{
            {.id = "AccountX1",
             .clientId = "ClientX",
             .providerId = "ProviderA",
             .slug = "account-x1",
             .name = "ClientX account"}
        })
    );
    EXPECT_EQ(
        resetAllTimestamps(accountManager.lookupAccounts({ .providerId = "ProviderA" })),
        (AccountRecords{
            {.id = "AccountX1",
             .clientId = "ClientX",
             .providerId = "ProviderA",
             .slug = "account-x1",
             .name = "ClientX account"},
        })
    );
    EXPECT_EQ(
        resetAllTimestamps(accountManager.lookupAccounts({ .providerId = "ProviderB" })),
        (AccountRecords{
            {.id = "AccountY1",
             .clientId = "ClientY",
             .providerId = "ProviderB",
             .slug = "account-y1",
             .name = "ClientY account"},
        }));
}

Y_UNIT_TEST(tvm_assign_revoke)
{
    DatabaseFixture fixture;
    fixture.insert<ClientsTable>(ClientRecords{
        {.id = "sedem-machine", .abcSlug = "maps-core-sedem-machine", .abcId = 1}
    })
    .insert<ProvidersTable>({
        ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
        ProviderRecord{.id = "ProviderB", .abcSlug = "provider-b"},
    });

    AccountRecord accA, accAA, accB;
    {  // Create accounts
        auto txn = fixture.pgPool().masterWriteableTransaction();
        accA = AccountManager{txn}.createAccount(
            {.clientId = "sedem-machine", .providerId = "ProviderA", .slug = "acc-a"}
        );
        accAA = AccountManager{txn}.createAccount(
            {.clientId = "sedem-machine", .providerId = "ProviderA", .slug = "acc-aa"}
        );
        accB = AccountManager{txn}.createAccount(
            {.clientId = "sedem-machine", .providerId = "ProviderB", .slug = "acc-b"}
        );
        txn->commit();
    }
    {  // Assign tvm
        auto txn = fixture.pgPool().masterWriteableTransaction();
        AccountManager{txn}.assignTvm(accA, {11111}, "a-one");
        AccountManager{txn}.assignTvm(accA, {22222}, "a-two");
        // Allowed to assign tvm to multiple accounts if providers are different
        AccountManager{txn}.assignTvm(accB, {11111}, "b-one");
        txn->commit();
    }
    {  // Failed assign - already assigned to another account of same provider
        auto txn = fixture.pgPool().masterWriteableTransaction();
        EXPECT_THROW(
            AccountManager{txn}.assignTvm(accAA, {11111}, "a-one"),
            sql_chemistry::UniqueViolationError
        );
    }
    {  // Failed assign - invalid account
        auto txn = fixture.pgPool().masterWriteableTransaction();
        EXPECT_THROW(
            AccountManager{txn}.assignTvm(
                {.id="No-Such-account", .providerId="ProviderA"}, {33333}, "whatever"),
            pqxx::foreign_key_violation
        );
    }
    {  // Check accounts
        auto txn = fixture.pgPool().slaveTransaction();
        auto accountManager = AccountManager{txn};
        auto bundle = accountManager.loadAccountBundle(accA);
        AccountBundle expectedBundle{
            .tvmRecords = {
                {.tvmId = 11111, .accountId = accA.id, .providerId = "ProviderA", .name = "a-one"},
                {.tvmId = 22222, .accountId = accA.id, .providerId = "ProviderA", .name = "a-two"},
            },
            .provider = ProviderBundle{.provider = {.id = "ProviderA", .abcSlug = "provider-a"}}
        };
        EXPECT_EQ(expectedBundle, bundle);

        bundle = accountManager.loadAccountBundle(accAA);
        expectedBundle = AccountBundle{
            .tvmRecords = {},
            .provider = ProviderBundle{.provider = {.id = "ProviderA", .abcSlug = "provider-a"}}
        };
        EXPECT_EQ(expectedBundle, bundle);

        bundle = accountManager.loadAccountBundle(accB);
        expectedBundle = AccountBundle{
            .tvmRecords = {
                {.tvmId = 11111, .accountId = accB.id, .providerId = "ProviderB", .name = "b-one"}
            },
            .provider = ProviderBundle{.provider = {.id = "ProviderB", .abcSlug = "provider-b"}}
        };
        EXPECT_EQ(expectedBundle, bundle);
    }

    { // Revoke fail
        auto txn = fixture.pgPool().masterWriteableTransaction();
        EXPECT_THROW(
            AccountManager{txn}.revokeTvm(accB.id, 22222),
            TvmNotFoundError
        );
    }
    { // Revoke all from accA
        auto txn = fixture.pgPool().masterWriteableTransaction();
        AccountManager{txn}.revokeTvm(accA.id, 11111);
        AccountManager{txn}.revokeTvm(accA.id, 22222);
        txn->commit();
    }
    { // Check accA has no tvm, but accB tvm still there
        auto txn = fixture.pgPool().slaveTransaction();
        auto accountManager = AccountManager{txn};
        auto bundle = accountManager.loadAccountBundle(accountManager.loadAccount(accA.id));
        AccountBundle expectedBundle{
            .tvmRecords = {},
            .provider = ProviderBundle{.provider = {.id = "ProviderA", .abcSlug = "provider-a"}}
        };
        EXPECT_EQ(expectedBundle, bundle);

        bundle = accountManager.loadAccountBundle(accB);
        expectedBundle = AccountBundle{
            .tvmRecords = {
                {.tvmId = 11111, .accountId = accB.id, .providerId = "ProviderB", .name = "b-one"}
            },
            .provider = ProviderBundle{.provider = {.id = "ProviderB", .abcSlug = "provider-b"}}
        };
        EXPECT_EQ(expectedBundle, bundle);
    }
    { // Now revoke from accB
        auto txn = fixture.pgPool().masterWriteableTransaction();
        AccountManager{txn}.revokeTvm(accB.id, 11111);
        txn->commit();
    }
    { // Check accB has no tvm too
        auto txn = fixture.pgPool().slaveTransaction();
        auto accountManager = AccountManager{txn};
        auto bundle = accountManager.loadAccountBundle(accB);
        AccountBundle expectedBundle{
            .tvmRecords = {},
            .provider = ProviderBundle{.provider = {.id = "ProviderB", .abcSlug = "provider-b"}}
        };
        EXPECT_EQ(expectedBundle, bundle);
    }
}

Y_UNIT_TEST(close)
{
    DatabaseFixture fixture;
    fixture.insert<ClientsTable>({ClientRecord{
        .id = "sedem-machine", .abcSlug = "maps-core-sedem-machine", .abcId = 1}})
    .insert<ProvidersTable>({
        {.id = "ProviderA", .abcSlug = "provider-a"},
        {.id = "release-machine", .abcSlug = "release_machine"}
    });

    AccountRecord account1, account2;
    { // Create couple of accounts
        auto txn = fixture.pgPool().masterWriteableTransaction();
        account1 = AccountManager{txn}.createAccount({
            .clientId = "sedem-machine",
            .providerId = "ProviderA",
            .slug = "main-account",
            .name = "Main Account",
            .folderId = "folder-id"});
        account2 = AccountManager{txn}.createAccount({
            .clientId = "sedem-machine",
            .providerId = "release-machine",
            .slug = "another-account",
            .name = "Another account",
            .folderId = "folder-id"});
        txn->commit();
    }
    { // close one account
        auto txn = fixture.pgPool().masterWriteableTransaction();
        account1 = AccountManager{txn}.closeAccount(account1.id);
        txn->commit();
    }
    { // check if account closed
        auto txn = fixture.pgPool().slaveTransaction();
        auto account1Updated =
            AccountManager{txn}.loadAccount(account1.id);
        auto account2Updated =
            AccountManager{txn}.loadAccount(account2.id);

        ASSERT_EQ(account1Updated.isClosed, true);
        ASSERT_EQ(account2Updated.isClosed, false);
    }


    fixture.insert<ResourcesTable>({ResourceRecord{
        .id = "releases",
        .providerId = "release-machine",
        .type = ResourceType::PerDayLimit}});
    fixture.insert<AccountQuotasTable>({AccountQuotaRecord{
        .accountId = account2.id,
        .providerId = "release-machine",
        .resourceId = "releases",
        .quota = 10,
        .allocated = 10}});
    fixture.insert<ClientQuotasTable>({ClientQuotaRecord{
        .clientId = "sedem-machine",
        .providerId = "release-machine",
        .resourceId = "releases",
        .quota = 100}});

    { // check throw if has provided quota
        auto txn = fixture.pgPool().masterWriteableTransaction();
        EXPECT_THROW(
            AccountManager{txn}.closeAccount(account2.id),
            HasProvidedQuotasError
        );
    }

    { // deallocate quota from  account2
        auto txn = fixture.pgPool().masterWriteableTransaction();
        QuotasManager{txn}.allocateQuota(account2, {{"releases", 0}});
        QuotasManager{txn}.dispenseQuota(account2, {{"releases", 0}});
        txn->commit();
    }
    { // close account2
        auto txn = fixture.pgPool().masterWriteableTransaction();
        AccountManager{txn}.closeAccount(account2.id);
        txn->commit();
    }

    { //  reopen
        auto txn = fixture.pgPool().masterWriteableTransaction();
        auto account1Updated =
            AccountManager{txn}.reopenAccount(account2.id);
        txn->commit();
        ASSERT_EQ(account1Updated.isClosed, false);
    }
}

Y_UNIT_TEST(tvm_rename)
{
    DatabaseFixture fixture;
    fixture.insert<ClientsTable>(ClientRecords{
        {.id = "sedem-machine", .abcSlug = "maps-core-sedem-machine", .abcId = 1}
    })
    .insert<ProvidersTable>({
        ProviderRecord{.id = "ProviderA", .abcSlug = "provider-a"},
    });

    AccountRecord accA;
    {  // Create accounts
        auto txn = fixture.pgPool().masterWriteableTransaction();
        accA = AccountManager{txn}.createAccount(
            {.clientId = "sedem-machine", .providerId = "ProviderA", .slug = "acc-a"}
        );
        txn->commit();
    }
    {  // Assign tvm
        auto txn = fixture.pgPool().masterWriteableTransaction();
        AccountManager{txn}.assignTvm(accA, {11111}, "a-one");
        txn->commit();
    }
    {  // Try to rename tvm
        auto txn = fixture.pgPool().masterWriteableTransaction();
        ASSERT_TRUE(AccountManager{txn}.tryRenameTvm(accA, 11111, "a-one-renamed"));
        txn->commit();
    }
    {  // Try to rename non-existent tvm
        auto txn = fixture.pgPool().masterWriteableTransaction();
        ASSERT_FALSE(AccountManager{txn}.tryRenameTvm(accA, 22222, "no-such-tvm"));
        txn->commit();
    }
    {  // Check accounts
        auto txn = fixture.pgPool().slaveTransaction();
        auto accountManager = AccountManager{txn};
        auto bundle = accountManager.loadAccountBundle(accA);
        AccountBundle expectedBundle{
            .tvmRecords = {
                {.tvmId = 11111, .accountId = accA.id, .providerId = "ProviderA", .name = "a-one-renamed"}
            },
            .provider = ProviderBundle{.provider = {.id = "ProviderA", .abcSlug = "provider-a"}}
        };
        EXPECT_EQ(expectedBundle, bundle);
    }
}
} // Y_UNIT_TEST_SUITE(account_manager)

} // namespace maps::quotateka::tests
