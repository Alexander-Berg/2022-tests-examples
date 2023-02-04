import pytest

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.departments.models import Department
from staff.lib.testing import (
    StaffFactory,
    ValueStreamFactory,
    verify_forms_error_code,
)
from staff.person.models import Staff

from staff.umbrellas.forms import UmbrellaAssignmentForm
from staff.umbrellas.tests.factories import UmbrellaFactory


@pytest.mark.django_db
def test_umbrella_assignment_form():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [
                _create_valid_person(value_stream=value_stream).login,
                _create_valid_person(value_stream=value_stream).login,
            ],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': 51,
                },
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': 49,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is True


@pytest.mark.django_db
def test_umbrella_assignment_form_wildcard_assignment_none_umbrella():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [
                _create_valid_person(value_stream=value_stream).login,
                _create_valid_person(value_stream=value_stream).login,
            ],
            'umbrellas': [
                {
                    'umbrella': None,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is True


@pytest.mark.django_db
def test_umbrella_assignment_form_wildcard_assignment_no_umbrella():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [
                _create_valid_person(value_stream=value_stream).login,
                _create_valid_person(value_stream=value_stream).login,
            ],
            'umbrellas': [
                {
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is True


@pytest.mark.django_db
def test_umbrella_assignment_form_empty():
    target = UmbrellaAssignmentForm()

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons', 'required')
    verify_forms_error_code(target.errors, 'umbrellas', 'required')


@pytest.mark.django_db
def test_umbrella_assignment_form_no_umbrellas():
    target = UmbrellaAssignmentForm(data={'persons': [_create_valid_person().login]})

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas', 'required')


@pytest.mark.django_db
def test_umbrella_assignment_form_invalid_umbrella():
    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person().login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(intranet_status=0).issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas[0][umbrella]', 'invalid_choice')


@pytest.mark.django_db
def test_umbrella_assignment_form_no_persons():
    target = UmbrellaAssignmentForm(
        data={
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory().issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons', 'required')


@pytest.mark.django_db
def test_umbrella_assignment_form_invalid_person():
    target = UmbrellaAssignmentForm(
        data={
            'persons': [StaffFactory(intranet_status=0).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory().issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons[0]', 'invalid_choice')


@pytest.mark.django_db
def test_umbrella_assignment_form_no_engagement():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person(value_stream=value_stream).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas[0][engagement]', 'required')


@pytest.mark.django_db
def test_umbrella_assignment_form_invalid_engagement():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person(value_stream=value_stream).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': 'test',
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas[0][engagement]', 'invalid')


@pytest.mark.django_db
def test_umbrella_assignment_form_engagement_min_value():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person(value_stream=value_stream).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': -1,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas[0][engagement]', 'min_value')


@pytest.mark.django_db
def test_umbrella_assignment_form_engagement_max_value():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person(value_stream=value_stream).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': 101,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas[0][engagement]', 'max_value')


@pytest.mark.django_db
def test_umbrella_assignment_form_not_unique_persons():
    value_stream = ValueStreamFactory()
    person = _create_valid_person(value_stream=value_stream)

    target = UmbrellaAssignmentForm(
        data={
            'persons': [person.login, person.login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons', 'persons_not_unique')


@pytest.mark.django_db
def test_umbrella_assignment_form_not_unique_umbrellas():
    value_stream = ValueStreamFactory()
    umbrella = UmbrellaFactory(value_stream=value_stream)

    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person(value_stream=value_stream).login],
            'umbrellas': [
                {
                    'umbrella': umbrella.issue_key,
                    'engagement': 50,
                },
                {
                    'umbrella': umbrella.issue_key,
                    'engagement': 50,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas', 'umbrellas_not_unique')


@pytest.mark.django_db
def test_umbrella_assignment_form_invalid_total_engagement():
    value_stream = ValueStreamFactory()

    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person(value_stream=value_stream).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': 50.1,
                },
                {
                    'umbrella': UmbrellaFactory(value_stream=value_stream).issue_key,
                    'engagement': 50,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'umbrellas', 'invalid_total_engagement')


@pytest.mark.django_db
def test_umbrella_assignment_form_person_without_assignment():
    target = UmbrellaAssignmentForm(
        data={
            'persons': [StaffFactory(intranet_status=1).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory().issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, '', 'persons_without_main_assignment')


@pytest.mark.django_db
def test_umbrella_assignment_form_person_without_main_assignment():
    person = StaffFactory(intranet_status=1)
    BudgetPositionAssignmentFactory(
        main_assignment=False,
        person=person,
    )

    target = UmbrellaAssignmentForm(
        data={
            'persons': [person.login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory().issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, '', 'persons_without_main_assignment')


@pytest.mark.django_db
def test_umbrella_assignment_form_person_from_nested_value_streams():
    root_value_stream = ValueStreamFactory()
    nested_value_stream = ValueStreamFactory(parent=root_value_stream)

    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person(value_stream=nested_value_stream).login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory(value_stream=root_value_stream).issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is True


@pytest.mark.django_db
def test_umbrella_assignment_form_umbrella_from_different_value_stream():
    target = UmbrellaAssignmentForm(
        data={
            'persons': [_create_valid_person().login],
            'umbrellas': [
                {
                    'umbrella': UmbrellaFactory().issue_key,
                    'engagement': 100,
                },
            ],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, '', 'umbrella_from_different_value_stream')


def _create_valid_person(value_stream: Department = None) -> Staff:
    person = StaffFactory(intranet_status=1)

    BudgetPositionAssignmentFactory(
        value_stream=value_stream or ValueStreamFactory(),
        main_assignment=True,
        person=person,
    )

    return person
