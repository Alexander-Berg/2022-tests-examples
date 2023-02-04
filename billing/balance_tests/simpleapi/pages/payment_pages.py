# coding: utf-8

import abc
import time
from contextlib import contextmanager

from selenium.common.exceptions import NoSuchElementException, ElementNotVisibleException, WebDriverException
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support import expected_conditions
from selenium.webdriver.support.wait import WebDriverWait

from btestlib import reporter
from btestlib.utils import empty_context_manager
from simpleapi.common.utils import WebDriverProvider, wait_for_ui
from simpleapi.data.cards_pool import get_masked_number_format, CVN, RBS
from simpleapi.data.uids_pool import rbs, sberbank

CARD_IFRAME_PATH = "//iframe[contains(@class, 'external-card-form')]"
CARD_REACT_IFRAME_PATH_OLD = "//iframe[contains(@class, 'card-form__frame')]"
CARD_REACT_IFRAME_PATH = "//iframe[contains(@class, 'YpcDieHardFrame')]"
CARD_IFRAME_TICKETS_PATH = "//iframe"
YM_IFRAME_PATH = "//iframe[contains(@class, 'yam_iframe')]"
#IFRAME_3DS_PATH = "//iframe[contains(@name, '3ds_frame')]"  # (@class, 'iframe-3ds')]"
IFRAME_3DS_PATH = "//iframe[contains(@class, 'frame-3ds__frame')]"
IFRAME_3DS_SUBPAGE = "//iframe[contains(@name, 'iframe-3ds')]"


@contextmanager
def inside_iframe(driver, path=CARD_IFRAME_PATH, descr=u'iframe карточной формы через PCIDSS'):
    """
    Контекст-менеджер работы с айфреймом pcidss
    Заходим в айфрейм перед выполнением действия и выходим из него в конце
    """
    wait_for_ui(driver, expected_conditions.presence_of_element_located,
                locator=(By.XPATH, path),
                message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                waiting_time=60)
    card_iframe = driver.find_element(By.XPATH, path, descr)
    driver.switch_to.frame(card_iframe, descr)

    yield

    driver.switch_to.default_content()


class CardForm(object):
    def __init__(self, driver):
        self.driver = driver

    def fill(self, card):
        self.driver.find_element(By.XPATH, "//input[@name='cardholder']").send_keys(card['cardholder'])
        self.driver.find_element(By.XPATH, "//input[@name='cvn']").send_keys(card['cvn'])
        self.driver.find_element(By.XPATH, "//input[@name='expiration_month']").send_keys(card['expiration_month'])
        self.driver.find_element(By.XPATH, "//input[@name='expiration_year']").send_keys(card['expiration_year'][-2:])

        for i, number_chunk in enumerate(BasePaymentPage.split_number(card['card_number'])):
            self.driver.find_element(By.XPATH, "//input[@name='cnumber{}']".format(i)).send_keys(number_chunk)

    def fill_cvn(self, cvn=CVN.base_success):
        self.driver.find_element(By.XPATH, "//input[@name='cvn']").send_keys(cvn)


class CardFormDisk(object):
    def __init__(self, driver):
        self.driver = driver

    def fill(self, card):
        self.driver.find_element(By.XPATH, "//input[@name='cardholder']",
                                 u"Поле 'Владелец карты'").send_keys(card['cardholder'])
        self.driver.find_element(By.XPATH, "//input[@name='cvn']", u"Поле 'CVV/VCC'").send_keys(card['cvn'])
        self.driver.find_element(By.XPATH, "//div[@id='expiration_month']//button",
                                 u"Кнопка выбора месяца действия карты").click()
        self.driver.find_element(By.XPATH, "//div[@class='nb-select__list']//div[@nb-select-value='{}']"
                                 .format(card['expiration_month']), u"Месяц действия карты").click()
        self.driver.find_element(By.XPATH, "//div[@id='expiration_year']//button",
                                 u"Кнопка выбора года действия карты").click()
        self.driver.find_element(By.XPATH, "//div[@class='nb-select__list']//div[@nb-select-value='{}']"
                                 .format(card['expiration_year']), u"Год действия карты").click()

        for i, number_chunk in enumerate(BasePaymentPage.split_number(card['card_number'])):
            self.driver.find_element(By.XPATH, "//span[@id='num-{}']//input".format((i + 1)),
                                     u"Номер карты, компонента {}".format((i + 1))).send_keys(number_chunk)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, "//button[@id='pay_button']", u"Кнопка оплаты").click()


class CardFormPCIDSS(object):
    def __init__(self, driver, iframe_path=CARD_IFRAME_PATH):
        self.driver = driver
        self.iframe_path = iframe_path

    def fill(self, card, need_cardholder=True,
             input_field_xpath=".//div[@class='input']/input[@class='input_field']",
             extra_input_field_xpath=None):
        with inside_iframe(self.driver, path=self.iframe_path):
            if need_cardholder:
                self.driver.find_element(By.XPATH,
                                         "//input[@name='cardholder']",
                                         u"Поле 'Владелец карты'").send_keys(
                    card['cardholder'])
            self.driver.find_element(By.XPATH, "//input[@name='expiration_month']",
                                     u"Месяц действия карты").send_keys(card['expiration_month'])
            self.driver.find_element(By.XPATH, "//input[@name='expiration_year']",
                                     u"Год действия карты").send_keys(card['expiration_year'][2:])

            # Есть два варианта страницы для ввода карты - стандартный и Yandex Pay
            # Разметка на них отличается
            try:
                self.driver.find_element(
                    By.XPATH,
                    input_field_xpath,
                    u"Номер карты {}".format(card['card_number'])
                ).send_keys(card['card_number'])
            except NoSuchElementException as e:
                if extra_input_field_xpath:
                    self.driver.find_element(
                        By.XPATH,
                        extra_input_field_xpath,
                        u"Номер карты {}".format(card['card_number'])
                    ).send_keys(card['card_number'])
                else:
                    raise e

            self.driver.find_element(By.XPATH, "//input[@name='cvn']", u"Поле 'CVV/VCC'").send_keys(card['cvn'])
            # Старый вариант верстки PCI-DSS
            # for i, number_chunk in enumerate(BasePaymentPage.split_number(card['card_number'])):
            # self.driver.find_element(By.XPATH, "//span[@class='card_number']/span[{}]/div/input".format((i + 1)),
            #                           u"Номер карты, компонента {}".format((i + 1))).send_keys(number_chunk)
            # a-vasin: без sleep форма некорректно заполняется у меня локально
            #  time.sleep(0.05)

    def fill_cvn(self, cvn=CVN.base_success):
        with inside_iframe(self.driver, path=self.iframe_path):
            self.driver.find_element(By.XPATH, "//input[@name='cvn']").send_keys(cvn)

    def press_pay_button(self):
        with inside_iframe(self.driver, path=self.iframe_path):
            self.driver.find_element(By.XPATH, u"//b[@class='action-button js-submit-button'] | "
                                               u"//button[contains(., 'Оплатить')]", u"Кнопка оплаты").click()


class CardFormMobilePCIDSS(CardFormPCIDSS):
    def __init__(self, driver, iframe_path=CARD_IFRAME_PATH):
        super(CardFormMobilePCIDSS, self).__init__(driver, iframe_path)
        self.driver = driver
        self.iframe_path = iframe_path

    def fill(self, card, need_cardholder=True):
        with inside_iframe(self.driver, path=self.iframe_path):
            if need_cardholder:
                self.driver.find_element(By.XPATH, "//input[@name='cardholder']",
                                         u"Поле 'Владелец карты'").send_keys(card['cardholder'])
            self.driver.find_element(By.XPATH, "//input[@name='cvn']", u"Поле 'CVV/VCC'").send_keys(card['cvn'])
            self.driver.find_element(By.XPATH, "//input[@name='expiration_month']".format(card['expiration_month']),
                                     u"Месяц действия карты").send_keys(card['expiration_month'])
            self.driver.find_element(By.XPATH, "//input[@name='expiration_year']".format(card['expiration_year']),
                                     u"Год действия карты").send_keys(card['expiration_year'])

            self.driver.find_element(By.XPATH, "//span[@class='card_number_input']/input[@class='input']",
                                     u"Номер карты {}".format(card['card_number'])).send_keys(card['card_number'])


class CardFormReactPCIDSS(CardFormPCIDSS):
    def __init__(self, driver, iframe_path=CARD_REACT_IFRAME_PATH):
        super(CardFormReactPCIDSS, self).__init__(driver, iframe_path)
        self.driver = driver
        self.iframe_path = iframe_path

    def fill(self, card, need_cardholder=True,
             input_field_xpath=".//div[@class='input']/input[@class='input_field']",
             extra_input_field_xpath=None):
        self.fill_email()
        super(CardFormReactPCIDSS, self).fill(card, need_cardholder,
                                              input_field_xpath, extra_input_field_xpath)

    def fill_email(self, email='usp@email.com'):
        try:
            self.driver.find_element(By.XPATH, "//input[@class='YpcEmailInput-Field']", u"Email").send_keys(email)
        except NoSuchElementException:
            pass

    def check_payment_data_on_processing_page(self, data_for_checks):
        pass


class CardFormReactPCIDSSOld(CardFormPCIDSS):
    def __init__(self, driver, iframe_path=CARD_REACT_IFRAME_PATH_OLD):
        super(CardFormReactPCIDSSOld, self).__init__(driver, iframe_path)
        self.driver = driver
        self.iframe_path = iframe_path

    def check_payment_data_on_processing_page(self, data_for_checks):
        pass


class CardFormMobileReactPCIDSS(CardFormMobilePCIDSS):
    def __init__(self, driver, iframe_path=CARD_REACT_IFRAME_PATH):
        super(CardFormMobileReactPCIDSS, self).__init__(driver, iframe_path)
        self.driver = driver
        self.iframe_path = iframe_path


class CardFormTV(object):
    def __init__(self, driver, iframe_path=CARD_REACT_IFRAME_PATH):
        self.driver = driver
        self.iframe_path = iframe_path

    def send_number_by_form_keyboard(self, number, scope_path):
        # вводим цифу из встроенной в страницу экранной клавиатуры (на самом деле просто строки с цифрами)

        locator = (By.XPATH, scope_path + "//div[@data-number='{}']".format(number))
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
                    message=u'Недоступна экранная клавиатура в течение {} секунд', waiting_time=60)

        self.driver.find_element(*locator).click()

    def fill(self, card):
        with inside_iframe(self.driver, path=self.iframe_path):

            for num in card['card_number']:
                self.send_number_by_form_keyboard(num, scope_path="//div[contains(@class, 'wrapper-card')]")
            for num in card['expiration_month'] + card['expiration_year'][-2:]:
                self.send_number_by_form_keyboard(num, scope_path="//div[contains(@class, 'wrapper-date')]")
            # кривонько, но оно так работает. Вводим дату и жмем на клавиатуре вниз
            # тогда возникает ошибка что не ввели cvn и клавиатура для его ввода становится доступной
            self.driver.find_element(By.XPATH, "//div[contains(@class, 'wrapper-date')]").send_keys(Keys.DOWN)
            for num in card['cvn']:
                self.send_number_by_form_keyboard(num, scope_path="//div[contains(@class, 'wrapper-cvc')]")

    def press_pay_button(self):
        with inside_iframe(self.driver, path=self.iframe_path):
            self.driver.find_element(By.XPATH, "//b[@class='footer__button focusable focus']", u"Кнопка оплаты").click()


class CardFormStore(object):
    def __init__(self, driver):
        self.driver = driver

    def fill(self, card):
        self.driver.find_element(By.XPATH, "//input[@name='cardholder']",
                                 u"Поле 'Владелец карты'").send_keys(card['cardholder'])
        self.driver.find_element(By.XPATH, "//input[@name='cvn']", u"Поле 'CVV/VCC'").send_keys(card['cvn'])

        self.driver.find_element(By.XPATH, "//span[@class='select select_size_m select_theme_normal "
                                           "i-bem select_js_inited']/button",
                                 u"Кнопка выбора месяца действия карты").click()
        self.driver.find_element(By.XPATH, "//div[@class='select__list']//span[contains(., '{}')]"
                                 .format(card['expiration_month'], u"Месяц действия карты")).click()

        self.driver.find_element(By.XPATH, "//span[@class='select select_size_m select_theme_normal "
                                           "select_width_100 manotice manotice_type_popup "
                                           "i-bem select_js_inited manotice_js_inited']/button",
                                 u"Кнопка выбора года действия карты").click()
        self.driver.find_element(By.XPATH, "//div[contains(@class, 'select__popup_width_100')]"
                                           "//span[contains(., '{}')]".format(card['expiration_year']),
                                 u"Год действия карты").click()

        for i, number_chunk in enumerate(BasePaymentPage.split_number(card['card_number'])):
            self.driver.find_element(By.XPATH, "//input[@name='number-{}']".format((i + 1)),
                                     u"Номер карты, компонента {}".format((i + 1))).send_keys(number_chunk)

    def fill_cvn(self, cvn=CVN.base_success):
        self.driver.find_element(By.XPATH, "//input[@name='cvn']", u"Поле 'CVV/VCC'").send_keys(cvn)


class BasePage(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self, driver=None, *args, **kwargs):
        if not driver:
            self.driver = WebDriverProvider.get_instance().get_driver()
        else:
            self.driver = driver
        self.open_page(*args, **kwargs)

    @abc.abstractmethod
    def open_page(self, *args, **kwargs):
        pass


class BaseSubpage(object):
    def __init__(self, driver, base_xpath):
        self.driver = driver
        self.base_xpath = base_xpath

    def fill_mailto(self, mail_to):
        self.driver.find_element(By.XPATH,
                                 self.base_xpath + "//input[@name='mailto']").send_keys(mail_to)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH,
                                 self.base_xpath + u"//b[contains(.,'Оплатить')]", u"Кнопка оплаты").click()


class BasePaymentPage(BasePage):
    def open_page(self, payment_form=None, payment_url=None):
        if payment_url is not None:
            url = payment_url
        elif payment_form is not None:
            payment_form_tmp = payment_form.copy()
            url = payment_form_tmp.pop('_TARGET') + '?'
            for key, value in payment_form_tmp.iteritems():
                url += '%s=%s&' % (key, value)
        else:
            raise WebPaymentPageException(u"You have to specify 'payment_form' or 'payment_url' "
                                          u"to open the payment page.")

        reporter.log(u'Open payment page: {}'.format(url))
        self.driver.get(url)
        self.wait_until_spinner_hide()
        # self.wait_for_card_form_present()

    def wait_for_card_form_present(self, waiting_time=60):
        locator = (By.XPATH, "//form[contains(@class, 'js_card-form')]")
        with inside_iframe(self.driver):
            wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
                        message=u'Не появилась карточная форма в течение {} секунд', waiting_time=waiting_time)

    def wait_until_spinner_hide(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-container')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)

    def wait_until_spinner_show(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-container')]")
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
                    message=u'На странице не появился спиннер в течение {} секунд', waiting_time=waiting_time)

    # TODO: REMOVE THIS SHIT WHEN TICKETS FIX THEIR VERSTKA :D
    def wait_until_promocode_spinner_do(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'promocode-new_loading')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер промокода в течение {} секунд', waiting_time=waiting_time)

    def wait_until_card_form_spinner_done(self, waiting_time=60):
        locator = (By.XPATH, "//span[@data-blocktype='Spinner']")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер карточной формы в течение {} секунд', waiting_time=waiting_time)

    def wait_for_redirect(self, waiting_time=2 * 60):
        WebDriverWait(self.driver, waiting_time) \
            .until(expected_conditions
                   .title_contains(u'Яндекс'),
                   message=u'Не произошел редирект на страницу yandex.ru в течение {} '
                           u'секунд после оплаты на странице trust'.format(waiting_time))

    def wait_for_success_payment_message(self, message, waiting_time=2 * 60,
                                         xpath=u"//div[@class='b-payment-status_text']"):
        WebDriverWait(self.driver, waiting_time, 5) \
            .until(expected_conditions
                   .text_to_be_present_in_element((By.XPATH, xpath), text_=message),
                   message=u'Не появился текст об успешном платеже на странице оплаты в '
                           u'течение {} секунд'.format(waiting_time))

    def wait_until_3ds_redirect(self, waiting_time=60, in_iframe=False):
        with inside_iframe(self.driver, path=IFRAME_3DS_PATH, descr=u'3DS iframe') \
                if in_iframe \
                else empty_context_manager():
            wait_for_ui(self.driver, expected_conditions.presence_of_element_located,
                        locator=(By.XPATH, u"//div[@class='panel-heading']"),
                        message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                        waiting_time=waiting_time)

    def fill_3ds_page(self, cvn, in_iframe=False):
        with inside_iframe(self.driver, path=IFRAME_3DS_PATH, descr=u'3DS iframe') \
                if in_iframe \
                else empty_context_manager():
            self.driver.find_element(By.ID, "cvc", u"Кнопка ввода cvc на странице 3ds").send_keys(cvn)
            self.driver.find_element(By.XPATH, u".//button[contains(.,'Confirm')]",
                                     u"Кнопка подтверждения на странице 3ds").click()

    def select_not_save_card(self):
        try:
            self.driver.find_element(By.XPATH, u".//span[contains(., 'Сохранить карту для быстрой оплаты в "
                                               u"дальнейшем')]",
                                     u"Чекбокс привязки карты после оплаты").click()
        except (ElementNotVisibleException, NoSuchElementException):
            reporter.logger().debug(u'На странице отсутствует(либо невидим) чекбокс сохранения карты')

    class PhoneSubpage(BaseSubpage):
        def __init__(self, driver):
            super(BasePaymentPage.PhoneSubpage,
                  self).__init__(driver, "//li[contains(@class, 'js_payment_item_sms')]")

        def fill_phone_number(self, number):
            self.driver.find_element(By.XPATH, "//span[@class='phone_number-code']/span/input").send_keys(number[2:5])
            self.driver.find_element(By.XPATH, "//span[@class='phone_number-number']/div/input").send_keys(number[5:])

    class YM3dsSubpage(BaseSubpage):
        def __init__(self, driver):
            super(BasePaymentPage.YM3dsSubpage,
                  self).__init__(driver, "//li[contains(@class, 'paymentypes_item')]")

        def fill_3ds_page(self, cvn, in_iframe=False):
            with inside_iframe(self.driver, path=IFRAME_3DS_SUBPAGE, descr=u'3DS iframe'):
                self.driver.find_element(By.XPATH, ".//input[@class='form-control']").send_keys(cvn)
                self.driver.find_element(By.XPATH, u".//button[contains(.,'Confirm')]", u"Кнопка оплаты").click()

        def wait_until_3ds_redirect(self, waiting_time=60, in_iframe=False):
            with inside_iframe(self.driver, path=IFRAME_3DS_SUBPAGE, descr=u'3DS iframe'):
                wait_for_ui(self.driver, expected_conditions.presence_of_element_located,
                            locator=(By.XPATH, u"//div[@class='panel-heading']"),
                            message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                            waiting_time=waiting_time)

    class RBS3dsSubpage(BaseSubpage):
        def __init__(self, driver):
            super(BasePaymentPage.RBS3dsSubpage,
                  self).__init__(driver, "//li[contains(@class, 'paymentypes_item')]")

        def fill_3ds_page(self, cvn, in_iframe=False):
            with inside_iframe(self.driver, path=IFRAME_3DS_SUBPAGE, descr=u'3DS iframe'):
                self.driver.find_element(By.XPATH, ".//input[@name='password']").send_keys(RBS.acs_password)
                self.driver.find_element(By.XPATH, u".//input[@value='Submit']", u"Кнопка оплаты").click()

        def wait_until_3ds_redirect(self, waiting_time=60, in_iframe=False):
            with inside_iframe(self.driver, path=IFRAME_3DS_SUBPAGE, descr=u'3DS iframe'):
                wait_for_ui(self.driver, expected_conditions.visibility_of_element_located,
                            locator=(By.XPATH, ".//input[@name='password']"),
                            message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                            waiting_time=waiting_time)

    class YamoneySubpage(BaseSubpage):
        def __init__(self, driver):
            super(BasePaymentPage.YamoneySubpage,
                  self).__init__(driver,
                                 "//iframe[contains(@class, 'yam_iframe')]")  # "//li[contains(@class, 'js_payment_item_yam')]")

        def send_sms_code(self):
            with inside_iframe(self.driver, path=self.base_xpath, descr=u'Iframe Яндекс.Денег'):
                self.driver.find_element(By.XPATH, "//input[@class='b-form-button__input']").click()

        def fill_sms_code(self, code):
            with inside_iframe(self.driver, path=self.base_xpath, descr=u'Iframe Яндекс.Денег'):
                self.driver.find_element(By.XPATH, "//input[@class='b-form-input__input']").send_keys(code)

        def press_pay_button(self):
            with inside_iframe(self.driver, path=self.base_xpath, descr=u'Iframe Яндекс.Денег'):
                self.driver.find_element(By.XPATH, u"//span[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

        def submit_sms_code(self):
            with inside_iframe(self.driver, path=self.base_xpath, descr=u'Iframe Яндекс.Денег'):
                self.driver.find_element(By.XPATH, u"//input[@type='submit']",
                                         u"Кнопка подтверждения кода из смс").click()

        def wait_for_toner_absence(self):
            WebDriverWait(self.driver, 10) \
                .until(expected_conditions.invisibility_of_element_located((By.XPATH, "//span[@class='toner']")))

    def select_phone_subpage(self):
        self.driver.find_element(By.XPATH, "//span[@class='select_text js-select_button']").click()
        self.driver.find_element(By.XPATH, u"//span[@data-value='с мобильного телефона']",
                                 u"Вкладка оплаты с мобильного телефона").click()
        return self.PhoneSubpage(self.driver)

    def select_3ds_subpage(self, user):
        if user in rbs.values() + sberbank.values():
            return self.RBS3dsSubpage(self.driver)
        else:
            return self.YM3dsSubpage(self.driver)

    def select_yamoney_subpage(self):
        self.driver.find_element(By.XPATH, "//span[@class='select_text js-select_button']").click()
        self.driver.find_element(By.XPATH, u"//span[@data-value='Яндекс.Деньгами']",
                                 u"Вкладка оплаты Яндекс.Деньгами").click()

        return self.YamoneySubpage(self.driver)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//b[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    @staticmethod
    def split_number(number):
        splited = []
        for j in range(0, len(number), 4):
            if j == 12:
                splited.append(number[j:])
                return splited
            else:
                splited.append(number[j:j + 4])
                # return [number[j:j + 4] for j in range(0, len(number), 4)]


class TicketsPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(TicketsPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormPCIDSS(self.driver, iframe_path=CARD_IFRAME_TICKETS_PATH)  # TRUST-1552

    def fill_mailto(self, mail_to):
        self.driver.find_element(By.XPATH, ".//input[@name='mailto']").send_keys(mail_to)

    def select_save_card(self, checked=True):
        try:
            self.driver.find_element(By.XPATH, ".//span[contains(@class, 'checkbox')]",
                                     u'Чекбокс сохранения карты').click()
        except (NoSuchElementException, ElementNotVisibleException):
            if checked:
                raise WebPaymentPageException(u'На странице оплаты отсутствует чекбокс сохранения карты')
            else:
                reporter.logger().debug(u'На странице оплаты отсутствует чекбокс сохранения карты')

    def select_card(self, card=None, card_id=None, checked=False):
        try:
            if card_id is not None:
                self.driver.find_element(By.XPATH, "//li[@data-card-id='{}']".format(card_id)).click()
            elif card is not None:
                self.driver.find_element(By.XPATH,
                                         u"//span[contains(., '{}')]".format(
                                             get_masked_number_format(card['card_number']))).click()
            else:
                # self.driver.find_element(By.XPATH,
                #                          u"//div[contains(@class, '__compact') and "
                #                          u"contains(., 'Новая карта')]//div[@class='_inner']/div").click()
                self.driver.find_element(By.XPATH,
                                         u"//li[contains(@class, 'bound-cards_item bound-cards_item__new')]").click()

        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('Fail while trying to select linked or new card on the payment page')

    def press_unbind_card(self, card_id=None, card=None):
        try:
            if card_id is not None:
                self.driver.find_element(By.XPATH, "//li[@data-card-id='{}']"
                                                   "/span[@class='bound-cards_delete']".format(card_id),
                                         u'Кнопка отвязки карты').click()
            else:
                self.driver.find_element(By.XPATH, u"//span[contains(., '{}')]"
                                                   u"/following-sibling::*"
                                         .format(get_masked_number_format(card['card_number'])),
                                         u'Кнопка отвязки карты').click()

        except NoSuchElementException:
            reporter.logger().debug('User has no linked cards on web page. No unbind button')

    def submit_unbind_card(self):
        self.driver.find_element(By.XPATH, u"//b[contains(., 'Удалить')]",
                                 u'Кнопка подтверждения отвязки карты').click()

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u"//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    # TODO: REMOVE THIS SHIT WHEN TICKETS FIX THEIR VERSTKA :D
    def press_promocode_pay_button(self):
        # class ="action-button js-submit-button action-button_promocode" > Оплатить промокодом < / button >
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить промокодом')]",
                                 u"Кнопка оплаты промокодом").click()

    def wait_until_spinner_show(self, waiting_time=60):
        locator = (By.XPATH, "//span[contains(@data-blocktype, 'Spinner')]")
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
                    message=u'На странице не появился спиннер в течение {} секунд', waiting_time=waiting_time)

    # def wait_until_3ds_subpage_show(self, waiting_time=60):
    #     locator = (By.XPATH, IFRAME_3DS_SUBPAGE)
    #     wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
    #                 message=u'Не открылась страница 3ds в течение {} секунд', waiting_time=waiting_time)


class MarketPlacePaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(MarketPlacePaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)

    def select_card(self, card, checked=False):
        try:
            self.driver.find_element(By.XPATH,
                                     u".//div[@class='bound-card__card-number' and contains(.,'{}')]".format(
                                         get_masked_number_format(card['card_number'])),
                                     u'Чекбокс выбора привязанной карты').click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

    def select_save_card(self):
        self.driver.find_element(By.XPATH, u".//span[@class='checkbox checkbox_checked']",
                                 u'Чекбокс сохранения карты').click()

    def select_new_card(self):
        self.driver.find_element(By.XPATH, u".//div[@class='bound-cards']/div[contains(., 'Новая карта')]",
                                 u'Чекбокс выбора новой карты').click()

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()
        # self.driver.find_element(By.XPATH, ".//div[@class='payment__action-button']", u"Кнопка оплаты").click()

    def select_yamoney_subpage(self):
        self.driver.find_element(By.XPATH, u".//ul//li[contains(.,'Яндекс.Деньгами')]", u"Кнопка ЯДа").click()
        return self.YamoneySubpage(self.driver)


class BlueMarketPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(BlueMarketPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)

    def wait_until_spinner_hide(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-overlay')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)

    def select_card(self, card, checked=False):
        try:
            self.driver.find_element(By.XPATH,
                                     u".//label[contains(.,'{}')]".format(
                                         get_masked_number_format(card['card_number'])),
                                     u'Чекбокс выбора привязанной карты').click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

    def select_new_card(self):
        try:
            self.driver.find_element(By.XPATH, u".//label[contains(.,'Новая карта')]",
                                     u'Чекбокс выбора новой карты').click()
        except NoSuchElementException:
            reporter.logger().info(u'Это первая карта пользователя')

    def select_save_card(self):
        self.driver.find_element(By.XPATH, u".//span[@class='service-checkbox__container']",
                                 u'Чекбокс сохранения карты').click()

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()


class MarketPlaceMobilePaymentPage(MarketPlacePaymentPage):
    def __init__(self, *args, **kwargs):
        super(MarketPlaceMobilePaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormMobilePCIDSS(self.driver)


class RealtyPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(RealtyPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormPCIDSS(self.driver)


class YDFPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(YDFPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormPCIDSS(self.driver)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()


class DefaultPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(DefaultPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormPCIDSS(self.driver,
                                    iframe_path=CARD_REACT_IFRAME_PATH)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH,
                                 u".//button[contains(.,'Оплатить')]",
                                 u"Кнопка оплаты").click()

    def select_card(self, card=None, checked=False):
        try:
            self.driver.find_element(By.XPATH,
                                     u"//div[contains(@class, 'bound-cards__bound-card bound-card')]"
                                     u"/div[contains(.,'{}')]"
                                     .format(get_masked_number_format(
                                         card['card_number']))).click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards '
                                              'on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page.'
                                        ' No need to select specific.')

    def select_new_card(self):
        try:
            self.driver.find_element(By.XPATH, u"//div[@class='bound-cards__"
                                               u"bound-card bound-card "
                                               u"bound-card_new']",
                                     u"Кнопка 'Новая карта'").click()
        except NoSuchElementException:
            self.driver.find_element(By.XPATH, u"//div[@class='bound-cards__bound-card "
                                               u"bound-card bound-card_new "
                                               u"bound-card_active']",
                                     u"Кнопка 'Новая карта'").click()


class MedicinePaymentPage(DefaultPaymentPage):
    def __init__(self, *args, **kwargs):
        super(MedicinePaymentPage, self).__init__(*args, **kwargs)

    def select_new_card(self):
        # Потому что на самом деле его нет
        pass


class TicketsToRidePaymentPage(DefaultPaymentPage):
    def __init__(self, *args, **kwargs):
        super(TicketsToRidePaymentPage, self).__init__(*args, **kwargs)


class TrustInProcessingPaymentPage(DefaultPaymentPage):
    def __init__(self, *args, **kwargs):
        super(TrustInProcessingPaymentPage, self).__init__(*args, **kwargs)


class KinopoiskPlusPaymentPageTV(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(KinopoiskPlusPaymentPageTV, self).__init__(*args, **kwargs)

        locator = (By.XPATH, "//div[@class='spinner']")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=60)

        # todo fellow никакие вейтеры не помогают, разобраться
        time.sleep(1)
        self.driver.maximize_window()

        self.card_form = CardFormTV(self.driver)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    def select_card(self, card=None, checked=False):
        try:
            self.driver.find_element(By.XPATH,
                                     u"//div[contains(@class, 'bound-cards__bound-card bound-card')]"
                                     u"/div[contains(.,'{}')]"
                                     .format(get_masked_number_format(card['card_number']))).click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')


class KinopoiskPlusPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(KinopoiskPlusPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)
        locator = (By.XPATH, "//div[@class='spinner']")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=60)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    def select_card(self, card=None, checked=False):
        try:
            self.driver.find_element(By.XPATH,
                                     u"//div[contains(@class, 'bound-cards__bound-card bound-card')]"
                                     u"/div[contains(.,'{}')]"
                                     .format(get_masked_number_format(card['card_number']))).click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

    def select_new_card(self):
        try:
            self.driver.find_element(By.XPATH, "//div[contains(@class, 'bound-card_new')]",
                                     u"Кнопка 'Новая карта'").click()
        except NoSuchElementException:
            # в новой верстке для анонимного пользователя этого элемента нет
            pass


class BusesPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(BusesPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)

    def wait_until_spinner_hide(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-overlay')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    def select_card(self, card_id, checked=False):
        try:
            self.driver.find_element(By.XPATH, u"//div[@data-card-id='{}']".format(card_id),
                                     u'Кнопка привязанной карты').click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

    def select_new_card(self):
        try:
            self.driver.find_element(By.XPATH, "//div[contains(@class, 'bound-card_new')]",
                                     u"Кнопка 'Новая карта'").click()
        except NoSuchElementException:
            # в новой верстке для анонимного пользователя этого элемента нет
            pass

    def press_unbind_card(self, card_id=None, card=None):
        try:
            self.driver.find_element(By.XPATH, "//div[@data-card-id='{}']"
                                               "/div[@class='bound-card__unbind']".format(card_id),
                                     u'Кнопка отвязки карты').click()
        except NoSuchElementException:
            reporter.logger().debug('User has no linked cards on web page. No unbind button')

    def submit_unbind_card(self):
        self.driver.find_element(By.XPATH, "//div[@class='dialog-inner']//button[@class='button accent']",
                                 u'Кнопка подтверждения отвязки карты').click()


class BusesMobilePaymentPage(BusesPaymentPage):
    def __init__(self, *args, **kwargs):
        super(BusesMobilePaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormMobileReactPCIDSS(self.driver)


class KinopoiskPlusMobilePaymentPage(KinopoiskPlusPaymentPage):
    def __init__(self, *args, **kwargs):
        super(KinopoiskPlusMobilePaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormMobileReactPCIDSS(self.driver)

    def select_new_card(self):
        try:
            self.driver.find_element(By.XPATH, "//div[contains(@class, 'bound-card_new')]",
                                     u"Кнопка 'Новая карта'").click()
        except NoSuchElementException:
            # в новой верстке для анонимного пользователя этого элемента нет
            pass


class TicketsToRideMobilePaymentPage(BusesPaymentPage):
    def __init__(self, *args, **kwargs):
        super(TicketsToRideMobilePaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormPCIDSS(self.driver,
                                        iframe_path=CARD_REACT_IFRAME_PATH)


class ShadPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(ShadPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormPCIDSS(self.driver)


class MusicPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(MusicPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)

    def fill_mailto(self, mail_to):
        self.driver.find_element(By.XPATH, ".//input[@name='email']").send_keys(mail_to)

    def select_yamoney_subpage(self):
        self.driver.find_element(By.XPATH, u".//span[contains(.,'Яндекс.Деньги')]", u"Вкладка оплаты через ЯД").click()
        return self.YamoneySubpage(self.driver)

    def select_card(self, card=None, checked=False):
        if card is not None:
            try:
                self.driver.find_element(By.XPATH,
                                         u"//div[@class='bound-card__card-number' and contains(., '{}')]"
                                         .format(get_masked_number_format(
                                             card['card_number']))).click()
            except NoSuchElementException:
                if checked:
                    raise WebPaymentPageException('User has no linked cards on web page.')
                else:
                    reporter.logger().debug('User has no linked cards on web page. No need to select specific.')
            except WebDriverException:
                pass
        else:
            try:
                self.driver.find_element(By.XPATH, u"//div[@class='bound-cards__"
                                                   u"bound-card bound-card "
                                                   u"bound-card_new']",
                                         u"Кнопка 'Новая карта'").click()
            except NoSuchElementException:
                try:
                    self.driver.find_element(By.XPATH, u"//div[@class='bound-cards__bound-card "
                                                       u"bound-card bound-card_new "
                                                       u"bound-card_active']",
                                             u"Кнопка 'Новая карта'").click()
                except NoSuchElementException:
                    reporter.logger().debug(u'У пользователя еще нет привязаных '
                                            u'карт. Нет кнопки "Новая карта"')

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]",
                                 u"Кнопка оплаты").click()

    def wait_until_spinner_show(self, waiting_time=60):
        locator = (By.XPATH, "//div[@class='spinner__inner-inner']")
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
                    message=u'На странице не появился спиннер в течение {} секунд', waiting_time=waiting_time)


class DiskPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(DiskPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)

    def press_pay_button(self):
        # self.driver.find_element(By.XPATH, "//b[@class='action-button js-submit-button']", u"Кнопка оплаты").click()
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    def select_card(self, card_id=None, checked=False):
        if card_id is not None:
            try:
                self.driver.find_element(By.XPATH, "//div[@data-card-id='{}']".format(card_id)).click()
            except NoSuchElementException:
                if checked:
                    raise WebPaymentPageException('User has no linked cards on web page.')
                else:
                    reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

        else:
            self.driver.find_element(By.XPATH,
                                     "//div[contains(@class, 'bound-card_new')]").click()


class StorePaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(StorePaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormStore(self.driver)

    def select_card(self, card_id, checked=False):
        try:
            self.driver.find_element(By.XPATH, "//input[@id='{}']".format(card_id)).click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//span[contains(.,'Оплатить')]", u"Кнопка оплаты").click()


class CloudPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(CloudPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormPCIDSS(self.driver, iframe_path=CARD_REACT_IFRAME_PATH)

    def wait_until_spinner_hide(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-overlay')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    def select_card(self, card=None, checked=False):
        if card is not None:
            try:
                self.driver.find_element(By.XPATH,
                                         u"//div[contains(@class, 'bound-cards__bound-card bound-card')]"
                                         u"/div[contains(.,'{}')]"
                                         .format(get_masked_number_format(card['card_number']))).click()
            except NoSuchElementException:
                if checked:
                    raise WebPaymentPageException('User has no linked cards on web page.')
                else:
                    reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

        else:
            self.driver.find_element(By.XPATH,
                                     "//div[contains(@class, 'bound-card_new')]").click()


class YandexMoneyWebPage(BasePaymentPage):
    def press_pay_button(self):
        self.driver.find_element(By.XPATH, ".//button[contains(@class,'payment-contract__pay-button')]",
                                 u"Кнопка оплаты").click()

    def wait_for_pay_button_available(self):
        WebDriverWait(self.driver, 10) \
            .until(expected_conditions
                   .visibility_of_element_located((By.XPATH,
                                                   ".//button[contains(@class,'payment-contract__pay-button')]")))

    def fill_sms_code(self, code):
        self.driver.find_element(By.XPATH, ".//input[@name='answer']", u'Поле ввода кода из смс').send_keys(code)


class RedMarketPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(RedMarketPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)

    def wait_until_spinner_hide(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-overlay')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)

    def select_card(self, card, checked=False):
        try:
            self.driver.find_element(By.XPATH,
                                     u".//div[@class='bound-card__card-number' and contains(.,'{}')]".format(
                                         get_masked_number_format(card['card_number'])),
                                     u'Чекбокс выбора привязанной карты').click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

    def select_new_card(self):
        self.driver.find_element(By.XPATH, u".//div[@class='bound-cards']/div[contains(., 'Новая карта')]",
                                 u'Чекбокс выбора новой карты').click()

    def select_save_card(self):
        self.driver.find_element(By.XPATH, u".//span[@class='checkbox checkbox_checked']",
                                 u'Чекбокс сохранения карты').click()

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//div[@class='d-button']", u"Кнопка оплаты").click()


class AfishaMoviePassPaymentPage(BasePaymentPage):
    def __init__(self, *args, **kwargs):
        super(AfishaMoviePassPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormReactPCIDSS(self.driver)

    def wait_until_spinner_hide(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-overlay')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//button[contains(.,'Оплатить')]", u"Кнопка оплаты").click()

    def select_card(self, card_id, checked=False):
        try:
            self.driver.find_element(By.XPATH, u"//div[@data-card-id='{}']".format(card_id),
                                     u'Кнопка привязанной карты').click()
        except NoSuchElementException:
            if checked:
                raise WebPaymentPageException('User has no linked cards on web page.')
            else:
                reporter.logger().debug('User has no linked cards on web page. No need to select specific.')

    def select_new_card(self):
        try:
            self.driver.find_element(By.XPATH, "//div[contains(@class, 'bound-card_new')]",
                                     u"Кнопка 'Новая карта'").click()
        except NoSuchElementException:
            # в новой верстке для анонимного пользователя этого элемента нет
            pass

    def select_not_save_card(self):
        try:
            self.driver.find_element(By.XPATH, u"//span[contains(.,'Сохранить карту для быстрой оплаты в дальнейшем')]",
                                     u"Чекбокс привязки карты после оплаты").click()
        except NoSuchElementException:
            # в новой верстке для анонимного пользователя этого элемента нет
            pass

    def press_unbind_card(self, card_id=None, card=None):
        try:
            self.driver.find_element(By.XPATH, "//div[@data-card-id='{}']"
                                               "/div[@class='bound-card__unbind']".format(card_id),
                                     u'Кнопка отвязки карты').click()
        except NoSuchElementException:
            reporter.logger().debug('User has no linked cards on web page. No unbind button')

    def submit_unbind_card(self):
        self.driver.find_element(By.XPATH, "//div[@class='dialog-inner']//button[@class='button accent']",
                                 u'Кнопка подтверждения отвязки карты').click()


class AfishaMoviePassMobilePaymentPage(BusesPaymentPage):
    def __init__(self, *args, **kwargs):
        super(AfishaMoviePassMobilePaymentPage, self).__init__(*args, **kwargs)
        self.card_form = CardFormMobileReactPCIDSS(self.driver)


class WebPaymentPageException(AssertionError):
    pass
