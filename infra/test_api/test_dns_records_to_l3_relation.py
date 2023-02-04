# encoding: utf-8
from awacs.model.cache import IAwacsCache
from awacs.model.util import clone_pb
from awacs.web.validation.l3_balancer import validate_remove_l3_balancer_request, validate_update_l3_with_dns_records
import pytest
import inject

from awacs.lib.rpc import exceptions
from awacs.web import dns_record_service
from infra.awacs.proto import api_pb2, model_pb2
from awtest.core import wait_until, wait_until_passes
from awtest.api import call
from awacs.model.zk import IZkStorage


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture(autouse=True)
def default_ns(deps, dao, cache):
    dao.create_default_name_servers()
    wait_until_passes(lambda: cache.must_get_name_server("infra", "rtc.yandex-team.ru"))


NS_ID = 'ns_with_l3_and_dns_records'
LOGIN = 'login'
DNS_RECORD_ID = "dns_record_id"


@pytest.fixture
def ns_id(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = 'ns_with_l3_and_dns_records'
    ns_pb.meta.category = 'ns_with_l3_and_dns_records'
    ns_pb.meta.abc_service_id = 123
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    zk_storage.create_namespace(ns_pb.meta.id, ns_pb)
    wait_until(lambda: cache.get_namespace(ns_pb.meta.id))
    return 'ns_with_l3_and_dns_records'


@pytest.fixture
def dns_record_request_pb_template():
    req_pb = api_pb2.CreateDnsRecordRequest()
    req_pb.meta.id = DNS_RECORD_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = "c"
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.order.name_server.namespace_id = 'infra'
    req_pb.order.name_server.id = 'rtc.yandex-team.ru'
    req_pb.order.address.zone = 'my-zone'
    req_pb.order.address.backends.type = req_pb.order.address.backends.L3_BALANCERS
    return req_pb


@pytest.fixture
def dns_record_template_pb():
    pb = model_pb2.DnsRecord()
    pb.meta.id = DNS_RECORD_ID
    pb.meta.namespace_id = NS_ID
    pb.meta.version = "version"
    pb.meta.comment = "c"
    pb.meta.auth.type = pb.meta.auth.STAFF
    pb.spec.name_server.namespace_id = 'infra'
    pb.spec.name_server.id = 'rtc.yandex-team.ru'
    pb.spec.address.zone = 'my-zone'
    pb.spec.address.backends.type = pb.spec.address.backends.L3_BALANCERS
    return pb


def create_l3_balancer(ns_id, id, fully_managed=True, virtual_services=None):
    l3_pb = model_pb2.L3Balancer()
    l3_pb.meta.namespace_id = ns_id
    l3_pb.meta.id = id
    if fully_managed:
        l3_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS
    else:
        l3_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY
    if virtual_services:
        for vs in virtual_services:
            l3_pb.spec.virtual_servers.add(ip=vs.ip, port=vs.port)
    zk = IZkStorage.instance()
    cache = IAwacsCache.instance()
    zk.create_l3_balancer(ns_id, l3_pb.meta.id, l3_pb)
    wait_until(lambda: cache.get_l3_balancer(ns_id, l3_pb.meta.id))
    return l3_pb


def test_dns_records_with_only_fully_managed_l3(cache, ns_id, dns_record_request_pb_template):
    l3_pb = create_l3_balancer(ns_id=ns_id, id="not_fully_managed_id", fully_managed=False)
    dns_record_request_pb_template.order.address.backends.l3_balancers.add(id=l3_pb.meta.id)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.create_dns_record, dns_record_request_pb_template, LOGIN)
    str(e.value) == ('"spec.address.backends.l3_balancers[0]": cannot use L3 balancer "not_fully_managed_id"'
                     'that is not fully managed by awacs')

    l3_pb = create_l3_balancer(ns_id=ns_id, id="fully_managed_id", fully_managed=True)
    dns_record_request_pb_template.order.address.backends.ClearField("l3_balancers")
    dns_record_request_pb_template.order.address.backends.l3_balancers.add(id=l3_pb.meta.id)
    call(dns_record_service.create_dns_record, dns_record_request_pb_template, LOGIN)
    wait_until(lambda: cache.get_dns_record(ns_id, DNS_RECORD_ID))


def test_l3_balancers_list_immutability_in_dns_records(zk_storage, cache, ns_id, dns_record_template_pb):
    l3_pb_A = create_l3_balancer(ns_id=ns_id, id="l3_A", fully_managed=True)

    dns_record_template_pb.spec.address.backends.l3_balancers.add(id=l3_pb_A.meta.id)
    zk_storage.create_dns_record(ns_id, dns_record_template_pb.meta.id, dns_record_template_pb)
    wait_until(lambda: cache.get_dns_record(ns_id, dns_record_template_pb.meta.id))

    l3_pb_B = create_l3_balancer(ns_id=ns_id, id="l3_B", fully_managed=True)
    dns_record_template_pb.spec.address.backends.l3_balancers.add(id=l3_pb_B.meta.id)
    update_dns_record_req_pb = api_pb2.UpdateDnsRecordRequest(spec=dns_record_template_pb.spec, meta=dns_record_template_pb.meta)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(dns_record_service.update_dns_record, update_dns_record_req_pb, LOGIN)
    assert str(e.value) == '"spec.address.backends.l3_balancers" can not be changed'


def test_forbid_remove_l3_with_dns_records(zk_storage, cache, ns_id, dns_record_template_pb):
    l3_pb = create_l3_balancer(ns_id=ns_id, id="l3", fully_managed=True)

    # OK
    remove_l3_req_pb = api_pb2.RemoveL3BalancerRequest(namespace_id=ns_id, id=l3_pb.meta.id)
    validate_remove_l3_balancer_request(remove_l3_req_pb)

    dns_record_template_pb.spec.address.backends.l3_balancers.add(id=l3_pb.meta.id)
    dns_record_path = ns_id + "/" + dns_record_template_pb.meta.id
    cache._set_dns_record_pb(path=dns_record_path, dns_record_pb=dns_record_template_pb)

    with pytest.raises(exceptions.BadRequestError) as e:
        validate_remove_l3_balancer_request(remove_l3_req_pb)
    assert str(e.value) == "There are dns records which point to this l3 balancer: {}".format(dns_record_path)


def test_virtual_services_list_and_manage_mode_immutability(cache, ns_id, dns_record_template_pb):
    vs_A_pb = model_pb2.L3BalancerSpec.VirtualServer(ip="192.168.0.1", port=80)
    l3_pb = create_l3_balancer(ns_id=ns_id, id="l3", fully_managed=True, virtual_services=[vs_A_pb])

    dns_record_template_pb.spec.address.backends.l3_balancers.add(id=l3_pb.meta.id)
    dns_record_path = ns_id + "/" + dns_record_template_pb.meta.id
    cache._set_dns_record_pb(path=dns_record_path, dns_record_pb=dns_record_template_pb)

    # OK
    old_l3_spec_pb = clone_pb(l3_pb.spec)
    validate_update_l3_with_dns_records(ns_id, l3_pb.meta.id, old_l3_spec_pb, l3_pb.spec)

    l3_pb.spec.virtual_servers.add(ip="8.8.8.8", port=80)

    with pytest.raises(exceptions.BadRequestError) as e:
        validate_update_l3_with_dns_records(ns_id, l3_pb.meta.id, old_l3_spec_pb, l3_pb.spec)
    assert str(e.value) == ('"spec.virtual_servers" can not be changed '
                            'cause there are dns records which point to this l3 balancer: {}'.format(dns_record_path))

    l3_pb.spec.MergeFrom(old_l3_spec_pb)
    l3_pb.spec.config_management_mode = l3_pb.spec.MODE_REAL_SERVERS_ONLY
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_update_l3_with_dns_records(ns_id, l3_pb.meta.id, old_l3_spec_pb, l3_pb.spec)
    assert str(e.value) == ('can not disable fully-managed by awacs mode (spec.config_management_mode=MODE_REAL_AND_VIRTUAL_SERVERS) '
                            'cause there are dns records which point to this l3 balancer: {}'.format(dns_record_path))
