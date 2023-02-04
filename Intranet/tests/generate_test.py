import pytest
from mock import patch

from django.test.client import RequestFactory

from staff.lib.testing import (
    UserFactory,
    StaffFactory,
    DepartmentFactory,
    DepartmentStaffFactory
)

from staff.vcard.vcard_generator import _get_vcard


@pytest.mark.django_db()
@patch('staff.vcard.vcard_generator._get_raw_photo')
def test_generate_vcard(get_photo_mock, kinds):
    get_photo_mock.return_value = None

    yandex = DepartmentFactory(intranet_status=1)
    volozh = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        is_big_boss=True,
        login='volozh',
        department=yandex
    )
    DepartmentStaffFactory(
        staff=volozh,
        department=yandex,
        role_id='C'
    )

    request = RequestFactory().get('/')
    request.user = UserFactory()

    def _is_valid_vcard(text):
        return ('BEGIN' in text) and ('END' in text)
    vcard1 = _get_vcard(volozh, True)
    assert _is_valid_vcard(vcard1)

    vcard2 = _get_vcard(volozh, True)
    assert _is_valid_vcard(vcard2)

    vcard3 = _get_vcard(volozh, True)
    assert _is_valid_vcard(vcard3)

    assert vcard1 == vcard2
    assert vcard2 == vcard3
