import json
from mock import Mock, patch
import pytest
from requests.exceptions import RequestException, Timeout

from django.conf import settings
from django.core.urlresolvers import reverse

API_URL = f'https://{settings.SLACK_HOST}/ya-admin/get-status/'


@pytest.mark.django_db
def test_slack_status_ok(client):
    login = 'uhura'
    actual_slack_status = {'status': 'active'}
    fake_get_result = Mock(status_code=200, json=lambda: actual_slack_status)
    response_mock = Mock(return_value=fake_get_result)
    switch_mock = Mock(return_value=True)

    url = reverse('profile:slack_status', kwargs={'login': login})

    with patch('staff.person_profile.views.slack.waffle.switch_is_active', switch_mock):
        with patch('staff.person_profile.views.slack.requests.get', response_mock):
            response = client.get(url)

            switch_mock.assert_called_with('show_slack_status')
            response_mock.assert_called_once_with(API_URL, params={'username': login}, timeout=0.15)
            assert json.loads(response.content.decode('utf-8')) == {'target': {'slack_status': actual_slack_status}}
            assert response.status_code == 200


@pytest.mark.django_db
def test_slack_status_api_not_responding(client):
    login = 'spock'
    response_mock = Mock(side_effect=Timeout())
    switch_mock = Mock(return_value=True)

    url = reverse('profile:slack_status', kwargs={'login': login})

    with patch('staff.person_profile.views.slack.waffle.switch_is_active', switch_mock):
        with patch('staff.person_profile.views.slack.requests.get', response_mock):
            response = client.get(url)

            switch_mock.assert_called_with('show_slack_status')
            response_mock.assert_called_once_with(API_URL, params={'username': login}, timeout=0.15)
            assert json.loads(response.content.decode('utf-8')) == {
                'error': f'{settings.SLACK_HOST} does not responding',
            }
            assert response.status_code == 200


@pytest.mark.django_db
def test_slack_status_bad_status(client):
    login = 'kirk'
    bad_status = 456
    fake_get_result = Mock(status_code=bad_status, content='Some error message')
    fake_get_result.raise_for_status = Mock(side_effect=RequestException())
    response_mock = Mock(return_value=fake_get_result)
    switch_mock = Mock(return_value=True)

    url = reverse('profile:slack_status', kwargs={'login': login})

    with patch('staff.person_profile.views.slack.waffle.switch_is_active', switch_mock):
        with patch('staff.person_profile.views.slack.requests.get', response_mock):
            response = client.get(url)

            switch_mock.assert_called_with('show_slack_status')
            response_mock.assert_called_once_with(API_URL, params={'username': login}, timeout=0.15)
            assert json.loads(response.content.decode('utf-8')) == {
                'error': f'{settings.SLACK_HOST} responded with status {bad_status}',
            }
            assert response.status_code == 200


@pytest.mark.django_db
def test_slack_status_invalid_json(client):
    login = 'spock'
    response_mock = Mock()
    response_mock.json = Mock(side_effect=json.JSONDecodeError('', '', 0))
    switch_mock = Mock(return_value=True)

    url = reverse('profile:slack_status', kwargs={'login': login})

    with patch('staff.person_profile.views.slack.waffle.switch_is_active', switch_mock):
        with patch('staff.person_profile.views.slack.requests.get', response_mock):
            response = client.get(url)

            switch_mock.assert_called_with('show_slack_status')
            response_mock.assert_called_once_with(API_URL, params={'username': login}, timeout=0.15)
            assert json.loads(response.content.decode('utf-8')) == {
                'error': f'{settings.SLACK_HOST} sent invalid json'
            }
            assert response.status_code == 200


@pytest.mark.django_db
def test_slack_status_off(client):
    login = 'dummy'
    switch_mock = Mock(return_value=False)

    url = reverse('profile:slack_status', kwargs={'login': login})

    with patch('staff.person_profile.views.slack.waffle.switch_is_active', switch_mock):
        response = client.get(url)
        assert json.loads(response.content.decode('utf-8')) == {'status': 'disabled'}
        assert response.status_code == 200
