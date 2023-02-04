#include <maps/garden/libs_server/graph/task_queue.h>

#include <utility>
#include <vector>

#include <library/cpp/testing/gtest/gtest.h>

struct VersionedTask {
    VersionedTask(std::string name): name(std::move(std::move(name))) {}
    std::string name;
    std::unordered_set<std::string> readLocks;
    std::unordered_set<std::string> writeLocks;
};

bool operator==(const VersionedTask& lhs, const VersionedTask& rhs)
{
    return lhs.name == rhs.name;
}

std::ostream& operator<<(std::ostream& str, const VersionedTask& task)
{
    return str << task.name;
}

namespace std {

template<>
struct hash<VersionedTask>
{
    size_t operator()(const VersionedTask& task) const {
        return std::hash<std::string>()(task.name);
    }
};

} // end namespace std

namespace maps::garden::core {

template<>
struct TaskTraits<VersionedTask> {
    static std::unordered_set<std::string> readLocks(const VersionedTask& task)
    {
        return task.readLocks;
    }

    static std::unordered_set<std::string> writeLocks(const VersionedTask& task)
    {
        return task.writeLocks;
    }
};

} // namespace maps::garden::core

struct TestGraph {
public:
    void addEdge(const VersionedTask& from, const VersionedTask& to)
    {
        children_[from].push_back(to);
        children_[to];
        tasks_.push_back(from);
        tasks_.push_back(to);
    }

    const std::vector<VersionedTask>& children(const VersionedTask& t) const
    {
        return children_.at(t);
    }

    using iterator = typename std::vector<VersionedTask>::const_iterator;

    iterator begin() const { return tasks_.begin(); }
    iterator end() const { return tasks_.end(); }

private:
    std::vector<VersionedTask> tasks_;
    std::unordered_map<VersionedTask, std::vector<VersionedTask>> children_;
};


TEST(TaskQueue, Add)
{
    maps::garden::core::TaskQueue<VersionedTask> queue;
    VersionedTask task1("1");
    std::vector<VersionedTask> noDependencies;

    queue.add(task1, noDependencies);
    queue.recomputeReady();
    EXPECT_EQ(queue.hasReady(), true);
}

TEST(TaskQueue, Collect)
{
    maps::garden::core::TaskQueue<VersionedTask> queue;

    VersionedTask task1("1"), task2("2");
    std::vector<VersionedTask> noDependencies;

    queue.add(task1, noDependencies);
    queue.add(task2, std::vector<VersionedTask> {task1});

    queue.recomputeReady();
    EXPECT_EQ(queue.hasReady(), true);

    auto peeked = queue.peek();
    auto task = queue.pop();
    EXPECT_EQ(task.name, "1");
    EXPECT_EQ(peeked.name, task.name);

    queue.collect(task);
    queue.recomputeReady();
    EXPECT_EQ(queue.hasReady(), 1);

    task = queue.pop();
    EXPECT_EQ(task.name, "2");

    queue.collect(task);
    queue.recomputeReady();
    EXPECT_EQ(queue.hasReady(), 0);
}


TEST(TaskQueue, Restore)
{
    maps::garden::core::TaskQueue<VersionedTask> queue;
    VersionedTask task1("1"), task2("2");
    std::vector<VersionedTask> noDependencies;

    // Note that .restore() behaves a bit weird, in that
    // restoring a task after it is ready will not remove
    // it from the list of ready tasks, but restoring it
    // before it becomes ready will prevent it from
    // being added to the list of ready tasks.
    //
    // Whether or not that's a bug is debatable, but at least
    // at the moment some garden tests depend on this behaviour

    queue.add(task1, noDependencies);
    queue.restore(task1);
    EXPECT_EQ(queue.hasReady(), false);

    queue.restore(task2);
    queue.add(task2, noDependencies);
    EXPECT_EQ(queue.hasReady(), false);
}


TEST(TaskQueue, Remove)
{
    maps::garden::core::TaskQueue<VersionedTask> queue;
    VersionedTask task1("1"), task2("2");
    std::vector<VersionedTask> task1Dependants;
    task1Dependants.push_back(task2);

    queue.add(task1, task1Dependants);
    queue.recomputeReady();
    EXPECT_EQ(queue.hasReady(), true);

    auto removed = queue.removeWithDependencies(task2);
    EXPECT_EQ(removed.size(), static_cast<size_t>(2));
    EXPECT_EQ(std::find(removed.begin(), removed.end(), task1) != removed.end(), true);
    EXPECT_EQ(std::find(removed.begin(), removed.end(), task2) != removed.end(), true);

    queue.recomputeReady();
    EXPECT_EQ(queue.hasReady(), false);
}


TEST(TaskQueue, Merge)
{
    maps::garden::core::TaskQueue<VersionedTask> queue;
    VersionedTask task1("1"), task2("2");
    TestGraph graph;

    graph.addEdge(task1, task2);
    queue.merge(graph);
    queue.recomputeReady();

    EXPECT_EQ(queue.hasReady(), true);
}
