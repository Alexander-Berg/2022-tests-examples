from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_enrollment import UpdateEnrollmentAction
from billing.yandex_pay.yandex_pay.core.actions.events.visa import VisaUpdateTokenStatusAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType, VisaTokenStatus
from billing.yandex_pay.yandex_pay.interactions import VisaClient
from billing.yandex_pay.yandex_pay.interactions.visa import create_visa_token_status_result


@pytest.fixture
def provisioned_token_id():
    return str(uuid4())


@pytest.fixture
def pan_enrollment_id():
    return str(uuid4())


@pytest.fixture
def token_status_update_notification(provisioned_token_id):
    return {
        'date': utcnow(),
        'vProvisionedTokenID': provisioned_token_id
    }


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
def patch_action_context(mocker, storage):
    mocker.patch.object(
        VisaUpdateTokenStatusAction,
        'context',
        storage=storage
    )
    mocker.patch.object(
        UpdateEnrollmentAction,
        'context',
        storage=storage
    )


def visa_token_status_response_body(tsp_token_status):
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


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'tsp_token_status',
    [each for each in VisaTokenStatus if each != VisaTokenStatus.DELETED],
)
async def test_should_set_token_status(
    provisioned_token_id: str,
    tsp_token_status: VisaTokenStatus,
    card: Card,
    card_enrollment: Enrollment,
    storage,
    yandex_pay_settings,
    mocker,
):
    mocker.patch.object(
        VisaClient,
        'get_token_status',
        return_value=create_visa_token_status_result(
            visa_token_status_response_body(tsp_token_status)
        )
    )

    await VisaUpdateTokenStatusAction(provisioned_token_id).run()

    card_enrollment = await storage.enrollment.get_by_tsp_token_id(
        tsp=TSPType.VISA,
        tsp_token_id=provisioned_token_id
    )

    assert_that(
        card_enrollment.tsp_token_status,
        equal_to(TSPTokenStatus.from_visa_status(tsp_token_status))
    )


@pytest.mark.asyncio
async def test_deleted_status_must_mark_enrollment_as_deleted(
    provisioned_token_id: str,
    card: Card,
    card_enrollment: Enrollment,
    storage,
    yandex_pay_settings,
    mocker,
):
    found = await storage.enrollment.get_by_tsp_token_id(
        tsp=TSPType.VISA,
        tsp_token_id=provisioned_token_id
    )
    assert found is not None

    status_deleted = VisaTokenStatus.DELETED

    mocker.patch.object(
        VisaClient,
        'get_token_status',
        return_value=create_visa_token_status_result(
            visa_token_status_response_body(status_deleted)
        )
    )

    await VisaUpdateTokenStatusAction(provisioned_token_id).run()
    from_base = await storage.enrollment.get_by_tsp_token_id(
        tsp=TSPType.VISA,
        tsp_token_id=provisioned_token_id
    )

    assert from_base.tsp_token_status == TSPTokenStatus.DELETED
