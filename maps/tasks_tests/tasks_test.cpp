#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/include/tasks.h>
#include <maps/renderer/denormalization/lib/tasks/tests/lib/common.h>
#include <maps/renderer/denormalization/lib/tasks/tests/lib/yt/test_context.h>

#include <maps/libs/log8/include/log8.h>

#include <library/cpp/compute_rt_graph/rt_graph.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <util/generic/vector.h>

#include <map>
#include <set>
#include <thread>
#include <vector>

namespace maps::renderer::denormalization {

namespace {

std::map<std::string, std::vector<std::string>> calcDependencies(
    const std::map<std::string, TaskPtr>& tasks)
{
    std::map<std::string, std::string> resourceNameToTaskName;
    for (const auto& [name, task]: tasks) {
        for (const auto& resource: task->creates()) {
            resourceNameToTaskName[resource.name()] = task->name();
        }
    }

    std::set<std::string> externalResourceNames;
    for (const auto& resource: externalResources()) {
        externalResourceNames.insert(resource.name());
    }

    std::map<std::string, std::vector<std::string>> result;
    for (const auto& [name, task]: tasks) {
        auto& dependencies = result[name];
        for (const auto& resourceName: task->demandsOutput()) {
            if (externalResourceNames.count(resourceName)) {
                continue;
            }
            dependencies.push_back(resourceNameToTaskName.at(resourceName));
        }
    }

    return result;
}

class TasksRunner {
public:
    TasksRunner()
        : ctx_{}
        , tasks_{createTasksMap()}
        , taskDependencies_{calcDependencies(tasks_)}
        , tasksGraph_{std::thread::hardware_concurrency()}
        , runningTasks_{}
    {
        prepareInputData(ctx_);
    }

    void runAll()
    {
        for (const auto& [name, task]: tasks_) {
            run(name);
        }

        for (const auto& [name, task]: runningTasks_) {
            try {
                task->GetFuture().GetValueSync();
            } catch (...) {
                ERROR() << "Error while running task " << name << ":";
                throw;
            }
        }
    }

private:
    void run(const std::string& taskName)
    {
        if (runningTasks_.count(taskName)) {
            return;
        }

        auto task = tasks_.at(taskName);

        TVector<NComputeRTGraph::ITaskPtr> dependencies;
        for (const auto& name: taskDependencies_.at(taskName)) {
            run(name);
            dependencies.push_back(runningTasks_.at(name));
        }

        runningTasks_[taskName] = tasksGraph_.AddFunc(
            [this, task]() { task->call(*ctx_.taskArgs(task->name())); }, dependencies);
    }

    YtTestContext ctx_;
    std::map<std::string, TaskPtr> tasks_;
    std::map<std::string, std::vector<std::string>> taskDependencies_;
    NComputeRTGraph::TRTGraph tasksGraph_;
    std::map<std::string, NComputeRTGraph::ITaskPtr> runningTasks_;
};

} // namespace

Y_UNIT_TEST_SUITE(tasks_tests) {

Y_UNIT_TEST(all_tasks_test)
{
    TasksRunner{}.runAll();
}

} // Y_UNIT_TEST_SUITE(tasks_tests)

} // namespace maps::renderer::denormalization
