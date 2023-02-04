from mock import MagicMock
import pytest
from django.core.management import call_command

from idm.monitorings.tasks import CalculateUnistatMetrics
from idm.core.constants.passport_login import PASSPORT_LOGIN_STATE
from idm.core.models import Role, UserPassportLogin
from idm.tests.utils import raw_make_role

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('threshold', [None, 1, 3])
def test_hanging_roles(arda_users, simple_system, client, settings, threshold):
    """Проверяем работу мониторинга зависших ролей"""
    if threshold is not None:
        settings.IDM_SID67_THRESHOLD = threshold

    raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-frodo'},
        system_specific={'passport-login': 'yndx-frodo'},
    )
    raw_make_role(
        arda_users.legolas, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-legolas'},
        system_specific={'passport-login': 'yndx-legolas'},
    )
    
    CalculateUnistatMetrics.delay()

    response = client.get('/monitorings/logins-to-subscribe/')
    if threshold in (None, 3):
        assert response.status_code == 200
    else:
        assert response.status_code == 400
        assert response.content == b'IDM has 2 passport logins to subscribe'


@pytest.mark.parametrize('subscribed', [True, False])
def test_subscibed_logins_actually_not_subscribed(client, arda_users, generic_system,
                                                  simple_system, monkeypatch, subscribed):

    class PassportMock(object):
        def userinfo(self, *args, **kwargs):
            return {
                'uid': '123456',
                'fields': {'suid': '1' if subscribed else None},
            }

    monkeypatch.setattr('idm.sync.passport.exists', lambda *args, **kwargs: True)
    monkeypatch.setattr('blackbox.Blackbox.userinfo', PassportMock().userinfo)

    fake_subscriber = MagicMock()
    fake_subscriber.return_value = True
    monkeypatch.setattr('idm.sync.passport.set_strongpwd', fake_subscriber)

    frodo = arda_users['frodo']

    Role.objects.request_role(
        frodo, frodo, generic_system, '', {'role': 'manager'}, {'passport-login': 'yndx-frodo'}
    )
    raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-frodo'},
        system_specific={'passport-login': 'yndx-frodo'},
    )

    login = UserPassportLogin.objects.get()

    login.subscribe()
    login.refresh_from_db()

    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    call_command('idm_check_passport_logins_subscribed')
    response = client.get('/monitorings/unsubscribed-logins/')

    if subscribed:
        assert response.status_code == 200
        assert response.content == b'ok'
    else:
        assert response.status_code == 400
        assert response.content == (
            b'IDM has 1 passport logins in subscribed state, but actually not subscribed: yndx-frodo'
        )
        assert fake_subscriber.call_count == 1
        call_command('idm_check_passport_logins_subscribed', fix_all=True)
        assert fake_subscriber.call_count == 2
        call_command('idm_check_passport_logins_subscribed', fix='yndx-frodo,yndx-smth')
        assert fake_subscriber.call_count == 3
        call_command('idm_check_passport_logins_subscribed', fix='frodo,smth')
        assert fake_subscriber.call_count == 3
