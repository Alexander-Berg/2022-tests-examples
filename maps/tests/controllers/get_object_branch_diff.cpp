#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/common.h>
#include <maps/wikimap/mapspro/services/editor/src/sync/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/sync/sync_objects.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_object_branch_diff)
{
enum class SyncViews
{
    Yes,
    No
};

TBranchId createStableBranch(SyncViews sync)
{
    TBranchId branchId;

    {
        using namespace revision;

        auto work = cfg()->poolCore().masterWriteableTransaction();
        RevisionsGateway rg(*work);
        auto headCommitId = rg.headCommitId();

        BranchManager bm(*work);
        if (bm.load({{BranchType::Stable, BranchManager::UNLIMITED}}).empty()) {
            bm.createApproved(TESTS_USER, /*attributes=*/{});
        } else {
            bm.loadStable().finish(*work, TESTS_USER);
        }

        CommitManager cm(*work);
        cm.approveAll(headCommitId);
        branchId = bm.createStable(TESTS_USER, /*attributes=*/{}).id();
        cm.mergeApprovedToStable(headCommitId);
        work->commit();
    }

    if (sync == SyncViews::No) {
        return branchId;
    }

    auto work = cfg()->poolCore().masterWriteableTransaction();
    sync::BranchLocker branchLocker(work);

    auto executionState = std::make_shared<ExecutionState>();
    sync::SyncParams syncParams(
            executionState,
            branchId,
            sync::SetProgressState::Yes);
    sync::SyncObjects controller(
            std::move(syncParams),
            branchLocker);
    controller.run({});
    REQUIRE(!executionState->fail,
            "execution state"
            << ", canceled: " << executionState->cancel
            << ", failed: " << executionState->fail);
    branchLocker.work().commit();

    return branchId;
}

xml3::Doc parseXmlResponse(const std::string& xml)
{
    validateXmlResponse(xml);
    auto doc = xml3::Doc::fromString(xml);
    doc.addNamespace("e", PROJECT_NAMESPACE);
    return doc;
}

void updateAdElGeom(TRevisionId revisionId, TBranchId branchId, SyncViews sync)
{
    const Geom newAdElGeom(createGeomFromJsonStr(R"geom(
    {
        "coordinates": [[36.999394089618676, 55.772976174429076],
                        [36.99938336078262, 55.771585212480176],
                        [37.1024410790596, 55.77157916471067],
                        [37.00239816371537, 55.77294593665589],
                        [36.999394089618676, 55.772976174429076]],
        "type": "LineString"
    }
    )geom"));

    ObjectsUpdateDataCollection coll;
    auto collId = coll.add(
            /*isPrimary=*/true,
            revisionId,
            /*uuid=*/std::string(),
            "ad_el",
            newAdElGeom);
    ObjectsEditContextsPtrMap editContexts;
    editContexts.insert({
            collId,
            std::make_shared<ObjectEditContext>(
                    View(TGeoPoint(37.0, 55.7), 17),
                    /*allowIntersections=*/true,
                    /*allowInvalidContours=*/true)});

    const auto observers = (sync == SyncViews::Yes)
        ? makeObservers<ViewSyncronizer>()
        : makeObservers<>();

    performRequest<SaveObject>(
        observers,
        UserContext(TESTS_USER, {}),
        /*force=*/true,
        branchId,
        coll,
        editContexts,
        SaveObject::IsLocalPolicy::Manual,
        /*feedbackTaskId*/ boost::none,
        StringMap(),
        ""
    );
}

void performTest(SyncViews sync)
{
    performSaveObjectRequest("tests/data/create_test_city.json");
    auto adId = getObjRevisionId("ad").objectId();

    TBranchId fromBranchId = createStableBranch(sync);
    TBranchId toBranchId = createStableBranch(SyncViews::Yes);

    auto xml1 = performXmlGetRequest<GetBranchDiff>(
            adId, fromBranchId, toBranchId, TESTS_USER, std::string());
    auto doc1 = parseXmlResponse(xml1);
    auto nodes = doc1.nodes("/e:editor/e:response-branch-diff/e:branch-diff/e:diff");
    WIKI_TEST_REQUIRE_EQUAL(nodes.size(), 1);
    UNIT_ASSERT(nodes[0].firstElementChild().isNull());

    updateAdElGeom(getObjRevisionId("ad_el"), toBranchId, SyncViews::Yes);

    auto xml2 = performXmlGetRequest<GetBranchDiff>(
            adId, fromBranchId, toBranchId, TESTS_USER, std::string());
    auto doc2 = parseXmlResponse(xml2);
    auto beforeGeomNodes = doc2.nodes(
            "/e:editor/e:response-branch-diff/e:branch-diff/e:diff"
            "/e:geometry/e:before");
    UNIT_ASSERT_EQUAL(beforeGeomNodes.size(), 1);
    auto afterGeomNodes = doc2.nodes(
            "/e:editor/e:response-branch-diff/e:branch-diff/e:diff"
            "/e:geometry/e:after");
    UNIT_ASSERT_EQUAL(afterGeomNodes.size(), 1);
}

WIKI_FIXTURE_TEST_CASE(test_object_branch_diff_from_view, EditorTestFixture)
{
    performTest(SyncViews::Yes);
}

WIKI_FIXTURE_TEST_CASE(test_object_branch_diff_from_revision, EditorTestFixture)
{
    performTest(SyncViews::No);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
