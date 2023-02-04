from inspect import isclass, getmembers

import pytest

from django.core.urlresolvers import reverse
from django.test import TransactionTestCase

from plan.ping.management.commands.ping import (
    validate,
    WrongStatus,
    PingException,
)


class PingTest(TransactionTestCase):
    """
    Тест ручки /ping
    """

    @pytest.fixture(autouse=True)
    def patch_client(self, client):
        self.client = client

    def test_get(self):
        response = self.client.get(reverse('ping'))
        self.assertEqual(200, response.status_code)

    def test_command(self):
        class FakeResponse(object):
            status_code = 500
            text = ''

        fake_resp = FakeResponse()

        self.assertRaises(WrongStatus, validate, fake_resp)

        fake_resp.status_code = 200
        self.assertEqual(None, validate(fake_resp))

    def test_exception_inheritance(self):
        from plan.ping.management.commands import ping

        def filt(value):
            return (
                isclass(value) and
                value is not PingException and
                issubclass(value, Exception)
            )

        exceptions = getmembers(ping, filt)

        # all ping module exceptions are subclasses of PingException
        for _, exc_class in exceptions:
            self.assertTrue(issubclass(exc_class, PingException))
