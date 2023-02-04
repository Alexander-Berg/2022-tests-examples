#include "tests/boost-tests/include/tools/map_tools.h"
#include "hotspots/HotspotsDataWriter.h"

#include <yandex/maps/renderer5/core/ColumnDefinition.h>
#include <maps/renderer/libs/base/include/string_convert.h>
#include <yandex/maps/renderer5/hotspots/IAttributesHolder.h>

#include <yandex/maps/renderer5/core/Map.h>

#include <rapidjson/document.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/output_test_stream.hpp>

#include <iostream>
#include <functional>

using namespace maps;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::hotspots::data_writer;
using namespace std::placeholders;

namespace
{
    const char* AttrsList[] = {
        "YANDEXMAPS_GEO_REF", "ID", "label", "source_id"
    };

    typedef boost::test_tools::output_test_stream TestOStream;

    class TestWrite: public hotspots::AttributesWriter
    {
    public:
        void addRecord(const std::string& key,
                       const std::string& value) override
        {
            out << key << ": " << value << std::endl;
            lastValue = value;
        }

        virtual void finalize() override {}

        TestOStream out;
        std::string lastValue;
    };

    class TestMapHolder
    {
    public:
        core::IMapGuiPtr mapGui;
        TestMapHolder(std::string filename)
        {
            mapGui = test::map::createTestMapGui();
            mapGui->loadFromXml(filename, false);
            core::OperationProgressPtr progress = test::map::createProgressStub();
            mapGui->open(progress);
        }
    };

    std::set<std::string> extractAttrs(const std::string& value)
    {
        if (value.empty())
            return std::set<std::string>();

        rapidjson::Document json;
        json.Parse<rapidjson::kParseDefaultFlags>(value.c_str());

        std::set<std::string> result;
        for (auto it = json.MemberBegin(); it != json.MemberEnd(); ++it)
            result.insert(it->name.GetString());
        return result;
    }

    std::vector<std::wstring> toVector(const std::set<std::string>& attrs)
    {
        std::vector<std::wstring> result;
        for (const auto& attrName: attrs)
            result.push_back(base::s2ws(attrName));
        return result;
    }

    void testMap(TestMapHolder& mapHldr, const std::set<std::string>& expectedAttrs)
    {
        TestWrite write;
        WriteJson wrJs(&write);
        Writer writer(&wrJs);
        writer.processMap(mapHldr.mapGui->map());

        std::stringstream ss;
        ss << "Hotspots data writer test ids\n";
        ss << write.out.str() << '\n';
        // uncomment next line to view result
        //std::cout << ss.str();

        std::set<std::string> collectedAttrs = extractAttrs(write.lastValue);
        BOOST_CHECK_EQUAL_COLLECTIONS(
            expectedAttrs.begin(), expectedAttrs.end(),
            collectedAttrs.begin(), collectedAttrs.end());
    }
}

BOOST_AUTO_TEST_SUITE( hotspot_tests )

BOOST_AUTO_TEST_SUITE( hotspots_data_writer )

BOOST_AUTO_TEST_CASE( test_ids )
{
    TestMapHolder mapHldr("tests/boost-tests/maps/HotspotsTestMap.xml");

    auto rootLayer = mapHldr.mapGui->rootLayer()->get<core::IGroupLayer>();
    BOOST_REQUIRE_EQUAL(rootLayer->childCount(), 1);
    auto layer = rootLayer->getLayerByPos(0);
    auto attrsHolder = layer->cast<hotspots::IAttributesHolder>();
    BOOST_REQUIRE(attrsHolder);
    BOOST_REQUIRE(attrsHolder->useAllColumns());

    std::set<std::string> columnNames(AttrsList, AttrsList + 4);
    testMap(mapHldr, columnNames);

    attrsHolder->setUseAllColumns(false);
    testMap(mapHldr, std::set<std::string>());

    columnNames.erase(columnNames.begin());
    attrsHolder->setDataColumns(toVector(columnNames));
    testMap(mapHldr, columnNames);

    auto it = columnNames.begin();
    ++it;
    columnNames.erase(it);
    attrsHolder->setDataColumns(toVector(columnNames));
    testMap(mapHldr, columnNames);
}

BOOST_AUTO_TEST_SUITE_END() // hotspots_data_writer
BOOST_AUTO_TEST_SUITE_END() // hotspot_tests
