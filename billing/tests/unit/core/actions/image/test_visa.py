import base64
import uuid

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.image.visa import UpdateVisaCardImageAction
from billing.yandex_pay.yandex_pay.core.entities.enums import ImageType
from billing.yandex_pay.yandex_pay.interactions import VisaClient
from billing.yandex_pay.yandex_pay.interactions.visa import VisaContentResult
from billing.yandex_pay.yandex_pay.interactions.visa.entities.content import Content


@pytest.fixture
def fake_image_data():
    return b'fake_image_data'


@pytest.fixture
def fake_content_response(fake_image_data) -> VisaContentResult:
    return VisaContentResult(
        alt_text='alt_text',
        content_type='image/png',
        content=[
            Content(
                mime_type='image/png',
                width=1024,
                height=1024,
                encoded_data=base64.b64encode(fake_image_data),
            ),
        ]
    )


@pytest.fixture(autouse=True)
def mocked_image_data_request(mocker, fake_content_response):
    return mocker.patch.object(
        VisaClient, 'get_content', return_value=fake_content_response
    )


@pytest.fixture
def guid() -> str:
    return "some-image-id"


@pytest.fixture
def card_id() -> uuid.UUID:
    return uuid.uuid4()


@pytest.mark.asyncio
async def test_can_load_image(
    guid,
    card_id,
    fake_image_data,
    mocked_image_data_request,
):
    action = UpdateVisaCardImageAction(guid, card_id)
    img_bytes = await action.load_image()

    assert_that(img_bytes, equal_to(fake_image_data))
    mocked_image_data_request.assert_called_once_with(guid)


def test_should_return_correct_external_image_id(
    guid,
    card_id,
):
    id = UpdateVisaCardImageAction(guid, card_id).external_image_id()
    assert_that(id, equal_to(guid))


def test_image_type_is_visa():
    assert_that(UpdateVisaCardImageAction.image_type, equal_to(ImageType.VISA_CARD_IMAGE))
