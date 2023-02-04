from unittest import mock

import pytest

from intranet.wiki.tests.wiki_tests.common import grid_helper
from intranet.wiki.tests.wiki_tests.common.data_helper import read_test_asset
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from wiki.grids.logic import grids_import
from wiki.legacy import json
from wiki.pages.models import Revision


GRID_STRUCTURE = {
    'done': False,
    'fields': [
        {'name': '100', 'title': 'Метод отгрузки', 'required': False, 'type': 'string', 'sorting': True},
        {'name': '101', 'title': 'Номер рейса', 'required': False, 'type': 'string', 'sorting': True},
        {'name': '102', 'title': 'Склад отгрузки', 'required': False, 'type': 'string', 'width': '7%', 'sorting': True},
        {'name': '103', 'title': 'Склад отгрузки id', 'required': False, 'type': 'string', 'sorting': True},
        {
            'name': '104',
            'title': 'Время отгрузки (часовой пояс склада отгрузки)',
            'required': False,
            'type': 'string',
            'width': '5%',
            'sorting': True,
        },
        {'name': '113', 'title': 'График забора', 'required': False, 'type': 'string', 'width': '7%', 'sorting': True},
        {'name': '114', 'title': 'День забора', 'required': False, 'type': 'string', 'sorting': True},
        {'name': '105', 'title': 'Транзитное время (ч.)', 'required': False, 'type': 'string', 'sorting': True},
        {'name': '106', 'title': 'Склад рзгрузки', 'required': False, 'type': 'string', 'width': '7%', 'sorting': True},
        {'name': '107', 'title': 'Склад разгрузки id', 'required': False, 'type': 'string', 'sorting': True},
        {
            'name': '108',
            'title': 'Разница в часовых поясах со складом разгрузки',
            'required': False,
            'type': 'string',
            'sorting': True,
        },
        {'name': '109', 'title': 'Время прибытия (местное)', 'required': False, 'type': 'string', 'sorting': True},
        {'name': '115', 'title': 'День приезда', 'required': False, 'type': 'string', 'sorting': True},
        {
            'name': '111',
            'title': 'Объем магистрали',
            'required': False,
            'type': 'string',
            'width': '3%',
            'sorting': True,
        },
        {'name': '112', 'title': 'Комментарий', 'required': False, 'type': 'string', 'sorting': True},
        {'type': 'string', 'name': '112', 'title': 'столбец 11', 'required': False, 'sorting': True},
    ],
    'title': 'Возвратный поток',
    'width': '100%',
    'sorting': [],
}


@only_intranet
@pytest.mark.django_db
def test_hangs_csv(client, api_url, wiki_users):
    content = read_test_asset('megacsv.csv').decode()
    client.login(wiki_users.thasonic)
    grid = grid_helper.create_grid(client, api_url, 'root/abcd', json.dumps(GRID_STRUCTURE), wiki_users.thasonic)
    grid.refresh_from_db()
    request = mock.MagicMock()
    request.user = wiki_users.thasonic
    request.user_auth = None
    COLS = ['100', '101', '102', '103', '104', '113', '114', '105', '106', '107', '108', '109', '115', '111', '112']
    x = {f'icolumn_{i}_to': tgt for i, tgt in enumerate(COLS)}
    x.update(
        {
            'icolumn_15_enabled': False,
            'charset': 'utf-8',
            'delimiter': ';',
            'quotechar': '"',
            'omit_first': False,
        }
    )

    with mock.patch('wiki.grids.logic.grids_import._get_contents_from_cache') as a, mock.patch(
        'wiki.grids.logic.grids_import._get_grid'
    ) as b, mock.patch('wiki.grids.logic.grids_import._prepare_grid') as c, mock.patch(
        'wiki.grids.logic.grids_import._get_http_params_value'
    ) as e, mock.patch(
        'wiki.grids.logic.grids_import._get_http_bool_param_value'
    ) as f:
        a.return_value = ('megacsv.csv', content)
        b.return_value = grid
        c.return_value = grid
        f.side_effect = lambda _, name, default_value=None: x.get(name, default_value)
        e.side_effect = lambda _, name: x.get(name)

        grids_import.validate_import_data(request, 'cache_key')
        grids_import.save_import_data(request, 'cache_key')

        Revision.objects.get(page=grid, mds_storage_id=grid.mds_storage_id)
