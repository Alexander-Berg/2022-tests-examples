#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/common/string_utils.h>

namespace maps::wiki::tests {

void
checkDiffForGeometry(TCommitId commitId, TOid objectId)
{
    auto result = performJsonGetRequest<GetCommitDiff>(
        commitId,
        objectId,
        TESTS_USER,
        "" /* dbToken */,
        revision::TRUNK_BRANCH_ID);
    validateJsonResponse(result, "GetCommitDiff");
    auto diffJson = json::Value::fromString(result);
    UNIT_ASSERT(!diffJson["modified"]["geometry"].isNull());
}

geos::geom::Envelope
diffEnvelope(TCommitId commitId, TOid objectId)
{
    auto result = performJsonGetRequest<GetCommitDiff>(
        commitId,
        objectId,
        TESTS_USER,
        "" /* dbToken */,
        revision::TRUNK_BRANCH_ID);
    validateJsonResponse(result, "GetCommitDiff");
    auto diffJson = json::Value::fromString(result);
    WIKI_TEST_REQUIRE(diffJson["bounds"].isArray());
    auto bounds = common::join(diffJson["bounds"],
        [](const json::Value& val) {
            return std::to_string(val.as<double>());
        },
        ",");
    auto env = createEnvelope(
        bounds,
        SpatialRefSystem::Geodetic);
    return env;
}

std::string
toString(const geos::geom::Envelope& envelope)
{
    std::stringstream out;
    out.precision(DOUBLE_FORMAT_PRECISION);
    const TGeoPoint lb = common::mercatorToGeodetic(envelope.getMinX(), envelope.getMinY());
    const TGeoPoint rt = common::mercatorToGeodetic(envelope.getMaxX(), envelope.getMaxY());
    out << lb.x() << "," << lb.y() << "," << rt.x() << "," << rt.y();
    return out.str();
}

void
checkGeomDiffForGeometry(TCommitId commitId, TOid objectId,
    const geos::geom::Envelope& envelope,
    size_t expectedGeomCount)
{
    auto result = performJsonGetRequest<GetCommitGeomDiff>(
        commitId,
        objectId,
        TESTS_USER,
        "" /* dbToken */,
        revision::TRUNK_BRANCH_ID,
        toString(envelope),
        TZoom(10));
    validateJsonResponse(result, "GetCommitGeomDiff");
    auto diffJson = json::Value::fromString(result);
    if (!expectedGeomCount) {
        UNIT_ASSERT(diffJson.empty());
        return;
    }
    const auto& geomJson = diffJson["geometry"];
    WIKI_TEST_REQUIRE(geomJson.isObject());
    UNIT_ASSERT_EQUAL(expectedGeomCount,
        geomJson["before"].size() + geomJson["after"].size());
}

Y_UNIT_TEST_SUITE(objects_update_move)
{
WIKI_FIXTURE_TEST_CASE(test_objects_update_move, EditorTestFixture)
{
    performObjectsImport("tests/data/create_bld_for_group_operation.json", db.connectionString());
    performAndValidateJson<ObjectsUpdateMove>(
        loadFile("tests/data/objects_update_move.json"),
        UserContext(TESTS_USER, {}),
        revision::TRUNK_BRANCH_ID,
        boost::none, /* feedbackTaskId */
        common::FormatType::JSON);

    auto branchCtx = BranchContextFacade::acquireRead(0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto bldRevisions = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr("cat:bld").defined());
    WIKI_TEST_REQUIRE_EQUAL(bldRevisions.size(), 2);

    // Checking geom diff for group move in history of each object
    for (const auto& rev : bldRevisions) {
        checkDiffForGeometry(rev.commitId(), rev.objectId());
    }
    auto commitId = bldRevisions.front().commitId();
    // Checking geom diff for group move in feed (no object selected)
    checkDiffForGeometry(commitId, 0);

    // Check diff/geometry by bbox

    const auto& fullEnvelope = diffEnvelope(commitId, 0);
    checkGeomDiffForGeometry(commitId, 0, fullEnvelope, 4);

    geos::geom::Coordinate centre;
    fullEnvelope.centre(centre);
    geos::geom::Envelope topRight(centre.x, centre.y, fullEnvelope.getMaxX(), fullEnvelope.getMaxY());
    geos::geom::Envelope bottomLeft(fullEnvelope.getMinX(), fullEnvelope.getMinY(), centre.x, centre.y);
    checkGeomDiffForGeometry(commitId, 0, topRight, 2);
    checkGeomDiffForGeometry(commitId, 0, bottomLeft, 2);
    geos::geom::Envelope outside(
        fullEnvelope.getMinX() - fullEnvelope.getWidth(),
        fullEnvelope.getMinY() - fullEnvelope.getHeight(),
        fullEnvelope.getMinX(),
        fullEnvelope.getMinY());
    checkGeomDiffForGeometry(commitId, 0, outside, 0);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
