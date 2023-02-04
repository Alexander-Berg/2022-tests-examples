import uuid
from collections import deque
from dataclasses import fields, is_dataclass, replace

import pytest

from sendr_utils import alist, json_value

from hamcrest import assert_that, greater_than, has_entries, has_length, has_properties, is_

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.register_in_balance import (
    RegisterPartnerInBalanceAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.update_data import UpdatePartnerDataAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PartnerDataFieldAlreadyExistsError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import RegistrationData


def test_should_run_in_transaction():
    assert_that(UpdatePartnerDataAction.transact, is_(True))


@pytest.mark.asyncio
async def test_update_partner_data(storage, user, partner, rands):
    reg_data = replace(partner.registration_data)

    q = deque([(reg_data, False)])
    expected_revision = 1
    while len(q) > 0:
        entity, modifiable = q.popleft()

        for field in fields(entity):
            value = getattr(entity, field.name)
            if is_dataclass(value):
                q.append((value, field.metadata.get('modifiable', modifiable)))
                continue

            expected_revision += 1
            if not modifiable:
                # set the field (nested) inside reg_data to None
                setattr(entity, field.name, None)
                partner = await storage.partner.save(
                    replace(partner, registration_data=reg_data),
                    ignore_fields=('revision',),  # don't bump the revision as this is a manual patch
                )
                # set the field to original value
                setattr(entity, field.name, value)
            else:
                setattr(entity, field.name, rands())
            await UpdatePartnerDataAction(user=user, partner=partner, registration_data=reg_data).run()
            partner = await storage.partner.get(partner.partner_id)
            assert_that(partner, has_properties(registration_data=reg_data, revision=expected_revision))

    assert_that(expected_revision, greater_than(1))


@pytest.mark.asyncio
async def test_cannot_update_not_null_fields(user, partner, rands):
    for field in fields(partner.registration_data):
        if is_dataclass(getattr(partner.registration_data, field.name)):
            continue
        value = rands()
        with pytest.raises(PartnerDataFieldAlreadyExistsError) as exc_info:
            await UpdatePartnerDataAction(
                user=user,
                partner=partner,
                registration_data=replace(partner.registration_data, **{field.name: value}),
            ).run()

        assert_that(
            exc_info.value,
            has_properties(
                params={
                    'path': field.name,
                    'existing_field': json_value(getattr(partner.registration_data, field.name)),
                    'new_field': value,
                }
            ),
        )


@pytest.fixture
async def store_integration(storage, merchant, status, for_testing):
    return await storage.integration.create(
        Integration(
            merchant_id=merchant.merchant_id,
            psp_id=uuid.uuid4(),
            psp_external_id='psp',
            status=status,
            for_testing=for_testing,
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', (True, False))
@pytest.mark.parametrize('completed', (True, False))
@pytest.mark.parametrize('status', [s for s in IntegrationStatus])
async def test_schedules_balance_registration(
    storage, user, partner, for_testing, completed, status, store_integration
):
    reg_data = replace(partner.registration_data) if completed else RegistrationData(ogrn='ogrn')
    partner.registration_data = RegistrationData()

    await UpdatePartnerDataAction(user=user, partner=partner, registration_data=reg_data).run()

    filters = {'action_name': RegisterPartnerInBalanceAction.action_name}
    tasks = await alist(storage.task.find(filters=filters))
    is_not_empty = not for_testing and completed and status == IntegrationStatus.DEPLOYED
    assert_that(tasks, has_length(is_not_empty))
    if is_not_empty:
        assert_that(
            tasks[0].params, has_entries(action_kwargs=has_entries(uid=user.uid, partner_id=str(partner.partner_id)))
        )
