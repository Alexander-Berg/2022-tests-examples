#include "common.h"

#include <maps/factory/libs/tasks/context.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/sequence.h>
#include <maps/factory/libs/tasks/team.h>

namespace maps::factory::tasks::tests {
namespace {

struct TypedNoHandler {
    static constexpr auto name = "test_task_1"sv;
    using Arg = TestArg;
};

struct TypedArg {
    static constexpr auto name = "test_task_2"sv;
    using Arg = TestArg;

    int* result;

    void operator()(Arg arg) const
    {
        *result += arg.val;
    }
};

struct TypedArgResult {
    static constexpr auto name = "test_task_3"sv;
    using Arg = TestArg;

    mutable int result = 0;

    TestArg operator()(TestArg arg) const
    {
        result += arg.val;
        return arg;
    }
};

} // namespace

Y_UNIT_TEST_SUITE(typed_should) {

Y_UNIT_TEST(create_with_separate_handler)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_1",
        typedWorker<TypedNoHandler>([&](TestArg arg) { result += arg.val; }));

    the.schedule.add(typedTask<TypedNoHandler>(42));

    team.work(the.schedule);
    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(create_with_arument)
{
    TaskFixture the{};
    int result = 0;
    Team team("team_1",
        typedWorker(TypedArg{&result}));

    the.schedule.add(typedTask<TypedArg>(42));

    team.work(the.schedule);
    EXPECT_EQ(result, 42);
}

Y_UNIT_TEST(create_with_argument_and_result)
{
    TaskFixture the{};
    TypedArgResult handler;
    Team team("team_1",
        typedWorker(&handler));

    the.schedule.add(typedTask<TypedArgResult>(42));

    team.work(the.schedule);
    EXPECT_EQ(handler.result, 42);
}

Y_UNIT_TEST(create_using_shared_ptr)
{
    TaskFixture the{};
    auto handler = std::make_shared<TypedArgResult>();
    Team team("team_1",
        typedWorker(handler));

    the.schedule.add(typedTask<TypedArgResult>(42));

    team.work(the.schedule);
    EXPECT_EQ(handler->result, 42);
}

} // suite

} // namespace maps::factory::tasks::tests

