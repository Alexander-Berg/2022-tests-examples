#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/ugc/backoffice/src/lib/contributions/modify.h>
#include <maps/wikimap/ugc/backoffice/src/tests/helpers/test_request_validator.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::backoffice::tests {

namespace {

auto& dbPool()
{
    static ugc::tests::DbFixture db;
    return db.pool();
}

const proto::backoffice::ModifyContributions modifyContributions()
{
    proto::backoffice::ModifyContributions modifyContributions;
    {
        auto* modifyContribution = modifyContributions.add_modify_contribution();
        modifyContribution->mutable_delete_()->set_id("1");
    }
    {
        auto* modifyContribution = modifyContributions.add_modify_contribution();
        auto* upsert = modifyContribution->mutable_upsert();
        upsert->set_id("2");
        auto* item = upsert->mutable_contribution();
        item->set_timestamp(1000000);
        auto& langToMetadata = *item->mutable_lang_to_metadata();

        proto::contributions::feedback::BaseMapContribution::Status::Pending pending;

        {
            proto::contribution::ContributionMetadata metadata;
            auto* baseMap = metadata.mutable_base_map();
            baseMap->set_id("internal_base_map_id");
            baseMap->set_title("Заголовок");
            baseMap->set_description("Описание");
            *baseMap->mutable_status()->mutable_pending() = pending;
            baseMap->set_type(proto::contributions::feedback::BaseMapContribution::ADDRESS);
            baseMap->set_user_message("Комментарий пользователя");

            auto* point = baseMap->mutable_center_point();
            point->set_lon(27.525566);
            point->set_lat(53.890712);

            langToMetadata["ru_RU"] = metadata;
        }
        {
            proto::contribution::ContributionMetadata metadata;
            auto* baseMap = metadata.mutable_base_map();
            baseMap->set_id("internal_base_map_id");
            baseMap->set_title("Title");
            baseMap->set_description("Description");
            *baseMap->mutable_status()->mutable_pending() = pending;
            baseMap->set_type(proto::contributions::feedback::BaseMapContribution::ADDRESS);
            baseMap->set_user_message("Комментарий пользователя");

            auto* point = baseMap->mutable_center_point();
            point->set_lon(27.525566);
            point->set_lat(53.890712);

            langToMetadata["en_US"] = metadata;
        }
    }
    return modifyContributions;
}

const Uid UID{111111};
const maps::auth::TvmId FAKETVM = 1;

} // namespace

Y_UNIT_TEST_SUITE(test_modify_contribution)
{

Y_UNIT_TEST(update_db_insert_and_no_delete)
{
    auto txn = dbPool().masterWriteableTransaction();
    updateDb(*txn, modifyContributions(), UID, FAKETVM, TestRequestValidator());

    const auto rows = maps::wiki::query_builder::SelectQuery(tables::CONTRIBUTION).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);

    const auto& row = rows[0];
    UNIT_ASSERT_VALUES_EQUAL(
        row[columns::METADATA_ID].as<MetadataId::ValueType>(),
        static_cast<unsigned>(proto::contribution::ContributionMetadata::ContributionCase::kBaseMap)
    );
    UNIT_ASSERT_VALUES_EQUAL(row[columns::CONTRIBUTION_ID].as<std::string>(), "2");
    UNIT_ASSERT_VALUES_EQUAL(row[columns::UID].as<Uid::ValueType>(), UID.value());

    auto dataRows = maps::wiki::query_builder::SelectQuery(tables::CONTRIBUTION_DATA).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(dataRows.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(
            tables::CONTRIBUTION_DATA,
            maps::wiki::query_builder::WhereConditions()
                .append(columns::UID, std::to_string(UID.value()))
                .appendQuoted(columns::CONTRIBUTION_ID, "2")
        ).exec(*txn).size(),
        2
    );
    proto::contribution::ContributionMetadata restored;
    pqxx::binarystring blob(dataRows[0][columns::DATA]);
    UNIT_ASSERT(restored.ParseFromString(TString{blob.str()}));
    UNIT_ASSERT_VALUES_EQUAL(restored.base_map().id(), "internal_base_map_id");
}

Y_UNIT_TEST(update_db_delete)
{
    auto txn = dbPool().masterWriteableTransaction();
    proto::backoffice::ModifyContributions deleteContributions;
    {
        auto* deleteContribution = deleteContributions.add_modify_contribution();
        deleteContribution->mutable_delete_()->set_id("1");
    }

    makeInsertContributionQuery(
        ContributionId{"1"}, // will be deleted
        UID,
        MetadataId{3},
        std::nullopt
    ).exec(*txn);
    // db state:
    // uid = 11111, contribution_id = 1

    makeInsertContributionQuery(
        ContributionId{"3"}, // do not delete
        UID,
        MetadataId{3},
        std::nullopt
    ).exec(*txn);
    // db state:
    // uid = 11111, contribution_id = 1
    // uid = 11111, contribution_id = 3

    makeInsertContributionDataQuery(
        ContributionId{"1"}, // will be deleted
        UID,
        Lang{"ru_RU"},
        modifyContributions().modify_contribution(1).upsert().contribution().lang_to_metadata().at("ru_RU")
    ).exec(*txn);
    // db state:
    // uid = 11111, contribution_id = 1, metadata was added to this object
    // uid = 11111, contribution_id = 3

    makeInsertContributionDataQuery(
        ContributionId{"3"}, // do not delete
        UID,
        Lang{"ru_RU"},
        modifyContributions().modify_contribution(1).upsert().contribution().lang_to_metadata().at("ru_RU")
    ).exec(*txn);
    // db state:
    // uid = 11111, contribution_id = 1,
    // uid = 11111, contribution_id = 3, metadata was added to this object

    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(tables::CONTRIBUTION).exec(*txn).size(),
        2
    );
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(tables::CONTRIBUTION_DATA).exec(*txn).size(),
        2
    );

    updateDb(*txn, modifyContributions(), UID, FAKETVM, TestRequestValidator());
    // db state:
    // uid = 11111, contribution_id = 2, has two metadatas
    // uid = 11111, contribution_id = 3, has one metadata

    const Uid otherUid{222222};
    updateDb(*txn, modifyContributions(), otherUid, FAKETVM, TestRequestValidator());
    // db state:
    // uid = 11111, contribution_id = 2, has two metadatas
    // uid = 11111, contribution_id = 3, has one metadata
    // uid = 22222, contribution_id = 2, has two metadatas

    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(tables::CONTRIBUTION).exec(*txn).size(),
        3
    );
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(tables::CONTRIBUTION_DATA).exec(*txn).size(),
        5
    );

    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(
            tables::CONTRIBUTION,
            maps::wiki::query_builder::WhereConditions()
                .append(columns::UID, std::to_string(UID.value()))
        ).exec(*txn).size(),
        2
    );
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(
            tables::CONTRIBUTION,
            maps::wiki::query_builder::WhereConditions()
                .append(columns::UID, std::to_string(UID.value()))
                .appendQuoted(columns::CONTRIBUTION_ID, "1") // was deleted
        ).exec(*txn).size(),
        0
    );
}

} // test_modify_contribution suite

} // namespace maps::wiki::ugc::backoffice::tests
