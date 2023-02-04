#include <library/cpp/testing/unittest/registar.h>
#include <yandex/maps/wiki/social/feedback/agent.h>

#include "feedback_helpers.h"
#include "helpers.h"

namespace maps::wiki::social::tests {

using namespace feedback;

namespace {


} // unnames namespase

Y_UNIT_TEST_SUITE(feedback_personal_feed) {

Y_UNIT_TEST_F(feed_params_date_and_id, DbFixture)
{
    std::vector<TId> taskIds;

    // create 5 tasks
    {
        pqxx::work txn(conn);
        Agent agent(txn, USER_ID);

        for (int i = 0; i < 5; ++i) {
            taskIds.emplace_back(createRevealedTask(agent));
        }
        taskIds.emplace_back(createTaskNeedInfoAvailable(agent));
        txn.commit();
    }

    // modify tasks
    {
        pqxx::work txn(conn);
        Agent agent(txn, USER2_ID);

        agent.resolveTaskCascade(
            getTaskForUpdate(agent, taskIds.at(1)),
            Resolution::createRejected(RejectReason::NoData));
        txn.commit();
    }
    sleep(1);
    {
        pqxx::work txn(conn);
        {
            Agent agent(txn, USER2_ID);

            agent.resolveTaskCascade(
                getTaskForUpdate(agent, taskIds.at(2)),
                Resolution::createAccepted());
            agent.needInfoTask(
                getTaskForUpdate(agent, taskIds.at(5)),
                0);
        }
        {
            Agent agent(txn, USER_ID);

            agent.resolveTaskCascade(
                getTaskForUpdate(agent, taskIds.at(3)),
                Resolution::createRejected(RejectReason::Spam));
        }
        txn.commit();
    }
    sleep(1);
    {
        pqxx::work txn(conn);
        Agent agent(txn, USER2_ID);

        agent.processingLevelUp(
            getTaskForUpdate(agent, taskIds.at(0)),
            Verdict::Rejected);
        agent.hideTask(getTaskForUpdate(agent, taskIds.at(4)));
        txn.commit();
    }

    // personal feed for USER_ID: 3
    // personal feed for USER2_ID: 0, 2, 5, 1
    // <4> is out of any personal feed
    pqxx::work txn(conn);
    GatewayRO gatewayRo(txn);

    TaskFilter filter;
    filter.modifiedBy(USER2_ID);

    // test feed count
    {
        auto count = gatewayRo.getTotalCountOfTasks(filter);
        UNIT_ASSERT_EQUAL(count, 4);
    }

    // test after-feed (forward direction)
    {
        TaskFeedParamsDateAndId feedParams(std::nullopt, std::nullopt, 2);
        auto feed = gatewayRo.tasksFeed(filter, feedParams);

        UNIT_ASSERT_EQUAL(feed.tasks.size(), 2);
        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::Yes);
        UNIT_ASSERT_EQUAL(feed.tasks.at(0).id(), taskIds.at(0));
        UNIT_ASSERT_EQUAL(feed.tasks.at(1).id(), taskIds.at(5));
    }
    {
        auto task = getTask(gatewayRo, taskIds.at(0));
        TaskFeedParamsDateAndId feedParams(
            std::nullopt,
            DateAndId(task.stateModifiedAt(), task.id()),
            4);
        auto feed = gatewayRo.tasksFeed(filter, feedParams);

        UNIT_ASSERT_EQUAL(feed.tasks.size(), 3);
        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::No);
        UNIT_ASSERT_EQUAL(feed.tasks.at(0).id(), taskIds.at(5));
        UNIT_ASSERT_EQUAL(feed.tasks.at(1).id(), taskIds.at(2));
        UNIT_ASSERT_EQUAL(feed.tasks.at(2).id(), taskIds.at(1));
    }
    {
        auto task = getTask(gatewayRo, taskIds.at(5));
        TaskFeedParamsDateAndId feedParams(
            std::nullopt,
            DateAndId(task.stateModifiedAt(), task.id()),
            1);
        auto feed = gatewayRo.tasksFeed(filter, feedParams);

        UNIT_ASSERT_EQUAL(feed.tasks.size(), 1);
        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::Yes);
        UNIT_ASSERT_EQUAL(feed.tasks.at(0).id(), taskIds.at(2));
    }

    // test before-feed (backward direction)
    {
        auto task = getTask(gatewayRo, taskIds.at(5));
        TaskFeedParamsDateAndId feedParams(
            DateAndId(task.stateModifiedAt(), task.id()),
            std::nullopt,
            2);
        auto feed = gatewayRo.tasksFeed(filter, feedParams);

        UNIT_ASSERT_EQUAL(feed.tasks.size(), 1);
        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::No);
        UNIT_ASSERT_EQUAL(feed.tasks.at(0).id(), taskIds.at(0));
    }
       {
        auto task = getTask(gatewayRo, taskIds.at(1));
        TaskFeedParamsDateAndId feedParams(
            DateAndId(task.stateModifiedAt(), task.id()),
            std::nullopt,
            2);
        auto feed = gatewayRo.tasksFeed(filter, feedParams);

        UNIT_ASSERT_EQUAL(feed.tasks.size(), 2);
        UNIT_ASSERT_EQUAL(feed.hasMore, HasMore::Yes);
        UNIT_ASSERT_EQUAL(feed.tasks.at(0).id(), taskIds.at(5));
        UNIT_ASSERT_EQUAL(feed.tasks.at(1).id(), taskIds.at(2));
    }
}

}

} // namespace maps::wiki::social::feedback::tests
