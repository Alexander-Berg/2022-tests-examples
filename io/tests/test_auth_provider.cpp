#include <yandex_io/interfaces/auth/connector/auth_provider.h>

#include <yandex_io/libs/ipc/mock/simple_connector.h>
#include "yandex_io/protos/account_storage.pb.h"
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/protos/enum_names/enum_names.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    class SdkContext: public AuthProvider::ISdkContext {
    public:
        void setUserAccountInfo(const std::string& authToken, const std::string& passportUid)
        {
            if (onUserAccountInfoChanged_) {
                onUserAccountInfoChanged_(authToken, passportUid);
            }
        }

        void connectToUserAccountInfoChanged(std::function<void(const std::string&, const std::string&)> onUserAccountInfoChanged) override {
            onUserAccountInfoChanged_ = onUserAccountInfoChanged;
        }

        void fireOAuthTokenIsInvalid(const std::string& token) override {
            if (onFireOAuthTokenIsInvalid_) {
                onFireOAuthTokenIsInvalid_(token);
            }
        }

        std::function<void(const std::string&)> onFireOAuthTokenIsInvalid_;

    private:
        std::function<void(const std::string&, const std::string&)> onUserAccountInfoChanged_;
    };

    auto messageEmptyStartupInfo = ipc::buildMessage([](auto& msg) {
        msg.mutable_all_startup_info();
    });

    auto messageStartupInfo = ipc::buildMessage([](auto& msg) {
        auto* startupInfo = msg.mutable_all_startup_info();
        {
            auto* owner = startupInfo->add_accounts();
            owner->set_auth_token("123token");
            owner->set_passport_uid("123");
            owner->set_type(proto::AccountType::OWNER);
            owner->set_tag(1);
        }
        {
            auto* guest = startupInfo->add_accounts();
            guest->set_auth_token("456token");
            guest->set_passport_uid("456");
            guest->set_type(proto::AccountType::GUEST);
            guest->set_tag(7);
        }
    });

    auto messageAddGuest = ipc::buildMessage([](auto& msg) {
        msg.mutable_add_user_event()->set_auth_token("567token");
        msg.mutable_add_user_event()->set_passport_uid("567");
        msg.mutable_add_user_event()->set_type(proto::AccountType::GUEST);
        msg.mutable_add_user_event()->set_tag(4);
    });

    auto messageAddOwnerEvent = ipc::buildMessage([](auto& msg) {
        msg.mutable_add_user_event()->set_auth_token("678token");
        msg.mutable_add_user_event()->set_passport_uid("678");
        msg.mutable_add_user_event()->set_type(proto::AccountType::OWNER);
        msg.mutable_add_user_event()->set_tag(1);
    });

    auto messageChangeOwnerEvent = ipc::buildMessage([](auto& msg) {
        msg.mutable_change_user_event()->set_auth_token("678token");
        msg.mutable_change_user_event()->set_passport_uid("678");
        msg.mutable_change_user_event()->set_type(proto::AccountType::OWNER);
        msg.mutable_change_user_event()->set_tag(2);
    });

    auto messageChangeOwnerToken = ipc::buildMessage([](auto& msg) {
        msg.mutable_change_token_event()->set_auth_token("333token");
        msg.mutable_change_token_event()->set_passport_uid("678");
        msg.mutable_change_token_event()->set_type(proto::AccountType::OWNER);
        msg.mutable_change_token_event()->set_tag(3);
    });

    auto messageDeleteOwner = ipc::buildMessage([](auto& msg) {
        msg.mutable_delete_user_event()->set_auth_token("123token");
        msg.mutable_delete_user_event()->set_passport_uid("123");
        msg.mutable_delete_user_event()->set_type(proto::AccountType::OWNER);
    });

    auto messageDeleteGuest = ipc::buildMessage([](auto& msg) {
        msg.mutable_delete_user_event()->set_auth_token("456token");
        msg.mutable_delete_user_event()->set_passport_uid("456");
        msg.mutable_delete_user_event()->set_type(proto::AccountType::GUEST);
    });

    auto messageRefreshOwner = ipc::buildMessage([](auto& msg) {
        msg.mutable_refresh_user_event()->set_auth_token("333token");
        msg.mutable_refresh_user_event()->set_passport_uid("678");
        msg.mutable_refresh_user_event()->set_type(proto::AccountType::OWNER);
        msg.mutable_refresh_user_event()->set_tag(5);
    });

} // namespace

Y_UNIT_TEST_SUITE(AuthProvider)
{

    Y_UNIT_TEST(testStatusName)
    {
        static_assert((int)IAuthProvider::AddUserResponse::Status::OK == (int)proto::AddUserResponse::OK);
        static_assert((int)IAuthProvider::AddUserResponse::Status::NO_INTERNET == (int)proto::AddUserResponse::NO_INTERNET);
        static_assert((int)IAuthProvider::AddUserResponse::Status::CODE_EXPIRED == (int)proto::AddUserResponse::CODE_EXPIRED);
        static_assert((int)IAuthProvider::AddUserResponse::Status::CRYPTO_ERROR == (int)proto::AddUserResponse::CRYPTO_ERROR);
        static_assert((int)IAuthProvider::AddUserResponse::Status::INVALID_TOKEN == (int)proto::AddUserResponse::INVALID_TOKEN);
        UNIT_ASSERT_VALUES_EQUAL(IAuthProvider::AddUserResponse{.status = IAuthProvider::AddUserResponse::Status::OK}.statusName(), addUserResponseStatusName(proto::AddUserResponse::OK));
        UNIT_ASSERT_VALUES_EQUAL(IAuthProvider::AddUserResponse{.status = IAuthProvider::AddUserResponse::Status::NO_INTERNET}.statusName(), addUserResponseStatusName(proto::AddUserResponse::NO_INTERNET));
        UNIT_ASSERT_VALUES_EQUAL(IAuthProvider::AddUserResponse{.status = IAuthProvider::AddUserResponse::Status::CODE_EXPIRED}.statusName(), addUserResponseStatusName(proto::AddUserResponse::CODE_EXPIRED));
        UNIT_ASSERT_VALUES_EQUAL(IAuthProvider::AddUserResponse{.status = IAuthProvider::AddUserResponse::Status::CRYPTO_ERROR}.statusName(), addUserResponseStatusName(proto::AddUserResponse::CRYPTO_ERROR));
        UNIT_ASSERT_VALUES_EQUAL(IAuthProvider::AddUserResponse{.status = IAuthProvider::AddUserResponse::Status::INVALID_TOKEN}.statusName(), addUserResponseStatusName(proto::AddUserResponse::INVALID_TOKEN));
    }

    Y_UNIT_TEST(testDefault)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);
        const auto owner = authProvider.ownerAuthInfo().value();
        UNIT_ASSERT(owner != nullptr);
        UNIT_ASSERT_VALUES_EQUAL((int)owner->source, (int)AuthInfo2::Source::UNDEFINED);
        UNIT_ASSERT_VALUES_EQUAL(owner->authToken, "");
        UNIT_ASSERT_VALUES_EQUAL(owner->passportUid, "");
        UNIT_ASSERT_VALUES_EQUAL(owner->tag, 0);

        const auto allUsers = authProvider.usersAuthInfo().value();
        UNIT_ASSERT_EQUAL(allUsers->size(), 0);

        std::atomic<bool> signal{false};
        authProvider.ownerAuthInfo().connect([&](auto... /*args*/) { signal = true; }, Lifetime::immortal);
        UNIT_ASSERT_VALUES_EQUAL(signal.load(), true); // Instantly send auth info
    }

    Y_UNIT_TEST(testSdk)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);
        sdkContext->setUserAccountInfo("123token", "999");

        const auto owner = authProvider.ownerAuthInfo().value();
        UNIT_ASSERT(owner != nullptr);

        UNIT_ASSERT_VALUES_EQUAL((int)owner->source, (int)AuthInfo2::Source::SDK);
        UNIT_ASSERT_VALUES_EQUAL(owner->authToken, "123token");
        UNIT_ASSERT_VALUES_EQUAL(owner->passportUid, "999");
        UNIT_ASSERT_VALUES_EQUAL((int)owner->userType, (int)UserType::OWNER);
        UNIT_ASSERT(owner->tag > 0);

        const auto allUsers = authProvider.usersAuthInfo().value();
        UNIT_ASSERT_VALUES_EQUAL(allUsers->size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(allUsers->front().authToken, owner->authToken);
        UNIT_ASSERT_VALUES_EQUAL(allUsers->front().passportUid, owner->passportUid);
        UNIT_ASSERT_VALUES_EQUAL((int)allUsers->front().source, (int)owner->source);
        UNIT_ASSERT_VALUES_EQUAL((int)allUsers->front().userType, (int)owner->userType);
        UNIT_ASSERT_VALUES_EQUAL(allUsers->front().tag, owner->tag);
    }

    Y_UNIT_TEST(testQuasarAuthStartupInfo)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(messageStartupInfo); });
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);
        const auto owner = authProvider.ownerAuthInfo().value();
        UNIT_ASSERT(owner != nullptr);

        UNIT_ASSERT_VALUES_EQUAL((int)owner->source, (int)AuthInfo2::Source::AUTHD);
        UNIT_ASSERT_VALUES_EQUAL(owner->authToken, "123token");
        UNIT_ASSERT_VALUES_EQUAL(owner->passportUid, "123");
        UNIT_ASSERT_VALUES_EQUAL((int)owner->userType, (int)UserType::OWNER);
        UNIT_ASSERT_VALUES_EQUAL(owner->tag, 1);

        authProvider.ownerAuthInfo().connect(
            [&owner](auto ai) {
                UNIT_ASSERT(ai != nullptr);
                UNIT_ASSERT_VALUES_EQUAL((int)owner->source, (int)ai->source);
                UNIT_ASSERT_VALUES_EQUAL(owner->authToken, ai->authToken);
                UNIT_ASSERT_VALUES_EQUAL(owner->passportUid, ai->passportUid);
                UNIT_ASSERT_VALUES_EQUAL((int)owner->userType, (int)ai->userType);
                UNIT_ASSERT_VALUES_EQUAL(owner->tag, ai->tag);
            }, Lifetime::immortal);

        const auto allUsers = authProvider.usersAuthInfo().value();
        UNIT_ASSERT_VALUES_EQUAL(allUsers->size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(allUsers->front().authToken, owner->authToken);
        UNIT_ASSERT_VALUES_EQUAL(allUsers->front().passportUid, owner->passportUid);
        UNIT_ASSERT_VALUES_EQUAL((int)allUsers->front().source, (int)owner->source);
        UNIT_ASSERT_VALUES_EQUAL((int)allUsers->front().userType, (int)owner->userType);
        UNIT_ASSERT_VALUES_EQUAL(allUsers->front().tag, owner->tag);

        UNIT_ASSERT_VALUES_EQUAL(allUsers->back().authToken, "456token");
        UNIT_ASSERT_VALUES_EQUAL(allUsers->back().passportUid, "456");
        UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().source, (int)AuthInfo2::Source::AUTHD);
        UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().userType, (int)UserType::GUEST);
        UNIT_ASSERT_VALUES_EQUAL(allUsers->back().tag, 7);

        authProvider.usersAuthInfo().connect(
            [&allUsers](auto users) {
                UNIT_ASSERT(users != nullptr);

                UNIT_ASSERT_VALUES_EQUAL(allUsers->front().authToken, users->front().authToken);
                UNIT_ASSERT_VALUES_EQUAL(allUsers->front().passportUid, users->front().passportUid);
                UNIT_ASSERT_VALUES_EQUAL((int)allUsers->front().source, (int)users->front().source);
                UNIT_ASSERT_VALUES_EQUAL((int)allUsers->front().userType, (int)users->front().userType);
                UNIT_ASSERT_VALUES_EQUAL(allUsers->front().tag, users->front().tag);

                UNIT_ASSERT_VALUES_EQUAL(allUsers->back().authToken, users->back().authToken);
                UNIT_ASSERT_VALUES_EQUAL(allUsers->back().passportUid, users->back().passportUid);
                UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().source, (int)users->back().source);
                UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().userType, (int)users->back().userType);
                UNIT_ASSERT_VALUES_EQUAL(allUsers->back().tag, users->back().tag);
            }, Lifetime::immortal);
    }

    Y_UNIT_TEST(testQuasarAuthEmptyStartupInfo)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(messageEmptyStartupInfo); });
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);

        int signalCounter = 0;
        authProvider.ownerAuthInfo().connect(
            [&](auto ai) {
                ++signalCounter;
                if (signalCounter == 1) {
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "");
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);
    }

    Y_UNIT_TEST(testQuasarAuthChangeMessages)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(messageStartupInfo); });
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);

        int ownerSignalCounter = 0;
        authProvider.ownerAuthInfo().connect(
            [&](auto ai) mutable {
                ++ownerSignalCounter;
                if (ownerSignalCounter == 1) {
                    // Startup
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "123token");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "123");
                } else if (ownerSignalCounter == 2) {
                    // Change owner
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "678token");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "678");
                    UNIT_ASSERT_VALUES_EQUAL(ai->tag, 2);
                } else if (ownerSignalCounter == 3) {
                    // Change owner token
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "333token");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "678");
                    UNIT_ASSERT_VALUES_EQUAL(ai->tag, 3);
                } else if (ownerSignalCounter == 4) {
                    // Refresh owner
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "333token");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "678");
                    UNIT_ASSERT_VALUES_EQUAL(ai->tag, 5);
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        int usersSignalCounter = 0;
        authProvider.usersAuthInfo().connect(
            [&](auto usersAi) mutable {
                ++usersSignalCounter;
                if (usersSignalCounter == 1) {
                    // Startup
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 2);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().authToken, "123token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().passportUid, "123");
                } else if (usersSignalCounter == 2) {
                    // Change owner
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 3);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().authToken, "123token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().passportUid, "123");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().authToken, "678token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().passportUid, "678");
                    UNIT_ASSERT(authProvider.ownerAuthInfo().value()->isSameAuth(usersAi->back()));
                } else if (usersSignalCounter == 3) {
                    // Change owner token
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 3);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().authToken, "333token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().passportUid, "678");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().tag, 3);
                    UNIT_ASSERT(authProvider.ownerAuthInfo().value()->isSameAuth(usersAi->back()));
                } else if (usersSignalCounter == 4) {
                    // Refresh owner
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 3);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().authToken, "333token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().passportUid, "678");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().tag, 5);
                    UNIT_ASSERT(authProvider.ownerAuthInfo().value()->isSameAuth(usersAi->back()));
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        connector->pushMessage(messageChangeOwnerEvent);
        connector->pushMessage(messageChangeOwnerToken);
        connector->pushMessage(messageRefreshOwner);
    }

    Y_UNIT_TEST(testQuasarAuthChangeOwner) {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(messageStartupInfo); });
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);

        int ownerSignalCounter = 0;
        authProvider.ownerAuthInfo().connect(
            [&](auto ai) mutable {
                ++ownerSignalCounter;
                if (ownerSignalCounter == 1) {
                    // Startup
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "123token");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "123");
                } else if (ownerSignalCounter == 2) {
                    // Change owner
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "678token");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "678");
                } else {
                    // No signal on add new owner and no signal on delete previous owner
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        int usersSignalCounter = 0;
        authProvider.usersAuthInfo().connect(
            [&](auto usersAi) mutable {
                ++usersSignalCounter;
                if (usersSignalCounter == 1) {
                    // Startup
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 2);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().authToken, "123token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().passportUid, "123");
                    UNIT_ASSERT(authProvider.ownerAuthInfo().value()->isSameAuth(usersAi->front()));
                } else if (usersSignalCounter == 2) {
                    // Add owner
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 3);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().authToken, "123token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().passportUid, "123");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().authToken, "678token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().passportUid, "678");
                    UNIT_ASSERT(authProvider.ownerAuthInfo().value()->isSameAuth(usersAi->front()));
                } else if (usersSignalCounter == 3) {
                    // Change owner
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 3);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().authToken, "123token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->front().passportUid, "123");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().authToken, "678token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().passportUid, "678");
                    UNIT_ASSERT(authProvider.ownerAuthInfo().value()->isSameAuth(usersAi->back()));
                } else if (usersSignalCounter == 4) {
                    // Delete previous owner
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->size(), 2);
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().authToken, "678token");
                    UNIT_ASSERT_VALUES_EQUAL(usersAi->back().passportUid, "678");
                    UNIT_ASSERT(authProvider.ownerAuthInfo().value()->isSameAuth(usersAi->back()));
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        connector->pushMessage(messageAddOwnerEvent);
        connector->pushMessage(messageChangeOwnerEvent);
        connector->pushMessage(messageDeleteOwner); // Previous owner, to be correct
    }

    Y_UNIT_TEST(testQuasarVsSdk)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(messageStartupInfo); });
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);

        int signalCounter = 0;
        connector->pushMessage(messageChangeOwnerEvent);
        authProvider.ownerAuthInfo().connect(
            [&](auto ai) {
                ++signalCounter;
                if (signalCounter == 1) {
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::AUTHD);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "678token");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "678");
                    UNIT_ASSERT_VALUES_EQUAL(ai->tag, 2);
                } else if (signalCounter == 2) {
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::SDK);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "sdkToken");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "111");
                    UNIT_ASSERT(ai->tag > 0);
                } else if (signalCounter == 3) {
                    UNIT_ASSERT(ai != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL((int)ai->source, (int)AuthInfo2::Source::SDK);
                    UNIT_ASSERT_VALUES_EQUAL(ai->authToken, "sdkToken2");
                    UNIT_ASSERT_VALUES_EQUAL(ai->passportUid, "222");
                    UNIT_ASSERT(ai->tag > 0);
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        sdkContext->setUserAccountInfo("sdkToken", "111");

        connector->pushMessage(messageChangeOwnerToken); // No effect
        connector->pushMessage(messageRefreshOwner);     // No effect

        sdkContext->setUserAccountInfo("sdkToken2", "222");
    }

    Y_UNIT_TEST(testAddOwnerUser)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto sdkContext = std::make_shared<SdkContext>();

        const TString authCode = "12345";
        const TString authToken = "#authToken";
        const TString xToken = "#xToken";
        const uint64_t id = 5;
        const int64_t tag = 55;

        connector->setSendRequestSyncMethod(
            [&](ipc::Message&& message, std::chrono::milliseconds /*timeout*/) -> ipc::SharedMessage {
                UNIT_ASSERT_VALUES_EQUAL(message.add_user_request().auth_code(), authCode);

                ipc::Message response;
                response.mutable_add_user_response()->set_status(proto::AddUserResponse::OK);
                response.mutable_add_user_response()->set_auth_token(authToken);
                response.mutable_add_user_response()->set_xtoken(xToken);
                response.mutable_add_user_response()->set_id(id);
                response.mutable_add_user_response()->set_tag(tag);
                return ipc::SharedMessage{std::move(response)};
            });

        AuthProvider authProvider(connector, sdkContext);
        const bool withXToken = true;
        auto response = authProvider.addUser(authCode, UserType::OWNER, withXToken, std::chrono::minutes(1));
        UNIT_ASSERT_VALUES_EQUAL((int)response.status, (int)IAuthProvider::AddUserResponse::Status::OK);
        UNIT_ASSERT_VALUES_EQUAL(response.authToken, authToken);
        UNIT_ASSERT_VALUES_EQUAL(response.xToken, xToken);
        UNIT_ASSERT_VALUES_EQUAL(response.id, id);
        UNIT_ASSERT_VALUES_EQUAL(response.tag, tag);
    }

    Y_UNIT_TEST(testChangeUser)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto sdkContext = std::make_shared<SdkContext>();

        const uint64_t id = 5;

        connector->setSendRequestSyncMethod([&](ipc::Message&& message, std::chrono::milliseconds /*timeout*/) -> ipc::SharedMessage {
            UNIT_ASSERT(message.change_user_request().id() > 0);
            return {};
        });

        AuthProvider authProvider(connector, sdkContext);
        authProvider.changeUser(id, std::chrono::minutes(1));
    }

    Y_UNIT_TEST(testRequestAuthTokenUpdateSdk)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto sdkContext = std::make_shared<SdkContext>();

        const std::string authToken{"someToken"};
        std::string invalidToken;

        sdkContext->onFireOAuthTokenIsInvalid_ =
            [&invalidToken](const std::string& token)
        {
            invalidToken = token;
            return true;
        };

        AuthProvider authProvider(connector, sdkContext);
        sdkContext->setUserAccountInfo(authToken, "uid");
        authProvider.requestAuthTokenUpdate("testRequestAuthTokenUpdateSdk");

        UNIT_ASSERT_VALUES_EQUAL(authToken, invalidToken);
    }

    Y_UNIT_TEST(testRequestAuthTokenUpdateAuthd)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto sdkContext = std::make_shared<SdkContext>();

        const TString authToken{"someToken"};
        std::string invalidToken;

        connector->setSendMessageMethod(
            [&invalidToken](const ipc::SharedMessage& message)
            {
                invalidToken = message->auth_token_update_request().auth_token();
                return true;
            });

        AuthProvider authProvider(connector, sdkContext);

        auto message = ipc::buildMessage([&authToken](auto& msg) {
            msg.mutable_change_user_event()->set_xtoken("xtoken");
            msg.mutable_change_user_event()->set_auth_token(authToken);
            msg.mutable_change_user_event()->set_passport_uid("uid");
            msg.mutable_change_user_event()->set_type(proto::AccountType::OWNER);
            msg.mutable_change_user_event()->set_tag(123);
        });
        connector->pushMessage(message);

        authProvider.requestAuthTokenUpdate("testRequestAuthTokenUpdateAuthd");

        UNIT_ASSERT_VALUES_EQUAL(authToken, invalidToken);
    }

    Y_UNIT_TEST(testDeleteUser)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto sdkContext = std::make_shared<SdkContext>();

        const int64_t id = 5;

        {
            connector->setSendRequestSyncMethod(
                [&id](ipc::Message&& message, std::chrono::milliseconds /*timeout*/) -> ipc::SharedMessage {
                    UNIT_ASSERT_VALUES_EQUAL(message.delete_user_request().id(), id);

                    ipc::Message response;
                    response.mutable_delete_user_response()->set_status(proto::DeleteUserResponse::OK);
                    return ipc::SharedMessage{std::move(response)};
                });

            AuthProvider authProvider(connector, sdkContext);
            auto response = authProvider.deleteUser(id, std::chrono::minutes(1));
            UNIT_ASSERT_VALUES_EQUAL((int)response.status, (int)IAuthProvider::DeleteUserResponse::Status::OK);
        }

        {
            connector->setSendRequestSyncMethod(
                [&id](ipc::Message&& message, std::chrono::milliseconds /*timeout*/) -> ipc::SharedMessage {
                    UNIT_ASSERT_VALUES_EQUAL(message.delete_user_request().id(), id);

                    ipc::Message response;
                    response.mutable_delete_user_response()->set_status(proto::DeleteUserResponse::WRONG_USER);
                    return ipc::SharedMessage{std::move(response)};
                });

            AuthProvider authProvider(connector, sdkContext);
            auto response = authProvider.deleteUser(id, std::chrono::minutes(1));
            UNIT_ASSERT_VALUES_EQUAL((int)response.status, (int)IAuthProvider::DeleteUserResponse::Status::WRONG_USER);
        }
    }

    Y_UNIT_TEST(testDeleteUserEvent) {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(messageStartupInfo); });
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);

        {
            const auto owner = authProvider.ownerAuthInfo().value();
            UNIT_ASSERT(owner != nullptr);
            UNIT_ASSERT_VALUES_UNEQUAL(owner->authToken, "");
            UNIT_ASSERT_VALUES_UNEQUAL(owner->passportUid, "");

            const auto allUsers = authProvider.usersAuthInfo().value();
            UNIT_ASSERT_VALUES_EQUAL(allUsers->size(), 2);
            UNIT_ASSERT_VALUES_EQUAL((int)allUsers->front().userType, (int)UserType::OWNER);
            UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().userType, (int)UserType::GUEST);
        }

        connector->pushMessage(messageDeleteGuest);

        {
            const auto owner = authProvider.ownerAuthInfo().value();
            UNIT_ASSERT(owner != nullptr);
            UNIT_ASSERT_VALUES_UNEQUAL(owner->authToken, "");
            UNIT_ASSERT_VALUES_UNEQUAL(owner->passportUid, "");

            const auto allUsers = authProvider.usersAuthInfo().value();
            UNIT_ASSERT_VALUES_EQUAL(allUsers->size(), 1);
            UNIT_ASSERT_EQUAL(*owner, allUsers->front());
        }

        connector->pushMessage(messageDeleteOwner);

        {
            const auto owner = authProvider.ownerAuthInfo().value();
            UNIT_ASSERT(owner != nullptr);
            UNIT_ASSERT_VALUES_EQUAL(owner->authToken, "");
            UNIT_ASSERT_VALUES_EQUAL(owner->passportUid, "");

            const auto allUsers = authProvider.usersAuthInfo().value();
            UNIT_ASSERT_VALUES_EQUAL(allUsers->size(), 0);
        }
    }

    Y_UNIT_TEST(testAddUserEvent) {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(messageStartupInfo); });
        auto sdkContext = std::make_shared<SdkContext>();

        AuthProvider authProvider(connector, sdkContext);
        const auto initialOwner = authProvider.ownerAuthInfo().value();
        UNIT_ASSERT_UNEQUAL(initialOwner, nullptr);

        const auto initialAllUsers = authProvider.usersAuthInfo().value();
        UNIT_ASSERT_VALUES_EQUAL(initialAllUsers->size(), 2);

        {
            const auto addedOnwer = AuthInfo2{
                .source = AuthInfo2::Source::AUTHD,
                .authToken = "321token",
                .passportUid = "321",
                .userType = UserType::OWNER,
                .tag = 9};

            auto messageAddOwner = ipc::buildMessage([&addedOnwer](auto& msg) {
                msg.mutable_add_user_event()->set_auth_token(TString(addedOnwer.authToken));
                msg.mutable_add_user_event()->set_passport_uid(TString(addedOnwer.passportUid));
                msg.mutable_add_user_event()->set_type(proto::AccountType::OWNER);
                msg.mutable_add_user_event()->set_tag(addedOnwer.tag);
            });

            // Add message should't change current account. Even if it's a new OWNER account.
            connector->pushMessage(messageAddOwner);

            const auto owner = authProvider.ownerAuthInfo().value();
            UNIT_ASSERT_VALUES_EQUAL((int)initialOwner->source, (int)owner->source);
            UNIT_ASSERT_VALUES_EQUAL(initialOwner->authToken, owner->authToken);
            UNIT_ASSERT_VALUES_EQUAL(initialOwner->passportUid, owner->passportUid);
            UNIT_ASSERT_VALUES_EQUAL((int)initialOwner->userType, (int)owner->userType);
            UNIT_ASSERT_VALUES_EQUAL(initialOwner->tag, owner->tag);

            const auto allUsers = authProvider.usersAuthInfo().value();
            UNIT_ASSERT_VALUES_EQUAL(allUsers->size(), initialAllUsers->size() + 1);
            UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().source, (int)addedOnwer.source);
            UNIT_ASSERT_VALUES_EQUAL(allUsers->back().authToken, addedOnwer.authToken);
            UNIT_ASSERT_VALUES_EQUAL(allUsers->back().passportUid, addedOnwer.passportUid);
            UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().userType, (int)addedOnwer.userType);
            UNIT_ASSERT_VALUES_EQUAL(allUsers->back().tag, addedOnwer.tag);
        }

        {
            const auto addedGuest = AuthInfo2{
                .source = AuthInfo2::Source::AUTHD,
                .authToken = "654token",
                .passportUid = "654",
                .userType = UserType::GUEST,
                .tag = 11};

            auto messageAddGuest = ipc::buildMessage([&addedGuest](auto& msg) {
                msg.mutable_add_user_event()->set_auth_token(TString(addedGuest.authToken));
                msg.mutable_add_user_event()->set_passport_uid(TString(addedGuest.passportUid));
                msg.mutable_add_user_event()->set_type(proto::AccountType::GUEST);
                msg.mutable_add_user_event()->set_tag(addedGuest.tag);
            });

            connector->pushMessage(messageAddGuest);

            const auto owner = authProvider.ownerAuthInfo().value();
            UNIT_ASSERT_VALUES_EQUAL((int)initialOwner->source, (int)owner->source);
            UNIT_ASSERT_VALUES_EQUAL(initialOwner->authToken, owner->authToken);
            UNIT_ASSERT_VALUES_EQUAL(initialOwner->passportUid, owner->passportUid);
            UNIT_ASSERT_VALUES_EQUAL((int)initialOwner->userType, (int)owner->userType);
            UNIT_ASSERT_VALUES_EQUAL(initialOwner->tag, owner->tag);

            const auto allUsers = authProvider.usersAuthInfo().value();
            UNIT_ASSERT_VALUES_EQUAL(allUsers->size(), initialAllUsers->size() + 2);
            UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().source, (int)addedGuest.source);
            UNIT_ASSERT_VALUES_EQUAL(allUsers->back().authToken, addedGuest.authToken);
            UNIT_ASSERT_VALUES_EQUAL(allUsers->back().passportUid, addedGuest.passportUid);
            UNIT_ASSERT_VALUES_EQUAL((int)allUsers->back().userType, (int)addedGuest.userType);
            UNIT_ASSERT_VALUES_EQUAL(allUsers->back().tag, addedGuest.tag);
        }
    }

} // Y_UNIT_TEST_SUITE(AuthProvider)
