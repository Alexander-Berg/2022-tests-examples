import pytest
from django.core.exceptions import ValidationError
from mock import patch, MagicMock

from random import randint, random

from django.conf import settings

from staff.proposal.forms.validators import StarTrekLoginExistenceValidator


def test_validate_logins_no_errors():
    logins = [f'login {i}' for i in range(randint(1, 5))]

    star_trek_response = MagicMock()
    star_trek_response.status_code = 422
    star_trek_response.json.return_value = {'errors': {}}

    json = MagicMock()
    star_trek_session = MagicMock()
    star_trek_session.post.return_value = star_trek_response

    target = StarTrekLoginExistenceValidator()

    with patch('staff.proposal.forms.validators.star_trek_login_existence.star_trek_session', star_trek_session):
        with patch('staff.proposal.forms.validators.star_trek_login_existence.json', json):
            target.validate_logins(logins)
            json.dumps.assert_called_once_with(
                {
                    'queue': 'STAFF',
                    'summary': 'summary',
                    'employees': logins,
                    'author': 'жопа с ручкой',
                },
            )
            star_trek_session.post.assert_called_once_with(
                f'{settings.STARTREK_API_URL}/v2/issues/',
                data=json.dumps.return_value,
            )


def test_validate_logins_with_errors():
    logins = [f'login {i}' for i in range(randint(1, 5))]

    star_trek_response = MagicMock()
    star_trek_response.status_code = 422
    employees_error = f'employees_error {random()}'
    star_trek_response.json.return_value = {'errors': {'employees': employees_error}}

    json = MagicMock()
    star_trek_session = MagicMock()
    star_trek_session.post.return_value = star_trek_response

    target = StarTrekLoginExistenceValidator()

    with patch('staff.proposal.forms.validators.star_trek_login_existence.star_trek_session', star_trek_session):
        with patch('staff.proposal.forms.validators.star_trek_login_existence.json', json):
            with pytest.raises(ValidationError) as validation_error:
                target.validate_logins(logins)
                json.dumps.assert_called_once_with(
                    {
                        'queue': 'STAFF',
                        'summary': 'summary',
                        'employees': logins,
                        'author': 'жопа с ручкой',
                    },
                )
                star_trek_session.post.assert_called_once_with(
                    f'{settings.STARTREK_API_URL}/v2/issues/',
                    data=json.dumps.return_value,
                )

    assert validation_error.value.code == 'logins_not_in_star_trek'
    assert validation_error.value.params == employees_error


def test_validate_logins_unexpected_response():
    logins = [f'login {i}' for i in range(randint(1, 5))]

    star_trek_response = MagicMock()
    star_trek_response.status_code = 423

    json = MagicMock()
    star_trek_session = MagicMock()
    star_trek_session.post.return_value = star_trek_response

    target = StarTrekLoginExistenceValidator()

    with patch('staff.proposal.forms.validators.star_trek_login_existence.star_trek_session', star_trek_session):
        with patch('staff.proposal.forms.validators.star_trek_login_existence.json', json):
            with pytest.raises(ValidationError) as validation_error:
                target.validate_logins(logins)
                json.dumps.assert_called_once_with(
                    {
                        'queue': 'STAFF',
                        'summary': 'summary',
                        'employees': logins,
                        'author': 'жопа с ручкой',
                    },
                )
                star_trek_session.post.assert_called_once_with(
                    f'{settings.STARTREK_API_URL}/v2/issues/',
                    data=json.dumps.return_value,
                )

    assert validation_error.value.code == 'unexpected_star_trek_response'
