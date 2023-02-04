#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/gossip/lib/include/config.h>
#include <maps/analyzer/libs/gossip/lib/include/exception.h>
#include <maps/analyzer/libs/gossip/lib/include/tree_manager.h>


#include <optional>
#include <string>
#include <vector>
#include <unordered_map>

const std::string goodConsumer = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/libs/gossip/tests/configs/good_consumer.json");
const std::string goodDistributor = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/libs/gossip/tests/configs/good_distributor.json");

//                                       *
//         SAS                          VLA                  MAN
//          /                            |                     \
//          *                            *                     *
//     /         \                    /      \           /            \
//     *         *                    *      *           *            *
//   /   \      /                    / \    / \       /     \        /
//   *   *      *                    3 4   11 14      *     *        *
//  / \ / \    /                                     / \   / \      / \
//  1 2 5 6    7                                     8 9  10 12    13  15

TEST(SimpleTreeManager, GoodDistributor) {
    maps::gossip::Config conf = maps::gossip::fromFile(goodDistributor);
    maps::gossip::TreeManager tree(conf);
    EXPECT_TRUE(tree.isDistributor());
    std::default_random_engine rnd{0};
    std::vector<std::string> target = { "localhost:5014" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    EXPECT_EQ(tree.updateTarget("localhost:5003", rnd), "localhost:5004");
    EXPECT_EQ(tree.updateTarget("localhost:5004", rnd), "localhost:5011");
    target = { "localhost:5014" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    tree.reincarnate("localhost:5004");
    EXPECT_EQ(tree.updateTarget("localhost:5011", rnd), "localhost:5014");
    target = { "localhost:5014" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    EXPECT_EQ(tree.updateTarget("localhost:5014", rnd), "localhost:5004");
    target = { "localhost:5004" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    EXPECT_EQ(tree.updateTarget("localhost:5004", rnd), "localhost:5015");
    tree.reincarnate("localhost:5011");
    target = { "localhost:5011" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
}

TEST(SimpleTreeManager, GoodConsumer) {
    maps::gossip::Config conf = maps::gossip::fromFile(goodConsumer);
    maps::gossip::TreeManager tree(conf);
    EXPECT_TRUE(tree.isConsumer());
    std::default_random_engine rnd{0};
    std::vector<std::string> target = { "localhost:5014", "localhost:5008", "localhost:5006" };
    EXPECT_EQ(tree.getAllTargets("localhost:1001", rnd), target);
    target = { "localhost:5006" };
    EXPECT_EQ(tree.getAllTargets("localhost:5003", rnd), target);
    EXPECT_TRUE(tree.getAllTargets("localhost:5002", rnd).empty());
    EXPECT_EQ(tree.updateTarget("localhost:5006", rnd).value(), "localhost:5005");
    EXPECT_EQ(tree.updateTarget("localhost:5005", rnd).value(), "localhost:5002");
    target = { "localhost:5002" };
    EXPECT_EQ(tree.getAllTargets("localhost:5003", rnd), target);
    tree.reincarnate("localhost:5005");
    target = { "localhost:5005" };
    EXPECT_EQ(tree.getAllTargets("localhost:5009", rnd), target);
    EXPECT_EQ(tree.updateTarget("localhost:5005", rnd).value(), "localhost:5002");
    EXPECT_EQ(tree.updateTarget("localhost:5002", rnd).value(), "localhost:5001");
    EXPECT_EQ(tree.updateTarget("localhost:5001", rnd), std::nullopt);
    EXPECT_EQ(tree.updateTarget("localhost:5003", rnd).value(), "localhost:5004");
    EXPECT_EQ(tree.updateTarget("localhost:5004", rnd).value(), "localhost:5014");
    EXPECT_EQ(tree.updateTarget("localhost:5014", rnd).value(), "localhost:5011");
    EXPECT_EQ(tree.updateTarget("localhost:5011", rnd), std::nullopt);
    target = { "localhost:5015" };
    EXPECT_EQ(tree.getAllTargets("localhost:1001", rnd), target);
    tree.reincarnate("localhost:5004");
    target = { "localhost:5004", "localhost:5015" };
    EXPECT_EQ(tree.getAllTargets("localhost:1001", rnd), target);
    EXPECT_TRUE(tree.getAllTargets("localhost:5001", rnd).empty());
    tree.reincarnate("localhost:5001");
    target = { "localhost:5004", "localhost:5015", "localhost:5001" };
    EXPECT_EQ(tree.getAllTargets("localhost:1001", rnd), target);
    tree.kill("localhost:5001");
    target = { "localhost:5004", "localhost:5015" };
    EXPECT_EQ(tree.getAllTargets("localhost:1001", rnd), target);
    tree.reincarnate("localhost:5001");
    target = { "localhost:5004", "localhost:5012", "localhost:5001" };
    EXPECT_EQ(tree.getAllTargets("localhost:1001", rnd), target);
    /*
    EXPECT_EQ(tree.updateTarget("localhost:5003", rnd), "localhost:5004");
    EXPECT_EQ(tree.updateTarget("localhost:5004", rnd), "localhost:5011");
    target = { "localhost:5014" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    tree.reincarnate("localhost:5004");
    EXPECT_EQ(tree.updateTarget("localhost:5011", rnd), "localhost:5014");
    target = { "localhost:5014" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    EXPECT_EQ(tree.updateTarget("localhost:5014", rnd), "localhost:5004");
    target = { "localhost:5004" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    EXPECT_EQ(tree.updateTarget("localhost:5004", rnd), "localhost:5015");
    tree.reincarnate("localhost:5011");
    target = { "localhost:5011" };
    EXPECT_EQ(tree.getAllTargets(rnd), target);
    */
}
