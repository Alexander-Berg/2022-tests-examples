package common.clients.geocoder.testkit

import common.clients.geocoder.GeocoderClient
import zio.test.mock.mockable

@mockable[GeocoderClient.Service]
object GeocoderClientMock {}
