import pytest

from unittest.mock import patch, Mock

from intranet.femida.src.staff.models import ValueStream
from intranet.femida.src.staff.tasks import sync_value_streams

from intranet.femida.tests import factories as f


@pytest.fixture
def value_streams_data():
    return [
        {
            'id': 1,
            'slug': 'slug1',
            'name': 'сервис1',
            'name_en': 'service1',
            'is_active': True,
            'oebs_product_id': 2,
            'st_translation_id': 5,
        },
        {
            'id': 2,
            'slug': 'slug2',
            'name': 'сервис2',
            'name_en': 'service2',
            'is_active': True,
            'oebs_product_id': 3,
            'st_translation_id': 7,
        },
    ]


def test_value_streams_sync(value_streams_data):
    f.ValueStreamFactory(staff_id=1)

    func_path = 'intranet.femida.src.staff.sync.get_value_streams'
    with patch(func_path, Mock(return_value=value_streams_data)):
        sync_value_streams()

    vs_data_by_id = {data['id']: data for data in value_streams_data}
    value_streams = (
        ValueStream.objects
        .values(
            'staff_id',
            'slug',
            'name_ru',
            'name_en',
            'is_active',
            'oebs_product_id',
            'startrek_id',
        )
    )

    for value_stream in value_streams:
        value_stream_data = vs_data_by_id[value_stream['staff_id']]
        value_stream_data['staff_id'] = value_stream_data.pop('id')
        value_stream_data['name_ru'] = value_stream_data.pop('name')
        value_stream_data['startrek_id'] = value_stream_data.pop('st_translation_id')
        assert value_stream == value_stream_data
