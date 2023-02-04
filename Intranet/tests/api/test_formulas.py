from operator import itemgetter

import pytest

from django.urls import reverse

from intranet.search.tests.helpers import models_helpers as mh
from intranet.search.tests.helpers.api_helpers import dump_model_list


pytestmark = pytest.mark.django_db(transaction=False)


def _dump_formulas(formulas):
    keys = ('id', 'search', 'index', 'service', 'name', 'polynomial', 'compiled', 'additional')
    return dump_model_list(formulas, keys)


def test_get_all_formulas(api_client):
    """
    Возвращаются все формулы
    """
    formulas = [mh.Formula(), mh.Formula()]
    url = reverse('formulas-get')
    r = api_client.get(url)
    assert r.status_code == 200
    assert sorted(r.json(), key=itemgetter('id')) == _dump_formulas(formulas)


@pytest.mark.parametrize('data', [
    {
        # для мета формулы сервис затерся
        'search': 'meta',
        'service': 'intrasearch-conductor',
        'expected_service': '',
    },
    {
        # для не мета формулы - остался без изменений
        'search': 'wiki',
        'service': 'intrasearch-conductor',
        'expected_service': 'intrasearch-conductor',
    },
])
def test_meta_search_formuls(api_client, data):
    """
    У формул метапоиска заменяется название сервиса
    """
    expected_service = data.pop('expected_service')
    mh.Formula(**data)

    url = reverse('formulas-get')
    r = api_client.get(url)
    assert r.json()[0]['service'] == expected_service
