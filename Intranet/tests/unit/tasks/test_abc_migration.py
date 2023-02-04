import datetime
import responses
from responses import matchers

from unittest.mock import patch
from freezegun import freeze_time

from watcher.db import (
    AbcMigration,
    Shift,
)
from watcher.config import settings
from watcher.logic.timezone import now
from watcher.tasks.abc_migration import create_migrations, prepare_migration


@freeze_time('2022-06-01')
@responses.activate
def test_migrate_abc_schedule(scope_session, service_factory, staff_factory, role_factory):

    service = service_factory()
    abc_schedule_id = 111

    staff_author = staff_factory()
    staff_mikey = staff_factory(login='mikey')
    staff_mouse = staff_factory(login='mouse')

    role = role_factory()
    duty_role = role_factory()
    scope_session.commit()

    responses.add(
        responses.GET,
        f'{settings.ABC_API_HOST}api/v4/duty/schedules/'
        f'?service={service.id}&fields=id%2Cslug%2Cconsider_other_schedules&page_size=10',
        status=200,
        json={
            'next': None,
            'results': [
                {
                    'id': abc_schedule_id,
                    'consider_other_schedules': True,
                    'slug': 'service',
                }
            ]
        }
    )

    responses.add(
        responses.GET,
        f'{settings.ABC_API_HOST}api/v4/duty/schedules/{abc_schedule_id}/'
        f'?fields=id%2Cslug%2Cname%2Cdescription%2Cdays_for_problem_notification%2Cdays_for_begin_shift_notification%2C'
        f'is_important%2Cautoapprove_timedelta%2Crecalculate%2Calgorithm%2Corders%2Cduration%2Cduty_on_holidays%2C'
        f'duty_on_weekends%2Cpersons_count%2Crole%2Crole_on_duty,show_in_staff',
        status=200,
        json={
            'recalculate': True,
            'name': 'Дежурство по IDM',
            'description': 'Дежурство по IDM',
            'algorithm': 'manual_order',
            'autoapprove_timedelta': '3 00:00:00',
            'slug': 'service',
            'days_for_problem_notification': 14,
            'days_for_begin_shift_notification': [0, 1, 7],
            'is_important': True,
            'duration': '7 00:00:00',
            'duty_on_holidays': True,
            'duty_on_weekends': True,
            'persons_count': 2,
            'show_in_staff': True,
            'role_on_duty': duty_role.id,
            'role': {
                'id': role.id,
            },
            'orders': [
                {
                    'person': {
                        'abc_id': staff_mikey.id,
                        'login': staff_mikey.login,
                    },
                    'order': 1,
                },
                {
                    'person': {
                        'abc_id': staff_mouse.id,
                        'login': staff_mouse.login,
                    },
                    'order': 0,
                },
            ]
        },
    )

    date_to = (now() + datetime.timedelta(days=settings.ABC_MIGRATION_SHIFT_END_DATE_DELTA)).strftime('%Y-%m-%d')
    responses.add(
        responses.GET,
        f'{settings.ABC_API_HOST}api/v4/duty/shifts/'
        f'?schedule={abc_schedule_id}&date_from={settings.ABC_MIGRATION_SHIFT_START_DATE}&date_to={date_to}'
        f'&fields=id%2Cperson%2Cstart_datetime%2Cend_datetime%2Cis_approved&ordering=start_datetime',
        status=200,
        json={
            'results': [
                {
                    'id': 1001,
                    'person': {
                        'abc_id': staff_mikey.id,
                        'login': staff_mikey.login,
                    },
                    'is_approved': True,
                    'start_datetime': '2022-05-01T00:00:00+03:00',
                    'end_datetime': '2022-08-01T00:00:00+03:00',
                },
                {
                    'id': 1002,
                    'person': {
                        'abc_id': staff_mouse.id,
                        'login': staff_mouse.login,
                    },
                    'is_approved': True,
                    'start_datetime': '2022-05-01T00:00:00+03:00',
                    'end_datetime': '2022-08-01T00:00:00+03:00',
                },
                {
                    'id': 1003,
                    'person': {
                        'abc_id': staff_mikey.id,
                        'login': staff_mikey.login,
                    },
                    'is_approved': False,
                    'start_datetime': '2022-08-01T00:00:00+03:00',
                    'end_datetime': '2022-11-01T00:00:00+03:00',
                },
                {
                    'id': 1004,
                    'person': {
                        'abc_id': staff_mouse.id,
                        'login': staff_mikey.login,
                    },
                    'is_approved': False,
                    'start_datetime': '2022-08-01T00:00:00+03:00',
                    'end_datetime': '2022-11-01T00:00:00+03:00',
                }
            ]
        },
    )

    responses.add(
        responses.POST,
        url=f'{settings.ABC_API_HOST}api/v4/duty/abc_to_watcher/',
        match=[
            matchers.json_params_matcher(
                {'abc_id': 111, 'watcher_id': 1}
            )
        ],
        status=201,
    )

    with patch('watcher.tasks.abc_migration.prepare_migration_task.delay'):
        create_migrations(session=scope_session, service_id=service.id, author_id=staff_author.id)

    migrations = scope_session.query(AbcMigration).all()
    assert len(migrations) == 1

    for migration in migrations:
        prepare_migration(session=scope_session, abc_migration=migration)

    shifts = scope_session.query(Shift).all()
    assert len(shifts) == 2
