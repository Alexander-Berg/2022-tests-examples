from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.sandbox_task import Worker as SandboxWorker


class SandboxTaskTestCase(BaseWorkerTestCase):
    worker_class = SandboxWorker
    worker_class_file = 'sandbox_task'

    def test_sandbox_task_parse_url(self):
        group_name = 'key'
        hostname_match = 'sandbox.yandex-team.ru'
        urls_data = (
            ('https://sandbox.yandex-team.ru/task/204816037/view', '204816037'),
            ('https://sandbox.yandex-team.ru/task/204816037/', '204816037'),
            ('https://sandbox.yandex-team.ru/task/204816037', '204816037')
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_sandbox_task_success(self):
        url = 'https://sandbox.yandex-team.ru/task/204816037'

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
                             "url": 'https://sandbox.yandex-team.ru/task/204816037',
                         }),

                    String(value='#204816037',
                           color='#ABABAB',
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://sandbox.yandex-team.ru/task/204816037',
                           }),
                    String(value='(T)'),
                    String(value='SUCCESS', color='#18A651'),
                    String(value=('{"action_id":"mapreduce/import_to_mr_merger",'
                                  '"params":{"date":"2018-01-23","logname":"syncserver-\naccess-log",'
                                  '"mrbasename":"yt_banach","srcpath":"kafka-\nimport",'
                                  '"with_closeday":1}}')),
                    List(
                        ttl=1800,
                        separator='',
                        value=[
                            String(value='S', color='red'),
                            String(value='TATINFRA', color='#888C91'),
                        ]
                    ),
                ]
            )
        }
        cassette_name = 'sandbox_task_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_sandbox_task_wrong_id_fail(self):
        url = 'https://sandbox.yandex-team.ru/task/20481603'

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
                             "url": 'https://sandbox.yandex-team.ru/task/20481603',
                         }),
                    String(value='https://sandbox.yandex-team.ru/task/20481603'),
                ]
            )
        }
        cassette_name = 'sandbox_task_wrong_id_fail.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_sandbox_task_api_error_fail(self):
        url = 'https://sandbox.yandex-team.ru/task/204816034234234234234234'

        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Sandbox',
                    ),
                    String(value='https://sandbox.yandex-team.ru/task/204816034234234234234234'),
                ],
                completed=False,
            )
        }
        cassette_name = 'sandbox_task_api_error_fail.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))
