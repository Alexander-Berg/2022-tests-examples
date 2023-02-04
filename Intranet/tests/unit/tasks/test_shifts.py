import datetime
import pytest
import pretend
import responses
import json
from decimal import Decimal

from unittest.mock import patch, call
from requests.exceptions import HTTPError
from contextlib import nullcontext as does_not_raise

from watcher.config import settings
from watcher.logic.timezone import now
from watcher.logic.exceptions import RatingNotFound
from watcher.logic.shift import shift_total_points
from watcher.crud.robot import get_duty_watcher_robot
from watcher.tasks.shift import (
    approve_schedules_shifts,
    finish_shift,
    start_shift,
    start_shifts,
    finish_shifts,
)
from watcher.crud.rating import get_rating_by_schedule_staff
from watcher import enums


@pytest.fixture
def shift_data(shift_factory, staff_factory, schedule_factory):
    schedule = schedule_factory()
    staff = staff_factory()

    create_shift = lambda start=0, end=10, replacement_for_id=None, empty=False, pr=dict(): shift_factory(
        schedule=schedule,
        staff=staff,
        start=now() + datetime.timedelta(hours=start),
        end=now() + datetime.timedelta(hours=end),
        replacement_for_id=replacement_for_id,
        status=enums.ShiftStatus.active,
        empty=empty,
        predicted_ratings=pr
    )
    shift = create_shift()
    shift_with_ss = create_shift(start=10, end=20, pr={staff.login: 0})
    subshift1 = create_shift(10, 15, replacement_for_id=shift_with_ss.id, pr={staff.login: 0})
    subshift2 = create_shift(15, 20, replacement_for_id=shift_with_ss.id, pr={staff.login: 0})
    subshift3 = create_shift(15, 20, replacement_for_id=shift_with_ss.id, pr={staff.login: 0}, empty=True)

    return pretend.stub(
        schedule=schedule,
        staff=staff,
        shift=shift,
        shift_with_ss=shift_with_ss,
        subshift1=subshift1,
        subshift2=subshift2,
        subshift3=subshift3,
    )


@responses.activate
@pytest.mark.parametrize(
    ('success', 'expected'),
    [
        (True, does_not_raise()),
        (False, pytest.raises(HTTPError))
    ]
)
@pytest.mark.parametrize(
    'days_for_notify_of_begin', (
        None, [],
        [datetime.timedelta(days=3), datetime.timedelta(days=7)],
        [datetime.timedelta(days=0)],
        [datetime.timedelta(days=0), datetime.timedelta(days=8)]
    )
)
def test_start_shift_success(shift_data, scope_session, success, expected, days_for_notify_of_begin):
    shift = shift_data.shift
    shift.schedule.days_for_notify_of_begin = days_for_notify_of_begin
    shift.status = enums.ShiftStatus.scheduled
    scope_session.commit()

    assert len(shift.notifications) == 0

    responses.add(
        responses.POST,
        'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
        status=200 if success else 500
    )
    with expected:
        start_shift(shift_id=shift.id)

    assert len(responses.calls) == 1
    expected = {
        'service': shift.schedule.service_id,
        'person': shift.staff.login,
        'role': shift.slot.role_on_duty_id,
        'comment': 'Начало дежурства',
        'silent': True,
    }
    assert json.loads(responses.calls[0].request.body) == expected

    scope_session.refresh(shift)
    expected_state = enums.ShiftStatus.scheduled
    if success:
        expected_state = enums.ShiftStatus.active
        if days_for_notify_of_begin and datetime.timedelta(days=0) in days_for_notify_of_begin:
            assert len(shift.notifications) == 1
            notification = shift.notifications[0]
            assert notification.send_at == shift.start
            assert notification.valid_to == shift.start + datetime.timedelta(minutes=15)
            assert notification.staff == shift.staff
        else:
            assert len(shift.notifications) == 0
    else:
        assert len(shift.notifications) == 0

    assert shift.status == expected_state


@responses.activate
@pytest.mark.parametrize('zero_rating', [True, False])
def test_finish_shift_no_active_roles(
    shift_data, shift_factory, rating_factory,
    scope_session, zero_rating,
):
    shift = shift_data.shift
    staff = shift_data.staff
    shift.next = shift_factory(staff=staff, schedule=shift.schedule, predicted_ratings={staff.login: 5})
    rating_factory(staff=staff, schedule=shift.schedule)

    if zero_rating:
        shift.slot.points_per_hour = 0

    assert shift.status == enums.ShiftStatus.active

    shift.predicted_ratings = {staff.login: 10}
    scope_session.commit()
    finish_shift(shift_id=shift.id)

    assert len(responses.calls) == 0

    scope_session.refresh(shift)
    scope_session.refresh(shift.next)

    assert shift.status == enums.ShiftStatus.completed
    expected_rating = 10
    if zero_rating:
        expected_rating = 0
    assert staff.ratings[0].rating == expected_rating
    assert shift.next.predicted_ratings == {staff.login: expected_rating}


@responses.activate
@pytest.mark.parametrize(
    ('status', 'expected'),
    [
        (200, does_not_raise()),
        (403, does_not_raise()),  # игнорируем 403
        (500, pytest.raises(HTTPError))
    ]
)
def test_finish_shift_with_active_role(shift_data, scope_session, member_factory, status, expected):
    shift = shift_data.shift
    shift.is_role_requested = True
    assert shift.status == enums.ShiftStatus.active
    member = member_factory(
        state=enums.MemberState.active,
        role=shift.slot.role_on_duty,
        staff=shift.staff,
        service=shift.schedule.service,
    )
    responses.add(
        responses.DELETE,
        f'https://abc-back.test.yandex-team.ru/api/frontend/services/members/{member.id}/',
        status=status,
    )
    with expected:
        finish_shift(shift_id=shift.id)

    assert len(responses.calls) == 1

    scope_session.refresh(shift)
    expected_state = enums.ShiftStatus.completed
    if status == 500:
        expected_state = enums.ShiftStatus.active

    assert shift.status == expected_state


def test_finish_shift_with_subshifts(shift_data, composition_participants_factory, scope_session):
    shift = shift_data.shift_with_ss
    shift.status = enums.ShiftStatus.scheduled
    composition_participant = composition_participants_factory(staff=shift.staff)
    shift_data.subshift1.slot.composition_id = composition_participant.composition.id
    composition_participant.composition.service = shift_data.subshift1.schedule.service
    scope_session.add(shift.slot)
    scope_session.commit()
    finish_shift(shift_id=shift_data.subshift1.id)

    scope_session.refresh(shift)
    scope_session.refresh(shift_data.subshift1)
    scope_session.refresh(shift_data.subshift2)

    staff_rating = get_rating_by_schedule_staff(scope_session, shift.schedule_id, shift_data.subshift1.staff_id)

    assert shift_data.subshift1.status == enums.ShiftStatus.completed
    assert shift_data.subshift2.status == enums.ShiftStatus.active
    assert shift.status == enums.ShiftStatus.scheduled
    assert staff_rating.rating == 5


def test_finish_shift_with_inconsistent_shift(
    shift_factory, composition_participants_factory, slot_factory,
    staff_factory, scope_session, schedule_factory, composition_factory
):
    staff = staff_factory(login='rick')
    schedule = schedule_factory()
    composition = composition_factory(service=schedule.service)
    composition_participants = composition_participants_factory(staff=staff, composition=composition)
    slot = slot_factory(composition=composition_participants.composition)
    slot.interval.schedule = schedule
    scope_session.commit()
    s1 = shift_factory(schedule=schedule, slot=slot, staff=staff, status=enums.ShiftStatus.active, predicted_ratings={'rick': 0})
    s2 = shift_factory(schedule=schedule, slot=slot, prev=s1, approved=True, predicted_ratings={'rick': 10})
    s3 = shift_factory(schedule=schedule, slot=slot, prev=s2)

    with patch('watcher.tasks.people_allocation.start_people_allocation.delay') as people_rotation_func:
        finish_shift(shift_id=s1.id)
        people_rotation_func.assert_called_once_with(
            schedules_group_id=schedule.schedules_group_id,
            start_date=s3.start
        )

    scope_session.refresh(s1)
    scope_session.refresh(s2)
    scope_session.refresh(s3)

    assert s1.status == enums.ShiftStatus.completed
    assert get_rating_by_schedule_staff(scope_session, s1.schedule_id, s1.staff_id).rating == 1
    assert s2.predicted_ratings == {'rick': 1}


def test_finish_shift_ghost_staff(shift_data, shift_factory, scope_session):
    shift = shift_data.shift
    shift.next = shift_factory()
    scope_session.add(shift)
    scope_session.commit()
    assert shift.status == enums.ShiftStatus.active

    finish_shift(shift_id=shift.id)
    scope_session.refresh(shift.next)

    with pytest.raises(RatingNotFound):
        get_rating_by_schedule_staff(
            db=scope_session, schedule_id=shift.schedule_id, staff_id=shift.staff_id
        )
    assert shift.next.predicted_ratings == {}


def test_approve_schedules_shifts(watcher_robot, schedule_factory, shift_factory, scope_session):
    robot = get_duty_watcher_robot(scope_session)

    schedule = schedule_factory(pin_shifts=datetime.timedelta(days=7))
    need_approve_shifts = [
        shift_factory(schedule=schedule, start=now() + datetime.timedelta(days=i))
        for i in range(6)
    ]
    not_need_approve_shifts = [
        shift_factory(schedule=schedule, start=now() + datetime.timedelta(days=i))
        for i in range(8, 12)
    ]

    approve_schedules_shifts(schedules_ids=[schedule.id])
    scope_session.expire_all()

    assert all([shift.approved for shift in need_approve_shifts])
    assert all([shift.approved_by == robot for shift in need_approve_shifts])
    assert all([not shift.approved for shift in not_need_approve_shifts])


def test_approve_schedules_shifts_with_subshifts(watcher_robot, schedule_factory, shift_factory, scope_session):
    robot = get_duty_watcher_robot(scope_session)

    schedule = schedule_factory(pin_shifts=datetime.timedelta(hours=1))
    main_shift = shift_factory(schedule=schedule, start=now())

    subshift = shift_factory(
        schedule=schedule,
        start=now() + datetime.timedelta(hours=2),
        replacement_for_id=main_shift.id
    )
    other_shift = shift_factory(schedule=schedule, start=now() + datetime.timedelta(hours=2))

    approve_schedules_shifts(schedules_ids=[schedule.id])
    scope_session.expire_all()
    assert all([shift.approved for shift in (main_shift, subshift)])
    assert all([shift.approved_by == robot for shift in (main_shift, subshift)])
    assert not other_shift.approved


def test_dont_approve_recently_disapproved_shift(watcher_robot, schedule_factory, shift_factory, scope_session):
    get_duty_watcher_robot(scope_session)

    schedule = schedule_factory(pin_shifts=datetime.timedelta(days=7))

    shifts = [shift_factory(schedule=schedule, start=now()) for i in range(6)]
    shifts[0].approved_removed_at = now()
    shifts[1].approved_removed_at = now() - datetime.timedelta(minutes=1)
    shifts[2].approved_removed_at = now() - datetime.timedelta(minutes=5)
    shifts[3].approved_removed_at = now() - datetime.timedelta(seconds=settings.DONT_APPROVE_DISAPPROVED_SHIFTS_FOR)
    shifts[4].approved_removed_at = now() - datetime.timedelta(minutes=20)
    scope_session.commit()

    approve_schedules_shifts(schedules_ids=[schedule.id])
    scope_session.expire_all()

    need_approve_shifts = shifts[3:]
    assert all([shift.approved for shift in need_approve_shifts])


def test_start_shifts(shift_factory):
    shift = shift_factory(
        start=now() + datetime.timedelta(minutes=28)
    )

    # смена начинается более чем через 30 минут
    shift_factory(
        schedule=shift.schedule, slot=shift.slot,
        start=now() + datetime.timedelta(minutes=32)
    )

    # у смены есть подсмены
    shift_with_subshifts = shift_factory(
        start=now() + datetime.timedelta(minutes=28),
        schedule=shift.schedule, slot=shift.slot
    )
    subshift = shift_factory(
        schedule=shift.schedule, slot=shift.slot,
        start=now() + datetime.timedelta(minutes=28),
        replacement_for_id=shift_with_subshifts.id,
    )

    # смена не в запланированном статусе
    shift_factory(
        schedule=shift.schedule, slot=shift.slot,
        start=now() + datetime.timedelta(minutes=28),
        status=enums.ShiftStatus.completed,
    )

    with patch('watcher.tasks.shift.start_shift') as mock_start_shift:
        start_shifts()
    mock_start_shift.delay.assert_has_calls(
        [
            call(shift_id=shift.id),
            call(shift_id=subshift.id),
        ]
    )


def test_finish_shifts(shift_factory):
    shift = shift_factory(
        status=enums.ShiftStatus.active,
        end=now() - datetime.timedelta(minutes=61)
    )

    # смена не в активном статусе
    shift_factory(
        schedule=shift.schedule, slot=shift.slot,
        start=now() - datetime.timedelta(minutes=61)
    )

    # смена закончилась меньше часа назад
    shift_factory(
        start=now() - datetime.timedelta(minutes=35),
        schedule=shift.schedule, slot=shift.slot,
        status=enums.ShiftStatus.active,
    )

    # смена уже завершена
    shift_factory(
        schedule=shift.schedule, slot=shift.slot,
        start=now() + datetime.timedelta(minutes=28),
        status=enums.ShiftStatus.completed,
    )

    with patch('watcher.tasks.shift.finish_shift') as mock_finish_shift:
        finish_shifts()
    mock_finish_shift.delay.assert_called_once_with(shift_id=shift.id)


@responses.activate
@pytest.mark.parametrize('is_member_has_role', (True, False))
def test_shift_is_member_has_role(shift_data, scope_session, is_member_has_role, member_factory):
    shift = shift_data.shift
    shift.status = enums.ShiftStatus.scheduled
    member = member_factory(staff=shift.staff, service=shift.schedule.service)
    if is_member_has_role:
        member.role_id = shift.slot.role_on_duty_id
    scope_session.commit()
    responses.add(
        responses.POST,
        'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
        status=200
    )
    start_shift(shift_id=shift.id)
    scope_session.refresh(shift)
    assert shift.is_role_requested != is_member_has_role

    if not is_member_has_role:
        member.role_id = shift.slot.role_on_duty_id
        scope_session.commit()
    responses.add(
        responses.DELETE,
        f'https://abc-back.test.yandex-team.ru/api/frontend/services/members/{member.id}/',
        status=200
    )
    finish_shift(shift_id=shift.id)
    if is_member_has_role:
        assert len(responses.calls) == 0
    else:
        assert len(responses.calls) == 2


@pytest.mark.parametrize('another_shift', ('sequential', 'parallel', 'another_role', 'another_service'))
def test_finish_shift_without_revoking_role(
    shift_factory, slot_factory, schedule_factory, scope_session,
    staff_factory, role_factory, member_factory, another_shift
):
    slot = slot_factory()
    staff = staff_factory()
    shift = shift_factory(
        staff=staff,
        slot=slot,
        status=enums.ShiftStatus.active,
        schedule=slot.interval.schedule,
        is_role_requested=True,
    )
    member_factory(
        staff=staff,
        role=slot.role_on_duty,
        service=shift.schedule.service,
    )
    shift_new = shift_factory(
        start=shift.end,  # sequential
        end=shift.end + datetime.timedelta(hours=1),
        is_role_requested=False,
        staff=shift.staff,
        slot=shift.slot,
        schedule=shift.schedule,
    )
    if another_shift == 'parallel':
        shift_new.end = shift.end + datetime.timedelta(hours=4)
        shift_new.start = shift.start + datetime.timedelta(minutes=10)
        shift_new.status = enums.ShiftStatus.active
    elif another_shift == 'another_service':
        shift_new.schedule = schedule_factory()
    elif another_shift == 'another_role':
        shift_new.slot = slot_factory()

    scope_session.commit()

    with patch('watcher.logic.member.abc_client.deprive_role') as deprive_role_mock:
        finish_shift(shift_id=shift.id)
    scope_session.refresh(shift_new)

    if another_shift in ('another_role', 'another_service'):
        deprive_role_mock.assert_called_once()
        assert shift_new.is_role_requested is False
    else:
        deprive_role_mock.assert_not_called()
        assert shift_new.is_role_requested is True


@responses.activate
@pytest.mark.parametrize('problem_type', ('nobody_on_duty', 'staff_has_gap'))
@pytest.mark.parametrize('backup_gap', (False, True))
def test_start_shift_backup_takes_primary(
    scope_session, shift_factory, slot_factory, staff_factory, interval_factory, gap_factory, problem_type, backup_gap
):
    slot_primary = slot_factory(interval=interval_factory(backup_takes_primary_shift=True))
    slot_backup = slot_factory(interval=slot_primary.interval, is_primary=False)

    schedule = slot_primary.interval.schedule
    backup_staff = staff_factory()
    shift_primary = shift_factory(slot=slot_primary, schedule=schedule)
    shift_backup = shift_factory(slot=slot_backup, schedule=schedule, is_primary=False, staff=backup_staff)

    if problem_type == 'nobody_on_duty':
        shift_primary.staff = None
    elif problem_type == 'staff_has_gap':
        shift_primary.staff = staff_factory()
        gap_factory(staff=shift_primary.staff, end=now()+datetime.timedelta(days=1))
    if backup_gap:
        gap_factory(staff=backup_staff, end=now()+datetime.timedelta(days=1))

    scope_session.commit()

    responses.add(
        responses.POST,
        'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
        status=200,
    )
    start_shift(shift_id=shift_backup.id)
    start_shift(shift_id=shift_primary.id)

    scope_session.refresh(shift_backup)
    scope_session.refresh(shift_primary)

    if backup_gap:
        assert shift_primary.staff != backup_staff
        assert shift_primary.status == enums.ShiftStatus.active
    else:
        assert shift_primary.staff == backup_staff
        assert shift_primary.status == enums.ShiftStatus.active
        assert shift_backup.gives_rating is False
    assert shift_backup.staff == backup_staff
    assert shift_backup.status == enums.ShiftStatus.active


def test_finish_shift_backup_gets_rating_for_primary(
    scope_session, shift_factory, slot_factory, schedule_factory, interval_factory, rating_factory, staff_factory
):
    slot_primary = slot_factory(
        interval=interval_factory(backup_takes_primary_shift=True),
        points_per_hour=10,
    )
    slot_backup = slot_factory(
        interval=slot_primary.interval, is_primary=False
    )

    schedule = slot_primary.interval.schedule
    shift_primary = shift_factory(
        slot=slot_primary, schedule=schedule,
        staff=staff_factory(), status=enums.ShiftStatus.active,
        start=now(), end=now()+datetime.timedelta(days=3),
    )
    shift_backup = shift_factory(
        slot=slot_backup, schedule=schedule, is_primary=False,
        staff=shift_primary.staff, status=enums.ShiftStatus.active,
        start=now(), end=now() + datetime.timedelta(days=3),
        gives_rating=False,
    )
    rating = rating_factory(schedule=schedule, staff=shift_primary.staff, rating=0.0)

    with patch('watcher.logic.member.abc_client.deprive_role'):
        finish_shift(shift_id=shift_backup.id)
        finish_shift(shift_id=shift_primary.id)

    scope_session.refresh(rating)
    assert rating.rating == Decimal(shift_total_points(shift_primary))


def test_finish_shift_no_role_on_duty(
    scope_session, staff_factory, slot_factory, shift_factory, rating_factory, assert_count_queries
):
    shift = shift_factory(
        staff=staff_factory(),
        slot=slot_factory(role_on_duty=None),
        status=enums.ShiftStatus.active,
    )
    rating = rating_factory(
        schedule=shift.schedule,
        staff=shift.staff,
        rating=Decimal(10.0),
    )

    shift_id = shift.id
    with assert_count_queries(6):
        with patch('watcher.logic.member.abc_client.deprive_role') as func:
            finish_shift(shift_id=shift_id)
        func.assert_not_called()
    scope_session.refresh(rating)
    assert rating.rating == Decimal(11.0)
