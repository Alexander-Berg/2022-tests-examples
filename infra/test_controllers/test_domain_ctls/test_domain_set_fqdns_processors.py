import logging

import inject
import pytest

from awacs.model.cache import fqdn_info_tuple
from awacs.model.domain.operations import set_fqdns as p
from awacs.model.domain.operations.set_fqdns import DomainSetFqdns
from infra.awacs.proto import model_pb2
from awtest import wait_until, wait_until_passes
from awtest.api import create_ns


DOMAIN_ID = 'domain_op-id'
CERT_ID = 'cert-id'
NS_ID = 'namespace-id'


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_set_fqdns_pb_with_cert_order(cache, zk_storage):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.author = 'author'
    domain_op_pb = model_pb2.DomainOperation(meta=meta)
    domain_op_pb.spec.incomplete = True
    domain_op_pb.order.content.set_fqdns.fqdns.extend(['test.common.name', 'test1.common.name', 'test2.common.name'])
    domain_op_pb.order.content.set_fqdns.shadow_fqdns.extend(['s1.ya.ru', 's2.ya.ru'])
    domain_op_pb.order.content.set_fqdns.cert_order.content.common_name = 'test.common.name'
    domain_op_pb.order.content.set_fqdns.cert_order.content.subject_alternative_names.extend(['test1.common.name',
                                                                                              'test2.common.name'])
    domain_op_pb.order.content.set_fqdns.cert_order.content.abc_service_id = 999
    domain_op_pb.order.content.set_fqdns.cert_order.content.ca_name = 'Internal'
    zk_storage.create_domain_operation(namespace_id=NS_ID,
                                       domain_id=DOMAIN_ID,
                                       domain_operation_pb=domain_op_pb)
    wait_domain_op(cache, lambda pb: pb)
    return DomainSetFqdns(domain_op_pb)


def create_set_fqdns_pb_with_cert_ref(cache, zk_storage,
                                      fqdns=('test.common.name', 'test1.common.name', 'test2.common.name')):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.author = 'author'
    domain_op_pb = model_pb2.DomainOperation(meta=meta)
    domain_op_pb.spec.incomplete = True
    domain_op_pb.order.content.set_fqdns.fqdns.extend(fqdns)
    domain_op_pb.order.content.set_fqdns.shadow_fqdns.extend(['s1.ya.ru', 's2.ya.ru'])
    domain_op_pb.order.content.set_fqdns.cert_ref.id = CERT_ID
    zk_storage.create_domain_operation(namespace_id=NS_ID,
                                       domain_id=DOMAIN_ID,
                                       domain_operation_pb=domain_op_pb)
    wait_domain_op(cache, lambda pb: pb)
    return DomainSetFqdns(domain_op_pb)


def create_domain(cache, zk_storage, protocol, fqdns=('old.1', 'old.2'), secondary_cert_id=None):
    domain_pb = model_pb2.Domain(meta=model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID))
    domain_pb.spec.yandex_balancer.config.protocol = protocol
    domain_pb.spec.yandex_balancer.config.fqdns.extend(fqdns)
    if secondary_cert_id:
        domain_pb.spec.yandex_balancer.config.secondary_cert.id = secondary_cert_id
    zk_storage.create_domain(NS_ID, DOMAIN_ID, domain_pb)
    wait_until_passes(lambda: cache.must_get_domain(NS_ID, DOMAIN_ID))
    return domain_pb


def create_cert(domain_op_pb, cache, zk_storage, cert_id):
    cert_meta = model_pb2.CertificateMeta(id=cert_id, namespace_id=NS_ID)
    cert_pb = model_pb2.Certificate(meta=cert_meta)
    cert_pb.order.content.CopyFrom(domain_op_pb.order.content.set_fqdns.cert_order.content)
    zk_storage.create_cert(NS_ID, cert_id, cert_pb)
    assert wait_until(lambda: cache.must_get_cert(NS_ID, cert_id))
    return cert_pb


def update_domain_op(cache, zk_storage, domain_op_pb, check):
    for pb in zk_storage.update_domain_operation(NS_ID, DOMAIN_ID):
        pb.CopyFrom(domain_op_pb)
    wait_domain_op(cache, check)


def wait_domain_op(cache, check):
    assert wait_until(lambda: check(cache.must_get_domain_operation(NS_ID, DOMAIN_ID)))


@pytest.mark.parametrize('create_domain_op', [
    create_set_fqdns_pb_with_cert_order,
    create_set_fqdns_pb_with_cert_ref,
])
def test_start(cache, zk_storage, ctx, create_domain_op):
    domain_op = create_domain_op(cache, zk_storage)
    create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTP_ONLY)
    assert p.Started(domain_op).process(ctx).name == 'CHECKING_CERT_INFO'


def test_checking_cert_info_for_ref(ctx, cache, zk_storage):
    domain_op = create_set_fqdns_pb_with_cert_ref(cache, zk_storage)
    create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)
    create_cert(domain_op.pb, cache, zk_storage, CERT_ID)
    assert p.CheckingCertInfo(domain_op).process(ctx).name == 'SAVING_DOMAIN_SPEC'


def test_checking_cert_info_for_http(ctx, cache, zk_storage):
    create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTP_ONLY)
    domain_op_pb = model_pb2.DomainOperation(meta=model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID))
    create_cert(domain_op_pb, cache, zk_storage, CERT_ID)
    assert p.CheckingCertInfo(DomainSetFqdns(domain_op_pb)).process(ctx).name == 'SAVING_DOMAIN_SPEC'


def test_checking_cert_info_for_order(ctx, cache, zk_storage):
    create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)
    domain_op = create_set_fqdns_pb_with_cert_order(cache, zk_storage)
    assert p.CheckingCertInfo(domain_op).process(ctx).name == 'CREATING_CERT_ORDER'


def test_creating_cert_order(ctx, cache, zk_storage):
    domain_op = create_set_fqdns_pb_with_cert_order(cache, zk_storage)
    create_ns(NS_ID, cache, zk_storage)
    assert p.CreatingCertOrders(domain_op).process(ctx).name == 'WAITING_FOR_CERT_ORDER'
    assert domain_op.context['cert_order_id'] == 'test.common.name'
    cert_pb = wait_until(lambda: cache.must_get_cert(NS_ID, 'test.common.name'))
    assert cert_pb.order.content == domain_op.pb.order.content.set_fqdns.cert_order.content
    assert cert_pb.meta.auth.staff.owners.logins == [u'author', u'ns1', u'ns2', u'nanny-robot']


def test_waiting_for_cert_noop(ctx, cache, zk_storage):
    domain_op = create_set_fqdns_pb_with_cert_ref(cache, zk_storage)
    assert p.WaitingForCertOrder(domain_op).process(ctx).name == 'SAVING_DOMAIN_SPEC'


def test_waiting_for_cert(ctx, cache, zk_storage):
    domain_op = create_set_fqdns_pb_with_cert_order(cache, zk_storage)
    domain_op.context['cert_order_id'] = DOMAIN_ID
    assert p.WaitingForCertOrder(domain_op).process(ctx).name == 'WAITING_FOR_CERT_ORDER'

    create_cert(domain_op.pb, cache, zk_storage, DOMAIN_ID)
    for cert_pb in zk_storage.update_cert(NS_ID, DOMAIN_ID):
        cert_pb.order.status.status = 'FINISHED'
        cert_pb.order.progress.state.id = 'FINISH'
    assert wait_until(lambda: p.WaitingForCertOrder(domain_op).process(ctx).name == 'SAVING_DOMAIN_SPEC')


def test_saving_domain_spec_with_cert_order(ctx, cache, zk_storage):
    create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTPS_ONLY)
    domain_op = create_set_fqdns_pb_with_cert_order(cache, zk_storage)
    domain_op.context['cert_order_id'] = DOMAIN_ID

    assert p.SavingDomainSpec(domain_op).process(ctx).name == 'FINISHED'

    def check_domain_spec():
        domain_op_pb = cache.get_domain_operation(NS_ID, DOMAIN_ID)
        assert not domain_op_pb.spec.incomplete
        assert set(domain_op_pb.spec.old_fqdns) == {'old.1', 'old.2'}

        domain_pb_ = cache.get_domain(NS_ID, DOMAIN_ID)
        config = domain_pb_.spec.yandex_balancer.config
        order = domain_op_pb.order.content.set_fqdns
        assert config.fqdns == order.fqdns
        assert config.shadow_fqdns == order.shadow_fqdns
        assert config.cert.id == DOMAIN_ID

    wait_until_passes(check_domain_spec)


def test_saving_domain_spec_with_cert_ref(ctx, cache, zk_storage):
    create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTPS_ONLY, secondary_cert_id='test')
    domain_op = create_set_fqdns_pb_with_cert_ref(cache, zk_storage)
    domain_op.pb.order.content.set_fqdns.remove_secondary_cert = True

    assert p.SavingDomainSpec(domain_op).process(ctx).name == 'FINISHED'

    def check_domain_spec():
        domain_op_pb = cache.get_domain_operation(NS_ID, DOMAIN_ID)
        assert not domain_op_pb.spec.incomplete
        assert set(domain_op_pb.spec.old_fqdns) == {'old.1', 'old.2'}

        domain_pb_ = cache.get_domain(NS_ID, DOMAIN_ID)
        config_pb = domain_pb_.spec.yandex_balancer.config
        order_pb = domain_op_pb.order.content.set_fqdns
        assert config_pb.fqdns == order_pb.fqdns
        assert config_pb.shadow_fqdns == order_pb.shadow_fqdns
        assert config_pb.cert.id == CERT_ID
        assert not config_pb.HasField('secondary_cert')

    wait_until_passes(check_domain_spec)


def test_cache_during_set_fqdns(cache, zk_storage):
    create_domain(cache, zk_storage, protocol=model_pb2.DomainSpec.Config.HTTP_ONLY,
                  fqdns=('fqdn1.ya.ru', 'fqdn2.ya.ru', 'fqdn.yandex'))
    assert len(cache.list_domain_fqdns()['ya.ru']['fqdn1']) == 1
    assert len(cache.list_domain_fqdns()['ya.ru']['fqdn2']) == 1
    assert len(cache.list_domain_fqdns()['yandex']['fqdn']) == 1
    create_set_fqdns_pb_with_cert_ref(cache, zk_storage, fqdns=('fqdn2.ya.ru', 'fqdn3.ya.ru'))
    assert cache.list_domain_fqdns()['ya.ru']['fqdn1'] == {fqdn_info_tuple('domain', NS_ID, DOMAIN_ID)}
    assert cache.list_domain_fqdns()['ya.ru']['fqdn2'] == {
        fqdn_info_tuple('domain', NS_ID, DOMAIN_ID),
        fqdn_info_tuple('domain op', NS_ID, DOMAIN_ID),
    }
    assert cache.list_domain_fqdns()['ya.ru']['fqdn3'] == {fqdn_info_tuple('domain op', NS_ID, DOMAIN_ID)}

    for op_pb in zk_storage.update_domain_operation(NS_ID, DOMAIN_ID):
        op_pb.spec.old_fqdns.extend(['fqdn1.ya.ru', 'fqdn.yandex'])
    wait_until(lambda: cache.get_domain_operation(NS_ID, DOMAIN_ID).spec.old_fqdns)

    zk_storage.remove_domain_operation(NS_ID, DOMAIN_ID)
    wait_until(lambda: cache.get_domain_operation(NS_ID, DOMAIN_ID) is None)
    assert 'fqdn1' not in cache.list_domain_fqdns()['ya.ru']
    assert 'yandex' not in cache.list_domain_fqdns()
    assert cache.list_domain_fqdns()['ya.ru']['fqdn2'] == {fqdn_info_tuple('domain', NS_ID, DOMAIN_ID)}
    assert cache.list_domain_fqdns()['ya.ru']['fqdn3'] == {fqdn_info_tuple('domain', NS_ID, DOMAIN_ID)}
