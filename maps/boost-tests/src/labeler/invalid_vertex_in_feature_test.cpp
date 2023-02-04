namespace maps { namespace renderer { }}
namespace maps { namespace renderer5 { using namespace ::maps::renderer; }}

#include "tests/boost-tests/include/tools/map_tools.h"

#include "labeler/labeler_exceptions.h"
#include "core/direct_file_text_layer.h"
#include <maps/renderer/libs/base/include/geom/vector.h>
#include "core/MemoryGeometryFeatureContainer.h"
#include "core/feature.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/box_legacy.h>
#include <yandex/maps/renderer5/labeler/label_generator.h>
#include <yandex/maps/renderer5/labeler/LabelingOperation.h>
#include <yandex/maps/renderer5/styles/styles.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/test/unit_test.hpp>

using namespace maps;
using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::labeler;
using namespace maps::renderer5::test;
using namespace maps::renderer::base;
using namespace maps::renderer;

namespace {
const unsigned int ZOOM_INDEX = 17;

class MemoryTextLayerSourceGeometryContainer:
    public MemoryGeometryFeatureContainer,
    public ILayerTextExpression
{
public:
    MemoryTextLayerSourceGeometryContainer()
        : MemoryGeometryFeatureContainer()
    {
        initCapabilities();
    }

    virtual std::wstring textExpression() const { return std::wstring(L"_text_"); }
    virtual void setTextExpression(const std::wstring&) {}

    virtual std::wstring textAlternativeExpression() const { return std::wstring(L"_text_alt_"); }
    virtual void setTextAlternativeExpression(const std::wstring& expr) {}

protected:
    virtual void initCapabilities()
    {
        MemoryGeometryFeatureContainer::initCapabilities();
        addCapability<ILayerTextExpression>(this);
    }
};

FeaturePtr createFeature(FeatureIdType id,
                         const PointD& point,
                         const std::wstring& text)
{
    FeatureCapabilities fc;
    fc.add(core::CapabilityFeatureText);
    auto f = std::make_shared<renderer::feature::Feature>(FeatureType::Point, fc);
    f->setId(id);
    f->text() = text;
    f->geom().shapes().addMoveTo(point);
    f->geom().shapes().addLineTo(point);
    f->geom().shapes().addStop();
    return f;
}

ILayerPtr createTextLayer(MapEnvironment& env,
                          core::IFeatureContainerUPtr&& sourceContainer)
{
    ILayerPtr textLayer(
        new DirectFileTextLayer(env, std::move(sourceContainer)));
    textLayer->setName(L"testTextLayer");
    textLayer->setId(env.uidGenerator->generateId());
    styles::TextRenderStylePtr textRenderStyle(new styles::TextRenderStyle());
    textRenderStyle->setEnabled(true);
    textRenderStyle->foreground().size().setValue(10);
    textLayer->get<IRenderStyleHolder>()->setRenderStyle(textRenderStyle);
    styles::PointLabelStylePtr pointLabelStyle(new styles::PointLabelStyle());
    pointLabelStyle->setEnabled(true);
    textLayer->get<ILabelStyleHolder>()->setLabelStyle(pointLabelStyle);
    return textLayer;
}

LabelingOperation labelingOperationForFeature(LayerIdType lid, FeatureIdType fid)
{
    LabelingOperation op;
    op.opType = LabelingOperation::opInsert;
    op.layerId = lid;
    op.featureId = fid;
    op.bb1 = core::boxToCoreBBox(proj::EARTH_BOX);
    op.bb2 = core::boxToCoreBBox(proj::EARTH_BOX);
    return op;
}

struct CreatedMapContext
{
    CreatedMapContext()
    {
        testMapGui = test::map::createTestMapGui();

        auto geometryFC = std::make_unique<MemoryTextLayerSourceGeometryContainer>();
        geometryFC->setFeatureType(FeatureType::Point);
        geometryFC->insertFeature(*createFeature(0, PointD(std::numeric_limits<double>::quiet_NaN(), 0), L"abc_nan"));
        geometryFC->insertFeature(*createFeature(1, PointD(std::numeric_limits<double>::infinity(), 0), L"abc_pinf"));
        geometryFC->insertFeature(*createFeature(2, PointD(-std::numeric_limits<double>::infinity(), 0), L"abc_ninf"));
        geometryFC->insertFeature(*createFeature(3, PointD(1000, 1000), L"abc"));
        textLayer = createTextLayer(testMapGui->map().env(), std::move(geometryFC));

        IGroupLayer* rootGroupLayer = testMapGui->map().rootLayer()->get<IGroupLayer>();
        rootGroupLayer->insertLayer(textLayer, static_cast<int>(rootGroupLayer->childCount()));

        testMapGui->open(map::createProgressStub());
    }

    IMapGuiPtr testMapGui;
    ILayerPtr textLayer;

};
}

BOOST_AUTO_TEST_SUITE( labeler )
BOOST_FIXTURE_TEST_CASE( invalidVertexInCompilation, CreatedMapContext )
{
    LabelingSettings settings;
    settings.setForCompilation();

    const LayersCollection textLayers = testMapGui->map().labelableLayers();
    BOOST_REQUIRE_EQUAL(getFeatureCount(textLayer->get<ISearchableLayer>()), 0);

    LabelGenerator lg(testMapGui->map(), textLayers, settings);
    BOOST_CHECK_NO_THROW(lg.placeLabels(map::createProgressStub(), proj::EARTH_BOX, ZOOM_INDEX));

    BOOST_CHECK_EQUAL(getFeatureCount(textLayer->get<ISearchableLayer>()), 1);
}

BOOST_AUTO_TEST_SUITE_END()

BOOST_AUTO_TEST_SUITE( labeler_partial )
BOOST_FIXTURE_TEST_CASE( invalidVertexInPartialLabeling, CreatedMapContext )
{
    const LayersCollection textLayers = testMapGui->map().labelableLayers();
    BOOST_REQUIRE_EQUAL(getFeatureCount(textLayer->get<ISearchableLayer>()), 0);
    LabelGenerator lg(testMapGui->map(), textLayers);

    {
        auto lo = labelingOperationForFeature(textLayer->id(), 0);
        lg.placeLabels(ZOOM_INDEX, lo);
    }
    {
        auto lo = labelingOperationForFeature(textLayer->id(), 1);
        lg.placeLabels(ZOOM_INDEX, lo);
    }
    {
        auto lo = labelingOperationForFeature(textLayer->id(), 2);
        lg.placeLabels(ZOOM_INDEX, lo);
    }

    BOOST_CHECK_EQUAL(getFeatureCount(textLayer->get<ISearchableLayer>()), 0);
}

BOOST_AUTO_TEST_SUITE_END()
