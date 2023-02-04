#include "../include/MapValidationTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

#include <cpl_error.h>

using namespace boost::unit_test;
using namespace maps::renderer5;
using namespace maps::renderer;
using namespace maps::renderer5::test;

namespace
{
    const char* origMapValidationMifFile = "tests/boost-tests/data/TwoPlineTest.mif";
    const char* origMapValidationMidFile = "tests/boost-tests/data/TwoPlineTest.mid";
    const char* mapValidationMifFile = "tmp/MapValidation.mif";
    const char* mapValidationMidFile = "tmp/MapValidation.mid";
    const char* mapValidationXmlFile = "tests/boost-tests/maps/MapValidation.xml";
}

core::IMapGuiPtr getOpenedTestMap()
{
    io::dir::create(io::tempDirPath());

    io::file::write(io::file::open(origMapValidationMifFile), mapValidationMifFile);
    io::file::write(io::file::open(origMapValidationMidFile), mapValidationMidFile);

    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(mapValidationXmlFile, true);

    mapGui->open(map::createProgressStub());

    return mapGui;
}

void map::dynamicGeometryFCValidationTest()
{
    map::deleteFilesFromTmpDir();

    core::IMapGuiPtr map = getOpenedTestMap();

    bool validateResult = false;

    validateResult = map->validate();

    BOOST_CHECK(validateResult);
    BOOST_CHECK(map->errorMessages().size() == 0);

    io::file::remove(mapValidationMidFile);

    CPLPushErrorHandler(CPLQuietErrorHandler);
    validateResult = map->validate();
    CPLPopErrorHandler();

    BOOST_CHECK(!validateResult);
    BOOST_CHECK(map->errorMessages().size() == 1);

    // second validate.
    //
    CPLPushErrorHandler(CPLQuietErrorHandler);
    validateResult = map->validate();
    CPLPopErrorHandler();

    BOOST_CHECK(!validateResult);
    BOOST_CHECK(map->errorMessages().size() == 1);

    io::file::write(io::file::open(origMapValidationMidFile), mapValidationMidFile);

    validateResult = map->validate();

    BOOST_CHECK(validateResult);
    BOOST_CHECK(map->errorMessages().size() == 0);

    io::file::remove(mapValidationMifFile);

    validateResult = map->validate();

    BOOST_CHECK(!validateResult);

    map->close();

    map::deleteFilesFromTmpDir();
}

test_suite * map::initValidationSuite()
{
    test_suite * suite = BOOST_TEST_SUITE("Map validation test suite");

    suite->add(
        BOOST_TEST_CASE(&map::dynamicGeometryFCValidationTest));

    return suite;
}
