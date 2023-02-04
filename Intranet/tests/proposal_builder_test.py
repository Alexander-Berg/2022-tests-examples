import pytest

from staff.departments.controllers.proposal import ProposalCtl
from staff.departments.controllers.proposal_execution import ProposalExecution
from staff.departments.models import DepartmentStaff, DepartmentRoles

from staff.proposal.proposal_builder import ProposalBuilder


@pytest.mark.django_db
def test_proposal_builder_can_deprive_crown(company, mocked_mongo):
    # given
    chief = company.persons['dep11-chief']
    department = company.dep11
    deputies = list(
        DepartmentStaff.objects
        .filter(department__url=department.url, role_id=DepartmentRoles.DEPUTY.value)
        .values_list('staff_id', flat=True)
    )

    proposal_id = (
        ProposalBuilder()
        .for_existing_department(department.url, lambda dep_config: dep_config.change_roles(None, deputies))
        .build(company.persons['yandex-chief'].login)
    )
    proposal_ctl = ProposalCtl(proposal_id=proposal_id)

    # when
    ProposalExecution(proposal_ctl).execute()

    # then
    assert deputies == list(
        DepartmentStaff.objects
        .filter(department__url=department.url, role_id=DepartmentRoles.DEPUTY.value)
        .values_list('staff_id', flat=True)
    )

    has_chief = (
        DepartmentStaff.objects
        .filter(department__url=department.url, staff_id=chief, role_id=DepartmentRoles.CHIEF.value)
        .exists()
    )
    assert not has_chief


@pytest.mark.django_db
def test_proposal_builder_saves_department_linked_ticket(company, mocked_mongo):
    # given
    department = company.dep11
    some_test_ticket = 'TSALARY-111'

    # when
    proposal_id = (
        ProposalBuilder()
        .for_existing_department(department.url, lambda dep_config: dep_config.set_name('test'))
        .with_linked_ticket(some_test_ticket)
        .build(company.persons['yandex-chief'].login)
    )

    # then
    proposal_document = ProposalCtl(proposal_id=proposal_id).proposal_object
    assert proposal_document['tickets']['department_linked_ticket'] == some_test_ticket
