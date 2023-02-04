import pytest
import collections

from ads.watchman.timeline.api.lib.common import utils as co_utils


TEXTS = collections.OrderedDict(
    [
        ('one_url', ("https://jing.yandex-team.ru/files/ilariia/a.png",
                     ["https://jing.yandex-team.ru/files/ilariia/a.png"])),
        ('multiple_urs', ("https://jing.yandex-team.ru/files/ilariia/a.png https://jing.yandex-team.ru/files/ilariia/b.png",
                          ["https://jing.yandex-team.ru/files/ilariia/a.png", "https://jing.yandex-team.ru/files/ilariia/b.png"])),
        ('multiple_urs_with_new_line', ("https://jing.yandex-team.ru/files/ilariia/a.png\nhttps://jing.yandex-team.ru/files/ilariia/b.png",
                                        ["https://jing.yandex-team.ru/files/ilariia/a.png", "https://jing.yandex-team.ru/files/ilariia/b.png"])),
        ('png', ("My picture: https://jing.yandex-team.ru/files/ilariia/a.png Cool",
                 ["https://jing.yandex-team.ru/files/ilariia/a.png"])),
        ('PNG', ("My picture: https://jing.yandex-team.ru/files/ilariia/a.PNG Cool",
                 ["https://jing.yandex-team.ru/files/ilariia/a.PNG"])),
        ('jpg', ("My picture: https://jing.yandex-team.ru/files/ilariia/a.jpg Cool",
                 ["https://jing.yandex-team.ru/files/ilariia/a.jpg"])),
        ('JPG', ("My picture: https://jing.yandex-team.ru/files/ilariia/a.JPG Cool",
                 ["https://jing.yandex-team.ru/files/ilariia/a.JPG"])),
        ('jpeg', ("My picture: https://jing.yandex-team.ru/files/ilariia/a.jpeg Cool",
                  ["https://jing.yandex-team.ru/files/ilariia/a.jpeg"])),
        ('JPEG', ("My picture: https://jing.yandex-team.ru/files/ilariia/a.JPEG Cool",
                  ["https://jing.yandex-team.ru/files/ilariia/a.JPEG"])),
        ('Dots in url', ("My picture: https://jing.yandex-team.ru/files/ilariia/a.and.b.jpg Cool",
                         ["https://jing.yandex-team.ru/files/ilariia/a.and.b.jpg"]))
    ]
)


@pytest.mark.parametrize(u'text,urls', TEXTS.values(), ids=TEXTS.keys())
def test_extract_jing_urls(text, urls):
    assert co_utils.extract_jing_urls(text) == urls
