#include "../lib/common.h"
#include "../lib/context.h"
#include "../lib/object_collector.h"
#include "../lib/object_matcher.h"
#include "../lib/ad_finder.h"
#include "../lib/magic_strings.h"

#include <yandex/maps/wiki/groupedit/session.h>
#include <yandex/maps/wiki/groupedit/object.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <yandex/maps/wiki/revision/filters.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/unittest/localdb.h>
#include <maps/libs/geolib/include/conversion.h>
#include <yandex/maps/log8.h>

#include <boost/test/unit_test.hpp>

#include <library/cpp/testing/unittest/env.h>

#include <fstream>

namespace fs = boost::filesystem;
namespace rf = maps::wiki::revision::filters;

namespace maps {
namespace wiki {
namespace merge {
namespace test {

namespace {

constexpr uint64_t TEST_TASK_ID = 1;
constexpr uint64_t TEST_UID = 1;

using TestRelations = std::map<TObjectId, std::set<TObjectId>>;

struct SetLogLevelFixture
{
    SetLogLevelFixture() { maps::log8::setLevel(maps::log8::Level::FATAL); }
};

struct DbFixture : public SetLogLevelFixture
{
    DbFixture()
        : SetLogLevelFixture()
        , targetDb()
        , sourceDb()
        , targetPool(targetDb.pool())
        , sourcePool(sourceDb.pool())
    {
    }

    unittest::MapsproDbFixture targetDb;
    unittest::MapsproDbFixture sourceDb;

    pgpool3::Pool& targetPool;
    pgpool3::Pool& sourcePool;

    void import(pgpool3::Pool& pool, const std::string& filename);
    void fillContourObjectsGeom(pgpool3::TransactionHandle& targetTxn);

    void compareIds(
        const ObjectIds& realIds,
        const ObjectIds& expectedIds,
        const std::string& prefix);

    void compareRelations(
        const RelationMap& realRelations,
        const TestRelations& expectedRelations,
        const std::string& prefix);

    void doTest(
        const std::string& targetDataFilepath,
        const std::string& sourceDataFilepath,
        const std::vector<TObjectId>& mergeRegionIds,
        const std::set<std::string>& layerNames,
        const ObjectIds& expectedToDelete,
        const ObjectIds& expectedToExport,
        const TestRelations& expectedRelations = {},
        const TestRelations& expectedRoadAdRelations = {},
        const TestRelations& expectedAdAdRelations = {});
};

void DbFixture::import(pgpool3::Pool& pool, const std::string& filename)
{
    auto fullFilename = ArcadiaSourceRoot()
        + "/maps/wikimap/mapspro/services/tasks/grinder/merge_worker/"
        + filename;

    std::ifstream file(fullFilename);
    REQUIRE(file, "Error opening file " << fullFilename);

    revisionapi::RevisionAPI jsonLoader(pool);
    auto commitIds = jsonLoader.importData(
        TEST_UID, revisionapi::IdMode::StartFromJsonId, file);
}

void DbFixture::fillContourObjectsGeom(pgpool3::TransactionHandle& targetTxn)
{
    revision::RevisionsGateway targetGateway(*targetTxn);
    auto targetHeadCommitId = targetGateway.headCommitId();
    auto snapshot = targetGateway.snapshot(targetHeadCommitId);

    auto filter = rf::Attr("cat:ad").defined()
            && rf::ObjRevAttr::isNotRelation()
            && rf::ObjRevAttr::isNotDeleted()
            && !rf::Geom::defined();

    ObjectIds adIds;
    std::map<TObjectId, uint64_t> adIdToLevelKind;

    for (const auto& rev : snapshot.objectRevisionsByFilter(filter)) {
        auto adId = rev.id().objectId();
        adIds.insert(adId);

        const auto& attributes = *rev.data().attributes;
        auto levelKind = boost::lexical_cast<uint64_t>(attributes.at("ad:level_kind"));
        adIdToLevelKind.emplace(adId, levelKind);
    }

    ComplexGeometryCache targetGeometryCache(targetPool, targetHeadCommitId);
    targetGeometryCache.computeGeometries(GeometryCacheCategory::Ad, adIds);

    for (auto adId : adIds) {
        const auto& geom = targetGeometryCache.geometryById(adId);
        if (geom->getGeometryTypeId() == geos::geom::GEOS_POINT) {
            continue;
        }

        std::ostringstream query;
        query << "INSERT INTO vrevisions_trunk.contour_objects_geom "
            << "(object_id, commit_id, the_geom, domain_attrs) VALUES ("
            << adId << ","
            << targetHeadCommitId << ","
            << "ST_GeomFromWKB('" << targetTxn->esc_raw(geom.wkb()) << "', 3395),"
            << "'cat:ad=>1,ad:level_kind=>" + std::to_string(adIdToLevelKind[adId]) + "'::hstore)";

        targetTxn->exec(query.str());
    }
}

void DbFixture::compareIds(
    const ObjectIds& realIds,
    const ObjectIds& expectedIds,
    const std::string& prefix)
{
    for (auto id : realIds) {
        BOOST_CHECK_MESSAGE(expectedIds.count(id), prefix << "wrong object " << id);
    }
    for (auto id : expectedIds) {
        BOOST_CHECK_MESSAGE(realIds.count(id), prefix <<  "missing object " << id);
    }
    BOOST_CHECK_EQUAL(realIds.size(), expectedIds.size());
}

void DbFixture::compareRelations(
    const RelationMap& realRelations,
    const TestRelations& expectedRelations,
    const std::string& prefix)
{
    for (const auto& pair : realRelations) {
        auto it = expectedRelations.find(pair.first);
        BOOST_REQUIRE_MESSAGE(it != expectedRelations.end(),
            prefix << "unexpected relations for object " << pair.first);

        for (const auto& rel : pair.second) {
            BOOST_CHECK_MESSAGE(it->second.count(rel.otherId),
                prefix << "wrong relation from " << pair.first
                << " to " << rel.otherId);
        }
    }
    for (const auto& pair : expectedRelations) {
        auto it = realRelations.find(pair.first);
        BOOST_REQUIRE_MESSAGE(it != realRelations.end(),
            prefix << "missing relations for object " << pair.first);

        const auto& objectRelations = it->second;

        for (const auto& otherId : pair.second) {
            BOOST_CHECK_MESSAGE(std::find_if(objectRelations.begin(), objectRelations.end(),
                [&](const Relation& rel) {
                    return rel.otherId == otherId;
                }) != objectRelations.end(),
                prefix << "missing relation from " << pair.first
                << " to " << otherId);
        }
    }
    BOOST_CHECK_EQUAL(realRelations.size(), expectedRelations.size());
}

void DbFixture::doTest(
    const std::string& targetDataFilepath,
    const std::string& sourceDataFilepath,
    const std::vector<TObjectId>& mergeRegionIds,
    const std::set<std::string>& layerNames,
    const ObjectIds& expectedToDelete,
    const ObjectIds& expectedToExport,
    const TestRelations& expectedRelations,
    const TestRelations& expectedRoadAdRelations,
    const TestRelations& expectedAdAdRelations)
{
    import(targetPool, targetDataFilepath);
    import(sourcePool, sourceDataFilepath);

    auto targetTxn = targetPool.masterWriteableTransaction();
    auto sourceTxn = sourcePool.masterReadOnlyTransaction();

    revision::RevisionsGateway targetGateway(*targetTxn);
    auto targetHeadCommitId = targetGateway.headCommitId();

    revision::RevisionsGateway sourceGateway(*sourceTxn);
    auto sourceHeadCommitId = sourceGateway.headCommitId();

    TaskParams params(
        TEST_TASK_ID,
        TEST_UID,
        Action::Replace,
        mergeRegionIds,
        sourceHeadCommitId);

    tasks::TaskPgLogger logger(targetPool, params.taskId);

    Context context(params,
        targetTxn,
        sourceTxn);

    context.prepareRegionGeoms();

    ObjectCache targetCache;
    ObjectCollector targetCollector(context, targetCache, WorkMode::Delete, logger, LoadMoreAdRdRelations::Yes);
    targetCollector.collect(layerNames);

    ComplexGeometryCache targetGeometryCache(targetPool, targetHeadCommitId);
    targetGeometryCache.buildIndex(targetCache);

    fillContourObjectsGeom(targetTxn);

    auto toDelete = targetCache.query(Selected::Yes).ids();
    compareIds(toDelete, expectedToDelete, "To delete: ");

    ObjectCache sourceCache;
    ObjectCollector sourceCollector(context, sourceCache, WorkMode::Export, logger, LoadMoreAdRdRelations::Yes);
    sourceCollector.collect(layerNames);

    ComplexGeometryCache sourceGeometryCache(sourcePool, sourceHeadCommitId);
    sourceGeometryCache.buildIndex(sourceCache);

    auto toExport = sourceCache.query(Selected::Yes).ids();
    compareIds(toExport, expectedToExport, "To export: ");

    auto sourceToImportIds = context.generateImportIds(toExport);

    context.deleteObjects(toDelete);
    context.exportAndImportObjects(sourceToImportIds, logger);

    ObjectMatcher matcher(targetCache, sourceCache, targetGeometryCache, sourceGeometryCache);
    matcher.computeMatches(layerNames);
    matcher.computeExternalRelations();

    auto newRelations = matcher.computeNewRelations(sourceToImportIds);
    compareRelations(newRelations, expectedRelations, "New relations: ");

    context.addRelations(newRelations);

    auto deletedAdIds = targetCache.query({cat::AD}, Selected::Yes).ids();

    AdFinder finder(targetTxn, context.targetSession(), sourceGeometryCache, sourceToImportIds, deletedAdIds);

    auto sourceRoadIds = sourceCache.query({cat::RD}, Selected::Yes).ids();
    auto roadRelations = finder.createParentAdRelations(GeometryCacheCategory::Rd, sourceRoadIds);
    compareRelations(roadRelations, expectedRoadAdRelations, "Road relations: ");
    context.addRelations(roadRelations);

    auto sourceAdIds = sourceCache.query({cat::AD}, Selected::Yes).ids();
    auto adRelations = finder.createParentAdRelations(GeometryCacheCategory::Ad, sourceAdIds);
    compareRelations(adRelations, expectedAdAdRelations, "Ad relations: ");
    context.addRelations(adRelations);

    targetTxn->commit();
}

} // namespace

BOOST_FIXTURE_TEST_CASE(test_merge_bld, DbFixture)
{
    ObjectIds expectedToDelete = { 61 };
    ObjectIds expectedToExport = { 11, 21 };

    doTest(
        "tests/data/bld-target.json",
        "tests/data/bld-source.json",
        { 31 }, //merge region ids
        { "bld" }, //layer names
        expectedToDelete,
        expectedToExport);
}

BOOST_FIXTURE_TEST_CASE(test_merge_ad, DbFixture)
{
    ObjectIds expectedToDelete = {
        61, //ad
        62, //ad_fc
        63, //ad_nm
        69, //ad_el
        101, //ad
        102, //ad_fc
        103, //ad_nm

        301, //ad_cnt
        291 //ad_cnt
    };

    ObjectIds expectedToExport = {
        151, //ad
        152, //ad_fc
        153, //ad_nm
        191, //ad
        192, //ad_fc
        193, //ad_nm
        251, //ad
        252, //ad_fc
        253, //ad_nm

        //ad_cnt
        211, 221,

        //ad_el
        159, 133, 138, 173, 199, 259, 233, 237, 178,

        //ad_jc
        131, 117, 132, 171, 236, 176, 231,
    };

    TestRelations expectedRelations =
    {
        {395, {21}},
        {387, {21, 181, 261}},
        {405, {21}}
    };

    doTest(
        "tests/data/ad-target.json",
        "tests/data/ad-source.json",
        { 271 }, //merge region ids
        { "ad_5" }, //layer names
        expectedToDelete,
        expectedToExport,
        expectedRelations);
}

BOOST_FIXTURE_TEST_CASE(test_merge_rd, DbFixture)
{
    ObjectIds expectedToDelete = {
        //rd_jc
        24, 25, 32, 33,

        //rd_el
        26, 34, 38, 42,

        //rd
        51,

        //rd_nm
        52,

        //cond
        101
    };

    ObjectIds expectedToExport = {
        //rd_jc
        24, 25, 34, 35, 36, 54,

        //rd_el
        26, 46, 37, 41, 55,

        //rd
        21, 31, 51,

        //rd_nm
        22, 32, 52,

        //cond
        61
    };

    TestRelations expectedRelations =
    {
        {147, {11}},
    };

    TestRelations expectedRoadAdRelations =
    {
        {142, {11}},
        {155, {11}},
    };

    doTest(
        "tests/data/rd-target.json",
        "tests/data/rd-source.json",
        { 111 }, //merge region ids
        { "rd_1", "rd_2", "rd_3", "rd_4", "rd_5", "rd_6", "rd_7", "rd_8", "rd_9", "rd_10" }, //layer names
        expectedToDelete,
        expectedToExport,
        expectedRelations,
        expectedRoadAdRelations);
}

BOOST_FIXTURE_TEST_CASE(test_merge_transport, DbFixture)
{
    ObjectIds expectedToDelete = {
        21, //poi
        22, //poi_nm
        31, //tram_jc
        32, //tram_jc
        33, //tram_el
        41, //terminal
        51, //metro_line
        52, //metro_nm
        54, //metro_jc
        55, //metro_jc
        56, //metro_el
        61, //metro station
        71, //metro exit
    };

    ObjectIds expectedToExport = {
        11, //water jc
        12, //water jc
        13, //water way
        21, //water station
        22, //water nm
        31, //airport
        32, //air nm
        41, //terminal
        42, //air nm
        51, //metro line
        52, //metro nm
        54, //metro jc
        55, //metro jc
        56, //metro el
        61, //metro station
        62, //metro nm
        71, //metro exit
        81, //metro exit
        91, //passage way
        101, //passage way
        111, //transport stop
        112, //transport nm
    };

    doTest(
        "tests/data/transport-target.json",
        "tests/data/transport-source.json",
        { 11 }, //merge region ids
        {
            "poi_medicine",
            "transport_ground",
            "transport_metro",
            "transport_railway",
            "transport_waterway",
            "transport_air"
        },
        expectedToDelete,
        expectedToExport);
}

BOOST_FIXTURE_TEST_CASE(test_find_ad, DbFixture)
{
    ObjectIds expectedToDelete;

    ObjectIds expectedToExport = {
        41, 42, 44, 45, 46,
        51, 52, 54, 55, 56,
        61, 62, 64, 65, 66, 67,
        71, 76,
        81, 82, 84, 85, 86,
        91, 92, 94, 95, 96,
        101, 102, 104, 105, 106
    };

    TestRelations expectedRelations;

    TestRelations expectedRoadAdRelations = {
        {47, {11}},
        {52, {11}},
        //{60, {11}},
    };

    doTest(
        "tests/data/ad-to-find-target.json",
        "tests/data/ad-to-find-source.json",
        { 31 }, //merge region ids
        { "rd_1", "rd_2", "rd_3", "rd_4", "rd_5", "rd_6", "rd_7", "rd_8", "rd_9", "rd_10" },
        expectedToDelete,
        expectedToExport,
        expectedRelations,
        expectedRoadAdRelations);
}

BOOST_FIXTURE_TEST_CASE(test_merge_ad_cnt, DbFixture)
{
    ObjectIds expectedToDelete;

    ObjectIds expectedToExport = {
        11, 21, //ad
        13, 24, //ad_nm
        12, //ad_fc
        17, //ad_el
        16, //ad_jc
        22, //ad_cnt
    };

    doTest(
        "tests/data/ad-cnt-target.json",
        "tests/data/ad-cnt-source.json",
        { 21 }, //merge region ids
        { "ad_3", "ad_4", "ad_5", "ad_6", "ad_7" }, //layer names
        expectedToDelete,
        expectedToExport);
}

BOOST_FIXTURE_TEST_CASE(test_merge_ad_cnt_find_parent, DbFixture)
{
    ObjectIds expectedToDelete;

    ObjectIds expectedToExport = {
        21, //ad
        24, //ad_nm
        22, //ad_cnt
    };

    TestRelations expectedRelations;
    TestRelations expectedRoadAdRelations;
    TestRelations expectedAdAdRelations =
    {
        {48, {31}},
    };

    doTest(
        "tests/data/ad-cnt-find-parent-target.json",
        "tests/data/ad-cnt-find-parent-source.json",
        { 21 }, //merge region ids
        { "ad_3", "ad_4", "ad_5", "ad_6", "ad_7" }, //layer names
        expectedToDelete,
        expectedToExport,
        expectedRelations,
        expectedRoadAdRelations,
        expectedAdAdRelations);
}

} // test
} // merge
} // wiki
} // maps
