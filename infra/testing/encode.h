#pragma once

#include <library/cpp/json/writer/json_value.h>

namespace NYP::NYPReplica::NTesting {

void EncodeShortJson(NJson::TJsonValue& shortJson);

void EncodeFullJson(NJson::TJsonValue& fullJson);

void EncodeJson(NJson::TJsonValue& json);

} // namespace NYP::NYPReplica::NTesting
