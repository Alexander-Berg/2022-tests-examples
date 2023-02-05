#include "helpers.h"
#include <yandex/maps/wiki/diffalert/object.h>
#include <yandex/maps/wiki/diffalert/revision/diff_context.h>
#include <yandex/maps/wiki/diffalert/revision/aoi_diff_loader.h>

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/snapshot_id.h>

#include <iostream>
#include <map>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

namespace {

const EditorConfig EDITOR_CONFIG(EDITOR_CONFIG_PATH);

struct GlobalFixture : SetLogLevelFixture
{
    GlobalFixture()
    {
        RevisionDB::clear();
        RevisionDB::execSqlFile(dataPath("sql/rev_loader_tests.sql"));
    }
};

enum class OldObjectPresent { Yes, No };
enum class NewObjectPresent { Yes, No };
enum class AttrsChanged { Yes, No };
enum class GeomChanged { Yes, No };

struct ExpectedDiff
{
    std::string categoryId;
    OldObjectPresent oldObjectPresent;
    NewObjectPresent newObjectPresent;
    AttrsChanged attrsChanged;
    GeomChanged geomChanged;

    Relations relationsAdded;
    Relations relationsDeleted;
    Relations tableAttrsAdded;
    Relations tableAttrsDeleted;
};

template<class DiffContext>
void checkDiff(
        const std::vector<DiffContext>& diff,
        std::map<TId, ExpectedDiff> expectedByOid)
{
    for (const auto& context : diff) {
        auto expectedIt = expectedByOid.find(context.objectId());
        if (expectedIt == std::end(expectedByOid)) {
            ERROR() << "Unexpected diff context for "
                << context.categoryId() << ' ' << context.objectId()
                << ' ' << ChangesPrinter{context};
            UNIT_FAIL_NONFATAL(
                "Unexpected diff context for "
                << context.categoryId() << ' ' << context.objectId());
            continue;
        }
        const auto& expected = expectedIt->second;

        INFO() << "Checking diff for "
             << context.categoryId() << ' ' << context.objectId();
        UNIT_ASSERT_STRINGS_EQUAL(context.categoryId(), expected.categoryId);
        UNIT_ASSERT_VALUES_EQUAL(!!context.oldObject(), expected.oldObjectPresent == OldObjectPresent::Yes);
        UNIT_ASSERT_VALUES_EQUAL(!!context.newObject(), expected.newObjectPresent == NewObjectPresent::Yes);
        UNIT_ASSERT_VALUES_EQUAL(context.attrsChanged(), expected.attrsChanged == AttrsChanged::Yes);
        UNIT_ASSERT_VALUES_EQUAL(context.geomChanged(), expected.geomChanged == GeomChanged::Yes);
        UNIT_ASSERT_EQUAL(context.relationsAdded(), expected.relationsAdded);
        UNIT_ASSERT_EQUAL(context.relationsDeleted(), expected.relationsDeleted);
        UNIT_ASSERT_EQUAL(context.tableAttrsAdded(), expected.tableAttrsAdded);
        UNIT_ASSERT_EQUAL(context.tableAttrsDeleted(), expected.tableAttrsDeleted);

        expectedByOid.erase(expectedIt);
    }

    for (const auto& missing : expectedByOid) {
        UNIT_FAIL_NONFATAL(
            "Missing diff context for "
            << missing.second.categoryId << ' ' << missing.first);
    }
}

std::vector<LongtaskDiffContext> getDiff(
        revision::DBID oldBranchId,
        revision::DBID oldCommitId,
        revision::DBID newBranchId,
        revision::DBID newCommitId)
{
    auto txn = RevisionDB::pool().masterReadOnlyTransaction();
    revision::BranchManager branchMgr(*txn);
    auto oldBranch = branchMgr.load(oldBranchId);
    auto oldSnapshotId =
        revision::SnapshotId::fromCommit(oldCommitId, oldBranch.type(), *txn);
    auto newBranch = branchMgr.load(newBranchId);
    auto newSnapshotId =
        revision::SnapshotId::fromCommit(newCommitId, newBranch.type(), *txn);
    txn.releaseConnection();

    auto result = LongtaskDiffContext::compareSnapshots(
        oldBranch, oldSnapshotId,
        newBranch, newSnapshotId,
        RevisionDB::pool(),
        ViewDB::pool(),
        EDITOR_CONFIG);

    UNIT_ASSERT(result.badObjects().empty());
    UNIT_ASSERT(result.badRelations().empty());

    return std::move(result).diffContexts();
}

Geom wktToGeom(const std::string& wkt)
{
    return Geom(common::wkt2wkb(wkt));
}

typedef std::vector<std::pair<std::string, double>> MinLinearObjectIntersectionRatios;

std::vector<AoiDiffContext> getAoiDiff(
        revision::DBID oldBranchId,
        revision::DBID oldCommitId,
        revision::DBID newBranchId,
        revision::DBID newCommitId,
        const std::set<std::string>& categoryIds,
        const Geom& geom = wktToGeom("POLYGON((0 0,10000000 0,10000000 10000000,0 10000000,0 0))"),
        double defaultMinLinearObjectIntersectionRatio = 1.0 - geolib3::EPS,
        const MinLinearObjectIntersectionRatios& minLinearObjectIntersectionRatios = MinLinearObjectIntersectionRatios())
{
    auto txn = RevisionDB::pool().masterReadOnlyTransaction();
    revision::BranchManager branchMgr(*txn);
    auto oldRg = revision::RevisionsGateway(*txn, branchMgr.load(oldBranchId));
    auto oldSnapshot = oldRg.snapshot(oldCommitId);

    auto newRg = revision::RevisionsGateway(*txn, branchMgr.load(newBranchId));
    auto newSnapshot = newRg.snapshot(newCommitId);

    AoiDiffLoader loader(EDITOR_CONFIG, std::move(geom), oldSnapshot, newSnapshot, oldBranchId, newBranchId, *txn);
    loader.setDefaultMinLinearObjectIntersectionRatio(defaultMinLinearObjectIntersectionRatio);
    for (const auto& categoryRatioPair : minLinearObjectIntersectionRatios) {
        loader.setMinLinearObjectIntersectionRatio(categoryRatioPair.first, categoryRatioPair.second);
    }
    return loader.loadDiffContexts(categoryIds, SplitPolicy::DoNotCheck);
}

} // namespace

Y_UNIT_TEST_SUITE_F(rev_loader, GlobalFixture) {

Y_UNIT_TEST(same_commit)
{
    checkDiff(
        getDiff(
            3, 17,
            3, 17),
        {});
}

Y_UNIT_TEST(same_changesets)
{
    checkDiff(
        getDiff(
            2, 11,
            3, 13),
        {});
}

Y_UNIT_TEST(aoi_diff_same_commits)
{
    checkDiff(
        getAoiDiff(
            3, 17,
            3, 17,
            {"rd_el"}),
        {});
}

Y_UNIT_TEST(aoi_diff_empty_categories)
{
    checkDiff(
        getAoiDiff(
            3, 9,
            3, 26,
            {}),
        {});
}

Y_UNIT_TEST(sample_changeset)
{
    checkDiff(
        getDiff(
            3, 9,
            3, 26),
        {
            {11, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {{112, 11, "start", 0}},
                {}, {}, {}}},
            {12, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {},
                {{22, 12, "start", 0}},
                {}, {}}},
            {13, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {{41, 13, "to", 0}},
                {}, {}, {}}},
            {21, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::No, AttrsChanged::Yes, GeomChanged::Yes,
                {},
                {{22, 21, "end", 0}},
                {}, {}}},
            {22, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::No, AttrsChanged::Yes, GeomChanged::Yes,
                {},
                {{22, 12, "start", 0}, {22, 21, "end", 0},
                    {31, 22, "part", 0}, {41, 22, "to", 0}},
                {}, {}}},
            {31, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {{31, 112, "part", 0}},
                {{31, 22, "part", 0}, {31, 51, "associated_with", 0}},
                {}, {}}},
            {41, {"cond",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{41, 13, "to", 0}, {41, 112, "to", 1}},
                {{41, 22, "to", 0}},
                {},
                {{41, 42, "applied_to", 0}}}},
            {51, {"addr",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {},
                {{31, 51, "associated_with", 0}},
                {{51, 131, "official", 0}},
                {{51, 52, "official", 0}}}},
            {111, {"rd_jc",
                OldObjectPresent::No, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{112, 111, "end", 0}},
                {}, {}, {}}},
            {112, {"rd_el",
                OldObjectPresent::No, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{31, 112, "part", 0}, {41, 112, "to", 1},
                    {112, 11, "start", 0}, {112, 111, "end", 0}},
                {}, {}, {}}},
        });
}

Y_UNIT_TEST(aoi_diff_sample_changeset)
{
    checkDiff(
        getAoiDiff(
            3, 9,
            3, 26,
            {"rd_el", "rd_jc"}),
        {
            {11, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {{112, 11, "start", 0}},
                {}, {}, {}}},
            {12, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {},
                {{22, 12, "start", 0}},
                {}, {}}},
            {13, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {{41, 13, "to", 0}},
                {}, {}, {}}},
            {21, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::No, AttrsChanged::Yes, GeomChanged::Yes,
                {},
                {{22, 21, "end", 0}},
                {}, {}}},
            {22, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::No, AttrsChanged::Yes, GeomChanged::Yes,
                {},
                {{22, 12, "start", 0}, {22, 21, "end", 0},
                    {31, 22, "part", 0}, {41, 22, "to", 0}},
                {}, {}}},
            {111, {"rd_jc",
                OldObjectPresent::No, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{112, 111, "end", 0}},
                {}, {}, {}}},
            {112, {"rd_el",
                OldObjectPresent::No, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{31, 112, "part", 0}, {41, 112, "to", 1},
                    {112, 11, "start", 0}, {112, 111, "end", 0}},
                {}, {}, {}}},
        });

    checkDiff(
        getAoiDiff(
            3, 9,
            3, 26,
            {"rd", "cond", "addr"}),
        {
            {31, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {{31, 112, "part", 0}},
                {{31, 22, "part", 0}, {31, 51, "associated_with", 0}},
                {}, {}}},
            {41, {"cond",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{41, 13, "to", 0}, {41, 112, "to", 1}},
                {{41, 22, "to", 0}},
                {},
                {{41, 42, "applied_to", 0}}}},
            {51, {"addr",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {},
                {{31, 51, "associated_with", 0}},
                {{51, 131, "official", 0}},
                {{51, 52, "official", 0}}}},
        });

    checkDiff(
        getAoiDiff(
            3, 9,
            3, 26,
            {"rd"}),
        {
            {31, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {{31, 112, "part", 0}},
                {{31, 22, "part", 0}, {31, 51, "associated_with", 0}},
                {}, {}}},
        });

    checkDiff(
        getAoiDiff(
            3, 9,
            3, 26,
            {"cond"}),
        {
            {41, {"cond",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{41, 13, "to", 0}, {41, 112, "to", 1}},
                {{41, 22, "to", 0}},
                {},
                {{41, 42, "applied_to", 0}}}},
        });

    checkDiff(
        getAoiDiff(
            3, 9,
            3, 26,
            {"addr"}),
        {
            {51, {"addr",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {},
                {{31, 51, "associated_with", 0}},
                {{51, 131, "official", 0}},
                {{51, 52, "official", 0}}}},
        });
}

Y_UNIT_TEST(diverged_branches)
{
    checkDiff(
        getDiff(
            2, 22,
            3, 25),
        {
            {11, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {{112, 11, "start", 0}},
                {}, {}, {}}},
            {31, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {{31, 112, "part", 0}},
                {}, {}, {}}},
            {41, {"cond",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::No,
                {}, {}, {},
                {{41, 42, "applied_to", 0}}}},
            {51, {"addr",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {},
                {{51, 131, "official", 0}},
                // 141 is unique to branch 2
                {{51, 141, "official", 0}}}},
            {111, {"rd_jc",
                OldObjectPresent::No, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{112, 111, "end", 0}},
                {}, {}, {}}},
            {112, {"rd_el",
                OldObjectPresent::No, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::Yes,
                {{31, 112, "part", 0},
                    {112, 11, "start", 0}, {112, 111, "end", 0}},
                {}, {}, {}}},
        });
}

Y_UNIT_TEST(geom_change_propagation)
{
    checkDiff(
        getDiff(
            3, 12,
            4, 19),
        {
            {62, {"ad_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
            {71, {"ad",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
            {81, {"ad_fc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
        });
}

Y_UNIT_TEST(aoi_diff_geom_change_propagation)
{
    checkDiff(
        getAoiDiff(
            3, 12,
            4, 19,
            {"ad", "ad_el", "ad_fc"}),
        {
            {62, {"ad_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
            {71, {"ad",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
            {81, {"ad_fc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
        });
}

Y_UNIT_TEST(undone_edits_1)
{
    checkDiff(
        getDiff(
            3, 21,
            3, 24),
        {});
}

Y_UNIT_TEST(undone_edits_2)
{
    checkDiff(
        getDiff(
            3, 23,
            3, 25),
        {});
}

Y_UNIT_TEST(rel_geom_change)
{
    checkDiff(
        getDiff(
            3, 14,
            3, 15),
        {
            {31, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {{31, 112, "part", 0}},
                {}, {}, {}}},
            {112, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {{31, 112, "part", 0}},
                {}, {}, {}}},
        });
}

Y_UNIT_TEST(aoi_diff_rel_geom_change)
{
    checkDiff(
        getAoiDiff(
            3, 14,
            3, 15,
            {"rd"}),
        {
            {31, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {{31, 112, "part", 0}},
                {}, {}, {}}},
        });
}


Y_UNIT_TEST(deleted_rel_geom_change)
{
    checkDiff(
        getDiff(
            3, 12,
            3, 13),
        {
            {12, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {},
                {{22, 12, "start", 0}},
                {}, {}}},
            {21, {"rd_jc",
                OldObjectPresent::Yes, NewObjectPresent::No, AttrsChanged::Yes, GeomChanged::Yes,
                {},
                {{22, 21, "end", 0}},
                {}, {}}},
            {22, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::No, AttrsChanged::Yes, GeomChanged::Yes,
                {},
                {{22, 12, "start", 0}, {22, 21, "end", 0},
                    {31, 22, "part", 0}},
                {}, {}}},
            {31, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {},
                {{31, 22, "part", 0}},
                {}, {}}},
        });
}

Y_UNIT_TEST(objects_inside_and_outside_aoi)
{
    RevisionDB::clear();
    auto snapshotsPair = loadData(
        dataPath("tests_data/aoi_diff_loader.before.json"),
        dataPath("tests_data/aoi_diff_loader.after.json"));

    Geom aoiGeom;
    {
        auto txn = RevisionDB::pool().slaveTransaction();
        revision::RevisionsGateway rg(*txn, snapshotsPair.newBranch);
        auto snapshot = rg.snapshot(snapshotsPair.newSnapshotId);
        auto aoiRev = snapshot.objectRevision(1);
        UNIT_ASSERT(aoiRev);
        const auto& data = aoiRev->data();
        UNIT_ASSERT(data.attributes);
        UNIT_ASSERT(data.attributes->count("cat:aoi"));
        UNIT_ASSERT(data.geometry);
        aoiGeom = Geom(*data.geometry);
    }

    auto categoryDiff = [&](
        const std::string& categoryId,
        const MinLinearObjectIntersectionRatios& minLinearObjectIntersectionRatios)
    {
        return getAoiDiff(
                snapshotsPair.oldBranch.id(), snapshotsPair.oldSnapshotId.commitId(),
                snapshotsPair.newBranch.id(), snapshotsPair.newSnapshotId.commitId(),
                {categoryId},
                aoiGeom,
                0.5,
                minLinearObjectIntersectionRatios);
    };

    checkDiff(
        categoryDiff("rd_el", MinLinearObjectIntersectionRatios()),
        {
            {13, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::No,
                {}, {}, {}, {}}},
            {23, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
        });

    checkDiff(
        categoryDiff("rd_el", {{ "rd_el", 1.0 }}),
        {
            {13, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::No,
                {}, {}, {}, {}}},
        });

    checkDiff(
        categoryDiff("rd_el", {{ "ad_el", 1.0 }}),
        {
            {13, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes, GeomChanged::No,
                {}, {}, {}, {}}},
            {23, {"rd_el",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::Yes,
                {}, {}, {}, {}}},
        });

    checkDiff(
        categoryDiff("rd", MinLinearObjectIntersectionRatios()),
        {
            {101, {"rd",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::Yes,
                GeomChanged::No, // only geoms of outside parts changed
                {}, {}, {}, {}}},
        });

    checkDiff(
        categoryDiff("ad", MinLinearObjectIntersectionRatios()),
        {
            {191, {"ad",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {}, {}, {{191, 251, "official", 0}}, {{191, 192, "official", 0}}}},
            // UNEXPECTED: object id 141 (geom not fully inside AOI)
        });

    checkDiff(
        categoryDiff("ad", {{ "rd_el", 1.0 }}),
        {
            {191, {"ad",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {}, {}, {{191, 251, "official", 0}}, {{191, 192, "official", 0}}}},
            // UNEXPECTED: object id 141 (geom not fully inside AOI)
        });

    checkDiff(
        categoryDiff("poi_edu", MinLinearObjectIntersectionRatios()),
        {
            {231, {"poi_edu",
                OldObjectPresent::Yes, NewObjectPresent::Yes, AttrsChanged::No, GeomChanged::No,
                {}, {}, {{231, 271, "official", 0}}, {{231, 232, "official", 0}}}},
            // UNEXPECTED: object id 221 (geom outside AOI)
        });
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
