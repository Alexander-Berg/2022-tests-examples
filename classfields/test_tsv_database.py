# coding=utf8
import os
import unittest
from common.tsv_database import TSVDatabase, TSVReader

__author__ = 'baibik'


class TestTSVDatabase(unittest.TestCase):
    TEST_FILE_PATH = "test_db.tsv"

    def test_save_to_db_without_pk(self):
        self.__do_common_asserts__(False)
        os.remove(self.TEST_FILE_PATH)

    def test_save_to_db_with_pk(self):
        self.__do_common_asserts__(True)

        db = TSVDatabase(self.TEST_FILE_PATH, pk=0)

        db.save_line(['a', 2, '1000'])

        line = db.get_by_primary_key_as_list('a')
        self.assertEqual(line[1], 2)
        self.assertEqual(line[2], '1000')

        db.commit()
        tsv_reader = TSVReader(self.TEST_FILE_PATH)
        lines = tsv_reader.get_arr_lines()
        self.assertEqual(len(lines), 2)

        self.assertEqual(lines[0][0], 'a')
        self.assertEqual(lines[0][1], '2')
        self.assertEqual(lines[0][2], '1000')

        db.save_line(['c', 5, '2000'])
        db.save_line(['d', 'df', '2werwer000'])
        db.commit()
        tsv_reader = TSVReader(self.TEST_FILE_PATH)
        lines = tsv_reader.get_arr_lines()
        self.assertEqual(len(lines), 4)

    def __do_common_asserts__(self, pk):
        if os.path.isfile(self.TEST_FILE_PATH):
            os.remove(self.TEST_FILE_PATH)

        if pk:
            db = TSVDatabase(self.TEST_FILE_PATH, pk=0)
        else:
            db = TSVDatabase(self.TEST_FILE_PATH)

        lines = [['a', 1, '34324'], ['2', 'c', '23434']]

        db.save_line(lines[0])

        tsv_reader = TSVReader(self.TEST_FILE_PATH)
        self.assertFalse(len(tsv_reader.get_arr_lines()))

        db.commit()

        tsv_reader = TSVReader(self.TEST_FILE_PATH)
        self.assertEqual(len(tsv_reader.get_arr_lines()), 1)

        if pk:
            db = TSVDatabase(self.TEST_FILE_PATH, pk=0)
        else:
            db = TSVDatabase(self.TEST_FILE_PATH)

        db.save_line(lines[1])

        tsv_reader = TSVReader(self.TEST_FILE_PATH)
        self.assertEqual(len(tsv_reader.get_arr_lines()), 1)
        db.commit()

        tsv_reader = TSVReader(self.TEST_FILE_PATH)
        self.assertEqual(len(tsv_reader.get_arr_lines()), 2)

        tsv_reader_lines = tsv_reader.get_arr_lines()

        self.assertEqual(tsv_reader_lines[0][0], 'a')
        self.assertEqual(tsv_reader_lines[1][0], '2')
