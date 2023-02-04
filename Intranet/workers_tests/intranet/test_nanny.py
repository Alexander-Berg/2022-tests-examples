from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.nanny import Worker as NannyWorker


class NannyTestCase(BaseWorkerTestCase):
    worker_class = NannyWorker
    worker_class_file = 'nanny'

    def test_nanny_parse_url(self):
        group_name = 'key'
        urls_data = (
            ('https://nanny.yandex-team.ru/ui/#/t/BEGEMOT-2766834/',
             'BEGEMOT-2766834', 'nanny.yandex-team.ru'),
            ('https://nanny.yandex-team.ru/ui/#/t/BEGEMOT-2766834',
             'BEGEMOT-2766834', 'nanny.yandex-team.ru'),
        )
        for url, fragment_match, hostname_match in urls_data:
            self.parse_url(url=url, hostname_match=hostname_match,
                           fragment_data={group_name: fragment_match})

    def test_nanny_url_strike_successful(self):
        url = 'https://nanny.yandex-team.ru/ui/#/t/BEGEMOT-2766834/'
        expected_data = {
            url: List(
                ttl=3600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(value='BEGEMOT-2766834', strike=True),
                    String(value='begemot_hamster_worker_e'),
                    String(value='DEPLOY_SUCCESS', color='#4caf50'),
                ]
            )
        }
        cassette_name = 'nanny_strike_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nanny_url_successful(self):
        url = 'https://nanny.yandex-team.ru/ui/#/t/BEGEMOT-2812348'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(value='BEGEMOT-2812348', strike=False),
                    String(value='begemot_hamster_worker_bravo'),
                    String(value='PREPARING', color='#00bcd4'),
                ]
            )
        }
        cassette_name = 'nanny_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nanny_url_without_service_successful(self):
        url = 'https://nanny.yandex-team.ru/ui/#/t/IMAGES-2815777'
        expected_data = {
            url: List(
                ttl=3600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(value='IMAGES-2815777', strike=True),
                    String(value='-'),
                    String(value='DEPLOY_SUCCESS', color='#4caf50'),
                ]
            )
        }
        cassette_name = 'nanny_without_service_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_nanny_url_wrong_number_failed(self):
        url = 'https://nanny.yandex-team.ru/ui/#/t/BEGEMOT-276683423423423423'
        cassette_name = 'nanny_wrong_number_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              ))

    def test_nanny_url_wrong_format_failed(self):
        url = 'https://nanny.yandex-team.ru/ui/#/t/BEGEMOT/'
        expected_data = {}
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))
