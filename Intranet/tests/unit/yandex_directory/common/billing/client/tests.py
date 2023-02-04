# -*- coding: utf-8 -*-
import datetime
from decimal import Decimal
from unittest.mock import patch, Mock, ANY
from xmlrpc.client import (
    Fault,
    ProtocolError
)
from socket import error

import pytest

from intranet.yandex_directory.src.yandex_directory.common.billing.client import (
    BillingClient,
    BANK_IDS,
    WORKSPACE_SERVICE_ID,
    TVM2Transport,
)
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    BillingInvalidField,
    BillingMissingField,
    BillingUnknownError,
    BillingUidNotFoundInPassport,
)

from testutils import TestCase

from hamcrest import (
    assert_that,
    equal_to,
)


class TestBillingClient(TestCase):
    def setUp(self, *args, **kwargs):
        self.endpoint = 'endpoint'
        self.token = 'token'
        self.client = self._get_billing_client(self.endpoint, self.token)
        self.operator_uid = 1180000111222333
        super(TestBillingClient, self).setUp(*args, **kwargs)

    def _get_billing_client(self, endpoint, token):
        mocked_xmlrpc = Mock()
        with patch('intranet.yandex_directory.src.yandex_directory.common.billing.client.xmlrpc.client', mocked_xmlrpc):
            client = BillingClient(endpoint=endpoint, token=token, manager_uid=1)
        mocked_xmlrpc.ServerProxy.assert_called_once_with(endpoint, allow_none=True, transport=ANY)
        return client

    def test_create_client(self):
        # проверим, что create_client вызовет xmlrcp метод с правильно указанными параметрами
        name = 'Тестовая организация'
        email = 'test_org_for_billing@yaconnect.com'
        phone = '7495111'
        currency = 'RUR'
        self.client.create_client(
            uid=self.operator_uid,
            name=name,
            email=email,
            phone=phone,
        )
        exp_params = {
            'name': name,
            'email': email,
            'phone': phone,
            'currency': currency,
        }
        getattr(self.client.server.Balance, 'CreateClient').assert_called_once_with(str(self.operator_uid), exp_params)

    def test_make_request_should_call_xmlrpc_method(self):
        # _make_request должен вызывать xml rpc метод с нужными параметрами
        method_name = 'some_method'
        params = {
            'param_1': 'value_1',
            'param_2': 'value_2',
        }

        self.client._make_request(
            method_name=method_name,
            params=params,
        )

        # проверим, что метод был вызван с правильными параметрами
        getattr(self.client.server.Balance, method_name).assert_called_once_with(params)

    def test_make_request_with_uid_should_call_xmlrpc_method(self):
        # _make_request должен вызывать xml rpc метод с нужными параметрами
        # если там передан uid, то он приведет его к строковому виду для Биллинга
        method_name = 'some_method'
        params = {
            'param_1': 'value_1',
            'param_2': 'value_2',
        }
        uid = 1180000123123123

        self.client._make_request(
            method_name=method_name,
            params=params,
            uid=uid,
        )

        # проверим, что метод был вызван с правильными параметрами
        getattr(self.client.server.Balance, method_name).assert_called_once_with(
            str(uid),
            params,
        )

    def test_get_client_contracts(self):
        # проверим, что get_client_contracts получает договоры пользователя
        client_id = 1

        some_contracts = [
            {
                'ID': 567,
                'SERVICES': [77777],
            },
            {
                'ID': 12345,
                'SERVICES': [WORKSPACE_SERVICE_ID],
            }
        ]

        self.client.server.Balance.GetClientContracts.return_value = some_contracts
        service_contract = self.client.get_client_contracts(
            client_id=client_id,
        )

        exp_params = {
            'ClientID': client_id,
            'Signed': 0
        }
        getattr(self.client.server.Balance, 'GetClientContracts').assert_called_once_with(exp_params)
        assert_that(
            service_contract,
            equal_to(
                [some_contracts[1]]
            )
        )

    def test_create_natural_person(self):
        # проверим, что create_natural_person создаёт плательщика физ лицо
        client_id = 1
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'M'
        phone = '+7916000'
        email = 'akhmetov@yandex-team.ru'

        self.client.create_natural_person(
            uid=self.operator_uid,
            client_id=client_id,
            first_name=first_name,
            last_name=last_name,
            middle_name=middle_name,
            phone=phone,
            email=email,
        )

        exp_params = {
            'type': 'ph',
            'client_id': client_id,
            'fname': first_name,
            'lname': last_name,
            'mname': middle_name,
            'phone': phone,
            'email': email,
        }
        getattr(self.client.server.Balance, 'CreatePerson').assert_called_once_with(str(self.operator_uid), exp_params)

    def test_create_legal_person(self):
        # проверим, что create_legal_person создаёт плательщика-юр лицо
        client_id = 1
        name = 'test name'
        long_name = 'test long name'
        phone = '+7916000'
        email = 'akhmetov@yandex-team.ru'
        postal_code = '123456'
        postal_address = 'test post address'
        legal_address = 'test legal address'
        inn = 123456789
        kpp = 987654321
        bik = 1000111222
        account = 555

        self.client.create_legal_person(
            uid=self.operator_uid,
            client_id=client_id,
            name=name,
            long_name=long_name,
            phone=phone,
            email=email,
            postal_code=postal_code,
            postal_address=postal_address,
            legal_address=legal_address,
            inn=inn,
            kpp=kpp,
            bik=bik,
            account=account,
        )

        exp_params = {
            'type': 'ur',
            'client_id': client_id,
            'name': name,
            'longname': long_name,
            'phone': phone,
            'email': email,
            'postcode': postal_code,
            'postaddress': postal_address,
            'legaladdress': legal_address,
            'inn': inn,
            'kpp': kpp,
            'bik': bik,
            'account': account,
        }
        getattr(self.client.server.Balance, 'CreatePerson').assert_called_once_with(str(self.operator_uid), exp_params)

    def test_create_contract_should_create_contract_in_billing(self):
        client_id = 100
        person_id = 15

        self.client.create_contract(
            uid=self.operator_uid,
            client_id=client_id,
            person_id=person_id,
        )

        exp_params = {
            'bank_details_id': BANK_IDS['resident'],
            'client_id': client_id,
            'person_id': person_id,
            'firm_id': 1,
            'manager_uid': self.client.manager_uid,
            'payment_type': 3,
            'currency': 'RUR',
            'payment_term': 30,
            'services': [202],
            'start_dt': None,
        }
        getattr(self.client.server.Balance, 'CreateCommonContract').assert_called_once_with(str(self.operator_uid), exp_params)

    def test_create_offer_should_create_offer_in_billing(self):
        # проверим что create_offer вызовет метод Billing.CreateOffer с правильными параметрами
        client_id = 100
        person_id = 15

        self.client.create_offer(
            uid=self.operator_uid,
            client_id=client_id,
            person_id=person_id,
        )

        exp_params = {
            'bank_details_id': BANK_IDS['resident'],
            'client_id': client_id,
            'person_id': person_id,
            'firm_id': 1,  # ООО Яндекс
            'manager_uid': self.client.manager_uid,
            'payment_type': 3,
            'currency': 'RUR',
            'payment_term': 30,
            'services': [202],
            'start_dt': None,
        }
        getattr(self.client.server.Balance, 'CreateOffer').assert_called_once_with(str(self.operator_uid), exp_params)

    def test_create_client_user_association(self):
        # проверим что create_client_user вызовет метод Billing.CreateUserClientAssociation с правильными параметрами
        client_id = 100
        user_uid = 55

        self.client.create_client_user_association(
            uid=self.operator_uid,
            client_id=client_id,
            user_uid=user_uid,
        )

        exp_args = (
            str(self.operator_uid),
            client_id,
            str(user_uid),
        )
        getattr(self.client.server.Balance, 'CreateUserClientAssociation').assert_called_once_with(*exp_args)

    def test_create_or_update_order(self):
        client_id = 100
        product_id = 11

        orders = [
            {
                'ServiceOrderID': client_id,
                'ServiceID':  WORKSPACE_SERVICE_ID,
                'ClientID': client_id,
                'ProductID': product_id,
            }
        ]

        self.client.create_or_update_order(
            uid=self.operator_uid,
            client_id=client_id,
            product_id=product_id,
        )

        exp_args = (
            str(self.operator_uid),
            orders,
        )
        getattr(self.client.server.Balance, 'CreateOrUpdateOrdersBatch').assert_called_once_with(*exp_args)

    def test_create_invoice_request(self):
        client_id = 100
        quantity = 5

        self.client.create_invoice_request(
            uid=self.operator_uid,
            client_id=client_id,
            quantity=quantity,
        )

        orders = [
            {
                'ServiceOrderID': client_id,
                'ServiceID': WORKSPACE_SERVICE_ID,
                'ClientID': client_id,
                'Qty': quantity,
            }
        ]
        exp_params = {
            'InvoiceDesireType': 'charge_note',
            'ReturnPath': None,
        }

        exp_args = (
            str(self.operator_uid),
            client_id,
            orders,
            exp_params,
        )
        getattr(self.client.server.Balance, 'CreateRequest2').assert_called_once_with(*exp_args)

    def test_create_invoice_request_with_return_path(self):
        client_id = 100
        quantity = 5
        return_path = 'some_return_path'

        self.client.create_invoice_request(
            uid=self.operator_uid,
            client_id=client_id,
            quantity=quantity,
            return_path=return_path,
        )

        orders = [
            {
                'ServiceOrderID': client_id,
                'ServiceID': 202,
                'ClientID': client_id,
                'Qty': quantity,
            }
        ]
        exp_params = {
            'InvoiceDesireType': 'charge_note',
            'ReturnPath': return_path,
        }

        exp_args = (
            str(self.operator_uid),
            client_id,
            orders,
            exp_params,
        )
        getattr(self.client.server.Balance, 'CreateRequest2').assert_called_once_with(*exp_args)

    def test_find_client(self):
        client_id = 100
        user_uid = 10

        self.client.find_client(
            client_id=client_id,
            user_uid=user_uid,
        )

        exp_params = {
            'ClientID': client_id,
            'PassportID': user_uid,
        }
        getattr(self.client.server.Balance, 'FindClient').assert_called_once_with(exp_params)

    def test_get_passport_by_uid(self):
        user_uid = 10

        self.client.get_passport_by_uid(
            uid=self.operator_uid,
            user_uid=user_uid,
        )

        exp_args = (
            str(self.operator_uid),
            str(user_uid),
            {'RepresentedClientIds': True},
        )
        getattr(self.client.server.Balance, 'GetPassportByUid').assert_called_once_with(*exp_args)

    def test_make_flat_request(self):
        exp_args = ('1', '2', '3')
        self.client._make_flat_request('TestMethod', *exp_args)
        getattr(self.client.server.Balance, 'TestMethod').assert_called_once_with(*exp_args)

    def test_get_balances_info(self):
        # проверяем, что правильно посчитаем баланс для договоров,
        # как разницу между ReceiptSum и ActSum и вернем самую первую дату задолженности

        first_contract_id = 1
        contracts_data = [
            {
                'FirstDebtFromDT': None,
                'ContractID': first_contract_id,
                'ReceiptSum': '500.5',
                'ActSum': '10000.1',
            },
        ]

        self.client.server.Balance.GetPartnerBalance.return_value = contracts_data
        result = self.client._get_balances_info([first_contract_id])

        exp_result = {
            'balance': Decimal('-9499.6'),
            'first_debt_act_date': None,
            'receipt_sum': Decimal('500.5'),
            'act_sum': Decimal('10000.1'),
        }

        assert_that(
            result,
            equal_to(exp_result)
        )

    def test_get_balance_info(self):
        # проверяем, что правильно вернем баланс для одного договора

        first_contract_id = 1
        contracts_data = [
            {
                'FirstDebtFromDT': None,
                'ContractID': first_contract_id,
                'ReceiptSum': '10000.1',
                'ActSum': '10000',
            }
        ]

        self.client.server.Balance.GetPartnerBalance.return_value = contracts_data
        self.client.server.Balance.GetClientContracts.return_value = [{
            'ID': first_contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        result = self.client.get_balance_info(client_id=1)

        exp_result = {
            'balance': Decimal('0.1'),
            'first_debt_act_date': None,
            'receipt_sum': Decimal('10000.1'),
            'act_sum': Decimal('10000'),
        }

        assert_that(
            result,
            equal_to(exp_result)
        )

    def test_get_balance_info_no_active_contract(self):
        # проверяем, что правильно вернем нулевой баланс, если нет активнных договоров

        first_contract_id = 1
        second_contract_id = 2

        self.client.server.Balance.GetPartnerBalance.return_value = []
        self.client.server.Balance.GetClientContracts.return_value = [
            {
                'ID': first_contract_id,
                'SERVICES': [WORKSPACE_SERVICE_ID],
                'IS_ACTIVE': 0,
            },
            {
                'ID': second_contract_id,
                'SERVICES': [WORKSPACE_SERVICE_ID],
                'IS_ACTIVE': 0,
            }
        ]
        result = self.client.get_balance_info(client_id=1)

        exp_result = {
            'balance': 0,
            'first_debt_act_date': None,
            'receipt_sum': 0,
            'act_sum': 0,
        }

        assert_that(
            result,
            equal_to(exp_result)
        )

    def test_get_client_acts(self):
        client_id = 100

        self.client.get_client_acts(
            client_id=client_id,
        )

        exp_args = (
            self.token,
            {'ClientID': client_id},
        )
        getattr(self.client.server.Balance, 'GetClientActs').assert_called_once_with(*exp_args)

    def test_get_contract_print_form(self):
        client_id = 100
        contract_id = 222

        self.client.server.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': 1,
        }]
        self.client.get_contract_print_form(client_id)

        getattr(self.client.server.Balance, 'GetContractPrintForm').assert_called_once_with(contract_id)


class TestBillingClient__make_request_retry(TestBillingClient):
    def test_retry_on_protocol_error(self):
        # ретраим запросы к Биллингу при сетевой ошибке
        self.client.server.Balance.GetClientContracts.side_effect = \
            ProtocolError('xmlrpc.balance.yandex.ru', 502, 'Bad Gateway', 'headers')
        try:
            self.client._make_flat_request('GetClientContracts', 11)
        except ProtocolError:
            pass
        self.assertEqual(self.client.server.Balance.GetClientContracts.call_count, 3)

    def test_retry_on_socket_error(self):
        # ретраим запросы к Биллингу при ошибках сокета
        self.client.server.Balance.GetClientContracts.side_effect = error()
        try:
            self.client._make_flat_request('GetClientContracts', 11)
        except error:
            pass
        self.assertEqual(self.client.server.Balance.GetClientContracts.call_count, 3)

    def test_billing_errors_handler_email(self):
        # проверим, что вызывается исключение, если поле email некорректно
        client_id = 1
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'M'
        phone = '+7916000'
        email = 'akhmetov'

        fault_msg = """
        <error><msg>Email address "akhmetov" is invalid</msg><email>akhmetov</email>
        <wo-rollback>0</wo-rollback><method>Balance.CreatePerson</method><code>WRONG_EMAIL</code></error>
        """
        self.client.server.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with pytest.raises(BillingInvalidField) as err:
            self.client.create_natural_person(
                uid=self.operator_uid,
                client_id=client_id,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        self.assertEqual(err.value.code, 'invalid_email')

    def test_billing_errors_handler_inn(self):
        # проверим, что вызывается исключение, если поле inn некорректно
        client_id = 1
        name = 'test name'
        long_name = 'test long name'
        phone = '+7916000'
        email = 'akhmetov@yandex-team.ru'
        postal_code = '123456'
        postal_address = 'test post address'
        legal_address = 'test legal address'
        inn = '123'
        kpp = 987654321
        bik = 1000111222
        account = 555

        fault_msg = """
        <error><msg>Invalid INN for ur or ua person</msg><wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>INVALID_INN</code></error>
        """
        self.client.server.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with pytest.raises(BillingInvalidField) as err:
            self.client.create_legal_person(
                uid=self.operator_uid,
                client_id=client_id,
                name=name,
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
            )
        self.assertEqual(err.value.code, 'invalid_inn')

    def test_billing_errors_handler_missing_field(self):
        # проверим, что вызывается исключение, если отсутсвует любое обязательное поле
        client_id = 1
        name = 'test name'
        long_name = ''
        phone = '+7916000'
        email = 'akhmetov@yandex-team.ru'
        postal_code = '123456'
        postal_address = 'test post address'
        legal_address = 'test legal address'
        inn = 'aaaa'
        kpp = 987654321
        bik = 1000111222
        account = 555

        fault_msg = """
        <error><msg>Missing mandatory person field 'longname' for person type ur</msg>
        <field>longname</field><wo-rollback>0</wo-rollback><person-type>ur</person-type>
        <method>Balance.CreatePerson</method><code>MISSING_MANDATORY_PERSON_FIELD</code></error>
        """
        self.client.server.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with pytest.raises(BillingMissingField) as err:
            self.client.create_legal_person(
                uid=self.operator_uid,
                client_id=client_id,
                name=name,
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
            )
        self.assertEqual(err.value.params['field'], 'long_name')

    def test_billing_errors_handler_uid_not_found(self):
        # проверим, что вызывается исключение, если биллинг не нашел uid пользователя
        name = 'test name'
        phone = '+7916000'
        email = 'akhmetov@yandex-team.ru'

        fault_msg = """
        <error><msg>Passport with ID 1130000000621392 not found in DB</msg>
        <wo-rollback>0</wo-rollback><code>2</code><object-id>1130000000621392</object-id><object >
        </object><method>Balance.CreateClient</method><code>PASSPORT_NOT_FOUND</code><parent-codes>
        <code>NOT_FOUND</code><code>EXCEPTION</code></parent-codes><contents>Passport with ID 1130000000621392
        not found in DB</contents></error>
        """
        self.client.server.Balance.CreateClient.side_effect = Fault(-1, fault_msg)

        with pytest.raises(BillingUidNotFoundInPassport) as err:
            self.client.create_legal_person_client(
                uid=self.operator_uid,
                name=name,
                phone=phone,
                email=email,
            )
        self.assertEqual(err.value.params['uid'], '1130000000621392')

    def test_billing_errors_handler_account_mismatch_bik(self):
        # проверим, что вызывается исключение, если аккаунт не соответствует бику банка
        client_id = 1
        name = 'test name'
        long_name = ''
        phone = '+7916000'
        email = 'akhmetov@yandex-team.ru'
        postal_code = '123456'
        postal_address = 'test post address'
        legal_address = 'test legal address'
        inn = 'aaaa'
        kpp = 987654321
        bik = 1000111222
        account = 555

        fault_msg = """
        <error><msg>Account 30101810400000000225 doesn't match bank with BIK=044525225</msg>
        <account>30101810400000000225</account><bik>044525225</bik><wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>WRONG_ACCOUNT</code><parent-codes>
        <code>INVALID_PARAM</code><code>EXCEPTION</code></parent-codes>
        <contents>Account 30101810400000000225 doesn't match bank with BIK=044525225</contents></error>
        """
        self.client.server.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with pytest.raises(BillingInvalidField) as err:
            self.client.create_legal_person(
                uid=self.operator_uid,
                client_id=client_id,
                name=name,
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
            )
        self.assertEqual(err.value.code, 'invalid_account')

    def test_billing_errors_handler_unknown_error(self):
        # проверим, что вызывается исключение, если биллинг вернул ошибку
        client_id = 1
        name = 'test name'
        long_name = ''
        phone = '+7916000'
        email = 'akhmetov@yandex-team.ru'
        postal_code = '123456'
        postal_address = 'test post address'
        legal_address = 'test legal address'
        inn = 'aaaa'
        kpp = 987654321
        bik = 1000111222
        account = 555

        fault_msg = """
        <error><msg>Unknown billing error</msg>
        <wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>UNKNOWN</code><parent-codes><code>INVALID_PARAM</code>
        <code>EXCEPTION</code></parent-codes></error>
        """
        self.client.server.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with pytest.raises(BillingUnknownError) as err:
            self.client.create_legal_person(
                uid=self.operator_uid,
                client_id=client_id,
                name=name,
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
            )
