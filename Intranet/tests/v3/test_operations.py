# -*- coding: utf-8 -*-
from django.test import TestCase
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.accounts.factories import UserFactory
from events.celery_app import Celery
from events.v3.types import OperationStatus


class MockTaskFinished:
    def ready(self):
        return True

    def failed(self):
        return False


class MockTaskFailed:
    def ready(self):
        return True

    def failed(self):
        return True

    @property
    def info(self):
        return 'failed task'


class MockTaskWaiting:
    def ready(self):
        return False

    def failed(self):
        return False

    @property
    def status(self):
        return 'SENT'


class MockTaskNotRunning:
    def ready(self):
        return False

    def failed(self):
        return False

    @property
    def status(self):
        return 'PENDING'


class TestOperation(TestCase):
    client_class = YandexClient

    def setUp(self) -> None:
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    def test_get_operation_info(self):
        with patch.object(Celery, 'AsyncResult') as mock_params:
            mock_params.return_value = MockTaskFinished()
            response = self.client.get('/v3/operations/100/')

        self.assertEqual(response.status_code, 200)
        operation = response.json()
        self.assertEqual(operation['id'], '100')
        self.assertEqual(operation['status'], OperationStatus.ok)
        self.assertEqual(operation['message'], None)

        with patch.object(Celery, 'AsyncResult') as mock_params:
            mock_params.return_value = MockTaskFailed()
            response = self.client.get('/v3/operations/100/')

        self.assertEqual(response.status_code, 200)
        operation = response.json()
        self.assertEqual(operation['id'], '100')
        self.assertEqual(operation['status'], OperationStatus.fail)
        self.assertEqual(operation['message'], 'failed task')

        with patch.object(Celery, 'AsyncResult') as mock_params:
            mock_params.return_value = MockTaskWaiting()
            response = self.client.get('/v3/operations/100/')

        self.assertEqual(response.status_code, 200)
        operation = response.json()
        self.assertEqual(operation['id'], '100')
        self.assertEqual(operation['status'], OperationStatus.wait)
        self.assertEqual(operation['message'], None)

        with patch.object(Celery, 'AsyncResult') as mock_params:
            mock_params.return_value = MockTaskNotRunning()
            response = self.client.get('/v3/operations/100/')

        self.assertEqual(response.status_code, 200)
        operation = response.json()
        self.assertEqual(operation['id'], '100')
        self.assertEqual(operation['status'], OperationStatus.not_running)
        self.assertEqual(operation['message'], None)

    def test_get_and_post_operations_info(self):

        def mock_method(_, operation_id: str):
            match operation_id:
                case '1':
                    return MockTaskFinished()
                case '2':
                    return MockTaskFailed()
                case '3':
                    return MockTaskWaiting()
                case _:
                    return MockTaskNotRunning()

        with patch.object(Celery, 'AsyncResult', mock_method):
            response = self.client.get('/v3/operations/', {'ids': '1,2,3,4'})

        self.assertEqual(response.status_code, 200)
        operations = response.json()

        self.assertEqual(len(operations), 4)

        self.assertEqual(operations[0]['id'], '1')
        self.assertEqual(operations[0]['status'], OperationStatus.ok)
        self.assertEqual(operations[0]['message'], None)

        self.assertEqual(operations[1]['id'], '2')
        self.assertEqual(operations[1]['status'], OperationStatus.fail)
        self.assertEqual(operations[1]['message'], 'failed task')

        self.assertEqual(operations[2]['id'], '3')
        self.assertEqual(operations[2]['status'], OperationStatus.wait)
        self.assertEqual(operations[2]['message'], None)

        self.assertEqual(operations[3]['id'], '4')
        self.assertEqual(operations[3]['status'], OperationStatus.not_running)
        self.assertEqual(operations[3]['message'], None)

        with patch.object(Celery, 'AsyncResult', mock_method):
            response = self.client.post('/v3/operations/', {'ids': ['1', '2', '3', '4']}, format='json')

        self.assertEqual(response.status_code, 200)
        operations = response.json()

        self.assertEqual(len(operations), 4)

        self.assertEqual(operations[0]['id'], '1')
        self.assertEqual(operations[0]['status'], OperationStatus.ok)
        self.assertEqual(operations[0]['message'], None)

        self.assertEqual(operations[1]['id'], '2')
        self.assertEqual(operations[1]['status'], OperationStatus.fail)
        self.assertEqual(operations[1]['message'], 'failed task')

        self.assertEqual(operations[2]['id'], '3')
        self.assertEqual(operations[2]['status'], OperationStatus.wait)
        self.assertEqual(operations[2]['message'], None)

        self.assertEqual(operations[3]['id'], '4')
        self.assertEqual(operations[3]['status'], OperationStatus.not_running)
        self.assertEqual(operations[3]['message'], None)
