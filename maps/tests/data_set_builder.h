#pragma once

#include <maps/carparks/renderer/yacare/lib/common.h>
#include <maps/carparks/renderer/yacare/lib/data_sets.h>

#include <yandex/maps/carparks/common2/carpark_info.h>
#include <yandex/maps/carparks/common2/info_collection.h>
#include <yandex/maps/renderer/feature/feature.h>
#include <maps/renderer/libs/data_sets/gms_builder/include/gms_builder.h>
#include <maps/libs/common/include/temporary_dir.h>

#include <library/cpp/testing/unittest/env.h>

#include <unordered_map>

namespace maps::carparks::renderer::tests {

namespace gms = maps::renderer::gms_builder;
namespace ft = maps::renderer::feature;

inline const std::string DATA_DIR =
    BuildRoot() + "/maps/carparks/renderer/yacare/lib/tests/data/";

class AttrsBuilder {
public:
    AttrsBuilder(const std::string& path);
    void add(const common2::CarparkInfo<mms::Standalone>& info);
    void finalize();
private:
    const std::string path_;
    common2::InfoCollection<mms::Standalone, common2::CarparkInfo> infos_;
};

class LayerBuilder {
public:
    LayerBuilder(
        ft::FeatureType ftType,
        const std::string& path,
        const std::string& layerName);

    void addFeature(
        const ft::Feature& feature,
        const common2::CarparkInfo<mms::Standalone>& info);

    void finalize();
private:
    AttrsBuilder attrsBuilder_;
    gms::GeometryStorageBuilder storageBuilder_;
};

class DataSetBuilder {
public:
    DataSetBuilder();

    void addPoint(
        const geolib3::Point2& pointGeo,
        const common2::CarparkInfo<mms::Standalone>& info,
        const std::string& layer = "points");

    void addPolyline(
        const std::vector<geolib3::Point2>& polylineGeo,
        const common2::CarparkInfo<mms::Standalone>& info);

    void addPolygon(
        const std::vector<std::vector<geolib3::Point2>>& polygonGeo,
        const common2::CarparkInfo<mms::Standalone>& info);

    std::string path() const { return tempDir_.path(); }

    void finalize();

private:
    common::TemporaryDir tempDir_;
    std::unordered_map<std::string, LayerBuilder> pointsBuilders_;

    LayerBuilder polylinesBuilder_;
    LayerBuilder polygonsBuilder_;
};

common2::CarparkInfo<mms::Standalone> buildInfo(
    common2::Id id,
    common2::CarparkType type = common2::CarparkType::Lot,
    const std::string& isocode = "RU",
    const std::string& orgId = "123",
    const std::string& price = "100");

maps::renderer::base::BoxD toQueryBbox(
    const geolib3::Point2& p1Geo,
    const geolib3::Point2& p2Geo);

} // namespace maps::carparks::renderer::tests
