from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.create_or_update import CreateOrUpdatePSPAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.get import GetPSPAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CorePSPNotFoundError


@pytest.fixture
def public_key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22JR/S9PUWKblmyl1CSGiHV0'
        'bAtlbeC3QgjQ=='
    )


@pytest.mark.asyncio
async def test_get_psp(public_key, rands):
    psp = await CreateOrUpdatePSPAction(
        psp_id=uuid4(),
        psp_external_id=rands(),
        psp_auth_keys=[{'key': public_key, 'alg': 'ES256'}],
    ).run()

    assert_that(
        await GetPSPAction(psp.psp_id).run(),
        equal_to(psp),
    )


@pytest.mark.asyncio
async def test_get_psp__when_not_found():
    with pytest.raises(CorePSPNotFoundError):
        await GetPSPAction(uuid4()).run()
