#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/editor_client/include/exception.h>
#include <maps/wikimap/mapspro/libs/editor_client/include/instance.h>
#include <maps/wikimap/mapspro/libs/editor_client/impl/request.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/point.h>

#include <iostream>

using namespace maps::wiki;
using namespace maps::wiki::editor_client;
using namespace maps::wiki::revision;
using namespace maps::wiki::poi_conflicts;

namespace {
const UserID WIKIMAPS = 82282794;
const std::string NKBETA_URL = "http://core-nmaps-editor.unstable.maps.n.yandex.ru/";
const std::string ERR_MISSING_OBJECT = "ERR_MISSING_OBJECT";
}

Y_UNIT_TEST_SUITE(instance) {
Y_UNIT_TEST(get_missing_object_exception)
{
    Instance instance(NKBETA_URL, WIKIMAPS);
    try {
        instance.getObjectById(11111);
        UNIT_FAIL("Didn't get ERR_MISSING_OBJECT from editor backend.");
    } catch (const ServerException& ex) {
        UNIT_ASSERT_VALUES_EQUAL(ex.status(), ERR_MISSING_OBJECT);
    }
}

Y_UNIT_TEST(create_get_lasso_delete_object)
{
    Instance instance(NKBETA_URL, WIKIMAPS);
    BasicEditorObject newObject;
    newObject.categoryId = "poi_goverment";
    newObject.setGeometryInGeodetic(maps::geolib3::Point2(33.080002297, 68.903202051));
    const auto savedObject = instance.saveObject(newObject, {{0, "bebe"}});
    const auto id =  savedObject.id;
    auto read = instance.getObjectById(id);
    UNIT_ASSERT(!read.deleted);
    UNIT_ASSERT(read.revisionId.has_value());
    try {
        auto byLasso = instance.getObjectsByLasso(
            {newObject.categoryId},
            *newObject.getGeometryInMercator(),
            0.1,
            100,
            GeomPredicate::CoveredBy);
        UNIT_ASSERT(!byLasso.empty());
        auto it = std::find_if(byLasso.begin(), byLasso.end(),
            [&](const auto& obj) {
                return obj.id == id;
            });
        UNIT_ASSERT(it != byLasso.end());
        UNIT_ASSERT(it->categoryId == newObject.categoryId);
    } catch (const ServerException& ex) {
        UNIT_FAIL("Server responded:\n" << ex.serverResponse());
    }
    instance.deleteObject(id);
    auto reread = instance.getObjectById(id);
    UNIT_ASSERT(reread.deleted);
}

Y_UNIT_TEST(get_top_history_record)
{
    Instance instance(NKBETA_URL, WIKIMAPS);
    const auto history = instance.getHistory(1543070223, 1, 1);
    UNIT_ASSERT_EQUAL(history.size(), 1);
    UNIT_ASSERT_EQUAL(history[0].id, 22088332);
    UNIT_ASSERT_EQUAL(history[0].author, 82282794);
}

namespace {
bool contains(const editor_client::PoiConflicts& poiConflicts, DBID objectId)
{
    std::map<size_t, std::map<poi_conflicts::ConflictSeverity, std::vector<ObjectIdentity>>> zoomToConflictingObjects;
    for (const auto& [_, severityToObjects] : poiConflicts.zoomToConflictingObjects) {
        for (const auto& [_, objects] : severityToObjects) {
            for (const auto& object : objects) {
                if (object.id == objectId) {
                    return true;
                }
            }
        }
    }
    return false;
}
} // namespace

Y_UNIT_TEST(create_get_conflicts_delete_object)
{
    Instance instance(NKBETA_URL, WIKIMAPS);
    BasicEditorObject newObject;
    newObject.categoryId = "poi_goverment";
    const maps::geolib3::Point2 position(34.080002297, 69.903202051);
    const auto mercatorPoint = maps::geolib3::convertGeodeticToMercator(position);
    newObject.setGeometryInGeodetic(position);
    const auto savedObject = instance.saveObject(newObject);
    const auto id =  savedObject.id;
    auto read = instance.getObjectById(id);
    UNIT_ASSERT(!read.deleted);
    UNIT_ASSERT(read.revisionId.has_value());
    try {
        auto conflicts = instance.getPoiConflicts(
            {},
            {},
            mercatorPoint,
            IsGeoproduct::False);
        UNIT_ASSERT(contains(conflicts, id));
        auto conflictsSelf = instance.getPoiConflicts(
            {id},
            {},
            mercatorPoint,
            IsGeoproduct::False);
        UNIT_ASSERT(!contains(conflictsSelf, id));
    } catch (const ServerException& ex) {
        UNIT_FAIL("Server responded:\n" << ex.serverResponse());
    }
    instance.deleteObject(id);
    auto reread = instance.getObjectById(id);
    UNIT_ASSERT(reread.deleted);
}

} // Y_UNIT_TEST_SUITE(instance)
