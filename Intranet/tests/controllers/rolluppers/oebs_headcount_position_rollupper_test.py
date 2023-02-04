import pytest
from mock import patch

from random import random, randint, choice
from typing import Optional, List

from django.db import transaction

from staff.budget_position.models import BudgetPositionAssignment, BudgetPositionAssignmentStatus, Reward
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory, RewardFactory
from staff.departments.models import Department, Geography, HRProduct
from staff.departments.tests.factories import HRProductFactory
from staff.lib.testing import (
    BudgetPositionFactory,
    DepartmentFactory,
    GeographyFactory,
    StaffFactory,
    ValueStreamFactory,
    get_random_date,
)
from staff.person.models import Staff

from staff.oebs.controllers.rolluppers import OebsHeadcountPositionRollupper
from staff.oebs.models import OebsHeadcountPosition
from staff.oebs.tests.factories import RewardFactory as OebsRewardFactory


@pytest.mark.django_db
def test_run_rollup_linking_brand_new_assignment():
    bp_code = randint(10, 9986)
    person = StaffFactory()
    department = DepartmentFactory()
    hr_product = HRProductFactory(value_stream=ValueStreamFactory())
    geography = GeographyFactory()
    reward = RewardFactory()
    assignment_index = 1
    _create_oebs_instance(assignment_index, None, bp_code, department, geography, hr_product, person, reward)
    target = OebsHeadcountPositionRollupper(create_absent=True)

    target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 1
    dis_assignment = dis_assignments[0]
    assert dis_assignment.person_id == person.id
    assert dis_assignment.department_id == department.id
    assert dis_assignment.value_stream_id == hr_product.value_stream_id
    assert dis_assignment.geography_id == geography.department_instance_id
    assert dis_assignment.reward_id == reward.id


@pytest.mark.django_db
def test_run_rollup_linking_occupied_to_unoccupied_existing_assignment():
    bp_code = randint(10, 9986)
    person = StaffFactory()
    department = DepartmentFactory()
    hr_product = HRProductFactory(value_stream=ValueStreamFactory())
    geography = GeographyFactory()
    reward = RewardFactory()
    assignment_index = 1
    _create_oebs_instance(assignment_index, None, bp_code, department, geography, hr_product, person, reward)
    BudgetPositionAssignmentFactory(
        budget_position=BudgetPositionFactory(code=bp_code),
        person=None,
    )
    target = OebsHeadcountPositionRollupper(create_absent=True)

    target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 1
    dis_assignment = dis_assignments[0]
    assert dis_assignment.person_id == person.id
    assert dis_assignment.department_id == department.id
    assert dis_assignment.value_stream_id == hr_product.value_stream_id
    assert dis_assignment.geography_id == geography.department_instance_id
    assert dis_assignment.reward_id == reward.id


@pytest.mark.django_db
def test_run_rollup_linking_unoccupied_to_unoccupied_existing_assignment():
    bp_code = randint(10, 9986)
    department = DepartmentFactory()
    hr_product = HRProductFactory(value_stream=ValueStreamFactory())
    geography = GeographyFactory()
    reward = RewardFactory()
    assignment_index = 1
    _create_oebs_instance(assignment_index, None, bp_code, department, geography, hr_product, None, reward)
    BudgetPositionAssignmentFactory(
        budget_position=BudgetPositionFactory(code=bp_code),
        person=None,
    )
    target = OebsHeadcountPositionRollupper(create_absent=True)

    target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 1
    dis_assignment = dis_assignments[0]
    assert dis_assignment.person_id is None
    assert dis_assignment.department_id == department.id
    assert dis_assignment.value_stream_id == hr_product.value_stream_id
    assert dis_assignment.geography_id == geography.department_instance_id
    assert dis_assignment.reward_id == reward.id


@pytest.mark.django_db
def test_run_rollup_linking_new_assignment_with_existing_assignments():
    bp_code = randint(10, 9986)
    person = StaffFactory()
    department = DepartmentFactory()
    hr_product = HRProductFactory(value_stream=ValueStreamFactory())
    geography = GeographyFactory()
    reward = RewardFactory()
    assignment_index = 1
    _create_oebs_instance(assignment_index, None, bp_code, department, geography, hr_product, person, reward)
    existing_assignment = BudgetPositionAssignmentFactory(
        budget_position=BudgetPositionFactory(code=bp_code),
        person=StaffFactory(),
    )
    target = OebsHeadcountPositionRollupper(create_absent=True)

    target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 2
    _assert_links(dis_assignments)
    assert dis_assignments[0].person_id == existing_assignment.person_id
    assert dis_assignments[0].department_id == existing_assignment.department_id
    assert dis_assignments[0].value_stream_id == existing_assignment.value_stream_id
    assert dis_assignments[0].geography_id == existing_assignment.geography_id
    assert dis_assignments[0].reward_id == existing_assignment.reward_id
    assert dis_assignments[1].person_id == person.id
    assert dis_assignments[1].department_id == department.id
    assert dis_assignments[1].value_stream_id == hr_product.value_stream_id
    assert dis_assignments[1].geography_id == geography.department_instance_id
    assert dis_assignments[1].reward_id == reward.id


@pytest.mark.django_db
def test_run_rollup_linking_new_assignment_and_vacancy_plan_to_existing_linked_vacancy_plan_assignment():
    """
    OEBS returns a person for an existing assignment and a vacancy for his replacement
    """
    bp_code = randint(10, 9986)
    person1 = StaffFactory()
    department1 = DepartmentFactory()
    hr_product1 = HRProductFactory(value_stream=ValueStreamFactory())
    geography1 = GeographyFactory()
    reward1 = RewardFactory()
    department2 = DepartmentFactory()
    hr_product2 = HRProductFactory(value_stream=ValueStreamFactory())
    geography2 = GeographyFactory()
    reward2 = RewardFactory()
    existing_assignment = BudgetPositionAssignmentFactory(
        budget_position=BudgetPositionFactory(code=bp_code),
        person=None,
    )
    _create_oebs_instance(1, None, bp_code, department1, geography1, hr_product1, person1, reward1)
    _create_oebs_instance(2, 1, bp_code, department2, geography2, hr_product2, None, reward2, existing_assignment)
    target = OebsHeadcountPositionRollupper(create_absent=True)

    target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 2
    _assert_links(dis_assignments)
    assert dis_assignments[0].person_id == person1.id
    assert dis_assignments[0].department_id == department1.id
    assert dis_assignments[0].value_stream_id == hr_product1.value_stream_id
    assert dis_assignments[0].geography_id == geography1.department_instance_id
    assert dis_assignments[0].reward_id == reward1.id
    assert dis_assignments[1].person_id is None
    assert dis_assignments[1].department_id == department2.id
    assert dis_assignments[1].value_stream_id == hr_product2.value_stream_id
    assert dis_assignments[1].geography_id == geography2.department_instance_id
    assert dis_assignments[1].reward_id == reward2.id


@pytest.mark.django_db
def test_run_rollup_zombie_assignment():
    bp_code = randint(10, 9986)
    person1 = StaffFactory()
    department1 = DepartmentFactory()
    hr_product1 = HRProductFactory(value_stream=ValueStreamFactory())
    geography1 = GeographyFactory()
    reward1 = RewardFactory()
    person2 = StaffFactory()
    department2 = DepartmentFactory()
    hr_product2 = HRProductFactory(value_stream=ValueStreamFactory())
    geography2 = GeographyFactory()
    reward2 = RewardFactory()
    existing_assignment = BudgetPositionAssignmentFactory(
        budget_position=BudgetPositionFactory(code=bp_code),
        person=None,
    )
    _create_oebs_instance(1, None, bp_code, department1, geography1, hr_product1, person1, reward1)
    _create_oebs_instance(2, 1, bp_code, department2, geography2, hr_product2, person2, reward2, existing_assignment)
    target = OebsHeadcountPositionRollupper(create_absent=True)

    with patch('staff.lib.sync_tools.rollupper.atomic', transaction.atomic):
        target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 1
    _assert_links(dis_assignments)
    assert dis_assignments[0].person_id == person2.id
    assert dis_assignments[0].department_id == department2.id
    assert dis_assignments[0].value_stream_id == hr_product2.value_stream_id
    assert dis_assignments[0].geography_id == geography2.department_instance_id
    assert dis_assignments[0].reward_id == reward2.id
    assert OebsHeadcountPosition.objects.get(code=bp_code, assignment_index=1).has_errors is True, 'Should be marked'


@pytest.mark.django_db
def test_run_rollup_linking_new_assignment_by_previous_links():
    bp_code = randint(10, 9986)
    person1 = StaffFactory()
    department1 = DepartmentFactory()
    hr_product1 = HRProductFactory(value_stream=ValueStreamFactory())
    geography1 = GeographyFactory()
    reward1 = RewardFactory()
    assignment_index1 = 1
    person2 = StaffFactory()
    department2 = DepartmentFactory()
    hr_product2 = HRProductFactory(value_stream=ValueStreamFactory())
    geography2 = GeographyFactory()
    reward2 = RewardFactory()
    assignment_index2 = 2
    existing = BudgetPositionAssignmentFactory(
        budget_position=BudgetPositionFactory(code=bp_code),
        person=StaffFactory(),
    )
    BudgetPositionAssignmentFactory(
        budget_position=existing.budget_position,
        person=StaffFactory(),
        previous_assignment=existing,
    )
    _create_oebs_instance(
        assignment_index1,
        None,
        bp_code,
        department1,
        geography1,
        hr_product1,
        person1,
        reward1,
        existing,
    )
    _create_oebs_instance(
        assignment_index2,
        assignment_index1,
        bp_code,
        department2,
        geography2,
        hr_product2,
        person2,
        reward2,
    )
    target = OebsHeadcountPositionRollupper(create_absent=True)

    target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 2
    _assert_links(dis_assignments)
    assert dis_assignments[0].person_id == person1.id
    assert dis_assignments[0].department_id == department1.id
    assert dis_assignments[0].value_stream_id == hr_product1.value_stream_id
    assert dis_assignments[0].geography_id == geography1.department_instance_id
    assert dis_assignments[0].reward_id == reward1.id
    assert dis_assignments[1].person_id == person2.id
    assert dis_assignments[1].department_id == department2.id
    assert dis_assignments[1].value_stream_id == hr_product2.value_stream_id
    assert dis_assignments[1].geography_id == geography2.department_instance_id
    assert dis_assignments[1].reward_id == reward2.id


@pytest.mark.django_db
def test_run_rollup_linking_new_assignment_by_chain():
    bp_code = randint(10, 9986)
    person1 = StaffFactory()
    department1 = DepartmentFactory()
    hr_product1 = HRProductFactory(value_stream=ValueStreamFactory())
    geography1 = GeographyFactory()
    reward1 = RewardFactory()
    person2 = StaffFactory()
    department2 = DepartmentFactory()
    hr_product2 = HRProductFactory(value_stream=ValueStreamFactory())
    geography2 = GeographyFactory()
    reward2 = RewardFactory()
    person3 = StaffFactory()
    department3 = DepartmentFactory()
    hr_product3 = HRProductFactory(value_stream=ValueStreamFactory())
    geography3 = GeographyFactory()
    reward3 = RewardFactory()
    person4 = StaffFactory()
    department4 = DepartmentFactory()
    hr_product4 = HRProductFactory(value_stream=ValueStreamFactory())
    geography4 = GeographyFactory()
    reward4 = RewardFactory()
    person5 = StaffFactory()
    department5 = DepartmentFactory()
    hr_product5 = HRProductFactory(value_stream=ValueStreamFactory())
    geography5 = GeographyFactory()
    reward5 = RewardFactory()
    person6 = StaffFactory()
    department6 = DepartmentFactory()
    hr_product6 = HRProductFactory(value_stream=ValueStreamFactory())
    geography6 = GeographyFactory()
    reward6 = RewardFactory()
    existing = BudgetPositionAssignmentFactory(
        budget_position=BudgetPositionFactory(code=bp_code),
        person=StaffFactory(),
    )
    second_existing = BudgetPositionAssignmentFactory(
        budget_position=existing.budget_position,
        person=StaffFactory(),
        previous_assignment=existing,
    )
    BudgetPositionAssignmentFactory(
        budget_position=second_existing.budget_position,
        person=StaffFactory(),
        previous_assignment=second_existing,
    )
    _create_oebs_instance(1, None, bp_code, department1, geography1, hr_product1, person1, reward1, existing)
    _create_oebs_instance(2, 1, bp_code, department2, geography2, hr_product2, person2, reward2)
    _create_oebs_instance(3, 2, bp_code, department3, geography3, hr_product3, person3, reward3)
    _create_oebs_instance(4, 3, bp_code, department4, geography4, hr_product4, person4, reward4)
    _create_oebs_instance(5, 4, bp_code, department5, geography5, hr_product5, person5, reward5)
    _create_oebs_instance(6, 5, bp_code, department6, geography6, hr_product6, person6, reward6)
    target = OebsHeadcountPositionRollupper(create_absent=True)

    target.run_rollup()

    dis_assignments = _get_dis_assignments(bp_code)
    assert len(dis_assignments) == 6
    _assert_links(dis_assignments)
    assert dis_assignments[0].person_id == person1.id
    assert dis_assignments[0].department_id == department1.id
    assert dis_assignments[0].value_stream_id == hr_product1.value_stream_id
    assert dis_assignments[0].geography_id == geography1.department_instance_id
    assert dis_assignments[0].reward_id == reward1.id
    assert dis_assignments[1].person_id == person2.id
    assert dis_assignments[1].department_id == department2.id
    assert dis_assignments[1].value_stream_id == hr_product2.value_stream_id
    assert dis_assignments[1].geography_id == geography2.department_instance_id
    assert dis_assignments[1].reward_id == reward2.id
    assert dis_assignments[2].person_id == person3.id
    assert dis_assignments[2].department_id == department3.id
    assert dis_assignments[2].value_stream_id == hr_product3.value_stream_id
    assert dis_assignments[2].geography_id == geography3.department_instance_id
    assert dis_assignments[2].reward_id == reward3.id
    assert dis_assignments[3].person_id == person4.id
    assert dis_assignments[3].department_id == department4.id
    assert dis_assignments[3].value_stream_id == hr_product4.value_stream_id
    assert dis_assignments[3].geography_id == geography4.department_instance_id
    assert dis_assignments[3].reward_id == reward4.id
    assert dis_assignments[4].person_id == person5.id
    assert dis_assignments[4].department_id == department5.id
    assert dis_assignments[4].value_stream_id == hr_product5.value_stream_id
    assert dis_assignments[4].geography_id == geography5.department_instance_id
    assert dis_assignments[4].reward_id == reward5.id
    assert dis_assignments[5].person_id == person6.id
    assert dis_assignments[5].department_id == department6.id
    assert dis_assignments[5].value_stream_id == hr_product6.value_stream_id
    assert dis_assignments[5].geography_id == geography6.department_instance_id
    assert dis_assignments[5].reward_id == reward6.id


def _get_dis_assignments(bp_code: int) -> List[BudgetPositionAssignment]:
    return list(
        BudgetPositionAssignment.objects
        .filter(budget_position__code=bp_code)
        .order_by('pk')
    )


def _create_oebs_instance(
    assignment_index: int,
    prev_assignment_index: Optional[int],
    bp_code: int,
    department: Department,
    geography: Geography,
    hr_product: HRProduct,
    person: Optional[Staff],
    reward: Reward,
    dis_instance: BudgetPositionAssignment = None
) -> OebsHeadcountPosition:
    assignment_id = randint(1, 1000000) if person else None
    return OebsHeadcountPosition.objects.create(
        id=f'{bp_code}_{assignment_id}',
        code=bp_code,
        current_login=person and person.login,
        department_id=department.id,
        hr_product_id=hr_product.id,
        geography_oebs_code=geography.oebs_code,
        assignment_id=assignment_id,
        name=f'name{random()}',
        status=choice([x.name for x in BudgetPositionAssignmentStatus]),
        state2=f'state2{random()}',
        relevance_date=get_random_date(),
        headcount=randint(-10, -5),
        bonus_id=randint(10, 100),
        reward_id=OebsRewardFactory(dis_instance=reward).pk,
        review_id=randint(200, 300),
        assignment_index=assignment_index,
        prev_assignment_index=prev_assignment_index,
        dis_budget_position_assignment=dis_instance,
    )


def _assert_links(dis_assignments: List[BudgetPositionAssignment]):
    for i in range(len(dis_assignments) - 1):
        assert dis_assignments[i + 1].previous_assignment == dis_assignments[i]
        assert dis_assignments[i].next_assignment.first() == dis_assignments[i + 1]

    if dis_assignments:
        assert dis_assignments[0].previous_assignment is None
        assert dis_assignments[-1].next_assignment.first() is None
