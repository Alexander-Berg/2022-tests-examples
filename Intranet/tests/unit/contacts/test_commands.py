import pytest

from plan.contacts.management.commands.oneoff_translate_st_query_links import Command
from common import factories

pytestmark = pytest.mark.django_db


links = [
    (
        'https://st.yandex-team.ru/filters/filter/?query=Queueabc+empty()',
        'https://st.yandex-team.ru/issues/?_q=Queueabc+empty()',
    ),
    (
        'https://st.yandex-team.ru/filters/filter:11861',
        'https://st.yandex-team.ru/filters/filter:11861',
    ),
    (
        'https://st.yandex-team.ru/filters/grouping:priority/filter/?query=Type',
        'https://st.yandex-team.ru/issues/?_g=priority&_q=Type',
    ),
    (
        'https://st.yandex-team.ru/filters/order:updated:false/filter?query=QUEUE',
        'https://st.yandex-team.ru/issues?_o=updated+DESC&_q=QUEUE',
    ),
    (
        'https://st.yandex-team.ru/filters/order:updated/filter?query=QUEUE',
        'https://st.yandex-team.ru/issues?_o=updated+ASC&_q=QUEUE',
    ),
]


@pytest.mark.parametrize('link,result', links)
def test_startrek_link_fix(link, result):
    contact = factories.ServiceContactFactory(content=link)
    Command().handle()
    contact.refresh_from_db()
    assert contact.content == result
