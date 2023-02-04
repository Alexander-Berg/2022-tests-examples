#include <ads/tensor_transport/user_embedder/user_embedder.h>
#include <ads/tensor_transport/lib_2/st_hash/difacto_model.h>
#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>
#include <ads/tensor_transport/lib_2/st_hash/st_hash.h>
#include <ads/tensor_transport/lib_2/difacto_model.h>

#include <mapreduce/yt/common/node_builder.h>
#include <library/cpp/yson/parser.h>

#include <library/cpp/testing/unittest/registar.h>

#include <sys/stat.h>


NBSYeti::TBigbPublicProto GetProfile(const NYT::TNode& node) {
    NBSYeti::TBigbPublicProto profile;
    const auto &profileDump = node["ProfileDump"];
    if (!profileDump.IsNull()) {
        Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString(profileDump.AsString());
    } else {
        Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString("");
    }
    return profile;
}

ui32 GetFileSize(TString filename) {
    struct stat stat_buf;
    int rc = stat(filename.c_str(), &stat_buf);
    return rc == 0 ? stat_buf.st_size : -1;
}


Y_UNIT_TEST_SUITE(TestSTHash) {
    Y_UNIT_TEST(TestSameAsTHashMap) {
        THashMap<ui64, TVector<float>> modelTable;
        NTsarTransport::TDifactoHashTableReader reader("./model_dump");
        reader.ReadTable(modelTable);
        auto userEmbedder = NTsarTransport::TUserEmbedder<NTsarTransport::TModel>(std::move(modelTable));

        NTsarTransport::ConvertModelToST("./model_dump", "./st_model_dump");
        auto userSTEmbedder = NTsarTransport::TUserEmbedder<NTsarTransport::TSTModel>("./st_model_dump");

        NTsarTransport::ConvertModelToST<ui16>("./model_dump", "./st_model_dump_compressed");
        auto userSTEmbedderCompressed = NTsarTransport::TUserEmbedder<NTsarTransport::TSTCompressedModel<ui16>>("./st_model_dump_compressed");

        ui32
            stModelSize = GetFileSize("./st_model_dump"),
            stModelSizeCompressed = GetFileSize("./st_model_dump_compressed");
        UNIT_ASSERT_C(
            stModelSizeCompressed < stModelSize * 0.6,
            TStringBuilder{} << "Commpressed models size too large\n" << "STModel size = " << stModelSize << "; " << "CompressedSTModel size = " << stModelSizeCompressed << Endl
        );
        Cout << "Original model size = " << stModelSize << "\nCompressed model size = " << stModelSizeCompressed << Endl;

        TFileInput inputData("input_table");
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);

        while (parser.Parse()) {
            auto mapNode = currentNode.AsList().front();
            auto profile = GetProfile(mapNode);
            Cout << "Profile is parsed" << Endl;

            auto vec1 = userEmbedder.Call(profile);
            Cout << "Classic userEmbedder works fine" << Endl;
            auto vec2 = userSTEmbedder.Call(profile);
            Cout << "New userEmbedder works fine" << Endl;
            auto vec3 = userSTEmbedderCompressed.Call(profile);
            Cout << "Compressed userEmbedder works fine" << Endl;

            for (size_t i = 0; i < vec1.size(); ++i) {
                Cout << vec1[i] << " " << vec2[i] << " " << vec3[i] << Endl;

                UNIT_ASSERT_DOUBLES_EQUAL_C(abs(vec1[i] - vec2[i]), 0, 1.e-7, TStringBuilder{} << vec1[i] << " != " << vec2[i]);
                UNIT_ASSERT_DOUBLES_EQUAL_C(abs(vec1[i] - vec3[i]), 0, 1.e-2, TStringBuilder{} << vec1[i] << " != " << vec3[i]);
            }
            currentNode.Clear();
        }
    }
}
