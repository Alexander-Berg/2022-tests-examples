#include "tests/boost-tests/include/tools/map_tools.h"
#include "tests/boost-tests/include/tools/transform_tools.h"

#include <yandex/maps/renderer5/hotspots/HotspotsRenderer.h>

#include <yandex/maps/renderer5/labeler/label_generator.h>
#include <yandex/maps/renderer5/labeler/LabelingOperation.h>
#include <yandex/maps/renderer5/rasterizer/Rasterizer.h>
#include <yandex/maps/renderer5/core/OperationProgress.h>
#include <yandex/maps/renderer5/core/Map.h>

#include <yandex/maps/renderer/feature/attributes.h>

#include <boost/lexical_cast.hpp>
#include <boost/test/unit_test.hpp>

#include <iostream>
#include <stdlib.h>

namespace ut = boost::unit_test;
using namespace maps;
using namespace maps::renderer5;
using namespace maps::renderer5::labeler;

namespace
{
const char* linesMap = "tests/boost-tests/maps/HotspotsTestMap2.xml";

class GeneratorMock : public maps::hotspots::base5::IGenerator
{
public:
    GeneratorMock()
        : m_settings("{\"enableRenderedGeometry\":true}")
    {
    }

    rapidjson::Value generate(
        const rapidjson::Value& source,
        const std::string& /*locale*/,
        rapidjson::Allocator* alloc) const override
    {
        maps::rjhelper::ValueRef ref(source);
        rapidjson::Value hsMeta;
        rjhelper::ObjectBuilder builder(&hsMeta, alloc);
        builder.Put("HotspotMetaData", [&](rjhelper::ObjectBuilder b) {
            b.Put("id", ref.GetMember<std::string>(renderer::feature::SOURCE_ID_ATTR_NAME));
            b.Put("layer", rand() % 2 ? "events" : "jams");
        });
        return hsMeta;
    }

    const std::string& settings(const std::string&) const override
    {
        return m_settings;
    }

private:
    const std::string m_settings;
};
} // anonymous

BOOST_AUTO_TEST_SUITE( hotspot_tests )

BOOST_AUTO_TEST_CASE( base_test )
{
    core::IMapGuiPtr mapGui = test::map::createTestMapGui();
    mapGui->loadFromXml(linesMap, false);
    core::OperationProgressPtr progress = test::map::createProgressStub();
    mapGui->open(progress);

    LabelGenerator labeler(mapGui->map(), mapGui->map().labelableLayers());
    labeler.placeLabels(test::map::createProgressStub(), mapGui->map().getExtent(), 19);

    core::Map& map = mapGui->map();
    agg::trans_affine mtx = test::mtxFromMerc(250.90075755, 250.46038949, 19);
    GeneratorMock generator;

    renderer5::hotspots::HotspotSearchRequest request;
    request.locale = "ru_RU";
    request.x = 0; // fictive value
    request.y = 0; // fictive value
    request.zoom = 19;

    renderer5::hotspots::HotspotsRenderer ren(request, &generator);

    ren.setClipBox(318, 338);
    rasterizer::Rasterizer rasterizer;
    rasterizer.drawHotspots(ren, progress, 318, 338,
        mtx, map, 0);
}

BOOST_AUTO_TEST_SUITE_END()
