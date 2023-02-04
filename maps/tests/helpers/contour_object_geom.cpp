#include <maps/wikimap/mapspro/libs/views/tests/helpers/contour_object_geom.h>

#include <maps/libs/geolib/include/serialization.h>


namespace maps::wiki::views::tests::contour_object_geom {

ContourObjectGeom::ContourObjectGeom(geolib3::Polygon2 coords)
    : geometry_(std::move(coords))
{}

void ContourObjectGeom::insert(pqxx::transaction_base& txn)
{
    txn.exec(
        "INSERT INTO vrevisions_trunk.contour_objects_geom VALUES (" +
        std::to_string(objectId_) + "," +
        std::to_string(commitId_) + "," +
        "ST_GeomFromEWKT('SRID=3395;" + geolib3::WKT::toString(geometry_) + "')," +
        std::to_string(zmin_) + "," +
        std::to_string(zmax_) + "," +
        toString(txn, domainAttrs_) + "," +
        toString(txn, serviceAttrs_) + "," +
        std::to_string(geometry_.area()) + ")"
    );
}

ContourObjectGeom& ContourObjectGeom::objectId(size_t newValue)
{
    objectId_ = newValue;
    return *this;
}

ContourObjectGeom& ContourObjectGeom::commitId(size_t newValue)
{
    commitId_ = newValue;
    return *this;
}

ContourObjectGeom& ContourObjectGeom::zmin(size_t newValue)
{
    zmin_ = newValue;
    return *this;
}

ContourObjectGeom& ContourObjectGeom::zmax(size_t newValue)
{
    zmax_ = newValue;
    return *this;
}

ContourObjectGeom& ContourObjectGeom::domainAttrs(common::Attributes newValue)
{
    domainAttrs_ = std::move(newValue);
    return *this;
}

ContourObjectGeom& ContourObjectGeom::serviceAttrs(common::Attributes newValue)
{
    serviceAttrs_ = std::move(newValue);
    return *this;
}

ContourObjectGeom& ContourObjectGeom::partOfRegion(size_t region)
{
    serviceAttrs_.emplace(SRV_IS_PART_OF_REGION, "1");
    serviceAttrs_.emplace(srvIsPartOfRegionAttrName(region), "1");
    return *this;
}

std::string ContourObjectGeom::toString(
    pqxx::transaction_base& txn, const common::Attributes& attrs) const
{
    const auto result = common::attributesToHstore(txn, attrs);
    return result.empty() ? "''::hstore" : result;
}

std::string srvIsPartOfRegionAttrName(size_t region)
{
    return SRV_IS_PART_OF_REGION + "_" + std::to_string(region);
}

} // namespace maps::wiki::views::tests::contour_objects_geom
