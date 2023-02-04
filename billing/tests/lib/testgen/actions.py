import abc
from dataclasses import dataclass

import hamcrest as hm

from billing.hot.tests.lib.matchers import base as matchers
from billing.hot.tests.lib.polling import poll
from billing.hot.tests.lib.state import state, contract as contr


class Action(abc.ABC):
    @abc.abstractmethod
    async def do(self, states: list[state.PipelineState]):
        pass

    def on_add(self, runner: 'runner.TestRunner'):
        self.runner = runner
        self._on_add()

    def _on_add(self):
        pass


@dataclass
class ProcessAction(Action):
    event_type: str
    amount: int = None

    def _on_add(self):
        if self.event_type.startswith('subvention'):
            self.runner.state_builder.try_fill_contract(contr.SubventionContract)
        elif (
            self.event_type.startswith('cashless')
            or self.event_type.startswith('commission')
            or self.event_type.startswith('revenue')
            or self.event_type.startswith('promocodes')
            or self.event_type.startswith('fuel_hold')
        ):
            self.runner.state_builder.try_fill_contract(contr.ServiceContract)
        elif self.event_type.startswith('logistics'):
            self.runner.state_builder.try_fill_contract(contr.LogisticsContract)
        elif self.event_type.startswith('corporate'):
            self.runner.state_builder.try_fill_contract(contr.CorporateContract)
        elif self.event_type == 'payout':
            pass
        else:
            raise NotImplementedError(f"add needed contract for event_type {self.event_type} here")

    async def do(self, states: list[state.PipelineState]):
        extended_params = {}
        if self.amount is not None:
            if self.event_type in ('cashless_refunds', 'commissions_refunds'):
                self.amount *= -1
            extended_params = {"amount": self.amount}

        async def poll_body():
            async with self.runner.processor_client.do(self.event_type, states[0],
                                                       extended_params=extended_params) as resp:
                if resp.status >= 500:
                    raise poll.RetryError
                elif resp.status >= 400:
                    raise ValueError(f"process request {self.event_type} status {resp.status}")

        await poll.poll(poll_body, interval_seconds=0.5, timeout_seconds=10)


@dataclass
class PayoutByClientAction(Action):
    async def do(self, states: list[state.PipelineState]):
        async with self.runner.payout_client.payout_by_client(states[0]) as resp:
            if resp.status != 200:
                raise ValueError(f"expected status 200 for payout-by-client, got {resp.status}")


@dataclass
class Account:
    name: str
    debit: int
    credit: int


@dataclass
class CheckAccountAction(Action):
    accounts: list[Account]

    async def do(self, states: list[state.PipelineState]):
        async with self.runner.accounts_client.read_balances(
            states[0],
            253370764800,  # equal to constants.MaxDt of accounts
            [account.name for account in self.accounts],
            'taxi',
        ) as resp:
            if resp.status != 200:
                raise ValueError(f"expected status 200 for accounts check, got {resp.status}")
            hm.assert_that(await resp.json(), matchers.success_accounts_read_batch_response({
                'balances': [
                    {
                        'loc': {
                            'type': account.name,
                        },
                        'credit': "{:.6f}".format(account.credit) if account.credit else "0",
                        'debit': "{:.6f}".format(account.debit) if account.debit else "0",
                    }
                    for account in self.accounts
                ]
            }))
