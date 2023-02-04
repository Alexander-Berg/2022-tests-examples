#include <maps/sprav/callcenter/libs/rediffed/rediffed_request.h>

#include <sprav/protos/signal.pb.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <util/generic/is_in.h>

#include <algorithm>
#include <cctype>
#include <filesystem>

namespace maps::sprav::callcenter::rediffed::ddt {

bool featureIsMultiple(uint64_t featureId) {
    return featureId == 1416;
}

const std::vector<std::string> DO_NOT_TEST{
    "address", // duplicated entries in old rediffed
    "phones", // duplicated entries in old rediffed
    "urls", // mismatches with raw values in tests
};

struct TestData {
    TestData(int id, std::string preparedChangesStr, std::string rediffedJsonStr)
            : id(id)
            , rediffedJsonStr(std::move(rediffedJsonStr)) {
        NProtobufJson::Json2Proto(
            TString{preparedChangesStr},
            preparedChanges,
            NProtobufJson::TJson2ProtoConfig().SetEnumValueMode(
                NProtobufJson::TJson2ProtoConfig::EnumValueMode::EnumCaseInsensetive));
    }

    int id;
    NSprav::Company preparedChanges;
    std::string rediffedJsonStr;
};

std::map<int, std::string> filesContentFromDir(const std::string& dir) {
    std::map<int, std::string> result;

    for (const auto& entry : std::filesystem::directory_iterator(dir)) {
        if (entry.is_regular_file()) {
            auto id = std::stoll(entry.path().filename().stem());
            auto content = common::readFileToString(entry.path());

            result.emplace(id, std::move(content));
        }
    }

    return result;
}

std::map<int, TestData> readTestData(const std::string& preparedChangesDir, const std::string& rediffedDir) {
    auto preparedChangesContent = filesContentFromDir(preparedChangesDir);
    auto rediffedContent = filesContentFromDir(rediffedDir);

    std::set<int> ids;
    std::transform(
        preparedChangesContent.begin(),
        preparedChangesContent.end(),
        std::inserter(ids, ids.end()),
        [](auto idContent) { return idContent.first; });

    std::map<int, TestData> result;
    for (const auto id : ids) {
        auto preparedChangesIt = preparedChangesContent.find(id);
        if (preparedChangesIt == preparedChangesContent.end()) {
            continue;
        }

        auto rediffedIt = rediffedContent.find(id);
        if (rediffedIt == rediffedContent.end()) {
            continue;
        }

        result.emplace(id, TestData{id, std::move(preparedChangesIt->second), std::move(rediffedIt->second)});
    }

    return result;
}

std::string filterRediffed(const std::string& input) {
    TStringBuf buf{input};
    NJson::TJsonValue value;
    NJson::ReadJsonTree(buf, &value);

    for (auto& field : value.GetMapSafe()) {
        if (IsIn(DO_NOT_TEST, field.first)) {
            auto& fieldMap = field.second.GetMapSafe();
            fieldMap.at("raw").GetArraySafe().clear();
            fieldMap.at("unified").GetArraySafe().clear();
        }
    };

    return NJson::WriteJson(value, /* formatOutput */ true, /* sortKeys */ true);
}

TEST(RediffedRequestJsonTest, CheckBatch) {
    auto cwd = GetWorkPath();
    auto parentDir = cwd + "/test_data";
    auto preparedChangesDir = parentDir + "/prepared_changes";
    auto rediffedDir = parentDir + "/rediffed";

    auto testsData = readTestData(preparedChangesDir, rediffedDir);
    EXPECT_EQ(testsData.size(), 13u);

    for (const auto& [id, testData] : testsData) {
        auto rediffedRequestActual = rediffedRequestJson(
            testData.preparedChanges, featureIsMultiple);

        EXPECT_EQ(
            filterRediffed(rediffedRequestActual),
            filterRediffed(testData.rediffedJsonStr));
    }
}

} // namespace maps::sprav::callcenter::rediffed::ddt
