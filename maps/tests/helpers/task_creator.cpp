#include "task_creator.h"

#include "event_creator.h"
#include "maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/gateway.h"

#include <maps/libs/common/include/exception.h>

#include <fmt/format.h>


namespace maps::wiki::social::tests {

using namespace fmt::literals;

Task
TaskCreator::create()
{
    const auto event = event_ ? *event_ : EventCreator(txn_).create();

    Gateway socialGw(txn_);
    auto result = socialGw.createTask(event, userCreatedOrUnbannedAt_);

    if (!updateQueryFormat_.empty()) {
        txn_.exec(fmt::format(updateQueryFormat_, "event_id"_a = event.id()));
        auto tasks = socialGw.loadTasksByTaskIds({result.id()});
        ASSERT(tasks.size() == 1);
        result = std::move(tasks[0]);
    }

    return result;
}

TaskCreator& TaskCreator::createdAt(const std::string& value)
{
    updateQueryFormat_ += fmt::format(
        "UPDATE social.task SET created_at = {} WHERE event_id = {{event_id}};\n",
        value
    );
    return *this;
}

TaskCreator&
TaskCreator::locked(TUid lockedBy, const std::string& lockedAt)
{
    updateQueryFormat_ += fmt::format(
        "UPDATE social.task SET locked_by = {}, locked_at = {} WHERE event_id = {{event_id}};\n",
        lockedBy, lockedAt
    );
    return *this;
}

TaskCreator&
TaskCreator::resolved(TUid resolvedBy, ResolveResolution resolveResolution, const std::string& resolvedAt)
{
    updateQueryFormat_ += fmt::format(
        "UPDATE social.task SET resolved_by = {}, resolved_at = {}, resolve_resolution = '{}' WHERE event_id = {{event_id}};\n",
        resolvedBy, resolvedAt, toString(resolveResolution)
    );
    return *this;
}

TaskCreator&
TaskCreator::closed(TUid closedBy, CloseResolution closeResolution, const std::string& closedAt)
{
    updateQueryFormat_ += fmt::format(
        "UPDATE social.task SET closed_by = {}, closed_at = {}, close_resolution = '{}' WHERE event_id = {{event_id}};\n",
        closedBy, closedAt, toString(closeResolution)
    );
    return *this;
}

} // namespace maps::wiki::social::tests
