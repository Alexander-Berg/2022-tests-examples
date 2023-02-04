# coding: utf-8
import logging

import inject
import mock
import pytest
import six
from sepelib.core import config as appconfig

from awacs.lib import certificator
from awacs.lib.order_processor.model import OverallStatus
from awacs.lib.rpc import exceptions
from awacs.model.cache import IAwacsCache
from awacs.model.util import clone_pb
from awacs.model.namespace.order import processors
from infra.awacs.proto import api_pb2, model_pb2, modules_pb2
from awacs.web import namespace_service, domain_service
from infra.swatlib.auth import abc
from awtest.api import call, create_namespace_with_order_in_progress, fill_object_upper_limits, make_unknown_layout
from awtest import check_log
from awtest.core import wait_until, wait_until_passes
from awtest.mocks.certificator import MockCertificatorClient


LOGIN = 'login'
GROUP = "1"
COMMENT = 'Creating turbo domain'
NAMESPACE_ID = 'hyperspace'
CERT_ID = 'test'


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        b.bind(certificator.ICertificatorClient, MockCertificatorClient)
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_domain_pb(zk_storage):
    domain_pb = model_pb2.Domain(meta=model_pb2.DomainMeta(id=NAMESPACE_ID, namespace_id=NAMESPACE_ID))
    zk_storage.create_domain(NAMESPACE_ID, NAMESPACE_ID, domain_pb)
    assert wait_until_passes(lambda: IAwacsCache.instance().must_get_domain(NAMESPACE_ID, NAMESPACE_ID))
    return domain_pb


@pytest.fixture
def domain_pb(zk_storage):
    return create_domain_pb(zk_storage)


def create_namespace(namespace_id):
    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_id
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    call(namespace_service.create_namespace, req_pb, 'bleed')
    assert wait_until_passes(lambda: IAwacsCache.instance().must_get_namespace(namespace_id))


@pytest.fixture
def namespace(ctx):
    create_namespace(NAMESPACE_ID)

    req_pb = api_pb2.GetNamespaceRequest(id=NAMESPACE_ID, consistency=api_pb2.STRONG)
    resp_pb = call(namespace_service.get_namespace, req_pb, LOGIN)

    processors.Finalizing(processors.NamespaceOrder(resp_pb.namespace)).process(ctx)
    make_unknown_layout(NAMESPACE_ID, LOGIN)


def create_cert(zk_storage, cert_id=CERT_ID):
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.namespace_id = NAMESPACE_ID
    cert_pb.meta.id = cert_id
    cert_pb.meta.version = 'xxx'
    cert_pb.order.content.common_name = 'm0.in.yandex-team.ru'
    zk_storage.create_cert(namespace_id=NAMESPACE_ID, cert_id=cert_id, cert_pb=cert_pb)


def create_cert_renewal(zk_storage, target_rev, cert_id=CERT_ID):
    renewal_pb = model_pb2.CertificateRenewal()
    renewal_pb.meta.namespace_id = NAMESPACE_ID
    renewal_pb.meta.id = cert_id
    renewal_pb.meta.version = 'xxx'
    renewal_pb.meta.target_rev = target_rev
    zk_storage.create_cert_renewal(namespace_id=NAMESPACE_ID, cert_renewal_id=cert_id, cert_renewal_pb=renewal_pb)


def create_order_domain_request_pb():
    """
    :rtype: api_pb2.CreateDomainRequest
    """
    req_pb = api_pb2.CreateDomainRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.comment = COMMENT
    return req_pb


@pytest.fixture
def domain_operation_request_pb():
    """
    :rtype: api_pb2.CreateDomainOperationRequest
    """
    req_pb = api_pb2.CreateDomainOperationRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.comment = COMMENT
    return req_pb


@pytest.fixture
def order_domain_request_pb():
    """
    :rtype: api_pb2.CreateDomainRequest
    """
    req_pb = create_order_domain_request_pb()
    req_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    req_pb.order.fqdns.append('m0.in.yandex-team.ru')
    req_pb.order.cert_order.content.common_name = 'm0.in.yandex-team.ru'
    req_pb.order.cert_order.content.ca_name = 'InternalCA'
    req_pb.order.cert_order.content.abc_service_id = 9999
    req_pb.order.include_upstreams.type = modules_pb2.ALL
    return req_pb


def test_forbidden_operations_during_namespace_order(zk_storage, cache, order_domain_request_pb,
                                                     domain_operation_request_pb, enable_auth):
    # forbid creation (including for operations), order cancellation, and removal
    create_namespace_with_order_in_progress(zk_storage, cache, NAMESPACE_ID)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    d_pb = model_pb2.Domain(meta=order_domain_request_pb.meta)
    d_pb.order.content.CopyFrom(order_domain_request_pb.order)
    d_pb.meta.version = 'xxx'
    zk_storage.create_domain(NAMESPACE_ID, NAMESPACE_ID, d_pb)
    wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID))

    req_pb = api_pb2.CancelDomainOrderRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID, version=d_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(domain_service.cancel_domain_order, req_pb, LOGIN)

    req_pb = api_pb2.RemoveDomainRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID, version=d_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(domain_service.remove_domain, req_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.fqdns.append('a.other.ru')
    domain_operation_request_pb.order.set_fqdns.cert_order.content.common_name = 'a.other.ru'
    domain_operation_request_pb.order.set_fqdns.cert_order.content.abc_service_id = 999
    domain_operation_request_pb.order.set_fqdns.cert_order.content.ca_name = 'test'
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


@pytest.mark.parametrize('max_count,custom_count', [
    (0, None),
    (1, None),
    (10, None),
    (5, 10),
    (10, 5),
])
def test_namespace_objects_total_limit(max_count, custom_count, namespace, order_domain_request_pb):
    appconfig.set_value('common_objects_limits.domain', max_count)
    if custom_count is not None:
        fill_object_upper_limits(NAMESPACE_ID, 'domain', custom_count, LOGIN)
    count = custom_count or max_count

    req_pb = order_domain_request_pb
    for _ in range(count):
        call(domain_service.create_domain, req_pb, LOGIN)
        req_pb.meta.id += 'a'
        cname = req_pb.order.cert_order.content.common_name
        req_pb.order.cert_order.content.common_name = 'a' + cname
        req_pb.order.ClearField('fqdns')
        req_pb.order.fqdns.append('a' + cname)

    def check():
        list_req_pb = api_pb2.ListDomainsRequest(namespace_id=NAMESPACE_ID)
        assert call(domain_service.list_domains, list_req_pb, LOGIN).total == count

    wait_until_passes(check)

    with pytest.raises(exceptions.BadRequestError,
                       match='Exceeded limit of domains in the namespace: {}'.format(count)):
        call(domain_service.create_domain, req_pb, LOGIN)


def test_tls_certificate_issuability_validation(zk_storage, mongo_storage, cache, namespace, order_domain_request_pb):
    cert_order_content_pb = order_domain_request_pb.order.cert_order.content
    cert_order_content_pb.ca_name = u'CertumProductionCA'
    cert_order_content_pb.common_name = u'google.com'
    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.append(u'google.com')

    with pytest.raises(exceptions.BadRequestError,
                       match=u'"order.content.cert_order.content.common_name" is not valid: '
                             u'TLS certificate for FQDN "google.com" can not be issued automatically'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)


def test_create_get_remove_domain(zk_storage, mongo_storage, cache, namespace, order_domain_request_pb):
    resp_pb = call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    domain_pb = resp_pb.domain
    assert domain_pb.meta.id == NAMESPACE_ID
    assert domain_pb.meta.author == LOGIN
    assert domain_pb.meta.comment == COMMENT
    assert domain_pb.order.content.fqdns == ['m0.in.yandex-team.ru']
    assert domain_pb.order.content.protocol == model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    assert domain_pb.order.content.cert_order.content.common_name == 'm0.in.yandex-team.ru'
    assert domain_pb.order.content.cert_order.content.ca_name == 'InternalCA'
    assert domain_pb.order.content.cert_order.content.abc_service_id == 9999

    # test empty required fields
    req_pb = create_order_domain_request_pb()
    req_pb.meta.id = NAMESPACE_ID + '_1'
    req_pb.order.cert_order.content.ca_name = 'CertumProductionCA'
    req_pb.order.cert_order.content.common_name = 'm0.in.yandex-team.ru'
    req_pb.order.cert_order.content.abc_service_id = 9999

    with pytest.raises(exceptions.BadRequestError, match='"order.content": "fqdns" or "shadow_fqdns" must be set'):
        call(domain_service.create_domain, req_pb, LOGIN)

    req_pb.order.fqdns.extend(['m1.in.yandex-team.ru', '2m.in.yandex-team.ru'])
    with pytest.raises(exceptions.BadRequestError, match='"protocol" cannot be NONE'):
        call(domain_service.create_domain, req_pb, LOGIN)

    req_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    with pytest.raises(exceptions.BadRequestError, match='"order.content.include_upstreams": must be set'):
        call(domain_service.create_domain, req_pb, LOGIN)

    req_pb.order.include_upstreams.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"order.content.include_upstreams.filter.any": must be true if'
                                                         ' "order.content.include_upstreams.type is NONE"'):
        call(domain_service.create_domain, req_pb, LOGIN)

    req_pb.order.include_upstreams.type = modules_pb2.BY_ID
    with pytest.raises(exceptions.BadRequestError, match='ids: is required'):
        call(domain_service.create_domain, req_pb, LOGIN)

    req_pb.order.include_upstreams.ids.append('123')
    with pytest.raises(exceptions.BadRequestError,
                       match=('"order.content": FQDNs "2m.in.yandex-team.ru", "m1.in.yandex-team.ru" '
                              'are not covered by certificate order')):
        call(domain_service.create_domain, req_pb, LOGIN)

    req_pb.order.cert_order.content.common_name = 'm1.in.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.content": FQDNs "2m.in.yandex-team.ru" '
                             'are not covered by certificate order'):
        call(domain_service.create_domain, req_pb, LOGIN)

    req_pb.order.cert_order.content.subject_alternative_names.append('2m.in.yandex-team.ru')
    call(domain_service.create_domain, req_pb, LOGIN)
    assert wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID + '_1'))

    # Test get
    req_pb = api_pb2.GetDomainRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
    resp_pb = call(domain_service.get_domain, req_pb, LOGIN)
    domain_pb = resp_pb.domain
    assert domain_pb.order.content.fqdns == ['m0.in.yandex-team.ru']

    req_pb = api_pb2.GetDomainRequest(namespace_id='missing', id='missing')
    with pytest.raises(exceptions.NotFoundError):
        call(domain_service.get_domain, req_pb, LOGIN)

    items, count = mongo_storage.list_domain_revs(namespace_id=NAMESPACE_ID, domain_id=NAMESPACE_ID)
    assert count == 1

    # Test remove during transfer
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.meta.is_being_transferred.value = True
    wait_until(lambda: cache.get_domain(NAMESPACE_ID, NAMESPACE_ID).meta.is_being_transferred.value)
    req_pb = api_pb2.RemoveDomainRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID,
        version=domain_pb.meta.version
    )
    with pytest.raises(exceptions.ForbiddenError, match="Cannot modify domain while it's being transferred"):
        call(domain_service.remove_domain, req_pb, LOGIN)

    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.meta.is_being_transferred.value = False
    wait_until(lambda: not cache.get_domain(NAMESPACE_ID, NAMESPACE_ID).meta.is_being_transferred.value)

    # Test remove with unfinished order
    req_pb = api_pb2.RemoveDomainRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID,
        version=domain_pb.meta.version
    )
    call(domain_service.remove_domain, req_pb, LOGIN)

    def check_remove():
        req_get_pb = api_pb2.GetDomainRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
        pb = call(domain_service.get_domain, req_get_pb, LOGIN)
        assert pb.domain.spec.deleted

    wait_until_passes(check_remove)


def test_secondary_cert_order(zk_storage, namespace, order_domain_request_pb):
    order_domain_request_pb.order.ClearField('cert_order')
    order_domain_request_pb.order.secondary_cert_order.content.ca_name = '...'
    order_domain_request_pb.order.secondary_cert_order.content.common_name = 'm1.in.yandex-team.ru'
    order_domain_request_pb.order.secondary_cert_order.content.subject_alternative_names.append('m0.in.yandex-team.ru')
    order_domain_request_pb.order.secondary_cert_order.content.abc_service_id = 9999
    order_domain_request_pb.order.fqdns.extend(['m1.in.yandex-team.ru'])
    order_domain_request_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    order_domain_request_pb.order.include_upstreams.type = modules_pb2.ALL
    with pytest.raises(exceptions.BadRequestError, match='"order.content": "cert_order" or "cert_ref" must be set'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.cert_order.content.common_name = 'mx.in.yandex-team.ru'
    order_domain_request_pb.order.cert_order.content.ca_name = '...'
    order_domain_request_pb.order.cert_order.content.abc_service_id = 1111
    with pytest.raises(exceptions.BadRequestError, match='"order.content": field "common_name" in primary and '
                                                         'secondary certs must match: '
                                                         '"mx.in.yandex-team.ru" != "m1.in.yandex-team.ru"'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.cert_order.content.common_name = 'm1.in.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError, match='"order.content": field "public_key_algorithm_id" in primary '
                                                         'and secondary certs must be different'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.cert_order.content.public_key_algorithm_id = 'rsa'
    order_domain_request_pb.order.secondary_cert_order.content.public_key_algorithm_id = 'ec'
    with pytest.raises(exceptions.BadRequestError, match='"order.content": field "public_key_algorithm_id" in primary '
                                                         'cert must have value "ec", not "rsa"'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.cert_order.content.public_key_algorithm_id = 'ec'
    order_domain_request_pb.order.secondary_cert_order.content.public_key_algorithm_id = 'rsa'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    assert ((six.text_type(e.value) == ('"order.content": field "subject_alternative_names" in primary and secondary certs '
                                        'must match: "[]" != "[u\'m0.in.yandex-team.ru\']"')) or
            (six.text_type(e.value) == ('"order.content": field "subject_alternative_names" in primary and secondary certs '
                                        'must match: "[]" != "[\'m0.in.yandex-team.ru\']"')))

    order_domain_request_pb.order.cert_order.content.subject_alternative_names.append('m0.in.yandex-team.ru')
    domain_pb = call(domain_service.create_domain, order_domain_request_pb, LOGIN).domain
    assert domain_pb.order.content.secondary_cert_order.content.ca_name == '...'
    assert domain_pb.order.content.secondary_cert_order.content.common_name == 'm1.in.yandex-team.ru'
    assert domain_pb.order.content.secondary_cert_order.content.abc_service_id == 9999
    assert domain_pb.order.content.secondary_cert_order.content.subject_alternative_names == ['m0.in.yandex-team.ru']


def test_secondary_cert_with_primary_ref(zk_storage, cache, namespace, order_domain_request_pb):
    order_domain_request_pb.order.ClearField('cert_order')
    order_domain_request_pb.order.secondary_cert_order.content.ca_name = '...'
    order_domain_request_pb.order.secondary_cert_order.content.common_name = 'mx.in.yandex-team.ru'
    order_domain_request_pb.order.secondary_cert_order.content.subject_alternative_names.append('m0.in.yandex-team.ru')
    order_domain_request_pb.order.secondary_cert_order.content.abc_service_id = 9999
    order_domain_request_pb.order.secondary_cert_order.content.public_key_algorithm_id = 'ec'
    order_domain_request_pb.order.fqdns.extend(['m1.in.yandex-team.ru'])
    order_domain_request_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    order_domain_request_pb.order.include_upstreams.type = modules_pb2.ALL

    cert_id = 'existing_cert'
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = NAMESPACE_ID
    cert_pb.spec.certificator.ca_name = 'CA'
    cert_pb.spec.fields.subject_common_name = 'm1.in.yandex-team.ru'
    cert_pb.spec.fields.subject_alternative_names.extend(['m0.in.yandex-team.ru',
                                                          'm1.in.yandex-team.ru',
                                                          'm2.in.yandex-team.ru'])
    cert_pb.spec.fields.public_key_info.algorithm_id = 'ec'
    zk_storage.create_cert(namespace_id=NAMESPACE_ID, cert_id=cert_id, cert_pb=cert_pb)
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, cert_id))

    order_domain_request_pb.order.cert_ref.id = cert_id
    with pytest.raises(exceptions.BadRequestError, match='"order.content": field "common_name" in primary and '
                                                         'secondary certs must match: '
                                                         '"m1.in.yandex-team.ru" != "mx.in.yandex-team.ru"'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.secondary_cert_order.content.common_name = 'm1.in.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError, match='"order.content": field "public_key_algorithm_id" in primary '
                                                         'and secondary certs must be different'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.secondary_cert_order.content.public_key_algorithm_id = 'rsa'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    assert (six.text_type(e.value) == ('"order.content": field "subject_alternative_names" in primary and secondary certs '
                                       'must match: "[u\'m0.in.yandex-team.ru\', u\'m2.in.yandex-team.ru\']" != '
                                       '"[u\'m0.in.yandex-team.ru\']"') or
            six.text_type(e.value) == ('"order.content": field "subject_alternative_names" in primary and secondary certs '
                                       'must match: "[\'m0.in.yandex-team.ru\', \'m2.in.yandex-team.ru\']" != '
                                       '"[\'m0.in.yandex-team.ru\']"'
                                       ))

    order_domain_request_pb.order.secondary_cert_order.content.subject_alternative_names.append('m2.in.yandex-team.ru')
    domain_pb = call(domain_service.create_domain, order_domain_request_pb, LOGIN).domain
    assert domain_pb.order.content.secondary_cert_order.content.common_name == 'm1.in.yandex-team.ru'
    assert domain_pb.order.content.secondary_cert_order.content.abc_service_id == 9999
    assert domain_pb.order.content.secondary_cert_order.content.subject_alternative_names == ['m0.in.yandex-team.ru',
                                                                                              'm2.in.yandex-team.ru']


def test_shadow_fqdns(zk_storage, cache, namespace, order_domain_request_pb, domain_operation_request_pb):
    order_domain_request_pb.order.cert_order.content.subject_alternative_names.extend(['*.my.test', '*.other.ru'])
    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.extend(['*.my.test', 'duplicate.my.test'])
    order_domain_request_pb.order.shadow_fqdns.extend(['*.my.test', 'duplicate.my.test'])

    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content.shadow_fqdns": FQDNs "\*.my.test", "duplicate.my.test" '
              r'cannot be used in "fqdns" field at the same time'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.extend(['real.my.test', 'real2.my.test'])
    order_domain_request_pb.order.shadow_fqdns.extend(['duplicate.my.test'])
    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content.shadow_fqdns": FQDNs "duplicate.my.test" are used multiple times'):  # noqa
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    del order_domain_request_pb.order.shadow_fqdns[:]
    order_domain_request_pb.order.shadow_fqdns.extend(['shadow.my.test'])
    call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    assert wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID))

    req_pb = clone_pb(order_domain_request_pb)
    req_pb.meta.id = NAMESPACE_ID + '_1'
    del req_pb.order.fqdns[:]
    del req_pb.order.shadow_fqdns[:]
    req_pb.order.fqdns.extend(['shadow.my.test'])  # duplicates shadow_fqdn of the first domain
    req_pb.order.shadow_fqdns.extend(['real.my.test'])  # duplicates fqdn of the first domain
    call(domain_service.create_domain, req_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.shadow_fqdns.append('real2.my.test')
    domain_operation_request_pb.order.set_fqdns.cert_order.content.common_name = 'real2.my.test'
    domain_operation_request_pb.order.set_fqdns.cert_order.content.abc_service_id = 999
    domain_operation_request_pb.order.set_fqdns.cert_order.content.ca_name = 'test'

    with pytest.raises(exceptions.BadRequestError, match='Cannot create operation for incomplete domain'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.spec.incomplete = False

    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)
    assert wait_until_passes(lambda: cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID))


def test_non_unique_fqdns(zk_storage, cache, namespace, order_domain_request_pb, domain_operation_request_pb):
    order_domain_request_pb.order.cert_order.content.subject_alternative_names.extend(['*.my.test', '*.other.ru'])
    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.extend(['*.my.test', 'duplicate.my.test'])

    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content.fqdns": FQDN "duplicate.my.test" is '
              r'already represented by FQDN "\*.my.test" in this domain'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.extend(['duplicate.my.test', '*.my.test'])
    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content.fqdns": FQDN "duplicate.my.test" is '
              r'already represented by FQDN "\*.my.test" in this domain'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.extend(['good.my.test'])
    call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    assert wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID))
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.spec.incomplete = False

    req_pb = clone_pb(order_domain_request_pb)
    req_pb.meta.id = NAMESPACE_ID + '_1'
    del req_pb.order.fqdns[:]
    req_pb.order.fqdns.extend(['good.my.test'])
    with pytest.raises(
        exceptions.BadRequestError,
        match='"order.content.fqdns": FQDN "good.my.test" already exists in domain "hyperspace:hyperspace"'):  # noqa
        call(domain_service.create_domain, req_pb, LOGIN)

    del req_pb.order.fqdns[:]
    req_pb.order.fqdns.extend(['*.my.test'])
    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content.fqdns": FQDN "\*.my.test" would overlap FQDN "good.my.test" '
              r'that already exists in domain "hyperspace:hyperspace"'):
        call(domain_service.create_domain, req_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.fqdns.append('a.other.ru')
    domain_operation_request_pb.order.set_fqdns.cert_order.content.common_name = 'a.other.ru'
    domain_operation_request_pb.order.set_fqdns.cert_order.content.abc_service_id = 999
    domain_operation_request_pb.order.set_fqdns.cert_order.content.ca_name = 'test'
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)
    assert wait_until_passes(lambda: cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID))

    del req_pb.order.fqdns[:]
    req_pb.order.fqdns.extend(['*.other.ru'])
    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content.fqdns": FQDN "\*.other.ru" would overlap FQDN "a.other.ru" '
              r'that already exists in domain op "hyperspace:hyperspace"'):
        call(domain_service.create_domain, req_pb, LOGIN)

    del req_pb.order.fqdns[:]
    req_pb.order.fqdns.extend(['b.other.ru'])
    call(domain_service.create_domain, req_pb, LOGIN)
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID + '_1'):
        domain_pb.spec.incomplete = False

    del domain_operation_request_pb.order.set_fqdns.fqdns[:]
    domain_operation_request_pb.order.set_fqdns.fqdns.extend(['b.other.ru', 'c.other.ru'])
    domain_operation_request_pb.order.set_fqdns.cert_order.content.subject_alternative_names.extend(['b.other.ru',
                                                                                                     'c.other.ru'])
    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.set_fqdns": FQDN "b.other.ru" already exists in domain "hyperspace:hyperspace_1"'):  # noqa
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.meta.id = NAMESPACE_ID + '_1'
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_non_unique_fqdns_cache(cache, zk_storage, namespace, order_domain_request_pb, domain_operation_request_pb,
                                caplog):
    order_domain_request_pb.order.cert_order.content.subject_alternative_names.extend(['*.my.test'])
    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.extend(['good1.my.test', 'good2.my.test'])
    call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID))

    with pytest.raises(exceptions.BadRequestError,
                       match='"order.content.fqdns": FQDN "good1.my.test" already exists in domain '
                             '"hyperspace:hyperspace"'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)
        assert wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID))

    for pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        pb.order.cancelled.value = True
        pb.spec.incomplete = False
    assert wait_until(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID).order.cancelled.value)

    with check_log(caplog) as log:
        zk_storage.remove_domain(NAMESPACE_ID, NAMESPACE_ID)
        assert wait_until(lambda: cache.get_domain(NAMESPACE_ID, NAMESPACE_ID) is None)
        assert 'good1' not in cache.list_domain_fqdns()['my.test']
        assert 'good2' not in cache.list_domain_fqdns()['my.test']
        assert 'domain pb removed from cache: hyperspace/hyperspace' in log.records_text()
        assert 'domain order was cancelled, getting fqdns from order' in log.records_text()
        assert 'fqdns to process: good1.my.test, good2.my.test' in log.records_text()
        assert ("result for fqdn \"good1 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain', namespace_id=u'hyperspace', domain_id=u'hyperspace')"
                in log.records_text() or
                "result for fqdn \"good1 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain', namespace_id='hyperspace', domain_id='hyperspace')"
                in log.records_text()
                )
        assert ("result for fqdn \"good2 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain', namespace_id=u'hyperspace', domain_id=u'hyperspace')"
                in log.records_text() or
                "result for fqdn \"good2 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain', namespace_id='hyperspace', domain_id='hyperspace')"
                in log.records_text()
                )

    call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID))
    assert 'good1' in cache.list_domain_fqdns()['my.test']
    assert 'good2' in cache.list_domain_fqdns()['my.test']
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.spec.incomplete = False

    domain_operation_request_pb.order.set_fqdns.fqdns.extend(['a.other.ru', 'good1.my.test'])
    domain_operation_request_pb.order.set_fqdns.cert_order.content.common_name = 'a.other.ru'
    domain_operation_request_pb.order.set_fqdns.cert_order.content.subject_alternative_names.append('good1.my.test')
    domain_operation_request_pb.order.set_fqdns.cert_order.content.abc_service_id = 999
    domain_operation_request_pb.order.set_fqdns.cert_order.content.ca_name = 'test'
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    for op_pb in zk_storage.update_domain_operation(NAMESPACE_ID, NAMESPACE_ID):
        op_pb.spec.old_fqdns.extend(['good2.my.test'])
    assert wait_until(lambda: cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID).spec.old_fqdns)
    assert len(cache.list_domain_fqdns()['other.ru']['a']) == 1
    assert len(cache.list_domain_fqdns()['my.test']['good1']) == 2
    assert len(cache.list_domain_fqdns()['my.test']['good2']) == 1

    with check_log(caplog) as log:
        zk_storage.remove_domain_operation(NAMESPACE_ID, NAMESPACE_ID)
        assert wait_until(lambda: cache.get_domain_operation(NAMESPACE_ID, NAMESPACE_ID) is None)
        assert len(cache.list_domain_fqdns()['other.ru']['a']) == 1
        assert len(cache.list_domain_fqdns()['my.test']['good1']) == 1
        assert 'good2' not in cache.list_domain_fqdns()['my.test']
        assert 'domain op pb removed from cache: hyperspace/hyperspace' in log.records_text()
        assert 'old fqdns to process: good2.my.test' in log.records_text()
        assert 'new fqdns to process: a.other.ru, good1.my.test' in log.records_text()
        assert ("result for fqdn \"good2 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain', namespace_id=u'hyperspace', domain_id=u'hyperspace')"
                in log.records_text() or
                "result for fqdn \"good2 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain', namespace_id='hyperspace', domain_id='hyperspace')"
                in log.records_text())
        assert ("result for fqdn \"a . other.ru\": removed record "
                "domain_info_tuple(entity_type='domain op', namespace_id=u'hyperspace', domain_id=u'hyperspace')"
                in log.records_text() or
                "result for fqdn \"a . other.ru\": removed record "
                "domain_info_tuple(entity_type='domain op', namespace_id='hyperspace', domain_id='hyperspace')"
                in log.records_text())
        assert ("result for fqdn \"good1 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain op', namespace_id=u'hyperspace', domain_id=u'hyperspace')"
                in log.records_text() or
                "result for fqdn \"good1 . my.test\": removed record "
                "domain_info_tuple(entity_type='domain op', namespace_id='hyperspace', domain_id='hyperspace')"
                in log.records_text())


def test_fqdns_not_in_cert_order(namespace, order_domain_request_pb):
    del order_domain_request_pb.order.fqdns[:]
    order_domain_request_pb.order.fqdns.extend(['*.my.test', 'some.yandex.ru'])
    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content": FQDNs "\*.my.test", "some.yandex.ru" are not covered by certificate order'):  # noqa
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.cert_order.content.subject_alternative_names.extend(['*.my.test', '*.yandex.ru'])
    call(domain_service.create_domain, order_domain_request_pb, LOGIN)


def test_fqdns_not_in_cert_order_in_op(zk_storage, namespace, order_domain_request_pb, domain_operation_request_pb):
    call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.spec.incomplete = False

    domain_operation_request_pb.order.set_fqdns.fqdns.append('op.yandex.ru')
    domain_operation_request_pb.order.set_fqdns.cert_order.content.common_name = 'another.yandex.ru'
    domain_operation_request_pb.order.set_fqdns.cert_order.content.abc_service_id = 999
    domain_operation_request_pb.order.set_fqdns.cert_order.content.ca_name = 'test'
    with pytest.raises(
        exceptions.BadRequestError,
        match='"order.set_fqdns": FQDNs "op.yandex.ru" are not covered by certificate order'):  # noqa
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.cert_order.content.subject_alternative_names.append('*.yandex.ru')
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_fqdns_not_in_cert_ref(zk_storage, cache, namespace):
    cert_id = 'cert1'
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = NAMESPACE_ID
    zk_storage.create_cert(namespace_id=NAMESPACE_ID, cert_id=cert_id, cert_pb=cert_pb)
    assert wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, cert_id))

    req_pb = create_order_domain_request_pb()
    req_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    req_pb.order.fqdns.extend(['*.my.test', 'some.yandex.ru'])
    req_pb.order.cert_ref.id = cert_id
    req_pb.order.include_upstreams.type = modules_pb2.ALL

    with pytest.raises(
        exceptions.BadRequestError,
        match=r'"order.content": FQDNs "\*.my.test", "some.yandex.ru" are not covered by certificate "cert1"'):  # noqa
        call(domain_service.create_domain, req_pb, LOGIN)

    cert_pb.spec.fields.subject_alternative_names.extend(['*.my.test', '*.yandex.ru'])
    for pb in zk_storage.update_cert(NAMESPACE_ID, cert_id):
        pb.CopyFrom(cert_pb)
    assert wait_until(
        lambda: (cache.get_cert(NAMESPACE_ID, cert_id).spec.fields.subject_alternative_names == ['*.my.test',
                                                                                                 '*.yandex.ru']),
        timeout=1)
    call(domain_service.create_domain, req_pb, LOGIN)


def test_fqdns_not_in_cert_ref_in_op(zk_storage, cache, namespace, order_domain_request_pb,
                                     domain_operation_request_pb):
    call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.spec.incomplete = False

    cert_id = 'cert1'
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = NAMESPACE_ID
    zk_storage.create_cert(namespace_id=NAMESPACE_ID, cert_id=cert_id, cert_pb=cert_pb)
    assert wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, cert_id))

    domain_operation_request_pb.order.set_fqdns.fqdns.extend([u'op.yandex.ru', u'привет.яндекс.рф'])
    domain_operation_request_pb.order.set_fqdns.cert_ref.id = cert_id
    with pytest.raises(exceptions.BadRequestError) as e:
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)
    assert six.text_type(e.value) == (u'"order.set_fqdns": FQDNs "op.yandex.ru", "привет.яндекс.рф" '
                                      u'are not covered by certificate "cert1"')

    cert_pb.spec.fields.subject_alternative_names.extend([u'*.yandex.ru', u'*.яндекс.рф'])
    for pb in zk_storage.update_cert(NAMESPACE_ID, cert_id):
        pb.CopyFrom(cert_pb)
    assert wait_until(
        lambda: (cache.get_cert(NAMESPACE_ID, cert_id).spec.fields.subject_alternative_names == [u'*.yandex.ru',
                                                                                                 u'*.яндекс.рф']),
        timeout=1)
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_list_domains(namespace):
    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    domain_pbs = {}
    for i, _id in enumerate(ids):
        create_namespace(_id)

        req_pb = create_order_domain_request_pb()
        req_pb.meta.id = _id
        req_pb.meta.namespace_id = _id
        req_pb.order.fqdns.append('{}.in.yandex-team.ru'.format(_id))
        req_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
        req_pb.order.cert_order.content.common_name = '{}.in.yandex-team.ru'.format(_id)
        req_pb.order.cert_order.content.ca_name = 'test'
        req_pb.order.cert_order.content.abc_service_id = 9999
        req_pb.order.include_upstreams.type = modules_pb2.ALL

        domain_pbs[_id] = call(domain_service.create_domain, req_pb, LOGIN).domain

    def check_list():
        list_pb = api_pb2.ListDomainsRequest(namespace_id='aaa')
        pb = call(domain_service.list_domains, list_pb, LOGIN)
        assert pb.total == 1
        assert len(pb.domains) == 1
        assert [b.meta.id for b in pb.domains] == ['aaa']
        assert all(pb.domains[0].HasField(f) for f in ('meta', 'order',))

    wait_until_passes(check_list)

    req_pb = api_pb2.ListDomainsRequest(namespace_id='bbb', skip=1)
    resp_pb = call(domain_service.list_domains, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.domains) == 0

    create_namespace('eee')
    req_pb = api_pb2.CreateDomainRequest()
    req_pb.meta.id = 'eee'
    req_pb.meta.namespace_id = 'eee'
    req_pb.meta.comment = COMMENT
    req_pb.order.fqdns.append('eee.in.yandex-team.ru')
    req_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    req_pb.order.cert_order.content.common_name = 'eee.in.yandex-team.ru'
    req_pb.order.cert_order.content.ca_name = 'test'
    req_pb.order.cert_order.content.abc_service_id = 9999
    req_pb.order.include_upstreams.type = modules_pb2.ALL

    domain_pbs[req_pb.meta.id] = call(domain_service.create_domain, req_pb, LOGIN).domain

    def check_list():
        pb = call(domain_service.list_domains, api_pb2.ListDomainsRequest(namespace_id='eee'), LOGIN)
        assert len(pb.domains) == 1

    wait_until_passes(check_list)

    req_pb = api_pb2.CreateDomainRequest()
    req_pb.meta.id = 'aaa2'
    req_pb.meta.namespace_id = 'aaa'
    req_pb.meta.comment = COMMENT
    req_pb.order.fqdns.append('aaa2.in.yandex-team.ru')
    req_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    req_pb.order.cert_order.content.common_name = 'aaa2.in.yandex-team.ru'
    req_pb.order.cert_order.content.ca_name = 'test'
    req_pb.order.cert_order.content.abc_service_id = 9999
    req_pb.order.include_upstreams.type = modules_pb2.ALL

    domain_pbs[req_pb.meta.id] = call(domain_service.create_domain, req_pb, LOGIN).domain

    def check_list():
        req_pb = api_pb2.ListDomainsRequest(namespace_id='aaa')
        pb = call(domain_service.list_domains, req_pb, LOGIN)
        assert len(pb.domains) == 2

        req_pb.query.id_regexp = 'aaa'
        pb = call(domain_service.list_domains, req_pb, LOGIN)
        assert len(pb.domains) == 2

        req_pb.query.id_regexp = '2$'
        pb = call(domain_service.list_domains, req_pb, LOGIN)
        assert len(pb.domains) == 1

        req_pb.query.id_regexp = '??'
        with pytest.raises(exceptions.BadRequestError,
                           match='"query.id_regexp" contains invalid regular expression: nothing to repeat'):
            call(domain_service.list_domains, req_pb, LOGIN)

    wait_until_passes(check_list)


def test_list_domain_operations(zk_storage, namespace):
    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    domain_op_pbs = {}
    for _id in ids:
        domain_pb = model_pb2.Domain()
        domain_pb.meta.namespace_id = NAMESPACE_ID
        domain_pb.meta.id = _id
        domain_pb.meta.version = 'xxx'
        domain_pb.spec.incomplete = False
        zk_storage.create_domain(namespace_id=NAMESPACE_ID, domain_id=_id, domain_pb=domain_pb)

        req_pb = api_pb2.CreateDomainOperationRequest()
        req_pb.meta.id = _id
        req_pb.meta.namespace_id = NAMESPACE_ID
        req_pb.meta.comment = COMMENT
        req_pb.order.set_protocol.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY

        domain_op_pbs[_id] = call(domain_service.create_domain_operation, req_pb, LOGIN).operation

    def check_list():
        list_pb = api_pb2.ListDomainOperationsRequest(namespace_id=NAMESPACE_ID)
        pb = call(domain_service.list_domain_operations, list_pb, LOGIN)
        assert pb.total == 4
        assert len(pb.operations) == 4

    wait_until_passes(check_list)

    req_pb = api_pb2.ListDomainOperationsRequest(namespace_id=NAMESPACE_ID, skip=4)
    resp_pb = call(domain_service.list_domain_operations, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.operations) == 0


def test_cancel_domain_order(zk_storage, cache, namespace, order_domain_request_pb):
    resp_pb = call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    domain_pb = resp_pb.domain
    assert domain_pb.meta.id == NAMESPACE_ID

    req_pb = api_pb2.CancelDomainOrderRequest()
    req_pb.id = NAMESPACE_ID
    req_pb.namespace_id = NAMESPACE_ID
    req_pb.version = 'xxx'

    for d_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        d_pb.order.status.status = OverallStatus.FINISHED.name
    assert wait_until(lambda: (cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID).order.status.status ==
                               OverallStatus.FINISHED.name),
                      timeout=1)
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel order that is not in progress'):
        call(domain_service.cancel_domain_order, req_pb, LOGIN)

    for d_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        d_pb.order.status.status = OverallStatus.IN_PROGRESS.name
    assert wait_until(lambda: (cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID).order.status.status ==
                               OverallStatus.IN_PROGRESS.name))
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel domain order at this stage'):
        call(domain_service.cancel_domain_order, req_pb, LOGIN)

    for op_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        op_pb.order.progress.state.id = 'STARTED'
    assert wait_until(lambda: (cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID).order.progress.state.id == 'STARTED'))
    call(domain_service.cancel_domain_order, req_pb, LOGIN)

    def check():
        get_req_pb = api_pb2.GetDomainRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
        pb = call(domain_service.get_domain, get_req_pb, LOGIN)
        assert pb.domain.order.cancelled.value

    wait_until_passes(check)


def test_tld_domain_order(zk_storage, namespace, order_domain_request_pb, cache):
    order_domain_request_pb.order.type = model_pb2.DomainSpec.Config.YANDEX_TLD

    with pytest.raises(exceptions.BadRequestError,
                       match='"order.content": YANDEX_TLD domain must have protocol HTTP_ONLY'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    order_domain_request_pb.order.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.content": YANDEX_TLD domain cannot have custom fqdns'):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)

    del order_domain_request_pb.order.fqdns[:]
    call(domain_service.create_domain, order_domain_request_pb, LOGIN)
    wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID))

    order_domain_request_pb.meta.id = 'another_tld_domain'
    with pytest.raises(exceptions.BadRequestError,
                       match='Only one YANDEX_TLD domain can exist in a namespace. Existing: {}'.format(NAMESPACE_ID)):
        call(domain_service.create_domain, order_domain_request_pb, LOGIN)


def test_tld_operation(zk_storage, namespace, domain_operation_request_pb):
    create_domain_pb(zk_storage)
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID):
        domain_pb.spec.yandex_balancer.config.type = model_pb2.DomainSpec.Config.YANDEX_TLD

    domain_operation_request_pb.order.set_protocol.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='You can only change upstreams for YANDEX_TLD domain'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.ClearField('set_protocol')
    domain_operation_request_pb.order.set_fqdns.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='You can only change upstreams for YANDEX_TLD domain'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.ClearField('set_fqdns')
    domain_operation_request_pb.order.set_cert.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='You can only change upstreams for YANDEX_TLD domain'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.ClearField('set_cert')
    domain_operation_request_pb.order.set_upstreams.include_upstreams.type = modules_pb2.ALL
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_operation_invalid(zk_storage, namespace, domain_operation_request_pb):
    with pytest.raises(exceptions.BadRequestError, match='"order" must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.SetInParent()
    with pytest.raises(exceptions.NotFoundError, match='Domain "hyperspace:hyperspace" does not exist'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    create_domain_pb(zk_storage)
    with pytest.raises(exceptions.BadRequestError, match='Operation field not specified'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_upstreams.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='order.set_upstreams.include_upstreams": must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_upstreams.include_upstreams.SetInParent()
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.set_upstreams.include_upstreams.filter.any": must be true if '
                             '"order.set_upstreams.include_upstreams.type is NONE"'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_upstreams.include_upstreams.type = modules_pb2.BY_ID
    with pytest.raises(exceptions.BadRequestError, match='ids: is required'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_upstreams.include_upstreams.ids.append('123')
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_operation_set_fqdns(namespace, domain_operation_request_pb, domain_pb):
    domain_operation_request_pb.order.set_fqdns.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"order.set_fqdns": "fqdns" or "shadow_fqdns" must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.fqdns.append('eee.in.yandex-team.ru')
    with pytest.raises(exceptions.BadRequestError, match='"cert_order" or "cert_ref" must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.cert_order.content.common_name = 'wrong.in.yandex-team.ru'
    domain_operation_request_pb.order.set_fqdns.cert_order.content.ca_name = 'test'
    domain_operation_request_pb.order.set_fqdns.cert_order.content.abc_service_id = 9999
    domain_operation_request_pb.order.set_fqdns.secondary_cert_order.content.common_name = 'wrong1.in.yandex-team.ru'
    domain_operation_request_pb.order.set_fqdns.secondary_cert_order.content.ca_name = 'test1'
    domain_operation_request_pb.order.set_fqdns.secondary_cert_order.content.abc_service_id = 9998

    with pytest.raises(exceptions.BadRequestError, match='"order.set_fqdns": field "common_name" in primary and '
                                                         'secondary certs must match: '
                                                         '"wrong.in.yandex-team.ru" != "wrong1.in.yandex-team.ru"'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.secondary_cert_order.content.common_name = 'wrong.in.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError, match='"order.set_fqdns": field "public_key_algorithm_id" in '
                                                         'primary and secondary certs must be different'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.cert_order.content.public_key_algorithm_id = 'ec'
    domain_operation_request_pb.order.set_fqdns.secondary_cert_order.content.public_key_algorithm_id = 'rsa'
    with pytest.raises(exceptions.BadRequestError, match='"order.set_fqdns": FQDNs "eee.in.yandex-team.ru" are not '
                                                         'covered by certificate order'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_fqdns.cert_order.content.common_name = 'eee.in.yandex-team.ru'
    domain_operation_request_pb.order.set_fqdns.secondary_cert_order.content.common_name = 'eee.in.yandex-team.ru'
    domain_operation_request_pb.order.set_fqdns.fqdns.append('eee.in.yandex-team.ru')
    with pytest.raises(exceptions.BadRequestError, match='"order.set_fqdns": FQDN "eee.in.yandex-team.ru" is '
                                                         'used more than once in this domain'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_operation_set_cert(zk_storage, cache, namespace, domain_operation_request_pb, domain_pb):
    domain_operation_request_pb.order.set_cert.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"cert_order" or "cert_ref" must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_cert.cert_ref.id = 'test'
    with pytest.raises(exceptions.BadRequestError, match='"order.set_cert.cert_ref.id": cert "test" not found'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    create_cert(zk_storage)
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID, domain_pb):
        domain_pb.spec.yandex_balancer.config.fqdns.append('missing.fqdn')
    assert wait_until(
        lambda: (cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID).spec.yandex_balancer.config.fqdns[-1] ==
                 'missing.fqdn'))

    domain_operation_request_pb.order.set_cert.secondary_cert_ref.id = 'test'
    with pytest.raises(exceptions.BadRequestError, match='"order.set_cert.secondary_cert_ref.id": must be different '
                                                         'from "order.set_cert.cert_ref.id"'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_cert.ClearField('secondary_cert_ref')
    with pytest.raises(exceptions.BadRequestError, match='"order.set_cert": FQDNs "missing.fqdn" are not '
                                                         'covered by certificate "test"'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    for cert_pb in zk_storage.update_cert(NAMESPACE_ID, 'test'):
        cert_pb.spec.fields.subject_alternative_names.append('missing.fqdn')
    assert wait_until(
        lambda: ('missing.fqdn' in cache.get_cert(NAMESPACE_ID, 'test').spec.fields.subject_alternative_names))
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_operation_set_protocol_http(namespace, domain_operation_request_pb, domain_pb):
    domain_operation_request_pb.order.set_protocol.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"protocol" cannot be NONE'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_protocol.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    domain_operation_request_pb.order.set_protocol.set_redirect_to_https.redirect_to_https.SetInParent()
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.set_protocol.set_redirect_to_https": '
                             '"redirect_to_https" can be enabled only for HTTP_AND_HTTPS'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_protocol.ClearField('set_redirect_to_https')
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_transfer_no_cert(namespace, domain_operation_request_pb, domain_pb):
    domain_operation_request_pb.order.transfer.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"order.transfer.include_upstreams": must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.transfer.include_upstreams.type = modules_pb2.ALL
    with pytest.raises(exceptions.BadRequestError, match='"order.transfer.target_namespace_id": is required'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.transfer.target_namespace_id = 'new'
    with pytest.raises(exceptions.NotFoundError, match='Namespace "new" not found'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    create_namespace('new')
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_transfer_with_cert(zk_storage, cache, namespace, domain_operation_request_pb, domain_pb):
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID, domain_pb):
        domain_pb.spec.yandex_balancer.config.cert.id = 'test'
    domain_operation_request_pb.order.transfer.target_namespace_id = 'new'
    create_namespace('new')
    with pytest.raises(exceptions.BadRequestError, match='"order.transfer.include_upstreams": must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.transfer.include_upstreams.type = modules_pb2.ALL
    with pytest.raises(exceptions.NotFoundError, match='Certificate "hyperspace:test" not found'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    create_cert(zk_storage)
    with pytest.raises(exceptions.BadRequestError,
                       match='Cannot transfer certificate stored in Nanny Vault: "hyperspace:test"'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    for cert_pb in zk_storage.update_cert(NAMESPACE_ID, 'test'):
        cert_pb.spec.storage.ya_vault_secret.secret_id = '123'
    assert wait_until(lambda: (cache.get_cert(NAMESPACE_ID, 'test').spec.storage.ya_vault_secret.secret_id == '123'))

    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID, domain_pb):
        domain_pb.spec.yandex_balancer.config.secondary_cert.id = 'test2'
    assert wait_until(lambda: (
        cache.get_domain(NAMESPACE_ID, NAMESPACE_ID).spec.yandex_balancer.config.secondary_cert.id == 'test2'))
    with pytest.raises(exceptions.NotFoundError, match='Certificate "hyperspace:test2" not found'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID, domain_pb):
        domain_pb.spec.yandex_balancer.config.ClearField('secondary_cert')
    assert wait_until(lambda: (
        not cache.get_domain(NAMESPACE_ID, NAMESPACE_ID).spec.yandex_balancer.config.secondary_cert.id))

    create_cert_renewal(zk_storage, target_rev=zk_storage.must_get_cert(NAMESPACE_ID, CERT_ID).meta.version)
    with pytest.raises(exceptions.BadRequestError,
                       match='Certificate "hyperspace:test" must be renewed before transferring the domain'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)
    zk_storage.remove_cert_renewal(NAMESPACE_ID, CERT_ID)

    domain_pb = model_pb2.Domain(meta=model_pb2.DomainMeta(id=NAMESPACE_ID + '_1', namespace_id=NAMESPACE_ID))
    domain_pb.spec.incomplete = True
    domain_pb.order.content.cert_ref.id = 'test'
    zk_storage.create_domain(NAMESPACE_ID, NAMESPACE_ID + '_1', domain_pb)
    assert wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID + '_1'))
    domain_pb = model_pb2.Domain(meta=model_pb2.DomainMeta(id=NAMESPACE_ID + '_2', namespace_id=NAMESPACE_ID))
    domain_pb.spec.incomplete = False
    domain_pb.spec.yandex_balancer.config.cert.id = 'test'
    zk_storage.create_domain(NAMESPACE_ID, NAMESPACE_ID + '_2', domain_pb)
    assert wait_until_passes(lambda: cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID + '_2'))
    domain_op_pb = model_pb2.DomainOperation(meta=model_pb2.DomainMeta(id=NAMESPACE_ID + '_3',
                                                                       namespace_id=NAMESPACE_ID))
    domain_op_pb.order.content.set_fqdns.cert_ref.id = 'test'
    zk_storage.create_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_3', domain_op_pb)
    assert wait_until_passes(lambda: cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_3'))
    domain_op_pb = model_pb2.DomainOperation(meta=model_pb2.DomainMeta(id=NAMESPACE_ID + '_4',
                                                                       namespace_id=NAMESPACE_ID))
    domain_op_pb.order.content.set_protocol.cert_ref.id = 'test'
    zk_storage.create_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_4', domain_op_pb)
    assert wait_until_passes(lambda: cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_4'))
    domain_op_pb = model_pb2.DomainOperation(meta=model_pb2.DomainMeta(id=NAMESPACE_ID + '_5',
                                                                       namespace_id=NAMESPACE_ID))
    domain_op_pb.order.content.set_cert.cert_ref.id = 'test'
    zk_storage.create_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_5', domain_op_pb)
    assert wait_until_passes(lambda: cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_5'))

    with pytest.raises(exceptions.BadRequestError,
                       match='Cannot transfer certificate "hyperspace:test" used in multiple domains: '
                             '"hyperspace_1", "hyperspace_2", "hyperspace_3", "hyperspace_4", "hyperspace_5"'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    zk_storage.remove_domain(NAMESPACE_ID, NAMESPACE_ID + '_1')
    zk_storage.remove_domain(NAMESPACE_ID, NAMESPACE_ID + '_2')
    zk_storage.remove_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_3')
    zk_storage.remove_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_4')
    zk_storage.remove_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_5')
    assert wait_until_passes(lambda: cache.get_domain_operation(NAMESPACE_ID, NAMESPACE_ID + '_5') is None)
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_operation_set_protocol_https(cache, zk_storage, namespace, domain_operation_request_pb, domain_pb):
    domain_operation_request_pb.order.set_protocol.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    domain_operation_request_pb.order.set_protocol.set_redirect_to_https.redirect_to_https.SetInParent()
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.set_protocol.set_redirect_to_https": '
                             '"redirect_to_https" can be enabled only for HTTP_AND_HTTPS'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_protocol.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.set_protocol": "cert_order" or "cert_ref" must be set'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    domain_operation_request_pb.order.set_protocol.cert_ref.id = 'test'
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.set_protocol.cert_ref.id": cert "test" not found'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    create_cert(zk_storage)
    for domain_pb in zk_storage.update_domain(NAMESPACE_ID, NAMESPACE_ID, domain_pb):
        domain_pb.spec.yandex_balancer.config.fqdns.append('missing.fqdn')
    assert wait_until(
        lambda: (cache.must_get_domain(NAMESPACE_ID, NAMESPACE_ID).spec.yandex_balancer.config.fqdns[-1] ==
                 'missing.fqdn'),
        timeout=1)
    with pytest.raises(exceptions.BadRequestError, match='"order.set_protocol": FQDNs "missing.fqdn" are not '
                                                         'covered by certificate "test"'):
        call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    for cert_pb in zk_storage.update_cert(NAMESPACE_ID, 'test'):
        cert_pb.spec.fields.subject_alternative_names.append('missing.fqdn')
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)


def test_cancel_domain_operation(zk_storage, cache, namespace, domain_operation_request_pb, domain_pb):
    domain_operation_request_pb.order.set_protocol.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    call(domain_service.create_domain_operation, domain_operation_request_pb, LOGIN)

    req_pb = api_pb2.CancelDomainOperationRequest()
    req_pb.id = NAMESPACE_ID
    req_pb.namespace_id = NAMESPACE_ID
    req_pb.version = 'xxx'

    for op_pb in zk_storage.update_domain_operation(NAMESPACE_ID, NAMESPACE_ID):
        op_pb.order.status.status = OverallStatus.FINISHED.name
    assert wait_until(lambda: (cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID).order.status.status ==
                               OverallStatus.FINISHED.name),
                      timeout=1)
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel operation that is not in progress'):
        call(domain_service.cancel_domain_operation, req_pb, LOGIN)

    for op_pb in zk_storage.update_domain_operation(NAMESPACE_ID, NAMESPACE_ID):
        op_pb.order.status.status = OverallStatus.IN_PROGRESS.name
    assert wait_until(lambda: (cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID).order.status.status ==
                               OverallStatus.IN_PROGRESS.name))
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel domain operation at this stage'):
        call(domain_service.cancel_domain_operation, req_pb, LOGIN)

    for op_pb in zk_storage.update_domain_operation(NAMESPACE_ID, NAMESPACE_ID):
        op_pb.order.progress.state.id = 'STARTED'
    assert wait_until(lambda: (
        cache.must_get_domain_operation(NAMESPACE_ID, NAMESPACE_ID).order.progress.state.id == 'STARTED'))
    call(domain_service.cancel_domain_operation, req_pb, LOGIN)

    def check():
        get_req_pb = api_pb2.GetDomainOperationRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
        pb = call(domain_service.get_domain_operation, get_req_pb, LOGIN)
        assert pb.operation.order.cancelled.value

    wait_until_passes(check)
