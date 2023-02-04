#pragma once

#include <ads/bigkv/preprocessor_primitives/base_preprocessor/base_preprocessor.h>


#include <library/cpp/yson/node/node.h>
#include <library/cpp/testing/unittest/registar.h>


namespace NProfilePreprocessing {

    class TAdGroupProtoBuilder {
    public:
        TAdGroupProtoBuilder() = default;

        NCSR::TAdGroupProfileProto* GetProfile();
        TString GetDump();

    private:
        NCSR::TAdGroupProfileProto Profile;
    };

}
