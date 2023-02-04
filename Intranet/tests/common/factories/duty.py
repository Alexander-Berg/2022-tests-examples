import factory

from django.utils import timezone
from django.utils.dateparse import parse_date
from factory import post_generation

from plan.common.utils.timezone import make_localized_datetime
from plan.duty.models import Gap, Schedule, Shift, Order, DutyToWatcher
from common.factories.staff import StaffFactory
from common.factories.roles import RoleFactory
from common.factories.services import ServiceFactory


class GapFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Gap

    gap_id = factory.Sequence(lambda n: n)
    staff = factory.SubFactory(StaffFactory)
    start = timezone.now()
    end = timezone.now() + timezone.timedelta(hours=5)
    type = 'type'
    full_day = False
    work_in_absence = False


class ScheduleFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Schedule

    name = factory.Sequence(lambda a: 'duty_schedule_%s' % a)
    description = factory.Sequence(lambda a: 'Duty Schedule %s' % a)
    slug = factory.Sequence(lambda a: 'slug_%s' % a)
    role = factory.SubFactory(RoleFactory)

    service = factory.SubFactory(ServiceFactory)
    duration = timezone.timedelta(days=5)
    start_date = timezone.now().date()
    allow_sequential_shifts = True


class ShiftFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Shift

    staff = factory.SubFactory(StaffFactory)
    schedule = factory.SubFactory(ScheduleFactory)
    start = timezone.now().date()
    end = timezone.now().date() + timezone.timedelta(days=5)
    start_datetime = factory.LazyAttribute(
        lambda a: make_localized_datetime(
            parse_date(a.start) if isinstance(a.start, str) else a.start,
            a.schedule.start_time,
        )
    )
    end_datetime = factory.LazyAttribute(
        lambda a: make_localized_datetime(
            (parse_date(a.end) if isinstance(a.end, str) else a.end) + timezone.timedelta(days=1),
            a.schedule.start_time,
        )
    )
    has_problems = False

    @post_generation
    def role(self, create, extracted, **kwargs):
        if extracted is None:
            self.role = self.schedule.get_role_on_duty()


class OrderFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Order

    staff = factory.SubFactory(StaffFactory)
    order = factory.LazyAttribute(lambda a: a)
    schedule = factory.SubFactory(ScheduleFactory)


class DutyToWatcherFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = DutyToWatcher

    abc_id = factory.Sequence(lambda n: 1000 + n)
    watcher_id = factory.Sequence(lambda n: 10000 + n)
