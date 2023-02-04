import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import (
    FloorFactory,
    StaffFactory,
    OfficeFactory,
    RoomFactory,
)

from staff.map.edit.room import mass_workplace_transfer


@pytest.mark.django_db
def test_mass_transfer_works(rf):
    staff_amount = 4
    morozov = OfficeFactory()
    floor = FloorFactory(office=morozov)
    new_room = RoomFactory(floor=floor)

    staff = [StaffFactory(office=morozov) for _ in range(staff_amount)]

    logins = [st.login for st in staff]

    url = reverse('map-mass_workspace_transfer')
    data = {
        'room': new_room.id,
        'persons': logins,
    }

    request = rf.post(url, data=json.dumps(data), content_type='application/json')
    request.user = StaffFactory()
    response = mass_workplace_transfer(request)

    assert response.status_code == 200


@pytest.mark.django_db
def test_mass_transfer_dont_work_for_too_mush_logins(rf):
    staff_amount = settings.MAX_PERSONS_FOR_WORKPLACE_TRANSFER * 2
    morozov = OfficeFactory()
    floor = FloorFactory(office=morozov)
    new_room = RoomFactory(floor=floor)

    staff = [StaffFactory(office=morozov) for _ in range(staff_amount)]

    logins = [st.login for st in staff]

    url = reverse('map-mass_workspace_transfer')
    data = {
        'room': new_room.id,
        'persons': logins,
    }

    request = rf.post(url, data=json.dumps(data), content_type='application/json')
    request.user = StaffFactory()
    response = mass_workplace_transfer(request)

    assert response.status_code == 400
    content = json.loads(response.content)
    expected_error = [
        {
            'code': 'too-much-logins',
            'params': {
                'max_logins_amount': settings.MAX_PERSONS_FOR_WORKPLACE_TRANSFER,
            },
        },
    ]
    assert content['errors']['persons'] == expected_error


@pytest.mark.django_db
def test_mass_transfer_dont_work_for_foreign_office(rf):
    staff_amount = 4

    morozov = OfficeFactory()
    mamontov = OfficeFactory()

    morozov_floor = FloorFactory(office=morozov)
    morozov_room = RoomFactory(floor=morozov_floor)

    staff = [StaffFactory(office=morozov) for _ in range(staff_amount - 1)]
    staff.append(StaffFactory(office=mamontov))

    logins = [st.login for st in staff]

    url = reverse('map-mass_workspace_transfer')
    data = {
        'room': morozov_room.id,
        'persons': logins,
    }

    request = rf.post(url, data=json.dumps(data), content_type='application/json')
    request.user = StaffFactory()
    response = mass_workplace_transfer(request)

    assert response.status_code == 400
    content = json.loads(response.content)
    expected_error = [
        {
            'code': 'can-not-transfer-btw-offices',
            'params': {
                'new_office_id': morozov.id,
                'logins': [staff[-1].login],
            },
        },
    ]

    assert content['errors']['persons'] == expected_error

    for st in staff:
        st.refresh_from_db()
    assert all(st.room == morozov_room for st in staff[:-1])
