import pytest
from django.core.management import call_command
from django.utils.encoding import force_text
from mock import mock
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS, HOST_VALIDATION_CODE_STATUS
from intranet.crt.core.models import HostToApprove, Certificate
from __tests__.utils.common import (
    attrdict, approve_cert, FakeGlobalsignClient, patch_globalsign_client, patch_get_host_txt_codes,
)


@pytest.mark.parametrize('known_domain_id', [True, False])
@pytest.mark.parametrize('domain_has_already_added', [True, False])
def test_globalsign_full_workflow(crt_client, users, known_domain_id, domain_has_already_added):
    FakeGlobalsignClient.ALREADY_ADDED_TO_PROFILE_ERROR_FLAG = domain_has_already_added
    domain = 'example.com'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.GLOBALSIGN_TEST_CA,
        'hosts': domain,
    }

    HostToApprove.objects.all().delete()
    # чтобы обойти кастомный save
    HostToApprove.objects.bulk_create([
        HostToApprove(
            auto_managed=True,
            managed_dns=True,
            host=domain,
            globalsign_domain_id=None,
        )
    ])

    globalsign_user = users['globalsign_user']
    crt_client.login(globalsign_user.username)

    with mock.patch('intranet.crt.api.base.serializer_mixins.need_approve.create_st_issue_for_cert_approval') as mocked:
        mocked.return_value = attrdict({'key': 'SECTASK-666'})
        response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get()
    assert cert.status == CERT_STATUS.NEED_APPROVE

    # подтвердим запрос
    approve_cert(crt_client)
    cert.refresh_from_db()
    assert cert.status == CERT_STATUS.REQUESTED

    # NOTE(rocco66): let's add domain to globalsign profile and get domain_id and data for DNS TXT

    with patch_globalsign_client() as mocked_globalsign_client:
        mocked_globalsign_client.domain = domain
        with mock.patch('intranet.crt.core.models.get_name_servers') as get_name_servers:
            get_name_servers.return_value = {'ns1.yandex.ru'}
            with mock.patch('intranet.crt.core.models.name_servers_are_managed') as name_servers_are_managed:
                name_servers_are_managed.return_value = True
                call_command('issue_certificates')
                if domain_has_already_added:
                    # NOTE(rocco66): everything should be fine next time
                    FakeGlobalsignClient.ALREADY_ADDED_TO_PROFILE_ERROR_FLAG = False
                    call_command('issue_certificates')

    cert.refresh_from_db()
    assert cert.status == CERT_STATUS.VALIDATION

    hta = HostToApprove.objects.get()
    assert hta.certificates.count() == 1
    assert hta.certificates.get() == cert
    assert hta.globalsign_domain_id == FakeGlobalsignClient.MSSL_DOMAIN_ID
    validation_code = hta.validation_codes.get(status=HOST_VALIDATION_CODE_STATUS.validation)
    assert validation_code.code == FakeGlobalsignClient.DNS_TEXT_DATA

    response = crt_client.json.get('/api/hosts-to-approve.xml')
    assert FakeGlobalsignClient.DNS_TEXT_DATA in force_text(response.content)

    with patch_globalsign_client():
        with patch_get_host_txt_codes(validation_code):
            call_command('check_txt_records')
    validation_code.refresh_from_db()
    assert validation_code.status == HOST_VALIDATION_CODE_STATUS.validated

    with patch_globalsign_client():
        cert.controller.ca._issue(cert)  # NOTE(rocco66): _issue_certificate
        issue_result = cert.controller.ca._issue(cert)  # NOTE(rocco66): _fetch_certificate

    assert issue_result == FakeGlobalsignClient.CERT_BODY
