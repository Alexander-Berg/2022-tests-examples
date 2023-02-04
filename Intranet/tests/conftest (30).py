import pytest
from django.conf import settings

from staff.achievery.tests.factories.model import IconFactory, AchievementFactory
from staff.lib.testing import StaffFactory
from .factories.model import LogFactory


@pytest.fixture
def bootstrap_achievement(db):

    StaffFactory(
        is_robot=True,
        login=settings.ACHIEVERY_ROBOT_PERSON_LOGIN,
    )

    def bootstrap_achievement_(ach_id, max_level=1):
        achievement = AchievementFactory(id=ach_id)
        for level in range(max_level):
            IconFactory(achievement=achievement, level=level+1)

    return bootstrap_achievement_


@pytest.fixture
def bootstrap_users():

    def boostrap_users_():
        factory = StaffFactory
        factory_model = factory._meta.model

        try:
            # На случай если пользователь уже создан тестовым клиентом.
            tester = factory_model.objects.get(login='tester')

        except factory_model.DoesNotExist:
            tester = factory(login='tester')

        taker1 = factory(login='taker1', work_email='taker1@dummy.none')
        taker2 = factory(login='taker2', work_email='taker2@dummy.none')

        return tester, taker1, taker2

    return boostrap_users_


@pytest.fixture
def generate_logs():
    def generate_logs_(issuer, recipient, count=10):
        logs = []

        for _ in range(count):
            logs.append(LogFactory(issuer=issuer, recipient=recipient))

        return logs

    return generate_logs_
