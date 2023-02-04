import pytest

from unittest.mock import patch, Mock

from intranet.femida.src.staff.choices import GEOGRAPHY_KINDS
from intranet.femida.src.staff.models import Geography
from intranet.femida.src.staff.tasks import sync_geographies

from intranet.femida.tests import factories as f


INTERNATIONAL_GEOGRAPHY_ID = 16008
CIS_GEOGRAPHY_ID = 666


@pytest.fixture
def geographies_data():
    return [
        {
            'ancestors': [],
            "is_deleted": False,
            "name": {
                "full": {
                    "ru": "СНГ",
                    "en": "CIS",
                },
                "short": {
                    "ru": "",
                    "en": "",
                },
            },
            "url": "geo_cis",
            "oebs_code": None,
            "st_translation_id": None,
            "id": CIS_GEOGRAPHY_ID,
        },
        {
            'ancestors': [
                {
                    'id': CIS_GEOGRAPHY_ID,
                },
            ],
            'is_deleted': False,
            'name': {
                'full': {
                    'ru': 'Беларусь',
                    'en': 'Belarus',
                },
                'short': {
                    'ru': '',
                    'en': '',
                },
            },
            'url': 'geo_belc',
            'oebs_code': 'BELc',
            'st_translation_id': 581,
            'id': 1,
        },
        {
            'ancestors': [
                {
                    'id': INTERNATIONAL_GEOGRAPHY_ID,
                },
            ],
            'is_deleted': False,
            'name': {
                'full': {
                    'ru': 'Египет',
                    'en': 'Egypt',
                },
                'short': {
                    'ru': '',
                    'en': '',
                },
            },
            'url': 'geo_egy',
            'oebs_code': 'EGY',
            'st_translation_id': 551,
            'id': 2,
        },
        {
            'ancestors': [],
            'is_deleted': False,
            'name': {
                'full': {
                    'ru': 'Весь мир',
                    'en': '',
                },
                'short': {
                    'ru': '',
                    'en': '',
                },
            },
            'url': 'geo_wor',
            'oebs_code': 'WOR',
            'st_translation_id': 59951,
            'id': INTERNATIONAL_GEOGRAPHY_ID,
        },
    ]


def test_geographies_sync(geographies_data):
    f.GeographyFactory(id=1)

    repo = Mock()
    repo.getiter.return_value = geographies_data
    func_path = 'intranet.femida.src.staff.sync.registry.get_repository'
    with patch('intranet.femida.src.staff.sync.get_service_ticket'):
        with patch(func_path, Mock(return_value=repo)):
            sync_geographies()

    geography_data_by_id = {data['id']: data for data in geographies_data}
    geographies = (
        Geography.objects
        .values(
            'id',
            'url',
            'name_ru',
            'name_en',
            'oebs_code',
            'startrek_id',
            'is_deleted',
            'ancestors',
            'kind',
        )
    )

    for geography in geographies:
        geography_data = geography_data_by_id[geography['id']]
        ancestors = [ancestor['id'] for ancestor in geography_data['ancestors']]
        name = geography_data['name']['full']

        if CIS_GEOGRAPHY_ID in ancestors or geography_data['id'] == CIS_GEOGRAPHY_ID:
            kind = GEOGRAPHY_KINDS.rus
        else:
            kind = GEOGRAPHY_KINDS.international

        assert geography['id'] == geography_data['id']
        assert geography['url'] == geography_data['url']
        assert geography['name_ru'] == name['ru']
        assert geography['name_en'] == name['en'] or name['ru']
        assert geography['oebs_code'] == geography_data['oebs_code']
        assert geography['startrek_id'] == geography_data['st_translation_id']
        assert geography['is_deleted'] == geography_data['is_deleted']
        assert geography['ancestors'] == ancestors
        assert geography['kind'] == kind
