"""Tests Bot client."""

import json

import pytest

import walle.clients.utils
import walle.util.misc
from infra.walle.server.tests.lib.util import load_mock_data
from object_validator import Integer, List
from sepelib.core import config
from walle.clients import certificator
from walle.clients.certificator import Certificate
from walle.util.validation import ApiDictScheme

_FAKE_HOST_FQDN = "fake-0000.search.yandex.net"


@pytest.fixture
def certificator_config(mp, certificator_token):
    mp.setitem(config.get_value("certificator"), "host", "certificator.test.yandex-team.ru")
    mp.setitem(config.get_value("certificator"), "params", {"type": "rc-server", "ca_name": "InternalTestCA"})
    mp.setitem(config.get_value("certificator"), "request_args", {"verify": False})
    mp.setitem(config.get_value("certificator"), "access_token", certificator_token)


@pytest.fixture
def mock_certificator(mp):
    return mp.function(walle.clients.utils.request)


@pytest.fixture(scope="module")
def certificator_mock_response():
    return json.loads(load_mock_data("mocks/certificator-response.json"))


@pytest.fixture(scope="module")
def certificator_mock_certificate():
    return load_mock_data("mocks/certificator-certificate-response.txt")


@pytest.mark.slow
@pytest.mark.usefixtures("certificator_config")
class TestCertificatorIntegration:
    """These tests make actual calls to the testing certificator api.
    Run with --certificator-token=XXXXXXXXXXXXXXXXX to execute them.
    """

    def test_api_client(self, certificator_config):
        certificate_list_schema = ApiDictScheme(
            {
                "count": Integer(),
                "results": List(certificator._CERTIFICATE_RESPONSE_SCHEME),
            }
        )

        all_certificates = certificator.json_request(
            certificator._api_url("/certificate/"), scheme=certificate_list_schema
        )

        for certificate in all_certificates["results"]:
            if certificate["status"] != certificator.CERTIFICATE_STATUS_REVOKED:
                revoked = Certificate.from_dict(certificate).revoke()
                assert revoked.revoked

    def test_request_certificate(self, certificator_config):
        certificate = Certificate.request(_FAKE_HOST_FQDN)
        assert certificate.url

        self.__class__.certificate = certificate

    def test_check_certificate(self, certificator_config):
        certificate = self.__class__.certificate
        new_certificate = certificate.refresh_info()

        assert new_certificate.url == certificate.url  # other fields (status) may differ

    def test_get_certificate(self, certificator_config):
        certificate = self.__class__.certificate
        while not certificate.download:
            certificate = certificate.refresh_info()

        pem = certificate.fetch()
        assert pem
        assert pem.startswith("-----BEGIN PRIVATE KEY-----")
        # And there are "END PRIVATE KEY" and "BEGIN CERTIFICATE" markers somewhere in the middle.
        # We don't test validity, we only test this is a certificate and not an html page.
        assert pem.endswith("-----END CERTIFICATE-----")

    def test_request_certificate_revokes_previous_certificate(self, certificator_config):
        certificate = self.__class__.certificate
        Certificate.request(_FAKE_HOST_FQDN)

        certificate = certificate.refresh_info()
        assert certificate.revoked


@pytest.mark.parametrize("status", [certificator.CERTIFICATE_STATUS_ISSUED, certificator.CERTIFICATE_STATUS_REQUESTED])
def test_certificator_issued_valid_certificate(mp, certificator_mock_response, status):
    mp.function(certificator.json_request, return_value=dict(certificator_mock_response, status=status))

    result = Certificate(
        status=status, url=certificator_mock_response["url"], download=certificator_mock_response["download2"] + ".pem"
    )
    assert Certificate.request(_FAKE_HOST_FQDN) == result


def test_certificator_issued_certificate_with_wrong_status(mp, certificator_mock_response):
    mp.function(
        certificator.json_request,
        return_value=dict(certificator_mock_response, status=certificator.CERTIFICATE_STATUS_REVOKED),
    )

    with pytest.raises(certificator.CertificatorPersistentError) as exc:
        Certificate.request(_FAKE_HOST_FQDN)

    assert str(exc.value).startswith(
        "Certificator failed to issue certificate: {}".format(certificator.CERTIFICATE_STATUS_REVOKED)
    )


@pytest.mark.parametrize("status", [certificator.CERTIFICATE_STATUS_ISSUED, certificator.CERTIFICATE_STATUS_REQUESTED])
def test_check_certificate_status(mp, certificator_mock_response, status):
    mp.function(certificator.json_request, return_value=dict(certificator_mock_response, status=status))

    certificate_mock = Certificate("url-mock", "status-mock")
    expected_certificate = Certificate(
        status=status, url=certificator_mock_response["url"], download=certificator_mock_response["download2"] + ".pem"
    )

    assert certificate_mock.refresh_info() == expected_certificate


def test_get_certificate(mp, certificator_mock_certificate):
    mp.function(certificator.raw_request, return_value=certificator_mock_certificate)

    certificate_mock = Certificate("url-mock", "status-mock", "download-link-mock")
    assert certificate_mock.fetch() == certificator_mock_certificate


def test_revoke_certificate(mp, certificator_mock_response):
    mp.function(
        certificator.json_request,
        return_value=dict(certificator_mock_response, status=certificator.CERTIFICATE_STATUS_REVOKED),
    )

    certificate_mock = Certificate("url-mock", "status-mock")
    expected_certificate = Certificate(
        status=certificator.CERTIFICATE_STATUS_REVOKED,
        url=certificator_mock_response["url"],
        download=certificator_mock_response["download2"] + ".pem",
    )

    assert certificate_mock.revoke() == expected_certificate


@pytest.mark.parametrize("status", [certificator.CERTIFICATE_STATUS_ISSUED, certificator.CERTIFICATE_STATUS_REQUESTED])
def test_certificator_failes_to_revoke_certificate(mp, certificator_mock_response, status):
    mp.function(certificator.json_request, return_value=dict(certificator_mock_response, status=status))

    with pytest.raises(certificator.CertificatorPersistentError) as exc:
        certificate_mock = Certificate("url-mock", "status-mock")
        certificate_mock.revoke()

    assert str(exc.value).startswith("Certificator didn't revoke the certificate: {}.".format(status))
