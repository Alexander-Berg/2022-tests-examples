
from importlib import import_module

from django.conf import settings
from django.contrib.messages.storage import default_storage
from django.core.cache import cache
from django.http import HttpRequest

from wiki.pages.api import save_page
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class BaseOldActionTestCase(BaseTestCase):
    """
    Base test case for tests of wiki.actions.classes.base.BaseAction's children.
    """


class HttpOldActionTestCase(BaseOldActionTestCase):
    """
    Base test case for tests of wiki.actions.classes.base.HttpAction's children.
    Provides some useful methods.
    """

    action_name = None

    def __init__(self, *args, **kwargs):
        super(HttpOldActionTestCase, self).__init__(*args, **kwargs)
        if self.action_name is None:
            class_name = self.__class__.__name__
            if class_name[-10:] == 'ActionTest':
                replaced = 'ActionTest'
            elif class_name[-8:] == 'TestCase':
                replaced = 'TestCase'
            else:
                replaced = 'Test'
            self.action_name = class_name.replace(replaced, '').lower()

    def setUp(self):
        super(HttpOldActionTestCase, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic
        self.page = self.create_page(tag='Тест', authors_to_add=[self.user], status=3)
        self.request = self.build_request()

    def tearDown(self):
        super(HttpOldActionTestCase, self).tearDown()
        cache.clear()

    def build_request(self, user=None, page=None):
        user = user or self.user
        page = page or self.page

        # Create dummy HttpRequest object
        request = HttpRequest()
        request.page = page
        request.user = user
        request.user_auth = None
        request.from_yandex_server = False
        engine = import_module(settings.SESSION_ENGINE)
        request.session = engine.SessionStore('test_session')
        request._messages = default_storage(request)
        request.LANGUAGE_CODE = user.staff.lang_ui

        # Save page to generate WOM to make handler .show working
        save_page(page, '{{%s}}' % self.action_name, self.action_name, request=request)

        return request

    def get(self, params=None):
        return self._request(params)

    def post(self, params=None, follow=True, supertag=None):
        return self._request(params, method='post', follow=follow, supertag=supertag)

    def _request(self, params=None, method='get', follow=True, supertag=None):
        if not params:
            params = {}
        if '__count__' not in params:
            params['__count__'] = 1

        if '__url__' not in params:
            if supertag:
                params['__url__'] = 'https://{0}/{1}'.format(
                    settings.API_WIKI_HOST,
                    supertag,
                )
                params['for'] = supertag
            else:
                params['__url__'] = self.request.page.absolute_url

        return getattr(self.client, method)(
            '/_api/frontend/.actions/' + self.action_name, params, follow=follow, HTTP_HOST=settings.API_WIKI_HOST
        )


class OldHttpActionTestCase(HttpOldActionTestCase):
    def _request(self, params=None, method='get', follow=True, supertag=None):
        if not params:
            params = {}
        if '__count__' not in params:
            params['__count__'] = 1

        if '__url__' not in params:
            params['__url__'] = 'http://%s/%s' % (settings.API_WIKI_HOST, supertag or self.request.page.supertag)

        return getattr(self.client, method)(
            '/_api/frontend/.actions/' + self.action_name, params, follow=follow, HTTP_HOST=settings.API_WIKI_HOST
        )
