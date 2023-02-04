import unittest
import sys
import os
from unittest.mock import patch, MagicMock
sys.modules['config.config'] = MagicMock()
sys.modules['const.const'] = MagicMock()
from utils import utils
from api.testpalm import TestPalmProject, TestPalm


class TestSelectLastRunTimeForEachCaseFromTestrunsInSelectingPeriod(unittest.TestCase):
    @patch.object(TestPalm, 'get_test_runs_cases')
    def test_should_select_not_skipped_cases_from_testruns(self, mock_get_test_run_cases):
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)
        mock_get_test_run_cases.return_value = MockCases.get_one_run_3_cases_different_status()

        cases = testpalm.get_cases_with_last_run_time("iOS", 1625856093892)

        self.assertDictEqual(cases,  {47: 1626856093892, 49: 1626856095618}, "not only skipped cases")

    @patch.object(TestPalm, 'get_test_runs_cases')
    def test_should_select_last_time_for_each_case(self, mock_get_test_run_cases):
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)
        mock_get_test_run_cases.return_value = MockCases.get_two_run_cases_with_different_finish_time()

        cases = testpalm.get_cases_with_last_run_time("iOS", 1625856093892)

        self.assertDictEqual(cases, {47: 1626995682816, 49: 1626995683871}, "not the newest cases run")

    @patch.object(TestPalm, 'get_test_runs_cases')
    def test_should_return_empty_dict_when_no_cases_selected_from_api(
            self, mock_get_test_run_cases):
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)
        mock_get_test_run_cases.return_value = None

        cases = testpalm.get_cases_with_last_run_time("iOS", 1625856093892)

        self.assertDictEqual(cases, {}, "not empty dict")


class TestSelectAllActualCasesWithSpecifiedSeverities(unittest.TestCase):

    @patch.object(TestPalm, 'select_non_archived_cases_with_severity')
    def test_should_select_only_non_automated_cases_for_ios_when_one_severity_specified(
            self,
            mock_select_non_archived_cases_with_severity
    ):
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)
        mock_select_non_archived_cases_with_severity.return_value = MockCases.get_actual_cases_severity_smoke_core()
        non_automated_ios_cases_id = [47, 49]
        selected_cases = []

        cases = testpalm.get_actual_cases_with_severities('iOS', ['Smoke core'])

        for case in cases:
            selected_cases.append(case['id'])
        self.assertListEqual(non_automated_ios_cases_id, sorted(selected_cases))

    @patch.object(TestPalm, 'select_non_archived_cases_with_severity')
    def test_should_select_only_non_automated_cases_for_android_when_one_severity_specified(
            self,
            mock_select_non_archived_cases_with_severity
    ):
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)

        mock_select_non_archived_cases_with_severity.return_value = MockCases.get_actual_cases_severity_smoke_core()
        non_automated_android_cases_id = [39, 49]
        selected_cases = []

        cases = testpalm.get_actual_cases_with_severities('Android', ['Smoke core'])

        for case in cases:
            selected_cases.append(case['id'])
        self.assertListEqual(non_automated_android_cases_id, sorted(selected_cases))

    @patch.object(TestPalm, 'select_non_archived_cases_with_severity')
    def test_should_return_only_non_automated_cases_for_ios_when_multiple_severities_specified(
            self,
            mock_select_non_archived_cases_with_severity
    ):
        def actual_cases_severities(severity):
            if severity == 'Smoke core':
                return MockCases.get_actual_cases_severity_smoke_core()
            elif severity == 'Major':
                return MockCases.get_actual_cases_severity_major()

        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)
        mock_select_non_archived_cases_with_severity.side_effect = actual_cases_severities
        non_automated_ios_cases_id = [47, 49, 51]
        selected_cases = []

        cases = testpalm.get_actual_cases_with_severities('iOS', ['Smoke core', 'Major'])

        for case in cases:
            selected_cases.append(case['id'])
        self.assertListEqual(non_automated_ios_cases_id, sorted(selected_cases))

    @patch.object(TestPalm, 'select_non_archived_cases_with_severity')
    def test_should_return_only_non_automated_cases_for_ios_when_multiple_severities_specified(
            self,
            mock_select_non_archived_cases_with_severity
    ):
        def actual_cases_severities(severity):
            if severity == 'Smoke core':
                return MockCases.get_actual_cases_severity_smoke_core()
            elif severity == 'Major':
                return MockCases.get_actual_cases_severity_major()

        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)
        mock_select_non_archived_cases_with_severity.side_effect = actual_cases_severities
        non_automated_ios_cases_id = [47, 49, 51]
        selected_cases = []

        cases = testpalm.get_actual_cases_with_severities('iOS', ['Smoke core', 'Major'])

        for case in cases:
            selected_cases.append(case['id'])
        self.assertListEqual(non_automated_ios_cases_id, sorted(selected_cases))

    @patch.object(TestPalm, 'select_non_archived_cases_with_severity')
    def test_should_return_empty_list_if_no_cases_got_from_api(
            self,
            mock_select_non_archived_cases_with_severity
    ):
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)
        mock_select_non_archived_cases_with_severity.return_value = []

        cases = testpalm.get_actual_cases_with_severities('iOS', ['Smoke core'])

        self.assertListEqual(cases, [])


class TestEnrichActualCasesWithLastRunTime(unittest.TestCase):

    @patch.object(TestPalm, 'get_actual_cases_with_severities')
    @patch.object(TestPalm, 'get_cases_with_last_run_time')
    def test_should_set_last_run_time_to_zero_when_no_info_about_last_run_time(
            self,
            mock_get_cases_with_last_run_time,
            mock_get_actual_cases_with_severities

    ):
        mock_get_actual_cases_with_severities.return_value = MockCases.get_one_actual_case()
        mock_get_cases_with_last_run_time.return_value = {33: 1626856093892}
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)

        cases = testpalm.get_actual_cases_with_last_run_stat('iOS', 1625856093892, ['Smoke core'])

        for case in cases:
            self.assertEqual(case['id'], 39)
            self.assertEqual(case['lastRunTime'], '0')

    @patch.object(TestPalm, 'get_actual_cases_with_severities')
    @patch.object(TestPalm, 'get_cases_with_last_run_time')
    def test_should_add_last_run_time_into_actual_case_in_lastRunTime_param(
            self,
            mock_get_cases_with_last_run_time,
            mock_get_actual_cases_with_severities

    ):
        mock_get_actual_cases_with_severities.return_value = MockCases.get_one_actual_case()
        mock_get_cases_with_last_run_time.return_value = {39: 1626856093892}
        testpalm = TestPalm(TestPalmProject.PETPROJECT.value)

        cases = testpalm.get_actual_cases_with_last_run_stat('iOS', 1625856093892, ['Smoke core'])

        for case in cases:
            self.assertEqual(case['id'], 39)
            self.assertEqual(case['lastRunTime'], 1626856093892)


class MockCases:

    @staticmethod
    def assets_path(asset_name):
        return os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir, 'assets', asset_name))

    @staticmethod
    def get_actual_cases():
        return utils.get_json_from_file(MockCases.assets_path('actual_cases.json'))

    @staticmethod
    def get_actual_cases_severity_smoke_core():
        return utils.get_json_from_file(MockCases.assets_path('actual_cases.json'))[:5]

    @staticmethod
    def get_actual_cases_severity_major():
        return utils.get_json_from_file(MockCases.assets_path('actual_cases.json'))[-1:]

    @staticmethod
    def get_one_run_3_cases_different_status():
        json_mock = utils.get_json_from_file(MockCases.assets_path('test_run_cases.json'))[:1]
        json_mock[0]['testGroups'] = json_mock[0]['testGroups'][:1]
        return json_mock

    @staticmethod
    def get_two_run_cases_with_different_finish_time():
        json_mock = utils.get_json_from_file(MockCases.assets_path('test_run_cases.json'))[:2]
        json_mock[0]['testGroups'] = json_mock[0]['testGroups'][:1]
        json_mock[1]['testGroups'] = json_mock[1]['testGroups'][:1]
        return json_mock

    @staticmethod
    def get_one_actual_case():
        return utils.get_json_from_file(MockCases.assets_path('actual_cases.json'))[:1]


if __name__ == '__main__':
    unittest.main()
