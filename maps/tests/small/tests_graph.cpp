#include <maps/wikimap/mapspro/libs/revision_meta/impl/graph.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>
#include <random>

namespace maps::wiki::revision_meta::tests {

using IntGraph = Graph<int>;

namespace {

IntGraph::Components
sort(IntGraph::Components&& components) {
    // As long as components elements are non-empty sorted containers, the
    // following approach is okay
    std::sort(
        components.begin(), components.end(),
        [](const auto lhs, const auto rhs) {
            UNIT_ASSERT(!lhs.empty());
            UNIT_ASSERT(!rhs.empty());
            return *lhs.cbegin() < *rhs.cbegin();
        }
    );
    return std::move(components);
}

} // namespace

Y_UNIT_TEST_SUITE(add_elements_to_graph) {
    Y_UNIT_TEST(should_add_vertex_several_times) {
        IntGraph graph;

        graph.addVertex(42);
        UNIT_ASSERT_NO_EXCEPTION(graph.addVertex(42));
    }

    Y_UNIT_TEST(should_add_edge_several_times) {
        IntGraph graph;

        graph.addEdge(0, 1);
        UNIT_ASSERT_NO_EXCEPTION(graph.addEdge(0, 1));
        UNIT_ASSERT_NO_EXCEPTION(graph.addEdge(1, 0));
    }

    Y_UNIT_TEST(should_add_vertex_and_edge_to_this_vertex) {
        IntGraph graph;

        graph.addVertex(2);
        UNIT_ASSERT_NO_EXCEPTION(graph.addEdge(2, 3));
        UNIT_ASSERT_NO_EXCEPTION(graph.addEdge(1, 2));
    }

    Y_UNIT_TEST(should_add_edge_and_vertexes_of_this_edge) {
        IntGraph graph;

        graph.addEdge(1, 2);
        UNIT_ASSERT_NO_EXCEPTION(graph.addVertex(2));
        UNIT_ASSERT_NO_EXCEPTION(graph.addVertex(1));
    }

    Y_UNIT_TEST(should_add_edge_to_itself) {
        IntGraph graph;
        UNIT_ASSERT_NO_EXCEPTION(graph.addEdge(0, 0));
    }
} // Y_UNIT_TEST_SUITE(add_elements_to_graph)


Y_UNIT_TEST_SUITE(components) {
    Y_UNIT_TEST(should_not_get_components_from_empty_graph) {
        IntGraph graph;
        UNIT_ASSERT(graph.components().empty());
    }

    Y_UNIT_TEST(should_get_components_from_graph_made_from_vertexs_only) {
        IntGraph graph;

        graph.addVertex(10);
        graph.addVertex(1);

        UNIT_ASSERT_EQUAL(
            sort(graph.components()),
            IntGraph::Components({{1}, {10}})
        );
    }

    Y_UNIT_TEST(should_get_the_only_component_asc_edges_order) {
        IntGraph graph;

        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);

        UNIT_ASSERT_EQUAL(
            graph.components(),
            IntGraph::Components({{0, 1, 2, 3}})
        );
    }

    Y_UNIT_TEST(should_get_the_only_component_desc_edges_order) {
        IntGraph graph;

        graph.addEdge(3, 2);
        graph.addEdge(2, 1);
        graph.addEdge(1, 0);

        UNIT_ASSERT_EQUAL(
            graph.components(),
            IntGraph::Components({{0, 1, 2, 3}})
        );
    }

    Y_UNIT_TEST(should_get_the_only_component_unordered_edges) {
        IntGraph graph;

        graph.addEdge(1, 2);
        graph.addEdge(3, 2);
        graph.addEdge(0, 1);

        UNIT_ASSERT_EQUAL(
            graph.components(),
            IntGraph::Components({{0, 1, 2, 3}})
        );
    }

    Y_UNIT_TEST(should_get_several_components) {
        IntGraph graph;

        graph.addEdge(1, 1);

        graph.addEdge(2, 3);
        graph.addEdge(2, 4);
        graph.addEdge(2, 5);
        graph.addEdge(4, 6);
        graph.addEdge(4, 7);

        graph.addEdge(8, 9);
        graph.addEdge(9, 9);
        graph.addEdge(9, 10);

        UNIT_ASSERT_EQUAL(
            sort(graph.components()),
            IntGraph::Components({
                {1},
                {2, 3, 4, 5, 6, 7},
                {8, 9, 10}
            })
        );
    }

    Y_UNIT_TEST(should_get_components_made_of_standalone_vertexes) {
        IntGraph graph;

        graph.addVertex(1);

        graph.addVertex(2);
        graph.addEdge(2, 3);
        graph.addVertex(3);

        graph.addEdge(4, 4);    // 4 is a standalone vertex too

        UNIT_ASSERT_EQUAL(
            sort(graph.components()),
            IntGraph::Components({
                {1},
                {2, 3},
                {4}
            })
        );
    }
} // Y_UNIT_TEST_SUITE(components)


Y_UNIT_TEST_SUITE(randomized_tests) {
    auto getSeed() {
        static const auto seed = std::random_device{}();
        std::cout << "seed = " << seed << "\n";
        return seed;
    }

    template <typename RndGenerator>
        std::vector<int> createComponent(
            RndGenerator& generator,
            IntGraph& graph,
            size_t begin,
            size_t end)
    {
        UNIT_ASSERT(end > begin);
        const size_t nodesNumber = end - begin;

        std::vector<int> nodes;
        nodes.reserve(nodesNumber);

        for (auto node = begin; node < end; ++node) {
            nodes.emplace_back(node);

            auto distribution = std::uniform_int_distribution(
                0ul,
                // The first node is connected with itself, whereas all other nodes
                // must not be connected with themselves.
                nodes.size() == 1 ? 0ul : nodes.size() - 2
            );

            const auto toNode = nodes[distribution(generator)];
            graph.addEdge(node, toNode);
        }

        return nodes;
    }

    Y_UNIT_TEST(should_get_random_components) {
        const size_t COMPONENTS_NUMBER = 100;
        const size_t NODES_PER_COMPONENT = 25;
        const size_t GAP = 10;
        std::mt19937 rndGenerator(getSeed());
        int testIx = 100;

        while (testIx-- > 0) {
            IntGraph graph;

            IntGraph::Components expectedComponents;
            for (size_t componentIx = 0; componentIx < COMPONENTS_NUMBER; ++componentIx) {
                const auto expectedComponent = createComponent(
                    rndGenerator,
                    graph,
                    componentIx * (NODES_PER_COMPONENT + GAP),
                    componentIx * (NODES_PER_COMPONENT + GAP) + NODES_PER_COMPONENT
                );
                expectedComponents.emplace_back(expectedComponent.cbegin(), expectedComponent.cend());
            }

            const auto components = sort(graph.components());
            UNIT_ASSERT_EQUAL(components.size(), COMPONENTS_NUMBER);
            for (size_t componentIx = 0; componentIx < COMPONENTS_NUMBER; ++componentIx) {
                UNIT_ASSERT_EQUAL(
                    components[componentIx],
                    expectedComponents[componentIx]
                );
            }
        }
    }

    Y_UNIT_TEST(should_get_components_fast) {
        const size_t NODES_NUMBER = 1'000'000;
        std::mt19937 rndGenerator(getSeed());

        IntGraph graph;

        createComponent(rndGenerator, graph, 0, NODES_NUMBER);

        const auto components = graph.components();
        UNIT_ASSERT_EQUAL(components.size(), 1);
        UNIT_ASSERT_EQUAL(components[0].size(), NODES_NUMBER);
    }
} // Y_UNIT_TEST_SUITE(randomized_tests)

} // namespace maps::wiki::revision_meta::tests
