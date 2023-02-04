# -*- coding: utf-8 -*-
import json
import responses

from django.conf import settings
from django.test import TestCase

from events.accounts.factories import UserFactory, GroupFactory
from events.idm import utils
from events.idm.client import IdmClient


class TestIdmClient(TestCase):
    @responses.activate
    def test_should_request_roles(self):
        responses.add(
            responses.POST,
            'https://idm-api.yandex-team.ru/api/v1/batchrolerequest/',
            json={
                'id': '9c13c96b-df84-4901-a33f-5e6eb5bf383b',
            },
            status=201,
        )
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        fields_data = {'object_pk': '123'}

        client = IdmClient()
        client.request_roles(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=[it.username for it in users],
            groups=[utils.get_group_id(it.name) for it in groups],
            fields_data=fields_data,
        )

        self.assertEqual(len(responses.calls), 1)
        body = json.loads(responses.calls[0].request.body.decode())
        self.assertTrue('requests' in body)
        self.assertEqual(len(body['requests']), 4)
        self.assertDictEqual(body['requests'][0], {
            'system': settings.IDM_SYSTEM,
            'path': f'/{settings.ROLE_FORM_MANAGER}/',
            'fields_data': fields_data,
            'user': users[0].username,
            'label': f'Request role for user {users[0].username}',
        })
        self.assertDictEqual(body['requests'][1], {
            'system': settings.IDM_SYSTEM,
            'path': f'/{settings.ROLE_FORM_MANAGER}/',
            'fields_data': fields_data,
            'user': users[1].username,
            'label': f'Request role for user {users[1].username}',
        })
        self.assertDictEqual(body['requests'][2], {
            'system': settings.IDM_SYSTEM,
            'path': f'/{settings.ROLE_FORM_MANAGER}/',
            'fields_data': fields_data,
            'group': utils.get_group_id(groups[0].name),
            'label': f'Request role for group {utils.get_group_id(groups[0].name)}',
        })
        self.assertDictEqual(body['requests'][3], {
            'system': settings.IDM_SYSTEM,
            'path': f'/{settings.ROLE_FORM_MANAGER}/',
            'fields_data': fields_data,
            'group': utils.get_group_id(groups[1].name),
            'label': f'Request role for group {utils.get_group_id(groups[1].name)}',
        })

    @responses.activate
    def test_shouldnt_request_roles(self):
        responses.add(
            responses.POST,
            'https://idm-api.yandex-team.ru/api/v1/batchrolerequest/',
            json={},
            status=201,
        )
        fields_data = {'object_pk': '123'}

        client = IdmClient()
        client.request_roles(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=[],
            groups=[],
            fields_data=fields_data,
        )
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_should_reject_roles(self):
        responses.add(
            responses.DELETE,
            'https://idm-api.yandex-team.ru/api/v1/roles/',
            json={
                'errors': 0,
                'errors_ids': [],
                'successes': 4,
                'successes_ids': [
                    {'id': 101},
                    {'id': 102},
                    {'id': 103},
                    {'id': 104},
                ],
            },
            status=201,
        )
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        fields_data = {'object_pk': '123'}

        client = IdmClient()
        client.reject_roles(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=[it.username for it in users],
            groups=[utils.get_group_id(it.name) for it in groups],
            fields_data=fields_data,
        )

        self.assertEqual(len(responses.calls), 1)
        body = json.loads(responses.calls[0].request.body.decode())
        self.assertDictEqual(body, {
            'system': settings.IDM_SYSTEM,
            'path': f'/{settings.ROLE_FORM_MANAGER}/',
            'fields_data': json.dumps(fields_data),
            'user': ','.join(it.username for it in users),
            'group': ','.join(str(utils.get_group_id(it.name)) for it in groups),
        })

    @responses.activate
    def test_shouldnt_reject_roles(self):
        responses.add(
            responses.DELETE,
            'https://idm-api.yandex-team.ru/api/v1/roles/',
            json={},
            status=201,
        )
        fields_data = {'object_pk': '123'}

        client = IdmClient()
        client.reject_roles(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=[],
            groups=[],
            fields_data=fields_data,
        )
        self.assertEqual(len(responses.calls), 0)
