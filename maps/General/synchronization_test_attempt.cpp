#include "synchronization_test_attempt.h"

#include <maps/goods/lib/error_reporter/error_reporter.h>
#include <maps/libs/json/include/std/optional.h>

namespace maps::goods {

SynchronizationTestAttempt SynchronizationTestAttempt::fromJson(const maps::json::Value& json)
{
    SynchronizationTestAttempt result;
    result.url = validateAndNormalizeUrl(json["url"].as<std::string>());
    
    result.fileType = getSynchronizationFileTypeFromJson(json["file_type"]);

    return result;
}

void json(const SynchronizationTestAttempt& attempt, maps::json::ObjectBuilder builder)
{
    builder["id"] << std::to_string(attempt.id);
    builder["url"] << attempt.url;
    builder["file_type"] << toString(attempt.fileType);
    builder["status"] << [&](maps::json::ObjectBuilder builder) {
        builder["sync_state"] << toString(attempt.syncState);
        builder["message"] << attempt.message;
        builder["parse_info"] << attempt.getParseInfo();
    };
}

} // namespace maps::goods
