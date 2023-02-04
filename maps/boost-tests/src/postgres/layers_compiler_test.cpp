#include "tests/boost-tests/include/tools/map_tools.h"
#include "tests/boost-tests/include/tools/compilation_tools.h"

#include "core/urn_constants.h"
#include "core/locale_filter.h"
#include "core/locale_tools.h"
#include "core/regional_filter.h"
#include "mapcompiler/processor_common.h"
#include "postgres/IsolatedTransactionProvider.h"
#include "postgres/PostgresConstants.h"
#include "postgres/PostgresBinaryResult.h"

#include <maps/renderer/libs/base/include/string_convert.h>
#include <yandex/maps/renderer5/postgres/IPostgresTransactionProvider.h>
#include <yandex/maps/renderer5/styles/PointRenderStyle.h>

#include <fstream>
#include <vector>
#include <boost/test/unit_test.hpp>
#include <cstdio>

using namespace boost::unit_test;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::core;

namespace maps { namespace renderer5 { namespace test {

namespace {

const std::string optionsMsk =
    " host=pg94.maps.dev.yandex.net"
    " user=renderer"
    " password=renderer"
    " port=5432"
    " dbname=renderer_test"
    " options=--search_path=yandex_moscow_20120803,public";

const std::string optionsRenderer =
    " host=pg94.maps.dev.yandex.net"
    " user=renderer"
    " password=renderer"
    " port=5432"
    " dbname=renderer_test"
    " options=--search_path=renderer,public";

// returns sample small area of compilation (center of Moscow)
// which shouldn't change
BoxD centerOfMoscowWindow()
{
    return renderer::base::BoxD(4170000.0, 7460000.0, 4200000.0, 7490000.0);
}

BoxD krymWindow()
{
    return renderer::base::BoxD(3000000.0, 5300000.0, 4600000.0, 6800000.0);
}

BoxD bigWindow()
{
    double big = 0.001 * std::numeric_limits<double>::max();
    return renderer::base::BoxD(-big, -big, big, big);
}

typedef styles::VisibilityScaling ZoomRange;

ZoomRange mkZr(unsigned int zmin, unsigned int zmax)
{
    ZoomRange zr;
    zr.setMin(zmin);
    zr.setMax(zmax);
    return zr;
}

class TestGroupLayer {
public:
    TestGroupLayer(ILayerPtr layer):
        layer_(layer)
    {
        groupLayer_.push_back(layer_->get<IGroupLayer>());
        BOOST_REQUIRE(groupLayer_.back());
    }

    TestGroupLayer& nameExpected(const std::string& name)
    {
        BOOST_CHECK_EQUAL(base::ws2s(groupLayer_.back()->name()), name);
        return *this;
    }

    TestGroupLayer& nameExpected(const std::wstring& name)
    {
        return nameExpected(base::ws2s(name));
    }

    TestGroupLayer& childCountExpected(size_t count)
    {
        auto layer = groupLayer_.back();
        setContext(*layer, layer->childCount() == count);
        BOOST_REQUIRE_EQUAL(layer->childCount(), count);
        return *this;
    }

    TestGroupLayer& testChild(size_t pos, const std::string& nameExpected,
        std::optional<ZoomRange> zr = std::nullopt)
    {
        auto layer = getLayerByPos(pos);
        BOOST_CHECK_EQUAL(base::ws2s(layer->name()), nameExpected);
        if (zr) {
            BOOST_REQUIRE(!layer->cast<IGroupLayer>());
            BOOST_CHECK_EQUAL(layer->renderStyle()->visibilityScaling().min(), zr->min());
            BOOST_CHECK_EQUAL(layer->renderStyle()->visibilityScaling().max(), zr->max());
        }
        return *this;
    }

    TestGroupLayer& testChildRegion(size_t pos, const std::string& nameExpected, ZoomRange zr,
        const char* enableIfRegionExpected, const char* disableIfRegionExpected)
    {
        testChild(pos, nameExpected, zr);

        auto layer = getLayerByPos(pos);
        auto enableIfRegionActual = layer->metadata()->get(ENABLE_IF_REGION);
        if (enableIfRegionExpected) {
            setContext(*layer, enableIfRegionActual && *enableIfRegionActual == enableIfRegionExpected);
            BOOST_REQUIRE(enableIfRegionActual);
            BOOST_CHECK_EQUAL(*enableIfRegionActual, enableIfRegionExpected);
        } else {
            setContext(*layer, !enableIfRegionActual);
            BOOST_CHECK(!enableIfRegionActual);
        }

        auto disableIfRegionActual = layer->metadata()->get(DISABLE_IF_REGION);
        if (disableIfRegionExpected) {
            setContext(*layer, disableIfRegionActual && *disableIfRegionActual == disableIfRegionExpected);
            BOOST_REQUIRE(disableIfRegionActual);
            BOOST_CHECK_EQUAL(*disableIfRegionActual, disableIfRegionExpected);
        } else {
            setContext(*layer, !disableIfRegionActual);
            BOOST_CHECK(!disableIfRegionActual);
        }

        return *this;
    }

    TestGroupLayer& testChildLocale(size_t pos, const std::string& nameExpected,
        const char* enableIfLocaleExpected)
    {
        testChild(pos, nameExpected);

        auto layer = getLayerByPos(pos);
        auto enableIfLocaleActual = layer->metadata()->get(ENABLE_IF_LOCALE);
        if (enableIfLocaleExpected) {
            setContext(*layer, enableIfLocaleActual && *enableIfLocaleActual == enableIfLocaleExpected);
            BOOST_REQUIRE(enableIfLocaleActual);
            BOOST_CHECK_EQUAL(*enableIfLocaleActual, enableIfLocaleExpected);
        } else {
            setContext(*layer, !enableIfLocaleActual);
            BOOST_CHECK(!enableIfLocaleActual);
        }

        return *this;
    }

    TestGroupLayer& childGroup(size_t pos)
    {
        groupLayer_.push_back(getLayerByPos(pos)->get<IGroupLayer>());
        BOOST_REQUIRE(groupLayer_.back());
        return *this;
    }

    TestGroupLayer& endGroup()
    {
        BOOST_REQUIRE(!groupLayer_.empty());
        groupLayer_.pop_back();
        return *this;
    }

private:
    void setContext(const ILayer& layer, bool condition) const
    {
        if (!condition)
            BOOST_TEST_MESSAGE("Layer " + base::ws2s(layer.name()));
    }

    ILayerPtr getLayerByPos(size_t pos) const
    {
        auto layer = groupLayer_.back();
        setContext(*layer, pos < groupLayer_.back()->childCount());
        return layer->getLayerByPos(int(pos));
    }

    ILayerPtr layer_;
    std::vector<IGroupLayer*> groupLayer_;
};

void verifyChecksum(const mapcompiler::Id2Checksum& current, const std::string& filename)
{
    auto expected = readChecksum(filename);
    bool fail = false;
    for (const auto& item : expected) {
        auto i = current.find(item.first);
        auto value = i == current.end() ? 0 : i->second;
        BOOST_CHECK_EQUAL(value, item.second);
        fail |= value != item.second;
    }
    if (fail)
        writeChecksum(current, filename);
}

} // anonymous

BOOST_AUTO_TEST_SUITE( layers_compiler_test )

BOOST_AUTO_TEST_CASE( train_stations_map_compilation )
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/moscow_train_stations.xml",
        "tmp/trains.static.xml",
        "tests/boost-tests/data/",
        false, // for navi
        false, // use RG
        true, // update extent
        false, // isolate transaction
        true, // pack files
        optionsMsk,
        13, // zmin
        17, // zmax
        centerOfMoscowWindow(),
    };

    auto result = compile(cmp);
    verifyChecksum(result.checksum, "trains.chk");

    auto group0 = result.rootLayer->cast<IGroupLayer>();
    BOOST_CHECK_EQUAL(group0->children().size(), 2); // GEOM_GROUP & TEXT_GROUP
    BOOST_CHECK_EQUAL(group0->name().compare(L"ROOT"), 0);

    auto group1 = group0->children().front()->cast<IGroupLayer>();
    BOOST_CHECK_EQUAL(group1->children().size(), 1); // ICON
    BOOST_CHECK_EQUAL(group1->name().compare(L"GEOM_GROUP"), 0);

    auto group2 = group1->children().front()->cast<IGroupLayer>();
    BOOST_CHECK_EQUAL(group2->children().size(), 1); // Auto
    BOOST_CHECK_EQUAL(group2->name().compare(L"ICON"), 0);

    auto group3 = group2->children().front()->cast<IGroupLayer>();
    BOOST_CHECK_EQUAL(group3->children().size(), 2); // Vokzal & Dom
    BOOST_CHECK_EQUAL(group3->name().compare(L"Auto"), 0);

    {
        auto rs = group3->children().front()->renderStyle();
        const auto& pointRS
            = reinterpret_cast<const styles::PointRenderStyle&>(*rs);

        std::string locationHave = pointRS.symbol().filename();
        std::string locationNeed = URN_TARBALL +
            ":trains.static.xml.data.tar:resources/NPGAWDR4OEXBROBGCRKOXMUOHKOY4RAA";

        BOOST_CHECK_EQUAL(locationHave, locationNeed);
    }

    {
        auto rs = group3->children().back()->renderStyle();
        const auto& pointRS
            = reinterpret_cast<const styles::PointRenderStyle&>(*rs);

        std::string locationHave = pointRS.symbol().filename();
        std::string locationNeed = URN_TARBALL +
            ":trains.static.xml.data.tar:resources/Y6CQ2IBOWO2E5XWJU62CRRNM3OMUSSKV";

        BOOST_CHECK_EQUAL(locationHave, locationNeed);
    }

}

BOOST_AUTO_TEST_CASE( navi_map_compilation )
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/moscow_day4navi.xml",
        "tmp/navi.static.xml",
        "",
        true, // for navi
        false, // use RG
        true, // update extent
        false, // isolate transaction
        false, // pack files
        optionsMsk,
        13, // zmin
        17, // zmax
        centerOfMoscowWindow(),
    };

    auto result = compile(cmp);
    verifyChecksum(result.checksum, "navi.chk");
}

BOOST_AUTO_TEST_CASE( extruded_map_compilation )
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/moscow_extrusion.xml",
        "tmp/extr.static.xml",
        "",
        false, // for navi
        false, // use RG
        true, // update extent
        false, // isolate transaction
        false, // pack files
        optionsMsk,
        8, // zmin
        18, // zmax
        centerOfMoscowWindow(),
    };

    auto result = compile(cmp);
    // yegor: TODO: why on Trusty results differ?

    verifyChecksum(result.checksum, "extr.chk");
}

BOOST_AUTO_TEST_CASE( krym_compilation )
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/krym.xml",
        "tmp/krym.static.xml",
        "",
        false, // for navi
        false, // use RG
        true, // update extent
        false, // isolate transaction
        false, // pack files
        optionsRenderer,
        4, // zmin
        6, // zmax
        krymWindow(),
    };

    auto result = compile(cmp);
    verifyChecksum(result.checksum, "krym.chk");

    auto root = result.rootLayer->cast<IGroupLayer>();
    BOOST_REQUIRE_EQUAL(root->childCount(), 2);

    auto geometry = root->getLayerByPos(0)->cast<IGroupLayer>();
    BOOST_CHECK_EQUAL(
        base::ws2s(geometry->name()),
        base::ws2s(mapcompiler::PredefinedNames::geometry));
    BOOST_REQUIRE_EQUAL(geometry->childCount(), 1);

    TestGroupLayer(geometry->getLayerByPos(0)).
        nameExpected("polygon").
        childCountExpected(6).
        childGroup(0).
            // zoom range, regions, disable_if_region
            nameExpected("russia_OTHER"). // disable_if_region = US
            childCountExpected(6).
            testChildRegion(0, "russia_OTHER_4-5_OTHER",  mkZr(4, 5), nullptr, "RU US").
            testChildRegion(1, "russia_OTHER_4-5_RU",     mkZr(4, 5), "RU",    "US").
            testChildRegion(2, "russia_OTHER_4-5_US",     mkZr(4, 5), "US",    "US").    // never visible
            testChildRegion(3, "russia_OTHER_6_OTHER",    mkZr(6, 6), nullptr, "RU US").
            testChildRegion(4, "russia_OTHER_6_RU",       mkZr(6, 6), "RU",    "US").
            testChildRegion(5, "russia_OTHER_6_US",       mkZr(6, 6), "US",    "US").    // never visible
        endGroup().
        childGroup(1).
            // zoom range, regions, enable_if_region
            nameExpected("russia_US"). // enable_if_region = US
            childCountExpected(6).
            testChildRegion(0, "russia_US_4-5_OTHER",     mkZr(4, 5), "US",    "RU US"). // never visible
            testChildRegion(1, "russia_US_4-5_RU",        mkZr(4, 5), "",      nullptr). // never visible
            testChildRegion(2, "russia_US_4-5_US",        mkZr(4, 5), "US",    nullptr).
            testChildRegion(3, "russia_US_6_OTHER",       mkZr(6, 6), "US",    "RU US"). // never visible
            testChildRegion(4, "russia_US_6_RU",          mkZr(6, 6), "",      nullptr). // never visible
            testChildRegion(5, "russia_US_6_US",          mkZr(6, 6), "US",    nullptr).
        endGroup().
        childGroup(2).
            // zoom range, regions
            nameExpected("russia_regions").
            childCountExpected(6).
            testChildRegion(0, "russia_regions_5",        mkZr(5, 5), nullptr, nullptr).
            testChildRegion(1, "russia_regions_5_RU",     mkZr(5, 5), "RU",    nullptr).
            testChildRegion(2, "russia_regions_5_US",     mkZr(5, 5), "US",    nullptr).
            testChildRegion(3, "russia_regions_6",        mkZr(6, 6), nullptr, nullptr).
            testChildRegion(4, "russia_regions_6_RU",     mkZr(6, 6), "RU",    nullptr).
            testChildRegion(5, "russia_regions_6_US",     mkZr(6, 6), "US",    nullptr).
        endGroup().
        childGroup(3).
            // regions
            nameExpected("ukraine").
            childCountExpected(3).
            testChildRegion(0, "ukraine_OTHER",           mkZr(4, 6), nullptr, "RU US").
            testChildRegion(1, "ukraine_RU",              mkZr(4, 6), "RU",    nullptr).
            testChildRegion(2, "ukraine_US",              mkZr(4, 6), "US",    nullptr).
        endGroup().
        childGroup(4).
            // zoom range, regions
            nameExpected("ukraine_regions").
            childCountExpected(8).
            testChildRegion(0, "ukraine_regions_5",       mkZr(5, 5), nullptr, nullptr).
            testChildRegion(1, "ukraine_regions_5_OTHER", mkZr(5, 5), nullptr, "RU US").
            testChildRegion(2, "ukraine_regions_5_RU",    mkZr(5, 5), "RU",    nullptr).
            testChildRegion(3, "ukraine_regions_5_US",    mkZr(5, 5), "US",    nullptr).
            testChildRegion(4, "ukraine_regions_6",       mkZr(6, 6), nullptr, nullptr).
            testChildRegion(5, "ukraine_regions_6_OTHER", mkZr(6, 6), nullptr, "RU US").
            testChildRegion(6, "ukraine_regions_6_RU",    mkZr(6, 6), "RU",    nullptr).
            testChildRegion(7, "ukraine_regions_6_US",    mkZr(6, 6), "US",    nullptr).
        endGroup().
        childGroup(5).
            // zoom range
            nameExpected("belarus").
            childCountExpected(2).
            testChildRegion(0, "belarus_4-5",             mkZr(4, 5), nullptr, nullptr).
            testChildRegion(1, "belarus_6",               mkZr(6, 6), nullptr, nullptr).
        endGroup();

    auto locales = core::getSupportedLocales();

    TestGroupLayer tester(root->getLayerByPos(1));
    tester.nameExpected(mapcompiler::PredefinedNames::text);
    tester.childCountExpected(locales.size());
    for (size_t i = 0; i < locales.size(); ++i)
        tester.testChildLocale(i, "LABELSET_" + locales[i], locales[i].c_str());
}

BOOST_AUTO_TEST_CASE(krym_no_regions_compilation)
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/krym_no_regions.xml",
        "tmp/krym_no_regions.static.xml",
        "",
        false, // for navi
        false, // use RG
        true, // update extent
        false, // isolate transaction
        false, // pack files
        optionsRenderer,
        4, // zmin
        6, // zmax
        krymWindow(),
    };

    auto result = compile(cmp);
    verifyChecksum(result.checksum, "krym.chk");

    auto root = result.rootLayer->cast<IGroupLayer>();
    BOOST_REQUIRE_EQUAL(root->childCount(), 2);

    auto geometry = root->getLayerByPos(0)->cast<IGroupLayer>();
    BOOST_CHECK_EQUAL(
        base::ws2s(geometry->name()),
        base::ws2s(mapcompiler::PredefinedNames::geometry));
    BOOST_REQUIRE_EQUAL(geometry->childCount(), 1);

    auto locales = core::splitByLang(core::getSupportedLocales());
    TestGroupLayer tester(root->getLayerByPos(1));
    tester.nameExpected(mapcompiler::PredefinedNames::text);
    tester.childCountExpected(locales.size());
    for (size_t i = 0; i < locales.size(); ++i) {
        BOOST_REQUIRE(!locales[i].empty());
        std::string localeList = locales[i][0];
        for (size_t j = 1; j < locales[i].size(); ++j)
            localeList += " " + locales[i][j];
        tester.testChildLocale(i, "LABELSET_" + locales[i][0], localeList.c_str());
    }
}

BOOST_AUTO_TEST_CASE( krym_rg_compilation )
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/krym.xml",
        "tmp/krym_rg.static.xml",
        "",
        false, // for navi
        true, // use RG
        true, // update extent
        false, // isolate transaction
        false, // pack files
        optionsRenderer,
        4, // zmin
        6, // zmax
        krymWindow(),
    };

    auto result = compile(cmp);
    verifyChecksum(result.checksum, "krym_rg.chk");

    auto root = result.rootLayer->cast<IGroupLayer>();
    BOOST_REQUIRE_EQUAL(root->childCount(), 2);

    auto geometry = root->getLayerByPos(0)->cast<IGroupLayer>();
    BOOST_CHECK_EQUAL(
        base::ws2s(geometry->name()),
        base::ws2s(mapcompiler::PredefinedNames::geometry));
    BOOST_REQUIRE_EQUAL(geometry->childCount(), 1);

    TestGroupLayer(geometry->getLayerByPos(0)).
        nameExpected("polygon").
        childCountExpected(6).
        childGroup(0).
            // zoom range, regions, disable_if_region
            nameExpected("russia_OTHER"). // disable_if_region = US
            childCountExpected(9).
            testChildRegion(0, "russia_OTHER_4_OTHER",    mkZr(4, 4), nullptr, "RU US").
            testChildRegion(1, "russia_OTHER_4_RU",       mkZr(4, 4), "RU",    "US").
            testChildRegion(2, "russia_OTHER_4_US",       mkZr(4, 4), "US",    "US").    // never visible
            testChildRegion(3, "russia_OTHER_5_OTHER",    mkZr(5, 5), nullptr, "RU US").
            testChildRegion(4, "russia_OTHER_5_RU",       mkZr(5, 5), "RU",    "US").
            testChildRegion(5, "russia_OTHER_5_US",       mkZr(5, 5), "US",    "US").    // never visible
            testChildRegion(6, "russia_OTHER_6_OTHER",    mkZr(6, 6), nullptr, "RU US").
            testChildRegion(7, "russia_OTHER_6_RU",       mkZr(6, 6), "RU",    "US").
            testChildRegion(8, "russia_OTHER_6_US",       mkZr(6, 6), "US",    "US").    // never visible
        endGroup().
        childGroup(1).
            // zoom range, regions, enable_if_region
            nameExpected("russia_US"). // enable_if_region = US
            childCountExpected(9).
            testChildRegion(0, "russia_US_4_OTHER",       mkZr(4, 4), "US",    "RU US"). // never visible
            testChildRegion(1, "russia_US_4_RU",          mkZr(4, 4), "",      nullptr). // never visible
            testChildRegion(2, "russia_US_4_US",          mkZr(4, 4), "US",    nullptr).
            testChildRegion(3, "russia_US_5_OTHER",       mkZr(5, 5), "US",    "RU US"). // never visible
            testChildRegion(4, "russia_US_5_RU",          mkZr(5, 5), "",      nullptr). // never visible
            testChildRegion(5, "russia_US_5_US",          mkZr(5, 5), "US",    nullptr).
            testChildRegion(6, "russia_US_6_OTHER",       mkZr(6, 6), "US",    "RU US"). // never visible
            testChildRegion(7, "russia_US_6_RU",          mkZr(6, 6), "",      nullptr). // never visible
            testChildRegion(8, "russia_US_6_US",          mkZr(6, 6), "US",    nullptr).
        endGroup().
        childGroup(2).
            // zoom range, regions
            nameExpected("russia_regions").
            childCountExpected(6).
            testChildRegion(0, "russia_regions_5",        mkZr(5, 5), nullptr, nullptr).
            testChildRegion(1, "russia_regions_5_RU",     mkZr(5, 5), "RU",    nullptr).
            testChildRegion(2, "russia_regions_5_US",     mkZr(5, 5), "US",    nullptr).
            testChildRegion(3, "russia_regions_6",        mkZr(6, 6), nullptr, nullptr).
            testChildRegion(4, "russia_regions_6_RU",     mkZr(6, 6), "RU",    nullptr).
            testChildRegion(5, "russia_regions_6_US",     mkZr(6, 6), "US",    nullptr).
        endGroup().
        childGroup(3).
            // regions
            nameExpected("ukraine").
            childCountExpected(9).
            testChildRegion(0, "ukraine_4_OTHER",         mkZr(4, 4), nullptr, "RU US").
            testChildRegion(1, "ukraine_4_RU",            mkZr(4, 4), "RU",    nullptr).
            testChildRegion(2, "ukraine_4_US",            mkZr(4, 4), "US",    nullptr).
            testChildRegion(3, "ukraine_5_OTHER",         mkZr(5, 5), nullptr, "RU US").
            testChildRegion(4, "ukraine_5_RU",            mkZr(5, 5), "RU",    nullptr).
            testChildRegion(5, "ukraine_5_US",            mkZr(5, 5), "US",    nullptr).
            testChildRegion(6, "ukraine_6_OTHER",         mkZr(6, 6), nullptr, "RU US").
            testChildRegion(7, "ukraine_6_RU",            mkZr(6, 6), "RU",    nullptr).
            testChildRegion(8, "ukraine_6_US",            mkZr(6, 6), "US",    nullptr).
        endGroup().
        childGroup(4).
            // zoom range, regions
            nameExpected("ukraine_regions").
            childCountExpected(8).
            testChildRegion(0, "ukraine_regions_5",       mkZr(5, 5), nullptr, nullptr).
            testChildRegion(1, "ukraine_regions_5_OTHER", mkZr(5, 5), nullptr, "RU US").
            testChildRegion(2, "ukraine_regions_5_RU",    mkZr(5, 5), "RU",    nullptr).
            testChildRegion(3, "ukraine_regions_5_US",    mkZr(5, 5), "US",    nullptr).
            testChildRegion(4, "ukraine_regions_6",       mkZr(6, 6), nullptr, nullptr).
            testChildRegion(5, "ukraine_regions_6_OTHER", mkZr(6, 6), nullptr, "RU US").
            testChildRegion(6, "ukraine_regions_6_RU",    mkZr(6, 6), "RU",    nullptr).
            testChildRegion(7, "ukraine_regions_6_US",    mkZr(6, 6), "US",    nullptr).
        endGroup().
        childGroup(5).
            // zoom range
            nameExpected("belarus").
            childCountExpected(3).
            testChildRegion(0, "belarus_4",               mkZr(4, 4), nullptr, nullptr).
            testChildRegion(1, "belarus_5",               mkZr(5, 5), nullptr, nullptr).
            testChildRegion(2, "belarus_6",               mkZr(6, 6), nullptr, nullptr).
        endGroup();

    auto locales = core::getSupportedLocales();

    TestGroupLayer tester(root->getLayerByPos(1));
    tester.nameExpected(mapcompiler::PredefinedNames::text);
    tester.childCountExpected(locales.size());
    for (size_t i = 0; i < locales.size(); ++i)
        tester.testChildLocale(i, "LABELSET_" + locales[i], locales[i].c_str());
}

BOOST_AUTO_TEST_CASE(zoom_range_compilation_z4_5)
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/zoom_range.xml",
        "tmp/zoom_range.static.xml",
        "",
        false, // for navi
        false, // use RG
        true, // update extent
        false, // isolate transaction
        false, // pack files
        optionsRenderer,
        4, // zmin
        5, // zmax
        bigWindow(),
    };

    auto result = compile(cmp);
    verifyChecksum(result.checksum, "zoom_range_4-5.chk");

    TestGroupLayer(result.rootLayer).
        childCountExpected(2).
        childGroup(0).
            nameExpected(mapcompiler::PredefinedNames::geometry).
            childCountExpected(3).
            testChild(0, "russia5z", mkZr(5, 5)).
            testChild(1, "russia5z_ex", mkZr(5, 5)).
            testChild(2, "russia5-8z", mkZr(5, 5)).
        endGroup().
        childGroup(1).
            nameExpected(mapcompiler::PredefinedNames::text).
            childCountExpected(0).
        endGroup();
}

BOOST_AUTO_TEST_CASE(zoom_range_compilation_z4_6)
{
    CompilationSetting cmp = {
        "tests/boost-tests/maps/zoom_range.xml",
        "tmp/zoom_range.static.xml",
        "",
        false, // for navi
        false, // use RG
        true, // update extent
        false, // isolate transaction
        false, // pack files
        optionsRenderer,
        4, // zmin
        6, // zmax
        bigWindow(),
    };

    auto result = compile(cmp);
    verifyChecksum(result.checksum, "zoom_range_4-6.chk");

    TestGroupLayer(result.rootLayer).
        childCountExpected(2).
        childGroup(0).
            nameExpected(mapcompiler::PredefinedNames::geometry).
            childCountExpected(5).
            testChild(0, "russia6z", mkZr(6, 6)).
            testChild(1, "russia5z", mkZr(5, 5)).
            testChild(2, "russia5z_ex", mkZr(5, 5)).
            testChild(3, "russia6z_ex", mkZr(6, 6)).
            childGroup(4).
                nameExpected("russia5-8z").
                childCountExpected(2).
                testChild(0, "russia5-8z_5", mkZr(5, 5)).
                testChild(1, "russia5-8z_6", mkZr(6, 6)).
            endGroup().
        endGroup().
        childGroup(1).
            nameExpected(mapcompiler::PredefinedNames::text).
            childCountExpected(0).
        endGroup();
}

BOOST_AUTO_TEST_SUITE_END()

} } } // maps::renderer5::test
