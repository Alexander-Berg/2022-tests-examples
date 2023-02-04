import pytest
from service_test_base import ServiceTestBase


class TestDatacollect(ServiceTestBase):
    def setup(self):
        super(TestDatacollect, self).setup()
        self.add_locations_from_file('datacollect.service.conf')

    @pytest.mark.parametrize(
        'endpoint',
        [
            'vehicle_data',
            'gps_signals'
        ]
    )
    def test_endpoint_returns_ok(self, endpoint):
        with self.bring_up_nginx() as nginx:
            self.assertEqual((200, ''), nginx.get(
                '/datacollect/1.x/' + endpoint))
