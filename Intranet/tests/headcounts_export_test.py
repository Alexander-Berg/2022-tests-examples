import random

import pytest

from staff.lib.testing import StaffFactory, BudgetPositionFactory

from staff.budget_position.models import BudgetPositionAssignmentStatus, ReplacementType
from staff.budget_position.tests.utils import RewardFactory, BudgetPositionAssignmentFactory
from staff.departments.tests.factories import VacancyFactory

from staff.headcounts.tests.headcounts_test import person_with_perm
from staff.headcounts.headcounts_export import FlexiblePositionsSheetPresenter
from staff.headcounts.views.headcounts_export_views import (
    _fill_positions_chains,
    get_positions_with_department_info,
    BudgetPositionAssignmentEntityInfo,
    BudgetPositionAssignmentFilterContext,
    Permissions,
    TallPager,
)


def get_departments(company, url):
    viewer_person = person_with_perm(
        company.persons['{}-chief'.format(url.split('_')[-1])],
        codename='can_view_headcounts',
    )
    permissions = Permissions(viewer_person)
    filter_context = BudgetPositionAssignmentFilterContext(observer_permissions=permissions)
    filler = BudgetPositionAssignmentEntityInfo(filter_context)
    pager = TallPager(filler, url)
    departments, _ = pager.get_grouped_entities()
    return departments


@pytest.mark.django_db()
def test_export_positions(company_with_module_scope):
    company = company_with_module_scope
    mass_reward = RewardFactory(category='Mass position bp_reward.category')

    occupied_position = BudgetPositionFactory()
    person = StaffFactory(login='current_login')

    vacancy_assignment = BudgetPositionAssignmentFactory(
        budget_position=occupied_position,
        department=company.dep1,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        replacement_type=ReplacementType.HAS_REPLACEMENT.value,
        value_stream=company.vs_root,
    )
    VacancyFactory(
        headcount_position_code=occupied_position.code,
        name='vacanciya',
        ticket='JOB-123',
    )

    occupied_assignment = BudgetPositionAssignmentFactory(
        budget_position=occupied_position,
        department=company.dep11,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=person,
        replacement_type=ReplacementType.HAS_REPLACEMENT_AND_BUSY.value,
        previous_assignment=vacancy_assignment,
        value_stream=company.vs_root,
    )
    position_name = f'position_name{random.randint(1, 433)}'
    plan_assignment = BudgetPositionAssignmentFactory(
        department=company.dep11,
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        value_stream=company.vs_root,
        name=position_name,   # TODO: Remove
        replacement_type=ReplacementType.WITHOUT_REPLACEMENT.value,
    )

    offer_assignment = BudgetPositionAssignmentFactory(
        budget_position=occupied_position,
        department=company.dep111,
        status=BudgetPositionAssignmentStatus.OFFER.value,
        replacement_type=ReplacementType.BUSY.value,
        reward=mass_reward,
        previous_assignment=occupied_assignment,
        value_stream=company.vs_root,
    )
    VacancyFactory(
        headcount_position_code=occupied_position.code,
        candidate_first_name='fname',
        candidate_last_name='lname',
        candidate_id=1234321,
        ticket='JOB-321',
        is_active=True,
    )

    departments = get_departments(company, company.dep11.url)
    departments = [department for department in get_positions_with_department_info(departments)]
    del departments[0]
    departments = [dep for dep in _fill_positions_chains(departments)]
    sheet = FlexiblePositionsSheetPresenter(departments)
    rows = [[cell.text for cell in row.cells] for row in sheet.rows()]

    assert rows[0] == [
        'Подразделение',
        'Иерархия подразделения',
        'Основной продукт\n (Value Stream)',
        'Иерархия основного продукта\n (Value Stream)',
        'География',
        'Категория',
        'Статус позиции',
        'Детали позиции',
        'ФИО',
        'Фемида',
        'Кому замена',
        'ФИО',
        'Численность',
        'Тип пересечения',
        'Есть замена',
        'ФИО',
        'ХК еще занят',
        'ФИО',
        'Номер БП',
        'Подразделение удалено',
        'ID подразделения',
    ]

    assert rows[1] == [
        'dep11',
        'Яндекс → Главный бизнес-юнит → dep11',
        plan_assignment.value_stream.name,
        plan_assignment.value_stream.name,
        departments[0]['geography']['name'],
        '',
        'Незанятый хедкаунт',
        position_name,
        '',
        '',
        '',
        '',
        1,
        'Нет пересечений',
        '',
        '',
        '',
        '',
        plan_assignment.budget_position.code,
        'Нет',
        company.dep11.id,
    ]

    assert rows[2] == [
        'dep111',
        'Яндекс → Главный бизнес-юнит → dep11 → dep111',
        offer_assignment.value_stream.name,
        offer_assignment.value_stream.name,
        departments[1]['geography']['name'],
        'Mass position bp_reward.category',
        'Оффер',
        'JOB-321',
        '',
        'fname lname',
        person.login,
        f'{person.first_name} {person.last_name}',
        1,
        'ХК еще занят',
        '',
        '',
        person.login,
        f'{person.first_name} {person.last_name}',
        offer_assignment.budget_position.code,
        'Нет',
        company.dep111.id,
    ]
