#pragma once

#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/enums.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/task.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/task_new.h>

#include <pqxx/pqxx>

#include <optional>


namespace maps::wiki::social::feedback::tests {

class FbTaskCreator {
public:
    static const TUid            DEFAULT_UID;
    static const geolib3::Point2 DEFAULT_POSITION;
    static const Type            DEFAULT_TYPE;
    static const std::string     DEFAULT_SOURCE;
    static const Description     DEFAULT_DESCRIPTION;

    FbTaskCreator(pqxx::transaction_base& txn, TaskState state = TaskState::Incoming);

    operator Task()  { return create(); }
    TaskForUpdate operator*() { return create(); }
    TaskForUpdate create();

    FbTaskCreator& createdAt(std::string value) { createdAt_ = std::move(value); return *this; }
    FbTaskCreator& resolvedAt(std::string value) { resolvedAt_ = std::move(value); return *this; }
    FbTaskCreator& source(std::string value) { task_.source = std::move(value); return *this; }
    FbTaskCreator& type(Type value) { task_.type = value; return *this; }
    FbTaskCreator& hidden(bool value) { task_.hidden = value; return *this; }
    FbTaskCreator& uid(TUid value) { uid_ = value; return *this; }
    FbTaskCreator& position(geolib3::Point2 pt) { task_.position = pt; return *this; }
    FbTaskCreator& objectUri(const std::string& value);

private:
    pqxx::transaction_base& txn_;

    TUid uid_{DEFAULT_UID};
    TaskNew task_{DEFAULT_POSITION, DEFAULT_TYPE, DEFAULT_SOURCE, DEFAULT_DESCRIPTION};
    TaskState state_;
    std::string createdAt_;
    std::string resolvedAt_;
};

} // namespace maps::wiki::social::feedback::tests
