#include "../../alternatives_selector/sharing_comparators.h"
#include "utils.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using maps::geolib3::Point2;
using maps::routing::PathSegment;

using maps::routing::alternatives_selector::weakOrder;
using maps::routing::alternatives_selector::SharingComparator;
using maps::routing::alternatives_selector::ComparingResult;

namespace {

PathSegment makePathSegment(uint32_t edgeId, double length) {
    return PathSegment(maps::road_graph::EdgeId(edgeId), length, 0, 0, 0, 0);
}

Route makeRouteByIndex(
    const std::vector<PathSegment>& pathSegments,
    const std::vector<uint32_t>& indices)
{
    Route result;

    for (const auto& index: indices) {
        result.pathSegments.emplace_back(pathSegments[index]);
    }

    result.requestPoints = {
        RequestPointWithPosition(
            0,
            RequestPoint(Point2(0, 0))),
        RequestPointWithPosition(
            result.pathSegments.size(),
            RequestPoint(Point2(0, 0)))
    };

    return result;
}

RouterResult makeRouteResult(const Route& route, int index) {
    RouterResult result(VehicleParameters(), Avoid(), route, &ROAD_GRAPH);
    result.alternativeIndex = index;
    return result;
}

} // namespace

double sharingTest(const AlternativeInfo& a, const AlternativeInfo& b) {
    double result, lenghtA, lengthB;
    result = lenghtA = lengthB = 0;
    std::set<maps::road_graph::EdgeId> edges;
    for (const auto& pathSegment: a.result.route.pathSegments) {
        edges.insert(pathSegment.edgeId);
        lenghtA += pathSegment.length;
    }
    for (const auto& pathSegment: b.result.route.pathSegments) {
        if (edges.count(pathSegment.edgeId)) {
            result += pathSegment.length;
        }
        lengthB += pathSegment.length;
    }
    return result / std::min(lenghtA, lengthB);
}

double maxSharing(const std::vector<AlternativeInfo>& selected, const AlternativeInfo& a) {
    double result = 0;
    for (const auto& alternative: selected) {
        result = std::max(result, sharingTest(alternative, a));
    }
    return result;
}

ComparingResult compareTest(
        const std::vector<AlternativeInfo>& selected,
        const AlternativeInfo& a,
        const AlternativeInfo& b) {
    bool sharingA, sharingB;
    sharingA = maxSharing(selected, a) >= 0.7;
    sharingB = maxSharing(selected, b) >= 0.7;
    return weakOrder(sharingA, sharingB);
}

void runTest(
        const std::vector<AlternativeInfo>& alternatives,
        const std::vector<AlternativeInfo>& queries) {
    SharingComparator comparator(alternatives, 0.7 /* maxRelativeSharing */);
    for (size_t i = 0; i < queries.size(); i ++) {
        for (size_t j = 0; j < queries.size(); j++) {
            auto resultTest = compareTest(alternatives, queries[i], queries[j]);
            auto result = comparator.compare(queries[i], queries[j]);
            UNIT_ASSERT_EQUAL(result, resultTest);
        }
    }
}

Y_UNIT_TEST_SUITE(alternative_sharing_tests) {

Y_UNIT_TEST(sharing_tests_1) {
    std::vector<double> lengths{1.0, 2.09, 3.4, 4.5, 3.2, 2.2, 1.5, 6.2, 8.2, 5.2};
    std::vector<PathSegment> pathSegments;
    pathSegments.reserve(lengths.size());
    for (uint32_t i = 0; i < lengths.size(); i++) {
        pathSegments.emplace_back(makePathSegment(i, lengths[i]));
    }

    std::vector<AlternativeInfo> alternatives, queries;
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 1, 2, 3, 9}), 1));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 1, 2, 3, 4, 9}), 2));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 3, 4, 5, 6, 2, 7, 9}), 3));

    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 3, 5, 6, 9}), 4));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 1, 2, 4, 7, 9}), 5));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 5, 6, 7, 8, 9}), 6));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 1, 8, 4, 2, 9}), 7));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 1, 5, 2, 6, 9}), 8));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 4, 7, 2, 6, 9}), 9));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {0, 3, 5, 7, 8, 6, 9}), 10));

    runTest(alternatives, queries);
}

Y_UNIT_TEST(sharing_tests_2) {
    std::vector<double> lengths{9.1, 11.2, 20, 12.7, 13.1, 14.9, 7.3, 24.3, 17.8, 43.2};
    std::vector<PathSegment> pathSegments;
    pathSegments.reserve(lengths.size());
    for (uint32_t i = 0; i < lengths.size(); i++) {
        pathSegments.emplace_back(makePathSegment(i, lengths[i]));
    }

    std::vector<AlternativeInfo> alternatives, queries;
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 9, 3, 6}), 1));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 2, 5, 8, 6}), 2));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 3, 7, 8, 4, 6,}), 3));

    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 3, 4, 6}), 4));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 2, 7, 8, 6}), 5));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 5, 7, 8, 9, 6}), 6));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 8, 4, 2, 6}), 7));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 5, 2, 6}), 8));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 4, 7, 2, 6}), 9));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {1, 3, 5, 7, 8, 6}), 10));

    runTest(alternatives, queries);
}

Y_UNIT_TEST(sharing_tests_3) {
    std::vector<double> lengths{100.3, 29.4, 39.4, 24.5, 86.1, 67.2, 93.6, 88.8, 32.2, 55.9};
    std::vector<PathSegment> pathSegments;
    pathSegments.reserve(lengths.size());
    for (uint32_t i = 0; i < lengths.size(); i++) {
        pathSegments.emplace_back(makePathSegment(i, lengths[i]));
    }

    std::vector<AlternativeInfo> alternatives, queries;
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 1, 3, 9, 2}), 1));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 1, 5, 3, 4, 9, 2}), 2));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 3, 4, 5, 6, 2}), 3));

    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 3, 5, 6, 2}), 4));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 1, 4, 7, 2}), 5));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 5, 6, 7, 2}), 6));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 1, 4, 9, 2}), 7));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 1, 5, 6, 2}), 8));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 4, 7, 6, 2}), 9));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {8, 3, 5, 7, 6, 2}), 10));

    runTest(alternatives, queries);
}

Y_UNIT_TEST(sharing_tests_4) {
    std::vector<double> lengths{105.8, 215.3, 500.0, 322.1, 144.8, 609.3, 282.5, 126.9, 707.2, 488.7};
    std::vector<PathSegment> pathSegments;
    pathSegments.reserve(lengths.size());
    for (uint32_t i = 0; i < lengths.size(); i++) {
        pathSegments.emplace_back(makePathSegment(i, lengths[i]));
    }

    std::vector<AlternativeInfo> alternatives, queries;
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 1, 6, 3, 2}), 1));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 1, 7, 3, 5, 2}), 2));
    alternatives.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 3, 8, 5, 6, 9, 7, 2}), 3));

    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 3, 5, 6, 2}), 4));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 1, 5, 8, 7, 2}), 5));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 5, 6, 7, 8, 2}), 6));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 1, 8, 9, 3, 2}), 7));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 1, 5, 3, 6, 2}), 8));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 9, 7, 3, 6, 2}), 9));
    queries.emplace_back(makeRouteResult(makeRouteByIndex(pathSegments, {4, 3, 5, 7, 8, 6, 2}), 10));

    runTest(alternatives, queries);
}

}
