#include "tests/boost-tests/include/tools/map_tools.h"
#include "../include/TestConfirmationProvider.h"
#include "../include/contexts.hpp"

#include "core/ISearchableLayer.h"
#include <yandex/maps/renderer5/core/FeatureCapabilities.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/StylesLibrary.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>

#include <boost/filesystem.hpp>

#include <sys/stat.h>

using namespace maps::renderer;
using namespace maps::renderer5;
namespace fs = boost::filesystem;

namespace
{
    const char* mapXmlFileName = "tests/boost-tests/maps/MapLayersAccessTest.xml";

    const char* mapXmlFileNameDefault = "tmp/map.xml";
    const char* mapXmlFileName1 = mapXmlFileNameDefault;
    const char* mapXmlFileName2 = "tmp/map2.xml";

    const char* sourceFileName = "tests/boost-tests/data/TwoPlineTest.mif";

    typedef std::set<std::string> FileNamesType;

    FileNamesType getFileNamesFromTmpDir()
    {
        FileNamesType fileNames;

        fs::directory_iterator dIt(io::tempDirPath());
        fs::directory_iterator dItEnd;

        for (; dIt != dItEnd; ++dIt)
        {
            std::string fileName = dIt->path().string();

            fileNames.insert(
                fileNames.end(),
                fileName);
        }

        return fileNames;
    }

    FileNamesType getCreatedFileNamesFromDir(
        const FileNamesType& oldFileNames)
    {
        FileNamesType fileNames;

        fs::directory_iterator dIt(io::tempDirPath());
        fs::directory_iterator dItEnd;

        for (; dIt != dItEnd; ++dIt)
        {
            std::string fileName = dIt->path().string();

            if (oldFileNames.find(fileName) == oldFileNames.end())
            {
                fileNames.insert(
                    fileNames.end(),
                    fileName);
            }
        }

        return fileNames;
    }

    bool checkAllFilesExists(
        FileNamesType& fileNames)
    {
        for (auto& fileName : fileNames)
            if (!fs::exists(fileName))
                return false;

        return true;
    }

    bool checkAllFilesNotExists(
        FileNamesType& fileNames)
    {
        for (auto& fileName : fileNames)
            if (fs::exists(fileName))
                return false;

        return true;
    }

    core::IMapGuiPtr createNewMap()
    {
        core::IMapGuiPtr mapGui = test::map::createTestMapGui();

        mapGui->setExternalConfirmationsProvider(
            test::map::TestConfirmationProviderPtr(
                new test::map::TestConfirmationProvider()));

        mapGui->open(test::map::createProgressStub());

        auto layers = mapGui->addLayerFromSource(
            test::map::createProgressStub(),
            io::path::absolute(sourceFileName),
            0);

        mapGui->annotateGeometryLayer(0, 0, layers.front()->id());

        return mapGui;
    }

    core::IMapGuiPtr getLoadedMap(
        const std::string& xmlFileName)
    {
        core::IMapGuiPtr mapGui = test::map::createTestMapGui();

        mapGui->setExternalConfirmationsProvider(
            test::map::TestConfirmationProviderPtr(
                new test::map::TestConfirmationProvider()));

        {
            core::IMapGuiPtr newMapGui = createNewMap();

            newMapGui->saveAs(test::map::createProgressStub(), xmlFileName);
        }

        mapGui->loadFromXml(xmlFileName, false);

        mapGui->open(test::map::createProgressStub());

        return mapGui;
    }
}

BOOST_AUTO_TEST_SUITE( map )
BOOST_AUTO_TEST_SUITE( load_save_operations )

BOOST_FIXTURE_TEST_CASE( loadedStateTest, CleanContext<> )
{
    core::IMapGuiPtr mapGui = test::map::createTestMapGui();

    BOOST_CHECK(mapGui->map().isLoaded() == false);

    mapGui->loadFromXml(mapXmlFileName, false);

    // test MAPSCORE-2719
    fs::path absMapXmlFileName
        = (fs::initial_path<fs::path>() / fs::path(mapXmlFileName))
            .normalize();

    BOOST_CHECK(core::strings::strieq(absMapXmlFileName.string(), mapGui->xmlFileName()));

    BOOST_CHECK(mapGui->map().isLoaded() == true);

    mapGui->close();
}

// MAPSCORE-2815
BOOST_FIXTURE_TEST_CASE( saveNewMapTest, CleanContext<> )
{
    FileNamesType fileNamesBeginState = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesBeginState.size() == 0);

    FileNamesType fileNamesAfterCreated;
    FileNamesType fileNamesAfterSaved;

    {
        core::IMapGuiPtr mapGui = createNewMap();
        fileNamesAfterCreated = getCreatedFileNamesFromDir(fileNamesBeginState);
        BOOST_CHECK(fileNamesAfterCreated.size() == 3); // containers files

        BOOST_REQUIRE_NO_THROW(mapGui->save(test::map::createProgressStub()));
        fileNamesAfterSaved = getCreatedFileNamesFromDir(fileNamesBeginState);
        BOOST_CHECK(fileNamesAfterSaved.size() == 4); // containers files + mapGui->xml
    }

#ifdef _WIN32
    BOOST_CHECK(checkAllFilesExists(fileNamesAfterSaved) == true);
    BOOST_CHECK(checkAllFilesExists(fileNamesAfterCreated) == true);
#else
    FileNamesType files = getCreatedFileNamesFromDir(fileNamesBeginState);
    BOOST_CHECK(files.size() == 1);
#endif
    BOOST_CHECK(fs::exists(mapXmlFileNameDefault));
}

// MAPSCORE-2815
BOOST_FIXTURE_TEST_CASE( saveNewMapAsTest, CleanContext<> )
{
    FileNamesType fileNamesBeginState = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesBeginState.size() == 0);

    FileNamesType fileNamesAfterCreated;
    FileNamesType fileNamesAfterSaved;

    {
        core::IMapGuiPtr map = createNewMap();
        fileNamesAfterCreated = getCreatedFileNamesFromDir(fileNamesBeginState);
        BOOST_CHECK(fileNamesAfterCreated.size() == 3); // containers files

        BOOST_REQUIRE_NO_THROW(map->saveAs(test::map::createProgressStub(), mapXmlFileName2));
        fileNamesAfterSaved = getCreatedFileNamesFromDir(fileNamesBeginState);
        BOOST_CHECK(fileNamesAfterSaved.size() == 4); // containers files + mapGui->xml
    }

#ifdef _WIN32
    BOOST_CHECK(checkAllFilesExists(fileNamesAfterSaved) == true);
    BOOST_CHECK(checkAllFilesExists(fileNamesAfterCreated) == true);
#else
    FileNamesType files = getCreatedFileNamesFromDir(fileNamesBeginState);
    BOOST_CHECK(files.size() == 1);
#endif
    BOOST_CHECK(fs::exists(mapXmlFileName2));
}

// MAPSCORE-2815
BOOST_FIXTURE_TEST_CASE( saveNewMapWithOverwritingTest, CleanContext<> )
{
    FileNamesType fileNamesBeginState = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesBeginState.size() == 0);

    FileNamesType fileNamesOfExistsMap;

    {
        core::IMapGuiPtr map = createNewMap();

        BOOST_REQUIRE_NO_THROW(map->saveAs(test::map::createProgressStub(), mapXmlFileName2));
        fileNamesOfExistsMap = getFileNamesFromTmpDir();

        BOOST_REQUIRE(fileNamesOfExistsMap.size() == 4); // containers files + map2.xml
        BOOST_REQUIRE(checkAllFilesExists(fileNamesOfExistsMap));
        BOOST_CHECK(fs::exists(mapXmlFileName2) == true);

        std::string fullXmlFileName = io::path::absolute(mapXmlFileName2);

        BOOST_REQUIRE(fileNamesOfExistsMap.find(fullXmlFileName) != fileNamesOfExistsMap.end());
        fileNamesOfExistsMap.erase(fileNamesOfExistsMap.find(fullXmlFileName));
    }

    FileNamesType fileNamesAfterSaved;
    {
        core::IMapGuiPtr map = createNewMap();

        BOOST_REQUIRE_NO_THROW(map->saveAs(test::map::createProgressStub(), mapXmlFileName2));
        fileNamesAfterSaved = getFileNamesFromTmpDir();

        BOOST_REQUIRE(fileNamesAfterSaved.size() == 4); // containers files + map2.xml
        BOOST_REQUIRE(checkAllFilesExists(fileNamesAfterSaved));
        BOOST_CHECK(fs::exists(mapXmlFileName2) == true);
    }

#ifdef _WIN32
    BOOST_CHECK(checkAllFilesExists(fileNamesOfExistsMap) == true);
    BOOST_CHECK(checkAllFilesExists(fileNamesAfterSaved) == true);

    FileNamesType checkFileNames = fileNamesOfExistsMap;
    checkFileNames.insert(io::path::absolute(mapXmlFileName2));
    BOOST_CHECK(checkFileNames == fileNamesAfterSaved);

    BOOST_CHECK(fileNamesAfterSaved == getFileNamesFromTmpDir());

#else
    FileNamesType files = getCreatedFileNamesFromDir(fileNamesBeginState);
    FileNamesType checkFileNames;
    checkFileNames.insert(io::path::absolute(mapXmlFileName2));
    BOOST_CHECK(files == checkFileNames);
#endif
}

// MAPSCORE-2815
BOOST_FIXTURE_TEST_CASE( saveLoadedMapTest, CleanContext<> )
{
    FileNamesType fileNamesBeginState = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesBeginState.size() == 0);

    core::IMapGuiPtr map = getLoadedMap(mapXmlFileName2);

    FileNamesType fileNamesOfExistsMap = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesOfExistsMap.size() == 4);

    BOOST_REQUIRE_NO_THROW(map->save(test::map::createProgressStub())); // save into mapXmlFileName = mapXmlFileNameDerault = tmp/mapGui->xml

    FileNamesType fileNamesOfSavedMap = getCreatedFileNamesFromDir(fileNamesOfExistsMap);
    BOOST_REQUIRE(fileNamesOfSavedMap.size() == 0);

    BOOST_CHECK(checkAllFilesExists(fileNamesOfExistsMap) == true);
}

// MAPSCORE-2815
BOOST_FIXTURE_TEST_CASE( saveLoadedMapAsTest, CleanContext<> )
{
    FileNamesType fileNamesBeginState = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesBeginState.size() == 0);

    core::IMapGuiPtr map = getLoadedMap(mapXmlFileName2);

    FileNamesType fileNamesOfExistsMap = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesOfExistsMap.size() == 4);

    BOOST_REQUIRE_NO_THROW(map->saveAs(test::map::createProgressStub(), mapXmlFileName1));

    FileNamesType fileNamesOfSavedMap = getCreatedFileNamesFromDir(fileNamesOfExistsMap);
    BOOST_REQUIRE(fileNamesOfSavedMap.size() == 1);

    std::string fullFileName = io::path::absolute(mapXmlFileName1);
    BOOST_REQUIRE(*fileNamesOfSavedMap.begin() == fullFileName);

    BOOST_CHECK(checkAllFilesExists(fileNamesOfExistsMap) == true);
    BOOST_CHECK(checkAllFilesExists(fileNamesOfSavedMap) == true);
}

// MAPSCORE-2815
// before test: save map into map1.xml
// load map from map2.xml
// save map as map1.xml (overwrite)
BOOST_FIXTURE_TEST_CASE( saveLoadedMapWithOverwritingTest, CleanContext<> )
{
    {
        core::IMapGuiPtr newMap = createNewMap();
        newMap->saveAs(test::map::createProgressStub(), mapXmlFileName1);
    }

#ifdef _WIN32
    const size_t expectedContainerFilesCount = 3;
#else
    const size_t expectedContainerFilesCount = 0;
#endif

    FileNamesType fileNamesBeginState = getFileNamesFromTmpDir();
    BOOST_REQUIRE(fileNamesBeginState.size() == 1 + expectedContainerFilesCount);

    core::IMapGuiPtr map = getLoadedMap(mapXmlFileName2);

    FileNamesType fileNamesOfExistsMap = getCreatedFileNamesFromDir(fileNamesBeginState);
    BOOST_REQUIRE(fileNamesOfExistsMap.size() == 4 - expectedContainerFilesCount);
    {
        std::string fullFileName = io::path::absolute(mapXmlFileName2);
        BOOST_REQUIRE(fileNamesOfExistsMap.find(fullFileName) != fileNamesOfExistsMap.end());
    }

    BOOST_REQUIRE_NO_THROW(map->saveAs(test::map::createProgressStub(), mapXmlFileName1));

    FileNamesType fileNamesOfSavedMap = getCreatedFileNamesFromDir(fileNamesOfExistsMap);
    BOOST_REQUIRE(fileNamesOfSavedMap.size() == 1 + expectedContainerFilesCount);

    BOOST_CHECK(checkAllFilesExists(fileNamesOfExistsMap));

    FileNamesType allFiles;

    allFiles.insert(
        fileNamesOfSavedMap.begin(),
        fileNamesOfSavedMap.end());

    allFiles.insert(
        fileNamesOfExistsMap.begin(),
        fileNamesOfExistsMap.end());

    BOOST_CHECK(allFiles.size() == 5);
    BOOST_CHECK(allFiles == getFileNamesFromTmpDir());
}

#ifdef __linux__
BOOST_FIXTURE_TEST_CASE( loadDynamicMapXMLPermissionDenied, CleanContext<> )
{
    const std::string origMapFileName("tests/boost-tests/maps/StaticMapCreation.xml");
    const std::string mapFileName("tmp/DynamicMap.xml");

    {
        BOOST_REQUIRE_NO_THROW(io::file::write(io::file::open(origMapFileName), mapFileName));
        BOOST_REQUIRE(chmod(mapFileName.c_str(), 0000) == 0);

        core::IMapGuiPtr mapGui;
        BOOST_REQUIRE_THROW(mapGui = test::map::loadMap(mapFileName), std::exception);
    }
}
#endif

BOOST_FIXTURE_TEST_CASE( loadStaticMapPermissionDenied, CleanContext<> )
{
    const std::string origMapFileName("tests/boost-tests/maps/StaticMapCreation.xml");
    const std::string dynamicMapFileName("tmp/DynamicMap.xml");
    const std::string staticMapFileName("tmp/StaticMap.xml");
    const std::string staticMapDataFileName("tmp/StaticMap.xml.data.tar");

    {
        BOOST_REQUIRE_NO_THROW(io::file::write(io::file::open(origMapFileName), dynamicMapFileName));

        core::IMapGuiPtr mapGui;
        BOOST_REQUIRE_NO_THROW(mapGui = test::map::openMap(dynamicMapFileName));

        mapcompiler::Options options;
        options.useFilePacking = true;
        BOOST_REQUIRE_NO_THROW(
            mapcompiler::compile(
                mapGui->map(),
                staticMapFileName,
                options,
                mapGui->map().zoomIndexes(),
                mapGui->map().locales(),
                test::map::createProgressStub()));

        BOOST_REQUIRE(io::exists(staticMapFileName));
        BOOST_REQUIRE(io::exists(staticMapDataFileName));
    }

#ifdef __linux__
    {
        BOOST_REQUIRE(chmod(staticMapFileName.c_str(), 0000) == 0);

        core::Map staticMap(core::MapMode::Static);
        staticMap.env().stylesLibrary.reset(new core::StylesLibrary(""));

        BOOST_CHECK_THROW(staticMap.loadFromXml(staticMapFileName, false), std::exception);
        BOOST_REQUIRE(chmod(staticMapFileName.c_str(), 0444) == 0);
    }
#endif
}

BOOST_AUTO_TEST_SUITE_END() // load_save_operations
BOOST_AUTO_TEST_SUITE_END() // map
