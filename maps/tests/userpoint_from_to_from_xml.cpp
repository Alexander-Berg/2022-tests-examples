#include <maps/infopoint/tests/common/fixture.h>
#include <maps/infopoint/tests/common/serialization.h>
#include <maps/infopoint/tests/common/time_io.h>
#include <maps/infopoint/lib/export/supplier_id_map.h>
#include <maps/infopoint/lib/export/xml_point_feed.h>
#include <maps/infopoint/lib/comments/comment.h>
#include <maps/infopoint/lib/misc/xml.h>
#include <maps/infopoint/lib/point/infopoint.h>
#include <maps/infopoint/lib/point/xml.h>

#include <yandex/maps/proto/common2/response.pb.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <boost/filesystem.hpp>

using namespace infopoint;
using namespace testing;
using std::chrono::system_clock;

const std::string DURATIONS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/durations.conf";
const std::string SUPPLIERS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/suppliers.conf";
const std::string CORRECT_POINTS_DIR = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/point/correct/";


void checkPointsEqual(const Infopoint& point, const Infopoint& newPoint)
{
    // Serialization to xml loses information about tags and about new events
    // types: 'speed/lane camera' and 'danger'.
    EXPECT_EQ(
        pointLegacyTypeFor1xExportFormat(point.type.legacyTypeDeprecated()),
        pointLegacyTypeFor1xExportFormat(newPoint.type.legacyTypeDeprecated()));
    EXPECT_EQ(
        system_clock::to_time_t(point.begin),
        system_clock::to_time_t(newPoint.begin));
    EXPECT_EQ(
        system_clock::to_time_t(point.end),
        system_clock::to_time_t(newPoint.end));

    EXPECT_NEAR(point.position.y(), newPoint.position.y(), 0.01);
    EXPECT_NEAR(point.position.x(), newPoint.position.x(), 0.01);

    EXPECT_EQ(point.address, newPoint.address);

    EXPECT_EQ(point.direction.has_value(), newPoint.direction.has_value());
    EXPECT_NEAR(
        point.direction.value_or(0.0),
        newPoint.direction.value_or(0.0),
        0.01);
}

void checkXml(const Infopoint& point, const PointDurations& pointDurations)
{
    auto xml = maps::xml3::Doc::fromString(
        infopoint::tests::toXml(point, SUPPLIERS_CONFIG));
    addXmlNamespaces(xml);

    auto newPoint = point::load(
        pointDurations,
        point.author,
        point.owner,
        point.rating,
        xml.node("//ym:GeoObject"));

    EXPECT_EQ(newPoint.storedVersion, 0);

    checkPointsEqual(point, newPoint);
}

void checkProtobuf(const Infopoint& point)
{
    // We do not have protobuf loading functionality, so we just check
    // that point can be serialized
    yandex::maps::proto::common2::response::Response pointMessage;
    const TString protoBufTStr(infopoint::tests::toProtobuf(point, SUPPLIERS_CONFIG));
    EXPECT_TRUE(pointMessage.ParseFromString(protoBufTStr));
}

TEST_F(Fixture, test_infopoint_to_from_xml)
{
    PointDurations pointDurations = PointDurations::fromFile(DURATIONS_CONFIG);
    maps::xml3::Doc xml (maps::xml3::Doc::create("void"));
    boost::filesystem::directory_iterator end_file;
    for (boost::filesystem::directory_iterator file(CORRECT_POINTS_DIR);
        file != end_file; ++file) {
        if (is_directory(file->status())) {
            continue;
        }

        std::cerr << "Testing " << file->path().string() << std::endl;
        xml = maps::xml3::Doc::fromFile(file->path().string());

        addXmlNamespaces(xml);

        auto point = Infopoint(
            toPointUuid("bc55b020-dcfe-490a-b8ba-001abb3f4705"),
            point::extractType(xml.root()),
            {} /* position */);
        point.rating = 0.95;
        auto loadPoint = [&] {
            point::update(&point, pointDurations, xml.root());
        };

        EXPECT_NO_THROW(loadPoint());

        checkXml(point, pointDurations);
        checkProtobuf(point);
    }
}
