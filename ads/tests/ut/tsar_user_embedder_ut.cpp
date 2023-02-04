#include <mapreduce/yt/common/node_builder.h>
#include <library/cpp/yson/parser.h>

#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/lib/tsar_user_embedder.h>
#include <ads/pytorch/deploy/model_builder_lib/partitioned_model.h>
#include <ads/pytorch/deploy/lib/tsar_computer.h>
#include <ads/bigkv/torch_v2/entities/user.h>

#include <util/stream/file.h>

using namespace NPytorchTransport;

NBSYeti::TBigbPublicProto GetProfile(const NYT::TNode& node) {
    NBSYeti::TBigbPublicProto profile;
    const auto& profileDump = node["ProfileDump"];
    if (!profileDump.IsNull()) {
        Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString(profileDump.AsString());
    }
    return profile;
}

Y_UNIT_TEST_SUITE(TTsarUserEmbedderTestSuit) {

    Y_UNIT_TEST(TPartitionedEigenModel) {
        auto computerPtr = MakeHolder<TTsarComputer>(new TPartitionedModel("UserNamespaces_2", false));
        TTsarUserEmbedder embedder(std::move(computerPtr));
        TFileInput inputData("input_table");
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);

        while (parser.Parse()) {
            auto mapNode = currentNode.AsList().front();
            auto profile = GetProfile(mapNode);
            auto embedding = embedder.Call(profile);
            UNIT_ASSERT_EQUAL(embedding.size(), 51ULL);
            UNIT_ASSERT_EQUAL(embedding[0], 1.f);
        }
    }
    Y_UNIT_TEST(TPartitionedYamlModel) {
        TPartitionedModel model("UserNamespaces_3", true);
        TFileInput inputData("input_table");
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);

        while (parser.Parse()) {
            auto mapNode = currentNode.AsList().front();
            auto profile = GetProfile(mapNode);
            const auto& userEntity = NTorchV2::TUser(profile, 1615282842);
            auto embedding = model.CalculateModel(userEntity.GetNamedFeatures(), userEntity.GetRealvalueFeatures());
            UNIT_ASSERT_EQUAL(embedding.size(), 51ULL);
            UNIT_ASSERT_EQUAL(embedding[0], 1.f);
        }
    }
};
