#include "tests/boost-tests/include/tools/map_tools.h"
#include "../../include/contexts.hpp"

#include "mapcompiler/consts.h"
#include "labeler/parse_labeling_rules.h"
#include "labeler/convert_label_backward.h"
#include "core/feature.h"
#include "labeler/i_labelable_layer.h"
#include <yandex/maps/renderer5/labeler/labeling_zone.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/StylesLibrary.h>
#include <yandex/maps/renderer5/mapcompiler/options.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer5/styles/styles.h>
#include <yandex/maps/renderer/feature/placed_label.h>
#include <yandex/maps/renderer/proj/mercator.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::labeler;
using namespace maps::renderer;

namespace {
bool isTextLayer(const core::ILayerPtr& layer, styles::Label::Type styleType)
{
    if (!layer)
        return false;

    auto* textLayer = layer->cast<ILabelableLayer>();
    if (!textLayer)
        return false;

    auto ls = textLayer->labelStyle();
    if (!ls)
        return false;

    return ls->type() == styleType;
}

core::ILayerPtr findLayerBySourceId(core::ILayerPtr layer, core::LayerIdType id)
{
    if (layer->isGroup()) {
        for (auto& child: layer->cast<core::IGroupLayer>()->children()) {
            auto r = findLayerBySourceId(child, id);
            if (r)
                return r;
        }
    }

    auto* metadata = layer->metadata();
    if (!metadata)
        return core::ILayerPtr();

    if (!metadata->has(mapcompiler::MetadataKeySourceId) ||
        !metadata->get(mapcompiler::MetadataKeySourceId))
        return core::ILayerPtr();

    return (std::stoi(metadata->get(mapcompiler::MetadataKeySourceId).value()) == id) ? layer : core::ILayerPtr();
}

struct LayerStats
{
    LayerStats() : nFeatures(0), nWithSourcePoint(0)
    {}
    size_t nFeatures;
    size_t nWithSourcePoint;
};

const core::LayerIdType DYNAMIC_POINT_TEXT_LAYER_ID = 8;
const core::LayerIdType DYNAMIC_POLYGON_TEXT_LAYER_ID = 9;
const core::LayerIdType DYNAMIC_POLYLINE_TEXT_LAYER_ID = 7;

class CompiledNavigatorMap: CleanContext<>
{
public:
    CompiledNavigatorMap(): staticMap(core::MapMode::Static)
    {
        const std::string DYNAMIC_MAP_XML = "tests/boost-tests/maps/NavigatorLabels.xml";
        const std::string STATIC_MAP_XML = "tmp/NavigatorLabels.compiled.xml";

        core::IMapGuiPtr dynamicMapGui;
        BOOST_REQUIRE_NO_THROW(dynamicMapGui = test::map::openMap(DYNAMIC_MAP_XML));

        BOOST_REQUIRE(isTextLayer(dynamicMapGui->getLayerById(DYNAMIC_POINT_TEXT_LAYER_ID), styles::Label::PointLabel));
        BOOST_REQUIRE(isTextLayer(dynamicMapGui->getLayerById(DYNAMIC_POLYGON_TEXT_LAYER_ID), styles::Label::PolygonLabel));
        BOOST_REQUIRE(isTextLayer(dynamicMapGui->getLayerById(DYNAMIC_POLYLINE_TEXT_LAYER_ID), styles::Label::PolylineLabel));

        std::set<unsigned int> zooms;
        zooms.insert(18);

        mapcompiler::Options options(mapcompiler::Options::ForNavigator);
        BOOST_REQUIRE(options.isForNavigator);

        BOOST_REQUIRE_NO_THROW(
            mapcompiler::compile(
                dynamicMapGui->map(),
                STATIC_MAP_XML,
                options,
                zooms,
                dynamicMapGui->map().locales(),
                test::map::createProgressStub()));

        staticMap.env().stylesLibrary.reset(new core::StylesLibrary(""));
        BOOST_REQUIRE_NO_THROW(staticMap.loadFromXml(STATIC_MAP_XML, true));
        BOOST_REQUIRE_NO_THROW(staticMap.open(test::map::createProgressStub()));
    }

    core::ILayerPtr findStaticLayerByDynamicId(core::LayerIdType id)
    {
        return findLayerBySourceId(staticMap.rootLayer(), id);
    }

    LayerStats collectLayerStats(core::LayerIdType dynamicLayerId, styles::Label::Type styleType)
    {
        auto textLayer = findStaticLayerByDynamicId(dynamicLayerId);
        BOOST_REQUIRE(isTextLayer(textLayer, styleType));

        auto* searchable = textLayer->cast<core::ISearchableLayer>();
        BOOST_REQUIRE(searchable);

        auto style = textLayer->renderStyle().get();
        auto rules = labeler::rules::parse(*style, *textLayer->labelStyle(),
            proj::pixelPerUnit(style->visibilityScaling().max()));
        labeler::ConvertLabelBackward convert(rules, staticMap.fontLoader());

        core::FeatureCapabilities fc {core::CapabilityFeaturePlacedLabel, core::CapabilityFeaturePlacedLabel2};
        auto fit = searchable->findFeatures(proj::EARTH_BOX, fc);
        fit->reset();

        LayerStats stats;
        while (fit->hasNext()) {
            auto& feature = fit->next();
            ++stats.nFeatures;

            auto& pl = feature.placedLabel();
            convert(feature.placedLabel2(), pl);
            if (pl.sourcePoint.has_value())
                ++stats.nWithSourcePoint;
        }

        return stats;
    }

public:
    core::Map staticMap;
};

}

BOOST_AUTO_TEST_SUITE( labeler )
BOOST_FIXTURE_TEST_SUITE( navigator_labels, CompiledNavigatorMap )

BOOST_AUTO_TEST_CASE( pointLabels )
{
    auto stats = collectLayerStats(DYNAMIC_POINT_TEXT_LAYER_ID, styles::Label::PointLabel);

    BOOST_CHECK(stats.nFeatures > 0);
    BOOST_CHECK_EQUAL(stats.nFeatures, stats.nWithSourcePoint);
}

BOOST_AUTO_TEST_CASE( polygonLabels )
{
    auto stats = collectLayerStats(DYNAMIC_POLYGON_TEXT_LAYER_ID, styles::Label::PolygonLabel);

    BOOST_CHECK(stats.nFeatures > 0);
    BOOST_CHECK_EQUAL(stats.nFeatures, stats.nWithSourcePoint);
}

BOOST_AUTO_TEST_CASE( polylineLabels )
{
    auto stats = collectLayerStats(DYNAMIC_POLYLINE_TEXT_LAYER_ID, styles::Label::PolylineLabel);

    BOOST_CHECK(stats.nFeatures > 0);
    BOOST_CHECK_EQUAL(stats.nWithSourcePoint, 0);
}

BOOST_AUTO_TEST_SUITE_END() // navigator_labels
BOOST_AUTO_TEST_SUITE_END() // labeler
