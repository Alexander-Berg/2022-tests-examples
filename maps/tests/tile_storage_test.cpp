#include <maps/libs/common/include/exception.h>
#include <yandex/maps/coverage5/builder.h>
#include <yandex/maps/coverage5/cmd_helper.h>
#include <yandex/maps/coverage5/coverage.h>
#include <yandex/maps/coverage5/tile_storage.h>
#include <yandex/maps/mms/holder2.h>
#include <yandex/maps/mms/copy.h>

#include <library/cpp/testing/common/env.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>
#include <boost/filesystem.hpp>

#include <fstream>
#include <iostream>
#include <initializer_list>
#include <string>

#include <maps/libs/coverage/common/tree_copier.h>
#include <maps/libs/coverage/tools/common/xml2mms_convertor.h>
#include <maps/libs/coverage/spatial_limits.h>
#include "common.h"

using namespace maps::coverage5;

// using test binary path in arcadia build
static const std::string outDir(BuildRoot() + "/maps/libs/coverage/ut/tile_storage_test/");

std::string readFile(const std::string& fileName)
{
    std::ifstream ifs(fileName, std::ios::binary);
    std::string data( (std::istreambuf_iterator<char>(ifs)),
                      (std::istreambuf_iterator<char>()) );
    return data;
}

void checkCoverage(const std::string& inputFile, double tileSize, int detailedZoomTreshold)
{
    std::string tmpDir = outDir + "tmp/";
    boost::filesystem::path path = tmpDir;
    boost::filesystem::create_directory(path);

    try
    {
        // Generating a set of protobufs
        convert::XML2MMSConvertor convertor1(CmdHelper({
            "-o", tmpDir,
            "--tile-size", std::to_string(tileSize),
            "--detailed-zoom-treshold", std::to_string(detailedZoomTreshold),
            "--output-format", "protobuf"}));
        convertor1.convertCoverage(inputFile);

        // Generating tile storage
        convert::XML2MMSConvertor convertor2(CmdHelper({
            "-o", tmpDir,
            "--tile-size", std::to_string(tileSize),
            "--detailed-zoom-treshold", std::to_string(detailedZoomTreshold),
            "--output-format", "mms"}));
        convertor2.convertCoverage(inputFile);

        std::string rootFile = tmpDir + "root.mms";
        std::string treeFile = tmpDir + "tree.mms";

        std::string rootData = readFile(rootFile);
        std::string treeData = readFile(treeFile);
        TileStorage tileStorage(tileSize, SpatialRefSystem::Geodetic,
            {rootData.c_str(), rootData.size()},
            {treeData.c_str(), treeData.size()});

        const unsigned int numTilesX = 1 << tileStorage.getDepthX();
        const unsigned int numTilesY = 1 << tileStorage.getDepthY();

        for(unsigned int x = 0; x < numTilesX; ++x) {
            for(unsigned int y = 0; y < numTilesY; ++y) {
                std::string pbFile = tmpDir + "1_" + std::to_string(x) + "_" + std::to_string(y) + ".pb";
                BOOST_TEST(readFile(pbFile) == tileStorage.getTile(x, y),
                    "Predicate failed for coverage " << inputFile << ", tile [" << x << ", " << y << "]");
            }
        }
        BOOST_TEST(readFile(tmpDir + "0_0_0.pb") == tileStorage.getRootTile(),
            "Predicate failed for coverage " << inputFile << ", root tile");

    } catch (maps::Exception& e) {
        boost::filesystem::remove_all(path);
        throw e;
    }

    boost::filesystem::remove_all(path);
}

BOOST_AUTO_TEST_CASE(tile_storage_coverage)
{
    setTestsDataCwd();
    checkCoverage("indoor.xml", 11.25, 1);
}

BOOST_AUTO_TEST_CASE(stl_coverage)
{
    setTestsDataCwd();
    checkCoverage("stl.xml", 1.40625, 5);
}
