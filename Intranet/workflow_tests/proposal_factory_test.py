from datetime import date, timedelta
from decimal import Decimal
from mock import Mock

import pytest

from staff.budget_position.workflow_service.entities import (
    BudgetPosition,
    BudgetPositionMove,
    Person,
    Placement,
    ProposalWorkflowFactory,
    ProposalChange,
    ProposalData,
    workflows,
)

from staff.budget_position.tests.workflow_tests.utils import OEBSServiceMock, StaffServiceMock


@pytest.mark.django_db
def test_create_workflow_move_without_budget_position():
    # given
    oebs_mock = OEBSServiceMock()
    old_budget_position = BudgetPosition(id=1, code=1)
    new_get_position = BudgetPosition(id=2, code=2)
    factory = ProposalWorkflowFactory(Mock(), oebs_mock)
    proposal_data = ProposalData(
        proposal_id=0,
        is_move_with_budget_position=False,
        person_id=3,
        proposal_changes=[
            ProposalChange(oebs_date=date.today(), office_id=100500),
            ProposalChange(oebs_date=date.today() + timedelta(days=1), salary='100'),
        ]
    )

    # when
    workflow = factory.create_workflow(proposal_data, BudgetPositionMove(old_budget_position, new_get_position))

    # then
    assert workflow.code == workflows.MoveWithoutBudgetPositionWorkflow.code
    assert len(workflow.changes) == 1
    assert workflow.changes[0].salary == Decimal('100')
    assert workflow.changes[0].office_id == 100500


@pytest.mark.django_db
def test_create_workflow_move_without_budget_position_uses_placement_from_proposal_if_provided():
    # given
    person_office_id = 1001
    new_person_office_id = 1002
    person_org_id = 2001
    old_placement = Placement(id=1, office_id=person_office_id, organization_id=person_org_id)
    new_placement = Placement(id=2, office_id=new_person_office_id, organization_id=person_org_id)

    old_budget_position = BudgetPosition(id=1, code=1)
    new_get_position = BudgetPosition(id=2, code=2)
    staff_service = StaffServiceMock()
    person = Person(id=1, login='iliketomoveit', office_id=person_office_id, organization_id=person_org_id)
    staff_service.set_person(person)
    staff_service.set_placement(old_placement)
    staff_service.set_placement(new_placement)
    factory = ProposalWorkflowFactory(staff_service, OEBSServiceMock())
    proposal_data = ProposalData(
        proposal_id=0,
        is_move_with_budget_position=False,
        person_id=person.id,
        proposal_changes=[
            ProposalChange(oebs_date=date.today(), office_id=new_person_office_id),
            ProposalChange(oebs_date=date.today() + timedelta(days=1), salary='100'),
        ]
    )

    # when
    workflow = factory.create_workflow(proposal_data, BudgetPositionMove(old_budget_position, new_get_position))

    # then
    assert workflow.code == workflows.MoveWithoutBudgetPositionWorkflow.code
    assert len(workflow.changes) == 1
    assert workflow.changes[0].salary == Decimal('100')
    assert workflow.changes[0].office_id == new_person_office_id
    assert workflow.changes[0].placement_id == new_placement.id
