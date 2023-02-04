import pytest
from django.utils import timezone

from idm.metrics.blocking_time import blocking_time

pytestmark = pytest.mark.django_db


def test_blocking_time_empty_list(arda_users):
    assert blocking_time() == [
        {'slug': 'blocking_time_90', 'value': 0.0},
        {'slug': 'blocking_time_95', 'value': 0.0},
        {'slug': 'blocking_time_99', 'value': 0.0},
        {'slug': 'blocking_time_100', 'value': 0.0},
    ]


def test_blocking_time_one_element(arda_users):
    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.ldap_active = False
    frodo.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=4)
    frodo.ldap_blocked_timestamp = timezone.now() - timezone.timedelta(hours=2)
    frodo.save()

    assert blocking_time() == [
        {'slug': 'blocking_time_90', 'value': 2.0},
        {'slug': 'blocking_time_95', 'value': 2.0},
        {'slug': 'blocking_time_99', 'value': 2.0},
        {'slug': 'blocking_time_100', 'value': 2.0},
    ]


def test_blocking_time_several_elements(arda_users):
    # Уволили 3 часа назад, в AD заблокировали 2 часа назад
    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.ldap_active = False
    frodo.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=3)
    frodo.ldap_blocked_timestamp = timezone.now() - timezone.timedelta(hours=2)
    frodo.save()

    # Уволили 3 часа назад, в AD не заблокировали
    bilbo = arda_users.bilbo
    bilbo.is_active = False
    bilbo.ldap_active = True
    bilbo.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=3)
    bilbo.save()

    # Уволили 31 день назад, в AD не заблокировали, на график попадёт
    aragorn = arda_users.aragorn
    aragorn.is_active = False
    aragorn.ldap_active = True
    aragorn.idm_found_out_dismissal = timezone.now() - timezone.timedelta(days=31)
    aragorn.save()

    # Уволили 32 дня назад, в AD заблокировали 31 день назад, на график не попадёт
    legolas = arda_users.legolas
    legolas.is_active = False
    legolas.ldap_active = False
    legolas.idm_found_out_dismissal = timezone.now() - timezone.timedelta(days=32)
    legolas.ldap_blocked_timestamp = timezone.now() - timezone.timedelta(days=31)
    legolas.save()

    assert blocking_time() == [
        {'slug': 'blocking_time_90', 'value': 3.0},
        {'slug': 'blocking_time_95', 'value': 3.0},
        {'slug': 'blocking_time_99', 'value': 3.0},
        {'slug': 'blocking_time_100', 'value': 744.0},
    ]
