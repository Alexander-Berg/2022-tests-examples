#include "../geom.h"
#include <maps/libs/geolib/include/polyline.h>
#include <yandex/maps/common/encoder.h>
#include <yandex/maps/log/log.h>

#include <geos/io/WKBReader.h>
#include <geos/io/WKBWriter.h>
#include <geos/geom/Point.h>
#include <fstream>

#define BOOST_TEST_MAIN
#define BOOST_TEST_DYN_LINK
#include <boost/test/unit_test.hpp>

BOOST_AUTO_TEST_CASE(test_geos_buffer)
{
    geos::geom::Coordinate ptCoord(10, 10);
    geos::geom::Point* geosPt = 
        geos::geom::GeometryFactory::getDefaultInstance()->createPoint(ptCoord);
    maps::wiki::Geom center(geosPt);
    maps::wiki::Geom buffer = center.createBuffer(5.0);
    std::ostringstream s;
    maps::wiki::OutputContext stdcont(s);
    center.coordinatesJson(stdcont);
    buffer.coordinatesJson(stdcont);
    BOOST_CHECK_EQUAL(s.str(), "[[10,10]][[15,15],[15,5],[5,5],[5,15],[15,15]]");
}

BOOST_AUTO_TEST_CASE(test_geos)
{
    //create from poslist
    std::string poly = "<gml:Polygon><gml:exterior><gml:LinearRing><gml:posList>37 55 38 55 38 54 37 54 37 55</gml:posList></gml:LinearRing></gml:exterior><gml:interior><gml:LinearRing><gml:posList>37 55 38 55 38 54 37 54 37 55</gml:posList></gml:LinearRing></gml:interior></gml:Polygon>";
    std::string poslist = "37.0 55.0 38.0 55.0 38.0 54.0 37.0 54.0 37.0 55.0";
    maps::wiki::StringVec holes;
    holes.push_back(poslist);
    maps::wiki::GeometryPtr g = maps::wiki::createPolygon(poslist, holes);
    std::stringstream bs;
    geos::io::WKBWriter wkbw;
    wkbw.write(*g, bs);
    geos::geom::GeometryFactory f;
    geos::io::WKBReader wkbr(f);
    maps::wiki::Geom g1(wkbr.read(bs));
    maps::wiki::StringOutputContext os;
    g1.gml(os);
    BOOST_CHECK_EQUAL(os.str(), poly);
}

BOOST_AUTO_TEST_CASE(test_base64Geom)
{
    std::string poslist = "37.0 55.0 38.0 55.0 38.0 54.0 37.0 54.0 37.0 55.0";
    maps::wiki::Geom g(maps::wiki::createPolygon(poslist, maps::wiki::StringVec()));
    std::string originalWkb = g.wkb();
    std::string encodedWkb  = g.base64Wkb();
    std::string wkbDecoded;
    ytl::base64safe::decode(encodedWkb.begin(), encodedWkb.end(), wkbDecoded);
    BOOST_CHECK_EQUAL(originalWkb, wkbDecoded);
}

BOOST_AUTO_TEST_CASE(test_geomread)
{
    try {
        geos::geom::GeometryFactory f;
        geos::io::WKBReader wkbr(f);

        std::string etalon = "<gml:Polygon><gml:exterior><gml:LinearRing><gml:posList>0.001581117 0.002155636 0.001715799 0.002083226 0.001859648 0.002454028 0.001679837 0.002472248 0.001581117 0.002155636</gml:posList></gml:LinearRing></gml:exterior></gml:Polygon>";
        
        for(int x = 0; x < 1000; x++){
            std::ifstream streamGeom("tests/data/test_streamGeom.bin", std::ios::in | std::ios::binary );
            maps::wiki::Geom geom(wkbr.read(streamGeom));
            maps::wiki::StringOutputContext os;
            os.stream().precision(9);
            os.stream() << std::fixed;
            geom.gml(os);
            BOOST_CHECK_EQUAL(os.str(), etalon);
        }
    } catch(const std::exception& ex) {
        BOOST_CHECK(false);
        std::cout << ex.what() << std::endl;
    }
}


BOOST_AUTO_TEST_CASE(test_geomInsertPoint)
{
    maps::log::init("ERROR");
    const std::string lineText = "0 0 1 2 2 4 3 2 4 0";
    const std::string pointText = "3 2";
    maps::wiki::Geom lineString(maps::wiki::createPolyline(lineText));
    maps::wiki::Geom point(maps::wiki::createPoint(pointText));
    BOOST_CHECK_EQUAL(lineString->getNumPoints(), 5);
    std::pair<size_t, bool> position = lineString.insertPoint(point->getCoordinate()->x, point->getCoordinate()->y, 0.000000001);
    BOOST_CHECK_EQUAL(position.first, 3);
    BOOST_CHECK_EQUAL(position.second, false);
}

BOOST_AUTO_TEST_CASE(test_emptyPoslist)
{
    BOOST_CHECK(!maps::wiki::createPolygon("", {}));
    BOOST_CHECK(!maps::wiki::createPolygon(
        "0 0 10 10 20 10 0 0",
        {"1 1 2 2 3 2 1 1", "", "2 2 3 3 4 3 2 2"}));
}
