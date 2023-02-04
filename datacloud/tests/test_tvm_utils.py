# -*- coding: utf-8 -*-
import os
import time
import responses
import unittest
import mock
from datacloud.dev_utils.tvm import tvm_utils
from datacloud.dev_utils.tvm.for_tests.fake_tvm_context import get_fake_service_context


TEST_TICKET_VAL = 'test-ticket-val'
TEST_TVM_SECRET = 'test-tvm-secret'
EXPECTED_TVM_KEYS = '1:CpkBCpQBCJYqEAAahgEwgYMCgYB2Fm4NHpZOItaocXT4aWBx5s'


class TestTVMTicket(unittest.TestCase):
    def test_create_ticket(self):
        ticket = tvm_utils.TVMTicket(TEST_TICKET_VAL)
        self.assertEqual(ticket.value, TEST_TICKET_VAL)
        self.assertEqual(str(ticket), TEST_TICKET_VAL)

    def test_valid_ticket(self):
        ticket = tvm_utils.TVMTicket(TEST_TICKET_VAL, int(time.time()) + 10)
        self.assertTrue(ticket.is_valid())

    def test_invalid_ticket(self):
        ticket = tvm_utils.TVMTicket(TEST_TICKET_VAL, int(time.time() - 1))
        self.assertFalse(ticket.is_valid())


class TestTVMManager(unittest.TestCase):
    def test_create_tvm_manager_with_secret(self):
        manager = tvm_utils.TVMManager(secret=TEST_TVM_SECRET)
        self.assertEqual(manager._secret, TEST_TVM_SECRET)

    def test_create_tvm_manager_with_env_varaible(self):
        os.environ['TVM_SECRET'] = TEST_TVM_SECRET
        manager = tvm_utils.TVMManager()
        self.assertEqual(manager._secret, TEST_TVM_SECRET)

    @responses.activate
    def test_request_tvm_keys(self):
        responses.add(responses.GET, tvm_utils.TVM_KEY_REQUEST_URL, body=EXPECTED_TVM_KEYS, status=200)
        manager = tvm_utils.TVMManager(secret=TEST_TVM_SECRET)
        tvm_keys = manager._request_tvm_keys()
        self.assertEqual(tvm_keys, EXPECTED_TVM_KEYS)

    @responses.activate
    @mock.patch('datacloud.dev_utils.tvm.tvm_utils.TVMManager._get_service_context', side_effect=get_fake_service_context)
    def test_tvm_get_ticket(self, urandom_function):
        EXPECTED_TVM_TICKET = u'3:serv:CPoqEM2-gN0FIggIiJp6EImOeg:I3WYb-3_bxvqF_rfEL8'
        responses.add(responses.GET, tvm_utils.TVM_KEY_REQUEST_URL, body=EXPECTED_TVM_KEYS, status=200)
        responses.add(responses.POST, tvm_utils.TVM_TICKET_REQUEST_URL, json={u'222': {u'ticket': EXPECTED_TVM_TICKET}}, status=200)
        manager = tvm_utils.TVMManager(secret=TEST_TVM_SECRET)
        ticket = manager.get_ticket(tvm_src=111, tvm_dst=222)
        self.assertEqual(ticket.value, EXPECTED_TVM_TICKET)

    @responses.activate
    @mock.patch('datacloud.dev_utils.tvm.tvm_utils.TVMManager._get_service_context', side_effect=get_fake_service_context)
    def test_tvm_get_ticket_fail(self, urandom_function):
        responses.add(responses.GET, tvm_utils.TVM_KEY_REQUEST_URL, body=EXPECTED_TVM_KEYS, status=200)
        responses.add(responses.POST, tvm_utils.TVM_TICKET_REQUEST_URL, json={}, status=400)
        manager = tvm_utils.TVMManager(secret=TEST_TVM_SECRET)
        ticket = manager.get_ticket(tvm_src=111, tvm_dst=222)
        self.assertEqual(ticket, None)
