# coding: utf-8
from unittest import mock
import pytest
from django.db.utils import OperationalError

from idm.core.models import Role

pytestmark = pytest.mark.django_db


class Raiser(object):
    def __init__(self, max, current=0):
        self.max = max
        self.current = current

    def __call__(self, *args, **kwargs):
        if self.current < self.max:
            self.current += 1
            raise OperationalError()


def test_lock_success(simple_system, arda_users):
    """Проверим, что в случае успеха _lock вызывается только один раз. Регрессия на IDM-4723"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    with mock.patch('time.sleep') as sleep:
        with mock.patch('idm.core.models.Role._lock') as _lock:
            role.lock(retry=True)

    assert sleep.call_args_list == []
    assert len(_lock.call_args_list) == 1


def test_lock_retry(simple_system, arda_users):
    """Проверим, что в случае неуспеха _lock вызывается до пяти раз"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    with mock.patch('time.sleep') as sleep:
        with mock.patch('idm.core.models.Role._lock') as _lock:
            _lock.side_effect = Raiser(3)  # 3 раза рейзимся, потом ничего не делаем
            role.lock(retry=True)

    assert len(_lock.call_args_list) == 4  # 3 попытки взять лок упали, но четвёртая прошла
    assert len(sleep.call_args_list) == 3  # спали мы при этом только 3 раза


def test_lock_failed(simple_system, arda_users):
    """Проверим, что если число ретраев превышено, эксепшен пробрасывается выше"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    with mock.patch('time.sleep') as sleep:
        with mock.patch('idm.core.models.Role._lock') as _lock:
            _lock.side_effect = Raiser(6)   # падаем на пятый раз
            with pytest.raises(OperationalError):
                role.lock(retry=True)

    assert len(sleep.call_args_list) == 5
    assert len(_lock.call_args_list) == 5
