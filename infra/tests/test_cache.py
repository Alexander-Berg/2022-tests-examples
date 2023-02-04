# coding: utf-8
import itertools
import pytest
import inject

from awacs.lib.rpc import exceptions
from awacs.lib.strutils import flatten_full_id
from awacs.web.validation.dns_record import validate_fqdn
from awacs.model import objects
from awacs.model import cache as cache_lib
from awacs.model.cache import _BalancerStateUpdate, _NamespaceUpdate
from infra.awacs.proto import model_pb2
from awtest.core import wait_until_passes


generations = itertools.count()


@pytest.fixture()
def deps(binder):
    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def to_update_ev(balancer_state_pb):
    return _BalancerStateUpdate(
        objects.L7BalancerStateDescriptor.uid_to_zk_path(balancer_state_pb.namespace_id, balancer_state_pb.balancer_id),
        balancer_state_pb
    )


def to_remove_ev(balancer_state_pb):
    return _BalancerStateUpdate(
        objects.L7BalancerStateDescriptor.uid_to_zk_path(balancer_state_pb.namespace_id, balancer_state_pb.balancer_id),
        None
    )


def test_indexes(cache):
    """
    :type cache: AwacsCache
    :return:
    """
    balancer_1_id = 'balancer-1'
    namespace_1_id = 'namespace-1'
    namespace_2_id = 'namespace-2'
    backend_1_id = 'backend-1'
    backend_2_id = 'backend-2'
    backend_3_id = 'backend-3'

    # process namespace-1/balancer-1 state with included namespace-1/backend-1 and namespace-1/backend-2
    balancer_state_pb = model_pb2.BalancerState(
        namespace_id=namespace_1_id,
        balancer_id=balancer_1_id,
        generation=next(generations),
    )
    balancer_state_pb.backends[backend_1_id].statuses.add()
    balancer_state_pb.backends[backend_2_id].statuses.add()
    ev = to_update_ev(balancer_state_pb)
    cache._process_event(ev)
    # check indexes
    assert cache._full_backend_ids_to_full_balancer_id_sets == {
        (namespace_1_id, backend_1_id): {(namespace_1_id, balancer_1_id)},
        (namespace_1_id, backend_2_id): {(namespace_1_id, balancer_1_id)},
    }
    assert cache._full_balancer_ids_to_full_backend_id_sets == {
        (namespace_1_id, balancer_1_id): {
            (namespace_1_id, backend_1_id),
            (namespace_1_id, backend_2_id),
        }
    }

    # process namespace-2/balancer-1 state with included namespace-1/backend-1 and namespace-2/backend-3
    balancer_state_pb = model_pb2.BalancerState(
        namespace_id=namespace_2_id,
        balancer_id=balancer_1_id,
        generation=next(generations),
    )
    full_backend_id = flatten_full_id(balancer_state_pb.namespace_id, (namespace_1_id, backend_1_id))
    balancer_state_pb.backends[full_backend_id].statuses.add()
    balancer_state_pb.backends[backend_3_id].statuses.add()
    ev = to_update_ev(balancer_state_pb)
    cache._process_event(ev)
    # check indexes
    assert cache._full_backend_ids_to_full_balancer_id_sets == {
        (namespace_1_id, backend_1_id): {
            (namespace_1_id, balancer_1_id),
            (namespace_2_id, balancer_1_id),
        },
        (namespace_1_id, backend_2_id): {
            (namespace_1_id, balancer_1_id),
        },
        (namespace_2_id, backend_3_id): {
            (namespace_2_id, balancer_1_id),
        }
    }
    assert cache._full_balancer_ids_to_full_backend_id_sets == {
        (namespace_1_id, balancer_1_id): {
            (namespace_1_id, backend_1_id),
            (namespace_1_id, backend_2_id)
        },
        (namespace_2_id, balancer_1_id): {
            (namespace_1_id, backend_1_id),
            (namespace_2_id, backend_3_id)
        },
    }

    # process namespace-1/balancer-1 state without included backends
    balancer_state_pb = model_pb2.BalancerState(
        namespace_id=namespace_1_id,
        balancer_id=balancer_1_id,
        generation=next(generations),
    )
    ev = to_update_ev(balancer_state_pb)
    cache._process_event(ev)
    # check indexes
    assert cache._full_backend_ids_to_full_balancer_id_sets == {
        (namespace_1_id, backend_1_id): {
            (namespace_2_id, balancer_1_id),
        },
        (namespace_1_id, backend_2_id): set(),
        (namespace_2_id, backend_3_id): {
            (namespace_2_id, balancer_1_id),
        }
    }
    assert cache._full_balancer_ids_to_full_backend_id_sets == {
        (namespace_1_id, balancer_1_id): set(),
        (namespace_2_id, balancer_1_id): {
            (namespace_1_id, backend_1_id),
            (namespace_2_id, backend_3_id),
        }
    }

    # process removed namespace-2/balancer-1 state
    balancer_state_pb = model_pb2.BalancerState(
        namespace_id=namespace_2_id,
        balancer_id=balancer_1_id,
        generation=next(generations),
    )
    ev = to_remove_ev(balancer_state_pb)
    cache._process_event(ev)
    assert cache._full_backend_ids_to_full_balancer_id_sets == {
        (namespace_1_id, backend_1_id): set(),
        (namespace_1_id, backend_2_id): set(),
        (namespace_2_id, backend_3_id): set(),
    }
    assert cache._full_balancer_ids_to_full_backend_id_sets == {
        (namespace_1_id, balancer_1_id): set(),
    }


def to_namespace_update_ev(namespace_pb):
    return _NamespaceUpdate(
        objects.NamespaceDescriptor.uid_to_zk_path(namespace_pb.meta.id),
        namespace_pb
    )


def to_namespace_remove_ev(namespace_pb):
    return _NamespaceUpdate(
        objects.NamespaceDescriptor.uid_to_zk_path(namespace_pb.meta.id),
        None
    )


def test_normalise_namespace_name_index(cache):
    namespace_pb = model_pb2.Namespace()
    namespace_pb.meta.id = 'namespace-id'
    namespace_pb.meta.generation = 1

    ev = to_namespace_update_ev(namespace_pb)

    cache._process_event(ev)

    assert cache.does_namespace_normalised_name_exist('namespace-id')
    assert cache.does_namespace_normalised_name_exist('namespace.id')
    assert cache.does_namespace_normalised_name_exist('namespace_id')

    ev = to_namespace_remove_ev(namespace_pb)

    cache._process_event(ev)

    assert not cache.does_namespace_normalised_name_exist('namespace-id')


def test_ignoring_outdated_watches(cache):
    # https://st.yandex-team.ru/SWAT-6589 for details
    namespace_1_pb = model_pb2.Namespace()
    namespace_1_pb.meta.id = 'test'
    namespace_1_pb.meta.ctime.FromNanoseconds(1)
    namespace_1_pb.meta.generation = 100

    cache._set_namespace_pb(path='test', namespace_pb=namespace_1_pb)
    assert cache.get_namespace('test') == namespace_1_pb

    # slightly older generation
    namespace_2_pb = model_pb2.Namespace()
    namespace_2_pb.CopyFrom(namespace_1_pb)
    namespace_2_pb.meta.generation = 99

    cache._set_namespace_pb(path='test', namespace_pb=namespace_2_pb)
    assert cache.get_namespace('test') == namespace_1_pb

    # slightly older generation, but slightly newer ctime
    namespace_3_pb = model_pb2.Namespace()
    namespace_3_pb.CopyFrom(namespace_2_pb)
    namespace_3_pb.meta.ctime.FromNanoseconds(2)

    cache._set_namespace_pb(path='test', namespace_pb=namespace_3_pb)
    assert cache.get_namespace('test') == namespace_3_pb


def test_dns_records_wildcards_coverage_indexes(cache, dao, deps):
    dao.create_default_name_servers()
    wait_until_passes(lambda: cache.must_get_name_server("infra",
                                                         "rtc.yandex.net"))

    def make_dns_record_template():
        dns_record_pb = model_pb2.DnsRecord()
        dns_record_pb.spec.name_server.namespace_id = "infra"
        dns_record_pb.spec.name_server.id = "rtc.yandex.net"
        return dns_record_pb

    dns_record_pb_wc_A = make_dns_record_template()
    dns_record_pb_wc_A.spec.address.zone = "*.awacs"
    dns_record_pb_wc_A.meta.namespace_id = "namespace_A"
    dns_record_pb_wc_A.meta.id = "dns_record_A_wc"

    dns_record_pb_wc_B = make_dns_record_template()
    dns_record_pb_wc_B.spec.address.zone = "*.test.sandbox"
    dns_record_pb_wc_B.meta.namespace_id = "namespace_B"
    dns_record_pb_wc_B.meta.id = "dns_record_B_wc"

    dns_record_pb_A = make_dns_record_template()
    dns_record_pb_A.spec.address.zone = "test.nanny.awacs"
    dns_record_pb_A.meta.namespace_id = "namespace_A"
    dns_record_pb_A.meta.id = "dns_record_A"

    dns_record_pb_B = make_dns_record_template()
    dns_record_pb_B.spec.address.zone = "sandbox"
    dns_record_pb_B.meta.namespace_id = "namespace_B"
    dns_record_pb_B.meta.id = "dns_record_B"

    # FIXME create 2 dns records so that they have common node
    dns_record_pb_C = make_dns_record_template()
    dns_record_pb_C.spec.address.zone = "prod.deploy"
    dns_record_pb_C.meta.namespace_id = "namespace_C"
    dns_record_pb_C.meta.id = "dns_record_C"

    dns_record_pb_D = make_dns_record_template()
    dns_record_pb_D.spec.address.zone = "test.deploy"
    dns_record_pb_D.meta.namespace_id = "namespace_D"
    dns_record_pb_D.meta.id = "dns_record_D"

    cache._set_dns_record_pb(path='namespace_A/dns_record_A_wc', dns_record_pb=dns_record_pb_wc_A)
    cache._set_dns_record_pb(path='namespace_B/dns_record_B_wc', dns_record_pb=dns_record_pb_wc_B)
    cache._set_dns_record_pb(path='namespace_A/dns_record_A', dns_record_pb=dns_record_pb_A)
    cache._set_dns_record_pb(path='namespace_B/dns_record_B', dns_record_pb=dns_record_pb_B)
    cache._set_dns_record_pb(path='namespace_C/dns_record_C', dns_record_pb=dns_record_pb_C)
    cache._set_dns_record_pb(path='namespace_D/dns_record_D', dns_record_pb=dns_record_pb_D)

    assert cache._namespace_id_by_dns_record_wildcard == {
        "awacs.rtc.yandex.net": "namespace_A",
        "test.sandbox.rtc.yandex.net": "namespace_B",
    }

    assert cache.namespaces_covered_fqdns_count_by_dns_record_potential_wildcard == {
        "nanny.awacs.rtc.yandex.net": {"namespace_A": 1},
        "awacs.rtc.yandex.net": {"namespace_A": 2},
        "test.sandbox.rtc.yandex.net": {"namespace_B": 1},
        "sandbox.rtc.yandex.net": {"namespace_B": 1},
        "deploy.rtc.yandex.net": {"namespace_C": 1, "namespace_D": 1},
        "rtc.yandex.net": {"namespace_A": 2, "namespace_B": 2, "namespace_C": 1, "namespace_D": 1},
        "yandex.net": {"namespace_A": 2, "namespace_B": 2, "namespace_C": 1, "namespace_D": 1},
        "net": {"namespace_A": 2, "namespace_B": 2, "namespace_C": 1, "namespace_D": 1},
    }

    cache._del_dns_record_pb(path='namespace_A/dns_record_A_wc')

    assert cache.namespaces_covered_fqdns_count_by_dns_record_potential_wildcard == {
        "nanny.awacs.rtc.yandex.net": {"namespace_A": 1},
        "awacs.rtc.yandex.net": {"namespace_A": 1},
        "test.sandbox.rtc.yandex.net": {"namespace_B": 1},
        "sandbox.rtc.yandex.net": {"namespace_B": 1},
        "deploy.rtc.yandex.net": {"namespace_C": 1, "namespace_D": 1},
        "rtc.yandex.net": {"namespace_A": 1, "namespace_B": 2, "namespace_C": 1, "namespace_D": 1},
        "yandex.net": {"namespace_A": 1, "namespace_B": 2, "namespace_C": 1, "namespace_D": 1},
        "net": {"namespace_A": 1, "namespace_B": 2, "namespace_C": 1, "namespace_D": 1},
    }

    cache._del_dns_record_pb(path='namespace_C/dns_record_C')

    assert cache.namespaces_covered_fqdns_count_by_dns_record_potential_wildcard == {
        "nanny.awacs.rtc.yandex.net": {"namespace_A": 1},
        "awacs.rtc.yandex.net": {"namespace_A": 1},
        "test.sandbox.rtc.yandex.net": {"namespace_B": 1},
        "sandbox.rtc.yandex.net": {"namespace_B": 1},
        "deploy.rtc.yandex.net": {"namespace_D": 1},
        "rtc.yandex.net": {"namespace_A": 1, "namespace_B": 2, "namespace_D": 1},
        "yandex.net": {"namespace_A": 1, "namespace_B": 2, "namespace_D": 1},
        "net": {"namespace_A": 1, "namespace_B": 2, "namespace_D": 1},
    }

    with pytest.raises(exceptions.BadRequestError) as e:
        validate_fqdn(name_server_namespace_id="infra", name_server_id="rtc.yandex.net",
                      zone="*.deploy", namespace_id="namespace_F",
                      dns_record_id="dns_record_F", field_name="zone")
    assert str(e.value) == '"zone": There are DNS record(s) with fqdn(s) in external namespace "namespace_D" which are covered by wildcard *.deploy.rtc.yandex.net'

    validate_fqdn(name_server_namespace_id="infra", name_server_id="rtc.yandex.net",
                  zone="*test.deploy", namespace_id="namespace_F",
                  dns_record_id="dns_record_F", field_name="zone")

    with pytest.raises(exceptions.BadRequestError) as e:
        validate_fqdn(name_server_namespace_id="infra", name_server_id="rtc.yandex.net",
                      zone="very-test.very-test.test.sandbox", namespace_id="namespace_F",
                      dns_record_id="dns_record_F", field_name="zone")
    assert str(e.value) == '"zone": There is DNS record in external namespace "namespace_B" with wildcard "*.test.sandbox.rtc.yandex.net" which coveres fqdn very-test.very-test.test.sandbox.rtc.yandex.net'

    # there is no wildcard covering very-prod.prod.deploy.rtc.yandex.net, only fqdn prod.deploy.rtc.yandex.net
    validate_fqdn(name_server_namespace_id="infra", name_server_id="rtc.yandex.net",
                  zone="very-prod.prod.deploy.rtc.yandex.net", namespace_id="namespace_F",
                  dns_record_id="dns_record_F", field_name="zone")
