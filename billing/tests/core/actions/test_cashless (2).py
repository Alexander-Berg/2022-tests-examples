from decimal import Decimal

import arrow
import hamcrest as hm
import pytest
from billing.library.python.calculator.models.transaction import TransactionModel

from billing.hot.calculators.oplata.calculator.core.actions.cashless import (
    ProcessPaymentAction, ProcessRefundAction,
)
from billing.hot.calculators.oplata.calculator.core.models.event import PaymentEvent, RefundEvent
from billing.hot.calculators.oplata.calculator.core.models.method import CashlessReferences


class TestProcessPaymentAction:
    @pytest.mark.parametrize(
        'payment_event_with_price_and_commission, expected_amount_wo_vat', [
            pytest.param((Decimal('120_000.00'), 10), Decimal('101.694915')),
            pytest.param((Decimal('10.00'), 10), Decimal('0.008475')),
            pytest.param((Decimal('1_000.00'), 1), Decimal('0.084746')),
            pytest.param((Decimal('1_000.00'), 50), Decimal('4.237288')),
        ],
        indirect=['payment_event_with_price_and_commission']
    )
    def test_amount_wo_vat(
        self,
        payment_event_with_price_and_commission: PaymentEvent,
        cashless_references_with_migration_info: CashlessReferences,
        expected_amount_wo_vat: Decimal,
    ):
        action = ProcessPaymentAction(
            payment_event_with_price_and_commission,
            cashless_references_with_migration_info,
        )

        amount_wo_vat = action.amount_wo_vat
        assert amount_wo_vat == expected_amount_wo_vat

    def test_tax_policy(
        self,
        payment_event: PaymentEvent,
        cashless_references_with_migration_info: CashlessReferences,
    ):
        action = ProcessPaymentAction(
            payment_event,
            cashless_references_with_migration_info,
        )

        assert action.tax_policy is not None and action.tax_policy.name == 'resident'

    @pytest.mark.parametrize(
        'payment_event_with_price_and_commission, expected_agent_rewards_amount', [
            pytest.param((Decimal('120_000.00'), 10), Decimal('120.00')),
            pytest.param((Decimal('10.00'), 10), Decimal('0.01')),
            pytest.param((Decimal('1_000.00'), 1), Decimal('0.10')),
            pytest.param((Decimal('1_000.00'), 50), Decimal('5.0')),
        ],
        indirect=['payment_event_with_price_and_commission']
    )
    def test_transactions_correctness_without_markup(
        self,
        payment_event_with_price_and_commission: PaymentEvent,
        cashless_references_with_migration_info: CashlessReferences,
        expected_agent_rewards_amount: Decimal,
    ):
        action = ProcessPaymentAction(
            payment_event_with_price_and_commission,
            cashless_references_with_migration_info,
        )

        actual_transactions = self._actual_transactions(action)

        expected_transactions = self._expected_transactions(
            action,
            expected_agent_rewards_amount
        )
        hm.assert_that(actual_transactions, hm.contains_inanyorder(*expected_transactions))

    @pytest.mark.parametrize(
        'payment_event_with_price_commission_and_markup, expected_agent_rewards_amount', [
            pytest.param(
                (
                    Decimal('120_000.00'),
                    10,
                    {'by_card': Decimal('110_000.00'), 'by_promocode': Decimal('10_000.00')},
                ),
                Decimal('120.00'),
            ),
            pytest.param(
                (
                    Decimal('10.00'),
                    10,
                    {'by_card': Decimal('0.00'), 'by_promocode': Decimal('10.00')},
                ),
                Decimal('0.01'),
            ),
            pytest.param(
                (
                    Decimal('1_000.00'),
                    1,
                    {'by_card': Decimal('899.00'), 'by_promocode': Decimal('101.00')},
                ),
                Decimal('0.10'),
            ),
            pytest.param(
                (
                    Decimal('1_000.00'),
                    50,
                    {'by_card': Decimal('1_000.00'), 'by_promocode': Decimal('0.00')}
                ),
                Decimal('5.0'),
            ),
        ],
        indirect=['payment_event_with_price_commission_and_markup']
    )
    def test_transactions_correctness_with_markup(
        self,
        payment_event_with_price_commission_and_markup: PaymentEvent,
        cashless_references_with_migration_info: CashlessReferences,
        expected_agent_rewards_amount: Decimal,
    ):
        action = ProcessPaymentAction(
            payment_event_with_price_commission_and_markup,
            cashless_references_with_migration_info,
        )

        actual_transactions = self._actual_transactions(action)

        expected_transactions = self._expected_transactions(
            action,
            expected_agent_rewards_amount
        )
        hm.assert_that(actual_transactions, hm.contains_inanyorder(*expected_transactions))

    @staticmethod
    def _actual_transactions(action: ProcessPaymentAction) -> list[TransactionModel]:
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(1))
        return client_transactions[0].transactions

    @staticmethod
    def _expected_transactions(
        action: ProcessPaymentAction,
        agent_rewards_amount: Decimal,
    ) -> list[TransactionModel]:
        by_card, by_promocode = action.event.paid_by_card_and_promocode
        dt = arrow.get(action.event.order.closed).int_timestamp

        agent_rewards_transaction = TransactionModel(
            loc={
                'namespace': 'oplata',
                'type': 'agent_rewards',
                'client_id': action.contract.client_id,
                'contract_id': action.contract.id,
                'currency': action.currency,
                'product': action.product_mdh_id,
            },
            amount=agent_rewards_amount,
            type='debit',
            dt=arrow.get(action.event.order.closed).int_timestamp
        )

        transactions = [agent_rewards_transaction]

        if by_card > 0:
            cashless_transaction = TransactionModel(
                loc={
                    'namespace': 'oplata',
                    'type': 'cashless',
                    'client_id': action.contract.client_id,
                    'contract_id': action.contract.id,
                    'currency': action.currency,
                    'terminal_id': action.event.terminal_id,
                },
                amount=by_card,
                type='credit',
                dt=dt,
            )
            transactions.append(cashless_transaction)

        if by_promocode > 0:
            promocodes_transaction = TransactionModel(
                loc={
                    'namespace': 'oplata',
                    'type': 'promocodes',
                    'client_id': action.contract.client_id,
                    'contract_id': action.contract.id,
                    'currency': action.currency,
                },
                amount=by_promocode,
                type='credit',
                dt=dt,
            )
            transactions.append(promocodes_transaction)

        return transactions


class TestRefundAction:
    def test_transactions_correctness_without_markup(
        self,
        refund_event: RefundEvent,
        cashless_references_with_migration_info: CashlessReferences,
    ):
        action = ProcessRefundAction(
            refund_event,
            cashless_references_with_migration_info,
        )

        actual_transactions = self._actual_transactions(action)

        expected_transactions = self._expected_transactions(action)
        hm.assert_that(actual_transactions, hm.contains_inanyorder(*expected_transactions))

    @pytest.mark.parametrize(
        'refund_event_with_price_and_markup', [
            (Decimal('1_100.00'), {'by_card': Decimal('1_000.00'), 'by_promocode': Decimal('100.00')}),
            (Decimal('100.00'), {'by_card': Decimal('0.00'), 'by_promocode': Decimal('100.00')}),
            (Decimal('150.00'), {'by_card': Decimal('150.00'), 'by_promocode': Decimal('0.00')}),
        ],
        indirect=['refund_event_with_price_and_markup']
    )
    def test_transactions_correctness_with_markup(
        self,
        refund_event_with_price_and_markup: RefundEvent,
        cashless_references_with_migration_info: CashlessReferences,
    ):
        action = ProcessRefundAction(
            refund_event_with_price_and_markup,
            cashless_references_with_migration_info,
        )

        actual_transactions = self._actual_transactions(action)

        expected_transactions = self._expected_transactions(action)
        hm.assert_that(actual_transactions, hm.contains_inanyorder(*expected_transactions))

    @staticmethod
    def _actual_transactions(action: ProcessRefundAction) -> list[TransactionModel]:
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(1))
        return client_transactions[0].transactions

    @staticmethod
    def _expected_transactions(action: ProcessRefundAction) -> list[TransactionModel]:
        by_card, by_promocode = action.event.refunded_by_card_and_promocode
        dt = arrow.get(action.event.refund.closed).int_timestamp

        transactions = []

        if by_card > 0:
            cashless_refunds_transaction = TransactionModel(
                loc={
                    'namespace': 'oplata',
                    'type': 'cashless_refunds',
                    'client_id': action.contract.client_id,
                    'contract_id': action.contract.id,
                    'currency': action.currency,
                    'terminal_id': action.event.terminal_id,
                },
                amount=by_card,
                type='debit',
                dt=dt,
            )
            transactions.append(cashless_refunds_transaction)

        if by_promocode > 0:
            promocodes_refunds_transaction = TransactionModel(
                loc={
                    'namespace': 'oplata',
                    'type': 'promocodes_refunds',
                    'client_id': action.contract.client_id,
                    'contract_id': action.contract.id,
                    'currency': action.currency,
                },
                amount=by_promocode,
                type='debit',
                dt=dt,
            )
            transactions.append(promocodes_refunds_transaction)

        return transactions
