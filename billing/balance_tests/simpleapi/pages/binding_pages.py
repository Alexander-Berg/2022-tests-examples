# coding: utf-8

from selenium.webdriver.common.by import By
from selenium.common.exceptions import NoSuchElementException
from selenium.webdriver.support import expected_conditions

import btestlib.reporter as reporter
from btestlib import environments
from simpleapi.common.utils import wait_for_ui
from simpleapi.pages import payment_pages


class BaseBindingPage(payment_pages.BasePage):
    def open_page(self, purchase_token=None):
        url = environments.simpleapi_env().trust_web_url.format('ru') + 'binding?purchase_token={}'.format(
            purchase_token)
        with reporter.step(u'Открываем страницу привязки карты по урлу {}'.format(url)):
            self.driver.get(url)
            self.wait_for_card_form_present()

    def wait_for_card_form_present(self, waiting_time=60):
        with payment_pages.inside_iframe(self.driver):
            wait_for_ui(self.driver, expected_conditions.visibility_of_element_located,
                        (By.XPATH, "//form[contains(@class, 'js_card-form')]"),
                        message=u'Не появилась карточная форма в течение {} секунд', waiting_time=waiting_time)

    def press_link_button(self):
        self.driver.find_element(By.XPATH, "//b[@class='action-button js-submit-button']|"
                                           "//button[@class='action-button js-submit-button']",
                                 u'Кнопка привязки').click()


class DirectBindingPage(BaseBindingPage):
    def __init__(self, *args, **kwargs):
        super(DirectBindingPage, self).__init__(*args, **kwargs)
        self.card_form = payment_pages.CardFormReactPCIDSS(self.driver)

    def wait_for_card_form_present(self, waiting_time=60):
        locator = (By.XPATH, "//div[contains(@class, 'spinner-overlay')]")
        wait_for_ui(self.driver, expected_conditions.invisibility_of_element_located, locator,
                    message=u'На странице висит спиннер в течение {} секунд', waiting_time=waiting_time)
        with payment_pages.inside_iframe(self.driver, payment_pages.CARD_REACT_IFRAME_PATH):
            wait_for_ui(self.driver, expected_conditions.visibility_of_element_located,
                        (By.XPATH, "//form[contains(@class, 'js_card-form')]"),
                        message=u'Не появилась карточная форма в течение {} секунд', waiting_time=waiting_time)

    def press_link_button(self):
        self.driver.find_element(By.XPATH, "//button[@class='button accent']", u'Кнопка привязки').click()


class PassportBindingPage(BaseBindingPage):
    def __init__(self, *args, **kwargs):
        super(PassportBindingPage, self).__init__(*args, **kwargs)
        self.card_form = payment_pages.CardFormPCIDSS(self.driver)


class MedicineBindingPage(BaseBindingPage):
    def __init__(self, *args, **kwargs):
        super(MedicineBindingPage, self).__init__(*args, **kwargs)
        self.card_form = payment_pages.CardFormReactPCIDSS(self.driver)

    def open_page(self, purchase_token=None, binding_url=None):
        with reporter.step(u'Открываем страницу привязки карты по урлу {}'.format(binding_url)):
            self.driver.get(binding_url)
            self.wait_for_card_form_present()

    def wait_for_card_form_present(self, waiting_time=60):
        with payment_pages.inside_iframe(self.driver, payment_pages.CARD_REACT_IFRAME_PATH):
            wait_for_ui(self.driver, expected_conditions.visibility_of_element_located,
                        (By.XPATH, "//form[contains(@class, 'js_card-form')]"),
                        message=u'Не появилась карточная форма в течение {} секунд', waiting_time=waiting_time)

    def press_link_button(self):
        # Есть два варианта страницы для ввода карты - стандартный и Yandex Pay
        # Разметка на них отличается
        try:
            self.driver.find_element(
                By.XPATH,
                "//div[@class='YpcPaymentForm-PaymentButtonContainer']//button", u'Кнопка привязки'
            ).click()
        except NoSuchElementException as e:
            self.driver.find_element(
                By.XPATH,
                "//div[@class='footer__right__button']//div[@class='formfield']//button", u'Кнопка привязки'
            ).click()


class CloudBindingPage(MedicineBindingPage):
    pass


import traceback


class YandexPage(payment_pages.BasePage):
    def open_page(self, *args, **kwargs):
        try:
            self.driver.get('http://yandex.ru')
        except Exception as e:
            traceback.print_exc()
