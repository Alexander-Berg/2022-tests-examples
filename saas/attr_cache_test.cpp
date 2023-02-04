#include "attr_cache.h"

#include <mapreduce/yt/interface/client.h>
#include <library/cpp/yson/node/node_io.h>

#include <mapreduce/yt/library/mock_client/yt_mock.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using testing::_;
using testing::Return;

namespace NYT {
    using TCypressClientMock = NTesting::TClientMock;
}

namespace {
    const TString TestAttr = "row_count";
    const TStringBuf TestYson1 = R"yson({
        "old_node"=<"row_count"=5;>#;
        "eternal_node"=<"row_count"=9;>#;
    })yson";

    const TStringBuf TestYson2 = R"yson({
        "eternal_node"=<"row_count"=90;"second_attr"=7;>#;
        "new_node"=<"row_count"=60;>#;
    })yson";

    Y_UNIT_TEST_SUITE(TAttributeCacheSuite) {
        Y_UNIT_TEST(Exists_NoLifetime) {
            const TString testFolder = "//some/folder";
            const TString testPath = testFolder + "/eternal_node";
            NYT::TCypressClientMock cypress;
            EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson1)));

            NSaas::TAttributeCache cache(100);
            auto result = cache.Get(cypress, testPath, TestAttr);
            ASSERT_NE(result, nullptr);
            ASSERT_EQ(result->AsInt64(), 9);

            auto result2 = cache.Get(cypress, testPath, TestAttr);
            ASSERT_NE(result2, nullptr);
            ASSERT_EQ(result2->AsInt64(), result->AsInt64());
        }

        Y_UNIT_TEST(Exists_Lifetime) {
            const TString testFolder = "//some/folder";
            const TString testPath = testFolder + "/eternal_node";
            NYT::TCypressClientMock cypress;
            {
                testing::InSequence s;
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson1)));
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson2)));
            }

            NSaas::TAttributeCache cache(100);
            auto result = cache.Get(cypress, testPath, TestAttr);
            ASSERT_NE(result, nullptr);
            ASSERT_EQ(result->AsInt64(), 9);

            auto result2 = cache.Get(cypress, testPath, TestAttr, TDuration::Days(1));
            ASSERT_NE(result2, nullptr);
            ASSERT_EQ(result2->AsInt64(), result->AsInt64());

            Sleep(TDuration::MilliSeconds(1));
            auto result3 = cache.Get(cypress, testPath, TestAttr, TDuration::MicroSeconds(100));
            ASSERT_NE(result3, nullptr);
            ASSERT_EQ(result3->AsInt64(), 90);
        }

        Y_UNIT_TEST(Exists_NewAttr) {
            const TString testFolder = "//some/folder";
            const TString testPath = testFolder + "/eternal_node";
            NYT::TCypressClientMock cypress;
            {
                testing::InSequence s;
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson1)));
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson2)));
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson2)));
            }

            NSaas::TAttributeCache cache(100);
            auto result = cache.Get(cypress, testPath, TestAttr);
            ASSERT_NE(result, nullptr);
            ASSERT_EQ(result->AsInt64(), 9);

            auto result2 = cache.Get(cypress, testPath, "second_attr");
            ASSERT_NE(result2, nullptr);
            ASSERT_EQ(result2->AsInt64(), 7);

            Sleep(TDuration::MilliSeconds(1));
            auto result3 = cache.Get(cypress, testPath, "unknown_attr");
            ASSERT_EQ(result3, nullptr);
        }

        Y_UNIT_TEST(Exists_Removed) {
            const TString testFolder = "//some/folder";
            const TString testPath = testFolder + "/old_node";
            NYT::TCypressClientMock cypress;
            {
                testing::InSequence s;
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson1)));
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson2)));
            }

            NSaas::TAttributeCache cache(100);
            auto result = cache.Get(cypress, testPath, TestAttr);
            ASSERT_NE(result, nullptr);
            ASSERT_EQ(result->AsInt64(), 5);

            auto result2 = cache.Get(cypress, testPath, TestAttr);
            ASSERT_NE(result2, nullptr);
            ASSERT_EQ(result2->AsInt64(), result->AsInt64());

            Sleep(TDuration::MilliSeconds(1));
            auto result3 = cache.Get(cypress, testPath, TestAttr, TDuration::MicroSeconds(100));
            ASSERT_EQ(result3, nullptr);
        }

        Y_UNIT_TEST(Removed_Exists) {
            const TString testFolder = "//some/folder";
            const TString testPath = testFolder + "/new_node";
            NYT::TCypressClientMock cypress;
            {
                testing::InSequence s;
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson1)));
                EXPECT_CALL(cypress, Get(testFolder, _)).WillOnce(Return(NYT::NodeFromYsonString(TestYson2)));
            }

            NSaas::TAttributeCache cache(100);
            auto result = cache.Get(cypress, testPath, TestAttr);
            ASSERT_EQ(result, nullptr);

            auto result2 = cache.Get(cypress, testPath, TestAttr);
            ASSERT_EQ(result2, nullptr);

            Sleep(TDuration::MilliSeconds(1));
            auto result3 = cache.Get(cypress, testPath, TestAttr, TDuration::MicroSeconds(100));
            ASSERT_NE(result3, nullptr);
            ASSERT_EQ(result3->AsInt64(), 60);
        }
    }
}
