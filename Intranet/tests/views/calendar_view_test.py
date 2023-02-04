import json
from datetime import timedelta, datetime
import pytest

from django.core.urlresolvers import reverse

from staff.gap.workflows.utils import find_workflow
from staff.lib.testing import FloorFactory, OfficeFactory, RoomFactory
from staff.map.models import ROOM_TYPES


@pytest.mark.django_db
def test_profile_gap(gap_test, client):
    OfficeWorkflow = find_workflow('office_work')
    base_gap = gap_test.get_base_gap(OfficeWorkflow)

    OfficeWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(OfficeWorkflow)
    base_gap['date_from'] = datetime.today() + timedelta(days=2)
    base_gap['date_to'] = datetime.today() + timedelta(days=2)
    OfficeWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id
    ).new_gap(base_gap)

    RemoteWorkflow = find_workflow('remote_work')
    base_gap = gap_test.get_base_gap(RemoteWorkflow)

    RemoteWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(RemoteWorkflow)
    base_gap['date_from'] = datetime.today() + timedelta(days=4)
    base_gap['date_to'] = datetime.today() + timedelta(days=4)
    RemoteWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id
    ).new_gap(base_gap)

    url = '{url}?filter=calendar_holidays&filter=calendar_gaps&\
        filter=calendar&filter=is_calendar_vertical&filter=activity&dropTimeout=true'.format(
        url=reverse('profile:calendar_gaps', args=[gap_test.test_person.login]),
    )

    response = client.get(url)

    assert response.status_code == 200

    calendar_gaps = json.loads(response.content)['target']['calendar_gaps']

    assert len(calendar_gaps) == 2


@pytest.mark.django_db
def test_office_work_gap(gap_test, client):
    office = OfficeFactory(name='Тест', name_en='Test', city=None, intranet_status=1, code='kr')
    floor = FloorFactory(name='Fifth', office=office, intranet_status=1)
    room = RoomFactory(
        name='Комната', name_en='Room',  floor=floor, intranet_status=1,
        room_type=ROOM_TYPES.OFFICE, num=123,
    )
    OfficeWorkflow = find_workflow('office_work')
    base_gap = gap_test.get_base_gap(OfficeWorkflow)
    base_gap['office'] = office.id
    base_gap['room'] = room.id
    base_gap['date_from'] = datetime.today() + timedelta(days=2)
    base_gap['date_to'] = datetime.today() + timedelta(days=2)
    OfficeWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id
    ).new_gap(base_gap)

    url = '{url}?filter=calendar_holidays&filter=calendar_gaps&\
        filter=calendar&filter=is_calendar_vertical&filter=activity&dropTimeout=true'.format(
        url=reverse('profile:calendar_gaps', args=[gap_test.test_person.login]),
    )

    response = client.get(url)

    assert response.status_code == 200

    calendar_gaps = json.loads(response.content)['target']['calendar_gaps']

    assert len(calendar_gaps) == 1
    calendar_gap_meta = calendar_gaps[0]['meta']

    assert calendar_gap_meta['office'] is not None
    assert calendar_gap_meta['office'] == office.id
    assert calendar_gap_meta['office_code'] is not None
    assert calendar_gap_meta['office_code'] == office.code
    assert calendar_gap_meta['office_name'] is not None
    assert calendar_gap_meta['office_name'] == office.name
    assert calendar_gap_meta['office_name_en'] is not None
    assert calendar_gap_meta['office_name_en'] == office.name_en

    assert calendar_gap_meta['room'] is not None
    assert calendar_gap_meta['room'] == room.id
    assert calendar_gap_meta['room_name'] is not None
    assert calendar_gap_meta['room_name'] == room.name
    assert calendar_gap_meta['room_name_en'] is not None
    assert calendar_gap_meta['room_name_en'] == room.name_en
    assert calendar_gap_meta['room_num'] == room.num
