from mock import MagicMock, patch

import random
from typing import List

from staff.departments.controllers.tickets import ValueStreamTicket


def test_restructurisation_ticket_get_accessors_no_ticket():
    proposal_context = _create_context(
        None,
        hr_product_heads=['user0', 'user1', 'user2', 'user3'],
    )
    target = ValueStreamTicket(proposal_context)

    result = target.get_accessors()

    assert set(result) == {'user0', 'user1', 'user2', 'user3'}


def test_restructurisation_ticket_get_accessors():
    ticket_key = f'ticket-{random.random()}'
    proposal_context = _create_context(
        ticket_key,
        hr_product_heads=['user0', 'user1', 'user2', 'user3'],
    )
    target = ValueStreamTicket(proposal_context)
    issue = _create_issue(['person-1', 'user2', 'user3', 'user4'])

    with patch('staff.departments.controllers.tickets.restructurisation.get_issue', return_value=issue) as get_issue:
        result = target.get_accessors()
        get_issue.assert_called_once_with(key=ticket_key)

    assert set(result) == {'user0', 'user1', 'user2', 'user3', 'user4', 'person-1'}


def _create_context(
    ticket_key: str or None,
    hr_product_heads: List[str] = None,
):
    proposal_context = MagicMock()
    proposal_context.department_actions = []
    proposal_context.proposal_object = {'tickets': {'value_stream': ticket_key}}
    proposal_context.hr_products.values.return_value = [MagicMock(hr_product_head=login) for login in hr_product_heads]

    return proposal_context


def _create_issue(access_logins):
    return MagicMock(access=[MagicMock(login=login) for login in access_logins])
