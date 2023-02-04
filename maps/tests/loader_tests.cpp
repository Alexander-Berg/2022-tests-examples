#include "helpers.h"

#include <maps/wikimap/mapspro/libs/validator/common/categories_list.h>
#include <maps/wikimap/mapspro/libs/validator/common/magic_strings.h>

#include <yandex/maps/wiki/validator/validator.h>

#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/registar.h>

#include <boost/algorithm/string/predicate.hpp>

#include <algorithm>
#include <vector>
#include <unordered_map>
#include <set>

namespace gl = maps::geolib3;
namespace rev = maps::wiki::revision;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

namespace {

const std::string CATEGORY_ATTR_PREFIX = "cat:";

// Moscow bbox wkt.
const std::string AOI_WKT = "POLYGON(("
    "4097012 7354775,4097012 7514504,4226528 7514504,4226528 7354775,4097012 7354775))";

typedef std::unordered_map<TCategoryId, std::set<RevisionID>> CategoryIdToRevisionIds;

CategoryIdToRevisionIds expectedCategoryRevisionIds()
{
    auto revisionTxn = revisionPgPool()->slaveTransaction();
    auto rg = revGateway(*revisionTxn);
    auto snapshot = rg.snapshot(rg.headCommitId());

    auto revisionIds = snapshot.revisionIdsByFilter(
        revision::filters::ObjRevAttr::isNotDeleted()
    );
    auto revisions = snapshot.reader().loadRevisions(revisionIds);

    CategoryIdToRevisionIds ret;

    for (const auto& revision : revisions) {
        ASSERT(revision.data().attributes);
        for (const auto& pair : *revision.data().attributes) {
            if (boost::algorithm::starts_with(pair.first, CATEGORY_ATTR_PREFIX)) {
                TCategoryId catId = pair.first.substr(CATEGORY_ATTR_PREFIX.length());
                if (allCategoryIds().count(catId)) {
                    ret[catId].insert(revision.id());
                }
            }
        }
    }

    return ret;
}

void checkResult(
        const ResultPtr& result,
        const CategoryIdToRevisionIds& expected,
        const std::string& testName)
{
    const Messages messages = result->drainAllMessages();

    std::unordered_map<TCategoryId, std::set<RevisionID>> got;
    for (const Message& message : messages) {
        UNIT_ASSERT_VALUES_EQUAL(message.revisionIds().size(), 1);
        got[message.attributes().description].insert(message.revisionIds()[0]);
    }

    for (const auto& pair : expected) {
        std::vector<RevisionID> notLoaded;
        std::set_difference(
            pair.second.begin(), pair.second.end(),
            got[pair.first].begin(), got[pair.first].end(),
            std::back_inserter(notLoaded)
        );
        UNIT_ASSERT_C(
            notLoaded.empty(),
            "some objects not loaded for category " << pair.first
            << " in test " << testName
            << ", ids: " << revisionIdsToString(notLoaded)
        );

        std::vector<RevisionID> extraLoaded;
        std::set_difference(
            got[pair.first].begin(), got[pair.first].end(),
            pair.second.begin(), pair.second.end(),
            std::back_inserter(extraLoaded)
        );
        UNIT_ASSERT_C(
            extraLoaded.empty(),
            "extra objects loaded for category " << pair.first
            << " in test " << testName
            << ", ids: " << revisionIdsToString(extraLoaded)
        );
    }
}

} // namespace

Y_UNIT_TEST_SUITE_F(loader, DbFixture) {

Y_UNIT_TEST(test_all_categories_load)
{
    const gl::Polygon2 aoi = gl::WKT::read<gl::Polygon2>(AOI_WKT);

    const DBID headCommitId = loadJson(
        *revisionPgPool(),
        dataPath("all_categories.data.json"));

    CategoryIdToRevisionIds expectedRevisionIdsByCategory = expectedCategoryRevisionIds();
    for (const TCategoryId& category : allCategoryIds()) {
        UNIT_ASSERT_C(
            !expectedRevisionIdsByCategory[category].empty(),
            "no test objects for category " << category
        );
    }

    const ResultPtr result = validator.run(
        {"categories_load"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    );
    checkResult(result, expectedRevisionIdsByCategory, "load_all");

    const ResultPtr byAoiResult = validator.run(
        {"categories_load"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId,
        aoi,
        "./test_all_categories_load"
    );
    checkResult(byAoiResult, expectedRevisionIdsByCategory, "load_by_aoi");
}

Y_UNIT_TEST(test_failed_data_load)
{
    RevisionID roadElementId;

    {   // create road element without start junction (2 commits)
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        const RevisionID endJunctionId = createJunction(rg, "POINT(1 1)");
        roadElementId = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)", boost::none, endJunctionId.objectId()
        );

        revisionTxn->commit();
    }

    const Messages messages = validator.run(
        {"report_all"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        roadElementId.commitId(),
        aoi,
        "./test_failed_data_load"
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    checkMessage(messages[0], "bad-start-junction-relation", {roadElementId});
}

Y_UNIT_TEST(test_missing_related_object_base_check)
{
    RevisionID missingStartJunctionId, roadElementId;
    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        missingStartJunctionId = RevisionID::createNewID(666);
        roadElementId = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            missingStartJunctionId.objectId(),
            createJunction(rg, "POINT(1 1)").objectId()
        );

        revisionTxn->commit();
    }

    const Messages messages = validator.run(
        {"rd_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        roadElementId.commitId()
    )->drainAllMessages();

    checkMessages(
        messages, {
            {"missing-related-object", {roadElementId, missingStartJunctionId}},
            {"bad-start-junction-relation", {roadElementId}}
        }
    );
}

Y_UNIT_TEST(test_deleted_related_object_base_check)
{
    RevisionID deletedStartJunctionId, roadElementId;
    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        RevisionID startJunctionId = createJunction(rg, "POINT(0 0)");
        roadElementId = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            startJunctionId.objectId(),
            createJunction(rg, "POINT(1 1)").objectId()
        );

        deletedStartJunctionId = deleteObject(rg, startJunctionId.objectId());

        revisionTxn->commit();
    }

    const Messages messages = validator.run(
        {"rd_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        deletedStartJunctionId.commitId()
    )->drainAllMessages();

    checkMessages(
        messages, {
            {"deleted-related-object", {roadElementId, deletedStartJunctionId}},
            {"bad-start-junction-relation", {roadElementId}}
        }
    );
}

Y_UNIT_TEST(test_duplicate_relations_base_check)
{
    const DBID headCommitId = loadJson(
        *revisionPgPool(), dataPath("duplicate_relations_base_check.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    checkMessage(messages[0], MESSAGE_DUPLICATE_RELATION, {{10, 1}, {12, 1}});
}

// small bbox inside AOI_WKT around disputable ad
const std::string DISP_AD_AOI_WKT =
    "POLYGON((4197874 7465453, 4197876 7465453"
    ",4197876 7465455, 4197874 7465455, 4197874 7465453))";

const DBID AD_SUBST_BY_AOI_ID = 21001;
const DBID GENERAL_AD_BY_AOI_ID = 21000;
const DBID PARENT_AD_BY_AOI_ID = 21002;

Y_UNIT_TEST(test_ad_subst_ad_relations_by_aoi_load)
{
    const gl::Polygon2 aoi = gl::WKT::read<gl::Polygon2>(DISP_AD_AOI_WKT);

    const DBID headCommitId = loadJson(
        *revisionPgPool(), dataPath("all_categories.data.json"));

    const ResultPtr byAoiResult = validator.run(
        {"categories_load"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId,
        aoi,
        "./test_ad_subst_ad_relations_by_aoi_load"
    );

    const Messages messages = byAoiResult->drainAllMessages();

    std::unordered_map<TCategoryId, std::set<DBID>> got;
    for (const auto& message : messages) {
        UNIT_ASSERT_VALUES_EQUAL(message.revisionIds().size(), 1);
        got[message.attributes().description].insert(message.revisionIds()[0].objectId());
    }

    UNIT_ASSERT(got.count(categories::AD_SUBST::id()));
    UNIT_ASSERT(got.count(categories::AD::id()));
    UNIT_ASSERT(got.at(categories::AD::id()).count(GENERAL_AD_BY_AOI_ID));
    UNIT_ASSERT(got.at(categories::AD::id()).count(PARENT_AD_BY_AOI_ID));
    UNIT_ASSERT(got.at(categories::AD_SUBST::id()).count(AD_SUBST_BY_AOI_ID));
}

}

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
