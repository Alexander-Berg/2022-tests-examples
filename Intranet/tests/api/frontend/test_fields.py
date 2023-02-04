import pytest

from intranet.crt.constants import CERT_TYPE
from intranet.crt.core.ca import get_ca

pytestmark = pytest.mark.django_db

# TODO(v-sopov): ОЧЕНЬ внимательно посмотреть на это
EXPECTED = {
        CERT_TYPE.HOST: {
            'fields': ['abc_service', 'desired_ttl_days', 'extended_validation',
                       'hosts', 'request',  'is_ecc', 'common_name'],
            'required': ['abc_service'],  # хосты на фронте вообще обязательные, но...
        },
        CERT_TYPE.PC: {
            'fields': ['desired_ttl_days', 'pc_hostname', 'pc_inum', 'pc_mac',
                       'pc_os', 'pc_serial_number', 'request', 'helpdesk_ticket'],
            'required': ['pc_hostname', 'pc_mac', 'pc_os', 'request'],
        },
        CERT_TYPE.BANK_PC: {
            'fields': ['desired_ttl_days', 'pc_hostname', 'pc_inum', 'pc_mac',
                       'pc_os', 'pc_serial_number', 'request', 'helpdesk_ticket'],
            'required': ['pc_hostname', 'pc_mac', 'pc_os', 'request'],
        },
        CERT_TYPE.ZOMB_PC: {
            'fields': ['desired_ttl_days', 'pc_hostname', 'pc_inum', 'pc_mac',
                       'pc_os', 'pc_serial_number', 'request', 'hardware_request_st_id', 'helpdesk_ticket'],
            'required': ['pc_hostname', 'pc_mac', 'pc_os', 'request', 'hardware_request_st_id'],
        },
        CERT_TYPE.COURTECY_VPN: {
            'fields': ['common_name', 'desired_ttl_days', 'pc_hostname',
                       'pc_inum', 'pc_mac', 'pc_os', 'pc_serial_number', 'request', 'helpdesk_ticket'],
            'required': ['pc_hostname', 'pc_mac', 'pc_os'],
        },
        CERT_TYPE.LINUX_PC: {
            'fields': ['common_name', 'desired_ttl_days', 'pc_hostname',
                       'pc_inum', 'pc_mac', 'pc_os', 'pc_serial_number', 'request', 'helpdesk_ticket'],
            'required': ['pc_hostname', 'pc_mac', 'pc_os'],
        },
        CERT_TYPE.LINUX_TOKEN: {
            'fields': ['common_name', 'desired_ttl_days', 'request', 'helpdesk_ticket'],
            'required': ['request'],
        },
        CERT_TYPE.MOBVPN: {
            'fields': ['desired_ttl_days', 'request', 'helpdesk_ticket'],
            'required': ['request'],
        },
        CERT_TYPE.BOTIK: {
            'fields': ['common_name', 'desired_ttl_days', 'helpdesk_ticket'],
            'required': ['common_name'],
        },
        CERT_TYPE.NINJA: {
            'fields': ['common_name', 'desired_ttl_days'],
            'required': [],
        },
        CERT_TYPE.NINJA_EXCHANGE: {
            'fields': ['common_name', 'desired_ttl_days'],
            'required': [],
        },
        CERT_TYPE.HYPERCUBE: {
            'fields': ['common_name', 'desired_ttl_days'],
            'required': ['common_name', 'desired_ttl_days'],
        },
        CERT_TYPE.ASSESSOR: {
            'fields': ['common_name', 'desired_ttl_days'],
            'required': [],
        },
        CERT_TYPE.RC_SERVER: {
            'fields': ['common_name', 'desired_ttl_days'],
            'required': ['common_name'],
        },
        CERT_TYPE.YC_SERVER: {
            'fields': ['common_name', 'hosts', 'desired_ttl_days'],
            'required': [],
        },
        CERT_TYPE.MDB: {
            'fields': ['common_name', 'hosts', 'desired_ttl_days'],
            'required': [],
        },
        CERT_TYPE.CLIENT_SERVER: {
            'fields': ['desired_ttl_days', 'abc_service', 'request'],
            'required': ['request'],
        },
        CERT_TYPE.BANK_CLIENT_SERVER: {
            'fields': ['desired_ttl_days', 'abc_service', 'request'],
            'required': ['request'],
        },
        CERT_TYPE.SDC: {
            'fields': ['common_name', 'desired_ttl_days'],
            'required': ['common_name'],
        },
        CERT_TYPE.VPN_TOKEN: {
            'fields': ['desired_ttl_days', 'request', 'helpdesk_ticket'],
            'required': ['request'],
        },
        CERT_TYPE.VPN_1D: {
            'fields': ['common_name', 'desired_ttl_days', 'helpdesk_ticket'],
            'required': ['common_name'],
        },
        CERT_TYPE.IMDM: {
            'fields': ['desired_ttl_days', 'pc_serial_number',
                       'request', 'user', 'helpdesk_ticket'],
            'required': ['pc_serial_number', 'request'],
        },
        CERT_TYPE.WIN_PC_AUTO: {
            'fields': ['desired_ttl_days'],
            'required': [],
        },
        CERT_TYPE.POSTAMATE: {
            'fields': ['common_name', 'desired_ttl_days'],
            'required': ['common_name'],
        },
        CERT_TYPE.ZOMBIE: {
            'fields': ['common_name', 'desired_ttl_days', 'request', 'helpdesk_ticket',
                       'pc_inum', 'pc_serial_number', 'pc_hostname', 'pc_mac'],
            'required': [],
        },
        CERT_TYPE.TPM_SMARTCARD_1C: {
            'fields': ['desired_ttl_days', 'request', 'helpdesk_ticket'],
            'required': ['request'],
        },
        CERT_TYPE.TEMP_PC: {
            'fields': ['desired_ttl_days', 'request', 'helpdesk_ticket'],
            'required': ['request'],
        },
}


def test_fields_with_parameters(crt_client, users):
    crt_client.login('helpdesk_user')

    # Без CA
    response = crt_client.json.get('/api/frontend/fields/')
    assert response.status_code == 400
    assert response.json()['detail'] == 'Invalid ca_name parameter'

    # CertumCA в этом окружении недоступен
    response = crt_client.json.get('/api/frontend/fields/', {'ca_name': 'CertumCA'})
    assert response.status_code == 400
    assert response.json()['detail'] == 'Invalid ca_name parameter'

    # Хороший CA, но плохой тип
    response = crt_client.json.get('/api/frontend/fields/', {'ca_name': 'InternalTestCA', 'type': 'eldorado'})
    assert response.status_code == 400
    assert response.json()['detail'] == 'Invalid type parameter'

    # Хорошие параметры
    response = crt_client.json.get('/api/frontend/fields/', {'ca_name': 'InternalTestCA', 'type': 'host'})
    assert response.status_code == 200
    data = response.json()

    expected_fields = EXPECTED[CERT_TYPE.HOST]['fields']
    # CERTOR-851: поле с CSR должно отсутствовать в форме
    try:
        expected_fields.remove('request')
    except ValueError:
        pass

    assert sorted(field['slug'] for field in data) == sorted(expected_fields)


def prepare_response(data):
    fields = []
    required = []
    for field in data:
        fields.append(field['slug'])
        if field['isRequired']:
            required.append(field['slug'])
    return {
        'fields': sorted(fields),
        'required': sorted(required),
    }


@pytest.mark.parametrize('cert_type', get_ca('InternalTestCA').supported_types)
def test_all_types(crt_client, users, cert_type):
    crt_client.login('helpdesk_user')

    response = crt_client.json.get(
        '/api/frontend/fields/',
        {'type': cert_type, 'ca_name': 'InternalTestCA'}
    )
    assert response.status_code == 200
    data = response.json()

    expected = EXPECTED[cert_type]
    expected['fields'].sort()
    expected['required'].sort()

    # CERTOR-851: поле с CSR должно отсутствовать в форме
    try:
        expected['fields'].remove('request')
    except ValueError:
        pass
    try:
        expected['required'].remove('request')
    except ValueError:
        pass

    assert prepare_response(data) == expected
