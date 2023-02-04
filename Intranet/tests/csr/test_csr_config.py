import pytest
from intranet.crt.utils.ssl import PemCertificateRequest
from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes

from intranet.crt.csr.base_config import BaseCsrConfig
from intranet.crt.csr.fields import (
    EmailSubjectField,
    UnitSubjectField,
    BasicConstraintsExtensionField,
    StateSubjectField,
    ExtendedKeyUsageExtensionField,
    CommonNameSubjectField,
    LocalitySubjectField,
    SansExtensionField,
    CountrySubjectField,
)

pytestmark = pytest.mark.django_db


class BaseCsrTestConfig(BaseCsrConfig):
    SUBJECT_ORDER = ['unit', 'email']
    EXTENSION_ORDER = ['basic']

    email = EmailSubjectField(required=True)
    unit = UnitSubjectField(value='Infra')

    basic = BasicConstraintsExtensionField()

    excess1 = StateSubjectField(value='Moscow')
    excess2 = ExtendedKeyUsageExtensionField()


class CsrTestConfig(BaseCsrTestConfig):
    SUBJECT_ORDER = ['unit', 'common_name', 'city', 'email']
    EXTENSION_ORDER = ['sans', 'basic']

    common_name = CommonNameSubjectField(required=True)
    city = LocalitySubjectField(value='Moscow')

    sans = SansExtensionField(required=True)

    excess3 = CountrySubjectField(value='ru')
    excess4 = ExtendedKeyUsageExtensionField()


def test_invalid_csr_config_class():
    with pytest.raises(RuntimeError, match=r'.*(test_field).*'):
        class BrokenTestCsrConfig(BaseCsrConfig):
            SUBJECT_ORDER = ['test_field']


def test_invalid_csr_config_class2():
    with pytest.raises(RuntimeError, match=r'.*(test_field).*'):
        class BrokenTestCsrConfig(BaseCsrConfig):
            EXTENSION_ORDER = ['test_field']


def test_invalid_csr_config_class3():
    with pytest.raises(RuntimeError, match=r'.*(test_field1, test_field2).*'):
        class BrokenTestCsrConfig(BaseCsrConfig):
            SUBJECT_ORDER = ['test_field1']
            EXTENSION_ORDER = ['test_field2']


def test_csr_config_class():
    subject_fields = ['unit', 'common_name', 'city', 'email']
    extension_fields = ['sans', 'basic']
    required_fields = {'common_name', 'email', 'sans'}

    email = 'asd@ya.ru'
    common_name = 'common'
    sans = ['ya.ru', 'ya.com']

    assert not set(subject_fields) - CsrTestConfig.subject_fields.keys()
    assert not set(extension_fields) - CsrTestConfig.extension_fields.keys()
    assert required_fields == set(CsrTestConfig.required_fields)

    with pytest.raises(RuntimeError, match=r'.*(common_name, sans).*'):
        CsrTestConfig(email=email)

    config = CsrTestConfig(
        common_name=common_name,
        email=email,
        sans=sans,
        unit='Miss',
    )

    assert config.common_name.value == common_name
    assert config.email.value == email
    assert config.sans.sans == sans
    assert config.unit.value == 'Infra'

    config2 = CsrTestConfig(
        common_name=common_name + '2',
        email=email + '2',
        sans=sans + ['ya.net'],
        unit='Miss',
    )

    assert config.common_name.value == common_name
    assert config.email.value == email
    assert config.sans.sans == sans
    assert config.unit.value == 'Infra'

    pem_csr = config.get_csr()

    csr = x509.load_pem_x509_csr(pem_csr, default_backend())

    oid_list = [config.unit, config.common_name, config.city, config.email]
    for i, subject_name in enumerate(csr.subject):
        config_field = oid_list[i]
        assert subject_name.oid == config_field.oid
        assert subject_name.value == config_field.value

    for i, extension in enumerate(csr.extensions):
        if i == 0:
            for j, san in enumerate(extension.value._general_names._general_names):
                assert san.value == config.sans.sans[j]
        elif i == 1:
            assert extension.value.ca == config.basic.ca

def test_csr_config_is_ecc():
    email = 'asd@ya.ru'
    csr = BaseCsrTestConfig(email=email).get_csr(is_ecc=True)
    assert PemCertificateRequest(csr).is_ecc
