#include "tests/boost-tests/include/tools/map_tools.h"

#include "core/XmlNamespaces.h"
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/MapException.h>
#include <yandex/maps/renderer5/core/MapPathResolverFactory.h>
#include <yandex/maps/renderer5/core/StyleHolders.h>
#include <yandex/maps/renderer5/core/LayerProperties.h>
#include <yandex/maps/renderer5/core/ITypedLayer.h>
#include <yandex/maps/renderer5/postgres/PfcParamsHolder.h>
#include <yandex/maps/renderer5/styles/styles.h>

#include <boost/test/unit_test.hpp>
#include <boost/lexical_cast.hpp>
#include <sstream>
#include <limits>

using namespace maps::renderer5;

namespace {

core::ILayer* layerById(core::Map& map, core::LayerIdType layerId)
{
    auto layer = map.rootGroupLayer()->getLayerById(layerId).get();
    BOOST_REQUIRE(layer);
    return layer;
}

core::ITypedLayer::Type layerType(core::Map& map, core::LayerIdType layerId) {
    auto layer = layerById(map, layerId);
    auto ptr = layer->cast<core::ITypedLayer>();
    BOOST_REQUIRE(ptr);
    return ptr->type();
}

std::string tableName(core::Map& map, core::LayerIdType layerId)
{
    auto layer = layerById(map, layerId);
    auto holder = layer->cast<postgres::IPfcParamsHolder>();
    BOOST_REQUIRE(holder);
    postgres::PfcParams pfcStub;
    pfcStub.sourceTableName = std::string("stub");
    pfcStub.layerId = 1;
    postgres::PfcParams pfc = holder->setPfcParams(pfcStub);
    const std::string sourceTableName = pfc.sourceTableName;
    holder->setPfcParams(pfc);
    return sourceTableName;
}

std::string emptyMapXmlNS(
    const std::string& rendererNS,
    const std::string& stylesNS)
{
    std::stringstream ss;
    ss <<
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
        "<r:Map "
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        "xmlns:xi=\"http://www.w3.org/2001/XInclude\" "
        "xmlns:refl=\"" << core::URN_REFLECTION_NS << "/1.0\" ";
    if (!rendererNS.empty())
        ss << "xmlns:r=\"" << rendererNS <<"\" ";
    if (!stylesNS.empty())
        ss << "xmlns:rs=\"" << stylesNS <<"\" ";
    ss << ">"
        "<r:rootLayer>"
        "<r:GroupLayer r:id=\"0\" />"
        "</r:rootLayer>"
        "</r:Map>";
    return ss.str();
}

std::string emptyMapXmlV(
    const std::string& rendererV,
    const std::string& stylesV)
{
    return emptyMapXmlNS(
        core::URN_RENDERER + "/" + rendererV,
        core::URN_RENDERER_STYLES + "/" + stylesV);
}

void tryLoadXml(const std::string& xml)
{
    core::Map map(core::MapMode::Dynamic);
    map.loadFromXml(xml.c_str(), false, "");
    map.close();
}

}  // namespace

BOOST_AUTO_TEST_SUITE( xslt_conversions )

BOOST_AUTO_TEST_CASE( correct_namespaces )
{
    std::string xml;

    xml = emptyMapXmlV("2.0", "2.0");
    BOOST_REQUIRE_NO_THROW(tryLoadXml(xml));

    xml = emptyMapXmlNS(
        core::rendererTypeNamespace.urn(), core::stylesTypeNamespace.urn());

    BOOST_REQUIRE_NO_THROW(tryLoadXml(xml));

    xml = emptyMapXmlNS(
        core::rendererTypeNamespace.urn(), "");

    BOOST_REQUIRE_NO_THROW(tryLoadXml(xml));

    xml = emptyMapXmlNS("", "");
    BOOST_REQUIRE_THROW(tryLoadXml(xml), core::MapException);

    xml = emptyMapXmlNS("xx", "yy");
    BOOST_REQUIRE_THROW(tryLoadXml(xml), core::MapDocumentNotValidException);

    std::string maxVersion = boost::lexical_cast<std::string>(
        std::numeric_limits<unsigned int>::max());

    xml = emptyMapXmlV(maxVersion, maxVersion); // "urn:yandex:maps:renderer/4294967295" without minor version
    BOOST_REQUIRE_THROW(tryLoadXml(xml), core::MapDocumentNotValidException);

    maxVersion += ".0"; // "urn:yandex:maps:renderer/4294967295.0"
    xml = emptyMapXmlV(maxVersion, maxVersion);
    BOOST_REQUIRE_NO_THROW(tryLoadXml(xml));
}

BOOST_AUTO_TEST_CASE( FileToPostgresContainers )
{
    core::IMapGuiPtr mapGui = test::map::openMap(
        "tests/boost-tests/maps/SmallFileBasedMap.xml");

    std::stringstream ss;
    ss << core::syspaths::xsltPatchDir() << "/conversions/FileToPostgresContainers.xslt";

    BOOST_REQUIRE_NO_THROW(mapGui->patchMap(test::map::createProgressStub(), ss.str()));

    BOOST_CHECK(layerType(mapGui->map(), 1) ==
        core::ITypedLayer::DirectDatabaseGeometryLayer);
    BOOST_CHECK(layerType(mapGui->map(), 2) ==
        core::ITypedLayer::MemoryDatabaseTextLayer);

    mapGui->map().close();
}

BOOST_AUTO_TEST_CASE( removeQuotesFromTableName )
{
    core::IMapGuiPtr mapGui = test::map::loadMap(
        "tests/boost-tests/maps/QuotedTableNameMap.xml");

    std::stringstream ss;
    ss << core::syspaths::xsltPatchDir() << "/conversions/removeQuotesFromTableName.xslt";

    BOOST_REQUIRE(tableName(mapGui->map(), 1) == std::string("\"public.geography_columns\""));

    BOOST_REQUIRE_NO_THROW(mapGui->patchMap(test::map::createProgressStub(), ss.str()));

    BOOST_CHECK(tableName(mapGui->map(), 1) == std::string("public.geography_columns"));
}

BOOST_AUTO_TEST_CASE( correctPostgresTableName )
{
    core::IMapGuiPtr mapGui = test::map::loadMap(
        "tests/boost-tests/maps/TableNameWithSchemaMap.xml");

    std::stringstream ss;
    ss << core::syspaths::xsltPatchDir() << "/conversions/correctPostgresTableName.xslt";

    BOOST_REQUIRE(tableName(mapGui->map(), 1) == std::string("public.geography_columns"));

    BOOST_REQUIRE_NO_THROW(mapGui->patchMap(test::map::createProgressStub(), ss.str()));

    BOOST_CHECK(tableName(mapGui->map(), 1) == std::string("geography_columns"));
}

BOOST_AUTO_TEST_SUITE_END()
