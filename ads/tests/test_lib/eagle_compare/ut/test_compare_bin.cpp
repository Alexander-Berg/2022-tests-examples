#include <library/cpp/testing/unittest/registar.h>

#include <ads/bsyeti/tests/test_lib/eagle_answers_proto/answers.pb.h>
#include <yabs/proto/user_profile.pb.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/common/env.h>

#include <util/folder/path.h>
#include <util/stream/file.h>
#include <util/stream/fwd.h>


Y_UNIT_TEST_SUITE(CompareBinary) {
    void CreateFileA(TString fileName) {
        if (TFsPath(fileName).IsFile()) {
            return;
        }
        TString jsonContent = R"JSONMSG({
            "Tests": [{
                "TestName": "A",
                "Requests": [
                    {
                        "Key": "bigb-fast.yandex.ru/bigb?client=admetrica-server&bigb-uid=1&format=protobuf",
                        "Answer": "filled later"
                    },
                    {
                        "Key": "bigb-fast.yandex.ru/bigb?client=admetrica-server&bigb-uid=123&format=protobuf",
                        "Answer": "filled later"
                    },
                    {
                        "Key": "bigb-fast.yandex.ru/bigb?client=admetrica-server&bigb-uid=1234&format=protobuf",
                        "Answer": "filled later"
                    }
                ]
            }]
        })JSONMSG";
        NTestsResult::TTests testPack;
        testPack = NProtobufJson::Json2Proto<NTestsResult::TTests>(jsonContent);
        {
            yabs::proto::Profile profile;
            profile.mutable_user_identifiers()->SetCryptaId(123);
            TString profileStr;
            Y_PROTOBUF_SUPPRESS_NODISCARD profile.SerializeToString(&profileStr);
            (*(*testPack.MutableTests())[0].MutableRequests())[1].SetAnswer(profileStr);
            (*(*testPack.MutableTests())[0].MutableRequests())[2].SetAnswer(profileStr);
        }
        {
            yabs::proto::Profile profile;
            profile.mutable_user_identifiers()->SetCryptaId(1337);
            TString profileStr;
            Y_PROTOBUF_SUPPRESS_NODISCARD profile.SerializeToString(&profileStr);
            (*(*testPack.MutableTests())[0].MutableRequests())[0].SetAnswer(profileStr);
        }
        TOFStream ofs(fileName);
        testPack.SerializeToArcadiaStream(&ofs);
    }

    void CreateFileB(TString fileName) {
        if (TFsPath(fileName).IsFile()) {
            return;
        }
        TString jsonContent = R"JSONMSG({
            "Tests": [{
                "TestName": "A",
                "Requests": [
                    {
                        "Key": "bigb-fast.yandex.ru/bigb?client=admetrica-server&bigb-uid=1&format=protobuf",
                        "Answer": "filled later"
                    },
                    {
                        "Key": "bigb-fast.yandex.ru/bigb?client=admetrica-server&bigb-uid=123&format=protobuf",
                        "Answer": "filled later"
                    },
                    {
                        "Key": "bigb-fast.yandex.ru/bigb?client=admetrica-server&bigb-uid=1234&format=protobuf",
                        "Answer": "filled later"
                    }
                ]
            }]
        })JSONMSG";
        NTestsResult::TTests testPack;
        testPack = NProtobufJson::Json2Proto<NTestsResult::TTests>(jsonContent);
        {
            yabs::proto::Profile profile;
            profile.mutable_user_identifiers()->SetCryptaId(123);
            TString profileStr;
            Y_PROTOBUF_SUPPRESS_NODISCARD profile.SerializeToString(&profileStr);
            (*(*testPack.MutableTests())[0].MutableRequests())[1].SetAnswer(profileStr);
        }
        {
            yabs::proto::Profile profile;
            profile.mutable_user_identifiers()->SetCryptaId(1337);
            TString profileStr;
            Y_PROTOBUF_SUPPRESS_NODISCARD profile.SerializeToString(&profileStr);
            (*(*testPack.MutableTests())[0].MutableRequests())[0].SetAnswer(profileStr);
            (*(*testPack.MutableTests())[0].MutableRequests())[2].SetAnswer(profileStr);
        }
        TOFStream ofs(fileName);
        testPack.SerializeToArcadiaStream(&ofs);
    }

    Y_UNIT_TEST(ComparePass) {
        CreateFileA("testfile_a");
        int status = system((BuildRoot() + TString{"/ads/bsyeti/tests/test_lib/eagle_compare/bin/compare testfile_a testfile_a"}).data());
        UNIT_ASSERT_EQUAL(WEXITSTATUS(status), 0);
    }
    Y_UNIT_TEST(CompareFail) {
        CreateFileA("testfile_a");
        CreateFileB("testfile_b");
        int status = system((BuildRoot() + TString{"/ads/bsyeti/tests/test_lib/eagle_compare/bin/compare testfile_a testfile_b"}).data());
        UNIT_ASSERT_EQUAL(WEXITSTATUS(status), 1);
    }
    Y_UNIT_TEST(CompareUidsPass) {
        CreateFileA("testfile_a");
        CreateFileB("testfile_b");
        int status = system((BuildRoot() + TString{"/ads/bsyeti/tests/test_lib/eagle_compare/bin/compare testfile_a testfile_b --uid=123 --uid=1"}).data());
        UNIT_ASSERT_EQUAL(WEXITSTATUS(status), 0);
    }
    Y_UNIT_TEST(CompareUidsFail) {
        CreateFileA("testfile_a");
        CreateFileB("testfile_b");
        int status = system((BuildRoot() + TString{"/ads/bsyeti/tests/test_lib/eagle_compare/bin/compare testfile_a testfile_b --uid=1234"}).data());
        UNIT_ASSERT_EQUAL(WEXITSTATUS(status), 1);
    }
}

