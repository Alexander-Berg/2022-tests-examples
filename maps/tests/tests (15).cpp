#include "../cleanspecial.h"

#include <maps/libs/json/include/value.h>
#include <yandex/maps/wiki/common/extended_xml_doc.h>
#include <yandex/maps/wiki/common/pgpool3_helpers.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <yandex/maps/wiki/unittest/unittest.h>

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MAIN
#include <boost/test/unit_test.hpp>

#include <fstream>
#include <set>

namespace rev = maps::wiki::revision;
namespace rf = maps::wiki::revision::filters;
namespace unittest = maps::wiki::unittest;

namespace maps {
namespace wiki {
namespace cleanspecial {
namespace test {

namespace {

constexpr int TEST_UID = 111;
constexpr uint64_t TEST_TASK_ID = 0;
constexpr rev::DBID TRUNK_BRANCH = 0;
constexpr rev::DBID TEST_AOI_ID = 1;
constexpr size_t COMMIT_BATCH_SIZE = 10000;
const std::string SERVICE_CONFIG = "./tests/services.local.cleanspecial-tests.xml";
const std::string JSON_FILE = "./tests/json/cleanspecial.json";

rev::DBID getHeadCommitId(pgpool3::Pool& pool)
{
    auto txn = pool.masterReadOnlyTransaction();
    rev::RevisionsGateway gtw(*txn);
    return gtw.headCommitId();
}

void importObjects(const std::string& jsonFile)
{
    common::ExtendedXmlDoc configXml(SERVICE_CONFIG);
    common::PoolHolder poolHolder(configXml, "long-read", "revisionapi");

    std::ifstream input;
    input.open(jsonFile);
    REQUIRE(!input.fail(), "Can't open '" << jsonFile << "' for reading");

    const auto mode = revisionapi::IdMode::StartFromJsonId;
    revisionapi::RevisionAPI revApi(poolHolder.pool());
    revApi.importData(TEST_UID, mode, input, COMMIT_BATCH_SIZE, 0);
}

std::vector<rev::DBID> objectIdsFromJson(const std::string& jsonFile)
{
    auto json = json::Value::fromFile(jsonFile);
    REQUIRE(json.hasField("objects"), "Bad json file: " << jsonFile);
    const auto& objects = json["objects"];

    std::vector<rev::DBID> objectIds;
    for (const auto& id : objects.fields()) {
        objectIds.push_back(std::stoi(id));
    }
    return objectIds;
}

std::vector<rev::DBID> filterDeletedObjectIds(
        std::vector<rev::DBID> allObjectIds,
        pgpool3::Pool& pool)
{
    auto txn = pool.masterReadOnlyTransaction();
    auto gtw = rev::RevisionsGateway(*txn, rev::BranchManager(*txn).load(TRUNK_BRANCH));
    auto headCommitId = getHeadCommitId(pool);

    rev::RevisionIds allRevisionIds;
    for (auto objId: allObjectIds) {
        allRevisionIds.push_back({objId, headCommitId});
    }

    auto deletedRevisions = gtw.reader().loadRevisions(
        allRevisionIds,
        revision::filters::ObjRevAttr::isDeleted());

    std::vector<rev::DBID> deletedObjectIds;
    for (const auto& objRev: deletedRevisions) {
        deletedObjectIds.push_back(objRev.id().objectId());
    }
    return deletedObjectIds;
}

} // namespace

class CleanspecialFixture : public unittest::DatabaseFixture
{
public:
    CleanspecialFixture()
        : unittest::DatabaseFixture(SERVICE_CONFIG, "cleanspecial-test")
    {
        clearDb();
        importObjects(JSON_FILE);
    }

    virtual ~CleanspecialFixture()
    {
    }

private:
    void clearDb()
    {
        auto txn = pool().masterWriteableTransaction();
        auto revGateway = rev::RevisionsGateway(
            *txn, rev::BranchManager(*txn).load(TRUNK_BRANCH));
        revGateway.truncateAll();
        revGateway.createDefaultBranches();
        txn->commit();
    }
};

BOOST_FIXTURE_TEST_SUITE(cleanspecial, CleanspecialFixture)

BOOST_AUTO_TEST_CASE(cleanspecial_test_1)
{
    {
        TaskParams params(TEST_TASK_ID, TRUNK_BRANCH, getHeadCommitId(pool()),
                TEST_UID, {}, TEST_AOI_ID);
        auto txn = pool().masterWriteableTransaction();
        cleanSpecial(txn, params);
        txn->commit();
    }

    auto allObjectIds = objectIdsFromJson(JSON_FILE);
    std::sort(allObjectIds.begin(), allObjectIds.end());

    auto deletedObjectIds = filterDeletedObjectIds(allObjectIds, pool());
    std::sort(deletedObjectIds.begin(), deletedObjectIds.end());

    std::set<rev::DBID> remainingObjectIds {
            1, 11, // aoi
            321, // error
            101, 102, 103, 111, 121, 122, 131, // intersecting ad
            231, 232, 241, 242, 251, // ad inside
            261, 262, 263, 271, 272, 291, // intersecting railway part
            21, 22, 23, 31, 32, 33, 41, 42, 43, 51, 52, 53, // high fc rd_el
            71, 72, 391, 392, // ditto
            191, 192, // high fc road w/ name
            201, 202, // ditto, inside
            151, // building outside
            401, // high fc cond
            161, 162, 163, // low fc road outside
            181, 182, // addr outside
            311, // airport outside
            351, 352, 353, 361, 371, 381, // intersecting contour urban
            341, // containing areal urban
            211, 212, 221, 222, 223 // hydro
    };

    std::vector<rev::DBID> expectedDeletedObjectIds;
    std::set_difference(
            allObjectIds.begin(), allObjectIds.end(),
            remainingObjectIds.begin(), remainingObjectIds.end(),
            std::back_inserter(expectedDeletedObjectIds));

    BOOST_CHECK_EQUAL_COLLECTIONS(
            deletedObjectIds.begin(), deletedObjectIds.end(),
            expectedDeletedObjectIds.begin(), expectedDeletedObjectIds.end());
}

BOOST_AUTO_TEST_SUITE_END()

} // test
} // cleanspecial
} // wiki
} // maps
