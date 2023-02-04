# coding: utf-8
import logging
import os
import sys
import uuid
from collections import OrderedDict
from contextlib import contextmanager
from pprint import PrettyPrinter

import allure
import attr
import enum
import requests
from lxml import etree
from lxml.builder import E
import urllib3
from selenium.webdriver.remote.remote_connection import LOGGER as webdriver_logger

from btestlib import config
from btestlib import utils

# providing methods of allure module as reporter's own
_methods_to_provide = [
    'label',
    'severity',
    'tag',
    'epic',
    # 'feature',
    'story',
    'link',
    'issue',
    'testcase',
    # 'step',
    'dynamic',
    'severity_level',
    # 'attach',
    'attachment_type']

for method in _methods_to_provide:
    globals()[method] = getattr(allure, method)

MAXLEN = 150

_ENVIRONMENT = OrderedDict()


def logger():
    # logger = logging.getLogger(__name__ + '.default')
    logger = logging.getLogger()
    return logger


def log(something):
    logger().info(something)


def log_extra(something):
    if config.EXTRA_LOGGING:
        logger().debug(something)


def allure_logger():
    return logging.getLogger(__name__ + '.allure')


def _config_logger(logger, handler, formatter, level, propagate=0):
    logger.setLevel(level)
    logger.propagate = propagate
    handler.setLevel(level)
    handler.setFormatter(fmt=formatter)
    logger.addHandler(handler)


def _config_loggers():
    _config_logger(logger=allure_logger(), level=logging.DEBUG, handler=logging.StreamHandler(stream=sys.stdout),
                   formatter=logging.Formatter(fmt=u'%(asctime)s %(message)s', datefmt='%H:%M:%S'))
    _config_logger(logger=logger(), level=logging.DEBUG, handler=logging.StreamHandler(stream=sys.stdout),
                   formatter=logging.Formatter(fmt=u'D %(asctime)s %(name)s %(message)s', datefmt='%H:%M:%S'))

    # настройка логгинга для сторонних библиотек
    logging.getLogger('boto').setLevel(logging.CRITICAL)
    request_loggin_level = logging.DEBUG if config.EXTRA_LOGGING else logging.WARNING
    logging.getLogger("requests").setLevel(request_loggin_level)
    logging.getLogger('faker.factory').setLevel(logging.ERROR)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    webdriver_logger.setLevel(logging.WARNING)


_config_loggers()  # применяем конфиг логгеров при импорте


def _log_border(text, extra=0):
    level = LogStep.level() + extra
    border = u'--' * level + u' ' if level else u''
    return utils.String.fill_newline(text=border + text, shift=9 + len(border))


def step(title, allure_=None, log_=None):
    if allure_ is None:
        allure_ = options().is_show_level(level=Level.MANUAL_STEPS_ONLY)
    if log_ is None:
        log_ = options().is_show_level(level=Level.MANUAL_ONLY)

    # _mark_next_step(log_)
    if log_:
        allure_logger().info(_log_border(utils.String.unicodify(title)))

    if allure_ and options().is_use_allure():
        return LogStep(allure.step(title), log_)
    else:
        return LogStep(utils.empty_context_manager(), log_)


def attach(label, attachment=u'', attachment_type=allure.attachment_type.TEXT,
           allure_=None, log_=None, oneline=False, maxlen=MAXLEN, separator=u':\n', log_label=True):
    if allure_ is None:
        allure_ = options().is_show_level(level=Level.MANUAL_ONLY)
    if log_ is None:
        log_ = options().is_show_level(level=Level.MANUAL_ONLY)

    if attachment_type == allure.attachment_type.TEXT and not isinstance(attachment, (str, unicode)):
        attachment = pformat(attachment)

    if attachment_type != allure.attachment_type.TEXT:
        log_text = u'Тело аттача с типом {} было обрезано'.format(attachment_type)
    elif attachment:
        if log_label:
            log_text = u'{}{}{}'.format(utils.String.unicodify(label), separator, utils.String.unicodify(attachment))
        else:
            log_text = utils.String.unicodify(attachment)
    else:
        log_text = utils.String.unicodify(label)

    onelinable = attachment_type == allure.attachment_type.TEXT and len(utils.single_line(log_text)) < maxlen
    if oneline or onelinable or not attachment:
        log_text = utils.single_line(log_text)
        if oneline and len(utils.single_line(log_text)) >= maxlen > 0:
            log_text = utils.single_line(log_text)[:maxlen] + u'...'

        with step(log_text, allure_=allure_, log_=log_):
            pass

    else:
        if allure_ and options().is_use_allure():
            allure.attach(attachment, name=label, attachment_type=attachment_type)
        if log_:
            allure_logger().info(_log_border(text=log_text))


def feature(*features):
    return allure.feature(*features)


def environment(**kwargs):
    # type (unicode, unicode) -> None
    for key, value in kwargs.iteritems():
        _ENVIRONMENT[key] = value


def _write_environment(report_dir):
    root = E.environment(E.id(str(uuid.uuid4())), E.name('Allure environment parameters'),
                         *[E.parameter(E.key(key), E.name('contact igogor'), E.value(value))
                           for key, value in _ENVIRONMENT.iteritems()])
    log('Allure environment file: {}'.format(os.path.join(report_dir, 'environment.xml')))
    log('Allure environment content: {}'.format(_ENVIRONMENT))
    with open(os.path.join(report_dir, 'environment.xml'), 'wb') as env_file:
        env_file.write(etree.tostring(root, pretty_print=True, encoding='utf-8'))


# метод аналогичен pprint.pformat но выводит красиво юникод
# todo-igogor в 3.6 есть чудесный параметр compact который хотелось бы иметь но в 2.7 его нет
def pformat(obj, stream=None, indent=1, width=180, depth=None):
    printer = UnicodePrettyPrinter(stream=stream, indent=indent, width=width, depth=depth)
    return unicode(printer.pformat(obj), encoding='utf-8', errors='ignore')


# методы начинающиеся со str_ преобразуют конкретные типы сущностей в строку для отчета
# для большинства случаев pformat должно быть достаточно, не стоит писать их слишком много
def str_call_args(args, kwargs):
    str_args = [pformat(arg) for arg in args]
    str_kwargs = [u'{}={}'.format(pformat(name), pformat(param)) for name, param in kwargs.iteritems()]

    lines = []
    line = []
    for param in str_args + str_kwargs:
        line_length = sum([len(line_param) for line_param in line]) + len(param)
        if u'\n' in param:
            lines.append(u', '.join(line))
            lines.append(param)
            line = []
        elif line_length > 100:
            lines.append(u', '.join(line))
            line = [param]
        else:
            line.append(param)
    return u',\n'.join(utils.remove_false(lines + [u', '.join(line)]))


def str_call(name, args, kwargs):
    pretty_args = str_call_args(args, kwargs)
    return u'{}({})'.format(name, utils.String.fill_newline(text=pretty_args, shift=len(name) + 1))


# Методы начинающиеся с report упрощают прикрепление к отчету более сложных структур. Не следует писать их слишком много

def report_urls(label, *title_url_tuples):
    urls_in_uri_list_format = u"\n".join([u"# {}\n{}".format(title, url) for title, url in title_url_tuples])
    attach(label, urls_in_uri_list_format, attachment_type=allure.attachment_type.URI_LIST, allure_=True, log_=False)
    for title, url in title_url_tuples:
        attach(title, url, oneline=True, maxlen=-1, allure_=False, log_=True)


def report_url(title, url):
    report_urls(title, (title, url))


def report_http_call(method, url, headers=None, data=None):
    headers_str = u'\n\nHeaders:\n\n{}'.format(utils.Presenter.pretty(headers)) if headers else u''
    data_str = u'\n\nData:\n\n{}'.format(utils.Presenter.pretty(data)) if data else u''
    attach(u'Параметры {}-запроса'.format(method),
           u'Url: {url}{headers}{data}'.format(url=url, headers=headers_str, data=data_str))


# todo-igogor имхо этот метод не особо полезен. Логики в нем нет, используется в 3х местах
def report_http_call_curl(method, url, headers=None, data=None, json_data=None, cookies=None):
    attach(u'{}-запрос'.format(method), utils.Presenter.http_call_curl(url, method, headers, data, json_data, cookies))


# Утилитные вещи для управления репортингом

class Level(enum.IntEnum):
    # ручные степы и аттачи - те что делаются явно в сценариях.
    # Авторепортинг - степы и аттачи деляющиеся неявно (инфо о работе с xmlrpc, mongo, webdriver и т.д.)
    ALL = 0  # выводим все ручные степы, аттачи и весь авторепортинг (степы, запросы и ответы)
    AUTO_STEPS_ONLY = 2  # выводим все ручные степы, аттачи и частично авторепортинг (степы, без запросов и ответов)
    AUTO_ONE_LINE = 3  # выводим все ручные степы, аттачи и инфо об авторепорте одной строкой
    MANUAL_ONLY = 4  # выводим все ручные степы, аттачи и не выводим авторепортинг
    MANUAL_STEPS_ONLY = 5  # выводим все ручные степы, без аттачей и не выводим авторепортинг
    NOTHING = 6  # не выводим ничего вообще


@attr.s
class Options(object):
    level = attr.ib(default=Level[config.REPORTER_LEVEL])

    # @utils.cached
    @property
    def is_show_all(self):
        return config.REPORTER_SHOW_ALL

    def is_use_allure(self):
        return utils.is_inside_test()

    def is_show_level(self, level):
        return level >= self.level or self.is_show_all


@utils.cached
def options():
    return Options()


@contextmanager
def reporting(level=Level.ALL):
    old_level = options().level
    options().level = level
    try:
        yield
    except Exception as e:
        raise e
    finally:
        options().level = old_level


# Служебные функции для красивого вывода объектов в текстовом виде

class UnicodePrettyPrinter(PrettyPrinter):
    """Unicode-friendly PrettyPrinter
    Prints:
      - u'привет' instead of u'\u043f\u0440\u0438\u0432\u0435\u0442'
      - 'привет' instead of '\xd0\xbf\xd1\x80\xd0\xb8\xd0\xb2\xd0\xb5\xd1\x82'
    """

    def format(self, *args, **kwargs):
        repr_, readable, recursive = PrettyPrinter.format(self, *args, **kwargs)
        if repr_:
            if repr_[0] in ('"', "'"):
                repr_ = repr_.decode('string_escape')
            elif repr_[0:2] in ("u'", 'u"'):
                repr_ = repr_.decode('unicode_escape').encode('utf-8')
        return repr_, readable, recursive

    def _repr(self, object, context, level):
        repr_, readable, recursive = self.format(object, context.copy(),
                                                 self._depth, level)
        if not readable:
            self._readable = False
        if recursive:
            self._recursive = True
        return repr_


# def pprint(obj, stream=None, indent=1, width=180, depth=None):
#     printer = UnicodePrettyPrinter(stream=stream, indent=indent, width=width, depth=depth)
#     printer.pprint(obj)



# Автоматическое прикрепление репортинга к действиям

# class ReportingCallable(object):
#     def __init__(self, callable_object):
#         self.callable_object = callable_object
#         self.__name__ = utils.Presenter.method_name(callable_object)
#
#     @staticmethod
#     def attach_to_global_log(request, result):
#         if options().is_use_log(level=LogLevel.ALL) and 'G' in utils.LOGGER_OPTIONS:
#             # Store all xmlrpc calls in global variable GLOBAL_XMLRPC_LOG
#             utils.GLOBAL_LOG.append({'Request': request, 'Response': utils.Presenter.pretty(result)})
#
#     def __call__(self, *args, **kwargs):
#         with step(u"Вызываем метод " + self.__name__, optional=not options().is_use_allure(level=AllureLevel.STEPS)):
#             request = self.prepare_request(*args, **kwargs)
#
#             result = self.make_call(*args, **kwargs)
#             attach(u'Запрос и Ответ', u'{}\n\n{}'.format(request, utils.Presenter.pretty(result)),
#                    optional=not options().is_use_allure(level=AllureLevel.ALL))
#
#             ReportingCallable.attach_to_global_log(request, result)
#
#             return result
#
#     def prepare_request(self, *args, **kwargs):
#         request = utils.Presenter.executable_method_call(self.callable_object, args, kwargs)
#
#         if 'R' in utils.LOGGER_OPTIONS:
#             if options().is_use_log(level=LogLevel.REQUEST):
#                 utils.log.debug(u'Вызов: {}'.format(request))
#             elif options().is_use_log(level=LogLevel.METHOD_NAME):
#                 utils.log.debug(u"Вызываем метод " + self.__name__)
#
#         return request
#
#     def make_call(self, *args, **kwargs):
#         try:
#             result = self.callable_object(*args, **kwargs)
#         except xmlrpclib.Fault as f:
#             # Fault может содержать нечитаемый repr от юникода, поэтому логгируем ошибку в читаемом виде
#             if options().is_use_log(level=LogLevel.ERROR) and 'R' in utils.LOGGER_OPTIONS:
#                 utils.log.error(u'XmlRpc error: {}'.format(f.faultString))
#             attach(u'XmlRpc error', f.faultString,
#                    optional=not options().is_use_allure(level=AllureLevel.ALL))
#             raise f
#
#         if options().is_use_log(level=LogLevel.ALL) and 'R' in utils.LOGGER_OPTIONS:
#             utils.log.debug(u'Ответ: {}\n'.format(utils.Presenter.pretty(result)))
#
#         return result

REPLACE_BY_REPORT_URL = U'ShouldBeReplacedForCurrentReportUrl'


# todo-igogor поскольку больше не прикладываем ссылки как html, это не нужно.
def links_html(*title_url_tuples):
    def body(content):
        jquery_script = u'<script type="text/javascript" src="https://yandex.st/jquery/1.9.1/jquery.min.js"></script>'
        return u'<html><head>{}\n<meta charset="utf-8"></head><body>{}</body></html>'.format(jquery_script, content)

    def href(title, url):
        return u'<a class="stSubmitLink" href="{url}" target="_blank">{title}</a>'.format(url=url, title=title)

    def div(content):
        return u'<div>{}</div>'.format(content)

    st_links = u'<br>\n'.join([href(url if len(url) < MAXLEN else title, url) for title, url in title_url_tuples])
    link_to_current_report_script = u'''<script type="text/javascript">
    $(document).ready(function () {
        // во всех ссылках на создание тикета подставляем текущий урл в href
        var url = (window.location != window.parent.location) ? window.parent.location.href : window.location.href;
        $.get("https://nda.ya.ru/--", {url: url}, function(short_url) {
            $('.stSubmitLink').each(function() {
                this.href = this.href.replace('ShouldBeReplacedForCurrentReportUrl', short_url);
            });
        }, "text");
    });
    </script>'''

    return body(div(u'{}\n{}'.format(st_links, link_to_current_report_script)))





# Методы для работы с кишками аллюр репорта

# def current_allure_testcase():
#     if allure.MASTER_HELPER.get_listener():
#         return allure.MASTER_HELPER.get_listener().test
#     else:
#         raise utils.TestsError(u'Trying to use allure functionality without active plugin. Contact igogor@')
# None


def add_feature(testcase, feature):
    pass
    # testcase.labels.append(TestLabel(name=Label.FEATURE, value=feature))


class LogStep(object):
    _MARKS = []

    @staticmethod
    def level():
        return len([mark for mark in LogStep._MARKS if mark])

    def __init__(self, allure_step, to_log):
        self.allure_step = allure_step
        self.to_log = to_log

    def __enter__(self):
        LogStep._MARKS.append(self.to_log)
        return self.allure_step.__enter__()

    def __exit__(self, exc_type, exc_val, exc_tb):
        LogStep._MARKS.pop()
        return self.allure_step.__exit__(exc_type, exc_val, exc_tb)

        # def _current_allure_level():
        # igogor: эта логика полагается на то что запустились с активным плагином аллюра. Поэтому убрал, но припасу.
        # steps = current_allure_testcase().steps if current_allure_testcase() else []
        #
        # level = 0
        # while steps and not steps[-1].stop:
        #     level += 1
        #     steps = steps[-1].steps
        # return level

# def _mark_next_step(mark):
#     global STEP_MARKS
#     current_level = _current_allure_level()
#     if current_level < len(STEP_MARKS):
#         STEP_MARKS = STEP_MARKS[:_current_allure_level()]
#     elif current_level > len(STEP_MARKS):
#         raise utils.TestsError('Error in calculating log level. Contact igogor@')
#     STEP_MARKS.append(mark)
