import unittest
import datetime

from agency_rewards.rewards.platform import get_expiring_calcs, format_expiring_calcs
from agency_rewards.rewards.utils.bunker import BunkerCalc


class TestCalcExpirations(unittest.TestCase):
    def setUp(self):
        self.insert_dt = datetime.datetime(2021, 3, 1)
        self.env = 'test'
        self.node = 'testing'
        self.calcs = [
            BunkerCalc(
                {
                    'scale': '1',
                    'freq': 'm',
                    'calendar': 'f',
                    'comm_type': ['direct', 'market'],
                    'till_dt': datetime.datetime(2021, 3, 20).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                },
                self.env,
                self.insert_dt,
                self.node,
                'base_1',
            ),
            BunkerCalc(
                {
                    'scale': '1',
                    'freq': 'm',
                    'calendar': 'f',
                    'comm_type': ['market', 'media'],
                    'till_dt': datetime.datetime(2021, 3, 30).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                },
                self.env,
                self.insert_dt,
                self.node,
                'base_2',
            ),
            BunkerCalc(
                {
                    'scale': '1',
                    'freq': 'm',
                    'calendar': 'f',
                    'comm_type': ['market', 'media'],
                    'till_dt': datetime.datetime(2021, 3, 31).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                },
                self.env,
                self.insert_dt,
                self.node,
                'base_3',
            ),
            BunkerCalc(
                {
                    'scale': '1',
                    'freq': 'm',
                    'calendar': 'f',
                    'comm_type': ['market', 'media3'],
                    'till_dt': datetime.datetime(2021, 4, 20).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                },
                self.env,
                self.insert_dt,
                self.node,
                'base_4',
            ),
        ]

    def test_get_expiring_calcs(self):
        """
        Проверяет, что функция достает только заканчивающиеся расчеты
        base_2 и base_3 -- угловые случаи для 30 дней
        """
        calcs = get_expiring_calcs(self.calcs, dt=self.insert_dt, delta=30)
        self.assertEqual(2, len(calcs))
        self.assertEqual(calcs[0].full_name, 'base_1')
        self.assertEqual(calcs[1].full_name, 'base_2')

        calcs = get_expiring_calcs(self.calcs, dt=self.insert_dt, delta=60)
        self.assertEqual(4, len(calcs))

    def test_format_expiring_calcs(self):
        """
        Проверяет форматирование строки с расчетами для сообщения
        """
        formatted_calcs = format_expiring_calcs(self.calcs)
        expected_formatted_calcs = """base_1 срок действия: до 2021-03-20
base_2 срок действия: до 2021-03-30
base_3 срок действия: до 2021-03-31
base_4 срок действия: до 2021-04-20
"""
        self.assertEqual(expected_formatted_calcs, formatted_calcs, formatted_calcs)
