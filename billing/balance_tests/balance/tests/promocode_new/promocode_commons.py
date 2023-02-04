# -*- coding: utf-8 -*-
import datetime
from decimal import Decimal, ROUND_UP, ROUND_HALF_UP
from hamcrest import has_item, has_entries, equal_to
from balance import balance_db as db
from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.constants import Products, Firms, Paysyses, PersonTypes, Currencies, Regions, Services, PromocodeClass, \
    Nds
from temp.igogor.balance_objects import Contexts
from btestlib.matchers import contains_dicts_with_entries

NOW = datetime.datetime.now()
DT_1_DAY_BEFORE = NOW - datetime.timedelta(days=1)
DT_1_DAY_AFTER = NOW + datetime.timedelta(days=1)

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, precision=Decimal('0.000001'),
                                                               nds=Nds.YANDEX_RESIDENT, nds_included=True)

DIRECT_YANDEX_FIRM_FISH_NON_RES = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, person_type=PersonTypes.YT,
                                                                       paysys=Paysyses.BANK_YT_RUB,
                                                                       precision=Decimal('0.000001'),
                                                                       nds=Nds.NOT_RESIDENT, nds_included=True)

VZGLYAD_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.VZGLYAD_BUCKS,
                                                               region=Regions.RU, currency=Currencies.RUB,
                                                               service=Services.VZGLYAD, precision=Decimal('0.000001'),
                                                               nds=Nds.YANDEX_RESIDENT, nds_included=True)

DIRECT_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                           firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                           paysys=Paysyses.BANK_KZ_UR_TG,
                                                           precision=Decimal('0.000001'),
                                                           nds=Nds.KAZAKHSTAN, nds_included=True)

DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                              product=Products.DIRECT_RUB,
                                                              precision=Decimal('0.0001'),
                                                              region=Regions.RU, currency=Currencies.RUB,
                                                              nds=Nds.YANDEX_RESIDENT, nds_included=True)

DIRECT_YANDEX_FIRM_USD = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4, person_type=PersonTypes.USU,
                                                              product=Products.DIRECT_USD,
                                                              paysys=Paysyses.BANK_US_UR_USD,
                                                              precision=Decimal('0.000001'),
                                                              region=Regions.US, currency=Currencies.USD,
                                                              nds=Nds.NOT_RESIDENT, nds_included=True)

DIRECT_YANDEX_FIRM_USD_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT,
                                                                   product=Products.DIRECT_FISH,
                                                                   firm=Firms.YANDEX_INC_4, person_type=PersonTypes.USU,
                                                                   paysys=Paysyses.BANK_US_UR_USD,
                                                                   precision=Decimal('0.000001'),
                                                                   nds=Nds.NOT_RESIDENT, nds_included=True)

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                          precision=Decimal('0.0001'),
                                                          currency=Currencies.KZT,
                                                          nds=Nds.KAZAKHSTAN, nds_included=True)

DIRECT_KZ_FIRM_KZU_QUASI = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT,
                                                                product=Products.DIRECT_KZT_QUASI,
                                                                firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                                paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                                precision=Decimal('0.000001'),
                                                                currency=Currencies.KZT, nds=Nds.KAZAKHSTAN,
                                                                nds_included=True)

DIRECT_BEL_FIRM_BYN_QUASI = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                                 firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                                 paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                                 currency=Currencies.BYN, precision=Decimal('0.000001'),
                                                                 nds=Nds.BELARUS, nds_included=True)

DIRECT_UZB_FIRM_AG = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_USD,
                                                          firm=Firms.YANDEX_1, person_type=PersonTypes.YT,
                                                          paysys=Paysyses.BANK_YT_RUB, region=Regions.UZB,
                                                          currency=Currencies.USD, precision=Decimal('0.000001'),
                                                          nds=0, nds_included=True)

DIRECT_UZB_FIRM_AG44 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_USD,
                                                            firm=Firms.YANDEX_1, person_type=PersonTypes.BY_YTPH,
                                                            paysys=Paysyses.CC_YT_UZS, region=Regions.UZB,
                                                            currency=Currencies.USD, precision=Decimal('0.000001'),
                                                            nds=0, nds_included=True)

DIRECT_TR_FIRM_TRY = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_TRY,
                                                          firm=Firms.YANDEX_TURKEY_8, person_type=PersonTypes.TRU,
                                                          paysys=Paysyses.BANK_TR_UR_TRY, region=Regions.TR,
                                                          precision=Decimal('0.000001'),
                                                          currency=Currencies.TRY,
                                                          nds=Nds.DEFAULT, nds_included=True)

DIRECT_SW_FIRM_CHF = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_CHF,
                                                          firm=Firms.EUROPE_AG_7, person_type=PersonTypes.SW_UR,
                                                          paysys=Paysyses.BANK_SW_UR_CHF, region=Regions.SW,
                                                          precision=Decimal('0.000001'),
                                                          currency=Currencies.CHF,
                                                          nds=Nds.EUROPE_AG_RESIDENT, nds_included=True)


def is_request_with_promocode(promocode_id, request_id):
    code_in_request = db.get_request_by_id(request_id)[0]['promo_code_id']
    return promocode_id == code_in_request


def delete_promocode(promocode_id):
    db.balance().execute('DELETE FROM t_promo_code WHERE id = :id', {'id': promocode_id})


def is_invoice_with_promocode(promocode_id, invoice_id):
    code_in_invoice = db.get_invoice_by_id(invoice_id)[0]['promo_code_id']
    return promocode_id == code_in_invoice


def import_promocode(calc_class_name=None, end_dt=None, promocodes=None, start_dt=None, calc_params=None,
                     minimal_amounts=None, firm_id=None, is_global_unique=None, valid_until_paid=None,
                     service_ids=None, new_clients_only=None, need_unique_urls=None, reservation_days=None,
                     binded_client=None):
    params = {'CalcClassName': calc_class_name,
              'EndDt': end_dt,
              'Promocodes': [{'code': promocode, 'client_id': binded_client} for promocode in promocodes],
              'StartDt': start_dt,
              'CalcParams': calc_params,
              'MinimalAmounts': minimal_amounts,
              'FirmId': firm_id,
              'IsGlobalUnique': is_global_unique,
              'ValidUntilPaid': valid_until_paid,
              'NewClientsOnly': new_clients_only,
              'NeedUniqueUrls': need_unique_urls,
              'ReservationDays': reservation_days
              }
    if service_ids is not None:
        params.update({'ServiceIds': service_ids})
    api.medium().ImportPromoCodes([params])
    response = []
    for promocode in promocodes:
        promocode_id = db.get_promocode_by_code(promocode)[0]['id']
        response.append((promocode_id, promocode))
    return response


def generate_code():
    return utils.generate_alfanumeric_string(16).upper()


def fill_calc_params(calc_class_name, discount_pct=None, apply_on_create=None, adjust_quantity=None,
                     product_bonuses=None, currency_bonuses=None, reference_currency=None,
                     currency=None, scale_points=None):
    if calc_class_name == PromocodeClass.FIXED_DISCOUNT:
        return fill_calc_params_fixed_discount(discount_pct, apply_on_create=apply_on_create,
                                               adjust_quantity=adjust_quantity)
    if calc_class_name == PromocodeClass.FIXED_QTY:
        return fill_calc_params_fixed_qty(product_bonuses=product_bonuses, apply_on_create=None, adjust_quantity=None)
    if calc_class_name == PromocodeClass.FIXED_SUM:
        return fill_calc_params_fixed_sum(currency_bonuses=currency_bonuses, reference_currency=reference_currency)
    if calc_class_name == PromocodeClass.SCALE:
        return fill_calc_params_scale(currency=currency, scale_points=scale_points)


def fill_calc_params_fixed_discount(discount_pct, apply_on_create=None, adjust_quantity=None):
    cals_params = {'discount_pct': discount_pct}
    if apply_on_create:
        cals_params['apply_on_create'] = apply_on_create
    if adjust_quantity:
        cals_params['adjust_quantity'] = adjust_quantity
    return cals_params


def fill_calc_params_act_bonus(apply_on_create=None, adjust_quantity=None, act_bonus_pct=None,
                               min_act_amount=None, max_bonus_amount=None, max_discount_pct=None,
                               currency=None, act_period_days=None):
    cals_params = {'act_bonus_pct': act_bonus_pct,
                   'min_act_amount': min_act_amount,
                   'max_bonus_amount': max_bonus_amount,
                   'max_discount_pct': max_discount_pct,
                   'currency': currency,
                   'act_period_days': act_period_days}
    if apply_on_create:
        cals_params['apply_on_create'] = apply_on_create
    if adjust_quantity:
        cals_params['adjust_quantity'] = adjust_quantity

    return cals_params


def fill_calc_params_fixed_sum(currency_bonuses, reference_currency, apply_on_create=None, adjust_quantity=None):
    cals_params = {'currency_bonuses': currency_bonuses,
                   'reference_currency': reference_currency}
    if apply_on_create:
        cals_params['apply_on_create'] = apply_on_create
    if adjust_quantity:
        cals_params['adjust_quantity'] = adjust_quantity
    return cals_params


def fill_calc_params_fixed_qty(product_bonuses, apply_on_create=None, adjust_quantity=None):
    cals_params = {'product_bonuses': product_bonuses}
    if apply_on_create:
        cals_params['apply_on_create'] = apply_on_create
    if adjust_quantity:
        cals_params['adjust_quantity'] = adjust_quantity
    return cals_params


def fill_calc_params_scale(currency, scale_points, apply_on_create=None, adjust_quantity=None):
    cals_params = {'currency': currency,
                   'scale_points': scale_points}
    if apply_on_create:
        cals_params['apply_on_create'] = apply_on_create
    if adjust_quantity:
        cals_params['adjust_quantity'] = adjust_quantity
    return cals_params


def create_and_reserve_promocode(client_id=None, start_dt=None, end_dt=None, firm_id=None, service_ids=None,
                                 is_global_unique=None, reservation_days=None, minimal_amounts={},
                                 new_clients_only=None, valid_until_paid=None, calc_params=None, calc_class_name=None,
                                 need_unique_urls=None, binded_client=None, reservation_dt=None):

    code = generate_code()
    promocode_id, promocode_code = import_promocode(calc_class_name=calc_class_name, start_dt=start_dt, end_dt=end_dt,
                                                        calc_params=calc_params,
                                                        firm_id=firm_id, is_global_unique=is_global_unique,
                                                        new_clients_only=new_clients_only,
                                                        valid_until_paid=valid_until_paid, promocodes=[code],
                                                        reservation_days=reservation_days, service_ids=service_ids,
                                                        minimal_amounts=minimal_amounts, need_unique_urls=need_unique_urls,
                                                        binded_client=binded_client)[0]
    if client_id:
        reserve(client_id, promocode_id, start_dt=reservation_dt)
    return promocode_id, promocode_code


def create_order(context, client_id, agency_id):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=context.product.id,
                                       service_id=context.service.id,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    return service_order_id, order_id


def create_request(context, client_id, qty=None, promocode_code=None, invoice_dt=None, agency_id=None,
                   orders_list=None, deny_promocode=None):
    if not orders_list:
        service_order_id, _ = create_order(context, client_id, agency_id)
        orders_list = [
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': NOW}]

    request_id = steps.RequestSteps.create(client_id=agency_id if agency_id else client_id, orders_list=orders_list,
                                           additional_params={'PromoCode': promocode_code,
                                                              'InvoiceDesireDT': invoice_dt,
                                                              'DenyPromocode': deny_promocode})
    return request_id, orders_list


def create_payed_invoice(request_id, person_id, paysys_id):
    invoice_id = create_invoice(request_id, person_id, paysys_id)

    steps.InvoiceSteps.pay(invoice_id)
    return invoice_id


def create_invoice(request_id, person_id, paysys_id):
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    return invoice_id


def create_act(client_id, person_id, context, dt, promocode_code=None, qty=None):
    request_id, orders_list = create_request(context, client_id, promocode_code=promocode_code,
                                             invoice_dt=dt, qty=qty)
    create_payed_invoice(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id)
    steps.CampaignsSteps.do_campaigns(context.service.id, orders_list[0]['ServiceOrderID'],
                                      {context.product.type.code: qty}, 0, dt)
    return steps.ActsSteps.generate(client_id, force=1, date=dt)[0], request_id


def reserve(client_id, promocode_id, start_dt=None):
    group_id = db.get_promocode_by_id(promocode_id)[0]['group_id']
    promocode_group = db.get_promocode_group_by_group_id(group_id)[0]
    start_dt = start_dt or promocode_group['start_dt']
    end_dt = promocode_group['end_dt']
    reservation_days = promocode_group['reservation_days']
    start_dt_nullable_time = utils.Date.nullify_time_of_date(start_dt)
    if reservation_days is not None:
        end_dt_depend_on_reservation_days = start_dt_nullable_time + datetime.timedelta(days=reservation_days)
        if end_dt < end_dt_depend_on_reservation_days:
            reservation_end_dt = end_dt
        else:
            reservation_end_dt = end_dt_depend_on_reservation_days
    else:
        reservation_end_dt = end_dt
    make_reservation(client_id, promocode_id, start_dt_nullable_time, reservation_end_dt)


def make_reservation(client_id, promocode_id, begin_dt, end_dt=None):
    if not end_dt:
        end_dt = begin_dt + datetime.timedelta(days=2)
    if is_promo_already_linked(promocode_id, client_id, begin_dt):
        reporter.attach(
            'Promocode {0} has already been reserved by client {1} at {2}'.format(promocode_id, client_id,
                                                                                  begin_dt))
        return
    else:
        delete_reservation(promocode_id)
        insert_reservation(client_id, promocode_id, begin_dt, end_dt)
        reporter.attach(
            'Promocode {0} is reserved by client {1} at {2} till {3}'.format(promocode_id, client_id, begin_dt,
                                                                             end_dt))


def delete_reservation_for_client(client_id):
    query = '''
            DELETE FROM t_promo_code_reservation WHERE client_id = :client_id
            '''
    query_params = {'client_id': client_id}
    db.balance().execute(query, query_params)
    reporter.attach('All reservations for the client {0} have been deleted'.format(client_id))


def is_promo_already_linked(promocode_id, client_id, begin_dt):
    reservation_list = db.get_promocodes_reservation_by_promocode_id(promocode_id)
    return has_item(has_entries({
        'begin_dt': begin_dt,
        'client_id': client_id,
        'promocode_id': promocode_id
    })).matches(reservation_list)


def delete_reservation(promocode_id):
    query = '''
            DELETE FROM t_promo_code_reservation WHERE promocode_id = :promocode_id
            '''
    query_params = {'promocode_id': promocode_id}
    db.balance().execute(query, query_params)
    reporter.attach('All reservations for the promocode {0} have been deleted'.format(promocode_id))


def insert_reservation(client_id, promocode_id, begin_dt, end_dt=None):
    query = '''
            INSERT INTO t_promo_code_reservation(client_id, promocode_id, begin_dt, end_dt) VALUES (:client_id, :promocode_id, :begin_dt, :end_dt)
            '''
    query_params = {'client_id': client_id, 'promocode_id': promocode_id, 'begin_dt': begin_dt, 'end_dt': end_dt}
    db.balance().execute(query, query_params)


def check_invoice_consumes_discount(invoice_id, classname, bonus=None, is_with_discount=None, qty=None,
                                    precision=None, nds=None, qty_before=None, invoice_discount=None, discount_pct=None,
                                    adjust_quantity=None, apply_on_create=None, nds_included=None, fixed_sum=None,
                                    fixed_qty=None, sum_before=None):
    if not adjust_quantity and apply_on_create:
        return check_invoice_consumes_is_with_discount_on_sum(invoice_id=invoice_id, qty_before=qty_before,
                                                              precision=precision, fixed_qty=fixed_qty,
                                                              discount_pct=discount_pct, fixed_sum=fixed_sum, nds=nds,
                                                              nds_included=nds_included, sum_before=sum_before,
                                                              adjust_quantity=adjust_quantity)
    else:
        return check_invoice_consumes_is_with_discount_on_qty(invoice_id=invoice_id, qty_before=qty_before,
                                                              precision=precision, discount_pct=discount_pct,
                                                              fixed_sum=fixed_sum, nds=nds, fixed_qty=fixed_qty,
                                                              adjust_quantity=adjust_quantity)


def check_invoice_rows_discount(invoice_id, classname, bonus=None, is_with_discount=None, qty=None,
                                precision=None, nds=None, fixed_sum=None, fixed_qty=None,
                                qty_before=None, invoice_discount=None, discount_pct=None, adjust_quantity=None,
                                apply_on_create=None, nds_included=None, sum_before=None):
    if not adjust_quantity and apply_on_create:
        check_invoice_rows_is_with_discount_on_sum(classname=classname, invoice_id=invoice_id, qty_before=qty_before,
                                                   fixed_sum=fixed_sum, nds=nds, nds_included=nds_included,
                                                   discount_pct=discount_pct, fixed_qty=fixed_qty,
                                                   sum_before=sum_before, adjust_quantity=adjust_quantity)
    else:
        check_invoice_rows_is_with_discount_on_qty(classname=classname, invoice_id=invoice_id, fixed_sum=fixed_sum,
                                                   qty_before=qty_before,
                                                   precision=precision, nds=nds, nds_included=nds_included,
                                                   discount_pct=discount_pct, fixed_qty=fixed_qty,
                                                   adjust_quantity=adjust_quantity)


def check_invoice_consumes_is_with_discount_on_qty(invoice_id, qty_before, precision, nds, discount_pct=None,
                                                   fixed_sum=None, fixed_qty=None, adjust_quantity=None):
    if discount_pct is None:
        if fixed_sum is not None:
            discount_pct = calculate_discount_pct_from_fixed_sum(invoice_id, fixed_sum, nds, qty=sum(qty_before),
                                                                 adjust_quantity=adjust_quantity)
        if fixed_qty is not None:
            discount_pct = calculate_discount_pct_from_fixed_qty(sum(qty_before), fixed_qty)
    consumes = db.get_consumes_by_invoice(invoice_id)
    expected_consumes = []
    for index, consume in enumerate(consumes):
        expected_qty = calculate_qty_with_static_discount(qty_before[index], discount_pct,
                                                          Decimal(precision))
        expected_consumes.append({'current_qty': expected_qty,
                                  'discount_pct': discount_pct,
                                  'static_discount_pct': discount_pct,
                                  'consume_qty': expected_qty})
    utils.check_that(consumes, contains_dicts_with_entries(expected_consumes))


def check_invoice_consumes_is_with_discount_on_sum(invoice_id, qty_before, nds, nds_included, precision, fixed_sum=None,
                                                   discount_pct=None, fixed_qty=None, sum_before=None,
                                                   adjust_quantity=None):
    if discount_pct is None:
        if fixed_sum is not None:
            discount_pct = calculate_discount_pct_from_fixed_sum(invoice_id, fixed_sum, nds, qty=sum(qty_before),
                                                                 sum_before=sum_before)
        if fixed_qty is not None:
            if fixed_qty == 0:
                discount_pct = Decimal('0')
            else:
                discount_pct = calculate_discount_pct_to_sum_from_fixed_qty(invoice_id=invoice_id, fixed_qty=fixed_qty)
    consumes = db.get_consumes_by_invoice(invoice_id)
    expected_consumes = []
    for index, consume in enumerate(consumes):
        internal_price = consume['price']
        sum_ = get_amount(qty_before[index], nds, internal_price, nds_included)
        expected_sum = utils.dround2(calculate_sum_with_static_discount(sum_, discount_pct))
        expected_consumes.append({'consume_sum': expected_sum,
                                  'discount_pct': discount_pct,
                                  'static_discount_pct': discount_pct,
                                  'current_sum': expected_sum})
    utils.check_that(consumes, contains_dicts_with_entries(expected_consumes))


def check_invoice_rows_is_with_discount_on_sum(invoice_id, classname, qty_before, nds, nds_included, fixed_sum=None,
                                               discount_pct=None, fixed_qty=None, sum_before=None,
                                               adjust_quantity=None):
    if discount_pct is None:
        if fixed_sum is not None:
            if fixed_sum != 0:
                discount_pct = calculate_discount_pct_from_fixed_sum(invoice_id, fixed_sum, nds, qty=sum(qty_before),
                                                                     sum_before=sum_before)
            else:
                discount_pct = Decimal('0')
        if fixed_qty is not None:
            if fixed_qty != 0:
                discount_pct = calculate_discount_pct_to_sum_from_fixed_qty(invoice_id=invoice_id, fixed_qty=fixed_qty)
            else:
                discount_pct = Decimal('0')
    invoice_rows = db.get_invoice_orders_by_invoice_id(invoice_id)
    for index, row in enumerate(invoice_rows):
        internal_price = row['internal_price']
        if sum_before:
            sum_ = sum_before[index]
        else:
            sum_ = get_amount(qty_before[index], nds, internal_price, nds_included)
        expected_sum = utils.dround2(calculate_sum_with_static_discount(sum_, discount_pct))
        utils.check_that([row], contains_dicts_with_entries([{'quantity': qty_before[index],
                                                              'discount_pct': discount_pct,
                                                              'amount_no_discount': sum_,
                                                              'amount': expected_sum,
                                                              'effective_sum': expected_sum}]))


def check_invoice_rows_is_with_discount_on_qty(invoice_id, classname, qty_before, precision, nds, nds_included,
                                               fixed_sum=None, discount_pct=None, fixed_qty=None, adjust_quantity=None):
    if discount_pct is None:
        if fixed_sum is not None or fixed_sum == 0:
            if fixed_sum != 0:
                discount_pct = calculate_discount_pct_from_fixed_sum(invoice_id, fixed_sum, nds, qty=sum(qty_before),
                                                                     adjust_quantity=adjust_quantity)
            else:
                discount_pct = Decimal('0')
        if fixed_qty is not None or fixed_qty == 0:
            if fixed_qty != 0:
                discount_pct = calculate_discount_pct_from_fixed_qty(qty=sum(qty_before), fixed_qty=fixed_qty)
            else:
                discount_pct = Decimal('0')
    invoice_rows = db.get_invoice_orders_by_invoice_id(invoice_id)
    for index, row in enumerate(invoice_rows):
        expected_qty = Decimal(calculate_qty_with_static_discount(qty_before[index], discount_pct, Decimal(precision)))
        expected_sum = utils.dround2(expected_qty * Decimal(row['internal_price']))
        utils.check_that([row], contains_dicts_with_entries([{'quantity': expected_qty,
                                                              'discount_pct': discount_pct,
                                                              'amount_no_discount': expected_sum}]))


def calculate_qty_with_static_discount(qty, discount, precision):
    if discount != 100:
        # увеличивает количество на заданный процент и округляет до точности в заказе
        return Decimal((qty / (Decimal('100') - Decimal(discount))) * Decimal('100')).quantize(Decimal(precision))
    else:
        return qty


def calculate_sum_with_static_discount(sum, discount):
    return Decimal(sum) * (Decimal('100') - Decimal(discount)) / Decimal('100')


def calculate_discount_pct_from_fixed_sum(invoice_id, fixed_sum, nds, qty, sum_before=None, adjust_quantity=None):
    internal_price = db.get_invoice_orders_by_invoice_id(invoice_id)[0]['internal_price']
    if sum_before:
        total_sum = sum(sum_before)
    else:
        total_sum = qty * Decimal(internal_price).quantize(Decimal('0.01'))
    bonus_with_nds = add_nds_to_amount(fixed_sum, nds)
    return calculate_static_discount_sum(total_sum=total_sum, bonus_with_nds=bonus_with_nds,
                                         adjust_quantity=adjust_quantity)


def calculate_discount_pct_from_fixed_qty(qty, fixed_qty):
    qty_with_bonus = Decimal(qty) + Decimal(fixed_qty)
    return calculate_static_discount_qty(qty, qty_with_bonus)


def calculate_discount_pct_to_sum_from_fixed_qty(invoice_id, fixed_qty):
    invoice_rows = db.get_invoice_orders_by_invoice_id(invoice_id)
    internal_price = invoice_rows[0]['internal_price']
    total_sum = db.get_invoice_by_id(invoice_id)[0]['total_sum']
    sum_with_bonus = Decimal(total_sum) + Decimal(internal_price) * Decimal(fixed_qty)
    return utils.dround2(Decimal(100) - Decimal(total_sum) / Decimal(sum_with_bonus) * Decimal('100'))


def get_amount(qty, nds, internal_price, nds_included):
    if nds_included or nds == 0:
        return utils.dround2(qty * Decimal(internal_price))
    else:
        amount = Decimal(qty) * Decimal(internal_price)
        return utils.dround2(add_nds_to_amount(amount, nds))


def add_nds_to_amount(amount, nds):
    return Decimal(amount) * (1 + Decimal(nds) / Decimal('100'))


def get_bonus_from_act_amount(amount, pct):
    return utils.dround2(Decimal(amount) * Decimal(pct) / Decimal('100'))


def calculate_static_discount_qty(qty, qty_with_bonus):
    discount_pct = Decimal(100) * (Decimal(1) - qty / Decimal(qty_with_bonus))
    rounded_discount = Decimal(discount_pct).quantize(Decimal('.01'))
    return rounded_discount


def calculate_static_discount_sum(total_sum, bonus_with_nds, adjust_quantity):
    if adjust_quantity:
        discount_pct = Decimal(100) / (Decimal(1) + total_sum / bonus_with_nds)
    else:
        discount_pct = Decimal(100) - Decimal(100) * (Decimal(1) - bonus_with_nds / Decimal(total_sum))

    rounded_discount = Decimal(discount_pct).quantize(Decimal('.0001'))
    return rounded_discount


def delete_promocode_from_invoices(promocode_id):
    query = '''UPDATE (SELECT * FROM t_invoice WHERE promo_code_id = :promocode_id) SET promo_code_id = NULL'''
    query_params = {'promocode_id': promocode_id}
    db.balance().execute(query, query_params)
    reporter.attach('The promocode {0} has been deleted from all invoices'.format(promocode_id))


def multiply_discount(invoice_discount, expected_discount):
    discount = Decimal('100') - (Decimal('100') - invoice_discount) * (
            Decimal('100') - expected_discount) / Decimal('100')
    return Decimal(discount).quantize(Decimal('.01'), rounding=ROUND_HALF_UP)
