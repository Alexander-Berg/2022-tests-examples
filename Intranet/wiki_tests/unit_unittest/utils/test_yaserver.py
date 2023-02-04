from datetime import datetime

from mock import patch
from pretend import stub

from wiki.utils.yandex_server_context import get_editors_of_page_for_yaserver
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class GetEditorsOfPageForYaserverTestCase(BaseApiTestCase):
    def test_page(self):
        page = stub()
        chapson_edited_at_1 = datetime(2014, 12, 22, 13, 50, 1)
        chapson_edited_at_2 = datetime(2010, 12, 22, 13, 50, 1)
        asm_edited_at = datetime(2013, 12, 22, 13, 50, 1)
        with patch(
            'wiki.utils.yandex_server_context.get_editors_from_db',
            lambda page: [
                ('chapson', chapson_edited_at_1),
                ('chapson', chapson_edited_at_2),
                ('asm', asm_edited_at),
            ],
        ):
            result = get_editors_of_page_for_yaserver(page)

        result = sorted(result, key=lambda x: x.login)
        self.assertEqual('asm', result[0].login)
        self.assertEqual(1, result[0].edited_times)
        self.assertEqual(1387720201, result[0].last_edition_date)
        self.assertEqual('chapson', result[1].login)
        self.assertEqual(2, result[1].edited_times)
        self.assertEqual(1419256201, result[1].last_edition_date)
