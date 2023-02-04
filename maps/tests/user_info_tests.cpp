#include "tvmtool_fixture.h"

#include <maps/libs/auth/include/blackbox.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::auth::tests {

Y_UNIT_TEST_SUITE(user_info) {

Y_UNIT_TEST(good)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_info.good");
        });

    auto info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setUid("111111111")
            .execute();
    ASSERT_TRUE(info);
    EXPECT_EQ(info->uid(), "111111111");
    EXPECT_EQ(info->login(), "test.user.1");
    ASSERT_TRUE(info->publicId());
    EXPECT_EQ(*info->publicId(), "a824gr35uyvnjk2erubtba9c41");

    info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setLogin("test.user.1")
            .execute();
    ASSERT_TRUE(info);
    EXPECT_EQ(info->uid(), "111111111");
    EXPECT_EQ(info->login(), "test.user.1");
    ASSERT_TRUE(info->publicId());
    EXPECT_EQ(*info->publicId(), "a824gr35uyvnjk2erubtba9c41");

    info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setPublicId("a824gr35uyvnjk2erubtba9c41")
            .execute();
    ASSERT_TRUE(info);
    EXPECT_EQ(info->uid(), "111111111");
    EXPECT_EQ(info->login(), "test.user.1");
    ASSERT_TRUE(info->publicId());
    EXPECT_EQ(*info->publicId(), "a824gr35uyvnjk2erubtba9c41");
}

Y_UNIT_TEST(display_name_good)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_info.display_name_good");
        });

    auto info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setUid("3000062912")
            .execute();
    ASSERT_TRUE(info);
    EXPECT_EQ(info->uid(), "3000062912");
    EXPECT_EQ(info->login(), "test");

    info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setLogin("test")
            .setQueryParams(maps::auth::BlackboxQueryParams().requestDisplayName())
            .execute();
    ASSERT_TRUE(info);
    EXPECT_EQ(info->uid(), "3000062912");
    EXPECT_EQ(info->login(), "test");
    ASSERT_TRUE(info->displayName());
    auto dn = *info->displayName();
    ASSERT_EQ(dn.name, "Козьма Прутков");
    ASSERT_EQ(dn.publicName, "Козьма П.");
    ASSERT_TRUE(dn.avatarId && *dn.avatarId == "4000217463");
    ASSERT_TRUE(dn.socialProfileId && *dn.socialProfileId == "5328");
    ASSERT_TRUE(dn.socialProfileProvider && *dn.socialProfileProvider == "tw");
    ASSERT_TRUE(dn.socialProfileRedirectTarget && *dn.socialProfileRedirectTarget == "1323266014.26924.5328.9e5e3b502d5ee16abc40cf1d972a1c17");
}

Y_UNIT_TEST(address_list_good)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_info.address_list_good");
        });

    auto info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setQueryParams(maps::auth::BlackboxQueryParams().requestAllEmailsInAddressList())
            .execute();
    ASSERT_TRUE(info);
    ASSERT_TRUE(info->addressList());
    ASSERT_EQ(info->addressList()->size(), 3u);

    auto defaultAddress = (*info->addressList())[0];
    ASSERT_EQ(defaultAddress.address, "test_default@yandex.ru");
    ASSERT_EQ(defaultAddress.bornDate, "2011-11-16 00:00:00");
    ASSERT_TRUE(defaultAddress.isDefault);
    ASSERT_TRUE(defaultAddress.isNative);
    ASSERT_TRUE(!defaultAddress.isRpop);
    ASSERT_TRUE(!defaultAddress.isSilent);
    ASSERT_TRUE(!defaultAddress.isUnsafe);
    ASSERT_TRUE(defaultAddress.isValidated);

    auto yandexAddress = (*info->addressList())[1];
    ASSERT_EQ(yandexAddress.address, "test_yandex@yandex.ru");
    ASSERT_EQ(yandexAddress.bornDate, "2011-11-17 00:00:00");
    ASSERT_TRUE(!yandexAddress.isDefault);
    ASSERT_TRUE(yandexAddress.isNative);
    ASSERT_TRUE(!yandexAddress.isRpop);
    ASSERT_TRUE(!yandexAddress.isSilent);
    ASSERT_TRUE(!yandexAddress.isUnsafe);
    ASSERT_TRUE(yandexAddress.isValidated);
}

Y_UNIT_TEST(good_batch)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_info_batch.good");
        });

    auto infos = blackboxApi.userInfoBatchQuery()
            .setRemoteAddress("127.0.0.1")
            .setUids({"111111111", "222222222"})
            .execute();
    ASSERT_EQ(infos.size(), 2u);
    EXPECT_EQ(infos[0].uid(), "111111111");
    EXPECT_EQ(infos[0].login(), "test.user.1");
    EXPECT_EQ(infos[1].uid(), "222222222");
    EXPECT_EQ(infos[1].login(), "test.user.2");
    ASSERT_TRUE(infos[0].publicId());
    EXPECT_EQ(*infos[0].publicId(), "abracadabra1");
    ASSERT_TRUE(infos[1].publicId());
    EXPECT_EQ(*infos[1].publicId(), "abracadabra2");
}

Y_UNIT_TEST(nonexistent)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_info.nonexistent");
        });

    auto info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setUid("111111111")
            .execute();
    ASSERT_FALSE(info);

    info = blackboxApi.userInfoQuery()
            .setRemoteAddress("127.0.0.1")
            .setLogin("test.user.1")
            .execute();
    ASSERT_FALSE(info);
}

Y_UNIT_TEST(nonexistent_batch)
{
    auto blackboxApi = BlackboxApi(Fixture().tvmtoolSettings());
    auto blackboxMock = http::addMock(
        blackboxApi.environment().url,
        [](const http::MockRequest&) {
            return http::MockResponse::fromArcadia(
                "maps/libs/auth/tests/responses/user_info_batch.nonexistent");
        });

    auto infos = blackboxApi.userInfoBatchQuery()
            .setRemoteAddress("127.0.0.1")
            .setUids({"111111111", "222222222"})
            .execute();
    ASSERT_EQ(infos.size(), 1u);
    EXPECT_EQ(infos[0].uid(), "222222222");
    EXPECT_EQ(infos[0].login(), "test.user.2");
}


Y_UNIT_TEST(too_large_batch)
{
    std::vector<std::string> uids(BlackboxApi::UserInfoBatchQuery::MAX_BATCH_SIZE);
    auto query = BlackboxApi(Fixture().tvmtoolSettings()).userInfoBatchQuery();
    EXPECT_NO_THROW(query.setUids(uids));

    uids.emplace_back();
    EXPECT_THROW(query.setUids(uids), maps::LogicError);
}

Y_UNIT_TEST(empty_batch)
{
    auto query = BlackboxApi(Fixture().tvmtoolSettings()).userInfoBatchQuery();
    EXPECT_THROW(query.setUids({}), maps::LogicError);
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::auth::tests
