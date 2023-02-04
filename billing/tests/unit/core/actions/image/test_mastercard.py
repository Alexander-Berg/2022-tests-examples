import uuid

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.image.mastercard import UpdateMasterCardCardImageAction
from billing.yandex_pay.yandex_pay.core.entities.enums import ImageType
from billing.yandex_pay.yandex_pay.interactions import MasterCardClient


@pytest.fixture
def fake_image_data():
    return b'fake_image_data'


@pytest.fixture(autouse=True)
def mocked_image_data_request(mocker, fake_image_data):
    return mocker.patch.object(
        MasterCardClient, 'get_image', return_value=fake_image_data
    )


@pytest.fixture
def download_url() -> str:
    return "some-image-id"


@pytest.fixture
def card_id() -> uuid.UUID:
    return uuid.uuid4()


@pytest.mark.asyncio
async def test_can_load_image(
    download_url,
    card_id,
    fake_image_data,
    mocked_image_data_request,
):
    action = UpdateMasterCardCardImageAction(download_url, card_id)
    img_bytes = await action.load_image()

    assert_that(img_bytes, equal_to(fake_image_data))
    mocked_image_data_request.assert_called_once_with(download_url)


def test_should_return_correct_external_image_id(
    download_url,
    card_id,
):
    id = UpdateMasterCardCardImageAction(download_url, card_id).external_image_id()
    assert_that(id, equal_to(download_url))


def test_image_type_is_mc():
    assert_that(UpdateMasterCardCardImageAction.image_type, equal_to(ImageType.MASTERCARD_CARD_IMAGE))
