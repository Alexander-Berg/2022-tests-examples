#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

namespace {

const std::string DATA_FILEPATH = "tests/data/create_parking_for_cleanup.json";
const TOid PARKING_ID = 1;
const TOid URBAN_AREAL_ID = 2;

void cleanupObject(TOid objectId)
{
    ObjectsCleanupRelations::Request request {
        {TESTS_USER, {}},
        objectId,
        revision::TRUNK_BRANCH_ID
    };

    ObjectsCleanupRelations(makeObservers<>(), request)();
}

size_t objectsCount(revision::RevisionsGateway& gateway)
{
    auto snapshot = gateway.snapshot(gateway.headCommitId());
    auto revIds = snapshot.revisionIdsByFilter(revision::filters::ObjRevAttr::isNotDeleted());
    return revIds.size();
};

} // namespace

Y_UNIT_TEST_SUITE(objects_cleanup)
{
WIKI_FIXTURE_TEST_CASE(test_objects_cleanup_relations_parking, EditorTestFixture)
{
    performObjectsImport(DATA_FILEPATH, db.connectionString());

    {
        auto branchCtx = BranchContextFacade::acquireWrite(revision::TRUNK_BRANCH_ID, TESTS_USER);
        revision::RevisionsGateway gateway(branchCtx.txnCore());

        WIKI_TEST_REQUIRE_EQUAL(objectsCount(gateway), 3); //parking, urban_areal, relation

        UNIT_CHECK_GENERATED_EXCEPTION(cleanupObject(PARKING_ID), LogicException); //ERR_FORBIDDEN

        setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, 0);

        UNIT_CHECK_GENERATED_EXCEPTION(cleanupObject(PARKING_ID), LogicException); //ERR_DATA_HAS_NO_CHANGES

        WIKI_TEST_REQUIRE_EQUAL(objectsCount(gateway), 3); //parking, urban_areal, relation

        ObjectsCache cache(branchCtx, boost::none);
        auto urbanAreal = cache.getExisting(URBAN_AREAL_ID);
        urbanAreal->setState(GeoObject::State::Deleted);
        cache.revisionsFacade().save({urbanAreal}, TESTS_USER,
            {{common::COMMIT_PROPKEY_ACTION, common::COMMIT_PROPVAL_OBJECT_DELETED}},
            {}, RevisionsFacade::CheckPermissionsPolicy::Check);

        branchCtx.commit();
    }

    auto txnCore = cfg()->poolCore().masterWriteableTransaction();
    revision::RevisionsGateway gateway(*txnCore);
    WIKI_TEST_REQUIRE_EQUAL(objectsCount(gateway), 2); //parking, relation

    UNIT_ASSERT_NO_EXCEPTION(cleanupObject(PARKING_ID));

    WIKI_TEST_REQUIRE_EQUAL(objectsCount(gateway), 1); //parking

    UNIT_CHECK_GENERATED_EXCEPTION(cleanupObject(PARKING_ID), LogicException); //ERR_DATA_HAS_NO_CHANGES
}

WIKI_FIXTURE_TEST_CASE(test_objects_cleanup_relations_urban_areal, EditorTestFixture)
{
    performObjectsImport(DATA_FILEPATH, db.connectionString());

    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, 0);

    {
        auto branchCtx = BranchContextFacade::acquireWrite(revision::TRUNK_BRANCH_ID, TESTS_USER);
        revision::RevisionsGateway gateway(branchCtx.txnCore());

        WIKI_TEST_REQUIRE_EQUAL(objectsCount(gateway), 3); //parking, urban_areal, relation

        ObjectsCache cache(branchCtx, boost::none);
        auto urbanAreal = cache.getExisting(URBAN_AREAL_ID);
        urbanAreal->setState(GeoObject::State::Deleted);
        cache.revisionsFacade().save({urbanAreal}, TESTS_USER,
            {{common::COMMIT_PROPKEY_ACTION, common::COMMIT_PROPVAL_OBJECT_DELETED}},
            {}, RevisionsFacade::CheckPermissionsPolicy::Check);

        branchCtx.commit();
    }

    auto txnCore = cfg()->poolCore().masterWriteableTransaction();
    revision::RevisionsGateway gateway(*txnCore);
    WIKI_TEST_REQUIRE_EQUAL(objectsCount(gateway), 2); //parking, relation

    UNIT_ASSERT_NO_EXCEPTION(cleanupObject(URBAN_AREAL_ID));

    WIKI_TEST_REQUIRE_EQUAL(objectsCount(gateway), 1); //parking

    UNIT_CHECK_GENERATED_EXCEPTION(cleanupObject(URBAN_AREAL_ID), LogicException); //ERR_DATA_HAS_NO_CHANGES
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
