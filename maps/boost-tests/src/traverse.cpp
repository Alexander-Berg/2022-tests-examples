#include "../include/tools.h"
#include "../include/contexts.hpp"

#include <yandex/maps/tilerenderer4/IOnlineRenderer.h>

#include <yandex/maps/renderer5/postgres/IPostgresTransactionProvider.h>
#include <yandex/maps/renderer5/postgres/DefaultPostgresTransactionProvider.h>
#include <yandex/maps/renderer5/postgres/SinglePostgresTransactionProvider.h>

#include <maps/renderer/libs/base/include/string_convert.h>

#include <boost/test/unit_test.hpp>
#include <boost/filesystem.hpp>

using namespace maps;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
using namespace maps::tilerenderer4;
using namespace maps::tilerenderer4::test;

namespace {
class TestTraversal: public maps::tilerenderer4::ILayersTraverse
{
public:
    virtual bool onEnterGroupLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"{Id:\"" << layer.id()
        << L"\",Name:\"" << base::s2ws(layer.name())
        << L"\",Children:[";

        return true;
    }

    virtual bool onVisitLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"{Id:\"" << layer.id()
            << L"\",Name:\"" << base::s2ws(layer.name())
            << L"\"}";

        return true;
    }

    virtual void onLeaveGroupLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"]}";
    }

    virtual bool isDone()
    {
        return false;
    }

    std::wstring readTraverse()
    {
        return m_stream.str();
    }

private:
    std::wstringstream m_stream;
};

class EmptyFilter: public maps::tilerenderer4::ILayersTraverse
{
public:
    virtual bool onEnterGroupLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"{Id:\"" << layer.id()
            << L"\",Name:\"" << base::s2ws(layer.name())
            << L"\",Children:[";

        return true;
    }

    virtual bool onVisitLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"{Id:\"" << layer.id()
            << L"\",Name:\"" << base::s2ws(layer.name())
            << L"\"}";

        return false;
    }

    virtual void onLeaveGroupLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"]}";
    }

    virtual bool isDone()
    {
        return false;
    }

    std::wstring readTraverse()
    {
        return m_stream.str();
    }

private:
    std::wstringstream m_stream;
};

class MatchAllFilter: public maps::tilerenderer4::ILayersTraverse
{
public:
    virtual bool onEnterGroupLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"{Id:\"" << layer.id()
            << L"\",Name:\"" << base::s2ws(layer.name())
            << L"\",Children:[";

        return true;
    }

    virtual bool onVisitLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"{Id:\"" << layer.id()
            << L"\",Name:\"" << base::s2ws(layer.name())
            << L"\"}";

        return true;
    }

    virtual void onLeaveGroupLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        m_stream << L"]}";
    }

    virtual bool isDone()
    {
        return false;
    }

    std::wstring readTraverse()
    {
        return m_stream.str();
    }

private:
    std::wstringstream m_stream;
};

class WikiFilter: public tilerenderer4::ILayersTraverse
{
public:
    WikiFilter()
    {
    }

    WikiFilter(const std::string& subLayers)
    {
        m_displayLayer = boost::lexical_cast<unsigned int>(subLayers);
    }

    bool onEnterGroupLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        return true;
    }

    bool onVisitLayer(const maps::tilerenderer4::LayerInfo& layer)
    {
        return layer.id() == m_displayLayer;
    }

    void onLeaveGroupLayer(const maps::tilerenderer4::LayerInfo& /*layer*/)
    {
    }

    bool isDone()
    {
        return false;
    }

private:
    unsigned int m_displayLayer;
};

class MoscowMapFiles
{
public:
    MoscowMapFiles(const std::string baseUri):
        m_baseUri(baseUri)
    {
        m_files["Address_m.DAT.gz"] = "Address_m.DAT";
        m_files["Address_m.ID.gz"] = "Address_m.ID";
        m_files["Address_m.MAP.gz"] = "Address_m.MAP";
        m_files["Address_m.TAB.gz"] = "Address_m.TAB";
        m_files["Road_m.DAT.gz"] = "Road_m.DAT";
        m_files["Road_m.ID.gz"] = "Road_m.ID";
        m_files["Road_m.MAP.gz"] = "Road_m.MAP";
        m_files["Road_m.TAB.gz"] = "Road_m.TAB";

        for (const auto& file : m_files)
        {
            std::string extractedPath = m_baseUri + file.second;
            boost::filesystem::remove(extractedPath);
            tools::unzip_file((m_baseUri + file.first).c_str(), extractedPath.c_str());
        }
    }

    ~MoscowMapFiles()
    {
        for (const auto& file : m_files)
            boost::filesystem::remove(m_baseUri + file.second);
    }

private:
    const std::string m_baseUri;
    std::map<std::string, std::string> m_files;
};

std::unique_ptr<IOnlineRenderer> createAndSetRenderer(base::ILoggerPtr logger, const std::string& mapXml)
{
    std::unique_ptr<IOnlineRenderer> onlineRenderer(createOnlineTileRenderer(logger));

    onlineRenderer->open(mapXml);
    onlineRenderer->setSkipEmptyTiles(true);

    return onlineRenderer;
}
}

BOOST_AUTO_TEST_SUITE( OnlineRenderer )
BOOST_AUTO_TEST_SUITE( traverse )

BOOST_FIXTURE_TEST_CASE( traverseTest, OnlineRendererContext<> )
{
    const std::string MAP_XML("tests/boost-tests/maps/GroupLayerOnly.xml");
    const std::wstring expectedResult(
        L"{Id:\"0\",Name:\"RootParent\",Children:[{Id:\"1\",Name:\"FirstChild\",Children:[]}{Id:\"2\",Name:\"SecondChild\",Children:[]}]}");

    BOOST_CHECK_NO_THROW(renderer->open(MAP_XML));

    TestTraversal traversal;

    BOOST_CHECK_NO_THROW(renderer->traverse(traversal));

    BOOST_CHECK_EQUAL(
        base::ws2s(traversal.readTraverse()),
        base::ws2s(expectedResult));
}

BOOST_FIXTURE_TEST_CASE( tile_render_traverse, OnlineRendererContext<> )
{
    const std::string MOSCOW_MAP_FILENAME = "tests/boost-tests/maps/msc.xml";
    MoscowMapFiles localFiles("tests/boost-tests/data/msk/");

    renderer->open(MOSCOW_MAP_FILENAME);

    postgres::PostgresTransactionProviderPtr noProvider;

    const double mtx[6] = { 1, 0, 0, 1, 0, 0 };

    // Just to check what tile will be rendered:
    // http://vec01.maps.yandex.net/tiles?l=map&v=2.28.0&x=9899&y=5128&z=14&lang=ru-RU
    const unsigned int x = 9899;
    const unsigned int y = 5128;
    const unsigned int z = 14;

    EmptyFilter emptyFilter;
    MatchAllFilter allFilter;

    auto renderedTile1 = renderer->render(
        x, y, z,
        maps::tilerenderer4::OutputFormatRgbaPng,
        noProvider,
        mtx,
        emptyFilter);

    BOOST_CHECK_NE(renderedTile1.outputDataSize, 0);

    auto renderedTile2 = renderer->render(
        x, y, z,
        maps::tilerenderer4::OutputFormatRgbaPng,
        noProvider,
        mtx,
        allFilter);

    BOOST_CHECK_NE(renderedTile2.outputDataSize, 0);
    BOOST_CHECK_NE(renderedTile1.outputDataSize, renderedTile2.outputDataSize);

    auto renderedTile3 = renderer->render(
        x, y, z,
        maps::tilerenderer4::OutputFormatRgbaPng,
        noProvider,
        mtx,
        emptyFilter);

    BOOST_CHECK_EQUAL(renderedTile1.outputDataSize, renderedTile3.outputDataSize);

    auto renderedTile4 = renderer->render(
        x, y, z,
        maps::tilerenderer4::OutputFormatRgbaPng,
        noProvider,
        mtx,
        allFilter);

    BOOST_CHECK_EQUAL(renderedTile2.outputDataSize, renderedTile4.outputDataSize);
    BOOST_CHECK_EQUAL_COLLECTIONS(
        renderedTile2.outputData.get(), renderedTile2.outputData.get() + renderedTile2.outputDataSize,
        renderedTile4.outputData.get(), renderedTile4.outputData.get() + renderedTile4.outputDataSize);
}

BOOST_AUTO_TEST_SUITE_END()
BOOST_AUTO_TEST_SUITE_END()
