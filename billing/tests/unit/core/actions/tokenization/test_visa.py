import base64
import uuid
from copy import deepcopy
from dataclasses import replace
from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock

import pytest

from sendr_interactions.clients.avatars.entities import UploadResult
from sendr_utils import alist, utcnow

from hamcrest import (
    all_of, assert_that, equal_to, has_entries, has_length, has_properties, has_property, match_equality
)

from billing.yandex_pay.yandex_pay.core.actions.tokenization.visa import VisaTokenizationAction
from billing.yandex_pay.yandex_pay.core.actions.visa_metadata import VisaUpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TaskType, TSPTokenStatus, TSPType, VisaTokenStatus
from billing.yandex_pay.yandex_pay.interactions import AvatarsClient, CardProxyClient, TrustPaysysClient, VisaClient
from billing.yandex_pay.yandex_pay.interactions.cardproxy import VisaEnrollment
from billing.yandex_pay.yandex_pay.interactions.cardproxy.entities import ExpirationDate, PaymentInstrument, TokenInfo
from billing.yandex_pay.yandex_pay.interactions.trust_paysys import TrustPaysysCardInfo
from billing.yandex_pay.yandex_pay.interactions.visa import VisaCardMetaDataResult, VisaContentResult
from billing.yandex_pay.yandex_pay.interactions.visa.entities.content import Content
from billing.yandex_pay.yandex_pay.interactions.visa.entities.metadata import CardData, CardMetaData
from billing.yandex_pay.yandex_pay.tests.matchers import close_to_datetime

FAKE_PAR = 'fake_payment_account_reference'
FAKE_PAN_LAST4 = '2222'
FAKE_TOKEN_LAST4 = '3333'


@pytest.fixture
async def setup_card(storage):
    return await storage.card.create(
        Card(
            trust_card_id='some_trust_id',
            owner_uid=1,
            tsp=TSPType.VISA,
            expire=utcnow(),
            last4='1234',
        )
    )


@pytest.fixture
def card_in_storage(setup_card) -> Card:
    return setup_card


@pytest.fixture
def card_id_param(card_in_storage):
    return {
        'card_id': card_in_storage.card_id,
    }


@pytest.fixture
def extra_action_params(request):
    return deepcopy(getattr(request, 'param', {}))


@pytest.fixture
def tokenization_action(
    card_id_param, extra_action_params
) -> VisaTokenizationAction:
    return VisaTokenizationAction(**card_id_param, **extra_action_params)


@pytest.fixture
def trust_card(card_in_storage) -> TrustPaysysCardInfo:
    return TrustPaysysCardInfo(
        card_id=card_in_storage.trust_card_id,
        card_token='pci_card_token',
        holder='holder',
        expiration_year=2025,
        expiration_month=10,
    )


@pytest.fixture
def cardproxy_token_status(request):
    return getattr(request, 'param', VisaTokenStatus.ACTIVE.value)


@pytest.fixture
def visa_enrollment(cardproxy_token_status) -> VisaEnrollment:
    expiration_date = ExpirationDate(
        year=2030,
        month=12
    )
    token_info = TokenInfo(
        last4=FAKE_TOKEN_LAST4,
        expiration_date=replace(expiration_date, year=2029),
        status=VisaTokenStatus(cardproxy_token_status)
    )
    payment_instrument = PaymentInstrument(
        last4=FAKE_PAN_LAST4,
        expiration_date=expiration_date,
        payment_account_reference=FAKE_PAR
    )

    pan_enrollment_id = str(uuid.uuid4())

    return VisaEnrollment(
        provisioned_token_id=str(uuid.uuid4()),
        pan_enrollment_id=pan_enrollment_id,
        payment_instrument=payment_instrument,
        token_info=token_info
    )


@pytest.fixture(autouse=True)
def trust_paysys_mock(mocker, trust_card) -> AsyncMock:
    return mocker.patch.object(TrustPaysysClient, 'get_card', AsyncMock(return_value=trust_card))


@pytest.fixture(autouse=True)
def cardproxy_mock(mocker, visa_enrollment) -> AsyncMock:
    return mocker.patch.object(
        CardProxyClient,
        'visa_enrollment',
        AsyncMock(return_value=visa_enrollment),
    )


@pytest.fixture
def visa_card_metadata_result():
    return VisaCardMetaDataResult(
        enrollment_id=str(uuid.uuid4()),
        payment_instrument=PaymentInstrument(
            last4='1111',
            expiration_date=ExpirationDate(month=12, year=2030),
            payment_account_reference='PAR-1111'
        ),
        tokens=[],
        card_metadata=CardMetaData(
            background_color="0x000000",
            foreground_color="0x7dffff",
            label_color="0x000000",
            contact_website="https://www.visatest.com",
            contact_email="test@visa.com",
            contact_number="1800768999",
            contact_name="visaTest",
            privacy_policy_url="https://prefix.html",
            terms_and_conditions_url="https://terms.html",
            short_description="FNDB Visa Classic Card",
            long_description="Visa Test Only Card",
            card_data=[
                CardData(
                    guid='wrong-guid',
                    content_type='notDigitalCardArt',
                    content=[
                        Content(
                            mime_type='image/png',
                            width=100,
                            height=100,
                            encoded_data=None
                        ),
                    ]),
                CardData(
                    guid=str(uuid.uuid4()),
                    content_type='digitalCardArt',
                    content=[
                        Content(
                            mime_type='image/png',
                            width=100,
                            height=100,
                            encoded_data=None
                        ),
                    ]),
            ]
        )
    )


@pytest.fixture
def visa_content_result():
    content = Content(
        mime_type='image/png',
        width=100,
        height=100,
        encoded_data=base64.b64encode('hello image'.encode('utf-8')).decode('utf-8'),
    )
    return VisaContentResult(
        alt_text='alt-text', content_type='digitalCardArt', content=[content]
    )


@pytest.fixture(autouse=True)
def mock_visa_client_mock(mocker, visa_card_metadata_result):
    return mocker.patch.object(
        VisaClient,
        'card_metadata',
        mocker.AsyncMock(return_value=visa_card_metadata_result),
    )


@pytest.fixture(autouse=True)
def mock_visa_content(mocker, visa_content_result):
    return mocker.patch.object(
        VisaClient,
        'get_content',
        mocker.AsyncMock(return_value=visa_content_result),
    )


@pytest.fixture
def avatars_upload_result():
    return UploadResult('unittest-ns', 'unittest-gr', 'uploaded-image')


@pytest.fixture
async def avatars_client(create_client):
    client = create_client(AvatarsClient)
    yield client
    await client.close()


@pytest.fixture(autouse=True)
def mock_avatars_upload(mocker, avatars_upload_result):
    return mocker.patch.object(
        AvatarsClient,
        'upload',
        mocker.AsyncMock(return_value=avatars_upload_result),
    )


@pytest.fixture
def mock_visa_update_metadata_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.core.actions.'
        'tokenization.visa.VisaUpdateEnrollmentMetadataAction'
    )
    mock_action_cls.return_value.run_async = mock_run
    return mock_action_cls


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'cardproxy_token_status',
    [each.value for each in VisaTokenStatus],
    indirect=True,
)
async def test_visa_tokenization_should_create_enrollment(
    tokenization_action,
    storage,
    card_in_storage,
    cardproxy_token_status,
    visa_enrollment,
    product_logs,
):
    await tokenization_action.run()

    enrollment = await storage.enrollment.get_by_card_id_and_merchant_id(
        card_id=card_in_storage.card_id, merchant_id=None
    )

    visa_status = VisaTokenStatus(cardproxy_token_status)
    expected_tsp_token_status = TSPTokenStatus.from_visa_status(visa_status)
    expected_token_expiration = datetime(2029, 12, 31, tzinfo=timezone.utc)
    expected_properties = {
        'tsp_token_id': visa_enrollment.provisioned_token_id,
        'tsp_card_id': visa_enrollment.pan_enrollment_id,
        'tsp_token_status': equal_to(expected_tsp_token_status),
        'expire': expected_token_expiration,
    }

    assert_that(enrollment, has_properties(expected_properties))

    [log] = product_logs()
    assert_that(
        log,
        has_properties(
            message='Tokenization done',
            _context=has_entries(
                tsp='visa',
                card={
                    'card_id': card_in_storage.card_id,
                    'pan_last4': card_in_storage.last4,
                },
                enrollment=has_entries(
                    enrollment_id=enrollment.enrollment_id,
                    merchant_id=enrollment.merchant_id,
                    tsp_token_id=enrollment.tsp_token_id,
                    tsp_card_id=enrollment.tsp_card_id,
                    tsp_token_status=enrollment.tsp_token_status,
                    expire=expected_token_expiration,
                ),
                visa_token_info={
                    'token_requestor_id': None,
                    'token_reference_id': None,
                },
                metadata=has_entries(
                    payment_account_reference=FAKE_PAR,
                    token_last4=FAKE_TOKEN_LAST4,
                    pan_enrollment_id=visa_enrollment.pan_enrollment_id,
                    vts_correlation_id=None,
                    token_requestor_id=None,
                    token_reference_id=None,
                ),
            ),
        )
    )


@pytest.mark.asyncio
async def test_visa_tokenization_should_update_metadata(
    tokenization_action,
    mock_visa_update_metadata_action,
    yandex_pay_settings,
):
    enrollment = await tokenization_action.run()

    mock_visa_update_metadata_action.assert_called_once_with(
        enrollment_id=enrollment.enrollment_id,
        pan_enrollment_id=enrollment.tsp_card_id,
    )

    expected_run_at = utcnow() + timedelta(
        seconds=yandex_pay_settings.TASKQ_METADATA_TASK_DELAY_SECONDS
    )
    mock_visa_update_metadata_action.return_value.run_async.assert_awaited_once_with(
        run_at=match_equality(close_to_datetime(expected_run_at, timedelta(seconds=10)))
    )


@pytest.mark.asyncio
async def test_visa_tokenization_should_create_update_metadata_task(
    tokenization_action,
    storage,
):
    enrollment = await tokenization_action.run()

    filters = {
        'task_type': 'run_action',
        'action_name': VisaUpdateEnrollmentMetadataAction.action_name,
    }
    metadata_update_tasks = await alist(
        storage.task.find(filters=filters, order=('task_id',))
    )
    assert_that(metadata_update_tasks, has_length(1))
    metadata_update_task = metadata_update_tasks[0]

    expected_metadata_task_params = {
        'max_retries': 20,
        'action_kwargs': {
            'enrollment_id': str(enrollment.enrollment_id),
            'pan_enrollment_id': enrollment.tsp_card_id,
        }
    }
    assert_that(
        metadata_update_task,
        has_properties(
            params=expected_metadata_task_params,
            task_type=TaskType.RUN_ACTION,
            action_name=VisaUpdateEnrollmentMetadataAction.action_name
        )
    )


@pytest.mark.asyncio
async def test_visa_tokenization_should_call_clients_with_expected_params(
    tokenization_action,
    card_in_storage,
    trust_card,
    trust_paysys_mock,
    cardproxy_mock,
):
    await tokenization_action.run()

    trust_paysys_mock.assert_awaited_once_with(trust_card_id=card_in_storage.trust_card_id)
    cardproxy_mock.assert_awaited_once_with(
        uid=card_in_storage.owner_uid,
        pci_card_token=trust_card.card_token,
        expiration_month=trust_card.expiration_month,
        expiration_year=trust_card.expiration_year
    )


@pytest.mark.asyncio
async def test_can_re_tokenize_card_if_enrollment_already_exist(
    tokenization_action,
    storage,
    card_in_storage,
    visa_enrollment,
):
    exist_enrollment = await storage.enrollment.create(
        Enrollment(
            card_id=card_in_storage.card_id,
            merchant_id=None,
            tsp_card_id=None,
            tsp_token_id='some_old_token',
            tsp_token_status=TSPTokenStatus.DELETED,
            card_last4=card_in_storage.last4,
        )
    )
    await tokenization_action.run()

    enrollments_after_update = await alist(storage.enrollment.find(filters={'card_id': card_in_storage.card_id}))
    assert_that(len(enrollments_after_update), equal_to(1))

    updated_enrollment = enrollments_after_update[0]

    assert_that(
        updated_enrollment,
        all_of(
            has_property('tsp_token_id', visa_enrollment.provisioned_token_id),
            has_property('tsp_token_status', equal_to(TSPTokenStatus.ACTIVE)),
            has_property('enrollment_id', equal_to(exist_enrollment.enrollment_id))
        )
    )


def test_action_should_run_in_transaction():
    assert_that(VisaTokenizationAction.transact, equal_to(True))
