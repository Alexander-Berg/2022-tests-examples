#include "helpers.h"

#include <yandex/maps/wiki/validator/storage/results_gateway.h>
#include <yandex/maps/wiki/validator/storage/exclusions_gateway.h>
#include <yandex/maps/wiki/validator/storage/exception.h>

#include <maps/libs/geolib/include/serialization.h>

#include <library/cpp/testing/unittest/registar.h>

namespace gl = maps::geolib3;
namespace rev = maps::wiki::revision;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

namespace {

const rev::UserID OTHER_UID = 222;

} // namespace

Y_UNIT_TEST_SUITE_F(exclusions_gateway, DbFixture) {

Y_UNIT_TEST(test_exclusions_filtering)
{
    static const int START_TEST_TASK_ID = 314;

    DBID commitId1, commitId2;
    {
        // create two revisions of road element
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);

        RevisionID id = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(1 1)").objectId()
        );
        commitId1 = id.commitId();

        auto oldRevision = rg.snapshot(id.commitId()).objectRevision(id.objectId());

        std::list<rev::RevisionsGateway::NewRevisionData> cmtData;
        rev::RevisionsGateway::NewRevisionData objData{
            oldRevision->id(),
            oldRevision->data()};
        (*objData.second.attributes)["rd_el:dr"] = "1";
        cmtData.push_back(objData);

        const auto commit = rg.createCommit(cmtData, TEST_UID, {{"description", "test"}});
        commitId2 = commit.id();
        revisionTxn->commit();
    }

    auto revisionTxn = revisionPgPool()->slaveTransaction();
    auto snapshot = headCommitSnapshot(*revisionTxn);

    storage::storeResult(
        validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            commitId1,
            aoi,
            "./test_exclusions_filtering.0"
        ),
        *validationPgPool(),
        START_TEST_TASK_ID
    );

    storage::MessageId messageId;
    {
        // add exclusion
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ResultsGateway resGateway(*validationTxn, START_TEST_TASK_ID);
        auto messages = resGateway.messages({}, snapshot, 0, 100, TEST_UID);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
        messageId = messages[0].id();

        storage::ExclusionsGateway exclGateway(*validationTxn);
        exclGateway.addExclusion(messageId, TEST_UID, snapshot);

        validationTxn->commit();
    }

    // check that message is filtered by exclusion
    storage::storeResult(
        validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            commitId1,
            aoi,
            "./test_exclusions_filtering.1"
        ),
        *validationPgPool(),
        START_TEST_TASK_ID + 1
    );

    {
        auto validationTxn = validationPgPool()->slaveTransaction();
        storage::ResultsGateway resGateway(*validationTxn, START_TEST_TASK_ID + 1);
        auto messages = resGateway.messages({}, snapshot, 0, 100, TEST_UID);
        UNIT_ASSERT(messages.empty());
    }

    storage::storeResult(
        validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            commitId2,
            aoi,
            "./test_exclusions_filtering.2"
        ),
        *validationPgPool(),
        START_TEST_TASK_ID + 2
    );

    {
        auto validationTxn = validationPgPool()->slaveTransaction();
        storage::ResultsGateway resGateway(*validationTxn, START_TEST_TASK_ID + 2);
        auto messages = resGateway.messages({}, snapshot, 0, 100, TEST_UID);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    }

    {
        // remove exclusion
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ExclusionsGateway exclGateway(*validationTxn);
        exclGateway.removeExclusion(messageId, snapshot);
        validationTxn->commit();
    }

    // check that no exclusion remains
    storage::storeResult(
        validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            commitId1,
            aoi,
            "test_exclusions_filtering.3"
        ),
        *validationPgPool(),
        START_TEST_TASK_ID + 3
    );

    {
        auto validationTxn = validationPgPool()->slaveTransaction();
        storage::ResultsGateway resGateway(*validationTxn, START_TEST_TASK_ID + 3);
        auto messages = resGateway.messages({}, snapshot, 0, 100, TEST_UID);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    }
}

Y_UNIT_TEST(test_exclusions_add_remove)
{
    static const int TEST_TASK_ID = 314;

    DBID commitId;

    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        const RevisionID id = createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(1 1)").objectId()
        );
        commitId = id.commitId();
        revisionTxn->commit();
    }

    auto revisionTxn = revisionPgPool()->slaveTransaction();
    auto snapshot = headCommitSnapshot(*revisionTxn);

    storage::storeResult(
        validator.run(
            {"report_all"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            commitId,
            aoi,
            "test_exclusions_add_remove"
        ),
        *validationPgPool(),
        TEST_TASK_ID
    );

    storage::MessageId messageId;
    {
        auto validationTxn = validationPgPool()->slaveTransaction();
        storage::ResultsGateway resGateway(*validationTxn, TEST_TASK_ID);
        auto messages = resGateway.messages({}, snapshot, 0, 100, TEST_UID);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
        messageId = messages[0].id();
    }

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ExclusionsGateway exclGateway(*validationTxn);
        exclGateway.addExclusion(messageId, TEST_UID, snapshot);
        validationTxn->commit();
    }

    {
        auto validationTxn = validationPgPool()->slaveTransaction();
        storage::ExclusionsGateway exclGateway(*validationTxn);
        UNIT_ASSERT_VALUES_EQUAL(exclGateway.exclusionsForCheck("report_all", boost::none).size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(exclGateway.exclusionsForCheck("report_all", aoi.boundingBox()).size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(
            exclGateway.exclusionsForCheck(
                "report_all",
                gl::WKT::read<gl::Polygon2>("POLYGON((-2 -2, -2 -1, -1 -1, -1 -2, -2 -2))").boundingBox()
            ).size(),
            0
        );
        UNIT_ASSERT_VALUES_EQUAL(exclGateway.exclusionsForCheck("nonexistent", boost::none).size(), 0);
    }

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ExclusionsGateway exclGateway(*validationTxn);
        UNIT_CHECK_GENERATED_EXCEPTION(
            exclGateway.addExclusion(messageId, TEST_UID, snapshot),
            storage::ExclusionExistsError
        );
    }

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ExclusionsGateway exclGateway(*validationTxn);
        storage::MessageId invalidId;
        UNIT_CHECK_GENERATED_EXCEPTION(
            exclGateway.addExclusion(invalidId, TEST_UID, snapshot),
            storage::NonexistentMessageError
        );
    }

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ExclusionsGateway exclGateway(*validationTxn);
        exclGateway.removeExclusion(messageId, snapshot);
        UNIT_ASSERT_VALUES_EQUAL(
            exclGateway.exclusionsForCheck("report_all", aoi.boundingBox()).size(),
            0
        );
        validationTxn->commit();
    }

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ExclusionsGateway exclGateway(*validationTxn);
        UNIT_CHECK_GENERATED_EXCEPTION(
            exclGateway.removeExclusion(messageId, snapshot),
            storage::NonexistentExclusionError
        );
    }
}

Y_UNIT_TEST(test_exclusions_view)
{
    static const int TEST_TASK_ID = 314;

    DBID commitId;

    {
        auto revisionTxn = revisionPgPool()->masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        createRoadElement(
            rg, "LINESTRING(0 0, 1 1)",
            createJunction(rg, "POINT(0 0)").objectId(),
            createJunction(rg, "POINT(1 1)").objectId()
        );
        const RevisionID id = createRoadElement(
            rg, "LINESTRING(4 4, 6 6)",
            createJunction(rg, "POINT(4 4)").objectId(),
            createJunction(rg, "POINT(6 6)").objectId()

        );
        commitId = id.commitId();
        revisionTxn->commit();
    }

    auto revisionTxn = revisionPgPool()->slaveTransaction();
    auto snapshot = headCommitSnapshot(*revisionTxn);

    storage::storeResult(
        validator.run(
            {"report_all", "report_all_without_geom"},
            *revisionPgPool(),
            rev::TRUNK_BRANCH_ID,
            commitId,
            aoi,
            "test_exclusions_view"
        ),
        *validationPgPool(),
        TEST_TASK_ID
    );

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ResultsGateway resGateway(*validationTxn, TEST_TASK_ID);
        storage::ExclusionsGateway exclGateway(*validationTxn);
        auto messages = resGateway.messages({}, snapshot, 0, 100, TEST_UID);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 4);

        // report_all check, 1st message: created by OTHER_UID, viewed by OTHER_UID
        // report_all check, 2nd message: created by TEST_UID, viewed by TEST_UID
        // report_all_without_geom check, 1st message: created by TEST_UID, viewed by TEST_UID & OTHER_UID
        // report_all_without_geom check, 2nd message: created by TEST_UID, viewed by TEST_UID & OTHER_UID
        bool otherUidAdded = false;
        for (const auto& message : messages) {
            if (!otherUidAdded && message.message().attributes().checkId == "report_all") {
                exclGateway.addExclusion(message.id(), OTHER_UID, snapshot);
                otherUidAdded = true;

                auto viewResult = exclGateway.viewExclusion(message.id(), OTHER_UID, snapshot);
                UNIT_ASSERT(viewResult.id() == message.id());
                UNIT_ASSERT_VALUES_EQUAL(viewResult.exclusionInfo()->viewedBy.size(), 1);
            } else {
                exclGateway.addExclusion(message.id(), TEST_UID, snapshot);
                if (message.message().attributes().checkId == "report_all") {
                    auto viewResult = exclGateway.viewExclusion(message.id(), TEST_UID, snapshot);
                    UNIT_ASSERT(viewResult.id() == message.id());
                    UNIT_ASSERT_VALUES_EQUAL(viewResult.exclusionInfo()->viewedBy.size(), 1);
                } else {
                    auto viewResult = exclGateway.viewExclusion(message.id(), TEST_UID, snapshot);
                    UNIT_ASSERT(viewResult.id() == message.id());
                    UNIT_ASSERT_VALUES_EQUAL(viewResult.exclusionInfo()->viewedBy.size(), 1);

                    viewResult = exclGateway.viewExclusion(message.id(), OTHER_UID, snapshot);
                    UNIT_ASSERT(viewResult.id() == message.id());
                    UNIT_ASSERT_VALUES_EQUAL(viewResult.exclusionInfo()->viewedBy.size(), 1);
                }
            }
        }

        validationTxn->commit();
    }

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        storage::ResultsGateway resGateway(*validationTxn, TEST_TASK_ID);
        storage::ExclusionsGateway exclGateway(*validationTxn);
        auto messages = resGateway.messages({}, snapshot, 0, 100, TEST_UID);

        // Try again to view exclusions from report_all_without_geom check - no exceptions should be raised
        for (const auto& message : messages) {
            if (message.message().attributes().checkId == "report_all_without_geom") {
                auto viewResult = exclGateway.viewExclusion(message.id(), TEST_UID, snapshot);
                UNIT_ASSERT(viewResult.id() == message.id());
                UNIT_ASSERT_VALUES_EQUAL(viewResult.exclusionInfo()->viewedBy.size(), 1);

                viewResult = exclGateway.viewExclusion(message.id(), OTHER_UID, snapshot);
                UNIT_ASSERT(viewResult.id() == message.id());
                UNIT_ASSERT_VALUES_EQUAL(viewResult.exclusionInfo()->viewedBy.size(), 1);
            }
        }

        validationTxn->commit();
    }

    auto validationTxn = validationPgPool()->slaveTransaction();
    storage::ExclusionsGateway exclGateway(*validationTxn);
    storage::ExclusionsFilter filter;

    auto checkFilterWithStat = [&exclGateway, &snapshot](
            const storage::ExclusionsFilter& filter,
            size_t exclusionsSize, size_t statsSize)
    {
        UNIT_ASSERT_VALUES_EQUAL(
            exclGateway.exclusions(
                filter,
                snapshot,
                {0, 0},
                common::BeforeAfter::After,
                10).exclusions.size(),
            exclusionsSize
        );
        storage::MessageStatistics stats = exclGateway.statistics(filter);
        UNIT_ASSERT_VALUES_EQUAL(stats.size(), statsSize);
        size_t sum = 0;
        for (const auto& stat : stats) {
            sum += stat.second;
        }
        UNIT_ASSERT_VALUES_EQUAL(sum, exclusionsSize);
    };

    checkFilterWithStat(filter, 4, 2);

    filter.attributes.checkId = "report_all";
    checkFilterWithStat(filter, 2, 1);

    filter.attributes.checkId = "nonexistent";
    checkFilterWithStat(filter, 0, 0);

    filter.attributes.checkId = "report_all";
    filter.createdBy = TEST_UID;
    checkFilterWithStat(filter, 1, 1);

    filter.createdBy = OTHER_UID;
    checkFilterWithStat(filter, 1, 1);

    filter.createdBy = boost::none;
    filter.attributes.checkId = boost::none;
    filter.bbox = gl::BoundingBox(gl::Point2(0, 0), gl::Point2(1, 1));
    checkFilterWithStat(filter, 1, 1);

    filter.bbox = gl::BoundingBox(gl::Point2(4, 4), gl::Point2(6, 6));
    checkFilterWithStat(filter, 1, 1);

    filter.bbox = gl::BoundingBox(gl::Point2(10, 10), gl::Point2(11, 11));
    checkFilterWithStat(filter, 0, 0);

    auto checkFilter = [&exclGateway, &snapshot](
            const storage::ExclusionsFilter& filter,
            size_t exclusionsSize)
    {
        UNIT_ASSERT_VALUES_EQUAL(
            exclGateway.exclusions(
                filter,
                snapshot,
                {0, 0},
                common::BeforeAfter::After,
                10).exclusions.size(),
            exclusionsSize
        );
    };

    filter.bbox = boost::none;
    filter.viewedBy = storage::ViewedByFilter{TEST_UID, storage::ViewedState::Viewed};
    checkFilter(filter, 3);
    filter.viewedBy = storage::ViewedByFilter{TEST_UID, storage::ViewedState::NotViewed};
    checkFilter(filter, 1);

    filter.attributes.checkId = "report_all";

    filter.viewedBy = storage::ViewedByFilter{TEST_UID, storage::ViewedState::Viewed};
    checkFilter(filter, 1);
    filter.viewedBy = storage::ViewedByFilter{TEST_UID, storage::ViewedState::NotViewed};
    checkFilter(filter, 1);
    filter.viewedBy = storage::ViewedByFilter{OTHER_UID, storage::ViewedState::Viewed};
    checkFilter(filter, 1);
    filter.viewedBy = storage::ViewedByFilter{OTHER_UID, storage::ViewedState::NotViewed};
    checkFilter(filter, 1);

    filter.attributes.checkId = "report_all_without_geom";

    filter.viewedBy = storage::ViewedByFilter{TEST_UID, storage::ViewedState::Viewed};
    checkFilter(filter, 2);
    filter.viewedBy = storage::ViewedByFilter{TEST_UID, storage::ViewedState::NotViewed};
    checkFilter(filter, 0);
    filter.viewedBy = storage::ViewedByFilter{OTHER_UID, storage::ViewedState::Viewed};
    checkFilter(filter, 2);
    filter.viewedBy = storage::ViewedByFilter{OTHER_UID, storage::ViewedState::NotViewed};
    checkFilter(filter, 0);
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
