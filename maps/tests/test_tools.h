#pragma once

#include <maps/libs/xml/include/xml.h>

#include <boost/filesystem.hpp>
#include <library/cpp/testing/common/env.h>

#include <fstream>
#include <string>


const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() +
    "/maps/analyzer/services/jams_analyzer/modules/outputbuilder/tests/data";
const std::string GEOBASE_BIN_PATH = (TFsPath(GetWorkPath()) / "geodata6.bin").GetPath();
const std::string GRAPH_DATA_PATH = BinaryPath("maps/data/test/graph3/road_graph.fb");
const std::string EDGES_PERSISTENT_INDEX_PATH =
    BinaryPath("maps/data/test/graph3/edges_persistent_index.fb");
const std::string LEVELS_PATH = TEST_DATA_ROOT + "/speedmap.xml.default";
const std::string NONEXISTENT_PATH = "nonexistent_path";


enum Mode {
    OK,
    NO_GRAPH_DATA,
    NO_EDGES_PERS_IDX,
    NO_GEODATA,
    NO_SPEEDMAP,
};


std::string makeOutputBuilderConfig(
    const std::string& configFilename, Mode mode = OK,
    const std::string& hostsConf = "outputbuilder-hosts.conf"
) {
    const auto configPath = TEST_DATA_ROOT + "/" + configFilename;
    maps::xml3::Doc doc(configPath, maps::xml3::Doc::File);
    auto root = doc.root();
    root.addChild("path_to_hosts_config", TEST_DATA_ROOT + "/" + hostsConf);
    root.addChild("path_to_graph_data", mode != NO_GRAPH_DATA ? GRAPH_DATA_PATH : NONEXISTENT_PATH);
    root.addChild("path_to_edges_persistent_index", mode != NO_EDGES_PERS_IDX ? EDGES_PERSISTENT_INDEX_PATH : NONEXISTENT_PATH);
    root.addChild("path_to_geobase", mode != NO_GEODATA ? GEOBASE_BIN_PATH : NONEXISTENT_PATH);
    root.addChild("path_to_levels", mode != NO_SPEEDMAP ? LEVELS_PATH : NONEXISTENT_PATH);
    const auto configPathTmp = configPath + ".tmp" + "_" + std::to_string(mode);
    std::ofstream out(configPathTmp);
    root.save(out, "utf-8");
    out.close();
    return configPathTmp;
}
