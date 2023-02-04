#pragma once

#include <json/json.h>

#include <string>

namespace quasar::TestUtils {

    std::string prepareSyncAddition(const std::string& requestId, int index);

    std::string prepareSyncAdditionWithShots(const std::string& requestId, int index);

    std::string prepareUrl(const std::string& requestId, const std::string& prefix = "");

    std::string prepareYandexMusicSuccessResponse(const std::string& requestId);

    Json::Value getMusicPlayOptions(std::string sessionId = "deadbeef");

    Json::Value getNextOptions(bool skip, bool setPause = false);

    Json::Value getPrevOptions(bool setPause);

    std::string getPrefixFromUrl(const std::string& url);

} // namespace quasar::TestUtils
