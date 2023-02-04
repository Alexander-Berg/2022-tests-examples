#include "common.h"

#include <maps/factory/libs/tasks/conditional_run.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/team.h>

#include <thread>

namespace maps::factory::tasks::tests {

Y_UNIT_TEST_SUITE(conditional_run_should) {

Y_UNIT_TEST(run_task_repeatedly)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_1",
        genericWorker("add", [&](TestArg arg) { result += arg.val; }));

    the.schedule.add(runRepeatedlyTask(Task("add", TestArg(10)), milliseconds(10), 3));

    std::this_thread::sleep_for(milliseconds(15));
    team.work(the.schedule);
    EXPECT_GE(result, 10);

    std::this_thread::sleep_for(milliseconds(15));
    team.work(the.schedule);
    EXPECT_GE(result, 20);

    std::this_thread::sleep_for(milliseconds(15));
    team.work(the.schedule);
    EXPECT_EQ(result, 30);

    std::this_thread::sleep_for(milliseconds(15));
    team.work(the.schedule);
    EXPECT_EQ(result, 30);
}

Y_UNIT_TEST(run_task_when_queue_is_empty)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_1",
        genericWorker("add", [&](TestArg arg) { result += arg.val; }));

    the.schedule.add(Task("do_nothing"));
    the.schedule.add(runWhenQueueIsEmptyTask(Task("add", TestArg(42)), milliseconds(10)));

    std::this_thread::sleep_for(milliseconds(15));
    team.work(the.schedule);
    EXPECT_EQ(result, 0);

    team.add(genericWorker("do_nothing", [&] {}));
    team.work(the.schedule);
    std::this_thread::sleep_for(milliseconds(15));
    team.work(the.schedule);
    EXPECT_EQ(result, 42);

    std::this_thread::sleep_for(milliseconds(15));
    team.work(the.schedule);
    EXPECT_EQ(result, 42);
}

} // suite

} // namespace maps::factory::tasks::tests

