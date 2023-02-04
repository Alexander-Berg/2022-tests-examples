#include "adgroup_proto_helpers.h"


namespace NProfilePreprocessing {
    NCSR::TAdGroupProfileProto* TAdGroupProtoBuilder::GetProfile() {
        return &Profile;
    }

    TString TAdGroupProtoBuilder::GetDump() {
        TString protoString;
        Y_PROTOBUF_SUPPRESS_NODISCARD Profile.SerializeToString(&protoString);
        return protoString;
    }
}
