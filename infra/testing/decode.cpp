#include "decode.h"

#include "testing_storage.h"

#include <library/cpp/string_utils/base64/base64.h>

namespace NYP::NYPReplica::NTesting {

namespace {

NJson::TJsonValue Decode(TString value) {
    value = Base64Decode(value);
    return NJson::ReadJsonFastTree(value, true);
}

}

void DecodeShortJson(NJson::TJsonValue& shortJson) {
    for (auto& [key, jsonValue] : shortJson["replica_objects"].GetMapSafe()) {
        jsonValue = Decode(jsonValue.GetStringRobust());
    }
    if (shortJson.Has("meta")) {
        shortJson["meta"] = Decode(shortJson["meta"].GetStringRobust());
    }
}

void DecodeFullJson(NJson::TJsonValue& fullJson) {
    for (auto& [key, jsonValue] : fullJson.GetMapSafe()) {
        jsonValue = Decode(jsonValue.GetStringRobust());
    }
}

void DecodeJson(NJson::TJsonValue& json) {
    if (IsFullJson(json)) {
        DecodeFullJson(json);
    } else {
        DecodeShortJson(json);
    }
}

} // namespace NYP::NYPReplica::NTesting
