#include <maps/infopoint/lib/auth/user_config.h>
#include <maps/infopoint/lib/misc/locale.h>

#include <yandex/maps/i18n/i18n.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <maps/infopoint/tests/common/fixture.h>

using namespace infopoint;
using namespace testing;

const std::string SUPPLIERS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/suppliers.conf";

constexpr auto EPSILON = 1.0e-5;

TEST_F(Fixture, test_read_value)
{
    currentLocale() = maps::i18n::bestLocale(maps::i18n::defaultLocale());

    UserConfig config(SUPPLIERS_CONFIG);

    auto gati = config.localizedSupplier(UserURI("http://www.gati-online.ru"));
    EXPECT_EQ(gati.name, "ГАТИ");
    EXPECT_EQ(gati.href, "http://www.gati-online.ru");
    EXPECT_EQ(gati.type, User::PARTNER);
    EXPECT_NEAR(gati.rating, 0.7, EPSILON);

    Supplier defaultSupplier = config.localizedSupplier(UserURI("some_uri"));
    EXPECT_EQ(defaultSupplier.uri.value(), "mobile.yandex.ru");
    EXPECT_EQ(defaultSupplier.name, "Мобильные Яндекс.Карты");
    EXPECT_EQ(defaultSupplier.href, "http://maps.yandex.ru");
    EXPECT_EQ(defaultSupplier.type, User::MOBILE);
    EXPECT_NEAR(defaultSupplier.rating, 0.3, EPSILON);

    config.addSupplier(
        UserURI("some_uri"),
        "some_name",
        "some_href",
        User::EXPERT);

    Supplier someSupplier = config.localizedSupplier(UserURI("some_uri"));
    EXPECT_EQ(someSupplier.uri.value(), "some_uri");
    EXPECT_EQ(someSupplier.name, "some_name");
    EXPECT_EQ(someSupplier.href, "some_href");
    EXPECT_EQ(someSupplier.type, User::EXPERT);
    EXPECT_NEAR(someSupplier.rating, 0.95, EPSILON);
}
