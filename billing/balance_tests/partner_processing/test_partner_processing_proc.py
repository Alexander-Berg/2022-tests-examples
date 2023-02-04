# -*- coding: utf-8 -*-

import datetime
from contextlib import contextmanager
from decimal import Decimal as D

import pytest
from mock import MagicMock

import balance.constants as const
import balance.exc as exc
import tests.object_builder as ob
from balance import mapper
from balance.processors.partner_processing_proc import (
    BaseProcessingUnit,
    # DefaultProcessingManagementUnit,  # импорт для регистрации метаклассом
    NoErrorReturn,
    PartnerProcessingProcContext,
    UnitDispatcher,
    UnitTypes,
    MODULE_NAME,
)
from butils.decimal_unit import DecimalUnit as DU

dt = datetime.datetime


@contextmanager
def no_exception_ctx(*args, **kwargs):
    yield


def pytest_raises_ctx(*args, **kwargs):
    def wrapper():
        return pytest.raises(*args, **kwargs)
    return wrapper


class ExceptionUnit(BaseProcessingUnit):
    unit_name = 'exception_unit'

    def execute(self, ctx):
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
                dt=dt.now(),
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
    return invoice


@pytest.fixture(scope='module')
def fixture_data(modular_session):
    session = modular_session
    client = ob.ClientBuilder().build(session).obj
    contract = ob.ContractBuilder(
        client=client,
        is_signed=dt.now(),
        firm_id=const.FirmId.YANDEX_OOO,
        currency=810,
    ).build(session).obj
    current_signed = contract.current_signed()

    pa_service_code = create_invoice(client)
    pa_service_code.contract = contract
    pa_service_code.type = 'personal_account'
    session.flush()
    # экстпропc должен проставиться на classname PersonalAccount
    session.expunge(pa_service_code)
    pa_service_code = session.query(mapper.PersonalAccount).getone(pa_service_code.id)
    pa_service_code.service_code = 'YANDEX_SERVICE'

    pa_no_service_code = create_invoice(client)
    pa_no_service_code.contract = contract
    pa_no_service_code.type = 'personal_account'

    pa_no_service_code.acts[0].dt = dt(2100, 1, 1)

    prepay = create_invoice(client)
    prepay.contract = contract
    prepay.type = 'prepayment'

    cpf_online = ob.OebsCashPaymentFactBuilder(
        amount=1,
        operation_type=const.OebsOperationType.ONLINE,
        invoice=pa_service_code).build(session).obj

    cpf_insert = ob.OebsCashPaymentFactBuilder(
        amount=10,
        operation_type=const.OebsOperationType.INSERT,
        invoice=pa_service_code).build(session)

    cpf_avans = ob.OebsCashPaymentFactBuilder(
        amount=100,
        operation_type=const.OebsOperationType.SF_AVANS,
        invoice=pa_service_code).build(session)

    cpf_insert_ya_netting = ob.OebsCashPaymentFactBuilder(
        amount=1000,
        operation_type=const.OebsOperationType.INSERT_YA_NETTING,
        invoice=pa_service_code).build(session)

    correction = mapper.ThirdPartyCorrection.create_correction(
        current_signed,
        dt.now(),
        const.ServiceId.BLUE_PAYMENTS,
        amount=D('388.5'),
        invoice=pa_service_code,
        payment_type='correction_commission',
        transaction_type='refund'
    )
    session.add(correction)
    correction.apply(receipt_management='no_receipt_management')
    correction = mapper.ThirdPartyCorrection.create_correction(
        current_signed,
        dt.now(),
        const.ServiceId.BLUE_PAYMENTS,
        amount=D('388.5'),
        invoice=pa_service_code,
        payment_type='correction_commission',
        transaction_type='payment'
    )
    session.add(correction)
    correction.apply(receipt_management='no_receipt_management')
    session.flush()

    return {
        'contract': contract,
        'current_signed': current_signed,
        'pa_service_code': pa_service_code,
        'pa_no_service_code': pa_no_service_code,
        'prepay': prepay,
        'cpf_online': cpf_online,
        'cpf_insert': cpf_insert,
        'cpf_avans': cpf_avans,
        'cpf_insert_ya_netting': cpf_insert_ya_netting,
    }


class TestManagementUnitBase(object):
    unit_type = UnitTypes.management_unit
    unit_name = None

    def prepare_env(self, code, queue, object_id, classname, on_dt, export_input, processing_units,
                    exception_context, expected_data, session):
        return None

    def check_results(self, code, queue, object_id, classname, on_dt, export_input, processing_units,
                      prepared_test_data, expected_data, management_unit, session):
        pass

    def execute(self, code, queue, object_id, classname, on_dt, export_input, processing_units,
                exception_context, expected_data, session):
        if exception_context is None:
            exception_context = no_exception_ctx
        prepared_test_data = self.prepare_env(code, queue, object_id, classname, on_dt, export_input, processing_units,
                                              exception_context, expected_data, session)
        session.flush()
        management_unit = None
        with exception_context() as ec:
            management_unit_cls = UnitDispatcher.get_unit(MODULE_NAME, self.unit_type, self.unit_name)
            management_unit = management_unit_cls(session, code, queue, object_id,
                                                  classname, on_dt, export_input=export_input,
                                                  processing_units=processing_units)
            management_unit.process()
        if exception_context is not no_exception_ctx:
            return
        session.flush()
        self.check_results(code, queue, object_id, classname, on_dt, export_input, processing_units,
                           prepared_test_data, expected_data, management_unit, session)


class TestDefaultProcessingManagementUnitErrors(TestManagementUnitBase):
    unit_name = 'default_processing_management'

    @pytest.mark.parametrize(
        'code, queue, object_id, classname, on_dt, export_input, processing_units, exception_context, expected_data', [
            ('blue_market_netting', 'PARTNER_PROCESSING', 666, 'Contract', dt.now(), {'forced': 1},
             [123],
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_UNIT_EXCEPTION,
                               match='Unit\(module_name: partner_processing_proc, unit_type: management_unit, '
                                     'unit_name: default_processing_management, '
                                     'execution_name: None\): wrong unit format in processing unit config'), None
             ),
            ('blue_market_netting', 'PARTNER_PROCESSING', 666, 'Contract', dt.now(), {'forced': 1},
             [{}],
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_UNIT_EXCEPTION,
                               match='Unit\(module_name: partner_processing_proc, unit_type: management_unit, '
                                     'unit_name: default_processing_management, '
                                     'execution_name: None\): no unit name in processing unit config'), None
             ),
            ('blue_market_netting', 'PARTNER_PROCESSING', 666, 'Contract', dt.now(), {'forced': 1},
             ['not_existing_unit_name'],
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_DISPATCHER_EXCEPTION,
                               match='Partner processing dispatcher error: module_name: PARTNER_PROCESSING_PROC '
                                     'unit_type: PROCESSING_UNIT unit_name: '
                                     'NOT_EXISTING_UNIT_NAME not registered'), None
             ),
            ('blue_market_netting', 'PARTNER_PROCESSING', 666, 'Contract', dt.now(), {'forced': 1},
             ['success_unit', 'no_error_return_unit', 'exception_unit'], None, {'success_unit': None}
             ),
            ('blue_market_netting', 'PARTNER_PROCESSING', 666, 'Contract', dt.now(), {'forced': 1},
             ['exception_unit', 'success_unit'], pytest_raises_ctx(Exception, match='SHEL I UPAL'), None
             ),
            ('blue_market_netting', 'PARTNER_PROCESSING', 666, 'Contract', dt.now(), {'forced': 1},
             [{'unit': 'exception_unit', 'execution_params': {'suppress_exception': True}}, 'success_unit'], None,
             {'success_unit': None}),

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
    def test(self, code, queue, object_id, classname, on_dt, export_input, processing_units, exception_context,
             expected_data, session):
        self.execute(code, queue, object_id, classname, on_dt, export_input, processing_units, exception_context,
                     expected_data, session)


class TestProcessingUnitBase(object):
    unit_type = UnitTypes.processing_unit
    unit_name = None

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        return None

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        pass

    def execute(self, params, execution_params, store, exception_context, expected_data, session, fixture_data):
        if exception_context is None:
            exception_context = no_exception_ctx
        prepared_test_data = self.prepare_env(params, execution_params, store, exception_context, expected_data,
                                              session, fixture_data)
        session.flush()
        unit = None
        ctx = PartnerProcessingProcContext(session=session, store=store)
        with exception_context() as ec:
            unit_cls = UnitDispatcher.get_unit(MODULE_NAME, self.unit_type, self.unit_name)
            unit = unit_cls(params=params, execution_params=execution_params)
            unit.process(ctx)
        if exception_context is not no_exception_ctx:
            return
        session.flush()
        self.check_results(params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                           session, fixture_data)


class TestEnrichPlaceValueToStore(TestProcessingUnitBase):
    unit_name = 'enrich_place_object_to_store'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({}, None, {},
             pytest_raises_ctx(exc.COMMON_JSONSCHEMA_VALIDATION_EXCEPTION,
                               match="'value_to_store' is a required property"),
             None),
            ({'value_to_store': 666}, {'substitute_store_names_mapping': {'to_key': 'key_666'}}, {}, None, None),
        ],
        ids=[
            'value_to_store_not_passed',
            'success_enrich_place_object_to_store',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        assert store['key_666'] == 666


class TestEnrichCopyStoreObjectProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_copy_store_object'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({}, {'substitute_store_names_mapping': {'from_key': 'not_existing_key'}}, {},
             pytest_raises_ctx(exc.PARAM_NOT_IN_STORE_EXCEPTION,
                               match='Param not_existing_key not in store'),
             None),
            ({'use_deepcopy': True},
             {'substitute_store_names_mapping': {'from_key': 'from', 'to_key': 'to'}},
             {'from': {'a': {'b': 'c'}}}, None, None),
            ({'use_deepcopy': False},
             {'substitute_store_names_mapping': {'from_key': 'from', 'to_key': 'to'}},
             {'from': {'a': {'b': 'c'}}}, None, None),
        ],
        ids=[
            'from_key_not_exists',
            'use_deepcopy',
            'not_use_deepcopy',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        if params['use_deepcopy']:
            assert store['to'] is not store['from']
        else:
            assert store['to'] is store['from']
        assert store['from']['a']['b'] == store['to']['a']['b'] == 'c'


class TestEnrichNowOnDtProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_now_on_dt'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'forced': True}, {}, {'on_dt': dt(2000, 1, 1)}, None, {'>': dt(2000, 1, 1)}),
            ({'forced': False}, {}, {'on_dt': dt(2000, 1, 1)}, None, {'==': dt(2000, 1, 1)}),
            ({}, {}, {'on_dt': None}, None, {'>=': dt.now()}),
        ],
        ids=[
            'forced_overwrite',
            'not_forced_not_overwrite',
            'not_forced_fill_empty'
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        store['session'] = session

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        comparator, _date = expected_data.items()[0]
        if comparator == '>':
            assert store['on_dt'] > _date
        elif comparator == '==':
            assert store['on_dt'] == _date
        elif comparator == '>=':
            assert store['on_dt'] >= _date
        else:
            raise Exception('Undescribed comparator')


class TestEnrichContractProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_contract'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, None),
        ],
        ids=[
            'contract_selected_and_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        contract = fixture_data['contract']
        store['object_id'] = contract.id
        return contract

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        contract = prepared_test_data
        assert store['contract'] is contract


class TestEnrichCurrentSignedProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_current_signed'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {}, None, None),
        ],
        ids=[
            'current_signed_placed',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        contract = fixture_data['contract']
        store['contract'] = contract
        store['on_dt'] = None
        return contract

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        contract = prepared_test_data
        assert store['current_signed']._contract is contract


class TestSkipNotSignedContractProcessingUnit(TestProcessingUnitBase):
    unit_name = 'skip_not_signed_contract'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {'current_signed': None}, pytest_raises_ctx(NoErrorReturn), None),
            (None, None, {'current_signed': object()}, None, None),
        ],
        ids=[
            'current_signed_None',
            'current_signed_not_None',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)


class TestSkipFutureContractProcessingUnit(TestProcessingUnitBase):
    unit_name = 'skip_future_contract'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            (None, None, {'current_signed': None, 'on_dt': dt(1986, 2, 3)},
             pytest_raises_ctx(exc.NOT_NONE_STORE_PARAM_REQUIRED, match='not None param current_signed required'), None),
            (None, None, {'current_signed': MagicMock(dt=dt(1986, 2, 3)), 'on_dt': None},
             pytest_raises_ctx(exc.NOT_NONE_STORE_PARAM_REQUIRED, match='not None param on_dt required'), None),
            (None, None, {'current_signed': MagicMock(dt=dt(1986, 2, 3)),'on_dt': dt(1986, 2, 3)}, None, None),
            (None, None, {'current_signed': MagicMock(dt=dt(1986, 2, 3)), 'on_dt': dt(1986, 2, 2)},
             pytest_raises_ctx(NoErrorReturn), None),
        ],
        ids=[
            'current_signed_None',
            'on_dt_None',
            'contract_not_future',
            'contract_future',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)


class TestSkipNoNettingContractProcessingUnit(TestProcessingUnitBase):
    unit_name = 'skip_no_netting_contract'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({}, None, {'current_signed': MagicMock(netting=1)}, None, None),
            ({}, None, {'current_signed': MagicMock(netting=0)},
             pytest_raises_ctx(NoErrorReturn), None),
        ],
        ids=[
            'ok_netting',
            'skip_no_netting',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)


class TestSkipNoServicesInContractProcessingUnit(TestProcessingUnitBase):
    unit_name = 'skip_no_services_in_contract'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'services': [111, 128]}, None, {'current_signed': MagicMock(services={111, 128, 666})}, None, None),
            ({'services': [111, 127]}, None, {'current_signed': MagicMock(services={111, 128, 666})},
             pytest_raises_ctx(NoErrorReturn), None),
        ],
        ids=[
            'ok',
            'not_ok'
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)


class TestSetDailyStateProcessingUnit(TestProcessingUnitBase):
    unit_name = 'set_daily_state'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'forced': True}, None, {'contract': MagicMock(daily_state=dt(2020, 2, 3)), 'on_dt': dt(2020, 3, 3)},
             None, dt(2020, 3, 3)),
            ({}, None, {'contract': MagicMock(daily_state=None), 'on_dt': dt(2020, 3, 3)},
             None, dt(2020, 3, 3)),
            ({'forced': False}, None, {'contract': MagicMock(daily_state=dt(2020, 2, 3)), 'on_dt': dt(2020, 3, 3)},
             None, dt(2020, 2, 3)),

        ],
        ids=[
            'forced',
            'not_forced_empty_and_set',
            'not_forced_not_empty_skip',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        assert store['contract'].daily_state == expected_data


class TestEnrichForcedFlagFromExportInputProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_forced_flag_from_export_input'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({}, None, {'export_input': {'forced': True}}, None, True,),
            ({}, None, {'export_input': {}}, None, False,)

        ],
        ids=[
            'forced_enriched',
            'forced_default_false',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        assert store['forced'] == expected_data


class TestSkipByDailyStateIfNotForcedProcessingUnit(TestProcessingUnitBase):
    unit_name = 'skip_by_daily_state_if_not_forced'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({}, None, {'forced': True, 'contract': MagicMock(daily_state=dt(2020, 3, 3)),
                        'current_signed': MagicMock(dt=dt(2020, 3, 3)), 'on_dt': dt(2020, 3, 3)},
             None, None),
            ({}, None, {'forced': False, 'contract': MagicMock(daily_state=dt(2020, 3, 3)),
                        'current_signed': MagicMock(dt=dt(2020, 3, 3)), 'on_dt': dt(2020, 3, 3)},
             pytest_raises_ctx(NoErrorReturn), None),
            ({}, None, {'forced': False, 'contract': MagicMock(daily_state=dt(2020, 2, 3)),
                        'current_signed': MagicMock(dt=dt(2020, 2, 3)), 'on_dt': dt(2020, 3, 3)},
             None, None),

        ],
        ids=[
            'forced_no_skip',
            'processed_this_day_skipped',
            'not_processed_this_day_not_skipped',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)


class TestEnrichInvoicesByContractProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_invoices_by_contract'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({}, None, {}, None, ['pa_service_code', 'pa_no_service_code', 'prepay']),
            ({'invoice_type': 'personal_account', 'service_code': 'YANDEX_SERVICE', 'mquery': {'consumes.0.order.service.id': 7}},
             None, {}, None, ['pa_service_code']),
            ({'mquery': {'type': 'personal_account'}}, None, {}, None, ['pa_no_service_code', 'pa_service_code']),
            ({'service_code': 'OLOLO'}, None, {},
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_UNIT_EXCEPTION, match='service_code param supported only for invoice_type: "personal_account"'), []),
        ],
        ids=[
            'success_InvoicesByContract_no_params',
            'success_InvoicesByContract_all_params',
            'success_InvoicesByContract_mquery_type',
            'fail_no_personal_account_service_code_filter',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):

        contract = fixture_data['contract']
        store.update({
            'contract': contract,
        })

        return {
            'pa_service_code': fixture_data['pa_service_code'],
            'pa_no_service_code': fixture_data['pa_no_service_code'],
            'prepay': fixture_data['prepay'],
        }

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        expected_invoices = {prepared_test_data[inv_name] for inv_name in expected_data}
        assert set(store['invoices']) == expected_invoices


class TestEnrichActsByInvoicesProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_acts_by_invoices'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'filter_on_dt': False}, None, {'on_dt': dt(2050, 1, 1)}, None, ['pa_service_code', 'pa_no_service_code', 'prepay']),
            ({}, None, {'on_dt': dt(2050, 1, 1)}, None, ['pa_service_code', 'prepay']),
            ({'mquery': {'invoice.service_code': 'YANDEX_SERVICE'}, 'filter_on_dt': False}, None, {'on_dt': dt(2050, 1, 1)}, None, ['pa_service_code']),
            ({'filter_on_dt': True}, None, {'on_dt': None},
             pytest_raises_ctx(exc.NOT_NONE_STORE_PARAM_REQUIRED,
                               match='Unit\(module_name: partner_processing_proc, unit_type: processing_unit, '
                                     'unit_name: enrich_acts_by_invoices, '
                                     'execution_name: None\): not None param on_dt required'), []),
        ],
        ids=[
            'success_no_params',
            'success_filter_on_dt',
            'success_mquery',
            'no_on_dt_while_filter',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        invoices = [fixture_data[inv_name] for inv_name in ['pa_service_code', 'pa_no_service_code', 'prepay']]
        store.update({
            'invoices': invoices,
        })

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        expected_invoices = {fixture_data[inv_name] for inv_name in expected_data}
        expected_acts = {a for inv in expected_invoices for a in inv.acts}
        assert set(store['acts']) == expected_acts


class TestEnrichTargetNettingInvoiceFromInvoicesProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_target_netting_invoice_from_invoices'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'mquery': {'service_code': 'YANDEX_SERVICE'}}, None, {}, None, 'pa_service_code'),
            ({}, None, {},
             pytest_raises_ctx(exc.COMMON_PARTNER_PROCESSING_UNIT_EXCEPTION,
                               match='only one invoice should remain'), []),
        ],
        ids=[
            'success',
            'several_invoices',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        invoices = [fixture_data[inv_name] for inv_name in ['pa_service_code', 'pa_no_service_code', 'prepay']]
        store.update({
            'invoices': invoices,
        })

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        expected_invoice = fixture_data[expected_data]
        assert store['target_netting_invoice'] is expected_invoice


class TestEnrichCashPaymentFactInsertNettingAmountProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_cash_payment_fact_amount'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({}, None, {}, None, ['cpf_online', 'cpf_insert', 'cpf_insert_ya_netting']),
            ({'include_operation_types': ['ONLINE', 'SF_AVANS'], 'filter_not_money_types': False}, None, {}, None,
             ['cpf_online', 'cpf_avans']),
            ({'exclude_operation_types': ['INSERT']}, None, {}, None,
             ['cpf_online', 'cpf_insert_ya_netting']),
            ({'exclude_operation_types': ['INSERT'], 'include_operation_types': ['INSERT_YA_NETTING']}, None, {}, None,
             ['cpf_insert_ya_netting']),
        ],
        ids=[
            'success_no_params',
            'no_filter_nomoney_filter_include',
            'filter_exclude',
            'exclude_and_inclde_filters',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        store.update({
            'target_netting_invoice': fixture_data['pa_service_code'],
        })

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        expected_cpf = [fixture_data[cpf_name] for cpf_name in expected_data]
        assert store['cash_payment_fact_amount'] == sum(cpf.amount for cpf in expected_cpf) or D(0)


class TestEnrichThirdpartyCorrectionsAmountProcessingUnit(TestProcessingUnitBase):
    unit_name = 'enrich_thirdparty_corrections_amount'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'filter_by_contract': True}, None, {}, None, D(777)),
            ({'filter_by_invoice': True}, None, {}, None, D(777)),
            ({'filter_by_contract': True, 'filter_by_invoice': True}, None, {}, None, D(777)),
            ({'filter_by_contract': True, 'netting_services': [const.ServiceId.BLUE_PAYMENTS]}, None, {}, None, D(777)),
            ({'filter_by_contract': True, 'netting_services': [const.ServiceId.BLUE_SRV]}, None, {}, None, D(0)),
            ({'filter_by_contract': True, 'payment_types': ['correction_commission', 'ololo']}, None, {}, None, D(777)),
            ({'filter_by_contract': True, 'payment_types': ['ololo']}, None, {}, None, D(0)),
            ({'filter_by_contract': True, 'transaction_types': ['refund']}, None, {}, None, D('388.5')),
            ({'filter_by_contract': True, 'transaction_types': ['payment']}, None, {}, None, D('388.5')),
            ({'filter_by_contract': True, 'filter_by_invoice': True, 'transaction_types': ['refund'],
              'netting_services': [const.ServiceId.BLUE_PAYMENTS], 'payment_types': ['correction_commission']},
             None, {}, None, D('388.5')),
        ],
        ids=[
            'contract_filter',
            'invoice_filter',
            'contract_invoice_filter',
            'netting_service_filter',
            'netting_services_fail',
            'payment_types_filter',
            'payment_types_fail',
            'transaction_types_filter_refund',
            'transaction_types_filter_payment',
            'all_filters'
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        store.update({
            'target_netting_invoice': fixture_data['pa_service_code'],
            'target_netting_contract': fixture_data['contract'],
        })

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        assert store['thirdparty_corrections_amount'] == expected_data


class TestActNettingProcessingUnitAllowNegativeNettingParam(TestProcessingUnitBase):
    unit_name = 'act_netting'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'allow_negative_netting': False}, None,
             {
                 'acts': [MagicMock(amount=DU(100, 'RUB')), MagicMock(amount=DU(200, 'USD'))],
                 'cash_payment_fact_insert_netting_amount': D(0),
                 'cash_payment_fact_real_payments_amount': D(0),
                 'thirdparty_corrections_refunds_amount': D(400),
                 'thirdparty_corrections_payments_amount': D(0),
             },
             None, [D('0'), D('0')]),
            ({}, None,
             {
                 'acts': [MagicMock(amount=DU(100, 'RUB')), MagicMock(amount=DU(200, 'USD'))],
                 'cash_payment_fact_insert_netting_amount': D(0),
                 'cash_payment_fact_real_payments_amount': D(0),
                 'thirdparty_corrections_refunds_amount': D(400),
                 'thirdparty_corrections_payments_amount': D(0),
             },
             None, [D('-100'), D('0')]),
            ({}, None,
             {
                 'acts': [MagicMock(amount=DU(100, 'RUB')), MagicMock(amount=DU(200, 'USD'))],
                 'cash_payment_fact_insert_netting_amount': D(200),
                 'cash_payment_fact_real_payments_amount': D(50),
                 'thirdparty_corrections_refunds_amount': D(200),
                 'thirdparty_corrections_payments_amount': D(25),
             },
             None, [D('100'), D('25')]),
        ],
        ids=[
            'not_allow_negative_netting',
            'allow_negative_netting',
            'normal_case',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        refund_amount, payment_amount = expected_data
        assert store['netting_refund_amount'] == refund_amount
        assert store['netting_payment_amount'] == payment_amount


class TestMakeThirdpartyCorrectionProcessingUnit(TestProcessingUnitBase):
    unit_name = 'make_thirdparty_correction_transaction'

    @pytest.mark.parametrize(
        'params, execution_params, store, exception_context, expected_data', [
            ({'netting_service': const.ServiceId.BLUE_PAYMENTS,}, None, {}, None,
             {'internal': 0, 'transaction_type': 'refund', 'payment_type': 'correction_commission',
              'paysys_type_cc': 'yandex', 'receipt_sum': D('0')}),
            ({'netting_service': const.ServiceId.BLUE_PAYMENTS, 'internal': 1, 'transaction_type': 'payment',
              'payment_type': 'correction_commission', 'paysys_type_cc': 'olololo',
              'receipt_management': 'no_receipt_management'}, None, {}, None,
             {'internal': 1, 'transaction_type': 'payment', 'payment_type': 'correction_commission',
              'paysys_type_cc': 'olololo', 'receipt_sum': D('0')}),
            ({'netting_service': const.ServiceId.BLUE_PAYMENTS, 'internal': 0, 'transaction_type': 'refund',
              'payment_type': 'correction_commission', 'paysys_type_cc': 'yandex',
              'receipt_management': 'correction_netting'}, None, {}, None,
             {'internal': 0, 'transaction_type': 'refund', 'payment_type': 'correction_commission',
              'paysys_type_cc': 'yandex', 'receipt_sum': D('777')}),
        ],
        ids=[
            'default_params',
            'specific_params_no_receipt_management',
            'specific_params_correction_netting_receipt_management',
        ]
    )
    def test(self, params, execution_params, store, exception_context, expected_data, modular_session, fixture_data):
        self.execute(params, execution_params, store, exception_context, expected_data, modular_session, fixture_data)

    def prepare_env(self, params, execution_params, store, exception_context, expected_data, session, fixture_data,
                    *args, **kwargs):
        store.update({
            'target_netting_current_signed': fixture_data['current_signed'],
            'netting_dt': dt.now(),
            'netting_amount': D(777),
            'target_netting_invoice': fixture_data['pa_service_code'],
        })

    def check_results(self, params, execution_params, store, exception_context, expected_data, unit, prepared_test_data,
                      session, fixture_data, *args, **kwargs):
        correction_id = store['netting_transaction'].id
        correction = session.query(mapper.ThirdPartyCorrection).getone(correction_id)
        receipt_sum = expected_data.pop('receipt_sum')
        assert correction.invoice.receipt_sum == receipt_sum
        for attr_name, value in expected_data.iteritems():
            assert getattr(correction, attr_name) == value
