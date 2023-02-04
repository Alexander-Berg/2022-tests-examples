import pytest

from intranet.femida.src.calendar.helpers import get_event_id_by_url


calendar_urls = [
    ('https://calendar.yandex-team.ru/event/1?applyToFuture=0', 1),
    ('https://calendar.yandex-team.ru/event/1', 1),
    ('https://calendar.yandex-team.ru/event/xcc', None),
    ('https://calendar.yandex-team.ru/event/?uid=1111&event_id=123', 123),
    ('https://calendar.yandex-team.ru/event/?event_id=123', 123),
    ('https://calendar.yandex-team.ru/event/?show_event_id=123', 123),
    ('https://calendar.yandex-team.ru/event/?uid=11', None),
]


@pytest.mark.parametrize('test_case', calendar_urls)
def test_get_event_id_by_url(test_case):
    url, event_id = test_case
    assert get_event_id_by_url(url) == event_id
