# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime
import sqlalchemy as sa
from datetime import timedelta

import balance.actions.acts as a_a
from balance import constants as const
from balance import contractpage
from balance import mapper
from balance import reverse_partners as rp
from balance.constants import *
from balance.providers.personal_acc_manager import PersonalAccountManager
from butils import logger
from tests import object_builder as ob
from tests.base import BalanceTest


log = logger.get_logger('test_revpartners')


# exists for compatibility with tests/balance_tests/test_partner_balance.py
# main logic migrated to pytest at tests/balance_tests/rev_partners/
class ReversePartnersBase(BalanceTest):
    def check_qty(self, act, completions):
        for r in act.rows:
            expect_qty = [x[1] for x in completions if r.order.product.id in x][0]
            self.assertEqual(r.act_qty, expect_qty)

    def check_sum(self, act, completions, discount_pct=D(0), price=D(1)):
        for r in act.rows:
            expect_sum = \
            [(x[1] - x[1] * discount_pct * D('0.01')) * price for x in completions if r.order.product.id in x][0]
            self.assertEqual(r.act_sum, expect_sum)

    def _create_scale(self, scale_code, points, namespace='adfox', y_unit_id=None, x_unit_id=799):
        test_scale = mapper.StaircaseScale(namespace=namespace, code=scale_code, x_unit_id=x_unit_id,
                                           y_unit_id=y_unit_id)
        for (x, y) in points:
            self.session.add(
                mapper.ScalePoint(namespace=namespace, scale_code=scale_code,
                                  start_dt=datetime.datetime(2015, 1, 1), x=x, y=y)
            )
        self.session.merge(test_scale)
        self.session.flush()

    def gen_contract(self, postpay=False, personal_account=False, con_func=None,
                     begin_dt=datetime.datetime(2014, 1, 1), finish_dt=None, client=None):
        from billing.contract_iface import contract_meta
        contract = mapper.Contract(ctype=contract_meta.ContractTypes(type='GENERAL'))
        self.session.add(contract)
        contract.client = client or ob.ClientBuilder().build(self.session).obj
        contract.person = ob.PersonBuilder(client=contract.client, type='ur').build(self.session).obj
        contract.col0.dt = begin_dt
        contract.col0.finish_dt = finish_dt

        contract.col0.firm = 1
        contract.col0.manager_code = 1122
        contract.col0.commission = 0
        contract.col0.payment_type = POSTPAY_PAYMENT_TYPE if postpay else PREPAY_PAYMENT_TYPE
        contract.col0.personal_account = 1 if personal_account else 0
        contract.col0.currency = 810
        contract.external_id = contract.create_new_eid()
        if postpay:
            contract.col0.partner_credit = 1
        contract.col0.is_signed = begin_dt + timedelta(3)

        if con_func:
            con_func(contract)

        contract.external_id = contract.create_new_eid()

        cp = contractpage.ContractPage(self.session, contract.id)
        cp.create_personal_accounts()
        self.session.flush()

        return contract

    # создание данных tpts, так чтобы транзакции были как в отрезке закрытия, так и вне его
    @staticmethod
    def get_multiple_tpts_data(begin_dt, end_dt, act_dt):
        return [
            {'dt': begin_dt - timedelta(1), 'yandex_reward': 1},
            {'dt': begin_dt + timedelta(1), 'yandex_reward': 2},
            {'dt': act_dt - timedelta(1), 'yandex_reward': 4},
            {'dt': act_dt + timedelta(1), 'yandex_reward': 8},
            {'dt': end_dt - timedelta(1), 'yandex_reward': 16},
        ]

    @staticmethod
    def get_invoice_eid(contract, service_code):
        return PersonalAccountManager(contract.session) \
            .for_contract(contract) \
            .for_service_code(service_code) \
            .get(auto_create=False) \
            .external_id

    @staticmethod
    def gen_tpts_for_contract(contract, tpts=()):
        session = contract.session
        with session.begin():
            for item in tpts:
                invoice_eid = None
                if 'service_code' in item:
                    invoice_eid = ReversePartnersBase.get_invoice_eid(contract, item.get('service_code'))

                session.add(mapper.ThirdPartyTransaction(
                    id=sa.func.next_value(sa.Sequence('s_request_order_id')),
                    service_id=item.get('service_id'),
                    product_id=item.get('product_id'),
                    contract_id=contract.id,
                    invoice_eid=invoice_eid,
                    amount=item.get('amount', 100),
                    yandex_reward=item.get('yandex_reward', 10),
                    dt=item.get('dt', contract.col0.dt + timedelta(days=1)),
                    paysys_type_cc=item.get('paysys_type_cc'),
                    payment_type=item.get('payment_type'),
                    transaction_type=item.get('transaction_type', 'payment')))

    def generate_acts(self, contract, act_month, dps, invoices, with_begin_dt=False):
        return a_a.ActAccounter(contract.client, act_month, dps=dps,
                                invoices=invoices, force=1).do()

    def get_pa(self, contract, service_code=None):
        paysys = rp.get_paysys(contract, const.ServiceId.TAXI_CASH)
        return PersonalAccountManager(self.session) \
            .for_contract(contract) \
            .for_paysys(paysys) \
            .for_service_code(service_code).get(auto_create=False)

    def gen_acts(self, rpc):
        res = rpc.process_and_enqueue_act()
        self.assertEqual(len(res), 2)
        acts = self.generate_acts(rpc.contract, rpc.act_month, dps=res[0], invoices=res[1])
        return acts
