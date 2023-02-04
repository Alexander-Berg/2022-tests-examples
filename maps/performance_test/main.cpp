#include <maps/libs/road_graph/include/graph.h>
#include <maps/libs/succinct_rtree/include/rtree.h>
#include <maps/libs/cmdline/include/cmdline.h>

#include <util/datetime/cputimer.h>
#include <util/random/fast.h>
#include <util/system/compiler.h>

#include <iostream>

using maps::geolib3::Point2;
using maps::geolib3::BoundingBox;
using maps::road_graph::Graph;
using maps::succinct_rtree::Rtree;

double randRange(TFastRng32& rng, double min, double max) {
    return min + rng.GenRandReal1() * (max - min);
}

const double MIN_LON = -8.651126;
const double MIN_LAT = 33.741070;
const double MAX_LON = 73.966062;
const double MAX_LAT = 54.231815;

template <class F>
size_t nearestBase(uint32_t seed, F f) {
    const size_t NUMBER = 20;
    const double CUTOFF_DISTANCE = 10000;
    TFastRng32 rng(seed, 0);
    size_t foundNumber = 0;

    for (size_t i = 0; i != 100000; ++i) {
        const Point2 point{
            randRange(rng, MIN_LON, MAX_LON),
            randRange(rng, MIN_LAT, MAX_LAT)};
        const auto range = f(point, CUTOFF_DISTANCE);
        auto iter = range.begin();
        for (size_t j = 0; j != NUMBER && iter != range.end();
                ++j, ++iter) {
            ++foundNumber;
        }
    }

    return foundNumber;
}

size_t nearestBaseEdges(const Rtree& rtree) {
    TTimeLogger logger("nearestBaseEdges");
    const uint32_t RANDOM_SEED = 9865932;
    return nearestBase(RANDOM_SEED, [&](Point2 point, double d) {
        return rtree.nearestBaseEdges(point, d);
    });
}

size_t nearestBaseSegments(const Rtree& rtree) {
    TTimeLogger logger("nearestBaseSegments");
    const uint32_t RANDOM_SEED = 256946;
    return nearestBase(RANDOM_SEED, [&](Point2 point, double d) {
        return rtree.nearestBaseSegments(point, d);
    });
}

template <class F>
size_t baseInWindow(uint32_t seed, F f) {
    TFastRng32 rng(seed, 0);
    size_t foundNumber = 0;

    for (size_t i = 0; i != 100000; ++i) {
        const Point2 point{
            randRange(rng, MIN_LON, MAX_LON),
            randRange(rng, MIN_LAT, MAX_LAT)};
        const BoundingBox bbox {
            point, 1e-2, 1e-2};
        for (const auto result: f(bbox)) {
            Y_UNUSED(result);
            ++foundNumber;
        }
    }

    return foundNumber;
}

size_t baseEdgesInWindow(const Rtree& rtree) {
    TTimeLogger logger("baseEdgesInWindow");
    const uint32_t RANDOM_SEED = 6924874;
    return baseInWindow(RANDOM_SEED, [&](BoundingBox bbox) {
        return rtree.baseEdgesInWindow(bbox);
    });
}

size_t baseSegmentsInWindow(const Rtree& rtree) {
    TTimeLogger logger("baseSegmentsInWindow");
    const uint32_t RANDOM_SEED = 1298979;
    return baseInWindow(RANDOM_SEED, [&](BoundingBox bbox) {
        return rtree.baseSegmentsInWindow(bbox);
    });
}

int main(int argc, char** argv) {
    maps::cmdline::Parser parser;
    const auto graphPath =
        parser.file("graph", 'g').required().help("Path to road graph");
    const auto rtreePath =
        parser.file("rtree", 'r').required().help("Path to rtree");
    const auto mode =
        parser.string("mode", 'm').defaultValue("ne").help(
                "Mode of operation.\n"
                "ne = nearestBaseEdges\n"
                "ns = nearestBaseSegments\n"
                "be = baseEdgesInWindow\n"
                "bs = baseSegmentsInWindow");

    parser.parse(argc, argv);

    Graph graph{graphPath};
    Rtree rtree{rtreePath, graph};

    if (mode == "ne") {
        std::cout << "Total edges: " << nearestBaseEdges(rtree) << '\n';
    } else if (mode == "ns") {
        std::cout << "Total segments: " << nearestBaseSegments(rtree) << '\n';
    } else if (mode == "be") {
        std::cout << "Total edges: " << baseEdgesInWindow(rtree) << '\n';
    } else if (mode == "bs") {
        std::cout << "Total segments: " << baseSegmentsInWindow(rtree) << '\n';
    }

    return 0;
}
