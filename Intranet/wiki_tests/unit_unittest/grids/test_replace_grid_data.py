import json

from django.conf import settings

from wiki.grids.logic.grid_replace import replace_grid_data
from wiki.grids.models import Grid, Revision
from wiki.grids.utils import dummy_request_for_grids
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.unit_unittest.grids import base

STRUCT = {
    'done': False,
    'width': '100%',
    'sorting': [],
    'title': 'admins in yang',
    'fields': [
        {'type': 'string', 'name': '100', 'title': 'Заказчик', 'required': False},
        {'type': 'string', 'name': '104', 'title': 'Контактное лицо Заказчика', 'required': False},
        {'type': 'string', 'name': '102', 'title': 'Название проекта', 'required': False},
        {'type': 'string', 'name': '103', 'title': 'ID проекта', 'required': False},
        {'type': 'string', 'name': '105', 'title': 'Админ проекта', 'required': False},
    ],
}

NEW_DATA = [
    {
        '100': 'Янг.Заказчик',
        '102': 'Оценка расширений запроса',
        '103': '1007',
        '104': 'alekseimoga@',
        '105': 'sash239@ ',
    },
    {
        '100': 'Янг.Заказчик',
        '102': 'Привлекательность картинки для видео в выдаче (написание обучающих комментариев)',
        '103': '1008',
        '104': 'alekseimoga@',
        '105': 'ivolga15@ ',
    },
    {
        '100': 'Янг.Тестировщик',
        '102': 'Отбор в Тестирование. Этап 1. Тренировочные тесты',
        '103': '1013',
        '104': 'kazah@',
        '105': 'link551@ ',
    },
]


class ReplaceGridData(BaseApiTestCase):
    def setUp(self):
        self.setOrganization()
        self.setUsers()
        self.original = base.create_grid(
            structure=json.dumps(STRUCT),
            tag='grd',
            supertag='grd',
            owner=self.user_chapson,
            title='Оригинал',
            formatter_version='300',
            description='I am real',
            keywords='help',
            data=[
                {
                    '100': 'Янг.Контент',
                    '102': 'Yandex.Вебмастер - Region',
                    '103': '1004',
                    '104': 'adgval@',
                    '105': 'tsymbalava@ ',
                },
                {
                    '100': 'Янг.Контент',
                    '102': 'Yandex.Вебмастер - Name',
                    '103': '1005',
                    '104': 'adgval@',
                    '105': 'tsymbalava@ ',
                },
            ],
        )
        if settings.IS_BUSINESS:
            self.original.org_id = self.org_42.id
            self.original.save()
        Revision.objects.create_from_page(self.original)

    def test_replace(self):
        r = dummy_request_for_grids()
        replace_grid_data(r, self.original, NEW_DATA)

        self.original = Grid.objects.get(pk=self.original.pk)  # refresh from DB не перегрузит из MDB
        assert self.original.revision_set.count() == 2
        assert len(self.original.access_data) == 3

    def test_replace_via_api(self):
        self.client.login('thasonic')
        response = self.client.post('/_api/frontend/grd/.grid/replace', data={'data': NEW_DATA})
        self.assertEqual(response.status_code, 200)

        self.original = Grid.objects.get(pk=self.original.pk)
        assert self.original.revision_set.count() == 2
        assert len(self.original.access_data) == 3
