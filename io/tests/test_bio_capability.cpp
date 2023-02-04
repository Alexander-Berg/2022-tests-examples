#include "library/cpp/testing/unittest/registar.h"
#include "mock_enrollment_engine.h"

#include <yandex_io/interfaces/auth/mock/auth_provider.h>

#include <yandex_io/libs/base/utils.h>
#include "yandex_io/libs/cryptography/cryptography.h"
#include "yandex_io/libs/cryptography/symmetric.h"
#include "yandex_io/libs/device_cryptography/device_cryptography.h"
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/telemetry/mock/mock_telemetry.h>

#include <yandex_io/sdk/interfaces/directive.h>

#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_capability.h>
#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>

#include <yandex_io/tests/testlib/null_device_state_capability/null_device_state_capability.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>

#include <yandex_io/modules/personalization/bio_capability.h>

#include <yandex_io/tests/testlib/test_callback_queue.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/value.h>

#include <algorithm>
#include <future>
#include <memory>
#include <optional>
#include <string>

using namespace testing;
using namespace quasar;
using namespace YandexIO;

namespace {

    MATCHER_P(BlobEqual, blob, "Check blobs equality") {
        return std::equal(blob.Begin(), blob.End(), arg.Begin(), arg.End());
    }

    MATCHER_P(CheckSemanticFrameName, name, "Check semantic frame name") {
        const auto& request = arg;
        const auto& event = request->getEvent();
        const Json::Value* tsf = findXPath(event, "payload/typed_semantic_frame");

        if (tsf == nullptr) {
            return false;
        }

        return tsf->isMember(name);
    }

    MATCHER_P3(CheckSemanticFrame, frameName, valuePath, value, "Check value in semantic frame") {
        const auto& request = arg;
        const auto& event = request->getEvent();
        const Json::Value* tsf = findXPath(event, "payload/typed_semantic_frame");

        if (tsf == nullptr) {
            return false;
        }

        if (!tsf->isMember(frameName)) {
            return false;
        }

        const Json::Value* tsfValue = findXPath((*tsf)[frameName], valuePath);

        if (tsfValue == nullptr) {
            return false;
        }

        return *tsfValue == value;
    }

    MATCHER_P3(CheckGuestOptions, puid, token, persId, "Check guest options") {
        const auto& request = arg;
        const auto& guestOptions = request->getGuestOptions();

        const std::string_view puidView{puid};
        const std::string_view tokenView{token};
        const std::string_view persIdView{persId};

        if (!puidView.empty() != guestOptions.has_yandexuid()) {
            return false;
        }

        if (!puidView.empty() && guestOptions.yandexuid() != puidView) {
            return false;
        }

        if (!tokenView.empty() != guestOptions.has_oauthtoken()) {
            return false;
        }

        if (!tokenView.empty() && guestOptions.oauthtoken() != tokenView) {
            return false;
        }

        if (!persIdView.empty() != guestOptions.has_persid()) {
            return false;
        }

        if (!persIdView.empty() && guestOptions.persid() != persIdView) {
            return false;
        }

        return true;
    }

    class MockIDeviceStateCapability: public NullDeviceStateCapability {
    public:
        MOCK_METHOD(void, setEnrollmentHeaders, (const NAlice::TEnrollmentHeaders&), (override));
    };

    class MockSDKInterface: public NullSDKInterface {
    public:
        MOCK_METHOD(std::shared_ptr<IAliceCapability>, getAliceCapability, (), (const override));
        MOCK_METHOD(std::shared_ptr<IFilePlayerCapability>, getFilePlayerCapability, (), (const override));
        MOCK_METHOD(std::shared_ptr<IDeviceStateCapability>, getDeviceStateCapability, (), (const override));
    };

    class MockAudioSourceClient: public IAudioSourceClient {
    public:
        MOCK_METHOD(void, subscribeToChannels, (RequestChannelType), ());
        MOCK_METHOD(void, unsubscribeFromChannels, (), ());
        MOCK_METHOD(void, start, (), ());
        MOCK_METHOD(void, addListener, (std::weak_ptr<Listener>));
    };

    class BioCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);

            {
                auto configuration = QuasarUnitTestFixture::makeTestConfiguration();
                auto mockHal = QuasarUnitTestFixture::makeTestHAL();

                deviceCryptography = mockHal->createDeviceCryptography(configuration->getCommonConfig()["cryptography"]);

                auto testDevice = std::make_unique<YandexIO::Device>(
                    QuasarUnitTestFixture::makeTestDeviceId(),
                    std::move(configuration),
                    mockTelemetry,
                    std::move(mockHal));

                setDeviceForTests(std::move(testDevice));
            }

            enableLoggingToTelemetry(mockTelemetry);

            ON_CALL(*mockSDKInterface, getAliceCapability())
                .WillByDefault(Return(mockAliceCapability));
            ON_CALL(*mockSDKInterface, getFilePlayerCapability())
                .WillByDefault(Return(mockFilePlayerCapability));
            ON_CALL(*mockSDKInterface, getDeviceStateCapability())
                .WillByDefault(Return(mockDeviceStateCapability));

            ON_CALL(*mockAudioSourceClient, addListener(_))
                .WillByDefault(Invoke([this](auto listener) {
                    audioListener = std::move(listener);
                }));

            bioConfig = BioConfig{
                .modelPath = "",
                .storagePath = JoinFsPaths(tryGetRamDrivePath(), "bio_storage.json"),
            };

            bioCapability = std::make_shared<YandexIO::BioCapability>(
                getDeviceForTests(),
                mockSDKInterface,
                mockAuthProvider,
                mockAudioSourceClient,
                mockEnrollmentEngine,
                bioConfig,
                testCallbackQueue);

            EXPECT_CALL(*mockAliceCapability, addListener(_));
            mockAliceCapability->addListener(bioCapability);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            TFsPath(bioConfig.storagePath).ForceDelete();

            QuasarUnitTestFixture::TearDown(context);
        }

        void pushChannelsData(const YandexIO::ChannelsData& data) const {
            if (auto listener = audioListener.lock()) {
                listener->onAudioData(data);
            }
        }

        static std::shared_ptr<YandexIO::Directive> createAddOwnerAccountDirective();
        std::shared_ptr<YandexIO::Directive> createAddGuestAccountDirective(const std::string& code, const std::string& tokenType, bool wrongSignature = false) const;

        AuthInfo2 addOwnerUser() const;
        void updateOwnerUser(const TString& id, const TString& version) const;

        AuthInfo2 addGuestUser(const std::string& tokenType) const;
        void startEnrollment(std::optional<Json::Value> timeout = std::nullopt) const;
        void finishEnrollment(const std::string& puid, const std::string& token, const TString& enrollId, const TString& version = "0", bool withFrame = false) const;

    public:
        std::shared_ptr<BioCapability> bioCapability;
        std::shared_ptr<MockSDKInterface> mockSDKInterface = std::make_shared<NiceMock<MockSDKInterface>>();
        std::shared_ptr<MockIAliceCapability> mockAliceCapability = std::make_shared<NiceMock<MockIAliceCapability>>();
        std::shared_ptr<MockIFilePlayerCapability> mockFilePlayerCapability = std::make_shared<StrictMock<MockIFilePlayerCapability>>();
        std::shared_ptr<MockIDeviceStateCapability> mockDeviceStateCapability = std::make_shared<StrictMock<MockIDeviceStateCapability>>();
        std::shared_ptr<mock::AuthProvider> mockAuthProvider = std::make_shared<mock::AuthProvider>();
        std::shared_ptr<MockAudioSourceClient> mockAudioSourceClient = std::make_shared<NiceMock<MockAudioSourceClient>>();
        std::shared_ptr<MockEnrollmentEngine> mockEnrollmentEngine = std::make_shared<StrictMock<MockEnrollmentEngine>>();
        std::shared_ptr<MockTelemetry> mockTelemetry = std::make_shared<StrictMock<MockTelemetry>>();
        std::shared_ptr<TestCallbackQueue> testCallbackQueue = std::make_shared<TestCallbackQueue>();
        std::shared_ptr<DeviceCryptography> deviceCryptography;
        std::weak_ptr<IAudioSourceClient::Listener> audioListener;
        BioConfig bioConfig;
    };

    std::shared_ptr<YandexIO::Directive> BioCapabilityFixture::createAddOwnerAccountDirective() {
        Json::Value directive;
        directive["name"] = "alice.multiaccount.add_account";
        directive["payload"]["user_type"] = "OWNER";

        const auto directiveData = YandexIO::Directive::Data::fromJson(directive);
        return std::make_shared<YandexIO::Directive>(directiveData, false);
    }

    std::shared_ptr<YandexIO::Directive> BioCapabilityFixture::createAddGuestAccountDirective(
        const std::string& code, const std::string& tokenType, bool wrongSignature) const {
        Cryptography cryptography;
        cryptography.setPublicKey(deviceCryptography->getEncryptPublicKey());

        const auto sessionKey = cryptography.generateAESKeyString();
        const auto encryptedSessionKey = cryptography.encrypt(sessionKey);

        auto aes128 = SymmetricCrypto::makeAES128(sessionKey);
        const auto encryptedCode = aes128->encrypt(code);

        const auto signature = wrongSignature ? "123" : Cryptography::hashWithHMAC_SHA256(encryptedCode, sessionKey);

        Json::Value payload;
        payload["encrypted_session_key"] = base64Encode(encryptedSessionKey.data(), encryptedSessionKey.size());
        payload["encrypted_code"] = base64Encode(encryptedCode.data(), encryptedCode.size());
        payload["signature"] = base64Encode(signature.data(), signature.size());
        payload["token_type"] = tokenType;
        payload["user_type"] = "GUEST";

        Json::Value directive;
        directive["name"] = "alice.multiaccount.add_account";
        directive["payload"] = std::move(payload);

        const auto directiveData = YandexIO::Directive::Data::fromJson(directive);
        return std::make_shared<YandexIO::Directive>(directiveData, false);
    }

    struct UpdateDirectiveJsonBuilder {
        std::optional<Json::Value> enrollment = std::nullopt;
        std::optional<Json::Value> personId = std::nullopt;
        std::optional<Json::Value> userType = std::nullopt;
        std::optional<Json::Value> version = std::nullopt;

        Json::Value build() const {
            Json::Value voiceprint{Json::objectValue};

            {
                Json::Value voiceprintHeader{Json::objectValue};
                if (personId.has_value()) {
                    voiceprintHeader["person_id"] = *personId;
                }
                if (userType.has_value()) {
                    voiceprintHeader["user_type"] = *userType;
                }
                if (version.has_value()) {
                    voiceprintHeader["version"] = *version;
                }
                voiceprint["voiceprint_header"] = std::move(voiceprintHeader);
            }

            if (enrollment.has_value()) {
                voiceprint["enrollment"] = *enrollment;
            }

            Json::Value voiceprints{Json::arrayValue};
            voiceprints.append(std::move(voiceprint));

            Json::Value payload{Json::objectValue};
            payload["voiceprints"] = std::move(voiceprints);

            Json::Value json;
            json["name"] = "update_voice_prints";
            json["payload"] = std::move(payload);
            return json;
        }
    };

    struct MatchDirectiveJsonBuilder {
        std::optional<Json::Value> biometryResult = std::nullopt;

        Json::Value build() const {
            Json::Value payload{Json::objectValue};
            if (biometryResult.has_value()) {
                payload["biometry_result"] = *biometryResult;
            }

            Json::Value json;
            json["name"] = "match_voice_print";
            json["payload"] = std::move(payload);
            return json;
        }
    };

    struct RemoveAccountJsonBuilder {
        std::optional<Json::Value> puid = std::nullopt;

        Json::Value build() const {
            Json::Value payload{Json::objectValue};
            if (puid.has_value()) {
                payload["puid"] = *puid;
            }

            Json::Value json;
            json["name"] = "alice.multiaccount.remove_account";
            json["payload"] = std::move(payload);
            return json;
        }
    };

    struct StartEnrollmentJsonBuilder {
        std::optional<Json::Value> timeout = std::nullopt;

        Json::Value build() const {
            Json::Value payload{Json::objectValue};
            if (timeout.has_value()) {
                payload["timeout_ms"] = *timeout;
            }

            Json::Value json;
            json["name"] = "enrollment_start";
            json["payload"] = std::move(payload);
            return json;
        }
    };

    struct CancelEnrollmentJsonBuilder {
        // NOLINTNEXTLINE(readability-convert-member-functions-to-static)
        Json::Value build() const {
            Json::Value payload{Json::objectValue};

            Json::Value json;
            json["name"] = "enrollment_cancel";
            json["payload"] = std::move(payload);
            return json;
        }
    };

    struct FinishEnrollmentJsonBuilder {
        std::optional<Json::Value> personId = std::nullopt;
        std::optional<Json::Value> withFrame = std::nullopt;

        Json::Value build() const {
            Json::Value payload{Json::objectValue};
            if (personId.has_value()) {
                payload["pers_id"] = *personId;
            }

            if (withFrame.has_value()) {
                payload["send_guest_enrollment_finish_frame"] = *withFrame;
            }

            Json::Value json;
            json["name"] = "enrollment_finish";
            json["payload"] = std::move(payload);
            return json;
        }
    };

    void BioCapabilityFixture::updateOwnerUser(const TString& enrollId, const TString& enrollVersion) const {
        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrollsSuccess", _));
        }

        EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
            .WillOnce(Return(THashMap<TString, TString>{{enrollId, enrollVersion}}));

        EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(
                                                    Property(&NAlice::TEnrollmentHeaders::GetHeaders, Contains(AllOf(
                                                                                                          Property(&NAlice::TEnrollmentHeader::GetPersonId, enrollId),
                                                                                                          Property(&NAlice::TEnrollmentHeader::GetUserType, NAlice::EUserType::OWNER),
                                                                                                          Property(&NAlice::TEnrollmentHeader::GetVersion, enrollVersion))))));

        EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(Contains(Key(enrollId))));

        {
            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .personId = enrollId,
                .userType = "__SYSTEM_OWNER_DO_NOT_USE_AFTER_2021",
                .version = enrollVersion,
            };
            Json::Value json = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    AuthInfo2 BioCapabilityFixture::addOwnerUser() const {
        InSequence sequence;

        EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(IsEmpty()));

        EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
            .WillOnce(Return(THashMap<TString, TString>{}));
        EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
        EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(
                                                    Property(&NAlice::TEnrollmentHeaders::GetHeaders, Contains(AllOf(
                                                                                                          Property(&NAlice::TEnrollmentHeader::GetPersonId, ""),
                                                                                                          Property(&NAlice::TEnrollmentHeader::GetUserType, NAlice::EUserType::OWNER),
                                                                                                          Property(&NAlice::TEnrollmentHeader::GetVersion, ""))))));

        AuthInfo2 owner{
            .authToken = "token",
            .passportUid = "42",
            .userType = UserType::OWNER,
        };

        mockAuthProvider->setOwner(owner);

        return owner;
    }

    AuthInfo2 BioCapabilityFixture::addGuestUser(const std::string& tokenType = "OAuthToken") const {
        AuthInfo2 guest{
            .source = AuthInfo2::Source::AUTHD,
            .authToken = "authToken",
            .passportUid = "666",
            .userType = UserType::GUEST,
        };

        mockAuthProvider->setAddUser([&guest](const std::string& /*parsedCode*/, UserType /*userType*/, bool withXToken, std::chrono::milliseconds /*timeout*/) {
            return IAuthProvider::AddUserResponse{
                .status = IAuthProvider::AddUserResponse::Status::OK,
                .authToken = guest.authToken,
                .xToken = withXToken ? "xToken" : "",
                .id = std::stoll(guest.passportUid),
            };
        });

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioAddAccount", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioAddAccountSuccess", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioSendEnrollmentSemanticFrame", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
        }

        EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
            .WillOnce(Return(false));

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrame("guest_enrollment_start_semantic_frame", "puid/string_value", guest.passportUid),
                CheckGuestOptions(guest.passportUid, guest.authToken, "")), Eq(nullptr)))
            .WillOnce(WithArgs<0>([](const auto& request) {
                EXPECT_FALSE(request->getIsParallel());
                EXPECT_FALSE(request->getIgnoreAnswer());
            }));
        // clang-format on

        const std::string code = "code";
        const auto addGuestDirective = createAddGuestAccountDirective(code, tokenType);
        bioCapability->handleDirective(addGuestDirective);

        EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(_));
        EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
            .WillOnce(Return(THashMap<TString, TString>{}));

        EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(_));

        mockAuthProvider->addGuest(guest);

        return guest;
    }

    void BioCapabilityFixture::startEnrollment(std::optional<Json::Value> timeout) const {
        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(false));
            EXPECT_CALL(*mockEnrollmentEngine, StartEnrollment());
        }

        {
            testing::InSequence sequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentStart", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentStartSuccess", _, _));
        }

        const auto delayedCallbackCount = testCallbackQueue->delayedCallbackCount();

        {
            const auto json = StartEnrollmentJsonBuilder{.timeout = std::move(timeout)}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->delayedCallbackCount(), delayedCallbackCount + 1);
    }

    void BioCapabilityFixture::finishEnrollment(const std::string& puid, const std::string& token, const TString& enrollId, const TString& enrollVersion, bool withFrame) const {
        const auto voiceprint = TBlob::FromString("voiceprint");

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(true));

            EXPECT_CALL(*mockEnrollmentEngine, CommitEnrollment())
                .WillOnce(Return(voiceprint));

            EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(Contains(Key(enrollId))));

            EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
                .WillOnce(Return(THashMap<TString, TString>{{enrollId, enrollVersion}}));

            // clang-format off
            EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(
                Property(&NAlice::TEnrollmentHeaders::GetHeaders,
                    Contains(AllOf(
                        Property(&NAlice::TEnrollmentHeader::GetPersonId, enrollId),
                        Property(&NAlice::TEnrollmentHeader::GetUserType, NAlice::EUserType::GUEST),
                        Property(&NAlice::TEnrollmentHeader::GetVersion, enrollVersion))))));
            // clang-format on
        }

        {
            testing::InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentFinish", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentFinishSuccess", _));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(puid, token, enrollId)), Eq(nullptr)));
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("guest_enrollment_finish_semantic_frame"),
                CheckGuestOptions(puid, token, enrollId)), Eq(nullptr)))
            .Times(withFrame);
        // clang-format on

        {
            const auto json = FinishEnrollmentJsonBuilder{.personId = enrollId, .withFrame = withFrame}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

} // namespace

Y_UNIT_TEST_SUITE(BioCapabilityTest) {
    Y_UNIT_TEST_F(testCreation, BioCapabilityFixture) {
    }

    Y_UNIT_TEST_F(testAddGuest_OAuthToken, BioCapabilityFixture) {
        addGuestUser();
    }

    Y_UNIT_TEST_F(testAddGuest_XToken, BioCapabilityFixture) {
        addGuestUser("XToken");
    }

    Y_UNIT_TEST_F(testAddGuest_UnknownToken, BioCapabilityFixture) {
        const std::string code = "test_wrong_code";
        const std::string tokenType = "WTFToken";

        EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
            .WillOnce(Return(false));

        InSequence eventsSequence;
        EXPECT_CALL(*mockTelemetry, reportEvent("bioAddAccount", _));
        EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr(tokenType), _, _, HasSubstr("BioCapability.AddAccountFailed")));
        EXPECT_CALL(*mockTelemetry, reportError("bioAddAccountFailed", _, _));

        EXPECT_CALL(*mockFilePlayerCapability, playSoundFile("guest_enrollment_failed.mp3", _, _, _));

        const auto addGuestDirective = createAddGuestAccountDirective(code, tokenType);
        bioCapability->handleDirective(addGuestDirective);
    }

    Y_UNIT_TEST_F(testAddGuest_WrongSignature, BioCapabilityFixture) {
        const std::string code = "test_wrong_code";
        const std::string tokenType = "OAuthToken";

        EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
            .WillOnce(Return(false));

        InSequence eventsSequence;
        EXPECT_CALL(*mockTelemetry, reportEvent("bioAddAccount", _));
        EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Wrong signature"), _, _, HasSubstr("BioCapability.AddAccountFailed")));
        EXPECT_CALL(*mockTelemetry, reportError("bioAddAccountFailed", _, _));

        EXPECT_CALL(*mockFilePlayerCapability, playSoundFile("guest_enrollment_failed.mp3", _, _, _));

        const bool wrongSignature = true;
        const auto addGuestDirective = createAddGuestAccountDirective(code, tokenType, wrongSignature);
        bioCapability->handleDirective(addGuestDirective);
    }

    Y_UNIT_TEST_F(testAddGuest_FailedAddGuest, BioCapabilityFixture) {
        const std::string code = "test_wrong_code";
        const std::string tokenType = "OAuthToken";

        mockAuthProvider->setAddUser([](const std::string& /*parsedCode*/, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
            return IAuthProvider::AddUserResponse{
                .status = IAuthProvider::AddUserResponse::Status::CODE_EXPIRED,
                .authToken = "authToken",
                .xToken = "",
                .id = 42,
            };
        });

        EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
            .WillOnce(Return(false));

        InSequence eventsSequence;
        EXPECT_CALL(*mockTelemetry, reportEvent("bioAddAccount", _));
        EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("AddUser failed"), _, _, HasSubstr("BioCapability.AddAccountFailed")));
        EXPECT_CALL(*mockTelemetry, reportError("bioAddAccountFailed", _, _));

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions("", "", "")), Eq(nullptr)));
        // clang-format on

        EXPECT_CALL(*mockFilePlayerCapability, playSoundFile("guest_enrollment_failed.mp3", _, _, _));

        const auto addGuestDirective = createAddGuestAccountDirective(code, tokenType);
        bioCapability->handleDirective(addGuestDirective);
    }

    Y_UNIT_TEST_F(testAddGuest_CheckAddGuestTimeoutCallback, BioCapabilityFixture) {
        const auto guest = addGuestUser();

        mockAuthProvider->setDeleteUser([&guest](int64_t id, std::chrono::milliseconds timeout) {
            UNIT_ASSERT_VALUES_EQUAL(guest.passportUid, std::to_string(id));
            UNIT_ASSERT_VALUES_EQUAL(10000, timeout.count());

            return IAuthProvider::DeleteUserResponse{
                .status = IAuthProvider::DeleteUserResponse::Status::OK};
        });

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Add account timeout"), _, _, HasSubstr("BioCapability.AddAccountTimeout")));
            EXPECT_CALL(*mockTelemetry, reportError("bioAddAccountTimeout", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
        }

        {
            testing::InSequence sequence;
            EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(IsEmpty()));
            EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
                .WillOnce(Return(THashMap<TString, TString>{}));
            EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(_));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(guest.passportUid, guest.authToken, "")), Eq(nullptr)));
        // clang-format on

        // AddGuestTimeout callback
        UNIT_ASSERT_VALUES_EQUAL(1, testCallbackQueue->delayedCallbackCount());
        UNIT_ASSERT_VALUES_EQUAL(600000, testCallbackQueue->firstDelayedCallbackTimeout().count());

        testCallbackQueue->pumpDelayedCallback();
    }

    Y_UNIT_TEST_F(testAddOwner, BioCapabilityFixture) {
        const auto owner = addOwnerUser();

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioAddAccount", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioAddAccountSuccess", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioSendEnrollmentSemanticFrame", _, _));
        }

        EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
            .WillOnce(Return(false));

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrame("guest_enrollment_start_semantic_frame", "puid/string_value", owner.passportUid),
                CheckGuestOptions(owner.passportUid, owner.authToken, "")), Eq(nullptr)))
            .WillOnce(WithArgs<0>([](const auto& request) {
                EXPECT_FALSE(request->getIsParallel());
                EXPECT_FALSE(request->getIgnoreAnswer());
            }));
        // clang-format on

        const auto addAccountDirective = createAddOwnerAccountDirective();
        bioCapability->handleDirective(addAccountDirective);
    }

    Y_UNIT_TEST_F(testUpdateVoicePrints_InvalidVoiceprints, BioCapabilityFixture) {
        {
            {
                InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'voiceprints' section is not an array"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));
            }

            Json::Value noVoicePrints = UpdateDirectiveJsonBuilder{}.build();
            noVoicePrints["payload"].removeMember("voiceprints");

            const auto directiveData = YandexIO::Directive::Data::fromJson(noVoicePrints);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            {
                InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'voiceprints' section is not an array"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));
            }

            Json::Value invalidVoicePrints = UpdateDirectiveJsonBuilder{}.build();
            invalidVoicePrints["payload"]["voiceprints"] = Json::Value{123};

            const auto directiveData = YandexIO::Directive::Data::fromJson(invalidVoicePrints);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testUpdateVoicePrints_InvalidEnrollment, BioCapabilityFixture) {
        {
            {
                InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'enrollment' is empty"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));
            }

            Json::Value noEnrollment = UpdateDirectiveJsonBuilder{}.build();
            noEnrollment["payload"]["voiceprints"][0].removeMember("enrollment");

            const auto directiveData = YandexIO::Directive::Data::fromJson(noEnrollment);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            {
                InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'enrollment' is empty"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));
            }

            const auto builder = UpdateDirectiveJsonBuilder{.enrollment = ""};
            Json::Value emptyEnrollment = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(emptyEnrollment);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            {
                InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'enrollment' is empty"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));
            }

            const auto builder = UpdateDirectiveJsonBuilder{.enrollment = 123};
            Json::Value invalidEnrollment = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(invalidEnrollment);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testUpdateVoicePrints_InvalidVoiceprintHeader, BioCapabilityFixture) {
        {
            {
                InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'voiceprint_header' section is not an object"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));
            }

            const auto builder = UpdateDirectiveJsonBuilder{.enrollment = "abc"};
            Json::Value noVoiceprintHeader = builder.build();
            noVoiceprintHeader["payload"]["voiceprints"][0].removeMember("voiceprint_header");

            const auto directiveData = YandexIO::Directive::Data::fromJson(noVoiceprintHeader);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            {
                InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'voiceprint_header' section is not an object"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));
            }

            const auto builder = UpdateDirectiveJsonBuilder{.enrollment = "abc"};
            Json::Value invalidVoiceprintHeader = builder.build();
            invalidVoiceprintHeader["payload"]["voiceprints"][0]["voiceprint_header"] = 123;

            const auto directiveData = YandexIO::Directive::Data::fromJson(invalidVoiceprintHeader);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testUpdateVoicePrints_InvalidPersonId, BioCapabilityFixture) {
        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'person_id' field is empty"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));

            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .version = "42",
            };
            Json::Value noPersonId = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(noPersonId);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'person_id' field is empty"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));

            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .personId = 123,
                .version = "42",
            };
            Json::Value invalidPersonId = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(invalidPersonId);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'person_id' field is empty"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));

            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .personId = "",
                .version = "42",
            };
            Json::Value emptyPersonId = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(emptyPersonId);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Unknown 'person_id'"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));

            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .personId = "id",
                .version = "42",
            };
            Json::Value unknownPersonId = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(unknownPersonId);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testUpdateVoicePrints_InitialSyncVoiceprintsFromServer, BioCapabilityFixture) {
        addOwnerUser();

        const TString id = "id";
        const TString version = "ver";
        updateOwnerUser(id, version);
    }

    Y_UNIT_TEST_F(testUpdateVoicePrints_InvalidUserType, BioCapabilityFixture) {
        addOwnerUser();

        const TString id = "id";
        const TString version = "ver";
        updateOwnerUser(id, version);

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Invalid 'user_type'"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));

            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .personId = id,
                .version = version,
            };
            Json::Value noUserType = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(noUserType);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Invalid 'user_type'"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));

            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .personId = id,
                .userType = 123,
                .version = version,
            };
            Json::Value invalidUserType = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(invalidUserType);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Invalid 'user_type'"), _, _, HasSubstr("BioCapability.UpdateVoicePrintsFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioUpdateEnrollsFailed", _, _));

            const auto builder = UpdateDirectiveJsonBuilder{
                .enrollment = "abc",
                .personId = id,
                .userType = "GUEST",
                .version = version,
            };
            Json::Value wrongUserType = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(wrongUserType);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testUpdateVoicePrints_FullyCorrect, BioCapabilityFixture) {
        addOwnerUser();

        const TString enrollId = "id";
        const TString enrollVersion1 = "ver1";
        updateOwnerUser(enrollId, enrollVersion1);

        // Change version

        const TString enrollVersion2 = "ver2";
        std::promise<void> waiter;

        {
            InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(_))
                .WillOnce(Invoke([&enrollId](const auto& enrollments) {
                    UNIT_ASSERT(enrollments.contains(enrollId));
                }));

            EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
                .WillOnce(Return(THashMap<TString, TString>{{enrollId, enrollVersion2}}));

            EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(
                                                        Property(&NAlice::TEnrollmentHeaders::GetHeaders, Contains(AllOf(
                                                                                                              Property(&NAlice::TEnrollmentHeader::GetPersonId, enrollId),
                                                                                                              Property(&NAlice::TEnrollmentHeader::GetUserType, NAlice::EUserType::OWNER),
                                                                                                              Property(&NAlice::TEnrollmentHeader::GetVersion, enrollVersion2))))))
                .WillOnce(InvokeWithoutArgs([&waiter] {
                    waiter.set_value();
                }));
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrolls", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioUpdateEnrollsSuccess", _));
        }

        const auto builder = UpdateDirectiveJsonBuilder{
            .enrollment = "abc",
            .personId = enrollId,
            .userType = "OWNER",
            .version = enrollVersion2,
        };
        Json::Value json = builder.build();

        const auto directiveData = YandexIO::Directive::Data::fromJson(json);
        const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
        bioCapability->handleDirective(directive);

        waiter.get_future().wait();
    }

    Y_UNIT_TEST_F(testMatchedUser_InvalidSignature, BioCapabilityFixture) {
        {
            InSequence engineSequence;
            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(false));
            EXPECT_CALL(*mockEnrollmentEngine, StartRequest());
            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillRepeatedly(Return(true));
        }

        {
            proto::AliceState state;
            state.set_state(proto::AliceState::LISTENING);
            bioCapability->onAliceStateChanged(state);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnroll", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'biometry_result' is empty"), _, _, HasSubstr("BioCapability.MatchVoicePrintFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioMatchEnrollFailed", _, _));
        }

        {
            Json::Value noBiometryResult = MatchDirectiveJsonBuilder{}.build();
            noBiometryResult["payload"].removeMember("biometry_result");

            const auto directiveData = YandexIO::Directive::Data::fromJson(noBiometryResult);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnroll", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'biometry_result' is empty"), _, _, HasSubstr("BioCapability.MatchVoicePrintFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioMatchEnrollFailed", _, _));
        }

        {
            const auto builder = MatchDirectiveJsonBuilder{.biometryResult = ""};
            Json::Value emptyBiometryResult = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(emptyBiometryResult);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        {
            InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnroll", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'biometry_result' is empty"), _, _, HasSubstr("BioCapability.MatchVoicePrintFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioMatchEnrollFailed", _, _));
        }

        {
            const auto builder = MatchDirectiveJsonBuilder{.biometryResult = 123};
            Json::Value invalidBiometryResult = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(invalidBiometryResult);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testMatchedUser_MatchVoicePrint, BioCapabilityFixture) {
        const auto owner = addOwnerUser();

        const TString enrollId = "id";
        const TString voiceprintVersion = "ver";
        updateOwnerUser(enrollId, voiceprintVersion);

        {
            InSequence engineSequence;
            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(false));
            EXPECT_CALL(*mockEnrollmentEngine, StartRequest());
        }

        {
            proto::AliceState state;
            state.set_state(::proto::AliceState::LISTENING);
            bioCapability->onAliceStateChanged(state);
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(CheckGuestOptions(owner.passportUid, owner.authToken, enrollId), Eq(nullptr)))
            .WillOnce(WithArgs<0>([](const auto& request) {
                UNIT_ASSERT_EQUAL(request->getEventSource().type(), NAlice::TSpeechKitRequestProto_TEventSource_EType_VoiceprintMatch);
                UNIT_ASSERT(request->getIsParallel());
                UNIT_ASSERT(request->getIsSilent());
                UNIT_ASSERT(request->getIgnoreAnswer());
            }));
        // clang-format on

        const TString biometryResult = "abc";

        // Handle first match directive and send that match has changed

        {
            InSequence enrollmentSequence;
            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockEnrollmentEngine, SetRequestExternalVoiceprint(BlobEqual(TBlob::FromString(biometryResult))));
            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(false));
            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockEnrollmentEngine, GetMatchedEnrollmentID())
                .WillOnce(Return(enrollId));
        }

        {
            InSequence telemetrySequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnroll", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnrollChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnrollSuccess", _));
        }

        {
            const auto builder = MatchDirectiveJsonBuilder{
                .biometryResult = base64Encode(biometryResult.c_str(), biometryResult.size()),
            };
            const Json::Value json = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        // Handle second match directive. Match has not changes - we shouldn't send match directive

        {
            InSequence enrollmentSequence;
            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockEnrollmentEngine, SetRequestExternalVoiceprint(BlobEqual(TBlob::FromString(biometryResult))));
            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(false));
            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockEnrollmentEngine, GetMatchedEnrollmentID())
                .WillOnce(Return(enrollId));
        }

        {
            InSequence telemetrySequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnroll", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnrollSuccess", _));
        }

        {
            const auto builder = MatchDirectiveJsonBuilder{
                .biometryResult = base64Encode(biometryResult.c_str(), biometryResult.size()),
            };
            const Json::Value json = builder.build();

            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testMatchedUser_LibraryMatches, BioCapabilityFixture) {
        const auto owner = addOwnerUser();

        const TString enrollId = "id";
        const TString enrollVersion = "ver";
        updateOwnerUser(enrollId, enrollVersion);

        {
            testing::InSequence eventsSequence;

            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnrollChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioMatchEnrollChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioLibraryDiagnosticData", _, _));
        }

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(false));

            EXPECT_CALL(*mockEnrollmentEngine, StartRequest());
        }

        {
            proto::AliceState state;
            state.set_state(proto::AliceState::LISTENING);
            bioCapability->onAliceStateChanged(state);
        }

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));

            EXPECT_CALL(*mockEnrollmentEngine, AddChunk(_, false));

            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(false));

            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));

            EXPECT_CALL(*mockEnrollmentEngine, GetMatchedEnrollmentID())
                .WillOnce(Return(enrollId));

            // clang-format off
            EXPECT_CALL(*mockAliceCapability, startRequest(CheckGuestOptions(owner.passportUid, owner.authToken, enrollId), Eq(nullptr)))
                .WillOnce(WithArgs<0>([](const auto& request) {
                    UNIT_ASSERT_EQUAL(request->getEventSource().type(), NAlice::TSpeechKitRequestProto_TEventSource_EType_VoiceprintMatch);
                    UNIT_ASSERT(request->getIsParallel());
                    UNIT_ASSERT(request->getIsSilent());
                    UNIT_ASSERT(request->getIgnoreAnswer());
                }));
            // clang-format on
        }

        const auto data = YandexIO::ChannelData{.isForRecognition = true};

        pushChannelsData({data});

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));
        }

        {
            proto::AliceState state;
            state.set_state(proto::AliceState::SPEAKING);
            bioCapability->onAliceStateChanged(state);
        }

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockEnrollmentEngine, AddChunk(_, true));

            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(false));

            EXPECT_CALL(*mockEnrollmentEngine, IsRequestActive())
                .WillOnce(Return(true));

            EXPECT_CALL(*mockEnrollmentEngine, GetMatchedEnrollmentID())
                .WillOnce(Return(Nothing()));

            // clang-format off
            EXPECT_CALL(*mockAliceCapability, startRequest(CheckGuestOptions("", "", ""), Eq(nullptr)))
                .WillOnce(WithArgs<0>([](const auto& request) {
                    UNIT_ASSERT_EQUAL(request->getEventSource().type(), NAlice::TSpeechKitRequestProto_TEventSource_EType_VoiceprintMatch);
                    UNIT_ASSERT(request->getIsParallel());
                    UNIT_ASSERT(request->getIsSilent());
                    UNIT_ASSERT(request->getIgnoreAnswer());
                }));
            // clang-format on

            EXPECT_CALL(*mockEnrollmentEngine, FinishRequest());
        }

        pushChannelsData({data});
    }

    Y_UNIT_TEST_F(testStartEnrollment_failure, BioCapabilityFixture) {
        EXPECT_CALL(*mockFilePlayerCapability, playSoundFile("guest_enrollment_failed.mp3", _, _, _));

        {
            testing::InSequence defaultTimeoutSequence;

            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentStart", _));
            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Can't start enrollment"), _, _, HasSubstr("BioCapability.EnrollmentStartFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioEnrollmentFailed", _, _));
        }

        {
            const auto defaultTimeout = StartEnrollmentJsonBuilder{}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(defaultTimeout);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->delayedCallbackCount(), 0);
    }

    Y_UNIT_TEST_F(testStartEnrollment_defaultTimeout, BioCapabilityFixture) {
        const auto owner = addOwnerUser();
        startEnrollment("qwe");

        EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment());

        {
            testing::InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Cancel enrollment by timeout"), _, _, HasSubstr("BioCapability.EnrollmentTimeout")));
            EXPECT_CALL(*mockTelemetry, reportError("bioEnrollmentTimeout", _, _));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(owner.passportUid, owner.authToken, "")), Eq(nullptr)));
        // clang-format on

        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->delayedCallbackCount(), 1);
        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->firstDelayedCallbackTimeout().count(), 600000);
        testCallbackQueue->pumpDelayedCallback();
    }

    Y_UNIT_TEST_F(testStartEnrollment_nonDefaultTimeout, BioCapabilityFixture) {
        const auto owner = addOwnerUser();
        startEnrollment(3000);

        EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment());

        {
            testing::InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Cancel enrollment by timeout"), _, _, HasSubstr("BioCapability.EnrollmentTimeout")));
            EXPECT_CALL(*mockTelemetry, reportError("bioEnrollmentTimeout", _, _));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(owner.passportUid, owner.authToken, "")), Eq(nullptr)));
        // clang-format on

        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->delayedCallbackCount(), 1);
        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->firstDelayedCallbackTimeout().count(), 3000);
        testCallbackQueue->pumpDelayedCallback();
    }

    Y_UNIT_TEST_F(testStartEnrollment_OK, BioCapabilityFixture) {
        addOwnerUser();
        startEnrollment();
    }

    Y_UNIT_TEST_F(testCancelEnrollment_noStart, BioCapabilityFixture) {
        EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment())
            .Times(0);
        EXPECT_CALL(*mockEnrollmentEngine, CommitEnrollment())
            .Times(0);

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentCancel", _));
            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(false));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("no active enrollment"), _, _, HasSubstr("BioCapability.EnrollmentCancelFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioEnrollmentFailed", _, _));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions("", "", "")), Eq(nullptr)));
        // clang-format on

        const auto json = CancelEnrollmentJsonBuilder{}.build();
        const auto directiveData = YandexIO::Directive::Data::fromJson(json);
        const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
        bioCapability->handleDirective(directive);
    }

    Y_UNIT_TEST_F(testCancelEnrollment_OK, BioCapabilityFixture) {
        EXPECT_CALL(*mockEnrollmentEngine, CommitEnrollment())
            .Times(0);

        const auto owner = addOwnerUser();
        startEnrollment();

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment());
        }

        {
            testing::InSequence eventsSequence;

            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentCancel", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentCancelSuccess", _));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(owner.passportUid, "", "")), Eq(nullptr)));
        // clang-format on

        const auto json = CancelEnrollmentJsonBuilder{}.build();
        const auto directiveData = YandexIO::Directive::Data::fromJson(json);
        const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
        bioCapability->handleDirective(directive);

        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->delayedCallbackCount(), 1);

        // Check that delayed callback does nothing
        testCallbackQueue->pumpDelayedCallback();
    }

    Y_UNIT_TEST_F(testFinishEnrollment_noStart, BioCapabilityFixture) {
        EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment())
            .Times(0);
        EXPECT_CALL(*mockEnrollmentEngine, CommitEnrollment())
            .Times(0);

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentFinish", _));
            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(false));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("No active enrollment"), _, _, HasSubstr("BioCapability.EnrollmentFinishFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioEnrollmentFailed", _, _));
        }

        const auto json = FinishEnrollmentJsonBuilder{}.build();
        const auto directiveData = YandexIO::Directive::Data::fromJson(json);
        const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
        bioCapability->handleDirective(directive);
    }

    Y_UNIT_TEST_F(testFinishEnrollment_invalidPersonId, BioCapabilityFixture) {
        EXPECT_CALL(*mockEnrollmentEngine, CommitEnrollment())
            .Times(0);

        EXPECT_CALL(*mockFilePlayerCapability, playSoundFile("guest_enrollment_failed.mp3", _, _, _));

        mockAuthProvider->setDeleteUser([](auto /*id*/, auto /*timeout*/) {
            return IAuthProvider::DeleteUserResponse{
                .status = IAuthProvider::DeleteUserResponse::Status::OK};
        });

        const auto guest = addGuestUser();
        startEnrollment();

        {
            testing::InSequence sequence;

            EXPECT_CALL(*mockEnrollmentEngine, IsEnrollmentActive())
                .WillOnce(Return(true));
            EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment());
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(guest.passportUid, guest.authToken, "")), Eq(nullptr)));
        // clang-format on

        {
            testing::InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentFinish", _));
            EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'pers_id' is empty"), _, _, HasSubstr("BioCapability.EnrollmentFinishFailed")));
            EXPECT_CALL(*mockTelemetry, reportError("bioEnrollmentFailed", _, _));
        }

        {
            const auto json = FinishEnrollmentJsonBuilder{}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }

        UNIT_ASSERT_VALUES_EQUAL(testCallbackQueue->delayedCallbackCount(), 2);

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(guest.passportUid, guest.authToken, "")), Eq(nullptr)));
        // clang-format on

        {
            testing::InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportLogError(_, _, _, HasSubstr("BioCapability.AddAccountTimeout")));
            EXPECT_CALL(*mockTelemetry, reportError("bioAddAccountTimeout", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
        }

        {
            testing::InSequence sequence;
            EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(IsEmpty()));
            EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
                .WillOnce(Return(THashMap<TString, TString>{}));
            EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(_));
        }

        // AddGuestTimeout callback
        testCallbackQueue->pumpDelayedCallback();

        // CancelEnrollment callback. Should do nothing.
        testCallbackQueue->pumpDelayedCallback();
    }

    Y_UNIT_TEST_F(testFinishEnrollment_OK, BioCapabilityFixture) {
        EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment())
            .Times(0);

        mockAuthProvider->setDeleteUser([](auto /*id*/, auto /*timeout*/) {
            UNIT_FAIL("deleteUser shouldn't be called.");
            return IAuthProvider::DeleteUserResponse{};
        });

        const auto voiceprint = TBlob::FromString("voiceprint");

        const auto guest = addGuestUser();
        startEnrollment();

        finishEnrollment(guest.passportUid, guest.authToken, "persId", "version0");

        testCallbackQueue->pumpDelayedQueueUntilEmpty();
    }

    Y_UNIT_TEST_F(testFinishEnrollmentWithFrame_OK, BioCapabilityFixture) {
        EXPECT_CALL(*mockEnrollmentEngine, CancelEnrollment())
            .Times(0);

        mockAuthProvider->setDeleteUser([](auto /*id*/, auto /*timeout*/) {
            UNIT_FAIL("deleteUser shouldn't be called.");
            return IAuthProvider::DeleteUserResponse{};
        });

        const auto voiceprint = TBlob::FromString("voiceprint");

        const auto guest = addGuestUser();
        startEnrollment();

        finishEnrollment(guest.passportUid, guest.authToken, "persId", "version0", true);

        testCallbackQueue->pumpDelayedQueueUntilEmpty();
    }

    Y_UNIT_TEST_F(testRemoveAccount_errors, BioCapabilityFixture) {
        mockAuthProvider->setDeleteUser([](auto /*id*/, auto /*timeout*/) {
            return IAuthProvider::DeleteUserResponse{
                .status = IAuthProvider::DeleteUserResponse::Status::OK};
        });

        {
            {
                testing::InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccount", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'puid' is empty."), _, _, HasSubstr("BioCapability.RemoveAccountFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioRemoveAccountFailed", _, _));
            }

            // clang-format off
            EXPECT_CALL(*mockAliceCapability, startRequest(
                ::testing::AllOf(
                    CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                    CheckGuestOptions("", "", "")), Eq(nullptr)));
            // clang-format on

            const auto noPuid = RemoveAccountJsonBuilder{}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(noPuid);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);

            testCallbackQueue->pumpDelayedQueueUntilEmpty();
        }

        {
            {
                testing::InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccount", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("'puid' is empty."), _, _, HasSubstr("BioCapability.RemoveAccountFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioRemoveAccountFailed", _, _));
            }

            // clang-format off
            EXPECT_CALL(*mockAliceCapability, startRequest(
                ::testing::AllOf(
                    CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                    CheckGuestOptions("", "", "")), Eq(nullptr)));
            // clang-format on

            const auto json = RemoveAccountJsonBuilder{.puid = "42"}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);

            testCallbackQueue->pumpDelayedQueueUntilEmpty();
        }

        {
            {
                testing::InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccount", _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Can't find user"), _, _, HasSubstr("BioCapability.RemoveAccountFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioRemoveAccountFailed", _, _));
            }

            // clang-format off
            EXPECT_CALL(*mockAliceCapability, startRequest(
                ::testing::AllOf(
                    CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                    CheckGuestOptions("42", "", "")), Eq(nullptr)));
            // clang-format on

            const auto json = RemoveAccountJsonBuilder{.puid = 42}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);

            testCallbackQueue->pumpDelayedQueueUntilEmpty();
        }

        mockAuthProvider->setDeleteUser([](auto /*id*/, auto /*timeout*/) {
            return IAuthProvider::DeleteUserResponse{
                .status = IAuthProvider::DeleteUserResponse::Status::UNDEFINED};
        });

        const auto guest = addGuestUser();

        {
            {
                testing::InSequence eventsSequence;
                EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccount", _));
                EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
                EXPECT_CALL(*mockTelemetry, reportLogError(HasSubstr("Can't remove user"), _, _, HasSubstr("BioCapability.RemoveAccountFailed")));
                EXPECT_CALL(*mockTelemetry, reportError("bioRemoveAccountFailed", _, _));
            }

            // clang-format off
            EXPECT_CALL(*mockAliceCapability, startRequest(
                ::testing::AllOf(
                    CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                    CheckGuestOptions(guest.passportUid, "", "")), Eq(nullptr)));
            // clang-format on

            {
                testing::InSequence sequence;
                EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(IsEmpty()));
                EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
                    .WillOnce(Return(THashMap<TString, TString>{}));
                EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(_));
            }

            const auto json = RemoveAccountJsonBuilder{.puid = std::stoi(guest.passportUid)}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testRemoveGuestAccount_OK, BioCapabilityFixture) {
        EXPECT_CALL(*mockTelemetry, reportLogError(_, _, _, _))
            .Times(0);

        const auto guest = addGuestUser();

        startEnrollment();
        finishEnrollment(guest.passportUid, guest.authToken, "persId");

        mockAuthProvider->setDeleteUser([](auto /*id*/, auto /*timeout*/) {
            return IAuthProvider::DeleteUserResponse{
                .status = IAuthProvider::DeleteUserResponse::Status::OK};
        });

        {
            testing::InSequence sequence;
            EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(IsEmpty()));
            EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
                .WillOnce(Return(THashMap<TString, TString>{}));
            EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(_));
        }

        {
            testing::InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccount", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccountSuccess", _, _));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(guest.passportUid, guest.authToken, "persId")), Eq(nullptr)));
        // clang-format on

        {
            const auto json = RemoveAccountJsonBuilder{.puid = std::stoi(guest.passportUid)}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testRemoveOwnerAccount_OK, BioCapabilityFixture) {
        EXPECT_CALL(*mockTelemetry, reportLogError(_, _, _, _))
            .Times(0);

        const auto owner = addOwnerUser();
        const TString enrollId = "persId";
        const TString enrollVersion = "ver";
        updateOwnerUser(enrollId, enrollVersion);

        {
            testing::InSequence sequence;
            EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(IsEmpty()));
            EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
                .WillOnce(Return(THashMap<TString, TString>{}));
            EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(_));
        }

        {
            testing::InSequence eventsSequence;
            EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccount", _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
            EXPECT_CALL(*mockTelemetry, reportEvent("bioRemoveAccountSuccess", _, _));
        }

        // clang-format off
        EXPECT_CALL(*mockAliceCapability, startRequest(
            ::testing::AllOf(
                CheckSemanticFrameName("multiaccount_enrollment_status_semantic_frame"),
                CheckGuestOptions(owner.passportUid, owner.authToken, enrollId)), Eq(nullptr)));
        // clang-format on

        {
            const auto json = RemoveAccountJsonBuilder{.puid = std::stoi(owner.passportUid)}.build();
            const auto directiveData = YandexIO::Directive::Data::fromJson(json);
            const auto directive = std::make_shared<YandexIO::Directive>(directiveData, false);
            bioCapability->handleDirective(directive);
        }
    }

    Y_UNIT_TEST_F(testRemoveUsersOnSpeakerSetup, BioCapabilityFixture) {
        addOwnerUser();

        const TString ownerEnrollId = "idOwner";
        const TString ownerEnrollVersion = "verOwner";
        updateOwnerUser(ownerEnrollId, ownerEnrollVersion);

        const auto guest = addGuestUser();
        startEnrollment();
        finishEnrollment(guest.passportUid, guest.authToken, "persId", "version0");

        EXPECT_CALL(*mockEnrollmentEngine, SetEnrollments(_))
            .WillOnce(Invoke([&ownerEnrollId](const auto& enrollments) {
                UNIT_ASSERT_VALUES_EQUAL(enrollments.size(), 1u);
                UNIT_ASSERT(enrollments.contains(ownerEnrollId));
            }));

        EXPECT_CALL(*mockEnrollmentEngine, GetEnrollmentsVersionInfo())
            .WillOnce(Return(THashMap<TString, TString>{}));
        EXPECT_CALL(*mockTelemetry, reportEvent("bioEnrollmentHeadersChanged", _, _));
        EXPECT_CALL(*mockDeviceStateCapability, setEnrollmentHeaders(_));

        mockAuthProvider->clearGuests();
    }
}
