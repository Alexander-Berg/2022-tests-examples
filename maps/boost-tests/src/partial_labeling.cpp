namespace maps { namespace renderer { }}
namespace maps { namespace renderer5 { using namespace ::maps::renderer; }}

#include "../../../core/direct_file_text_layer.h"

#include "tests/boost-tests/include/tools/map_tools.h"
#include "../include/contexts.hpp"

#include "core/feature.h"
#include "core/IRasterizableLayer.h"
#include "labeler/i_labelable_layer.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/box_legacy.h>
#include <yandex/maps/renderer5/core/ITypedLayer.h>
#include <yandex/maps/renderer5/labeler/label_generator.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::labeler;

namespace {
const std::string MAP_XML_NAME("tests/boost-tests/maps/PartialLabeled.xml");
const core::LayerIdType FULL_LABELED_LOW_PRIORITY_POINT_LAYER_ID = 10;
const core::LayerIdType FULL_LABELED_HIGH_PRIORITY_POINT_LAYER_ID = 1;
const core::LayerIdType FULL_LABELED_POINT_LAYER_IDS[] = {FULL_LABELED_LOW_PRIORITY_POINT_LAYER_ID, FULL_LABELED_HIGH_PRIORITY_POINT_LAYER_ID};
const size_t FULL_LABELED_POINT_LAYER_IDS_COUNT = sizeof(FULL_LABELED_POINT_LAYER_IDS)/sizeof(FULL_LABELED_POINT_LAYER_IDS[0]);
const core::LayerIdType PARTIAL_LABELED_POINT_LAYER_ID = 2;
const core::LayerIdType FULL_LABELED_POLYLINE_LAYER_ID = 7;
const core::LayerIdType PARTIAL_LABELED_POLYLINE_LAYER_OVERRIDE_ID = 8;
const core::LayerIdType PARTIAL_LABELED_POLYLINE_LAYER_HIDE_ID = 9;
const core::LayerIdType PARTIAL_LABELED_OFFESETTED_POINT_LAYER_ID = 11;

core::FeatureIdType getRealFeatureId(core::ILayer& layer, core::FeatureIdType sourceFeatureId)
{
    auto* labelableLayer = layer.get<ILabelableLayer>();
    auto* searchableLayer = labelableLayer->getSource().get<core::ISearchableLayer>();
    auto fIt = searchableLayer->findFeatures(
            layer.getExtent(),
            searchableLayer->featureCapabilities());
    fIt->reset();
    while (fIt->hasNext()) {
        auto& f = fIt->next();
        if (f.sourceId() == sourceFeatureId)
            return f.id();
    }

    throw std::runtime_error("Feature not found");
}

size_t posInCollection(core::LayerIdType id, const core::LayersCollection& layers)
{
    return
        std::distance(
            layers.begin(),
            std::find_if(
                layers.begin(),
                layers.end(),
                [id](const core::ILayerPtr& l){ return l->id() == id; }));
}

double layerPriority(const core::ILayerPtr& layer)
{
    auto* labelableLayer = layer->cast<labeler::ILabelableLayer>();
    BOOST_REQUIRE(labelableLayer);
    return static_cast<styles::Label&>(*labelableLayer->labelStyle()).priority();
}

class PartialLabelingContext: public CleanContext<>
{
public:
    PartialLabelingContext()
    {
        mapGui = test::map::createTestMapGui();
        mapGui->loadFromXml(MAP_XML_NAME, false);
        mapGui->open(test::map::createProgressStub());

        mapExtent = mapGui->map().getExtent();

        lgPartial = std::make_unique<LabelGenerator>(mapGui->map(), mapGui->map().labelableLayers());
    }

    void placeAllLabels(unsigned int zoom)
    {
        LabelingSettings settings;
        settings.setForCompilation();

        LabelGenerator lg(mapGui->map(), mapGui->map().labelableLayers(), settings);
        lg.placeLabels(test::map::createProgressStub(), mapExtent, zoom);
    }

    uint64_t countFeatures(core::ILayer& layer)
    {
        return test::getFeatureCount(layer.get<core::ISearchableLayer>());
    }

    uint64_t countFeatures(core::LayerIdType layerid)
    {
        auto layer = mapGui->map().rootGroupLayer()->getLayerById(layerid);
        BOOST_REQUIRE(layer);
        return countFeatures(*layer);
    }

    uint64_t countFeatures(const core::LayerIdType* layerIds, size_t layersIdsCount)
    {
        uint64_t result = 0;
        for (size_t i = 0; i < layersIdsCount; ++i)
            result += countFeatures(layerIds[i]);

        return result;
    }

    core::ILayerPtr layerById(core::LayerIdType layerId)
    {
        core::ILayerPtr layer = mapGui->map().rootGroupLayer()->getLayerById(layerId);
        BOOST_REQUIRE(layer);
        return layer;
    }

    core::ILayerPtr partialLabelableLayerById(core::LayerIdType layerId)
    {
        core::ILayerPtr layer = layerById(layerId);
        BOOST_REQUIRE(layer->get<core::ITypedLayer>()->type() == core::ITypedLayer::DirectFileTextLayer);
        BOOST_REQUIRE(layer->has<labeler::IPartialLabelableLayer>());
        return layer;
    }

public:
    core::IMapGuiPtr mapGui;
    base::BoxD mapExtent;

    std::unique_ptr<LabelGenerator> lgPartial;
};

}  // namespace

BOOST_AUTO_TEST_SUITE( labeler_partial )

BOOST_FIXTURE_TEST_CASE( placePointLabelNoConflicts, PartialLabelingContext )
{
    const unsigned int ZOOM_INDEX = 18;

    core::ILayerPtr partialLabelableLayer = partialLabelableLayerById(PARTIAL_LABELED_POINT_LAYER_ID);

    // full labeling
    partialLabelableLayer->get<core::IRenderStyleHolder>()->renderStyle()->setEnabled(false);
    BOOST_REQUIRE_NO_THROW(placeAllLabels(ZOOM_INDEX));

    // check that 'full labelable' point layer has all labels
    BOOST_CHECK(countFeatures(FULL_LABELED_POINT_LAYER_IDS, FULL_LABELED_POINT_LAYER_IDS_COUNT) == 2);
    // check that 'partial labelable' point layer wasn't labeled
    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 0);

    // partial labeling
    partialLabelableLayer->get<core::IRenderStyleHolder>()->renderStyle()->setEnabled(true);
    LabelingOperation op;
    op.opType = LabelingOperation::opInsert;
    op.layerId = PARTIAL_LABELED_POINT_LAYER_ID;
    op.featureId = getRealFeatureId(*partialLabelableLayer, 3);
    op.bb1 = core::boxToCoreBBox(mapExtent);
    op.bb2 = core::boxToCoreBBox(mapExtent);
    BOOST_REQUIRE_NO_THROW(lgPartial->placeLabels(ZOOM_INDEX, op));

    BOOST_CHECK(countFeatures(FULL_LABELED_POINT_LAYER_IDS, FULL_LABELED_POINT_LAYER_IDS_COUNT) == 2);
    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 1);
}

/**
 * Point label conflicts with itself, placed during full labeling process.
 */
BOOST_FIXTURE_TEST_CASE( conflictingPointLabelNoInsert, PartialLabelingContext )
{
    const unsigned int ZOOM_INDEX = 18;

    // full labeling
    BOOST_REQUIRE_NO_THROW(placeAllLabels(ZOOM_INDEX));

    // check that 'full labelable' point layer has all labels
    BOOST_CHECK(countFeatures(FULL_LABELED_POINT_LAYER_IDS, FULL_LABELED_POINT_LAYER_IDS_COUNT) == 2);

    // check that 'partial labelable' point layer was labeled
    core::ILayerPtr partialLabelableLayer = partialLabelableLayerById(PARTIAL_LABELED_POINT_LAYER_ID);
    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 1);

    // partial labeling
    LabelingOperation op;
    op.opType = LabelingOperation::opInsert;
    op.layerId = PARTIAL_LABELED_POINT_LAYER_ID;
    op.featureId = getRealFeatureId(*partialLabelableLayer, 3);
    op.bb1 = core::boxToCoreBBox(mapExtent);
    op.bb2 = core::boxToCoreBBox(mapExtent);
    BOOST_REQUIRE_NO_THROW(lgPartial->placeLabels(ZOOM_INDEX, op));

    BOOST_CHECK(countFeatures(FULL_LABELED_POINT_LAYER_IDS, FULL_LABELED_POINT_LAYER_IDS_COUNT) == 2);
    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 1);
}

/**
 * Point label intersects with 2 neighbors: one with priority 0.5, and another with
 * priority 10. With first label it forms 'delayedConflict', intersection with
 * second removes the new label, thus new label's id from delayedConflict doesn't
 * point to any label. During resolving delayedConflict we'll try to fetch label
 * by incorrect id and we'll get a segfault. This test ensures that we will skip
 * invalid ids in situations like this;
 */
BOOST_FIXTURE_TEST_CASE( brokenDelayedConflict, PartialLabelingContext )
{
    const unsigned int ZOOM_INDEX = 17;

    // full labeling
    core::ILayerPtr partialLabelableLayer = partialLabelableLayerById(PARTIAL_LABELED_POINT_LAYER_ID);
    partialLabelableLayer->get<core::IRenderStyleHolder>()->renderStyle()->setEnabled(false);
    BOOST_REQUIRE_NO_THROW(placeAllLabels(ZOOM_INDEX));

    // check that layer with lower priority precede the one with high priority
    auto textLayers = mapGui->map().labelableLayers();
    BOOST_REQUIRE(
        layerPriority(layerById(FULL_LABELED_LOW_PRIORITY_POINT_LAYER_ID)) <
        layerPriority(layerById(FULL_LABELED_HIGH_PRIORITY_POINT_LAYER_ID)));
    BOOST_REQUIRE(
        posInCollection(FULL_LABELED_LOW_PRIORITY_POINT_LAYER_ID, textLayers) <
        posInCollection(FULL_LABELED_HIGH_PRIORITY_POINT_LAYER_ID, textLayers));

    // check that 'full labelable' point layer has all labels
    BOOST_CHECK(countFeatures(FULL_LABELED_POINT_LAYER_IDS, FULL_LABELED_POINT_LAYER_IDS_COUNT) == 2);

    // check that 'partial labelable' point layer wasn't labeled
    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 0);

    // partial labeling
    partialLabelableLayer->get<core::IRenderStyleHolder>()->renderStyle()->setEnabled(true);
    LabelingOperation op;
    op.opType = LabelingOperation::opInsert;
    op.layerId = PARTIAL_LABELED_POINT_LAYER_ID;
    op.featureId = getRealFeatureId(*partialLabelableLayer, 3);
    op.bb1 = core::boxToCoreBBox(mapExtent);
    op.bb2 = core::boxToCoreBBox(mapExtent);
    BOOST_REQUIRE_NO_THROW(lgPartial->placeLabels(ZOOM_INDEX, op));

    BOOST_CHECK(countFeatures(FULL_LABELED_POINT_LAYER_IDS, FULL_LABELED_POINT_LAYER_IDS_COUNT) == 2);
    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 0);
}

/**
 * Polyline label conflicts with previous placed labels and force them
 * to be removed by higher priority.
 */
BOOST_FIXTURE_TEST_CASE( placePolylineLabelWithConflicts, PartialLabelingContext )
{
    const unsigned int ZOOM_INDEX = 18;

    core::ILayerPtr partialPolylineOverrideLayer = partialLabelableLayerById(PARTIAL_LABELED_POLYLINE_LAYER_OVERRIDE_ID);

    // full labeling without 'partial labelabele' polyline layers
    partialPolylineOverrideLayer->get<core::IRenderStyleHolder>()->renderStyle()->setEnabled(false);
    partialLabelableLayerById(PARTIAL_LABELED_POLYLINE_LAYER_HIDE_ID)->get<core::IRenderStyleHolder>()->renderStyle()->setEnabled(false);
    BOOST_REQUIRE_NO_THROW(placeAllLabels(ZOOM_INDEX));
    BOOST_CHECK_EQUAL(0, countFeatures(*partialPolylineOverrideLayer));
    BOOST_CHECK_EQUAL(0, countFeatures(PARTIAL_LABELED_POLYLINE_LAYER_HIDE_ID));
    BOOST_CHECK_EQUAL(4, countFeatures(FULL_LABELED_POLYLINE_LAYER_ID));

    // partial labeling
    partialPolylineOverrideLayer->get<core::IRenderStyleHolder>()->renderStyle()->setEnabled(true);
    LabelingOperation op;
    op.opType = LabelingOperation::opInsert;
    op.layerId = PARTIAL_LABELED_POLYLINE_LAYER_OVERRIDE_ID;
    op.featureId = getRealFeatureId(*partialPolylineOverrideLayer, 1);
    op.bb1 = core::boxToCoreBBox(mapExtent);
    op.bb2 = core::boxToCoreBBox(mapExtent);
    BOOST_REQUIRE_NO_THROW(lgPartial->placeLabels(ZOOM_INDEX, op));

    BOOST_CHECK_EQUAL(3, countFeatures(FULL_LABELED_POLYLINE_LAYER_ID));
    BOOST_CHECK_EQUAL(1, countFeatures(*partialPolylineOverrideLayer));
}

BOOST_FIXTURE_TEST_CASE( labelWithoutCandidates, PartialLabelingContext )
{
    const unsigned int ZOOM_INDEX = 17;

    // full labeling
    BOOST_CHECK_NO_THROW(placeAllLabels(ZOOM_INDEX));

    auto partialLabelableLayer = partialLabelableLayerById(PARTIAL_LABELED_POINT_LAYER_ID);

    // partial labeling
    LabelingOperation op;
    op.opType = LabelingOperation::opUpdate;
    op.layerId = PARTIAL_LABELED_POINT_LAYER_ID;
    op.featureId = getRealFeatureId(*partialLabelableLayer, 3);
    op.bb1 = core::boxToCoreBBox(mapExtent);
    op.bb2 = core::boxToCoreBBox(mapExtent);
    BOOST_CHECK_NO_THROW(lgPartial->placeLabels(ZOOM_INDEX, op));

    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 0);
}

BOOST_FIXTURE_TEST_CASE( placeOffsettedPointLabel, PartialLabelingContext )
{
    const unsigned int ZOOM_INDEX = 18;

    core::ILayerPtr partialLabelableLayer = partialLabelableLayerById(PARTIAL_LABELED_OFFESETTED_POINT_LAYER_ID);

    LabelingOperation op;
    op.opType = LabelingOperation::opInsert;
    op.layerId = PARTIAL_LABELED_OFFESETTED_POINT_LAYER_ID;
    op.featureId = getRealFeatureId(*partialLabelableLayer, 3);
    BOOST_REQUIRE_NO_THROW(lgPartial->placeLabels(ZOOM_INDEX, op));
    BOOST_CHECK(countFeatures(*partialLabelableLayer) == 1);
    BOOST_CHECK(op.locality.valid());
}

BOOST_AUTO_TEST_SUITE_END()
