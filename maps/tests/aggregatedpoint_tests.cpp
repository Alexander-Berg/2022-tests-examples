#include <maps/infopoint/tests/common/fixture.h>
#include <maps/infopoint/tests/common/time_io.h>
#include <maps/infopoint/tests/common/serialization.h>
#include <maps/infopoint/lib/backend/query/point.h>
#include <maps/infopoint/lib/comments/comment.h>
#include <maps/infopoint/lib/export/xml_point_feed.h>
#include <maps/infopoint/lib/misc/xml.h>
#include <maps/infopoint/lib/auth/user.h>
#include <maps/infopoint/lib/export/supplier_id_map.h>
#include <maps/infopoint/lib/point/infopoint.h>
#include <maps/infopoint/lib/point/xml.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <boost/filesystem.hpp>

using namespace infopoint;

const std::string DURATIONS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/durations.conf";
const std::string SUPPLIERS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/suppliers.conf";
const std::string CORRECT_POINTS_DIR = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/point/correct/";

Infopoint createAggrPoint()
{
    using bsoncxx::builder::basic::make_array;
    using bsoncxx::builder::basic::make_document;
    using bsoncxx::builder::basic::kvp;

    auto obj = make_document(
        kvp("_id", "d6e6ed35-5052-4ad1-b3c2-80dce51b2bc1"),
        kvp("point", make_document(
            kvp("lat", 40.0),
            kvp("lon", -20.0))),
        kvp("version", 0),
        kvp("tags", make_array("accident")),
        kvp("begin", db::toMongoDate(1306161608)),
        kvp("modified", db::toMongoDate(1306161608)),
        kvp("end", make_document(
            kvp("auto", false),
            kvp("time", db::toMongoDate(1306168808)))),
        kvp("originalConfidence", 0.7),
        kvp("confidence", 0.7),
        kvp("rating", 0.7),
        kvp("direction", 231.0),
        kvp("attribution", make_document(
            kvp("author", "urn:login:author"),
            kvp("owner", "urn:login:owner"))),
        kvp("description", make_document(
            kvp("content", "Comment text"),
            kvp("moderation", make_document(
                kvp("status", "approved"),
                kvp("verdicts", make_array()),
                kvp("started", db::toMongoDate(1306168812)),
                kvp("next_retry", db::toMongoDate(1306168808)))))),
        kvp("address", "Улица им. Шмулица"),
        kvp("deleted", false));

    return db::toPoint(obj);
}

/// Generates XML from infopoint and loads it again to another infopoint.
Infopoint toFromXml(const Infopoint &point)
{
    auto pointDurations = PointDurations::fromFile(DURATIONS_CONFIG);

    auto xml = maps::xml3::Doc::fromString(
        infopoint::tests::toXml(point, SUPPLIERS_CONFIG));
    addXmlNamespaces(xml);
    auto newPoint = createAggrPoint();
    auto pointXmlRoot = xml.node("//ym:GeoObject");
    point::update(&newPoint, pointDurations, pointXmlRoot);
    return newPoint;
}

TEST_F(Fixture, test_infopoint_from_to_from_xml)
{
    PointDurations pointDurations =
        PointDurations::fromFile(DURATIONS_CONFIG);

    boost::filesystem::directory_iterator end_file;
    for (boost::filesystem::directory_iterator file(CORRECT_POINTS_DIR);
        file != end_file; ++file) {
        if (is_directory(file->status())) {
            continue;
        }

        std::cerr << "Testing " << file->path().string() << std::endl;
        auto xml = maps::xml3::Doc::fromFile(file->path().string());
        addXmlNamespaces(xml);

        auto point = createAggrPoint();
        auto loadPoint = [&]() {
            point::update(&point, pointDurations, xml.root());
        };
        EXPECT_NO_THROW(loadPoint());

        auto newPoint = createAggrPoint();

        EXPECT_NO_THROW(newPoint = toFromXml(point));

        EXPECT_EQ(
            pointLegacyTypeFor1xExportFormat(
                point.type.legacyTypeDeprecated()),
            pointLegacyTypeFor1xExportFormat(
                newPoint.type.legacyTypeDeprecated()));
        EXPECT_EQ(point.direction.has_value(), newPoint.direction.has_value());
        EXPECT_NEAR(
            point.direction.value_or(0.0),
            newPoint.direction.value_or(0.0),
            0.01);
        EXPECT_EQ(point.begin, newPoint.begin);
        EXPECT_EQ(point.end, newPoint.end);

        EXPECT_EQ(0, newPoint.storedVersion);

        EXPECT_NEAR(point.position.y(), newPoint.position.y(), 0.01);
        EXPECT_NEAR(point.position.x(), newPoint.position.x(), 0.01);
        EXPECT_EQ(point.address, newPoint.address);
    }
}
