
#include <maps/garden/libs_server/graph/graph.h>

#include <vector>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::garden::core {

template<>
struct VertexTraits<int> {
    static constexpr int null() { return 42; }
};

template<>
struct VertexTraits<char> {
    static constexpr char null() { return 'x'; }
};

} // namespace maps::garden::core

TEST(Graph, Basic)
{
    maps::garden::core::Graph<int> g;
    EXPECT_EQ(g.size(), static_cast<size_t>(0));

    // Adding edge implicitly adds missing vertices
    g.addEdge(0, 1);
    EXPECT_EQ(g.size(), static_cast<size_t>(2));
    g.addEdge(1, 2);
    EXPECT_EQ(g.size(), static_cast<size_t>(3));
    g.addVertex(3);
    EXPECT_EQ(g.size(), static_cast<size_t>(4));
    g.addEdge(1, 3);
    EXPECT_EQ(g.size(), static_cast<size_t>(4));

    // 0 -> 1 -> 2
    //        ↘
    //          3

    int sum = 0;
    for (auto v : g) {
        sum += v;
    }
    EXPECT_EQ(sum, 0 + 1 + 2 + 3);
    EXPECT_EQ(g.vertices() == std::unordered_set<int>({0, 1, 2, 3}), true);

    maps::garden::core::Graph<int> copy = g;

    // Deleting an edge doesn't remove the vertex from the graph
    g.removeEdge(1,3);
    EXPECT_EQ(g.hasEdge(1,3), false);
    EXPECT_EQ(g.size(), static_cast<size_t>(4));

    int removed = 0;
    g.removeSubtree(1, [&removed](int){ ++removed; });
    EXPECT_EQ(g.size(), static_cast<size_t>(2));
    EXPECT_EQ(removed, 2);
    g.removeSubtree(0, [&removed](int){ ++removed; });
    EXPECT_EQ(g.size(), static_cast<size_t>(1));
    EXPECT_EQ(removed, 3);
    g.removeSubtree(3, [&removed](int){ ++removed; });
    EXPECT_EQ(g.size(), static_cast<size_t>(0));
    EXPECT_EQ(removed, 4);

    // Check that copy constructor made a real copy
    EXPECT_EQ(copy.size(), static_cast<size_t>(4));
}

TEST(Graph, Reverse)
{
    maps::garden::core::Graph<int> g;
    g.addEdge(0, 1);
    g.addEdge(1, 2);
    g.addEdge(1, 3);

    // 0 -> 1 -> 2
    //        ↘
    //          3

    auto reverse = reverseGraph(g);
    EXPECT_EQ(g.size(), static_cast<size_t>(4));

    EXPECT_EQ(reverse.children(0).size(), static_cast<size_t>(0));
    EXPECT_EQ(reverse.children(1).size(), static_cast<size_t>(1));
    EXPECT_EQ(reverse.children(2).size(), static_cast<size_t>(1));
    EXPECT_EQ(reverse.children(3).size(), static_cast<size_t>(1));
}

TEST(Graph, NoSelfEdges)
{
    maps::garden::core::Graph<int> g;
    EXPECT_THROW(g.addEdge(1, 1), maps::Exception);
}

TEST(Graph, NoNullVertex)
{
    maps::garden::core::Graph<int> g;

    int null = maps::garden::core::VertexTraits<int>::null();
    EXPECT_THROW(g.addVertex(null), maps::Exception);
    EXPECT_THROW(g.addEdge(1, null), maps::Exception);
    EXPECT_THROW(g.addEdge(null, 2), maps::Exception);

    // note VertexTraits at the top of this file
    EXPECT_THROW(g.addVertex(42), maps::Exception);

    // After the previous failed attempts, the graph should still be in its original state
    EXPECT_EQ(g.size(), static_cast<size_t>(0));
    g.addVertex(0);
    EXPECT_EQ(g.size(), static_cast<size_t>(1));
}

TEST(Graph, Dfs)
{
    maps::garden::core::Graph<char> g;
    g.addEdge('a', 'b');
    g.addEdge('b', 'c');
    g.addEdge('a', 'c');

    int count = 0;
    std::vector<char> start;
    start.push_back('a');

    // post-order condition only is called once per vertex
    dfs(g, start, [] (char, char) { return false; }, [&count](char,char){ ++count; });
    EXPECT_EQ(count, 3);
}

TEST(Graph, Cycle)
{

    maps::garden::core::Graph<char> g;
    g.addEdge('a', 'b');
    g.addEdge('b', 'c');
    g.addEdge('c', 'a');

    EXPECT_EQ(findCycle(g).size(), static_cast<size_t>(3));
}
