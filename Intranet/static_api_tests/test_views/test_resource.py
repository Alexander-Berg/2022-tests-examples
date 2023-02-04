# coding: utf-8
from __future__ import unicode_literals

import json

from django.conf import settings
from django.core.urlresolvers import reverse
from django.test.client import RequestFactory

from mock.mock import MagicMock

from static_api import storage
from static_api_tests.base import MongoTestCase
from static_api.views.resource import resource_view


class ViewsTest(MongoTestCase):
    def setUp(self):
        super(ViewsTest, self).setUp()
        persons_collection = storage.manager.db['person']
        persons = [
            {'login': 'tester', 'uid': '1'},
            {'login': 'tester2', 'uid': '2'},
        ]
        persons_collection.insert_many(persons)

        access_rules_collection = storage.manager.db['fields_access_rules']
        access_rules = [
            {
                'subject_id': '1',
                'resource': 'person',
                'subject_type': 'user',
                'field': 'name.first',
                'idm_role_id': '1',
            },
            {
                'subject_id': '2',
                'resource': 'person',
                'subject_type': 'user',
                'field': 'name.first',
                'idm_role_id': '2',
            }
        ]
        access_rules_collection.insert_many(access_rules)
        settings.STATIC_API_CHECK_FIELDS_ACCESS = True
        settings.CACHES = {'default': {'BACKEND': 'django.core.cache.backends.locmem.LocMemCache'}}

    def test_resource_view_checks_allowed_fields(self):
        url = reverse('static_api:resource', kwargs={'resource': 'person'})
        request = RequestFactory().get(url, data={'_fields': 'name.first'})
        request.yauser = MagicMock()
        request.yauser.uid = '1'
        request.yauser.service_ticket = None

        res = resource_view(request, 'person')

        assert res.status_code == 200
        assert json.loads(res.content) == {
            'links': {},
            'page': 1,
            'limit': 50,
            'result': [{}, {}],
            'total': 2,
            'pages': 1,
        }

    def test_resource_view_checks_forbidden_fields(self):
        url = reverse('static_api:resource', kwargs={'resource': 'person'})
        request = RequestFactory().get(url, data={'_fields': 'name.last'})
        request.yauser = MagicMock()
        request.yauser.uid = '2'
        request.yauser.service_ticket = None

        res = resource_view(request, 'person')

        assert res.status_code == 403
        assert json.loads(res.content) == {
            'error_message': 'Fields access forbidden. Refer to %s' % settings.STATIC_API_FIELDS_FORBIDDEN_URL,
            'role_request_url': settings.IDM_ROLE_REQUEST_URL_TEMPLATE % {'resource': 'person'},
            'details': {
                '_fields': [
                    'Forbidden field `name.last.ru`',
                    'Forbidden field `name.last.en`',
                    'Forbidden field `name.last`',
                ],
            },
        }
