import unittest

from datetime import datetime
from test_file_system import TestFileSystem


TESTING_DATE = datetime(2021, 12, 7).date()
TESTING_BASE_PATH = "maps/carparks/testing"
PRODUCTION_BASE_PATH = "maps/carparks/production"


class InputArgsTest(unittest.TestCase):
    def test_invalid_ages(self):
        self.assertRaisesRegex(
            Exception,
            "Invalid time frame",
            TestFileSystem,
            TESTING_DATE,
            TESTING_BASE_PATH,
            resource_name="",
            mined_hard_delete_age=0,
            mined_soft_delete_age=0,
            snippets_delete_age=0)
        self.assertRaisesRegex(
            Exception,
            "Invalid time frame",
            TestFileSystem,
            TESTING_DATE,
            TESTING_BASE_PATH,
            resource_name="",
            mined_hard_delete_age=-10,
            mined_soft_delete_age=-10,
            snippets_delete_age=-10)

    def test_ages_overlap(self):
        self.assertRaisesRegex(
            Exception,
            "Invalid time frame",
            TestFileSystem,
            TESTING_DATE,
            TESTING_BASE_PATH,
            resource_name="",
            mined_hard_delete_age=10,
            mined_soft_delete_age=20,  # soft age - can't be less than hard age
            snippets_delete_age=10)

    def test_production_constraints(self):
        self.assertRaisesRegex(
            Exception,
            "Dangerous time frame is used for production",
            TestFileSystem,
            TESTING_DATE,
            PRODUCTION_BASE_PATH,
            resource_name="",
            mined_hard_delete_age=700,  # can't be less than 730 (2 year)
            mined_soft_delete_age=182,
            snippets_delete_age=365)
        self.assertRaisesRegex(
            Exception,
            "Dangerous time frame is used for production",
            TestFileSystem,
            TESTING_DATE,
            PRODUCTION_BASE_PATH,
            resource_name="",
            mined_hard_delete_age=730,
            mined_soft_delete_age=150,  # can't be less than 182 (half a year)
            snippets_delete_age=365)
        self.assertRaisesRegex(
            Exception,
            "Dangerous time frame is used for production",
            TestFileSystem,
            TESTING_DATE,
            PRODUCTION_BASE_PATH,
            resource_name="",
            mined_hard_delete_age=730,
            mined_soft_delete_age=182,
            snippets_delete_age=300)  # can't be less than 365 (1 year)
        self.assertIsNotNone(
            TestFileSystem(
                TESTING_DATE,
                PRODUCTION_BASE_PATH,
                resource_name="",
                mined_hard_delete_age=730,
                mined_soft_delete_age=182,
                snippets_delete_age=365))
        self.assertIsNotNone(
            TestFileSystem(
                TESTING_DATE,
                PRODUCTION_BASE_PATH,
                resource_name="",
                mined_hard_delete_age=None,
                mined_soft_delete_age=182,
                snippets_delete_age=365))

    def test_positive(self):
        self.assertIsNotNone(
            TestFileSystem(
                TESTING_DATE,
                TESTING_BASE_PATH,
                resource_name="",
                mined_hard_delete_age=20,
                mined_soft_delete_age=10,
                snippets_delete_age=10))
        self.assertIsNotNone(
            TestFileSystem(
                TESTING_DATE,
                TESTING_BASE_PATH,
                resource_name="",
                mined_hard_delete_age=None,
                mined_soft_delete_age=10,
                snippets_delete_age=10))
