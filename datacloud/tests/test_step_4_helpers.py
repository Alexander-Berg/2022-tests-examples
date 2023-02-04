import unittest
from datacloud.features.geo.constants import DISTANCE_THRESH
from datacloud.features.geo.helpers import DistanceToFMapper


class TestStep4Helpers(unittest.TestCase):
    def test_distance_to_f_mapper_cont(self):
        df_mapper = DistanceToFMapper(distance_thresh=DISTANCE_THRESH)

        distance_pairs = [
            (9, 1),
            (90, 0.9999700728500203),
            (300, 0.9311022177040369),
            (1500, 0.32151074503336163),
            (5000, 0.09966779661208958),
            (30000, 0.01666511807422788),
        ]
        for distance, feature in distance_pairs:
            self.assertAlmostEqual(df_mapper.distances2features(distance), feature)

    def test_distance_to_f_mapper_bin(self):
        df_mapper = DistanceToFMapper(distance_thresh=DISTANCE_THRESH)

        distance_pairs = [(0, 1.), (100, 1.), (500, 0.),
                          (1000, 0.), (5000, 0.)]
        for distance, feature in distance_pairs:
            self.assertEqual(df_mapper.distances2features_binary(distance), feature)

    def test_distance_to_f_mapper(self):
        df_mapper = DistanceToFMapper(distance_thresh=DISTANCE_THRESH)
        rows = [
            {'distance': 200},
            {'distance': 600, 'something_else': 'here'}
        ]
        mapped_rows = list(mapped_row for row in rows for mapped_row in df_mapper(row))
        mapped_rows_expected = [
            {'feature': 1.},
            {'feature': 0., 'something_else': 'here'}
        ]
        self.assertListEqual(mapped_rows, mapped_rows_expected)

        with self.assertRaises(KeyError):
            next(df_mapper({'here_is': 'no_distance'}))
