import pytest
from mock import MagicMock

from staff.lib.testing import StaffPhoneFactory
from staff.person.models import StaffPhone
from staff.person_profile.controllers.phones import PhonesCtl


@pytest.mark.django_db
def test_update_phones_delete():
    phone = StaffPhoneFactory()

    permissions_ctl_mock = MagicMock()
    permissions_ctl_mock.properties.get_target_data = MagicMock(return_value={'id': phone.staff_id})

    target = PhonesCtl(phone.staff.login, permissions_ctl_mock)

    target.update_phones([])

    db_phone = StaffPhone.objects.get(id=phone.id)
    assert db_phone.intranet_status == 0
