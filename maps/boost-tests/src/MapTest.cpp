#include "../include/MapTest.h"

#include "../include/MapDestroyTest.h"
#include "../include/MapValidationTest.h"
#include "../include/MapLayersAccessTest.h"
#include "../include/GetSourceNameTest.h"
#include "../include/MapFeaturesSelectionTest.h"
#include "../include/PathResolverTest.h"
#include "../include/RawFeatureDataTest.h"

using namespace maps::renderer5;
using namespace boost::unit_test;

test_suite * test::map::init_suite()
{
    test_suite * suite = BOOST_TEST_SUITE("Map test suite");

    suite->add(test::map::initMapDestroySuite());
    suite->add(test::map::initMapLayersAccessSuite());
    suite->add(test::map::initValidationSuite());
    suite->add(test::map::initMapFeaturesSelectionTestSuite());
    suite->add(test::map::initPathResolverTestSuite());

    suite->add(BOOST_TEST_CASE(&map::rawFeatureDataFromFileTest));
    suite->add(BOOST_TEST_CASE(&map::getFileSourceNameTest));

    return suite;
}
