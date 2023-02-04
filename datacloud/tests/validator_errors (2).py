# -*- coding: utf-8 -*-
import unittest
import os
import csv
from library.python import resource

from datacloud.input_pipeline.normalizer.helpers import get_xprod_normalizer


class TestValidatorErrorsSearch(unittest.TestCase):
    RETRO_DATE_FOLDER = 'retro_date'
    DELIMITER = '\t'

    def regular_case(self, input_resource, normalized_resource, is_iter=False, normalizer=None):
        normalizer = normalizer or get_xprod_normalizer()

        input_data = csv.reader(
            resource.find(input_resource).splitlines(),
            delimiter=self.DELIMITER
        )
        if is_iter:
            rows = normalizer.inormalize(input_data)
        else:
            rows = normalizer.normalize(input_data)

        normalized_data = csv.reader(
            resource.find(normalized_resource).splitlines(),
            delimiter=self.DELIMITER
        )
        for row, n_row in zip(rows, normalized_data):
            self.assertEqual(row, n_row)

    def check_assert(self, input_resource, assert_row, normalizer=None):
        normalizer = normalizer or get_xprod_normalizer()

        data = csv.reader(
            resource.find(input_resource).splitlines(),
            delimiter=self.DELIMITER
        )
        rows = normalizer.inormalize(data)

        for _ in range(assert_row):
            rows.next()

        try:
            r = rows.next()
            raise ValueError(r)
        except AssertionError:
            pass
        except ValueError as e:
            self.fail('Bad value {0} should be asserted!'.format(e))

    def test_phone(self):
        self.regular_case('phone_r.tsv', 'phone_n.tsv', False)
        self.regular_case('phone_r.tsv', 'phone_n.tsv', True)

    def test_email(self):
        self.regular_case('email_r.tsv', 'email_n.tsv', False)
        self.regular_case('email_r.tsv', 'email_n.tsv', True)

    def test_target(self):
        self.regular_case('target_r.tsv', 'target_n.tsv', False)
        self.regular_case('target_r.tsv', 'target_n.tsv', True)

    def test_retro_date_no_parse(self):
        test_data_file = os.path.join(self.RETRO_DATE_FOLDER, 'no_parse.tsv')
        self.check_assert(test_data_file, 1)

    def test_retro_new_format(self):
        test_data_file = os.path.join(self.RETRO_DATE_FOLDER, 'new_format.tsv')
        self.check_assert(test_data_file, 4)

    def test_retro_date(self):
        normalizer = get_xprod_normalizer(hard_kill=True)
        test_r = os.path.join(self.RETRO_DATE_FOLDER, 'retro_date_r.tsv')
        test_n = os.path.join(self.RETRO_DATE_FOLDER, 'retro_date_n.tsv')
        self.regular_case(test_r, test_n, False, normalizer=normalizer)
        self.regular_case(test_r, test_n, True, normalizer=normalizer)

    def test_retro_given_format(self):
        normalizer = get_xprod_normalizer(date_format='%d.%m.%Y')
        test_data_file = os.path.join(self.RETRO_DATE_FOLDER, 'given_format.tsv')
        self.check_assert(test_data_file, 2, normalizer=normalizer)

    def test_yuid(self):
        self.regular_case('yuid_r.tsv', 'yuid_n.tsv', False)
        self.regular_case('yuid_r.tsv', 'yuid_n.tsv', True)


if __name__ == '__main__':
    unittest.main()
