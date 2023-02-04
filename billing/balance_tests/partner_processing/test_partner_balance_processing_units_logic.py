# -*- coding: utf-8 -*-

from contextlib import contextmanager
import datetime
from decimal import Decimal as D
from mock import MagicMock
import pytest

from yt.wrapper.mappings import FrozenDict

import balance.exc as exc
import balance.constants as const
from balance import mapper
from balance.partner_balance import (
    MODULE_NAME,
    BaseProcessingUnit,
    NoErrorReturn,
    UnitDispatcher,
    UnitTypes,
    PartnerBalanceUnitMediator
)
from butils.decimal_unit import DecimalUnit as DU
import tests.object_builder as ob


dt_ = datetime.datetime


ACT_PAYMENT_TERM_DT = dt_(2050, 2, 2)
ACT_DT = dt_(2020, 5, 1)


@contextmanager
def no_exception_ctx(*args, **kwargs):
    yield


def pytest_raises_ctx(*args, **kwargs):
    def wrapper():
        return pytest.raises(*args, **kwargs)
    return wrapper


class ExceptionUnit(BaseProcessingUnit):
    unit_name = 'exception_unit'

    def execute(self, store):
        raise Exception('SHEL I UPAL')


class SuccessUnit(BaseProcessingUnit):
    unit_name = 'success_unit'

    def execute(self, ctx):
        pass


class NoErrorReturnUnit(BaseProcessingUnit):
    unit_name = 'no_error_return_unit'

    def execute(self, ctx):
        raise NoErrorReturn


def create_invoice(client):
    invoice = ob.InvoiceBuilder(
        paysys=ob.Getter(mapper.Paysys, 1003),
        request=ob.RequestBuilder(
            firm_id=1,
            basket=ob.BasketBuilder(
                dt=dt_.now(),
                client=client,
                rows=[ob.BasketItemBuilder(
                    order=ob.OrderBuilder(
                        client=client,
                        service_id=const.ServiceId.DIRECT,
                        product=ob.Getter(mapper.Product, const.DIRECT_PRODUCT_RUB_ID)
                    ),
                    quantity=100000,
                )]
            )
        ),
    ).build(client.session).obj
    invoice.turn_on_rows()
    invoice.close_invoice(invoice.dt)
    for act in invoice.acts:
        act.payment_term_dt = ACT_PAYMENT_TERM_DT
        act.dt = ACT_DT
    return invoice


@pytest.fixture(scope='module')
def fixture_data(modular_session):
    session = modular_session
    client = ob.ClientBuilder().build(session).obj
    contract_signed = ob.ContractBuilder(
        client=client,
        is_signed=dt_.now(),
        firm_id=const.FirmId.YANDEX_OOO,
        currency=810,
    ).build(session).obj

    contract_unsigned = ob.ContractBuilder(
        client=client,
        is_signed=None,
        firm_id=const.FirmId.YANDEX_OOO,
        currency=810,
    ).build(session).obj

    contract = ob.ContractBuilder(
        client=client,
        is_signed=dt_.now(),
        firm_id=const.FirmId.YANDEX_OOO,
        currency=810,
        netting=1,
    ).build(session).obj
    contract.offer_accepted = 1
    contract.daily_state = dt_(2020, 2, 2)
    current_signed = contract.current_signed()

    pa_service_code = create_invoice(client)
    pa_service_code.contract = contract
    pa_service_code.person = contract.person
    pa_service_code.type = 'personal_account'
    pa_service_code.postpay = 1
    session.flush()
    # экстпропc должен проставиться на classname PersonalAccount
    session.expunge(pa_service_code)
    pa_service_code = session.query(mapper.PersonalAccount).getone(pa_service_code.id)
    pa_service_code.service_code = 'YANDEX_SERVICE'

    pa_no_service_code = create_invoice(client)
    pa_no_service_code.contract = contract
    pa_no_service_code.person = contract.person
    pa_no_service_code.type = 'personal_account'
    pa_no_service_code.postpay = 1
    session.flush()
    session.expunge(pa_no_service_code)
    pa_no_service_code = session.query(mapper.PersonalAccount).getone(pa_no_service_code.id)

    return {
        'contract_signed': contract_signed,
        'contract_unsigned': contract_unsigned,
        'contract': contract,
        'current_signed': current_signed,
        'pa_service_code': pa_service_code,
        'pa_no_service_code': pa_no_service_code,
    }


class TestManagementUnitBase(object):
    unit_type = UnitTypes.management_unit
    unit_name = None

    def prepare_env(self, service_id, contract_id, dt, raw_processing_units, exception_context, expected_data, session):
        return None

    def check_results(self, service_id, contract_id, dt, raw_processing_units, exception_context, expected_data,
                      prepared_test_data, management_unit, result, session):
        pass

    def execute(self, service_id, contract_id, dt, raw_processing_units, exception_context, expected_data, session):
        if exception_context is None:
            exception_context = no_exception_ctx
        prepared_test_data = self.prepare_env(service_id, contract_id, dt, raw_processing_units, exception_context,
                                              expected_data, session)
        session.flush()
        management_unit = None
        with exception_context() as ec:
            management_unit_cls = UnitDispatcher.get_unit(MODULE_NAME, self.unit_type, self.unit_name)
            management_unit = management_unit_cls(session, service_id, contract_id,
                                                  dt=dt, raw_processing_units=raw_processing_units)
            result = management_unit.process()
        if exception_context is not no_exception_ctx:
            return
        session.flush()
        self.check_results(service_id, contract_id, dt, raw_processing_units, exception_context, expected_data,
                           prepared_test_data, management_unit, result, session)


class TestDefaultProcessingManagementUnitErrors(TestManagementUnitBase):
    unit_name = 'management_unit'

    @pytest.mark.parametrize(
        'service_id, contract_id, dt, raw_processing_units, exception_context, expected_data', [
            (None, None, None, [123],
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_UNIT_EXCEPTION,
                               match='Unit\(module_name: partner_balances, unit_type: management_unit, unit_name: '
                                     'management_unit, execution_name: None\): wrong unit format in processing '
                                     'unit config'),
             None
             ),
            (None, None, None, [{}],
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_UNIT_EXCEPTION,
                               match='Unit\(module_name: partner_balances, unit_type: management_unit, '
                                     'unit_name: management_unit, execution_name: None\): '
                                     'no unit name in processing unit config'),
             None),
            (None, None, None, ['not_existing_unit_name'],
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_DISPATCHER_EXCEPTION,
                               match='Partner processing dispatcher error: module_name: PARTNER_BALANCES '
                                     'unit_type: PROCESSING_UNIT unit_name: NOT_EXISTING_UNIT_NAME not registered'),
             None),
            (None, None, None, ['success_unit', 'no_error_return_unit', 'exception_unit'],
             None,
             None
             ),
            (None, None, None,
             ['exception_unit', 'success_unit'], pytest_raises_ctx(Exception, match='SHEL I UPAL'),
             None
             ),
            (None, None, None,
             [{'unit': 'exception_unit', 'execution_params': {'suppress_exception': True, 'execution_name': 'ololo'}},
              'success_unit'], None,
             {}),
        ],
        ids=[
            'wrong_unit_format',
            'no_unit_name',
            'unit_not_exists',
            'no_error_return',
            'exception_raised',
            'exception_suppressed'
        ]
    )
    def test(self, service_id, contract_id, dt, raw_processing_units, exception_context, expected_data, session):
        self.execute(service_id, contract_id, dt, raw_processing_units, exception_context, expected_data, session)

    def check_results(self, service_id, contract_id, dt, raw_processing_units, exception_context, expected_data,
                      prepared_test_data, management_unit, result, session):
        assert result == expected_data


class TestProcessingUnitBase(object):
    unit_type = UnitTypes.processing_unit
    unit_name = None

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        ctx = PartnerBalanceUnitMediator(session, None, None, None)
        if store:
            ctx.data.update(store)
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        pass

    def execute(self, params, execution_params, store, exception_context, expected_data, session,
                fixture_data):
        if exception_context is None:
            exception_context = no_exception_ctx
        prepared_test_data = self.prepare_env(params, execution_params, store, exception_context,
                                              expected_data, session, fixture_data)
        session.flush()
        ctx = prepared_test_data['ctx']
        unit = None
        with exception_context() as ec:
            unit_cls = UnitDispatcher.get_unit(MODULE_NAME, self.unit_type, self.unit_name)
            unit = unit_cls(params=params, execution_params=execution_params)
            unit.process(ctx)
        if exception_context is not no_exception_ctx:
            return
        session.flush()
        self.check_results(params, execution_params, store, exception_context, expected_data,
                           unit, prepared_test_data, session, fixture_data)


class TestEnrichContractPorcessingUnits(TestProcessingUnitBase):
    unit_name = 'enrich_contract'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, None),
        ],
        ids=[
            'contract_selected_and_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        contract = fixture_data['contract']
        ctx = PartnerBalanceUnitMediator(session, contract.id, None, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx,
                'contract': contract}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        contract = prepared_test_data['contract']
        assert prepared_test_data['ctx'].data['contract'] is contract


class TestEnrichCurrentSignedProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_current_signed'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, {'contract_signed': 'contract_signed'}),
            ({'active': True}, None, {}, None, {'contract_signed': 'contract_signed'}),
            ({'active': True}, None, {}, pytest_raises_ctx(exc.CONTRACT_IS_NOT_SIGNED),
             {'contract_unsigned': None}),
            ({'active': False}, None, {}, None, {'contract_signed': 'contract_signed'}),
        ],
        ids=[
            'current_signed_no_params',
            'current_signed__active_true__contract_signed',
            'current_signed__active_true__contract_unsigned',
            'current_signed__active_false__contract_signed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        contract_param = expected_data.keys()[0]
        contract = fixture_data[contract_param]
        store['contract'] = contract
        ctx = PartnerBalanceUnitMediator(session, contract.id, None, dt_.now())
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        result_contract_param = expected_data[expected_data.keys()[0]]
        result_contract = result_contract_param and fixture_data[result_contract_param]
        assert prepared_test_data['ctx'].data['current_signed']._contract is result_contract


class TestPlaceClientIDProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_client_id'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, None),
        ],
        ids=[
            'client_id_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        contract = fixture_data['contract']
        store['contract'] = contract
        ctx = PartnerBalanceUnitMediator(session, contract.id, None, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx,
                'contract': contract}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        assert prepared_test_data['ctx'].output['ClientID'] == fixture_data['contract'].client_id


class TestPlaceCurrencyProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_currency'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, None),
        ],
        ids=[
            'currency_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['current_signed'] = fixture_data['current_signed']
        ctx = PartnerBalanceUnitMediator(session, None, None, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        assert prepared_test_data['ctx'].output['Currency'] == 'RUB'


class TestPlaceOfferAcceptedProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_offer_accepted'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, None),
        ],
        ids=[
            'offer_accepted_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params,  execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['contract'] = fixture_data['contract']
        store['current_signed'] = fixture_data['current_signed']
        ctx = PartnerBalanceUnitMediator(session, None, None, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        assert prepared_test_data['ctx'].output['OfferAccepted'] == 1


class TestPlaceNettingLastDateProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_netting_last_date'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, None),
        ],
        ids=[
            'netting_last_date_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['contract'] = fixture_data['contract']
        store['current_signed'] = fixture_data['current_signed']
        ctx = PartnerBalanceUnitMediator(session, None, None, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        assert prepared_test_data['ctx'].output['NettingLastDt'] == dt_(2020, 2, 2).isoformat()


class TestEnrichPersonalAccountProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_personal_account'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, pytest_raises_ctx(exc.MULTIPLE_PERSONAL_ACCOUNTS), None),
            ({'service_code': False}, None, {}, pytest_raises_ctx(exc.MULTIPLE_PERSONAL_ACCOUNTS), None),
            ({'service_code': None}, None, {}, None, 'pa_no_service_code'),
            ({'service_code': 'YANDEX_SERVICE'}, None, {}, None, 'pa_service_code'),
        ],
        ids=[
            'no_params_multiple_personal_accounts',
            'service_code_False_multiple_personal_accounts',
            'service_code_None_found_pa',
            'service_code_YANDEX_SERVICE_found_pa',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['contract'] = fixture_data['contract']
        # store['service_id'] = 666

        ctx = PartnerBalanceUnitMediator(session, None, 666, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        assert prepared_test_data['ctx'].data['personal_account'] == fixture_data[expected_data]


class TestEnrichMultiplePersonalAccountProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_multiple_personal_accounts'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, ['pa_no_service_code', 'pa_service_code']),
            ({'service_codes_include': [None]}, None, {}, None, ['pa_no_service_code']),
            ({'service_codes_exclude': ['YANDEX_SERVICE']}, None, {}, None, ['pa_no_service_code']),
            ({'service_codes_include': [None, 'YANDEX_SERVICE']}, None, {}, None, ['pa_service_code', 'pa_no_service_code']),
            ({'service_codes_exclude': [None, 'YANDEX_SERVICE']}, None, {}, None, []),
            ({'service_codes_include': ['NOT_EXISTENT_SERVICE']}, None, {}, None, []),

        ],
        ids=[
            'no_params',
            'service_codes_include_single',
            'service_codes_exclude_single',
            'service_codes_include_multiple',
            'service_codes_exclude_multiple',
            'service_codes_include_non_existent',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['contract'] = fixture_data['contract']

        ctx = PartnerBalanceUnitMediator(session, None, 666, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        assert set(prepared_test_data['ctx'].data['personal_accounts']) == set(fixture_data[ed] for ed in expected_data)


class TestPlaceCustomResultItemsProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_custom_output_items'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, {}),
            ({'varenik': 'pelmen'}, None, {}, None, {'varenik': 'pelmen'}),
            ({'varenik': 'pelmen', u'ёёё': u'ёёё'}, None, {}, None, {'varenik': 'pelmen', u'ёёё': u'ёёё'}),
        ],
        ids=[
            'custom_results_empty',
            'one_custom_result_placed',
            'two_custom_result_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):

        assert prepared_test_data['ctx'].output == expected_data


class TestPlacePersonalAccountDataProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_personal_account_data'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, {}),
            ({'receipt_sum': True}, None, {}, None, {'ReceiptSum': 0}),
            ({'receipt_sum_1c': True}, None, {}, None, {'ReceiptSum1C': 0}),
            ({'external_id': True}, {'external_id': 'AAA-BBB-CCC'}, {}, None,
             {'PersonalAccountExternalID': 'AAA-BBB-CCC'}),
            ({'receipt_sum': True, 'receipt_sum_1c': True, 'consume_sum': True, 'act_sum': True}, None, {}, None,
             {'ReceiptSum': 0, 'ReceiptSum1C': 0, 'ConsumeSum': 100000, 'ActSum': 100000}),
        ],
        ids=[
            'no_params_no_result',
            'receipt_sum_passed',
            'receipt_sum_1c_passed',
            'external_id_passed',
            'all_params_passed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['personal_account'] = fixture_data['pa_no_service_code']
        if execution_params and 'external_id' in execution_params:
            store['personal_account'].external_id = execution_params['external_id']
        ctx = PartnerBalanceUnitMediator(session, None, None, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):

        assert prepared_test_data['ctx'].output == expected_data


class TestPlaceMultiplePersonalAccountDataProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_multiple_personal_accounts_data'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, {}),
            ({'fields': ['receipt_sum']}, None, {}, None, {'receipt_sum': 0}),
        ],
        ids=[
            'no_params',
            'fields_receipt_sum_passed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['personal_accounts'] = [fixture_data['pa_no_service_code'], fixture_data['pa_service_code']]
        ctx = PartnerBalanceUnitMediator(session, None, None, None)
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        expected_data_constructed = set()
        for pa in [fixture_data['pa_no_service_code'], fixture_data['pa_service_code']]:
            pa_data = {
                'id': pa.id,
                'external_id': pa.external_id,
                'service_code': pa.service_code,
            }
            if expected_data:
                pa_data.update(expected_data)
            expected_data_constructed.add(FrozenDict(pa_data))

        output_data = {FrozenDict(d) for d in prepared_test_data['ctx'].output['PersonalAccounts']}
        assert output_data == expected_data_constructed


class TestPlaceActAndDebtInfoProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_act_and_debt_info'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None,
             {
                 'ActSum': DU('100000', 'RUB'),
                 'ExpiredDT': ACT_PAYMENT_TERM_DT.isoformat(),
                 'ExpiredDebtAmount': DU('100000', 'RUB'),
                 'FirstDebtAmount': DU('100000', 'RUB'),
                 'FirstDebtFromDT': ACT_DT.isoformat(),
                 'FirstDebtPaymentTermDT': ACT_PAYMENT_TERM_DT.isoformat(),
                 'LastActDT': ACT_DT.isoformat()
             }),
        ],
        ids=[
            'default_case',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session,
                    fixture_data):
        store['personal_account'] = fixture_data['pa_no_service_code']
        # store['dt'] = dt_(2100, 2, 2)

        ctx = PartnerBalanceUnitMediator(session, None, None, dt_(2100, 2, 2))
        store.update(ctx.data)
        ctx.data = store
        return {'ctx': ctx}

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):

        assert prepared_test_data['ctx'].output == expected_data


class TestPlaceMetadataLogbrokerTopicProcessingUnit(TestProcessingUnitBase):
    unit_name = 'place_metadata_lb_topic'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, pytest_raises_ctx(exc.COMMON_JSONSCHEMA_VALIDATION_EXCEPTION), None),
            ({'topic': 'partner-fast-balance-zaxi'}, None, {}, None,
             {'meta': {'logbroker': {'topic': 'partner-fast-balance-zaxi'}}}),
        ],
        ids=[
            'no_topic_param',
            'with_topic_param',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session,
             fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session,
                     fixture_data)

    def check_results(self, params, execution_params, store, exception_context, expected_data,
                      unit, prepared_test_data, session, fixture_data):
        assert prepared_test_data['ctx'].output == expected_data
