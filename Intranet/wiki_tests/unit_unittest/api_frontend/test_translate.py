
import re

from mock import patch
from ujson import loads

import wiki.legacy.translate
from wiki import access as wiki_access
from wiki.utils.supertag import translit
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_STRUCTURE = """
{
  "title" : "Заголовок таблицы",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name1",
      "title" : "Столбец один"
    },
    {
      "name" : "name2",
      "title" : "Столбец два"
    },
    {
      "name" : "name3",
      "title" : "Столбец 3"
    }
  ]
}
"""


def translate_phrase(phrase, direction):
    # специально для test_translate_page_to_ru
    if direction == 'en-ru' and phrase == 'text for translation':
        return 'текст для перевода'

    return translit(phrase)


def translate_chunk(chunk, direction):
    boundary_regex = re.compile(r'(=========(?:<>)+ )')

    return ''.join(
        translate_phrase(w, direction) if i % 2 == 0 else w  # Слова переводим, разд:елители не трогаем
        for i, w in enumerate(boundary_regex.split(chunk))
    )


def translate_mock(chunks, lang, is_retry=False, timeout=None, srv=None):
    """
    Мок для низкоуровневой функции wiki.legacy.translate._multi_query
    которая непосредственно ходит в переводчик. Уровень выше брать хуже, потому что там операции типа эскейпа html.
    """
    return ''.join(translate_chunk(c, lang) for c in chunks)


@patch.object(wiki.legacy.translate, '_multi_query', translate_mock)
class ApiTranslateTest(BaseGridsTest):
    """
    Тесты для АПИ перевода страницы.
    """

    def setUp(self):
        super(ApiTranslateTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_error404(self):
        """
        404
        """
        response = self.client.get('{api_url}/NonExistentPage/.translate/ru-en'.format(api_url=self.api_url))

        json = loads(response.content)
        self.assertTrue('error' in json)
        status_code = response.status_code
        self.assertEqual(status_code, 404)

    def test_access_error(self):
        page = self.create_page(
            tag='СтраницаАнтона', body='page test', authors_to_add=[self.user_chapson], last_author=self.user_chapson
        )
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson)
        response = self.client.get(
            '{api_url}/{page_supertag}/.translate/ru-en'.format(api_url=self.api_url, page_supertag=page.supertag)
        )

        json = loads(response.content)
        self.assertTrue('error' in json)
        status_code = response.status_code
        self.assertEqual(status_code, 403)

    def test_translate_grid(self):
        """
        Перевести табличный список
        """
        supertag = 'grid'

        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name1='русский текст', name2='%%текст внутри форматера%%', name3=''))

        self._add_row(
            supertag,
            dict(name1='**вторая строка**', name2='<# "текст внутри <del>зачеркнутый</del> html" #>', name3=''),
            after_id='last',
        )

        response = self.client.get(
            '{api_url}/{supertag}/.translate/ru-en'.format(api_url=self.api_url, supertag=supertag)
        )

        grid_data = loads(response.content)['data']

        self.assertEqual(200, response.status_code)

        self.assertEqual(grid_data['title'], 'zagolovoktablicy')
        self.assertEqual(grid_data['structure']['title'], 'zagolovoktablicy')

        self.assertTrue('structure' in grid_data)
        fields = grid_data['structure']['fields']
        self.assertEqual(len(fields), 3)
        self.assertEqual(fields[0]['title'], 'stolbecodin')
        self.assertEqual(fields[1]['title'], 'stolbecdva')
        self.assertEqual(fields[2]['title'], 'stolbec3')

        self.assertTrue('rows' in grid_data)
        rows = grid_data['rows']
        self.assertEqual(len(rows), 2)
        self.assertEqual(len(rows[0]), 3)

    def test_get_grid_translation_from_cache(self):
        """
        Получить перевод табличного списка, сохраненного в кэше.
        """
        supertag = 'grid'

        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name1='русский текст', name2='%%текст внутри форматера%%', name3=''))

        request_url = '{api_url}/{supertag}/.translate/ru-en'.format(api_url=self.api_url, supertag=supertag)
        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        # запросить перевод из кеша
        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        grid_data = loads(response.content)['data']

        self.assertEqual(grid_data['title'], 'zagolovoktablicy')
        self.assertEqual(grid_data['structure']['title'], 'zagolovoktablicy')

        self.assertTrue('structure' in grid_data)
        fields = grid_data['structure']['fields']
        self.assertEqual(len(fields), 3)
        self.assertEqual(fields[0]['title'], 'stolbecodin')
        self.assertEqual(fields[1]['title'], 'stolbecdva')
        self.assertEqual(fields[2]['title'], 'stolbec3')

        self.assertTrue('rows' in grid_data)
        rows = grid_data['rows']
        self.assertEqual(len(rows), 1)
        self.assertEqual(len(rows[0]), 3)
