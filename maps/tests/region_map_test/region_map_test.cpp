#define BOOST_TEST_MAIN

#include <library/cpp/testing/common/env.h>
#include <maps/analyzer/libs/common/include/region_map.h>
#include <maps/libs/geolib/include/point.h>
#include <yandex/maps/coverage5/coverage.h>

#include <boost/test/unit_test.hpp>

#include <boost/test/floating_point_comparison.hpp>
#include <boost/assign/std/vector.hpp>
#include <boost/assign/list_of.hpp>

#include <iostream>

using namespace std;
using namespace boost::assign;
using maps::geolib3::Point2;
namespace geolib = maps::geolib3;
namespace ma = maps::analyzer;

namespace boost {
    template<class T>
    std::ostream& operator << (std::ostream& os,
                               const boost::optional<T>& optional)
    {
        if (!optional) {
            os << "none";
        } else {
            os << *optional;
        }
        return os;
    }
} //namespace boost

class RegionPointChecker {
public:
    /*!
     * Stores reference to given map.
     */
    explicit RegionPointChecker(const ma::RegionMap& regionMap)
        : regionMap_(regionMap)
    {}

    /*!
     * If regions are not set, they are not checked
     */
    void operator() (
        const Point2& point,
        boost::optional<size_t> correctRegion,
        const std::vector<size_t>& correctRegions
    ) const
    {
        const boost::optional<size_t> region = regionMap_.region(point);
        const std::vector<size_t> regions = regionMap_.regions(point);
        BOOST_CHECK_EQUAL(region, correctRegion);
        BOOST_CHECK_EQUAL_COLLECTIONS(
            regions.begin(), regions.end(),
            correctRegions.begin(), correctRegions.end());
    }

private:
    const ma::RegionMap& regionMap_;
};


void testDefaultRegionMap(const RegionPointChecker& checker) {
    // moscow
    checker(Point2(37.614924, 55.75298), 213, list_of(213)(1));

    // moscow obl
    checker(Point2(37.306299, 56.185361), 1, list_of(1));

    // spb
    checker(Point2(30.348842, 59.922735), 2, list_of(2)(10174));

    // ekb
    checker(Point2(60.596572, 56.8220), 54, list_of(54)(11162));

    // n_novgorod
    checker(Point2(43.995539, 56.31316), 47, list_of(47)(11079));

    // Novosib
    checker(Point2(82.94651, 55.020292), 65, list_of(65)(11316));

    // Rostov_On_Don
    checker(Point2(39.717452, 47.22501), 39, list_of(39)(11029));

    // Krasnodar
    checker(Point2(38.976719, 45.02637), 35, list_of(35)(10995));

    // Samara
    checker(Point2(50.103002, 53.19561), 51, list_of(51)(11131));

    // 11119
    checker(Point2(49.138646, 55.7842), 43, list_of(43)(11119));

    // Kharkov
    checker(Point2(36.273572, 49.9922), 147, list_of(147)(20538));

    // Odessa
    checker(Point2(30.726804, 46.4698), 145, list_of(145)(20541));

    // Orel
    checker(Point2(36.054444, 52.96820), 10, list_of(10)(10772));

    // Ufa
    checker(Point2(55.968055, 54.73239), 172, list_of(172)(11111));

    // Chelabinsk
    checker(Point2(61.406719, 55.165152), 56, list_of(56)(11225));

    // Tatarstan
    checker(Point2(52.398899, 55.73952), 236, list_of(236)(11119));

    // Kazan
    checker(Point2(49.112763, 55.798156), 43, list_of(43)(11119));

    // Tula
    checker(Point2(37.611376, 54.19317), 15, list_of(15)(10832));

    // Ryazan
    checker(Point2(39.715767, 54.62023), 11, list_of(11)(10776));

    // Dnepropetrovsk
    checker(Point2(35.010917, 48.45004), 141, list_of(141)(20537));

    // Donetsk
    checker(Point2(37.766474, 47.9877), 142, list_of(142)(20536));

    // Lvov
    checker(Point2(24.010295, 49.83033), 144, list_of(144)(20529));

    // Zaporoj'e
    checker(Point2(35.185939, 47.83949), 960, list_of(960)(20539));

    // Almaty
    checker(Point2(76.925725, 43.27825), 162, list_of(162)(29406));

    // Astana
    checker(Point2(71.452658, 51.16235), 163, list_of(163)(29403));

    // Krasnoyarsk
    checker(Point2(92.859769, 56.01328), 62, list_of(62)(11309));

    // Omsk
    checker(Point2(73.380814, 54.98903), 66, list_of(66)(11318));

    // Poltava
    checker(Point2(34.53795, 49.59151), 964, list_of(964)(20549));

    // Volgograd
    checker(Point2(44.504595, 48.70926), 38, list_of(38)(10950));

    // Yaroslavl
    checker(Point2(39.893894, 57.62412), 16, list_of(16)(10841));

    // Khabarovsk
    checker(Point2(135.083941, 48.47598), 76, list_of(76)(11457));

    // Orenburg
    checker(Point2(55.10971, 51.76954), 48, list_of(48)(11084));

    // Perm
    checker(Point2(56.241077, 58.00003), 50, list_of(50)(11108));

    // Saratov
    checker(Point2(46.023685, 51.53794), 194, list_of(194)(11146));


    // nowhere
    checker(Point2(30.348842, 85.922735), boost::none, std::vector<size_t>());
}


BOOST_AUTO_TEST_CASE(RegionMapTest)
{
    ma::RegionMap regionMap(BinaryPath("maps/analyzer/libs/common/tests/data"));
    testDefaultRegionMap(RegionPointChecker(regionMap));
}


namespace std {

template <typename CharT>
inline std::basic_ostream<CharT>& operator << (std::basic_ostream<CharT>& out, const maps::geolib3::Point2& pt)
{
    return out << "{" << pt.x() << ";" << pt.y() << "}";
}

}


void checkPolylineMiddle(const maps::geolib3::Polyline2& pline, const maps::geolib3::Point2& pt)
{
    const maps::geolib3::Polyline2 plineRev{ maps::geolib3::PointsVector(
        pline.points().rbegin(),
        pline.points().rend()
    )};

    BOOST_CHECK_EQUAL(ma::polylineMiddle(pline), pt);
    BOOST_CHECK_EQUAL(ma::polylineMiddle(pline), ma::polylineMiddle(plineRev));
}


BOOST_AUTO_TEST_CASE(PolylineMiddlePointTest)
{
    checkPolylineMiddle(
        maps::geolib3::Polyline2{ maps::geolib3::PointsVector {
            { 0.0, 0.0 },
            { 10.0, 0.0 },
            { 10.0, 10.0 },
            { 0.0, 10.0 }
        }},
        { 10.0, 5.0 }
    );

    checkPolylineMiddle(
        maps::geolib3::Polyline2{ maps::geolib3::PointsVector{
            { 0.0, 0.0 },
            { 10.0, 0.0 },
            { 10.0, 10.0 }
        }},
        { 10.0, 0.0 }
    );
}
