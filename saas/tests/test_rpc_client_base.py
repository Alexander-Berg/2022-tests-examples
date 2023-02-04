# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import mock
import unittest

from infra.nanny.yp_lite_api.py_stubs.endpoint_sets_api_stub import YpLiteUIEndpointSetsServiceStub

import saas.library.python.nanny_proto.rpc_client_base as rpc_client_base


class TestSingletonWithRpcClient(unittest.TestCase):

    @mock.patch.object(rpc_client_base.PersistentTokenStore, 'get_token_from_store_env_or_file')
    def test_client_initialize(self, m):

        class TestClient(rpc_client_base.NannyRpcClientBase):
            _OAUTH_SLUG = 'test'
            _RPC_URL = 'http://test'
            _API_STUB = YpLiteUIEndpointSetsServiceStub

        instance = TestClient()
        self.assertIsInstance(instance._CLIENT, YpLiteUIEndpointSetsServiceStub)
        self.assertIsNone(rpc_client_base.NannyRpcClientBase._CLIENT)
        m.assert_called_once_with(TestClient._OAUTH_SLUG)

    @mock.patch.object(rpc_client_base.PersistentTokenStore, 'get_token_from_store_env_or_file')
    def test_singleton_identity(self, m):
        class TestClientWithIdentity(rpc_client_base.NannyRpcClientBase):
            _identity = ['first', 'second']
            _OAUTH_SLUG = 'test'
            _RPC_URL = 'http://test'
            _API_STUB = YpLiteUIEndpointSetsServiceStub

            def __init__(self, first, second, third):
                super(TestClientWithIdentity, self).__init__()
                self.first = first
                self.second = second
                self.third = third

        instance = TestClientWithIdentity('a', 'b', 1)
        same_instance = TestClientWithIdentity('a', 'b', 2)
        other_instance = TestClientWithIdentity('a', 'c', 3)

        self.assertIs(instance, same_instance)
        self.assertIsNot(instance, other_instance)

        self.assertIsInstance(instance._CLIENT, YpLiteUIEndpointSetsServiceStub)
        self.assertIs(instance._CLIENT, other_instance._CLIENT)
        self.assertIs(instance.LOGGER, other_instance.LOGGER)
        m.assert_called_once_with(TestClientWithIdentity._OAUTH_SLUG)

    @mock.patch.object(rpc_client_base.PersistentTokenStore, 'get_token_from_store_env_or_file')
    def test_different_classes(self, m):
        class TestClientOne(rpc_client_base.NannyRpcClientBase):
            _identity = ['first', 'second']
            _OAUTH_SLUG = 'test1'
            _RPC_URL = 'http://test'
            _API_STUB = YpLiteUIEndpointSetsServiceStub

            def __init__(self, first, second, third):
                super(TestClientOne, self).__init__()
                self.first = first
                self.second = second
                self.third = third

        class TestClientTwo(rpc_client_base.NannyRpcClientBase):
            _identity = ['first', 'second']
            _OAUTH_SLUG = 'test1'
            _RPC_URL = 'http://test'
            _API_STUB = YpLiteUIEndpointSetsServiceStub

            def __init__(self, first, second, third):
                super(TestClientTwo, self).__init__()
                self.first = first
                self.second = second
                self.third = third

        instance1 = TestClientOne('a', 'b', 1)
        m.assert_called_once_with(TestClientOne._OAUTH_SLUG)
        m.reset_mock()

        instance2 = TestClientTwo('a', 'b', 1)
        m.assert_called_once_with(TestClientTwo._OAUTH_SLUG)

        self.assertIsNot(instance1._CLIENT, instance2._CLIENT)
        self.assertIsNot(instance1.LOGGER, instance2.LOGGER)
