import datetime

from unittest.mock import patch
from freezegun import freeze_time

from watcher.tasks import upload_duty_to_yt


@freeze_time('2021-03-23')
def test_upload_shifts_to_yt(staff_factory, shift_factory):
    yesterday = datetime.date.today() - datetime.timedelta(days=1)
    yesterday_dt = datetime.datetime.fromisoformat(yesterday.isoformat())

    staff = staff_factory()
    shift = shift_factory(
        staff=staff,
        start=yesterday,
        end=yesterday_dt + datetime.timedelta(hours=1),
    )

    # без стаффа - не должно быть в выгрузке
    shift_factory(
        schedule=shift.schedule,
        slot=shift.slot,
        start=yesterday,
        end=yesterday_dt + datetime.timedelta(hours=1),
    )

    # есть замены не должно быть в выгрузке
    shift_with_replace = shift_factory(
        schedule=shift.schedule,
        slot=shift.slot,
        staff=staff_factory(),
        start=yesterday,
        end=yesterday_dt + datetime.timedelta(hours=2),
    )
    shift_2 = shift_factory(
        replacement_for=shift_with_replace,
        staff=shift.staff,
        start=yesterday,
        end=yesterday_dt + datetime.timedelta(hours=5),
    )

    # смена в другой день
    shift_factory(
        staff=shift.staff,
        start=yesterday_dt + datetime.timedelta(days=2),
        end=yesterday_dt + datetime.timedelta(days=3),
    )
    with patch('watcher.tasks.statistics._run_yql') as mock_run:
        upload_duty_to_yt()
    mock_run.assert_called_once_with(
        columns=(
            'service_slug', 'schedule_slug', 'service_id', 'schedule_id',
            'staff_login', 'staff_id',
            'is_primary', 'is_weekend', 'is_holiday',
            'duty_hours', 'current_date',
        ),
        table='home/abc/duty2/shifts/2021-03-22',
        items={
            (
                shift.schedule.service.slug, shift.schedule.slug,
                shift.schedule.service_id, shift.schedule.id,
                shift.staff.login, shift.staff_id, True, False, False, 1, '2021-03-22',
            ),
            (
                shift_2.schedule.service.slug, shift_2.schedule.slug,
                shift_2.schedule.service_id, shift_2.schedule_id,
                shift_2.staff.login, shift_2.staff_id, True, False, False, 5, '2021-03-22',
            ),
        }
    )


@freeze_time('2021-03-23')
def test_upload_gaps_to_yt(staff_factory, gap_factory):
    yesterday = datetime.date.today() - datetime.timedelta(days=1)
    gap = gap_factory(
        start=yesterday - datetime.timedelta(days=4),
        end=yesterday + datetime.timedelta(days=4),
        full_day=True,
    )

    gap_1 = gap_factory(
        start=datetime.datetime.fromisoformat(yesterday.isoformat()) + datetime.timedelta(hours=5),
        end=datetime.datetime.fromisoformat(yesterday.isoformat()) + datetime.timedelta(hours=7),
    )

    # не должен быть в выгрузке
    gap_factory(
        start=yesterday + datetime.timedelta(days=2),
        end=yesterday + datetime.timedelta(days=5),
    )

    with patch('watcher.tasks.statistics._run_yql') as mock_run:
        upload_duty_to_yt()
    mock_run.assert_called_once_with(
        columns=(
            'staff_login',
            'staff_id',
            'is_weekend',
            'is_holiday',
            'gap_hours',
            'current_date',
        ),
        table='home/abc/duty2/gaps/2021-03-22',
        items={
            (gap.staff.login, gap.staff_id, False, False, 24, '2021-03-22'),
            (gap_1.staff.login, gap_1.staff_id, False, False, 2, '2021-03-22')
        }
    )


@freeze_time('2021-03-23')
def test_upload_manual_gaps_to_yt(
    schedule_factory, service_factory, scope_session,
    manual_gap_factory
):
    yesterday = datetime.date.today() - datetime.timedelta(days=1)
    schedule = schedule_factory()
    gap = manual_gap_factory(
        start=yesterday - datetime.timedelta(days=4),
        end=yesterday + datetime.timedelta(days=4),
    )
    gap.gap_settings.schedules.append(schedule)

    gap_1 = manual_gap_factory(
        start=datetime.datetime.fromisoformat(yesterday.isoformat()) + datetime.timedelta(hours=5),
        end=datetime.datetime.fromisoformat(yesterday.isoformat()) + datetime.timedelta(hours=7),
    )
    gap_1.gap_settings.services.append(schedule.service)

    scope_session.commit()

    # не должен быть в выгрузке
    manual_gap_factory(
        start=yesterday + datetime.timedelta(days=2),
        end=yesterday + datetime.timedelta(days=5),
    )

    with patch('watcher.tasks.statistics._run_yql') as mock_run:
        upload_duty_to_yt()
    mock_run.assert_called_once_with(
        columns=(
            'staff_login',
            'staff_id',
            'all_services',
            'services',
            'schedules',
            'is_weekend',
            'is_holiday',
            'gap_hours',
            'current_date',
        ),
        table='home/abc/duty2/manual_gaps/2021-03-22',
        items={
            (gap.staff.login, gap.staff_id, False, tuple(), (schedule.id, ), False, False, 24, '2021-03-22'),
            (gap_1.staff.login, gap_1.staff_id, False, (schedule.service.id, ), tuple(), False, False, 2, '2021-03-22'),
        }
    )
