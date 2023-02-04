#include "common.h"

#include "../commit_writer.h"
#include "../object_diff.h"
#include "../object_loader.h"
#include "../relation_data.h"
#include <yandex/maps/wiki/groupedit/session.h>
#include <yandex/maps/wiki/groupedit/object.h>
#include <yandex/maps/wiki/groupedit/relation.h>

#include <yandex/maps/wiki/revision/commit.h>
#include <yandex/maps/wiki/revision/filters.h>
#include <yandex/maps/wiki/revision/objectrevision.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/threadutils/threadpool.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <maps/libs/common/include/exception.h>

#include <boost/test/unit_test.hpp>

#include <algorithm>
#include <atomic>
#include <condition_variable>
#include <functional>
#include <list>
#include <mutex>
#include <unordered_set>
#include <vector>

namespace maps {
namespace wiki {
namespace groupedit {
namespace tests {

namespace rf = revision::filters;

namespace {

const std::string AOI_WKB = wkt2wkb("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
const std::string LINEAR_AOI_WKB = wkt2wkb("LINESTRING (1 3, 5 3, 9 10)");

const std::string IN_POINT_WKB = wkt2wkb("POINT (3 3)");
const std::string IN_POLYLINE_WKB = wkt2wkb("LINESTRING (2 2, 6 7, 10 9)");
const std::string IN_POLYGON_WKB =
    wkt2wkb("POLYGON ((4 4, 10 4, 10 10, 4 10, 4 4))");

const std::string OUT_POINT_WKB = wkt2wkb("POINT (99 99)");

revision::RevisionsGateway revGateway(
    pqxx::transaction_base& txn,
    TBranchId branchId)
{
    return revision::RevisionsGateway(
        txn,
        revision::BranchManager(txn).load(branchId));
}

revision::RevisionsGateway::NewRevisionData createData(
        revision::RevisionsGateway& gateway,
        const std::string& geometryWkb,
        const revision::Attributes& attributes)
{
    revision::RevisionsGateway::NewRevisionData objData;

    objData.first = gateway.acquireObjectId();
    objData.second.geometry = geometryWkb;
    objData.second.attributes = attributes;

    return objData;
}

revision::RevisionsGateway::NewRevisionData createRelData(
        revision::RevisionsGateway& gateway,
        TObjectId masterId,
        TObjectId slaveId,
        const revision::Attributes& attributes)
{
    revision::RevisionsGateway::NewRevisionData relData;

    relData.first = gateway.acquireObjectId();
    relData.second.relationData = revision::RelationData(masterId, slaveId);
    relData.second.attributes = attributes;

    return relData;
}

class Barrier
{
public:
    explicit Barrier(size_t barrierCount)
        : barrierCount_(barrierCount)
        , waitingCount_(0)
    { }

    ~Barrier()
    {
        waitingCount_ = barrierCount_;
        reachedCond_.notify_all();
    }

    void await()
    {
        std::unique_lock<std::mutex> lock(mutex_);
        if (++waitingCount_ >= barrierCount_) {
            reachedCond_.notify_all();
            return;
        }

        reachedCond_.wait(lock,
            [this] { return waitingCount_ >= barrierCount_; });
    }

    void reset()
    { waitingCount_ = 0; }

private:
    const size_t barrierCount_;
    std::atomic<size_t> waitingCount_;
    std::mutex mutex_;
    std::condition_variable reachedCond_;
};

} // namespace

BOOST_GLOBAL_FIXTURE( SetLogLevelFixture );

BOOST_FIXTURE_TEST_CASE( test_loader, unittest::ArcadiaDbFixture )
{
    TObjectId inPointId = 0;
    TObjectId outPointId = 0;

    auto txn = pool().masterWriteableTransaction();
    auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);
    Session session(*txn, revision::TRUNK_BRANCH_ID);

    // Fill DB
    {
        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;

        cmtData.push_back(createData(rg, IN_POINT_WKB,
            {{"cat:jc", "1"}, {"cat:bound_jc", "1"}}));
        inPointId = cmtData.back().first.objectId();
        cmtData.push_back(createData(rg, IN_POLYLINE_WKB, {{"cat:el", "1"}}));
        cmtData.push_back(createData(rg, IN_POLYGON_WKB, {{"cat:poly", "1"}}));
        cmtData.push_back(createData(rg, OUT_POINT_WKB, {{"cat:jc", "1"}}));
        outPointId = cmtData.back().first.objectId();

        rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});
    }

    // Check load all
    {
        size_t count = 0;
        session
            .query(rf::True())
            .visit([&](const Object&) { ++count; });

        BOOST_CHECK_EQUAL(count, 4);
    }

    // Check load all within polygonal aoi
    {
        size_t count = 0;
        session.query(
                rf::True(),
                GeomPredicate::Within,
                AOI_WKB).visit(
            [&](const Object& obj)
            {
                ++count;
                BOOST_CHECK(obj.id() != outPointId);
            });

        BOOST_CHECK_EQUAL(count, 3);
    }

    // Check load all intersecting polygonal aoi
    {
        size_t count = 0;
        session.query(
                rf::True(),
                GeomPredicate::Intersects,
                AOI_WKB).visit(
            [&](const Object& obj)
            {
                ++count;
                BOOST_CHECK(obj.id() != outPointId);
            });

        BOOST_CHECK_EQUAL(count, 3);
    }

    // Check load all intersecting linear aoi
    {
        size_t count = 0;
        session.query(
                rf::True(),
                GeomPredicate::Intersects,
                LINEAR_AOI_WKB).visit(
            [&](const Object& obj)
            {
                ++count;
                BOOST_CHECK(obj.id() != outPointId);
            });

        BOOST_CHECK_EQUAL(count, 3);
    }

    // Check load all w/ filter
    {
        size_t count = 0;
        session
            .query(rf::Attr("cat:jc").defined()).visit(
            [&](const Object& obj)
            {
                ++count;
                BOOST_CHECK(obj.id() == outPointId || obj.id() == inPointId);
            });

        BOOST_CHECK_EQUAL(count, 2);
    }

    // Check load w/ filter & within aoi
    {
        size_t count = 0;
        session
            .query(
                rf::Attr("cat:jc").defined(),
                GeomPredicate::Within,
                AOI_WKB).visit(
            [&](const Object& obj)
            {
                ++count;
                BOOST_CHECK_EQUAL(obj.id(), inPointId);
            });

        BOOST_CHECK_EQUAL(count, 1);
    }
}

BOOST_AUTO_TEST_CASE( test_object )
{
    TObjectId pointId = 1;

    revision::ObjectData objData;
    objData.attributes = revision::Attributes{
        {"cat:jc", "1"},
        {"jc:test", "1"}
    };
    objData.geometry = IN_POINT_WKB;

    Object obj(
        revision::RevisionID::createNewID(pointId),
        objData,
        std::list<Relation>{});

    BOOST_CHECK_EQUAL(obj.category(), "jc");

    BOOST_REQUIRE_EQUAL(obj.id(), pointId);
    BOOST_CHECK(obj.attribute("cat:jc"));

    auto geom = obj.geometryWkb();
    BOOST_REQUIRE(geom);
    BOOST_REQUIRE(*geom == IN_POINT_WKB);
    BOOST_REQUIRE(!obj.hasChanges());

    // Degenerate updates
    obj.setGeometryWkb(*geom);
    obj.setAttribute("jc:test", "1");
    obj.removeAttribute("nonexistent");
    BOOST_REQUIRE(!obj.hasChanges());

    // Geometry type change
    BOOST_CHECK_THROW(obj.setGeometryWkb(IN_POLYGON_WKB), maps::Exception);
    {
        auto currGeom = obj.geometryWkb();
        BOOST_REQUIRE(currGeom);
        BOOST_CHECK(*currGeom == IN_POINT_WKB);
    }
    BOOST_REQUIRE(!obj.hasChanges());

    // Geometry update
    obj.setGeometryWkb(OUT_POINT_WKB);
    {
        auto currGeom = obj.geometryWkb();
        BOOST_REQUIRE(currGeom);
        BOOST_CHECK(*currGeom == OUT_POINT_WKB);
    }
    BOOST_CHECK(obj.hasChanges());
    obj.setGeometryWkb(IN_POINT_WKB);
    {
        auto currGeom = obj.geometryWkb();
        BOOST_REQUIRE(currGeom);
        BOOST_CHECK(*currGeom == IN_POINT_WKB);
    }
    BOOST_REQUIRE(!obj.hasChanges());

    // Attribute update
    obj.setAttribute("jc:test", "yes");
    {
        auto currAttrValue = obj.attribute("jc:test");
        BOOST_REQUIRE(currAttrValue);
        BOOST_CHECK_EQUAL(*currAttrValue, "yes");
    }
    BOOST_CHECK(obj.hasChanges());
    obj.setAttribute("jc:test", "1");
    {
        auto currAttrValue = obj.attribute("jc:test");
        BOOST_REQUIRE(currAttrValue);
        BOOST_CHECK_EQUAL(*currAttrValue, "1");
    }
    BOOST_REQUIRE(!obj.hasChanges());

    // Attribute insertion
    obj.setAttribute("jc:attr", "value");
    {
        auto currAttrValue = obj.attribute("jc:attr");
        BOOST_CHECK(currAttrValue && *currAttrValue == "value");
    }
    BOOST_CHECK(obj.hasChanges());
    obj.removeAttribute("jc:attr");
    BOOST_CHECK(!obj.attribute("jc:attr"));
    BOOST_REQUIRE(!obj.hasChanges());

    // Attribute deletion
    obj.removeAttribute("jc:test");
    BOOST_CHECK(!obj.attribute("jc:test"));
    BOOST_CHECK(obj.hasChanges());
    obj.setAttribute("jc:test", "maybe");
    {
        auto currAttrValue = obj.attribute("jc:test");
        BOOST_REQUIRE(currAttrValue);
        BOOST_CHECK_EQUAL(*currAttrValue, "maybe");
    }
    BOOST_CHECK(obj.hasChanges());
    obj.setAttribute("jc:test", "1");
    {
        auto currAttrValue = obj.attribute("jc:test");
        BOOST_REQUIRE(currAttrValue);
        BOOST_CHECK_EQUAL(*currAttrValue, "1");
    }
    BOOST_REQUIRE(!obj.hasChanges());

    // Final attributes state
    BOOST_CHECK(obj.attributes()
        == revision::Attributes({{"cat:jc", "1"}, {"jc:test", "1"}}));

    // Category updates
    BOOST_CHECK_THROW(obj.removeAttribute("cat:jc"), maps::Exception);
    BOOST_CHECK_THROW(
            obj.setAttribute("cat:jc", ""),
            maps::Exception);
    BOOST_REQUIRE(!obj.hasChanges());

    // Category change
    BOOST_CHECK_THROW(obj.changeCategory({}), maps::Exception);
    BOOST_CHECK_THROW(obj.changeCategory("poi:"), maps::Exception);

    obj.changeCategory("jc");
    BOOST_CHECK(!obj.hasChanges());

    obj.changeCategory("poi");
    BOOST_CHECK_EQUAL(obj.category(), "poi");
    BOOST_CHECK(obj.attributes()
        == revision::Attributes({{"cat:poi", "1"}, {"poi:test", "1"}}));
    BOOST_CHECK(obj.hasChanges());

    obj.changeCategory("jc");
    BOOST_CHECK(!obj.hasChanges());

    // Object deletion
    obj.setDeleted();
    BOOST_CHECK(obj.hasChanges());
}

BOOST_AUTO_TEST_CASE( test_object_diff )
{
    const size_t TESTS_NUM = 4;

    std::vector<Object> objects;

    // Create objects
    for (TObjectId curObjectId = 1; curObjectId <= TESTS_NUM; ++curObjectId) {
        revision::ObjectData objData;
        objData.attributes = revision::Attributes{
            {"cat:jc", "1"},
            {"jc:test", "1"}
        };
        objData.geometry = IN_POINT_WKB;

        objects.emplace_back(
            revision::RevisionID::createNewID(curObjectId),
            objData,
            std::list<Relation>{});

    }

    {
        auto getTestObj = [&objects]
        {
            REQUIRE(!objects.empty(), "Not enough test objects");
            Object obj = std::move(objects.back());
            objects.pop_back();
            return obj;
        };

        // No changes
        {
            ObjectDiff diff(getTestObj());
            BOOST_CHECK_EQUAL(diff.revisionsCount(), 0);
            auto cmtData = ObjectDiff::extractCommitData(std::move(diff));
            BOOST_CHECK(cmtData.empty());
        }

        // Geometry change
        {
            Object obj = getTestObj();
            TObjectId origId = obj.id();

            obj.setGeometryWkb(OUT_POINT_WKB);

            ObjectDiff diff(std::move(obj));
            BOOST_CHECK_EQUAL(diff.revisionsCount(), 1);

            auto cmtData = ObjectDiff::extractCommitData(std::move(diff));
            BOOST_REQUIRE_EQUAL(cmtData.size(), 1);
            const auto& objData = cmtData.front();

            BOOST_CHECK_EQUAL(objData.first.objectId(), origId);
            BOOST_CHECK(!objData.second.attributes);
            BOOST_CHECK(!objData.second.description);
            BOOST_CHECK(!objData.second.relationData);
            BOOST_CHECK(!objData.second.deleted);
            BOOST_CHECK(objData.second.geometry);
            BOOST_CHECK(*objData.second.geometry == OUT_POINT_WKB);
        }

        // Attributes change
        {
            Object obj = getTestObj();
            TObjectId origId = obj.id();

            obj.removeAttribute("jc:test");
            obj.setAttribute("jc:node", "1");

            BOOST_CHECK(obj.attributes()
                == revision::Attributes({{"cat:jc", "1"}, {"jc:node", "1"}}));

            ObjectDiff diff(std::move(obj));
            BOOST_CHECK_EQUAL(diff.revisionsCount(), 1);

            auto cmtData = ObjectDiff::extractCommitData(std::move(diff));
            BOOST_REQUIRE_EQUAL(cmtData.size(), 1);
            const auto& objData = cmtData.front();

            BOOST_CHECK_EQUAL(objData.first.objectId(), origId);
            BOOST_CHECK(!objData.second.description);
            BOOST_CHECK(!objData.second.geometry);
            BOOST_CHECK(!objData.second.relationData);
            BOOST_CHECK(!objData.second.deleted);
            BOOST_CHECK(objData.second.attributes);
            BOOST_CHECK(*objData.second.attributes
                == revision::Attributes({{"cat:jc", "1"}, {"jc:node", "1"}}));
        }

        // Deletion
        {
            Object obj = getTestObj();
            TObjectId origId = obj.id();

            obj.setDeleted();

            ObjectDiff diff(std::move(obj));
            BOOST_CHECK_EQUAL(diff.revisionsCount(), 1);

            auto cmtData = ObjectDiff::extractCommitData(std::move(diff));
            BOOST_REQUIRE_EQUAL(cmtData.size(), 1);

            const auto& objData = cmtData.front();
            BOOST_CHECK_EQUAL(objData.first.objectId(), origId);
            BOOST_CHECK(!objData.second.attributes);
            BOOST_CHECK(!objData.second.description);
            BOOST_CHECK(!objData.second.geometry);
            BOOST_CHECK(!objData.second.relationData);
            BOOST_CHECK(objData.second.deleted);
        }
    }
}

BOOST_AUTO_TEST_CASE( test_relation_delete )
{
    TObjectId jcId = 1;
    TObjectId elId = 2;
    TObjectId relId = 3;

    std::vector<Object> objects;

    // Create objects
    {
        auto relData = std::make_shared<RelationData>("start", elId, "el", jcId, "jc");
        relData->revisionId = revision::RevisionID::createNewID(relId);

        revision::ObjectData objData;

        objData.attributes = revision::Attributes{{"cat:el", "1"} };
        objData.geometry = IN_POLYLINE_WKB;

        std::list<Relation> elRelations;
        elRelations.emplace_back(elId, relData);

        objects.emplace_back(
            revision::RevisionID::createNewID(elId),
            objData,
            std::move(elRelations));

        objData.attributes = revision::Attributes{{"cat:jc", "1"}};
        objData.geometry = IN_POINT_WKB;

        std::list<Relation> jcRelations;
        jcRelations.emplace_back(jcId, relData);

        objects.emplace_back(
            revision::RevisionID::createNewID(jcId),
            objData,
            std::move(jcRelations));
    }

    for (Object& obj : objects) {
        auto relationsRange = obj.relations();
        BOOST_REQUIRE_EQUAL(
                std::distance(relationsRange.begin(), relationsRange.end()), 1);

        auto& rel = *relationsRange.begin();
        BOOST_CHECK_EQUAL(rel.role(), "start");

        if (obj.category() == "el") {
            BOOST_CHECK(rel.type() == Relation::Type::Slave);
            BOOST_CHECK_EQUAL(rel.otherId(), jcId);
            BOOST_CHECK_EQUAL(rel.otherCategory(), "jc");
        } else {
            BOOST_REQUIRE_EQUAL(obj.category(), "jc");
            BOOST_CHECK(rel.type() == Relation::Type::Master);
            BOOST_CHECK_EQUAL(rel.otherId(), elId);
            BOOST_CHECK_EQUAL(rel.otherCategory(), "el");
        }
    }

    // Delete relation concurrently, check deleted once
    {
        ThreadPool relDeleters(2);
        Barrier barrier(2);
        auto relDeleter = [&](Relation& rel)
        {
            barrier.await();
            rel.setDeleted();
        };
        for (Object& obj : objects) {
            relDeleters.push(
                    std::bind(relDeleter, std::ref(*obj.relations().begin())));
        }
        relDeleters.shutdown();

        bool relDeleted = false;
        for (Object& obj : objects) {
            ObjectDiff diff(std::move(obj));
            size_t diffRevisionsCount = diff.revisionsCount();
            auto cmtData = ObjectDiff::extractCommitData(std::move(diff));

            BOOST_REQUIRE_EQUAL(diffRevisionsCount, cmtData.size());

            if (!cmtData.empty()) {
                BOOST_REQUIRE_EQUAL(cmtData.size(), 1);

                const auto& rev = cmtData.front();
                BOOST_REQUIRE_EQUAL(rev.first.objectId(), relId);
                BOOST_CHECK(rev.second.deleted);
                BOOST_REQUIRE(!relDeleted);
                relDeleted = rev.second.deleted;
            }
        }
        BOOST_REQUIRE(relDeleted);
    }
}

BOOST_FIXTURE_TEST_CASE( test_relation_object_delete, unittest::ArcadiaDbFixture )
{
    static const size_t MASTER_OBJECTS_NUM = 10;
    static const size_t SLAVE_OBJECTS_NUM = 13;

    TObjectId idToDelete = 0;

    TObjectId unrelatedMaster = 0;
    TObjectId unrelatedSlave = 0;

    // Fill DB
    {
        auto txn = pool().masterWriteableTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
        cmtData.push_back(createData(rg, IN_POINT_WKB, {{"cat:jc", "1"}}));
        idToDelete = cmtData.front().first.objectId();

        for (size_t i = 0; i < MASTER_OBJECTS_NUM; ++i) {
            cmtData.push_back(createData(rg, IN_POLYLINE_WKB,
                {{"cat:el", "1"}}));
            cmtData.push_back(createRelData(rg,
                cmtData.back().first.objectId(), idToDelete,
                {{"rel:master", "el"}, {"rel:slave", "jc"},
                    {"rel:role", "start"}}));
        }
        unrelatedMaster = cmtData.back().first.objectId();

        for (size_t i = 0; i < SLAVE_OBJECTS_NUM; ++i) {
            cmtData.push_back(createData(rg, IN_POINT_WKB,
                {{"cat:jc_jc", "1"}}));
            cmtData.push_back(createRelData(rg,
                idToDelete, cmtData.back().first.objectId(),
                {{"rel:master", "jc"}, {"rel:slave", "jc_jc"},
                    {"rel:role", "meh"}}));
        }
        unrelatedSlave = cmtData.back().first.objectId();

        cmtData.push_back(createRelData(rg,
                unrelatedMaster, unrelatedSlave,
                {{"rel:master", "el"}, {"rel:slave", "jc_jc"},
                    {"rel:role", "blah"}}));

        rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});

        txn->commit();
    }

    // Check objects
    {
        auto txn = pool().slaveTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        size_t count = 0;

        session.query(rf::True()).visit(
            [&](const Object& obj) {
                ++count;

                auto relRange = obj.relations();
                if (obj.id() == idToDelete) {
                    BOOST_REQUIRE_EQUAL(
                            std::distance(relRange.begin(), relRange.end()),
                            MASTER_OBJECTS_NUM + SLAVE_OBJECTS_NUM);
                } else if (obj.id() == unrelatedMaster
                           || obj.id() == unrelatedSlave) {
                    BOOST_REQUIRE_EQUAL(
                            std::distance(relRange.begin(), relRange.end()), 2);
                } else {
                    BOOST_REQUIRE_EQUAL(
                            std::distance(relRange.begin(), relRange.end()), 1);
                }
            });

        BOOST_REQUIRE_EQUAL(count, 1 + MASTER_OBJECTS_NUM + SLAVE_OBJECTS_NUM);
    }

    // Delete object
    {
        auto txn = pool().masterWriteableTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        auto commitIds = session.query(std::vector<TObjectId>{idToDelete}).update(
            "test-action", TEST_UID,
            [](Object& obj) { obj.setDeleted(); });

        BOOST_REQUIRE(!commitIds.empty());
        txn->commit();
    }

    // Check relations are absent
    {
        auto txn = pool().slaveTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        size_t count = 0;

        session.query(rf::True()).visit(
            [&](const Object& obj) {
                ++count;

                BOOST_CHECK_NE(obj.id(), idToDelete);
                auto relRange = obj.relations();
                if (obj.id() == unrelatedMaster || obj.id() == unrelatedSlave) {
                    BOOST_CHECK_EQUAL(
                            std::distance(relRange.begin(), relRange.end()), 1);
                } else {
                    BOOST_CHECK(relRange.begin() == relRange.end());
                }
            });

        BOOST_REQUIRE_EQUAL(count, MASTER_OBJECTS_NUM + SLAVE_OBJECTS_NUM);
    }
}

BOOST_FIXTURE_TEST_CASE( test_writer, unittest::ArcadiaDbFixture )
{
    static const size_t OBJECTS_NUM = 10;
    static const size_t MAX_COMMIT_SIZE = 3;

    // Zero commit size
    {
        auto txn = pool().masterWriteableTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);
        BOOST_CHECK_THROW(
            CommitWriter().write(rg, TEST_UID, "test", 0),
            maps::Exception);
    }

    // Valid commit sizes
    for (size_t commitSize = 1; commitSize <= MAX_COMMIT_SIZE; ++commitSize) {
        std::vector<Object> objects;
        std::unordered_set<TObjectId> objectIds;
        size_t curObjectId = 1;

        // Fill DB
        for (size_t i = 0; i < OBJECTS_NUM; ++i) {
            revision::ObjectData objData;
            objData.attributes = revision::Attributes{{"cat:jc", "1"} };
            objData.geometry = IN_POLYLINE_WKB;

            objects.emplace_back(
                revision::RevisionID::createNewID(curObjectId),
                objData,
                std::list<Relation>{});

            objectIds.insert(curObjectId);
            ++curObjectId;
        }

        auto txn = pool().masterWriteableTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::vector<TCommitId> commitIds;
        // Write commits
        {
            ThreadPool pushers(OBJECTS_NUM);
            CommitWriter writer;

            Barrier barrier(OBJECTS_NUM);
            auto pusher = [&](Object& obj)
            {
                obj.setAttribute("test", "1");
                ObjectDiff diff(std::move(obj));

                barrier.await();
                writer.add(std::move(diff));
            };
            for (size_t i = 0; i < OBJECTS_NUM; ++i) {
                pushers.push(std::bind(pusher, std::ref(objects[i])));
            }
            pushers.shutdown();

            commitIds = writer.write(rg, TEST_UID, "test", commitSize);
            BOOST_REQUIRE_EQUAL(
                commitIds.size(),
                (OBJECTS_NUM + commitSize - 1) / commitSize);
        }

        // Check commit descriptions and contents
        {
            auto reader = rg.reader();
            for (TCommitId commitId : commitIds) {
                auto commit = revision::Commit::load(*txn, commitId);

                BOOST_CHECK_EQUAL(commit.createdBy(), TEST_UID);
                const auto& commitAttrs = commit.attributes();
                auto actionIt = commitAttrs.find("action");
                BOOST_CHECK(actionIt != std::end(commitAttrs)
                    && actionIt->second == "test");

                auto commitRevs = reader.commitRevisions(commitId);
                BOOST_CHECK(commitRevs.size() == commitSize
                        || commitRevs.size() == OBJECTS_NUM % commitSize);

                for (const auto& objRev : commitRevs) {
                    BOOST_CHECK(objectIds.erase(objRev.id().objectId()));

                    BOOST_REQUIRE(objRev.data().attributes);
                    BOOST_CHECK(objRev.data().attributes->count("test"));
                }
            }
            BOOST_CHECK(objectIds.empty());
        }
    }
}

BOOST_FIXTURE_TEST_CASE( test_object_wo_attrs, unittest::ArcadiaDbFixture )
{
    auto txn = pool().masterWriteableTransaction();
    auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

    std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
    revision::RevisionsGateway::NewRevisionData objData;

    objData.first = rg.acquireObjectId();
    objData.second.geometry = IN_POINT_WKB;
    cmtData.push_back(objData);
    rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});

    Session session(*txn, revision::TRUNK_BRANCH_ID);
    BOOST_CHECK_THROW(
            session.query(rf::True()).visit([](const Object&) { }),
            maps::Exception);
}

BOOST_FIXTURE_TEST_CASE( test_object_wo_cat, unittest::ArcadiaDbFixture )
{
    auto txn = pool().masterWriteableTransaction();
    auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

    std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
    cmtData.push_back(createData(rg, IN_POINT_WKB, {{"test", "1"}}));
    rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});

    Session session(*txn, revision::TRUNK_BRANCH_ID);
    BOOST_CHECK_THROW(
            session.query(rf::True()).visit([](const Object&) { }),
            maps::Exception);
}

BOOST_FIXTURE_TEST_CASE( test_object_multi_cat, unittest::ArcadiaDbFixture )
{
    auto txn = pool().masterWriteableTransaction();
    auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

    std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
    cmtData.push_back(createData(rg, IN_POINT_WKB, {
                {"cat:jc", "1"},
                    {"cat:node", "1"}}));
    rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});

    Session session(*txn, revision::TRUNK_BRANCH_ID);
    BOOST_CHECK_THROW(
            session.query(rf::True()).visit([](const Object&) { }),
            maps::Exception);
}

BOOST_FIXTURE_TEST_CASE( test_invalid_set_geom, unittest::ArcadiaDbFixture )
{
    auto txn = pool().masterWriteableTransaction();
    auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

    std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
    revision::RevisionsGateway::NewRevisionData objData;

    objData.first = rg.acquireObjectId();
    objData.second.attributes = revision::Attributes{{"cat:nogeom", "1"}};
    cmtData.push_back(objData);
    rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});

    Session session(*txn, revision::TRUNK_BRANCH_ID);
    BOOST_CHECK_THROW(
            session.query(rf::True()).update(
                "test", TEST_UID,
                [](Object& obj) { obj.setGeometryWkb(OUT_POINT_WKB); }),
            maps::Exception);
}

BOOST_FIXTURE_TEST_CASE( test_actions_visit, unittest::ArcadiaDbFixture )
{
    static const size_t OBJECTS_NUM = 1000;

    auto txn = pool().masterWriteableTransaction();

    // Fill DB
    {
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
        for (size_t i = 0; i < OBJECTS_NUM; ++i) {
            cmtData.push_back(createData(rg, IN_POINT_WKB, {{"cat:jc", "1"}}));
        }
        rg.createCommit(std::move(cmtData), TEST_UID,
            {{"description", "data"}});
    }

    std::unordered_set<TObjectId> seenObjectIds;
    size_t totalObjectsSeen = 0;

    // Run visitor
    {
        Session session(*txn, revision::TRUNK_BRANCH_ID);
        session.query(rf::True()).visit(
            [&](const Object& obj) {
                seenObjectIds.insert(obj.id());
                ++totalObjectsSeen;
            });

        BOOST_CHECK_EQUAL(seenObjectIds.size(), OBJECTS_NUM);
        BOOST_CHECK_EQUAL(totalObjectsSeen, OBJECTS_NUM);
    }
}

BOOST_FIXTURE_TEST_CASE( test_actions_modify, unittest::ArcadiaDbFixture )
{
    static const size_t OBJECTS_NUM = 10;
    const std::string COMMIT_ACTION = "test-action";

    std::unordered_set<TObjectId> objectIds;
    auto txn = pool().masterWriteableTransaction();

    // Fill DB
    {
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
        for (size_t i = 0; i < OBJECTS_NUM; ++i) {
            cmtData.push_back(createData(rg, IN_POINT_WKB, {{"cat:jc", "1"}}));
            objectIds.insert(cmtData.back().first.objectId());
        }
        rg.createCommit(std::move(cmtData), TEST_UID,
            {{"description", "data"}});
    }

    Session session(*txn, revision::TRUNK_BRANCH_ID);
    auto commitIds =
        session.query(rf::True()).update(
            COMMIT_ACTION, TEST_UID,
            [](Object& obj) {
                obj.setAttribute("test", "1");
            });

    // Check result
    {
        BOOST_REQUIRE(!commitIds.empty());

        auto reader = revGateway(*txn, revision::TRUNK_BRANCH_ID).reader();

        for (TCommitId commitId : commitIds) {
            auto commit = revision::Commit::load(*txn, commitId);

            BOOST_CHECK_EQUAL(commit.createdBy(), TEST_UID);
            const auto& commitAttrs = commit.attributes();
            auto actionIt = commitAttrs.find("action");
            BOOST_CHECK(actionIt != std::end(commitAttrs)
                        && actionIt->second == COMMIT_ACTION);

            auto commitRevs = reader.commitRevisions(commitId);
            for (const auto& objRev : commitRevs) {
                BOOST_CHECK(objectIds.erase(objRev.id().objectId()));

                const auto& attributes = objRev.data().attributes;
                BOOST_REQUIRE(attributes);
                BOOST_CHECK(attributes->count("test"));
            }
        }
    }

    BOOST_CHECK(objectIds.empty());
}

BOOST_FIXTURE_TEST_CASE( test_actions_throw, unittest::ArcadiaDbFixture )
{
    TObjectId objectId = 0;
    TCommitId initCommitId = 0;

    // Init DB
    {
        auto txn = pool().masterWriteableTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
        cmtData.push_back(createData(rg, IN_POINT_WKB, {{"cat:jc", "1"}}));
        objectId = cmtData.back().first.objectId();

        rg.createCommit(std::move(cmtData), TEST_UID,
            {{"description", "init"}});

        initCommitId = rg.headCommitId();

        txn->commit();
    }

    auto currentCommitId = [&]
    {
        auto txn = pool().slaveTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);
        auto rev = rg.snapshot(rg.headCommitId()).objectRevision(objectId);
        BOOST_REQUIRE(rev);
        return rev->id().commitId();
    };

    {
        auto txn = pool().masterWriteableTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);
        BOOST_CHECK_THROW(
            session.query(rf::True()).visit(
                [](const Object&) { throw maps::LogicError(); }),
            maps::LogicError);

        BOOST_CHECK_THROW(
            session.query(rf::True()).update(
                "test-action", TEST_UID,
                [](Object&) { throw maps::LogicError(); }),
            maps::LogicError);

        BOOST_REQUIRE_EQUAL(currentCommitId(), initCommitId);
    }
}

BOOST_FIXTURE_TEST_CASE( test_actions_delete, unittest::ArcadiaDbFixture )
{
    TObjectId objId = 0;
    auto txn = pool().masterWriteableTransaction();

    // Init DB
    {
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;
        cmtData.push_back(createData(rg, IN_POINT_WKB, {{"cat:jc", "1"}}));
        objId = cmtData.back().first.objectId();
        rg.createCommit(std::move(cmtData), TEST_UID,
            {{"description", "init"}});
    }

    Session session(*txn, revision::TRUNK_BRANCH_ID);
    auto commitIds =
        session.query(rf::True()).update(
            "delete-action", TEST_UID,
            [](Object& obj) { obj.setDeleted(); });

    // Check result
    {
        BOOST_REQUIRE_EQUAL(commitIds.size(), 1);

        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        auto objRev = rg.snapshot(commitIds.front()).objectRevision(objId);
        BOOST_REQUIRE(objRev);
        BOOST_CHECK(objRev->data().deleted);
    }
}

BOOST_FIXTURE_TEST_CASE( test_query_by_ids, unittest::ArcadiaDbFixture )
{
    TObjectId outPointId = 0;
    TObjectId polygonId = 0;

    // Fill DB
    {
        auto txn = pool().masterWriteableTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;

        cmtData.push_back(createData(rg, IN_POINT_WKB, {{"cat:jc", "1"}}));
        cmtData.push_back(createData(rg, IN_POLYLINE_WKB, {{"cat:el", "1"}}));
        cmtData.push_back(createData(rg, IN_POLYGON_WKB, {{"cat:poly", "1"}}));
        polygonId = cmtData.back().first.objectId();
        cmtData.push_back(createData(rg, OUT_POINT_WKB, {{"cat:jc", "1"}}));
        outPointId = cmtData.back().first.objectId();

        rg.createCommit(std::move(cmtData), TEST_UID,
            {{"description", "data"}});

        txn->commit();
    }

    {
        auto txn = pool().slaveTransaction();
        std::unordered_set<TObjectId> seenObjectIds;

        Session session(*txn, revision::TRUNK_BRANCH_ID);
        BOOST_CHECK_NO_THROW(
            session.query(std::vector<TObjectId>{}).visit(
                [&](const Object& obj) {
                    seenObjectIds.insert(obj.id());
                }));
        BOOST_CHECK_EQUAL(seenObjectIds.size(), 0);

        session
            .query(
                rf::ObjRevAttr::objectId() != polygonId,
                GeomPredicate::Within,
                AOI_WKB)
            .visit(
                [&](const Object& obj) {
                    seenObjectIds.insert(obj.id());
                });

        BOOST_REQUIRE_EQUAL(seenObjectIds.size(), 2);
        BOOST_CHECK(!seenObjectIds.count(outPointId));
        BOOST_CHECK(!seenObjectIds.count(polygonId));

        session.query(std::vector<TObjectId>{outPointId, polygonId}).visit(
            [&](const Object& obj) {
                seenObjectIds.insert(obj.id());
            });

        BOOST_REQUIRE_EQUAL(seenObjectIds.size(), 4);
        BOOST_CHECK(seenObjectIds.count(outPointId));
        BOOST_CHECK(seenObjectIds.count(polygonId));
    }
}

BOOST_FIXTURE_TEST_CASE( test_add_relations, unittest::ArcadiaDbFixture )
{
    TObjectId jcId = 0;
    TObjectId elId = 0;

    // Fill DB
    {
        auto txn = pool().masterWriteableTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;

        cmtData.push_back(createData(rg, IN_POINT_WKB, {{"cat:jc", "1"}}));
        jcId = cmtData.back().first.objectId();
        cmtData.push_back(createData(rg, IN_POLYLINE_WKB, {{"cat:el", "1"}}));
        elId = cmtData.back().first.objectId();

        rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});

        txn->commit();
    }

    // Add relations and perform object edits
    {
        auto txn = pool().masterWriteableTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);
        session.query(rf::True()).update(
            "add-relations", TEST_UID,
            [elId, jcId](Object& obj) {
                if (obj.category() == "jc") {
                        obj.addRelationToMaster(elId, "el", "role1");

                        // add relation and delete it right away
                        obj.addRelationToMaster(elId, "el", "role2");
                        obj.relations().begin()->setDeleted();
                    } else if (obj.category() == "el") {
                        obj.setAttribute("el:fc", "1");
                        obj.addRelationToSlave(jcId, "jc", "role");
                    };
                });
        txn->commit();
    }

    // Check
    {
        auto txn = pool().slaveTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);
        session.query(std::vector<TObjectId>{elId, jcId}).visit(
            [elId](const Object& obj) {
                size_t totalRels = 0;
                size_t relsToSlave = 0;
                for (const auto& rel : obj.relations()) {
                    ++totalRels;
                    if (rel.type() == Relation::Type::Slave) {
                        ++relsToSlave;
                    }
                }

                BOOST_CHECK_EQUAL(totalRels, 2);
                if (obj.id() == elId) {
                    BOOST_CHECK_EQUAL(relsToSlave, totalRels);
                } else {
                    BOOST_CHECK_EQUAL(relsToSlave, 0);
                }
            });
    }
}

BOOST_FIXTURE_TEST_CASE( test_change_category, unittest::ArcadiaDbFixture )
{
    TObjectId objectId = 0;
    TObjectId masterId = 0;
    TObjectId slaveId = 0;

    // Fill DB
    {
        auto txn = pool().masterWriteableTransaction();
        auto rg = revGateway(*txn, revision::TRUNK_BRANCH_ID);

        std::list<revision::RevisionsGateway::NewRevisionData> cmtData;

        cmtData.push_back(createData(rg, IN_POINT_WKB,
            {{"cat:jc", "1"}, {"jc:test", "xxx"}}));
        objectId = cmtData.front().first.objectId();

        cmtData.push_back(createData(rg, IN_POLYLINE_WKB,
            {{"cat:el", "1"}}));
        masterId = cmtData.back().first.objectId();

        cmtData.push_back(createRelData(rg,
            masterId, objectId,
            {{"rel:master", "el"}, {"rel:slave", "jc"},
                {"rel:role", "start"}}));

        cmtData.push_back(createData(rg, IN_POINT_WKB,
            {{"cat:jc_jc", "1"}}));
        slaveId = cmtData.back().first.objectId();

        cmtData.push_back(createRelData(rg,
            objectId, slaveId,
            {{"rel:master", "jc"}, {"rel:slave", "jc_jc"},
                {"rel:role", "meh"}}));

        rg.createCommit(std::move(cmtData), TEST_UID, {{"description", "data"}});

        txn->commit();
    }

    // Change category
    {
        auto txn = pool().masterWriteableTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        auto commitIds = session.query({objectId}).update(
            "test-action",
            TEST_UID,
            [](Object& obj) {
                obj.changeCategory("poi");
            });

        BOOST_REQUIRE(!commitIds.empty());
        txn->commit();
    }

    // Check relations
    {
        auto txn = pool().slaveTransaction();
        Session session(*txn, revision::TRUNK_BRANCH_ID);

        session.query({objectId}).visit(
            [&](const Object& obj) {
                BOOST_CHECK_EQUAL(obj.category(), "poi");
                BOOST_CHECK(obj.attributes()
                    == revision::Attributes({{"cat:poi", "1"}, {"poi:test", "xxx"}}));

                size_t count = 0;
                for (const auto& relation : obj.relations()) {
                    BOOST_CHECK(!relation.role().empty()); //Fake check to calm down the compiler
                    count++;
                }
                BOOST_CHECK_EQUAL(count, 2);
            });

        session.query({masterId, slaveId}).visit(
            [&](const Object& obj) {
                size_t count = 0;
                for (const auto& relation : obj.relations()) {
                    BOOST_CHECK_EQUAL(relation.otherCategory(), "poi");
                    count++;
                }
                BOOST_CHECK_EQUAL(count, 1);
            });
    }
}

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps
