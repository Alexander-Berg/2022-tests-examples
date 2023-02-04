# coding: utf-8

from datetime import datetime

import mock

from billing.dcs.dcs.utils.common import Struct
from billing.dcs.dcs.monitorings.check_runs import extract_run_on_relative_kwargs, \
    ProcessPeriodReturnCodes, process_mnclose_schedule, parse_cron_string, \
    calculate_expected_start_time, calculate_previous_expected_start_time, \
    process_pycron_schedule, filter_runs

from billing.dcs.tests.utils import BaseTestCase, create_patcher

patch = create_patcher('billing.dcs.dcs.monitorings.check_runs')


class ExtractRunOnRelativeKWArgsTestCase(BaseTestCase):
    def test_arg_passed(self):
        self.assertEqual(extract_run_on_relative_kwargs(
            '/bin/check check_code_name --run-on relative(days=+1)'
        ), {'days': +1})

    def test_arg_not_passed(self):
        self.assertIsNone(extract_run_on_relative_kwargs(
            '/bin/check check_code_name'
        ))


@patch('process_mnclose_month')
class ProcessMNCloseScheduleTestCase(BaseTestCase):
    def test_process_one_month(self, process_mnclose_month_mock):
        # Fill params with random unique values for later assertions
        (schedule, mon_start_time, mon_run_id,
         current_month, current_month_mnclose,
         previous_month, previous_month_mnclose) = range(7)

        process_mnclose_schedule(schedule, mon_start_time, mon_run_id,
                                 current_month, current_month_mnclose,
                                 previous_month, previous_month_mnclose)

        self.check_mock_calls(process_mnclose_month_mock, [mock.call(
            mon_run_id, mon_start_time, schedule, current_month,
            current_month_mnclose
        )])

    def test_check_previous_month(self, process_mnclose_month_mock):
        # Fill params with random unique values for later assertions
        (schedule, mon_start_time, mon_run_id,
         current_month, current_month_mnclose,
         previous_month, previous_month_mnclose) = range(7)

        process_mnclose_month_mock.return_value = \
            ProcessPeriodReturnCodes.check_previous_period

        process_mnclose_schedule(schedule, mon_start_time, mon_run_id,
                                 current_month, current_month_mnclose,
                                 previous_month, previous_month_mnclose)

        self.check_mock_calls(process_mnclose_month_mock, [
            mock.call(mon_run_id, mon_start_time, schedule, current_month,
                      current_month_mnclose),
            mock.call(mon_run_id, mon_start_time, schedule, previous_month,
                      previous_month_mnclose),
        ])


class ParseCronStringTestCase(BaseTestCase):
    def test_day_specified(self):
        self.assertDictEqual(parse_cron_string('1 11 21 * *'),
                             Struct(minute=1, hour=11, day='21'))

    def test_day_not_specified(self):
        self.assertDictEqual(parse_cron_string('1 11 * * *'),
                             Struct(minute=1, hour=11, day='*'))

    def test_concrete_month(self):
        with self.assertRaises(AssertionError):
            parse_cron_string('1 11 21 1 *')

    def test_concrete_day_of_week(self):
        with self.assertRaises(AssertionError):
            parse_cron_string('1 11 21 * 1')

    def test_not_numbers(self):
        with self.assertRaises(ValueError):
            parse_cron_string('a b 21 * *')

    def test_bad_string_format(self):
        with self.assertRaises(ValueError):
            parse_cron_string('bad')


class CalculateExpectedStartTimeTestCase(BaseTestCase):
    def test_day_specified(self):
        self.assertEqual(
            calculate_expected_start_time(
                on_dt=datetime(2015, 3, 5),
                cron=parse_cron_string('10 11 21 * *'),
                run_on_relative_kwargs=None
            ),
            datetime(2015, 3, 21, 11, 10)
        )

    def test_day_not_specified(self):
        self.assertEqual(
            calculate_expected_start_time(
                on_dt=datetime(2015, 3, 5),
                cron=parse_cron_string('10 11 * * *'),
                run_on_relative_kwargs=None
            ),
            datetime(2015, 3, 5, 11, 10)
        )

    def test_run_on_specified(self):
        self.assertEqual(
            calculate_expected_start_time(
                on_dt=datetime(2015, 3, 5),
                cron=parse_cron_string('10 11 20-31 * *'),
                run_on_relative_kwargs={'days': +2}
            ),
            datetime(2015, 3, 7, 11, 10)
        )


class CalculatePreviousExpectedStartTimeTestCase(BaseTestCase):
    def test_day_specified(self):
        self.assertEqual(
            calculate_previous_expected_start_time(
                cron=parse_cron_string('10 11 21 * *'),
                current_expected_start_time=datetime(2015, 3, 21, 11, 10),
                run_on_relative_kwargs=None
            ),
            datetime(2015, 2, 21, 11, 10)
        )

    def test_day_not_specified(self):
        self.assertEqual(
            calculate_previous_expected_start_time(
                cron=parse_cron_string('10 11 * * *'),
                current_expected_start_time=datetime(2015, 3, 21, 11, 10),
                run_on_relative_kwargs=None
            ),
            datetime(2015, 3, 20, 11, 10)
        )

    def test_run_on_specified(self):
        self.assertEqual(
            calculate_previous_expected_start_time(
                cron=parse_cron_string('10 11 20-31 * *'),
                current_expected_start_time=datetime(2015, 4, 30, 11, 10),
                run_on_relative_kwargs={'days': -5}
            ),
            datetime(2015, 3, 25, 11, 10)
        )


@patch('load_pycron_task_info')
@patch('process_pycron_period')
class ProcessPycronScheduleTestCase(BaseTestCase):
    def test_check_one_period(self, process_pycron_period_mock,
                              load_pycron_task_info_mock):
        schedule = mock.Mock()
        mon_start_time = datetime.now()
        mon_run_id = 1
        cron_string = '1 1 1 * *'
        command = '/bin/check check_code_name'
        load_pycron_task_info_mock.return_value = (cron_string, command)
        expected_start_time = calculate_expected_start_time(
            mon_start_time,
            cron=parse_cron_string(cron_string),
            run_on_relative_kwargs=extract_run_on_relative_kwargs(command)
        )

        process_pycron_schedule(schedule, mon_start_time, mon_run_id)
        self.check_mock_calls(process_pycron_period_mock, [mock.call(
            schedule, mon_start_time, mon_run_id, expected_start_time
        )])

    def test_check_previous_period(self, process_pycron_period_mock,
                                   load_pycron_task_info_mock):
        schedule = mock.Mock()
        mon_start_time = datetime.now()
        mon_run_id = 1
        cron_string = '1 1 1 * *'
        command = '/bin/check check_code_name --run-on "relative(day=5)"'
        load_pycron_task_info_mock.return_value = (cron_string, command)
        cron = parse_cron_string(cron_string)
        run_on_relative_kwargs = extract_run_on_relative_kwargs(command)
        expected_start_time = calculate_expected_start_time(
            mon_start_time, cron, run_on_relative_kwargs
        )
        process_pycron_period_mock.return_value = \
            ProcessPeriodReturnCodes.check_previous_period

        process_pycron_schedule(schedule, mon_start_time, mon_run_id)
        self.check_mock_calls(process_pycron_period_mock, [
            mock.call(schedule, mon_start_time, mon_run_id,
                      expected_start_time),
            mock.call(schedule, mon_start_time, mon_run_id,
                      calculate_previous_expected_start_time(
                          cron, expected_start_time, run_on_relative_kwargs
                      ))
        ])


@patch('is_test_run')
class TestFilterRuns(BaseTestCase):
    def test(self, is_test_run_mock):
        is_test_run_mock.side_effect = lambda run: run.is_test
        runs = [Struct(id=1, is_test=False),
                Struct(id=2, is_test=True)]
        self.assertListEqual(
            list(filter_runs(runs)),
            [runs[0]]
        )
