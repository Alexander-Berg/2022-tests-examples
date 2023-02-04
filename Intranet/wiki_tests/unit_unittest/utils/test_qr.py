
from urllib.parse import urlparse

from wiki.utils import qr
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class QRCodeTest(BaseTestCase):
    def setUp(self):
        super(QRCodeTest, self).setUp()
        self.setUsers()

    def test_qr_code_in_context(self):
        tag = 'тестСтраница'
        qr_url = qr.generate_qr_url(tag, user=self.user_thasonic)
        page_url, uid, timestamp = qr.fetch_data_from_qr_url(qr_url)

        self.assertEqual(tag, urlparse(page_url).path.strip('/'))
        self.assertEqual(uid, self.user_thasonic.staff.uid)
