# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import copy
import mock
import unittest
import yatest.common

from faker import Faker

from yp_proto.yp.client.api.proto import autogen_pb2 as yp_client_api_pb2
from yt_proto.yt.core.ytree.proto import attributes_pb2
from infra.nanny.yp_lite_api.proto import pod_sets_api_pb2

from saas.library.python.nanny_proto import Pod
from saas.library.python.nanny_proto.pod import YpLiteUIPodSetsServiceStub

from saas.library.python.nanny_proto.tests.fake import YpLiteProvider, COMMON_CLUSTERS

fake = Faker()
fake.add_provider(YpLiteProvider)


@mock.patch.object(Pod, '_CLIENT_CLASS', autospec=True)
@mock.patch.object(Pod, '_API_STUB', autospec=True)
@mock.patch.object(Pod, '_CLIENT', spec=YpLiteUIPodSetsServiceStub)
class TestEndpointSet(unittest.TestCase):

    # @classmethod
    # def setUpClass(cls):
    #     pass

    # def setUp(self):
    #     pass

    @staticmethod
    def proto_pod_generator(labels=None):
        labels = labels if labels is not None else fake.pydict(5, True, str)
        if 'nanny_version' not in labels.keys():
            labels['nanny_version'] = fake.pystr(8, 64)
        if 'nanny_service_id' not in labels.keys():
            labels['nanny_service_id'] = fake.pystr(8, 64)
        labels_list = [attributes_pb2.TAttribute(key=k.encode(), value=v.encode()) for k, v in labels.items()]
        return yp_client_api_pb2.TPod(labels=attributes_pb2.TAttributeDictionary(attributes=labels_list), meta=yp_client_api_pb2.TPodMeta(id=fake.pod_id()))

    def test_from_fqdn(self, client, *args):
        test_cluster = fake.yp_lite_cluster()
        test_id = fake.pod_id()
        test_fqdn = '{}.{}.yp-c.yandex.net'.format(test_id, test_cluster.lower().replace('_', '-'))

        pod = Pod.from_fqdn(test_fqdn)

        self.assertEqual(pod.id, test_id)
        self.assertEqual(pod.cluster, test_cluster)

        test_id2 = fake.pod_id('saas_yp_test-service')
        test_fqdn2 = '{}.{}.yp-c.yandex.net'.format(test_id2, test_cluster.lower().replace('_', '-'))

        pod2 = Pod.from_fqdn(test_fqdn2)
        self.assertEqual(pod2.id, test_id2)
        self.assertEqual(pod2.cluster, test_cluster)

        test_pod_ids = []
        with open(yatest.common.source_path('saas/library/python/nanny_proto/tests/data/pod_ids.txt')) as test_data:
            for tid in test_data:
                if tid.isspace():
                    continue
                test_pod_ids.append(tid.strip())

        self.assertGreater(len(test_pod_ids), 0)

        for test_pod_id in test_pod_ids:
            for test_cluster in COMMON_CLUSTERS:
                pod = Pod.from_fqdn('{}.{}.yp-c.yandex.net'.format(test_pod_id, test_cluster.lower().replace('_', '-')))
                self.assertEqual(pod.id, test_pod_id)
                self.assertEqual(pod.cluster, test_cluster)
        client.assert_not_called()

    def test_list(self, client, *args):
        test_pods = [self.proto_pod_generator(), self.proto_pod_generator()]
        test_ids = [p.meta.id for p in test_pods]
        test_cluster = fake.yp_lite_cluster()
        test_service = 'test_service'

        client.list_pods = mock.Mock(return_value=pod_sets_api_pb2.ListPodsResponse(pods=test_pods, total=2))

        result = Pod.list(test_cluster, test_service)
        result_ids = [p.id for p in result]
        self.assertListEqual(test_ids, result_ids)

        client.list_pods.assert_called_once_with(pod_sets_api_pb2.ListPodsRequest(service_id=test_service, cluster=test_cluster, limit=500))

    def test_labels(self, client, *args):
        labels = fake.pydict(5, True, str)

        test_pod_proto = self.proto_pod_generator(labels=labels)
        test_cluster = fake.yp_lite_cluster()

        client.get_pod = mock.Mock(return_value=pod_sets_api_pb2.GetPodResponse(pod=test_pod_proto))

        pod = Pod(test_cluster, test_pod_proto.meta.id)

        self.assertDictEqual(labels, pod.labels)
        client.get_pod.assert_called_once_with(pod_sets_api_pb2.GetPodRequest(pod_id=test_pod_proto.meta.id, cluster=test_cluster))

    def test_labels_set(self, client, *args):
        test_cluster = fake.yp_lite_cluster()
        test_pod_proto = self.proto_pod_generator()

        test_pod = Pod(test_cluster, test_pod_proto.meta.id, test_pod_proto)
        original_pod_version = test_pod.version

        new_labels = fake.pydict(5, True, str)

        get_labels = copy.deepcopy(new_labels)
        get_labels['nanny_version'] = fake.pystr(64, 128)
        get_labels['nanny_service_id'] = test_pod.service_id

        self.assertNotEqual(original_pod_version, get_labels['nanny_version'])

        client.update_pod = mock.Mock(return_value=pod_sets_api_pb2.UpdatePodResponse())
        client.get_pod = mock.Mock(return_value=pod_sets_api_pb2.GetPodResponse(pod=self.proto_pod_generator(labels=get_labels)))

        test_pod.labels = new_labels

        self.assertDictEqual(get_labels, test_pod.labels)

        new_labels['nanny_service_id'] = test_pod.service_id

        client.update_pod.assert_called_once_with(pod_sets_api_pb2.UpdatePodRequest(pod_id=test_pod_proto.meta.id, version=original_pod_version, labels=new_labels, cluster=test_cluster))
        client.get_pod.assert_called_once_with(pod_sets_api_pb2.GetPodRequest(pod_id=test_pod_proto.meta.id, cluster=test_cluster))

    def test_labels_update(self, client, *args):
        client.update_pod = mock.Mock(return_value=pod_sets_api_pb2.UpdatePodResponse())

        test_cluster = fake.yp_lite_cluster()
        labels = {'test_key1': 'test_value1', 'test_key2': 'test_value2', 'test_key_3': 'test_value3', 'nanny_service_id': 'test_service'}

        test_pod_proto = self.proto_pod_generator(labels=labels)
        test_pod = Pod(test_cluster, test_pod_proto.meta.id, test_pod_proto)
        version = test_pod.version

        test_pod.update_labels(test_key2='updated_test_value2', test_key_3=None)

        client.update_pod.assert_called_once_with(pod_sets_api_pb2.UpdatePodRequest(
            pod_id=test_pod_proto.meta.id, version=version, cluster=test_cluster,
            labels={'test_key1': 'test_value1', 'test_key2': 'updated_test_value2', 'nanny_service_id': 'test_service'}
        ))
        client.get_pod.assert_called_once_with(pod_sets_api_pb2.GetPodRequest(pod_id=test_pod_proto.meta.id, cluster=test_cluster))

    def test_getters(self, client, *args):
        test_cluster = fake.yp_lite_cluster()
        test_service = fake.pystr(16, 64)
        test_version = fake.pystr(64, 128)

        labels = fake.pydict(5, True, str)
        labels['nanny_service_id'] = test_service
        labels['nanny_version'] = test_version
        test_pod_proto = self.proto_pod_generator(labels=labels)
        client.get_pod = mock.Mock(return_value=pod_sets_api_pb2.GetPodResponse(pod=test_pod_proto))

        pod = Pod(test_cluster, test_pod_proto.meta.id)
        self.assertEqual(pod.service_id, test_service)
        self.assertEqual(pod.cluster, test_cluster)
        self.assertEqual(pod.version, test_version)
