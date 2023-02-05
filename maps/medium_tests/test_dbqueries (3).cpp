#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/support/lib/dbqueries.h>

#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::support::tests {

using namespace internal;
using namespace std::literals::chrono_literals;

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

samsara::Article makeBasicArticle(
    std::string body,
    samsara::TicketArticleType type,
    uint64_t id,
    maps::chrono::TimePoint createdTs)
{
    return samsara::Article{
        .body = std::move(body),
        .type = type,
        .id = id,
        .createdTs = createdTs,
        .from = {},
        .to = {},
        .attachments = {},
        .subject = std::nullopt,
        .customData = {},
        .channel = std::nullopt,
        .contentType = std::nullopt,
        .tags = {},
    };
}

} // namespace

Y_UNIT_TEST_SUITE(test_dbqueries)
{

const auto TIMEPOINT = maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00");

Y_UNIT_TEST(make_update_need_info_task_query)
{
    auto txn = dbPool().masterWriteableTransaction();

    FeedbackTask task{
        TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
        Service::Support,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::NeedInfo,
        api::tests::EMPTY_ORIGINAL_TASK,
        {/*integration*/},
        TIMEPOINT - 24h,
        TIMEPOINT - 12h
    };
    samsara::Ticket samsaraTicket{
        ServiceObjectId("samsara_id1"),
        samsara::QUEUE_NEED_INFO_1L,
        samsara::TicketStatus::Closed,
        samsara::TicketResolution::Resolved,
        {
            makeBasicArticle("2", samsara::TicketArticleType::Note, 2u, TIMEPOINT - 2h),
            makeBasicArticle("1", samsara::TicketArticleType::Note, 1u, TIMEPOINT - 1h),
        },
        {}
    };
    const auto query = buildUpdateNeedInfoTask(task, samsaraTicket, TIMEPOINT).asString(*txn);
    const auto expected =
        "UPDATE " + dbqueries::tables::TASK + " SET "
        "service = 'nmaps', "
        "status = 'new', "
        "updated_at = '2020-04-01 01:00:00+00:00', "
        "original_task = jsonb_set(original_task, '{support_message}', to_jsonb('1'::text)) "
        "WHERE id = 'fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885'";
    UNIT_ASSERT_NO_EXCEPTION(txn->exec(query));
    UNIT_ASSERT_VALUES_EQUAL(query, expected);
}

Y_UNIT_TEST(make_insert_need_info_change_task_query)
{
    auto txn = dbPool().masterWriteableTransaction();

    FeedbackTask task{
        TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
        Service::Support,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::NeedInfo,
        api::tests::EMPTY_ORIGINAL_TASK,
        {/*integration*/},
        TIMEPOINT - 24h,
        TIMEPOINT - 20h
    };
    samsara::Ticket samsaraTicket{
        ServiceObjectId("samsara_id"),
        samsara::QUEUE_NEED_INFO_1L,
        samsara::TicketStatus::Open,
        samsara::TicketResolution::Resolved,
        {
            makeBasicArticle("message", samsara::TicketArticleType::In, 21u, TIMEPOINT - 2h),
            makeBasicArticle("message", samsara::TicketArticleType::In, 1u, TIMEPOINT - 1h),
        },
        {}
    };
    const auto query =
        buildInsertNeedInfoChangeTask(task, samsaraTicket, TIMEPOINT)
        .asString(*txn);

    // Cannot insert task in feedback_task_changes if task with the same
    // id is not present in feedback_task table
    api::tests::insertTask(*txn, task);
    const auto expected =
        "INSERT INTO " + dbqueries::tables::TASK_CHANGES + " "
        "(created_at, service, status, task_id) VALUES ("
            "'2020-04-01 01:00:00+00:00', "
            "'support', "
            "'new', "
            "'fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885'"
        ")";
    UNIT_ASSERT_NO_EXCEPTION(txn->exec(query));
    UNIT_ASSERT_VALUES_EQUAL(query, expected);
}

Y_UNIT_TEST(make_insert_need_info_change_task_query_without_message)
{
    auto txn = dbPool().masterWriteableTransaction();

    FeedbackTask task{
        TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
        Service::Support,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::NeedInfo,
        api::tests::EMPTY_ORIGINAL_TASK,
        {/*integration*/},
        TIMEPOINT - 24h,
        TIMEPOINT - 14h
    };
    samsara::Ticket samsaraTicket{
        ServiceObjectId("samsara_id"),
        samsara::QUEUE_NEED_INFO_1L,
        samsara::TicketStatus::Open,
        samsara::TicketResolution::Resolved,
        {
            makeBasicArticle("message", samsara::TicketArticleType::In, 2u, TIMEPOINT - 2h),
            makeBasicArticle("message", samsara::TicketArticleType::In, 1u, TIMEPOINT - 1h),
        },
        {}
    };
    const auto query =
        buildInsertNeedInfoChangeTask(task, samsaraTicket, TIMEPOINT)
        .asString(*txn);

    // Cannot insert task in feedback_task_changes if task with the same
    // id is not present in feedback_task table
    api::tests::insertTask(*txn, task);
    const auto expected =
        "INSERT INTO " + dbqueries::tables::TASK_CHANGES + " "
        "(created_at, service, status, task_id) VALUES ("
            "'2020-04-01 01:00:00+00:00', "
            "'support', "
            "'new', "
            "'fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885'"
        ")";
    UNIT_ASSERT_NO_EXCEPTION(txn->exec(query));
    UNIT_ASSERT_VALUES_EQUAL(query, expected);
}

Y_UNIT_TEST(load_tasks_empty)
{
    const auto needInfoTasks = loadNeedInfoSupportTasks(dbPool());
    UNIT_ASSERT(needInfoTasks.empty());
}

} // test_dbqueries suite

} // namespace maps::wiki::feedback::api::support::tests
