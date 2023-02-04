import factory
import pytest

from django.contrib.auth.models import Permission
from django.core.exceptions import PermissionDenied
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory
from staff.person_profile.views.badge_view import deactivate
from staff.rfid.constants import STATE
from staff.rfid.models import Badge


class BadgeFactory(factory.DjangoModelFactory):
    class Meta:
        model = Badge


@pytest.fixture()
def admin():
    admin = StaffFactory(login='admin_login')
    admin.user.user_permissions.add(
        Permission.objects.get(codename='can_deactivate_badge')
    )
    return admin


@pytest.fixture()
def not_admin():
    return StaffFactory(login='not_admin_login')


@pytest.mark.django_db
def test_deactivate_by_absent_login(rf, admin):
    # given
    non_existent_login = 'non_existent_login'
    request = rf.post(reverse('profile:deactivate_badge', kwargs={'login': non_existent_login}))
    request.user = admin.user

    # when
    result = deactivate(request, non_existent_login)

    # then
    assert result.status_code == 404


@pytest.mark.django_db
def test_deactivate_absent_badge(rf, admin):
    # given
    person = StaffFactory()
    request = rf.post(reverse('profile:deactivate_badge', kwargs={'login': person.login}))
    request.user = admin.user

    # when
    result = deactivate(request, person.login)

    # then
    assert result.status_code == 404


@pytest.mark.django_db
def test_user_deactivates_somebody_else_badge(rf, not_admin):
    # given
    badge = BadgeFactory(person=StaffFactory(login='user1'), state=STATE.ACTIVE)
    request = rf.post(reverse('profile:deactivate_badge', kwargs={'login': badge.person.login}))
    request.user = not_admin.user

    # when
    with pytest.raises(PermissionDenied):
        assert deactivate(request, badge.person.login)

    # then
    assert Badge.objects.filter(person__login=badge.person.login).count() == 1
    # бейдж должен остаться активным
    assert Badge.objects.filter(state=STATE.ACTIVE, person__login=badge.person.login).count() == 1


@pytest.mark.django_db
def test_deactivate_existing_badge_by_admin(rf, admin):
    # given
    badge = BadgeFactory(person=StaffFactory(login='loser'), state=STATE.ACTIVE)  # бейдж для блокировки
    BadgeFactory(person=StaffFactory(login='user1'), state=STATE.ACTIVE)  # этот бейдж не блокируем
    BadgeFactory(person=StaffFactory(login='user2'), state=STATE.ACTIVE)  # этот бейдж не блокируем
    request = rf.post(reverse('profile:deactivate_badge', kwargs={'login': badge.person.login}))
    request.user = admin.user
    assert Badge.objects.filter(state=STATE.ACTIVE).count() == 3
    assert Badge.objects.filter(state=STATE.ACTIVE, person__login=badge.person.login).count() == 1

    # when
    result = deactivate(request, badge.person.login)

    # then
    assert result.status_code == 200
    # бейдж заданного сотрудника должен перестать быть активным
    assert Badge.objects.filter(state=STATE.ACTIVE, person__login=badge.person.login).count() == 0
    # остальные бейджи должны остаться активными
    assert Badge.objects.filter(state=STATE.ACTIVE).count() == 2


@pytest.mark.django_db
def test_user_deactivates_his_badge(rf, not_admin):
    # given
    badge = BadgeFactory(person=not_admin, state=STATE.ACTIVE)  # бейдж для блокировки
    BadgeFactory(person=StaffFactory(login='user1'), state=STATE.ACTIVE)  # этот бейдж не блокируем
    BadgeFactory(person=StaffFactory(login='user2'), state=STATE.ACTIVE)  # этот бейдж не блокируем
    request = rf.post(reverse('profile:deactivate_badge', kwargs={'login': badge.person.login}))
    request.user = not_admin.user
    assert Badge.objects.filter(state=STATE.ACTIVE).count() == 3
    assert Badge.objects.filter(state=STATE.ACTIVE, person__login=badge.person.login).count() == 1

    # when
    result = deactivate(request, badge.person.login)

    # then
    assert result.status_code == 200
    # бейдж заданного сотрудника должен перестать быть активным
    assert Badge.objects.filter(state=STATE.ACTIVE, person__login=badge.person.login).count() == 0
    # остальные бейджи должны остаться активными
    assert Badge.objects.filter(state=STATE.ACTIVE).count() == 2


@pytest.mark.django_db
def test_user_deactivates_his_blocked_badge(rf, not_admin):
    # given
    badge = BadgeFactory(person=not_admin, state=STATE.INACTIVE)  # заблокированный бейдж
    request = rf.post(reverse('profile:deactivate_badge', kwargs={'login': badge.person.login}))
    request.user = not_admin.user
    assert Badge.objects.filter(state=STATE.INACTIVE, person__login=badge.person.login).count() == 1

    # when
    result = deactivate(request, badge.person.login)

    # then
    assert result.status_code == 404
