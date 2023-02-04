#include "task.h"
#include "task_processor.h"

#include <maps/analyzer/libs/rtp/include/task_pool.h>
#include <maps/analyzer/libs/rtp/include/user_global_state.h>

#include <boost/test/unit_test.hpp>


namespace {

using RequestsQueue = std::multiset<boost::posix_time::ptime>;

class Scheduler
{
public:
    void setGlobalState(maps::analyzer::rtp::EmptyUserGlobalState*) { }

    Task create(size_t id, RequestsQueue* unprocessedRequests) const
    {
        ASSERT(!unprocessedRequests->empty());
        Task task(0, id, *unprocessedRequests->begin());
        unprocessedRequests->erase(unprocessedRequests->begin());
        return task;
    }

    void update(Task*, const ProcessedInfo*, RequestsQueue*) const { }

    std::optional<Task> next(const Task&, const ProcessedInfo&, RequestsQueue*) const
    {
        return std::nullopt;
    }
};

struct UserTypes
{
    using RequestObject = boost::posix_time::ptime;
    using TaskId = size_t;
    using Task = ::Task;
    using RequestsQueue = RequestsQueue;
    using ProcessedInfo = ::ProcessedInfo;
    using Scheduler = Scheduler;
    using Processor = TaskStoreProcessor;
    using GlobalState = maps::analyzer::rtp::EmptyUserGlobalState;
};

boost::posix_time::ptime scheduleTimeForTask(int id, boost::posix_time::ptime start)
{
    // Make this gap large to make sure that if the age of a wrong task is returned, we notice it.
    boost::posix_time::seconds timeGapBetweenTasks(100);
    return start + timeGapBetweenTasks * id;
}

boost::posix_time::time_duration absTimeDuration(boost::posix_time::time_duration x)
{
    return x.is_negative() ? x.invert_sign() : x;
}

} // namespace

BOOST_AUTO_TEST_CASE(oldest_task_age)
{
    TaskStoreProcessor processor(3, nullptr);
    Scheduler scheduler;
    maps::analyzer::rtp::TaskPool<UserTypes> taskPool(processor, scheduler, 32);

    boost::posix_time::seconds firstTaskOffset(10000);
    boost::posix_time::ptime start = maps::nowUtc();
    // I want the "oldest" task to be neither the first one nor the last one to catch stupid bugs
    // like e.g. when requests are processed in order of inserts.
    for (int i = 500; i >= 0; --i) {
        taskPool.addRequest(i, scheduleTimeForTask(i, start + firstTaskOffset));
    }
    for (int i = 501; i < 1000; ++i) {
        taskPool.addRequest(i, scheduleTimeForTask(i, start + firstTaskOffset));
    }
    // All tasks are scheduled in the future, so oldestTaskAge must be negative, and its absolute
    // value must be really close to firstTaskOffset minus the time spent on scheduling requests.
    auto expectedTaskAge = -firstTaskOffset + (maps::nowUtc() - start);
    auto oldestTaskAge = taskPool.statistic().oldestTaskAge;
    BOOST_REQUIRE(!oldestTaskAge.is_special());
    BOOST_CHECK_LT(
        absTimeDuration(expectedTaskAge - oldestTaskAge),
        boost::posix_time::seconds(5));
}
