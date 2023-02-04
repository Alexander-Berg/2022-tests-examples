# -*- coding: utf-8 -*-
from decimal import Decimal as D
import datetime
import bson
from collections import defaultdict
import json
import pytest
import copy
import mock

import sqlalchemy as sa

from billing.contract_iface.constants import ContractTypeId
from billing.contract_iface.contract_meta import ContractTypes

from balance import mapper, muzzle_util as ut, thirdparty_transaction as tt
from balance.constants import ServiceId, FirmId, RegionId, \
    SPENDABLE_PROMO_TYPE, SPENDABLE_MARKETPLACE_TYPE, SPENDABLE_CORPTAXI_TYPE, POSTPAY_PAYMENT_TYPE, PREPAY_PAYMENT_TYPE
from balance import contractpage
from balance.mapper import TrustPayment, Refund, ServiceProduct
from tests import object_builder as ob
from tests.object_builder import (
    BasketBuilder, BasketItemBuilder, OrderBuilder, RequestBuilder,
    PersonBuilder, ProductBuilder
)
from tests.balance_tests.thirdparty_transaction.common import (
    TestThirdpartyTransactions,
    TEST_CARD_PAYMENT_METHOD, CURRENCY_CHAR_CODE, PAYSYS_PARTNER_ID, PAYMENT_DT,
    DT, AMOUNT, TEST_SERVICE_ID, BS_SOURCE_SCHEME, TAXI_REWARD_PCT
)

__author__ = 'quark'

TAXI_CARD_PAYMENT_TYPE = 'card'
CASH_PAYMENT_METHOD = 'cash-12345'
DIRECT_CARD_PAYMENT_METHOD = 'direct_card'
PROMOCODE_PAYMENT_METHOD = 'promocode'
COMPENSATION_PAYMENT_METHOD = 'compensation'
CDISCOUNT_PAYMENT_METHOD = 'compensation_discount'
TRUST_GROUP_PAYMENT_PAYMENT_METHOD = 'composite'
SPASIBO_PAYMENT_METHOD = 'spasibo'

NOT_YANDEX_YM_SHOP_ID = 29437

SMALL_AMOUNT = D('1')

NOT_BS_SOURCE_SCHEME = 'bo'
MINIMAL_PAYMENT_COMMISSION_VALUE = D('30')
PARTNER_COMMISSION_PCT_VALUE = D('15')

PARTNER_COMMISSION_SUM = D('60')
PARTNER_COMMISSION_SUM2 = D('30')

NOT_ACCEPTED_JSON_ROW_SERVICES = {
    ServiceId.TAXI_CORP,  # в создание тесовых данных нужно добавить passport_id
    None,  # синий маркет
}


@pytest.mark.parametrize('s, prefix, expected', [
    ['virtual::sberbank_credit', 'virtual::', 'sberbank_credit'],
    ['virtual::new_promocode', 'virtual::', 'new_promocode'],
    ['new_promocode', 'virtual::', 'new_promocode'],
    ['virtual', 'virtual::', 'virtual'],
])
def test_remove_prefix(s, prefix, expected):
    actual = tt.remove_prefix(s, prefix)
    assert actual == expected


class PaymentTestMixIn(object):
    def create_test_refund(self, actual_rows='request_rows'):
        payment = self.create_payment(actual_rows=actual_rows)['payment']
        refund = self.create_refund(payment, actual_rows=actual_rows)
        return refund

    def create_payment(self, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid)
        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, **kwargs)

        return {
            'payment': payment,
            'contract': contract
        }

    def test_payment(self):
        info = self.create_payment()
        res = self.process(info['payment'])
        self.check(info, res)
        self.additional_check(info, res)

    def test_json_payment(self):
        if self.sid in NOT_ACCEPTED_JSON_ROW_SERVICES:
            pytest.skip('JSON rows not implemented for service {}'.format(self.sid))
        info = self.create_payment(actual_rows='json_rows')
        res = self.process(info['payment'])
        self.check(info, res)
        self.additional_check(info, res)

    def test_refund(self):
        refund = self.create_test_refund()
        self.process(refund)

    def test_json_refund(self):
        if self.sid in NOT_ACCEPTED_JSON_ROW_SERVICES:
            pytest.skip('JSON rows not implemented for service {}'.format(self.sid))
        refund = self.create_test_refund(actual_rows='json_rows')
        self.process(refund)

    def process(self, transaction):
        with transaction.session.begin():
            return tt.TransactionProcessor(transaction).process()

    def check(self, info, res):
        contract = info['contract']
        transaction = info['payment']
        self.assertEqual(len(res), 3)
        self.assertTrue(all(r.contract_id == contract.id for r in res))
        self.assertTrue(all(r.payment_id == transaction.id for r in res))
        self.assertTrue(all(r.partner == contract.client for r in res))
        self.assertTrue(all(r.amount == AMOUNT for r in res))
        self.assertTrue(all(r.partner_currency == contract.current_signed().get_currency().char_code for r in res))
        self.assertTrue(all(r.amount_fee is None for r in res))
        self.assertTrue(all(r.payment_type == 'card' for r in res))
        self.assertTrue(all(r.transaction_type == 'payment' for r in res))
        self.assertTrue(all(r.currency == transaction.currency for r in res))
        self.assertTrue(all(r.service_id == self.sid for r in res))
        self.assertTrue(all(r.trust_id == transaction.trust_payment_id for r in res))
        self.assertTrue(all(r.trust_payment_id == transaction.trust_payment_id for r in res))

    def additional_check(self, info, res):
        pass


class TestTaxiPayment(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TAXI_PAYMENT

    def create_payment(self, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=FirmId.TAXI)
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def test_payment_without_contract(self):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=FirmId.TAXI,
                                        contract_signed=False)
        payment = self.create_trust_payment(service_id=self.sid,
                                            thirdparty_service=thirdparty_service,
                                            contract=contract)
        self.assertRaises(tt.Delay, tt.TransactionProcessor(payment).process)

    def test_payment_json_without_contract(self):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=FirmId.TAXI,
                                        contract_signed=False)
        payment = self.create_trust_payment(service_id=self.sid,
                                            thirdparty_service=thirdparty_service,
                                            contract=contract, actual_rows='json_rows')
        self.assertRaises(tt.Delay, tt.TransactionProcessor(payment).process)

    def test_reward_wo_nds(self):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=22, currency=840, person_type='eu_yt')
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, currency='USD')

        trans = self.process(payment)
        self.assertEqual(trans[0].yandex_reward, trans[0].yandex_reward_wo_nds)

    def test_reward_wo_nds_json(self):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=22, currency=840, person_type='eu_yt')
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, currency='USD', actual_rows='json_rows')

        trans = self.process(payment)
        self.assertEqual(trans[0].yandex_reward, trans[0].yandex_reward_wo_nds)

    def test_payment(self):
        # старая логика - конфига нет
        if 'TAXI_RF_NO_MIN_REWARD_BEGIN_DT' in self.session.config.__dict__:
            self.session.config.__dict__.pop('TAXI_RF_NO_MIN_REWARD_BEGIN_DT')
        return super(TestTaxiPayment, self).test_payment()

    def test_json_payment(self):
        # старая логика - конфига нет
        if 'TAXI_RF_NO_MIN_REWARD_BEGIN_DT' in self.session.config.__dict__:
            self.session.config.__dict__.pop('TAXI_RF_NO_MIN_REWARD_BEGIN_DT')
        return super(TestTaxiPayment, self).test_json_payment()

    def test_no_reward_rf_config_before(self):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=13, country=225, partner_commission_pct2=None)
        for rows_type in ('request_rows', 'json_rows'):
            # self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
            payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                                contract=contract, actual_rows=rows_type)
            self.session.config.__dict__['TAXI_RF_NO_MIN_REWARD_BEGIN_DT'] = PAYMENT_DT
            trans = self.process(payment)
            info = {'contract': contract, 'payment': payment}
            self.check(info, trans)
            self.assertEqual(trans[0].yandex_reward, D('0'))
            self.assertEqual(trans[0].yandex_reward, trans[0].yandex_reward_wo_nds)
            self.assertEqual(D(sum(r.yandex_reward for r in trans)), D('0'))
            self.assertEqual(D(sum(r.yandex_reward_wo_nds for r in trans)), D('0'))
            self.assertTrue(all(r.paysys_type_cc == 'yamoney' for r in trans))

    def test_no_reward_rf_config_after(self):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, firm=13, country=225, partner_commission_pct2=None)
        for rows_type in ('request_rows', 'json_rows'):
            # self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
            payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                                contract=contract, actual_rows=rows_type)
            self.session.config.__dict__['TAXI_RF_NO_MIN_REWARD_BEGIN_DT'] = PAYMENT_DT + datetime.timedelta(days=1)
            trans = self.process(payment)
            info = {'contract': contract, 'payment': payment}
            self.check(info, trans)
            self.assertEqual(trans[0].yandex_reward, D('0.01'))
            self.assertEqual(D(sum(r.yandex_reward for r in trans)), D('0.03'))
            self.assertEqual(D(sum(r.yandex_reward_wo_nds for r in trans)),
                             D('0.03') / mapper.Nds.get_nds_koef_on_dt(self.session, payment.payment_dt))
            self.assertTrue(all(r.paysys_type_cc == 'yamoney' for r in trans))

    def additional_check(self, info, res):
        self.assertEqual(D(sum(r.yandex_reward for r in res)), D('0.03'))
        self.assertEqual(D(sum(r.yandex_reward_wo_nds for r in res)),
                         D('0.03') / mapper.Nds.get_nds_koef_on_dt(self.session, info['payment'].payment_dt))
        self.assertTrue(all(r.paysys_type_cc == 'yamoney' for r in res))


class TestTaxiPromo(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TAXI_PROMO

    def create_payment(self, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_PROMO)
        contract = self.create_contract(service_id=self.sid, ctype='SPENDABLE',
                                        contract_type=SPENDABLE_PROMO_TYPE)

        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        contract2 = self.create_contract(service_id=ServiceId.TAXI_PROMO, link_contract_id=contract.id,
                                         ctype='SPENDABLE', contract_type=SPENDABLE_PROMO_TYPE)
        payment = self.create_trust_payment(service_id=ServiceId.TAXI_PROMO, thirdparty_service=thirdparty_service,
                                            contract=contract2, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def test_no_ctype(self):
        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_PROMO)
        contract = self.create_contract(service_id=ServiceId.TAXI_PAYMENT)
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        contract2 = self.create_contract(service_id=ServiceId.TAXI_PROMO, link_contract_id=contract.id,
                                         contract_type=SPENDABLE_PROMO_TYPE)
        payment = self.create_trust_payment(service_id=ServiceId.TAXI_PROMO, thirdparty_service=thirdparty_service,
                                            contract=contract2)
        self.assertRaises(tt.Delay, tt.TransactionProcessor(payment).process)

    def test_no_link_contract(self):
        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_PROMO)
        contract = self.create_contract(service_id=ServiceId.TAXI_PROMO)
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        payment = self.create_trust_payment(service_id=ServiceId.TAXI_PROMO, thirdparty_service=thirdparty_service,
                                            contract=contract)
        self.assertRaises(tt.Delay, tt.TransactionProcessor(payment).process)

    def test_different_currency(self):
        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_PROMO)
        contract = self.create_contract(service_id=ServiceId.TAXI_PAYMENT)
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        contract2 = self.create_contract(service_id=ServiceId.TAXI_PROMO, link_contract_id=contract.id,
                                         ctype='SPENDABLE', contract_type=SPENDABLE_PROMO_TYPE)
        payment = self.create_trust_payment(service_id=ServiceId.TAXI_PROMO, thirdparty_service=thirdparty_service,
                                            contract=contract2, currency='USD')

        # tt.TransactionProcessor(payment).process()
        self.assertRaises(tt.ThirdpartyError, tt.TransactionProcessor(payment).process)

    def check(self, info, res):
        contract = info['contract']
        transaction = info['payment']
        self.assertEqual(len(res), 3)
        self.assertTrue(all(r.payment_id == transaction.id for r in res))
        self.assertTrue(all(r.amount == AMOUNT for r in res))
        self.assertTrue(all(r.partner_currency == contract.current_signed().get_currency().char_code for r in res))
        self.assertTrue(all(r.amount_fee is None for r in res))
        self.assertTrue(all(r.payment_type == 'card' for r in res))
        self.assertTrue(all(r.transaction_type == 'payment' for r in res))
        self.assertTrue(all(r.currency == transaction.currency for r in res))
        self.assertTrue(all(r.service_id == self.sid for r in res))
        self.assertTrue(all(r.trust_id == transaction.trust_payment_id for r in res))
        self.assertTrue(all(r.trust_payment_id == transaction.trust_payment_id for r in res))


class TestTaxiCorp(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TAXI_CORP

    def create_payment(self, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=[self.sid, ServiceId.TAXI_CORP_PARTNERS], ctype='SPENDABLE',
                                        contract_type=SPENDABLE_CORPTAXI_TYPE)
        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def test_no_ctype(self):
        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_CORP)
        contract = self.create_contract(service_id=ServiceId.TAXI_CORP, contract_type=SPENDABLE_CORPTAXI_TYPE)
        payment = self.create_trust_payment(service_id=ServiceId.TAXI_CORP, thirdparty_service=thirdparty_service,
                                            contract=contract)
        self.assertRaises(tt.Delay, tt.TransactionProcessor(payment).process)


class TestMultiship(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.MULTISHIP_PAYMENT

    def additional_check(self, info, res):
        self.assertEqual(D(sum(r.yandex_reward for r in res)), D('0.03'))
        self.assertEqual(D(sum(r.yandex_reward_wo_nds for r in res)),
                         D('0.03') / mapper.Nds.get_nds_koef_on_dt(self.session, info['payment'].payment_dt))
        self.assertTrue(all(r.paysys_type_cc == 'yamoney' for r in res))


class TestRealty(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.REALTY_PAYMENTS

    def create_payment(self, **kwargs):
        contract = self.create_contract(service_id=self.sid)
        force_partner_id = contract.client_id
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid, force_partner_id=force_partner_id)

        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def test_no_force_partner_id(self):
        contract = self.create_contract(service_id=ServiceId.REALTY_PAYMENTS)
        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.REALTY_PAYMENTS)

        payment = self.create_trust_payment(service_id=ServiceId.REALTY_PAYMENTS, thirdparty_service=thirdparty_service,
                                            contract=contract)

        self.assertRaises(tt.ThirdpartyError, tt.TransactionProcessor(payment).process)

    def additional_check(self, info, res):
        self.assertEqual(sum(r.yandex_reward for r in res), 3 * AMOUNT)
        self.assertEqual(D(sum(r.yandex_reward_wo_nds for r in res)),
                         3 * AMOUNT / mapper.Nds.get_nds_koef_on_dt(self.session, info['payment'].payment_dt))
        self.assertTrue(all(r.paysys_type_cc == 'yamoney' for r in res))


class TicketsTestMixIn(PaymentTestMixIn):
    with_service_product = True

    def create_contract(self, **kwargs):
        contract = super(TicketsTestMixIn, self).create_contract(partner_commission_pct2=0.0, **kwargs)
        contract.col0.personal_account = 1
        contract.col0.currency = 810
        self.session.flush()
        cp = contractpage.ContractPage(self.session, contract.id)
        cp.create_personal_accounts()
        return contract

    def create_payment(self, **kwargs):
        res = super(TicketsTestMixIn, self).create_payment(**kwargs)
        if self.with_service_product:
            self.create_service_product(res['contract'], self.sid, 'YANDEX_SERVICE_WO_VAT', 666)
        return res

    def test_payment_with_payout(self):
        payout_ready_dt = DT + datetime.timedelta(days=10)
        payment = self.create_payment(payout_ready_dt=payout_ready_dt)['payment']
        res = self.process(payment)
        # платёж должен успешно разобраться, у строчек теперь должна появиться дата выплаты
        for row in res:
            self.assertEqual(row.payout_ready_dt, payout_ready_dt)

    def additional_check(self, info, res):
        self.assertEqual(D(sum(r.yandex_reward for r in res)), D('0.03'))
        self.assertEqual(D(sum(r.yandex_reward_wo_nds for r in res)),
                         D('0.03') / mapper.Nds.get_nds_koef_on_dt(self.session, info['payment'].payment_dt))
        self.assertTrue(all(r.paysys_type_cc == 'yamoney' for r in res))


class TestTickets(TicketsTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TICKETS

    with_service_product = False

    def test_no_min_reward_certificate(self):
        request = self.create_request(self.sid)
        for i in range(len(request.request_orders)):
            request.request_orders[i].order.commission_category = 1

        payment = self.create_payment(amount=SMALL_AMOUNT, request=request, service_fee=2)['payment']
        res = self.process(payment)
        self.assertEqual(sum(r.yandex_reward for r in res), D(0))


class TestTickets2(TicketsTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TICKETS2


class TestTicketsToEvents(TicketsTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TICKETS_TO_EVENTS

    def test_new_promocode_without_tag(self):
        promocode_payment = self.create_payment(payment_method='new_promocode')['payment']
        self.process(promocode_payment)

    def test_new_promocode_no_composite_part(self):
        composite_tag = 1000000007
        promocode_payment = self.create_payment(payment_method='new_promocode', composite_tag=composite_tag)['payment']
        self.assertRaises(tt.Delay, tt.TransactionProcessor(promocode_payment).process)

    def test_new_promocode_composite_part_not_processed(self):
        composite_tag = 1000000007
        promocode_payment = self.create_payment(payment_method='new_promocode', composite_tag=composite_tag)['payment']

        # create composite payments
        self.create_payment(composite_tag=composite_tag)
        self.create_payment(composite_tag=composite_tag)

        self.assertRaises(tt.Delay, tt.TransactionProcessor(promocode_payment).process)

    def test_new_promocode_composite_part_not_processed_completely(self):
        composite_tag = 1000000007
        promocode_payment = self.create_payment(payment_method='new_promocode', composite_tag=composite_tag)['payment']
        composite_payments = [
            self.create_payment(composite_tag=composite_tag)['payment'],
            self.create_payment(composite_tag=composite_tag)['payment']
        ]

        self.process(composite_payments[0])

        self.assertRaises(tt.Delay, tt.TransactionProcessor(promocode_payment).process)

    def test_new_promocode(self):
        composite_tag = 1000000007
        postauth_dt = DT
        promocode_payment = \
            self.create_payment(payment_method='new_promocode', postauth_dt=postauth_dt - datetime.timedelta(days=1),
                                composite_tag=composite_tag)['payment']
        composite_payments = [
            self.create_payment(composite_tag=composite_tag, postauth_dt=postauth_dt)['payment'],
            self.create_payment(composite_tag=composite_tag, postauth_dt=postauth_dt)['payment']
        ]
        for payment in composite_payments:
            self.process(payment)

        res = self.process(promocode_payment)
        self.assertTrue(all(r.dt == postauth_dt for r in res))


class TestTickets3(TestTicketsToEvents):
    sid = ServiceId.TICKETS3
    add_payout_ready_dt = True


class TestBuses(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.BUSES
    TEST_COMM_CATEGORY = '1500'

    def create_payment(self, contract=None, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)

        if kwargs.get('pct'):
            pct = kwargs.pop('pct')
        else:
            pct = None

        if not contract:
            contract = self.create_contract(service_id=self.sid)
            contract.col0.partner_commission_pct2 = pct or PARTNER_COMMISSION_PCT_VALUE

        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def test_payment_pct2_is_null_no_comm_category(self, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid)
        contract.col0.partner_commission_pct2 = None

        request = self.create_request(self.sid)
        for i in range(len(request.request_orders)):
            request.request_orders[i].order.commission_category = None

        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, request=request, **kwargs)
        res = self.process(payment)
        info = {
            'payment': payment,
            'contract': contract
        }
        self.check(info, res)
        self.assertTrue(all(r.yandex_reward == D('0.01') for r in res))

    def create_payment_pct2_is_null(self, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid)
        contract.col0.partner_commission_pct2 = None

        request = self.create_request(self.sid)
        for i in range(len(request.request_orders)):
            request.request_orders[i].order.commission_category = self.TEST_COMM_CATEGORY

        payment = self.create_trust_payment(service_id=self.sid, thirdparty_service=thirdparty_service,
                                            contract=contract, request=request, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def test_payment_pct2_is_null(self):
        info = self.create_payment_pct2_is_null()
        res = self.process(info['payment'])
        self.check(info, res)
        self.assertEqual(D(sum(r.yandex_reward for r in res)),
                         D('300') * D(self.TEST_COMM_CATEGORY) * D('0.01') * D('0.01'))

    def additional_check(self, info, res):
        pct = info['contract'].col0.partner_commission_pct2
        self.assertEqual(D(sum(r.yandex_reward for r in res)), D('300') * D(pct) * D('0.01'))

    def test_refund(self):
        pass


class TestMarketplace(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = ServiceId.MARKETPLACE

    def create_payment(self, **kwargs):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, ctype='SPENDABLE',
                                        contract_type=SPENDABLE_MARKETPLACE_TYPE)

        payment = self.create_trust_payment(service_id=self.sid, payment_method='subsidy',
                                            thirdparty_service=thirdparty_service,
                                            contract=contract, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def check(self, info, res):
        contract = info['contract']
        transaction = info['payment']
        self.assertEqual(len(res), 3)
        self.assertTrue(all(r.payment_id == transaction.id for r in res))
        self.assertTrue(all(r.amount == AMOUNT for r in res))
        self.assertTrue(all(r.partner_currency == contract.current_signed().get_currency().char_code for r in res))
        self.assertTrue(all(r.amount_fee is None for r in res))
        self.assertTrue(all(r.payment_type == 'subsidy' for r in res))
        self.assertTrue(all(r.transaction_type == 'payment' for r in res))
        self.assertTrue(all(r.currency == transaction.currency for r in res))
        self.assertTrue(all(r.service_id == self.sid for r in res))
        self.assertTrue(all(r.trust_id == transaction.trust_payment_id for r in res))
        self.assertTrue(all(r.trust_payment_id == transaction.trust_payment_id for r in res))

    def additional_check(self, info, res):
        self.assertTrue(all(r.amount == AMOUNT for r in res))
        self.assertTrue(all(r.paysys_type_cc == 'yamarket' for r in res))

    def test_no_subsidy(self):
        thirdparty_service = self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, ctype='SPENDABLE',
                                        contract_type=SPENDABLE_MARKETPLACE_TYPE)

        payment = self.create_trust_payment(service_id=self.sid,
                                            thirdparty_service=thirdparty_service,
                                            contract=contract)
        self.assertRaises(tt.Skip, tt.TransactionProcessor(payment).process)


class TestBlueMarketSpasibo(PaymentTestMixIn, TestThirdpartyTransactions):
    sid = None

    def create_blue_contract(self, person_type='ph', **kwargs):
        partner = mapper.Client(client_type_id=0, name='ООО "Яндекс.Маркет"')
        self.session.add(partner)
        self.session.flush()
        person = mapper.Person(client=partner, type=person_type, name='Невероятный Бухалк')
        self.session.add(person)
        self.session.flush()

        contract = mapper.Contract(ContractTypes(type='GENERAL'))
        self.session.add(contract)

        contract.client = partner
        contract.person = person

        col0 = contract.col0
        col0.commission = ContractTypeId.OFFER
        col0.currency = 810
        col0.fake_id = 0
        col0.firm = FirmId.MARKET
        col0.payment_type = POSTPAY_PAYMENT_TYPE
        col0.partner_credit = 1
        col0.personal_account = 1
        col0.services = {ServiceId.BLUE_PAYMENTS: 1, ServiceId.BLUE_SRV: 1}
        col0.unilateral = 1

        dt = ut.trunc_date(self.session.now() - datetime.timedelta(days=365))
        col0.dt = dt
        col0.is_signed = dt

        col0.maybe_create_barcode()
        contract.external_id = contract.create_new_eid()
        self.session.flush()

        cp = contractpage.ContractPage(self.session, contract.id)
        cp.create_personal_accounts()
        self.session.flush()
        return contract

    def create_tech_data(self):
        tech_contract = self.create_blue_contract(person_type='ph')

        # В базе есть реальные данные, если по какой-то причине станут неактуальными или их не станет -
        # можно раскомментировать. Клиенты и договоры чистятся, поэтому force_partner и договор надо создавать

        # thirdparty_service_610 = self.create_thirdparty_service(
        #     service_id=ServiceId.BLUE_PAYMENTS, reward_refund=None, get_commission_from=None, agent_report=1,
        #     force_partner_id=force_partner.id,
        #     product_mapping_config='{"service_product_options": {"457420": "no_process", "519442": "no_process"}, '
        #                            '"default_product_mapping": {"RUB": {"default": null}}}')
        # thirdparty_service_609 = self.create_thirdparty_service(service_id=ServiceId.BLUE_SUB, reward_refund=None,
        #                                                         get_commission_from=None)

        thirdparty_service_610 = self.session.query(mapper.ThirdPartyService).getone(id=610)
        thirdparty_service_610.force_partner_id = tech_contract.client.id
        self.session.flush()
        return tech_contract

    def create_orders(self, client):
        product = ProductBuilder()
        order_no_fee = OrderBuilder(client=client, product=product).build(self.session).obj
        service_product_no_fee = ServiceProduct(
            service_id=ServiceId.BLUE_PAYMENTS,
            partner_id=client.id,
            product_id=product.id,
            name=product.name,
            external_id='no_fee')

        order_no_fee.service_product = service_product_no_fee

        order_fee = OrderBuilder(client=client, product=product).build(self.session).obj
        service_product_fee = ServiceProduct(
            service_id=ServiceId.BLUE_PAYMENTS,
            partner_id=client.id,
            product_id=product.id,
            name=product.name,
            service_fee=1,
            external_id='fee')
        order_fee.service_product = service_product_fee
        self.session.flush()

        return order_no_fee, order_fee

    def create_base_objects(self):

        contract_payments = self.create_blue_contract(person_type='ur')

        client, person = contract_payments.client, contract_payments.person

        contract_subs = self.create_contract(ctype='SPENDABLE', service_id=ServiceId.BLUE_SUB, firm=FirmId.MARKET,
                                             person=person)

        order_no_fee, order_fee = self.create_orders(client)

        return contract_payments, contract_subs, order_no_fee, order_fee

    def create_trust_payment(self, request=None,
                             amount=None,
                             payment_method=TEST_CARD_PAYMENT_METHOD,
                             service_id=TEST_SERVICE_ID,
                             ym_shop_id=None,
                             source_scheme=BS_SOURCE_SCHEME,
                             dt=DT,
                             payment_dt=PAYMENT_DT,
                             postauth_dt=DT,
                             currency=CURRENCY_CHAR_CODE,
                             composite_tag=None,
                             developer_payload=None,
                             payout_ready_dt=None
                             ):
        payment = TrustPayment(None)
        payment.trust_payment_id = str(bson.ObjectId())
        assert request or amount
        if amount:
            payment.amount = amount
        else:
            payment.amount = request.request_sum
        payment.payment_method = payment_method
        payment.ym_shop_id = ym_shop_id
        payment.payment_dt = payment_dt
        payment.dt = dt
        payment.paysys_partner_id = PAYSYS_PARTNER_ID
        payment.service = self.get_service(service_id)
        payment.source_scheme = source_scheme
        payment.postauth_dt = postauth_dt
        payment.composite_tag = composite_tag
        # payment.terminal = self.session.query(mapper.Terminal).getone(TERMINAL_ID)
        payment.payout_ready_dt = payout_ready_dt

        payment.currency = currency
        payment.request = request

        self.session.add(payment)

        # all extprops stuff needs to be set to object, attached to session
        payment.developer_payload = json.dumps(developer_payload) if developer_payload else None

        self.session.flush()
        return payment

    def create_payments(self, orders_amounts_data, client, **kwargs):

        composite_tag = ut.nvl(self.session.query(sa.func.max(mapper.TrustPayment.composite_tag)).scalar(), 0) + 10000

        payment_method_payments = defaultdict(list)
        total_sum = D('0')
        for payment_method, orders_amounts in orders_amounts_data.iteritems():
            rows = []
            for i, (order, amount) in enumerate(orders_amounts.iteritems(), start=1):
                rows.append(BasketItemBuilder(quantity=i, order=order, desired_discount_pct=0, user_data=str(i)))
            request = RequestBuilder(basket=BasketBuilder(rows=rows, client=client)).build(self.session).obj
            request_sum = D('0')
            for ro in request.request_orders:
                ro.order_sum = orders_amounts[ro.order]
                request_sum += ro.order_sum
            request.request_sum = request_sum
            total_sum += request_sum
            self.session.flush()
            payment = self.create_trust_payment(request=request, service_id=ServiceId.BLUE_PAYMENTS,
                                                payment_method=payment_method, composite_tag=composite_tag, **kwargs)
            payment_method_payments[payment_method].append(payment)

        group_payment = self.create_trust_payment(request=None, amount=total_sum, service_id=ServiceId.BLUE_PAYMENTS,
                                                  payment_method=TRUST_GROUP_PAYMENT_PAYMENT_METHOD, **kwargs)
        for payment_method, payments in payment_method_payments.iteritems():
            for payment in payments:
                payment.trust_group_id = group_payment.trust_payment_id
        self.session.flush()
        return payment_method_payments, group_payment

    def test_skip_group(self):
        self.create_tech_data()
        contract_payments, contract_subs, order_no_fee, order_fee = self.create_base_objects()

        orders_amounts_data = {
            SPASIBO_PAYMENT_METHOD: {order_no_fee: D('132'), order_fee: D('133')},
            TEST_CARD_PAYMENT_METHOD: {order_no_fee: D('134'), order_fee: D('135')},
        }
        payment_method_payments, group_payment = self.create_payments(orders_amounts_data, contract_payments.client)
        with pytest.raises(tt.Skip):
            self.process(group_payment)

    def test_skip_spasibo_wo_card(self):
        self.create_tech_data()
        contract_payments, contract_subs, order_no_fee, order_fee = self.create_base_objects()

        orders_amounts_data = {
            SPASIBO_PAYMENT_METHOD: {order_no_fee: D('132'), order_fee: D('133')},
            TEST_CARD_PAYMENT_METHOD: {order_no_fee: D('134'), order_fee: D('135')},
        }
        payment_method_payments, group_payment = self.create_payments(orders_amounts_data, contract_payments.client)

        for payment in payment_method_payments[SPASIBO_PAYMENT_METHOD]:
            with pytest.raises(tt.Delay):
                self.process(payment)

    def test_payment(self):
        tech_contract = self.create_tech_data()
        contract_payments, contract_subs, order_no_fee, order_fee = self.create_base_objects()
        payout_ready_dt = datetime.datetime.now().replace(microsecond=0)

        orders_amounts_data = {
            SPASIBO_PAYMENT_METHOD: {order_no_fee: D('132'), order_fee: D('133')},
            TEST_CARD_PAYMENT_METHOD: {order_no_fee: D('134'), order_fee: D('135')},
        }
        payment_method_payments, group_payment = self.create_payments(orders_amounts_data, contract_payments.client)

        # без payout_ready_dt
        for payment in payment_method_payments[TEST_CARD_PAYMENT_METHOD]:
            total_sum = sum(amount for amount in orders_amounts_data[TEST_CARD_PAYMENT_METHOD].values())
            res = self.process(payment)
            self.assertEqual(len(res), 2)

            fee_row, = filter(lambda r: r.internal == 1, res)
            row_amount = orders_amounts_data[TEST_CARD_PAYMENT_METHOD][order_fee]
            row_amount_wo_nds = row_amount / mapper.Nds.get_nds_koef_on_dt(self.session, payment.payment_dt)
            self.assertEqual(fee_row.service_id, ServiceId.BLUE_PAYMENTS)
            self.assertEqual(fee_row.total_sum, total_sum)
            self.assertEqual(fee_row.amount, row_amount)
            self.assertEqual(fee_row.yandex_reward, row_amount)
            self.assertEqual(fee_row.yandex_reward_wo_nds, row_amount_wo_nds)
            self.assertEqual(fee_row.amount_fee, None)
            self.assertEqual(fee_row.invoice_eid, None)
            self.assertEqual(fee_row.contract_id, tech_contract.id)
            self.assertEqual(fee_row.partner, tech_contract.client)
            self.assertEqual(fee_row.paysys_type_cc, 'yamoney')
            self.assertEqual(fee_row.payment_type, 'card')
            self.assertEqual(fee_row.payout_ready_dt, None)

            no_fee_row, = filter(lambda r: r.internal is None, res)
            agent_reward_pa, = filter(lambda i: i.service_code == 'AGENT_REWARD', contract_payments.invoices)
            row_amount = orders_amounts_data[TEST_CARD_PAYMENT_METHOD][order_no_fee]
            self.assertEqual(no_fee_row.service_id, ServiceId.BLUE_PAYMENTS)
            self.assertEqual(no_fee_row.total_sum, total_sum)
            self.assertEqual(no_fee_row.amount, row_amount)
            self.assertEqual(no_fee_row.yandex_reward, 0)  # No developers payload and min commission in contract
            self.assertEqual(no_fee_row.yandex_reward_wo_nds, 0)
            self.assertEqual(no_fee_row.amount_fee, None)
            self.assertEqual(no_fee_row.invoice_eid, agent_reward_pa.external_id)
            self.assertEqual(no_fee_row.contract_id, contract_payments.id)
            self.assertEqual(no_fee_row.partner, contract_payments.client)
            self.assertEqual(no_fee_row.paysys_type_cc, 'yamoney')
            self.assertEqual(no_fee_row.payment_type, 'card')
            self.assertEqual(fee_row.payout_ready_dt, None)

        for payment in payment_method_payments[SPASIBO_PAYMENT_METHOD]:
            total_sum = sum(amount for amount in orders_amounts_data[SPASIBO_PAYMENT_METHOD].values())
            res = self.process(payment)
            self.assertEqual(len(res), 2)

            fee_row, = filter(lambda r: r.internal == 1, res)
            row_amount = orders_amounts_data[SPASIBO_PAYMENT_METHOD][order_fee]
            row_amount_wo_nds = row_amount / mapper.Nds.get_nds_koef_on_dt(self.session, payment.payment_dt)
            self.assertEqual(fee_row.service_id, ServiceId.BLUE_PAYMENTS)
            self.assertEqual(fee_row.total_sum, total_sum)
            self.assertEqual(fee_row.amount, row_amount)
            self.assertEqual(fee_row.yandex_reward, row_amount)
            self.assertEqual(fee_row.yandex_reward_wo_nds, row_amount_wo_nds)
            self.assertEqual(fee_row.amount_fee, None)
            self.assertEqual(fee_row.invoice_eid, None)
            self.assertEqual(fee_row.contract_id, tech_contract.id)
            self.assertEqual(fee_row.partner, tech_contract.client)
            self.assertEqual(fee_row.paysys_type_cc, 'yamoney')
            self.assertEqual(fee_row.payment_type, 'spasibo')
            self.assertEqual(fee_row.payout_ready_dt, None)

            no_fee_row, = filter(lambda r: r.internal is None, res)
            row_amount = orders_amounts_data[SPASIBO_PAYMENT_METHOD][order_no_fee]
            self.assertEqual(no_fee_row.service_id, ServiceId.BLUE_SUB)
            self.assertEqual(no_fee_row.total_sum, total_sum)
            self.assertEqual(no_fee_row.amount, row_amount)
            self.assertEqual(no_fee_row.yandex_reward, None)
            self.assertEqual(no_fee_row.yandex_reward_wo_nds, None)
            self.assertEqual(no_fee_row.amount_fee, None)
            self.assertEqual(no_fee_row.contract_id, contract_subs.id)
            self.assertEqual(no_fee_row.partner, contract_subs.client)
            self.assertEqual(no_fee_row.paysys_type_cc, 'spasibo')
            self.assertEqual(no_fee_row.payment_type, 'spasibo')
            self.assertEqual(no_fee_row.payout_ready_dt, None)

        # c payout_ready_dt
        for payment in payment_method_payments[TEST_CARD_PAYMENT_METHOD]:
            payment.payout_ready_dt = payout_ready_dt
            self.session.flush()
            total_sum = sum(amount for amount in orders_amounts_data[TEST_CARD_PAYMENT_METHOD].values())
            res = self.process(payment)
            self.assertEqual(len(res), 2)

            fee_row, = filter(lambda r: r.internal == 1, res)
            row_amount = orders_amounts_data[TEST_CARD_PAYMENT_METHOD][order_fee]
            row_amount_wo_nds = row_amount / mapper.Nds.get_nds_koef_on_dt(self.session, payment.payment_dt)
            self.assertEqual(fee_row.service_id, ServiceId.BLUE_PAYMENTS)
            self.assertEqual(fee_row.total_sum, total_sum)
            self.assertEqual(fee_row.amount, row_amount)
            self.assertEqual(fee_row.yandex_reward, row_amount)
            self.assertEqual(fee_row.yandex_reward_wo_nds, row_amount_wo_nds)
            self.assertEqual(fee_row.amount_fee, None)
            self.assertEqual(fee_row.invoice_eid, None)
            self.assertEqual(fee_row.contract_id, tech_contract.id)
            self.assertEqual(fee_row.partner, tech_contract.client)
            self.assertEqual(fee_row.paysys_type_cc, 'yamoney')
            self.assertEqual(fee_row.payment_type, 'card')
            self.assertEqual(fee_row.payout_ready_dt, payout_ready_dt)

            no_fee_row, = filter(lambda r: r.internal is None, res)
            agent_reward_pa, = filter(lambda i: i.service_code == 'AGENT_REWARD', contract_payments.invoices)
            row_amount = orders_amounts_data[TEST_CARD_PAYMENT_METHOD][order_no_fee]
            self.assertEqual(no_fee_row.service_id, ServiceId.BLUE_PAYMENTS)
            self.assertEqual(no_fee_row.total_sum, total_sum)
            self.assertEqual(no_fee_row.amount, row_amount)
            self.assertEqual(no_fee_row.yandex_reward, 0)  # No developers payload and min commission in contract
            self.assertEqual(no_fee_row.yandex_reward_wo_nds, 0)
            self.assertEqual(no_fee_row.amount_fee, None)
            self.assertEqual(no_fee_row.invoice_eid, agent_reward_pa.external_id)
            self.assertEqual(no_fee_row.contract_id, contract_payments.id)
            self.assertEqual(no_fee_row.partner, contract_payments.client)
            self.assertEqual(no_fee_row.paysys_type_cc, 'yamoney')
            self.assertEqual(no_fee_row.payment_type, 'card')
            self.assertEqual(no_fee_row.payout_ready_dt, payout_ready_dt)

        for payment in payment_method_payments[SPASIBO_PAYMENT_METHOD]:
            payment.payout_ready_dt = payout_ready_dt
            self.session.flush()
            total_sum = sum(amount for amount in orders_amounts_data[SPASIBO_PAYMENT_METHOD].values())
            res = self.process(payment)
            self.assertEqual(len(res), 2)

            fee_row, = filter(lambda r: r.internal == 1, res)
            row_amount = orders_amounts_data[SPASIBO_PAYMENT_METHOD][order_fee]
            row_amount_wo_nds = row_amount / mapper.Nds.get_nds_koef_on_dt(self.session, payment.payment_dt)
            self.assertEqual(fee_row.service_id, ServiceId.BLUE_PAYMENTS)
            self.assertEqual(fee_row.total_sum, total_sum)
            self.assertEqual(fee_row.amount, row_amount)
            self.assertEqual(fee_row.yandex_reward, row_amount)
            self.assertEqual(fee_row.yandex_reward_wo_nds, row_amount_wo_nds)
            self.assertEqual(fee_row.amount_fee, None)
            self.assertEqual(fee_row.invoice_eid, None)
            self.assertEqual(fee_row.contract_id, tech_contract.id)
            self.assertEqual(fee_row.partner, tech_contract.client)
            self.assertEqual(fee_row.paysys_type_cc, 'yamoney')
            self.assertEqual(fee_row.payment_type, 'spasibo')
            self.assertEqual(fee_row.payout_ready_dt, payout_ready_dt)

            no_fee_row, = filter(lambda r: r.internal is None, res)
            row_amount = orders_amounts_data[SPASIBO_PAYMENT_METHOD][order_no_fee]
            self.assertEqual(no_fee_row.service_id, ServiceId.BLUE_SUB)
            self.assertEqual(no_fee_row.total_sum, total_sum)
            self.assertEqual(no_fee_row.amount, row_amount)
            self.assertEqual(no_fee_row.yandex_reward, None)
            self.assertEqual(no_fee_row.yandex_reward_wo_nds, None)
            self.assertEqual(no_fee_row.amount_fee, None)
            self.assertEqual(no_fee_row.contract_id, contract_subs.id)
            self.assertEqual(no_fee_row.partner, contract_subs.client)
            self.assertEqual(no_fee_row.paysys_type_cc, 'spasibo')
            self.assertEqual(no_fee_row.payment_type, 'spasibo')
            self.assertEqual(no_fee_row.payout_ready_dt, payout_ready_dt)

    def test_refund(self):
        raise pytest.skip(u"На момент разработки платежей спасибо даже Траст не знает, как будет выглядеть рефанд.")


class TestViaYandexUnit(TestThirdpartyTransactions):
    pipeline = tt.ViaYandexUnit()

    def test_payment_pass(self):
        """Test TrustPayment via yandex pass"""
        payment = self.create_trust_payment()
        self._test_equal(payment, {})

    def test_payment_fail(self):
        """Test TrustPayment via yandex fail"""
        payment = self.create_trust_payment(service_id=124, ym_shop_id=NOT_YANDEX_YM_SHOP_ID)
        with pytest.raises(tt.Skip):
            self._test_equal(payment, {})

    def test_refund_pass(self):
        """Test Refund via yandex pass"""
        payment = self.create_trust_payment()
        refund = self.create_refund(payment)
        self._test_equal(payment, {})
        self._test_equal(refund, {})

    def test_refund_fail(self):
        """Test Refund via yandex fail"""
        payment = self.create_trust_payment(service_id=124, ym_shop_id=NOT_YANDEX_YM_SHOP_ID)
        refund = self.create_refund(payment)
        with pytest.raises(tt.Skip):
            self._test_equal(payment, {})

        with pytest.raises(tt.Skip):
            self._test_equal(refund, {})


def test_pipeline_integrity():
    """Test ProcessingUnit.requires satisfaction for pipeline"""
    tt.TransactionProcessor.check_pipeline()


class TestRewardUnit(TestThirdpartyTransactions):
    pipeline = tt.ChainUnit([tt.PartnerUnit(), tt.ContractUnit(), tt.AmountUnit(), tt.OrderRewardUnit()])

    def test_minimal_payment_commission_reward(self):
        """Test reward from contract attribute MINIMAL_PAYMENT_COMMISSION"""

        contract = self.create_contract(
            MINIMAL_PAYMENT_COMMISSION=MINIMAL_PAYMENT_COMMISSION_VALUE)

        payment = self.create_trust_payment(contract=contract)
        expect = {'yandex_reward': MINIMAL_PAYMENT_COMMISSION_VALUE}
        self._test_equal(payment, expect)

    def test_order_commission(self):
        """Test reward from order.commission_category (test 10%) """

        TEST_COMM_CATEGORY = '1000'

        request = self.create_request(self.sid)
        for i in range(len(request.request_orders)):
            request.request_orders[i].order.commission_category = TEST_COMM_CATEGORY

        payment = self.create_trust_payment(request=request)
        expect = {'yandex_reward': D('10')}
        self._test_equal(payment, expect)

        request = self.create_request(self.sid)
        for i in range(len(request.request_orders)):
            request.request_orders[i].order.commission_category = TEST_COMM_CATEGORY

        refund = self.create_refund(payment, request=request)
        expect = {'yandex_reward': D('10')}
        self._test_equal(refund, expect)


class TestRewardUnitAddNDS(TestThirdpartyTransactions):
    pipeline = tt.ChainUnit(
        [tt.PartnerUnit(),
         tt.ContractUnit(),
         tt.AmountUnit(),
         tt.RewardUnit(reward_is_amount=True,
                       add_nds_firms={
                           FirmId.YANDEX_OOO: [
                               {
                                   'add_nds': 1,
                                   'start_dt': datetime.datetime.now() - datetime.timedelta(days=1),
                               },
                               {
                                   'add_nds': 0,
                                   'start_dt': datetime.datetime.now() + datetime.timedelta(days=3)
                               }
                           ]
                       }
                       )])

    def test_add_nds_firms(self):
        """Проверка накидывния НДС"""

        # по умолчанию НДС не накидывается
        # дополнительно проверяется округление
        payment = self.create_trust_payment(amount=D('100.333'),
                                            payment_dt=datetime.datetime.now() - datetime.timedelta(days=2))
        self._test_equal(payment, {'yandex_reward': D('100.33')})

        # с какого-то момента накидывается
        payment = self.create_trust_payment(amount=D('99.77'),
                                            payment_dt=datetime.datetime.now())
        reward_with_nds = ut.round00(D('99.77') * mapper.Nds.get_nds_koef_on_dt(self.session, payment.payment_dt))
        self._test_equal(payment, {'yandex_reward': reward_with_nds})

        # потом снова не накидывается
        payment = self.create_trust_payment(amount=D('100'),
                                            payment_dt=datetime.datetime.now() + datetime.timedelta(days=4))
        self._test_equal(payment, {'yandex_reward': D('100')})


class TestRewardUnitAddNDSMinimalCommission(TestThirdpartyTransactions):
    pipeline = tt.ChainUnit(
        [tt.PartnerUnit(),
         tt.ContractUnit(),
         tt.AmountUnit(),
         tt.RewardUnit(reward_is_amount=True,
                       use_min_reward=True,
                       min_reward_value=D('99.77'),
                       add_nds_firms={
                           FirmId.YANDEX_OOO: [
                               {
                                   'add_nds': 1,
                                   'start_dt': datetime.datetime.now(),
                               },
                           ]
                       }
                       )])

    def test_minimal_commission_nds(self):
        """Считаем что есть или нет НДС в минималке зависит от опции
        есть НДС в сумме платежа или нет.
        """

        # НДС на минималку не накидывается
        payment = self.create_trust_payment(amount=D('1'),
                                            payment_dt=datetime.datetime.now() - datetime.timedelta(days=5))
        self._test_equal(payment, {'yandex_reward': D('99.77')})

        # НДС на минималку накидывается
        payment_with_added_nds = self.create_trust_payment(amount=D('1'),
                                                           payment_dt=datetime.datetime.now())
        minimal_reward_with_added_nds = ut.round00(
            D('99.77') * mapper.Nds.get_nds_koef_on_dt(self.session, payment_with_added_nds.payment_dt))
        self._test_equal(payment_with_added_nds, {'yandex_reward': minimal_reward_with_added_nds})


class TestTaxiRewardUnit(TestThirdpartyTransactions):
    pipeline = tt.ChainUnit([tt.PartnerUnit(), tt.ContractUnit(
        # configuration for TAXI_PAYMENT
        contract_filters=[
            {'filter': {'name': 'service'}},
            {'filter': {'name': 'partner'}},
            {'filter': {'name': 'transaction_or_side_payment_dt'}},
            {'filter': {'name': 'contract_type', 'params': {'contract_type': 'GENERAL'}}},
        ],
        multicurrency=True
    ), tt.TaxiAmountUnit(), tt.TaxiRewardUnit()])

    def test_compute_reward_taxi_min_commission(self):
        """Test taxi minimal commission (hardcoded 0.01%)"""

        TEST_AMOUNT = D('10000')

        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_PAYMENT)
        contract = self.create_contract(service_id=ServiceId.TAXI_PAYMENT)
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)

        payment = self.create_trust_payment(amount=TEST_AMOUNT, service_id=ServiceId.TAXI_PAYMENT,
                                            thirdparty_service=thirdparty_service, contract=contract)
        expect = {'yandex_reward': TEST_AMOUNT * TAXI_REWARD_PCT * D('0.01')}
        self._test_equal(payment, expect)

        # refund = self.create_refund(payment, amount=TEST_AMOUNT)
        # expect = {'yandex_reward': TEST_AMOUNT * D('0.01') * D('0.01')}
        # self._test_equal(refund, expect)


class TestReferenceCurrencyAmount(TestThirdpartyTransactions):
    pipeline = tt.ChainUnit([tt.PartnerUnit(), tt.ContractUnit(),
                             tt.ReferenceCurrencyAmount()])

    def test_reference_amount(self):
        """Test ReferenceAmount unit"""

        TEST_AMOUNT = D('10000')

        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_PAYMENT)
        contract = self.create_contract(service_id=ServiceId.TAXI_PAYMENT,
                                        country=RegionId.BELARUS)
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        payment = self.create_trust_payment(amount=TEST_AMOUNT,
                                            service_id=ServiceId.TAXI_PAYMENT,
                                            thirdparty_service=thirdparty_service,
                                            contract=contract)
        rate = mapper.CurrencyRate.get_real_currency_rate_by_date(
            self.session,
            'RUB',
            payment.dt,
            base_cc='BYN').rate

        expect = {
            'reference_amount': TEST_AMOUNT * rate,
            'reference_currency': 'BYN'
        }
        self._test_equal(payment, expect)

        refund = self.create_refund(payment, amount=TEST_AMOUNT)
        expect = {
            'reference_amount': TEST_AMOUNT * rate,
            'reference_currency': 'BYN'
        }
        self._test_equal(refund, expect)


class TestTaxiReferenceCurrencyAmount(TestThirdpartyTransactions):
    pipeline = tt.ChainUnit([tt.PartnerUnit(), tt.ContractUnit(),
                             tt.TaxiReferenceCurrencyAmount()])

    def test_taxi_reference_amount(self):
        """Test ReferenceAmount unit"""

        TEST_AMOUNT = D('10000')
        # select min(rate_dt) from bo.t_currency_rate_v2 where base_cc = 'BYN' and rate_src_id = 1006;
        dt = datetime.datetime(2016, 8, 18)

        thirdparty_service = self.create_thirdparty_service(service_id=ServiceId.TAXI_PAYMENT)
        contract = self.create_contract(dt=dt - datetime.timedelta(days=30),
                                        service_id=ServiceId.TAXI_PAYMENT,
                                        country=RegionId.BELARUS)
        self.create_taxi_stat(contract, TAXI_CARD_PAYMENT_TYPE)
        payment = self.create_trust_payment(amount=TEST_AMOUNT,
                                            service_id=ServiceId.TAXI_PAYMENT,
                                            thirdparty_service=thirdparty_service,
                                            contract=contract,
                                            start_dt=dt)
        rate = mapper.CurrencyRate.get_real_currency_rate_by_date(
            self.session, 'RUB', dt, base_cc='BYN', rate_src_id=1006).rate
        rate_on_transaction_dt = mapper.CurrencyRate.get_real_currency_rate_by_date(
            self.session, 'RUB', payment.dt, base_cc='BYN', rate_src_id=1006).rate

        assert rate != rate_on_transaction_dt

        expect = {
            'reference_amount': ut.round00(TEST_AMOUNT * rate),
            'reference_currency': 'BYN'
        }
        self._test_equal(payment, expect)

        refund = self.create_refund(payment, amount=TEST_AMOUNT)
        expect = {
            'reference_amount': ut.round00(TEST_AMOUNT * rate),
            'reference_currency': 'BYN'
        }
        self._test_equal(refund, expect)


class TestMarketPlaceNew(TestThirdpartyTransactions):
    _service_id = ServiceId.MARKETPLACE_NEW

    # простой предоплатный договор с плательщиком-юриком
    def _create_contract(self):
        person = PersonBuilder(type='ur').build(self.session).get_obj()
        client = person.client

        with self.session.begin():
            contract = mapper.Contract(ContractTypes(type='GENERAL'))
            self.session.add(contract)

            contract.client = client
            contract.person = person

            col0 = contract.col0
            col0.commission = ContractTypeId.OFFER
            col0.currency = 810
            col0.fake_id = 0
            col0.firm = FirmId.MARKET
            col0.partner_commission_pct = 5
            col0.payment_type = PREPAY_PAYMENT_TYPE
            col0.services = {self._service_id: 1}
            col0.unilateral = 1

            dt = ut.trunc_date(self.session.now() - datetime.timedelta(days=2))
            col0.dt = dt
            col0.is_signed = dt

            col0.maybe_create_barcode()
            contract.external_id = contract.create_new_eid()

        return contract

    def test_payment_with_payout(self):
        contract = self._create_contract()
        thirdparty_service = self.create_thirdparty_service(self._service_id)
        payout_ready_dt = DT + datetime.timedelta(days=10)

        payment = self.create_trust_payment(amount=AMOUNT, contract=contract, payout_ready_dt=payout_ready_dt,
                                            service_id=self._service_id, thirdparty_service=thirdparty_service)
        processor = tt.TransactionProcessor(payment)

        # платёж должен успешно разобраться, у строчек теперь должна появиться дата выплаты
        result_rows = processor.process()
        for row in result_rows:
            self.assertEqual(row.payout_ready_dt, payout_ready_dt)

    def test_payment_wo_payout(self):
        contract = self._create_contract()
        thirdparty_service = self.create_thirdparty_service(self._service_id)

        payment = self.create_trust_payment(amount=AMOUNT, contract=contract,
                                            service_id=self._service_id,
                                            thirdparty_service=thirdparty_service)
        processor = tt.TransactionProcessor(payment)

        # платёж не должен разобраться, должно быть возбуждено исключение Delay
        self.assertRaises(tt.Delay, processor.process)

    def test_payment_compensation(self):
        contract = self._create_contract()
        thirdparty_service = self.create_thirdparty_service(self._service_id)

        # создаём платёж-компенсацию, без даты выплаты
        payment = self.create_trust_payment(amount=AMOUNT, contract=contract,
                                            service_id=self._service_id, thirdparty_service=thirdparty_service,
                                            payment_method='compensation')
        processor = tt.TransactionProcessor(payment)

        # платёж должен успешно разобраться без даты выплаты
        result_rows = processor.process()
        for row in result_rows:
            assert row.payout_ready_dt is None


class TestSidePaymentMixIn(object):
    sid = NotImplemented

    def create_side_payment(self,
                            contract=None,
                            price=None,
                            payment_type=None,
                            currency='USD',
                            transaction_type='payment',
                            **kwargs):
        contract = contract or self.create_contract()
        service_id = self.sid
        if kwargs.get('service_id'):
            service_id = kwargs['service_id']
            del kwargs['service_id']

        return ob.SidePaymentBuilder(
            service_id=service_id,
            transaction_id=ob.generate_int(23),
            price=price,
            transaction_type=transaction_type,
            client_id=contract.client_id,
            payment_type=payment_type,
            currency=currency,
            **kwargs
        ).build(self.session).obj

    def create_side_refund(self, orig_transaction_id, **kwargs):
        kwargs['transaction_type'] = 'refund'
        kwargs['orig_transaction_id'] = orig_transaction_id
        return self.create_side_payment(**kwargs)


class TestTravel(TestSidePaymentMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TRAVEL
    TEST_AMOUNT = D("666.67")
    common_params = {
        'currency': 'USD',
        'dt': datetime.datetime.now() + datetime.timedelta(days=1),
    }
    expected_params = {
        'transaction_type': 'payment',
        'service_id': sid,
        'commission_currency': 'USD',
        'commission_iso_currency': 'USD',
        'currency': 'USD',
        'iso_currency': 'USD',
        'paysys_type_cc': 'yamoney',
        'payment_type': 'reward',
        'transaction_dt': common_params['dt'],
        'total_sum': TEST_AMOUNT,
        'amount': TEST_AMOUNT,
        'yandex_reward': TEST_AMOUNT,
        'partner_currency': 'USD',
        'partner_iso_currency': 'USD',
        'oebs_org_id': 121,
    }

    def test_payment_reward(self):
        contract = self.create_contract(service_id=self.sid, currency=840)
        self.create_thirdparty_service(self.sid)
        payment = self.create_side_payment(
            contract=contract,
            price=self.TEST_AMOUNT,
            payment_type='reward',
            **self.common_params
        )

        expect = copy.deepcopy(self.expected_params)
        expect.update({
            'contract_id': contract.id,
            'person_id': contract.person.id,
        })
        self._test_equal(payment, expect)

    def test_payment_cost(self):
        contract = self.create_contract(service_id=self.sid, currency=840)
        self.create_thirdparty_service(self.sid)
        payment = self.create_side_payment(
            contract=contract,
            price=self.TEST_AMOUNT,
            payment_type='cost',
            **self.common_params
        )

        expect = copy.deepcopy(self.expected_params)
        expect.update({
            'contract_id': contract.id,
            'person_id': contract.person.id,
            'yandex_reward': None,
            'payment_type': 'cost',
        })
        self._test_equal(payment, expect)

    def test_refund_reward(self):
        contract = self.create_contract(service_id=self.sid, currency=840)
        self.create_thirdparty_service(self.sid)
        payment = self.create_side_payment(
            contract=contract,
            price=self.TEST_AMOUNT,
            payment_type='reward',
            **self.common_params
        )

        refund = self.create_side_refund(
            payment.transaction_id,
            contract=contract,
            price=self.TEST_AMOUNT,
            payment_type='reward',
            **self.common_params
        )
        expect = copy.deepcopy(self.expected_params)
        expect.update({
            'contract_id': contract.id,
            'person_id': contract.person.id,
            'total_sum': self.TEST_AMOUNT,
            'transaction_type': 'refund',
        })

        self._test_equal(refund, expect)

    def test_refund_cost(self):
        contract = self.create_contract(service_id=self.sid, currency=840)
        self.create_thirdparty_service(self.sid)
        payment = self.create_side_payment(
            contract=contract,
            price=self.TEST_AMOUNT,
            payment_type='cost',
            **self.common_params
        )

        self.create_side_refund(
            payment.transaction_id,
            contract=contract,
            price=self.TEST_AMOUNT,
            payment_type='cost',
            **self.common_params
        )

        expect = copy.deepcopy(self.expected_params)
        expect.update({
            'contract_id': contract.id,
            'person_id': contract.person.id,
            'total_sum': -self.TEST_AMOUNT,
            'transaction_type': 'refund',
            'yandex_reward': None,
            'payment_type': 'cost',
        })


class TestTLogTaxiSubventions(TestThirdpartyTransactions):
    sid = ServiceId.TAXI_PROMO
    spendable_contract_type = SPENDABLE_PROMO_TYPE

    def create_payment(self, **kwargs):
        self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, ctype='SPENDABLE', firm=FirmId.TAXI,
                                        contract_type=self.spendable_contract_type, currency=643)
        link_contract = self.create_contract(ctype='GENERAL', person=contract.person, firm=FirmId.TAXI)
        contract.col0.link_contract_id = link_contract.id
        self.session.flush()
        payment = self._create_side_payment(service_id=self.sid, contract=contract, **kwargs)
        return {'payment': payment, 'contract': contract}

    def _create_side_payment(self, service_id=None, contract=None, **kwargs):
        transaction_id = self.session.query(sa.func.nvl(sa.func.max(mapper.SidePayment.id), 0)) \
                             .filter(mapper.SidePayment.service_id == service_id).scalar() + 10000

        payment = mapper.SidePayment(transaction_id=transaction_id,
                                     price=AMOUNT,
                                     dt=PAYMENT_DT,
                                     client_id=contract.client.id,
                                     currency=contract.current_signed().get_currency().iso_code,
                                     transaction_type='payment',
                                     service_id=service_id,
                                     transaction_dt=self.session.now(),  # transaction_time
                                     extra_dt_0=self.session.now(),  # transaction_time
                                     extra_num_0=None,  # orig_transaction_id
                                     extra_str_0='0481bd733e9911e999728c85905b3985',  # service_transaction_id
                                     payment_type='subsidy',
                                     extra_str_1=None,  # payload
                                     )
        self.session.add(payment)
        self.session.flush()
        return payment

    def create_refund(self, **kwargs):
        self.create_thirdparty_service(service_id=self.sid)
        contract = self.create_contract(service_id=self.sid, ctype='SPENDABLE', firm=FirmId.TAXI,
                                        contract_type=self.spendable_contract_type, currency=643)
        link_contract = self.create_contract(ctype='GENERAL', person=contract.person, firm=FirmId.TAXI)
        contract.col0.link_contract_id = link_contract.id
        self.session.flush()
        payment = self._create_side_refund(service_id=self.sid, contract=contract, **kwargs)
        return {
            'payment': payment,
            'contract': contract
        }

    def _create_side_refund(self, service_id=None, contract=None, **kwargs):
        transaction_id = self.session.query(sa.func.nvl(sa.func.max(mapper.SidePayment.id), 0)) \
                             .filter(mapper.SidePayment.service_id == service_id).scalar() + 10000

        refund = mapper.SidePayment(transaction_id=transaction_id,
                                    price=AMOUNT,
                                    dt=PAYMENT_DT,
                                    client_id=contract.client.id,
                                    currency=contract.current_signed().get_currency().iso_code,
                                    transaction_type='refund',
                                    service_id=service_id,
                                    transaction_dt=self.session.now(),  # transaction_time
                                    extra_dt_0=self.session.now(),  # transaction_time
                                    orig_transaction_id=transaction_id - 1,  # orig_transaction_id
                                    extra_str_0='0481bd733e9911e999728c85905b3986',  # service_transaction_id
                                    payment_type='coupon',
                                    extra_str_1=None,  # payload
                                    )
        self.session.add(refund)
        self.session.flush()
        return refund

    def test_refund(self):
        info = self.create_refund()
        res = self.process(info['payment'])
        self.check(info, res)
        self.additional_check(info, res)

    def test_payment(self):
        info = self.create_payment()
        res = self.process(info['payment'])
        self.check(info, res)
        self.additional_check(info, res)

    def process(self, transaction):
        with transaction.session.begin():
            return tt.TransactionProcessor(transaction).process()

    def check(self, info, res):
        contract = info['contract']
        char_code = contract.current_signed().get_currency().char_code
        iso_code = contract.current_signed().get_currency().iso_code
        transaction = info['payment']
        tt, = res
        self.assertEqual(D(tt.amount), transaction.price)
        self.assertEqual(tt.commission_currency, char_code)
        self.assertEqual(tt.commission_iso_currency, iso_code)
        self.assertEqual(tt.contract_id, contract.id)
        self.assertEqual(tt.currency, char_code)
        self.assertEqual(tt.dt, transaction.dt)
        self.assertEqual(tt.iso_currency, iso_code)
        self.assertEqual(tt.oebs_org_id, contract.firm.firm_exports['OEBS'].oebs_org_id)
        self.assertEqual(tt.partner.id, transaction.client_id)
        self.assertEqual(tt.partner_currency, char_code)
        self.assertEqual(tt.partner_iso_currency, iso_code)
        self.assertEqual(tt.payment_id, transaction.id)
        self.assertEqual(tt.payment_type, transaction.payment_type)
        self.assertEqual(tt.paysys_type_cc, 'yataxi')
        self.assertEqual(tt.person_id, contract.person_id)
        self.assertEqual(tt.service_id, transaction.service_id)
        self.assertEqual(tt.service_order_id_str, transaction.extra_str_0)
        self.assertEqual(tt.transaction_dt, transaction.dt)
        self.assertEqual(tt.transaction_type, transaction.transaction_type)
        self.assertEqual(tt.trust_id, transaction.transaction_id)
        self.assertEqual(tt.trust_payment_id,
                         transaction.transaction_id if transaction.transaction_type == 'payment'
                         else transaction.orig_transaction_id)
        self.assertEqual(tt.yandex_reward, None)

    def additional_check(self, info, res):
        pass


class TestBenzaki(TestSidePaymentMixIn, TestThirdpartyTransactions):
    sid = ServiceId.DRIVE_REFUELLER_SPENDABLE
    TEST_AMOUNT = D("666.67")
    common_params = {
        'currency': 'RUB',
        'dt': datetime.datetime.now() + datetime.timedelta(days=1),
    }
    spendable_expected_params = {
        'transaction_type': 'payment',
        'service_id': sid,
        'currency': 'RUR',
        'iso_currency': 'RUB',
        'paysys_type_cc': 'yadrive',
        'payment_type': 'drive_fueler',
        'transaction_dt': common_params['dt'],
        'total_sum': TEST_AMOUNT,
        'amount': TEST_AMOUNT,
        'partner_currency': 'RUR',
        'partner_iso_currency': 'RUB',
        'oebs_org_id': 118724,
    }

    penalty_expected_params = {
        'transaction_type': 'refund',
        'service_id': ServiceId.DRIVE_REFUELLER_PENALTY,
        'currency': 'RUR',
        'iso_currency': 'RUB',
        'paysys_type_cc': 'extra_profit',
        'payment_type': 'correction_commission',
        'transaction_dt': common_params['dt'],
        'total_sum': TEST_AMOUNT,
        'amount': TEST_AMOUNT,
        'partner_currency': 'RUR',
        'partner_iso_currency': 'RUB',
        'oebs_org_id': 118724,
    }

    def test_payment_payment(self):
        penalty_params = {'personal_account': 1, 'ctype': 'GENERAL'}
        penalty_contract = self.create_contract(service_id=ServiceId.DRIVE_REFUELLER_PENALTY,
                                                currency=810, firm=FirmId.DRIVE,
                                                payment_type=2,
                                                **penalty_params)

        def get_paysys(contract, service_id, cert=0, nds=None):
            return contract.session.query(mapper.Paysys).filter_by(id=3001117).one()

        import balance.reverse_partners as reverse_partners
        reverse_partners.get_paysys = get_paysys

        cp = contractpage.ContractPage(self.session, penalty_contract.id)
        cp.create_personal_accounts()

        spendable_params = {'personal_account': 1,
                            'ctype': 'SPENDABLE',
                            'link_contract_id': penalty_contract.id,
                            }
        spendable_contract = self.create_contract(service_id=self.sid, currency=643, firm=FirmId.DRIVE,
                                                  client_id=penalty_contract.client_id,
                                                  **spendable_params)

        self.create_thirdparty_service(self.sid)
        self.create_thirdparty_service(ServiceId.DRIVE_REFUELLER_PENALTY)
        payment = self.create_side_payment(
            contract=spendable_contract,
            price=self.TEST_AMOUNT,
            payment_type='drive_fueler',
            transaction_type='payment',
            **self.common_params
        )

        expect = copy.deepcopy(self.spendable_expected_params)
        expect.update({
            'contract_id': spendable_contract.id,
            'person_id': spendable_contract.person.id,
            'invoice_eid': None,
            'yandex_reward': None
        })

        self._test_equal(payment, expect)

        penalty = self.create_side_payment(
            contract=spendable_contract,
            price=self.TEST_AMOUNT,
            payment_type='card',
            service_id=ServiceId.DRIVE_REFUELLER_PENALTY,
            **self.common_params
        )

        expect = copy.deepcopy(self.penalty_expected_params)
        expect.update({
            'contract_id': spendable_contract.id,
            'person_id': spendable_contract.person.id,
            'invoice_eid': penalty_contract.invoices[0].external_id,
            'yandex_reward': None
        })

        self._test_equal(penalty, expect)


class TestFood(TestThirdpartyTransactions):
    food_services = {
        ServiceId.FOOD_SRV,
        ServiceId.FOOD_PAYMENT,
        ServiceId.FOOD_COURIERS_PAYMENT,
        ServiceId.FOOD_COURIERS_SPENDABLE,
        ServiceId.FOOD_PICKERS_PAYMENT,
        ServiceId.FOOD_PICKERS_BUILD_ORDER,
        ServiceId.REST_SITES_PAYMENT,
    }

    @pytest.mark.parametrize("service_id", food_services, ids=lambda sid: "service_id=%s" % sid)
    def test_skip_trust_payment(self, service_id):
        """
        BALANCE-31697, BALANCE-31793
        Процессор транзакций должен не брать в обработку трастовые платежи с сервисами Еды
        """
        payment = self.create_food_trust_payment(service_id)
        with pytest.raises(tt.Skip, match=r"service id \d+"):
            tt.TransactionProcessor(payment).process()

    @pytest.mark.parametrize("service_id", food_services, ids=lambda sid: "service_id=%s" % sid)
    def test_skip_refund(self, service_id):
        """
        https://st.yandex-team.ru/BALANCE-31889#5d2c439c701665001c53a151
        """
        refund = self.create_food_refund(service_id)
        with pytest.raises(tt.Skip, match=r"service id \d+"):
            tt.TransactionProcessor(refund).process()

    def create_food_trust_payment(self, service_id, amount=D("3.14")):
        payment = TrustPayment(None)
        payment.trust_payment_id = str(bson.ObjectId())
        payment.payment_method = DIRECT_CARD_PAYMENT_METHOD
        payment.amount = amount
        payment.service = self.get_service(service_id)
        payment.service_id = service_id
        payment.source_scheme = BS_SOURCE_SCHEME
        self.session.add(payment)
        return payment

    def create_food_refund(self, service_id, amount=D("6.28")):
        payment = self.create_food_trust_payment(service_id, amount)
        refund = Refund(payment, amount or payment.amount, None, None)
        refund.orig_payment = payment  # жестко выставляем orig_payment т.к связей через базу нет
        refund.service_id = service_id
        return refund


class TestPaysysTypeCCUnit(TestSidePaymentMixIn, TestThirdpartyTransactions):
    sid = ServiceId.TAXI_PAYMENT

    @pytest.mark.parametrize("config, payment_type, expected_paysys_type_cc", [
        ({
             "force_paysys_type_cc": "yazapravki",  # enforce paysys_type_cc for this service
         }, "yandex", "yazapravki"),
        ({
             "side_payment_default": "yazapravki",  # enforce paysys_type_cc for this service
         }, "yandex", "yazapravki"),
        ({
             "payment_type_map": {  # depends on row.payment_type
                 "yataxi": "taxi_stand_svo",
                 "zen_payment": {  # depends on row.payment.payment_type if row.payment_type == zen_payment
                     "transaction_payment_type_map": {
                         "wallet": "yamoney"
                     }
                 },
             },
             "default": "bank_credit",  # if default is not set will try to get global rules
         }, "yataxi", "taxi_stand_svo"),
        ({
             "payment_type_map": {  # depends on row.payment_type
                 "yataxi": "taxi_stand_svo",
                 "zen_payment": {  # depends on row.payment.payment_type if row.payment_type == zen_payment
                     "transaction_payment_type_map": {
                         "wallet": "yamoney",
                     }
                 },
             },
             "default": "bank_credit",  # if default is not set will try to get global rules
         }, "yandex", "bank_credit"),
        ({
             "account_payment": {  # must be one of PLUS_WALLET_PAYMENT_METHODS
                 "yandex_account_topup": "yazapravki",
                 "yandex_account_withdraw": "yazapravki"
             },
         }, "yandex_account_topup", "yazapravki"),
        ({
             "payment_type_map": {  # depends on row.payment_type
                 "yataxi": "taxi_stand_svo",
                 "zen_payment": {  # depends on row.payment.payment_type if row.payment_type == zen_payment
                     "transaction_payment_type_map": {
                         "wallet": "yamoney",
                     }
                 },
             },
         }, "yandex", None),
    ])
    def test_paysys_type_cc_unit(self, config, payment_type, expected_paysys_type_cc, monkeypatch, contract):
        payment = self.create_side_payment(
            contract=contract,
            price=100,
            payment_type=payment_type,
            currency='USD',
            dt=datetime.datetime.now() + datetime.timedelta(days=1),
        )

        def patch(self, incoming_row, result_row):
            paysys_type_cc = self.choose_paysys_type_cc(incoming_row)
            assert paysys_type_cc == expected_paysys_type_cc, "different paysys_type_cc %s - %s" % (
                paysys_type_cc, expected_paysys_type_cc)
            return result_row

        monkeypatch.setattr(tt.PaysysTypeCCUnit, 'process_row', patch)
        monkeypatch.setattr(tt.TransactionProcessor, 'get_pipeline', lambda self, row: tt.PaysysTypeCCUnit(config))

        tt.TransactionProcessor(payment).process()

    @pytest.fixture
    def contract(self):
        contract = self.create_contract(service_id=self.sid, currency=840)
        tpt_service = self.create_thirdparty_service(self.sid)
        yield contract

    def test_tpt_paysys_type_cc_config(self, contract, monkeypatch):
        payment = self.create_side_payment(
            contract=contract,
            price=100,
            currency='USD',
            dt=datetime.datetime.now() + datetime.timedelta(days=1),
        )
        expected_config = {"compensation": "yandex", "new_promocode": "yandex"}
        paysys_type_cc_unit = tt.PaysysTypeCCUnit()
        side_row_wrap = tt.SidePaymentWrap(payment)
        monkeypatch.setattr(tt.TransactionProcessor, 'get_pipeline', lambda self, row: paysys_type_cc_unit)
        monkeypatch.setattr(tt, 'transaction_factory', lambda transaction, process_modification=True: side_row_wrap)

        # assert correct config retrieved
        with mock.patch('balance.thirdparty_transaction.PaysysTypeCCUnit._payment_type_cc_from_config') as mock_payment_type_cc_from_config:
            tt.TransactionProcessor(payment).process()
            mock_payment_type_cc_from_config.assert_called_once_with(side_row_wrap.rows[0], payment_type_map=expected_config)

    def test_all_pipelines_have_unit(self, monkeypatch):
        has_paysys_type_cc_unit = set()

        @classmethod
        def custom_check(cls, node, stack, ord):
            if isinstance(node, tt.PaysysTypeCCUnit):
                has_paysys_type_cc_unit.add(stack[0][0])

        monkeypatch.setattr(tt.TransactionProcessor, 'custom_check', custom_check)

        for _ in tt.TransactionProcessor.check_node(tt.transaction_pipeline, []):
            pass
        assert len(set(tt.transaction_pipeline.choice_map.keys()).difference(
            has_paysys_type_cc_unit)) == 0, 'Not all integrations have PaysysTypeCCUnit'
