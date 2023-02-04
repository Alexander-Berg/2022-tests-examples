#include <maps/carparks/libs/geometry/geometry.h>

#include <yandex/maps/carparks/geometry/build.h>

#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/polygon.h>

#include <boost/filesystem.hpp>


using namespace maps::carparks::geometry;
using namespace maps::geolib3;

PointsVector carparkPoints{
    Point2(37.59352773427963, 55.73538584807276),
    Point2(37.59377986192703, 55.735512698442484),
    Point2(37.592004239559174, 55.735467394786355),
    Point2(37.59216517210007, 55.73476367124215)};

PointsVector carparkLine1{
    Point2(37.59204179048538, 55.73535564554304),
    Point2(37.59241193532944, 55.73527711885633),
    Point2(37.59231001138687, 55.73515328799092),
    Point2(37.59185940027237, 55.73522879466291)};

// To test that point is not snapped to the segment
// from last to first line point
PointsVector carparkLine2{
        Point2(47, 65),
        Point2(47.001, 65),
        Point2(47.001, 65.001),
        Point2(47, 65.001)};

PointsVector carparkPolygon1{
    Point2(37.593989074230194, 55.735340544269384),
    Point2(37.593774497509, 55.73499321336328),
    Point2(37.5935760140419, 55.73467306213855),
    Point2(37.593479454517365, 55.734552249673214),
    Point2(37.59286791086197, 55.73465494029257)};

PointsVector carparkPolygon2_with_duplicate_last_vertex{
    Point2(37.590974271297455, 55.73537074681084),
    Point2(37.59118348360062, 55.73523483519036),
    Point2(37.590947449207306, 55.73521671360521),
    Point2(37.59081870317459, 55.73529826067213),
    Point2(37.590974271297455, 55.73537074681084)};

PointsVector carparkPolygon3{
        Point2(47, 55),
        Point2(47.001, 55),
        Point2(47.001, 55.001),
        Point2(47, 55.001)};

template<typename T>
std::string asEkb(const T& geometry)
{
    std::ostringstream s;
    maps::geolib3::WKB::write(geometry, s);
    return s.str();
}

std::vector<CarparkGeometrySource> populateGeoms()
{
    using namespace maps::carparks::common2;

    std::vector<CarparkGeometrySource> result;

    int id = 0;
    for (const auto& p : carparkPoints) {
        result.push_back(
            {CarparkInfo<mms::Standalone>(id++, CarparkType::Lot, "", "", ""),
             asEkb(p)});
    }

    result.push_back(
        {CarparkInfo<mms::Standalone>(id++, CarparkType::Lot, "", "", ""),
         asEkb(maps::geolib3::Polyline2(carparkLine1))});
    result.push_back(
            {CarparkInfo<mms::Standalone>(id++, CarparkType::Lot, "", "", ""),
             asEkb(maps::geolib3::Polyline2(carparkLine2))});
    result.push_back(
        {CarparkInfo<mms::Standalone>(id++, CarparkType::Lot, "", "", ""),
         asEkb(maps::geolib3::Polygon2(carparkPolygon1))});
    result.push_back(
        {CarparkInfo<mms::Standalone>(id++, CarparkType::Lot, "", "", ""),
         asEkb(maps::geolib3::Polygon2(carparkPolygon2_with_duplicate_last_vertex))});
    result.push_back(
        {CarparkInfo<mms::Standalone>(id++, CarparkType::Lot, "", "", ""),
         asEkb(maps::geolib3::Polygon2(carparkPolygon3))});

    return result;
}

int main(int /*argc*/, char** argv)
{
    boost::filesystem::create_directory(argv[1]);
    buildIndex(populateGeoms(), argv[1]);
}
