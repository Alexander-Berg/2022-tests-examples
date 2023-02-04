#include <yandex/maps/wiki/social/feedback/agent.h>
#include <yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <yandex/maps/wiki/social/feedback/task_aoi.h>
#include <yandex/maps/wiki/social/feedback/partition.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/social/feedback/util.h>

namespace maps::wiki::social::feedback::tests {

using namespace unittest;

namespace {

const TId TASK_ID = 10;
const TId AOI_ID = 12;
const TId AOI_ID_1 = 13;
const TId AOI_ID_2 = 14;
const TUid SOME_USER_ID = 1000;

const std::string SOME_SOURCE = "some-source";
const std::string SOME_DESCR = "some-descr";

struct AoiFeedback
{
    TId aoiId;
    TId feedbackTaskId;
};

bool operator == (const AoiFeedback& lhs, const AoiFeedback& rhs)
{
    return lhs.aoiId == rhs.aoiId &&
           lhs.feedbackTaskId == rhs.feedbackTaskId;
}

std::vector<AoiFeedback> selectFromAoiFeed(
    pqxx::transaction_base& socialTxn,
    Partition partition)
{
    std::stringstream query;
    query << "SELECT * FROM " << aoiFeedbackFeedPartitionTable(partition);

    std::vector<AoiFeedback> res;
    for (const auto& row : socialTxn.exec(query.str())) {
        res.push_back(AoiFeedback{
            row[sql::col::AOI_ID].as<TId>(),
            row[sql::col::FEEDBACK_TASK_ID].as<TId>()
        });
    }
    return res;
}

} // namespace anonymous

Y_UNIT_TEST_SUITE(feedback_feed_tests) {

Y_UNIT_TEST_F(add_to_pending_partition, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    TId taskId = TASK_ID;

    TIds aoiIds {AOI_ID_1};
    addTaskToAoiFeed(socialTxn, taskId, aoiIds, Partition::Pending);

    auto pendingFeed =
        selectFromAoiFeed(socialTxn, Partition::Pending);
    auto outgoingOpenedFeed =
        selectFromAoiFeed(socialTxn, Partition::OutgoingOpened);
    auto outgoingClosedFeed =
        selectFromAoiFeed(socialTxn, Partition::OutgoingClosed);

    UNIT_ASSERT_VALUES_EQUAL(pendingFeed.size(), 1);
    UNIT_ASSERT_EQUAL(pendingFeed, (std::vector<AoiFeedback>{{AOI_ID_1, TASK_ID}}));
    UNIT_ASSERT(outgoingOpenedFeed.empty());
    UNIT_ASSERT(outgoingClosedFeed.empty());
}

Y_UNIT_TEST_F(multiple_aois, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    TId taskId = TASK_ID;
    TIds aoiIds {AOI_ID_1, AOI_ID_2};

    addTaskToAoiFeed(socialTxn, taskId, aoiIds, Partition::Pending);

    auto feed = selectFromAoiFeed(socialTxn, Partition::Pending);

    UNIT_ASSERT_VALUES_EQUAL(feed.size(), 2);
    UNIT_ASSERT(std::find(feed.begin(), feed.end(),
        AoiFeedback{AOI_ID_1, TASK_ID}) != feed.end());
    UNIT_ASSERT(std::find(feed.begin(), feed.end(),
        AoiFeedback{AOI_ID_2, TASK_ID}) != feed.end());
}

Y_UNIT_TEST_F(calc_aois_empty, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    auto counters = calcAoiTaskCounters(
        socialTxn, {}, TaskFilter(), Partition::OutgoingOpened);

    UNIT_ASSERT(counters.empty());
}

Y_UNIT_TEST_F(calc_aois_absent_in_feed, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    auto counters = calcAoiTaskCounters(
        socialTxn, {AOI_ID}, TaskFilter(), Partition::OutgoingOpened);

    UNIT_ASSERT_VALUES_EQUAL(counters.size(), 1);
    UNIT_ASSERT(counters.count(AOI_ID));

    const auto& taskCouters = counters.at(AOI_ID);
    UNIT_ASSERT_VALUES_EQUAL(taskCouters.old, 0);
    UNIT_ASSERT_VALUES_EQUAL(taskCouters.total, 0);
}

Y_UNIT_TEST_F(calc_aois_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();
    GatewayRW gatewayRw(socialTxn);

    auto task = gatewayRw.addTask(SOME_USER_ID, TaskNew(
        geolib3::Point2(0, 0), Type::Address, SOME_SOURCE, SOME_DESCR));

    addTaskToAoiFeed(socialTxn, task.id(), {AOI_ID}, Partition::Pending);

    auto counters = calcAoiTaskCounters(socialTxn, {AOI_ID},
         TaskFilter().type(Type::Barrier), Partition::Pending);

    const auto& taskCouters = counters.at(AOI_ID);
    UNIT_ASSERT_VALUES_EQUAL(taskCouters.old, 0);
    UNIT_ASSERT_VALUES_EQUAL(taskCouters.total, 0);
}

Y_UNIT_TEST_F(calc_aois, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();
    GatewayRW gatewayRw(socialTxn);

    auto task = gatewayRw.addTask(SOME_USER_ID, TaskNew(
        geolib3::Point2(0, 0), Type::Address, SOME_SOURCE, SOME_DESCR));

    addTaskToAoiFeed(socialTxn, task.id(), {AOI_ID}, Partition::Pending);

    auto counters = calcAoiTaskCounters(
        socialTxn, {AOI_ID}, TaskFilter(), Partition::Pending);

    const auto& taskCouters = counters.at(AOI_ID);
    UNIT_ASSERT_VALUES_EQUAL(taskCouters.old, 0);
    UNIT_ASSERT_VALUES_EQUAL(taskCouters.total, 1);
}

Y_UNIT_TEST_F(calc_aois_stat, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();
    Agent agent(socialTxn, SOME_USER_ID);

    {
        auto task = agent.addTask(TaskNew(
            geolib3::Point2(0, 0), Type::Address, SOME_SOURCE, SOME_DESCR));

        addTaskToAoiFeed(socialTxn, task.id(), {AOI_ID}, Partition::OutgoingOpened);
        agent.revealTaskByIdCascade(task.id());
    }
    {
        TaskNew newTask(
            geolib3::Point2(0, 0), Type::Address, SOME_SOURCE, SOME_DESCR);
        newTask.hidden = true;

        auto task = agent.addTask(newTask);
        addTaskToAoiFeed(socialTxn, task.id(), {AOI_ID}, Partition::OutgoingOpened);
        agent.revealTaskByIdCascade(task.id());
    }
    {
        TaskNew newTask(
            geolib3::Point2(0, 0), Type::Building, SOME_SOURCE, SOME_DESCR);
        newTask.hidden = true;

        auto task = agent.addTask(newTask);
        addTaskToAoiFeed(socialTxn, task.id(), {AOI_ID}, Partition::OutgoingOpened);
        agent.revealTaskByIdCascade(task.id());
    }
    refreshAoiTaskStatMv(socialTxn);

    auto counters = calcAoiOpenedTaskStatCounters(
        socialTxn, {AOI_ID}, AoiTaskFilter());

    const auto& taskCounters = counters.at(AOI_ID);
    UNIT_ASSERT_VALUES_EQUAL(taskCounters.old, 0);
    UNIT_ASSERT_VALUES_EQUAL(taskCounters.total, 3);
    UNIT_ASSERT_VALUES_EQUAL(taskCounters.totalHidden, 2);

    auto countersBuilding = calcAoiOpenedTaskStatCounters(
        socialTxn, {AOI_ID}, AoiTaskFilter().types(Types{Type::Building}));
    const auto& taskCountersBuilding = countersBuilding.at(AOI_ID);
    UNIT_ASSERT_VALUES_EQUAL(taskCountersBuilding.old, 0);
    UNIT_ASSERT_VALUES_EQUAL(taskCountersBuilding.total, 1);
    UNIT_ASSERT_VALUES_EQUAL(taskCountersBuilding.totalHidden, 1);
}

namespace {

TId addTaskWithTimeLag(
    pqxx::transaction_base& socialTxn,
    Type type,
    unsigned hoursBeforeNow,
    TId aoiId)
{
    Agent agent(socialTxn, SOME_USER_ID);

    auto task = agent.addTask(TaskNew(
        geolib3::Point2(0, 0), type, SOME_SOURCE, SOME_DESCR));

    execUpdateTasks(socialTxn,
        sql::col::CREATED_AT + " = now() - '" +
            std::to_string(hoursBeforeNow) + " hours'::interval",
        sql::col::ID + " = " + std::to_string(task.id()));

    addTaskToAoiFeed(socialTxn, task.id(), {aoiId}, Partition::OutgoingOpened);
    agent.revealTaskByIdCascade(task.id());
    refreshAoiOldestTaskMv(socialTxn);
    return task.id();
}

} // namespace anonymous

Y_UNIT_TEST_F(aois_oldest_task_none, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    auto aoiToTask = calcAoiOldestOpenedTask(
        socialTxn, {AOI_ID}, AoiTaskFilter());

    UNIT_ASSERT(aoiToTask.count(AOI_ID));
    const auto& task = aoiToTask.at(AOI_ID);
    UNIT_ASSERT(task == std::nullopt);
}

Y_UNIT_TEST_F(aois_oldest_task, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    TId prevTaskId = addTaskWithTimeLag(socialTxn, Type::Address, 1, AOI_ID);
    addTaskWithTimeLag(socialTxn, Type::Address, 0, AOI_ID);

    auto aoiToTask = calcAoiOldestOpenedTask(
        socialTxn, {AOI_ID}, AoiTaskFilter());

    UNIT_ASSERT(aoiToTask.count(AOI_ID));

    const auto& task = aoiToTask.at(AOI_ID);
    UNIT_ASSERT(task != std::nullopt);
    UNIT_ASSERT(task->id() == prevTaskId);
}

Y_UNIT_TEST_F(aois_oldest_task_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    addTaskWithTimeLag(socialTxn, Type::Address, 1, AOI_ID);
    TId entranceTaskId = addTaskWithTimeLag(socialTxn, Type::Entrance, 0, AOI_ID);

    auto aoiToTask = calcAoiOldestOpenedTask(socialTxn, {AOI_ID},
        AoiTaskFilter().types(Types{Type::Entrance}));

    UNIT_ASSERT(aoiToTask.count(AOI_ID));

    const auto& task = aoiToTask.at(AOI_ID);
    UNIT_ASSERT(task != std::nullopt);
    UNIT_ASSERT(task->id() == entranceTaskId);
}

Y_UNIT_TEST_F(aois_oldest_task_two_regions, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    TId taskId1 = addTaskWithTimeLag(socialTxn, Type::Address, 2, AOI_ID_1);
    TId taskId2 = addTaskWithTimeLag(socialTxn, Type::Address, 1, AOI_ID_2);

    auto aoiToTask = calcAoiOldestOpenedTask(socialTxn, {AOI_ID_1, AOI_ID_2},
        AoiTaskFilter());

    UNIT_ASSERT(aoiToTask.count(AOI_ID_1));
    UNIT_ASSERT(aoiToTask.count(AOI_ID_2));

    UNIT_ASSERT(aoiToTask.at(AOI_ID_1) != std::nullopt);
    UNIT_ASSERT(aoiToTask.at(AOI_ID_1)->id() == taskId1);

    UNIT_ASSERT(aoiToTask.at(AOI_ID_2) != std::nullopt);
    UNIT_ASSERT(aoiToTask.at(AOI_ID_2)->id() == taskId2);
}

namespace {

std::vector<TId> createAoiFeedPending(
   pqxx::transaction_base& socialTxn,
   int taskNum)
{
    GatewayRW gatewayRw(socialTxn);
    std::vector<TId> taskIds;
    for (int i = 0; i < taskNum; i++) {
        auto task = gatewayRw.addTask(SOME_USER_ID, TaskNew(
            geolib3::Point2(0, 0), Type::Address,
            SOME_SOURCE, SOME_DESCR));

        addTaskToAoiFeed(socialTxn,
            task.id(), {AOI_ID}, Partition::Pending);

        taskIds.push_back(task.id());
    }
    return taskIds;
}

struct FixtureWithFeedOf4Tasks : unittest::ArcadiaDbFixture
{
    pgpool3::TransactionHandle socialTxnHandle;
    pqxx::transaction_base& socialTxn;
    TId taskId1;
    TId taskId2;
    TId taskId3;
    TId taskId4;

    FixtureWithFeedOf4Tasks() :
        socialTxnHandle(pool().masterWriteableTransaction()),
        socialTxn(socialTxnHandle.get())
    {
        auto tasksIds = createAoiFeedPending(socialTxn, 4);
        taskId1 = tasksIds.at(0);
        taskId2 = tasksIds.at(1);
        taskId3 = tasksIds.at(2);
        taskId4 = tasksIds.at(3);
    }
};

const int PAGE_SIZE = 2;

} // namespace anonymous

Y_UNIT_TEST_F(aoi_feed_before_and_after, FixtureWithFeedOf4Tasks)
{
    UNIT_ASSERT_EXCEPTION(
        aoiTaskFeed(
            socialTxn,
            AOI_ID,
            TaskFeedParamsId(taskId1, taskId4, PAGE_SIZE, TasksOrder::OldestFirst),
            TaskFilter(),
            {Partition::OutgoingOpened}
        ),
        maps::RuntimeError);
}

Y_UNIT_TEST_F(aoi_feed_before, FixtureWithFeedOf4Tasks)
{
    auto feed = aoiTaskFeed(
        socialTxn,
        AOI_ID,
        TaskFeedParamsId(taskId4, 0, PAGE_SIZE, TasksOrder::OldestFirst),
        TaskFilter(),
        {Partition::Pending}
    );

    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.size(), PAGE_SIZE);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(0).id(), taskId2);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(1).id(), taskId3);
    UNIT_ASSERT(feed.hasMore == HasMore::Yes);
}

Y_UNIT_TEST_F(aoi_feed_after, FixtureWithFeedOf4Tasks)
{
    auto feed = aoiTaskFeed(
        socialTxn,
        AOI_ID,
        TaskFeedParamsId(0, taskId1, PAGE_SIZE, TasksOrder::OldestFirst),
        TaskFilter(),
        {Partition::Pending}
    );

    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.size(), PAGE_SIZE);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(0).id(), taskId2);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(1).id(), taskId3);
    UNIT_ASSERT(feed.hasMore == HasMore::Yes);
}

Y_UNIT_TEST_F(aoi_feed_not_before_and_not_after, FixtureWithFeedOf4Tasks)
{
    auto feed = aoiTaskFeed(
        socialTxn,
        AOI_ID,
        TaskFeedParamsId(0, 0, PAGE_SIZE, TasksOrder::OldestFirst),
        TaskFilter(),
        {Partition::Pending}
    );

    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.size(), PAGE_SIZE);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(0).id(), taskId1);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(1).id(), taskId2);
    UNIT_ASSERT(feed.hasMore == HasMore::Yes);
}

Y_UNIT_TEST_F(aoi_feed_has_more_false, FixtureWithFeedOf4Tasks)
{
    auto feed = aoiTaskFeed(
        socialTxn,
        AOI_ID,
        TaskFeedParamsId(0, taskId2, PAGE_SIZE, TasksOrder::OldestFirst),
        TaskFilter(),
        {Partition::Pending}
    );

    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.size(), PAGE_SIZE);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(0).id(), taskId3);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(1).id(), taskId4);
    UNIT_ASSERT(feed.hasMore == HasMore::No);
}

Y_UNIT_TEST_F(aoi_feed_filter, FixtureWithFeedOf4Tasks)
{
    auto feed = aoiTaskFeed(
        socialTxn,
        AOI_ID,
        TaskFeedParamsId(0, taskId2, PAGE_SIZE, TasksOrder::OldestFirst),
        TaskFilter().source("very rare source"),
        {Partition::Pending}
    );

    UNIT_ASSERT(feed.tasks.empty());
    UNIT_ASSERT(feed.hasMore == HasMore::No);
}

Y_UNIT_TEST_F(aoi_feed_no_partitions, FixtureWithFeedOf4Tasks)
{
    UNIT_ASSERT_EXCEPTION(
        aoiTaskFeed(
            socialTxn,
            AOI_ID,
            TaskFeedParamsId(0, 0, PAGE_SIZE, TasksOrder::OldestFirst),
            TaskFilter(),
            {}
        ),
        LogicError);
}

Y_UNIT_TEST_F(aoi_feed_two_partitions, FixtureWithFeedOf4Tasks)
{
    Agent agent(socialTxn, SOME_USER_ID);

    agent.revealTaskByIdCascade(taskId3);
    agent.revealTaskByIdCascade(taskId1);

    auto feed = aoiTaskFeed(
        socialTxn,
        AOI_ID,
        TaskFeedParamsId(0, 0, 4, TasksOrder::OldestFirst),
        TaskFilter(),
        {Partition::Pending, Partition::OutgoingOpened}
    );

    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.size(), 4);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(0).id(), taskId1);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(1).id(), taskId2);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(2).id(), taskId3);
    UNIT_ASSERT_VALUES_EQUAL(feed.tasks.at(3).id(), taskId4);
}

Y_UNIT_TEST_F(tasks_aois, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    // clang-format off
    TaskIdToAoiIds expected{
        {TId(1), TIds()},
        {TId(2), TIds{1}},
        {TId(3), TIds{1, 2}}
    };
    // clang-format on

    TIds taskIds;
    for (const auto& [taskId, aoiIds]: expected) {
        addTaskToAoiFeed(socialTxn, taskId, aoiIds, Partition::Pending);
        taskIds.insert(taskId);
    }

    auto result = getTasksAoiIds(socialTxn, taskIds);

    UNIT_ASSERT(result == expected);
}

Y_UNIT_TEST_F(tasks_aois_for_zero_tasks, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    TIds taskIds;
    auto result = getTasksAoiIds(socialTxn, taskIds);

    UNIT_ASSERT_VALUES_EQUAL(result.size(), 0);
}

} // Y_UNIT_TEST_SUITE(feedback_feed_tests)

} // namespace maps::wiki::social::feedback::tests
