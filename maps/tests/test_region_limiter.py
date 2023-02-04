from freezegun import freeze_time
import unittest

from maps.analyzer.services.eta_comparison.lib import RegionLimiter

CONFIG = [
    {
        "min_distance": 5000.0,
        "max_distance": 10000.0,
        "id": 42,
        "requests_per_day": 10
    },
    {
        "min_distance": 3000.0,
        "max_distance": 5000.0,
        "id": 42,
        "requests_per_day": 10
    },
    {
        "min_distance": 5000.0,
        "max_distance": 10000.0,
        "id": 43,
        "requests_per_day": 10
    }
]


class TestRegionLimiter(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.geobase = unittest.mock.Mock()

    def setUp(self):
        self.freezer = freeze_time("1970-01-01 00:00:00")
        self.ftime = self.freezer.start()
        self.region_limiter = RegionLimiter(CONFIG, self.geobase)
        self.geobase.get_parents_ids.return_value = []

    def tearDown(self):
        self.freezer.stop()

    def test_non_config_region(self):
        self.assertTrue(self.region_limiter.has_limit(41, 41, 41).has_limit)

    def test_config_region(self):
        self.assertFalse(self.region_limiter.has_limit(42, 6000, 0).has_limit)
        self.assertFalse(self.region_limiter.has_limit(42, 3000, 0).has_limit)
        self.assertFalse(self.region_limiter.has_limit(43, 9000, 0).has_limit)
        self.assertTrue(self.region_limiter.has_limit(42, 8000, 0).has_limit)
        self.assertTrue(self.region_limiter.has_limit(42, 4000, 0).has_limit)
        self.assertTrue(self.region_limiter.has_limit(43, 7000, 0).has_limit)

    def test_parent_region(self):
        self.geobase.get_parents_ids.return_value = [42]
        limit_value = self.region_limiter.has_limit(41, 6000, 0)
        self.assertFalse(limit_value.has_limit)
        self.assertEqual(limit_value.parent_region_id, 42)

    def test_limit(self):
        for i in range(0, 86400, 864):
            if i % 8640 == 0:
                self.assertFalse(self.region_limiter.has_limit(42, 6000, i * 8640).has_limit)
            else:
                self.assertTrue(self.region_limiter.has_limit(42, 6000, i * 8640).has_limit)
            self.ftime.tick(864)
