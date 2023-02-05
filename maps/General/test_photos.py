import lib.avatars as avatars

from data_types.util import generate_image

from lib.server import server
import lib.async_processor as async_processor

import pytest
import random


def test_photo_url(user):
    url = 'http://example.com'

    response = server.post_photos_url(user, url) >> 200
    assert 'id' in response
    assert 'template_url' in response
    assert sorted(response['sizes']) == sorted(['small', 'orig', 'medium'])

    image_data = avatars.get_image_by_template(response['template_url'], 'orig')
    assert len(image_data) > 0


def test_photo_file_upload(user):
    image_data = generate_image()
    response = server.post_photo_file(user, image_data) >> 200

    assert 'id' in response
    assert 'template_url' in response
    assert sorted(response['sizes']) == sorted(['small', 'orig', 'medium'])

    downloaded_data = avatars.get_image_by_template(response['template_url'], 'orig')
    assert downloaded_data == image_data


def test_dispatch_cannot_process_image_error_on_upload(user):
    url = 'http://bad-image.com'
    avatars.add_erroneous_url(
        url=url,
        code=400,
        description='cannot process image: invalid format')

    response = server.post_photos_url(user, url) >> 422
    assert response['code'] == 'PHOTO_UPLOAD_FAILED'


def test_dispatch_blacklist_error_on_upload(user):
    url = 'http://bad-image.com'
    avatars.add_erroneous_url(
        url=url,
        code=409,
        description='The image is found in blacklist')

    response = server.post_photos_url(user, url) >> 422
    assert response['code'] == 'PHOTO_UPLOAD_FAILED'


def test_dispatch_too_small_error_on_upload(user):
    url = 'http://bad-image.com'
    avatars.add_erroneous_url(
        url=url,
        code=415,
        description='Image is too small')

    response = server.post_photos_url(user, url) >> 422
    assert response['code'] == 'PHOTO_UPLOAD_FAILED'


def test_dispatch_download_error_on_upload(user):
    url = 'http://bad-image.com'
    avatars.add_erroneous_url(
        url=url,
        code=434,
        description='Could not download the image: invalid URL')

    response = server.post_photos_url(user, url) >> 422
    assert response['code'] == 'PHOTO_UPLOAD_FAILED'


@pytest.mark.parametrize("last_is_404", [False, True])
def test_photos_duplicate_url_eventual_deletion(user, last_is_404):
    delete_attempts = random.randint(2, 6)
    avatars.set_delete_settings(
        delete_attempts=delete_attempts,
        last_is_404=last_is_404)

    url = 'http://example.com'

    first_upload = server.post_photos_url(user, url) >> 200
    second_upload = server.post_photos_url(user, url) >> 200

    assert first_upload == second_upload
    async_processor.perform_all_work()
    assert avatars.get_namespace_size(avatars.DEFAULT_NAMESPACE) == 1


@pytest.mark.parametrize("last_is_404", [False, True])
def test_photos_duplicate_data_eventual_deletion(user, last_is_404):
    delete_attempts = random.randint(2, 6)
    avatars.set_delete_settings(
        delete_attempts=delete_attempts,
        last_is_404=last_is_404)

    url = 'http://example.com'

    first_upload = server.post_photos_url(user, url) >> 200
    image_data = avatars.get_image_by_template(first_upload['template_url'], 'small')
    second_upload = server.post_photo_file(user, image_data) >> 200

    assert first_upload == second_upload
    async_processor.perform_all_work()
    assert avatars.get_namespace_size(avatars.DEFAULT_NAMESPACE) == 1
