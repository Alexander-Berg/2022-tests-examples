#include "encode.h"

#include "testing_storage.h"

#include <library/cpp/string_utils/base64/base64.h>

namespace NYP::NYPReplica::NTesting {

void EncodeShortJson(NJson::TJsonValue& shortJson) {
    for (auto& [key, jsonValue] : shortJson["replica_objects"].GetMapSafe()) {
        jsonValue = Base64Encode(jsonValue.GetStringRobust());
    }
    if (shortJson.Has("meta")) {
        shortJson["meta"] = Base64Encode(shortJson["meta"].GetStringRobust());
    }
}

void EncodeFullJson(NJson::TJsonValue& fullJson) {
    for (auto& [key, jsonValue] : fullJson.GetMapSafe()) {
        jsonValue = Base64Encode(jsonValue.GetStringRobust());
    }
}

void EncodeJson(NJson::TJsonValue& json) {
    if (NTesting::IsFullJson(json)) {
        EncodeFullJson(json);
    } else {
        EncodeShortJson(json);
    }
}

} // namespace NYP::NYPReplica::NTesting
