# coding: utf-8

import json

import pytest
from pkg_resources import resource_string

from procu.utils.test import assert_status, parametrize, prepare_user

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------


params = [
    {'obj': 'cfos', 'pk': 1},
    {'obj': 'product_lines', 'pk': 1},
    {'obj': 'purchase_groups', 'pk': 1},
    {'obj': 'services', 'pk': 1},
    {'obj': 'tasks', 'pk': 1},
]


# ------------------------------------------------------------------------------


@parametrize(params, 'obj')
def test_oebs_list(clients, obj):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['manager'])

    resp = client.get(f'/api/oebs/{obj}')
    assert_status(resp, 200)

    reference = json.loads(
        resource_string('procu', f'fixtures/oebs/list/{obj}.json')
    )

    assert resp.json() == reference


@parametrize(params, 'obj', 'pk')
def test_oebs_retrieve(clients, obj, pk):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['manager'])

    resp = client.get(f'/api/oebs/{obj}/{pk}')
    assert_status(resp, 200)

    reference = json.loads(
        resource_string('procu', f'fixtures/oebs/retrieve/{obj}.json')
    )

    assert resp.json() == reference


# ------------------------------------------------------------------------------
# OPTIONS


@parametrize(params, 'obj', 'pk')
def test_oebs_options(clients, obj, pk):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.options(f'/api/oebs/{obj}')
    assert_status(resp, 200)

    resp = client.options(f'/api/oebs/{obj}/{pk}')
    assert_status(resp, 200)
