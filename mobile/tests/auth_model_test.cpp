#include <yandex/maps/navikit/auth/account.h>
#include <yandex/maps/navikit/auth/auth_model.h>
#include <yandex/maps/navikit/auth/auth_model_creator.h>
#include <yandex/maps/navikit/check_context.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/auth/account.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navikit::auth::tests {

namespace {

const std::string USER_LOGIN = "myLogin";
const std::string USER_UID = "01234567-89AB-CDEF";

class RuntimeAccount : public runtime::auth::Account {
public:
    RuntimeAccount(const std::string& uid) : uid_(uid) {}

    virtual boost::optional<std::string> httpAuth(const std::string& /*token*/) override { ASSERT(false); }
    virtual std::string uid() override { return uid_; }
    virtual void invalidateToken(const std::string& /*token*/) override { ASSERT(false); }

    virtual void requestToken(std::unique_ptr<runtime::auth::TokenListener> tokenListener) override
    {
        assertNotUi();
        tokenListener->onTokenReceived("token:" + uid_);
    }

private:
    const std::string uid_;
};

class NavikitAccount : public navikit::auth::Account {
public:
    NavikitAccount(const std::string& login, const std::string& uid)
    : login_(login)
    , account_(std::make_shared<RuntimeAccount>(uid)) {}

    virtual const std::shared_ptr<::yandex::maps::runtime::auth::Account> account() const override
    {
        return account_;
    }

    virtual boost::any yandexAccount() const override { ASSERT(false); }

    virtual boost::optional<std::string> username() const override { return login_; }

    virtual boost::optional<std::string> email() const override { ASSERT(false); }

    virtual std::int64_t uid() const override { ASSERT(false); }

    virtual bool isStaff() const override { ASSERT(false); }

    virtual bool isBetaTester() const override { ASSERT(false); }

    virtual bool isPlus() const override { ASSERT(false); }

    virtual bool isSocial() const override { ASSERT(false); }

    virtual const std::shared_ptr<::yandex::maps::navikit::auth::AuthUrlListener> platformUrlListener(
        const std::shared_ptr<::yandex::maps::navikit::auth::NativeAuthUrlListener>& /*nativeListener*/) override
    {
        ASSERT(false);
    }

    virtual void requestAuthUrl(
        const std::string& /*redirectUrl*/,
        const std::shared_ptr<::yandex::maps::navikit::auth::AuthUrlListener>& /*urlListener*/) override
    {
        ASSERT(false);
    }

private:
    const std::string login_;
    const std::shared_ptr<RuntimeAccount> account_;
};


class AuthTestFixture {
protected:
    AuthTestFixture() = default;

    virtual ~AuthTestFixture()
    {
        runtime::async::ui()->spawn([&] { tearDown(); }).wait();
    }

    void tearDown() {
        authModel_.reset();
        account_.reset();
    }

    void setCurrentAccount(const std::string& login, const std::string& uid)
    {
        account_ = std::make_shared<NavikitAccount>(login, uid);
    }

    void initAuthModel()
    {
        authModel_ = createAuthModel(account_);
    }

    std::shared_ptr<navikit::auth::Account> account_;
    std::shared_ptr<AuthModel> authModel_;
};

}  // namespace

BOOST_FIXTURE_TEST_SUITE(auth, AuthTestFixture)

BOOST_AUTO_TEST_CASE(StartAuthWithoutAccount)
{
    runtime::async::ui()
        ->spawn([this] {
            initAuthModel();
            BOOST_CHECK(!authModel_->username());
        })
        .wait();
}

BOOST_AUTO_TEST_CASE(StartAuthWithAccount)
{
    runtime::async::ui()
        ->spawn([this] {
            setCurrentAccount(USER_LOGIN, USER_UID);
            initAuthModel();
            BOOST_CHECK(authModel_->username() == USER_LOGIN);
        })
        .wait();
}

BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex
