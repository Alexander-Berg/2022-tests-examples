#include "../scheme_parser_utils_impl.h"

#include <boost/optional.hpp>
#include <boost/test/unit_test.hpp>

using namespace yandex::maps::navi::scheme_parser;
using namespace std::string_literals;

BOOST_AUTO_TEST_SUITE(scheme_parser_utils)

BOOST_AUTO_TEST_CASE(NaviSchemeTest)
{
    BOOST_CHECK(
        parseScheme(
                "yandexnavi",
                "build_route_on_map?lat_from=55.751802&lon_from=37.586684&lat_to=55.758192&lon_to=37.642817") ==
            "build_route_on_map?lat_from=55.751802&lon_from=37.586684&lat_to=55.758192&lon_to=37.642817"s);

    BOOST_CHECK(
        parseScheme("yandexnavi", "show_point_on_map?lat=55.42&lon=37.279&no-balloon=1") ==
            "show_point_on_map?lat=55.42&lon=37.279&no-balloon=1"s);

    BOOST_CHECK(
        parseScheme("yandexnavi", "//map_search?text=Moscow") ==
            "map_search?text=Moscow"s);

    BOOST_CHECK(parseScheme("yandexnavi", "//") == boost::none);
}

BOOST_AUTO_TEST_CASE(AndroidSchemeTest)
{
    BOOST_CHECK(
        parseScheme("geo", "55,37") ==
            "show_point_on_map?lat=55&lon=37"s);
    BOOST_CHECK(
        parseScheme("geo", "-55.42,-37.279") ==
            "show_point_on_map?lat=-55.42&lon=-37.279"s);

    BOOST_CHECK(
        parseScheme("geo", "55,37?z=10") ==
            "show_point_on_map?lat=55&lon=37&zoom=10"s);
    BOOST_CHECK(
        parseScheme("geo", "-55.0,-37.0?z=10") ==
            "show_point_on_map?lat=-55.0&lon=-37.0&zoom=10"s);

    BOOST_CHECK(
        parseScheme("geo", "0,0?q=55,37(Treasure)") ==
            "show_point_on_map?lat=55&lon=37&desc=Treasure"s);
    BOOST_CHECK(
        parseScheme("geo", "0,0?q=-55.31416,-37.42(Treasure)") ==
            "show_point_on_map?lat=-55.31416&lon=-37.42&desc=Treasure"s);

    BOOST_CHECK(
        parseScheme("geo", "0,0?q=search+request") ==
            "map_search?text=search+request"s);
}

BOOST_AUTO_TEST_CASE(YandexMapsSchemeTest)
{
    BOOST_CHECK(
        parseScheme("http", "ll=37.279,55.42") ==
            "show_point_on_map?lat=55.42&lon=37.279&no-balloon=1"s);

    BOOST_CHECK(
        parseScheme("https", "ll=-37.42%2C-55.31416&z=10") ==
            "show_point_on_map?lat=-55.31416&lon=-37.42&zoom=10&no-balloon=1"s);

    BOOST_CHECK(
        parseScheme("http", "?text=Moscow") == boost::none);

    BOOST_CHECK(
        parseScheme("http", "yandex.com.tr/harita/?text=Moscow") ==
            "map_search?text=Moscow"s);

    BOOST_CHECK(
        parseScheme("http", "harita.yandex.com.tr/?text=Moscow") ==
            "map_search?text=Moscow"s);

    BOOST_CHECK(
        parseScheme("http", "maps.yandex.ru/?text=Moscow") == boost::none);
}

BOOST_AUTO_TEST_CASE(WhitelistedUrlsTest)
{
    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.com.tr/promo/asd") == true
        );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.ru/promo/") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.kz/promo/foo/bar/") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://lp-constructor.yandex-team.ru/promo/.sandbox/dfgdfgdfgdfg") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.com/set/lp/ghgh") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.by/promo/navi/dfgfgd") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://extmaps-api.yandex.net/fdglmkdfg/dsdf") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.ru/maps/mobile-feedback?type=business") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.ru/maps/fooo") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.ru/ugcpub/cabinet?main_tab=professions") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.ru/ugcpub/push-org?xxxxxxx=yyyyy") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://mail.ru/phishing?ignore=https://yandex.ru/promo/") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://lenta.ru/ffhhh") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://fakeyandex.ru/promo/") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.ru/promo/..") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://www.yandex.ru/promo/../clck/jsredir?some_awfulredir=mail.ru") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://www.yandex.ru/promo/%2E./clck/jsredir?some_awfulredir=mail.ru") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://www.yandex.ru/promo/.%2E/clck/jsredir?some_awfulredir=mail.ru") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://www.yandex.ru/promo/%2E%2E/clck/jsredir?some_awfulredir=mail.ru") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://www.yandex.ru/promo/%2e%2e/clck/jsredir?some_awfulredir=mail.ru") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandexfake.ru/promo/") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://yandex.biz/promo/") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://www.yandex.xn--p1ai/promo/") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://renins.promo.maps.yandex.ru/") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://hankook.promo.maps.yandex.ru/") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://toyota.promo.maps.yandex.com.tr/") == true
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://toyota.promo.maps.yandex.biz/") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://toyota.promo.maps.yandexfake.ru/") == false
    );

    BOOST_CHECK(
        isWhitelistedUrl("https://pwn.haxor.us/toyota.promo.maps.yandex.com.tr/") == false
    );
}

BOOST_AUTO_TEST_SUITE_END()
