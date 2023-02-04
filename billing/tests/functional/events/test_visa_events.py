import base64
import hashlib
import re
from datetime import datetime, timedelta, timezone
from uuid import uuid4

import pytest

from sendr_interactions.clients.avatars.entities import UploadResult
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_properties

from billing.yandex_pay.yandex_pay.api.schemas.events.visa import VisaTokenStatusUpdateType
from billing.yandex_pay.yandex_pay.core.actions.image.visa import UpdateVisaCardImageAction
from billing.yandex_pay.yandex_pay.core.actions.visa_metadata import VisaUpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import (
    ImageType, TaskType, TSPTokenStatus, TSPType, VisaTokenStatus
)
from billing.yandex_pay.yandex_pay.interactions import AvatarsClient, DuckGoClient


@pytest.fixture
def provisioned_token_id():
    return str(uuid4())


@pytest.fixture
def pan_enrollment_id():
    return str(uuid4())


@pytest.fixture
def api_key():
    return 'api-key'


@pytest.fixture(autouse=True)
async def clear_tasks(storage):
    async def clear():
        for task in await alist(storage.task.find()):
            await storage.task.delete(task)

    await clear()
    yield
    await clear()


@pytest.fixture
async def card(storage, randn):
    card = await storage.card.create(
        Card(
            trust_card_id='trust-card-id',
            owner_uid=randn(),
            tsp=TSPType.VISA,
            expire=utcnow() + timedelta(days=30),
            last4='0000',
            card_id=uuid4(),
        )
    )
    yield card

    await storage.card.delete(card)


@pytest.fixture
async def card_enrollment(storage, card, provisioned_token_id, pan_enrollment_id) -> Enrollment:
    enrollment = await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=pan_enrollment_id,
            tsp_token_id=provisioned_token_id,
            card_last4=card.last4,
        )
    )
    yield enrollment
    await storage.enrollment.delete(enrollment)


@pytest.fixture(autouse=True)
def mock_duckgo_visa_verify_request(mocker):
    return mocker.patch.object(
        DuckGoClient,
        'visa_verify_request',
        return_value=None,
    )


class TestTokenStatusUpdateNotification:

    @pytest.fixture
    def token_status_update_notification(self, provisioned_token_id):
        return {
            'date': int(utcnow().timestamp()),
            'vProvisionedTokenID': provisioned_token_id
        }

    @pytest.fixture
    def params(self, api_key):
        return {
            'apiKey': api_key,
            'eventType': VisaTokenStatusUpdateType.TOKEN_STATUS_UPDATED.value,
            'sync': 1,
        }

    @pytest.fixture
    def visa_token_status_response_body(self, tsp_token_status):
        return {
            "deviceBindingInfoList":
                [{
                    "clientDeviceID": "9C4...72",
                    "deviceName": "...",
                    "status": "CHALLENGED"
                }],
            "tokenInfo": {
                "tokenStatus": tsp_token_status.value,
                "ignore01field": "D2553045",
                "expirationDate": {
                    "year": "2022",
                    "month": "12"
                }}
        }

    @pytest.fixture()
    def mock_visa_client_urls(
        self,
        aioresponses_mocker,
        yandex_pay_settings,
        visa_token_status_response_body,
    ):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.ZORA_URL}.*'),
            status=200,
            payload=visa_token_status_response_body,
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'tsp_token_status',
        [each for each in VisaTokenStatus if each != VisaTokenStatus.DELETED],
    )
    async def test_token_status_update_notification(
        self,
        app,
        storage,
        card,
        provisioned_token_id,
        pan_enrollment_id,
        card_enrollment,
        params,
        tsp_token_status,
        mock_visa_client_urls,
        token_status_update_notification,
    ):
        response = await app.post(
            'events/visa/v1/provisionedToken',
            params=params,
            json=token_status_update_notification
        )

        assert_that(response.status, equal_to(200))
        json_body = await response.json()
        assert_that(json_body, equal_to({}))

        enrollment = await storage.enrollment.get(card_enrollment.enrollment_id)
        expected_status = TSPTokenStatus.from_visa_status(tsp_token_status)
        assert_that(
            enrollment,
            has_properties(
                tsp_token_status=expected_status,
                expire=datetime(2022, 12, 31, tzinfo=timezone.utc),
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'tsp_token_status',
        [VisaTokenStatus.DELETED],
    )
    async def test_token_status_update_notification_should_mark_enrollment_as_deleted(
        self,
        app,
        storage,
        card,
        provisioned_token_id,
        pan_enrollment_id,
        card_enrollment,
        tsp_token_status,
        params,
        token_status_update_notification,
        visa_token_status_response_body,
        mock_duckgo_visa_verify_request,
        mock_visa_client_urls,
    ):
        response = await app.post(
            'events/visa/v1/provisionedToken',
            params=params,
            json=token_status_update_notification)

        assert_that(response.status, equal_to(200))
        json_body = await response.json()
        assert_that(json_body, equal_to({}))

        mock_duckgo_visa_verify_request.assert_called_once()

        enrollment = await storage.enrollment.get(card_enrollment.enrollment_id)
        assert enrollment.tsp_token_status == TSPTokenStatus.DELETED


class TestCardMetaDataUpdateNotification:

    @pytest.fixture
    def bg_color(self):
        return '0x000000'

    @pytest.fixture
    def guid(self):
        return str(uuid4())

    @pytest.fixture
    def visa_card_metadata_response_body(self, guid, bg_color):
        return {
            "paymentInstrument": {
                "last4": "0507",
                "expirationDate": {
                    "ignore02field": "FA238F20",
                    "month": "12",
                    "year": "2022"
                },
                "cvv2PrintedInd": "Y",
                "ignore01field": "E100786F",
                "expDatePrintedInd": "Y",
                "paymentAccountReference": "V0010013020294400836524608503",
                "enabledServices": {
                    "merchantPresentedQR": "N"
                }
            },
            "vPanEnrollmentID": "7d6a589ca81a1bf7b15d1307413daa01",
            "tokens": [
                {
                    "vProvisionedTokenID": "2af631b105f5aa692c8a13f635173401",
                    "tokenStatus": "ACTIVE"
                }
            ],
            "cardMetaData": {
                "backgroundColor": bg_color,
                "foregroundColor": "0x7dffff",
                "labelColor": "0x000000",
                "contactWebsite": "https://www.visatest.com",
                "contactEmail": "test@visa.com",
                "contactNumber": "1800768999",
                "contactName": "visaTest",
                "privacyPolicyURL": "https://prefix.html",
                "termsAndConditionsURL": "https://terms.html",
                "shortDescription": "FNDB Visa Classic Card",
                "longDescription": "Visa Test Only Card",
                "cardData": [
                    {
                        "guid": "a0b72f0e60284f4f9809c9375557a77c",
                        "contentType": "cardSymbol",
                        "content": [
                            {
                                "mimeType": "image/png",
                                "width": "100",
                                "height": "100"
                            }
                        ]
                    },
                    {
                        "guid": guid,
                        "contentType": "digitalCardArt",
                        "content": [
                            {
                                "mimeType": "image/png",
                                "width": "1536",
                                "height": "969"
                            }
                        ]
                    }
                ],
            }
        }

    @pytest.fixture
    def img_data(self) -> str:
        return base64.b64encode(b'thisisimagedata').decode('utf-8')

    @pytest.fixture
    def visa_content_response_body(self, img_data):
        return {
            "altText": "cardSymbol",
            "contentType": "cardSymbol",
            "content": [
                {
                    "mimeType": "image/png",
                    "width": "100",
                    "height": "100",
                    "encodedData": img_data
                }
            ]
        }

    @pytest.fixture
    def card_metadata_update_notification(self, pan_enrollment_id):
        return {
            'date': int(utcnow().timestamp()),
            'vPanEnrollmentID': pan_enrollment_id
        }

    @pytest.fixture
    def params(self, api_key):
        return {
            'apiKey': api_key,
            'sync': 1,
        }

    @pytest.fixture(autouse=True)
    def mock_visa_card_metadata_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_card_metadata_response_body,
    ):
        return aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.ZORA_URL}.*'),
            status=200,
            payload=visa_card_metadata_response_body,
        )

    @pytest.fixture(autouse=True)
    def mock_visa_content_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_content_response_body,
    ):
        return aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.ZORA_URL}.*'),
            status=200,
            payload=visa_content_response_body,
        )

    @pytest.fixture(autouse=True)
    def mock_avatars_upload(self, mocker):
        return mocker.patch.object(
            AvatarsClient,
            'upload',
            mocker.AsyncMock(return_value=UploadResult('unittest-ns', 'unittest-gr', 'uploaded-image')),
        )

    @pytest.mark.asyncio
    async def test_card_metadata_update_notification(
        self,
        app,
        storage,
        card,
        guid,
        run_action,
        params,
        img_data,
        bg_color,
        pan_enrollment_id,
        card_enrollment,
        card_metadata_update_notification,
    ):
        response = await app.post(
            'events/visa/v1/panMetadata',
            params=params,
            json=card_metadata_update_notification)

        assert_that(response.status, equal_to(200))
        json_body = await response.json()
        assert_that(json_body, equal_to({}))

        # проверим, что создался таск на апдейт и подтолкнем его руками
        created_tasks = await alist(storage.task.find())
        assert_that(len(created_tasks), equal_to(1))

        created_task = created_tasks[0]
        expected_params = {
            'max_retries': 20,
            'action_kwargs': {
                'enrollment_id': str(card_enrollment.enrollment_id),
                'pan_enrollment_id': card_metadata_update_notification['vPanEnrollmentID'],
            }
        }

        assert_that(
            created_task,
            has_properties(
                params=expected_params,
                task_type=TaskType.RUN_ACTION,
                action_name=VisaUpdateEnrollmentMetadataAction.action_name
            )
        )

        await run_action(
            action_cls=VisaUpdateEnrollmentMetadataAction,
            action_kwargs=created_task.params["action_kwargs"],
        )

        filters = {
            'task_type': 'run_action',
            'action_name': UpdateVisaCardImageAction.action_name,
        }
        update_card_image_tasks = await alist(
            storage.task.find(filters=filters, order=('task_id',))
        )

        assert_that(len(update_card_image_tasks), equal_to(1))
        update_card_image_task = update_card_image_tasks[0]

        expected_image_update_properties = {
            'params': {
                "max_retries": 10,
                "action_kwargs": {
                    "card_id": str(card.card_id),
                    "guid": guid,
                }
            },
            'state': TaskState.PENDING
        }

        assert_that(update_card_image_task, has_properties(expected_image_update_properties))
        await run_action(
            action_cls=UpdateVisaCardImageAction,
            action_kwargs=update_card_image_task.params["action_kwargs"],
        )

        img_hash = hashlib.sha256(
            base64.b64decode(img_data.encode('utf-8')),
            usedforsecurity=False  # type: ignore
        ).hexdigest()

        image = await storage.image.get_by_sha(img_hash)
        assert_that(
            image.image_type,
            equal_to(ImageType.VISA_CARD_IMAGE)
        )
        updated_card = await storage.card.find_ensure_one(
            filters={'card_id': card.card_id},
            for_update=False
        )

        assert_that(
            updated_card.image_id,
            equal_to(image.image_id)
        )
