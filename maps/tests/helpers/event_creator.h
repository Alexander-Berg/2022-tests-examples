#pragma once

#include "maps/wikimap/mapspro/libs/social/factory.h"
#include "maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/event.h"

namespace maps::wiki::social::tests {

class EventCreator {
public:
    EventCreator(pqxx::transaction_base& txn)
        : txn_(txn)
    {
        ++commitId_;
    }

    operator Event() {
        return create();
    }

    Event create() {
        return Factory::componentCreate<Event>(
            txn_,
            eventType_,
            uid_,
            CommitData(branchId_, commitId_, action_, bounds_),
            primaryObjData_,
            aoiIds_,
            extraData_
        );
    }

    EventCreator& uid(TUid id) { uid_ = id; return *this; };
    EventCreator& branchId(TId id) { branchId_ = id; return *this; };
    EventCreator& commitId(TId id) { commitId_ = id; return *this; };
    EventCreator& action(std::string action) { action_ = std::move(action); return *this; };
    EventCreator& bounds(std::string bounds) { bounds_ = std::move(bounds); return *this; };
    EventCreator& primaryObjData(PrimaryObjectData data) { primaryObjData_ = std::move(data); return *this; };
    EventCreator& extraData(EventExtraData data) { extraData_ = std::move(data); return *this; }
    EventCreator& aoiIds(TIds ids) { aoiIds_ = std::move(ids); return *this; };
    EventCreator& type(EventType eventType) { eventType_ = eventType; return *this; }

private:
    pqxx::transaction_base& txn_;

    TUid uid_ = 1;
    TId branchId_ = 0;
    static TId commitId_;
    std::string action_;
    std::string bounds_;
    std::optional<PrimaryObjectData> primaryObjData_;
    std::optional<EventExtraData> extraData_;
    TIds aoiIds_;
    EventType eventType_ = EventType::Edit;
};

} // namespace maps::wiki::social::tests
