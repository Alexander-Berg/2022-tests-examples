import pytest

from datetime import date

from staff.lib.testing import StaffFactory, OfficeFactory, RoomFactory, FloorFactory
from staff.map.api.forms import EditHistoryForm
from staff.map.forms.region import RegionForm
from staff.map.forms.room import BindRoomForm, UnbindRoomForm


def test_serialize_deserialize_multipolygon():
    region_coordinates = '1445,1344,1450,1309,1456,1301,1464,1299,1502,1298,1512,1304,1516,1314,1516,1354'
    multipolygon = RegionForm.from_text_to_polygon(region_coordinates)
    assert multipolygon.num_geom == 1
    poly = multipolygon[0]
    perimeter = poly[0]
    assert perimeter.coords == (
        (1445.0, 1344.0),
        (1450.0, 1309.0),
        (1456.0, 1301.0),
        (1464.0, 1299.0),
        (1502.0, 1298.0),
        (1512.0, 1304.0),
        (1516.0, 1314.0),
        (1516.0, 1354.0),
        (1445.0, 1344.0)
    )
    assert RegionForm.from_polygon_to_text(multipolygon) == region_coordinates


def test_edit_history_form():
    form = EditHistoryForm({})
    assert not form.is_valid()

    form = EditHistoryForm({'date_from': '2012-01-01'})
    assert not form.is_valid()

    form = EditHistoryForm({'date_to': '2012-01-01'})
    assert not form.is_valid()

    form = EditHistoryForm({'date_from': '2012-01-01', 'date_to': '2012-01-01'})
    assert form.is_valid()
    assert form.cleaned_data['date_from'] == date(2012, 1, 1)
    assert form.cleaned_data['date_to'] == date(2012, 1, 2)


@pytest.mark.django_db
def test_bind_room_form_foreign_office():
    office = OfficeFactory()
    other_office = OfficeFactory()
    other_room = RoomFactory(floor=FloorFactory(office=other_office))
    person = StaffFactory(office=office)

    data = {
        'room': other_room.id,
        'person': person.login,
    }
    form = BindRoomForm(data=data)

    assert not form.is_valid()
    assert form.errors['errors'][''][0]['code'] == 'foreign-office'
    assert form.errors['errors'][''][0]['params']['office_id'] == other_office.id


@pytest.mark.django_db
def test_bind_room_form_nonexistent_room():
    person = StaffFactory()

    data = {
        'room': 100500,
        'person': person.login,
    }
    form = BindRoomForm(data=data)

    assert not form.is_valid()
    assert form.errors['errors']['room'][0]['code'] == 'invalid_choice'


@pytest.mark.django_db
def test_bind_room_form_nonexistent_person():
    room = RoomFactory()

    data = {
        'room': room.id,
        'person': 'imanobody',
    }
    form = BindRoomForm(data=data)

    assert not form.is_valid()
    assert form.errors['errors']['person'][0]['code'] == 'invalid_choice'


@pytest.mark.django_db
def test_bind_room_form_valid_data():
    office = OfficeFactory()
    person = StaffFactory(office=office)
    room = RoomFactory(floor=FloorFactory(office=office))

    data = {
        'room': room.id,
        'person': person.login,
    }
    form = BindRoomForm(data=data)

    assert form.is_valid()
    assert form.cleaned_data['room'] == room
    assert form.cleaned_data['person'] == person


@pytest.mark.django_db
def test_unbind_room_form_valid_data():
    person = StaffFactory()

    data = {
        'person': person.login,
    }
    form = UnbindRoomForm(data=data)

    assert form.is_valid()
    assert form.cleaned_data['person'] == person


@pytest.mark.django_db
def test_unbind_room_form_nonexistent_person():
    data = {
        'person': 'imanobody'
    }
    form = UnbindRoomForm(data=data)

    assert not form.is_valid()
    assert form.errors['errors']['person'][0]['code'] == 'invalid_choice'
