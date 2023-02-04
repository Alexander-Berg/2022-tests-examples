#include "data_set_builder.h"

#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/common/include/file_utils.h>

#include <boost/filesystem.hpp>
#include <fstream>

namespace maps::carparks::renderer::tests {

namespace {

using maps::renderer::base::PointD;

const std::vector<std::string> POINT_LAYERS = {
    "points", "markers", "shields", "dynamic_markers"};

struct ChangeCurrentDir {
    ChangeCurrentDir()
    {
        boost::filesystem::current_path(DATA_DIR);
    }
};
const ChangeCurrentDir CHANGE_DIR;

PointD toMercator(
    const geolib3::Point2& p)
{
    const geolib3::Point2 merc = geolib3::geoPoint2Mercator(p);
    return {merc.x(), merc.y()};
}

void addLine(
    const std::vector<geolib3::Point2>& lineGeo,
    maps::renderer::base::Vertices& dst)
{
    for (auto pIt = lineGeo.begin(); pIt != lineGeo.end(); ++pIt) {
        const auto mercPt = toMercator(*pIt);

        if (pIt == lineGeo.begin())
            dst.addMoveTo(mercPt);
        else
            dst.addLineTo(mercPt);
    }
}

} // namespace


AttrsBuilder::AttrsBuilder(const std::string& path)
    : path_(path)
{}

void AttrsBuilder::add(const common2::CarparkInfo<mms::Standalone>& info)
{
    infos_.emplace(info.id(), info);
}

void AttrsBuilder::finalize()
{
    std::ofstream out(path_);
    mms::write(out, infos_);
}

LayerBuilder::LayerBuilder(
    ft::FeatureType ftType,
    const std::string& path,
    const std::string& layerName)
    : attrsBuilder_(common::joinPath(path, layerName + ".info.mms"))
    , storageBuilder_(ftType,
                      common::joinPath(path, layerName + ".geometry"),
                      common::joinPath(path, layerName + ".index"))
{}

void LayerBuilder::addFeature(
    const ft::Feature& feature,
    const common2::CarparkInfo<mms::Standalone>& info)
{
    storageBuilder_.addFeature(feature);
    attrsBuilder_.add(info);
}

void LayerBuilder::finalize()
{
    attrsBuilder_.finalize();
    storageBuilder_.finalize();
}

DataSetBuilder::DataSetBuilder()
    : polylinesBuilder_(ft::FeatureType::Polyline, tempDir_.path(), "polylines")
    , polygonsBuilder_(ft::FeatureType::Polygon, tempDir_.path(), "polygons")
{
    for (const auto& layer : POINT_LAYERS)
        pointsBuilders_.emplace(
            layer, LayerBuilder(ft::FeatureType::Point, tempDir_.path(), layer));
}

void DataSetBuilder::addPoint(
    const geolib3::Point2& pointGeo,
    const common2::CarparkInfo<mms::Standalone>& info,
    const std::string& layer)
{
    auto builderIt = pointsBuilders_.find(layer);
    REQUIRE(builderIt != pointsBuilders_.end(), "Unknown layer: " + layer);

    ft::Feature feature(ft::FeatureType::Point);
    feature.setSourceId(info.id());
    feature.geom().shapes().addGeomPoint(toMercator(pointGeo));

    builderIt->second.addFeature(feature, info);
}

void DataSetBuilder::addPolyline(
    const std::vector<geolib3::Point2>& polylineGeo,
    const common2::CarparkInfo<mms::Standalone>& info)
{
    ft::Feature feature(ft::FeatureType::Polyline);

    addLine(polylineGeo, feature.geom().contours());

    feature.setSourceId(info.id());
    polylinesBuilder_.addFeature(feature, info);
}

void DataSetBuilder::addPolygon(
    const std::vector<std::vector<geolib3::Point2>>& polygonGeo,
    const common2::CarparkInfo<mms::Standalone>& info)
{
    ft::Feature feature(ft::FeatureType::Polygon);

    std::vector<std::vector<PointD>> polygonMerc;
    for (const auto& ring : polygonGeo) {
        addLine(ring, feature.geom().shapes());
        feature.geom().shapes().addEndPoly();
    }

    feature.setSourceId(info.id());
    polygonsBuilder_.addFeature(feature, info);
}

void DataSetBuilder::finalize()
{
    for (auto& [layer, pointBuilder] : pointsBuilders_)
        pointBuilder.finalize();

    polylinesBuilder_.finalize();
    polygonsBuilder_.finalize();
}

common2::CarparkInfo<mms::Standalone> buildInfo(
    common2::Id id,
    common2::CarparkType type,
    const std::string& isocode,
    const std::string& orgId,
    const std::string& price)
{
    return {id, type, isocode, orgId, price};
}

maps::renderer::base::BoxD toQueryBbox(
    const geolib3::Point2& p1Geo,
    const geolib3::Point2& p2Geo)
{
    const maps::renderer::base::PointD p1Merc = toMercator(p1Geo);
    const maps::renderer::base::PointD p2Merc = toMercator(p2Geo);
    return {p1Merc.x, p1Merc.y, p2Merc.x, p2Merc.y};
}

} // namespace maps::carparks::renderer::tests
