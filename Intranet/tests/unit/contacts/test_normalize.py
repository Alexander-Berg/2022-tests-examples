import pytest

from plan.contacts.helpers import get_url_helper
from plan.exceptions import ContactError

data = [
    ('WIKI', 'wiki.yandex-team.ru/lala', 'lala'),
    ('WIKI', 'https://wiki.yandex-team.ru/lala/lala', 'lala/lala'),
    ('WIKI', 'wiki.yandex-team.ru/lala?a=5&b=6#c', 'lala?a=5&b=6#c'),
    ('WIKI', 'woko.yandex-team.ru', 'woko.yandex-team.ru'),

    ('AT_CLUB', 'at.yandex-team.ru/club', 'club'),

    ('MAILLIST', 'plan-dev', 'plan-dev'),
    ('MAILLIST', 'plan-dev@', 'plan-dev'),
    ('MAILLIST', 'plan-dev@yandex-team.ru', 'plan-dev'),
    ('MAILLIST', 'ml.yandex-team.ru/lists/plan-dev', 'plan-dev'),

    ('METRIKA_ID', '12345', '12345'),
    ('METRIKA_ID', 'ml.yandex-team.ru/stat/dashboard?counter_id=123', '123'),

    ('ST_FILTER', 'https://st.yandex-team.ru/filters/', 'https://st.yandex-team.ru/filters/'),

    ('URL', 'http://yandex.ru', 'http://yandex.ru'),
]


@pytest.mark.parametrize('contact_type,content,expected', data)
def test_normalize(contact_type, content, expected):
    try:
        normalized_content = get_url_helper(contact_type).normalize(content)
    except ContactError:
        assert False
    else:
        assert normalized_content == expected, normalized_content
