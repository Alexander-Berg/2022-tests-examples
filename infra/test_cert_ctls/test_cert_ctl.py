import logging

import inject
import monotonic
import pytest
import time
from sepelib.core import config

from awacs.model.certs.ctl import CertCtl
from infra.awacs.proto import model_pb2
from awtest import wait_until, check_log


CERT_ID = 'cert-id'
NS_ID = 'namespace-id'
BALANCER_ID = 'balancer_pb-id_sas'


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.INFO)
    inject.clear_and_configure(binder)
    config.set_value('run.allow_automatic_cert_management', False)
    config.set_value('run.days_until_cert_expiration_to_renew', 1)
    yield
    config.set_value('run.days_until_cert_expiration_to_renew', 0)
    inject.clear()


@pytest.fixture
def ctl(cert_pb):
    ctl = CertCtl(NS_ID, CERT_ID)
    ctl._pb = cert_pb
    return ctl


@pytest.fixture
def cert_pb(cache, zk_storage):
    meta = model_pb2.CertificateMeta(id=CERT_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    cert = model_pb2.Certificate(meta=meta)
    zk_storage.create_cert(namespace_id=NS_ID,
                           cert_id=CERT_ID,
                           cert_pb=cert)
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID), timeout=1)
    yield cert
    zk_storage.remove_cert(namespace_id=NS_ID,
                           cert_id=CERT_ID)
    assert wait_until(lambda: not cache.get_cert(NS_ID, CERT_ID), timeout=1)


@pytest.fixture
def cert_renewal_pb(cache, zk_storage):
    meta = model_pb2.CertificateRenewalMeta(id=CERT_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    cert_renewal = model_pb2.CertificateRenewal(meta=meta)
    zk_storage.create_cert_renewal(namespace_id=NS_ID,
                                   cert_renewal_id=CERT_ID,
                                   cert_renewal_pb=cert_renewal)
    assert wait_until(lambda: cache.get_cert_renewal(NS_ID, CERT_ID), timeout=1)
    yield cert_renewal
    zk_storage.remove_cert_renewal(namespace_id=NS_ID,
                                   cert_renewal_id=CERT_ID)
    assert wait_until(lambda: not cache.get_cert_renewal(NS_ID, CERT_ID), timeout=1)


@pytest.fixture
def balancer_state_pb(cache, zk_storage):
    balancer_state_pb = model_pb2.BalancerState(balancer_id=BALANCER_ID, namespace_id=NS_ID)
    balancer_state_pb.certificates[CERT_ID].statuses.add()
    zk_storage.create_balancer_state(namespace_id=NS_ID,
                                     balancer_id=BALANCER_ID,
                                     balancer_state_pb=balancer_state_pb)
    assert wait_until(lambda: cache.list_full_balancer_ids_for_cert(NS_ID, CERT_ID), timeout=1)
    yield balancer_state_pb
    zk_storage.remove_balancer_state(namespace_id=NS_ID, balancer_id=BALANCER_ID)
    assert wait_until(lambda: not cache.list_full_balancer_ids_for_cert(NS_ID, CERT_ID), timeout=1)


@pytest.fixture
def balancer_pb(cache, zk_storage):
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = BALANCER_ID
    balancer_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer(namespace_id=NS_ID,
                               balancer_id=BALANCER_ID,
                               balancer_pb=balancer_pb)
    assert wait_until(lambda: cache.list_all_balancers(NS_ID), timeout=1)
    yield balancer_pb
    zk_storage.remove_balancer(namespace_id=NS_ID, balancer_id=BALANCER_ID)
    assert wait_until(lambda: not cache.list_all_balancers(NS_ID), timeout=1)


def test_cert_needs_renewal(caplog, ctx, ctl, cert_pb):
    assert ctl._renewal_deadline == 86400

    with pytest.raises(RuntimeError) as e:
        ctl._needs_renewal(ctx)
    e.match('Cert has no field "validity.not_after"')

    assert not ctl._needs_renewal(ctx)

    ctl._renewal_check_deadline = monotonic.monotonic()
    cert_pb.spec.fields.validity.not_after.FromSeconds(0)
    with pytest.raises(RuntimeError) as e:
        ctl._needs_renewal(ctx)
    e.match('Cert has "validity.not_after" equal to 0')

    ctl._renewal_check_deadline = monotonic.monotonic()
    cert_pb.spec.fields.validity.not_after.FromSeconds(int(time.time()) + 100000)
    assert not ctl._needs_renewal(ctx)


def test_cert_needs_renewal_expiration(caplog, ctx, ctl, zk_storage, cert_pb):
    cert_pb.spec.fields.validity.not_after.GetCurrentTime()
    for c_pb in zk_storage.update_cert(NS_ID, CERT_ID):
        c_pb.spec.fields.validity.not_after.GetCurrentTime()
    assert ctl._needs_renewal(ctx)

    ctl._renewal_check_deadline = monotonic.monotonic()
    for c_pb in zk_storage.update_cert(NS_ID, CERT_ID):
        c_pb.spec.fields.validity.not_after.FromSeconds(int(time.time()) + 100000)
    assert not ctl._needs_renewal(ctx)


def test_cert_needs_renewal_forced(caplog, ctx, ctl, zk_storage, cert_pb):
    cert_pb.meta.force_renewal.value = True
    cert_pb.meta.force_renewal.comment = 'test'
    cert_pb.meta.force_renewal.author = 'robot'
    for c_pb in zk_storage.update_cert(NS_ID, CERT_ID):
        c_pb.meta.force_renewal.CopyFrom(cert_pb.meta.force_renewal)
    with check_log(caplog) as log:
        assert ctl._needs_renewal(ctx)
        assert 'renewal is forced by robot with comment "test"' in log.records_text()


def test_make_cert_renewal_pb(ctx, ctl, cert_pb):
    with pytest.raises(RuntimeError) as e:
        ctl._make_cert_renewal_pb(ctx)
    e.match('Unknown CA common name "". Supported names: Test-YandexInternal-Ca, Yandex CA, YandexInternalCA')

    for issuer_common_name, ca_name in (
            ('Yandex CA', 'CertumProductionCA'),
            ('Test-YandexInternal-Ca', 'InternalTestCA'),
            ('YandexInternalCA', 'InternalCA')):
        cert_pb.spec.fields.issuer_common_name = issuer_common_name
        pb = ctl._make_cert_renewal_pb(ctx)
        assert pb.order.content.ca_name == ca_name

    for ca_name in ('CertumProductionCA', 'InternalTestCA', 'InternalCA'):
        cert_pb.spec.certificator.ca_name = ca_name
        pb = ctl._make_cert_renewal_pb(ctx)
        assert pb.order.content.ca_name == ca_name

    for algo in ('rsa', 'ec'):
        cert_pb.spec.fields.public_key_info.algorithm_id = algo
        pb = ctl._make_cert_renewal_pb(ctx)
        assert pb.order.content.public_key_algorithm_id == algo

    cert_pb.spec.imported.abc_service_id = 999
    cert_pb.spec.fields.subject_common_name = 'test'
    cert_pb.spec.fields.subject_alternative_names.extend(('test1', 'test2'))
    for _ in range(2):
        allow_automation = bool(config.get_value('run.allow_automatic_cert_management'))
        config.set_value('run.allow_automatic_cert_management', not allow_automation)
        if allow_automation:
            cert_pb.meta.force_renewal.cert_ttl = 90
        ctl._allow_automation = bool(config.get_value('run.allow_automatic_cert_management'))
        pb = ctl._make_cert_renewal_pb(ctx)
        assert pb.meta.id == cert_pb.meta.id
        assert pb.meta.namespace_id == cert_pb.meta.namespace_id
        assert pb.meta.target_rev == cert_pb.meta.version
        assert pb.spec.incomplete
        assert pb.order.content.common_name == 'test'
        assert pb.order.content.abc_service_id == 999
        assert pb.order.content.subject_alternative_names == ['test1', 'test2']
        if ctl._allow_automation:
            assert not pb.meta.paused.value
            assert not pb.order.content.ttl
        else:
            assert pb.meta.paused.value
            assert pb.order.content.ttl == 90

    cert_pb.spec.source = model_pb2.CertificateSpec.CERTIFICATOR
    cert_pb.spec.certificator.abc_service_id = 888
    pb = ctl._make_cert_renewal_pb(ctx)
    assert pb.order.content.abc_service_id == 888


def test_start_renewal(caplog, ctx, ctl, cert_pb):
    cert_pb.spec.fields.issuer_common_name = 'Test-YandexInternal-Ca'
    cert_pb.spec.fields.subject_common_name = 'test'

    with check_log(caplog) as log:
        ctl._start_renewal(ctx)
        assert 'cert renewal successfully started' in log.records_text()

    with check_log(caplog) as log:
        ctl._start_renewal(ctx)
        assert 'cert renewal successfully started' not in log.records_text()


def test_cert_start_renewal_conflict(ctx, cache, ctl, zk_storage, cert_pb, cert_renewal_pb):
    cert_pb.meta.version = 'wrong_version'
    with pytest.raises(RuntimeError) as e:
        ctl._start_renewal(ctx)
    e.match('Renewal already exists, but has invalid target_rev')


def test_ready_to_delete(caplog, zk_storage, ctx, ctl, balancer_state_pb, cache, cert_pb):
    assert not ctl._ready_to_delete(ctx)

    cert_pb.meta.mtime.FromSeconds(0)
    with check_log(caplog) as log:
        assert not ctl._ready_to_delete(ctx)
        expected_error = "cannot delete cert since it's used in balancers: {}".format(BALANCER_ID)
        assert expected_error in log.records_text()

    assert not ctl._ready_to_delete(ctx)

    zk_storage.remove_balancer_state(namespace_id=NS_ID, balancer_id=BALANCER_ID)
    assert wait_until(lambda: not cache.list_full_balancer_ids_for_cert(NS_ID, CERT_ID), timeout=1)

    ctl._self_deletion_check_deadline = monotonic.monotonic()
    assert ctl._ready_to_delete(ctx)


def test_self_delete_is_used(caplog, zk_storage, ctx, ctl, balancer_pb, balancer_state_pb, cache, cert_renewal_pb):
    with pytest.raises(RuntimeError) as e:
        ctl._self_delete(ctx)
    e.match("Critical error: would delete a referenced cert if it wasn't for this raise")
    assert cache.get_cert(namespace_id=NS_ID, cert_id=CERT_ID)

    zk_storage.remove_balancer(namespace_id=NS_ID, balancer_id=BALANCER_ID)
    balancer_pb.meta.namespace_id = 'another-namespace-id'
    zk_storage.create_balancer(balancer_id=balancer_pb.meta.id,
                               namespace_id=balancer_pb.meta.namespace_id,
                               balancer_pb=balancer_pb)
    assert wait_until(lambda: cache.list_all_balancers('another-namespace-id'), timeout=1)
    assert wait_until(lambda: not cache.list_all_balancers(NS_ID), timeout=1)
    with check_log(caplog) as log:
        ctl._self_delete(ctx)
        assert 'removing cert renewal from zk' in log.records_text()
        assert 'removing cert from db' in log.records_text()
    assert wait_until(lambda: not cache.get_cert(namespace_id=NS_ID, cert_id=CERT_ID), timeout=1)
    assert wait_until(lambda: not cache.get_cert_renewal(namespace_id=NS_ID, cert_renewal_id=CERT_ID), timeout=1)


def test_self_delete_full(caplog, zk_storage, ctx, ctl, cert_pb, cache):
    ctl._yav_client.remove_secret = lambda *args, **kwargs: None
    ctl._certificator_client.revoke_certificate = lambda *args, **kwargs: None
    cert_pb.spec.state = model_pb2.CertificateSpec.REVOKED_AND_REMOVED_FROM_AWACS_AND_STORAGE
    cert_pb.spec.storage.ya_vault_secret.secret_id = '123'
    cert_pb.spec.storage.ya_vault_secret.secret_ver = '123'
    cert_pb.spec.certificator.order_id = '123'
    with check_log(caplog) as log:
        ctl._self_delete(ctx)
        assert 'finished revoking cert' in log.records_text()
        assert 'finished removing cert from storage' in log.records_text()
    assert wait_until(lambda: not cache.get_cert(namespace_id=NS_ID, cert_id=CERT_ID), timeout=1)


def test_self_delete_with_copy(caplog, zk_storage, ctx, ctl, cert_pb, cache):
    ctl._yav_client.remove_secret = lambda *args, **kwargs: None
    ctl._certificator_client.revoke_certificate = lambda *args, **kwargs: None
    cert_pb.spec.state = model_pb2.CertificateSpec.REVOKED_AND_REMOVED_FROM_AWACS_AND_STORAGE
    cert_pb.spec.storage.ya_vault_secret.secret_id = '123'
    cert_pb.spec.storage.ya_vault_secret.secret_ver = '123'
    cert_pb.spec.certificator.order_id = '123'
    cert_pb.meta.unrevokable.value = True
    cert_pb.meta.unrevokable.author = 'robot'
    cert_pb.meta.unrevokable.comment = 'comment'
    with check_log(caplog) as log:
        ctl._self_delete(ctx)
        assert 'finished revoking cert' not in log.records_text()
        assert 'not revoking cert, it\'s marked as unrevokable by robot with comment "comment"' in log.records_text()
        assert 'finished removing cert from storage' in log.records_text()
    assert wait_until(lambda: not cache.get_cert(namespace_id=NS_ID, cert_id=CERT_ID), timeout=1)


def test_self_delete_from_storage(caplog, zk_storage, ctx, ctl, cert_pb, cache):
    ctl._yav_client.remove_secret = lambda *args, **kwargs: None
    cert_pb.spec.state = model_pb2.CertificateSpec.REMOVED_FROM_AWACS_AND_STORAGE
    cert_pb.spec.storage.ya_vault_secret.secret_id = '123'
    cert_pb.spec.storage.ya_vault_secret.secret_ver = '123'
    with check_log(caplog) as log:
        ctl._self_delete(ctx)
        assert 'finished revoking cert' not in log.records_text()
        assert 'finished removing cert from storage' in log.records_text()
    assert wait_until(lambda: not cache.get_cert(namespace_id=NS_ID, cert_id=CERT_ID), timeout=1)
