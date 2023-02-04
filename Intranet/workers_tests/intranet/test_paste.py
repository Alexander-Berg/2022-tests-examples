from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.paste import Worker as PasteWorker


class PasteWorkerTestCase(BaseWorkerTestCase):
    worker_class = PasteWorker
    worker_class_file = 'paste'

    def test_paste_parse_url(self):
        hostname_match = 'paste.yandex-team.ru'
        group_name = 'id'
        urls_data = (
            ('https://paste.yandex-team.ru/123', '123'),
            ('https://paste.yandex-team.ru/123/', '123'),
            ('https://paste.yandex-team.ru/1084677#tab=test_data&tags=&plot_groups=main', '1084677')
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_paste_success(self):
        url = 'https://paste.yandex-team.ru/123'
        expected_data = {
            url: List(
                ttl=3600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Paste',
                    ),
                    String(value='paste.yandex-team.ru/123',
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://paste.yandex-team.ru/123'
                           }
                           ),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url, expected_data=expected_data, ))

    def test_paste_fail_without_id(self):
        url = 'https://paste.yandex-team.ru/'
        expected_data = {}

        self.loop.run_until_complete(self.response_check(url, expected_data=expected_data, ))

    def test_paste_fail_wrong_path(self):
        url = 'https://paste.yandex-team.ru/last'
        expected_data = {}

        self.loop.run_until_complete(self.response_check(url, expected_data=expected_data, ))
