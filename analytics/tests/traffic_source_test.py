from analytics.collections.plotter_collections.plots.traffic_source import get_traffic_source


def test_no_url():
    assert 'no_url' == get_traffic_source(
        None,
        'https://man2-903bc19650fb-50717.tun.si.yandex.ru/',
        'Chrome'
    )
    assert 'no_url' == get_traffic_source(
        '',
        'https://man2-903bc19650fb-50717.tun.si.yandex.ru/',
        'Chrome'
    )


def test_broken_url():
    assert 'broken_url' == get_traffic_source(
        "https://yandex.rDJN\xd0\xc5\xdb\xaa\xdec]'\xcc\xfcqE`er/taniania88/kustovye-rozy/?utm_medium=informer&utm_source=yamain&utm_campaign=regular&utm_term=kustovye-rozy",
        'https://yandex.kz/',
        'YandexBrowser'
    )


# serp_kokos
def test_serp_kokos():
    assert 'serp_kokos' == get_traffic_source(
        'https://yandex.ru/collections/user/uid-r5zt3sua/kukhni/?utm_source=yandex&utm_medium=serp&utm_campaign=dynamic',  # noqa
        'https://man2-903bc19650fb-50717.tun.si.yandex.ru/',
        'ChromeMobile'
    )
    assert 'serp_kokos' == get_traffic_source(
        'https://yandex.ru/collections/user/dmitry-alexandrovich52/zimnii-les/?utm_source=yandex&utm_medium=serp&utm_campaign=dynamic',  # noqa
        None,
        'Chrome'
    )


# просмотрщик на Серпе
def test_serp_kokos_viewer():
    assert 'serp_kokos_viewer' == get_traffic_source(
        'https://yandex.ua/collections/user/romanowasony/chiornye-risunki-dlia-srisovki/?utm_source=yandex&utm_medium=from_serpviewer&utm_campaign=dynamic',  # noqa
        'https://man3-39b090059d32-52251.tun.si.yandex.ru/',
        'YandexBrowser'
    )
    assert 'serp_kokos_viewer' == get_traffic_source(
        'https://yandex.kz/collections/user/lafoyru/dizain-kukhni-v-stile-loft/?utm_source=yandex&utm_medium=from_serpviewer&utm_campaign=dynamic',  # noqa
        'https://yandex.kz/',
        'YandexBrowser'
    )


# колдунщик коллекций в кортинках
def test_images_kokoko():
    assert 'images_kokoko' == get_traffic_source(
        'https://yandex.ru/collections/user/vladdt19/lego-roboty/?utm_source=images&utm_medium=serp&utm_campaign=3ko&fromPassport=1',  # noqa
        'https://passport.yandex.ru/auth?origin=collections_header&retpath=https://yandex.ru/collections/user/vladdt19/lego-roboty/?utm_source=images&utm_medium=serp&utm_campaign=3ko&fromPassport=1',  # noqa
        'Safari'
    )
    assert 'images_kokoko' == get_traffic_source(
        'https://yandex.ru/collections/user/liudmilagorbushina/vidy-tigrov/?utm_source=images&utm_medium=serp&utm_campaign=3ko',  # noqa
        'https://yandex.ru/images/search?text=%D1%82%D0%B8%D0%B3%D1%80%D1%8B&from=tabbar',
        'Safari'
    )


# images_eko
def test_images_eko():
    assert 'images_eko' == get_traffic_source(
        'https://yandex.ru/collections/user/rom201720171/smeshnye-risunki/?utm_source=serp&utm_medium=thumb&utm_campaign=thisimg',  # noqa
        'http://yandex.ru/searchapp?text=',
        'YandexSearch'
    )
    assert 'images_eko' == get_traffic_source(
        'https://yandex.ru/collections/user/aleksmarr80/brusketta-klassicheskaia/?utm_source=images&utm_medium=thumb&utm_campaign=thisimg',  # noqa
        'https://yandex.ru/showcaptcha?cc=1&retpath=https%3A//yandex.ru/collections/user/aleksmarr80/brusketta-klassicheskaia%3Futm_source%3Dimages%26utm_medium%3Dthumb%26utm_campaign%3Dthisimg_4fe08745d80ea782bfa10c6d6142a893&t=0/1572103507/d919a032e75e59cd61bcafac76d294e0&s=91c3fdcb75a3d02d83209d694f72006c',  # noqa
        'MobileSafari'
    )


# header
def test_icon_header():
    assert 'icon_header' == get_traffic_source(
        'https://yandex.ru/collections/?utm_medium=header&utm_source=serp',
        'https://yandex.ru/',
        'YandexBrowser'
    )
    assert 'icon_header' == get_traffic_source(
        'https://yandex.ru/collections/?utm_medium=header&utm_source=video',
        'https://yandex.ru/video/touch/search/?text=%D0%A2%D1%80%D0%B0%D1%85%D0%BD%D1%83%D0%BB%20%D0%BC%D0%B0%D0%BC%D1%83%20%D0%B4%D0%B5%D0%B2%D1%83%D1%88%D0%BA%D0%B8%20%D0%B2%20%D1%81%D0%BF%D0%BE%D1%80%D1%82%D0%B7%D0%B0%D0%BB%D0%B5%20',  # noqa
        'ChromeMobile'
    )


def test_push():
    assert 'push' == get_traffic_source(
        'https://yandex.ru/collections/card/5cc3109801cde42e30b8e586/?utm_campaign=repin&clid=2272374&utm_medium=push&utm_source=yandex_app&push_id=1572764558637320-5344103854933408759',  # noqa
        None,
        'YandexBrowser'
    )


def test_bell():
    assert 'bell' == get_traffic_source(
        'https://yandex.ru/collections/user/progressor73/?utm_term=collections&utm_medium=notifier&utm_campaign=new_card_in_subscribed_board_male',  # noqa
        'https://yandex.ru/global-notifications/yandex-notifications/v0.0.100/static/index.ru.html?from=browser_android_ntp',  # noqa
        'YandexBrowser'
    )


def test_pp_informer():
    assert 'pp_informer' == get_traffic_source(
        'https://yandex.ru/collections/channel/temnyy-manikyur/?utm_medium=informer&utm_term=temnyy-manikyur&utm_content=krasota-i-ukhod&utm_source=yandex_app&utm_campaign=regular&rec_flags=viewed_channels%3D58a474ef1bb6040028a1f120%2C58594fa4eae4cc0027273b3c%2C586527081bb6040024c98c19%2C5859552e1bb6040026124dfe%2C5859586f1bb6040027123443%2C585956d31bb6040026125451%2C5821e47beae4cc0024114b96%2C585959791bb604002312296f%2C5821ef321bb60400270643c9%2C58d29cc4eae4cc004344c45b',  # noqa
        None,
        'YandexSearch'
    )


def test_morda_informer_feed():
    assert 'morda_informer_feed' == get_traffic_source(
        'https://yandex.ru/collections/feed/?rec_flags=forced_items%3D80387fc7-48b5bbb2-cb6c9b1c-ee88d7c5%7C9936c34007fe0f46762d10b570b3abb3%2C80387fc7-48b5bbb2-cb6c9b1c-ee88d7c5%7Cbcc72c0d0c0b7e36fcd2a670ffff8387&utm_medium=informer&utm_source=yamain&utm_campaign=regular&utm_term=5b4b7613467d08007cf267be',  # noqa
        'https://yandex.ru/',
        'Opera'
    )


def test_morda_informer():
    assert 'morda_informer' == get_traffic_source(
        'https://yandex.ru/collections/user/infopatrushev/fitnes-devushki/?&utm_term=fitnes-devushki&utm_campaign=regular&utm_source=yamain&utm_medium=informer',  # noqa
        'https://yandex.ru/',
    )
    assert 'morda_informer' == get_traffic_source(
        'https://yandex.ru/collections/user/infopatrushev/fitnes-devushki/?&utm_term=fitnes-devushki&utm_campaign=regular&utm_source=yamain&utm_medium=informer',  # noqa
    )


def test_zen():
    assert 'zen' == get_traffic_source(
        'https://yandex.ru/collections/user/avrorabocharova/smeshnye-koty/?utm_referrer=https%3A%2F%2Fzen.yandex.com',
    )
    assert 'zen' == get_traffic_source(
        'https://yandex.ru/collections/user/dischkova/otkrytki-s-chistym-chetvergom/',
        ' https:zen.yandex.ru',
    )

    assert 'zen' == get_traffic_source(
        'https://yandex.ru/collections/user/company@ailaikpet/zhivotnye-s-neobychnym-okrasom/?utm_referrer=https%3A%2F%2Fzen.yandex.com%2F%3Ffromzen%3Dsearchapp',  # noqa
        'https://yandex.ru/',
    )
    assert 'zen' == get_traffic_source(
        'https://yandex.ru/collections/user/company@ailaikpet/zhivotnye-s-neobychnym-okrasom/',  # noqa
        'https://zen.yandex.ru/',
    )

    assert 'zen_source' == get_traffic_source(
        'https://yandex.ru/collections/user/rid-nica/dilan-minnett/?utm_term=dilan-minnett&utm_campaign=regular&utm_source=zen_source&utm_medium=zen_medium',  # noqa
        'https://yandex.ru/',
    )
    assert 'zen_source' == get_traffic_source(
        'https://yandex.ru/collections/user/capitan2008/sssr-chetyre-pokoleniia-v-semeinom-albome-sssr-ot-nachala-i-do-kontsa/?&utm_term=sssr-chetyre-pokoleniia-v-semeinom-albome-sssr-ot-nachala-i-do-kontsa&utm_campaign=regular&utm_source=yamain&utm_medium=informer',  # noqa
        'https://zen.yandex.ru/',
    )


def test_visited():
    assert 'visited' == get_traffic_source(
        'https://yandex.ru/collections/channel/dizayn-mansardy/?utm_source=yandex&utm_medium=visited&utm_content=interiors&utm_term=dizayn-mansardy',  # noqa
        'https://yandex.ru/',
        'Edge'
    )


def test_fotki():
    assert 'fotki' == get_traffic_source(
        'https://yandex.ru/collections/contests/?utm_source=fotki',
        None,
        'Chrome'
    )


# Yandex Browser
def test_bro_desktop():
    assert 'bro_desktop' == get_traffic_source(
        'https://yandex.ru/collections/?isBrowserFavorites=1',
        None,
        'YandexBrowser'
    )


def test_bro_mmorda():
    assert 'bro_mmorda' == get_traffic_source(
        'https://yandex.ru/collections/bro/feed/?app_id=ru.yandex.mobile.search&app_platform=iphone&app_version=1910020000&appsearch_header=1&collections_ppfeed_inbro=true&did=E0660418-772F-463A-9E7A-49A2708756E2&uuid=13f7e9abe8ad4f489266aae2c6199388',  # noqa
        None,
        'YandexBrowser'
    )


def test_bro_recent():
    assert 'bro_recent' == get_traffic_source(
        'https://yandex.ru/collections/bro/recent/',
        None,
        'YandexBrowser'
    )


def test_bro_bookmarks():
    assert 'bro_bookmarks' == get_traffic_source(
        'https://yandex.ru/collections/bro/bookmarks/',
        None,
        'YandexBrowser'
    )


# SearchApp
def test_pp_mmorda():
    assert 'pp_mmorda' == get_traffic_source(
        'https://yandex.ru/collections/app/recent/?app_build_number=12630&app_id=ru.yandex.mobile&app_platform=iphone&app_version=17010000&app_version_name=17.10&did=FA07BD4E-95CD-4DC0-8A6A-D7782B5B436F&manufacturer=Apple&model=iPhone6%2C2&os_version=12.4.2&uuid=643516dc8171459fb806aa3f88731969',  # noqa
        None,
        'YandexSearch'
    )


def test_pp_other():
    assert 'pp_other' == get_traffic_source(
        'https://yandex.ru/collections/card/5ccb5793a4ce8e58b451f12f/?boardId=5ccb5733c8ba05802a3d19ed',  # noqa
        'http://yandex.ru/searchapp?text=',
        'YandexSearch'
    )


# Toloka
def test_toloka():
    assert 'toloka' == get_traffic_source(
        'https://m.yandex.ru/collections/card/5b8e4071e0149a00a364791a/',
        'https://iframe-toloka.com/',  # noqa
        'Chrome'
    )


# internal
def test_internal():
    assert 'internal_board' == get_traffic_source(
        'https://yandex.kz/collections/user/ok-griczencko2016/iana-samoilova-foto/',
        'https://yandex.kz/collections/user/mochalovanikonova2011/moia/',  # noqa
        'MobileSafari'
    )


# serp_collections_snippet
def test_serp_collections_snippet():
    assert 'serp_collections_snippet' == get_traffic_source(
        'https://yandex.ru/collections/card/5a5c45c20c1ed2b28e093dda/?boardId=582816df66351850fc64dafe',
        'https://yandex.ru/',
        'YandexBrowser'
    )


# Autotests
def test_autotests():
    assert 'autotests' == get_traffic_source(
        'https://yandex.ru/collections/',
        'https://iva5-8a200147a267-14696.tun.si.yandex.ru/search/pad/?text=foreverdata&lr=213&tld=ru&foreverdata=667594539&promo=nomooa&noredirect=1&no-tests=1&test-id=1&srcskip=ATOM_PROXY&exp_flags=autotest_selector%3D.serp-footer&exp_flags=autotest_id%3Da3aae6b&exp_flags=test_tool%3Dhermione&exp_flags=cspid%3DY2h1Y2sgbm9ycmlz&exp_flags=turbo-host%3Dhmstr&tpid=809213b%2Fipad&testRunId=ODA5MjEzYi9pcGFkLDE1NzI0MzcxNDY4MDYsLA%3D%3D&template_exp_flags=enable-t-classes',  # noqa
        'MobileSafari'
    )


# Direct
def test_direct():
    assert 'direct' == get_traffic_source(
        'https://yandex.ru/collections/user/hzoxy4/muzykalnye-otkrytki/',
        None,
        'MZBrowser'
    )


# jsredir
def test_jsredir():
    assert 'images_search' == get_traffic_source(
        'https://yandex.ru/collections/card/5c998dca943cef00899714a1/',
        'http://yandex.by/clck/jsredir?from=yandex.by%3Bimages%2Ftouch%2Fsearch%3Bimages%3B%3B&text=&etext=8003.NYNajDBqgN65WOg2g2PpCK6X6VDXDeFZLnWTIGyMUcqzA-tWjsxi877wHNl-DjnLuIgRwTPP3VbJZlRfcrAw4OGnPmuZAA4LSCiy7s5NSJI0w5eAecV2K9IlZS3GLtj7ny-aApo-JEwG9mJSmvWulw.a4d33a6cf309a0814d610d3014e0c8fd27249965&uuid=&state=UE97xbmeeKJBbecmHtARxxOYpCLpw9PVSa7fA3kSMZrnt07WSKJzC5vt0Kyj2Wp7K6uz4n6Jk7evbVsv9JYLgL58PHuy3C6K9Y05iTcQzNlEcUNLBQVyKw,,&data=UlNrNmk5WktYejY4cHFySjRXSWhXTWRwNjROVUR3SGZ4d0ZxNlBTblBJdUdvRnZUTTVoTXdhMUZlTFpfOXBMQXdkR0VhMHA1dVlqald2a2dyYk9UTmtBbG1iWU1BRHM3aG9aNUNvcFJCNlEwVjBmc3FHMXQwNTFIR2RkWkNfWjhWT2RYZzZ5Vi1HV0I5UDVLQV9kUXdVY3owYWt5dmg4dw,,&sign=52298d6708d3be1d5ffa3bb40345fe04&keyno=0&b64e=2&l10n=ru',  # noqa
        'Chrome'
    )
    assert 'yandex' == get_traffic_source(
        'https://yandex.ru/images/touch/?redircnt=1572953128.1',
        'http://www.yandex.ru/clck/jsredir?from=yandex.ru%3Bsuggest%3Bbrowser&text=',  # noqa
        'Chrome'
    )


# Some Another Yandex
def test_yandex():
    assert 'yandex' == get_traffic_source(
        'https://yandex.ru/collections/feed/?redircnt=1570715312.1',
        'https://yandex.ru/',
        'Samsung Internet'
    )
    assert 'yandex_other' == get_traffic_source(
        'https://yandex.com/collections/user/fatalex73/portrety-izvestnykh-krasotok/',
        'https://yandex.com/showcaptcha?retpath=https%3A//yandex.com/collections/user/fatalex73%3F_c107d45a31e1edeb767fa7290014d70c&t=0/1570694101/57c6e27995cf66a709d89cf569dbe700&s=1d2589c563be2f21f846e65742995631',  # noqa
        'ChromeMobile'
    )


# social, mailru
def test_noyandex():
    assert 'social' == get_traffic_source(
        'https://yandex.com/collections/card/58a4be1a77f31a67f1aff4d6/',
        'https://away.vk.com/',
        'Chrome'
    )
    assert 'mailru_rambler' == get_traffic_source(
        'https://yandex.ru/collections/user/ugarova-marina2017/raskraski-dlia-devochek/',
        'https://nova.rambler.ru/search?query=%D1%82%D0%BE%D0%BF-%D0%BC%D0%BE%D0%B4%D0%B5%D0%BB%D0%B8%20%20%D0%B4%D0%BB%D1%8F%20%D1%80%D0%B0%D1%81%D0%BA%D1%80%D0%B0%D1%88%D0%B8%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F',  # noqa
        'YandexBrowser'
    )


# Some Sites
def test_some_sites():
    assert 'some_site' == get_traffic_source(
        'https://yandex.ru/collections/',
        'https://huyandex.ru',
        'YandexBrowser'
    )
    assert 'some_site' == get_traffic_source(
        'https://yandex.ru/collections/',
        'https://test-site.ru',
        'YandexBrowser'
    )
    assert 'some_site' == get_traffic_source(
        'https://yandex.ru/collections/',
        'https://yandex.narod.ru',
        'YandexBrowser'
    )


# Misc
def test_strange_referrer():
    assert 'yandex' == get_traffic_source(
        'https://yandex.ru/collections/user/dischkova/otkrytki-s-chistym-chetvergom/',
        'http:yandex.ru',
    )
    assert 'yandex' == get_traffic_source(
        'https://yandex.ru/collections/user/dischkova/otkrytki-s-chistym-chetvergom/',
        'https:yandex.ru',
    )
    assert 'social' == get_traffic_source(
        'https://yandex.ru/collections/user/dischkova/otkrytki-s-chistym-chetvergom/',
        ' https://ok.ru/',
    )
