# from infra.awacs.proto
from infra.awacs.proto import api_pb2, model_pb2, modules_pb2  # noqa
from maps.pylibs.infrastructure_api.awacs.awacs_api import AwacsApi


class MockAwacsApi:
    AWACS_ROBOT_LOGIN = AwacsApi.AWACS_ROBOT_LOGIN
    _prepare_backend_modification = AwacsApi._prepare_backend_modification
    _prepare_upstream_modification = AwacsApi._prepare_upstream_modification
    l7_balancer_service_names = AwacsApi.l7_balancer_service_names

    def __init__(self, fqdns, locations, pause):
        self._fqdns = fqdns
        self._balancers = {fqdn: [
            self._create_balancer(dc, fqdn, fqdn + dc in pause) for dc in locations[fqdn]
        ] for fqdn in self._fqdns}

        self._backends = {fqdn: [
            self._create_backend(dc, fqdn) for dc in locations[fqdn]
        ] for fqdn in self._fqdns}

        self._upstreams = {fqdn: [
            self._create_upstream('default', fqdn)
        ] for fqdn in self._fqdns}

    @staticmethod
    def _create_balancer(dc, namespace_id, paused=False):
        balancer = model_pb2.Balancer()
        balancer.meta.id = namespace_id + dc
        balancer.meta.namespace_id = namespace_id
        balancer.meta.transport_paused.value = paused
        balancer.meta.location.yp_cluster = dc
        balancer.spec.yandex_balancer.yaml = f'balancer content for {namespace_id} balancer'
        balancer.spec.yandex_balancer.mode = model_pb2.YandexBalancerSpec.ConfigMode.EASY_MODE
        balancer.status.active.status = 'True'
        return balancer

    @staticmethod
    def _create_backend(dc, namespace_id):
        backend = model_pb2.Backend()
        backend.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
        backend.spec.selector.port.CopyFrom(AwacsApi.port(None))
        yp_endpoint_set = backend.spec.selector.yp_endpoint_sets.add()
        yp_endpoint_set.endpoint_set_id = 'endpoint_id'
        yp_endpoint_set.cluster = dc.lower()
        backend.spec.selector.use_mtn = True

        backend.meta.id = namespace_id + dc
        backend.meta.namespace_id = namespace_id
        backend.statuses.add().active['fqdn'].status = "True"

        return backend

    @staticmethod
    def _create_upstream(upstream_id, namespace_id):
        upstream = model_pb2.Upstream()
        upstream.meta.id = upstream_id
        upstream.meta.namespace_id = namespace_id
        upstream.spec.yandex_balancer.yaml = f'content for {upstream_id} upstream of {namespace_id}'
        upstream.spec.labels.update({'order': str(5000)})
        upstream.statuses.add().active['fqdn'].status = "True"
        return upstream

    def balancers(self, fqdn):
        response = api_pb2.ListBalancersResponse()
        response.balancers.extend(self._balancers[fqdn])
        return response

    def backends(self, namespace_id):
        response = api_pb2.ListBackendsResponse()
        response.backends.extend(self._backends[namespace_id])
        return response

    def upstreams(self, namespace_id):
        response = api_pb2.ListUpstreamsResponse()
        response.upstreams.extend(self._upstreams[namespace_id])
        return response

    @classmethod
    def upstream_order(cls, upstream):
        return upstream.spec.labels.get('order')

    @classmethod
    def upstream_labels(cls, upstream):
        return upstream.spec.labels

    @classmethod
    def backend_labels(cls, backend):
        return backend.spec.labels

    def namespace(self, namespace_id):
        response = api_pb2.GetNamespaceResponse()
        response.namespace.meta.abc_service_id = 100
        return response.namespace

    def update_balancer(self, balancer, yaml_config, pause_action=AwacsApi.PauseAction.SKIP,
                        commit_message=None, pause_author=None):
        for b in self._balancers[balancer.meta.namespace_id]:
            if balancer.meta.id != b.meta.id:
                continue
            b.meta.comment = commit_message if commit_message else ''
            if pause_action != AwacsApi.PauseAction.SKIP:
                b.meta.transport_paused.comment = commit_message
                b.meta.transport_paused.value = (pause_action == AwacsApi.PauseAction.PAUSE)
                b.meta.transport_paused.author = pause_author or 'SEDEM'
            b.status.active.status = 'False'
            if yaml_config:
                b.spec.yandex_balancer.yaml = yaml_config

    def update_backend(self, *, auth, backend, selector, comment, labels):
        for b in self._backends[backend.meta.namespace_id]:
            b.spec.labels.update(backend.spec.labels)
            if backend.meta.id != b.meta.id:
                continue

            b.spec.labels.update(backend.spec.labels)
            b.meta.version = backend.meta.version
            b.statuses.add().active['fqdn'].status = "False"
            self._prepare_backend_modification(b, backend.meta.namespace_id, auth,
                                               backend.meta.id, selector, comment, labels)

    def update_upstream(self, *, auth, upstream, yaml, comment=None, order=None, labels=None):
        for u in self._upstreams[upstream.meta.namespace_id]:
            if u.meta.id != upstream.meta.id:
                continue
            u.spec.labels.update(upstream.spec.labels)
            u.meta.version = upstream.meta.version
            u.statuses.add().active['fqdn'].status = "False"
            self._prepare_upstream_modification(u, upstream.meta.namespace_id, auth,
                                                upstream.meta.id, yaml, comment, order, labels)


mock_awacs = None


def get_mock_awacs(token: str = None, dry_run: bool = None, reset: bool = False):
    return mock_awacs


def initialize(fqdns, locations, pause):
    global mock_awacs
    mock_awacs = MockAwacsApi(fqdns=fqdns, locations=locations, pause=pause)
    return mock_awacs
