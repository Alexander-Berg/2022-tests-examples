# coding: utf-8

__author__ = 'fellow'

import calendar
import collections
import copy
import ctypes
import datetime
import decimal
import difflib
import functools
import json
import hashlib
import hmac
import inspect
import logging
import math
import numbers
import os
import pickle
import pkgutil
import random
import re
import shutil
import socket
import ssl
import string
import sys
import time
import urlparse
import warnings
import xmlrpclib
from contextlib import contextmanager
from csv import DictReader
from decimal import Decimal
from io import StringIO

import _pytest.mark
import boto
import hamcrest
import paramiko
import portalocker
import pytz
import requests
import selenium.webdriver as webdriver
import six
from boto.exception import S3ResponseError
from boto.s3.key import Key
from dateutil.relativedelta import relativedelta
from dateutil.tz import gettz
from deepdiff import DeepDiff
from hamcrest import *
from hamcrest.core.matcher import Matcher
from hamcrest.core.string_description import StringDescription
from lxml import etree
from selenium.common.exceptions import WebDriverException
from selenium.webdriver.common.by import By
from selenium.webdriver.remote.remote_connection import LOGGER as webdriver_logger
from tenacity import retry, retry_if_result, stop_after_attempt, RetryError

from btestlib import config

DROUND_PRECISION = 5


# a-vasin: декоратор для разового вычисления функции
# и дальнейшнего использования полученного значения
def cached(function):
    def wrapper(*args, **kwargs):
        if hasattr(wrapper, 'value'):
            return wrapper.value
        value = function(*args, **kwargs)
        wrapper.value = value
        return value

    return wrapper


class DecimalEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, decimal.Decimal):
            return str(o)
        return super(DecimalEncoder, self).default(o)


def dict_hash(d):
    dhash = hashlib.md5()
    encoded = json.dumps(d, cls=DecimalEncoder, sort_keys=True).encode()
    dhash.update(encoded)
    return dhash.hexdigest()


def handle_unhashable(item):
    if isinstance(item, dict):
        return dict_hash(item)
    from balance.balance_objects import Context
    if isinstance(item, Context):
        return item.name
    return item


# Кеширует ответ функции или метода для каждого набора переданных параметров.
# Параметры функции должны быть immutable, либо вы должны понимать -
# как закешируются mutable параметры в кортеже, и будет ли валидным этот кеш.
def memoize(func):
    cache = {}

    def wrapper(*args, **kwargs):
        kw_keys = kwargs.keys()
        kw_keys.sort()

        args_part = tuple(handle_unhashable(i) for i in args)
        kw_keys = tuple(kw_keys)
        kw_values = tuple(handle_unhashable(kwargs[k]) for k in kw_keys)
        key = (args_part, kw_keys, kw_values)

        if key in cache:
            return cache[key]
        else:
            res = func(*args, **kwargs)
            cache[key] = res
        return res

    return wrapper


# a-vasin: декоратор для вычисления времен
def measure_time(_test_name=None, _func_name=None):
    def decorator(func):
        def wrapper(*args, **kwargs):
            if is_local_launch():
                return func(*args, **kwargs)

            test_name = _test_name
            func_name = _func_name

            if test_name is None:
                if not hasattr(sys, '_running_test_name'):
                    return func(*args, **kwargs)

                test_name = sys._running_test_name

            if func_name is None:
                func_name = func.__name__

            before = time.time()
            result = func(*args, **kwargs)
            diff = time.time() - before

            from balance.tests.conftest import TestStatsCollector
            if 'runtime' not in TestStatsCollector.TESTS_STATS['tests'][test_name]:
                TestStatsCollector.TESTS_STATS['tests'][test_name]['runtime'] = collections.defaultdict(list)
            TestStatsCollector.TESTS_STATS['tests'][test_name]['runtime'][func_name].append(diff)

            return result

        return wrapper

    return decorator


def make_build_unique_key(key_prefix, build_number=None, project_name=None, buildconf_name=None,
                          additional_info_required=True):
    if project_name is None:
        project_name = config.TEAMCITY_PROJECT_NAME

    if buildconf_name is None:
        buildconf_name = config.TEAMCITY_BUILDCONF_NAME

    if build_number is None:
        build_number = config.BUILD_NUMBER

    key = "{}_{}_{}_{}".format(key_prefix, project_name, buildconf_name, build_number) if additional_info_required \
        else "{}".format(key_prefix)
    return key


def save_slave_value(key, value):
    file_name = project_file(str(key) + '_' + str(os.getpid()))
    with open(file_name, 'w+') as f:
        f.write(pickle.dumps(value))


def collect_slave_values(key):
    result = {}
    for filename in os.listdir(project_dir()):
        if not filename.startswith(key + '_'):
            continue
        with open(project_file(filename), 'r') as f:
            result[filename.split('_')[-1]] = pickle.loads(f.read())
    return result


class aDict(dict):
    def __init__(self, *args, **kwargs):
        self.update(*args, **kwargs)

    def __getattr__(self, attr):
        try:
            return self[attr]
        except KeyError:
            raise AttributeError

    def __dir__(self):
        return self.keys() + dir(self.__class__)


@cached
def s3storage():
    return S3Storage(bucket_name="balance-autotesting")


@cached
def s3storage_cache():
    return S3Storage(bucket_name="balance-autotest-cache")


@cached
def s3storage_pagediff():
    return S3Storage(bucket_name="balance-autotesign-pagediff")


@cached
def s3storage_stats():
    return S3Storage(bucket_name="balance-autotesting-statistics")


def current_time_milliseconds():
    return int(round(time.time() * 1000))


def random_string(size=10, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


def generate_string_id():
    return "{}-{}-{}".format(current_time_milliseconds(), random_string(), os.getpid())


def single_line(text):
    return ' '.join(text.split())


# тоже самое можно делать с помощью https://github.com/jd/tenacity
def try_to_execute(executable, description="execute", retry_limit=5, is_exception_to_retry=lambda exc: True,
                   raise_on_limit_exceeded=False):
    import btestlib.reporter as reporter
    tries = 0
    while tries < retry_limit:
        try:
            reporter.log("Try to {}".format(description))
            return executable()
        except Exception as exc:
            if is_exception_to_retry(exc):
                tries += 1
                if tries == retry_limit and raise_on_limit_exceeded:
                    raise exc
                reporter.log("Failed to {}. Tries: {}".format(description, tries))
                reporter.log("Exception: {}".format(exc))
            else:
                raise exc
    reporter.log("Failed to {}. Retry limit exceeded".format(description))


def dround(number, decimal_places=DROUND_PRECISION, rounding=None):
    if isinstance(number, numbers.Number):
        number = str(number)
    return Decimal(number).quantize(Decimal(10) ** -decimal_places, rounding=rounding)


def dround2(number, rounding=decimal.ROUND_HALF_UP):
    return dround(number, decimal_places=2, rounding=rounding)


# blubimov а это не тоже самое что dround2 ?
def round00(value, round=decimal.ROUND_HALF_UP):
    if not isinstance(value, decimal.Decimal):
        value = decimal.Decimal(value)
    return value.quantize(decimal.Decimal('0.01'), round)


# (1234.123, 2)  -> 1234.13
# (1234.123, -2) -> 1300
def roundup(number, ndigits=0):
    # type: () -> Decimal
    if isinstance(number, numbers.Number):
        number = str(number)
    p = Decimal('10') ** -ndigits
    return int(math.ceil(Decimal(number) / p)) * p


def pct_sum(sum, pct):
    return Decimal(str(sum)) * Decimal(str(pct)) / Decimal('100')


# blubimov: а что значит int_percent ? у нас бывают и не целые налоги!
def fraction_from_percent(int_percent, offset=Decimal('1')):
    return offset + int_percent / Decimal('100')


def get_sum_with_nds(sum, nds):
    assert sum is not None, 'sum is None'
    assert nds is not None, 'nds is None'
    return dround2(Decimal(str(sum)) * fraction_from_percent(Decimal(str(nds))))


def get_sum_wo_nds(sum, nds):
    return dround2(Decimal(str(sum)) / fraction_from_percent(Decimal(str(nds))))


def get_nds_amount(sum, nds):
    return dround2(Decimal(str(sum)) - get_sum_wo_nds(sum, nds))


def generate_alfanumeric_string(length):
    return ''.join([random.choice(string.ascii_letters + string.digits) for _ in xrange(length)])


def generate_numeric_string(length):
    return ''.join([random.choice(string.digits) for _ in xrange(length)])


def remove_empty(params):
    return {k: v for k, v in params.items() if v is not None}


def remove_false(dict_or_list):
    if isinstance(dict_or_list, dict):
        return {k: v for k, v in dict_or_list.iteritems() if bool(v)}
    else:
        return [value for value in dict_or_list if bool(value)]


def replace_none(dict_, to_value=''):
    return {k: v if v is not None else to_value for k, v in dict_.items()}


def keys_to_lowercase(dict_):
    return {k.lower(): v for k, v in dict_.items()}


def add_key_prefix(data, prefix):
    if isinstance(data, dict):
        return {'{}{}'.format(prefix, key): data[key] for key in data.keys()}
    else:
        return {prefix + 'values': data}


def round_dict_string_fields(dictionary, fields):
    for field in fields:
        if dictionary[field]:
            dictionary[field] = unicode(dround(Decimal(dictionary[field]), DROUND_PRECISION))


# если ключи пересекаются, то значения из последующих словарей переопределяют значения из предыдущих
# (порядок обработки словарей менять не нужно!)
def merge_dicts(dicts, check_conflicts=False):
    result = {}
    for dict_ in dicts:
        if dict_:
            if check_conflicts:
                check_condition(set(result.keys()).intersection(set(dict_.keys())), empty(),
                                error=u"Словари имеют пересекающийся набор ключей")
            result.update(dict_)
    return result


# возвращаем элементы словаря first у которых ключ или значение(по совпадающим ключам)
# не совпадает с элементами словаря second
def dicts_diff(first_dict, second_dict):
    return {k: first_dict[k] for k in first_dict if not (k in second_dict and second_dict[k] == first_dict[k])}


def columns(left, right, width=70):
    return ('{:<' + str(width) + '} | {}').format(left, right)


def pair(label, value, separator='; '):
    return '{}{}{}'.format(label, separator, value)


def get_url_parameter(url, param):
    parsed = urlparse.parse_qs(urlparse.urlparse(url).query)
    return parsed[param]


def get_index(iterable, index, default=None):
    try:
        return iterable[index]
    except IndexError:
        return default


def fill(alist, to_len):
    if len(alist) > to_len:
        raise ValueError(u"Can't fill a list to len smaller than its own")
    return alist * (to_len // len(alist) or 1) + alist[:(to_len % len(alist))]


def flatten(container):
    # for item in container:
    #     if isinstance(item, collections.Iterable) and not isinstance(item, (str, bytes, unicode)):
    #         for element in flatten(item):
    #             yield element
    #     else:
    #         yield item
    container = [sublist if isinstance(sublist, collections.Iterable) and not isinstance(sublist, (str, bytes, unicode))
                 else [sublist]
                 for sublist in container]
    return list([item for sublist in container for item in list(sublist)])


def divide(condition, lst):
    passes = []
    failes = []
    for element in lst:
        if condition(element):
            passes.append(element)
        else:
            failes.append(element)
    return passes, failes


def change_newlines_to_win(file_path):
    with open(file_path, 'r+') as f:
        text = f.read()
        if not '\r\n' in text:
            f.seek(0)
            f.write(text.replace('\n', '\r\n'))
            f.truncate()


# Todo Куда-нибудь перенести
def log_response(func):
    import btestlib.reporter as reporter

    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        response = func(*args, **kwargs)
        if isinstance(response, dict):
            reporter.log('Response:')
            for key, value in response.iteritems():
                reporter.log('\t%s: %s', key, value)
        else:
            reporter.log('Response: %s', repr(response))
        return response

    return wrapper


def currency_to_iso(currency):
    return 'RUB' if currency == 'RUR' else currency


# a-vasin: пытается каждое значение из dictionary кастануть к типу значения с тем же ключем в reference_dictionary
# NB: каждый ненайденный ключ просто копируется без каста
def cast_dictionary(dictionary, reference_dictionary):
    result_dictionary = copy.deepcopy(dictionary)

    if dictionary is None or reference_dictionary is None:
        return result_dictionary

    for key, item in dictionary.iteritems():
        # a-vasin: кривовато, но случай пустых строк надо обработать =(
        if key not in reference_dictionary:
            continue

        if item == '' and isinstance(reference_dictionary[key], unicode) \
                or item == u'' and isinstance(reference_dictionary[key], str):
            result_dictionary[key] = type(reference_dictionary[key])(item)
            continue

        if item is not None \
                and reference_dictionary[key] is not None and item != '' and item != u'' \
                and not isinstance(reference_dictionary[key], Matcher) \
                and not isinstance(item, type(reference_dictionary[key])):
            try:
                if isinstance(reference_dictionary[key], Decimal) and isinstance(item, numbers.Number):
                    item = str(item)
                # if type(reference_dictionary[key]) == unicode:
                result_dictionary[key] = type(reference_dictionary[key])(item)
            except UnicodeDecodeError:
                result_dictionary[key] = decode_obj(item)

    return result_dictionary


def deep_diff_compare(actual, expected):
    deep_diff = DeepDiff(expected, actual)

    missing_keys = deep_diff.get('dictionary_item_removed', set())
    missing_keys.update(set(deep_diff.get('iterable_item_removed', {}).keys()))
    missing_keys.update(set(deep_diff.get('set_item_removed', [])))
    missing_keys.update(set(deep_diff.get('attribute_removed', [])))

    extra_keys = deep_diff.get('dictionary_item_added', set())
    extra_keys.update(set(deep_diff.get('iterable_item_added', {}).keys()))
    extra_keys.update(set(deep_diff.get('set_item_added', [])))
    extra_keys.update(set(deep_diff.get('attribute_added', [])))

    different_values = deep_diff.get('values_changed', {})
    different_values.update(deep_diff.get('type_changes', {}))

    return missing_keys, extra_keys, different_values


def copy_and_update_dict(dictionary, *args, **kwargs):
    new_dictionary = copy.deepcopy(dictionary)

    for arg in args:
        new_dictionary.update(arg)

    new_dictionary.update(kwargs)

    return new_dictionary


def csv_data_to_dict_list(csv_data, delimiter=','):
    decoded_csv_data = csv_data if isinstance(csv_data, unicode) else csv_data.decode()
    csv_reader = DictReader(UTF8EncodedStringIO(decoded_csv_data), delimiter=delimiter)
    return [{key: value.decode('utf-8') for key, value in row.iteritems()} for row in csv_reader]


def compare_texts_line_by_line(text, reference_text):
    from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
    with reporter.step(u"Сравниваем строку с эталоном"):
        reporter.attach(u'Эталон', Presenter.pretty(text))
        reporter.attach(u'Строка', Presenter.pretty(reference_text))

        text_lines = reference_text.splitlines()
        reference_text_lines = text.splitlines()

        diff = [line for line in difflib.ndiff(reference_text_lines, text_lines) if
                line.startswith('+ ') or line.startswith('- ') or line.startswith('? ')]

        reporter.attach(u'Различия в строках', Presenter.pretty(diff))

        return diff


def corresponding_substring(substring, string_, corresponding_string):
    if substring not in string_:
        return ''

    pattern = '(\\1)'.join([re.escape(part) for part in string_.split(substring)])
    pattern = pattern.replace('(\\1)', '(.*)', 1)
    s = re.match(pattern, corresponding_string)
    return s.group(1) if s else ''


# выполняем проверку и переводим тест в Failed в случае ошибки
def check_that(value, matcher, step=u'Выполняем проверку', error=u''):
    from btestlib import reporter
    with reporter.step(step):
        reporter.attach(u'Полученное и ожидаемое значения',
                        u'{}\n\n{}'.format(Presenter.pretty(value), Presenter.matcher(matcher)))
        hamcrest.assert_that(value, matcher, error)


# выполняем проверку и переводим тест в Broken в случае ошибки
def check_condition(value, matcher, error=u'Значение не удовлетворяет условию'):
    try:
        hamcrest.assert_that(value, matcher, error)
    except AssertionError as e:
        raise TestsError(e)


# если атрибут не передан при инициализации и для него ничего нет в default_values, то он получает значение None
def namedtuple_with_defaults(typename, field_names, default_values=()):
    T = collections.namedtuple(typename, field_names)
    T.__new__.__defaults__ = (None,) * len(T._fields)
    if isinstance(default_values, collections.Mapping):
        prototype = T(**default_values)
    else:
        prototype = T(*default_values)
    T.__new__.__defaults__ = tuple(prototype)
    return T


def arguments_count(func):
    return func.func_code.co_argcount


class Context(object):
    @staticmethod
    @cached
    def header():
        if pkgutil.find_loader('btestlib.local_config'):
            import btestlib.local_config as cfg

            if hasattr(cfg, 'XMLRPC_COVERAGE'):
                return cfg.XMLRPC_COVERAGE

        return 'testcontext'

    def __enter__(self):
        import balance.balance_api as api
        api.coverage().server.Coverage.Collect(Context.header(), True)
        return self

    def __exit__(self, exc_type=None, exc_value=None, traceback=None):
        import balance.balance_api as api
        self.value = api.coverage().server.Coverage.Collect(Context.header(), True)

    def __repr__(self):
        return str(self.value)


def DictDifference(current_dict_post_query, past_dict_contract_type):
    from btestlib.data import contract_defaults
    items_by_type = contract_defaults.get_contract_template_by_name()
    source = items_by_type[past_dict_contract_type]
    source_tmp = urlparse.parse_qsl(source, True)
    past_dict_contract = {key: value.decode('utf-8') for (key, value) in source_tmp}

    source_tmp = urlparse.parse_qsl(current_dict_post_query, True)
    current_dict = {key: value.decode('utf-8') for (key, value) in source_tmp}

    set_current, set_past = set(current_dict.keys()), set(past_dict_contract.keys())
    intersect = set_current.intersection(set_past)

    added = {key: current_dict[key] for key in (set_current - intersect)}
    removed = {key: past_dict_contract[key] for key in (set_past - intersect)}
    changed = {key: [current_dict[key], past_dict_contract[key]] for key in
               set(o for o in intersect if past_dict_contract[o] != current_dict[o])}
    unchanged = set(o for o in intersect if past_dict_contract[o] == current_dict[o])
    return {'added': added, 'removed': removed, 'unchanged': unchanged, 'changed': changed}


def add_months_to_date(base_date, months):
    warnings.warn("deprecated - use utils.Date.shift_date() instead", DeprecationWarning)
    a_months = months
    if abs(a_months) > 11:
        s = a_months // abs(a_months)
        div, mod = divmod(a_months * s, 12)
        a_months = mod * s
        a_years = div * s
    else:
        a_years = 0

    year = base_date.year + a_years

    month = base_date.month + a_months
    if month > 12:
        year += 1
        month -= 12
    elif month < 1:
        year -= 1
        month += 12
    day = min(calendar.monthrange(year, month)[1],
              base_date.day)
    return datetime.datetime.combine(datetime.date(year, month, day), base_date.time())


def is_local_launch():
    return config.TEAMCITY_VERSION is None or config.TEAMCITY_VERSION == 'LOCAL'


def is_inside_test():
    return hasattr(sys, '_running_test_name')


# https://stackoverflow.com/questions/36141349/pytest-configure-upfront-before-running-in-parallel-with-xdist
def is_master_node(config):
    """True if the code running the given pytest.config object is running in a xdist master
    node or not running xdist at all.
    """
    return not hasattr(config, 'slaveinput')


def project_dir(relative_path=None):
    above_root = os.path.realpath(__file__).split('btestlib')[0]
    if relative_path:
        return os.path.join(above_root, os.path.normpath(relative_path))
    else:
        return above_root


def project_file(relative_path):
    return os.path.join(project_dir(), os.path.normpath(relative_path))


def merge_dirs(firstdir, seconddir, dstdir=None, check_conflicts=True):
    # todo-igogor если директорий не существует я предчувствую беду, особенно dstdir
    dstdir = dstdir or seconddir

    firstdir_files = set(os.listdir(firstdir))
    seconddir_files = set(os.listdir(seconddir))
    if check_conflicts and firstdir_files.intersection(seconddir_files):
        raise TestsError("Cannot merge directories {} {}, has same files".format(firstdir, seconddir))

    for filename in firstdir_files:
        shutil.copy(os.path.join(firstdir, filename), os.path.join(dstdir, filename))
    if dstdir != seconddir:
        for filename in seconddir_files:
            shutil.copy(os.path.join(seconddir, filename), os.path.join(dstdir, filename))

    pass


def generate_hmac(msg):
    from btestlib.secrets import get_secret, MuzzleSecret

    key = get_secret(*MuzzleSecret.HMAC_SECRET)
    hmac_ = hmac.new(str(key), msg, digestmod=hashlib.sha512)
    return hmac_.hexdigest()


def get_secret_key(passport_id, yandex_uid='', days=None):
    if days is None:
        days = int(time.time()) / 86400

    base = '{passport_id}:{yandex_uid}:{days}'
    params = {
        'passport_id': passport_id or '0',
        'yandex_uid': yandex_uid,
        'days': days,
    }

    s = base.format(**params)
    hex_digest = generate_hmac(s)

    return '{flag}{hex_digest}'.format(
        flag='u' if passport_id else 'y',
        hex_digest=hex_digest,
    )


def encode_obj(in_obj):
    def encode_list(in_list):
        return [encode_obj(el) for el in in_list]

    if isinstance(in_obj, unicode):
        return in_obj.encode('utf-8')
    elif isinstance(in_obj, (list, tuple, set)):
        return type(in_obj)(encode_list(in_obj))
    elif isinstance(in_obj, dict):
        return dict(zip(in_obj.keys(), encode_list(in_obj.values())))

    return in_obj


def decode_obj(in_obj, errors='strict'):
    def decode_list(in_list):
        return [decode_obj(el) for el in in_list]

    if isinstance(in_obj, str):
        return in_obj.decode('utf-8', errors=errors)
    elif isinstance(in_obj, (list, tuple, set)):
        return type(in_obj)(decode_list(in_obj))
    elif isinstance(in_obj, dict):
        return dict(zip(in_obj.keys(), decode_list(in_obj.values())))

    return in_obj


def call_http(session, url, params=None, custom_headers=None, method='POST'):
    import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость
    if params:
        params = replace_none(params)

    if method == 'POST':
        data, params = params, None
    else:
        data = None

    req = requests.Request(method, url, data=data, params=params, headers=custom_headers)
    prepared_request = session.prepare_request(req)

    with reporter.step(u'{}-запрос: {}'.format(method, prepared_request.path_url)):
        # вообще нам нужны prepared_request.headers, но для краткости пока логируем только custom_headers
        reporter.report_http_call(method, prepared_request.url, custom_headers, data)

        response = session.send(prepared_request, verify=False)

        reporter.attach(u'Ответ: Код: {}, Урл: {}\n'.format(response.status_code, response.url))

        check_errors_absence_in_http_response(response.content)

        return response


# проверка html на ошибки аналогично balance_web.check_errors_absence_on_page
def check_errors_absence_in_http_response(html_text):
    tree = etree.HTML(html_text)
    _check_http_error_absence(tree)
    _check_balance_error_absence(tree)


def _check_http_error_absence(html_tree):
    from btestlib import reporter
    nginx_errors = html_tree.xpath(Web.HttpError.HTTP_ERROR_BLOCK[1])
    if nginx_errors:
        reporter.attach(u'Страница с ошибкой', etree.tostring(html_tree), reporter.allure.attachment_type.HTML)
        Web.HttpError.raise_error(u'В ответе http-запроса присутствует http-ошибка', html_tree.xpath("//title")[0].text)


def _check_balance_error_absence(html_tree):
    from btestlib import reporter
    balance_errors = html_tree.xpath(Web.BalanceError.ERROR_BLOCK[1])
    if balance_errors:
        is_timeout = Web.BalanceError.is_timeout(balance_errors[0].text)
        error_code = balance_errors[0].attrib.get('name')
        error_messages = html_tree.xpath(Web.BalanceError.ERROR_MSG_BLOCK[1])
        error_msg = error_messages[0].attrib.get('value') if error_messages else None
        tracebacks = html_tree.xpath(Web.BalanceError.TRACEBACK_BLOCK[1])
        traceback = tracebacks[0].text if tracebacks else None
        reporter.attach(u'Страница с ошибкой', etree.tostring(balance_errors[0]), reporter.allure.attachment_type.HTML)
        Web.BalanceError.raise_error(u'В ответе http-запроса присутствует ошибка баланса',
                                     is_timeout, error_code, error_msg, traceback)


@contextmanager
def empty_context_manager():
    yield


class ConstantsContainer(object):
    """
    Преимущества перед использованием енумов:
        в качестве констант можно использовать объекты любого типа
        класс констант можно определить прямо внутри контейнера
        можно динамически формировать/изменять набор
        можно создать новый объект того же типа что и константа вне набора
        меньше бойлерплейта - не надо писать __init__ - и в целом короче
    В отличие от енумов нельзя использовать сравнение через is, т.к. не синглтоны, что по-моему плюс
    """
    # эта переменная обязательно должна быть заполнена в классе наследнике
    constant_type = None

    @classmethod
    def values(cls):
        return cls.constants_dict().values()

    @classmethod
    def items(cls):
        return cls.constants_dict().items()

    @classmethod
    def constants_dict(cls):
        if not cls.constant_type:
            raise TestsError('constant_type class field is not specified in {}'.format(cls))
        return {name: value for name, value in cls.__dict__.iteritems() if
                isinstance(value, cls.constant_type) and name[0] != '_'}

    # Получение имени константы в нашем контейнере почти также удобно как в енуме
    # (предполагаем что у нас нет констант с одинаковыми значениями)
    @classmethod
    def name(cls, constant):
        vals = cls.constants_dict().values()
        return cls.constants_dict().keys()[vals.index(constant)] if constant in vals else 'NONAME'


class TestsError(Exception):
    pass


class ServiceError(AssertionError):
    pass


class UTF8EncodedStringIO(StringIO):
    def next(self):
        return super(UTF8EncodedStringIO, self).next().encode('utf-8')


class Date(object):
    dt_delta = datetime.timedelta

    @staticmethod
    def moscow_offset_dt(dt=None):
        if not dt:
            dt = datetime.datetime.now()
        return pytz.timezone('Europe/Moscow').localize(dt)

    @staticmethod
    def first_day_of_month(dt=None):
        if not dt:
            dt = datetime.datetime.now()
        return Date.nullify_time_of_date(dt.replace(day=1))

    @staticmethod
    def last_day_of_month(dt=None):
        if not dt:
            dt = datetime.datetime.now()
        return Date.first_day_of_month(dt) + relativedelta(months=1, days=-1)

    @staticmethod
    def yesterday():
        return datetime.datetime.now() - datetime.timedelta(days=1)

    @staticmethod
    def get_last_day_of_previous_month():
        return Date.nullify_time_of_date(datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1))

    @staticmethod
    def current_month_last_working_day():
        # type: () -> datetime.date
        return Date.last_working_day(Date.last_day_of_month())

    # рабочий день предшествующий дате dt или dt, если это рабочий день
    @staticmethod
    def last_working_day(dt=None):
        # type: (datetime.datetime) -> datetime.date
        from balance import balance_api
        if not dt:
            dt = datetime.datetime.now()
        cal = balance_api.medium().GetWorkingCalendar(dt.year)
        for cal_day in reversed(cal):
            cal_date = datetime.datetime.strptime(cal_day['dt'], '%Y-%m-%d').date()
            if cal_date <= dt.date() and \
                    cal_day['calendar_day'] == 1 and cal_day['five_day'] == 1:
                return cal_date
        return Date.last_working_day(Date.shift_date(dt.replace(month=1, day=1), days=-1))

    @staticmethod
    def date_to_iso_format(dt, pass_none=False):
        if pass_none and dt is None:
            return None
        elif isinstance(dt, (str, unicode)):
            return Date.validate_iso(dt)
        else:
            dt_wo_microseconds = dt.replace(microsecond=0)
            return dt_wo_microseconds.isoformat()

    @staticmethod
    def validate_iso(date_text):
        try:
            datetime.datetime.strptime(date_text, '%Y-%m-%d')
            return date_text
        except ValueError as e:
            raise TestsError("Invalid ISO date string, should be YYYY-MM-DD. " + e.message)

    @staticmethod
    def from_iso_to_date_format(dt_iso):
        return datetime.datetime.strptime(dt_iso, "%Y-%m-%dT%H:%M:%S")

    to_iso = date_to_iso_format

    @staticmethod
    def date_to_slash_format(date):
        return str(date).replace('-', '/')  # todo может этот формат как-то называется?

    @staticmethod
    def nullify_time_of_date(date):
        return date.replace(hour=0, minute=0, second=0, microsecond=0) if date else None

    @staticmethod
    def set_timezone_of_date(dt, timezone):
        return dt.replace(tzinfo=gettz(timezone))

    @staticmethod
    def nullify_microseconds_of_date(dt):
        return dt.replace(microsecond=0)

    # тут возвращаем дату без времени
    @staticmethod
    def previous_month_first_and_last_days(dt=None):
        if not dt:
            dt = datetime.datetime.today()

        end_dt = datetime.datetime.fromordinal(dt.toordinal()).replace(day=1) \
                 - datetime.timedelta(days=1)
        start_dt = end_dt.replace(day=1)
        return start_dt, end_dt

    # а тут возвращаем дату со временем (хотя лучше бы единообразно с previous_month сделать)
    @staticmethod
    def current_month_first_and_last_days(dt=None):
        if not dt:
            dt = datetime.datetime.today()
        start_dt = dt.replace(day=1)
        year = dt.year
        month = dt.month
        day_of_week, last_day = calendar.monthrange(year, month)
        end_dt = dt.replace(day=last_day)
        return start_dt, end_dt

    @staticmethod
    def next_month_first_and_last_days(dt=None):
        if not dt:
            dt = datetime.datetime.today()
        year = dt.year
        month = dt.month
        day_of_week, last_day = calendar.monthrange(year, month)
        start_dt = dt.replace(day=last_day) + datetime.timedelta(days=1)
        next_month = start_dt.month
        year_of_next_month = start_dt.year
        day_of_week, last_day = calendar.monthrange(year_of_next_month, next_month)
        end_dt = start_dt.replace(day=last_day)
        return start_dt, end_dt

    @staticmethod
    def previous_three_months_start_end_dates(dt=datetime.datetime.today()):
        month3_start_dt, month3_end_dt = Date.previous_month_first_and_last_days(dt)
        month2_start_dt, month2_end_dt = Date.previous_month_first_and_last_days(month3_start_dt)
        month1_start_dt, month1_end_dt = Date.previous_month_first_and_last_days(month2_start_dt)
        return month1_start_dt, month1_end_dt, \
               month2_start_dt, month2_end_dt, \
               month3_start_dt, month3_end_dt

    @staticmethod
    def previous_two_months_dates(dt=datetime.datetime.today()):
        month2_start_dt, month2_end_dt = Date.previous_month_first_and_last_days(dt)
        month1_start_dt, month1_end_dt = Date.previous_month_first_and_last_days(month2_start_dt)
        return month1_start_dt, month1_end_dt, \
               month2_start_dt, month2_end_dt

    @staticmethod
    def get_quarter_borders(dt):
        quarter = int(dt.month // 3.01)
        start = quarter * 3 + 1
        end = quarter * 3 + 3
        start_date = datetime.datetime(dt.year, start, 1)
        end_date = datetime.datetime(dt.year, end, calendar.monthrange(dt.year, end)[1])
        return start_date, end_date

    @staticmethod
    def get_previous_quarter(dt):
        current_quarter = int(dt.month // 3.01)
        current_year = dt.year
        if current_quarter == 0:
            previous_quarter = 3
            year = current_year - 1
        else:
            previous_quarter = current_quarter - 1
            year = current_year
        return Date.get_quarter_borders(datetime.datetime(year, (previous_quarter * 3) + 1, 1))

    @staticmethod
    def get_last_quarter_day(dt):
        current_month = dt.month
        months_shift = (3 - current_month % 3) % 3

        return Date.last_day_of_month(dt + relativedelta(months=months_shift))

    @staticmethod
    def get_first_quarter_day(dt):
        current_month = dt.month
        months_shift = (current_month + 2) % 3

        return Date.first_day_of_month(dt - relativedelta(months=months_shift))

    @staticmethod
    def shift_date(dt=None, years=0, months=0, days=0, hours=0, minutes=0, seconds=0):
        return dt + relativedelta(years=years, months=months, days=days, hours=hours, minutes=minutes, seconds=seconds)

    @staticmethod
    def shift(dt=None, years=0, months=0, days=0, hours=0, minutes=0, seconds=0):
        dt = dt or datetime.datetime.now()
        return dt + relativedelta(years=years, months=months, days=days, hours=hours, minutes=minutes, seconds=seconds)

    @staticmethod
    def get_timestamp(dt):
        return time.mktime(dt.timetuple()) + dt.microsecond / 1E6

    @staticmethod
    def date_period_cutter(from_dt, till_dt):
        DAY_SHIFT = datetime.timedelta(days=1)
        till_dt -= DAY_SHIFT
        start_dt = from_dt
        _, end_dt = Date.current_month_first_and_last_days(from_dt)
        while end_dt < till_dt:
            yield (start_dt, end_dt + DAY_SHIFT)
            start_dt, end_dt = Date.next_month_first_and_last_days(start_dt)
        yield (start_dt, till_dt + DAY_SHIFT)

    @staticmethod
    def holidays(from_dt, till_dt=None):
        # todo-igogor здесь пожалуй надо бы умнее сделать, а то для больших промежутков запаришься вносить.
        _holidays = [
            datetime.datetime(2018, 11, 4),
            datetime.datetime(2019, 1, 1),
            datetime.datetime(2019, 1, 2),
            datetime.datetime(2019, 1, 3),
            datetime.datetime(2019, 1, 4),
            datetime.datetime(2019, 1, 5),
            datetime.datetime(2019, 1, 6),
            datetime.datetime(2019, 1, 7),
            datetime.datetime(2019, 1, 8),
            datetime.datetime(2019, 2, 23),

            datetime.datetime(2019, 3, 8),
            datetime.datetime(2019, 5, 1),
            datetime.datetime(2019, 5, 9),
            datetime.datetime(2019, 6, 12),
            datetime.datetime(2019, 11, 4),
        ]
        till_dt = till_dt or datetime.datetime.now()
        return len([day for day in _holidays if day >= from_dt and day <= till_dt])


class String(object):
    @staticmethod
    def as_is(any_case):
        return any_case

    # todo-igogor для преобразования из других кейсов нужно добавить параметры для других кейсов: и по тому
    # какой (обязательно один) передан передан - понимать из какого конвертим
    @staticmethod
    def to_camel_case(snake_case):
        return ''.join([word.capitalize() if word != 'id' else 'ID' for word in snake_case.split('_')])

    @staticmethod
    def to_upper_underscore(snake_case):
        return '_'.join([word.upper() for word in snake_case.split('_')])

    @staticmethod
    def to_kebab_case(upper_underscore=None, snake_case=None):
        String._only_one_parameter_specified(copy.copy(locals()))

        if upper_underscore:
            return '-'.join([word.lower() for word in upper_underscore.split('_')])
        elif snake_case:
            return String.to_kebab_case(upper_underscore=String.to_upper_underscore(snake_case=snake_case))

    # todo-igogor этому методу здесь не место
    @staticmethod
    def _only_one_parameter_specified(params_dict):
        if len([name for name, value in params_dict.iteritems() if value is not None]) != 1:
            raise TestsError('Method must have one and only one parameter specified')

    @staticmethod
    def unicodify(something):
        if isinstance(something, six.text_type):
            return something
        elif isinstance(something, six.binary_type):
            return something.decode('utf-8', 'replace')
        else:
            try:
                return six.text_type(something)  # @UndefinedVariable
            except (UnicodeEncodeError, UnicodeDecodeError):
                return u'<nonpresentable %s>' % type(something)  # @UndefinedVariable

    @staticmethod
    def fill_newline(text, shift=0, filler=u' '):
        if shift >= 0:
            return text.replace(u'\n', u'\n' + filler * shift)
        else:
            return text.replace(u'\n' + filler * abs(shift), u'\n')

    @staticmethod
    def normalize_whitespaces(text):
        separator = u' ' if isinstance(text, unicode) else ' '
        return separator.join(text.split()).strip()


class ConditionHasNotOccurred(ServiceError):
    def __init__(self, message, last_found_value):
        super(ConditionHasNotOccurred, self).__init__(message)
        self.last_found_value = last_found_value


class PredicateStateIncorrect(ServiceError):
    def __init__(self, message, last_found_value):
        super(PredicateStateIncorrect, self).__init__(message)
        self.last_found_value = last_found_value


class Presenter(object):
    class UnicodeMatcherWrapper(object):
        def __init__(self, matcher):
            if hasattr(matcher, 'matcher'):
                matcher.matcher = Presenter.UnicodeMatcherWrapper(matcher.matcher)

                if hasattr(matcher, 'matchers'):
                    matcher.matchers = [Presenter.UnicodeMatcherWrapper(nested_matcher) for nested_matcher in
                                        matcher.matchers]

            self.matcher = matcher

        def __getattr__(self, item):
            return getattr(self.matcher, item)

        def __str__(self):
            return unicode(StringDescription().append_description_of(self.matcher))

    @staticmethod
    def pretty(obj):
        import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость
        return reporter.pformat(obj)

    @staticmethod
    def cut(str, limit=1500):
        return str[:limit - 3] + u'...' if len(str) > limit else str

    @staticmethod
    def method_name(callable):
        if isinstance(callable, str):
            return callable
        elif isinstance(callable, xmlrpclib._Method):
            return callable._Method__name
        elif hasattr(callable, 'im_class'):
            return callable.im_class.__name__ + '.' + callable.__name__
        elif isinstance(callable, functools.partial):
            return 'Partial ' + callable.func.__name__
        else:
            return callable.__name__

    @staticmethod
    def method_call(callable, args, kwargs):
        return '{}(args={}, kwargs={})'.format(Presenter.method_name(callable), str(args), str(kwargs))

    @staticmethod
    def get_headers_string(headers):
        if headers is None:
            return ''
        prefix = ' -H '
        return prefix + prefix.join(["'{}: {}'".format(key, value) for key, value in headers.iteritems()])

    @staticmethod
    def http_call_curl(url, method, headers=None, data=None, json_data=None, cookies=None):
        if isinstance(data, dict):
            # slppls: превращаем дикт в строку такого же формата, которую использует внутри себя request
            # и который воспроизводится в curl
            from requests.models import RequestEncodingMixin as req
            data = req._encode_params(data)
            # data = json.dumps(data)

        if json_data:
            json_headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }

            if headers:
                headers = copy_and_update_dict(headers, json_headers)
            else:
                headers = json_headers

            data = json_data

        if cookies:
            cookies = ", ".join("=".join(_) for _ in cookies.items())

        headers_str = u'{} '.format(Presenter.get_headers_string(headers)) if headers else u''
        data_str = u" -d '{}'".format(data) if data else u''
        cookies_str = u" -b '{}'".format(cookies) if cookies else u''
        method_str = u"-X {} ".format(method)
        return u'curl {method}{headers}{url}{data}{cookies}'.format(method=method_str, headers=headers_str,
                                                                    url=url, data=data_str, cookies=cookies_str)

    @staticmethod
    def executable_method_call(callable, args, kwargs):
        arg_format = []
        format_params = [Presenter.method_name(callable)]

        if args:
            arg_format.append(u'%s')
            format_params.append(u', '.join([Presenter.pretty(arg) for arg in args]))

        if kwargs:
            arg_format.append(u'%s')
            format_params.append(u', '.join([(unicode(parameter) + u'=' + Presenter.pretty(value))
                                             for parameter, value in kwargs.iteritems()]))

        callable_format = u'%s(' + u', '.join(arg_format) + u')'
        return Presenter.cut(callable_format % tuple(format_params))

    @staticmethod
    def web_locator(by, value):
        return u'By {}: {}'.format(String.unicodify(str(by)), String.unicodify(value))

    @staticmethod
    def matcher(matcher):
        # todo-igogor некоторые матчеры, например equal_to имеют хуевый str
        return unicode(Presenter.UnicodeMatcherWrapper(matcher))

    @staticmethod
    def match_expr(value, matcher):
        return str(value) + ' ' + Presenter.matcher(matcher)

    @staticmethod
    def mongo_query(collection, method, filter):
        return '{}.{}({})'.format(collection, method, Presenter.pretty(filter))

    @staticmethod
    def hex_string_to_ic_id(hex_number_string):
        check_condition(hex_number_string, has_length(32), u'Строка должна иметь длину в 32 символа')
        check_condition(hex_number_string, matches_regexp('[0-9A-F]+'), u'Строка должна быть шестнадцатиричным числом')
        return "{}-{}-{}-{}-{}".format(hex_number_string[:8], hex_number_string[8:12], hex_number_string[12:16],
                                       hex_number_string[16:20], hex_number_string[20:])

    @staticmethod
    def sql_query(query, params):
        # todo-igogor сделать так чтобы запрос и параметры группировались и можно было скопировать - выполнить
        return "{}\n{}".format(query, params if params else '')

        # todo-igogor добавить xmlrpc


class Web(object):
    webdriver_logger.setLevel(logging.WARNING)

    class ReportingDriver(object):

        def __init__(self, driver):
            self._driver = driver
            self.switch_to = Web.ReportingSwitchTo(self._driver.switch_to)

        def __getattr__(self, item):
            return getattr(self._driver, item)

        def get(self, url, name=u''):
            import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость
            with reporter.step(u'Открываем страницу: ' + name):
                reporter.log(u'Открываем url: ' + url)
                reporter.report_url(u'Урл', url)
                return self._driver.get(url)

        # todo-igogor find_element_by_* не попадут в отчет
        def find_element(self, by, value, name=None):
            import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость
            element_name = name or u'{}: {}'.format(by, value)
            with reporter.step(u'Ищем элемент веб страницы: ' + element_name):
                reporter.attach(u'Локатор', Presenter.web_locator(by, value))
                return Web.ReportingElement(self._driver.find_element(by=by, value=value), name=element_name)

        def find_elements(self, by, value, name=None):
            import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость
            element_name = name or '{}: {}'.format(by, value)
            with reporter.step(u'Ищем элементы веб страницы: ' + element_name):
                reporter.attach(u'Локатор', Presenter.web_locator(by, value))

                elements = self._driver.find_elements(by=by, value=value)
                return [Web.ReportingElement(element=elem, name=element_name) for elem in elements]

    class ReportingElement(object):

        def __init__(self, element, name):
            self._element = element
            self.name = name

        def __getattr__(self, item):
            attr = getattr(self._element, item)
            if item in ['click', 'send_keys', 'submit']:
                return Web.ReportingAction(attr, self.name)
            else:
                return attr

    class ReportingAction(object):

        def __init__(self, action, element_name):
            self.action = action
            self.element_name = element_name

        def __call__(self, *args, **kwargs):
            import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость
            with reporter.step(u"Действие над элементом: {} -> {}".format(self.element_name, self.action.__name__)):
                reporter.attach(u'Вызов метода', Presenter.executable_method_call(self.action, args, kwargs))
                self.action(*args, **kwargs)

    class ReportingSwitchTo(object):

        def __init__(self, switch_to):
            self._switch_to = switch_to
            self.alert = Web.ReportingAlert(self._switch_to.alert)

        def __getattr__(self, item):
            return getattr(self._switch_to, item)

        def frame(self, element, name=None):
            from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
            element_name = name or element.name
            with reporter.step(u'Переключаемся во фрейм ' + element_name):
                self._switch_to.frame(element._element)

    class ReportingAlert(object):
        def __init__(self, alert):
            self._alert = alert

        def __getattr__(self, item):
            return getattr(self._alert, item)

        def accept(self, name=None):
            from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
            alert_name = name or self._alert.text
            with reporter.step(u"Подтверждаем алерт '{alert_name}'".format(alert_name=alert_name)):
                self._alert.accept()

    class CustomCapabilities(object):
        class FIREFOX(object):
            name = 'FIREFOX'

            def __init__(self):
                self.local_driver_cls = webdriver.Firefox
                self.remote_capabilities = webdriver.DesiredCapabilities.FIREFOX.copy()

        class CHROME(object):
            name = 'CHROME'

            def __init__(self):
                self.local_driver_cls = webdriver.Chrome
                self.remote_capabilities = webdriver.DesiredCapabilities.CHROME.copy()
                self.remote_capabilities['sessionTimeout'] = '3m'
                self.remote_capabilities['version'] = '80.0'

        class IE(object):
            name = 'Internet Explorer'

            def __init__(self):
                self.local_driver_cls = webdriver.Ie
                self.remote_capabilities = webdriver.DesiredCapabilities.INTERNETEXPLORER.copy()
                self.remote_capabilities['version'] = '10'

        def default(self):
            import os

            capabilities_str = os.environ.get('CAPABILITIES', Web.CustomCapabilities.CHROME.name)

            if capabilities_str == Web.CustomCapabilities.FIREFOX.name:
                return self.FIREFOX()
            elif capabilities_str == Web.CustomCapabilities.CHROME.name:
                return self.CHROME()
            elif capabilities_str == Web.CustomCapabilities.IE.name:
                return self.IE()
            else:
                raise TestsError(u"Not a valid capabilities. Now available only 'FIREFOX', 'CHROME' or 'IE")

    class DriverProvider(object):
        def __init__(self, capabilities=None):
            self.DRIVER = None
            self.capabilities = capabilities

        def _prepare_driver(self):
            # profile = webdriver.FirefoxProfile()
            # profile.set_preference('browser.helperApps.neverAsk.saveToDisk', 'application/force-download')
            # profile.set_preference('browser.helperApps.neverAsk.saveToDisk', 'image/jpeg')
            # profile.set_preference("browser.download.folderList", 2)
            # todo-igogor папку надо иметь возможность кастомизировать
            # profile.set_preference('browser.download.dir', project_dir('balalayka/files'))
            # profile.update_preferences()  # без этой строки не работает с Remote драйвером

            use_capabilities = self.capabilities or Web.CustomCapabilities().default()

            if is_local_launch():
                driver_cls = use_capabilities.local_driver_cls

                def get_chromium_path():
                    if pkgutil.find_loader('btestlib.local_config'):
                        import btestlib.local_config as cfg

                        return cfg.ENVIRONMENT_SETTINGS['chromium_path']

                self.DRIVER = driver_cls(executable_path=get_chromium_path())

            else:
                self.DRIVER = webdriver.Remote(
                    'http://balance:balance@sw.yandex-team.ru:80/v0',
                    desired_capabilities=use_capabilities.remote_capabilities)
                # browser_profile=profile)
            self.DRIVER = Web.ReportingDriver(self.DRIVER)

        def __enter__(self):
            import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость

            self._prepare_driver()
            reporter.log(u'Открываем браузер {browser_name} (version {version})'.format(
                browser_name=self.DRIVER.capabilities.get('browserName', 'Unknown').title(),
                version=self.DRIVER.capabilities.get('version', 'Unknown')))
            return self.DRIVER

        def __exit__(self, exc_type, exc_val, exc_tb):
            import btestlib.reporter as reporter  # todo-igogor ебаная циклическая зависимость
            try:
                if exc_type:
                    if issubclass(exc_type, WebDriverException) and \
                            exc_val.msg and 'was terminated due to TIMEOUT' in exc_val.msg:
                        reporter.attach(u'Ошибка', u'Selenium Grid убил браузер т.к. долго не было действий.')
                        return False

                    reporter.attach(u"Скриншот страницы где произошла ошибка", self.DRIVER.get_screenshot_as_png(),
                                    attachment_type=reporter.allure.attachment_type.PNG)

                    reporter.report_url(u"URL страницы где произошла ошибка", self.DRIVER.current_url)
            finally:
                if self.DRIVER:
                    self.DRIVER.quit()
                return False

    @staticmethod
    def dynamic_locator(by, value_format, name=None):
        # первый параметр нужен только чтобы вызывать как атрибут класса
        def _dynamic_locator(_, **kwargs):
            _name = name.format(**kwargs) if name else name
            return by, value_format.format(**kwargs), _name

        return _dynamic_locator

    @staticmethod
    def check_checkbox(element, driver):
        # todo-igogor сделать чтобы чекбокс чекался/расчекивался в зависимости от параметра
        checkbox_id = element.get_attribute('id')
        # todo-igogor драйвер можно получить из элемента
        driver.execute_script('var checkbox = document.getElementById("{0}"); '
                              'checkbox.checked = true; '
                              '$(checkbox).change();'.format(checkbox_id))

    @staticmethod
    def switch_to_opened_page(driver):
        driver.switch_to_window(driver.window_handles[-1])

    @staticmethod
    def switch_to_alert(driver):
        return driver.switch_to_alert()

    @staticmethod
    def is_element_present(driver, locator):
        return len(driver.find_elements(*locator)) > 0

    @staticmethod
    def is_link(element):
        return element.tag_name == 'a'

    @staticmethod
    def table(locator, column_names, name=u'', headers_as_row=False):
        # первый параметр нужен чтобы обращаться как к атрибуту класса
        def _table(_, driver):
            return Web.Table(driver, locator, column_names, name, headers_as_row)

        return _table

    class Table(object):

        @staticmethod
        def as_values(elements):
            # todo-igogor можно кэшировать значения если будет долго
            return [{key: value.text for key, value in row.iteritems()} for row in elements]

        @staticmethod
        def row_cells(row_element, column_names, cell_xpath="./td"):
            cells = row_element.find_elements(By.XPATH, cell_xpath)
            if cells and len(cells) != len(column_names):
                raise TestsError('Table has more columns than specified')
            return dict(zip(column_names, cells))

        def __init__(self, driver, locator, column_names, name=u'', headers_as_row=False):
            self.driver = driver
            self.locator = locator
            self.column_names = column_names
            self.name = name
            self.headers_as_row = headers_as_row

            self._table_element = None
            self._elements = []
            self._headers = []
            self._values = []

        @property
        def table_element(self):
            if not self._table_element:
                self._table_element = self.driver.find_element(*self.locator)
            return self._table_element

        @property
        def elements(self):
            if not self._elements:
                rows_elements = self.table_element.find_elements(By.XPATH, "./tbody/tr")
                if self.headers_as_row:
                    rows_elements = rows_elements[1:]
                self._elements = [Web.Table.row_cells(row_element, self.column_names) for row_element in rows_elements]
            return self._elements

        @property
        def values(self):
            if not self._values:
                self._values = Web.Table.as_values(self.elements)
            return self._values

        def headers(self, as_elements=True):
            if not self._headers:
                if self.headers_as_row:
                    row_xpath, cell_xpath = "./tbody/tr[1]", "./th"
                else:
                    row_xpath, cell_xpath = "./thead/tr", "./th"
                row_element = self.table_element.find_element(By.XPATH, row_xpath)
                self._headers = Web.Table.row_cells(row_element, cell_xpath=cell_xpath, column_names=self.column_names)
            return self._headers if as_elements else Web.Table.as_values([self._headers])[0]

        def rows(self, as_elements=True, **filters):
            if not filters:
                filters = {}

            filtered_rows = [row for row in self.elements
                             if all([row[column].text == value for column, value in filters.iteritems()])]

            return filtered_rows if as_elements else Web.Table.as_values(filtered_rows)

        def row(self, as_elements=True, **filters):
            rows = self.rows(as_elements, **filters)

            if not rows:
                return None
            if len(rows) > 1:
                raise TestsError(u"Table {} have more than one row for given filters: {}".format(self.name, filters))
            return rows[0]

        def cell(self, column, as_elements=True, **filters):
            if column not in self.column_names:
                raise TestsError(u'Invalid cell name {} for table {}'.format(column, self.name))
            row = self.row(as_elements, **filters)
            if not row:
                return None
            return row[column]

    class BalanceError(object):
        ERROR_BLOCK = (By.XPATH, "//div[contains(@class,'error')]")
        ERROR_MSG_BLOCK = (By.XPATH, "//div[contains(@class,'error')]/form/input[@id='exception']")
        TRACEBACK_BLOCK = (By.XPATH, "//div[contains(@class,'error')]/pre")

        TIMEOUT_MESSAGES = [u'Таймаут операции превышен',
                            u'Сервис временно недоступен',
                            u'Ошибка при обращении к серверу',
                            u'ошибка при обращении к серверу']

        @staticmethod
        def is_timeout(error_text):
            return any(timeout_msg in error_text for timeout_msg in Web.BalanceError.TIMEOUT_MESSAGES)

        @staticmethod
        def raise_error(desc, is_timeout, error_code, error_msg, traceback):
            trace_error = traceback.split('\n')[0] if traceback else None
            raise ServiceError(
                u'{desc}{is_timeout}{error_code}{error_msg}{trace_error}'
                    .format(desc=desc,
                            is_timeout=u' по таймауту' if is_timeout else u'',
                            error_code=u', code: {}'.format(error_code) if error_code else u'',
                            error_msg=u', msg: {}'.format(error_msg) if error_msg else u'',
                            trace_error=u', {}'.format(trace_error) if trace_error else u''))

    class HttpError(object):
        HTTP_ERROR_BLOCK = (By.XPATH, "//center[contains(.,'nginx')]")

        @staticmethod
        def parse(error_text):
            return

        @staticmethod
        def is_timeout(error_text):
            match = re.match('(?P<code>\d+) (?P<msg>[\w\s]+)', error_text)
            if match:
                http_error = match.groupdict()
                return http_error['code'] == '504'
            else:
                return False

        @staticmethod
        def raise_error(desc, error_text):
            raise ServiceError(u'{desc}{is_timeout}: {error_text}'
                               .format(desc=desc,
                                       is_timeout=u' по таймауту' if Web.HttpError.is_timeout(error_text) else u'',
                                       error_text=error_text))


class ReportingCallable(object):
    def __init__(self, callable_object):
        self.callable_object = callable_object
        self.__name__ = Presenter.method_name(callable_object)

    def __call__(self, *args, **kwargs):
        from btestlib import reporter  # todo-igogor ебаный костыль #циклическая зависимость

        with reporter.step(u"Вызываем метод " + self.__name__,
                           allure_=reporter.options().is_show_level(reporter.Level.AUTO_STEPS_ONLY), log_=False):
            try:
                start = time.time()
                result = self.callable_object(*args, **kwargs)
                duration = (time.time() - start)
            except Exception as e:
                error_descr = reporter.pformat(e)
                # убираем детали для группировки ошибок
                if isinstance(e, xmlrpclib.Fault):
                    error_descr = e.faultString
                    if 'Timeout' in e.faultString:
                        e.faultString = re.sub('(Description: \d+\.)\d+', r'\1_', e.faultString)
                    # if 'ORA-12541: TNS:no listener' in e.faultString \
                    #         or 'ORA-12170: TNS:Connect timeout occurred' in e.faultString:
                    #     e.faultString = e.faultString.split('Traceback:')[0] + 'Traceback: _OBFUSCATED_'
                    e.faultString = ReportingCallable.omit_traceback(e.faultString)
                elif isinstance(e, XmlRpc.XmlRpcError):
                    if 'ORA-00054: resource busy and acquire with NOWAIT specified or timeout expired' in e.response:
                        e = XmlRpc.XmlRpcError(message=e.message,
                                               response=re.sub("('param_1': )\d+", r'\1_OMITTED_', e.response))
                    if 'Traceback:' in e.response:
                        e = XmlRpc.XmlRpcError(message=e.message,
                                               response=ReportingCallable.omit_traceback(e.response))

                # Не удалять!
                # Во-первых здесь логгируются параметры запроса, который упал
                # Во-вторых xmlrpclib.Fault может содержать нечитаемый repr от юникода, и здесь логгируется ошибка
                # в читаемом виде
                reporter.attach(u'Полный текст ошибки из {}'.format(self.__name__),
                                u'{}\nОшибка:\n{}'.format(reporter.str_call(self.__name__, args, kwargs), error_descr),
                                allure_=True, log_=True, log_label=False)
                raise e

            duration_name = u'({:.3f}s) {}'.format(duration, self.__name__)
            pretty_result = reporter.pformat(result)

            reporter.attach(duration_name,
                            u'({})\n=> {}'.format(String.fill_newline(reporter.str_call_args(args, kwargs),
                                                                      shift=len(duration_name) + 1),
                                                  String.fill_newline(pretty_result, shift=3)),
                            separator=u'', oneline=reporter.options().level == reporter.Level.AUTO_ONE_LINE,
                            allure_=False, log_=reporter.options().is_show_level(reporter.Level.AUTO_ONE_LINE))
            reporter.attach(duration_name, u'{}\n{}\n=>\n{}'.format(str(datetime.datetime.fromtimestamp(start)),
                                                                    reporter.str_call(self.__name__, args, kwargs),
                                                                    pretty_result),
                            oneline=reporter.options().level == reporter.Level.AUTO_ONE_LINE,
                            allure_=reporter.options().is_show_level(reporter.Level.AUTO_ONE_LINE), log_=False)
            return result

    @staticmethod
    def omit_traceback(msg):
        return msg.split('Traceback:')[0].rstrip() + ' Traceback: _OMITTED_' if 'Traceback:' in msg else msg


class XmlRpc(object):
    class XmlRpcError(ServiceError):
        def __init__(self, message, response=None):
            self.message = message
            self.response = response
            # unicode рейзится в читаемом виде, только если  Exception принимает один аргумент
            msg = u"{msg}{resp}".format(msg=message,
                                        resp=u"\nResponse: {}".format(response) if response else u'')
            super(XmlRpc.XmlRpcError, self).__init__(msg)

    class UnverifiedTransport(xmlrpclib.SafeTransport, object):
        def __init__(self, use_datetime=0):
            super(XmlRpc.UnverifiedTransport, self).__init__(use_datetime, context=self.unverified_context())

        @staticmethod
        def unverified_context():
            return hasattr(ssl, '_create_unverified_context') and ssl._create_unverified_context() or None

    class TrustAutotestUnverifiedTransport(xmlrpclib.SafeTransport, object):
        def __init__(self, use_datetime=0):
            super(XmlRpc.TrustAutotestUnverifiedTransport, self).__init__(use_datetime,
                                                                          context=self.unverified_context())

        @staticmethod
        def unverified_context():
            return hasattr(ssl, '_create_unverified_context') and ssl._create_unverified_context() or None

        def send_host(self, connection, header):
            connection.putheader('X-Trust-Me-I-Am-Autotest', 'WE_ARE_THE_BALANCE')

    class CoverageTransport(xmlrpclib.Transport, object):
        def __init__(self, use_datetime=0, coverage_header_value='testcontext'):
            super(XmlRpc.CoverageTransport, self).__init__(use_datetime)
            self.coverage_header_value = coverage_header_value

        def send_host(self, connection, header):
            connection.putheader('X-TEST-CONTEXT', self.coverage_header_value)

    class ReportingServerProxy(object):
        def __init__(self, url, namespace, context=None, transport=None):
            if not transport:
                transport = XmlRpc.load_custom_transport(url)

            self.server = xmlrpclib.ServerProxy(uri=url, allow_none=1, context=context, use_datetime=1,
                                                transport=transport)
            self.namespace = namespace

        def __getattr__(self, name):
            attr = '{}.{}'.format(self.namespace, name) if self.namespace else name
            return XmlRpc.ReportingXmlRpcCallable(self.server.__getattr__(attr))

    @staticmethod
    def load_custom_transport(url):

        parsed_url = urlparse.urlparse(url)
        if parsed_url.scheme == 'https' and 'trust' in parsed_url.hostname:
            # default_transport = XmlRpc.UnverifiedTransport(use_datetime=1)
            default_transport = XmlRpc.TrustAutotestUnverifiedTransport(use_datetime=1)
        else:
            default_transport = None

        if 'coverage' not in url:
            return default_transport

        if pkgutil.find_loader('btestlib.local_config'):
            import btestlib.local_config as cfg

            if hasattr(cfg, 'XMLRPC_COVERAGE'):
                return XmlRpc.CoverageTransport(use_datetime=1, coverage_header_value=cfg.XMLRPC_COVERAGE)

        xmlrpc_coverage = os.getenv('xmlrpc_coverage', False)
        if xmlrpc_coverage:
            return XmlRpc.CoverageTransport(use_datetime=1, coverage_header_value=xmlrpc_coverage)

        return default_transport

    @staticmethod
    def parse_request_xml(request):
        encoded = request.encode(errors='xmlcharrefreplace') if isinstance(request, unicode) else request
        return xmlrpclib.loads(encoded, use_datetime=1)

    class ReportingXmlRpcCallable(ReportingCallable):
        def __call__(self, *args, **kwargs):
            response = super(XmlRpc.ReportingXmlRpcCallable, self).__call__(*args, **kwargs)

            # a-vasin: дополнительные проверки против ложного срабатывания
            if isinstance(response, dict):

                if ('traceback' in response) \
                        and (response["traceback"] is not None) and (response["traceback"] != "None"):
                    # blubimov duplicated attaches
                    # reporter.attach(u"XmlRpc ответ", Presenter.pretty(response))
                    # reporter.attach(u"Стектрейс", response['traceback'])
                    err = response['error'] if (response.get('error') is not None) and \
                                               (response.get('error') != 'None') \
                        else response['traceback']
                    raise XmlRpc.XmlRpcError(u"Traceback in XmlRpc response is not None", err)

                    # if (response.get('type') == Export.Type.OEBS) and (response.get('output') == 'No firms'):
                    #     # reporter.attach(u"XmlRpc ответ", Presenter.pretty(response))
                    #     raise XmlRpc.XmlRpcError(u"{} was not exported to OEBS".format(response['classname']), 'No firms')

            return response

    @staticmethod
    def to_request_dict(keys_to_case):
        """Декоратор реализует стратегию преобразования ключей для метода"""

        def _to_request_dict(**kwargs):
            return {keys_to_case(key): value for key, value in kwargs.iteritems()}

        return _to_request_dict


class Pytest(object):
    # params_format_marker = pytest.mark.params_format
    #
    # @staticmethod
    # def format_parametrized_python_item_name(items):
    #     for item in items:
    #         parametrize_marker = item.get_marker('parametrize')
    #         if not parametrize_marker:
    #             continue
    #
    #         format_params_marker = item.get_marker('params_format')
    #         if not format_params_marker:
    #             continue
    #
    #         params = item.callspec.params.copy()
    #         for k, v in params.iteritems():
    #             params[k] = Pytest._unwrap(v)[0]
    #
    #         param_formatters = format_params_marker.kwargs
    #         for param_name, formatter in param_formatters.iteritems():
    #             params[param_name] = formatter(params[param_name])
    #
    #         params_names_ordered = [name for name in item.fixturenames if name in params]
    #         default_format = '-'.join(['{{{}}}'.format(param_name) for param_name in params_names_ordered])
    #         format_string = format_params_marker.args[0] if format_params_marker.args \
    #             else default_format
    #         item.name = '{func}[{params}]'.format(func=item.name.split('[')[0],
    #                                               params=format_string.format(**params))

    @staticmethod
    def combine(*named_params_groups):
        names = ', '.join([named_group[0] for named_group in named_params_groups])
        params_groups = [named_group[1] for named_group in named_params_groups]
        max_len = max([len(params_set) for params_set in params_groups])

        # дополним каждый набор параметров до максимальной длины своими же значениями идущими по порядку
        filled_params_groups = [fill(params_set, to_len=max_len) for params_set in params_groups]
        params_sets = zip(*filled_params_groups)

        params_marks = []
        params_values = []
        for params_set in params_sets:
            params_values.append(flatten([Pytest._unwrap(param)[0] for param in params_set]))
            params_marks.append(flatten([Pytest._unwrap(param)[1] for param in params_set]))

        marked_params_list = [Pytest._combine_marks(marks, values) if marks else values
                              for marks, values in zip(params_marks, params_values)]

        return names, marked_params_list

    @staticmethod
    def combine_set(*named_params_groups):
        def get_cartesian_product_from(data, length, shift):
            import itertools
            prepared_data = []
            for i in range(length):
                k = data[i][shift]
                prepared_data.append(k)
            return itertools.product(*prepared_data)

        names = ', '.join([named_group[0] for named_group in named_params_groups])
        params_groups = [named_group[1] for named_group in named_params_groups]

        params_set_count = len(params_groups)
        params_count = len(params_groups[0])

        params_sets = []
        for i in range(params_count):
            params_sets.extend(get_cartesian_product_from(params_groups, params_set_count, i))

        params_marks = []
        params_values = []
        for params_set in params_sets:
            params_values.append([Pytest._unwrap(param)[0] for param in params_set])
            params_marks.append(flatten([Pytest._unwrap(param)[1] for param in params_set]))

        marked_params_list = [Pytest._wrap_marks(marks, values) if marks else values
                              for marks, values in zip(params_marks, params_values)]

        return names, marked_params_list

    ParamsSet = collections.namedtuple('ParamsSet', 'names, values')

    @staticmethod
    def _unwrap(param):
        marks = []
        while isinstance(param, _pytest.mark.MarkDecorator):
            marks.append(_pytest.mark.MarkDecorator(_pytest.mark.Mark(param.name, param.args[:-1], param.kwargs)))
            param = param.args[-1]
        return param, marks

    @staticmethod
    def _combine_marks(marks, params):
        repeating_marks = collections.defaultdict(list)
        for mark in marks:
            repeating_marks[mark.name].append(mark)
        unique_marks = []
        for name, name_marks in repeating_marks.iteritems():
            if len(name_marks) > 1:
                unique_marks.append(functools.reduce(lambda mark1, mark2: mark1(*mark2.args, **mark2.kwargs),
                                                     name_marks))
            else:
                unique_marks.append(name_marks[0])

        return functools.reduce(lambda mark1, mark2: mark2(mark1), [tuple(params)] + unique_marks)

    @staticmethod
    def _wrap_marks(marks, params):
        if not marks:
            return params
        else:
            for mark in marks[:]:
                marks.remove(mark)
                return Pytest._wrap_marks(marks, mark(params))


class XFailPredicate(object):
    def __init__(self, attr_name, predicate, test_name=None, reason=""):
        self.attr_name = attr_name
        self.predicate = predicate
        self.test_name = test_name
        self.reason = reason


class ReportingSFTPClient(object):
    def __init__(self, url, login, password=None, port=22, pkey_path=None):
        transport = paramiko.Transport((url, port))
        key = paramiko.RSAKey.from_private_key_file(project_file(pkey_path)) if pkey_path else None
        transport.connect(username=login, password=password, pkey=key)
        self.client = paramiko.SFTPClient.from_transport(transport)

    def __getattr__(self, name):
        return ReportingCallable(getattr(self.client, name))

    def upload_file(self, source_file_path, target_dir, rename_file=False):
        from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
        with reporter.step(u"Загружаем файл через sftp"):
            target_file_path = target_dir + "/" + (rename_file if rename_file else \
                                                       source_file_path.split(os.sep)[-1])

            reporter.attach(u"Путь к исходному файлу", source_file_path)
            reporter.attach(u"Путь к целевому файлу", target_file_path)

            self.put(source_file_path, target_file_path)

    def clear_dir(self, path):
        from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
        with reporter.step(u"Удаляем все файлы в директории"):
            reporter.attach(u"Путь к папке", path)

            file_names = [single_file.filename for single_file in self.listdir_attr(path)]
            reporter.attach(u"Имена файлов", Presenter.pretty(file_names))

            for file_name in file_names:
                self.remove(path + "/" + file_name)


class GraphitSender(object):
    DEFAULT_SENDER_HOST = 'greed-dev3f_balance_os_yandex_ru'
    DEFAULT_RECEIVER_HOST = 'localhost-ipv6'
    DEFAULT_RECEIVER_PORT = 42000
    DEFAULT_TPL = 'one_day.%(sender_host)s.%(source)s.%(name)s %(metric)s %(time)s'

    @staticmethod
    def send(source, name, value, time_point, tpl=DEFAULT_TPL, sender_host=DEFAULT_SENDER_HOST,
             receiver_host=DEFAULT_RECEIVER_HOST, port=DEFAULT_RECEIVER_PORT):

        from btestlib import reporter  # todo-igogor ебаная циклическая зависимость

        reporter.log(
            'source: {0} name {1} value {2} time {3} tpl {4} sender {5} receiver {6}'.format(source, name, value,
                                                                                             time_point, tpl,
                                                                                             sender_host,
                                                                                             receiver_host))

        message = (tpl + "\n") % dict(source=source, name=name, metric=value, time=time_point,
                                      sender_host=sender_host)

        reporter.log('{0}:{1} {2}'.format(receiver_host, port, message))

        sock = socket.socket(socket.AF_INET6)
        try:
            sock.connect((receiver_host, port))
            sock.sendall(message)
        except Exception as exc:
            reporter.log("Stats can't be sent to GRAPHIT")
            pass
        finally:
            sock.close()


class S3Storage(object):
    CONNECTION = None

    def __init__(self, bucket_name):
        self.bucket_name = bucket_name
        self._bucket = None

    @property
    def bucket(self):
        # todo-igogor чет я не уверен что эта оптимизация нужна
        if not S3Storage.CONNECTION:
            from btestlib import config, secrets  # igogor: вива ля циклические зависимости
            S3Storage.CONNECTION = boto.connect_s3(
                host='s3.mds.yandex.net', is_secure=True,
                aws_access_key_id=secrets.get_secret(*secrets.Infr.S3_ACCESS_KEY_ID),
                aws_secret_access_key=secrets.get_secret(*secrets.Infr.S3_ACCESS_KEY_SECRET))

        if not self._bucket:
            try:
                self._bucket = S3Storage.CONNECTION.head_bucket(self.bucket_name)
            except S3ResponseError:
                self._bucket = S3Storage.CONNECTION.create_bucket(self.bucket_name)
        return self._bucket

    def __getattribute__(self, name):
        attr = object.__getattribute__(self, name)
        if callable(attr):
            return ReportingCallable(attr)
        return attr

    def set_string_value(self, key, value):
        bucket_key = Key(self.bucket)

        bucket_key.key = key
        bucket_key.set_contents_from_string(value)

    def set_string_value_simple_key(self, key, value):
        bucket_key = Key(self.bucket)

        bucket_key.key = key
        bucket_key.set_contents_from_string(value)

    def set_file_value(self, key, path):
        bucket_key = Key(self.bucket)

        bucket_key.key = key
        bucket_key.set_contents_from_filename(path)

    def is_present(self, key):
        return self.bucket.lookup(key) is not None

    def get_string_value(self, key):
        content = self.bucket.get_key(key)
        if content:
            result = content.get_contents_as_string()
        else:
            result = None

        return result

    def get_file_value(self, key, path):
        self.bucket.get_key(key).get_contents_to_filename(path)

    def delete_key(self, key):
        self.bucket.delete_key(key)

    def get_keys(self):
        return self.bucket.list()

    def get_url(self, key, expires_in=86400):
        content = self.bucket.get_key(key)
        return content.generate_url(expires_in=expires_in, force_http=True,
                                    query_auth=False)


class SkipContextManagerBodyException(Exception):
    pass


@cached
def get_test_db_restore_dt_value():
    import btestlib.reporter as reporter

    from balance import balance_db as db
    value = db.balance().execute("SELECT value_dt FROM bo.t_config WHERE item = 'TEST_DB_RESTORE_DT'")
    restore_dt = None
    if value:
        restore_dt = value[0]['value_dt']
    else:
        reporter.logger().warning('TEST_DB_RESTORE_DT is empty. Check this')
    return restore_dt


# Контекст-менеджер для кеширования сущностей, которые можно переиспользовать между запусками тестов.
class CachedData(object):
    def __init__(self, data_cache, expected_keys, force_invalidate=False):
        # уникальное имя теста и кешированные данные для него (или None) из фикстуры data_cache (conftest.py)
        self.item, self.stored_cache = data_cache
        self.expected_keys = expected_keys
        self.invalidated = False
        self.force_invalidate = force_invalidate

    def __enter__(self):
        # Данные могут уже быть инвалидированы на данный момент - при передаче флага --invalidate_cache в параметрах
        # запуска. Тогда инвалидация происходит в фикстуре data_cache

        # В первую очередь проверяем необходимость явной инвалидации. Чтобы изменения формата не прождали ошибок.
        if self.force_invalidate or not self.stored_cache:
            self.invalidated = True
        else:
            # Если в хранилище есть какие-либо данные разбираем их и будем проверять
            self.cache_dt = self.stored_cache.get('cache_dt', None)
            self.cached_data = self.stored_cache.get('data', None)

            # Если дата сохранения данных меньше, чем дата переналивки базы - инвалидируем данные
            db_restore_dt = get_test_db_restore_dt_value()
            invalidated_by_date = self.cache_dt < db_restore_dt if db_restore_dt else False

            # Проверяем совпадение ожидаемых и сохранённых ключей
            invalidated_by_keys = set(self.cached_data.keys()) != set(self.expected_keys)

            self.invalidated = invalidated_by_date or invalidated_by_keys

        return self.invalidated

    def __exit__(self, exc_type, exc_val, exc_tb):

        import btestlib.environments as env
        import btestlib.reporter as reporter

        # # Если создавались новые данные для теста - сохраняем их
        # if self.invalidated:
        #     # Подавляем любые ошибки, чтобы не валить тесты, если не удалось сохранить значение в кеш.
        #     try:
        #         to_cache = {key: self.locals[key] for key in self.expected}
        #         s3storage_cache().set_string_value(self.item, pickle.dumps(to_cache))
        #     except Exception, exc:
        #         reporter.log('Error while save data to s3storage_cache: {}'.format(exc))

        # Магия от Димы dimonb@ и Коли ngolovkin@
        if exc_type is None:

            frame = inspect.currentframe().f_back
            try:
                cache_value = {key: frame.f_locals[key] for key in self.expected_keys}
                to_cache = {'cache_dt': datetime.datetime.now(), 'data': cache_value}
                key_prefix = env.ENV_TO_S3_PREFIX[env.balance_env().name]
                with reporter.reporting():
                    s3storage_cache().set_string_value('{}/{}'.format(key_prefix, self.item), pickle.dumps(to_cache))
            except Exception, exc:
                reporter.logger().error('Error while save data to s3storage_cache: {}'.format(exc))

        if exc_type == SkipContextManagerBodyException:
            frame = inspect.currentframe().f_back
            frame.f_locals.update(self.cached_data)
            ctypes.pythonapi.PyFrame_LocalsToFast(ctypes.py_object(frame), ctypes.c_int(0))
            return True


class ParseCollaterals(object):
    COLLATERAL_HEADER_LENGTH = 5
    COLLATERAL_FOOTER_LENGTH = 7

    @staticmethod
    def get_ids_set(param_lines):
        regexp = re.compile(r'-grp-(\d+)-')
        ids = set()

        for line in param_lines:
            match = regexp.search(line.split('=')[0])
            if match:
                ids.add(int(match.group(1)))

        return ids

    @staticmethod
    def split_collaterals(param_string):
        lines = param_string.split('&')
        header = lines[:ParseCollaterals.COLLATERAL_HEADER_LENGTH]
        footer = lines[-ParseCollaterals.COLLATERAL_FOOTER_LENGTH:]

        # a-vasin: вторым параметром указан id, поэтому нужно впилить плейсхолдер для параметра
        # Это очень некрасиво, но для скрипта, который нужно запускать руками, пойдет
        header[1] = "col-new-collateral-type={}"

        body = lines[ParseCollaterals.COLLATERAL_HEADER_LENGTH:-ParseCollaterals.COLLATERAL_FOOTER_LENGTH]
        ids_set = ParseCollaterals.get_ids_set(body)

        params_dict = {id: [line for line in body if '-grp-' + str(id) + '-' in line.split('=')[0]]
                       for id in ids_set}

        return '&'.join(header), \
               {collateral_id: '&'.join(params) for collateral_id, params in params_dict.iteritems()}, \
               '&'.join(footer)


class UnsuccessfulResponse(TestsError):
    pass


class CheckMode(object):
    BROKEN = 'broken'
    FAILED = 'failed'
    IGNORED = 'ignored'

    def __init__(self, mode=BROKEN):
        self.mode = mode

    def set(self, mode):
        self.mode = mode

    @staticmethod
    def obfuscate(response):
        simple_keys = ('status',
                       'status_code',
                       'status_desc')

        def is_simple(response_):
            return all(k in response_ for k in simple_keys)

        if response and is_simple(response):
            return {k: response.get(k) for k in response if k in simple_keys}

        return response

    @staticmethod
    def result_matches(matcher):
        def checker(func):
            @functools.wraps(func)
            def wrapper(*args, **kwargs):
                response = func(*args, **kwargs)
                if check_mode_instance().mode == CheckMode.IGNORED:
                    return response
                elif not matcher.matches(response):
                    error_str = 'Error while running step \'{}\'' \
                                '\n\tExpected response: {} ' \
                                '\n\tBut actual response is: {}' \
                        .format(func.__name__, matcher, CheckMode.obfuscate(response))
                    if check_mode_instance().mode == CheckMode.BROKEN:
                        raise UnsuccessfulResponse(error_str)
                    elif check_mode_instance().mode == CheckMode.FAILED:
                        assert False, error_str

                return response

            return wrapper

        return checker


def retry_if(matcher, attempts=3, skip_error=True):
    def retrier(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            @retry(retry=retry_if_result(matcher.matches),
                   stop=stop_after_attempt(attempts))
            def func_with_retries():
                return func(*args, **kwargs)

            try:
                return func_with_retries()
            except RetryError as e:
                if skip_error:
                    return e.last_attempt.result()
                else:
                    raise e

        return wrapper

    return retrier


class FileLock(object):
    LOCK_DIR = project_dir('locks_directory') + os.sep

    def __init__(self, key, timeout=1):
        self.key = key
        self.file = None
        self.timeout = timeout
        self.value = None

        if not os.path.exists(self.LOCK_DIR + self.key):
            try:
                self.file = open(self.LOCK_DIR + self.key, 'w')
                portalocker.lock(self.file, portalocker.LOCK_EX)
                self.file.write('{} {}'.format(0, False))
                portalocker.unlock(self.file)
                self.file.close()
            except portalocker.exceptions.LockException:
                pass
            finally:
                self.file = None

    def lock(self):
        while not self.try_lock():
            time.sleep(self.timeout)

    def try_lock(self):
        try:
            self.file = open(self.LOCK_DIR + self.key, 'r+')
            portalocker.lock(self.file, portalocker.LOCK_EX)
            return True
        except portalocker.exceptions.LockException:
            self.file = None
            return False

    def unlock(self):
        if not self.file:
            return False

        try:
            portalocker.unlock(self.file)
            self.file.close()
            self.file = None
            return True
        except OSError:
            return False

    def read_value(self):
        return self.file.read()

    def write_value(self, value):
        self.file.seek(0)
        self.file.write(value)
        self.file.truncate()


class ReadWriteLock(object):
    def __init__(self, key, write=True, timeout=1):
        self.file_lock = FileLock(key)
        self.write = write
        self.timeout = timeout

    def read_values(self):
        raw_values = self.file_lock.read_value().split()
        return int(raw_values[0]), raw_values[1] == 'True'

    def lock_write(self, expected_readers=0):
        while True:
            self.file_lock.lock()
            readers, is_writing = self.read_values()
            if not is_writing and readers == expected_readers:
                break
            self.file_lock.unlock()
            time.sleep(self.timeout)
        self.file_lock.write_value('{} {}'.format(0, True))
        self.file_lock.unlock()

    def unlock_write(self, readers=0):
        self.file_lock.lock()
        self.file_lock.write_value('{} {}'.format(readers, False))
        self.file_lock.unlock()

    def lock_read(self):
        while True:
            self.file_lock.lock()
            readers, is_writing = self.read_values()
            if not is_writing:
                break
            self.file_lock.unlock()
            time.sleep(self.timeout)
        self.file_lock.write_value('{} {}'.format(readers + 1, False))
        self.file_lock.unlock()

    def unlock_read(self):
        self.file_lock.lock()
        readers, _ = self.read_values()
        self.file_lock.write_value('{} {}'.format(readers - 1, False))
        self.file_lock.unlock()

    def upgrade_to_write(self):
        if self.write:
            return

        self.unlock()
        self.write = True
        self.lock()

    def downgrade_to_read(self):
        if not self.write:
            return

        self.unlock()
        self.write = False
        self.lock()

    def lock(self):
        if self.write:
            self.lock_write()
        else:
            self.lock_read()

    def unlock(self):
        if self.write:
            self.unlock_write()
        else:
            self.unlock_read()


class FileStorage(object):
    @staticmethod
    def read(file_name, base_path=''):
        try:
            with open(base_path + file_name, 'r') as storage:
                data = storage.read()
        except IOError:
            data = None

        return data

    @staticmethod
    def write(file_name, data, base_path=''):
        with open(base_path + file_name, 'a') as storage:
            storage.write(str(data) + '\n')


@cached
def check_mode_instance():
    return CheckMode(CheckMode.BROKEN)


@contextmanager
def check_mode(mode):
    """
    Контекст-менеджер для степов
    Устанавливает необходимый режим,
    а при выходе из контекста сбрасывает его в изначальное состояние
    """

    mode_before = check_mode_instance().mode

    check_mode_instance().set(mode)
    yield
    check_mode_instance().set(mode_before)


class InnGenerator(object):
    @staticmethod
    def generate_inn_rus_ur_10(region_code='78'):
        # type: (str) -> str
        """ Генератор валидного 10 значного ИНН для юр.лиц РФ """
        inn = region_code + ''.join(str(random.randint(0, 9)) for _ in range(7))
        return inn + str(InnGenerator._key_figure(inn, [2, 4, 10, 3, 5, 9, 4, 6, 8]))

    @staticmethod
    def generate_inn_rus_ph_12(region_code='78'):
        # type: (str) -> str
        """ Генератор валидного 12 значного ИНН для физ.лиц РФ """
        inn = region_code + ''.join(str(random.randint(0, 9)) for _ in range(8))
        inn += str(InnGenerator._key_figure(inn, [7, 2, 4, 10, 3, 5, 9, 4, 6, 8]))
        return inn + str(InnGenerator._key_figure(inn, [3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8]))

    @staticmethod
    def generate_inn_oebs_unique(inn_generator):
        """ Генератор ИНН, которого еще нет в ОЕБС """
        from balance import balance_db as db
        attempts = 10
        for i in range(attempts):
            inn = inn_generator()
            # blubimov фирма здесь не имеет значения?
            qty = \
                db.oebs().execute("SELECT count(*) AS qty FROM apps.hz_parties WHERE orig_system_reference=:object_id",
                                  {'object_id': 'I{}'.format(inn)}, single_row=True)['qty']
            if qty == 0:
                return inn
        raise TestsError('{} attempts to generate oebs unique INN failed'.format(attempts))

    @staticmethod
    def _key_figure(inn, coeffs):
        """вычисление контрольной цифры"""
        figure = sum([int(a) * b for a, b in zip(inn, coeffs)])
        return (figure % 11) % 10


class WaiterTypes(ConstantsContainer):
    WaiterType = namedtuple_with_defaults('WaiterType', ['name', 'time_of_observ', 'max_fails'],
                                          default_values={'time_of_observ': 5 * 60, 'max_fails': 10})
    constant_type = WaiterType

    PAYMENT_DONE = WaiterType(name=u'payment_done')
    PAYMENT_REFUNDED = WaiterType(name=u'payment_refunded')
    CLEARING_DONE = WaiterType(name=u'clearing_done')
    FISCAL_DONE = WaiterType(name=u'fiscal_done')
    REFUND_DONE = WaiterType(name=u'refund_done')
    AUTOREFUND_DONE = WaiterType(name=u'autorefund_done')
    REAL_POSTAUTH_DONE = WaiterType(name=u'real_postauth_done')
    BINDING_DONE = WaiterType(name=u'binding_done')
    SUBS_CONTINUATION = WaiterType(name=u'subs_continuation', time_of_observ=6 * 60, max_fails=5)
    SUBS_PHANTOM_PERIOD = WaiterType(name=u'subs_phantom_period', time_of_observ=6 * 60, max_fails=5)
    SUBS_INTRODUCTORY_PERIOD_CONTINUATION = WaiterType(name=u'subs_introductory_period_continuation',
                                                       time_of_observ=6 * 60, max_fails=5)


class WaiterHolder(object):
    WAITER_DIR = project_dir('waiters_directory') + os.sep

    def append_problem(self, waiter_type):
        FileStorage.write(waiter_type.name, data=time.time(), base_path=self.WAITER_DIR)

    def problem_detected(self, waiter_type):
        times_str = FileStorage.read(waiter_type.name, base_path=self.WAITER_DIR)

        if not times_str:
            return False

        times = [float(time_) for time_ in times_str.split('\n') if time_]
        # считаем количество падений в интервале времени [now, now-time_of_observ]
        # если падений в этом диапазоне более max_fails, значит с вейтером проблема
        if len([time_ for time_ in times if time.time() - time_ <= waiter_type.time_of_observ]) > waiter_type.max_fails:
            return True

        return False


@cached
def wh_instance():
    return WaiterHolder()


@contextmanager
def waiting_context(timeout, timeout_fast=None, waiter_type=None):
    if not is_local_launch() and waiter_type:
        waiter_holder = wh_instance()

        if waiter_holder.problem_detected(waiter_type=waiter_type):
            from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
            with reporter.step(u'В вэйтере {} обнаружены проблемы, '
                               u'уменьшим заданное время ожидания'.format(waiter_type.name)):
                actual_timeout = timeout_fast or timeout / 3.
        else:
            actual_timeout = timeout

        try:
            yield actual_timeout
        except ConditionHasNotOccurred as e:
            waiter_holder.append_problem(waiter_type=waiter_type)
            raise e

    else:
        yield timeout


@measure_time()
def wait_until(predicate, success_condition, failure_condition=None,
               timeout=120, sleep_time=5, descr=None, timeout_fast=None, waiter_type=None):
    from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
    with reporter.step(u'Ожидаем выполнения условия'):
        condition = Presenter.matcher(success_condition)
        reporter.attach(u'Условие', condition)

        value = None

        with waiting_context(timeout=timeout,
                             timeout_fast=timeout_fast,
                             waiter_type=waiter_type) as actual_timeout:
            until = time.time() + actual_timeout
            while time.time() < until:
                value = predicate()

                if failure_condition and failure_condition.matches(value):
                    reporter.attach(u"Последнее полученное (некорректное) значение", value)
                    raise PredicateStateIncorrect("Incorrect predicate state while waiting for condition '{}'{}. "
                                                  "The reason is '{}'"
                                                  .format(condition,
                                                          u'({})'.format(descr) if descr else u'',
                                                          Presenter.matcher(failure_condition)),
                                                  value)
                if success_condition.matches(value):
                    reporter.attach(u"Возвращаемое значение", value)
                    return value

                time.sleep(sleep_time)

            reporter.attach(u"Последнее полученное значение", value)
            raise ConditionHasNotOccurred(u"Condition '{}'{} has not occurred in {} seconds"
                                          .format(condition,
                                                  u'({})'.format(descr) if descr else u'',
                                                  unicode(timeout)),
                                          value)


def wait_until2(predicate, matcher, timeout=180, sleep_time=5):
    from btestlib import reporter  # todo-igogor ебаная циклическая зависимость
    def _wait_until(*args, **kwargs):
        until = time.time() + timeout
        descr = Presenter.match_expr(Presenter.method_call(predicate, args, kwargs), matcher)
        with reporter.step(u'Ожидаем выполнения условия'):
            reporter.attach(u'Условие', descr)
            while time.time() < until:
                value = predicate(*args, **kwargs)
                if matcher.matches(value):
                    return value
                time.sleep(sleep_time)
            raise ConditionHasNotOccurred(
                "Condition {} has not occured in {} seconds".format(descr, str(timeout)), value)

    return _wait_until


def get_captcha_answer(captcha_key):
    resp = requests.get('http://api.captcha.yandex.net/answer', {'key': captcha_key})
    xml = etree.fromstring(resp.text)
    answer = xml.xpath('//answer/text()')[0]
    return answer


def get_subitem(src, path, not_found_value=None):
    # Скопировано из проекта "Балалайка"
    """Возвращает значение, проходя указанным путём по вложенным в указанный словарь словарям.

    :param dict|object src: Объект объектов (или словарь словарей),
        в котором требуется обнаружить значение по указанному пути.

    :param str path: Путь до нужного элемента, образованный из имён элементов вложенных объектов.
        Имена разделяются точками. Например: root.subitem.subsub.val

    :param not_found_value:

    """
    dict_mode = isinstance(src, dict)

    path = path.split('.')
    path.reverse()

    src = src
    value = not_found_value

    while path:
        chunk = path.pop()

        if dict_mode:
            value = src.get(chunk, not_found_value)
        else:
            value = getattr(src, chunk, not_found_value)

        if value is not_found_value:
            break

        src = value

    return value


def flatten_parametrization(*args):
    """
    Получает на вход несколько коллекций, комбинирует их в одну умножением.
    Для первой коллекции поддерживается mark - той же меткой будут помечены наборы в итоговой коллекции
    Быстрый костыль для нескольких параметризаций и SharedBlock - чтобы пайтест выполнял
    их всегда в одном порядке
    """
    import _pytest
    flatten = []
    top = args[0]
    bottom = args[1:]

    def _to_list(val):
        if isinstance(val, collections.Iterable) and not isinstance(val, (bytes, basestring)):
            return [subval for subval in val]
        else:
            return [val]

    for params_t in top:  # для первой параметризации поддерживаются mark
        use_mark = 0
        if isinstance(params_t, _pytest.mark.MarkDecorator):
            use_mark = 1
            _params_t = _to_list(params_t.args[0])
            _name_t = params_t.name
        else:
            _params_t = _to_list(params_t)
        result_combination = [_params_t]
        for params_b in bottom:  # итерируемся по надору параметризаций (кроме первой)
            cur_combination = []
            for comb in result_combination:  # итерируемся по уже сделанным кобинациям
                for params in params_b:  # размножаем уже сделанные комбинации параметры из очередной параметризации
                    cur_combination.append(comb + _to_list(params))
            result_combination = cur_combination
        for comb in result_combination:
            if use_mark:
                flatten.append(_pytest.mark.MarkDecorator(_pytest.mark.Mark(_name_t, (comb,), {})))
            else:
                flatten.append(comb)
    return flatten


def reformat_item(item):
    if '[' in item:
        bracket = '['
    elif '(' in item:
        bracket = '('
    else:
        return item
    name, params = item.split(bracket)
    params = params[:-1]

    params_splitted = params.split('-')
    params_splitted.sort()
    return name + '[' + '-'.join(params_splitted) + ']'


# Monkeypatched method for teamcity_messages.pytest_plugin.EchoTeamCityMessages
def format_test_id(self, nodeid, location):
    id_from_location = self.get_id_from_location(location)

    if id_from_location is not None:
        return id_from_location

    test_id = nodeid

    if test_id:
        if test_id.find("::") < 0:
            test_id += "::top_level"
    else:
        test_id = "top_level"

    first_bracket = test_id.find("[")
    if first_bracket > 0:
        params = "[" + test_id[first_bracket + 1:]
        test_id = test_id[:first_bracket]
        if test_id.endswith("::"):
            test_id = test_id[:-2]
    else:
        params = ""

    test_id = test_id.replace("::()::", "::")
    test_id = re.sub(r"\.pyc?::", r"::", test_id)
    test_id = test_id.replace(".", "_").replace(os.sep, ".").replace("/", ".").replace('::', '.')

    if params:
        params = params.replace(".", "_")
        test_id += params

    return test_id
