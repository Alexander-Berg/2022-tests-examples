# coding: utf-8
import logging

import gevent
import inject
import mock
import pytest
from sepelib.core import config as appconfig

from awacs.model import db, alerting
from awacs.model.backend.manager import (
    BackendCtlManager,
    BackendCtlManagerV2,
)
from awacs.model.balancer.manager import (
    BalancerCtlManager,
    BalancerOrderCtlManager,
    BalancerOperationCtlManager,
    BalancerRemovalCtlManager,
    BalancerRemovalCtlManagerV2,
    BalancerOrderCtlManagerV2,
    BalancerOperationCtlManagerV2,
    BalancerCtlManagerV2,
)
from awacs.model.certs.manager import (
    CertCtlManager,
    CertOrderCtlManager,
)
from awacs.model.certs.renewal_manager import (
    CertRenewalCtlManager,
    CertRenewalOrderCtlManager,
)
from awacs.model.dns_records.manager import (
    DnsRecordCtlManager,
    DnsRecordOrderCtlManager,
    DnsRecordRemovalCtlManager,
    DnsRecordOperationCtlManager,
)
from awacs.model.domain.managers import (
    DomainOrderCtlManager,
    DomainCtlManager,
    DomainOperationCtlManager,
    DomainCtlManagerV2,
    DomainOrderCtlManagerV2,
    DomainOperationCtlManagerV2,
)
from awacs.model.errors import ConflictError
from awacs.model.l3_balancer.ctl import L3BalancerCtl
from awacs.model.l3_balancer.ctl_v2 import L3BalancerCtlV2
from awacs.model.l3_balancer.manager import (
    L3BalancerCtlManager,
    L3BalancerOrderCtlManager,
)
from awacs.model.namespace.manager import (
    NamespaceOrderCtlManager,
    NamespaceCtlManager,
    NamespaceCtlManagerV2,
    NamespaceOrderCtlManagerV2,
    NamespaceOperationCtlManager,
)
from awtest import wait_until, wait_until_passes
from infra.awacs.proto import model_pb2
from infra.swatlib.auth import abc
from infra.swatlib.gevent import force_kill_greenlet
from infra.swatlib.gevent.exclusiveservice2 import ExclusiveService


NS_ID = 'namespace-id'


@pytest.fixture(autouse=True)
def deps(caplog, binder):
    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        binder(b)

    caplog.set_level(logging.DEBUG)
    inject.clear_and_configure(configure)
    appconfig.set_value('run.days_until_cert_expiration_to_renew', 1)
    appconfig.set_value('run.ignore_cert_renewal_pause_on_expiration_deadline', False)
    appconfig.set_value('run.allow_automatic_cert_management', False)
    appconfig.set_value('alerting',
                        {'name_prefix': 'test_awacs', 'sync_delay_interval_from': 10, 'sync_delay_interval_to': 1810})
    yield
    inject.clear()


def make_mocking_manager(ctls, manager_cls, zk, cache):
    class MockingCtlManager(manager_cls):
        def _create_ctl(self, ctl_id):
            _ctl = super(MockingCtlManager, self)._create_ctl(ctl_id)

            def _start(*_):
                _ctl._cache.bind(_ctl._callback)
                _ctl._started = True

            def _stop(*_):
                _ctl._cache.unbind(_ctl._callback)
                _ctl._started = False

            _ctl._start = mock.Mock(wraps=_start)
            _ctl._stop = mock.Mock(wraps=_stop)
            _ctl._process_event = mock.Mock(return_value=False)
            _ctl._process_empty_queue = mock.Mock(return_value=False)
            ctls[ctl_id] = _ctl
            return _ctl

    return MockingCtlManager(zk, cache)


def make_mocking_model_manager(manager_cls, zk_client, cache, ctls):
    class MockingModelCtlManager(manager_cls):
        def create_ctl(self, ctl_id):
            ctl = super(MockingModelCtlManager, self).create_ctl(ctl_id)
            ctl.start = mock.MagicMock()
            ctl.stop = mock.MagicMock()
            ctl.process = mock.MagicMock()
            ctls[ctl_id] = ctl
            return ctl

    return MockingModelCtlManager(zk_client=zk_client, cache=cache, allowed_namespace_id_matcher=None)


@pytest.fixture(autouse=True)
def default_ns(deps, dao):
    dao.create_default_name_servers()


def _get_corrected_id(full_entity_id):
    if isinstance(full_entity_id, str):
        return (full_entity_id,)  # workaround for namespace_id
    return full_entity_id


def update_entity(zk_storage, update_func_name, full_entity_id):
    for entity_pb in getattr(zk_storage, update_func_name)(*_get_corrected_id(full_entity_id)):
        entity_pb.meta.version = '999'


def remove_entity(zk_storage, remove_func_name, full_entity_id):
    getattr(zk_storage, remove_func_name)(*_get_corrected_id(full_entity_id))


def finish_order(zk_storage, update_func_name, full_entity_id):
    for entity_pb in getattr(zk_storage, update_func_name)(*_get_corrected_id(full_entity_id)):
        entity_pb.spec.incomplete = False
        entity_pb.order.status.status = 'FINISHED'


def finish_balancer_order(zk_storage, full_entity_id):
    finish_order(zk_storage, 'update_balancer', full_entity_id)


def finish_l3_balancer_order(zk_storage, full_entity_id):
    finish_order(zk_storage, 'update_l3_balancer', full_entity_id)


def finish_cert_order(zk_storage, full_entity_id):
    finish_order(zk_storage, 'update_cert', full_entity_id)


def finish_cert_renewal_order(zk_storage, full_entity_id):
    finish_order(zk_storage, 'update_cert_renewal', full_entity_id)


def finish_domain_order(zk_storage, full_entity_id):
    finish_order(zk_storage, 'update_domain', full_entity_id)


def finish_dns_record_order(zk_storage, full_entity_id):
    finish_order(zk_storage, 'update_dns_record', full_entity_id)


def finish_namespace_order(zk_storage, full_entity_id):
    for ns_pb in zk_storage.update_namespace(full_entity_id):
        ns_pb.order.status.status = 'FINISHED'
        ns_pb.spec.incomplete = False
        ns_pb.spec.alerting.SetInParent()


def mark_as_deleted(zk_storage, update_func_name, full_entity_id):
    for entity_pb in getattr(zk_storage, update_func_name)(*_get_corrected_id(full_entity_id)):
        entity_pb.spec.deleted = True


def mark_balancer_as_deleted(zk_storage, full_entity_id):
    mark_as_deleted(zk_storage, 'update_balancer', full_entity_id)


def mark_dns_record_as_deleted(zk_storage, full_entity_id):
    mark_as_deleted(zk_storage, 'update_dns_record', full_entity_id)


def create_empty_namespace(dao, cache):
    namespace_spec = model_pb2.NamespaceSpec()
    try:
        dao.create_namespace(
            meta_pb=model_pb2.NamespaceMeta(id=NS_ID),
            login='test',
            spec_pb=namespace_spec
        )
        wait_until_passes(lambda: cache.must_get_namespace(NS_ID))
    except ConflictError:
        pass
    return NS_ID


def create_backend(i, zk_storage, dao, cache, selector_type=model_pb2.BackendSelector.NANNY_SNAPSHOTS):
    create_empty_namespace(dao, cache)
    backend_id = 'id-{}'.format(i)
    spec_pb = model_pb2.BackendSpec(
        selector=model_pb2.BackendSelector(
            type=selector_type,
            nanny_snapshots=[model_pb2.BackendSelector.NannySnapshot(service_id='a', snapshot_id='1')]
        )
    )
    zk_storage.create_backend(NS_ID, backend_id, model_pb2.Backend(
        meta=model_pb2.BackendMeta(namespace_id=NS_ID, id=backend_id),
        spec=spec_pb
    ))
    wait_until_passes(lambda: cache.must_get_backend(NS_ID, backend_id))
    return NS_ID, backend_id


def create_balancer(i, zk_storage, dao, cache, ns_id=None):
    if ns_id is None:
        create_empty_namespace(dao, cache)
        ns_id = NS_ID

    balancer_id = 'id-{}'.format(i)
    zk_storage.create_balancer(ns_id, balancer_id, model_pb2.Balancer(
        meta=model_pb2.BalancerMeta(namespace_id=ns_id, id=balancer_id),
        spec=model_pb2.BalancerSpec()
    ))
    wait_until_passes(lambda: cache.must_get_balancer(ns_id, balancer_id))
    return ns_id, balancer_id


def create_removed_balancer(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    balancer_id = 'id-{}'.format(i)
    zk_storage.create_balancer(NS_ID, balancer_id, model_pb2.Balancer(
        meta=model_pb2.BalancerMeta(namespace_id=NS_ID, id=balancer_id),
        spec=model_pb2.BalancerSpec(deleted=True)
    ))
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, balancer_id))
    return NS_ID, balancer_id


def create_balancer_w_order(i, zk_storage, dao, cache, ns_id=None):
    if ns_id is None:
        create_empty_namespace(dao, cache)
        ns_id = NS_ID

    balancer_id = 'id-{}'.format(i)
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.namespace_id = ns_id
    balancer_pb.spec.incomplete = True
    balancer_pb.order.SetInParent()
    zk_storage.create_balancer(ns_id, balancer_id, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(ns_id, balancer_id))
    return ns_id, balancer_id


def create_dns_record(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    dns_record_id = 'id-{}'.format(i)
    dns_record_pb = model_pb2.DnsRecord()
    dns_record_pb.meta.namespace_id = NS_ID
    dns_record_pb.meta.id = dns_record_id
    dns_record_pb.spec.address.zone = 'aaa'
    dns_record_pb.spec.name_server.namespace_id = 'infra'
    dns_record_pb.spec.name_server.id = 'in.yandex.net'
    zk_storage.create_dns_record(NS_ID, dns_record_id, dns_record_pb)
    wait_until_passes(lambda: cache.must_get_dns_record(NS_ID, dns_record_id))
    return NS_ID, dns_record_id


def create_removed_dns_record(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    dns_record_id = 'id-{}'.format(i)
    zk_storage.create_dns_record(NS_ID, dns_record_id, model_pb2.DnsRecord(
        meta=model_pb2.DnsRecordMeta(namespace_id=NS_ID, id=dns_record_id),
        spec=model_pb2.DnsRecordSpec(deleted=True)
    ))
    wait_until_passes(lambda: cache.must_get_dns_record(NS_ID, dns_record_id))
    return NS_ID, dns_record_id


def create_dns_record_w_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    dns_record_id = 'id-{}'.format(i)
    dns_record_pb = model_pb2.DnsRecord()
    dns_record_pb.meta.id = dns_record_id
    dns_record_pb.meta.namespace_id = NS_ID
    dns_record_pb.spec.incomplete = True
    dns_record_pb.order.SetInParent()
    zk_storage.create_dns_record(NS_ID, dns_record_id, dns_record_pb)
    wait_until_passes(lambda: cache.must_get_dns_record(NS_ID, dns_record_id))
    return NS_ID, dns_record_id


def create_dns_record_op(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    dns_record_id = 'id-{}'.format(i)
    dns_record_op_pb = model_pb2.DnsRecordOperation()
    dns_record_op_pb.meta.id = dns_record_id
    dns_record_op_pb.meta.namespace_id = NS_ID
    dns_record_op_pb.spec.incomplete = True
    dns_record_op_pb.order.SetInParent()
    zk_storage.create_dns_record_operation(NS_ID, dns_record_id, dns_record_op_pb)
    wait_until_passes(lambda: cache.must_get_dns_record_operation(NS_ID, dns_record_id))
    return NS_ID, dns_record_id


def create_l3_balancer_wo_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    l3_balancer_id = 'id-{}'.format(i)
    zk_storage.create_l3_balancer(NS_ID, l3_balancer_id, model_pb2.L3Balancer(
        meta=model_pb2.L3BalancerMeta(namespace_id=NS_ID, id=l3_balancer_id),
        spec=model_pb2.L3BalancerSpec(ctl_version=1)
    ))
    wait_until_passes(lambda: cache.must_get_l3_balancer(NS_ID, l3_balancer_id))
    return NS_ID, l3_balancer_id


def create_l3_balancer_w_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    l3_balancer_id = 'id-{}'.format(i)
    zk_storage.create_l3_balancer(NS_ID, l3_balancer_id, model_pb2.L3Balancer(
        meta=model_pb2.L3BalancerMeta(namespace_id=NS_ID, id=l3_balancer_id),
        spec=model_pb2.L3BalancerSpec(incomplete=True, ctl_version=1),
        order=model_pb2.L3BalancerOrder()
    ))
    wait_until_passes(lambda: cache.must_get_l3_balancer(NS_ID, l3_balancer_id))
    return NS_ID, l3_balancer_id


def create_namespace_w_order(i, zk_storage, dao, cache):
    namespace_id = '{}-{}'.format(NS_ID, i)
    dao.create_namespace(
        meta_pb=model_pb2.NamespaceMeta(id=namespace_id),
        login='test',
        order_content_pb=model_pb2.NamespaceOrder.Content(),
    )
    wait_until_passes(lambda: cache.must_get_namespace(namespace_id))
    return namespace_id


def create_namespace_wo_order(i, zk_storage, dao, cache):
    namespace_id = '{}-{}'.format(NS_ID, i)
    namespace_spec = model_pb2.NamespaceSpec()
    namespace_spec.incomplete = False
    namespace_spec.alerting.version = str(alerting.CURRENT_VERSION)
    dao.create_namespace(
        meta_pb=model_pb2.NamespaceMeta(id=namespace_id),
        login='test',
        spec_pb=namespace_spec
    )
    wait_until_passes(lambda: cache.must_get_namespace(namespace_id))
    return namespace_id


def create_cert_wo_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    cert_id = 'id-{}'.format(i)
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = NS_ID
    cert_pb.spec.incomplete = False
    zk_storage.create_cert(NS_ID, cert_id, cert_pb)
    wait_until_passes(lambda: cache.must_get_cert(NS_ID, cert_id))
    return NS_ID, cert_id


def create_cert_w_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    cert_id = 'id-{}'.format(i)
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = NS_ID
    cert_pb.spec.incomplete = True
    cert_pb.order.SetInParent()
    zk_storage.create_cert(NS_ID, cert_id, cert_pb)
    wait_until_passes(lambda: cache.must_get_cert(NS_ID, cert_id))
    return NS_ID, cert_id


def create_cert_renewal_wo_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    cert_renewal_id = 'id-{}'.format(i)
    cert_renewal_pb = model_pb2.CertificateRenewal()
    cert_renewal_pb.meta.id = cert_renewal_id
    cert_renewal_pb.meta.namespace_id = NS_ID
    cert_renewal_pb.meta.target_rev = cert_renewal_id
    cert_renewal_pb.spec.incomplete = False
    cert_rev = model_pb2.CertificateRevision(spec=cert_renewal_pb.spec)
    cert_rev.meta.namespace_id = cert_renewal_pb.meta.namespace_id
    cert_rev.meta.id = cert_renewal_id
    cert_rev.meta.certificate_id = cert_renewal_id
    db.IMongoStorage.instance().save_cert_rev(cert_rev)
    zk_storage.create_cert_renewal(NS_ID, cert_renewal_id, cert_renewal_pb)
    wait_until_passes(lambda: cache.must_get_cert_renewal(NS_ID, cert_renewal_id))
    return NS_ID, cert_renewal_id


def create_cert_renewal_w_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    cert_id = 'id-{}'.format(i)
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = NS_ID
    cert_pb.spec.incomplete = False
    cert_pb = dao.create_cert(meta_pb=cert_pb.meta, spec_pb=cert_pb.spec, login='xxx')
    cert_renewal_pb = model_pb2.CertificateRenewal()
    cert_renewal_pb.meta.id = cert_id
    cert_renewal_pb.meta.target_rev = cert_pb.meta.version
    cert_renewal_pb.meta.namespace_id = NS_ID
    cert_renewal_pb.spec.incomplete = True
    cert_renewal_pb.order.SetInParent()
    zk_storage.create_cert_renewal(NS_ID, cert_id, cert_renewal_pb)
    wait_until_passes(lambda: cache.must_get_cert_renewal(NS_ID, cert_id))
    return NS_ID, cert_id


def create_domain_w_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    domain_id = 'id-{}'.format(i)
    domain_pb = model_pb2.Domain()
    domain_pb.meta.id = domain_id
    domain_pb.meta.namespace_id = NS_ID
    domain_pb.spec.incomplete = True
    domain_pb.order.SetInParent()
    zk_storage.create_domain(NS_ID, domain_id, domain_pb)
    wait_until_passes(lambda: cache.must_get_domain(NS_ID, domain_id))
    return NS_ID, domain_id


def create_domain_wo_order(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    domain_id = 'id-{}'.format(i)
    domain_pb = model_pb2.Domain()
    domain_pb.meta.id = domain_id
    domain_pb.meta.namespace_id = NS_ID
    domain_pb.spec.incomplete = False
    zk_storage.create_domain(NS_ID, domain_id, domain_pb)
    wait_until_passes(lambda: cache.must_get_domain(NS_ID, domain_id))
    return NS_ID, domain_id


def create_domain_op(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    domain_id = 'id-{}'.format(i)
    domain_op_pb = model_pb2.DomainOperation()
    domain_op_pb.meta.id = domain_id
    domain_op_pb.meta.namespace_id = NS_ID
    domain_op_pb.spec.incomplete = True
    domain_op_pb.order.SetInParent()
    zk_storage.create_domain_operation(NS_ID, domain_id, domain_op_pb)
    wait_until_passes(lambda: cache.must_get_domain_operation(NS_ID, domain_id))
    return NS_ID, domain_id


def create_balancer_op(i, zk_storage, dao, cache):
    create_empty_namespace(dao, cache)
    balancer_id = 'id-{}'.format(i)
    balancer_op_pb = model_pb2.BalancerOperation()
    balancer_op_pb.meta.id = balancer_id
    balancer_op_pb.meta.namespace_id = NS_ID
    balancer_op_pb.spec.incomplete = True
    balancer_op_pb.order.SetInParent()
    zk_storage.create_balancer_operation(NS_ID, balancer_id, balancer_op_pb)
    wait_until_passes(lambda: cache.must_get_balancer_operation(NS_ID, balancer_id))
    return NS_ID, balancer_id


def create_namespace_op_pb(i, dao, cache):
    create_empty_namespace(dao, cache)
    op_id = 'id-{}'.format(i)
    ns_op_pb = model_pb2.NamespaceOperation()
    ns_op_pb.meta.id = op_id
    ns_op_pb.meta.namespace_id = NS_ID
    ns_op_pb.spec.incomplete = True
    ns_op_pb.order.SetInParent()
    return ns_op_pb


class _BalancerCtlManager(BalancerCtlManager):
    def __init__(self, coord, cache):
        super(_BalancerCtlManager, self).__init__(coord, cache)


class _BalancerCtlManagerV2(BalancerCtlManagerV2):
    def __init__(self, coord, cache):
        super(_BalancerCtlManagerV2, self).__init__(coord, cache)


class _BalancerOrderCtlManager(BalancerOrderCtlManager):
    def __init__(self, coord, cache):
        super(_BalancerOrderCtlManager, self).__init__(coord, cache)


class _BalancerOrderCtlManagerV2(BalancerOrderCtlManagerV2):
    def __init__(self, coord, cache):
        super(_BalancerOrderCtlManagerV2, self).__init__(coord, cache)


class _BalancerOperationCtlManager(BalancerOperationCtlManager):
    def __init__(self, coord, cache):
        super(_BalancerOperationCtlManager, self).__init__(coord, 'testmemberid', cache)


class _BalancerOperationCtlManagerV2(BalancerOperationCtlManagerV2):
    def __init__(self, coord, cache):
        super(_BalancerOperationCtlManagerV2, self).__init__(coord, cache)


class _BalancerRemovalCtlManager(BalancerRemovalCtlManager):
    def __init__(self, coord, cache):
        super(_BalancerRemovalCtlManager, self).__init__(coord, cache)


class _BalancerRemovalCtlManagerV2(BalancerRemovalCtlManagerV2):
    def __init__(self, coord, cache):
        super(_BalancerRemovalCtlManagerV2, self).__init__(coord, cache)


class _BackendCtlManager(BackendCtlManager):
    def __init__(self, coord, cache):
        super(_BackendCtlManager, self).__init__(coord, 'testmemberid', '', cache)


class _BackendCtlManagerV2(BackendCtlManagerV2):
    def __init__(self, coord, cache):
        super(_BackendCtlManagerV2, self).__init__(coord, 'testmemberid', '', cache)


class _DnsRecordCtlManager(DnsRecordCtlManager):
    def __init__(self, coord, cache):
        super(_DnsRecordCtlManager, self).__init__(coord, cache)


class _DnsRecordOrderCtlManager(DnsRecordOrderCtlManager):
    def __init__(self, coord, cache):
        super(_DnsRecordOrderCtlManager, self).__init__(coord, cache)


class _DnsRecordRemovalCtlManager(DnsRecordRemovalCtlManager):
    def __init__(self, coord, cache):
        super(_DnsRecordRemovalCtlManager, self).__init__(coord, cache)


class _DnsRecordOperationCtlManager(DnsRecordOperationCtlManager):
    def __init__(self, coord, cache):
        super(_DnsRecordOperationCtlManager, self).__init__(coord, cache)


class _NamespaceOrderCtlManager(NamespaceOrderCtlManager):
    def __init__(self, coord, cache):
        super(_NamespaceOrderCtlManager, self).__init__(coord, 'testmemberid', 'p', cache)


class _NamespaceOrderCtlManagerV2(NamespaceOrderCtlManagerV2):
    def __init__(self, coord, cache):
        super(_NamespaceOrderCtlManagerV2, self).__init__(coord, cache)


class _NamespaceCtlManager(NamespaceCtlManager):
    def __init__(self, coord, cache, allowed_namespace_id_matcher=None):
        def namespace_id_matcher(namespace_id):
            return namespace_id != 'infra'  # skip system namespace

        super(_NamespaceCtlManager, self).__init__(coord, 'testmemberid', 'p', cache,
                                                   allowed_namespace_id_matcher=namespace_id_matcher)


class _NamespaceCtlManagerV2(NamespaceCtlManagerV2):
    def __init__(self, coord, cache, allowed_namespace_id_matcher=None):
        def namespace_id_matcher(namespace_id):
            return namespace_id != 'infra'  # skip system namespace

        super(_NamespaceCtlManagerV2, self).__init__(coord, cache,
                                                     allowed_namespace_id_matcher=namespace_id_matcher)


class _DomainCtlManager(DomainCtlManager):
    def __init__(self, coord, cache):
        super(_DomainCtlManager, self).__init__(coord, 'testmemberid', cache)


class _DomainCtlManagerV2(DomainCtlManagerV2):
    def __init__(self, coord, cache):
        super(_DomainCtlManagerV2, self).__init__(coord, cache)


class _DomainOrderCtlManager(DomainOrderCtlManager):
    def __init__(self, coord, cache):
        super(_DomainOrderCtlManager, self).__init__(coord, 'testmemberid', cache)


class _DomainOrderCtlManagerV2(DomainOrderCtlManagerV2):
    def __init__(self, coord, cache):
        super(_DomainOrderCtlManagerV2, self).__init__(coord, cache)


class _DomainOperationCtlManager(DomainOperationCtlManager):
    def __init__(self, coord, cache):
        super(_DomainOperationCtlManager, self).__init__(coord, 'testmemberid', cache)


class _DomainOperationCtlManagerV2(DomainOperationCtlManagerV2):
    def __init__(self, coord, cache):
        super(_DomainOperationCtlManagerV2, self).__init__(coord, cache)


class _CertCtlManager(CertCtlManager):
    def __init__(self, coord, cache):
        super(_CertCtlManager, self).__init__(coord, 'testmemberid', '', cache)


class _CertOrderCtlManager(CertOrderCtlManager):
    def __init__(self, coord, cache):
        super(_CertOrderCtlManager, self).__init__(coord, 'testmemberid', '', cache)


class _CertRenewalCtlManager(CertRenewalCtlManager):
    def __init__(self, coord, cache):
        super(_CertRenewalCtlManager, self).__init__(coord, 'testmemberid', '', cache)


class _CertRenewalOrderCtlManager(CertRenewalOrderCtlManager):
    def __init__(self, coord, cache):
        super(_CertRenewalOrderCtlManager, self).__init__(coord, 'testmemberid', '', cache)


@pytest.mark.parametrize('manager_cls,create_entity,update_func_name,remove_func_name', [
    (_BackendCtlManager, create_backend, 'update_backend', 'remove_backend'),
    (_BackendCtlManagerV2, create_backend, 'update_backend', 'remove_backend'),
    (_BalancerCtlManager, create_balancer, 'update_balancer', 'remove_balancer'),
    (_BalancerCtlManagerV2, create_balancer, 'update_balancer', 'remove_balancer'),
    (_BalancerOrderCtlManager, create_balancer_w_order, 'update_balancer', 'remove_balancer'),
    (_BalancerOrderCtlManagerV2, create_balancer_w_order, 'update_balancer', 'remove_balancer'),
    (_BalancerOperationCtlManager, create_balancer_op, 'update_balancer_operation', 'remove_balancer_operation'),
    (_BalancerOperationCtlManagerV2, create_balancer_op, 'update_balancer_operation', 'remove_balancer_operation'),
    (_BalancerRemovalCtlManager, create_removed_balancer, 'update_balancer', 'remove_balancer'),
    (_BalancerRemovalCtlManagerV2, create_removed_balancer, 'update_balancer', 'remove_balancer'),
    (L3BalancerCtlManager, create_l3_balancer_wo_order, 'update_l3_balancer', 'remove_l3_balancer'),
    (L3BalancerOrderCtlManager, create_l3_balancer_w_order, 'update_l3_balancer', 'remove_l3_balancer'),
    (_DnsRecordCtlManager, create_dns_record, 'update_dns_record', 'remove_dns_record'),
    (_DnsRecordOrderCtlManager, create_dns_record_w_order, 'update_dns_record', 'remove_dns_record'),
    (_DnsRecordRemovalCtlManager, create_removed_dns_record, 'update_dns_record', 'remove_dns_record'),
    (_DnsRecordOperationCtlManager, create_dns_record_op, 'update_dns_record_operation', 'remove_dns_record_operation'),
    (_NamespaceOrderCtlManager, create_namespace_w_order, 'update_namespace', 'remove_namespace'),
    (_NamespaceOrderCtlManagerV2, create_namespace_w_order, 'update_namespace', 'remove_namespace'),
    (_NamespaceCtlManager, create_namespace_wo_order, 'update_namespace', 'remove_namespace'),
    (_NamespaceCtlManagerV2, create_namespace_wo_order, 'update_namespace', 'remove_namespace'),
    (_CertCtlManager, create_cert_wo_order, 'update_cert', 'remove_cert'),
    (_CertOrderCtlManager, create_cert_w_order, 'update_cert', 'remove_cert'),
    (_CertRenewalCtlManager, create_cert_renewal_wo_order, 'update_cert_renewal', 'remove_cert_renewal'),
    (_CertRenewalOrderCtlManager, create_cert_renewal_w_order, 'update_cert_renewal', 'remove_cert_renewal'),
    (_DomainCtlManager, create_domain_wo_order, 'update_domain', 'remove_domain'),
    (_DomainCtlManagerV2, create_domain_wo_order, 'update_domain', 'remove_domain'),
    (_DomainOrderCtlManager, create_domain_w_order, 'update_domain', 'remove_domain'),
    (_DomainOrderCtlManagerV2, create_domain_w_order, 'update_domain', 'remove_domain'),
    (_DomainOperationCtlManager, create_domain_op, 'update_domain_operation', 'remove_domain_operation'),
    (_DomainOperationCtlManagerV2, create_domain_op, 'update_domain_operation', 'remove_domain_operation'),
])
def test_manager(manager_cls, create_entity, update_func_name, remove_func_name, zk, zk_storage, dao, cache):
    ctls = {}

    full_entity_1_id = create_entity(1, zk_storage, dao, cache)
    m = make_mocking_manager(ctls, manager_cls, zk, cache)
    g = gevent.spawn(m.run)

    assert wait_until(lambda: set(m._ctls) == {full_entity_1_id} and ctls[full_entity_1_id]._start.called)

    full_entity_2_id = create_entity(2, zk_storage, dao, cache)
    assert wait_until(lambda: full_entity_2_id in m._ctls)
    ctl = m._ctls[full_entity_2_id]
    assert isinstance(ctl, ExclusiveService)
    assert wait_until(lambda: ctls[full_entity_2_id]._start.called)

    update_entity(zk_storage, update_func_name, full_entity_2_id)
    assert wait_until(lambda: full_entity_2_id in m._ctls)
    ctl = m._ctls[full_entity_2_id]
    assert isinstance(ctl, ExclusiveService)
    assert wait_until(lambda: ctls[full_entity_2_id]._process_event.called)

    remove_entity(zk_storage, remove_func_name, full_entity_2_id)
    assert wait_until(lambda: set(m._ctls) == {full_entity_1_id} and ctls[full_entity_2_id]._stop.called)

    force_kill_greenlet(g)
    assert not m._ctls


@pytest.mark.parametrize(u'manager_cls,create_pb', [
    (NamespaceOperationCtlManager, create_namespace_op_pb),
])
def test_model_manager(ctx, zk, dao, cache, manager_cls, create_pb):
    ctls = {}

    ctx.log.info(u'Step 1: create object in zk, then start manager and check that it starts ctl for the object')
    pb_1 = create_pb(1, dao, cache)
    full_id_1 = (pb_1.meta.namespace_id, pb_1.meta.id)
    manager_cls.model.zk.create(pb_1)

    m = make_mocking_model_manager(manager_cls, zk, cache, ctls)
    g = gevent.spawn(m.run)
    assert wait_until(lambda: full_id_1 in m._ctls and ctls[full_id_1].start.called)
    assert isinstance(m._ctls[full_id_1], ExclusiveService)

    ctx.log.info(u'Step 2: create second object in zk and check that manager starts its ctl')
    pb_2 = create_pb(2, dao, cache)
    full_id_2 = (pb_2.meta.namespace_id, pb_2.meta.id)
    manager_cls.model.zk.create(pb_2)
    assert wait_until(lambda: full_id_2 in m._ctls and ctls[full_id_2].start.called)
    assert isinstance(m._ctls[full_id_2], ExclusiveService)

    ctx.log.info(u'Step 3: remove second object from zk and check that manager stops its ctl')
    manager_cls.model.zk.remove(*full_id_2)
    assert wait_until(lambda: ctls[full_id_2].stop.called)

    ctx.log.info(u'Step 4: force-stop manager and check that it stopped all ctls')
    force_kill_greenlet(g)
    assert not m._ctls


@pytest.mark.parametrize('selector_type', [
    model_pb2.BackendSelector.MANUAL,
    model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD,
])
@pytest.mark.parametrize('manager_class', [
    _BackendCtlManager,
    _BackendCtlManagerV2,
])
def test_backends_ctl_manager(zk, zk_storage, dao, cache, selector_type, manager_class):
    """
    1. Create NANNY_SNAPSHOTS-backends, make sure their ctls are ok and running
    2. Change one of the backends to `selector_type`, make sure its ctl is stopped as
       there is nothing to process
    3. Mark it removed and make sure its ctl gets up and running again to perform
       self-deletion procedure
    4. Remove it from the storage and make sure ctl is stopped
    """
    ctls = {}

    # 1.
    create_backend(0, zk_storage, dao, cache, selector_type=model_pb2.BackendSelector.MANUAL)
    full_backend_1_id = create_backend(1, zk_storage, dao, cache)
    m = make_mocking_manager(ctls, manager_class, zk, cache)
    g = gevent.spawn(m.run)
    # Note: we check that `full_backend_0_id` is not running
    assert wait_until(lambda: set(m._ctls) == {full_backend_1_id} and ctls[full_backend_1_id]._start.called)

    full_backend_2_id = create_backend(2, zk_storage, dao, cache)
    assert wait_until(lambda: full_backend_2_id in m._ctls)
    ctl = m._ctls[full_backend_2_id]
    assert isinstance(ctl, ExclusiveService)
    assert wait_until(lambda: ctls[full_backend_2_id]._start.called)

    # 2.
    for backend_2_pb in zk_storage.update_backend(*full_backend_2_id):
        backend_2_pb.spec.selector.type = selector_type
    assert wait_until(lambda: set(m._ctls) == {full_backend_1_id} and ctls[full_backend_2_id]._stop.called)

    # 3.
    for backend_2_pb in zk_storage.update_backend(*full_backend_2_id):
        backend_2_pb.spec.deleted = True
    assert wait_until(lambda: full_backend_2_id in m._ctls)
    ctl = m._ctls[full_backend_2_id]
    assert isinstance(ctl, ExclusiveService)
    assert wait_until(lambda: ctls[full_backend_2_id]._start.called)

    # 4.
    zk_storage.remove_backend(*full_backend_2_id)
    assert wait_until(lambda: set(m._ctls) == {full_backend_1_id} and ctls[full_backend_2_id]._stop.called)

    # stop everything
    force_kill_greenlet(g)
    assert not m._ctls


def test_namespace_manager_on_balancer_change(zk, zk_storage, dao, cache):
    ns_ctls = {}
    ns_id = create_namespace_wo_order(1, zk_storage, dao, cache)
    m_ns = make_mocking_manager(ns_ctls, _NamespaceCtlManagerV2, zk, cache)
    g_ns = gevent.spawn(m_ns.run)

    assert wait_until(lambda: set(m_ns._ctls) == {ns_id} and ns_ctls[ns_id]._start.called)

    ns_ctls[ns_id]._process_event.reset_mock()

    bal_ctls = {}
    bal_id = create_balancer(2, zk_storage, dao, cache, ns_id)
    m_bal = make_mocking_manager(bal_ctls, _BalancerCtlManagerV2, zk, cache)
    g_bal = gevent.spawn(m_bal.run)

    update_entity(zk_storage, "update_balancer", bal_id)
    assert wait_until(lambda: bal_id in m_bal._ctls)
    assert wait_until(lambda: ns_ctls[ns_id]._process_event.called)

    ns_ctls[ns_id]._process_event.reset_mock()

    remove_entity(zk_storage, "remove_balancer", bal_id)
    assert wait_until(lambda: ns_ctls[ns_id]._process_event.called)
    assert wait_until(lambda: set(m_bal._ctls) == set() and bal_ctls[bal_id]._stop.called)

    # stop everything
    force_kill_greenlet(g_ns)
    force_kill_greenlet(g_bal)


@pytest.mark.parametrize('first_manager,second_manager,create_entity,transition_entity', [
    (_BalancerOrderCtlManager, _BalancerCtlManager, create_balancer_w_order, finish_balancer_order),
    (_BalancerOrderCtlManagerV2, _BalancerCtlManagerV2, create_balancer_w_order, finish_balancer_order),
    (_BalancerCtlManager, _BalancerRemovalCtlManager, create_balancer, mark_balancer_as_deleted),
    (_BalancerCtlManagerV2, _BalancerRemovalCtlManagerV2, create_balancer, mark_balancer_as_deleted),
    (_DnsRecordOrderCtlManager, _DnsRecordCtlManager, create_dns_record_w_order, finish_dns_record_order),
    (_DnsRecordCtlManager, _DnsRecordRemovalCtlManager, create_dns_record, mark_dns_record_as_deleted),
    (L3BalancerOrderCtlManager, L3BalancerCtlManager, create_l3_balancer_w_order, finish_l3_balancer_order),
    (_NamespaceOrderCtlManager, _NamespaceCtlManager, create_namespace_w_order, finish_namespace_order),
    (_NamespaceOrderCtlManagerV2, _NamespaceCtlManagerV2, create_namespace_w_order, finish_namespace_order),
    (_CertOrderCtlManager, _CertCtlManager, create_cert_w_order, finish_cert_order),
    (_CertRenewalOrderCtlManager, _CertRenewalCtlManager, create_cert_renewal_w_order, finish_cert_renewal_order),
    (_DomainOrderCtlManager, _DomainCtlManager, create_domain_w_order, finish_domain_order),
    (_DomainOrderCtlManagerV2, _DomainCtlManagerV2, create_domain_w_order, finish_domain_order),
])
def test_manager_transition(first_manager, second_manager, create_entity, transition_entity, zk,
                            zk_storage, dao, cache):
    ctls = {}

    full_entity_id = create_entity(1, zk_storage, dao, cache)
    m1 = make_mocking_manager(ctls, first_manager, zk, cache)
    m2 = make_mocking_manager(ctls, second_manager, zk, cache)
    g1 = gevent.spawn(m1.run)
    g2 = gevent.spawn(m2.run)
    assert wait_until(lambda: set(m1._ctls) == {full_entity_id} and ctls[full_entity_id]._start.called)
    assert not m2._ctls

    # check that after we transition entity, the second controller is started
    transition_entity(zk_storage, full_entity_id)
    assert wait_until(lambda: set(m2._ctls) == {full_entity_id} and not m1._ctls)

    force_kill_greenlet(g2)
    assert not m2._ctls

    # check that after we transition entity, the first controller is stopped
    full_entity_id_2 = create_entity(2, zk_storage, dao, cache)
    assert wait_until(lambda: set(m1._ctls) == {full_entity_id_2} and ctls[full_entity_id_2]._start.called)
    transition_entity(zk_storage, full_entity_id_2)
    assert wait_until(lambda: ctls[full_entity_id_2]._stop.called)

    force_kill_greenlet(g1)
    assert not m1._ctls


def test_l3_balancer_ctl_version_switching(zk, zk_storage, cache, dao, checker):
    ctls = {}

    full_l3_balancer_id = create_l3_balancer_wo_order(1, zk_storage, dao, cache)
    m = make_mocking_manager(ctls, L3BalancerCtlManager, zk, cache)
    gevent.spawn(m.run)
    for a in checker:
        with a:
            assert set(m._ctls) == {full_l3_balancer_id}
            assert ctls[full_l3_balancer_id]._start.called
            assert isinstance(ctls[full_l3_balancer_id], L3BalancerCtl)

    for l3_pb in zk_storage.update_l3_balancer(*full_l3_balancer_id):
        l3_pb.spec.ctl_version = 2
    assert wait_until(lambda: isinstance(ctls[full_l3_balancer_id], L3BalancerCtlV2))

    # check that after we transition entity, the second controller is started
    for l3_pb in zk_storage.update_l3_balancer(*full_l3_balancer_id):
        l3_pb.spec.ctl_version = 1
    assert wait_until(lambda: isinstance(ctls[full_l3_balancer_id], L3BalancerCtl))
