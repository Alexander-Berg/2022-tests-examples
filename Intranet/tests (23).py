# coding: utf-8

import pytest
from django.core import mail

from procu.api import models
from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]


def test_create_cold_supplier(clients):

    mail.outbox = []

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {'email': 'foo@bar.baz', 'title': 'FooBar'}

    resp = client.post('/api/suppliers/cold/', data=data)
    assert_status(resp, 201)

    assert not mail.outbox

    qs = models.Supplier.objects.filter(is_cold=True, title=data['title'])
    assert qs.exists()

    supplier = qs.first()

    assert models.User.objects.filter(
        is_staff=False, email=data['email'], supplier=supplier
    ).exists()


def test_warmup_cold_supplier(clients):

    mail.outbox = []

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    supplier = models.Supplier.objects.get(id=2)

    resp = client.post('/api/suppliers/2/warmup')
    assert_status(resp, 200)

    supplier.refresh_from_db()
    contact = supplier.agents.first()

    assert not supplier.is_cold
    assert not contact.is_cold

    assert mail.outbox
