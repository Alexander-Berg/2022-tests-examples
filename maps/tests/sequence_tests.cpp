#include "common.h"

#include <maps/factory/libs/tasks/context.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/sequence.h>
#include <maps/factory/libs/tasks/team.h>

namespace maps::factory::tasks::tests {

Y_UNIT_TEST_SUITE(sequence_should) {

Y_UNIT_TEST(handle_one_task)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_handle_one_task",
        genericWorker("task_1", [&] { result += 10; }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        s.addTask(Task("task_1"));
    }));

    team.work(the.schedule);

    EXPECT_EQ(result, 10);
}

Y_UNIT_TEST(handle_many_tasks)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_handle_many_tasks",
        genericWorker("task_1", [&] { result += 10; }),
        genericWorker("task_2", [&](TestArg arg) { result += arg.val; }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        s.addTask(Task("task_1"));
        s.addTask(Task("task_2", TestArg(1000)).inSameTeam());
        s.addTask(Task("task_2", TestArg(42)));
        s.addTask(Task("task_1").inSameTeam());
    }));

    team.work(the.schedule);

    EXPECT_EQ(result, 10 + 1000 + 42 + 10);
}

Y_UNIT_TEST(retry_failed_task)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team_retry_failed_task",
        genericWorker("task_add", [&] { result += 10; }),
        genericWorker("task_throw", [&](TestArg arg) {
            if (shouldThrow) { throw std::exception{}; }
            result += arg.val;
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        s.addTask(Task("task_add"));
        s.addTask(Task("task_throw", TestArg(42)));
        s.addTask(Task("task_add"));
        s.addTask(Task("task_throw", TestArg(1000)));
        s.addTask(Task("task_add"));
    }));

    team.work(the.schedule);
    EXPECT_EQ(result, 10);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_EQ(result, 10 + 42 + 10 + 1000 + 10);
}

Y_UNIT_TEST(add_child_tasks)
{
    TaskFixture the{};
    bool shouldThrow = true;
    int result = 0;
    Team team("team_1",
        genericWorker("task_consume", [&](TestArg arg) { result += arg.val; }),
        genericWorker("task_add_children", [&](Context& ct) {
            ct.add(Task("task_consume", TestArg(10)));
            ct.add(Task("task_consume", TestArg(1000)).inSameTeam());
            if (shouldThrow) { throw std::exception{}; }
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        s.addTask(Task("task_add_children"));
    }));

    team.work(the.schedule);
    EXPECT_EQ(result, 0);

    shouldThrow = false;
    team.work(the.schedule);
    EXPECT_EQ(result, 1010);
}

Y_UNIT_TEST(pass_result_to_the_next_task)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_pass_result_to_the_next_task",
        genericWorker("task_source", [&] {
            return TestArg(1);
        }),
        genericWorker("task_transform", [&](TestArg arg) {
            return TestArg(arg.val + 100);
        }),
        genericWorker("task_consume", [&](TestArg arg) {
            result += arg.val;
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        auto a = s.addTaskWithResult(Task("task_source"));
        auto b = s.addTaskWithResult(Task("task_transform", a));
        auto c = s.addTaskWithResult(Task("task_transform", b));
        s.addTask(Task("task_consume", c));
    }));

    team.work(the.schedule);
    EXPECT_EQ(result, 1 + 100 + 100);
}

Y_UNIT_TEST(not_pass_results_when_error)
{
    TaskFixture the{};
    bool shouldThrow1 = true;
    bool shouldThrow2 = true;
    std::vector<int> result;
    Team team("team_not_pass_results_when_error",
        genericWorker("task_source_1", [&] {
            if (shouldThrow1) { throw std::exception{}; }
            return TestArg(42);
        }),
        genericWorker("task_source_2", [&] {
            if (shouldThrow2) { throw std::exception{}; }
            return TestArg(100);
        }),
        genericWorker("task_consume", [&](TestArg arg) {
            result.push_back(arg.val);
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        auto a = s.addTaskWithResult(Task("task_source_1"));
        s.addTask(Task("task_consume", a));
        auto b = s.addTaskWithResult(Task("task_source_2"));
        s.addTask(Task("task_consume", b));
    }));

    team.work(the.schedule);
    EXPECT_THAT(result, ElementsAre());

    shouldThrow1 = false;
    team.work(the.schedule);
    EXPECT_THAT(result, ElementsAre(42));

    shouldThrow2 = false;
    team.work(the.schedule);
    EXPECT_THAT(result, ElementsAre(42, 100));
}

Y_UNIT_TEST(pass_result_to_all_next_tasks)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_pass_result_to_all_next_tasks",
        genericWorker("task_source", [&] {
            return TestArg(10);
        }),
        genericWorker("task_consume", [&](TestArg arg) {
            result += arg.val;
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        auto a = s.addTaskWithResult(Task("task_source"));
        s.addTask(Task("task_consume", a));
        s.addTask(Task("task_consume", a));
        s.addTask(Task("task_consume", a));
        s.addTask(Task("task_consume", a));
    }));

    team.work(the.schedule);
    EXPECT_EQ(result, 10 * 4);
}

Y_UNIT_TEST(restart_sequence)
{
    TaskFixture the{};
    std::vector<int> result;
    bool shouldThrow = true;
    Team team("team_restart_sequence",
        genericWorker("task_restart", [&] {
            if (shouldThrow) {
                shouldThrow = false;
                throw RestartSequenceError{};
            }
        }),
        genericWorker("task_produce", [&] {
            return TestArg(10);
        }),
        genericWorker("task_consume", [&](TestArg arg) {
            result.push_back(arg.val);
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        auto a = s.addTaskWithResult(Task("task_produce"));
        s.addTask(Task("task_consume", a));
        s.addTask(Task("task_consume", TestArg(20)));
        s.addTask(Task("task_restart"));
        s.addTask(Task("task_consume", a));
        s.addTask(Task("task_consume", TestArg(100)));
    }));

    team.work(the.schedule);
    EXPECT_THAT(result, ElementsAre(10, 20, 10, 20, 10, 100));
}

Y_UNIT_TEST(serialize_sequence)
{
    auto task = sequence([](SequenceBuilder& s) {
        auto a = s.addTaskWithResult(Task("task_1", TestArg(10)));
        auto b = s.addTaskWithResult(Task("task_2", TestOtherArg(20)));
        s.addTask(Task("task_3", a, b));
        s.addTask(Task("task_3", TestArg(30), b));
    });
    constexpr auto expected =
        "{\"name\":\"task_1\",\"args\":{\"~sequence\":{\"context\":{},\"current\":0,\"steps\":[{\"store\":\"_task_1_0\",\"task\":{\"args\":{\"val\":10},\"name\":\"task_1\"}},{\"store\":\"_task_2_1\",\"task\":{\"args\":{\"otherVal\":20},\"name\":\"task_2\",\"team\":\"~parent\"}},{\"task\":{\"args\":{\"_task_1_0\":null,\"_task_2_1\":null},\"name\":\"task_3\",\"team\":\"~parent\"}},{\"task\":{\"args\":{\"_task_2_1\":null,\"val\":30},\"name\":\"task_3\",\"team\":\"~parent\"}}]}}}";
    EXPECT_EQ((json::Builder() << task).str(), expected);
}

Y_UNIT_TEST(run_sequence)
{
    TaskFixture the{};
    std::vector<int> result;
    Team team("team_run_sequence",
        genericWorker("task_consume", [&](TestArg arg) {
            result.push_back(arg.val);
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        s.addTask(Task("task_consume", TestArg(10)));
        s.addTask(Task("task_consume", TestArg(20)));
        s.addTask(Task("task_consume", TestArg(30)));
    }));

    team.work(the.schedule);
    EXPECT_THAT(result, ElementsAre(10, 20, 30));
}

Y_UNIT_TEST(build_sequence_with_return_types)
{
    TaskFixture the{};
    std::vector<double> result;
    Team team("team_build_sequence_with_return_types",
        genericWorker("task_produce_int", [&] {
            return TestArg(10);
        }),
        genericWorker("task_add_int_double", [&](TestArg a, TestOtherArg b) {
            return TestOtherArg(a.val + b.val());
        }),
        genericWorker("task_square_double", [&](TestOtherArg b) {
            return TestOtherArg(b.val() * b.val());
        }),
        genericWorker("task_consume_double", [&](TestOtherArg arg) {
            result.push_back(arg.val());
        }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        auto i10 = s.addTaskWithResult(Task("task_produce_int"));
        auto d30 = s.addTaskWithResult(Task("task_add_int_double", i10, TestOtherArg(20)));
        auto d900 = s.addTaskWithResult(Task("task_square_double", d30));
        auto d910 = s.addTaskWithResult(Task("task_add_int_double", i10, d900));
        s.addTask(Task("task_consume_double", d30));
        s.addTask(Task("task_consume_double", d900));
        s.addTask(Task("task_consume_double", d910));
    }));

    team.work(the.schedule);
    EXPECT_THAT(result, ElementsAre(30, 900, 910));
}

Y_UNIT_TEST(run_task_in_same_team)
{
    TaskFixture the{};
    std::vector<int> result;

    Team team1("team_1",
        genericWorker("task_produce_int", [&] { return TestArg(10); }));

    Team team2("team_2",
        genericWorker("task_consume_int", [&](TestArg a) { result.push_back(a.val + 1000); }));

    the.schedule.add(sequence([](SequenceBuilder& s) {
        auto i10 = s.addTaskWithResult(Task("task_produce_int"));
        s.addTask(Task("task_consume_int", i10).inSameTeam());
    }));

    team1.work(the.schedule);
    team2.work(the.schedule);

    EXPECT_THAT(result, ElementsAre());

    team1.add(
        genericWorker("task_consume_int", [&](TestArg a) { result.push_back(a.val); }));

    team1.work(the.schedule);
    team2.work(the.schedule);

    EXPECT_THAT(result, ElementsAre(10));
}

} // suite

} // namespace maps::factory::tasks::tests

