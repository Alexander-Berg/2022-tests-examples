import pytest

from freezegun import freeze_time

from django.conf import settings
from django.utils import timezone

from plan.duty.models import Problem, Schedule
from plan.metrics.duty import duty_counters
from common import factories


pytestmark = [
    pytest.mark.django_db, pytest.mark.postgresql
]


@freeze_time('2019-01-01')
def test_metrics_duty(client, duty_role):
    factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    service1 = factories.ServiceFactory()
    service2 = factories.ServiceFactory()
    service3 = factories.ServiceFactory()
    factories.ServiceFactory()
    manual_order = factories.ScheduleFactory(
        service=service1,
        algorithm=Schedule.MANUAL_ORDER,
    )
    auto_order = factories.ScheduleFactory(service=service2)
    factories.ScheduleFactory(service=service3)
    shifts = []
    for _ in range(10):
        factories.ShiftFactory(schedule=manual_order)
        shifts.append(factories.ShiftFactory(schedule=auto_order))
        factories.ShiftFactory(schedule=manual_order, start=timezone.now().date() - timezone.timedelta(days=5))

    Problem.open_shift_problem(shifts[0], Problem.NOBODY_ON_DUTY, shifts[0].start_datetime)
    Problem.open_shift_problem(shifts[1], Problem.NOBODY_ON_DUTY, shifts[1].start_datetime)
    Problem.open_schedule_new_member(manual_order, factories.StaffFactory().id)

    values = duty_counters()

    values = {item['slug']: item['value'] for item in values}

    assert values['services_count'] == 3
    assert values['schedules_count'] == 3
    assert values['schedules_noorder'] == 2
    assert values['schedules_manual'] == 1
    assert values['shift_problems_count'] == 2
    assert values['schedule_problems_count'] == 1
    assert values['problematic_services'] == 2
    assert values['staff_in_schedules_count'] == 20
    assert values['active_staff_in_schedules_count'] == 10
