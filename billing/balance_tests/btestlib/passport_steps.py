# coding: utf-8
import urlparse
from urllib import urlencode

import requests
from hamcrest import equal_to
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

import btestlib.reporter as reporter
from btestlib.constants import Passports, Users
from utils import wait_until, TestsError

log = reporter.logger()

# кэш авторизационных данных
# при создании каждой сессии запоминаем ее и куки из нее, если сессию не создаем, то запоминаем только куки
users_sessions_cache = {}
users_cookies_cache = {}


# авторизация get-запросом через акву
def auth_get(driver, user=Users.YB_ADM, passport=Passports.PROD):
    auth_params = {'mode': 'auth', 'login': user.login, 'passwd': user.password,
                   'host': urlparse.urljoin(passport.url, '/passport')}
    auth_url = 'https://aqua.yandex-team.ru/auth.html?{params}'.format(params=urlencode(auth_params))

    if is_authorised(driver, user, passport.url):
        return

    with reporter.step(u"Авторизуемся под {}".format(user)):
        driver.get(auth_url, name=passport.descr)
        wait_until(lambda: is_authorised(driver, user, passport.url), equal_to(True))
        cache_cookies(user, driver.get_cookies())


# авторизация в сессии (без драйвера)
def auth_session(user=Users.YB_ADM, passport=Passports.PROD, need_allure=True):
    # todo по-хорошему здесь нужно вместо session.post использовать наш utils.call_http,
    # но перед этим нужно сделать аллюр отключаемым в call_http

    cached_user_session = get_cached_user_session(user)
    if cached_user_session:
        return cached_user_session

    auth_data = dict(login=user.login, passwd=user.password)
    with reporter.step(u"Создаем авторизованную сессию для {}".format(user), allure_=not need_allure):
        session = requests.Session()
        session.post(urlparse.urljoin(passport.url, '/auth'), auth_data)
        if is_user_in_cookies(user, session.cookies, passport.url):
            cache_session_and_cookies(user, session)
            return session
        else:
            raise TestsError(u"Не удалось создать авторизованную сессию для {} "
                             u"(нужно проверить возможность логина под данным пользователем вручную)".format(user))


# авторизуемся post-запросом в паспорте и подкладываем куки в драйвер
def auth_post(driver, user=Users.YB_ADM, passport=Passports.PROD, is_redirected_to_passport=False):
    if not get_cached_user_cookies(user):
        auth_session(user, passport, need_allure=False)
    auth_web(driver, user, passport, is_redirected_to_passport)


# авторизация через веб-форму
def auth_web(driver, user=Users.YB_ADM, passport=Passports.PROD, is_redirected_to_passport=False):
    cookies = get_cached_user_cookies(user)

    with reporter.step(u"Авторизуемся под {}".format(user)):

        if not is_redirected_to_passport:
            driver.get(passport.url, name=passport.descr)

        if cookies and is_user_in_cookies(user, cookies, passport.url):
            # не проверяем авторизованы ли мы в драйвере, и всегда добавляем закэшированные куки в драйвер
            # т.к. запрос кук из драйвера иногда зависает
            _add_cookies_to_driver(driver, cookies, passport.url)
        else:
            PassportPage.auth(driver, user.login, user.password)
            wait_until(lambda: is_authorised(driver, user, passport.url), equal_to(True))
            cache_cookies(user, driver.get_cookies())


def is_authorised(driver, user, passport_url):
    reporter.log_extra("Check if user is in driver.cookies")
    if is_user_in_cookies(user, driver.get_cookies(), passport_url):
        with reporter.step(u"Уже авторизованы под пользователем {}".format(user)):
            return True
    return False


# есть ли авторизационная кука пользователя для данного паспорта в cookies
def is_user_in_cookies(user, cookies, passport_url):
    reporter.log_extra("Checking if user {} in cookies {}".format(user, cookies))
    cookie_with_login = _get_cookie(_convert_cookies_to_webdriver_format(cookies), name='yandex_login')
    is_login_in_cookies = cookie_with_login is not None and \
                          (cookie_with_login.get('value') == user.login or \
                          # в логинах tus паспорт меняет последний дефис на точку
                           cookie_with_login.get('value') ==
                           (user.login[:user.login.rfind('-')] + '.' + user.login[user.login.rfind('-')+1:])) \
                          and cookie_with_login.get('domain') == _get_login_cookie_domain(passport_url)
    reporter.log_extra("Result: {}".format(is_login_in_cookies))
    return is_login_in_cookies


def _get_cookie(cookies, name):
    for cookie in cookies:
        if cookie['name'] == name:
            return cookie
    return None


# по урл определяем домен с которым сохраняется кука с логином
def _get_login_cookie_domain(passport_url):
    return '.' + '.'.join(urlparse.urlparse(passport_url).netloc.split('.')[1:])


# куки можно подкладывать только для домена на котором мы находимся,
# поэтому перед подкладыванием отфильтровываем неподходящие
def _add_cookies_to_driver(driver, cookies, current_url):
    domain = urlparse.urlparse(current_url).netloc
    filtered_cookies = _filter_cookies_by_domain(cookies, domain)

    if filtered_cookies:
        with reporter.step(u"Подкладываем пользовательские куки в браузер"):
            # map(driver.add_cookie, filtered_cookies)
            for cookie in filtered_cookies:
                reporter.log_extra('Cookie to add: {}'.format(cookie))
                driver.add_cookie(cookie)
            reporter.log_extra('All cookies added')
    else:
        log.debug(u'Отсутствуют куки подходящие для домена {}'.format(domain))


def _convert_cookies_to_webdriver_format(cookies):
    if cookies and isinstance(cookies, requests.cookies.RequestsCookieJar):
        cookie_list = []
        for cookie in cookies:
            cookie_list.append({'domain': cookie.domain, 'name': cookie.name, 'value': cookie.value,
                                # 'expiry': cookie.expires,
                                'path': cookie.path,
                                'httpOnly': True if 'HttpOnly' in cookie._rest else False,
                                'secure': cookie.secure})
        return cookie_list
    else:
        return cookies


def driver_cookies_to_dict(driver_cookies):
    return {cookie['name']: cookie['value'] for cookie in driver_cookies}


# возвращаем куки домена и всех его поддоменов
def _filter_cookies_by_domain(cookies, domain):
    domain_list = [domain]
    domain_parts = domain.split('.')[1:]
    while domain_parts:
        subdomain = '.'.join(domain_parts)
        domain_list.extend(['.' + subdomain, subdomain])
        domain_parts = domain_parts[1:]

    return [cookie for cookie in cookies if cookie['domain'] in domain_list]


def cache_session_and_cookies(user, session):
    users_sessions_cache[user.uid] = session
    cache_cookies(user, session.cookies)


def cache_cookies(user, cookies):
    users_cookies_cache[user.uid] = _convert_cookies_to_webdriver_format(cookies)


def get_cached_user_session(user):
    cached_user_session = users_sessions_cache.get(user.uid)
    reporter.log_extra("user: {}, cached_user_session: {}".format(user, cached_user_session))
    return cached_user_session


def get_cached_user_cookies(user):
    cached_user_cookies = users_cookies_cache.get(user.uid)
    reporter.log_extra("user: {}, cached_user_cookies: {}".format(user, cached_user_cookies))
    return cached_user_cookies


class PassportPage(object):
    LOGIN_FIELD = (By.ID, 'login', u'Логин')
    PASSWORD_FIELD = (By.ID, 'passwd', u'Пароль')
    # Новый интерфейс
    # LOGIN_FIELD = (By.NAME, 'login', u'Логин')
    # PASSWORD_FIELD = (By.NAME, 'passwd', u'Пароль')

    SUBMIT_BUTTON = (By.XPATH, "//button[@type='submit']", u"Кнопка 'Войти'")

    @staticmethod
    def auth(driver, login, password):
        with reporter.step(u"Вводим данные пользователя"):
            WebDriverWait(driver, timeout=10).until(EC.presence_of_element_located(PassportPage.LOGIN_FIELD))

            driver.find_element(*PassportPage.LOGIN_FIELD).send_keys(login)
            driver.find_element(*PassportPage.PASSWORD_FIELD).send_keys(password)
            driver.find_element(*PassportPage.SUBMIT_BUTTON).click()


# todo sunshineguy: Сейчас работает только для КП, жестко захоркожено в url, возможно есть смысл сделать выбор?
def get_broker_task_id(session=None, cookies=None):
    """
        Функция возвращает task_id брокера для связи Паспортного аккаунта с аккаунтом социальной сети,
        в данном случае - с кинопоиском. Подробнее об этом: https://wiki.yandex-team.ru/social/api/
    """
    url = 'https://social-test.yandex.ru/broker2/start?' \
          'retpath=http%3A%2F%2Ftesting.kinopoisk.ru%2F&retnopopup=false&' \
          'consumer=kinopoisk&place=fragment&application=yandex-kinopoisk'
    if not session and cookies:
        session = requests.Session()
        session.cookies = cookies
    for _ in range(10):  # Количесвто допустимых редиректов
        redirect_url = session.get(url,
                                   verify=False, allow_redirects=False).headers['Location']
        if redirect_url.split('&')[-1].startswith('task_id='):
            return redirect_url.split('&')[-1][8::]
        url = redirect_url
    raise TestsError(u'Вышли за количество допустимых редиректов. Скорее всего что-то пошло не так')


def process_getting_broker_task_id(user, passport=Passports.TEST):
    session = auth_session(user, passport, need_allure=False)
    return get_broker_task_id(session)
