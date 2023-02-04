import responses
import pytest

from contextlib import nullcontext as does_not_raise
from requests.exceptions import HTTPError

from watcher.config import settings
from watcher.logic.clients.jns import jns_client
from watcher.logic.notification import (
    get_notification_channels,
)


def test_get_channel(bot_user_factory, staff_factory, scope_session):
    bot_user = bot_user_factory()
    staff = staff_factory()
    channels = get_notification_channels(
        db=scope_session,
        staff_ids=[staff.id, bot_user.staff.id]
    )
    assert channels == {
        staff.id: 'email',
        bot_user.staff.id: 'telegram',
    }


@responses.activate
def test_jns_client_send_message():
    responses.add(
        responses.POST,
        'https://jns.yandex-team.ru/api/messages/send',
        status=200,
        json={'results': []}
    )
    response = jns_client.send_message(
        template=settings.JNS_START_SHIFT_SOON_TEMPLATE,
        login='some_login',
        channel='email',
        params={'duty_name': {'string_value': 'some_duty_name'}},
    )
    assert response.request.body == (
        b'{"project": "Watcher-test", "template": "duty_start_notification_soon", '
        b'"recipient": {"email": {"internal": [{"login": "some_login"}]}}, '
        b'"params": {"duty_name": {"string_value": "some_duty_name"}}}'
    )


@responses.activate
@pytest.mark.parametrize(
    ('status_code', 'response', 'expected'),
    [
        (400, {'code': 5}, pytest.raises(HTTPError)),
        (400, {'code': 3}, does_not_raise()),
        (409, {'code': 6}, does_not_raise()),
        (409, {'code': 7}, pytest.raises(HTTPError)),
        (500, {}, pytest.raises(HTTPError)),
    ]
)
def test_jns_client_send_message_with_error(status_code, response, expected):
    responses.add(
        responses.POST,
        'https://jns.yandex-team.ru/api/messages/send',
        status=status_code,
        json=response,
    )
    with expected:
        jns_client.send_message(
            template=settings.JNS_START_SHIFT_SOON_TEMPLATE,
            login='some_login',
            channel='email',
            params={'duty_name': {'string_value': 'some_duty_name'}},
        )
