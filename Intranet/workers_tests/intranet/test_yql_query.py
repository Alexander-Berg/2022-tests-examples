from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.yql_query import Worker as YqlQueryWorker


class YqlQueryWorkerTestCase(BaseWorkerTestCase):
    worker_class = YqlQueryWorker
    worker_class_file = 'yql_query'

    def test_yql_query_parse_url(self):
        group_name = 'id'
        urls_data = (
            ('https://yql.yandex-team.ru/Queries/58073240792deae4c056aa10',
             '58073240792deae4c056aa10', 'yql.yandex-team.ru'),
            ('https://yql-dev.yandex-team.ru/Queries/58073240792deae4c056aa10',
             '58073240792deae4c056aa10', 'yql-dev.yandex-team.ru'),
            ('https://yql-test.yandex-team.ru/Queries/58073240792deae4c056aa10',
             '58073240792deae4c056aa10', 'yql-test.yandex-team.ru'),
        )
        for url, path_match, hostname_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_yql_query_url_successful(self):
        url = 'https://yql-test.yandex-team.ru/Queries/58073240792deae4c056aa10'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='YQL',
                    ),
                    String(value='js'),
                    User(login='blinkov'),
                ]
            )
        }
        cassette_name = 'yql_query_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_yql_query_url_failed(self):
        url = 'https://yql-test.yandex-team.ru/Queries/123456789012345678901234'
        cassette_name = 'yql_query_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              ))
