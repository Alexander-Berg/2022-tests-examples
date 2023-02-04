# coding: utf-8


import pytest

from django.utils import timezone

from idm.users.models import User

pytestmark = [pytest.mark.django_db]


def test_not_blocked_ad_users_ok(client, arda_users):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    legolas = arda_users.legolas

    frodo.idm_found_out_dismissal = None
    frodo.ldap_active = True
    frodo.is_active = True
    frodo.save()

    bilbo.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=1)
    bilbo.is_active = False
    bilbo.ldap_active = True
    bilbo.save()

    legolas.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=3)
    legolas.is_active = False
    legolas.ldap_active = False
    legolas.save()

    User.objects.exclude(pk__in=[bilbo.pk, frodo.pk, legolas.pk]).delete()

    response = client.get('/monitorings/not-blocked-ad-users/')
    assert response.status_code == 200


def test_not_blocked_ad_users_fail(client, arda_users):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    legolas = arda_users.legolas

    frodo.idm_found_out_dismissal = None
    frodo.ldap_active = True
    frodo.is_active = True
    frodo.save()

    bilbo.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=1)
    bilbo.is_active = False
    bilbo.ldap_active = None
    bilbo.save()

    legolas.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=3)
    legolas.is_active = False
    legolas.ldap_active = True
    legolas.save()

    User.objects.exclude(pk__in=[bilbo.pk, frodo.pk, legolas.pk]).delete()
    response = client.get('/monitorings/not-blocked-ad-users/')

    assert response.status_code == 400
    assert response.content == b'IDM has 1 fired, but active in AD users: %s' % legolas.username.encode('utf-8')
