import pytest

from intranet.search.yt.jobs.extract_links import normalize_url


@pytest.mark.parametrize('url,base_url,expected', (
    ('/absolute/page', 'http://wiki.y-t.ru/cluster1', 'http://wiki.y-t.ru/absolute/page'),
    ('./relative/page', 'http://wiki.y-t.ru/cluster1', 'http://wiki.y-t.ru/cluster1/relative/page'),
    ('!/relative/page', 'http://wiki.y-t.ru/cluster1', 'http://wiki.y-t.ru/cluster1/relative/page'),
    ('relative/page', 'http://wiki.y-t.ru/cluster1', 'http://wiki.y-t.ru/cluster1/relative/page'),
    ('../relative/parent', 'http://wiki.y-t.ru/cluster1/cluster2', 'http://wiki.y-t.ru/cluster1/relative/parent'),
    ('/page/.edit', 'http://wiki.y-t.ru/', 'http://wiki.y-t.ru/page'),
    ('/page/.create', 'http://wiki.y-t.ru/', 'http://wiki.y-t.ru/page'),
    ('/page/.preview', 'http://wiki.y-t.ru/', 'http://wiki.y-t.ru/page'),
    ('/page/', 'http://wiki.y-t.ru/', 'http://wiki.y-t.ru/page'),
    ('http://doc.y-t.ru/some/page', 'http://wiki.y-t.ru/cluster1', 'http://doc.y-t.ru/some/page'),
    ('//doc.y-t.ru/some/page', 'http://wiki.y-t.ru/cluster1', 'http://doc.y-t.ru/some/page'),
    ('http://DOC.Y-t.ru/Some/Page', '', 'http://doc.y-t.ru/some/page'),
    ('http://doc.y-t.ru/ПоРусски', '', 'http://doc.y-t.ru/%D0%BF%D0%BE%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8'),
    ('http://doc.y-t.ru/По%20Русски', '', 'http://doc.y-t.ru/%D0%BF%D0%BE%20%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8'),
    ('http://doc.y-t.ru/some/with%20space', '', 'http://doc.y-t.ru/some/with%20space'),
    ('http://doc.y-t.ru/some/with#fragment', '', 'http://doc.y-t.ru/some/with'),
    ('#just_fragment', 'http://doc.y-t.ru/some/with', 'http://doc.y-t.ru/some/with'),
    ('?just=query', 'http://doc.y-t.ru/some/with', 'http://doc.y-t.ru/some/with'),
    ('http://doc.y-t.ru/some/with?query=param&another=param', '', 'http://doc.y-t.ru/some/with'),
    ('mailto:some-email@yandex-team.ru', 'http://wiki.y-t.ru/cluster1', 'mailto:some-email@yandex-team.ru'),
    ('file:///miracle/file', 'http://wiki.y-t.ru/cluster1', None),
    ('', 'http://wiki.y-t.ru/cluster1', None),
    (None, 'http://wiki.y-t.ru/cluster1', None),
    ('http://h.yandex-team.ru/?https%3A%2F%2Fclickhouse.yandex%2Fdocs%2Fen%2Fintroduction%2Fperformance',
     '', 'https://clickhouse.yandex/docs/en/introduction/performance'),
    ('https://h.yandex-team.ru/?http%3A%2F%2F%3CBFGServer%3E%2F%3C%D1%83%D1%80%D0%BB',
     '', 'http://%3Cbfgserver%3E/%3C%D1%83%D1%80%D0%BB'),
))
def test_url_normalization(url, base_url, expected):
    assert normalize_url(url, base_url) == expected
