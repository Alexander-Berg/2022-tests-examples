from collections import namedtuple
from datetime import date
from unittest import TestCase, mock

from common.yt.autoparts_offers_dump import OffersDump, Mappings


class TestOffersDump(TestCase):
    @mock.patch('common.yt.autoparts_offers_dump.OffersDump._client')
    def test_sellers_offers_count_per_status(self, mclient: mock.MagicMock):
        def get_results(*args, **kwargs):
            return namedtuple("Results", "table")(wanted)

        wanted = [(date.today(), 13, 'some_status', 9999), ]
        mclient.query().run().get_results = get_results
        got, = OffersDump.sellers_offers_count_per_status()
        self.assertTrue(mclient.query.called)
        self.assertEqual(Mappings.offs_count_per_status(*wanted[0]), got)
