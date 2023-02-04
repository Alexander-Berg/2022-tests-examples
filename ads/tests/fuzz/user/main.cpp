#include <ads/tensor_transport/user_embedder/user_embedder.h>
#include <ads/tensor_transport/lib_2/st_hash/difacto_model.h>
#include <ads/tensor_transport/lib_2/difacto_model.h>
#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>

#include <library/cpp/testing/unittest/registar.h>


DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    THashMap<ui64, TVector<float>> modelTable;
    NTsarTransport::TDifactoHashTableReader reader("./model_dump");
    reader.ReadTable(modelTable);
    NTsarTransport::TUserEmbedder<NTsarTransport::TModel> userEmbedder(std::move(modelTable));
    auto vec1 = userEmbedder.Call(profile);
    UNIT_ASSERT_EQUAL(vec1.size(), 50 + 1);

    NTsarTransport::TUserEmbedder<NTsarTransport::TSTModel> stUserEmbedder("./st_model_dump");
    auto vec2 = stUserEmbedder.Call(profile);
    UNIT_ASSERT_EQUAL(vec2.size(), 50 + 1);

    auto vec3 = userEmbedder.Call(profile);
    UNIT_ASSERT_EQUAL(vec3.size(), 50 + 1);
    for (ui32 i = 0; i < vec1.size(); ++i) {
        UNIT_ASSERT_DOUBLES_EQUAL(vec1[i], vec3[i], 1e-2);
    }
}
