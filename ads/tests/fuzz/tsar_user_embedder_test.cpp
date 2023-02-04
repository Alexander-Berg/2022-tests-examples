#include <ads/pytorch/deploy/lib/tsar_user_embedder.h>
#include <ads/pytorch/deploy/model_builder_lib/partitioned_model.h>
#include <ads/pytorch/deploy/lib/tsar_computer.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NPytorchTransport;

DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    auto computerPtr = MakeHolder<TTsarComputer>(new TPartitionedModel("UserNamespaces_2", false));
    TTsarUserEmbedder userEmbedder(std::move(computerPtr));

    auto vec1 = userEmbedder.Call(profile);
    UNIT_ASSERT_EQUAL(vec1.size(), 51ULL);
    UNIT_ASSERT_EQUAL(vec1[0], 1.f);
}
