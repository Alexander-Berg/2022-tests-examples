#include <library/cpp/testing/unittest/registar.h>
#include <maps/libs/geolib/include/conversion.h>
#include <yandex/maps/wiki/common/robot.h>
#include <yandex/maps/wiki/social/exception.h>
#include <yandex/maps/wiki/social/feedback/agent.h>
#include <yandex/maps/wiki/social/feedback/commits.h>
#include <yandex/maps/wiki/social/feedback/description.h>
#include <yandex/maps/wiki/social/feedback/description_keys.h>
#include <yandex/maps/wiki/social/feedback/duplicates.h>
#include <yandex/maps/wiki/social/feedback/enums.h>
#include <yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <yandex/maps/wiki/social/feedback/history_patch.h>
#include <yandex/maps/wiki/social/feedback/task.h>
#include <yandex/maps/wiki/social/feedback/task_aoi.h>
#include <yandex/maps/wiki/social/feedback/task_filter.h>
#include <yandex/maps/wiki/social/feedback/task_patch.h>
#include <yandex/maps/wiki/social/feedback/twins.h>
#include <yandex/maps/wiki/social/feedback/types_back_compatibility.h>

#include "feedback_helpers.h"
#include "helpers.h"
#include <maps/wikimap/mapspro/libs/social/magic_strings.h>
#include <maps/wikimap/mapspro/libs/social/feedback/util.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/fb_task_creator.h>

#include <array>
#include <sstream>

template <>
void Out<maps::wiki::social::TIds>(
    IOutputStream& os,
    const maps::wiki::social::TIds& ids)
{
    std::ostringstream ostr;
    ostr << maps::wiki::common::join(ids, ",");
    os << ostr.str();
}

namespace maps::wiki::social::tests {

using namespace feedback;
using feedback::tests::FbTaskCreator;

namespace {

const feedback::Type TYPE = Type::RoadClosure;
const std::string SRC = "some-source";
const std::string SRC_FEEDBACK = "fbapi";
const std::string SRC_1 = "some-source-1";
const std::string SRC_2 = "some-source-2";
const std::string SRC_FBAPI = "fbapi-samsara";

Task createTaskTemplate(GatewayRW& gatewayRw)
{
    return gatewayRw.addTask(
        USER_ID,
        TaskNew(
            ZERO_POSITION,
            Type::Other,
            SRC,
            DESCR()
        )
    );
}

struct DuplicatesSimpleCluster
{
    TaskForUpdate head;
    TaskForUpdate duplicate0, duplicate1;
};

//    task1     {Incoming}
//    ^   ^
//    |   |
// task0 task2  {Incoming}
DuplicatesSimpleCluster createDuplicatesTemplate(GatewayRW& gatewayRw)
{
    auto task0 = createTaskTemplate(gatewayRw);
    auto task1 = createTaskTemplate(gatewayRw);
    auto task2 = createTaskTemplate(gatewayRw);

    auto head = bindDuplicates(gatewayRw, USER_ID, TIds({task0.id(), task1.id(), task2.id()}));
    UNIT_ASSERT(head);
    auto tasks = gatewayRw.tasksForUpdateByFilter(TaskFilter().duplicateHeadId(head->id()));
    UNIT_ASSERT(tasks.size() == 2);

    return DuplicatesSimpleCluster{*head, tasks[0], tasks[1]};
}

bool isRevealed(const Task& task)
{
    return task.bucket() == Bucket::Outgoing;
}

bool isResolved(const Task& task, TUid uid, Resolution resolution)
{
    return task.resolved()
            && task.resolved()->uid == uid
            && task.resolved()->resolution == resolution;
}

bool notResolved(const Task& task)
{
    return !task.resolved();
}

bool isDeployed(const Task& task)
{
    return !!task.deployedAt();
}

bool notDeployed(const Task& task)
{
    return !isDeployed(task);
}

std::optional<social::feedback::RejectReason> getRejectReason(
    const HistoryItem& historyItem)
{
    using social::feedback::RejectReason;

    std::optional<RejectReason> reason;
    if (historyItem.params().count(HISTORY_PARAM_REJECT_REASON)) {
        reason = enum_io::fromString<RejectReason>(
            historyItem.params().at(HISTORY_PARAM_REJECT_REASON));
    }
    return reason;
}

void checkLastHistoryItem(
    const History& history,
    size_t requiredItemsSize,
    const HistoryItem& sampleForLast)
{
    UNIT_ASSERT(requiredItemsSize > 0);
    const auto& items = history.items();
    UNIT_ASSERT_EQUAL(items.size(), requiredItemsSize);
    const auto& item = items[requiredItemsSize - 1];
    UNIT_ASSERT_EQUAL(item.modifiedBy(), sampleForLast.modifiedBy());
    UNIT_ASSERT_EQUAL(item.operation(), sampleForLast.operation());
    UNIT_ASSERT_EQUAL(item.params(), sampleForLast.params());

    const auto latestRejectReason = getRejectReason(item);
    const auto sampleRejectReason = getRejectReason(sampleForLast);
    UNIT_ASSERT_EQUAL(latestRejectReason, sampleRejectReason);
    if (latestRejectReason && sampleRejectReason) {
        UNIT_ASSERT_EQUAL(*latestRejectReason, *sampleRejectReason);
    }
}

HistoryItem createHistoryItem(
    TUid uid,
    TaskOperation operation,
    HistoryItemParams params = HistoryItemParams())
{
    return HistoryItem(
        chrono::TimePoint::clock::now(),
        uid,
        operation,
        params,
        std::nullopt
    );
}

void checkDescriptionInOut(
    const Description& descr,
    GatewayRW& gatewayRw)
{
    auto task = gatewayRw.addTask(USER_ID,
        TaskNew(ZERO_POSITION, TYPE, SRC, descr));

    UNIT_ASSERT(descr == task.description());
}

const TUid SOME_USER_UID = 111;
const std::string SOME_SOURCE = "some_source";
const std::string SOME_DESCR = "some_description";

Task createNewTask(Agent& agent)
{
    TaskNew newTask(ZERO_POSITION, TYPE, SRC_FEEDBACK, DESCR());
    return agent.addTask(newTask);
}

Task createOldTask(pqxx::transaction_base& txn)
{
    GatewayRW gatewayRw(txn);
    TaskNew newTask(ZERO_POSITION, TYPE, SRC_FEEDBACK, DESCR());
    auto task = gatewayRw.addTask(USER_ID, newTask);

    execUpdateTasks(txn,
        sql::col::CREATED_AT + " = now() - '25 hours'::interval",
        sql::col::ID + " = " + std::to_string(task.id()));

    auto updatedTask = gatewayRw.taskById(task.id());
    ASSERT(updatedTask);
    return std::move(*updatedTask);
}

Task createHypothesis(Agent& agent)
{
    TaskNew newTask(ZERO_POSITION, TYPE, SRC, DESCR());
    return agent.addTask(newTask);
}

std::vector<Column> allAggregationColumns()
{
    std::vector<Column> retVal;
    enumerateValues(retVal);
    return retVal;
}

using FieldValue = std::string;
using GroupByFields = std::map<Column, FieldValue>;

GroupByFields getGroupByFieldValues(const AggregatedCounter& counter)
{
    GroupByFields retVal;
    for (auto col: allAggregationColumns()) {
        // Will not complile if new value is added to Column;
        switch (col) {
            case Column::Workflow:
                if (counter.workflow.has_value()) {
                    retVal[col] = std::string(toString(counter.workflow.value()));
                }
                break;
            case Column::Source:
                if (counter.source.has_value()) {
                    retVal[col] = counter.source.value();
                }
                break;
            case Column::Type:
                if (counter.type.has_value()) {
                    retVal[col] = std::string(toString(counter.type.value()));
                }
                break;
        }
    }

    return retVal;
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(feedback_tasks) {

Y_UNIT_TEST_F(update_feedback_tasks, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);
    Agent agentRobot(txn, common::ROBOT_UID);
    TId taskId = 0;

    size_t expectedHistoryItemsCount = 0;

    {   // create task
        TaskNew newTask(
            ZERO_POSITION,
            TYPE,
            SRC_FBAPI,
            DESCR());
        newTask.attrs.addCustom("nmaps_link", "https://yandex.ru");
        newTask.attrs.addCustom("strange attr", "2'2'''");
        newTask.objectId = 15;

        auto task = agentRobot.addTask(newTask);
        taskId = task.id();

        UNIT_ASSERT(!task.acquired());
        UNIT_ASSERT(!task.resolved());
        UNIT_ASSERT(!task.hidden());
        UNIT_ASSERT_EQUAL(task.processingLevel(), 0);
        UNIT_ASSERT_EQUAL(task.type(), TYPE);
        UNIT_ASSERT_EQUAL(task.bucket(), Bucket::Incoming);
        UNIT_ASSERT_EQUAL(task.source(), SRC_FBAPI);
        UNIT_ASSERT(task.description() == DESCR());
        UNIT_ASSERT(task.attrs().existCustom("nmaps_link"));
        UNIT_ASSERT_EQUAL(task.attrs().getCustom("nmaps_link"), "https://yandex.ru");
        UNIT_ASSERT(task.attrs().existCustom("strange attr"));
        UNIT_ASSERT_EQUAL(task.attrs().getCustom("strange attr"), "2'2'''");
        UNIT_ASSERT(task.objectId());
        UNIT_ASSERT_EQUAL(*task.objectId(), 15);
        UNIT_ASSERT_EQUAL(task.viewedBy().size(), 0);
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(common::ROBOT_UID, TaskOperation::Create));
    }
    {   // change bucket
        auto task = gatewayRw.updateTaskById(taskId,
            TaskPatch(common::ROBOT_UID).setBucket(Bucket::Deferred));
        UNIT_ASSERT(task);
        UNIT_ASSERT_EQUAL(task->bucket(), Bucket::Deferred);
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(common::ROBOT_UID, TaskOperation::Defer));
    }
    {   // reveal task
        auto outgoingTask = agentRobot.revealTaskByIdCascade(taskId);
        UNIT_ASSERT(outgoingTask);
        UNIT_ASSERT_EQUAL(outgoingTask->bucket(), Bucket::Outgoing);
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(common::ROBOT_UID, TaskOperation::Reveal));
    }
    {   // mark viewed
        auto viewedTask = agentRobot.markViewedTask(getTaskForUpdate(agentRobot, taskId));
        UNIT_ASSERT_EQUAL(viewedTask->viewedBy(), TUids{common::ROBOT_UID});
        UNIT_ASSERT_EQUAL(agentRobot.gatewayRo().history(taskId).items().size(), expectedHistoryItemsCount);
    }
    {   // hide task
        Agent agent(txn, 1001);
        auto hiddenTask = agent.hideTask(getTaskForUpdate(agent, taskId));
        UNIT_ASSERT(hiddenTask);
        UNIT_ASSERT(hiddenTask->hidden());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1001, TaskOperation::Hide));
    }
    {   // acquire task
        Agent agent(txn, 1003);
        auto taskAfterAcquire = agent.acquireTask(getTaskForUpdate(agent, taskId));
        UNIT_ASSERT(taskAfterAcquire);
        UNIT_ASSERT(!taskAfterAcquire->resolved());
        UNIT_ASSERT_EQUAL(taskAfterAcquire->type(), TYPE);
        UNIT_ASSERT(taskAfterAcquire->acquired());
        UNIT_ASSERT_EQUAL(taskAfterAcquire->acquired()->uid, 1003);
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1003, TaskOperation::Acquire));
    }
    {   // release (==de-acquire) task
        Agent agent(txn, 1003);
        auto task = agent.releaseTask(getTaskForUpdate(agent, taskId));
        UNIT_ASSERT(task);
        UNIT_ASSERT(!task->resolved());
        UNIT_ASSERT_EQUAL(task->type(), TYPE);
        UNIT_ASSERT(!task->acquired());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1003, TaskOperation::Release));
    }
    {   // reject task
        Agent agent(txn, 1004);
        auto taskAfterResolve = agent.resolveTaskCascade(
            getTaskForUpdate(agent, taskId),
            Resolution::createRejected(RejectReason::Spam));
        UNIT_ASSERT(taskAfterResolve);
        UNIT_ASSERT(taskAfterResolve->resolved());
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->uid, 1004);
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->resolution,
            Resolution::createRejected(RejectReason::Spam));
        HistoryItemParams params{
            {HISTORY_PARAM_REJECT_REASON, std::string(toString(RejectReason::Spam))}
        };
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1004, TaskOperation::Reject, params));
    }
    {   // open task
        Agent agent(txn, 1005);
        auto reopenedTask = agent.openTask(getTaskForUpdate(agent, taskId));
        UNIT_ASSERT(reopenedTask);
        UNIT_ASSERT(!reopenedTask->resolved());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1005, TaskOperation::Open));
    }
    {   // processing level up for task
        Agent agent(txn, 1015);
        auto task = agent.processingLevelUp(getTaskForUpdate(agent, taskId), Verdict::Rejected);
        UNIT_ASSERT(task);
        UNIT_ASSERT_EQUAL(task->processingLevel(), 1);
        HistoryItemParams params{
            {HISTORY_PARAM_NEW_PROCESSING_LVL, std::string(toString(ProcessingLvl::Level1))},
            {HISTORY_PARAM_SUGGESTED_VERDICT, std::string(toString(Verdict::Rejected))},
        };
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(
                1015,
                TaskOperation::ChangeProcessingLvl,
                std::move(params)));
    }
    {   // processing level up
        // Maximum processing level reached.
        // agent.processingLevelUp() returns std::nullopt (because of imposible operation)
        // , so use gatewayRw.updateTask()
        Agent agent(txn, 1016);
        auto task = gatewayRw.updateTask(
            getTaskForUpdate(agent, taskId),
            TaskPatch(1016).setProcessingLvl(ProcessingLvl::Level1, Verdict::Rejected));
        UNIT_ASSERT_EQUAL(task.processingLevel(), 1);
        UNIT_ASSERT_EQUAL(agentRobot.gatewayRo().history(taskId).items().size(), expectedHistoryItemsCount);
    }
    {   // processing level down
        Agent agent(txn, 1014);
        auto task = agent.processingLevelDown(getTaskForUpdate(agent, taskId));
        UNIT_ASSERT(task);
        UNIT_ASSERT_EQUAL(task->processingLevel(), 0);
        HistoryItemParams params{
            {HISTORY_PARAM_NEW_PROCESSING_LVL, std::string(toString(ProcessingLvl::Level0))},
        };
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(
                1014,
                TaskOperation::ChangeProcessingLvl,
                std::move(params)));
    }
    {   // processing level down beneath limit
        // minimum processing level reached
        // agent.processingLevelDown() returns std::nullopt (because of imposible operation)
        // , so use gatewayRw.updateTask()
        Agent agent(txn, 1013);
        auto task = gatewayRw.updateTask(
            getTaskForUpdate(agent, taskId),
            TaskPatch(1016).setProcessingLvl(ProcessingLvl::Level0));
        UNIT_ASSERT_EQUAL(task.processingLevel(), 0);
        UNIT_ASSERT_EQUAL(agentRobot.gatewayRo().history(taskId).items().size(), expectedHistoryItemsCount);
    }
    {   // reject with reason
        Agent agent(txn, 1004);
        const auto resolution = Resolution::createRejected(RejectReason::NoInfo);
        auto taskAfterResolve = agent.resolveTaskCascade(
            getTaskForUpdate(agent, taskId),
            resolution);
        UNIT_ASSERT(taskAfterResolve);
        UNIT_ASSERT(taskAfterResolve->resolved());
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->uid, 1004);
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->resolution, resolution);
        HistoryItemParams params{
            {HISTORY_PARAM_REJECT_REASON, std::string(toString(*resolution.rejectReason()))}
        };
        checkLastHistoryItem(
            agentRobot.gatewayRo().history(taskId),
            ++expectedHistoryItemsCount,
            createHistoryItem(
                1004, TaskOperation::Reject, params));
    }
    {   // Change reject reason NoProcess -> Spam.
        Agent agent(txn, 1004);
        const auto resolution = Resolution::createRejected(RejectReason::Spam);
        auto taskAfterResolve = agent.resolveTaskCascade(
            getTaskForUpdate(agent, taskId),
            resolution);
        UNIT_ASSERT(taskAfterResolve);
        UNIT_ASSERT(taskAfterResolve->resolved());
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->uid, 1004);
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->resolution, resolution);
        HistoryItemParams params{
            {HISTORY_PARAM_REJECT_REASON, std::string(toString(*resolution.rejectReason()))}
        };
        checkLastHistoryItem(
            agentRobot.gatewayRo().history(taskId),
            ++expectedHistoryItemsCount,
            createHistoryItem(
                1004, TaskOperation::Reject, params));
    }
    {   // open task
        Agent agent(txn, 1005);
        auto reopenedTask = agent.openTask(getTaskForUpdate(agent, taskId));
        UNIT_ASSERT(reopenedTask);
        UNIT_ASSERT(!reopenedTask->resolved());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1005, TaskOperation::Open));
    }
    {   // reject acquired task
        Agent agent(txn, 1004);
        agent.acquireTask(getTaskForUpdate(agent, taskId));
        ++expectedHistoryItemsCount;
        auto taskAfterResolve = agent.resolveTaskCascade(
            getTaskForUpdate(agent, taskId),
            Resolution::createRejected(RejectReason::ProhibitedByRules));
        expectedHistoryItemsCount += 2; // Release & reject
        UNIT_ASSERT(taskAfterResolve);
        UNIT_ASSERT(taskAfterResolve->resolved());
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->uid, 1004);
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->resolution,
            Resolution::createRejected(RejectReason::ProhibitedByRules));
        UNIT_ASSERT(!taskAfterResolve->acquired());
        UNIT_ASSERT_EQUAL(agentRobot.gatewayRo().history(taskId).items().size(), expectedHistoryItemsCount);
    }
    {   // open task
        Agent agent(txn, 1005);
        auto reopenedTask = agent.openTask(getTaskForUpdate(agent, taskId));
        UNIT_ASSERT(reopenedTask);
        UNIT_ASSERT(!reopenedTask->resolved());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1005, TaskOperation::Open));
    }
    {   // change task type
        auto updatedTask = agentRobot.changeTaskType(
            getTaskForUpdate(agentRobot, taskId),
            Type::Road);
        UNIT_ASSERT(updatedTask);
        UNIT_ASSERT_EQUAL(updatedTask->type(), Type::Road);
        checkLastHistoryItem(
            agentRobot.gatewayRo().history(taskId),
            ++expectedHistoryItemsCount,
            createHistoryItem(
                common::ROBOT_UID,
                TaskOperation::ChangeType,
                HistoryItemParams{
                    {HISTORY_PARAM_OLD_TYPE, std::string(toString(TYPE))},
                    {HISTORY_PARAM_NEW_TYPE, std::string(toString(Type::Road))}
                }
            )
        );
    }
    static const geolib3::Point2 SOME_POSITION_GEO(30, 40);
    static const geolib3::Point2 SOME_POSITION_MERC
        = geolib3::convertGeodeticToMercator(SOME_POSITION_GEO);
    static const std::set<uint64_t> AOI_IDS_FOR_OLD_POSITION {11, 22};
    static const std::set<uint64_t> AOI_IDS_FOR_NEW_POSITION {444, 555};
    {   // add task->aoi data
        addTaskToAoiFeed(txn, taskId, AOI_IDS_FOR_OLD_POSITION, Partition::OutgoingOpened);
    }
    {   // change task for released task is forbidden
        auto updatedTask = agentRobot.changeTaskPosition(
            getTaskForUpdate(agentRobot, taskId),
            SOME_POSITION_MERC,
            AOI_IDS_FOR_NEW_POSITION);
        UNIT_ASSERT(!updatedTask);
    }
    {   // change task position (for acquired task)
        Agent agent(txn, 1008);
        agent.acquireTask(getTaskForUpdate(agent, taskId));
        ++expectedHistoryItemsCount;
        auto updatedTask = agent.changeTaskPosition(
            getTaskForUpdate(agentRobot, taskId),
            SOME_POSITION_MERC,
            AOI_IDS_FOR_NEW_POSITION);
        UNIT_ASSERT(updatedTask);
        UNIT_ASSERT_DOUBLES_EQUAL(updatedTask->position().x(), SOME_POSITION_MERC.x(), 1e-7);
        UNIT_ASSERT_DOUBLES_EQUAL(updatedTask->position().y(), SOME_POSITION_MERC.y(), 1e-7);
        {
            auto tasksAoiIds = getTasksAoiIds(txn, {taskId});
            UNIT_ASSERT(tasksAoiIds.contains(taskId));
            const TIds& aoiIds = tasksAoiIds.at(taskId);
            UNIT_ASSERT_EQUAL(aoiIds, AOI_IDS_FOR_NEW_POSITION);
        }
        checkLastHistoryItem(
            agentRobot.gatewayRo().history(taskId),
            expectedHistoryItemsCount += 2,  // release & change-type
            createHistoryItem(
                1008,
                TaskOperation::ChangePosition,
                HistoryItemParams{
                    {HISTORY_PARAM_OLD_POSITION, "0.000000000,0.000000000"},
                    {HISTORY_PARAM_NEW_POSITION, "30.000000000,40.000000000"}
                }
            )
        );
    }
    {   // accept task
        Agent agent(txn, 1006);
        auto taskAfterResolve = agent.resolveTaskCascade(
            getTaskForUpdate(agent, taskId),
            Resolution::createAccepted());
        UNIT_ASSERT(taskAfterResolve);
        UNIT_ASSERT(taskAfterResolve->resolved());
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->uid, 1006);
        UNIT_ASSERT_EQUAL(taskAfterResolve->resolved()->resolution, Resolution::createAccepted());
        UNIT_ASSERT(!taskAfterResolve->deployedAt());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(1006, TaskOperation::Accept));
    }
    {   // release resolved task
        Agent agent(txn, 1007);
        agent.releaseTask(getTaskForUpdate(agent, taskId));
        auto releasedTask = gatewayRw.taskById(taskId);
        UNIT_ASSERT(releasedTask);
        UNIT_ASSERT(!releasedTask->acquired());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), expectedHistoryItemsCount,
            createHistoryItem(1006, TaskOperation::Accept));
    }
    {   // deploy task
        auto deployedTask = agentRobot.deployTaskByIdCascade(
            taskId,
            std::chrono::system_clock::now());
        UNIT_ASSERT(deployedTask);
        UNIT_ASSERT(deployedTask->deployedAt());
        checkLastHistoryItem(agentRobot.gatewayRo().history(taskId), ++expectedHistoryItemsCount,
            createHistoryItem(common::ROBOT_UID, TaskOperation::Deploy));
    }
}

Y_UNIT_TEST_F(description, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    Description descrCheckMulti = DescriptionI18n(
        tanker::fb_desc::ADDR_CORRECTION_KEY,
        {
            {tanker::CORRECT_STREET, Description("a")},
            {tanker::CORRECT_HOUSE, Description("b")}
        }
    );

    checkDescriptionInOut(descrCheckMulti, gatewayRw);

    Description descrCheckEmpty(DescriptionI18n(tanker::fb_desc::ADDRESS_UNKNOWN_KEY, {}));
    checkDescriptionInOut(descrCheckEmpty, gatewayRw);

    Description descrCheckRaw(R"(".a")");
    checkDescriptionInOut(descrCheckRaw, gatewayRw);
}

Y_UNIT_TEST_F(valid_reject_reasons, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);

    {
        auto taskNew = TaskNew(ZERO_POSITION, SOME_TYPE, "fbapi", DESCR());
        auto task = agent.revealTaskByIdCascade(agent.addTask(taskNew).id());
        UNIT_ASSERT(task);

        UNIT_ASSERT_VALUES_EQUAL(Agent::validRejectReasons(*task).size(), 21);
    }
    {
        auto taskNew = TaskNew(ZERO_POSITION, SOME_TYPE, "not-fbapi", DESCR());
        auto task = agent.revealTaskByIdCascade(agent.addTask(taskNew).id());
        UNIT_ASSERT(task);

        UNIT_ASSERT_VALUES_EQUAL(Agent::validRejectReasons(*task).size(), 2);
    }
}

Y_UNIT_TEST_F(create, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    // cut latitude by 85 on add
    {
        auto task = gatewayRw.addTask(
            USER_ID,
            TaskNew(
                geolib3::convertGeodeticToMercator(geolib3::Point2(89., 89.)),
                TYPE,
                SRC,
                DESCR()
            )
        );
        auto geoPosition = geolib3::convertMercatorToGeodetic(task.position());
        UNIT_ASSERT_DOUBLES_EQUAL(geoPosition.x(), 89., 1e-7);
        UNIT_ASSERT_DOUBLES_EQUAL(geoPosition.y(), 84.999, 1e-7);
    }
}

Y_UNIT_TEST_F(filter_deployed, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);

    {
        auto task = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                DESCR()
            )
        );
        agent.revealTaskByIdCascade(task.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, task.id()),
            Resolution::createAccepted());
    }

    {
        auto task = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                DESCR()
            )
        );
        agent.revealTaskByIdCascade(task.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, task.id()),
            Resolution::createRejected(RejectReason::NoData));
    }

    {
        auto task = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                DESCR()
            )
        );
        agent.revealTaskByIdCascade(task.id());
        agent.resolveTaskCascade(getTaskForUpdate(agent, task.id()),
            Resolution::createRejected(RejectReason::NoData));
        agent.deployTaskByIdCascade(task.id(), std::chrono::system_clock::now());
    }

    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.resolved(true);
        filter.deployed(false);
        filter.verdict(Verdict::Accepted);

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
    }
}

Y_UNIT_TEST_F(filter_processing_level, DbFixture)
{
    std::vector<TId> taskIds;

    // create 2 tasks
    {
        pqxx::work txn(conn);
        Agent agent(txn, USER_ID);

        for (size_t i = 0; i < 2; ++i) {
            taskIds.emplace_back(createRevealedTask(agent));
        }
        txn.commit();
    }

    // modify tasks
    {
        pqxx::work txn(conn);
        Agent agent(txn, USER2_ID);

        agent.processingLevelUp(
            getTaskForUpdate(agent, taskIds.at(1)),
            Verdict::Rejected);
        txn.commit();
    }

    // check TaskFilter
    pqxx::work txn(conn);
    GatewayRO gatewayRo(txn);
    {
        auto tasks = gatewayRo.tasksByFilter(TaskFilter());
        UNIT_ASSERT_EQUAL(
            getIds(tasks),
            (TIds({taskIds.at(0), taskIds.at(1)})));
    }
    {
        auto result = gatewayRo.tasksBriefByFilter(
            TaskFilter().processingLvls({{ProcessingLvl::Level0, ProcessingLvl::Level1}}),
            std::nullopt);
        auto& tasks = result.tasks;
        UNIT_ASSERT_EQUAL(
            getIds(tasks),
            (TIds({taskIds.at(0), taskIds.at(1)})));
    }
    {
        auto tasks = gatewayRo.tasksByFilter(TaskFilter().processingLvls({{ProcessingLvl::Level0}}));
        UNIT_ASSERT_EQUAL(
            getIds(tasks),
            (TIds({taskIds.at(0)})));
    }
}

Y_UNIT_TEST_F(filter_indoor_level, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);

    auto addTask = [&](const auto& indoorLevelOpt, auto resolution) {
        auto taskNew = TaskNew(ZERO_POSITION, SOME_TYPE, SRC, DESCR());
        taskNew.indoorLevel = indoorLevelOpt;

        auto task = agent.addTask(taskNew);
        agent.revealTaskByIdCascade(task.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, task.id()), resolution);
    };

    const std::optional<std::string> SOME_INDOOR_LEVEL = "some_indoor_level_id";
    const std::optional<std::string> SOME_OTHER_INDOOR_LEVEL = "some_other_indoor_level_id";
    const std::optional<std::string> EMPTY_INDOOR_LEVEL = "";
    const std::optional<std::string> NULL_INDOOR_LEVEL = std::nullopt;
    const std::vector<std::optional<std::string>> levels = {
        SOME_INDOOR_LEVEL, SOME_OTHER_INDOOR_LEVEL, EMPTY_INDOOR_LEVEL, NULL_INDOOR_LEVEL};
    const std::vector resolutions = {
        Resolution::createAccepted(),
        Resolution::createRejected(RejectReason::Spam)
    };

    for (const auto& level: levels) {
        for (auto resolution: resolutions) {
            addTask(level, resolution);
        }
    }

    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.indoorLevel(*SOME_INDOOR_LEVEL);

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 6);
    }
}

Y_UNIT_TEST_F(feedback_cascade_simple, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    auto trinity = createDuplicatesTemplate(gatewayRw);
    auto headId = trinity.head.id();
    auto dup0Id = trinity.duplicate0.id();
    auto dup1Id = trinity.duplicate1.id();

    {
        // head = Created
        // duplicate0 = Created
        // duplicate1 = Created

        // DO reveal
        // All should be in Revealed
        Agent agent(txn, USER_ID);
        auto headResult = agent.revealTaskByIdCascade(headId);
        auto head = *gatewayRw.taskById(headId);
        auto dup0 = *gatewayRw.taskById(dup0Id);
        auto dup1 = *gatewayRw.taskById(dup1Id);

        UNIT_ASSERT(isRevealed(head) && notResolved(head));
        UNIT_ASSERT(isRevealed(dup0) && notResolved(dup0));
        UNIT_ASSERT(isRevealed(dup1) && notResolved(dup1));
    }

    {
        Agent agent(txn, USER_ID);
        auto headResult = agent.resolveTaskCascade(
            getTaskForUpdate(agent, headId),
            Resolution::createAccepted());
        auto head = *gatewayRw.taskById(headId);
        auto dup0 = *gatewayRw.taskById(dup0Id);
        auto dup1 = *gatewayRw.taskById(dup1Id);

        UNIT_ASSERT(isResolved(head, USER_ID, Resolution::createAccepted())
                && notDeployed(head));
        UNIT_ASSERT(isResolved(dup0, USER_ID, Resolution::createAccepted())
                && notDeployed(dup0));
        UNIT_ASSERT(isResolved(dup1, USER_ID, Resolution::createAccepted())
                && notDeployed(dup1));
    }

    {
        Agent agent(txn, USER_ID);
        auto headResult = agent.deployTaskByIdCascade(
                headId, std::chrono::system_clock::now());
        auto head = *gatewayRw.taskById(headId);
        auto dup0 = *gatewayRw.taskById(dup0Id);
        auto dup1 = *gatewayRw.taskById(dup1Id);

        UNIT_ASSERT(isResolved(head, USER_ID, Resolution::createAccepted())
                && isDeployed(head));
        UNIT_ASSERT(isResolved(dup0, USER_ID, Resolution::createAccepted())
                && isDeployed(dup0));
        UNIT_ASSERT(isResolved(dup1, USER_ID, Resolution::createAccepted())
                && isDeployed(dup1));
    }
}

Y_UNIT_TEST_F(feedback_cascade_complicate, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    auto trinity = createDuplicatesTemplate(gatewayRw);
    auto headId = trinity.head.id();
    auto dup0Id = trinity.duplicate0.id();
    auto dup1Id = trinity.duplicate1.id();

    {
        Agent agent(txn, USER2_ID);
        agent.revealTaskByIdCascade(dup0Id);
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, dup0Id),
            Resolution::createRejected(RejectReason::NoData));
        // head       = Created                   Revealed
        // duplicate0 = Resolved(R) -> Reveal ->  Resolved(R)
        // duplicate1 = Created                   Revealed
        auto headResult = agent.revealTaskByIdCascade(headId);
        auto head = *gatewayRw.taskById(headId);
        auto dup0 = *gatewayRw.taskById(dup0Id);
        auto dup1 = *gatewayRw.taskById(dup1Id);

        UNIT_ASSERT(isRevealed(head) && notResolved(head));

        UNIT_ASSERT(isResolved(dup0, USER2_ID, Resolution::createRejected(RejectReason::NoData)));

        UNIT_ASSERT(isRevealed(dup1) && notResolved(dup1));
    }

    {
        Agent agent(txn, USER_ID);
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, dup1Id), Resolution::createAccepted());
        agent.deployTaskByIdCascade(dup1Id, std::chrono::system_clock::now());
        // head       = Revealed                      Resolve(A)
        // duplicate0 = Resolved(R) -> Resolve(A) ->  Resolved(R)
        // duplicate1 = Deployed(A)                   Deployed(A)
        auto headResult = agent.resolveTaskCascade(
            getTaskForUpdate(agent, headId), Resolution::createAccepted());
        auto head = *gatewayRw.taskById(headId);
        auto dup0 = *gatewayRw.taskById(dup0Id);
        auto dup1 = *gatewayRw.taskById(dup1Id);

        UNIT_ASSERT(isResolved(head, USER_ID, Resolution::createAccepted()));

        UNIT_ASSERT(isResolved(dup0, USER2_ID, Resolution::createRejected(RejectReason::NoData)));

        UNIT_ASSERT(isResolved(dup1, USER_ID, Resolution::createAccepted())
                && isDeployed(dup1));
    }

    {
        Agent agent(txn, USER_ID);
        // head       = Closed(A)                 Deployed(A)
        // duplicate0 = Closed(R)   -> Deploy ->  Closed(R)
        // duplicate1 = Deployed(A)               Deployed(A)
        auto headResult = agent.deployTaskByIdCascade(
                headId, std::chrono::system_clock::now());
        auto head = *gatewayRw.taskById(headId);
        auto dup0 = *gatewayRw.taskById(dup0Id);
        auto dup1 = *gatewayRw.taskById(dup1Id);

        UNIT_ASSERT(isResolved(head, USER_ID, Resolution::createAccepted())
                && isDeployed(head));

        UNIT_ASSERT(isResolved(dup0, USER2_ID, Resolution::createRejected(RejectReason::NoData))
                && notDeployed(dup0));

        UNIT_ASSERT(isResolved(dup1, USER_ID, Resolution::createAccepted())
                && isDeployed(dup1));
    }
}

Y_UNIT_TEST_F(feedback_change_head_status, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);
    Agent agent(txn, USER_ID);

    {
        auto trinity = createDuplicatesTemplate(gatewayRw);
        // head = Created
        // duplicate0 = Created
        // duplicate1 = Created

        // head should stay in Created
        auto head = changeHeadStatusAccordingToDuplicates(
            gatewayRw, USER_ID, trinity.head);
        UNIT_ASSERT_EQUAL(trinity.head.id(), head.id());
        UNIT_ASSERT_EQUAL(head.bucket(), Bucket::Incoming);
    }

    {
        auto trinity = createDuplicatesTemplate(gatewayRw);
        // head = Created

        // duplicate0 = Revealed
        agent.revealTaskByIdCascade(trinity.duplicate0.id());

        // duplicate1 = Revealed
        agent.revealTaskByIdCascade(trinity.duplicate1.id());

        // head should move to Revealed
        auto head = changeHeadStatusAccordingToDuplicates(
            gatewayRw, USER_ID, trinity.head);
        UNIT_ASSERT_EQUAL(trinity.head.id(), head.id());
        UNIT_ASSERT_EQUAL(head.bucket(), Bucket::Outgoing);
    }

    {
        auto trinity = createDuplicatesTemplate(gatewayRw);
        // head = Created

        // duplicate0 = Deployed
        agent.revealTaskByIdCascade(trinity.duplicate0.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, trinity.duplicate0.id()),
            Resolution::createAccepted());
        agent.deployTaskByIdCascade(
            trinity.duplicate0.id(), std::chrono::system_clock::now());

        // duplicated1 = Closed (Rejected)
        agent.revealTaskByIdCascade(trinity.duplicate1.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, trinity.duplicate1.id()),
            Resolution::createRejected(RejectReason::NoData));

        // head should stay at Created status
        auto head = changeHeadStatusAccordingToDuplicates(
            gatewayRw, USER_ID, trinity.head);
        UNIT_ASSERT_EQUAL(trinity.head.id(), head.id());
        UNIT_ASSERT_EQUAL(head.bucket(), Bucket::Incoming);
    }

    {
        auto trinity = createDuplicatesTemplate(gatewayRw);
        // head = Created

        // duplicate0 = Deployed
        agent.revealTaskByIdCascade(trinity.duplicate0.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, trinity.duplicate0.id()),
            Resolution::createAccepted());
        agent.deployTaskByIdCascade(
            trinity.duplicate0.id(), std::chrono::system_clock::now());

        // duplicated1 = Closed (Rejected)
        agent.revealTaskByIdCascade(trinity.duplicate1.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, trinity.duplicate1.id()),
            Resolution::createRejected(RejectReason::NoData));

        // head should not move to Resolved (Rejected), because all duplicates are closed
        auto head = changeHeadStatusAccordingToDuplicates(
            gatewayRw, USER_ID, trinity.head);
        UNIT_ASSERT_VALUES_EQUAL(trinity.head.id(), head.id());
        UNIT_ASSERT(!head.resolved());
        UNIT_ASSERT_EQUAL(head.bucket(), Bucket::Incoming);
    }

    {
        auto trinity = createDuplicatesTemplate(gatewayRw);
        // head = Revealed
        agent.revealTaskByIdCascade(trinity.duplicate0.id());
        agent.revealTaskByIdCascade(trinity.head.id());

        // duplicate0 = Closed (Accepted)
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, trinity.duplicate0.id()),
            Resolution::createAccepted());

        // duplicated1 = Closed (Rejected)
        agent.revealTaskByIdCascade(trinity.duplicate1.id());
        agent.resolveTaskCascade(
            getTaskForUpdate(agent, trinity.duplicate1.id()),
            Resolution::createRejected(RejectReason::Spam));

        // head should move to Closed (Accepted)
        auto head = changeHeadStatusAccordingToDuplicates(
            gatewayRw, USER_ID, trinity.head);
        UNIT_ASSERT_EQUAL(trinity.head.id(), head.id());
    }
}

Y_UNIT_TEST_F(feedback_bind_duplicates, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);
    Agent agent(txn, USER_ID);

    auto task0 = createTaskTemplate(gatewayRw);
    // not revealed feedback has a higher priority to be head
    agent.revealTaskByIdCascade(task0.id());

    auto task1 = createTaskTemplate(gatewayRw);

    {
        // TASK0  TASK1
        auto head = bindDuplicates(gatewayRw, USER_ID, TIds({task0.id(), task1.id()}));
        // task1
        //   ^
        //   |
        // task0
        UNIT_ASSERT(head && head->id() == task1.id() && head->duplicateHeadId() == std::nullopt);
        auto duplicates = gatewayRw.tasksByFilter(TaskFilter().duplicateHeadId(head->id()));
        UNIT_ASSERT_EQUAL(getIds(duplicates), (TIds({task0.id()})));
    }

    auto task2 = createTaskTemplate(gatewayRw);
    agent.revealTaskByIdCascade(task2.id());

    {
        // TASK1  TASK2
        //   ^
        //   |
        // task0
        auto head = bindDuplicates(gatewayRw, USER_ID, TIds({task1.id(), task2.id()}));
        //    task1
        //    ^   ^
        //    |   |
        // task0 task2
        UNIT_ASSERT(head && head->id() == task1.id() && head->duplicateHeadId() == std::nullopt);
        auto result = gatewayRw.tasksBriefByFilter(TaskFilter().duplicateHeadId(head->id()), std::nullopt);
        auto& duplicates = result.tasks;
        UNIT_ASSERT_EQUAL(getIds(duplicates), (TIds({task0.id(), task2.id()})));
    }

    agent.revealTaskByIdCascade(task1.id());
    auto task3 = createTaskTemplate(gatewayRw);

    {
        //    task1
        //    ^   ^
        //    |   |
        // task0 TASK2  TASK3
        auto head = bindDuplicates(gatewayRw, USER_ID, TIds({task2.id(), task3.id()}));
        //       task3
        //     ^   ^   ^
        //     |   |   |
        // task0 task1 task2
        UNIT_ASSERT(head && head->id() == task3.id() && head->duplicateHeadId() == std::nullopt);
        auto duplicates = gatewayRw.tasksByFilter(TaskFilter().duplicateHeadId(head->id()));
        UNIT_ASSERT_EQUAL(getIds(duplicates), (TIds({task0.id(), task1.id(), task2.id()})));
    }

    auto task4 = createTaskTemplate(gatewayRw);
    agent.revealTaskByIdCascade(task4.id());

    auto task5 = createTaskTemplate(gatewayRw);

    {
        // TASK4  TASK5
        auto head = bindDuplicates(gatewayRw, USER_ID, TIds({task4.id(), task5.id()}));
        // task5
        //   ^
        //   |
        // task4
        UNIT_ASSERT(head && head->id() == task5.id() && head->duplicateHeadId() == std::nullopt);
        auto duplicates = gatewayRw.tasksByFilter(TaskFilter().duplicateHeadId(head->id()));
        UNIT_ASSERT_EQUAL(getIds(duplicates), (TIds({task4.id()})));
    }

    auto task6 = createTaskTemplate(gatewayRw);

    {
        //       task3         TASK5
        //     ^   ^   ^         ^
        //     |   |   |         |      TASK6
        // task0 TASK1 task2   task4
        agent.revealTaskByIdCascade(task3.id());
        agent.revealTaskByIdCascade(task5.id());

        auto head = bindDuplicates(gatewayRw, USER_ID, TIds({task1.id(), task5.id(), task6.id()}));
        //                task6
        //     ^     ^    ^   ^    ^     ^
        //     |     |    |   |    |     |
        // task0 task1 task2 task3 task4 task5
        UNIT_ASSERT(head && head->id() == task6.id() && head->duplicateHeadId() == std::nullopt);
        auto duplicates = gatewayRw.tasksByFilter(TaskFilter().duplicateHeadId(head->id()));
        UNIT_ASSERT_EQUAL(
            getIds(duplicates),
            TIds({
                task0.id(), task1.id(), task2.id(),
                task3.id(), task4.id(), task5.id()
            })
        );
    }
}

Y_UNIT_TEST_F(test_feedback_bind_duplicates2, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);
    Agent agent(txn, USER_ID);

    auto trinity0 = createDuplicatesTemplate(gatewayRw);
    auto head0Id = trinity0.head.id();
    auto dup00Id = trinity0.duplicate0.id();
    auto dup01Id = trinity0.duplicate1.id();
    agent.revealTaskByIdCascade(head0Id);

    auto trinity1 = createDuplicatesTemplate(gatewayRw);
    auto head1Id = trinity1.head.id();
    auto dup10Id = trinity1.duplicate0.id();
    auto dup11Id = trinity1.duplicate1.id();
    agent.revealTaskByIdCascade(dup10Id);
    agent.revealTaskByIdCascade(dup11Id);

    {
        //    head0          head1
        //    ^   ^          ^   ^
        //    |   |          |   |
        // dup00 DUP01    DUP10 dup11
        auto head = bindDuplicates(gatewayRw, USER_ID, TIds({dup01Id, dup10Id}));
        //             head1
        //     ^     ^   ^   ^     ^
        //     |     |   |   |     |
        // dup00 dup01 dup10 dup11 head0
        UNIT_ASSERT(head && head->id() == head1Id && head->duplicateHeadId() == std::nullopt);
        auto duplicates = gatewayRw.tasksByFilter(TaskFilter().duplicateHeadId(head->id()));
        UNIT_ASSERT_EQUAL(
            getIds(duplicates),
            TIds({dup00Id, dup01Id, dup10Id, dup11Id, head0Id})
        );
    }
}

Y_UNIT_TEST_F(filter_feedback_tasks, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRO gatewayRo(txn);
    Agent agent(txn, 1);

    auto acquiredTask = agent.addTask(
        TaskNew(
            ZERO_POSITION,
            TYPE,
            SRC,
            DESCR()
        )
    );
    agent.revealTaskByIdCascade(acquiredTask.id());
    agent.acquireTask(getTaskForUpdate(agent, acquiredTask.id()));

    {
        auto unresolvedTask = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                DESCR()
            )
        );
    }

    auto unresolvedTask = agent.addTask(
        TaskNew(
            geolib3::Point2(2., 3.8),
            TYPE,
            SRC,
            DESCR()
        )
    );
    agent.hideTask(getTaskForUpdate(agent, unresolvedTask.id()));

    auto resolvedTask = agent.addTask(
        TaskNew(
            geolib3::Point2(4., 4.),
            TYPE,
            SRC,
            DESCR()
        )
    );
    agent.revealTaskByIdCascade(resolvedTask.id());
    agent.resolveTaskCascade(
        getTaskForUpdate(agent, resolvedTask.id()),
        Resolution::createRejected(RejectReason::NoData));

    auto resolvedHiddenTask = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                DESCR()
            )
    );
    agent.revealTaskByIdCascade(resolvedHiddenTask.id());
    agent.resolveTaskCascade(
        getTaskForUpdate(agent, resolvedHiddenTask.id()),
        Resolution::createRejected(RejectReason::NoData));
    agent.hideTask(getTaskForUpdate(agent, resolvedHiddenTask.id()));

    {
        geolib3::BoundingBox bbox(geolib3::Point2(2., 2.), 3.9999, 3.9999);
        const auto filter = TaskFilter().boxBoundary(bbox);
        auto result = gatewayRo.tasksBriefByFilter(filter, std::nullopt);
        auto& tasks = result.tasks;
        UNIT_ASSERT(tasks.size() == 1);
        UNIT_ASSERT_EQUAL(tasks[0].id(), unresolvedTask.id());
        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({unresolvedTask.id()}));
    }

    {
        TaskFilter filter;
        filter.ids({unresolvedTask.id()});
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        UNIT_ASSERT_EQUAL(tasks[0].id(), unresolvedTask.id());

        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({unresolvedTask.id()}));
    }

    {
        TaskFilter filter;
        filter.ids({
                acquiredTask.id(),
                resolvedTask.id(),
                resolvedTask.id() + 3000});
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 2);

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({acquiredTask.id(), resolvedTask.id()}));
    }

    {
        TaskFilter filter;
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 5);

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 5);
    }

    {
        TaskFilter filter;
        filter.ids({acquiredTask.id(), unresolvedTask.id()});
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 2);

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 2);
    }

    {
        TaskFilter filter;
        filter.hidden(false);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 3);

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 3);
    }

    {
        TaskFilter filter;
        filter.hidden(true);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 2);

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 2);
    }

    {
        TaskFilter filter;
        filter.resolved(false);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 3);

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 3);
    }

    { // &filter=opened
        TaskFilter filter;
        filter.resolved(false).hidden(false);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 2);

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 2);
    }

    { // &filter=hidden
        TaskFilter filter;
        filter.resolved(false).hidden(true);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        UNIT_ASSERT_EQUAL(tasks[0].id(), unresolvedTask.id());

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({unresolvedTask.id()}));
    }

    {
        TaskFilter filter;
        filter.acquiredBy(1);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        UNIT_ASSERT_EQUAL(tasks[0].id(), acquiredTask.id());

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({acquiredTask.id()}));
    }

    {
        TaskFilter filter;
        filter.acquiredBy(2);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT(tasks.empty());

        const auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT(taskIds.empty());
    }

    {
        // check empty sources
        TaskFilter filter;
        filter.sources(std::vector<std::string>{});
        UNIT_ASSERT(gatewayRo.tasksByFilter(filter).empty());
        UNIT_ASSERT(gatewayRo.taskIdsByFilter(filter).empty());
    }

    {
        // check non-empty source
        TaskFilter filter;
        filter.sources(std::vector<std::string>{SRC});
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), 5);
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), 5);
    }

    {
        // check empty workflows
        TaskFilter filter;
        filter.workflows(Workflows{});
        UNIT_ASSERT(gatewayRo.tasksByFilter(filter).empty());
        UNIT_ASSERT(gatewayRo.taskIdsByFilter(filter).empty());
    }

    {
        // check non-empty workflows
        TaskFilter filter;
        filter.workflows(Workflows{Workflow::Task});
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), 5);
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), 5);
    }

    {
        // check empty types
        TaskFilter filter;
        filter.types(Types{});
        UNIT_ASSERT(gatewayRo.tasksByFilter(filter).empty());
        UNIT_ASSERT(gatewayRo.taskIdsByFilter(filter).empty());
    }

    {
        // check non-empty types
        TaskFilter filter;
        filter.types(Types{TYPE});
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), 5);
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), 5);
    }
}

Y_UNIT_TEST_F(filter_feedback_tasks_created_at, DbFixture)
{
    std::map<chrono::TimePoint, TaskForUpdate> timeToTask;
    std::vector<chrono::TimePoint> times;

    for (int i = 1; i <= 3; ++i) {
        pqxx::work txn{conn};
        Agent agent(txn, USER_ID);
        auto taskForUpdate = agent.addTask(TaskNew(ZERO_POSITION, TYPE, SRC, DESCR()));
        taskForUpdate = agent.revealTaskByIdCascade(taskForUpdate.id()).value();
        txn.commit();

        auto time = taskForUpdate.createdAt();
        const auto& [it, success] = timeToTask.insert({time, taskForUpdate});
        ASSERT(success);
        times.push_back(time);
        sleep(1);
    }
    UNIT_ASSERT(times.size() == timeToTask.size());

    pqxx::work txn{conn};
    GatewayRO gatewayRo{txn};
    { // all created_at values
        TaskFilter filter;
        filter.createdAt(DateTimeCondition(std::nullopt, std::nullopt));
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), times.size());
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), times.size());
    }
    { // filter up to 1st task creation time not including
        TaskFilter filter;
        filter.createdAt(DateTimeCondition(std::nullopt, times.front()));
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), 0);
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), 0);
    }
    { // filter from -inf to 2nd task creation time not including
        TaskFilter filter;
        auto time = times[1];
        filter.createdAt(DateTimeCondition(std::nullopt, time));
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), 1);
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), 1);
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).front().id(), timeToTask.at(times[0]).id());
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter), TIds({timeToTask.at(times[0]).id()}));
    }
    { // filter from 1st (including) to last (not including) time
        TaskFilter filter;
        filter.createdAt(DateTimeCondition(times.front(), times.back()));
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), (times.size() - 1));
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), (times.size() - 1));
    }
    { // filter from 1st (including) to +inf time
        TaskFilter filter;
        filter.createdAt(DateTimeCondition(times.front(), std::nullopt));
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), times.size());
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), times.size());
    }
    { // filter from 1st (including) to now
        TaskFilter filter;
        filter.createdAt(DateTimeCondition(times.front(), std::chrono::system_clock::now()));
        UNIT_ASSERT_EQUAL(gatewayRo.tasksByFilter(filter).size(), times.size());
        UNIT_ASSERT_EQUAL(gatewayRo.taskIdsByFilter(filter).size(), times.size());
    }
}

Y_UNIT_TEST_F(filter_feedback_resolved_by_robots, unittest::ArcadiaDbFixture)
{
    ASSERT(!common::isRobot(SOME_USER_UID));

    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();
    GatewayRO gatewayRo(socialTxn);
    Agent agent(socialTxn, common::ROBOT_UID);

    const auto& robots = common::ALL_ROBOTS_UIDS;
    auto addTaskResolvedByUid = [&](TUid uid) {
        Agent agent(socialTxn, uid);
        auto task = agent.addTask(
            TaskNew(
                geolib3::Point2(0, 0),
                Type::Address,
                SOME_SOURCE,
                SOME_DESCR));
        agent.revealTaskByIdCascade(task.id());
        return *agent.resolveTaskCascade(
            getTaskForUpdate(agent, task.id()),
            Resolution::createRejected(RejectReason::NoData));
    };

    TUids resolvers(
        robots.begin(), robots.end());
    resolvers.insert(SOME_USER_UID);

    // Resolve one task by each robot.
    TIds resolvedByRobotsTaskIds;
    for (auto uid: robots) {
        const auto task = addTaskResolvedByUid(uid);
        resolvedByRobotsTaskIds.insert(task.id());
    }
    TIds allTasksIds = resolvedByRobotsTaskIds;
    // Resolve one task by a user.
    const auto humanResolvedTask = addTaskResolvedByUid(SOME_USER_UID);
    allTasksIds.insert(humanResolvedTask.id());

    // Leave one task unresolved.
    const auto nonResolvedTask = agent.addTask(TaskNew(
        geolib3::Point2(0, 0), Type::Address, SOME_SOURCE, SOME_DESCR));
    agent.revealTaskByIdCascade(nonResolvedTask.id());
    allTasksIds.insert(nonResolvedTask.id());

    TId beforeId = 0;
    TId afterId = 0;
    uint64_t perPage = 20;
    TaskFeedParamsId feedParams(
        beforeId, afterId, perPage, TasksOrder::NewestFirst);

    auto checkAllExpectedTasksRetrieved = [&](const TaskFilter& filter,
                                              const TIds& expectedTasksIds) {
        const auto tasksFeedWithCount =
            gatewayRo.tasksFeedWithCount(filter, feedParams);

        UNIT_ASSERT_EQUAL(tasksFeedWithCount.hasMore, HasMore::No);

        const auto& filteredTasks = tasksFeedWithCount.tasks;

        UNIT_ASSERT_EQUAL(expectedTasksIds.size(), filteredTasks.size());
        for (const auto& task: filteredTasks) {
            UNIT_ASSERT_EQUAL(expectedTasksIds.count(task.id()), 1);
        }
    };

    // Check that every resolved task is retrieved with empty filter.
    {
        const TIds expectedTasksIds{allTasksIds};

        TaskFilter filter;
        checkAllExpectedTasksRetrieved(filter, expectedTasksIds);
    }
    // Check that only user-resolved and non-resolved tasks are retrieved.
    {
        const TIds expectedTasksIds{nonResolvedTask.id(), humanResolvedTask.id()};

        TaskFilter filter;
        filter.notResolvedBy({robots.begin(), robots.end()});
        checkAllExpectedTasksRetrieved(filter, expectedTasksIds);
    }
}

Y_UNIT_TEST_F(filter_age_type, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRo(txn);
    Agent agent(txn, USER_ID);
    auto newTask = createNewTask(agent);
    auto oldTask = createOldTask(txn);

    {
        TaskFilter filter;
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT(tasks.size() == 2);
        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT(taskIds.size() == 2);
    }
    {
        TaskFilter filter;
        filter.ageTypes(AgeTypes{});
        UNIT_ASSERT(gatewayRo.tasksByFilter(filter).empty());
        UNIT_ASSERT(gatewayRo.taskIdsByFilter(filter).empty());
    }
    {
        TaskFilter filter;
        filter.ageType(AgeType::New);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        UNIT_ASSERT_EQUAL(tasks.front().id(), newTask.id());

        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({newTask.id()}));
    }
    {
        TaskFilter filter;
        filter.ageType(AgeType::Old);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT(tasks.size() == 1);
        UNIT_ASSERT(tasks.front().id() == oldTask.id());

        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({oldTask.id()}));
    }
    {
        TaskFilter filter;
        filter.ageTypes(AgeTypes{AgeType::New, AgeType::Old});
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 2);
        UNIT_ASSERT_EQUAL(getIds(tasks), TIds({newTask.id(), oldTask.id()}));

        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds, TIds({newTask.id(), oldTask.id()}));
    }

    createHypothesis(agent);

    {
        TaskFilter filter;
        filter.ageType(AgeType::New);
        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT(tasks.size() == 2);

        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT(taskIds.size() == 2);
    }
}

Y_UNIT_TEST_F(task_feed, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    auto taskId0 = createTaskTemplate(gatewayRw).id();
    auto taskId1 = createTaskTemplate(gatewayRw).id();
    auto taskId2 = createTaskTemplate(gatewayRw).id();
    auto taskId3 = createTaskTemplate(gatewayRw).id();

    {
        // no before and after ids
        //
        TaskFeedParamsId feedParams(0, 0, 10, TasksOrder::OldestFirst);
        auto feed = gatewayRw.tasksFeedWithCount(TaskFilter(), feedParams);

        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::No);
        UNIT_ASSERT_EQUAL(feed.totalCount, 4);

        const auto& tasks = feed.tasks;

        UNIT_ASSERT_EQUAL(tasks.size(), 4);
        UNIT_ASSERT_EQUAL(tasks.at(0).id(), taskId0);
        UNIT_ASSERT_EQUAL(tasks.at(1).id(), taskId1);
        UNIT_ASSERT_EQUAL(tasks.at(2).id(), taskId2);
        UNIT_ASSERT_EQUAL(tasks.at(3).id(), taskId3);
    }
    {
        // order: oldest first, select: before
        //
        TaskFeedParamsId feedParams(taskId2, 0, 10, TasksOrder::OldestFirst);
        auto feed = gatewayRw.tasksFeedWithCount(TaskFilter(), feedParams);

        const auto& tasks = feed.tasks;

        UNIT_ASSERT_EQUAL(tasks.size(), 2);
        UNIT_ASSERT_EQUAL(tasks.at(0).id(), taskId0);
        UNIT_ASSERT_EQUAL(tasks.at(1).id(), taskId1);
    }
    {
        // order: newest first, select: before
        //
        TaskFeedParamsId feedParams(taskId1, 0, 10, TasksOrder::NewestFirst);
        auto feed = gatewayRw.tasksFeedWithCount(TaskFilter(), feedParams);

        const auto& tasks = feed.tasks;

        UNIT_ASSERT_EQUAL(tasks.size(), 2);
        UNIT_ASSERT_EQUAL(tasks.at(0).id(), taskId3);
        UNIT_ASSERT_EQUAL(tasks.at(1).id(), taskId2);
    }
    {
        // order: oldest first, select: after
        //
        TaskFeedParamsId feedParams(0, taskId1, 10, TasksOrder::OldestFirst);
        auto feed = gatewayRw.tasksFeedWithCount(TaskFilter(), feedParams);

        const auto& tasks = feed.tasks;

        UNIT_ASSERT_EQUAL(tasks.size(), 2);
        UNIT_ASSERT_EQUAL(tasks.at(0).id(), taskId2);
        UNIT_ASSERT_EQUAL(tasks.at(1).id(), taskId3);
    }
    {
        // order: newest first, select: after
        //
        TaskFeedParamsId feedParams(0, taskId3, 10, TasksOrder::NewestFirst);
        auto feed = gatewayRw.tasksFeedWithCount(TaskFilter(), feedParams);

        const auto& tasks = feed.tasks;

        UNIT_ASSERT_EQUAL(tasks.size(), 3);
        UNIT_ASSERT_EQUAL(tasks.at(0).id(), taskId2);
        UNIT_ASSERT_EQUAL(tasks.at(1).id(), taskId1);
        UNIT_ASSERT_EQUAL(tasks.at(2).id(), taskId0);
    }
    {
        // check limit and has more
        //
        TaskFeedParamsId feedParams(0, taskId0, 2, TasksOrder::OldestFirst);
        auto feed = gatewayRw.tasksFeedWithCount(TaskFilter(), feedParams);

        const auto& tasks = feed.tasks;

        UNIT_ASSERT_EQUAL(tasks.size(), 2);
        UNIT_ASSERT_EQUAL(tasks.at(0).id(), taskId1);
        UNIT_ASSERT_EQUAL(tasks.at(1).id(), taskId2);

        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::Yes);
        UNIT_ASSERT_EQUAL(feed.totalCount, 4);
    }
    {
        // check case of limit = 0
        //
        TaskFeedParamsId feedParams(0, taskId0, 0, TasksOrder::OldestFirst);
        auto feed = gatewayRw.tasksFeedWithCount(TaskFilter(), feedParams);

        const auto& tasks = feed.tasks;

        UNIT_ASSERT_EQUAL(tasks.size(), 0);
        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::Yes);
        UNIT_ASSERT_EQUAL(feed.totalCount, 4);
    }
}

Y_UNIT_TEST_F(commits_binding, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRo(txn);
    Agent agent(txn, USER_ID);

    auto taskWithHistory = agent.addTask(
        TaskNew(
            ZERO_POSITION,
            TYPE,
            SRC,
            DESCR()
        )
    );
    auto taskWithoutHistory = agent.addTask(
        TaskNew(
            ZERO_POSITION,
            TYPE,
            SRC,
            DESCR()
        )
    );
    const TId commitId1 = 1;
    const TId commitId2 = 2;
    const TId absentCommitId = 3;
    bindCommitsToTask(txn, taskWithHistory.id(), {commitId1, commitId2});

    auto taskByCommit1 = gatewayRo.taskByCommitId(commitId1);
    UNIT_ASSERT(taskByCommit1);
    UNIT_ASSERT_EQUAL(taskByCommit1->id(), taskWithHistory.id());

    auto taskByCommit3 = gatewayRo.taskByCommitId(absentCommitId);
    UNIT_ASSERT(!taskByCommit3);

    auto commitsByTaskWithHistory = commitIdsByTaskId(txn, taskWithHistory.id());
    UNIT_ASSERT_EQUAL(commitsByTaskWithHistory.size(), 2);
    UNIT_ASSERT_EQUAL(commitsByTaskWithHistory.count(commitId1), 1);
    UNIT_ASSERT_EQUAL(commitsByTaskWithHistory.count(commitId2), 1);

    auto commitsByTaskWithoutHistory = commitIdsByTaskId(txn, taskWithoutHistory.id());
    UNIT_ASSERT(commitsByTaskWithoutHistory.empty());

    auto commitIdToTaskId = taskIdsByCommitIds(txn, {commitId1});
    std::unordered_map<TId, TId> expectedCommitIdToTaskId{{commitId1, taskWithHistory.id()}};
    UNIT_ASSERT(commitIdToTaskId == expectedCommitIdToTaskId);
}

Y_UNIT_TEST_F(description_db_io, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);

    {
        auto task = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                Description()
            )
        );

        const auto& descr = task.description();
        UNIT_ASSERT(descr.isNonTranslatable());
        UNIT_ASSERT_STRINGS_EQUAL(descr.asNonTranslatable(), "");
    }
    {
        auto task = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                Description("")
            )
        );

        const auto& descr = task.description();
        UNIT_ASSERT(descr.isNonTranslatable());
        UNIT_ASSERT_STRINGS_EQUAL(descr.asNonTranslatable(), "");
    }
    {
        auto task = agent.addTask(
            TaskNew(
                ZERO_POSITION,
                TYPE,
                SRC,
                Description("{}")
            )
        );

        const auto& descr = task.description();
        UNIT_ASSERT(descr.isNonTranslatable());
        UNIT_ASSERT_STRINGS_EQUAL(descr.asNonTranslatable(), "{}");
    }
}

Y_UNIT_TEST_F(add_task_if_not_twin, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    const auto isDescriptionsEqual =
        [&](const TaskNew& newTask, const Task& oldTask) {
            return newTask.description.asNonTranslatable() ==
                   oldTask.description().asNonTranslatable();
    };

    auto addedTask = addTaskIfNotTwin(
        gatewayRw,
        USER_ID,
        TaskNew(
            ZERO_POSITION,
            Type::Address,
            SRC,
            Description("first-description")
        ),
        isDescriptionsEqual
    );

    UNIT_ASSERT(addedTask);

    auto dublicatedTask = addTaskIfNotTwin(
        gatewayRw,
        USER_ID,
        TaskNew(
            ZERO_POSITION,
            Type::Address,
            SRC,
            Description("first-description")
        ),
        isDescriptionsEqual
    );

    UNIT_ASSERT(!dublicatedTask);

    auto anotherDescriptionAddedTask = addTaskIfNotTwin(
        gatewayRw,
        USER_ID,
        TaskNew(
            ZERO_POSITION,
            Type::Address,
            SRC,
            Description("second-description")
        ),
        isDescriptionsEqual
    );

    UNIT_ASSERT(anotherDescriptionAddedTask);

    auto anotherTypeAddedTask = addTaskIfNotTwin(
        gatewayRw,
        USER_ID,
        TaskNew(
            ZERO_POSITION,
            Type::Other,
            SRC,
            Description("first-description")
        ),
        isDescriptionsEqual
    );

    UNIT_ASSERT(anotherTypeAddedTask);
}

Y_UNIT_TEST_F(add_task_internal_content, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRo(txn);
    Agent agent(txn, USER_ID);
    {
        TaskNew newTask(ZERO_POSITION, TYPE, SRC, DESCR());
        auto createdTask = agent.addTask(newTask);

        auto recievedTask = gatewayRo.taskById(createdTask.id());
        UNIT_ASSERT(recievedTask);
        UNIT_ASSERT_EQUAL(recievedTask->internalContent(), false);
    }
    {
        TaskNew newTask(ZERO_POSITION, TYPE, SRC, DESCR());
        newTask.internalContent = true;
        auto createdTask = agent.addTask(newTask);

        auto recievedTask = gatewayRo.taskById(createdTask.id());
        UNIT_ASSERT(recievedTask);
        UNIT_ASSERT_EQUAL(recievedTask->internalContent(), true);
    }
}

Y_UNIT_TEST_F(need_info_filter, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);
    Agent agent2(txn, USER2_ID);

    auto taskId = createTaskNeedInfoAvailable(agent);
    auto createdTime = getTaskForUpdate(agent, taskId).createdAt();

    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.lastNeedInfoAt(DateTimeCondition(std::nullopt, std::nullopt));

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT(tasks.empty());

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT(tasks.empty());
    }

    // move to need-info state
    agent2.needInfoTask(getTaskForUpdate(agent2, taskId), 0);

    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.lastNeedInfoBy(USER2_ID);
        filter.lastNeedInfoAt(DateTimeCondition(createdTime, createdTime + std::chrono::seconds(600)));

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 1);
    }
    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.lastNeedInfoBy(USER_ID);

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT(tasks.empty());

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT(tasks.empty());
    }
    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.lastNeedInfoBy(USER2_ID);

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 1);
    }
    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.lastNeedInfoAt(DateTimeCondition(createdTime - std::chrono::seconds(600), std::nullopt));

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 1);
    }
    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.lastNeedInfoAt(DateTimeCondition(std::nullopt, createdTime - std::chrono::seconds(600)));

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT(tasks.empty());

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT(tasks.empty());
    }
}

Y_UNIT_TEST_F(history_filter, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);
    Agent agent2(txn, USER2_ID);

    auto taskId = createRevealedTask(agent);

    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.modifiedBy(USER_ID);

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT(tasks.empty());

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT(tasks.empty());
    }

    // reject task
    agent2.resolveTaskCascade(
        getTaskForUpdate(agent2, taskId),
        Resolution::createRejected(RejectReason::NoData));

    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.modifiedBy(USER2_ID);

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 1);

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 1);
    }
    {
        GatewayRW gatewayRw(txn);
        TaskFilter filter;
        filter.modifiedBy(USER_ID);

        auto tasks = gatewayRw.tasksByFilter(filter);
        UNIT_ASSERT_EQUAL(tasks.size(), 0);

        auto taskIds = gatewayRw.taskIdsByFilter(filter);
        UNIT_ASSERT_EQUAL(taskIds.size(), 0);
    }
}

Y_UNIT_TEST_F(count_tasks_with_filter, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRo(txn);
    Agent agent(txn, USER_ID);
    { // zero tasks without aggregation
        const auto counters = gatewayRo.getAggregatedCountOfTasks(
            TaskFilter(), {});

        UNIT_ASSERT_EQUAL(counters.size(), 1);
        UNIT_ASSERT_EQUAL(counters[0].count, 0);
    }

    // Create tasks.
    {
        TaskNew newTask(ZERO_POSITION, Type::Road, SRC, DESCR());
        for (int i = 0; i < 2; ++i) {
            auto createdTask = agent.addTask(newTask);
        }
    }
    {
        TaskNew newTask(ZERO_POSITION, Type::NoRoad, SRC, DESCR());
        auto createdTask = agent.addTask(newTask);
    }
    {
        TaskNew newTask(ZERO_POSITION, Type::Building, SRC, DESCR());
        auto createdTask = agent.addTask(newTask);
    }

    { // Empty filter
        const auto counters = gatewayRo.getAggregatedCountOfTasks(TaskFilter(), {});

        UNIT_ASSERT_EQUAL(counters.size(), 1);
        UNIT_ASSERT_EQUAL(counters[0].count, 4);
    }
    { // Filter for Type::Road (2 tasks) and Type::NoRoad (1 task)
        const TaskFilter filter = TaskFilter().types(Types{Type::Road, Type::NoRoad});
        const auto counters = gatewayRo.getAggregatedCountOfTasks(filter, {});

        UNIT_ASSERT_EQUAL(counters.size(), 1);
        UNIT_ASSERT_EQUAL(counters[0].count, 3);
    }
}

Y_UNIT_TEST_F(count_tasks_aggregation, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRo(txn);
    Agent agent(txn, USER_ID);

    auto getCounters = [&]() {
        return gatewayRo.getAggregatedCountOfTasks(
            TaskFilter(),
            {Column::Workflow, Column::Type});
    };

    { // zero tasks with aggregation
        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), 0);
    }

    std::map<GroupByFields, uint64_t> expected;

    // Create tasks.
    {
        TaskNew newTask(ZERO_POSITION, Type::Road, SRC_FBAPI, DESCR());
        auto createdTask = agent.addTask(newTask);

        const GroupByFields columns = {
            {Column::Workflow, std::string(toString(Workflow::Feedback))},
            {Column::Type,     std::string(toString(Type::Road))}};
        expected[columns] += 1;

        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), 1);
        const auto& c = counters.front();
        UNIT_ASSERT(expected.count(getGroupByFieldValues(c)));
        UNIT_ASSERT_EQUAL(expected.at(getGroupByFieldValues(c)), c.count);
    }

    {
        TaskNew newTask(ZERO_POSITION, Type::Road, SRC_FBAPI, DESCR());
        auto createdTask = agent.addTask(newTask);

        const GroupByFields columns = {
            {Column::Workflow, std::string(toString(Workflow::Feedback))},
            {Column::Type,     std::string(toString(Type::Road))}};
        expected[columns] += 1;

        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), 1);
        const auto& c = counters.front();
        UNIT_ASSERT_EQUAL(getGroupByFieldValues(c), columns);
        UNIT_ASSERT_EQUAL(c.count, 2);
    }
    {
        TaskNew newTask(ZERO_POSITION, Type::NoRoad, SRC_FBAPI, DESCR());
        auto createdTask = agent.addTask(newTask);

        const GroupByFields columns = {
            {Column::Workflow, std::string(toString(Workflow::Feedback))},
            {Column::Type,     std::string(toString(Type::NoRoad))}};
        expected[columns] += 1;

        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), expected.size());
        for (const auto& counter: counters) {
            const auto count = counter.count;
            const auto cols = getGroupByFieldValues(counter);
            UNIT_ASSERT(expected.count(cols));
            UNIT_ASSERT_EQUAL(expected.at(cols), count);
        }
    }
    {
        TaskNew newTask(ZERO_POSITION, Type::Building, SRC_FBAPI, DESCR());
        auto createdTask = agent.addTask(newTask);

        const GroupByFields columns = {
            {Column::Workflow, std::string(toString(Workflow::Feedback))},
            {Column::Type,     std::string(toString(Type::Building))}};
        expected[columns] += 1;

        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), expected.size());
        for (const auto& counter: counters) {
            const auto count = counter.count;
            const auto cols = getGroupByFieldValues(counter);
            UNIT_ASSERT(expected.count(cols));
            UNIT_ASSERT_EQUAL(expected.at(cols), count);
        }
    }
    {
        TaskNew newTask(ZERO_POSITION, Type::Building, SRC_1, DESCR());
        auto createdTask = agent.addTask(newTask);

        const GroupByFields columns = {
            {Column::Workflow, std::string(toString(Workflow::Task))},
            {Column::Type,     std::string(toString(Type::Building))}};
        expected[columns] += 1;

        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), expected.size());
        for (const auto& counter: counters) {
            const auto count = counter.count;
            const auto cols = getGroupByFieldValues(counter);
            UNIT_ASSERT(expected.count(cols));
            UNIT_ASSERT_EQUAL(expected.at(cols), count);
        }
    }
}

Y_UNIT_TEST_F(count_tasks_aggregation_with_filter, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRo(txn);
    Agent agent(txn, USER_ID);

    auto getCounters = [&]() {
        return gatewayRo.getAggregatedCountOfTasks(
            TaskFilter().types(Types{Type::Road, Type::Building}).source(SRC_1),
            {Column::Workflow, Column::Source, Column::Type});
    };

    { // zero tasks with aggregation and filters
        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), 0);
    }

    std::map<GroupByFields, uint64_t> expected;
    // Create tasks.
    { // Filtered out.
        TaskNew newTask(ZERO_POSITION, Type::Road, SRC, DESCR());
        auto createdTask = agent.addTask(newTask);
    }
    { // Filtered out.
        TaskNew newTask(ZERO_POSITION, Type::NoRoad, SRC, DESCR());
        auto createdTask = agent.addTask(newTask);
    }
    { // Filtered out.
        TaskNew newTask(ZERO_POSITION, Type::Building, SRC_2, DESCR());
        auto createdTask = agent.addTask(newTask);
    }

    {
        TaskNew newTask(ZERO_POSITION, Type::Road, SRC_1, DESCR());
        const GroupByFields columns = {
            {Column::Workflow, std::string(toString(Workflow::Task))},
            {Column::Type,     std::string(toString(Type::Road))},
            {Column::Source,   SRC_1}};

        for (int i = 0; i < 2; ++i) {
            auto createdTask = agent.addTask(newTask);
            expected[columns] += 1;
        }
    }
    {
        TaskNew newTask(ZERO_POSITION, Type::Building, SRC_1, DESCR());
        auto createdTask = agent.addTask(newTask);

        const GroupByFields columns = {
            {Column::Workflow, std::string(toString(Workflow::Task))},
            {Column::Type,     std::string(toString(Type::Building))},
            {Column::Source,   SRC_1}};
        expected[columns] += 1;
    }
    {
        const auto counters = getCounters();

        UNIT_ASSERT_EQUAL(counters.size(), expected.size());
        for (const auto& counter: counters) {
            const auto count = counter.count;
            const auto cols = getGroupByFieldValues(counter);
            UNIT_ASSERT(expected.count(cols));
            UNIT_ASSERT_EQUAL(expected.at(cols), count);
        }
    }
}

Y_UNIT_TEST_F(check_empty_external_reference_id, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    TaskNew task(
        ZERO_POSITION,
        Type::Other,
        SRC,
        Description());

    task.externalReferenceId = "";
    UNIT_ASSERT_EXCEPTION(gatewayRw.addTask(USER_ID, task), maps::RuntimeError);
}

Y_UNIT_TEST_F(check_duplicated_external_reference_id, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    TaskNew task(
        ZERO_POSITION,
        Type::Other,
        SRC,
        Description());

    task.externalReferenceId = "sprav:123";
    UNIT_ASSERT_NO_EXCEPTION(gatewayRw.addTask(USER_ID, task));

    UNIT_ASSERT_EXCEPTION(gatewayRw.addTask(USER_ID, task), pqxx::unique_violation);
}

Y_UNIT_TEST_F(load_task_by_external_reference_id, DbFixture)
{
    pqxx::work txn(conn);
    GatewayRW gatewayRw(txn);

    TaskNew task(
        ZERO_POSITION,
        Type::Other,
        SRC,
        Description());

    auto ID = "sprav:123";
    task.externalReferenceId = ID;
    auto createdTask = gatewayRw.addTask(USER_ID, task);
    auto loadedTask = gatewayRw.taskByExternalReferenceId(ID);
    UNIT_ASSERT(loadedTask);
    UNIT_ASSERT_EQUAL(createdTask.id(), loadedTask->id());
}

Y_UNIT_TEST_F(should_filter_by_source_not_in, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);

    std::array taskIds = {
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "source-1", DESCR())).id(), // 0
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "source-2", DESCR())).id(), // 1
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "source-3", DESCR())).id(), // 2
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "source-2", DESCR())).id(), // 3
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "source-1", DESCR())).id()  // 4
    };

    GatewayRO gw(txn);
    UNIT_ASSERT_EQUAL(
        gw.getTotalCountOfTasks(TaskFilter()),
        taskIds.size()
    );
    UNIT_ASSERT_EQUAL(
        gw.taskIdsByFilter(TaskFilter().sourceNotIn({"source-1"})),
        TIds({taskIds[1], taskIds[2], taskIds[3]})
    );
    UNIT_ASSERT_EQUAL(
        gw.taskIdsByFilter(TaskFilter().sourceNotIn({"source-2", "source-3"})),
        TIds({taskIds[0], taskIds[4]})
    );
}

Y_UNIT_TEST_F(should_filter_experiments, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);

    std::array taskIds = {
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "experiment-source-1",   DESCR())).id(), // 0
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "non-experiment-source", DESCR())).id(), // 1
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "other-source",          DESCR())).id(), // 2
        agent.addTask(TaskNew(ZERO_POSITION, TYPE, "experiment-source-2",   DESCR())).id(), // 3
    };

    GatewayRO gw(txn);
    UNIT_ASSERT_EQUAL(gw.getTotalCountOfTasks(TaskFilter()), taskIds.size());
    UNIT_ASSERT_EQUAL(
        gw.taskIdsByFilter(TaskFilter().experiment(true)),
        TIds({taskIds[0], taskIds[3]})
    );
    UNIT_ASSERT_EQUAL(
        gw.taskIdsByFilter(TaskFilter().experiment(false)),
        TIds({taskIds[1], taskIds[2]})
    );
}

Y_UNIT_TEST_F(aoid_task_filter, DbFixture)
{
    pqxx::work txn(conn);
    {
        auto task = FbTaskCreator(txn, TaskState::Opened).source("fbapi-samsara").create();

        Agent agent(txn, USER_ID);
        auto acquiredTask = agent.acquireTask(task);
        UNIT_ASSERT(acquiredTask);
        UNIT_ASSERT(agent.changeTaskPosition(*acquiredTask, task.position(), {10, 11}));
    }
    {
        GatewayRO gatewayRo(txn);
        TaskFilter filter;
        filter.aoiId(12);

        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT(tasks.empty());

        auto taskIds = gatewayRo.taskIdsByFilter(filter);
        UNIT_ASSERT(tasks.empty());
    }
    {
        GatewayRW gatewayRo(txn);
        TaskFilter filter;
        filter.aoiId(11);

        auto tasks = gatewayRo.tasksByFilter(filter);
        UNIT_ASSERT(!tasks.empty());
    }
    {
        GatewayRW gatewayRo(txn);
        TaskFilter filter;
        filter.aoiId(11);
        filter.modifiedBy(USER_ID);

        UNIT_ASSERT_NO_EXCEPTION(gatewayRo.tasksByFilter(filter));
    }
}

Y_UNIT_TEST_F(filter_with_more, DbFixture)
{
    std::vector<TId> taskIds;

    // create 3 tasks
    {
        pqxx::work txn(conn);
        for (size_t i = 0; i < 3; ++i) {
            FbTaskCreator(txn, TaskState::Opened).source("fbapi-samsara").create();
        }
        txn.commit();
    }

    //
    pqxx::work txn(conn);
    GatewayRO gatewayRo(txn);
    auto allTasksFilter = TaskFilter();
    {
        auto result = gatewayRo.tasksBriefByFilter(allTasksFilter, 0);
        UNIT_ASSERT_EQUAL(result.tasks.size(), 0);
        UNIT_ASSERT_EQUAL(result.hasMore, HasMore::Yes);
    }
    {
        auto result = gatewayRo.tasksBriefByFilter(allTasksFilter, 1);
        UNIT_ASSERT_EQUAL(result.tasks.size(), 1);
        UNIT_ASSERT_EQUAL(result.hasMore, HasMore::Yes);
    }
    {
        auto result = gatewayRo.tasksBriefByFilter(allTasksFilter, 2);
        UNIT_ASSERT_EQUAL(result.tasks.size(), 2);
        UNIT_ASSERT_EQUAL(result.hasMore, HasMore::Yes);
    }
    {
        auto result = gatewayRo.tasksBriefByFilter(allTasksFilter, 3);
        UNIT_ASSERT_EQUAL(result.tasks.size(), 3);
        UNIT_ASSERT_EQUAL(result.hasMore, HasMore::No);
    }
    {
        auto result = gatewayRo.tasksBriefByFilter(allTasksFilter, std::nullopt);
        UNIT_ASSERT_EQUAL(result.tasks.size(), 3);
        UNIT_ASSERT_EQUAL(result.hasMore, HasMore::No);
    }
}

Y_UNIT_TEST_F(filter_base_dimensions, DbFixture)
{
    pqxx::work txn(conn);
    Agent agent(txn, USER_ID);

    FbTaskCreator fbCreator(txn, TaskState::Opened);

    std::array taskIds = {
        fbCreator.type(Type::Subway).source("fbapi").create().id(),
        fbCreator.type(Type::Poi).source("fbapi").create().id(),
        fbCreator.type(Type::Address).source("not-fbapi").hidden(true).create().id(),
    };

    GatewayRO& gw = agent.gatewayRo();

    // no baseDimensions
    {
        TaskFilter filter;
        UNIT_ASSERT_VALUES_EQUAL(
            gw.taskIdsByFilter(filter),
            TIds({taskIds[0], taskIds[1], taskIds[2]})
        );
    }

    // one baseDimensions
    {
        TaskFilter filter;
        filter.addBaseDimensions(BaseDimensions().types({{Type::Subway}}));
        UNIT_ASSERT_VALUES_EQUAL(
            gw.taskIdsByFilter(filter),
            TIds({taskIds[0]})
        );
    }

    // two baseDimensions
    {
        TaskFilter filter;
        filter.addBaseDimensions(BaseDimensions().types({{Type::Subway}}));
        filter.addBaseDimensions(BaseDimensions()
            .types({{Type::Poi, Type::Fence}})
            .sources({{"fbapi", "fbapi-samsara"}})
            .workflows({{Workflow::Feedback, Workflow::Task}})
            .hidden(false)
        );
        UNIT_ASSERT_VALUES_EQUAL(
            gw.taskIdsByFilter(filter),
            TIds({taskIds[0], taskIds[1]})
        );
    }

    // baseDimensions + other filters
    {
        TaskFilter filter;
        filter.types({{Type::Subway, Type::Poi, Type::Address, Type::Barrier}});
        filter.addBaseDimensions(BaseDimensions()
            .types({{Type::Subway, Type::Address}})
        );
        UNIT_ASSERT_VALUES_EQUAL(
            gw.taskIdsByFilter(filter),
            TIds({taskIds[0], taskIds[2]})
        );

        // Associative property
        filter.types({{Type::Subway, Type::Poi, Type::Address, Type::Barrier}});
        UNIT_ASSERT_VALUES_EQUAL(
            gw.taskIdsByFilter(filter),
            TIds({taskIds[0], taskIds[2]})
        );
    }

    // hidden only
    {
        TaskFilter filter;
        filter.addBaseDimensions(BaseDimensions()
            .types({{Type::Poi, Type::Address}})
            .hidden(true)
        );
        UNIT_ASSERT_VALUES_EQUAL(
            gw.taskIdsByFilter(filter),
            TIds({taskIds[2]})
        );
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::social::tests
