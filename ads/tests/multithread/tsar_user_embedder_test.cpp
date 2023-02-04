#include <mapreduce/yt/common/node_builder.h>
#include <library/cpp/yson/parser.h>

#include <ads/pytorch/deploy/lib/tsar_user_embedder.h>
#include <ads/pytorch/deploy/model_builder_lib/partitioned_model.h>
#include <ads/pytorch/deploy/lib/tsar_computer.h>

#include <thread>

#include <library/cpp/testing/unittest/registar.h>

#include <util/stream/file.h>

using namespace NPytorchTransport;

namespace {
    NBSYeti::TBigbPublicProto GetProfile(const NYT::TNode& node) {
        NBSYeti::TBigbPublicProto profile;
        const auto& profileDump = node["ProfileDump"];
        if (!profileDump.IsNull()) {
            Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString(profileDump.AsString());
        }
        return profile;
    }

    TVector<NBSYeti::TBigbPublicProto> ReadProfiles(const TString& inputPath) {
        TVector<NBSYeti::TBigbPublicProto> profiles;
        TFileInput inputData(inputPath);
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);

        while (parser.Parse()) {
            auto mapNode = currentNode.AsList().front();
            profiles.emplace_back(GetProfile(mapNode));
        }

        return profiles;
    }

    void TestUserEmbedder(const TTsarUserEmbedder& userEmbedder, const TVector<NBSYeti::TBigbPublicProto>& profiles) {
        TVector<std::thread> threads;
        auto calculateEmbedding = [&userEmbedder, &profiles]() {
            for (const auto& profile : profiles) {
                auto embed1 = userEmbedder.Call(profile);
                UNIT_ASSERT_EQUAL(embed1.size(), 51ULL);
                UNIT_ASSERT_EQUAL(embed1[0], 1.f);
            }
        };

        for (size_t i = 0; i < 10; ++i) {
            threads.emplace_back(std::thread(calculateEmbedding));
        }

        for (auto& thr : threads) {
            thr.join();
        }
    }
}

Y_UNIT_TEST_SUITE(TTsarUserEmbedderMultithreadingTest) {

    Y_UNIT_TEST(EigenMultithreadTest) {
        auto computerPtr = MakeHolder<TTsarComputer>(new TPartitionedModel("UserNamespaces_2", false));
        TTsarUserEmbedder userEmbedder(std::move(computerPtr));
        auto profiles = ReadProfiles("input_table");
        TestUserEmbedder(userEmbedder, profiles);
    }
};
