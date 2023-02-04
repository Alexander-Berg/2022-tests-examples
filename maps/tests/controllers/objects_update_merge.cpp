#include <maps/wikimap/mapspro/services/editor/src/actions/objects_update_merge.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/objects_cache.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

namespace {

std::string makeRequestXml(const std::vector<TOid>& oids)
{
    std::stringstream ss;
    ss << "<editor xmlns=\"http://maps.yandex.ru/mapspro/editor/1.x\">"
       << "<request-objects-update>"
       << "<objects>";
    for (TOid oid : oids) {
        ss << "<object id=\"" << oid << "\"/>";
    }
    ss << "</objects>"
       << "<context><merge/></context>"
       << "</request-objects-update>"
       << "</editor>";
    return ss.str();
}

} // namespace

Y_UNIT_TEST_SUITE(objects_update_merge)
{
WIKI_FIXTURE_TEST_CASE(test_objects_update_merge, EditorTestFixture)
{
    performObjectsImport("tests/data/objects_update_merge.json",
        db.connectionString());

    TOid primaryRdOid = 1488715469;
    TOid rdOidToMerge = 1488715479;
    TOid rdElOid1 = 1488715441;
    TOid rdElOid2 = 1488715450;

    {
        // attempt to merge objects of different categories
        ObjectsUpdateMerge::Request badRequest {
            makeRequestXml({primaryRdOid, rdElOid1}), {TESTS_USER, {}}, 0,
            common::FormatType::XML};
        UNIT_CHECK_GENERATED_EXCEPTION(
                ObjectsUpdateMerge(makeObservers<>(), badRequest)(),
                LogicException);
    }

    {
        // attempt to merge into object which is not complex
        auto oidsStr = std::to_string(rdElOid1) + "," + std::to_string(rdElOid2);
        ObjectsUpdateMerge::Request badRequest {
            makeRequestXml({rdElOid1, rdElOid2}), {TESTS_USER, {}}, 0,
            common::FormatType::XML};
        UNIT_CHECK_GENERATED_EXCEPTION(
                ObjectsUpdateMerge(makeObservers<>(), badRequest)(),
                LogicException);
    }

    {
        // attempt to merge object into itself
        ObjectsUpdateMerge::Request badRequest {
            makeRequestXml({primaryRdOid, primaryRdOid}), {TESTS_USER, {}}, 0,
            common::FormatType::XML};
        UNIT_CHECK_GENERATED_EXCEPTION(
                ObjectsUpdateMerge(makeObservers<>(), badRequest)(),
                LogicException);
    }

    {
        // merge two roads
        ObjectsUpdateMerge::Request request {
            makeRequestXml({primaryRdOid, rdOidToMerge}), {TESTS_USER, {}}, 0,
            common::FormatType::XML};
        ObjectsUpdateMerge(makeObservers<>(), request)();
    }

    {
        auto branchCtx = BranchContextFacade::acquireRead(0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto primaryRd = cache.getExisting(primaryRdOid);
        auto mergedRd = cache.getExisting(rdOidToMerge);
        UNIT_ASSERT(mergedRd->isDeleted());

        std::set<TOid> slaves;
        for (const auto& rel :
                 primaryRd->relations(RelationType::Slave).range()) {
            UNIT_ASSERT(slaves.insert(rel.id()).second);
        }
        std::set<TOid> expectedSlaves {
            1488715470, // rd_nm
            1488715441, 1488715450, 1488715461, // rd_el
            1488715499, 1488715489, // addr
        };

        WIKI_TEST_EQUAL_COLLECTIONS(
                slaves.begin(), slaves.end(),
                expectedSlaves.begin(), expectedSlaves.end());
    }

    {
        // attempt to merge with deleted object
        ObjectsUpdateMerge::Request badRequest {
            makeRequestXml({primaryRdOid, rdOidToMerge}), {TESTS_USER, {}}, 0,
            common::FormatType::XML};
        UNIT_CHECK_GENERATED_EXCEPTION(
                ObjectsUpdateMerge(makeObservers<>(), badRequest)(),
                LogicException);
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
