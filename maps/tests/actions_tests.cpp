#include "common.h"
#include "json_compare.h"
#include <yandex/maps/wiki/groupedit/session.h>
#include <yandex/maps/wiki/groupedit/actions/delete.h>
#include <yandex/maps/wiki/groupedit/actions/move.h>
#include <yandex/maps/wiki/groupedit/actions/update_attrs.h>

#include <yandex/maps/wiki/revision/branch.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/common.h>
#include <yandex/maps/wiki/revision/filters.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <maps/libs/json/include/value.h>
#include <boost/test/unit_test.hpp>

#include <library/cpp/testing/common/env.h>

#include <fstream>
#include <memory>
#include <sstream>
#include <vector>

namespace maps {
namespace wiki {
namespace groupedit {
namespace tests {

namespace {

const TUserId JSON_UPLOAD_UID = 5;

const std::string AOI_WKB = wkt2wkb("POLYGON (("
        "5000000 5000000, 5000000 5000100, 5000100 5000100, 5000100 5000000,"
        " 5000000 5000000))");

const std::string OBJECTS_FIELD = "objects";

void loadJson(pgpool3::Pool& pool, const std::string& fname)
{
    std::ifstream file(fname);
    REQUIRE(file, "Error opening file " << fname);

    revisionapi::RevisionAPI jsonLoader(pool);
    jsonLoader.importData(
        JSON_UPLOAD_UID, revisionapi::IdMode::StartFromJsonId, file);
}

revision::Branch trunkBranch(pgpool3::Pool& pool)
{
    auto writeTxn = pool.masterWriteableTransaction();
    return revision::BranchManager(*writeTxn).load(revision::TRUNK_BRANCH_ID);
}

json::Value objectsJson(pgpool3::Pool& pool, TCommitId commitId)
{
    std::stringstream stream;

    auto trunk = trunkBranch(pool);

    revisionapi::ExportParams params(
        trunk,
        commitId,
        revisionapi::RelationsExportFlags::MasterToSlave);

    revisionapi::RevisionAPI(pool).exportData(
        params, revisionapi::SingleStreamWrapper(stream));
    return json::Value::fromStream(stream)[OBJECTS_FIELD];
}

} // namespace

BOOST_GLOBAL_FIXTURE( SetLogLevelFixture );

BOOST_FIXTURE_TEST_CASE( test_linear_move, unittest::ArcadiaDbFixture )
{
    loadJson(pool(), SRC_("data/move_action.json"));

    TCommitId lastCommitId = 0;
    {
        auto txn = pool().masterWriteableTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        auto commitIds = actions::moveObjects(
            session,
            revision::filters::True(),
            AOI_WKB,
            5, -5,
            TEST_UID,
            actions::PolygonMode::Partial);

        BOOST_REQUIRE(!commitIds.empty());

        lastCommitId =
            *std::max_element(std::begin(commitIds), std::end(commitIds));

        txn->commit();
    }

    checkExpectedJson(
        objectsJson(pool(), lastCommitId),
        json::Value::fromFile(SRC_("data/move_action_linear.diff.json")));
}

BOOST_FIXTURE_TEST_CASE( test_attributes, unittest::ArcadiaDbFixture )
{
    loadJson(pool(), SRC_("data/attributes_action.json"));

    actions::UpdateAttrsAction action;
    action.addAttributeValue("rd_el:access_id", "3");
    action.removeAttribute("rd_el:paved");

    TCommitId lastCommitId = 0;
    {
        auto txn = pool().masterWriteableTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        auto commitIds = action.perform(
            session,
            {
                revision::filters::Attr("cat:rd_el").defined(),
                GeomPredicate::Within,
                AOI_WKB
            },
            TEST_UID);

        BOOST_REQUIRE(!commitIds.empty());

        lastCommitId =
            *std::max_element(std::begin(commitIds), std::end(commitIds));

        txn->commit();
    }

    checkExpectedJson(
        objectsJson(pool(), lastCommitId),
        json::Value::fromFile(SRC_("data/attributes_action.diff.json")));
}

BOOST_FIXTURE_TEST_CASE( test_delete, unittest::ArcadiaDbFixture )
{
    static const std::vector<std::string> CATEGORIES_TO_REMOVE {
        "cat:rd_el",
        "cat:ad_el",
        "cat:transport_stop"
    };

    loadJson(pool(), SRC_("data/delete_action.json"));

    TCommitId lastCommitId = 0;
    {
        auto txn = pool().masterWriteableTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        auto commitIds = actions::deleteObjects(
            session,
            {{
                revision::filters::Attr::definedAny(CATEGORIES_TO_REMOVE),
                GeomPredicate::Within,
                AOI_WKB
            }},
            TEST_UID);

        BOOST_REQUIRE(!commitIds.empty());

        lastCommitId =
            *std::max_element(std::begin(commitIds), std::end(commitIds));

        txn->commit();
    }

    checkExpectedJson(
        objectsJson(pool(), lastCommitId),
        json::Value::fromFile(SRC_("data/delete_action.diff.json")));
}

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps
