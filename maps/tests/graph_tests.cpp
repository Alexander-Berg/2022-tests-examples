#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/wikimap/mapspro/services/autocart/libs/detection/include/graph.h>

#include <map>


namespace maps {
namespace wiki {
namespace autocart {
namespace tests {

Y_UNIT_TEST_SUITE(graph_tests)
{
    Y_UNIT_TEST(test_extract_spanning_forest)
    {
        Graph<char, char> graph;
        int v[4];
        for (int i = 0; i < 4; i++) {
            v[i] = graph.addVertex('a');
        }
        graph.addEdge(v[0], v[1], 'e');
        graph.addEdge(v[0], v[2], 'e');
        graph.addEdge(v[0], v[3], 'e');
        graph.addEdge(v[1], v[2], 'e');

        std::vector<Graph<char, char>::GraphIndices>
            forest = graph.extractSpanningForest();

        EXPECT_EQ(forest.size(), 1);
        EXPECT_EQ(forest[0].verticesSize(), 4);
        EXPECT_EQ(forest[0].edgesSize(), 3);

        std::map<int, int> verts;
        for (auto it = forest[0].beginVertices();
            it != forest[0].endVertices(); it++) {
            verts[it->second.data] = it->first;
        }

        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 4; j++) {
                EXPECT_TRUE(forest[0].isVertsConnected(verts[v[i]], verts[v[j]]));
            }
        }
    }
} //Y_UNIT_TEST_SUITE(graph_tests)

} //namespace tests
} //namespace autocart
} //namespace wiki
} //namespace maps
