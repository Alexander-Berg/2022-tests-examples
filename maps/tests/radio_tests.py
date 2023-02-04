from service_test_base import ServiceTestBase
from maps.pylibs.nginx_testlib.test_helpers import HasDummyServiceTicket


class TestRadio(ServiceTestBase):
    def setup(self):
        super(TestRadio, self).setup()
        self.add_locations_from_file('radio.service.conf')
        self.upstream = self.add_upstream(
            'auto_radio_upstream',
            'auto-radio.maps.yandex.net'
        )

    def test_stationlist_redirects(self):
        with self.bring_up_nginx() as nginx:
            self.upstream.get(
                '/radio/1.x/stationlist',
                headers_matcher=HasDummyServiceTicket('auto-radio')
            ).AndReturn(200, 'Info')

            self.assertEqual((200, 'Info'), nginx.get(
                '/radio/1.x/stationlist'))

    def test_default_station_redirects(self):
        with self.bring_up_nginx() as nginx:
            LL_ARG = '37.589433,55.733667'
            CAR_TYPE = 'AM'
            URL = '/radio/1.x/default_station?ll={ll}&car_type={car_type}'.format(
                ll=LL_ARG,
                car_type=CAR_TYPE,
            )

            self.upstream.get(
                URL,
                headers_matcher=HasDummyServiceTicket('auto-radio')
            ).AndReturn(200, 'Radio')

            self.assertEqual((200, 'Radio'), nginx.get(URL))

    def test_alice_handle_unavailable(self):
        with self.bring_up_nginx() as nginx:
            self.assertEqual(404, nginx.post('/radio/1.x/mm/run', '')[0])
