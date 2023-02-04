#include <yandex_io/libs/protobuf_utils/debug.h>

#include <yandex_io/protos/account_storage.pb.h>
#include <yandex_io/protos/quasar_proto.pb.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <string>

using namespace quasar;

Y_UNIT_TEST_SUITE(DebugString) {

    Y_UNIT_TEST(TestAuthTokenQuasarMessage) {
        const std::string token{"some_nonempty_token"};
        const std::string maskedToken{"auth_token: \"********\""};

        proto::QuasarMessage message;
        message.mutable_auth_token_update_request()->set_auth_token(TString(token));
        message.mutable_refresh_user_event()->set_auth_token(TString(token));

        const auto stringMessage{shortUtf8DebugString(message)};

        EXPECT_EQ(stringMessage.find(token), std::string::npos);
        EXPECT_NE(stringMessage.find(maskedToken), std::string::npos);
    }

    Y_UNIT_TEST(TestAuthTokenAccountInfo) {
        const std::string token{"some_nonempty_token"};
        const std::string maskedToken{"auth_token: \"********\""};

        proto::AccountInfo message;
        message.set_id(1);
        message.set_auth_token(TString(token));

        const auto stringMessage{shortUtf8DebugString(message)};

        EXPECT_EQ(stringMessage.find(token), std::string::npos);
        EXPECT_NE(stringMessage.find(maskedToken), std::string::npos);
    }

    Y_UNIT_TEST(TestXTokenAddUserResponse) {
        const std::string token{"some_nonempty_token"};
        const std::string maskedToken{"xtoken: \"********\""};

        proto::AddUserResponse message;
        message.set_status(proto::AddUserResponse_Status_OK);
        message.set_xtoken(TString(token));

        const auto stringMessage{shortUtf8DebugString(message)};

        EXPECT_EQ(stringMessage.find(token), std::string::npos);
        EXPECT_NE(stringMessage.find(maskedToken), std::string::npos);
    }

    Y_UNIT_TEST(TestOAuthTokenIOControlCommand) {
        const std::string token{"some_nonempty_token"};
        const std::string maskedToken{"oauth_token: \"********\""};

        proto::IOControlCommand message;
        message.mutable_provide_user_account_info()->set_oauth_token(TString(token));

        const auto stringMessage{shortUtf8DebugString(message)};

        EXPECT_EQ(stringMessage.find(token), std::string::npos);
        EXPECT_NE(stringMessage.find(maskedToken), std::string::npos);
    }

    Y_UNIT_TEST(TestConvertMessageToDeepJsonString) {
        proto::QuasarMessage message;
        message.mutable_log_message()->set_level(proto::LogMessage::INFO);
        message.mutable_log_message()->set_timestamp_ms(555);
        message.mutable_log_message()->set_msg(R"({"id":666, "loop": "disperser"})");

        message.mutable_xiva_subscriptions()->add_xiva_subscriptions("text1");
        message.mutable_xiva_subscriptions()->add_xiva_subscriptions("text2");
        message.mutable_xiva_subscriptions()->add_xiva_subscriptions(R"({"text":"undefined3"})");
        message.mutable_xiva_subscriptions()->add_xiva_subscriptions("text4");

        auto text = convertMessageToDeepJsonString(message);
        UNIT_ASSERT_VALUES_EQUAL(text, "{\"logMessage\":{\"level\":\"INFO\",\"msg\":{\"_\":{\"id\":666,\"loop\":\"disperser\"}},\"timestampMs\":\"555\"},\"xivaSubscriptions\":{\"xivaSubscriptions\":[\"text1\",\"text2\",{\"_\":{\"text\":\"undefined3\"}},\"text4\"]}}");
    }

    Y_UNIT_TEST(TestConvertJsonToMessageOk) {
        proto::QuasarMessage message;
        message.mutable_log_message()->set_level(proto::LogMessage::INFO);
        message.mutable_log_message()->set_timestamp_ms(555);

        auto json = convertMessageToJson(message);
        UNIT_ASSERT(json);

        auto pb = convertJsonToProtobuf<proto::QuasarMessage>(jsonToString(*json));
        UNIT_ASSERT(pb);
        UNIT_ASSERT(pb->has_log_message());
        UNIT_ASSERT_VALUES_EQUAL((int)pb->log_message().level(), (int)message.log_message().level());
        UNIT_ASSERT_VALUES_EQUAL(pb->log_message().timestamp_ms(), message.log_message().timestamp_ms());
    }

    Y_UNIT_TEST(TestConvertJsonToMessageInvalidJson) {
        auto pb = convertJsonToProtobuf<proto::QuasarMessage>("invalid json string");
        UNIT_ASSERT(!pb);
    }

    Y_UNIT_TEST(TestConvertJsonToMessageWrongProtoType) {
        proto::QuasarMessage message;
        message.mutable_log_message()->set_level(proto::LogMessage::INFO);
        message.mutable_log_message()->set_timestamp_ms(555);

        auto json = convertMessageToJson(message);
        UNIT_ASSERT(json);

        auto pb = convertJsonToProtobuf<proto::Audio>(jsonToString(*json));
        UNIT_ASSERT(!pb);
    }
}
