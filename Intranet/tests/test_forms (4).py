from decimal import Decimal

import pytest

from staff.lib.testing import RoomFactory

from staff.workspace_management.forms import ShareForm, RoomSharePieForm
from staff.workspace_management.tests.factories import BusinessUnitFactory


@pytest.mark.django_db
def test_share_form_correct():
    # given
    bu = BusinessUnitFactory()
    data = {'business_unit': bu.id, 'share_value': '50.0'}
    form = ShareForm(data)

    # when
    is_valid = form.is_valid()

    # then
    assert is_valid
    assert form.cleaned_data['business_unit'].id == bu.id
    assert form.cleaned_data['share_value'] == Decimal(50)


@pytest.mark.django_db
@pytest.mark.parametrize('share_value', ['-1', '100.1'])
def test_share_form_checks_share_value(share_value):
    # given
    data = {'business_unit': BusinessUnitFactory().id, 'share_value': share_value}
    form = ShareForm(data)

    # when
    is_valid = form.is_valid()

    # then
    assert not is_valid
    assert 'share_value' in form.errors['errors']


@pytest.mark.django_db
def test_room_share_pie_form_correct():
    # given
    room = RoomFactory()
    share = {'business_unit': BusinessUnitFactory().id, 'share_value': '100.0'}
    data = {'room': room.id, 'room_area': '50.0', 'shares': [share]}
    form = RoomSharePieForm(data)

    # when
    is_valid = form.is_valid()

    # then
    assert is_valid
    assert form.cleaned_data['room'].id == room.id
    assert form.cleaned_data['room_area'] == Decimal(50)


@pytest.mark.django_db
def test_room_share_pie_form_check_total_shares():
    # given
    room = RoomFactory()
    share1 = {'business_unit': BusinessUnitFactory().id, 'share_value': '90.0'}
    share2 = {'business_unit': BusinessUnitFactory().id, 'share_value': '20.0'}
    data = {'room': room.id, 'room_area': '50.0', 'shares': [share1, share2]}
    form = RoomSharePieForm(data)

    # when
    is_valid = form.is_valid()

    # then
    assert not is_valid
    assert 'shares' in form.errors['errors']
