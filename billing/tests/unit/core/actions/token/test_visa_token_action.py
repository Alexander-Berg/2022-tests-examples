import logging
from typing import Optional
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, has_item

from billing.yandex_pay.yandex_pay.core.actions.token.visa_token_actions import (
    VisaDeleteTokenAction, VisaResumeTokenAction, VisaSuspendTokenAction
)
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import (
    TSPTokenStatus, TSPType, VisaTokenStatus, VisaTokenStatusUpdateReason
)
from billing.yandex_pay.yandex_pay.interactions import VisaClient


@pytest.fixture
def mock_delete_enrollment_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.core.actions.'
        'token.visa_token_actions.MarkEnrollmentAsDeletedAction'
    )
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


@pytest.fixture
def mock_update_enrollment_status_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.core.actions.'
        'token.visa_token_actions.UpdateEnrollmentAction'
    )
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


@pytest.fixture
def mock_visa_client(mocker):
    return mocker.patch.object(
        VisaClient,
        'change_token_status',
        mocker.AsyncMock(),
    )


@pytest.fixture
async def card(storage, randn):
    card = await storage.card.create(
        Card(
            trust_card_id='trust-card-id',
            owner_uid=randn(),
            tsp=TSPType.VISA,
            expire=utcnow(),
            last4='0000',
            card_id=uuid4(),
        )
    )
    yield card

    await storage.card.delete(card)


@pytest.fixture
async def enrollment(storage, card) -> Enrollment:
    enrollment = await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=str(uuid4()),
            tsp_token_id=str(uuid4()),
            card_last4=card.last4,
        )
    )
    yield enrollment

    await storage.enrollment.delete(enrollment)


@pytest.mark.parametrize('reason', list(VisaTokenStatusUpdateReason))
@pytest.mark.parametrize('description', [None, 'some description'])
@pytest.mark.parametrize(
    'action_cls,status',
    [
        (VisaResumeTokenAction, VisaTokenStatus.ACTIVE),
        (VisaSuspendTokenAction, VisaTokenStatus.SUSPENDED),
        (VisaDeleteTokenAction, VisaTokenStatus.DELETED),
    ]
)
@pytest.mark.asyncio
async def test_should_call_visa_api_and_change_status(
    enrollment: Enrollment,
    status: VisaTokenStatus,
    reason: VisaTokenStatusUpdateReason,
    description: Optional[str],
    action_cls,
    mock_visa_client,
    mock_update_enrollment_status_action,
    mock_delete_enrollment_action,
    caplog,
):
    caplog.set_level(logging.INFO)
    kwargs = {
        'enrollment': enrollment,
        'reason': reason,
        'description': description
    }
    await action_cls(**kwargs).run()

    mock_visa_client.assert_called_once_with(
        provisioned_token_id=enrollment.tsp_token_id,
        status=status,
        update_reason=reason,
        description=description
    )

    log_messages = [rec.message for rec in caplog.records]
    assert_that(log_messages, has_item('Attempt to change token status'))

    if status == VisaTokenStatus.DELETED:
        mock_delete_enrollment_action.assert_called_once_with(
            tsp=TSPType.VISA,
            tsp_token_id=enrollment.tsp_token_id,
            force_delete=False,
            reason=reason,
        )
        mock_delete_enrollment_action.return_value.run.assert_awaited_once_with()
    else:
        mock_update_enrollment_status_action.assert_called_once_with(
            tsp=TSPType.VISA,
            tsp_token_id=enrollment.tsp_token_id,
            tsp_token_status=TSPTokenStatus.from_visa_status(status),
            reason=reason,
        )
        mock_update_enrollment_status_action.return_value.run.assert_awaited_once_with()


@pytest.mark.asyncio
async def test_should_pass_force_delete_param(
    enrollment: Enrollment,
    mock_visa_client,
    mock_delete_enrollment_action,
):
    await VisaDeleteTokenAction(enrollment=enrollment, force_delete=True).run()

    mock_delete_enrollment_action.assert_called_once_with(
        tsp=TSPType.VISA,
        tsp_token_id=enrollment.tsp_token_id,
        force_delete=True,
        reason=VisaTokenStatusUpdateReason.CUSTOMER_CONFIRMED,
    )
    mock_delete_enrollment_action.return_value.run.assert_awaited_once_with()
