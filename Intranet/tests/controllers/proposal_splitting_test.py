import pytest
from assertpy import assert_that
from mock import Mock, patch

from staff.budget_position.models.workflow import WORKFLOW_STATUS
from staff.budget_position.workflow_service.gateways.workflow_repository import WorkflowRepository
from staff.departments.controllers.proposal import ProposalCtl
from staff.departments.controllers.proposal_execution import ProposalExecution
from staff.departments.models import Department, ProposalMetadata

from staff.proposal.controllers.proposal_splitting import ProposalSplitting, ProposalSplittingData
from staff.proposal.proposal_builder import ProposalBuilder

from staff.lib.tests.startrek_issue import StartrekIssue


@pytest.mark.django_db
@patch('staff.lib.startrek.issues.startrek_issues_repository')
def test_persons_to_split_checks_ticket_approval_status(mocked_st, company, mocked_mongo):
    proposal_id = (
        ProposalBuilder()
        .with_person(
            company.persons['dep11-person'].login,
            lambda person: person.staff_position('some').with_ticket('T-1'),
        )
        .with_person(
            company.persons['dep11-chief'].login,
            lambda person: person.staff_position('some2').with_ticket('T-2'),
        )
        .build(author_login='yandex-chief')
    )

    mocked_st.get = Mock(return_value=[
        StartrekIssue({
            'key': 'T-1',
            ProposalSplitting.approval_field: ProposalSplitting.approval_field_value,
        }),
        StartrekIssue({
            'key': 'T-2',
            ProposalSplitting.approval_field: 'some other state',
        }),
    ])

    proposal_splitting = ProposalSplitting(ProposalCtl(proposal_id=proposal_id))
    splitting_data = proposal_splitting.splitting_data()

    assert_that(splitting_data.persons).is_equal_to(['dep11-chief'])


@pytest.mark.django_db
@patch('staff.lib.startrek.issues.startrek_issues_repository')
def test_persons_to_split_checks_personal_ticket_absence(mocked_st, company, mocked_mongo):
    proposal_id = (
        ProposalBuilder()
        .with_person(
            company.persons['dep11-person'].login,
            lambda person: person.staff_position('some').with_ticket('T-1'),
        )
        .with_person(
            company.persons['dep11-chief'].login,
            lambda person: person.staff_position('some2'),
        )
        .build(author_login='yandex-chief')
    )

    mocked_st.get = Mock(return_value=[
        StartrekIssue({
            'key': 'T-1',
            ProposalSplitting.approval_field: ProposalSplitting.approval_field_value,
        }),
    ])

    proposal_splitting = ProposalSplitting(ProposalCtl(proposal_id=proposal_id))
    splitting_data = proposal_splitting.splitting_data()

    assert_that(splitting_data.persons).is_empty()


@pytest.mark.django_db
@patch('staff.lib.startrek.issues.startrek_issues_repository')
def test_persons_to_split_checks_department_deletion(mocked_st, company, mocked_mongo):
    proposal_id = (
        ProposalBuilder()
        .delete_department(company.dep111.url)
        .delete_department(company.dep11.url)
        .with_person(
            company.persons['dep111-person'].login,
            lambda person: person.new_salary('100500', '1050').with_ticket('dep-111'),
        )
        .with_person(
            company.persons['dep11-person'].login,
            lambda person: person.new_salary('100500', '1050').with_ticket('dep-11'),
        )
        .build(author_login='yandex-chief')
    )

    mocked_st.get = Mock(return_value=[
        StartrekIssue({
            'key': 'dep-111',
            ProposalSplitting.approval_field: ProposalSplitting.approval_field_value,
        }),
        StartrekIssue({
            'key': 'dep-11',
            ProposalSplitting.approval_field: 'false',
        }),
    ])

    proposal_splitting = ProposalSplitting(ProposalCtl(proposal_id=proposal_id))
    splitting_data = proposal_splitting.splitting_data()

    assert splitting_data.persons == ['dep11-person']
    assert splitting_data.departments == [company.dep11.id]

    mocked_st.get = Mock(return_value=[
        StartrekIssue({
            'key': 'dep-111',
            ProposalSplitting.approval_field: 'false',
        }),
        StartrekIssue({
            'key': 'dep-11',
            ProposalSplitting.approval_field: ProposalSplitting.approval_field_value,
        }),
    ])

    proposal_splitting = ProposalSplitting(ProposalCtl(proposal_id=proposal_id))
    splitting_data = proposal_splitting.splitting_data()

    assert splitting_data.persons == ['dep111-person']
    assert_that(set(splitting_data.departments)).is_equal_to({company.dep11.id, company.dep111.id})


@pytest.mark.django_db
def test_split_actions_for_new_proposal(mocked_mongo, company):
    proposal_id = (
        ProposalBuilder()
        .for_new_department(
            'some_name',
            lambda department: (
                department.set_parent(company.dep1.url)
                .use_for_person(
                    company.persons['dep11-person'].login,
                    lambda person: person.staff_position('some').with_ticket('T-1'),
                )
                .use_for_person(
                    company.persons['dep11-chief'].login,
                    lambda person: person.staff_position('some2').with_ticket('T-2'),
                )
            )
        )
        .build(author_login='yandex-chief')
    )

    splitting_data = ProposalSplittingData(persons=[company.persons['dep11-person'].login])

    proposal_ctl = ProposalCtl(proposal_id=proposal_id)
    proposal_ctl.split_actions_for_new_proposal(splitting_data)

    proposal_ctl = ProposalCtl(proposal_id=proposal_id)
    persons = proposal_ctl.proposal_object['persons']
    tickets = proposal_ctl.proposal_object['tickets']

    assert len(persons['splitted_actions']) == 1
    assert persons['splitted_actions'][0]['login'] == company.persons['dep11-person'].login
    assert len(tickets['splitted_persons']) == 1
    assert tickets['splitted_persons'][company.persons['dep11-person'].login] == 'T-1'

    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_id).id
    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, WORKFLOW_STATUS.PENDING)
    assert len(workflows) == 1


@pytest.mark.django_db
def test_proposal_creation_with_splitted_actions(mocked_mongo, company):
    proposal_id = (
        ProposalBuilder()
        .for_new_department(
            'some_name',
            lambda department: (
                department.set_parent(company.dep1.url)
                .use_for_person(
                    company.persons['dep11-person'].login,
                    lambda person: person.staff_position('some').with_ticket('T-1'),
                )
                .use_for_person(
                    company.persons['dep11-chief'].login,
                    lambda person: person.staff_position('some2').with_ticket('T-2'),
                )
            )
        )
        .build(author_login='yandex-chief')
    )

    splitting_data = ProposalSplittingData(persons=[company.persons['dep11-person'].login])

    proposal_ctl = ProposalCtl(proposal_id=proposal_id)
    proposal_ctl.split_actions_for_new_proposal(splitting_data)
    ProposalExecution(proposal_ctl).execute()

    assert proposal_ctl.splitted_to is not None

    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_id).id
    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, WORKFLOW_STATUS.CONFIRMED)
    assert len(workflows) == 1

    new_proposal_id = proposal_ctl.splitted_to
    new_proposal_ctl = ProposalCtl(proposal_id=new_proposal_id)
    persons = new_proposal_ctl.proposal_object['persons']
    tickets = new_proposal_ctl.proposal_object['tickets']

    assert new_proposal_ctl.author == proposal_ctl.author
    assert len(persons['actions']) == 1
    assert persons['actions'][0]['login'] == splitting_data.persons[0]
    assert persons['actions'][0]['department']['fake_department'] == ''
    assert persons['actions'][0]['department']['department'] == Department.objects.get(name='some_name').url
    assert len(tickets['persons']) == 1
    assert tickets['persons'][splitting_data.persons[0]] == 'T-1'

    postgres_id = ProposalMetadata.objects.get(proposal_id=new_proposal_id)
    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, WORKFLOW_STATUS.PENDING)
    assert len(workflows) == 1
