from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.yql_tutorial import Worker as YqlTutorialWorker


class YqlTutorialWorkerTestCase(BaseWorkerTestCase):
    worker_class = YqlTutorialWorker
    worker_class_file = 'yql_tutorial'

    def test_yql_tutorial_parse_url(self):
        group_name = 'name'
        urls_data = (
            ('https://yql.yandex-team.ru/Tutorial/01_Select_all_columns',
             '01_Select_all_columns', 'yql.yandex-team.ru'),
            ('https://yql-dev.yandex-team.ru/Tutorial/01_Select_all_columns',
             '01_Select_all_columns', 'yql-dev.yandex-team.ru'),
            ('https://yql-test.yandex-team.ru/Tutorial/01_Select_all_columns',
             '01_Select_all_columns', 'yql-test.yandex-team.ru'),
        )
        for url, path_match, hostname_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_yql_tutorial_url_successful(self):
        url = 'https://yql-test.yandex-team.ru/Tutorial/01_Select_all_columns'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='YQL',
                    ),
                    String(value='01. Select all columns'),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))
