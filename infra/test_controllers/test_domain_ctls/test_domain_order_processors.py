# coding: utf-8
import logging

import inject
import pytest

from awacs.model.domain.order import processors as p
from awacs.model.domain.order.processors import DomainOrder
from awacs.model.util import clone_pb
from infra.awacs.proto import model_pb2
from awtest import wait_until, wait_until_passes, check_log
from awtest.api import create_ns


DOMAIN_ID = 'domain-id'
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


def create_domain_pb_with_cert_order(cache, zk_storage,
                                     common_name=u'test.common.name',
                                     domain_id=DOMAIN_ID):
    meta = model_pb2.DomainMeta(id=domain_id, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.author = 'author'
    domain_pb = model_pb2.Domain(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.cert_order.content.common_name = common_name
    domain_pb.order.content.cert_order.content.subject_alternative_names.extend(['test1.common.name',
                                                                                 'test2.common.name'])
    domain_pb.order.content.cert_order.content.abc_service_id = 999
    domain_pb.order.content.cert_order.content.ca_name = 'Internal'
    domain_pb.order.content.fqdns.extend(['a1.ya.ru', 'a2.ya.ru'])
    domain_pb.order.content.shadow_fqdns.extend(['s1.ya.ru', 's2.ya.ru'])
    zk_storage.create_domain(namespace_id=NS_ID,
                             domain_id=domain_id,
                             domain_pb=domain_pb)
    wait_domain(cache, lambda pb: pb, domain_id=domain_id)
    return DomainOrder(domain_pb)


def create_domain_pb_with_cert_ref(cache, zk_storage):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    domain_pb = model_pb2.Domain(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.cert_ref.id = CERT_ID
    domain_pb.order.content.fqdns.extend(['a1.ya.ru', 'a2.ya.ru'])
    domain_pb.order.content.shadow_fqdns.extend(['s1.ya.ru', 's2.ya.ru'])
    zk_storage.create_domain(namespace_id=NS_ID,
                             domain_id=DOMAIN_ID,
                             domain_pb=domain_pb)
    wait_domain(cache, lambda pb: pb)
    return DomainOrder(domain_pb)


def create_cert(cert_order_pb, cache, zk_storage, cert_id):
    cert_meta = model_pb2.CertificateMeta(id=cert_id, namespace_id=NS_ID)
    cert_pb = model_pb2.Certificate(meta=cert_meta)
    cert_pb.order.content.CopyFrom(cert_order_pb.content)
    zk_storage.create_cert(NS_ID, cert_id, cert_pb)
    assert wait_until(lambda: cache.get_cert(NS_ID, cert_id))
    return cert_pb


def update_domain(cache, zk_storage, domain_pb, check):
    for pb in zk_storage.update_domain(NS_ID, DOMAIN_ID):
        pb.CopyFrom(domain_pb)
    wait_domain(cache, check)


def wait_domain(cache, check, domain_id=DOMAIN_ID):
    assert wait_until(lambda: check(cache.get_domain(NS_ID, domain_id)))


@pytest.mark.parametrize('create_domain', [
    create_domain_pb_with_cert_order,
    create_domain_pb_with_cert_ref,
])
def test_start(cache, zk_storage, ctx, create_domain):
    domain = create_domain(cache, zk_storage)
    assert p.Started(domain).process(ctx).name == 'CHECKING_CERT_INFO'


def test_checking_cert_info_for_ref(ctx, cache, zk_storage):
    domain = create_domain_pb_with_cert_ref(cache, zk_storage)
    create_cert(domain.pb.order.content.cert_order, cache, zk_storage, CERT_ID)
    assert p.CheckingCertsInfo(domain).process(ctx).name == 'SAVING_DOMAIN_SPEC'


def test_checking_cert_info_for_http(ctx, cache, zk_storage):
    domain_pb = model_pb2.Domain(meta=model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID))
    domain_pb.order.progress.state.id = 'CHECKING_CERT_INFO'
    domain_pb.order.content.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    create_cert(domain_pb.order.content.cert_order, cache, zk_storage, CERT_ID)
    assert p.CheckingCertsInfo(DomainOrder(domain_pb)).process(ctx).name == 'SAVING_DOMAIN_SPEC'


def test_checking_cert_info_for_order(ctx, cache, zk_storage):
    domain = create_domain_pb_with_cert_order(cache, zk_storage)
    assert p.CheckingCertsInfo(domain).process(ctx).name == 'CREATING_CERT_ORDER'


@pytest.mark.parametrize('use_secondary', (False, True))
def test_creating_cert_orders(ctx, cache, zk_storage, use_secondary):
    create_ns(NS_ID, cache, zk_storage, logins=['ns1', 'ns1', 'ns2'])  # check for duplicates handling
    domain = create_domain_pb_with_cert_order(cache, zk_storage)
    secondary_cert_id = 'test.common.name_ec'
    if use_secondary:
        cert_id = 'test.common.name_rsa'
        domain.order.secondary_cert_order.CopyFrom(domain.order.cert_order)
        domain.order.cert_order.content.public_key_algorithm_id = 'rsa'
        domain.order.secondary_cert_order.content.public_key_algorithm_id = 'ec'
    else:
        cert_id = 'test.common.name'

    assert p.CreatingCertOrders(domain).process(ctx).name == 'WAITING_FOR_CERT_ORDER'
    assert domain.context['cert_order_id'] == cert_id
    if use_secondary:
        assert domain.context['secondary_cert_order_id'] == secondary_cert_id
    else:
        assert 'secondary_cert_order_id' not in domain.context

    cert_pb = wait_until(lambda: cache.must_get_cert(NS_ID, cert_id))
    assert cert_pb.meta.auth.staff.owners.logins == [u'author', u'ns1', u'ns2', u'nanny-robot']
    assert cert_pb.order.content == domain.order.cert_order.content
    if use_secondary:
        secondary_cert_pb = wait_until(lambda: cache.must_get_cert(NS_ID, secondary_cert_id))
        assert secondary_cert_pb.order.content == domain.order.secondary_cert_order.content
    else:
        assert cache.get_cert(NS_ID, secondary_cert_id) is None


def test_non_ascii_cert_common_name(ctx, cache, zk_storage):
    create_ns(NS_ID, cache, zk_storage)
    domain = create_domain_pb_with_cert_order(cache, zk_storage,
                                              common_name=u'привет.рф',
                                              domain_id='privet.rf')
    assert p.CreatingCertOrders(domain).process(ctx).name == 'WAITING_FOR_CERT_ORDER'
    # expect that domain id is used as a certificate id
    assert domain.context['cert_order_id'] == 'privet.rf'


def test_creating_cert_order_with_existing(ctx, cache, zk_storage):
    cert_id = 'test.common.name'
    domain = create_domain_pb_with_cert_order(cache, zk_storage)
    domain.order.secondary_cert_order.CopyFrom(domain.order.cert_order)
    domain.order.cert_order.content.public_key_algorithm_id = 'rsa'
    domain.order.secondary_cert_order.content.public_key_algorithm_id = 'ec'
    create_ns(NS_ID, cache, zk_storage)
    create_cert(domain.pb.order.content.cert_order, cache, zk_storage, cert_id + '_rsa')
    create_cert(domain.pb.order.content.secondary_cert_order, cache, zk_storage, cert_id + '_ec')
    assert p.CreatingCertOrders(domain).process(ctx).name == 'WAITING_FOR_CERT_ORDER'
    assert domain.context['cert_order_id'] == cert_id + '_rsa_1'
    assert domain.context['secondary_cert_order_id'] == cert_id + '_ec_1'
    cert_pb = wait_until(lambda: cache.must_get_cert(NS_ID, cert_id + '_rsa_1'))
    secondary_cert_pb = wait_until(lambda: cache.must_get_cert(NS_ID, cert_id + '_ec_1'))
    assert cert_pb.order.content == domain.order.cert_order.content
    assert secondary_cert_pb.order.content == domain.order.secondary_cert_order.content

    new_domain_pb = clone_pb(domain.pb)
    new_domain_pb.meta.id = DOMAIN_ID + '1'
    new_domain_pb.order.progress.context.clear()
    zk_storage.create_domain(namespace_id=NS_ID, domain_id=DOMAIN_ID + '1', domain_pb=new_domain_pb)
    wait_domain(cache, lambda pb: pb)
    new_domain = DomainOrder(new_domain_pb)
    new_domain.order.secondary_cert_order.CopyFrom(new_domain.order.cert_order)
    new_domain.order.cert_order.content.public_key_algorithm_id = 'rsa'
    new_domain.order.secondary_cert_order.content.public_key_algorithm_id = 'ec'
    assert p.CreatingCertOrders(new_domain).process(ctx).name == 'WAITING_FOR_CERT_ORDER'
    assert new_domain.context['cert_order_id'] == cert_id + '_rsa_2'
    assert new_domain.context['secondary_cert_order_id'] == cert_id + '_ec_2'
    cert_pb = wait_until(lambda: cache.must_get_cert(NS_ID, cert_id + '_rsa_2'))
    secondary_cert_pb = wait_until(lambda: cache.must_get_cert(NS_ID, cert_id + '_ec_2'))
    assert cert_pb.order.content == new_domain.order.cert_order.content
    assert secondary_cert_pb.order.content == new_domain.order.secondary_cert_order.content


def test_waiting_for_cert_noop(ctx, cache, zk_storage):
    domain = create_domain_pb_with_cert_ref(cache, zk_storage)
    assert p.WaitingForCertOrders(domain).process(ctx).name == 'SAVING_DOMAIN_SPEC'


def test_waiting_for_cert(ctx, cache, zk_storage):
    domain = create_domain_pb_with_cert_order(cache, zk_storage)
    domain.context['cert_order_id'] = DOMAIN_ID
    assert p.WaitingForCertOrders(domain).process(ctx).name == 'WAITING_FOR_CERT_ORDER'

    create_cert(domain.pb.order.content.cert_order, cache, zk_storage, DOMAIN_ID)
    for cert_pb in zk_storage.update_cert(NS_ID, DOMAIN_ID):
        cert_pb.order.status.status = 'FINISHED'
        cert_pb.order.progress.state.id = 'FINISH'
    assert wait_until(lambda: p.WaitingForCertOrders(domain).process(ctx).name == 'SAVING_DOMAIN_SPEC')


def test_saving_domain_spec_with_cert_order(ctx, cache, zk_storage):
    domain = create_domain_pb_with_cert_order(cache, zk_storage)
    domain.context['cert_order_id'] = DOMAIN_ID
    domain.context['secondary_cert_order_id'] = DOMAIN_ID
    assert p.SavingDomainSpec(domain).process(ctx).name == 'FINISHED'

    def check_domain_spec():
        domain_pb = cache.get_domain(NS_ID, DOMAIN_ID)
        assert not domain_pb.spec.incomplete
        config = domain_pb.spec.yandex_balancer.config
        order = domain_pb.order.content
        assert config.fqdns == order.fqdns
        assert config.shadow_fqdns == order.shadow_fqdns
        assert config.include_upstreams == order.include_upstreams
        assert config.protocol == order.protocol
        assert config.cert.id == DOMAIN_ID
        assert config.secondary_cert.id == DOMAIN_ID

    wait_until_passes(check_domain_spec)


def test_saving_domain_spec_with_cert_ref(ctx, cache, zk_storage):
    domain = create_domain_pb_with_cert_ref(cache, zk_storage)
    assert p.SavingDomainSpec(domain).process(ctx).name == 'FINISHED'

    def check_domain_spec():
        domain_pb = cache.get_domain(NS_ID, DOMAIN_ID)
        assert not domain_pb.spec.incomplete
        config = domain_pb.spec.yandex_balancer.config
        order = domain_pb.order.content
        assert config.fqdns == order.fqdns
        assert config.shadow_fqdns == order.shadow_fqdns
        assert config.include_upstreams == order.include_upstreams
        assert config.protocol == order.protocol
        assert config.cert.id == CERT_ID

    wait_until_passes(check_domain_spec)


def test_cancelling(ctx, cache, zk_storage):
    domain = create_domain_pb_with_cert_order(cache, zk_storage)
    assert p.Cancelling(domain).process(ctx).name == 'WAITING_FOR_CERT_CANCEL'


def test_waiting_for_cert_cancel(ctx, cache, zk_storage, caplog):
    domain = create_domain_pb_with_cert_order(cache, zk_storage)
    domain.context['cert_order_id'] = DOMAIN_ID
    domain.context['secondary_cert_order_id'] = DOMAIN_ID + '_1'
    create_cert(domain.pb.order.content.cert_order, cache, zk_storage, DOMAIN_ID)
    create_cert(domain.pb.order.content.secondary_cert_order, cache, zk_storage, DOMAIN_ID + '_1')
    wait_until_passes(lambda: cache.must_get_cert(NS_ID, DOMAIN_ID))
    wait_until_passes(lambda: cache.must_get_cert(NS_ID, DOMAIN_ID + '_1'))

    with check_log(caplog) as log:
        assert p.WaitingForCertsCancel(domain).process(ctx).name == 'WAITING_FOR_CERT_CANCEL'
        assert 'primary cert is still in progress' in log.records_text()

    for cert_pb in zk_storage.update_cert(NS_ID, DOMAIN_ID):
        cert_pb.order.status.status = 'FINISHED'
        cert_pb.order.progress.state.id = 'FINISH'
    assert wait_until(lambda: cache.get_cert(NS_ID, DOMAIN_ID).order.status.status == 'FINISHED')
    with check_log(caplog) as log:
        assert p.WaitingForCertsCancel(domain).process(ctx).name == 'WAITING_FOR_CERT_CANCEL'
        assert 'secondary cert is still in progress' in log.records_text()

    for cert_pb in zk_storage.update_cert(NS_ID, DOMAIN_ID + '_1'):
        cert_pb.order.status.status = 'FINISHED'
        cert_pb.order.progress.state.id = 'FINISH'
    assert wait_until(lambda: cache.get_cert(NS_ID, DOMAIN_ID + '_1').order.status.status == 'FINISHED')
    assert p.WaitingForCertsCancel(domain).process(ctx).name == 'CANCELLED'
    assert not domain.pb.spec.incomplete
