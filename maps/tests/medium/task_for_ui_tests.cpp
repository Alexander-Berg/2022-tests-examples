#include <maps/wikimap/mapspro/libs/social_serv_serialize/tests/medium/db_fixture.h>
#include <library/cpp/testing/unittest/registar.h>
#include <yandex/maps/wiki/social/feedback/agent.h>
#include <yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <maps/wikimap/mapspro/libs/social_serv_serialize/include/task_for_ui.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/attribute_names.h>
#include <util/stream/output.h>

#include <maps/wikimap/mapspro/libs/common/include/yandex/maps/wiki/common/robot.h>

#include <yandex/maps/wiki/social/feedback/task_new.h>

#include <vector>

template <>
void Out<maps::wiki::socialsrv::serialize::HistoryEventUI>(
    IOutputStream& os,
    const maps::wiki::socialsrv::serialize::HistoryEventUI& uiEvent)
{
    os << "operation: " << uiEvent.historyItem.operation()
        << ", modifiedBy: " << uiEvent.historyItem.modifiedBy();
}

namespace maps::wiki::socialsrv::serialize {

bool operator==(const HistoryEventUI& left, const HistoryEventUI& rigth)
{
    return left.historyItem.operation() == rigth.historyItem.operation()
        && left.historyItem.modifiedBy() == rigth.historyItem.modifiedBy();
}

namespace tests {

namespace sf = social::feedback;

namespace {

struct User {
    social::TUid uid;
    SubstitutionStrategy strategy;
};

const User COMMON_USER{1, SubstitutionStrategy::ForZeroProcessingLine};
const User ANOTHER_COMMON_USER{2, SubstitutionStrategy::ForZeroProcessingLine};

const User CARTOGRAPHER{1000, SubstitutionStrategy::None};

const social::Comments EMPTY_COMMENTS;
const social::TId SOME_COMMENT_ID = 42;

class TxnCommitter {
public:
    TxnCommitter(pqxx::connection& connection): txn_(connection) {
        sleep(1);
    }

    ~TxnCommitter() {
        txn_.commit();
    }

    pqxx::work& operator*() { return txn_; }
private:
    pqxx::work txn_;
};

void pushBack(HistoryEventsUI& userEvents, sf::TaskOperation operation, social::TUid uid)
{
    social::feedback::HistoryItem historyItem(
        maps::chrono::TimePoint::clock::now(),
        uid,
        operation,
        {},
        std::nullopt);
    userEvents.emplace_back(historyItem);
}

} // namespace

Y_UNIT_TEST_SUITE(task_for_ui)
{

Y_UNIT_TEST_F(just_leveled_up_task_for_ui_test, DbFixture)
{
    HistoryEventsUI commonUserUiEvents;
    social::TId taskId;
    {
        TxnCommitter txn{conn};
        sf::Agent agent(*txn, common::ROBOT_UID);
        const auto taskForUpdate = agent.addTask(sf::TaskNew(
            geolib3::Point2(0, 0),
            sf::Type::Poi,
            "some_task_source",
            sf::Description("some_description")));
        taskId = taskForUpdate.id();

        agent.revealTaskByIdCascade(taskId);
        pushBack(commonUserUiEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.releaseTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createAccepted());
        pushBack(commonUserUiEvents, sf::TaskOperation::Accept, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.openTask(*agent.taskForUpdateById(taskId));
        pushBack(commonUserUiEvents, sf::TaskOperation::Open, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    auto cartographerUiEvents = commonUserUiEvents;
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.processingLevelUp(
            *agent.taskForUpdateById(taskId), sf::Verdict::Rejected);
        pushBack(commonUserUiEvents, sf::TaskOperation::Reject, COMMON_USER.uid);
        pushBack(cartographerUiEvents, sf::TaskOperation::ChangeProcessingLvl, COMMON_USER.uid);
    }

    pqxx::work txn(conn);
    sf::GatewayRO gw(txn);
    const auto task = gw.taskById(taskId);
    UNIT_ASSERT(task);
    UNIT_ASSERT(!task->resolved());
    UNIT_ASSERT_EQUAL(task->processingLevel(), 1);
    const auto history = gw.history(taskId);

    {
        const auto user = COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.state(), sf::TaskState::Rejected);
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT_EQUAL(taskForCommon.validOperations().size(), 1);
        UNIT_ASSERT(taskForCommon.validOperations().count(sf::TaskOperation::Open));
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserUiEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution.verdict(), sf::Verdict::Rejected);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, COMMON_USER.uid);
    }
    {
        const auto user = ANOTHER_COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.state(), sf::TaskState::Rejected);
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT(taskForCommon.validOperations().empty());
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserUiEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution.verdict(), sf::Verdict::Rejected);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, COMMON_USER.uid);
    }
    {
        const auto user = CARTOGRAPHER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCartographer = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCartographer.state(), sf::TaskState::Opened);
        UNIT_ASSERT_EQUAL(taskForCartographer.processingLevel(), 1);
        UNIT_ASSERT_EQUAL(taskForCartographer.validOperations(), validOperations);

        UNIT_ASSERT_VALUES_EQUAL(taskForCartographer.historyEventsUI(), cartographerUiEvents);

        UNIT_ASSERT(!taskForCartographer.resolved());
    }
}

Y_UNIT_TEST_F(real_resolution_equals_suggested_resolution_test, DbFixture)
{
    HistoryEventsUI commonUserUiEvents;
    HistoryEventsUI fullUiEvents;
    social::TId taskId;
    {
        TxnCommitter txn{conn};
        sf::Agent agent(*txn, common::ROBOT_UID);
        auto taskNew = sf::TaskNew(
            geolib3::Point2(0, 0),
            sf::Type::Poi,
            "fbapi",
            sf::Description("some_description"));
        taskNew.attrs.addCustom(sf::attrs::USER_EMAIL, "mail@mail.ma");
        const auto taskForUpdate = agent.addTask(taskNew);
        taskId = taskForUpdate.id();

        agent.revealTaskByIdCascade(taskId);
        pushBack(commonUserUiEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
        pushBack(fullUiEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.processingLevelUp(
            *agent.taskForUpdateById(taskId), sf::Verdict::Accepted);
        pushBack(commonUserUiEvents, sf::TaskOperation::Accept, COMMON_USER.uid);
        pushBack(fullUiEvents, sf::TaskOperation::ChangeProcessingLvl, COMMON_USER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createRejected(sf::RejectReason::IncorrectData));
        pushBack(fullUiEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.openTask(*agent.taskForUpdateById(taskId));
        pushBack(fullUiEvents, sf::TaskOperation::Open, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        UNIT_ASSERT(agent.needInfoTask(
            *agent.taskForUpdateById(taskId), SOME_COMMENT_ID));
        pushBack(fullUiEvents, sf::TaskOperation::NeedInfo, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.openTask(*agent.taskForUpdateById(taskId));
        pushBack(fullUiEvents, sf::TaskOperation::Open, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId), sf::Resolution::createAccepted());
        pushBack(fullUiEvents, sf::TaskOperation::Accept, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, common::ROBOT_UID};
        agent.deployTaskByIdCascade(taskId, maps::chrono::TimePoint::clock::now());
        pushBack(commonUserUiEvents, sf::TaskOperation::Deploy, common::ROBOT_UID);
        pushBack(fullUiEvents, sf::TaskOperation::Deploy, common::ROBOT_UID);
    }

    pqxx::work txn(conn);
    sf::GatewayRO gw(txn);
    const auto task = gw.taskById(taskId);
    UNIT_ASSERT(task);
    UNIT_ASSERT(task->resolved());
    UNIT_ASSERT_EQUAL(task->resolved()->uid, CARTOGRAPHER.uid);
    UNIT_ASSERT_EQUAL(task->processingLevel(), 1);
    const auto history = gw.history(taskId);

    {
        const auto user = COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.state(), sf::TaskState::Deployed);
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT(taskForCommon.validOperations().empty());
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserUiEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution.verdict(), sf::Verdict::Accepted);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, COMMON_USER.uid);
    }
    {
        const auto user = ANOTHER_COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.state(), sf::TaskState::Deployed);
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT(taskForCommon.validOperations().empty());
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserUiEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution.verdict(), sf::Verdict::Accepted);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, COMMON_USER.uid);
    }
    {
        const auto user = CARTOGRAPHER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCartographer = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCartographer.state(), sf::TaskState::Deployed);
        UNIT_ASSERT_EQUAL(taskForCartographer.processingLevel(), 1);
        UNIT_ASSERT_EQUAL(taskForCartographer.validOperations(), validOperations);

        UNIT_ASSERT_VALUES_EQUAL(taskForCartographer.historyEventsUI(), fullUiEvents);

        UNIT_ASSERT(taskForCartographer.resolved());
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->resolution.verdict(), sf::Verdict::Accepted);
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->uid, CARTOGRAPHER.uid);
    }
}

Y_UNIT_TEST_F(valid_reasons_for_common_test, DbFixture)
{
    social::TId taskId;
    // create feedback Task
    {
        TxnCommitter txn{conn};
        sf::Agent agent(*txn, common::ROBOT_UID);
        auto taskNew = sf::TaskNew(
            geolib3::Point2(0, 0),
            sf::Type::Poi,
            "fbapi",
            sf::Description("some_description"));
        taskNew.attrs.addCustom(sf::attrs::USER_EMAIL, "mail@mail.ma");
        const auto taskForUpdate = agent.addTask(taskNew);
        taskId = taskForUpdate.id();

        agent.revealTaskByIdCascade(taskId);
    }
    // look at Task as common user
    {
        pqxx::work txn(conn);
        const auto user = COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto task = agent.gatewayRo().taskById(taskId);
        UNIT_ASSERT(task);
        const auto history = agent.gatewayRo().history(taskId);
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForUi = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_VALUES_EQUAL(taskForUi.validRejectReasons().size(), 0);
    }
    // look at Task as cartographer
    {
        pqxx::work txn(conn);
        const auto user = CARTOGRAPHER;
        sf::Agent agent{txn, user.uid};
        const auto task = agent.gatewayRo().taskById(taskId);
        UNIT_ASSERT(task);
        const auto history = agent.gatewayRo().history(taskId);
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForUi = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT(taskForUi.validRejectReasons().size() > 0);
        UNIT_ASSERT_VALUES_EQUAL(
            taskForUi.validRejectReasons().size(),
            sf::Agent::validRejectReasons(*task).size());
    }
}

Y_UNIT_TEST_F(real_resolution_not_equals_suggested_resolution_test, DbFixture)
{
    HistoryEventsUI commonUserEvents;
    HistoryEventsUI fullUiEvents;
    social::TId taskId;
    {
        TxnCommitter txn{conn};
        sf::Agent agent(*txn, common::ROBOT_UID);
        auto taskNew = sf::TaskNew(
            geolib3::Point2(0, 0),
            sf::Type::Poi,
            "fbapi",
            sf::Description("some_description"));
        taskNew.attrs.addCustom(sf::attrs::USER_EMAIL, "mail@mail.ma");
        const auto taskForUpdate = agent.addTask(taskNew);
        taskId = taskForUpdate.id();

        agent.revealTaskByIdCascade(taskId);
        pushBack(commonUserEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
        pushBack(fullUiEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.processingLevelUp(
            *agent.taskForUpdateById(taskId), sf::Verdict::Accepted);
        pushBack(commonUserEvents, sf::TaskOperation::Accept, COMMON_USER.uid);
        pushBack(fullUiEvents, sf::TaskOperation::ChangeProcessingLvl, COMMON_USER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createRejected(sf::RejectReason::Spam));
        pushBack(commonUserEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
        pushBack(fullUiEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.openTask(*agent.taskForUpdateById(taskId));
        pushBack(fullUiEvents, sf::TaskOperation::Open, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        UNIT_ASSERT(agent.needInfoTask(
            *agent.taskForUpdateById(taskId), SOME_COMMENT_ID));
        pushBack(fullUiEvents, sf::TaskOperation::NeedInfo, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.openTask(*agent.taskForUpdateById(taskId));
        pushBack(fullUiEvents, sf::TaskOperation::Open, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createRejected(sf::RejectReason::Spam));
        pushBack(fullUiEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, common::ROBOT_UID};
        agent.deployTaskByIdCascade(taskId, maps::chrono::TimePoint::clock::now());
        pushBack(commonUserEvents, sf::TaskOperation::Deploy, common::ROBOT_UID);
        pushBack(fullUiEvents, sf::TaskOperation::Deploy, common::ROBOT_UID);
    }

    pqxx::work txn(conn);
    sf::GatewayRO gw(txn);
    const auto task = gw.taskById(taskId);
    UNIT_ASSERT(task);
    UNIT_ASSERT(task->resolved());
    UNIT_ASSERT_EQUAL(task->resolved()->uid, CARTOGRAPHER.uid);
    UNIT_ASSERT_EQUAL(task->processingLevel(), 1);
    const auto history = gw.history(taskId);

    {
        const auto user = COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT(taskForCommon.validOperations().empty());
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution.verdict(), sf::Verdict::Rejected);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, CARTOGRAPHER.uid);
    }
    {
        const auto user = ANOTHER_COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT(taskForCommon.validOperations().empty());
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution.verdict(), sf::Verdict::Rejected);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, CARTOGRAPHER.uid);
    }
    {
        const auto user = CARTOGRAPHER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCartographer = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCartographer.processingLevel(), 1);
        UNIT_ASSERT_EQUAL(taskForCartographer.validOperations(), validOperations);

        UNIT_ASSERT_VALUES_EQUAL(taskForCartographer.historyEventsUI(), fullUiEvents);

        UNIT_ASSERT(taskForCartographer.resolved());
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->resolution.verdict(), sf::Verdict::Rejected);
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->uid, CARTOGRAPHER.uid);
    }
}

Y_UNIT_TEST_F(reopened_on_1st_level_test, DbFixture)
{
    HistoryEventsUI commonUserEvents;
    HistoryEventsUI fullUiEvents;
    std::optional<sf::TaskResolved> commonUserResolution;
    social::TId taskId;
    {
        TxnCommitter txn{conn};
        sf::Agent agent(*txn, common::ROBOT_UID);
        auto taskNew = sf::TaskNew(
            geolib3::Point2(0, 0),
            sf::Type::Poi,
            "fbapi",
            sf::Description("some_description"));
        taskNew.attrs.addCustom(sf::attrs::USER_EMAIL, "mail@mail.ma");
        const auto taskForUpdate = agent.addTask(taskNew);
        taskId = taskForUpdate.id();

        agent.revealTaskByIdCascade(taskId);
        pushBack(commonUserEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
        pushBack(fullUiEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        const auto task = agent.processingLevelUp(
            *agent.taskForUpdateById(taskId), sf::Verdict::Accepted);
        UNIT_ASSERT(task);
        const auto history = agent.gatewayRo().history(taskId);
        pushBack(commonUserEvents, sf::TaskOperation::Accept, COMMON_USER.uid);
        commonUserResolution = sf::TaskResolved{
            COMMON_USER.uid,
            history.items().back().modifiedAt(),
            sf::Resolution::createAccepted()
        };
        pushBack(fullUiEvents, sf::TaskOperation::ChangeProcessingLvl, COMMON_USER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createRejected(sf::RejectReason::NoInfo));
        pushBack(fullUiEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.openTask(*agent.taskForUpdateById(taskId));
        pushBack(fullUiEvents, sf::TaskOperation::Open, CARTOGRAPHER.uid);
    }

    pqxx::work txn(conn);
    sf::GatewayRO gw(txn);
    const auto task = gw.taskById(taskId);
    UNIT_ASSERT(task);
    UNIT_ASSERT(!task->resolved());
    UNIT_ASSERT_EQUAL(task->state(), sf::TaskState::Opened);
    const auto history = gw.history(taskId);

    {
        const auto user = COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT(taskForCommon.validOperations().empty());
        UNIT_ASSERT_EQUAL(taskForCommon.state(), sf::TaskState::Accepted);
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution, commonUserResolution->resolution);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, commonUserResolution->uid);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->date, commonUserResolution->date);
    }
    {
        const auto user = CARTOGRAPHER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCartographer = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT(!taskForCartographer.resolved());
        UNIT_ASSERT_EQUAL(taskForCartographer.state(), sf::TaskState::Opened);
        UNIT_ASSERT_EQUAL(taskForCartographer.processingLevel(), 1);
        UNIT_ASSERT_EQUAL(taskForCartographer.validOperations(), validOperations);

        UNIT_ASSERT_VALUES_EQUAL(taskForCartographer.historyEventsUI(), fullUiEvents);

        UNIT_ASSERT(!taskForCartographer.resolved());
    }
}

Y_UNIT_TEST_F(using_first_of_equal_final_resolution_test, DbFixture)
{
    HistoryEventsUI commonUserEvents;
    HistoryEventsUI fullUiEvents;
    std::optional<sf::TaskResolved> commonUserResolution;
    social::TId taskId;
    {
        TxnCommitter txn{conn};
        sf::Agent agent(*txn, common::ROBOT_UID);
        auto taskNew = sf::TaskNew(
            geolib3::Point2(0, 0),
            sf::Type::Poi,
            "fbapi",
            sf::Description("some_description"));
        taskNew.attrs.addCustom(sf::attrs::USER_EMAIL, "mail@mail.ma");
        const auto taskForUpdate = agent.addTask(taskNew);
        taskId = taskForUpdate.id();

        agent.revealTaskByIdCascade(taskId);
        pushBack(commonUserEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
        pushBack(fullUiEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, COMMON_USER.uid};
        const auto task = agent.processingLevelUp(
            *agent.taskForUpdateById(taskId), sf::Verdict::Accepted);
        UNIT_ASSERT(task);
        const auto history = agent.gatewayRo().history(taskId);
        pushBack(commonUserEvents, sf::TaskOperation::Accept, COMMON_USER.uid);
        commonUserResolution = sf::TaskResolved{
            COMMON_USER.uid,
            history.items().back().modifiedAt(),
            sf::Resolution::createAccepted()
        };
        pushBack(fullUiEvents, sf::TaskOperation::ChangeProcessingLvl, COMMON_USER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        const auto task = agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createRejected(sf::RejectReason::Spam));
        commonUserResolution = task->resolved();
        pushBack(commonUserEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
        pushBack(fullUiEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.openTask(*agent.taskForUpdateById(taskId));
        pushBack(fullUiEvents, sf::TaskOperation::Open, CARTOGRAPHER.uid);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createRejected(sf::RejectReason::NoInfo));
        pushBack(fullUiEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
    }

    pqxx::work txn(conn);
    sf::GatewayRO gw(txn);
    const auto task = gw.taskById(taskId);
    UNIT_ASSERT(task);
    const auto cartographerResolution = task->resolved();
    const auto history = gw.history(taskId);

    {
        const auto user = COMMON_USER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCommon = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_EQUAL(taskForCommon.processingLevel(), 0);
        UNIT_ASSERT(taskForCommon.validOperations().empty());
        UNIT_ASSERT_EQUAL(taskForCommon.state(), sf::TaskState::Rejected);
        UNIT_ASSERT_VALUES_EQUAL(taskForCommon.historyEventsUI(), commonUserEvents);

        UNIT_ASSERT(taskForCommon.resolved());
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->resolution, commonUserResolution->resolution);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->uid, commonUserResolution->uid);
        UNIT_ASSERT_EQUAL(taskForCommon.resolved()->date, commonUserResolution->date);
    }
    {
        const auto user = CARTOGRAPHER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCartographer = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT(taskForCartographer.resolved());
        UNIT_ASSERT_EQUAL(taskForCartographer.state(), sf::TaskState::Rejected);
        UNIT_ASSERT_EQUAL(taskForCartographer.processingLevel(), 1);
        UNIT_ASSERT_EQUAL(taskForCartographer.validOperations(), validOperations);

        UNIT_ASSERT_VALUES_EQUAL(taskForCartographer.historyEventsUI(), fullUiEvents);

        UNIT_ASSERT(taskForCartographer.resolved());
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->resolution, cartographerResolution->resolution);
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->uid, cartographerResolution->uid);
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->date, cartographerResolution->date);
    }
}

Y_UNIT_TEST_F(visible_created_operation_for_non_robots_test, DbFixture)
{
    HistoryEventsUI fullUiEvents;
    social::TId taskId;
    {
        TxnCommitter txn{conn};
        sf::Agent agent(*txn, COMMON_USER.uid);
        auto taskNew = sf::TaskNew(
            geolib3::Point2(0, 0),
            sf::Type::Poi,
            "fbapi",
            sf::Description("some_description"));
        taskNew.attrs.addCustom(sf::attrs::USER_EMAIL, "mail@mail.ma");
        const auto taskForUpdate = agent.addTask(taskNew);
        taskId = taskForUpdate.id();
        pushBack(fullUiEvents, sf::TaskOperation::Create, COMMON_USER.uid);

        sf::Agent agent2(*txn, common::ROBOT_UID);
        agent2.revealTaskByIdCascade(taskId);
        pushBack(fullUiEvents, sf::TaskOperation::Reveal, common::ROBOT_UID);
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.acquireTask(*agent.taskForUpdateById(taskId));
    }
    {
        TxnCommitter txn{conn};
        sf::Agent agent{*txn, CARTOGRAPHER.uid};
        agent.resolveTaskCascade(
            *agent.taskForUpdateById(taskId),
            sf::Resolution::createRejected(sf::RejectReason::IncorrectData));
        pushBack(fullUiEvents, sf::TaskOperation::Reject, CARTOGRAPHER.uid);
    }

    pqxx::work txn(conn);
    sf::GatewayRO gw(txn);
    const auto task = gw.taskById(taskId);
    UNIT_ASSERT(task);
    UNIT_ASSERT(task->resolved());
    UNIT_ASSERT_EQUAL(task->resolved()->uid, CARTOGRAPHER.uid);
    const auto history = gw.history(taskId);

    {
        const auto user = CARTOGRAPHER;
        sf::Agent agent{txn, user.uid};
        const auto& validOperations = agent.validOperations(*task, user.uid);
        const auto taskForCartographer = TaskForUI(
            *task,
            history,
            user.strategy,
            EMPTY_COMMENTS,
            validOperations,
            user.uid
        );
        UNIT_ASSERT_VALUES_EQUAL(taskForCartographer.historyEventsUI(), fullUiEvents);

        UNIT_ASSERT(taskForCartographer.resolved());
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->resolution.verdict(), sf::Verdict::Rejected);
        UNIT_ASSERT_EQUAL(taskForCartographer.resolved()->uid, CARTOGRAPHER.uid);
    }
}

} // Y_UNIT_TEST_SUITE(task_for_ui)


} //tests
} //maps::wiki::socialsrv::serialize
