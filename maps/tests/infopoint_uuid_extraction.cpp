#include <maps/infopoint/tests/common/fixture.h>
#include <maps/infopoint/lib/misc/xml.h>
#include <maps/infopoint/lib/point/infopoint.h>
#include <maps/infopoint/lib/point/xml.h>
#include <maps/infopoint/lib/auth/user.h>

#include <maps/libs/common/include/exception.h>

#include <iostream>

#include <library/cpp/testing/gtest/gtest.h>

using namespace infopoint;
using maps::xml3::Doc;

const std::string DURATIONS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/durations.conf";
const std::string CORRECT_UUID_DIR = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/uuid/correct/";
const std::string INCORRECT_UUID_DIR = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/uuid/incorrect/";

PointUuid xmlToPointUuid(Doc xml) {
    static PointDurations pointDurations =
        PointDurations::fromFile(DURATIONS_CONFIG);

    addXmlNamespaces(xml);

    const auto pointNode = xml.node("//ym:GeoObject");
    auto author = User(
        User::MOBILE,
        UserURI("http://maps.yandex.ru"),
        0.3 /* rating */);
    auto point = point::load(
        pointDurations,
        author.uri(),
        author.uri(),
        author.rating(),
        pointNode);
    point.validate();
    return point.uuid;
}

TEST_F(Fixture, infopoint_uuid_same)
{
    EXPECT_EQ(
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "full.xml")),
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "full.xml")));

    EXPECT_EQ(
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "tags1.xml")),
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "tags1.xml")));

    EXPECT_EQ(
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "tags1.xml")),
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "tags2.xml")));
}

TEST_F(Fixture, infopoint_uuid_different)
{
    EXPECT_NE(
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "full.xml")),
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "minimal.xml")));

    EXPECT_NE(
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "chat1.xml")),
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "chat2.xml")));

    EXPECT_NE(
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "tags1.xml")),
        xmlToPointUuid(Doc::fromFile(CORRECT_UUID_DIR + "tags3.xml")));
}

TEST_F(Fixture, infopoint_uuid_incorrect)
{
    EXPECT_THROW(
        xmlToPointUuid(Doc::fromFile(INCORRECT_UUID_DIR + "no_pos.xml")),
        maps::RuntimeError);
    EXPECT_THROW(
        xmlToPointUuid(Doc::fromFile(INCORRECT_UUID_DIR + "no_type.xml")),
        maps::RuntimeError);
    EXPECT_THROW(
        xmlToPointUuid(Doc::fromFile(INCORRECT_UUID_DIR + "bad_id.xml")),
        maps::RuntimeError);
}
