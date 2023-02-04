import pytest
from datetime import date, timedelta
from django.conf import settings
from django.test.utils import override_settings

from staff.celery_app import app

from staff.achievery.models import GivenAchievement
from staff.achievery.tests.factories import model
from staff.achievery.tests.factories.notifications import RouteFactory

from staff.lib.testing import (
    StaffFactory,
    GroupMembershipFactory,
)

from staff.person.tasks import (
    GiveAchievements,
    DepriveBeginnerAchievements,
    GiveEmployeeAchievements,
    DepriveEmployeeAchievements,
    ChangeLevelEmployeeAchievements,
)


class TestData(object):
    robot = None
    membership = None
    achievement = None


@pytest.fixture
def test_data(db):
    result = TestData()
    result.robot = StaffFactory(
        is_robot=True,
        login=settings.ACHIEVERY_ROBOT_PERSON_LOGIN,
    )
    result.membership = GroupMembershipFactory(
        staff=result.robot,
        group__url='achieveryadmin'
    )

    result.achievement = model.AchievementFactory()
    model.IconFactory(achievement=result.achievement, level=-1)
    RouteFactory(transport_id='email')

    return result


def test_give_achievement(test_data):
    victim = StaffFactory(
        is_robot=False,
        affiliation='yandex',
        is_dismissed=False,
    )

    @app.task
    class GiveTestAchievements(GiveAchievements):
        achievement_id = test_data.achievement.id

    GiveTestAchievements(persons_ids=[victim.id], nolock=True)

    assert victim.givenachievement_set.count() == 1

    given = victim.givenachievement_set.get()

    assert given.achievement.id == test_data.achievement.id
    assert given.person.id == victim.id


def test_deprive_beginner_achievement(test_data):
    users = [
        StaffFactory(join_at=d) for d in (
            date.today() - timedelta(settings.BEGINNER_DAYS - 1),
            date.today() - timedelta(settings.BEGINNER_DAYS),
            date.today() - timedelta(settings.BEGINNER_DAYS + 1),
        )
    ]
    givens = [
        model.GivenAchievementFactory(achievement=test_data.achievement, person=u, is_active=True)
        for u in users
    ]

    with override_settings(ACHIEVEMENT_BEGINNER_ID=test_data.achievement.id):
        # noinspection PyArgumentList
        DepriveBeginnerAchievements(nolock=True)

    assert GivenAchievement.objects.get(pk=givens[0].id).is_active

    assert not GivenAchievement.objects.get(pk=givens[1].id).is_active
    assert not GivenAchievement.objects.get(pk=givens[2].id).is_active


def test_update_employee_give(test_data):
    count = 5
    for d in range(count):
        StaffFactory(
            login='user{}'.format(d),
            is_dismissed=False,
            join_at=date.today() - timedelta(d * 366),
            affiliation='yandex',
            is_robot=False,
        )

    for level in range(count):
        model.IconFactory(achievement=test_data.achievement, level=level)

    with override_settings(ACHIEVEMENT_EMPLOYEE_ID=test_data.achievement.id):
        # noinspection PyArgumentList
        GiveEmployeeAchievements(nolock=True)
        DepriveEmployeeAchievements(nolock=True)
        ChangeLevelEmployeeAchievements(nolock=True)

    assert list(GivenAchievement.objects.values_list('level', flat=True)) == list(range(1, count))


def test_update_employee_levelup(test_data):
    count = 5
    users = [
        StaffFactory(
            login='user{}'.format(d),
            is_dismissed=False,
            join_at=date.today() - timedelta(d * 366),
            affiliation='yandex',
            is_robot=False,
        ) for d in range(count)
    ]
    for u in users:
        model.GivenAchievementFactory(
            achievement=test_data.achievement, level=1, person=u,
            is_active=True, is_hidden=False,
        )

    for level in range(count):
        model.IconFactory(achievement=test_data.achievement, level=level)

    with override_settings(ACHIEVEMENT_EMPLOYEE_ID=test_data.achievement.id):
        # noinspection PyArgumentList
        GiveEmployeeAchievements(nolock=True)
        DepriveEmployeeAchievements(nolock=True)
        ChangeLevelEmployeeAchievements(nolock=True)

    assert set(GivenAchievement.active.values_list('level', flat=True)) == set(range(1, count))


def test_update_employee_takeaway(test_data):
    count = 6
    users = [
        StaffFactory(
            is_dismissed=bool(d % 2),
            join_at=date.today() - timedelta(d * 366),
            affiliation=('yandex', 'yamoney', 'external')[d % 3],
            is_robot=False,
        ) for d in range(count)
    ]
    for u in users:
        model.GivenAchievementFactory(
            achievement=test_data.achievement, level=1, person=u,
            is_active=True, is_hidden=False,
        )

    for level in range(count):
        model.IconFactory(achievement=test_data.achievement, level=level)

    with override_settings(ACHIEVEMENT_EMPLOYEE_ID=test_data.achievement.id):
        # noinspection PyArgumentList
        GiveEmployeeAchievements(nolock=True)
        DepriveEmployeeAchievements(nolock=True)
        ChangeLevelEmployeeAchievements(nolock=True)

    assert GivenAchievement.inactive.count() == 1
