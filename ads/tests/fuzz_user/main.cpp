#include <ads/pytorch/deploy_v2/deep_embedding_model/model.h>
#include <ads/pytorch/deploy_v2/factory/factory.h>
#include <ads/bigkv/preprocessor_primitives/base_entities/infer_converters.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/helpers.h>
#include <ads/bigkv/search/entities/user/user.h>
#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NProfilePreprocessing;


DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    NPytorchTransport2::TDeepEmbeddingModel userModel(
        NPytorchTransport2::LoadDeepEmbeddingModelFromDisk("./user")
    );

    NSearchTsar::TUserEntity userEntity;
    NProfilePreprocessing::TUserProtoBuilder profileBuilder;

    THashMap<TString, NPytorchTransport2::TCategoricalInputs> catFeatures;
    THashMap<TString, NPytorchTransport2::TRealvalueInputs> realvalueFeatures;

    userEntity.MakeInputs(
        {profile}, {123},
        catFeatures, realvalueFeatures
    );

    auto result = userModel.Forward(catFeatures, realvalueFeatures);

    Y_UNUSED(result);
}
