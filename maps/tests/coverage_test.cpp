#include "common.h"
#include "../test_tools/test_author.h"

#include <yandex/maps/coverage5/coverage.h>
#include <yandex/maps/coverage5/layer.h>
#include <yandex/maps/coverage5/region.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/bounding_box.h>

#include <maps/libs/xml/include/xml.h>

#include <boost/optional.hpp>
#include <boost/test/unit_test.hpp>

#include <iostream>
#include <memory>
#include <algorithm>
#include <string>
#include <vector>
#include <map>

using namespace maps::coverage5;

using maps::geolib3::Point2;
using maps::geolib3::BoundingBox;

using maps::coverage5::test::TestAuthor;
using maps::coverage5::test::TestAuthors;

struct RegionsOrderingTestData {
    std::string layerName;
    std::string pointDescription;
    Point2 coord;
    boost::optional<Zoom> zoom;
    std::vector<RegionId> regions;
}
    g_regionsOrdegingTests[] =
{
    {"geoid", "Moscow", {37.614924, 55.75298}, boost::none, {213, 225} },
    {"geoid", "Moscow obl", {37.306299, 56.185361}, boost::none, {1, 225} },
    {"geoid", "Spb", {30.348842, 59.922735}, boost::none, {2, 10174, 225} },
    {"trf", "Spb", {30.348842, 59.922735}, boost::none, {10174} },
    {"geoid", "Ekb", {60.596572, 56.8220}, boost::none, {54, 225} },
    {"geoid", "N. Novgorod", {43.995539, 56.31316}, boost::none, {47, 225} },
    {"geoid", "Nikolaiv", {32.0, 47.0}, boost::none, {148, 187} },
    {"trf", "Nikolaiv", {32.0, 47.0}, boost::none, {148} }
};

struct RegionsTestData {
    std::string layerName;
    boost::optional<Zoom> zoom;
    size_t count;
}
    g_regionsTests[] =
{
    {"geoid", boost::none, 133},
    {"trf", boost::none, 2},
    {"stl", boost::none, 308},
    {"stl", 20ul, 1},
    {"stl", 18ul, 247},
    {"geoid", 18ul, 133},
    {"trf", 18, 1}
};

const double BBOX_EPS = 1e-5;
const double AREA_EPS = 0.1;

struct RegionsByPointZoomTestData {
    std::string layerName;
    std::string pointDescription;
    Point2 coord;
    boost::optional<Zoom> zoom;
    size_t regionsCount;
}
    g_regionsByPointZoomTests[] =
{
    {"stl", "--", {36.96, 55.72}, 18ul, 1}
};

struct RegionsByPointTestData {
    std::string layerName;
    std::string pointDescription;
    Point2 coord;
    boost::optional<Zoom> zoom;
    std::vector<RegionId> regions;
    boost::optional<size_t> firstPolygonsNumber;
    boost::optional<BoundingBox> firstBBox;
    boost::optional<double> firstArea;
}
    g_regionsByPointTests[] =
{
    {"geoid", "Lisiy Nos, Spb", {30.0, 60.0}, boost::none, {2, 10174},
        1,
        {{{29.42578476, 59.6338075991}, {30.75929208, 60.2427815009}}},
        0.365},
    {"trf", "Lisiy Nos, Spb", {30.0, 60.0}, boost::none, {10174},
        1,
        {{{27.73411416, 58.4174996512}, {35.6954742, 61.3306487897}}},
        boost::none},
    {"trf", "Nikolaiv", {32.0, 47.0}, boost::none, {148},
        1,
        {{{31.88070792, 46.8185380555}, {32.13528588, 47.0475909512}}},
        boost::none},
    {"geoid", "Voronezh", {39.226, 51.703}, boost::none, {193},
        2,
        boost::none,
        boost::none},
    {"geoid", "Krasnoselskoye, Voronezh. obl", {39.595, 51.877}, boost::none, {193},
        boost::none,
        boost::none,
        boost::none},
    {"geoid", "Voronezhskaya obl", {39.4843, 51.8301}, boost::none, {10672},
        boost::none,
        boost::none,
        boost::none}
};

typedef std::map<std::string, std::string> VersionMap;
typedef std::map<std::string, std::map<std::string, std::string> >
    LayersVersionMap;

LayersVersionMap g_layersVersions =
{
    { "libcoverage1.xml", VersionMap { {"geoid", "1.0"} } },
    { "libcoverage1", VersionMap { {"geoid", "1.0"} } },
    { "libcoverage2.xml", VersionMap {
        {"geoid", "1.0"},
        {"stl", "2.0"},
        {"trf", "3.0"}
    } },
    { "libcoverage2", VersionMap {
        {"geoid", "1.0"},
        {"stl", "2.0"},
        {"trf", "3.0"}
    } },
    { "libcoverage3.xml", VersionMap { {"stl", "2.0"} } },
    { "libcoverage3", VersionMap { {"stl", "2.0"} } },
    { "libcoverage4.xml", VersionMap {
        {"geoid", "1.1.0-0"},
        {"stl", "2.0"},
        {"trf", "3.0"}
    } },
    { "libcoverage4", VersionMap {
        {"geoid", "1.1.0-0"},
        {"stl", "2.0"},
        {"trf", "3.0"}
    } }
};

std::string g_missingRefTestData[] =
{
    {"missing_ref"},
    {"missing_ref.xml"}
};

struct LayersAdditionData {
    std::string path;
    LayersNames addedNames;
};


typedef std::vector<LayersAdditionData> MultipleAdditionTestData;

MultipleAdditionTestData g_multipleAdditionTestData[] =
{
    MultipleAdditionTestData {
        LayersAdditionData {"mms/geoid.mms.1", LayersNames {"geoid"}},
        LayersAdditionData {"mms/libstl.mms.1", LayersNames {"stl"}},
        LayersAdditionData {"libcoverage2", LayersNames {"trf"}}
    }
};

struct ZoomTestData {
    std::string layerName;
    boost::optional<Zoom>
        (Layer::*zoomComputer)(const Point2&) const;
    Point2 coord;
    boost::optional<Zoom> correctZoom;
}
    g_zoomTests[] =
{
    {"stl", &Layer::maxZoom, {29.7, 59.65}, 18ul},
    {"stl", &Layer::maxZoom, {-179.0, -84.084059}, 12ul},
    {"stl", &Layer::maxZoom, {-180.0, -185.084059}, boost::none},
    {"stl", &Layer::minZoom, {29.7, 59.65}, 0ul},
    {"trf", &Layer::minZoom, {-179.0, -84.084059}, boost::none},
    {"trf", &Layer::minZoom, {32.0, 47.0}, 12ul},
    {"trf", &Layer::maxZoom, {32.0, 47.0}, 17ul},
};

struct BBoxZoomTestData {
    std::string layerName;
    boost::optional<Zoom>
        (Layer::*zoomComputer)(const BoundingBox&) const;
    BoundingBox bbox;
    boost::optional<Zoom> correctZoom;
}
    g_bboxZoomTests[] =
{
    {"stl", &Layer::maxZoom, {{30.286002, 59.834371}, {30.438059, 59.967162}}, 19ul},
    {"stl", &Layer::maxZoom, {{-179.1, -84.184059}, {-178.9, -83.984059}}, 12ul},
    {"stl", &Layer::maxZoom, {{-180.0, -185.084059}, {180.0, -91.0}}, boost::none},
    {"stl", &Layer::minZoom, {{30.286002, 59.834371}, {30.438059, 59.967162}}, 0ul},
    {"trf", &Layer::minZoom, {{-179.0, -84.084059}, {179.0, -74.084059}}, boost::none},
    {"trf", &Layer::minZoom, {{31.0, 46.0}, {33.0, 48.0}}, 12ul},
    {"trf", &Layer::maxZoom, {{31.0, 46.0}, {33.0, 48.0}}, 17ul},
};

struct BBoxCoverageTestData {
    std::string layerName;
    BoundingBox bbox;
    boost::optional<Zoom> zoom;
    bool coverageExists;
}
    g_bboxCoverageTests[] =
{
    {"stl", {{20.0, 30.0}, {40.0, 50.0}}, 18ul, true},
    {"geoid", {{20.0, 30.0}, {40.0, 60.0}}, 0ul, true},
    {"geoid", {{20.0, 30.0}, {40.0, 60.0}}, 12ul, true},
    {"geoid", {{20.0, 30.0}, {40.0, 60.0}}, 23ul, true},
    {"trf", {{31.0, 46.0}, {33.0, 48.0}}, 11ul, false},
    {"trf", {{31.0, 46.0}, {33.0, 48.0}}, 12ul, true},
    {"trf", {{31.0, 46.0}, {33.0, 48.0}}, 17ul, true},
    {"trf", {{31.0, 46.0}, {33.0, 48.0}}, 18ul, false},
    {"trf", {{31.0, 46.0}, {33.0, 48.0}}, 12ul, true},
    {"trf", {{31.0, 46.0}, {33.0, 48.0}}, 17ul, true},
    {"trf", {{29.0, 59.0}, {30.0, 60.0}}, 0ul, true},
    {"trf", {{29.0, 59.0}, {30.0, 60.0}}, 23ul, true},
    {"trf", {{29.0, 59.0}, {30.0, 60.0}}, 9ul, true},
    {"trf", {{29.0, 59.0}, {30.0, 60.0}}, 20ul, true},
    // edges checks
    {"trf", {{28.597036, 58.594893}, {29.301534, 58.774017}}, 11ul, true}
};

struct AuthorTestData {
    std::string layer;
    boost::optional<Zoom> zoom;
    TestAuthors authors;
}
    g_authorsTests[] =
{
    {"trf", boost::none, TestAuthors { TestAuthor {
        {"dicentra"},
        {"http://staff.yandex-team.ru/dicentra"},
        {"dicentra@yandex-team.ru", "anastasia.petrushkina@gmail.com"}
        } }
    }
};

struct LocaleMinZoomTest {
    std::string layer;
    Point2 point;
    boost::optional<Zoom> minZoom;
}
    g_localeMinZoomTests[] =
{
    {"trf", {30.0, 60.0}, 0ul}
};

struct LocaleVersionTestData {
    std::string layer;
    std::string version;
}
    g_localeVersionTests[] =
{
    {"trf", "3.0"}
};

std::vector<RegionId> getIDs(const Regions& regions)
{
    std::vector<RegionId> ids;
    ids.reserve(regions.size());
    for (const Region& region: regions) {
        ids.push_back(*region.id());
    }
    return ids;
}

void checkConstruction(const Coverage& cov,
    const LayersVersionMap::mapped_type& layers, const LayersNames& names)
{
    BOOST_CHECK_EQUAL(names.size(), layers.size());
    for (auto layer: layers) {
        BOOST_CHECK(
            std::find(names.begin(), names.end(), layer.first) !=
                names.end());
        BOOST_CHECK_EQUAL(cov[layer.first].version(), layer.second);
    }
}

BOOST_AUTO_TEST_CASE(test_construction)
{
    setTestsDataCwd();
    for (auto coverage: g_layersVersions) {
        {
            Coverage cov(SpatialRefSystem::Geodetic);
            const LayersNames& names = cov.addLayers(coverage.first);
            checkConstruction(cov, coverage.second, names);
        }
        {
            Coverage cov(coverage.first, SpatialRefSystem::Geodetic);
            const LayersNames& names = cov.layersNames();
            checkConstruction(cov, coverage.second, names);
        }
    }
    for (auto missingRefCov: g_missingRefTestData) {
        BOOST_CHECK_THROW(
            CoverageTest::createCoverage(missingRefCov),
            maps::Exception);
        Coverage cov(SpatialRefSystem::Geodetic);
        BOOST_CHECK_THROW(cov.addLayers(missingRefCov), maps::Exception);
    }
}

BOOST_AUTO_TEST_CASE(test_multiple_addition)
{
    setTestsDataCwd();
    for (auto additionTest: g_multipleAdditionTestData) {
        Coverage cov(SpatialRefSystem::Geodetic);
        for (auto additionData: additionTest) {
            LayersNames names = cov.addLayers(additionData.path);
            std::sort(names.begin(), names.end());
            BOOST_CHECK(names == additionData.addedNames);
        }
    }
}

BOOST_AUTO_TEST_CASE(test_regions)
{
    setTestsDataCwd();
    Coverage cov("libcoverage2.xml", SpatialRefSystem::Geodetic);

    for (auto rTest: g_regionsTests) {
        const Regions& r = cov[rTest.layerName].regions(rTest.zoom);
        BOOST_CHECK_EQUAL(r.size(), rTest.count);
    }

    for (auto rTest: g_regionsByPointZoomTests) {
        const Regions& r =
            cov[rTest.layerName].regions(rTest.coord, rTest.zoom);
        BOOST_CHECK(r.size() == rTest.regionsCount);
    }

    for (auto rTest: g_regionsByPointTests) {
        const Regions& r =
            cov[rTest.layerName].regions(rTest.coord, rTest.zoom);
        BOOST_CHECK(getIDs(r) == rTest.regions);
        if (rTest.firstPolygonsNumber) {
            BOOST_CHECK_EQUAL(
                r.front().geoms().polygonsNumber(),
                *rTest.firstPolygonsNumber);
        }
        if (rTest.firstBBox) {
            BoundingBox bbox = r.front().geoms().boundingBox();
            BOOST_CHECK_CLOSE(bbox.minX(), (*rTest.firstBBox).minX(), BBOX_EPS);
            BOOST_CHECK_CLOSE(bbox.minY(), (*rTest.firstBBox).minY(), BBOX_EPS);
            BOOST_CHECK_CLOSE(bbox.maxX(), (*rTest.firstBBox).maxX(), BBOX_EPS);
            BOOST_CHECK_CLOSE(bbox.maxY(), (*rTest.firstBBox).maxY(), BBOX_EPS);
        }
        if (rTest.firstArea) {
            BOOST_CHECK_CLOSE(r.front().area(), *rTest.firstArea, AREA_EPS);
        }
        if (r.size() > 0) {
            std::string metadata(r.front().metaData());
            if (!metadata.empty()) {
                maps::xml3::Doc doc(maps::xml3::Doc::fromString(metadata));
                doc.root();
            }
        }
    }
}

BOOST_AUTO_TEST_CASE(test_regions_ordering_and_min_area)
{
    setTestsDataCwd();
    Coverage cov("libcoverage4.xml", SpatialRefSystem::Geodetic);

    for (const RegionsOrderingTestData& testCase:
        g_regionsOrdegingTests)
    {
        Regions regions = cov[testCase.layerName].regions(
            testCase.coord, testCase.zoom);
        BOOST_CHECK(getIDs(regions) == testCase.regions);
        RegionId minAreaId = *(*cov[testCase.layerName].minAreaRegion(
            testCase.coord, testCase.zoom)).id();
        BOOST_CHECK_EQUAL(minAreaId, testCase.regions.front());
    }
}

BOOST_AUTO_TEST_CASE(test_zoom_range)
{
    setTestsDataCwd();
    Coverage cov("libcoverage2.xml", SpatialRefSystem::Geodetic);

    for (auto zoomTest: g_zoomTests) {
        boost::optional<Zoom> zoom =
            (cov[zoomTest.layerName].*(zoomTest.zoomComputer))(
                zoomTest.coord);
        BOOST_CHECK((!zoom && !zoomTest.correctZoom) ||
            (zoom && zoomTest.correctZoom && *zoom == *zoomTest.correctZoom));
    }

    for (auto zoomTest: g_bboxZoomTests) {
        boost::optional<Zoom> zoom =
            (cov[zoomTest.layerName].*(zoomTest.zoomComputer))(
                zoomTest.bbox);
        BOOST_CHECK((!zoom && !zoomTest.correctZoom) ||
            (zoom && zoomTest.correctZoom && *zoom == *zoomTest.correctZoom));
    }
}

BOOST_AUTO_TEST_CASE(test_loading_legacy_versions)
{
    setTestsDataCwd();
    Coverage cov("legacy/map.mms.1", SpatialRefSystem::Geodetic);
    BOOST_CHECK(cov["map"].version() == "2.45.0");
}

BOOST_AUTO_TEST_CASE(test_metadata_access)
{
    setTestsDataCwd();
    Coverage cov("libcoverage2.xml", SpatialRefSystem::Geodetic);

    boost::optional<std::string> metadata = cov["trf"].metadata();
    BOOST_CHECK(metadata != boost::none);
    BOOST_CHECK(metadata->find("<scaled>true</scaled>") != std::string::npos);
}

BOOST_AUTO_TEST_CASE(test_coverage_by_bbox)
{
    setTestsDataCwd();
    std::unique_ptr<Coverage> pcov(
        CoverageTest::createCoverage("libcoverage2.xml"));
    Coverage& cov = *pcov;

    for (auto test: g_bboxCoverageTests) {
        bool coverageExists = cov[test.layerName].exists(test.bbox, test.zoom);
        BOOST_CHECK(coverageExists == test.coverageExists);
    }
}

BOOST_AUTO_TEST_CASE(test_author)
{
    setTestsDataCwd();
    using maps::coverage5::test::checkAuthors;

    Coverage cov("libcoverage2.xml", SpatialRefSystem::Geodetic);

    for (auto authorTest: g_authorsTests) {
        Regions r = cov[authorTest.layer].regions(authorTest.zoom);
        BOOST_CHECK(!r.empty());
        checkAuthors(authorTest.authors, r.front().authors());
    }
}

BOOST_AUTO_TEST_CASE(test_locale_min_zoom)
{
    setTestsDataCwd();
    std::unique_ptr<Coverage> pcov(
        CoverageTest::createCoverage("libcoverage2.xml"));
    Coverage& cov = *pcov;
    for (auto localeTest: g_localeMinZoomTests) {
        boost::optional<Zoom> minZoom = cov[localeTest.layer].minZoom(localeTest.point);
        BOOST_REQUIRE(minZoom);
        BOOST_REQUIRE(localeTest.minZoom);
        BOOST_CHECK_EQUAL(*minZoom, *localeTest.minZoom);
    }
}

BOOST_AUTO_TEST_CASE(test_localized_version)
{
    setTestsDataCwd();
    std::unique_ptr<Coverage> pcov(
        CoverageTest::createCoverage("libcoverage2.xml"));
    Coverage& cov = *pcov;

    for (auto test: g_localeVersionTests) {
        const std::string& version = cov[test.layer].version();
        BOOST_CHECK_EQUAL(version, test.version);
    }
}
