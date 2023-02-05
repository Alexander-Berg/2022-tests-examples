#pragma once

#include "common.h"

#include <yandex/maps/geotex/document.h>
#include <yandex/maps/geotex/scene.h>

namespace maps {
namespace wiki {
namespace test_tools {
namespace doc {

template <class TestData>
class TexTestFormatter {
public:
    explicit TexTestFormatter(geotex::Document& document)
        : document_(document)
    {}

    virtual ~TexTestFormatter() {}

    void format(const std::vector<TestData>& testsData)
    {
        for (const auto& testData : testsData) {
            format(testData);
        }
    }

    void format(const TestData& testData);

protected:

    geotex::ScenePtr
    layersToScene(const LayerPtrList& layers) const
    {
        geotex::ScenePtr scenePtr = std::make_shared<geotex::Scene>();
        for (auto layerPtr : layers) {
            scenePtr->append(layerPtr);
        }

        return scenePtr;
    }

    struct ScenePair {
        geotex::ScenePtr before;
        geotex::ScenePtr after;
    };

    struct TestPrintData {
        ScenePair scenes;
        geolib3::BoundingBox mainViewRect;
        std::list<geolib3::BoundingBox> detalizedRects;
    };

    virtual LayerPtrList geomLayersBefore(const TestData& testData) const = 0;
    virtual LayerPtrList geomLayersAfter(const TestData& testData) const = 0;

    virtual LayerPtrList highlightLayersBefore(const TestData& /*testData*/) const
    { return {}; }
    virtual LayerPtrList highlightLayersAfter(const TestData& /*testData*/) const
    { return {}; }

    virtual std::list<geolib3::BoundingBox>
    customGeomViewRects(const TestData& /*testData*/) const { return {}; }

    void
    format(const ScenePair& scenes, const geolib3::BoundingBox& sceneRect) const;

    TestPrintData
    createTestPrintData(const TestData& testData) const;

    geotex::Document& document_;
};

} // namespace doc
} // namespace test_tools
} // namespace wiki
} // namespace maps

#define YANDEX_MAPS_WIKI_TEST_TOOLS_FORMATTER_H
#include "formatter_inl.h"
#undef YANDEX_MAPS_WIKI_TEST_TOOLS_FORMATTER_H
