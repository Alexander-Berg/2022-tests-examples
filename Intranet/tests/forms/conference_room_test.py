import pytest

from staff.lib.testing import FloorFactory, OfficeFactory

from staff.map.forms.conference_room import ConferenceRoomForm


@pytest.mark.django_db
@pytest.mark.parametrize(
    'name_exchange, name_exchange_is_valid',
    [
        ['русский', False],
        ['english', True],
        ['name_1.2-3', True],
        ['name_1 2', False],
        ['name_1 ', False],
    ],
)
def test_name_exchange(name_exchange, name_exchange_is_valid):
    floor = FloorFactory(intranet_status=1, office=OfficeFactory(have_map=True, intranet_status=1))

    target = ConferenceRoomForm(
        data={
            'name': 'name',
            'name_en': 'name',
            'name_exchange': name_exchange,
            'floor': floor.id,
            'office': floor.office.id,
            'coord_x': 1,
            'coord_y': 1,
        },
    )

    assert target.is_valid() == name_exchange_is_valid, target.errors

    if not name_exchange_is_valid:
        assert 'name_exchange' in target.errors
        assert target.errors['name_exchange'] == [{'error_key': 'invalid'}]
