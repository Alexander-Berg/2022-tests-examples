#include "db_fixture.h"
#include <maps/wikimap/jams_arm2/fastcgi/lib/globals.h>
#include <maps/wikimap/jams_arm2/fastcgi/lib/operations.h>
#include <maps/wikimap/jams_arm2/fastcgi/lib/generate_dataset.h>
#include <maps/libs/geolib/include/conversion.h>
#include <library/cpp/testing/unittest/registar.h>

#include "iostream"

namespace maps::wiki::jams_arm2 {

namespace {

namespace ic = maps::infopoints_client;

const std::string roadEventStr_1 = R"({
    "period":{
        "sinceDate":"2030-05-07",
        "sinceTime":"20:30:00",
        "tillDate":"2030-05-08",
        "tillTime":"21:30:00"
     },
     "point":{"type":"Point","coordinates":[37.4757,55.6465]},
     "closureLine":{"type":"LineString","coordinates":[
         [37.475669,55.646487],[37.475744,55.646546],[37.475857,55.646626]]},
     "type":"closed",
     "description":"desc_1",
     "name":"name_1"
     })";

const std::string roadEventStr_2 = R"({
    "period":{
        "sinceDate":"2030-05-07",
        "sinceTime":"20:30:00",
        "tillDate":"2030-06-08",
        "tillTime":"14:30:00"
     },
     "point":{"type":"Point","coordinates":[37.4757,55.6465]},
     "closureLine":{"type":"LineString","coordinates":[
         [37.475669,55.646487],[37.475744,55.646546],[37.475857,55.646626]]},
     "type":"closed",
     "description":"desc_2",
     "name":"name_2"
     })";


const std::string closureTemplateStr_1 = R"({
     "schedule":[
      {
        "sinceDate":"2030-05-07",
        "sinceTime":"20:30:00",
        "tillDate":"2030-05-08",
        "tillTime":"14:30:00"
      },
      {
        "sinceDate":"2030-05-08",
        "sinceTime":"21:30:00",
        "tillDate":"2030-05-08",
        "tillTime":"23:30:00"
      },
      {
        "sinceDate":"2030-05-11",
        "sinceTime":"21:30:00",
        "tillDate":"2030-05-11",
        "tillTime":"23:30:00"
      }],
     "point":{"type":"Point","coordinates":[37.4757,55.6465]},
     "closureLine":{"type":"LineString","coordinates":[
         [37.475669,55.646487],[37.475744,55.646546],[37.475857,55.646626]]},
     "type":"closed",
     "description":"template_desc_1",
     "name":"template_name_1"
    })";

const std::string closureTemplateStr_2 = R"({
     "schedule":[
      {
        "sinceDate":"2030-05-08",
        "sinceTime":"21:30:00",
        "tillDate":"2030-05-08",
        "tillTime":"23:30:00"
      },
      {
        "sinceDate":"2030-05-11",
        "sinceTime":"21:30:00",
        "tillDate":"2030-05-11",
        "tillTime":"23:30:00"
      }],
     "point":{"type":"Point","coordinates":[37.4757,55.6465]},
     "closureLine":{"type":"LineString","coordinates":[
         [37.475669,55.646487],[37.475744,55.646546],[37.475857,55.646626]]},
     "type":"closed",
     "description":"template_desc_2",
     "name":"template_name_2"
    })";

const std::string areaStr_1 = R"({
    "description": "area_1 descr",
    "bounds": {"type":"Polygon","coordinates": [[[0,0],[0,40],[10,40],[10,0],[0,0]]]},
    "tags": ["poi", "admin"],
    "texts": ["first text", "второй текст", "third text", "';\"%) _ (,\" %('''"],
    "objectIds": ["1","2020","4040404040","not_a_number"]
    })";

std::string areaDescription_2 = "Another text";
geolib3::Polygon2 areaBounds_2 = geolib3::Polygon2({{5, 0}, {0, 30}, {15, 30}, {15, 0}, {5, 0}});
std::vector<std::string> areaTexts_2 = { "Several", "Texts" };
std::vector<db::BlacklistTag> areaTags_2 = { db::BlacklistTag::Transit, db::BlacklistTag::Road, db::BlacklistTag::Building };

db::User testUser() {
    db::User user;
    user.setUid(553)
        .setLogin("test_user")
        .setAggregatorUserUri("test_aggregatorUserUri")
        .setRole(db::UserRole::Expert);
    return user;
}

void createTestUsers(maps::pgpool3::Pool& pool) {
    auto txn = pool.masterWriteableTransaction();
    auto ugw = db::UserGateway(*txn);

    db::User robotUser;
    robotUser.setUid(12345)
        .setLogin("robot_user")
        .setAggregatorUserUri("robot_aggregatorUserUri")
        .setRole(db::UserRole::Expert);
    ugw.insert(robotUser);
    ugw.insert(testUser());
    txn->commit();
}

} // namespace


Y_UNIT_TEST_SUITE()
{
Y_UNIT_TEST(RoadEvent)
{
    test::DbFixture fixture;
    Globals::get().initForTest(fixture.poolPtr());
    createTestUsers(fixture.pool());

    int id = -1;

    auto txn = fixture.pool().masterWriteableTransaction();

    { // create
        auto jsonVal = json::Value::fromString(roadEventStr_1);
        auto roadEventPostBody = roadEventPostBodyFromJson(jsonVal);

        ClosureDbOperationResult result = createRoadEvent(*txn, roadEventPostBody, testUser());
        UNIT_ASSERT(result.needCommit);
        UNIT_ASSERT_EQUAL(result.roadEvent.closure()->infopoint().timeBegin(),
                    chrono::parseSqlDateTime("2030-05-07 20:30:00+00:00"));
        UNIT_ASSERT_EQUAL(result.roadEvent.closure()->infopoint().name(), "name_1");

        id = result.roadEvent.closure()->key().id;
    }

    auto closures = db::closures(*txn);
    UNIT_ASSERT_EQUAL(closures.size(), 1);
    UNIT_ASSERT_EQUAL(closures[0].infopoint().description(), "desc_1");
    UNIT_ASSERT_EQUAL(closures[0].key().version, 0);
    auto ids = db::closureIdsForAggregatorUpdate(*txn);
    UNIT_ASSERT_EQUAL(ids.size(), 1);
    UNIT_ASSERT_EQUAL(ids[0], id);

    { // update
        auto jsonVal = json::Value::fromString(roadEventStr_2);
        auto roadEventPostBody = roadEventPostBodyFromJson(jsonVal);

        auto existingClosure = db::closureById(*txn, id, db::DbOpType::UpdateOp);

        UNIT_ASSERT(existingClosure);

        ClosureDbOperationResult result =
            updateRoadEvent(*txn, roadEventPostBody, testUser(), existingClosure, std::to_string(id));

        UNIT_ASSERT(result.needCommit);
        UNIT_ASSERT_EQUAL(result.roadEvent.closure()->infopoint().timeEnd(),
                    chrono::parseSqlDateTime("2030-06-08 14:30:00+00:00"));
        UNIT_ASSERT_EQUAL(result.roadEvent.closure()->infopoint().name(), "name_2");
    }

    closures = db::closures(*txn);
    UNIT_ASSERT_EQUAL(closures.size(), 1);
    UNIT_ASSERT_EQUAL(closures[0].infopoint().description(), "desc_2");
    UNIT_ASSERT_EQUAL(closures[0].key().id, id);
    UNIT_ASSERT_EQUAL(closures[0].key().version, 1);
    ids = db::closureIdsForAggregatorUpdate(*txn);
    UNIT_ASSERT_EQUAL(ids.size(), 1);
    UNIT_ASSERT_EQUAL(ids[0], id);

    txn->commit();
    txn = fixture.pool().masterWriteableTransaction();

    { // tile
        geolib3::BoundingBox bbox(
            geolib3::convertGeodeticToMercator(geolib3::Point2{37.4756,55.6464}),
            geolib3::convertGeodeticToMercator(geolib3::Point2{37.4759,55.6467}));

        auto result = roadEventsInBBox("", bbox, std::vector<ic::Type>());

        UNIT_ASSERT_EQUAL(result.size(), 1);
        UNIT_ASSERT(!result[0].infopoint());
        UNIT_ASSERT_EQUAL(result[0].closure()->infopoint().description(), "desc_2");
    }

    { // delete
        auto existingClosure = db::closureById(*txn, id, db::DbOpType::UpdateOp);

        UNIT_ASSERT(existingClosure);
        bool result = deleteRoadEvent(*txn, testUser(), existingClosure, std::to_string(id));

        UNIT_ASSERT(result);
    }

    closures = db::closures(*txn);
    UNIT_ASSERT_EQUAL(closures.size(), 0);
    ids = db::closureIdsForAggregatorUpdate(*txn);
    UNIT_ASSERT_EQUAL(ids.size(), 1);

    {
        auto version = db::closureById(*txn, id, db::DbOpType::UpdateOp, true);
        UNIT_ASSERT(version);
        UNIT_ASSERT(version->deleted());
        UNIT_ASSERT_EQUAL(version->key().version, 2);
    }

    { // from infopoint
        auto jsonVal = json::Value::fromString(roadEventStr_2);
        auto roadEventPostBody = roadEventPostBodyFromJson(jsonVal);

        ClosureDbOperationResult result = updateRoadEvent(
            *txn, roadEventPostBody, testUser(), std::optional<db::Closure>(), "fake-infopoint-id");

        UNIT_ASSERT(result.needCommit);
        UNIT_ASSERT_EQUAL(result.roadEvent.closure()->infopoint().timeEnd(),
                    chrono::parseSqlDateTime("2030-06-08 14:30:00+00:00"));
        UNIT_ASSERT_EQUAL(result.roadEvent.closure()->infopoint().name(), "name_2");

        id = result.roadEvent.closure()->key().id;
    }

    ids = db::closureIdsForAggregatorUpdate(*txn);
    UNIT_ASSERT_EQUAL(ids.size(), 2);
    UNIT_ASSERT_EQUAL(ids[1], id);

    { // getRoadEvent
        auto result = getRoadEvent(*txn, "fake-infopoint-id");
        UNIT_ASSERT_EQUAL(result.closure()->key().id, id);

        auto result2 = getRoadEvent(*txn, std::to_string(id));
        UNIT_ASSERT_EQUAL(*result2.closure()->infopointId(), "fake-infopoint-id");
    }

//    txn->commit();
}


Y_UNIT_TEST(ClosureTemplate)
{
    test::DbFixture fixture;
    Globals::get().initForTest(fixture.poolPtr());
    createTestUsers(fixture.pool());

    int id = -1;

    auto txn = fixture.pool().masterWriteableTransaction();

    { // create
        auto jsonVal = json::Value::fromString(closureTemplateStr_1);
        auto newClosureTemplate = newClosureTemplateFromJson(jsonVal);

        auto result = createClosureTemplate(*txn, newClosureTemplate, testUser().uid());

        UNIT_ASSERT_EQUAL(result.dbClosureTemplate().infopointName(), "template_name_1");
        id = result.dbClosureTemplate().key().id;
    }

    auto closures = db::closures(*txn);
    UNIT_ASSERT_EQUAL(closures.size(), 1);
    UNIT_ASSERT_EQUAL(closures[0].infopoint().description(), "template_desc_1");
    UNIT_ASSERT_EQUAL(closures[0].infopoint().timeBegin(),
                    chrono::parseSqlDateTime("2030-05-07 20:30:00+00:00"));
    auto ids = db::closureIdsForAggregatorUpdate(*txn);
    UNIT_ASSERT_EQUAL(ids.size(), 1);

    { // update
        auto jsonVal = json::Value::fromString(closureTemplateStr_2);
        auto newClosureTemplate = newClosureTemplateFromJson(jsonVal);

        auto result = updateClosureTemplate(*txn, newClosureTemplate, testUser().uid(), id);

        UNIT_ASSERT_EQUAL(result.dbClosureTemplate().infopointName(), "template_name_2");
        UNIT_ASSERT_EQUAL(result.dbClosureTemplate().key().id, id);
    }

    closures = db::closures(*txn);
    UNIT_ASSERT_EQUAL(closures.size(), 1);
    UNIT_ASSERT_EQUAL(closures[0].infopoint().description(), "template_desc_2");
    UNIT_ASSERT_EQUAL(closures[0].infopoint().timeBegin(),
                    chrono::parseSqlDateTime("2030-05-08 21:30:00+00:00"));
    ids = db::closureIdsForAggregatorUpdate(*txn);
    UNIT_ASSERT_EQUAL(ids.size(), 1);

    auto closureTemplates = getClosureTemplates(*txn);
    UNIT_ASSERT_EQUAL(closureTemplates.size(), 1);

    auto closureTemplate = getClosureTemplate(*txn, id);
    UNIT_ASSERT_EQUAL(closureTemplate.dbClosureTemplate().infopointName(), "template_name_2");

    // delete
    deleteClosureTemplate(*txn, testUser().uid(), id);

    closures = db::closures(*txn);
    UNIT_ASSERT(closures.empty());

    closureTemplates = getClosureTemplates(*txn);
    UNIT_ASSERT(closureTemplates.empty());
}


Y_UNIT_TEST(RendererBlacklist)
{
    test::DbFixture fixture;
    Globals::get().initForTest(fixture.poolPtr());
    createTestUsers(fixture.pool());

    auto txn = fixture.pool().masterWriteableTransaction();

    { // create
        auto jsonVal = json::Value::fromString(areaStr_1);
        auto area = blacklistAreaFromJson(jsonVal);
        createBlacklistArea(*txn, area);
    }

    auto areas = getBlacklistAreas(*txn);
    UNIT_ASSERT_EQUAL(areas.size(), 1);
    UNIT_ASSERT_EQUAL(areas[0].data().description, "area_1 descr");
    UNIT_ASSERT_EQUAL(areas[0].data().tags, std::vector<db::BlacklistTag>({db::BlacklistTag::Poi, db::BlacklistTag::Admin}));
    UNIT_ASSERT_EQUAL(areas[0].data().texts, std::vector<std::string>({"first text", "второй текст", "third text", "';\"%) _ (,\" %('''"}));
    UNIT_ASSERT_EQUAL(areas[0].data().objectIds, std::vector<std::string>({"1", "2020", "4040404040","not_a_number"}));

    const auto blacklistedObjectIds = getBlacklistedObjectIds(areas);
    UNIT_ASSERT_EQUAL(blacklistedObjectIds, "{\"blacklist\":[1,2020,4040404040]}");

    int id = areas[0].key();
    { // update
        auto area = db::BlacklistArea(areaBounds_2, {areaDescription_2, areaTags_2, areaTexts_2, {}});
        area.setKey(id);
        updateBlacklistArea(*txn, area);
    }

    areas = getBlacklistAreas(*txn);
    UNIT_ASSERT_EQUAL(areas.size(), 1);

    UNIT_ASSERT_EQUAL(areas[0].data().description, areaDescription_2);

    UNIT_ASSERT_EQUAL(areas[0].data().tags.size(), 3);
    for (int i = 0; i < 3; ++i)
        UNIT_ASSERT_EQUAL(areas[0].data().tags[i], areaTags_2[i]);

    UNIT_ASSERT_EQUAL(areas[0].data().texts.size(), 2);
    for (int i = 0; i < 2; ++i)
        UNIT_ASSERT_EQUAL(areas[0].data().texts[i], areaTexts_2[i]);

    // delete
    deleteBlacklistArea(*txn, id);
    areas = getBlacklistAreas(*txn);
    UNIT_ASSERT(areas.empty());
}

}; // Y_UNIT_TEST_SUITE

} //namespace maps::wiki::jams_arm2
