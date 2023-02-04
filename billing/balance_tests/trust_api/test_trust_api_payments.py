# -*- coding: utf-8 -*-

import xmlrpclib

import datetime
import mock
import pytest
import time

import balance.constants as cst
from balance import exc
from balance import mapper
from balance import muzzle_util as ut
from balance.actions.trust_payment import PayRequestProcessor
from balance.application import getApplication
from billing.contract_iface import ContractTypeId
from tests import tutils as tut
from tests.object_builder import (
    get_big_number,
    ClientBuilder,
    ProductBuilder,
    OrderBuilder,
    PersonBuilder,
    BasketBuilder,
    BasketItemBuilder,
    RequestBuilder,
    InvoiceBuilder,
    ContractBuilder,
    PassportBuilder,
    create_pay_policy_service,
    create_pay_policy_region,
    create_pay_policy_payment_method
)

CURRENT_DT = datetime.datetime.now()
CURRENT_TS = '%d' % (time.mktime(CURRENT_DT.timetuple()) * 1000)


def set_unique_external_id(contract):
    contract.external_id = contract.create_new_eid()


def not_existing_contract_id(session):
    return session.execute('select bo.s_contract_id.nextval from dual').scalar()


def not_existing_external_id(session):
    base = session.execute('select bo.s_contract_external_id.nextval from dual').scalar()
    year = datetime.date.today().year
    return u'%04d/%02d' % (base, year % 100)


def set_same_external_id(contracts):
    external_id = contracts[0].create_new_eid()
    for contract in contracts:
        contract.external_id = external_id


def create_contract(session, base_params=None, **params):
    contract_params = base_params if base_params else {}
    if params:
        contract_params.update(params)

    if 'client' not in contract_params:
        client = ClientBuilder().build(session).obj
        contract_params.update(client=client)

    if 'person' not in contract_params:
        person = PersonBuilder(client=contract_params['client']).build(session).obj
        contract_params.update(person=person)

    return ContractBuilder(**contract_params).build(session).obj


@pytest.fixture
def service_id(session):
    service_id = get_big_number()
    service_params = session.execute('select * from bo.t_service where id = 7').fetchone()
    new_params = dict(service_params)
    new_params['id'] = service_id
    new_params['cc'] = service_id

    service_insert_sql = 'insert into t_service (%s) values (%s)' % (
        ', '.join(k for k in new_params),
        ', '.join(':%s' % k for k in new_params)
    )
    session.execute(service_insert_sql, new_params)

    paysys_ccs = ['ur_trust_card_RUB', 'ph_trust_card_RUB']
    paysyses = session.query(mapper.Paysys) \
        .filter(
        mapper.Paysys.cc.in_(paysys_ccs),
        mapper.Paysys.firm_id == cst.FirmId.YANDEX_OOO
    )

    session.execute(
        'insert into t_paysys_service (service_id, paysys_id, weight, extern) values (:s_id, :ps_id, 666, 0)',
        [
            {'s_id': service_id, 'ps_id': ps.id}
            for ps in paysyses
        ]
    )
    ppp_id = create_pay_policy_service(session, service_id, cst.FirmId.YANDEX_OOO)
    create_pay_policy_payment_method(session, ppp_id, 'RUB', cst.PaymentMethodIDs.credit_card, paysyses[0].group_id)

    create_pay_policy_region(session, ppp_id, cst.RegionId.RUSSIA)

    return service_id


@pytest.fixture
def service(session, service_id):
    return session.query(mapper.Service).getone(service_id)


@pytest.fixture
def client(session):
    return ClientBuilder().build(session).obj


@pytest.fixture
def request_obj(session, service_id, client):
    product = ProductBuilder(price=10, engine_id=service_id)
    order = OrderBuilder(client=client, product=product, service_id=service_id)

    return RequestBuilder(
        basket=BasketBuilder(
            rows=[BasketItemBuilder(quantity=10, order=order)]
        )
    ).build(session).obj


@pytest.fixture
def person(session, client):
    return PersonBuilder(client=client, name='Name').build(session).obj


@pytest.fixture
def invoice(session, request_obj, person):
    paysys = session.query(mapper.Paysys).filter_by(cc='ph_trust_card_RUB', firm_id=1).one()
    return InvoiceBuilder(
        request=request_obj,
        person=person,
        paysys=paysys
    ).build(session).obj


@pytest.fixture
def mock_trust_paymethods(request):
    import balance.trust_api.actions as trust_actions

    pm_ids = request.param
    pms = []
    base_pm = {
        u'payment_method': u'card',
        u'currency': u'RUB',
        u'firm_id': 1,
        u'max_amount': 666
    }
    for pm_id in pm_ids:
        pm = base_pm.copy()
        if pm_id is not None:
            pm['id'] = pm_id
        pms.append(pm)

    old_func = trust_actions.get_payment_methods
    trust_actions.get_payment_methods = lambda *args, **kwargs: pms

    yield

    trust_actions.get_payment_methods = old_func


@pytest.fixture
def mock_api_calls(request, request_obj):
    payment_status = request.param
    calls = []

    def mock_call(self, *args, **kwargs):
        calls.append((args, kwargs))
        return payment_status.copy()

    def mock_get_payment(self, payment_id):
        invoice = sorted(request_obj.invoices, key=lambda i: i.id)[-1]
        return ut.Struct(transaction_id=payment_id, invoice=invoice, invoice_id=invoice.id)

    import balance.trust_api.balance_payments as api
    old_call = api.BalancePaymentsApi.do_call
    old_get_payment = api.BalancePaymentsApi._payment_from_id
    api.BalancePaymentsApi.do_call = mock_call
    api.BalancePaymentsApi._payment_from_id = mock_get_payment

    yield calls

    api.BalancePaymentsApi.do_call = old_call
    api.BalancePaymentsApi._payment_from_id = old_get_payment


@pytest.fixture()
def payments_api_mock():
    mock_path = 'balance.trust_api.balance_payments.BalancePaymentsApi'
    with mock.patch(mock_path) as m:
        m.return_value = 'mocked'
        yield m


class TestGetRequestPaymentMethods(object):

    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_w_trust(self, xmlrpcserver, request_obj, person):
        res = xmlrpcserver.GetRequestPaymentMethods({
            'OperatorUid': request_obj.passport_id,
            'RequestID': request_obj.id
        })

        required_res = [
            {
                'person_id': person.id,
                'person_name': person.name,
                'region_id': 225,
                'resident': 1,
                'legal_entity': 0,
                'currency': 'RUB',
                'payment_method_id': None,
                'payment_method_type': 'card',
                'payment_method_info': {'max_amount': '666'},
                'contract_id': None
            },
            {
                'person_id': person.id,
                'person_name': person.name,
                'region_id': 225,
                'resident': 1,
                'legal_entity': 0,
                'currency': 'RUB',
                'payment_method_id': 'card-666',
                'payment_method_type': 'card',
                'payment_method_info': {'max_amount': '666'},
                'contract_id': None
            }
        ]
        assert required_res == res

    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[]],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    @pytest.mark.usefixtures('person')
    def test_wo_trust(self, xmlrpcserver, request_obj):
        res = xmlrpcserver.GetRequestPaymentMethods({
            'OperatorUid': request_obj.passport_id,
            'RequestID': request_obj.id
        })

        assert [] == res

    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_w_trust_wo_person(self, xmlrpcserver, request_obj):
        res = xmlrpcserver.GetRequestPaymentMethods({
            'OperatorUid': request_obj.passport_id,
            'RequestID': request_obj.id
        })

        required_res = [
            {
                'person_id': None,
                'person_name': None,
                'region_id': 225,
                'resident': 1,
                'legal_entity': 0,
                'currency': 'RUB',
                'payment_method_id': None,
                'payment_method_type': 'card',
                'payment_method_info': {'max_amount': '666'},
                'contract_id': None
            },
            {
                'person_id': None,
                'person_name': None,
                'region_id': 225,
                'resident': 1,
                'legal_entity': 0,
                'currency': 'RUB',
                'payment_method_id': 'card-666',
                'payment_method_type': 'card',
                'payment_method_info': {'max_amount': '666'},
                'contract_id': None
            }
        ]
        assert required_res == res

    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[]],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_anon(self, xmlrpcserver, request_obj, person):
        res = xmlrpcserver.GetRequestPaymentMethods({
            'OperatorUid': 0,
            'RequestID': request_obj.id
        })

        required_res = [
            {
                'person_id': person.id,
                'person_name': person.name,
                'region_id': 225,
                'resident': 1,
                'legal_entity': 0,
                'currency': 'RUB',
                'payment_method_id': None,
                'payment_method_type': 'card',
                'payment_method_info': {},
                'contract_id': None
            },
        ]
        assert required_res == res


@pytest.mark.usefixtures('session')
class TestGetRequestPaymentMethodsContractCases(object):
    class IdType(object):
        ID = 'id'
        EID = 'external_id'

    ID_TYPE_TO_PARAM = {IdType.ID: 'ContractID',
                        IdType.EID: 'ContractExternalID'}

    ACTIVE_CONTRACT_PARAMS = [
        {},
        {'is_suspended': tut.shift_date(days=-3), 'is_deactivated': 1},
    ]

    NOT_ACTIVE_CONTRACT_PARAMS = [
        {'dt': tut.shift_date(days=3)},
        {'finish_dt': tut.shift_date(days=-3)},
        {'is_signed': None},
        {'is_cancelled': tut.shift_date(days=-3)},
        {'is_suspended': tut.shift_date(days=-3)},
    ]

    @pytest.mark.parametrize('id_type', [IdType.ID, IdType.EID])
    @pytest.mark.parametrize('contract_params', ACTIVE_CONTRACT_PARAMS, ids=lambda param: str(param.keys()))
    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_active_contract(self, session, xmlrpcserver, client, person, request_obj, id_type, contract_params):
        contract = self._create_contract(session, client=client, person=person, services={request_obj.service.id},
                                         **contract_params)
        set_unique_external_id(contract)
        params = {self.ID_TYPE_TO_PARAM[id_type]: getattr(contract, id_type)}
        resp = self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)
        self._check_response(resp, contract)

    @pytest.mark.parametrize('id_type', [IdType.ID, IdType.EID])
    @pytest.mark.parametrize('contract_params', NOT_ACTIVE_CONTRACT_PARAMS, ids=lambda param: str(param.keys()))
    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_not_active_contract(self, session, xmlrpcserver, client, person, request_obj, id_type, contract_params):
        contract = self._create_contract(session, client=client, person=person, services={request_obj.service.id},
                                         **contract_params)
        set_unique_external_id(contract)
        params = {self.ID_TYPE_TO_PARAM[id_type]: getattr(contract, id_type)}
        resp = self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)
        assert [] == resp

    @pytest.mark.parametrize('id_type, get_id_value',
                             [
                                 (IdType.ID, not_existing_contract_id),
                                 (IdType.EID, not_existing_external_id)
                             ],
                             ids=[IdType.ID, IdType.EID])
    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_not_found_contract(self, session, xmlrpcserver, client, person, request_obj, id_type, get_id_value):
        id_value = get_id_value(session)
        params = {self.ID_TYPE_TO_PARAM[id_type]: id_value}
        with pytest.raises(xmlrpclib.Fault) as exc_info:
            self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)
        expected_msg = "Contract {}={} not found".format(id_type, id_value)
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    @pytest.mark.parametrize('id_type', [IdType.ID, IdType.EID])
    @pytest.mark.parametrize('contract_params', NOT_ACTIVE_CONTRACT_PARAMS, ids=lambda param: str(param.keys()))
    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_not_unique_external_id_one_active(self, session, xmlrpcserver, client, person, request_obj, id_type,
                                               contract_params):
        contract1 = self._create_contract(session, client=client, person=person, services={request_obj.service.id})
        contract2 = self._create_contract(session, client=client, person=person, services={request_obj.service.id},
                                          **contract_params)
        set_same_external_id([contract1, contract2])

        params = {self.ID_TYPE_TO_PARAM[id_type]: getattr(contract1, id_type)}
        resp = self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)
        self._check_response(resp, contract1)

    @pytest.mark.parametrize('id_type, is_error_expected', [
        (IdType.ID, False),
        (IdType.EID, True)
    ])
    @pytest.mark.parametrize('contract_params', ACTIVE_CONTRACT_PARAMS, ids=lambda param: str(param.keys()))
    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_not_unique_external_id_two_active(self, session, xmlrpcserver, client, person, request_obj, id_type,
                                               is_error_expected,
                                               contract_params):
        contract1 = self._create_contract(session, client=client, person=person, services={request_obj.service.id})
        contract2 = self._create_contract(session, client=client, person=person, services={request_obj.service.id},
                                          **contract_params)
        set_same_external_id([contract1, contract2])

        params = {self.ID_TYPE_TO_PARAM[id_type]: getattr(contract1, id_type)}
        if is_error_expected:
            with pytest.raises(xmlrpclib.Fault) as exc_info:
                self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)

            expected_msg = "Object is not unique: Contract: filter: {{'external_id': u'{}'}}".format(
                contract1.external_id)
            assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')
        else:
            resp = self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)
            self._check_response(resp, contract1)

    @pytest.mark.parametrize('id_type', [IdType.ID, IdType.EID])
    @pytest.mark.parametrize(
        'mock_trust_paymethods',
        [[None, 'card-666']],
        indirect=True,
        ids=['^_^']
    )
    @pytest.mark.usefixtures('mock_trust_paymethods')
    def test_not_unique_external_id_two_not_active(self, session, xmlrpcserver, client, person, request_obj, id_type):
        contract1 = self._create_contract(session, client=client, person=person, services={request_obj.service.id},
                                          is_signed=None)
        contract2 = self._create_contract(session, client=client, person=person, services={request_obj.service.id},
                                          is_signed=None)
        set_same_external_id([contract1, contract2])

        params = {self.ID_TYPE_TO_PARAM[id_type]: getattr(contract1, id_type)}
        if id_type == self.IdType.ID:
            resp = self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)
            assert [] == resp
        else:
            with pytest.raises(xmlrpclib.Fault) as exc_info:
                self._call_GetRequestPaymentMethods(xmlrpcserver, request_obj.id, **params)

            expected_msg = "Active contract with external_id={} not found".format(contract1.external_id)
            assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    # ContractCases Utils

    def _create_contract(self, session, **params):
        base_params = dict(
            commission=ContractTypeId.NON_AGENCY,
            firm=cst.FirmId.YANDEX_OOO,
            payment_type=cst.PREPAY_PAYMENT_TYPE,
            services={cst.DIRECT_SERVICE_ID},
            is_signed=datetime.datetime.now(),
        )
        return create_contract(session, base_params, **params)

    def _call_GetRequestPaymentMethods(self, xmlrpcserver, RequestID, OperatorUid=None, ContractID=None,
                                       ContractExternalID=None):
        if OperatorUid is None:
            OperatorUid = PassportBuilder.construct(self.session).passport_id

        params = ut.clear_dict_with_none_values({
            'OperatorUid': OperatorUid,
            'RequestID': RequestID,
            'ContractID': ContractID,
            'ContractExternalID': ContractExternalID,
        })
        return xmlrpcserver.GetRequestPaymentMethods(params)

    def _check_response(self, actual_response, contract):
        expected_response = [
            {
                'person_id': contract.person.id,
                'person_name': contract.person.name,
                'region_id': 225,
                'resident': 1,
                'legal_entity': 0,
                'currency': 'RUB',
                'payment_method_id': None,
                'payment_method_type': 'card',
                'payment_method_info': {'max_amount': '666'},
                'contract_id': contract.id
            },
            {
                'person_id': contract.person.id,
                'person_name': contract.person.name,
                'region_id': 225,
                'resident': 1,
                'legal_entity': 0,
                'currency': 'RUB',
                'payment_method_id': 'card-666',
                'payment_method_type': 'card',
                'payment_method_info': {'max_amount': '666'},
                'contract_id': contract.id
            }
        ]
        assert expected_response == actual_response


class TestPayRequest(object):

    def get_real_request_id(self, real_session, test_name):
        with real_session.begin():
            query = '''
            select object_id from bo.t_test_committed_objects
            where test_name = :test_name and object_name = 'request'
            '''
            res = real_session.execute(query, {'test_name': test_name}).fetchone()
            if res:
                request_id = res.object_id
            else:
                request = RequestBuilder().build(real_session).obj
                real_session.add(request)
                query = '''
                insert into bo.t_test_committed_objects (test_name, object_name, object_id)
                VALUES (:test_name, 'request', :request_id)
                '''
                real_session.execute(query, {'test_name': test_name, 'request_id': request.id})
                request_id = request.id

        return request_id

    def test_pay_request_locks_create_payment(self, session, xmlrpcserver, payments_api_mock):
        session.oracle_namespace_lock('test_pay_request_locks_create_payment', lockmode='exclusive', timeout=10)

        class TestPayRequestProcessor(PayRequestProcessor):
            def init_invoice_for_payment(self):
                request = self.session.query(mapper.Request).getone(self.request_id)
                return request, None

            def create_payment(self, request, invoice, payments_api):
                real_session.query(mapper.Request).with_lockmode('update_nowait').getone(request_id)
                super(TestPayRequestProcessor, self).create_payment(request, invoice, payments_api)

        real_session = getApplication().real_new_session()
        request_id = self.get_real_request_id(real_session, 'test_pay_request_locks_create_payment')

        with real_session.begin():
            with pytest.raises(exc.REQUEST_IS_LOCKED) as exc_info:
                processor = TestPayRequestProcessor(
                    session,
                    request_id=request_id,
                    iso_currency='RUB',
                    payment_method_id='card-666'
                )
                processor.do()
            msg = 'Request {} is locked by another operation'.format(request_id)
            assert exc_info.value.msg == msg

    def test_pay_request_locks_start_payment(self, session, xmlrpcserver, payments_api_mock):
        session.oracle_namespace_lock('test_pay_request_locks_start_payment', lockmode='exclusive', timeout=10)

        class TestPayRequestProcessor(PayRequestProcessor):
            def init_invoice_for_payment(self):
                request = self.session.query(mapper.Request).getone(self.request_id)
                return request, None

            def create_payment(self, request, invoice, payments_api):
                return

            def start_payment(self, request, payments_api):
                real_session.query(mapper.Request).with_lockmode('update_nowait').getone(request_id)
                super(TestPayRequestProcessor, self).start_payment(request, payments_api)

        real_session = getApplication().real_new_session()
        request_id = self.get_real_request_id(real_session, 'test_pay_request_locks_start_payment')

        with real_session.begin():
            with pytest.raises(exc.REQUEST_IS_LOCKED) as exc_info:
                processor = TestPayRequestProcessor(
                    session,
                    request_id=request_id,
                    iso_currency='RUB',
                    payment_method_id='card-666'
                )
                processor.do()
            msg = 'Request {} is locked by another operation'.format(request_id)
            assert exc_info.value.msg == msg

    def test_pay_request_product_locked(self, session, xmlrpcserver, payments_api_mock):
        session.oracle_namespace_lock('test_pay_request_product_locked', lockmode='exclusive', timeout=10)

        class TestPayRequestProcessor(PayRequestProcessor):
            def init_invoice_for_payment(self):
                real_session.query(mapper.Product).with_lockmode('update_nowait').getone(product_id)
                return super(TestPayRequestProcessor, self).init_invoice_for_payment()

            def _create_invoice(self, request):
                return None

            def create_payment(self, request, invoice, payments_api):
                return

            def start_payment(self, request, payments_api):
                return

        real_session = getApplication().real_new_session()
        with real_session.begin():
            query = '''
            select object_id from bo.t_test_committed_objects
            where test_name = 'test_pay_request_product_locked' and object_name = 'request'
            '''
            real_req = real_session.execute(query).fetchone()
            query = '''
            select object_id from bo.t_test_committed_objects
            where test_name = 'test_pay_request_product_locked' and object_name = 'product'
            '''
            real_prod = real_session.execute(query).fetchone()
            if real_req and real_prod:
                request_id = real_req.object_id
                product_id = real_prod.object_id
            else:
                request = RequestBuilder().build(real_session).obj
                real_session.add(request)
                query = '''
                insert into bo.t_test_committed_objects (test_name, object_name, object_id)
                VALUES ('test_pay_request_product_locked', 'request', :request_id)
                '''
                real_session.execute(query, {'request_id': request.id})
                request_id = request.id

                product = request.request_orders[0].product
                real_session.add(product)
                query = '''
                insert into bo.t_test_committed_objects (test_name, object_name, object_id)
                VALUES ('test_pay_request_product_locked', 'product', :product_id)
                '''
                real_session.execute(query, {'product_id': product.id})
                product_id = product.id

        with real_session.begin():
            processor = TestPayRequestProcessor(
                session,
                request_id=request_id,
                iso_currency='RUB',
                payment_method_id='card-666'
            )
            processor.do()


class TestCheckRequestPayment(object):
    PAYMENT_INFO_FINISHED = {
        'status': 'success',
        'cancel_ts': CURRENT_TS,
        'payment_ts': CURRENT_TS,
        'postauth_ts': CURRENT_TS,
        'resp_code': 'code_resp',
        'resp_desc': 'desc_resp',
        'transaction_id': '666-transaction-666',
        'ts': CURRENT_TS,
        'amount': '100',
        'currency': 'RUR',
        'developer_payload': 'payload'
    }

    @pytest.mark.parametrize(
        'mock_api_calls',
        [PAYMENT_INFO_FINISHED],
        indirect=True,
        ids=['^_^']
    )
    def test_by_payment(self, session, service, xmlrpcserver, invoice, mock_api_calls):
        call_res = xmlrpcserver.CheckRequestPayment(
            session.oper_id,
            {
                'service_id': service.id,
                'transaction_id': '666-transaction-666'
            }
        )

        required_ans = {
            'invoice_id': invoice.id,
            'request_id': str(invoice.request_id),
            'cancel_dt': CURRENT_DT,
            'payment_dt': CURRENT_DT,
            'postauth_dt': CURRENT_DT,
            'resp_code': 'code_resp',
            'resp_desc': 'desc_resp',
            'transaction_id': '666-transaction-666',
            'dt': CURRENT_DT,
            'amount': '100',
            'currency': 'RUR',
            'developer_payload': 'payload',
        }
        assert required_ans == call_res

        required_calls = [
            (
                ('CheckTrustAPIPayment',),
                {'transaction_id': '666-transaction-666'}
            ),
        ]
        assert required_calls == mock_api_calls

    @pytest.mark.parametrize(
        'mock_api_calls',
        [PAYMENT_INFO_FINISHED],
        indirect=True,
        ids=['^_^']
    )
    def test_no_payment(self, session, service, xmlrpcserver, request_obj, mock_api_calls):
        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.CheckRequestPayment(
                session.oper_id,
                {
                    'service_id': service.id,
                    'request_id': request_obj.id
                }
            )
        expected_msg = 'There are no payments for request {}'.format(request_obj.id)
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

        required_calls = []
        assert required_calls == mock_api_calls
