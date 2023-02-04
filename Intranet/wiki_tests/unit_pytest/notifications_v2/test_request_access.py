import pytest

from wiki.notifications.models import PageEvent
from wiki.notifications_v2.request_access.message import create_messages
from wiki.utils import timezone
from wiki.pages.models import AccessRequest
from model_mommy import mommy

from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet, only_biz

pytestmark = [pytest.mark.django_db]


def _generate_request_access_event(page, applicant, reason='foobar') -> tuple[PageEvent, AccessRequest]:
    request = mommy.make(AccessRequest, applicant=applicant, page=page, reason=reason)

    event = mommy.make(
        PageEvent,
        event_type=PageEvent.EVENT_TYPES.request_access,
        author=applicant,
        timeout=timezone.now(),
        page=page,
        meta={'access_request_id': request.id, 'reason': reason},
    )
    return event, request


@only_biz
def test__message__b2b(wiki_users, test_page):
    author = wiki_users.kolomeetz
    test_page.authors.set([author])

    event, request = _generate_request_access_event(test_page, applicant=wiki_users.asm)
    cloud_uid, message = create_messages(event)[0]

    assert cloud_uid == wiki_users.kolomeetz.get_cloud_uid()
    assert_json(
        message.dict(),
        {
            'authorName': {'en': 'Konstantin Kolomeetz', 'ru': 'Константин Коломеец'},
            'userStaff': {'en': 'Alexey Mazurov', 'ru': 'Алексей Мазуров'},
            'userName': {'en': 'Alexey Mazurov', 'ru': 'Алексей Мазуров'},
            # 'userInflect': {
            #     'genitive': 'Алексея Мазурова',
            #     'dative': 'Алексею Мазурову',
            # },
            'page': {
                'slug': 'testpage',
                'shortSlug': '/testpage',
                'title': test_page.title,
                'link': 'https://wiki.test.yandex.ru/testpage/',
            },
            'accessLink': {
                'grant': f'https://wiki.test.yandex.ru/testpage/.requestaccess?action=allow&id={request.id}',
                'deny': f'https://wiki.test.yandex.ru/testpage/.requestaccess?action=deny&id={request.id}',
            },
            'userVerb': '',
            'reason': 'foobar',
            'docLink': 'https://yandex.ru/support/connect-wiki/notifications.html',
        },
    )


@only_intranet
def test__message__intranet(wiki_users, test_page):
    author = wiki_users.kolomeetz
    test_page.authors.set([author])

    event, request = _generate_request_access_event(test_page, applicant=wiki_users.asm)
    cloud_uid, message = create_messages(event)[0]

    assert cloud_uid == wiki_users.kolomeetz.get_cloud_uid()
    assert_json(
        message.dict(),
        {
            'authorName': {'en': 'Konstantin Kolomeetz', 'ru': 'Константин Коломеец'},
            'userStaff': {
                'en': '[Alexey Mazurov](https://staff.yandex-team.ru/asm)',
                'ru': '[Алексей Мазуров](https://staff.yandex-team.ru/asm)',
            },
            'userName': {'en': 'Alexey Mazurov', 'ru': 'Алексей Мазуров'},
            # 'userInflect': {
            #     'genitive': 'Алексея Мазурова',
            #     'dative': 'Алексею Мазурову',
            # },
            'page': {
                'slug': 'testpage',
                'shortSlug': '/testpage',
                'title': test_page.title,
                'link': 'https://wiki.test.yandex-team.ru/testpage/',
            },
            'accessLink': {
                'grant': f'https://wiki.test.yandex-team.ru/testpage/.requestaccess?action=allow&id={request.id}',
                'deny': f'https://wiki.test.yandex-team.ru/testpage/.requestaccess?action=deny&id={request.id}',
            },
            'userVerb': '',
            'reason': 'foobar',
            'docLink': 'https://wiki.test.yandex-team.ru/wiki/vodstvo/pageChanges/',
        },
    )
