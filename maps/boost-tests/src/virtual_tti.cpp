#include "tests/boost-tests/include/tools/map_tools.h"

#include "mapcompiler/consts.h"
#include "core/ISearchableLayer.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/StyleHolders.h>
#include <yandex/maps/renderer5/core/StylesLibrary.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer5/styles/styles.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::mapcompiler;
using namespace maps::renderer5::test;

namespace {

const char* NAME_SRC_ONLYICON    = "tests/boost-tests/maps/OnlyIconMap.xml";
const char* NAME_SRC_TEXTANDICON = "tests/boost-tests/maps/TextAndIconMap.xml";
const char* NAME_DST_ONLYICON    = "tmp/TextIconsOnlyIcon.compiled.xml";
const char* NAME_DST_TEXTANDICON = "tmp/TextIconsTextAndIcon.compiled.xml";

core::ILayer* findLayerByName(core::ILayer& layer, const std::wstring& name)
{
    if (layer.name() == name)
        return &layer;

    if (auto groupLayer = layer.cast<core::IGroupLayer>()) {
        for (const auto& childLayer : groupLayer->children())
            if (auto res = findLayerByName(*childLayer, name))
                return res;
    }
    return 0;
}

void compileMap(Map& map, Map& staticMap, const std::string& staticMapName)
{
    std::set<unsigned int> zooms;
    zooms.insert(18);
    zooms.insert(19);

    mapcompiler::Options options;
    BOOST_REQUIRE(options.resolveXmlLinks == false);

        mapcompiler::compile(
            map,
            staticMapName,
            options,
            zooms,
            map.locales(),
            map::createProgressStub());

    staticMap.env().stylesLibrary.reset(new core::StylesLibrary(""));
    staticMap.loadFromXml(staticMapName, true);
    staticMap.open(map::createProgressStub());
}

core::IMapGuiPtr openMapAndLoadIconStyle(
    const std::string& mapName,
    styles::Symbol& iconStyle)
{
    core::IMapGuiPtr dynamicMapGui;
    BOOST_REQUIRE_NO_THROW(dynamicMapGui = test::map::openMap(mapName));

    // open and check dynamic map
    auto iconLayer = dynamicMapGui->getLayerById(2);
    BOOST_REQUIRE(iconLayer);
    auto holder = iconLayer->cast<core::ILabelStyleHolder>();
    BOOST_REQUIRE(holder);
    auto labelStyle = holder->labelStyle();
    BOOST_REQUIRE(labelStyle);
    BOOST_REQUIRE(labelStyle->type() == styles::Label::PointLabel);

    auto pointLabelStyle = static_cast<styles::PointLabelStyle*>(labelStyle.get());
    iconStyle = pointLabelStyle->pointPosition().icon();

    return dynamicMapGui;
}

core::IGroupLayer* getIconGroup(core::Map& map)
{
    auto iconSubtree = findLayerByName(*map.rootLayer(), PredefinedNames::text);

    BOOST_REQUIRE(iconSubtree);
    core::IGroupLayer* iconSubtreeGroup = iconSubtree->cast<core::IGroupLayer>();
    BOOST_REQUIRE(iconSubtreeGroup);

    return iconSubtreeGroup;
}

void collectLeafs(core::LayersCollection& layers, core::IGroupLayer* group)
{
    for (const auto& child : group->children()) {
        core::IGroupLayer* childGroup = child->cast<core::IGroupLayer>();
        if (childGroup) {
            collectLeafs(layers, childGroup);
        } else {
            layers.push_back(child);
        }
    }
}

void checkIconLayers(
    const core::LayersCollection& layers,
    styles::Symbol& iconStyle)
{
    for (const auto& layer : layers) {
        auto typedLayer = layer->cast<core::ITypedLayer>();
        BOOST_REQUIRE(typedLayer);
        if (typedLayer->type() != core::ITypedLayer::IconLayer)
            continue;
        auto rsh = layer->cast<core::IRenderStyleHolder>();
        BOOST_REQUIRE(rsh);
        auto& renderStyle = rsh->renderStyle();
        BOOST_REQUIRE(renderStyle);
        BOOST_REQUIRE(renderStyle->type() == styles::Render::PointRender);

        auto* pointRenderStyle = static_cast<styles::PointRenderStyle*>(renderStyle.get());

        // compare everything except filename.
        // (because it was changed and it is correct behavior)
        iconStyle.setFilename(pointRenderStyle->symbol().filename());
        BOOST_CHECK(pointRenderStyle->symbol() == iconStyle);

        auto searchableLayer = layer->cast<ISearchableLayer>();
        BOOST_REQUIRE(searchableLayer);
        BOOST_CHECK_EQUAL(getFeatureCount(searchableLayer), 3);
    }
}

void checkTextLayers(const core::LayersCollection& layers)
{
    for (const auto& layer : layers) {
        auto typedLayer = layer->cast<core::ITypedLayer>();
        BOOST_REQUIRE(typedLayer);
        if (typedLayer->type() != core::ITypedLayer::OptimizedFileTextLayer)
            continue;
        auto lsh = layer->cast<core::ILabelStyleHolder>();
        BOOST_REQUIRE(lsh);
        auto& labelStyle = lsh->labelStyle();
        BOOST_REQUIRE(labelStyle);
        BOOST_REQUIRE(labelStyle->type() == styles::Label::PointLabel);

        auto& pointLabelStyle = static_cast<styles::PointLabelStyle&>(*labelStyle);
        BOOST_CHECK(!pointLabelStyle.pointPosition().icon().enabled());

        auto searchableLayer = layer->cast<ISearchableLayer>();
        BOOST_REQUIRE(searchableLayer);
        BOOST_CHECK_EQUAL(getFeatureCount(searchableLayer), 3);
    }
}


}  // namespace

BOOST_AUTO_TEST_SUITE( virtual_tti )

BOOST_AUTO_TEST_CASE( onlyIcons )
{
    map::deleteFilesFromTmpDir();
    styles::Symbol iconStyle;

    {
        core::IMapGuiPtr dynamicMapGui = openMapAndLoadIconStyle(NAME_SRC_ONLYICON, iconStyle);

        // compile and test map with only-icon labels
        core::Map staticMap(core::MapMode::Static);
        compileMap(dynamicMapGui->map(), staticMap, NAME_DST_ONLYICON);

        auto iconGroup = getIconGroup(staticMap);
        BOOST_REQUIRE_EQUAL(iconGroup->childCount(), 4);
        core::LayersCollection iconLayers;
        collectLeafs(iconLayers, iconGroup);
        checkIconLayers(iconLayers, iconStyle);
    }
}

BOOST_AUTO_TEST_CASE( iconsAndText )
{
    map::deleteFilesFromTmpDir();
    styles::Symbol iconStyle;


    {
        core::IMapGuiPtr dynamicMapGui = openMapAndLoadIconStyle(NAME_SRC_TEXTANDICON, iconStyle);

        // compile and test map with text and icon labels
        core::Map staticMap(core::MapMode::Static);
        compileMap(dynamicMapGui->map(), staticMap, NAME_DST_TEXTANDICON);

        // check icon part of the map

        auto iconGroup = getIconGroup(staticMap);
        BOOST_REQUIRE_EQUAL(iconGroup->childCount(), 3);

        core::LayersCollection iconLayers;
        collectLeafs(iconLayers, iconGroup);
        checkIconLayers(iconLayers, iconStyle);

        // check text part of the map

        auto textSubtree = findLayerByName(*staticMap.rootLayer(), PredefinedNames::text);
        BOOST_REQUIRE(textSubtree);

        core::IGroupLayer* textSubtreeGroup = textSubtree->cast<core::IGroupLayer>();
        BOOST_REQUIRE(textSubtreeGroup);

        // process single text copy for empty locale

        core::IGroupLayer* mainTextGroup = textSubtreeGroup->cast<core::IGroupLayer>();
        BOOST_REQUIRE(mainTextGroup);
        BOOST_REQUIRE_EQUAL(mainTextGroup->childCount(), 3);

        core::LayersCollection textLayers;
        collectLeafs(textLayers, mainTextGroup);
        checkTextLayers(textLayers);
    }
}

BOOST_AUTO_TEST_SUITE_END()
