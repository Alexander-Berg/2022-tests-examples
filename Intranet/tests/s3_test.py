from collections import namedtuple
import random
import string
from io import BytesIO

from django.core.files.uploadedfile import SimpleUploadedFile
from django.conf import settings

from staff.map.tests import mock_classes
from staff.lib.testing import OfficeFactory, FloorFactory
from PIL import Image
import pytest


Response = namedtuple('Response', ['status', 'content'])


@pytest.fixture()
def create_floor_map_form(db):
    # create random 500x500 image
    size = (500, 500)
    color = (255, 0, 0, 0)
    image = BytesIO()
    img = Image.new("RGBA", size, color)
    img.save(image, 'png')
    image.seek(0)

    image_name = ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(10))

    office = OfficeFactory()
    office.save()
    floor = FloorFactory()
    floor.save()

    img_bytes = image.read()

    data = {'image': SimpleUploadedFile(image_name, img_bytes)}
    form = mock_classes.MockFloorMapForm({'floor': floor.id}, data)
    mock_classes.saved_instances = {}

    assert form.is_valid()
    return form.save(), img_bytes


def request(file_name, headers):
    assert file_name in mock_classes.saved_instances
    assert ('HOST' in headers and headers['HOST'] == settings.STAFF_DOMAIN)
    return Response(status=200, content=mock_classes.saved_instances[file_name])


def test_save_to_mds3(create_floor_map_form):
    floor_map, image_content = create_floor_map_form
    response = request(floor_map.file_name, {'HOST': 'staff.yandex-team.ru'})

    assert len(mock_classes.saved_instances) == 1
    assert response.status == 200
    assert response.content == image_content


def test_tile_cutter(create_floor_map_form):
    floor_map, image_content = create_floor_map_form
    tile_cutter = mock_classes.MockTileCutter

    tile_cutter(map_id=floor_map.id, nolock=True)
    assert len(mock_classes.saved_instances) == 1
