# -*- coding: utf-8 -*-

import datetime
import decimal
import itertools

import pytest
import hamcrest
import mock

from balance import mapper
from balance.providers import personal_acc_manager as pam
import balance.muzzle_util as ut
from balance.actions import consumption as a_c
from balance.constants import (
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
)

from tests import object_builder as ob
from tests.base_routine import consumes_match

from tests.balance_tests.act.generation.common import (
    calculate_consumption,
    generate_act,
    create_consume,
    consume_credit,
)

D = decimal.Decimal

PAST = datetime.datetime(2000, 1, 1)
PRESENT = ut.trunc_date(datetime.datetime.now())
NEAR_PAST = PRESENT - datetime.timedelta(3)
CUR_MONTH = mapper.ActMonth(for_month=PRESENT)


@pytest.mark.taxes_update
class TestOveractCompensation(object):

    @staticmethod
    def consumes_match(consumes_state):
        return consumes_match(
            consumes_state,
            forced_params=[
                'current_qty',
                'current_sum',
                'completion_qty',
                'completion_sum',
                'act_qty',
                'act_sum',
                'tax_policy_pct_id',
            ]
        )

    @staticmethod
    def _create_order(session, client, product=DIRECT_PRODUCT_ID):
        return ob.OrderBuilder(
            product=ob.Getter(mapper.Product, product) if isinstance(product, int) else product,
            client=client, agency=client.agency,
        ).build(session).obj

    @staticmethod
    def _create_product_w_taxes(session, tax_policy):
        tpp1, tpp2 = tax_policy.taxes
        return ob.ProductBuilder(
            taxes=tax_policy,
            prices=[
                (PAST, 'RUR', 30, tpp1),
                (PRESENT, 'RUR', 30, tpp2),
            ]
        ).build(session).obj

    @pytest.fixture
    def tax_policy(self, session):
        return ob.TaxPolicyBuilder(
            tax_pcts=[
                (PAST, 18),
                (PRESENT, 20)
            ]
        ).build(session).obj

    @staticmethod
    def _create_product_w_static_taxes(session):
        tax_policy = ob.TaxPolicyBuilder(tax_pcts=[(PAST, 20)]).build(session).obj
        tpp, = tax_policy.taxes
        return ob.ProductBuilder(
            taxes=tax_policy,
            prices=[(PAST, 'RUR', 30, tpp)]
        ).build(session).obj

    @staticmethod
    def _create_endbuyer_budget(session, credit_contract, endbuyer, subclient, sum_):
        budget = mapper.EndbuyerBudget(
            endbuyer=endbuyer,
            contract=credit_contract,
            period_dt=mapper.ActMonth().begin_dt,
            sum=sum_
        )
        budget_subclient = mapper.EndbuyerSubclient(
            budget=budget,
            agency=credit_contract.client,
            client=subclient,
            priority=666
        )
        session.add(budget)
        session.add(budget_subclient)
        session.flush()
        return budget

    def test_fpa(self, session, credit_contract, subclient, paysys, split_act_creation):
        on_dt = datetime.datetime.now() - datetime.timedelta(60)

        product = self._create_product_w_static_taxes(session)
        order1 = self._create_order(session, subclient, product)
        order2 = self._create_order(session, subclient, product)
        pa = consume_credit(credit_contract, [(order1, 6666)], paysys.id)

        calculate_consumption(order1, on_dt, 10)
        generate_act(pa, mapper.ActMonth(), split_act_creation)

        calculate_consumption(order1, on_dt, 0)
        order1.transfer(order2, force_transfer_acted=True)
        calculate_consumption(order2, on_dt, 25)

        generate_act(pa, mapper.ActMonth(), split_act_creation)

        assert sum(co.act_qty for co in order1.consumes) == 0
        assert sum(co.act_qty for co in order2.consumes) == 25
        assert sum(co.act_sum for co in order2.consumes) == 750
        hamcrest.assert_that(
            list(itertools.chain.from_iterable(r.acts for r in pa.repayments)),
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    amount=300,
                    amount_nds=D('50'),
                    type='generic',
                ),
                hamcrest.has_properties(
                    amount=450,
                    amount_nds=D('75'),
                    type='generic'
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            consumes_match(
                [(0, 0, 0, 0, 0, 0)],
                forced_params=[
                    'current_qty', 'current_sum',
                    'completion_qty', 'completion_sum',
                    'act_qty', 'act_sum',
                ]
            )
        )
        hamcrest.assert_that(
            order2.consumes,
            consumes_match(
                [(6666, 6666 * 30, 25, 25 * 30, 25, 25 * 30)],
                forced_params=[
                    'current_qty', 'current_sum',
                    'completion_qty', 'completion_sum',
                    'act_qty', 'act_sum',
                ]
            )
        )

    def test_prepay_taxes_update(self, session, client, person, tax_policy, split_act_creation):
        tpp1, tpp2 = tax_policy.taxes
        product_old = self._create_product_w_taxes(session, tax_policy)
        product_new = self._create_product_w_taxes(session, tax_policy)

        order_old = self._create_order(session, client, product_old)
        order_new = self._create_order(session, client, product_new)

        # включаем счёт со старыми налогами
        invoice = ob.InvoiceBuilder(
            person=person,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    dt=NEAR_PAST,
                    client=client,
                    rows=[ob.BasketItemBuilder(order=order_old, quantity=100)]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(on_dt=invoice.dt)

        # откручиваем и актим со старыми налогами
        calculate_consumption(order_old, NEAR_PAST, 20)
        generate_act(invoice, NEAR_PAST, split_act_creation)

        # убираем откруки, переносим свободные средства на другой заказ с новым налогом
        calculate_consumption(order_old, NEAR_PAST, 0)
        order_old.transfer(order_new, force_transfer_acted=True)

        # откручиваем и актим с новыми налогами
        calculate_consumption(order_new, PRESENT, order_new.consume_qty)
        generate_act(invoice, PRESENT, split_act_creation)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                total_act_sum=3000,
                acts=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        amount=600,
                        amount_nds=D('91.53'),
                        type='generic',
                        tax_policy_pct=tpp1
                    ),
                    hamcrest.has_properties(
                        amount=2400,
                        amount_nds=400,
                        type='generic',
                        tax_policy_pct=tpp2
                    )
                )
            )
        )
        hamcrest.assert_that(
            order_old.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id)
            ])
        )
        hamcrest.assert_that(
            order_new.consumes,
            self.consumes_match([
                (100, 3000, 100, 3000, 100, 3000, tpp2.id)
            ])
        )

    def test_fpa_taxes_update(self, session, credit_contract, subclient, paysys, tax_policy, split_act_creation):
        # продукт
        tpp1, tpp2 = tax_policy.taxes
        product = self._create_product_w_taxes(session, tax_policy)

        # заказы
        order_old = self._create_order(session, subclient, product)
        order_new = self._create_order(session, subclient, product)

        # ЛС, зачисление
        pa = (
            pam.PersonalAccountManager(session)
                .for_paysys(paysys)
                .for_contract(credit_contract)
                .get()
        )
        price_obj_old = pa.internal_price(order_old, NEAR_PAST)
        create_consume(pa, order_old, 6666, price_obj_old)

        # откручиваем и актим заказ со старыми налогами
        calculate_consumption(order_old, NEAR_PAST, 10)
        generate_act(pa, CUR_MONTH, split_act_creation)

        # откатываем открутки, переносим на другой заказ
        calculate_consumption(order_old, NEAR_PAST, 0)
        order_old.transfer(order_new, force_transfer_acted=True)

        # актим с новыми налогами
        calculate_consumption(order_new, PRESENT, 25)
        generate_act(pa, CUR_MONTH, split_act_creation)

        hamcrest.assert_that(
            list(itertools.chain.from_iterable(r.acts for r in pa.repayments)),
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    amount=300,
                    amount_nds=D('45.76'),
                    type='generic',
                    tax_policy_pct=tpp1
                ),
                hamcrest.has_properties(
                    amount=450,
                    amount_nds=75,
                    type='generic',
                    tax_policy_pct=tpp2
                )
            )
        )
        hamcrest.assert_that(
            order_old.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id)
            ])
        )
        hamcrest.assert_that(
            order_new.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id),
                (6666, 6666 * 30, 25, 25 * 30, 25, 25 * 30, tpp2.id)
            ])
        )

    def test_prepay_mixed_tax_single_act(self, session, client, person, tax_policy, split_act_creation):
        tpp1, tpp2 = tax_policy.taxes
        product = self._create_product_w_taxes(session, tax_policy)

        order1 = self._create_order(session, client, product)
        order2 = self._create_order(session, client, product)

        # включаем счёт со старыми налогами
        invoice = ob.InvoiceBuilder(
            person=person,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    dt=NEAR_PAST,
                    client=client,
                    rows=[ob.BasketItemBuilder(order=order1, quantity=666)]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)

        old_price = invoice.internal_price(order1, NEAR_PAST)
        new_price = invoice.internal_price(order1, PRESENT)

        # зачисляем и актим со старым налогом
        acted_consume = create_consume(invoice, order1, 20, old_price)
        calculate_consumption(order1, NEAR_PAST, 20)
        generate_act(invoice, NEAR_PAST, split_act_creation)

        # создаём другой конзюм на тот же заказ с новым налогом
        a_c.reverse_consume(acted_consume, None, acted_consume.current_qty)
        create_consume(invoice, order1, 15, new_price)
        calculate_consumption(order1, PRESENT, 15)

        # зачисляем на другой заказ со старым налогом
        create_consume(invoice, order2, 10, old_price)
        calculate_consumption(order2, NEAR_PAST, 10)

        # создаём второй акт
        generate_act(invoice, PRESENT, split_act_creation)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                total_act_sum=750,
                acts=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        amount=600,
                        amount_nds=D('91.53'),
                        type='generic',
                        tax_policy_pct=tpp1
                    ),
                    hamcrest.has_properties(
                        amount=150,
                        amount_nds=25,
                        type='generic',
                        tax_policy_pct=tpp2
                    )
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id),
                (15, 15 * 30, 15, 15 * 30, 15, 15 * 30, tpp2.id),
            ])
        )
        hamcrest.assert_that(
            order2.consumes,
            self.consumes_match([
                (10, 10 * 30, 10, 10 * 30, 10, 10 * 30, tpp1.id)
            ])
        )

    def test_prepay_mixed_tax_multiple_acts(self, session, client, person, tax_policy, split_act_creation):
        tpp1, tpp2 = tax_policy.taxes
        product = self._create_product_w_taxes(session, tax_policy)

        order1 = self._create_order(session, client, product)
        order2 = self._create_order(session, client, product)

        # включаем счёт со старыми налогами
        invoice = ob.InvoiceBuilder(
            person=person,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    dt=NEAR_PAST,
                    client=client,
                    rows=[ob.BasketItemBuilder(order=order1, quantity=666)]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)

        old_price = invoice.internal_price(order1, NEAR_PAST)
        new_price = invoice.internal_price(order1, PRESENT)

        # зачисляем и актим со старым налогом
        acted_consume = create_consume(invoice, order1, 15, old_price)
        calculate_consumption(order1, NEAR_PAST, 15)
        generate_act(invoice, NEAR_PAST, split_act_creation)

        # создаём другой конзюм на тот же заказ с новым налогом
        a_c.reverse_consume(acted_consume, None, acted_consume.current_qty)
        create_consume(invoice, order1, 10, new_price)
        calculate_consumption(order1, PRESENT, 10)

        # зачисляем на другой заказ со старым налогом
        create_consume(invoice, order2, 20, old_price)
        calculate_consumption(order2, NEAR_PAST, 20)

        # создаём второй акт
        generate_act(invoice, PRESENT, split_act_creation)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                total_act_sum=900,
                acts=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        amount=450,
                        amount_nds=D('68.64'),
                        type='generic',
                        tax_policy_pct=tpp1
                    ),
                    hamcrest.has_properties(
                        amount=150,
                        amount_nds=hamcrest.is_in([D('22.88'), D('22.89')]),
                        type='generic',
                        tax_policy_pct=tpp1
                    ),
                    hamcrest.has_properties(
                        amount=300,
                        amount_nds=50,
                        type='generic',
                        tax_policy_pct=tpp2
                    )
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id),
                (10, 10 * 30, 10, 10 * 30, 10, 10 * 30, tpp2.id),
            ])
        )
        hamcrest.assert_that(
            order2.consumes,
            self.consumes_match([
                (20, 20 * 30, 20, 20 * 30, 20, 20 * 30, tpp1.id)
            ])
        )

    def test_fpa_mixed_tax_single_act(self, session, credit_contract, subclient, paysys, tax_policy, split_act_creation):
        # продукт
        tpp1, tpp2 = tax_policy.taxes
        product = self._create_product_w_taxes(session, tax_policy)

        # заказы
        order1 = self._create_order(session, subclient, product)
        order2 = self._create_order(session, subclient, product)

        # ЛС
        pa = (
            pam.PersonalAccountManager(session)
                .for_paysys(paysys)
                .for_contract(credit_contract)
                .get()
        )
        old_price = pa.internal_price(order1, NEAR_PAST)
        new_price = pa.internal_price(order1, PRESENT)

        # зачисляем и актим со старым налогом
        acted_consume = create_consume(pa, order1, 20, old_price)
        calculate_consumption(order1, NEAR_PAST, 20)
        generate_act(pa, CUR_MONTH, split_act_creation)

        # создаём другой конзюм на тот же заказ с новым налогом
        a_c.reverse_consume(acted_consume, None, acted_consume.current_qty)
        create_consume(pa, order1, 15, new_price)
        calculate_consumption(order1, PRESENT, 15)

        # зачисляем на другой заказ со старым налогом
        create_consume(pa, order2, 10, old_price)
        calculate_consumption(order2, NEAR_PAST, 10)

        # создаём второй акт
        generate_act(pa, CUR_MONTH, split_act_creation)

        hamcrest.assert_that(
            list(itertools.chain.from_iterable(r.acts for r in pa.repayments)),
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    amount=600,
                    amount_nds=D('91.53'),
                    type='generic',
                    tax_policy_pct=tpp1
                ),
                hamcrest.has_properties(
                    amount=150,
                    amount_nds=25,
                    type='generic',
                    tax_policy_pct=tpp2
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id),
                (15, 15 * 30, 15, 15 * 30, 15, 15 * 30, tpp2.id),
            ])
        )
        hamcrest.assert_that(
            order2.consumes,
            self.consumes_match([
                (10, 10 * 30, 10, 10 * 30, 10, 10 * 30, tpp1.id)
            ])
        )

    def test_fpa_mixed_tax_multiple_acts(self, session, credit_contract,
                                         subclient, paysys, tax_policy, split_act_creation):
        # продукт
        tpp1, tpp2 = tax_policy.taxes
        product = self._create_product_w_taxes(session, tax_policy)

        # заказы
        order1 = self._create_order(session, subclient, product)
        order2 = self._create_order(session, subclient, product)

        # ЛС
        pa = (
            pam.PersonalAccountManager(session)
                .for_paysys(paysys)
                .for_contract(credit_contract)
                .get()
        )
        old_price = pa.internal_price(order1, NEAR_PAST)
        new_price = pa.internal_price(order1, PRESENT)

        # зачисляем и актим со старым налогом
        acted_consume = create_consume(pa, order1, 20, old_price)
        calculate_consumption(order1, NEAR_PAST, 20)
        generate_act(pa, CUR_MONTH, split_act_creation)

        # создаём другой конзюм на тот же заказ с новым налогом
        a_c.reverse_consume(acted_consume, None, acted_consume.current_qty)
        create_consume(pa, order1, 15, new_price)
        calculate_consumption(order1, PRESENT, 15)

        # зачисляем на другой заказ со старым налогом
        create_consume(pa, order2, 30, old_price)
        calculate_consumption(order2, NEAR_PAST, 30)

        # создаём второй акт
        generate_act(pa, CUR_MONTH, split_act_creation)

        hamcrest.assert_that(
            list(itertools.chain.from_iterable(r.acts for r in pa.repayments)),
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    amount=600,
                    amount_nds=D('91.53'),
                    type='generic',
                    tax_policy_pct=tpp1
                ),
                hamcrest.has_properties(
                    amount=300,
                    amount_nds=D('45.76'),
                    type='generic',
                    tax_policy_pct=tpp1
                ),
                hamcrest.has_properties(
                    amount=450,
                    amount_nds=D('75'),
                    type='generic',
                    tax_policy_pct=tpp2
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id),
                (15, 15 * 30, 15, 15 * 30, 15, 15 * 30, tpp2.id),
            ])
        )
        hamcrest.assert_that(
            order2.consumes,
            self.consumes_match([
                (30, 30 * 30, 30, 30 * 30, 30, 30 * 30, tpp1.id),
            ])
        )

    def test_prepay_zero_act(self, session, client, person, tax_policy, split_act_creation):
        tpp1, tpp2 = tax_policy.taxes
        product = self._create_product_w_taxes(session, tax_policy)

        order1 = self._create_order(session, client, product)
        order2 = self._create_order(session, client, product)

        # включаем счёт со старыми налогами
        invoice = ob.InvoiceBuilder(
            person=person,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    dt=NEAR_PAST,
                    client=client,
                    rows=[ob.BasketItemBuilder(order=order1, quantity=666)]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)

        old_price = invoice.internal_price(order1, NEAR_PAST)

        # зачисляем и актим со старым налогом
        acted_consume = create_consume(invoice, order1, 15, old_price)
        calculate_consumption(order1, NEAR_PAST, 15)
        generate_act(invoice, NEAR_PAST, split_act_creation)

        # создаём переакт
        a_c.reverse_consume(acted_consume, None, acted_consume.current_qty)
        calculate_consumption(order1, NEAR_PAST, 0)

        # зачисляем на другой заказ со старым налогом
        create_consume(invoice, order2, 15, old_price)
        calculate_consumption(order2, NEAR_PAST, 15)

        # создаём второй акт
        invoice.internal_acts = True  # чтобы создался нулевой акт
        generate_act(invoice, PRESENT, split_act_creation)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                total_act_sum=450,
                acts=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        amount=450,
                        amount_nds=D('68.64'),
                        type='generic',
                        tax_policy_pct=tpp1
                    ),
                    hamcrest.has_properties(
                        amount=0,
                        amount_nds=0,
                        type='internal',
                        tax_policy_pct=tpp1
                    ),
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id),
            ])
        )
        hamcrest.assert_that(
            order2.consumes,
            self.consumes_match([
                (15, 15 * 30, 15, 15 * 30, 15, 15 * 30, tpp1.id)
            ])
        )

    def test_fpa_zero_act(self, session, credit_contract, subclient, paysys, tax_policy, split_act_creation):
        # продукт
        tpp1, tpp2 = tax_policy.taxes
        product = self._create_product_w_taxes(session, tax_policy)

        # заказы
        order1 = self._create_order(session, subclient, product)
        order2 = self._create_order(session, subclient, product)

        # ЛС
        pa = (
            pam.PersonalAccountManager(session)
                .for_paysys(paysys)
                .for_contract(credit_contract)
                .get()
        )
        old_price = pa.internal_price(order1, NEAR_PAST)

        # зачисляем и актим со старым налогом
        acted_consume = create_consume(pa, order1, 15, old_price)
        calculate_consumption(order1, NEAR_PAST, 15)
        generate_act(pa, CUR_MONTH, split_act_creation)

        # создаём переакт
        a_c.reverse_consume(acted_consume, None, acted_consume.current_qty)
        calculate_consumption(order1, NEAR_PAST, 0)

        # зачисляем на другой заказ со старым налогом
        create_consume(pa, order2, 15, old_price)
        calculate_consumption(order2, NEAR_PAST, 15)

        # создаём второй акт
        pa.internal_acts = True  # чтобы создался нулевой акт
        generate_act(pa, CUR_MONTH, split_act_creation)

        hamcrest.assert_that(
            list(itertools.chain.from_iterable(r.acts for r in pa.repayments)),
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    amount=450,
                    amount_nds=D('68.64'),
                    type='generic',
                    tax_policy_pct=tpp1
                ),
            )
        )
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                total_act_sum=0,
                acts=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        amount=0,
                        amount_nds=0,
                        type='internal',
                        tax_policy_pct=tpp1
                    ),
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            self.consumes_match([
                (0, 0, 0, 0, 0, 0, tpp1.id),
            ])
        )
        hamcrest.assert_that(
            order2.consumes,
            self.consumes_match([
                (15, 15 * 30, 15, 15 * 30, 15, 15 * 30, tpp1.id)
            ])
        )

    def test_rows_limit(self, session, client, person, split_act_creation):
        order1 = self._create_order(session, client, DIRECT_PRODUCT_RUB_ID)
        order2 = self._create_order(session, client, DIRECT_PRODUCT_RUB_ID)

        # включаем счёт со старыми налогами
        invoice = ob.InvoiceBuilder(
            person=person,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    dt=NEAR_PAST,
                    client=client,
                    rows=[
                        ob.BasketItemBuilder(order=order1, quantity=7),
                        ob.BasketItemBuilder(order=order2, quantity=15),
                    ]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        calculate_consumption(order1, NEAR_PAST, 7)
        generate_act(invoice, NEAR_PAST, split_act_creation)

        acted_consume, = order1.consumes
        a_c.reverse_consume(acted_consume, None, acted_consume.current_qty)
        calculate_consumption(order1, NEAR_PAST, 0)

        calculate_consumption(order2, NEAR_PAST, 15)
        with mock.patch('balance.actions.acts.grouper.OebsExportActGrouperInfo.max_num_of_groups', return_value=2):
            generate_act(invoice, PRESENT, split_act_creation)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                total_act_sum=15,
                acts=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(amount=7),
                    hamcrest.has_properties(amount=8),
                )
            )
        )
        hamcrest.assert_that(
            order1.consumes,
            consumes_match([
                (invoice.id, 0, 0, 0, 0, 7, 7),
            ])
        )
        hamcrest.assert_that(
            order2.consumes,
            consumes_match([
                (invoice.id, 15, 15, 15, 15, 8, 8)
            ])
        )

    @pytest.mark.parametrize(
        'budget_sum, act_w_budget_sum, act_wo_budget_sum',
        [
            (330, 30, 420),
            (630, 330, 120),
        ],
        ids=['small_budget', 'big_budget']
    )
    def test_endbuyers_overact_split(self, session, credit_contract, endbuyer, subclient, paysys,
                                     budget_sum, act_w_budget_sum, act_wo_budget_sum, split_act_creation):
        on_dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(60))

        budget = self._create_endbuyer_budget(session, credit_contract, endbuyer, subclient, budget_sum)

        product = self._create_product_w_static_taxes(session)
        order1 = self._create_order(session, subclient, product)
        order2 = self._create_order(session, subclient, product)
        pa = consume_credit(credit_contract, [(order1, 6666)], paysys.id)

        calculate_consumption(order1, on_dt, 10)
        generate_act(pa, mapper.ActMonth(), split_act_creation)

        calculate_consumption(order1, on_dt, 0)
        order1.transfer(order2, force_transfer_acted=True)
        calculate_consumption(order2, on_dt, 25)

        generate_act(pa, mapper.ActMonth(), split_act_creation)

        hamcrest.assert_that(budget, hamcrest.has_properties(act_sum=budget_sum))
        hamcrest.assert_that(
            list(itertools.chain.from_iterable(r.acts for r in pa.repayments)),
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    amount=300,
                    rows=hamcrest.has_items(hamcrest.has_properties(budget_id=budget.id))
                ),
                hamcrest.has_properties(
                    amount=act_w_budget_sum,
                    rows=hamcrest.has_items(hamcrest.has_properties(budget_id=budget.id))
                ),
                hamcrest.has_properties(
                    amount=act_wo_budget_sum,
                ),
            )
        )
