#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yandex_io/services/authd/account_storage.h>
#include <yandex_io/services/authd/auth_endpoint.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/message.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/protos/account_storage.pb.h>
#include <yandex_io/protos/quasar_proto.pb.h>

#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <util/folder/path.h>

#include <cstdint>
#include <chrono>
#include <future>
#include <map>
#include <vector>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace std::chrono;

namespace {

    class Fixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            quasarDirPath = JoinFsPaths(tryGetRamDrivePath(), "quasarDir-" + makeUUID());
            storagePath = JoinFsPaths(quasarDirPath, "accountStorage.dat");

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            auto& serviceConfig = config[AuthEndpoint::SERVICE_NAME];
            int httpPort = oauthServer.start(getPort());

            serviceConfig["accountStorageFile"] = storagePath.GetPath();
            serviceConfig["passportUrl"] = "http://localhost:" + std::to_string(httpPort);
            serviceConfig["loginUrl"] = serviceConfig["passportUrl"];

            serviceConfig["xTokenClientId"] = "clientId1";
            serviceConfig["xTokenClientSecret"] = "clientSecret1";

            serviceConfig["authTokenClientId"] = "clientId2";
            serviceConfig["authTokenClientSecret"] = "clientSecret2";

            quasarDirPath.ForceDelete();
            quasarDirPath.MkDirs();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            TFsPath(quasarDirPath).ForceDelete();

            Base::TearDown(context);
        }

    protected:
        TFsPath quasarDirPath;
        TFsPath storagePath;

        YandexIO::Configuration::TestGuard testGuard;
        TestHttpServer oauthServer;
    };

    struct MockCallbackQueue: public quasar::ICallbackQueue {
        std::string name() const override {
            return "MockCallbackQueue";
        }

        void add(std::function<void()> callback) override {
            YIO_LOG_INFO("Added callback");
            callbacks.emplace_back(std::move(callback));
        }

        void add(std::function<void()> callback, Lifetime::Tracker tracker) override {
            add(makeSafeCallback(std::move(callback), std::move(tracker)));
        }

        bool tryAdd(std::function<void()> /*callback*/) override {
            throw std::runtime_error("not implemented");
        }

        bool tryAdd(std::function<void()> callback, Lifetime::Tracker tracker) override {
            return tryAdd(makeSafeCallback(std::move(callback), tracker));
        }

        void addDelayed(std::function<void()> callback, std::chrono::milliseconds timeout) override {
            YIO_LOG_INFO("Added delayed callback. Timeout " << timeout.count() << "ms");
            delayedCallbacks.emplace(now + timeout, std::move(callback));
        }

        void addDelayed(std::function<void()> callback, std::chrono::milliseconds timeout, Lifetime::Tracker tracker) override {
            addDelayed(makeSafeCallback(std::move(callback), std::move(tracker)), timeout);
        }

        void wait(AwatingType /*type*/) noexcept override {
            runAll(std::chrono::milliseconds{0});
        }

        bool isWorkingThread() const noexcept override {
            return true;
        }

        size_t size() const override {
            return callbacks.size() + delayedCallbacks.size();
        }

        void clear() {
            callbacks.clear();
            delayedCallbacks.clear();
        }

        void setMaxSize(size_t /*size*/) override {
            // do nothing
        }

        void runAll(std::chrono::milliseconds elapsed)
        {
            YIO_LOG_INFO("Run all callbacks within " << elapsed.count() << "ms");
            auto callbacksCopy = callbacks;
            callbacks.clear();
            for (auto& cb : callbacksCopy) {
                cb();
            }

            now += elapsed;

            auto notReadyCallbacksIt = delayedCallbacks.upper_bound(now);
            const decltype(delayedCallbacks) readyCallbacks(delayedCallbacks.begin(), notReadyCallbacksIt);
            delayedCallbacks.erase(delayedCallbacks.begin(), notReadyCallbacksIt);
            for (auto& [_, callback] : readyCallbacks) {
                callback();
            }
        }

        std::vector<std::function<void()>> callbacks;
        std::multimap<std::chrono::milliseconds, std::function<void()>> delayedCallbacks;

        std::chrono::milliseconds now{0};
    };

} // namespace

Y_UNIT_TEST_SUITE_F(AuthEndpointTest, Fixture) {
    Y_UNIT_TEST(testAuthAddOwnerUser)
    {
        const std::string authCode = "auth_code";
        bool xTokenRequestReceived = false;
        bool authTokenRequestReceived = false;

        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/token");
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "POST");
            auto formData = parseHttpFormData(payload);
            Json::Value response;

            if (!xTokenRequestReceived) // First request
            {
                UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "authorization_code");
                UNIT_ASSERT_VALUES_EQUAL(formData["code"], authCode);
                UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId1");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret1");
                response["token_type"] = "bearer";
                response["access_token"] = "xToken";
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";

                xTokenRequestReceived = true;
            } else { // Second request
                UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "x-token");
                UNIT_ASSERT_VALUES_EQUAL(formData["access_token"], "xToken");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId2");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret2");
                response["token_type"] = "bearer";
                response["access_token"] = "authToken";
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";
                response["uid"] = 1130000029536845L;

                authTokenRequestReceived = true;
            }
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        AuthEndpoint authEndpoint(getDeviceForTests(), ipcFactoryForTests());

        auto connector = createIpcConnectorForTests("authd");
        connector->connectToService();
        connector->waitUntilConnected();

        auto connector2 = createIpcConnectorForTests("authd");
        std::promise<ipc::SharedMessage> changeUserPromise;
        connector2->setMessageHandler([&changeUserPromise](const auto& message) {
            if (message->has_change_user_event()) {
                changeUserPromise.set_value(message);
            }
        });
        connector2->connectToService();
        connector2->waitUntilConnected();

        authEndpoint.waitConnectionsAtLeast(2);

        {
            auto request = ipc::buildUniqueMessage([&authCode](auto& msg) {
                msg.mutable_add_user_request()->set_auth_code(TString(authCode));
                msg.mutable_add_user_request()->set_account_type(proto::AccountType::OWNER);
                msg.mutable_add_user_request()->set_with_xtoken(true);
            });
            auto response = connector->sendRequestSync(std::move(request), std::chrono::seconds(600));
            UNIT_ASSERT_VALUES_EQUAL(int(response->add_user_response().status()), int(proto::AddUserResponse::OK));
            UNIT_ASSERT_VALUES_EQUAL(response->add_user_response().auth_token(), "authToken");

            UNIT_ASSERT(xTokenRequestReceived);
            UNIT_ASSERT(authTokenRequestReceived);

            const long accountId = response->add_user_response().id();

            {
                changeUserPromise = std::promise<ipc::SharedMessage>();

                auto changeUserRequest = ipc::buildMessage([&accountId](auto& msg) {
                    msg.mutable_change_user_request()->set_id(accountId);
                });
                connector->sendMessage(changeUserRequest);

                const auto changeUserResponse = changeUserPromise.get_future().get();
                UNIT_ASSERT(changeUserResponse->has_change_user_event());
                UNIT_ASSERT_VALUES_EQUAL(changeUserResponse->change_user_event().auth_token(), "authToken");
                UNIT_ASSERT_VALUES_EQUAL(changeUserResponse->change_user_event().passport_uid(), "1130000029536845");
            }
        }
    }

    Y_UNIT_TEST(testAuthAddGuestUserWithXToken)
    {
        const std::string authCode = "auth_code";
        const std::string authToken = "oauthToken";
        const AccountStorage::AccountId uid = 1130000029536845L;

        bool xTokenRequestReceived = false;
        bool oauthTokenRequestReceived = false;

        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/token");
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "POST");
            auto formData = parseHttpFormData(payload);
            Json::Value response;

            if (!xTokenRequestReceived) // First request
            {
                UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "authorization_code");
                UNIT_ASSERT_VALUES_EQUAL(formData["code"], authCode);
                UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId1");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret1");
                response["token_type"] = "bearer";
                response["access_token"] = "xToken";
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";

                xTokenRequestReceived = true;
            } else { // Second request
                UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "x-token");
                UNIT_ASSERT_VALUES_EQUAL(formData["access_token"], "xToken");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId2");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret2");
                response["token_type"] = "bearer";
                response["access_token"] = authToken;
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";
                response["uid"] = uid;

                oauthTokenRequestReceived = true;
            }
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        AuthEndpoint authEndpoint(getDeviceForTests(), ipcFactoryForTests());

        auto connector = createIpcConnectorForTests("authd");
        connector->connectToService();
        connector->waitUntilConnected();

        auto request = ipc::buildUniqueMessage([&authCode](auto& msg) {
            msg.mutable_add_user_request()->set_auth_code(TString(authCode));
            msg.mutable_add_user_request()->set_account_type(proto::AccountType::GUEST);
            msg.mutable_add_user_request()->set_with_xtoken(true);
        });
        auto response = connector->sendRequestSync(std::move(request), std::chrono::seconds(600));
        UNIT_ASSERT_VALUES_EQUAL(int(response->add_user_response().status()), int(proto::AddUserResponse::OK));
        UNIT_ASSERT_VALUES_EQUAL(response->add_user_response().auth_token(), authToken);
        UNIT_ASSERT_VALUES_UNEQUAL(response->add_user_response().xtoken(), "");

        UNIT_ASSERT(xTokenRequestReceived);
        UNIT_ASSERT(oauthTokenRequestReceived);

        UNIT_ASSERT_VALUES_EQUAL(response->add_user_response().id(), uid);
    }

    Y_UNIT_TEST(testAuthAddGuestUserWithoutXToken)
    {
        const std::string authCode = "auth_code";
        const std::string authToken = "oauthToken";
        const AccountStorage::AccountId uid = 1130000029536845L;

        bool oauthTokenRequestReceived = false;
        bool uidRequestReceived = false;

        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "POST");
            auto formData = parseHttpFormData(payload);
            Json::Value response;

            if (!oauthTokenRequestReceived) // First request
            {
                UNIT_ASSERT_VALUES_EQUAL(header.resource, "/token");

                UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "authorization_code");
                UNIT_ASSERT_VALUES_EQUAL(formData["code"], authCode);
                UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId2");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret2");
                response["token_type"] = "bearer";
                response["access_token"] = authToken;
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";

                oauthTokenRequestReceived = true;
            } else { // Second request
                UNIT_ASSERT_VALUES_EQUAL(header.resource, "/info");

                UNIT_ASSERT_VALUES_EQUAL(formData["format"], "json");
                UNIT_ASSERT_VALUES_EQUAL(header.getHeader("Authorization"), "OAuth " + authToken);
                response["id"] = std::to_string(uid);

                uidRequestReceived = true;
            }
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        AuthEndpoint authEndpoint(getDeviceForTests(), ipcFactoryForTests());

        auto connector = createIpcConnectorForTests("authd");
        connector->connectToService();
        connector->waitUntilConnected();

        auto request = ipc::buildUniqueMessage([&authCode](auto& msg) {
            msg.mutable_add_user_request()->set_auth_code(TString(authCode));
            msg.mutable_add_user_request()->set_account_type(proto::AccountType::GUEST);
            msg.mutable_add_user_request()->set_with_xtoken(false);
        });
        auto response = connector->sendRequestSync(std::move(request), std::chrono::seconds(600));
        UNIT_ASSERT_VALUES_EQUAL(int(response->add_user_response().status()), int(proto::AddUserResponse::OK));
        UNIT_ASSERT_VALUES_EQUAL(response->add_user_response().auth_token(), authToken);
        UNIT_ASSERT_VALUES_EQUAL(response->add_user_response().xtoken(), "");

        UNIT_ASSERT(oauthTokenRequestReceived);
        UNIT_ASSERT(uidRequestReceived);

        UNIT_ASSERT_VALUES_EQUAL(response->add_user_response().id(), uid);
    }

    Y_UNIT_TEST(testAuthAddUserCodeExpired)
    {
        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headrs*/, const std::string& /*payload*/,
                                          TestHttpServer::HttpConnection& handler) {
            Json::Value errorResponse;
            errorResponse["error_description"] = "Code has expired";
            errorResponse["error"] = "invalid_grant";
            handler.doReplay(400, "application/json", jsonToString(errorResponse));
        };

        AuthEndpoint authEndpoint(getDeviceForTests(), ipcFactoryForTests());

        auto connector = createIpcConnectorForTests("authd");
        connector->connectToService();
        connector->waitUntilConnected();

        {
            auto request = ipc::buildUniqueMessage([](auto& msg) {
                msg.mutable_add_user_request()->set_auth_code("auth_code");
                msg.mutable_add_user_request()->set_account_type(proto::AccountType::OWNER);
                msg.mutable_add_user_request()->set_with_xtoken(true);
            });
            auto response = connector->sendRequestSync(std::move(request), std::chrono::seconds(600));
            UNIT_ASSERT_VALUES_EQUAL(int(response->add_user_response().status()), int(proto::AddUserResponse::CODE_EXPIRED));
        }
    }

    Y_UNIT_TEST(testAuthAddUserNoInternet)
    {
        AuthEndpoint authEndpoint(getDeviceForTests(), ipcFactoryForTests());

        auto connector = createIpcConnectorForTests("authd");
        connector->connectToService();
        connector->waitUntilConnected();

        oauthServer.stop();

        auto request = ipc::buildUniqueMessage([](auto& msg) {
            msg.mutable_add_user_request()->set_auth_code("auth_code");
            msg.mutable_add_user_request()->set_account_type(proto::AccountType::OWNER);
        });
        auto response = connector->sendRequestSync(std::move(request), std::chrono::seconds(600));
        UNIT_ASSERT_VALUES_EQUAL(int(response->add_user_response().status()), int(proto::AddUserResponse::NO_INTERNET));
    }

    Y_UNIT_TEST(testAccountStorageLoad)
    {
        proto::AccountInfo firstAccount;
        firstAccount.set_id(9875);
        firstAccount.set_xtoken("x_token");
        firstAccount.set_auth_token("auth_token");
        firstAccount.set_refresh_token("refresh_token");

        proto::AccountInfo secondAccount;
        secondAccount.set_id(9876);
        secondAccount.set_xtoken("x_token2");
        secondAccount.set_auth_token("auth_token2");
        secondAccount.set_refresh_token("refresh_token2");
        secondAccount.set_type(proto::AccountType::OWNER);

        {
            AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());

            auto currentAccount = storage.getCurrentAccount();
            UNIT_ASSERT(!currentAccount.has_value()); // No accounts yet

            storage.addAccount(firstAccount);

            // We can't change account to non-admin account.
            UNIT_ASSERT_EXCEPTION(storage.changeAccount(firstAccount.id()), std::runtime_error);

            firstAccount.set_type(proto::AccountType::OWNER);
            storage.addAccount(firstAccount);

            storage.changeAccount(firstAccount.id());

            currentAccount = storage.getCurrentAccount();
            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), firstAccount.id());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->xtoken(), firstAccount.xtoken());
            UNIT_ASSERT_VALUES_EQUAL(int(currentAccount->type()), int(proto::AccountType::OWNER));

            storage.addAccount(secondAccount);
            storage.changeAccount(secondAccount.id());

            currentAccount = storage.getCurrentAccount();
            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), secondAccount.id());

            // ожидаем, что выбросится исключение, так как такого аккаунта у нас нет
            UNIT_ASSERT_EXCEPTION(storage.changeAccount(firstAccount.id()), std::runtime_error);

            // check that current account didn't change
            currentAccount = storage.getCurrentAccount();
            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), secondAccount.id());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->xtoken(), secondAccount.xtoken());
            UNIT_ASSERT_VALUES_EQUAL(int(currentAccount->type()), int(proto::AccountType::OWNER));
        }
        // Reload
        {
            AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
            auto currentAccount = storage.getCurrentAccount();

            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), secondAccount.id());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->xtoken(), secondAccount.xtoken());
            UNIT_ASSERT_VALUES_EQUAL(int(currentAccount->type()), int(proto::AccountType::OWNER));

            // ожидаем, что выбросится исключение, так как такого аккаунта у нас нет
            UNIT_CHECK_GENERATED_EXCEPTION(storage.changeAccount(firstAccount.id()), std::exception);

            // check that current account didn't change
            currentAccount = storage.getCurrentAccount();
            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), secondAccount.id());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->xtoken(), secondAccount.xtoken());
        }
        // Reload
        {
            AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
            auto currentAccount = storage.getCurrentAccount();

            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), secondAccount.id());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->xtoken(), secondAccount.xtoken());

            storage.addAccount(firstAccount);
            storage.changeAccount(firstAccount.id(), false);

            currentAccount = storage.getCurrentAccount();
            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), firstAccount.id());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->xtoken(), firstAccount.xtoken());

            std::vector<AccountStorage::AccountId> expectedAccountsIds{
                firstAccount.id(),
                secondAccount.id(),
            };

            const std::vector<proto::AccountInfo> accounts = storage.getAllAccounts();
            UNIT_ASSERT_VALUES_EQUAL(expectedAccountsIds.size(), accounts.size());
            ASSERT_THAT(expectedAccountsIds, ::testing::ElementsAre(accounts.front().id(), accounts.back().id()));
        }
        // Reload
        {
            AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
            auto currentAccount = storage.getCurrentAccount();

            UNIT_ASSERT(currentAccount.has_value());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->id(), firstAccount.id());
            UNIT_ASSERT_VALUES_EQUAL(currentAccount->xtoken(), firstAccount.xtoken());

            std::vector<AccountStorage::AccountId> expectedAccountsIds{
                firstAccount.id(),
                secondAccount.id(),
            };

            const std::vector<proto::AccountInfo> accounts = storage.getAllAccounts();
            UNIT_ASSERT_VALUES_EQUAL(expectedAccountsIds.size(), accounts.size());
            ASSERT_THAT(expectedAccountsIds, ::testing::ElementsAre(accounts.front().id(), accounts.back().id()));
        }
    }

    Y_UNIT_TEST(testAccountAuthTokenChanged) {
        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        std::string new_auth_token{"new_auth_token"};

        bool onNewAuthTokenSet{false};

        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        storage.onTokenChange = [&new_auth_token, &onNewAuthTokenSet](const proto::AccountInfo& newAccount) {
            onNewAuthTokenSet = (newAccount.auth_token() == new_auth_token);
        };

        auto currentAccount = storage.getCurrentAccount();
        UNIT_ASSERT(!currentAccount.has_value()); // No accounts yet

        storage.addAccount(account);
        storage.changeAccount(account.id());

        account.set_auth_token(TString(new_auth_token));

        storage.addAccount(account);
        storage.changeAccount(account.id());

        UNIT_ASSERT(onNewAuthTokenSet);
    }

    Y_UNIT_TEST(testAccountChanged) {
        proto::AccountInfo account;
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        const int old_id{9875};
        const int new_id{8888};

        bool onNewAccountSet{false};
        bool onOldAccountDeleted{false};

        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        auto currentAccount = storage.getCurrentAccount();
        UNIT_ASSERT(!currentAccount.has_value()); // No accounts yet

        storage.onAccountChange = [old_id](const proto::AccountInfo& oldAccount) {
            UNIT_ASSERT_VALUES_EQUAL(static_cast<int>(oldAccount.id()), old_id);
        };
        storage.onAccountDeleted = [](const proto::AccountInfo& oldAccount) {
            Y_UNUSED(oldAccount);
            UNIT_ASSERT(false);
        };

        account.set_id(old_id);

        storage.addAccount(account);
        storage.changeAccount(account.id());

        storage.onAccountChange = [new_id, &onNewAccountSet](const proto::AccountInfo& newAccount) {
            UNIT_ASSERT(!onNewAccountSet);
            UNIT_ASSERT_VALUES_EQUAL(static_cast<int>(newAccount.id()), new_id);
            onNewAccountSet = true;
        };
        storage.onAccountDeleted = [old_id, &onOldAccountDeleted](const proto::AccountInfo& oldAccount) {
            UNIT_ASSERT(!onOldAccountDeleted);
            UNIT_ASSERT_VALUES_EQUAL(static_cast<int>(oldAccount.id()), old_id);
            onOldAccountDeleted = true;
        };

        account.set_id(new_id);

        storage.addAccount(account);
        storage.changeAccount(account.id());

        UNIT_ASSERT(onNewAccountSet);
        UNIT_ASSERT(onOldAccountDeleted);
    }

    Y_UNIT_TEST(testRefreshAccount) {
        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        auto newRefreshTimestamp = 1600000099;

        bool onRefreshAccount{false};

        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        storage.onAccountRefresh = [&newRefreshTimestamp, &onRefreshAccount](const proto::AccountInfo& newAccount) {
            onRefreshAccount = (newAccount.last_token_refresh_timestamp() == newRefreshTimestamp &&
                                newAccount.id() == 9875 &&
                                newAccount.xtoken() == "x_token" &&
                                newAccount.auth_token() == "auth_token" &&
                                newAccount.refresh_token() == "refresh_token");
        };

        auto currentAccount = storage.getCurrentAccount();
        storage.addAccount(account);
        storage.changeAccount(account.id());

        UNIT_ASSERT(!onRefreshAccount);

        storage.addAccount(account, AccountStorage::TimePoint() + std::chrono::seconds{newRefreshTimestamp});
        storage.changeAccount(account.id());

        UNIT_ASSERT(onRefreshAccount);
    }

    Y_UNIT_TEST(testAccountStorageAddAccountByCode) {
        const std::string authCode = "auth_code";
        constexpr auto UID = 1130000029536845L;
        const std::string OAUTH_TOKEN = "authToken";
        const std::string XTOKEN = "xToken";

        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/token");
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "POST");
            auto formData = parseHttpFormData(payload);
            Json::Value response;

            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = "refreshToken";
            if (formData.count("code")) {
                UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "authorization_code");
                UNIT_ASSERT_VALUES_EQUAL(formData["code"], authCode);
                UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId1");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret1");
                response["token_type"] = "bearer";
                response["access_token"] = XTOKEN;
            } else if (formData.count("access_token")) { // Second request
                UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "x-token");
                UNIT_ASSERT_VALUES_EQUAL(formData["access_token"], "xToken");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId2");
                UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret2");
                response["token_type"] = "bearer";
                response["access_token"] = OAUTH_TOKEN;
                response["uid"] = UID;
            }
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        proto::AccountInfo resultInfo;
        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        const auto result = storage.requestAccount(authCode, proto::AccountType::OWNER, true, resultInfo);
        UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::OK));

        UNIT_ASSERT_VALUES_EQUAL(resultInfo.xtoken(), XTOKEN);
        UNIT_ASSERT_VALUES_EQUAL(resultInfo.auth_token(), OAUTH_TOKEN);
        UNIT_ASSERT_VALUES_EQUAL(resultInfo.id(), UID);
    }

    Y_UNIT_TEST(testAccountStorageAddAccountByCodeRetriesInvalidGrantGood) {
        /**
         * @NOTE: Check that Account Storage retries 400 + "invalid_grant" error
         */
        constexpr auto UID = 1130000029536845L;
        const std::string OAUTH_TOKEN = "authToken";
        const std::string XTOKEN = "xToken";

        std::atomic_int attempts{0};
        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            const auto formData = parseHttpFormData(payload);
            Json::Value response;

            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = "refreshToken";
            if (formData.count("code")) {
                response["token_type"] = "bearer";
                response["access_token"] = XTOKEN;
            } else if (formData.count("access_token")) { // Second request
                if (++attempts < 3) {
                    response["error"] = "invalid_grant";
                    response["error_description"] = "some error_description";
                    handler.doReplay(400, "application/json", jsonToString(response));
                    return;
                }
                response["token_type"] = "bearer";
                response["access_token"] = OAUTH_TOKEN;
                response["uid"] = UID;
            }
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        proto::AccountInfo resultInfo;
        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        const auto result = storage.requestAccount("auth_code", proto::AccountType::OWNER, true, resultInfo);
        UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::OK));
        /* Check that make 3 expected requests */
        UNIT_ASSERT_VALUES_EQUAL(attempts.load(), 3);
        UNIT_ASSERT_VALUES_EQUAL(resultInfo.xtoken(), XTOKEN);
        UNIT_ASSERT_VALUES_EQUAL(resultInfo.auth_token(), OAUTH_TOKEN);
        UNIT_ASSERT_VALUES_EQUAL(resultInfo.id(), UID);
    }

    Y_UNIT_TEST(testAccountStorageAddAccountByCodeRetriesInvalidGrantBad) {
        /**
         * @NOTE: Check that Account Storage retries 400 + "invalid_grant" error, but stops
         *        after 3d attempt
         */
        std::atomic_int attempts{0};
        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            const auto formData = parseHttpFormData(payload);
            Json::Value response;

            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = "refreshToken";
            if (formData.count("code")) {
                response["token_type"] = "bearer";
                response["access_token"] = "xToken";
                handler.doReplay(200, "application/json", jsonToString(response));
            } else if (formData.count("access_token")) { // Second request
                ++attempts;
                response["error"] = "invalid_grant";
                response["error_description"] = "some error_description";
                handler.doReplay(400, "application/json", jsonToString(response));
            }
        };

        proto::AccountInfo resultInfo;
        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        const auto result = storage.requestAccount("auth_code", proto::AccountType::OWNER, true, resultInfo);
        UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::WRONG_TOKEN));
        /* Make sure that AccountStorage does not retry infinitely and stops after 3 attempts */
        UNIT_ASSERT_VALUES_EQUAL(attempts.load(), 3);
    }

    Y_UNIT_TEST(testAccountStorageAddAccountByCodeXtokenChanged) {
        const std::string authCode = "auth_code";
        constexpr auto UID = 1130000029536845L;
        const std::string OAUTH_TOKEN = "authToken";
        const std::string XTOKEN = "xToken";
        const std::string NEW_XTOKEN = "newXToken";
        const std::string REFRESH_TOKEN = "refreshToken";

        proto::AccountInfo account;
        account.set_id(UID);
        account.set_xtoken(TString(XTOKEN));
        account.set_auth_token(TString(OAUTH_TOKEN));
        account.set_refresh_token(TString(REFRESH_TOKEN));
        account.set_type(proto::AccountType::OWNER);

        bool onNewXTokenSet{false};
        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        storage.onTokenChange = [&NEW_XTOKEN, &onNewXTokenSet](const proto::AccountInfo& newAccount) {
            onNewXTokenSet = (newAccount.xtoken() == NEW_XTOKEN);
        };

        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/token");
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "POST");
            auto formData = parseHttpFormData(payload);
            Json::Value response;

            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = REFRESH_TOKEN;
            if (formData.count("code")) {
                response["token_type"] = "bearer";
                response["access_token"] = NEW_XTOKEN;
            } else if (formData.count("access_token")) { // Second request
                response["token_type"] = "bearer";
                response["access_token"] = OAUTH_TOKEN;
                response["uid"] = UID;
            }
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        auto currentAccount = storage.getCurrentAccount();
        UNIT_ASSERT(!currentAccount.has_value()); // No accounts yet

        storage.addAccount(account);
        storage.changeAccount(account.id());

        proto::AccountInfo resultInfo;
        storage.requestAccount(authCode, proto::AccountType::OWNER, true, resultInfo);
        storage.changeAccount(account.id());

        UNIT_ASSERT(onNewXTokenSet);
    }

    Y_UNIT_TEST(testAccountStorageAddAccountByCodeDontRetriesBackendErrors) {
        /**
         * @NOTE: Check that Account Storage do not retry backend errors (only "invalid_grant" should be retried)
         */
        std::atomic_int attempts{0};
        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            const auto formData = parseHttpFormData(payload);
            Json::Value response;

            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = "refreshToken";
            if (formData.count("code")) {
                response["token_type"] = "bearer";
                response["access_token"] = "xToken";
                handler.doReplay(200, "application/json", jsonToString(response));
            } else if (formData.count("access_token")) { // Second request
                ++attempts;
                response["error"] = "some error";
                response["error_description"] = "some error_description";
                handler.doReplay(503, "application/json", jsonToString(response));
            }
        };

        proto::AccountInfo resultInfo;
        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());
        const auto result = storage.requestAccount("auth_code", proto::AccountType::OWNER, true, resultInfo);
        UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::WRONG_TOKEN));
        /* Make sure that AccountStorage do not retries backend errors (invalid_grant only) */
        UNIT_ASSERT_VALUES_EQUAL(attempts.load(), 1);
    }

    Y_UNIT_TEST(testAccountStorageUpdateOAuthToken) {
        const AccountStorage::AccountId UID = 1130000029536845L;
        const std::string OAUTH_TOKEN = "authToken";
        const std::string NEW_OAUTH_TOKEN = "newAuthToken";
        const std::string XTOKEN = "xToken";

        proto::AccountInfo ownerAccount;
        ownerAccount.set_id(UID);
        ownerAccount.set_xtoken(TString(XTOKEN));
        ownerAccount.set_auth_token(TString(OAUTH_TOKEN));
        ownerAccount.set_type(proto::AccountType::OWNER);

        AccountStorage storage(getDeviceForTests(), std::make_shared<MockCallbackQueue>());

        {
            proto::AccountInfo guestAccount;
            guestAccount.set_id(4242);
            guestAccount.set_xtoken("some_xtoken");
            guestAccount.set_auth_token("some_token");
            guestAccount.set_type(proto::AccountType::GUEST);

            storage.addAccount(guestAccount);

            const auto result = storage.updateOAuthToken(guestAccount.auth_token());
            UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::WRONG_TOKEN));
        }

        {
            proto::AccountInfo ownerAccountWithoutXToken;
            ownerAccountWithoutXToken.set_id(42);
            ownerAccountWithoutXToken.set_auth_token("other_token");
            ownerAccountWithoutXToken.set_type(proto::AccountType::OWNER);

            storage.addAccount(ownerAccountWithoutXToken);

            const auto result = storage.updateOAuthToken(ownerAccountWithoutXToken.auth_token());
            UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::WRONG_TOKEN));
        }

        storage.addAccount(ownerAccount);

        storage.changeAccount(ownerAccount.id());
        ownerAccount = storage.getCurrentAccount().value();

        {
            const auto result = storage.updateOAuthToken("wrongAuthToken");
            UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::WRONG_TOKEN));
        }

        oauthServer.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& payload,
                                          TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/token");
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "POST");
            auto formData = parseHttpFormData(payload);

            Json::Value response;
            UNIT_ASSERT_VALUES_EQUAL(formData["grant_type"], "x-token");
            UNIT_ASSERT_VALUES_EQUAL(formData["access_token"], XTOKEN);
            UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], "clientId2");
            UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], "clientSecret2");
            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = "refreshToken";
            response["token_type"] = "bearer";
            response["access_token"] = NEW_OAUTH_TOKEN;
            response["uid"] = UID;
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        storage.onTokenChange = [&](const proto::AccountInfo& newAccount) {
            UNIT_ASSERT_VALUES_EQUAL(newAccount.auth_token(), NEW_OAUTH_TOKEN);
            UNIT_ASSERT_VALUES_EQUAL(ownerAccount.xtoken(), newAccount.xtoken());
            UNIT_ASSERT_VALUES_EQUAL(ownerAccount.id(), newAccount.id());
            UNIT_ASSERT_LT(ownerAccount.last_token_refresh_timestamp(), newAccount.last_token_refresh_timestamp());
        };

        // Sleep for 1 second to check that last_token_refresh_timestamp will be updated.
        std::this_thread::sleep_for(std::chrono::seconds{1});

        const auto result = storage.updateOAuthToken(ownerAccount.auth_token());
        UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::OK));
    }

    Y_UNIT_TEST(testAccountStorageScheduleXTokenProlongationOnAddAccount) {
        // We have to schedule x-token prolongation for added account.

        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            storage.addAccount(account);
            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            auto& [timeout, callback] = *callbackQueue->delayedCallbacks.begin();
            UNIT_ASSERT_GE(timeout, std::chrono::days{31});
        }
    }

    Y_UNIT_TEST(testAccountStorageScheduleXTokenProlongationOnCall) {
        // There might be 2 cases:
        // - last_token_refresh_timestamp is too old. In that case we schedule x-token prolongation during 24 hours.
        // - last_token_refresh_timestamp is not so old. We schedule x-token prolongation in a month after last_token_refresh_timestamp.

        proto::AccountInfo newAccount;
        newAccount.set_id(9875);
        newAccount.set_xtoken("x_token");
        newAccount.set_auth_token("auth_token");
        newAccount.set_refresh_token("refresh_token");
        newAccount.set_type(proto::AccountType::OWNER);

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            storage.addAccount(newAccount);
            storage.changeAccount(newAccount.id());
        }

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 0);

            storage.scheduleProlongAccounts();

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            auto& [timeout, callback] = *callbackQueue->delayedCallbacks.begin();
            UNIT_ASSERT_GE(timeout, std::chrono::days{31});
        }

        proto::AccountInfo oldAccount;
        oldAccount.set_id(9999);
        oldAccount.set_xtoken("x_token");
        oldAccount.set_auth_token("auth_token");
        oldAccount.set_refresh_token("refresh_token");
        oldAccount.set_type(proto::AccountType::OWNER);

        const auto now = std::chrono::time_point_cast<AccountStorage::Duration>(std::chrono::system_clock::now());
        const auto lastRefresh = now - std::chrono::days{60};

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 0);

            storage.scheduleProlongAccounts();

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            callbackQueue->runAll(std::chrono::days{32});

            storage.addAccount(oldAccount, lastRefresh);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            auto& [timeout, callback] = *callbackQueue->delayedCallbacks.begin();
            UNIT_ASSERT_LE(timeout - callbackQueue->now, std::chrono::days{1});

            storage.changeAccount(oldAccount.id());
        }

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 0);

            storage.scheduleProlongAccounts();

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            auto& [timeout, callback] = *callbackQueue->delayedCallbacks.begin();
            UNIT_ASSERT_LE(timeout, std::chrono::days{1});
        }
    }

    Y_UNIT_TEST(testAccountStorageScheduleXTokenProlongationOnAccountUpdate) {
        // We have to reschedule x-token prolongation on successful account update request.

        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        const auto now = std::chrono::time_point_cast<AccountStorage::Duration>(std::chrono::system_clock::now());
        const auto lastRefresh = now - std::chrono::days{5};

        std::atomic<int> updateOAuthTokenRequestCounter{0};
        oauthServer.onHandlePayload = [&account, &updateOAuthTokenRequestCounter](const TestHttpServer::Headers& /*headers*/, const std::string& payload, TestHttpServer::HttpConnection& handler)
        {
            ++updateOAuthTokenRequestCounter;

            auto formData = parseHttpFormData(payload);

            Json::Value response;
            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = "refreshToken";
            response["token_type"] = "bearer";
            response["access_token"] = "newAuthToken";
            response["uid"] = account.id();
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            storage.addAccount(account, lastRefresh);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            const auto prolongTimeoutOnAdd = callbackQueue->delayedCallbacks.begin()->first;

            const auto result = storage.updateOAuthToken(account.auth_token());
            UNIT_ASSERT_VALUES_EQUAL(int(result), int(AccountStorage::ErrorCode::OK));

            // We initiated update request and this led to addAccount, so we added a new callback for x-token prolongation.
            UNIT_ASSERT_VALUES_EQUAL(updateOAuthTokenRequestCounter.load(), 1);
            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 2);

            // New callback have to have timeout bigger than previous one.
            const auto prolongTimeoutOnUpdate = (++callbackQueue->delayedCallbacks.begin())->first;
            UNIT_ASSERT_LT(prolongTimeoutOnAdd, prolongTimeoutOnUpdate);

            // Run the old callback and check that it doesn't trigger OAuthToken request update.
            callbackQueue->runAll(prolongTimeoutOnAdd);
            UNIT_ASSERT_VALUES_EQUAL(updateOAuthTokenRequestCounter.load(), 1);
        }
    }

    Y_UNIT_TEST(testAccountStorageRescheduleXTokenProlongationWhenAccountInfoWasChanged) {
        // last_token_refresh_timestamp was changed between the moment current callback was added and the moment it's executed.
        // It means that we have more recent callback and we should skip current callback.

        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        const auto now = std::chrono::time_point_cast<AccountStorage::Duration>(std::chrono::system_clock::now());
        const auto lastRefresh = now - std::chrono::days{5};

        std::atomic<int> updateOAuthTokenRequestCounter = 0;
        oauthServer.onHandlePayload = [&account, &updateOAuthTokenRequestCounter](const TestHttpServer::Headers& /*headers*/, const std::string& payload, TestHttpServer::HttpConnection& handler)
        {
            ++updateOAuthTokenRequestCounter;

            auto formData = parseHttpFormData(payload);

            Json::Value response;
            response["expires_in"] = 365 * 24 * 3600; // 1 year
            response["refresh_token"] = "refreshToken";
            response["token_type"] = "bearer";
            response["access_token"] = "newAuthToken";
            response["uid"] = account.id();
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            storage.addAccount(account, lastRefresh);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);
            const auto prolongTimeoutOnAdd1 = callbackQueue->delayedCallbacks.begin()->first;

            storage.addAccount(account);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 2);

            const auto prolongTimeoutOnAdd2 = (++callbackQueue->delayedCallbacks.begin())->first;

            UNIT_ASSERT_LT(prolongTimeoutOnAdd1, prolongTimeoutOnAdd2);

            // Run the old callback and check that it doesn't trigger OAuthToken request update.
            callbackQueue->runAll(prolongTimeoutOnAdd1);
            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(updateOAuthTokenRequestCounter.load(), 0);

            // Run the new callback and check that it triggers OAuthToken request update.
            callbackQueue->runAll(prolongTimeoutOnAdd2 - prolongTimeoutOnAdd1);
            UNIT_ASSERT_VALUES_EQUAL(updateOAuthTokenRequestCounter.load(), 1);
        }
    }

    Y_UNIT_TEST(testAccountStorageRescheduleXTokenProlongationOnNoInternet) {
        // There is no internet - we have to reschedule x-token prolongation.

        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        std::atomic<int> updateOAuthTokenRequestCounter = 0;
        oauthServer.onHandlePayload = [&updateOAuthTokenRequestCounter](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& /*connection*/)
        {
            ++updateOAuthTokenRequestCounter;
            throw std::runtime_error{"Emulate NO_INTERNET"};
        };

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            storage.addAccount(account);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            auto timeout = callbackQueue->delayedCallbacks.begin()->first;
            callbackQueue->runAll(timeout);

            // refreshAccountOAuthToken tries to make a request 3 times in case of an error.
            UNIT_ASSERT_VALUES_EQUAL(updateOAuthTokenRequestCounter.load(), 3);
            // NO_INTERNET error had to add new callback.
            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            timeout = callbackQueue->delayedCallbacks.begin()->first;
            UNIT_ASSERT_EQUAL(timeout - callbackQueue->now, std::chrono::seconds{300});

            callbackQueue->runAll(timeout);

            UNIT_ASSERT_VALUES_EQUAL(updateOAuthTokenRequestCounter.load(), 6);
            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);
        }
    }

    Y_UNIT_TEST(testAccountStorageRescheduleXTokenProlongationOnWrongToken) {
        // Trying to update account with a wrong token.

        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        std::atomic<int> updateOAuthTokenRequestCounter = 0;
        oauthServer.onHandlePayload = [&updateOAuthTokenRequestCounter](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler)
        {
            ++updateOAuthTokenRequestCounter;

            Json::Value errorResponse;
            errorResponse["error_description"] = "Some error description";
            errorResponse["error"] = "some_error";
            handler.doReplay(400, "application/json", jsonToString(errorResponse));
        };

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            AccountStorage storage(getDeviceForTests(), callbackQueue);

            storage.addAccount(account);

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

            auto timeout = callbackQueue->delayedCallbacks.begin()->first;
            callbackQueue->runAll(timeout);

            UNIT_ASSERT_VALUES_EQUAL(updateOAuthTokenRequestCounter.load(), 1);

            // Check that we don't add new callback on WRONG_TOKEN error.
            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 0);
        }
    }

    Y_UNIT_TEST(testAccountStorageRefreshAccountOAuthTokenBackoffValues) {
        proto::AccountInfo account;
        account.set_id(9875);
        account.set_xtoken("x_token");
        account.set_auth_token("auth_token");
        account.set_refresh_token("refresh_token");
        account.set_type(proto::AccountType::OWNER);

        oauthServer.onHandlePayload = [](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& /*handler*/)
        {
            throw std::runtime_error("No internet");
        };

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            auto device = getDeviceForTests();
            AccountStorage storage(device, callbackQueue);

            storage.addAccount(account);

            // Clear prolongation callback.
            callbackQueue->clear();

            UNIT_ASSERT_VALUES_EQUAL(int(storage.updateOAuthToken(account.auth_token())), int(AccountStorage::ErrorCode::NO_INTERNET));

            const std::vector<std::uint64_t> backoffIntervalsMin{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 44640, 44640};

            std::chrono::milliseconds previousTimeout{};
            for (const auto interval : backoffIntervalsMin) {
                UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 1);

                const auto callbackTimeout = callbackQueue->delayedCallbacks.begin()->first - previousTimeout;
                UNIT_ASSERT_VALUES_EQUAL(interval * 60 * 1000, callbackTimeout.count());
                callbackQueue->runAll(callbackTimeout);

                previousTimeout += callbackTimeout;
            }
        }
    }

    Y_UNIT_TEST(testAccountStorageDeleteAccount) {
        proto::AccountInfo owner;
        owner.set_id(9875);
        owner.set_xtoken("x_token1");
        owner.set_auth_token("auth_token1");
        owner.set_refresh_token("refresh_token1");
        owner.set_type(proto::AccountType::OWNER);

        proto::AccountInfo guest;
        guest.set_id(9999);
        guest.set_xtoken("x_token2");
        guest.set_auth_token("auth_token2");
        guest.set_refresh_token("refresh_token2");
        guest.set_type(proto::AccountType::GUEST);

        {
            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            auto device = getDeviceForTests();
            AccountStorage storage(device, callbackQueue);

            UNIT_ASSERT_VALUES_EQUAL(int(storage.deleteAccount(42)), int(AccountStorage::ErrorCode::WRONG_USER));
        }

        {
            oauthServer.onHandlePayload = [&owner](const TestHttpServer::Headers& /*headers*/, const std::string& payload, TestHttpServer::HttpConnection& handler)
            {
                auto formData = parseHttpFormData(payload);

                Json::Value response;
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";
                response["token_type"] = "bearer";
                response["access_token"] = "newAuthToken";
                response["uid"] = owner.id();
                handler.doReplay(200, "application/json", jsonToString(response));
            };

            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            auto device = getDeviceForTests();
            AccountStorage storage(device, callbackQueue);

            storage.addAccount(owner);
            storage.changeAccount(owner.id());

            // Clear prolongation callback.
            callbackQueue->clear();

            UNIT_ASSERT_VALUES_EQUAL(int(storage.deleteAccount(owner.id())), int(AccountStorage::ErrorCode::WRONG_USER));
        }

        {
            oauthServer.onHandlePayload = [&owner](const TestHttpServer::Headers& /*headers*/, const std::string& payload, TestHttpServer::HttpConnection& handler)
            {
                auto formData = parseHttpFormData(payload);

                Json::Value response;
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";
                response["token_type"] = "bearer";
                response["access_token"] = "newAuthToken";
                response["uid"] = owner.id();
                handler.doReplay(200, "application/json", jsonToString(response));
            };

            auto callbackQueue = std::make_shared<MockCallbackQueue>();
            auto device = getDeviceForTests();
            AccountStorage storage(device, callbackQueue);

            storage.addAccount(owner);
            storage.changeAccount(owner.id());

            // Clear prolongation callback.
            callbackQueue->clear();

            oauthServer.onHandlePayload = [&guest](const TestHttpServer::Headers& /*headers*/, const std::string& payload, TestHttpServer::HttpConnection& handler)
            {
                auto formData = parseHttpFormData(payload);

                Json::Value response;
                response["expires_in"] = 365 * 24 * 3600; // 1 year
                response["refresh_token"] = "refreshToken";
                response["token_type"] = "bearer";
                response["access_token"] = "newAuthToken";
                response["uid"] = guest.id();
                handler.doReplay(200, "application/json", jsonToString(response));
            };

            storage.addAccount(guest);

            // Clear prolongation callback.
            callbackQueue->clear();

            UNIT_ASSERT_VALUES_EQUAL(int(storage.deleteAccount(guest.id())), int(AccountStorage::ErrorCode::OK));

            UNIT_ASSERT_VALUES_EQUAL(callbackQueue->size(), 2);

            oauthServer.onHandlePayload = [this, &guest](const TestHttpServer::Headers& header, const std::string& payload, TestHttpServer::HttpConnection& handler)
            {
                UNIT_ASSERT_VALUES_EQUAL(header.resource, "/revoke_token");

                auto formData = parseHttpFormData(payload);
                const auto config = getDeviceForTests()->configuration()->getServiceConfig(AuthEndpoint::SERVICE_NAME);

                if (formData["access_token"] == guest.auth_token()) {
                    UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], config["authTokenClientId"].asString());
                    UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], config["authTokenClientSecret"].asString());
                } else if (formData["access_token"] == guest.xtoken()) {
                    UNIT_ASSERT_VALUES_EQUAL(formData["client_id"], config["xTokenClientId"].asString());
                    UNIT_ASSERT_VALUES_EQUAL(formData["client_secret"], config["xTokenClientSecret"].asString());
                } else {
                    UNIT_FAIL("Wrong access_token " + formData["access_token"]);
                }

                Json::Value response;
                response["uid"] = guest.id();
                handler.doReplay(200, "application/json", jsonToString(response));
            };

            // Run both revoke_token callbacks
            callbackQueue->runAll(std::chrono::milliseconds::zero());
        }
    }
}
