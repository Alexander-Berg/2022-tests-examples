# coding: utf-8

import sys
from urllib import urlencode
from urlparse import urlparse, parse_qs
import time

from hamcrest import empty, is_not, equal_to, contains_string, any_of
from selenium.common.exceptions import NoSuchElementException
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support import expected_conditions as ec
from selenium.webdriver.support.ui import Select
from selenium.webdriver.support.wait import WebDriverWait

import btestlib.environments as envs
from btestlib.web import AlfaBankPayForm
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Passports, Users, Cards

log = reporter.logger()

DEFAULT_WAIT_TIMEOUT = 60


class Driver(utils.Web.DriverProvider):
    # добавим авторизацию в контекст менеджер драйвера
    def __init__(self, user=Users.YB_ADM, passport=Passports.PROD):
        super(Driver, self).__init__()
        self.user = user
        self.passport = passport

    def __enter__(self):
        try:
            driver = super(Driver, self).__enter__()
            passport_steps.auth_post(driver, self.user, self.passport)
            return driver
        except:
            if self.__exit__(*sys.exc_info()):
                pass
            else:
                raise

    def __exit__(self, exc_type, exc_val, exc_tb):
        return super(Driver, self).__exit__(exc_type, exc_val, exc_tb)


WAIT_DELAY = 60


def wait_and_get(driver, locator):
    WebDriverWait(driver, WAIT_DELAY).until(
        ec.presence_of_element_located(locator))
    return driver.find_element(*locator)


def wait_and_click(driver, locator, timeout=WAIT_DELAY):
    WebDriverWait(driver, timeout).until(
        ec.presence_of_element_located(locator) and ec.element_to_be_clickable(locator))
    el = driver.find_element(*locator)
    el.click()
    return el


def do_until(action, condition, driver, locator, times=15, delay=1):
    action()
    try_number = 0
    if not condition(driver, locator):
        try_number += 1
        time.sleep(delay)
        action()
        if try_number == times:
            raise Exception

class DefaultPage(object):
    DESCRIPTION = u'Страница'

    def __init__(self, driver):
        check_errors_absence_on_page(driver)

        self.driver = driver

    @classmethod
    def url(cls, *args, **kwargs):
        raise NotImplementedError(u'Метод url не реализован в данном классе: {}'.format(cls.__name__))

    @classmethod
    def open(cls, driver, *args, **kwargs):
        driver.get(cls.url(*args, **kwargs), name=cls.DESCRIPTION)
        return cls(driver)


class AdminInterface(object):
    class ClientEditPage(DefaultPage):
        DESCRIPTION = u"Страница редактирования клиента"

        NON_RESIDENT_CHECKBOX = (By.XPATH, '//input[@name="is-non-resident"]', u"Чекбокс 'Нерезидент'")
        FULLNAME_INPUT = (By.XPATH, '//input[@name="fullname"]', u"Поле 'Полное название'")
        CURRENCY_PAYMENT_SELECT = (By.XPATH, '//select[@name="currency-payment"]', u"Поле для выбора 'Валюта расчетов'")
        DOMAIN_CHECK_SELECT = (By.XPATH, '//select[@name="domain-check-status"]', u"Поле для выбора 'Проверка доменов'")
        REGION_SELECT = (By.XPATH, '//select[@name="region_id"]', u"Поле для выбора 'Регион'")
        DENY_OVERDRAFT_CHECKBOX = (By.XPATH, '//input[@name="deny_overdraft"]',
                                   u"Чекбокс 'Не должен получать овердрафт'")
        FORCE_CONTRACTLESS_INVOICE_CHECKBOX = (By.XPATH, '//input[@name="force_contractless_invoice"]',
                                               u"Чекбокс 'Выставление счетов по оферте'")
        INTERCOMPANY_SELECT = (By.XPATH, '//select[@name="intercompany"]', u"Поле для выбора 'Интеркомпани'")
        SAVE_BUTTON = (By.XPATH, '//form[@action="set-client.xml"]//input[@type="submit"]', u"Кнопка 'Сохранить'")

        @classmethod
        def url(cls, client_id):
            return '{base_url}/editclient.xml?tcl_id={client_id}'.format(base_url=envs.balance_env().balance_ai,
                                                                         client_id=client_id)

        def set_non_resident_fields(self, currency_payment_char_code):
            with reporter.step(u'Заполняем поля клиента-нерезидента ({})'.format(currency_payment_char_code)):
                self.driver.find_element(*self.NON_RESIDENT_CHECKBOX).click()
                self.driver.find_element(*self.FULLNAME_INPUT).send_keys(u'Клиент-нерезидент')
                currency = Select(self.driver.find_element(*self.CURRENCY_PAYMENT_SELECT))
                currency.select_by_value(currency_payment_char_code)

        def set_domain_check_status(self, status_id):
            domain_check_status = Select(self.driver.find_element(*self.DOMAIN_CHECK_SELECT))
            domain_check_status.select_by_value(str(status_id))

        def set_region(self, region_id):
            with reporter.step(u'Выбираем регион {}'.format(region_id)):
                region = Select(self.driver.find_element(*self.REGION_SELECT))
                region.select_by_value(str(region_id))

        def is_region_select_present(self):
            return utils.Web.is_element_present(self.driver, self.REGION_SELECT)

        def set_deny_overdraft(self):
            self.driver.find_element(*self.DENY_OVERDRAFT_CHECKBOX).click()
            WebDriverWait(self.driver, WAIT_DELAY).until(ec.alert_is_present())
            self.driver.switch_to.alert.accept()

        def is_deny_overdraft_checkbox_present(self):
            return utils.Web.is_element_present(self.driver, self.DENY_OVERDRAFT_CHECKBOX)

        def set_force_contractless_invoice(self):
            self.driver.find_element(*self.FORCE_CONTRACTLESS_INVOICE_CHECKBOX).click()
            WebDriverWait(self.driver, WAIT_DELAY).until(ec.alert_is_present())
            self.driver.switch_to.alert.accept()

        def is_force_contractless_invoice_checkbox_present(self):
            return utils.Web.is_element_present(self.driver, self.FORCE_CONTRACTLESS_INVOICE_CHECKBOX)

        def set_intercompany(self, intercompany):
            with reporter.step(u'Выбираем Интеркомпани {}'.format(intercompany)):
                intercompany_select = Select(self.driver.find_element(*self.INTERCOMPANY_SELECT))
                intercompany_select.select_by_value(intercompany)

        def save_client(self):
            with reporter.step(u'Нажимаем кнопку Сохранить'):
                save_btn = self.driver.find_element(*self.SAVE_BUTTON)
                save_btn.submit()
                check_errors_absence_on_page(self.driver)

    class ContractEditPage(DefaultPage):
        DESCRIPTION = u"Страница редактирования договора"
        FOOTER = (By.XPATH, '/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]', u'Дожидаеся появления футера')

        CANCELLED_CHECKBOX = (By.ID, 'is-cancelled-div', u'Чек-бокс "Аннулирован"')
        @classmethod
        def url(cls, contract_id):
            return "{base_url}/contract-edit.xml?contract_id={contract_id}".format(
                base_url=envs.balance_env().balance_ai,
                contract_id=contract_id)

        @classmethod
        def open_and_wait(cls, driver, contract_id):
            page = cls.open(driver, contract_id)
            utils.wait_until(lambda: utils.Web.is_element_present(driver, cls.FOOTER), equal_to(True),
                             descr=u'Ожидаем загрузки страницы договора')
            return page

        def get_cancelled_checkbox_attributes(self):
            return wait_and_get(self.driver, self.CANCELLED_CHECKBOX)


    class ContractPage(DefaultPage):
        DESCRIPTION = u"Страница договора"

        @classmethod
        def url(cls, contract_id):
            return "{base_url}/contract.xml?contract_id={contract_id}".format(
                base_url=envs.balance_env().balance_ai,
                contract_id=contract_id)

    class ContractPageNew(DefaultPage):
        DESCRIPTION = u"Страница нового договора"

        SIGNED_BUTTON = (By.ID, 'is-signed', u"Договор на странице")

        @classmethod
        def url(cls):
            return "{base_url}/contract-edit.xml".format(
                base_url=envs.balance_env().balance_ai)

        def is_signed_button_active(self):
            signed_button = self.driver.find_element(*self.SIGNED_BUTTON)
            pass

    class InvoicePage(DefaultPage):
        DESCRIPTION = u"Страница 'Счет' (АИ)"

        INVOICE_REEXPORT_BUTTON = (By.XPATH,
                                   u"//button[@id='invoice-status__btn-reexport']",
                                   u"Кнопка 'Перевыгрузить'")

        INVOICE_REEXPORT_BUTTON_OLD = (By.XPATH,
                                   u"//form[@action='reexport-object.xml']/input[@name='obj_name' and @value='invoice']/following-sibling::input[@type='submit']",
                                   u"Кнопка 'Перевыгрузить'")

        INVOICE_INFO_TABLE = utils.Web.table((By.XPATH, "//div[@class='subcontent'][1]//table"),
                                             column_names=['date', 'owner', 'is_agent', 'paysys', 'invoice_sum',
                                                           'paid_sum', 'consumed_sum_on_orders', 'acts_sum', 'manager'],
                                             headers_as_row=True,
                                             name=u'Таблица информации о счете')

        SET_BAD_DEBT_BUTTON = (By.XPATH,
                               u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-acts')]//button[contains(.,'Плохой долг')]",
                               u"Кнопка 'Плохой долг'")

        CONFIRM_PAYMENT_BUTTON = (By.XPATH,
                                  u"//*[@id='confirm-payment__btn']",
                                  u"Кнопка 'Внести оплату'")

        ## Блок возврата в кредит
        RETURN_ON_CREDIT_BUTTON = (By.XPATH, u"//button[@id='rollback__btn']",u'Кнопка "Вернуть неоткрученное в кредит"')
        RETURN_ON_CREDIT_BUTTON_DISABLE = (By.XPATH, u"//button[@id='rollback__btn'][contains(@aria-disabled, 'true')]",u'Кнопка "Вернуть неоткрученное в кредит" недоступна')
        AMOUNT_IN_INVOICE_CURRENCY_INPUT = (By.XPATH,
                                            u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-rollback')]//input[@class='Textinput-Control']",
                                            u'Поле "Сумма в валюте счета"')
        ORDER_INPUT = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-rollback')]//input[@id='rollback__order']", u'Поле "Заказ"')
        ORDER_OPERATION_QTY_FIELD = classmethod(utils.Web.dynamic_locator(By.XPATH,
        u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-operations')]//tr[./td[contains(.,'с заказа') and ./a[contains(.,'{service_order_id}')]]]"))

        INFORM_MESSAGE = (By.XPATH, u"//*[@id='react-container']/main/section[7]/form/div[4]/div[2]/div",
                          u'Сообщение при попытке возврата в кредит')

        ## Блок снятия средств
        WITHDRAWAL_BUTTON = (By.XPATH, u"//*[@id='rollback__btn']",
                             u'Кнопка "Снять средства со счета"')
        ROLLBACK_BLOCK = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-rollback')]//div", u'Блок возврата в кредит/снятия средств')
        CONSUMES_BLOCK = (By.XPATH, u"//*[@id='consumes-container']//div/div/table/thead", u'Блок заявок')
        OPERATIONS_BLOCK = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-operations')]", u'Блок операций')
        PERSON_BLOCK = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-person')]", u'Блок плательщика')

        ACTS_CONTAINER = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-acts')]"
                                    u"//table", u"Контейнер 'История актов'")
        ACTS_TABLE_CONTAINER = classmethod(utils.Web.table(
            (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-acts')]//table"),
            column_names=['act_number', 'factura_number', 'date', 'amount',
                          'tax', 'tax_amount'],
            name=u"Таблица актов"))
        PRINT_FORM_BLOCK = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-print-form')]", u'Блок ПФ')

        CONFIRM_ACTION_BUTTON = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-messages__accept')]",
                                 u'Кнопка подтверждения действия')
        CONFIRMATION_TEXT = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-messages__text')]",
                             u'Текст подтверждения')

        class CloseInvoice(object):
            # SHOW_CALENDAR_BUTTON = (By.XPATH,
            #                         u"//form[@action='close_invoice.xml']//input[@class='calendar-button']",
            #                         u"Кнопка 'Показать календарь'")
            SHOW_CALENDAR_BUTTON = (By.XPATH,
                                    u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-close-invoice')]"
                                    u"//input[@type='text']",
                                    u"Кнопка 'Показать календарь'")

            # TODAY_BUTTON = (By.XPATH, u"//td[@class='set-today']", u"Кнопка календаря 'Сегодня'")
            TODAY_BUTTON = (By.XPATH,
                                    u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-close-invoice')]//div[@class='react-datepicker__today-button']",
                                    u"Кнопка 'Показать календарь'")

            # SUBMIT_BUTTON = (By.XPATH,
            #                  u"//form[@action='close_invoice.xml']/input[@type='submit']",
            #                  u"Кнопка 'Закрыть счет'")
            SUBMIT_BUTTON = (By.XPATH,
                                    u"//button[@id='close-invoice__btn']",
                                    u"Кнопка 'Закрыть счёт'")

            ACTS_CONTAINER = (By.XPATH,
                                    u"//*[contains(concat(' ', @class, ' '), 'yb-invoice-acts')]",
                                    u"Контейнер 'История актов'")

        @classmethod
        def url(cls, invoice_id):
            return '{base_url}/invoice.xml?invoice_id={invoice_id}'.format(base_url=envs.balance_env().balance_ai,
                                                                           invoice_id=invoice_id)


        @classmethod
        def open_url(cls, driver, url):
            driver.get(url, name=u"Страница счета")
            page_object = cls(driver=driver)
            time.sleep(10)
            return page_object

        # def wait_for_data(self):
        #     utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.PAGE_TITLE),
        #                      equal_to(True), descr=u'Waiting for data on page')


        def is_confirm_payment_button_present(self):
            wait_and_get(self.driver, self.PERSON_BLOCK)
            return utils.Web.is_element_present(self.driver, self.CONFIRM_PAYMENT_BUTTON)

        def is_print_form_block_present(self):
            wait_and_get(self.driver, self.PERSON_BLOCK)
            return utils.Web.is_element_present(self.driver, self.PRINT_FORM_BLOCK)

        def confirm_payment(self):
            with reporter.step(u'Нажимаем кнопку Внести оплату'):
                # confirm_payment_btn = self.driver.find_element(*self.CONFIRM_PAYMENT_BUTTON)
                confirm_payment_btn = wait_and_get(self.driver, self.CONFIRM_PAYMENT_BUTTON)
                confirm_payment_btn.click()
                time.sleep(1)
                self.check_alert()
                check_errors_absence_on_page(self.driver)
                wait_and_get(self.driver, self.OPERATIONS_BLOCK)

        def is_reexport_invoice_button_present(self):
            return utils.Web.is_element_present(self.driver, self.INVOICE_REEXPORT_BUTTON)

        def reexport_invoice(self):
            # reexport_btn = self.driver.find_element(*self.INVOICE_REEXPORT_BUTTON_OLD)
            reexport_btn = wait_and_get(self.driver, self.INVOICE_REEXPORT_BUTTON)
            utils.wait_until(lambda: bool(ec.element_to_be_clickable(self.INVOICE_REEXPORT_BUTTON)(self.driver)), equal_to(True),
                             descr=u'Ожидаем загрузки страницы договора')
            reexport_btn.click()
            check_errors_absence_on_page(self.driver)

        def reexport_invoice_without_check_errors(self):
            # reexport_btn = self.driver.find_element(*self.INVOICE_REEXPORT_BUTTON)
            reexport_btn = wait_and_get(self.driver, self.INVOICE_REEXPORT_BUTTON)
            reexport_btn.click()

        def get_pop_up_alert_text(self):
            WebDriverWait(self.driver, WAIT_DELAY).until(ec.alert_is_present())
            pop_up = self.driver._driver.switch_to_alert()
            return pop_up.text

        def is_set_bad_debt_button_present(self):
            return utils.Web.is_element_present(self.driver, self.SET_BAD_DEBT_BUTTON)

        def set_bad_debt(self):
            with reporter.step(u'Нажимаем кнопку Плохой долг'):
                # set_bad_debt_btn = self.driver.find_element(*self.SET_BAD_DEBT_BUTTON)
                set_bad_debt_btn = wait_and_get(self.driver, self.SET_BAD_DEBT_BUTTON)
                set_bad_debt_btn.click()
                self.check_alert()
                check_errors_absence_on_page(self.driver)

        def rollback_button(self, is_credit=True, expected_alert_text=None):
            rollback_button_name = self.RETURN_ON_CREDIT_BUTTON if is_credit else self.WITHDRAWAL_BUTTON
            rollback_button = wait_and_get(self.driver, rollback_button_name)
            time.sleep(2)
            rollback_button.click()
            if expected_alert_text:
                self.check_alert(expected_alert_text)
            check_errors_absence_on_page(self.driver)

        def is_rollback_block_present_on_page(self):
            wait_and_get(self.driver, self.PERSON_BLOCK)
            return utils.Web.is_element_present(self.driver, self.ROLLBACK_BLOCK)

        def check_alert(self, expected_alert_text=None):
            confirmation_text = wait_and_get(self.driver, self.CONFIRMATION_TEXT)
            if expected_alert_text:
                utils.check_that(confirmation_text.text, equal_to(expected_alert_text), u'Проверяем текст алерта')
            confirmation_button = wait_and_get(self.driver, self.CONFIRM_ACTION_BUTTON)
            confirmation_button.click()

        def check_return_button_availability(self, disable=True):
            return wait_and_get(self.driver, self.RETURN_ON_CREDIT_BUTTON_DISABLE) if disable else wait_and_get(self.driver, self.RETURN_ON_CREDIT_BUTTON)


        def check_rollback_message(self, expected_text):
            rollback_message = wait_and_get(self.driver, self.INFORM_MESSAGE).text
            return utils.check_that(rollback_message, equal_to(expected_text))

        def set_amount(self, amount):
            # amount_field = self.driver.find_element(*self.AMOUNT_IN_INVOICE_CURRENCY_INPUT)
            amount_field = wait_and_get(self.driver, self.AMOUNT_IN_INVOICE_CURRENCY_INPUT)
            amount_field.send_keys(str(amount))

        def set_order(self, service_id, product_id, service_order_id, client_id):
            # order = self.driver.find_element(*self.ORDER_INPUT)
            order = wait_and_get(self.driver, self.ORDER_INPUT)
            order.send_keys(u'{service_id}-{service_order_id}: "Py_Test order {service_id}-{product_id}"'
                            .format(service_id=service_id, service_order_id=service_order_id, client_id=client_id, product_id = product_id))

            ORDER_SUGGEST = (By.XPATH, u"//div[@class='control textinput__suggest-item textinput__suggest-item_type_text']")
            order_suggest = wait_and_get(self.driver, ORDER_SUGGEST)
            order_suggest.click()
            pass
            # order.send_keys(Keys.DOWN)
            # order.send_keys(Keys.UP)
            # order.send_keys(Keys.DOWN)
            # order.send_keys(Keys.ENTER)

        def get_operation_string(self, service_order_id):
            return WebDriverWait(self.driver, WAIT_DELAY).until(
                ec.presence_of_element_located(self.ORDER_OPERATION_QTY_FIELD(service_order_id=service_order_id))).text

        def is_element_present_on_page(self, target_element):
            try:
                self.driver.find_element(*target_element)
            except NoSuchElementException:
                return False
            return True

        def is_order_field_on_page(self):
            return self.is_element_present_on_page(self.ORDER_INPUT)

        def is_close_invoice_button_present(self):
            return utils.Web.is_element_present(self.driver, self.CloseInvoice.SUBMIT_BUTTON)

        def close_invoice(self):
            with reporter.step(u'Закрываем счет'):
                calendar = wait_and_get(self.driver, self.CloseInvoice.SHOW_CALENDAR_BUTTON)
                # Новый datepicker не всегда открывается с первого клика =(
                # Кликаем пока не откроется
                time.sleep(2)
                do_until(action=calendar.click,
                         condition=utils.Web.is_element_present,
                         driver=self.driver,
                         locator=self.CloseInvoice.TODAY_BUTTON)

                set_today_btn = wait_and_get(self.driver, self.CloseInvoice.TODAY_BUTTON)
                set_today_btn.click()
                close_invoice_btn = self.driver.find_element(*self.CloseInvoice.SUBMIT_BUTTON)
                close_invoice_btn.click()

                self.check_alert()
                check_errors_absence_on_page(self.driver)

        def get_acts_history_data(self):
            wait_and_get(self.driver, self.ACTS_CONTAINER)
            return self.ACTS_TABLE_CONTAINER(self.driver).elements

    class ClientCreditPage(DefaultPage):
        DESCRIPTION = u"Страница кредитов клиента"
        FOOTER = (By.XPATH, '/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]', u'Дожидаеся появления футера')

        CONTRACTS_HEADER = (By.CSS_SELECTOR, "[class='yb-contract__title']", u'Дожидаемся появления кредитов')
        GENERAL_CREDITS_TABLE = (By.XPATH, "//*[@id='react-container']/main/div/div[2]/section/div[2]/div[1]",
                                 u'Таблица кредитов')
        INDIVIDUAL_CREDITS_TABLE = (By.XPATH, "//*[@id='react-container']/main/div/div[2]/section/div[2]/div[2]",
                                    u'Таблица индивидуальных кредитов')

        def is_credit_table_present(self):
            return utils.Web.is_element_present(self.driver, self.GENERAL_CREDITS_TABLE)

        def is_individual_credit_table_present(self):
            return utils.Web.is_element_present(self.driver, self.INDIVIDUAL_CREDITS_TABLE)

        @classmethod
        def url(cls, client_id):
            return "{base_url}/credits.xml?tcl_id={client_id}".format(
                base_url=envs.balance_env().balance_ai,
                client_id=client_id)

        @classmethod
        def open_and_wait(cls, driver, client_id):
            page = cls.open(driver, client_id)
            utils.wait_until(lambda: utils.Web.is_element_present(driver, cls.CONTRACTS_HEADER), equal_to(True),
                             descr=u'Ожидаем загрузки страницы кредитов клиента')
            return page

    class InvoicesSearchPage(DefaultPage):
        DESCRIPTION = u"Страница поиска счетов (АИ)"

        CONFIRM_PAYMENT_BUTTON = classmethod(utils.Web.dynamic_locator(
            By.XPATH,
            u"//*[@id='react-container']/main/section[2]/div[2]/div/table/tbody/tr/td[12]/button",
            u"Кнопка 'Оплата' у счета с id = {invoice_id}"))

        CONFIRM_ACTION_BUTTON = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-messages__accept')]",
                                 u'Кнопка подтверждения действия')
        CONFIRMATION_TEXT = (By.XPATH, u"//*[contains(concat(' ', @class, ' '), 'yb-messages__text')]",
                             u'Текст подтверждения')

        def check_alert(self, expected_alert_text=None):
            confirmation_text = wait_and_get(self.driver, self.CONFIRMATION_TEXT)
            if expected_alert_text:
                utils.check_that(confirmation_text.text, equal_to(expected_alert_text), u'Проверяем текст алерта')
            confirmation_button = wait_and_get(self.driver, self.CONFIRM_ACTION_BUTTON)
            confirmation_button.click()

        @classmethod
        def url(cls, **kwargs):
            page_url = '{base_url}/invoices.xml'.format(base_url=envs.balance_env().balance_ai)

            if kwargs:
                page_url = '{page_url}?{params}'.format(page_url=page_url, params=urlencode(utils.encode_obj(kwargs)))

            return page_url

        def is_confirm_payment_button_present(self, invoice_id):
            return utils.Web.is_element_present(self.driver,
                                                self.CONFIRM_PAYMENT_BUTTON(invoice_id=invoice_id))

        def confirm_payment(self, invoice_id):
            with reporter.step(u'Нажимаем кнопку Оплата у счета с id = {id}'.format(id=invoice_id)):
                confirm_payment_btn = wait_and_get(self.driver, self.CONFIRM_PAYMENT_BUTTON(invoice_id=invoice_id))
                confirm_payment_btn.click()
                self.check_alert()

                check_errors_absence_on_page(self.driver)

    class EdoPage(DefaultPage):
        DESCRIPTION = u"Страница просмотра графика ЭДО"

        OFFER_TABLE = (
            By.XPATH, "//*[@id='react-container']//div[1]/div/div/table", u"Таблица 'Оферты'")
        CONTRACT_TABLE = (
            By.XPATH, "//*[@id='react-container']//div[2]/div/div/table", u"Таблица 'Договоры'")
        CONTRACT_FIELD = (
            By.XPATH, "//*[@id='react-container']//div[2]/div/div/table/tbody/tr/td[2]",
            u"Поле 'Договор' в таблице 'Договоры'")
        UNIT_FIELD = (By.XPATH, "//*[@id='react-container']//div[1]/div/div/table/tbody/tr/td[2]",
                      u"Поле 'Бизнес-юнит' в таблице 'Оферты'")
        @classmethod
        def url(cls, client_id):
            return '{base_url}/edo.xml?client_id={client_id}'.format(base_url=envs.balance_env().balance_ai,
                                                                     client_id=client_id)

        def is_offer_table_present(self):
            return wait_and_get(self.driver, self.OFFER_TABLE)

        def is_contract_table_present(self):
            return wait_and_get(self.driver, self.CONTRACT_TABLE)

        def get_contract_eid_from_table(self):
            contract_field = wait_and_get(self.driver, self.CONTRACT_FIELD)
            return contract_field.text

        def get_unit_name_from_table(self):
            unit_field = wait_and_get(self.driver, self.UNIT_FIELD)
            return unit_field.text


class ClientInterface(object):
    class ActsPage(DefaultPage):
        DESCRIPTION = u"Страница поиска актов"

        INVOICE_EXTERNAL_INPUT = (By.XPATH, "//input[@name='invoice_eid']", u'Поле номера счета')

        ACT_EXTERNAL_INPUT = (By.XPATH, "//input[@name='external_id']", u'Поле номера акта')

        PERSON_SELECT = (By.XPATH, '//select[@name="person_id"]', u"Выпадающий список плательщиков")

        SHOW_BUTTON = (By.XPATH, u"//input[@type='submit' and @value='Вывести']", u'Кнопка "Вывести"')

        ADVANCED_SEARCH_BUTTON = (By.XPATH, u"//span[contains(.,'расширенный поиск')]", u'Кнопка "Расширенный поиск"')

        ADVANCED_FORM = (By.XPATH, '//div[@class="xf-form"]', u"Форма расширенного поиска свернута")

        ADVANCED_FORM_EXPANDED = (
            By.XPATH, '//div[@class="xf-form xf-form_acts xf-advanced"]', u"Форма расширенного поиска развернута")

        ACTS_TABLE = classmethod(utils.Web.table(
            (By.XPATH, "//form[@id='ecopies-get-form']/table"),
            column_names=['checkbox', 'act_number', 'factura_number', 'data', 'amount',
                          'tax', 'pay_till', 'invoice_number'],
            name=u"Таблица актов"))

        # FROM_INPUT = (By.XPATH, "//form[@class='sendActForm']/div[1]/div[1]/span[2]/input[1]", u'Поле "Дата от"')
        FROM_INPUT = (By.XPATH, "//input[@id='act_dt_from']", u'Поле "Дата от"')
        TO_INPUT = (By.XPATH, "//input[@id='act_dt_to']", u'Поле "Дата от"')

        EMAIL_REPORT_INPUT = (By.XPATH, "//input[@name='act-email']", u'Поле электронного адреса')

        SEND_BY_EMAIL_BUTTON = (By.XPATH, "//input[@id='btnSendByEmail']", u'Кнопка "Отправить по электронной почте"')

        GET_ALL_ORIGINAL_BUTTON = (By.XPATH, '//button[contains(.,"Получить все оригиналы")]',
                                   u'Кнопка "Получить все оригиналы"')
        GET_SELECT_ORIGINAL_BUTTON = (By.XPATH, '//button[contains(.,"Получить выбранные оригиналы (всего 1)")]',
                                      u'Кнопка "Получить выбранные оригиналы"')
        SELECT_ALL_DOCS = (By.XPATH, '//input[@name="checkall"]', u'Чекбокс для выбора всех актов')

        SUCCESS_ORDER = (By.XPATH, '//div[contains(.,"Ваш заказ отправлен и будет обработан")]',
                         u'Уведомление после успешного заказа оригиналов')
        @classmethod
        def url(cls):
            return '{base_url}/acts.xml'.format(base_url=envs.balance_env().balance_ci)

        @classmethod
        def open_act(cls, driver, external_act_id):
            page_url = '{base_url}/acts.xml?external_id={external_act_id}'.format(
                base_url=envs.balance_env().balance_ci,
                external_act_id=external_act_id)
            driver.get(page_url, name=u"Страница с найденным актом")

            return ClientInterface.ActsPage(driver)

        @classmethod
        def prepare_date(cls, raw_date):
            if not raw_date:
                return ''

            return raw_date.date().isoformat()

        @classmethod
        def open_dates(cls, driver, from_dt=None, to_dt=None):
            from_dt = cls.prepare_date(from_dt)
            to_dt = cls.prepare_date(to_dt)

            page_url = '{base_url}/acts.xml?act_dt_from={from_dt}&act_dt_to={to_dt}'.format(
                base_url=envs.balance_env().balance_ci,
                from_dt=from_dt, to_dt=to_dt)
            driver.get(page_url, name=u"Страница с актами, подходящими по датам")

            return ClientInterface.ActsPage(driver)

        def is_act_downloadable(self, external_act_id):
            with reporter.step(u"Ищем ссылку на скачиваение акта"):
                act_number_cell = self.ACTS_TABLE(self.driver).cell('act_number', act_number=external_act_id)
                is_act_downloadable = act_number_cell and \
                                      utils.Web.is_element_present(act_number_cell, (By.XPATH, './a'))
                reporter.attach(u"Можно ли скачать акт", utils.Presenter.pretty(is_act_downloadable))

                return is_act_downloadable

        def is_act_present(self, external_act_id):
            with reporter.step(u"Ищем номер акта на странице"):
                is_act_present = self.ACTS_TABLE(self.driver).cell('act_number', act_number=external_act_id) is not None
                reporter.attach(u"Есть ли номер акта на странице", utils.Presenter.pretty(is_act_present))

                return is_act_present

        def is_advanced_form_expanded(self):
            with reporter.step(u"Проверяем, развернута ли форма расширенного поиска"):
                utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.ADVANCED_FORM_EXPANDED),
                                 equal_to(True))
                utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.ADVANCED_FORM), equal_to(False))
                self.driver.find_element(*self.PERSON_SELECT)
                self.driver.find_element(*self.SHOW_BUTTON)

        def order_original_docs(self, type='all'):
            person = wait_and_get(self.driver, self.PERSON_SELECT)
            person.send_keys(Keys.DOWN)
            show_button = wait_and_get(self.driver, self.SHOW_BUTTON)
            show_button.click()
            if type == 'all':
                get_docs_btn = wait_and_get(self.driver, self.GET_ALL_ORIGINAL_BUTTON)
            if type == 'selected':
                select_all_docs = wait_and_get(self.driver, self.SELECT_ALL_DOCS)
                select_all_docs.click()
                get_docs_btn = wait_and_get(self.driver, self.GET_SELECT_ORIGINAL_BUTTON)
            get_docs_btn.click()
            wait_and_get(self.driver, self.SUCCESS_ORDER)

        def request_async_xls_report(self, email):
            with reporter.step(u'Заказываем расширенный отчёт на email'):
                email_input = self.driver.find_element(*self.EMAIL_REPORT_INPUT)
                email_input.send_keys(email)
                send_button = self.driver.find_element(*self.SEND_BY_EMAIL_BUTTON)
                send_button.click()

                # После заказа отчёта появляется всплывающее окно с подтверждением - закрываем его.
                # TODO: Нормально обработать ожидание pop-up окна
                # time.sleep(4)

                # self. driver._driver.switch_to_alert().accept()
                # check_errors_absence_on_page(self.driver)

    class PaychoosePage(DefaultPage):
        DESCRIPTION = u"Страница выбора способа оплаты"

        CONTRACT = classmethod(utils.Web.dynamic_locator(By.ID, '{contract_id}', u"Договор на странице"))
        PERSON_TYPE_PH = (By.ID, 'is_ur_0', u"Физическое лицо")
        PERSON_TYPE_UR = (By.ID, 'is_ur_1', u"Юридическое лицо")
        DIRECT_PAYMENT_LINK = (By.ID, 'direct_payment_tip', u"Ссылка на страницу direct_payment")

        INPUT_PAYMENT_BY_BANK_1301003 = (By.XPATH, '//div[@id="lab_paysys_id_1301003"]/input',
                                 u'Оплата "Рублями со счета в банке (Россия)"')
        INPUT_PAYMENT_BY_CARD_1301033 = (By.XPATH, '//div[@id="lab_paysys_id_1301033"]/input',
                                 u'Оплата "Корпоративная банковская карта юридического лица"')

        INPUT_PAYMENT_BY_CARD_1033 = (By.XPATH, '//div[@id="lab_paysys_id_1033"]/input',
                                 u'Оплата "Корпоративная банковская карта юридического лица"')
        INPUT_PAYMENT_BY_CARD_11101033 = (By.XPATH, '//div[@id="lab_paysys_id_11101033"]/input',
                                 u'Оплата "Корпоративная банковская карта юридического лица"')
        BUTTON_CHOOSE = (By.ID, 'sub', u'Кнопка "Выбрать"')

        BANK_CARD_LIMIT = (By.XPATH, '//[@id="lab_paysys_id_1002"]/div/div[1]',
                           u'Лимит суммы счета, оплата банковской картой')
        BANK_CARD_PH_UNAVAILABLE_REASON = (By.XPATH, '//*[@id="lab_paysys_id_1002"]/div/div[2]/div',
                                           u'Причина недоступности оплаты банковской картой')
        YAMONEY_PH_UNAVAILABLE_REASON = (By.XPATH, '//*[@id="lab_paysys_id_1000"]/div/div[2]/div',
                                         u'Причина недоступности оплаты Я.Деньги')

        BANK_CARD_PH = (By.XPATH, '//*[@id="lab_paysys_id_1002"]', u'Банковская карта для ФЛ')
        BANK_CARD_UR = (By.XPATH, '//*[@id="lab_paysys_id_1033"]', u'Банковская карта для ФЛ')

        EXPERIMENT_BUTTON = (By.XPATH, '//div[@class="yb-design-switcher yb-user-header-content__design-switcher"]//button',
                             u"Отключение эксперимента с новым ЛК")
        CHOOSE_CHOICES = (By.XPATH, '//a[@class="blc_change_payment_method_or_person_button"]/button')

        @classmethod
        def url(cls, request_id):
            return '{base_url}/paychoose.xml?request_id={request_id}'.format(base_url=envs.balance_env().balance_ci,
                                                                             request_id=request_id)

        def pay_by_bank_1301003(self, skip_choose=False):
            return self.pay_by(self.INPUT_PAYMENT_BY_BANK_1301003, skip_choose)

        def pay_by_card_1301033(self):
            return self.pay_by(self.INPUT_PAYMENT_BY_CARD_1301033)

        def pay_by(self, element, skip_choose=False):
            if not skip_choose:
                payment_method = self.driver.find_element(*element)
                payment_method.click()

            return self.click_choose()

        def click_choose(self):
            submit_button = self.driver.find_element(*self.BUTTON_CHOOSE)
            submit_button.submit()

            return ClientInterface.PaypreviewPage(self.driver)

        def choose_ph(self):
            person_type_ph = wait_and_get(self.driver, self.PERSON_TYPE_PH)
            person_type_ph.click()

        def choose_ur(self):
            person_type_ur = wait_and_get(self.driver, self.PERSON_TYPE_UR)
            person_type_ur.click()

        def turn_off_experiment(self):
            experiment_buttons = self.driver.find_elements(*self.EXPERIMENT_BUTTON)
            if len(experiment_buttons) == 1:
                experiment_button, = experiment_buttons
                if experiment_button._element.get_attribute('aria-pressed') == u'true':
                    experiment_button.click()
                    # choose_button = wait_and_get(self.driver, self.CHOOSE_CHOICES)
                    # choose_button.click()

        def is_direct_payment_link_present(self):
            return utils.Web.is_element_present(self.driver, self.DIRECT_PAYMENT_LINK)

        def is_choose_button_present(self):
            return utils.Web.is_element_present(self.driver, self.BUTTON_CHOOSE)

        def is_bank_card_ph_available(self):
            return utils.Web.is_element_present(self.driver, self.BANK_CARD_PH)

        def is_bank_card_ur_available(self):
            return utils.Web.is_element_present(self.driver, self.BANK_CARD_UR)

        def get_bank_card_ph_reason(self):
            return wait_and_get(self.driver, self.BANK_CARD_PH_UNAVAILABLE_REASON).text

        def get_yamoney_ph_reason(self):
            return wait_and_get(self.driver, self.YAMONEY_PH_UNAVAILABLE_REASON).text


    class PaypreviewPage(DefaultPage):
        DESCRIPTION = u"Страница выставления счета"

        CREATE_INVOICE = (By.ID, 'gensub', u"Кнопка 'Выставить счет'")

        ALERT_MESSAGE_TITLE = (By.CLASS_NAME, 'b-alert-message__title', u'Заголовок предупреждения '
                                                                        u'о просроченной задолженности')
        ALERT_MESSAGE_TEXT = (By.CLASS_NAME, 'b-alert-message__text', u'Текст предупреждения '
                                                                      u'о просроченной задолженности')
        NOTIFY_MESSAGE_TITLE = (By.CLASS_NAME, 'b-notify-message__title', u'Заголовок напоминания о задолженности"')
        NOTIFY_MESSAGE_TEXT = (By.CLASS_NAME, 'b-notify-message__text', u'Текст напоминания о задолженности"')

        EXPERIMENT_BUTTON = (
            By.XPATH, '//div[@class="yb-design-switcher yb-user-header-content__design-switcher"]//button',
            u"Отключение эксперимента с новым ЛК")

        @classmethod
        def url(cls, request_id, person_id=None, paysys_id=None, contract_id=None, **kwargs):
            # igogor **kwargs нужен чтобы можно было передать словарь с лишними полями, что возможно грех
            # a-vasin: поставить дефолтом '' вместо None?
            return '{base_url}/paypreview.xml?person_id={person_id}&request_id={request_id}&paysys_id={paysys_id}' \
                   '&contract_id={contract_id}&coupon=&mode=ci'.format(base_url=envs.balance_env().balance_ci,
                                                                       person_id=person_id or '',
                                                                       request_id=request_id or '',
                                                                       paysys_id=paysys_id or '',
                                                                       contract_id=contract_id or '')

        def generate_invoice(self):
            generate_button = self.driver.find_element(*self.CREATE_INVOICE)
            generate_button.submit()

            return ClientInterface.SuccessPage(self.driver)

        def turn_off_experiment(self):
            experiment_buttons = self.driver.find_elements(*self.EXPERIMENT_BUTTON)
            if len(experiment_buttons) == 1:
                experiment_button, = experiment_buttons
                if experiment_button._element.get_attribute('aria-pressed') == u'true':
                    experiment_button.click()
                    # choose_button = wait_and_get(self.driver, self.CHOOSE_CHOICES)
                    # choose_button.click()

        def is_alert_message_present(self):
            return utils.Web.is_element_present(self.driver, self.ALERT_MESSAGE_TITLE)

        def get_alert_message_title(self):
            return wait_and_get(self.driver, self.ALERT_MESSAGE_TITLE).text

        def get_alert_text(self):
            return wait_and_get(self.driver, self.ALERT_MESSAGE_TEXT).text

        def get_notify_message_title(self):
            return wait_and_get(self.driver, self.NOTIFY_MESSAGE_TITLE).text

        def get_notify_text(self):
            return wait_and_get(self.driver, self.NOTIFY_MESSAGE_TEXT).text

        def is_generate_invoice_button_present(self):
            return utils.Web.is_element_present(self.driver, self.CREATE_INVOICE)


    class PaystepPage(DefaultPage):
        WAIT_CONDITION = (By.XPATH, '//div[@class="yb-paystep-preload"]', u'Загрузка')
        SUM_FIELD = (By.XPATH, '//div[@class="yb-paystep-preview__sum"]', u'Сумма к оплате')
        PAYMENT_CHOICES = (By.XPATH, '//div[@class="yb-paystep-main__pay-method"]//span//div//button', u'Выбор способа оплаты')
        CARD_PAYMENT = (By.ID, 'card', u"Картой")
        BUTTON_PAY = (By.XPATH, '//section[@class="yb-paystep-main__pay yb-paystep-main-pay"]//button', u"Кнопка 'Оплатить'")

        @classmethod
        def url(cls, request_id):
            return '{base_url}/paystep.xml?request_id={request_id}'.format(base_url=envs.balance_env().balance_ci, request_id=request_id)

        def wait(self, locator=None):
            locator = locator or self.WAIT_CONDITION
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, locator), equal_to(False),
                             descr=u'Ожидаем пока прогрузится страничка')

        def choose_card(self):
            self.wait()
            wait_and_click(self.driver, self.PAYMENT_CHOICES)
            wait_and_click(self.driver, self.CARD_PAYMENT)

        def pay(self, alpha=True):
            self.wait()
            pay_button = self.driver.find_element(*self.BUTTON_PAY)
            pay_button.submit()
            self.wait(self.BUTTON_PAY)

            if self.driver.title == u'Яндекс.Баланс: Счет':
                success_page = ClientInterface.SuccessPage(self.driver)
                return success_page.pay(alpha)

            if alpha:
                return ClientInterface.AlfaBankPaymentPage(self.driver)
            else:
                return ClientInterface.TrustAsProcessingPage(self.driver)


    class SuccessPage(DefaultPage):
        DESCRIPTION = u"Страница успешного выставления счета"

        PAGE_TITLE = (By.XPATH, '//h1', u'Заголовок страницы')

        PRINT_LINK = (By.XPATH, u"//a[contains(.,'Распечатать')]", u"Ссылка 'Распечатать'")
        DOWNLOAD_LINK = (By.XPATH, u"//a[contains(.,'Загрузить в формате Word')]", u"Ссылка 'Загрузить в формате Word'")

        SEND_LINK = (By.XPATH, u"//span/strong[contains(.,'Отправить по электронной почте')]",
                     u"Ссылка 'Отправить по электронной почте'")
        EMAIL_INPUT = (By.XPATH, "//form[@action='send-invoice.xml']//input[@id='mail_addr']")
        CAPTCHA_IMG = (By.XPATH, "//form[@action='send-invoice.xml']//img[contains(@src, 'captcha')]")
        CAPTCHA_INPUT = (By.XPATH, "//form[@action='send-invoice.xml']//input[@id='captcha-rep']")
        SEND_BUTTON = (By.XPATH, "//form[@action='send-invoice.xml']//input[@id='mail_send_btn']")

        BUTTON_PAY = (By.XPATH, u'//input[@value="Оплатить"]', u"Кнопка 'Оплатить'")

        @classmethod
        def url(cls, invoice_id):
            return '{base_url}/success.xml?invoice_id={invoice_id}'.format(base_url=envs.balance_env().balance_ci,
                                                                           invoice_id=invoice_id)

        @classmethod
        def open(cls, driver, invoice_id):
            return cls.open_url(driver=driver, url=cls.url(invoice_id=invoice_id))

        @classmethod
        def open_url(cls, driver, url):
            driver.get(url, name=ClientInterface.SuccessPage.DESCRIPTION)
            page_object = cls(driver=driver)
            page_object.wait_for_data()
            return page_object

        def wait_for_data(self):
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.PAGE_TITLE),
                             equal_to(True), descr=u'Waiting for data on page')

        def get_title(self):
            page_title = self.driver.find_element(*self.PAGE_TITLE)
            return page_title.text

        def get_printable_invoice(self):
            print_link = self.driver.find_element(*self.PRINT_LINK)
            print_link.click()

            utils.Web.switch_to_opened_page(self.driver)

            return ClientInterface.InvoicePublishPage(self.driver)

        def send_print_form(self, email="test-balance-notify@yandex-team.ru"):
            with reporter.step(u'Раскрываем блок отправки ПФ на email'):
                self.driver.find_element(*self.SEND_LINK).click()

            with reporter.step(u'Вводим email'):
                email_input = self.driver.find_element(*self.EMAIL_INPUT)
                email_input.clear()
                email_input.send_keys(email)

            with reporter.step(u'Вводим капчу'):
                captcha_url = self.driver.find_element(*self.CAPTCHA_IMG).get_attribute('src')
                captcha_key = parse_qs(urlparse(captcha_url).query)['key'][0]
                self.driver.find_element(*self.CAPTCHA_INPUT).send_keys(utils.get_captcha_answer(captcha_key))

            with reporter.step(u'Отправляем ПФ'):
                self.driver.find_element(*self.SEND_BUTTON).click()

            return ClientInterface.SuccessPage(self.driver)

        def pay(self, alpha=True):
            pay_button = self.driver.find_element(*self.BUTTON_PAY)
            pay_button.submit()
            if alpha:
                return ClientInterface.AlfaBankPaymentPage(self.driver)
            else:
                return ClientInterface.TrustAsProcessingPage(self.driver)

        def is_pf_block_present(self):
            print_link = utils.Web.is_element_present(self.driver, self.PRINT_LINK)
            download_link = utils.Web.is_element_present(self.driver, self.DOWNLOAD_LINK)
            send_link = utils.Web.is_element_present(self.driver, self.SEND_LINK)
            return print_link & download_link & send_link

        def is_payment_button_present(self):
            return utils.Web.is_element_present(self.driver, self.BUTTON_PAY)

    class TrustAsProcessingPage(DefaultPage):
        NEW_CARD_BUTTON = (By.XPATH, "//article[contains(@class, 'YpcNewBankCard')]/following-sibling::label", u'Новая карта')
        CARD_NUMBER = (By.ID, "card_number-input",
                       u'Поле ввода номера карты')
        EXPIRATION_MONTH = (By.ID, "card_month-input",
                            u'Поле ввода месяца')
        EXPIRATION_YEAR = (By.XPATH, "//input[@name='expiration_year']",
                           u'Поле ввода года')
        CVV_INPUT = (By.XPATH, "//input[@name='cvn']", u'Поле ввода CVV')
        CVV_INPUT_NEW_FORM = (By.XPATH, '//input[@id="card_cvv-input"]', u'Поле ввода CVV')
        EMAIL_INPUT = (By.XPATH, "//input[@class='YpcEmailInput-Field']", u"Email")
        PAY_BUTTON = (By.XPATH, '//*[@id="root"]/div/div[2]/div[3]/div/div/button', u'Кнопка "Оплатить"')
        PAY_BUTTON_NEW_FORM = (By.XPATH, "//div[@class='YpcPaymentForm-PaymentButtonContainer']//button", u'Кнопка "Оплатить"')
        OK_BUTTON = (By.XPATH, '//*[@id="root"]/div/div[2]/div/div[3]/button', u'Кнопка "ОК"')

        PAYMENT_SUCCESS_BUTTON = (By.XPATH, '/html/body/div/div/div[2]/div/div[3]/button',
                                  u'Кнопка ОК')
        PAYMENT_SUCCESS_BUTTON_NEW_FORM = (By.XPATH, '//button[@title="Готово"]',
                                  u'Кнопка ОК')
        CARD_REACT_IFRAME_PATH = "//iframe[contains(@class, 'YpcDieHardFrame')]"

        SUCCESS_TEXT = (By.XPATH, '//*[@id="root"]/div/div[2]/div/div[2]')

        def get_payment_status(self):
            payment_status = self.driver.find_element(*self.SUCCESS_TEXT)
            return payment_status.text

        def pay(self, card=Cards.VALID_3DS):
            self.driver.find_element(*self.EMAIL_INPUT).send_keys('usp@email.com')
            # для простоты всегда заново вводим данные карты
            try:
                wait_and_click(self.driver, self.NEW_CARD_BUTTON, 10)
            except Exception as e:
                print(e)
                pass
            #
            # # переходим на фрейм карты
            card_iframe = self.driver.find_element(By.XPATH, self.CARD_REACT_IFRAME_PATH)
            self.driver.switch_to.frame(card_iframe)
            wait_and_get(self.driver, self.CARD_NUMBER).send_keys(card.number)
            self.driver.find_element(*self.EXPIRATION_MONTH).send_keys(card.month)
            self.driver.find_element(*self.EXPIRATION_YEAR).send_keys(card.year)
            self.driver.find_element(*self.CVV_INPUT).send_keys(card.cvv)
            wait_and_get(self.driver, self.CVV_INPUT_NEW_FORM).send_keys(card.cvv)
            self.driver.switch_to.default_content()
            wait_and_get(self.driver, self.PAY_BUTTON_NEW_FORM).click()


            # по-честному нужно жать ОК, но мы не будем
            time.sleep(15)
            # жмем ОК
            # костыль: в тимсити нет дырок до траста, страница таймаутит и тест падает
            # wait_and_get(self.driver, self.OK_BUTTON).click()

            # utils.wait_until(lambda: self.driver.title, equal_to(u'Яндекс.Баланс: Счет'), sleep_time=1)
            # return ClientInterface.InvoicePage(self.driver)


    class AlfaBankPaymentPage(DefaultPage):
        DESCRIPTION = (By.ID, 'description', u'Описание платежа')

        CARD_NUMBER_INPUT = (By.ID, 'pan_visible', u'Поле ввода номера карты')
        MONTH_SELECT = (By.ID, 'month', u'Поле выбора месяца действия карты')
        YEAR_SELECT = (By.ID, 'year', u'Поле выбора года действия карты')
        CARD_HOLDER_INPUT = (By.ID, 'iTEXT', u'Поле ввода владельца карты')
        CVV_INPUT = (By.ID, 'iCVC', u'Поле ввода CVV')
        PAY_BUTTON = (By.ID, 'buttonPayment', u'Кнопка "Оплатить"')

        # a-vasin: страница притворяется загруженной, хотя поле описания платежа еще не подгрузилось
        def get_payment_description(self):
            def get_description():
                try:
                    description_element = self.driver.find_element(*self.DESCRIPTION)
                    return description_element.text
                except NoSuchElementException:
                    return None

            return utils.wait_until(get_description, is_not(empty()), timeout=20, sleep_time=1)

        def pay(self, card=Cards.VALID_3DS):
            def fill_card_number():
                card_number_input = self.driver.find_element(*self.CARD_NUMBER_INPUT)
                card_number_input.click()
                card_number_input.send_keys(card.number)
                return card_number_input.get_attribute('value')

            utils.wait_until(fill_card_number, contains_string(card.number), timeout=20, sleep_time=1)

            self.driver.execute_script("jQuery('#month').removeAttr('style');")
            month_select = Select(self.driver.find_element(*self.MONTH_SELECT))
            month_select.select_by_value(str(card.month))

            self.driver.execute_script("jQuery('#year').removeAttr('style');")
            year_select = Select(self.driver.find_element(*self.YEAR_SELECT))
            year_select.select_by_value(str(card.year))

            card_holder_input = self.driver.find_element(*self.CARD_HOLDER_INPUT)
            card_holder_input.send_keys(card.card_holder)

            cvv_input = self.driver.find_element(*self.CVV_INPUT)
            cvv_input.send_keys(card.cvv)

            pay_button = self.driver.find_element(*self.PAY_BUTTON)
            pay_button.click()

            utils.wait_until(lambda: self.driver.title,
                             any_of(equal_to(u'Payment confirmation'), equal_to(u'Подтверждение оплаты')),
                             sleep_time=1, timeout=30)

            AlfaBankPayForm.fill_pay_password(self.driver, card.password)
            submit_button = self.driver.find_element(*AlfaBankPayForm.SUBMIT_BUTTON_LOCATOR)
            submit_button.submit()

            utils.wait_until(lambda: self.driver.title, equal_to(u'Яндекс.Баланс: Счет'), sleep_time=1)

            return ClientInterface.InvoicePage(self.driver)

    class InvoicePublishPage(DefaultPage):
        DESCRIPTION = u"Страница печатной формы платежа"

        TITLE = (By.XPATH, "//p[@class='c23']/span[@class='c5']/b", u'Заголовок печатной формы')
        PRODUCT = (By.XPATH, "//p[@class='c37']/span[@class='c5']", u'Наименование продукта')
        TEXT_LAYER = (By.CLASS_NAME, 'textLayer', u'Текстовый блок с данными')
        PF_BODY = (By.XPATH, "//*[@id='pageContainer1']/div[2]/div[1]", u'Тело ПФ')

        @classmethod
        def url(cls, invoice_id):
            return '{base_url}/invoice-publish.xml?ft=html&object_id={invoice_id}'.format(
                base_url=envs.balance_env().balance_ci, invoice_id=invoice_id)

        @classmethod
        def open_and_wait(cls, driver, invoice_id):
            page = cls.open(driver, invoice_id)
            utils.wait_until(lambda: utils.Web.is_element_present(driver, cls.PF_BODY), equal_to(True),
                             descr=u'Ожидаем загрузки данных печатной формы')
            return page

        def get_title(self):
            page_title = self.driver.find_element(*self.TITLE)
            return page_title.text

        def get_product(self):
            page_title = self.driver.find_element(*self.PRODUCT)
            return page_title.text

    class InvoicePage(DefaultPage):
        PAGE_TITLE = (By.XPATH, '//h1', u'Заголовок страницы')
        # a-vasin: уродливый xpath, но по-другому никак =(
        # PAYMENT_STATUS = (By.XPATH, '/html/body/table[2]/tbody/tr/td/div[3]/span', u'Статус оплаты картой')
        PAYMENT_STATUS = (By.XPATH, u"//div[@class='sub']/span[contains(text(),'Ваш платеж успешно завершен!')]")

        DATA_LOADING_IMAGE = (By.XPATH, "//img[@alt='Waiting for data']")
        OFERTA_TEXTAREA = (By.XPATH, '//textarea[1]')
        PF_BLOCK = (By.XPATH, '//h3[text()="Печатная форма"]', u'Блок ПФ')
        PAYMENT_BUTTON = (By.XPATH, '//input[@type="submit" and @value="Оплатить"]', u'Кнопка "Оплатить"')
        DEBT_NOTIFY = (By.CLASS_NAME, 'b-alert-message__title', u'Заголовок напоминания о задолженности"')


        @classmethod
        def url(cls, invoice_id, payment_id=None):
            url = '{base_url}/invoice.xml?invoice_id={invoice_id}'.format(base_url=envs.balance_env().balance_ci,
                                                                           invoice_id=invoice_id)
            if payment_id:
                url += '&payment_id=' + payment_id
            return url

        @classmethod
        def open(cls, driver, invoice_id, payment_id=None):
            return cls.open_url(driver=driver, url=cls.url(invoice_id=invoice_id, payment_id=payment_id))

        @classmethod
        def open_url(cls, driver, url):
            driver.get(url, name=u"Страница счета")
            page_object = cls(driver=driver)
            page_object.wait_for_data()
            return page_object

        def get_title(self):
            page_title = self.driver.find_element(*self.PAGE_TITLE)
            return page_title.text

        def get_payment_status(self):
            payment_status = self.driver.find_element(*self.PAYMENT_STATUS)
            return payment_status.text

        def wait_for_data(self):
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.DATA_LOADING_IMAGE),
                             equal_to(False), descr=u'Waiting for data on page')

        def is_pf_block_present(self):
            return utils.Web.is_element_present(self.driver, self.PF_BLOCK)

        def is_payment_button_present(self):
            return utils.Web.is_element_present(self.driver, self.PAYMENT_BUTTON)

        def is_debt_notify_present(self):
            return utils.Web.is_element_present(self.driver, self.DEBT_NOTIFY)

    class BalanceQAWeb(DefaultPage):
        DESCRIPTION = u"Balance QA Web"

        LOGIN = (By.XPATH, '/html/body/div/div/div[1]/div', u'Логин')

        CREATE_CLIENT_BUTTON = (By.XPATH, '/html/body/div/div/div[2]/div[1]/div/div[1]/section[1]/div[3]/div[1]/button',
                                u'Кнопка создания клиента')
        CLIENT_ID = (By.XPATH, '/html/body/div/div/div[2]/div[1]/div/div[1]/section[1]/div[4]/div', u'id клиента')

        @classmethod
        def url(cls):
            # хардкодим tm, т.к. на других средах нет асессорского пакета
            return "https://user-balance.greed-tm.paysys.yandex.ru/static/qa/index.html".\
                format(base_url=envs.balance_env().balance_ci)

        @classmethod
        def open_and_wait(cls, driver, client_id):
            page = cls.open(driver, client_id)
            utils.wait_until(lambda: utils.Web.is_element_present(driver, cls.LOGIN), equal_to(True),
                             descr=u'Ожидаем загрузки страницы')
            return page

        def is_create_client_button_present(self):
            return utils.Web.is_element_present(self.driver, self.CREATE_CLIENT_BUTTON)

        def create_client(self):
            time.sleep(1)
            create_client_button = self.driver.find_element(*self.CREATE_CLIENT_BUTTON)
            create_client_button.click()
            time.sleep(3)
            client_id_text = self.driver.find_element(*self.CLIENT_ID).text
            return client_id_text


class SnoutInterface(object):
    class MainPage(DefaultPage):
        DESCRIPTION = u"Страница Snout"

        API_TITLE = (By.XPATH, "//div[contains(@class, 'info_title')]", u"API")
        CLIENT_RESOURCE = (By.XPATH, '//li[@id="resource_client"]', u"Блок для клиента'")
        CLIENT_REF = (By.XPATH, '//a[@data-id="client"]', u"Ссылка для клиента'")
        CLIENT_GET = (By.XPATH, '//*[@id="client_get_client_person"]/div[1]/h3/span[1]/a', u"GET для клиента'")

        EDO_RESOURCE = (By.XPATH, '//li[@id="resource_edo"]', u"Блок для edo'")
        INVOICE_RESOURCE = (By.XPATH, '//li[@id="resource_invoice"]', u"Блок для счета'")
        ORDER_RESOURCE = (By.XPATH, '//li[@id="resource_order"]', u"Блок для заказа'")
        DEBUG_RESOURCE = (By.XPATH, '//li[@id="resource_debug"]', u"Блок для debug'")

        @classmethod
        def url(cls):
            return '{base_url}/'.format(base_url=envs.snout_env().snout_url)

        @classmethod
        def open(cls, driver):
            return cls.open_url(driver=driver, url=cls.url())

        @classmethod
        def open_url(cls, driver, url):
            driver.get(url, name=u"Страница Snout")
            page_object = cls(driver=driver)
            page_object.wait_for_data()
            return page_object

        def wait_for_data(self):
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.API_TITLE), equal_to(True))
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.CLIENT_RESOURCE), equal_to(True))
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.EDO_RESOURCE), equal_to(True))
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.INVOICE_RESOURCE), equal_to(True))
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.ORDER_RESOURCE), equal_to(True))
            utils.wait_until(lambda: utils.Web.is_element_present(self.driver, self.DEBUG_RESOURCE), equal_to(True))


class CheckInterface(object):
    class CheckPage(DefaultPage):
        DESCRIPTION = u"ПФ чека"

        TEXT_LAYER = (By.CLASS_NAME, 'textLayer', u'Текстовый блок с данными')
        BASE_CHECK_RENDER_URL = 'https://greed-ts.paysys.yandex.net:8019'

        @classmethod
        def url(cls, fd, fn, fpd, url_type):
            url = cls.BASE_CHECK_RENDER_URL + ('' if url_type == 'html' else ('/' + url_type))
            url += '/?n={}&fn={}&fpd={}'.format(fd, fn, fpd)
            return url

        @classmethod
        def open_and_wait(cls, driver, check_id, check_sn, check_fp, url_type='html'):
            page = cls.open(driver, check_id, check_sn, check_fp, url_type)
            utils.wait_until(lambda: utils.Web.is_element_present(driver, cls.TEXT_LAYER), equal_to(True),
                             descr=u'Ожидаем загрузки данных печатной формы')
            return page


def check_errors_absence_on_page(driver):
    with reporter.step(u'Проверяем отсутствие ошибки баланса на странице'):
        _check_http_error_absence(driver)
        _check_balance_error_absence(driver)


def _check_http_error_absence(driver):
    if utils.Web.is_element_present(driver, utils.Web.HttpError.HTTP_ERROR_BLOCK):
        utils.Web.HttpError.raise_error(u'На странице отображается http-ошибка', driver.title)


def _check_balance_error_absence(driver):
    balance_errors = driver.find_elements(*utils.Web.BalanceError.ERROR_BLOCK)
    if balance_errors and balance_errors[0].is_displayed():
        is_timeout = utils.Web.BalanceError.is_timeout(balance_errors[0].text)
        error_code = balance_errors[0].get_attribute('name')
        error_messages = driver.find_elements(*utils.Web.BalanceError.ERROR_MSG_BLOCK)
        error_msg = error_messages[0].get_attribute('value') if error_messages else None
        tracebacks = driver.find_elements(*utils.Web.BalanceError.TRACEBACK_BLOCK)
        traceback = tracebacks[0].text if tracebacks else None
        utils.Web.BalanceError.raise_error(u'На странице отображается ошибка баланса',
                                           is_timeout, error_code, error_msg, traceback)


class OtherInterface(object):
    class PassportPage(DefaultPage):

        @classmethod
        def url(cls):
            return 'https://passport-test.yandex.ru/registration?mode=register&retpath=https%3A%2F%2Fpassport-test.yandex.ru%2Fpassport%3Fmode%3Dpassport&origin=passport_auth2reg'

        @classmethod
        def open_url(cls, driver, url):
            driver.get(url, name=u"Страница паспорт")
            page_object = cls(driver=driver)
            import time
            time.sleep(10)
            return page_object

        def fill_reg_form(self, login, password):
            # from selenium.webdriver.common.by import By
            # path = 'https://passport-test.yandex.ru/registration?mode=register&retpath=https%3A%2F%2Fpassport-test.yandex.ru%2Fpassport%3Fmode%3Dpassport&origin=passport_auth2reg'
            # balance_path = 'https://user-balance.greed-ts.paysys.yandex.ru/'
            # driver.get(path)
            self.driver.find_element(By.XPATH, "//input[@id='firstname']").send_keys('Pupkin')
            self.driver.find_element(By.XPATH, "//input[@id='lastname']").send_keys('Vasily')
            self.driver.find_element(By.XPATH, "//input[@id='login']").send_keys(login)
            self.driver.find_element(By.XPATH, "//input[@id='password']").send_keys(password)
            self.driver.find_element(By.XPATH, "//input[@id='password_confirm']").send_keys(password)
            self.driver.find_element(By.XPATH,
                                "//span[@class='toggle-link link_has-no-phone']").click()
            time.sleep(1)
            self.driver.find_element(By.XPATH, "//input[@id='hint_answer']").send_keys('Pupkin')
            # driver.find_element(By.XPATH, "//span[@id='hint_question_id']").click()
            # тут надо ввести капчу
            reporter.log('login %s registered' % login)
            # driver.get(balance_path)
            reporter.log('login %s registered at balance\n' % login)


if __name__ == "__main__":
    with Driver() as driver_:
        page = AdminInterface.ClientEditPage.open(driver_, 11111)
        INTERCOMPANY_EUROPE_AG = 'CH10'
        page.set_intercompany(INTERCOMPANY_EUROPE_AG)
        page.save_client()
