import pytest
from marshmallow import ValidationError

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.root_public_key import RootPublicKey
from billing.yandex_pay.yandex_pay.file_storage.root_public_key import RootPublicKeyStorage


def test_for_development():
    storage = RootPublicKeyStorage.for_development()

    assert_that(
        storage.get_keys(),
        equal_to([])
    )


def test_parse():
    data = {
        'keys': [
            {
                'keyExpiration': '123',
                'keyValue': 'keykey',
                'protocolVersion': '1234',
            }
        ]
    }

    actual = RootPublicKeyStorage._parse_resource_from_dict(data)

    expected = {
        'keys': [RootPublicKey(key_value='keykey', key_expiration='123', protocol_version='1234')],
    }
    assert_that(expected, equal_to(actual))


@pytest.mark.parametrize('data', (
    pytest.param({'keys': [{'protocolVersion': '123', 'keyExpiration': '123'}]}, id='missing-key-value'),
    pytest.param({'keys': [{'keyValue': '123', 'keyExpiration': '123'}]}, id='missing-protocol-version'),
    pytest.param({'keys': [{'keyValue': '123', 'protocolVersion': '123'}]}, id='missing-key-exp'),
    pytest.param({'keys': {}}, id='invalid "keys" type'),
    pytest.param({}, id='no-keys'),
))
def test_raises_on_malformed_input(data):
    with pytest.raises(ValidationError):
        RootPublicKeyStorage._parse_resource_from_dict(data)


def test_get_keys(mocker):
    rbks = RootPublicKeyStorage(keys=[1, 2, 3])
    assert rbks.get_keys() == [1, 2, 3]
