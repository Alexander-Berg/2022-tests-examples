#include <ads/tensor_transport/user_embedder/user_embedder.h>
#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>
#include <ads/tensor_transport/lib_2/difacto_model.h>
#include <ads/tensor_transport/lib_2/vinyl/difacto_model.h>
#include <ads/tensor_transport/lib_2/vinyl/vinyl.h>

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


Y_UNIT_TEST_SUITE(TestVinyl) {
    Y_UNIT_TEST(TestSameAsTHashMap) {
        THashMap<ui64, TVector<float>> modelTable;
        NTsarTransport::TDifactoHashTableReader reader("./model_dump");
        reader.ReadTable(modelTable);
        auto userEmbedder = NTsarTransport::TUserEmbedder<NTsarTransport::TModel>(std::move(modelTable));

        NTsarTransport::ConvertModelToVinyl("./model_dump", "./vinyl_model_dump", "", 2);
        auto userVinylEmbedder = NTsarTransport::TUserEmbedder<NTsarTransport::TVinylModel>("./vinyl_model_dump", false, 2);

        NTsarTransport::ConvertModelToVinyl<ui16>("./model_dump", "./vinyl_model_dump_compressed");
        auto userVinylEmbedderCompressed = NTsarTransport::TUserEmbedder<NTsarTransport::TVinylCompressedModel<ui16>>("./vinyl_model_dump_compressed", false, 1);

        ui32
            vinylModelSize = GetFileSize("./vinyl_model_dump"),
            vinylModelSizeCompressed = GetFileSize("./vinyl_model_dump_compressed");
        UNIT_ASSERT_C(
            vinylModelSizeCompressed < vinylModelSize * 0.6,
            TStringBuilder{} << "Commpressed models size too large\n" << "VinylModel size = " << vinylModelSize << "; " << "CompressedVinylModel size = " << vinylModelSizeCompressed << Endl
        );
        UNIT_ASSERT_C(
            vinylModelSizeCompressed >= vinylModelSize * 0.4,
            TStringBuilder{} << "Commpressed models size too small\n" << "VinylModel size = " << vinylModelSize << "; " << "CompressedVinylModel size = " << vinylModelSizeCompressed << Endl
        );
        Cout << "Original model size = " << vinylModelSize << "\nCompressed model size = " << vinylModelSizeCompressed << Endl;

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
            auto vec2 = userVinylEmbedder.Call(profile);
            Cout << "New userEmbedder works fine" << Endl;
            auto vec3 = userVinylEmbedderCompressed.Call(profile);
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
