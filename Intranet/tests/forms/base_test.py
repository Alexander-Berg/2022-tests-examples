import pytest
from staff.lib.testing import FloorFactory, OfficeFactory

from staff.map.forms.base import GeometryBaseMapEditForm


@pytest.mark.django_db
@pytest.mark.parametrize(
    'data_patch, valid',
    [
        ({'geometry': ''}, True),
        ({'geometry': '0,1,1,1,0,1'}, False),
        ({'geometry': '0,1,1,1,1,0,0,0'}, False),
        ({'geometry': '0,1,1,1,1,0,0,1'}, True),
        ({'geometry': '0,1,1,1,1,0,0,1a'}, False),
        ({'geometry': '0,1,1,1,1,0,0,0,1'}, False),
        ({'geometry': '0,-1,-1,-1,0,-1'}, False),
        ({'geometry': '0,-1,-1,-1,-1,0,0,0'}, False),
        ({'geometry': '0,-1,-1,-1,-1,0,0,-1'}, True),
        ({'geometry': '0,-1,-1,-1,-1,0,0,-1a'}, False),
        ({'geometry': '0,-1,-1,-1,-1,0,0,0,-1'}, False),
    ],
)
def test_geometry_base_map_edit_form(data_patch, valid):
    floor = FloorFactory(office=OfficeFactory(have_map=True, intranet_status=1), intranet_status=1)
    data = {
        'floor': floor.id,
        'coord_x': 1,
        'coord_y': 1,
    }

    for key, value in data_patch.items():
        data[key] = value

    form = GeometryBaseMapEditForm(data=data)

    assert form.is_valid() == valid, form.errors
