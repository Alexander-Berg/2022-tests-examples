# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
import string

from faker import Faker
from faker.providers import BaseProvider

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider

from infra.nanny.yp_lite_api.proto import endpoint_sets_pb2

from saas.library.python.nanny_proto import EndpointSet

fake = Faker()
fake.add_provider(CommonProvider)

COMMON_CLUSTERS = ['SAS', 'MAN', 'VLA', 'MAN_PRE']


class Provider(BaseProvider):
    pod_name_symbols = string.ascii_lowercase + string.digits

    def yp_lite_cluster(self):
        return self.random_element(COMMON_CLUSTERS)

    # noinspection PyMethodMayBeStatic
    def endpoint_set_id(self):
        return '-'.join(fake.words())

    def pod_id(self, service_name=None):
        if service_name:
            # if service name present assume enumerated replica pod_id
            return '{}-{}'.format(service_name.replace('_', '-'), self.random_int(1, 999))
        else:
            return fake.random_string(16)

    def pod_filter(self):
        filers = {}
        for _ in range(0, self.random_digit_not_null()):
            filers['/labels/{}'.format(self.random_letters())] = self.random_letters()
        result = []
        for k, v in six.iteritems(filers):
            operator = self.random_element(('=', '!='))  # https://yt.yandex-team.ru/docs/description/dynamic_tables/dyn_query_language.html#where
            result.append('[{}] {} {}'.format(k, operator, v))
        return ' and '.join(result)

    def endpoint_set(self, cluster=None, endpoint_set_id=None, service_id=None, pod_filter=''):
        cluster = cluster or self.yp_lite_cluster()
        endpoint_set_id = endpoint_set_id or self.endpoint_set_id()
        service_id = service_id or self.endpoint_set_id()  # TODO: change fake service id generator
        meta = endpoint_sets_pb2.EndpointSetMeta(id=endpoint_set_id, service_id=service_id, version=''.join(self.random_letters()))
        spec = endpoint_sets_pb2.EndpointSetSpec(pod_filter=pod_filter, protocol='tcp', port=80, description=fake.text())
        return EndpointSet(
            cluster, self.endpoint_set_id(),
            endpoint_sets_pb2.EndpointSet(meta=meta, spec=spec, status=endpoint_sets_pb2.EndpointSetStatus())
        )

    def yp_pod_hostname(self, cluster=None, enumerated_replica_name=None):
        cluster = cluster or self.yp_lite_cluster()
        if enumerated_replica_name:
            pod_base_name = '{}-{}'.format(enumerated_replica_name.replace('_', '-'), fake.pyint(min_value=1))
        else:
            pod_base_name = ''.join(fake.random.choice(self.pod_name_symbols) for _ in range(16))
        return '{}.{}.yp-c.yandex.net'.format(pod_base_name, cluster.lower())
