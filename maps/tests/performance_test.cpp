#include <yandex/maps/coverage5/coverage.h>
#include <yandex/maps/coverage5/layer.h>

#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/test_tools/test_tools.h>
#include <maps/libs/geolib/include/test_tools/random_geometry_factory.h>
#include <maps/libs/geolib/include/test_tools/pseudo_random.h>

#include <maps/libs/common/include/profiletimer.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>

#include <iostream>

using namespace maps::coverage5;
using namespace maps::geolib3;

BOOST_AUTO_TEST_CASE(check_regions_by_point_performance)
{
    boost::unit_test::unit_test_log.set_threshold_level(
        boost::unit_test::log_messages);

    const unsigned long TEST_COUNT = 20000000ul;
    test_tools::PseudoRandom random;
    test_tools::RandomGeometryFactory factory(
        BoundingBox(Point2(30, 40), Point2(90, 70)),
        &random);

    Coverage cov("./mms/libcov_geoid.mms.1");

    ProfileTimer timer;
    for (unsigned long i = 0; i < TEST_COUNT; ++i) {
        Regions r = cov["geoid"].regions(
            factory.getPoint(), boost::optional<Zoom>());
    }

    double elapsedTime;
    std::istringstream(timer.getElapsedTime()) >> elapsedTime;

    BOOST_MESSAGE("Calling regions(Point) " << TEST_COUNT <<
        " times takes " << elapsedTime << " seconds");

    BOOST_MESSAGE("Calling regions(Point) takes " <<
        elapsedTime / (double)TEST_COUNT << " seconds in average");

    BOOST_CHECK_MESSAGE(TEST_COUNT * 2e-4 > elapsedTime,
        "Calling regions(Point) takes more than " << 2e-4 << " time ");
}

BOOST_AUTO_TEST_CASE(check_min_area_region_performance)
{
    boost::unit_test::unit_test_log.set_threshold_level(
        boost::unit_test::log_messages);

    const unsigned long TEST_COUNT = 20000000ul;
    test_tools::PseudoRandom random;
    test_tools::RandomGeometryFactory factory(
        BoundingBox(Point2(30, 40), Point2(90, 70)),
        &random);

    Coverage cov("./mms/libcov_geoid.mms.1");

    ProfileTimer timer;
    for (unsigned long i = 0; i < TEST_COUNT; ++i) {
        boost::optional<Region> r = cov["geoid"].minAreaRegion(
            factory.getPoint(), boost::optional<Zoom>());
    }

    double elapsedTime;
    std::istringstream(timer.getElapsedTime()) >> elapsedTime;

    BOOST_MESSAGE("Calling minAreaRegion(Point) " << TEST_COUNT <<
        " times takes " << elapsedTime << " seconds");

    BOOST_MESSAGE("Calling minAreaRegion(Point) takes " <<
        elapsedTime / (double)TEST_COUNT << " seconds in average");

    BOOST_CHECK_MESSAGE(TEST_COUNT * 2e-4 > elapsedTime,
        "Calling minAreaRegion(Point) takes more than " << 2e-4 << " time ");
}

BOOST_AUTO_TEST_CASE(check_regions_by_bbox_performance_small)
{
    boost::unit_test::unit_test_log.set_threshold_level(
        boost::unit_test::log_messages);

    const unsigned long TEST_COUNT = 1000000ul;
    test_tools::PseudoRandom random;
    test_tools::RandomGeometryFactory factory(
         BoundingBox(Point2(30, 40), Point2(90, 70)), &random);

    Coverage cov("/usr/share/yandex/maps/coverage5/sat.mms.1");

    double avgSize = 0.;
    ProfileTimer timer;
    for (unsigned long i = 0; i < TEST_COUNT; ++i) {
        const Point2& point = factory.getPoint();
        BoundingBox box(
            Point2(point.x() - 10, point.y() - 8),
            Point2(point.x() + 10, point.y() + 8));
        Regions r = cov["sat"].regions(box, boost::none);
        avgSize += r.size() / (double)TEST_COUNT;
    }

    double elapsedTime;
    std::istringstream(timer.getElapsedTime()) >> elapsedTime;

    BOOST_MESSAGE("Calling regions(BoundingBox) " << TEST_COUNT <<
        " times takes " << elapsedTime << " seconds");

    BOOST_MESSAGE("Calling regions(BoundingBox) takes " <<
        elapsedTime / (double)TEST_COUNT << " seconds in average");

    BOOST_MESSAGE("Average answer size is " << avgSize << " regions");
}
