import io

import pytest
from PIL import Image
from django.core.exceptions import ValidationError
from mock import patch, Mock

from staff.achievery.controllers.icon import IconCtl

FAKE_ICON_DATA = b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x02\x00\x00\x00\x02\x08\x06\x00\x00\x00r' \
                 b'\xb6\r$\x00\x00\x00\x01sRGB\x00\xae\xce\x1c\xe9\x00\x00\x00\x1bIDAT\x18Wc\xfc\xff\xff\xff' \
                 b'\xff\xb9\x1a\x1b\x19\x18\xe7\xa8o\xf8\x9f|\xc3\x9f\x01\x00c!\tq\xa8g\xee\xa4\x00\x00\x00' \
                 b'\x00IEND\xaeB`\x82'


@patch('staff.achievery.controllers.icon.requests.get', Mock(return_value=Mock(content=FAKE_ICON_DATA)))
def test_get_icon_data_from_url():
    url = 'some url'
    mocked_image = Mock(
        width=IconCtl.ICON_SIZE_BIG,
        height=IconCtl.ICON_SIZE_BIG,
        format='PNG',
    )

    patch_get_resized_image = patch('staff.achievery.controllers.icon.IconCtl._get_resized_image')
    patch_get_image_data = patch('staff.achievery.controllers.icon.IconCtl._get_image_data')

    with patch('staff.achievery.controllers.icon.IconCtl._get_image_by_url', Mock(return_value=mocked_image)):
        with patch_get_image_data, patch_get_resized_image:
            IconCtl.get_icon_data_from_url(url)


@patch('staff.achievery.controllers.icon.requests.get', Mock(return_value=Mock(content=FAKE_ICON_DATA)))
def test_get_image_by_url():
    expected_result = Image.open(io.BytesIO(FAKE_ICON_DATA))
    assert IconCtl._get_image_by_url('') == expected_result


@patch('staff.achievery.controllers.icon.requests.get', Mock(return_value=Mock(content=FAKE_ICON_DATA)))
def test_get_icon_data_from_url_bad_data():
    url = 'some url'

    with pytest.raises(ValidationError):
        IconCtl.get_icon_data_from_url(url)
