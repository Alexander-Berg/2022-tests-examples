#pragma once

#include "maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/event.h"
#include "maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/task.h"

#include <pqxx/pqxx>

#include <optional>


namespace maps::wiki::social::tests {

class TaskCreator {
public:
    TaskCreator(pqxx::transaction_base& txn)
        : txn_(txn)
    {}

    operator Task() {
        return create();
    }

    Task operator()() {
        return create();
    }

    Task create();

    TaskCreator& event(Event event) { event_ = std::move(event); return *this; };
    TaskCreator& userCreatedOrUnbannedAt(std::string userCreatedOrUnbannedAt) { userCreatedOrUnbannedAt_ = std::move(userCreatedOrUnbannedAt); return *this; }
    TaskCreator& createdAt(const std::string& value);

    TaskCreator& locked(TUid lockedBy, const std::string& lockedAt = "NOW()");
    TaskCreator& resolved(
        TUid resolvedBy,
        ResolveResolution resolveResolution = ResolveResolution::Accept,
        const std::string& resolvedAt = "NOW()");
    TaskCreator& closed(
        TUid closedBy,
        CloseResolution closeResolution = CloseResolution::Approve,
        const std::string& closedAt = "NOW()");

private:
    pqxx::transaction_base& txn_;
    std::string updateQueryFormat_;

    std::optional<Event> event_;
    std::string userCreatedOrUnbannedAt_ = "2015-01-25 00:00:00";
};

} // namespace maps::wiki::social::tests
