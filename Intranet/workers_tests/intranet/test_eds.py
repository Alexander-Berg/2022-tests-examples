from intranet.magiclinks.src.links.workers.intranet.eds import Worker as EdsWorker
from intranet.magiclinks.src.links.dto import List, String, Image

from ..base import BaseWorkerTestCase


class EdsWorkerTestCase(BaseWorkerTestCase):
    worker_class = EdsWorker
    worker_class_file = 'eds'
    maxDiff = None

    def test_eds_parse_url(self):
        cab_hostname_match = 'cab.test.tools.yandex-team.ru'
        eds_hostname_match = 'eds.test.tools.yandex-team.ru'

        urls_data = (
            ('https://cab.test.tools.yandex-team.ru/eds/document/1234', {'id': '1234'}, cab_hostname_match),
            ('https://cab.test.tools.yandex-team.ru/eds/document/1234/', {'id': '1234'}, cab_hostname_match),
            ('https://cab.test.tools.yandex-team.ru/eds/file/1234', {'id': '1234'}, cab_hostname_match),
            ('https://cab.test.tools.yandex-team.ru/eds/file/1234/', {'id': '1234'}, cab_hostname_match),

            ('https://eds.test.tools.yandex-team.ru/document/1234', {'id': '1234'}, eds_hostname_match),
            ('https://eds.test.tools.yandex-team.ru/document/1234/', {'id': '1234'}, eds_hostname_match),
            ('https://eds.test.tools.yandex-team.ru/file/1234', {'id': '1234'}, eds_hostname_match),
            ('https://eds.test.tools.yandex-team.ru/file/1234/', {'id': '1234'}, eds_hostname_match),
        )

        for url, path_data, hostname_match in urls_data:
            self.parse_url(url, hostname_match, path_data)

    def test_document_not_found(self):
        url = 'https://cab.test.tools.yandex-team.ru/eds/document/9999/'

        expected_data = {
            url: List(
                ttl=600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(
                        value='Документ не найден',
                        color='#999999',
                    ),
                ],
            )
        }

        cassette_name = 'eds_document_not_found.yaml'
        self.loop.run_until_complete(self.response_check(
            url,
            expected_data=expected_data,
            cassette_name=cassette_name,
        ))

    def test_document_signed(self):
        url = 'https://eds.test.tools.yandex-team.ru/document/1732/'

        action = {
            'event': 'click',
            'type': 'halfscreenpreview',
            'url': 'https://eds.test.tools.yandex-team.ru/file/1732/?_embedded=1',
        }

        expected_data = {
            url: List(
                ttl=600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(
                        value=u'[подписан]',
                        color='#3dbf41',
                        action=action
                    ),
                    String(
                        value=u'Приказ на командировку',
                        action=action,
                    ),
                ]
            )
        }
        cassette_name = 'eds_document_signed.yaml'
        self.loop.run_until_complete(self.response_check(
            url,
            expected_data=expected_data,
            cassette_name=cassette_name,
        ))

    def test_manual_document_unsigned_deadline_expired(self):
        url = 'https://cab.test.tools.yandex-team.ru/eds/document/1733/'

        action = {
            'event': 'click',
            'type': 'halfscreenpreview',
            'url': 'https://eds.test.tools.yandex-team.ru/file/1733/?_embedded=1',
        }

        expected_data = {
            url: List(
                ttl=600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(
                        value=u'[ожидает подписания вручную]',
                        color='#eabf3d',
                        action=action
                    ),
                    String(
                        value=u'Приказ на командировку',
                        action=action,
                    ),
                ]
            )
        }

        cassette_name = 'eds_document_unsigned_deadline_expired.yaml'
        self.loop.run_until_complete(self.response_check(
            url,
            expected_data=expected_data,
            cassette_name=cassette_name,
        ))

    def test_eds_document_unsigned_deadline_not_expired(self):
        url = 'https://cab.test.tools.yandex-team.ru/eds/document/1734/'

        action = {
            'event': 'click',
            'type': 'halfscreenpreview',
            'url': 'https://eds.test.tools.yandex-team.ru/file/1734/?_embedded=1',
        }

        expected_data = {
            url: List(
                ttl=600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(
                        value=u'[ожидает подписания ЭП]',
                        color='#eabf3d',
                        action=action
                    ),
                    String(
                        value=u'Приказ на командировку',
                        action=action,
                    ),
                    String(
                        value=u'истекает 17.11.2018',
                        color='#999999',
                        action=action
                    ),
                ]
            )
        }

        cassette_name = 'eds_document_unsigned_deadline_not_expired.yaml'
        self.loop.run_until_complete(self.response_check(
            url,
            expected_data=expected_data,
            cassette_name=cassette_name,
        ))

    def test_eds_document_unsigned_deadline_none(self):
        url = 'https://cab.test.tools.yandex-team.ru/eds/document/1735/'

        action = {
            'event': 'click',
            'type': 'halfscreenpreview',
            'url': 'https://eds.test.tools.yandex-team.ru/file/1735/?_embedded=1',
        }

        expected_data = {
            url: List(
                ttl=600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(
                        value=u'[ожидает подписания ЭП]',
                        color='#eabf3d',
                        action=action
                    ),
                    String(
                        value=u'Приказ на командировку',
                        action=action,
                    ),
                ]
            )
        }

        cassette_name = 'eds_document_unsigned_deadline_none.yaml'
        self.loop.run_until_complete(self.response_check(
            url,
            expected_data=expected_data,
            cassette_name=cassette_name,
        ))

    def test_eds_document_with_canceled_status(self):
        url = 'https://cab.test.tools.yandex-team.ru/eds/document/1736/'

        action = {
            'event': 'click',
            'type': 'halfscreenpreview',
            'url': 'https://eds.test.tools.yandex-team.ru/file/1736/?_embedded=1',
        }

        expected_data = {
            url: List(
                ttl=600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(
                        value=u'[отменен]',
                        color='#999999',
                        action=action
                    ),
                    String(
                        value=u'Приказ на командировку',
                        action=action,
                    ),
                ]
            )
        }

        cassette_name = 'eds_document_with_canceled_status.yaml'
        self.loop.run_until_complete(self.response_check(
            url,
            expected_data=expected_data,
            cassette_name=cassette_name,
        ))
