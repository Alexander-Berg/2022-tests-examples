#include <maps/infopoint/tests/common/fixture.h>
#include <maps/infopoint/lib/auth/find_author.h>
#include <maps/infopoint/lib/misc/xml.h>

#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

using namespace infopoint;

const std::string ATTRIBUTION_XML = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/attribution.xml";

void findAuthorAndPrintException(
    const maps::xml3::Node& attribution,
    const maps::xml3::Node& point)
{
    try {
        findAuthorURI(attribution, point);
    } catch (const std::exception &e) {
        std::cerr << "\tGot: " << e.what() << std::endl;
        throw;
    }
}

TEST_F(Fixture, test_node_attribution_generator)
{
    auto xml = maps::xml3::Doc::fromFile(ATTRIBUTION_XML);
    addXmlNamespaces(xml);

    INFO() << "Testing absent ym:attribution:";
    EXPECT_NO_THROW(
        findAuthorAndPrintException(
            xml.node("/AttributionTest/ya:Attribution"),
            xml.node("/AttributionTest/incorrect/ym:GeoObject[1]")));

    INFO() << "Testing incorrect ym:attribution:";
    EXPECT_THROW(
        findAuthorAndPrintException(
            xml.node("/AttributionTest/ya:Attribution"),
            xml.node("/AttributionTest/incorrect/ym:GeoObject[2]")),
        infopoint::HttpErrorException<>);

    INFO() << "Testing http://uri.maps.yandex.ru attribution";
    EXPECT_EQ(
        findAuthorURI(
            xml.node("/AttributionTest/ya:Attribution"),
            xml.node("/AttributionTest/correct/ym:GeoObject[1]")),
        UserURI("http://uri.maps.yandex.ru"));

    INFO() << "Testing http://noname.maps.yandex.ru attribution";
    EXPECT_EQ(
        findAuthorURI(
            xml.node("/AttributionTest/ya:Attribution"),
            xml.node("/AttributionTest/correct/ym:GeoObject[2]")),
        UserURI("http://noname.maps.yandex.ru"));

    INFO() << "Testing http://link.maps.yandex.ru attribution";
    EXPECT_EQ(
        findAuthorURI(
            xml.node("/AttributionTest/ya:Attribution"),
            xml.node("/AttributionTest/correct/ym:GeoObject[3]")),
        UserURI("http://link.maps.yandex.ru"));
}

TEST_F(Fixture, test_missing_attribution)
{
    maps::xml3::Doc xml(maps::xml3::Doc::fromFile(ATTRIBUTION_XML));
    addXmlNamespaces (xml);

    INFO() << "Testing missing ya:Attribution section:";
    EXPECT_THROW(
        findAuthorAndPrintException(
            xml.node("/MissingNode", true),
            xml.node("/AttributionTest/correct/ym:GeoObject[1]")),
        infopoint::HttpErrorException<>);
}
