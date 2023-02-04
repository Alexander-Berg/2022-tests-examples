from dataclasses import replace
from hashlib import sha256
from uuid import uuid4

import psycopg2
import pytest

from hamcrest import assert_that, equal_to, has_properties, instance_of, none

from billing.yandex_pay.yandex_pay.core.entities.enums import ImageType
from billing.yandex_pay.yandex_pay.core.entities.image import Image


@pytest.fixture
def fake_sha():
    return sha256(b'fake').hexdigest()


@pytest.fixture(params=list(ImageType))
def image_entity(request):
    return Image(
        image_type=request.param, external_image_id=str(uuid4())
    )


@pytest.mark.asyncio
async def test_create(storage, image_entity):
    created = await storage.image.create(image_entity)
    assert_that(
        created,
        has_properties(
            image_id=instance_of(int),
            sha256=none(),
            avatars_url=none(),
            external_image_id=instance_of(str),
        )
    )


@pytest.mark.asyncio
async def test_cannot_create_images_with_duplicate_hash(storage, image_entity, fake_sha):
    image_entity.sha256 = fake_sha
    created = await storage.image.create(image_entity)
    assert_that(
        created,
        has_properties(sha256=fake_sha)
    )

    duplicate_image_entity = replace(image_entity, external_image_id=str(uuid4()))
    pattern = 'duplicate key value violates unique constraint "images_sha256_idx"'
    with pytest.raises(psycopg2.IntegrityError, match=pattern):
        await storage.image.create(duplicate_image_entity)


@pytest.mark.asyncio
async def test_get(storage, image_entity):
    created = await storage.image.create(image_entity)
    assert_that(
        await storage.image.get(created.image_id),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Image.DoesNotExist):
        await storage.image.get(100500)


@pytest.mark.asyncio
async def test_save(storage, image_entity, fake_sha):
    created = await storage.image.create(image_entity)
    created.external_image_id = 'fake'
    created.sha256 = fake_sha
    created.avatars_url = "/get-fake-namespace/603/fake-imagename"

    saved = await storage.image.save(created)
    created.updated = saved.updated
    assert_that(saved, equal_to(created))


@pytest.mark.asyncio
async def test_get_by_sha(storage, image_entity, fake_sha):
    image_entity.sha256 = fake_sha
    created = await storage.image.create(image_entity)

    loaded = await storage.image.get_by_sha(fake_sha)
    assert_that(loaded, equal_to(created))


@pytest.mark.asyncio
async def test_get_by_sha_not_found(storage, image_entity, fake_sha):
    image_entity.sha256 = fake_sha
    await storage.image.create(image_entity)

    other_sha = sha256(b'other_sha').hexdigest()

    with pytest.raises(Image.DoesNotExist):
        await storage.image.get_by_sha(other_sha)


@pytest.mark.asyncio
async def test_get_by_external_id_and_type(storage, image_entity):
    created = await storage.image.create(image_entity)

    loaded = await storage.image.get_by_external_id_and_type(
        image_entity.external_image_id, image_entity.image_type
    )
    assert_that(loaded, equal_to(created))


@pytest.mark.asyncio
async def test_get_by_external_id_and_type_not_found(storage, image_entity):
    await storage.image.create(image_entity)

    if image_entity.image_type == ImageType.MASTERCARD_CARD_IMAGE:
        image_type = ImageType.VISA_CARD_IMAGE
    else:
        image_type = ImageType.MASTERCARD_CARD_IMAGE
    with pytest.raises(Image.DoesNotExist):
        await storage.image.get_by_external_id_and_type(
            image_entity.external_image_id, image_type
        )

    with pytest.raises(Image.DoesNotExist):
        await storage.image.get_by_external_id_and_type(
            str(uuid4()), image_entity.image_type
        )
