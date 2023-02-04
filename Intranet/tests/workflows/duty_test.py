import datetime
from itertools import chain
from unittest.mock import MagicMock, ANY

from mock import Mock, patch, call
import pytest

from django.conf import settings

from staff.gap.workflows.choices import GAP_STATES as GS
from staff.gap.workflows.duty.workflow import DutyWorkflow
from staff.gap.workflows.duty.sync import fetch_schedules, sync_from_abc, _fetch_shifts, ABC_FIELDS
from staff.lib.testing import StaffFactory, ctx_combine
from staff.person.models import Staff

abc_schedules_responses = {
        f'{settings.ABC_URL}/api/v4/duty/schedules-cursor/': {
            'next': 'next_page_url_1',
            'previous': None,
            'results': [
                {
                    'id': 1001,
                    'role': {'name': {'ru': 'DevOps', 'en': 'DevOps'}},
                    'service': {
                        'slug': 'test-api-postman-cyan-ivory-olive-teal',
                        'name': {'ru': 'ABC-10458', 'en': 'ABC-10458'},
                    },
                    'show_in_staff': False,
                },
            ],
        },
        'next_page_url_1': {
            'next': 'next_page_url_2',
            'previous': f'{settings.ABC_URL}/api/v4/duty/schedules-cursor/',
            'results': [
                {
                    'id': 1002,
                    'role': {'name': {'ru': 'DevOps', 'en': 'DevOps'}},
                    'service': {
                        'slug': 'test-api-postman-cyan-ivory-olive-teal',
                        'name': {'ru': 'ABC-10458', 'en': 'ABC-10458'},
                    },
                    'show_in_staff': False,
                },
            ],
        },
        'next_page_url_2': {
            'next': None,
            'previous': 'next_page_url_1',
            'results': [
                {
                    'id': 1003,
                    'role': {'name': {'ru': 'DevOps', 'en': 'DevOps'}},
                    'service': {
                        'slug': 'test-api-postman-cyan-ivory-olive-teal',
                        'name': {'ru': 'ABC-10458', 'en': 'ABC-10458'},
                    },
                    'show_in_staff': False,
                },
            ],
        },
    }


def abc_schedules_requests_get(url, *args, **kwargs):
    mocked_response = Mock()
    mocked_response.json = Mock(return_value=abc_schedules_responses[url])
    return mocked_response


@pytest.mark.django_db
def test_new_gap(gap_test):
    now = gap_test.mongo_now()

    base_gap = gap_test.get_base_gap(DutyWorkflow)

    gap = DutyWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_gap(base_gap)

    gap_test.base_assert_new_gap(DutyWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert gap['state'] == GS.NEW
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID


@patch('staff.gap.workflows.duty.sync.tvm2.get_tvm_ticket_by_deploy', Mock(return_value='ticket'))
def test_fetch_schedules():
    with patch('staff.gap.workflows.duty.sync.requests.get', abc_schedules_requests_get):
        results = chain(*[res['results'] for res in abc_schedules_responses.values()])
        expected_result = {
            sch['id']: sch
            for sch in results
        }
        assert fetch_schedules() == expected_result


abc_schedule = {
    'id': 1,
    'show_in_staff': True,
    'service': {'slug': 'test', 'name': None},
    'role': None,
}
base_abc_shift = {
    'schedule': abc_schedule,
    'is_approved': True,
}

base_workflow_call = {
    'service_slug': 'test',
    'service_name': '',
    'role_on_duty': '',
    'work_in_absence': True
}

abc_time_format = '%Y-%m-%d %H:%M:%S+03:00'
moscow_tz = datetime.timezone(datetime.timedelta(hours=3))


def _form_abc_shift(person: Staff, start_time, end_time, shift_id=1, replaces=None):
    shift = dict(
        id=shift_id,
        person={'login': person.login, 'id': person.id},
        start_datetime=start_time.strftime(abc_time_format),
        end_datetime=end_time.strftime(abc_time_format),
        **base_abc_shift,
    )
    if replaces is not None:
        shift['replaces'] = replaces
    return shift


@pytest.mark.django_db
def test_abc_sync_full_day(gap_test):
    person = StaffFactory()
    start_time = datetime.datetime.now(tz=moscow_tz).replace(hour=0, minute=0, second=0, microsecond=0)
    end_time = start_time + datetime.timedelta(days=2)
    abc_shift = _form_abc_shift(person=person, start_time=start_time, end_time=end_time, replaces=[])

    mocked_new_gap = MagicMock()
    mocked_schedules = Mock(return_value={abc_schedule['id']: abc_schedule})
    with ctx_combine(
        patch('staff.gap.workflows.duty.sync.fetch_schedules', mocked_schedules),
        patch('staff.gap.workflows.duty.sync._fetch_shifts', Mock(return_value=[abc_shift])),
        patch('staff.gap.workflows.duty.workflow.DutyWorkflow.new_gap', mocked_new_gap)
    ):
        sync_from_abc()

    mocked_new_gap.assert_called_once_with(
        dict(
            shift_id=abc_shift['id'],
            person_login=person.login,
            person_id=person.id,
            date_from=start_time.replace(tzinfo=None),
            date_to=end_time.replace(tzinfo=None),
            full_day=True,
            **base_workflow_call,
        ),
    )


@pytest.mark.django_db
def test_abc_sync_partial(gap_test):
    person = StaffFactory()
    start_time = datetime.datetime.now(tz=moscow_tz).replace(hour=12, minute=0, second=0, microsecond=0)
    end_time = start_time + datetime.timedelta(days=2)
    abc_shift = _form_abc_shift(person=person, start_time=start_time, end_time=end_time, replaces=[])

    mocked_new_gap = MagicMock()
    mocked_schedules = Mock(return_value={abc_schedule['id']: abc_schedule})
    with ctx_combine(
        patch('staff.gap.workflows.duty.sync.fetch_schedules', mocked_schedules),
        patch('staff.gap.workflows.duty.sync._fetch_shifts', Mock(return_value=[abc_shift])),
        patch('staff.gap.workflows.duty.workflow.DutyWorkflow.new_gap', mocked_new_gap)
    ):
        sync_from_abc()

    mocked_new_gap.assert_called_once_with(
        dict(
            shift_id=abc_shift['id'],
            person_login=person.login,
            person_id=person.id,
            date_from=start_time,
            date_to=end_time,
            full_day=False,
            **base_workflow_call,
        ),
    )


@pytest.mark.django_db
def test_abc_sync_partial_with_replaces(gap_test):
    person = StaffFactory()
    other_person = StaffFactory()
    start_time = datetime.datetime.now(tz=moscow_tz).replace(hour=12, minute=0, second=0, microsecond=0)
    end_time = start_time + datetime.timedelta(days=5)
    replace_start_time = start_time + datetime.timedelta(days=1)
    replace_end_time = replace_start_time + datetime.timedelta(hours=5)
    abc_shift = _form_abc_shift(
        person=person,
        start_time=start_time,
        end_time=end_time,
        shift_id=1,
        replaces=[
            _form_abc_shift(person=other_person, start_time=replace_start_time, end_time=replace_end_time, shift_id=2),
        ]
    )

    mocked_new_gap = MagicMock()
    mocked_schedules = Mock(return_value={abc_schedule['id']: abc_schedule})
    with ctx_combine(
        patch('staff.gap.workflows.duty.sync.fetch_schedules', mocked_schedules),
        patch('staff.gap.workflows.duty.sync._fetch_shifts', Mock(return_value=[abc_shift])),
        patch('staff.gap.workflows.duty.workflow.DutyWorkflow.new_gap', mocked_new_gap)
    ):
        sync_from_abc()

    expected_calls = [
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=start_time,
                date_to=replace_start_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=replace_end_time,
                date_to=end_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['replaces'][0]['id'],
                person_login=other_person.login,
                person_id=other_person.id,
                date_from=replace_start_time,
                date_to=replace_end_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
    ]
    mocked_new_gap.assert_has_calls(calls=expected_calls, any_order=True)


@pytest.mark.django_db
def test_abc_sync_full_with_replaces(gap_test):
    person = StaffFactory()
    other_person = StaffFactory()
    start_time = datetime.datetime.now(tz=moscow_tz).replace(hour=0, minute=0, second=0, microsecond=0)
    end_time = start_time + datetime.timedelta(days=5)
    replace_start_time = start_time + datetime.timedelta(days=1)
    replace_end_time = replace_start_time + datetime.timedelta(days=2)
    abc_shift = _form_abc_shift(
        person=person,
        start_time=start_time,
        end_time=end_time,
        shift_id=1,
        replaces=[
            _form_abc_shift(person=other_person, start_time=replace_start_time, end_time=replace_end_time, shift_id=2),
        ]
    )

    mocked_new_gap = MagicMock()
    mocked_schedules = Mock(return_value={abc_schedule['id']: abc_schedule})
    with ctx_combine(
        patch('staff.gap.workflows.duty.sync.fetch_schedules', mocked_schedules),
        patch('staff.gap.workflows.duty.sync._fetch_shifts', Mock(return_value=[abc_shift])),
        patch('staff.gap.workflows.duty.workflow.DutyWorkflow.new_gap', mocked_new_gap)
    ):
        sync_from_abc()

    expected_calls = [
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=start_time.replace(tzinfo=None),
                date_to=replace_start_time.replace(tzinfo=None),
                full_day=True,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=replace_end_time.replace(tzinfo=None),
                date_to=end_time.replace(tzinfo=None),
                full_day=True,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['replaces'][0]['id'],
                person_login=other_person.login,
                person_id=other_person.id,
                date_from=replace_start_time.replace(tzinfo=None),
                date_to=replace_end_time.replace(tzinfo=None),
                full_day=True,
                **base_workflow_call,
            ),
        ),
    ]
    mocked_new_gap.assert_has_calls(calls=expected_calls, any_order=True)


@pytest.mark.django_db
def test_abc_sync_partial_with_full_replaces(gap_test):
    person = StaffFactory()
    other_person = StaffFactory()
    start_time = datetime.datetime.now(tz=moscow_tz).replace(hour=12, minute=0, second=0, microsecond=0)
    end_time = start_time + datetime.timedelta(days=5)
    replace_start_time = (start_time + datetime.timedelta(days=1)).replace(hour=0)
    replace_end_time = replace_start_time + datetime.timedelta(days=2)
    abc_shift = _form_abc_shift(
        person=person,
        start_time=start_time,
        end_time=end_time,
        shift_id=1,
        replaces=[
            _form_abc_shift(person=other_person, start_time=replace_start_time, end_time=replace_end_time, shift_id=2),
        ]
    )

    mocked_new_gap = MagicMock()
    mocked_schedules = Mock(return_value={abc_schedule['id']: abc_schedule})
    with ctx_combine(
        patch('staff.gap.workflows.duty.sync.fetch_schedules', mocked_schedules),
        patch('staff.gap.workflows.duty.sync._fetch_shifts', Mock(return_value=[abc_shift])),
        patch('staff.gap.workflows.duty.workflow.DutyWorkflow.new_gap', mocked_new_gap)
    ):
        sync_from_abc()

    expected_calls = [
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=start_time,
                date_to=replace_start_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=replace_end_time,
                date_to=end_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['replaces'][0]['id'],
                person_login=other_person.login,
                person_id=other_person.id,
                date_from=replace_start_time.replace(tzinfo=None),
                date_to=replace_end_time.replace(tzinfo=None),
                full_day=True,
                **base_workflow_call,
            ),
        ),
    ]
    mocked_new_gap.assert_has_calls(calls=expected_calls, any_order=True)


@pytest.mark.django_db
def test_abc_sync_full_with_partial_replaces(gap_test):
    person = StaffFactory()
    other_person = StaffFactory()
    start_time = datetime.datetime.now(tz=moscow_tz).replace(hour=0, minute=0, second=0, microsecond=0)
    end_time = start_time + datetime.timedelta(days=5)
    replace_start_time = (start_time + datetime.timedelta(days=1)).replace(hour=12)
    replace_end_time = replace_start_time + datetime.timedelta(days=2)
    abc_shift = _form_abc_shift(
        person=person,
        start_time=start_time,
        end_time=end_time,
        shift_id=1,
        replaces=[
            _form_abc_shift(person=other_person, start_time=replace_start_time, end_time=replace_end_time, shift_id=2),
        ]
    )

    mocked_new_gap = MagicMock()
    mocked_schedules = Mock(return_value={abc_schedule['id']: abc_schedule})

    with ctx_combine(
        patch('staff.gap.workflows.duty.sync.fetch_schedules', mocked_schedules),
        patch('staff.gap.workflows.duty.sync._fetch_shifts', Mock(return_value=[abc_shift])),
        patch('staff.gap.workflows.duty.workflow.DutyWorkflow.new_gap', mocked_new_gap)
    ):
        sync_from_abc()

    expected_calls = [
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=start_time,
                date_to=replace_start_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['id'],
                person_login=person.login,
                person_id=person.id,
                date_from=replace_end_time,
                date_to=end_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
        call(
            dict(
                shift_id=abc_shift['replaces'][0]['id'],
                person_login=other_person.login,
                person_id=other_person.id,
                date_from=replace_start_time,
                date_to=replace_end_time,
                full_day=False,
                **base_workflow_call,
            ),
        ),
    ]
    mocked_new_gap.assert_has_calls(calls=expected_calls, any_order=True)


@patch('staff.lib.tvm2.get_tvm_ticket_by_deploy', Mock(return_value=1))
def test_fetch_shifts():
    local_base_abc_shift = {
        'start_datetime': '',
        'end_datetime': '',
        'person': {'login': '', 'id': 1},
        'replaces': [],
    }
    shift1 = dict(
        id=1,
        **base_abc_shift,
        **local_base_abc_shift,
    )
    shift2 = dict(
        id=2,
        **base_abc_shift,
        **local_base_abc_shift,
    )
    shift3 = dict(
        id=3,
        **base_abc_shift,
        **local_base_abc_shift,
    )
    abc_responses = [
        {'results': [shift1, shift2]},
        {'results': [shift1, shift2, shift3]},
        {'results': [shift3]},
        {'results': []},
    ]
    base_expected_call = {
        'url': ANY,
        'headers': ANY,
        'timeout': ANY,
    }
    base_params = {
        'fields': ','.join(ABC_FIELDS),
        'service': 1,
        'with_watcher': 1,
    }
    date_start = datetime.datetime.utcnow().date()
    expected_calls = [
        call(
            params=dict(
                date_from=date_start.strftime('%Y-%m-%d'),
                date_to=(date_start + datetime.timedelta(7)).strftime('%Y-%m-%d'),
                **base_params,
            ),
            **base_expected_call,
        ),
        call().json(),
        call(
            params=dict(
                date_from=(date_start + datetime.timedelta(7)).strftime('%Y-%m-%d'),
                date_to=(date_start + datetime.timedelta(14)).strftime('%Y-%m-%d'),
                **base_params,
            ),
            **base_expected_call,
        ),
        call().json(),
        call(
            params=dict(
                date_from=(date_start + datetime.timedelta(14)).strftime('%Y-%m-%d'),
                date_to=(date_start + datetime.timedelta(21)).strftime('%Y-%m-%d'),
                **base_params,
            ),
            **base_expected_call,
        ),
        call().json(),
        call(
            params=dict(
                date_from=(date_start + datetime.timedelta(21)).strftime('%Y-%m-%d'),
                date_to=(date_start + datetime.timedelta(28)).strftime('%Y-%m-%d'),
                **base_params,
            ),
            **base_expected_call,
        ),
        call().json(),
    ]

    mocked_get = MagicMock()
    mocked_get.return_value.json = MagicMock(side_effect=abc_responses)

    with patch('staff.gap.workflows.duty.sync.requests.get', mocked_get):
        result = _fetch_shifts(service_id=1)

    mocked_get.assert_has_calls(calls=expected_calls, any_order=False)
    assert result == [shift1, shift2, shift3]
