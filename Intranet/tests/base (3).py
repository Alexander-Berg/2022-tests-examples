from django.test import TestCase
from django.db.models.signals import pre_save
from json import loads

from staff.lib.testing import TokenFactory


class BaseApiTestCase(TestCase):
    unsubscribe_passport = True

    def setUp(self):
        if self.unsubscribe_passport:
            pre_save.disconnect(dispatch_uid='babylon_staff_update_logger')
        self.token = TokenFactory(token='killa', ips='127.0.0.1', hostnames='localhost')
        if hasattr(self, 'prepare_fixtures'):
            self.prepare_fixtures()

    def get_json(self, url, fields=None, query=''):
        return loads(
            self.client.get(url + self._get_query(fields, query)).content
        )

    def get_page(self, url, fields=None, query=''):
        return self.client.get(url + self._get_query(fields, query))

    def _get_query(self, fields, query):
        if fields:
            _query = '?token=%s&fields=%s' % \
                (self.token.token, '|'.join(fields))
        else:
            _query = '?token=%s' % self.token.token
        if query:
            return _query + '&' + query
        return _query
