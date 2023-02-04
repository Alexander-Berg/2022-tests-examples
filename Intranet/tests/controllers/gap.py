import datetime
from mock import Mock, patch

from staff.person_profile.controllers.gap import get_calendar_gaps


def fake_gap_ctl(gap):
    mocked_ctl = Mock()
    mocked_ctl.find_gaps = Mock(return_value=[gap])
    mocked_ctl_class = Mock(return_value=mocked_ctl)

    return mocked_ctl_class


@patch('staff.person_profile.controllers.gap._enrich_gaps', Mock(return_value=None))
def test_get_calendar_gaps_vacation():
    gap = {
        'workflow': 'vacation',
        'gap_type': 'vacation',
        'created_at': datetime.datetime(2021, 11, 18, 12, 0, 0),
        'created_by_id': 12345,
        'created_by_uid': '112000000012345',
        'person_id': 12345,
        'person_login': 'kirk',
        'date_from': datetime.datetime(2021, 1, 14),
        'date_to': datetime.datetime(2021, 2, 2),
        'full_day': True,
        'state': 'new',
        'work_in_absence': False,
        'comment': 'test_qwerty',
        'to_notify': [],
        'hactions': {},
        'periodic_gap_id': None,
        'is_selfpaid': False,
        'countries_to_visit': [],
        'id': 10001,
        'log': [],
        'modified_by_id': 12345,
        'modified_at': datetime.datetime(2021, 11, 26, 12, 0, 0),
        'last_day_when_commented_vacation_approval_reminder': None,
    }

    expected_result = [
        {
            'type': 'vacation',
            'from': datetime.datetime(2021, 1, 14),
            'to': datetime.datetime(2021, 2, 1),
            'fullDay': True,
            'meta': {
                'work_in_absence': False,
                'id': 10001,
                'comment': 'test_qwerty',
                'show_delete_button': True,
                'show_edit_button': True,
                'show_external_link_button': False,
                'periodic_gap_id': None,
                'mandatory': False,
                'vacation_updated': False,
                'deadline': None,
            },
        },
    ]

    with patch('staff.person_profile.controllers.gap.GapCtl', fake_gap_ctl(gap)):
        assert get_calendar_gaps(
            datetime.datetime(2020, 12, 31),
            datetime.datetime(2021, 12, 31),
            'kirk',
        ) == expected_result


@patch('staff.person_profile.controllers.gap._enrich_gaps', Mock(return_value=None))
def test_get_calendar_gaps_mandatory_vacation():
    gap = {
        'workflow': 'vacation',
        'gap_type': 'vacation',
        'created_at': datetime.datetime(2021, 11, 18, 12, 0, 0),
        'created_by_id': 12345,
        'created_by_uid': '112000000012345',
        'person_id': 12345,
        'person_login': 'kirk',
        'date_from': datetime.datetime(2021, 1, 14),
        'date_to': datetime.datetime(2021, 2, 2),
        'full_day': True,
        'state': 'new',
        'work_in_absence': False,
        'comment': 'test_qwerty',
        'to_notify': [],
        'hactions': {},
        'periodic_gap_id': None,
        'is_selfpaid': False,
        'countries_to_visit': [],
        'id': 10001,
        'log': [],
        'modified_by_id': 12345,
        'modified_at': datetime.datetime(2021, 11, 26, 12, 0, 0),
        'last_day_when_commented_vacation_approval_reminder': None,
        'mandatory': True,
        'vacation_updated': False,
        'deadline': None,
    }

    expected_result = [
        {
            'type': 'vacation',
            'from': datetime.datetime(2021, 1, 14),
            'to': datetime.datetime(2021, 2, 1),
            'fullDay': True,
            'meta': {
                'work_in_absence': False,
                'id': 10001,
                'comment': 'test_qwerty',
                'show_delete_button': False,
                'show_edit_button': True,
                'show_external_link_button': False,
                'periodic_gap_id': None,
                'mandatory': True,
                'vacation_updated': False,
                'deadline': None,
            },
        },
    ]

    with patch('staff.person_profile.controllers.gap.GapCtl', fake_gap_ctl(gap)):
        assert get_calendar_gaps(
            datetime.datetime(2020, 12, 31),
            datetime.datetime(2021, 12, 31),
            'kirk',
        ) == expected_result


@patch('staff.person_profile.controllers.gap._enrich_gaps', Mock(return_value=None))
def test_get_calendar_gaps_duty():
    gap = {
        'workflow': 'duty',
        'gap_type': 'duty',
        'created_at': datetime.datetime(2021, 9, 15, 0, 0, 0),
        'created_by_id': 12345,
        'created_by_uid': '112000000012345',
        'person_id': 12345,
        'person_login': 'kirk',
        'date_from': datetime.datetime(2021, 9, 22),
        'date_to': datetime.datetime(2021, 9, 23),
        'full_day': True,
        'state': 'new',
        'work_in_absence': True,
        'comment': '',
        'to_notify': [],
        'hactions': {},
        'periodic_gap_id': None,
        'service_slug': 'allkindsofduty',
        'service_name': 'Все виды дежурств',
        'shift_id': 21317944,
        'role_on_duty': {
            'name': {
                'ru': 'Функциональный тестировщик',
                'en': 'Functional tester',
            },
        },
        'id': 10003,
        'log': [],
        'modified_by_id': 12345,
        'modified_at': datetime.datetime(2021, 9, 15, 0, 0, 0),
    }

    expected_result = [
        {
            'type': 'duty',
            'from': datetime.datetime(2021, 9, 22),
            'to': datetime.datetime(2021, 9, 22),
            'fullDay': True,
            'meta': {
                'work_in_absence': True,
                'id': 10003,
                'comment': '',
                'show_delete_button': False,
                'show_edit_button': False,
                'show_external_link_button': True,
                'periodic_gap_id': None,
                'service_slug': 'allkindsofduty',
                'service_name': 'Все виды дежурств',
                'shift_id': 21317944,
                'role_on_duty': {
                    'name': {
                        'ru': 'Функциональный тестировщик',
                        'en': 'Functional tester',
                    },
                },
            },
        },
    ]

    with patch('staff.person_profile.controllers.gap.GapCtl', fake_gap_ctl(gap)):
        assert get_calendar_gaps(
            datetime.datetime(2020, 12, 31),
            datetime.datetime(2021, 12, 31),
            'kirk',
        ) == expected_result


@patch('staff.person_profile.controllers.gap._enrich_gaps', Mock(return_value=None))
def test_get_calendar_gaps_trip():
    gap = {
        'workflow': 'trip',
        'gap_type': 'trip',
        'created_at': datetime.datetime(2021, 11, 30, 18, 0, 0),
        'created_by_id': 12345,
        'created_by_uid': '112000000012345',
        'person_id': 12345,
        'person_login': 'kirk',
        'date_from': datetime.datetime(2021, 12, 2),
        'date_to': datetime.datetime(2021, 12, 5),
        'full_day': True,
        'state': 'new',
        'work_in_absence': True,
        'comment': 'Москва – Санкт-Петербург – Сочи – Москва\n',
        'to_notify': [],
        'hactions': {},
        'periodic_gap_id': None,
        'master_issue': 'TRAVEL-12345',
        'slave_issues': ['TRAVEL-12345'],
        'form_key': 'some-form-key',
        'id': 10004,
        'log': [],
        'modified_by_id': 12345,
        'modified_at': datetime.datetime(2021, 11, 30, 18, 0, 0),
    }

    expected_result = [
        {
            'type': 'trip',
            'from': datetime.datetime(2021, 12, 2),
            'to': datetime.datetime(2021, 12, 4),
            'fullDay': True,
            'meta': {
                'work_in_absence': True,
                'id': 10004,
                'comment': 'Москва – Санкт-Петербург – Сочи – Москва\n',
                'show_delete_button': False,
                'show_edit_button': True,
                'show_external_link_button': False,
                'periodic_gap_id': None,
            },
        },
    ]

    with patch('staff.person_profile.controllers.gap.GapCtl', fake_gap_ctl(gap)):
        assert get_calendar_gaps(
            datetime.datetime(2020, 12, 31),
            datetime.datetime(2021, 12, 31),
            'kirk',
        ) == expected_result


@patch('staff.person_profile.controllers.gap._enrich_gaps', Mock(return_value=None))
def test_get_calendar_gaps_remote_work():
    gap = {
        'type': 'remote_work',
        'from': datetime.datetime(2021, 8, 16),
        'to': datetime.datetime(2021, 8, 16),
        'fullDay': True,
        'meta': {
            'work_in_absence': True,
            'id': 10002,
            'comment': 'Тест',
            'show_delete_button': True,
            'show_edit_button': True,
            'show_external_link_button': False,
            'periodic_gap_id': 59,
        },
    }

    expected_result = [
        {
            'type': 'remote_work',
            'from': datetime.datetime(2021, 8, 16),
            'to': datetime.datetime(2021, 8, 16),
            'fullDay': True,
            'meta': {
                'work_in_absence': True,
                'id': 10002,
                'comment': 'Тест',
                'show_delete_button': True,
                'show_edit_button': True,
                'show_external_link_button': False,
                'periodic_gap_id': 59,
            },
        },
    ]

    with patch('staff.person_profile.controllers.gap.GapCtl', fake_gap_ctl(gap)):
        assert get_calendar_gaps(
            datetime.datetime(2020, 12, 31),
            datetime.datetime(2021, 12, 31),
            'kirk',
        ) == expected_result
