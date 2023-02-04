# coding=utf-8

import urlparse

from lxml import etree
from collections import defaultdict

import btestlib.reporter as reporter
from btestlib import environments
from btestlib import utils as butils
from btestlib.constants import Services
from simpleapi.common.oauth import Auth
from simpleapi.data import defaults
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_masked_number, get_card_type
from simpleapi.steps import passport_steps as passport
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import webapi_steps

__author__ = 'slppls'


def get_purchase_token(payment_form=None, payment_url=None):
    if payment_form:
        return payment_form['purchase_token']
    if payment_url:
        parsed = urlparse.urlparse(payment_url)
        return urlparse.parse_qs(parsed.query)['purchase_token'][0]


def get_card_id(user, card_number):
    token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
    paymethods = trust.list_payment_methods(token)['payment_methods']
    for card_id in paymethods:
        if paymethods[card_id]['number'] == get_masked_number(card_number):
            return card_id


def get_xrf_token_and_cookies(user=None, payment_form=None, payment_url=None):
    def get_xrf_token_from_session():
        with reporter.step(u'Получаем xrf_token со страницы pcidss-ной формы'):
            url = environments.simpleapi_env().pcidss_web_url + \
                  'card_form?purchase_token={}'.format(purchase_token)
            r = session.get(url, verify=False, cookies=cookies)
            root = etree.HTML(r.text)
            tree = etree.ElementTree(root)
            try:
                return tree.xpath('//input[@name="xrf_token"]')[0].get('value')
            except:
                raise butils.TestsError('Could not get xrf_token')

    purchase_token = get_purchase_token(payment_form, payment_url)

    if user is not None and user != uids.anonymous:
        session = passport.get_passport_session_with_cookies(user)
        cookies = dict(yandexuid=session.cookies['yandexuid'],
                       Session_id=session.cookies['Session_id'])
    else:
        session, cookies = passport.get_yandexru_session_with_cookies()

    xrf_token = get_xrf_token_from_session()

    return xrf_token, cookies, purchase_token


class Desktop(object):
    class Default(object):
        @staticmethod
        def pay_by_card(card, user=None, save_card=False, payment_form=None, payment_url=None, **kwargs):
            card_number, exp_month, exp_year, cvn, cardholder = \
                card['card_number'], card['expiration_month'], \
                card['expiration_year'], card['cvn'], card['cardholder']
            xrf_token, cookies, purchase_token = get_xrf_token_and_cookies(user, payment_form, payment_url)

            if save_card:
                webapi_steps.update_payment(purchase_token, bind_card='true')
            webapi_steps.pcidss_start_payment(purchase_token, defaults.PayMethodsInWeb.new_card, card_number,
                                              exp_month, exp_year, cardholder, cvn, xrf_token, cookies=cookies)

        @staticmethod
        def pay_by_linked_card(card, card_id=None, user=None, payment_form=None, payment_url=None, **kwargs):
            xrf_token, cookies, purchase_token = get_xrf_token_and_cookies(user, payment_form, payment_url)
            card_id = card_id or get_card_id(user, card['card_number'])

            webapi_steps.pcidss_start_payment(purchase_token, payment_method=card_id, cvn=card['cvn'],
                                              xrf_token=xrf_token, cookies=cookies)

    class Tickets(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, region_id=225, **kwargs):
            Desktop.Tickets.fill_form_pay_by_card(card, user, kwargs.get('save_card'), payment_form, payment_url)

        @staticmethod
        def fill_form_pay_by_card(card, user, save_card, payment_form=None, payment_url=None):
            card_number, exp_month, exp_year, cvn, cardholder = \
                card['card_number'], card['expiration_month'], card['expiration_year'], card['cvn'], card['cardholder']
            xrf_token, cookies, purchase_token = get_xrf_token_and_cookies(user, payment_form, payment_url)
            webapi_steps.preview_payment(purchase_token, card_type=get_card_type(card))  # без этого не работают скидки

            webapi_steps.update_payment(purchase_token, email=defaults.email)
            if save_card:
                webapi_steps.update_payment(purchase_token, bind_card='true')
            webapi_steps.pcidss_start_payment(purchase_token, defaults.PayMethodsInWeb.new_card, card_number, exp_month,
                                              exp_year, cardholder, cvn, xrf_token, cookies=cookies)

        @staticmethod
        def pay_by_linked_card(card, save_card=None, user=None, card_id=None,
                               payment_form=None, payment_url=None, **kwargs):
            xrf_token, cookies, purchase_token = get_xrf_token_and_cookies(user, payment_form, payment_url)
            card_id = card_id or get_card_id(user, card['card_number'])
            webapi_steps.preview_payment(purchase_token, card_type=get_card_type(card))  # без этого не работают скидки

            webapi_steps.update_payment(purchase_token, email=defaults.email, bind_card='true')
            if save_card:
                webapi_steps.update_payment(purchase_token, bind_card='true')
            # if promocode:
            webapi_steps.pcidss_start_payment(purchase_token, payment_method=card_id, cvn=card['cvn'],
                                              xrf_token=xrf_token, cookies=cookies)

        @staticmethod
        def pay_by_only_promocode(user=None, payment_form=None, payment_url=None, **kwargs):
            xrf_token, cookies, purchase_token = get_xrf_token_and_cookies(user, payment_form, payment_url)
            promocode_id = kwargs.get('promocode_id')

            webapi_steps.update_payment(purchase_token, email=defaults.email)
            webapi_steps.update_payment(purchase_token, promocode_id=promocode_id)
            webapi_steps.preview_payment(purchase_token, promocode_id=promocode_id)
            webapi_steps.pcidss_start_payment(purchase_token, payment_method=defaults.PayMethodsInWeb.new_promocode)

    paymethods_by_services = defaultdict(lambda: Desktop.Default)
    paymethods_by_services.update({Services.TICKETS: Tickets,
                                   Services.EVENTS_TICKETS: Tickets,
                                   Services.EVENTS_TICKETS_NEW: Tickets,
                                   Services.EVENTS_TICKETS3: Tickets,
                                   })


def get_paymethods(service, template_tag=defaults.TemplateTag.desktop):
    mapping = {
        defaults.TemplateTag.desktop: Desktop,
        # TODO-slppls: add this for nonweb too
        # defaults.TemplateTag.mobile: Mobile,
        # defaults.TemplateTag.smarttv: SmartTV,
    }
    return mapping.get(template_tag, Desktop).paymethods_by_services[service]
