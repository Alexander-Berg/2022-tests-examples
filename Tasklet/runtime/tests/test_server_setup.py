import copy
import pytest

from tasklet.api import tasklet_pb2

from tasklet import runtime
from tasklet.runtime import context


def _get_default_context():
    services_to_cons = {}
    services_info = context._get_services_info_by_domain(tasklet_pb2.Domain.LOCAL)
    for service_name in context._ALWAYS_CONSTRUCT_SERVICES:
        services_to_cons[context._SERVICES_DEFAULT_INFO[service_name]["field_name"]] = copy.deepcopy(services_info[service_name])
        services_to_cons[context._SERVICES_DEFAULT_INFO[service_name]["field_name"]]["name"] = service_name

    ctx_msg = tasklet_pb2.Context()
    for service, service_descr in services_to_cons.items():
        ctx_msg.entries.add().CopyFrom(context._create_service_descr(service_descr['name'], service_descr))

    return ctx_msg


def _get_context_with_2_sheds():
    ctx_msg = _get_default_context()

    ctx_msg.entries.add().CopyFrom(context._create_service_descr(context._SCHEDULER_SERVICE_NAME, {
        "field_name": "sched_loc_2",
        "service_path": "tasklet.api.sched",
        "impl_path": "tasklet.domain.local:LocalScheduler",
    }))

    return ctx_msg


def _get_context_with_2_spy():
    ctx_msg = _get_default_context()

    ctx_msg.entries.add().CopyFrom(context._create_service_descr("Spy", {
        "field_name": "yet_another_spy",
        "service_path": "tasklet.api.spy",
        "impl_path": "tasklet.domain.local:LocalSpy",
    }))

    return ctx_msg


def test_entries_splitting_simple():
    ctx_msg = _get_default_context()

    splitted_entries = runtime.LocalGrpcServerPool._split_ident_entries(ctx_msg.entries)
    assert len(splitted_entries) == 1
    assert len(splitted_entries[0]) == len(ctx_msg.entries)


def test_entries_splitting_2_scheds():
    ctx_msg = _get_context_with_2_sheds()

    splitted_entries = runtime.LocalGrpcServerPool._split_ident_entries(ctx_msg.entries)
    assert len(splitted_entries) == 2
    assert len(splitted_entries[0]) == len(ctx_msg.entries) - 1
    assert len(splitted_entries[1]) == 1


def test_multiserver_refs_one_server():
    ctx = _get_default_context()
    s = runtime.LocalGrpcServerPool()
    s.setup_services(ctx)


def test_multiserver_refs_many_servers():
    ctx = _get_context_with_2_sheds()
    s = runtime.LocalGrpcServerPool()
    s.setup_services(ctx)


def test_error_multiserver_refs_many_servers():
    with pytest.raises(RuntimeError):
        ctx = _get_context_with_2_spy()
        s = runtime.LocalGrpcServerPool()
        s.setup_services(ctx)
