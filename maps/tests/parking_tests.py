import pytest
from service_test_base import ServiceTestBase


class TestParking(ServiceTestBase):
    def setup(self):
        super(TestParking, self).setup()
        self.add_locations_from_file('parking.service.conf')

    @pytest.mark.parametrize(
        'endpoint',
        [
            'active_sessions',
            'register_device'
        ]
    )
    def test_returns_ok(self, endpoint):
        with self.bring_up_nginx() as nginx:
            url_path = '/parking/1.x/' + endpoint
            self.assertEqual((200, ''), nginx.get(url_path))

    def test_stop_session_fails(self):
        with self.bring_up_nginx() as nginx:
            url_path = '/parking/1.x/stop_session'
            self.assertEqual((200, ''), nginx.get(url_path))
