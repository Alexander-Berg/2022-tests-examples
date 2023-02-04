
import pytz
from mock import patch
from pretend import call, call_recorder, stub

from wiki.middleware.timezone import select_timezone
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TimezoneMiddlewareTestCase(BaseApiTestCase):
    def test_anonymous_user(self):
        user = stub(is_authenticated=False)
        self.assertEqual('Europe/Paris1', select_timezone(user, 'Europe/Paris1'))

    def test_user_without_tz(self):
        profile = stub(tz='', uid='1')
        user = stub(is_authenticated=True, staff=profile)
        self.assertEqual('Europe/Paris1', select_timezone(user, 'Europe/Paris1'))

    def test_user(self):
        profile = stub(tz='Europe/Berlin1')
        user = stub(is_authenticated=True, staff=profile)
        self.assertEqual('Europe/Berlin1', select_timezone(user, 'Europe/Berlin1'))

    @patch('wiki.middleware.timezone.select_timezone', lambda *args: 'Europe/Moscow')
    def test_middleware(self):
        from django.utils.timezone import activate

        request = stub(user=None)
        from wiki.middleware.timezone import TimezoneMiddleware

        called_func = call_recorder(activate)
        with patch('wiki.middleware.timezone.timezone.activate', called_func):
            middleware = TimezoneMiddleware()
            middleware.process_request(request)

        self.assertEqual(called_func.calls, [call(pytz.timezone('Europe/Moscow'))])
