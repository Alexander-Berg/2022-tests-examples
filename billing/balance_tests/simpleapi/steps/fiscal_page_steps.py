# coding=utf-8
from btestlib import reporter
from simpleapi.pages import fiscal_pages

__author__ = 'sunshineguy'


class FiscalPageTakeData(object):
    def __init__(self, service_order_id=None):
        self.service_order_id = service_order_id
        self.fiscal_page = fiscal_pages.TaxiMultiCheckPage(service_order_id=self.service_order_id)

    def take_fiscal_basic_data(self, fiscal_number):
        with reporter.step(u'Считываем шапку чека № = {}'.format(fiscal_number)):
            return {
                'firm_name': self.fiscal_page.get_firm_name(fiscal_number),
                'location': self.fiscal_page.get_location(fiscal_number),
                'firm_inn': self.fiscal_page.get_firm_inn(fiscal_number),
                'fiscal_title': self.fiscal_page.get_title(fiscal_number),
                'n_number': self.fiscal_page.get_n_number(fiscal_number),
                'n_auto': self.fiscal_page.get_n_auto(fiscal_number),
                'relay': self.fiscal_page.get_relay(fiscal_number),
                'data': self.fiscal_page.get_date(fiscal_number),
            }

    def take_fiscal_total_field(self, fiscal_number):
        with reporter.step(u'Считываем названия итоговых полей чека № = {}'.format(fiscal_number)):
            return {
                'agent': self.fiscal_page.get_agent(fiscal_number, return_name=True),
                'total_amount': self.fiscal_page.get_total_amount(fiscal_number, return_name=True),
                'e-money': self.fiscal_page.get_emoney(fiscal_number, return_name=True),
                'n_kkt': self.fiscal_page.get_n_kkt(fiscal_number, return_name=True),
                'fd': self.fiscal_page.get_fd(fiscal_number, return_name=True),
                'fp': self.fiscal_page.get_fp(fiscal_number, return_name=True),
                'fn': self.fiscal_page.get_fn(fiscal_number, return_name=True),
                'sno': self.fiscal_page.get_sno(fiscal_number, return_name=True),
                'zn_kkt': self.fiscal_page.get_zn_kkt(fiscal_number, return_name=True),
                'recipient_email': self.fiscal_page.get_recipient_email(fiscal_number, return_name=True),
                'sender_email': self.fiscal_page.get_sender_email(fiscal_number, return_name=True),
                'fns_website': self.fiscal_page.get_fns_website(fiscal_number, return_name=True),
                'amount-': self.fiscal_page.get_nds_amount(fiscal_number, return_name=True),
            }

    def take_fiscal_total_data(self, fiscal_number):
        with reporter.step(u'Считываем данные итоговых полей чека № = {}'.format(fiscal_number)):
            return {
                'agent': self.fiscal_page.get_agent(fiscal_number, return_value=True),
                'total_amount': self.fiscal_page.get_total_amount(fiscal_number, return_value=True),
                'e-money': self.fiscal_page.get_emoney(fiscal_number, return_value=True),
                'n_kkt': self.fiscal_page.get_n_kkt(fiscal_number, return_value=True),
                'fd': self.fiscal_page.get_fd(fiscal_number, return_value=True),
                'fp': self.fiscal_page.get_fp(fiscal_number, return_value=True),
                'fn': self.fiscal_page.get_fn(fiscal_number, return_value=True),
                'sno': self.fiscal_page.get_sno(fiscal_number, return_value=True),
                'zn_kkt': self.fiscal_page.get_zn_kkt(fiscal_number, return_value=True),
                'recipient_email': self.fiscal_page.get_recipient_email(fiscal_number, return_value=True),
                'sender_email': self.fiscal_page.get_sender_email(fiscal_number, return_value=True),
                'fns_website': self.fiscal_page.get_fns_website(fiscal_number, return_value=True),
                'amount-': self.fiscal_page.get_nds_amount(fiscal_number, return_value=True)
            }

    def take_fiscal_order_field(self, fiscal_number, order):
        with reporter.step(u'Считываем названия полей для ордера № = {} в чеке № = {}'.format(order, fiscal_number)):
            return {
                'order_number': self.fiscal_page.get_order_number(fiscal_number, order, return_name=True),
                'order_name': self.fiscal_page.get_order_name(fiscal_number, order, return_name=True),
                'unit_price': self.fiscal_page.get_unit_price(fiscal_number, order, return_name=True),
                'count': self.fiscal_page.get_count(fiscal_number, order, return_name=True),
                'nds': self.fiscal_page.get_nds(fiscal_number, order, return_name=True),
                'order_amount': self.fiscal_page.get_order_current_amount(fiscal_number, order, return_name=True),
            }

    def take_fiscal_order_data(self, fiscal_number, order):
        with reporter.step(u'Считываем данные полей для ордера № = {} в чеке № = {}'.format(order, fiscal_number)):
            return {
                'order_number': self.fiscal_page.get_order_number(fiscal_number, order, return_value=True),
                'order_name': self.fiscal_page.get_order_name(fiscal_number, order, return_value=True),
                'unit_price': self.fiscal_page.get_unit_price(fiscal_number, order, return_value=True),
                'count': self.fiscal_page.get_count(fiscal_number, order, return_value=True),
                'nds': self.fiscal_page.get_nds(fiscal_number, order, return_value=True),
                'order_amount': self.fiscal_page.get_order_current_amount(fiscal_number, order, return_value=True),
            }
