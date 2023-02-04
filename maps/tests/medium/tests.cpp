#include <yandex/maps/wiki/social/feedback/description.h>
#include <yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <yandex/maps/wiki/social/gateway.h>
#include <yandex/maps/wiki/social/i_feed.h>
#include <yandex/maps/wiki/social/profile.h>
#include <yandex/maps/wiki/social/profile_gateway.h>
#include <yandex/maps/wiki/social/task_feed.h>

#include <maps/wikimap/mapspro/libs/social/helpers.h>
#include <maps/wikimap/mapspro/libs/social/magic_strings.h>
#include <maps/wikimap/mapspro/libs/social/tasks/acquire.h>
#include <maps/wikimap/mapspro/libs/social/tasks/release.h>
#include <maps/wikimap/mapspro/libs/social/tasks/resolve.h>

#include <maps/wikimap/mapspro/libs/social/tests/medium/bounds.h>
#include <maps/wikimap/mapspro/libs/social/tests/medium/helpers.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/event_creator.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/task_creator.h>

#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/unittest.h>

#include <maps/libs/chrono/include/days.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/serialization.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <array>
#include <chrono>
#include <unistd.h>


namespace {

const std::string CREATE_COMMIT_EVENT_TASKS_SCRIPT = SRC_("scripts/create_commit_event_tasks_data.sql");

const std::string CREATE_TASKS_SCRIPT = SRC_("scripts/create_tasks_data.sql");

}

namespace maps::wiki::social::tests {

using namespace std::chrono_literals;
using unittest::txnNow;

namespace {

const TUid TEST_UID = 111;
const TUid TEST_OTHER_UID = 234;

const std::string USER_CREATED_OR_UNBANNED_TIME = "2010-10-10 10:10:10";
const TId  TEST_STABLE_BRANCH_ID = 321;
const TId  TEST_AOI_ID = 123;
const TId  TEST_OTHER_AOI_ID = 1231;

const size_t OFFSET  = 0;
const size_t LIMIT  = 10;

const TUid NO_UID = 0;
const TIds NO_AOI_IDS;

const TId TEST_COMMIT_ID = 12345;
const TId TEST_COMMIT_OTHER_ID = TEST_COMMIT_ID+1;

const TId TEST_OBJECT_ID = 54321;
const auto NO_FEEDBACK_ID = std::nullopt;

const TId TRUNK = 0;

template <typename Checker>
void checkSuperConsole(pqxx::connection& conn, Checker checker)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);
    auto modConsole = gateway.superModerationConsole(TEST_UID);
    checker(txn, modConsole);
}

Task createCommitTask(
    pqxx::transaction_base& txn,
    TId commitId,
    const std::optional<PrimaryObjectData>& objData)
{
    Gateway gateway(txn);
    auto eventCreator = EventCreator(txn).uid(TEST_UID)
        .commitId(commitId).action("action").aoiIds({TEST_AOI_ID})
        .bounds("[46.12423009, 51.476361237, 46.124904577, 51.476770414]");
    if (objData) {
        eventCreator.primaryObjData(*objData);
    }
    return gateway.createTask(eventCreator, USER_CREATED_OR_UNBANNED_TIME);
}

feedback::Task createFeedbackTask(feedback::GatewayRW& gatewayRw)
{
    return gatewayRw.addTask(
        TEST_UID,
        feedback::TaskNew(
            geolib3::Point2(0., 0.),
            feedback::Type::Address,
            "source",
            feedback::Description("description")
        )
    );
}

Task createFeedbackModerationTask(
    pqxx::transaction_base& txn,
    const feedback::Task& feedback,
    TUid createdBy,
    const TIds& aoiIds)
{
    Gateway gtw(txn);
    auto event = gtw.createCloseFeedbackEvent(createdBy, feedback, aoiIds);
    return gtw.createTask(event, USER_CREATED_OR_UNBANNED_TIME);
}

Task createAndLockCommitTask(pqxx::transaction_base& txn, TId commitId, TUid lockedUid)
{
    auto task = createCommitTask(txn, commitId, std::nullopt);
    if (lockedUid == NO_UID) {
        return task;
    }

    Gateway gateway(txn);
    auto tasks = gateway
        .moderationConsole(lockedUid)
        .acquireTasks(EventFilter().deferred(Deferred::No), LIMIT, TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);
    UNIT_ASSERT_EQUAL(tasks.size(), 1);
    auto lockedTask = std::move(tasks.front());
    UNIT_ASSERT_EQUAL(lockedTask.commitId(), commitId);
    const auto& lockedEvent = task.event();
    UNIT_ASSERT_EQUAL(lockedEvent.createdBy(), TEST_UID);
    UNIT_ASSERT(lockedEvent.commitData());
    UNIT_ASSERT_EQUAL(lockedEvent.commitData()->action(), "action");
    return lockedTask;
}


void lockCommitTask(pqxx::transaction_base& txn, TId commitId, TUid lockBy)
{
    Gateway(txn)
        .moderationConsole(lockBy)
        .acquireTasks(EventFilter().commitIds({commitId}), LIMIT, TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);
}

void makeResolvedTimeOlderThanSupervisorDelay(pqxx::transaction_base& txn, TId taskId)
{
    auto query =
        " UPDATE " + sql::table::TASK +
        " SET " + sql::col::RESOLVED_AT + " = " +
            sql::col::RESOLVED_AT + " - " + sqlIntervalInSeconds(TEST_MODERATION_TIME_INTERVALS.supervisorDelay) +
            " - '10 minutes'::interval"
        " WHERE " + sql::col::EVENT_ID + " = " + std::to_string(taskId);
    txn.exec(query);
}

std::map<TId, TaskCounts> countsByAoiIds(
    const CountsByAoiCategoryId& counts)
{
    std::map<TId, TaskCounts> result;
    for (const auto& aoi2counts : counts.taskCounts) {
        for (const auto& category2counts : aoi2counts.second) {
            TaskCounts& taskCounts = result[aoi2counts.first];
            taskCounts += category2counts.second;
        }
    }
    return result;
}

void checkCounts(
    const SuperModerationConsole& console,
    ModerationMode mode,
    size_t available,
    size_t acquired,
    size_t total)
{
    auto hasAcquirableTasks = available || acquired;
    auto hasTasks = hasAcquirableTasks || total;

    auto counts = countsByAoiIds(
        console.countsByAoiCategoryId(mode, EventFilter().deferred(Deferred::No), {TEST_AOI_ID}, TEST_MODERATION_TIME_INTERVALS));
    UNIT_ASSERT_EQUAL(counts.size(), hasTasks ? 1 : 0);
    UNIT_ASSERT_EQUAL(counts[TEST_AOI_ID].available(), available);
    UNIT_ASSERT_EQUAL(counts[TEST_AOI_ID].acquired(), acquired);
    UNIT_ASSERT_EQUAL(counts[TEST_AOI_ID].total(), total);
    UNIT_ASSERT_EQUAL(console.hasAcquirableTasks(mode, {TEST_AOI_ID}, TEST_MODERATION_TIME_INTERVALS), hasAcquirableTasks);
}

} //anonymous namespace

Y_UNIT_TEST_SUITE(tests_suite) {

Y_UNIT_TEST_F(select_active_tasks, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    Gateway gateway(txn);
    const auto activeTasks = gateway.loadAllActiveEditTasks();
    UNIT_ASSERT_EQUAL(activeTasks.size(), 2);
    UNIT_ASSERT_EQUAL(activeTasks.front().id(), 32643153);
    UNIT_ASSERT_EQUAL(activeTasks.back().id(), 35892476);

    const auto activeTasksCommitIds = gateway.getAllActiveEditTasksCommitIds();
    const auto activeTasksCommitIdsExpected = TIds({34919586, 38175684});
    UNIT_ASSERT_EQUAL(
        activeTasksCommitIds,
        activeTasksCommitIdsExpected
    );
}

Y_UNIT_TEST_F(get_tasks_by_resolved_at, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_TASKS_SCRIPT));

    auto now = txnNow(txn);
    Gateway gateway(txn);

    DateTimeCondition resolvedAt(now - 5min, now + 5min);
    UNIT_ASSERT_EQUAL((TIds{2, 3}), gateway.getTaskIdsResolvedAt(resolvedAt));

    resolvedAt = DateTimeCondition(now - 10min, now);
    UNIT_ASSERT_EQUAL((TIds{4}), gateway.getTaskIdsResolvedAt(resolvedAt));
}

Y_UNIT_TEST_F(get_tasks_by_closed_at, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_TASKS_SCRIPT));

    auto now = txnNow(txn);
    Gateway gateway(txn);

    approveTask(txn, 4, TEST_UID, now - 5min);
    approveTask(txn, 3, TEST_UID, now);
    approveTask(txn, 2, TEST_UID, now + 5min);
    approveTask(txn, 1, TEST_UID, now + 10min);

    DateTimeCondition closedAt(now, now + 10min);
    UNIT_ASSERT_EQUAL((TIds{2, 3}), gateway.getTaskIdsClosedAt(closedAt));
}

Y_UNIT_TEST_F(should_get_tasks_by_filter, DbFixture)
{
    pqxx::work txn(conn);
    const auto now = txnNow(txn);

    const std::array taskIds = {
        TaskCreator(txn).event(EventCreator(txn).primaryObjData({1, "cat 1", "label", "note"}))().id(), // 0
        TaskCreator(txn).event(EventCreator(txn).primaryObjData({1, "cat 2", "label", "note"}))().id(), // 1
        TaskCreator(txn).event(EventCreator(txn).primaryObjData({1, "cat 3", "label", "note"}))().id(), // 2

        TaskCreator(txn)                                                                                // 3
            .resolved(11, ResolveResolution::Revert, "NOW() - '10 min'::interval")
            .closed  (12, CloseResolution::Approve,  "NOW() - ' 1 min'::interval")
            .create().id(),
        TaskCreator(txn)                                                                                // 4
            .resolved(21, ResolveResolution::Accept, "NOW() - '20 min'::interval")
            .closed  (22, CloseResolution::Revert,   "NOW() - ' 2 min'::interval")
            .create().id(),
        TaskCreator(txn)                                                                                // 5
            .resolved(31, ResolveResolution::Accept, "NOW() - '30 min'::interval")
            .closed  (32, CloseResolution::Revert,   "NOW() - ' 3 min'::interval")
            .create().id(),
        TaskCreator(txn)                                                                                // 6
            .resolved(41, ResolveResolution::Revert, "NOW() - '40 min'::interval")
            .closed  (42, CloseResolution::Approve,  "NOW() - ' 4 min'::interval")
            .create().id()
    };

    Gateway gateway(txn);

    {
        TaskFilter filter;
        filter.setCategories(CategoryIdsSet{"cat 1", "cat 3"});
        UNIT_ASSERT_EQUAL(gateway.getTaskIds(filter), TIds({taskIds[0], taskIds[2]}));
    }

    {
        TaskFilter filter;
        filter.setResolvedBy({11, 31, 41});
        UNIT_ASSERT_EQUAL(gateway.getTaskIds(filter), TIds({taskIds[3], taskIds[5], taskIds[6]}));
    }

    {
        TaskFilter filter;
        filter.setResolvedAt(DateTimeCondition{now - 35min, std::nullopt});
        UNIT_ASSERT_EQUAL(gateway.getTaskIds(filter), TIds({taskIds[3], taskIds[4], taskIds[5]}));
    }

    {
        TaskFilter filter;
        filter.setResolveResolution(ResolveResolution::Accept);
        UNIT_ASSERT_EQUAL(gateway.getTaskIds(filter), TIds({taskIds[4], taskIds[5]}));
    }

    {
        TaskFilter filter;
        filter.setClosedBy({12, 22, 42});
        UNIT_ASSERT_EQUAL(gateway.getTaskIds(filter), TIds({taskIds[3], taskIds[4], taskIds[6]}));
    }

    {
        TaskFilter filter;
        filter.setClosedAt(DateTimeCondition{std::nullopt, now - 1min - 30s});
        UNIT_ASSERT_EQUAL(gateway.getTaskIds(filter), TIds({taskIds[4], taskIds[5], taskIds[6]}));
    }

    {
        TaskFilter filter;
        filter.setCloseResolution(CloseResolution::Approve);
        UNIT_ASSERT_EQUAL(gateway.getTaskIds(filter), TIds({taskIds[3], taskIds[6]}));
    }
}

Y_UNIT_TEST_F(select_edit_tasks_by_commit_ids, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    Gateway gateway(txn);
    const auto noTasks = gateway.loadEditTasksByCommitIds({});
    UNIT_ASSERT(noTasks.empty());

    const auto oneTask = gateway.loadEditTasksByCommitIds({34919586});
    UNIT_ASSERT_EQUAL(oneTask.size(), 1);
}

Y_UNIT_TEST_F(load_tasks_closed, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    constexpr TId before = 0;
    constexpr TId after = 0;
    constexpr size_t perPage = 11;

    social::TaskFeedParams params(before, after, perPage, TaskFeedParams::OrderBy::ClosedAt);
    social::TaskFilter filter;
    filter.setClosedBy({TEST_UID});

    Gateway gateway(txn);
    const auto taskFeed = gateway.loadTasks(params, filter);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 3);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().front().id(), 32686898);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().back().id(), 32686854);
    UNIT_ASSERT_EQUAL(taskFeed.hasMore(), social::HasMore::No);
}

Y_UNIT_TEST_F(load_tasks_closed_next, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    constexpr TId before = 0;
    constexpr TId after = 32686854;
    constexpr size_t perPage = 4;

    social::TaskFeedParams params(before, after, perPage, TaskFeedParams::OrderBy::ClosedAt);
    social::TaskFilter filter;
    filter.setClosedBy({TEST_UID});

    Gateway gateway(txn);
    const auto taskFeed = gateway.loadTasks(params, filter);
    UNIT_ASSERT(taskFeed.tasks().empty());
    UNIT_ASSERT_EQUAL(taskFeed.hasMore(), social::HasMore::No);
}

Y_UNIT_TEST_F(load_tasks_closed_prev, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    constexpr TId before = 32686769;
    constexpr TId after = 0;
    constexpr size_t perPage = 2;

    social::TaskFeedParams params(before, after, perPage, TaskFeedParams::OrderBy::ClosedAt);
    social::TaskFilter filter;
    filter.setClosedBy({TEST_UID});

    Gateway gateway(txn);
    const auto taskFeed = gateway.loadTasks(params, filter);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 2);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().front().id(), 32686898);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().back().id(), 32686823);
    UNIT_ASSERT_EQUAL(taskFeed.hasMore(), social::HasMore::No);
}

Y_UNIT_TEST_F(load_tasks_resolved, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    constexpr TId before = 0;
    constexpr TId after = 0;
    constexpr size_t perPage = 2;

    social::TaskFeedParams params(after, before, perPage, TaskFeedParams::OrderBy::ResolvedAt);
    social::TaskFilter filter;
    filter.setResolvedBy({TEST_UID});

    Gateway gateway(txn);
    const auto taskFeed = gateway.loadTasks(params, filter);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 2);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().front().id(), 35892476);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().back().id(), 32686898);
    UNIT_ASSERT_EQUAL(taskFeed.hasMore(), social::HasMore::Yes);
}

Y_UNIT_TEST_F(load_tasks_resolved_next, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    constexpr TId before = 0;
    constexpr TId after = 32686854;
    constexpr size_t perPage = 3;

    social::TaskFeedParams params(before, after, perPage, TaskFeedParams::OrderBy::ResolvedAt);
    social::TaskFilter filter;
    filter.setResolvedBy({TEST_UID});

    Gateway gateway(txn);
    const auto taskFeed = gateway.loadTasks(params, filter);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 3);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().front().id(), 32686823);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().back().id(), 32643153);
    UNIT_ASSERT_EQUAL(taskFeed.hasMore(), social::HasMore::No);
}

Y_UNIT_TEST_F(load_tasks_resolved_prev, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_COMMIT_EVENT_TASKS_SCRIPT));

    constexpr TId before = 32643153;
    constexpr TId after = 0;
    constexpr size_t perPage = 2;

    social::TaskFeedParams params(before, after, perPage, TaskFeedParams::OrderBy::ResolvedAt);
    social::TaskFilter filter;
    filter.setResolvedBy({TEST_UID});

    Gateway gateway(txn);
    const auto taskFeed = gateway.loadTasks(params, filter);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 2);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().front().id(), 32686823);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().back().id(), 32686769);
    UNIT_ASSERT_EQUAL(taskFeed.hasMore(), social::HasMore::Yes);
}

Y_UNIT_TEST_F(should_load_only_commit_moderation_tasks, DbFixture)
{
    pqxx::work txn(conn);
    txn.exec(maps::common::readFileToString(CREATE_TASKS_SCRIPT));
    Gateway gateway(txn);

    constexpr TId before = 0;
    constexpr TId after = 0;
    constexpr size_t perPage = 10; // large enough to load all tasks

    social::TaskFeedParams params(before, after, perPage, TaskFeedParams::OrderBy::ResolvedAt);
    social::TaskFilter filter;
    filter.setResolvedBy({TEST_UID});

    const auto taskFeed = gateway.loadTasks(params, filter);
    UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 2);
    UNIT_ASSERT_EQUAL(taskFeed.tasks()[0].id(), 2);
    UNIT_ASSERT_EQUAL(taskFeed.tasks()[1].id(), 4);
    UNIT_ASSERT_EQUAL(taskFeed.hasMore(), HasMore::No);
}

Y_UNIT_TEST_F(should_load_tasks_filtered_by_resolved_or_closed_by, DbFixture)
{
    pqxx::work txn(conn);
    social::Gateway gateway(txn);

    const std::array taskIds = {
        TaskCreator(txn)
            .resolved(10, ResolveResolution::Accept, "NOW() - '10 min'::interval")
            .closed  (11, CloseResolution::Approve,  "NOW() - ' 1 min'::interval")
            .create().id(),
        TaskCreator(txn)
            .resolved(20, ResolveResolution::Accept, "NOW() - '20 min'::interval")
            .closed  (21, CloseResolution::Approve,  "NOW() - ' 2 min'::interval")
            .create().id(),
        TaskCreator(txn)
            .resolved(30, ResolveResolution::Accept, "NOW() - '30 min'::interval")
            .closed  (31, CloseResolution::Approve,  "NOW() - ' 3 min'::interval")
            .create().id()
    };

    const TId BEFORE = 0;
    const TId AFTER = 0;
    const size_t PER_PAGE = 10;
    const social::TaskFeedParams params(BEFORE, AFTER, PER_PAGE, TaskFeedParams::OrderBy::ResolvedAt);

    {
        social::TaskFilter filter;
        filter.setResolvedBy({10, 30});

        const auto taskFeed = gateway.loadTasks(params, filter);

        UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 2);
        UNIT_ASSERT_EQUAL(taskFeed.tasks()[0].id(), taskIds[0]);
        UNIT_ASSERT_EQUAL(taskFeed.tasks()[1].id(), taskIds[2]);
        UNIT_ASSERT_EQUAL(taskFeed.hasMore(), HasMore::No);
    }

    {
        social::TaskFilter filter;
        filter.setClosedBy({11, 21});

        const auto taskFeed = gateway.loadTasks(params, filter);

        UNIT_ASSERT_EQUAL(taskFeed.tasks().size(), 2);
        UNIT_ASSERT_EQUAL(taskFeed.tasks()[0].id(), taskIds[0]);
        UNIT_ASSERT_EQUAL(taskFeed.tasks()[1].id(), taskIds[1]);
        UNIT_ASSERT_EQUAL(taskFeed.hasMore(), HasMore::No);
    }
}

Y_UNIT_TEST_F(acquire_commit_moderation_tasks_by_aoi_id, DbFixture)
{
    pqxx::work txn(conn);
    social::Gateway gateway(txn);

    const auto taskId = TaskCreator(txn)
        .createdAt("NOW() - '2 days'::interval")
        .event(EventCreator(txn).aoiIds({TEST_AOI_ID}))
    .create().id();

    TaskCreator(txn)
        .createdAt("NOW() - '2 days'::interval")
        .event(EventCreator(txn).aoiIds({TEST_OTHER_AOI_ID}))
    .create();

    const auto eventFilter = EventFilter()
        .moderationMode(ModerationMode::Supervisor)
        .commonTasksPermitted(true)
        .aoiId(TEST_AOI_ID);

    const auto acquiredTasks = tasks::acquire(
        txn,
        TEST_UID,
        eventFilter,
        std::nullopt,
        TasksOrder::NewestFirst,
        TEST_MODERATION_TIME_INTERVALS);

    UNIT_ASSERT_EQUAL(acquiredTasks.size(), 1);
    UNIT_ASSERT_EQUAL(acquiredTasks[0].id(), taskId);
}

Y_UNIT_TEST_F(acquire_commit_moderation_tasks_by_event_type, DbFixture)
{
    pqxx::work txn(conn);
    social::Gateway gateway(txn);

    const auto taskId = TaskCreator(txn)
        .createdAt("NOW() - '2 days'::interval")
        .event(EventCreator(txn).type(EventType::RequestForDeletion))
    .create().id();

    TaskCreator(txn)
        .createdAt("NOW() - '2 days'::interval")
        .event(EventCreator(txn).type(EventType::Complaint))
    .create().id();

    const auto eventFilter = EventFilter()
        .commonTasksPermitted(true)
        .eventType(EventType::RequestForDeletion);

    const auto acquiredTasks = tasks::acquire(
        txn,
        TEST_UID,
        eventFilter,
        std::nullopt,
        TasksOrder::NewestFirst,
        TEST_MODERATION_TIME_INTERVALS);

    UNIT_ASSERT_EQUAL(acquiredTasks.size(), 1);
    UNIT_ASSERT_EQUAL(acquiredTasks[0].id(), taskId);
}

Y_UNIT_TEST_F(acquire_commit_moderation_tasks_by_category_ids, DbFixture)
{
    pqxx::work txn(conn);
    social::Gateway gateway(txn);

    const auto taskId = TaskCreator(txn)
        .createdAt("NOW() - '2 days'::interval")
        .event(EventCreator(txn).primaryObjData({1, "category_id", "", ""}))
    .create().id();

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            EventFilter()
                .moderationMode(ModerationMode::Supervisor)
                .commonTasksPermitted(true)
                .categoryIds({"another_category_id"}),
            std::nullopt,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT(acquiredTasks.empty());
    }

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            EventFilter()
                .moderationMode(ModerationMode::Supervisor)
                .commonTasksPermitted(true)
                .categoryIds({"category_id", "another_category_id"}),
            std::nullopt,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT_EQUAL(acquiredTasks.size(), 1);
        UNIT_ASSERT_EQUAL(acquiredTasks[0].id(), taskId);
    }

}

Y_UNIT_TEST_F(acquire_commit_moderation_tasks_of_novice_users, DbFixture)
{
    pqxx::work txn(conn);
    const auto now = txnNow(txn);
    social::Gateway gateway(txn);

    const auto taskId = TaskCreator(txn)
        .createdAt("NOW() - '2 days'::interval")
        .event(EventCreator(txn).uid(TEST_UID))
        .userCreatedOrUnbannedAt(chrono::formatSqlDateTime(now))
    .create().id();

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            EventFilter()
                .moderationMode(ModerationMode::Supervisor)
                .commonTasksPermitted(true)
                .noviceUsers(false),
            std::nullopt,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT(acquiredTasks.empty());
    }

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            EventFilter()
                .moderationMode(ModerationMode::Supervisor)
                .commonTasksPermitted(true)
                .noviceUsers(true),
            std::nullopt,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT_EQUAL(acquiredTasks.size(), 1);
        UNIT_ASSERT_EQUAL(acquiredTasks[0].id(), taskId);
    }
}

Y_UNIT_TEST_F(acquire_commit_moderation_tasks_order_and_limits, DbFixture)
{
    pqxx::work txn(conn);
    social::Gateway gateway(txn);

    const std::vector<TId> taskIds {
        TaskCreator(txn).createdAt("NOW() - '2 days'::interval")().id(),
        TaskCreator(txn).createdAt("NOW() - '2 days'::interval")().id(),
        TaskCreator(txn).createdAt("NOW() - '2 days'::interval")().id()};

    const auto eventFilter = EventFilter()
        .moderationMode(ModerationMode::Supervisor)
        .commonTasksPermitted(true);

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            eventFilter,
            2,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT_EQUAL(acquiredTasks.size(), 2);
        UNIT_ASSERT_EQUAL(acquiredTasks[0].id(), taskIds[2]);
        UNIT_ASSERT_EQUAL(acquiredTasks[1].id(), taskIds[1]);

        tasks::release(txn, TEST_UID, {taskIds[1], taskIds[2]});
    }

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            eventFilter,
            2,
            TasksOrder::OldestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT_EQUAL(acquiredTasks.size(), 2);
        UNIT_ASSERT_EQUAL(acquiredTasks[0].id(), taskIds[0]);
        UNIT_ASSERT_EQUAL(acquiredTasks[1].id(), taskIds[1]);
    }
}

Y_UNIT_TEST_F(acquire_commit_moderation_tasks_of_suspicious_users, DbFixture)
{
    pqxx::work txn(conn);
    const auto now = txnNow(txn);
    social::Gateway gateway(txn);

    txn.exec(
        "INSERT INTO social.suspicious_users (uid, registered_or_unbanned_at) "
        "VALUES (" + std::to_string(TEST_UID) + ", NOW())");

    const auto taskId = TaskCreator(txn)
        .createdAt("NOW() - '2 days'::interval")
        .event(EventCreator(txn).uid(TEST_UID))
        .userCreatedOrUnbannedAt(chrono::formatSqlDateTime(now))
    .create().id();

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            EventFilter()
                .moderationMode(ModerationMode::Supervisor)
                .commonTasksPermitted(true)
                .suspiciousUsers(false),
            std::nullopt,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT(acquiredTasks.empty());
    }

    {
        const auto acquiredTasks = tasks::acquire(
            txn,
            TEST_UID,
            EventFilter()
                .moderationMode(ModerationMode::Supervisor)
                .commonTasksPermitted(true)
                .suspiciousUsers(true),
            std::nullopt,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

        UNIT_ASSERT_EQUAL(acquiredTasks.size(), 1);
        UNIT_ASSERT_EQUAL(acquiredTasks[0].id(), taskId);
    }
}

Y_UNIT_TEST_F(load_tasks_by_ids, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);
    feedback::GatewayRW gatewayRw(txn);

    auto feedbackModerationTaskId = createFeedbackModerationTask(
        txn,
        createFeedbackTask(gatewayRw),
        TEST_UID,
        {TEST_AOI_ID}
    ).id();

    auto commitModerationTaskId = createCommitTask(
        txn,
        TEST_COMMIT_ID,
        std::nullopt
    ).id();

    {
        auto tasks = gtw.loadTasksByTaskIds({feedbackModerationTaskId});
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        UNIT_ASSERT_EQUAL(tasks.front().id(), feedbackModerationTaskId);
    }
    {
        auto tasks = gtw.loadTasksByTaskIds({commitModerationTaskId});
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        UNIT_ASSERT_EQUAL(tasks.front().id(), commitModerationTaskId);
    }
    {
        auto tasks = gtw.loadTasksByTaskIds(
            {feedbackModerationTaskId, commitModerationTaskId}
        );
        UNIT_ASSERT_EQUAL(tasks.size(), 2);
        UNIT_ASSERT_EQUAL(
            std::set<TId>({tasks[0].id(), tasks[1].id()}),
            std::set<TId>({commitModerationTaskId, feedbackModerationTaskId})
        );
    }
}

Y_UNIT_TEST_F(empty_database_tasks, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    UNIT_ASSERT(gateway.loadAllActiveEditTasks().empty());
    UNIT_ASSERT(gateway.getAllActiveEditTasksCommitIds().empty());
    UNIT_ASSERT(gateway.loadActiveTasksByTaskIds({}).empty());
    UNIT_ASSERT(gateway.loadTasksByTaskIds({}).empty());
    UNIT_ASSERT(gateway.loadActiveEditTasksByCommitIds({}).empty());
    UNIT_ASSERT(gateway.loadEditTasksByCommitIds({}).empty());

    TaskIds ids{0,1,2,3};
    UNIT_ASSERT(gateway.loadActiveTasksByTaskIds(ids).empty());
    UNIT_ASSERT(gateway.loadTasksByTaskIds(ids).empty());
    UNIT_ASSERT(gateway.loadActiveEditTasksByCommitIds(ids).empty());
    UNIT_ASSERT(gateway.loadEditTasksByCommitIds(ids).empty());
}

Y_UNIT_TEST_F(empty_database_events, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    UNIT_ASSERT_EQUAL(gateway.loadEditEventsByCommitIds({}).size(), 0);

    TIds ids{0, 1, 2, 3};
    UNIT_ASSERT_EQUAL(gateway.loadEditEventsByCommitIds(ids).size(), 0);
}

Y_UNIT_TEST_F(trunk_feed_user_aoi, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    auto feedAoi = gateway.feed(TRUNK, TEST_AOI_ID, FeedType::Aoi);
    auto feedUser = gateway.feed(TRUNK, TEST_UID, FeedType::User);

    auto filteredFeedAoi = gateway.feed(
        TRUNK, TEST_AOI_ID, FeedType::Aoi, FeedFilter().categoryIds({{"bld"}, InclusionPolicy::Including}));
    auto filteredFeedUser = gateway.feed(
        TRUNK, TEST_UID, FeedType::User, FeedFilter().categoryIds({{"rd_el"}, InclusionPolicy::Including}));

    auto checkFeed = [&](const Feed& feed, FeedType type, size_t count)
    {
        UNIT_ASSERT_EQUAL(feed.branchId(), TRUNK);

        UNIT_ASSERT_EQUAL(feed.subscriberId(), type == FeedType::Aoi ? TEST_AOI_ID : TEST_UID);
        UNIT_ASSERT_EQUAL(feed.feedType(), type == FeedType::Aoi ? FeedType::Aoi : FeedType::User );

        UNIT_ASSERT_EQUAL(feed.count(), count);
        UNIT_ASSERT_EQUAL(feed.events(OFFSET, LIMIT).size(), count);
        auto eventsResult = feed.eventsHead(LIMIT);
        UNIT_ASSERT_EQUAL(eventsResult.first.size(), count);
        UNIT_ASSERT(eventsResult.second == HasMore::No);
    };

    checkFeed(feedAoi, FeedType::Aoi, 0);
    checkFeed(feedUser, FeedType::User, 0);

    EventCreator(txn).uid(TEST_UID).aoiIds({TEST_AOI_ID}).create();
    checkFeed(feedAoi, FeedType::Aoi, 1);
    checkFeed(feedUser, FeedType::User, 1);
    checkFeed(filteredFeedAoi, FeedType::Aoi, 0);
    checkFeed(filteredFeedUser, FeedType::User, 0);

    EventCreator(txn).uid(TEST_UID).primaryObjData({777, "bld", "label1", "notes1"})
        .aoiIds({TEST_AOI_ID}).create();
    checkFeed(feedAoi, FeedType::Aoi, 2);
    checkFeed(feedUser, FeedType::User, 2);
    checkFeed(filteredFeedAoi, FeedType::Aoi, 1);
    checkFeed(filteredFeedUser, FeedType::User, 0);

    EventCreator(txn).uid(TEST_UID).primaryObjData({888, "rd_el", "label2", "notes2"})
        .aoiIds({TEST_AOI_ID}).create();
    checkFeed(feedAoi, FeedType::Aoi, 3);
    checkFeed(feedUser, FeedType::User, 3);
    checkFeed(filteredFeedAoi, FeedType::Aoi, 1);
    checkFeed(filteredFeedUser, FeedType::User, 1);
}

Y_UNIT_TEST_F(non_trunk_feed, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    UNIT_ASSERT_EXCEPTION(
        gateway.feed(TEST_STABLE_BRANCH_ID, 0, FeedType::User), maps::Exception);

    auto feed = gateway.feed(TEST_STABLE_BRANCH_ID, TEST_UID, FeedType::User);

    auto checkFeed = [&](size_t count)
    {
        UNIT_ASSERT_EQUAL(feed.branchId(), TEST_STABLE_BRANCH_ID);
        UNIT_ASSERT_EQUAL(feed.subscriberId(), TEST_UID);
        UNIT_ASSERT_EQUAL(feed.feedType(), FeedType::User);

        UNIT_ASSERT_EQUAL(feed.count(), count);
        UNIT_ASSERT_EQUAL(feed.events(OFFSET, LIMIT).size(), count);
        auto eventsResult = feed.eventsHead(LIMIT);
        UNIT_ASSERT_EQUAL(eventsResult.first.size(), count);
        UNIT_ASSERT(eventsResult.second == HasMore::No);
    };

    checkFeed(0);
    EventCreator(txn).uid(TEST_UID).branchId(TEST_STABLE_BRANCH_ID).create();
    checkFeed(1);
}

Y_UNIT_TEST_F(should_filter_feed_by_commit_ids, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    EventCreator(txn).uid(TEST_UID).commitId(11).create();
    EventCreator(txn).uid(TEST_UID).commitId(33).create();
    EventCreator(txn).uid(TEST_UID).commitId(55).create();

    UNIT_ASSERT_EQUAL(
        gateway.feed(TRUNK, TEST_UID, FeedType::User, FeedFilter().commitIds({22, 44, 66})).count(),
        0
    );

    UNIT_ASSERT_EQUAL(
        gateway.feed(TRUNK, TEST_UID, FeedType::User, FeedFilter().commitIds({22, 33, 44, 66})).count(),
        1
    );

    UNIT_ASSERT_EQUAL(
        gateway.feed(TRUNK, TEST_UID, FeedType::User, FeedFilter().commitIds({22, 33, 44, 55})).count(),
        2
    );

    UNIT_ASSERT_EQUAL(
        gateway.feed(TRUNK, TEST_UID, FeedType::User, FeedFilter().commitIds({11, 22, 33, 44, 55})).count(),
        3
    );

    UNIT_ASSERT_EQUAL(
        gateway.feed(TRUNK, TEST_UID, FeedType::User, FeedFilter().commitIds({11, 33, 55})).count(),
        3
    );
}

Y_UNIT_TEST_F(preapproved_feed_should_contain_commits_from_trunk_only, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    const size_t OFFSET = 0;
    const size_t SUBSCRIBER_ID = 0; // There are no subscribers for preapproved feed.

    const std::array events = {
        EventCreator(txn).uid(TEST_UID).branchId(TRUNK).create(),
        EventCreator(txn).uid(TEST_UID).branchId(42).create(),
        EventCreator(txn).uid(TEST_UID).branchId(TRUNK).create()
    };

    const TIds expected = {events[0].id(), events[2].id()};
    const auto result = getIds(
        gateway
        .feed(TRUNK, SUBSCRIBER_ID, FeedType::PreApproved, FeedFilter())
        .events(OFFSET, events.size())
    );

    UNIT_ASSERT_EQUAL(result, expected);
}

Y_UNIT_TEST_F(preapproved_feed_should_contain_edit_events_only, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    const size_t OFFSET = 0;
    const size_t SUBSCRIBER_ID = 0; // There are no subscribers for preapproved feed.

    const std::array events = {
        EventCreator(txn).uid(TEST_UID).type(EventType::Complaint).create(),
        EventCreator(txn).uid(TEST_UID).type(EventType::Edit).create(),
        EventCreator(txn).uid(TEST_UID).type(EventType::RequestForDeletion).create()
    };

    const TIds expected = {events[1].id()};
    const auto result = getIds(
        gateway
        .feed(TRUNK, SUBSCRIBER_ID, FeedType::PreApproved, FeedFilter())
        .events(OFFSET, events.size())
    );

    UNIT_ASSERT_EQUAL(result, expected);
}

Y_UNIT_TEST_F(empty_database_moderation_console, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    UNIT_ASSERT_EXCEPTION(
        gateway.moderationConsole(0),
        maps::Exception);

    auto superConsole = gateway.superModerationConsole(TEST_UID);

    for (auto mode : {ModerationMode::Moderator,
                      ModerationMode::SuperModerator,
                      ModerationMode::Supervisor}) {

        UNIT_ASSERT_EQUAL(
            superConsole.countsByAoiCategoryId(
                mode, social::EventFilter(), {TEST_AOI_ID}, TEST_MODERATION_TIME_INTERVALS
            ).taskCounts.size(),
            0);
        UNIT_ASSERT_EQUAL(superConsole.hasAcquirableTasks(
            mode, {TEST_AOI_ID}, TEST_MODERATION_TIME_INTERVALS), false);

        auto modConsole = gateway.moderationConsole(TEST_UID);

        UNIT_ASSERT_EQUAL(modConsole.acquireTasks(
            {}, LIMIT, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS).size(), 0);
        UNIT_ASSERT_EQUAL(modConsole.releaseTasks().size(), 0);
    }
}

Y_UNIT_TEST_F(empty_database_super_moderation_console, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gateway(txn);

    UNIT_ASSERT_EXCEPTION(gateway.superModerationConsole(0), maps::Exception);

    auto modConsole = gateway.superModerationConsole(TEST_UID);

    TaskIds ids{0,1,2,3};
    for (auto resolution : {ResolveResolution::Accept,
                            ResolveResolution::Edit,
                            ResolveResolution::Revert}) {

        UNIT_ASSERT_EQUAL(modConsole.resolveTasksByTaskIds(resolution, ids).size(), 0);
        UNIT_ASSERT_EQUAL(modConsole.resolveEditTasksByCommitIds(resolution, ids).size(), 0);
    }

    for (auto resolution : {CloseResolution::Approve,
                            CloseResolution::Edit,
                            CloseResolution::Revert}) {

        UNIT_ASSERT_EQUAL(modConsole.closeTasksByTaskIds(resolution, ids).size(), 0);
    }

    UNIT_ASSERT_EQUAL(modConsole.closeEditTasksByCommitIds(
        ids, ResolveResolution::Accept, CloseResolution::Approve).size(), 0);
    UNIT_ASSERT_EQUAL(modConsole.closeEditTasksByCommitIds(
        ids, ResolveResolution::Edit, CloseResolution::Edit).size(), 0);
    UNIT_ASSERT_EQUAL(modConsole.closeEditTasksByCommitIds(
        ids, ResolveResolution::Revert, CloseResolution::Revert).size(), 0);
}

Y_UNIT_TEST_F(simple_database_super_moderation_console_resolve, DbFixture)
{
    for (auto resolution : {ResolveResolution::Accept,
                            ResolveResolution::Edit,
                            ResolveResolution::Revert}) {

        for (TUid lockedUid : {NO_UID, TEST_OTHER_UID}) {
            checkSuperConsole(
                conn,
                [&](pqxx::transaction_base& txn, SuperModerationConsole& modConsole)
                {
                    auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, lockedUid);
                    UNIT_ASSERT_EQUAL(modConsole.resolveTasksByTaskIds(resolution, {0, task.id()}).size(), 0);
                }
            );
        }

        checkSuperConsole(
            conn,
            [&](pqxx::transaction_base& txn, SuperModerationConsole& superConsole)
            {
                const auto mode = ModerationMode::Moderator;

                Gateway gateway(txn);
                checkCounts(superConsole, mode, 0, 0, 0);

                auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, TEST_UID);
                auto other_task = createAndLockCommitTask(txn, TEST_COMMIT_OTHER_ID, TEST_OTHER_UID);
                checkCounts(superConsole, mode, 0, 1, 2);

                auto result = superConsole.resolveTasksByTaskIds(
                        resolution, {0, task.id()});
                UNIT_ASSERT_EQUAL(result.size(), 1);
                UNIT_ASSERT_EQUAL(*result.begin(), task.id());

                checkCounts(superConsole, mode, 0, 0, 1);
            });

        for (TUid lockedUid : {NO_UID, TEST_UID, TEST_OTHER_UID}) {
            checkSuperConsole(
                conn,
                [&](pqxx::transaction_base& txn, SuperModerationConsole& modConsole)
                {
                    auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, lockedUid);
                    auto result = modConsole.resolveEditTasksByCommitIds(
                        resolution, {0, TEST_COMMIT_ID});
                    UNIT_ASSERT_EQUAL(result.size(), 1);
                    UNIT_ASSERT_EQUAL(*result.begin(), task.id());
                });
        }
    }
}

Y_UNIT_TEST_F(simple_database_super_moderation_console_close, DbFixture)
{
    for (TUid lockedUid : {NO_UID,TEST_UID,TEST_OTHER_UID}) {
        for (auto resolution : {CloseResolution::Approve,
                                CloseResolution::Edit,
                                CloseResolution::Revert}) {

            // not resolved
            checkSuperConsole(
                conn,
                [&](pqxx::transaction_base& txn, SuperModerationConsole& modConsole)
                {
                    auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, lockedUid);

                    Gateway gateway(txn);
                    auto tasks = gateway.loadActiveTasksByTaskIds({task.id()});
                    UNIT_ASSERT_EQUAL(tasks.size(), 1);
                    UNIT_ASSERT(!tasks.front().isClosed());

                    tasks = gateway.loadTasksByTaskIds({task.id()});
                    UNIT_ASSERT_EQUAL(tasks.size(), 1);
                    UNIT_ASSERT(!tasks.front().isClosed());

                    UNIT_ASSERT_EQUAL(modConsole.closeTasksByTaskIds(resolution, {0, task.id()}).size(), 0);
                }
            );
        }

        checkSuperConsole(
            conn,
            [&](pqxx::transaction_base& txn, SuperModerationConsole& modConsole)
            {
                auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, lockedUid);

                Gateway gateway(txn);
                UNIT_ASSERT_EQUAL(gateway.loadActiveEditTasksByCommitIds({TEST_COMMIT_ID}).size(), 1);
                UNIT_ASSERT_EQUAL(gateway.loadEditTasksByCommitIds({TEST_COMMIT_ID}).size(), 1);

                auto result = modConsole.closeEditTasksByCommitIds(
                    {0, TEST_COMMIT_ID}, ResolveResolution::Accept, CloseResolution::Approve);
                UNIT_ASSERT_EQUAL(result.size(), 1);
                UNIT_ASSERT_EQUAL(*result.begin(), task.id());

                auto tasks = gateway.loadActiveTasksByTaskIds({task.id()});
                UNIT_ASSERT_EQUAL(tasks.size(), 0);

                tasks = gateway.loadTasksByTaskIds({task.id()});
                UNIT_ASSERT_EQUAL(tasks.size(), 1);
                UNIT_ASSERT(tasks.front().isClosed());

                UNIT_ASSERT_EQUAL(gateway.loadActiveEditTasksByCommitIds({TEST_COMMIT_ID}).size(), 0);
                UNIT_ASSERT_EQUAL(gateway.loadEditTasksByCommitIds({TEST_COMMIT_ID}).size(), 1);

            }
        );

        for (auto pair : std::map<ResolveResolution, CloseResolution>{
                            {ResolveResolution::Edit,   CloseResolution::Edit},
                            {ResolveResolution::Revert, CloseResolution::Revert} }) {

            checkSuperConsole(
                conn,
                [&](pqxx::transaction_base& txn, SuperModerationConsole& modConsole)
                {
                    auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, lockedUid);
                    auto result = modConsole.closeEditTasksByCommitIds(
                            {0, TEST_COMMIT_ID}, pair.first, pair.second);
                    UNIT_ASSERT_EQUAL(result.size(), 1);
                    UNIT_ASSERT_EQUAL(*result.begin(), task.id());
                });
        }
    }
}

Y_UNIT_TEST_F(events_load, DbFixture)
{
    pqxx::work txn(conn);
    auto task = createCommitTask(txn, TEST_COMMIT_ID, std::nullopt);

    Gateway gateway(txn);
    UNIT_ASSERT_EQUAL(
        gateway.loadEditEventsByCommitIds({TEST_COMMIT_ID + 1}).size(),
        0);

    {
        auto events = gateway.loadEditEventsByCommitIds({TEST_COMMIT_ID});
        UNIT_ASSERT(!events.empty());
        UNIT_ASSERT_EQUAL(events.size(), 1);
        UNIT_ASSERT_EQUAL(events.front().id(), task.event().id());
    }

    {
        auto events = gateway.loadEditEventsByCommitIds(
            {TEST_COMMIT_ID, TEST_COMMIT_ID + 1, TEST_COMMIT_ID});
        UNIT_ASSERT(!events.empty());
        UNIT_ASSERT_EQUAL(events.size(), 1);
        UNIT_ASSERT_EQUAL(events.front().id(), task.event().id());
    }

    {
        const auto now = chrono::TimePoint::clock::now();

        auto events = gateway.loadEditEventsByCreationInterval(DateTimeCondition(now - 48h, now - 24h));
        UNIT_ASSERT(events.empty());
    }

    {
        const auto now = chrono::TimePoint::clock::now();

        auto events = gateway.loadEditEventsByCreationInterval(DateTimeCondition(now - 24h, now));
        UNIT_ASSERT(!events.empty());
        UNIT_ASSERT_EQUAL(events.size(), 1);
        UNIT_ASSERT_EQUAL(events.front().id(), task.event().id());
    }

    {
        UNIT_ASSERT_EQUAL(
            gateway.loadEditEventsByCommitRange(TEST_COMMIT_ID + 1, TEST_COMMIT_ID + 2).size(),
            0);

        auto events = gateway.loadEditEventsByCommitRange(TEST_COMMIT_ID, TEST_COMMIT_ID + 2);
        UNIT_ASSERT(!events.empty());
        UNIT_ASSERT_EQUAL(events.size(), 1);
        UNIT_ASSERT_EQUAL(events.front().id(), task.event().id());
    }

    {
        auto events = gateway.loadEventsByIds({task.event().id()});
        UNIT_ASSERT(!events.empty());
        UNIT_ASSERT_EQUAL(events.size(), 1);
        UNIT_ASSERT_EQUAL(events.front().id(), task.event().id());
    }
}

Y_UNIT_TEST_F(event_extra_data, DbFixture)
{
    pqxx::work txn(conn);
    auto eventWithoutExtraData = EventCreator(txn).create();
    auto eventWithExtraData = EventCreator(txn).extraData({0xace, 0xface}).create();
    txn.commit();

    pqxx::work txn2(conn);
    Gateway gateway(txn2);

    {
        auto events = gateway.loadEditEventsWithExtraDataByCommitIds(
            {eventWithoutExtraData.commitData()->commitId()}
        );
        UNIT_ASSERT(!events.empty());

        UNIT_ASSERT(!events.front().extraData());
    }

    {
        auto events = gateway.loadEditEventsWithExtraDataByCommitIds(
            {eventWithExtraData.commitData()->commitId()}
        );
        UNIT_ASSERT(!events.empty());

        auto extraData = events.front().extraData();
        UNIT_ASSERT(extraData);

        UNIT_ASSERT(extraData->ftTypeId);
        UNIT_ASSERT_EQUAL(*extraData->ftTypeId, 0xace);

        UNIT_ASSERT(extraData->businessRubricId);
        UNIT_ASSERT_EQUAL(*extraData->businessRubricId, 0xface);
    }
}

Y_UNIT_TEST_F(tasks_defer, DbFixture)
{
    pqxx::work txn(conn);
    auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, TEST_UID);
    const auto mode = ModerationMode::Moderator;

    Gateway gw(txn);
    auto superConsole = gw.superModerationConsole(TEST_UID);
    checkCounts(superConsole, mode, 0, 1, 1);

    auto expires = txn.exec("select now() + interval '1 seconds'")[0][0].as<std::string>();
    auto result = superConsole.deferTasksByTaskIds({task.id()}, expires);
    UNIT_ASSERT_EQUAL(result.size(), 1);
    UNIT_ASSERT_EQUAL(*result.begin(), task.id());

    checkCounts(superConsole, mode, 0, 0, 0);

    ::sleep(2);
    // NOW() not modified
    checkCounts(superConsole, mode, 0, 0, 0);
    txn.commit();
    {
        pqxx::work txn(conn); // renew NOW()
        Gateway gw(txn);
        auto superConsole = gw.superModerationConsole(TEST_UID);
        checkCounts(superConsole, mode, 1, 0, 1);
    }
}

Y_UNIT_TEST_F(tasks_release_by_moderator, DbFixture)
{
    // test ordinary moderators
    pqxx::work txn(conn);
    createAndLockCommitTask(txn, TEST_COMMIT_ID, TEST_UID);
    const auto mode = ModerationMode::Moderator;

    Gateway gw(txn);
    auto superConsole = gw.superModerationConsole(TEST_UID);
    checkCounts(superConsole, mode, 0, 1, 1);

    auto modConsole = gw.moderationConsole(TEST_UID);
    modConsole.releaseTasks();
    checkCounts(superConsole, mode, 1, 0, 1);
}

Y_UNIT_TEST_F(tasks_release_by_supervisor, DbFixture)
{
    pqxx::work txn(conn);
    auto task = createAndLockCommitTask(txn, TEST_COMMIT_ID, TEST_UID);
    const auto mode = ModerationMode::Supervisor;

    Gateway gw(txn);
    auto superConsole = gw.superModerationConsole(TEST_UID);
    checkCounts(superConsole, mode, 0, 0, 0);

    superConsole.resolveTasksByTaskIds(ResolveResolution::Accept, {task.id()});
    makeResolvedTimeOlderThanSupervisorDelay(txn, task.id());

    checkCounts(superConsole, mode, 1, 0, 1);

    auto modConsole = gw.moderationConsole(TEST_UID);
    modConsole.acquireTasks({}, LIMIT, TasksOrder::OldestFirst, TEST_MODERATION_TIME_INTERVALS);
    checkCounts(superConsole, mode, 0, 1, 1);

    modConsole.releaseTasks();
    checkCounts(superConsole, mode, 1, 0, 1);
}

Y_UNIT_TEST_F(tasks_object_categories, DbFixture)
{
    pqxx::work txn(conn);
    PrimaryObjectData testObj1(777, "bld", "label1", "notes1");
    auto task1 = createCommitTask(txn, TEST_COMMIT_ID, testObj1);
    PrimaryObjectData testObj2(888, "rd_el", "label2", "notes2");
    auto task2 = createCommitTask(txn, TEST_COMMIT_ID + 1, testObj2);

    Gateway gw(txn);
    auto modConsole = gw.moderationConsole(TEST_UID);

    auto superConsole = gw.superModerationConsole(TEST_UID);
    auto countsByCategoryId = [&]()
    {
        auto counts = superConsole.countsByAoiCategoryId(
            ModerationMode::Moderator, social::EventFilter(), {TEST_AOI_ID}, TEST_MODERATION_TIME_INTERVALS);
        return std::move(counts.taskCounts[TEST_AOI_ID]);
    };
    auto countsByCategoryIdForUser = [&](TUid uid)
    {
        auto counts = superConsole.countsByAoiCategoryId(
            ModerationMode::Moderator, social::EventFilter().createdBy(uid),  {TEST_AOI_ID}, TEST_MODERATION_TIME_INTERVALS);
        return std::move(counts.taskCounts[TEST_AOI_ID]);
    };

    auto counts = countsByCategoryId();
    UNIT_ASSERT_EQUAL(counts.size(), 2);
    UNIT_ASSERT_EQUAL(counts["bld"].available(), 1);
    UNIT_ASSERT_EQUAL(counts["bld"].acquired(), 0);
    UNIT_ASSERT_EQUAL(counts["rd_el"].available(), 1);
    UNIT_ASSERT_EQUAL(counts["rd_el"].acquired(), 0);
    UNIT_ASSERT_EQUAL(countsByCategoryIdForUser(TEST_UID)["rd_el"].available(), 1);
    UNIT_ASSERT_EQUAL(countsByCategoryIdForUser(TEST_UID)["bld"].available(), 1);
    UNIT_ASSERT_EQUAL(countsByCategoryIdForUser(TEST_OTHER_UID)["rd_el"].available(), 0);
    UNIT_ASSERT_EQUAL(countsByCategoryIdForUser(TEST_OTHER_UID)["bld"].available(), 0);

    auto checkNonEmptyTasks = [&](const Tasks& tasks)
    {
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        auto objData = tasks.front().event().primaryObjectData();
        UNIT_ASSERT(objData);
        UNIT_ASSERT_EQUAL(objData->categoryId(), "bld");
        auto counts = countsByCategoryId();
        UNIT_ASSERT_EQUAL(counts.size(), 2);
        UNIT_ASSERT_EQUAL(counts["bld"].available(), 0);
        UNIT_ASSERT_EQUAL(counts["bld"].acquired(), 1);
        UNIT_ASSERT_EQUAL(counts["rd_el"].available(), 1);
        UNIT_ASSERT_EQUAL(counts["rd_el"].acquired(), 0);
    };

    EventFilter filter;
    auto acquireTasks = [&]()
    {
        return modConsole.acquireTasks(filter, 10, TasksOrder::OldestFirst, TEST_MODERATION_TIME_INTERVALS);
    };

    filter.categoryIds({"bld"});
    checkNonEmptyTasks(acquireTasks());
    filter.eventType(EventType::Edit);
    checkNonEmptyTasks(acquireTasks());
    filter.createdBy(TEST_UID);
    checkNonEmptyTasks(acquireTasks());
    filter.objectId(777);
    checkNonEmptyTasks(acquireTasks());
    filter.eventType(EventType::Complaint);
    UNIT_ASSERT_EQUAL(acquireTasks().size(), 0);
    filter.eventType(EventType::RequestForDeletion);
    UNIT_ASSERT_EQUAL(acquireTasks().size(), 0);

    auto checkAllCounts = [&](const CountsByAoiCategoryId& allCounts)
    {
        UNIT_ASSERT_EQUAL(allCounts.taskCounts.size(), 1);
        UNIT_ASSERT_EQUAL(allCounts.taskCounts.begin()->first, TEST_AOI_ID);
        auto counts = allCounts.taskCounts.at(TEST_AOI_ID);
        UNIT_ASSERT_EQUAL(counts.size(), 2);
        UNIT_ASSERT_EQUAL(counts["bld"].available(), 0);
        UNIT_ASSERT_EQUAL(counts["bld"].acquired(), 1);
        UNIT_ASSERT_EQUAL(counts["rd_el"].available(), 1);
        UNIT_ASSERT_EQUAL(counts["rd_el"].acquired(), 0);
    };

    TIds aoiIds{TEST_AOI_ID, TEST_AOI_ID+1};
    checkAllCounts(superConsole.countsByAoiCategoryId(
        ModerationMode::Moderator,
        social::EventFilter(),
        aoiIds,
        TEST_MODERATION_TIME_INTERVALS));
    checkAllCounts(superConsole.countsByAoiCategoryId(
        ModerationMode::Moderator,
        social::EventFilter().eventType(EventType::Edit),
        aoiIds,
        TEST_MODERATION_TIME_INTERVALS));
    UNIT_ASSERT_EQUAL(superConsole.countsByAoiCategoryId(
        ModerationMode::Moderator,
        social::EventFilter().eventType(EventType::Complaint),
        aoiIds,
        TEST_MODERATION_TIME_INTERVALS).taskCounts.size(), 0);
    UNIT_ASSERT_EQUAL(superConsole.countsByAoiCategoryId(
        ModerationMode::Moderator,
        social::EventFilter().eventType(EventType::RequestForDeletion),
        aoiIds,
        TEST_MODERATION_TIME_INTERVALS).taskCounts.size(), 0);
}

Y_UNIT_TEST_F(today_processed_count, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);

    // commitModerationTask below is also created by TEST_UID
    //
    auto commitModerationTaskId = createCommitTask(
        txn,
        TEST_COMMIT_ID,
        std::nullopt
    ).id();

    auto anotherCommitModerationTaskId = createCommitTask(
        txn,
        TEST_COMMIT_OTHER_ID,
        std::nullopt
    ).id();

    tasks::resolveByTaskIdsNoCheckOfLocked(
        txn,
        TEST_OTHER_UID,
        ResolveResolution::Accept,
        {commitModerationTaskId, anotherCommitModerationTaskId}
    );

    makeResolvedTimeOlderThanSupervisorDelay(txn, commitModerationTaskId);
    makeResolvedTimeOlderThanSupervisorDelay(txn, anotherCommitModerationTaskId);

    auto modConsole = gtw.superModerationConsole(TEST_OTHER_UID);

    const int MOSCOW_TZ_MINS = -180;
    auto count = modConsole.todayProcessedCount(MOSCOW_TZ_MINS);
    UNIT_ASSERT_EQUAL(2, count);
}

Y_UNIT_TEST(parse_bounds)
{
    UNIT_ASSERT_NO_EXCEPTION(
            CommitData(TRUNK, 1, "action", "[33.446499885, -90, 33.4465246955, -90]"));
    UNIT_ASSERT_EXCEPTION(
            CommitData(TRUNK, 1, "action", "[33.446499885, -90, 33.4465246955,]"),
            std::exception);
    UNIT_ASSERT(!CommitData(TRUNK, TEST_COMMIT_ID, "action", "").bbox());
    CommitData data(
            0, TEST_COMMIT_ID, "action", "[46.12423009, 51.476361237, 46.124904577, 51.476770414]");
    UNIT_ASSERT(data.bbox());
    UNIT_ASSERT_DOUBLES_EQUAL(data.bbox()->minX(), 5134525.8, 1e-1);
    UNIT_ASSERT_DOUBLES_EQUAL(data.bbox()->minY(), 6672542.7, 1e-1);
}

Y_UNIT_TEST_F(suspicious_feed, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    FeedFilter trivialFilter;
    UNIT_ASSERT_EQUAL(gw.suspiciousFeed(TRUNK, trivialFilter).count(), 0);

    auto eventToFind = EventCreator(txn).uid(TEST_UID).action("commit-reverted").bounds("[37.580, 55.820, 37.583, 55.821]").create();
    auto eventToFilterOff = EventCreator(txn).uid(TEST_UID + 1).action("action").bounds("[37.578, 55.803, 37.578, 55.803]").create();

    FeedFilter userFilter;
    userFilter.createdBy({TEST_UID});
    UNIT_ASSERT_EQUAL(gw.suspiciousFeed(TRUNK, userFilter).count(), 1);

    FeedFilter actionsFilter;
    actionsFilter.actionsAllowed({FeedAction::CommitReverted});
    UNIT_ASSERT_EQUAL(gw.suspiciousFeed(TRUNK, actionsFilter).count(), 1);
}

Y_UNIT_TEST_F(feed_limited_count, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    auto ev1 = EventCreator(txn).action("action").bounds(BOUNDS_1).create();
    auto ev2 = EventCreator(txn).action("action").bounds(BOUNDS_2).create();
    FeedFilter trivialFilter;
    UNIT_ASSERT_EQUAL(gw.suspiciousFeed(TRUNK, trivialFilter).limitedCount(1), 1);
}

Y_UNIT_TEST_F(comments, DbFixture)
{
    using namespace comments;

    const std::string DATA = "'test comment \"abcdd\"' !11";
    const TId TEST_COMMIT_ID = 100500;
    const TId TEST_OBJECT_ID = 12345;
    const TId UNKNOWN_OBJECT_ID = 0;
    const TUid OTHER_TEST_UID = 123;
    const TId AOI1 = 10;
    const TId AOI2 = 20;
    const TId AOI3 = 30;

    pqxx::work txn(conn);
    Gateway gateway(txn);

    auto feedAll = gateway.commentsFeed(CommentsFeedParams());
    UNIT_ASSERT_EQUAL(feedAll.count(), 0);

    auto feedBy = gateway.commentsFeed(CommentsFeedParams().setCreatedBy(TEST_UID));
    UNIT_ASSERT_EQUAL(feedBy.count(), 0);

    auto comment1 = gateway.createComment(
        TEST_UID, CommentType::Warn, DATA,
        TEST_COMMIT_ID, UNKNOWN_OBJECT_ID,
        ANY_FEEDBACK_TASK_ID,
        {AOI1, AOI2}); // aoiIds

    UNIT_ASSERT_EQUAL(comment1.id(), 1); // first
    UNIT_ASSERT_EQUAL(comment1.type(), CommentType::Warn);
    UNIT_ASSERT_EQUAL(comment1.data(), DATA);

    UNIT_ASSERT_EQUAL(comment1.commitId(), TEST_COMMIT_ID);
    UNIT_ASSERT_EQUAL(comment1.objectId(), UNKNOWN_OBJECT_ID);
    UNIT_ASSERT_EQUAL(comment1.feedbackTaskId(), ANY_FEEDBACK_TASK_ID);

    UNIT_ASSERT_EQUAL(comment1.createdBy(), TEST_UID);

    UNIT_ASSERT_EQUAL(feedAll.count(), 1);
    UNIT_ASSERT_EQUAL(feedBy.count(), 1);

    auto feedByOther = gateway.commentsFeed(CommentsFeedParams().setCreatedBy(OTHER_TEST_UID));
    UNIT_ASSERT_EQUAL(feedByOther.count(), 0);

    for (auto aoiId : {AOI1, AOI2, AOI3}) {
        auto feedAoi = gateway.commentsFeed(CommentsFeedParams().setAoiId(aoiId));
        UNIT_ASSERT_EQUAL(feedAoi.count(), aoiId != AOI3 ? 1 : 0);
    }

    UNIT_ASSERT_EQUAL(comment1.deletedBy(), 0);
    UNIT_ASSERT(comment1.deletedAt().empty());
    UNIT_ASSERT(!comment1.data().empty());
    UNIT_ASSERT_EQUAL(comment1.deleteBy(txn, TEST_UID), true);
    UNIT_ASSERT_EQUAL(comment1.deleteBy(txn, TEST_UID), false); // already deleted
    UNIT_ASSERT_EQUAL(comment1.deletedBy(), TEST_UID);
    UNIT_ASSERT(!comment1.deletedAt().empty());

    auto comment2 = gateway.createComment(
        TEST_UID, CommentType::Info, DATA,
        TEST_COMMIT_ID, TEST_OBJECT_ID,
        ANY_FEEDBACK_TASK_ID,
        NO_AOI_IDS);

    auto commentIds = {comment1.id(), comment2.id()};

    UNIT_ASSERT_EQUAL(gateway.loadComments(commentIds).size(), 2);

    auto paramsByObject = CommentsFeedParams()
        .setObjectId(TEST_OBJECT_ID)
        .setTypes(CommentTypes{ CommentType::Info, CommentType::Complaint });
    UNIT_ASSERT_EQUAL(gateway.commentsFeed(paramsByObject).count(), 1);

    gateway.createComment(
        TEST_UID, CommentType::Complaint, DATA,
        TEST_COMMIT_ID, TEST_OBJECT_ID,
        ANY_FEEDBACK_TASK_ID,
        {AOI1});

    paramsByObject.setTypes(ANY_TYPE);
    UNIT_ASSERT_EQUAL(gateway.commentsFeed(paramsByObject).count(), 2);

    {
        // Test feedback_task comment creation
        feedback::GatewayRW gatewayRw(txn);
        auto feedbackTaskId = gatewayRw.addTask(
            TEST_UID,
            feedback::TaskNew(
                geolib3::Point2(0., 0.),
                feedback::Type::Address,
                "source",
                feedback::Description("description")
            )
        ).id();

        auto feedbackComment = gateway.createComment(
            TEST_UID, CommentType::Info, DATA, ANY_COMMIT_ID,
            ANY_OBJECT_ID,
            feedbackTaskId,
            NO_AOI_IDS);

        UNIT_ASSERT_EQUAL(feedbackComment.feedbackTaskId(), feedbackTaskId);

        // Test feedback_task comment feed
        gateway.createComment(
            TEST_UID, CommentType::Info, DATA, ANY_COMMIT_ID, ANY_OBJECT_ID,
            feedbackTaskId,
            NO_AOI_IDS);

        auto paramsForFeedbackTask = CommentsFeedParams().setFeedbackTaskId(feedbackTaskId);
        UNIT_ASSERT_EQUAL(gateway.commentsFeed(paramsForFeedbackTask).count(), 2);
    }

    auto feedByTestUid = gateway.commentsFeed(CommentsFeedParams().setCreatedBy(TEST_UID));
    UNIT_ASSERT_EQUAL(feedByTestUid.count(), 4);
    UNIT_ASSERT(feedByTestUid.commentsHead(2).second == HasMore::Yes);
    UNIT_ASSERT(feedByTestUid.commentsNewer(2, 2).second == HasMore::Yes);
    UNIT_ASSERT_EQUAL(feedByTestUid.commentsNewer(2, 1).first.front().id(), 3);
    UNIT_ASSERT(feedByTestUid.commentsNewer(3, 2).second == HasMore::No);
    UNIT_ASSERT(feedByTestUid.commentsNewer(4, 1).second == HasMore::No);
    UNIT_ASSERT(feedByTestUid.commentsOlder(4, 1).second == HasMore::Yes);
    UNIT_ASSERT_EQUAL(feedByTestUid.commentsOlder(4, 1).first.front().id(), 3);
}

Y_UNIT_TEST_F(clear_user_comments, DbFixture)
{
    using namespace comments;

    const std::string DATA = "'test comment \"abcdd\"' !11";
    const TId TEST_COMMIT_ID = 100500;
    const TId TEST_OBJECT_ID = 12345;
    const TUid OTHER_TEST_UID = 123;

    pqxx::work txn(conn);
    Gateway gateway(txn);

    auto checkComments = [&](const Comments& clearedComments, TIds expectedIds) {
        TIds clearedCommentsIds;
        for (const auto& comment: clearedComments) {
            clearedCommentsIds.insert(comment.id());
        }
        UNIT_ASSERT_EQUAL(
            clearedCommentsIds,
            expectedIds
        );
    };

    auto commentCreatedForObject = gateway.createComment(
        OTHER_TEST_UID, CommentType::Info, DATA,
        TEST_COMMIT_ID, TEST_OBJECT_ID,
        ANY_FEEDBACK_TASK_ID,
        NO_AOI_IDS);

    // Deleting object comments
    //
    checkComments(gateway.clearUserComments(
        OTHER_TEST_UID,
        TEST_UID),
        {commentCreatedForObject.id()});
}

Y_UNIT_TEST_F(tasks_acquire, DbFixture)
{
    pqxx::work txn(conn);
    PrimaryObjectData testObj1(777, "bld", "label1", "notes1");
    auto task1 = createCommitTask(txn, TEST_COMMIT_ID, testObj1);
    PrimaryObjectData testObj2(888, "rd_el", "label2", "notes2");
    auto task2 = createCommitTask(txn, TEST_COMMIT_ID + 1, testObj2);
    PrimaryObjectData testObj3(999, "ad_jc", "label3", "notes3");
    auto task3 = createCommitTask(txn, TEST_COMMIT_ID + 2, testObj3);

    Gateway gw(txn);
    auto modConsole = gw.moderationConsole(TEST_UID);

    auto oldestTasks = modConsole.acquireTasks({}, 3, TasksOrder::OldestFirst, TEST_MODERATION_TIME_INTERVALS);
    UNIT_ASSERT_EQUAL(oldestTasks.size(), 3);
    UNIT_ASSERT_EQUAL(oldestTasks.front().event().primaryObjectData()->id(), task1.event().primaryObjectData()->id());
    UNIT_ASSERT_EQUAL(oldestTasks.back().event().primaryObjectData()->id(), task3.event().primaryObjectData()->id());

    auto newestTasks = modConsole.acquireTasks({}, 3, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS);
    UNIT_ASSERT_EQUAL(newestTasks.size(), 3);
    UNIT_ASSERT_EQUAL(newestTasks.front().event().primaryObjectData()->id(), task3.event().primaryObjectData()->id());
    UNIT_ASSERT_EQUAL(newestTasks.back().event().primaryObjectData()->id(), task1.event().primaryObjectData()->id());
}

Y_UNIT_TEST_F(subscriptions, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    auto console = gw.subscriptionConsole(TEST_UID);
    auto added = console.subscribe(111);
    UNIT_ASSERT_EQUAL(added.feedId(), 111);
    console.subscribe(222);

    auto subscriptions = console.subscriptions();
    UNIT_ASSERT_EQUAL(subscriptions.size(), 2);

    subscriptions.sort([](const Subscription& left, const Subscription& right) {
            return left.feedId() < right.feedId();
    });
    auto subscriptionIt = subscriptions.begin();
    UNIT_ASSERT_EQUAL(subscriptionIt->feedId(), 111);
    ++subscriptionIt;
    UNIT_ASSERT_EQUAL(subscriptionIt->feedId(), 222);

    console.dropSubscription(111);
    UNIT_ASSERT_EQUAL(console.subscriptions().size(), 1);
    UNIT_ASSERT_EXCEPTION(console.dropSubscription(333), SubscriptionNotFound);
    UNIT_ASSERT_EXCEPTION(console.subscribe(222), DuplicateSubscription);
}

Y_UNIT_TEST_F(event_alerts, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    UNIT_ASSERT_EQUAL(gw.loadEventAlerts({}).size(), 0);
    UNIT_ASSERT_EQUAL(gw.loadEventAlerts({111}).size(), 0);
    UNIT_ASSERT_NO_EXCEPTION(gw.saveEventAlerts({}));

    EventAlert alert1(111, 1, "test-description", 123);
    EventAlert alert2(111, 2, "test-description", 123);
    gw.saveEventAlerts({alert1, alert2});

    auto loaded = gw.loadEventAlerts({alert1.eventId(), alert2.eventId()});
    UNIT_ASSERT_EQUAL(loaded.size(), 2);
    UNIT_ASSERT_EQUAL(loaded[0].eventId(), alert1.eventId());
    UNIT_ASSERT_EQUAL(loaded[0].priority(), alert1.priority());
    UNIT_ASSERT_EQUAL(loaded[0].description(), alert1.description());
    UNIT_ASSERT_EQUAL(loaded[0].objectId(), alert1.objectId());

    UNIT_ASSERT_EQUAL(loaded[1].eventId(), alert2.eventId());
    UNIT_ASSERT_EQUAL(loaded[1].priority(), alert2.priority());
    UNIT_ASSERT_EQUAL(loaded[1].description(), alert2.description());
    UNIT_ASSERT_EQUAL(loaded[1].objectId(), alert2.objectId());
}

Y_UNIT_TEST_F(get_user_profiles, DbFixture)
{
    pqxx::work txn(conn);
    ProfileGateway gw(txn);

    UNIT_ASSERT(gw.getUserProfiles({}).empty());

    const TId USER_ID = 555;
    const std::string ABOUT = "test";
    const std::string EMAIL = "test@yandex.ru";
    const bool BROADCAST_SUBSCRIPTION = true;
    const bool NEWS_SUBSCRIPTION = true;
    const bool NEWS_SUBSCRIPTION_WELCOME_MAIL_SENT = true;
    const std::string LOCALE = "tr_TR";

    UNIT_ASSERT(gw.getUserProfiles({USER_ID}).empty());

    ProfileOptionalFields fields;
    fields.about = ABOUT;
    fields.email = EMAIL;
    fields.hasBroadcastSubscription = BROADCAST_SUBSCRIPTION;
    fields.hasNewsSubscription = NEWS_SUBSCRIPTION;
    fields.newsSubscriptionWelcomeMailSent = NEWS_SUBSCRIPTION_WELCOME_MAIL_SENT;
    fields.locale = LOCALE;
    gw.insertProfile(USER_ID, fields);

    auto loaded = gw.getUserProfiles({USER_ID, USER_ID + 1});
    UNIT_ASSERT_EQUAL(loaded.size(), 1);
    const auto& loadedProfile = loaded.front();
    UNIT_ASSERT_EQUAL(loadedProfile.uid(), USER_ID);
    UNIT_ASSERT_EQUAL(loadedProfile.about(), ABOUT);
    UNIT_ASSERT_EQUAL(loadedProfile.email(), EMAIL);
    UNIT_ASSERT_EQUAL(loadedProfile.hasBroadcastSubscription(), BROADCAST_SUBSCRIPTION);
    UNIT_ASSERT_EQUAL(loadedProfile.hasNewsSubscription(), NEWS_SUBSCRIPTION);
    UNIT_ASSERT_EQUAL(loadedProfile.isNewsSubscriptionWelcomeMailSent(), NEWS_SUBSCRIPTION_WELCOME_MAIL_SENT);
    UNIT_ASSERT_UNEQUAL(loadedProfile.newsSubscriptionModifiedAt(), std::nullopt);
    UNIT_ASSERT_EQUAL(loadedProfile.locale(), LOCALE);
}

Y_UNIT_TEST_F(get_all_user_profiles, DbFixture)
{
    pqxx::work txn(conn);
    ProfileGateway gw(txn);

    UNIT_ASSERT(gw.getAllUserProfiles().empty());

    const std::vector<TUid> USER_ID{555, 556};
    const size_t sz = USER_ID.size();
    std::vector<std::string> ABOUT;
    std::vector<std::string> EMAIL;
    std::vector<bool> BROADCAST_SUBSCRIPTION;
    std::vector<bool> NEWS_SUBSCRIPTION;
    std::vector<bool> NEWS_SUBSCRIPTION_WELCOME_MAIL_SENT;
    for (size_t i = 0; i < sz; ++i) {
        ABOUT.push_back(std::to_string(USER_ID[i]) + "test about text.");
        EMAIL.push_back(std::to_string(USER_ID[i]) + "test@yandex.ru");
        BROADCAST_SUBSCRIPTION.push_back(false);
        NEWS_SUBSCRIPTION.push_back(false);
        NEWS_SUBSCRIPTION_WELCOME_MAIL_SENT.push_back(false);
    }
    const std::vector<std::string> LOCALE{sz, "tr_TR"};

    for (size_t i = 0; i < sz; ++i) {
        ProfileOptionalFields fields;
        fields.about = ABOUT[i];
        fields.email = EMAIL[i];
        fields.hasBroadcastSubscription = BROADCAST_SUBSCRIPTION[i];
        fields.hasNewsSubscription = NEWS_SUBSCRIPTION[i];
        fields.newsSubscriptionWelcomeMailSent = NEWS_SUBSCRIPTION_WELCOME_MAIL_SENT[i];
        fields.locale = LOCALE[i];
        gw.insertProfile(USER_ID[i], fields);
    }

    std::unordered_map<TId, Profile> idToProfile;
    {
        const auto profiles = gw.getAllUserProfiles();
        for (const auto& profile: profiles) {
            idToProfile.emplace(profile.uid(), profile);
        }
    }
    UNIT_ASSERT_EQUAL(idToProfile.size(), USER_ID.size());
    for (size_t i = 0; i < sz; ++i) {
        const auto& loadedProfile = idToProfile.at(USER_ID[i]);
        UNIT_ASSERT_EQUAL(loadedProfile.uid(), USER_ID[i]);
        UNIT_ASSERT_EQUAL(loadedProfile.about(), ABOUT[i]);
        UNIT_ASSERT_EQUAL(loadedProfile.email(), EMAIL[i]);
        UNIT_ASSERT_EQUAL(
            loadedProfile.hasBroadcastSubscription(), BROADCAST_SUBSCRIPTION[i]);
        UNIT_ASSERT_EQUAL(
            loadedProfile.hasNewsSubscription(), NEWS_SUBSCRIPTION[i]);
        UNIT_ASSERT_EQUAL(
            loadedProfile.isNewsSubscriptionWelcomeMailSent(),
            NEWS_SUBSCRIPTION_WELCOME_MAIL_SENT[i]);
        UNIT_ASSERT_UNEQUAL(
            loadedProfile.newsSubscriptionModifiedAt(), std::nullopt);
        UNIT_ASSERT_EQUAL(loadedProfile.locale(), LOCALE[i]);
    }
}

Y_UNIT_TEST_F(default_profile_values, DbFixture)
{
    pqxx::work txn(conn);
    ProfileGateway gw(txn);

    const TId USER_ID = 555;

    ProfileOptionalFields fields;
    gw.insertProfile(USER_ID, fields);

    auto loaded = gw.getUserProfiles({USER_ID});
    UNIT_ASSERT_EQUAL(loaded.size(), 1);
    const auto& loadedProfile = loaded.front();
    UNIT_ASSERT_EQUAL(loadedProfile.uid(), USER_ID);

    UNIT_ASSERT_EQUAL(loadedProfile.hasNewsSubscription(), false);
    UNIT_ASSERT_EQUAL(loadedProfile.isNewsSubscriptionWelcomeMailSent(), false);
    UNIT_ASSERT_EQUAL(loadedProfile.newsSubscriptionModifiedAt(), std::nullopt);
}

Y_UNIT_TEST_F(update_news_subscription_value, DbFixture)
{
    pqxx::work txn(conn);
    ProfileGateway gw(txn);

    // Create new profile.
    const TId USER_ID = 555;
    {
        ProfileOptionalFields fields;
        gw.insertProfile(USER_ID, fields);
    }

    // Update news_subscription field.
    const bool NEWS_SUBSCRIPTION = true;
    {
        ProfileOptionalFields fields;
        fields.hasNewsSubscription = NEWS_SUBSCRIPTION;
        gw.updateProfile(USER_ID, fields);
    }

    auto loaded = gw.getUserProfiles({USER_ID, USER_ID + 1});
    UNIT_ASSERT_EQUAL(loaded.size(), 1);
    const auto& loadedProfile = loaded.front();

    UNIT_ASSERT_EQUAL(loadedProfile.uid(), USER_ID);
    UNIT_ASSERT_EQUAL(loadedProfile.hasNewsSubscription(), NEWS_SUBSCRIPTION);
    UNIT_ASSERT_UNEQUAL(loadedProfile.newsSubscriptionModifiedAt(), std::nullopt);
}

Y_UNIT_TEST_F(insert_update_profiles, DbFixture)
{
    pqxx::work txn(conn);
    ProfileGateway gw(txn);

    const TId USER_ID = 1;
    const std::string UPDATED_EMAIL = "updated_email@yandex.ru";
    const std::string INSERTED_ABOUT = "inserted_about";

    ProfileOptionalFields fields1;
    fields1.email = "inserted_email@yandex.ru";
    fields1.about = INSERTED_ABOUT;

    ProfileOptionalFields fields2;
    fields2.email = UPDATED_EMAIL;

    UNIT_ASSERT(!gw.updateProfile(USER_ID, fields1));
    UNIT_ASSERT(gw.getUserProfiles({USER_ID}).empty());

    auto insertedProfile = gw.insertProfile(USER_ID, fields1);
    UNIT_ASSERT_EQUAL(insertedProfile.locale(), "");
    auto updatedProfile = gw.updateProfile(USER_ID, fields2);
    UNIT_ASSERT(updatedProfile);
    UNIT_ASSERT_EQUAL(updatedProfile->email(), UPDATED_EMAIL);
    UNIT_ASSERT_EQUAL(updatedProfile->about(), INSERTED_ABOUT);

    auto profile = gw.getUserProfiles({USER_ID}).at(0);
    UNIT_ASSERT_EQUAL(profile.email(), UPDATED_EMAIL);
    UNIT_ASSERT_EQUAL(profile.about(), INSERTED_ABOUT);

    UNIT_ASSERT_EXCEPTION(gw.insertProfile(USER_ID, fields2), maps::RuntimeError);
}

Y_UNIT_TEST_F(upsert_profiles, DbFixture)
{
    pqxx::work txn(conn);
    ProfileGateway gw(txn);

    const TId USER_ID = 1;
    const std::string LOCALE = "ru";
    ProfileOptionalFields fields;
    fields.locale = LOCALE;

    auto profile = gw.upsertProfile(USER_ID, fields);
    UNIT_ASSERT(profile.email().empty());
    UNIT_ASSERT_EQUAL(profile.locale(), LOCALE);
}

Y_UNIT_TEST_F(internal_comments, DbFixture)
{
    // Data preparation
    const TIds AOI_IDS{};

    pqxx::work txn(conn);
    Gateway gw(txn);

    auto externalComplaint = gw.createComment(
        TEST_UID, CommentType::Complaint, "",
        TEST_COMMIT_ID, TEST_OBJECT_ID, NO_FEEDBACK_ID, AOI_IDS);

    auto internalComplaint = gw.createComment(
        TEST_UID, CommentType::Complaint, "",
        TEST_COMMIT_ID, TEST_OBJECT_ID, NO_FEEDBACK_ID, AOI_IDS, Comment::Internal::Yes);

    // Check that internal flags for comments are set correctly.
    {
        UNIT_ASSERT(externalComplaint.internal() == Comment::Internal::No);
        UNIT_ASSERT(internalComplaint.internal() == Comment::Internal::Yes);
    }

    // Check that the comments feed is correctly filtered by the internal flag.
    {
        auto checkFeed =
            [&](const std::optional<Comment::Internal>& internalFilter,
                const std::vector<Comment::Internal>& expectedCommentTypes)
            {
                using namespace comments;

                auto params = CommentsFeedParams().setInternal(internalFilter);

                CommentsFeed feed = gw.commentsFeed(params);
                UNIT_ASSERT_EQUAL(feed.count(), expectedCommentTypes.size());

                size_t expCommentTypeIx{0};
                for (const auto& comment: feed.commentsHead(feed.count()).first) {
                    UNIT_ASSERT(comment.internal() == expectedCommentTypes[expCommentTypeIx++]);
                }
            };

        checkFeed(comments::ANY_INTERNAL, {Comment::Internal::Yes, Comment::Internal::No});
        checkFeed(Comment::Internal::No,  {Comment::Internal::No});
        checkFeed(Comment::Internal::Yes, {Comment::Internal::Yes});
    }
}

Y_UNIT_TEST_F(creation_of_entries_for_suspicious_users, DbFixture)
{
    using UIds = std::vector<uint64_t>;

    {
        pqxx::work txn(conn);
        Gateway gw(txn);
        gw.createTask(EventCreator(txn).uid(1), 25_hours_ago);
        gw.createTask(EventCreator(txn).uid(10), 23_hours_ago);
        txn.commit();
        UNIT_ASSERT(suspiciousUsersIds() == UIds({10}));
    }

    {
        pqxx::work txn(conn);
        Gateway gw(txn);
        gw.createAcceptedTask(EventCreator(txn).uid(2), 25_hours_ago);
        gw.createAcceptedTask(EventCreator(txn).uid(20), 23_hours_ago);
        txn.commit();
        UNIT_ASSERT(suspiciousUsersIds() == UIds({10, 20}));
    }
}

Y_UNIT_TEST_F(getting_active_users, DbFixture)
{
    pqxx::work txn(conn);

    auto now = std::chrono::system_clock::now();
    chrono::TimePoint activityBegin = now - 12h;
    chrono::TimePoint activityEnd = now - 1h;

    // Skip not interesting columns
    txn.exec("ALTER TABLE social.commit_event ALTER COLUMN action DROP NOT NULL");
    txn.exec("ALTER TABLE social.commit_event ALTER COLUMN branch_id DROP NOT NULL");
    txn.exec("ALTER TABLE social.commit_event ALTER COLUMN commit_id DROP NOT NULL");

    // Fill events and tasks
    txn.exec(
        "INSERT INTO social.commit_event "
        "(event_id, created_by, created_at, type) VALUES "
        // Users with just one event
        "(1, 1, NOW() - '1 day'::interval, 'edit'),"        // outside the interval
        "(2, 2, NOW() - '7 hours'::interval, 'edit'),"      // inside the interval
        "(3, 3, NOW() - '3 hours'::interval, 'complaint')," // inside the interval, but not an `edit`
        // User 4 - active with several events
        "(4, 4, NOW() - '6 hours'::interval, 'edit'),"      // inside the interval
        "(5, 4, NOW(), 'edit'),"                            // outside the interval
        // User 5 - active with several events
        "(6, 5, NOW() - '2 hours'::interval, 'edit'),"                 // inside the interval
        "(7, 5, NOW() - '5 hours'::interval, 'request-for-deletion')," // inside the interval, but not an `edit`
        // User 6 - inactive with several events
        "(8, 6, NOW() - '13 hours'::interval, 'edit'),"     // ouside the interval
        "(9, 6, NOW() - '8 hours'::interval, 'complaint')," // inside the interval, but not an `edit`
        // Commit without task
        "(10, 7, NOW() - '7 hours'::interval, 'edit');"
    );

    txn.exec(
        // Just copy data from the table above
        "INSERT INTO social.task "
        "(event_id, created_at) "
        "(SELECT event_id, created_at FROM social.commit_event);"

        // Remove the task for the commit without task
        "DELETE FROM social.task WHERE event_id = 10;"
    );

    auto activeUsers = Gateway(txn).getActiveUserIds(activityBegin, activityEnd);
    std::sort(activeUsers.begin(), activeUsers.end());

    const std::vector<TUid> expected = {2, 4, 5};
    UNIT_ASSERT_EQUAL(activeUsers, expected);
}

Y_UNIT_TEST_F(updating_skills, DbFixture)
{
    // Skip not interesting columns and drop not interesting constraints
    pqxx::work setupTxn(conn);
    setupTxn.exec("ALTER TABLE social.commit_event ALTER COLUMN action DROP NOT NULL");
    setupTxn.exec("ALTER TABLE social.commit_event ALTER COLUMN branch_id DROP NOT NULL");
    setupTxn.exec("ALTER TABLE social.commit_event ALTER COLUMN commit_id DROP NOT NULL");
    setupTxn.exec("ALTER TABLE social.commit_event ALTER COLUMN created_at DROP NOT NULL");
    setupTxn.exec("ALTER TABLE social.commit_event DROP CONSTRAINT check_primary_object");
    setupTxn.exec("ALTER TABLE social.task ALTER COLUMN commit_id DROP NOT NULL");
    setupTxn.exec("ALTER TABLE social.task ALTER COLUMN created_at DROP NOT NULL");
    setupTxn.commit();

    {   // Should consider all resolution types
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit'),"
            "(2, 2, 'bld', 'edit'),"
            "(3, 3, 'bld', 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', 'accept'),"
            "(2, 'bld', 'edit', 'revert'),"
            "(3, 'bld', 'edit', 'edit');"
        );

        Gateway(txn).updateSkills({1, 2, 3});
        UNIT_ASSERT(
            queryHelpers.compareQueries(
                txn,
                "SELECT * FROM social.skills ORDER BY uid",
                "VALUES "
                "(1, 'bld', 'accept', 1),"
                "(2, 'bld', 'revert', 1),"
                "(3, 'bld', 'edit', 1)"
            )
        );
    }

    {   // Should update an entry
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.skills VALUES "
            "(1, 'bld', 'accept', 100);"

            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', 'accept');"
        );

        Gateway(txn).updateSkills({1, 2, 3});
        UNIT_ASSERT(
            queryHelpers.compareQueries(
                txn,
                "SELECT * FROM social.skills",
                "VALUES (1, 'bld', 'accept', 1)"
            )
        );
    }

    {   // Should group by users, category_groups and resolutions
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit'),"
            "(2, 1, 'bld', 'edit'),"
            "(3, 1, 'addr', 'edit'),"
            "(4, 1, 'addr', 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', 'accept'),"
            "(2, 'bld', 'edit', 'accept'),"
            "(3, 'addr', 'edit', 'revert'),"
            "(4, 'addr', 'edit', 'edit');"
        );

        Gateway(txn).updateSkills({1});
        UNIT_ASSERT(
            queryHelpers.compareQueries(
                txn,
                "SELECT * FROM social.skills ORDER BY category_id, resolve_resolution",
                "VALUES "
                "(1, 'addr', 'edit', 1),"
                "(1, 'addr', 'revert', 1),"
                "(1, 'bld', 'accept', 2)"
            )
        );
    }

    {   // Should consider edits only
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit'),"
            "(2, 2, 'bld', 'complaint'),"
            "(3, 3, 'bld', 'request-for-deletion');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', 'accept'),"
            "(2, 'bld', 'complaint', 'accept'),"
            "(3, 'bld', 'request-for-deletion', 'accept');"
        );

        Gateway(txn).updateSkills({1, 2, 3});
        UNIT_ASSERT(
            queryHelpers.compareQueries(
                txn,
                "SELECT * FROM social.skills",
                "VALUES (1, 'bld', 'accept', 1)"
            )
        );
    }

    {   // Should consider tasks only
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit'),"
            "(2, 2, 'bld', 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', 'accept');"
        );

        Gateway(txn).updateSkills({1, 2, 3});
        UNIT_ASSERT(
            queryHelpers.compareQueries(
                txn,
                "SELECT * FROM social.skills",
                "VALUES (1, 'bld', 'accept', 1)"
            )
        );
    }

    {   // Should not consider own reverts and resolves (applicable for experts)
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit'),"
            "(2, 2, 'bld', 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolved_by, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', 1, 'accept'),"
            "(2, 'bld', 'edit', 2, 'revert');"
        );

        Gateway(txn).updateSkills({1, 2});
        UNIT_ASSERT(
            txn.exec("SELECT * FROM social.skills").empty()
        );
    }

    {   // Should update certain users only
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit'),"
            "(2, 2, 'bld', 'edit'),"
            "(3, 3, 'bld', 'edit'),"
            "(4, 4, 'bld', 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', 'accept'),"
            "(2, 'bld', 'edit', 'accept'),"
            "(3, 'bld', 'edit', 'accept'),"
            "(4, 'bld', 'edit', 'accept');"
        );

        Gateway(txn).updateSkills({2, 4});
        UNIT_ASSERT(
            queryHelpers.compareQueries(
                txn,
                "SELECT uid FROM social.skills ORDER BY uid",
                "VALUES (2), (4)"
            )
        );
    }

    {   // Should not consider tasks without resolution
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, 'bld', 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, 'bld', 'edit', NULL);"
        );

        Gateway(txn).updateSkills({1});
        UNIT_ASSERT(
            txn.exec("SELECT * FROM social.skills").empty()
        );
    }

    {   // Should not consider tasks without category group
        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.commit_event "
            "(event_id, created_by, primary_object_category_id, type) VALUES "
            "(1, 1, NULL, 'edit');"

            "INSERT INTO social.task "
            "(event_id, primary_object_category_id, type, resolve_resolution) VALUES "
            "(1, NULL, 'edit', 'accept');"
        );

        Gateway(txn).updateSkills({1});
        UNIT_ASSERT(
            txn.exec("SELECT * FROM social.skills").empty()
        );
    }
}

Y_UNIT_TEST_F(are_there_edit_events_created_by, DbFixture)
{
    pqxx::work txn(conn);
    TIds commitIds;
    std::map<TUid, TId> RecentCommitIdByUid;

    Gateway gw(txn);
    UNIT_ASSERT(!gw.getRecentEditCommitMadeBy(1, commitIds));
    UNIT_ASSERT(!gw.getRecentEditCommitMadeBy(2, commitIds));

    commitIds.insert(EventCreator(txn).uid(1).create().commitData()->commitId());
    RecentCommitIdByUid[1] = *commitIds.rbegin();
    UNIT_ASSERT_EQUAL(*gw.getRecentEditCommitMadeBy(1, commitIds), RecentCommitIdByUid[1]);
    UNIT_ASSERT(!gw.getRecentEditCommitMadeBy(2, commitIds));

    commitIds.insert(EventCreator(txn).uid(1).create().commitData()->commitId());
    RecentCommitIdByUid[1] = *commitIds.rbegin();
    UNIT_ASSERT_EQUAL(*gw.getRecentEditCommitMadeBy(1, commitIds), RecentCommitIdByUid[1]);
    UNIT_ASSERT(!gw.getRecentEditCommitMadeBy(2, commitIds));

    commitIds.insert(EventCreator(txn).uid(2).create().commitData()->commitId());
    RecentCommitIdByUid[2] = *commitIds.rbegin();
    UNIT_ASSERT_EQUAL(*gw.getRecentEditCommitMadeBy(1, commitIds), RecentCommitIdByUid[1]);
    UNIT_ASSERT_EQUAL(*gw.getRecentEditCommitMadeBy(2, commitIds), RecentCommitIdByUid[2]);

    commitIds.insert(EventCreator(txn).uid(1).create().commitData()->commitId());
    RecentCommitIdByUid[1] = *commitIds.rbegin();
    UNIT_ASSERT_EQUAL(*gw.getRecentEditCommitMadeBy(1, commitIds), RecentCommitIdByUid[1]);
    UNIT_ASSERT_EQUAL(*gw.getRecentEditCommitMadeBy(2, commitIds), RecentCommitIdByUid[2]);
}

Y_UNIT_TEST_F(should_acquire_by_commit_ids, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);

    const std::array allTasks{
        gw.createTask(EventCreator(txn).uid(1), 0_hours_ago),
        gw.createTask(EventCreator(txn).uid(1), 1_hours_ago),
        gw.createTask(EventCreator(txn).uid(2), 2_hours_ago),
        gw.createTask(EventCreator(txn).uid(1), 3_hours_ago)
    };

    const TIds commitIds{
        allTasks[1].commitId(),
        allTasks[2].commitId(),
        allTasks[3].commitId()
    };

    const auto result = gw
        .moderationConsole(TEST_UID)
        .acquireTasks(
            EventFilter().commitIds(commitIds).createdBy(1),
            LIMIT,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

    UNIT_ASSERT(getIds(result) == TIds({allTasks[1].id(), allTasks[3].id()}));
}

Y_UNIT_TEST_F(should_acquire_by_resolved_by, DbFixture)
{
    const TId MODERATOR_135 = 135;
    const TId MODERATOR_4   = 4;

    pqxx::work txn(conn);
    Gateway gw(txn);

    const std::array allTasks{
        gw.createTask(EventCreator(txn), 0_hours_ago),
        gw.createTask(EventCreator(txn), 1_hours_ago),
        gw.createTask(EventCreator(txn), 2_hours_ago),
        gw.createTask(EventCreator(txn), 3_hours_ago),
        gw.createTask(EventCreator(txn), 4_hours_ago),
        gw.createTask(EventCreator(txn), 5_hours_ago)
    };

    lockCommitTask(txn, allTasks[1].commitId(), MODERATOR_135);
    lockCommitTask(txn, allTasks[3].commitId(), MODERATOR_135);
    lockCommitTask(txn, allTasks[5].commitId(), MODERATOR_135);
    gw.superModerationConsole(MODERATOR_135).resolveTasksByTaskIds(
        ResolveResolution::Accept, {allTasks[1].id(), allTasks[5].id()}
    );
    gw.superModerationConsole(MODERATOR_135).resolveTasksByTaskIds(
        ResolveResolution::Edit, {allTasks[3].id()}
    );
    gw.moderationConsole(MODERATOR_135).releaseTasks();

    lockCommitTask(txn, allTasks[4].commitId(), MODERATOR_4);
    gw.superModerationConsole(MODERATOR_4).resolveTasksByTaskIds(
        ResolveResolution::Revert, {allTasks[4].id()}
    );
    gw.moderationConsole(MODERATOR_4).releaseTasks();

    // Try to acquire tasks filtered by moderators
    auto result = gw
        .moderationConsole(TEST_UID)
        .acquireTasks(
            EventFilter().resolvedBy(MODERATOR_135),
            LIMIT,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

    UNIT_ASSERT(getIds(result) == TIds({allTasks[1].id(), allTasks[3].id(), allTasks[5].id()}));
    gw.moderationConsole(TEST_UID).releaseTasks();

    result = gw
        .moderationConsole(TEST_UID)
        .acquireTasks(
            EventFilter().resolvedBy(MODERATOR_4),
            LIMIT,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);

    UNIT_ASSERT(getIds(result) == TIds({allTasks[4].id()}));
}

Y_UNIT_TEST_F(should_reacquire_all_tasks_for_specific_filters, DbFixture)
{
    {   // commitIds filter
        pqxx::work txn(conn);
        Gateway gw(txn);

        const std::array allTasks{
            gw.createTask(EventCreator(txn), 0_hours_ago),
            gw.createTask(EventCreator(txn), 1_hours_ago),
            gw.createTask(EventCreator(txn), 2_hours_ago)
        };

        const TIds commitIds{
            allTasks[0].commitId(),
            allTasks[2].commitId()
        };

        const TIds expectedTaskIds{{allTasks[0].id(), allTasks[2].id()}};

        auto result = gw
            .moderationConsole(TEST_UID)
            .acquireTasks(EventFilter().commitIds(commitIds), LIMIT, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT(getIds(result) == expectedTaskIds);

        tasks::release(txn, TEST_UID, {allTasks[2].id()});

        result = gw
            .moderationConsole(TEST_UID)
            .acquireTasks(EventFilter().commitIds(commitIds), LIMIT, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT(getIds(result) == expectedTaskIds);
    }

    {   // objectId filter
        pqxx::work txn(conn);
        Gateway gw(txn);

        const auto BLD_ID   = 1;
        const auto RD_EL_ID = 2;

        PrimaryObjectData objBld (BLD_ID,    "bld",   "bld_label",   "bld_notes");
        PrimaryObjectData objRdEl(RD_EL_ID, "rd_el", "rd_el_label", "rd_el_notes");

        const std::array allTasks{
            gw.createTask(EventCreator(txn).primaryObjData(objBld),  0_hours_ago),
            gw.createTask(EventCreator(txn).primaryObjData(objRdEl), 1_hours_ago),
            gw.createTask(EventCreator(txn).primaryObjData(objBld),  2_hours_ago)
        };

        const TIds expectedTaskIds{{allTasks[0].id(), allTasks[2].id()}};

        auto result = gw
            .moderationConsole(TEST_UID)
            .acquireTasks(EventFilter().objectId(BLD_ID), LIMIT, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT(getIds(result) == expectedTaskIds);

        tasks::release(txn, TEST_UID, {allTasks[2].id()});

        result = gw
            .moderationConsole(TEST_UID)
            .acquireTasks(EventFilter().objectId(BLD_ID), LIMIT, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT(getIds(result) == expectedTaskIds);
    }

    {   // createdBy filter
        pqxx::work txn(conn);
        Gateway gw(txn);

        const std::array allTasks{
            gw.createTask(EventCreator(txn).uid(1), 0_hours_ago),
            gw.createTask(EventCreator(txn).uid(1), 1_hours_ago),
            gw.createTask(EventCreator(txn).uid(2), 2_hours_ago)
        };

        const TIds expectedTaskIds{{allTasks[0].id(), allTasks[1].id()}};

        auto result = gw
            .moderationConsole(TEST_UID)
            .acquireTasks(EventFilter().createdBy(1), LIMIT, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT(getIds(result) == expectedTaskIds);

        tasks::release(txn, TEST_UID, {allTasks[0].id()});

        result = gw
            .moderationConsole(TEST_UID)
            .acquireTasks(EventFilter().createdBy(1), LIMIT, TasksOrder::NewestFirst, TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT(getIds(result) == expectedTaskIds);
    }
}

Y_UNIT_TEST_F(should_acquire_tasks_according_to_moderation_mode, DbFixture)
{
    {
        pqxx::work txn(conn);

        const std::array tasks{
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval")(),
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval").resolved(2, ResolveResolution::Accept, "NOW() - '10 min'::interval")(),
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval").resolved(2, ResolveResolution::Accept, "NOW()")(),
            TaskCreator(txn).createdAt("NOW()")()
        };

        const auto result = Gateway(txn).moderationConsole(TEST_UID).acquireTasks(
            EventFilter().moderationMode(ModerationMode::Moderator),
            LIMIT,
            TasksOrder::OldestFirst,
            TEST_MODERATION_TIME_INTERVALS
        );

        UNIT_ASSERT_EQUAL(result.size(), 2);
        UNIT_ASSERT_EQUAL(result[0].id(), tasks[0].id());
        UNIT_ASSERT_EQUAL(result[1].id(), tasks[3].id());
    }

    {
        pqxx::work txn(conn);

        const std::array tasks{
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval")(),
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval").resolved(2, ResolveResolution::Accept, "NOW() - '10 min'::interval")(),
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval").resolved(2, ResolveResolution::Accept, "NOW()")(),
            TaskCreator(txn).createdAt("NOW()")()
        };

        const auto result = Gateway(txn).moderationConsole(TEST_UID).acquireTasks(
            EventFilter().moderationMode(ModerationMode::SuperModerator),
            LIMIT,
            TasksOrder::OldestFirst,
            TEST_MODERATION_TIME_INTERVALS
        );

        UNIT_ASSERT_EQUAL(result.size(), 2);
        UNIT_ASSERT_EQUAL(result[0].id(), tasks[0].id());
        UNIT_ASSERT_EQUAL(result[1].id(), tasks[1].id());
    }

    {
        pqxx::work txn(conn);

        const std::array tasks{
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval")(),
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval").resolved(2, ResolveResolution::Accept, "NOW() - '10 min'::interval")(),
            TaskCreator(txn).createdAt("NOW() - '2 days'::interval").resolved(2, ResolveResolution::Accept, "NOW()")(),
            TaskCreator(txn).createdAt("NOW()")()
        };

        const auto result = Gateway(txn).moderationConsole(TEST_UID).acquireTasks(
            EventFilter().moderationMode(ModerationMode::Supervisor),
            LIMIT,
            TasksOrder::OldestFirst,
            TEST_MODERATION_TIME_INTERVALS
        );

        UNIT_ASSERT_EQUAL(result.size(), 2);
        UNIT_ASSERT_EQUAL(result[0].id(), tasks[0].id());
        UNIT_ASSERT_EQUAL(result[1].id(), tasks[1].id());
    }
}

}

} // namespace maps::wiki::social::tests
