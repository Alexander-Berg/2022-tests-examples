import asyncio
from dataclasses import dataclass
import random
import typing as tp

from billing.hot.tests.clients.accounts import client as accounts
from billing.hot.tests.clients.payout import client as payout
from billing.hot.tests.clients.processor import client as processor
from billing.hot.tests.lib.state import builder, state
from billing.hot.tests.lib.testgen import actions


@dataclass
class ActionGroup:
    actions: list[actions.Action]

    async def execute(self, states: list[state.PipelineState]) -> tuple:
        return await asyncio.gather(*[action.do(states) for action in self.actions], return_exceptions=True)


class TestRunner:
    def __init__(
        self,
        payout_client: payout.Client,
        accounts_client: accounts.Client,
        processor_client: processor.Client,
        create_state_builder: tp.Callable,
    ) -> None:
        self.payout_client = payout_client
        self.accounts_client = accounts_client
        self.processor_client = processor_client

        self.state_builder: builder.Builder = create_state_builder()
        self.states = []
        self.action_groups: list[ActionGroup] = []

    def add_action_groups(self, *args) -> None:
        groups = list(args)
        for group in groups:
            random.shuffle(group.actions)
            for action in group.actions:
                action.on_add(self)

        self.action_groups.extend(groups)

    async def execute(self):
        self.state_builder.write_clients_migrated()
        self.states.append(self.state_builder.built_state())

        for group in self.action_groups:
            yield await group.execute(self.states)
