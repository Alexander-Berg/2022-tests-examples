# coding: utf-8

import json

import pytest
from pkg_resources import resource_string

from procu.utils.test import assert_status, parametrize, prepare_user

pytestmark = [pytest.mark.django_db]

# ------------------------------------------------------------------------------


params = [
    {'suggest': 'addresses', 'query': 'колхоза', 'pk': 1},
    {'suggest': 'agents', 'query': 's001', 'pk': 2},
    {'suggest': 'categories', 'query': 'Электроника', 'pk': 1},
    {'suggest': 'currencies', 'query': 'EUR', 'pk': 1},
    {'suggest': 'enquiries', 'query': 'мобили', 'pk': 1},
    {'suggest': 'legal_entities', 'query': 'ООО', 'pk': 1},
    {'suggest': 'managers', 'query': 'Робот', 'pk': 1},
    {'suggest': 'supplier_tags', 'query': 'мебель', 'pk': 1},
    {'suggest': 'suppliers', 'query': 'копыта', 'pk': 1},
    {'suggest': 'suppliers_mixed', 'query': 'копыта', 'pk': 1},
    {'suggest': 'users', 'query': 'Робот', 'pk': 1},
    {'suggest': 'oebs/budget_lines', 'query': 'Operations', 'pk': 1},
    {'suggest': 'oebs/cfos', 'query': 'Системы', 'pk': 1},
    {'suggest': 'oebs/companies', 'query': 'MLU', 'pk': 1},
    {'suggest': 'oebs/currencies', 'query': 'RUB', 'pk': 1},
    {'suggest': 'oebs/mvps', 'query': 'ENRU', 'pk': 1},
    {'suggest': 'oebs/product_lines', 'query': 'BV01', 'pk': 1},
    {'suggest': 'oebs/programs', 'query': 'NOC0003', 'pk': 1},
    {'suggest': 'oebs/projects', 'query': 'Основной', 'pk': 1},
    {'suggest': 'oebs/purchase_groups', 'query': 'Item', 'pk': 1},
    {'suggest': 'oebs/services', 'query': 'Enterprise', 'pk': 1},
    {'suggest': 'oebs/subsystems', 'query': 'GCN', 'pk': 1},
    {'suggest': 'oebs/suppliers', 'query': 'Вершки', 'pk': 1},
    {'suggest': 'oebs/systems', 'query': 'СКС', 'pk': 1},
    {'suggest': 'oebs/tasks', 'query': 'КЦ', 'pk': 1},
]


# ------------------------------------------------------------------------------


@parametrize(params, 'suggest')
def test_suggests_empty(clients, suggest):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get(f'/api/suggests/{suggest}')
    assert_status(resp, 200)

    fixture = suggest.replace('/', '-')
    reference = json.loads(
        resource_string('procu', f'fixtures/suggests/{fixture}.json')
    )

    assert reference == resp.json()


@parametrize(params, 'suggest', 'query')
def test_suggests_search_existing(clients, suggest, query):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    # Search for existing entries
    resp = client.get(f'/api/suggests/{suggest}', data={'search': query})
    assert_status(resp, 200)
    assert resp.json()['results']


@parametrize(params, 'suggest')
def test_suggests_search_nonexisting(clients, suggest):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get(f'/api/suggests/{suggest}', data={'search': '__NONE__'})
    assert_status(resp, 200)
    assert not resp.json()['results']


@parametrize(params, 'suggest', 'pk')
def test_suggests_include_by_id(clients, suggest, pk):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get(f'/api/suggests/{suggest}', data={'id': pk})
    assert_status(resp, 200)

    results = resp.json()['results']
    ids = [x['id'] for x in results]

    assert ids == [pk]


@parametrize(params, 'suggest', 'pk')
def test_suggests_exclude_by_id(clients, suggest, pk):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get(
        f'/api/suggests/{suggest}', data={'id': pk, 'exclude_id': pk}
    )
    assert_status(resp, 200)

    assert not resp.json()['results']
