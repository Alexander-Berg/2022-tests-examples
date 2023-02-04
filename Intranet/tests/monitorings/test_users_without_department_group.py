# coding: utf-8


import pytest

from django.utils import timezone

from idm.users.models import User

pytestmark = [pytest.mark.django_db]


def test_users_without_department_group_ok(client, arda_users):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    User.objects.exclude(pk__in=[bilbo.pk, frodo.pk]).delete()

    assert frodo.memberships.filter(state='active', group__type='department').exists()
    bilbo.memberships.filter(group__type='department').update(
        state='inactive', date_leaved=timezone.now() - timezone.timedelta(hours=1)
    )

    response = client.get('/monitorings/users-without-department-group/')
    assert response.status_code == 200


def test_users_without_department_group_fail(client, arda_users):
    frodo = arda_users.frodo
    User.objects.exclude(pk=frodo.pk).delete()
    frodo.date_joined = timezone.now() - timezone.timedelta(hours=4)
    frodo.save()
    frodo.memberships.filter(group__type='department').update(
        state='inactive', date_leaved=timezone.now() - timezone.timedelta(hours=4)
    )
    response = client.get('/monitorings/users-without-department-group/')
    assert response.status_code == 400
    assert response.content == b'IDM has 1 users without department group: %s' % frodo.username.encode('utf-8')
