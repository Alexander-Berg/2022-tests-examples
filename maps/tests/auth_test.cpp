#include <maps/indoor/long-tasks/src/evotor/include/oauth.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::mirc::evotor::auth::tests {

Y_UNIT_TEST_SUITE(oauth_token)
{

Y_UNIT_TEST(holder)
{
    const std::string dataRef = "12341";
    const std::chrono::seconds expiresIn{impl::TOKEN_EXPIRATION_OVERLAP * 10};
    impl::TokenHolder holder(Scope::GetUnexpiredIds, expiresIn, dataRef);
    UNIT_ASSERT_VALUES_EQUAL(holder.scope(), Scope::GetUnexpiredIds);
    UNIT_ASSERT(!holder.isExpiring());
    UNIT_ASSERT_VALUES_EQUAL(holder.get(), dataRef);
}

Y_UNIT_TEST(holder_expiring)
{
    const std::chrono::seconds expiresIn{impl::TOKEN_EXPIRATION_OVERLAP / 2};
    impl::TokenHolder holder(Scope::GetUnexpiredIds, expiresIn, "123");
    UNIT_ASSERT(holder.isExpiring());
}

Y_UNIT_TEST(storage)
{
    class TestTokenStorage : public TokenStorageBase {
    public:
        explicit TestTokenStorage(std::string tokenData)
            : tokenData_(std::move(tokenData))
        {}

    private:
        impl::TokenHolder makeNewToken(Scope scope) override
        {
            const int expiresIn = impl::TOKEN_EXPIRATION_OVERLAP.count() * 10;

            static int requestsCount = 0;
            UNIT_ASSERT_VALUES_EQUAL(requestsCount++, 0); // should be called only once
            return {scope, std::chrono::seconds(expiresIn), tokenData_};
        }

        const std::string tokenData_;
    };

    const std::string tokenData = "1234221";
    const auto scope = Scope::SaveDeviceLocation;

    TestTokenStorage storage(tokenData);
    UNIT_ASSERT_VALUES_EQUAL(storage.getToken(scope), tokenData);
    UNIT_ASSERT_VALUES_EQUAL(storage.getToken(scope), tokenData);
}

Y_UNIT_TEST(storage_reloads_expiring_token)
{
    class TestTokenStorage : public TokenStorageBase {
    public:
        TestTokenStorage(std::string tokenData1, std::string tokenData2)
            : tokenData1_(std::move(tokenData1))
            , tokenData2_(std::move(tokenData2))
        {}

    private:
        impl::TokenHolder makeNewToken(Scope scope) override
        {
            const int expiresIn = impl::TOKEN_EXPIRATION_OVERLAP.count() / 2;

            static int requestsCount = 0;
            auto data = requestsCount++ == 0 ? tokenData1_ : tokenData2_;
            return {scope, std::chrono::seconds(expiresIn), std::move(data)};
        }

        const std::string tokenData1_;
        const std::string tokenData2_;
    };


    const auto scope = Scope::SaveDeviceLocation;
    const std::string tokenData = "token-data-1";
    const std::string tokenData2 = "token-data-2";

    TestTokenStorage storage(tokenData, tokenData2);
    UNIT_ASSERT_VALUES_EQUAL(storage.getToken(scope), tokenData);
    UNIT_ASSERT_VALUES_EQUAL(storage.getToken(scope), tokenData2);
}

}

} // namespace maps::mirc::evotor::auth::tests
