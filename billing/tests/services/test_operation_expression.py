from decimal import Decimal
from typing import Optional

import hamcrest as hm
import pytest

from billing.library.python.calculator.services.account import (
    OperationContextManager, OperationExpression, SessionService,
    Context, AnalyticConfig, AccountPlanService,
    AccountType,
)
from billing.library.python.calculator.models.account import AccountModel
from billing.library.python.calculator.test_utils.builder import gen_account

NAMESPACE = 'namespace'


class PaymentAnalyticConfig(AnalyticConfig):
    __slots__ = ('client_id', 'contract_id', 'currency', 'terminal_id')

    def __init__(self, client_id: int, contract_id: int, currency: str, terminal_id: Optional[int] = 0):
        self.client_id = client_id
        self.contract_id = contract_id
        self.currency = currency
        self.terminal_id = terminal_id


class ContractAnalyticConfig(AnalyticConfig):
    __slots__ = ('client_id', 'contract_id', 'currency')

    def __init__(self, client_id: int, contract_id: int, currency: str):
        self.client_id = client_id
        self.contract_id = contract_id
        self.currency = currency


class CommissionAnalyticConfig(AnalyticConfig):
    __slots__ = ('client_id', 'contract_id', 'currency', 'product', 'detailed_product', 'region')

    def __init__(
        self,
        client_id: int,
        contract_id: int,
        currency: str,
        product: str,
        detailed_product: str,
        region: Optional[str] = '',
    ):
        self.client_id = client_id
        self.contract_id = contract_id
        self.currency = currency
        self.product = product
        self.detailed_product = detailed_product
        self.region = region


test_plan_template = {
    'cashless': (PaymentAnalyticConfig, AccountType.PASSIVE),
    'cashless_refunds': (PaymentAnalyticConfig, AccountType.ACTIVE),
    'compensations': (ContractAnalyticConfig, AccountType.PASSIVE),
    'compensations_refunds': (ContractAnalyticConfig, AccountType.ACTIVE),
    'commissions': (CommissionAnalyticConfig, AccountType.ACTIVE),
    'commissions_with_vat': (CommissionAnalyticConfig, AccountType.ACTIVE),
}


def get_session_by_accounts(accounts):
    account_models = [AccountModel(**a) for a in accounts]
    account_plan = AccountPlanService.build_plan(NAMESPACE, test_plan_template, account_models)
    return SessionService(account_plan=account_plan)


def generate_operation_expression(name, amount, operation_type):
    return dict(amount=amount, account=name, operation_type=operation_type)


class AccountData:
    def __init__(self, account, datetime: int):
        self.name = account['loc']['type']
        self.credit = int(account['credit'])
        self.debit = int(account['debit'])
        self.datetime = datetime
        self.loc = account['loc']


class TestOperationExpressions:
    CLIENT_ID = 1
    GENERAL_CONTRACT_ID = 2
    COMMON_DT_TS = 1577876040
    AMOUNT = Decimal('100.00')

    cashless_analytic = PaymentAnalyticConfig(CLIENT_ID, GENERAL_CONTRACT_ID, 'RUB')
    compensation_analytic = ContractAnalyticConfig(CLIENT_ID, GENERAL_CONTRACT_ID, 'RUB')
    commission_analytic = CommissionAnalyticConfig(CLIENT_ID, GENERAL_CONTRACT_ID, 'RUB', '', '')

    params = [
        [
            gen_account(
                NAMESPACE, 'cashless',
                CLIENT_ID, GENERAL_CONTRACT_ID,
                credit=Decimal('2500.00'),
                debit=Decimal('1000.00'),
                ts=COMMON_DT_TS,
            ),
            gen_account(
                NAMESPACE, 'compensations',
                CLIENT_ID, GENERAL_CONTRACT_ID,
                credit=Decimal('2000.00'),
                debit=Decimal('1000.00'),
                ts=COMMON_DT_TS,
            ),
            gen_account(
                NAMESPACE, 'cashless_refunds',
                CLIENT_ID, GENERAL_CONTRACT_ID,
                credit=Decimal('1000.00'),
                debit=Decimal('2700.00'),
                ts=COMMON_DT_TS,
            ),
            gen_account(
                NAMESPACE, 'compensations_refunds',
                CLIENT_ID, GENERAL_CONTRACT_ID,
                credit=Decimal('1000.00'),
                debit=Decimal('1700.00'),
                ts=COMMON_DT_TS,
            ),
        ]
    ]

    separate_context_managers_expected_expr = [
        OperationExpression(name='test_1',
                            expressions=[
                                generate_operation_expression('cashless', AMOUNT, 'DEBIT'),
                                generate_operation_expression('cashless_refunds', AMOUNT, 'CREDIT')
                            ]),
        OperationExpression(name='test_2',
                            expressions=[
                                generate_operation_expression('commissions', 3 * AMOUNT, 'DEBIT'),
                                generate_operation_expression('commissions_with_vat', 3 * AMOUNT, 'CREDIT')
                            ])
    ]

    single_expression_expected_expr = [
        OperationExpression(name='test_1', expressions=[
            generate_operation_expression('cashless', AMOUNT, 'DEBIT'),
            generate_operation_expression('cashless_refunds', AMOUNT, 'CREDIT'),
            generate_operation_expression('commissions', 3 * AMOUNT, 'DEBIT'),
            generate_operation_expression('commissions_with_vat', 3 * AMOUNT, 'CREDIT')
        ])
    ]

    nested_expressions_expected_expr = [
        OperationExpression(name='test_3', expressions=[
            generate_operation_expression('cashless', AMOUNT, 'DEBIT'),
            generate_operation_expression('cashless_refunds', AMOUNT, 'CREDIT')
        ]),
        OperationExpression(name='test_2', expressions=[
            generate_operation_expression('cashless', AMOUNT, 'DEBIT'),
            generate_operation_expression('cashless_refunds', AMOUNT, 'CREDIT'),
            generate_operation_expression('commissions', 3 * AMOUNT, 'DEBIT'),
            generate_operation_expression('commissions_with_vat', 3 * AMOUNT, 'CREDIT')
        ]),
        OperationExpression(name='test_4', expressions=[
            generate_operation_expression('commissions', 3 * AMOUNT, 'DEBIT'),
            generate_operation_expression('commissions_with_vat', 3 * AMOUNT, 'CREDIT'),
        ]),
        OperationExpression(name='test_1', expressions=[
            generate_operation_expression('cashless', AMOUNT, 'DEBIT'),
            generate_operation_expression('cashless_refunds', AMOUNT, 'CREDIT'),
            generate_operation_expression('commissions', 3 * AMOUNT, 'DEBIT'),
            generate_operation_expression('commissions_with_vat', 3 * AMOUNT, 'CREDIT'),
            generate_operation_expression('compensations', 2 * AMOUNT, 'DEBIT'),
            generate_operation_expression('cashless', 2 * AMOUNT, 'CREDIT'),
        ])
    ]

    @pytest.mark.parametrize('accounts', params)
    def test_single_expression(self, accounts):
        session = get_session_by_accounts(accounts)

        with session.context.get_operation_context('test_1'):
            session.debit_account('cashless', self.AMOUNT, self.cashless_analytic)
            session.credit_account('cashless_refunds', self.AMOUNT, self.cashless_analytic)

            session.debit_account('commissions', 3 * self.AMOUNT, self.commission_analytic)
            session.credit_account('commissions_with_vat', 3 * self.AMOUNT, self.commission_analytic)

        operation_expressions = session.get_operation_expressions()
        hm.assert_that(
            operation_expressions,
            hm.equal_to(TestOperationExpressions.single_expression_expected_expr),
            'check with expected expressions',
        )

    @pytest.mark.parametrize('accounts', params)
    def test_two_separate_expressions(self, accounts):
        session = get_session_by_accounts(accounts)
        with session.context.get_operation_context('test_1'):
            session.debit_account('cashless', self.AMOUNT, self.cashless_analytic)
            session.credit_account('cashless_refunds', self.AMOUNT, self.cashless_analytic)

        with session.context.get_operation_context('test_2'):
            session.debit_account('commissions', 3 * self.AMOUNT, self.commission_analytic)
            session.credit_account('commissions_with_vat', 3 * self.AMOUNT, self.commission_analytic)

        operation_expressions = session.get_operation_expressions()
        hm.assert_that(
            operation_expressions,
            hm.equal_to(TestOperationExpressions.separate_context_managers_expected_expr),
            'check with expected expressions',
        )

    @pytest.mark.parametrize('accounts', params)
    def test_nested_context_managers(self, accounts):
        session = get_session_by_accounts(accounts)

        with session.context.get_operation_context('test_1'):
            session.debit_account('cashless', self.AMOUNT, self.cashless_analytic)
            session.credit_account('cashless_refunds', self.AMOUNT, self.cashless_analytic)

            with session.context.get_operation_context('test_2'):
                session.debit_account('cashless', self.AMOUNT, self.cashless_analytic)
                session.credit_account('cashless_refunds', self.AMOUNT, self.cashless_analytic)

                with session.context.get_operation_context('test_3'):
                    session.debit_account('cashless', self.AMOUNT, self.cashless_analytic)
                    session.credit_account('cashless_refunds', self.AMOUNT, self.cashless_analytic)

                session.debit_account('commissions', 3 * self.AMOUNT, self.commission_analytic)
                session.credit_account('commissions_with_vat', 3 * self.AMOUNT, self.commission_analytic)

            session.debit_account('commissions', 3 * self.AMOUNT, self.commission_analytic)
            session.credit_account('commissions_with_vat', 3 * self.AMOUNT, self.commission_analytic)

            with session.context.get_operation_context('test_4'):
                session.debit_account('commissions', 3 * self.AMOUNT, self.commission_analytic)
                session.credit_account('commissions_with_vat', 3 * self.AMOUNT, self.commission_analytic)

            session.debit_account('compensations', 2 * self.AMOUNT, self.compensation_analytic)
            session.credit_account('cashless', 2 * self.AMOUNT, self.cashless_analytic)

        operation_expressions = session.get_operation_expressions()
        hm.assert_that(
            operation_expressions,
            hm.equal_to(TestOperationExpressions.nested_expressions_expected_expr),
            'check with expected expressions',
        )

    def test_context_manager_without_name(self):
        context = Context('test_1')
        with pytest.raises(TypeError) as excinfo:
            with context.get_operation_context():
                pass
        assert 'get_operation_context() missing 1 required positional argument: \'name\'' in str(excinfo.value)

        context_manager = OperationContextManager(context)
        with pytest.raises(ValueError) as excinfo:
            with context_manager:
                pass
        assert 'OperationContextManager entered without name' in str(excinfo.value)

    @pytest.mark.parametrize('accounts', params)
    def test_payments_without_context(self, accounts):
        session = get_session_by_accounts(accounts)
        with session.context.get_operation_context('test_1'):
            pass

        session.debit_account('compensations', 2 * self.AMOUNT, self.compensation_analytic)
        session.credit_account('cashless', 2 * self.AMOUNT, self.cashless_analytic)
