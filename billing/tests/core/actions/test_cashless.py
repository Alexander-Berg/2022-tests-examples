from decimal import Decimal

import arrow
import pytest

from billing.library.python.calculator.models.transaction import TransactionModel

from billing.hot.calculators.bnpl.calculator.core.actions.cashless import (
    ProcessPaymentAction, ProcessRefundAction,
)
from billing.hot.calculators.bnpl.calculator.core.models.event import PaymentEvent, RefundEvent
from billing.hot.calculators.bnpl.calculator.core.models.method import CashlessReferences


amount_wo_vat_params = [
    pytest.param((Decimal('120_000.00'), Decimal('10.00'), Decimal('2.00')), Decimal('10.169492')),
    pytest.param((Decimal('10.00'), Decimal('10.00'), Decimal('3.00')), Decimal('11.016949')),
    pytest.param((Decimal('1_000.00'), Decimal('1.00'), Decimal('-8.00')), Decimal('7.627119')),
    pytest.param((Decimal('1_000.00'), Decimal('50.00'), Decimal('5.00')), Decimal('46.610169')),
]

parametrize_payment_amount_wo_vat = pytest.mark.parametrize(
    'payment_event_with_price_and_commission, expected_amount_wo_vat', amount_wo_vat_params,
    indirect=['payment_event_with_price_and_commission']
)

parametrize_refund_amount_wo_vat = pytest.mark.parametrize(
    'refund_event_with_price_and_commission, expected_amount_wo_vat', amount_wo_vat_params,
    indirect=['refund_event_with_price_and_commission']
)


class TestProcessPaymentAction:
    @parametrize_payment_amount_wo_vat
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
        'payment_event_with_price_and_commission, expected_agent_commission_amount', [
            pytest.param((Decimal('120_000.00'), Decimal('10.00'), Decimal('2.00')), Decimal('12.0')),
            pytest.param((Decimal('10.00'), Decimal('10.00'), Decimal('3.00')), Decimal('13.0')),
            pytest.param((Decimal('1_000.00'), Decimal('1.00'), Decimal('-8.00')), Decimal('9.0')),
            pytest.param((Decimal('1_000.00'), Decimal('50.00'), Decimal('5.00')), Decimal('55.0')),
        ],
        indirect=['payment_event_with_price_and_commission']
    )
    def test_transactions_correctness(
            self,
            payment_event_with_price_and_commission: PaymentEvent,
            cashless_references_with_migration_info: CashlessReferences,
            expected_agent_commission_amount: Decimal,
    ):
        action = ProcessPaymentAction(
            payment_event_with_price_and_commission,
            cashless_references_with_migration_info,
        )

        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        assert len(client_transactions) == 1

        expected_transactions = self._expected_transactions(
            action,
            expected_agent_commission_amount
        )
        assert client_transactions[0].transactions == expected_transactions

    @staticmethod
    def _expected_transactions(
            action: ProcessPaymentAction,
            commission_amount: Decimal,
    ) -> list[TransactionModel]:
        cashless_transaction = TransactionModel(
            loc={
                'namespace': 'bnpl',
                'type': 'cashless',
                'client_id': action.contract.client_id,
                'contract_id': action.contract.id,
                'currency': action.currency
            },
            amount=action.event.transaction_amount,
            type='credit',
            dt=arrow.get(action.event.transaction_dt).int_timestamp
        )
        commission_transaction = TransactionModel(
            loc={
                'namespace': 'bnpl',
                'type': 'commissions',
                'client_id': action.contract.client_id,
                'contract_id': action.contract.id,
                'currency': action.currency,
                'product': action.event.product_id,
            },
            amount=commission_amount,
            type='debit',
            dt=arrow.get(action.event.transaction_dt).int_timestamp
        )

        return [cashless_transaction, commission_transaction]


class TestRefundAction:
    @parametrize_refund_amount_wo_vat
    def test_amount_wo_vat(
            self,
            refund_event_with_price_and_commission: RefundEvent,
            cashless_references_with_migration_info: CashlessReferences,
            expected_amount_wo_vat: Decimal,
    ):
        action = ProcessRefundAction(
            refund_event_with_price_and_commission,
            cashless_references_with_migration_info,
        )

        amount_wo_vat = action.amount_wo_vat
        assert amount_wo_vat == expected_amount_wo_vat

    def test_transactions_correctness(
            self,
            refund_event: RefundEvent,
            cashless_references_with_migration_info: CashlessReferences,
    ):
        action = ProcessRefundAction(
            refund_event,
            cashless_references_with_migration_info,
        )

        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        assert len(client_transactions) == 1

        expected_transactions = self._expected_transactions(action)
        assert client_transactions[0].transactions == expected_transactions

    @staticmethod
    def _expected_transactions(
            action: ProcessRefundAction
    ) -> list[TransactionModel]:
        cashless_transaction = TransactionModel(
            loc={
                'namespace': 'bnpl',
                'type': 'cashless_refunds',
                'client_id': action.contract.client_id,
                'contract_id': action.contract.id,
                'currency': action.currency
            },
            amount=action.event.transaction_amount,
            type='debit',
            dt=arrow.get(action.event.transaction_dt).int_timestamp
        )
        commission_transaction = TransactionModel(
            loc={
                'namespace': 'bnpl',
                'type': 'commission_refunds',
                'client_id': action.contract.client_id,
                'contract_id': action.contract.id,
                'currency': action.currency,
                'product': action.event.product_id,
            },
            amount=action.event.aquiring_commission + action.event.service_commission,
            type='credit',
            dt=arrow.get(action.event.transaction_dt).int_timestamp
        )

        return [cashless_transaction, commission_transaction]
