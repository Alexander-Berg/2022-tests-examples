#pragma once

#include <maps/goods/lib/goods_db/entities/synchronization_attempt.h>

namespace maps::goods {

class SynchronizationTestAttempt
    : private ParseInfoHolder
{
public:
    using Id = int64_t;

    Id id = 0;
    Organization::Id organizationId = 0;
    int64_t authorUid = 0;
    std::string url;
    ImportFileType fileType = ImportFileType::Yml;
    maps::chrono::TimePoint syncTime = {};
    SynchronizationStatus syncState = SynchronizationStatus::Waiting;
    std::optional<std::string> message;

public:
    using ParseInfoHolder::getParseInfo;
    using ParseInfoHolder::setParseInfo;

    static SynchronizationTestAttempt fromJson(const maps::json::Value& json);

private:
    template<typename T>
    static auto introspect(T& t)
    {
        return std::tie(
            t.id,
            t.organizationId,
            t.authorUid,
            t.url,
            t.fileType,
            t.syncTime,
            t.syncState,
            t.message,
            t.totalItemsCount,
            t.parsedItemsCount,
            t.itemsWithPhotoCount
        );
    };
    friend class sql_chemistry::GatewayAccess<SynchronizationTestAttempt>;
};

void json(const SynchronizationTestAttempt& attempt, maps::json::ObjectBuilder builder);

} // namespace maps::goods
