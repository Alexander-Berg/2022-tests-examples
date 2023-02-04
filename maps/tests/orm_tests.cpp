#include <maps/infra/apiteka/datamodel/tests/fixture.h>
#include <maps/infra/apiteka/datamodel/tests/matchers.h>
#include <maps/infra/apiteka/datamodel/tests/samples.h>

#include <maps/infra/apiteka/datamodel/include/tables/schema.h>

#include <gmock/gmock-matchers.h>
#include <gtest/gtest.h>

namespace maps::apiteka::tests {
using maps::sql_chemistry::Gateway;

template<typename T>
inline T makeCopy(T value)
{
    return value;
}

class OrmTestFixture : public testing::Test, public DatabaseFixture {
public:
    template<typename T>
    class AdHocTransaction {
    public:
        AdHocTransaction(pgpool3::TransactionHandle handle)
            : handle_{std::move(handle)}
            , gateway_{*handle_}
        {}

        ~AdHocTransaction()
        try {
            if (!std::uncaught_exceptions())
                handle_->commit();
        } catch (const std::exception& ex) {
            ADD_FAILURE();
            std::cerr << "Exception was throws while committing test transation: "
                      << ex.what();
        }

        const Gateway<T>* operator->() const noexcept {
            return &gateway_;
        }

        Gateway<T>* operator->() noexcept {
            return &gateway_;
        }
    private:
        pgpool3::TransactionHandle handle_;
        Gateway<T> gateway_;
    };

    template<typename T>
    auto adHocWritableTxn()
    {
        return AdHocTransaction<T>{pgPool().masterWriteableTransaction()};
    }

    template<typename T>
    auto adHocReadonlyTxn()
    {
        return AdHocTransaction<T>{pgPool().masterReadOnlyTransaction()};
    }
};

TEST_F(OrmTestFixture, ReadWriteApikeys)
{
    auto gateway{adHocWritableTxn<tables::ApiKey>()};
    std::array keys{samples::APIKEYS_KEY, samples::KEYSERV_KEY};

    ASSERT_NO_THROW(gateway->insert(keys));
    ASSERT_THAT(
        gateway->load(),
        testing::UnorderedElementsAre(
            matchers::equalsTo(samples::APIKEYS_KEY),
            matchers::equalsTo(samples::KEYSERV_KEY)));
}

TEST_F(OrmTestFixture, ReadWriteProviders)
{
    auto transaction{pgPool().masterWriteableTransaction()};
    {
        Gateway<tables::Provider> gateway{*transaction};

        ASSERT_NO_THROW(gateway.insert(makeCopy(samples::PROVIDER)));
        ASSERT_THAT(
            gateway.load(),
            testing::ElementsAre(matchers::equalsTo(samples::PROVIDER)));
    }
    {
        Gateway<tables::ProviderTvm> gateway{*transaction};

        ASSERT_NO_THROW(gateway.insert(makeCopy(samples::PROVIDER_TVM)));
        ASSERT_THAT(
            gateway.load(),
            testing::ElementsAre(matchers::equalsTo(samples::PROVIDER_TVM)));
    }
}

TEST_F(OrmTestFixture, ReadWritePlans)
{
    auto transaction{pgPool().masterWriteableTransaction()};
    ASSERT_NO_THROW(Gateway<tables::Provider>{*transaction}.insert(
        makeCopy(samples::PROVIDER)));

    Gateway<tables::Plan> gateway{*transaction};
    ASSERT_NO_THROW(gateway.insert(makeCopy(samples::APIKEYS_PLAN)));
    ASSERT_THAT(
        gateway.load(),
        testing::ElementsAre(matchers::equalsTo(samples::APIKEYS_PLAN)));
}

TEST_F(OrmTestFixture, ReadWriteAssignments)
{
    auto transaction{pgPool().masterWriteableTransaction()};
    ASSERT_NO_THROW(Gateway<tables::Provider>{*transaction}.insert(
        makeCopy(samples::PROVIDER)));
    ASSERT_NO_THROW(Gateway<tables::Plan>{*transaction}.insert(
        makeCopy(samples::APIKEYS_PLAN)));
    ASSERT_NO_THROW(Gateway<tables::ApiKey>{*transaction}.insert(
        makeCopy(samples::APIKEYS_KEY)));

    Gateway<tables::Assignment> gateway{*transaction};
    ASSERT_NO_THROW(gateway.insert(makeCopy(samples::APIKEYS_ASSIGNMENT)));
    ASSERT_THAT(
        gateway.load(),
        testing::ElementsAre(matchers::equalsTo(samples::APIKEYS_ASSIGNMENT)));
}

TEST_F(OrmTestFixture, ReadWriteLatestSnapshot)
{
    auto snapshot{samples::APIKEYS_FAKE_SNAPSHOT};
    auto gateway{adHocWritableTxn<tables::LatestSnapshot>()};
    ASSERT_NO_THROW(gateway->insert(snapshot));
    ASSERT_THAT(
        gateway->load(),
        testing::ElementsAre(AllOf(
            matchers::originOfLatestSnapshotIs(snapshot.origin),
            matchers::snapshotOfLatestSnapshotIs(snapshot.snapshot),
            matchers::syncTimeOfLatestSnapshotIs(
                testing::Gt(chrono::TimePoint{})))));
}

TEST_F(OrmTestFixture, ConcurrentReadWriteLatestSnapshot)
{
    ASSERT_NO_THROW(adHocWritableTxn<tables::LatestSnapshot>()->insert(
        makeCopy(samples::APIKEYS_FAKE_SNAPSHOT)));

    auto loadedSnapshot{adHocReadonlyTxn<tables::LatestSnapshot>()->loadOne()};
    loadedSnapshot.snapshot += "0";
    {
        auto gateway{adHocWritableTxn<tables::LatestSnapshot>()};
        auto interleavingSnapshot{gateway->loadOne()};
        interleavingSnapshot.snapshot += "1";
        ASSERT_NO_THROW(gateway->update(interleavingSnapshot));
    }

    ASSERT_THROW(
        adHocWritableTxn<tables::LatestSnapshot>()->update(loadedSnapshot),
        sql_chemistry::EditConflict);
    ASSERT_THAT(
        adHocReadonlyTxn<tables::LatestSnapshot>()->loadOne(),
        matchers::snapshotOfLatestSnapshotIs(testing::EndsWith("1")));
}

} // namespace maps::apiteka::tests
