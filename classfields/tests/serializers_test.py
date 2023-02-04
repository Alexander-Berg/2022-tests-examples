import pytest

from parts.models import PartImage, PartInfo
from parts.serializers import PartInfoListSerializer


@pytest.mark.django_db
def test_get_images(part_info: PartInfo, image: PartImage):
    result = PartInfoListSerializer().get_images(part=part_info)
    assert len(result) == 1
    only_image, = result
    assert only_image["link"] == image.image.avatar_url
    assert only_image["is_good"] == image.is_good
