#include "helpers.h"
#include <maps/wikimap/mapspro/libs/validator/common/magic_strings.h>

#include <yandex/maps/wiki/validator/validator.h>

#include <library/cpp/testing/unittest/registar.h>

#include <algorithm>
#include <vector>
#include <set>

namespace rev = maps::wiki::revision;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

namespace {

const std::string TESTS_AUX_MODULE_NAME = "tests-aux";

}

Y_UNIT_TEST_SUITE_F(runner, DbFixture) {

Y_UNIT_TEST(test_loading)
{
    auto modules = validator.modules();
    UNIT_ASSERT_VALUES_UNEQUAL(modules.size(), 0);

    auto moduleIt = std::find_if(
        modules.begin(), modules.end(),
        [](const ModuleInfo& module) { return module.name() == TESTS_AUX_MODULE_NAME; });
    UNIT_ASSERT_UNEQUAL(moduleIt, modules.end());

    auto checksSet = std::set<TCheckId> {
        moduleIt->checkIds().begin(), moduleIt->checkIds().end()
    };
    UNIT_ASSERT_EQUAL(
            checksSet, std::set<TCheckId>(
                { "do_nothing", "long", "throwing", "report_all",
                  "report_all_without_geom", "without_oids",
                  "report_relations", "report_nonexistent_id" }));
}

Y_UNIT_TEST(test_empty_checks)
{
    ResultPtr result = validator.run(
        {},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        0,
        aoi,
        "./test_empty_checks");

    UNIT_ASSERT(!result);
}

Y_UNIT_TEST(test_empty_db)
{
    DBID commitId;

    {
        auto revisionTxn = revisionPool().masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        commitId = createStubCommit(rg);
        revisionTxn->commit();
    }

    ResultPtr result = validator.run(
        {"report_all"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        commitId,
        aoi,
        "./test_empty_db"
    );

    auto buffer = result->popMessages();
    UNIT_ASSERT(buffer.empty());
}

Y_UNIT_TEST(test_check_exception)
{
    const Messages messages = validator.run(
        {"throwing"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        startCommitId,
        aoi,
        "./test_check_exception"
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    UNIT_ASSERT_STRINGS_EQUAL(messages.front().attributes().checkId, "throwing");

    checkMessage(messages[0], "check-internal-error");
}

Y_UNIT_TEST(test_correct_destruction)
{
    auto v = std::make_unique<Validator>(validatorConfig);
    v->initModules();
    ResultPtr result = v->run(
        {"long"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        startCommitId,
        aoi,
        "./test_correct_destruction"
    );
    for (int i = 0; i < 5; ++i) {
        result = v->run(
            {"long"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            startCommitId,
            aoi,
            "./test_correct_destruction." + std::to_string(i)
        );
    }

    v.reset(0);
    auto buffer = result->popMessages(); // waiting should not take forever;
}

Y_UNIT_TEST(test_reporting)
{
    RevisionID id1, id2;

    {   // create road elements
        auto revisionTxn = revisionPool().masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        id1 = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(1 1)").objectId()
        );
        id2 = createRoadElement(
            rg, "LINESTRING(2 2, 3 3)",
            createJunction(rg, "POINT(2 2)").objectId(),
            createJunction(rg, "POINT(3 3)").objectId()
        );
        revisionTxn->commit();
    }

    const DBID headCommitId = id2.commitId();
    {
        const Messages messages = validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            headCommitId ,
            aoi,
            "./test_reporting"
        )->drainAllMessages();

        checkMessages(
            messages, {
                {"stub-description", RevisionIds{id1}},
                {"stub-description", RevisionIds{id2}}
            }
        );
    }
}

Y_UNIT_TEST(test_validate_commit)
{
    RevisionID id1, id2, id3;

    {   // create 3 road elements
        auto revisionTxn = revisionPool().masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        id1 = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(1 1)").objectId()
        );
        id2 = createRoadElement(
            rg, "LINESTRING(2 2, 3 3)",
            createJunction(rg, "POINT(2 2)").objectId(),
            createJunction(rg, "POINT(3 3)").objectId()
        );

        id3 = createRoadElement(
            rg, "LINESTRING(4 4, 5 5)",
            createJunction(rg, "POINT(4 4)").objectId(),
            createJunction(rg, "POINT(5 5)").objectId()
        );
        revisionTxn->commit();
    }

    {   // check 1
        const Messages messages = validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            id2.commitId(),
            aoi,
            "./test_validate_commit.1"
        )->drainAllMessages();

        checkMessages(
            messages, {
                {"stub-description", RevisionIds{id1}},
                {"stub-description", RevisionIds{id2}}
            }
        );
    }

    {   // check 2 (+ one commit from check 1)
        const Messages messages = validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            id3.commitId(),
            aoi,
            "./test_validate_commit.2"
        )->drainAllMessages();

        checkMessages(
            messages, {
                {"stub-description", RevisionIds{id1}},
                {"stub-description", RevisionIds{id2}},
                {"stub-description", RevisionIds{id3}}
            }
        );
    }
}

Y_UNIT_TEST(test_branch_validation)
{
    DBID stableBranchId;
    RevisionID id1, id2;

    {   // create two road elements in trunk and stable branches
        auto revisionTxn = revisionPool().masterWriteableTransaction();
        auto trunkRg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        id1 = createRoadElement(
            trunkRg, "LINESTRING(0 0, 1 1)",
            createJunction(trunkRg, "POINT(0 0)").objectId(),
            createJunction(trunkRg, "POINT(1 1)").objectId()
        );

        rev::BranchManager branchManager(*revisionTxn);
        auto branch = branchManager.createStable(TEST_UID, {});
        branch.setState(*revisionTxn, rev::BranchState::Normal);
        stableBranchId = branch.id();

        rev::RevisionsGateway stableRg(*revisionTxn, branch);
        id2 = createRoadElement(
            stableRg, "LINESTRING(2 2, 3 3)",
            createJunction(stableRg, "POINT(2 2)").objectId(),
            createJunction(stableRg, "POINT(3 3)").objectId()
        );

        revisionTxn->commit();
    }

    {   // check trunk
        const Messages messages = validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            id1.commitId(),
            aoi,
            "./test_branch_validation.trunk"
        )->drainAllMessages();

        checkMessages(
            messages, {
                {"stub-description", RevisionIds{id1}}
            }
        );
    }

    {   // check stable branch
        const Messages messages = validator.run(
            {"report_all"},
            *revisionPgPool(),
            stableBranchId,
            id2.commitId(),
            aoi,
            "./test_branch_validation.stable"
        )->drainAllMessages();

        checkMessages(
            messages, {
                {"stub-description", RevisionIds{id2}}
            }
        );
    }

    {   // check nonexistent branch
        const TId nonexistentBranchId = 666;
        UNIT_CHECK_GENERATED_EXCEPTION(
            validator.run(
                {"report_all"},
                *revisionPgPool(),
                nonexistentBranchId,
                startCommitId,
                aoi,
                "./test_branch_validation.other"
            ),
            maps::Exception
        );
    }
}

Y_UNIT_TEST(test_report_nonexistent_ids)
{
    // check 'nonexistent_id' tries to report these ids
    RevisionID startJunctionId, roadElementId;
    const RevisionID endJunctionId = RevisionID::createNewID(101);
    const TId nonexistentObjectId = 12345;;

    {   // Create road element with nonexistent end junction
        auto revisionTxn = revisionPool().masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);

        startJunctionId = createJunction(rg, "POINT(0 0)");
        roadElementId = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            startJunctionId.objectId(),
            endJunctionId.objectId()
        );
        auto snapshot = rg.snapshot(rg.headCommitId());
        UNIT_ASSERT(!snapshot.objectRevision(endJunctionId.objectId()));
        UNIT_ASSERT(!snapshot.objectRevision(nonexistentObjectId));
        revisionTxn->commit();
    }

    {
        const Messages messages = validator.run(
            {"report_relations"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            roadElementId.commitId(),
            aoi,
            "./test_report_nonexistent_ids.1"
        )->drainAllMessages();

        checkMessages(
            messages, {
                {"missing-related-object", RevisionIds{roadElementId, endJunctionId}},
                {"bad-end-junction-relation", RevisionIds{roadElementId}}
            }
        );
    }

    {
        const Messages messages = validator.run(
            {"report_nonexistent_id"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            roadElementId.commitId(),
            aoi,
            "./test_report_nonexistent_ids.2"
        )->drainAllMessages();

        checkMessages(
            messages, {
                {"missing-related-object", RevisionIds{roadElementId, endJunctionId}},
                {"bad-end-junction-relation", RevisionIds{roadElementId}},
                {"check-internal-error", RevisionIds{}}
            }
        );
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
