import datetime
from freezegun import freeze_time
from mock import patch

import pytest
from infra.cauth.server.common.alchemy import Session
from infra.cauth.server.master.notify.events import removed_from_group
from infra.cauth.server.master.notify.tasks import send_notifications
from infra.cauth.server.common.models import Access
from __tests__.utils import (
    assert_contains,
    add_user_to_group,
    create_user_group,
    create_server,
    create_access_rule,
)


pytestmark = pytest.mark.django_db


@patch('infra.cauth.server.master.notify.notifications.shorten_url', return_value='https://nda.ya.ru/t/shortened')
def test_send_notifications(mocked, users):
    user = users.frodo
    group = create_user_group('The Fellowship Of The Ring', 1)
    server = create_server('mordor.yandex-team.ru')
    create_access_rule('ssh', group, server)
    create_access_rule('sudo', group, server)
    access_additional = create_access_rule('sudo', users.legolas, server)
    access_additional.approver = None
    Session.commit()
    with freeze_time(datetime.datetime.now() - datetime.timedelta(days=14)):
        add_user_to_group(user, group, datetime.datetime.now())
        # TODO Сделать через синхронизацию
        removed_from_group('frodo', 'The Fellowship Of The Ring')

    correct_text = [
        # 'CAuth: некоторые доступы будут отозваны',
        'Здравствуйте',
        'Некоторые ваши доступы будут отозваны',
        'в связи с выходом из групп.',
        'Чтобы предотвратить отзыв ролей, вы можете восстановить членство в группах,',
        'связавшись с ответственными группы или запросить такую же роль в IDM.',
        'В связи с выходом из группы The Fellowship Of The Ring будут отозваны:',
        '* ssh: mordor.yandex-team.ru',
        '* sudo: mordor.yandex-team.ru (ALL=(ALL) ALL)',
        'Запросить роли в IDM: https://nda.ya.ru/',
        'Централизованная Аутентификация',
        'http://wiki.yandex-team.ru/security/CAuth',
    ]
    with patch('smtplib.SMTP') as mocked_smtp:
        access_query = Access.query.all()
        send_notifications()
        assert Access.query.all() == access_query
        from_mail, to_mail, message_text = mocked_smtp.mock_calls[1][1]
        assert from_mail == 'CAuth Mailer <cauth-robot-rw@yandex-team.ru>'
        assert to_mail == {'cauth-cc@yandex-team.ru', 'frodo@yandex-team.ru'}
        assert_contains(correct_text, message_text)
