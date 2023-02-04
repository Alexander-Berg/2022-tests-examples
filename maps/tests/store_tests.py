import pytest
from service_test_base import ServiceTestBase
from maps.pylibs.nginx_testlib.test_helpers import HasDummyServiceTicket


class TestStore(ServiceTestBase):
    def setup(self):
        super(TestStore, self).setup()
        self.add_locations_from_file('store.service.conf')
        self.upstream = self.add_upstream(
            'updater_upstream',
            'auto-updater.maps.yandex.net'
        )

    @pytest.mark.parametrize('endpoint', ['app_details', 'download_meta', 'app_list'])
    def test_redirects(self, endpoint):
        with self.bring_up_nginx() as nginx:
            url_path = '/store/1.x/' + endpoint
            self.upstream.get(
                url_path,
                headers_matcher=HasDummyServiceTicket('auto-updater')
            ).AndReturn(200, 'Info')

            self.assertEqual((200, 'Info'), nginx.get(url_path))
