import pytest

from staff.budget_position.workflow_service.gateways.workflow_repository import WorkflowRepository
from staff.departments.controllers.proposal import ProposalCtl
from staff.departments.controllers.proposal_execution import ProposalExecution
from staff.departments.models import Department, ProposalMetadata

from staff.proposal.proposal_builder import ProposalBuilder


def with_existing_department(company):
    return (
        ProposalBuilder()
        .for_existing_department(
            'yandex_dep1_dep11_dep111',
            lambda department: department.use_for_vacancy(
                company.vacancies['dep11-vac'].id, lambda vacancy: vacancy.with_ticket('T-1'),
            )
        )
    )


def with_new_depatrment(company):
    return (
        ProposalBuilder()
        .for_new_department(
            'some_name',
            lambda department: (
                department.set_parent(company.dep1.url)
                .use_for_vacancy(
                    company.vacancies['dep11-vac'].id,
                    lambda vacancy: vacancy.with_ticket('T-1'),
                )
            )
        )
    )


@pytest.mark.django_db
@pytest.mark.parametrize('proposal_builder, is_new_department', (
    (with_new_depatrment, True),
    (with_existing_department, False),
))
def test_vacancy_proposal_execution(company, mocked_mongo, proposal_builder, is_new_department):
    proposal_id = proposal_builder(company).build(author_login='yandex-chief')
    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_id).id

    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id)
    assert len(workflows) == 1

    proposal_ctl = ProposalCtl(proposal_id=proposal_id)
    ProposalExecution(proposal_ctl).execute()

    vacancies = proposal_ctl.proposal_object['vacancies']
    tickets = proposal_ctl.proposal_object['tickets']

    assert proposal_ctl.author.login == company.persons['yandex-chief'].login

    assert len(vacancies['actions']) == 1
    assert vacancies['actions'][0]['vacancy_id'] == company.vacancies['dep11-vac'].id

    if is_new_department:
        assert vacancies['actions'][0]['department'] == Department.objects.get(name='some_name').url
        assert vacancies['actions'][0]['fake_department'] == Department.objects.get(name='some_name').code
    else:
        assert vacancies['actions'][0]['department'] == company['dep111'].url
        assert vacancies['actions'][0]['fake_department'] == ''

    assert len(tickets['vacancies']) == 1
    assert tickets['vacancies'][company.vacancies['dep11-vac'].id] == 'T-1'
