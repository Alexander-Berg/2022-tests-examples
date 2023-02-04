from collections import namedtuple

import sqlalchemy as sa
from sqlalchemy.sql import select

from billing.hot.tests.clients.database.accounts.base import (
    state_account, event_batch, event, account
)


class AccountsDB:
    def __init__(self, client):
        self.client = client

    async def get_event_batches(self, external_ids: list):
        return await self.client.execute(
            event_batch.select().where(event_batch.c.external_id.in_(map(str, external_ids)))
        )

    async def get_events_by_batch_id(self, event_batch_ids: list):
        rows = await self.client.execute(
            select(
                columns=[event, account.c.type.label('account_type'),
                         account.c.attribute_1.label('account_attribute_1')],
                whereclause=event.c.event_batch_id.in_(event_batch_ids),
                from_obj=event.join(account, account.c.id == event.c.account_id)
            )
        )
        if not rows:
            return []

        cls = namedtuple('Event', rows[0].keys())
        return [cls(*r.values()) for r in rows]

    async def get_accounts(self, account_ids: list):
        return await self.client.execute(
            account.select().where(account.c.id.in_(account_ids))
        )

    async def get_states(self, state_type: str, attributes: list):
        conditions = [state_account.c.type == state_type]
        conditions.extend([getattr(state_account.c, 'attribute_' + str(i + 1)) == str(v)
                           for i, v in enumerate(attributes)])
        return await self.client.execute(
            state_account.select().where(sa.and_(*conditions))
        )

    async def execute(self, *args, **kwargs):
        return await self.client.execute(*args, **kwargs)
