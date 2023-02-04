#include <yandex/maps/wiki/pubsub/commit_consumer.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::wiki::pubsub::tests {

namespace {

const revision::UserID TEST_UID = 111;
const std::string TEST_CONSUMER_ID = "test";

revision::DBID createCommit(pqxx::transaction_base& txn)
{
    revision::RevisionsGateway rg(txn);
    auto oid = rg.acquireObjectId();
    std::list<revision::RevisionsGateway::NewRevisionData>
        newData{{oid, revision::ObjectData()}};
    return rg.createCommit(newData, TEST_UID, {{"action", "test"}}).id();
}

} // namespace

Y_UNIT_TEST_SUITE_F(pubsub, unittest::ArcadiaDbFixture) {

Y_UNIT_TEST(test_empty_database)
{
    auto revisionConn = pool().getMasterConnection();
    auto consumerConn = pool().getMasterConnection();

    pqxx::work consumerTxn(consumerConn.get());
    CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);
    EXPECT_TRUE(consumer.isFirstRun());

    pqxx::work revisionTxn(revisionConn.get());
    EXPECT_EQ(consumer.consumeBatch(revisionTxn).size(), 0ul);
}

Y_UNIT_TEST(test_commit)
{
    auto revisionConn = pool().getMasterConnection();
    auto consumerConn = pool().getMasterConnection();

    revision::DBID commitId;
    {
        pqxx::work revisionTxn(revisionConn.get());
        commitId = createCommit(revisionTxn);
        revisionTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);
        EXPECT_TRUE(consumer.isFirstRun());

        pqxx::work revisionTxn(revisionConn.get());
        auto batch = consumer.consumeBatch(revisionTxn);
        EXPECT_EQ(batch.size(), 1ul);
        EXPECT_EQ(batch[0], commitId);
        consumerTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);

        pqxx::work revisionTxn(revisionConn.get());
        EXPECT_EQ(consumer.consumeBatch(revisionTxn).size(), 0ul);
    }
}

Y_UNIT_TEST(test_fast_forward)
{
    auto revisionConn = pool().getMasterConnection();
    auto consumerConn = pool().getMasterConnection();

    {
        pqxx::work revisionTxn(revisionConn.get());
        createCommit(revisionTxn);
        revisionTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);
        EXPECT_TRUE(consumer.isFirstRun());

        consumer.setOutOfOrderDisabled(true).fastForwardToLastCommit();
        {
            pqxx::work revisionTxn(revisionConn.get());
            auto batch = consumer.consumeBatch(revisionTxn);
            EXPECT_EQ(batch.size(), 0ul);
            consumerTxn.commit();
        }
    }
}

Y_UNIT_TEST(test_fast_forward_out_of_order)
{
    auto revisionConn = pool().getMasterConnection();
    auto consumerConn = pool().getMasterConnection();

    {
        pqxx::work revisionTxn(revisionConn.get());
        createCommit(revisionTxn);
        revisionTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);
        EXPECT_TRUE(consumer.isFirstRun());
        consumer.fastForwardToLastCommit();

        {
            pqxx::work revisionTxn(revisionConn.get());
            createCommit(revisionTxn);
            auto batch = consumer.consumeBatch(revisionTxn);
            revisionTxn.commit();
            EXPECT_EQ(batch.size(), 1ul);
            consumerTxn.commit();
        }
    }
}

Y_UNIT_TEST(test_out_of_order)
{
    auto revisionConn = pool().getMasterConnection();
    auto revisionConn2 = pool().getMasterConnection();
    auto consumerConn = pool().getMasterConnection();

    pqxx::work longTxn(revisionConn2.get());
    longTxn.exec("SELECT txid_current()"); // get real txid for this txn

    revision::DBID commitId;
    {
        pqxx::work revisionTxn(revisionConn.get());
        commitId = createCommit(revisionTxn);
        revisionTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);

        pqxx::work revisionTxn(revisionConn.get());
        EXPECT_EQ(
                consumer.setOutOfOrderDisabled(true).consumeBatch(revisionTxn).size(), 0ul);

        auto batch = consumer.setOutOfOrderDisabled(false).consumeBatch(revisionTxn);
        EXPECT_EQ(batch.size(), 1ul);
        EXPECT_EQ(batch[0], commitId);
        consumerTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);

        pqxx::work revisionTxn(revisionConn.get());
        EXPECT_EQ(
                consumer.setOutOfOrderDisabled(true).consumeBatch(revisionTxn).size(), 0ul);
        EXPECT_EQ(
                consumer.setOutOfOrderDisabled(false).consumeBatch(revisionTxn).size(), 0ul);
    }
}

Y_UNIT_TEST(test_locking)
{
    auto consumerConn = pool().getMasterConnection();
    auto consumerConn2 = pool().getMasterConnection();

    {
        pqxx::work consumerTxn(consumerConn.get());
        EXPECT_NO_THROW(CommitConsumer(consumerTxn, TEST_CONSUMER_ID, 0));
        EXPECT_NO_THROW(CommitConsumer(consumerTxn, TEST_CONSUMER_ID, 1));
        consumerTxn.commit();
    }

    pqxx::work consumerTxn(consumerConn.get());
    pqxx::work consumerTxn2(consumerConn2.get());
    EXPECT_NO_THROW(CommitConsumer(consumerTxn, TEST_CONSUMER_ID, 0));
    EXPECT_NO_THROW(CommitConsumer(consumerTxn, TEST_CONSUMER_ID, 1));

    EXPECT_THROW(
            CommitConsumer(consumerTxn2, TEST_CONSUMER_ID, 0),
            AlreadyLockedException);
    EXPECT_THROW(
            CommitConsumer(consumerTxn2, TEST_CONSUMER_ID, 1),
            AlreadyLockedException);
}

Y_UNIT_TEST(test_count_commits_in_queue)
{
    auto revisionConn = pool().getMasterConnection();
    auto consumerConn = pool().getMasterConnection();

    {
        // Create OOO consumed commit
        pqxx::work revisionTxn(revisionConn.get());
        createCommit(revisionTxn);
        revisionTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        CommitConsumer consumer(consumerTxn, TEST_CONSUMER_ID, 0);
        pqxx::work revisionTxn(revisionConn.get());
        // Consume OOO commit
        EXPECT_EQ(consumer.consumeBatch(revisionTxn).size(), 1ul);
        // Create next commit in the queue
        consumerTxn.commit();
        createCommit(revisionTxn);
        revisionTxn.commit();
    }

    {
        pqxx::work consumerTxn(consumerConn.get());
        pqxx::work revisionTxn(revisionConn.get());
        EXPECT_EQ(
            countCommitsInQueue(consumerTxn, revisionTxn, TEST_CONSUMER_ID, 0), 1ul);
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::pubsub::tests
