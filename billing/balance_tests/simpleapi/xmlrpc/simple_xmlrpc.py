# coding: utf-8

import balance.balance_api as common_api
import btestlib.utils as utils
from btestlib import environments
from btestlib import utils as butils
from btestlib.secrets import get_secret, Certificates
from btestlib.utils import Date
from simpleapi.common.utils import remove_empty
from simpleapi.data import defaults


@utils.cached
def ssl_context():
    return common_api.ssl_context(certfile=get_secret(*Certificates.XMLRPC_CLIENT_CERT),
                                  keyfile=get_secret(*Certificates.XMLRPC_CLIENT_KEY),
                                  password=get_secret(*Certificates.XMLRPC_CLIENT_KEY_PWD))

server_ora = butils.XmlRpc.ReportingServerProxy(environments.simpleapi_env_ora().simple_url,
                                                namespace='BalanceSimple',
                                                context=ssl_context())
server_pg = butils.XmlRpc.ReportingServerProxy(environments.simpleapi_env_pg().simple_url,
                                               namespace='BalanceSimple',
                                               context=ssl_context())


def server():
    if environments.SimpleapiEnvironment.XMLRPC_URL == environments.TrustApiUrls.XMLRPC_PG:
        return server_pg
    elif environments.SimpleapiEnvironment.XMLRPC_URL == environments.TrustApiUrls.XMLRPC_ORA:
        return server_ora
    else:
        return butils.XmlRpc.ReportingServerProxy(environments.SimpleapiEnvironment.XMLRPC_URL,
                                                  namespace='BalanceSimple',
                                                  context=ssl_context())


def list_payment_methods(service, uid, phone=None, masterpass_fingerprint_seed=None,
                         region_id=None, uber_oauth_token=None, uber_uid=None):
    p = remove_empty({
        'uid': uid,
        'user_ip': '127.0.0.1',
        'phone': phone,
        'masterpass_fingerprint_seed': masterpass_fingerprint_seed,
        'region_id': region_id,
        'uber_oauth_token': uber_oauth_token,
        'uber_uid': uber_uid,
    })
    return server().ListPaymentMethods(service.token, p)


def set_card_label(service, uid, card_id, label, action):
    p = remove_empty({'uid': uid,
                      'card_id': card_id,
                      'label': label,
                      'action': action
                      })
    return server().SetCardLabel(service.token, p)


def create_service_product(service, service_product_id,
                           partner_id, shop_params, name,
                           prices, parent_service_product_id,
                           type_, subs_period, subs_trial_period,
                           active_until_dt, single_purchase, bonuses, service_fee,
                           subs_introductory_period, subs_introductory_period_prices,
                           fiscal_nds, fiscal_title, processing_cc, aggregated_charging):
    p = remove_empty({
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
    return server().CreateServiceProduct(service.token, p)


def create_partner(service,
                   name=defaults.partner_info['name'],
                   email=defaults.partner_info['email'],
                   operator_uid=defaults.partner_info['operator_uid']):
    p = remove_empty({
        'name': name,
        'email': email,
        'operator_uid': operator_uid,
        'phone': '+79214567323',
        'fax': '+79214567323',
        'url': 'test.url',
        'city': 'St.Petersburg'
    })
    response = server().CreatePartner(service.token, p)
    response.update({'info': p})
    return response


def create_order_or_subscription(service, uid, user_ip,
                                 service_product_id, region_id,
                                 purchase_token=None,
                                 service_order_id=None,
                                 commission_category=None,
                                 developer_payload=None,
                                 start_ts=None, subs_begin_ts=None,
                                 parent_service_order_id=None):
    p = remove_empty({
        'user_ip': user_ip,
        'service_product_id': service_product_id,
        'region_id': region_id,
        'uid': uid,
        'purchase_token': purchase_token,
        'service_order_id': service_order_id,
        'commission_category': commission_category,
        'developer_payload': developer_payload,
        'start_ts': start_ts.isoformat() if start_ts else None,  # a-vasin: datetime без tz вызовет падение ручки
        'subs_begin_ts': Date.date_to_iso_format(
            Date.set_timezone_of_date(subs_begin_ts, 'Europe/Moscow')) if subs_begin_ts else None,
        'parent_service_order_id': parent_service_order_id,
    })
    return server().CreateOrderOrSubscription(service.token, p)


def create_basket(service, orders, user_ip,
                  paymethod_id, ym_schema=None, wait_for_cvn=None,
                  uid=defaults.partner_info['operator_uid'],
                  back_url=None, return_path=None,
                  payment_timeout=None, accept_promo=None,
                  currency='RUB', pass_params=None,
                  discounts=None, apple_token=None,
                  payment_mode=None, fiscal_taxation_type=None,
                  fiscal_partner_inn=None, fiscal_partner_phone=None, user_email=defaults.email,
                  promocode_id=None, lang=None, user_phone=defaults.phone, verify_user_phone=None,
                  template_tag=None, uber_oauth_token=None, uber_uid=None, domain_sfx=None,
                  developer_payload=None, paymethod_markup=None, spasibo_order_map=None):
    p = remove_empty({
        'user_ip': user_ip,
        'orders': orders,
        'paymethod_markup': paymethod_markup,  # https://wiki.yandex-team.ru/TRUST/composite-payments/
        'spasibo_order_map': spasibo_order_map,
        'payment_timeout': int(payment_timeout or 0),
        'paymethod_id': paymethod_id,
        'uid': uid,
        'back_url': back_url,
        'return_path': return_path,
        'ym_schema': ym_schema,
        'wait_for_cvn': wait_for_cvn,
        'accept_promo': accept_promo,
        'currency': currency,
        'pass_params': pass_params,
        'discounts': discounts,
        'apple_token': apple_token,
        'payment_mode': payment_mode,
        'fiscal_taxation_type': fiscal_taxation_type,
        'fiscal_partner_inn': fiscal_partner_inn,
        'fiscal_partner_phone': fiscal_partner_phone,
        'user_email': user_email,
        'promocode_id': promocode_id,
        'user_phone': user_phone,
        'verify_user_phone': verify_user_phone,
        'uber_oauth_token': uber_oauth_token,
        'uber_uid': uber_uid,
        'template_tag': template_tag,   # 'mobile/form' 'desktop/form' 'smarttv/form'
        'lang': lang,  # 'ru' 'en' 'uk' 'tr'
        'domain_sfx': domain_sfx,  # 'ru' 'ua' 'com' 'com.tr' 'by'
        'developer_payload': developer_payload
    })
    return server().CreateBasket(service.token, p)


def pay_basket(service, user_ip,
               uid, token, auth,
               bypass_auth, trust_payment_id,
               purchase_token, ):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'purchase_token': purchase_token,
        'auth': auth,
        'bypass_auth': bypass_auth,
        'trust_payment_id': trust_payment_id,
    })
    return server().PayBasket(service.token, p)


def check_basket(service, user_ip,
                 user, token,
                 trust_payment_id,
                 purchase_token=None, with_promocodes=None):
    p = remove_empty({
        'uid': user,
        'token': token,
        'user_ip': user_ip,
        'purchase_token': purchase_token,
        'trust_payment_id': trust_payment_id,
        'with_promocodes': with_promocodes,
    })
    return server().CheckBasket(service.token, p)


def update_basket(service, orders,
                  trust_payment_id,
                  user_ip=None, reason_desc=None,
                  uid=None, token=None, paymethod_markup=None):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'reason_desc': reason_desc,
        'trust_payment_id': trust_payment_id,
        'orders': orders,
        'paymethod_markup': paymethod_markup,
    })
    return server().UpdateBasket(service.token, p)


def create_refund(service, user_ip,
                  reason_desc, orders,
                  trust_payment_id,
                  uid=None, token=None,
                  paymethod_markup=None, spasibo_order_map=None):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'reason_desc': reason_desc,
        'trust_payment_id': trust_payment_id,
        'orders': orders,
        'paymethod_markup': paymethod_markup,
        'spasibo_order_map': spasibo_order_map,
    })
    return server().CreateRefund(service.token, p)


def do_refund(service, user_ip,
              trust_refund_id,
              uid=None, token=None):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'trust_refund_id': trust_refund_id,
    })
    return server().DoRefund(service.token, p)


def load_partner(service, partner_id):
    p = {
        'partner_id': partner_id
    }
    return server().LoadPartner(service.token, p)


def create_binding(service, uid):
    p = {
        'uid': uid
    }
    return server().CreateBinding(service.token, p)


def do_binding(service, purchase_token):
    p = {
        'purchase_token': purchase_token
    }
    return server().DoBinding(service.token, p)


def check_binding(service, purchase_token):
    p = {
        'purchase_token': purchase_token
    }
    return server().CheckBinding(service.token, p)


def unbind_card(service, session_id, user_ip, card):
    p = {'session_id': session_id,
         'user_ip': user_ip,
         'card': card
         }
    return server().UnbindCard(service.token, p)


def create_order(service, service_order_id,
                 service_product_id, region_id, uid=None,
                 token=None, ym_schema=None):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'service_order_id': service_order_id,
        'service_product_id': service_product_id,
        'region_id': region_id,
        'ym_schema': ym_schema,
        'back_url': 'https://balance.greed-tm1f.yandex.ru/'
    })
    return server().CreateOrder(service.token, p)


def pay_order(service, user_ip, service_order_id,
              paymethod_id, uid=None, token=None,
              purchase_token=None, currency=None,
              back_url=None, return_path=None,
              ym_schema=None):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'purchase_token': purchase_token,
        'service_order_id': service_order_id,
        'paymethod_id': paymethod_id,
        'currency': currency,
        'back_url': back_url,
        'return_path': return_path,
        'ym_schema': ym_schema,
    })
    return server().PayOrder(service.token, p)


def check_order(service, user_ip, service_order_id,
                uid=None, token=None,
                purchase_token=None,
                trust_payment_id=None):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'purchase_token': purchase_token,
        'service_order_id': service_order_id,
        'trust_payment_id': trust_payment_id,
    })
    return server().CheckOrder(service.token, p)


def refund_order(service, user_ip, service_order_id,
                 uid=None, token=None,
                 reason_desc=None, force=None,
                 trust_payment_id=None):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'force': force,
        'reason_desc': reason_desc,
        'service_order_id': service_order_id,
        'trust_payment_id': trust_payment_id,
    })
    return server().RefundOrder(service.token, p)


def stop_subscription(service, user_ip, service_order_id,
                      uid=None, token=None,
                      stop_flag=1):
    p = remove_empty({
        'uid': uid,
        'token': token,
        'user_ip': user_ip,
        'service_order_id': service_order_id,
        'stop_flag': stop_flag,
    })
    return server().StopSubscription(service.token, p)


def check_card(service, uid, card_id, cvn=None, region_id=None, uber_uid=None, uber_oauth_token=None):
    p = remove_empty({
        'uid': uid,
        'card_id': card_id,
        'cvn': cvn,
        'region_id': region_id,
        'uber_uid': uber_uid,
        'uber_oauth_token': uber_oauth_token
    })
    return server().CheckCard(service.token, p)


def get_service_product_public_key(service, service_product_id):
    p = remove_empty({
        'service_product_id': service_product_id
    })
    return server().GetServiceProductPublicKey(service.token, p)


def create_promoseries(service, name, services, amount, begin_dt, limit=None, description=None,
                       end_dt=None, partial_only=None, full_payment_only=None, extra_pay=None, usage_limit=None):
    p = remove_empty({
        'name': name,
        'services': services,
        'limit': limit,
        'amount': amount,
        'description': description,
        'begin_dt': begin_dt,
        'end_dt': end_dt,
        'partial_only': partial_only,
        'full_payment_only': full_payment_only,
        'extra_pay': extra_pay,
        'usage_limit': usage_limit,
    })
    return server().CreatePromoseries(service.token, p)


def get_promoseries_status(service, series_id):
    p = remove_empty({
        'series_id': series_id,
    })
    return server().GetPromoseriesStatus(service.token, p)


def create_promocode(service, series_id, code=None, amount=None, begin_dt=None, end_dt=None,
                     quantity=None, code_length=None):
    p = remove_empty({
        'series_id': series_id,
        'code': code,
        'amount': amount,
        'begin_dt': begin_dt,
        'end_dt': end_dt,
        'quantity': quantity,
        'code_length': code_length,
    })
    return server().CreatePromocode(service.token, p)


def get_promocode_status(service, code=None, promocode_id=None, with_payments=None):
    p = remove_empty({
        'code': code,
        'promocode_id': promocode_id,
        'with_payments': with_payments
    })
    return server().GetPromocodeStatus(service.token, p)


def get_promocodes_in_series(service, series_id, page, page_size=None):
    p = remove_empty({
        'series_id': series_id,
        'page': page,
        'page_size': page_size,
    })
    return server().GetPromocodesInSeries(service.token, p)


def sign_service_product_message(service, service_product_id, message=None, binary_message=None):
    p = remove_empty({
        'service_product_id': service_product_id,
        'message': message,
        'binary_message': binary_message
    })
    return server().SignServiceProductMessage(service.token, p)


def get_payment_receipt(service, trust_payment_id=None, trust_refund_id=None):
    p = remove_empty({
        'trust_payment_id': trust_payment_id,
        'trust_refund_id': trust_refund_id,
    })
    return server().GetPaymentReceipt(service.token, p)


def create_payment_for_invoice(service, invoice_id, passport_id=None, user_ip=None,
                               back_url=None, developer_payload=None,
                               paymethod_id=None):
    p = remove_empty({
        'invoice_id': invoice_id,
        'passport_id': passport_id,
        'user_ip': user_ip,
        'back_url': back_url,
        'developer_payload': developer_payload,
        'paymethod_id': paymethod_id,
    })
    return server().CreatePaymentForInvoice(service.token, p)


def start_trust_api_payment(service, transaction_id):
    p = remove_empty({
        'transaction_id': transaction_id,
    })
    return server().StartTrustAPIPayment(service.token, p)


def check_trust_api_payment(service, transaction_id):
    p = remove_empty({
        'transaction_id': transaction_id,
    })
    return server().CheckTrustAPIPayment(service.token, p)
