import pytest

from staff.departments.controllers.person_action_validator import PersonActionValidator, PersonActionValidationError
from staff.departments.tests.factories import VacancyFactory
from staff.femida.constants import VACANCY_STATUS
from staff.femida.utils import FemidaUtils
from staff.headcounts.tests.factories import HeadcountPositionFactory


def test_validate_no_department():
    person_action = {}
    PersonActionValidator().validate(person_action)


def test_validate_no_move_without_budget():
    department = {'with_budget': True}
    person_action = {'department': department}
    PersonActionValidator().validate(person_action)


def _setup_validator(key):
    url = 'url'
    department = {'vacancy_url': url}
    person_action = {'department': department}

    def call(x):
        assert x == url
        return key

    femida_utils = FemidaUtils()
    femida_utils.get_vacancy_issue_key_from_url = call

    return PersonActionValidator(femida_utils), person_action


def test_validate_invalid_vacancy_key():
    validator, person_action = _setup_validator(None)

    with pytest.raises(PersonActionValidationError) as info:
        validator.validate(person_action)

    assert info.value.code == 'person_action_cannot_find_vacancy_key_error'


@pytest.mark.django_db
def test_validate_no_vacancy():
    key = 'key'
    validator, person_action = _setup_validator(key)
    VacancyFactory()

    with pytest.raises(PersonActionValidationError) as info:
        validator.validate(person_action)

    assert info.value.code == 'person_action_no_vacancies_error'


@pytest.mark.django_db
def test_validate_multiple_vacancies():
    key = 'key'
    validator, person_action = _setup_validator(key)
    VacancyFactory(ticket=key)
    VacancyFactory(ticket=key)

    with pytest.raises(PersonActionValidationError) as info:
        validator.validate(person_action)

    assert info.value.code == 'person_action_multiple_vacancies_error'


@pytest.mark.django_db
def test_validate_not_in_progress_vacancy():
    key = 'key'
    validator, person_action = _setup_validator(key)
    vacancy = VacancyFactory(ticket=key)
    HeadcountPositionFactory(code=vacancy.headcount_position_code)

    validator.validate(person_action)


@pytest.mark.django_db
def test_validate_no_budget_position():
    key = 'key'
    validator, person_action = _setup_validator(key)
    VacancyFactory(
        ticket=key,
        status=VACANCY_STATUS.IN_PROGRESS,
        budget_position=None,
        headcount_position_code=None,
    )

    with pytest.raises(PersonActionValidationError) as info:
        validator.validate(person_action)

    assert info.value.code == 'person_action_vacancy_without_budget_position_error'


@pytest.mark.django_db
def test_validate_no_headcount_position():
    key = 'key'
    validator, person_action = _setup_validator(key)
    VacancyFactory(ticket=key, status=VACANCY_STATUS.IN_PROGRESS)

    with pytest.raises(PersonActionValidationError) as info:
        validator.validate(person_action)

    assert info.value.code == 'person_action_no_headcount_position_error'


@pytest.mark.django_db
def test_validate():
    key = 'key'
    validator, person_action = _setup_validator(key)
    vacancy = VacancyFactory(ticket=key, status=VACANCY_STATUS.IN_PROGRESS)
    HeadcountPositionFactory(code=vacancy.headcount_position_code)

    validator.validate(person_action)
