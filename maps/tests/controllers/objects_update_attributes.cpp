#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(objects_update_attributes)
{
void
checkAttributeDiff(
    const json::Value& attrJson,
    const std::string& attrName,
    const std::string& valueAfter)
{
    WIKI_TEST_REQUIRE_EQUAL(attrJson["id"].as<std::string>(), attrName);
    UNIT_ASSERT(!attrJson.hasField("before"));
    WIKI_TEST_REQUIRE_EQUAL(attrJson["after"].toString(), valueAfter);
}

WIKI_FIXTURE_TEST_CASE(test_objects_update_attributes, EditorTestFixture)
{
    performObjectsImport("tests/data/create_bld_for_group_operation.json", db.connectionString());
    performAndValidateJson<ObjectsUpdateAttributes>(
        loadFile("tests/data/objects_update_attributes.json"),
        UserContext(TESTS_USER, {}),
        revision::TRUNK_BRANCH_ID,
        boost::none, /* feedbackTaskId */
        common::FormatType::JSON);

    auto branchCtx = BranchContextFacade::acquireRead(0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto bldRevisions = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr("cat:bld").defined());
    WIKI_TEST_REQUIRE_EQUAL(bldRevisions.size(), 2);

    for (size_t i = 0; i < bldRevisions.size(); ++i) {
        auto result = performJsonGetRequest<GetCommitDiff>(
            bldRevisions[i].commitId(),
            (TOid)0,
            TESTS_USER,
            "" /* dbToken */,
            revision::TRUNK_BRANCH_ID);
        validateJsonResponse(result, "GetCommitDiff");
        auto diffJson = json::Value::fromString(result);

        auto jsonAffectedObjectIds = diffJson["affectedObjectIds"];
        UNIT_ASSERT(jsonAffectedObjectIds.isArray());
        UNIT_ASSERT(jsonAffectedObjectIds.size() == 2);

        UNIT_ASSERT(!diffJson["categoryId"].isNull());

        UNIT_ASSERT(!diffJson["bounds"].isNull());

        auto jsonGeomBefore = diffJson["modified"]["geometry"]["before"];
        auto jsonGeomAfter = diffJson["modified"]["geometry"]["after"];
        UNIT_ASSERT(jsonGeomBefore.isArray() && jsonGeomAfter.isArray());
        UNIT_ASSERT(jsonGeomBefore.size() == 2);
        UNIT_ASSERT(jsonGeomAfter.size() == 2);

        for (size_t i = 0; i < jsonGeomBefore.size(); ++i) {
            auto geomBefore = createGeomFromJson(jsonGeomBefore[i]);
            auto geomAfter = createGeomFromJson(jsonGeomAfter[i]);
            UNIT_ASSERT(geomBefore->equalsExact(geomAfter.get()));
        }

        auto attrsJson = diffJson["modified"]["attrs"];
        UNIT_ASSERT(attrsJson.isArray());
        WIKI_TEST_REQUIRE_EQUAL(attrsJson.size(), 3);

        checkAttributeDiff(attrsJson[0], "bld:cond", "3");
        checkAttributeDiff(attrsJson[1], "bld:ft_type_id", "103");
        checkAttributeDiff(attrsJson[2], "bld:height", "30");
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
