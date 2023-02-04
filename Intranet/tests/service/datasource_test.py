import random
from copy import deepcopy

from staff.groups.service.datasource import _get_service_data


def test_get_service_data_name_en():
    abc_data = {
        'name': {
            'ru': f'name ru {random.random()}',
            'en': f'name en {random.random()}',
        }
    }

    result = _get_service_data(deepcopy(abc_data))

    assert result['name'] == abc_data['name']['ru']
    assert result['name_en'] == abc_data['name']['en']
