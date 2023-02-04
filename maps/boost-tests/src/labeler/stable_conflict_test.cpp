#include "tests/boost-tests/include/tools/map_tools.h"
#include "../../include/contexts.hpp"

#include "labeler/i_labelable_layer.h"
#include "core/feature.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/StylesLibrary.h>
#include <yandex/maps/renderer5/labeler/label_generator.h>
#include <yandex/maps/renderer5/mapcompiler/options.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer/feature/placed_label.h>
#include <yandex/maps/renderer/feature/placed_label2.h>
#include <maps/renderer/libs/base/include/string_convert.h>

#include <boost/test/unit_test.hpp>

#include <vector>

using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::labeler;

namespace {
std::vector<std::wstring> collectLabels(const core::Map& map, const base::BoxD& extent)
{
    std::vector<std::wstring> result;

    core::FeatureCapabilities fc;
    fc.add(core::CapabilityFeaturePlacedLabel);

    for (auto& textLayer : map.labelableLayers()) {
        auto fIt = textLayer->get<ILabelableLayer>()->findFeatures(extent, fc);
        fIt->reset();
        while (fIt->hasNext()) {
            const auto& f = fIt->next();
            if (f.has(core::CapabilityFeaturePlacedLabel2)) {
                result.push_back(base::s2ws(f.placedLabel2().textMain));
                BOOST_CHECK(!result.back().empty());
            } else {
                const PlacedLabel& pl = f.placedLabel();
                BOOST_CHECK(has(pl.ptextMain));
                result.push_back(base::s2ws(pl.ptextMain->text));
            }
        }
    }
    return result;
}
} // namespace


BOOST_AUTO_TEST_SUITE( labeler )

BOOST_AUTO_TEST_CASE( StableSortLabelsInConflict )
{
    const std::string DYNAMIC_MAP_XML = "tests/boost-tests/maps/StableConflicts.xml";
    const std::string STATIC_MAP_XML = "tmp/StableConflicts.compiled.xml";
    const unsigned int ZOOM_INDEX_WITH_CONFLICT = 17;
    const unsigned int ZOOM_INDEX_NO_CONFLICT = 18;

    core::IMapGuiPtr dynamicMapGui = test::map::openMap(DYNAMIC_MAP_XML);

    LabelingSettings settings;
    settings.setForCompilation();

    auto& map = dynamicMapGui->map();
    LabelGenerator lg(map, map.labelableLayers(), settings);
    const base::BoxD mapExtent = map.getExtent();

    // check that both features are labelable with different text
    lg.placeLabels(test::map::createProgressStub(), mapExtent, ZOOM_INDEX_NO_CONFLICT);
    {
        auto allLabels = collectLabels(map, mapExtent);
        BOOST_REQUIRE_EQUAL(allLabels.size(), 2);
        BOOST_REQUIRE(allLabels[0] != allLabels[1]);
    }

    // label dynamic map and remember it's texts
    lg.placeLabels(test::map::createProgressStub(), mapExtent, ZOOM_INDEX_WITH_CONFLICT);
    auto dynamicLabels = collectLabels(map, mapExtent);
    BOOST_CHECK_EQUAL(dynamicLabels.size(), 1);

    mapcompiler::compile(map, STATIC_MAP_XML, mapcompiler::Options(),
        {ZOOM_INDEX_WITH_CONFLICT}, map.locales(), test::map::createProgressStub());

    // open static map
    core::Map staticMap(core::MapMode::Static);
    staticMap.env().stylesLibrary.reset(new core::StylesLibrary(""));
    staticMap.loadFromXml(STATIC_MAP_XML, true);
    staticMap.open(test::map::createProgressStub());

    // collect static labels and compare with dynamic
    auto staticLabels = collectLabels(staticMap, staticMap.getExtent());
    BOOST_CHECK_EQUAL(staticLabels.size(), 1);
    BOOST_CHECK(dynamicLabels == staticLabels);
}

BOOST_AUTO_TEST_SUITE_END()
