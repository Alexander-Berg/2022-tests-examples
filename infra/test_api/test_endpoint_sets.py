# coding: utf-8

import inject
import mock
import pytest
import six

from awacs.lib.rpc import exceptions
from awacs.model import util
from infra.awacs.proto import api_pb2
from awacs.web import endpoint_set_service, backend_service
from infra.swatlib.auth import abc
from awtest.api import call, create_namespace, set_login_to_root_users
from awtest.core import wait_until, wait_until_passes


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_backend(cache, namespace_id, backend_id, is_system=False, is_global=False):
    """
    :param cache:
    :type namespace_id: six.text_type
    :type backend_id: six.text_type
    :type is_system: bool
    :type is_global: bool
    """
    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.is_system.value = is_system
    req_pb.spec.is_global.value = is_global
    req_pb.spec.selector.type = req_pb.spec.selector.MANUAL
    set_login_to_root_users(util.NANNY_ROBOT_LOGIN)
    resp_pb = call(backend_service.create_backend, req_pb, util.NANNY_ROBOT_LOGIN)
    wait_until_passes(lambda: cache.must_get_backend(namespace_id, backend_id))
    return resp_pb.backend.meta.version


def test_create_endpoint_set(zk_storage, cache):
    login = 'ferenets'
    es_id = 'es_id'
    namespace_id = 'ns_id'
    backend_id = es_id

    create_namespace(zk_storage, cache, namespace_id)
    version = create_backend(cache, namespace_id, backend_id)

    req_pb = api_pb2.CreateEndpointSetRequest()
    req_pb.meta.id = es_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == '"backend_version" must be set'

    req_pb.backend_version = version
    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == '"spec" must be set'

    req_pb.spec.deleted = True
    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == '"spec.deleted" must not be set'

    req_pb.spec.deleted = False
    req_pb.spec.instances.add(host='y.ru')
    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == '"spec.instances[0].port" must be set'

    req_pb.spec.instances[0].port = 8080
    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == ('"spec.instances[0].weight" must be set and must not be 0. '
                                      'Please use -1 instead of 0 to assign zero weight to the instance')

    req_pb.spec.instances[0].weight = -1
    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == '"spec.instances[0].ipv6_addr" must be set'

    req_pb.spec.instances[0].ipv6_addr = '127.0.0.1'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == '"spec.instances[0].ipv6_addr": is not a valid IPv6 address'

    req_pb.spec.instances[0].ipv6_addr = '::1'
    req_pb.spec.instances.extend([req_pb.spec.instances[0]])
    with pytest.raises(exceptions.BadRequestError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == '"spec.instances[1]": duplicate host and port "y.ru:8080"'

    del req_pb.spec.instances[1]
    call(endpoint_set_service.create_endpoint_set, req_pb, login)

    with pytest.raises(exceptions.ConflictError) as e:
        call(endpoint_set_service.create_endpoint_set, req_pb, login)
    assert six.text_type(e.value) == 'Endpoint set "es_id" already exists in namespace "ns_id".'


def test_global_es(zk_storage, cache):
    login = 'morrison'
    namespace_id = 'ns_id'
    backend_id = 'backend-1'
    create_namespace(zk_storage, cache, namespace_id)
    version = create_backend(cache, namespace_id, backend_id)

    req_pb = api_pb2.CreateEndpointSetRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.is_global.value = True
    req_pb.backend_version = version
    req_pb.spec.instances.add(host='y.ru', port=8080, weight=-1, ipv6_addr='::1')

    with pytest.raises(exceptions.ForbiddenError,
                       match='"spec.is_global": global endpoint set only be created by roots'):
        call(endpoint_set_service.create_endpoint_set, req_pb, login)

    req_pb.spec.is_global.value = False
    es_pb = call(endpoint_set_service.create_endpoint_set, req_pb, login).endpoint_set

    req_pb = api_pb2.UpdateEndpointSetRequest()
    req_pb.backend_version = version
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.version = es_pb.meta.version
    req_pb.meta.comment = 'update'
    req_pb.spec.is_global.value = True
    req_pb.spec.instances.add(host='y.ru', port=8080, weight=-1, ipv6_addr='::1')

    with pytest.raises(exceptions.ForbiddenError, match='"spec.is_global" can only be changed by roots'):
        call(endpoint_set_service.update_endpoint_set, req_pb, login)

    set_login_to_root_users(login)
    with pytest.raises(exceptions.ForbiddenError,
                       match='"spec.is_global" cannot be set if the parent backend is not global'):
        call(endpoint_set_service.update_endpoint_set, req_pb, login)

    for b_pb in zk_storage.update_backend(namespace_id, backend_id):
        b_pb.spec.is_global.value = True
    assert wait_until(lambda: cache.must_get_backend(namespace_id, backend_id).spec.is_global.value, timeout=1)
    es_pb = call(endpoint_set_service.update_endpoint_set, req_pb, login).endpoint_set

    req_pb.meta.version = es_pb.meta.version
    req_pb.spec.is_global.value = False
    with pytest.raises(exceptions.ForbiddenError, match='"spec.is_global" cannot be changed from True to False'):
        call(endpoint_set_service.update_endpoint_set, req_pb, login)


def test_system_es(zk_storage, cache):
    login = 'morrison'
    namespace_id = 'ns_id'
    backend_id = 'backend-1'
    create_namespace(zk_storage, cache, namespace_id)
    version = create_backend(cache, namespace_id, backend_id)

    req_pb = api_pb2.CreateEndpointSetRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.is_system.value = True
    req_pb.backend_version = version
    req_pb.spec.instances.add(host='y.ru', port=8080, weight=-1, ipv6_addr='::1')

    with pytest.raises(exceptions.ForbiddenError,
                       match='"meta.is_system": system endpoint set only be created by roots'):
        call(endpoint_set_service.create_endpoint_set, req_pb, login)

    set_login_to_root_users(login)

    with pytest.raises(exceptions.ForbiddenError,
                       match='"meta.is_system": endpoint set is marked as system, but its backend is not system'):
        call(endpoint_set_service.create_endpoint_set, req_pb, login)

    for b_pb in zk_storage.update_backend(namespace_id, backend_id):
        b_pb.meta.is_system.value = True
    assert wait_until(lambda: cache.must_get_backend(namespace_id, backend_id).meta.is_system.value, timeout=1)
    es_pb = call(endpoint_set_service.create_endpoint_set, req_pb, login).endpoint_set

    req_pb = api_pb2.UpdateEndpointSetRequest()
    req_pb.backend_version = version
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.version = es_pb.meta.version
    req_pb.meta.comment = 'update'
    req_pb.meta.is_system.value = False

    with pytest.raises(exceptions.ForbiddenError, match='"meta.is_system" cannot be changed from True to False'):
        call(endpoint_set_service.update_endpoint_set, req_pb, login)
