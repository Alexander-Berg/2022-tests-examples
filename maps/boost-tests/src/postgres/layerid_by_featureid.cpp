#include "tests/boost-tests/include/tools/map_tools.h"
#include "../../include/contexts.hpp"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/labeler/find_layers_by_features.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::postgres;
using namespace maps::renderer5::test;
using namespace maps::renderer;

namespace {
const std::string MAP_XML_FILE_NAME = "tests/boost-tests/maps/LayerIdByFeatureId.xml";

std::vector<unsigned int> getIds(const LayersCollection& layers)
{
    std::vector<unsigned int> ids;
    ids.reserve(layers.size());
    for (const auto& layer : layers)
        ids.push_back(layer->id());
    std::sort(ids.begin(), ids.end());
    return ids;
}
}

BOOST_FIXTURE_TEST_CASE( findLayersByFeaturesTest, TransactionProviderContext<> )
{
    typedef std::vector<FeatureIdType> FeatureIds;
    const double EPS = 0.00001;

    core::IMapGuiPtr mapGui = map::createTestMapGui();
    core::Map& map = mapGui->map();

    map.setPostgresTransactionProvider(provider);

    BOOST_REQUIRE_NO_THROW(map.loadFromXml(MAP_XML_FILE_NAME, true));
    BOOST_REQUIRE_NO_THROW(map.open());
    BOOST_REQUIRE(map.valid());

    LayersCollection labelableLayers = map.labelableLayers();
    const unsigned int zoomIndex = 15;

    std::vector<unsigned int> zoom14LayerIds = { 32, 34, 35 };
    std::vector<unsigned int> zoom15LayerIds = { 32, 33, 34, 35 };
    std::vector<unsigned int> zoom16LayerIds = { 34, 35, 36 };

    std::vector<labeler::FeaturesToLabel> featuresToLabel;
    std::vector<labeler::FeaturesToLabel::LayerFeatures> features;

    featuresToLabel = labeler::findLayersByFeatures(
        labelableLayers, {zoomIndex}, FeatureIds{3766699}, providerEx);
    BOOST_REQUIRE_EQUAL(featuresToLabel.size(), 1);
    BOOST_CHECK_EQUAL(featuresToLabel[0].zoom, zoomIndex);
    features = featuresToLabel[0].features;
    BOOST_REQUIRE_EQUAL(features.size(), 1);
    BOOST_CHECK_EQUAL(features[0].layer->id(), 32);
    BOOST_CHECK(features[0].ids == FeatureIds{3766699});
    const base::BoxD box3766699(4168904.25, 7458999, 4169329.5, 7459322);
    base::BoxD box = featuresToLabel[0].box;
    BOOST_CHECK_CLOSE(box.x1, box3766699.x1, EPS);
    BOOST_CHECK_CLOSE(box.y1, box3766699.y1, EPS);
    BOOST_CHECK_CLOSE(box.x2, box3766699.x2, EPS);
    BOOST_CHECK_CLOSE(box.y2, box3766699.y2, EPS);
    BOOST_CHECK(getIds(featuresToLabel[0].layers) == zoom15LayerIds);

    featuresToLabel = labeler::findLayersByFeatures(
        labelableLayers, {zoomIndex}, FeatureIds{3771056}, providerEx);
    BOOST_REQUIRE_EQUAL(featuresToLabel.size(), 1);
    BOOST_CHECK_EQUAL(featuresToLabel[0].zoom, zoomIndex);
    features = featuresToLabel[0].features;
    BOOST_REQUIRE_EQUAL(features.size(), 1);
    BOOST_CHECK_EQUAL(features[0].layer->id(), 33);
    BOOST_CHECK(features[0].ids == FeatureIds{3771056});
    const base::BoxD box3771056(4168767.25, 7459518.5, 4169075.5, 7459789);
    box = featuresToLabel[0].box;
    BOOST_CHECK_CLOSE(box.x1, box3771056.x1, EPS);
    BOOST_CHECK_CLOSE(box.y1, box3771056.y1, EPS);
    BOOST_CHECK_CLOSE(box.x2, box3771056.x2, EPS);
    BOOST_CHECK_CLOSE(box.y2, box3771056.y2, EPS);
    BOOST_CHECK(getIds(featuresToLabel[0].layers) == zoom15LayerIds);

    featuresToLabel = labeler::findLayersByFeatures(
        labelableLayers, {zoomIndex}, FeatureIds{3771057}, providerEx);
    BOOST_CHECK(featuresToLabel.empty());

    featuresToLabel = labeler::findLayersByFeatures(
        labelableLayers, {zoomIndex}, FeatureIds{3800139}, providerEx);
    BOOST_REQUIRE_EQUAL(featuresToLabel.size(), 1);
    BOOST_CHECK_EQUAL(featuresToLabel[0].zoom, zoomIndex);
    features = featuresToLabel[0].features;
    BOOST_REQUIRE_EQUAL(features.size(), 2);
    if (features[0].layer->id() > features[1].layer->id())
        std::swap(features[0], features[1]);
    BOOST_CHECK_EQUAL(features[0].layer->id(), 34);
    BOOST_CHECK_EQUAL(features[1].layer->id(), 35);
    BOOST_CHECK(features[0].ids == FeatureIds{3800139});
    BOOST_CHECK(features[1].ids == FeatureIds{3800139});
    const base::BoxD box3800139(4168821.25, 7459217.5, 4168904.5, 7459322);
    box = featuresToLabel[0].box;
    BOOST_CHECK_CLOSE(box.x1, box3800139.x1, EPS);
    BOOST_CHECK_CLOSE(box.y1, box3800139.y1, EPS);
    BOOST_CHECK_CLOSE(box.x2, box3800139.x2, EPS);
    BOOST_CHECK_CLOSE(box.y2, box3800139.y2, EPS);
    BOOST_CHECK(getIds(featuresToLabel[0].layers) == zoom15LayerIds);

    featuresToLabel = labeler::findLayersByFeatures(
        labelableLayers, map.zoomIndexes(), FeatureIds{3766699, 3771056, 1234567}, providerEx);
    std::sort(featuresToLabel.begin(), featuresToLabel.end(),
        [](const labeler::FeaturesToLabel& a, const labeler::FeaturesToLabel& b) {
            return a.zoom < b.zoom;
        });
    BOOST_REQUIRE_EQUAL(featuresToLabel.size(), 3);
    BOOST_CHECK_EQUAL(featuresToLabel[0].zoom, 14);
    BOOST_CHECK_EQUAL(featuresToLabel[1].zoom, 15);
    BOOST_CHECK_EQUAL(featuresToLabel[2].zoom, 16);

    features = featuresToLabel[0].features;
    BOOST_REQUIRE_EQUAL(features.size(), 1);
    BOOST_CHECK_EQUAL(features[0].layer ->id(), 32);
    BOOST_CHECK(features[0].ids == FeatureIds{3766699});
    box = featuresToLabel[0].box;
    BOOST_CHECK_CLOSE(box.x1, box3766699.x1, EPS);
    BOOST_CHECK_CLOSE(box.y1, box3766699.y1, EPS);
    BOOST_CHECK_CLOSE(box.x2, box3766699.x2, EPS);
    BOOST_CHECK_CLOSE(box.y2, box3766699.y2, EPS);
    BOOST_CHECK(getIds(featuresToLabel[0].layers) == zoom14LayerIds);

    features = featuresToLabel[1].features;
    BOOST_REQUIRE_EQUAL(features.size(), 2);
    if (features[0].layer ->id() > features[1].layer->id())
        std::swap(features[0], features[1]);
    BOOST_CHECK_EQUAL(features[0].layer->id(), 32);
    BOOST_CHECK_EQUAL(features[1].layer->id(), 33);
    BOOST_CHECK(features[0].ids == FeatureIds{3766699});
    BOOST_CHECK(features[1].ids == FeatureIds{3771056});
    base::BoxD box3766699_3771056(box3766699);
    box3766699_3771056.enlarge(box3771056);
    box = featuresToLabel[1].box;
    BOOST_CHECK_CLOSE(box.x1, box3766699_3771056.x1, EPS);
    BOOST_CHECK_CLOSE(box.y1, box3766699_3771056.y1, EPS);
    BOOST_CHECK_CLOSE(box.x2, box3766699_3771056.x2, EPS);
    BOOST_CHECK_CLOSE(box.y2, box3766699_3771056.y2, EPS);
    BOOST_CHECK(getIds(featuresToLabel[1].layers) == zoom15LayerIds);

    features = featuresToLabel[2].features;
    BOOST_REQUIRE_EQUAL(features.size(), 1);
    BOOST_CHECK_EQUAL(features[0].layer->id(), 36);
    BOOST_CHECK(features[0].ids == FeatureIds{3766699});
    box = featuresToLabel[2].box;
    BOOST_CHECK_CLOSE(box.x1, box3766699.x1, EPS);
    BOOST_CHECK_CLOSE(box.y1, box3766699.y1, EPS);
    BOOST_CHECK_CLOSE(box.x2, box3766699.x2, EPS);
    BOOST_CHECK_CLOSE(box.y2, box3766699.y2, EPS);
    BOOST_CHECK(getIds(featuresToLabel[2].layers) == zoom16LayerIds);
}

