# -*- coding: utf-8 -*-
import responses

from django.test import TestCase, override_settings
from requests.exceptions import RequestException
from unittest.mock import patch

from events.common_app.helpers import override_cache_settings
from events.common_app.startrek.client import get_startrek_client


class TestStartrekClient(TestCase):

    @override_cache_settings()
    @responses.activate
    def test_create_issue(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'tags': ['foo', 'bar'],
            },
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-2025/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'tags': ['foo', 'bar'],
            },
        )
        client = get_startrek_client()
        data = {
            'queue': 'FORMS',
            'summary': 'test summary',
            'description': 'test description',
            'tags': ['foo', 'bar'],
        }
        issue = client.create_issue(data)
        another_issue = client.get_issue(issue['key'])
        self.assertEqual(len(responses.calls), 2)

        self.assertTrue(issue['key'].startswith('FORMS'))
        self.assertEqual(issue['summary'], 'test summary')
        self.assertEqual(issue['description'], 'test description')
        self.assertListEqual(issue['tags'], ['foo', 'bar'])

        self.assertEqual(issue['id'], another_issue['id'])
        self.assertEqual(issue['key'], another_issue['key'])
        self.assertEqual(issue['summary'], another_issue['summary'])
        self.assertEqual(issue['description'], another_issue['description'])
        self.assertListEqual(issue['tags'], another_issue['tags'])

    @override_cache_settings()
    @responses.activate
    def test_create_issue_for_parent(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'parent': 'FORMS-42',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'tags': ['foo', 'bar'],
            },
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-2025/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'parent': 'FORMS-42',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'tags': ['foo', 'bar'],
            },
        )
        client = get_startrek_client()
        data = {
            'parent': 'FORMS-42',
            'summary': 'test summary',
            'description': 'test description',
            'tags': ['foo', 'bar'],
        }
        issue = client.create_issue(data)
        another_issue = client.get_issue(issue['key'])
        self.assertEqual(len(responses.calls), 2)

        self.assertTrue(issue['key'].startswith('FORMS'))
        self.assertEqual(issue['parent'], 'FORMS-42')
        self.assertEqual(issue['summary'], 'test summary')
        self.assertEqual(issue['description'], 'test description')
        self.assertListEqual(issue['tags'], ['foo', 'bar'])

        self.assertEqual(issue['id'], another_issue['id'])
        self.assertEqual(issue['key'], another_issue['key'])
        self.assertEqual(issue['summary'], another_issue['summary'])
        self.assertEqual(issue['description'], another_issue['description'])
        self.assertListEqual(issue['tags'], another_issue['tags'])

    @override_cache_settings()
    @responses.activate
    def test_create_issue_for_parent_with_queue(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'parent': 'TEST-775',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'tags': ['foo', 'bar'],
            },
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-2025/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'parent': 'TEST-775',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'tags': ['foo', 'bar'],
            },
        )
        client = get_startrek_client()
        data = {
            'parent': 'TEST-775',
            'queue': 'FORMS',
            'summary': 'test summary',
            'description': 'test description',
            'tags': ['foo', 'bar'],
        }
        issue = client.create_issue(data)
        another_issue = client.get_issue(issue['key'])
        self.assertEqual(len(responses.calls), 2)

        self.assertTrue(issue['key'].startswith('FORMS'))
        self.assertEqual(issue['parent'], 'TEST-775')
        self.assertEqual(issue['summary'], 'test summary')
        self.assertEqual(issue['description'], 'test description')
        self.assertListEqual(issue['tags'], ['foo', 'bar'])

        self.assertEqual(issue['id'], another_issue['id'])
        self.assertEqual(issue['key'], another_issue['key'])
        self.assertEqual(issue['summary'], another_issue['summary'])
        self.assertEqual(issue['description'], another_issue['description'])
        self.assertListEqual(issue['tags'], another_issue['tags'])

    @override_cache_settings()
    @responses.activate
    def test_create_issue_should_raise_exception(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={'errors': {}, 'errorMessages': ['Queue does not exist.'], 'statusCode': 404},
            status=404,
        )
        client = get_startrek_client()
        data = {
            'queue': 'FORMZZZ',
            'summary': 'test summary',
            'description': 'test description',
            'tags': ['foo', 'bar'],
        }
        with self.assertRaises(RequestException) as e:
            client.create_issue(data)
            self.assertEqual(e.response.status_code, 400)

    @override_cache_settings()
    @responses.activate
    def test_update_issue(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'tags': ['foo', 'bar'],
            },
        )
        responses.add(
            responses.PATCH,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-2025/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'key': 'FORMS-2025',
                'summary': 'test summary 2',
                'description': 'test description',
                'tags': ['foo', 'bar', 'zip'],
            },
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-2025/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'key': 'FORMS-2025',
                'summary': 'test summary 2',
                'description': 'test description',
                'tags': ['foo', 'bar', 'zip'],
            },
        )
        client = get_startrek_client()
        insert_data = {
            'queue': 'FORMS',
            'summary': 'test summary',
            'description': 'test description',
            'tags': ['foo', 'bar'],
        }
        update_data = {
            'summary': 'test summary 2',
            'tags': ['foo', 'bar', 'zip'],
        }
        issue = client.create_issue(insert_data)
        issue_key = issue.get('key')
        issue = client.update_issue(issue_key, update_data)
        another_issue = client.get_issue(issue_key)

        self.assertEqual(issue['key'], issue_key)
        self.assertEqual(issue['summary'], 'test summary 2')
        self.assertEqual(issue['description'], 'test description')
        self.assertListEqual(issue['tags'], ['foo', 'bar', 'zip'])

        self.assertEqual(issue['id'], another_issue['id'])
        self.assertEqual(issue['key'], another_issue['key'])
        self.assertEqual(issue['summary'], another_issue['summary'])
        self.assertEqual(issue['description'], another_issue['description'])
        self.assertListEqual(issue['tags'], another_issue['tags'])

    @override_cache_settings()
    @responses.activate
    def test_update_issue_should_raise_exception(self):
        responses.add(
            responses.PATCH,
            'https://st-api.test.yandex-team.ru/service/issues/FORMZZZ-XXX/',
            json={'errors': {}, 'errorMessages': ['Internal error'], 'statusCode': 500},
            status=500,
        )
        client = get_startrek_client()
        issue_key = 'FORMZZZ-XXX'
        data = {
            'queue': 'FORMZZZ',
            'summary': 'test summary',
            'tags': ['foo', 'bar'],
        }
        with self.assertRaises(RequestException) as e:
            client.update_issue(issue_key, data)
            self.assertEqual(e.response.status_code, 400)

    @override_cache_settings()
    @responses.activate
    def test_get_issue_by_unique_value(self):
        unique_value = 'FORMS/59ef04201c9eab16872c1152'
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json=[{
                'id': '5fec8f5475e499650504b1a4',
                'key': 'FORMS-1070',
                'summary': 'testme',
                'unique': unique_value,
            }],
        )
        client = get_startrek_client()
        issue = client.get_issue_by_unique_value(unique_value)

        self.assertIsNotNone(issue)
        self.assertEqual(issue['key'], 'FORMS-1070')
        self.assertEqual(issue['unique'], unique_value)
        self.assertEqual(issue['summary'], 'testme')

    @override_cache_settings()
    @responses.activate
    def test_get_issue_by_unique_value_should_return_none(self):
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json=[],
        )
        client = get_startrek_client()
        unique_value = 'FORMS/not-exists-unique-key'
        issue = client.get_issue_by_unique_value(unique_value)

        self.assertIsNone(issue)

    @override_cache_settings()
    @responses.activate
    def test_create_queue(self):
        description = 'some test description'
        name = 'Forms testing queue'
        lead = 'kdunaev'
        key = 'FORMSTESTCREATE'
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/queues/',
            json={
                'id': 4730,
                'key': key,
                'name': name,
                'description': description,
                'lead': {'id': lead},
            },
        )
        responses.add(
            responses.GET,
            f'https://st-api.test.yandex-team.ru/service/queues/{key}/',
            json={
                'id': 4730,
                'key': key,
                'name': name,
                'description': description,
                'lead': {'id': lead},
            },
        )
        client = get_startrek_client()
        queue = client.create_queue(
            name=name,
            description=description,
            key=key,
            lead=lead,
        )
        another_queue = client.get_queue(key)

        self.assertEqual(queue['description'], description)
        self.assertEqual(queue['name'], name)
        self.assertEqual(queue['key'], key)
        self.assertEqual(queue['lead']['id'], lead)

        self.assertEqual(queue['id'], another_queue['id'])
        self.assertEqual(queue['key'], another_queue['key'])
        self.assertEqual(queue['description'], another_queue['description'])
        self.assertEqual(queue['lead']['id'], another_queue['lead']['id'])

    @override_cache_settings()
    @responses.activate
    def test_create_attachment(self):
        file_name = 'textfile.txt'
        file_content = b'some content'
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/attachments/',
            json={
                'id': '1597770',
                'name': file_name,
                'size': len(file_content),
            },
        )
        client = get_startrek_client()
        attachment = client.create_attachment(file_name, file_content)

        self.assertIsNotNone(attachment)
        self.assertEqual(attachment['id'], '1597770')
        self.assertEqual(attachment['name'], file_name)
        self.assertEqual(attachment['size'], len(file_content))

    @override_cache_settings()
    @responses.activate
    def test_get_attachments(self):
        attachment_id = '1597770'
        file_name = 'textfile.txt'
        file_size = 12
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5fec8f5475e499650504b1a4',
                'key': 'FORMS-2025',
                'summary': 'test summary',
                'description': 'test description',
                'attachments': [{
                    'id': attachment_id,
                    'name': file_name,
                    'size': file_size,
                }],
            },
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-2025/attachments/',
            json=[{
                'id': attachment_id,
                'name': file_name,
                'size': file_size,
            }],
        )
        client = get_startrek_client()
        data = {
            'queue': 'FORMS',
            'summary': 'test summary',
            'description': 'test description',
            'attachmentIds': [attachment_id],
        }
        issue = client.create_issue(data)
        self.assertIsNotNone(issue)
        attachments = client.get_attachments(issue['key'])

        self.assertIsNotNone(attachments)
        self.assertEqual(len(attachments), 1)
        attachment = attachments[0]
        self.assertEqual(attachment['name'], file_name)
        self.assertEqual(attachment['size'], file_size)

    @override_cache_settings()
    @responses.activate
    def test_shouldnt_create_ticket_if_not_unique(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={'errors': {}, 'errorMessages': ['Задача уже существует'], 'statusCode': 409},
            headers={'X-Ticket-Key': 'FORMS-1070'},
            status=409,
        )
        client = get_startrek_client()
        unique_value = 'FORMS/59ef04201c9eab16872c1152'
        data = {
            'queue': 'FORMS',
            'summary': 'test summary',
            'description': 'test description',
            'unique': unique_value,
        }
        with self.assertRaises(RequestException) as ex:
            client.create_issue(data)

        ex = ex.exception
        self.assertIsNotNone(ex.response)
        self.assertEqual(ex.response.status_code, 409)
        self.assertEqual(ex.response.headers['X-Ticket-Key'], 'FORMS-1070')


class TestStartrekFieldsFindField(TestCase):
    fixtures = ['initial_data.json']

    @override_settings(IS_BUSINESS_SITE=True)
    @override_cache_settings()
    @responses.activate
    def test_should_return_field_for_biz(self):
        response_json = {
            'id': 'tags',
            'key': 'tags',
            'name': 'Tags',
            'schema': {'type': 'array', 'items': 'string'},
        }

        field_id = 'tags'
        url = f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/'
        responses.add(responses.GET, url, json=response_json)

        with patch('events.common_app.startrek.client.get_robot_tracker') as mock_robot_tracker:
            mock_robot_tracker.return_value = '321'
            client = get_startrek_client(dir_id='123')
            field = client.get_field(field_id)

        expected = {
            'id': 'tags',
            'key': 'tags',
            'name': 'Tags',
            'schema': {'type': 'array', 'items': 'string'},
        }
        self.assertEqual(field, expected)
        self.assertEqual(len(responses.calls), 1)
        mock_robot_tracker.assert_called_once_with('123')

    @override_settings(IS_BUSINESS_SITE=True)
    @override_cache_settings()
    @responses.activate
    def test_shouldnt_return_field_for_biz(self):
        field_id = 'tagger'
        url = f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/'
        responses.add(responses.GET, url, status=404)

        with patch('events.common_app.startrek.client.get_robot_tracker') as mock_robot_tracker:
            mock_robot_tracker.return_value = '321'
            client = get_startrek_client(dir_id='234')
            field = client.get_field(field_id)

        self.assertIsNone(field)
        self.assertEqual(len(responses.calls), 1)
        mock_robot_tracker.assert_called_once_with('234')

    @override_cache_settings()
    @responses.activate
    def test_should_return_system_field(self):
        response_json = {
            'id': 'tags',
            'key': 'tags',
            'name': 'Tags',
            'schema': {'type': 'array', 'items': 'string'},
        }
        field_id = 'tags'
        url = f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/'
        responses.add(responses.GET, url, json=response_json)

        client = get_startrek_client()
        field = client.get_field(field_id)

        expected = {
            'id': 'tags',
            'key': 'tags',
            'name': 'Tags',
            'schema': {'type': 'array', 'items': 'string'},
        }
        self.assertEqual(field, expected)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_should_return_system_field_cached(self):
        response_json = {
            'id': 'tagz',
            'key': 'tagz',
            'name': 'Tagz',
            'schema': {'type': 'array', 'items': 'string'},
        }
        field_id = 'tags'
        url = f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/'
        responses.add(responses.GET, url, json=response_json)

        client = get_startrek_client()
        field = client.get_field(field_id)

        expected = {
            'id': 'tagz',
            'key': 'tagz',
            'name': 'Tagz',
            'schema': {'type': 'array', 'items': 'string'},
        }
        self.assertEqual(field, expected)
        self.assertEqual(len(responses.calls), 1)

        field = client.get_field(field_id)
        self.assertEqual(field, expected)
        self.assertEqual(len(responses.calls), 1)

    @override_cache_settings()
    @responses.activate
    def test_shouldnt_return_system_field(self):
        field_id = 'tagger'
        url = f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/'
        responses.add(responses.GET, url, status=404)

        client = get_startrek_client()
        field = client.get_field(field_id)

        self.assertIsNone(field)
        self.assertEqual(len(responses.calls), 1)

    @override_cache_settings()
    @responses.activate
    def test_should_return_local_field(self):
        response_json = {
            'id': '01234--tagging',
            'key': 'tagging',
            'name': 'Tagging',
            'schema': {'type': 'string'},
            'queue': {'key': 'TEST'},
        }
        field_id = '01234--tagging'
        url = f'https://st-api.test.yandex-team.ru/service/localFields/{field_id}/'
        responses.add(responses.GET, url, json=response_json)

        client = get_startrek_client()
        field = client.get_field(field_id)

        expected = {
            'id': '01234--tagging',
            'key': 'tagging',
            'name': 'Tagging',
            'schema': {'type': 'string'},
            'queue': {'key': 'TEST'},
        }
        self.assertEqual(field, expected)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_should_return_local_field_cached(self):
        response_json = {
            'id': '01234--tagging',
            'key': 'tagging',
            'name': 'Tagging',
            'schema': {'type': 'string'},
            'queue': {'key': 'TEST'},
        }
        field_id = '01234--tagging'
        url = f'https://st-api.test.yandex-team.ru/service/localFields/{field_id}/'
        responses.add(responses.GET, url, json=response_json)

        client = get_startrek_client()
        field = client.get_field(field_id)

        expected = {
            'id': '01234--tagging',
            'key': 'tagging',
            'name': 'Tagging',
            'schema': {'type': 'string'},
            'queue': {'key': 'TEST'},
        }
        self.assertEqual(field, expected)
        self.assertEqual(len(responses.calls), 1)

        field = client.get_field(field_id)
        self.assertEqual(field, expected)
        self.assertEqual(len(responses.calls), 1)

    @override_cache_settings()
    @responses.activate
    def test_shouldnt_return_local_field(self):
        field_id = '01234--tagging'
        url = f'https://st-api.test.yandex-team.ru/v2/localFields/{field_id}/'
        responses.add(responses.GET, url, status=404)

        client = get_startrek_client()
        field = client.get_field(field_id)

        self.assertIsNone(field)
        self.assertEqual(len(responses.calls), 1)

    @override_cache_settings()
    @responses.activate
    def test_should_return_local_fields(self):
        queue = 'TEST'
        response_json = [{
            'id': '01234--tagging',
            'key': 'tagging',
            'name': 'Tagging',
            'schema': {'type': 'string'},
            'queue': {'key': queue},
        }]
        url = f'https://st-api.test.yandex-team.ru/v2/queues/{queue}/localFields/'
        responses.add(responses.GET, url, json=response_json)

        client = get_startrek_client()
        fields = client.get_local_fields(queue)

        expected = [{
            'id': '01234--tagging',
            'key': 'tagging',
            'name': 'Tagging',
            'schema': {'type': 'string'},
            'queue': {'key': queue},
        }]
        self.assertEqual(fields, expected)
        self.assertEqual(len(responses.calls), 1)

    @override_cache_settings()
    @responses.activate
    def test_should_return_system_fields(self):
        response_json = [{
            'id': 'tags',
            'key': 'tags',
            'name': 'Tags',
            'schema': {'type': 'array', 'items': 'string'},
        }]
        url = 'https://st-api.test.yandex-team.ru/service/fields/'
        responses.add(responses.GET, url, json=response_json)

        client = get_startrek_client()
        fields = client.get_fields()

        expected = [{
            'id': 'tags',
            'key': 'tags',
            'name': 'Tags',
            'schema': {'type': 'array', 'items': 'string'},
        }]
        self.assertEqual(fields, expected)
        self.assertEqual(len(responses.calls), 1)
