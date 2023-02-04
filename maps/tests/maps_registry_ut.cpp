#include <maps/jams/renderer2/realtime/yacare/lib/registry.h>
#include <maps/jams/renderer2/realtime/yacare/lib/util.h>

#include <maps/jams/renderer2/common/yacare/lib/config.h>
#include <maps/jams/renderer2/common/yacare/lib/timestamps.h>

#include <maps/libs/mms/include/yandex/maps/mms/writer.h>

#include <maps/renderer/libs/data_sets/data_set/include/data_set.h>
#include <maps/renderer/libs/data_sets/data_set/include/view_queriable.h>
#include <yandex/maps/renderer/depot/gms/gms.h>
#include <yandex/maps/renderer/depot/rtree/rtree.h>
#include <yandex/maps/renderer/feature/feature.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/filesystem.hpp>

#include <fstream>
#include <thread>

namespace fs = boost::filesystem;

namespace maps::jams::renderer::realtime {

namespace {

const std::string TEST_DESIGN_PATH = SRC_("data/design");
const std::string TEST_DESIGN_PATH_V2 = SRC_("data/design_v2");

Config createConfig(
    const std::string& basePath,
    const std::string& designPath = TEST_DESIGN_PATH,
    bool useStrictVersion = false)
{
    return { basePath, designPath, useStrictVersion };
}

void createTestMapData(
    const fs::path& path,
    std::optional<time_t> checkVersion = std::nullopt)
{
    using namespace maps::renderer;
    time_t version = 0;
    try {
        version = realtimeTimeStringToVersion(path.filename().string());
    } catch (const std::exception&) {
        // ignore invalid path in test
    }
    fs::create_directories(path);
    if (checkVersion && version != 0) {
        UNIT_ASSERT_EQUAL(version, *checkVersion);
        // create legacy map to switch version
        std::ofstream((path / (std::to_string(version) + "_legacy.map")).string());
    }

    MapDataPaths paths(path.string());
    for (size_t i : common::CATS) {
        fs::create_directory(paths.jamsGeometry[i]);
        std::string geometryPath = (fs::path(paths.jamsGeometry[i]) / "geometry").string();
        std::string indexPath = (fs::path(paths.jamsGeometry[i]) /"index").string();
        depot::StorageOptions options;
        options.type = feature::FeatureType::Polyline;
        auto writer = depot::gms::createWriter(geometryPath, options);
        auto index = depot::rtree::createBuilder(options.maxZoom);
        index->build(indexPath, writer.get());

        common::standalone::Jams jams;
        std::ofstream os(paths.jamsData[i]);
        mms::write(os, jams);
    }
    for (size_t i : common::CATS) {
        fs::create_directory(paths.eventsGeometry[i]);
        std::string geometryPath = (fs::path(paths.eventsGeometry[i]) / "geometry").string();
        std::string indexPath = (fs::path(paths.eventsGeometry[i]) / "index").string();
        depot::StorageOptions options;
        options.type = feature::FeatureType::Point;
        auto writer = depot::gms::createWriter(geometryPath, options);
        auto index = depot::rtree::createBuilder(options.maxZoom);
        if (i == 1) {
            feature::Feature ft(feature::FeatureType::Point);
            ft.setSourceId(0);
            ft.geom().shapes().addGeomPoint({10, 10});
            REQUIRE(depot::tryAdd(index.get(), writer.get(), ft),
                    "Can't add test feature " << ft.sourceId());
        }
        index->build(indexPath, writer.get());

        standalone::Events events;
        if (i == 1) {
            events.emplace_back();
            events[0].description = std::to_string(version);
            events[0].category = 1;
            events[0].startTime = 0;
            events[0].tzOffset = 0;
            events[0].moderated = true;
        }
        std::ofstream os(paths.eventsData[i]);
        mms::write(os, events);
    }
}

std::string testMapDataVersion(const Map& map)
{
    // version was saved as description of event
    using namespace maps::renderer;
    auto dataIt = map.data.find(TRF_DATA_NAME);
    ASSERT(dataIt != map.data.end());
    auto data = dataIt->second;
    data_set::ViewQueryParams params({0, 0, 100, 100}, {0, 23});
    params.auxData = MapDataQueryParams();
    data_set::ViewQueryContext ctx(&params);
    params.layers.emplace();
    params.layers->insert(EVENTS_SOURCE_LAYER);
    auto eventsView = data->asViewQueriable().queryView(ctx);
    ASSERT(eventsView.size() == 1);
    auto it = eventsView[0].iterator();
    ASSERT(it->hasNext());
    auto& ft = it->next();
    ASSERT(ft.sourceId() == 0);
    auto attrs = ft.attr.get();
    ASSERT(attrs->IsObject());
    ASSERT(attrs->HasMember("description"));
    return (*attrs)["description"].GetString();
}

std::string empty()
{
    auto path = fs::current_path() / "empty";

    fs::create_directory(path);
    return path.string();
}

std::string emptyVersionFile()
{
    auto path = fs::current_path() / "empty_version_file";
    fs::create_directory(path);
    std::ofstream((path / "version").string());

    return path.string();
}

std::string goodData()
{
    auto path = fs::current_path() / "good_data";
    createTestMapData(path / "2020.01.01.12.00.00");
    createTestMapData(path / "2020.01.01.12.01.00");
    std::ofstream((path / "version").string()) << "1577880060";
    return path.string();
}

std::string dataForStrictVersion()
{
    auto path = fs::current_path() / "good_data2";
    createTestMapData(path / "2020.01.01.12.00.00");
    createTestMapData(path / "2020.01.01.12.01.00");
    createTestMapData(path / "2020.01.01.12.02.00");
    std::ofstream((path / "version").string()) << "1577880060";
    return path.string();
}

std::string twoMaps(
    const std::string& path1,
    const std::string& path2)
{
    static int count = 0;
    auto path = fs::current_path() / ("two_maps_" + std::to_string(++count));

    createTestMapData(path / path1);
    createTestMapData(path / path2);

    return path.string();
}

std::string manyMaps(size_t count)
{
    auto path = fs::current_path() / "many_maps";

    for (size_t i = 0; i < count; ++i) {
        auto s = std::to_string(i / 10) + std::to_string(i % 10);
        auto directory = path / ("2020.01.01.12.00." + s);
        std::string version = std::to_string(1577880000 + i);
        createTestMapData(directory);
    }

    return path.string();
}

Map getMap(const RealtimeRegistry& registry, std::optional<time_t> version)
{
    return registry.map(version, DesignType::Default, std::nullopt);
}

} // namespace

Y_UNIT_TEST_SUITE(MapRegistryTests)
{

Y_UNIT_TEST(EmptyMapDir)
{
    RealtimeRegistry registry(createConfig(empty()));
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), true);
    UNIT_ASSERT_EQUAL(registry.anyVersionActivated(), false);
    UNIT_ASSERT_EQUAL(registry.activeVersion(), 0);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880000), std::runtime_error);
}

Y_UNIT_TEST(NoDesignDir)
{
    RealtimeRegistry registry(createConfig(empty(), "no-design"));
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), false);
    UNIT_ASSERT_EQUAL(registry.anyVersionActivated(), false);
    UNIT_ASSERT_EQUAL(registry.activeVersion(), 0);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880000), std::runtime_error);
}

Y_UNIT_TEST(EmptyVersionFile)
{
    RealtimeRegistry registry(createConfig(emptyVersionFile()));
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), true);
    UNIT_ASSERT_EQUAL(registry.anyVersionActivated(), false);
    UNIT_ASSERT_EQUAL(registry.activeVersion(), 0);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880000), std::runtime_error);
}

Y_UNIT_TEST(OpenSwitchCloseBadPath)
{
    RealtimeRegistry registry(createConfig(empty()));
    UNIT_ASSERT_EXCEPTION(registry.open("very_bad_path"), maps::Exception);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880000), std::runtime_error);
    UNIT_ASSERT_EXCEPTION(registry.switchVersion(1577880000), maps::Exception);
    UNIT_ASSERT_NO_EXCEPTION(registry.close(1577880000));
}

Y_UNIT_TEST(GoodScenario)
{
    RealtimeRegistry registry(createConfig(goodData()));
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), true);
    UNIT_ASSERT_EQUAL(registry.anyVersionActivated(), true);

    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880000));
    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880060));

    UNIT_ASSERT_EQUAL(registry.activeVersion(), 1577880060);

    UNIT_ASSERT_NO_EXCEPTION(registry.close(1577880000));
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880000), SuitableMapNotFoundException);
    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880060));

    // Couldn't close active map version
    UNIT_ASSERT_EXCEPTION(registry.close(1577880060), maps::RuntimeError);

    UNIT_ASSERT(!registry.imageStorage()->getImage("missing.svg"));
    UNIT_ASSERT(registry.imageStorage()->getImage(
        TEST_DESIGN_PATH + "/stable/icons.tar/green_11.svg"));
}

Y_UNIT_TEST(MapsWithoutDesign)
{
    RealtimeRegistry registry(createConfig(goodData(), "no-design"));
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), false);
    UNIT_ASSERT_EQUAL(registry.anyVersionActivated(), true);

    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880000), yacare::errors::NotFound);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880060), yacare::errors::NotFound);
}

Y_UNIT_TEST(ReturnedVersionsAreCorrect)
{
    RealtimeRegistry registry(createConfig(twoMaps(
        "2020.01.01.12.00.10",
        "2020.01.01.12.00.20")));
    UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, 1577880010)), "1577880010");
    UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, 1577880020)), "1577880020");

    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880005), SuitableMapNotFoundException);

    UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, 1577880015)), "1577880010");
    UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, 1577880025)), "1577880020");

    // Opened but not active map is not returned
    UNIT_ASSERT_NO_EXCEPTION(registry.switchVersion(1577880010));
    UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, 1577880020)), "1577880010");
}

Y_UNIT_TEST(BadMapDataPathIsSkipped)
{
    RealtimeRegistry registry(createConfig(twoMaps(
        "bad_data_path",
        "2020.01.01.12.00.20")));

    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880010), SuitableMapNotFoundException);

    UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, 1577880020)), "1577880020");
}

Y_UNIT_TEST(TooManyMapsDoesNotOpened)
{
    size_t mapsToCreate = MAX_MAPS_TO_OPEN_ON_START + 4;

    RealtimeRegistry registry(createConfig(manyMaps(mapsToCreate)));

    std::vector<time_t> opened(MAX_MAPS_TO_OPEN_ON_START);
    std::vector<time_t> notOpened(4);
    std::iota(opened.begin(), opened.end(), 1577880004);
    std::iota(notOpened.begin(), notOpened.end(), 1577880000);

    for (time_t v: opened) {
        UNIT_ASSERT_NO_EXCEPTION(getMap(registry, v));
        UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, v)), std::to_string(v));
    }

    for (time_t v: notOpened) {
        UNIT_ASSERT_EXCEPTION(getMap(registry, v), SuitableMapNotFoundException);
    }
}

Y_UNIT_TEST(StrictVersion)
{
    RealtimeRegistry registry(createConfig(dataForStrictVersion(), TEST_DESIGN_PATH, true));

    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880000));
    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880060));
    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880120));

    UNIT_ASSERT_EQUAL(registry.activeVersion(), 1577880060);

    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577879940), StrictMapNotFoundException);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880030), StrictMapNotFoundException);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880150), StrictMapNotFoundException);
}

Y_UNIT_TEST(EmptyVersion)
{
    RealtimeRegistry registry(createConfig(twoMaps(
        "2020.01.01.12.00.10",
        "2020.01.01.12.00.20")));
    UNIT_ASSERT_NO_EXCEPTION(registry.switchVersion(1577880010));

    UNIT_ASSERT_EQUAL(testMapDataVersion(getMap(registry, std::nullopt)), "1577880010");
}

Y_UNIT_TEST(ReportMetrics)
{
    RealtimeRegistry registry(createConfig(twoMaps(
        "2020.01.01.12.00.10",
        "2020.01.01.12.00.20")));

    UNIT_ASSERT_NO_EXCEPTION(registry.timestamps());
    UNIT_ASSERT_NO_EXCEPTION(registry.openVersionCount());
}

Y_UNIT_TEST(ReloadDesign)
{
    fs::path dataPath = fs::current_path() / "test_reload_design" / "data";
    fs::path designPath = fs::current_path() / "test_reload_design" / "design";
    createTestMapData(dataPath / "2020.01.01.12.00.10", 1577880010);
    RealtimeRegistry registry(createConfig(dataPath.string(), designPath.string()));

    // No design error
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), false);
    UNIT_ASSERT_EXCEPTION(getMap(registry, 1577880010), yacare::errors::NotFound);
    UNIT_ASSERT_EXCEPTION(registry.reloadDesign(), maps::RuntimeError);
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), false);
    UNIT_ASSERT_EXCEPTION(registry.designInfo(), yacare::errors::NotFound);

    // Load design
    fs::create_symlink(TEST_DESIGN_PATH, designPath);
    UNIT_ASSERT_NO_EXCEPTION(registry.reloadDesign());
    UNIT_ASSERT_EQUAL(registry.isDesignLoaded(), true);
    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880010));
    UNIT_ASSERT_EQUAL(registry.designInfo().starts_with("{"), true);

    // Load new data
    createTestMapData(dataPath / "2020.01.01.12.00.20", 1577880020);
    registry.open("2020.01.01.12.00.20");
    registry.switchVersion(1577880020);
    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880020));

    // Reload design and load new data
    fs::remove(designPath);
    fs::create_symlink(TEST_DESIGN_PATH_V2, designPath);
    UNIT_ASSERT_NO_EXCEPTION(registry.reloadDesign());
    createTestMapData(dataPath / "2020.01.01.12.00.30", 1577880030);
    registry.open("2020.01.01.12.00.30");
    registry.switchVersion(1577880030);
    UNIT_ASSERT_NO_EXCEPTION(getMap(registry, 1577880030));

    // Last map version uses last design
    auto map = registry.map(1577880030, DesignType::Default, {});
    UNIT_ASSERT_EQUAL(map.design.basicMode.styleSet->layers.size(), 2);
    UNIT_ASSERT_EQUAL(map.design.revision, "1000");
    map = registry.map(1577880030, DesignType::Navi, {});
    UNIT_ASSERT_EQUAL(map.design.basicMode.styleSet->layers.size(), 1);
    UNIT_ASSERT_EQUAL(map.design.revision, "2200");
    map = registry.map(1577880030, DesignType::Default, "exp-1111");
    UNIT_ASSERT_EQUAL(map.design.revision, "1111");

    // Previous map version uses previous design
    map = registry.map(1577880020, DesignType::Default, {});
    UNIT_ASSERT_EQUAL(map.design.basicMode.styleSet->layers.size(), 2);
    UNIT_ASSERT_EQUAL(map.design.revision, "1000");
    map = registry.map(1577880020, DesignType::Navi, {});
    UNIT_ASSERT_EQUAL(map.design.basicMode.styleSet->layers.size(), 0);
    UNIT_ASSERT_EQUAL(map.design.revision, "2000");
    map = registry.map(1577880020, DesignType::Default, "exp-1111");
    UNIT_ASSERT_EQUAL(map.design.revision, "1000"); // no experiment, stable design is used
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::jams::renderer::realtime
