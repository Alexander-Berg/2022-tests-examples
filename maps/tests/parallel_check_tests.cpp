#include "helpers.h"

#include <yandex/maps/wiki/validator/validator.h>

#include <maps/libs/pgpool/include/pgpool3.h>

#include <library/cpp/testing/unittest/registar.h>

#include <map>
#include <set>
#include <string>

namespace rev = maps::wiki::revision;

namespace maps::wiki::validator::tests {

Y_UNIT_TEST_SUITE_F(parallel_checks, DbFixture) {

Y_UNIT_TEST(test_multipart)
{
    auto messages = validator.run({"multipart"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        startCommitId,
        aoi,
        "./multipart"
    )->drainAllMessages();

    checkMessages(messages, { {"part1", RevisionIds{}}, {"part2", RevisionIds{}} });
    for (const auto& message : messages) {
        UNIT_ASSERT_VALUES_EQUAL(message.attributes().checkId, "multipart");
    }
}

Y_UNIT_TEST(test_deduplication)
{
    auto messages = validator.run(
        {"deduplication_multipart", "deduplication_simple"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        startCommitId,
        aoi,
        "./multipart"
    )->drainAllMessages();

    checkMessages(messages, { {"check1_part2", RevisionIds{}}, {"check2", RevisionIds{}} });
}

Y_UNIT_TEST(test_batch_visit)
{
    RevisionID lastRoadElementId;
    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        createRoadElement(
            rg, "LINESTRING(2 2, 3 3)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        createRoadElement(
            rg, "LINESTRING(4 4, 5 5)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        createRoadElement(
            rg, "LINESTRING(6 6, 7 7)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        lastRoadElementId = createRoadElement(
            rg, "LINESTRING(8 8, 9 9)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        revisionTxn->commit();
    }

    std::set<RevisionID> revSet, batchRevSet;

    {
        const Messages messages = validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            lastRoadElementId.commitId(),
            aoi,
            "./test_batch_visit.1"
        )->drainAllMessages();

        for (const auto& message: messages) {
            UNIT_ASSERT_VALUES_EQUAL(message.revisionIds().size(), 1);
            revSet.insert(message.revisionIds().front());
        }
        UNIT_ASSERT_VALUES_EQUAL(revSet.size(), 5);
    }

    {
        const Messages messages = validator.run(
            {"report_all_batch"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            lastRoadElementId.commitId(),
            aoi,
            "./test_batch_visit.2"
        )->drainAllMessages();

        for (const auto& message: messages) {
            UNIT_ASSERT_VALUES_EQUAL(message.attributes().checkId, "report_all_batch");
            UNIT_ASSERT_VALUES_EQUAL(message.revisionIds().size(), 1);
            batchRevSet.insert(message.revisionIds().front());

        }
        UNIT_ASSERT_VALUES_EQUAL(batchRevSet.size(), 5);
    }

    UNIT_ASSERT_VALUES_EQUAL(batchRevSet.size(), 5);
    UNIT_ASSERT_EQUAL(revSet, batchRevSet);
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::wiki::validator::tests
