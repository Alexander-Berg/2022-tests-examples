# coding=utf-8
__author__ = 'igogor'

import collections
import datetime
import json
from decimal import Decimal, ROUND_UP, ROUND_HALF_UP

from hamcrest import has_item, has_entries, equal_to
from common_steps import CommonSteps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import PromocodeClass, Users


class PromocodeSteps(object):
    @staticmethod
    def is_request_with_promo(promocode_id, request_id):
        code_in_request = db.get_request_by_id(request_id)[0]['promo_code_id']
        return promocode_id == code_in_request

    @staticmethod
    def delete_promocode(promocode_id):
        db.balance().execute('DELETE FROM t_promo_code WHERE id = :id', {'id': promocode_id})

    @staticmethod
    def is_invoice_with_promo(promocode_id, invoice_id):
        code_in_invoice = db.get_invoice_by_id(invoice_id)[0]['promo_code_id']
        return promocode_id == code_in_invoice

    @staticmethod
    def create_new(calc_class_name=None, end_dt=None, promocodes=None, start_dt=None, calc_params=None,
                   minimal_amounts=None, firm_id=None, is_global_unique=True, valid_until_paid=0,
                   service_ids=None, new_clients_only=1, need_unique_urls=0, reservation_days=0,
                   skip_reservation_check=False):
        promocodes = promocodes or []
        minimal_amounts = minimal_amounts or {}
        params = {'CalcClassName': calc_class_name,
                  'EndDt': end_dt,
                  'Promocodes': [{'code': promocode} for promocode in promocodes],
                  'StartDt': start_dt,
                  'CalcParams': calc_params,
                  'MinimalAmounts': minimal_amounts,
                  'FirmId': firm_id,
                  'IsGlobalUnique': is_global_unique,
                  'ValidUntilPaid': valid_until_paid,
                  'ServiceIds': service_ids,
                  'NewClientsOnly': new_clients_only,
                  'NeedUniqueUrls': need_unique_urls,
                  'ReservationDays': reservation_days,
                  'SkipReservationCheck': skip_reservation_check,
                  }
        if service_ids is None:
            del params['ServiceIds']
        api.medium().ImportPromoCodes([params])
        response = []
        for promocode in promocodes:
            promocode_id = db.get_promocode_by_code(promocode)[0]['id']
            response.append({'id': promocode_id, 'code': promocode})
        return response

    @staticmethod
    def create(start_dt, bonus1, bonus2, minimal_qty, middle_dt=None, end_dt=None, code=None, new_clients_only=0,
               series=0, discount_pct=0, valid_until_paid=1, any_same_series=0, is_global_unique=0,
               event_id=1, need_unique_urls=0, firm_id=1, reservation_days=0, multicurrency_bonuses=None,
               ticket_id=None):
        if not end_dt:
            end_dt = start_dt + datetime.timedelta(days=1)
        if middle_dt is None:
            middle_dt = start_dt + datetime.timedelta(seconds=1)
        if not code:
            code = 'TEST' + utils.generate_alfanumeric_string(12).upper()
        id = CommonSteps.next_sequence_id('s_promo_code_id')
        # TODO: таким образом написанный запрос заставляет поддерживать каждое новое НЕобязательное поле
        query = '''
        INSERT INTO t_promo_code (id, code, start_dt, end_dt, discount_pct, series, new_clients_only, valid_until_paid, 
        any_same_series, middle_dt, bonus1, bonus2, is_global_unique, event_id, need_unique_urls, firm_id, 
        reservation_days, minimal_qty, multicurrency_bonuses, ticket_id) 
        VALUES (:id, :code, :start_dt, :end_dt, :discount_pct, :series, :new_clients_only, :valid_until_paid,
        :any_same_series, :middle_dt, :bonus1, :bonus2, :is_global_unique, :event_id, :need_unique_urls, :firm_id, 
        :reservation_days, :minimal_qty, :multicurrency_bonuses, :ticket_id)
        '''
        query_params = {'id': id, 'code': code, 'start_dt': start_dt, 'end_dt': end_dt, 'discount_pct': discount_pct,
                        'series': series,
                        'new_clients_only': new_clients_only, 'valid_until_paid': valid_until_paid,
                        'any_same_series': any_same_series,
                        'middle_dt': middle_dt, 'bonus1': bonus1, 'bonus2': bonus2,
                        'is_global_unique': is_global_unique,
                        'event_id': event_id, 'need_unique_urls': need_unique_urls, 'firm_id': firm_id,
                        'reservation_days': reservation_days,
                        'minimal_qty': minimal_qty, 'multicurrency_bonuses': multicurrency_bonuses,
                        'ticket_id': ticket_id}
        db.balance().execute(query, query_params)
        reporter.attach('Promocode with id {0} and code {1} was created!'.format(id, code))
        return id

    @staticmethod
    def generate_code():
        return 'TEST' + utils.generate_alfanumeric_string(12).upper()

    @staticmethod
    def fill_calc_params(promocode_type, services_list=[], middle_dt=None, multicurrency_bonus1=None,
                         multicurrency_bonus2=None, convert_currency=None,
                         discount_pct=None, bonus1=None, bonus2=None, currency=None,
                         scale_points=None, sum_=0, reference_currency=None, minimal_qty=None,
                         multicurrency_minimal_qty=0):
        if promocode_type in (PromocodeClass.FIXED_QTY, PromocodeClass.LEGACY_PROMO):
            return {"middle_dt": middle_dt,
                    "multicurrency_bonuses":
                        {currency:
                             {"bonus1": multicurrency_bonus1,
                              "bonus2": multicurrency_bonus2,
                              'minimal_qty': multicurrency_minimal_qty}} if currency else {},
                    "discount_pct": discount_pct,
                    "bonus1": bonus1,
                    "bonus2": bonus2,
                    "minimal_qty": minimal_qty}
        elif promocode_type == PromocodeClass.FIXED_DISCOUNT:
            return {'discount_pct': discount_pct}
        elif promocode_type == PromocodeClass.FIXED_SUM:
            return {"currency_bonuses": {currency: sum_},
                    "reference_currency": reference_currency}
        elif promocode_type == PromocodeClass.SCALE:
            return {'currency': currency,
                    'convert_currency': convert_currency,
                    'scale_points': scale_points}

    @staticmethod
    def make_global_unique(promocode_id):
        query = '''
            UPDATE T_PROMO_CODE SET IS_GLOBAL_UNIQUE = 0 WHERE id = :id
            '''
        query_params = {'id': promocode_id}
        db.balance().execute(query, query_params)
        reporter.attach('Promocode {0} is global unique now'.format(promocode_id))

    @staticmethod
    def is_linked(promocode_id, client_id, begin_dt):
        reservation_list = db.get_promocodes_reservation_by_promocode_id(promocode_id)

        return has_item(has_entries({
            'begin_dt': begin_dt,
            'client_id': client_id,
            'promocode_id': promocode_id
        })).matches(reservation_list)

    @staticmethod
    def insert_reservation(client_id, promocode_id, begin_dt, end_dt=None):
        query = '''
            INSERT INTO t_promo_code_reservation(client_id, promocode_id, begin_dt, end_dt) VALUES (:client_id, :promocode_id, :begin_dt, :end_dt)
            '''
        query_params = {'client_id': client_id, 'promocode_id': promocode_id, 'begin_dt': begin_dt, 'end_dt': end_dt}
        db.balance().execute(query, query_params)

    @staticmethod
    def delete_reservation(promocode_id):
        query = '''
            DELETE FROM t_promo_code_reservation WHERE promocode_id = :promocode_id
            '''
        query_params = {'promocode_id': promocode_id}
        db.balance().execute(query, query_params)
        reporter.attach('All reservations for the promocode {0} have been deleted'.format(promocode_id))

    @staticmethod
    def delete_reservation_for_client(client_id):
        query = '''
            DELETE FROM t_promo_code_reservation WHERE client_id = :client_id
            '''
        query_params = {'client_id': client_id}
        db.balance().execute(query, query_params)
        reporter.attach('All reservations for the client {0} have been deleted'.format(client_id))

    @staticmethod
    def delete_promocode_from_invoices(promocode_id):
        query = '''UPDATE (SELECT * FROM t_invoice WHERE promo_code_id = :promocode_id) SET promo_code_id = NULL'''
        query_params = {'promocode_id': promocode_id}
        db.balance().execute(query, query_params)
        reporter.attach('The promocode {0} has been deleted from all invoices'.format(promocode_id))

    @staticmethod
    def clean_up(promocode_id):
        PromocodeSteps.delete_reservation(promocode_id)
        PromocodeSteps.delete_promocode_from_invoices(promocode_id)
        reporter.attach('Promocode {0} is cleaned up'.format(promocode_id))

    @staticmethod
    def set_services(promocode_id, service_list):
        for service in service_list:
            CommonSteps.set_extprops('PromoCode', promocode_id, 'service_ids', params={'value_num': service},
                                     insert_force=True)
        reporter.attach('Promocode {0} is available for services {1}'.format(promocode_id, service_list))

    @staticmethod
    def set_services_new(promocode_id, service_list):
        group_id = db.get_promocode2_by_id(promocode_id)[0]['group_id']
        service_list = json.dumps(service_list) if service_list is not None else service_list
        db.balance().execute('''UPDATE T_PROMO_CODE_GROUP SET SERVICE_IDS = :service_ids WHERE id = :group_id''',
                             {'group_id': group_id, 'service_ids': service_list})
        reporter.attach('Promocode {0} is available for services {1}'.format(promocode_id, service_list))

    @staticmethod
    def delete_all_services(promocode_id):
        db.balance().execute(
            '''DELETE FROM t_extprops WHERE classname = 'PromoCode' AND attrname = 'service_ids' AND object_id = :promocode_id''',
            {'promocode_id': promocode_id})

    @staticmethod
    def make_reservation(client_id, promocode_id, begin_dt, end_dt=None):
        if not end_dt:
            end_dt = begin_dt + datetime.timedelta(days=2)
        if PromocodeSteps.is_linked(promocode_id, client_id, begin_dt):
            # primary key in t_promo_Code_Reservation is (promocode_id, client_id, begin_dt)
            reporter.attach(
                'Promocode {0} has already been reserved by client {1} at {2}'.format(promocode_id, client_id,
                                                                                      begin_dt))
            return
        else:
            PromocodeSteps.delete_reservation(promocode_id)
            PromocodeSteps.insert_reservation(client_id, promocode_id, begin_dt, end_dt)
            reporter.attach(
                'Promocode {0} is reserved by client {1} at {2} till {3}'.format(promocode_id, client_id, begin_dt,
                                                                                 end_dt))

    @staticmethod
    def reserve(client_id, promocode_id):
        promocode = db.get_promocode_by_id(promocode_id)[0]
        start_dt = promocode['start_dt']
        end_dt = promocode['end_dt']
        reservation_days = promocode['reservation_days']
        start_dt_nullable_time = utils.Date.nullify_time_of_date(start_dt)
        if reservation_days is not None:
            end_dt_depend_on_reservation_days = start_dt_nullable_time + datetime.timedelta(days=reservation_days)
            if end_dt < end_dt_depend_on_reservation_days:
                reservation_end_dt = end_dt
            else:
                reservation_end_dt = end_dt_depend_on_reservation_days
        else:
            reservation_end_dt = end_dt
        PromocodeSteps.make_reservation(client_id, promocode_id, start_dt_nullable_time, reservation_end_dt)

    @staticmethod
    def reserve_new(client_id, promocode_id):
        group_id = db.get_promocode_by_id(promocode_id)[0]['group_id']
        promocode_settings = db.get_promocode_group_by_promocode_id(group_id)[0]
        start_dt = promocode_settings['start_dt']
        end_dt = promocode_settings['end_dt']
        reservation_days = promocode_settings['reservation_days']
        start_dt_nullable_time = utils.Date.nullify_time_of_date(start_dt)
        if reservation_days is not None:
            end_dt_depend_on_reservation_days = start_dt_nullable_time + datetime.timedelta(days=reservation_days)
            if end_dt < end_dt_depend_on_reservation_days:
                reservation_end_dt = end_dt
            else:
                reservation_end_dt = end_dt_depend_on_reservation_days
        else:
            reservation_end_dt = end_dt
        PromocodeSteps.make_reservation(client_id, promocode_id, start_dt_nullable_time, reservation_end_dt)

    @staticmethod
    def set_dates(promocode_id, start_dt, middle_dt=None, end_dt=None):
        if not end_dt:
            end_dt = start_dt + datetime.timedelta(days=2)
        if not middle_dt:
            middle_dt = start_dt + datetime.timedelta(seconds=1)

        pc_query = """
        select distinct gr.id, gr.calc_class_name, gr.calc_params 
        from t_promo_code_group gr 
        join bo.t_promo_code pc on pc.group_id=gr.id
        where pc.id=:id
        """
        group_params = db.balance().execute(pc_query, {'id': promocode_id})[0]
        calc_params = json.loads(group_params.get('calc_params', '{}'))
        if calc_params and 'middle_dt' in calc_params:
            calc_params['middle_dt'] = middle_dt.isoformat(' ')

        query = '''
        UPDATE t_promo_code_group SET start_dt = :start_dt, end_dt = :end_dt, calc_params =:calc_params  WHERE id = :id
        '''
        query_params = {'start_dt': start_dt, 'end_dt': end_dt, 'id': group_params['id'], 'calc_params': json.dumps(calc_params)}
        db.balance().execute(query, query_params)
        reporter.attach('Promocode {0} is valid from {1} to {2}'.format(promocode_id, start_dt, end_dt))

    @staticmethod
    def calculate_static_discount(qty, bonus, nds=None):
        if nds:
            qty_sum = qty / Decimal(nds)
        else:
            qty_sum = qty
        qty_sum_bonus_included = Decimal(qty_sum) + Decimal(bonus)
        discount_pct = Decimal('100') * (Decimal('1') - qty_sum / qty_sum_bonus_included)
        rounded_discount = Decimal(discount_pct).quantize(Decimal('.01'), rounding=ROUND_UP)
        return rounded_discount

    @staticmethod
    def define_bonus_on_dt(promocode_id, dt):
        promocode = db.get_promocode_by_id(promocode_id)[0]
        start_dt = promocode['start_dt']
        end_dt = promocode['end_dt']
        middle_dt = promocode['middle_dt']
        bonus1 = promocode['bonus1']
        bonus2 = promocode['bonus2']
        if start_dt <= dt < middle_dt:
            return bonus1
        elif middle_dt <= dt <= end_dt:
            return bonus2
        else:
            return None

    @staticmethod
    def calculate_qty_with_static_discount(qty, discount, precision):
        return Decimal((qty / (100 - discount)) * 100).quantize(Decimal(precision))

    @staticmethod
    def check_invoice_is_with_discount(invoice_id, bonus, is_with_discount, qty, precision='0.000001', nds=None,
                                       qty_before=None, invoice_discount=None):
        # todo-blubimov лучше обернуть в step
        consumes = db.get_consumes_by_invoice(invoice_id)
        discount_list = [consume['discount_pct'] for consume in consumes]
        expected_discount = PromocodeSteps.calculate_static_discount(qty, bonus, nds) if is_with_discount else Decimal(
            '0')
        if invoice_discount:
            common_invoice_discount = PromocodeSteps.multiply_discount(invoice_discount, expected_discount)
        else:
            common_invoice_discount = expected_discount
        for discount in discount_list:
            utils.check_that(Decimal(discount), equal_to(common_invoice_discount))
        for consume in consumes:
            if qty_before is None:
                current_sum = Decimal(consume['current_sum'])
                price = Decimal(consume['price'])
                qty_before = current_sum / price
            expected_qty = Decimal(
                PromocodeSteps.calculate_qty_with_static_discount(qty_before, expected_discount, Decimal(precision)))
            utils.check_that(Decimal(consume['current_qty']), equal_to(expected_qty))

    @staticmethod
    def check_invoice_is_with_fixed_discount(invoice_id, discount_from_promo):
        consumes = db.get_consumes_by_invoice(invoice_id)
        consume_discount = set([consume['discount_pct'] for consume in consumes])
        utils.check_that(consume_discount, equal_to({discount_from_promo}))

    @staticmethod
    def check_invoice_discount(promocode_type, invoice_id, bonus=None, is_with_discount=None, qty=None,
                               precision='0.000001', nds=None,
                               qty_before=None, invoice_discount=None, discount=None):
        if promocode_type == PromocodeClass.LEGACY_PROMO:
            return PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus, is_with_discount, qty,
                                                                 precision='0.000001', nds=None,
                                                                 qty_before=None, invoice_discount=None)
        if promocode_type == PromocodeClass.FIXED_DISCOUNT:
            return PromocodeSteps.check_invoice_is_with_fixed_discount(invoice_id=invoice_id,
                                                                       discount_from_promo=discount)

    @staticmethod
    def multiply_discount(invoice_discount, expected_discount):
        discount = Decimal('100') - (Decimal('100') - invoice_discount) * (
            Decimal('100') - expected_discount) / Decimal('100')
        return Decimal(discount).quantize(Decimal('.01'), rounding=ROUND_HALF_UP)

    @staticmethod
    def set_multicurrency_bonuses(promocode_id, list_of_bonuses):
        result_dict = collections.defaultdict(dict)
        for bonus in list_of_bonuses:
            currency_dict = dict(bonus1=bonus['bonus1'], bonus2=bonus['bonus2'], minimal_qty=bonus['minimal_qty'])
            result_dict[bonus['currency']] = currency_dict
        result = json.dumps(result_dict)
        query = '''
         UPDATE T_PROMO_CODE SET multicurrency_bonuses = :multicurrency_bonuses WHERE id = :id
         '''
        query_params = {'multicurrency_bonuses': result, 'id': promocode_id}
        db.balance().execute(query, query_params)
        reporter.attach('Multicurrency bonus {0} has been set on promocode {1}'.format(result, promocode_id))

    @staticmethod
    def tear_promocode_off(invoice_id):
        user = Users.YB_ADM
        session = passport_steps.auth_session(user=user)
        tear_promocode_off_url = '{base_url}/ajax-helper.xml'.format(base_url=env.balance_env().balance_ai)
        gen_sk = utils.get_secret_key(passport_id=user.uid)
        params = dict(invoice_id=invoice_id, method='tear_promo_code_off', sk=gen_sk)
        headers = {'X-Requested-With': 'XMLHttpRequest'}
        utils.call_http(session, tear_promocode_off_url, params, headers)
        return invoice_id

    @staticmethod
    def tear_off_promocode(invoice_id=None, invoice_external_id=None, client_id=None, service_id=None,
                           service_order_id=None, promocode_id=None, promocode_code=None):
        user = Users.YB_ADM
        params = {}
        if invoice_id:
            params['InvoiceID'] = invoice_id
        elif invoice_external_id:
            params['InvoiceEID'] = invoice_external_id
        elif client_id:
            params['ClientID'] = client_id
        elif service_id and service_order_id:
            params['ServiceID'] = service_id
            params['ServiceOrderID'] = service_order_id
        if promocode_id:
            params['PromocodeID'] = promocode_id
        if promocode_code:
            params['Promocode'] = promocode_code
        with reporter.step(u'Отрываем промокод'):
            api.medium().TearOffPromocode(user.uid, params)
