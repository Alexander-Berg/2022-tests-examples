import unittest
import datetime

from agency_rewards.rewards.utils.const import CalendarType, CommType
from agency_rewards.rewards.platform.intersect import (
    format_intersections,
    get_calculation_intersections,
)
from agency_rewards.rewards.utils.bunker import BunkerCalc


class TestCalcIntersections(unittest.TestCase):
    def setUp(self):
        self.insert_dt = datetime.datetime.now()
        self.env = 'test'
        self.node = 'testing'
        self.calcs = [
            BunkerCalc(
                {'scale': '1', 'freq': 'm', 'calendar': 'f', 'comm_type': ['direct', 'market']},
                self.env,
                self.insert_dt,
                self.node,
                'base_1',
            ),
            BunkerCalc(
                {'scale': '1', 'freq': 'm', 'calendar': 'g', 'comm_type': ['market', 'media']},
                self.env,
                self.insert_dt,
                self.node,
                'base_2',
            ),
            BunkerCalc(
                {'scale': '1', 'freq': 'm', 'calendar': 'f', 'comm_type': ['market', 'media3']},
                self.env,
                self.insert_dt,
                self.node,
                'base_3',
            ),
        ]

    def test_get_calculation_intersection(self):
        intersection = get_calculation_intersections(self.calcs)
        self.assertIsNotNone(intersection)
        self.assertEqual(1, len(intersection), intersection)
        self.assertEqual(intersection[0][0].scale, 1)
        self.assertEqual(intersection[0][0].freq, 'm')
        self.assertEqual(intersection[0][0].calendar, CalendarType.Financial)
        self.assertEqual(intersection[0][1], CommType.Market.value)
        self.assertEqual(len(intersection[0][2]), 2)
        calc = intersection[0][2]
        self.assertTrue(CommType.Market.value in calc[0].comm_types)
        self.assertTrue(CommType.Market.value in calc[1].comm_types)

    def test_format_intersections(self):
        calcs = self.calcs + [
            BunkerCalc(
                {'scale': 'prof', 'freq': 'm', 'calendar': 'g', 'comm_type': ['market', 'direct', 'media']},
                self.env,
                self.insert_dt,
                self.node,
                'prof_1',
            ),
            BunkerCalc(
                {'scale': '2', 'freq': 'm', 'calendar': 'g', 'comm_type': ['market', 'direct', 'media2']},
                self.env,
                self.insert_dt,
                self.node,
                'prof_2',
            ),
        ]
        intersection = get_calculation_intersections(calcs)
        body = format_intersections(intersection)
        need = """

        * Тип комиссии=11, Шкала=1, Частота=m:
            - base_1
            - base_3

        * Тип комиссии=11, Шкала=2, Частота=m:
            - prof_1
            - prof_2"""
        self.assertEqual(body, need)
