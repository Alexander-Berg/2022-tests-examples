import unittest

from maps.garden.scheduler.lib.views import unistat


class TestUniStat(unittest.TestCase):
    def test_unitest(self):
        result = unistat._json_to_unistat({"key1": 1, "key2": 1.1})
        expected = [["key1_max", 1], ["key2_max", 1.1]]
        result.sort(key=lambda item: item[0])
        self.assertEqual(result, expected)


if __name__ == "__main__":
    unittest.main()
