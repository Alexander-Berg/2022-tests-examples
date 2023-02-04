from http.client import HTTPException
from ssl import SSLError
from urllib.error import URLError

import pytest
from zeep import exceptions
from mock import mock

from intranet.crt.core.ca.certum import CertumZeepClient
from intranet.crt.core.ca.exceptions import CaError, RetryCaException


@pytest.mark.parametrize(
    'exception, reraised_exception',
    [
        (exceptions.Fault('Unauthorized'), RetryCaException),
        (exceptions.Fault('Some other fault'), CaError),
        (exceptions.TransportError('Server returned response (500) with invalid XML'), RetryCaException),
        (exceptions.TransportError('Some other transport error'), CaError),
        (exceptions.Error('Stub error'), CaError),
        (HTTPException('Stub error'), RetryCaException),
        (URLError('Stub error'), RetryCaException),
        (SSLError('Stub error'), RetryCaException),
        (Exception('Bad Gateway'), RetryCaException),
        (Exception('Stub error'), CaError),
        (exceptions.Fault('Connection timed out'), RetryCaException),
    ]
)
@mock.patch('intranet.crt.core.ca.zeep_client.Transport', mock.Mock())
@mock.patch('intranet.crt.core.ca.zeep_client.SqliteCache', mock.Mock())
@mock.patch('intranet.crt.core.ca.zeep_client.Client')
def test_zeep_client_exception_handling(mocked_client, exception, reraised_exception):
    zeep_client = CertumZeepClient.create('https://certum.url', username='a', password='b')
    zeep_client.client = mocked_client
    mocked_client.service.quickOrder.side_effect = exception

    with pytest.raises(reraised_exception):
        zeep_client.call('quickOrder')
