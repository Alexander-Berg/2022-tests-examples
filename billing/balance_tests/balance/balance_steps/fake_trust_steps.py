# coding: utf-8
from datetime import datetime
from dateutil.relativedelta import relativedelta
from decimal import Decimal
from operator import itemgetter
from uuid import uuid4
import collections
import copy
import json
import re
import time

import balance.balance_api as api
from balance import balance_db as db
from balance.balance_steps.export_steps import ExportSteps
from balance.balance_steps.partner_steps import CommonPartnerSteps
from balance.balance_steps.simple_api_steps import SimpleApi
from btestlib.constants import Currencies, Export, PaymentMethods, Services, ServiceSchemaParams
from btestlib.data import simpleapi_defaults
from btestlib import utils
import btestlib.reporter as reporter
from cashmachines.data.constants import CMNds

from simpleapi.common.payment_methods import TYPE, BasePaymethod, LinkedCard
from simpleapi.data import cards_pool, defaults
import simpleapi.steps.simple_steps as simple_steps


DEFAULT_JSON_SERVICE_ORDER_ID_NUMBER = 22222222  # random
DEFAULT_JSON_SOURCE_ID = 333333  # random


# генератор платежных методов для простейших случаев
class ConstantPaymethod(BasePaymethod):
    def __init__(self, paymethod):
        super(ConstantPaymethod, self).__init__()
        self._id = self.type = self.title = paymethod


# фабрика для платежных методов, используемых в разметке.
# обрабатывается узкий список методов используемых в тестах, поэтому оставляю здесь, а не в payment_methods
class PaymethodFactory():
    @staticmethod
    def get_instance(payment_type):
        if payment_type == TYPE.CARD:
            return LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD)
        # для
        # TYPE.COMPOSITE
        # TYPE.VIRTUAL_PROMOCODE
        # TYPE.VIRTUAL_KINOPOISK_SUBS_DISCOUNT
        # TYPE.VIRTUAL_KINOPOISK_CARD_DISCOUNT
        return ConstantPaymethod(paymethod=payment_type)


class FakeTrustApi(object):
    """
    Методы для подмены трастовых ручек
    SimpleApi.create_trust_payment
    SimpleApi.create_refund
    SimpleApi.create_multiple_trust_payments
    SimpleApi.create_multiple_tickets_payment
    SimpleApi.create_multiple_refunds
    SimpleApi.get_multiple_promocode_payment_ids_by_composite_tag
    SimpleNewApi.create_payment
    SimpleNewApi.create_topup_payment
    SimpleNewApi.create_refund
    SimpleNewApi.create_multiple_orders_for_payment
    simpleapi_steps.process_promocode_creating

    Генерируют правдоподобные данные транзакций для сервисов, перешедших на json строки платежей
    ( а сейчас в проде перешли все сервисы )
    Не генерируют request и request_orders!
    На данный момент не работает с разметкой корзины
    На данный момент не работает частичная/полная очистка корзины
    (т.е нельзя провести clean clear с заказмми до экспорта в баланс)
    """
    def __init__(self):
        # previous payments by trust_payment_id
        self._previous_payments_arguments = {}
        # created promo codes
        self._promo_amounts = {}
        # previous order data
        self._orders = None
        self._orders_service_product_ids = None
        self._orders_commission_category_list = None
        # previous markup data
        self._previous_paymethod_markup = None

    # -------------------------------------------------------------------
    # PREVIOUS PARAMETERS PART
    """
     При создании возвратов, в ручки траста не передается часть данных, которая была в платеже.
     Что бы ее получить - мы храним предыдущие вызовы в словаре
     trust_payment_id ->  Все переменные в запуске создания платежа
    """
    def save_payment_arguments(self, trust_payment_id, args):
        del args['self']
        self._previous_payments_arguments[trust_payment_id] = args

    def get_payment_arguments(self, trust_payment_id):
        if trust_payment_id not in self._previous_payments_arguments:
            raise ValueError('Payment with trust_payment_id "{}" not found in this test'.format(trust_payment_id))
        return self._previous_payments_arguments[trust_payment_id]

    def get_previous_paymethod_markup(self):
        return copy.deepcopy(self._previous_paymethod_markup)

    # -------------------------------------------------------------------
    # SIMPLE API PART
    # здесь неиспользуемые параметры нужны для однообразности сигнатур с реальными методами
    def create_multiple_trust_payments(self,
                                       service,
                                       service_product_id_list,
                                       service_order_id_list=None,
                                       currency=Currencies.RUB,
                                       region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                       commission_category_list=None,
                                       prices_list=None,
                                       user=simpleapi_defaults.DEFAULT_USER,
                                       paymethod=None,
                                       user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                                       order_dt=None,
                                       need_postauthorize=True,
                                       developer_payload_list=None,
                                       fiscal_nds_list=None,
                                       developer_payload_basket=None,
                                       pass_params=None,
                                       pass_cvn=False,
                                       back_url=None,
                                       qty_list=None,
                                       wait_for_export_from_bs=True,
                                       paymethod_markup=None,
                                       spasibo_order_map=None,
                                       export_payment=False,
                                       ):
        with reporter.step(u'ФЕЙКОВЫМ TRUST Создаем платеж для сервиса: {} и продуктов: {}.'.format(
                service.name, service_product_id_list)):
            return self._create_multiple_fake_payments(
                service=service,
                service_product_id_list=service_product_id_list,
                service_order_id_list=service_order_id_list,
                currency=currency,
                region_id=region_id,
                commission_category_list=commission_category_list,
                prices_list=prices_list,
                user=user,
                paymethod=paymethod,
                user_ip=user_ip,
                order_dt=order_dt,
                developer_payload_list=developer_payload_list,
                fiscal_nds_list=fiscal_nds_list,
                developer_payload_basket=developer_payload_basket,
                qty_list=qty_list,
                paymethod_markup=paymethod_markup,
                export_payment=export_payment,
            )

    def create_trust_payment(self,
                             service,
                             service_product_id,
                             service_order_id=None,
                             currency=Currencies.RUB,
                             region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                             commission_category=simpleapi_defaults.DEFAULT_COMMISSION_CATEGORY,
                             price=simpleapi_defaults.DEFAULT_PRICE,
                             user=simpleapi_defaults.DEFAULT_USER,
                             paymethod=None,
                             user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                             order_dt=None,
                             need_postauthorize=True,
                             developer_payload=None,
                             fiscal_nds=None,
                             developer_payload_basket=None,
                             pass_params=None,
                             pass_cvn=False,
                             back_url=None,
                             qty=None,
                             wait_for_export_from_bs=True,  # для единого интерфейса
                             export_payment=False
                             ):
        service_order_ids, trust_payment_id, purchase_token, payment_id = self._create_multiple_fake_payments(
            service=service,
            service_product_id_list=[service_product_id],
            service_order_id_list=[service_order_id] if service_order_id else None,
            currency=currency,
            region_id=region_id,
            commission_category_list=[commission_category],
            prices_list=[price],
            user=user,
            paymethod=paymethod,
            user_ip=user_ip,
            order_dt=order_dt,
            developer_payload_list=[developer_payload] if developer_payload else None,
            fiscal_nds_list=[fiscal_nds] if fiscal_nds else None,
            developer_payload_basket=developer_payload_basket,
            qty_list=[qty] if qty else None,
            export_payment=export_payment
        )
        return service_order_ids[0], trust_payment_id, purchase_token, payment_id

    def create_multiple_refunds(self,
                                service,
                                service_order_id_list,
                                trust_payment_id,
                                delta_amount_list=None,
                                export_payment=False,
                                paymethod_markup=None,
                                spasibo_order_map=None):
        return self._create_multiple_fake_refunds(
            service=service,
            service_order_id_list=service_order_id_list,
            trust_payment_id=trust_payment_id,
            delta_amount_list=delta_amount_list,
            export_payment=export_payment,
            paymethod_markup=paymethod_markup,
        )

    def create_refund(self,
                      service,
                      service_order_id,
                      trust_payment_id,
                      service_order_id_fee=None,
                      delta_amount=simpleapi_defaults.DEFAULT_PRICE,
                      export_payment=False,
                      paymethod_markup=None,
                      spasibo_order_map=None):
        service_order_id_list = [service_order_id]
        delta_amount_list = [delta_amount]
        if service_order_id_fee:
            # когда отменяем платеж - сервисный сбор отменяется автоматом
            service_order_id_list.append(service_order_id_fee)
            payment_args = self.get_payment_arguments(trust_payment_id)
            delta_amount_list.append(payment_args['prices_list'][1])

        return self._create_multiple_fake_refunds(
            service=service,
            service_order_id_list=service_order_id_list,
            trust_payment_id=trust_payment_id,
            delta_amount_list=delta_amount_list,
            export_payment=export_payment,
            paymethod_markup=paymethod_markup
        )

    def create_multiple_tickets_payment(self, service, products, user=simpleapi_defaults.DEFAULT_USER,
                                        product_fees=None, discounts=None, promocode_id=None, paymethod=None,
                                        region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                        currency=simpleapi_defaults.DEFAULT_CURRENCY):
        with reporter.step(u'Создаем платеж для продуктов: {}, сборов: {}'.format(products, product_fees)):
            reporter.attach(u'Скидки', utils.Presenter.pretty(discounts))
            reporter.attach(u'Промокод', utils.Presenter.pretty(promocode_id))

            paymethod = paymethod or PaymethodFactory.get_instance(TYPE.CARD)
            product_fees = product_fees or []

            product_price = simpleapi_defaults.DEFAULT_PRICE
            if discounts:
                discount_factor = self._get_discount_factor(discounts)
                product_price = product_price * discount_factor
            prices_list = [product_price] * len(products) + \
                          [simpleapi_defaults.DEFAULT_FEE] * len(product_fees)

            service_product_id_list = products + product_fees
            commission_category_list = [simpleapi_defaults.DEFAULT_COMMISSION_CATEGORY] * len(service_product_id_list)

            promo_service_product_id_list = None
            if promocode_id and promocode_id in self._promo_amounts:
                promo_amount = self._promo_amounts[promocode_id]
                (promo_prices_list, prices_list,
                 promo_service_product_id_list, service_product_id_list,
                 promo_commission_category_list, commission_category_list) = self._split_params_by_promo(
                    promo_amount=promo_amount,
                    prices_list=prices_list,
                    service_product_id_list=service_product_id_list,
                    commission_category_list=commission_category_list,
                )

            main_payment_id = None
            if service_product_id_list:  # если после применения промокода что-то осталось
                service_order_id_list, trust_payment_id, purchase_token, main_payment_id = self._create_multiple_fake_payments_wo_markup(
                    service=service,
                    service_product_id_list=service_product_id_list,
                    currency=currency,
                    region_id=region_id,
                    commission_category_list=commission_category_list,
                    prices_list=prices_list,
                    user=user,
                    paymethod=paymethod
                )

            if promo_service_product_id_list:  # если что-то откусили промокодом
                promo_service_order_id_list, promo_trust_payment_id, promo_purchase_token, promo_payment_id = \
                    self._create_multiple_fake_payments_wo_markup(
                        service=service,
                        service_product_id_list=promo_service_product_id_list,
                        service_order_id_list=service_order_id_list,
                        currency=currency,
                        region_id=region_id,
                        commission_category_list=promo_commission_category_list,
                        prices_list=promo_prices_list,
                        user=user,
                        paymethod=PaymethodFactory.get_instance(TYPE.VIRTUAL_PROMOCODE)
                    )
                if main_payment_id:  # если покрываем не весь платеж - свяжем с основным, иначе основной- промокодный
                    self.join_payments_by_composite_tag([main_payment_id, promo_payment_id])
                else:
                    service_order_id_list, trust_payment_id, purchase_token, _ = (
                        promo_service_order_id_list, promo_trust_payment_id, promo_purchase_token, promo_payment_id
                    )

            service_order_id_products, service_order_id_fees = \
                service_order_id_list[:len(products)], service_order_id_list[len(products):]
            return purchase_token, service_order_id_fees, service_order_id_products, trust_payment_id

    def get_multiple_promocode_payment_ids_by_composite_tag(self, composite_tag):
        rows = db.balance().execute(
            "select id, trust_payment_id from bo.t_ccard_bound_payment "
            "where composite_tag = :composite_tag and payment_method = 'virtual::new_promocode'",
            {'composite_tag': composite_tag})
        return [r['id'] for r in rows], [r['trust_payment_id'] for r in rows]

    def postauthorize(self,
                      service,
                      trust_payment_id,
                      service_order_id_list,
                      user=simpleapi_defaults.DEFAULT_USER,
                      amounts=None,
                      actions=None,
                      paymethod_markup=None):
        self._create_reversals(
            service=service,
            trust_payment_id=trust_payment_id,
            service_order_id_list=service_order_id_list,
            amounts=amounts,
            actions=actions,
            paymethod_markup=paymethod_markup
        )
        payment_args = self.get_payment_arguments(trust_payment_id)
        if service.id == Services.BLUE_MARKET_PAYMENTS.id:
            self.create_refund_for_blue_cashback(payment_args['payment_id'])

    # -------------------------------------------------------------------
    # SIMPLE NEW API PART
    def new_create_payment(self,
                           service,
                           product_id=None,
                           amount=simpleapi_defaults.DEFAULT_PRICE,
                           paymethod=None,
                           paymethod_markup=None,
                           user=simpleapi_defaults.USER_NEW_API,
                           orders=None,
                           wait_for_export_from_bs=True,
                           need_clearing=True,
                           pass_params=None,
                           currency=Currencies.RUB.iso_code,
                           discounts=None,
                           fiscal_nds=None,
                           fiscal_title=None,
                           developer_payload=None,
                           ignore_missing_trust_payment_id=False
                           ):
        currency = self._get_currency_object(currency)
        fiscal_nds_list = self._get_from_orders('fiscal_nds')
        if fiscal_nds_list is None:
            fiscal_nds_list = [fiscal_nds]
            fiscal_title_list = [fiscal_title]
        else:
            fiscal_nds_list = self._convert_fiscal_nds_strings_to_objects(fiscal_nds_list)
            fiscal_title_list = self._get_from_orders('fiscal_title')

        service_order_id_list = [o['order_id'] for o in orders] if orders else self._get_from_orders('order_id')
        service_product_id_list = [product_id] if product_id else self._orders_service_product_ids
        prices_list = self._get_from_orders('price') or [amount]
        if service_order_id_list:
            prices_list = prices_list[:len(service_order_id_list)]

        _, trust_payment_id, purchase_token, payment_id = self._create_multiple_fake_payments(
            service,
            service_product_id_list=service_product_id_list,
            prices_list=prices_list,
            paymethod=paymethod,
            paymethod_markup=paymethod_markup,
            user=user,
            currency=currency,
            fiscal_nds_list=fiscal_nds_list,
            service_order_id_list=service_order_id_list,
            commission_category_list=self._orders_commission_category_list,
            developer_payload_basket=developer_payload,
            fiscal_title_list=fiscal_title_list
        )
        return trust_payment_id, payment_id, purchase_token

    def new_create_topup_payment(self,
                                 service,
                                 product_id=None,
                                 amount=simpleapi_defaults.DEFAULT_PRICE,
                                 paymethod=None,
                                 user=None,
                                 wait_for_export_from_bs=True,
                                 pass_params=None,
                                 currency=Currencies.RUB.iso_code,
                                 fiscal_nds=None,
                                 fiscal_title=None,
                                 developer_payload=None):
        return self.new_create_payment(
            service=service,
            product_id=product_id,
            amount=amount,
            paymethod=paymethod,
            user=user,
            wait_for_export_from_bs=wait_for_export_from_bs,
            pass_params=pass_params,
            currency=currency,
            fiscal_nds=fiscal_nds,
            fiscal_title=fiscal_title,
            developer_payload=developer_payload
        )

    def new_create_refund(self,
                          service,
                          purchase_token,
                          user=simpleapi_defaults.USER_NEW_API,
                          orders=None,
                          paymethod_markup=None):
        trust_payment_id = self.purchase_token_to_trust_payment_id(purchase_token)
        delta_amount_list = None
        if orders:
            delta_amount_list = [order['delta_amount'] for order in orders]

        return self._create_multiple_fake_refunds(
            service=service,
            service_order_id_list=[],
            trust_payment_id=trust_payment_id,
            delta_amount_list=delta_amount_list,
            paymethod_markup=paymethod_markup
        )

    def new_create_multiple_orders_for_payment(self,
                                               service,
                                               product_id_list=None,
                                               user=None,
                                               orders_structure=defaults.Order.structure_rub_two_orders,
                                               commission_category_list=None,
                                               amount_list=None,
                                               fiscal_nds=None,
                                               service_order_ids=None):
        with reporter.step(
                u'ФЕЙКОВЫМ TRUST Создаем заказы для сервиса: {} с продуктами: {}.'.format(
                    service.name, product_id_list)):
            orders = orders_structure
            amount_list = amount_list or [simpleapi_defaults.DEFAULT_PRICE] * len(orders_structure)
            service_order_ids = service_order_ids or range(5000000000, 5000000000 + len(orders_structure))
            for order, amount, order_id in zip(orders, amount_list, service_order_ids):
                order['order_id'] = order_id
                if fiscal_nds:
                    order['fiscal_nds'] = fiscal_nds
                if amount is not None:
                    order['price'] = str(amount)

            self._orders_service_product_ids = product_id_list
            self._orders_commission_category_list = commission_category_list
            self._orders = orders
            return orders

    def new_unhold_payment(self, service, purchase_token, user=None):
        trust_payment_id = self.purchase_token_to_trust_payment_id(purchase_token)
        payment_args = self.get_payment_arguments(trust_payment_id)
        self._create_reversals(
            service,
            trust_payment_id,
            payment_args['service_order_id_list'],
            # отменяем все заказы
            actions=('cancel',) * len(payment_args['service_order_id_list'])
        )

    def new_resize_multiple_orders(self,
                                   service,
                                   purchase_token,
                                   orders,
                                   amount_list,
                                   user=None,
                                   paymethod_markup=None,
                                   qty_list=None):
        trust_payment_id = self.purchase_token_to_trust_payment_id(purchase_token)
        payment_args = self.get_payment_arguments(trust_payment_id)
        self._create_reversals(
            service,
            trust_payment_id,
            payment_args['service_order_id_list'],
            amounts=amount_list,
            actions=('clear',) * len(payment_args['service_order_id_list']),
            paymethod_markup=paymethod_markup
        )

    def process_promocode_creating(self,
                                   service, services=None, promo_status=defaults.Promocode.Status.active,
                                   name=defaults.Promocode.name,
                                   series_amount=defaults.Promocode.series_amount,
                                   promo_amount=defaults.Promocode.promocode_amount_part,
                                   quantity=defaults.Promocode.quantity,
                                   partial_only=None, full_payment_only=None, extended_response=False):
        # генерируем uuid для промокода, запоминаем сумму для дальнейшего использования
        promo_id = str(uuid4())
        with reporter.step(u'Запоминаем фейковый промокод {} на скидку "{}"'.format(promo_id, promo_amount)):
            self._promo_amounts[promo_id] = promo_amount
        return promo_id

    def none(self, *args, **kwargs):
        return

    # -------------------------------------------------------------------
    # UTILS PART
    def create_refund_for_blue_cashback(self, payment_id):
        trust_payment_id = self.get_cashback_trust_id_by_parent_id(payment_id)
        payment_args = self.get_payment_arguments(trust_payment_id)
        self._create_multiple_fake_refunds(
            service=payment_args['service'],
            service_order_id_list=payment_args['service_order_id_list'],
            trust_payment_id=trust_payment_id,
            delta_amount_list=payment_args['prices_list']
        )

    def get_refund_paymethod_markup_for_postauthorize(self, paymethod_markup, service_order_id_list, actions):
        if paymethod_markup:
            refund_markup = self.get_previous_paymethod_markup()
            for order_id, markup in paymethod_markup.items():
                order_idx = service_order_id_list.index(order_id)
                action = actions[order_idx]
                for method, amount in markup.items():
                    payment_amount = Decimal(refund_markup[order_id][method])
                    amount = payment_amount - Decimal(amount) if action == 'clear' else payment_amount
                    refund_markup[order_id][method] = str(amount)
            return refund_markup
        return self.get_previous_paymethod_markup()

    def get_remain_payment_method_amounts(self, refund_markup):
        previous_paymethod_markup = self.get_previous_paymethod_markup()
        if not previous_paymethod_markup:
            return None
        payment_method_amounts = collections.defaultdict(Decimal)
        for order_id, markup in previous_paymethod_markup.items():
            for payment_method, amount in markup.items():
                # промокоды и баллы одностадийны и клирятся сразу, не изменяем их
                if payment_method in ('virtual::new_promocode', 'spasibo'):
                    continue
                refund_amount = (refund_markup[order_id] or {}).get(payment_method, 0)
                payment_method_amounts[payment_method] += Decimal(amount) - Decimal(refund_amount)
        return payment_method_amounts

    @staticmethod
    def split_by_payment_method(markup):
        """
        Трансформируем массив разметки корзины
        в словарь из платежного метода в массив данных заказов по этому методу
        """
        paymethod_to_markups = collections.defaultdict(list)
        for order_id, amounts in markup.items():
            for method, amount in amounts.items():
                paymethod_to_markups[method].append({
                    'order_id': order_id,
                    'amount': amount
                })
        return paymethod_to_markups

    @staticmethod
    def get_children_trust_group_payments(trust_payment_id):
        payments = db.balance().execute(
            """
            select p.trust_payment_id, cp.payment_method from bo.t_payment p
                join bo.t_ccard_bound_payment cp on p.id = cp.id
            where trust_group_id = :group_id""",
            {'group_id': trust_payment_id})
        return {p['payment_method']: p['trust_payment_id'] for p in payments}

    @staticmethod
    def get_cashback_trust_id_by_parent_id(parent_id):
        rows = db.balance().execute(
            'select trust_payment_id from bo.t_payment where cashback_parent_id = :parent_id',
            {'parent_id': parent_id}
        )
        if not rows:
            raise ValueError('There is no cashback payment for {}'.format(parent_id))
        return rows[0]['trust_payment_id']

    def purchase_token_to_trust_payment_id(self, purchase_token):
        try:
            args = next(args
                        for args in self._previous_payments_arguments.values()
                        if args['purchase_token'] == purchase_token)
        except StopIteration:
            raise ValueError('Payment with purchase_token "{}" not found in this test'.format(purchase_token))
        return args['trust_payment_id']

    @staticmethod
    def get_default_paymethod(currency):
        if currency == Currencies.KZT:
            return LinkedCard(card=cards_pool.RBS.Success.Without3DS.card_mastercard)
        return LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD)

    @staticmethod
    def add_developer_payload_to_payment(payment_id, developer_payload, user):
        if not developer_payload:
            return

        db.balance().execute(
            "insert into BO.T_EXTPROPS (ID, CLASSNAME, ATTRNAME, OBJECT_ID, VALUE_CLOB, PASSPORT_ID)"
            "values (BO.S_EXTPROPS.NEXTVAL, 'Payment', 'developer_payload', "
            ":payment_id, TO_CLOB(:developer_payload), :passport_id)",
            {
                'payment_id': payment_id,
                'developer_payload': json.dumps(developer_payload),
                'passport_id': user.id_ if user else None
            }
        )

    @staticmethod
    def cancel_payment(payment_id):
        with reporter.step('Отменяем платеж "{}"'.format(payment_id)):
            db.balance().execute(
                'update bo.t_payment set cancel_dt = :cancel_dt where id = :id',
                {'id': payment_id, 'cancel_dt': datetime.now() + relativedelta(seconds=15)}
            )

    @staticmethod
    def join_payments_to_trust_group(payment_ids, group_trust_payment_id):
        # свяжем платежи в корзине через trust_group_id
        db.balance().execute(
            'update bo.t_payment set trust_group_id = :group_id where id in ({})'.format(','.join(payment_ids)),
            {'group_id': group_trust_payment_id})

    @staticmethod
    def join_cashback_payments_to_trust_group(payment_ids, parent_id):
        # свяжем платежи spasibo c родительским платежом по cashback_parent_id
        # и отвяжем все по trust_group_id
        db.balance().execute(
            '''
            update (
                select cp.payment_method, p.* from bo.t_payment p
                join bo.t_ccard_bound_payment cp on cp.id = p.id
                where p.id in ({})
            ) p set p.cashback_parent_id = :parent_id, p.payment_method_id = :payment_method_id, p.trust_group_id = null
            where p.payment_method = :payment_method
            '''.format(','.join(payment_ids)),
            {'parent_id': parent_id,
             'payment_method_id': PaymentMethods.SPASIBO_CASHBACK.id,
             'payment_method': PaymentMethods.SPASIBO_CASHBACK.cc})
        # отвяжем composite_tag
        db.balance().execute(
            '''
            update bo.t_ccard_bound_payment set composite_tag = null
            where id in ({}) and payment_method = :payment_method
            '''.format(','.join(payment_ids)),
            {'parent_id': parent_id, 'payment_method': PaymentMethods.SPASIBO_CASHBACK.cc})

    @staticmethod
    def join_payments_by_composite_tag(payment_ids, composite_tag=None):
        # свяжем платежи в корзине через composite_tag
        composite_tag = composite_tag or int(uuid4())
        db.balance().execute(
            'update bo.t_ccard_bound_payment set composite_tag = :composite_tag where id in ({})'.format(
                ','.join(payment_ids)),
            {'composite_tag': composite_tag})

    @staticmethod
    def get_service_products(service_product_id_list, region_id, currency_iso_code):
        product_ids = ("'{}'".format(p_id) for p_id in service_product_id_list)
        products = db.balance().execute(
            '''
            select product.id, product.external_id, price.price from bo.t_service_product product
            left join bo.t_service_price price
            on (price.service_product_id = product.id
                and price.region_id = :region_id and price.iso_currency = :currency)
            where product.external_id in ({})'''.format(','.join(product_ids)),
            {'region_id': region_id, 'currency': currency_iso_code})

        missed_products = set(service_product_id_list) - set(product['external_id'] for product in products)
        if missed_products:
            raise ValueError('Not found products for "{}"'.format(missed_products))

        products = {product['external_id']: product for product in products}
        return [products[service_product_id]
                for service_product_id in service_product_id_list]

    @staticmethod
    def update_payment_postauth_amount(trust_payment_id, amount, payment_method_amounts=None):
        db.balance().execute(
            'update bo.t_ccard_bound_payment set postauth_amount = :amount where trust_payment_id = :trust_payment_id',
            {'amount': amount, 'trust_payment_id': trust_payment_id})
        if not payment_method_amounts:
            return
        # для композитных платежей обновим дочерние платежи
        for payment_method, amount in payment_method_amounts.items():
            db.balance().execute(
                '''
                update bo.t_ccard_bound_payment set postauth_amount = :amount where id in (
                    select p.id from bo.t_payment p join bo.t_ccard_bound_payment cp on p.id = cp.id
                    where trust_group_id = :trust_payment_id and payment_method = :payment_method
                )
                ''',
                {'amount': amount, 'trust_payment_id': trust_payment_id, 'payment_method': payment_method})

    @staticmethod
    def get_payment_id():
        return db.balance().sequence_nextval('s_payment_id')

    @staticmethod
    def get_purchase_token():
        return str(uuid4())

    @staticmethod
    def get_service_order_id():
        return db.balance().sequence_nextval('s_request_order_id')

    @staticmethod
    def get_order_id():
        return db.balance().sequence_nextval('s_request_order_id')

    @staticmethod
    def prepare_payment_rows(service,
                             service_products,
                             service_order_id_list,
                             commission_category_list,
                             prices_list,
                             qty_list,
                             fiscal_nds_list,
                             fiscal_title_list,
                             developer_payload_list,
                             region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                             user=None,
                             order_dt=None,
                             ):
        # подготавливаем данные для вставки в t_payment.payment_rows
        dt = datetime.now()
        row_dt = dt - relativedelta(seconds=1)
        update_dt = dt - relativedelta(seconds=30)
        moscow_offset = 3
        start_dt_utc, start_dt_offset = (order_dt - relativedelta(hours=moscow_offset), moscow_offset) \
            if order_dt else (None, None)
        payment_rows = []

        for service_product, service_order_id, commission_category, price, qty, fiscal_nds, fiscal_title, developer_payload in zip(
            service_products, service_order_id_list, commission_category_list, prices_list,
            qty_list, fiscal_nds_list, fiscal_title_list, developer_payload_list
        ):
            commission_category = str(commission_category) if commission_category is not None else None
            price = str(price if price else service_product["price"])
            payment_rows.append({"fiscal_nds": fiscal_nds.name if fiscal_nds else "",
                                 "fiscal_inn": "",
                                 "fiscal_title": fiscal_title if fiscal_title else "",
                                 "fiscal_item_code": "",
                                 "fiscal_agent_type": "",
                                 "price": price,
                                 "id": FakeTrustApi.get_order_id(),
                                 "amount": str(utils.dround2(Decimal(price) * Decimal(qty))),
                                 "source_id": DEFAULT_JSON_SOURCE_ID,
                                 "cancel_dt": None,
                                 "order": {
                                     "region_id": region_id,
                                     "contract_id": None,
                                     "update_dt": time.mktime(update_dt.timetuple()),
                                     "text": None,
                                     "price": None,
                                     "service_order_id_number": DEFAULT_JSON_SERVICE_ORDER_ID_NUMBER,
                                     "start_dt_utc": time.mktime(start_dt_utc.timetuple()) if start_dt_utc else None,
                                     "developer_payload": developer_payload,
                                     "clid": None,
                                     "service_order_id": service_order_id,
                                     "service_product_id": service_product["id"],
                                     "service_product_external_id": service_product["external_id"],
                                     "start_dt_offset": start_dt_offset,
                                     "service_id": service.id,
                                     "dt": time.mktime(row_dt.timetuple()),
                                     "passport_id": user.id_ if user else None,
                                     "commission_category": commission_category},
                                 "quantity": qty})
        return payment_rows

    @staticmethod
    def get_paysys_partner_id(paymethod):
        if getattr(paymethod, 'PAYMETHOD_DASH', None):
            return paymethod.type[len(paymethod.PAYMETHOD_DASH) + 1:] or None

    @staticmethod
    def get_terminal_id_for(currency, payment_method_cc):
        # подбираем произвольный терминал с подходящим платежным методом
        rows = db.balance().execute(
            '''
            select t.id from bo.t_terminal t
                join bo.t_payment_method pm on t.payment_method_id = pm.id
                join bo.t_processing p on p.id = t.processing_id
            where t.currency = :currency and pm.cc = :cc
                and t.contract_id is null and (p.module != 'walletone_cardapi' or p.module is null)
                and rownum = 1
            ''', {'currency': currency.char_code.upper(), 'cc': payment_method_cc}
        )

        if not rows:
            raise ValueError('There is no terminal for currency {}, payment_method {}'.format(currency,
                                                                                              payment_method_cc))
        return rows[0]['id']

    @staticmethod
    def insert_payment(payment_rows,
                       service,
                       currency=Currencies.RUB,
                       price=simpleapi_defaults.DEFAULT_PRICE,
                       user=simpleapi_defaults.DEFAULT_USER,
                       paymethod=None,
                       user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                       terminal_id=None,
                       ):
        # вставляем данные по платежу в t_payment и t_ccard_bound_payment

        query_insert_to_payment = '''
            insert into t_payment (ID, DT, CREATOR_UID, PAYSYS_CODE, AMOUNT,
                                   CURRENCY, PAYMENT_DT, USER_IP, RESP_CODE, RESP_DESC, SERVICE_ID, SOURCE_SCHEME,
                                   TERMINAL_ID, TRANSACTION_ID, POSTAUTH_DT, POSTAUTH_AMOUNT,RRN,
                                   TRUST_PAYMENT_ID, PURCHASE_TOKEN, PAYMENT_ROWS, EXPORT_FROM_TRUST, CANCEL_DT,
                                   USER_ACCOUNT, APPROVAL_CODE, PAYSYS_PARTNER_ID
                                   )
            values (:payment_id, :dt, :passport_id, 'TRUST', :amount,
                    :currency, :payment_dt, :user_ip, 'success', 'paid ok', :service_id, 'bs',
                    :terminal_id, 'h62m8wiroe4dxxq0ludd', :postauth_dt, :amount, '58985',
                    :trust_payment_id, :purchase_token, TO_CLOB(:payment_rows), :export_from_trust,
                    :cancel_dt, '510000****8634', '111111', :paysys_partner_id)'''

        query_insert_ccard_bound_payment = '''
            insert into t_ccard_bound_payment (ID, PAYMENT_METHOD, TRUST_PAYMENT_ID,
                                               PURCHASE_TOKEN, START_DT,USER_PHONE,
                                               USER_EMAIL, POSTAUTH_DT, POSTAUTH_AMOUNT, RRN)
            values (:payment_id, :payment_method, :trust_payment_id,
                    :purchase_token, :dt, '+79999999999',
                    'test@test.ru', :postauth_dt, :amount, '58985')'''

        payment_id = FakeTrustApi.get_payment_id()
        purchase_token = FakeTrustApi.get_purchase_token()
        trust_payment_id = SimpleApi.generate_fake_trust_payment_id()
        dt = datetime.now().replace(microsecond=0)
        payment_dt = dt - relativedelta(seconds=4)
        postauth_dt = dt + relativedelta(seconds=8)
        paysys_partner_id = FakeTrustApi.get_paysys_partner_id(paymethod)
        payment_method = getattr(paymethod, 'via_id', None) or paymethod._id or paymethod.type
        if payment_method == 'yandex_account_withdraw':
            payment_method = 'yandex_account-w/{}'.format(uuid4())

        params = {'payment_id': payment_id,
                  'dt': dt,
                  'passport_id': user.id_ if user else None,
                  'amount': price,
                  'currency': currency.char_code.upper(),  # kzt and KZT
                  'payment_dt': payment_dt,
                  'user_ip': user_ip,
                  'service_id': service.id,
                  'postauth_dt': postauth_dt,
                  'trust_payment_id': trust_payment_id,
                  'purchase_token': purchase_token,
                  'payment_rows': json.dumps(payment_rows),
                  'export_from_trust': datetime.now(),
                  'cancel_dt': None,
                  # не хотим вызывать init платежного метода
                  'payment_method': payment_method,
                  'paysys_partner_id': paysys_partner_id,
                  'terminal_id': terminal_id or '96013105'
                  }

        db.balance().execute(query_insert_to_payment, params)
        db.balance().execute(query_insert_ccard_bound_payment, params)
        return trust_payment_id, purchase_token, payment_id

    @staticmethod
    def insert_refund(payment_id,
                      paymethod,
                      payment_rows,
                      service,
                      currency=Currencies.RUB,
                      amount=simpleapi_defaults.DEFAULT_PRICE,
                      user=simpleapi_defaults.DEFAULT_USER,
                      is_reversal=0,
                      ):
        # вставляем данные по рефанду в t_payment и t_refund
        query_insert_to_payment = '''
            insert into t_payment (ID, DT,CREATOR_UID, PAYSYS_CODE, AMOUNT,
                                   CURRENCY,PAYMENT_DT,RESP_CODE,SERVICE_ID,SOURCE_SCHEME,
                                   PAYMENT_ROWS,REFUND_TO, EXPORT_FROM_TRUST, PAYSYS_PARTNER_ID)
            values (:payment_id,:dt, :passport_id,'REFUND',:amount,
                    :currency,:payment_dt,'success',:service_id,'bs',
                    TO_CLOB(:payment_rows),'paysys', :export_from_trust, :paysys_partner_id)'''

        query_insert_refund = '''
            insert into T_REFUND (ID, DESCRIPTION, ORIG_PAYMENT_ID,
                                  TRUST_REFUND_ID, IS_REVERSAL)
            values (:payment_id, 'test1', :orig_payment_id,
                    :trust_refund_id, :is_reversal)'''

        refund_id = FakeTrustApi.get_payment_id()
        trust_refund_id = SimpleApi.generate_fake_trust_payment_id()
        dt = datetime.now().replace(microsecond=0) + relativedelta(seconds=13)
        payment_dt = dt + relativedelta(seconds=14)
        paysys_partner_id = FakeTrustApi.get_paysys_partner_id(paymethod)

        params = {'payment_id': refund_id,
                  'payment_rows': json.dumps(payment_rows),
                  'amount': amount,
                  'service_id': service.id,
                  'currency': currency.char_code.upper(),  # kzt and KZT
                  'dt': dt,
                  'payment_dt': payment_dt,
                  'payment_method': paymethod.type,
                  'orig_payment_id': payment_id,
                  'trust_refund_id': trust_refund_id,
                  'passport_id': user.id_ if user else None,
                  'export_from_trust': datetime.now(),
                  'paysys_partner_id': paysys_partner_id,
                  'is_reversal': is_reversal,
                  }

        db.balance().execute(query_insert_to_payment, params)
        db.balance().execute(query_insert_refund, params)
        return trust_refund_id, refund_id

    @staticmethod
    def create_partner(service):
        with reporter.step(u'ФЕЙКОВЫМ TRUST Создаем партнёра для сервиса {}'.format(service)):
            name = defaults.partner_info['name']
            email = defaults.partner_info['email']
            operator_uid = defaults.partner_info['operator_uid']
            p = utils.remove_empty({
                'name': name,
                'email': email,
                'phone': '+79214567323',
                'fax': '+79214567323',
                'url': 'test.url',
                'city': 'St.Petersburg'
            })
            p_medium = {k.upper(): v for k, v in p.items()}
            p_medium.update({'SERVICE_ID': service.id})
            _, _, client_id = api.medium().CreateClient(operator_uid, p_medium)
            client_id = str(client_id)
            p['operator_uid'] = operator_uid
            return {'status': 'success', 'partner_id': client_id, 'info': p}, client_id

    @staticmethod
    def create_service_product(service,
                               service_product_id=None,
                               partner_id=None,
                               shop_params=None,
                               name=defaults.product_name,
                               prices=defaults.product_prices,  #
                               parent_service_product_id=None,
                               type_='app',
                               subs_period=None,
                               subs_trial_period=None,
                               active_until_dt=None,
                               single_purchase=None,
                               bonuses=None,
                               service_fee=None,
                               subs_introductory_period=None,
                               subs_introductory_period_prices=None,
                               fiscal_nds=defaults.Fiscal.NDS.nds_none,
                               fiscal_title=defaults.Fiscal.fiscal_title,
                               processing_cc=None,
                               aggregated_charging=None):
        if not service_product_id:
            service_product_id = simple_steps.get_service_product_id(service)
        if service not in simple_steps.get_services_by_schema(ServiceSchemaParams.TRUST_PRICE, 1):
            prices = None
        with reporter.step(u'ФЕЙКОВЫМ TRUST Создаем сервисный продукт для сервиса {}: '
                           u'service_product_id={}'.format(service, service_product_id)):
            p = utils.remove_empty({
                'service_product_id': service_product_id,
                'name': name,
                'partner_id': partner_id,
                'shop_params': shop_params,
                'prices': prices,
                'product_type': type_,
                'parent_service_product_id': parent_service_product_id,
                'subs_period': subs_period,
                'subs_trial_period': subs_trial_period,
                'active_until_dt': active_until_dt,
                'single_purchase': single_purchase,
                'bonuses': bonuses,
                'service_fee': service_fee,
                'subs_introductory_period': subs_introductory_period,
                'subs_introductory_period_prices': subs_introductory_period_prices,
                'fiscal_nds': fiscal_nds,
                'fiscal_title': fiscal_title,
                'processing_cc': processing_cc,
                'aggregated_charging': aggregated_charging,
            })
            return api.test_balance().CreateServiceProduct(service.token, p)


    def _get_from_orders(self, field):
        if self._orders is None:
            return None
        return [order[field] for order in self._orders]

    @staticmethod
    def _get_currency_object(currency):
        try:
            return next(c for c in Currencies.values() if c.iso_code == currency)
        except StopIteration:
            raise ValueError('Unknown currency "{}"'.format(currency))

    @staticmethod
    def _get_discount_factor(discounts):
        max_discount = max([d for d in defaults.Discounts.values() if d['id'] in discounts], key=itemgetter('pct'))
        return Decimal(1) - max_discount['pct'] / 100

    @staticmethod
    def _convert_fiscal_nds_strings_to_objects(items):
        nds_objects = {v.name: v for v in CMNds.values()}
        return [nds_objects[nds] for nds in items]

    @staticmethod
    def _split_params_by_promo(promo_amount, prices_list, service_product_id_list, commission_category_list):
        # вычитаем промокод из стоимости
        i = 0
        while promo_amount > 0 and i < len(prices_list):
            promo_amount = promo_amount - prices_list[i]
            if promo_amount > 0:
                i += 1

        promo_prices_list, prices_list = list(prices_list[:i]), list(prices_list[i:])
        promo_service_product_id_list, service_product_id_list = \
            service_product_id_list[:i], service_product_id_list[i:]
        promo_commission_category_list, commission_category_list = \
            commission_category_list[:i], commission_category_list[i:]
        if promo_amount < 0:
            promo_prices_list.append(prices_list[0] + promo_amount)
            prices_list[0] = -promo_amount

            promo_commission_category_list.append(commission_category_list[0])
            promo_service_product_id_list.append(service_product_id_list[0])
        return (promo_prices_list, prices_list,
                promo_service_product_id_list, service_product_id_list,
                promo_commission_category_list, commission_category_list)

    @staticmethod
    def _prepare_params(service_product_id_list,
                        service_order_id_list=None,
                        commission_category_list=None,
                        prices_list=None,
                        developer_payload_list=None,
                        fiscal_nds_list=None,
                        qty_list=None,
                        fiscal_title_list=None):
        # количество продуктов не всегда честно отражает количество платежей - может быть передано больше
        # поэтому дополнительно смотрим на количество цен
        payments_count = len(service_product_id_list)
        if prices_list:
            payments_count = min(len(prices_list), payments_count)
        none_list = (None,) * payments_count
        # для промокодных платежей должны быть те же order_id то и для основных,
        # но есть случаи когда промокодных платежей больше - в этом случае нужно догенеривать строки
        service_order_id_list = service_order_id_list or []
        service_order_id_list += [FakeTrustApi.get_service_order_id()
                                  for _ in range(payments_count - len(service_order_id_list))]
        commission_category_list = commission_category_list or none_list
        prices_list = prices_list or (simpleapi_defaults.DEFAULT_PRICE,) * payments_count
        fiscal_nds_list = fiscal_nds_list or none_list
        fiscal_title_list = fiscal_title_list or none_list
        developer_payload_list = developer_payload_list or none_list
        qty_list = qty_list or (1,) * payments_count
        return (
            service_order_id_list,
            commission_category_list,
            prices_list,
            developer_payload_list,
            fiscal_nds_list,
            qty_list,
            fiscal_title_list,
        )

    def _create_child_payments_for_markup(self,
                                          service,
                                          paymethod_markup,
                                          service_product_id_list=None,
                                          service_order_id_list=None,
                                          currency=Currencies.RUB,
                                          region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                          commission_category_list=None,
                                          user=simpleapi_defaults.DEFAULT_USER,
                                          user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                                          order_dt=None,
                                          developer_payload_list=None,
                                          fiscal_nds_list=None,
                                          developer_payload_basket=None,
                                          qty_list=None,
                                          export_payment=False,
                                          ):
        paymethod_to_markups = self.split_by_payment_method(paymethod_markup)
        # для формирования данных важен порядок
        child_payment_ids = []
        # для каждого платежного метода создается отдельный платеж
        for method, order_parts in paymethod_to_markups.items():
            method_orders = []
            method_service_product_id_list = []
            method_service_order_id_list = []
            method_commission_category_list = []
            method_prices_list = []
            method_fiscal_nds_list = []
            method_qty_list = []
            method_fiscal_title_list = []
            method_developer_payload_list = []
            order_parts = {o['order_id']: o for o in order_parts}
            for order_index, o in enumerate(self._orders):
                if o['order_id'] not in order_parts:
                    continue
                part = order_parts[o['order_id']]
                order = copy.copy(self._orders[order_index])
                order['price'] = part['amount']
                method_orders.append(order)
                method_service_product_id_list.append(service_product_id_list[order_index])
                method_service_order_id_list.append(service_order_id_list[order_index])
                method_commission_category_list.append(commission_category_list[order_index])
                method_prices_list.append(order['price'])
                method_fiscal_nds_list.append(fiscal_nds_list[order_index])
                method_qty_list.append(qty_list[order_index])
                method_fiscal_title_list.append(order['fiscal_title'])
                method_developer_payload_list.append(developer_payload_list[order_index])

            if method.split('-')[0] == 'yandex_account':
                method = 'yandex_account_withdraw'

            _, _, _, ch_payment_id = self._create_multiple_fake_payments_wo_markup(
                service=service,
                service_product_id_list=method_service_product_id_list,
                service_order_id_list=method_service_order_id_list,
                currency=currency,
                region_id=region_id,
                commission_category_list=method_commission_category_list,
                prices_list=method_prices_list,
                user=user,
                paymethod=PaymethodFactory.get_instance(method),  # here
                user_ip=user_ip,
                order_dt=order_dt,
                developer_payload_list=method_developer_payload_list,
                fiscal_nds_list=method_fiscal_nds_list,
                developer_payload_basket=developer_payload_basket,
                qty_list=method_qty_list,
                export_payment=export_payment,
                fiscal_title_list=method_fiscal_title_list
            )
            child_payment_ids.append(ch_payment_id)
        return child_payment_ids

    def _create_multiple_fake_payments_with_markup(
            self,
            service,
            service_product_id_list=None,
            service_order_id_list=None,
            currency=Currencies.RUB,
            region_id=simpleapi_defaults.DEFAULT_REGION_ID,
            commission_category_list=None,
            prices_list=None,
            user=simpleapi_defaults.DEFAULT_USER,
            user_ip=simpleapi_defaults.DEFAULT_USER_IP,
            order_dt=None,
            developer_payload_list=None,
            fiscal_nds_list=None,
            developer_payload_basket=None,
            qty_list=None,
            paymethod_markup=None,
            export_payment=False,
            fiscal_title_list=None, ):
        # если заказов нет - создадим их из разметки
        if not self._orders:
            if not service_product_id_list:
                raise ValueError(u'service_product_id_list required for markup payments without orders')
            self.new_create_multiple_orders_for_payment(
                service=service,
                product_id_list=service_product_id_list,
                commission_category_list=commission_category_list,
                amount_list=[sum(map(Decimal, paymethod_markup[order_id].values()))
                             for order_id in service_order_id_list],
                service_order_ids=service_order_id_list,
            )

        service_product_id_list = self._orders_service_product_ids
        commission_category_list = self._orders_commission_category_list
        (service_order_id_list, commission_category_list, prices_list,
         developer_payload_list, fiscal_nds_list, qty_list, fiscal_title_list) = self._prepare_params(
            service_product_id_list=service_product_id_list,
            service_order_id_list=service_order_id_list,
            commission_category_list=commission_category_list,
            prices_list=prices_list,
            developer_payload_list=developer_payload_list,
            fiscal_nds_list=fiscal_nds_list,
            qty_list=qty_list,
            fiscal_title_list=fiscal_title_list,
        )

        # создаем родительский композитный платеж
        gr_service_order_id_list, gr_trust_payment_id, gr_purchase_token, gr_payment_id = \
            self._create_multiple_fake_payments_wo_markup(
                service=service,
                service_product_id_list=service_product_id_list,
                service_order_id_list=service_order_id_list,
                currency=currency,
                region_id=region_id,
                commission_category_list=commission_category_list,
                prices_list=prices_list,
                user=user,
                paymethod=PaymethodFactory.get_instance(TYPE.COMPOSITE),
                user_ip=user_ip,
                order_dt=order_dt,
                developer_payload_list=developer_payload_list,
                fiscal_nds_list=fiscal_nds_list,
                developer_payload_basket=developer_payload_basket,
                qty_list=qty_list,
                export_payment=export_payment,
                fiscal_title_list=fiscal_title_list
            )

        # для синего маркета создаем платежи spasibo_cashback
        real_paymethod_markup = copy.deepcopy(paymethod_markup)
        if service.id == Services.BLUE_MARKET_PAYMENTS.id and real_paymethod_markup:
            first_order = real_paymethod_markup.values()[0]  # real_paymethod_markup[real_paymethod_markup.keys()[0]]
            if 'spasibo' in first_order:
                first_order['spasibo_cashback'] = '1'

        child_payment_ids = self._create_child_payments_for_markup(
            service=service,
            paymethod_markup=real_paymethod_markup,
            service_product_id_list=service_product_id_list,
            service_order_id_list=service_order_id_list,
            currency=currency,
            region_id=region_id,
            commission_category_list=commission_category_list,
            user=user,
            user_ip=user_ip,
            order_dt=order_dt,
            developer_payload_list=developer_payload_list,
            fiscal_nds_list=fiscal_nds_list,
            developer_payload_basket=developer_payload_basket,
            qty_list=qty_list,
            export_payment=export_payment,
        )

        self.join_payments_to_trust_group(child_payment_ids, gr_trust_payment_id)
        self.join_payments_by_composite_tag(child_payment_ids)
        self.join_cashback_payments_to_trust_group(child_payment_ids, gr_payment_id)
        self._previous_paymethod_markup = paymethod_markup
        return gr_service_order_id_list, gr_trust_payment_id, gr_purchase_token, gr_payment_id

    def _create_multiple_fake_payments_wo_markup(self,
                                                 service,
                                                 service_product_id_list,
                                                 service_order_id_list=None,
                                                 currency=Currencies.RUB,
                                                 region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                                 commission_category_list=None,
                                                 prices_list=None,
                                                 user=simpleapi_defaults.DEFAULT_USER,
                                                 paymethod=None,
                                                 user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                                                 order_dt=None,
                                                 developer_payload_list=None,
                                                 fiscal_nds_list=None,
                                                 developer_payload_basket=None,
                                                 qty_list=None,
                                                 export_payment=False,
                                                 fiscal_title_list=None,
                                                 ):
        with reporter.step(u'ФЕЙКОВЫМ TRUST Создаем платеж для сервиса: {} и продуктов: {}.'.format(
                service.name, service_product_id_list)):
            # готовим дефолтные параметры
            service_products = FakeTrustApi.get_service_products(service_product_id_list, region_id,
                                                                 currency.iso_code)
            paymethod = paymethod or LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD)
            (service_order_id_list, commission_category_list, prices_list,
             developer_payload_list, fiscal_nds_list, qty_list, fiscal_title_list) = self._prepare_params(
                service_product_id_list=service_product_id_list,
                service_order_id_list=service_order_id_list,
                commission_category_list=commission_category_list,
                prices_list=prices_list,
                developer_payload_list=developer_payload_list,
                fiscal_nds_list=fiscal_nds_list,
                qty_list=qty_list,
                fiscal_title_list=fiscal_title_list,
            )

            # подготавливаем данные для t_payment.payment_rows
            payment_rows = FakeTrustApi.prepare_payment_rows(
                service,
                service_products,
                service_order_id_list,
                commission_category_list,
                prices_list,
                qty_list,
                fiscal_nds_list,
                fiscal_title_list,
                developer_payload_list,
                region_id,
                user,
                order_dt,
            )

            total = sum(map(Decimal, [row['amount'] for row in payment_rows]))
            terminal_id = self.get_terminal_id_for(currency, re.split('[-:]', paymethod.type)[0])
            # вставляем данные по платежу в t_payment и t_ccard_bound_payment
            trust_payment_id, purchase_token, payment_id = FakeTrustApi.insert_payment(
                payment_rows,
                service,
                currency,
                total,
                user,
                paymethod,
                user_ip,
                terminal_id
            )

            # добавляем developer_payload к платежу если он передан
            FakeTrustApi.add_developer_payload_to_payment(payment_id, developer_payload_basket, user)

            # всталяем строку в t_export для созданного платежа
            ExportSteps.create_export_record(payment_id, Export.Classname.PAYMENT,
                                             Export.Type.THIRDPARTY_TRANS)
            if export_payment:
                # разбираем платеж в t_export
                CommonPartnerSteps.export_payment(payment_id)

            self.save_payment_arguments(trust_payment_id, locals())
            return service_order_id_list, trust_payment_id, purchase_token, payment_id

    def _create_multiple_fake_payments(self,
                                       service,
                                       service_product_id_list,
                                       service_order_id_list=None,
                                       currency=Currencies.RUB,
                                       region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                       commission_category_list=None,
                                       prices_list=None,
                                       user=simpleapi_defaults.DEFAULT_USER,
                                       paymethod=None,
                                       user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                                       order_dt=None,
                                       developer_payload_list=None,
                                       fiscal_nds_list=None,
                                       developer_payload_basket=None,
                                       qty_list=None,
                                       paymethod_markup=None,
                                       export_payment=False,
                                       fiscal_title_list=None,
                                       ):
        with reporter.step(u'ФЕЙКОВЫМ TRUST Создаем платеж для сервиса: {} и продуктов: {}.'.format(
                service.name, service_product_id_list)):
            if paymethod_markup:
                return self._create_multiple_fake_payments_with_markup(
                    service=service,
                    service_product_id_list=service_product_id_list,
                    service_order_id_list=service_order_id_list,
                    currency=currency,
                    region_id=region_id,
                    commission_category_list=commission_category_list,
                    prices_list=prices_list,
                    user=user,
                    user_ip=user_ip,
                    order_dt=order_dt,
                    developer_payload_list=developer_payload_list,
                    fiscal_nds_list=fiscal_nds_list,
                    developer_payload_basket=developer_payload_basket,
                    qty_list=qty_list,
                    paymethod_markup=paymethod_markup,
                    export_payment=export_payment,
                    fiscal_title_list=fiscal_title_list
                )
            return self._create_multiple_fake_payments_wo_markup(
                service=service,
                service_product_id_list=service_product_id_list,
                service_order_id_list=service_order_id_list,
                currency=currency,
                region_id=region_id,
                commission_category_list=commission_category_list,
                prices_list=prices_list,
                user=user,
                paymethod=paymethod,
                user_ip=user_ip,
                order_dt=order_dt,
                developer_payload_list=developer_payload_list,
                fiscal_nds_list=fiscal_nds_list,
                developer_payload_basket=developer_payload_basket,
                qty_list=qty_list,
                export_payment=export_payment,
                fiscal_title_list=fiscal_title_list
            )

    def _create_multiple_fake_refunds_wo_markup(self,
                                                service,
                                                service_order_id_list,
                                                trust_payment_id,
                                                delta_amount_list=None,
                                                export_payment=False,
                                                is_reversal=0,
                                                ):
        payment_args = self.get_payment_arguments(trust_payment_id)
        service_products = payment_args['service_products']
        commission_category_list = payment_args['commission_category_list']
        qty_list = payment_args['qty_list']
        fiscal_nds_list = payment_args['fiscal_nds_list']
        fiscal_title_list = payment_args['fiscal_title_list']
        developer_payload_list = payment_args['developer_payload_list']
        prices_list = payment_args['prices_list']

        if not service_order_id_list:
            service_order_id_list = payment_args['service_order_id_list']
        else:
            # порядок для правильного выбора параметров
            service_order_id_index = {service_order_id: payment_args['service_order_id_list'].index(service_order_id)
                                      for service_order_id in service_order_id_list}
            service_products = [service_products[service_order_id_index[s_id]] for s_id in service_order_id_list]
            commission_category_list = [commission_category_list[service_order_id_index[s_id]] for s_id in service_order_id_list]
            qty_list = [qty_list[service_order_id_index[s_id]] for s_id in service_order_id_list]
            fiscal_nds_list = [fiscal_nds_list[service_order_id_index[s_id]] for s_id in service_order_id_list]
            fiscal_title_list = [fiscal_title_list[service_order_id_index[s_id]] for s_id in service_order_id_list]
            developer_payload_list = [developer_payload_list[service_order_id_index[s_id]] for s_id in service_order_id_list]
            prices_list = [prices_list[service_order_id_index[s_id]] for s_id in service_order_id_list]

        delta_amount_list = delta_amount_list or prices_list

        with reporter.step(
                u'ФЕЙКОВЫМ TRUST Создаем возврат для сервиса: {} и заказов: {} с платежом: {}.'.format(
                    service.name, service_order_id_list, trust_payment_id)):
            payment_rows = FakeTrustApi.prepare_payment_rows(service,
                                                             service_products,
                                                             service_order_id_list,
                                                             commission_category_list,
                                                             delta_amount_list,
                                                             qty_list,
                                                             fiscal_nds_list,
                                                             fiscal_title_list,
                                                             developer_payload_list,
                                                             payment_args['region_id'],
                                                             payment_args['user'],
                                                             payment_args['order_dt'],
                                                             )
            # вставляем данные по рефанду в t_payment и t_refund
            trust_refund_id, refund_id = FakeTrustApi.insert_refund(payment_args['payment_id'],
                                                                    payment_args['paymethod'],
                                                                    payment_rows,
                                                                    service,
                                                                    payment_args['currency'],
                                                                    sum(map(Decimal, delta_amount_list)),
                                                                    payment_args['user'],
                                                                    is_reversal=is_reversal,
                                                                    )
            FakeTrustApi.cancel_payment(payment_args['payment_id'])
            # всталяем строку в t_export для созданного рефанда
            ExportSteps.create_export_record(refund_id, Export.Classname.PAYMENT,
                                             Export.Type.THIRDPARTY_TRANS)
            # разбираем рефанд в t_export
            if export_payment:
                CommonPartnerSteps.export_payment(refund_id)
            return trust_refund_id, refund_id

    def _create_child_refunds_for_markup(self,
                                         service,
                                         paymethod_markup,
                                         trust_payment_id,
                                         export_payment=False,
                                         is_reversal=0,
                                         ):
        paymethod_to_markups = self.split_by_payment_method(paymethod_markup)
        paymethod_to_trust_payment_id = self.get_children_trust_group_payments(trust_payment_id)
        # для каждого из возвращаемых платежей создадим возврат на требуемые суммы
        for method, orders in paymethod_to_markups.items():
            ch_service_order_id_list = [o['order_id'] for o in orders]
            ch_delta_amount_list = [o['amount'] for o in orders]
            ch_trust_payment_id = paymethod_to_trust_payment_id[method]
            self._create_multiple_fake_refunds_wo_markup(
                service,
                ch_service_order_id_list,
                ch_trust_payment_id,
                delta_amount_list=ch_delta_amount_list,
                export_payment=export_payment,
                is_reversal=is_reversal
            )

    def _create_multiple_fake_refunds_with_markup(self,
                                                  service,
                                                  service_order_id_list,
                                                  trust_payment_id,
                                                  delta_amount_list=None,
                                                  export_payment=False,
                                                  paymethod_markup=None,
                                                  is_reversal=0,
                                                  ):
        assert self._orders, u'Orders required for markup payments'
        # создаем возврат к композитному (родительском платежу)
        orig = self._create_multiple_fake_refunds_wo_markup(
            service,
            service_order_id_list,
            trust_payment_id,
            delta_amount_list=delta_amount_list,
            export_payment=export_payment,
            is_reversal=is_reversal
        )

        self._create_child_refunds_for_markup(
            service,
            paymethod_markup,
            trust_payment_id,
            export_payment,
            is_reversal=is_reversal,
        )

        return orig

    def _create_multiple_fake_refunds(self,
                                      service,
                                      service_order_id_list,
                                      trust_payment_id,
                                      delta_amount_list=None,
                                      export_payment=False,
                                      paymethod_markup=None,
                                      is_reversal=0,
                                      ):
        with reporter.step(
                u'ФЕЙКОВЫМ TRUST Создаем возврат для сервиса: {} и заказов: {} с платежом: {}.'.format(
                    service.name, service_order_id_list, trust_payment_id)):
            if paymethod_markup:
                return self._create_multiple_fake_refunds_with_markup(
                    service=service,
                    service_order_id_list=service_order_id_list,
                    trust_payment_id=trust_payment_id,
                    delta_amount_list=delta_amount_list,
                    export_payment=export_payment,
                    paymethod_markup=paymethod_markup,
                    is_reversal=is_reversal,
                )
            return self._create_multiple_fake_refunds_wo_markup(
                service=service,
                service_order_id_list=service_order_id_list,
                trust_payment_id=trust_payment_id,
                delta_amount_list=delta_amount_list,
                export_payment=export_payment,
                is_reversal=is_reversal
            )

    def _create_reversals(self,
                          service,
                          trust_payment_id,
                          service_order_id_list,
                          amounts=None,
                          actions=None,
                          paymethod_markup=None):
        if not actions:
            raise ValueError('Actions required')
        # если clear - то создаем reversal, оставив указанную сумму
        # если другое - создаем reversal на всю сумму платежа
        payment_args = self.get_payment_arguments(trust_payment_id)
        prices_list = map(Decimal, payment_args['prices_list'])
        # если не передано - возвращаем весь платеж
        amounts = amounts or (0,) * len(prices_list)
        delta_amounts = [prices_list[i] - amount if actions[i] == 'clear' else prices_list[i]
                         for i, amount in enumerate(amounts)]

        # заказы, для которых не будет создан возврат исключаем
        service_order_id_list = [service_order_id
                                 for i, service_order_id in enumerate(service_order_id_list)
                                 if delta_amounts[i] != 0]
        delta_amounts = [amount for amount in delta_amounts if amount != 0]

        refund_markup = self.get_refund_paymethod_markup_for_postauthorize(paymethod_markup, service_order_id_list, actions)
        remain_payment_method_amounts = self.get_remain_payment_method_amounts(refund_markup)
        # изменим заклиренную сумму платежа
        self.update_payment_postauth_amount(
            trust_payment_id,
            sum(amount for i, amount in enumerate(amounts) if actions[i] == 'clear'),
            remain_payment_method_amounts,
        )

        self._create_multiple_fake_refunds(
            service=service,
            service_order_id_list=service_order_id_list,
            trust_payment_id=trust_payment_id,
            paymethod_markup=refund_markup,
            delta_amount_list=delta_amounts,
            is_reversal=1
        )
