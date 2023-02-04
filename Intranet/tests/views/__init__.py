# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from core.tests import YauthTestCase


class BaseProfileTestCase(YauthTestCase):
    # Содержимое базы задается json-ом из external/src/app/fixtures/
    fixtures = ['test_views']
    url = None

    def _pre_setup(self):
        super(BaseProfileTestCase, self)._pre_setup()
        self.client = self.client_class(HTTP_X_FORWARDED_HOST='yandex.com')

    def _get_response(self):
        return self.client.get(self.url)

    def _get_response_context(self):
        return self._get_response().context

    def _post_response(self, data):
        return self.client.post(self.url, follow=True, data=data)
