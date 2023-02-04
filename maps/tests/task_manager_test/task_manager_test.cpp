#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/rtp/impl/task_manager.h>
#include <maps/analyzer/libs/rtp/impl/internal_types.h>

#include "user_types.h"

#include <boost/test/included/unit_test.hpp>

#include <iostream>
#include <set>


using boost::posix_time::ptime;
using boost::posix_time::not_a_date_time;
using boost::posix_time::seconds;
using boost::posix_time::hours;
using boost::unit_test::test_suite;
using namespace std;

namespace rtp = maps::analyzer::rtp;

typedef rtp::impl::InternalTypes<UserTypes> InternalTypes;
typedef rtp::impl::TaskManager<InternalTypes, UserTypes> TaskManager;
typedef InternalTypes::TaskDescriptor TaskDescriptor;

const size_t DELETE_TYPE = 3;
const size_t TIME_DELAY = 1;

struct TaskManagerTest {
    ptime start_;
    IncrementTaskScheduler scheduler_;
    TaskManager manager_;

    TaskManagerTest()
        :start_(maps::nowUtc()),
         scheduler_(start_, DELETE_TYPE, TIME_DELAY),
         manager_(scheduler_)
    {
    }

    void checkNoTaskInQueue()
    {
        BOOST_CHECK(manager_.empty());
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 0);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), not_a_date_time);
        BOOST_CHECK(!manager_.hasTaskToProcess());
    }

    void checkEmpty()
    {
        checkNoTaskInQueue();
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 0);
    }

    void testPopTask(const Task& task, size_t maxDelay = 3)
    {
        BOOST_CHECK_EQUAL(justPopTask(maxDelay).task, task);
    }

    TaskDescriptor justPopTask(size_t maxDelay = 3)
    {
        ptime ctime = maps::nowUtc();
        while (!manager_.hasTaskToProcess()) {
            if (maps::nowUtc() - ctime > seconds(maxDelay)) {
                throw maps::Exception("test failed: too long wait");
            }
        }
        return manager_.popNextTask();
    }

    void addAndPopInEmptyManager(size_t id, size_t request)
    {
        manager_.addRequest(id, request);

        //One task in queue
        BOOST_CHECK(!manager_.empty());
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_);
        BOOST_CHECK(manager_.hasTaskToProcess());
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 1);

        //Pop task
        Task task(0, id, start_);
        task.addNumber(request);
        testPopTask(task);

        checkNoTaskInQueue();

        //Check task status
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 1);
        BOOST_CHECK(manager_.taskStatusMap_[id]->inProgress());
    }

    void oneTask()
    {
        const size_t request = 732819;
        const size_t id = selectId(request);
        addAndPopInEmptyManager(id, request);
        manager_.eraseTaskStatus(manager_.taskStatusMap_.begin());
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 0);

        //Adds task once again, pop
        addAndPopInEmptyManager(id, request);

        //than schedule back
        manager_.addNextTask(manager_.taskStatusMap_.begin(),
                Task(0, id, start_));
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_ + seconds(1));

        testPopTask(Task(1, id, start_ + seconds(1)), 1);
        //we wait exactly second
        BOOST_CHECK_EQUAL(maps::nowUtc(), start_ + seconds(1));
        checkNoTaskInQueue();
        manager_.eraseTaskStatus(manager_.taskStatusMap_.begin());
    }

    void beforeTask()
    {
        const size_t request = 732819;
        const size_t id = selectId(request);
        manager_.addRequest(id, request);

        //One task in queue
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_);
        //Task is enough old
        BOOST_CHECK(manager_.hasTaskToProcess());

        //Pop task
        Task task(0, id, start_);
        task.addNumber(request);
        testPopTask(task, 0);
        BOOST_CHECK(maps::nowUtc() >= start_);
        BOOST_CHECK(maps::nowUtc() - start_ <= seconds(1));

        checkNoTaskInQueue();
    }

    void rescheduleTask()
    {
        const size_t request = 732819;
        const size_t id = selectId(request);
        addAndPopInEmptyManager(id, request);
        manager_.addNextTask(manager_.taskStatusMap_.begin(),
                Task(0, id, start_));
        manager_.addRequest(id, request);
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_);

        Task task(0, id, start_);
        task.addNumber(request);
        testPopTask(task, 0);

        checkNoTaskInQueue();
        manager_.eraseTaskStatus(manager_.taskStatusMap_.begin());
    }

    void manyTasks()
    {
        const size_t requestA = 4173131;
        const size_t idA = selectId(requestA);
        const size_t requestB = 732812;
        const size_t idB = selectId(requestB);
        const size_t requestC = 31733;
        const size_t idC = selectId(requestC);

        manager_.addRequest(idA, requestA);
        manager_.addRequest(idB, requestB);

        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 2);
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 2);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_);

        TaskDescriptor firstTask = justPopTask();
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_);

        //Add task back. Time of next task will be greater.
        manager_.addNextTask(firstTask.iterator, firstTask.task);
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 2);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_);

        TaskDescriptor secondTask = justPopTask();
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_ + seconds(1));

        //Add task back. Time of next task will be greater.
        manager_.addNextTask(secondTask.iterator, secondTask.task);
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 2);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_ + seconds(1));

        manager_.addRequest(idC, requestC);
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 3);
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 3);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_);

        TaskDescriptor thirdTask = justPopTask();
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 2);
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 3);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_ + seconds(1));

        //Add task back. Time of next task will be greater.
        manager_.addNextTask(thirdTask.iterator, thirdTask.task);
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 3);
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 3);
        BOOST_CHECK_EQUAL(manager_.oldestTaskTime(), start_ + seconds(1));

        justPopTask();
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 2);
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 3);
        justPopTask();
        BOOST_CHECK_EQUAL(manager_.tasksNumber(), 1);
        BOOST_CHECK_EQUAL(manager_.differentIdsNumber(), 3);
        justPopTask();
        checkNoTaskInQueue();

        manager_.clear();
        checkEmpty();
    }

    void checkClear()
    {
        const size_t requestA = 4173132;
        const size_t idA = selectId(requestA);
        const size_t requestB = 732813;
        const size_t idB = selectId(requestB);
        const size_t requestC = 31794;
        const size_t idC = selectId(requestC);

        manager_.addRequest(idA, requestA);
        manager_.addRequest(idB, requestB);
        manager_.addRequest(idC, requestC);

        manager_.clear();
        checkEmpty();
    }

};

//Checks that empty manager works correct
BOOST_FIXTURE_TEST_CASE( CheckEmpty, TaskManagerTest)
{
    checkEmpty();
}


//Checks that one task works correct
BOOST_FIXTURE_TEST_CASE( OneTask, TaskManagerTest)
{
    oneTask();
}

//Checks that manager output task, that wait enough time
BOOST_FIXTURE_TEST_CASE( BeforeTask, TaskManagerTest)
{
    beforeTask();
}

//Checks that manager reschedule task works corect
BOOST_FIXTURE_TEST_CASE( RescheduleTask, TaskManagerTest)
{
    rescheduleTask();
}

//Checks that manager returns tasks in correct order
BOOST_FIXTURE_TEST_CASE( ManyTasks , TaskManagerTest)
{
    manyTasks();
}

//Checks that manager returns tasks in correct order
BOOST_FIXTURE_TEST_CASE( Clear, TaskManagerTest)
{
    checkClear();
}

