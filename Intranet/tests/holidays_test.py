from datetime import date

from mock import Mock, patch

from django.conf import settings

from staff.gap.holidays import (
    get_holidays_by_year,
    get_holidays_from_calendar_api,
    OutModes,
)


def test_get_holidays_by_year_cache_miss():
    mocked_cache = Mock()
    mocked_cache.get = Mock(return_value=None)
    mocked_cache.set = Mock()

    year = 2021
    geo_id = 225
    out_mode = OutModes.holidays

    fake_api_response = [
        date(2021, 1, 1),
    ]

    with patch('staff.gap.holidays.cache', mocked_cache):
        with patch('staff.gap.holidays.get_holidays_from_calendar_api', Mock(return_value=fake_api_response)):
            get_holidays_by_year(year=year, geo_id=geo_id, out_mode=out_mode)

            mocked_cache.get.assert_called_once_with(
                settings.HOLIDAYS_CACHE_PREFIX.format(geo_id=geo_id, year=year, out_mode=out_mode),
            )
            mocked_cache.set.assert_called_once_with(
                settings.HOLIDAYS_CACHE_PREFIX.format(geo_id=geo_id, year=year, out_mode=out_mode),
                fake_api_response,
                settings.HOLIDAYS_CACHE_TTL,
            )


def test_get_holidays_by_year_from_cache():
    fake_api_response = [
        date(2021, 1, 1),
    ]

    mocked_cache = Mock()
    mocked_cache.get = Mock(return_value=fake_api_response)
    mocked_cache.set = Mock()
    mocked_get_holidays_from_calendar_api = Mock(return_value=fake_api_response)

    year = 2021
    geo_id = 225
    out_mode = OutModes.holidays

    with patch('staff.gap.holidays.cache', mocked_cache):
        with patch('staff.gap.holidays.get_holidays_from_calendar_api', mocked_get_holidays_from_calendar_api):
            get_holidays_by_year(year=year, geo_id=geo_id, out_mode=out_mode)

            mocked_cache.get.assert_called_once_with(
                settings.HOLIDAYS_CACHE_PREFIX.format(geo_id=geo_id, year=year, out_mode=out_mode),
            )
            mocked_cache.set.assert_not_called()
            mocked_get_holidays_from_calendar_api.assert_not_called()


def test_get_holidays_from_calendar_api_no_transfer():
    geo_id = 225
    year = 2022
    out_mode = OutModes.holidays

    fake_get_holidays_result = {
        geo_id: [
            {
                'date': date(2022, 2, 23),
                'is-holiday': True,
                'day-type': 'holiday',
                'is-transfer': False,
                'holiday-name': 'День защитника Отечества',
            },
            {
                'date': date(2022, 3, 8),
                'is-holiday': True,
                'day-type': 'holiday',
                'is-transfer': False,
                'holiday-name': 'Международный женский день',
            },
            {
                'date': date(2022, 5, 1),
                'is-holiday': True,
                'day-type': 'holiday',
                'is-transfer': False,
                'holiday-name': 'Праздник Весны и Труда',
            },
        ],
    }

    expected_result = [
        date(2022, 2, 23),
        date(2022, 3, 8),
        date(2022, 5, 1),
    ]

    mocked_get_holidays = Mock(return_value=fake_get_holidays_result)

    with patch('staff.gap.holidays.get_holidays', mocked_get_holidays):
        assert get_holidays_from_calendar_api(year=year, geo_id=geo_id, out_mode=out_mode) == expected_result
        mocked_get_holidays.assert_called_once_with(
            start_date=date(year, 1, 1),
            end_date=date(year, 12, 31),
            geo_ids=[geo_id],
            out_mode=out_mode.calendar_out_mode,
        )


def test_get_holidays_from_calendar_api_transfer():
    geo_id = 225
    year = 2022
    out_mode = OutModes.holidays

    fake_get_holidays_result = {
        geo_id: [
            {
                'date': date(2022, 2, 23),
                'is-holiday': True,
                'day-type': 'holiday',
                'is-transfer': False,
                'holiday-name': 'День защитника Отечества',
            },
            {
                'date': date(2022, 3, 5),
                'is-holiday': False,
                'day-type': 'weekday',
                'is-transfer': False,
                'holiday-name': 'Перенос рабочего дня с 7 марта',
            },
            {
                'date': date(2022, 3, 7),
                'is-holiday': True,
                'day-type': 'weekend',
                'is-transfer': False,
                'holiday-name': 'Перенос с субботы 5 марта',
            },
            {
                'date': date(2022, 3, 8),
                'is-holiday': True,
                'day-type': 'holiday',
                'is-transfer': False,
                'holiday-name': 'Международный женский день',
            },
        ],
    }

    expected_result = [
        date(2022, 2, 23),
        date(2022, 3, 8),
    ]

    mocked_get_holidays = Mock(return_value=fake_get_holidays_result)

    with patch('staff.gap.holidays.get_holidays', mocked_get_holidays):
        assert get_holidays_from_calendar_api(year=year, geo_id=geo_id, out_mode=out_mode) == expected_result
        mocked_get_holidays.assert_called_once_with(
            start_date=date(year, 1, 1),
            end_date=date(year, 12, 31),
            geo_ids=[geo_id],
            out_mode=out_mode.calendar_out_mode,
        )
