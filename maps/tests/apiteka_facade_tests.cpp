#include <maps/infra/apiteka/datamodel/include/tables/schema.h>
#include <maps/infra/apiteka/datamodel/tests/fixture.h>
#include <maps/infra/apiteka/datamodel/tests/matchers.h>
#include <maps/infra/apiteka/server/include/apiteka.h>
#include <maps/infra/apiteka/server/tests/samples.h>

#include <maps/libs/sql_chemistry/include/gateway.h>

#include <library/cpp/testing/gtest_protobuf/matcher.h>
#include <gmock/gmock.h>
#include <gtest/gtest.h>

namespace maps::apiteka::tests {
using sql_chemistry::Gateway;
using NGTest::EqualsProto;

class ApitekaFacadeFixture : public testing::Test, public DatabaseFixture {
protected:
    void SetUp() override
    {
        auto transaction{pgPool().masterWriteableTransaction()};
        auto provider{samples::PROVIDER};
        ASSERT_NO_THROW(
            Gateway<tables::Provider>{*transaction}.insert(provider));
        ASSERT_NO_THROW(Gateway<tables::ProviderTvm>{*transaction}.insert(
            ProviderTvm{samples::PROVIDER_TVM}));
        transaction->commit();
    }
};

TEST_F(ApitekaFacadeFixture, FirstTimeSnapshotApplication)
{
    EXPECT_FALSE(
        Gateway<tables::LatestSnapshot>{*pgPool().masterReadOnlyTransaction()}
            .tryLoadOne());

    Apiteka apiteka{pgPool()};
    ASSERT_NO_THROW(apiteka.applySnapshot(samples::DEFAULT_SNAPSHOT));

    auto transaction{pgPool().masterReadOnlyTransaction()};

    EXPECT_THAT(
        Gateway<tables::ApiKey>{*transaction}.load(),
        testing::UnorderedElementsAre(
            matchers::equalsTo(samples::APIKEYS_KEY),
            matchers::equalsTo(samples::APIKEYS_INACTIVE_KEY)));
    EXPECT_THAT(
        Gateway<tables::Plan>{*transaction}.load(),
        testing::UnorderedElementsAre(testing::AllOf(
            matchers::equalsTo(samples::APIKEYS_PLAN),
            matchers::providerIdOfPlanIs(samples::PROVIDER.id))));

    EXPECT_THAT(
        Gateway<tables::Assignment>{*transaction}.load(),
        testing::UnorderedElementsAre(matchers::equalsTo(Assignment{
            .providerId = samples::PROVIDER.id,
            .planId = samples::APIKEYS_PLAN.id,
            .apiKey = samples::APIKEYS_KEY.id})));

    const auto snapshotRecord{
        Gateway<tables::LatestSnapshot>{*transaction}.tryLoadOne()};
    ASSERT_TRUE(snapshotRecord);
    EXPECT_EQ(snapshotRecord->origin, Origin::Apikeys);

    proto::Snapshot snapshot;
    ASSERT_TRUE(snapshot.ParseFromString(TString{snapshotRecord->snapshot}));
    ASSERT_THAT(snapshot, EqualsProto(samples::DEFAULT_SNAPSHOT));
}

TEST_F(ApitekaFacadeFixture, EmptyInventoryForExistingProviderWithoutKeys)
{
    ASSERT_TRUE(Apiteka{pgPool()}
                    .getInventory(samples::PROVIDER_TVM.tvm)
                    .keys_by_plan()
                    .empty());
}

class PreconfiguredApitekaFacadeFixture : public ApitekaFacadeFixture {
protected:
    void SetUp() override {
        ApitekaFacadeFixture::SetUp();
        ASSERT_NO_THROW(apiteka_.applySnapshot(samples::DEFAULT_SNAPSHOT));
    }
protected:
    Apiteka apiteka_{pgPool()};
};

TEST_F(PreconfiguredApitekaFacadeFixture, IncrementalSnapshotUpdate)
{
    const auto initialSnapshotRecord{
        Gateway<tables::LatestSnapshot>{*pgPool().masterReadOnlyTransaction()}
            .tryLoadOne(tables::LatestSnapshot::origin == Origin::Apikeys)};
    ASSERT_TRUE(initialSnapshotRecord);
    ASSERT_NO_THROW(apiteka_.applySnapshot(samples::EMPTY_SNAPSHOT));

    auto transaction{pgPool().masterReadOnlyTransaction()};
    EXPECT_TRUE(Gateway<tables::ApiKey>{*transaction}.load().empty());
    EXPECT_TRUE(Gateway<tables::Plan>{*transaction}.load().empty());
    EXPECT_TRUE(Gateway<tables::Assignment>{*transaction}.load().empty());

    const auto updatedSnapshotRecord{
        Gateway<tables::LatestSnapshot>{*transaction}.tryLoadOne(
            tables::LatestSnapshot::origin == Origin::Apikeys)};
    ASSERT_TRUE(updatedSnapshotRecord);
    ASSERT_GT(updatedSnapshotRecord->syncTime(), initialSnapshotRecord->syncTime());

    proto::Snapshot updatedSnapshot;
    ASSERT_TRUE(updatedSnapshot.ParseFromString(
        TString{updatedSnapshotRecord->snapshot}));
    ASSERT_THAT(updatedSnapshot, EqualsProto(samples::EMPTY_SNAPSHOT));
}

TEST_F(PreconfiguredApitekaFacadeFixture, ListProviders)
{
    EXPECT_THAT(
        apiteka_.listProviders().providers(),
        testing::ElementsAre(EqualsProto(convert(samples::PROVIDER))));
    ASSERT_NO_THROW(apiteka_.applySnapshot(samples::EMPTY_SNAPSHOT));
    ASSERT_NO_THROW(apiteka_.deleteProvider(samples::PROVIDER.id));
    EXPECT_THAT(apiteka_.listProviders().providers(), testing::IsEmpty());
}

TEST_F(PreconfiguredApitekaFacadeFixture, DeleteProvider)
{
    EXPECT_THROW(apiteka_.deleteProvider(samples::PROVIDER.id), PreconditionFailed);
    ASSERT_NO_THROW(apiteka_.applySnapshot(samples::EMPTY_SNAPSHOT));
    EXPECT_NO_THROW(apiteka_.deleteProvider(samples::PROVIDER.id));
}

TEST_F(PreconfiguredApitekaFacadeFixture, AddProvider)
{
    const auto& anotherProvider{samples::ANOTHER_PROVIDER};
    ASSERT_NO_THROW(apiteka_.addProvider(anotherProvider));
    EXPECT_THAT(
        apiteka_.listProviders().providers(),
        testing::Contains(testing::AllOf(
            testing::Property(
                &proto::ProviderList::ProviderSummary::id, anotherProvider.id()),
            testing::Property(
                &proto::ProviderList::ProviderSummary::abc_slug,
                anotherProvider.abc_slug()))));
}

TEST_F(PreconfiguredApitekaFacadeFixture, CantAddExistingProvider)
{
    ASSERT_THROW(
        apiteka_.addProvider(samples::DEFAULT_PROVIDER), PreconditionFailed);
}

TEST_F(PreconfiguredApitekaFacadeFixture, CantAddProviderWithExistingTvm)
{
    auto anotherProvider{samples::ANOTHER_PROVIDER};
    anotherProvider.add_tvms(samples::PROVIDER_TVM.tvm);

    ASSERT_THROW(apiteka_.addProvider(anotherProvider), PreconditionFailed);
}

TEST_F(PreconfiguredApitekaFacadeFixture, ExceptionForUnknownTvmOnInventoryRequest)
{
    ASSERT_THROW(
        apiteka_.getInventory(samples::ANOTHER_PROVIDER_TVM_ID),
        PreconditionFailed);
}

TEST_F(PreconfiguredApitekaFacadeFixture, GetNonemptyInventoryByValidTvmId)
{
    using PlanSpec = proto::ProviderInventory::PlanSpec;
    using namespace testing;

    ASSERT_NO_THROW(apiteka_.applySnapshot(samples::DEFAULT_SNAPSHOT));
    ASSERT_THAT(
        apiteka_.getInventory(samples::PROVIDER_TVM.tvm).keys_by_plan(),
        ElementsAre(testing::AllOf(
            Property(
                "plan",
                &PlanSpec::plan,
                EqualsProto(convert(samples::APIKEYS_PLAN))),
            ResultOf(
                [](const PlanSpec& v) { return v.keys(); },
                ElementsAre(EqualsProto(convert(samples::APIKEYS_KEY)))))));
}
} // namespace maps::apiteka::tests
