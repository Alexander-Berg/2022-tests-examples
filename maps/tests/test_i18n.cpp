#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/radio_service/lib/i18n.h>
#include <maps/infra/yacare/include/yacare.h>
#include <maps/libs/locale/include/codes.h>
#include <maps/libs/locale/include/convert.h>

namespace maps::automotive::radio_service {

namespace {

void test(
    TStringBuf expected,
    const google::protobuf::Map<TString, TString>& map,
    const std::string& locale)
{
    auto l = locale::to<locale::Locale>(locale);
    EXPECT_EQ(expected, gettext(map, l));
}

} // namespace

TEST(i18n, gettext)
{
    ASSERT_EQ("", gettext({}, locale::Locale(locale::Lang::Ru)));
    ASSERT_EQ("", gettext({}, locale::Locale(locale::Lang::En)));
    {
        google::protobuf::Map<TString, TString> map;
        map["en_US"] = "enUS";

        test("enUS", map, "ru_RU");
        test("enUS", map, "ru_US");
        test("enUS", map, "ru_UA");
        test("enUS", map, "ru_TR");
        test("enUS", map, "en_US");
        test("enUS", map, "en_RU");
        test("enUS", map, "tr_TR");
    }
    {
        google::protobuf::Map<TString, TString> map;
        map["ru_RU"] = "руРУ";

        test("руРУ", map, "ru_RU");
        test("руРУ", map, "ru_US");
        test("руРУ", map, "ru_UA");
        test("руРУ", map, "ru_TR");
        test("руРУ", map, "en_US");
        test("руРУ", map, "en_RU");
        test("руРУ", map, "tr_TR");
    }
    {
        google::protobuf::Map<TString, TString> map;
        map["ru_RU"] = "руРУ";
        map["en_US"] = "enUS";
        test("руРУ", map, "ru_RU");
        // test("enUS", map, "ru_US");  // NMAPS-15258
        // test("enUS", map, "ru_UA");  // NMAPS-15258
        test("руРУ", map, "ru_TR");
        test("enUS", map, "en_US");
        // test("руРУ", map, "en_RU");  // NMAPS-15258
        test("enUS", map, "en_GB");
        test("enUS", map, "tr_TR");
    }
}

} // namespace maps::automotive::radio_service
