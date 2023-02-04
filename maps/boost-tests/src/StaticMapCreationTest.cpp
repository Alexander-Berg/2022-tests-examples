#include "tests/boost-tests/include/tools/map_tools.h"
#include "../include/contexts.hpp"
#include <maps/renderer/libs/base/include/hash.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/layers_filter.h>
#include <yandex/maps/renderer5/core/StylesLibrary.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer5/core/locale_tools.h>
#include <maps/renderer/libs/base/include/mms.h>
#include <maps/renderer/libs/base/include/string_convert.h>

#include <boost/test/unit_test.hpp>
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
    const char* staticMapXmlFileName = "tmp/staticMap.xml";
    const char* staticMapPath = "tmp/staticMap.xml.data";
    const char* originalFontPath = "/usr/share/yandex/maps/renderer5/fonts/LiberationSans-Regular.ttf";
    const char* staticMapFontsPath = "tmp/staticMap.xml.data/fonts";
    const char* staticMapSdfFontsPath = "tmp/staticMap.xml.data/sdf_fonts";

    const char* smallFileBaseMap = "tests/boost-tests/maps/SmallFileBasedMap.xml";
    const char* smallFileBaseMap1Lang = "tests/boost-tests/maps/SmallFileBasedMap1Lang.xml";
    const char* smallFileBaseMap2Lang = "tests/boost-tests/maps/SmallFileBasedMap2Lang.xml";

    const std::vector<std::string> mapRegional0Lang = {};
    const std::vector<std::string> mapRegional1Lang = {"en_US"};
    const std::vector<std::string> mapRegional2Lang = {"en_US", "ru_RU"};

    template <core::ITypedLayer::Type T>
    class TypedLayersFilter:
        public core::SimpleLayersFilterBase
    {
    public:
        virtual bool operator()(const core::ILayer& layer) const
        {
            auto ptr = layer.cast<core::ITypedLayer>();
            return ptr && ptr->type() == T;
        }
    };

    typedef TypedLayersFilter<core::ITypedLayer::OptimizedFileGeometryLayer>
            OptimizedFileGeometryLayersFilter;
    typedef TypedLayersFilter<core::ITypedLayer::OptimizedFileTextLayer>
            OptimizedFileTextLayersFilter;

    bool areIdsEqual(const std::vector<core::ILayer*> layers, const int* const ids) {
        for (size_t i = 0; i < layers.size(); ++i) {
            if (layers[i]->id() != ids[i]) {
                std::stringstream msg;
                msg << "item[" << i << "]: " << layers[i]->id() << " != " << ids[i];
                BOOST_TEST_MESSAGE(msg.str());
                return false;
            }
        }
        return true;
    }

    bool areNamesEqual(const std::vector<core::ILayer*> layers, const wchar_t** names) {
        for (size_t i = 0; i < layers.size(); ++i) {
            if (layers[i]->name() != names[i]) {
                std::stringstream msg;
                msg << "item[" << i << "]: "
                    << base::ws2s(layers[i]->name()) << " != "
                    << base::ws2s(names[i]);
                BOOST_TEST_MESSAGE(msg.str());
                return false;
            }
        }
        return true;
    }
}

void compileMapWithOptions(
    core::IMapGuiPtr mapGui,
    const std::string& staticMap,
    const mapcompiler::Options& options)
{
    mapcompiler::compile(
        mapGui->map(),
        staticMap,
        options,
        mapGui->map().zoomIndexes(),
        mapGui->map().locales(),
        test::map::createProgressStub());
}

size_t countLayers(core::IMapGuiPtr mapGui, const core::ILayersFilter& filter)
{
    auto layers = mapGui->map().rootGroupLayer()->getChildrenPtrRecursive(filter);
    return layers.size();
}

BOOST_AUTO_TEST_SUITE( static_map_creation )

BOOST_FIXTURE_TEST_CASE( compileSmallMapTest, CleanContext<> )
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    io::file::write(io::file::open(origMapXmlFileName), mapXmlFileName);

    mapGui->loadFromXml(mapXmlFileName, true);

    {
        // test MAPSCORE-2719
        fs::path absMapXmlFileName
            = (fs::initial_path<fs::path>() / fs::path(mapXmlFileName))
                  .normalize();

        BOOST_CHECK(core::strings::strieq(absMapXmlFileName.string(), mapGui->xmlFileName()));
    }

    BOOST_REQUIRE_NO_THROW(mapGui->open(map::createProgressStub()));

    {
        core::OperationProgressPtr operationProgress(map::createProgressStub());

        BOOST_REQUIRE_NO_THROW(
            mapcompiler::compile(
                mapGui->map(),
                staticMapXmlFileName,
                mapcompiler::Options(),
                mapGui->map().zoomIndexes(),
                mapGui->map().locales(),
                operationProgress));

        core::Map staticMap(core::MapMode::Static);

        staticMap.env().stylesLibrary.reset(
            new core::StylesLibrary(""));

        BOOST_REQUIRE_NO_THROW(
            staticMap.loadFromXml(staticMapXmlFileName, false));

        BOOST_REQUIRE_NO_THROW(
            staticMap.open(operationProgress));

        BOOST_CHECK(io::exists(staticMapPath));

        // test MAPSCORE-2927
        BOOST_CHECK(io::exists(staticMapXmlFileName));

        {
            // test MAPSCORE-2719
            fs::path absMapXmlFileName
                = (fs::initial_path<fs::path>() / fs::path(staticMapXmlFileName))
                    .normalize();

            BOOST_CHECK(core::strings::strieq(absMapXmlFileName.string(), staticMap.xmlFileName()));
        }

        staticMap.destroy();

        BOOST_CHECK(!io::exists(staticMapPath));
    }

    // test MAPSCORE-2725: delete map folder with map.xml and tmp files
    //
    BOOST_CHECK(!io::dir::empty(io::tempDirPath()));

    BOOST_CHECK(io::exists(mapXmlFileName));

    mapGui->destroy();

    BOOST_CHECK(io::dir::empty(io::tempDirPath()));
}

BOOST_FIXTURE_TEST_CASE(compileWithNoGeometryOptionTest, CleanContext<>)
{
    core::IMapGuiPtr dynamicMap = map::openMap(smallFileBaseMap);

    mapcompiler::Options options;
    options.excludeGeometryLayers = true;

    BOOST_REQUIRE_NO_THROW(
        compileMapWithOptions(
            dynamicMap, staticMapXmlFileName, options));

    // checks static map
    //
    core::IMapGuiPtr staticMap = test::map::openMap(staticMapXmlFileName);

    BOOST_CHECK(
        countLayers(staticMap, OptimizedFileGeometryLayersFilter()) == 0);
    BOOST_CHECK(
        countLayers(staticMap, OptimizedFileTextLayersFilter()) != 0);
}

BOOST_AUTO_TEST_CASE(compileTextWithLanguages)
{
    mapcompiler::Options options;
    options.excludeGeometryLayers = true;

    auto check = [&](const char* map, const std::vector<std::string>& locales, size_t layers) {
        map::deleteFilesFromTmpDir();
        core::IMapGuiPtr dynamicMap = map::openMap(map);
        dynamicMap->setLocales(locales);
        BOOST_REQUIRE_NO_THROW(compileMapWithOptions(dynamicMap, staticMapXmlFileName, options));
        core::IMapGuiPtr staticMap = test::map::openMap(staticMapXmlFileName);
        BOOST_CHECK_EQUAL(countLayers(staticMap, OptimizedFileTextLayersFilter()), layers);
    };
    check(smallFileBaseMap, mapRegional0Lang, 10);
    check(smallFileBaseMap1Lang, mapRegional1Lang, 6);
    check(smallFileBaseMap2Lang, mapRegional2Lang, 12);
}

void compileSelectedLanguagesWithOptions(
    core::IMapGuiPtr mapGui,
    const std::string& staticMap,
    const std::vector<std::string>& locales,
    const mapcompiler::Options& options)
{
    mapcompiler::compile(
        mapGui->map(),
        staticMap,
        options,
        mapGui->map().zoomIndexes(),
        !locales.empty() ? locales : mapGui->map().locales(),
        test::map::createProgressStub());
}

BOOST_AUTO_TEST_CASE(compileTextWithSelectedLanguages)
{
    mapcompiler::Options options;
    options.excludeGeometryLayers = true;

    auto check = [&](const char* map, size_t layersNumber, std::vector<std::string> requestedLocales) {
        map::deleteFilesFromTmpDir();
        core::IMapGuiPtr dynamicMap = map::openMap(map);

        std::stringstream msg;
        BOOST_REQUIRE_NO_THROW(compileSelectedLanguagesWithOptions(dynamicMap, staticMapXmlFileName, requestedLocales, options));
        core::IMapGuiPtr staticMap = test::map::openMap(staticMapXmlFileName);
        BOOST_CHECK_EQUAL(countLayers(staticMap, OptimizedFileTextLayersFilter()), layersNumber);
    };

    auto splittedLocalesCount = core::splitByLang(core::getSupportedLocales()).size();
    size_t zoomCount = 10;

    check(smallFileBaseMap, zoomCount * 1, {"ru_RU"});
    check(smallFileBaseMap, zoomCount * 1, {});

    size_t zoomCount2 = 6;
    check(smallFileBaseMap1Lang, zoomCount2 * 1,  {"ru_RU"});
    check(smallFileBaseMap1Lang, zoomCount2 * 1,  {"en_US"});
    check(smallFileBaseMap1Lang, zoomCount2 * splittedLocalesCount, {});

    check(smallFileBaseMap2Lang, zoomCount2 * 1,  {"ru_RU"});
    check(smallFileBaseMap2Lang, zoomCount2 * 2,  {"ru_RU", "en_US"});
    check(smallFileBaseMap2Lang, zoomCount2 * 2,  {"ru_RU", "en_US", "en_UA"});
    check(smallFileBaseMap2Lang, zoomCount2 * 3,  {"ru_RU", "en_US", "tr_TR"});
}

BOOST_FIXTURE_TEST_CASE(compileWithNoTextOptionTest, CleanContext<>)
{
    core::IMapGuiPtr dynamicMap = map::openMap(smallFileBaseMap);

    mapcompiler::Options options;
    options.excludeTextLayers = true;

    BOOST_REQUIRE_NO_THROW(
        compileMapWithOptions(
            dynamicMap, staticMapXmlFileName, options));

    // checks static map
    //
    core::IMapGuiPtr staticMap = test::map::openMap(staticMapXmlFileName);

    BOOST_CHECK(
        countLayers(staticMap, OptimizedFileGeometryLayersFilter()) != 0);
    BOOST_CHECK(
        countLayers(staticMap, OptimizedFileTextLayersFilter()) == 0);
}

BOOST_FIXTURE_TEST_CASE(compileWithNoTextNoGeometryOptionsTest, CleanContext<>)
{
    core::IMapGuiPtr dynamicMap = map::openMap(smallFileBaseMap);

    mapcompiler::Options options;
    options.excludeGeometryLayers = true;
    options.excludeTextLayers = true;

    BOOST_REQUIRE_NO_THROW(
        compileMapWithOptions(
            dynamicMap, staticMapXmlFileName, options));

    // checks static map
    //
    core::IMapGuiPtr staticMap = test::map::openMap(staticMapXmlFileName);

    BOOST_CHECK(
        countLayers(staticMap, OptimizedFileGeometryLayersFilter()) == 0);
    BOOST_CHECK(
        countLayers(staticMap, OptimizedFileTextLayersFilter()) == 0);
}

BOOST_FIXTURE_TEST_CASE(openStaticMapInRoMode, CleanContext<>)
{
    core::IMapGuiPtr dynamicMap = map::openMap(smallFileBaseMap);

    mapcompiler::Options options;
    options.useFilePacking = true;

    BOOST_REQUIRE_NO_THROW(
        compileMapWithOptions(
        dynamicMap, staticMapXmlFileName, options));

    dynamicMap.reset();

    std::string mapFile = staticMapXmlFileName;
    std::string tarFile = mapFile + ".data.tar";

    BOOST_REQUIRE(io::exists(mapFile));
    BOOST_REQUIRE(io::exists(tarFile));

    core::IMapGuiPtr staticMap;

    chmodFile(mapFile, ReadOnly);
    chmodFile(tarFile, ReadOnly);

    BOOST_REQUIRE_NO_THROW(staticMap =
        test::map::openMap(staticMapXmlFileName));

    BOOST_CHECK(staticMap);

    if (staticMap)
        staticMap->close();

    staticMap.reset();
}

BOOST_AUTO_TEST_SUITE_END()
