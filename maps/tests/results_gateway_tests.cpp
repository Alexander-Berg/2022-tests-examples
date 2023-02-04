#include "helpers.h"

#include <yandex/maps/wiki/validator/storage/results_gateway.h>
#include <yandex/maps/wiki/validator/storage/message_attributes_filter.h>

#include <library/cpp/testing/unittest/registar.h>

namespace rev = maps::wiki::revision;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

Y_UNIT_TEST_SUITE_F(results_gateway, DbFixture) {

Y_UNIT_TEST(test_results_gateway)
{
    static const int TEST_TASK_ID = 312;
    RevisionID id1, id2;
    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        id1 = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        id2 = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        revisionTxn->commit();
    }

    auto revisionTxn = revisionPgPool()->slaveTransaction();
    auto snapshot = headCommitSnapshot(*revisionTxn);

    storage::storeResult(
        validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            id2.commitId(),
            aoi,
            "./test_results_gateway"
        ),
        *validationPgPool(),
        TEST_TASK_ID
    );

    auto validationTxn = validationPgPool()->slaveTransaction();
    storage::ResultsGateway gateway(*validationTxn, TEST_TASK_ID);

    UNIT_ASSERT_VALUES_EQUAL(gateway.messageCount(), 2);

    auto stats = gateway.statistics({});
    UNIT_ASSERT_VALUES_EQUAL(stats.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(stats.begin()->second, 2);

    {
        Messages messages;
        for (const auto& datum : gateway.messages({}, snapshot, 0, 100, TEST_UID)) {
            messages.push_back(datum.message());
        }

        checkMessages(
            messages, {
                {"stub-description", RevisionIds{id1}},
                {"stub-description", RevisionIds{id2}}
            }
        );
    }

    {
        storage::MessageAttributesFilter filter;
        filter.checkId = "report_all";

        Messages messages;
        for (const auto& datum : gateway.messages(filter, snapshot, 0, 100, TEST_UID)) {
            messages.push_back(datum.message());
        }
        checkMessages(
            messages, {
                {"stub-description", RevisionIds{id1}},
                {"stub-description", RevisionIds{id2}}
            }
        );
    }
}

Y_UNIT_TEST(test_messages_without_oids)
{
    static const int TEST_TASK_ID = 313;
    RevisionID id;
    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        id = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );

        revisionTxn->commit();
    }

    storage::storeResult(
        validator.run(
            {"without_oids"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            id.commitId(),
            aoi,
            "./test_messages_without_oids"
        ),
        *validationPgPool(),
        TEST_TASK_ID
    );

    auto validationTxn = validationPgPool()->slaveTransaction();
    storage::ResultsGateway gateway(*validationTxn, TEST_TASK_ID);

    UNIT_ASSERT_VALUES_EQUAL(gateway.messageCount(), 1);
}

Y_UNIT_TEST(test_message_activity)
{
    static const int TEST_TASK_ID = 314;
    RevisionID id1, id2;
    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);

        id1 = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );
        id2 = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(0 0)").objectId()
        );

        const auto oldRevision = rg.snapshot(rg.headCommitId()).objectRevision(id1.objectId());

        UNIT_ASSERT(oldRevision);

        std::list<rev::RevisionsGateway::NewRevisionData> cmtData;
        rev::RevisionsGateway::NewRevisionData objData{
            oldRevision->id(),
            oldRevision->data()};
        (*objData.second.attributes)["rd_el:dr"] = "1";
        cmtData.push_back(objData);

        rg.createCommit(cmtData, TEST_UID, {{"description", "meh"}});
        revisionTxn->commit();
    }

    auto revisionTxn = revisionPgPool()->slaveTransaction();
    auto snapshot = headCommitSnapshot(*revisionTxn);

    storage::storeResult(
        validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            id2.commitId(),
            aoi,
            "./test_message_activity"
        ),
        *validationPgPool(),
        TEST_TASK_ID
    );

    auto validationTxn = validationPgPool()->slaveTransaction();
    storage::ResultsGateway gateway(*validationTxn, TEST_TASK_ID);

    auto messages = gateway.messages({}, snapshot, 0, 100, TEST_UID);
    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 2);
    for (const auto& message: messages) {
        const RevisionID& id = message.message().revisionIds().front();
        const bool notHeadRevisionId  = (id.objectId() == id1.objectId());
        UNIT_ASSERT_EQUAL(notHeadRevisionId, !message.isActive());
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
