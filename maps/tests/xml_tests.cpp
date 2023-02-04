#include <maps/infopoint/lib/misc/xml.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace infopoint;

TEST(xml_tests, test_read_value)
{
    maps::xml3::Doc xml (maps::xml3::Doc::fromString(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?><values><int>123</int>\
        <string>str</string></values>"));

    EXPECT_NO_THROW(
        infopoint::readValue<int> (xml.root(), "/values/int"));
    EXPECT_THROW(
        infopoint::readValue<int> (xml.root(), "/values/string"),
        infopoint::HttpErrorException<>);
    EXPECT_THROW(
        infopoint::readValue<int> (xml.root(), "/values/missing"),
        infopoint::HttpErrorException<>);
    EXPECT_NO_THROW(
        infopoint::readValue (xml.root(), "/values/missing", 1));
    EXPECT_THROW(
        infopoint::readValue (xml.root(), "/values/string", 1),
        infopoint::HttpErrorException<>);
}

TEST(xml_tests, test_encode)
{
    EXPECT_EQ(infopoint::encode ("te&st"), "te&amp;st");
    EXPECT_EQ(infopoint::encode ("<tag>"), "&lt;tag&gt;");
}
