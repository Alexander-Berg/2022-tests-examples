import pytest
from service_test_base import ServiceTestBase
from maps.pylibs.nginx_testlib.test_helpers import HasDummyServiceTicket


class TestUpdater(ServiceTestBase):
    def setup(self):
        super(TestUpdater, self).setup()
        self.add_locations_from_file('updater.service.conf')
        self.upstream = self.add_upstream(
            'updater_upstream',
            'auto-updater.maps.yandex.net'
        )

    @pytest.mark.parametrize('version', ['1.x', '2.x'])
    def test_redirects(self, version):
        with self.bring_up_nginx() as nginx:
            url_path = '/updater/{}/'.format(version)
            self.upstream.get(
                url_path,
                headers_matcher=HasDummyServiceTicket('auto-updater')
            ).AndReturn(200, 'Info')

            self.assertEqual((200, 'Info'), nginx.get(url_path))
