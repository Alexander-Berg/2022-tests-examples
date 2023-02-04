import factory

from plan.holidays.models import Holiday


class HolidayFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Holiday

    is_holiday = True
