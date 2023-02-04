#ifndef MOBILE_BUILD

#include <maps/analyzer/libs/guidance/impl/binder/candidates_graph.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <vector>

namespace guidance = maps::analyzer::guidance;

using Id = std::size_t;
using Path = std::vector<Id>;

struct Skipped {
    Skipped& operator += (const Skipped& other) {
        skipped += other.skipped + 1;
        return *this;
    }
    std::size_t skipped = 0;
};

inline Skipped operator + (const Skipped& lhs, const Skipped& rhs) {
    return {lhs.skipped + rhs.skipped + 1};
}

struct NullType {};

using CGraph = guidance::CandidatesGraph<Id, Path, NullType, Skipped>;

constexpr double inf = std::numeric_limits<double>::infinity();

static const std::vector<std::vector<CGraph::WeightedState>> LAYERS_DATA{
    {{-inf, 0}, {-10.0, 1}, {-20.0, 2}}, // best is 1 with -10.0
    {{-5.0, 3}, {-20.0, 4}, {-inf, 5}}, // best is 3 with -5.0
    {{-40.0, 6}, {-20.0, 7}, {-30.0, 8}}, // best is 7 with -2.0
    {{-40.0, 9}, {-20.0, 10}, {-inf, 11}}, // best is 10 with -20.0
    {{-5.0, 12}, {-20.0, 13}, {-inf, 14}}, // best is 12 with -5.0
};

// Find best path on `LAYERS_DATA`, allowed to skip layers
Path findBestPath(std::size_t skipLayers) {
    CGraph cgraph{skipLayers + 1};

    for (const auto& l: LAYERS_DATA) {
        cgraph.appendLayer(
            {}, {}, l,
            [] (const NullType&, const NullType&, const Skipped&, const Path& path, const Id& from, const Id& to) {
                Path ret = path;
                if (ret.empty()) {
                    ret.push_back(from);
                }
                ret.push_back(to);
                return std::make_pair(0.0, ret);
            }
        );
    }

    EXPECT_FALSE(cgraph.empty());

    const auto bestIt = std::max_element(
        cgraph.frontLayer().candidates.begin(),
        cgraph.frontLayer().candidates.end()
    );
    EXPECT_TRUE(bestIt != cgraph.frontLayer().candidates.end());
    return bestIt->path;
}

Y_UNIT_TEST_SUITE(CandidatesGraphTests) {
    Y_UNIT_TEST(CandidatesGraphTest) {
        EXPECT_EQ(findBestPath(0), (Path{1, 3, 7, 10, 12}));
        EXPECT_EQ(findBestPath(1), (Path{1, 7, 12}));
        EXPECT_EQ(findBestPath(2), (Path{1, 3, 12}));
        EXPECT_EQ(findBestPath(3), (Path{1, 12}));
    }
}

#endif
