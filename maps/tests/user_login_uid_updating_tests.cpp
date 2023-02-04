/// @file user_login_uid_updating_tests.cpp
/// Tests for the logic of propagating uid and login changes into the DB

#include <maps/b2bgeo/identity/backend/libs/common/exceptions.h>
#include <maps/b2bgeo/identity/backend/libs/db/memory_storage.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <memory>

namespace maps::b2bgeo::identity {

const Apikey TEST_APIKEY{"f04c6079-4385-45e5-8d34-c19f42dea79e"};

auto createTestStorage()
{
    return std::make_unique<MemoryStorage>();
}

struct Query {
    std::string login;
    std::string uid;
    bool should_throw = false;
};

void testCreateUserAndQuery(
    IStorage& storage,
    const std::string& login,
    const Role role,
    const std::vector<Query>& queries)
{
    auto company = storage.createCompany(TEST_APIKEY);
    auto user = storage.createUser(login, company.id, role);
    EXPECT_EQ(user.value.login, login);
    EXPECT_EQ(user.value.uid, std::nullopt);

    for (const auto& query: queries) {
        if (query.should_throw) {
            EXPECT_THROW(
                storage.getUserByLoginAndUid(query.login, query.uid),
                AuthenticationException);
        } else {
            auto userNew = storage.getUserByLoginAndUid(query.login, query.uid);
            EXPECT_EQ(userNew.id, user.id);
            EXPECT_EQ(userNew.value.login, query.login);
            EXPECT_EQ(userNew.value.uid, query.uid);
            EXPECT_EQ(userNew.companies[0].role, user.companies[0].role);
        }
    }
}

TEST(StorageUserUpdateTest, TestLoginOnlyRegistration)
{
    auto storage = createTestStorage();
    testCreateUserAndQuery(*storage, "user1", Role::Admin, {{"user1", "uiduid"}});
}

TEST(StorageUserUpdateTest, TestAppLoginChange)
{
    auto storage = createTestStorage();
    testCreateUserAndQuery(
        *storage,
        "appuser1",
        Role::App,
        {{"appuser1", "uiduid"}, {"anotherappuser1", "uiduid"}});
}

TEST(StorageUserUpdateTest, TestAppUidChange)
{
    auto storage = createTestStorage();
    testCreateUserAndQuery(
        *storage,
        "appuser1",
        Role::App,
        {{"appuser1", "uiduid"}, {"appuser1", "uider"}});
}

TEST(StorageUserUpdateTest, TestAdminUidChange)
{
    auto storage = createTestStorage();
    testCreateUserAndQuery(
        *storage,
        "user1",
        Role::Admin,
        {{"user1", "uiduid"}, {"user1", "uider", true}});
}

} // namespace maps::b2bgeo::identity
