import mock
import pytest

from staff.person.models import Staff


from staff.trip_questionary.controller.operations.taxi import (
    activate_corporate_account,
    deactivate_corporate_account,
    edit_account_url_template,
    search_account_url,
    TARIFS,
    TAXI_TIMEOUT,
    AVAILABLE_COST_CENTERS,
    COST_CENTERS_FORMAT,
)

organization_name = 'ООО Бюрократология'
client_id = '058004a25585441281b4b749102c0000'
oauth_token = 'AQAAAAAf2dY9AAVs9vW0OW0OW0O'
issue_key = 'TRIP-123'


@pytest.fixture
def taxi_settings(settings):
    settings.TAXI_API_CREDENTIALS = {
        organization_name:
            {
                'login': 'YNX3225',
                'password': 'xIridIUm19',
                'client_id': client_id,
                'oauth_token': oauth_token,
            },
    }
    return settings.TAXI_API_CREDENTIALS[organization_name]


@pytest.fixture
def mocked_request(monkeypatch):
    result = mock.Mock(
        return_value=mock.Mock(
            **{
                'json.return_value': {
                    'total': 1,
                    'users': [{'is_active': True, 'nickname': issue_key}],
                },
                'status_code': 200
            }
        )
    )
    monkeypatch.setattr('staff.lib.requests.Session.request', result)
    return result


def test_taxi_account_activating(taxi_settings, mocked_request):
    person = Staff(login='dep12-person', first_name='Пётр', last_name='Первый')
    phone_no = '+79250635552'
    issue_key = 'TRIP-123'

    activate_corporate_account(
        issue_key=issue_key,
        person=person,
        phone_no=phone_no,
        client_id=client_id,
        oauth_token=oauth_token,
    )

    mocked_request.assert_called_with(
        url='https://business.taxi.yandex.ru/api/1.0/client/058004a25585441281b4b749102c0000/user/',
        headers={'Authorization': oauth_token},
        json={
            'phone': phone_no,
            'role': {'classes': TARIFS},
            'nickname': issue_key,
            'cost_center': '',
            'fullname': '{0} {1} ({2})'.format(person.last_name, person.first_name, person.login),
            'is_active': True,
            'email': '',
            'cost_centers': {
                'required': True,
                'format': COST_CENTERS_FORMAT,
                'values': AVAILABLE_COST_CENTERS,
            }
        },
        method='PATCH',
        timeout=TAXI_TIMEOUT
    )


def test_taxi_account_deactivating(taxi_settings, mocked_request):
    phone_no = '+79250635552'
    issue_key = 'TRIP-123'

    deactivate_corporate_account(
        issue_key=issue_key,
        phone_no=phone_no,
        client_id=client_id,
        oauth_token=oauth_token,
    )

    assert len(mocked_request._mock_mock_calls) == 2  # Сделано два запроса
    call1, call2 = mocked_request._mock_mock_calls

    # первый - POST запрос за состоянием аккаунта
    _, args, kwargs = call1
    assert kwargs['method'] == 'POST'
    assert kwargs['url'] == search_account_url
    assert kwargs['json'] == {
        'user_phone': phone_no,
        'client_id': client_id,
    }

    # второй - PATCH запрос за отключением аккаунта
    _, args, kwargs = call2
    assert kwargs['method'] == 'PATCH'
    assert kwargs['url'] == edit_account_url_template.format(taxi_settings['client_id'])

    assert kwargs['json'] == {
        'phone': phone_no,
        'role': {'classes': TARIFS},
        'is_active': False,
    }
