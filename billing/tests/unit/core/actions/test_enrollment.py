from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay.yandex_pay.core.actions.enrollment.delete import MarkEnrollmentAsDeletedAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType, VisaTokenStatusUpdateReason


@pytest.fixture
async def card(storage):
    return await storage.card.create(
        Card(
            trust_card_id='trust-card-id',
            owner_uid=5555,
            tsp=TSPType.VISA,
            expire=utcnow(),
            last4='0000',
            card_id=uuid4(),
        )
    )


@pytest.fixture
async def merchant(storage, merchant_entity):
    created = await storage.merchant.create(merchant_entity)
    yield created
    await storage.merchant.delete(created)


@pytest.fixture
def tsp_card_id():
    return uuid4()


@pytest.fixture
def tsp_token_id():
    return uuid4()


@pytest.fixture
def enrollment_entity(card, merchant, tsp_card_id, tsp_token_id):
    return Enrollment(
        card_id=card.card_id,
        merchant_id=None,
        tsp_card_id=tsp_card_id,
        tsp_token_id=tsp_token_id,
        tsp_token_status=TSPTokenStatus.ACTIVE,
        card_last4=card.last4,
    )


class TestDeleteEnrollment:
    @pytest.fixture
    def time_now(self):
        return utcnow()

    @pytest.mark.asyncio
    async def test_should_mark_as_deleted(self, card, enrollment_entity, storage):
        enrollment = await storage.enrollment.create(enrollment_entity)
        await MarkEnrollmentAsDeletedAction(
            tsp=card.tsp,
            tsp_token_id=enrollment.tsp_token_id,
        ).run()

        from_base = await storage.enrollment.get(enrollment.enrollment_id)
        assert from_base.tsp_token_status == TSPTokenStatus.DELETED

    @pytest.mark.asyncio
    async def test_can_force_delete_enrollment_from_base(self, card, enrollment_entity, storage):
        enrollment = await storage.enrollment.create(enrollment_entity)
        await MarkEnrollmentAsDeletedAction(
            tsp=card.tsp,
            tsp_token_id=enrollment.tsp_token_id,
            force_delete=True,
        ).run()

        with pytest.raises(Enrollment.DoesNotExist):
            await storage.enrollment.get(enrollment.enrollment_id)

    @pytest.mark.asyncio
    async def test_should_write_expected_logs(self, card, enrollment_entity, storage, product_logs):
        extra_params = {'some': {'extra': 'params'}}

        enrollment = await storage.enrollment.create(enrollment_entity)
        await MarkEnrollmentAsDeletedAction(
            tsp=card.tsp,
            tsp_token_id=enrollment.tsp_token_id,
            reason=VisaTokenStatusUpdateReason.CUSTOMER_CONFIRMED,
            extra_params=extra_params,
        ).run()

        [log] = product_logs()
        assert_that(
            log,
            has_properties(
                message='Enrollment was deleted',
                _context=has_entries(
                    tsp=card.tsp,
                    uid=card.owner_uid,
                    card={'card_id': card.card_id, 'pan_last4': card.last4},
                    enrollment={
                        'enrollment_id': enrollment.enrollment_id,
                        'merchant_id': enrollment.merchant_id,
                        'tsp_token_id': enrollment.tsp_token_id,
                        'tsp_card_id': enrollment.tsp_card_id,
                        'previous_tsp_token_status': TSPTokenStatus.ACTIVE,
                        'tsp_token_status': TSPTokenStatus.DELETED,
                        'expire': enrollment.expire,
                    },
                    reason=VisaTokenStatusUpdateReason.CUSTOMER_CONFIRMED,
                    extra_params=extra_params,
                )
            )
        )

    @pytest.mark.asyncio
    async def test_delete_action_should_not_raise_on_missing_enrollment(self, card):
        await MarkEnrollmentAsDeletedAction(
            tsp=card.tsp,
            tsp_token_id=str(uuid4())
        ).run()

    def test_serialize_kwargs(self, time_now):
        extra_params = {'foo': ['bar']}
        action = MarkEnrollmentAsDeletedAction(
            tsp=TSPType.MASTERCARD,
            tsp_token_id='tsp_token_id',
            event_timestamp=time_now,
            extra_params=extra_params,
        )

        expected_serialized_kwargs = {
            'tsp': 'mastercard',
            'tsp_token_id': 'tsp_token_id',
            'event_timestamp': time_now.isoformat(sep=' '),
            'extra_params': extra_params,
        }
        assert_that(
            action.serialize_kwargs(action._init_kwargs),
            equal_to(expected_serialized_kwargs)
        )

    def test_deserialize_kwargs(self, time_now):
        extra_params = {'baz': ['quux']}
        raw_params = {
            'tsp': 'mastercard',
            'tsp_token_id': 'tsp_token_id',
            'event_timestamp': time_now.isoformat(sep=' '),
            'extra_params': extra_params,
        }

        expected_deserialized_kwargs = dict(
            tsp=TSPType.MASTERCARD,
            tsp_token_id='tsp_token_id',
            event_timestamp=time_now,
            extra_params=extra_params,
        )
        assert_that(
            MarkEnrollmentAsDeletedAction.deserialize_kwargs(raw_params),
            equal_to(expected_deserialized_kwargs),
        )
