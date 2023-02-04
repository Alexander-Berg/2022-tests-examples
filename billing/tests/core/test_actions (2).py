import typing as tp
from datetime import datetime
from decimal import Decimal

import arrow
import hamcrest as hm
import pytest
from billing.contract_iface import JSONContract
from billing.library.python.calculator import exceptions as exc
from billing.library.python.calculator import util
from billing.library.python.calculator.models import transaction as tx
from billing.library.python.calculator.models.firm import FirmModel
from billing.library.python.calculator.services.tax import TaxService
from billing.library.python.calculator.test_utils.builder import gen_general_contract, gen_firm

from billing.hot.calculators.bnpl_income.calculator.core import actions
from billing.hot.calculators.bnpl_income.calculator.core import models


@pytest.fixture
def firm() -> FirmModel:
    return FirmModel(**gen_firm(identity=1, mdh_id='mdh'))


@pytest.fixture
def migration_info(firm: FirmModel) -> tp.List[tp.Dict]:
    return [
        {
            'namespace': 'bnpl_income',
            'from_dt': util.to_msk_dt(datetime(2020, 3, 8)),
            'dry_run': True,
            'filter': 'Firm',
            'object_id': firm.id,
        },
    ]


@pytest.fixture
def contracts(firm: FirmModel, event: models.CommissionEvent) -> tp.List[tp.Dict]:
    return [
        gen_general_contract(
            contract_id=event.billing_contract_id,
            client_id=event.billing_client_id,
            person_id=7,
            services=[actions.BNPL_INCOME_SERVICE_ID],
            firm=firm.id,
            person_type=firm.person_categories[0].category,
        ),
    ]


@pytest.fixture
def products() -> tp.Dict:
    return {
        'id': 'mdh_id',
        'default': 'default_mdh_id',
    }


@pytest.fixture
def references(
    firm: FirmModel,
    migration_info: tp.List[tp.Dict],
    contracts: tp.List[tp.Dict],
    products: tp.Dict,
) -> models.CommissionReferences:
    return models.CommissionReferences(
        firm=firm,
        migration_info=migration_info,
        contracts=contracts,
        products=products,
    )


@pytest.fixture(params=[
    models.TransactionType.PAYMENT,
    models.TransactionType.REFUND,
])
def event(request) -> models.CommissionEvent:
    return models.CommissionEvent(
        transaction_id='o28391jk209213',
        transaction_amount=Decimal('120.22'),
        transaction_dt=util.to_msk_dt(datetime.now()),
        transaction_type=request.param,
        billing_client_id=12,
        billing_contract_id=123,
        currency='RUB',
        product_id='foo-bar',
    )


@pytest.fixture
def method(
    event: models.CommissionEvent,
    references: models.CommissionReferences,
) -> models.CommissionMethod:
    return models.CommissionMethod(
        event=event,
        references=references,
    )


class TestProcessCommissionActionRun:
    testcases = ['not_migrated', 'processed_payment', 'processed_refund']

    @pytest.fixture
    def far_future(self) -> datetime:
        return util.to_msk_dt(datetime(4_202, 3, 8))

    @pytest.fixture(params=testcases)
    def bundle(
        self,
        request,
        event: models.CommissionEvent,
        references: models.CommissionReferences,
        firm: FirmModel,
        contracts: tp.List[tp.Dict],
        products: tp.Dict,
        far_future: datetime,
    ) -> tp.Dict[str, tp.Any]:
        return {
            'not_migrated': {
                'method': models.CommissionMethod(
                    event=event,
                    references=models.CommissionReferences(
                        firm=firm,
                        contracts=contracts,
                        products=products,
                        migration_info=[
                            {
                                'namespace': 'bnpl_income',
                                'from_dt': far_future,
                                'dry_run': True,
                                'filter': 'Firm',
                                'object_id': firm.id,
                            },
                        ]
                    )
                ),
                'expected_client_transactions': [],
            },
            'processed_payment': {
                'method': models.CommissionMethod(
                    event=models.CommissionEvent(
                        transaction_id='wdjirqe43203fcjsks',
                        transaction_amount=Decimal('1299.99'),
                        transaction_dt=util.to_msk_dt(datetime(2022, 3, 21)),
                        transaction_type=models.TransactionType.PAYMENT,
                        billing_client_id=event.billing_client_id,
                        billing_contract_id=event.billing_contract_id,
                        currency='RUB',
                        product_id='foo',
                    ),
                    references=references,
                ),
                'expected_client_transactions': [
                    tx.ClientTransactionBatchModel(
                        client_id=12,
                        transactions=[
                            tx.TransactionModel(
                                loc={
                                    'namespace': 'bnpl_income',
                                    'type': 'commissions',
                                    'client_id': event.billing_client_id,
                                    'contract_id': event.billing_contract_id,
                                    'currency': 'RUB',
                                    'product': 'foo',
                                },
                                amount=Decimal('1299.99'),
                                type='debit',
                                dt=arrow.get(datetime(2022, 3, 21)).int_timestamp
                            ),
                        ],
                    ),
                ],
            },
            'processed_refund': {
                'method': models.CommissionMethod(
                    event=models.CommissionEvent(
                        transaction_id='kalsdjk341309fjsdkj93',
                        transaction_amount=Decimal('999.99'),
                        transaction_dt=util.to_msk_dt(datetime(2022, 3, 22)),
                        transaction_type=models.TransactionType.REFUND,
                        billing_client_id=event.billing_client_id,
                        billing_contract_id=event.billing_contract_id,
                        currency='RUB',
                        product_id='bar',
                    ),
                    references=references,
                ),
                'expected_client_transactions': [
                    tx.ClientTransactionBatchModel(
                        client_id=12,
                        transactions=[
                            tx.TransactionModel(
                                loc={
                                    'namespace': 'bnpl_income',
                                    'type': 'commission_refunds',
                                    'client_id': event.billing_client_id,
                                    'contract_id': event.billing_contract_id,
                                    'currency': 'RUB',
                                    'product': 'bar',
                                },
                                amount=Decimal('999.99'),
                                type='credit',
                                dt=arrow.get(datetime(2022, 3, 22)).int_timestamp
                            ),
                        ],
                    ),
                ],
            },
        }[request.param]

    @pytest.fixture
    def method(self, bundle) -> models.CommissionMethod:
        return bundle.get('method')

    @pytest.fixture
    def expected_client_transactions(self, bundle) -> tp.List[tx.ClientTransactionBatchModel]:
        return bundle.get('expected_client_transactions')

    def test_run(
        self,
        method: models.CommissionMethod,
        expected_client_transactions: tp.List[tx.ClientTransactionBatchModel],
    ):
        action = actions.from_transaction_type(method)

        transaction_batch = action.run()
        client_transactions = transaction_batch.client_transactions

        hm.assert_that(client_transactions, hm.equal_to(expected_client_transactions))


class TestProcessCommissionActionContract:
    testcases = ['ok', 'not_found', 'client_ids_mismatch']

    @pytest.fixture(params=testcases)
    def bundle(
        self,
        request,
        event: models.CommissionEvent,
        firm: FirmModel,
        migration_info: tp.List[tp.Dict],
        contracts: tp.List[tp.Dict],
        products: tp.Dict,
    ) -> tp.Dict[str, tp.Any]:
        return {
            'ok': {
                'method': models.CommissionMethod(
                    event=event,
                    references=models.CommissionReferences(
                        firm=firm,
                        migration_info=migration_info,
                        contracts=contracts,
                        products=products,
                    ),
                ),
            },
            'not_found': {
                'method': models.CommissionMethod(
                    event=event,
                    references=models.CommissionReferences(
                        firm=firm,
                        migration_info=migration_info,
                        products=products,
                        contracts=[]
                    ),
                ),
                'expected_exc': exc.ContractNotFoundError,
            },
            'client_ids_mismatch': {
                'method': models.CommissionMethod(
                    event=event,
                    references=models.CommissionReferences(
                        firm=firm,
                        migration_info=migration_info,
                        products=products,
                        contracts=[
                            gen_general_contract(
                                contract_id=event.billing_contract_id,
                                client_id=event.billing_client_id + 1,
                                person_id=7,
                                services=[actions.BNPL_INCOME_SERVICE_ID],
                                firm=firm.id,
                                person_type=firm.person_categories[0].category,
                            ),
                        ],
                    ),
                ),
                'expected_exc': exc.NoActiveContractsError,
            }
        }.get(request.param)

    @pytest.fixture
    def method(self, bundle: tp.Dict[str, tp.Any]) -> models.CommissionMethod:
        return bundle.get('method')

    @pytest.fixture
    def expected_exc(self, bundle: tp.Dict[str, tp.Any]) -> tp.Optional[tp.Type[Exception]]:
        return bundle.get('expected_exc')

    def test_contract(
        self,
        method: models.CommissionMethod,
        expected_exc: tp.Optional[tp.Type[Exception]],
    ):
        action = actions.from_transaction_type(method)

        if expected_exc:
            with pytest.raises(expected_exc):
                _ = action.contract
        else:
            expected = JSONContract(contract_data=method.references.contracts[0])
            hm.assert_that(action.contract.id, hm.equal_to(expected.id))


class TestProcessCommissionActionProductMdhId:
    testcases = ['ok', 'no_product', 'missing_product']

    @pytest.fixture(params=testcases)
    def bundle(self, request) -> tp.Dict[str, tp.Any]:
        return {
            'ok': {
                'product_id': 'id',
                'expected_mdh_id': 'mdh_id',
            },
            'no_product': {
                'expected_mdh_id': 'default_mdh_id',
            },
            'missing_product': {
                'product_id': 'missing_id',
                'expected_mdh_id': 'default_mdh_id',
            }
        }[request.param]

    @pytest.fixture
    def method(
        self,
        bundle: tp.Dict[str, tp.Any],
        event: models.CommissionEvent,
        references: models.CommissionReferences,
    ) -> models.CommissionMethod:
        return models.CommissionMethod(
            event=models.CommissionEvent(
                transaction_id='lqkllqwkqwlw120312',
                transaction_amount=Decimal('1000.00'),
                transaction_dt=util.to_msk_dt(datetime(2022, 3, 22)),
                transaction_type=models.TransactionType.REFUND,
                billing_client_id=event.billing_client_id,
                billing_contract_id=event.billing_contract_id,
                currency='RUB',
                product_id=bundle.get('product_id'),
            ),
            references=references,
        )

    @pytest.fixture
    def expected_mdh_id(self, bundle: tp.Dict[str, tp.Any]) -> tp.Optional[str]:
        return bundle.get('expected_mdh_id')

    def test_product_mdh_id(
        self,
        method: models.CommissionMethod,
        expected_mdh_id: tp.Optional[str],
    ):
        action = actions.from_transaction_type(method)
        mdh_id = action.product_mdh_id

        hm.assert_that(mdh_id, hm.equal_to(expected_mdh_id))


class TestProcessCommissionActionTaxPolicy:
    def test_tax_policy(self, method: models.CommissionMethod):
        action = actions.from_transaction_type(method)

        signed = JSONContract(
            contract_data=method.references.contracts[0],
        ).current_signed(util.to_msk_dt_naive(action.on_dt))

        expected_tax_policy = TaxService.tax_policy_by_contract_state(
            method.references.firm,
            signed,
            action.on_dt,
        )

        hm.assert_that(action.tax_policy, hm.equal_to(expected_tax_policy))


class TestProcessCommissionActionExternalId:
    def test_external_id(self, method: models.CommissionMethod):
        action = actions.from_transaction_type(method)

        hm.assert_that(action.external_id, hm.equal_to(method.event.transaction_id))


class TestProcessCommissionActionAmountWoVat:
    def test_amount_wo_vat(self, method: models.CommissionMethod):
        action = actions.from_transaction_type(method)

        expected_amount_wo_vat = TaxService.tax_by_date(
            action.tax_policy,
            action.on_dt,
        ).sum_wo_tax(method.event.transaction_amount)

        hm.assert_that(action.amount_wo_vat, hm.equal_to(expected_amount_wo_vat))
