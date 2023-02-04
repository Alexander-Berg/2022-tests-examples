import pytest
from mock import MagicMock, patch

import datetime

from django.conf import settings

from staff.achievery.tests.factories.model import AchievementFactory, GivenAchievementFactory
from staff.achievery.models import GivenAchievement
from staff.lib.testing import StaffFactory, OfficeFactory, CityFactory

from staff.preprofile.controllers import ControllerError
from staff.preprofile.controllers.adopt_controller import AdoptController
from staff.preprofile.models import CANDIDATE_TYPE, PREPROFILE_STATUS


@pytest.mark.django_db
def test_adopt_moving_fired_employee():
    action_context = _create_action_context(candidate_type=CANDIDATE_TYPE.EXTERNAL_EMPLOYEE, is_dismissed=True)
    target = AdoptController(action_context)

    with pytest.raises(ControllerError) as exc:
        target.adopt()

    _assert_exception_message(exc, 'moving_fired_employee')


@pytest.mark.django_db
def test_adopt_join_date_in_future():
    action_context = _create_action_context()
    action_context.preprofile.join_at = datetime.date.today() + datetime.timedelta(days=1)

    target = AdoptController(action_context)

    with pytest.raises(ControllerError) as exc:
        target.adopt()

    _assert_exception_message(exc, 'join_date_in_future')


@pytest.mark.django_db
def test_adopt_has_inactive_department():
    action_context = _create_action_context()
    action_context.preprofile.department.intranet_status = 0

    target = AdoptController(action_context)

    with pytest.raises(ControllerError) as exc:
        target.adopt()

    _assert_exception_message(exc, 'inactive_department')


def _assert_exception_message(exc, message):
    errors = exc.value.errors_dict.get('errors')
    assert errors
    assert len(errors) == 1
    assert errors[0].get('message')
    assert errors[0]['message'] == message


def _create_action_context(
    candidate_type=CANDIDATE_TYPE.NEW_EMPLOYEE,
    is_dismissed=False,
):
    action_context = MagicMock()

    action_context.requested_by = StaffFactory()
    action_context.requested_by.user.is_superuser = True

    person = StaffFactory(is_dismissed=is_dismissed)
    action_context.preprofile.candidate_type = candidate_type
    action_context.preprofile.login = person.login
    action_context.preprofile.status = PREPROFILE_STATUS.READY
    action_context.preprofile.join_at = datetime.date.today() - datetime.timedelta(days=1)

    return action_context


@pytest.mark.django_db
@patch('staff.preprofile.adopt_api.create_staff_from_model', MagicMock())
def test_adopt_intern_first_time_get_achievement():
    today = datetime.date.today()
    date_completion_internship = today + datetime.timedelta(30)

    novosib = CityFactory(name='Новосибирск')
    novosib_office = OfficeFactory(city=novosib)

    achievement = AchievementFactory()
    settings.ACHIEVEMENT_INTERN_ID = achievement.id

    action_context = _create_action_context()
    action_context.preprofile.date_completion_internship = date_completion_internship
    action_context.preprofile.department.intranet_status = 1
    action_context.preprofile.office = novosib_office

    target = AdoptController(action_context)
    target.adopt()

    assert (
        GivenAchievement.objects
        .filter(
            person__login=action_context.preprofile.login,
            achievement_id=achievement.id,
            level=1,
            comment=f'Новосибирск, {today.year}'
        )
        .exists()
    )


@pytest.mark.django_db
@patch('staff.preprofile.adopt_api.create_staff_from_model', MagicMock())
def test_adopt_intern_second_time_get_achievement_level_rise():
    today = datetime.date.today()
    date_completion_internship = today + datetime.timedelta(30)

    novosib = CityFactory(name='Новосибирск')
    novosib_office = OfficeFactory(city=novosib)

    action_context = _create_action_context()
    action_context.preprofile.date_completion_internship = date_completion_internship
    action_context.preprofile.department.intranet_status = 1
    action_context.preprofile.office = novosib_office
    action_context.preprofile.login = 'uhura'

    person = StaffFactory(login=action_context.preprofile.login)
    achievement = AchievementFactory()
    settings.ACHIEVEMENT_INTERN_ID = achievement.id
    GivenAchievementFactory(achievement=achievement, level=1, person=person, comment='Москва, 2019')

    target = AdoptController(action_context)
    target.adopt()

    assert (
        GivenAchievement.objects
        .filter(
            person__login=action_context.preprofile.login,
            achievement_id=achievement.id,
            comment=f'Москва, 2019\nНовосибирск, {today.year}',
            level=2,
        )
        .exists()
    )


@pytest.mark.django_db
@patch('staff.preprofile.adopt_api.create_staff_from_model', MagicMock())
def test_intern_in_past_doesnt_get_achievement_level_rise():
    today = datetime.date.today()
    date_completion_internship = today - datetime.timedelta(30)

    action_context = _create_action_context()
    action_context.preprofile.date_completion_internship = date_completion_internship
    action_context.preprofile.department.intranet_status = 1

    person = StaffFactory(login=action_context.preprofile.login)
    achievement = AchievementFactory()
    settings.ACHIEVEMENT_INTERN_ID = achievement.id
    GivenAchievementFactory(achievement=achievement, level=1, person=person)

    target = AdoptController(action_context)
    target.adopt()

    with patch('staff.preprofile.controllers.adopt_controller.give_internship_achievement') as mocked_give:
        mocked_give.assert_not_called()


@pytest.mark.django_db
@patch('staff.preprofile.adopt_api.create_staff_from_model', MagicMock())
def test_not_intern_doesnt_get_achievement():
    action_context = _create_action_context()
    action_context.preprofile.date_completion_internship = None
    action_context.preprofile.department.intranet_status = 1

    target = AdoptController(action_context)
    target.adopt()

    with patch('staff.preprofile.controllers.adopt_controller.give_internship_achievement') as mocked_give:
        mocked_give.assert_not_called()
