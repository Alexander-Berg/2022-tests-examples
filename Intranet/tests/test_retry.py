# coding: utf-8

import mock
import pytest

from django.db import connection
from django.db.backends.utils import CursorWrapper
from django.db.utils import DatabaseError, DataError
from django.test import Client

from idm.core.models import System
from idm.utils import reverse
from psycopg2._psycopg import InterfaceError
from django.db.backends.postgresql_psycopg2.base import BaseDatabaseWrapper
from django_pgaas.wsgi import IdempotentClientHandler


class RetryingClient(Client):
    def __init__(self, enforce_csrf_checks=False, **defaults):
        super(RetryingClient, self).__init__(enforce_csrf_checks=enforce_csrf_checks, **defaults)
        self.handler = IdempotentClientHandler(enforce_csrf_checks)


class FailFirst(BaseDatabaseWrapper):
    def __init__(self, *args, **kwargs):
        self.should_fail = True
        super(FailFirst, self).__init__(*args, **kwargs)

    def create_cursor(self):
        if self.should_fail:
            self.should_fail = False
            raise InterfaceError('Connection already closed')
        else:
            return super(FailFirst, self).create_cursor()


@pytest.mark.django_db(transaction=True)
def test_retry_view(arda_users, settings):
    client = RetryingClient()
    settings.MIDDLEWARE_CLASSES = []

    from django.db.transaction import get_connection
    conn = get_connection('default')
    assert not conn.in_atomic_block

    with mock.patch('django.db.backends.postgresql_psycopg2.base.DatabaseWrapper', FailFirst):
        response = client.get('/ping/')
        assert response.status_code == 200


@pytest.mark.django_db(transaction=True)
def test_retry_middleware(client, arda_users):
    from django.db.transaction import get_connection
    conn = get_connection('default')
    assert not conn.in_atomic_block

    with mock.patch('django.db.backends.postgresql_psycopg2.base.DatabaseWrapper', FailFirst):
        client.login('frodo')
        url = reverse('api_dispatch_detail', api_name='frontend', resource_name='users', username='frodo')
        response = client.get(url)
        assert response.status_code == 200


@pytest.mark.django_db(transaction=True)
def test_not_in_atomic_by_default(client, arda_users):
    from django.db.transaction import get_connection
    conn = get_connection('default')
    assert not conn.in_atomic_block


def test_retry_db_with_cursor():
    # retry should work after two failed attempts
    with mock.patch.object(CursorWrapper, 'execute', side_effect=[DatabaseError] * 2 + [None]):
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")


def test_retry_db_with_cursor_no_extra_retries():
    with mock.patch.object(CursorWrapper, 'execute', side_effect=[DatabaseError("mock")] * 6 + [None]):
        with pytest.raises(DatabaseError, match="mock"):
            with connection.cursor() as cursor:
                cursor.execute("SELECT 1")


@pytest.mark.django_db(transaction=True)
def test_retry_db_with_orm(simple_system):
    with mock.patch.object(CursorWrapper, 'execute', side_effect=[DatabaseError, DatabaseError, DataError('mock')]):
        # because we didn't fetch the results, so we expect ORM to go error
        with pytest.raises(DataError, match='mock'):
            System.objects.get(id=simple_system.id)


@pytest.mark.django_db
def test_retry_db_with_orm_no_extra_retries(simple_system):
    with mock.patch.object(CursorWrapper, 'execute', side_effect=[DatabaseError('mock')] * 6 + [None]):
        with pytest.raises(DatabaseError, match='mock'):
            System.objects.get(id=simple_system.id)
