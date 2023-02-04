#define BOOST_TEST_MAIN

#include "../helpers.h"
#include <maps/libs/graph/po_helpers/include/graph_entity.h>

#include <boost/assign/std/vector.hpp>
#include <boost/test/unit_test.hpp>

#include <vector>

namespace mg = maps::graph2;
using boost::assign::operator +=;

using mg::po::EntitiesPaths;

void testGraphEntityHas(GraphEntity::Value entities,
        const std::vector<GraphEntity::Value>& entitiesVector)
{
    for (GraphEntity::Value entity : GraphEntity::all()) {
        BOOST_CHECK_EQUAL(
            GraphEntity::has(entities, entity),
            std::find(entitiesVector.begin(), entitiesVector.end(), entity)
                != entitiesVector.end()
            );
    }
}

BOOST_AUTO_TEST_CASE( entity ) {
    std::vector<GraphEntity::Value> entitiesVector;
    testGraphEntityHas(GraphEntity::NONE, entitiesVector);

    entitiesVector += GraphEntity::PRECALC;
    testGraphEntityHas(GraphEntity::PRECALC, entitiesVector);

    entitiesVector += GraphEntity::TOPOLOGY, GraphEntity::TITLES;
    testGraphEntityHas(GraphEntity::PRECALC | GraphEntity::TOPOLOGY |
                       GraphEntity::TITLES,
                       entitiesVector);

    entitiesVector += GraphEntity::EDGES_PERSISTENT_INDEX;
    entitiesVector += GraphEntity::COMPACT_PERSISTENT_INDEX;
    testGraphEntityHas(GraphEntity::PRECALC | GraphEntity::TOPOLOGY |
                       GraphEntity::TITLES | GraphEntity::EDGES_PERSISTENT_INDEX |
                       GraphEntity::COMPACT_PERSISTENT_INDEX,
                       entitiesVector);

    entitiesVector += GraphEntity::DATA,
                      GraphEntity::PATCH,
                      GraphEntity::EDGES_RTREE,
                      GraphEntity::SEGMENTS_RTREE,
                      GraphEntity::SPECIFIED_TURNS,
                      GraphEntity::TURN_PENALTIES,
                      GraphEntity::ROAD_GRAPH;
    checkEqualAsMultisets(entitiesVector, GraphEntity::all());
    BOOST_CHECK_EQUAL(entitiesVector.size(), GraphEntity::totalNumber());
}
