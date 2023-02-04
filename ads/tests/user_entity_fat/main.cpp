#include <ads/tensor_transport/lib_2/user.h>

#include <mapreduce/yt/common/node_builder.h>
#include <library/cpp/yson/parser.h>
#include <library/cpp/protobuf/yt/yt2proto.h>
#include <util/generic/algorithm.h>
#include <util/stream/file.h>
#include <util/string/split.h>
#include <util/string/join.h>
#include <util/generic/hash_set.h>

#include <library/cpp/testing/unittest/registar.h>


TVector<ui64> ParseNumbersFromLine(const TString& line) {
    TVector<ui64> result;
    if (line.size() > 0) {
        for (const auto& p : StringSplitter(line).Split('\t')) {
        result.emplace_back(FromString<ui64>(p.Token()));
        }
        Sort(result);
    }
    return result;
}

TVector<ui64> GetFeatureVector(const NYT::TNode& node) {
    NBSYeti::TBigbPublicProto profile;
    const auto &profileDump = node["ProfileDump"];
    if (!profileDump.IsNull()) {
        Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString(profileDump.AsString());
    } else {
        Y_PROTOBUF_SUPPRESS_NODISCARD profile.ParseFromString("");
    }

    NTsarTransport::TUser user(profile);
    auto vectorResult = user.GetFeatures();

    Sort(vectorResult);

    return vectorResult;
}

Y_UNIT_TEST_SUITE(TestUserEntity) {
    Y_UNIT_TEST(TestSameAsVW) {
        TFileInput inputData("input_table");
        TFileInput canonData("out_canon");
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);
        TString line;
        while (parser.Parse()) {
            canonData.ReadLine(line);
            auto expected = ParseNumbersFromLine(line);
            THashSet<ui64> expectedSet (expected.begin(), expected.end());

            auto mapNode = currentNode.AsList().front();
            auto processed = GetFeatureVector(mapNode);
            THashSet<ui64> processedSet(processed.begin(), processed.end());
            currentNode.Clear();

            TString message = TStringBuilder{}
                << "\nExpected: " << JoinSeq("\t", expected)
                << "\nProcessed: " << JoinSeq("\t", processed);
            for (auto x: expectedSet) {
                if (!processedSet.contains(x)) {
                    Cerr << message << Endl;
                    ythrow yexception() << "Stop";
                }
            }
        }
    }
}
