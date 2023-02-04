#define BOOST_TEST_MAIN

#include "../helpers.h"

#include <library/cpp/testing/common/env.h>

#include <maps/libs/graph/po_helpers/include/graph_objects.h>
#include <maps/libs/common/include/exception.h>

#include <boost/test/unit_test.hpp>

#include <stdexcept>

#include <unistd.h>

namespace mg = maps::graph2;

const std::string GRAPH_FOLDER_REL   = "maps/data/test/graph4/";

const std::string GRAPH_FOLDER       = BinaryPath(GRAPH_FOLDER_REL);
const std::string PATH_TO_TOPOLOGY   = BinaryPath(GRAPH_FOLDER_REL + "topology.mms.2");
const std::string PATH_TO_DATA       = BinaryPath(GRAPH_FOLDER_REL + "data.mms.2");
const std::string PATH_TO_PERS_INDEX = BinaryPath(GRAPH_FOLDER_REL + "edges_persistent_index.mms.1");
const std::string PATH_TO_ROAD_GRAPH = BinaryPath(GRAPH_FOLDER_REL + "road_graph.fb");
const std::string BINARY_PROG        = "./binary";


struct Identity {
    template<class T>
    const T& operator() (const T& t) const
    {
        return t;
    }
};

void testObjects(const std::vector<std::string>& arguments,
                Entity entities,
                const std::string& version)
{
    boost_po::options_description optionsDescr("Options");
    mg::GraphObjects objects(&optionsDescr, entities);
    BOOST_CHECK(!objects.areLoaded());

    boost_po::variables_map vm = createVariablesMap(arguments, optionsDescr);
    BOOST_CHECK(!objects.areLoaded());

    for (size_t t = 0; t < 2; ++t) {
        // check that second load does not change anything
        objects.load();
        BOOST_CHECK(objects.areLoaded());

        testVersionsFromCollection(objects, entities, version, Identity());
        BOOST_CHECK(objects.areLoaded());
    }
}

std::string activatedVersion() {
    const size_t MAX_PATH_LENGTH = 1000;
    const char* GRAPH_PATH = GRAPH_FOLDER.c_str();

    std::vector<char> buffer(MAX_PATH_LENGTH);
    ssize_t length = readlink(GRAPH_PATH , &buffer[0], buffer.size());
    if (length == -1 || length == static_cast<ssize_t>(buffer.size())) {
        BOOST_TEST_MESSAGE("Could not read symbolic link from " << GRAPH_PATH);
        return "";
    }
    std::string activatedGraphPath = std::string(&buffer[0], length);
    return filenameFromPath(fs::path(activatedGraphPath));
}

BOOST_AUTO_TEST_CASE( objects_test ) {
    if (activatedVersion() != "") {
        testObjects({BINARY_PROG},
            GraphEntity::DATA | GraphEntity::TOPOLOGY | GraphEntity::EDGES_RTREE
            | GraphEntity::PATCH | GraphEntity::TITLES,
            activatedVersion());
    }

    testObjects(
        {
            BINARY_PROG,
            "--graph-data", PATH_TO_DATA,
            "--graph-topology", PATH_TO_TOPOLOGY,
            "--graph-edges-persistent-index", PATH_TO_PERS_INDEX,
            "--graph-road-graph", PATH_TO_ROAD_GRAPH,
            "--graph-version", "1543"
        },
        GraphEntity::DATA | GraphEntity::TOPOLOGY | GraphEntity::EDGES_PERSISTENT_INDEX
        | GraphEntity::ROAD_GRAPH,
        "4.0.0");

    testObjects(
        {
            BINARY_PROG,
            "--graph-folder", GRAPH_FOLDER,
            "--filename-format", "%ENTITY%.%FILE_FORMAT%%MMS_VERSION%"
        },
        GraphEntity::DATA | GraphEntity::TOPOLOGY | GraphEntity::EDGES_RTREE
        | GraphEntity::EDGES_PERSISTENT_INDEX | GraphEntity::ROAD_GRAPH,
        "4.0.0");
}


BOOST_AUTO_TEST_CASE( objects_pointers_test ) {
    boost_po::options_description optionsDescr("Options");
    mg::GraphObjects objects(&optionsDescr, GraphEntity::DATA);
    std::vector<std::string> arguments = {
        "./binary",
        "--graph-version", "version",
        "--graph-data", PATH_TO_DATA.c_str()
    };
    boost_po::variables_map vm = createVariablesMap(arguments, optionsDescr);
    objects.load();

    BOOST_CHECK_THROW(objects.getPointer<GraphEntity::TOPOLOGY>(),
                      std::out_of_range);
    BOOST_CHECK_THROW(objects.getPointer<GraphEntity::EDGES_PERSISTENT_INDEX>(),
                      std::out_of_range);

    auto* ptr = objects.getPointer<GraphEntity::DATA>();
    auto& ref = objects.get<GraphEntity::DATA>();
    BOOST_CHECK_EQUAL(ptr, &ref);
    BOOST_CHECK_EQUAL(ptr->version(), ref.version());
}

BOOST_AUTO_TEST_CASE( persistent_index_test ) {
    boost_po::options_description optionsDescr("Options");
    mg::GraphObjects objects(&optionsDescr, GraphEntity::EDGES_PERSISTENT_INDEX);
    std::vector<std::string> arguments = {
        "./binary",
        "--graph-edges-persistent-index", PATH_TO_PERS_INDEX.c_str()
    };
    boost_po::variables_map vm = createVariablesMap(arguments, optionsDescr);

    objects.load();
    auto* ptr = objects.getPointer<GraphEntity::EDGES_PERSISTENT_INDEX>();
    auto& ref = objects.get<GraphEntity::EDGES_PERSISTENT_INDEX>();
    BOOST_CHECK_EQUAL(ptr, &ref);
    BOOST_CHECK_EQUAL(ptr->version(), ref.version());
    BOOST_CHECK(ref.size() > 0);

    bool foundAny = false;
    for (size_t i = 0; i < ref.size() && i < 1000; ++i) {
        auto longId = ref.findLongId(i);
        if (longId) {
            BOOST_CHECK_EQUAL(ref.shortId(*longId), i);
            foundAny = true;
        }
    }
    BOOST_CHECK(foundAny);
}

BOOST_AUTO_TEST_CASE( road_graph_test ) {
    boost_po::options_description optionsDescr("Options");
    mg::GraphObjects objects(&optionsDescr, GraphEntity::ROAD_GRAPH);
    std::vector<std::string> arguments = {
        "./binary",
        "--graph-road-graph", PATH_TO_ROAD_GRAPH.c_str()
    };
    boost_po::variables_map vm = createVariablesMap(arguments, optionsDescr);

    objects.load();
    auto* ptr = objects.getPointer<GraphEntity::ROAD_GRAPH>();
    auto& ref = objects.get<GraphEntity::ROAD_GRAPH>();
    BOOST_CHECK_EQUAL(ptr, &ref);
    BOOST_CHECK_EQUAL(ptr->version(), ref.version());
}
