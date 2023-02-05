#include "helpers.h"
#include "../revision/db_access.h"
#include "../revision/geom_index.h"
#include "../revision/snapshot_impl.h"

#include <maps/libs/common/include/exception.h>
#include <yandex/maps/wiki/common/geom.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/objectrevision.h>
#include <yandex/maps/wiki/revision/snapshot.h>

#include <memory>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

namespace {

const Envelope ALL_OBJECTS_ENVELOPE { 4188000, 4890000, 7473000, 7475000 };

const TId BBOX_OBJECT_ID = 107;

const GeomIndexPtr FAKE_GEOM_INDEX_PTR(new GeomIndex({}));

struct DataContext
{
    DataContext()
    {
        editorConfig.reset(new EditorConfig(EDITOR_CONFIG_PATH));
        snapshotsPairPtr.reset(new SnapshotsPair(loadData(
                dataPath("tests_data/longtask_snapshot_tests.before.json"),
                dataPath("tests_data/longtask_snapshot_tests.after.json"))));

#ifdef USE_VIEW
        ViewDB::syncView(snapshotsPairPtr->oldBranch.id());
        ViewDB::syncView(snapshotsPairPtr->newBranch.id());
#endif

        oldSnapshotPtr.reset(new LongtaskSnapshot(LongtaskSnapshot::Impl(
                RevSnapshotFactory(
                        snapshotsPairPtr->oldBranch,
                        snapshotsPairPtr->oldSnapshotId,
                        RevisionDB::pool()),
                ViewTxnFactory(
                        snapshotsPairPtr->oldBranch,
                        ViewDB::pool()),
                FAKE_GEOM_INDEX_PTR,
                *editorConfig)));
        newSnapshotPtr.reset(new LongtaskSnapshot(LongtaskSnapshot::Impl(
                RevSnapshotFactory(
                        snapshotsPairPtr->newBranch,
                        snapshotsPairPtr->newSnapshotId,
                        RevisionDB::pool()),
                ViewTxnFactory(
                        snapshotsPairPtr->newBranch,
                        ViewDB::pool()),
                FAKE_GEOM_INDEX_PTR,
                *editorConfig)));
    }

    std::unique_ptr<EditorConfig> editorConfig;
    std::unique_ptr<SnapshotsPair> snapshotsPairPtr;
    std::unique_ptr<LongtaskSnapshot> oldSnapshotPtr;
    std::unique_ptr<LongtaskSnapshot> newSnapshotPtr;
};

DataContext& dataContext()
{
    static DataContext dc;
    return dc;
}

Envelope bboxOf(
        const revision::Branch& branch,
        const revision::SnapshotId& snapshotId,
        TId objectId)
{
    auto txn = RevisionDB::pool().slaveTransaction();
    revision::RevisionsGateway revGw(*txn, branch);
    auto snapshot = revGw.snapshot(snapshotId);
    auto optionalRev = snapshot.objectRevision(objectId);
    REQUIRE(optionalRev, "Nonexistent object " << objectId);
    REQUIRE(optionalRev->data().geometry,
           "Object " << objectId << " has no geometry");
    common::Geom geom(*optionalRev->data().geometry);
    auto* envelopePtr = geom.geosGeometryPtr()->getEnvelopeInternal();
    ASSERT(envelopePtr);
    return *envelopePtr;
}

bool intersectsByEnvelope(const Geom& geom, const Envelope& envelope)
{ return envelope.intersects(geom->getEnvelopeInternal()); }

} // namespace

Y_UNIT_TEST_SUITE_F(longtask_snapshot, SetLogLevelFixture) {

Y_UNIT_TEST(all_objects)
{
    UNIT_ASSERT_VALUES_EQUAL(
        dataContext().oldSnapshotPtr->primitivesByEnvelope(
            ALL_OBJECTS_ENVELOPE, GeometryType::All, {}).size(),
        17);

    UNIT_ASSERT_VALUES_EQUAL(
        dataContext().newSnapshotPtr->primitivesByEnvelope(
            ALL_OBJECTS_ENVELOPE, GeometryType::All, {}).size(),
        6);
}

Y_UNIT_TEST(bbox_filter)
{
    auto filterBbox = bboxOf(
        dataContext().snapshotsPairPtr->oldBranch, dataContext().snapshotsPairPtr->oldSnapshotId,
        BBOX_OBJECT_ID);

    auto oldObjectPtrs = dataContext().oldSnapshotPtr->primitivesByEnvelope(
            filterBbox, GeometryType::All, {});
    UNIT_ASSERT_VALUES_EQUAL(oldObjectPtrs.size(), 12);
    for (const auto& oldObjectPtr : oldObjectPtrs) {
        UNIT_ASSERT_C(
            intersectsByEnvelope(oldObjectPtr->geom(), filterBbox),
            "unexpected old object " << oldObjectPtr->id());
    }

    auto newObjectPtrs = dataContext().newSnapshotPtr->primitivesByEnvelope(
            filterBbox, GeometryType::All, {});
    UNIT_ASSERT_VALUES_EQUAL(newObjectPtrs.size(), 5);
    for (const auto& newObjectPtr : newObjectPtrs) {
        UNIT_ASSERT_C(
            intersectsByEnvelope(newObjectPtr->geom(), filterBbox),
            "unexpected new object " << newObjectPtr->id());
    }
}

Y_UNIT_TEST(cat_filter)
{
    auto rdElPtrs = dataContext().oldSnapshotPtr->primitivesByEnvelope(
            ALL_OBJECTS_ENVELOPE, GeometryType::All, {"rd_el"});
    UNIT_ASSERT_VALUES_EQUAL(rdElPtrs.size(), 3);
    for (const auto& rdElPtr : rdElPtrs) {
        UNIT_ASSERT_C(
            rdElPtr->categoryId() == "rd_el",
            "unexpected non-rd_el object " << rdElPtr->id());
    }

    auto bldPoiPtrs = dataContext().oldSnapshotPtr->primitivesByEnvelope(
            ALL_OBJECTS_ENVELOPE, GeometryType::All, {"poi_shopping", "bld"});
    UNIT_ASSERT_VALUES_EQUAL(bldPoiPtrs.size(), 3);
    for (const auto& bldPoiPtr : bldPoiPtrs) {
        UNIT_ASSERT_C(
            bldPoiPtr->categoryId() == "bld" ||
                bldPoiPtr->categoryId() == "poi_shopping",
            "unexpected non-bld non-poi object " << bldPoiPtr->id());
    }
}

Y_UNIT_TEST(geom_type_filter)
{
    auto pointPtrs = dataContext().oldSnapshotPtr->primitivesByEnvelope(
        ALL_OBJECTS_ENVELOPE, GeometryType::Point, {});
    UNIT_ASSERT_VALUES_EQUAL(pointPtrs.size() , 10);
    for (const auto& pointPtr : pointPtrs) {
        UNIT_ASSERT_STRINGS_EQUAL(
            pointPtr->geom().geometryTypeName(),
            common::Geom::geomTypeNamePoint);
    }

    auto linePtrs = dataContext().oldSnapshotPtr->primitivesByEnvelope(
        ALL_OBJECTS_ENVELOPE, GeometryType::LineString, {});
    UNIT_ASSERT_VALUES_EQUAL(linePtrs.size() , 5);
    for (const auto& linePtr : linePtrs) {
        UNIT_ASSERT_STRINGS_EQUAL(
            linePtr->geom().geometryTypeName(),
            common::Geom::geomTypeNameLine);
    }

    auto polyPtrs = dataContext().oldSnapshotPtr->primitivesByEnvelope(
        ALL_OBJECTS_ENVELOPE, GeometryType::Polygon, {});
    UNIT_ASSERT_VALUES_EQUAL(polyPtrs.size() , 2);
    for (const auto& polyPtr : polyPtrs) {
        UNIT_ASSERT_STRINGS_EQUAL(
            polyPtr->geom().geometryTypeName(),
            common::Geom::geomTypeNamePolygon);
    }
}

Y_UNIT_TEST(isolation)
{
    auto newRdElPtrs = dataContext().newSnapshotPtr->primitivesByEnvelope(
        ALL_OBJECTS_ENVELOPE, GeometryType::LineString, {"rd_el"});
    UNIT_ASSERT_VALUES_EQUAL(newRdElPtrs.size(), 1);
    auto newRdElId = newRdElPtrs.front()->id();

    auto oldRdElPtrs = dataContext().oldSnapshotPtr->primitivesByEnvelope(
        ALL_OBJECTS_ENVELOPE, GeometryType::LineString, {"rd_el"});
    UNIT_ASSERT_VALUES_EQUAL(oldRdElPtrs.size(), 3);
    for (const auto& oldRdElPtr : oldRdElPtrs) {
        UNIT_ASSERT_VALUES_UNEQUAL(oldRdElPtr->id(), newRdElId);
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
