#include "helpers.h"
#include <yandex/maps/wiki/diffalert/revision/diff_context.h>
#include <yandex/maps/wiki/diffalert/revision/aoi_diff_loader.h>

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

struct GlobalFixture : SetLogLevelFixture
{
    GlobalFixture()
    {
        RevisionDB::clear();
        RevisionDB::execSqlFile(dataPath("sql/rd_el_split_tests.sql"));
    }
};

Y_UNIT_TEST_SUITE_F(split, GlobalFixture) {

Y_UNIT_TEST(test_rd_el_split)
{
    EditorConfig editorConfig(EDITOR_CONFIG_PATH);

    Geom aoiGeom(common::wkt2wkb("POLYGON((0 0,10000000 0,10000000 10000000,0 10000000,0 0))"));
    const auto branchId = 0;
    const auto oldCommitId = 1;
    const auto newCommitId = 4;

    auto txn = RevisionDB::pool().masterReadOnlyTransaction();

    revision::BranchManager branchMgr(*txn);
    revision::RevisionsGateway gateway(*txn, branchMgr.load(branchId));

    auto oldSnapshot = gateway.snapshot(oldCommitId);
    auto newSnapshot = gateway.snapshot(newCommitId);

    AoiDiffLoader loader(editorConfig, std::move(aoiGeom), oldSnapshot, newSnapshot, branchId, branchId, *txn);

    auto diffs = loader.loadDiffContexts({"rd_el"}, SplitPolicy::DoNotCheck);
    UNIT_ASSERT_VALUES_EQUAL(diffs.size(), 2);

    diffs = loader.loadDiffContexts({"rd_el"}, SplitPolicy::Check);
    UNIT_ASSERT_VALUES_EQUAL(diffs.size(), 0);
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
