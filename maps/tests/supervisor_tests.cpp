#include "common.h"

#include <maps/factory/libs/tasks/context.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/supervisor.h>
#include <maps/factory/libs/tasks/team.h>

#include <util/system/event.h>

#include <thread>

namespace maps::factory::tasks::tests {

Y_UNIT_TEST_SUITE(supervisor_should) {

Y_UNIT_TEST(get_working_teams)
{
    TaskFixture the{};
    TManualEvent started1, started2;

    Team team1("team_1", genericWorker("task_1", [&] {
        started1.Signal();
    }));
    Team team2("team_2", genericWorker("task_2", [&] {
        started2.Signal();
    }));
    the.schedule.add({Task("task_1"), Task("task_2")});

    Supervisor sup{the.pool};

    EXPECT_THAT(sup.teams(), IsEmpty());

    std::thread thread1([&] {
        team1.setUnlimitedIterations().work(the.schedule);
    });
    std::thread thread2([&] {
        team2.setUnlimitedIterations().work(the.schedule);
    });

    started1.Wait();
    started2.Wait();

    EXPECT_THAT(sup.teams(), UnorderedElementsAre("team_1", "team_2"));

    team1.stop();
    team2.stop();
    thread1.join();
    thread2.join();

    EXPECT_THAT(sup.teams(), IsEmpty());
}

Y_UNIT_TEST(get_dead_teams)
{
    TaskFixture the{};
    TManualEvent started, stop;

    Team team("team_1", genericWorker("task_1", [&] {
        started.Signal();
        stop.Wait();
    }));
    the.schedule.add(Task("task_1"));

    constexpr auto timeout = milliseconds(10);
    Supervisor sup{the.pool};

    EXPECT_THAT(sup.teams(), IsEmpty());
    EXPECT_THAT(sup.deadTeams(timeout), IsEmpty());

    std::thread thread([&] {
        team.setUnlimitedIterations().work(the.schedule);
    });

    started.Wait();

    EXPECT_THAT(sup.teams(), ElementsAre("team_1"));
    EXPECT_THAT(sup.deadTeams(timeout), IsEmpty());

    std::this_thread::sleep_for(timeout + milliseconds(1));

    EXPECT_THAT(sup.teams(), ElementsAre("team_1"));
    EXPECT_THAT(sup.deadTeams(timeout), ElementsAre("team_1"));

    stop.Signal();

    team.stop();
    thread.join();

    EXPECT_THAT(sup.teams(), IsEmpty());
    EXPECT_THAT(sup.deadTeams(timeout), IsEmpty());
}

Y_UNIT_TEST(remove_completed_tasks)
{
    TaskFixture the{};

    Team team("team_1", genericWorker("task_1", [&] {}));
    the.schedule.add({Task("task_1"), Task("task_1"), Task("task_1")});

    Supervisor sup{the.pool};

    EXPECT_THAT(sup.completedTasks(), IsEmpty());

    team.work(the.schedule);

    EXPECT_THAT(sup.completedTasks(), SizeIs(3u));

    sup.removeCompletedTasks(sup.completedTasks());

    EXPECT_THAT(sup.completedTasks(), IsEmpty());
}

Y_UNIT_TEST(suspend_failed_tasks)
{
    TaskFixture the{};

    bool shouldThrow = true;
    int result = 0, tried = 0;
    Team team("team_1",
        genericWorker("task_1", [&](TestArg arg) {
            tried += 1;
            if (shouldThrow) { throw std::exception{}; }
            result += arg.val;
        }));
    the.schedule.add({
        Task("task_1", TestArg(10)),
        Task("task_1", TestArg(20)),
        Task("task_1", TestArg(30)),
    });

    Supervisor sup{the.pool};

    EXPECT_EQ(sup.failedTasksCount(), 0u);
    EXPECT_THAT(sup.failedTasks(), IsEmpty());
    EXPECT_THAT(sup.suspendedTasks(), IsEmpty());

    tried = result = 0;
    team.work(the.schedule);

    EXPECT_EQ(tried, 6);
    EXPECT_EQ(result, 0);
    EXPECT_EQ(sup.failedTasksCount(), 3u);
    EXPECT_THAT(sup.failedTasks(), SizeIs(3u));
    EXPECT_THAT(sup.suspendedTasks(), IsEmpty());

    sup.suspendFailed(sup.failedTasks());

    EXPECT_EQ(sup.failedTasksCount(), 3u);
    EXPECT_THAT(sup.failedTasks(), SizeIs(3u));
    EXPECT_THAT(sup.suspendedTasks(), SizeIs(3u));

    tried = result = 0;
    team.work(the.schedule);

    EXPECT_EQ(tried, 0);
    EXPECT_EQ(result, 0);
    EXPECT_THAT(sup.failedTasks(), SizeIs(3u));
    EXPECT_THAT(sup.suspendedTasks(), SizeIs(3u));

    tried = result = 0;
    shouldThrow = false;
    team.work(the.schedule);

    EXPECT_EQ(tried, 0);
    EXPECT_EQ(result, 0);
    EXPECT_THAT(sup.failedTasks(), SizeIs(3u));
    EXPECT_THAT(sup.suspendedTasks(), SizeIs(3u));
    EXPECT_THAT(sup.completedTasks(), IsEmpty());

    sup.continueSuspended(sup.suspendedTasks());

    tried = result = 0;
    team.work(the.schedule);

    EXPECT_EQ(tried, 3);
    EXPECT_EQ(result, 60);
    EXPECT_EQ(sup.failedTasksCount(), 0u);
    EXPECT_THAT(sup.failedTasks(), IsEmpty());
    EXPECT_THAT(sup.suspendedTasks(), IsEmpty());
    EXPECT_THAT(sup.completedTasks(), SizeIs(3u));
}

} // suite

} // namespace maps::factory::tasks::tests

