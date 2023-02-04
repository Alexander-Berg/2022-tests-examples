import pytest
from mock import patch, MagicMock

from typing import Any, Dict
from random import random

from django.core.exceptions import ValidationError

from staff.departments.tests.factories import HRProductFactory
from staff.lib.testing import ValueStreamFactory, StaffFactory
from staff.proposal.forms.person import PersonsChangesProposalForm


@pytest.mark.django_db
def test_persons_changes_proposal_form():
    person = StaffFactory()
    target = PersonsChangesProposalForm(
        data={
            'actions': [
                _create_valid_change(person.login),
            ],
        },
    )

    login_existence_validator = MagicMock()

    with patch('staff.proposal.forms.person.StarTrekLoginExistenceValidator', return_value=login_existence_validator):
        result = target.is_valid()
        login_existence_validator.validate_logins.assert_called_once_with([person.login])

    assert result is True, target.errors


@pytest.mark.django_db
def test_persons_changes_proposal_form_not_in_star_trek():
    person = StaffFactory()
    target = PersonsChangesProposalForm(
        data={
            'actions': [
                _create_valid_change(person.login),
            ],
        },
    )

    login_existence_validator = MagicMock()
    validation_error_params = f'employees_error {random()}'
    validation_error_code = f'logins_not_in_star_trek {random()}'
    login_existence_validator.validate_logins.side_effect = ValidationError(
        'Some logins are not in Star Trek',
        code=validation_error_code,
        params=validation_error_params,
    )

    with patch('staff.proposal.forms.person.StarTrekLoginExistenceValidator', return_value=login_existence_validator):
        result = target.is_valid()
        login_existence_validator.validate_logins.assert_called_once_with([person.login])

    assert result is False
    assert target.errors == {'errors': {'': [{'code': validation_error_code, 'params': validation_error_params}]}}


def _create_valid_change(login: str) -> Dict[str, Any]:
    value_stream = ValueStreamFactory()
    HRProductFactory(value_stream=value_stream)

    return {
        'action_id': '1',
        'comment': 'comment 12345',
        'login': login,
        'value_stream': {
            'value_stream': value_stream.url,
        },
        'sections': ['value_stream'],
    }
