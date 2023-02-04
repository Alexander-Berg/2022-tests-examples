#include "storage.h"
#include "cache.h"
#include "load_domain_configs.h"
#include "legacy_storage_impl.h"
#include "storage_impl.h"
#include <yandex/maps/i18n.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/system/env.h>
#include <boost/filesystem.hpp>
#include <maps/libs/locale/include/convert.h>

#include <boost/test/unit_test.hpp>

const std::string TEST_DATA_ROOT = BuildRoot() + "/maps/mobile/server/cacheinfo/tests/test_data/storage/";
const std::string TEST_LEGACY_DATA_ROOT = BuildRoot() + "/maps/mobile/server/cacheinfo/tests/test_data/legacy_storage/";

namespace {

const yandex::maps::proto::offline_cache::region_config::Region& findRegion(
        const yandex::maps::proto::offline_cache::region_config::RegionList& list,
        RegionId id)
{
    auto iter = std::find_if(list.regions().cbegin(), list.regions().cend(),
            [id] (const yandex::maps::proto::offline_cache::region_config::Region& region) {
        return region.id() == id;
    });
    BOOST_REQUIRE(iter != list.regions().cend());
    return *iter;
}

bool geodataAvailable()
{
    bool available = boost::filesystem::exists(GEODATA_PATH);
    if (!available)
        BOOST_TEST_WARN("Geobase not available, skip geobase dependent test");
    return available;
}

maps::locale::Locale toLocale(const std::string& name)
{
    return maps::locale::to<maps::locale::Locale>(name);
}

}

BOOST_AUTO_TEST_CASE(test_legacy_storage)
{
    LegacyStorageImpl navi(TEST_LEGACY_DATA_ROOT + "navi.pb");
    LegacyStorageImpl mapkit(TEST_LEGACY_DATA_ROOT + "mapkit.pb");

    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {}).regions_size(), 0);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 2., {}).regions_size(), 0);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 3., {}).regions_size(), 0);

    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 1., {}).regions_size(), 0);
    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 2., {}).regions_size(), 0);
    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 3., {}).regions_size(), 0);

    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta"}).regions_size(), 2);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 2., {"meta"}).regions_size(), 0);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 3., {"meta"}).regions_size(), 0);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 3., {"map"}).regions_size(),  2);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_OS"), 1., {"meta"}).regions_size(), 2);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_OS"), 2., {"meta"}).regions_size(), 0);

    BOOST_REQUIRE_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 1., {"map"}).regions_size(), 1);
    BOOST_REQUIRE_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 2., {"map"}).regions_size(), 2);
    BOOST_REQUIRE_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 3., {"map"}).regions_size(), 2);
    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_OS"), 1., {"map"}).regions_size(), 1);
    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_OS"), 2., {"map"}).regions_size(), 2);

    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 1., {"map"}).regions(0).files_size(), 1);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta", "map"}).regions(0).files_size(), 2);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta", "map"}).regions(0).id(), 1);

    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta"}).regions(0).size().text(), "66 MB");
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"map", "meta"}).regions(1).size().text(), "0.1 kB");
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 2., {"map"}).regions(0).size().text(), "174 MB");

    BOOST_REQUIRE_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 1., {"map", "search"}).regions_size(), 1);
    BOOST_REQUIRE_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 2., {"map", "search"}).regions_size(), 1);
    BOOST_REQUIRE_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 1., {"search"}).regions_size(), 1);
    BOOST_REQUIRE_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 2., {"search"}).regions_size(), 1);
    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 1., {"search"}).regions(0).files_size(), 4);
    BOOST_CHECK_EQUAL(mapkit.selectMaps(toLocale("en_RU"), 2., {"search"}).regions(0).files_size(), 4);
}

BOOST_AUTO_TEST_CASE(test_legacy_storage_locales)
{
    LegacyStorageImpl navi(TEST_LEGACY_DATA_ROOT + "navi.pb");

    // Cache/tile locales with available cache
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta"}).regions_size(), 2);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta"}).regions(0).name(), "Moscow and Moscow region");
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta"}).regions(1).name(), "Petrozavodsk");

    // Cache/tile locales with no cache
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("ru_RU"), 1., {"meta"}).regions_size(), 0);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_RU"), 1., {"meta"}).regions_size(), 2);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("tr_TR"), 1., {"meta"}).regions_size(), 0);

    // Other supported locales
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("fr_XD"), 1., {"meta"}).regions_size(), 2);
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_OS"), 1., {"meta"}).regions_size(), 2);

    // Units localization
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("fr_XD"), 1., {"meta"}).regions(0).size().text(), "66 Mo");
    BOOST_CHECK_EQUAL(navi.selectMaps(toLocale("en_OS"), 1., {"meta"}).regions(0).size().text(), "66 MB");
}

BOOST_AUTO_TEST_CASE(test_storage_select_maps)
{
    if (!geodataAvailable())
        return;

    StorageImpl storage(TEST_DATA_ROOT + "mapkit");

    auto regions = storage.selectMaps(toLocale("en_RU"), 1., {"map", "driving"});
    BOOST_TEST(2, regions.regions_size());
    auto moscow_oblast = findRegion(regions, 1);
    BOOST_TEST(moscow_oblast.name() == "Moscow and Moscow Oblast");
    BOOST_TEST(moscow_oblast.country() == "Russia");
    BOOST_TEST(moscow_oblast.cities_size() == 10);
    BOOST_CHECK_CLOSE(moscow_oblast.center_point().lon(), 37.380031, .000001);
    BOOST_CHECK_CLOSE(moscow_oblast.center_point().lat(), 55.815792, .000001);
    BOOST_TEST(moscow_oblast.size().value() == 1048576 * 2);
    BOOST_TEST(moscow_oblast.size().text() == "2 MB");
    BOOST_TEST(moscow_oblast.files_size() == 2);
    BOOST_TEST(moscow_oblast.release_time() == 3000000);


    auto petersburg = findRegion(regions, 2);
    BOOST_TEST(petersburg.name() == "Санкт-Петербург");
    BOOST_TEST(petersburg.cities_size() == 1);

    regions = storage.selectMaps(toLocale("ru_RU"), 1., {"map", "driving"});
    BOOST_TEST(findRegion(regions, 1).name() == "Москва и Московская область");
}

BOOST_AUTO_TEST_CASE(test_storage_select_maps_with_unknown_locales)
{
    if (!geodataAvailable())
        return;

    StorageImpl storage(TEST_DATA_ROOT + "mapkit");
    auto heRegions = storage.selectMaps(toLocale("he_IL"), 1., {"map", "driving"});
    auto iwRegions = storage.selectMaps(toLocale("iw_IL"), 1., {"map", "driving"});

    BOOST_TEST(heRegions.regions_size() == iwRegions.regions_size());
    BOOST_TEST(findRegion(heRegions, 1).name() == findRegion(iwRegions, 1).name());
    BOOST_TEST(findRegion(heRegions, 1).files_size() == findRegion(iwRegions, 1).files_size());
    BOOST_TEST(findRegion(heRegions, 2).name() == findRegion(iwRegions, 2).name());
    BOOST_TEST(findRegion(heRegions, 2).files_size() == findRegion(iwRegions, 2).files_size());

}

BOOST_AUTO_TEST_CASE(test_storage_select_fonts)
{
    if (!geodataAvailable())
        return;

    StorageImpl storage(TEST_DATA_ROOT + "mapkit");

    auto regions = storage.selectMaps(toLocale("ru_RU"), 1., {"fonts"});
    BOOST_TEST(regions.regions_size() == 1);
}

BOOST_AUTO_TEST_CASE(test_load_domain_configs)
{
    if (!geodataAvailable())
        return;

    auto configs = loadDomainConfigs(TEST_DATA_ROOT, TEST_LEGACY_DATA_ROOT);
    auto mapkitStorage = configs.find("mapkit");
    BOOST_REQUIRE(mapkitStorage != configs.end());
    BOOST_CHECK(dynamic_cast<StorageImpl*>(mapkitStorage->second.get()) != nullptr);

    BOOST_CHECK(configs.find("navi") != configs.end());
    BOOST_CHECK(configs.find("invalid") == configs.end());
}

BOOST_AUTO_TEST_CASE(test_cache)
{
    Cache cache;
    auto ru_RU = maps::Locale{maps::LANG_RU, maps::REGION_RU};
    Cache::Key key = {maps::packLocale(ru_RU), "2.0", "layers"};

    cache.insert(key, "content");
    BOOST_REQUIRE(cache.find(key));
    BOOST_CHECK_EQUAL(*cache.find(key), "content");
}
