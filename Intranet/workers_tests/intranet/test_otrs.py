from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.otrs import Worker as OtrsWorker


class OtrsWorkerTestCase(BaseWorkerTestCase):
    worker_class = OtrsWorker
    worker_class_file = 'otrs'

    def test_otrs_parse_url(self):
        hostname_match = 'help-otrs.yandex-team.ru'
        group_name = 'id'
        urls_data = (
            ('https://help-otrs.yandex-team.ru/~43666101', '43666101'),
            ('https://help-otrs.yandex-team.ru/~что-угодно', 'что-угодно'),
            ('https://help-otrs.yandex-team.ru/~1084677#tab=test_data&tags=&plot_groups=main', '1084677')
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_otrs_success(self):
        url = 'https://help-otrs.yandex-team.ru/~43666101'
        expected_data = {
            url: List(
                separator='',
                ttl=259200,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='OTRS',
                    ),
                    String(value='43666101'),

                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_otrs_fail_wrong_path(self):
        url = 'https://help-otrs.yandex-team.ru/234234233'
        expected_data = {}

        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))
