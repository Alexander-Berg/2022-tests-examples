#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/actions/save_object.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/commit_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/extended_editor_fixture.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/object_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/objects_creator/objects_creator.h>

#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>

#include <yandex/maps/wiki/social/feedback/gateway_ro.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>


namespace maps::wiki::tests {

using DbWithAoi = ExtendedEditorFixture<ViewSyncronizer>;

const auto NO_FEEDBACK_TASK_ID = std::nullopt;

const std::vector<social::CommentType> MODERATED_COMMENT_TYPES = {
    social::CommentType::Complaint,
    social::CommentType::RequestForDeletion,
};

const std::vector<social::CommentType> NON_MODERATED_COMMENT_TYPES = {
    social::CommentType::Info,
    social::CommentType::Warn,
    social::CommentType::Annotation,
    social::CommentType::RevertReason,
};


Y_UNIT_TEST_SUITE(comments)
{
    WIKI_FIXTURE_TEST_CASE_P(
        should_not_create_feedback_task_for_non_moderated_comments, DbWithAoi,
        NON_MODERATED_COMMENT_TYPES)
    {
        auto commentType = getParam();

        const auto aoiCreate = saveObject(user, aoi(), observers);
        performRequest<CommentsCreate>(
            observers,
            UserContext(user, {}),
            commentType,
            "Comment",
            aoiCreate.commitId,
            aoiCreate.objectId,
            NO_FEEDBACK_TASK_ID
        );

        auto txn = db.pool().slaveTransaction();
        auto tasks = social::feedback::GatewayRO(*txn).tasksByFilter(
            social::feedback::TaskFilter().objectId(aoiCreate.objectId));
        UNIT_ASSERT(tasks.empty());
    }

    WIKI_FIXTURE_TEST_CASE_P(
        should_create_feedback_task_for_moderated_comments, DbWithAoi,
        MODERATED_COMMENT_TYPES)
    {
        auto commentType = getParam();

        saveObject(user, aoi(), observers);
        const auto buildingCreate = saveObject(user, building(), observers);
        performRequest<CommentsCreate>(
            observers,
            UserContext(user, {}),
            commentType,
            "Comment",
            buildingCreate.commitId,
            buildingCreate.objectId,
            NO_FEEDBACK_TASK_ID
        );

        auto txn = db.pool().slaveTransaction();
        social::feedback::GatewayRO gtw(*txn);
        auto tasks = gtw.tasksByFilter(
            social::feedback::TaskFilter().objectId(buildingCreate.objectId));
        UNIT_ASSERT_EQUAL(tasks.size(), 1);
        const auto& task = tasks.front();

        UNIT_ASSERT_EQUAL(task.type(), social::feedback::Type::Building);
        UNIT_ASSERT_EQUAL(task.workflow(), social::feedback::Workflow::Feedback);
        UNIT_ASSERT_EQUAL(task.hidden(), true);
        UNIT_ASSERT_EQUAL(task.internalContent(), false);
        UNIT_ASSERT_EQUAL(task.source(), "nmaps-" + std::string(toString(commentType)));

        auto history = gtw.history(task.id());
        UNIT_ASSERT_EQUAL(history.items().size(), 1);
        const auto& item = history.items().front();
        UNIT_ASSERT_EQUAL(item.operation(), social::feedback::TaskOperation::Create);
        UNIT_ASSERT_EQUAL(item.modifiedBy(), user);
    }
}

} // namespace maps::wiki::tests
