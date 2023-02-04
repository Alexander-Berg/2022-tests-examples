from analytics.collections.plotter_collections.plots.utils import get_collections_page


def test_page_board():
    assert 'board' == get_collections_page('https://yandex.kz/collections/user/a020681/risunki-i-kartinki-na-paskhu/')
    assert 'board' == get_collections_page('https://yandex.ru/collections/user/uid-c7wveglq/zhirnye-liudi/')
    assert 'board' == get_collections_page('https://yandex.by/collections/user/smirnov-swyatoslav/videokanal-miss-katy/')
    assert 'board' == get_collections_page('https://yandex.com/collections/user/madam-selischeva1960/izdeliia-iz-priazhi/')
    assert 'board' == get_collections_page('https://yandex.com.tr/collections/user/madam-selischeva1960/izdeliia-iz-priazhi/')


def test_page_card():
    assert 'card' == get_collections_page('https://yandex.ru/collections/card/5e47704e874e975867c9812a/')
    assert 'card' == get_collections_page('https://yandex.kz/collections/card/5e98b62013cf9a294514e5d8/')
    assert 'card' == get_collections_page('https://yandex.com/collections/card/5aedfc8cc75bad86afa33791/')
    assert 'card' == get_collections_page('https://yandex.com.tr/collections/card/5aedfc8cc75bad86afa33791/')


def test_page_channel():
    assert 'channel' == get_collections_page('https://yandex.ru/collections/channel/pochvopokrovnye-rasteniya-dlya-sada/')
    assert 'channel' == get_collections_page('https://yandex.ua/collections/channel/dizayn-malenkoy-vannoy/')
    assert 'channel' == get_collections_page('https://yandex.kz/collections/channel/kotyata/')


def test_page_profile():
    assert 'profile' == get_collections_page('https://yandex.ru/collections/user/nataly-bystrova/')
    assert 'profile' == get_collections_page('https://yandex.by/collections/user/consdelibus/')
    assert 'profile' == get_collections_page('https://yandex.com/collections/user/alinkanikitina5/')


def test_page_main():
    assert 'main' == get_collections_page('https://yandex.kz/collections/')
    assert 'main' == get_collections_page('https://yandex.ru/collections/')
    assert 'main' == get_collections_page('https://yandex.ru/collections/?source=promophoto')
    assert 'main' == get_collections_page('https://yandex.com/collections/')
    assert 'main' == get_collections_page('https://yandex.com.tr/collections/')


def test_page_myprofile():
    assert 'myProfile' == get_collections_page('https://yandex.ru/collections/user/')
    assert 'myProfile' == get_collections_page('https://yandex.by/collections/user/')
    assert 'myProfile' == get_collections_page('https://yandex.com/collections/user/')
    assert 'myProfile' == get_collections_page('https://yandex.com.tr/collections/user/')


def test_page_feed():
    assert 'feed' == get_collections_page('https://yandex.ru/collections/feed/')
    assert 'broFeed' == get_collections_page('https://yandex.ru/collections/bro/')
    assert 'imagesFeed' == get_collections_page('https://yandex.ru/images/')
    assert 'imagesFeed' == get_collections_page('https://yandex.ru/images')
    assert 'imagesTouchFeed' == get_collections_page('https://yandex.ru/images/touch/')
    assert 'imagesTouchFeed' == get_collections_page('https://yandex.ru/images/touch')

    assert 'feed' == get_collections_page('https://yandex.com/collections/feed/')
    assert 'broFeed' == get_collections_page('https://yandex.com/collections/bro/')
    assert 'imagesFeed' == get_collections_page('https://yandex.com/images/')
    assert 'imagesFeed' == get_collections_page('https://yandex.com/images')
    assert 'imagesTouchFeed' == get_collections_page('https://yandex.com/images/touch/')
    assert 'imagesTouchFeed' == get_collections_page('https://yandex.com/images/touch')

    assert 'feed' == get_collections_page('https://yandex.com.tr/collections/feed/')
    assert 'broFeed' == get_collections_page('https://yandex.com.tr/collections/bro/')
