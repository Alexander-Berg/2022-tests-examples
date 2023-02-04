#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/cmdline_graph/include/cmdline_graph.h>


namespace maps::analyzer::cmdline_graph {

namespace {

struct CmdlineGraphTest : public ::testing::Test {
    CmdlineGraphTest(): ents(parser, {
        Entity::RoadGraph,
        Entity::EdgesPersistentIndexFB,
        Entity::RTreeFB,
    }) {}

    void parse(const std::vector<std::string>& args) {
        std::vector<char*> argv;
        argv.reserve(args.size());
        for (auto& s: args) {
            argv.push_back(const_cast<char*>(s.data()));
        }
        parser.parse(argv.size(), argv.data());
    }

    maps::cmdline::Parser parser;
    EntitiesLoader ents;
};

} // `anonymous`

TEST_F(CmdlineGraphTest, testCmdlineDefault) {
    parse({"."});

    EXPECT_EQ(ents.getPath(Entity::RoadGraph), DEFAULT_GRAPH_PATH + "road_graph.fb");
    EXPECT_EQ(ents.getPath(Entity::EdgesPersistentIndexFB), DEFAULT_GRAPH_PATH + "edges_persistent_index.fb");
    EXPECT_EQ(ents.getPath(Entity::RTreeFB), DEFAULT_GRAPH_PATH + "rtree.fb");
}

TEST_F(CmdlineGraphTest, testCmdlineParse) {
    parse({
        ".",
        "--graph-version", "test",
    });

    EXPECT_EQ(ents.getPath(Entity::RoadGraph), ROOT_PATH + "test/road_graph.fb");
    EXPECT_EQ(ents.getPath(Entity::EdgesPersistentIndexFB), ROOT_PATH + "test/edges_persistent_index.fb");
    EXPECT_EQ(ents.getPath(Entity::RTreeFB), ROOT_PATH + "test/rtree.fb");
}

TEST_F(CmdlineGraphTest, testCmdlineDefined) {
    parse({
        ".",
        "--graph-version", "test",
        "--road-graph", "./road_graph.fb",
        "--edges-persistent-index-fb", "./edges_persistent_index.fb",
    });

    EXPECT_TRUE(!ents.defined(Entity::RTreeFB));
    EXPECT_EQ(ents.getPath(Entity::RTreeFB), ROOT_PATH + "test/rtree.fb");
    EXPECT_TRUE(ents.defined(Entity::RoadGraph));
    EXPECT_EQ(ents.getPath(Entity::RoadGraph), "./road_graph.fb");
    EXPECT_TRUE(ents.defined(Entity::EdgesPersistentIndexFB));
    EXPECT_EQ(ents.getPath(Entity::EdgesPersistentIndexFB), "./edges_persistent_index.fb");
}

TEST_F(CmdlineGraphTest, testCmdlineLoad) {
    parse({
        ".",
        "--graph-folder", BinaryPath("maps/data/test/graph3"),
    });

    auto graph = ents.load<Entity::RoadGraph>();
    ents.load<Entity::EdgesPersistentIndexFB>();
    ents.load<Entity::RTreeFB>(*graph);
}

TEST_F(CmdlineGraphTest, testCmdlineLoadExplicit) {
    parse({
        ".",
        "--graph-folder", BinaryPath("maps/data/test/graph3"),
        "--road-graph", BinaryPath("maps/data/test/graph3") + "/road_graph.fb",
    });

    auto roadGraph = ents.loadExplicit<Entity::RoadGraph>();
    EXPECT_TRUE(roadGraph.get() != nullptr);
    auto rtree = ents.loadExplicit<Entity::RTreeFB>(*roadGraph);
    EXPECT_TRUE(!rtree);
}

} // maps::analyzer::cmdline_graph
