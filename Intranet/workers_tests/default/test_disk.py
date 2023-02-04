from ..base import BaseWorkerTestCase

from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.default.disk import Worker as DiskWorker


class DiskTestCase(BaseWorkerTestCase):
    worker_class = DiskWorker
    worker_class_file = 'disk'

    def test_disk_parse_url(self):
        group_name = 'id'
        urls_data = (
            ('https://yadi.sk/i/eMAk33TUi79Qw',
             'i/eMAk33TUi79Qw', 'yadi.sk'),
            ('https://yadi.sk/i/OuF8o9gPiFssE/',
             'i/OuF8o9gPiFssE/', 'yadi.sk'),
        )
        for url, path_match, hostname_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_disk_url_successful(self):
        url = 'https://yadi.sk/i/Th58_sPS3NvcnQ'
        expected_data = {
            url: List(
                ttl=300,
                value=[
                    List(
                        ttl=300,
                        value=[
                            Image(
                                src=self.worker_class.FAVICON,
                                text='Disk'

                            )],
                        action={
                            "event": "click",
                            "type": "halfscreenpreview",
                            "url": ('https://downloader.disk.yandex.ru/preview/167f3507292a6e5c47991'
                                    'bf32e67dd6c7d6d3ece46aebf2164e89b1ce3f76517/inf/8ElnrpTjCloKyhb0gYm'
                                    'LsSkgL4Q1IE7YeubrUdp3PgMvu0iaO_rhQyMZGBssRioXN96sxc-vyrnSZPDvaVwYHw%3'
                                    'D%3D?uid=0&filename=2017-10-20_14-52-24.png&disposition=inline&hash=&li'
                                    'mit=0&content_type=image%2Fpng&tknv=v2&size=XL&crop=0'),
                        },

                    ),
                    String(value='2017-10-20_14-52-24.png',),
                    String(value='@smoskerYan'),
                    Image(
                        src=self.worker_class.COUNT_IMAGE,
                        text='Count',
                    ),
                    String(value='37'),
                ]
            )
        }
        cassette_name = 'disk_operation_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_disk_url_failed(self):
        url = 'https://yadi.sk/i/Th58_sPS3NvcnQrr'
        cassette_name = 'disk_operation_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              ))
