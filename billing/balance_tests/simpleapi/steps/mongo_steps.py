# coding=utf-8
import datetime
import time

import mongoengine as me
from hamcrest import is_, equal_to

from btestlib import environments
from btestlib import reporter
from btestlib import utils as butils

me.connect('trust',
           host=environments.simpleapi_env().mongo_url.format('f'),
           port=27217,
           username=environments.simpleapi_env().mongo_usr,
           password=environments.simpleapi_env().mongo_pwd)


def query_(Document, only_first=True, **params):
    cursor = Document.objects(**params)
    reporter.attach(label='Mongo query', attachment=cursor._query)
    if only_first:
        result = cursor.first()
    else:
        result = cursor
    reporter.attach(label='Mongo result', attachment=result)
    return result


class Refund(me.Document):
    meta = {'strict': False}

    uid = me.IntField()
    stage = me.StringField(default='initial')
    _terminal_id = me.IntField(db_field='terminal')
    amount = me.DecimalField()
    currency = me.StringField()
    reason_desc = me.StringField()
    pass_params = me.DictField()
    payment_id = me.ObjectIdField()
    transaction_id = me.StringField()
    orders = me.ListField()
    fiscal_data = me.DictField()
    dt = me.DateTimeField()
    update_dt = me.DateTimeField()
    queue_obj = me.ObjectIdField()
    refund_timeout = me.IntField()
    start_dt = me.DateTimeField()
    need_check = me.BooleanField()
    refund_result_dt = me.DateTimeField()
    refund_result = me.StringField()
    refund_result_desc = me.StringField()

    def __str__(self):
        return 'stage={stage}, ' \
               'amount={amount}, ' \
               'payment_id={payment_id}, ' \
               'refund_result={refund_result}, ' \
               'refund_result_desc={refund_result_desc}'.format(stage=self.stage,
                                                                amount=self.amount,
                                                                payment_id=self.payment_id,
                                                                refund_result=self.refund_result,
                                                                refund_result_desc=self.refund_result_desc)

    @staticmethod
    def wait_until_done(trust_payment_id):
        with reporter.step(u'Ждём, пока в Mongo проставится рефанд...'):
            return butils.wait_until(lambda: Refund.get_refund_result(trust_payment_id),
                                     success_condition=is_(equal_to('success')),
                                     failure_condition=is_('error'),
                                     timeout=4 * 60)

    @staticmethod
    def find_by_payment(trust_payment_id):
        with reporter.step(u'Ищем в MongoDB рефанд по платежу {}'.format(trust_payment_id)):
            return query_(Refund, payment_id=trust_payment_id)

    @staticmethod
    def get_refund_result(trust_payment_id):
        refund = Refund.find_by_payment(trust_payment_id)
        return refund.refund_result


class Payment(me.Document):
    meta = {'strict': False}

    stages = ('before_payment', 'discount_payment', 'register_payment',
              'do_payment', 'wait_for_result', 'after_payment',
              'fiscal_receipt', 'send_response'
              )

    uid = me.IntField()
    token = me.StringField()
    payment_method = me.StringField()
    payment_mode = me.StringField(choices=('api_payment', 'web_payment',
                                           'external_web_payment',
                                           'invoicing'))
    sum = me.DecimalField()
    commission = me.DecimalField()
    currency = me.StringField()
    region_id = me.IntField()
    state = me.StringField(default='initial',
                           choices=('initial', 'done') + stages)

    payment_result = me.StringField()
    binding_result = me.StringField()
    error_desc = me.StringField()
    error_method = me.StringField()
    purchase_token = me.StringField()
    description = me.StringField()
    card_id = me.ObjectIdField()

    _terminal_id = me.IntField(db_field='terminal')
    processing_cc = me.StringField()
    payment_method_cc = me.StringField()

    dt = me.DateTimeField()
    start_dt = me.DateTimeField()
    update_dt = me.DateTimeField()

    def __str__(self):
        return 'id={id}, ' \
               'sum={sum}, ' \
               'currency={currency}, ' \
               'state={state}, ' \
               'error_desc={error_desc}'.format(id=self.id,
                                                sum=self.sum,
                                                currency=self.currency,
                                                state=self.state,
                                                error_desc=self.error_desc)

    @staticmethod
    def find_by(trust_payment_id=None, purchase_token=None, first=True):
        if trust_payment_id:
            with reporter.step(u'Ищем в MongoDB платеж {}'.format(trust_payment_id)):
                return query_(Payment, only_first=first, id=trust_payment_id)
        elif purchase_token:
            with reporter.step(u'Ищем в MongoDB платеж {}'.format(purchase_token)):
                return query_(Payment, only_first=first, purchase_token=purchase_token)

    @staticmethod
    def get_id_by_purchase_token(purchase_token):
        payment = Payment.find_by(purchase_token=purchase_token)
        return payment.id

    @staticmethod
    def get_card_id_from_payment(trust_payment_id):
        payment = Payment.find_by(trust_payment_id=trust_payment_id)
        return payment.card_id

    @staticmethod
    def get_error_params_from_payment(trust_payment_id):
        payment = Payment.find_by(trust_payment_id=trust_payment_id)
        return payment.error_desc, payment.payment_result

    @staticmethod
    def get_start_dt_by_purchase_token(purchase_token):
        payment = Payment.find_by(purchase_token=purchase_token)
        return payment.start_dt

    @staticmethod
    def update_data(data_to_update, trust_payment_id=None, purchase_token=None):
        payment = Payment.find_by(trust_payment_id, purchase_token, first=False)
        with reporter.step(u'Меняем данные платежа в MongoDB {}'.format(data_to_update)):
            result = payment.update_one(**data_to_update)
            reporter.log(u'Update result: {}'.format(result))

    @staticmethod
    def clear_all_of_user(user):
        payments = Payment.objects(uid=user.uid)
        reporter.log('{} payments of user {} will be remove from payment collection'.format(len(payments), user))
        return payments.delete()


class Processing(me.EmbeddedDocument):
    meta = {'strict': False}

    cc = me.StringField()
    env_type = me.StringField()
    passport_type = me.StringField()
    web_payment = me.IntField()
    api_payment = me.IntField()
    direct_card_payment = me.IntField()


class ServiceTerminal(me.EmbeddedDocument):
    meta = {'strict': False}

    service_id = me.IntField()
    service_cc = me.StringField()


class ServiceLabel(me.EmbeddedDocument):
    meta = {'strict': False}

    service_id = me.IntField()
    label = me.StringField(max_length=1024,
                           min_length=3)
    dt = me.DateTimeField(default=datetime.datetime.utcnow)


class Terminal(me.Document):
    meta = {'strict': False}

    id = me.IntField(primary_key=True)
    processing = me.EmbeddedDocumentField(Processing)
    services = me.ListField(me.EmbeddedDocumentField(ServiceTerminal))
    payment_method_cc = me.StringField()
    dt = me.DateTimeField()
    update_dt = me.DateTimeField()
    currency = me.StringField()
    firm_id = me.IntField()
    partner_id = me.IntField()
    service_product_id = me.IntField()
    description = me.StringField()
    min_amount = me.DecimalField()
    max_amount = me.DecimalField()
    pri_id = me.StringField()
    sec_id = me.StringField()
    aux_id = me.StringField()

    @me.queryset_manager
    def get_by_id(self, queryset, _id):
        return queryset.filter(id=_id).first()

    @me.queryset_manager
    def get_all_by(self, queryset, **kwargs):
        return queryset.filter(**kwargs)

    def __str__(self):
        return '%s(id=%s, firm_id=%s, partner_id=%s,' \
               ' currency=%s, pri_id=%s, sec_id=%s, aux_id=%s)' % (
                   self.__class__.__name__, self.id, self.firm_id, self.partner_id,
                   self.currency, self.pri_id, self.sec_id, self.aux_id
               )


class Card(me.Document):
    meta = {'strict': False}

    token = me.StringField()  # encrypted
    card_token = me.StringField()  # encrypted
    binding_hash = me.StringField()
    card_number = me.StringField()  # masked
    dt = me.DateTimeField()
    update_dt = me.DateTimeField()
    system = me.StringField()
    holder = me.StringField()
    expiration_month = me.StringField()
    expiration_year = me.StringField()
    blocking_reason = me.StringField()
    service_labels = me.ListField(me.EmbeddedDocumentField(ServiceLabel))

    def __str__(self):
        return 'card_number={card_number}, ' \
               'blocking_reason={blocking_reason}'.format(card_number=self.card_number,
                                                          blocking_reason=self.blocking_reason)

    @staticmethod
    def find(trust_payment_id, first=False):
        card_id = Payment.get_card_id_from_payment(trust_payment_id)
        with reporter.step(u'Ищем карту по card_id={}'.format(card_id)):
            return query_(Card, only_first=first, id=card_id)

    @staticmethod
    def update_data(trust_payment_id, data_to_update, wait=True):
        mongo_card = Card.find(trust_payment_id)
        with reporter.step(u'Меняем данные карты в MongoDB {}'.format(data_to_update)):
            result = mongo_card.update_one(**data_to_update)
            reporter.log(u'Update result: {}'.format(result))
        if wait:
            time.sleep(2 * 60)

    @staticmethod
    def unset_token(trust_payment_id):
        mongo_card = Card.find(trust_payment_id)
        with reporter.step(u'Удаляем карточный токен из MongoDB'):
            mongo_card.update_one(unset__card_token=1)

    @staticmethod
    def get_ym_and_card_tokens(trust_payment_id):
        mongo_card = Card.find(trust_payment_id, first=True)
        with reporter.step(u'Получаем карточный и ЯндексДеньговый токены карты'):
            return mongo_card.token, mongo_card.card_token

    @staticmethod
    def update_binding_hash(trust_payment_id, end_string='new'):
        mongo_card = Card.find(trust_payment_id, first=True)
        with reporter.step(u'Добавляем к концу card_token: {} в MongoDB'.format(end_string)):
            mongo_card.binding_hash += end_string
            mongo_card.save()


class PaymentQueue(me.Document):
    meta = {'strict': False}

    state = me.StringField()
    rate = me.IntField(default=0)
    pos = me.IntField()
    pid = me.IntField()
    doc = me.ReferenceField(Terminal)
    hostname = me.StringField()
    update_dt = me.DateTimeField()
    dt = me.DateTimeField()
    next_dt = me.DateTimeField()
    exception = me.StringField()

    @staticmethod
    def clear():
        with reporter.step(u'Чистим очередь платежей в mongo'):
            objs = PaymentQueue.objects()
            reporter.log('{} objects will be remove from payment_queue'.format(len(objs)))
            objs.delete()


class Bindings(me.Document):
    meta = {'strict': False}

    id = me.IntField(primary_key=True)
    dt = me.DateTimeField()
    update_dt = me.DateTimeField()
    new_cards_v2 = me.ListField(me.DictField())

    @staticmethod
    def clear_for_(users):
        uids = [user.uid for user in users]

        with reporter.step(u'Чистим привязки для пользователей {}'.format(users)):
            objs = Bindings.objects(id__in=uids)
            reporter.log('{} objects will be remove from bindings'.format(len(objs)))
            objs.delete()
            
    @staticmethod
    def find(user, first=True):
        uid = user.uid
        with reporter.step(u'Ищем привязку у пользователя = {} в MongoDB'.format(user.uid)):
            return query_(Bindings, only_first=first, id=uid)

    @staticmethod
    def find_card(card, binding=None, user=None):
        if not binding and user:
            binding = Bindings.find(user)
        for binding_card in binding.new_cards_v2:
            if str(binding_card['card']) == card[6::]:
                return binding_card
        return None

    @staticmethod
    def delete_card(card_id=None, mongo_card=None, user=None, binding=None):
        with reporter.step(u'Удаляем карту card = {} у пользователя uid = {} в MongoDB'.format(
                str(mongo_card) if mongo_card else card_id, user if user else binding.id)):
            if not binding and user:
                binding = Bindings.find(user)
            if not mongo_card and card_id:
                mongo_card = Bindings.find_card(card_id, binding)
            try:
                binding.new_cards_v2.remove(mongo_card)
                binding.save()
            except ValueError:
                reporter.logger().info(u'Карта уже отсутствует в коллекции MongoDB')

    @staticmethod
    def add_card(card_id=None, mongo_card=None, user=None, binding=None):
        with reporter.step(u'Добавляем карту card = {} пользователю uid = {} в MongoDB'.format(
                str(mongo_card) if mongo_card else card_id, user if user else binding.id)):
            if not binding and user:
                binding = Bindings.find(user)
            if not mongo_card and card_id:
                mongo_card = Bindings.find_card(card_id, binding)
            binding.new_cards_v2.append(mongo_card)
            binding.save()

    @staticmethod
    def change_card_holder(card_holder, new_card_holder, card):
        with reporter.step(u'Меняем владельца карты в MongoDB с {} на {}'.format(card_holder, new_card_holder)):
            binding = Bindings.find(card_holder)
            binding_card = Bindings.find_card(card, binding=binding)
            Bindings.add_card(mongo_card=binding_card, user=new_card_holder)
            Bindings.delete_card(mongo_card=binding_card, binding=binding)

    @staticmethod
    def delete_all_cards(user=None, binding=None):
        with reporter.step(u'Удаляем все карты в MongoDB у пользователя uid = {}'.format(user if user else binding.id)):
            if not binding and user:
                binding = Bindings.find(user)
            try:
                binding.new_cards_v2 = list()
                binding.save()
            except AttributeError:
                reporter.logger().info(u'У пользователя еще никогда не было привязок в Mongo DB')
                

class PaymentMethodsCache(me.Document):
    meta = {'strict': False}

    uid = me.IntField()

    @staticmethod
    def find_cache(user, first=False):
        with reporter.step(u'Ищем кеш записи в MongoDB для пользователя uid = {}'.format(user)):
            return query_(PaymentMethodsCache, only_first=first, uid=user.uid)

    @staticmethod
    def clean_lpm_cache_for_user(user):
        with reporter.step(u'Удаляем все записи из кеша в MongoDB для пользователя uid = {} '.format(user)):
            cache = PaymentMethodsCache.find_cache(user)
            cache.delete()
