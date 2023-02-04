#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <maps/analyzer/tools/mapbox_quality/lib/dsu.h>

#include <algorithm>

namespace mq = maps::analyzer::tools::mapbox_quality;

double speedFor(int64_t src, int64_t dst) {
    return 1. * src + 1000. * dst;
}

mq::SourceLine createEdge(int64_t src, int64_t dst) {
    return { src, dst, mq::Speed{speedFor(src, dst)} };
}

mq::LineComponent createComp(std::list<int64_t> pts) {
    if (pts.empty()) { return {}; }

    std::list<mq::Speed> speeds;
    for (auto pIt = pts.begin(); std::next(pIt) != pts.end(); ++pIt) {
        speeds.push_back(speedFor(*pIt, *std::next(pIt)));
    }
    
    return {
        std::move(pts),
        std::move(speeds)
    };
}

template<typename T>
void cmpLists(const std::list<T>& lhs, const std::list<T>& rhs) {
    EXPECT_EQ(lhs.size(), rhs.size());
    for (auto lIt = lhs.begin(), rIt = rhs.begin(); lIt != lhs.end(); ++lIt, ++rIt) {
        EXPECT_EQ(*lIt, *rIt);
    }
}

void assertCompEq(const mq::LineComponent& lhs, const mq::LineComponent& rhs) {
    cmpLists(lhs.pts, rhs.pts);
    cmpLists(lhs.speeds, rhs.speeds);
}

void sortComps(std::vector<mq::LineComponent>& v) {
    std::sort(v.begin(), v.end(), [](const auto& c1, const auto& c2) {
        return std::lexicographical_compare(c1.pts.begin(), c1.pts.end(), c2.pts.begin(), c2.pts.end());
    });
}

void testImpl(std::vector<mq::LineComponent> comps, std::vector<std::list<int64_t>> expectedId) {
    const auto n = expectedId.size();
    EXPECT_EQ(comps.size(), n);
    sortComps(comps);

    std::vector<mq::LineComponent> expectedComps;
    expectedComps.reserve(n);
    for (auto&& ids: expectedId) {
        expectedComps.emplace_back(createComp(std::move(ids)));
    }
    sortComps(expectedComps);

    for (size_t i = 0; i < n; ++i) {
        assertCompEq(comps[i], expectedComps[i]);
    }
}

Y_UNIT_TEST_SUITE(test_dsu)
{
    Y_UNIT_TEST(test_single_edge)
    {
        //  0     1
        // (*)-->(*)
        auto merged = mq::mergeLines({
            createEdge(0, 1)
        });

        testImpl(merged, {
            {0, 1}
        });
    }

    Y_UNIT_TEST(test_seq_edges)
    {
        //  0     1     2
        // (*)-->(*)-->(*)
        auto merged = mq::mergeLines({
            createEdge(0, 1),
            createEdge(1, 2)
        });

        testImpl(merged, {
            {0, 1, 2}
        });
    }

    Y_UNIT_TEST(test_cdir_edges)
    {
        //  0     1     2
        // (*)-->(*)<--(*)
        auto merged = mq::mergeLines({
            createEdge(0, 1),
            createEdge(2, 1)
        });

        testImpl(merged, {
            {0, 1},
            {2, 1}
        });
    }

    Y_UNIT_TEST(test_fork)
    {
        //                     3     4
        //                  ->(*)-->(*)
        //  0     1     2 /             \    7     8     9
        // (*)-->(*)-->(*)                ->(*)-->(*)-->(*)
        //                \    5     6  /
        //                  ->(*)-->(*)
        auto merged = mq::mergeLines({
            createEdge(0, 1),
            createEdge(1, 2),
            createEdge(2, 3),
            createEdge(3, 4),
            createEdge(4, 7),
            createEdge(2, 5),
            createEdge(5, 6),
            createEdge(6, 7),
            createEdge(7, 8),
            createEdge(8, 9)
        });

        testImpl(merged, {
            {0, 1, 2},
            {2, 3, 4, 7},
            {2, 5, 6, 7},
            {7, 8, 9},
        });
    }

    Y_UNIT_TEST(test_circle)
    {
        //     ->(*) -
        //   /    1    \
        //  |           v
        // (*)  <---   (*)
        //  0           2
        auto merged = mq::mergeLines({
            createEdge(0, 1),
            createEdge(1, 2),
            createEdge(2, 0)
        });

        testImpl(merged, {
            {0, 1, 2, 0}
        });
    }
}
