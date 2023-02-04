# coding: utf-8
from selenium.webdriver.common.by import By
from btestlib import reporter
from btestlib.constants import Services
from btestlib.utils import TestsError
from simpleapi.common.utils import WebDriverProvider

__author__ = 'sunshineguy'
"""

"""
LINK_TO_TAXI_MULTI_CHECK = 'https://trust-test.yandex.ru/receipts/{}-'.format(Services.TAXI.id)


class TaxiMultiCheckPage(object):
    XPATH_FIRM_NAME = "/div[@class='firm-name']"
    XPATH_LOCATION = "/div[@class='location']"
    XPATH_FIRM_INN = "/div[@class='firm-inn']"
    XPATH_TITLE = "/div[@class='title']"
    XPATH_N_NUMBER = "/table[@class='info-table'][1]//tr[1]/td[1]"
    XPATH_N_AUTO = "/table[@class='info-table'][1]//tr[1]/td[2]"
    XPATH_RELAY = "/table[@class='info-table'][1]//tr[2]/td[1]"
    XPATH_DATE = "/table[@class='info-table'][1]//tr[2]/td[2]"
    # Этот блок xpath используется в some_xpath_in_check_position выбор данных
    # осуществляется по номеру позиции,
###############################################################################
    NUMBER_VALUE = 1
    NAME_VALUE = 2
    UNIT_PRICE = 3
    COUNT = 4
    NDS = 5
    CURRENT_AMOUNT = 6
###############################################################################
    # Этот блок xpath используется в some_xpath_in_total_info выбор данных
    # осуществляется по номеру позиции,
###############################################################################
    AMOUNT = 1
    AGENT = 2
    TOTAL_AMOUNT = 3
    EMONEY = 4
###############################################################################
    # Этот блок xpath используется в some_xpath_in_check_info выбор данных
    # осуществляется по номеру позиции,
###############################################################################
    N_KKT = 1
    FD = 2
    FN = 3
    FP = 4
    SNO = 5
    ZN_KKT = 6
###############################################################################
    # Этот блок xpath используется в some_text_from_contact_info выбор данных
    # осуществляется по номеру позиции,
###############################################################################
    RECIPIENT_EMAIL = 1
    SENDER_EMAIL = 2
    FNS_WEBSITE = 3
###############################################################################

    def __init__(self, driver=None, service_order_id=None):
        if not driver:
            self.driver = WebDriverProvider.get_instance().get_driver()
        else:
            self.driver = driver
        self.driver.get(LINK_TO_TAXI_MULTI_CHECK + str(service_order_id) +'/')

    @staticmethod
    def take_name_or_value(return_name=False, return_value=False):
        if return_value and not return_name:
            name_or_value = 2
        elif return_name and not return_value:
            name_or_value = 1
        else:
            raise TestsError(u'Неправильно передан набор аргументов: return_name = {}, return_value = {}'
                             u' аргументы не должны быть равны!'.format(return_name, return_value))
        return name_or_value

    def get_check_text(self, xpath, check_number=1):
        """
        Метод  возвращает текст из заданного поля в заданном чеке.
        Необходимо передать валидный XPath и номер чека(по дефолту первый)
        """
        return self.driver.find_element(By.XPATH,
                                        ".//div[@class='container'][{}]".format(
                                            check_number) + xpath).text

    def some_text_from_check_position(self, check_number, xpath, order_number=1, return_name=False, return_value=False):
        """
        Метод возвращает текст значения(по дефолту)/наименования поля,
        переданного в xpath, из полей по текущей позиции  из структуры orders,
        передается в order_number.
        Для возвращения текста значение поля - return_value = True
        Для возвращения текста наименование поля - return_name = True
        return_name не должно быть равно return_value
        Номер чека передавать в check_number
        """
        final_xpath = "//div[@class='receipt'][{}]/table[{}]//td[{}]".format(order_number, xpath,
                                                                             self.take_name_or_value(return_name,
                                                                                                     return_value))
        return self.get_check_text(final_xpath, check_number)

    def some_text_from_total_info(self, check_number, xpath, return_name=False, return_value=False):
        """
        Метод возвращает текст значения(по дефолту)/наименования поля,
        переданного в xpath, из итоговых полей чека
        Для возвращения текста значение поля - return_value = True
        Для возвращения текста наименование поля - return_name = True
        return_name не должно быть равно return_value
        Номер чека передавать в check_number
        """
        final_xpath = "//table[@class='totals-table']//tr[{}]/td[{}]".format(xpath,
                                                                             self.take_name_or_value(return_name,
                                                                                                     return_value))
        return self.get_check_text(final_xpath, check_number)

    def some_text_from_check_info(self, check_number, xpath, return_name=False, return_value=False):
        """
        Метод возвращает текст значения(по дефолту)/наименования поля,
        переданного в xpath, из информационных полей в чеке.
        Для возвращения текста значение поля - return_value = True
        Для возвращения текста наименование поля - return_name = True
        return_name не должно быть равно return_value
        Номер чека передавать в check_number
        """
        final_xpath = "//div[@class='info']//table[{}]//td[{}]".format(xpath, self.take_name_or_value(return_name,
                                                                                                      return_value))
        return self.get_check_text(final_xpath, check_number)

    def some_text_from_contact_info(self, check_number, xpath, return_name=False, return_value=False):
        """
        Метод возвращает текст значения(по дефолту)/наименования поля,
        переданного в xpath, из контактной инфорации в чеке.
        Для возвращения текста значение поля - return_value = True
        Для возвращения текста наименование поля - return_name = True
        return_name не должно быть равно return_value
        Номер чека передавать в check_number
        """
        final_xpath = "//table[@class='info-table'][2]//tr[{}]/td[{}]".format(xpath,
                                                                              self.take_name_or_value(return_name,
                                                                                                      return_value))
        return self.get_check_text(final_xpath, check_number)

    def get_firm_name(self, check_number):
        reporter.logger().debug(u'Получаем имя фирмы')
        return self.get_check_text(self.XPATH_FIRM_NAME, check_number)

    def get_location(self, check_number):
        reporter.logger().debug(u'Получаем локализацию')
        return self.get_check_text(self.XPATH_LOCATION, check_number)

    def get_firm_inn(self, check_number):
        reporter.logger().debug(u'Получаем ИНН фирмы')
        return self.get_check_text(self.XPATH_FIRM_INN, check_number)

    def get_title(self, check_number):
        reporter.logger().debug(u'Получаем заголовок чека')
        return self.get_check_text(self.XPATH_TITLE, check_number)

    def get_n_number(self, check_number):
        reporter.logger().debug(u'Получаем номер')
        return self.get_check_text(self.XPATH_N_NUMBER, check_number)

    def get_n_auto(self, check_number):
        reporter.logger().debug(u'Получаем номер авто')
        return self.get_check_text(self.XPATH_N_AUTO, check_number)

    def get_relay(self, check_number):
        reporter.logger().debug(u'Получаем номер смены')
        return self.get_check_text(self.XPATH_RELAY, check_number)

    def get_date(self, check_number):
        reporter.logger().debug(u'Получаем дату')
        return self.get_check_text(self.XPATH_DATE, check_number)

    def get_order_number(self, check_number, order_number=1, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем номер платежа')
        return self.some_text_from_check_position(check_number,
                                                  self.NUMBER_VALUE,
                                                  order_number,
                                                  return_name, return_value)

    def get_order_name(self, check_number, order_number=1, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем имя ордера')
        return self.some_text_from_check_position(check_number,
                                                  self.NAME_VALUE,
                                                  order_number,
                                                  return_name, return_value)

    def get_unit_price(self, check_number, order_number=1, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем цену за еденицу')
        return self.some_text_from_check_position(check_number,
                                                  self.UNIT_PRICE,
                                                  order_number,
                                                  return_name, return_value)

    def get_count(self, check_number, order_number=1, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем количество товаров')
        return self.some_text_from_check_position(check_number,
                                                  self.COUNT,
                                                  order_number,
                                                  return_name, return_value)

    def get_nds(self, check_number, order_number=1, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем НДС')
        return self.some_text_from_check_position(check_number,
                                                  self.NDS,
                                                  order_number,
                                                  return_name, return_value)

    def get_order_current_amount(self, check_number, order_number=1,
                                 return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем итоговую сумму по ордеру')
        return self.some_text_from_check_position(check_number,
                                                  self.CURRENT_AMOUNT,
                                                  order_number,
                                                  return_name, return_value)

    def get_nds_amount(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем сумму НДС')
        return self.some_text_from_total_info(check_number,
                                              self.AMOUNT,
                                              return_name, return_value)

    def get_agent(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем агента')
        return self.some_text_from_total_info(check_number,
                                              self.AGENT,
                                              return_name, return_value)

    def get_total_amount(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем итоговую сумму')
        return self.some_text_from_total_info(check_number,
                                              self.TOTAL_AMOUNT,
                                              return_name, return_value)

    def get_emoney(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем сумму, которая оплачена через электронные деньги')
        return self.some_text_from_total_info(check_number,
                                              self.EMONEY,
                                              return_name, return_value)

    def get_n_kkt(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем Н ККТ')
        n_kkt = self.some_text_from_check_info(check_number, self.N_KKT,
                                               return_name, return_value)
        return n_kkt if return_name else int(n_kkt)

    def get_fd(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем ФД')
        fd = self.some_text_from_check_info(check_number, self.FD,
                                            return_name, return_value)
        return fd if return_name else int(fd)

    def get_fn(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем ФН')
        fn = self.some_text_from_check_info(check_number, self.FN,
                                            return_name, return_value)
        return fn if return_name else int(fn)

    def get_fp(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем ФП')
        fp = self.some_text_from_check_info(check_number, self.FP,
                                            return_name, return_value)
        return fp if return_name else int(fp)

    def get_sno(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем СНО')
        return self.some_text_from_check_info(check_number,
                                              self.SNO,
                                              return_name, return_value)

    def get_zn_kkt(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем ЗН ККТ')
        zn_kkt = self.some_text_from_check_info(check_number, self.ZN_KKT,
                                                return_name, return_value)
        return zn_kkt if return_name else int(zn_kkt)

    def get_recipient_email(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем e-mail получателя(клиента)')
        return self.some_text_from_contact_info(check_number,
                                                self.RECIPIENT_EMAIL,
                                                return_name, return_value)

    def get_sender_email(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем e-mail отправителя(сервис)')
        return self.some_text_from_contact_info(check_number,
                                                self.SENDER_EMAIL,
                                                return_name, return_value)

    def get_fns_website(self, check_number, return_name=False, return_value=False):
        reporter.logger().debug(u'Получаем сайт ФНС')
        return self.some_text_from_contact_info(check_number,
                                                self.FNS_WEBSITE,
                                                return_name, return_value)