# -*- coding: utf-8 -*-

import re

import json
import unittest
import contextlib

import six
import mock
from tvmauth import TicketStatus

from butils.tvm.ticket import ServiceTicket
from butils.tvm.exception import TvmException
from butils.tvm.api.tvmtool import TVMToolApi
from butils.http_message import HttpRequest, HttpResponse


# TODO: move to utils
@contextlib.contextmanager
def mock_http_call(return_value=None, side_effect=None):
    """
    :type return_value: any
    :type side_effect: func | list[any]
    :return: mock.MagicMock
    """
    target = "butils.post.run_http_call_via_requests"
    with mock.patch(
        target=target, return_value=return_value, side_effect=side_effect
    ) as http_call_mock:
        yield http_call_mock


# TODO: Move to base module
class BaseTestTVMApi(unittest.TestCase):
    def setUp(self):
        self._deepcopy_mock = mock.patch(
            "copy.deepcopy", side_effect=lambda obj, *args, **kwargs: obj
        )
        self._deepcopy_mock.start()

    def tearDown(self):
        self._deepcopy_mock.stop()


class TestTVMToolApi(BaseTestTVMApi):
    def setUp(self):
        super(TestTVMToolApi, self).setUp()

        self.access_token = "b026324c6904b2a9cb4b88d6d61c81d1"

        tvm_access_token_mock_path = (
            "butils.tvm.api.tvmtool.TVMToolApi._receive_access_token"
        )
        self._tvm_access_token_mock = mock.patch(
            tvm_access_token_mock_path, return_value=self.access_token
        )
        self._tvm_access_token_mock.start()

        self.url = "http://localhost:18303"

        self.api = TVMToolApi(url=self.url)

    def tearDown(self):
        super(TestTVMToolApi, self).tearDown()

    @staticmethod
    def response(status=200, body=None, headers=None):
        return HttpResponse(status=status, body=body, headers=headers)

    def test_retries(self):
        expected_keys = "1:keys"

        responses = [
            self.response(500),
            self.response(500),
            self.response(200, body=expected_keys),
        ]

        with mock_http_call(side_effect=responses) as m:
            keys = self.api.keys()
            self.assertEqual(keys, expected_keys)
            self.assertEqual(m.call_count, len(responses))

    def test_keys(self):
        expected_keys = "1:keys"
        with mock_http_call(self.response(200, expected_keys)) as m:
            keys = self.api.keys()
            self.assertEqual(expected_keys, keys)

            # TODO: move to self.assert_had_request
            request = m.call_args and m.call_args[0] and m.call_args[0][0]
            self.assertIsInstance(request, HttpRequest)

            self.assertEqual(request.method, "GET")
            self.assertEqual(request.uri, self.url + "/tvm/keys")
            self.assertEqual(request.headers.get("Authorization"), self.access_token)

            keys_uri = m.call_args and m.call_args[0] and m.call_args[0][1]
            self.assertEqual(six.text_type(keys_uri), self.url + "/tvm/keys")

    def test_tickets(self):
        src_id = 2001385
        dst_id = 2001574
        ticket = (
            "3:serv:CNomELX2o9sFIggI6ZN6EKaVeg:Nfp_IixawIvhZL6oMRZD2-oD"
            "pMeQ0_BUn412FSV0mT8eKN9YuQ0xDkRfH_FVCFkc858XlgekLAIM7JYuLx"
            "2AvbWNZmbBFvAd9iPPSTIOciHsPtNOwoBWvU2TAqNBl4-twb7LgsVJ22pW"
            "Ly58kofM8KkCz-r3aB1OUT2mgKt6-ZI"
        )

        body_dict = {"application-id": {"ticket": ticket, "tvm_id": dst_id}}
        body = json.dumps(body_dict)
        headers = {"Content-Type": "application/json", "Content-Length": len(body)}

        with mock_http_call(self.response(200, body, headers)):
            tickets = self.api.tickets(src_id, dst_id)

        expected_tickets = {dst_id: ticket}
        self.assertEqual(tickets, expected_tickets)

    def test_check_service_ticket(self):
        src_id = 2001385
        dst_id = 2001574
        ticket = (
            "3:serv:CNomELX2o9sFIggI6ZN6EKaVeg:Nfp_IixawIvhZL6oMRZD2-oD"
            "pMeQ0_BUn412FSV0mT8eKN9YuQ0xDkRfH_FVCFkc858XlgekLAIM7JYuLx"
            "2AvbWNZmbBFvAd9iPPSTIOciHsPtNOwoBWvU2TAqNBl4-twb7LgsVJ22pW"
            "Ly58kofM8KkCz-r3aB1OUT2mgKt6-ZI"
        )

        body_dict = {
            "src": src_id,
            "dst": dst_id,
            "scopes": None,
            "debug_string": ";".join(
                (
                    "ticket_type=service",
                    "expiration_time=1533606709",
                    "src=2001385",
                    "dst=2001574",
                    "scope=",
                )
            ),
            "logging_string": "3:serv:CNomELX2o9sFIggI6ZN6EKaVeg:",
        }
        body = json.dumps(body_dict)
        headers = {"Content-Type": "application/json", "Content-Length": len(body)}

        with mock_http_call(self.response(200, body, headers)):
            service_ticket = self.api.check_service_ticket(dst_id, ticket)

        expected_service_ticket = ServiceTicket(src=src_id)
        self.assertEqual(service_ticket, expected_service_ticket)

    def test_general_exceptions(self):
        def run(status, expected_exc, expected_msg):
            with mock_http_call(self.response(status, expected_msg)):
                exception_re = re.compile(expected_msg)
                self.assertRaisesRegexp(expected_exc, exception_re, self.api.keys)

        run(400, TvmException, "some parameter incorrect")
        run(401, TvmException, "Invalid authentication token")
        run(404, TvmException, "unknown tvmtool answer: 404 Not Found")

    def test_check_service_ticket_exceptions(self):
        def run(error_message, expected_status):
            content = {
                "error": error_message,
                "debug_string": "",
                "logging_string": "3:serv:xxx",
            }

            with mock_http_call(self.response(403, json.dumps(content))):
                try:
                    self.api.check_service_ticket(1, "ticket")
                except TvmException as e:
                    self.assertEqual(e.status, expected_status)
                else:
                    self.assertFalse("check_service_ticket was successful")

        run("expired ticket", TicketStatus.Expired)
        run("unsupported ticket version", TicketStatus.UnsupportedVersion)
        run("unsupported ticket type", TicketStatus.InvalidTicketType)
        run("invalid signature format", TicketStatus.SignBroken)
        run("Wrong ticket dst, expected 2, got 1", TicketStatus.InvalidDst)
        run("illegal base64 data at input byte 13", TicketStatus.Malformed)
        run("proto: can't skip unknown wire type 7", TicketStatus.Malformed)


if __name__ == "__main__":
    unittest.main()

# vim:ts=4:sts=4:sw=4:tw=79:et:
