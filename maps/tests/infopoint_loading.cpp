#include <maps/infopoint/lib/point/infopoint.h>
#include <maps/infopoint/lib/point/xml.h>
#include <maps/infopoint/lib/misc/xml.h>
#include <maps/infopoint/tests/common/time_io.h>
#include <maps/infopoint/tests/common/fixture.h>

#include <yandex/maps/proto/common2/geo_object.pb.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <boost/filesystem.hpp>

using namespace infopoint;
using std::chrono::system_clock;

const auto DEFAULT_POINT = Infopoint(
    generateRandomPointUuid(),
    PointType(PointTags{"other"}),
    {} /* position */);

const std::string DURATIONS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/durations.conf";
const std::string CORRECT_POINTS_DIR = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/point/correct/";
const std::string INCORRECT_POINTS_DIR = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/point/incorrect/";

void loadXml(const std::string& filename, Infopoint* point)
{
    static const auto pointDurations =
        PointDurations::fromFile(DURATIONS_CONFIG);

    auto xml = maps::xml3::Doc::fromFile(filename);
    addXmlNamespaces(xml);
    point::update(point, pointDurations, xml.root());
}

void checkFullValues(const TimePoint& testStartTime, const Infopoint& point)
{
    EXPECT_EQ(point.type.legacyTypeDeprecated(), "accident");
    EXPECT_EQ(point.begin, system_clock::from_time_t(1387123005));
    EXPECT_EQ(point.end, system_clock::from_time_t(1387123005));

    EXPECT_EQ(point.storedVersion, 0);

    EXPECT_TRUE(point.modified >= testStartTime);
    EXPECT_TRUE(point.modified <= getTime());

    EXPECT_NEAR(point.position.y(), 50.0, 0.01);
    EXPECT_NEAR(point.position.x(), 40.0, 0.01);

    EXPECT_EQ(point.address, "Улица им. Шмулица");
}

TEST_F(Fixture, test_infopoint_load_xml)
{
    auto testStartTime = getTime();
    auto point = DEFAULT_POINT;

    loadXml(CORRECT_POINTS_DIR + "full.xml", &point);

    checkFullValues(testStartTime, point);
    EXPECT_TRUE(point.direction.has_value());
    EXPECT_NEAR(*point.direction, 123.0, 0.01);
}

TEST_F(Fixture, test_infopoint_load_tags)
{
    auto point = DEFAULT_POINT;
    loadXml(CORRECT_POINTS_DIR + "tags.xml", &point);
    EXPECT_EQ(point.type.legacyTypeDeprecated(), "speed camera");
    EXPECT_EQ(
        point.type.tags(),
        PointTags({"police", "speed_control", "lane_control"}));
}

TEST_F(Fixture, test_infopoint_dont_load_invalid_tags)
{
    auto point = DEFAULT_POINT;
    EXPECT_THROW(
        loadXml(INCORRECT_POINTS_DIR + "bad_tags.xml", &point),
        InvalidTagsError);
}

void checkDefaultValues(const TimePoint& testStartTime, const Infopoint& point)
{
    EXPECT_EQ(point.type.legacyTypeDeprecated(), "accident");
    EXPECT_FALSE(point.direction.has_value());

    //TODO fix test or loading    EXPECT_TRUE(point.begin() >= testStartTime);
    EXPECT_LE(point.begin, system_clock::now());
    EXPECT_GE(point.end, testStartTime);
    EXPECT_GE(point.modified, testStartTime);
    EXPECT_LE(point.modified, system_clock::now());
    EXPECT_EQ(point.storedVersion, 0);
    EXPECT_EQ(point.address, "");
}

TEST_F(Fixture, test_infopoint_xml_defaults)
{
    auto testStartTime = getTime();
    auto point = DEFAULT_POINT;
    loadXml(CORRECT_POINTS_DIR + "minimal.xml", &point);
    checkDefaultValues(testStartTime, point);
}

TEST_F(Fixture, test_type_change_allowed)
{
    auto point = DEFAULT_POINT;
    loadXml(CORRECT_POINTS_DIR + "full.xml", &point);
    loadXml(CORRECT_POINTS_DIR + "type_change.xml", &point);
}

TEST_F(Fixture, test_gml_point_missing_ok)
{
    auto point = DEFAULT_POINT;
    loadXml(CORRECT_POINTS_DIR + "minimal.xml", &point);

    auto prevPoint = point.position;

    loadXml(INCORRECT_POINTS_DIR + "no_gml_point.xml", &point);

    EXPECT_NEAR(point.position.y(), prevPoint.y(), 0.01);
    EXPECT_NEAR(point.position.x(), prevPoint.x(), 0.01);
}

void loadXmlAndPrintException(const std::string& filename) {
    try {
        auto point = DEFAULT_POINT;
        loadXml(filename, &point);
        point.validate();
    } catch (std::exception &e) {
        std::cerr << "\tGot: " << e.what() << std::endl;
        throw;
    }
}

TEST_F(Fixture, test_infopoint_incorrect_xml)
{
    auto xml = maps::xml3::Doc::create("void");

    boost::filesystem::directory_iterator end_file;
    for (boost::filesystem::directory_iterator file(INCORRECT_POINTS_DIR);
            file != end_file; ++file) {
        if (is_directory(file->status())) {
            continue;
        }

        EXPECT_THROW(
            loadXmlAndPrintException(file->path().string()),
            maps::RuntimeError);
    }
}
