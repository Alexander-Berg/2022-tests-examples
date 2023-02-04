#include "../test_tools.h"

#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/finished_observer.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/config.h>
#include <maps/libs/deprecated/boost_time/utils.h>

#include <string>

using boost::posix_time::ptime;

Y_UNIT_TEST_SUITE(FinishObserverTest)
{
    Y_UNIT_TEST(single_observer_unfinish_test)
    {
        FinishedObserver observer;
        checkUnfinish(observer.state());
        ptime start = maps::nowUtc();
        observer.addTask(start);
        checkUnfinish(observer.state());
        observer.addTask(start);
        checkUnfinish(observer.state());
    }

    Y_UNIT_TEST(single_observer_finish_test)
    {
        FinishedObserver observer;
        ptime start = maps::nowUtc();

        observer.addTask(start);
        checkUnfinish(observer.state());

        //processed
        observer.complete(start);
        checkFinished(observer.state(), start);

        //update
        observer.delTask(start);
        ptime next = start + boost::posix_time::seconds(1);
        observer.addTask(next);
        checkUnfinish(observer.state());

        //processed
        observer.complete(next);
        checkFinished(observer.state(), next);
    }

    Y_UNIT_TEST(single_observer_k_task_finish_test)
    {
        const size_t NUM_TASKS = 10;
        FinishedObserver observer;
        ptime start = maps::nowUtc();

        for(size_t i = 0; i < NUM_TASKS; ++i) {
            observer.addTask(start);
            checkUnfinish(observer.state());
        }

        for(size_t i = 0; i < NUM_TASKS; ++i) {
            //processed
            observer.complete(start);
            if (i + 1 == NUM_TASKS) {
                checkFinished(observer.state(), start);
            } else {
                checkUnfinish(observer.state());
            }
        }
    }

    Y_UNIT_TEST(single_observer_same_time_unfinish)
    {
        FinishedObserver observer;
        ptime start = maps::nowUtc();

        observer.addTask(start);
        checkUnfinish(observer.state());

        //processed
        observer.complete(start);
        checkFinished(observer.state(), start);

        observer.addTask(start);
        checkUnfinish(observer.state());

        //processed
        observer.complete(start);
        checkFinished(observer.state(), start);
    }

    Y_UNIT_TEST(single_observer_new_time)
    {
        const size_t NUM_TASKS = 10;
        FinishedObserver observer;
        ptime start = maps::nowUtc();

        for(size_t i = 0; i < NUM_TASKS; ++i) {
            observer.addTask(start + boost::posix_time::seconds(i));
            checkUnfinish(observer.state());
        }

        ptime finish = start + boost::posix_time::seconds(NUM_TASKS - 1);
        for(size_t i = 0; i < NUM_TASKS; ++i) {
            //processed
            observer.updateTask(finish);
            checkUnfinish(observer.state());
        }
        for(size_t i = 0; i < NUM_TASKS; ++i) {
            //processed
            observer.complete(finish);
            if (i + 1 == NUM_TASKS) {
                checkFinished(observer.state(), finish);
            } else {
                checkUnfinish(observer.state());
            }
        }
    }

    Y_UNIT_TEST(single_observer_prev_time)
    {
        FinishedObserver observer;
        ptime start = maps::nowUtc();

        observer.addTask(start);
        checkUnfinish(observer.state());

        //processed
        observer.complete(start);
        checkFinished(observer.state(), start);

        observer.addTask(start - boost::posix_time::seconds(1));
        checkUnfinish(observer.state());

        //processed
        observer.updateTask(start);
        checkUnfinish(observer.state());

        //processed
        observer.complete(start);
        checkFinished(observer.state(), start);
    }
}
