#define BOOST_TEST_MAIN


#include <maps/analyzer/services/jams_analyzer/modules/usershandler/tests/test_tools/include/test_tools.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/task_scheduler.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/task.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/config.h>

#include <util/system/yassert.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/data/test_case.hpp>
#include <boost/test/test_tools.hpp>
#include <boost/assign.hpp>
#include <boost/date_time/posix_time/ptime.hpp>

#include <cassert>
#include <optional>
#include <set>
#include <vector>

namespace pt = boost::posix_time;
namespace ma = maps::analyzer;
namespace mad = ma::data;
using namespace boost::assign;

const pt::time_duration PROCESS_WAITING = pt::seconds(10);
const pt::time_duration SIGNALS_WINDOW = pt::seconds(60);
const pt::time_duration MAX_SIGNALS_GAP = pt::seconds(135);
const pt::time_duration STORE_DURATION = pt::seconds(2000);

mad::GpsSignal createSignal(pt::ptime time, pt::ptime receiveTime)
{
    mad::GpsSignal signal;
    signal.setReceiveTime(receiveTime);
    signal.setTime(time);
    signal.setVehicleId(ma::VehicleId("0", "1"));
    return signal;
}

Task createProcessingTask(const mad::GpsSignal& signal)
{
    Task task(
        TaskType::ProcessSignal,
        signal.vehicleId(),
        signal.receiveTime() + PROCESS_WAITING
    );
    task.addSignal(signal);
    return task;
}

BOOST_AUTO_TEST_CASE(createTest)
{
    pt::ptime ctime = maps::nowUtc();
    TaskScheduler scheduler(PROCESS_WAITING, SIGNALS_WINDOW, pt::seconds(2 * 60));
    SignalsQueue queue;
    mad::GpsSignal signal = createSignal(ctime, ctime);
    ma::VehicleId id = signal.vehicleId();
    BOOST_CHECK_THROW(scheduler.create(id, &queue), maps::Exception);
    queue.insert(signal);
    Task firstTask = createProcessingTask(signal);
    BOOST_CHECK_EQUAL(scheduler.create(id, &queue), firstTask);
    BOOST_CHECK(queue.empty());
    BOOST_CHECK_THROW(scheduler.create(id, &queue), maps::Exception);

    queue.insert(signal);
    mad::GpsSignal nextSignal = createSignal(
        ctime - pt::seconds(10),
        ctime + pt::seconds(10)
    );
    queue.insert(nextSignal);

    Task secondTask = createProcessingTask(nextSignal);
    BOOST_CHECK_EQUAL(scheduler.create(id, &queue), secondTask);
    BOOST_CHECK_EQUAL(queue.size(), 1);
    BOOST_CHECK_EQUAL(*queue.begin(), signal);
}

BOOST_AUTO_TEST_CASE(updateTest)
{
    pt::ptime ctime = maps::nowUtc();
    TaskScheduler scheduler(PROCESS_WAITING, SIGNALS_WINDOW, pt::seconds(2 * 60));
    SignalsQueue queue;

    mad::GpsSignal signal = createSignal(ctime, ctime);
    mad::GpsSignal elderSignal = createSignal(
        ctime  - pt::seconds(10),
        ctime + pt::seconds(10)
    );

    Task processTask = createProcessingTask(signal);

    Task forceMatchTask(
        TaskType::ForceMatch,
        signal.vehicleId(),
        ctime + PROCESS_WAITING
    );

    BOOST_CHECK_THROW(
        scheduler.update(&processTask, 0, &queue),
        maps::Exception
    );
    BOOST_CHECK_THROW(
        scheduler.update(&forceMatchTask, 0, &queue),
        maps::Exception
    );

    {
        queue.insert(signal);
        Task inQueue = forceMatchTask;
        scheduler.update(&inQueue, 0, &queue);
        BOOST_CHECK_EQUAL(inQueue, processTask);
        BOOST_CHECK(queue.empty());
    }
    {
        queue.insert(signal);
        Task inQueue = createProcessingTask(elderSignal);
        scheduler.update(&inQueue, 0, &queue);
        BOOST_CHECK_EQUAL(inQueue, createProcessingTask(elderSignal));
        BOOST_CHECK_EQUAL(queue.size(), 1);
        BOOST_CHECK_EQUAL(*queue.begin(), signal);
        queue.clear();
    }
    {
        queue.insert(elderSignal);
        Task inQueue = createProcessingTask(signal);
        scheduler.update(&inQueue, 0, &queue);
        BOOST_CHECK_EQUAL(inQueue, createProcessingTask(elderSignal));
        BOOST_CHECK_EQUAL(queue.size(), 1);
        BOOST_CHECK_EQUAL(*queue.begin(), signal);
    }
}

BOOST_AUTO_TEST_CASE(nextTest)
{
    pt::ptime ctime = maps::nowUtc();
    const size_t MAX_DELTA = 135;
    const pt::time_duration MAX_SIGNALS_GAP = pt::seconds(MAX_DELTA);
    TaskScheduler scheduler(PROCESS_WAITING, SIGNALS_WINDOW, MAX_SIGNALS_GAP);
    SignalsQueue queue;
    const size_t STORE_TIME = 27 * 63;
    ma::VehicleId id("0", "1");
    VehicleInfo vehicleInfo(id, STORE_TIME);
    mad::GpsSignal signal = createSignal(ctime, ctime);
    Task processTask = createProcessingTask(signal);
    Task reMatchTask(
        TaskType::ReMatch,
        id,
        ctime + pt::seconds(10)
    );
    Task forceMatchTask(
        TaskType::ForceMatch,
        id,
        ctime + pt::seconds(10)
    );
    Task deleteVehicleTask(
        TaskType::DeleteVehicle,
        id,
        ctime + pt::seconds(10)
    );

    // empty queue test
    {
        BOOST_CHECK_EQUAL(
            *scheduler.next(processTask, vehicleInfo, &queue),
            Task(TaskType::ReMatch, id, processTask.time() + SIGNALS_WINDOW)
        );
        BOOST_CHECK_EQUAL(
            *scheduler.next(reMatchTask, vehicleInfo, &queue),
            Task(TaskType::ForceMatch, id, reMatchTask.time() + MAX_SIGNALS_GAP - SIGNALS_WINDOW)
        );
        BOOST_CHECK_EQUAL(
            *scheduler.next(forceMatchTask, vehicleInfo, &queue),
            Task(TaskType::DeleteVehicle, id, forceMatchTask.time() + pt::seconds(STORE_TIME))
        );
        BOOST_CHECK(
            scheduler.next(deleteVehicleTask, vehicleInfo, &queue) == std::nullopt
        );
    }
    // non empty queue test
    {
        std::vector<Task> tasks;
        tasks += processTask, forceMatchTask, deleteVehicleTask;
        for (const Task& task : tasks) {
            queue.insert(signal);
            BOOST_CHECK_EQUAL(
                *scheduler.next(task, vehicleInfo, &queue),
                processTask
            );
            BOOST_CHECK(queue.empty());
        }
    }
    BOOST_CHECK_THROW(
        scheduler.next(Task(static_cast<TaskType>(17), id, ctime), vehicleInfo, &queue),
        std::exception
    );
}

// adds signals and checks whether tasks are correct
// signals must be already sorted in order of receive_time
void testSequence(
    TaskScheduler scheduler,
    const ma::VehicleId& id,
    const mad::GpsSignal& signal,
    const std::vector<Task>& tasks)
{
    SignalsQueue queue;
    // const size_t STORE_TIME = 27 * 63;
    VehicleInfo vehicleInfo(id, STORE_DURATION.total_seconds());

    auto taskIt = tasks.begin();

    queue.insert(signal);

    // create first task
    auto task = scheduler.create(id, &queue);
    Y_ASSERT(taskIt != tasks.end());
    BOOST_CHECK_EQUAL(task, *taskIt++);

    while (taskIt != tasks.end()) {
        auto nextTask = scheduler.next(task, vehicleInfo, &queue);
        BOOST_CHECK(nextTask);
        BOOST_CHECK_EQUAL(*nextTask, *taskIt);
        task = *nextTask;
        ++taskIt;
    }

    BOOST_CHECK(scheduler.next(task, vehicleInfo, &queue) == std::nullopt);
}


BOOST_AUTO_TEST_CASE(taskSequenceTest)
{
    const pt::ptime ctime = maps::nowUtc();
    const ma::VehicleId id("clid", "uuid");
    const auto signal = createSignal(ctime, ctime);

    pt::ptime taskTime = ctime;
    auto mkNextTask = [&] (TaskType taskType, const pt::time_duration& delta) {
        taskTime += delta;
        auto task = Task(
            taskType,
            id,
            taskTime
        );
        if (taskType == TaskType::ProcessSignal) {
            task.addSignal(signal);
        }
        return task;
    };

    taskTime = ctime;
    testSequence(
        TaskScheduler(PROCESS_WAITING, SIGNALS_WINDOW, MAX_SIGNALS_GAP),
        id,
        signal,
        {
            mkNextTask(TaskType::ProcessSignal, PROCESS_WAITING),
            mkNextTask(TaskType::ReMatch, SIGNALS_WINDOW),
            mkNextTask(TaskType::ForceMatch, MAX_SIGNALS_GAP - SIGNALS_WINDOW),
            mkNextTask(TaskType::DeleteVehicle, STORE_DURATION)
        }
    );

    taskTime = ctime;
    testSequence(
        TaskScheduler(PROCESS_WAITING, pt::seconds(0), MAX_SIGNALS_GAP),
        id,
        signal,
        {
            mkNextTask(TaskType::ProcessSignal, PROCESS_WAITING),
            mkNextTask(TaskType::ForceMatch, MAX_SIGNALS_GAP),
            mkNextTask(TaskType::DeleteVehicle, STORE_DURATION)
        }
    );
}
