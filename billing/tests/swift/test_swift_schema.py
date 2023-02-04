from datetime import datetime

import pytest
from django.utils import timezone

from refs.swift.models import Event, Holiday, EventTypes, RecordStatuses, InstitutionTypes, EntityTypes, HolidayTypes


@pytest.mark.django_db
def test_events(client, django_assert_num_queries):

    date = datetime(2018, 8, 28).date()

    events = [
        Event(
            id='t1', event_type=EventTypes.BIC_CREATED, event_date=date, status=RecordStatuses.CURRENT,
            inst_type=InstitutionTypes.FINANCIAL, entity_type=EntityTypes.OPERATIONAL,
            bic_8='55667788', bic_branch='111', active=True
        ),
        Event(
            id='t2', event_type=EventTypes.BIC_CREATED, event_date=date, status=RecordStatuses.CURRENT,
            inst_type=InstitutionTypes.FINANCIAL, entity_type=EntityTypes.OPERATIONAL,
            bic_8='55667788', bic_branch='222', active=True
        ),
        Event(
            id='t3', event_type=EventTypes.BIC_CREATED, event_date=date, status=RecordStatuses.CURRENT,
            inst_type=InstitutionTypes.FINANCIAL, entity_type=EntityTypes.OPERATIONAL,
            bic_8='55667788', bic_branch='333', active=False
        ),

    ]
    Event.objects.bulk_create(events)

    with django_assert_num_queries(1) as _:

        result = client.get('/api/swift/?query=query{events(id:"t1") {bic8}}').json()
        assert result == {'data': {'events': [{'bic8': '55667788'}]}}

    with django_assert_num_queries(1) as _:

        result = client.get(
            '/api/swift/?query=query{bics(bic:["55667788", "55667788333"]) {bic8 bicBranch}}').json()

        data = result['data']['bics']

        assert len(data) == 2
        assert {'bic8': '55667788', 'bicBranch': '111'} in data
        assert {'bic8': '55667788', 'bicBranch': '222'} in data


@pytest.mark.django_db
def test_holidays(client, django_assert_num_queries):

    date = timezone.now().date()

    holidays = [
        Holiday(date=date, type=HolidayTypes.HOLIDAY, country_code='RU', checksum='t1'),
        Holiday(date=date, type=HolidayTypes.HOLIDAY, country_code='GB', checksum='t2'),
    ]
    Holiday.objects.bulk_create(holidays)

    with django_assert_num_queries(1) as _:

        result = client.get(
            '/api/swift/?query=query{holidays(country:"RU") {checksum}}').json()

        assert result == {'data': {'holidays': [{'checksum': 't1'}]}}
