#include "common.h"

#include <maps/garden/libs_server/yt_task_handler/include/task_runner.h>
#include <maps/garden/libs_server/yt_task_handler/include/types.h>

#include <mapreduce/yt/util/ypath_join.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::garden::yt_task_handler::tests {

Y_UNIT_TEST_SUITE(TaskRunnerSuit) {

Y_UNIT_TEST(TaskStart) {
    auto client = getClient();

    auto task = runTask("force_exit");

    client->WaitForOperation(task.operationId.value());
}

Y_UNIT_TEST(NetworkProblem) {
    CommonYtSettings commonSpec;
    commonSpec.cluster = "IncorrectCluster";
    commonSpec.specOverrides = R"({"acl": [], "max_failed_job_count": 1})";

    TaskFullSpec taskFullSpec;
    TaskRunner runner(commonSpec, 1, 1, "search_marker", "module_executor");
    runner.enqueueTaskStart(taskFullSpec);

    std::vector<TaskStatus> ops;
    size_t attempts = 0;
    while (ops.size() == 0 && attempts++ < 6000) {
        ops = runner.popStartedTasks();
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    UNIT_ASSERT_VALUES_EQUAL(ops.size(), 1);
    UNIT_ASSERT_VALUES_UNEQUAL(ops[0].error, "");
}

} // Y_UNIT_TEST_SUITE(TaskRunnerSuit)

} // namespace maps::garden::yt_task_handler::tests
