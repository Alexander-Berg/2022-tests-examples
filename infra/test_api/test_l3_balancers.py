# coding: utf-8
import copy

import inject
import mock
import pytest
import six
from sepelib.core import config as appconfig
from six.moves import range

import awtest
from awtest.api import call, create_namespace_with_order_in_progress, fill_object_upper_limits
from awtest.core import wait_until, wait_until_passes
from awacs.lib import l3mgrclient
from awacs.lib.rpc import exceptions
from awacs.model import objects
from awacs.web import namespace_service
from awacs.web import validation as webvalidation, l3_balancer_service, backend_service
from infra.awacs.proto import api_pb2, model_pb2
from infra.swatlib.auth import abc


LOGIN = u'login'
GROUP = u"1"
NS_ID = u'namespace'
BALANCER_ID = u'balancer'
COMMENT = u'Creating very important l3_balancer'
BACKEND_ID = u'test_backend'
FQDN = u'test.yandex.ru'


@pytest.fixture(autouse=True)
def deps(binder, l3_mgr_client, abc_client):
    def configure(b):
        b.bind(abc.IAbcClient, abc_client)
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_namespace(zk_storage, namespace_id):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = namespace_id
    ns_pb.meta.category = namespace_id
    ns_pb.meta.abc_service_id = 123
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    zk_storage.create_namespace(namespace_id, ns_pb)
    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id)
    wait_until(lambda: call(namespace_service.get_namespace, req_pb, LOGIN).namespace.meta.id == namespace_id)


def create_backend(zk_storage, namespace_id=NS_ID, backend_id=BACKEND_ID, login=LOGIN, is_system=False, deleted=False):
    b_pb = model_pb2.Backend()
    b_pb.meta.id = backend_id
    b_pb.meta.namespace_id = namespace_id
    b_pb.meta.comment = 'c'
    b_pb.meta.auth.type = b_pb.meta.auth.STAFF
    if is_system:
        b_pb.meta.is_system.value = True
    b_pb.spec.deleted = deleted
    b_pb.spec.selector.type = model_pb2.BackendSelector.MANUAL
    zk_storage.create_backend(namespace_id, backend_id, b_pb)
    req_pb = api_pb2.GetBackendRequest(namespace_id=namespace_id, id=backend_id)
    wait_until(lambda: call(backend_service.get_backend, req_pb, login).backend.meta.id == backend_id)


def create_balancer(zk_storage, cache, balancer_id=u'test_id'):
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = balancer_id
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    zk_storage.create_balancer(NS_ID, balancer_id, b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, balancer_id))


def create_l3_balancer_pb():
    l3_pb = model_pb2.L3Balancer()
    l3_pb.meta.id = BALANCER_ID
    l3_pb.meta.namespace_id = NS_ID
    l3_pb.meta.auth.type = l3_pb.meta.auth.STAFF
    l3_pb.meta.version = u'xxx'
    l3_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    l3_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    l3_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS
    vs_pb = l3_pb.spec.virtual_servers.add(traffic_type=model_pb2.L3BalancerSpec.VirtualServer.TT_EXTERNAL,
                                           ip=u'127.0.0.1',
                                           port=443)
    vs_pb.health_check_settings.url = u'/pong'
    vs_pb.health_check_settings.check_type = model_pb2.L3BalancerSpec.VirtualServer.HealthCheckSettings.CT_SSL_GET
    return l3_pb


@pytest.fixture
def namespace(ctx, zk_storage, cache):
    create_namespace(zk_storage, NS_ID)

    for ns_pb in zk_storage.update_namespace(NS_ID):
        ns_pb.spec.incomplete = False
        ns_pb.order.status.status = 'FINISHED'


@pytest.fixture
def backend(zk_storage, namespace):
    create_backend(zk_storage)


@pytest.fixture
def create_l3_req_pb():
    req_pb = api_pb2.CreateL3BalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    return req_pb


def test_forbidden_operations_during_namespace_order(zk_storage, cache, enable_auth):
    # forbid creation and removal
    create_namespace_with_order_in_progress(zk_storage, cache, NS_ID)
    create_backend(zk_storage, NS_ID, BACKEND_ID, LOGIN)

    req_pb = api_pb2.CreateL3BalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    with awtest.raises(exceptions.ForbiddenError, text='Cannot do this while namespace order is in progress'):
        call(l3_balancer_service.create_l3_balancer, req_pb, LOGIN)

    req_pb_2 = api_pb2.ImportL3BalancerRequest()
    req_pb_2.meta.CopyFrom(req_pb.meta)
    req_pb_2.spec.CopyFrom(req_pb.spec)
    req_pb_2.spec.l3mgr_service_id = '1234'
    with awtest.raises(exceptions.ForbiddenError, text='Cannot do this while namespace order is in progress'):
        call(l3_balancer_service.import_l3_balancer, req_pb_2, LOGIN)

    b_pb = model_pb2.L3Balancer(spec=req_pb.spec, meta=req_pb.meta)
    b_pb.meta.version = 'xxx'
    zk_storage.create_l3_balancer(NS_ID, BALANCER_ID, b_pb)

    req_pb = api_pb2.UpdateL3BalancerRequest(meta=req_pb.meta)
    req_pb.meta.version = 'xxx'
    with awtest.raises(exceptions.ForbiddenError, text='Cannot do this while namespace order is in progress'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    req_pb = api_pb2.RemoveL3BalancerRequest(namespace_id=NS_ID, id=BALANCER_ID, version=b_pb.meta.version)
    with awtest.raises(exceptions.ForbiddenError, text='Cannot do this while namespace order is in progress'):
        call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)


@pytest.mark.parametrize('max_count,custom_count', [
    (0, None),
    (1, None),
    (10, None),
    (5, 10),
    (10, 5),
])
def test_namespace_objects_total_limit(max_count, custom_count, create_l3_req_pb, namespace, backend, checker):
    appconfig.set_value('common_objects_limits.l3_balancer', max_count)
    if custom_count is not None:
        fill_object_upper_limits(NS_ID, 'l3_balancer', custom_count, LOGIN)
    count = custom_count or max_count

    create_l3_req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    create_l3_req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    for _ in range(count):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)
        create_l3_req_pb.meta.id += 'a'
        create_l3_req_pb.spec.l3mgr_service_id += 'a'

    for check in checker:
        with check:
            list_req_pb = api_pb2.ListL3BalancersRequest(namespace_id=NS_ID)
            assert call(l3_balancer_service.list_l3_balancers, list_req_pb, LOGIN).total == count

    with awtest.raises(exceptions.BadRequestError,
                       text='Exceeded limit of l3_balancers in the namespace: {}'.format(count)):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)


@pytest.mark.parametrize('order_field', ('order', 'spec'))
def test_create_l3_balancer_with_custom_abc_service_id(order_field,
                                                       create_l3_req_pb, namespace, backend, l3_mgr_client):
    getattr(create_l3_req_pb, order_field).real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    getattr(create_l3_req_pb, order_field).real_servers.backends.add(id=BACKEND_ID)
    create_l3_req_pb.order.fqdn = FQDN
    create_l3_req_pb.order.abc_service_id = 1234

    with mock.patch.object(webvalidation.util, 'validate_user_belongs_to_abc_service') as validate_abc_service_id_m, \
            mock.patch.object(webvalidation.l3_balancer,
                              'validate_nanny_robot_is_responsible_for_l3') as validate_nanny_robot_is_l3_responsible_m:
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)
    l3_mgr_client.get_abc_service_info.assert_called_once_with(u'1234_slug')
    validate_abc_service_id_m.assert_called_once()
    validate_nanny_robot_is_l3_responsible_m.assert_called_once()
    l3_mgr_client.awtest_reset_mocks()

    create_l3_req_pb.meta.id = u'another'
    l3_mgr_client.get_abc_service_info.return_value = {u'lb': []}
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"order.content.abc_service_id": ABC service "1234_slug (1234)" '
                            u'has no available L3 load balancers. '
                            u'Please create a ticket in st/TRAFFIC to allocate LBs for your ABC service.'):
        with mock.patch.object(webvalidation.util, 'validate_user_belongs_to_abc_service'), \
                mock.patch.object(webvalidation.l3_balancer, 'validate_nanny_robot_is_responsible_for_l3'):
            call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)
    l3_mgr_client.get_abc_service_info.assert_called_once_with(u'1234_slug')


def test_preserve_foreign_rs(cache, zk_storage, create_l3_req_pb, namespace, backend, l3_mgr_client, checker):
    l3_mgr_client.awtest_set_default_config()
    rs_pb = model_pb2.L3BalancerRealServersSelector(type=model_pb2.L3BalancerRealServersSelector.BACKENDS)
    rs_pb.backends.add(id=BACKEND_ID)

    create_l3_req_pb.order.real_servers.CopyFrom(rs_pb)
    create_l3_req_pb.order.fqdn = FQDN
    create_l3_req_pb.order.preserve_foreign_real_servers = True
    with awtest.raises(exceptions.ForbiddenError) as e:
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN, enable_auth=True)
    assert six.text_type(e.value) == (u'"order.preserve_foreign_real_servers": '
                                      u'Only awacs admins can change this setting. '
                                      u'Please create a ticket in st/BALANCERSUPPORT if you need to change it')
    call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    create_l3_req_pb.meta.id += u'_1'
    create_l3_req_pb.ClearField('order')
    create_l3_req_pb.spec.real_servers.CopyFrom(rs_pb)
    create_l3_req_pb.spec.preserve_foreign_real_servers = True
    create_l3_req_pb.spec.l3mgr_service_id = u'9876'
    with awtest.raises(exceptions.ForbiddenError) as e:
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN, enable_auth=True)
    assert six.text_type(e.value) == (u'"spec.preserve_foreign_real_servers": '
                                      u'Only awacs admins can change this setting. '
                                      u'Please create a ticket in st/BALANCERSUPPORT if you need to change it')
    call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    req_pb = api_pb2.ImportL3BalancerRequest()
    req_pb.meta.CopyFrom(create_l3_req_pb.meta)
    req_pb.meta.id += u'_1'
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    req_pb.spec.preserve_foreign_real_servers = True
    req_pb.spec.l3mgr_service_id = u'1234'
    with awtest.raises(exceptions.ForbiddenError,
                       text=u'"spec.preserve_foreign_real_servers": '
                            u'Only awacs admins can change this setting. '
                            u'Please create a ticket in st/BALANCERSUPPORT if you need to change it'):
        call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN, enable_auth=True)
    resp_pb = call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN)
    l3_mgr_client.update_meta.assert_called_once_with(
        svc_id=u'1234',
        data={
            u'OWNER': u'awacs',
            u'LINK': u'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/'
                     u'namespace/l3-balancers/list/balancer_1_1/show/'
        })
    l3_mgr_client.awtest_reset_mocks()

    for b_pb in zk_storage.update_l3_balancer(resp_pb.l3_balancer.meta.namespace_id, resp_pb.l3_balancer.meta.id):
        b_pb.spec.incomplete = False
        b_pb.order.status.status = 'FINISHED'

    update_req_pb = api_pb2.UpdateL3BalancerRequest()
    update_req_pb.meta.CopyFrom(resp_pb.l3_balancer.meta)
    update_req_pb.spec.CopyFrom(resp_pb.l3_balancer.spec)
    update_req_pb.spec.preserve_foreign_real_servers = False
    for check in checker:
        with check:
            with awtest.raises(exceptions.ForbiddenError, text=u'"spec.preserve_foreign_real_servers": '
                                                               u'Only awacs admins can change this setting. '
                                                               u'Please create a ticket in st/BALANCERSUPPORT '
                                                               u'if you need to change it'):
                call(l3_balancer_service.update_l3_balancer, update_req_pb, LOGIN, enable_auth=True)
    call(l3_balancer_service.update_l3_balancer, update_req_pb, LOGIN)


@mock.patch.object(webvalidation.util, 'validate_user_belongs_to_abc_service')
def test_import_l3_balancer(_, zk_storage, cache, create_l3_req_pb, namespace, l3_mgr_client, checker):
    l3_mgr_client.awtest_set_default_config()
    create_backend(zk_storage, deleted=True)

    req_pb = api_pb2.ImportL3BalancerRequest()
    req_pb.meta.CopyFrom(create_l3_req_pb.meta)
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError, text='"spec.real_servers.type" is set to "BACKENDS", '
                                                                'but "spec.real_servers.backends" field is empty'):
                call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN)

    req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    with awtest.raises(exceptions.BadRequestError,
                       text='"spec.real_servers.backends[0]": backend "test_backend" '
                            'is marked as removed and cannot be used'):
        call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN)

    for b_pb in zk_storage.update_backend(NS_ID, BACKEND_ID):
        b_pb.spec.deleted = False
        b_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
        b_pb.spec.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='test')

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text='"spec.real_servers.backends[0]": backend "test_backend" '
                                    'has type YP_ENDPOINT_SETS_SD, which cannot be used in L3 balancer, '
                                    'change its type to YP_ENDPOINT_SETS'):
                call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN)

    for b_pb in zk_storage.update_backend(NS_ID, BACKEND_ID):
        b_pb.spec.selector.type = model_pb2.BackendSelector.MANUAL

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError, text='"spec.l3mgr_service_id" must be set'):
                call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN)

    req_pb.spec.l3mgr_service_id = '1234'
    for check in checker:
        with check:
            with mock.patch.object(webvalidation.l3_balancer, 'validate_nanny_robot_is_responsible_for_l3') as m:
                call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN)
    m.assert_called_once()
    l3_mgr_client.update_meta.assert_called_once_with(
        svc_id=u'1234',
        data={
            u'OWNER': u'awacs',
            u'LINK': u'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/'
                     u'namespace/l3-balancers/list/balancer/show/'
        })
    l3_mgr_client.awtest_reset_mocks()


@mock.patch.object(webvalidation.util, 'validate_user_belongs_to_abc_service')
@mock.patch.object(webvalidation.l3_balancer, 'validate_nanny_robot_is_responsible_for_l3')
def test_import_l3_balancer_full_management(_1, _2, zk_storage, cache, create_l3_req_pb, namespace, l3_mgr_client,
                                            checker):
    create_backend(zk_storage, backend_id=BACKEND_ID, is_system=True)
    create_balancer(zk_storage, cache, BACKEND_ID)

    req_pb = api_pb2.ImportL3BalancerRequest()
    req_pb.meta.CopyFrom(create_l3_req_pb.meta)
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    req_pb.spec.l3mgr_service_id = u'1234'
    req_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text=u'"spec.config_management_mode" must be MODE_REAL_SERVERS_ONLY. '
                                    u'You can migrate to other modes after import'):
                call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN)

    req_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY


def test_create_l3_balancer_with_invalid_fqdn(l3_mgr_client, create_l3_req_pb, namespace, backend):
    create_l3_req_pb.order.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    create_l3_req_pb.order.real_servers.backends.add(id=BACKEND_ID)
    create_l3_req_pb.order.fqdn = u'geoadv_agency_cabinet_testing_internal_l3'

    with awtest.raises(exceptions.BadRequestError, text_startswith=u'"order.fqdn" must match'):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)


@pytest.mark.parametrize('order_field', ('order', 'spec'))
def test_create_l3_balancer_with_existing_fqdn(l3_mgr_client, order_field, create_l3_req_pb, namespace, backend):
    getattr(create_l3_req_pb, order_field).real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    getattr(create_l3_req_pb, order_field).real_servers.backends.add(id=BACKEND_ID)
    create_l3_req_pb.order.fqdn = FQDN

    def list_services_by_fqdn(*_, **__):
        return [
            {u'fqdn': u'test.yandex.ru'}
        ]

    with mock.patch.object(l3_mgr_client, 'list_services_by_fqdn', list_services_by_fqdn):
        with awtest.raises(exceptions.ConflictError,
                           text='L3 balancer with FQDN "test.yandex.ru" already exists in L3mgr. '
                                'You can use "Add existing L3 Balancer" instead of creating it.'):
            call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    def list_services_by_fqdn_fine(*_, **__):
        return [
            {u'fqdn': u'another.test.yandex.net'}
        ]

    with mock.patch.object(l3_mgr_client, 'list_services_by_fqdn', list_services_by_fqdn_fine):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)


@pytest.mark.parametrize('order_field', ('order', 'spec'))
def test_create_get_remove_l3_balancer(zk_storage, cache, mongo_storage,
                                       order_field, create_l3_req_pb, namespace, checker):
    create_backend(zk_storage, deleted=True)

    getattr(create_l3_req_pb, order_field).real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    getattr(create_l3_req_pb, order_field).real_servers.backends.add(id=BACKEND_ID)
    if order_field == 'order':
        create_l3_req_pb.order.fqdn = u'test.yandex.ru'

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text='"{}.real_servers.backends[0]": backend "test_backend" '
                                    'is marked as removed and cannot be used'.format(order_field)):
                call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    for b_pb in zk_storage.update_backend(NS_ID, BACKEND_ID):
        b_pb.spec.deleted = False

    def check_field(l3_pb):
        if order_field == 'spec':
            assert l3_pb.spec.real_servers.type == model_pb2.L3BalancerRealServersSelector.BACKENDS
            assert l3_pb.spec.real_servers.backends[0].id == BACKEND_ID
        else:
            assert l3_pb.order.content.real_servers.type == model_pb2.L3BalancerRealServersSelector.BACKENDS
            assert l3_pb.order.content.real_servers.backends[0].id == BACKEND_ID

    for check in checker:
        with check:
            l3_balancer_pb = call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN).l3_balancer

    assert l3_balancer_pb.meta.id == BALANCER_ID
    assert l3_balancer_pb.meta.author == LOGIN
    assert l3_balancer_pb.meta.comment == COMMENT
    assert l3_balancer_pb.meta.auth.staff.owners.logins == [LOGIN]
    check_field(l3_balancer_pb)

    req_pb = api_pb2.GetL3BalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    b_pb = call(l3_balancer_service.get_l3_balancer, req_pb, LOGIN).l3_balancer
    check_field(b_pb)

    items, count = mongo_storage.list_l3_balancer_revs(namespace_id=NS_ID, l3_balancer_id=BALANCER_ID)
    assert count == 1
    rev_pb = items[0]
    assert rev_pb.meta.id == b_pb.meta.version
    assert rev_pb.spec == b_pb.spec

    req_pb = api_pb2.RemoveL3BalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    if order_field == 'spec':
        call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)
    else:
        with awtest.raises(exceptions.BadRequestError, text='Cannot remove while order is in progress'):
            call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)
        for l3_balancer_pb in zk_storage.update_l3_balancer(NS_ID, BALANCER_ID):
            l3_balancer_pb.order.status.status = u'FINISHED'
        for check in checker:
            with check:
                call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)

    for check in checker:
        with check:
            req_pb_ = api_pb2.GetL3BalancerRequest(namespace_id=NS_ID, id=BALANCER_ID, consistency=api_pb2.STRONG)
            with awtest.raises(exceptions.NotFoundError):
                call(l3_balancer_service.get_l3_balancer, req_pb_, LOGIN)

    assert mongo_storage.get_l3_balancer_rev(b_pb.meta.version) is None


def test_remove_l3_balancer_with_namespace_ops(zk_storage, cache, create_l3_req_pb, namespace, backend, checker):
    create_l3_req_pb.order.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    create_l3_req_pb.order.real_servers.backends.add(id=BACKEND_ID)
    create_l3_req_pb.order.fqdn = u'test.yandex.ru'
    l3_pb_1 = call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN).l3_balancer
    for l3_balancer_pb in zk_storage.update_l3_balancer(NS_ID, create_l3_req_pb.meta.id):
        l3_balancer_pb.order.status.status = u'FINISHED'

    create_l3_req_pb.meta.id = u'another_l3_balancer'
    l3_pb_2 = call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN).l3_balancer
    for l3_balancer_pb in zk_storage.update_l3_balancer(NS_ID, create_l3_req_pb.meta.id):
        l3_balancer_pb.order.status.status = u'FINISHED'

    meta_pb = model_pb2.NamespaceOperationMeta(namespace_id=NS_ID, id=l3_pb_1.meta.id)
    meta_pb.parent_versions.l3_versions[l3_pb_1.meta.id] = l3_pb_1.meta.version
    order_pb = model_pb2.NamespaceOperationOrder.Content()
    order_pb.add_ip_address_to_l3_balancer.l3_balancer_id = l3_pb_1.meta.id
    objects.NamespaceOperation.create(meta_pb, order_pb, u'test')
    wait_until(lambda: objects.NamespaceOperation.cache.list(NS_ID))
    assert len(objects.NamespaceOperation.cache.list(NS_ID)) == 1

    # first, remove L3 balancer without NS operation and make sure the operation still exists
    req_pb = api_pb2.RemoveL3BalancerRequest(namespace_id=NS_ID, id=l3_pb_2.meta.id)
    call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)
    assert len(objects.NamespaceOperation.cache.list(NS_ID)) == 1
    assert l3_pb_1.meta.id in objects.NamespaceOperation.cache.list(NS_ID)[0].meta.parent_versions.l3_versions

    # then remove L3 balancer with NS operation and make sure the operation is also removed
    req_pb = api_pb2.RemoveL3BalancerRequest(namespace_id=NS_ID, id=l3_pb_1.meta.id)
    call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)
    wait_until(lambda: not objects.NamespaceOperation.cache.list(NS_ID))
    assert len(objects.NamespaceOperation.cache.list(NS_ID)) == 0


def test_ctl_full_management_order(zk_storage, cache, create_l3_req_pb, namespace, backend):
    create_l3_req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    create_l3_req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    create_l3_req_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS

    with awtest.raises(exceptions.BadRequestError, text=u'"order" must be set'):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    create_l3_req_pb.order.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    create_l3_req_pb.order.real_servers.backends.add(id=BACKEND_ID)
    create_l3_req_pb.order.fqdn = FQDN
    create_l3_req_pb.order.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS

    with awtest.raises(exceptions.BadRequestError,
                       text=u'"order.real_servers" must point to L7 balancers in fully-managed L3 balancers'):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    create_backend(zk_storage, backend_id=u'test_id', is_system=True)
    create_balancer(zk_storage, cache, u'test_id')
    create_l3_req_pb.order.real_servers.type = model_pb2.L3BalancerRealServersSelector.BALANCERS
    create_l3_req_pb.order.real_servers.balancers.add(id=u'test_id')
    with awtest.raises(exceptions.BadRequestError, text=u'"order.abc_service_id" must be set'):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    create_l3_req_pb.order.abc_service_id = 999
    l3_pb = call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN).l3_balancer
    assert l3_pb.order.content == create_l3_req_pb.order


def test_update_virtual_servers(cache, zk_storage, create_l3_req_pb, namespace, checker):
    create_backend(zk_storage, backend_id=BACKEND_ID, is_system=True)
    create_balancer(zk_storage, cache, BACKEND_ID)
    l3_pb = create_l3_balancer_pb()
    l3_pb.spec.real_servers.type = l3_pb.spec.real_servers.BALANCERS
    l3_pb.spec.real_servers.balancers.add(id=BACKEND_ID)
    zk_storage.create_l3_balancer(NS_ID, BALANCER_ID, l3_pb)

    req_pb = api_pb2.UpdateL3BalancerRequest(meta=l3_pb.meta, spec=l3_pb.spec)
    req_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY
    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text=u'"spec.virtual_servers" must not be set for RS-only L3 balancer'):
                call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    req_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS
    req_pb.spec.ClearField(b'virtual_servers')
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers" must be set for fully-managed L3 balancer'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs_pb = req_pb.spec.virtual_servers.add()
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers[0].ip" must be set'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs_pb.ip = u'127.0.0.2asd'
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers[0].ip": "127.0.0.2asd" is not a valid IP address'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs_pb.ip = u'127.0.0.2'
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers[0].port" must be set'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs_pb.port = 111
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers[0].traffic_type" must be set'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs_pb.traffic_type = vs_pb.TT_INTERNAL
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers[0].health_check_settings.url" must be set'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs_pb.health_check_settings.url = u'/pong'
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers[0].health_check_settings.check_type" must be set'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs_pb.health_check_settings.check_type = vs_pb.health_check_settings.CT_SSL_GET
    vs2_pb = req_pb.spec.virtual_servers.add()
    vs2_pb.CopyFrom(vs_pb)
    with awtest.raises(exceptions.BadRequestError,
                       text=u'"spec.virtual_servers[1]": IP+port "127.0.0.2:111" is already used in another VS'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    vs2_pb.port = 999
    call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)


def test_full_management_migration(l3_mgr_client, checker):
    l3_mgr_client.awtest_set_default_config()
    vs = l3_mgr_client.vs[0]
    vs[u'config'][u'ANNOUNCE'] = True
    vs[u'config'][u'DC_FILTER'] = True
    vs[u'config'][u'SCHEDULER'] = u'wrr'
    vs[u'config'][u'MH_FALLBACK'] = True

    req_pb = api_pb2.CheckL3BalancerMigrationRequest(l3mgr_service_id=u'735')
    resp_pb = call(l3_balancer_service.check_l3_balancer_migration, req_pb)
    assert not resp_pb.vs_config_conflicts

    vs_1 = copy.deepcopy(vs)
    vs_1[u'ip'] = u'127.0.0.8'
    vs_1[u'port'] = u'1'
    config = vs_1[u'config']
    config[u'SCHEDULER'] = u'mh'
    config[u'OPS'] = None
    config[u'MH_PORT'] = False
    config[u'DYNAMICWEIGHT'] = False
    config[u'DYNAMICWEIGHT_ALLOW_ZERO'] = False
    config[u'DYNAMICWEIGHT_RATIO'] = 9
    config[u'CHECK_TYPE'] = u'TCP_GET'
    config[u'CONNECT_IP'] = u'::1'
    config[u'CONNECT_PORT'] = u'2'
    config[u'ANNOUNCE'] = False
    config[u'MH_FALLBACK'] = None
    l3_mgr_client.awtest_add_vs(vs_1)

    vs_2 = copy.deepcopy(vs)
    vs_2[u'ip'] = u'127.0.0.8'
    vs_2[u'port'] = u'443'
    config = vs_2[u'config']
    config[u'CONNECT_IP'] = u'127.0.0.8'
    config[u'CHECK_TYPE'] = u'HTTP_GET'
    config[u'CONNECT_PORT'] = u'80'
    l3_mgr_client.awtest_add_vs(vs_2)

    vs_3 = copy.deepcopy(vs)
    vs_3[u'ip'] = u'127.0.0.8'
    vs_3[u'port'] = u'80'
    config = vs_3[u'config']
    config[u'CONNECT_IP'] = u'127.0.0.8'
    config[u'CHECK_TYPE'] = u'SSL_GET'
    config[u'CONNECT_PORT'] = u'80'
    l3_mgr_client.awtest_add_vs(vs_3)

    resp_pb = call(l3_balancer_service.check_l3_balancer_migration, req_pb)
    assert len(resp_pb.vs_config_conflicts) == 3
    conflict_pb = resp_pb.vs_config_conflicts[0]  # type: api_pb2.CheckL3BalancerMigrationResponse.ConfigValuesConflict
    assert conflict_pb.ip == u'127.0.0.8'
    assert conflict_pb.port == u'1'
    assert conflict_pb.vs_id == u'1'
    assert len(conflict_pb.diff) == 9
    diff_pb = conflict_pb.diff[u'ANNOUNCE']
    assert diff_pb.expected == u'True'
    assert diff_pb.actual == u'False'
    assert diff_pb.hint == u'Health checks for this virtual server are currently ignored, this is not recommended. ' \
                           u'Read more: https://clubs.at.yandex-team.ru/traffic/954'
    diff_pb = conflict_pb.diff[u'CHECK_TYPE']
    assert diff_pb.expected == u'HTTP_GET'
    assert diff_pb.actual == u'TCP_GET'
    assert diff_pb.hint == u'Awacs supports only two types of checks: SSL_GET for port 443, ' \
                           u'and HTTP_GET for other ports.'
    diff_pb = conflict_pb.diff[u'CONNECT_IP']
    assert diff_pb.expected == u'127.0.0.8'
    assert diff_pb.actual == u'::1'
    assert diff_pb.hint == u'Health checks should be directed to the IP of this virtual server.'
    diff_pb = conflict_pb.diff[u'CONNECT_PORT']
    assert diff_pb.expected == u'1'
    assert diff_pb.actual == u'2'
    assert diff_pb.hint == u'Health checks should be directed to the port of this virtual server.'
    diff_pb = conflict_pb.diff[u'DYNAMICWEIGHT']
    assert diff_pb.expected == u'True'
    assert diff_pb.actual == u'False'
    assert diff_pb.hint == u'Reduces connection errors when L7 balancer is restarted.'
    diff_pb = conflict_pb.diff[u'DYNAMICWEIGHT_ALLOW_ZERO']
    assert diff_pb.expected == u'True'
    assert diff_pb.actual == u'False'
    assert diff_pb.hint == u'Reduces connection errors when L7 balancer is restarted.'
    diff_pb = conflict_pb.diff[u'DYNAMICWEIGHT_RATIO']
    assert diff_pb.expected == u'30'
    assert diff_pb.actual == u'9'
    assert diff_pb.hint == u'Reduces connection errors when L7 balancer is restarted.'
    diff_pb = conflict_pb.diff[u'SCHEDULER']
    assert diff_pb.expected == u'wrr'
    assert diff_pb.actual == u'mh'
    diff_pb = conflict_pb.diff[u'MH_FALLBACK']
    assert diff_pb.expected == u'True'
    assert diff_pb.actual == u'None'
    assert diff_pb.hint == u'Used for future compatibility with Maglev balancing, safe to change'

    conflict_pb = resp_pb.vs_config_conflicts[1]  # type: api_pb2.CheckL3BalancerMigrationResponse.ConfigValuesConflict
    assert conflict_pb.ip == u'127.0.0.8'
    assert conflict_pb.port == u'443'
    assert conflict_pb.vs_id == u'2'
    assert len(conflict_pb.diff) == 2
    diff_pb = conflict_pb.diff[u'CHECK_TYPE']
    assert diff_pb.expected == u'SSL_GET'
    assert diff_pb.actual == u'HTTP_GET'
    assert diff_pb.hint == u'Health check for port 443 must check SSL connection, not plain HTTP.'
    diff_pb = conflict_pb.diff[u'CONNECT_PORT']
    assert diff_pb.expected == u'443'
    assert diff_pb.actual == u'80'
    assert diff_pb.hint == u'Health checks should be directed to the port of this virtual server.'

    conflict_pb = resp_pb.vs_config_conflicts[2]  # type: api_pb2.CheckL3BalancerMigrationResponse.ConfigValuesConflict
    assert conflict_pb.ip == u'127.0.0.8'
    assert conflict_pb.port == u'80'
    assert conflict_pb.vs_id == u'3'
    print(conflict_pb.diff.items())
    assert len(conflict_pb.diff) == 1
    diff_pb = conflict_pb.diff[u'CHECK_TYPE']
    assert diff_pb.expected == u'HTTP_GET'
    assert diff_pb.actual == u'SSL_GET'
    assert diff_pb.hint == u'Health check for ports except 443 must check plain HTTP connection, not SSL.'


@pytest.mark.parametrize('order_field', ('order', 'spec'))
def test_balancers_backend(zk_storage, cache, order_field, create_l3_req_pb, namespace, checker):
    getattr(create_l3_req_pb, order_field).real_servers.type = model_pb2.L3BalancerRealServersSelector.BALANCERS
    getattr(create_l3_req_pb, order_field).real_servers.balancers.add(id='test_id')
    if order_field == 'order':
        create_l3_req_pb.order.fqdn = u'test.yandex.net'

    with awtest.raises(exceptions.NotFoundError, text='Balancer "namespace:test_id" not found'):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    b_pb = model_pb2.Balancer()
    b_pb.meta.id = 'test_id'
    b_pb.meta.namespace_id = NS_ID
    b_pb.spec.deleted = True
    zk_storage.create_balancer(NS_ID, 'test_id', b_pb)

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text='"{}.real_servers.balancers[0]": balancer "test_id" cannot be used as a backend '
                                    'because it\'s not located in YP. Use BACKENDS type for this L3 balancer instead, '
                                    'with backends that point to your L7 balancers.'.format(order_field)):
                call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    for b_pb in zk_storage.update_balancer(NS_ID, 'test_id'):
        b_pb.meta.location.type = b_pb.meta.location.YP_CLUSTER
    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text='"{}.real_servers.balancers[0]": balancer "test_id" '
                                    'is marked as removed and cannot be used'.format(order_field)):
                call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    for b_pb in zk_storage.update_balancer(NS_ID, 'test_id'):
        b_pb.spec.deleted = False
    for check in checker:
        with check:
            with awtest.raises(exceptions.NotFoundError,
                               text=('"{}.real_servers.balancers[0]": backend for balancer "test_id" not found'.format(
                                   order_field))):
                call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    b_pb = model_pb2.Backend()
    b_pb.meta.id = 'test_id'
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.is_system.value = True
    b_pb.spec.selector.type = model_pb2.BackendSelector.BALANCERS
    b_pb.spec.selector.balancers.add(id='test_id')
    b_pb.spec.deleted = True
    zk_storage.create_backend(NS_ID, 'test_id', b_pb)

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text='"{}.real_servers.balancers[0]": backend for balancer "test_id" '
                                    'is marked as removed and cannot be used'.format(order_field)):
                call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)

    for b_pb in zk_storage.update_backend(NS_ID, 'test_id'):
        b_pb.spec.deleted = False

    def check_fields(l3_pb):
        if order_field == 'spec':
            assert l3_pb.spec.real_servers.type == model_pb2.L3BalancerRealServersSelector.BALANCERS
            assert l3_pb.spec.real_servers.balancers[0].id == 'test_id'
        else:
            assert l3_pb.order.content.real_servers.type == model_pb2.L3BalancerRealServersSelector.BALANCERS
            assert l3_pb.order.content.real_servers.balancers[0].id == 'test_id'

    for check in checker:
        with check:
            b_pb = call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN).l3_balancer
    assert b_pb.meta.id == BALANCER_ID
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.comment == COMMENT
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]
    check_fields(b_pb)

    for check in checker:
        with check:
            get_req_pb = api_pb2.GetL3BalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
            l3_balancer_pb = call(l3_balancer_service.get_l3_balancer, get_req_pb, LOGIN).l3_balancer
            check_fields(l3_balancer_pb)

    # allow keeping deleted balancers/backends if they were already present before update
    for b_pb in zk_storage.update_backend(NS_ID, 'test_id'):
        b_pb.spec.deleted = True
    for b_pb in zk_storage.update_balancer(NS_ID, 'test_id'):
        b_pb.spec.deleted = True
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = 'b2'
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.location.type = b_pb.meta.location.YP_CLUSTER
    zk_storage.create_balancer(NS_ID, 'b2', b_pb)
    b_pb = model_pb2.Backend()
    b_pb.meta.id = 'b2'
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.is_system.value = True
    b_pb.spec.selector.type = model_pb2.BackendSelector.BALANCERS
    b_pb.spec.selector.balancers.add(id='b2')
    zk_storage.create_backend(NS_ID, 'b2', b_pb)

    req_pb = api_pb2.UpdateL3BalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = l3_balancer_pb.meta.version
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BALANCERS
    req_pb.spec.real_servers.balancers.add(id='b2')
    for check in checker:
        with check:
            resp_pb = call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    # don't allow using deleted balancers/backends if real servers type has changed
    req_pb = api_pb2.UpdateL3BalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = resp_pb.l3_balancer.meta.version
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    req_pb.spec.real_servers.backends.add(id='test_id')
    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text='"spec.real_servers.backends[0]": backend "test_id" '
                                    'is marked as removed and cannot be used'):
                call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)


@pytest.mark.parametrize('order_field', ('order', 'spec'))
def test_create_l3_balancer_with_yp_sd(cache, zk_storage, order_field, create_l3_req_pb, namespace, checker):
    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = 'test_id'
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = 'c'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    req_pb.spec.selector.yp_endpoint_sets.add(cluster='sas', endpoint_set_id='test')
    call(backend_service.create_backend, req_pb, LOGIN)
    req_pb = api_pb2.GetBackendRequest(namespace_id=NS_ID, id='test_id')
    for check in checker:
        with check:
            assert call(backend_service.get_backend, req_pb, LOGIN).backend.meta.id == 'test_id'

    getattr(create_l3_req_pb, order_field).real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    getattr(create_l3_req_pb, order_field).real_servers.backends.add(id='test_id')
    if order_field == 'order':
        create_l3_req_pb.order.fqdn = u'test'

    with awtest.raises(exceptions.BadRequestError,
                       text='"{}.real_servers.backends[0]": backend "test_id" has type YP_ENDPOINT_SETS_SD, '
                            'which cannot be used in L3 balancer, '
                            'change its type to YP_ENDPOINT_SETS'.format(order_field)):
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)


def test_update_l3_balancer(zk_storage, cache, mongo_storage, namespace, backend, checker):
    create_backend(zk_storage, NS_ID, 'test_id_2')

    req_pb = api_pb2.CreateL3BalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    req_pb.spec.real_servers.backends.add(id=BACKEND_ID)

    resp_pb = call(l3_balancer_service.create_l3_balancer, req_pb, LOGIN)
    b_pb = resp_pb.l3_balancer
    assert b_pb.meta.id == BALANCER_ID
    initial_version = b_pb.meta.version
    assert mongo_storage.list_l3_balancer_revs(namespace_id=b_pb.meta.namespace_id, l3_balancer_id=b_pb.meta.id).total == 1

    for backend_pb in zk_storage.update_backend(NS_ID, BACKEND_ID):
        backend_pb.spec.deleted = True

    req_pb = api_pb2.UpdateL3BalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = 'xxx'
    req_pb.meta.comment = COMMENT

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text='at least one of the "spec", "meta.auth" or "meta.transport_paused" '
                                    'fields must be present'):
                call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.incomplete = False
    req_pb.spec.real_servers.backends.add(id='test_id_2')
    with awtest.raises(exceptions.ConflictError) as e:
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)
    e.match('L3 balancer modification conflict')
    assert mongo_storage.list_l3_balancer_revs(namespace_id=b_pb.meta.namespace_id, l3_balancer_id=b_pb.meta.id).total == 1

    req_pb.meta.version = b_pb.meta.version
    call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)

    for check in checker:
        with check:
            req_pb_ = api_pb2.GetL3BalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
            b_pb_ = call(l3_balancer_service.get_l3_balancer, req_pb_, LOGIN).l3_balancer
            assert len(b_pb_.spec.real_servers.backends) == 2
            assert b_pb_.meta.comment == COMMENT
            assert b_pb_.meta.version != initial_version
            assert mongo_storage.list_l3_balancer_revs(namespace_id=b_pb_.meta.namespace_id,
                                                       l3_balancer_id=b_pb_.meta.id).total == 2

    rev_pb = mongo_storage.get_l3_balancer_rev(b_pb.meta.version)
    assert rev_pb.meta.id == b_pb.meta.version
    assert rev_pb.meta.l3_balancer_id == BALANCER_ID
    assert rev_pb.meta.namespace_id == rev_pb.meta.namespace_id
    assert rev_pb.spec == b_pb.spec


@pytest.mark.parametrize('order_field', ('order', 'spec'))
def test_list_l3_balancers_and_revisions(zk_storage, cache, order_field, checker):
    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    l3_balancer_pbs = {}
    for i, l3_id in enumerate(ids):
        create_namespace(zk_storage, l3_id)
        create_backend(zk_storage, l3_id, 'test_id_1')

        req_pb = api_pb2.CreateL3BalancerRequest()
        req_pb.meta.id = l3_id
        req_pb.meta.namespace_id = l3_id
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.spec.l3mgr_service_id = six.text_type(i)
        getattr(req_pb, order_field).real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
        getattr(req_pb, order_field).real_servers.backends.add(id='test_id_1')
        if order_field == 'order':
            req_pb.order.fqdn = l3_id
        l3_balancer_pbs[l3_id] = call(l3_balancer_service.create_l3_balancer, req_pb, LOGIN).l3_balancer

    req_pb = api_pb2.ListL3BalancersRequest(namespace_id='aaa')

    for check in checker:
        with check:
            resp_pb = call(l3_balancer_service.list_l3_balancers, req_pb, LOGIN)
            assert len(resp_pb.l3_balancers) == 1
            assert [b.meta.id for b in resp_pb.l3_balancers] == ['aaa']
            assert all(resp_pb.l3_balancers[0].HasField(f) for f in ('meta', 'spec', 'l3_status'))

    req_pb.field_mask.paths.append('meta')
    resp_pb = call(l3_balancer_service.list_l3_balancers, req_pb, LOGIN)
    assert len(resp_pb.l3_balancers) == 1
    assert [b.meta.id for b in resp_pb.l3_balancers] == ['aaa']
    assert resp_pb.l3_balancers[0].HasField('meta')
    assert all(not resp_pb.l3_balancers[0].HasField(f) for f in ('spec', 'l3_status'))

    req_pb = api_pb2.ListL3BalancersRequest(namespace_id='aaa')
    resp_pb = call(l3_balancer_service.list_l3_balancers, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.l3_balancers) == 1

    req_pb = api_pb2.ListL3BalancersRequest(namespace_id='bbb', skip=1)
    resp_pb = call(l3_balancer_service.list_l3_balancers, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.l3_balancers) == 0

    # add yet another l3_balancer
    create_namespace(zk_storage, 'eee')
    create_backend(zk_storage, 'eee', 'test_id_2')

    req_pb = api_pb2.CreateL3BalancerRequest()
    req_pb.meta.id = 'eee'
    req_pb.meta.namespace_id = 'eee'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    getattr(req_pb, order_field).real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    getattr(req_pb, order_field).real_servers.backends.add(id='test_id_2')
    if order_field == 'order':
        req_pb.order.fqdn = 'test'

    l3_balancer_pbs[req_pb.meta.id] = call(l3_balancer_service.create_l3_balancer, req_pb, LOGIN).l3_balancer
    for check in checker:
        with check:
            resp_pb = call(l3_balancer_service.list_l3_balancers, api_pb2.ListL3BalancersRequest(namespace_id='eee'),
                           LOGIN)
            assert len(resp_pb.l3_balancers) == 1

    # remove added l3_balancer
    req_pb = api_pb2.RemoveL3BalancerRequest(namespace_id='eee', id='eee')
    if order_field == 'spec':
        call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)
    else:
        with awtest.raises(exceptions.BadRequestError, text='Cannot remove while order is in progress'):
            call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)
        for l3_balancer_pb in zk_storage.update_l3_balancer('eee', 'eee'):
            l3_balancer_pb.order.status.status = u'FINISHED'
        for check in checker:
            with check:
                call(l3_balancer_service.remove_l3_balancer, req_pb, LOGIN)
    for check in checker:
        with check:
            resp_pb = call(l3_balancer_service.list_l3_balancers, api_pb2.ListL3BalancersRequest(namespace_id='eee'),
                           LOGIN)
            assert len(resp_pb.l3_balancers) == 0

    for i in range(2):
        create_backend(zk_storage, 'aaa', 'test_id_{}'.format(3 + i), 'romanovich')
        req_pb = api_pb2.UpdateL3BalancerRequest()
        req_pb.meta.id = 'aaa'
        req_pb.meta.namespace_id = 'aaa'
        req_pb.meta.version = l3_balancer_pbs['aaa'].meta.version
        req_pb.spec.CopyFrom(l3_balancer_pbs['aaa'].spec)
        req_pb.spec.real_servers.backends.add(id='test_id_{}'.format(3 + i))
        req_pb.spec.incomplete = False
        l3_balancer_pbs['aaa'] = call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN).l3_balancer

    req_pb = api_pb2.ListL3BalancerRevisionsRequest(namespace_id='aaa', id='aaa')
    resp_pb = call(l3_balancer_service.list_l3_balancer_revisions, req_pb, LOGIN)
    assert resp_pb.total == 3
    assert set(rev.meta.l3_balancer_id for rev in resp_pb.revisions) == {'aaa'}
    assert len(resp_pb.revisions) == 3
    assert resp_pb.revisions[0].meta.id == l3_balancer_pbs['aaa'].meta.version

    req_pb = api_pb2.ListL3BalancerRevisionsRequest(namespace_id='aaa', id='aaa', skip=2)
    resp_pb = call(l3_balancer_service.list_l3_balancer_revisions, req_pb, LOGIN)
    assert resp_pb.total == 3
    assert len(resp_pb.revisions) == 1

    req_pb = api_pb2.ListL3BalancerRevisionsRequest(namespace_id='aaa', id='aaa', skip=2, limit=1)
    resp_pb = call(l3_balancer_service.list_l3_balancer_revisions, req_pb, LOGIN)
    assert resp_pb.total == 3
    assert len(resp_pb.revisions) == 1


def test_l3mgr_service_id_validation(zk_storage):
    namespace_1_id = 'aaa'
    namespace_2_id = 'bbb'
    l3_balancer_id = 'xxx'

    create_namespace(zk_storage, namespace_1_id)
    create_backend(zk_storage, namespace_1_id, BACKEND_ID)
    create_namespace(zk_storage, namespace_2_id)
    create_backend(zk_storage, namespace_2_id, BACKEND_ID)

    req_pb = api_pb2.CreateL3BalancerRequest()
    req_pb.meta.id = l3_balancer_id
    req_pb.meta.namespace_id = namespace_1_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.l3mgr_service_id = '123'
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    req_pb.spec.real_servers.backends.add(id=BACKEND_ID)

    call(l3_balancer_service.create_l3_balancer, req_pb, LOGIN)

    req_pb.meta.namespace_id = namespace_2_id
    with awtest.raises(exceptions.ConflictError) as e:
        call(l3_balancer_service.create_l3_balancer, req_pb, LOGIN)

    e.match('L3 balancer with L3mgr id "123" is already managed by another awacs L3 balancer: "{}:{}"'.format(
        namespace_1_id, l3_balancer_id))
    req_pb.spec.l3mgr_service_id = '456'
    resp_pb = call(l3_balancer_service.create_l3_balancer, req_pb, LOGIN)

    req_pb = api_pb2.UpdateL3BalancerRequest()
    req_pb.meta.id = l3_balancer_id
    req_pb.meta.namespace_id = namespace_2_id
    req_pb.meta.version = resp_pb.l3_balancer.meta.version
    req_pb.spec.CopyFrom(resp_pb.l3_balancer.spec)
    req_pb.spec.l3mgr_service_id = '123'
    req_pb.spec.incomplete = False

    with awtest.raises(exceptions.BadRequestError,
                       text='"spec.l3mgr_service_id": cannot change for existing balancer'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)


@pytest.mark.parametrize('use_order, prohibit_explicit', [(False, False), (False, True), (True, False), (True, True)])
def test_create_l3_balancer_with_explicit_selector(create_l3_req_pb, namespace, backend, use_order, prohibit_explicit):
    if prohibit_explicit:
        req_pb = api_pb2.GetNamespaceRequest(id=NS_ID)
        namespace_pb = call(namespace_service.get_namespace, req_pb, LOGIN).namespace
        namespace_pb.spec.easy_mode_settings.prohibit_explicit_l3_selector.value = True
        req_pb = api_pb2.UpdateNamespaceRequest(meta=namespace_pb.meta, spec=namespace_pb.spec)
        call(namespace_service.update_namespace, req_pb, LOGIN)

    if use_order:
        create_l3_req_pb.order.real_servers.type = create_l3_req_pb.order.real_servers.BACKENDS
        create_l3_req_pb.order.real_servers.backends.add(id=BACKEND_ID)
        create_l3_req_pb.order.fqdn = FQDN
    else:
        create_l3_req_pb.spec.real_servers.type = create_l3_req_pb.spec.real_servers.BACKENDS
        create_l3_req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    if prohibit_explicit:
        with awtest.raises(exceptions.ForbiddenError,
                           text='Only L3 balancers with BALANCERS selector can be created in this namespace'):
            call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)
    else:
        call(l3_balancer_service.create_l3_balancer, create_l3_req_pb, LOGIN)


def test_dzen_l3_restrictions(l3_mgr_client, create_l3_req_pb, namespace, backend):
    req_pb = api_pb2.GetNamespaceRequest(id=NS_ID)
    namespace_pb = call(namespace_service.get_namespace, req_pb, LOGIN).namespace
    namespace_pb.meta.annotations['project'] = 'dzen'
    namespace_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    req_pb = api_pb2.UpdateNamespaceRequest(meta=namespace_pb.meta, spec=namespace_pb.spec)
    call(namespace_service.update_namespace, req_pb, LOGIN)

    req_pb = api_pb2.ImportL3BalancerRequest()
    req_pb.meta.CopyFrom(create_l3_req_pb.meta)
    req_pb.meta.id += u'_1'
    req_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    req_pb.spec.real_servers.backends.add(id=BACKEND_ID)
    req_pb.spec.l3mgr_service_id = u'1234'
    with awtest.raises(exceptions.ForbiddenError,
                       text=u'Only L3 balancers with "vk_dzen_l3_management" '
                            u'ABC service can be created in dzen awacs namespace'):
        call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN, enable_auth=True)

    l3_mgr_client.abc_slug = 'vk_dzen_l3_management'
    resp_pb = call(l3_balancer_service.import_l3_balancer, req_pb, LOGIN, enable_auth=True)

    meta_pb = resp_pb.l3_balancer.meta
    spec_pb = resp_pb.l3_balancer.spec
    spec_pb.skip_tests.value = False
    req_pb = api_pb2.UpdateL3BalancerRequest(meta=meta_pb, spec=spec_pb)
    with awtest.raises(exceptions.BadRequestError,
                       text='"spec.skip_tests": cannot be changed for the time being'):
        call(l3_balancer_service.update_l3_balancer, req_pb, LOGIN)
