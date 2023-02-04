#include "common.h"
#include <maps/wikimap/mapspro/libs/revision/sql_strings.h>

#include <yandex/maps/wiki/revision/branch.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/exception.h>

#include <boost/test/unit_test.hpp>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

namespace {

const Branch::LockId LOCK_ID_1 = 0;

const Branch::LockId INVALID_LOCK_ID = Branch::MAX_LOCK_ID + 1;

struct BranchLockTestDatabaseCreator: public DbFixture
{
    static DBID STABLE_BRANCH_ID;

    BranchLockTestDatabaseCreator()
    {
        pqxx::work txn(getConnection());
        STABLE_BRANCH_ID = BranchManager(txn).createStable(131, {}).id();
        txn.commit();
    }
};

DBID BranchLockTestDatabaseCreator::STABLE_BRANCH_ID;

} // namespace

BOOST_FIXTURE_TEST_SUITE(BranchLockTest, BranchLockTestDatabaseCreator)

BOOST_AUTO_TEST_CASE(test_invalid_lock_id)
{
    pqxx::connection conn(connectionString());
    pqxx::work txn(conn);

    Branch trunk = BranchManager(txn).load(TRUNK_BRANCH_ID);
    BOOST_CHECK_THROW(trunk.lock(txn, INVALID_LOCK_ID, Branch::LockMode::Wait),
        BranchLockIdOutOfRange);
    BOOST_CHECK_THROW(trunk.lock(txn, INVALID_LOCK_ID, Branch::LockMode::Nowait),
        BranchLockIdOutOfRange);
    BOOST_CHECK_THROW(trunk.tryLock(txn, INVALID_LOCK_ID),
        BranchLockIdOutOfRange);
}

BOOST_AUTO_TEST_CASE(test_unique_lock_one_branch)
{
    pqxx::connection conn(connectionString());
    pqxx::work txn(conn);

    Branch trunk = BranchManager(txn).load(TRUNK_BRANCH_ID);
    BOOST_CHECK_NO_THROW(trunk.lock(txn, LOCK_ID_1, Branch::LockMode::Nowait));

    pqxx::connection otherConn(connectionString());
    pqxx::work otherTxn(otherConn);
    Branch trunkInOtherTxn = BranchManager(otherTxn).load(TRUNK_BRANCH_ID);
    BOOST_CHECK_EQUAL(trunkInOtherTxn.tryLock(otherTxn, LOCK_ID_1), false);
    BOOST_CHECK_THROW(trunkInOtherTxn.lock(otherTxn, LOCK_ID_1, Branch::LockMode::Nowait),
        BranchAlreadyLockedException);
}

BOOST_AUTO_TEST_CASE(test_lock_different_branches)
{
    pqxx::connection conn(connectionString());
    pqxx::work txn(conn);

    Branch trunk = BranchManager(txn).load(TRUNK_BRANCH_ID);
    BOOST_CHECK_NO_THROW(trunk.lock(txn, LOCK_ID_1, Branch::LockMode::Nowait));

    {
        pqxx::connection otherConn(connectionString());
        pqxx::work otherTxn(otherConn);
        Branch stableInOtherTxn = BranchManager(otherTxn).load(
            BranchLockTestDatabaseCreator::STABLE_BRANCH_ID
        );
        BOOST_CHECK_EQUAL(stableInOtherTxn.tryLock(otherTxn, LOCK_ID_1), true);
    }
    {
        pqxx::connection otherConn(connectionString());
        pqxx::work otherTxn(otherConn);
        Branch stableInOtherTxn = BranchManager(otherTxn).load(
            BranchLockTestDatabaseCreator::STABLE_BRANCH_ID
        );
        BOOST_CHECK_NO_THROW(stableInOtherTxn.lock(otherTxn, LOCK_ID_1, Branch::LockMode::Nowait));
    }
}

BOOST_AUTO_TEST_CASE(test_shared_lock)
{
    pqxx::connection conn(connectionString());
    pqxx::work txn(conn);

    Branch trunk = BranchManager(txn).load(TRUNK_BRANCH_ID);
    trunk.lock(txn, LOCK_ID_1, Branch::LockMode::Nowait, Branch::LockType::Shared);

    {
        pqxx::connection otherConn(connectionString());
        pqxx::work otherTxn(otherConn);

        BOOST_CHECK(!trunk.tryLock(otherTxn, LOCK_ID_1));
        BOOST_CHECK_THROW(
                trunk.lock(otherTxn, LOCK_ID_1, Branch::LockMode::Nowait),
                BranchAlreadyLockedException);

        BOOST_CHECK(trunk.tryLock(otherTxn, LOCK_ID_1, Branch::LockType::Shared));
        BOOST_CHECK_NO_THROW(
                trunk.lock(
                        otherTxn, LOCK_ID_1,
                        Branch::LockMode::Nowait, Branch::LockType::Shared));
    }
}

BOOST_AUTO_TEST_CASE(test_exclusive_lock)
{
    pqxx::connection conn(connectionString());
    pqxx::work txn(conn);

    Branch trunk = BranchManager(txn).load(TRUNK_BRANCH_ID);
    trunk.lock(txn, LOCK_ID_1, Branch::LockMode::Nowait, Branch::LockType::Exclusive);

    {
        pqxx::connection otherConn(connectionString());
        pqxx::work otherTxn(otherConn);

        BOOST_CHECK(!trunk.tryLock(otherTxn, LOCK_ID_1));
        BOOST_CHECK_THROW(
                trunk.lock(otherTxn, LOCK_ID_1, Branch::LockMode::Nowait),
                BranchAlreadyLockedException);

        BOOST_CHECK(!trunk.tryLock(otherTxn, LOCK_ID_1, Branch::LockType::Shared));
        BOOST_CHECK_THROW(
                trunk.lock(
                        otherTxn, LOCK_ID_1,
                        Branch::LockMode::Nowait, Branch::LockType::Shared),
                BranchAlreadyLockedException);
    }
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
