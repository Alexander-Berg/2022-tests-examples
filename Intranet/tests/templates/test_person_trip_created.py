import pytest
from datetime import date
from jinja2 import Template
from library.python import resource

from intranet.trip.src.logic.tracker import get_context_for_belarus_offline_trip


pytestmark = pytest.mark.asyncio


person_trip_url = 'https://trip.test.yandex-team.ru/trips/1/person/1'
expected_text = f'''Создана заявка в сервисе Я.Командировки: {person_trip_url}, TRAVEL-1
Участник: Иван Иванов (ivanivanov)
Даты командировки: 2021-01-01 — 2021-01-05
Маршрут: Минск — Москва — Санкт-Петербург — Минск
Цель: Цель 1, Цель 2
Описание цели: Описание командировки


Минск (Беларусь) — Москва (Россия)
Дата отправления в город: 2021-01-01

Москва (Россия) — Минск (Беларусь)
Дата отправления в город: 2021-01-03

Санкт-Петербург (Россия) — Москва (Россия)
Дата отправления в город: 2021-01-05


Пожелания и данные паспорта:
Описание билетов и паспортные данные.'''


async def test_person_trip_belarus_created(f, uow):
    await f.create_purpose(purpose_id=1, name='Цель 1')
    await f.create_purpose(purpose_id=2, name='Цель 2')
    await f.create_person(person_id=1, first_name='Иван', last_name='Иванов', login='ivanivanov')
    await f.create_trip(trip_id=1, comment='Описание командировки', purpose_ids=[1, 2])
    await f.create_person_trip(
        trip_id=1,
        person_id=1,
        gap_date_from=date(2021, 1, 1),
        gap_date_to=date(2021, 1, 5),
        description='Описание билетов и паспортные данные.',
        route=[{
            'city': 'Минск',
            'country': 'Беларусь',
            'provider_city_id': 1,
            'date': '2021-01-01',
        }, {
            'city': 'Москва',
            'country': 'Россия',
            'provider_city_id': 2,
            'date': '2021-01-03',
        }, {
            'city': 'Санкт-Петербург',
            'country': 'Россия',
            'provider_city_id': 3,
            'date': '2021-01-05',
        }]
    )

    trip = await uow.trips.get_detailed_trip(trip_id=1)
    person_trip = trip.person_trips[0]

    path = 'intranet/trip/src/templates/tracker/person_trip_belarus_created.jinja2'
    template = resource.resfs_read(path).decode('utf-8')
    context = get_context_for_belarus_offline_trip(trip, person_trip, 'TRAVEL-1')
    text = Template(template).render(**context)
    assert text == expected_text
