import copy

import inject
import mock
import pytest
import six
import time
import yaml
from sepelib.core import config as appconfig
from six.moves import range

from . import test_balancers
from awacs.lib.nannyrpcclient import INannyRpcClient
from awacs.lib.rpc import exceptions
from awacs.lib.rpc.exceptions import BadRequestError
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model import util
from infra.awacs.proto import api_pb2, model_pb2
from awacs.web import backend_service, balancer_service, validation
from awacs.web.validation.backend import validate_spec
from awtest.mocks.yp_lite_client import YpLiteMockClient
from nanny_repo import repo_pb2, repo_api_pb2
from awtest.api import call, create_namespace_with_order_in_progress, create_namespace, set_login_to_root_users, \
    fill_object_upper_limits
from awtest.core import wait_until, wait_until_passes


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client):
    def configure(b):
        b.bind(IYpLiteRpcClient, YpLiteMockClient())
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


_YAML_CONFIG_1 = {
    'errorlog': {
        'log_level': 'DEBUG',
        'log': '/tmp/log.txt',
    },
}
YAML_CONFIG_1 = yaml.dump(_YAML_CONFIG_1)

_YAML_CONFIG_2 = copy.deepcopy(_YAML_CONFIG_1)
_YAML_CONFIG_2['errorlog']['log_level'] = 'ERROR'
YAML_CONFIG_2 = yaml.dump(_YAML_CONFIG_2)

NAMESPACE = 'test_balancer'
LOGIN = 'login'


def create_balancer(namespace_id, id, yp_cluster=None):
    appconfig.set_value('run.root_users', [util.NANNY_ROBOT_LOGIN])
    req = api_pb2.CreateBalancerRequest()
    req.meta.id = id
    req.meta.namespace_id = namespace_id
    req.meta.category = 'users/romanovich'
    req.meta.auth.type = req.meta.auth.STAFF
    req.meta.location.type = req.meta.location.YP_CLUSTER
    req.meta.location.yp_cluster = yp_cluster or 'SAS'
    req.spec.config_transport.nanny_static_file.service_id = 'prod_balancer'
    req.spec.type = model_pb2.YANDEX_BALANCER
    req.spec.yandex_balancer.yaml = test_balancers.YAML_CONFIG_1
    return call(balancer_service.create_balancer, req, util.NANNY_ROBOT_LOGIN).balancer


def test_forbidden_operations_during_namespace_order(zk_storage, cache, enable_auth):
    # allow creation and modification, but forbid removal
    create_namespace_with_order_in_progress(zk_storage, cache, 'ns')
    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = 'b'
    req_pb.meta.namespace_id = 'ns'
    req_pb.meta.comment = 'c'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.SetInParent()
    with mock.patch.object(validation.backend, 'validate_request'):
        resp_pb = call(backend_service.create_backend, req_pb, LOGIN)

    req_pb = api_pb2.UpdateBackendRequest()
    req_pb.meta.namespace_id = 'ns'
    req_pb.meta.id = 'b'
    req_pb.meta.version = resp_pb.backend.meta.version
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend(('second',))
    with mock.patch.object(validation.backend, 'validate_request'):
        resp_pb = call(backend_service.update_backend, req_pb, LOGIN)

    req_pb = api_pb2.RemoveBackendRequest(namespace_id='ns', id='b', version=resp_pb.backend.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(backend_service.remove_backend, req_pb, LOGIN)


@pytest.mark.parametrize('max_count,custom_count', [
    (0, None),
    (1, None),
    (10, None),
    (5, 10),
    (10, 5),
])
def test_namespace_objects_total_limit(max_count, custom_count, create_default_namespace):
    namespace_id = 'ns'
    appconfig.set_value('common_objects_limits.backend', max_count)
    create_default_namespace(namespace_id)
    create_balancer(namespace_id, 'ns_sas')
    if custom_count is not None:
        fill_object_upper_limits(namespace_id, 'backend', custom_count, LOGIN)
    count = custom_count or max_count

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = 'b'
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.comment = 'c'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.selector.type = req_pb.spec.selector.YP_ENDPOINT_SETS_SD
    req_pb.spec.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='pumpurum')
    for _ in range(count):
        call(backend_service.create_backend, req_pb, LOGIN)
        req_pb.meta.id += 'a'

    def check():
        list_req_pb = api_pb2.ListBackendsRequest(namespace_id=namespace_id)
        assert call(backend_service.list_backends, list_req_pb, LOGIN).total == count

    wait_until_passes(check)

    with pytest.raises(exceptions.BadRequestError,
                       match='Exceeded limit of backends in the namespace: {}'.format(count)):
        call(backend_service.create_backend, req_pb, LOGIN)


def test_create_get_delete_backend(mongo_storage, create_default_namespace):
    login = 'morrison'
    backend_id = 'backend-1'
    namespace_id = NAMESPACE
    balancer_id = namespace_id + '_sas'
    create_default_namespace(namespace_id)
    create_balancer(namespace_id, balancer_id)

    comment = 'Creating very important backend'

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.SetInParent()  # TODO
    with pytest.raises(exceptions.BadRequestError) as e:
        with mock.patch.object(validation.backend, 'validate_request',
                               side_effect=BadRequestError('BAD')):
            call(backend_service.create_backend, req_pb, login)
    assert six.text_type(e.value) == 'BAD'

    req_pb.meta.comment = comment
    with mock.patch.object(validation.backend, 'validate_request'):
        resp_pb = call(backend_service.create_backend, req_pb, login)

    backend_pb = resp_pb.backend
    assert backend_pb.meta.id == backend_id
    assert backend_pb.meta.namespace_id == namespace_id
    assert backend_pb.meta.author == login
    assert backend_pb.meta.comment == comment

    def check_backend():
        req_pb = api_pb2.GetBackendRequest(namespace_id=namespace_id, id=backend_id)
        resp_pb = call(backend_service.get_backend, req_pb, login)
        backend_pb = resp_pb.backend
        assert backend_pb.meta.comment == comment
        assert len(backend_pb.statuses) == 1
        r_status = backend_pb.statuses[-1]
        assert r_status.id == backend_pb.meta.version
        assert not r_status.validated and not r_status.in_progress and not r_status.active
        return r_status

    r_status = wait_until(check_backend, timeout=1)

    rev_pb = mongo_storage.must_get_backend_rev(r_status.id)
    assert rev_pb.meta.id == r_status.id
    assert rev_pb.spec == backend_pb.spec

    req_pb = api_pb2.RemoveBackendRequest(namespace_id=namespace_id, id=backend_id, version=r_status.id)
    call(backend_service.remove_backend, req_pb, login)

    def check():
        req_pb = api_pb2.GetBackendRequest(namespace_id=namespace_id, id=backend_id)
        backend_pb = call(backend_service.get_backend, req_pb, login).backend
        assert backend_pb.spec.deleted
        assert backend_pb.meta.comment == 'Marked as deleted by {}'.format(login)

    wait_until_passes(check)


def test_backend_spec_validation():
    namespace_id = 'ns_id'
    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = spec_pb.selector.YP_ENDPOINT_SETS_SD
    es_pb = spec_pb.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='pumpurum')

    es_pb.port.policy = es_pb.port.OVERRIDE
    es_pb.port.override = 10101
    with pytest.raises(BadRequestError) as e:
        validate_spec(spec_pb, namespace_id=namespace_id)
    e.match(r'"spec.selector.yp_endpoint_sets\[0\].port": must be set to KEEP -- '
            'YP_ENDPOINT_SETS_SD do not support port overriding')

    es_pb.port.policy = es_pb.port.KEEP
    es_pb.weight.policy = es_pb.weight.OVERRIDE
    es_pb.weight.override = 10101
    with pytest.raises(BadRequestError) as e:
        validate_spec(spec_pb, namespace_id=namespace_id)
    e.match(r'"spec.selector.yp_endpoint_sets\[0\].weight": must be set to KEEP -- '
            'YP_ENDPOINT_SETS_SD do not support weight overriding')

    es_pb.weight.policy = es_pb.weight.KEEP
    validate_spec(spec_pb, namespace_id=namespace_id)

    es_pb.ClearField('port')
    es_pb.ClearField('weight')
    validate_spec(spec_pb, namespace_id=namespace_id)

    spec_pb.selector.port.policy = spec_pb.selector.port.OVERRIDE
    spec_pb.selector.port.override = 10101
    with pytest.raises(BadRequestError) as e:
        validate_spec(spec_pb, namespace_id=namespace_id)
    e.match('"spec.selector.port": must be set to KEEP -- '
            'YP_ENDPOINT_SETS_SD do not support port overriding')


def test_update_backend(mongo_storage, dao, zk_storage, cache, create_default_namespace):
    login = 'morrison'
    namespace_id = NAMESPACE
    balancer_id = namespace_id + '_sas'
    backend_id = 'backend-1'

    create_default_namespace(namespace_id)
    create_balancer(namespace_id, balancer_id)

    comment = 'Updating very important backend'

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    with mock.patch.object(validation.backend, 'validate_request'):
        resp_pb = call(backend_service.create_backend, req_pb, login)
    backend_pb = resp_pb.backend
    assert backend_pb.meta.id == backend_id
    assert backend_pb.meta.namespace_id == namespace_id
    initial_version = backend_pb.meta.version
    assert mongo_storage.list_backend_revs(namespace_id=namespace_id, backend_id=backend_id).total == 1

    req_pb = api_pb2.UpdateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.version = 'xxx'
    req_pb.spec.labels['hello'] = '1'

    with pytest.raises(exceptions.ConflictError, match='Backend modification conflict'):
        with mock.patch.object(validation.backend, 'validate_request'):
            call(backend_service.update_backend, req_pb, login)
    assert mongo_storage.list_backend_revs(namespace_id=namespace_id, backend_id=backend_id).total == 1

    req_pb.meta.version = backend_pb.meta.version
    req_pb.meta.comment = comment
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    req_pb.validate_yp_endpoint_sets = True

    with pytest.raises(exceptions.BadRequestError, match='All endpoint sets in backend "backend-1" are empty'):
        with mock.patch.object(validation.backend, 'validate_request'):
            call(backend_service.update_backend, req_pb, login)

    req_pb.validate_yp_endpoint_sets = False
    with mock.patch.object(validation.backend, 'validate_request'):
        call(backend_service.update_backend, req_pb, login)

    def check_backend():
        req_pb = api_pb2.GetBackendRequest(namespace_id=namespace_id, id=backend_id)
        resp_pb = call(backend_service.get_backend, req_pb, login)
        backend_pb = resp_pb.backend
        assert backend_pb.meta.version != initial_version
        assert backend_pb.meta.comment == comment
        assert mongo_storage.list_backend_revs(namespace_id=namespace_id, backend_id=backend_id).total == 2
        assert not backend_pb.spec.selector.HasField('port')
        assert len(backend_pb.statuses) == 1
        assert [r.id for r in backend_pb.statuses] == [backend_pb.meta.version]
        r_status_pb = backend_pb.statuses[-1]
        assert r_status_pb.id == backend_pb.meta.version
        assert not r_status_pb.validated and not r_status_pb.in_progress and not r_status_pb.active
        return backend_pb, r_status_pb

    backend_pb, r_status_pb = wait_until(check_backend, timeout=1)

    rev_pb = mongo_storage.must_get_backend_rev(r_status_pb.id)
    assert rev_pb.meta.id == r_status_pb.id
    assert rev_pb.meta.comment == comment
    assert rev_pb.spec == backend_pb.spec

    req_pb.meta.version = backend_pb.meta.version
    req_pb.meta.comment = comment
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    req_pb.meta.comment = u'changed using awacsctl'

    with pytest.raises(exceptions.BadRequestError) as e:
        with mock.patch.object(validation.backend, 'validate_request'):
            call(backend_service.update_backend, req_pb, login)
    e.match(u'awacsctl would change YP_ENDPOINT_SETS_SD to YP_ENDPOINT_SETS, please see SWAT-6425 for details')

    req_pb.spec.selector.type = model_pb2.BackendSelector.NANNY_SNAPSHOTS
    req_pb.spec.selector.nanny_snapshots.add(service_id='x', snapshot_id='y')
    req_pb.meta.comment = u''
    with pytest.raises(exceptions.BadRequestError) as e:
        call(backend_service.update_backend, req_pb, login)
    assert (six.text_type(e.value) == u'"spec.selector.nanny_snapshots[0].snapshot_id" must be not specified, '
                                      u'see SWAT-6604 for details')

    req_pb.spec.selector.nanny_snapshots[0].ClearField('snapshot_id')
    backend_pb = call(backend_service.update_backend, req_pb, login).backend

    req_pb.meta.version = backend_pb.meta.version
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    req_pb.spec.selector.yp_endpoint_sets.add(
        endpoint_set_id='test',
        cluster='sas'
    )
    backend_pb = call(backend_service.update_backend, req_pb, login).backend

    dao.create_default_name_servers()
    dns_record_pb = model_pb2.DnsRecord()
    dns_record_pb.meta.namespace_id = namespace_id
    dns_record_pb.meta.id = 'test'
    dns_record_pb.spec.address.zone = 'aaa'
    dns_record_pb.spec.name_server.namespace_id = 'infra'
    dns_record_pb.spec.name_server.id = 'in.yandex.net'
    dns_record_pb.spec.address.backends.backends.add(id=req_pb.meta.id, namespace_id=req_pb.meta.namespace_id)
    zk_storage.create_dns_record(namespace_id, 'test', dns_record_pb)
    wait_until_passes(lambda: cache.must_get_dns_record(namespace_id, 'test'))
    dns_state_pb = model_pb2.DnsRecordState()
    dns_state_pb.namespace_id = namespace_id
    dns_state_pb.dns_record_id = 'test'
    dns_state_pb.backends[req_pb.meta.id].statuses.add()
    zk_storage.create_dns_record_state(namespace_id, 'test', dns_state_pb)
    wait_until_passes(lambda: cache.must_get_dns_record_state(namespace_id, 'test'))

    req_pb.meta.version = backend_pb.meta.version
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    with pytest.raises(exceptions.BadRequestError) as e:
        with mock.patch.object(validation.backend, 'validate_request'):
            call(backend_service.update_backend, req_pb, login)
    e.match(u'Cannot set YP_ENDPOINT_SETS_SD type for backends that are used in DNS records: "test_balancer:test"')


@mock.patch.object(validation.backend, 'validate_request')
def test_global_and_system_backend(mongo_storage, dao, zk_storage, cache):
    login = 'morrison'
    namespace_id = NAMESPACE
    backend_id = 'backend-1'
    create_namespace(zk_storage, cache, namespace_id)

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.is_global.value = True
    req_pb.meta.is_system.value = True
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS

    set_login_to_root_users(login)

    with pytest.raises(exceptions.ForbiddenError,
                       match='"meta.is_system,spec.is_global": global backend cannot be system at the same time'):
        call(backend_service.create_backend, req_pb, login)


@mock.patch.object(validation.backend, 'validate_request')
def test_global_backend(mongo_storage, dao, zk_storage, cache):
    namespace_id = NAMESPACE
    backend_id = 'backend-1'
    create_namespace(zk_storage, cache, namespace_id)

    comment = 'Updating very important backend'

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.is_global.value = True
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS

    with pytest.raises(exceptions.ForbiddenError,
                       match='"spec.is_global": global backend can only be created by roots'):
        call(backend_service.create_backend, req_pb, LOGIN, enable_auth=True)

    req_pb.spec.is_global.value = False

    with mock.patch.object(validation.util, 'validate_yp_endpoint_sets'):
        resp_pb = call(backend_service.create_backend, req_pb, LOGIN)
    backend_pb = resp_pb.backend
    assert backend_pb.meta.id == backend_id
    assert backend_pb.meta.namespace_id == namespace_id

    req_pb = api_pb2.UpdateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.version = backend_pb.meta.version
    req_pb.meta.comment = comment
    req_pb.spec.is_global.value = True

    with pytest.raises(exceptions.ForbiddenError, match='"spec.is_global" can only be changed by roots'):
        call(backend_service.update_backend, req_pb, LOGIN)

    set_login_to_root_users(LOGIN)
    for b_pb in zk_storage.update_backend(namespace_id, backend_id):
        b_pb.meta.is_system.value = True
    wait_until(lambda: cache.must_get_backend(namespace_id, backend_id).meta.is_system.value, timeout=1)

    with pytest.raises(exceptions.ForbiddenError, match='System backend cannot be modified'):
        call(backend_service.update_backend, req_pb, LOGIN)

    for b_pb in zk_storage.update_backend(namespace_id, backend_id):
        b_pb.meta.is_system.value = False
    wait_until(lambda: not cache.must_get_backend(namespace_id, backend_id).meta.is_system.value, timeout=1)

    backend_pb = call(backend_service.update_backend, req_pb, LOGIN).backend

    req_pb.meta.version = backend_pb.meta.version
    req_pb.spec.is_global.value = False
    with pytest.raises(exceptions.ForbiddenError, match='"spec.is_global" cannot be changed from True to False'):
        call(backend_service.update_backend, req_pb, LOGIN)


def test_system_balancers_backend(mongo_storage, dao, zk_storage, cache):
    namespace_id = NAMESPACE
    backend_id = 'backend-1'
    balancer_id = 'sas.some'
    create_namespace(zk_storage, cache, namespace_id)
    balancer_pb = create_balancer(namespace_id, balancer_id)

    comment = 'Updating very important backend'

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.is_system.value = True
    req_pb.spec.selector.type = model_pb2.BackendSelector.BALANCERS
    req_pb.spec.selector.balancers.add(id=balancer_id)

    with pytest.raises(exceptions.ForbiddenError,
                       match='"meta.is_system": system backend can only be created by roots'):
        call(backend_service.create_backend, req_pb, LOGIN, enable_auth=True)

    req_pb.meta.is_system.value = False

    with pytest.raises(exceptions.BadRequestError,
                       match='"meta.is_system.value" must be set to "true" for BALANCERS backend'):
        call(backend_service.create_backend, req_pb, LOGIN)

    req_pb.meta.is_system.value = True
    set_login_to_root_users(LOGIN)

    resp_pb = call(backend_service.create_backend, req_pb, LOGIN)
    backend_pb = resp_pb.backend
    assert backend_pb.meta.id == backend_id
    assert backend_pb.meta.namespace_id == namespace_id

    req_pb = api_pb2.UpdateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.version = backend_pb.meta.version
    req_pb.meta.comment = comment
    req_pb.meta.version = backend_pb.meta.version
    req_pb.meta.is_system.value = False
    with pytest.raises(exceptions.ForbiddenError, match='System backend cannot be modified'):
        call(backend_service.update_backend, req_pb, LOGIN)

    req_pb = api_pb2.ListBackendsRequest()
    service_id = balancer_pb.spec.config_transport.nanny_static_file.service_id
    req_pb.query.yp_endpoint_set_full_id_in.add(cluster=balancer_pb.meta.location.yp_cluster.lower(),
                                                id=util.make_system_endpoint_set_id(service_id))

    def check():
        response = call(backend_service.list_backends, req_pb, LOGIN)
        assert len(response.backends) == 1
        assert [b.meta.id for b in response.backends] == [backend_id]

    wait_until_passes(check, timeout=1)


@mock.patch.object(validation.backend, 'validate_request')
def test_list_backends_and_revisions(_1, create_default_namespace, cache):
    login = 'morrison'
    namespace_id = NAMESPACE
    balancer_id = namespace_id + '_sas'
    create_default_namespace(namespace_id)
    create_balancer(namespace_id, balancer_id)

    ids = ['a', 'b', 'c', 'd']
    backends = {}
    for id in ids:
        req_pb = api_pb2.CreateBackendRequest()
        req_pb.meta.id = id
        req_pb.meta.namespace_id = namespace_id
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.spec.SetInParent()
        backends[id] = call(backend_service.create_backend, req_pb, login).backend

    es_backends = {
        'x': [{'cluster': 'sas', 'id': 'es1'}, {'cluster': 'sas', 'id': 'es2'}],
        'y': [{'cluster': 'sas', 'id': 'es1'}]
    }
    es_ids = sorted(es_backends.keys())
    ids.extend(es_ids)
    for id in es_ids:
        req_pb = api_pb2.CreateBackendRequest()
        req_pb.meta.id = id
        req_pb.meta.namespace_id = namespace_id
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
        for es in es_backends[id]:
            yp_es = req_pb.spec.selector.yp_endpoint_sets.add()
            yp_es.cluster = es['cluster']
            yp_es.endpoint_set_id = es['id']
        backends[id] = call(backend_service.create_backend, req_pb, login).backend

    def check_backend():
        req_pb = api_pb2.ListBackendsRequest(namespace_id=namespace_id)
        response = call(backend_service.list_backends, req_pb, login)
        assert len(response.backends) == len(ids)
        assert [b.meta.id for b in response.backends] == sorted(ids)
        return req_pb

    req_pb = wait_until(check_backend, timeout=1)

    req_pb.skip = 1
    response = call(backend_service.list_backends, req_pb, login)
    assert len(response.backends) == len(ids) - 1
    assert [b.meta.id for b in response.backends] == ids[1:]

    req_pb.skip = 1
    req_pb.limit = 2
    response = call(backend_service.list_backends, req_pb, login)
    assert len(response.backends) == 2
    assert [b.meta.id for b in response.backends] == ids[1:3]

    req_pb.skip = 100
    req_pb.limit = 0
    response = call(backend_service.list_backends, req_pb, login)
    assert len(response.backends) == 0

    req_pb.skip = 0
    req_pb.limit = 0
    req_pb.namespace_id = ''
    req_pb.query.yp_endpoint_set_full_id_in.add(cluster='sas', id='es2')
    response = call(backend_service.list_backends, req_pb, login)
    assert len(response.backends) == 1
    assert response.backends[0].meta.id == 'x'

    es = req_pb.query.yp_endpoint_set_full_id_in.add(cluster='sas', id='es1')
    response = call(backend_service.list_backends, req_pb, login)
    assert set(b.meta.id for b in response.backends) == set(es_ids)

    for _ in range(3):
        req_pb = api_pb2.UpdateBackendRequest()
        req_pb.meta.id = 'a'
        req_pb.meta.namespace_id = namespace_id
        req_pb.meta.version = backends['a'].meta.version
        req_pb.spec.CopyFrom(backends['a'].spec)
        req_pb.spec.labels['test'] = req_pb.spec.labels.get('test', '') + 't'
        backends['a'] = call(backend_service.update_backend, req_pb, login).backend

    req_pb = api_pb2.ListBackendRevisionsRequest(id='a', namespace_id=namespace_id)
    resp_pb = call(backend_service.list_backend_revisions, req_pb, login)
    assert resp_pb.total == 4
    assert set(rev.meta.namespace_id for rev in resp_pb.revisions) == {namespace_id}
    assert len(resp_pb.revisions) == 4
    assert resp_pb.revisions[0].meta.id == backends['a'].meta.version

    req_pb = api_pb2.ListBackendRevisionsRequest(id='a', namespace_id=namespace_id, skip=2)
    resp_pb = call(backend_service.list_backend_revisions, req_pb, login)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 2

    req_pb = api_pb2.ListBackendRevisionsRequest(id='a', namespace_id=namespace_id, skip=2, limit=1)
    resp_pb = call(backend_service.list_backend_revisions, req_pb, login)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 1


@mock.patch.object(validation.backend, 'validate_request')
def test_get_backend_revision(_, cache, create_default_namespace):
    login = 'morrison'
    namespace_id = NAMESPACE
    balancer_id = namespace_id + '_sas'
    create_default_namespace(namespace_id)
    create_balancer(namespace_id, balancer_id)

    backend_id_to_pb = {}
    ids = ['a', 'b', 'c', 'd']
    for id in ids:
        req_pb = api_pb2.CreateBackendRequest()
        req_pb.meta.id = id
        req_pb.meta.namespace_id = namespace_id
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.spec.labels['test'] = req_pb.spec.labels.get('test', '') + 't'

        b = call(backend_service.create_backend, req_pb, login).backend
        backend_id_to_pb[b.meta.id] = b

    def check_revision():
        req_pb = api_pb2.GetBackendRevisionRequest(id=backend_id_to_pb['a'].meta.version)
        resp_pb = call(backend_service.get_backend_revision, req_pb, login)
        assert resp_pb.revision.spec == backend_id_to_pb['a'].spec

    wait_until_passes(check_revision)


@mock.patch('awacs.lib.nannyrpcclient.INannyRpcClient')
def test_validate_services_policy(mock_nanny_rpc_client):
    def mock_get_replication_policy(service_id):
        policy = repo_pb2.ReplicationPolicy()
        if service_id == 'move':
            policy.spec.replication_method = repo_pb2.ReplicationPolicySpec.MOVE
        elif service_id == 'timeout':
            policy.spec.replication_method = repo_pb2.ReplicationPolicySpec.MOVE
            time.sleep(100)
        else:
            policy.spec.replication_method = repo_pb2.ReplicationPolicySpec.REPLACE
        return repo_api_pb2.GetReplicationPolicyResponse(policy=policy)

    mock_nanny_rpc_client.get_replication_policy.side_effect = mock_get_replication_policy

    def configure_injector(binder):
        binder.bind(INannyRpcClient, mock_nanny_rpc_client)

    inject.clear_and_configure(configure_injector)

    # Check move policy is forbidden
    with pytest.raises(Exception) as e:
        validation.util.validate_services_policy(['replace', 'move'])
    msg = (
        "Service move has MOVE replication policy and can not be served by a backend with "
        "NANNY_SNAPSHOTS selector type. "
        "Please see https://wiki.yandex-team.ru/cplb/awacs/awacs-backends-restrictions/ "
        "for details.")
    assert six.text_type(e.value) == msg
    # Check replace policy is ok
    validation.util.validate_services_policy(['replace-1', 'replace-2'])
    # Check Nanny API timeout is not blocking
    with mock.patch.object(validation.util, 'NANNY_WAIT_TIMEOUT', 0.1):
        validation.util.validate_services_policy(['timeout', 'replace'])


def test_removal_checks(create_default_namespace):
    namespace_id = NAMESPACE
    backend_id = 'xxx'
    set_login_to_root_users(LOGIN)
    create_default_namespace(namespace_id)

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS
    req_pb.spec.selector.yp_endpoint_sets.add(
        endpoint_set_id='test',
        cluster='sas'
    )
    call(backend_service.create_backend, req_pb, LOGIN)

    req_pb = api_pb2.GetBackendRemovalChecksRequest(namespace_id=namespace_id, id=backend_id)
    resp_pb = call(backend_service.get_backend_removal_checks, req_pb, LOGIN)
    balancer_usage_check, l3_balancer_usage_check, dns_record_usage_check = resp_pb.checks
    assert balancer_usage_check.state == balancer_usage_check.PASSED
    assert balancer_usage_check.message == 'Backend is not used in balancers'
    assert l3_balancer_usage_check.state == l3_balancer_usage_check.PASSED
    assert l3_balancer_usage_check.message == 'Backend is not used in L3 balancers'
    assert dns_record_usage_check.state == dns_record_usage_check.PASSED
    assert dns_record_usage_check.message == 'Backend is not used in DNS records'
