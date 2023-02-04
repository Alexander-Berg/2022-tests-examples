from dataclasses import replace
from uuid import uuid4

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.authorize import AuthorizeOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import OriginNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin


@pytest.mark.asyncio
async def test_success(storage, partner, merchant):
    origin = await storage.origin.create(
        Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='https://a.test')
    )

    await AuthorizeOriginAction(partner_id=partner.partner_id, origin_id=origin.origin_id).run()


@pytest.mark.asyncio
async def test_not_found(storage, partner):
    with pytest.raises(OriginNotFoundError):
        await AuthorizeOriginAction(partner_id=partner.partner_id, origin_id=uuid4()).run()


@pytest.mark.asyncio
async def test_origin_has_different_partner_id(storage, partner, merchant):
    other_partner = await storage.partner.create(
        replace(
            partner,
            name='other',
            partner_id=None,
        )
    )
    other_merchant = await storage.merchant.create(
        replace(
            merchant,
            name='other',
            partner_id=other_partner.partner_id,
            merchant_id=None,
        )
    )
    origin = await storage.origin.create(
        Origin(
            merchant_id=other_merchant.merchant_id,
            origin_id=uuid4(),
            origin='https://a.test',
        )
    )

    with pytest.raises(OriginNotFoundError):
        await AuthorizeOriginAction(
            partner_id=partner.partner_id,
            origin_id=origin.origin_id,
        ).run()
