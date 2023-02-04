import logging

import inject
import mock
import pytest
import ujson

from awacs.lib import juggler_client
from awacs.lib.nannyrpcclient import INannyRpcClient
from awacs.lib.staffclient import IStaffClient
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model import events
from awacs.model.namespace.order.ctl import NamespaceOrderCtl as BaseNamespaceOrderCtl
from awacs.model.namespace.order.processors import (
    ValidatingNamespace,
    WaitingForBalancersToBeInProgress,
    ActivatingNannyServices,
    ValidatingDnsRecord,
)
from infra.awacs.proto import model_pb2
from awtest.mocks.staff_client import StaffMockClient
from awtest.mocks.yp_lite_client import YpLiteMockClient
from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
from awtest import wait_until, check_log, wait_until_passes


NS_ID = "namespace-id"


class NamespaceOrderCtl(BaseNamespaceOrderCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(NamespaceOrderCtl, self).__init__(*args, **kwargs)
        self._state_runner.processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture
def juggler_client_mock():
    m = mock.Mock(juggler_client.JugglerClient)
    m.get_telegram_subscribers.side_effect = lambda logins: {
        login: None for login in logins
    }
    return m


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, juggler_client_mock, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(IYpLiteRpcClient, YpLiteMockClient())
        b.bind(INannyRpcClient, NannyRpcMockClient())
        b.bind(IStaffClient, StaffMockClient())
        b.bind(juggler_client.IJugglerClient, juggler_client_mock)
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def ctl(cache, zk_storage, namespace_pb):
    ctl = NamespaceOrderCtl(NS_ID)
    ctl._pb = namespace_pb
    return ctl


@pytest.fixture
def namespace_pb(cache, zk_storage):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.order.content.certificate_order_content.ca_name = "Internal"
    ns_pb.order.content.certificate_order_content.common_name = "a.yandex.ru"
    ns_pb.order.content.dns_record_request.default.zone = "in.yandex-team.ru"
    ns_pb.order.content.dns_record_request.default.name_server.namespace_id = "infra"
    ns_pb.order.content.dns_record_request.default.name_server.id = "in.yandex-team.ru"
    ns_pb.spec.incomplete = True
    ns_pb.spec.preset = model_pb2.NamespaceSpec.PR_DEFAULT
    zk_storage.create_namespace(namespace_id=NS_ID, namespace_pb=ns_pb)
    assert wait_until(lambda: cache.get_namespace(NS_ID), timeout=1)
    return ns_pb


def update_ns(cache, zk_storage, ns_pb, check):
    for pb in zk_storage.update_namespace(NS_ID):
        pb.CopyFrom(ns_pb)
    wait_ns(cache, check)


def wait_ns(cache, check):
    assert wait_until(lambda: check(cache.get_namespace(NS_ID)), timeout=1)
    return cache.get_namespace(NS_ID)


def test_old_event_generation(caplog, ctx, ctl):
    event = events.NamespaceUpdate(path="", pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert "Skipped event with stale generation -1" in log.records_text()
        assert 'Assigned initial state "START"' not in log.records_text()


def test_not_started(caplog, ctx, ctl):
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "START"' in log.records_text()


def test_finished(caplog, cache, zk_storage, ctx, ctl, namespace_pb):
    namespace_pb.order.status.status = "FINISHED"
    update_ns(
        cache,
        zk_storage,
        namespace_pb,
        check=lambda pb: pb.order.status.status == "FINISHED",
    )
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Status is already FINISHED, nothing to process" in log.records_text()


def test_cancelling(caplog, dao, cache, zk_storage, ctx, ctl, namespace_pb):
    namespace_pb.order.progress.state.id = "WAITING_FOR_BALANCERS_TO_ALLOCATE"
    namespace_pb.order.status.status = "IN_PROGRESS"
    namespace_pb.order.cancelled.value = True
    update_ns(
        cache, zk_storage, namespace_pb, check=lambda pb: pb.order.cancelled.value
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: WAITING_FOR_BALANCERS_TO_ALLOCATE" in log.records_text()
        assert "Processed, next state: CANCELLED" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CANCELLED")


def test_transitions_easy_mode(caplog, dao, cache, zk_storage, ctx, ctl, namespace_pb):
    namespace_pb.order.progress.state.id = "START"
    namespace_pb.order.content.flow_type = model_pb2.NamespaceOrder.Content.YP_LITE
    namespace_pb.order.content.backends["backend"].SetInParent()
    namespace_pb.order.content.alerting_simple_settings.notify_staff_group_id = 1
    update_ns(
        cache,
        zk_storage,
        namespace_pb,
        check=lambda pb: pb.order.progress.state.id == "START",
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: START" in log.records_text()
        assert "Processed, next state: CREATING_BALANCERS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_BALANCERS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_BALANCERS" in log.records_text()
        assert (
            "Processed, next state: WAITING_FOR_BALANCERS_TO_ALLOCATE"
            in log.records_text()
        )
    wait_ns(
        cache,
        lambda pb: pb.order.progress.state.id == "WAITING_FOR_BALANCERS_TO_ALLOCATE",
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: WAITING_FOR_BALANCERS_TO_ALLOCATE" in log.records_text()
        assert "Processed, next state: WAITING_FOR_BALANCERS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "WAITING_FOR_BALANCERS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: WAITING_FOR_BALANCERS" in log.records_text()
        assert "Processed, next state: CREATING_BACKENDS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_BACKENDS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_BACKENDS" in log.records_text()
        assert "Processed, next state: CREATING_UPSTREAMS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_UPSTREAMS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_UPSTREAMS" in log.records_text()
        assert "Processed, next state: CREATING_CERTS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_CERTS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_CERTS" in log.records_text()
        assert "Processed, next state: WAITING_FOR_CERTS" in log.records_text()
    ns_pb = wait_ns(cache, lambda pb: pb.order.progress.state.id == "WAITING_FOR_CERTS")

    ns_pb.order.progress.context["completed_cert_ids"] = ujson.dumps(
        {"a.yandex.ru": True}
    )
    update_ns(
        cache,
        zk_storage,
        ns_pb,
        check=lambda pb: pb.order.progress.context["completed_cert_ids"]
        == '{"a.yandex.ru":true}',
    )
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: WAITING_FOR_CERTS" in log.records_text()
        assert "Processed, next state: VALIDATING_AWACS_NAMESPACE" in log.records_text()
    wait_ns(
        cache, lambda pb: pb.order.progress.state.id == "VALIDATING_AWACS_NAMESPACE"
    )

    with check_log(caplog) as log, mock.patch.object(
        ValidatingNamespace, "_is_namespace_valid", return_value=True
    ):
        ctl._process(ctx)
        assert "Current state: VALIDATING_AWACS_NAMESPACE" in log.records_text()
        assert (
            "Processed, next state: UNPAUSING_BALANCER_CONFIG_UPDATES"
            in log.records_text()
        )
    wait_ns(
        cache,
        lambda pb: pb.order.progress.state.id == "UNPAUSING_BALANCER_CONFIG_UPDATES",
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: UNPAUSING_BALANCER_CONFIG_UPDATES" in log.records_text()
        assert (
            "Processed, next state: WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS"
            in log.records_text()
        )
    wait_ns(
        cache,
        lambda pb: pb.order.progress.state.id
        == "WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS",
    )

    with check_log(caplog) as log, mock.patch.object(
        WaitingForBalancersToBeInProgress,
        "_is_namespace_in_progress",
        return_value=True,
    ):
        ctl._process(ctx)
        assert (
            "Current state: WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS"
            in log.records_text()
        )
        assert "Processed, next state: ACTIVATING_NANNY_SERVICES" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "ACTIVATING_NANNY_SERVICES")

    with check_log(caplog) as log, mock.patch.object(
        ActivatingNannyServices, "_activate_balancer_snapshot"
    ):
        ctl._process(ctx)
        assert "Current state: ACTIVATING_NANNY_SERVICES" in log.records_text()
        assert "Processed, next state: CREATING_DNS_RECORD" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_DNS_RECORD")

    dao.create_default_name_servers()
    wait_until_passes(lambda: cache.must_get_name_server("infra", "in.yandex-team.ru"))
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_DNS_RECORD" in log.records_text()
        assert "Processed, next state: VALIDATING_DNS_RECORD" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "VALIDATING_DNS_RECORD")

    with check_log(caplog) as log, mock.patch.object(
        ValidatingDnsRecord, "_is_dns_record_valid", return_value=True
    ):
        ctl._process(ctx)
        assert "Current state: VALIDATING_DNS_RECORD" in log.records_text()
        assert (
            "Processed, next state: ENABLING_NAMESPACE_ALERTING" in log.records_text()
        )
    wait_ns(
        cache, lambda pb: pb.order.progress.state.id == "ENABLING_NAMESPACE_ALERTING"
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: ENABLING_NAMESPACE_ALERTING" in log.records_text()
        assert "Processed, next state: FINALIZING" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "FINALIZING")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: FINALIZING" in log.records_text()
        assert "Processed, next state: FINISHED" in log.records_text()
    ns_pb = wait_ns(cache, lambda pb: pb.order.progress.state.id == "FINISHED")
    assert not ns_pb.spec.incomplete


def test_transitions_easy_mode_with_domains(
    caplog, dao, cache, zk_storage, ctx, ctl, namespace_pb
):
    namespace_pb.order.progress.state.id = "START"
    namespace_pb.order.content.flow_type = model_pb2.NamespaceOrder.Content.QUICK_START
    namespace_pb.order.content.certificate_order_content.common_name = "test"
    namespace_pb.order.content.endpoint_sets.add(cluster="sas", id="test")
    namespace_pb.order.content.alerting_simple_settings.notify_staff_group_id = 1
    update_ns(
        cache,
        zk_storage,
        namespace_pb,
        check=lambda pb: pb.order.progress.state.id == "START",
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: START" in log.records_text()
        assert "Processed, next state: CREATING_BALANCERS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_BALANCERS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_BALANCERS" in log.records_text()
        assert (
            "Processed, next state: WAITING_FOR_BALANCERS_TO_ALLOCATE"
            in log.records_text()
        )
    wait_ns(
        cache,
        lambda pb: pb.order.progress.state.id == "WAITING_FOR_BALANCERS_TO_ALLOCATE",
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: WAITING_FOR_BALANCERS_TO_ALLOCATE" in log.records_text()
        assert "Processed, next state: WAITING_FOR_BALANCERS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "WAITING_FOR_BALANCERS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: WAITING_FOR_BALANCERS" in log.records_text()
        assert "Processed, next state: CREATING_BACKENDS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_BACKENDS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_BACKENDS" in log.records_text()
        assert "Processed, next state: CREATING_UPSTREAMS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_UPSTREAMS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_UPSTREAMS" in log.records_text()
        assert "Processed, next state: CREATING_DOMAINS" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_DOMAINS")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_DOMAINS" in log.records_text()
        assert "Processed, next state: WAITING_FOR_DOMAINS" in log.records_text()
    ns_pb = wait_ns(
        cache, lambda pb: pb.order.progress.state.id == "WAITING_FOR_DOMAINS"
    )

    ns_pb.order.progress.context["completed_domain_ids"] = ujson.dumps({"test": True})
    update_ns(
        cache,
        zk_storage,
        ns_pb,
        check=lambda pb: pb.order.progress.context["completed_domain_ids"]
        == '{"test":true}',
    )
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: WAITING_FOR_DOMAINS" in log.records_text()
        assert "Processed, next state: VALIDATING_AWACS_NAMESPACE" in log.records_text()
    wait_ns(
        cache, lambda pb: pb.order.progress.state.id == "VALIDATING_AWACS_NAMESPACE"
    )

    with check_log(caplog) as log, mock.patch.object(
        ValidatingNamespace, "_is_namespace_valid", return_value=True
    ):
        ctl._process(ctx)
        assert "Current state: VALIDATING_AWACS_NAMESPACE" in log.records_text()
        assert (
            "Processed, next state: UNPAUSING_BALANCER_CONFIG_UPDATES"
            in log.records_text()
        )
    wait_ns(
        cache,
        lambda pb: pb.order.progress.state.id == "UNPAUSING_BALANCER_CONFIG_UPDATES",
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: UNPAUSING_BALANCER_CONFIG_UPDATES" in log.records_text()
        assert (
            "Processed, next state: WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS"
            in log.records_text()
        )
    wait_ns(
        cache,
        lambda pb: pb.order.progress.state.id
        == "WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS",
    )

    with check_log(caplog) as log, mock.patch.object(
        WaitingForBalancersToBeInProgress,
        "_is_namespace_in_progress",
        return_value=True,
    ):
        ctl._process(ctx)
        assert (
            "Current state: WAITING_FOR_BALANCERS_TO_BE_IN_PROGRESS"
            in log.records_text()
        )
        assert "Processed, next state: ACTIVATING_NANNY_SERVICES" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "ACTIVATING_NANNY_SERVICES")

    with check_log(caplog) as log, mock.patch.object(
        ActivatingNannyServices, "_activate_balancer_snapshot"
    ):
        ctl._process(ctx)
        assert "Current state: ACTIVATING_NANNY_SERVICES" in log.records_text()
        assert "Processed, next state: CREATING_DNS_RECORD" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "CREATING_DNS_RECORD")

    dao.create_default_name_servers()
    wait_until_passes(lambda: cache.must_get_name_server("infra", "in.yandex-team.ru"))
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: CREATING_DNS_RECORD" in log.records_text()
        assert "Processed, next state: VALIDATING_DNS_RECORD" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "VALIDATING_DNS_RECORD")

    with check_log(caplog) as log, mock.patch.object(
        ValidatingDnsRecord, "_is_dns_record_valid", return_value=True
    ):
        ctl._process(ctx)
        assert "Current state: VALIDATING_DNS_RECORD" in log.records_text()
        assert (
            "Processed, next state: ENABLING_NAMESPACE_ALERTING" in log.records_text()
        )
    wait_ns(
        cache, lambda pb: pb.order.progress.state.id == "ENABLING_NAMESPACE_ALERTING"
    )

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: ENABLING_NAMESPACE_ALERTING" in log.records_text()
        assert "Processed, next state: FINALIZING" in log.records_text()
    wait_ns(cache, lambda pb: pb.order.progress.state.id == "FINALIZING")

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert "Current state: FINALIZING" in log.records_text()
        assert "Processed, next state: FINISHED" in log.records_text()
    ns_pb = wait_ns(cache, lambda pb: pb.order.progress.state.id == "FINISHED")
    assert not ns_pb.spec.incomplete
