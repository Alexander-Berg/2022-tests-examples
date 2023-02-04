#include <yandex/maps/wiki/social/gateway.h>

#include <maps/libs/json/include/value.h>
#include <library/cpp/testing/unittest/registar.h>
#include "helpers.h"
#include <thread>

namespace maps::wiki::social::tests {

namespace {

const TUid TEST_UID = 111;

} // namespace

Y_UNIT_TEST_SUITE(user_data_suite) {

Y_UNIT_TEST_F(noexistent, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);

    auto data = gtw.getUserData(TEST_UID);
    UNIT_ASSERT_EQUAL(data.has_value(), false);
}

Y_UNIT_TEST_F(insert_and_get, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);

    auto testData = [&](const UserData& userdata) {
        UNIT_ASSERT_EQUAL(userdata.uid(), TEST_UID);
        UNIT_ASSERT_EQUAL(json::Value::fromString(userdata.data()).isObject(), true);
        UNIT_ASSERT_EQUAL(userdata.createdAt(), userdata.modifiedAt());
    };

    {
        auto userdata = gtw.setUserData(TEST_UID, "{}");
        testData(userdata);
    }
    {
        auto userdata = gtw.getUserData(TEST_UID);
        UNIT_ASSERT_EQUAL(userdata.has_value(), true);
        testData(*userdata);
    }
}

Y_UNIT_TEST_F(insert_update, DbFixture)
{
    auto insertData = [&] {
        pqxx::work txn(conn);
        Gateway gtw(txn);

        auto userdata = gtw.setUserData(TEST_UID, "{}");
        txn.commit();

        return userdata;
    };

    auto newData = insertData();
    UNIT_ASSERT_EQUAL(newData.uid(), TEST_UID);
    UNIT_ASSERT_EQUAL(json::Value::fromString(newData.data()).isObject(), true);
    UNIT_ASSERT_EQUAL(newData.createdAt(), newData.modifiedAt());

    const auto SLEEP_TIME = std::chrono::seconds(1);
    std::this_thread::sleep_for(SLEEP_TIME);

    pqxx::work txn(conn);
    Gateway gtw(txn);

    auto updatedData = gtw.setUserData(TEST_UID, "[]");
    UNIT_ASSERT_EQUAL(updatedData.uid(), TEST_UID);
    UNIT_ASSERT_EQUAL(json::Value::fromString(updatedData.data()).isArray(), true);

    UNIT_ASSERT_EQUAL(newData.createdAt(), updatedData.createdAt());
    UNIT_ASSERT(newData.createdAt() + SLEEP_TIME <= updatedData.modifiedAt());
}

} // user_data_suite

} // namespace maps::wiki::social::tests
