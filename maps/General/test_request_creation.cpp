#include <maps/sprav/callcenter/libs/request_creation/create_request.h>
#include <maps/sprav/callcenter/libs/request_creation/creation_data.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>
#include <maps/sprav/callcenter/libs/test_helpers/types.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <maps/sprav/callcenter/proto/request.pb.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_extensions/assertions.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

#include <sprav/lib/testing/mock_server/mock_server.h>


namespace maps::sprav::callcenter::tests {

class RequestCreationTest: public testing::Test {
protected:
    void SetUp() override {
        db.reset(new DbFixture());
        auto tx = db->pool().masterWriteableTransaction();
        setUpTypes(*tx);
        tx->commit();
    }

    static void SetUpTestSuite() {
        spravEditorMock.reset(new NSprav::TMockServerContainer(
            TFsPath(GetWorkPath()).Child("sprav_editor_mock"),
            GetOutputPath().Child("sprav_editor_mock"),
            "SPRAV_EDITOR_MOCK_PROXY"
        ));
        spravEditor.reset(new NSprav::TSpravEditorClient(
            "localhost", spravEditorMock->GetPort(), 100, "1120000000036585"
        ));
        geobase.reset(new NGeobase::TLookup(BinaryPath("geobase/data/v6/geodata6.bin")));
    }

    static void TearDownTestSuite() {
        spravEditorMock.reset();
        spravEditor.reset();
        geobase.reset();
    }

    std::string getActualizationRequestType(uint64_t permalink, const std::string& baseType) {
        request_creation::ActualizationData data;
        data.actualization.set_permalink(permalink);
        data.baseTypeId = baseType;
        return makeRequest(data, *spravEditor, *geobase, db->pool()).type();
    }

    std::unique_ptr<DbFixture> db;

    static std::unique_ptr<NSprav::TMockServerContainer> spravEditorMock;
    static std::unique_ptr<NSprav::TSpravEditorClient> spravEditor;
    static std::unique_ptr<NGeobase::TLookup> geobase;
};

std::unique_ptr<NSprav::TMockServerContainer> RequestCreationTest::spravEditorMock = nullptr;
std::unique_ptr<NSprav::TSpravEditorClient> RequestCreationTest::spravEditor = nullptr;
std::unique_ptr<NGeobase::TLookup> RequestCreationTest::geobase = nullptr;

TEST_F(RequestCreationTest, makeActualizationRequest) {
    request_creation::ActualizationData data;
    data.actualization.set_comment("test_comment");
    data.actualization.set_permalink(1108632133);
    data.actualization.set_time_receive(0);
    data.actualization.set_user_id(1);
    data.oidSpace = NSprav::OriginalIdSpace::CC_OIS_SUNDUK;
    data.baseTypeId = "actualization";
    data.priority = 2;

    auto request = makeRequest(data, *spravEditor, *geobase, db->pool());
    auto expectedRequest = test_helpers::protoFromTextFormat<proto::Request>(R"(
        priority: 2
        actualization {
          permalink: 1108632133
          user_id: 1
          comment: "test_comment"
        }
        type: "actualization"
        type_priority: 0
        oid_space: CC_OIS_SUNDUK
        original_id: ""
        user_id: 1
        receive_time: 0
        permalink: 1108632133
        belongs_to_chain: true
        chain_id: 61730
        related_phones: "+7 (499) 734-33-09"
        tz_offset: 180
    )");
    EXPECT_THAT(request, NGTest::EqualsProto(expectedRequest));
}

TEST_F(RequestCreationTest, createActuliationRequestSuccess) {
    request_creation::ActualizationData data;
    data.actualization.set_comment("test_comment");
    data.actualization.set_permalink(1108632133);
    data.actualization.set_time_receive(0);
    data.actualization.set_user_id(1);
    data.baseTypeId = "actualization";
    data.priority = 2;

    auto creationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_TRUE(creationResult.has_created_request_id());
}

TEST_F(RequestCreationTest, createActuliationRequestError) {
    request_creation::ActualizationData data;
    data.actualization.set_comment("test_comment");
    data.actualization.set_permalink(0);
    data.actualization.set_time_receive(0);
    data.actualization.set_user_id(1);
    data.baseTypeId = "actualization";
    data.priority = 2;

    auto creationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_TRUE(creationResult.has_error_message());
}

TEST_F(RequestCreationTest, actualizationRequestType) {
    EXPECT_EQ(getActualizationRequestType(1108632133, "actualization"), "actualization");
    EXPECT_EQ(getActualizationRequestType(1108632133, "auto_close"), "auto_close_for_chain");
    EXPECT_EQ(getActualizationRequestType(1014707470, "auto_close"), "auto_close");
}

TEST_F(RequestCreationTest, makeFeedbackRequest) {
    request_creation::FeedbackData data;
    google::protobuf::TextFormat::ParseFromString(NResource::Find("feedback_requests/feedback_update.pb.txt"), &data.feedback);
    auto request = makeRequest(data, *spravEditor, *geobase, db->pool());
    auto expectedRequest = test_helpers::protoFromTextFormat<proto::Request>(R"(
        priority: 0
        type: "feedback_update"
        type_priority: 0
        oid_space: CC_OIS_FEEDBACK
        original_id: "892719693"
        receive_time: 1645731622234
        permalink: 77058537833
        user_id: 240594218
        related_phones: "+7 (987) 213-33-05"
        tz_offset: 180
    )");

    expectedRequest.mutable_feedback()->CopyFrom(data.feedback);
    EXPECT_THAT(request, NGTest::EqualsProto(expectedRequest));
}

TEST_F(RequestCreationTest, createFeedbackRequestSuccess) {
    request_creation::FeedbackData data;
    google::protobuf::TextFormat::ParseFromString(NResource::Find("feedback_requests/feedback_update.pb.txt"), &data.feedback);
    auto creationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_TRUE(creationResult.has_created_request_id());
}

TEST_F(RequestCreationTest, createFeedbackRequestError) {
    request_creation::FeedbackData data;
    google::protobuf::TextFormat::ParseFromString(NResource::Find("feedback_requests/feedback_update.pb.txt"), &data.feedback);
    data.feedback.clear_permalink();
    data.feedback.clear_prepared_changes();
    auto creationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_TRUE(creationResult.has_error_message());
}

TEST_F(RequestCreationTest, feedbackRequestType) {
    NResource::TResources resources;
    NResource::FindMatch("feedback_requests", &resources);
    for (const auto& resource: resources) {
        request_creation::FeedbackData data;
        google::protobuf::TextFormat::ParseFromString(resource.Data, &data.feedback);
        auto type = resource.Key;
        type.AfterPrefix("feedback_requests/", type);
        type.BeforeSuffix(".pb.txt", type);

        EXPECT_EQ(type, makeRequest(data, *spravEditor, *geobase, db->pool()).type());
    }
}

TEST_F(RequestCreationTest, duplicateFeedbacks) {
    request_creation::FeedbackData data;
    google::protobuf::TextFormat::ParseFromString(NResource::Find("feedback_requests/feedback_update.pb.txt"), &data.feedback);
    auto creationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_FALSE(creationResult.has_error_message());
    auto duplicateCreationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_TRUE(duplicateCreationResult.has_collision_request_id());
    EXPECT_EQ(creationResult.created_request_id(), duplicateCreationResult.collision_request_id());
}

TEST_F(RequestCreationTest, tooBigFeedbackContent) {
    request_creation::FeedbackData data;
    google::protobuf::TextFormat::ParseFromString(NResource::Find("feedback_requests/feedback_update.pb.txt"), &data.feedback);
    data.feedback.set_comment(std::string("*", 20_MB));
    auto creationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_PRED_FORMAT2(testing::IsSubstring, "Content is too big", creationResult.error_message());
}

TEST_F(RequestCreationTest, tooBigActualizationContent) {
    request_creation::ActualizationData data;
    data.actualization.set_comment(std::string("*", 20_MB));
    data.actualization.set_permalink(1108632133);
    data.actualization.set_time_receive(0);
    data.actualization.set_user_id(1);
    data.oidSpace = NSprav::OriginalIdSpace::CC_OIS_SUNDUK;
    data.baseTypeId = "actualization";
    data.priority = 2;

    auto creationResult = request_creation::createRequest(data, *spravEditor, *geobase, db->pool());
    EXPECT_PRED_FORMAT2(testing::IsSubstring, "Content is too big", creationResult.error_message());
}

} // maps::sprav::callcenter::tests
