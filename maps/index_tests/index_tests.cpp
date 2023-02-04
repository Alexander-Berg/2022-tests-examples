#include <library/cpp/testing/unittest/registar.h>

#include "../helpers.h"

#include <chrono>
#include <algorithm>

namespace maps {
namespace gpstiles_realtime {
namespace index {
namespace test {

struct TestData {
    TestIndexPtr index;
    PointsVector expPs;
};

TestData buildTestData(TestIndexData data, const Query& query)
{
    PointsVector expPs;
    for (const auto& iData : data) {
        for (const auto& p : iData.second) {
            UNIT_ASSERT(iData.first.contains(p.time()));
            if (query.timeInterval().contains(p.time())
                && geolib3::intersects(query.bbox(), p.point.boundingBox()))
            {
                expPs.push_back(p);
            }
        }
    }

    return {createTestIndex(std::move(data)), std::move(expPs)};
}

PointsVector buildTestResult(const TestIndexPtr& idx, const Query& query)
{
    const auto& res = idx->find(query);

    PointsVector recvPs;
    for (const auto& p : res) {
        recvPs.push_back(p);
    }

    return recvPs;
}

void checkEqual(const PointsVector& recvPs, const PointsVector& expPs)
{
    UNIT_ASSERT(recvPs.size() == expPs.size());
    for (const auto& p : expPs) {
        UNIT_ASSERT(std::find(recvPs.begin(), recvPs.end(), p) != recvPs.end());
    }
    for (const auto& p : recvPs) {
        UNIT_ASSERT(std::find(expPs.begin(), expPs.end(), p) != expPs.end());
    }
}

void runSimpleTest(TestIndexData data, const Query& q)
{
    auto td = buildTestData(std::move(data), q);
    checkEqual(buildTestResult(td.index, q), td.expPs);
}

Y_UNIT_TEST_SUITE(index_tests)
{

// num in [0..10)
const geolib3::Point2& getPoint(size_t num)
{
    static const geolib3::PointsVector POINTS = {
        {-1.0, 1.0},
        {1.0, -1.0},
        {-1.0, 2.0},
        {-10.0, 10.0},
        {-2.0, 2.0},
        {10.0, -10.0},
        {-5.0, -5.0},
        {-3.0, 4.0},
        {-1.0, 5.0},
        {0.0, 0.0}
    };

    return POINTS.at(num);
}

const TestIntervalIndexData I_1_DATA = {
    TimeInterval {getTime(3), getTime(8)},
    {
        {getPoint(0), getTime(3)},
        {getPoint(1), getTime(4)},
        {getPoint(2), getTime(5)},
        {getPoint(3), getTime(6)},
        {getPoint(4), getTime(7)},
        {getPoint(5), getTime(8)},
        {getPoint(6), getTime(3)}
    }
};

const TestIntervalIndexData I_2_DATA = {
    TimeInterval {getTime(11), getTime(17)},
    {
        {getPoint(2), getTime(11)},
        {getPoint(4), getTime(12)},
        {getPoint(1), getTime(13)},
        {getPoint(5), getTime(14)},
        {getPoint(3), getTime(15)},
        {getPoint(9), getTime(16)},
        {getPoint(0), getTime(17)},
        {getPoint(8), getTime(13)},
        {getPoint(6), getTime(15)}
    }
};

const TestIntervalIndexData I_3_DATA = {
    TimeInterval {getTime(6), getTime(11)},
    {
        {getPoint(1), getTime(6)},
        {getPoint(2), getTime(7)},
        {getPoint(0), getTime(8)},
        {getPoint(3), getTime(9)},
        {getPoint(4), getTime(10)},
        {getPoint(5), getTime(11)}
    }
};

const TestIntervalIndexData I_4_DATA = {
    TimeInterval {getTime(13), getTime(15)},
    {
        {getPoint(1), getTime(13)},
        {getPoint(2), getTime(14)},
        {getPoint(0), getTime(15)},
        {getPoint(3), getTime(13)},
        {getPoint(4), getTime(14)},
        {getPoint(5), getTime(15)}
    }
};

const TestIntervalIndexData I_5_DATA = {
    TimeInterval {getTime(1), getTime(9)},
    {
        {getPoint(2), getTime(1)},
        {getPoint(4), getTime(2)},
        {getPoint(1), getTime(3)},
        {getPoint(5), getTime(4)},
        {getPoint(3), getTime(5)},
        {getPoint(9), getTime(6)},
        {getPoint(0), getTime(7)},
        {getPoint(8), getTime(8)},
        {getPoint(6), getTime(9)}
    }
};

Y_UNIT_TEST(contained_interval_test)
{
    TestIndexData data = {
        I_1_DATA,
        I_5_DATA
    };

    Query q(
        {getTime(3), getTime(6)},
        {{-5.0, -5.0}, {5.0, 5.0}});


    runSimpleTest(std::move(data), q);
}

Y_UNIT_TEST(covered_interval_test)
{
    TestIndexData data = {
        I_1_DATA,
        I_5_DATA
    };

    Query q(
        {getTime(0), getTime(10)},
        {{-5.0, -5.0}, {5.0, 5.0}});

    runSimpleTest(std::move(data), q);
}

Y_UNIT_TEST(overlapping_intervals_test)
{
    TestIndexData data = {
        I_1_DATA,
        I_3_DATA
    };

    Query q(
        {getTime(5), getTime(10)},
        {{-5.0, -5.0}, {5.0, 5.0}});

    runSimpleTest(std::move(data), q);
}

Y_UNIT_TEST(touching_intervals_test)
{
    TestIndexData data = {
        I_2_DATA,
        I_3_DATA
    };

    Query q(
        {getTime(10), getTime(12)},
        {{-5.0, -5.0}, {5.0, 5.0}});

    runSimpleTest(std::move(data), q);
}

Y_UNIT_TEST(disjoint_intervals_test)
{
    TestIndexData data = {
        I_1_DATA,
        I_4_DATA
    };

    Query q(
        {getTime(9), getTime(12)},
        {{-20.0, -20.0}, {20.0, 20.0}});

    runSimpleTest(std::move(data), q);
}

Y_UNIT_TEST(test_remove_outdated)
{
    TestIndexData data = {
        I_1_DATA,
        I_2_DATA,
        I_3_DATA,
        I_4_DATA,
        I_5_DATA
    };

    Query q(
        {getTime(0), getTime(5)},
        {{-20.0, -20.0}, {20.0, 20.0}});

    auto testData = buildTestData(std::move(data), q);
    auto res = buildTestResult(testData.index, q);
    checkEqual(res, testData.expPs);

    testData.index->remove(getTime(10)); // removes all I_1 and I_5 ata
    auto i1 = I_1_DATA.first;
    auto i5 = I_5_DATA.first;
    res.erase(
        std::remove_if(
            res.begin(), res.end(),
            [i1, i5] (const TestPoint& p)
            {
                return i1.contains(p.time()) || i5.contains(p.time());
            }),
        res.end());
    checkEqual(buildTestResult(testData.index, q), res);

    testData.index->remove(getTime(12)); // removes all I_3 data
    auto i3 = I_3_DATA.first;
    res.erase(
        std::remove_if(
            res.begin(), res.end(),
            [i3] (const TestPoint& p) { return i3.contains(p.time()); }),
        res.end());
    checkEqual(buildTestResult(testData.index, q), res);

    testData.index->remove(getTime(18)); // removes all data
    q = Query({getTime(0), getTime(18)}, q.bbox());
    checkEqual(buildTestResult(testData.index, q), {});
}

} // test suite end

} // namespace test
} // namespace index
} // namespace gpstiles_realtime
} // namespace maps
