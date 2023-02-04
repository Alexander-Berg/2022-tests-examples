#pragma once

#include <maps/libs/leptidea/tests/lib/test_data_generation.h>

/*
 * "--->" -- toll-free edge
 * "===>" -- toll edge
 *
 *         ^ Y
 *         |
 *         |         4
 *         |        /^
 *         |     e5/ |
 *         |      /  |e3
 *         | e2  /   |
 *         1========>2
 *         ^   /     ^
 *       e0|  /      |e4
 *         | /       |
 *         |L        |
 *         0-------->3-------------> X
 *              e1
 */

struct Graph5V6EFixture : SmallGraph {
    Graph5V6EFixture() : SmallGraph(
        TopologyDescription{{0, 1, 2, 3}, {4}},
        Edges{
            {0, 1}, {0, 3}, {1, 2}, {2, 4}, {3, 2}, {4, 0}},
        []() {
            std::vector<rg::MutableEdgeData> edgesData(
                6, buildEdgeData(1 /*speed*/, false /*toll*/, rg::AccessId::Automobile));

            edgesData[2] =
                buildEdgeData(2 /*speed*/, true /*toll*/, rg::AccessId::Automobile);
            setLengths(&edgesData, {1, 1, 1, 1, 1, 1});
            edgesData[2].endsWithTollPost = true;

            edgesData[5].length = 2.236067;

            return edgesData;
        }(),
        {},
        {},
        {})
    {}
    leptidea7::RoadTraits traits = RoadTrait::AllowedForAuto;
    leptidea7::RoadTraits tollFreeTraits = RoadTrait::TollFree | RoadTrait::AllowedForAuto;
};
