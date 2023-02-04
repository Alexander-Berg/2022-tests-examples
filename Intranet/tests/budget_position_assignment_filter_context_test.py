import pytest

from random import random

from staff.budget_position.models import BudgetPositionAssignmentStatus, ReplacementType
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory, RewardFactory
from staff.departments.tests.factories import VacancyFactory
from staff.lib.testing import (
    BudgetPositionFactory,
    DepartmentFactory,
    GeographyDepartmentFactory,
    StaffFactory,
    ValueStreamFactory,
)

from staff.headcounts.budget_position_assignment_filter_context import BudgetPositionAssignmentFilterContext
from staff.headcounts.forms import QUANTITY_FILTER


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_position_statuses():
    status = BudgetPositionAssignmentStatus.MATERNITY.value
    assignment = BudgetPositionAssignmentFactory(status=status)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(position_statuses=[status])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_replacement_type():
    replacement_type = ReplacementType.WITHOUT_REPLACEMENT.value
    assignment = BudgetPositionAssignmentFactory(replacement_type=replacement_type)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(replacement_type=[replacement_type])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'category_is_new',
    [
        True,
        False,
    ],
)
def test_positions_objects_qs_filter_by_category_is_new(category_is_new):
    assignment = BudgetPositionAssignmentFactory(creates_new_position=category_is_new)
    BudgetPositionAssignmentFactory(creates_new_position=not category_is_new)

    target = BudgetPositionAssignmentFilterContext(category_is_new=category_is_new)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'main_assignment',
    [
        True,
        False,
    ],
)
def test_positions_objects_qs_filter_by_main_assignment(main_assignment):
    assignment = BudgetPositionAssignmentFactory(main_assignment=main_assignment)
    BudgetPositionAssignmentFactory(main_assignment=not main_assignment)

    target = BudgetPositionAssignmentFilterContext(main_assignment=main_assignment)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_is_crossing():
    assignment = BudgetPositionAssignmentFactory()
    next_assignment = BudgetPositionAssignmentFactory(previous_assignment=assignment)

    target_class = BudgetPositionAssignmentFilterContext

    assert target_class(is_crossing=True).positions_objects_qs().get().pk == assignment.pk
    assert target_class(is_crossing=False).positions_objects_qs().get().pk == next_assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_category():
    reward = RewardFactory(category='Mass', name='Mass')
    assignment = BudgetPositionAssignmentFactory(reward=reward)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(category=['Mass'])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_current_person():
    current_person = StaffFactory()
    assignment = BudgetPositionAssignmentFactory(person=current_person)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(current_person=current_person)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_replaced_person():
    replaced_person = StaffFactory()
    assignment = BudgetPositionAssignmentFactory(
        previous_assignment=BudgetPositionAssignmentFactory(person=replaced_person),
    )
    BudgetPositionAssignmentFactory(previous_assignment=BudgetPositionAssignmentFactory())

    target = BudgetPositionAssignmentFilterContext(replaced_person=replaced_person)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_code():
    budget_position = BudgetPositionFactory()
    assignment = BudgetPositionAssignmentFactory(budget_position=budget_position)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(code=budget_position.code)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_codes():
    budget_position = BudgetPositionFactory()
    assignment = BudgetPositionAssignmentFactory(budget_position=budget_position)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(codes=[budget_position.code])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_department_url():
    department = DepartmentFactory()
    assignment = BudgetPositionAssignmentFactory(department=department)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(department_url=department.url)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_exact_department():
    department = DepartmentFactory()
    assignment = BudgetPositionAssignmentFactory(department=department)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(department=department)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_nested_department():
    department = DepartmentFactory()
    a_nested_department = DepartmentFactory(parent=department)
    assignment = BudgetPositionAssignmentFactory(department=a_nested_department)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(department=department)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_exact_value_stream():
    value_stream = ValueStreamFactory()
    assignment = BudgetPositionAssignmentFactory(value_stream=value_stream)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(value_stream=value_stream)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_nested_value_stream():
    value_stream = ValueStreamFactory()
    a_nested_value_stream = ValueStreamFactory(parent=value_stream)
    assignment = BudgetPositionAssignmentFactory(value_stream=a_nested_value_stream)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(value_stream=value_stream)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_exact_geography():
    geography = GeographyDepartmentFactory()
    assignment = BudgetPositionAssignmentFactory(geography=geography)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(geography=geography)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_nested_geography():
    geography = GeographyDepartmentFactory()
    a_nested_geography = GeographyDepartmentFactory(parent=geography)
    assignment = BudgetPositionAssignmentFactory(geography=a_nested_geography)
    BudgetPositionAssignmentFactory()

    target = BudgetPositionAssignmentFilterContext(geography=geography)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'quantity, correct_headcount, wrong_headcount',
    [
        [QUANTITY_FILTER.ZERO, 0, 1],
        [QUANTITY_FILTER.ZERO, 0, -1],
        [QUANTITY_FILTER.GREATER_THAN_ZERO, 1, 0],
        [QUANTITY_FILTER.GREATER_THAN_ZERO, 1, -1],
        [QUANTITY_FILTER.LESS_THAN_ZERO, -1, 0],
        [QUANTITY_FILTER.LESS_THAN_ZERO, -1, 1],
    ],
)
def test_positions_objects_qs_filter_by_quantity(quantity, correct_headcount, wrong_headcount):
    assignment = BudgetPositionAssignmentFactory(budget_position=BudgetPositionFactory(headcount=correct_headcount))
    BudgetPositionAssignmentFactory(budget_position=BudgetPositionFactory(headcount=wrong_headcount))

    target = BudgetPositionAssignmentFilterContext(quantity=quantity)

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_qs_default_fields():
    assignment = BudgetPositionAssignmentFactory(person=StaffFactory())

    target = BudgetPositionAssignmentFilterContext()

    result = target.positions_qs()

    assert result.get() == {
        'budget_position__code': assignment.budget_position.code,
        'name': assignment.name,
        'budget_position__headcount': assignment.budget_position.headcount,
        'change_reason': assignment.change_reason,
        'creates_new_position': assignment.creates_new_position,
        'department__id': assignment.department_id,
        'department__name': assignment.department.name,
        'department__name_en': assignment.department.name_en,
        'department__url': assignment.department.url,
        'department_id': assignment.department_id,
        'geography__id': assignment.geography.id,
        'geography__name': assignment.geography.name,
        'geography__name_en': assignment.geography.name_en,
        'geography__url': assignment.geography.url,
        'geography_id': assignment.geography.id,
        'id': assignment.id,
        'main_assignment': assignment.main_assignment,
        'next_assignment__id': None,
        'previous_assignment_id': None,
        'person__first_name': assignment.person.first_name,
        'person__first_name_en': assignment.person.first_name_en,
        'person__last_name': assignment.person.last_name,
        'person__last_name_en': assignment.person.last_name_en,
        'person__login': assignment.person.login,
        'previous_assignment__person__first_name': None,
        'previous_assignment__person__first_name_en': None,
        'previous_assignment__person__last_name': None,
        'previous_assignment__person__last_name_en': None,
        'previous_assignment__person__login': None,
        'replacement_type': str(assignment.replacement_type),
        'reward_id': assignment.reward.id,
        'reward__category': assignment.reward.category,
        'status': assignment.status,
        'value_stream__id': assignment.value_stream.id,
        'value_stream__name': assignment.value_stream.name,
        'value_stream__name_en': assignment.value_stream.name_en,
        'value_stream__url': assignment.value_stream.url,
        'value_stream_id': assignment.value_stream.id,
    }


@pytest.mark.django_db
@pytest.mark.parametrize(
    'exclude_reserve, expected_results',
    [
        [True, 2],
        [False, 3],
    ],
)
def test_positions_quantity_qs_exclude_reserve(exclude_reserve, expected_results):
    department = DepartmentFactory()
    BudgetPositionAssignmentFactory(
        department=department,
        status=BudgetPositionAssignmentStatus.RESERVE.value,
        budget_position=BudgetPositionFactory(headcount=-1)
    )
    BudgetPositionAssignmentFactory(department=department, status=BudgetPositionAssignmentStatus.RESERVE.value)
    BudgetPositionAssignmentFactory(department=department)

    target = BudgetPositionAssignmentFilterContext(exclude_reserve=exclude_reserve)

    result = target.positions_quantity_qs('department')

    assert list(result) == [{'department': department.id, 'qty': expected_results}]


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'field',
    [
        'login',
        'first_name',
        'last_name',
        'first_name_en',
        'last_name_en',
    ],
)
def test_positions_objects_qs_filter_by_search_text_in_person_fields(field):
    generated_text = _get_random_searchable_field_value()
    correct_status = BudgetPositionAssignmentStatus.OCCUPIED
    person = StaffFactory(**{field: generated_text})
    assignment = BudgetPositionAssignmentFactory(person=person, status=correct_status.value)
    BudgetPositionAssignmentFactory()

    for status in _get_choices_excluding(BudgetPositionAssignmentStatus, [correct_status]):
        BudgetPositionAssignmentFactory(person=person, status=status)

    target = BudgetPositionAssignmentFilterContext(search_text=generated_text[2:])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'field',
    [
        'candidate_first_name',
        'candidate_last_name',
    ],
)
def test_positions_objects_qs_filter_by_search_text_in_offer_fields(field):
    generated_text = _get_random_searchable_field_value()
    correct_status = BudgetPositionAssignmentStatus.OFFER
    vacancy = VacancyFactory(**{field: generated_text, 'is_active': True})
    assignment = BudgetPositionAssignmentFactory(budget_position=vacancy.budget_position, status=correct_status.value)
    BudgetPositionAssignmentFactory()

    for status in _get_choices_excluding(BudgetPositionAssignmentStatus, [correct_status]):
        wrong_vacancy = VacancyFactory(**{field: generated_text, 'is_active': True})
        BudgetPositionAssignmentFactory(budget_position=wrong_vacancy.budget_position, status=status)

    target = BudgetPositionAssignmentFilterContext(search_text=generated_text[2:])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
def test_positions_objects_qs_filter_by_search_text_in_vacancy_name():
    generated_text = _get_random_searchable_field_value()
    correct_status = BudgetPositionAssignmentStatus.VACANCY_OPEN
    vacancy = VacancyFactory(name=generated_text, is_active=True)
    assignment = BudgetPositionAssignmentFactory(budget_position=vacancy.budget_position, status=correct_status.value)
    BudgetPositionAssignmentFactory()

    for status in _get_choices_excluding(BudgetPositionAssignmentStatus, [correct_status]):
        wrong_vacancy = VacancyFactory(name=generated_text, is_active=True)
        BudgetPositionAssignmentFactory(budget_position=wrong_vacancy.budget_position, status=status)

    target = BudgetPositionAssignmentFilterContext(search_text=generated_text[2:])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'correct_status',
    [
        BudgetPositionAssignmentStatus.VACANCY_OPEN,
        BudgetPositionAssignmentStatus.OFFER,
    ],
)
def test_positions_objects_qs_filter_by_search_text_in_ticket(correct_status):
    generated_text = _get_random_searchable_field_value()
    correct_statues = [BudgetPositionAssignmentStatus.VACANCY_OPEN, BudgetPositionAssignmentStatus.OFFER]
    vacancy = VacancyFactory(ticket=generated_text, is_active=True)
    assignment = BudgetPositionAssignmentFactory(budget_position=vacancy.budget_position, status=correct_status.value)
    BudgetPositionAssignmentFactory()

    for status in _get_choices_excluding(BudgetPositionAssignmentStatus, correct_statues):
        wrong_vacancy = VacancyFactory(ticket=generated_text, is_active=True)
        BudgetPositionAssignmentFactory(budget_position=wrong_vacancy.budget_position, status=status)

    target = BudgetPositionAssignmentFilterContext(search_text=generated_text[2:])

    result = target.positions_objects_qs()

    assert result.get().pk == assignment.pk


def _get_choices_excluding(enum, exclude_choices):
    exclude_values = [choice.value for choice in exclude_choices]
    return (choice[0] for choice in enum.choices() if choice[0] not in exclude_values)


def _get_random_searchable_field_value():
    return f'field-{random()}'
