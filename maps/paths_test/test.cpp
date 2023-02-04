#define BOOST_TEST_MAIN

#include "../helpers.h"

#include <maps/libs/graph/po_helpers/include/graph_paths.h>
#include <maps/libs/common/include/exception.h>

#include <boost/assign/list_of.hpp>
#include <boost/assign/std/vector.hpp>
#include <boost/test/unit_test.hpp>

#include <map>

namespace mg = maps::graph2;
namespace boost_po = boost::program_options;

using namespace boost::assign;
using mg::po::EntitiesPaths;


void testPaths(
        const std::vector<std::string>& arguments,
        const EntitiesPaths& correctPaths,
        Entity unusedEntities) {
    boost_po::options_description optionsDescr("Options");

    Entity entities = GraphEntity::NONE;
    for (auto item : correctPaths)
        entities |= item.first;

    mg::po::GraphPaths paths(&optionsDescr, entities);

    boost_po::variables_map vm = createVariablesMap(arguments, optionsDescr);

    for (auto item : correctPaths)
        BOOST_CHECK_EQUAL(paths.get(item.first), item.second);


    for (Entity entity : GraphEntity::all()) {
        if (GraphEntity::has(unusedEntities, entity)) {
            BOOST_CHECK_THROW(paths.get(entity), std::exception);
        }
    }
}

void testPathsExcept(const std::vector<std::string>& arguments,
                     Entity usedEntities)
{
    boost_po::options_description optionsDescr("Options");

    mg::po::GraphPaths paths(&optionsDescr, usedEntities);

    boost_po::variables_map vm = createVariablesMap(arguments, optionsDescr);

    for (Entity entity : GraphEntity::all()) {
        if (GraphEntity::has(usedEntities, entity)) {
            BOOST_CHECK_THROW(paths.get(entity), std::exception);
        } else {
            BOOST_CHECK_THROW(paths.get(entity), std::exception);
        }
    }
}

BOOST_AUTO_TEST_CASE( paths_test )
{
    testPaths(list_of("./binary"), map_list_of
        (GraphEntity::TOPOLOGY,
                    "/usr/share/yandex/maps/graph/topology.mms.2")
        (GraphEntity::EDGES_RTREE,
                    "/usr/share/yandex/maps/graph/edges_rtree.mms.2")
        (GraphEntity::PATCH,
                    "/usr/share/yandex/maps/graph/graph_patch_forward.mms.1"),
        GraphEntity::DATA | GraphEntity::PRECALC | GraphEntity::TITLES);

    testPaths(list_of("./binary")("--graph-folder")("folder"), map_list_of
        (GraphEntity::TOPOLOGY,
                    "folder/topology.mms.2")
        (GraphEntity::EDGES_RTREE,
                    "folder/edges_rtree.mms.2")
        (GraphEntity::PRECALC,
                    "folder/graph_precalc.mms.2")
        (GraphEntity::DATA,
                    "folder/data.mms.2")
        (GraphEntity::PATCH,
                    "folder/graph_patch_forward.mms.1")
        (GraphEntity::TITLES,
                    "folder/titles.mms.2")
        (GraphEntity::SEGMENTS_RTREE,
                    "folder/segments_rtree.mms.2")
        (GraphEntity::SPECIFIED_TURNS,
                    "folder/specified_turns.mms.1")
        (GraphEntity::TURN_PENALTIES,
                    "folder/turn_penalties.mms.2")
        (GraphEntity::EDGES_PERSISTENT_INDEX,
                    "folder/edges_persistent_index.mms.1")
        (GraphEntity::ROAD_GRAPH,
                    "folder/road_graph.fb"),
        GraphEntity::NONE);

    testPaths(list_of("./binary")
                     ("--graph-data")("data")
                     ("--graph-patch")("patch")
                     ("--graph-edges-rtree")("rtree")
                     ("--graph-titles")("titles")
                     ("--graph-topology")("topology")
                     ("--graph-version")("version")
                     ("--graph-precalc")("precalc"),
              map_list_of(GraphEntity::TOPOLOGY, "topology")
                         (GraphEntity::PRECALC, "precalc")
                         (GraphEntity::DATA, "data")
                         (GraphEntity::PATCH, "patch")
                         (GraphEntity::EDGES_RTREE, "rtree")
                         (GraphEntity::TITLES, "titles"),
              GraphEntity::NONE
              );

    testPaths(list_of("./binary")
                     ("--graph-data")("my_data")
                     ("--graph-version")("15.4.3")
                     ("--graph-titles")("my_titles"),
              map_list_of(GraphEntity::TOPOLOGY,
                  "/var/spool/yandex/maps/graph/15.4.3/topology.mms.2")
                         (GraphEntity::DATA, "my_data")
                         (GraphEntity::EDGES_RTREE,
                  "/var/spool/yandex/maps/graph/15.4.3/edges_rtree.mms.2")
                         (GraphEntity::TITLES, "my_titles"),
              GraphEntity::PRECALC | GraphEntity::PATCH);

    testPaths(list_of("./binary")
                     ("--graph-data")("my_data")
                     ("--graph-titles")("my_titles"),
              map_list_of(GraphEntity::TOPOLOGY,
                          "/usr/share/yandex/maps/graph/topology.mms.2")
                         (GraphEntity::DATA, "my_data")
                         (GraphEntity::EDGES_RTREE,
                          "/usr/share/yandex/maps/graph/edges_rtree.mms.2")
                         (GraphEntity::TITLES, "my_titles"),
              GraphEntity::PRECALC | GraphEntity::PATCH);

    testPaths(list_of("./binary")
                     ("--graph-data")("my_data")
                     ("--graph-titles")("my_titles")
                     ("--graph-folder")("/graph"),
              map_list_of(GraphEntity::TOPOLOGY,
                          "/graph/topology.mms.2")
                         (GraphEntity::DATA, "my_data")
                         (GraphEntity::TITLES, "my_titles"),
              GraphEntity::EDGES_RTREE | GraphEntity::PRECALC |
              GraphEntity::PATCH
              );

    testPaths(list_of("./binary")
                     ("--graph-folder")("/home/user/graph_folder/")
                     ("--graph-data")("custom_data")
                     ("--filename-format")("test_%ENTITY%.mms.2"),
              map_list_of(GraphEntity::DATA, "custom_data")
                     (GraphEntity::TOPOLOGY,
                     "/home/user/graph_folder/test_topology.mms.2")
                     (GraphEntity::TITLES,
                     "/home/user/graph_folder/test_titles.mms.2"),
              GraphEntity::EDGES_RTREE | GraphEntity::PRECALC |
              GraphEntity::PATCH);

    testPaths(list_of("./binary")
                     ("--graph-folder")("/home/user/graph_folder/")
                     ("--graph-data")("custom_data")
                     ("--filename-format")("test_%ENTITY%.%FILE_FORMAT%%MMS_VERSION%"),
              map_list_of(GraphEntity::DATA, "custom_data")
                     (GraphEntity::TOPOLOGY,
                     "/home/user/graph_folder/test_topology.mms.2")
                     (GraphEntity::TITLES,
                     "/home/user/graph_folder/test_titles.mms.2")
                     (GraphEntity::EDGES_PERSISTENT_INDEX,
                     "/home/user/graph_folder/test_edges_persistent_index.mms.1")
                     (GraphEntity::ROAD_GRAPH,
                     "/home/user/graph_folder/test_road_graph.fb"),
              GraphEntity::EDGES_RTREE | GraphEntity::PRECALC |
              GraphEntity::PATCH);

    testPathsExcept(list_of("./binary")
                     ("--graph-data")("my_data")
                     ("--graph-titles")("my_titles")
                     ("--graph-folder")("/graph")
                     ("--graph-version")("1.2.3"),
                     GraphEntity::DATA | GraphEntity::TITLES |
                     GraphEntity::PRECALC);

}
