# coding: utf-8
from typing import Dict

import pytest

from idm.core.models.appmetrica import AppMetrica
from idm.tests.models.test_appmetrica import generate_app_record
from idm.tests.utils import random_slug
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


@pytest.fixture
def suggest_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/fields/app-metrica/all')


@pytest.fixture
def apps() -> Dict[str, AppMetrica]:
    return {
        name: AppMetrica.objects.create(
            **generate_app_record(application_id=application_id, application_name=name).as_dict
        )
        for application_id, name in [
            ('12345', 'natural'),
            ('112358', 'fibonachi'),
            ('124816', '2pow'),
            ('2941017', 'random'),
        ]
    }


def test_all(client, suggest_url, apps):
    response = client.json.get(suggest_url)
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': app.application_id, 'name': app.name} for app in apps.values()
        ]
    }


def test_filter__by_prefix_id(client, suggest_url, apps):
    response = client.json.get(suggest_url, {'q': '123'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': apps['natural'].application_id, 'name': apps['natural'].name}
        ]
    }


def test_filter__by_name_substring(client, suggest_url, apps):
    response = client.json.get(suggest_url, {'q': 'na'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': apps['natural'].application_id, 'name': apps['natural'].name},
            {'id': apps['fibonachi'].application_id, 'name': apps['fibonachi'].name},
        ]
    }


def test_filter__intersection(client, suggest_url, apps):
    response = client.json.get(suggest_url, {'q': '2'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': apps['2pow'].application_id, 'name': apps['2pow'].name},
            {'id': apps['random'].application_id, 'name': apps['random'].name},
        ]
    }


def test_not_found(client, suggest_url, apps):
    response = client.json.get(suggest_url, {'q': random_slug()})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': []
    }
