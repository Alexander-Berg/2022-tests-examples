import pytest
from marshmallow import ValidationError

from hamcrest import assert_that, contains, equal_to

from billing.yandex_pay.yandex_pay.file_storage.csrf_anti_forgery_key import CSRFAntiForgeryKeyStorage


def test_for_development(yandex_pay_settings):
    yandex_pay_settings.FILESTORAGE_CSRF_ANTI_FORGERY_DEVELOPMENT_KEY = 'unittest'

    storage = CSRFAntiForgeryKeyStorage.for_development()

    assert_that(
        storage.get_keys(),
        equal_to(['unittest'])
    )


def test_parse():
    data = {
        'key': 'keykey',
        'old_key': 'oldkeykey',
    }

    actual = CSRFAntiForgeryKeyStorage._parse_resource_from_dict(data)

    expected = {
        'old_key': 'oldkeykey',
        'key': 'keykey'
    }
    assert_that(expected, equal_to(actual))


@pytest.mark.parametrize('data', (
    pytest.param({'old_key': {}, 'key': '123'}, id='invalid "old_key" type'),
    pytest.param({'key': {}, 'old_key': '123'}, id='invalid "key" type'),
    pytest.param({'key': '', 'old_key': '123'}, id='"key" is too short'),
    pytest.param({'key': '123', 'old_key': ''}, id='"old_key" is too short'),
    pytest.param({}, id='no keys at all'),
))
def test_raises_on_malformed_input(data):
    with pytest.raises(ValidationError):
        CSRFAntiForgeryKeyStorage._parse_resource_from_dict(data)


def test_get_keys(mocker):
    rbks = CSRFAntiForgeryKeyStorage(key='a', old_key='b')
    assert_that(rbks.get_keys(), contains('a', 'b'))


def test_get_actual_key(mocker):
    rbks = CSRFAntiForgeryKeyStorage(
        old_key='old-key',
        key='key',
    )
    assert_that(
        rbks.get_actual_key(),
        equal_to('key'),
    )
