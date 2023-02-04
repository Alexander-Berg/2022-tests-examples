# coding=utf-8
import btestlib.passport_steps
import btestlib.reporter as reporter
from btestlib.constants import Passports, User as BalanceUser
from simpleapi.common.utils import WebDriverProvider

__author__ = 'fellow'


def _define_passport(user, domain='ru'):
    if user.is_test():
        passport = Passports.TEST
    else:
        passport = Passports.PROD

    return passport.with_domain(domain)


domain_map = {225: 'ru',
              983: 'com.tr',
              None: 'ru'}


def auth_via_page(user, passport=None, driver=None, region=225):
    with reporter.step(u'Авторизуемся на странице паспорта пользователем {}'.format(user)):
        reporter.log_extra("passport: {}, driver: {}".format(passport, driver))
        if driver is None:
            driver = WebDriverProvider.get_instance().get_driver()

        if passport is None:
            domain = domain_map.get(region, 'com')
            passport = _define_passport(user, domain=domain)

        balance_user = BalanceUser(user.id_, user.login, user.password)
        btestlib.passport_steps.auth_post(driver, balance_user, passport)


def get_current_session_id():
    with reporter.step(u'Получаем значение текущего session_id'):
        driver = WebDriverProvider.get_instance().get_driver()
        return driver.get_cookie(name='sessionid2').get('value')


def get_passport_session_with_cookies(user):
    with reporter.step(u'Авторизуемся на странице паспорта пользователем {}'.format(user)):
        passport = _define_passport(user)
        balance_user = BalanceUser(user.id_, user.login, user.password)
        return btestlib.passport_steps.auth_session(balance_user, passport=passport, need_allure=False)


def get_yandexru_session_with_cookies():
    import requests
    url = 'https://yandex.ru'
    session = requests.Session()
    session.get(url, verify=False)
    cookies = dict(yandexuid=session.cookies['yandexuid'])
    return session, cookies