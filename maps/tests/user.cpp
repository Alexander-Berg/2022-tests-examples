/// @file user.cpp
/// Tests for the logic of the user functions in DB

#include <maps/b2bgeo/identity/backend/libs/common/exceptions.h>
#include <maps/b2bgeo/identity/backend/libs/db/memory_storage.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <memory>

namespace maps::b2bgeo::identity {

const Apikey TEST_APIKEY{"f04c6079-4385-45e5-8d34-c19f42dea79e"};

TEST(StorageUserLogicTest, TestGetUsersByLogin)
{
    auto storage = std::make_unique<MemoryStorage>();
    auto company = storage->createCompany(TEST_APIKEY);

    auto user = storage->createUser("test-user", company.id, Role::Admin);

    for (const auto& login:
         {"test-user", "te*", "tes*er", "*t-u*", "*", "**", "?es?-user"}) {
        EXPECT_EQ(static_cast<int>(storage->matchByRegexLogin(login).size()), 1);
    }
}

TEST(StorageUserLogicTest, TestGetUsers)
{
    auto storage = std::make_unique<MemoryStorage>();
    auto company = storage->createCompany(TEST_APIKEY);

    storage->createUser("user_1", company.id, Role::Admin);
    storage->createUser("user_2", company.id, Role::Dispatcher);
    storage->createUser("user_3", company.id, Role::Manager);
    storage->createUser("user_4", company.id, Role::App);
    storage->createUser("user_5", company.id, Role::App);
    storage->createUser("user_6", company.id, Role::App);

    ASSERT_TRUE(storage->companyUsers(Company::Id(10000), 1).empty());
    EXPECT_EQ(static_cast<int>(storage->companyUsers(company.id, 1).size()), 6);

    ASSERT_TRUE(
        storage->companyAppUsers(User::Id(1), Company::Id(10000), 1).empty());
    EXPECT_EQ(
        static_cast<int>(
            storage->companyAppUsers(User::Id(1), company.id, 1).size()),
        4);
}

} // namespace maps::b2bgeo::identity
