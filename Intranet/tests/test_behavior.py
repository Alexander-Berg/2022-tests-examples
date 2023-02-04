import pytest

from json import dumps, loads

from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory, GroupMembershipFactory

from .factories.model import (
    AchievementFactory,
    GivenAchievementFactory,
    IconFactory,
)
from .factories.notifications import RouteFactory


@pytest.mark.django_db()
def test_clear_slot(client):
    """
    STAFF-3829 — нельзя взять и перетащить ачивку из Основных в Полученные
    """
    u = StaffFactory(login='tester')
    RouteFactory(transport_id='email')

    a1 = AchievementFactory()
    IconFactory(achievement=a1, level=-1)
    g1 = GivenAchievementFactory(achievement=a1, person=u, slot=1)

    response = client.post(
        reverse('achievery:given_details', kwargs={'pk': g1.id}),
        '{"slot": null, "revision": 0}', content_type='application/json',
    )

    assert response.status_code == 200, response.content

    answer = loads(response.content)

    assert answer['slot'] is None

    a2 = AchievementFactory()
    IconFactory(achievement=a2, level=-1)
    g2 = GivenAchievementFactory(achievement=a2, person=u, slot=1)
    url = reverse('achievery:given_details', kwargs={'pk': g2.id})

    response = client.post(
        url, '{"slot": "", "revision": 0}', content_type='application/json',
    )

    assert response.status_code == 200, response.content

    answer = loads(response.content)

    assert answer['slot'] is None


@pytest.mark.django_db()
def test_check_roles_after_giving(client):
    """
    STAFF-3826 — Сразу после выдачи ачивки на поп-апе нет кнопки «Отобрать»
    """
    u = StaffFactory(login='tester')
    RouteFactory(transport_id='email')
    m = GroupMembershipFactory(staff=u)

    a1 = AchievementFactory(owner_group=m.group)
    IconFactory(achievement=a1, level=-1)

    data = dumps({
        "achievement.id": a1.id, "person.id": u.id, "level": -1
    })
    response = client.post(
        reverse('achievery:given_list'),
        data, content_type='application/json',
    )

    assert response.status_code == 201, response.content

    answer = loads(response.content)
    roles = answer['roles']

    assert roles['active_holder'], roles


@pytest.mark.django_db()
def test_give_by_non_owner(client):
    """
    STAFF-3855 — не владелец ачивки может ее выдать через АПИ
    """
    u = StaffFactory(login='tester')
    RouteFactory(transport_id='email')

    a1 = AchievementFactory()
    IconFactory(achievement=a1, level=-1)

    roles = loads(client.get(
        reverse('achievery:achievement_details', kwargs={'pk': a1.id})
    ).content)['roles']

    assert not roles['active_owner']
    assert not roles['inactive_owner']
    assert not roles['hidden_owner']

    data = dumps({
        "achievement.id": a1.id, "person.id": u.id, "level": -1
    })
    response = client.post(
        reverse('achievery:given_list'),
        data, content_type='application/json',
    )

    assert response.status_code == 404, response.content


@pytest.mark.django_db()
def test_owner_take_away(client):
    """
    STAFF-4909: ачивки: 403 у владельца при попытке отобрать ачивку
    """
    u = StaffFactory(login='tester')
    u2 = StaffFactory(login='holder')
    RouteFactory(transport_id='email')
    m = GroupMembershipFactory(staff=u)

    a = AchievementFactory(owner_group=m.group)
    IconFactory(achievement=a, level=-1)
    g = GivenAchievementFactory(achievement=a, person=u2, slot=1)

    url = reverse('achievery:given_details', kwargs={'pk': g.id})
    response = client.post(
        url,
        dumps({"is_active": False, "revision": 0}),
        content_type='application/json',
    )

    assert response.status_code == 200, response.content
