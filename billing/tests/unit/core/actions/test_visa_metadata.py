import logging
import uuid
from datetime import timedelta

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_properties, is_

from billing.yandex_pay.yandex_pay.core.actions.visa_metadata import VisaUpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.exceptions import CoreCardRemovedError, CoreMissingCardMetadataError
from billing.yandex_pay.yandex_pay.interactions import VisaClient
from billing.yandex_pay.yandex_pay.interactions.visa import VisaCardMetaDataResult
from billing.yandex_pay.yandex_pay.interactions.visa.entities.content import Content
from billing.yandex_pay.yandex_pay.interactions.visa.entities.metadata import (
    CardData, CardMetaData, ExpirationDate, PaymentInstrument
)
from billing.yandex_pay.yandex_pay.interactions.visa.exceptions import InternalVisaError


@pytest.fixture
def pan_enrollment_id():
    return str(uuid.uuid4())


@pytest.fixture
def enrollment_id() -> uuid.UUID:
    return uuid.uuid4()


@pytest.fixture
async def card(storage, randn):
    return await storage.card.create(
        Card(
            trust_card_id='card-x123abc',
            owner_uid=randn(),
            tsp=TSPType.MASTERCARD,
            expire=utcnow() + timedelta(days=30),
            last4='0000',
        )
    )


@pytest.fixture
async def enrollment(storage, card, enrollment_id):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_card_id='tsp_card_id',
            tsp_token_id='tsp_token_id',
            tsp_token_status=TSPTokenStatus.ACTIVE,
            enrollment_id=enrollment_id,
            expire=card.expire,
            card_last4=card.last4,
        )
    )


@pytest.fixture
def mock_update_enrollment_metadata_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.core.actions.'
        'visa_metadata.UpdateEnrollmentMetadataAction'
    )
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


class TestVisaMetadataWorks:
    @pytest.fixture
    def visa_card_metadata_result(self, pan_enrollment_id):
        return VisaCardMetaDataResult(
            pan_enrollment_id,
            payment_instrument=PaymentInstrument(
                last4='1111',
                expiration_date=ExpirationDate(month=12, year=2030),
                payment_account_reference='PAR-1111'
            ),
            tokens=[],
            card_metadata=CardMetaData(
                background_color='0FAFCF',
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
    def mock_visa_card_metadata(self, mocker, visa_card_metadata_result):
        return mocker.patch.object(
            VisaClient,
            'card_metadata',
            mocker.AsyncMock(return_value=visa_card_metadata_result),
        )

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('enrollment')
    async def test_visa_metadata_valid_request(
        self,
        enrollment_id: uuid.UUID,
        pan_enrollment_id: str,
        visa_card_metadata_result: VisaCardMetaDataResult,
        mock_visa_card_metadata,
        mock_update_enrollment_metadata_action,
        storage,
    ):
        params = {
            'enrollment_id': enrollment_id,
            'pan_enrollment_id': pan_enrollment_id
        }

        await VisaUpdateEnrollmentMetadataAction(**params).run()

        mock_visa_card_metadata.assert_called_once_with(pan_enrollment_id)
        mock_update_enrollment_metadata_action.assert_called_once_with(
            raw_tsp_metadata=visa_card_metadata_result.get_metadata(),
            enrollment_id=enrollment_id,
            tsp=TSPType.VISA,
        )

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('enrollment', 'mock_update_enrollment_metadata_action', 'mock_visa_card_metadata')
    async def test_visa_metadata_should_update_last4_expire(
        self,
        enrollment_id: uuid.UUID,
        pan_enrollment_id: str,
        visa_card_metadata_result: VisaCardMetaDataResult,
        storage,
    ):
        params = {
            'enrollment_id': enrollment_id,
            'pan_enrollment_id': pan_enrollment_id
        }

        enrollment_before = await storage.enrollment.get(enrollment_id)
        await VisaUpdateEnrollmentMetadataAction(**params).run()
        enrollment_after = await storage.enrollment.get(enrollment_id)

        assert enrollment_before.card_last4 != enrollment_after.card_last4
        assert enrollment_after.card_last4 == visa_card_metadata_result.payment_instrument.last4

        assert enrollment_before.expire != enrollment_after.expire
        assert enrollment_after.expire == visa_card_metadata_result.payment_instrument.get_pan_expiration_date()

    @pytest.fixture
    def mock_visa_card_metadata_without_card_data(self, aioresponses_mocker, yandex_pay_settings, pan_enrollment_id):
        return aioresponses_mocker.get(yandex_pay_settings.ZORA_URL, payload={
            'vPanEnrollmentID': pan_enrollment_id,
            'paymentInstrument': {
                'expirationDate': {'month': '07', 'year': '2021'},
                'last4': '1111',
            },
            'tokens': [],
            'cardMetaData': {'privacyPolicyURL': 'https://www.atfbank.kz/'}
        })

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('enrollment')
    async def test_visa_metadata_without_card_data(
        self,
        enrollment_id,
        pan_enrollment_id,
        mock_update_enrollment_metadata_action,
        mock_visa_card_metadata_without_card_data,
    ):
        params = {
            'enrollment_id': enrollment_id,
            'pan_enrollment_id': pan_enrollment_id
        }

        await VisaUpdateEnrollmentMetadataAction(**params).run()

        mock_update_enrollment_metadata_action.assert_called_once_with(
            raw_tsp_metadata={
                'payment_instrument': {
                    'expiration_date': {'month': 7, 'year': 2021},
                    'last4': '1111',
                },
                'card_metadata': {'privacy_policy_url': 'https://www.atfbank.kz/'}
            },
            enrollment_id=enrollment_id,
            tsp=TSPType.VISA,
        )


def test_serialize_kwargs(
    enrollment_id: uuid.UUID,
    pan_enrollment_id: str,
):
    action = VisaUpdateEnrollmentMetadataAction(
        enrollment_id=enrollment_id,
        pan_enrollment_id=pan_enrollment_id
    )

    expected_serialized_kwargs = {
        'enrollment_id': str(enrollment_id),
        'pan_enrollment_id': str(pan_enrollment_id),
    }

    assert_that(
        action.serialize_kwargs(action._init_kwargs),
        equal_to(expected_serialized_kwargs)
    )


def test_deserialize_kwargs(
    enrollment_id: uuid.UUID,
    pan_enrollment_id: str,
):
    raw_params = {
        'enrollment_id': str(enrollment_id),
        'pan_enrollment_id': str(pan_enrollment_id),
    }

    expected_deserialized_kwargs = dict(
        enrollment_id=enrollment_id,
        pan_enrollment_id=pan_enrollment_id
    )

    assert_that(
        VisaUpdateEnrollmentMetadataAction.deserialize_kwargs(raw_params),
        equal_to(expected_deserialized_kwargs),
    )


@pytest.mark.parametrize(
    'exception,expected',
    [
        (CoreCardRemovedError(), False),
        (Exception, None),
    ],
)
def test_should_retry_exception(exception, expected):
    assert_that(
        VisaUpdateEnrollmentMetadataAction.should_retry_exception(exception),
        is_(expected),
    )


class TestVisaMetadataRaisingErrors:
    @pytest.fixture
    def mock_visa_card_metadata_error(self, mocker):
        return mocker.patch.object(
            VisaClient,
            'card_metadata',
            mocker.AsyncMock(
                side_effect=InternalVisaError(
                    status_code='InternalError',
                    method='GET',
                    service='VISA',
                    response_status='fail reason',
                    params={'a': 'b'}
                )
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('enrollment')
    async def test_visa_metadata_bad_visa_response(
        self,
        enrollment_id: uuid.UUID,
        pan_enrollment_id: str,
        mock_visa_card_metadata_error,
    ):
        params = {
            'enrollment_id': enrollment_id,
            'pan_enrollment_id': pan_enrollment_id
        }

        with pytest.raises(CoreMissingCardMetadataError):
            await VisaUpdateEnrollmentMetadataAction(**params).run()

        mock_visa_card_metadata_error.assert_awaited_once_with(pan_enrollment_id)

    @pytest.mark.asyncio
    async def test_enrollment_does_not_exist(
        self,
        enrollment_id: uuid.UUID,
        pan_enrollment_id: str,
        mock_update_enrollment_metadata_action,
        dummy_logger,
        caplog,
    ):
        caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)
        params = {
            'enrollment_id': enrollment_id,
            'pan_enrollment_id': pan_enrollment_id
        }

        await VisaUpdateEnrollmentMetadataAction(**params).run()
        [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(
            log,
            has_properties(
                message='Enrollment not found',
                levelno=logging.WARNING,
            )
        )

        mock_update_enrollment_metadata_action.assert_not_called()

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('enrollment')
    async def test_card_is_removed(
        self,
        enrollment_id: uuid.UUID,
        pan_enrollment_id: str,
        card,
        storage,
        mock_update_enrollment_metadata_action,
    ):
        card.is_removed = True
        await storage.card.save(card)

        params = {
            'enrollment_id': enrollment_id,
            'pan_enrollment_id': pan_enrollment_id
        }

        with pytest.raises(CoreCardRemovedError):
            await VisaUpdateEnrollmentMetadataAction(**params).run()

        mock_update_enrollment_metadata_action.assert_not_called()
