#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/wikimap/mapspro/services/autocart/libs/satellite/include/load_sat_params.h>

#include <optional>
#include <string>

namespace maps {
namespace wiki {
namespace autocart {
namespace tests {

namespace {

} //anonymous namespace

Y_UNIT_TEST_SUITE(read_satellite_params)
{

    Y_UNIT_TEST(test_normal_case)
    {
        std::string xml = "{\"elev\": \"90\", \"azim_angle\": \"180\"}";
        double expectedElevInRadians = M_PI / 2.;
        double expectedAzimInRadians = M_PI;
        std::stringstream ss(xml);

        std::optional<SatelliteAngles> angles;
        ss >> angles;

        EXPECT_TRUE(angles.has_value());
        EXPECT_DOUBLE_EQ(angles->elev, expectedElevInRadians);
        EXPECT_DOUBLE_EQ(angles->azimAngle, expectedAzimInRadians);
    }

    Y_UNIT_TEST(test_uninitialized_case)
    {
        std::string xml = "{\"azim_angle\": \"180\"}";

        std::stringstream ss(xml);

        std::optional<SatelliteAngles> angles;
        ss >> angles;

        EXPECT_TRUE(!angles.has_value());
    }

} // Y_UNIT_TEST_SUITE(read satellite params)

Y_UNIT_TEST_SUITE(parse_satellite_xml_tests)
{

    Y_UNIT_TEST(test_normal_case)
    {
        std::string data = "<?xml version='1.0' encoding='utf-8'?>"
                           "<mosaics lat=\"50\" lon=\"50\">"
                           "    <mosaic zorder=\"1\" release-status=\"production\">"
                           "        <tags/>"
                           "        <metadata>{\"elev\": \"90\", \"azim_angle\": \"180\"}</metadata>"
                           "    </mosaic>"
                           "</mosaics>";
        xml3::Doc satXml = xml3::Doc::fromString(data);
        double expectedElevInRadians = M_PI / 2.;
        double expectedAzimInRadians = M_PI;

        std::optional<SatelliteAngles> angles = parseSatXml(satXml);

        EXPECT_TRUE(angles.has_value());
        EXPECT_DOUBLE_EQ(angles->elev, expectedElevInRadians);
        EXPECT_DOUBLE_EQ(angles->azimAngle, expectedAzimInRadians);
    }

    Y_UNIT_TEST(test_ignore_not_production_mosaic)
    {
        std::string data = "<?xml version='1.0' encoding='utf-8'?>"
                           "<mosaics lat=\"50\" lon=\"50\">"
                           "    <mosaic zorder=\"2\" release-status=\"frozen\">"
                           "        <tags/>"
                           "        <metadata>{\"elev\": \"190\", \"azim_angle\": \"80\"}</metadata>"
                           "     </mosaic>"
                           "    <mosaic zorder=\"1\" release-status=\"production\">"
                           "        <tags/>"
                           "        <metadata>{\"elev\": \"0\", \"azim_angle\": \"90\"}</metadata>"
                           "    </mosaic>"
                           "</mosaics>";
        xml3::Doc satXml = xml3::Doc::fromString(data);
        double expectedElevInRadians = 0.;
        double expectedAzimInRadians = M_PI / 2.;

        std::optional<SatelliteAngles> angles = parseSatXml(satXml);

        EXPECT_TRUE(angles.has_value());
        EXPECT_DOUBLE_EQ(angles->elev, expectedElevInRadians);
        EXPECT_DOUBLE_EQ(angles->azimAngle, expectedAzimInRadians);
    }

    Y_UNIT_TEST(test_find_max_zorder)
    {
        std::string data = "<?xml version='1.0' encoding='utf-8'?>"
                           "<mosaics lat=\"50\" lon=\"50\">"
                           "    <mosaic zorder=\"2\" release-status=\"production\">"
                           "        <tags/>"
                           "        <metadata>{\"elev\": \"90\", \"azim_angle\": \"180\"}</metadata>"
                           "     </mosaic>"
                           "    <mosaic zorder=\"1\" release-status=\"production\">"
                           "        <tags/>"
                           "        <metadata>{\"elev\": \"0\", \"azim_angle\": \"90\"}</metadata>"
                           "    </mosaic>"
                           "</mosaics>";
        xml3::Doc satXml = xml3::Doc::fromString(data);
        double expectedElevInRadians = M_PI / 2.;
        double expectedAzimInRadians = M_PI;

        std::optional<SatelliteAngles> angles = parseSatXml(satXml);

        EXPECT_TRUE(angles.has_value());
        EXPECT_DOUBLE_EQ(angles->elev, expectedElevInRadians);
        EXPECT_DOUBLE_EQ(angles->azimAngle, expectedAzimInRadians);
    }

    Y_UNIT_TEST(test_empty_metadata)
    {
        std::string data = "<?xml version='1.0' encoding='utf-8'?>"
                           "<mosaics lat=\"50\" lon=\"50\">"
                           "    <mosaic zorder=\"1\" release-status=\"production\">"
                           "        <tags/>"
                           "        <metadata>{}</metadata>"
                           "    </mosaic>"
                           "</mosaics>";
        xml3::Doc satXml = xml3::Doc::fromString(data);

        std::optional<SatelliteAngles> angles = parseSatXml(satXml);

        EXPECT_TRUE(!angles.has_value());
    }

    Y_UNIT_TEST(test_without_elev)
    {
        std::string data = "<?xml version='1.0' encoding='utf-8'?>"
                           "<mosaics lat=\"50\" lon=\"50\">"
                           "    <mosaic zorder=\"1\" release-status=\"production\">"
                           "        <tags/>"
                           "        <metadata>{\"azim_angle\": \"90\"}</metadata>"
                           "    </mosaic>"
                           "</mosaics>";
        xml3::Doc satXml = xml3::Doc::fromString(data);

        std::optional<SatelliteAngles> angles = parseSatXml(satXml);

        EXPECT_TRUE(!angles.has_value());
    }

    Y_UNIT_TEST(test_without_metadata)
    {
        std::string data = "<?xml version='1.0' encoding='utf-8'?>"
                           "<mosaics lat=\"50\" lon=\"50\">"
                           "    <mosaic zorder=\"1\" release-status=\"production\">"
                           "    </mosaic>"
                           "</mosaics>";
        xml3::Doc satXml = xml3::Doc::fromString(data);

        std::optional<SatelliteAngles> angles = parseSatXml(satXml);

        EXPECT_TRUE(!angles.has_value());
    }

} // Y_UNIT_TEST_SUITE(parse_satellite_xml)

} //namespace tests
} //namespace autocart
} //namespace wiki
} //namespace maps
