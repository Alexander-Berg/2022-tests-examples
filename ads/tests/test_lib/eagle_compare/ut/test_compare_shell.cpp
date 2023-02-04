#include <library/cpp/testing/unittest/registar.h>

#include <ads/bsyeti/tests/test_lib/eagle_compare/lib/compare.h>

#include <ads/bsyeti/tests/test_lib/eagle_answers_proto/answers.pb.h>
#include <yabs/proto/user_profile.pb.h>

#include <library/cpp/protobuf/json/json2proto.h>


Y_UNIT_TEST_SUITE(CompareShell) {

    void AddTestPack(NTestsResult::TTests& testPack, const TString testName, const TMap<TString, TString>& testRequests) {
        auto* newTest = testPack.MutableTests()->Add();
        newTest->SetTestName(testName);
        for (const auto& [testKey, testAnswer] : testRequests) {
            yabs::proto::Profile profile = NProtobufJson::Json2Proto<yabs::proto::Profile>(testAnswer);
            TString profileStr;
            Y_PROTOBUF_SUPPRESS_NODISCARD profile.SerializeToString(&profileStr);
            auto* newCase = newTest->MutableRequests()->Add();
            newCase->SetKey(testKey);
            newCase->SetAnswer(profileStr);
        }
    }

    Y_UNIT_TEST(CompareEqualTests) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), true);
    }

    Y_UNIT_TEST(CompareUnorderedEqualTests) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        AddTestPack(testPack1, "b", {{"r2", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 1337
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "b", {{"r2", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 1337
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), true);
    }

    Y_UNIT_TEST(CompareAddedTestFail) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "b", {{"r2", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 1337
            }
        })JSONMSG"}});
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), false);
    }

    Y_UNIT_TEST(CompareDeletedTestFail) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        AddTestPack(testPack1, "b", {{"r2", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 1337
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), false);
    }

    Y_UNIT_TEST(CompareCaseAddedFail) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "a", {
            {"r1", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 123
                }
            })JSONMSG"}, {"r2", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 1337
                }
            })JSONMSG"}
        });
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), false);
    }

    Y_UNIT_TEST(CompareCaseDeletedFail) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {
            {"r1", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 123
                }
            })JSONMSG"},
            {"r2", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 1337
                }
            })JSONMSG"}
        });
        AddTestPack(testPack2, "a", {{"r1", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), false);
    }

    Y_UNIT_TEST(CompareCaseMiddleAddedFail) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {{"r2", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        AddTestPack(testPack2, "a", {
            {"r1", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 1337
                }
            })JSONMSG"},
            {"r2", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 123
                }
            })JSONMSG"}
        });
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), false);
    }

    Y_UNIT_TEST(CompareCaseMiddleDeletedFail) {
        NTestsResult::TTests testPack1;
        NTestsResult::TTests testPack2;
        AddTestPack(testPack1, "a", {
            {"r1", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 1337
                }
            })JSONMSG"},
            {"r2", R"JSONMSG({
                "user_identifiers": {
                    "CryptaId": 123
                }
            })JSONMSG"}
        });
        AddTestPack(testPack2, "a", {{"r2", R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG"}});
        UNIT_ASSERT_EQUAL(NBSYeti::CompareAnswers(testPack1, testPack2, {}), false);
    }
}
