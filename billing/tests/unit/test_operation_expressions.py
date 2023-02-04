import pytest

from hamcrest import assert_that, equal_to

from billing.hot.calculators.taxi.calculator.core.accounter import (
    Context, OperationContextManager, OperationExpression, Session
)
from billing.hot.calculators.taxi.calculator.core.taxi_accounts import build_taxi_plan
from billing.hot.calculators.taxi.calculator.core.taxi_analytics import (
    CommissionAnalytic, ContractAnalytic, PaymentAnalytic
)
from billing.hot.calculators.taxi.calculator.tests.builder import gen_account
from billing.hot.calculators.taxi.calculator.tests.const import CLIENT_ID, COMMON_DT_TS, GENERAL_CONTRACT_ID

params = [
    [
        gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='2500', debit='1000', ts=COMMON_DT_TS),
        gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='2000', debit='1000', ts=COMMON_DT_TS),
        gen_account('cashless_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000', debit='2700',
                    ts=COMMON_DT_TS),
        gen_account('compensations_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000', debit='1700',
                    ts=COMMON_DT_TS),
    ]
]


AMOUNT = 100


def get_session_by_accounts(accounts):
    accounts_data = []
    for account in accounts:
        accounts_data.append(AccountData(account))
    account_plan = build_taxi_plan(accounts_data=accounts_data)
    return Session(account_plan=account_plan)


def generate_operation_expression(name, amount, operation_type):
    return dict(amount=amount, account=name, operation_type=operation_type)


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


class AccountData:
    def __init__(self, account):
        self.name = account['loc']['type']
        self.credit = int(account['credit'])
        self.debit = int(account['debit'])
        self.datetime = COMMON_DT_TS
        self.loc = account['loc']


class TestOperationExpressions:
    cashless_analytic = PaymentAnalytic(CLIENT_ID, GENERAL_CONTRACT_ID, 'RUB')
    compensation_analytic = ContractAnalytic(CLIENT_ID, GENERAL_CONTRACT_ID, 'RUB')
    commission_analytic = CommissionAnalytic(CLIENT_ID, GENERAL_CONTRACT_ID, 'RUB', '', '')

    @pytest.mark.parametrize('accounts', params)
    def test_single_expression(self, accounts):
        session = get_session_by_accounts(accounts)

        with session.context.get_operation_context('test_1'):
            session.debit_account('cashless', AMOUNT, self.cashless_analytic)
            session.credit_account('cashless_refunds', AMOUNT, self.cashless_analytic)

            session.debit_account('commissions', 3 * AMOUNT, self.commission_analytic)
            session.credit_account('commissions_with_vat', 3 * AMOUNT, self.commission_analytic)

        operation_expressions = session.get_operation_expressions()
        assert_that(operation_expressions, equal_to(single_expression_expected_expr),
                    'check with expected expressions')

    @pytest.mark.parametrize('accounts', params)
    def test_two_separate_expressions(self, accounts):
        session = get_session_by_accounts(accounts)
        with session.context.get_operation_context('test_1'):
            session.debit_account('cashless', AMOUNT, self.cashless_analytic)
            session.credit_account('cashless_refunds', AMOUNT, self.cashless_analytic)

        with session.context.get_operation_context('test_2'):
            session.debit_account('commissions', 3 * AMOUNT, self.commission_analytic)
            session.credit_account('commissions_with_vat', 3 * AMOUNT, self.commission_analytic)

        operation_expressions = session.get_operation_expressions()
        assert_that(operation_expressions, equal_to(separate_context_managers_expected_expr),
                    'check with expected expressions')

    @pytest.mark.parametrize('accounts', params)
    def test_nested_context_managers(self, accounts):
        session = get_session_by_accounts(accounts)

        with session.context.get_operation_context('test_1'):
            session.debit_account('cashless', AMOUNT, self.cashless_analytic)
            session.credit_account('cashless_refunds', AMOUNT, self.cashless_analytic)

            with session.context.get_operation_context('test_2'):
                session.debit_account('cashless', AMOUNT, self.cashless_analytic)
                session.credit_account('cashless_refunds', AMOUNT, self.cashless_analytic)

                with session.context.get_operation_context('test_3'):
                    session.debit_account('cashless', AMOUNT, self.cashless_analytic)
                    session.credit_account('cashless_refunds', AMOUNT, self.cashless_analytic)

                session.debit_account('commissions', 3 * AMOUNT, self.commission_analytic)
                session.credit_account('commissions_with_vat', 3 * AMOUNT, self.commission_analytic)

            session.debit_account('commissions', 3 * AMOUNT, self.commission_analytic)
            session.credit_account('commissions_with_vat', 3 * AMOUNT, self.commission_analytic)

            with session.context.get_operation_context('test_4'):
                session.debit_account('commissions', 3 * AMOUNT, self.commission_analytic)
                session.credit_account('commissions_with_vat', 3 * AMOUNT, self.commission_analytic)

            session.debit_account('compensations', 2 * AMOUNT, self.compensation_analytic)
            session.credit_account('cashless', 2 * AMOUNT, self.cashless_analytic)

        operation_expressions = session.get_operation_expressions()
        assert_that(operation_expressions, equal_to(nested_expressions_expected_expr),
                    'check with expected expressions')

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

        session.debit_account('compensations', 2 * AMOUNT, self.compensation_analytic)
        session.credit_account('cashless', 2 * AMOUNT, self.cashless_analytic)
