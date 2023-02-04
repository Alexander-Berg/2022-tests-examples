#include <ads/tensor_transport/user_embedder/user_embedder.h>
#include <ads/tensor_transport/lib_2/st_hash/difacto_model.h>
#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>
#include <ads/tensor_transport/lib_2/st_hash/st_hash.h>
#include <ads/tensor_transport/lib_2/difacto_model.h>

#include <mapreduce/yt/common/node_builder.h>
#include <library/cpp/yson/parser.h>

#include <thread>

#include <library/cpp/testing/unittest/registar.h>


Y_UNIT_TEST_SUITE(TestMultithread) {
    Y_UNIT_TEST(TestMultithread) {
        THashMap<ui64, TVector<float>> modelTable;
        NTsarTransport::TDifactoHashTableReader reader("./model_dump");
        reader.ReadTable(modelTable);
        auto userEmbedder = NTsarTransport::TUserEmbedder<NTsarTransport::TModel>(std::move(modelTable));

        NTsarTransport::ConvertModelToST("./model_dump", "./st_model_dump");
        auto userSTEmbedder = NTsarTransport::TUserEmbedder<NTsarTransport::TSTModel>("./st_model_dump");

        NTsarTransport::ConvertModelToST<ui16>("./model_dump", "./st_model_dump_compressed");
        auto userSTEmbedderCompressed = NTsarTransport::TUserEmbedder<NTsarTransport::TSTCompressedModel<ui16>>("./st_model_dump_compressed");

        TFileInput inputData("input_table");
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);
        TVector<NBSYeti::TBigbPublicProto> profiles;
        NBSYeti::TBigbPublicProto profile;

        while (parser.Parse()) {
            auto mapNode = currentNode.AsList().front();
            auto profileDump = mapNode["ProfileDump"];
            if (!profileDump.IsNull()) {
                Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString(profileDump.AsString());
            } else {
                Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString("");
            }
            profiles.push_back(profile);
            currentNode.Clear();
        }

        auto applyModel = [&]() {
            for (auto &profile: profiles) {
                userEmbedder.Call(profile);
            }
        };
        auto applySTModel = [&]() {
            for (auto &profile: profiles) {
                userSTEmbedder.Call(profile);
            }
        };
        auto applyCompressedSTModel = [&]() {
            for (auto &profile: profiles) {
                userSTEmbedderCompressed.Call(profile);
            }
        };

        TVector<std::thread> ths;
        for (size_t i = 0; i < 10; ++i) {
            ths.emplace_back(std::thread(applyModel));
        }
        for (size_t i = 0; i < 10; ++i) {
            ths.emplace_back(std::thread(applySTModel));
        }
        for (size_t i = 0; i < 10; ++i) {
            ths.emplace_back(std::thread(applyCompressedSTModel));
        }
        for (auto &t: ths) {
            t.join();
        }
    }
}
