# coding: utf-8
import copy
import functools
import itertools
import json
import logging
import pkgutil
import random
import re
from base64 import b64encode, b64decode

import attr
import baluhn
import requests
import six
from Crypto.Cipher import PKCS1_OAEP as Cipher
from Crypto.Hash import SHA256, SHA512
from Crypto.PublicKey import RSA
from Crypto.Signature import PKCS1_v1_5
from selenium.common.exceptions import TimeoutException
from selenium.common.exceptions import WebDriverException
from selenium.webdriver.support.wait import WebDriverWait
from xmltodict import parse

from btestlib import environments
from btestlib import reporter
from btestlib import utils as butils
from btestlib.constants import Services
from simpleapi.common import logger

__author__ = 'fellow'

log = logger.get_logger()
logging.getLogger("requests").setLevel(logging.WARNING)


def remove_empty(params):
    return {k: v for k, v in params.items() if v is not None}


def keys_to_lowercase(params):
    return {k.lower(): v for k, v in params.items()}


def find_dict_in_list(list_of_dicts, **kwargs):
    for _dict in list_of_dicts:
        if all(kwargs.get(key) == type(kwargs.get(key))(_dict.get(key)) for key in kwargs.keys()):
            return _dict


class CallHttpValueError(Exception):
    pass


def call_http_raw(uri, params=None, method='POST', headers=None, json_data=None, cookies=None, auth_user=None,
                  cert_path=None, key_path=None, **kwargs):
    with reporter.step(u'Вызываем HTTP-{}: {}'.format(method, uri)):
        if 'trust' in uri:
            headers = headers or {}
            headers['X-Trust-Me-I-Am-Autotest'] = 'WE_ARE_THE_BALANCE'
        auth = None
        if auth_user:
            auth = (auth_user.login, auth_user.password)

        if method in ['POST', 'PUT', 'DELETE']:
            reporter.report_http_call_curl(method, uri, headers, params, json_data, cookies=cookies)
            func = getattr(requests, method.lower())
            result = func(uri, data=params, headers=headers, verify=False, json=json_data, cookies=cookies, auth=auth,
                          cert=(cert_path, key_path), **kwargs)
        elif method in ['GET', 'OPTIONS']:
            req = requests.Request(method, uri, params=params, headers=headers)
            prepared_request = req.prepare()
            reporter.report_http_call_curl(method, prepared_request.url, headers, cookies=cookies)
            result = requests.get(uri, params=params, headers=headers, verify=False, cookies=cookies,
                                  auth=auth, cert=(cert_path, key_path), **kwargs)
        else:
            raise CallHttpValueError('Incorrect method name given')

        try:
            result_content = json.loads(result.content)
        except ValueError:
            result_content = result.content

        if 'application/xml' in result.headers.get('content-type') and result_content:
            import xml.dom.minidom
            xml = xml.dom.minidom.parseString(result_content)
            result_content = xml.toprettyxml()

        reporter.attach(u'Ответ', u'Код ответа: {}\nТело ответа:\n{}'.format(result.status_code,
                                                                             reporter.pformat(result_content)))

        return result, result_content


def call_http(uri, params=None, method='POST', headers=None, json_data=None, cookies=None, auth_user=None,
              cert_path=None, key_path=None, check_code=False):
    result, result_content = call_http_raw(uri, params, method, headers, json_data, cookies, auth_user, cert_path,
                                           key_path)

    if check_code:
        if isinstance(check_code, list):
            assert result.status_code in check_code, \
                '%s Error: %s for url: %s' % (result.status_code, result.reason, result.url)
        else:
            result.raise_for_status()

    return result_content


def parse_xml(func):
    def wrapper(*args, **kwargs):
        expected_xml = func(*args, **kwargs)
        return parse(expected_xml)

    return wrapper


def restart_if(key, error, max_tries=2):
    def restarter(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            tries = 0

            def check_if_need_restart(resp):
                if isinstance(resp, dict) and response[key] == error:
                    return True
                return False

            response = func(*args, **kwargs)
            need_restart = check_if_need_restart(response)

            while need_restart and tries < max_tries:
                log.debug("Got an error '{}' while execute function {}. Try to restart"
                          .format(error, func.__name__))
                response = func(*args, **kwargs)
                need_restart = check_if_need_restart(response)
                tries += 1

            return response

        return wrapper

    return restarter


def return_with(key):
    def returner(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            response = func(*args, **kwargs)
            if isinstance(key, list):
                resp = response.copy()
                for k in key:
                    resp = resp.get(k)
                return response, resp

            return response, response.get(key)

        return wrapper

    return returner


def return_only(key):
    def returner(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            response = func(*args, **kwargs)
            return response.get(key)

        return wrapper

    return returner


def sorted_orders(func):
    @functools.wraps(func)
    def sorting(*args, **kwargs):
        keys_to_be_sorted = {
            'orders': 'service_order_id',
            'discount_details': 'order',
        }

        response = func(*args, **kwargs)

        for key, sorting_by in keys_to_be_sorted.iteritems():
            if response.get(key) and len(response.get(key)) > 1:
                log.debug('Sorting orders...')
                sorted_list = sorted(response.get(key), key=lambda k: k[sorting_by])
                response.update({key: sorted_list})

        return response

    return sorting


def data_product(*data):
    result = []
    for element in itertools.product(*data):
        result.append(element)

    return result


class SimpleRandom(object):
    """
    По-умолчанию модуль random использует при инициализации генератора псевдослучайных чисел
    либо текущее время, либо сгенерированное системным генератором случайное число
    При запуске на тимсити используется системный генератор который (таинственно) генерит одно и то же чило
    Таким образом random инициализирутеся одним и
    тем же числом и возвращает одну и ту же последовательность "случайных" чисел

    Чтобы избежать этой проблемы явным образом используем для инициализации текущее время
    """

    def seed(self):
        random.seed(random.SystemRandom().random())

    def randint(self, a, b):
        self.seed()
        return random.randint(a, b)

    def random(self):
        self.seed()
        return random.random()

    def choice(self, seq):
        self.seed()
        return random.choice(seq)

    def sample(self, population, k):
        self.seed()
        return random.sample(population, k)


simple_random = SimpleRandom()


class LuhnNumber(object):
    # base is shorter by one digit!
    # it's normal
    # get_new_number appends correct last digit
    # to satisfy Luhn validation procedure
    base_master_card = 5 * 10 ** 14

    @staticmethod
    def _get_base(base):
        # this logic for https://st.yandex-team.ru/PCIDSS-98
        nonprodbase = 1 * 10 ** 8
        return str(base + simple_random.randint(nonprodbase, nonprodbase + 9 * 10 ** 8))
        # return str(simple_random.randint(base, base + 9 * 10 ** 13))

    @staticmethod
    def get_new_number(base=base_master_card):
        num_base = LuhnNumber._get_base(base)
        return str(num_base) + baluhn.generate(num_base)

    @staticmethod
    def get_new_number_with_prefix(prefix):
        prefix = str(prefix)
        base = prefix + ''.join([str(random.randint(0, 9))
                                 for i in range(16 - len(prefix) - 1)])
        return base + baluhn.generate(base)


class WebDriverProvider(object):
    INSTANCE = None
    driver = None

    @classmethod
    def get_instance(cls):
        if not cls.INSTANCE:
            cls.INSTANCE = WebDriverProvider()
        return cls.INSTANCE

    def get_driver(self):
        from selenium import webdriver

        if not self.driver:

            if butils.is_local_launch():
                if pkgutil.find_loader('btestlib.local_config'):
                    import btestlib.local_config as cfg
                    chromium_path = cfg.ENVIRONMENT_SETTINGS['chromium_path']
                    self.driver = webdriver.Chrome(chromium_path)
            else:
                capabilities = webdriver.DesiredCapabilities.CHROME.copy()
                capabilities['version'] = '80.0'
                from selenium.webdriver.remote.remote_connection import RemoteConnection
                command_executor = RemoteConnection('http://balance:balance@sw.yandex-team.ru:80/v0', resolve_ip=False)
                command_executor.set_timeout(150)
                self.driver = webdriver.Remote(
                    command_executor,
                    desired_capabilities=capabilities)
            reporter.log(u'(WebDriverProvider) Открываем браузер {browser_name} (version {version})'.format(
                browser_name=self.driver.capabilities.get('browserName', 'Unknown').title(),
                version=self.driver.capabilities.get('version', 'Unknown')))

            self.driver = butils.Web.ReportingDriver(self.driver)

        return self.driver

    def close_driver(self):
        if self.driver:
            try:
                self.driver.close()
            except WebDriverException as e:
                if 'was terminated due to TIMEOUT' in e.msg \
                        or 'Session timed out or not found' in e.msg:
                    # если поймали одно из таких исключений,
                    # значит просто долго не обращались в драйверу и он протух
                    # это не ошибка
                    pass
                else:
                    raise e

        self.driver = None


@attr.s()
class DataObject(object):
    """
    Общий объект для класса данных во всех тестах.
    Нужен для унификации данных и объединения общих частей (типа ids).
    Пока находится здесь, но вообще надо переместить в utils.
    Обеспечивает удобное хранение и использование данных в тестах.
    Содержит функции ids_... для всех данных, а так же общую ids для пака данных,
    которая формируюется из ids_... для каждого атрибута.
    Есть уже введенные атрибуты: service, user, paymethod и т.д.
    Это общие атрибуты, которые используются в большей части тестов.
    Но всегда можно ввести новый, специфический для данного теста (тестов) атрибут, передав его в функцию new.
    Например: DataObject(currency='RUB').new(region=225, person_type=defaults.Person.PH,
                                                  product_id=defaults.Direct.product_id)

    Надо еще подумать можно ли выделить какие-то предопределенные паки данных (типа PAYMETHOD_CARD_RANDOM_USER)
    """
    service = attr.ib(default=None)
    user = attr.ib(default=None)
    user_type = attr.ib(default=None)
    user_ip = attr.ib(default=None)
    paymethod = attr.ib(default=None)
    currency = attr.ib(default=None)
    country_data = attr.ib(default=None)
    region_id = attr.ib(default=None)
    orders_structure = attr.ib(default=None)
    card = attr.ib(default=None)
    error = attr.ib(default=None)
    # Два описательных поля. Отличие в том что если есть descr, то строка параметризации состоит только из него
    # Если есть comment, то он добавляется в конце сформированной по параметрам строке параметризации
    descr = attr.ib(default=None)
    comment = attr.ib(default=None)

    def new(self, **kwargs):
        new_inst = copy.copy(self)
        for attr_name, attr_value in kwargs.iteritems():
            setattr(new_inst, attr_name, attr_value)
        return new_inst

    @staticmethod
    def ids_service(val):
        if isinstance(val, Services.Service):
            return 'service={}'.format(val)
        elif val.service:
            return 'service={}'.format(val.service)
        return ''

    @staticmethod
    def ids_paymethod(val):
        from simpleapi.common.payment_methods import BasePaymethod

        if isinstance(val, BasePaymethod):
            return 'paymethod={}'.format(val.title)
        elif val.paymethod:
            return 'paymethod={}'.format(val.paymethod.title)
        return ''

    @staticmethod
    def ids_currency(val):
        if val.currency:
            return 'currency={}'.format(val.currency)
        elif val.country_data:
            return 'currency={}'.format(val.country_data.get('currency'))
        return ''

    @staticmethod
    def ids_region_id(val):
        if isinstance(val, (int, basestring)):
            return 'region_id={}'.format(val)
        elif val.region_id:
            return 'region_id={}'.format(val.region_id)
        elif val.country_data:
            return 'region_id={}'.format(val.country_data.get('region_id'))
        return ''

    @staticmethod
    def ids_orders(val):
        if not val:
            return "with_product_id"
        elif isinstance(val, (list, tuple)):
            return "{}_order(s)".format(len(val))
        elif val.orders_structure:
            return "{}_order(s)".format(len(val.orders_structure))
        return ''

    @staticmethod
    def ids_clearing_plan(val):
        if hasattr(val, 'clearing_plan') and val.clearing_plan:
            return "clearing_plan={}".format(val.clearing_plan)
        return ''

    @staticmethod
    def ids_user(val):
        from simpleapi.data.uids_pool import anonymous, autoremove

        if not val.user:
            return ''
        if val.user == anonymous:
            return 'anonymous_user'
        if val.user == autoremove:
            return 'new_user'
        return ''

    @staticmethod
    def ids_user_type(val):
        from simpleapi.data.uids_pool import User, Type

        entity = val
        if not isinstance(val, Type):
            entity = val.user_type

        if not entity:
            return ''
        elif isinstance(entity, User):
            return DataObject.ids_user(val)
        elif entity.anonymous:
            return 'anonymous_user'
        elif entity.phonish:
            return 'phonish_user'
        return ''

    @staticmethod
    def ids_card_length(val):
        return "{}-valued_card".format(val)

    @staticmethod
    def ids_card(val):
        return val.get('descr')

    @staticmethod
    def ids_custom(label):
        def ids(val):
            return '{}={}'.format(label, val)

        return ids

    @staticmethod
    def ids_product_name(val):
        return val['name']

    @staticmethod
    def ids_error(val):
        return "error='{}'".format(val.error['status_desc'])

    @staticmethod
    def ids_comment(val):
        if val.comment:
            return val.comment
        return ''

    @staticmethod
    def ids(val):
        if val.descr:
            return val.descr
        else:
            result = DataObject.ids_service(val) + '-' + \
                     DataObject.ids_paymethod(val) + '-' + \
                     DataObject.ids_currency(val) + '-' + \
                     DataObject.ids_region_id(val) + '-' + \
                     DataObject.ids_orders(val) + '-' + \
                     DataObject.ids_clearing_plan(val) + '-' + \
                     DataObject.ids_user_type(val) + '-' + \
                     DataObject.ids_comment(val)

            while '--' in result:
                result = result.replace('--', '-')

            if result.startswith('-'):
                result = result[1:]
            if result.endswith('-'):
                result = result[:-1]

            return result

    def astuple(self, keys):
        return tuple(self.__dict__.get(key) for key in keys if self.__dict__.get(key))


def apply_discount(apply_to, discount_pct=None, discount_part=None):
    if isinstance(discount_pct, (list, tuple)):
        for discount in discount_pct:
            discount_part = discount / 100
            apply_to *= (1 - discount_part)
        return apply_to
    else:
        discount_part = discount_part or discount_pct / 100
        return apply_to * (1 - discount_part)


def current_scheme_is(scheme):
    return environments.simpleapi_env().name.endswith(scheme)


def get_host_from_trust_url(url):
    # TODO: slppls - очень специфичный метод и в данный момент не используется, мб стоит выпилить
    pattern = re.compile(r'\/{2}(\w*)')
    return pattern.findall(url)[0]


def parse_subs_period(subs_period):
    seconds = 0
    days = 0
    months = 0
    years = 0

    count = int(subs_period[:-1])
    period = subs_period[-1]
    if period == 'S':  # секунды
        seconds = count
    elif period == 'D':  # дни
        days = count
    elif period == 'W':  # недели
        days = count * 7
    elif period == 'M':  # месяцы
        months = count
    elif period == 'Y':  # годы
        years = count

    return seconds, days, months, years


def wait_for_ui(driver, condition, locator, message, waiting_time):
    # slppls: ебаная циклическая зависимость (с)
    from simpleapi.data.defaults import default_waiting_time

    if not waiting_time:
        waiting_time = default_waiting_time

    try:
        with reporter.step(u'Ожидаем выполнения условия {} в UI с элементом {}'.format(condition.__name__, locator)):
            WebDriverWait(driver, waiting_time) \
                .until(condition(locator),
                       message=message.format(waiting_time))
    except TimeoutException as te:
        reporter.attach(u"Скриншот страницы где произошла ошибка", driver.get_screenshot_as_png(),
                        attachment_type=reporter.attachment_type.PNG)
        raise TimeoutException(msg=te.msg, screen=te.screen)


def get_uber_signature(params, headers):
    def sign_data(data, private_key_fname, passphrase='1234'):
        with open(private_key_fname) as f:
            rsakey = RSA.importKey(f.read(), passphrase=passphrase)
        signer = PKCS1_v1_5.new(rsakey)
        digest = SHA256.new(data)
        signature = signer.sign(digest)
        encoded_sign = b64encode(signature)
        return encoded_sign

    def pack_params(params):
        return u';'.join(
            u'%s=%s' % (k, v) for k, v in
            sorted((k.lower(), v) for k, v in params.items()))

    def get_data_for_signing(headers, params):
        return (u'%s;;%s' % (pack_params(headers),
                             pack_params(params))).encode('utf-8')

    params_cleared = {k: v for k, v in params.items() if k not in ('card_number', 'cvn',
                                                                   'expiration_year', 'expiration_month')}
    return sign_data(data=get_data_for_signing(headers, params_cleared),
                     private_key_fname=butils.project_file('simpleapi/resources/private.pem'))


class BindingV2Cipher(object):
    @staticmethod
    def read_private_key(private_key_fname, passphrase=None):
        with open(private_key_fname) as f:
            rsa_key = RSA.importKey(f.read(), passphrase=passphrase)
        return rsa_key

    @staticmethod
    def read_public_key(public_key_fname):
        with open(public_key_fname) as f:
            rsa_key = RSA.importKey(f.read())
        return rsa_key

    @staticmethod
    def decrypt_data(encoded_ciphertext, private_keys, passphrase=None):
        with reporter.step(u"Расшифровываемое сообщение: {}".format(encoded_ciphertext)):
            if not isinstance(private_keys, (list, tuple)):
                private_keys = [private_keys]

            for private_key in private_keys:
                rsa_key = BindingV2Cipher.read_private_key(private_key, passphrase=passphrase)
                # MGF1 is used by default as a mask generation function
                cipher = Cipher.new(key=rsa_key,
                                    hashAlgo=SHA512)
                try:
                    message = cipher.decrypt(b64decode(encoded_ciphertext))
                    return message
                except ValueError:
                    pass
            raise ValueError("Incorrect decryption")

    @staticmethod
    def encrypt_data(message, public_key):
        with reporter.step(u"Шифруемое сообщение: {}".format(message)):
            if isinstance(message, six.text_type):
                message = message.encode('utf-8')

            rsa_key = BindingV2Cipher.read_public_key(public_key)
            cipher = Cipher.new(key=rsa_key,
                                hashAlgo=SHA512)
            ciphertext = cipher.encrypt(message)
            return b64encode(ciphertext)


def encrypt_data_by_service(message, service):
    with reporter.step(u"Шифруем сообщение для сервиса {}".format(service.id)):
        service_fname = '{}-public.pem'.format(service.id)
        public_key_fname = butils.project_file('simpleapi/data/keys/{}'.format(service_fname))
        return BindingV2Cipher.encrypt_data(message, public_key_fname)


def decrypt_data_by_service(message, service, passphrase='1234'):
    with reporter.step(u"Расшифровываем сообщение для сервиса {}".format(service.id)):
        service_fname = '{}-private.pem'.format(service.id)
        private_key_fname = butils.project_file('simpleapi/data/keys/{}'.format(service_fname))
        return BindingV2Cipher.decrypt_data(message, private_key_fname, passphrase)
