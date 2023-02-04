import pytest
from mock import MagicMock, patch

import random
from typing import List

from staff.departments.controllers.tickets import RestructurisationTicket
from staff.lib.testing import DepartmentAttrsFactory, StaffFactory


@pytest.mark.django_db
def test_restructurisation_ticket_get_accessors_many_analysts():
    proposal_context = _create_context(None, number_of_analyst=3)
    target = RestructurisationTicket(proposal_context)

    result = target.get_accessors()

    assert result == []


@pytest.mark.django_db
def test_restructurisation_ticket_get_accessors_no_ticket():
    proposal_context = _create_context(
        None,
        department_logins=[['user0', 'user1', 'user2', 'user3'], ['user1', 'user2', 'user7']],
        person_actions_logins=['user3', 'user10'],
    )
    target = RestructurisationTicket(proposal_context)

    result = target.get_accessors()

    assert set(result) == {'user0', 'user1', 'user2', 'user7'}


@pytest.mark.django_db
def test_restructurisation_ticket_get_accessors():
    ticket_key = f'ticket-{random.random()}'
    proposal_context = _create_context(
        ticket_key,
        department_logins=[['user0', 'user1', 'user2', 'user3'], ['user1', 'user2', 'user7']],
        person_actions_logins=['person-1', 'user3', 'user10'],
    )
    target = RestructurisationTicket(proposal_context)
    issue = _create_issue(['person-1', 'person-9', 'user2', 'user3', 'user4'])

    with patch('staff.departments.controllers.tickets.restructurisation.get_issue', return_value=issue) as get_issue:
        result = target.get_accessors()
        get_issue.assert_called_once_with(key=ticket_key)

    assert set(result) == {'user0', 'user1', 'user2', 'user7', 'person-9', 'user4'}


def _create_context(
    ticket_key: str or None,
    department_logins: List[List[str]] = None,
    person_actions_logins: List[str] = None,
    number_of_analyst: int = 2,
):
    analysts = set()
    for i in range(number_of_analyst):
        analysts.add(f'login-analyst-{i}')

    all_logins = dict()
    departments = set()
    if department_logins:
        for department in department_logins:
            for login in department:
                if login not in all_logins:
                    all_logins[login] = StaffFactory(login=login)
            department_attrs = DepartmentAttrsFactory()
            department_attrs.ticket_access = [all_logins[login] for login in department]
            departments.add(department_attrs)

    person_actions = [_create_person_action(login) for login in person_actions_logins or []]

    proposal_context = MagicMock()
    proposal_context.department_analysts = analysts
    proposal_context.department_actions = []
    proposal_context.proposal_object = {'tickets': {'restructurisation': ticket_key}}
    proposal_context.department_attrs_without_chains = departments
    proposal_context.person_actions = person_actions + [_create_person_action(f'person-{i}') for i in range(6)]

    return proposal_context


def _create_person_action(login: str):
    return {'login': login, 'office': 14}


def _create_issue(access_logins):
    return MagicMock(access=[MagicMock(login=login) for login in access_logins])
