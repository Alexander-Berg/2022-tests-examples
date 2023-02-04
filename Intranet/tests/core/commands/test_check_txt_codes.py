import mock
import pytest
from django.core.management import call_command

from intranet.crt.core.ca.globalsign import GlobalSignProductionCA
from intranet.crt.constants import HOST_VALIDATION_CODE_STATUS, CA_NAME
from __tests__.utils import factories as f
from __tests__.utils.common import patch_globalsign_client, patch_get_host_txt_codes


@pytest.yield_fixture(autouse=True)
def patch_external_methods():
    with mock.patch('intranet.crt.core.models.get_name_servers', return_value=set()):
        with mock.patch('intranet.crt.core.ca.certum.CertumProductionCA.verify_domain'):
            yield


def _patch_certum_get_verification_state_by_hosts(validation_code, state, error_code):
    function = 'intranet.crt.core.ca.certum.CertumProductionCA._get_verification_state_by_hosts'
    return_value = {
        validation_code.host.host: {
            'state': state,
            'error_code': error_code,
        }
    }
    return mock.patch(function, return_value=return_value)


@pytest.mark.parametrize(
    'certum_code_state, certum_error_code, expected_status, expected_history_exists',
    [
        ('VERIFIED', None, HOST_VALIDATION_CODE_STATUS.validated, True),
        ('FAILED', 'ALREADY_VERIFIED', HOST_VALIDATION_CODE_STATUS.validated, True),
        ('FAILED', 'LINK_EXPIRED', HOST_VALIDATION_CODE_STATUS.error, False),
        ('FAILED', 'OTHER_ERROR', HOST_VALIDATION_CODE_STATUS.error, False),
        ('FAILED', 'FILE_INVALID_CONTENT', HOST_VALIDATION_CODE_STATUS.error, False),
        ('FAILED', 'FILE_CONNECTION_ERROR', HOST_VALIDATION_CODE_STATUS.error, False),
        ('FAILED', 'FILE_HTTP_ERROR', HOST_VALIDATION_CODE_STATUS.error, False),
        ('FAILED', 'DNS_NO_RECORDS', HOST_VALIDATION_CODE_STATUS.validation, False),
        ('FAILED', 'DNS_NO_PROPER_RECORDS', HOST_VALIDATION_CODE_STATUS.validation, False),
    ],
)
def test_certum_code_validated(certum_code_state, certum_error_code, expected_status, expected_history_exists):
    validation_code = f.HostValidationCode(status=HOST_VALIDATION_CODE_STATUS.validation)
    validation_code.host.certificates.add(validation_code.certificate)

    with _patch_certum_get_verification_state_by_hosts(validation_code, certum_code_state, certum_error_code):
        with patch_get_host_txt_codes(validation_code):
            call_command('check_txt_records')

    validation_code.refresh_from_db()
    assert validation_code.status == expected_status

    history = validation_code.host.history.filter(
        validation_code=validation_code.code,
        action='validated',
    )
    assert history.exists() == expected_history_exists


@pytest.mark.parametrize('success_globalsign_verification_request', [True, False])
@pytest.mark.parametrize('domain_status', [
    GlobalSignProductionCA.DOMAIN_STATUS_VETTING,
    GlobalSignProductionCA.DOMAIN_STATUS_AVAILABLE,
])
def test_globalsign_validation(success_globalsign_verification_request, domain_status):
    validation_code = f.HostValidationCode(status=HOST_VALIDATION_CODE_STATUS.validation)
    validation_code.code = '_globalsign-domain-verification=Zbe9nAtEzUXNCpgmo_yw8N_A3PM763kFPAAf11fVGG'
    validation_code.save()

    validation_code.certificate.ca_name = CA_NAME.GLOBALSIGN_TEST_CA
    validation_code.certificate.save()
    validation_code.host.certificates.add(validation_code.certificate)

    with patch_globalsign_client() as mocked_globalsign_client:
        mocked_globalsign_client.VERIFY_DOMAIN_ERROR_FLAG = not success_globalsign_verification_request
        mocked_globalsign_client.VERIFY_DOMAIN_STATUS = domain_status
        with patch_get_host_txt_codes(validation_code):
            call_command('check_txt_records')

    validation_code.refresh_from_db()
    history = validation_code.host.history.filter(
        validation_code=validation_code.code,
        action='validated',
    )
    if domain_status == GlobalSignProductionCA.DOMAIN_STATUS_AVAILABLE:
        assert validation_code.status == HOST_VALIDATION_CODE_STATUS.validated
        assert history.exists()
    else:
        assert validation_code.status == HOST_VALIDATION_CODE_STATUS.validation
        assert not history.exists()

