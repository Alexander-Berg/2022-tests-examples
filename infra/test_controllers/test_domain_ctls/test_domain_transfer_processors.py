import logging

import inject
import pytest
import ujson

from awacs.lib import ya_vault
from awacs.lib.order_processor.model import FeedbackMessage
from awacs.model.cache import fqdn_info_tuple
from awacs.model.domain.operations import transfer as p
from infra.awacs.proto import model_pb2, modules_pb2
from awtest import wait_until_passes, wait_until, check_log
from awtest.mocks.yav import MockYavClient


DOMAIN_ID = 'domain_op-id'
CERT_ID = 'cert-id'
NS_ID = 'namespace-id'
NS_ID_2 = 'namespace-id-2'


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(ya_vault.IYaVaultClient, MockYavClient)
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_transfer_pb(cache, zk_storage, protocol):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    domain_pb = model_pb2.DomainOperation(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.transfer.target_namespace_id = NS_ID_2
    domain_pb.order.content.transfer.include_upstreams.type = modules_pb2.ALL
    if protocol != model_pb2.DomainSpec.Config.HTTP_ONLY:
        domain_pb.order.progress.context['cert_id'] = ujson.dumps(CERT_ID)
    zk_storage.create_domain_operation(namespace_id=NS_ID,
                                       domain_id=DOMAIN_ID,
                                       domain_operation_pb=domain_pb)
    wait_domain_op(cache, lambda pb: pb)
    return domain_pb


def create_domain_op(cache, zk_storage, protocol):
    return p.DomainTransfer(create_transfer_pb(cache, zk_storage, protocol))


def create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
                  ns_id=NS_ID, domain_id=DOMAIN_ID, fqdns=('fqdn1.ya.ru',), is_being_transferred=False):
    domain_meta_pb = model_pb2.DomainMeta(id=domain_id, namespace_id=ns_id)
    domain_meta_pb.is_being_transferred.value = is_being_transferred
    domain_pb = model_pb2.Domain(meta=domain_meta_pb)
    domain_pb.spec.yandex_balancer.config.protocol = protocol
    domain_pb.spec.yandex_balancer.config.include_upstreams.type = modules_pb2.BY_ID
    domain_pb.spec.yandex_balancer.config.include_upstreams.ids.extend(['u1', 'u2'])
    domain_pb.spec.yandex_balancer.config.fqdns.extend(fqdns)
    domain_pb.spec.incomplete = False
    if protocol != model_pb2.DomainSpec.Config.HTTP_ONLY:
        domain_pb.spec.yandex_balancer.config.cert.id = CERT_ID
    zk_storage.create_domain(ns_id, domain_id, domain_pb)
    wait_until_passes(lambda: cache.must_get_domain(ns_id, domain_id))
    return domain_pb


def create_cert(cache, zk_storage, ns_id=NS_ID, unrevokable=False, is_being_transferred=False):
    cert_meta_pb = model_pb2.CertificateMeta(id=CERT_ID, namespace_id=ns_id)
    cert_meta_pb.unrevokable.value = unrevokable
    cert_meta_pb.is_being_transferred.value = is_being_transferred
    cert_pb = model_pb2.Certificate(meta=cert_meta_pb)
    cert_pb.spec.fields.serial_number = '120213199433569577268469'
    zk_storage.create_cert(ns_id, CERT_ID, cert_pb)
    wait_until_passes(lambda: cache.must_get_cert(ns_id, CERT_ID))
    return cert_pb


def update_domain_op(cache, zk_storage, domain_op_pb, check):
    for pb in zk_storage.update_domain_operation(NS_ID, DOMAIN_ID):
        pb.CopyFrom(domain_op_pb)
    wait_domain_op(cache, check)


def wait_domain_op(cache, check):
    assert wait_until(lambda: check(cache.get_domain_operation(NS_ID, DOMAIN_ID)))


def wait_domain(cache, check, ns_id=NS_ID):
    assert wait_until(lambda: check(cache.get_domain(ns_id, DOMAIN_ID)))


def wait_cert(cache, check, ns_id=NS_ID):
    assert wait_until(lambda: check(cache.get_cert(ns_id, CERT_ID)))


def test_cache_during_transfer(cache, zk_storage):
    create_domain(cache, zk_storage)
    assert cache.list_domain_fqdns()['ya.ru']['fqdn1'] == {fqdn_info_tuple('domain', NS_ID, DOMAIN_ID)}
    create_domain(cache, zk_storage, ns_id=NS_ID + '_1')
    assert cache.list_domain_fqdns()['ya.ru']['fqdn1'] == {
        fqdn_info_tuple('domain', NS_ID, DOMAIN_ID),
        fqdn_info_tuple('domain', NS_ID + '_1', DOMAIN_ID),
    }
    zk_storage.remove_domain(NS_ID + '_1', DOMAIN_ID)
    wait_until_passes(lambda: cache.get_domain(NS_ID + '_1', DOMAIN_ID) is None)
    assert cache.list_domain_fqdns()['ya.ru']['fqdn1'] == {fqdn_info_tuple('domain', NS_ID, DOMAIN_ID)}


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_start(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, protocol)

    assert p.Started(domain_op).process(ctx).name == 'MARKING_OLD_DOMAIN'
    if protocol == model_pb2.DomainSpec.Config.HTTP_ONLY:
        assert 'cert_id' not in domain_op.context
    else:
        assert domain_op.context['cert_id'] == CERT_ID


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_marking_old_domain(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, protocol)

    next_state = p.MarkingOldDomain(domain_op).process(ctx).name
    wait_domain(cache, check=lambda pb: pb.meta.is_being_transferred.value)
    if protocol == model_pb2.DomainSpec.Config.HTTP_ONLY:
        assert next_state == 'CREATING_NEW_DOMAIN'
    else:
        assert next_state == 'MARKING_OLD_CERT'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_marking_old_cert(mongo_storage, cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage)
    assert mongo_storage.list_cert_revs(NS_ID, CERT_ID).total == 0

    next_state = p.MarkingOldCerts(domain_op).process(ctx).name
    wait_cert(cache, check=lambda pb: pb.meta.unrevokable.value)
    assert next_state == 'CREATING_NEW_CERT_SECRET'
    assert mongo_storage.list_cert_revs(NS_ID, CERT_ID).total == 0


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_creating_new_cert_secret(caplog, cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage)
    with check_log(caplog) as log:
        next_state = p.CreatingNewCertSecrets(domain_op).process(ctx).name
        assert 'secret contains files with inexact name matches' not in log.records_text()
    assert next_state == 'CREATING_NEW_CERT'
    assert domain_op.context['cert_secret_uuid']
    assert domain_op.context['cert_secret_version']


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_creating_new_cert(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage, unrevokable=True, is_being_transferred=True)
    domain_op.context['cert_secret_uuid'] = 'new_secret_id'
    domain_op.context['cert_secret_version'] = 'new_secret_ver'

    next_state = p.CreatingNewCerts(domain_op).process(ctx).name
    assert next_state == 'CREATING_NEW_DOMAIN'

    wait_cert(cache, ns_id=NS_ID_2, check=lambda pb: (
        pb.spec.storage.ya_vault_secret.secret_id == 'new_secret_id'
        and pb.spec.storage.ya_vault_secret.secret_ver == 'new_secret_ver'
        and pb.meta.unrevokable.value
        and pb.meta.is_being_transferred.value))


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_creating_new_domain(checker, cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    old_domain_pb = create_domain(cache, zk_storage, protocol, is_being_transferred=True)

    next_state = p.CreatingNewDomain(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_REMOVAL_APPROVAL'
    for a in checker:
        with a:
            domain_pb = cache.must_get_domain(NS_ID_2, DOMAIN_ID)
            assert domain_pb.meta.is_being_transferred.value
            assert old_domain_pb.spec.yandex_balancer.config.include_upstreams.type == modules_pb2.BY_ID
            assert domain_pb.spec.yandex_balancer.config.include_upstreams.type == modules_pb2.ALL
            assert not domain_pb.spec.yandex_balancer.config.include_upstreams.ids


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_waiting_for_removal_approval(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)

    msg = p.WaitingForRemovalApproval(domain_op).process(ctx)
    assert isinstance(msg, FeedbackMessage)
    assert msg.pb_error_type == model_pb2.DomainOperationOrder.OrderFeedback.WAITING_FOR_REMOVAL_APPROVAL

    domain_op.pb.order.approval.before_removal = True
    next_state = p.WaitingForRemovalApproval(domain_op).process(ctx).name
    assert next_state == 'REMOVING_OLD_DOMAIN'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_removing_old_domain(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, protocol)

    next_state = p.RemovingOldDomain(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_OLD_DOMAIN_REMOVAL'
    wait_domain(cache, check=lambda pb: pb.spec.deleted)

    zk_storage.remove_domain(NS_ID, DOMAIN_ID)
    assert wait_until(lambda: cache.get_domain(NS_ID, DOMAIN_ID) is None)
    next_state = p.RemovingOldDomain(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_OLD_DOMAIN_REMOVAL'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_waiting_for_old_domain_removal(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, protocol)

    next_state = p.WaitingForOldDomainRemoval(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_OLD_DOMAIN_REMOVAL'

    zk_storage.remove_domain(NS_ID, DOMAIN_ID)
    assert wait_until(lambda: cache.get_domain(NS_ID, DOMAIN_ID) is None)
    next_state = p.WaitingForOldDomainRemoval(domain_op).process(ctx).name
    if protocol == model_pb2.DomainSpec.Config.HTTP_ONLY:
        assert next_state == 'UNMARKING_NEW_DOMAIN'
    else:
        assert next_state == 'REMOVING_OLD_CERT'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_removing_old_cert(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage)

    next_state = p.RemovingOldCerts(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_OLD_CERT_REMOVAL'
    wait_cert(cache, check=lambda pb: pb.spec.state == model_pb2.CertificateSpec.REMOVED_FROM_AWACS_AND_STORAGE)

    zk_storage.remove_cert(NS_ID, CERT_ID)
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID) is None)
    next_state = p.RemovingOldCerts(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_OLD_CERT_REMOVAL'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_waiting_for_old_cert_removal(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage)

    next_state = p.WaitingForOldCertsRemoval(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_OLD_CERT_REMOVAL'

    zk_storage.remove_cert(NS_ID, CERT_ID)
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID) is None)
    next_state = p.WaitingForOldCertsRemoval(domain_op).process(ctx).name
    assert next_state == 'UNMARKING_NEW_CERT'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_unmarking_new_cert(mongo_storage, cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage, ns_id=NS_ID_2, unrevokable=True, is_being_transferred=True)
    assert mongo_storage.list_cert_revs(NS_ID, CERT_ID).total == 0

    next_state = p.UnmarkingNewCerts(domain_op).process(ctx).name
    assert next_state == 'UNMARKING_NEW_DOMAIN'
    wait_cert(cache, ns_id=NS_ID_2, check=(
        lambda pb: not pb.meta.is_being_transferred.value and not pb.meta.unrevokable.value))
    assert mongo_storage.list_cert_revs(NS_ID, CERT_ID).total == 0


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_unmarking_new_domain(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, ns_id=NS_ID_2, is_being_transferred=True)

    next_state = p.UnmarkingNewDomain(domain_op).process(ctx).name
    assert next_state == 'FINISHING'
    wait_domain(cache, ns_id=NS_ID_2, check=lambda pb: not pb.meta.is_being_transferred.value)


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_finishing(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)

    next_state = p.Finishing(domain_op).process(ctx).name
    assert next_state == 'FINISHED'
    wait_domain_op(cache, check=lambda pb: not pb.spec.incomplete)


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_removing_new_domain(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, protocol, ns_id=NS_ID_2)

    next_state = p.RemovingNewDomain(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_NEW_DOMAIN_REMOVAL'
    wait_domain(cache, ns_id=NS_ID_2, check=lambda pb: pb.spec.deleted)

    zk_storage.remove_domain(NS_ID_2, DOMAIN_ID)
    assert wait_until(lambda: cache.get_domain(NS_ID_2, DOMAIN_ID) is None)
    next_state = p.RemovingNewDomain(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_NEW_DOMAIN_REMOVAL'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_waiting_for_new_domain_removal(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, protocol, ns_id=NS_ID_2)

    next_state = p.WaitingForNewDomainRemoval(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_NEW_DOMAIN_REMOVAL'

    zk_storage.remove_domain(NS_ID_2, DOMAIN_ID)
    assert wait_until(lambda: cache.get_domain(NS_ID_2, DOMAIN_ID) is None)
    next_state = p.WaitingForNewDomainRemoval(domain_op).process(ctx).name
    if protocol == model_pb2.DomainSpec.Config.HTTP_ONLY:
        assert next_state == 'UNMARKING_OLD_DOMAIN'
    else:
        assert next_state == 'REMOVING_NEW_CERT'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_removing_new_cert(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage, ns_id=NS_ID_2)

    next_state = p.RemovingNewCerts(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_NEW_CERT_REMOVAL'
    wait_cert(cache, ns_id=NS_ID_2,
              check=lambda pb: pb.spec.state == model_pb2.CertificateSpec.REMOVED_FROM_AWACS_AND_STORAGE)

    zk_storage.remove_cert(NS_ID_2, CERT_ID)
    assert wait_until(lambda: cache.get_cert(NS_ID_2, CERT_ID) is None)
    next_state = p.RemovingNewCerts(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_NEW_CERT_REMOVAL'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_waiting_for_new_cert_removal(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage, ns_id=NS_ID_2)

    next_state = p.WaitingForNewCertsRemoval(domain_op).process(ctx).name
    assert next_state == 'WAITING_FOR_NEW_CERT_REMOVAL'

    zk_storage.remove_cert(NS_ID_2, CERT_ID)
    assert wait_until(lambda: cache.get_cert(NS_ID_2, CERT_ID) is None)
    next_state = p.WaitingForNewCertsRemoval(domain_op).process(ctx).name
    assert next_state == 'UNMARKING_OLD_CERT'


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_unmarking_old_cert(mongo_storage, cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_cert(cache, zk_storage, unrevokable=True, is_being_transferred=True)
    assert mongo_storage.list_cert_revs(NS_ID, CERT_ID).total == 0

    next_state = p.UnmarkingOldCerts(domain_op).process(ctx).name
    assert next_state == 'UNMARKING_OLD_DOMAIN'
    wait_cert(cache, check=(
        lambda pb: not pb.meta.is_being_transferred.value and not pb.meta.unrevokable.value))
    assert mongo_storage.list_cert_revs(NS_ID, CERT_ID).total == 0


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_unmarking_old_domain(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)
    create_domain(cache, zk_storage, is_being_transferred=True)

    next_state = p.UnmarkingOldDomain(domain_op).process(ctx).name
    assert next_state == 'CANCELLING'
    wait_domain(cache, check=lambda pb: not pb.meta.is_being_transferred.value)


@pytest.mark.parametrize('protocol', [
    model_pb2.DomainSpec.Config.HTTP_ONLY,
    model_pb2.DomainSpec.Config.HTTPS_ONLY,
    model_pb2.DomainSpec.Config.HTTP_AND_HTTPS,
])
def test_cancelling(cache, zk_storage, ctx, protocol):
    domain_op = create_domain_op(cache, zk_storage, protocol)

    next_state = p.Cancelling(domain_op).process(ctx).name
    assert next_state == 'CANCELLED'
    wait_domain_op(cache, check=lambda pb: not pb.spec.incomplete)
