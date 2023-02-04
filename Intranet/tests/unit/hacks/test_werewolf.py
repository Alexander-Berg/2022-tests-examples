# coding: utf-8
from __future__ import absolute_import, unicode_literals

from django.test import modify_settings
from django.urls import reverse

from plan.hacks.models import Werewolf


@modify_settings(MIDDLEWARE_CLASSES={
    'append': 'plan.hacks.middleware.MoonNightMiddleware',
})
def test_werewolf(client, staff_factory):
    staff1 = staff_factory()
    staff2 = staff_factory()
    client.login(staff1.login)
    url = reverse('api-v4-common:user')
    response = client.json.get(url)
    data = response.json()
    assert data['login'] == staff1.login
    Werewolf.objects.create(login=staff1.login, mask=staff2.login, is_active=True)
    url = reverse('api-v4-common:user')
    response = client.json.get(url)
    data = response.json()
    assert data['login'] == staff2.login


@modify_settings(MIDDLEWARE_CLASSES={
    'append': 'plan.hacks.middleware.MoonNightMiddleware',
})
def test_common_werewolf(client, staff_factory, person, department):
    staff = staff_factory(department=department)
    person.department = department
    person.staff = staff
    person.save()

    client.login(person.login)

    Werewolf.objects.create(login=person.login, mask=staff.login, is_active=True)

    response = client.json.get('/common/werewolf/')
    assert response.status_code == 200
    result = response.json()['content']

    assert result == {
        'werewolf': {
            'id': staff.id,
            'login': staff.login,
            'firstName': staff.i_first_name,
            'lastName': staff.i_last_name,
            'isDismissed': staff.is_dismissed,
            'fullName': staff.get_full_name(),
            'is_robot': staff.is_robot,
            'affiliation': staff.affiliation,
            'is_frozen': staff.is_frozen,
        }
    }
