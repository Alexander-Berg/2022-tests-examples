#pragma once

#include <library/cpp/json/writer/json_value.h>

namespace NYP::NYPReplica::NTesting {

void DecodeShortJson(NJson::TJsonValue& shortJson);

void DecodeFullJson(NJson::TJsonValue& fullJson);

void DecodeJson(NJson::TJsonValue& json);

} // namespace NYP::NYPReplica::NTesting
