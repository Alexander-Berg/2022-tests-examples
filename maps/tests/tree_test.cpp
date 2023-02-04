#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/gossip/lib/include/config.h>
#include <maps/analyzer/libs/gossip/lib/include/exception.h>
#include <maps/analyzer/libs/gossip/lib/include/tree.h>

#include <optional>
#include <string>
#include <vector>
#include <unordered_map>

const std::string goodConf = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/libs/gossip/tests/configs/good_conf3.json");

TEST(SimpleGossipTree, SuccessConf) {
    maps::gossip::Config t = maps::gossip::fromFile(goodConf);
    maps::gossip::GossipTree tree(t.consumers_);
    /*
     *                            _____________________________________
     *                           /                                     \
     *              __________________________                        _
     *             /                          \                      /
     *       __________               ______________                _
     *      /          \             /              \              /
     *    ____        ____        ______          ______          _
     *   /    \      /    \      /      \        /      \        /
     *  /\    /\    /\    /\    /\      /\      /\      /\      /\
     * 1  2  3  4  5  6  7  8  9  10  11  12  13  14  15  16  17  18
     * */
    std::default_random_engine rnd{0};
    std::string currentHost = "localhost:5008";
    std::vector<std::string> targets = { "localhost:5018", "localhost:5016", "localhost:5004", "localhost:5006", "localhost:5007" };
    EXPECT_EQ(tree.getChain(currentHost), targets);
    targets = { "localhost:5007" };
    EXPECT_EQ(tree.getChain(currentHost, "localhost:5005"), targets);
    EXPECT_TRUE(tree.getChain(currentHost, "localhost:5007").empty());
    targets = { "localhost:5004", "localhost:5006", "localhost:5007" };
    EXPECT_EQ(tree.getChain(currentHost, "localhost:5013"), targets);
    EXPECT_EQ(*tree.updateTarget(currentHost, "localhost:5014", rnd), "localhost:5013");
    EXPECT_EQ(tree.updateTarget(currentHost, "localhost:5005", rnd), std::nullopt);
    std::vector<std::string> ill;
    std::vector<std::string> target_ill = { "localhost:5005", "localhost:5014" };
    tree.getIll(ill);
    std::sort(ill.begin(), ill.end());
    EXPECT_EQ(ill, target_ill);
    tree.kill("localhost:5018");
    target_ill = { "localhost:5005", "localhost:5014", "localhost:5018" };
    ill.clear();
    tree.getIll(ill);
    std::sort(ill.begin(), ill.end());
    EXPECT_EQ(ill, target_ill);
    targets = { "localhost:5017", "localhost:5016", "localhost:5004", "localhost:5006", "localhost:5007" };
    EXPECT_EQ(tree.getChain(currentHost), targets);
    tree.reincarnate("localhost:5018");
    tree.kill("localhost:5004");
    targets = { "localhost:5018", "localhost:5016", "localhost:5003", "localhost:5006", "localhost:5007" };
    EXPECT_EQ(tree.getChain(currentHost), targets);
    target_ill = { "localhost:5004", "localhost:5005", "localhost:5014" };
    ill.clear();
    tree.getIll(ill);
    std::sort(ill.begin(), ill.end());
    EXPECT_EQ(ill, target_ill);
    tree.reincarnate("localhost:5004");
    tree.reincarnate("localhost:5005");
    tree.reincarnate("localhost:5014");
    tree.reincarnate("localhost:5014");
    ill.clear();
    tree.getIll(ill);
    EXPECT_TRUE(ill.empty());

    // now change currentHost
    currentHost = "localhost:5010";
    targets = { "localhost:5018", "localhost:5008", "localhost:5016", "localhost:5012", "localhost:5009" };
    EXPECT_EQ(tree.getChain(currentHost), targets);
    rnd = std::default_random_engine(0);
    EXPECT_EQ(tree.getRandomTarget(currentHost, rnd), "localhost:5009");
    EXPECT_EQ(tree.getRandomTarget(currentHost, rnd), "localhost:5006");
    EXPECT_EQ(tree.getRandomTarget(currentHost, rnd), "localhost:5002");
    targets = { "localhost:5012", "localhost:5009" };
    EXPECT_EQ(tree.getChain(currentHost, "localhost:5013"), targets);
    EXPECT_TRUE(tree.getChain(currentHost, "localhost:5009").empty());
    EXPECT_EQ(tree.updateTarget(currentHost, "localhost:5001", rnd), std::nullopt);
    EXPECT_EQ(tree.updateTarget(currentHost, "localhost:5017", rnd), std::nullopt);
    EXPECT_EQ(tree.updateTarget(currentHost, "localhost:5008", rnd), "localhost:5007");
    target_ill = { "localhost:5001", "localhost:5008", "localhost:5017" };
    tree.getIll(ill);
    std::sort(ill.begin(), ill.end());
    EXPECT_EQ(ill, target_ill);
    targets = { "localhost:5018", "localhost:5007", "localhost:5016", "localhost:5012", "localhost:5009" };
    EXPECT_EQ(tree.getChain(currentHost), targets);
    tree.kill("localhost:5018");
    targets = { "localhost:5007", "localhost:5016", "localhost:5012", "localhost:5009" };
    EXPECT_EQ(tree.getChain(currentHost), targets);
    tree.reincarnate("localhost:5018");
    tree.kill("localhost:5004");
    targets = { "localhost:5018", "localhost:5007", "localhost:5016", "localhost:5012", "localhost:5009" };
    EXPECT_EQ(tree.getChain(currentHost), targets);
}
