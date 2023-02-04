
from mock import patch
from pretend import stub

from wiki.api_v1.views.serializers.files import FileProxy
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class FileProxyTestCase(BaseApiTestCase):
    def test_docviewer_url(self):
        file_stub = stub(url='hello.jpg', page=stub(supertag='pagename', tag='ПагеНаме', url='/PageName'))
        file_proxy = FileProxy(file_stub)
        with patch('wiki.api_v1.views.serializers.files.settings.NGINX_HOST', 'wiki.yandex-team.ru'):
            self.assertEqual('https://wiki.yandex-team.ru/PageName/.files/hello.jpg', file_proxy.docviewer_url)
