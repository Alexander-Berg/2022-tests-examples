#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/include/tasks.h>
#include <maps/renderer/denormalization/lib/tasks/tests/lib/common.h>
#include <maps/renderer/denormalization/lib/tasks/tests/lib/yt/test_context.h>

#include <maps/renderer/denormalization/lib/base/include/string_utils.h>

#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <boost/graph/adjacency_list.hpp>
#include <boost/graph/topological_sort.hpp>

#include <algorithm>
#include <map>
#include <set>
#include <vector>

namespace maps::renderer::denormalization {

namespace {

bool isTableResource(const GardenResource& resource)
{
    return std::holds_alternative<Table>(resource) || std::holds_alternative<TmpTable>(resource);
}

std::set<std::string> fetchResourceNames(YtTestContext& ctx)
{
    std::set<std::string> resources;
    for (const auto& resource: ctx.ytClient->List(TString{ctx.outputPath})) {
        resources.insert(resource.AsString());
    }
    return resources;
}

struct ResourceWithStatus {
    GardenResource resource;
    bool hidden;
};
using ResourceDescriptions = std::map<std::string, ResourceWithStatus>;

void hideUnrelatedResources(
    YtTestContext& ctx, const GardenTask& task, ResourceDescriptions& resourceDescriptions)
{
    static const std::string renameSuffix = "_hidden";

    auto demandsOutput = task.demandsOutput();
    std::set<std::string> demandsOutputSet{demandsOutput.begin(), demandsOutput.end()};

    for (auto& [name, description]: resourceDescriptions) {
        if (!isTableResource(description.resource)) {
            continue;
        }
        bool hide = !demandsOutputSet.count(name);
        if (description.hidden != hide) {
            description.hidden = hide;

            std::string nameFrom = description.resource.name();
            std::string nameTo = description.resource.name() + renameSuffix;
            if (!hide) {
                std::swap(nameFrom, nameTo);
            }
            ctx.ytClient->Move(
                TString{ctx.outputPath + '/' + nameFrom},
                TString{ctx.outputPath + '/' + nameTo},
                NYT::TMoveOptions().Force(true));
        }
    }
}

void resolveDependencies(std::vector<TaskPtr>& tasks)
{
    std::map<std::string, size_t> resourceName2taskIdx;
    for (size_t idx = 0; idx < tasks.size(); ++idx) {
        for (const auto& resource: tasks[idx]->creates()) {
            resourceName2taskIdx[resource.name()] = idx;
        }
    }

    std::set<std::string> externalNames;
    for (const auto& resource: externalResources()) {
        externalNames.insert(resource.name());
    }

    boost::adjacency_list<> graph(tasks.size());
    for (size_t idx = 0; idx < tasks.size(); ++idx) {
        for (const auto& resource: tasks[idx]->demandsOutput()) {
            if (externalNames.count(resource)) {
                continue;
            }

            ASSERT_TRUE(resourceName2taskIdx.count(resource))
                << "invalid demandsOutput " << resource << " for task " << tasks[idx]->name();

            boost::add_edge(resourceName2taskIdx[resource], idx, graph);
        }
    }

    std::vector<size_t> topologicalOrder;
    topological_sort(graph, std::back_inserter(topologicalOrder));
    std::reverse(topologicalOrder.begin(), topologicalOrder.end());

    std::vector<TaskPtr> result;
    for (auto idx: topologicalOrder) {
        result.push_back(tasks[idx]);
    }
    tasks = result;
}

void checkOutputResources(
    const std::vector<TaskPtr>& tasks,
    const std::set<std::string>& resourcesBefore,
    const std::set<std::string>& resourcesAfter)
{
    std::set<std::string> newResources;
    std::set_difference(
        resourcesAfter.begin(),
        resourcesAfter.end(),
        resourcesBefore.begin(),
        resourcesBefore.end(),
        std::inserter(newResources, newResources.begin()));

    std::set<std::string> declaredResources;
    for (const auto& task: tasks) {
        for (const auto& resource: task->creates()) {
            if (!isTableResource(resource)) {
                continue;
            }
            declaredResources.insert(resource.name());
        }
    }

    std::vector<std::string> missingResources;
    std::set_difference(
        declaredResources.begin(),
        declaredResources.end(),
        newResources.begin(),
        newResources.end(),
        std::back_inserter(missingResources));

    ASSERT_TRUE(missingResources.empty())
        << "The following resources were not created or already exists: "
        << joinWithCommas(missingResources);

    std::vector<std::string> notDeclaredResources;
    std::set_difference(
        newResources.begin(),
        newResources.end(),
        declaredResources.begin(),
        declaredResources.end(),
        std::back_inserter(notDeclaredResources));

    ASSERT_TRUE(notDeclaredResources.empty())
        << "The following resources were not declared: " << joinWithCommas(notDeclaredResources);
}

} // namespace

Y_UNIT_TEST_SUITE(dependencies_test) {

Y_UNIT_TEST(dependencies_test)
{
    YtTestContext ctx;
    prepareEmptyInputData(ctx);

    std::vector<TaskPtr> tasks = createTasks();
    resolveDependencies(tasks);

    ResourceDescriptions descriptions;
    for (const auto& resource: externalResources()) {
        descriptions.insert({resource.name(), {resource, false}});
    }

    log8::setLevel(log8::Level::DEBUG);

    for (const auto& task: tasks) {
        try {
            hideUnrelatedResources(ctx, *task, descriptions);

            auto resourcesBefore = fetchResourceNames(ctx);

            task->call(*ctx.taskArgs());

            auto resourcesAfter = fetchResourceNames(ctx);
            checkOutputResources({task}, resourcesBefore, resourcesAfter);

            for (const auto& resource: task->creates()) {
                descriptions.insert({resource.name(), {resource, false}});
            }
        } catch (...) {
            ERROR() << "Error while running task " << task->name() << ":";
            throw;
        }
    }
}

} // Y_UNIT_TEST_SUITE(dependencies_test)

} // namespace maps::renderer::denormalization
