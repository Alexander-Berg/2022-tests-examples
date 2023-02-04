#define BOOST_TEST_MAIN

#include "tools.h"
#include "user_types.h"
#include "task_processor.h"
#include "task_scheduler.h"
#include "task_checker.h"

#include <maps/analyzer/libs/rtp/include/task_pool.h>

#include <boost/test/included/unit_test.hpp>
#include <iostream>

namespace rtp = maps::analyzer::rtp;

struct UserTypesWithState
{
    typedef size_t RequestObject;
    typedef size_t TaskId;
    typedef ::Task Task;
    typedef std::multiset<RequestObject> RequestsQueue;
    typedef ::ProcessedInfo ProcessedInfo;
    typedef IncrementTaskScheduler Scheduler;
    typedef TaskStoreProcessor Processor;
    typedef TestGlobalState GlobalState;
};

class NoCheck : public TaskChecker
{
    void check(const ProcessedInfo&) const override
    { /*do nothing*/ }
};

template <class TaskPool>
void addRequests(TaskPool& handler, size_t count)
{
    for(size_t i = 0; i < count; ++i) {
        handler.addRequest(selectId(i), i);
    }
}

BOOST_AUTO_TEST_CASE(empty_global_state)
{
    const size_t DELETE_TYPE = 3;
    boost::posix_time::ptime start = maps::nowUtc();
    NoCheck checker;
    TaskStoreProcessor processor(DELETE_TYPE, &checker);
    IncrementTaskScheduler scheduler(start, DELETE_TYPE, 0);
    rtp::TaskPool<UserTypes> handler(processor,
            scheduler,
            3);
    addRequests(handler, 10000);
    for(int _ = 0; _ < 100; ++_) {
        handler.userGlobalState();//just access on load
    }
    handler.stop();
    BOOST_CHECK(scheduler.testState() == 0);
    BOOST_CHECK(scheduler.emptyState() != 0);

    handler.userGlobalState();//just access
}

void checkState(const TestGlobalState& state, size_t count)
{
    BOOST_CHECK_EQUAL(state.ids.size(), count);
    BOOST_CHECK_EQUAL(state.requests.size(), count);
    for(size_t i = 0; i < count; ++i) {
        BOOST_CHECK_EQUAL(state.ids[i], selectId(i));
        BOOST_CHECK_EQUAL(state.requests[i], i);
    }
}

void checkNonEmptyState(TestGlobalState state, size_t count)
{
    BOOST_CHECK_EQUAL(state.ids[0], 1);
    BOOST_CHECK_EQUAL(state.requests[0], 3);
    state.ids.erase(state.ids.begin());
    state.requests.erase(state.requests.begin());
    checkState(state, count);
}

BOOST_AUTO_TEST_CASE(test_global_state)
{
    const size_t DELETE_TYPE = 3;
    boost::posix_time::ptime start = maps::nowUtc();
    NoCheck checker;
    TaskStoreProcessor processor(DELETE_TYPE, &checker);
    IncrementTaskScheduler scheduler(start, DELETE_TYPE, 0);
    rtp::TaskPool<UserTypesWithState> handler(processor,
            scheduler,
            3);
    const size_t COUNT = 10000;
    addRequests(handler, COUNT);
    BOOST_CHECK(scheduler.testState() != 0);
    BOOST_CHECK(scheduler.emptyState() == 0);
    for(int _ = 0; _ < 100; ++_) {
        checkState(handler.userGlobalState(), COUNT);
        checkState(*scheduler.testState(), COUNT);
    }
}

BOOST_AUTO_TEST_CASE(test_non_empty_global_state)
{
    const size_t DELETE_TYPE = 3;
    boost::posix_time::ptime start = maps::nowUtc();
    NoCheck checker;
    TaskStoreProcessor processor(DELETE_TYPE, &checker);
    IncrementTaskScheduler scheduler(start, DELETE_TYPE, 0);
    TestGlobalState state;
    state.ids.push_back(1);
    state.requests.push_back(3);
    rtp::TaskPool<UserTypesWithState> handler(processor,
            scheduler,
            3,
            state);
    const size_t COUNT = 10000;
    addRequests(handler, COUNT);
    BOOST_CHECK(scheduler.testState() != 0);
    BOOST_CHECK(scheduler.emptyState() == 0);
    for(int _ = 0; _ < 100; ++_) {
        checkNonEmptyState(handler.userGlobalState(), COUNT);
        checkNonEmptyState(*scheduler.testState(), COUNT);
    }
}

