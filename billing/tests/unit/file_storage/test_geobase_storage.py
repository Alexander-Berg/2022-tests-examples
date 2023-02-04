import pytest
from marshmallow import ValidationError

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.file_storage.geobase import GeobaseStorage


def test_for_development(yandex_pay_settings):
    yandex_pay_settings.FILESTORAGE_GEOBASE_DEVELOPMENT_FORBIDDEN_REGIONS = [{'region_id': 'what'}]

    storage = GeobaseStorage.for_development()

    assert storage.is_forbidden_region('what')


def test_parse():
    data = {
        'forbidden_regions': [
            {'Id': '1'},
            {'Id': 2},
        ],
    }

    result = GeobaseStorage._parse_resource_from_dict(data)

    assert_that(
        result,
        equal_to({
            'forbidden_regions': [{'region_id': 1}, {'region_id': 2}],
        }),
    )


@pytest.mark.parametrize('data', (
    pytest.param({'forbidden_regions': [{'iD': 1}]}, id='wrong key'),
    pytest.param({'forbidden_regions': [{'Id': 'string'}]}, id='wrong type'),
    pytest.param({'forbidden_regions': []}, id='empty array'),
    pytest.param({}, id='empty dict'),
))
def test_raises_on_malformed_input(data):
    with pytest.raises(ValidationError):
        GeobaseStorage._parse_resource_from_dict(data)


def test_region_is_forbidden(mocker):
    storage = GeobaseStorage(forbidden_regions=[{'region_id': 5}])

    assert storage.is_forbidden_region(5)


def test_region_is_not_forbidden(mocker):
    storage = GeobaseStorage(forbidden_regions=[{'region_id': 5}])

    assert not storage.is_forbidden_region(6)
