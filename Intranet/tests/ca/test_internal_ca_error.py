import mock
import pytest
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS
from intranet.crt.core.models import Certificate
from intranet.crt.core.ca.exceptions import CaError, RetryCaException
from __tests__.utils.common import capture_raw_http

pytestmark = pytest.mark.django_db


@pytest.fixture
def request_data():
    return {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.INTERNAL_TEST_CA,
        'hosts': 'hostname.yandex-team.com'
    }


# CERTOR-1965
def test_saving_certificate_on_ca_retry(request_data, crt_client, users):
    def retry_on_certificate_request(*args, **kwargs):
        raise RetryCaException('Test retry')

    crt_client.login(users['helpdesk_user'].username)

    with capture_raw_http(side_effect=retry_on_certificate_request):
        response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get()
    assert cert.status == CERT_STATUS.REQUESTED


def test_saving_certificate_on_ca_internal_error(request_data, crt_client, users):
    crt_client.login(users['helpdesk_user'].username)

    with capture_raw_http(answer='error', status_code=status.HTTP_500_INTERNAL_SERVER_ERROR):
        response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR

    cert = Certificate.objects.get()
    assert cert.status == CERT_STATUS.ERROR
    assert cert.error_message == 'Internal CA returned wrong status code: 500, answer: error'


def test_logging_on_ca_error(request_data, crt_client, users):
    crt_client.login(users['helpdesk_user'].username)
    request_data['ca_name'] = CA_NAME.INTERNAL_TEST_CA

    sample_ca_error_response = """<HTML>
<Head>
	<Title>Some service</Title>
</Head>
<Body>
    Some CA error.
</Body>
</HTML>""".encode()
    sample_answer = 'Some service Some CA error.'
    sample_timestamp = 'Mon Dec 12 12:12:12 1212'

    mocked_ca_error = mock.Mock(return_value=CaError())

    with (
        mock.patch('intranet.crt.core.ca.exceptions.CaError.__new__', mocked_ca_error),
        mock.patch('time.asctime', mock.Mock(return_value=sample_timestamp)),
        capture_raw_http(answer=sample_ca_error_response, status_code=status.HTTP_200_OK),
    ):
        crt_client.json.post('/api/certificate/', data=request_data)
    mocked_ca_error.assert_called_once()
    assert (
        mocked_ca_error.call_args.args[1] ==
        f'Invalid answer from Internal CA at {sample_timestamp}. Answer is: {sample_answer}'
    )
