#include "fb_task_creator.h"

#include <maps/libs/common/include/exception.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/agent.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/attribute_names.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/gateway.h>

#include <fmt/format.h>


namespace maps::wiki::social::feedback::tests {

const TUid            FbTaskCreator::DEFAULT_UID         = 1;
const geolib3::Point2 FbTaskCreator::DEFAULT_POSITION    = {0, 0};
const Type            FbTaskCreator::DEFAULT_TYPE        = feedback::Type::Road;
const std::string     FbTaskCreator::DEFAULT_SOURCE      = "fbapi";
const Description     FbTaskCreator::DEFAULT_DESCRIPTION = feedback::Description();


namespace {

std::string
prepareCreateResolveQuery(TId taskId, const std::string& createdAt, const std::string& resolvedAt)
{
    std::string result;
    const auto& stateModifiedAt = resolvedAt.empty() ? createdAt : resolvedAt;

    if (!createdAt.empty()) {
        result += fmt::format(
            "UPDATE social.feedback_task SET created_at = '{}' WHERE id = {};\n",
            createdAt, taskId
        );
    }

    if (!resolvedAt.empty()) {
        result += fmt::format(
            "UPDATE social.feedback_task SET resolved_at = '{}' WHERE id = {};\n",
            resolvedAt, taskId
        );
    }

    if (!stateModifiedAt.empty()) {
        result += fmt::format(
            "UPDATE social.feedback_task SET state_modified_at = '{}' WHERE id = {};\n",
            stateModifiedAt, taskId
        );
    }

    return result;
}


TId
createComment(pqxx::transaction_base& txn, TUid uid, TId taskId)
{
    static const std::string EMPTY_COMMENT = "";
    static const TId COMMIT_ID = 0;
    static const TId OBJECT_ID = 0;
    static const TIds AOI_IDS = {};

    return Gateway(txn).createComment(
        uid,
        CommentType::Info,
        EMPTY_COMMENT,
        COMMIT_ID,
        OBJECT_ID,
        taskId,
        AOI_IDS
    ).id();
}


TaskForUpdate
unwrap(std::optional<TaskForUpdate>&& task)
{
    ASSERT(task);
    return *std::move(task);
}


} // namespace


FbTaskCreator::FbTaskCreator(pqxx::transaction_base& txn, TaskState state)
    : txn_(txn)
    , state_(state)
{
    task_.attrs.addCustom(attrs::USER_EMAIL, "user@yandex.ru");
}

FbTaskCreator& FbTaskCreator::objectUri(const std::string& value)
{
    task_.attrs.addCustom(attrs::OBJECT_URI, value);
    return *this;
}

TaskForUpdate
FbTaskCreator::create()
{
    Agent agent{txn_, uid_};

    auto result = agent.addTask(task_);

    if (state_ != TaskState::Incoming) {
        result = unwrap(agent.revealTaskByIdCascade(result.id()));
    }

    switch (state_) {
        case TaskState::Incoming: break;
        case TaskState::Opened: break;
        case TaskState::NeedInfo:
            result = unwrap(agent.needInfoTask(result, createComment(txn_, uid_, result.id()), std::nullopt));
            break;
        case TaskState::Accepted:
            result = unwrap(agent.resolveTaskCascade(result, Resolution::createAccepted(), std::nullopt));
            break;
        default:
            throw LogicError("Not implemented.");
    }

    const auto createResolveQuery = prepareCreateResolveQuery(result.id(), createdAt_, resolvedAt_);
    if (!createResolveQuery.empty()) {
        txn_.exec(createResolveQuery);
        result = unwrap(agent.taskForUpdateById(result.id()));
    }

    return result;
}

} // namespace maps::wiki::social::feedback::tests
