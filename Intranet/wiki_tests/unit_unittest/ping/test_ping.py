"""
ADMIN-4537
"""
from inspect import getmembers, isclass
from unittest import skipIf

from django.conf import settings

from wiki.ping.management.commands.ping import (
    CriticalDatabaseError,
    DatabaseError,
    PingException,
    WrongStatus,
    check_databases,
)
from wiki.ping.management.commands.ping import validate as ping_validate
from intranet.wiki.tests.wiki_tests.common.wiki_client import WikiClient
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class FakeResponse(object):
    status_code = 500
    text = ''


class PingTest(WikiDjangoTestCase):
    """
    Тест ручки /ping
    """

    client_class = WikiClient

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_get(self):
        response = self.client.get('/ping')

        self.assertEqual(200, response.status_code)

    def test_command_ping(self):
        fake_resp = FakeResponse()

        # incorrect http status
        self.assertRaises(WrongStatus, ping_validate, fake_resp)

        fake_resp.status_code = 200
        self.assertEqual(None, ping_validate(fake_resp))

    def test_exception_inheritance(self):
        """
        All ping module exceptions are subclasses of PingException
        """
        from wiki.ping.management.commands import ping, ping_celery

        filt = lambda value: isclass(value) and value is not PingException and issubclass(value, Exception)

        exceptions = getmembers(ping, filt)
        exceptions.extend(getmembers(ping_celery, filt))

        # all ping module exceptions are subclasses of PingException
        for _, exc_class in exceptions:
            self.assertTrue(issubclass(exc_class, PingException))

    def test_check_databases(self):
        ALIASES = ['default', 'slave']
        from wiki.ping.management.commands import ping

        _db_is_alive = ping.db_is_alive

        def db_is_alive(db_name, cache_seconds=0, number_of_tries=1, force=False):
            return db_name in ALIASES

        try:
            ping.db_is_alive = db_is_alive

            self.assertEqual(None, check_databases(ALIASES))

            self.assertRaises(CriticalDatabaseError, lambda: check_databases(['slow', 'slow2']))

            self.assertRaises(DatabaseError, lambda: check_databases(['default', 'slow']))
        finally:
            ping.db_is_alive = _db_is_alive
