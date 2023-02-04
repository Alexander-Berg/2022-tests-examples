#include <maps/wikimap/ugc/account/src/lib/contributions.h>
#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/db_fixture.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::account::tests {

namespace qb = maps::wiki::query_builder;

namespace {

const Uid UID_1{111111};
const Uid UID_2{222222};

const ContributionId CONTRIBUTION_ID_1{"11"};
const ContributionId CONTRIBUTION_ID_2{"22"};
const ContributionId CONTRIBUTION_ID_3{"33"};

const MetadataId METADATA_ID_1{1111};
const MetadataId METADATA_ID_2{2222};
const MetadataId METADATA_ID_3{3333};

auto& dbPool()
{
    static ugc::tests::DbFixture db;
    return db.pool();
}

void insertContribution(
    maps::pgpool3::Pool& pool,
    Uid uid,
    ContributionId contributionId,
    MetadataId metadataId)
{
    auto txn = pool.masterWriteableTransaction();

    auto query = qb::InsertQuery(tables::CONTRIBUTION);
    query
        .append(columns::UID, std::to_string(uid.value()))
        .appendQuoted(columns::CONTRIBUTION_ID, contributionId.value())
        .append(columns::METADATA_ID, std::to_string(metadataId.value()));

    query.exec(*txn);

    txn->commit();
}

} // namespace


Y_UNIT_TEST_SUITE(test_contributions_stat)
{

Y_UNIT_TEST(count)
{
    insertContribution(dbPool(), UID_1, CONTRIBUTION_ID_1, METADATA_ID_1);
    insertContribution(dbPool(), UID_2, CONTRIBUTION_ID_2, METADATA_ID_2);
    insertContribution(dbPool(), UID_2, CONTRIBUTION_ID_3, METADATA_ID_3);

    auto txn = dbPool().slaveTransaction();

    UNIT_ASSERT_VALUES_EQUAL(
        1,
        getContributionsStat(*txn, UID_1, {METADATA_ID_1}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        0,
        getContributionsStat(*txn, UID_1, {METADATA_ID_2}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        1,
        getContributionsStat(*txn, UID_1, {METADATA_ID_1, METADATA_ID_2}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        0,
        getContributionsStat(*txn, UID_1, {METADATA_ID_2, METADATA_ID_3}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        1,
        getContributionsStat(*txn, UID_1, {METADATA_ID_1, METADATA_ID_2, METADATA_ID_3}).count());


    UNIT_ASSERT_VALUES_EQUAL(
        0,
        getContributionsStat(*txn, UID_2, {METADATA_ID_1}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        1,
        getContributionsStat(*txn, UID_2, {METADATA_ID_2}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        1,
        getContributionsStat(*txn, UID_1, {METADATA_ID_1, METADATA_ID_2}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        2,
        getContributionsStat(*txn, UID_2, {METADATA_ID_2, METADATA_ID_3}).count());
    UNIT_ASSERT_VALUES_EQUAL(
        2,
        getContributionsStat(*txn, UID_2, {METADATA_ID_1, METADATA_ID_2, METADATA_ID_3}).count());
}

}

} // namespace maps::wiki::ugc::account::tests
