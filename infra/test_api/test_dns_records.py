# coding: utf-8

import inject
import mock
import pytest
import six
from sepelib.core import config as appconfig
from six.moves import range

from awacs.lib.rpc import exceptions
from awacs.model.dao import DEFAULT_NAME_SERVERS, INFRA_NAMESPACE_ID
from awacs.model.util import clone_pb
from infra.awacs.proto import api_pb2, model_pb2
from awacs.web import dns_record_service
from awacs.web import namespace_service
from infra.swatlib.auth import abc
from awtest.api import call, create_namespace_with_order_in_progress, fill_object_upper_limits
from awtest.core import wait_until, wait_until_passes


NS_ID = 'hyperspace'
LOGIN = 'ribbon_robot'
GROUP = "1"
COMMENT = 'comment comment'


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture(autouse=True)
def default_ns(deps, dao):
    dao.create_default_name_servers()


def create_namespace(namespace_id):
    """
    :type namespace_id: str
    """
    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_id
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    call(namespace_service.create_namespace, req_pb, LOGIN)


@pytest.fixture
def namespace():
    return create_namespace(NS_ID)


def create_backend(zk_storage, cache, namespace_id, backend_id, backend_type):
    b_pb = model_pb2.Backend()
    b_pb.meta.id = backend_id
    b_pb.meta.namespace_id = namespace_id
    b_pb.spec.selector.type = backend_type
    zk_storage.create_backend(namespace_id, backend_id, b_pb)
    wait_until_passes(lambda: cache.must_get_backend(namespace_id, backend_id))


@pytest.fixture
def create_dns_record_request_pb():
    req_pb = api_pb2.CreateDnsRecordRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.name_server.namespace_id = 'infra'
    req_pb.spec.name_server.id = 'in.yandex-team.ru'
    req_pb.spec.address.zone = 'hyperspace'
    return req_pb


@pytest.fixture
def create_dns_record_order_pb():
    req_pb = api_pb2.CreateDnsRecordRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.order.name_server.namespace_id = 'infra'
    req_pb.order.name_server.id = 'in.yandex-team.ru'
    req_pb.order.address.zone = 'hyperspace'
    return req_pb


@pytest.fixture
def create_dns_record_operation_pb():
    req_pb = api_pb2.CreateDnsRecordOperationRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.dns_record_id = NS_ID
    req_pb.meta.comment = COMMENT
    return req_pb


def test_yp_sd_backend(zk_storage, cache, namespace, create_dns_record_request_pb):
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id='eee', id='b1')

    with pytest.raises(exceptions.BadRequestError,
                       match=r'"spec.address.backends.backends\[0\]": cannot use backend "b1" '
                             r'from another namespace "eee"'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    create_dns_record_request_pb.spec.address.backends.backends[0].namespace_id = NS_ID
    with pytest.raises(exceptions.NotFoundError, match='Backend "hyperspace:b1" not found'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    create_backend(zk_storage, cache, NS_ID, 'b1', model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD)
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"spec.address.backends.backends\[0\]": backend "b1" has type '
                             r'YP_ENDPOINT_SETS_SD, which cannot be used in DNS record'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    create_backend(zk_storage, cache, NS_ID, 'b2', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    del create_dns_record_request_pb.spec.address.backends.backends[:]
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='b2')
    call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)


def test_forbidden_operations_during_namespace_order(zk_storage, cache, enable_auth, create_dns_record_request_pb):
    # forbid creation and removal
    create_namespace_with_order_in_progress(zk_storage, cache, NS_ID)
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    d_pb = model_pb2.DnsRecord(meta=create_dns_record_request_pb.meta, spec=create_dns_record_request_pb.spec)
    d_pb.meta.version = 'xxx'
    d_pb.meta.auth.staff.owners.logins.append(LOGIN)
    zk_storage.create_dns_record(NS_ID, NS_ID, d_pb)
    wait_until_passes(lambda: cache.must_get_dns_record(NS_ID, NS_ID))

    req_pb = api_pb2.RemoveDnsRecordRequest(namespace_id=NS_ID, id=NS_ID, version=d_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(dns_record_service.remove_dns_record, req_pb, LOGIN)


@pytest.mark.parametrize('max_count,custom_count', [
    (0, None),
    (1, None),
    (10, None),
    (5, 10),
    (10, 5),
])
def test_namespace_objects_total_limit(max_count, custom_count, zk_storage, cache, create_default_namespace,
                                       create_dns_record_request_pb):
    create_default_namespace(NS_ID)
    appconfig.set_value('common_objects_limits.dns_record', max_count)
    if custom_count is not None:
        fill_object_upper_limits(NS_ID, 'dns_record', custom_count, LOGIN)
    count = custom_count or max_count
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)

    b = model_pb2.DnsBackendsSelector.Backend(namespace_id=NS_ID, id='some_back')
    create_dns_record_request_pb.spec.address.backends.backends.extend([b])
    for _ in range(count):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
        create_dns_record_request_pb.meta.id += 'a'
        create_dns_record_request_pb.spec.address.zone += 'a'

    def check():
        list_req_pb = api_pb2.ListDnsRecordsRequest(namespace_id=NS_ID)
        assert call(dns_record_service.list_dns_records, list_req_pb, LOGIN).total == count

    wait_until_passes(check)

    with pytest.raises(exceptions.BadRequestError,
                       match='Exceeded limit of dns_records in the namespace: {}'.format(count)):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)


def test_create_get_remove_dns_record(zk_storage, cache, mongo_storage, namespace, create_dns_record_request_pb,
                                      checker):
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
    resp_pb = call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    b_pb = resp_pb.dns_record
    assert b_pb.meta.id == NS_ID
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.comment == COMMENT
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]
    assert b_pb.spec.address.zone == 'hyperspace'
    assert b_pb.spec.name_server.namespace_id == 'infra'
    assert b_pb.spec.name_server.id == 'in.yandex-team.ru'
    assert b_pb.spec.address.backends.backends[0].namespace_id == NS_ID
    assert b_pb.spec.address.backends.backends[0].id == 'some_back'

    # test duplicated zone
    dup_req_pb = clone_pb(create_dns_record_request_pb)
    dup_req_pb.meta.id = NS_ID + '_1'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, dup_req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec": DNS record for "hyperspace.in.yandex-team.ru" already exists: ' \
                                     '"hyperspace:hyperspace"'

    # test trailing dot in zone name
    dup_req_pb.spec.address.zone = 'hyperspace.'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, dup_req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.address.zone" is not valid: domain name must not end in .'

    # test empty backends list
    dup_req_pb.spec.address.zone = 'hyperspace1'
    del dup_req_pb.spec.address.backends.backends[:]
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, dup_req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.address.backends.backends" must be set'

    # Test get
    req_pb = api_pb2.GetDnsRecordRequest(namespace_id=NS_ID, id=NS_ID)
    b_pb = call(dns_record_service.get_dns_record, req_pb, LOGIN).dns_record
    assert b_pb.spec.address.backends.backends[0].namespace_id == NS_ID
    assert b_pb.spec.address.backends.backends[0].id == 'some_back'

    req_pb = api_pb2.GetDnsRecordRequest(namespace_id='missing', id='missing')
    with pytest.raises(exceptions.NotFoundError):
        call(dns_record_service.get_dns_record, req_pb, LOGIN)

    items, count = mongo_storage.list_dns_record_revs(namespace_id=NS_ID, dns_record_id=NS_ID)
    assert count == 1
    rev_pb = items[0]
    assert rev_pb.meta.id == b_pb.meta.version
    assert rev_pb.spec == b_pb.spec

    # Test get state
    req_pb = api_pb2.GetDnsRecordStateRequest(namespace_id=NS_ID, id=NS_ID)
    resp_pb = call(dns_record_service.get_dns_record_state, req_pb, LOGIN)
    s_pb = resp_pb.state
    assert s_pb.dns_record_id == NS_ID
    assert s_pb.namespace_id == NS_ID

    req_pb = api_pb2.GetDnsRecordRequest(namespace_id='missing', id='missing')
    with pytest.raises(exceptions.NotFoundError):
        call(dns_record_service.get_dns_record, req_pb, LOGIN)

    req_pb = api_pb2.GetDnsRecordStateRequest(namespace_id='missing', id='missing')
    with pytest.raises(exceptions.NotFoundError):
        call(dns_record_service.get_dns_record_state, req_pb, LOGIN)

    items, count = mongo_storage.list_dns_record_revs(namespace_id=NS_ID, dns_record_id=NS_ID)
    assert count == 1
    rev_pb = items[0]
    assert rev_pb.meta.id == b_pb.meta.version
    assert rev_pb.spec == b_pb.spec

    # Test remove
    req_pb = api_pb2.RemoveDnsRecordRequest(namespace_id=NS_ID, id=NS_ID, version=rev_pb.meta.id)
    call(dns_record_service.remove_dns_record, req_pb, LOGIN)

    for a in checker:
        with a:
            pb = call(dns_record_service.get_dns_record,
                      api_pb2.GetDnsRecordRequest(namespace_id=NS_ID, id=NS_ID),
                      LOGIN).dns_record
            assert pb.spec.deleted

    _, count = mongo_storage.list_dns_record_revs(namespace_id=NS_ID, dns_record_id=NS_ID)
    assert count == 2


def test_order_dns_record(zk_storage, cache, namespace, checker, create_dns_record_order_pb):
    create_dns_record_order_pb.order.address.backends.backends.add(namespace_id=NS_ID, id='some_back')

    with pytest.raises(exceptions.NotFoundError):
        call(dns_record_service.create_dns_record, create_dns_record_order_pb, LOGIN)

    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    b_pb = call(dns_record_service.create_dns_record, create_dns_record_order_pb, LOGIN).dns_record
    assert b_pb.meta.id == NS_ID
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.comment == COMMENT
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]
    assert b_pb.order.content.address.zone == 'hyperspace'
    assert b_pb.order.content.name_server.namespace_id == 'infra'
    assert b_pb.order.content.name_server.id == 'in.yandex-team.ru'
    assert b_pb.order.content.address.backends.backends[0].namespace_id == NS_ID
    assert b_pb.order.content.address.backends.backends[0].id == 'some_back'
    assert b_pb.spec.incomplete

    # test duplicated zone
    dup_req_pb = clone_pb(create_dns_record_order_pb)
    dup_req_pb.meta.id = NS_ID + '_1'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, dup_req_pb, LOGIN)
    assert six.text_type(e.value) == '"order": DNS record for "hyperspace.in.yandex-team.ru" already exists: ' \
                                     '"hyperspace:hyperspace"'

    # test trailing dot in zone name
    dup_req_pb.order.address.zone = 'hyperspace.'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, dup_req_pb, LOGIN)
    assert six.text_type(e.value) == '"order.address.zone" is not valid: domain name must not end in .'

    # test empty backends list
    dup_req_pb.order.address.zone = 'hyperspace1'
    del dup_req_pb.order.address.backends.backends[:]
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, dup_req_pb, LOGIN)
    assert six.text_type(e.value) == '"order.address.backends.backends" must be set'


def test_cancel_order(zk_storage, cache, namespace, checker, create_dns_record_request_pb, create_dns_record_order_pb):
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    req_pb = api_pb2.CancelDnsRecordOrderRequest(namespace_id=NS_ID, id=NS_ID)
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel order that is not in progress'):
        call(dns_record_service.cancel_dns_record_order, req_pb, LOGIN)

    create_dns_record_order_pb.meta.id = NS_ID + '1'
    create_dns_record_order_pb.order.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
    create_dns_record_order_pb.order.address.zone = 'hyperspace1'
    call(dns_record_service.create_dns_record, create_dns_record_order_pb, LOGIN)

    req_pb = api_pb2.CancelDnsRecordOrderRequest(namespace_id=NS_ID, id=NS_ID + '1')
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel DNS record order at this stage'):
        call(dns_record_service.cancel_dns_record_order, req_pb, LOGIN)

    for dns_record_pb in zk_storage.update_dns_record(NS_ID, NS_ID + '1'):
        dns_record_pb.order.progress.state.id = 'GETTING_L3_IP_ADDRESSES'
        dns_record_pb.order.status.status = 'IN_PROGRESS'
    assert wait_until(lambda: cache.must_get_dns_record(NS_ID, NS_ID + '1').order.status.status == 'IN_PROGRESS')
    call(dns_record_service.cancel_dns_record_order, req_pb, LOGIN)


def test_operation_modify_addresses(zk_storage, cache, namespace, checker, create_dns_record_operation_pb,
                                    create_dns_record_request_pb):
    with pytest.raises(exceptions.BadRequestError, match='"order" must be set'):
        call(dns_record_service.create_dns_record_operation, create_dns_record_operation_pb, LOGIN)
    create_dns_record_operation_pb.order.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"order.content.modify_addresses" must be set'):
        call(dns_record_service.create_dns_record_operation, create_dns_record_operation_pb, LOGIN)
    create_dns_record_operation_pb.order.content.modify_addresses.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"order.content.modify_addresses.requests" must be set'):
        call(dns_record_service.create_dns_record_operation, create_dns_record_operation_pb, LOGIN)

    address_req_pb_0 = create_dns_record_operation_pb.order.content.modify_addresses.requests.add()
    address_req_pb_0.action = address_req_pb_0.CREATE
    address_req_pb_0.address = '127.0.0.1'
    address_req_pb_1 = create_dns_record_operation_pb.order.content.modify_addresses.requests.add()
    address_req_pb_1.action = address_req_pb_1.REMOVE
    address_req_pb_1.address = '::1'

    with pytest.raises(exceptions.NotFoundError,
                       match="Cannot create operation for DNS record that doesn't exist: \"hyperspace:hyperspace\""):
        call(dns_record_service.create_dns_record_operation, create_dns_record_operation_pb, LOGIN)

    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    with pytest.raises(exceptions.ForbiddenError, match='This method is only available for awacs admins'):
        call(dns_record_service.create_dns_record_operation, create_dns_record_operation_pb, LOGIN, enable_auth=True)

    op_pb = call(dns_record_service.create_dns_record_operation,
                 create_dns_record_operation_pb, LOGIN).operation

    def check_op(pb):
        assert pb.meta.dns_record_id == NS_ID
        assert pb.meta.namespace_id == NS_ID
        assert pb.meta.id.startswith(NS_ID)
        assert pb.meta.author == LOGIN
        assert pb.meta.comment == COMMENT
        assert len(pb.order.content.modify_addresses.requests) == 2
        assert pb.order.content.modify_addresses.requests[0] == address_req_pb_0
        assert pb.order.content.modify_addresses.requests[1] == address_req_pb_1
        assert pb.spec.incomplete

    check_op(op_pb)
    op_pb = call(dns_record_service.get_dns_record_operation,
                 api_pb2.GetDnsRecordOperationRequest(id=op_pb.meta.id, namespace_id=op_pb.meta.namespace_id),
                 LOGIN).operation
    check_op(op_pb)

    dup_req_pb = clone_pb(create_dns_record_operation_pb)
    del dup_req_pb.order.content.modify_addresses.requests[:]
    address_req_pb = dup_req_pb.order.content.modify_addresses.requests.add()
    address_req_pb.action = address_req_pb.NONE
    address_req_pb.address = 'invalid'

    with pytest.raises(exceptions.BadRequestError, match=r'"order.content.modify_addresses.requests\[0\].action": '
                                                         r'unsupported address modification action: "NONE"'):
        call(dns_record_service.create_dns_record_operation, dup_req_pb, LOGIN)
    address_req_pb.action = address_req_pb.CREATE

    with pytest.raises(exceptions.BadRequestError, match=r'"order.content.modify_addresses.requests\[0\].address": '
                                                         r'"invalid" is not a valid IP address'):
        call(dns_record_service.create_dns_record_operation, dup_req_pb, LOGIN)

    dup_req_pb.meta.id = NS_ID + '1'
    del dup_req_pb.order.content.modify_addresses.requests[:]
    address_req_pb = dup_req_pb.order.content.modify_addresses.requests.add()
    address_req_pb.action = address_req_pb.CREATE
    address_req_pb.address = '127.0.0.2'
    with pytest.raises(exceptions.BadRequestError, match=r'"meta.dns_record_id" must be equal to "meta.id"'):
        call(dns_record_service.create_dns_record_operation, dup_req_pb, LOGIN)

    dup_req_pb.meta.dns_record_id = NS_ID + '1'
    create_dns_record_request_pb.meta.id = NS_ID + '1'
    create_dns_record_request_pb.spec.address.zone = 'another'
    call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    call(dns_record_service.create_dns_record_operation, dup_req_pb, LOGIN)

    for a in checker:
        with a:
            assert len(call(dns_record_service.list_dns_record_operations,
                            api_pb2.ListDnsRecordOperationsRequest(namespace_id=NS_ID),
                            LOGIN).operations) == 2

    req_pb = api_pb2.CancelDnsRecordOperationRequest(namespace_id=NS_ID, id=NS_ID)
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel DNS record operation at this stage'):
        call(dns_record_service.cancel_dns_record_operation, req_pb, LOGIN)

    for op_pb in zk_storage.update_dns_record_operation(NS_ID, NS_ID):
        op_pb.order.progress.state.id = 'STARTED'
        op_pb.order.status.status = 'IN_PROGRESS'
    assert wait_until(lambda: cache.must_get_dns_record_operation(NS_ID, NS_ID).order.status.status == 'IN_PROGRESS')

    with pytest.raises(exceptions.ForbiddenError, match='This method is only available for awacs admins'):
        call(dns_record_service.cancel_dns_record_operation, req_pb, LOGIN, enable_auth=True)

    call(dns_record_service.cancel_dns_record_operation, req_pb, LOGIN)


def test_list_dns_records_and_revisions(zk_storage, cache, checker, create_dns_record_request_pb):
    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    dns_record_pbs = {}
    for i, _id in enumerate(ids):
        create_namespace(_id)
        create_backend(zk_storage, cache, _id, 'some_back_{}'.format(i), model_pb2.BackendSelector.YP_ENDPOINT_SETS)

        req_pb = clone_pb(create_dns_record_request_pb)
        req_pb.meta.id = _id
        req_pb.meta.namespace_id = _id
        req_pb.spec.address.zone = 'hyperspace-{}'.format(i)
        req_pb.spec.address.backends.backends.add(namespace_id=_id, id='some_back_{}'.format(i))
        dns_record_pbs[_id] = call(dns_record_service.create_dns_record, req_pb, LOGIN).dns_record

    for a in checker:
        with a:
            req_pb = api_pb2.ListDnsRecordsRequest(namespace_id='aaa')
            resp_pb = call(dns_record_service.list_dns_records, req_pb, LOGIN)
            assert resp_pb.total == 1
            assert len(resp_pb.dns_records) == 1
            assert [b.meta.id for b in resp_pb.dns_records] == ['aaa']
            assert all(resp_pb.dns_records[0].HasField(f) for f in ('meta', 'spec'))

    req_pb = api_pb2.ListDnsRecordsRequest(namespace_id='bbb', skip=1)
    resp_pb = call(dns_record_service.list_dns_records, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.dns_records) == 0

    # add yet another dns_records
    create_namespace('eee')
    create_backend(zk_storage, cache, 'eee', 'some_back_eee', model_pb2.BackendSelector.YP_ENDPOINT_SETS)

    req_pb = api_pb2.CreateDnsRecordRequest()
    req_pb.meta.id = 'eee'
    req_pb.meta.namespace_id = 'eee'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.name_server.namespace_id = 'infra'
    req_pb.spec.name_server.id = 'in.yandex-team.ru'
    req_pb.spec.address.zone = 'hyperspace-eee'
    req_pb.spec.address.backends.backends.add(namespace_id='eee', id='some_back_eee')

    dns_record_pbs[req_pb.meta.id] = call(dns_record_service.create_dns_record, req_pb, LOGIN).dns_record

    for a in checker:
        with a:
            resp_pb = call(dns_record_service.list_dns_records, api_pb2.ListDnsRecordsRequest(namespace_id='eee'),
                           LOGIN)
            assert len(resp_pb.dns_records) == 1

    # remove added dns_records
    req_pb = api_pb2.RemoveDnsRecordRequest(namespace_id='eee', id='eee', version=resp_pb.dns_records[0].meta.version)
    call(dns_record_service.remove_dns_record, req_pb, LOGIN)

    for a in checker:
        with a:
            resp_pb = call(dns_record_service.list_dns_records, api_pb2.ListDnsRecordsRequest(namespace_id='eee'),
                           LOGIN)
            assert len(resp_pb.dns_records) == 1
            assert resp_pb.dns_records[0].spec.deleted

    # Revisions
    for i in range(3):
        create_backend(zk_storage, cache, 'aaa', 'test_id_{}'.format(3 + i), model_pb2.BackendSelector.YP_ENDPOINT_SETS)
        req_pb = api_pb2.UpdateDnsRecordRequest()
        req_pb.meta.id = 'aaa'
        req_pb.meta.namespace_id = 'aaa'
        req_pb.meta.version = dns_record_pbs['aaa'].meta.version
        req_pb.spec.CopyFrom(dns_record_pbs['aaa'].spec)
        req_pb.spec.address.backends.backends.add(namespace_id='aaa', id='test_id_{}'.format(3 + i))
        dns_record_pbs['aaa'] = call(dns_record_service.update_dns_record, req_pb, LOGIN).dns_record

    req_pb = api_pb2.ListDnsRecordRevisionsRequest(namespace_id='aaa', id='aaa')
    resp_pb = call(dns_record_service.list_dns_record_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert set(rev.meta.dns_record_id for rev in resp_pb.revisions) == {'aaa'}
    assert len(resp_pb.revisions) == 4
    assert resp_pb.revisions[0].meta.id == dns_record_pbs['aaa'].meta.version

    req_pb = api_pb2.ListDnsRecordRevisionsRequest(namespace_id='aaa', id='aaa', skip=2)
    resp_pb = call(dns_record_service.list_dns_record_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 2

    req_pb = api_pb2.ListDnsRecordRevisionsRequest(namespace_id='aaa', id='aaa', skip=2, limit=1)
    resp_pb = call(dns_record_service.list_dns_record_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 1


def test_list_name_servers(dao, cache, checker):
    dao.create_default_name_servers()
    dao.create_default_name_servers()  # check idempotence
    wait_until(lambda: len(cache.list_all_name_servers(INFRA_NAMESPACE_ID)) > 0)

    resp_pb = call(dns_record_service.list_name_servers, api_pb2.ListNameServersRequest(), LOGIN)
    assert resp_pb.total == len(DEFAULT_NAME_SERVERS)
    assert len(resp_pb.name_servers) == len(DEFAULT_NAME_SERVERS)

    name_servers = {pb.meta.id: pb for pb in resp_pb.name_servers}
    for ns_id, (ns_type, ns_selector) in six.iteritems(DEFAULT_NAME_SERVERS):
        ns_pb = name_servers.get(ns_id)
        assert ns_id is not None
        assert ns_pb.meta.namespace_id == INFRA_NAMESPACE_ID
        assert ns_pb.spec.zone == ns_id
        assert ns_pb.spec.type == ns_type
        assert ns_pb.spec.selector == ns_selector


def test_get_name_server_config(dao, zk_storage, cache, namespace, create_dns_record_request_pb):
    dao.create_default_name_servers()
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
    call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    wait_until(lambda: len(cache.list_all_dns_records(name_server_full_id=('infra', 'in.yandex-team.ru'))) == 1)

    es_pb = model_pb2.EndpointSet()
    es_pb.meta.id = 'some_back'
    es_pb.meta.namespace_id = NS_ID
    es_pb.spec.instances.add(host='y.ru', port=8080, weight=-1, ipv6_addr='::1')
    dao.create_endpoint_set(meta_pb=es_pb.meta, spec_pb=es_pb.spec, login=LOGIN)
    es_pb = wait_until(lambda: cache.must_get_endpoint_set(NS_ID, 'some_back'))

    for dns_state_pb in zk_storage.update_dns_record_state(NS_ID, NS_ID):
        rev_pb = dns_state_pb.backends['some_back'].statuses.add()
        rev_pb.revision_id = 'yyy'
        rev_pb.validated.status = 'True'
        rev_pb = dns_state_pb.endpoint_sets['some_back'].statuses.add()
        rev_pb.revision_id = es_pb.meta.version
        rev_pb.validated.status = 'True'
    wait_until(lambda: cache.must_get_dns_record_state(NS_ID, NS_ID).backends)

    get_req_pb = api_pb2.GetNameServerConfigRequest(namespace_id='infra', id='in.yandex-team.ru')
    resp_pb = call(dns_record_service.get_name_server_config, get_req_pb, LOGIN)

    assert resp_pb.zone == 'in.yandex-team.ru'
    assert len(resp_pb.records) == 1
    record_pb = resp_pb.records[0]
    assert record_pb.type == record_pb.ADDRESS
    assert record_pb.address.zone == 'hyperspace'
    assert record_pb.address.ipv6_addrs[0].value == '::1'

    create_backend(zk_storage, cache, NS_ID, 'some_back_2', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    create_dns_record_request_pb.meta.id = NS_ID + '_2'
    create_dns_record_request_pb.spec.address.zone = 'hyperspace.2'
    del create_dns_record_request_pb.spec.address.backends.backends[:]
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back_2')
    call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    wait_until(lambda: len(cache.list_all_dns_records(name_server_full_id=('infra', 'in.yandex-team.ru'))) == 2)

    resp_pb = call(dns_record_service.get_name_server_config, get_req_pb, LOGIN)

    assert len(resp_pb.records) == 1  # dns record state for second record is still not updated

    es_pb = model_pb2.EndpointSet()
    es_pb.meta.id = 'some_back_2'
    es_pb.meta.namespace_id = NS_ID
    es_pb.spec.instances.add(host='y.ru', port=8080, weight=-1, ipv6_addr='::2')
    dao.create_endpoint_set(meta_pb=es_pb.meta, spec_pb=es_pb.spec, login=LOGIN)
    es_pb = wait_until(lambda: cache.must_get_endpoint_set(NS_ID, 'some_back_2'))
    for dns_state_pb in zk_storage.update_dns_record_state(NS_ID, NS_ID + '_2'):
        rev_pb = dns_state_pb.backends['some_back_2'].statuses.add()
        rev_pb.revision_id = 'yyy'
        rev_pb.validated.status = 'True'
        rev_pb = dns_state_pb.endpoint_sets['some_back_2'].statuses.add()
        rev_pb.revision_id = es_pb.meta.version
        rev_pb.validated.status = 'True'
    wait_until(lambda: cache.must_get_dns_record_state(NS_ID, NS_ID + '_2').backends)

    resp_pb = call(dns_record_service.get_name_server_config, get_req_pb, LOGIN)
    assert resp_pb.zone == 'in.yandex-team.ru'
    assert len(resp_pb.records) == 2
    record_pbs = sorted(resp_pb.records, key=lambda r_pb: r_pb.address.zone)
    record_pb = record_pbs[0]
    assert record_pb.type == record_pb.ADDRESS
    assert record_pb.address.zone == 'hyperspace'
    assert record_pb.address.ipv6_addrs[0].value == '::1'
    record_pb = record_pbs[1]
    assert record_pb.type == record_pb.ADDRESS
    assert record_pb.address.zone == 'hyperspace.2'
    assert record_pb.address.ipv6_addrs[0].value == '::2'

    l3_pb = model_pb2.L3Balancer()
    l3_pb.meta.id = 'some_l3'
    l3_pb.meta.namespace_id = NS_ID
    l3_pb.spec.virtual_servers.add(ip='::3')
    dao.create_l3_balancer(meta_pb=l3_pb.meta, spec_pb=l3_pb.spec, login=LOGIN)
    wait_until(lambda: cache.must_get_l3_balancer(NS_ID, 'some_l3'))

    create_dns_record_request_pb.meta.id = NS_ID + '_3'
    create_dns_record_request_pb.spec.address.zone = 'hyperspace.3'
    del create_dns_record_request_pb.spec.address.backends.backends[:]
    create_dns_record_request_pb.spec.address.backends.type = model_pb2.DnsBackendsSelector.L3_BALANCERS
    create_dns_record_request_pb.spec.address.backends.l3_balancers.add(id='some_l3')
    with mock.patch('awacs.web.validation.dns_record.validate_dns_record_order'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    wait_until(lambda: len(cache.list_all_dns_records(name_server_full_id=('infra', 'in.yandex-team.ru'))) == 3)

    resp_pb = call(dns_record_service.get_name_server_config, get_req_pb, LOGIN)
    assert resp_pb.zone == 'in.yandex-team.ru'
    assert len(resp_pb.records) == 3
    record_pbs = sorted(resp_pb.records, key=lambda r_pb: r_pb.address.zone)
    record_pb = record_pbs[0]
    assert record_pb.type == record_pb.ADDRESS
    assert record_pb.address.zone == 'hyperspace'
    assert record_pb.address.ipv6_addrs[0].value == '::1'
    record_pb = record_pbs[1]
    assert record_pb.type == record_pb.ADDRESS
    assert record_pb.address.zone == 'hyperspace.2'
    assert record_pb.address.ipv6_addrs[0].value == '::2'
    record_pb = record_pbs[2]
    assert record_pb.type == record_pb.ADDRESS
    assert record_pb.address.zone == 'hyperspace.3'
    assert record_pb.address.ipv6_addrs[0].value == '::3'


def test_update_dns_record(zk_storage, cache, mongo_storage, namespace, create_dns_record_request_pb):
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back')

    resp_pb = call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    b_pb = resp_pb.dns_record
    assert b_pb.meta.id == NS_ID
    assert b_pb.meta.version
    initial_version = b_pb.meta.version

    req_pb = api_pb2.UpdateDnsRecordRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = 'xxx'
    req_pb.meta.comment = COMMENT

    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.update_dns_record, req_pb, LOGIN)
    assert six.text_type(e.value) == 'at least one of the "spec" or "meta.auth" fields must be present'

    create_backend(zk_storage, cache, NS_ID, 'new_back_add', model_pb2.BackendSelector.YP_ENDPOINT_SETS)

    req_pb.spec.CopyFrom(b_pb.spec)
    b = model_pb2.DnsBackendsSelector.Backend(namespace_id=NS_ID, id='new_back_add')
    req_pb.spec.address.backends.backends.extend([b])
    req_pb.meta.version = b_pb.meta.version
    call(dns_record_service.update_dns_record, req_pb, LOGIN)

    # test changing name server
    req_pb.spec.address.zone = 'hyperspace-2'
    req_pb.spec.name_server.id = 'in.yandex.net'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.update_dns_record, req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.name_server": cannot change name server for an existing DNS record'

    # test changing zone
    req_pb.spec.name_server.id = 'in.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.update_dns_record, req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.address.zone": cannot change zone for an existing DNS record'

    req_pb = api_pb2.GetDnsRecordRequest(namespace_id=NS_ID, id=NS_ID)
    resp_pb = call(dns_record_service.get_dns_record, req_pb, LOGIN)
    b_pb = resp_pb.dns_record
    assert len(b_pb.spec.address.backends.backends) == 2
    assert b_pb.meta.comment == COMMENT
    assert b_pb.meta.version != initial_version

    # test duplicated zone after update
    req_pb = clone_pb(create_dns_record_request_pb)
    req_pb.meta.id = NS_ID + '_1'
    req_pb.spec.address.zone = 'hyperspace-1'
    b = model_pb2.DnsBackendsSelector.Backend()
    req_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='new_back_add')

    resp_pb = call(dns_record_service.create_dns_record, req_pb, LOGIN)
    req_pb.spec.CopyFrom(resp_pb.dns_record.spec)
    req_pb.meta.version = resp_pb.dns_record.meta.version
    req_pb.spec.address.zone = 'hyperspace'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.update_dns_record, req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec": DNS record for "hyperspace.in.yandex-team.ru" already exists: ' \
                                     '"hyperspace:hyperspace"'

    # test empty backends list
    req_pb.spec.address.zone = 'hyperspace-1'
    del req_pb.spec.address.backends.backends[:]
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.update_dns_record, req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.address.backends.backends" must be set'


def test_balancers_backend(dao, zk_storage, cache, namespace, create_dns_record_request_pb):
    dao.create_default_name_servers()

    create_dns_record_request_pb.spec.address.backends.type = model_pb2.DnsBackendsSelector.BALANCERS
    create_dns_record_request_pb.spec.address.backends.balancers.add(id='test_id')

    with pytest.raises(exceptions.NotFoundError, match=r'Balancer "hyperspace:test_id" not found'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    b_pb = model_pb2.Balancer()
    b_pb.meta.id = 'test_id'
    b_pb.meta.namespace_id = NS_ID
    b_pb.spec.deleted = True
    zk_storage.create_balancer(NS_ID, 'test_id', b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'test_id'))

    with pytest.raises(exceptions.BadRequestError,
                       match=r'"spec.address.backends.balancers\[0\]": balancer "test_id" is marked as removed '
                             r'and cannot be used'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    for b_pb in zk_storage.update_balancer(NS_ID, 'test_id'):
        b_pb.spec.deleted = False
    assert wait_until(lambda: not cache.must_get_balancer(NS_ID, 'test_id').spec.deleted)

    with pytest.raises(exceptions.BadRequestError,
                       match=r'"spec.address.backends.balancers\[0\]": backend for balancer "test_id" not found'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    b_pb = model_pb2.Backend()
    b_pb.meta.id = 'test_id'
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.auth.type = b_pb.meta.auth.STAFF
    b_pb.meta.is_system.value = True
    b_pb.spec.selector.type = model_pb2.BackendSelector.BALANCERS
    b_pb.spec.selector.balancers.add(id='test_id')
    b_pb.spec.deleted = True
    zk_storage.create_backend(NS_ID, 'test_id', b_pb)
    wait_until_passes(lambda: cache.must_get_backend(NS_ID, 'test_id'))

    with pytest.raises(exceptions.BadRequestError,
                       match=r'"spec.address.backends.balancers\[0\]": backend "test_id" is marked '
                             r'as removed and cannot be used'):
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)

    for b_pb in zk_storage.update_backend(NS_ID, 'test_id'):
        b_pb.spec.deleted = False
    assert wait_until(lambda: not cache.must_get_backend(NS_ID, 'test_id').spec.deleted)

    resp_pb = call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    b_pb = resp_pb.dns_record
    assert b_pb.meta.id == NS_ID
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]
    assert b_pb.spec.address.backends.type == model_pb2.DnsBackendsSelector.BALANCERS
    assert b_pb.spec.address.backends.balancers[0].id == 'test_id'

    req_pb = api_pb2.GetDnsRecordRequest(namespace_id=NS_ID, id=NS_ID)
    resp_pb = call(dns_record_service.get_dns_record, req_pb, LOGIN)
    dns_pb = resp_pb.dns_record
    assert dns_pb.spec.address.backends.type == model_pb2.DnsBackendsSelector.BALANCERS
    assert dns_pb.spec.address.backends.balancers[0].id == 'test_id'

    # allow keeping deleted balancers/backends if they were already present before update
    for b_pb in zk_storage.update_backend(NS_ID, 'test_id'):
        b_pb.spec.deleted = True
    for b_pb in zk_storage.update_balancer(NS_ID, 'test_id'):
        b_pb.spec.deleted = True
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = 'test_id_2'
    b_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer(NS_ID, 'test_id_2', b_pb)
    b_pb = model_pb2.Backend()
    b_pb.meta.id = 'test_id_2'
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.is_system.value = True
    b_pb.spec.selector.type = model_pb2.BackendSelector.BALANCERS
    b_pb.spec.selector.balancers.add(id='test_id_2')
    zk_storage.create_backend(NS_ID, 'test_id_2', b_pb)
    assert wait_until(lambda: cache.must_get_backend(NS_ID, 'test_id').spec.deleted)
    assert wait_until(lambda: cache.must_get_balancer(NS_ID, 'test_id').spec.deleted)
    wait_until_passes(lambda: cache.must_get_backend(NS_ID, 'test_id_2'))
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'test_id_2'))

    req_pb = api_pb2.UpdateDnsRecordRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = dns_pb.meta.version
    req_pb.spec.CopyFrom(dns_pb.spec)
    req_pb.spec.address.backends.Clear()
    req_pb.spec.address.backends.type = model_pb2.DnsBackendsSelector.BALANCERS
    req_pb.spec.address.backends.balancers.add(id='test_id_2')
    call(dns_record_service.update_dns_record, req_pb, LOGIN)

    # don't allow using deleted balancers/backends if real servers type has changed
    req_pb.spec.address.backends.type = model_pb2.DnsBackendsSelector.EXPLICIT
    req_pb.spec.address.backends.backends.add(id='test_id')
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"spec.address.backends.backends\[0\]": backend "test_id" '
                             r'is marked as removed and cannot be used'):
        call(dns_record_service.update_dns_record, req_pb, LOGIN)


def test_l3_balancers_backend(dao, zk_storage, cache, namespace, create_dns_record_request_pb):
    dao.create_default_name_servers()

    # test wrong name server #1
    create_dns_record_request_pb.order.address.zone = 'test'
    create_dns_record_request_pb.order.name_server.namespace_id = 'infra'
    create_dns_record_request_pb.order.name_server.id = 'yandex-team.ru'
    create_dns_record_request_pb.order.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    assert six.text_type(e.value) == '"order.address.backends": must use L3 balancers as a backend ' \
                                     'for name server "yandex-team.ru"'

    # test non-existent L3
    del create_dns_record_request_pb.order.address.backends.backends[:]
    create_dns_record_request_pb.order.address.backends.l3_balancers.add(id='some_l3')
    create_dns_record_request_pb.order.address.backends.type = model_pb2.DnsBackendsSelector.L3_BALANCERS
    with pytest.raises(exceptions.NotFoundError) as e:
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    assert six.text_type(e.value) == 'L3 balancer "hyperspace:some_l3" not found'

    # test incomplete L3
    l3_pb = model_pb2.L3Balancer()
    l3_pb.meta.id = 'some_l3'
    l3_pb.meta.namespace_id = NS_ID
    l3_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    l3_pb.spec.real_servers.backends.add(id='some_back')
    l3_pb.spec.incomplete = True
    zk_storage.create_l3_balancer(NS_ID, 'some_l3', l3_pb)
    wait_until_passes(lambda: cache.must_get_l3_balancer(NS_ID, 'some_l3'))
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    assert six.text_type(e.value) == '"order.address.backends.l3_balancers[0]": ' \
                                     'cannot use L3 balancer "some_l3" that is not created yet'

    # test wrong name server #2
    for l3_pb in zk_storage.update_l3_balancer(NS_ID, 'some_l3', l3_pb):
        l3_pb.spec.incomplete = False
    assert wait_until(lambda: not cache.get_l3_balancer(NS_ID, 'some_l3').spec.incomplete)
    create_dns_record_request_pb.order.name_server.id = 'in.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
    assert six.text_type(e.value) == '"order.address.backends.l3_balancers[0]": cannot use L3 balancer "some_l3"' \
                                     ' that is not fully managed by awacs'

    # test non-admin with DNS_MANAGER zone
    create_dns_record_request_pb.order.name_server.id = 'yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, 'another_login', enable_auth=True)
    assert six.text_type(e.value) == '"order.name_server.id": Only awacs admins can use name server "yandex-team.ru"'


@pytest.mark.parametrize('use_order, prohibit_explicit', [(False, False), (False, True), (True, False), (True, True)])
def test_create_dns_record_with_explicit_selector(create_dns_record_request_pb, zk_storage, cache, namespace, use_order, prohibit_explicit):
    create_backend(zk_storage, cache, NS_ID, 'some_back', model_pb2.BackendSelector.YP_ENDPOINT_SETS)
    if prohibit_explicit:
        for ns_pb in zk_storage.update_namespace(NS_ID):
            ns_pb.spec.easy_mode_settings.prohibit_explicit_dns_selector.value = True

    if use_order:
        create_dns_record_request_pb.order.address.zone = 'test'
        create_dns_record_request_pb.order.name_server.namespace_id = 'infra'
        create_dns_record_request_pb.order.name_server.id = 'in.yandex-team.ru'
        create_dns_record_request_pb.order.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
        create_dns_record_request_pb.order.address.backends.type = create_dns_record_request_pb.order.address.backends.EXPLICIT
    else:
        create_dns_record_request_pb.spec.address.backends.backends.add(namespace_id=NS_ID, id='some_back')
        create_dns_record_request_pb.spec.address.backends.type = create_dns_record_request_pb.spec.address.backends.EXPLICIT
    if prohibit_explicit:
        with pytest.raises(exceptions.ForbiddenError) as e:
            call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
        assert six.text_type(e.value) == 'Only DNS records with BALANCERS or L3_BALANCERS selector can be created in this namespace'
    else:
        call(dns_record_service.create_dns_record, create_dns_record_request_pb, LOGIN)
