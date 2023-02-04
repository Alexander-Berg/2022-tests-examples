from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.sandbox_scheduler import Worker as SandboxWorker


class SandboxSchedulerTestCase(BaseWorkerTestCase):
    worker_class = SandboxWorker
    worker_class_file = 'sandbox_scheduler'

    def test_sandbox_scheduler_parse_url(self):
        group_name = 'key'
        hostname_match = 'sandbox.yandex-team.ru'
        urls_data = (
            ('https://sandbox.yandex-team.ru/scheduler/7807/view', '7807'),
            ('https://sandbox.yandex-team.ru/scheduler/7807/', '7807'),
            ('https://sandbox.yandex-team.ru/scheduler/7807', '7807')
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_sandbox_scheduler_success(self):
        url = 'https://sandbox.yandex-team.ru/scheduler/7801'

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
                             "url": 'https://sandbox.yandex-team.ru/scheduler/7801',
                         }),
                    String(value='#7801',
                           color='#ABABAB',
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://sandbox.yandex-team.ru/scheduler/7801',
                           }),
                    String(value='(S)'),
                    String(value='STOPPED', color='#FD0D1B'),
                    String(value=('ML-ENGINE task arc:/yabs/utils/learn-'
                                  '\ntasks2/fram/images/search_auto_lol4_unstable_images_try4.yml')),
                    User(login='fram'),
                ]
            )
        }
        cassette_name = 'sandbox_scheduler_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_sandbox_scheduler_wrong_id_fail(self):
        url = 'https://sandbox.yandex-team.ru/scheduler/78074444'

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
                             "url": 'https://sandbox.yandex-team.ru/scheduler/78074444',
                         }),
                    String(value='https://sandbox.yandex-team.ru/scheduler/78074444'),
                ]
            )
        }
        cassette_name = 'sandbox_scheduler_wrong_id_fail.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))
