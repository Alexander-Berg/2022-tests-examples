# -*- coding: utf-8 -*-

from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.by import By
from balance.balance_web import DefaultPage
from btestlib import utils, environments

__author__ = 'kostya-karpus'

BASE_URL = environments.apikeys_env().apikeys_ai
# BASE_URL = 'https://apikeys-dev.yandex.ru:8092'


class ApikeysDefaultPage(DefaultPage):
    DESCRIPTION = u"Apikeys Страница"

    YA_LINK = (By.XPATH, './/*[@class="logo__link"]', u"Ссылка на морду'")
    TECH_LINK = (By.XPATH, u'.//*[text()="Технологии"]', u"Ссылка на технологии")
    APIKEYS_LINK = (By.XPATH, u'.//*[text()="Кабинет разработчика"]', u"Ссылка на Кабинет разработчика")
    FEEDBACK_LINK = (By.XPATH, u'.//*[text()="Обратная связь"]', u"Обратная связь")
    LANG = (By.XPATH, './/*[@class="lang-switcher__lang-name"]', u"Выбор языка")
    COPYRIGHT = (By.XPATH, './/*[@class="copyright"]', u"copyright")

    @classmethod
    def url(cls):
        return '{base_url}/'.format(base_url=BASE_URL)

    def is_ya_link_present(self):
        return utils.Web.is_element_present(self.driver, self.YA_LINK)

    def is_tech_link_present(self):
        return utils.Web.is_element_present(self.driver, self.TECH_LINK)

    def is_apikeys_link_present(self):
        return utils.Web.is_element_present(self.driver, self.APIKEYS_LINK)

    def is_feedback_link_present(self):
        return utils.Web.is_element_present(self.driver, self.FEEDBACK_LINK)

    def is_copyright_present(self):
        return utils.Web.is_element_present(self.driver, self.COPYRIGHT)

    def is_lang_present(self):
        return utils.Web.is_element_present(self.driver, self.LANG)


class GetKeyPage(ApikeysDefaultPage):
    DESCRIPTION = u"Страница получения ключа"
    GET_KEY_BUTTON = (By.XPATH, '//button', u"Кнопка 'Получить Кллюч'")
    UNDO_BUTTON = (By.XPATH, '//form/div[3]/button', u"Кнопка 'Отменить'")
    SERVICES = (By.NAME, "service")

    def get_key(self):
        self.driver.find_element(*self.GET_KEY_BUTTON).click()

    def is_services_present(self):
        return utils.Web.is_element_present(self.driver, self.SERVICES)

    def is_undo_button_present(self):
        return self.driver.find_elements(*self.UNDO_BUTTON)[0].text == u'Отменить'

    def select_service_by_name(self, name):
        return self.driver.find_elements('xpath', u".//*[text()='{}']/..".format(name))[0].click()


class ErrorPage(ApikeysDefaultPage):
    DESCRIPTION = u"Страница 404"
    ERROR_TEXT = (By.CLASS_NAME, u'http-error__text')
    MAIN_PAGE_LINK = (By.XPATH, './/*[contains(@class, "http-error__main")]/child::*', u'Ссылка на главную')

    def get_main_page(self):
        return self.driver.find_elements(*self.MAIN_PAGE_LINK)[0].click()


class LoginPage(ApikeysDefaultPage):
    DESCRIPTION = u"Страница без залогиненого пользователя"
    DOMIK = (By.CLASS_NAME, u'domik__roof-body')

    def is_domik_present(self):
        return utils.Web.is_element_present(self.driver, self.DOMIK)


class LinkedKeyPage(ApikeysDefaultPage):
    DESCRIPTION = u"Страница информации по ключу"
    KEY_TAB = (By.ID, u'developer-tabs-tab-0')
    FINANCE_TAB = (By.ID, u'developer-tabs-tab-1')
    KEY_NAME = (By.CLASS_NAME, u'keys-list__name-name')
    KEY_INPUT = (By.NAME, u'name')
    KEY_CLIPBOARD = (By.ID, u'ZeroClipboardMovie_2')
    SERVICES = (By.NAME, "service")

    def is_services_present(self):
        return utils.Web.is_element_present(self.driver, self.SERVICES)

    def is_key_tab_present(self):
        return utils.Web.is_element_present(self.driver, self.KEY_TAB)

    def is_finance_tab_present(self):
        return utils.Web.is_element_present(self.driver, self.FINANCE_TAB)

    def is_service_present(self, service_name):
        return utils.Web.is_element_present(self.driver, (By.XPATH, u'.//*[text()="{}"]'.format(service_name)))

    def switch_to_finance(self):
        self.driver.find_element(*self.FINANCE_TAB).click()

    def rename_key(self, new_name):
        self.driver.find_element(*self.KEY_NAME).click()
        key_name_input = self.driver.find_element(*self.KEY_INPUT)
        key_name_input.send_keys(Keys.CONTROL + 'a')
        key_name_input.send_keys(new_name)
        key_name_input.send_keys(Keys.ENTER)


class FreeKeyPage(ApikeysDefaultPage):
    DESCRIPTION = u"Страница ключа без сервиса"
    KEY = (By.CLASS_NAME, u'keys-list__value-value')
    PLUGIN_SERVICE_BUTTON = (By.XPATH, u'/html/body/div[4]/div/div/div[1]/div/div[2]/div/div[3]/div[2]/button/span')

    def get_key(self):
        return self.driver.find_elements(*self.KEY)[0].text

    def is_plugin_service_button_present(self):
        return utils.Web.is_element_present(self.driver, self.PLUGIN_SERVICE_BUTTON)


class FinancePage(ApikeysDefaultPage):
    DESCRIPTION = u"Страница Финансы"
    KEY_TAB = (By.ID, u'developer-tabs-tab-0')
    FINANCE_TAB = (By.ID, u'developer-tabs-tab-1')
    SERVICES_NAME = (By.CLASS_NAME, u'developer-contracts__service-name')
    CONTRACT_INFO = (By.CLASS_NAME, u'contract-info__title')
    PERSON_BUTTON = (By.XPATH, './/*[contains(@class, "attach-person")]/child::*')
    TARIFF_CHANGE_BUTTON = (By.XPATH, u'.//*[text()="Изменить тариф"]')
    PERSON_LISTBOX = (By.XPATH, u'/html/body/div[10]/div[1]/div[2]/span/button')
    SELECT_BUTTON = (By.XPATH, './/*[contains(@class, "button button_theme_action")]')
    PERSON_POPUP_TITLE = (By.XPATH, u'.//*[text()="Выбор плательщика"]')
    PERSON_SELECT = (By.XPATH, './/*[contains(@class, "button_arrow_down")]')
    PERSON_POPUP_WARNING = (By.XPATH, u'.//*[text()="Внимание! Это решение нельзя будет изменить."]')
    PERSON_SELECTED = (By.CLASS_NAME, u'person')

    def is_key_tab_present(self):
        return utils.Web.is_element_present(self.driver, self.KEY_TAB)

    def is_finance_tab_present(self):
        return utils.Web.is_element_present(self.driver, self.FINANCE_TAB)

    def is_services_name_present(self):
        return utils.Web.is_element_present(self.driver, self.SERVICES_NAME)

    def is_contract_info_present(self):
        return utils.Web.is_element_present(self.driver, self.CONTRACT_INFO)

    def is_person_present(self):
        return utils.Web.is_element_present(self.driver, self.PERSON_SELECTED)

    def get_person(self):
        self.driver.find_element(*self.PERSON_BUTTON).click()
        WebDriverWait(self.driver, 50).until(EC.presence_of_element_located(self.PERSON_SELECT))
        self.driver.find_element(*self.SELECT_BUTTON).click()
        WebDriverWait(self.driver, 50).until(EC.presence_of_element_located(self.PERSON_SELECTED))


