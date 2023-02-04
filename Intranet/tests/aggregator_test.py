from datetime import date, timedelta

import pytest

from staff.lib.testing import StaffFactory

from staff.lenta.models import CountAndGrowth
from staff.lenta.objects import Aggregator


@pytest.mark.django_db
def test_hire_one(departments_and_offices):
    start = date.today()
    end = start + timedelta(1)
    StaffFactory(
        first_name='Mike',
        last_name='Mouse',
        login='mikki',
        position='Head of department',
        department=departments_and_offices.infra,
        office=departments_and_offices.red_rose,
        organization=departments_and_offices.yandex_org,
    )

    Aggregator(start, end).run()

    assert CountAndGrowth.objects.count() == 1

    counter = CountAndGrowth.objects.get()
    assert counter.created_at == start
    assert counter.office == departments_and_offices.red_rose
    assert counter.organization == departments_and_offices.yandex_org
    assert counter.department_0 == departments_and_offices.yandex
    assert counter.department_1 == departments_and_offices.infra
    assert counter.department_2 is None
    assert counter.total == 1
    assert counter.hired == 1
    assert counter.fired == 0
    assert counter.returned == 0
    assert counter.joined == 0
    assert counter.left == 0


@pytest.mark.django_db
def test_hire_two(departments_and_offices):
    start = date.today()
    end = start + timedelta(1)
    StaffFactory(
        user__username='user1',
        department=departments_and_offices.infra,
        office=departments_and_offices.red_rose,
        organization=departments_and_offices.yandex_org,
    )
    StaffFactory(
        user__username='user2',
        position='Head of department',
        department=departments_and_offices.infra,
        office=departments_and_offices.red_rose,
        organization=departments_and_offices.yandex_org,
    )

    Aggregator(start, end).run()

    assert CountAndGrowth.objects.count() == 1

    counter = CountAndGrowth.objects.get()
    assert counter.created_at == start
    assert counter.office == departments_and_offices.red_rose
    assert counter.organization == departments_and_offices.yandex_org
    assert counter.department_0 == departments_and_offices.yandex
    assert counter.department_1 == departments_and_offices.infra
    assert counter.department_2 is None
    assert counter.total == 2
    assert counter.hired == 2
    assert counter.fired == 0
    assert counter.returned == 0
    assert counter.joined == 0
    assert counter.left == 0


@pytest.mark.django_db
def test_hire_and_fire(departments_and_offices):
    start = date.today()
    end = start + timedelta(1)
    person1 = StaffFactory(
        user__username='user1',
        department=departments_and_offices.infra,
        office=departments_and_offices.red_rose,
        organization=departments_and_offices.yandex_org,
    )
    person1.is_dismissed = True
    person1.save()

    Aggregator(start, end).run()

    assert CountAndGrowth.objects.count() == 1

    counter = CountAndGrowth.objects.get()
    assert counter.created_at == start
    assert counter.office == departments_and_offices.red_rose
    assert counter.organization == departments_and_offices.yandex_org
    assert counter.department_0 == departments_and_offices.yandex
    assert counter.department_1 == departments_and_offices.infra
    assert counter.department_2 is None
    assert counter.total == 0
    assert counter.hired == 1
    assert counter.fired == 1
    assert counter.returned == 0
    assert counter.joined == 0
    assert counter.left == 0


@pytest.mark.django_db
def test_smoke_chart_json(departments_and_offices, client):
    response = client.get('/lenta/chart.json')
    assert response.status_code == 200
