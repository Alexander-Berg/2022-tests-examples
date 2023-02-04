from unittest.mock import MagicMock

from django.test import Client, SimpleTestCase
from django.core.exceptions import PermissionDenied

from intranet.magiclinks.src.middleware import YauthMiddleware
from intranet.magiclinks.src.common_views import ping
from intranet.magiclinks.src.links.views import LinksView


class YauthMiddlewareTestCase(SimpleTestCase):
    def setUp(self):
        self.client = Client()
        self.middleware = YauthMiddleware()
        self.request = MagicMock()
        self.request.yauser = None

    def test_yauth_check_exempt_success(self):
        self.assertIsNone(self.middleware.process_view(self.request, ping, None, None))

    def test_yauth_check_exempt_fail(self):
        self.assertRaises(PermissionDenied, self.middleware.process_view,
                          self.request, LinksView, None, None,
                          )
