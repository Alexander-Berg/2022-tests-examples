#include "../include/MapDestroyTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"
#include <boost/filesystem.hpp>

using namespace boost::unit_test;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::test;
namespace fs = boost::filesystem;

namespace
{
    const char* origMapXmlFileName = "tests/boost-tests/maps/StaticMapCreation.xml";
    const char* mapXmlFileName = "tmp/StaticMapCreation.xml";
    const char* sourceFile = "tests/boost-tests/data/TwoPlineTest.mif";

    void beginState()
    {
        const std::string tmpDir = io::tempDirPath();
        if (!fs::exists(tmpDir))
            fs::create_directories(tmpDir);
        else
            map::deleteFilesFromDir(tmpDir);
    }

    size_t countFilesInDir(
        const std::string& dirName = io::tempDirPath())
    {
        size_t countFiles = 0;

        fs::directory_iterator dIt(io::tempDirPath());
        fs::directory_iterator dItEnd;

        for (; dIt != dItEnd; ++dIt)
            ++countFiles;

        return countFiles;
    }
}

void map::createAndCloseMapTest()
{
    beginState();

#ifdef _WIN32
    const size_t expectedCountFiles = 4;
#else
    const size_t expectedCountFiles = 1;
#endif


    {
        core::IMapGuiPtr mapGui = map::createTestMapGui();

        io::file::write(io::file::open(origMapXmlFileName), mapXmlFileName);

        BOOST_REQUIRE_NO_THROW(mapGui->loadFromXml(mapXmlFileName, true));

        BOOST_REQUIRE_NO_THROW(mapGui->open(map::createProgressStub()));

        mapGui->close();

        BOOST_REQUIRE(countFilesInDir() == expectedCountFiles);
    }

    BOOST_REQUIRE(countFilesInDir() == expectedCountFiles);

    map::deleteFilesFromTmpDir();
}

void map::destroyDynamicMapTest()
{
    beginState();
    {
        core::IMapGuiPtr mapGui = createTestMapGui();

        BOOST_REQUIRE_NO_THROW(
            mapGui->open(createProgressStub()));

        BOOST_REQUIRE_NO_THROW(
            mapGui->addLayerFromSource(
                createProgressStub(),
                io::path::absolute(sourceFile),
                0));

        // files: .index, .sqlite, .geometry
        BOOST_REQUIRE(countFilesInDir() == 3);
    }

    BOOST_REQUIRE(countFilesInDir() == 0);

    map::deleteFilesFromTmpDir();
}

test_suite* map::initMapDestroySuite()
{
    test_suite * suite = BOOST_TEST_SUITE("Map destroy test suite");

    suite->add(
        BOOST_TEST_CASE(&map::createAndCloseMapTest));

    suite->add(
        BOOST_TEST_CASE(&map::destroyDynamicMapTest));

    return suite;
}
