import unittest
from unittest.mock import patch, MagicMock
import datetime

from dwh.grocery.rebuild_mv import RebuildSingleMV


class TestRebuildSingleMV(unittest.TestCase):

    @patch("dwh.grocery.rebuild_mv.date", wraps=datetime.date)
    @patch("dwh.grocery.rebuild_mv.create_engine")
    @patch("dwh.grocery.rebuild_mv.DBExportable.exportable_from_uri")
    def test_from_to(self, exportable_mock, create_engine_mock, date_mock):
        """
            Проверяем правильное заполнение from_ и to_ в методе run
            класса RebuildSingleMV
        """
        # общие моки для правильной работы
        exportable_mock.return_value = MagicMock()
        exportable_mock.return_value.schema = "SCHEMA"
        exportable_mock.return_value.table = "TABLE"

        engine_mock = MagicMock()
        create_engine_mock.return_value = engine_mock
        test_cases = [
            # нормальный случай без перехода через границу года
            {
                "start_month": datetime.datetime(year=2022, month=2, day=1),
                "end_month": datetime.datetime(year=2022, month=5, day=1),
                "from": 3,
                "to": 6,
                "today": datetime.date(year=2022, month=8, day=1)
            },
            # переход через границу года
            {
                "start_month": datetime.datetime(year=2022, month=12, day=1),
                "end_month": datetime.datetime(year=2022, month=12, day=1),
                "from": 1,
                "to": 1,
                "today": datetime.date(year=2023, month=1, day=1)
            }

        ]
        for case in test_cases:
            date_mock.today.return_value = case["today"]
            task = RebuildSingleMV(uri="", start_month=case["start_month"], end_month=case["end_month"])
            task.run()
            expected = f"""
                begin
                    SYSTEM.PK_TAB_DT_REFR.REFRESH_T('SCHEMA','TABLE', {case["from"]},
                                                     {case["to"]}, '', 0);
                end;
            """
            engine_mock.execute.assert_called_with(expected)
