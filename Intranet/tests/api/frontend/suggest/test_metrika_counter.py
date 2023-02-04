# coding: utf-8
from typing import Dict

import pytest

from idm.core.models.metrikacounter import MetrikaCounter
from idm.tests.models.test_metrikacounter import generate_counter_record
from idm.tests.utils import random_slug
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


@pytest.fixture
def suggest_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/fields/metrika-counter/all')


@pytest.fixture
def counters() -> Dict[str, MetrikaCounter]:
    return {
        name: MetrikaCounter.objects.create(
            **generate_counter_record(counter_id=counter_id, counter_name=name).as_dict
        )
        for counter_id, name in [
            ('12345', 'natural'),
            ('112358', 'fibonachi'),
            ('124816', '2pow'),
            ('2941017', 'random'),
        ]
    }


def test_all(client, suggest_url, counters):
    response = client.json.get(suggest_url)
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': counter.counter_id, 'name': counter.name} for counter in counters.values()
        ]
    }


def test_filter__by_prefix_id(client, suggest_url, counters):
    response = client.json.get(suggest_url, {'q': '123'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': counters['natural'].counter_id, 'name': counters['natural'].name}
        ]
    }


def test_filter__by_name_substring(client, suggest_url, counters):
    response = client.json.get(suggest_url, {'q': 'na'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': counters['natural'].counter_id, 'name': counters['natural'].name},
            {'id': counters['fibonachi'].counter_id, 'name': counters['fibonachi'].name},
        ]
    }


def test_filter__intersection(client, suggest_url, counters):
    response = client.json.get(suggest_url, {'q': '2'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': counters['2pow'].counter_id, 'name': counters['2pow'].name},
            {'id': counters['random'].counter_id, 'name': counters['random'].name},
        ]
    }


def test_not_found(client, suggest_url, counters):
    response = client.json.get(suggest_url, {'q': random_slug()})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': []
    }
