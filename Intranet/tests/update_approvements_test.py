import json
import pytest
from mock import Mock

from staff.lib.testing import StaffFactory
from staff.proposal.tasks import (
    approvement_ctl,
    update_approvement,
    ProposalCtl,
    ProposalContext,
    ApprovementStatus,
    ApprovementTicketType,
)
from staff.proposal.proposal_builder import ProposalBuilder


OK_UUID = 'someuuid'


@pytest.fixture
def create_ok_mock(monkeypatch):
    def create(result=None, status_code=200):
        calls = []
        result = result or {'uuid': OK_UUID}

        def call_ok(url, *args, **kwargs):
            calls.append((url, kwargs))
            return Mock(
                json=lambda: result,
                status_code=status_code,
            )
        monkeypatch.setattr(approvement_ctl.ok_session, 'post', call_ok)

        return calls

    return create


def test_create_approvement(create_ok_mock, mocked_mongo):
    login = StaffFactory().login
    ticket = 'KEY-1234'
    new_salary = '100500'
    old_salary = '1050'
    ok_calls = create_ok_mock()

    proposal_id = (
        ProposalBuilder()
        .with_person(
            login,
            lambda person: person.new_salary(new_salary, old_salary).with_ticket(ticket),
        )
        .build(StaffFactory(login='author-user').login)
    )

    ctx = ProposalContext.from_proposal_id(proposal_id)
    update_approvement(ctx, ticket, ApprovementTicketType.PERSONAL, ApprovementStatus.WAIT_CREATE)

    url, kwargs = ok_calls[0]
    assert url.endswith('/api/approvements/')
    data = json.loads(kwargs['data'])
    assert data['flow_context']['person_actions'][0]['salary']['new_salary'] == new_salary, data
    assert data['flow_context']['ticket_type'] == ApprovementTicketType.PERSONAL.value, data

    ctl = ProposalCtl(proposal_id)
    approvement = ctl.get_approvements()[0]
    assert approvement.ticket_key == ticket
    assert approvement.uuid == OK_UUID
    assert approvement.status == ApprovementStatus.OK.value
    assert approvement.ticket_type == ApprovementTicketType.PERSONAL.value


def test_delete_approvement(create_ok_mock, mocked_mongo):
    login = StaffFactory().login
    ticket = 'KEY-1234'
    old_ok_uuid = 'old_ok_uuid'
    ok_calls = create_ok_mock()

    proposal_id = (
        ProposalBuilder()
        .with_person(
            login,
            lambda person: person.new_salary('100500', '1050').with_ticket(ticket),
        )
        .build(StaffFactory(login='author-user').login)
    )

    ctx = ProposalContext.from_proposal_id(proposal_id)
    ctx.proposal.update_approvement(
        ticket_key=ticket,
        ticket_type=ApprovementTicketType.PERSONAL,
        uuid=old_ok_uuid,
        status=ApprovementStatus.OK,
    )
    update_approvement(ctx, ticket, ApprovementTicketType.PERSONAL, ApprovementStatus.WAIT_DELETE)

    del_url = ok_calls[0][0]
    assert del_url.endswith(f'/api/approvements/{old_ok_uuid}/close/')


def test_rerun_approvement(create_ok_mock, mocked_mongo):
    login = StaffFactory().login
    ticket = 'KEY-1234'
    old_ok_uuid = 'old_ok_uuid'
    new_salary = '100500'
    old_salary = '1050'
    ok_calls = create_ok_mock()

    proposal_id = (
        ProposalBuilder()
        .with_person(
            login,
            lambda person: person.new_salary(new_salary, old_salary).with_ticket(ticket),
        )
        .build(StaffFactory(login='author-user').login)
    )

    ctx = ProposalContext.from_proposal_id(proposal_id)
    ctx.proposal.update_approvement(
        ticket_key=ticket,
        ticket_type=ApprovementTicketType.PERSONAL,
        uuid=old_ok_uuid,
        status=ApprovementStatus.OK,
    )
    update_approvement(ctx, ticket, ApprovementTicketType.PERSONAL, ApprovementStatus.WAIT_RERUN)

    del_url = ok_calls[0][0]
    assert del_url.endswith(f'/api/approvements/{old_ok_uuid}/close/')

    create_url, kwargs = ok_calls[1]
    assert create_url.endswith('/api/approvements/')
    data = json.loads(kwargs['data'])
    assert data['flow_context']['person_actions'][0]['salary']['new_salary'] == new_salary, data
    approvement = ProposalCtl(proposal_id).get_approvements()[0]
    assert data['uid'] == approvement.staff_uid, data

    ctl = ProposalCtl(proposal_id)
    approvement = ctl.get_approvements()[0]
    assert approvement.ticket_key == ticket
    assert approvement.uuid == OK_UUID
    assert approvement.status == ApprovementStatus.OK.value
