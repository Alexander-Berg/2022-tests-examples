#include <yandex/maps/wiki/unittest/arcadia.h>

#include <yandex/maps/wiki/geolocks/geolocks.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/branch_manager.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/serialization.h>

#include <boost/test/unit_test.hpp>

#include <sstream>
#include <thread>
#include <atomic>

namespace maps {
namespace wiki {
namespace geolocks {
namespace tests {

namespace gl = geolib3;

namespace {

const TUId TEST_UID = 111;

using revision::TRUNK_BRANCH_ID;

class DbFixture : public unittest::ArcadiaDbFixture
{
public:
    DbFixture()
        : connHandle(pool().getMasterConnection())
        , conn(connHandle.get())
    {
        pqxx::work txn(conn);

        STABLE_BRANCH_ID =
            revision::BranchManager(txn).createStable(TEST_UID, {}).id();

        txn.commit();
    }

private:
    pgpool3::ConnectionHandle connHandle;

public:
    pqxx::connection& conn;

    revision::DBID STABLE_BRANCH_ID;
};

template<typename TGeom>
std::string wkt2wkb(const std::string& wkt)
{
    std::ostringstream ss;
    gl::WKB::write(gl::WKT::read<TGeom>(wkt), ss);
    return ss.str();
}

const std::string LOCKED_AREA_WKB = wkt2wkb<gl::Polygon2>(
        "POLYGON((0 5, 5 10, 10 5, 5 0, 0 5))");
const gl::BoundingBox BBOX_INSIDE(gl::Point2(5, 5), 0.1, 0.1);
const gl::BoundingBox BBOX_OUTSIDE(gl::Point2(2, 2), 0.1, 0.1);
const gl::Point2 POINT_INSIDE(5, 5);
const gl::Point2 POINT_OUTSIDE(10, 10);

} // namespace

BOOST_FIXTURE_TEST_CASE( test_lock_unlock, DbFixture )
{
    boost::optional<GeoLock> trunkLock;

    {
        trunkLock = GeoLock::tryLock(
                conn, TEST_UID, TRUNK_BRANCH_ID, LOCKED_AREA_WKB);
        BOOST_REQUIRE_MESSAGE(trunkLock, "could not lock trunk");

        pqxx::work txn(conn);

        BOOST_CHECK_MESSAGE(
                !isLocked(txn, TRUNK_BRANCH_ID, BBOX_OUTSIDE),
                "trunk appears locked outside locked extent");
        BOOST_CHECK_MESSAGE(
                !isLocked(txn, TRUNK_BRANCH_ID, POINT_OUTSIDE),
                "trunk appears locked outside locked extent");

        BOOST_CHECK_MESSAGE(
                !isLocked(txn, STABLE_BRANCH_ID, BBOX_INSIDE),
                "stable branch locked");
        BOOST_CHECK_MESSAGE(
                !isLocked(txn, STABLE_BRANCH_ID, POINT_INSIDE),
                "stable branch locked");

        BOOST_CHECK_MESSAGE(
                isLocked(txn, TRUNK_BRANCH_ID, BBOX_INSIDE),
                "trunk not locked inside locked extent");
        BOOST_CHECK_MESSAGE(
                isLocked(txn, TRUNK_BRANCH_ID, POINT_INSIDE),
                "trunk not locked inside locked extent");
        BOOST_CHECK_MESSAGE(
                isLocked(txn, TRUNK_BRANCH_ID, POINT_INSIDE, GeolockType::Manual),
                "trunk not locked inside locked extent");
    }

    {
        BOOST_CHECK_MESSAGE(
                !GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID, LOCKED_AREA_WKB),
                "locked same extent in trunk twice");
        BOOST_CHECK_MESSAGE(
                GeoLock::tryLock(conn, TEST_UID, STABLE_BRANCH_ID, LOCKED_AREA_WKB),
                "could not lock stable");
    }

    {
        pqxx::work txn(conn);

        BOOST_CHECK_EQUAL(
                GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, BBOX_OUTSIDE).size(), 0);
        BOOST_CHECK_EQUAL(
                GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, POINT_OUTSIDE).size(), 0);

        auto loadedByBbox = GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, BBOX_INSIDE);
        BOOST_REQUIRE_EQUAL(loadedByBbox.size(), 1);
        auto loadedByPoint = GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, POINT_INSIDE);
        BOOST_REQUIRE_EQUAL(loadedByPoint.size(), 1);
        auto loadedByPointManual = GeoLock::loadByGeom(
            txn, TRUNK_BRANCH_ID, POINT_INSIDE, GeolockType::Manual);
        BOOST_REQUIRE_EQUAL(loadedByPointManual.size(), 1);

        auto loaded = GeoLock::load(txn, trunkLock->id());

        BOOST_CHECK(trunkLock->extentWkb() == loaded.extentWkb());
        BOOST_CHECK(trunkLock->extentWkb() == loadedByBbox.front().extentWkb());
        BOOST_CHECK(trunkLock->extentWkb() == loadedByPoint.front().extentWkb());
    }

    {
        pqxx::work txn(conn);
        trunkLock->unlock(txn);
        BOOST_CHECK_THROW(trunkLock->unlock(txn), NotFoundException);
        BOOST_CHECK_EQUAL(
                GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, BBOX_INSIDE).size(), 0);
        txn.commit();

        BOOST_CHECK_MESSAGE(
                GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID, LOCKED_AREA_WKB),
                "could not lock trunk after original lock was unlocked");
    }
}

BOOST_FIXTURE_TEST_CASE( test_concurrent_lock, DbFixture )
{
    std::atomic<int> locked(0);
    std::atomic<bool> started(false);

    auto locker = [&]()
    {
        auto handle = pool().getMasterConnection();
        auto& conn = handle.get();

        while (!started) {
            // busy-wait
        }

        if (GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID, LOCKED_AREA_WKB)) {
            locked += 1;
        }
    };

    std::vector<std::thread> lockers;
    for (size_t i = 0; i < 3; ++i) {
        lockers.emplace_back(locker);

    }

    started = true;

    for (auto& locker : lockers) {
        locker.join();
    }

    BOOST_CHECK_EQUAL(locked, 1);
}

BOOST_FIXTURE_TEST_CASE( test_concurrent_read, DbFixture )
{
    std::atomic<int> readUnlockedTotal(0);
    int readUnlockedAfterLock = -1;
    boost::optional<GeoLock> lock;

    auto reader = [&]()
    {
        auto handle = pool().getMasterConnection();
        auto& conn = handle.get();

        pqxx::work txn(conn);
        sleep(1);
        if (!isLocked(txn, TRUNK_BRANCH_ID, BBOX_INSIDE)) {
            readUnlockedTotal += 1;
        }
        sleep(1);
    };

    auto locker = [&]()
    {
        lock = GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID, LOCKED_AREA_WKB);
        readUnlockedAfterLock = readUnlockedTotal;
    };

    std::vector<std::thread> readers;
    for (size_t i = 0; i < 5; ++i) {
        readers.emplace_back(reader);
    }
    sleep(1);
    std::thread lockerThread(locker);

    for (auto& reader : readers) {
        reader.join();
    }
    lockerThread.join();

    BOOST_CHECK_EQUAL(readUnlockedAfterLock, readUnlockedTotal);
    BOOST_CHECK_MESSAGE(lock, "did not lock");
}

BOOST_FIXTURE_TEST_CASE( test_nonpolygonal_lock, DbFixture )
{
    BOOST_CHECK_NO_THROW(
        GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID, LOCKED_AREA_WKB));
    BOOST_CHECK_THROW(
        GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID,
            wkt2wkb<gl::Point2>("POINT (100 100)")),
        maps::RuntimeError);
    BOOST_CHECK_THROW(
        GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID,
            wkt2wkb<gl::Polyline2>("LINESTRING (25 25, 25 50)")),
        maps::RuntimeError);
}

BOOST_FIXTURE_TEST_CASE( test_multilock, DbFixture )
{
    const gl::BoundingBox BIG_BBOX(gl::Point2(50, 50), 100, 100);
    auto lock =
        GeoLock::tryLock(conn, TEST_UID, TRUNK_BRANCH_ID, LOCKED_AREA_WKB);
    BOOST_REQUIRE(lock);

    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(
            GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, BIG_BBOX).size(), 1);
    }

    std::vector<std::string> extents{
        wkt2wkb<gl::Polygon2>("POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))"),
        wkt2wkb<gl::Polygon2>("POLYGON ((100 100, 150 100, 100 150, 100 100))"),
        wkt2wkb<gl::Polygon2>("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))")};

    {
        BOOST_CHECK(
            !GeoLock::tryLockAll(conn, TEST_UID, TRUNK_BRANCH_ID, extents));
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(
            GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, BIG_BBOX).size(), 1);
    }

    {
        auto exts = extents;
        exts.pop_back();

        BOOST_CHECK(
            !GeoLock::tryLockAll(conn, TEST_UID, TRUNK_BRANCH_ID, exts));
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(
            GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, BIG_BBOX).size(), 1);
    }

    {
        auto exts = extents;
        std::swap(exts.front(), exts.back());
        exts.pop_back();

        auto locks =
            GeoLock::tryLockAll(conn, TEST_UID, TRUNK_BRANCH_ID, exts);
        BOOST_REQUIRE(locks);
        BOOST_CHECK_EQUAL(locks->size(), 2);
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(
            GeoLock::loadByGeom(txn, TRUNK_BRANCH_ID, BIG_BBOX).size(), 3);
    }
}

} // namespace tests
} // namespace geolocks
} // namespace wiki
} // namespace maps
