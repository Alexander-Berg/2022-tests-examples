#include "tests/boost-tests/include/tools/map_tools.h"

#include "core/feature.h"
#include "core/IRasterizableLayer.h"

#include <yandex/maps/renderer5/core/ContextProvider.h>
#include <yandex/maps/renderer5/core/IContextable.h>
#include <yandex/maps/renderer5/core/ILayerScalingExpression.h>
#include <yandex/maps/renderer5/core/MapTools.h>
#include <yandex/maps/renderer5/core/RenderingContextVariables.h>
#include <yandex/maps/renderer5/core/StyleHolders.h>
#include <yandex/maps/renderer5/styles/PointRenderStyle.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer;

namespace maps {
namespace renderer5 {
namespace core {

namespace {
    const std::wstring zMinExpr = L"ID + 12";
    const std::wstring zMaxExpr = L"19 - ID";
    const std::string sourceFileName = "tests/boost-tests/data/three_points.mif";
    const std::string patternFile = "tests/boost-tests/data/patterns/owl.png";

    void createTestMap(const std::string& xmlFileName)
    {
        core::IMapGuiPtr map = test::map::createTestMapGui();

        map->open(test::map::createProgressStub());

        auto layers = map->addLayerFromSource(
            test::map::createProgressStub(),
            io::path::absolute(sourceFileName), 0);

        BOOST_REQUIRE(layers.size() == 1);

        auto layer = layers.front();

        auto rsh = layer->cast<core::IRenderStyleHolder>();
        BOOST_REQUIRE(rsh);
        {
            using namespace styles;
            auto& pointStyle = static_cast<styles::PointRenderStyle&>(*rsh->renderStyle());

            pointStyle.setEnabled(true);
            pointStyle.symbol().setEnabled(true);
            pointStyle.symbol().setFilename(io::path::absolute(patternFile));
            pointStyle.symbol().scale().setValue(10);
        }

        auto exprHolder = layer->cast<core::ILayerScalingExpression>();
        BOOST_REQUIRE(exprHolder != 0);

        exprHolder->setZMinExpression(zMinExpr);
        exprHolder->setZMaxExpression(zMaxExpr);

        map->saveAs(test::map::createProgressStub(), xmlFileName);
        map->close();

        BOOST_REQUIRE(io::exists(xmlFileName));
    }

    inline size_t fItSize(renderer::feature::FeatureIterUPtr& fIt)
    {
        size_t sz = 0;
        for (fIt->reset(); fIt->hasNext(); fIt->next(), ++sz) {}
        return sz;
    }

    inline void resetZoomVariable(
        ContextProviderPtr provider,
        unsigned int zoomValue)
    {
        using namespace RenderingContextVariables;
        provider->resetVariable(zoom, zoomValue);
    }

    inline bool checkVScaling(ILayer& layer, unsigned int zMin, unsigned int zMax)
    {
        const styles::VisibilityScaling& vs = layer.renderStyle()->visibilityScaling();
        return (vs.min() == zMin) && (vs.max() == zMax);
    }
}

BOOST_AUTO_TEST_SUITE( scaling_expressions )

BOOST_AUTO_TEST_CASE( loadSaveMap )
{
    test::map::deleteFilesFromTmpDir();

    const std::string mapXmlFileName = io::tempDirPath() + "/test.xml";

    BOOST_REQUIRE_NO_THROW(createTestMap(mapXmlFileName));

    // load saved map, and check scaling expessions
    //
    core::IMapGuiPtr map = test::map::openMap(mapXmlFileName);
    const core::LayerIdType layerId = 1;

    auto layer = map->rootLayer()->get<IGroupLayer>()->getLayerById(layerId);

    BOOST_REQUIRE(layer);
    {
        core::ILayerScalingExpression* exprHolder =
            layer->cast<core::ILayerScalingExpression>();
        BOOST_REQUIRE(exprHolder);
        BOOST_CHECK(exprHolder->zMinExpression() == zMinExpr);
        BOOST_CHECK(exprHolder->zMaxExpression() == zMaxExpr);
    }

    BOOST_REQUIRE(layer->has<core::IContextable>());

    core::ContextProviderPtr contextProvider = core::createContextProvider();
    map->setContextProvider(contextProvider);

    resetZoomVariable(contextProvider, 15);

    IRasterizableLayer* rLayer = layer->cast<IRasterizableLayer>();
    BOOST_REQUIRE(rLayer);

    maps::renderer::base::BoxD bbox = proj::EARTH_BOX;
    {
        FeatureCapabilities fc;
        auto fIt1 = rLayer->findFeatures(bbox, fc, 15);

        fc.add(CapabilityFeatureScaling);
        auto fIt2 = rLayer->findFeatures(bbox, fc, 15);

        BOOST_CHECK(fItSize(fIt1) == 3);
        BOOST_CHECK(fItSize(fIt2) == 3);

        fIt2->reset();
        BOOST_REQUIRE(fIt2->hasNext());
        renderer::feature::Feature& f = fIt2->next();

        BOOST_CHECK(f.zMin().value() == 13);
        BOOST_CHECK(f.zMax().value() == 18);
    }

    core::ISearchableLayer* searchablelayer = layer->cast<core::ISearchableLayer>();
    BOOST_REQUIRE(searchablelayer);

    {
        FeatureCapabilities fc;
        fc.add(CapabilityFeatureScaling);

        resetZoomVariable(contextProvider, 13);
        auto fIt = searchablelayer->findFeatures(bbox, fc);
        BOOST_CHECK(fItSize(fIt) == 1);

        resetZoomVariable(contextProvider, 14);
        fIt = searchablelayer->findFeatures(bbox, fc);
        BOOST_CHECK(fItSize(fIt) == 2);

        resetZoomVariable(contextProvider, 15);
        fIt = searchablelayer->findFeatures(bbox, fc);
        BOOST_CHECK(fItSize(fIt) == 3);

        resetZoomVariable(contextProvider, 16);
        fIt = searchablelayer->findFeatures(bbox, fc);
        BOOST_CHECK(fItSize(fIt) == 3);

        resetZoomVariable(contextProvider, 17);
        fIt = searchablelayer->findFeatures(bbox, fc);
        BOOST_CHECK(fItSize(fIt) == 2);

        resetZoomVariable(contextProvider, 18);
        fIt = searchablelayer->findFeatures(bbox, fc);
        BOOST_CHECK(fItSize(fIt) == 1);
    }

    map->close();

    test::map::deleteFilesFromTmpDir();
}

BOOST_AUTO_TEST_CASE( compileMap )
{
    test::map::deleteFilesFromTmpDir();

    const std::string mapXmlFileName =
        io::tempDirPath() + "/test.xml";
    const std::string staticMapXmlFileName =
        io::tempDirPath() + "/test.static.xml";

    BOOST_REQUIRE_NO_THROW(createTestMap(mapXmlFileName));

    core::IMapGuiPtr map = test::map::openMap(mapXmlFileName);
    {

        std::set<unsigned int> zoomIndices;
        for (unsigned int i = 13; i < 19; ++i)
            zoomIndices.insert(i);

        mapcompiler::Options options;
        options.excludeTextLayers = true;

        options.isForNavigator = true;

        BOOST_REQUIRE_NO_THROW(
            mapcompiler::compile(
                map->map(),
                staticMapXmlFileName,
                options,
                zoomIndices,
                map->map().locales(),
                test::map::createProgressStub()));
    }
    map->close();

    map = test::map::openMap(staticMapXmlFileName);
    {
        auto layers = map->rootLayer()->get<IGroupLayer>()->getSharedChildrenRecursive(
            RasterizableLayersFilter());

        BOOST_REQUIRE(layers.size() == 3);

        auto it = layers.begin();
        BOOST_CHECK(checkVScaling(**(it++), 13, 18) == true);
        BOOST_CHECK(checkVScaling(**(it++), 14, 17) == true);
        BOOST_CHECK(checkVScaling(**(it++), 15, 16) == true);
    }
    map->close();

    test::map::deleteFilesFromTmpDir();
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace core
} // namespace renderer5
} // namespace maps
