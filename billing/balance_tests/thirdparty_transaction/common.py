# coding: utf-8
from decimal import Decimal as D
import datetime
import bson
import json
import pytest
import random
import time

import sqlalchemy as sa

from balance import mapper, muzzle_util as ut, thirdparty_transaction as tt
from balance.utils.sql_alchemy import get_polymorphic_subclass
from balance.constants import PaymentMethodIDs, ServiceId, FirmId
from balance.mapper import TrustPayment, Refund, PaymentRegister, PaymentRegisterLine, ServiceProduct
from tests.object_builder import (
    ContractBuilder, BasketBuilder, BasketItemBuilder, OrderBuilder, RequestBuilder,
    PersonBuilder, ClientBuilder, ProductBuilder
)
import uuid


TEST_CARD_PAYMENT_METHOD = 'card-38379773'

TEST_YM_SHOP_ID = 6666
CURRENCY_CHAR_CODE = 'RUR'
PAYSYS_PARTNER_ID = 29198812

PAYMENT_DT = datetime.datetime.now().replace(microsecond=0) - datetime.timedelta(days=1)
DT = datetime.datetime.now().replace(microsecond=0) - datetime.timedelta(hours=23)
REFUND_DT = DT + datetime.timedelta(seconds=1)

AMOUNT = D('100')
SMALL_AMOUNT = D('1')

TEST_SERVICE_ID = 1
TAXI_SERVICE_ORDER_STR = ut.md5(str(random.random()))[0:24]
TERMINAL_ID = 300112

BS_SOURCE_SCHEME = 'bs'

PAYSYS_COMMISSION = D('10')

TAXI_REWARD_PCT = D('0.01')


@pytest.mark.usefixtures('session')
class TestThirdpartyTransactions(object):
    sid = TEST_SERVICE_ID
    pipeline = tt.transaction_pipeline

    def assertEqual(self, first, second):
        assert first == second

    def assertRaises(self, exc_type, func):
        with pytest.raises(exc_type):
            func()

    def assertTrue(self, value):
        assert value

    def create_payment_request_rows(self, request, payment, amount, service_id, contract, start_dt,
                                    service_fee=None, row_developer_payload=None):
        payment.request = request
        for ro in payment.request.request_orders:
            service_product = ServiceProduct(service_id=service_id,
                                             partner_id=contract.client_id,
                                             product_id=ro.order.product.id,
                                             name=ro.product.name,
                                             external_id=ro.id)
            self.session.add(service_product)

            if service_fee:
                service_product.service_fee = service_fee
            if row_developer_payload:
                ro.order.developer_payload = row_developer_payload
            ro.order.service_product = service_product
            ro.order_sum = amount
            if not start_dt:
                start_dt = datetime.datetime.now()
            ro.order.start_dt_utc = start_dt
            ro.order.start_dt_offset = 0

            if service_id in (ServiceId.TAXI_PAYMENT, ServiceId.TAXI_CORP):
                ro.order.service_order_id_str = TAXI_SERVICE_ORDER_STR

    def create_refund_request_rows(self, request, refund, orig_payment, amount):
        refund.request = request

        for i in range(len(refund.request.request_orders)):
            service_product = ServiceProduct(service_id=refund.service.id,
                                             partner_id=orig_payment.request.request_orders[
                                                 0].order.service_product.partner_id,
                                             product_id=refund.request.request_orders[i].order.product.id,
                                             name=refund.request.request_orders[i].order.product.name,
                                             external_id=refund.request.request_orders[i].id)
            refund.request.request_orders[i].order.service_product = service_product
            refund.request.request_orders[i].order_sum = amount
            refund.request.request_orders[i].order.start_dt_utc = orig_payment.request.request_orders[
                i].order.start_dt_utc
            refund.request.request_orders[i].order.start_dt_offset = orig_payment.request.request_orders[
                i].order.start_dt_offset

            if refund.service.id in (ServiceId.TAXI_PAYMENT, ServiceId.TAXI_CORP):
                refund.request.request_orders[i].order.service_order_id_str = orig_payment.request.request_orders[
                    i].order.service_order_id_str

    def create_payment_json_rows(self, request, payment, amount, service_id, contract, start_dt):

        if not start_dt:
            start_dt = datetime.datetime.now()
        start_dt = str(time.mktime(start_dt.timetuple()))
        json_rows = []
        for ro in request.request_orders:
            service_product = ServiceProduct(service_id=service_id,
                                             partner_id=contract.client_id,
                                             product_id=ro.order.product.id,
                                             name=ro.product.name,
                                             external_id=ro.id)
            self.session.add(service_product)
            self.session.flush()

            order_dt = str(time.mktime(ro.order.dt.timetuple()))
            json_rows.append(
                {
                    "id": str(ro.id),
                    "amount": str(amount),
                    "cancel_dt": None,
                    "fiscal_inn": " 7811736789",
                    "fiscal_nds": None,
                    "fiscal_title": "Перевозка пассажиров и багажа",
                    "price": str(amount),
                    "quantity": "1",
                    "order": {
                        "clid": None,
                        "commission_category": None,
                        "contract_id": None,
                        "dt": order_dt,
                        "service_id": str(ro.order.service_id),
                        "service_order_id": TAXI_SERVICE_ORDER_STR,
                        "service_order_id_number": ro.order.service_order_id,
                        "start_dt_offset": 0,
                        "start_dt_utc": start_dt,
                        "service_product_id": service_product.id,
                        "service_product_external_id": None,
                        "text": None,
                        "price": "10.0",
                        "passport_id": None,
                    },
                }
            )

        payment.payment_rows = json_rows

    def create_refund_json_rows(self, request, refund, orig_payment, amount):
        json_rows = []
        actual_rows = tt.transaction_factory(orig_payment).rows

        for i in range(len(request.request_orders)):
            json_rows.append(
                {
                    "id": str(request.request_orders[i].id),
                    "amount": str(actual_rows[i].amount),
                    "cancel_dt": None,
                    "fiscal_inn": " 7811736789",
                    "fiscal_nds": None,
                    "fiscal_title": "Перевозка пассажиров и багажа",
                    "price": str(actual_rows[i].amount),
                    "quantity": "1",
                    "order": {
                        "clid": None,
                        "commission_category": None,
                        "contract_id": None,
                        "dt": str(time.mktime(actual_rows[i].order.dt.timetuple())),
                        "service_id": actual_rows[i].order.service_id,
                        "service_order_id": actual_rows[i].order.service_order_id,
                        "service_order_id_number": actual_rows[i].order.service_order_id_number,
                        "start_dt_offset": actual_rows[i].order.start_dt_offset,
                        "start_dt_utc": str(time.mktime(actual_rows[i].order.start_dt_utc.timetuple())),
                        "service_product_id": actual_rows[i].order.service_product_id,
                        "service_product_external_id": None,
                        "text": None,
                        "price": "10.0",
                        "passport_id": None,
                    },
                }
            )

        refund.payment_rows = json_rows

    def create_trust_payment(self, amount=AMOUNT,
                             payment_method=TEST_CARD_PAYMENT_METHOD,
                             service_id=TEST_SERVICE_ID,
                             ym_shop_id=TEST_YM_SHOP_ID,
                             source_scheme=BS_SOURCE_SCHEME, dt=DT,
                             payment_dt=PAYMENT_DT, register=None,
                             with_register=True, register_line=None,
                             postauth_dt=DT, contract=None,
                             request=None, thirdparty_service=None,
                             currency=CURRENCY_CHAR_CODE,
                             composite_tag=None, start_dt=None,
                             service_fee=None,
                             developer_payload=None,
                             row_developer_payload=None,
                             actual_rows='request_rows',
                             payout_ready_dt=None
                             ):
        payment = TrustPayment(None)
        payment.trust_payment_id = str(bson.ObjectId())
        payment.amount = amount
        payment.payment_method = payment_method
        payment.ym_shop_id = ym_shop_id
        payment.payment_dt = payment_dt
        payment.dt = dt
        payment.paysys_partner_id = PAYSYS_PARTNER_ID
        payment.service = self.get_service(service_id)
        payment.source_scheme = source_scheme
        payment.postauth_dt = postauth_dt
        payment.composite_tag = composite_tag
        payment.terminal = self.session.query(mapper.Terminal).getone(TERMINAL_ID)
        payment.payout_ready_dt = payout_ready_dt
        if not contract:
            contract = self.create_contract(service_id=service_id)

        if not thirdparty_service:
            self.create_thirdparty_service(service_id=service_id)

        if with_register and not register:
            payment.register = self.create_register(paysys_code=payment.paysys_code)
            payment.register_line = self.create_register_line(payment.register)
        else:
            payment.register = register
            payment.register_line = register_line

        if not request:
            request = self.create_request(service_id=service_id, request_sum=amount * 3)
        if actual_rows == 'request_rows':
            self.create_payment_request_rows(request, payment, amount, service_id, contract, start_dt,
                                             service_fee, row_developer_payload)
        elif actual_rows == 'json_rows':
            self.create_payment_json_rows(request, payment, amount, service_id, contract, start_dt)
        else:
            raise Exception('wrong payment rows type')

        payment.currency = currency

        self.session.add(payment)

        # all extprops stuff needs to be set to object, attached to session
        payment.developer_payload = json.dumps(developer_payload) if developer_payload else None

        self.session.flush()
        return payment

    def create_refund(self, orig_payment, source_scheme=BS_SOURCE_SCHEME, dt=REFUND_DT, payment_dt=PAYMENT_DT,
                      request=None, contract=None, amount=AMOUNT, actual_rows='request_rows', **kwargs):
        refund = Refund(orig_payment, orig_payment.amount, None, None)
        refund.trust_refund_id = str(bson.ObjectId())
        refund.orig_payment = orig_payment
        refund.dt = dt
        refund.service = orig_payment.service
        refund.payment_dt = payment_dt
        refund.source_scheme = source_scheme

        if not request:
            request = self.create_request(service_id=orig_payment.service.id, request_sum=amount * 3)

        if actual_rows == 'request_rows':
            self.create_refund_request_rows(request, refund, orig_payment, amount)
        elif actual_rows == 'json_rows':
            self.create_refund_json_rows(request, refund, orig_payment, amount)
        else:
            raise Exception('wrong payment rows type')

        for arg in kwargs:
            setattr(refund, arg, kwargs[arg])

        self.session.add(refund)
        self.session.flush()
        return refund

    def create_contract(self, dt=None, contract_signed=True, sign_dt=datetime.datetime.now(),
                        service_id=TEST_SERVICE_ID, firm=FirmId.YANDEX_OOO, person_type='ur',
                        person=None, **kwargs):
        session = self.session
        if not person:
            person = PersonBuilder(type=person_type).build(session).get_obj()
        client = person.client
        if contract_signed:
            contract = ContractBuilder(client=client, is_signed=sign_dt, services=service_id,
                                       firm=firm, **kwargs).build(session).get_obj()
        else:
            contract = ContractBuilder(client=client, services=service_id, is_signed=None,
                                       **kwargs).build(session).get_obj()
        contract.person = person
        reward_pct = TAXI_REWARD_PCT
        if 'partner_commission_pct2' in kwargs:
            reward_pct = kwargs['partner_commission_pct2']

        contract.col0.partner_commission_pct2 = reward_pct  # necessary for TAXI
        if not dt and contract_signed:
            dt = DT - datetime.timedelta(days=100)
            contract.col0.dt = dt
        session.add(contract)
        session.flush()
        return contract

    def get_service(self, service_id):
        service = self.session.query(mapper.Service).get(service_id)
        return service

    def create_request(self, service_id, request_sum=AMOUNT * 3):
        client = ClientBuilder()
        product = ProductBuilder()
        request = RequestBuilder(basket=BasketBuilder(
            rows=[BasketItemBuilder(quantity=2, order=OrderBuilder(client=client, product=product,
                                                                   service_id=service_id),
                                    desired_discount_pct=0, user_data='1'),
                  BasketItemBuilder(quantity=1, order=OrderBuilder(client=client, product=product,
                                                                   service_id=service_id),
                                    desired_discount_pct=3, user_data='2'),
                  BasketItemBuilder(quantity=10, order=OrderBuilder(client=client, product=product,
                                                                    service_id=service_id),
                                    desired_discount_pct=12, user_data='3')],
            client=client)).build(self.session).obj
        request.request_sum = request_sum
        return request

    def create_thirdparty_service(self, service_id=TEST_SERVICE_ID, postauth_check=0, postauth_ready_check=0,
                                  reward_refund=1, get_commission_from='COMMISSION_CATEGORY',
                                  force_partner_id=None, **kwargs):
        thirdparty_service = mapper.ThirdPartyService(
            id=service_id, postauth_check=postauth_check, postauth_ready_check=postauth_ready_check,
            reward_refund=reward_refund, get_commission_from=get_commission_from,
            force_partner_id=force_partner_id, enabled=True, _session=self.session, **kwargs)
        self.session.merge(thirdparty_service)
        self.session.flush()
        return thirdparty_service

    def create_payment_with_register_commission(self, commission):
        register = self.create_register(register_commission=commission,
                                        paysys_code=sa.inspect(TrustPayment).polymorphic_identity)
        register_line = self.create_register_line(register=register, register_commission=commission)
        payment = self.create_trust_payment(register=register, register_line=register_line)
        return payment

    def create_register(self, register_commission=PAYSYS_COMMISSION, amount=AMOUNT,
                        paysys_code='YAMONEY'):
        PaymentRegisterSubclass = get_polymorphic_subclass(PaymentRegister, paysys_code)
        register = PaymentRegisterSubclass(DT, amount, commission=register_commission, currency='RUR')
        return register

    def create_register_line(self, register, amount=AMOUNT, register_commission=PAYSYS_COMMISSION):
        PaymentRegisterLineSubclass = get_polymorphic_subclass(PaymentRegisterLine, register.paysys_code)
        register_line = PaymentRegisterLineSubclass(
            register, DT, amount, commission=register_commission, ym_invoice_id=1,
            wallet='w', aviso_dt=datetime.datetime.now()
        )
        return register_line

    def create_taxi_stat(self, contract, payment_type):
        taxi_stat = mapper.PartnerTaxiStat(order_text=TAXI_SERVICE_ORDER_STR, dt=PAYMENT_DT,
                                           client_id=contract.client_id,
                                           payment_type=payment_type,
                                           tariffication_dt_utc=PAYMENT_DT - datetime.timedelta(hours=3),
                                           tariffication_dt_offset=3)
        self.session.add(taxi_stat)
        self.session.flush()

    def create_service_product(self, contract, service_id, service_code=None, service_fee=None):
        product = self.session.query(mapper.Product).filter(
            mapper.Product.engine_id == service_id,
            mapper.Product.service_code == service_code
        ).one()
        service_product = ServiceProduct(service_id=service_id,
                                         partner_id=contract.client_id,
                                         product_id=product.id,
                                         name='Service Product',
                                         service_fee=service_fee,
                                         external_id=str(uuid.uuid4()))
        self.session.add(service_product)
        self.session.flush()
        return service_product

    def get_incoming_rows(self, transaction):
        processor = tt.TransactionProcessor(transaction)
        return processor.transaction.rows

    def _test_equal(self, payment, expected):
        """
        :param payment:
        :param expected: dictionary  {result_row.attr: expected_value}
        :return:
        """
        rows = self.get_incoming_rows(payment)
        result_row = tt.construct_3rdparty(rows[0])
        if not self.pipeline:
            raise Exception('No pipeline')

        result_row = self.pipeline.process(rows[0], result_row)
        for attr, expect_value in expected.items():
            res_value = getattr(result_row, attr)
            assert res_value == expect_value, attr
