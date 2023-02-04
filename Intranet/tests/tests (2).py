# coding: utf-8

from datetime import date

import pytest
from django.db.models import F

from procu.api import models
from procu.api.enums import ES, PRIORITY as PR
from procu.utils.test import assert_status, prepare_user
from . import reference

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------


def test_enquiries_list(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries_all')

    assert_status(resp, 200)
    assert resp.json() == reference.ENQUIRIES_ALL


@pytest.mark.parametrize(
    'field',
    (
        'id',
        'priority',
        'subject',
        'status',
        'deadline_at',
        'delivery_at',
        'manager__last_name',
        'updated_at',
        'author__last_name',
    ),
)
@pytest.mark.parametrize('direction', ('', '-'))
def test_enquiries_list_ordering(clients, field, direction):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    params = {'ordering': direction + field}
    resp = client.get('/api/enquiries_all', data=params)
    assert_status(resp, 200)


@pytest.mark.parametrize(
    'filter_field,filter_value,model_field,model_values',
    (
        (
            'status',
            [ES.keys[ES.REVIEW], ES.keys[ES.CLOSED]],
            'status',
            [ES.REVIEW, ES.CLOSED],
        ),
        ('status', [ES.keys[ES.CLOSED]], 'status', [ES.CLOSED]),
        ('priority', [PR.keys[PR.CRITICAL]], 'priority', [PR.CRITICAL]),
        ('cfo', 1, 'cfo', [1]),
        ('author', 1, 'author', [1]),
        ('author', [1, 3], 'author', [1, 3]),
        ('manager', 'none', 'manager', [None]),
        ('manager', [1, 3], 'manager', [1, 3]),
        ('category', 'none', 'category', [None]),
        ('category', [1], 'category', [1]),
        ('address', [1], 'address', [1]),
        ('legal_entity', [1], 'legal_entity', [1]),
    ),
)
def test_enquiries_list_filters(
    clients, filter_field, filter_value, model_field, model_values
):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    params = {filter_field: filter_value}
    resp = client.get('/api/enquiries_all', data=params)
    assert_status(resp, 200)

    results = resp.json().get('results', [])
    assert len(results) > 0

    found = models.Enquiry.objects.values_list(model_field, flat=True).filter(
        id__in=(e['id'] for e in results)
    )
    assert all(c in model_values for c in found)


def test_enquiries_search_by_key(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    params = {'search': 'YP1'}
    resp = client.get('/api/enquiries_all', data=params)
    assert_status(resp, 200)

    results = resp.json().get('results', [])
    assert len(results) == 1

    ids = {e['id'] for e in results}
    assert ids == {1}


def test_enquiries_search_by_subject(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    query = 'мобил'

    params = {'search': query}
    resp = client.get('/api/enquiries_all', data=params)
    assert_status(resp, 200)

    results = resp.json().get('results', [])

    assert all(query in e['subject'] for e in results)


# ------------------------------------------------------------------------------


ORACLE_DATA = {
    'cfo': 1,
    'project': 1,
    'task': 1,
    'mvp': 1,
    'service': 1,
    'budget_line': 1,
    'system': 1,
    'subsystem': 1,
}

PRODUCTS_DATA = {
    'products': [
        {'name': 'GT', 'qty': 10, 'comment': 'fast'},
        {'name': 'Электробайк', 'qty': 5, 'comment': 'and furious'},
    ]
}

CREATE_GOODS_DATA = {
    'subject': 'Велосипеды',
    'no_replacement': True,
    'description': 'Септаккорд обретает субъект власти',
    'reference': [],
    'attachments': [1, 2],
    **PRODUCTS_DATA,
    **ORACLE_DATA,
}


CREATE_PROCU_DATA = {
    'subject': 'Велосипеды',
    'no_replacement': True,
    'description': 'Септаккорд обретает субъект власти',
    'reference': [],
    'attachments': [1, 2],
    'assignee': 1,
    'legal_entity': 1,
    'address': 1,
    'category': 1,
    'priority': PR.keys[PR.BLOCKER],
    **PRODUCTS_DATA,
    **ORACLE_DATA,
}


@pytest.mark.parametrize(
    'type,data,ref',
    (
        ('goods', CREATE_GOODS_DATA, reference.CREATE_GOODS),
        ('procu', CREATE_PROCU_DATA, reference.CREATE_PROCU),
    ),
)
def test_create_enquiry(clients, type, data, ref):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.post(f'/api/enquiries?type={type}', data=data)
    assert_status(resp, 201)

    assert resp.json() == ref


# ------------------------------------------------------------------------------


def test_get_enquiry(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get(f'/api/enquiries/1')
    assert_status(resp, 200)

    assert resp.json() == reference.ENQUIRY


def test_update_enquiry(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {
        'description': 'foo',
        'due_at': date(2020, 1, 1),
        'no_replacement': True,
        'products': [{'name': 'iPhone XS', 'qty': 12, 'comment': ''}],
    }

    resp = client.patch('/api/enquiries/1', data=data)
    assert_status(resp, 200)

    obj = models.Enquiry.objects.values(
        'due_at',
        'no_replacement',
        products=F('initial_products'),
        description=F('internal_comment'),
    ).get(id=1)

    assert obj == data


def test_get_enquiry_header(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get(f'/api/enquiries/1/header')
    assert_status(resp, 200)

    assert resp.json() == reference.ENQUIRY_HEADER


def test_update_enquiry_header(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {'assignee': 3, 'subject': 'foobar'}

    resp = client.patch('/api/enquiries/1/header', data=data)
    assert_status(resp, 200)

    obj = models.Enquiry.objects.values('subject', assignee=F('manager')).get(
        id=1
    )

    assert obj == data


def test_cancel_enquiry(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.post('/api/enquiries/1/cancel')
    assert_status(resp, 200)

    status = models.Enquiry.objects.values_list('status', flat=True).get(id=1)
    assert status == ES.CLOSED


@pytest.mark.parametrize(
    'role,enquiry_id,ref',
    (
        ('admin', 1, reference.OPTIONS_ADMIN),
        ('admin', 2, reference.OPTIONS_PUBLISHABLE),
        ('employee', 1, reference.OPTIONS_EMPLOYEE),
    ),
)
def test_enquiry_options(clients, role, enquiry_id, ref):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=[role])

    resp = client.get(f'/api/enquiries/{enquiry_id}/options')
    assert_status(resp, 200)

    assert resp.json() == ref
