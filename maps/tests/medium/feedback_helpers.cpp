#include <library/cpp/testing/unittest/registar.h>

#include "feedback_helpers.h"

namespace maps::wiki::social::tests {

using namespace feedback;

Task getTask(GatewayRO& gatewayRo, social::TId taskId)
{
    auto task = gatewayRo.taskById(taskId);
    UNIT_ASSERT(task);
    return std::move(*task);
}

TaskForUpdate getTaskForUpdate(Agent& agent, social::TId taskId)
{
    auto task = agent.taskForUpdateById(taskId);
    UNIT_ASSERT(task);
    return std::move(*task);
}

TId createRevealedTask(feedback::Agent& agent)
{
    auto taskNew = TaskNew(ZERO_POSITION, SOME_TYPE, SOME_SRC, DESCR());

    auto task = agent.addTask(taskNew);
    agent.revealTaskByIdCascade(task.id());
    return task.id();
};

TId createTaskNeedInfoAvailable(feedback::Agent& agent)
{
    auto taskNew = TaskNew(ZERO_POSITION, SOME_TYPE, "fbapi", DESCR());
    taskNew.attrs.addCustom("userEmail", "user@mail.org");

    auto task = agent.addTask(taskNew);
    agent.revealTaskByIdCascade(task.id());
    return task.id();
};

} // namespace maps::wiki::social::feedback::tests
