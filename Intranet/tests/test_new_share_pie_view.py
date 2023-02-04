from datetime import date
from decimal import Decimal
import json

import pytest

from django.core.urlresolvers import reverse
from django.test import RequestFactory

from staff.lib.testing import RoomFactory, StaffFactory

from staff.workspace_management.models import RoomSharePie
from staff.workspace_management.views import new_share_pie
from staff.workspace_management.tests.factories import BusinessUnitFactory


@pytest.mark.django_db
def test_creation_of_new_share_pie(rf: RequestFactory) -> None:
    # given
    author = StaffFactory()
    author.user.is_superuser = True
    author.user.save()
    room = RoomFactory()
    room_area = Decimal(50)
    share_value = Decimal(100)
    share = {'business_unit': BusinessUnitFactory().id, 'share_value': str(share_value)}
    data = {'room': room.id, 'room_area': str(room_area), 'shares': [share]}
    request = rf.post(
        reverse('workspace-management-api:new-share-pie'),
        data=json.dumps(data),
        content_type='application/json',
    )
    request.user = author.user

    # when
    result = new_share_pie(request)

    # then
    assert result.status_code == 200
    room_share_pie = RoomSharePie.objects.get(room_id=room.id)
    assert room_share_pie.room_area == room_area
    assert room_share_pie.from_date == date.today()
    assert room_share_pie.to_date == date.max
    assert room_share_pie.shares.count() == 1
    assert room_share_pie.shares.get().share_value == share_value
