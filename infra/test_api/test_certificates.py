# coding=utf-8
import inject
import mock
import pytest
import time
from datetime import datetime

import six
from sepelib.core import config

from infra.swatlib.auth import abc
from infra.awacs.proto import api_pb2, model_pb2
from awacs.lib import certificator
from awacs.lib.order_processor.model import OverallStatus
from awacs.lib.rpc import exceptions
from awacs.model.util import SECONDS_IN_DAY
from awacs.web import certificate_service
from awacs.web import namespace_service
from awacs.web.validation.util import DOMAIN_PATTERN
from awtest.api import call, create_namespace_with_order_in_progress, set_login_to_root_users
from awtest.core import wait_until, wait_until_passes
from awtest.mocks.certificator import MockCertificatorClient


LOGIN = 'login'
LOGIN2 = 'login2'
GROUP = "1"
COMMENT = 'Creating turbo certificate'
NAMESPACE_ID = 'hyperspace'


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client):
    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        b.bind(certificator.ICertificatorClient, MockCertificatorClient)
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    config.set_value('certificator.environment', 'production')
    yield
    inject.clear()
    config.set_value('certificator.environment', None)


def create_namespace(namespace_id):
    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_id
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    call(namespace_service.create_namespace, req_pb, 'bleed')


@pytest.fixture
def namespace():
    create_namespace(NAMESPACE_ID)


def create_order_cert_request_pb():
    req_pb = api_pb2.OrderCertificateRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    return req_pb


@pytest.fixture
def order_cert_request_pb():
    req_pb = create_order_cert_request_pb()
    req_pb.order.common_name = 'm0.in.*.yandex-team.ru'
    req_pb.order.ca_name = 'InternalCA'
    req_pb.order.abc_service_id = 9999
    return req_pb


def make_cert_renewal_pb(spec_pb, namespace_id, cert_id, not_before=None, paused=False, incomplete=False):
    cert_renewal_pb = model_pb2.CertificateRenewal()
    cert_renewal_pb.meta.namespace_id = namespace_id
    cert_renewal_pb.meta.id = cert_id
    cert_renewal_pb.meta.version = 'xxx'
    cert_renewal_pb.meta.auth.type = model_pb2.Auth.STAFF
    cert_renewal_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    cert_renewal_pb.order.content.common_name = 'm0.in.*.yandex-team.ru'
    cert_renewal_pb.spec.CopyFrom(spec_pb)
    cert_renewal_pb.spec.storage.ya_vault_secret.secret_ver = 'zzz'
    cert_renewal_pb.spec.fields.validity.not_before.FromDatetime(not_before or datetime.utcnow())
    cert_renewal_pb.spec.incomplete = incomplete
    if paused:
        cert_renewal_pb.meta.paused.value = paused
    return cert_renewal_pb


def make_cert_pb(spec_pb, namespace_id, cert_id, not_before=None, not_after=None, incomplete=False):
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.namespace_id = namespace_id
    cert_pb.meta.id = cert_id
    cert_pb.meta.version = 'xxx'
    cert_pb.meta.auth.type = model_pb2.Auth.STAFF
    cert_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    cert_pb.order.content.common_name = 'm0.in.*.yandex-team.ru'
    cert_pb.spec.CopyFrom(spec_pb)
    cert_pb.spec.storage.ya_vault_secret.secret_ver = 'zzz'
    cert_pb.spec.fields.validity.not_after.FromDatetime(not_after or datetime.utcnow())
    cert_pb.spec.fields.validity.not_before.FromDatetime(not_before or datetime.utcnow())
    cert_pb.spec.incomplete = incomplete
    return cert_pb


@pytest.fixture
def cert_renewal_pb(create_cert_req_pb, zk_storage):
    cert_renewal_pb = make_cert_renewal_pb(create_cert_req_pb.spec, NAMESPACE_ID, NAMESPACE_ID)
    zk_storage.create_cert_renewal(namespace_id=NAMESPACE_ID,
                                   cert_renewal_id=NAMESPACE_ID,
                                   cert_renewal_pb=cert_renewal_pb)
    return cert_renewal_pb


@pytest.fixture
def paused_cert_renewal_pb(create_cert_req_pb, zk_storage):
    cert_renewal_pb = make_cert_renewal_pb(create_cert_req_pb.spec, NAMESPACE_ID, NAMESPACE_ID)
    cert_renewal_pb.meta.paused.value = True
    zk_storage.create_cert_renewal(namespace_id=NAMESPACE_ID,
                                   cert_renewal_id=NAMESPACE_ID,
                                   cert_renewal_pb=cert_renewal_pb)
    return cert_renewal_pb


def make_create_cert_req_pb():
    req_pb = api_pb2.CreateCertificateRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.storage.ya_vault_secret.secret_id = 'xxx'
    req_pb.spec.storage.ya_vault_secret.secret_ver = 'yyy'
    req_pb.spec.imported.abc_service_id = 456
    return req_pb


@pytest.fixture
def create_cert_req_pb():
    return make_create_cert_req_pb()


def test_tls_certificate_issuability_validation(zk_storage, cache, order_cert_request_pb, enable_auth):
    order_cert_request_pb.order.common_name = u'yandex.ru'
    order_cert_request_pb.order.subject_alternative_names.append(u'google.com')
    order_cert_request_pb.order.ca_name = u'CertumProductionCA'
    order_cert_request_pb.meta.id = u'cert-id'
    with pytest.raises(exceptions.BadRequestError,
                       match=u'TLS certificate for FQDN "google.com" can not be issued automatically'):
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)


def test_forbidden_operations_during_namespace_order(zk_storage, cache, order_cert_request_pb, enable_auth):
    # forbid removal, editing, and order cancelling
    create_namespace_with_order_in_progress(zk_storage, cache, NAMESPACE_ID)
    req_pb = make_create_cert_req_pb()

    with pytest.raises(exceptions.ForbiddenError, match='Can only be created directly by roots, use OrderCertificate instead'):
        call(certificate_service.create_certificate, req_pb, LOGIN2)
    set_login_to_root_users(LOGIN2)
    c_pb_1 = call(certificate_service.create_certificate, req_pb, LOGIN2).certificate

    order_cert_request_pb.order.common_name = 'm0.in.yandex-team.ru'
    order_cert_request_pb.meta.id = 'c2'
    c_pb_2 = call(certificate_service.order_certificate, order_cert_request_pb, LOGIN).certificate

    req_pb = api_pb2.RemoveCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID, version=c_pb_1.meta.version,
                                              state=model_pb2.CertificateSpec.REMOVED_FROM_AWACS)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(certificate_service.remove_certificate, req_pb, LOGIN)

    req_pb = api_pb2.CancelCertificateOrderRequest(namespace_id=NAMESPACE_ID, id='c2', version=c_pb_2.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(certificate_service.cancel_certificate_order, req_pb, LOGIN)

    req_pb = api_pb2.UpdateCertificateRequest(meta=c_pb_1.meta)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(certificate_service.update_certificate, req_pb, LOGIN)


def test_forbidden_operations_during_transfer(zk_storage, cache, namespace, create_cert_req_pb, enable_auth):
    create_cert_req_pb.meta.is_being_transferred.value = True
    set_login_to_root_users(LOGIN2)
    c_pb = call(certificate_service.create_certificate, create_cert_req_pb, LOGIN2).certificate
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID))

    req_pb = api_pb2.RemoveCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID, version=c_pb.meta.version,
                                              state=model_pb2.CertificateSpec.REMOVED_FROM_AWACS)
    with pytest.raises(exceptions.ForbiddenError, match="Cannot modify certificate while it's being transferred"):
        call(certificate_service.remove_certificate, req_pb, LOGIN)

    req_pb = api_pb2.RestoreCertificateFromBackupRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID,
                                                         version=c_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match="Cannot modify certificate while it's being transferred"):
        call(certificate_service.cancel_certificate_order, req_pb, LOGIN)

    req_pb = api_pb2.UnpauseCertificateRenewalRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID,
                                                      version=c_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match="Cannot modify certificate while it's being transferred"):
        call(certificate_service.update_certificate, req_pb, LOGIN)

    req_pb = api_pb2.ForceCertificateRenewalRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID,
                                                    version=c_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match="Cannot modify certificate while it's being transferred"):
        call(certificate_service.update_certificate, req_pb, LOGIN)


def test_order_get_remove_certificate(zk_storage, cache, mongo_storage, namespace, order_cert_request_pb):
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"m0.in.*.yandex-team.ru" in "order.common_name" is not valid: hostname wildcard ' \
                                     'can\'t contain asterisk in a non-leading position'

    order_cert_request_pb.order.common_name = 'm0.*.*.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"m0.*.*.yandex-team.ru" in "order.common_name" is not valid: hostname wildcard' \
                                     ' can\'t contain more than 1 asterisk'

    order_cert_request_pb.order.common_name = '*a.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"*a.yandex-team.ru" in "order.common_name" is not valid: hostname wildcard ' \
                                     'must start with *.'

    order_cert_request_pb.order.common_name = 'a' * 100 + '.a'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"{}" in "order.common_name" is not valid: hostname length must be less than 65'.format(
        order_cert_request_pb.order.common_name)

    order_cert_request_pb.order.common_name = 'bad.domain.name?'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"bad.domain.name?" in "order.common_name" is not valid: string "name?"' \
                                     ' does not match pattern: {}'.format(DOMAIN_PATTERN)

    order_cert_request_pb.order.common_name = u'bad-domain-name'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"bad-domain-name" in "order.common_name" is not valid: ' \
                                     'hostname must contain at least one "."'

    order_cert_request_pb.order.common_name = '0m.in.yandex-team.ru'
    order_cert_request_pb.order.subject_alternative_names.extend(['m1.*.yandex-team.ru'])
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"m1.*.yandex-team.ru" in "order.subject_alternative_names" is not valid: hostname ' \
                                     'wildcard can\'t contain asterisk in a non-leading position'

    del order_cert_request_pb.order.subject_alternative_names[:]
    order_cert_request_pb.order.subject_alternative_names.extend(['0m.in.yandex-team.ru'])
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"order.common_name" must not be included in "order.subject_alternative_names": ' \
                                     '"0m.in.yandex-team.ru"'

    del order_cert_request_pb.order.subject_alternative_names[:]
    for i in range(101):
        order_cert_request_pb.order.subject_alternative_names.extend(['a{}.in.y-t.ru'.format(i)])
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert six.text_type(e.value) == '"order.subject_alternative_names" cannot contain more than 100 domains. ' \
                                     'Current amount: 101'

    order_cert_request_pb.order.ttl = 420
    del order_cert_request_pb.order.subject_alternative_names[:]
    order_cert_request_pb.order.subject_alternative_names.extend(['m.in.yandex-team.ru', 'm2.in.yandex-team.ru'])
    with pytest.raises(exceptions.ForbiddenError, match='"order.ttl" can be set only by admins'):
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN, enable_auth=True)

    with pytest.raises(exceptions.BadRequestError, match='"order.ttl": incorrect value "420", '
                                                         'must be between 14 and 182'):
        call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)

    order_cert_request_pb.order.ttl = 69
    resp_pb = call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    b_pb = resp_pb.certificate
    assert b_pb.meta.id == NAMESPACE_ID
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.comment == COMMENT
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]
    assert b_pb.order.content.common_name == '0m.in.yandex-team.ru'
    assert b_pb.order.content.ca_name == 'InternalCA'
    assert b_pb.order.content.subject_alternative_names == ['m.in.yandex-team.ru', 'm2.in.yandex-team.ru']
    assert b_pb.order.content.ttl == 69

    del order_cert_request_pb.order.subject_alternative_names[:]
    order_cert_request_pb.order.common_name = '*.in.yandex-team.ru'
    order_cert_request_pb.order.subject_alternative_names.extend(['*.tt.in.yandex-team.ru', 'm2.in.yandex-team.ru'])
    order_cert_request_pb.meta.id = NAMESPACE_ID + '_0'
    resp_pb = call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    b_pb = resp_pb.certificate
    assert b_pb.meta.id == NAMESPACE_ID + '_0'
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.comment == COMMENT
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]
    assert b_pb.order.content.common_name == '*.in.yandex-team.ru'
    assert b_pb.order.content.ca_name == 'InternalCA'
    assert b_pb.order.content.subject_alternative_names == ['*.tt.in.yandex-team.ru', 'm2.in.yandex-team.ru']

    # test empty required fields
    req_pb = create_order_cert_request_pb()
    req_pb.meta.id = NAMESPACE_ID + '_1'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"order" must be set'
    req_pb.order.subject_alternative_names.extend(['m1.in.yandex-team.ru', 'm2.in.yandex-team.ru'])
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"order.abc_service_id" must be set'
    req_pb.order.abc_service_id = 9999
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"order.common_name" must be set'
    req_pb.order.common_name = 'm1.in.yandex-team.ru'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"order.ca_name" must be set'
    req_pb.order.ca_name = '...'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.order_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"order.common_name" must not be included in "order.subject_alternative_names": ' \
                                     '"m1.in.yandex-team.ru"'
    req_pb.order.common_name = u'привет.рф'
    req_pb.order.subject_alternative_names.append(u'*.привет.рф')

    call(certificate_service.order_certificate, req_pb, LOGIN)

    # Test get
    req_pb = api_pb2.GetCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
    resp_pb = call(certificate_service.get_certificate, req_pb, LOGIN)
    b_pb = resp_pb.certificate
    assert b_pb.order.content.subject_alternative_names == ['m.in.yandex-team.ru', 'm2.in.yandex-team.ru']

    for c_pb in zk_storage.update_cert(NAMESPACE_ID, NAMESPACE_ID):
        c_pb.spec.fields.serial_number = '432745928323474932744197'
    cert_pb = call(certificate_service.get_certificate, req_pb, LOGIN).certificate
    assert cert_pb.spec.fields.serial_number == '5BA3342B00020009C005'

    req_pb = api_pb2.GetCertificateRequest(namespace_id='missing', id='missing')
    with pytest.raises(exceptions.NotFoundError):
        call(certificate_service.get_certificate, req_pb, LOGIN)

    items, count = mongo_storage.list_cert_revs(namespace_id=NAMESPACE_ID, cert_id=NAMESPACE_ID)
    assert count == 1

    # Test remove with unfinished order
    req_pb = api_pb2.RemoveCertificateRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID,
        version=b_pb.meta.version,
        state=model_pb2.CertificateSpec.REVOKED_AND_REMOVED_FROM_AWACS_AND_STORAGE
    )
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.remove_certificate, req_pb, LOGIN)
    assert (six.text_type(e.value) == "Cannot remove certificate while it's being ordered. "
                                      "Please either cancel the order or wait until it's finished.")

    # Test remove with finished order
    req_pb = make_create_cert_req_pb()
    req_pb.meta.id = NAMESPACE_ID + '2'
    req_pb.spec.storage.type = model_pb2.CertificateSpec.Storage.NANNY_VAULT
    req_pb.spec.storage.nanny_vault_secret.keychain_id = '123'
    req_pb.spec.storage.nanny_vault_secret.secret_id = '123'
    req_pb.spec.storage.nanny_vault_secret.secret_revision_id = '123'
    req_pb.spec.storage.ya_vault_secret.secret_id = ''
    set_login_to_root_users(LOGIN2)
    resp_pb = call(certificate_service.create_certificate, req_pb, LOGIN2)
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID + '2'))
    req_pb = api_pb2.RemoveCertificateRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID + '2',
        version=resp_pb.certificate.meta.version,
        state=model_pb2.CertificateSpec.PRESENT
    )
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.remove_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == 'No "state" specified.'
    req_pb.state = model_pb2.CertificateSpec.REVOKED_AND_REMOVED_FROM_AWACS_AND_STORAGE
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.remove_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == 'Cannot revoke certificate: no "certificator.order_id" in cert spec'
    req_pb.state = model_pb2.CertificateSpec.REMOVED_FROM_AWACS_AND_STORAGE
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.remove_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == 'Cannot remove certificate from storage: NANNY_VAULT storage is not supported'
    req_pb.state = model_pb2.CertificateSpec.REMOVED_FROM_AWACS
    resp_pb = call(certificate_service.remove_certificate, req_pb, LOGIN)

    def check_cert():
        req_get_pb = api_pb2.GetCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID + '2')
        pb = call(certificate_service.get_certificate, req_get_pb, LOGIN)
        assert pb.certificate.spec.state == model_pb2.CertificateSpec.REMOVED_FROM_AWACS

    wait_until_passes(check_cert)

    # Test revoke with copy
    for cert_pb in zk_storage.update_cert(NAMESPACE_ID, NAMESPACE_ID + '2'):
        cert_pb.meta.unrevokable.value = True
        cert_pb.meta.unrevokable.author = 'robot'
        cert_pb.meta.unrevokable.comment = "It's a copy of {0}:{0}".format(NAMESPACE_ID + '0')
        cert_pb.spec.certificator.order_id = '999'
    wait_until(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID + '2').meta.unrevokable.value, timeout=1)
    req_pb = api_pb2.RemoveCertificateRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID + '2',
        version=resp_pb.certificate.meta.version,
        state=model_pb2.CertificateSpec.REVOKED_AND_REMOVED_FROM_AWACS_AND_STORAGE
    )
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.remove_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == 'Cannot revoke certificate: "It\'s a copy of hyperspace0:hyperspace0"'


def test_list_certificates(namespace):
    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    certificate_pbs = {}
    for i, _id in enumerate(ids):
        create_namespace(_id)

        req_pb = create_order_cert_request_pb()
        req_pb.meta.id = _id
        req_pb.meta.namespace_id = _id
        req_pb.order.common_name = '{}.in.yandex-team.ru'.format(_id)
        req_pb.order.ca_name = 'test'
        req_pb.order.abc_service_id = 9999

        certificate_pbs[_id] = call(certificate_service.order_certificate, req_pb, LOGIN).certificate

    def check_list():
        list_pb = api_pb2.ListCertificatesRequest(namespace_id='aaa')
        pb = call(certificate_service.list_certificates, list_pb, LOGIN)
        assert pb.total == 1
        assert len(pb.certificates) == 1
        assert [b.meta.id for b in pb.certificates] == ['aaa']
        assert all(pb.certificates[0].HasField(f) for f in ('meta', 'order',))

    wait_until_passes(check_list)

    req_pb = api_pb2.ListCertificatesRequest(namespace_id='bbb', skip=1)
    resp_pb = call(certificate_service.list_certificates, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.certificates) == 0

    # add yet another certificates
    create_namespace('eee')

    req_pb = api_pb2.OrderCertificateRequest()
    req_pb.meta.id = 'eee'
    req_pb.meta.namespace_id = 'eee'
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.order.common_name = 'eee.in.yandex-team.ru'
    req_pb.order.ca_name = 'test'
    req_pb.order.abc_service_id = 9999

    certificate_pbs[req_pb.meta.id] = call(certificate_service.order_certificate, req_pb, LOGIN).certificate

    def check_list():
        pb = call(certificate_service.list_certificates, api_pb2.ListCertificatesRequest(namespace_id='eee'), LOGIN)
        assert len(pb.certificates) == 1

    wait_until_passes(check_list)

    req_pb = api_pb2.OrderCertificateRequest()
    req_pb.meta.id = 'aaa2'
    req_pb.meta.namespace_id = 'aaa'
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.order.common_name = 'aaa2.in.yandex-team.ru'
    req_pb.order.ca_name = 'test'
    req_pb.order.abc_service_id = 9999

    certificate_pbs[req_pb.meta.id] = call(certificate_service.order_certificate, req_pb, LOGIN).certificate

    def check_list():
        req_pb = api_pb2.ListCertificatesRequest(namespace_id='aaa')
        pb = call(certificate_service.list_certificates, req_pb, LOGIN)
        assert len(pb.certificates) == 2

        req_pb.query.id_regexp = 'aaa'
        pb = call(certificate_service.list_certificates, req_pb, LOGIN)
        assert len(pb.certificates) == 2

        req_pb.query.id_regexp = '2$'
        pb = call(certificate_service.list_certificates, req_pb, LOGIN)
        assert len(pb.certificates) == 1

        req_pb.query.id_regexp = '??'
        with pytest.raises(exceptions.BadRequestError,
                           match='"query.id_regexp" contains invalid regular expression: nothing to repeat'):
            call(certificate_service.list_certificates, req_pb, LOGIN)

    wait_until_passes(check_list)


def test_update_certificate(cache, namespace, order_cert_request_pb):
    order_cert_request_pb.order.common_name = 'm0.in.yandex-team.ru'
    resp_pb = call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    assert resp_pb.certificate.meta.id == NAMESPACE_ID
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID))

    req_pb = api_pb2.UpdateCertificateRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.version = resp_pb.certificate.meta.version
    req_pb.meta.comment = COMMENT

    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.update_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"meta.auth" must be set'

    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.append('robot2')
    resp_pb = call(certificate_service.update_certificate, req_pb, LOGIN)

    req_pb.meta.version = resp_pb.certificate.meta.version
    req_pb.spec.fields.public_key_info.algorithm_id = 'ec'

    with pytest.raises(exceptions.ForbiddenError) as e:
        call(certificate_service.update_certificate, req_pb, LOGIN, enable_auth=True)
    assert six.text_type(e.value) == 'Only admins can update certificate spec'

    resp_pb = call(certificate_service.update_certificate, req_pb, LOGIN)

    req_pb.meta.unrevokable.value = True
    req_pb.meta.unrevokable.comment = 'comment'
    req_pb.meta.version = resp_pb.certificate.meta.version
    with pytest.raises(exceptions.ForbiddenError, match='"meta.unrevokable" can only be changed by roots'):
        call(certificate_service.update_certificate, req_pb, LOGIN)
    set_login_to_root_users(LOGIN)
    resp_pb = call(certificate_service.update_certificate, req_pb, LOGIN)
    assert resp_pb.certificate.meta.unrevokable.value
    assert resp_pb.certificate.meta.unrevokable.author == LOGIN
    assert resp_pb.certificate.meta.unrevokable.comment == 'comment'


def test_update_certificate_renewal(namespace, cert_renewal_pb):
    req_pb = api_pb2.UpdateCertificateRenewalRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.version = cert_renewal_pb.meta.version
    req_pb.meta.comment = COMMENT

    req_pb.meta.version = req_pb.meta.version
    req_pb.spec.fields.public_key_info.algorithm_id = 'ec'

    with pytest.raises(exceptions.ForbiddenError) as e:
        call(certificate_service.update_certificate_renewal, req_pb, LOGIN, enable_auth=True)
    assert six.text_type(e.value) == 'Only admins can update certificate spec'

    resp_pb = call(certificate_service.update_certificate_renewal, req_pb, LOGIN)
    assert resp_pb.certificate_renewal.spec.fields.public_key_info.algorithm_id == 'ec'

    # check that we can't change paused status
    req_pb.meta.version = resp_pb.certificate_renewal.meta.version
    req_pb.meta.paused.comment = 'changed'
    resp_pb = call(certificate_service.update_certificate_renewal, req_pb, LOGIN)
    assert resp_pb.certificate_renewal.meta.paused.comment == ''

    req_pb.meta.version = resp_pb.certificate_renewal.meta.version
    req_pb.meta.target_rev = 'new_rev'
    resp_pb = call(certificate_service.update_certificate_renewal, req_pb, LOGIN)
    assert resp_pb.certificate_renewal.meta.target_rev == 'new_rev'

    req_pb.meta.version = resp_pb.certificate_renewal.meta.version
    req_pb.meta.auth.type = model_pb2.Auth.STAFF
    req_pb.meta.auth.staff.owners.logins.append('robot2')
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.update_certificate_renewal, req_pb, LOGIN)
    assert six.text_type(e.value) == 'Changing certificate renewal auth is prohibited'


def test_cancel_certificate_order(zk_storage, cache, namespace, order_cert_request_pb):
    order_cert_request_pb.order.common_name = 'm0.in.yandex-team.ru'
    order_cert_request_pb.order.subject_alternative_names.extend(['m1.in.yandex-team.ru', 'm2.in.yandex-team.ru'])

    resp_pb = call(certificate_service.order_certificate, order_cert_request_pb, LOGIN)
    b_pb = resp_pb.certificate
    assert b_pb.meta.id == NAMESPACE_ID

    req_pb = api_pb2.CancelCertificateOrderRequest()
    req_pb.id = NAMESPACE_ID
    req_pb.namespace_id = NAMESPACE_ID
    req_pb.version = 'xxx'

    for c_pb in zk_storage.update_cert(NAMESPACE_ID, NAMESPACE_ID):
        c_pb.order.status.status = OverallStatus.FINISHED.name
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.cancel_certificate_order, req_pb, LOGIN)
    assert six.text_type(e.value) == 'Cannot cancel order that is not in progress'

    for c_pb in zk_storage.update_cert(NAMESPACE_ID, NAMESPACE_ID):
        c_pb.order.status.status = OverallStatus.IN_PROGRESS.name
    assert wait_until(lambda: (cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID).order.status.status == 'IN_PROGRESS'))
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel cert order at this stage'):
        call(certificate_service.cancel_certificate_order, req_pb, LOGIN)

    for op_pb in zk_storage.update_cert(NAMESPACE_ID, NAMESPACE_ID):
        op_pb.order.progress.state.id = 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'
    assert wait_until(lambda: (cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID).order.progress.state.id ==
                               'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'))
    call(certificate_service.cancel_certificate_order, req_pb, LOGIN)

    def check():
        get_req_pb = api_pb2.GetCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
        pb = call(certificate_service.get_certificate, get_req_pb, LOGIN)
        assert pb.certificate.order.cancelled.value

    wait_until_passes(check)


def test_create_certificate(namespace, create_cert_req_pb):
    set_login_to_root_users(LOGIN)
    create_cert_req_pb.meta.id = 'hyperspace.1'
    resp_pb = call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)
    b_pb = resp_pb.certificate
    assert b_pb.meta.id == 'hyperspace.1'
    assert b_pb.spec.storage.ya_vault_secret.secret_id == create_cert_req_pb.spec.storage.ya_vault_secret.secret_id
    assert b_pb.spec.storage.ya_vault_secret.secret_ver == create_cert_req_pb.spec.storage.ya_vault_secret.secret_ver

    with pytest.raises(exceptions.ConflictError) as e:
        call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)
    assert six.text_type(e.value) == 'Certificate "hyperspace.1" already exists in namespace "hyperspace".'

    create_cert_req_pb.meta.id = "hyperspace_1"
    with pytest.raises(exceptions.ConflictError) as e:
        call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)
    assert six.text_type(e.value) == '"meta.id": normalized id matches existing certificate "hyperspace.1". ' \
                                     'To avoid this, change or add any alphanumeric characters in this id'

    create_cert_req_pb.meta.id = "hyperspace_2"
    create_cert_req_pb.spec.storage.ya_vault_secret.secret_id = 'asd'
    call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)


def test_create_certificate_invalid(namespace):
    set_login_to_root_users(LOGIN)
    req_pb = api_pb2.CreateCertificateRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.storage.ya_vault_secret.secret_id = 'xxx'
    req_pb.spec.storage.ya_vault_secret.secret_ver = 'yyy'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.create_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.imported" must be set if source is IMPORTED'

    req_pb.spec.source = model_pb2.CertificateSpec.CERTIFICATOR
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.create_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.certificator" must be set if source is CERTIFICATOR'

    req_pb.spec.source = model_pb2.CertificateSpec.CERTIFICATOR_TESTING
    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.create_certificate, req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.certificator" must be set if source is CERTIFICATOR_TESTING'


def test_restore_certificate_from_backup(cache, zk_storage, namespace, create_cert_req_pb, cert_renewal_pb):
    set_login_to_root_users(LOGIN)
    resp_pb = call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)
    req_pb = api_pb2.RestoreCertificateFromBackupRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID,
        version=resp_pb.certificate.meta.version,
    )
    for c_pb in zk_storage.update_cert_renewal(NAMESPACE_ID, NAMESPACE_ID):
        c_pb.meta.target_rev = resp_pb.certificate.meta.version

    with pytest.raises(exceptions.BadRequestError) as e:
        call(certificate_service.restore_certificate_from_backup, req_pb, LOGIN)
    assert six.text_type(e.value) == 'Certificate backup has already expired'

    create_cert_req_pb.spec.fields.validity.not_after.FromSeconds(int(time.time()) + SECONDS_IN_DAY)
    create_cert_req_pb.spec.storage.ya_vault_secret.secret_ver = 'zzz'
    create_cert_req_pb.meta.id = NAMESPACE_ID + '1'
    resp_pb = call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID + '1'))
    cert_renewal_pb.meta.target_rev = resp_pb.certificate.meta.version
    cert_renewal_pb.meta.id = NAMESPACE_ID + '1'
    zk_storage.create_cert_renewal(NAMESPACE_ID, NAMESPACE_ID + '1', cert_renewal_pb)

    req_pb = api_pb2.RestoreCertificateFromBackupRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID + '1',
        version=resp_pb.certificate.meta.version,
    )

    call(certificate_service.restore_certificate_from_backup, req_pb, LOGIN)

    def check():
        get_req_pb = api_pb2.GetCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID + '1')
        b_pb = call(certificate_service.get_certificate, get_req_pb, LOGIN)
        assert b_pb.certificate.spec.storage.ya_vault_secret.secret_ver == 'zzz'
        r_pb = zk_storage.get_cert_renewal(namespace_id=NAMESPACE_ID, cert_renewal_id=NAMESPACE_ID + '1')
        assert r_pb.meta.paused.value

    wait_until_passes(check)


def test_force_cert_renewal(cache, zk_storage, namespace, create_cert_req_pb, cert_renewal_pb):
    set_login_to_root_users(LOGIN)
    create_cert_req_pb.spec.fields.validity.not_after.FromSeconds(int(time.time()) + SECONDS_IN_DAY)
    resp_pb = call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID))
    req_pb = api_pb2.ForceCertificateRenewalRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID,
        version=resp_pb.certificate.meta.version,
    )
    call(certificate_service.force_certificate_renewal, req_pb, LOGIN)

    get_req_pb = api_pb2.GetCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)

    def check():
        b_pb = call(certificate_service.get_certificate, get_req_pb, LOGIN)
        assert b_pb.certificate.meta.force_renewal.value
        assert b_pb.certificate.meta.force_renewal.author == LOGIN
        assert b_pb.certificate.meta.force_renewal.comment == 'Forced certificate renewal'

    wait_until_passes(check)

    with pytest.raises(exceptions.BadRequestError, match='Renewal is already forced'):
        call(certificate_service.force_certificate_renewal, req_pb, LOGIN)


def test_unpause_cert_renewal(cache, zk_storage, namespace, create_cert_req_pb, paused_cert_renewal_pb):
    set_login_to_root_users(LOGIN)
    config.set_value('run.allow_automatic_cert_management', False)
    resp_pb = call(certificate_service.create_certificate, create_cert_req_pb, LOGIN)
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID))
    req_pb = api_pb2.UnpauseCertificateRenewalRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID,
        version=resp_pb.certificate.meta.version,
    )

    call(certificate_service.unpause_certificate_renewal, req_pb, LOGIN)

    get_req_pb = api_pb2.GetCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
    pb = call(certificate_service.get_certificate, get_req_pb, LOGIN)
    assert pb.certificate.spec.storage.ya_vault_secret.secret_ver == 'yyy'
    r_pb = zk_storage.get_cert_renewal(namespace_id=NAMESPACE_ID, cert_renewal_id=NAMESPACE_ID)
    assert not r_pb.meta.paused.value
    assert not r_pb.meta.HasField('target_discoverability')


def test_unpause_cert_renewal_with_discoverability(cache, zk_storage, namespace, create_cert_req_pb,
                                                   paused_cert_renewal_pb):
    set_login_to_root_users(LOGIN2)
    config.set_value('run.allow_automatic_cert_management', False)
    resp_pb = call(certificate_service.create_certificate, create_cert_req_pb, LOGIN2)
    wait_until_passes(lambda: cache.must_get_cert(NAMESPACE_ID, NAMESPACE_ID))
    target_discoverability_pb = model_pb2.DiscoverabilityCondition()
    target_discoverability_pb.default.value = False
    target_discoverability_pb.per_location.values['MAN'].value = True
    req_pb = api_pb2.UnpauseCertificateRenewalRequest(
        namespace_id=NAMESPACE_ID,
        id=NAMESPACE_ID,
        version=resp_pb.certificate.meta.version,
        target_discoverability=target_discoverability_pb
    )

    with pytest.raises(exceptions.ForbiddenError) as e:
        call(certificate_service.unpause_certificate_renewal, req_pb, LOGIN)
    assert six.text_type(e.value) == u'"target_discoverability" can only be set by roots'

    set_login_to_root_users(LOGIN)
    call(certificate_service.unpause_certificate_renewal, req_pb, LOGIN)
    get_req_pb = api_pb2.GetCertificateRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID)
    pb = call(certificate_service.get_certificate, get_req_pb, LOGIN)
    assert pb.certificate.spec.storage.ya_vault_secret.secret_ver == 'yyy'
    r_pb = zk_storage.get_cert_renewal(namespace_id=NAMESPACE_ID, cert_renewal_id=NAMESPACE_ID)
    assert not r_pb.meta.paused.value

    assert r_pb.meta.HasField('target_discoverability')
    discoverability_pb = r_pb.meta.target_discoverability
    assert not discoverability_pb.default.value
    assert discoverability_pb.default.author == LOGIN
    assert discoverability_pb.default.mtime.seconds > 0
    assert discoverability_pb.per_location.values['MAN'].value
    assert discoverability_pb.per_location.values['MAN'].author == LOGIN
    assert discoverability_pb.per_location.values['MAN'].mtime.seconds > 0


def test_list_get_certificate_renewals(zk_storage, namespace, create_cert_req_pb):
    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    certificate_renewal_pbs = {}
    for i, _id in enumerate(ids):
        create_namespace(_id)
        cert_renewal_pb = make_cert_renewal_pb(create_cert_req_pb.spec, _id, _id)
        zk_storage.create_cert_renewal(namespace_id=_id,
                                       cert_renewal_id=_id,
                                       cert_renewal_pb=cert_renewal_pb)
        certificate_renewal_pbs[_id] = cert_renewal_pb

    def check_list():
        list_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id='aaa')
        pb = call(certificate_service.list_certificate_renewals, list_pb, LOGIN)
        assert pb.total == 1
        assert len(pb.certificate_renewals) == 1
        assert [b.meta.id for b in pb.certificate_renewals] == ['aaa']
        assert all(pb.certificate_renewals[0].HasField(f) for f in ('meta', 'order', 'spec',))

    wait_until_passes(check_list)

    req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id='bbb', skip=1)
    resp_pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.certificate_renewals) == 0

    # add yet another certificates
    create_namespace('eee')

    cert_renewal_pb = make_cert_renewal_pb(create_cert_req_pb.spec, 'eee', 'eee')
    zk_storage.create_cert_renewal(namespace_id='eee',
                                   cert_renewal_id='eee',
                                   cert_renewal_pb=cert_renewal_pb)

    certificate_renewal_pbs[cert_renewal_pb.meta.id] = cert_renewal_pb

    def check_list():
        pb = call(certificate_service.list_certificate_renewals,
                  api_pb2.ListCertificateRenewalsRequest(namespace_id='eee'),
                  LOGIN)
        assert len(pb.certificate_renewals) == 1
        r_pb = api_pb2.GetCertificateRenewalRequest(namespace_id='eee', id='eee')
        pb = call(certificate_service.get_certificate_renewal, r_pb, LOGIN)
        assert pb.certificate_renewal == cert_renewal_pb

    wait_until_passes(check_list)

    def check_list():
        for c_pb in zk_storage.update_cert_renewal('eee', 'eee'):
            c_pb.spec.fields.serial_number = '2215605510564605947266274012786846555'
        r_pb = api_pb2.GetCertificateRenewalRequest(namespace_id='eee', id='eee')
        cert_pb = call(certificate_service.get_certificate_renewal, r_pb, LOGIN).certificate_renewal
        assert cert_pb.spec.fields.serial_number == '1AAB5C9194CB1C2AFE170FD441FEB5B'

    wait_until_passes(check_list)

    def check_list_all():
        pb = call(certificate_service.list_certificate_renewals,
                  api_pb2.ListCertificateRenewalsRequest(),
                  LOGIN)
        assert len(pb.certificate_renewals) == 5

    wait_until_passes(check_list_all)


def test_list_get_certificate_renewals_query(zk_storage, create_cert_req_pb):
    paused = [True, True, True, False, False]
    incomplete = [False, True, False, True, False]
    target_not_after = list(map(datetime.utcfromtimestamp, [5000, 40000, 30000, 20000, 10000]))
    not_before = list(map(datetime.utcfromtimestamp, [55000, 20000, 30000, 40000, 50000]))
    namespace_id = 'test'
    create_namespace(namespace_id)
    certificate_renewal_pbs = {}
    certificate_pbs = {}
    for i in range(0, 5):
        cert_id = 'cert-{}'.format(i)
        cert_renewal_pb = make_cert_renewal_pb(
            create_cert_req_pb.spec, namespace_id, cert_id,
            paused=paused[i],
            incomplete=incomplete[i],
            not_before=not_before[i])
        zk_storage.create_cert_renewal(namespace_id=namespace_id, cert_renewal_id=cert_id, cert_renewal_pb=cert_renewal_pb)
        certificate_renewal_pbs[cert_id] = cert_renewal_pb

        cert_pb = make_cert_pb(create_cert_req_pb.spec, namespace_id, cert_id, incomplete=False, not_after=target_not_after[i])
        zk_storage.create_cert(namespace_id=namespace_id, cert_id=cert_id, cert_pb=cert_pb)
        certificate_pbs[cert_id] = cert_pb

    def check_list_1():
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        req_pb.query.id_regexp = 'cert-[01]'
        pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert pb.total == 2
        assert set(b.meta.id for b in pb.certificate_renewals) == {'cert-0', 'cert-1'}

    wait_until_passes(check_list_1)

    def check_list_2():
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        req_pb.query.incomplete.value = True
        pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert pb.total == incomplete.count(True)

    wait_until_passes(check_list_2)

    def check_list_3():
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        req_pb.query.paused.value = False
        pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert pb.total == paused.count(False)

    wait_until_passes(check_list_3)

    def check_list_4():
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        req_pb.query.validity_not_before.gte.FromDatetime(datetime.utcfromtimestamp(15000))
        req_pb.query.validity_not_before.lte.FromDatetime(datetime.utcfromtimestamp(35000))
        pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert pb.total == 2

    wait_until_passes(check_list_4)

    def check_list_5():
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        req_pb.query.validity_not_before.gte.FromDatetime(datetime.utcfromtimestamp(35000))
        req_pb.query.paused.value = True
        req_pb.query.incomplete.value = True
        pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert pb.total == 0

    wait_until_passes(check_list_5)

    def check_list_6():
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        req_pb.query.validity_not_before.lte.FromDatetime(datetime.utcfromtimestamp(35000))
        req_pb.query.paused.value = True
        req_pb.query.incomplete.value = True
        pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert pb.total == 1

    wait_until_passes(check_list_6)

    def check_list_7():
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        resp_pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert [pb.meta.id for pb in resp_pb.certificate_renewals] == ['cert-{}'.format(i) for i in range(5)]

        expected = ['cert-0', 'cert-4', 'cert-3', 'cert-2', 'cert-1']
        req_pb = api_pb2.ListCertificateRenewalsRequest(namespace_id=namespace_id)
        req_pb.sort_target = req_pb.TARGET_CERT_VALIDITY_NOT_AFTER
        resp_pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        assert [pb.meta.id for pb in resp_pb.certificate_renewals] == expected

        req_pb.sort_order = api_pb2.DESCEND
        resp_pb = call(certificate_service.list_certificate_renewals, req_pb, LOGIN)
        expected.reverse()
        assert [pb.meta.id for pb in resp_pb.certificate_renewals] == expected

    wait_until_passes(check_list_7)
