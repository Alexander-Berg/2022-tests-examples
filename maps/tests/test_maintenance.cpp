#include <library/cpp/testing/common/env.h>

#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/updater/config/maintenance.pb.h>
#include <maps/automotive/updater/lib/config.h>
#include <maps/automotive/updater/test_helpers/mocks.h>

#include <maps/libs/common/include/file_utils.h>

namespace maps::automotive::updater {

using Maintenance = Fixture;

TEST_F(Maintenance, UpdateConfigOk)
{
    auto before = std::make_shared<proto::Config>(*config());
    before->mutable_drive()->set_force_updates(false);
    before->mutable_passport()->set_signature_validation(proto::Config::Passport::IF_PRESENT);
    setConfig(before);
    auto res = mockPatch("/config", "drive: {force_updates: true}");
    ASSERT_EQ(res.status, 200);
    before->mutable_drive()->set_force_updates(true);
    before->mutable_passport()->set_signature_validation(proto::Config::Passport::IF_PRESENT);
    PROTO_EQ(*before, *config());

    res = mockPatch("/config", "passport: {signature_validation: NONE}");
    before->mutable_drive()->set_force_updates(true);
    before->mutable_passport()->set_signature_validation(proto::Config::Passport::NONE);
    PROTO_EQ(*before, *config());

    res = mockPatch(
        "/config",
        "drive: {"
        "    force_updates: false"
        "}"
        "passport: {"
        "    signature_validation: IF_PRESENT"
        "}");
    before->mutable_drive()->set_force_updates(false);
    before->mutable_passport()->set_signature_validation(proto::Config::Passport::IF_PRESENT);
    PROTO_EQ(*before, *config());
}

TEST_F(Maintenance, UpdateConfigForbiddenFields)
{
    auto before = std::make_shared<proto::Config>(*config());
    before->mutable_drive()->set_force_updates(false);
    before->mutable_passport()->set_signature_validation(proto::Config::Passport::NONE);
    setConfig(before);
    auto res = mockPatch(
        "/config",
        common::readFileToString(
            SRC_("data/Maintenance__UpdateConfigForbiddenFields_request.prototxt")));
    EXPECT_EQ(res.status, 400);
    PROTO_EQ(*before, *config());
}

namespace {

void checkIfSubtree(
    const NProtoBuf::FieldDescriptor* pField,
    const NProtoBuf::Descriptor* cMessage)
{
    auto cField = cMessage->FindFieldByName(pField->name());
    ASSERT_TRUE(cField != nullptr);
    EXPECT_EQ(cField->type(), pField->type());

    if (auto pMessage = pField->message_type()) {
        for (auto i = 0; i < pMessage->field_count(); ++i) {
            checkIfSubtree(pMessage->field(i), cField->message_type());
        }
    }
}

} // namespace

TEST_F(Maintenance, UpdateConfigSubtree)
{
    auto patch = proto::ConfigPatch::default_instance().descriptor();
    auto config = proto::Config::default_instance().descriptor();

    for (auto i = 0; i < patch->field_count(); ++i) {
        checkIfSubtree(patch->field(i), config);
        EXPECT_TRUE(patch->field(i)->is_optional());
    }
}

TEST_F(Maintenance, GetConfigOk)
{
    auto nonemptyConfig = std::make_shared<proto::Config>(*config());
    nonemptyConfig->mutable_store()->set_url("nonempty_url");
    nonemptyConfig->mutable_store()->set_timeout("42");
    setConfig(nonemptyConfig);
    auto response = mockGet("/config");
    ASSERT_EQ(200, response.status);
    proto::Config receivedConfig;
    parseFromString(response.body, receivedConfig);
    PROTO_EQ(*nonemptyConfig, receivedConfig);
}

TEST_F(Maintenance, UpdateSignatureValidation)
{
    ASSERT_EQ(proto::Config::Passport::IF_PRESENT, config()->passport().signature_validation());

    ASSERT_EQ(200, mockPatch("/config", "passport { signature_validation: REQUIRE }").status);
    ASSERT_EQ(proto::Config::Passport::REQUIRE, config()->passport().signature_validation());

    ASSERT_EQ(200, mockPatch("/config", "passport { signature_validation: NONE }").status);
    ASSERT_EQ(proto::Config::Passport::NONE, config()->passport().signature_validation());

    // no change
    ASSERT_EQ(200, mockPatch("/config", "").status);
    ASSERT_EQ(proto::Config::Passport::NONE, config()->passport().signature_validation());
}

} // namespace maps::automotive::updater
