# coding=utf-8

import hamcrest
import pytest

from apikeys import apikeys_steps, apikeys_api as api, apikeys_defaults as defaults
from apikeys.tests_by_typical_flows.web_pages import *
from balance import balance_web as web, balance_steps as steps
from btestlib import utils, environments, secrets
from btestlib.constants import User

__author__ = 'kostya-karpus'


UID, LOGIN = defaults.APIKEYS_LOGIN_POOL[1]
WEB_USER = User(UID, LOGIN, secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

BASE_URL = environments.apikeys_env().apikeys_ai
# BASE_URL = 'https://apikeys-dev.paysys.yandex.ru:8092'

SERVICE_TOKEN = 'market_f5de92175940968a63a4e8abaa056c8b6c4ebbc5'
SERVICE_CC = 'market'
TARIFF = 'market_vendor_mini'
PRICE = 20000
ADMIN = defaults.ADMIN


@pytest.fixture(scope='function', autouse=True)
def clean_user():
    apikeys_steps.clean_up(WEB_USER.uid)


def test_header_and_footer():
    with web.Driver(WEB_USER) as driver:
        main_page = ApikeysDefaultPage(driver).open(driver)
        utils.check_that((
            main_page.is_ya_link_present(),
            main_page.is_apikeys_link_present(),
            main_page.is_tech_link_present(),
            main_page.is_apikeys_link_present(),
            main_page.is_copyright_present(),
            main_page.is_lang_present(),
            main_page.is_feedback_link_present()
        ), hamcrest.only_contains(True), u'Проверяем наличие элементов на странице')


def test_get_key():
    with web.Driver(WEB_USER) as driver:
        get_key_page = GetKeyPage(driver).open(driver)
        get_key_page.get_key()
        utils.check_that((
            get_key_page.is_services_present(),
        ), hamcrest.only_contains(True), u'Проверяем наличие сервисов в попапе')

        utils.check_that((
            get_key_page.is_undo_button_present(),
        ), hamcrest.only_contains(True), u'Проверяем наличие кнопки Отменить')


def test_link_key_to_service():
    with web.Driver(WEB_USER) as driver:
        get_key_page = GetKeyPage(driver).open(driver)
        get_key_page.get_key()
        get_key_page.select_service_by_name('Safe Browsing API')

        # page_404 = ErrorPage(driver)
        # WebDriverWait(driver, 30).until(EC.presence_of_element_located(page_404.ERROR_TEXT))
        # page_404.get_main_page()
        key_page = LinkedKeyPage(driver)
        WebDriverWait(driver, 30).until(EC.presence_of_element_located(key_page.KEY_NAME))

        utils.check_that(key_page.is_service_present('Safe Browsing API'), hamcrest.is_(True))


def test_without_cookies():
    with web.Driver(WEB_USER) as driver:
        driver.delete_all_cookies()
        driver.get(BASE_URL)
        log_in = LoginPage(driver)
        utils.check_that(log_in.is_domik_present(), hamcrest.is_(True))


def test_key_without_service():
    api.UI.create_key(WEB_USER.uid)
    key = api.UI.list_keys(WEB_USER.uid).json()['result']['keys'][0]['id']
    with web.Driver(WEB_USER) as driver:
        ApikeysDefaultPage(driver).open(driver)
        key_page = FreeKeyPage(driver)
        WebDriverWait(driver, 30).until(EC.presence_of_element_located(key_page.PLUGIN_SERVICE_BUTTON))
        utils.check_that(key_page.get_key(), hamcrest.equal_to(key), u'Проверяем ключ на странице')
        utils.check_that(key_page.is_plugin_service_button_present(),
                         hamcrest.equal_to(True),
                         u'Проверяем наличие кнопки "Подключить сервисы"')


def test_person_select():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, WEB_USER.login)
    steps.PersonSteps.create(client_id, 'ur')
    service_data = apikeys_steps.get_service_data(SERVICE_CC)
    token, service_id = service_data['token'], int(service_data['_id'])
    key = api.BO().create_key(defaults.ADMIN, WEB_USER.uid).json()[u'result'][u'key']
    api.BO().update_service_link(defaults.ADMIN, key, service_id)
    api.BO().get_client_from_balance(defaults.ADMIN, WEB_USER.uid)

    with web.Driver(WEB_USER) as driver:
        ApikeysDefaultPage(driver).open(driver)
        key_page = LinkedKeyPage(driver)
        utils.check_that(key_page.is_service_present(u'API Яндекс.Маркета'), hamcrest.equal_to(True))
        key_page.switch_to_finance()
        finance_page = FinancePage(driver)
        utils.check_that(finance_page.is_services_name_present(), hamcrest.equal_to(True))
        finance_page.get_person()
        utils.check_that(finance_page.is_person_present(), hamcrest.equal_to(True))


@pytest.mark.parametrize('new_key_name', ['test', u'Я тест', '!@#$%^&*()/\?><'],
                         ids=lambda x: u'new_key_name: {}'.format(x).encode('utf-8'))
def test_rename_key(new_key_name):
    service_id = int(apikeys_steps.get_service_data(SERVICE_CC)['_id'])
    key = api.BO().create_key(defaults.ADMIN, WEB_USER.uid).json()[u'result'][u'key']
    api.BO().update_service_link(defaults.ADMIN, key, service_id)

    with web.Driver(WEB_USER) as driver:
        ApikeysDefaultPage(driver).open(driver)
        key_page = LinkedKeyPage(driver)
        WebDriverWait(driver, 10).until(EC.presence_of_element_located(key_page.KEY_NAME))
        key_page.rename_key(new_key_name)
        driver.implicitly_wait(0.5)
        utils.check_that(driver.find_element(*key_page.KEY_NAME).text, hamcrest.equal_to(new_key_name),
                         u'Проверяем новое имя ключа до перезагрузки страницы')

        ApikeysDefaultPage(driver).open(driver)
        key_page = LinkedKeyPage(driver)
        WebDriverWait(driver, 10).until(EC.presence_of_element_located(key_page.KEY_NAME))
        utils.check_that(driver.find_element(*key_page.KEY_NAME).text, hamcrest.equal_to(new_key_name),
                         u'Проверяем новое имя ключа после перезагрузки страницы')


@pytest.mark.parametrize('service_id', [10000003, 15, 3, 20],
                         ids=lambda x: '[1 linked with service_id: {}]'.format(x))
@pytest.mark.parametrize('free_keys_amount', [19, 1],
                         ids=lambda x: '[free_keys: {}]'.format(x))
def test_finance_tab(service_id, free_keys_amount):
    api.BO().update_service_link(ADMIN,
                                 api.BO().create_key(ADMIN, WEB_USER.uid).json()['result']['key'],
                                 service_id)

    for _ in xrange(free_keys_amount):
        api.BO().create_key(ADMIN, WEB_USER.uid)

    with web.Driver(WEB_USER) as driver:
        ApikeysDefaultPage(driver).open(driver)
        key_page = LinkedKeyPage(driver)
        utils.check_that(key_page.is_key_tab_present(), hamcrest.is_(True))
