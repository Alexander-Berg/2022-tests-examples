#pragma once

#include "common.h"
#include <yandex/maps/renderer/proj/tile.h>
#include <maps/renderer/libs/data_sets/gms_data_set/include/data_set.h>
#include <maps/renderer/libs/data_sets/gms_data_set/include/storage_info.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::carparks::dump {

namespace gms = maps::renderer::gms_data_set;

template<class ExpectedFeature>
const ExpectedFeature* findFeature(
    const common2::Id& id,
    const std::vector<ExpectedFeature>& expectedFeatures)
{
    for (auto& feature : expectedFeatures) {
        if (feature.id() == id) {
            return &feature;
        }
    }
    return nullptr;
}

template<class ExpectedFeature>
void checkType(const std::string& fileName,
               const std::string& directory,
               ft::FeatureType featureType,
               const std::vector<ExpectedFeature>& expectedFeatures)
{
    std::vector<gms::StorageInfo> layer;
    auto& storage = layer.emplace_back();
    storage.geometryPath = directory + fileName + ".geometry";
    storage.indexPath = directory + fileName + ".index";
    storage.zoomRange = {0, 23};

    gms::DataSet dataSet({{fileName, layer}});

    typename ExpectedFeature::InfoCollectionHolder infos(
        directory + fileName + ".info.mms");

    renderer::data_set::ViewQueryParams params(renderer::proj::EARTH_BOX, {0, 23});
    renderer::data_set::ViewQueryContext ctx(&params);
    auto views = dataSet.queryView(ctx);

    ASSERT_EQ(views.size(), 1u);

    auto ftIterator = views.front().iterator();
    size_t featuresCount = 0;

    while (ftIterator->hasNext()) {
        ++featuresCount;
        const auto& feature = ftIterator->next();

        auto sourceId = feature.sourceId();
        auto* expectedFeature = findFeature(sourceId, expectedFeatures);
        ASSERT_TRUE(expectedFeature != nullptr);

        ASSERT_TRUE(featureGeometryEqualToExpected(
            feature, featureType, expectedFeature->geometry));

        const auto& foundInfo = (*infos)[sourceId];
        ASSERT_EQ(*expectedFeature, ExpectedFeature(foundInfo));

    }

    ASSERT_EQ(featuresCount, expectedFeatures.size());
}

} // namespace maps::carparks::dump
