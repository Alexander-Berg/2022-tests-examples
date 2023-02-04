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

/*!
  Check that all good if huge number if requests are added for one id
  in same moment
*/
class SameMomentRequestsChecker : public TaskChecker
{
public:
    SameMomentRequestsChecker(const boost::posix_time::ptime& start,
            size_t deleteType,
            size_t numRequests,
            size_t timeDelay)
        :start_(start),
         deleteType_(deleteType),
         numRequests_(numRequests),
         timeDelay_(timeDelay) { }

    void check(const ProcessedInfo& info) const override
    {
        SAFE_CHECK_EQUAL(info.size(), deleteType_ + 1 + numRequests_);
        //it is task which was passed to createNewProcessedInfo
        const Task& firstTask = info[0];
        size_t id = firstTask.id();
        SAFE_CHECK(firstTask == info[1]);
        for(size_t i = 0; i + 1 < info.size(); ++i) {
            const Task& task = info[i + 1];
            //First numRequests_ task has type 0, then 1, 2, ..., deleteType_
            size_t type = i < numRequests_ ? 0 : i - numRequests_ + 1;
            SAFE_CHECK_EQUAL(task.type(), type);
            SAFE_CHECK_EQUAL(task.id(), id);
            //We accept if time is greater for type > 0
            boost::posix_time::ptime correctTime = start_ +
                boost::posix_time::seconds(task.type() * timeDelay_);
            if (task.type() > 0) {
                SAFE_CHECK(task.time() >= correctTime);
            } else {
                SAFE_CHECK_EQUAL(task.time(), correctTime);
            }
            const std::vector<size_t>& numbers = task.numbers();
            if (task.type() == 0) {
                //there is exactly one number equal to id
                SAFE_CHECK_EQUAL(numbers.size(), 1);
                SAFE_CHECK_EQUAL(numbers[0], id + i * ID_MODULE);
            } else {
                SAFE_CHECK(numbers.empty());
            }
        }
    }
private:
    boost::posix_time::ptime start_;
    size_t deleteType_;
    size_t numRequests_;
    size_t timeDelay_;
};

void addRequests(rtp::TaskPool<UserTypes>& handler, size_t count)
{
    for(size_t i = 0; i < count; ++i) {
        handler.addRequest(selectId(i), i);
    }
}

void waitProcessing(rtp::TaskPool<UserTypes>& handler)
{
    while (!handler.statistic().allProcessed()) {
        sleep(1);
    }
}

void run(rtp::TaskPool<UserTypes>& handler, size_t count)
{
    addRequests(handler, count);
    waitProcessing(handler);
}

void checkSameMomentRequestsWithRestart(size_t numRequests,
        size_t threadsNumber, size_t timeDelay = 1)
{
    std::cerr << "Checking " << numRequests << " requests in "
        << threadsNumber << " threads with delay " << timeDelay <<
        " seconds\n";
    const size_t DELETE_TYPE = 3;
    // This test assumes that all requests are added before any task proceeds
    // to the next task type, which is not guaranteed in general.
    // So delay tasks execution by 1 second to give the test time to add requests.
    boost::posix_time::ptime start = maps::nowUtc() + boost::posix_time::seconds(1);
    SameMomentRequestsChecker checker(start, DELETE_TYPE,
            numRequests, timeDelay);
    TaskStoreProcessor processor(DELETE_TYPE, &checker);
    IncrementTaskScheduler scheduler(start, DELETE_TYPE, timeDelay);
    rtp::TaskPool<UserTypes> handler(processor,
            scheduler,
            threadsNumber);
    run(handler, ID_MODULE * numRequests);
}

void checkSameMomentRequestsWithRecover(size_t numRequests,
        size_t threadsNumber, size_t timeDelay = 1)
{
    std::cerr << "Checking " << numRequests << " requests in "
        << threadsNumber << " threads with delay " << timeDelay <<
        " seconds and recovering\n";
    const size_t DELETE_TYPE = 3;
    // This test assumes that all requests are added before any task proceeds
    // to the next task type, which is not guaranteed in general.
    // So delay tasks execution by 1 second to give the test time to add requests.
    boost::posix_time::ptime start = maps::nowUtc() + boost::posix_time::seconds(1);
    SameMomentRequestsChecker checker(start, DELETE_TYPE,
            numRequests, timeDelay);
    TaskStoreProcessor processor(DELETE_TYPE, &checker);
    IncrementTaskScheduler scheduler(start, DELETE_TYPE, timeDelay);


    std::unique_ptr<rtp::TaskPool<UserTypes> > handlerStart(
            new rtp::TaskPool<UserTypes>(processor,
            scheduler,
            threadsNumber));
    addRequests(*handlerStart, ID_MODULE * numRequests);

    const size_t NUM_STOPS = 5;
    for(size_t i = 0; i < NUM_STOPS; ++i) {
        std::unique_ptr<rtp::TaskPool<UserTypes>::Dump> dump =
            handlerStart->dump();

        scheduler.unsetEmptyState();
        std::unique_ptr<rtp::TaskPool<UserTypes> > handlerContinue(
            new rtp::TaskPool<UserTypes>(processor,
            scheduler,
            threadsNumber,
            std::move(dump)));

        std::swap(handlerStart, handlerContinue);
    }
    waitProcessing(*handlerStart);
}

BOOST_AUTO_TEST_CASE(one_task_test)
{
    checkSameMomentRequestsWithRestart(1, 1, 1);
}

BOOST_AUTO_TEST_CASE(many_tasks_test)
{
    checkSameMomentRequestsWithRestart(1 << 10, 1, 1);
    const size_t MAX_THREADS = 12;
    checkSameMomentRequestsWithRestart(1 << 15, MAX_THREADS, 1);
}

BOOST_AUTO_TEST_CASE(many_tasks_test_with_dump_recovering)
{
    checkSameMomentRequestsWithRecover(1 << 15, 12, 1);
}
