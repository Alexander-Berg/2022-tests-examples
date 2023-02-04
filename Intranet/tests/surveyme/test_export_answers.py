# -*- coding: utf-8 -*-
from io import BytesIO
from django.test import TestCase

from events.surveyme.export_answers import (
    CsvExporter,
)


class TestCsvExporter(TestCase):
    def test_csv_exporter(self):
        answers = iter([
            ('Текст1', 'Текст2', 'Текст3'),
            ('один+один+один', 'два+два', 'три'),
            ('11111', '222', '3'),
        ])
        with BytesIO() as st:
            with CsvExporter(st) as exporter:
                exporter.writeheader(next(answers))
                for values in answers:
                    exporter.writerow(values)
            self.assertEqual(
                st.getvalue(),
                'Текст1,Текст2,Текст3\r\nодин+один+один,два+два,три\r\n11111,222,3\r\n'.encode(),
            )
