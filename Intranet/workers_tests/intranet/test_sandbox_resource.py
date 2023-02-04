from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.sandbox_resource import Worker as SandboxWorker


class SandboxResourceTestCase(BaseWorkerTestCase):
    worker_class = SandboxWorker
    worker_class_file = 'sandbox_resource'

    def test_sandbox_resource_parse_url(self):
        group_name = 'key'
        hostname_match = 'sandbox.yandex-team.ru'
        urls_data = (
            ('https://sandbox.yandex-team.ru/resource/465178639/view', '465178639'),
            ('https://sandbox.yandex-team.ru/resource/465178639/', '465178639'),
            ('https://sandbox.yandex-team.ru/resource/465178639', '465178639')
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_sandbox_resource_success(self):
        url = 'https://sandbox.yandex-team.ru/resource/465178639'

        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    List(ttl=1800,
                         value=[
                             Image(
                                 src=self.worker_class.FAVICON,
                                 text='Sandbox',
                             )],
                         action={
                             "event": "click",
                             "type": "halfscreenpreview",
                             "url": 'https://sandbox.yandex-team.ru/resource/465178639',
                         }),
                    String(value='#465178639',
                           color='#ABABAB',
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://sandbox.yandex-team.ru/resource/465178639',
                           }
                           ),
                    String(value='(R)'),
                    String(value='READY', color='#18a651'),
                    String(value='output_1_1.html'),
                    List(
                        ttl=1800,
                        separator='',
                        value=[
                            String(value='A', color='red'),
                            String(value='UTOCHECK', color='#888C91'),
                        ]
                    ),
                ]
            )
        }
        cassette_name = 'sandbox_resource_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_sandbox_resource_wrong_id_fail(self):
        url = 'https://sandbox.yandex-team.ru/resource/4651786393'

        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    List(ttl=1800,
                         value=[
                             Image(
                                 src=self.worker_class.FAVICON,
                                 text='Sandbox',
                             )],
                         action={
                             "event": "click",
                             "type": "halfscreenpreview",
                             "url": 'https://sandbox.yandex-team.ru/resource/4651786393',
                         }),
                    String(value='https://sandbox.yandex-team.ru/resource/4651786393'),
                ]
            )
        }
        cassette_name = 'sandbox_resource_wrong_id_fail.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))
