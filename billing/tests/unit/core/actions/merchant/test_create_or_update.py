from uuid import uuid4

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, has_properties

from billing.yandex_pay.yandex_pay.core.actions.merchant.create_or_update import CreateOrUpdateMerchantAction


@pytest.mark.asyncio
async def test_create_merchant_result(storage):
    merchant = await CreateOrUpdateMerchantAction(
        merchant_id=uuid4(), name='some_name',
        origins=[{'origin': 'https://foo.test'}],
        callback_url='callback',
    ).run()

    assert_that(
        merchant,
        has_properties({
            'name': 'some_name',
            'callback_url': 'callback',
            'origins': contains(
                has_properties({
                    'origin': 'https://foo.test:443'
                })
            )
        })
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
    await CreateOrUpdateMerchantAction(
        merchant_id=merchant_id, name='some_name',
        origins=[{'origin': 'https://foo.test'}, {'origin': 'https://bar.test'}],
    ).run()

    await CreateOrUpdateMerchantAction(
        merchant_id=merchant_id, name='some_other_name',
        origins=[{'origin': 'https://foo.test'}, {'origin': 'https://baz.test'}],
        callback_url='callback',
    ).run()

    assert_that(
        await storage.merchant.get(merchant_id),
        has_properties({
            'merchant_id': merchant_id,
            'name': 'some_other_name',
            'callback_url': 'callback',
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
