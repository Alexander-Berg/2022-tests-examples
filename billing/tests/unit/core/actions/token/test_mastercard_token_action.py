import logging
from uuid import uuid4

import pytest
from aiohttp import ClientResponse

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_items, has_properties, has_string

from billing.yandex_pay.yandex_pay.core.actions.token.mastercard_token_actions import MastercardDeleteTokenAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.interactions import MasterCardClient
from billing.yandex_pay.yandex_pay.interactions.mastercard import TokenAlreadyDeletedError


@pytest.fixture
async def card(storage, randn) -> Card:
    return await storage.card.create(
        Card(
            trust_card_id='trust-card-id',
            owner_uid=randn(),
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='0000',
            card_id=uuid4(),
        )
    )


@pytest.fixture
async def enrollment(storage, card) -> Enrollment:
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=None,
            tsp_token_id=str(uuid4()),
            card_last4=card.last4,
        )
    )


class TestMastercardDeleteTokenAction:
    @pytest.fixture
    def fake_mastercard_response(self):
        return {'srcCorrelationId': 'fake_correlation_id'}

    @pytest.fixture(autouse=True)
    def mock_mastercard_client_token_delete(self, mocker, fake_mastercard_response):
        mock_json = mocker.AsyncMock(return_value=fake_mastercard_response)
        mock_response = mocker.Mock(spec_set=ClientResponse, json=mock_json)
        mock_response.headers.get.return_value = None
        mock_delete_card = mocker.AsyncMock(return_value=mock_response)
        return mocker.patch.object(MasterCardClient, 'delete_card', mock_delete_card)

    @pytest.mark.asyncio
    async def test_delete_token(
        self,
        storage,
        card,
        enrollment,
        mock_mastercard_client_token_delete,
        fake_mastercard_response,
        product_logs,
    ):
        await MastercardDeleteTokenAction(enrollment=enrollment).run()

        enrollment = await storage.enrollment.get(enrollment.enrollment_id)
        assert enrollment.tsp_token_status == TSPTokenStatus.DELETED

        loaded_card = await storage.card.get(card.card_id)
        assert_that(loaded_card, equal_to(card))

        mock_mastercard_client_token_delete.assert_awaited_once_with(
            src_digital_card_id=enrollment.tsp_token_id
        )

        logs = product_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='TSP token deleted',
                    _context=has_entries(
                        tsp=TSPType.MASTERCARD,
                        uid=card.owner_uid,
                        card={'card_id': card.card_id, 'pan_last4': card.last4},
                        enrollment={
                            'enrollment_id': enrollment.enrollment_id,
                            'merchant_id': enrollment.merchant_id,
                            'tsp_token_id': enrollment.tsp_token_id,
                            'tsp_card_id': enrollment.tsp_card_id,
                            'tsp_token_status': TSPTokenStatus.ACTIVE,
                            'expire': enrollment.expire,
                        },
                        response={
                            'src_correlation_id': fake_mastercard_response['srcCorrelationId'],
                            'src_cx_flow_id': None,
                        },
                    )
                )
            )
        )

    @pytest.mark.asyncio
    async def test_can_force_delete_enrollment_from_base(
        self,
        storage,
        card,
        enrollment,
        mock_mastercard_client_token_delete,
        fake_mastercard_response,
    ):
        await MastercardDeleteTokenAction(enrollment=enrollment, force_delete=True).run()

        with pytest.raises(Enrollment.DoesNotExist):
            await storage.enrollment.get(enrollment.enrollment_id)

    @pytest.mark.asyncio
    async def test_delete_token_when_enrollment_is_missing(
        self,
        storage,
        card,
        enrollment,
        mock_mastercard_client_token_delete,
        caplog,
    ):
        caplog.set_level(logging.INFO)
        await storage.enrollment.delete(enrollment)

        await MastercardDeleteTokenAction(enrollment=enrollment).run()

        loaded_card = await storage.card.get(card.card_id)
        assert_that(loaded_card, equal_to(card))

        mock_mastercard_client_token_delete.assert_awaited_once_with(
            src_digital_card_id=enrollment.tsp_token_id
        )

        log_messages = [rec.message for rec in caplog.records]
        assert_that(
            log_messages,
            has_items('Attempt to delete enrollment', 'Enrollment not found'),
        )

    @pytest.mark.asyncio
    async def test_delete_token_empty_tsp_token(self, enrollment):
        enrollment.tsp_token_id = None

        with pytest.raises(AssertionError) as excinfo:
            await MastercardDeleteTokenAction(enrollment=enrollment).run()

        assert_that(
            excinfo.value,
            has_string('TSP token id must not be empty')
        )

    @pytest.mark.asyncio
    async def test_delete_token_when_already_removed_by_tsp(
        self,
        enrollment,
        mock_mastercard_client_token_delete,
        caplog,
    ):
        caplog.set_level(logging.INFO)
        mock_mastercard_client_token_delete.side_effect = TokenAlreadyDeletedError(
            status_code=404, method='DELETE', service=MasterCardClient.SERVICE
        )

        await MastercardDeleteTokenAction(enrollment=enrollment).run()
        mock_mastercard_client_token_delete.assert_awaited_once_with(
            src_digital_card_id=enrollment.tsp_token_id
        )

        log_messages = [rec.message for rec in caplog.records]
        assert_that(
            log_messages,
            has_items('Attempt to delete enrollment', 'Token already deleted by TSP'),
        )

    @pytest.mark.asyncio
    async def test_multiple_deletes_succeed(
        self, storage, enrollment, mock_mastercard_client_token_delete
    ):
        await MastercardDeleteTokenAction(enrollment=enrollment).run()

        enrollment = await storage.enrollment.get(enrollment.enrollment_id)
        assert enrollment.tsp_token_status == TSPTokenStatus.DELETED

        await MastercardDeleteTokenAction(enrollment=enrollment).run()

        assert_that(mock_mastercard_client_token_delete.call_count, equal_to(2))
