# coding: utf-8
from decimal import Decimal as D

from hamcrest import equal_to
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions
from selenium.webdriver.support.select import Select

from btestlib import reporter
from btestlib.constants import Processings
from btestlib.web import AlfaBankPayForm
from btestlib.utils import check_that
from simpleapi.common.utils import wait_for_ui
from simpleapi.data.cards_pool import DEFAULT_3DS_PASSWORD
from simpleapi.data.defaults import default_waiting_time
from simpleapi.pages.payment_pages import BasePage, WebPaymentPageException, BasePaymentPage, CardFormReactPCIDSS, \
    BusesPaymentPage

__author__ = 'fellow'


class InvoicePage(BasePage):
    INVOICE_CI = (By.XPATH, u"//span[contains(text(),'Ваш платеж успешно завершен!')]")
    PAYMENT_RESULT = (
        By.XPATH, "//table[@class='content']//span[contains(@style, 'color: green') or contains(@style, 'color: red')]")

    def wait_for_ci_invoice_page(self, waiting_time=default_waiting_time):
        from btestlib import reporter

        reporter.attach(u"Скриншот перед окрытием страницы счета", self.driver.get_screenshot_as_png(),
                        attachment_type=reporter.attachment_type.PNG)

        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, self.INVOICE_CI,
                    message=u'Не отклылась страница счета КИ Баланса в течение {} секунд', waiting_time=waiting_time)

    def open_page(self, payment_url=None):
        if payment_url is not None:
            url = payment_url
        else:
            raise WebPaymentPageException(u"You have to specify 'payment_form' or 'payment_url' "
                                          u"to open the payment page.")

        reporter.log(u'Открываем страницу: {}'.format(url))
        self.driver.get(url)

    def check_payment_status(self, expected_text):
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, self.PAYMENT_RESULT,
                    message=u'Не появился результат оплаты счёта в течение {} секунд',
                    waiting_time=default_waiting_time)

        actual_text = self.driver.find_element(*self.PAYMENT_RESULT).text

        check_that(actual_text, equal_to(expected_text), step=u'Проверяем результат платежа на странице счёта')

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u"//input[@type='submit']").click()


class ProfessionalServicePaymentPage(BasePage):
    def open_page(self, *args, **kwargs):
        pass

    def __init__(self, *args, **kwargs):
        super(ProfessionalServicePaymentPage, self).__init__(*args, **kwargs)
        processing = kwargs.get('processing', Processings.ALPHA)
        self.card_form = PAYSTEP_PROCESSINGS.get(processing)(self.driver)

    def press_pay_button(self):
        self.driver.find_element(By.XPATH, u".//span[contains(.,'Оплатить')]", u"Кнопка оплаты").click()


class WebMoneyPaymentPage(object):
    AMOUNT = (By.XPATH, u"//div[@class='merchant-payment__main-descr']/div[contains(.,'WMR')]")
    DESCRIPTION = (By.XPATH, u"//div[@class='merchant-payment__main-descr']/div[contains(.,'Оплата счета')]")

    AMOUNT_FORMAT = u'{:,.2f} {}'
    AMOUNT_FORMAT_TRANSFORMATION = staticmethod(lambda x: x.replace(',', ' '))
    DESCRIPTION_FORMAT = u'[Yandex.Balance.Test #1184] Оплата счета {}'

    def __init__(self, driver):
        self.driver = driver

    def wait_for_webmoney_form_present(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.presence_of_element_located, self.DESCRIPTION,
                    message=u'Не появилась кнопка выбора платежной системы в течение {} секунд',
                    waiting_time=waiting_time)

    def check_payment_data_on_processing_page(self, data_for_checks):
        actual_description = self.driver.find_element(*self.DESCRIPTION).text
        actual_amount = self.driver.find_element(*self.AMOUNT).text

        expected_amount = self.AMOUNT_FORMAT.format(D(data_for_checks['total_sum']),
                                                    data_for_checks['currency_iso_code'])
        expected_amount = self.AMOUNT_FORMAT_TRANSFORMATION(expected_amount)
        expected_description = self.DESCRIPTION_FORMAT.format(data_for_checks['external_id'])

        check_that(actual_amount, equal_to(expected_amount), step=u'Проверяем сумму платежа')
        check_that(actual_description, equal_to(expected_description), step=u'Проверяем назначение платежа')


class PayPalPaymentPage(object):
    PAYPAL_TEST_ACCOUNT_LOGIN = "yndx-paypal@yandex.ru"
    PAYPAL_TEST_ACCOUNT_PASSWORD = "yandex_paypal"
    LOGIN_DIV = (By.XPATH, "//div[@id='contents']")
    LOGIN_LOCATOR = (By.XPATH, "//input[@id='email']")
    PASSWORD_LOCATOR = (By.XPATH, "//input[@id='password']")
    SUBMIT_LOGIN_BUTTON = (By.XPATH, "//button[@id='btnLogin']")
    CONTINUE_BUTTON = (By.XPATH, "//input[@id ='confirmButtonTop']")
    SPINNER = (By.XPATH, "//div[@class='spinWrap']")
    NEXT_BUTTON = (By.XPATH, "//button[@id='btnNext']")
    START_AUTRIZATION_BUTTON = (By.XPATH, "//a[contains(., 'Log In')]")

    def __init__(self, driver):
        self.driver = driver

    def wait_and_input(self, value, locator, info=u''):
        wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, locator,
                    message=u'Елемент {} в PayPal '.format(info) + u'недоступна для ввода в течение {} секунд',
                    waiting_time=default_waiting_time)

        input_field = self.driver.find_element(*locator)
        input_field.clear()
        input_field.send_keys(value)

    def wait_and_click(self, locator, info=u''):
        wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, locator,
                    message=u'Не появилась {} в PayPal '.format(info) + u'в течение {} секунд',
                    waiting_time=default_waiting_time)

        self.driver.find_element(*locator).click()

    def login(self):
        wait_for_ui(self.driver, expected_conditions.presence_of_element_located, self.LOGIN_DIV,
                    message=u'Не появилась форма залогинивания в PayPal в течение {} секунд',
                    waiting_time=default_waiting_time * 2)
        self.wait_and_click(locator=self.START_AUTRIZATION_BUTTON, info=u'Кнопка начала авторизации')
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, self.SPINNER,
                    message=u'На странице PayPal спиннер при входе в авторизацию в течение {} секунд',
                    waiting_time=default_waiting_time)
        while True:
            self.wait_and_input(value=self.PAYPAL_TEST_ACCOUNT_LOGIN, locator=self.LOGIN_LOCATOR,
                                info=u'поле ввода логина')
            self.wait_and_click(locator=self.NEXT_BUTTON,
                                info=u'Кнопка Next, переход к вводу пароля')
            if (self.driver.find_element(By.XPATH, "//div[@id='emailErrorMessage']").get_attribute(
                    "class")) == 'errorMessage':
                break
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator=self.SPINNER,
                    message=u'На странице PayPal спиннер после ввода e-mail в течение {} секунд',
                    waiting_time=default_waiting_time)
        self.wait_and_input(value=self.PAYPAL_TEST_ACCOUNT_PASSWORD, locator=self.PASSWORD_LOCATOR,
                            info=u'поле ввода пароля')
        self.wait_and_click(locator=self.SUBMIT_LOGIN_BUTTON, info=u'Кнопка авторизации')
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator=self.SPINNER,
                    message=u'На странице PayPal спиннер после авторизации в течение {} секунд',
                    waiting_time=default_waiting_time)
        self.wait_and_click(locator=self.CONTINUE_BUTTON, info=u'Кнопка завершения платежа')


class PriorPaymentPage(object):
    # check_payment_data_on_processing_page
    # AMOUNT = (By.XPATH, "//span[contains(@class,'text-amount')]")
    # CURRENCY = (By.XPATH, "//span[contains(@class,'text-currency')]")
    # DESCRIPTION = (By.XPATH, "//span[contains(@class,'text-offer-description')][contains(.,'Payment for Invoice')]")
    AMOUNT_WITH_CURRENCY = (By.XPATH, "//div[@class='row padding-bottom-10']/div[4]/div[2]/div/div/div")

    AMOUNT_FORMAT = u'{:.2f} {}'

    # 3ds
    SUBMIT_3DS_BUTTON = (By.XPATH, "//input[@name='submit']")
    PASSWORD_LOCATOR_3DS = (By.XPATH, "//input[@name='password']")

    # report page
    RETURN_TO_WEBSITE_BUTTON = (By.XPATH, "//button[@class='cancelButton']")

    #
    CURRENCY_OPTION = (By.XPATH, "//input[@id='ChargeMerchantAmount' and @value='True']/parent::label")
    CURRENCY_SELECT_NEXT_BUTTON = (By.XPATH, "//button[@name='SubmitToNext']")

    class CardForm(object):
        CREDIT_NUMBER_LOCATOR = (By.XPATH, "//input[@id='cc_pan']")
        MONTH = (By.XPATH, "//input[@id='cc_month']")
        YEAR = (By.XPATH, "//input[@id='cc_year']")
        CVV = (By.XPATH, "//input[@id='cc_cvv']")
        CARDHOLDER = (By.XPATH, "//input[@id='cc_name']")
        EMAIL = (By.XPATH, "//input[@id='cc_email']")
        SUBMIT_BUTTON = (By.XPATH, "//button[@id='payBtn']")

        RETURN_TO_BALANCE_BUTTON = (By.XPATH, "//a[contains(., 'Back to site')]")

        def __init__(self, driver):
            self.driver = driver

        def back_to_balance(self):
            wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.RETURN_TO_BALANCE_BUTTON,
                        message=u'Не появилась кнопка возврата в Баланас на странице процессинга в течение {} секунд',
                        waiting_time=default_waiting_time)
            self.driver.find_element(*self.RETURN_TO_BALANCE_BUTTON).click()

        def fill(self, card):
            self.driver.find_element(*self.MONTH).send_keys(card['expiration_month'])
            self.driver.find_element(*self.YEAR).send_keys(card['expiration_year'])

            self.driver.find_element(*self.CVV).send_keys(card['cvn'])
            self.driver.find_element(*self.CARDHOLDER).send_keys(card['cardholder'])

            self.driver.find_element(*self.EMAIL).send_keys('test@mail.test')

        def press_pay_button(self):
            self.driver.find_element(*self.SUBMIT_BUTTON).click()
            self.back_to_balance()

    def __init__(self, driver):
        self.driver = driver
        self.card_form = self.CardForm(self.driver)

    def wait_for_present(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located,
                    self.card_form.CREDIT_NUMBER_LOCATOR,
                    message=u'Не появилась кнопка выбора платежной системы в течение {} секунд',
                    waiting_time=waiting_time)

    def wait_until_3ds_redirect(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, self.SUBMIT_3DS_BUTTON,
                    message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                    waiting_time=waiting_time)

    def fill_3ds_subpage(self, card):
        self.driver.find_element(*self.PASSWORD_LOCATOR_3DS).send_keys(card['3ds_password'])
        self.driver.find_element(*self.SUBMIT_3DS_BUTTON).click()

    def post_3ds_actions(self):
        wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.RETURN_TO_WEBSITE_BUTTON,
                    message=u'Не появилась кнопка "Close" на странице с отчётом в течение {} секунд',
                    waiting_time=default_waiting_time)
        self.driver.find_element(*self.RETURN_TO_WEBSITE_BUTTON).click()

    def check_payment_data(self, data_for_checks):
        actual_amount_with_currency = self.driver.find_element(*self.AMOUNT_WITH_CURRENCY).text

        expected_amount_with_currency = self.AMOUNT_FORMAT.format(float(data_for_checks['total_sum']),
                                                                  data_for_checks['currency_iso_code']) \
            .replace('.', ',')

        check_that(actual_amount_with_currency, equal_to(expected_amount_with_currency),
                   step=u'Проверяем сумму платежа в процессинге',
                   error=u'Сумма и валюта платежа на странице процессинга некорректны')


class BilderlingsPaymentPage(object):
    # check_payment_data_on_processing_page
    # AMOUNT = (By.XPATH, "//span[contains(@class,'text-amount')]")
    # CURRENCY = (By.XPATH, "//span[contains(@class,'text-currency')]")
    # DESCRIPTION = (By.XPATH, "//span[contains(@class,'text-offer-description')][contains(.,'Payment for Invoice')]")
    AMOUNT_WITH_CURRENCY = (
        By.XPATH, u"//div[@class='payBoard orderSummary mb20']/div[contains(@class,'borderD')]/div[2]")

    AMOUNT_FORMAT = u'{} {}'

    YEAR_FORMAT = u'20{}'

    # 3ds
    SUBMIT_3DS_BUTTON = (By.XPATH, "//input[@name='submit']")
    PASSWORD_LOCATOR_3DS = (By.XPATH, "//input[@name='password']")

    # report page
    RETURN_TO_WEBSITE_BUTTON = (By.XPATH, "//button[@class='cancelButton']")

    #
    CURRENCY_OPTION = (By.XPATH, "//input[@id='ChargeMerchantAmount' and @value='True']/parent::label")
    CURRENCY_SELECT_NEXT_BUTTON = (By.XPATH, "//button[@name='SubmitToNext']")

    class CardForm(object):
        CREDIT_NUMBER_LOCATOR = (By.XPATH, "//input[@id='cardNumber']")
        MONTH = (By.XPATH, "//input[contains(@class,'cardMonth')]")
        YEAR = (By.XPATH, "//input[contains(@class,'cardYear')]")
        CVV = (By.XPATH, "//input[@id='cardCVV']")
        CARDHOLDER = (By.XPATH, "//input[@id='cardName']")
        SUBMIT_BUTTON = (By.XPATH, "//button[contains(@class,'payBtn')]")

        def __init__(self, driver):
            self.driver = driver

        def fill(self, card):
            credit_number_locator = self.driver.find_element(*self.CREDIT_NUMBER_LOCATOR)
            credit_number_locator.clear()
            # credit_number_locator.send_keys(card['card_number'])

            for i, number_chunk in enumerate(BasePaymentPage.split_number(card['card_number'])):
                credit_number_locator.send_keys(number_chunk)
                credit_number_locator.send_keys(' ')

            self.driver.find_element(*self.MONTH).send_keys(card['expiration_month'])
            self.driver.find_element(*self.YEAR).send_keys(card['expiration_year'])

            self.driver.find_element(*self.CVV).send_keys(card['cvn'])
            self.driver.find_element(*self.CARDHOLDER).send_keys(card['cardholder'])

        def press_pay_button(self):
            self.driver.find_element(*self.SUBMIT_BUTTON).click()

    def __init__(self, driver):
        self.driver = driver
        self.card_form = self.CardForm(self.driver)

    def wait_for_present(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.card_form.CREDIT_NUMBER_LOCATOR,
                    message=u'Не появилась кнопка выбора платежной системы в течение {} секунд',
                    waiting_time=waiting_time)

    def wait_until_3ds_redirect(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, self.SUBMIT_3DS_BUTTON,
                    message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                    waiting_time=waiting_time)

    def fill_3ds_subpage(self, card):
        self.driver.find_element(*self.PASSWORD_LOCATOR_3DS).send_keys(card['3ds_password'])
        self.driver.find_element(*self.SUBMIT_3DS_BUTTON).click()

    def post_3ds_actions(self):
        wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.RETURN_TO_WEBSITE_BUTTON,
                    message=u'Не появилась кнопка "Close" на странице с отчётом в течение {} секунд',
                    waiting_time=default_waiting_time)
        self.driver.find_element(*self.RETURN_TO_WEBSITE_BUTTON).click()

    def check_payment_data(self, data_for_checks):
        actual_amount_with_currency = self.driver.find_element(*self.AMOUNT_WITH_CURRENCY).text
        # actual_description = self.driver.find_element(*self.DESCRIPTION).text

        expected_amount_and_currency = self.AMOUNT_FORMAT.format(D(data_for_checks['total_sum']),
                                                                 data_for_checks['currency_iso_code'])
        # expected_description = u'Payment for Invoice # {}'.format(data_for_checks['external_id'])

        check_that(actual_amount_with_currency, equal_to(expected_amount_and_currency), step=u'Проверяем сумму платежа')
        # check_that(actual_description, equal_to(expected_description), step=u'Проверяем назначение платежа')


class SAFERPAYPaymentPage(object):
    # check_payment_data_on_processing_page
    AMOUNT = (By.XPATH, "//span[contains(@class,'text-amount')]")
    CURRENCY = (By.XPATH, "//span[contains(@class,'text-currency')]")
    DESCRIPTION = (By.XPATH, "//span[contains(@class,'text-offer-description')][contains(.,'Payment for Invoice')]")

    AMOUNT_FORMAT = '{:,.2f}'

    # 3ds
    SUBMIT_3DS_BUTTON = (By.XPATH, "//input[@id='Submit']")

    # report page
    CLOSE_BUTTON = (By.XPATH, "//button[contains(@class,'btn-next')]")

    #
    CURRENCY_OPTION = (By.XPATH, "//input[@id='ChargeMerchantAmount' and @value='True']/parent::label")
    CURRENCY_SELECT_NEXT_BUTTON = (By.XPATH, "//button[@name='SubmitToNext']")

    class CardForm(object):
        # fill
        VISA_CARD_PAYMENT_BUTTON = (By.XPATH, "//button[contains(@class, 'btn-card-visa')]")
        CREDIT_NUMBER_LOCATOR = (By.XPATH, "//input[@id='CardNumber']")
        MONTH = (By.XPATH, "//select[contains(@id,'ExpMonth')]")
        YEAR = (By.XPATH, "//select[contains(@id,'ExpYear')]")
        CVV = (By.XPATH, "//input[contains(@id,'VerificationCode')]")
        CARDHOLDER = (By.XPATH, "//input[contains(@id,'HolderName')]")
        SUBMIT_BUTTON = (By.XPATH, "//button[@name='SubmitToNext']")

        YEAR_FORMAT = u'20{}'

        def __init__(self, driver):
            self.driver = driver

        def fill(self, card):
            self.driver.find_element(*self.VISA_CARD_PAYMENT_BUTTON).click()

            wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.CREDIT_NUMBER_LOCATOR,
                        message=u'Не появилась кнопка выбора платежной системы в течение {} секунд',
                        waiting_time=default_waiting_time)

            credit_number_locator = self.driver.find_element(*self.CREDIT_NUMBER_LOCATOR)
            credit_number_locator.clear()
            credit_number_locator.send_keys(card['card_number'])

            Select(self.driver.find_element(*self.MONTH)).select_by_visible_text(card['expiration_month'])
            full_year = self.YEAR_FORMAT.format(card['expiration_year'])
            Select(self.driver.find_element(*self.YEAR)).select_by_visible_text(full_year)

            cvn_locator = self.driver.find_element(*self.CVV)
            cvn_locator.clear()
            cvn_locator.send_keys(card['cvn'])

            card_holder_locator = self.driver.find_element(*self.CARDHOLDER)
            card_holder_locator.clear()
            card_holder_locator.send_keys(card['cardholder'])

        def press_pay_button(self):
            self.driver.find_element(*self.SUBMIT_BUTTON).click()

            # todo  непонятно, что мы тут ожидаем, сейчас сразу перекидывает на 3дс и тест падает на этих вейтерах
            # пока отключу, когда/если будет понятна суть проверки, надо дописать говорящие message (без ...)
            # if card in [Saferpay.Valid.card_eur, Saferpay.Valid.card_usd]:
            #     wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.CURRENCY_OPTION,
            #                 message=u'Не появилась ... в течение {} секунд', waiting_time=default_waiting_time)
            #     self.driver.find_element(*self.CURRENCY_OPTION).click()
            #
            #     wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.CURRENCY_SELECT_NEXT_BUTTON,
            #                 message=u'Не появилась ... в течение {} секунд', waiting_time=default_waiting_time)
            #     self.driver.find_element(*self.CURRENCY_SELECT_NEXT_BUTTON).click()

    def __init__(self, driver):
        self.driver = driver
        self.card_form = self.CardForm(self.driver)

    def wait_for_present(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.card_form.VISA_CARD_PAYMENT_BUTTON,
                    message=u'Не появилась кнопка выбора платежной системы в течение {} секунд',
                    waiting_time=waiting_time)

    def wait_until_3ds_redirect(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, self.SUBMIT_3DS_BUTTON,
                    message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                    waiting_time=waiting_time)

    def fill_3ds_subpage(self, card):
        self.driver.find_element(*self.SUBMIT_3DS_BUTTON).click()

    def post_3ds_actions(self):
        wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.CLOSE_BUTTON,
                    message=u'Не появилась кнопка "Close" на странице с отчётом в течение {} секунд',
                    waiting_time=default_waiting_time)
        self.driver.find_element(*self.CLOSE_BUTTON).click()

    def check_payment_data(self, data_for_checks):
        actual_amount = self.driver.find_element(*self.AMOUNT).text
        actual_currency = self.driver.find_element(*self.CURRENCY).text
        actual_description = self.driver.find_element(*self.DESCRIPTION).text

        expected_amount = self.AMOUNT_FORMAT.format(D(data_for_checks['total_sum']))
        expected_currency = data_for_checks['currency_iso_code']
        expected_description = u'Payment for Invoice # {}'.format(data_for_checks['external_id'])

        check_that(actual_amount, equal_to(expected_amount), step=u'Проверяем сумму платежа')
        check_that(actual_currency, equal_to(expected_currency), step=u'Проверяем валюту платежа')
        check_that(actual_description, equal_to(expected_description), step=u'Проверяем назначение платежа')


class INGPaymentPage(object):
    # check_payment_data_on_processing_page
    AMOUNT = (By.XPATH, "//div[@class='label'][contains(.,'Tutar')]/following-sibling::div")

    AMOUNT_FORMAT = u'{} TL'

    # 3ds
    # VISA_3DS_PASSWORD_LOCATOR = (By.XPATH, ".//input[@class='form-control']")
    # MASTERCARD_3DS_PASSWORD_LOCATOR = (By.XPATH, ".//input[@type='password']")
    # SUBMIT_3DS_BUTTON = (By.XPATH, ".//input[@type='submit']")

    PASSWORD_LOCATOR_3DS = (By.XPATH, "//input[@name='password']")
    SUBMIT_BUTTON_3DS = (By.XPATH, "//input[@id='submitbutton']")

    class CardForm(object):
        CREDIT_NUMBER_LOCATOR = (By.XPATH, "//input[@name='pan']")
        MONTH = (By.XPATH, "//select[@name='Ecom_Payment_Card_ExpDate_Month']")
        YEAR = (By.XPATH, "//select[@name='Ecom_Payment_Card_ExpDate_Year']")
        CVV = (By.XPATH, "//input[@name='cv2']")
        SUBMIT_BUTTON = (By.XPATH, "//input[@name='gonder']")

        YEAR_FORMAT = u'20{}'

        def __init__(self, driver):
            self.driver = driver

        def fill(self, card):
            self.driver.find_element(*self.CREDIT_NUMBER_LOCATOR).send_keys(card['card_number'])

            Select(self.driver.find_element(*self.MONTH)).select_by_visible_text(card['expiration_month'])
            full_year = self.YEAR_FORMAT.format(card['expiration_year'])
            Select(self.driver.find_element(*self.YEAR)).select_by_visible_text(full_year)

            self.driver.find_element(*self.CVV).send_keys(card['cvn'])

        def press_pay_button(self):
            self.driver.find_element(*self.SUBMIT_BUTTON).click()

    def __init__(self, driver):
        self.driver = driver
        self.card_form = self.CardForm(self.driver)

    def wait_until_3ds_redirect(self, waiting_time=default_waiting_time):
        for locator in [self.PASSWORD_LOCATOR_3DS, self.SUBMIT_BUTTON_3DS]:
            wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, locator,
                        message=u'Не произошёл редирект на страницу с 3ds в течение {} секунд',
                        waiting_time=waiting_time)

    def fill_3ds_subpage(self, card):
        password_3ds = card.get('3ds_password', DEFAULT_3DS_PASSWORD)
        self.driver.find_element(*self.PASSWORD_LOCATOR_3DS).send_keys(password_3ds)
        self.driver.find_element(*self.SUBMIT_BUTTON_3DS).click()

    def wait_for_present(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located,
                    self.card_form.CREDIT_NUMBER_LOCATOR,
                    message=u'Не появилась карточная форма на странице процессинга '
                            u'в течение {} секунд', waiting_time=waiting_time)

    def check_payment_data(self, data_for_checks):
        actual_amount = self.driver.find_element(*self.AMOUNT).text

        expected_amount = self.AMOUNT_FORMAT.format(data_for_checks['total_sum'])

        check_that(actual_amount, equal_to(expected_amount), step=u'Проверяем сумму платежа')


class PrivatPaymentPage(object):
    # check_payment_data_on_processing_page
    AMOUNT = (By.XPATH, u"//div[@class='container__block__info__summ']")
    DESCRIPTION = (By.XPATH, u"//div[@class='container__block__info__description__toggle']")

    AMOUNT_FORMAT = u'{}UAH'

    class CardForm(object):
        CREDIT_NUMBER_LOCATOR = (By.XPATH, u"//input[@el='Card' and @class='input card']")
        MONTH_YEAR = (By.XPATH, u"//input[@el='MMYY']")
        CVV = (By.XPATH, u"//input[@el='CardCvv']")
        SUBMIT_BUTTON = (By.XPATH, u"//div[@el='Send']")
        PAY_BUTTON = (By.XPATH, u"//div[@el='Send' and contains(.,'Оплатить')]")

        def __init__(self, driver):
            self.driver = driver

        def fill(self, card):
            card_number = self.driver.find_element(*self.CREDIT_NUMBER_LOCATOR)

            # Номер карты заполняется по частям
            for i, number_chunk in enumerate(BasePaymentPage.split_number(card['card_number'])):
                card_number.send_keys(number_chunk)

            month_year = self.driver.find_element(*self.MONTH_YEAR)
            month_year.send_keys(card['expiration_month'])
            month_year.send_keys(card['expiration_year'])

            self.driver.find_element(*self.CVV).send_keys(card['cvn'])

            self.driver.find_element(*self.SUBMIT_BUTTON).click()

            wait_for_ui(self.driver, expected_conditions.element_to_be_clickable, self.PAY_BUTTON,
                        message=u'Кнопка оплаты недоступна для действий '
                                u'в течение {} секунд', waiting_time=default_waiting_time)

        def press_pay_button(self):
            self.driver.find_element(*self.PAY_BUTTON).click()

    def __init__(self, driver):
        self.driver = driver
        self.card_form = self.CardForm(self.driver)

    def wait_for_present(self, waiting_time=default_waiting_time):
        wait_for_ui(self.driver, expected_conditions.visibility_of_element_located,
                    self.card_form.CREDIT_NUMBER_LOCATOR,
                    message=u'Не появилась карточная форма на странице процессинга '
                            u'в течение {} секунд', waiting_time=waiting_time)

    def check_payment_data(self, data_for_checks):
        actual_amount = self.driver.find_element(*self.AMOUNT).text
        actual_description = self.driver.find_element(*self.DESCRIPTION).text

        expected_amount = self.AMOUNT_FORMAT.format(data_for_checks['total_sum'])
        expected_description = data_for_checks['external_id']

        check_that(actual_amount, equal_to(expected_amount), step=u'Проверяем сумму платежа')
        check_that(actual_description, equal_to(expected_description), step=u'Проверяем назначение платежа')


class AlphaPaymentPage(object):
    # check_payment_data_on_processing_page
    AMOUNT = (By.ID, "amount")
    DESCRIPTION = (By.ID, "description")

    # AMOUNT_FORMAT = u'{}.00 RUR'
    AMOUNT_FORMAT = '{:.2f} RUR'
    DESCRIPTION_FORMAT = u'Оплата счета {}'

    # 3ds
    MASTERCARD_3DS_PASSWORD_LOCATOR = (By.XPATH, ".//input[@type='password']")
    SUBMIT_3DS_BUTTON = (By.XPATH, ".//input[@type='image']")

    class CardForm(object):
        CREDIT_NUMBER_LOCATOR = (By.ID, "pan_visible")
        MONTH = (By.ID, "month")
        YEAR = (By.ID, "year")
        CVV = (By.ID, "iCVC")
        CARDHOLDER = (By.ID, "iTEXT")
        SUBMIT_BUTTON = (By.ID, "buttonPayment")

        YEAR_FORMAT = u'20{}'

        def __init__(self, driver):
            self.driver = driver

        # TODO: torvald: add unTrusted clients (old protocol)
        def fill(self, card):
            # если не кликать в поле перед вводом номера карты, возникает плавающая ошибка "номер карты введен неверно"
            self.driver.find_element(*self.CREDIT_NUMBER_LOCATOR).click()
            self.driver.find_element(*self.CREDIT_NUMBER_LOCATOR).send_keys(card['card_number'])
            self.driver.find_element(*self.CARDHOLDER).send_keys(card['cardholder'])

            # для выбора срока действия карты вначале снимем "style='display: none;'"
            #  селектов чтобы к ним можно было обращаться из вебдрайвера
            script = "jQuery('#{0}').removeAttr('style');"
            self.driver.execute_script(script.format('month'))
            self.driver.execute_script(script.format('year'))

            Select(self.driver.find_element(*self.MONTH)).select_by_value(card['expiration_month'])
            full_year = self.YEAR_FORMAT.format(card['expiration_year']) if len(card['expiration_year']) == 2 else card[
                'expiration_year']

            Select(self.driver.find_element(*self.YEAR)).select_by_visible_text(full_year)

            self.driver.find_element(*self.CVV).send_keys(card['cvn'])

        def press_pay_button(self):
            self.driver.find_element(*self.SUBMIT_BUTTON).click()

    def __init__(self, driver):
        self.driver = driver
        self.card_form = self.CardForm(self.driver)

    def wait_for_present(self, waiting_time=default_waiting_time):
        for locator in [self.AMOUNT, self.DESCRIPTION, self.card_form.CREDIT_NUMBER_LOCATOR]:
            wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
                        message=u'Не появилась карточная форма на странице процессинга'
                                u' в течение {} секунд', waiting_time=waiting_time)

    def wait_until_3ds_redirect(self, waiting_time=default_waiting_time):
        for locator in [AlfaBankPayForm.PASSWORD_INPUT_LOCATOR, AlfaBankPayForm.SUBMIT_BUTTON_LOCATOR]:
            wait_for_ui(self.driver, expected_conditions.visibility_of_element_located, locator,
                        message=u'Не произошел редирект на страницу 3DS в течение {} секунд', waiting_time=waiting_time)

    def fill_3ds_subpage(self, card):
        password_3ds = card.get('3ds_password', DEFAULT_3DS_PASSWORD)
        AlfaBankPayForm.fill_pay_password(self.driver, password_3ds)
        self.driver.find_element(*AlfaBankPayForm.SUBMIT_BUTTON_LOCATOR).click()

    def check_payment_data(self, data_for_checks):
        actual_amount = self.driver.find_element(*self.AMOUNT).text
        actual_description = self.driver.find_element(*self.DESCRIPTION).text

        expected_amount = self.AMOUNT_FORMAT.format(D(data_for_checks['total_sum']))
        expected_description = self.DESCRIPTION_FORMAT.format(data_for_checks['external_id'])

        # check_that(actual_amount, equal_to(expected_amount), step=u'Проверяем сумму платежа')
        # check_that(actual_description, equal_to(expected_description), step=u'Проверяем назначение платежа')

        # TODO: torvald: checkPaymentClosedDt?


class TrustPaymentPage(BusesPaymentPage):
    class CardForm(CardFormReactPCIDSS):
        def press_pay_button(self):
            self.driver.find_element(By.XPATH, u"//button[@class='button accent']", u"Кнопка оплаты").click()

    def __init__(self, *args, **kwargs):
        super(TrustPaymentPage, self).__init__(*args, **kwargs)
        self.card_form = self.CardForm(self.driver)

    def wait_until_spinner_hide(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-overlay')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)

    def open_page(self, payment_form=None, payment_url=None):
        self.wait_until_spinner_hide()

    def wait_for_present(self, waiting_time=default_waiting_time):
        self.select_new_card()

    def press_ok_button(self):
        self.driver.find_element(By.XPATH, u"//button[@class='button accent']",
                                 u"Кнопка OK после совершения платежа").click()

    def check_payment_data(self, data_for_checks):
        pass


def get_payment_page(driver, processing):
    return PAYSTEP_PROCESSINGS.get(processing, TrustPaymentPage)(driver)


PAYSTEP_PROCESSINGS = {
    Processings.TRUST: TrustPaymentPage,
    Processings.ALPHA: AlphaPaymentPage,
    Processings.PRIVAT: PrivatPaymentPage,
    Processings.ING: INGPaymentPage,
    Processings.SAFERPAY: SAFERPAYPaymentPage,
    Processings.BILDERLINGS: BilderlingsPaymentPage,
    Processings.PRIOR: PriorPaymentPage
}
