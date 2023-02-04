# -*- coding: utf-8 -*-
import unittest
import tempfile
import csv
from library.python import resource
import yatest.common as yc

from datacloud.input_pipeline.input_checker.helpers import get_xprod_validator


class TestValidatorErrorsSearch(unittest.TestCase):
    DELIMITER = ';'

    def regular_case(self, case_resource, num_of_problems):
        case = resource.find(case_resource)
        with tempfile.NamedTemporaryFile(dir=yc.output_path()) as csv_file:
            csv_file.write(case)
            csv_file.seek(0)

            validator = get_xprod_validator(csv_file.name, self.DELIMITER)
            data = csv.reader(csv_file, delimiter=self.DELIMITER)

            problems = validator.validate_xprod(data)
            self.assertEqual(len(problems), num_of_problems)
            csv_file.seek(0)
            problems = [problem for problem in validator.ivalidate_xprod(data)]
            self.assertEqual(len(problems), num_of_problems)

    def test_phone(self):
        self.regular_case('phones.csv', 4)

    def test_email(self):
        self.regular_case('emails.csv', 6)

    def test_required(self):
        """
            Tests retro_date (it's required) and
            there should be one of the idintificators (listed in constants id_fields)
        """
        self.regular_case('required.csv', 3)

    def test_birth_date(self):
        self.regular_case('dates.csv', 5)

    def test_yuid(self):
        self.regular_case('yuids.csv', 2)


if __name__ == '__main__':
    unittest.main()
