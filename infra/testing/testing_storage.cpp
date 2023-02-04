#include "testing_storage.h"

namespace NYP::NYPReplica::NTesting {

bool IsFullJson(const NJson::TJsonValue& json) {
    return !json.Has("replica_objects");
}

} // NYP::NYPReplica::NTesting
