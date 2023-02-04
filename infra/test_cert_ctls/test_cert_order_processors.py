import logging

import inject
import mock
import pytest
import ujson
from sepelib.core import config

from awacs.lib import certificator, ya_vault, startrekclient
from awacs.lib.certificator import CertificatorClient
from awacs.model.certs.processors import processors as p
from awacs.model.certs.processors import renewal_processors as rp
from awacs.model.certs.processors.processors import CertOrder
from awacs.model.certs.processors.renewal_processors import CertRenewalOrder
from infra.awacs.proto import model_pb2
from awtest import wait_until, check_log
from awtest.mocks.yav import MockYavClient
from awtest.mocks.certificator import MockCertificatorClient
from awtest.mocks.startrek import SECTASK_ID, MockStartrekClient


CERT_ID = 'cert-id'
NS_ID = 'namespace-id'


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(certificator.ICertificatorClient, MockCertificatorClient)
        b.bind(ya_vault.IYaVaultClient, MockYavClient)
        b.bind(startrekclient.IStartrekClient, MockStartrekClient)
        binder(b)

    inject.clear_and_configure(configure)
    startrekclient.IStartrekClient.instance().issues[SECTASK_ID].__init__()  # reset modifications
    yield
    inject.clear()


def create_cert_(cache, zk_storage, progress_state_name):
    meta = model_pb2.CertificateMeta(id=CERT_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.auth.staff.owners.logins.append('robot')
    cert_pb = model_pb2.Certificate(meta=meta)
    cert_pb.spec.incomplete = True
    cert_pb.order.content.common_name = 'test.common.name'
    cert_pb.order.content.subject_alternative_names.extend(['test1.common.name', 'test2.common.name'])
    cert_pb.order.content.abc_service_id = 999
    cert_pb.order.content.ca_name = 'Internal'
    cert_pb.order.progress.state.id = progress_state_name
    zk_storage.create_cert(namespace_id=NS_ID,
                           cert_id=CERT_ID,
                           cert_pb=cert_pb)
    wait_cert_(cache, lambda pb: pb)
    return CertOrder(cert_pb)


def create_cert_renewal_(cache, zk_storage, progress_state_name):
    meta = model_pb2.CertificateRenewalMeta(id=CERT_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    cert_pb = model_pb2.CertificateRenewal(meta=meta)
    cert_pb.spec.incomplete = True
    cert_pb.order.content.common_name = 'test.common.name'
    cert_pb.order.content.subject_alternative_names.extend(['test1.common.name', 'test2.common.name'])
    cert_pb.order.content.abc_service_id = 999
    cert_pb.order.content.ca_name = 'Internal'
    cert_pb.order.progress.state.id = progress_state_name
    zk_storage.create_cert_renewal(namespace_id=NS_ID,
                                   cert_renewal_id=CERT_ID,
                                   cert_renewal_pb=cert_pb)
    wait_cert_renewal_(cache, lambda pb: pb)
    original_cert_pb = create_cert_(cache, zk_storage, 'FINISH')
    wait_cert_(cache, lambda pb: original_cert_pb)
    return CertRenewalOrder(cert_pb)


def update_cert_(cache, zk_storage, cert, check):
    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.CopyFrom(cert.pb)
    wait_cert_(cache, check)
    cert.__init__(cert.pb)


def update_cert_renewal_(cache, zk_storage, cert_renewal, check):
    for pb in zk_storage.update_cert_renewal(NS_ID, CERT_ID):
        pb.CopyFrom(cert_renewal.pb)
    wait_cert_renewal_(cache, check)
    cert_renewal.__init__(cert_renewal.pb)


def wait_cert_(cache, check):
    assert wait_until(lambda: check(cache.get_cert(NS_ID, CERT_ID)), timeout=1)


def wait_cert_renewal_(cache, check):
    assert wait_until(lambda: check(cache.get_cert_renewal(NS_ID, CERT_ID)), timeout=1)


def get_cert_(cache):
    return CertOrder(cache.get_cert(NS_ID, CERT_ID))


def get_cert_renewal_(cache):
    return CertRenewalOrder(cache.get_cert_renewal(NS_ID, CERT_ID))


def test_start(cache, zk_storage, ctx):
    cert = create_cert_(cache, zk_storage, 'START')
    assert p.Start(cert).process(ctx).name == 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'


@pytest.mark.parametrize('processor,create_cert,update_cert,get_cert,final_state', [
    (p.SendingCreateRequestToCertificator, create_cert_, update_cert_, get_cert_,
     'ADDING_COMMENT_TO_SECTASK'),
    (rp.SendingCreateRequestToCertificator, create_cert_renewal_, update_cert_renewal_, get_cert_renewal_,
     'ADDING_COMMENT_TO_SECTASK'),
])
def test_sending_create_request(ctx, cache, zk_storage, processor, create_cert, update_cert, get_cert, final_state):
    create_cert(cache, zk_storage, 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR')
    original_cert = CertOrder(cache.get_cert(NS_ID, CERT_ID))

    del original_cert.pb.meta.auth.staff.owners.logins[:]
    update_cert_(cache, zk_storage, original_cert, lambda pb: pb.meta.auth.staff.owners.logins == [])
    cert = get_cert(cache)
    with pytest.raises(RuntimeError, match='cert has no owners'):
        processor(cert).process(ctx)

    original_cert.pb.meta.auth.staff.owners.logins.extend(['robot'])
    update_cert_(cache, zk_storage, original_cert, lambda pb: pb.meta.auth.staff.owners.logins == ['robot'])
    cert = get_cert(cache)

    for algo, is_ecc in (('rsa', False), ('ec', True)):
        cert.pb.order.content.public_key_algorithm_id = algo
        update_cert(cache, zk_storage, cert, lambda pb: pb.order.content.public_key_algorithm_id == algo)
        assert processor(cert).process(ctx).name == final_state
        resp = MockCertificatorClient.last_response

        assert cert.context['status'] == resp['status']
        assert resp['is_ecc'] == is_ecc

        assert cert.pb.spec.source == p.CERT_ORDER_SOURCES[MockCertificatorClient.environment]
        assert cert.pb.spec.certificator.order_id == 'url'
        assert cert.pb.spec.certificator.abc_service_id == cert.pb.order.content.abc_service_id
        assert cert.pb.spec.certificator.ca_name == cert.pb.order.content.ca_name
        assert cert.pb.spec.certificator.approval.startrek.issue_id == resp['st_issue_key']
        cert.pb.spec.certificator.order_id = ''

    for ca_name, desired_ttl_days in (('CertumProductionCA', 182), ('InternalTestCCA', None)):
        cert.pb.order.content.ca_name = ca_name
        update_cert(cache, zk_storage, cert, lambda pb: pb.order.content.ca_name == ca_name)
        assert processor(cert).process(ctx).name == final_state
        resp = MockCertificatorClient.last_response

        assert cert.context['status'] == resp['status']
        assert resp['desired_ttl_days'] == desired_ttl_days

        assert cert.pb.spec.source == p.CERT_ORDER_SOURCES[MockCertificatorClient.environment]
        assert cert.pb.spec.certificator.order_id == 'url'
        assert cert.pb.spec.certificator.abc_service_id == cert.pb.order.content.abc_service_id
        assert cert.pb.spec.certificator.ca_name == cert.pb.order.content.ca_name
        assert cert.pb.spec.certificator.approval.startrek.issue_id == resp['st_issue_key']
        cert.pb.spec.certificator.order_id = ''

    cert.pb.order.content.ttl = 420
    update_cert(cache, zk_storage, cert, lambda pb: pb.order.content.ttl == 420)
    assert processor(cert).process(ctx).name == final_state
    resp = MockCertificatorClient.last_response

    assert cert.context['status'] == resp['status']
    assert resp['desired_ttl_days'] == 182

    assert cert.pb.spec.source == p.CERT_ORDER_SOURCES[MockCertificatorClient.environment]
    assert cert.pb.spec.certificator.order_id == 'url'
    assert cert.pb.spec.certificator.abc_service_id == cert.pb.order.content.abc_service_id
    assert cert.pb.spec.certificator.ca_name == cert.pb.order.content.ca_name
    assert cert.pb.spec.certificator.approval.startrek.issue_id == resp['st_issue_key']
    cert.pb.spec.certificator.order_id = ''


@pytest.mark.parametrize('notify_on_expiration', [True, False])
def test_certificator_request_json(notify_on_expiration):
    with mock.patch('awacs.lib.certificator.json_request') as mock_json_request:
        client = CertificatorClient('mock_url')
        client.send_create_request('a', 'b', ['c'], 1, notify_on_expiration=notify_on_expiration)
        call_args = mock_json_request.call_args
        assert call_args[0] == ('post', 'mock_url/api/certificate/')
        assert call_args[1]['json'] == {
            'hosts': 'b,c',
            'ca_name': 'a',
            'notify_on_expiration': notify_on_expiration,
            'common_name': 'b',
            'is_ecc': False,
            'desired_ttl_days': None,
            'type': 'host',
            'abc_service': 1}


@pytest.mark.parametrize(
    'notify_on_expiration, response_noe, logging_called',
    [
        (True, False, 1),
        (False, True, 1),
        (True, True, 0),
        (False, False, 0)
    ]
)
def test_certificator_response_alerting(notify_on_expiration, response_noe, logging_called):
    with mock.patch('awacs.lib.certificator.json_request') as mock_json_request:
        mock_json_request.return_value= {'notify_on_expiration': response_noe}
        with mock.patch('awacs.lib.certificator.log.error') as mock_logging:
            client = CertificatorClient('mock_url')
            client.send_create_request('a', 'b', ['c'], 1, notify_on_expiration=notify_on_expiration)
            assert mock_logging.call_count == logging_called
            if logging_called:
                call_args = mock_logging.call_args[0]
                assert call_args[1] == notify_on_expiration
                assert call_args[2] == response_noe


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.SendingCreateRequestToCertificator, create_cert_, update_cert_,
     'ADDING_COMMENT_TO_SECTASK'),
    (rp.SendingCreateRequestToCertificator, create_cert_renewal_, update_cert_renewal_,
     'ADDING_COMMENT_TO_SECTASK'),
])
def test_sending_create_request_noop(ctx, cache, zk_storage, processor, create_cert, update_cert, final_state):
    cert = create_cert(cache, zk_storage, 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR')
    cert.context['status'] = 'ok'
    cert.pb.spec.certificator.order_id = '1'
    cert.pb.meta.auth.staff.owners.logins.extend(['robot'])
    assert processor(cert).process(ctx).name == final_state

    assert cert.pb.spec.certificator.order_id == '1'
    assert not cert.pb.spec.source
    assert not cert.pb.spec.certificator.abc_service_id
    assert not cert.pb.spec.certificator.ca_name
    assert not cert.pb.spec.certificator.approval.startrek.issue_id


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.AddingCommentToSectask, create_cert_, update_cert_,
     'ADDING_CERT_OWNERS_TO_SECTASK'),
    (rp.AddingCommentToSectask, create_cert_renewal_, update_cert_renewal_,
     'ADDING_CERT_OWNERS_TO_SECTASK'),
])
def test_adding_comment_to_sectask(ctx, caplog, cache, zk_storage, processor, create_cert, update_cert, final_state):
    cert = create_cert(cache, zk_storage, 'ADDING_COMMENT_TO_SECTASK')

    cert.pb.meta.auth.staff.owners.logins.extend(['robot'])
    cert.pb.spec.certificator.approval.startrek.issue_id = SECTASK_ID
    update_cert(cache, zk_storage, cert, lambda pb: pb.spec.certificator.approval.startrek.issue_id == SECTASK_ID)
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Adding comment to SECTASK-XXX' in log.records_text()


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.AddingCommentToSectask, create_cert_, update_cert_,
     'ADDING_CERT_OWNERS_TO_SECTASK'),
    (rp.AddingCommentToSectask, create_cert_renewal_, update_cert_renewal_,
     'ADDING_CERT_OWNERS_TO_SECTASK'),
])
def test_adding_comment_to_sectask_noop(ctx, caplog, cache, zk_storage, processor, create_cert, update_cert,
                                        final_state):
    cert = create_cert(cache, zk_storage, 'ADDING_COMMENT_TO_SECTASK')
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Adding comment to SECTASK-XXX' not in log.records_text()

    cert.pb.spec.certificator.approval.startrek.issue_id = SECTASK_ID
    cert.pb.order.progress.context['sectask_comment_id'] = ujson.dumps('fake_id')
    update_cert(cache, zk_storage, cert, lambda pb: pb.spec.certificator.approval.startrek.issue_id == SECTASK_ID)
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Adding comment to SECTASK-XXX' not in log.records_text()


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.AddingCertOwnersToSectask, create_cert_, update_cert_,
     'ADDING_AWACS_ON_DUTY_TO_SECTASK'),
    (rp.AddingCertOwnersToSectask, create_cert_renewal_, update_cert_renewal_,
     'ADDING_AWACS_ON_DUTY_TO_SECTASK'),
])
def test_adding_owners_to_sectask(ctx, caplog, cache, zk_storage, processor, create_cert, update_cert, final_state):
    create_cert(cache, zk_storage, 'ADDING_CERT_OWNERS_TO_SECTASK')
    cert = CertOrder(cache.get_cert(NS_ID, CERT_ID))  # always modify original cert
    cert.pb.spec.certificator.approval.startrek.issue_id = SECTASK_ID
    cert.pb.meta.auth.staff.owners.logins.append('not-a-robot')
    update_cert_(cache, zk_storage, cert, lambda pb: pb.spec.certificator.approval.startrek.issue_id == SECTASK_ID)
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Added followers to SECTASK-XXX: not-a-robot' in log.records_text()


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.AddingCertOwnersToSectask, create_cert_, update_cert_,
     'ADDING_AWACS_ON_DUTY_TO_SECTASK'),
    (rp.AddingCertOwnersToSectask, create_cert_renewal_, update_cert_renewal_,
     'ADDING_AWACS_ON_DUTY_TO_SECTASK'),
])
def test_adding_owners_to_sectask_noop(ctx, caplog, cache, zk_storage, processor, create_cert, update_cert,
                                       final_state):
    create_cert(cache, zk_storage, 'ADDING_CERT_OWNERS_TO_SECTASK')
    cert = CertOrder(cache.get_cert(NS_ID, CERT_ID))  # always modify original cert
    cert.pb.meta.auth.staff.owners.logins.append('not-a-robot')
    update_cert_(cache, zk_storage, cert, lambda pb: pb.meta.auth.staff.owners.logins == ['robot', 'not-a-robot'])
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'not-a-robot' not in log.records_text()


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.AddingAwacsOnDutyToSectask, create_cert_, update_cert_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
    (rp.AddingAwacsOnDutyToSectask, create_cert_renewal_, update_cert_renewal_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
])
def test_adding_on_duty_to_sectask(ctx, caplog, cache, zk_storage, processor, create_cert, update_cert, final_state):
    create_cert(cache, zk_storage, 'ADDING_AWACS_ON_DUTY_TO_SECTASK')
    cert = CertOrder(cache.get_cert(NS_ID, CERT_ID))  # always modify original cert
    cert.pb.spec.certificator.approval.startrek.issue_id = SECTASK_ID
    config.set_value('run.on_duty_users', ['not-a-robot'])
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Added followers to SECTASK-XXX: not-a-robot' in log.records_text()


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.AddingAwacsOnDutyToSectask, create_cert_, update_cert_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
    (rp.AddingAwacsOnDutyToSectask, create_cert_renewal_, update_cert_renewal_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
])
def test_adding_on_duty_to_sectask_noop(ctx, caplog, cache, zk_storage, processor, create_cert, update_cert,
                                        final_state):
    create_cert(cache, zk_storage, 'ADDING_AWACS_ON_DUTY_TO_SECTASK')
    cert = CertOrder(cache.get_cert(NS_ID, CERT_ID))  # always modify original cert
    config.set_value('run.on_duty_users', ['not-a-robot'])
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'not-a-robot' not in log.records_text()


def test_sent_request(ctx, cache, zk_storage):
    cert = create_cert_(cache, zk_storage, 'SENT_REQUEST_TO_CERTIFICATOR')
    assert p.SentRequestToCertificator(cert).process(ctx).name == 'ADDING_COMMENT_TO_SECTASK'


@pytest.mark.parametrize('processor,create_cert,final_state', [
    (p.PollingCertificatorForStorageInfo, create_cert_,
     'FETCHING_CERT_INFO_FROM_STORAGE'),
    (rp.PollingCertificatorForStorageInfo, create_cert_renewal_,
     'FETCHING_CERT_INFO_FROM_STORAGE'),
])
def test_polling(ctx, cache, zk_storage, processor, create_cert, final_state):
    cert = create_cert(cache, zk_storage, 'POLLING_CERTIFICATOR_FOR_STORAGE_INFO')
    assert processor(cert).process(ctx).name == final_state
    resp = MockCertificatorClient.get_cert()

    assert cert.pb.spec.storage.ya_vault_secret.secret_id == resp['yav_secret_id']
    assert cert.pb.spec.storage.ya_vault_secret.secret_ver == resp['yav_secret_version']
    assert cert.context['serial_number'] == resp['serial_number']


@pytest.mark.parametrize('processor,create_cert,final_state', [
    (p.PollingCertificatorForStorageInfo, create_cert_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
    (rp.PollingCertificatorForStorageInfo, create_cert_renewal_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
])
def test_polling_not_uploaded(ctx, cache, zk_storage, processor, create_cert, final_state):
    cert = create_cert(cache, zk_storage, 'POLLING_CERTIFICATOR_FOR_STORAGE_INFO')
    with mock.patch.object(MockCertificatorClient, 'get_cert', return_value={'uploaded_to_yav': False, 'status': '-'}):
        assert processor(cert).process(ctx).name == final_state

    assert not cert.pb.spec.storage.ya_vault_secret.secret_id
    assert not cert.pb.spec.storage.ya_vault_secret.secret_ver
    assert 'status' not in cert.pb.order.progress.context


@pytest.mark.parametrize('processor,create_cert,final_state', [
    (p.PollingCertificatorForStorageInfo, create_cert_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
    (rp.PollingCertificatorForStorageInfo, create_cert_renewal_,
     'POLLING_CERTIFICATOR_FOR_STORAGE_INFO'),
])
def test_polling_not_uploaded_needs_approve(ctx, cache, zk_storage, processor, create_cert, final_state):
    cert = create_cert(cache, zk_storage, 'POLLING_CERTIFICATOR_FOR_STORAGE_INFO')
    with mock.patch.object(MockCertificatorClient, 'get_cert', return_value={'uploaded_to_yav': False,
                                                                             'status': 'need_approve'}):
        assert processor(cert).process(ctx).name == final_state

    assert not cert.pb.spec.storage.ya_vault_secret.secret_id
    assert not cert.pb.spec.storage.ya_vault_secret.secret_ver
    assert cert.context['status'] == 'need_approve'


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.PollingCertificatorForStorageInfo, create_cert_, update_cert_,
     'FETCHING_CERT_INFO_FROM_STORAGE'),
    (rp.PollingCertificatorForStorageInfo, create_cert_renewal_, update_cert_renewal_,
     'FETCHING_CERT_INFO_FROM_STORAGE'),
])
def test_polling_noop(ctx, cache, zk_storage, processor, create_cert, update_cert, final_state):
    cert = create_cert(cache, zk_storage, 'POLLING_CERTIFICATOR_FOR_STORAGE_INFO')
    cert.pb.spec.storage.ya_vault_secret.secret_id = 'noop_id'
    cert.pb.spec.storage.ya_vault_secret.secret_ver = 'noop_ver'
    cert.context['serial_number'] = 'xxx'
    assert processor(cert).process(ctx).name == final_state

    assert cert.pb.spec.storage.ya_vault_secret.secret_id == 'noop_id'
    assert cert.pb.spec.storage.ya_vault_secret.secret_ver == 'noop_ver'


def test_got_storage_info(ctx, cache, zk_storage):
    cert = create_cert_(cache, zk_storage, 'GOT_STORAGE_INFO')
    assert p.GotStorageInfo(cert).process(ctx).name == 'FETCHING_CERT_INFO_FROM_STORAGE'


@pytest.mark.parametrize('processor,create_cert,final_state', [
    (p.FetchingCertInfoFromStorage, create_cert_, 'MODIFYING_YAV_SECRET_ACCESS'),
    (rp.FetchingCertInfoFromStorage, create_cert_renewal_, 'MODIFYING_YAV_SECRET_ACCESS'),
])
def test_fetching_cert(caplog, ctx, cache, zk_storage, processor, create_cert, final_state):
    cert = create_cert(cache, zk_storage, 'FETCHING_CERT_INFO_FROM_STORAGE')
    cert.context['serial_number'] = "1974C54600010024F4F5"
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'secret contains files with inexact name matches' not in log.records_text()

    fields_pb = cert.pb.spec.fields
    assert fields_pb.subject_alternative_names == ['test1.test.yandex-team.ru', 'test2.test.yandex-team.ru']
    assert fields_pb.subject == "Email Address: pki@yandex-team.ru, Common Name: test1.test.yandex-team.ru, " \
                                "Organizational Unit: ITO, Organization: Yandex LLC, Locality: " \
                                "Moscow, State/Province: Russian Federation, Country: RU"
    assert fields_pb.subject_common_name == "test1.test.yandex-team.ru"
    assert fields_pb.issuer == "Common Name: Test-YandexInternal-Ca; Domain Component: ldtestca, local"
    assert fields_pb.issuer_common_name == "Test-YandexInternal-Ca"
    assert fields_pb.serial_number == "120213199433569577268469"
    assert fields_pb.version == "v3"
    assert fields_pb.signature.algorithm_id == "sha1_rsa"
    assert fields_pb.public_key_info.algorithm_id == "rsa"
    assert not fields_pb.signature.parameters
    assert fields_pb.validity.not_before.seconds == 1552395050
    assert fields_pb.validity.not_after.seconds == 1615467050

    assert cert.context['got_cert_secret']

    del cert.context['got_cert_secret']
    processor(cert).process(ctx)  # SWAT-7302: check that repeated runs don't duplicate SANs
    assert cert.pb.spec.fields.subject_alternative_names == ['test1.test.yandex-team.ru', 'test2.test.yandex-team.ru']


@pytest.mark.parametrize('processor,create_cert', [
    (p.FetchingCertInfoFromStorage, create_cert_),
    (rp.FetchingCertInfoFromStorage, create_cert_renewal_),
])
def test_fetching_bad_cert(ctx, cache, zk_storage, processor, create_cert):
    cert = create_cert(cache, zk_storage, 'FETCHING_CERT_INFO_FROM_STORAGE')
    cert.context['serial_number'] = "120213199433569577268469"
    with mock.patch.object(MockYavClient, 'get_version',
                           return_value={'value': {'f': b'abc'}}), pytest.raises(ValueError) as e:
        processor(cert).process(ctx)
    e.match("cert namespace-id/cert-id: secret doesn't contain both public and private cert parts")

    with mock.patch.object(MockYavClient, 'get_version',
                           return_value={'value': {'_certificate': b'abc', '_private_key': b'abc'}}):
        with pytest.raises(ValueError) as e:
            processor(cert).process(ctx)
    e.match('pem_bytes does not appear to contain PEM-encoded data - no BEGIN/END combination found')


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.FetchingCertInfoFromStorage, create_cert_, update_cert_, 'MODIFYING_YAV_SECRET_ACCESS'),
    (rp.FetchingCertInfoFromStorage, create_cert_renewal_, update_cert_renewal_, 'MODIFYING_YAV_SECRET_ACCESS'),
])
def test_fetching_cert_noop(ctx, cache, zk_storage, processor, create_cert, final_state, update_cert):
    cert = create_cert(cache, zk_storage, 'FETCHING_CERT_INFO_FROM_STORAGE')
    cert.pb.order.progress.context['got_cert_secret'] = 'true'
    update_cert(cache, zk_storage, cert, lambda pb: pb.order.progress.context['got_cert_secret'] == 'true')
    assert processor(cert).process(ctx).name == final_state

    fields_pb = cert.pb.spec.fields
    assert not fields_pb.subject_alternative_names
    assert not fields_pb.subject
    assert not fields_pb.subject_common_name
    assert not fields_pb.issuer
    assert not fields_pb.issuer_common_name
    assert not fields_pb.serial_number
    assert not fields_pb.version
    assert not fields_pb.signature.algorithm_id
    assert not fields_pb.signature.parameters
    assert not fields_pb.validity.not_before.seconds
    assert not fields_pb.validity.not_after.seconds


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.ModifyingYavSecretAccess, create_cert_, update_cert_, 'FINISH'),
    (rp.ModifyingYavSecretAccess, create_cert_renewal_, update_cert_renewal_, 'FINISH'),
])
def test_modifying_yav_secret_access(ctx, caplog, cache, zk_storage, processor, create_cert, final_state, update_cert):
    cert = create_cert(cache, zk_storage, 'MODIFYING_YAV_SECRET_ACCESS')
    cert.pb.spec.certificator.abc_service_id = 999
    with mock.patch.object(MockYavClient, 'get_owners',
                           return_value=[{'login': 'user'},
                                         {'abc_id': 999, 'role_slug': 'OWNER', 'abc_scope_id': 85}]):
        with check_log(caplog) as log:
            assert processor(cert).process(ctx).name == final_state
            assert 'Unexpected user with access to certificate secret' in log.records_text()
            assert 'Removed access to cert secret' in log.records_text()
    assert not cert.pb.spec.incomplete


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.ModifyingYavSecretAccess, create_cert_, update_cert_, 'FINISH'),
    (rp.ModifyingYavSecretAccess, create_cert_renewal_, update_cert_renewal_, 'FINISH'),
])
def test_modifying_yav_secret_access_noop(ctx, caplog, cache, zk_storage, processor, create_cert, final_state,
                                          update_cert):
    cert = create_cert(cache, zk_storage, 'MODIFYING_YAV_SECRET_ACCESS')
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Unexpected user with access to certificate secret' not in log.records_text()
        assert 'Removed access to cert secret' not in log.records_text()
    assert not cert.pb.spec.incomplete


def test_finish(ctx, cache, zk_storage):
    cert = create_cert_(cache, zk_storage, 'GOT_CERT_INFO')
    assert p.GotCertInfo(cert).process(ctx).name == 'FINISH'


@pytest.mark.parametrize('processor,create_cert,update_cert,final_state', [
    (p.Cancelling, create_cert_, update_cert_,
     'CANCELLED'),
    (rp.Cancelling, create_cert_renewal_, update_cert_renewal_,
     'CANCELLED'),
])
def test_cancel(ctx, caplog, cache, zk_storage, processor, create_cert, update_cert, final_state):
    cert = create_cert(cache, zk_storage, 'CANCELLING')
    cert.pb.spec.certificator.approval.startrek.issue_id = SECTASK_ID
    update_cert(cache, zk_storage, cert, lambda pb: pb.spec.certificator.approval.startrek.issue_id == SECTASK_ID)
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Adding cancel comment to SECTASK-XXX' in log.records_text()


@pytest.mark.parametrize('processor,create_cert,final_state', [
    (p.Cancelling, create_cert_,
     'CANCELLED'),
    (rp.Cancelling, create_cert_renewal_,
     'CANCELLED'),
])
def test_cancel_noop(ctx, caplog, cache, zk_storage, processor, create_cert, final_state):
    cert = create_cert(cache, zk_storage, 'CANCELLING')
    with check_log(caplog) as log:
        assert processor(cert).process(ctx).name == final_state
        assert 'Adding cancel comment to SECTASK-XXX' not in log.records_text()
