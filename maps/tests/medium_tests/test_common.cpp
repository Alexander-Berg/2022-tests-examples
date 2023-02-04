#include <maps/wikimap/ugc/backoffice/src/lib/common.h>
#include <maps/wikimap/ugc/backoffice/src/lib/contributions/modify.h>
#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::backoffice::tests {

namespace {

auto& dbPool()
{
    static ugc::tests::DbFixture db;
    return db.pool();
}

const Uid UID{111111};

} // namespace

Y_UNIT_TEST_SUITE(test_common)
{

Y_UNIT_TEST(test_new_metadata_id)
{
    proto::backoffice::ContributionItem item;
    auto& langToMetadata = *item.mutable_lang_to_metadata();
    proto::contribution::ContributionMetadata metadata;
    metadata.mutable_route();
    langToMetadata["lang1"] = metadata;
    langToMetadata["lang2"] = metadata;
    UNIT_ASSERT_VALUES_EQUAL(
        getMetadataId(
            item.lang_to_metadata(),
            [] (const proto::contribution::ContributionMetadata& c) { return c.contribution_case(); }
        ).value(),
        static_cast<unsigned>(proto::contribution::ContributionMetadata::ContributionCase::kRoute)
    );
    UNIT_ASSERT_VALUES_EQUAL(
        getMetadataId(
            item.lang_to_metadata(),
            [] (const proto::contribution::ContributionMetadata&) { return 1; }
        ).value(),
        1
    );

    proto::contribution::ContributionMetadata orgMetadata;
    metadata.mutable_organization();
    langToMetadata["lang3"] = orgMetadata;
    UNIT_ASSERT_EXCEPTION(
        getMetadataId(
            item.lang_to_metadata(),
            [] (const proto::contribution::ContributionMetadata& c) { return c.contribution_case(); }
        ),
        maps::RuntimeError
    );
}

Y_UNIT_TEST(test_old_metadata_id)
{
    auto txn = dbPool().masterWriteableTransaction();
    makeInsertContributionQuery(ContributionId{"id1"}, UID, MetadataId{5}, std::nullopt).exec(*txn);

    const auto metadataId = loadMetadataId(
        *txn,
        tables::CONTRIBUTION,
        columns::CONTRIBUTION_ID,
        "id1"
    );
    UNIT_ASSERT(metadataId);
    UNIT_ASSERT_VALUES_EQUAL(metadataId->value(), 5);

    const auto emptyMetadataId = loadMetadataId(
        *txn,
        tables::CONTRIBUTION,
        columns::CONTRIBUTION_ID,
        "unknown_id"
    );
    UNIT_ASSERT(!emptyMetadataId);
}

} // test_common suite

} // namespace maps::wiki::ugc::backoffice::tests
