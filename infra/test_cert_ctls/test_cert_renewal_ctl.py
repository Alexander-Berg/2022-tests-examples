import logging

import inject
import monotonic
import pytest
import time
from mock import Mock
from sepelib.core import config

from awacs.lib import certificator, ya_vault
from awacs.model.certs.renewal_ctl import CertRenewalCtl
from awacs.model.util import SECONDS_IN_DAY
from infra.awacs.proto import model_pb2
from awtest import (wait_until, check_log,
                             wait_until_passes)
from awtest.mocks.appconfig import modified_appconfig
from awtest.mocks.yav import MockYavClient
from awtest.mocks.certificator import MockCertificatorClient


CERT_ID = 'cert-id'
CERT_REV = 'zzz'
NS_ID = 'namespace-id'
BALANCER_ID = 'balancer_pb-id_sas'

FUTURE_DATE = int(time.time()) + SECONDS_IN_DAY * 2


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.INFO)

    def configure(b):
        b.bind(certificator.ICertificatorClient, MockCertificatorClient)
        b.bind(ya_vault.IYaVaultClient, MockYavClient)
        binder(b)

    inject.clear_and_configure(configure)
    config.set_value('run.ignore_cert_renewal_pause_on_expiration_deadline', False)
    yield
    config.set_value('run.ignore_cert_renewal_pause_on_expiration_deadline', False)
    inject.clear()


@pytest.fixture
def ctl(cert_renewal_pb, cert_pb):
    return CertRenewalCtl(NS_ID, CERT_ID)


@pytest.fixture
def cert_pb(cache, zk_storage, mongo_storage):
    cert = model_pb2.Certificate()
    cert.meta.id = CERT_ID
    cert.meta.namespace_id = NS_ID
    cert.meta.version = CERT_REV
    cert.spec.state = model_pb2.CertificateSpec.PRESENT
    cert.spec.storage.ya_vault_secret.secret_id = 'xxx'
    cert.spec.storage.ya_vault_secret.secret_ver = 'yyy'
    cert.spec.fields.version = '123'
    cert.spec.fields.subject_alternative_names.extend(('a.y-t.ru', 'b.y-t.ru'))
    cert.spec.fields.validity.not_after.FromSeconds(FUTURE_DATE)
    cert.spec.fields.signature.algorithm_id = 'sha256_rsa'
    cert.spec.fields.public_key_info.algorithm_id = 'rsa'
    cert.spec.certificator.order_id = 'old_order_id'
    cert_rev = model_pb2.CertificateRevision(spec=cert.spec)
    cert_rev.meta.namespace_id = cert.meta.namespace_id
    cert_rev.meta.id = CERT_REV
    cert_rev.meta.certificate_id = cert.meta.id
    mongo_storage.save_cert_rev(cert_rev)
    zk_storage.create_cert(namespace_id=NS_ID,
                           cert_id=CERT_ID,
                           cert_pb=cert)
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID), timeout=1)
    return cert


@pytest.fixture
def cert_renewal_pb(cache, zk_storage, cert_pb):
    cert_renewal = model_pb2.CertificateRenewal()
    cert_renewal.meta.target_rev = CERT_REV
    cert_renewal.meta.namespace_id = NS_ID
    cert_renewal.meta.id = CERT_ID
    cert_renewal.spec.CopyFrom(cert_pb.spec)
    zk_storage.create_cert_renewal(namespace_id=NS_ID,
                                   cert_renewal_id=CERT_ID,
                                   cert_renewal_pb=cert_renewal)
    assert wait_until(lambda: cache.get_cert_renewal(NS_ID, CERT_ID), timeout=1)
    yield cert_renewal


def test_should_renew_nonexistent(ctx, ctl, caplog, zk_storage):
    zk_storage.remove_cert(NS_ID, CERT_ID)
    with check_log(caplog) as log:
        def check():
            assert not ctl._should_renew(ctx)
            assert 'target certificate does not exist' in log.records_text()
    wait_until_passes(check)

    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)


def test_should_renew_deleted(ctx, ctl, caplog, zk_storage, cache):
    for cert_pb in zk_storage.update_cert(NS_ID, CERT_ID):
        cert_pb.spec.state = model_pb2.CertificateSpec.REMOVED_FROM_AWACS
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).spec.state == model_pb2.CertificateSpec.REMOVED_FROM_AWACS,
                      timeout=1)
    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)
        assert 'target certificate is marked for deletion' in log.records_text()

    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)


def test_should_renew_mismatching_spec(ctx, ctl, caplog, zk_storage, cache):
    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.spec.certificator.order_id = '1'
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).spec.certificator.order_id == '1', timeout=1)
    with pytest.raises(RuntimeError) as e:
        ctl._should_renew(ctx)
    e.match("Current certificate spec doesn't match spec from backup revision")


def test_should_renew_expiring_paused(ctx, ctl, caplog):
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    ctl._pb.meta.paused.value = True

    ctl._not_after = int(time.time())
    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)
        assert 'target cert expires in less than 24h' in log.records_text()
        assert 'but renewal is paused' in log.records_text()


def test_should_renew_expiring_pause_override(ctx, ctl, caplog):
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    ctl._pb.meta.paused.value = True
    ctl._allow_time_pressured_renewal = True

    ctl._not_after = int(time.time())
    with check_log(caplog) as log:
        assert ctl._should_renew(ctx)
        assert 'target cert expires in less than 24h' in log.records_text()
        assert 'forcing spec copy' in log.records_text()


def test_should_renew_expiring_forced_paused(ctx, ctl, caplog, zk_storage, cache):
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    ctl._pb.meta.paused.value = True
    ctl._pb.meta.paused.comment = 'test'
    ctl._pb.meta.paused.author = 'robot'
    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.meta.force_renewal.value = True
    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)
        assert 'renewal is paused by robot with comment "test"' in log.records_text()


def test_should_renew_expiring_forced_unpaused(ctx, ctl, caplog, zk_storage, cache):
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    ctl._pb.meta.paused.value = False
    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.meta.force_renewal.value = True
        pb.meta.force_renewal.comment = 'test'
        pb.meta.force_renewal.author = 'robot'
    wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).meta.force_renewal.value, timeout=1)
    with check_log(caplog) as log:
        assert ctl._should_renew(ctx)
        assert 'renewal is forced by robot with comment "test"' in log.records_text()


def test_should_renew_already_renewed(ctx, ctl, caplog, zk_storage, cache):
    ctl._pb.spec.certificator.order_id = 'new_order_id'

    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.meta.version = 'RENEWED_VERSION'
        pb.spec.fields.validity.not_after.CopyFrom(ctl._pb.spec.fields.validity.not_after)
    wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).meta.version == 'RENEWED_VERSION', timeout=1)
    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)
        assert 'target certificate is already renewed' in log.records_text()


def test_should_renew_bad_version(ctx, ctl, zk_storage, cache):
    ctl._pb.spec.certificator.order_id = 'new_order_id'

    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.meta.version = 'FAKE_VERSION'
        pb.spec.fields.validity.not_after.GetCurrentTime()
    wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).meta.version == 'FAKE_VERSION', timeout=1)
    with pytest.raises(RuntimeError) as e:
        ctl._should_renew(ctx)
    assert e.match('Certificate version "FAKE_VERSION" doesn\'t match expected version "zzz"')


def test_should_renew_not_before(ctx, ctl, caplog):
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    ctl._pb.spec.fields.validity.not_before.FromSeconds(int(time.time()) - 3600)

    ctl._not_after = FUTURE_DATE
    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)
        assert ('renewed cert has been valid for less than "run.cert_renewal_delay_after_issuing" = '
                '{} seconds'.format(SECONDS_IN_DAY)) in log.records_text()

    with modified_appconfig('run.cert_renewal_delay_after_issuing', 1800):
        ctl._renewal_check_deadline = monotonic.monotonic()
        assert ctl._should_renew(ctx)

    ctl._renewal_check_deadline = monotonic.monotonic()
    ctl._pb.spec.fields.validity.not_before.FromSeconds(int(time.time()) - SECONDS_IN_DAY * 2)
    with check_log(caplog) as log:
        assert ctl._should_renew(ctx)
        assert 'ordinary renewal' in log.records_text()


def test_should_renew_paused(ctx, ctl, caplog):
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    ctl._not_after = FUTURE_DATE

    ctl._pb.meta.paused.value = True
    with check_log(caplog) as log:
        assert not ctl._should_renew(ctx)
        assert 'renewal is paused' in log.records_text()

    ctl._renewal_check_deadline = monotonic.monotonic()
    ctl._pb.meta.paused.value = False
    with check_log(caplog) as log:
        assert ctl._should_renew(ctx)
        assert 'ordinary renewal' in log.records_text()


def test_check_algos_twice(ctx, ctl, caplog):
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    ctl._not_after = FUTURE_DATE
    ctl._check_algorithms = Mock()

    assert ctl._should_renew(ctx)
    assert len(ctl._check_algorithms.mock_calls) == 2


def test_algo_mismatch(ctx, ctl, cert_pb, cert_renewal_pb):
    cert_pb.spec.fields.signature.algorithm_id = 'ec'
    with pytest.raises(RuntimeError) as e:
        ctl._check_algorithms(cert_pb, cert_renewal_pb, 'signature')
    assert e.match('signature algorithm mismatch. Original cert has "ec", renewal has "sha256_rsa"')

    cert_pb.spec.fields.signature.algorithm_id = 'rsa'

    cert_pb.spec.fields.public_key_info.algorithm_id = 'ec'
    with pytest.raises(RuntimeError) as e:
        ctl._check_algorithms(cert_pb, cert_renewal_pb, 'public_key_info')
    assert e.match('public_key_info algorithm mismatch. Original cert has "ec", renewal has "rsa"')


def test_algo_params_mismatch(ctx, ctl, cert_pb, cert_renewal_pb):
    cert_pb.spec.fields.signature.parameters = 'sha256'
    cert_renewal_pb.spec.fields.signature.parameters = 'sha1'
    with pytest.raises(RuntimeError) as e:
        ctl._check_algorithms(cert_pb, cert_renewal_pb, 'signature')
    assert e.match(r'signature algorithm mismatch. '
                   r'Original cert has "sha256_rsa \(sha256\)", renewal has "sha256_rsa \(sha1\)"')

    cert_pb.spec.fields.signature.parameters = ''
    cert_renewal_pb.spec.fields.signature.parameters = ''

    cert_pb.spec.fields.public_key_info.parameters = 'secp256r1'
    cert_renewal_pb.spec.fields.public_key_info.parameters = 'another'
    with pytest.raises(RuntimeError) as e:
        ctl._check_algorithms(cert_pb, cert_renewal_pb, 'public_key_info')
    assert e.match(r'public_key_info algorithm mismatch. '
                   r'Original cert has "rsa \(secp256r1\)", renewal has "rsa \(another\)"')


def test_copy_spec_to_target_cert(ctx, ctl, caplog, zk_storage, cache):
    ctl._pb.meta.target_discoverability.default.value = False
    ctl._pb.spec.certificator.order_id = 'new_order_id'
    old_target_spec = model_pb2.CertificateSpec()
    old_target_spec.CopyFrom(ctl._backup_pb.spec)

    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.meta.force_renewal.value = True
        pb.meta.force_renewal.comment = 'test'
        pb.meta.force_renewal.author = 'robot'
    wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).meta.force_renewal.value, timeout=1)

    with check_log(caplog) as log:
        ctl._copy_spec_to_target_cert(ctx)
        assert 'finished updating target cert spec' in log.records_text()

    def check_specs():
        new_cert_pb = cache.get_cert(NS_ID, CERT_ID)
        assert new_cert_pb.spec == ctl._pb.spec
        assert new_cert_pb.spec != old_target_spec
        assert not new_cert_pb.meta.force_renewal.value
        assert not new_cert_pb.meta.force_renewal.comment
        assert not new_cert_pb.meta.force_renewal.author
        assert new_cert_pb.meta.HasField('discoverability')
        assert not new_cert_pb.meta.discoverability.default.value

    wait_until_passes(check_specs)


def test_delete_backup(ctx, ctl, caplog, cache):
    with check_log(caplog) as log:
        ctl._delete_backup(ctx)
        assert 'finished removing old cert from storage' in log.records_text()


def test_delete_empty_backup(ctx, ctl, caplog, cache):
    ctl._backup_pb.spec.storage.ya_vault_secret.secret_id = ''
    ctl._backup_pb.spec.certificator.order_id = ''
    with check_log(caplog) as log:
        ctl._delete_backup(ctx)
        assert 'not removing old cert from storage: no storage.ya_vault_secret.secret_id' in \
               log.records_text()


def test_should_delete_backup(ctx, ctl):
    assert not ctl._should_delete_backup()

    ctl._backup_pb.spec.fields.validity.not_after.FromSeconds(int(time.time()) - 1)
    assert ctl._should_delete_backup()


def test_should_self_delete(ctx, ctl, zk_storage, cache):
    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.spec.fields.validity.not_after.FromSeconds(int(time.time()) - 1)
        pb.spec.certificator.order_id = '1'
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).spec.certificator.order_id == '1', timeout=1)

    assert not ctl._should_self_delete()

    ctl._backup_pb.spec.fields.validity.not_after.FromSeconds(int(time.time()) - 1)
    assert not ctl._should_self_delete()

    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.spec.fields.validity.not_after.FromSeconds(FUTURE_DATE)
        pb.spec.certificator.order_id = '2'
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID).spec.certificator.order_id == '2', timeout=1)
    assert ctl._should_self_delete()


def test_self_delete(ctx, ctl, caplog, cache):
    with check_log(caplog) as log:
        ctl._delete_self(ctx)
        assert 'removing cert renewal from zk' in log.records_text()
    assert wait_until(lambda: not cache.get_cert_renewal(namespace_id=NS_ID, cert_renewal_id=CERT_ID), timeout=1)
