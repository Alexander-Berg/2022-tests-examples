# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import mock
import unittest

from faker import Faker

from infra.nanny.yp_lite_api.proto import endpoint_sets_pb2
from infra.nanny.yp_lite_api.proto import endpoint_sets_api_pb2

from saas.library.python.token_store import PersistentTokenStore
from saas.library.python.nanny_proto import EndpointSet
from saas.library.python.nanny_proto.endpoint_set import YpLiteUIEndpointSetsServiceStub
from saas.library.python.nanny_proto.tests.fake.fake import Provider as YpLiteProvider

fake = Faker()
fake.add_provider(YpLiteProvider)


@mock.patch.object(EndpointSet, '_CLIENT_CLASS', autospec=True)
@mock.patch.object(EndpointSet, '_API_STUB', autospec=True)
class TestEndpointSet(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        PersistentTokenStore.add_token('nanny', 'test_token')

    # def setUp(self):
    #     pass

    def test_client_configuration(self, api_stub, client_class_stub):
        EndpointSet._init_client()

        client_class_stub.assert_called_once_with(
            rpc_url='https://yp-lite-ui.nanny.yandex-team.ru/api/yplite/endpoint-sets/', oauth_token='test_token', request_timeout=30, retry_connection_errors=True
        )
        api_stub.assert_called_once()

        self.assertIsInstance(EndpointSet._CLIENT, YpLiteUIEndpointSetsServiceStub)

    @mock.patch.object(EndpointSet, '_CLIENT', spec=YpLiteUIEndpointSetsServiceStub)
    def test_getters(self, client_stub, client_class_stub, api_stub):
        endpoint_set = EndpointSet('man-pre', fake.endpoint_set_id())
        version = 'test_version'
        service_id = 'test_service_id'
        pod_filter = '[/labels/test_label] = "test value"'
        client_stub.get_endpoint_set = mock.Mock(
            return_value=endpoint_sets_api_pb2.GetEndpointSetResponse(
                endpoint_set=endpoint_sets_pb2.EndpointSet(
                    meta=endpoint_sets_pb2.EndpointSetMeta(id=endpoint_set.id, service_id=service_id, version=version),
                    spec=endpoint_sets_pb2.EndpointSetSpec(pod_filter=pod_filter, protocol='tcp', port=80, description=fake.text())
                )
            )
        )
        self.assertEqual(endpoint_set.pod_filter, pod_filter)
        self.assertEqual(endpoint_set.service_id, service_id)
        self.assertEqual(endpoint_set.version, version)
        client_stub.get_endpoint_set.assert_called_once_with(endpoint_sets_api_pb2.GetEndpointSetRequest(id=endpoint_set.id, cluster='MAN_PRE'))

    @mock.patch.object(EndpointSet, '_CLIENT', spec=YpLiteUIEndpointSetsServiceStub)
    def test_create_one(self, client_stub, client_class_stub, api_stub):
        test_cluster = fake.yp_lite_cluster()
        test_service_id = '-'.join(fake.words())
        test_es_id = fake.endpoint_set_id()
        test_pod_filter = fake.pod_filter()

        def mock_create_endpoint_set(req):
            return endpoint_sets_api_pb2.CreateEndpointSetResponse(endpoint_set=endpoint_sets_pb2.EndpointSet(meta=req.meta, spec=req.spec, status=endpoint_sets_pb2.EndpointSetStatus()))

        client_stub.create_endpoint_set = mock.MagicMock(side_effect=mock_create_endpoint_set)
        client_stub.get_endpoint_set = mock.MagicMock()

        es = EndpointSet.create([test_cluster, ], test_service_id, test_es_id, pod_filter=test_pod_filter)[0]

        self.assertIsInstance(es, EndpointSet)
        self.assertEqual(es.cluster, test_cluster)
        self.assertEqual(es.service_id, test_service_id)
        self.assertEqual(es.id, test_es_id)
        self.assertEqual(es.pod_filter, test_pod_filter)

        client_stub.create_endpoint_set.assert_called_once()
        client_stub.get_endpoint_set.assert_not_called()

        client_class_stub.assert_not_called()
        api_stub.assert_not_called()

    @mock.patch.object(EndpointSet, '_CLIENT', spec=YpLiteUIEndpointSetsServiceStub)
    def test_search_empty(self, client_stub, client_class_stub, api_stub):
        search_request = fake.word()
        test_cluster = fake.yp_lite_cluster()
        empty_search_result = endpoint_sets_api_pb2.SearchEndpointSetsResponse()

        client_stub.search_endpoint_sets = mock.MagicMock(return_value=empty_search_result)

        result = EndpointSet.search(test_cluster, search_request)
        self.assertListEqual(result, [])

        client_class_stub.assert_not_called()
        api_stub.assert_not_called()
        client_stub.search_endpoint_sets.assert_called_once_with(endpoint_sets_api_pb2.SearchEndpointSetsRequest(substring=search_request, limit=100, cluster=test_cluster))

    @mock.patch.object(EndpointSet, '_CLIENT', spec=YpLiteUIEndpointSetsServiceStub)
    def test_search_results(self, client_stub, client_class_stub, api_stub):
        def search_endpoint_sets_response(count=1):
            res_list = []
            for x in range(0, count):
                res_list.append(endpoint_sets_api_pb2.EndpointSetRef(id=fake.endpoint_set_id()))
            return endpoint_sets_api_pb2.SearchEndpointSetsResponse(endpoint_sets=res_list)

        search_request = fake.word()
        test_cluster = fake.yp_lite_cluster()

        client_stub.search_endpoint_sets = mock.MagicMock(return_value=search_endpoint_sets_response())
        client_stub.get_endpoint_set = mock.MagicMock()

        result = EndpointSet.search(test_cluster, search_request)

        client_stub.search_endpoint_sets.assert_called_once_with(endpoint_sets_api_pb2.SearchEndpointSetsRequest(substring=search_request, limit=100, cluster=test_cluster))
        for es in result:
            self.assertIsInstance(es, EndpointSet)
            self.assertEqual(es.cluster, test_cluster)

        client_stub.get_endpoint_set.assert_not_called()
        client_class_stub.assert_not_called()
        api_stub.assert_not_called()

    @mock.patch.object(EndpointSet, '_CLIENT', spec=YpLiteUIEndpointSetsServiceStub)
    def test_update_pod_filter(self, client_stub, client_class_stub, api_stub):
        test_service_id = '-'.join(fake.words())

        def mock_update_endpoint_set(req):
            meta = endpoint_sets_pb2.EndpointSetMeta(id=req.id, service_id=test_service_id, version=''.join(fake.random_letters()))
            spec = endpoint_sets_pb2.EndpointSetSpec(pod_filter=req.spec.pod_filter, protocol=req.spec.protocol, port=req.spec.port, description=req.spec.description)
            return endpoint_sets_api_pb2.UpdateEndpointSetResponse(endpoint_set=endpoint_sets_pb2.EndpointSet(meta=meta, spec=spec, status=endpoint_sets_pb2.EndpointSetStatus()))

        endpoint_set = fake.endpoint_set()  # type: EndpointSet
        self.assertIsInstance(endpoint_set._proto_msg, endpoint_sets_pb2.EndpointSet)
        self.assertIsInstance(endpoint_set._proto_msg.spec, endpoint_sets_pb2.EndpointSetSpec)
        self.assertIsInstance(endpoint_set._proto_msg.meta, endpoint_sets_pb2.EndpointSetMeta)

        test_pod_filter = fake.pod_filter()

        client_stub.update_endpoint_set = mock.MagicMock(side_effect=mock_update_endpoint_set)
        client_stub.get_endpoint_set = mock.MagicMock()

        endpoint_set.pod_filter = test_pod_filter

        self.assertEqual(endpoint_set.pod_filter, test_pod_filter)

        client_stub.update_endpoint_set.assert_called_once()
        client_stub.get_endpoint_set.assert_not_called()
        client_class_stub.assert_not_called()
        api_stub.assert_not_called()

    @mock.patch.object(EndpointSet, '_CLIENT', spec=YpLiteUIEndpointSetsServiceStub)
    def test_remove(self, client_stub, client_class_stub, api_stub):
        endpoint_set = fake.endpoint_set()  # type: EndpointSet

        client_stub.remove_endpoint_set = mock.MagicMock()

        endpoint_set.remove()

        client_stub.remove_endpoint_set.assert_called_once_with(
            endpoint_sets_api_pb2.RemoveEndpointSetRequest(id=endpoint_set.id, version=endpoint_set.version, cluster=endpoint_set.cluster)
        )
