# -*- coding: utf-8 -*-
import socket

from django.conf import settings
from django.test import TestCase
from django.test.utils import override_settings

from unittest.mock import patch, Mock

from events.balance.balance_client import BalanceClient


mocked_xmlrpc = Mock()
mocked_get_service_product = Mock()
mocked_get_service_product.GetServiceProduct = Mock(side_effect=socket.error)
mocked_xmlrpc.ServerProxy = Mock(return_value=mocked_get_service_product)


@override_settings(BALANCE_CLIENT_RETRY_LIMIT_COUNT=3)
class TestBalanceClient__make_request(TestCase):
    @patch('events.balance.balance_client.xmlrpc.client', mocked_xmlrpc)
    def test_should_retry_if_socket_error_raised(self):
        balance_client = BalanceClient(token=None)
        try:
            balance_client._make_request('GetServiceProduct', {})
            raised = False
        except socket.error:
            raised = True
        self.assertTrue(raised)
        msg = 'Если balance недоступен, то нужно попробовать сделать запрос несколько раз'
        self.assertEqual(
            balance_client.server.GetServiceProduct.call_count,
            settings.BALANCE_CLIENT_RETRY_LIMIT_COUNT + 1,
            msg=msg,
        )

    @patch('events.balance.balance_client.xmlrpc.client', Mock())
    def test_should_not_retry_if_socket_error_is_not_raised(self):
        balance_client = BalanceClient(token=None)
        try:
            balance_client._make_request('GetServiceProduct', {})
            raised = False
        except socket.error:
            raised = True
        self.assertFalse(raised)
        msg = 'Если balance доступен, то запрос должен быть один'
        self.assertEqual(
            balance_client.server.GetServiceProduct.call_count,
            1,
            msg=msg,
        )
