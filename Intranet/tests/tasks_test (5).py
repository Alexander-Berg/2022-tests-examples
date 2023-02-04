from mock import patch
import pytest

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.models import Workflow
from staff.departments.models import ProposalMetadata
from staff.lib.testing import StaffFactory, OrganizationFactory

from staff.proposal.tasks import (
    notify_person_tickets,
    ProposalContext,
    ProposalCtl,
    PersonTicket,
    create_persons_tickets,
    delete_proposal_task,
)
from staff.proposal.proposal_builder import ProposalBuilder


@pytest.fixture
def startrek_created_tickets(monkeypatch):
    tickets_commented = []

    def add_comment(self, ticket, *args, **kwargs):
        tickets_commented.append(self.ticket_key)
    monkeypatch.setattr(PersonTicket, 'add_comment_if_has_ticket', add_comment)

    return tickets_commented


def test_execute_proposal_with_executable_action(startrek_created_tickets, mocked_mongo):
    login = StaffFactory().login
    ticket = 'KEY-1234'

    proposal_id = (
        ProposalBuilder()
        .with_person(
            login,
            lambda person: person.organization(OrganizationFactory().id).with_ticket(ticket),
        )
        .build(StaffFactory(login='author-user').login)
    )

    ctl = ProposalCtl(proposal_id)
    notify_person_tickets(ProposalContext(ctl))
    assert startrek_created_tickets == [ticket]


def test_execute_proposal_without_executable_action(startrek_created_tickets, mocked_mongo):
    login = StaffFactory().login
    ticket = 'KEY-1234'

    proposal_id = (
        ProposalBuilder()
        .with_person(
            login,
            lambda person: person.new_salary(None, None).with_ticket(ticket),
        )
        .build(StaffFactory(login='author-user').login)
    )

    ctl = ProposalCtl(proposal_id)

    notify_person_tickets(ProposalContext(ctl))
    assert startrek_created_tickets == []


@patch('staff.departments.controllers.tickets.person.PersonTicket.create_ticket', side_effect=Exception)
def test_create_personal_tickets_doesnt_swallow_exceptions_silently(mocked_method, mocked_mongo):
    # given
    login = StaffFactory().login

    proposal_id = (
        ProposalBuilder()
        .with_person(login, lambda person: person.new_salary(None, None))
        .build(StaffFactory(login='author-user').login)
    )

    # when
    with pytest.raises(Exception):
        create_persons_tickets(proposal_id=proposal_id)


@pytest.mark.django_db
def test_delete_proposal_task(startrek_created_tickets, mocked_mongo):
    login = StaffFactory().login
    ticket = 'KEY-1234'

    proposal_id = (
        ProposalBuilder()
        .with_person(
            login,
            lambda person: person.organization(OrganizationFactory().id).with_ticket(ticket),
        )
        .build(StaffFactory(login='author-user').login)
    )

    ctl = ProposalCtl(proposal_id)
    ctl.update_tickets(restructurisation_ticket=ticket)
    ctl.save()
    assert _alive_workflows_exist(proposal_id), 'no alive workflows after proposal creation'

    delete_proposal_task(ProposalMetadata.objects.get(proposal_id=proposal_id).id, login)

    assert ProposalCtl(proposal_id).is_deleted
    assert not _alive_workflows_exist(proposal_id), 'alive workflows after proposal deletion'


def _alive_workflows_exist(proposal_id):
    return (
        Workflow
        .objects
        .filter(proposal__proposal_id=proposal_id)
        .exclude(status=WORKFLOW_STATUS.CANCELLED)
        .exists()
    )
