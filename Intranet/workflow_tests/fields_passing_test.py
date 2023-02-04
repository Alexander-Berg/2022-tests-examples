from datetime import date
from decimal import Decimal

import pytest
from mock import MagicMock

from staff.lib.testing import (
    DepartmentFactory,
    GeographyFactory,
    OfficeFactory,
    OrganizationFactory,
    PlacementFactory,
    StaffFactory,
)
from staff.oebs.models import HRProduct

from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.budget_position.tests.workflow_tests.utils import OEBSServiceMock
from staff.budget_position.workflow_service import (
    FemidaData,
    ProposalChange,
    ProposalData,
    entities,
    gateways,
)
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.payment.enums import WAGE_SYSTEM


@pytest.mark.django_db
def test_passing_fields_from_femida(company):
    # given
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        department=company.yandex,
    )
    geo = GeographyFactory()
    hr_product = HRProduct.objects.create(product_id=111)
    office = OfficeFactory()
    department = DepartmentFactory()
    org = OrganizationFactory()
    placement = PlacementFactory(organization=org, office=office)
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        office_id=office.id,
        geography_url=geo.department_instance.url,
        department_id=department.id,
        hr_product_id=hr_product.product_id,
        organization_id=org.id,
        ticket='TESTTICKET-1',
        dismissal_date='2019-12-15',
        rate=Decimal(0.75),
        salary='11223355',
        currency='RUB',
        is_vacancy=True,
    )
    workflow_factory = entities.FemidaWorkflowFactory(
        gateways.StaffService(),
        MagicMock(spec=entities.OEBSService),
        gateways.BudgetPositionsRepository(),
    )

    # when
    workflow = workflow_factory.create(data)

    # then
    assert workflow.changes[0].budget_position.code == assignment.budget_position.code
    assert workflow.changes[0].office_id == office.id
    assert workflow.changes[0].geography_url == geo.department_instance.url
    assert workflow.changes[0].department_id == department.id
    assert workflow.changes[0].hr_product_id == hr_product.product_id
    assert workflow.changes[0].organization_id == org.id
    assert workflow.changes[0].ticket == 'TESTTICKET-1'
    assert workflow.changes[0].dismissal_date == date(year=2019, month=12, day=15)
    assert workflow.changes[0].rate == Decimal(0.75)
    assert workflow.changes[0].salary == Decimal(11223355)
    assert workflow.changes[0].currency == 'RUB'
    assert workflow.changes[0].placement_id == placement.id


@pytest.mark.django_db
@pytest.mark.parametrize(
    'wage_system, pay_system',
    [
        (WAGE_SYSTEM.PIECEWORK, 'XXYA_JOBPRICE'),
        (WAGE_SYSTEM.HOURLY, 'XXYA_FIXED_SALARY'),
        (WAGE_SYSTEM.FIXED, 'XXYA_FIXED_SALARY'),
        (None, None),
    ],
)
def test_passing_fields_from_proposal(company, wage_system, pay_system):
    # given
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        department=company.yandex,
    )
    person = StaffFactory()
    department = DepartmentFactory()
    data = ProposalData(
        proposal_id=1,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=person.id,
        proposal_changes=[ProposalChange(
            department_id=department.id,
            occupation_id='BackendDeveloper',
            wage_system=wage_system,
        )]
    )
    staff_service = gateways.StaffService()
    oebs_mock = OEBSServiceMock()
    proposal_workflow_factory = entities.ProposalWorkflowFactory(staff_service, oebs_mock)
    # when
    workflow = proposal_workflow_factory.create_workflow(
        data,
        entities.BudgetPositionMove(assignment.budget_position, assignment.budget_position),
    )

    # then
    assert workflow.changes[0].budget_position.code == assignment.budget_position.code
    assert workflow.changes[0].ticket == 'TJOB-1'
    assert workflow.changes[0].person_id == person.id
    assert workflow.changes[0].department_id == department.id
    assert workflow.changes[0].wage_system == wage_system
    assert workflow.changes[0].pay_system == pay_system
