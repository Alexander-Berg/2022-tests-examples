# coding: utf-8

import json
from decimal import Decimal
from unittest.mock import patch

import pytest
import responses
from django.conf import settings
from pkg_resources import resource_string

from procu.api import models
from procu.utils.test import assert_status, parametrize, prepare_user

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------


params = [
    {
        'obj': 'addresses',
        'pk': 1,
        'model': models.Address,
        'patch': {'label': 'Контора'},
    },
    {
        'obj': 'categories',
        'pk': 1,
        'model': models.EnquiryCategory,
        'patch': {'name': 'Мыши'},
    },
    {
        'obj': 'contacts',
        'pk': 2,
        'model': models.User,
        'patch': {'comment': 'пам-пам'},
    },
    {
        'obj': 'currencies',
        'pk': 1,
        'model': models.Currency,
        'patch': {'name': 'тугрик'},
    },
    {
        'obj': 'discounts',
        'pk': 1,
        'model': models.Discount,
        'patch': {'value': Decimal('99.99')},
    },
    {
        'obj': 'legal_entities',
        'pk': 1,
        'model': models.LegalEntity,
        'patch': {'title': 'ОООО Яндекс'},
    },
    {'obj': 'units', 'pk': 1, 'model': models.Unit, 'patch': {'name': 'mmmm'}},
    {
        'obj': 'suppliers',
        'pk': 1,
        'model': models.Supplier,
        'patch': {'legal_name': 'waaat'},
    },
    {
        'obj': 'supplier_tags',
        'pk': 1,
        'model': models.SupplierTag,
        'patch': {'label': 'waaat'},
    },
]


def fixture(name):
    return resource_string('procu', f'fixtures/tracker/{name}.json')


# ------------------------------------------------------------------------------


@parametrize(params, 'obj')
def test_directory_list(clients, obj):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['manager'])

    resp = client.get(f'/api/{obj}')
    assert_status(resp, 200)

    reference = json.loads(
        resource_string('procu', f'fixtures/directories/list/{obj}.json')
    )

    assert resp.json() == reference


@parametrize(params, 'obj', 'pk')
def test_directory_retrieve(clients, obj, pk):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['manager'])

    resp = client.get(f'/api/{obj}/{pk}')
    assert_status(resp, 200)

    reference = json.loads(
        resource_string('procu', f'fixtures/directories/retrieve/{obj}.json')
    )

    assert resp.json() == reference


@parametrize(params, 'obj', 'pk', 'model')
def test_directory_destroy(clients, obj, pk, model):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.delete(f'/api/{obj}/{pk}')
    assert_status(resp, 204)

    qs = model.objects.filter(pk=pk)

    if hasattr(model, 'is_deleted'):
        qs = qs.filter(is_deleted=False)

    assert not qs.exists()


@parametrize(params, 'obj', 'pk', 'model', 'patch')
def test_directory_update(clients, obj, pk, model, patch):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.patch(f'/api/{obj}/{pk}', data=patch)
    assert_status(resp, 200)

    obj = model.objects.values(*patch.keys()).get(pk=pk)
    assert obj == patch


@parametrize(params, 'obj', 'pk', 'model')
def test_directory_create(clients, obj, pk, model):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = json.loads(
        resource_string('procu', f'fixtures/directories/create/{obj}.json')
    )

    with patch('procu.api.spark.tasks.update_supplier_risks') as task:
        resp = client.post(f'/api/{obj}', data=data)
        assert_status(resp, 201)

        if obj == 'suppliers':
            task.apply_async.assert_called()

    assert model.objects.filter(pk=resp.json()['id']).exists()


# ------------------------------------------------------------------------------
# OPTIONS


@parametrize(params, 'obj', 'pk')
def test_directory_options(clients, obj, pk):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    with responses.RequestsMock() as rsps:

        if obj == 'legal_entities':
            rsps.add(
                rsps.GET,
                f'{settings.STARTREK_API_URL}/v2/fields/logisticsWorktype',
                body=fixture('field_worktype'),
                status=200,
                content_type='application/json',
            )

        if obj == 'addresses':
            rsps.add(
                rsps.GET,
                f'{settings.STARTREK_API_URL}/v2/fields/logisticsLocation',
                body=fixture('field_location'),
                status=200,
                content_type='application/json',
            )

        resp = client.options(f'/api/{obj}')
        assert_status(resp, 200)

        resp = client.options(f'/api/{obj}/{pk}')
        assert_status(resp, 200)
