#pragma once

#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/polygon.h>
#include <yandex/maps/wiki/common/pg_utils.h>

#include <pqxx/pqxx>

#include <string>


namespace maps::wiki::views::tests::contour_object_geom {

const std::string SRV_IS_PART_OF_REGION = "srv:is_part_of_region";

const size_t DEFAULT_OBJECT_ID = 1;
const size_t DEFAULT_COMMIT_ID = 1;
const size_t DEFAULT_ZMIN = 0;
const size_t DEFAULT_ZMAX = 0;
const size_t DEFAULT_REGION = 42;
const geolib3::Polygon2 DEFAULT_REGION_GEOMETRY({{48, 48}, {52, 48}, {52, 52}, {48, 52}});
const geolib3::BoundingBox BBOX_INSIDE_DEFAULT_REGION({49, 49}, {51, 51});
const geolib3::BoundingBox BBOX_INTERSECT_DEFAULT_REGION({51, 51}, {53, 53});
const geolib3::BoundingBox BBOX_OUTSIDE_DEFAULT_REGION({53, 53}, {55, 55});
const common::Attributes DEFAULT_DOMAIN_ATTRIBUTES = {{"cat:ad", "1"}};


class ContourObjectGeom {
public:
    ContourObjectGeom(geolib3::Polygon2 coords = DEFAULT_REGION_GEOMETRY);

    void insert(pqxx::transaction_base& txn);

    ContourObjectGeom& objectId(size_t newValue);
    ContourObjectGeom& commitId(size_t newValue);
    ContourObjectGeom& zmin(size_t newValue);
    ContourObjectGeom& zmax(size_t newValue);
    ContourObjectGeom& domainAttrs(common::Attributes newValue);
    ContourObjectGeom& serviceAttrs(common::Attributes newValue);
    ContourObjectGeom& partOfRegion(size_t region);

private:
    geolib3::Polygon2 geometry_;
    size_t objectId_ = DEFAULT_OBJECT_ID;
    size_t commitId_ = DEFAULT_COMMIT_ID;
    size_t zmin_ = DEFAULT_ZMIN;
    size_t zmax_ = DEFAULT_ZMAX;
    common::Attributes domainAttrs_ = DEFAULT_DOMAIN_ATTRIBUTES;
    common::Attributes serviceAttrs_;

    std::string toString(pqxx::transaction_base& txn, const common::Attributes& attrs) const;
};


std::string srvIsPartOfRegionAttrName(size_t region);

} // namespace maps::wiki::views::tests::contour_object_geom
