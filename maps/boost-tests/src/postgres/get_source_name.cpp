#include "tests/boost-tests/include/tools/map_tools.h"
#include "../../include/contexts.hpp"
#include "labeler/i_labelable_layer.h"

#include <yandex/maps/renderer5/postgres/DefaultPostgresTransactionProvider.h>
#include <yandex/maps/renderer5/postgres/SinglePostgresTransactionProvider.h>
#include <yandex/maps/renderer5/core/ITypedLayer.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/LayerProperties.h>


#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::postgres;
using namespace maps::renderer5::test::map;
using namespace maps::renderer5::test::postgres;


namespace {
const char* getPostgresSourceNameXmlFileName = "tests/boost-tests/maps/GetPostgresSourceNameTestMap.xml";
const wchar_t* postgresSourceName = L"wiki_streets";

bool checkLayerType(
    const core::ILayer* layer,
    core::ITypedLayer::Type type)
{
    if (layer) {
        if (auto ptr = layer->cast<core::ITypedLayer>()) {
            return ptr->type() == type;
        }
    }
    return false;
}

// throws core::NoCapabilityException
std::string getSourceFileName(
    core::ILayer& layer)
{
    if (auto ptr = layer.cast<labeler::ILabelableLayer>()) {
        return ptr->getSource().get<core::ILayerSourceFileName>()->sourceFileName();
    }
    return layer.get<core::ILayerSourceFileName>()->sourceFileName();
}

// throws core::NoCapabilityException
bool checkSourceFileName(
    core::ILayer& layer,
    const std::string& sourceFileName = std::string())
{
    return getSourceFileName(layer) == sourceFileName;
}

// throws core::NoCapabilityException
std::wstring getTableName(core::ILayer& layer)
{
    if (auto ptr = layer.cast<labeler::ILabelableLayer>()) {
        return ptr->getSource().get<core::ILayerTableName>()->tableName();
    }
    return layer.get<core::ILayerTableName>()->tableName();
}

// throws core::NoCapabilityException
bool checkTableName(
    core::ILayer& layer,
    const std::wstring& tableName = std::wstring())
{
    return getTableName(layer) == tableName;
}

}

BOOST_AUTO_TEST_SUITE( postgres )

BOOST_FIXTURE_TEST_CASE( getTableName, TransactionProviderContext<CleanContext<>> )
{
    core::IMapGuiPtr mapGui = createTestMapGui();
    core::Map& map = mapGui->map();

    map.setPostgresTransactionProvider(provider);

    map.loadFromXml(getPostgresSourceNameXmlFileName, true);

    map.open();

    auto layer = map.rootGroupLayer()->getLayerById(30);

    BOOST_REQUIRE(checkLayerType(layer.get(), ITypedLayer::GroupLayer));
    BOOST_CHECK_THROW(checkSourceFileName(*layer), core::NoCapabilityException);
    BOOST_CHECK_THROW(checkTableName(*layer), core::NoCapabilityException);

    layer = map.rootGroupLayer()->getLayerById(31);

    BOOST_REQUIRE(checkLayerType(layer.get(), ITypedLayer::DirectDatabaseGeometryLayer));
    BOOST_REQUIRE(checkTableName(*layer, postgresSourceName));

    layer = map.rootGroupLayer()->getLayerById(32);

    BOOST_REQUIRE(checkLayerType(layer.get(), ITypedLayer::WikiDatabaseTextLayer));
    BOOST_REQUIRE(checkTableName(*layer, postgresSourceName));
}

BOOST_AUTO_TEST_SUITE_END()
