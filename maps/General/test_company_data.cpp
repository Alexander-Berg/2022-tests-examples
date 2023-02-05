#include "common.h"

#include <maps/sprav/callcenter/libs/task/company_data.h>
#include <maps/sprav/callcenter/libs/task/errors.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>
#include <maps/sprav/callcenter/libs/proto_tools/parsing.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>


namespace maps::sprav::callcenter::task::tests {

using ::testing::ElementsAre;

TEST(CompanyDataTest, Constructor) {
    EXPECT_NO_THROW(PermalinkCompanyData({createRequest(1, 1), createRequest(2, 1)}));
    EXPECT_THROW(PermalinkCompanyData({createRequest(1, 2), createRequest(2, 1)}), ConflictError);
    EXPECT_THROW(PermalinkCompanyData(std::vector<proto::Request>{}), maps::LogicError);
    EXPECT_THROW(CreationCompanyData(createRequest(1, 1)), maps::LogicError);
    EXPECT_NO_THROW(CreationCompanyData(createRequest(1, std::nullopt)));
}

TEST(CompanyDataTest, AddRequests) {
    PermalinkCompanyData companyData(1);

    EXPECT_NO_THROW(
        companyData.addRequest(createRequest(1, 1))
    );
    EXPECT_NO_THROW(
        companyData.addRequest(createRequest(2, 1))
    );

    EXPECT_THROW(
        companyData.addRequest(createRequest(3, 2)),
        ConflictError
    );

    EXPECT_THAT(
        companyData.requests(),
        ElementsAre(NGTest::EqualsProto(createRequest(1, 1)), NGTest::EqualsProto(createRequest(2, 1)))
    );
}

TEST(CompanyDataTest, SetCompanyUpdate) {
    PermalinkCompanyData companyData({createRequest(1, 1), createRequest(2, 1)});

    EXPECT_THROW(
        companyData.setCompanyUpdate(createTaskCompanyUpdate(1, std::nullopt, 2)),
        ConflictError
    );

    EXPECT_NO_THROW(
        companyData.setCompanyUpdate(createTaskCompanyUpdate(1, std::nullopt, 1))
    );

    EXPECT_THAT(
        companyData.companyUpdate(),
        NGTest::EqualsProto(createTaskCompanyUpdate(1, std::nullopt, 1))
    );

    CreationCompanyData creationCompanyData({createRequest(1, std::nullopt)});

    EXPECT_THROW(
        creationCompanyData.setCompanyUpdate(createTaskCompanyUpdate(1, 2, std::nullopt)),
        ConflictError
    );

    EXPECT_NO_THROW(
        creationCompanyData.setCompanyUpdate(createTaskCompanyUpdate(1, 1, std::nullopt))
    );

    EXPECT_THAT(
        creationCompanyData.companyUpdate(),
        NGTest::EqualsProto(createTaskCompanyUpdate(1, 1, std::nullopt))
    );
}

TEST(CompanyDataTest, RefreshCompanyId) {
    PermalinkCompanyData companyData({createRequest(1, 1), createRequest(2, 1)});

    companyData.setCommitId(100);
    EXPECT_TRUE(companyData.commitId().has_value());

    companyData.refreshCommitId();
    EXPECT_FALSE(companyData.commitId().has_value());

    companyData.setCommitId(100);
    companyData.setCompanyUpdate(createTaskCompanyUpdate(1, std::nullopt, 1));
    EXPECT_TRUE(companyData.commitId().has_value());

    companyData.refreshCommitId();
    EXPECT_FALSE(companyData.commitId().has_value());

    companyData.setCommitId(100);
    auto companyUpdate = createTaskCompanyUpdate(1, std::nullopt, 1);
    companyUpdate.mutable_company_update();
    companyData.setCompanyUpdate(companyUpdate);
    EXPECT_TRUE(companyData.commitId().has_value());

    companyData.refreshCommitId();
    EXPECT_TRUE(companyData.commitId().has_value());
}

TEST(CompanyDataTest, TdsCompany) {
    PermalinkCompanyData companyData({createRequest(1, 1), createRequest(2, 1)});
    companyData.setCommitId(100);
    EXPECT_THROW(
        companyData.setTdsCompany(createTdsCompany(10, 100)),
        ConflictError
    );

    EXPECT_THROW(
        companyData.setTdsCompany(createTdsCompany(1, 999)),
        ConflictError
    );

    EXPECT_NO_THROW(companyData.setTdsCompany(createTdsCompany(1, 100)));

    companyData.refreshCommitId();
    EXPECT_NO_THROW(companyData.setTdsCompany(createTdsCompany(1, 999)));
    EXPECT_EQ(companyData.commitId(), 999);
}

TEST(CompanyDataTest, Ordering) {
    auto request1 = createRequest(1, 1);
    request1.set_type_priority(1);
    request1.set_receive_time(10);

    auto request2 = createRequest(2, 1);
    request2.set_type_priority(1);
    request2.set_receive_time(10);

    auto request3 = createRequest(3, 1);
    request3.set_type_priority(1);
    request3.set_receive_time(5);

    auto request4 = createRequest(4, 1);
    request4.set_type_priority(10);
    request4.set_receive_time(5);

    PermalinkCompanyData companyData({request1, request2, request3, request4});
    EXPECT_EQ(companyData.mainRequest().id(), 3u);
}

} // maps::sprav::callcenter::task::tests
