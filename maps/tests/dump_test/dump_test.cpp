#define BOOST_TEST_MAIN

#include "user_types.h"
#include "task_processor.h"
#include "task_scheduler.h"
#include "task_checker.h"

#include <maps/analyzer/libs/rtp/include/task_pool.h>

#include <boost/test/included/unit_test.hpp>
#include <iostream>

namespace rtp = maps::analyzer::rtp;

class NoCheck : public TaskChecker
{
    void check(const ProcessedInfo&) const override
    { /*do nothing*/ }
};

void checkNotProcessedDumpObject(
    const std::multiset<size_t>* requests,
    const ProcessedInfo* info,
    const Task* task,
    size_t id,
    size_t numRequests,
    const boost::posix_time::ptime& start)
{
    BOOST_CHECK(!info);
    BOOST_CHECK_EQUAL(numRequests - 1, requests->size());
    BOOST_CHECK_EQUAL(id, task->id());
    BOOST_CHECK_EQUAL(0, task->type());
    BOOST_CHECK_EQUAL(start, task->time());
    BOOST_CHECK_EQUAL(1, task->numbers().size());
}

void dumpTest(bool stopBeforeDump)
{
    const size_t DELETE_TYPE = 3;
    boost::posix_time::ptime start = maps::nowUtc() +
        boost::posix_time::seconds(10); //Delay start of processing
    NoCheck checker;
    TaskStoreProcessor processor(DELETE_TYPE, &checker);
    IncrementTaskScheduler scheduler(start, DELETE_TYPE, 0);
    rtp::TaskPool<UserTypes> handler(processor,
            scheduler,
            3);
    //Add ten requests for all Ids
    const int NUM_REQUESTS = 10;
    for(size_t i = 0; i < NUM_REQUESTS * ID_MODULE; ++i) {
        handler.addRequest(selectId(i), i);
    }

    if (stopBeforeDump) {
        handler.stop();
    }

    typedef rtp::TaskPool<UserTypes>::Dump Dump;
    std::unique_ptr<Dump> wholeDump = handler.dump();

    //check const accessors
    for(size_t i = 0; i < wholeDump->size(); ++i) {
        Dump::ConstDumpObject dump = wholeDump->at(i);
        checkNotProcessedDumpObject(
                dump.unprocessedRequests,
                dump.processedInfo,
                dump.unprocessedTask,
                i,
                NUM_REQUESTS,
                start);
    }
    //check release
    for(size_t i = 0; i < wholeDump->size(); ++i) {
        Dump::DumpObjectPtr dump = wholeDump->release(i);
        checkNotProcessedDumpObject(
                &dump->unprocessedRequests,
                dump->processedInfo.get(),
                &dump->unprocessedTask,
                i,
                NUM_REQUESTS,
                start);
    }
}

void checkProcessedDumpObject(
    const std::multiset<size_t>* requests,
    const ProcessedInfo* info,
    const Task* task,
    size_t id,
    size_t numRequests,
    const boost::posix_time::ptime& taskTime)
{
    BOOST_CHECK(info);

    //all processed
    BOOST_CHECK_EQUAL(info->size(), numRequests + 1/*firstTask*/);
    BOOST_CHECK_EQUAL(requests->size(), 0);

    BOOST_CHECK_EQUAL(id, task->id());
    BOOST_CHECK_EQUAL(1, task->type());
    BOOST_CHECK_EQUAL(taskTime, task->time());
    BOOST_CHECK_EQUAL(0, task->numbers().size());
}

void waitWhileProcessingRequests(const rtp::TaskPool<UserTypes>& handler)
{
    while (handler.statistic().unprocessedRequestsNumber > 0) {
        sleep(1);
    }
}

void processedDumpTest()
{
    const size_t DELETE_TYPE = 3;
    boost::posix_time::ptime start = maps::nowUtc();
    NoCheck checker;
    TaskStoreProcessor processor(DELETE_TYPE, &checker);
    const size_t DELAY = 1000;//big delay to avoid process more, than one task
    IncrementTaskScheduler scheduler(start, DELETE_TYPE, DELAY);
    rtp::TaskPool<UserTypes> handler(processor,
            scheduler,
            12);
    //Add two requests for all Ids
    const size_t NUM_REQUESTS = 2;
    for(size_t i = 0; i < NUM_REQUESTS * ID_MODULE; ++i) {
        handler.addRequest(selectId(i), i);
    }

    waitWhileProcessingRequests(handler);

    typedef rtp::TaskPool<UserTypes>::Dump Dump;
    std::unique_ptr<Dump> wholeDump = handler.dump();

    //check const accessors
    for(size_t i = 0; i < wholeDump->size(); ++i) {
        Dump::ConstDumpObject dump = wholeDump->at(i);
        checkProcessedDumpObject(
                dump.unprocessedRequests,
                dump.processedInfo,
                dump.unprocessedTask,
                i,
                NUM_REQUESTS,
                start + boost::posix_time::seconds(DELAY));
    }
    //check release
    for(size_t i = 0; i < wholeDump->size(); ++i) {
        Dump::DumpObjectPtr dump = wholeDump->release(i);
        checkProcessedDumpObject(
                &dump->unprocessedRequests,
                dump->processedInfo.get(),
                &dump->unprocessedTask,
                i,
                NUM_REQUESTS,
                start + boost::posix_time::seconds(DELAY));
    }
}

BOOST_AUTO_TEST_CASE(dump_test)
{
    dumpTest(false);
    dumpTest(true);
}

BOOST_AUTO_TEST_CASE(processed_dump_test)
{
    processedDumpTest();
}

