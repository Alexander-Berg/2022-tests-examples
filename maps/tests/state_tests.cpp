#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <maps/libs/common/include/exception.h>

#include <mapreduce/yt/util/ypath_join.h>
#include <mapreduce/yt/interface/client.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/state.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(state_tests)
{

Y_UNIT_TEST(basic_test)
{
    const TString STATE_PATH = "//home/state";

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    State::initialize(client, STATE_PATH);

    EXPECT_THROW(
        State::initialize(client, "//home/another_state"),
        maps::Exception
    );

    EXPECT_TRUE(client->Exists(STATE_PATH));

    EXPECT_EQ(State::regionsTablePath(), NYT::JoinYPaths(STATE_PATH, "regions"));
    EXPECT_EQ(State::areasTablePath(), NYT::JoinYPaths(STATE_PATH, "areas"));
    EXPECT_EQ(State::roadsTablePath(), NYT::JoinYPaths(STATE_PATH, "roads"));
    EXPECT_EQ(State::buildingsTablePath(), NYT::JoinYPaths(STATE_PATH, "blds"));
    EXPECT_EQ(State::dwellplacesTablePath(), NYT::JoinYPaths(STATE_PATH, "dwellplaces"));
    EXPECT_EQ(State::cellsTablePath(), NYT::JoinYPaths(STATE_PATH, "cells"));
    EXPECT_EQ(State::detectionTablePath(), NYT::JoinYPaths(STATE_PATH, "detection"));
    EXPECT_EQ(State::validationTablePath(), NYT::JoinYPaths(STATE_PATH, "validation"));
    EXPECT_EQ(State::notRejectedTablePath(), NYT::JoinYPaths(STATE_PATH, "not_rejected"));

    TString tempPath;
    {
        NYT::TTempTable tmpTable = State::getTempTable(client);
        tempPath = tmpTable.Name();
        EXPECT_TRUE(client->Exists(tempPath));
        std::vector<TString> tablesPath;
        for (const NYT::TNode& tableName : client->List(NYT::JoinYPaths(STATE_PATH, "tmp"))) {
            tablesPath.push_back(
                NYT::JoinYPaths(NYT::JoinYPaths(STATE_PATH, "tmp"), tableName.AsString())
            );
        }
        EXPECT_EQ(tablesPath.size(), 1u);
        EXPECT_EQ(tempPath, tablesPath.front());
    }
    EXPECT_TRUE(!client->Exists(tempPath));

    State::remove(client);

    EXPECT_TRUE(!client->Exists(STATE_PATH));
}

} // Y_UNIT_TEST_SUITE(state_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
