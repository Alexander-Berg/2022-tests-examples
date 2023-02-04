from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.yql_operation import Worker as YqlOperationWorker


class YqlQueryOperationTestCase(BaseWorkerTestCase):
    worker_class = YqlOperationWorker
    worker_class_file = 'yql_operation'

    def test_yql_operation_parse_url(self):
        group_name = 'id'
        urls_data = (
            ('https://yql.yandex-team.ru/Operations/WAcden4XKgf2wkjo3Qq3v_EvV5zLNtU_ml738GX8Fao=',
             'WAcden4XKgf2wkjo3Qq3v_EvV5zLNtU_ml738GX8Fao=', 'yql.yandex-team.ru'),
            ('https://yql-dev.yandex-team.ru/Operations/WAcden4XKgf2wkjo3Qq3v_EvV5zLNtU_ml738GX8Fao=',
             'WAcden4XKgf2wkjo3Qq3v_EvV5zLNtU_ml738GX8Fao=', 'yql-dev.yandex-team.ru'),
            ('https://yql-test.yandex-team.ru/Operations/WAcden4XKgf2wkjo3Qq3v_EvV5zLNtU_ml738GX8Fao=',
             'WAcden4XKgf2wkjo3Qq3v_EvV5zLNtU_ml738GX8Fao=', 'yql-test.yandex-team.ru'),
        )
        for url, path_match, hostname_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_yql_operation_url_successful(self):
        url = 'https://yql-test.yandex-team.ru/Operations/WAcden4XKgf2wkjo3Qq3v_EvV5zLNtU_ml738GX8Fao='
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='YQL',
                    ),
                    String(value='RUN', color='green'),
                    String(value='js'),
                    User(login='blinkov'),
                ]
            )
        }
        cassette_name = 'yql_operation_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_yql_operation_url_failed(self):
        url = 'https://yql-test.yandex-team.ru/Operations/unknown-id'
        cassette_name = 'yql_operation_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              ))
