#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/types/include/vehicle_id.h>

#include <boost/test/unit_test.hpp>

#include <sstream>


using maps::analyzer::VehicleId;

BOOST_AUTO_TEST_CASE(comparison)
{
    BOOST_CHECK_EQUAL(VehicleId("0", "1"), VehicleId("0", "1"));
    BOOST_CHECK_EQUAL(false, VehicleId("", "1") == VehicleId("0", "1"));
    BOOST_CHECK_EQUAL(false, VehicleId("0", "") == VehicleId("0", "1"));

    BOOST_CHECK_EQUAL(true, VehicleId("0", "1") < VehicleId("1", "1"));
    BOOST_CHECK_EQUAL(false, VehicleId("0", "1") < VehicleId("0", "1"));
    BOOST_CHECK_EQUAL(true, VehicleId("0", "0") < VehicleId("0", "1"));
    BOOST_CHECK_EQUAL(true, VehicleId("", "0") < VehicleId("0", "0"));
    BOOST_CHECK_EQUAL(false, VehicleId("0", "0") < VehicleId("0", ""));
}

BOOST_AUTO_TEST_CASE(insert_to_stream)
{
    std::ostringstream stream;
    stream << VehicleId("abc", "123");
    BOOST_CHECK_EQUAL("abc 123", stream.str());
}

BOOST_AUTO_TEST_CASE(extract_from_stream)
{
    std::istringstream stream("123   abc");
    VehicleId vehicleId;
    stream >> vehicleId;
    BOOST_CHECK_EQUAL(VehicleId("123", "abc"), vehicleId);
}
