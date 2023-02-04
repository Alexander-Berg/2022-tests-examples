import pytest

from staff.achievery.models import GivenAchievement
from staff.kudos.controllers import AddKudoController
from staff.kudos.tasks import AchKudosGivenTask
from staff.lib.testing import RouteFactory


@pytest.mark.django_db
def test_achievement_give(bootstrap_achievement, bootstrap_users, generate_logs, settings):

    settings.ACHIEVEMENT_KUDOS_GIVER_ID = 777

    bootstrap_achievement(ach_id=settings.ACHIEVEMENT_KUDOS_GIVER_ID, max_level=3)
    tester, taker1, _ = bootstrap_users()

    ach_active = GivenAchievement.active

    def generate(count):
        generate_logs(issuer=tester, recipient=taker1, count=count)
        AddKudoController.give_achievement(issuer=tester)

    generate(count=4)
    assert ach_active.count() == 0

    generate(count=1)
    assert ach_active.count() == 1


def test_achievement_calculate_level():
    calculate_level = AchKudosGivenTask.calculate_level
    assert calculate_level(4) == 0
    assert calculate_level(5) == 1
    assert calculate_level(9) == 1
    assert calculate_level(10) == 2
    assert calculate_level(19) == 2
    assert calculate_level(20) == 3


@pytest.mark.django_db
def test_notification_kudos_given(bootstrap_users, generate_logs, notifications, django_assert_num_queries):

    tester, taker1, taker2 = bootstrap_users()

    logs = []
    logs.extend(generate_logs(issuer=tester, recipient=taker1, count=1))
    logs.extend(generate_logs(issuer=tester, recipient=taker2, count=1))

    RouteFactory(
        target='@',
        department=None,
        office=None,
        staff=None,
        transport_id='email',
        params='{}'
    )

    with django_assert_num_queries(1):
        AddKudoController.notify(logs)

    assert len(notifications) == 1
    assert notifications[0].params['recipients'] == [
        'taker1@dummy.none', 'taker2@dummy.none']
