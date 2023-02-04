#include <ads/tensor_transport/lib_2/banner.h>
#include <ads/tensor_transport/proto/banner.pb.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/yson/parser.h>
#include <library/cpp/protobuf/yt/yt2proto.h>
#include <mapreduce/yt/common/node_builder.h>

#include <util/generic/algorithm.h>
#include <util/stream/file.h>
#include <util/string/join.h>
#include <util/string/split.h>

TVector<ui64> ParseNumbersFromLine(const TString& line) {
    TVector<ui64> result;
    for (const auto& p : StringSplitter(line).Split('\t')) {
        result.emplace_back(FromString<ui64>(p.Token()));
    }
    Sort(result);
    return result;
}

TVector<ui64> GetFeatureVector(const NYT::TNode& node) {
    TensorTransport::TBannerRecord record;
    YtNodeToProto(node, record);
    NTsarTransport::TBanner banner(record);
    auto vectorResult = banner.GetFeatures();
    Sort(vectorResult);
    return vectorResult;
}

Y_UNIT_TEST_SUITE(TestBannerEntity) {
    Y_UNIT_TEST(TestSameAsVW) {
        TFileInput inputData("input_table");
        TFileInput canonData("out_canon");
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);
        TString line;
        int lineNumber = 0;
        while (parser.Parse()) {
            canonData.ReadLine(line);
            auto expected = ParseNumbersFromLine(line);

            auto mapNode = currentNode.AsList().front();
            auto processed = GetFeatureVector(mapNode);
            currentNode.Clear();

            TString message = TStringBuilder{}
                    << "\nLine number:" << lineNumber++
                    << "\nExpected: " << JoinSeq("\t", expected)
                    << "\nProcessed: " << JoinSeq("\t", processed);

            UNIT_ASSERT_EQUAL_C(expected, processed, message);
        }
    }
}
