#include "tests/boost-tests/include/tools/map_tools.h"

#include "core/MemoryGeometryFeatureContainer.h"
#include "core/feature.h"
#include "core/direct_file_text_layer.h"
#include <maps/renderer/libs/base/include/geom/vector.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/labeler/label_generator.h>
#include <yandex/maps/renderer5/labeler/LabelingOperation.h>
#include <yandex/maps/renderer5/styles/styles.h>
#include <yandex/maps/renderer5/core/box_legacy.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/test/unit_test.hpp>

using namespace maps;
using namespace maps::renderer::base;
using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::labeler;
using namespace maps::renderer5::test;
using namespace maps::renderer;

namespace {
const std::wstring TEXT_WITH_ALL_GLYPHS_VALID = L"abc";
const std::wstring TEXT_WITH_INVALID_GLYPHS = L"abc\r\nabc";
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
    virtual void setTextExpression(const std::wstring& expr) {}

    virtual std::wstring textAlternativeExpression() const { return std::wstring(L"_text_alt_"); }
    virtual void setTextAlternativeExpression(const std::wstring& expr) {}

protected:
    virtual void initCapabilities()
    {
        MemoryGeometryFeatureContainer::initCapabilities();
        addCapability<ILayerTextExpression>(this);
    }
};

FeaturePtr createFeature(FeatureIdType id, const PointD& point, const std::wstring& text)
{
    FeatureCapabilities fc;
    fc.add(core::CapabilityFeatureText);
    auto f = std::make_shared<renderer::feature::Feature>(FeatureType::Point, fc);
    f->setId(id);
    f->text() = text;
    f->geom().shapes().addGeomPoint(point);
    return f;
}

ILayerPtr createTextLayer(MapEnvironment& env, core::IFeatureContainerUPtr&& sourceContainer)
{
    ILayerPtr textLayer(new DirectFileTextLayer(env, std::move(sourceContainer)));
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
}

BOOST_AUTO_TEST_SUITE( labeler )
BOOST_AUTO_TEST_CASE( labelFeatureWithUnknownGlyph )
{
    map::deleteFilesFromTmpDir();

    core::IMapGuiPtr testMapGui = test::map::createTestMapGui();

    auto geometryFC = std::make_unique<MemoryTextLayerSourceGeometryContainer>();
    geometryFC->setFeatureType(FeatureType::Point);
    geometryFC->insertFeature(*createFeature(0, PointD(0, 0), TEXT_WITH_ALL_GLYPHS_VALID));
    geometryFC->insertFeature(*createFeature(1, PointD(0, 10000), TEXT_WITH_INVALID_GLYPHS));
    ILayerPtr textLayer = createTextLayer(testMapGui->map().env(), std::move(geometryFC));

    IGroupLayer* rootGroupLayer = testMapGui->map().rootLayer()->get<IGroupLayer>();
    rootGroupLayer->insertLayer(textLayer, static_cast<int>(rootGroupLayer->childCount()));

    testMapGui->open(map::createProgressStub());

    const LayersCollection textLayers = testMapGui->map().labelableLayers();
    BOOST_REQUIRE(textLayers.size() == 1);

    LabelingSettings settings;
    settings.setForCompilation();

    LabelGenerator lg(testMapGui->map(), textLayers, settings);
    BOOST_CHECK_NO_THROW(lg.placeLabels(map::createProgressStub(), proj::EARTH_BOX, ZOOM_INDEX));

    BOOST_CHECK(getFeatureCount(textLayer->get<ISearchableLayer>()) == 2);
}
BOOST_AUTO_TEST_SUITE_END()

BOOST_AUTO_TEST_SUITE( labeler_partial )
BOOST_AUTO_TEST_CASE( labelFeatureWithUnknownGlyph )
{
    const FeatureIdType FEATURE_ID_TO_LABEL = 0;

    map::deleteFilesFromTmpDir();

    core::IMapGuiPtr testMapGui = test::map::createTestMapGui();

    IGroupLayer* rootGroupLayer = testMapGui->map().rootLayer()->get<IGroupLayer>();
    auto geometryFC = std::make_unique<MemoryTextLayerSourceGeometryContainer>();
    geometryFC->setFeatureType(FeatureType::Point);
    geometryFC->insertFeature(*createFeature(FEATURE_ID_TO_LABEL, PointD(0, 0), TEXT_WITH_INVALID_GLYPHS));
    ILayerPtr textLayer = createTextLayer(testMapGui->map().env(), std::move(geometryFC));
    rootGroupLayer->insertLayer(textLayer, static_cast<int>(rootGroupLayer->childCount()));

    testMapGui->open(map::createProgressStub());

    const LayersCollection textLayers = testMapGui->map().labelableLayers();
    BOOST_REQUIRE(textLayers.size() == 1);
    LabelGenerator lg(testMapGui->map(), textLayers);

    // partial labeling
    LabelingOperation op;
    op.opType = LabelingOperation::opInsert;
    op.layerId = textLayer->id();
    op.featureId = FEATURE_ID_TO_LABEL;
    op.bb1 = core::boxToCoreBBox(proj::EARTH_BOX);
    op.bb2 = core::boxToCoreBBox(proj::EARTH_BOX);
    BOOST_CHECK_NO_THROW(lg.placeLabels(ZOOM_INDEX, op));

    BOOST_CHECK(getFeatureCount(textLayer->get<ISearchableLayer>()) == 1);
}

BOOST_AUTO_TEST_SUITE_END()
