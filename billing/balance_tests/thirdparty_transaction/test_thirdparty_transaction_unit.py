# -*- coding: utf-8 -*-
import json
from collections import Counter

import datetime
import hamcrest as hm
import copy
import pytest
import mock

from tests.object_builder import ContractBuilder, PersonBuilder, ClientBuilder, \
    RequestBuilder, BasketBuilder, BasketItemBuilder, OrderBuilder, ProductBuilder

from billing.contract_iface.constants import ContractTypeId

from balance.mapper import ServiceProduct, MigrationNewBilling
from balance.constants import FirmId, ServiceId
from balance.thirdparty_transaction import (
    CONTRACT_SETTER_CONFIGURATION,
    ContractUnit, ContractResolver, BaseValidatorChain, BaseModifierChain, Skip, Delay, ForcePartner, ThirdpartyError,
    get_force_partner_id, SetNewBillingTransactionInternalUnit, ProductMappingUnit,
)
from tests.balance_tests.thirdparty_transaction.common import TestThirdpartyTransactions

DEFAULT_CONTRACT_TYPE = 'GENERAL'


def create_contract(session,
                    contract_type=DEFAULT_CONTRACT_TYPE,
                    service_id=ServiceId.TICKETS_TO_EVENTS,
                    firm=FirmId.YANDEX_OOO,
                    person_type='ur',
                    sign_dt=datetime.datetime.now(),
                    **kwargs):
    person = PersonBuilder(type=person_type).build(session).get_obj()
    client = person.client
    contract = ContractBuilder(client=client, is_signed=sign_dt, services=service_id,
                               firm=firm, ctype=contract_type, **kwargs).build(session).get_obj()
    session.flush()
    return contract


class MockObject(object):
    """
    Класс для проверки обработки строк
    Умеет рекурсивно проставлять значения по пути вида 'a.b.c'
    """

    def __init__(self, **kwargs):
        for k, v in kwargs.items():
            self.set_value_by_path(k, v)

    def set_value_by_path(self, path, value):
        if isinstance(path, basestring):
            path = path.split('.')

        if len(path) == 1:
            setattr(self, path[0], value)
            return

        cur_key = path[0]
        if not getattr(self, cur_key, None):
            setattr(self, cur_key, MockObject())
        child = getattr(self, cur_key)
        child.set_value_by_path(path[1:], value)


@pytest.mark.usefixtures('session')
class TestContractUnit(object):
    @pytest.mark.parametrize('contract_type', ('GENERAL', 'SPENDABLE'))
    def test_contract_type_setter_configuration(self, contract_type):
        contract = create_contract(self.session, contract_type)
        cs = contract.current_signed(contract.col0.is_signed, active=True)
        contract_unit = ContractUnit(with_check=False)
        incoming_row = mock.MagicMock()
        result_row = MockObject()

        with mock.patch.object(contract_unit, 'get_current_signed_contract', return_value=cs):
            contract_unit.process_row(incoming_row, result_row)
        configuration = CONTRACT_SETTER_CONFIGURATION[contract_type]
        for key in configuration.keys():
            hm.assert_that(result_row, hm.has_property(key))
        hm.assert_that(result_row.__dict__, hm.has_length(len(configuration)))

    def test_custom_setter_configuration(self):
        contract = create_contract(self.session, 'GENERAL')
        cs = contract.current_signed(contract.col0.is_signed, active=True)
        contract_unit = ContractUnit(custom_setter_configuration={'real_cs': 'cs', 'real_contract': 'cs.contract'},
                                     with_check=False)
        incoming_row = mock.MagicMock()
        result_row = MockObject()

        with mock.patch.object(contract_unit, 'get_current_signed_contract', return_value=cs):
            contract_unit.process_row(incoming_row, result_row)

        hm.assert_that(result_row, hm.has_property('real_cs', cs))
        hm.assert_that(result_row, hm.has_property('real_contract', contract))


# Тестируем ContractResolver и связанные с ним фильтры
@pytest.mark.usefixtures('session')
class TestContractResolver(object):
    BASE_FILTERS = [{'filter': {'name': 'partner'}}, {'filter': {'name': 'transaction_dt'}}]

    def get_base_filters(self, contract_type=DEFAULT_CONTRACT_TYPE):
        filters = copy.copy(self.BASE_FILTERS)
        filters.append({'filter': {'name': 'contract_type', 'params': {'contract_type': contract_type}}})
        return filters

    @staticmethod
    def base_checks(incoming_row, result_row, filters, needed_contract_exists):
        entry = dict(incoming_row=incoming_row, result_row=result_row)
        contract_resolver = ContractResolver(entry, filters)
        filtered_contracts = contract_resolver.resolve()
        hm.assert_that(filtered_contracts, hm.has_length(int(needed_contract_exists)))

    @pytest.mark.parametrize('contract_type', ('GENERAL', 'SPENDABLE'))
    @pytest.mark.parametrize('filter_contract_type', ('GENERAL', 'SPENDABLE'))
    def test_contract_type_filter(self, contract_type, filter_contract_type):
        contract = create_contract(self.session, contract_type)
        filters = self.get_base_filters(filter_contract_type)

        incoming_row = MockObject(transaction_dt=datetime.datetime.now())
        result_row = MockObject(partner=contract.client)

        self.base_checks(incoming_row, result_row, filters, contract_type == filter_contract_type)

    @pytest.mark.parametrize('filter_id, field', (('partner', 'partner'), ('client', 'client')))
    @pytest.mark.parametrize('is_right_client', (True, False))
    def test_partner_and_client_filter(self, filter_id, field, is_right_client):
        contract = create_contract(self.session)

        client = contract.client
        another_client = ClientBuilder().build(self.session).get_obj()

        # удалим дефолтный фильтр
        filters = [f for f in self.get_base_filters() if f['filter']['name'] != 'partner']
        filters.append({'filter': {'name': filter_id}})

        incoming_row = MockObject(transaction_dt=datetime.datetime.now())
        result_row = MockObject()
        result_row.set_value_by_path(field, client if is_right_client else another_client)

        self.base_checks(incoming_row, result_row, filters, is_right_client)

    @pytest.mark.parametrize('filter_id, field', (
        ('transaction_dt', 'transaction_dt'),
        ('payment_dt', 'payment.payment_dt'),
        ('order.start_dt', 'order.start_dt.local'),
    ))
    @pytest.mark.parametrize('is_right_time', (True, False))
    def test_contract_dt_filter(self, filter_id, field, is_right_time):
        contract = create_contract(self.session)
        client = contract.client

        # удалим дефолтный фильтр
        filters = [f for f in self.get_base_filters() if f['filter']['name'] != 'transaction_dt']
        filters.append({'filter': {'name': filter_id}})

        dt = contract.col0.is_signed
        dt_before_signed = dt - datetime.timedelta(days=10)

        incoming_row = MockObject()
        incoming_row.set_value_by_path(field, dt if is_right_time else dt_before_signed)
        result_row = MockObject(partner=client)

        self.base_checks(incoming_row, result_row, filters, is_right_time)

    @pytest.mark.parametrize('field', ('transaction_dt', 'order.start_dt.local'))
    @pytest.mark.parametrize('is_right_time', (True, False))
    def test_transaction_or_side_payment_dt_filter(self, field, is_right_time):
        filter_id = 'transaction_or_side_payment_dt'
        contract = create_contract(self.session)
        client = contract.client

        # удалим дефолтный фильтр
        filters = [f for f in self.get_base_filters() if f['filter']['name'] != 'transaction_dt']
        filters.append({'filter': {'name': filter_id}})

        dt = contract.col0.is_signed
        dt_before_signed = dt - datetime.timedelta(days=10)

        incoming_row = MockObject()
        incoming_row.set_value_by_path(field, dt if is_right_time else dt_before_signed)
        if field == 'transaction_dt':
            incoming_row.set_value_by_path('is_side_payment', True)
        result_row = MockObject(partner=client)

        self.base_checks(incoming_row, result_row, filters, is_right_time)

    @pytest.mark.parametrize('is_right_contract_id', (True, False))
    def test_contract_external_id_filter(self, is_right_contract_id):
        contract = create_contract(self.session)
        client = contract.client
        filters = self.get_base_filters()
        filters.append({'filter': {'name': 'contract.external_id'}})

        incoming_row = MockObject(transaction_dt=datetime.datetime.now())
        incoming_row.set_value_by_path('developer_payload.external_id',
                                       contract.external_id if is_right_contract_id else 'not')
        result_row = MockObject(partner=client)

        self.base_checks(incoming_row, result_row, filters, is_right_contract_id)

    @pytest.mark.parametrize('is_right_service', (True, False))
    def test_service_filter(self, is_right_service):
        contract_service_id = ServiceId.MUSIC
        filter_service_id = contract_service_id if is_right_service else ServiceId.ZEN

        contract = create_contract(self.session, service_id=contract_service_id)
        filters = self.get_base_filters()
        filters.append({'filter': {'name': 'service'}})

        incoming_row = MockObject(transaction_dt=datetime.datetime.now())
        result_row = MockObject(partner=contract.client, service_id=filter_service_id)

        self.base_checks(incoming_row, result_row, filters, is_right_service)

    @pytest.mark.parametrize('fiscal_nds', ('nds_18', 'nds_18_118', 'nds_20', 'nds_20_120', 'nds_0', 'nds_none'))
    @pytest.mark.parametrize('contract_commission', (ContractTypeId.NON_AGENCY, ContractTypeId.LICENSE))
    def test_commission_filter(self, fiscal_nds, contract_commission):
        contract = create_contract(self.session, commission=contract_commission)
        filter_commission = ContractTypeId.LICENSE if fiscal_nds in {'nds_0', 'nds_none'} else ContractTypeId.NON_AGENCY
        is_right_commission = contract_commission == filter_commission

        filters = self.get_base_filters()
        filters.append({'filter': {'name': 'commission'}})

        incoming_row = MockObject(transaction_dt=datetime.datetime.now(),
                                  is_payment=True, payment_fiscal_nds=fiscal_nds)

        result_row = MockObject(partner=contract.client)
        self.base_checks(incoming_row, result_row, filters, is_right_commission)

    @pytest.mark.parametrize('signed_linked_contract_exists', (True, False))
    def test_linked_contract_filter(self, signed_linked_contract_exists):
        linked_contract = create_contract(self.session) if signed_linked_contract_exists else None
        contract = create_contract(self.session, link_contract_id=getattr(linked_contract, 'id', None))
        filters = self.get_base_filters()
        filters.append({'filter': {'name': 'signed_linked_contract_exists'}})

        incoming_row = MockObject(transaction_dt=datetime.datetime.now())
        result_row = MockObject(partner=contract.client)

        self.base_checks(incoming_row, result_row, filters, signed_linked_contract_exists)

    @pytest.mark.parametrize('for_resident_firm', (True, False))
    @pytest.mark.parametrize('linked_contract_exists', (True, False))
    @pytest.mark.parametrize('is_right_service', (True, False))
    @pytest.mark.parametrize('is_right_firm', (True, False))
    def test_balalayka_filter(self, for_resident_firm, linked_contract_exists,
                              is_right_service, is_right_firm):
        linked_contract = create_contract(self.session) if linked_contract_exists else None
        contract_service = ServiceId.AUTORU
        filter_service = contract_service if is_right_service else ServiceId.ZEN
        resident_firm_id = FirmId.PROBKI
        non_resident_firm_id = FirmId.SERVICES_EU_AG

        right_firm = resident_firm_id if for_resident_firm else non_resident_firm_id

        contract = create_contract(self.session,
                                   service_id=contract_service,
                                   firm=right_firm if is_right_firm else FirmId.YANDEX_KZ,
                                   link_contract_id=getattr(linked_contract, 'id', None))

        filters = self.get_base_filters()
        filters.append({
            'filter': {
                'name': 'balalayka',
                'params': {
                    'firm_resident_id': resident_firm_id,
                    'firm_nonresident_id': non_resident_firm_id
                }
            }
        })

        incoming_row = MockObject(transaction_dt=datetime.datetime.now())
        result_row = MockObject(partner=contract.client, service_id=filter_service)

        self.base_checks(incoming_row, result_row, filters,
                         is_right_service and is_right_firm and
                         (for_resident_firm or not linked_contract_exists))


# Тестируем вкупе валидаторы
@pytest.mark.usefixtures('session')
class TestValidatorChain(object):
    @staticmethod
    def base_test(validators, field, data, with_error, entry_field='transaction'):
        """
        Базовый метод для тестирования валидаторов
        :param validators: Список конфигураций
        :param field: Поле, в объекте, в которое будет положена data
        :param data: Проверяемые валидатором данные
        :param with_error: Флаг - валидация должна завершится ошибкой
        :param entry_field: Поле в верхнеуровневом объекте, куда будет положен объект с полем data
        :return:
        """
        obj = MockObject()
        obj.set_value_by_path(field, data)
        entry = {entry_field: obj}
        validator = BaseValidatorChain(entry, validators)
        if with_error:
            with pytest.raises(Exception):
                validator.validate()
        else:
            validator.validate()

    @pytest.mark.parametrize('field, data, lengths, is_right_len', [
        ['a', [1, 2, 3], [3], True],
        ['c.d.c', [1, 2], [1, 2, 3], True],
        ['c.d.c', [1, 2], [1, 3], False],
    ])
    def test_length_validator(self, field, data, lengths, is_right_len):
        validators = [
            {'validator': {'name': 'length',
                           'params': {
                               'fields': ['.'.join(('transaction', field))],
                               'lengths': lengths}}}
        ]
        self.base_test(validators, field, data, not is_right_len)

    @pytest.mark.parametrize('data, lengths, is_right_len', [
        [[1, 2, 3], [3], True],
        [[1, 2], [1, 3], False],
    ])
    def test_transaction_orders_length_validator(self, data, lengths, is_right_len):
        validators = [
            {'validator': {'name': 'transaction_orders_length',
                           'params': {'lengths': lengths}}}
        ]
        self.base_test(validators, 'request.request_orders', data, not is_right_len)

    @pytest.mark.parametrize('field, subfield, data, values, is_contains', [
        ['a', '', 'b', ['a', 'b', 'c'], True],
        ['a.c', '', 'd', ['a', 'b', 'c'], False],
        ['a.c', '', 'c', ['a', 'b', 'c'], True],
        ['a', 'c', [{'c': 'a'}, {'c': 'b'}], ['a', 'b', 'c'], True],
        ['a', 'c', [{'c': 'a'}, {'c': 'd'}], ['a', 'b', 'c'], False],
    ])
    def test_contained_in_validator(self, field, subfield, data, values, is_contains):
        checked_field = '.'.join((field, subfield)) if subfield else field
        validators = [
            {'validator': {'name': 'contained_in',
                           'params': {
                               'fields': ['.'.join(('transaction', checked_field))],
                               'values': values}}}
        ]
        self.base_test(validators, field, data, not is_contains)

    @pytest.mark.parametrize('fees, values, is_contains', [
        [[None, 1], [None, 1, 2], True],
        [[1, 3], [None, 1, 2], False]
    ])
    def test_transaction_service_fee_contained_in_validator(self, fees, values, is_contains):
        validators = [
            {'validator': {'name': 'transaction_service_fee_contained_in',
                           'params': {'values': values}}}
        ]

        data = []
        for fee in fees:
            order = MockObject()
            order.set_value_by_path('order.service_product.service_fee', fee)
            data.append(order)

        self.base_test(validators, 'request.request_orders', data, not is_contains)

    def test_error_message(self):
        expected_message = '--Test message--'
        validators = [
            {'validator': {'name': 'transaction_orders_length',
                           'params': {'lengths': [1], 'error_message': expected_message}}}
        ]

        with pytest.raises(Exception) as e:
            self.base_test(validators, 'request.request_orders', [1, 2, 3], False)
        hm.assert_that(e.value.message, hm.equal_to(expected_message))

    @pytest.mark.parametrize('error, expected_exception', [
        ['skip', Skip],
        ['delay', Delay],
        ['default', Exception],
    ])
    def test_error_class(self, error, expected_exception):
        validators = [
            {'validator': {'name': 'transaction_orders_length',
                           'params': {'lengths': [2], 'error': error}}}
        ]

        with pytest.raises(expected_exception):
            self.base_test(validators, 'request.request_orders', [], False)


parametrize_rows_type = pytest.mark.parametrize('rows_type', ('json', 'request'))


def get_entry(d, field):
    return d[field]


class TestRowCopier(TestThirdpartyTransactions):
    def create_payment(self, sid=ServiceId.TICKETS2, contract=None, rows_type='request', **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=sid)
        contract = contract or self.create_contract(service_id=sid)
        payment = self.create_trust_payment(service_id=sid, thirdparty_service=thirdparty_service,
                                            contract=contract,
                                            actual_rows='{}_rows'.format(rows_type),
                                            **kwargs)

        return payment, contract

    @staticmethod
    def get_service_product(session, service_product_id):
        return session.query(ServiceProduct).filter(ServiceProduct.id == service_product_id).one()

    @staticmethod
    def get_service_fee(session, service_product_id):
        if not service_product_id:
            return None
        product = TestRowCopier.get_service_product(session, service_product_id)
        return product.service_fee

    @staticmethod
    def getter_for(item):
        return get_entry if type(item) == dict else getattr

    @staticmethod
    def get_expected_order(session, order, service_product):
        get = TestRowCopier.getter_for(order)
        if type(order) == dict:
            service_order_id = get(order, 'service_order_id_number')
            service_order_id_str = get(order, 'service_order_id')
            service_product = service_product or TestRowCopier.get_service_product(session,
                                                                                   get(order, 'service_product_id'))
        else:
            service_order_id = get(order, 'service_order_id')
            service_order_id_str = get(order, 'service_order_id_str')
            service_product = service_product or get(order, 'service_product')
        return {
            'commission_category': get(order, 'commission_category'),
            'service_id': int(get(order, 'service_id')),
            'service_order_id': service_order_id_str,
            'service_order_id_number': service_order_id,
            'service_product_id': service_product.id if service_product else None,
            'service_product_external_id': service_product.external_id if service_product else None,
        }

    @staticmethod
    def get_expected_row(session, row, copy_order_sum, service_product):
        get = TestRowCopier.getter_for(row)
        return hm.has_entries({
            'order': hm.has_entries(TestRowCopier.get_expected_order(session, get(row, 'order'), service_product)),
            'amount': str(get(row, 'amount') if copy_order_sum else 0),
            'is_copied': True
        })

    @staticmethod
    def get_expected_request_rows(payment, service_fee_copy, copy_order_sum, service_product):
        return [
            TestRowCopier.get_expected_row(payment.session, row, copy_order_sum, service_product)
            for row in payment.request.rows
            if row.order.service_product.service_fee in service_fee_copy
        ]

    @staticmethod
    def get_expected_json_rows(payment, service_fee_copy, copy_order_sum, service_product):
        return [
            TestRowCopier.get_expected_row(payment.session, row, copy_order_sum, service_product)
            for row in payment.payment_rows
            if TestRowCopier.get_service_fee(
                payment.session, row['order']['service_product_id']) in service_fee_copy
        ]

    @staticmethod
    def get_expected_rows(payment, rows_type, service_fee_copy, copy_order_sum, service_product):
        if rows_type == 'request':
            return TestRowCopier.get_expected_request_rows(payment, service_fee_copy, copy_order_sum, service_product)
        if rows_type == 'json':
            return TestRowCopier.get_expected_json_rows(payment, service_fee_copy, copy_order_sum, service_product)
        raise ValueError('Unknown rows type: "{}"'.format(rows_type))

    @staticmethod
    def check(payment, rows_type, actual_rows, service_fee_copy, copy_order_sum, service_product=None):
        expected_rows = TestRowCopier.get_expected_rows(payment, rows_type,
                                                        service_fee_copy, copy_order_sum, service_product)
        hm.assert_that(actual_rows, hm.has_length(len(expected_rows)))
        for row in expected_rows:
            hm.assert_that(actual_rows, hm.has_item(row))

    @staticmethod
    def common_modify(entry, modifiers, result_type='json'):
        modifier = BaseModifierChain(entry, modifiers)
        entry = modifier.modify()
        modified_payment = entry['transaction']
        if result_type == 'json':
            hm.assert_that(modified_payment.partner_payment_rows, hm.not_none())
            actual_rows = modified_payment.partner_payment_rows
        elif result_type == 'request':
            hm.assert_that(modified_payment.partner_request, hm.not_none())
            actual_rows = modified_payment.partner_request.rows
        else:
            raise ValueError('Unknown result type: "{}"'.format(result_type))
        return actual_rows

    @pytest.mark.parametrize('rows_type', ('json', 'request'))
    @pytest.mark.parametrize('service_fee_copy, copy_order_sum', [
        [(None, 1), True],
        [(None, 1), False],
        [(1,), False],  # Empty case
    ])
    def test_transaction_row_json_copier(self, service_fee_copy, copy_order_sum, rows_type):
        payment, _ = self.create_payment(rows_type=rows_type)
        entry = dict(transaction=payment)
        modifiers = [
            {'modifier': {
                'name': 'transaction_row_json_copier',
                'params': {'service_fee_copy': service_fee_copy, 'copy_order_sum': copy_order_sum}
            }}
        ]

        actual_rows = self.common_modify(entry, modifiers)
        self.check(payment, rows_type, actual_rows, service_fee_copy, copy_order_sum)

    @parametrize_rows_type
    @pytest.mark.parametrize('service_fee_copy, copy_order_sum', [
        [(None, 1), True],
        [(None, 1), False],
        [(1,), False],  # Empty case
    ])
    def test_transaction_row_new_order_by_service_product_json_copier(self, service_fee_copy, copy_order_sum,
                                                                      rows_type):
        payment, contract = self.create_payment(rows_type=rows_type)
        service_code, service_fee = 'YANDEX_SERVICE_WO_VAT', 666
        service_product = self.create_service_product(contract, payment.service_id, service_code, service_fee)
        entry = dict(transaction=payment)
        modifiers = [
            {'modifier': {
                'name': 'transaction_row_new_order_by_service_product_json_copier',
                'params': {'service_fee_copy': service_fee_copy, 'copy_order_sum': copy_order_sum,
                           'service_fee': service_fee}
            }}
        ]

        actual_rows = self.common_modify(entry, modifiers)
        self.check(payment, rows_type, actual_rows, service_fee_copy, copy_order_sum, service_product=service_product)

    def create_zaxi_payment(self, with_fee):
        sid = ServiceId.ZAXI
        contract = self.create_contract(service_id=sid)
        client = contract.client
        product = ProductBuilder()
        basket_rows = [BasketItemBuilder(quantity=1, order=OrderBuilder(client=client, product=product),
                                         desired_discount_pct=0, user_data='0')]
        if with_fee:
            basket_rows.append(BasketItemBuilder(quantity=1, order=OrderBuilder(client=client, product=product),
                                                 desired_discount_pct=0, user_data='1'))

        request = RequestBuilder(basket=BasketBuilder(
            rows=basket_rows, client=client)).build(self.session).obj
        request.request_sum = 100  # doesn't matter
        payment, _ = self.create_payment(sid=sid, contract=contract, amount=100, request=request)
        return payment, contract, sid

    @staticmethod
    def check_zaxi(session, actual_rows, template, copy_order_sum):
        hm.assert_that(actual_rows, hm.has_length(len(template)))

        for row in template:
            service_product = row['service_product']
            amount = row['amount']
            order = row['order']
            hm.assert_that(actual_rows, hm.has_item(
                hm.has_entries({
                    'order': hm.has_entries(TestRowCopier.get_expected_order(session, order, service_product)),
                    'amount': str(amount if copy_order_sum else 0),
                    'is_copied': True
                })
            ))

    @pytest.mark.parametrize('with_fee', [True, False])
    @pytest.mark.parametrize('copy_order_sum', [True, False])
    def test_transaction_row_zaxi_json_copier_request(self, with_fee, copy_order_sum):
        service_fee_copy = (None, 0, 1)
        payment, contract, sid = self.create_zaxi_payment(with_fee)
        service_product_666 = self.create_service_product(contract, sid, 'YANDEX_SERVICE', 666)
        service_product_667 = self.create_service_product(contract, sid, 'YANDEX_SERVICE', 667)
        entry = dict(transaction=payment)
        modifiers = [
            {'modifier': {
                'name': 'transaction_row_zaxi_json_copier',
                'params': {'service_fee_copy': service_fee_copy, 'copy_order_sum': copy_order_sum}
            }}
        ]

        # Две строки сумма покупки бензина АЗС и маржа при продаже бензина соответственно
        amount, amount_fee = 122, 88
        payment.request.request_orders[0].order.service_product.service_fee = None
        payment.request.request_orders[0].order_sum = amount
        main_order = payment.request.request_orders[0].order
        template = [{'service_product': service_product_666, 'amount': amount, 'order': main_order, },
                    {'service_product': service_product_667, 'amount': amount, 'order': main_order, }]
        if with_fee:
            payment.request.request_orders[1].order.service_product.service_fee = 1
            payment.request.request_orders[1].order_sum = amount_fee
            template.append({'service_product': service_product_666, 'amount': amount_fee,
                             'order': payment.request.request_orders[1].order})

        actual_rows = self.common_modify(entry, modifiers)
        TestRowCopier.check_zaxi(payment.session, actual_rows, template, copy_order_sum)

    def create_zaxi_payment_with_json_rows(self, with_fee):
        sid = ServiceId.ZAXI
        payment, contract = self.create_payment(sid=sid, rows_type='json')
        rows_count = 2 if with_fee else 1
        payment.payment_rows = payment.payment_rows[:rows_count]
        return payment, contract, sid

    @pytest.mark.parametrize('with_fee', [True, False])
    @pytest.mark.parametrize('copy_order_sum', [True, False])
    def test_transaction_row_zaxi_copier_json(self, with_fee, copy_order_sum):
        service_fee_copy = (None, 0, 1)
        payment, contract, sid = self.create_zaxi_payment_with_json_rows(with_fee)
        service_product_1 = self.create_service_product(contract, sid, service_fee=1)
        service_product_666 = self.create_service_product(contract, sid, 'YANDEX_SERVICE', 666)
        service_product_667 = self.create_service_product(contract, sid, 'YANDEX_SERVICE', 667)
        entry = dict(transaction=payment)
        modifiers = [
            {'modifier': {
                'name': 'transaction_row_zaxi_json_copier',
                'params': {'service_fee_copy': service_fee_copy, 'copy_order_sum': copy_order_sum}
            }}
        ]

        # Две строки сумма покупки бензина АЗС и маржа при продаже бензина соответственно
        amount, amount_fee = 122, 88
        payment.payment_rows[0]['amount'] = amount
        main_order = payment.payment_rows[0]['order']
        template = [{'service_product': service_product_666, 'amount': amount, 'order': main_order, },
                    {'service_product': service_product_667, 'amount': amount, 'order': main_order, }]
        if with_fee:
            payment.payment_rows[1]['amount'] = amount_fee
            payment.payment_rows[1]['order']['service_product_id'] = service_product_1.id
            template.append({'service_product': service_product_666, 'amount': amount_fee,
                             'order': payment.payment_rows[1]['order']})

        actual_rows = self.common_modify(entry, modifiers)
        TestRowCopier.check_zaxi(payment.session, actual_rows, template, copy_order_sum)

    @pytest.mark.parametrize('copy_order_sum', [True, False])
    def test_transaction_row_zaxi_copier_json_only_fee(self, copy_order_sum):
        service_fee_copy = (None, 0, 1)
        with_fee = True
        payment, contract, sid = self.create_zaxi_payment_with_json_rows(with_fee)
        # оставляем только fee
        payment.payment_rows = payment.payment_rows[1:]
        service_product_1 = self.create_service_product(contract, sid, service_fee=1)
        service_product_666 = self.create_service_product(contract, sid, 'YANDEX_SERVICE', 666)
        entry = dict(transaction=payment)
        modifiers = [
            {'modifier': {
                'name': 'transaction_row_zaxi_json_copier',
                'params': {'service_fee_copy': service_fee_copy, 'copy_order_sum': copy_order_sum,
                           'required_main': False}
            }}
        ]

        amount_fee = 88
        payment.payment_rows[0]['amount'] = amount_fee
        payment.payment_rows[0]['order']['service_product_id'] = service_product_1.id
        template = [{'service_product': service_product_666, 'amount': amount_fee,
                     'order': payment.payment_rows[0]['order']}]

        actual_rows = self.common_modify(entry, modifiers)
        TestRowCopier.check_zaxi(payment.session, actual_rows, template, copy_order_sum)


@pytest.mark.usefixtures('session')
class TestForcePartnerUnit(TestThirdpartyTransactions):

    @pytest.fixture()
    def tpt_service_data(self, session):
        currency_list = ['USD', 'RUB', 'EUR']
        force_partner_map = {}
        force_partner = PersonBuilder().build(session).get_obj().client
        expected_data = {}
        for currency in currency_list:
            person = PersonBuilder().build(session).get_obj()
            force_partner_map[str(person.client.id)] = {'currency_iso_code': currency}
            expected_data[currency] = person.client.id
        tpt_service = self.create_thirdparty_service(force_partner_id=force_partner.id,
                                                     force_partner_map=force_partner_map)
        return (tpt_service, expected_data)

    @pytest.mark.parametrize('currency_iso_code', ['USD', 'RUB', 'EUR', 'KZT'])
    def test_force_partner_choose(self, tpt_service_data, currency_iso_code, session):
        tpt_service, expected_data = tpt_service_data
        incoming_row = mock.MagicMock(service_id=tpt_service.id, thirdparty_service=tpt_service,
                                      currency_iso_code=currency_iso_code, session=session)
        result_row = MockObject()
        force_partner_unit = ForcePartner()
        result_row = force_partner_unit.process_row(incoming_row, result_row)

        if currency_iso_code in expected_data:
            assert result_row.partner.id == expected_data[currency_iso_code]
        else:
            assert result_row.partner.id == tpt_service.force_partner_id

    @pytest.mark.parametrize('force_partner_map, expected_data', [
        # client id 123 matches
        ({'123': {'currency_iso_code': 'USD', 'paysys_type_cc': 'yandex'},
          '321': {'currency_iso_code': 'KZT', 'paysys_type_cc': 'yandex'}}, 123),
        # both matches but 321 matches with more fields
        ({'123': {'currency_iso_code': 'USD'},
          '321': {'currency_iso_code': 'USD', 'paysys_type_cc': 'yandex'}}, 321),
        # both matches with same number of fields so we throw exception
        ({'123': {'currency_iso_code': 'USD'},
          '321': {'paysys_type_cc': 'yandex'}}, ThirdpartyError),
        # None of them matches
        ({'123': {'currency_iso_code': 'KZT', 'paysys_type_cc': 'yandex'},
          '321': {'currency_iso_code': 'USD', 'paysys_type_cc': 'card'}}, None),
    ])
    def test_filters(self, force_partner_map, expected_data, session):
        row_params = {'currency_iso_code': 'USD', 'paysys_type_cc': 'yandex'}
        tpt_service = self.create_thirdparty_service(force_partner_map=force_partner_map)
        incoming_row = mock.MagicMock(service_id=tpt_service.id, thirdparty_service=tpt_service, session=session, **row_params)

        if isinstance(expected_data, type):
            with pytest.raises(ThirdpartyError) as e:
                get_force_partner_id(incoming_row)
        else:
            client_id = get_force_partner_id(incoming_row)
            assert client_id == expected_data


@pytest.mark.usefixtures('session')
class TestProductMappingUnit(TestThirdpartyTransactions):

    @staticmethod
    def add_products(session):
        products = [
            ProductBuilder(id=14369211, mdh_id='mdh-1'),
            ProductBuilder(id=24369212, mdh_id='mdh-2'),
            ProductBuilder(id=34369213, mdh_id='mdh-2')
        ]
        for product in products:
            product._generate_id = False
            product.build(session)

    @pytest.mark.parametrize('product_mapping, expected_data', [
        ({'default': 123}, 123),
        ({'default': {'product_id': 123}}, 123),
        ({'default': {'product_mdh_id': 'mdh-1'}}, 14369211),
        (
            {'default': {'product_mdh_id': 'mdh-2'}},
            u'Multiple results found for product with MDH_ID mdh-2'
        ),
        (
            {'default': {'product_mdh_id': 'mdh-3'}},
            u'Product with MDH_ID mdh-2 not found'
        ),
        (
            {'default': {}},
            u'Wrong format of product mapping config'
        ),
    ])
    def test_product_mappings(self, product_mapping, expected_data, session):
        TestProductMappingUnit.add_products(session)
        incoming_row = mock.MagicMock(
            session=session,
        )
        product_mapping_unit = ProductMappingUnit()
        if isinstance(expected_data, basestring):
            with pytest.raises(ThirdpartyError) as exc_info:
                product_mapping_unit.try_to_get_product_id(
                    iso_currency=None,
                    mapping=product_mapping,
                    key_value=None,
                    incoming_row=incoming_row,
                    error_reason_name=None
                )
                assert exc_info.value.faultString == expected_data
        else:
            _, product_id = product_mapping_unit.try_to_get_product_id(
                iso_currency=None,
                mapping=product_mapping,
                key_value=None,
                incoming_row=incoming_row,
                error_reason_name=None
            )
            assert product_id == expected_data


@pytest.mark.usefixtures('session')
class TestSetNewBillingTransactionInternalUnit(TestThirdpartyTransactions):
    def create_migration(self, namespace, filter_, object_id, from_dt, dry_run):
        migration = MigrationNewBilling(namespace=namespace, filter=filter_, object_id=object_id,
                                        from_dt=from_dt, dry_run=dry_run)
        self.session.merge(migration)
        self.session.flush()
        return migration

    @pytest.mark.parametrize('filters, firm_id, expected_internal', [
        pytest.param(
            [['taxi', 'Client', None, datetime.datetime(2020, 1, 1), 1]],
            FirmId.TAXI,
            None,
            id='Filter by client_id with dry_run doesn\'t change internal'
        ),
        pytest.param(
            [['taxi', 'Client', None, datetime.datetime(2020, 1, 1), 0]],
            FirmId.TAXI,
            True,
            id='Filter by client_id without dry_run change internal'
        ),
        pytest.param(
            [['taxi', 'Firm', FirmId.TAXI, datetime.datetime(2020, 1, 1), 0]],
            FirmId.TAXI,
            True,
            id='Filter by firm without dry_run change internal'
        ),
        pytest.param(
            [['taxi', 'Firm', FirmId.TAXI, datetime.datetime(2020, 1, 1), 0]],
            FirmId.DRIVE,
            None,
            id='Filter by firm with other firm doesn\'t change internal'
        ),
        pytest.param(
            [['taxi', 'Namespace', 0, datetime.datetime(2020, 1, 1), 0]],
            FirmId.TAXI,
            True,
            id='Filter by namespace change internal'
        ),
        pytest.param(
            [['taxi', 'Namespace', 0, datetime.datetime(2050, 1, 1), 0]],
            FirmId.TAXI,
            None,
            id='Filter with future date doesn\'t change internal'
        ),
        pytest.param(
            [['taxi', 'Namespace', 0, datetime.datetime(2020, 1, 1), 0],
             ['taxi', 'Client', 0, datetime.datetime(2020, 1, 1), 1]
             ],
            FirmId.TAXI,
            None,
            id='Filter by client has gt priority'
        ),
        pytest.param(
            [['taxi', 'Firm', 0, datetime.datetime(2020, 1, 1), 1],
             ['taxi', 'Namespace', 0, datetime.datetime(2020, 1, 1), 1],
             ['taxi', 'Client', 0, datetime.datetime(2020, 1, 1), 0]
             ],
            FirmId.TAXI,
            True,
            id='Filter by client has gt priority'
        ),
    ])
    def test_filter_selection(self, filters, firm_id, expected_internal, session):
        contract = create_contract(self.session, firm=firm_id)
        for flt in filters:
            if flt[1] == 'Client':
                flt[2] = contract.client_id
            self.create_migration(*flt)
        incoming_row = MockObject(session=session, service_id=ServiceId.TAXI_PAYMENT, id=None, transaction=None)
        incoming_row.set_value_by_path('dt', datetime.datetime(2021, 1, 1))
        cs = contract.current_signed(contract.col0.is_signed, active=True)
        result_row = MockObject(paysys_type_cc='', cs=cs, internal=None)
        unit = SetNewBillingTransactionInternalUnit()
        result_row = unit.process_row(incoming_row, result_row)
        assert result_row.internal == expected_internal
