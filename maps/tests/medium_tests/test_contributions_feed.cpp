#include <maps/wikimap/ugc/account/src/lib/contributions.h>
#include <maps/wikimap/ugc/backoffice/src/lib/contributions/modify.h>
#include <maps/wikimap/ugc/backoffice/src/tests/helpers/test_request_validator.h>
#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/db_fixture.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>
#include <maps/libs/locale/include/convert.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::account::tests {

namespace backoffice = maps::wiki::ugc::backoffice;
namespace qb = maps::wiki::query_builder;

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
        {
            auto* modifyContribution = modifyContributions.add_modify_contribution();
            auto* upsert = modifyContribution->mutable_upsert();
            upsert->set_id("2");
            auto* item = upsert->mutable_contribution();
            item->set_timestamp(1609765480); // 2021-01-04T13:04:40+00:00
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
        {
            auto* modifyContribution = modifyContributions.add_modify_contribution();
            auto* upsert = modifyContribution->mutable_upsert();
            upsert->set_id("3");
            auto* item = upsert->mutable_contribution();
            item->set_timestamp(1609765490); // 2021-01-04T13:04:50+00:00
            auto& langToMetadata = *item->mutable_lang_to_metadata();

            proto::contributions::feedback::OrganizationContribution::Status::Pending pending;

            {
                proto::contribution::ContributionMetadata metadata;
                auto* organization = metadata.mutable_organization();
                organization->set_id("internal_org_id");
                organization->set_title("Название организации");
                organization->set_description("Описание организации");
                *organization->mutable_status()->mutable_pending() = pending;
                organization->set_user_message("Комментарий пользователя про организацию");

                auto* point = organization->mutable_center_point();
                point->set_lon(27.525566);
                point->set_lat(53.890712);

                langToMetadata["ru_RU"] = metadata;
            }
        }
        {
            auto* modifyContribution = modifyContributions.add_modify_contribution();
            auto* upsert = modifyContribution->mutable_upsert();
            upsert->set_id("10");
            auto* item = upsert->mutable_contribution();
            item->set_timestamp(1609765491); // 2021-01-04T13:04:51+00:00
            auto& langToMetadata = *item->mutable_lang_to_metadata();

            proto::contribution::ContributionMetadata metadata;
            auto* photo = metadata.mutable_photo_metadata()->add_contributed_photos();
            photo->set_id("photo1");
            langToMetadata["ru_RU"] = metadata;
        }
        {
            auto* modifyContribution = modifyContributions.add_modify_contribution();
            auto* upsert = modifyContribution->mutable_upsert();
            upsert->set_id("11");
            auto* item = upsert->mutable_contribution();
            item->set_timestamp(1609765492); // 2021-01-04T13:04:52+00:00
            auto& langToMetadata = *item->mutable_lang_to_metadata();

            proto::contribution::ContributionMetadata metadata;
            auto* photo = metadata.mutable_photo_metadata()->add_contributed_photos();
            photo->set_id("photo2");
            langToMetadata["ru_RU"] = metadata;
        }
        {
            auto* modifyContribution = modifyContributions.add_modify_contribution();
            auto* upsert = modifyContribution->mutable_upsert();
            upsert->set_id("12");
            auto* item = upsert->mutable_contribution();
            item->set_timestamp(1609765492); // 2021-01-04T13:04:52+00:00
            auto& langToMetadata = *item->mutable_lang_to_metadata();

            proto::contribution::ContributionMetadata metadata;
            auto* photo = metadata.mutable_photo_metadata()->add_contributed_photos();
            photo->set_id("photo3");
            langToMetadata["ru_RU"] = metadata;
        }
        {
            auto* modifyContribution = modifyContributions.add_modify_contribution();
            auto* upsert = modifyContribution->mutable_upsert();
            upsert->set_id("13");
            auto* item = upsert->mutable_contribution();
            item->set_timestamp(1609765492); // 2021-01-04T13:04:52+00:00
            auto& langToMetadata = *item->mutable_lang_to_metadata();

            proto::contribution::ContributionMetadata metadata;
            auto* photo = metadata.mutable_photo_metadata()->add_contributed_photos();
            photo->set_id("photo4");
            langToMetadata["ru_RU"] = metadata;
        }
    }
    return modifyContributions;
}

const Uid UID{111111};
const int FAKETVM = 11111;
const backoffice::tests::TestRequestValidator REQUEST_VALIDATOR;

void updateContributionsInDb(
    pqxx::transaction_base& txn,
    const proto::backoffice::ModifyContributions& modifyContributions,
    Uid uid)
{
    backoffice::updateDb(
        txn,
        modifyContributions,
        uid,
        FAKETVM,
        REQUEST_VALIDATOR
    );
}

} // namespace

Y_UNIT_TEST_SUITE(test_contributions_feed)
{

Y_UNIT_TEST(test_contribution_metadaata)
{
    auto txn = dbPool().masterWriteableTransaction();
    updateContributionsInDb(*txn, modifyContributions(), UID);

    maps::locale::Locale locale;
    std::istringstream("ru_RU") >> locale;
    auto metadata = getContributionMetadata(*txn, ContributionId{"2"}, UID, locale);
    UNIT_ASSERT(metadata.has_base_map());
    UNIT_ASSERT_VALUES_EQUAL(metadata.base_map().title(), "Заголовок");

    UNIT_ASSERT_EXCEPTION_CONTAINS(
        getContributionMetadata(*txn, ContributionId{"5"}, UID, locale),
        maps::RuntimeError,
        "No data for contribution 5 and uid 111111"
    );
}

Y_UNIT_TEST(test_append_contributions)
{
    auto txn = dbPool().masterWriteableTransaction();
    updateContributionsInDb(*txn, modifyContributions(), UID);
    auto rows = qb::SelectQuery(tables::CONTRIBUTION, qb::WhereConditions()).exec(*txn);

    maps::locale::Locale locale;
    std::istringstream("ru_RU") >> locale;
    proto::ugc_account::Contributions contributions;
    appendContributions(*txn, contributions, UID, rows, locale);
    UNIT_ASSERT_VALUES_EQUAL(contributions.contribution_size(), 6);
    UNIT_ASSERT_VALUES_EQUAL(contributions.contribution(0).contribution_id(), "2");
    UNIT_ASSERT_VALUES_EQUAL(contributions.contribution(0).metadata().base_map().title(), "Заголовок");
}

Y_UNIT_TEST(test_find_contributions)
{
    auto txn = dbPool().masterWriteableTransaction();
    updateContributionsInDb(*txn, modifyContributions(), UID);
    updateContributionsInDb(*txn, modifyContributions(), Uid{22222});

    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            // no such metadata in feed
            {MetadataId{2}},
            Paging{},
            Lang{"ru_RU"}.locale()
        ).contribution_size(),
        0
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            Uid{33333}, // no such uid
            {MetadataId{3}},
            Paging{},
            Lang{"ru_RU"}.locale()
        ).contribution_size(),
        0
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            Uid{22222},
            {MetadataId{3}},
            // after_limit = 4, base_id is null, get 1 object from feed start
            // because feed has only one object
            Paging{0, 4, std::nullopt},
            Lang{""}.locale()
        ).contribution_size(),
        // got id = "2"
        1
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            {MetadataId{3}},
            // after_limit = 1, base_id is null, get 1 object from feed start
            Paging{0, 1, std::nullopt},
            Lang{"en_US"}.locale()
        ).contribution_size(),
        // got id = "3"
        1
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            {MetadataId{3}, MetadataId{4}},
            // default paging: get 0 object from feed start
            Paging{},
            Lang{"by_BY"}.locale()
        ).contribution_size(),
        0
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            {MetadataId{3}, MetadataId{4}},
            // after_limit = 1, base_id is null, get 1 object from feed start
            Paging{std::nullopt, 1, std::nullopt},
            Lang{"en_GB"}.locale()
        ).contribution_size(),
        // got id = "3"
        1
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            {MetadataId{1}, MetadataId{3}, MetadataId{4}},
            // before_limit = 5, after_limit = 5, base_id="2"
            // get all objects from feed
            Paging{5, 5, "2"},
            Lang{"uk_BY"}.locale()
        ).contribution_size(),
        // got feed: 13, 12, 11, 10, 3, 2
        6
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            {MetadataId{1}, MetadataId{3}, MetadataId{4}},
            // before_limit = 1, after_limit = 1, base_id="10"
            Paging{1, 1, "10"},
            Lang{"by_RU"}.locale()
        ).contribution_size(),
        // got feed: 11, 10, 3
        3
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            {MetadataId{3}, MetadataId{4}},
            // after_limit = 1, base_id = "3"
            // default before_limit = 0
            Paging{std::nullopt, 1, "3"},
            Lang{"ru-RU"}.locale()
        ).contribution_size(),
        // got feed: 2
        1
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            UID,
            {MetadataId{1}, MetadataId{3}, MetadataId{4}},
            // before_limit = 5, base_id = "12"
            // default after_limit = 0
            Paging{5, std::nullopt, "12"},
            Lang{"ru-RU"}.locale()
        ).contribution_size(),
        // got feed: 13
        1
    );
    const auto contributions = findContributions(
        *txn,
        UID,
        {MetadataId{3}, MetadataId{4}},
        // before_limit = 0, after_limit = 1, base_id = "3"
        Paging{0, 1, "3"},
        Lang{"ru-RU_RU"}.locale()
    );
    UNIT_ASSERT_VALUES_EQUAL(contributions.contribution_size(), 1);
    const auto& contribution = contributions.contribution(0);
    UNIT_ASSERT_VALUES_EQUAL(contribution.contribution_id(), "2");
    UNIT_ASSERT_VALUES_EQUAL(contribution.time().value(), 1609765480);
    UNIT_ASSERT_VALUES_EQUAL(contribution.time().tz_offset(), 0);
    UNIT_ASSERT_VALUES_EQUAL(contribution.time().text(), "2021-01-04T13:04:40Z");
    UNIT_ASSERT(contribution.metadata().has_base_map());
    UNIT_ASSERT_VALUES_EQUAL(contribution.metadata().base_map().title(), "Заголовок");

    const auto enContributions = findContributions(
        *txn,
        UID,
        {MetadataId{3}, MetadataId{4}},
        // before_limit = 0, after_limit = 1, base_id = "3"
        Paging{0, 1, "3"},
        Lang{"en_US"}.locale()
    );
    UNIT_ASSERT_VALUES_EQUAL(enContributions.contribution_size(), 1);
    const auto& enContribution = enContributions.contribution(0);
    UNIT_ASSERT_VALUES_EQUAL(enContribution.contribution_id(), "2");
    UNIT_ASSERT(enContribution.metadata().has_base_map());
    UNIT_ASSERT_VALUES_EQUAL(enContribution.metadata().base_map().title(), "Title");
}

Y_UNIT_TEST(test_find_contributions_with_same_timestamp)
{
    const Uid uid{11111};
    auto txn = dbPool().masterWriteableTransaction();

    // Insert data to database

    proto::backoffice::ModifyContributions contributionsWithSameTimestamp;
    {
        const auto timestamp = 1609765480; // 2021-01-04T13:04:40+00:00
        for (size_t index = 0; index < 12; ++index) {
            auto* modifyContribution = contributionsWithSameTimestamp.add_modify_contribution();
            auto* upsert = modifyContribution->mutable_upsert();
            upsert->set_id(std::to_string(index).c_str());
            auto* item = upsert->mutable_contribution();
            item->set_timestamp(timestamp);
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
        }
    }
    backoffice::updateDb(
        *txn,
        contributionsWithSameTimestamp,
        uid,
        FAKETVM,
        REQUEST_VALIDATOR
    );

    // Test result

    // expected sorting order in feed: 9, 8, 7, 6, 5, 4, 3, 2, 11, 10, 1, 0
    // order is lexicographic

    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            uid,
            {MetadataId{3}},
            // after_limit = 10, base_id is null, get all object from the feed start
            // because feed has 10 object
            Paging{0, 10, std::nullopt},
            Lang{""}.locale()
        ).contribution_size(),
        10
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            uid,
            {MetadataId{3}},
            // after_limit = 10, base_id=0, get empty feed
            // because feed is sorted by id
            Paging{0, 10, {"0"}},
            Lang{""}.locale()
        ).contribution_size(),
        0
    );
    UNIT_ASSERT_VALUES_EQUAL(
        findContributions(
            *txn,
            uid,
            {MetadataId{3}},
            // before_limit = 4, base_id=7, get 2 objects: 9, 8
            // because feed is sorted by id
            Paging{4, 0, {"7"}},
            Lang{""}.locale()
        ).contribution_size(),
        2
    );
}

} // test_contributions_feed suite

} // namespace maps::wiki::ugc::account::tests
