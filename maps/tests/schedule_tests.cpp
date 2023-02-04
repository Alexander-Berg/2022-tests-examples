#include "common.h"

#include <maps/factory/libs/tasks/impl/notifier.h>
#include <maps/factory/libs/tasks/impl/tasks_gateway.h>
#include <maps/factory/libs/tasks/context.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/team.h>

#include <util/system/event.h>

namespace maps::factory::tasks::tests {
using namespace table::alias;

Y_UNIT_TEST_SUITE(schedule_should) {

Y_UNIT_TEST(save_load_task)
{
    chrono::TimePoint time = chrono::TimePoint::clock::now();
    Task task("test_task",
        [](json::ObjectBuilder b) { b["arg1"] = 42; },
        [](json::ObjectBuilder b) { b["arg2"] = "test"; });
    task.scheduleAt(time);
    task.inSameTeam();

    auto json = impl::toJson(task);
    Task result(json);

    EXPECT_EQ(result, task);
    EXPECT_TRUE(result.isScheduled());
    EXPECT_TRUE(result.isInParentTaskTeam());
    EXPECT_TRUE(result.isAssigned());
    EXPECT_EQ(impl::toString(result.arguments()), R"({"arg1":42,"arg2":"test"})");
}

Y_UNIT_TEST(do_task_without_arguments)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_1",
        genericWorker("test_task_1", [&] { result += 42; }),
        genericWorker("test_task_2", [] {}),
        genericWorker("test_task_3", [] { return TestArg(42); })
    );

    the.schedule.add(Task("test_task_1"));
    the.schedule.add(Task("test_task_2"));
    the.schedule.add(Task("test_task_3"));

    team.work(the.schedule);

    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(do_task_with_arguments)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_2",
        genericWorker("test_task_2", [&](TestArg arg) {
            result += arg.val;
        }));

    the.schedule.add(Task("test_task_2", TestArg(42)));

    team.work(the.schedule);

    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(do_task_only_one_once)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_3",
        genericWorker("test_task_3", [&] { result += 42; }));

    the.schedule.add(Task("test_task_3"));

    team.work(the.schedule);
    team.work(the.schedule);
    team.work(the.schedule);

    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(do_many_tasks)
{
    TaskFixture the{};

    int result = 0;
    Team team("team_3",
        genericWorker("test_task_41", [&] { result += 10; }),
        genericWorker("test_task_42", [&] { result += 100; }),
        genericWorker("test_task_43", [&] { result += 1000; }));

    the.schedule.add({
        Task("test_task_41"),
        Task("test_task_42"),
        Task("test_task_43"),
    });

    team.work(the.schedule);

    EXPECT_EQ(result, 1110);
}

Y_UNIT_TEST(do_one_task_many_times)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_5",
        genericWorker("test_task_5", [&] { result += 10; }));

    the.schedule.add({
        Task("test_task_5"),
        Task("test_task_5"),
        Task("test_task_5"),
    });

    team.work(the.schedule);

    EXPECT_EQ(result, 30);
}

Y_UNIT_TEST(do_one_task_with_different_arguments)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_6",
        genericWorker("test_task_6", [&](TestArg arg) {
            result += arg.val;
        }));

    the.schedule.add({
        Task("test_task_6", TestArg(10)),
        Task("test_task_6", TestArg(100)),
        Task("test_task_6", TestArg(1000)),
    });

    team.work(the.schedule);

    EXPECT_EQ(result, 1110);
}

Y_UNIT_TEST(retry_failed_task)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team_7",
        genericWorker("test_task_7", [&](TestArg arg) {
            if (shouldThrow) { throw std::exception{}; }
            result += arg.val;
        }));

    the.schedule.add(Task("test_task_7", TestArg(42)));

    team.work(the.schedule);
    shouldThrow = false;
    team.work(the.schedule);

    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(add_continuation_task)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_8",
        genericWorker("test_task_81", [&](TestArg arg, Context& p) {
            result += arg.val;
            p.add(Task("test_task_82", arg));
        }),
        genericWorker("test_task_82", [&](Context& p) {
            auto[d] = p.argsTuple<int>("val");
            result += d * 10;
        }));

    the.schedule.add(Task("test_task_81", TestArg(10)));

    team.work(the.schedule);

    EXPECT_EQ(result, 110);
}

Y_UNIT_TEST(do_tasks_except_failed)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team_9",
        genericWorker("test_task_91", [&] {
            if (shouldThrow) { throw std::exception{}; }
            result += 10;
        }),
        genericWorker("test_task_92", [&] {
            result += 100;
        }));

    the.schedule.add({Task("test_task_91"), Task("test_task_92")});

    team.work(the.schedule);
    EXPECT_EQ(result, 100);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_EQ(result, 110);
}

Y_UNIT_TEST(do_not_add_continuation_for_failed_task)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team_10",
        genericWorker("test_task_101", [&](Context& p) {
            p.add(Task("test_task_102"));
            if (shouldThrow) { throw std::exception{}; }
            result += 10;
        }),
        genericWorker("test_task_102", [&] {
            result += 100;
        }));

    the.schedule.add(Task("test_task_101"));

    team.work(the.schedule);
    EXPECT_EQ(result, 0);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_EQ(result, 110);
}

Y_UNIT_TEST(wait_between_iterations)
{
    TaskFixture the{};
    Team team("team_11");

    const auto started = std::chrono::steady_clock::now();
    team.setMaxIterations(3).setIterationTime(milliseconds(10)).work(the.schedule);
    const auto ended = std::chrono::steady_clock::now();

    const milliseconds passed = std::chrono::duration_cast<milliseconds>(ended - started);
    const milliseconds expected = (team.maxIterations() - 1) * team.iterationTime();
    EXPECT_GE(passed.count(), expected.count());
}

Y_UNIT_TEST(stop_taking_new_tasks_when_too_many_failed)
{
    TaskFixture the{};

    bool shouldThrow = true;
    int result = 0;
    Team team("team_12",
        genericWorker("test_task_121", [&] {
            if (shouldThrow) { throw std::exception{}; }
            result += 10;
        }),
        genericWorker("test_task_122", [&] {
            if (shouldThrow) { throw std::exception{}; }
            result += 1000;
        }));

    for (int i = 0; i < 10; ++i) {
        the.schedule.add({Task("test_task_121"), Task("test_task_122")});
    }

    team.setTakeNewThreshold(4).work(the.schedule);

    EXPECT_EQ(result, 0);
    const auto ofTeam = _Task::takenBy == team.name();
    EXPECT_EQ(TasksGateway(*the.txn()).count(ofTeam), 4u);
    EXPECT_EQ(TasksGateway(*the.txn()).count(ofTeam && _Task::error.isNotNull()), 4u);
    EXPECT_EQ(TasksGateway(*the.txn()).count(ofTeam && _Task::completedAt.isNotNull()), 0u);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_EQ(result, 10100);
}

Y_UNIT_TEST(stop_handling_tasks_when_too_many_failed)
{
    TaskFixture the{};

    bool shouldThrow = true;
    int result = 0;
    Team team("team_13",
        genericWorker("test_task_131", [&] {
            if (shouldThrow) { throw std::exception{}; }
            result += 10;
        }),
        genericWorker("test_task_132", [&] {
            result += 1000;
        }));

    for (int i = 0; i < 10; ++i) {
        the.schedule.add({Task("test_task_131"), Task("test_task_132")});
    }

    team.setHandleThreshold(4).work(the.schedule);

    EXPECT_EQ(result, 3000);
    const auto ofTeam = _Task::takenBy == team.name();
    EXPECT_EQ(TasksGateway(*the.txn()).count(ofTeam), 7u);
    EXPECT_EQ(TasksGateway(*the.txn()).count(ofTeam && _Task::error.isNotNull()), 4u);
    EXPECT_EQ(TasksGateway(*the.txn()).count(ofTeam && _Task::completedAt.isNotNull()), 3u);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_EQ(result, 10100);
}

Y_UNIT_TEST(schedule_task_locally)
{
    TaskFixture the{};

    int result = 0;
    Team team("team_14",
        genericWorker("test_task_141", [&](Context& ct) {
            result += 10;
            ct.add(Task("test_task_142").inSameTeam());
        }));

    the.schedule.add(Task("test_task_141"));

    team.work(the.schedule);
    EXPECT_EQ(result, 10);
    EXPECT_EQ(TasksGateway(*the.txn()).loadOne(_Task::name == "test_task_142").takenBy(), team.name());
}

Y_UNIT_TEST(save_task_result)
{
    TaskFixture the{};

    Team team("team_15",
        genericWorker("test_task_15", [&](TestArg arg) {
            arg.val += 1;
            return arg;
        }));

    the.schedule.add(Task("test_task_15", TestArg(42)));

    team.work(the.schedule);

    EXPECT_EQ(impl::toString(TasksGateway(*the.txn()).loadOne().result()), R"({"val":43})");
}

Y_UNIT_TEST(schedule_task)
{
    TaskFixture the{};

    int result = 0;
    Team team("team_16",
        genericWorker("test_task_16", [&] { result += 10; }));

    the.schedule.add(Task("test_task_16").scheduleAfter(std::chrono::hours(1)));
    team.work(the.schedule);

    EXPECT_EQ(result, 0);

    the.schedule.add(Task("test_task_16").scheduleAfter(milliseconds(1)));
    std::this_thread::sleep_for(milliseconds(1));
    team.work(the.schedule);

    EXPECT_EQ(result, 10);
}

Y_UNIT_TEST(do_task_with_many_arguments)
{
    TaskFixture the{};
    double result = 0;
    Team team("team_17",
        genericWorker("test_task_17", [&](TestArg arg, TestOtherArg otherArg) {
            result += arg.val;
            result *= otherArg.val();
        }));

    the.schedule.add(Task("test_task_17", TestArg(42), TestOtherArg(10)));

    team.work(the.schedule);

    EXPECT_DOUBLE_EQ(result, 420);
}

Y_UNIT_TEST(delay_failed_task)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team",
        genericWorker("test_task", [&](TestArg arg) {
            if (shouldThrow) { throw std::exception{}; }
            result += arg.val;
        }));

    the.schedule.add(Task("test_task", TestArg(42)));

    team.setRetryDelay(hours(1));
    team.work(the.schedule);
    EXPECT_EQ(result, 0);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_EQ(result, 0);

    std::this_thread::sleep_for(milliseconds(10));
    team.work(the.schedule);
    EXPECT_EQ(result, 0);
}

Y_UNIT_TEST(retry_task_after_delay)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team",
        genericWorker("test_task", [&](TestArg arg) {
            if (shouldThrow) { throw std::exception{}; }
            result += arg.val;
        }));

    the.schedule.add(Task("test_task", TestArg(42)));

    team.setRetryDelay(milliseconds(5));
    team.work(the.schedule);
    EXPECT_EQ(result, 0);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_THAT(result, testing::AnyOf(0, 42));

    std::this_thread::sleep_for(milliseconds(10));
    team.work(the.schedule);
    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(retry_task_after_exponential_delay)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team",
        genericWorker("test_task", [&](TestArg arg) {
            if (shouldThrow) { throw std::exception{}; }
            result += arg.val;
        }));

    the.schedule.add(Task("test_task", TestArg(42)));

    team.setExponentialRetryDelay(milliseconds(1), milliseconds(10));
    team.work(the.schedule);
    team.work(the.schedule);
    team.work(the.schedule);
    team.work(the.schedule);
    EXPECT_EQ(result, 0);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_THAT(result, testing::AnyOf(0, 42));

    std::this_thread::sleep_for(milliseconds(10));
    team.work(the.schedule);
    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(same_team_take_assigned_tasks_that_have_workers)
{
    TaskFixture the{};
    int result = 0;
    constexpr auto team = "test_team";
    Team team1(team,
        genericWorker("add", [&](TestArg arg, Context& ct) {
            result += arg.val;
            ct.add(Task("mul", TestArg(10)).inSameTeam());
        }));
    Team team2(team,
        genericWorker("mul", [&](TestArg arg) { result *= arg.val; }));

    the.schedule.add(Task("add", TestArg(42)));

    team1.work(the.schedule);
    team2.work(the.schedule);

    EXPECT_EQ(result, 420);
}

Y_UNIT_TEST(copy_team)
{
    TaskFixture the{};
    int result = 0;
    Team team1("team_1",
        genericWorker("add", [&](TestArg arg) { result += arg.val; }));
    Team team2 = team1.clone("team_2");

    the.schedule.add(Task("add", TestArg(42)).assignTo("team_1"));
    the.schedule.add(Task("add", TestArg(100)).assignTo("team_2"));

    team1.work(the.schedule);
    EXPECT_EQ(result, 42);
    team2.work(the.schedule);
    EXPECT_EQ(result, 142);
}

} // suite

} // namespace maps::factory::tasks::tests

