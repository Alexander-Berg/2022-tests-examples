from uuid import uuid4

import pytest

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, contains, contains_inanyorder, has_properties, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.create_or_update import CreateOrUpdateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    MerchantWithRelated,
    YandexDeliveryParams,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_origin import MerchantOrigin


@pytest.mark.asyncio
async def test_create_merchant_result(storage):
    partner_id = uuid4()
    merchant_id = uuid4()
    delivery_integration_params = DeliveryIntegrationParams(
        yandex_delivery=YandexDeliveryParams(oauth_token=YandexDeliveryParams.encrypt_oauth_token('oauth')),
    )
    merchant = await CreateOrUpdateMerchantAction(
        merchant_id=merchant_id,
        name='some_name',
        origins=[{'origin': 'https://foo.test'}],
        callback_url='callback',
        partner_id=partner_id,
        split_merchant_id='smi',
        delivery_integration_params=delivery_integration_params,
    ).run()

    assert_that(
        merchant,
        equal_to(
            MerchantWithRelated(
                merchant_id=merchant_id,
                name='some_name',
                origins=[
                    MerchantOrigin(
                        merchant_id=merchant_id,
                        origin='https://foo.test:443',
                        created=match_equality(not_none()),
                        updated=match_equality(not_none()),
                    )
                ],
                partner_id=partner_id,
                callback_url='callback',
                split_merchant_id='smi',
                delivery_integration_params=delivery_integration_params,
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            )
        ),
    )


@pytest.mark.asyncio
async def test_create_merchant__entities_are_created(storage):
    merchant_id = uuid4()
    await CreateOrUpdateMerchantAction(
        merchant_id=merchant_id, name='some_name',
        origins=[{'origin': 'https://foo.test'}],
    ).run()

    assert_that(
        await storage.merchant.get(merchant_id),
        has_properties({
            'merchant_id': merchant_id,
            'name': 'some_name',
        })
    )
    assert_that(
        await storage.merchant_origin.find_by_merchant_id(merchant_id),
        contains(
            has_properties({
                'origin': 'https://foo.test:443'
            })
        )
    )


@pytest.mark.asyncio
async def test_update_merchant__entities_are_updated(storage):
    merchant_id = uuid4()
    partner_id = uuid4()
    await CreateOrUpdateMerchantAction(
        merchant_id=merchant_id, name='some_name',
        origins=[{'origin': 'https://foo.test'}, {'origin': 'https://bar.test'}],
    ).run()

    await CreateOrUpdateMerchantAction(
        merchant_id=merchant_id, name='some_other_name',
        origins=[{'origin': 'https://foo.test'}, {'origin': 'https://baz.test'}],
        callback_url='callback',
        split_merchant_id='smi',
        partner_id=partner_id,
    ).run()

    assert_that(
        await storage.merchant.get(merchant_id),
        has_properties({
            'merchant_id': merchant_id,
            'name': 'some_other_name',
            'callback_url': 'callback',
            'split_merchant_id': 'smi',
            'partner_id': partner_id,
        })
    )
    assert_that(
        await storage.merchant_origin.find_by_merchant_id(merchant_id),
        contains_inanyorder(
            has_properties({
                'origin': 'https://foo.test:443'
            }),
            has_properties({
                'origin': 'https://baz.test:443'
            })
        )
    )
