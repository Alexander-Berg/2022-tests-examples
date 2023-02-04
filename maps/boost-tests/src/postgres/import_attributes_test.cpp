#include "tools/map_tools.h"
#include "contexts.hpp"

#include "hotspots/attributes_storage_mms.h"
#include "hotspots/consts.h"
#include "hotspots/HotspotsTools.h"

#include "core/IRasterizableLayer.h"
#include <yandex/maps/renderer5/postgres/DefaultPostgresTransactionProvider.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer5/core/Map.h>

#include <yandex/maps/renderer/proj/tile.h>

using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::core;

BOOST_AUTO_TEST_CASE(wiki_map_metro_compilation)
{
    const std::string dynamicMap = "tests/boost-tests/maps/wmap/wiki_map_metro.dynamic.xml";
    const std::string staticMap = "tmp/wiki_metro.static.xml";
    const std::string dboptions =
        " host=     pg94.maps.dev.yandex.net"
        " user=     renderer"
        " password= renderer"
        " port=     5432"
        " dbname=   renderer_test";

    {
        auto mapGui = test::map::openMap(dynamicMap);
        auto& map = mapGui->map();

        postgres::PostgresTransactionProviderPtr postgres(
            new postgres::DefaultPostgresTransactionProvider(dboptions));

        map.setPostgresTransactionProvider(postgres);

        mapcompiler::Options options;
        options.importAttributes = true;
        options.attributesStorage = mapcompiler::Options::MMS;

        std::set<unsigned int> zoomIndices;
        zoomIndices.insert(14);
        zoomIndices.insert(15);
        zoomIndices.insert(16);

        BOOST_CHECK_NO_THROW(mapcompiler::compile(
            map,
            staticMap,
            options,
            zoomIndices,
            map.locales(),
            test::map::createProgressStub()));
    }

    auto mapGui = test::map::openMap(staticMap, true, core::MapOpenFlags::DISABLE_VIRTUAL_ICON_LAYERS);
    auto& map = mapGui->map();

    const std::string mmsFileName =
        staticMap + ".data/" +
        hotspots::STORAGE_DIR + "/" +
        hotspots::MMS_STORAGE_NAME;

    hotspots::AttributesStorageMMS storage(mmsFileName);

    auto layers = map.getRasterizableLayers(nullptr);
    BOOST_CHECK_EQUAL(layers.size(), 3);

    {
        hotspots::FeatureKeyGenerator keyGen(hotspots::layerKey(*layers[0]));
        BOOST_CHECK_NO_THROW(storage.get(keyGen.generate(116636)));
        BOOST_CHECK_NO_THROW(storage.get(keyGen.generate(157775)));
        BOOST_CHECK_NO_THROW(storage.get(keyGen.generate(1716266)));
    }

    for (auto& layer : layers) {
        hotspots::FeatureKeyGenerator keyGen(hotspots::layerKey(*layer));
        auto cap = layer->get<core::IRasterizableLayer>();
        auto features = cap->findFeatures(
            proj::EARTH_BOX,
            cap->featureCapabilities(),
            layer->renderStyle()->visibilityScaling().min());
        features->reset();
        while (features->hasNext()) {
            BOOST_CHECK_NO_THROW(storage.get(keyGen.generate(features->next())));
        }
    }
}
