import hashlib
import uuid
from dataclasses import replace

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains, has_entries, has_properties, has_property, instance_of

from billing.yandex_pay.yandex_pay.core.actions.image.mastercard import UpdateCardImageAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import ImageType, TSPType
from billing.yandex_pay.yandex_pay.core.entities.image import Image
from billing.yandex_pay.yandex_pay.interactions import AvatarsClient

PREDEFINED_CARD_ID = 'aaf024bb-2f0e-4cad-9010-40328ffcae9a'
OWNER_UID = 5555
FAKE_AVATARS_URL = 'fake/avatars/url'


@pytest.fixture
def card_entity():
    return Card(
        trust_card_id='fake_id',
        owner_uid=OWNER_UID,
        tsp=TSPType.MASTERCARD,
        expire=utcnow(),
        last4='0000',
        card_id=uuid.UUID(PREDEFINED_CARD_ID),
    )


@pytest.fixture(autouse=True)
async def card(storage, card_entity):
    return await storage.card.create(card_entity)


@pytest.fixture
def image_type(request):
    return getattr(request, 'param', ImageType.MASTERCARD_CARD_IMAGE)


@pytest.fixture
def fake_image_data():
    return b'fake_image_data'


@pytest.fixture
def fake_image_data_sha(fake_image_data):
    return hashlib.sha256(fake_image_data, usedforsecurity=False).hexdigest()


@pytest.fixture
def external_image_id():
    return str(uuid.uuid4())


@pytest.fixture(autouse=True)
def mocked_avatars_upload(mocker):
    response = mocker.Mock(url=FAKE_AVATARS_URL)
    return mocker.patch.object(AvatarsClient, 'upload', return_value=response)


@pytest.fixture
def mock_external_image_id(mocker, external_image_id):
    return mocker.patch.object(
        UpdateCardImageAction,
        'external_image_id',
        mocker.Mock(return_value=external_image_id),
    )


@pytest.fixture
def mock_load_image(mocker, fake_image_data):
    return mocker.patch.object(
        UpdateCardImageAction,
        'load_image',
        mocker.AsyncMock(return_value=fake_image_data),
    )


@pytest.fixture(autouse=True)
def patch_class_image_action(mocker, image_type):
    mocker.patch.object(UpdateCardImageAction, 'image_type', image_type, create=True)


@pytest.mark.asyncio
@pytest.mark.parametrize('image_type', list(ImageType))
async def test_image_created(
    card,
    image_type,
    external_image_id,
    fake_image_data_sha,
    storage,
    mock_external_image_id,
    mock_load_image,
    product_logs,
):
    await UpdateCardImageAction(card_id=card.card_id).run()

    image = await storage.image.get_by_external_id_and_type(
        external_image_id, image_type
    )

    assert_that(
        image,
        has_properties(
            image_type=image_type,
            avatars_url=FAKE_AVATARS_URL,
            sha256=fake_image_data_sha,
            image_id=instance_of(int),
            external_image_id=external_image_id,
        )
    )

    [product_log] = product_logs()
    assert_that(
        product_log,
        has_properties(
            message='Card image added',
            _context=has_entries(
                uid=card.owner_uid,
                card={'card_id': card.card_id, 'pan_last4': card.last4},
                image={
                    'image_type': image.image_type,
                    'image_id': image.image_id,
                    'image_sha256': image.sha256,
                    'external_image_id': image.external_image_id,
                    'avatars_url': image.avatars_url,
                }
            )
        )
    )


@pytest.mark.asyncio
async def test_duplicate_image_not_created(
    card,
    image_type,
    fake_image_data,
    card_entity,
    external_image_id,
    fake_image_data_sha,
    storage,
    mock_external_image_id,
    mock_load_image,
    mocked_avatars_upload,
):
    await UpdateCardImageAction(card_id=card.card_id).run()

    new_card = replace(card_entity, trust_card_id='fake_id2', card_id=uuid.uuid4())
    await storage.card.create(new_card)

    await UpdateCardImageAction(card_id=new_card.card_id,).run()

    image = await storage.image.get_by_external_id_and_type(
        external_image_id, image_type
    )

    card1 = await storage.card.get(card.card_id)
    card2 = await storage.card.get(new_card.card_id)
    for card in (card1, card2):
        assert_that(card, has_property('image_id', image.image_id))

    mocked_avatars_upload.assert_called_once_with(fake_image_data)


@pytest.mark.asyncio
async def test_new_image_not_created_if_hash_matches_existing(
    card,
    image_type,
    fake_image_data,
    card_entity,
    external_image_id,
    fake_image_data_sha,
    storage,
    mocker,
    mock_load_image,
    mocked_avatars_upload,
):
    mocker.patch.object(
        UpdateCardImageAction,
        'external_image_id',
        mocker.Mock(return_value=external_image_id),
    )
    mocker.patch.object(UpdateCardImageAction, 'image_type', image_type, create=True)

    await UpdateCardImageAction(card_id=card.card_id).run()

    new_card = replace(card_entity, trust_card_id='fake_id2', card_id=uuid.uuid4())
    await storage.card.create(new_card)

    another_image_id = str(uuid.uuid4())
    mocker.patch.object(
        UpdateCardImageAction,
        'external_image_id',
        mocker.Mock(return_value=another_image_id),
    )

    await UpdateCardImageAction(card_id=new_card.card_id).run()

    image = await storage.image.get_by_external_id_and_type(
        external_image_id, image_type
    )

    with pytest.raises(Image.DoesNotExist):
        await storage.image.get_by_external_id_and_type(
            another_image_id, image_type
        )

    assert_that(
        image, has_property('external_image_id', external_image_id)
    )

    card1 = await storage.card.get(card.card_id)
    card2 = await storage.card.get(new_card.card_id)
    for card in (card1, card2):
        assert_that(card, has_property('image_id', image.image_id))

    mocked_avatars_upload.assert_called_once_with(fake_image_data)


@pytest.mark.asyncio
async def test_replace_image_for_card(
    card,
    image_type,
    fake_image_data,
    card_entity,
    external_image_id,
    fake_image_data_sha,
    storage,
    mocker,
    mock_external_image_id,
    mock_load_image,
    mocked_avatars_upload,
    product_logs,
):
    await UpdateCardImageAction(card_id=PREDEFINED_CARD_ID).run()

    image = await storage.image.get_by_external_id_and_type(
        external_image_id, image_type
    )

    another_image_id = 'https://another.test/another.png'
    new_image = replace(
        image,
        sha256='new_fake_sha',
        image_id=None,
        external_image_id=another_image_id,
    )
    new_image = await storage.image.create(new_image)
    assert_that(new_image, has_property('image_id', instance_of(int)))

    mocker.patch.object(
        UpdateCardImageAction,
        'external_image_id',
        mocker.Mock(return_value=another_image_id),
    )

    await UpdateCardImageAction(card_id=card.card_id).run()

    card = await storage.card.get(card.card_id)
    assert_that(
        card, has_property('image_id', new_image.image_id)
    )

    logs = product_logs()
    assert_that(
        logs,
        contains(
            has_properties(
                message='Card image added',
                _context=has_entries(
                    uid=card.owner_uid,
                    card={'card_id': card.card_id, 'pan_last4': card.last4},
                    image={
                        'image_type': image.image_type,
                        'image_id': image.image_id,
                        'image_sha256': image.sha256,
                        'external_image_id': image.external_image_id,
                        'avatars_url': image.avatars_url,
                    }
                )
            ),
            has_properties(
                message='Card image updated',
                _context=has_entries(
                    uid=card.owner_uid,
                    card={'card_id': card.card_id, 'pan_last4': card.last4},
                    image={
                        'image_type': new_image.image_type,
                        'image_id': new_image.image_id,
                        'image_sha256': new_image.sha256,
                        'external_image_id': new_image.external_image_id,
                        'avatars_url': new_image.avatars_url,
                        'previous_image_id': image.image_id,
                    }
                )
            )
        )
    )
