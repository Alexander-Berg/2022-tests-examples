import pytest
from smarttv.droideka.proxy.views.search import SearchView, patch_collection_image_if_necessary, \
    fix_parent_collections_image_urls, FIELD_IMAGE_ORIGINAL
from smarttv.droideka.tests.mock import oo_collection_images_with_bad_image, oo_actual_bad_collection_image_url, \
    oo_expected_good_collection_image_url, oo_parent_collection_images_with_bad_image
from smarttv.droideka.utils import PlatformInfo, PlatformType

import logging

logger = logging.getLogger(__name__)

collection_images = [
    (
        {
            'original': 'https://pbs.twimg.com/media/BfoCUBbCIAA44yt.jpg',
            'mds_avatar_id': '67347/78419931'
        },
        'https://avatars.mds.yandex.net/get-entity_search/67347/78419931/orig'  # changed
    ),
    (
        {
            'original': 'https://avatars.mds.yandex.net/get-entity_search/123/123/orig',
            'mds_avatar_id': '67347/78419931'
        },
        'https://avatars.mds.yandex.net/get-entity_search/123/123/orig'  # not changed
    )
]


platform_info = PlatformInfo(PlatformType.ANDROID, '9', '1.4', '', '')


@pytest.mark.parametrize('original_image, result_url', collection_images)
def test_patch_collections_image(original_image, result_url):
    patch_collection_image_if_necessary(original_image, platform_info, None)
    assert original_image[FIELD_IMAGE_ORIGINAL] == result_url


def test_patch_real_response():
    image_to_patch = oo_collection_images_with_bad_image
    url_before = image_to_patch[
        'entity_data']['related_object'][0]['object'][0]['collection_images'][0][FIELD_IMAGE_ORIGINAL]
    assert url_before == oo_actual_bad_collection_image_url
    SearchView.fix_collections_image_urls(oo_collection_images_with_bad_image, platform_info)
    url_after = image_to_patch[
        'entity_data']['related_object'][0]['object'][0]['collection_images'][0][FIELD_IMAGE_ORIGINAL]
    assert url_after == oo_expected_good_collection_image_url


def test_patch_parent_collection_images():
    image_to_patch = oo_parent_collection_images_with_bad_image
    url_before = image_to_patch['entity_data']['parent_collection']['object'][0]['image'][FIELD_IMAGE_ORIGINAL]
    assert url_before == oo_actual_bad_collection_image_url
    fix_parent_collections_image_urls(
        oo_parent_collection_images_with_bad_image['entity_data']['parent_collection']['object'], platform_info)
    url_after = image_to_patch['entity_data']['parent_collection']['object'][0]['image'][FIELD_IMAGE_ORIGINAL]
    assert url_after == oo_expected_good_collection_image_url
