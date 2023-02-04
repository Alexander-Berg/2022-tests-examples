#include "tasks.h"


namespace maps::wiki::tests {

social::TId
acquireTask(
    const social::Gateway& socialGw,
    social::TUid uid,
    const social::EventFilter& eventFilter,
    const social::ModerationTimeIntervals& moderationTimeIntervals)
{
    const auto ACQUIRE_TASKS_LIMIT = 2;

    const auto tasks =
        socialGw
        .moderationConsole(uid)
        .acquireTasks(eventFilter, ACQUIRE_TASKS_LIMIT, social::TasksOrder::OldestFirst, moderationTimeIntervals);
    REQUIRE(tasks.size() > 0, "No task has been acquired by the event filter. Consider filter changing, so only one task is acquired.");
    REQUIRE(tasks.size() < 2, "More than one task has been acquired by the event filter. Consider filter changing, so only one task is acquired.");

    return tasks.cbegin()->id();
}

} // namespace maps::wiki::tests
