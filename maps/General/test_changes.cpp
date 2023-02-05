#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/changes.h>

#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

namespace maps::sprav::callcenter::company_changes::tests {

TEST(ChangesTest, ExtractChanges) {
    std::vector<proto::Request> requests = {
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_1.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_2.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_3.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_4.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_5.pb.txt")),
    };

    NSpravTDS::Company company = test_helpers::protoFromTextFormat<NSpravTDS::Company>(
        NResource::Find("data/company.pb.txt")
    );

    auto changes = extractChanges(requests, company);

    EXPECT_EQ(changes.name.size(), 7u);
    EXPECT_EQ(changes.address.size(), 1u);
    EXPECT_EQ(changes.chain.size(), 1u);
    EXPECT_EQ(changes.duplicateCompanyId.size(), 1u);
    EXPECT_EQ(changes.email.size(), 2u);
    EXPECT_EQ(changes.feature.size(), 1u);
    EXPECT_EQ(changes.inn.size(), 1u);
    EXPECT_EQ(changes.ogrn.size(), 1u);
    EXPECT_EQ(changes.phone.size(), 3u);
    EXPECT_EQ(changes.publishingStatus.size(), 1u);
    EXPECT_EQ(changes.rubric.size(), 1u);
    EXPECT_EQ(changes.url.size(), 12u);
    EXPECT_EQ(changes.workingTime.size(), 1u);
}

TEST(ChangesTest, MergeChanges) {
    std::vector<proto::Request> requests = {
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_1.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_2.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_3.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_4.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_5.pb.txt")),
    };

    NSpravTDS::Company company = test_helpers::protoFromTextFormat<NSpravTDS::Company>(
        NResource::Find("data/company.pb.txt")
    );

    auto changes = extractChanges(requests, company);
    auto result = mergeChanges(changes);

    NSpravTDS::Company expected = test_helpers::protoFromTextFormat<NSpravTDS::Company>(
        NResource::Find("data/expected_merged_changes.pb.txt")
    );

    EXPECT_THAT(result, NGTest::EqualsProto(expected));
}

TEST(ChangesTest, MergeRequests) {
    std::vector<proto::Request> requests = {
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_1.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_2.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_3.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_4.pb.txt")),
        test_helpers::protoFromTextFormat<proto::Request>(NResource::Find("data/request_5.pb.txt")),
    };
    auto result = mergeRequests(requests);

    NSpravTDS::Company expected = test_helpers::protoFromTextFormat<NSpravTDS::Company>(
        NResource::Find("data/expected_merged_requests.pb.txt")
    );

    EXPECT_THAT(result, NGTest::EqualsProto(expected));
}

} // maps::sprav::callcenter::company_changes::tests
